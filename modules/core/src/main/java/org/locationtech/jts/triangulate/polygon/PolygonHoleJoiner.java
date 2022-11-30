/*
 * Copyright (c) 2021 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.triangulate.polygon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import org.locationtech.jts.algorithm.PolygonNodeTopology;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.noding.BasicSegmentString;
import org.locationtech.jts.noding.MCIndexSegmentSetMutualIntersector;
import org.locationtech.jts.noding.SegmentIntersectionDetector;
import org.locationtech.jts.noding.SegmentSetMutualIntersector;
import org.locationtech.jts.noding.SegmentString;
import org.locationtech.jts.noding.SegmentStringUtil;

/**
 * Transforms a polygon with holes into a single self-touching (invalid) ring
 * by joining holes to the exterior shell or to another hole. 
 * The holes are added from the lowest upwards. 
 * As the resulting shell develops, a hole might be added to what was
 * originally another hole.
 * <p>
 * There is no attempt to optimize the quality of the join lines.
 * In particular, a hole which already touches at a vertex may be
 * joined at a different vertex.
 */
public class PolygonHoleJoiner {
  
  public static Polygon joinAsPolygon(Polygon inputPolygon) {
    return inputPolygon.getFactory().createPolygon(join(inputPolygon));
  }
  
  public static Coordinate[] join(Polygon inputPolygon) {
    PolygonHoleJoiner joiner = new PolygonHoleJoiner(inputPolygon);
    return joiner.compute();
  }
  
  private List<Coordinate> shellCoords;
  // a sorted and searchable version of the shellCoords
  private TreeSet<Coordinate> shellCoordsSorted;
  // Key: starting end of the cut; Value: list of the other end of the cut
  private HashMap<Coordinate, ArrayList<Coordinate>> joinMap;
  private SegmentSetMutualIntersector polygonIntersector;

  private Polygon inputPolygon;

  public PolygonHoleJoiner(Polygon polygon) {
    this.inputPolygon = polygon;
  }

  /**
   * Computes the joined ring.
   * 
   * @return the points in the joined ring
   */
  public Coordinate[] compute() {
    Polygon polygon = node(inputPolygon);
    //--- copy the input polygon shell coords
    shellCoords = ringCoordinates(polygon.getExteriorRing());
    if (polygon.getNumInteriorRing() != 0) {
      joinHoles(polygon);
    }
    return shellCoords.toArray(new Coordinate[0]);
  }

  private Polygon node(Polygon polygon) {
    if (polygon.getNumInteriorRing() == 0) {
      return polygon;
    }
    //-- force polygon to be fully noded
    //TODO: do this faster!
    return (Polygon) polygon.union(polygon);
  }

  private static List<Coordinate> ringCoordinates(LinearRing ring) {
    Coordinate[] coords = ring.getCoordinates();
    List<Coordinate> coordList = new ArrayList<Coordinate>();
    for (Coordinate p : coords) {
      coordList.add(p);
    }
    return coordList;
  }
  
  private void joinHoles(Polygon polygon) {
    polygonIntersector = createPolygonIntersector(polygon);

    shellCoordsSorted = new TreeSet<Coordinate>();
    shellCoordsSorted.addAll(shellCoords);
    joinMap = new HashMap<Coordinate, ArrayList<Coordinate>>();
    List<LinearRing> orderedHoles = sortHoles(polygon);
    for (int i = 0; i < orderedHoles.size(); i++) {
      joinHole(orderedHoles.get(i));
    }
  }

  /**
   * Joins a single hole to the current shellRing.
   * 
   * 1) Get a list of the leftmost Hole Vertex indices. 
   * 2) Get a list of candidate joining shell vertices. 
   * 3) Get the pair that has the shortest distance between them. 
   * This pair is the endpoints of the cut 
   * 4) The selected ShellVertex may occurs multiple times in
   * shellCoords[], so find the proper one and add the hole after it.
   * 
   * @param hole the hole to join
   */
  private void joinHole(LinearRing hole) {
    final Coordinate[] holeCoords = hole.getCoordinates();
    
    //-- first check if hole is touching
    boolean isTouching = joinTouchingHole(holeCoords);
    if (isTouching)
      return;
    joinNonTouchingHole(hole, holeCoords);
  }
  
