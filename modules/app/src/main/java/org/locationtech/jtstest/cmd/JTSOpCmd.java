/*
 * Copyright (c) 2019 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jtstest.cmd;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jtstest.command.CommandLine;
import org.locationtech.jtstest.command.Option;
import org.locationtech.jtstest.command.OptionSpec;
import org.locationtech.jtstest.command.ParseException;
import org.locationtech.jtstest.function.DoubleKeyMap;
import org.locationtech.jtstest.geomfunction.BaseGeometryFunction;
import org.locationtech.jtstest.geomfunction.GeometryFunction;
import org.locationtech.jtstest.geomfunction.GeometryFunctionRegistry;
import org.locationtech.jtstest.util.io.MultiFormatReader;

/**
 * A CLI to run JTS TestBuilder operations.
 * Allows easier execution of JTS functions on test data for debugging purposes.
 * <p>
 * Examples:
 * 
 * <pre>
 * --- Compute the area of a WKT geometry, output it
 * jtsop -a some-file-with-geom.wkt -f txt area 
 * 
 * --- Compute the unary union of a WKT geometry, output as WKB
 * jtsop -a some-file-with-geom.wkt -f wkb Overlay.unaryUnion 
 * 
 * --- Compute the union of two geometries in WKT and WKB, output as WKT
 * jtsop -a some-file-with-geom.wkt -b some-other-geom.wkb -f wkt Overlay.Union
 * 
 * --- Compute the buffer of distance 10 of a WKT geometry, output as GeoJSON
 * jtsop -a some-file-with-geom.wkt -f geojson Buffer.buffer 10
 * 
 * --- Compute the buffer of a literal geometry, output as WKT
 * jtsop -a "POINT (10 10)" -f wkt Buffer.buffer 10
 * 
 * --- Compute multiple buffers
 * jtsop -a "POINT (10 10)" -f wkt Buffer.buffer val(1,10,100)
 * 
 * --- Run op for each A 
 * jtsop -a "MULTIPOINT ((10 10), (20 20))" -each A -f wkt Buffer.buffer
 * 
 * --- Output a literal geometry as GeoJSON
 * jtsop -a "POINT (10 10)" -f geojson
 * </pre>
 * 
 * @author Martin Davis
 *
 */
public class JTSOpCmd {

  // TODO: add option -ab to read both geoms from a file
  // TODO: allow -a stdin  to indicate reading from stdin.  
  public static final String ERR_INVALID_PARAMETER = "Invalid Parameter";

  private static final String MACRO_VAL = "val";

  public static void main(String[] args)
  {    
    JTSOpCmd cmd = new JTSOpCmd();
    try {
      JTSOpRunner.OpParams cmdArgs = cmd.parseArgs(args);
      cmd.execute(cmdArgs);
    } 
    catch (CommandError e) {
      // for command errors, just print the message
      System.err.println(e.getMessage() );
    }
    catch (Exception e) {
      // unexpected errors get a stack track to help debugging
      e.printStackTrace();
    }
  }

  private static CommandLine createCmdLine() {
    CommandLine commandLine = new CommandLine('-');
    commandLine.addOptionSpec(new OptionSpec(CommandOptions.GEOMFUNC, OptionSpec.NARGS_ONE_OR_MORE))
    .addOptionSpec(new OptionSpec(CommandOptions.VERBOSE, 0))
    .addOptionSpec(new OptionSpec(CommandOptions.V, 0))
    .addOptionSpec(new OptionSpec(CommandOptions.HELP, 0))
    .addOptionSpec(new OptionSpec(CommandOptions.OP, 1))
    .addOptionSpec(new OptionSpec(CommandOptions.GEOMA, 1))
    .addOptionSpec(new OptionSpec(CommandOptions.GEOMB, 1))
    .addOptionSpec(new OptionSpec(CommandOptions.EACH, 1))
    .addOptionSpec(new OptionSpec(CommandOptions.FORMAT, 1))
    .addOptionSpec(new OptionSpec(CommandOptions.REPEAT, 1))
    .addOptionSpec(new OptionSpec(CommandOptions.VALIDATE, 0))
    .addOptionSpec(new OptionSpec(OptionSpec.OPTION_FREE_ARGS, OptionSpec.NARGS_ONE_OR_MORE));
    return commandLine;
  }

