/*
 * Copyright (c) 2019 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.operation.overlayng;

import java.util.Collection;
import java.util.List;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Location;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.geomgraph.Label;
import org.locationtech.jts.noding.MCIndexNoder;
import org.locationtech.jts.noding.Noder;
import org.locationtech.jts.noding.snap.SnappingNoder;
import org.locationtech.jts.noding.snapround.SnapRoundingNoder;
import org.locationtech.jts.operation.overlay.OverlayOp;

/**
 * Computes the geometric overlay of two {@link Geometry}s, 
 * using an explicit precision model to allow robust computation.
 * <p>
 * The overlay can be used to determine any of the 
 * following set-theoretic operations (boolean combinations) of the geometries:</p>
 * <ul>
 * <li>{@link #INTERSECTION} - all points which lie in both geometries</li>
 * <li>{@link #UNION} - all points which lie in at least one geometry</li>
 * <li>{@link #DIFFERENCE} - all points which lie in the first geometry but not the second</li>
 * <li>{@link #SYMDIFFERENCE} - all points which lie in one geometry but not both</li>
 * </ul>
 * Input geometries may have different dimension.  
 * Input collections must be homogeneous (all elements must have the same dimension).
 * Inputs may be <b>simple</b> {@link GeometryCollection}s.
 * A GeometryCollection is simple if it can be flattened into a valid Multi-geometry;
 * i.e. it is homogeneous and does not contain any overlapping Polygons.  
 * <p>
 * The precision model used for the computation can be supplied 
 * independent of the precision model of the input geometry.
 * The main use for this is to allow using a fixed precision 
 * for geometry with a floating precision model.
 * This does two things: ensures robust computation;
 * and forces the output to be validly rounded to the precision model.</p>
 * <p>
 * For fixed precision models noding is performed using a {@link SnapRoundingNoder}.
 * This provides robust computation (as long as precision is limited to
 * around 13 decimal digits).</p>
 * <p>
 * For floating precision an {@link MCIndexNoder} is used. 
 * This is not fully robust, so can sometimes result in 
 * {@link TopologyException}s being thrown. 
 * For robust full-precision overlay see {@link OverlayNGRobust}.</p>
 * <p>
 * A custom {@link Noder} can be supplied.
 * This allows using a more performant noding strategy in specific cases, 
 * for instance in {@link CoverageUnion}.</p>
 * <p>
 * <b>Note:</b If a {@link SnappingNoder} is used 
 * it is best to specify a fairly small snap tolerance,
 * since the intersection clipping optimization can 
 * interact with the snapping to alter the result.</p>
 * <p>
 * Optionally the overlay computation can process using strict mode
 * (via {@link #setStrictMode(boolean)}.
 * In strict mode result semantics are:</p>
 * <ul>
 * <li>Lines and Points resulting from topology collapses are not included in the result</li>
 * <li>Result geometry is homogeneous 
 *     for the {@link #INTERSECTION} and {@link #DIFFERENCE} operations.</li>
 * <li>Result geometry is homogeneous
 *     for the {@link #UNION} and {@link #SYMDIFFERENCE} operations
 *     if the inputs have the same dimension</li>
 * </ul>
 * <p>
 * Strict mode has the following benefits:</p>
 * <ul>
 * <li>Results are simpler</li>
 * <li>Overlay operations are chainable 
 *     without needing to remove lower-dimension elements</li>
 * </ul>
 * <p>
 * The original JTS overlay semantics corresponds to non-strict mode.</p>
 * <p>
 * If a robustness error occurs, a {@link TopologyException} is thrown.
 * These are usually caused by numerical rounding causing the noding output
 * to not be fully noded.
 * For robust computation with full-precision {@link OverlayNGRobust} can be used.</p>
 * 
 * @author mdavis
 * 
 * @see OverlayNGRobust
 *
 */
public class OverlayNG 
{
  /**
   * The code for the Intersection overlay operation.
   */
  public static final int INTERSECTION  = OverlayOp.INTERSECTION;
  
  /**
   * The code for the Union overlay operation.
   */
  public static final int UNION         = OverlayOp.UNION;
  
  /**
   *  The code for the Difference overlay operation.
   */
  public static final int DIFFERENCE    = OverlayOp.DIFFERENCE;
  
  /**
   *  The code for the Symmetric Difference overlay operation.
   */
  public static final int SYMDIFFERENCE = OverlayOp.SYMDIFFERENCE;

