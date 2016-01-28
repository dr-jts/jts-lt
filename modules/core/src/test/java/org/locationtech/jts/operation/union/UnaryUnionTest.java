/*
 * The JTS Topology Suite is a collection of Java classes that
 * implement the fundamental operations required to validate a given
 * geo-spatial data set to a known topological specification.
 * 
 * Copyright (C) 2016 Vivid Solutions
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Vivid Solutions BSD
 * License v1.0 (found at the root of the repository).
 * 
 */

package org.locationtech.jts.operation.union;

import java.util.*;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.*;
import org.locationtech.jts.operation.union.*;

import test.jts.junit.*;

import junit.framework.TestCase;

public class UnaryUnionTest extends TestCase 
{
	GeometryFactory geomFact = new GeometryFactory();
	
  public UnaryUnionTest(String name) {
    super(name);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(UnaryUnionTest.class);
  }

  public void testEmptyCollection()
  throws Exception
  {
    doTest(new String[]{}, "GEOMETRYCOLLECTION EMPTY");
  }

  public void testPoints()
  throws Exception
  {
    doTest(new String[]{ "POINT (1 1)", "POINT (2 2)"}, "MULTIPOINT ((1 1), (2 2))");
  }

  public void testLineNoding()
  throws Exception
  {
    doTest(new String[]{ "LINESTRING (0 0, 10 0, 5 -5, 5 5)"}, 
        "MULTILINESTRING ((0 0, 5 0), (5 0, 10 0, 5 -5, 5 0), (5 0, 5 5))");
  }

  public void testAll()
  throws Exception
  {
    doTest(new String[]{"GEOMETRYCOLLECTION (POLYGON ((0 0, 0 90, 90 90, 90 0, 0 0)),   POLYGON ((120 0, 120 90, 210 90, 210 0, 120 0)),  LINESTRING (40 50, 40 140),  LINESTRING (160 50, 160 140),  POINT (60 50),  POINT (60 140),  POINT (40 140))"},
    		"GEOMETRYCOLLECTION (POINT (60 140),   LINESTRING (40 90, 40 140), LINESTRING (160 90, 160 140), POLYGON ((0 0, 0 90, 40 90, 90 90, 90 0, 0 0)), POLYGON ((120 0, 120 90, 160 90, 210 90, 210 0, 120 0)))");  }

  private void doTest(String[] inputWKT, String expectedWKT) 
  throws ParseException
  {
  	Geometry result;
  	Collection geoms = GeometryUtils.readWKT(inputWKT);
  	if (geoms.size() == 0)
  		result = UnaryUnionOp.union(geoms, geomFact);
  	else
  		result = UnaryUnionOp.union(geoms);
  	
  	assertTrue(GeometryUtils.isEqual(GeometryUtils.readWKT(expectedWKT), result));
  }

}