  private void joinNonTouchingHole(LinearRing hole, Coordinate[] holeCoords) {
    List<Integer> holeLeftVerticesIndex = findLeftVertices(hole);
    Coordinate holeLeftCoord = holeCoords[holeLeftVerticesIndex.get(0)];
    List<Coordinate> shellJoinCoords = findJoinableShellVertices(holeLeftCoord);
    
    int holeJoinIndex = 0;
    Coordinate shellJoinCoord = shellJoinCoords.get(0);
    /**
     * In the case of a vertical join line, there may be multiple
     * shell vertices with the same X value,
     * and some of the join lines may touch the polygon boundary. 
     * Find the shortest join pair to ensure the join line 
     * does not intersect the polygon boundary.
     */
    if (shellJoinCoord.x == holeLeftCoord.x) {
      double minLen = Double.MAX_VALUE;
      for (int i = 0; i < holeLeftVerticesIndex.size(); i++) {
        for (int j = 0; j < shellJoinCoords.size(); j++) {
          double currLen = Math.abs(shellJoinCoords.get(j).y - holeCoords[holeLeftVerticesIndex.get(i)].y);
          if ( currLen < minLen ) {
            minLen = currLen;
            holeJoinIndex = holeLeftVerticesIndex.get(i);
            shellJoinCoord = shellJoinCoords.get(j);
          }
        }
      }
    }
    
    Coordinate holeJoinCoord = holeCoords[holeJoinIndex];
    int shellJoinIndex = findShellJoinIndex(shellJoinCoord, holeJoinCoord);
    addJoin(shellJoinCoord, holeJoinCoord);
    addHoleToShell(shellJoinIndex, holeCoords, holeJoinIndex);
  }

  private boolean joinTouchingHole(Coordinate[] holeCoords) {
    //TODO: find fast way to identify touching holes (perhaps during initial noding?)
    int holeTouchIndex = findHoleTouchIndex(holeCoords);
    if (holeTouchIndex < 0)
      return false;
    int shellTouchIndex = findShellTouchIndex(holeCoords, holeTouchIndex);
    addHoleToShell(shellTouchIndex, holeCoords, holeTouchIndex);
    return true;
  }

  private int findShellTouchIndex(Coordinate[] holeCoords, int holeTouchIndex) {
    //-- linear scan is slow but only done once per hole
    Coordinate holeCoord = holeCoords[holeTouchIndex];
    for (int i = 0; i < shellCoords.size(); i++) {
      if (holeCoord.equals2D(shellCoords.get(i))) {
        if (isLineInterior(shellCoords, i, holeCoords, holeTouchIndex)) {
          return i;
        }
      }
    }
    return -1;
  }

  private boolean isLineInterior(List<Coordinate> shellCoords, int shellTouchindex, 
      Coordinate[] holeCoords, int holeTouchIndex) {
    Coordinate nodePt = shellCoords.get(shellTouchindex);
    Coordinate shell0 = shellCoords.get( prev(shellTouchindex, shellCoords.size()) );
    Coordinate shell1 = shellCoords.get( next(shellTouchindex, shellCoords.size()) );
    Coordinate hole0 = holeCoords[ prev(holeTouchIndex, holeCoords.length) ];
    return PolygonNodeTopology.isInteriorSegment(nodePt, shell0, shell1, hole0);
  }

  private static int prev(int i, int size) {
    int prev = i - 1;
    if (prev < 0)
      return size - 2;
    return prev;
  }

  private static int next(int i, int size) {
    int next = i + 1;
    if (next > size - 2)
      return 0;
    return next;
  }

  private int findHoleTouchIndex(Coordinate[] holeCoords) {
    for (int i = 0; i < holeCoords.length; i++) {
      if (shellCoordsSorted.contains(holeCoords[i])) 
        return i;
    }
    return -1;
  }