  /**
   * The default setting for Strict Mode.
   * 
   * The original JTS overlay semantics used non-strict result
   * semantics, including;
   * - An Intersection result can be mixed-dimension,
   *   due to inclusion of intersection components of all dimensions
   * - Results can include lines caused by Area topology collapse
   */
  static final boolean STRICT_MODE_DEFAULT = false;

  /**
   * Tests whether a point with a given topological {@link Label}
   * relative to two geometries is contained in 
   * the result of overlaying the geometries using
   * a given overlay operation.
   * <p>
   * The method handles arguments of {@link Location#NONE} correctly
   * 
   * @param label the topological label of the point
   * @param opCode the code for the overlay operation to test
   * @return true if the label locations correspond to the overlayOpCode
   */
  static boolean isResultOfOpPoint(OverlayLabel label, int opCode)
  {
    int loc0 = label.getLocation(0);
    int loc1 = label.getLocation(1);
    return isResultOfOp(opCode, loc0, loc1);
  }
  
  /**
   * Tests whether a point with given {@link Location}s
   * relative to two geometries would be contained in 
   * the result of overlaying the geometries using
   * a given overlay operation.
   * This is used to determine whether components
   * computed during the overlay process should be
   * included in the result geometry.
   * <p>
   * The method handles arguments of {@link Location#NONE} correctly.
   * 
   * @param overlayOpCode the code for the overlay operation to test
   * @param loc0 the code for the location in the first geometry 
   * @param loc1 the code for the location in the second geometry 
   *
   * @return true if a point with given locations is in the result of the overlay operation
   */
  static boolean isResultOfOp(int overlayOpCode, int loc0, int loc1)
  {
    if (loc0 == Location.BOUNDARY) loc0 = Location.INTERIOR;
    if (loc1 == Location.BOUNDARY) loc1 = Location.INTERIOR;
    switch (overlayOpCode) {
    case INTERSECTION:
      return loc0 == Location.INTERIOR
          && loc1 == Location.INTERIOR;
    case UNION:
      return loc0 == Location.INTERIOR
          || loc1 == Location.INTERIOR;
    case DIFFERENCE:
      return loc0 == Location.INTERIOR
          && loc1 != Location.INTERIOR;
    case SYMDIFFERENCE:
      return   (     loc0 == Location.INTERIOR &&  loc1 != Location.INTERIOR)
            || (     loc0 != Location.INTERIOR &&  loc1 == Location.INTERIOR);
    }
    return false;
  }
  
  /**
   * Computes an overlay operation for 
   * the given geometry operands, with the
   * noding strategy determined by the precision model.
   * 
   * @param geom0 the first geometry argument
   * @param geom1 the second geometry argument
   * @param opCode the code for the desired overlay operation
   * @param pm the precision model to use
   * @return the result of the overlay operation
   */
  public static Geometry overlay(Geometry geom0, Geometry geom1, 
      int opCode, PrecisionModel pm)
  {
    OverlayNG ov = new OverlayNG(geom0, geom1, pm, opCode);
    Geometry geomOv = ov.getResult();
    return geomOv;
  }

  /**
   * Computes an overlay operation on the given geometry operands, 
   * using a supplied {@link Noder}.
   * 
   * @param geom0 the first geometry argument
   * @param geom1 the second geometry argument
   * @param opCode the code for the desired overlay operation
   * @param pm the precision model to use (which may be null if the noder does not use one)
   * @param noder the noder to use
   * @return the result of the overlay operation
   */
  public static Geometry overlay(Geometry geom0, Geometry geom1, 
      int opCode, PrecisionModel pm, Noder noder)
  {
    OverlayNG ov = new OverlayNG(geom0, geom1, pm, opCode);
    ov.setNoder(noder);
    Geometry geomOv = ov.getResult();
    return geomOv;
  }

  /**
   * Computes an overlay operation on the given geometry operands, 
   * using a supplied {@link Noder}.
   * 
   * @param geom0 the first geometry argument
   * @param geom1 the second geometry argument
   * @param opCode the code for the desired overlay operation
   * @param noder the noder to use
   * @return the result of the overlay operation
   */
  public static Geometry overlay(Geometry geom0, Geometry geom1, 
      int opCode, Noder noder)
  {
    OverlayNG ov = new OverlayNG(geom0, geom1, null, opCode);
    ov.setNoder(noder);
    Geometry geomOv = ov.getResult();
    return geomOv;
  }