  static final String[] helpDoc = new String[] {
  "",
  "Usage: jtsop - CLI for JTS operations",
  "           [ -a <wkt> | <wkb> | stdin | <filename.ext> ]",
  "           [ -b <wkt> | <wkb> | stdin | <filename.ext> ]",
  "           [ -each ( a | b | ab ) ]",
  "           [ -f ( txt | wkt | wkb | geojson | gml | svg ) ]",
  "           [ -repeat <num> ]",
  "           [ -validate ]",
  "           [ -geomfunc <classname> ]",
  "           [ -v, -verbose ]",
  "           [ -help]",
  "           [ op [ args... ]]",
  "  op              name of the operation (Category.op)",
  "  args            one or more scalar arguments to the operation",
  "                  - Use val(v1,v2,v3,..) for multiple arguments",
  "",
  "  -a              Geometry A: literal, stdin (WKT or WKB), or filename (extension: WKT, WKB, GeoJSON, GML, SHP)",
  "  -b              Geometry A: literal, stdin (WKT or WKB), or filename (extension: WKT, WKB, GeoJSON, GML, SHP)",
  "  -each           execute op on each component of A and/or B",
  "  -f              output format to use.  If omitted output is silent",
  "  -repeat         repeat the operation N times",
  "  -validate       validate the result of each operation",
  "  -geomfunc       specifies class providing geometry operations",
  "  -v, -verbose    display information about execution",
  "  -help           print a list of available operations"
  };
  
  private void printHelp(boolean showFunctions) {
    for (String s : helpDoc) {
      System.out.println(s);
    }
    if (showFunctions) {
      System.out.println();
      System.out.println("Operations:");
      printFunctions();
   }
  }
  
  private void printFunctions() {
    //TODO: include any loaded functions
    DoubleKeyMap funcMap = funcRegistry.getCategorizedGeometryFunctions();
    @SuppressWarnings("unchecked")
    Collection<String> categories = funcMap.keySet();
    for (String category : categories) {
      @SuppressWarnings("unchecked")
      Collection<GeometryFunction> funcs = funcMap.values(category);
      for (GeometryFunction fun : funcs) {
        System.out.println(category + "." + functionDesc(fun));
      }
    }
  }

  private static String functionDesc(GeometryFunction fun) {
    // TODO: display this in a command arg style
    BaseGeometryFunction geomFun = (BaseGeometryFunction) fun;
    return geomFun.getSignature();
    //return geomFun.getName();
  }

  private GeometryFunctionRegistry funcRegistry = GeometryFunctionRegistry.createTestBuilderRegistry();
  private CommandLine commandLine = createCmdLine();

  private boolean isHelp = false;
  private boolean isHelpWithFunctions = false;

  private JTSOpRunner opRunner;

  public JTSOpCmd() {
    opRunner = new JTSOpRunner();
  }
  
  public void captureOutput() {
    opRunner.captureOutput();
  }
  
  public void captureResult() {
    opRunner.captureResult();
  }
  
  public List<Geometry> getResultGeometry() {
    return opRunner.getResultGeometry();
  }
  
  public void replaceStdIn(InputStream inStream) {
    opRunner.replaceStdIn(inStream);
  }
  
  public String getOutput() {
    return opRunner.getOutput();
  }
  
  public static boolean isFilename(String arg) {
    if (arg == null) return false;
    if (MultiFormatReader.isWKB(arg)) return false;
    if (isWKT(arg)) return false;
    /*
    if (arg.indexOf("/") > 0
        || arg.indexOf("\\") > 0
        || arg.indexOf(":") > 0)
      return true;
      */
    return true;
  }

  private static boolean isWKT(String arg) {
    // TODO: make this smarter
    boolean hasParen = (arg.indexOf("(") > 0) && arg.indexOf(")") > 0;
    if (hasParen) return true;
    return false;
  }