  /**
   * Gets the shell vertex index that the hole should join after.
   * 
   * @param shellVertex the shell vertex
   * @param holeVertex the hole vertex
   * @return the shell vertex index to join after
   */
  private int findShellJoinIndex(Coordinate shellVertex, Coordinate holeVertex) {
    int numSkip = 0;
    if ( joinMap.containsKey(shellVertex) ) {
      for (Coordinate coord : joinMap.get(shellVertex)) {
        if ( coord.y < holeVertex.y ) {
          numSkip++;
        }
      }
    } 
    return getShellCoordIndexSkip(shellVertex, numSkip);
  }  
  
  private void addJoin(Coordinate shellVertex, Coordinate holeVertex) {
    ArrayList<Coordinate> newValueList = new ArrayList<Coordinate>();
    newValueList.add(holeVertex);
    
    if ( joinMap.containsKey(shellVertex) ) {
      joinMap.get(shellVertex).add(holeVertex);
    } 
    else {
      joinMap.put(shellVertex, newValueList);
    }
    if (! joinMap.containsKey(holeVertex) ) {
      joinMap.put(holeVertex, new ArrayList<Coordinate>(newValueList));
    }
  }

  /**
   * Find the index of the coordinate in ShellCoords ArrayList,
   * skipping over some number of matches
   * 
   * @param coord
   * @return
   */
  private int getShellCoordIndexSkip(Coordinate coord, int numSkip) {
    for (int i = 0; i < shellCoords.size(); i++) {
      if ( shellCoords.get(i).equals2D(coord) ) {
        if ( numSkip == 0 )
          return i;
        numSkip--;
      }
    }
    throw new IllegalStateException("Vertex is not in shellcoords");
  }

  /**
   * Gets a list of shell vertices that could be used to join a hole.
   * This is a vertex or vertices in the half-place left of the hole vertex.
   * <p>
   * If the highest shell vertex in the plane is strictly left of the hole vertex
   * and the joining line does not cross the polygon boundary,
   * that single vertex is returned.
   * <p>
   * If there are shell vertices in the same vertical line as the hole vertex,
   * all vertices on that line are returned.
   * Join lines to some of them may touch the polygon boundary.
   * A subsequent search finds the shortest join line to one of them,
   * which will not intersect the boundary.
   * 
   * @param holeJoinCoord the hole coordinate to join to
   * @return a list of candidate join shell vertices
   */
  private List<Coordinate> findJoinableShellVertices(Coordinate holeJoinCoord) {
    ArrayList<Coordinate> list = new ArrayList<Coordinate>();
    double holeX = holeJoinCoord.x;
    //-- find highest shell vertex in half-plane left of hole pt
    Coordinate candidate = shellCoordsSorted.higher(holeJoinCoord);
    while (candidate.x == holeX) {
      candidate = shellCoordsSorted.higher(candidate);
    }
    //-- find rightmost joinable shell vertex
    do {
      candidate = shellCoordsSorted.lower(candidate);
    } while (crossesPolygon(holeJoinCoord, candidate) 
        && ! candidate.equals(shellCoordsSorted.first()));
    list.add(candidate);
    //-- if not same X as hole, return single vertex
    if ( candidate.x != holeX )
      return list;
    
    //-- include ALL vertices with same X as hole vertex
    while (candidate.x == holeX) {
      candidate = shellCoordsSorted.lower(candidate);
      if ( candidate == null || candidate.x != holeX )
        break;
      list.add(candidate);
    }
    return list;
  }
  
  /**
   * Tests whether a line segment crosses the polygon boundary.
   * 
   * @param p0 a vertex
   * @param p1 a vertex
   * @return true if the line segment crosses the polygon boundary
   */
  private boolean crossesPolygon(Coordinate p0, Coordinate p1) {
    SegmentString segString = new BasicSegmentString(
        new Coordinate[] { p0, p1 }, null);
    List<SegmentString> segStrings = new ArrayList<SegmentString>();
    segStrings.add(segString);
    
    SegmentIntersectionDetector segInt = new SegmentIntersectionDetector();
    segInt.setFindProper(true);
    polygonIntersector.process(segStrings, segInt);
    
    return segInt.hasProperIntersection();
  }
  