  /**
   * Computes an overlay operation on 
   * the given geometry operands,
   * using the precision model of the geometry.
   * and an appropriate noder.
   * <p>
   * The noder is chosen according to the precision model specified.
   * <ul>
   * <li>For {@link PrecisionModel#FIXED}
   * a snap-rounding noder is used, and the computation is robust.
   * <li>For {@link PrecisionModel#FLOATING}
   * a non-snapping noder is used,
   * and this computation may not be robust.
   * If errors occur a {@link TopologyException} is thrown.
   * </ul>
   * 
   * 
   * @param geom0 the first argument geometry
   * @param geom1 the second argument geometry
   * @param opCode the code for the desired overlay operation
   * @return the result of the overlay operation
   */
  public static Geometry overlay(Geometry geom0, Geometry geom1, int opCode)
  {
    OverlayNG ov = new OverlayNG(geom0, geom1, opCode);
    return ov.getResult();
  }

  /**
   * Computes a union operation on 
   * the given geometry, with the supplied precision model.
   * <p>
   * The input must be a valid geometry.
   * Collections must be homogeneous.
   * <p>
   * To union an overlapping set of polygons in a more performant way use {@link UnaryUnionNG}.
   * To union a polyonal coverage or linear network in a more performant way, 
   * use {@link CoverageUnion}.
   * 
   * @param geom0 the geometry
   * @param pm the precision model to use
   * @return the result of the union operation
   * 
   * @see OverlayMixedPoints
   */
  static Geometry union(Geometry geom, PrecisionModel pm)
  {    
    OverlayNG ov = new OverlayNG(geom, pm);
    Geometry geomOv = ov.getResult();
    return geomOv;
  }

  /**
   * Computes a union of a single geometry using a custom noder.
   * <p>
   * The primary use of this is to support coverage union.
   * Because of this the overlay is performed using strict mode.
   * 
   * @param geom the geometry to union
   * @param pm the precision model to use (maybe be null)
   * @param noder the noder to use
   * @return the result geometry
   * 
   * @see CoverageUnion
   */
  static Geometry union(Geometry geom, PrecisionModel pm, Noder noder)
  {    
    OverlayNG ov = new OverlayNG(geom, pm);
    ov.setNoder(noder);
    ov.setStrictMode(true);
    Geometry geomOv = ov.getResult();
    return geomOv;
  }
  
  private int opCode;
  private InputGeometry inputGeom;
  private GeometryFactory geomFact;
  private PrecisionModel pm;
  private Noder noder;
  private boolean isStrictMode = STRICT_MODE_DEFAULT;
  private boolean isOptimized = true;
  private boolean isAreaResultOnly = false;
  private boolean isOutputEdges = false;
  private boolean isOutputResultEdges = false;
  private boolean isOutputNodedEdges = false;

  /**
   * Creates an overlay operation on the given geometries,
   * with a defined precision model.
   * The noding strategy is determined by the precision model.
   * 
   * @param geom0 the A operand geometry
   * @param geom1 the B operand geometry (may be null)
   * @param pm the precision model to use
   * @param opCode the overlay opcode
   */
  public OverlayNG(Geometry geom0, Geometry geom1, PrecisionModel pm, int opCode) {
    this.pm = pm;
    this.opCode = opCode;
    geomFact = geom0.getFactory();
    inputGeom = new InputGeometry( geom0, geom1 );
  }  
  
  /**
   * Creates an overlay operation on the given geometries
   * using the precision model of the geometries.
   * <p>
   * The noder is chosen according to the precision model specified.
   * <ul>
   * <li>For {@link PrecisionModel#FIXED}
   * a snap-rounding noder is used, and the computation is robust.
   * <li>For {@link PrecisionModel#FLOATING}
   * a non-snapping noder is used,
   * and this computation may not be robust.
   * If errors occur a {@link TopologyException} is thrown.
   * </ul>
   *  
   * @param geom0 the A operand geometry
   * @param geom1 the B operand geometry (may be null)
   * @param opCode the overlay opcode
   */
  public OverlayNG(Geometry geom0, Geometry geom1, int opCode) {
    this(geom0, geom1, geom0.getFactory().getPrecisionModel(), opCode);
  }  
  
  /**
   * Creates a union of a single geometry with a given precision model.
   * 
   * @param geom the geometry
   * @param pm the precision model to use
   */
  OverlayNG(Geometry geom, PrecisionModel pm) {
    this(geom, null, pm, UNION);
  }  
  
