/*
 * Copyright (c) 2016 Vivid Solutions.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.operation.polygonize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryComponentFilter;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;


/**
 * Polygonizes a set of {@link Geometry}s which contain linework that
 * represents the edges of a planar graph.
 * All types of Geometry are accepted as input;  
 * the constituent linework is extracted as the edges to be polygonized.
 * The processed edges must be correctly noded; that is, they must only meet
 * at their endpoints.  Polygonization will accept incorrectly noded input
 * but will not form polygons from non-noded edges, 
 * and reports them as errors.
 * <p>
 * The Polygonizer reports the follow kinds of errors:
 * <ul>
 * <li><b>{@link #getDangles() Dangles}</b> - edges which have one or both ends which are not incident on another edge endpoint
 * <li><b>{@link #getCutEdges() Cut Edges}</b> - edges which are connected at both ends but which do not form part of polygon
 * <li><b>{@link #getInvalidRingLines() Invalid Ring Lines}</b> - edges which form rings which are invalid
 * (e.g. the component lines contain a self-intersection)
 * </ul>
 * The {@link #Polygonizer(boolean)} constructor allows
 * extracting only polygons which form a valid polygonal result.
 * The set of extracted polygons is guaranteed to be edge-disjoint.
 * This is useful where it is known that the input lines form a
 * valid polygonal geometry (which may include holes or nested polygons).
 *
 * @version 1.7
 */
public class Polygonizer
{  
  /**
   * Adds every linear element in a {@link Geometry} into the polygonizer graph.
   */
  private static class LineStringAdder
      implements GeometryComponentFilter
  {
    Polygonizer p;
    
    LineStringAdder(Polygonizer p) {
      this.p = p;
    }
    
    public void filter(Geometry g) {
      if (g instanceof LineString)
        p.add((LineString) g);
    }
  }

  // default factory
  private LineStringAdder lineStringAdder = new LineStringAdder(this);

  protected PolygonizeGraph graph;
  // initialize with empty collections, in case nothing is computed
  protected Collection<LineString> dangles = new ArrayList<LineString>();
  protected List<LineString> cutEdges = new ArrayList<LineString>();
  protected List<LineString> invalidRingLines = new ArrayList<LineString>();

  protected List<EdgeRing> holeList = null;
  protected List<EdgeRing> shellList = null;
  protected List<Polygon> polyList = null;

  private boolean isCheckingRingsValid = true;
  private boolean extractOnlyPolygonal;

  private GeometryFactory geomFactory = null;

  /**
   * Creates a polygonizer that extracts all polygons.
   */
  public Polygonizer()
  {
    this(false);
  }
  
  /**
   * Creates a polygonizer, specifying whether a valid polygonal geometry must be created.
   * If the argument is <code>true</code>
   * then areas may be discarded in order to 
   * ensure that the extracted geometry is a valid polygonal geometry.
   * 
   * @param extractOnlyPolygonal true if a valid polygonal geometry should be extracted
   */
  public Polygonizer(boolean extractOnlyPolygonal)
  {
    this.extractOnlyPolygonal = extractOnlyPolygonal;
  }

  /**
   * Adds a collection of geometries to the edges to be polygonized.
   * May be called multiple times.
   * Any dimension of Geometry may be added;
   * the constituent linework will be extracted and used.
   *
   * @param geomList a list of {@link Geometry}s with linework to be polygonized
   */
  public void add(Collection geomList)
  {
    for (Iterator i = geomList.iterator(); i.hasNext(); ) {
      Geometry geometry = (Geometry) i.next();
      add(geometry);
    }
  }

  /**
   * Add a {@link Geometry} to the edges to be polygonized.
   * May be called multiple times.
   * Any dimension of Geometry may be added;
   * the constituent linework will be extracted and used
   *
   * @param g a {@link Geometry} with linework to be polygonized
   */
  public void add(Geometry g)
  {
    g.apply(lineStringAdder);
  }

