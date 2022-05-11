/*
 * Copyright (c) 2022 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.algorithm.hull;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.overlayng.CoverageUnion;
import org.locationtech.jts.triangulate.polygon.ConstrainedDelaunayTriangulator;
import org.locationtech.jts.triangulate.tri.Tri;

public class ConcaveHullOfPolygons {
  
  private static final int FRAME_EXPAND_FACTOR = 4;

  public static Geometry concaveHullByLength(Geometry constraints, double maxLength) {
    return concaveHullByLength(constraints, maxLength, false, false);
  }
  
  public static Geometry concaveHullByLength(Geometry constraints, double maxLength,
      boolean isTight, boolean isHolesAllowed) {
    ConcaveHullOfPolygons hull = new ConcaveHullOfPolygons(constraints);
    hull.setMaximumEdgeLength(maxLength);
    hull.setHolesAllowed(isHolesAllowed);
    hull.setTight(isTight);
    return hull.getHull();
  }
  
  public static Geometry concaveHullByLengthRatio(Geometry constraints, double lengthRatio) {
    return concaveHullByLengthRatio(constraints, lengthRatio, false, false);
  }
  
  public static Geometry concaveHullByLengthRatio(Geometry constraints, double lengthRatio,
      boolean isTight, boolean isHolesAllowed) {
    ConcaveHullOfPolygons hull = new ConcaveHullOfPolygons(constraints);
    hull.setMaximumEdgeLengthRatio(lengthRatio);
    hull.setHolesAllowed(isHolesAllowed);
    hull.setTight(isTight);
    return hull.getHull();
  }
  
  
  private Geometry inputPolygons;
  private double maxEdgeLength = -1;
  private double maxEdgeLengthRatio = -1;
  private boolean isHolesAllowed = false;
  private boolean isTight = false;
  
  private GeometryFactory geomFactory;
  private LinearRing[] polygonRings;
  
  private Set<Tri> hullTris;
  private ArrayDeque<Tri> borderTriQue;
  /**
   * Records the border edge of border tris,
   * so it can be tested for length and possible removal.
   */
  private Map<Tri, Integer> borderEdgeMap = new HashMap<Tri, Integer>();
  
  public ConcaveHullOfPolygons(Geometry polygons) {
    this.inputPolygons = polygons;
    geomFactory = inputPolygons.getFactory();
  }

  /**
   * Sets the target maximum edge length for the concave hull.
   * The length value must be zero or greater.
   * <ul>
   * <li>The value 0.0 produces the concave hull of smallest area
   * that is still connected.
   * <li>Larger values produce less concave results.
   * A value equal or greater than the longest Delaunay Triangulation edge length
   * produces the convex hull.
   * </ul>
   * The {@link #uniformGridEdgeLength(Geometry)} value may be used as
   * the basis for estimating an appropriate target maximum edge length.
   * 
   * @param edgeLength a non-negative length
   * 
   * @see #uniformGridEdgeLength(Geometry)
   */
  public void setMaximumEdgeLength(double edgeLength) {
    if (edgeLength < 0)
      throw new IllegalArgumentException("Edge length must be non-negative");
    this.maxEdgeLength = edgeLength;
    maxEdgeLengthRatio = -1;
  }
  
  /**
   * Sets the target maximum edge length ratio for the concave hull.
   * The edge length ratio is a fraction of the difference
   * between the longest and shortest edge lengths 
   * in the Delaunay Triangulation of the input points.
   * It is a value in the range 0 to 1. 
   * <ul>
   * <li>The value 0.0 produces a concave hull of minimum area
   * that is still connected.
   * <li>The value 1.0 produces the convex hull.
   * <ul> 
   * 
   * @param edgeLengthRatio a length factor value between 0 and 1
   */
  public void setMaximumEdgeLengthRatio(double edgeLengthRatio) {
    if (edgeLengthRatio < 0 || edgeLengthRatio > 1)
      throw new IllegalArgumentException("Edge length ratio must be in range [0,1]");
    this.maxEdgeLengthRatio = edgeLengthRatio;
  }
  
  /**
   * Sets whether holes are allowed in the concave hull polygon.
   * 
   * @param isHolesAllowed true if holes are allowed in the result
   */
  public void setHolesAllowed(boolean isHolesAllowed) {
    this.isHolesAllowed = isHolesAllowed;
  }
  
  /**
   * Sets whether the boundary of the hull polygon is kept
   * tight to the outer edges of the input polygons.
   * 
   * @param isTightBoundary true if the boundary is kept tight
   */
  public void setTight(boolean isTight) {
    this.isTight = isTight;
  }
  
  public Geometry getHull() {
    polygonRings = extractShellRings(inputPolygons);
    Polygon frame = createFrame(inputPolygons.getEnvelopeInternal(), polygonRings, geomFactory);
    ConstrainedDelaunayTriangulator cdt = new ConstrainedDelaunayTriangulator(frame);
    List<Tri> tris = cdt.getTriangles();
    
    Coordinate[] framePts = frame.getExteriorRing().getCoordinates();
    if (maxEdgeLengthRatio >= 0) {
      maxEdgeLength = computeTargetEdgeLength(tris, framePts, maxEdgeLengthRatio);
    }
    
     removeFrameCornerTris(tris, framePts);
    
    removeBorderTris();
    if (isHolesAllowed) removeHoleTris();
    
    Geometry hull = buildHullPolygon(hullTris);
    return hull;
  }

  private static double computeTargetEdgeLength(List<Tri> triList, 
      Coordinate[] frameCorners,
      double edgeLengthRatio) {
    if (edgeLengthRatio == 0) return 0;
    double maxEdgeLen = -1;
    double minEdgeLen = -1;
    for (Tri tri : triList) {
      //-- don't include frame triangles
      if (isFrameTri(tri, frameCorners))
        continue;
      
      for (int i = 0; i < 3; i++) {
        //-- constraint edges are not used to determine ratio
        if (! tri.hasAdjacent(i))
          continue;
        
        double len = tri.getLength(i);
        if (len > maxEdgeLen) 
          maxEdgeLen = len;
        if (minEdgeLen < 0 || len < minEdgeLen)
          minEdgeLen = len;
      }
    }
    //-- if ratio = 1 ensure all edges are included
    if (edgeLengthRatio == 1) 
      return 2 * maxEdgeLen;
    
    return edgeLengthRatio * (maxEdgeLen - minEdgeLen) + minEdgeLen;
  }

  private static boolean isFrameTri(Tri tri, Coordinate[] frameCorners) {
    int index = vertexIndex(tri, frameCorners);
    boolean isFrameTri = index >= 0;
    return isFrameTri;
  }
  
  private void removeFrameCornerTris(List<Tri> tris, Coordinate[] frameCorners) {
    hullTris = new HashSet<Tri>();
    borderTriQue = new ArrayDeque<Tri>();
    for (Tri tri : tris) {
      int index = vertexIndex(tri, frameCorners);
      boolean isFrameTri = index >= 0;
      if (isFrameTri) {
        /**
         * Frame tris are adjacent to at most one border tri,
         * which is opposite the frame corner vertex.
         * The opposite tri may be another frame tri. 
         * This is detected when it is processed,
         * since it is not in the hullTri set.
         */
        int oppIndex = Tri.oppEdge(index);
        addBorderTri(tri, oppIndex);
        tri.remove();
      }
      else {
        hullTris.add(tri);
      }
    }
  }

  /**
   * Get the tri vertex index of some point in a list, 
   * or -1 if none are vertices.
   * 
   * @param tri the tri to test for containing a point
   * @param pts the points to test
   * @return the vertex index of a point, or -1
   */
  private static int vertexIndex(Tri tri, Coordinate[] pts) {
    for (Coordinate p : pts) {
      int index = tri.getIndex(p);
      if (index >= 0) 
        return index;
    }
    return -1;
  }
  
  private void removeBorderTris() {
    while (! borderTriQue.isEmpty()) {
      Tri tri = borderTriQue.pop();
      //-- tri might have been removed already
      if (! hullTris.contains(tri)) {
        continue;
      }
      if (isRemovable(tri)) {
        addBorderTris(tri);
        removeBorderTri(tri);
      }
    }
  }

  private void removeHoleTris() {
    while (true) {
      Tri holeTri = findHoleTri(hullTris);
      if (holeTri == null)
        return;
      addBorderTris(holeTri);
      removeBorderTri(holeTri);
      removeBorderTris();
    }
  }
  
  private Tri findHoleTri(Set<Tri> tris) {
    for (Tri tri : tris) {
      if (isHoleTri(tri))
        return tri;
    }
    return null;
  }

  private boolean isHoleTri(Tri tri) {
    for (int i = 0; i < 3; i++) {
      if (tri.hasAdjacent(i)
          && tri.getLength(i) > maxEdgeLength)
         return true;
    }
    return false;
  }

  private boolean isRemovable(Tri tri) {
    //-- remove non-bridging tris if keeping hull boundary tight
    if (isTight && isTouchingSinglePolygon(tri))
      return true;
    
    //-- check if outside edge is longer than threshold
    if (borderEdgeMap.containsKey(tri)) {
      int borderEdgeIndex = borderEdgeMap.get(tri);
      double edgeLen = tri.getLength(borderEdgeIndex);
      if (edgeLen > maxEdgeLength)
        return true;
    }
    return false;
  }

  /**
   * Tests whether a triangle touches a single polygon at all vertices.
   * If so, it is a candidate for removal if the hull polygon
   * is being kept tight to the outer boundary of the input polygons.
   * Tris which touch more than one polygon are called "bridging".
   * 
   * @param tri
   * @return true if the tri touches a single polygon
   */
  private boolean isTouchingSinglePolygon(Tri tri) {
    Envelope envTri = envelope(tri);
    for (LinearRing ring : polygonRings) {
      //-- optimization heuristic: a touching tri must be in ring envelope
      if (ring.getEnvelopeInternal().intersects(envTri)) {
        if (hasAllVertices(ring, tri))
          return true;
      }
    }
    return false;
  }

  private void addBorderTris(Tri tri) {
    addBorderTri(tri, 0);
    addBorderTri(tri, 1);
    addBorderTri(tri, 2);
  }
  
  /**
   * Adds an adjacent tri to the current border.
   * The adjacent edge is recorded as the border edge for the tri.
   * Note that only edges adjacent to another tri can become border edges.
   * Since constraint-adjacent edges do not have an adjacent tri,
   * they can never be on the border and thus will not be removed
   * due to being shorter than the length threshold.
   * The tri containing them may still be removed via another edge, however. 
   * 
   * @param tri the tri adjacent to the tri to be added to the border
   * @param index the index of the adjacent tri
   */
  private void addBorderTri(Tri tri, int index) {
    Tri adj = tri.getAdjacent( index );
    if (adj == null) 
      return;
    borderTriQue.add(adj);
    int borderEdgeIndex = adj.getIndex(tri);
    borderEdgeMap.put(adj, borderEdgeIndex);
  }

  private void removeBorderTri(Tri tri) {
    tri.remove();
    hullTris.remove(tri);
    borderEdgeMap.remove(tri);
  }
  
  private static boolean hasAllVertices(LinearRing ring, Tri tri) {
    for (int i = 0; i < 3; i++) {
      Coordinate v = tri.getCoordinate(i);
      if (! hasVertex(ring, v)) {
        return false;
      }
    }
    return true;
  }
  
  private static boolean hasVertex(LinearRing ring, Coordinate v) {
    for(int i = 1; i < ring.getNumPoints(); i++) {
      if (v.equals2D(ring.getCoordinateN(i))) {
        return true;
      }
    }
    return false;
  }

  private static Envelope envelope(Tri tri) {
    Envelope env = new Envelope(tri.getCoordinate(0), tri.getCoordinate(1));
    env.expandToInclude(tri.getCoordinate(2));
    return env;
  }

  private Geometry buildHullPolygon(Set<Tri> hullTris) {
    //-- union triangulation
    Geometry triCoverage = Tri.toGeometry(hullTris, geomFactory);
    Geometry filler = CoverageUnion.union(triCoverage);
    
    if (filler.isEmpty()) {
      return inputPolygons.copy();
    }
    //-- union with input polygons
    Geometry[] geoms = new Geometry[] { filler, inputPolygons };
    GeometryCollection geomColl = geomFactory.createGeometryCollection(geoms);
    Geometry hull = CoverageUnion.union(geomColl);
    return hull;
  }
  
  /**
   * Creates a rectangular "frame" around the input polygons,
   * with the input polygons as holes in it.
   * The frame corners are far enough away that the constrained Delaunay triangulation
   * of it should contain the convex hull of the input as edges.
   * Thus the frame corner triangles can be removed and leave a correct 
   * triangulation of the space between the input polygons.
   * 
   * @param polygonsEnv
   * @param polygonRings
   * @param geomFactory 
   * @return the frame polygon
   */
  private static Polygon createFrame(Envelope polygonsEnv, LinearRing[] polygonRings, GeometryFactory geomFactory) {
    double diam = polygonsEnv.getDiameter();
    Envelope envFrame = polygonsEnv.copy();
    envFrame.expandBy(FRAME_EXPAND_FACTOR * diam);
    Polygon frameOuter = (Polygon) geomFactory.toGeometry(envFrame);
    LinearRing shell = (LinearRing) frameOuter.getExteriorRing().copy();
    Polygon frame = geomFactory.createPolygon(shell, polygonRings);
    return frame;
  }

  private static LinearRing[] extractShellRings(Geometry polygons) {
    LinearRing[] rings = new LinearRing[polygons.getNumGeometries()];
    for (int i = 0; i < polygons.getNumGeometries(); i++) {
      Polygon consPoly = (Polygon) polygons.getGeometryN(i);
      rings[i] = (LinearRing) consPoly.getExteriorRing().copy();
    }
    return rings;
  }
}