  /**
   * Sets whether the overlay results are computed according to strict mode
   * semantics.
   * <ul>
   * <li>Lines resulting from topology collapse are not included
   * <li>Result geometry is homogeneous 
   *     for the {@link #INTERSECTION} and {@link #DIFFERENCE} operations.
   * <li>Result geometry is homogeneous 
   *     for the {@link #UNION} and {@link #SYMDIFFERENCE} operations
   *     if the inputs have the same dimension
   * </ul>
   * 
   * @param isStrictMode true if strict mode is to be used
   */
  public void setStrictMode(boolean isStrictMode) {
    this.isStrictMode = isStrictMode;
  }
  
  /**
   * Sets whether overlay processing optimizations are enabled.
   * It may be useful to disable optimizations
   * for testing purposes.
   * Default is TRUE (optimization enabled).
   * 
   * @param isOptimized whether to optimize processing
   */
  public void setOptimized(boolean isOptimized) {
    this.isOptimized = isOptimized;
  }
  
  /**
   * Sets whether the result can contain only {@link Polygon} components.
   * This is used if it is known that the result must be an (possibly empty) area.
   * 
   * @param isAreaResultOnly true if the result should contain only area components
   */
  void setAreaResultOnly(boolean isAreaResultOnly) {
    this.isAreaResultOnly = isAreaResultOnly;
  }
  
  //------ Testing options -------
  
  /**
   * 
   * @param isOutputEdges
   */
  public void setOutputEdges(boolean isOutputEdges ) {
    this.isOutputEdges = isOutputEdges;
  }
  
  public void setOutputNodedEdges(boolean isOutputNodedEdges ) {
    this.isOutputEdges = true;
    this.isOutputNodedEdges = isOutputNodedEdges;
  }
  
  public void setOutputResultEdges(boolean isOutputResultEdges ) {
    this.isOutputResultEdges = isOutputResultEdges;
  }
  //---------------------------------
  
  void setNoder(Noder noder) {
    this.noder = noder;
  }
  
  /**
   * Gets the result of the overlay operation.
   * 
   * @return the result of the overlay operation.
   * 
   * @throws IllegalArgumentException if the input is not supported (e.g. a mixed-dimension geometry)
   * @throws TopologyException if a robustness error occurs
   */
  public Geometry getResult() {
    // handle empty inputs which determine result
    if (OverlayUtil.isEmptyResult(opCode, 
        inputGeom.getGeometry(0), 
        inputGeom.getGeometry(1),
        pm)) {
      return createEmptyResult();
    }

    /**
     * The elevation model is only computed if the input geometries have Z values.
     */
    ElevationModel elevModel = ElevationModel.create(inputGeom.getGeometry(0), inputGeom.getGeometry(1));
    Geometry result;
    if (inputGeom.isAllPoints()) {
      // handle Point-Point inputs
      result = OverlayPoints.overlay(opCode, inputGeom.getGeometry(0), inputGeom.getGeometry(1), pm);
    }
    else if (! inputGeom.isSingle() &&  inputGeom.hasPoints()) {
      // handle Point-nonPoint inputs 
      result = OverlayMixedPoints.overlay(opCode, inputGeom.getGeometry(0), inputGeom.getGeometry(1), pm);
    }
    else {
      // handle case where both inputs are formed of edges (Lines and Polygons)
      result = computeEdgeOverlay();
    }
    /**
     * This is a no-op if the elevation model was not computed due to Z not present or if Z interpolation is turned off
     */
    elevModel.populateZ(result);
    return result;
  }
  
  private Geometry computeEdgeOverlay() 
  {
    
    List<Edge> edges = nodeEdges();
    
    OverlayGraph graph = buildGraph(edges);
    
    if (isOutputNodedEdges) {
      return OverlayUtil.toLines(graph, isOutputEdges, geomFact);
    }

    labelGraph(graph);
    //for (OverlayEdge e : graph.getEdges()) {  Debug.println(e);  }
    
    if (isOutputEdges || isOutputResultEdges) {
      return  OverlayUtil.toLines(graph, isOutputEdges, geomFact);
    }
    
    Geometry result = extractResult(opCode, graph);
    
    /**
     * Heuristic check on result area. 
     * Catches cases where noding causes vertex to move
     * and make topology graph area "invert".
     */
    if (OverlayUtil.isFloating(pm)) {
      boolean isAreaConsistent = OverlayUtil.isResultAreaConsistent(inputGeom.getGeometry(0), inputGeom.getGeometry(1), opCode, result);
      if (! isAreaConsistent)
        throw new TopologyException("Result area inconsistent with overlay operation");    
    }
    return result;
  }