  /**
   * Adds a linestring to the graph of polygon edges.
   *
   * @param line the {@link LineString} to add
   */
  private void add(LineString line)
  {
    // record the geometry factory for later use
    geomFactory  = line.getFactory();
    // create a new graph using the factory from the input Geometry
    if (graph == null)
      graph = new PolygonizeGraph(geomFactory);
    graph.addEdge(line);
  }

  /**
   * Allows disabling the valid ring checking, 
   * to optimize situations where invalid rings are not expected.
   * <p>
   * The default is <code>true</code>.
   * 
   * @param isCheckingRingsValid true if generated rings should be checked for validity
   */
  public void setCheckRingsValid(boolean isCheckingRingsValid)
  {
    this.isCheckingRingsValid = isCheckingRingsValid;
  }
  
  /**
   * Gets the list of polygons formed by the polygonization.
   * @return a collection of {@link Polygon}s
   */
  public Collection getPolygons()
  {
    polygonize();
    return polyList;
  }

  /**
   * Gets a geometry representing the polygons formed by the polygonization.
   * If a valid polygonal geometry was extracted the result is a {@link org.locationtech.jts.geom.Polygonal} geometry.
   * 
   * @return a geometry containing the polygons
   */
  public Geometry getGeometry()
  {
    if (geomFactory == null) geomFactory = new GeometryFactory();
    polygonize();
    if (extractOnlyPolygonal) {
      return geomFactory.buildGeometry(polyList);
    }
    // result may not be valid Polygonal, so return as a GeometryCollection
    return geomFactory.createGeometryCollection(GeometryFactory.toGeometryArray(polyList));
  }

  /**
   * Gets the list of dangling lines found during polygonization.
   * @return a collection of the input {@link LineString}s which are dangles
   */
  public Collection getDangles()
  {
    polygonize();
    return dangles;
  }

  /**
   * Gets the list of cut edges found during polygonization.
   * @return a collection of the input {@link LineString}s which are cut edges
   */
  public Collection getCutEdges()
  {
    polygonize();
    return cutEdges;
  }

  /**
   * Gets the list of lines forming invalid rings found during polygonization.
   * @return a collection of the input {@link LineString}s which form invalid rings
   */
  public Collection getInvalidRingLines()
  {
    polygonize();
    return invalidRingLines;
  }

  /**
   * Performs the polygonization, if it has not already been carried out.
   */
  private void polygonize()
  {
    // check if already computed
    if (polyList != null) return;
    polyList = new ArrayList<Polygon>();

    // if no geometries were supplied it's possible that graph is null
    if (graph == null) return;

    dangles = graph.deleteDangles();
    cutEdges = graph.deleteCutEdges();
    List<EdgeRing> edgeRingList = graph.getEdgeRings();

    //Debug.printTime("Build Edge Rings");

    List<EdgeRing> validEdgeRingList = new ArrayList<EdgeRing>();
    List<EdgeRing> invalidRings = new ArrayList<EdgeRing>();
    if (isCheckingRingsValid) {
      findValidRings(edgeRingList, validEdgeRingList, invalidRings);
      invalidRingLines = extractInvalidLines(invalidRings);
    }
    else {
      validEdgeRingList = edgeRingList;
    }
    //Debug.printTime("Validate Rings");
    
    findShellsAndHoles(validEdgeRingList);
    HoleAssigner.assignHolesToShells(holeList, shellList);
    
    // order the shells to make any subsequent processing deterministic
    Collections.sort(shellList, new EdgeRing.EnvelopeComparator());

    //Debug.printTime("Assign Holes");
    
    boolean includeAll = true;
    if (extractOnlyPolygonal) {
      findDisjointShells(shellList);
      includeAll = false;
    }
    polyList = extractPolygons(shellList, includeAll);
  }


  private void findValidRings(List<EdgeRing> edgeRingList, List<EdgeRing> validEdgeRingList, List<EdgeRing> invalidRingList)
  {
    for (EdgeRing er : edgeRingList) {
      er.computeValid();
      if (er.isValid())
        validEdgeRingList.add(er);
      else
        invalidRingList.add(er);
    }
  }