  /**
   * Add hole vertices at proper position in shell vertex list.
   * This code assumes that if hole touches (shell or other hole),
   * it touches at a node.  This requires an initial noding step.
   * In this case, the code avoids duplicating join vertices.
   * 
   * Also adds hole points to ordered coordinates.
   * 
   * @param shellJoinIndex index of join vertex in shell
   * @param holeCoords the vertices of the hole to be inserted
   * @param holeJoinIndex index of join vertex in hole
   */
  private void addHoleToShell(int shellJoinIndex, Coordinate[] holeCoords, int holeJoinIndex) {
    Coordinate shellJoinPt = shellCoords.get(shellJoinIndex);
    Coordinate holeJoinPt = holeCoords[holeJoinIndex];
    
    //-- check for touching (zero-length) join to avoid inserting duplicate vertices
    boolean isVertexTouch = shellJoinPt.equals2D(holeJoinPt);
    Coordinate addShellJoinPt = isVertexTouch ? null : shellJoinPt;

    //-- create new section of vertices to insert in shell
    List<Coordinate> newSection = createHoleSection(holeCoords, holeJoinIndex, addShellJoinPt);
    
    //-- add section after shell join vertex
    int shellAddIndex = shellJoinIndex + 1;
    shellCoords.addAll(shellAddIndex, newSection);
    shellCoordsSorted.addAll(newSection);
  }

  /**
   * Creates the new section of vertices for the added hole.
   * 
   * @param holeCoords
   * @param holeCutIndex
   * @param shellCutPt
   * @return
   */
  private List<Coordinate> createHoleSection(Coordinate[] holeCoords, int holeCutIndex, 
      Coordinate shellCutPt) {
    List<Coordinate> newSection = new ArrayList<Coordinate>();
    
    boolean isHoleDoesNotTouch = shellCutPt != null;
    /**
     * Add all hole vertices, including duplicate at cut vertex
     * Except, if hole DOES touch, cut vertex is already in shell ring
     */
    if (isHoleDoesNotTouch)
      newSection.add(holeCoords[holeCutIndex].copy());
    
    final int holeSize = holeCoords.length - 1;
    
    int index = holeCutIndex;
    for (int i = 0; i < holeSize; i++) {
      index = (index + 1) % holeSize;
      newSection.add(holeCoords[index].copy());
    }
    /**
     * Add duplicate shell vertex at end of the 2nd cut line.
     * Except, if hole DOES touch, cut line is zero-length so does not need end vertex
     */
    if (isHoleDoesNotTouch) { 
      newSection.add(shellCutPt.copy());
    }
    
    return newSection;
  }

  /**
   * Sort the hole rings by minimum X, minimum Y.
   * 
   * @param poly polygon that contains the holes
   * @return a list of sorted hole rings
   */
  private static List<LinearRing> sortHoles(final Polygon poly) {
    List<LinearRing> holes = new ArrayList<LinearRing>();
    for (int i = 0; i < poly.getNumInteriorRing(); i++) {
      holes.add(poly.getInteriorRingN(i));
    }
    Collections.sort(holes, new EnvelopeComparator());
    return holes;
  }

  /**
   * Gets a list of indices of the leftmost vertices in a ring.
   * 
   * @param geom the hole ring
   * @return indices of the leftmost vertices
   */
  private static List<Integer> findLeftVertices(LinearRing ring) {
    Coordinate[] coords = ring.getCoordinates();
    ArrayList<Integer> leftmostIndex = new ArrayList<Integer>();
    double leftX = ring.getEnvelopeInternal().getMinX();
    for (int i = 0; i < coords.length - 1; i++) {
      if ( coords[i].x == leftX ) {
        leftmostIndex.add(i);
      }
    }
    return leftmostIndex;
  }
    
  private static SegmentSetMutualIntersector createPolygonIntersector(Polygon polygon) {
    @SuppressWarnings("unchecked")
    List<SegmentString> polySegStrings = SegmentStringUtil.extractSegmentStrings(polygon);
    return new MCIndexSegmentSetMutualIntersector(polySegStrings);
  }
  
  private static class EnvelopeComparator implements Comparator<Geometry> {
    public int compare(Geometry g1, Geometry g2) {
      Envelope e1 = g1.getEnvelopeInternal();
      Envelope e2 = g2.getEnvelopeInternal();
      return e1.compareTo(e2);
    }
  }
      
}