  private List<Edge> nodeEdges() {
    /**
     * Node the edges, using whatever noder is being used
     */
    EdgeNodingBuilder nodingBuilder = new EdgeNodingBuilder(pm, noder);
    
    /**
     * Optimize Intersection and Difference by clipping to the 
     * result extent, if enabled.
     */
    if ( isOptimized ) {
      Envelope clipEnv = OverlayUtil.clippingEnvelope(opCode, inputGeom, pm);
      if (clipEnv != null)
        nodingBuilder.setClipEnvelope( clipEnv );
    }
    
    List<Edge> mergedEdges = nodingBuilder.build(
        inputGeom.getGeometry(0), 
        inputGeom.getGeometry(1));
    
    /**
     * Record if an input geometry has collapsed.
     * This is used to avoid trying to locate disconnected edges
     * against a geometry which has collapsed completely.
     */
    inputGeom.setCollapsed(0, ! nodingBuilder.hasEdgesFor(0) );
    inputGeom.setCollapsed(1, ! nodingBuilder.hasEdgesFor(1) );
    
    return mergedEdges;
  }

  private OverlayGraph buildGraph(Collection<Edge> edges) {
    OverlayGraph graph = new OverlayGraph();
    for (Edge e : edges) {
      graph.addEdge(e.getCoordinates(), e.createLabel());
    }
    return graph;
  }
  
  private void labelGraph(OverlayGraph graph) {
    OverlayLabeller labeller = new OverlayLabeller(graph, inputGeom);
    labeller.computeLabelling();
    labeller.markResultAreaEdges(opCode);
    labeller.unmarkDuplicateEdgesFromResultArea();
  }

  /**
   * Extracts the result geometry components from the fully labelled topology graph.
   * <p>
   * This method implements the semantic that the result of an 
   * intersection operation is homogeneous with highest dimension.  
   * In other words, 
   * if an intersection has components of a given dimension
   * no lower-dimension components are output.
   * For example, if two polygons intersect in an area, 
   * no linestrings or points are included in the result, 
   * even if portions of the input do meet in lines or points.
   * This semantic choice makes more sense for typical usage, 
   * in which only the highest dimension components are of interest.
   * 
   * @param opCode the overlay operation
   * @param graph the topology graph
   * @return the result geometry
   */
  private Geometry extractResult(int opCode, OverlayGraph graph) {
    boolean isAllowMixedIntResult = ! isStrictMode;
    
    //--- Build polygons
    List<OverlayEdge> resultAreaEdges = graph.getResultAreaEdges();
    PolygonBuilder polyBuilder = new PolygonBuilder(resultAreaEdges, geomFact);
    List<Polygon> resultPolyList = polyBuilder.getPolygons();
    boolean hasResultAreaComponents = resultPolyList.size() > 0;
    
    List<LineString> resultLineList = null;
    List<Point> resultPointList = null;
    
    if (! isAreaResultOnly) {
      //--- Build lines
      boolean allowResultLines = ! hasResultAreaComponents 
          || isAllowMixedIntResult
          || opCode == SYMDIFFERENCE
          || opCode == UNION;
      if ( allowResultLines ) {
        LineBuilder lineBuilder = new LineBuilder(inputGeom, graph, hasResultAreaComponents, opCode, geomFact);
        lineBuilder.setStrictMode(isStrictMode);
        resultLineList = lineBuilder.getLines();
      }
      /**
       * Operations with point inputs are handled elsewhere.
       * Only an Intersection op can produce point results
       * from non-point inputs. 
       */
      boolean hasResultComponents = hasResultAreaComponents || resultLineList.size() > 0;
      boolean allowResultPoints = ! hasResultComponents || isAllowMixedIntResult;
      if ( opCode == INTERSECTION && allowResultPoints ) {
        IntersectionPointBuilder pointBuilder = new IntersectionPointBuilder(graph, geomFact);
        pointBuilder.setStrictMode(isStrictMode);
        resultPointList = pointBuilder.getPoints();
      }
    }
    
    if (isEmpty(resultPolyList) 
        && isEmpty(resultLineList) 
        && isEmpty(resultPointList))
      return createEmptyResult();
    
    Geometry resultGeom = OverlayUtil.createResultGeometry(resultPolyList, resultLineList, resultPointList, geomFact);
    return resultGeom;
  }

  private static boolean isEmpty(List list) {
    return list == null || list.size() == 0;
  }
  
  private Geometry createEmptyResult() {
    return OverlayUtil.createEmptyResult(
        OverlayUtil.resultDimension(opCode, 
            inputGeom.getDimension(0), 
            inputGeom.getDimension(1)), 
          geomFact);
  }
 
}