  private void findShellsAndHoles(List<EdgeRing> edgeRingList)
  {
    holeList = new ArrayList<EdgeRing>();
    shellList = new ArrayList<EdgeRing>();
    for (EdgeRing er : edgeRingList) {
      er.computeHole();
      if (er.isHole())
        holeList.add(er);
      else
        shellList.add(er);
    }
  }

  private static void findDisjointShells(List<EdgeRing> shellList) {
    findOuterShells(shellList);
    
    boolean isMoreToScan;
    int lastUnsetNumber = -1;
    do {
      isMoreToScan = false;
      int unsetNumber = 0;
      for (EdgeRing er : shellList) {
        if (er.isIncludedSet()) 
          continue;
        unsetNumber++;
        er.updateIncluded();
        if (! er.isIncludedSet()) {
          isMoreToScan = true;
        }
      }
      if (unsetNumber > 0 && unsetNumber == lastUnsetNumber) {
        throw new IllegalStateException("Failed to polygonize with extractOnlyPolygonal option : input may not be correctly noded");
      }
      lastUnsetNumber = unsetNumber;
    } while (isMoreToScan);
  }

  /**
   * For each outer hole finds and includes a single outer shell.
   * This seeds the traversal algorithm for finding only polygonal shells.
   *  
   * @param shellList the list of shell EdgeRings
   */
  private static void findOuterShells(List<EdgeRing> shellList) {

    for (EdgeRing er : shellList) {
      EdgeRing outerHoleER = er.getOuterHole();
      if (outerHoleER != null && ! outerHoleER.isProcessed()) {
        er.setIncluded(true);
        outerHoleER.setProcessed(true);
      }
    }
  }
  
  /**
   * Extracts unique lines for invalid rings, 
   * discarding rings which correspond to outer rings and hence contain
   * duplicate linework.
   * 
   * @param invalidRings
   * @return
   */
  private List<LineString> extractInvalidLines(List<EdgeRing> invalidRings) {
    /**
     * Sort rings by increasing envelope area.
     * This causes inner rings to be processed before the outer rings
     * containing them, which allows outer invalid rings to be discarded
     * since their linework is already reported in the inner rings.
     */
    Collections.sort(invalidRings, new EdgeRing.EnvelopeAreaComparator());
    /**
     * Scan through rings.  Keep only rings which have an adjacent EdgeRing
     * which is either valid or marked as not processed.  
     * This avoids including outer rings which have linework which is duplicated.
     */
    List<LineString> invalidLines = new ArrayList<LineString>();
    for (EdgeRing er : invalidRings) {
      if (isIncludedInvalid(er)) {
        invalidLines.add(er.getLineString());
      }
      er.setProcessed(true);
    }
    return invalidLines;
  }

  /**
   * Tests if a invalid ring should be included in
   * the list of reported invalid rings.
   * 
   * Rings are included only if they contain 
   * linework which is not already in a valid ring,
   * or in an already-included ring.
   * 
   * Because the invalid rings list is sorted by extent area,
   * this results in outer rings being discarded, 
   * since all their linework is reported in the rings they contain.
   * 
   * @param invalidRing the ring to test
   * @return true if the ring should be included
   */
  private boolean isIncludedInvalid(EdgeRing invalidRing) {
    for (PolygonizeDirectedEdge de : invalidRing.getEdges()) {
      PolygonizeDirectedEdge deAdj = (PolygonizeDirectedEdge) de.getSym();
      EdgeRing erAdj = deAdj.getRing();
      /**
       * 
       */
      boolean isEdgeIncluded = erAdj.isValid() || erAdj.isProcessed();
      if ( ! isEdgeIncluded) 
        return true;
    }
    return false;
  }

  private static List<Polygon> extractPolygons(List<EdgeRing> shellList, boolean includeAll) {
    List<Polygon> polyList = new ArrayList<Polygon>();
    for (EdgeRing er : shellList) {
      if (includeAll || er.isIncluded()) {
        polyList.add(er.getPolygon());
      }
    }
    return polyList;
  }
}