  void execute(JTSOpRunner.OpParams cmdArgs) {
    if (isHelp || isHelpWithFunctions) {
      printHelp(isHelpWithFunctions);
      return;
    }
    opRunner.setRegistry(funcRegistry);
    opRunner.execute(cmdArgs);
  }

  JTSOpRunner.OpParams parseArgs(String[] args) throws ParseException, ClassNotFoundException {
    
    if (args.length == 0) {
      isHelp = true;
      return null;
    }
    commandLine.parse(args);

    JTSOpRunner.OpParams cmdArgs = new JTSOpRunner.OpParams();
    cmdArgs.operation = commandLine.getOptionArg(CommandOptions.OP, 0);
    
    
    String argA = commandLine.getOptionArg(CommandOptions.GEOMA, 0);
    if (argA != null) {
      if (isFilename(argA)) {
        cmdArgs.fileA = argA;
      }
      else {
        cmdArgs.geomA = argA;
      }
    }
    String argB = commandLine.getOptionArg(CommandOptions.GEOMB, 0);
    if (argB != null) {
      if (isFilename(argB)) {
        cmdArgs.fileB = argB;
      }
      else {
        cmdArgs.geomB = argB;
      }
    }
    
    cmdArgs.format = commandLine.getOptionArg(CommandOptions.FORMAT, 0);
    
    cmdArgs.repeat = commandLine.hasOption(CommandOptions.REPEAT)
        ? commandLine.getOptionArgAsInt(CommandOptions.REPEAT, 0)
            : 1;
    cmdArgs.validate  = commandLine.hasOption(CommandOptions.VALIDATE);

    if (commandLine.hasOption(CommandOptions.EACH)) {
      String each = commandLine.getOptionArg(CommandOptions.EACH, 0);

      if (each.equalsIgnoreCase("a")) {
        cmdArgs.eachA = true;
      }
      else if (each.equalsIgnoreCase("b")) {
        cmdArgs.eachB = true;
      }
      else if (each.equalsIgnoreCase("ab")) {
        cmdArgs.eachA = true;
        cmdArgs.eachB = true;
      }
      else {
        throw new CommandError(ERR_INVALID_PARAMETER, "-each " + each);
      }
    }
    boolean isVerbose = commandLine.hasOption(CommandOptions.VERBOSE)
                  || commandLine.hasOption(CommandOptions.V);
    opRunner.setVerbose(isVerbose);
    
    isHelpWithFunctions = commandLine.hasOption(CommandOptions.HELP);

    if (commandLine.hasOption(CommandOptions.GEOMFUNC)) {
      Option opt = commandLine.getOption(CommandOptions.GEOMFUNC);
      for (int i = 0; i < opt.getNumArgs(); i++) {
        String geomFuncClassname = opt.getArg(i);
        try {
          funcRegistry.add(geomFuncClassname);
          System.out.println("Added Geometry Functions from: " + geomFuncClassname);
        } catch (ClassNotFoundException ex) {
          System.out.println("Unable to load function class: " + geomFuncClassname);
        }
      }
    }
    
    String[] freeArgs = commandLine.getOptionArgs(OptionSpec.OPTION_FREE_ARGS);
    if (freeArgs != null) {
      if (freeArgs.length >= 1) {
        cmdArgs.operation = freeArgs[0];
      }
      if (freeArgs.length >= 2) {
        cmdArgs.argList = parseOpArg(freeArgs[1]);
      }
    }
    return cmdArgs;
  }
  
  private String[] parseOpArg(String arg) {
    if (arg.startsWith(MACRO_VAL + "(")) 
      return parseValues(arg);
    // no other macros, for now
    if (arg.contains("(")) 
        throw new CommandError(ERR_INVALID_PARAMETER, arg); 
    return new String[] { arg };
  }

  private String[] parseValues(String arg) {
    int indexLeft = arg.indexOf('(');
    int indexRight = arg.indexOf(')');
    if (indexRight <= 0) 
      throw new CommandError(ERR_INVALID_PARAMETER, arg);  
    // TODO: error if no R paren
    String content = arg.substring(indexLeft + 1, indexRight);
    return content.split(",");
  }



}
