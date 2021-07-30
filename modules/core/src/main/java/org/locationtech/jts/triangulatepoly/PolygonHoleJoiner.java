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
package org.locationtech.jts.triangulatepoly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

/**
 * Transforms a polygon into a single self-touching ring
 * by connecting holes to the exterior shell, 
 * The holes are added from the lowest upwards. 
 * As the resulting shell develops, a hole might be added to what was
 * originally another hole.
 */
public class PolygonHoleJoiner {
  
  public static Polygon joinGeometry(Polygon inputPolygon) {
    return inputPolygon.getFactory().createPolygon(joinPoints(inputPolygon));
  }
  
  public static Coordinate[] joinPoints(Polygon inputPolygon) {
    PolygonHoleJoiner joiner = new PolygonHoleJoiner(inputPolygon);
    return joiner.compute();
  }
  
  private static final double EPS = 1.0E-4;
  
  private final GeometryFactory geomFact;
  private PreparedGeometry inputPrepGeom;
  private List<Coordinate> shellCoords;
  // orderedCoords a copy of shellCoords for sort purpose
  private TreeSet<Coordinate> orderedCoords;
  // Key: starting end of the cut; Value: list of the other end of the cut
  private HashMap<Coordinate, ArrayList<Coordinate>> cutMap;

  private Polygon inputPolygon;

  public PolygonHoleJoiner(Polygon inputPolygon) {
    this.inputPolygon = inputPolygon;
    geomFact = inputPolygon.getFactory();
  }

  /**
   * Computes the joined ring.
   * 
   * @return the points in the joined ring
   */
  public Coordinate[] compute() {
    //--- copy the input polygon shell coords
    shellCoords = ringCoordinates(inputPolygon.getExteriorRing());
    if ( inputPolygon.getNumInteriorRing() != 0 ) {
      PreparedGeometryFactory pgFact = new PreparedGeometryFactory();
      inputPrepGeom = pgFact.create(inputPolygon);
      joinHoles();
    }
    return shellCoords.toArray(new Coordinate[0]);
  }

  private static List<Coordinate> ringCoordinates(LinearRing ring) {
    Coordinate[] coords = ring.getCoordinates();
    List<Coordinate> coordList = new ArrayList<Coordinate>();
    for (Coordinate p : coords) {
      coordList.add(p);
    }
    return coordList;
  }
  
  /**
   * @param shellCoords Shell Coordinates of the polygon.
   */
  private void joinHoles() {
    orderedCoords = new TreeSet<Coordinate>();
    orderedCoords.addAll(shellCoords);
    cutMap = new HashMap<Coordinate, ArrayList<Coordinate>>();
    List<Geometry> orderedHoles = getOrderedHoles(inputPolygon);
    for (int i = 0; i < orderedHoles.size(); i++) {
      joinHole(orderedHoles.get(i));
    }
  }

  /**
   * 1) Get a list of HoleVertex Index. 
   * 2) Get a list of ShellVertex. 
   * 3) Get the
   * pair that outputs the shortest distance between. This pair is the two ending
   * points of the cut 
   * 4) selected ShellVertex may occurs multiple times in
   * shellCoords[], find the proper one and add the hole behind.
   * 
   * @param hole
   */
  private void joinHole(Geometry hole) {
    final Coordinate[] holeCoords = hole.getCoordinates();
    ArrayList<Integer> holeLeftVerticesIndex = getLeftMostVertex(hole);
    Coordinate holeCoord = holeCoords[holeLeftVerticesIndex.get(0)];
    ArrayList<Coordinate> shellCoordsList = getLeftShellVertex(holeCoord);
    Coordinate shellCoord = shellCoordsList.get(0);
    int shortestHoleVertexIndex = 0;
    // pick the shellvertex holevertex pair that gives the shortest
    // distance
    if ( Math.abs(shellCoord.x - holeCoord.x) < EPS ) {
      double shortest = Double.MAX_VALUE;
      for (int i = 0; i < holeLeftVerticesIndex.size(); i++) {
        for (int j = 0; j < shellCoordsList.size(); j++) {
          double currLength = Math.abs(shellCoordsList.get(j).y - holeCoords[holeLeftVerticesIndex.get(i)].y);
          if ( currLength < shortest ) {
            shortest = currLength;
            shortestHoleVertexIndex = i;
            shellCoord = shellCoordsList.get(j);
          }
        }
      }
    }
    int shellVertexIndex = getShellCoordIndex(shellCoord,
        holeCoords[holeLeftVerticesIndex.get(shortestHoleVertexIndex)]);
    joinHole(shellVertexIndex, holeCoords, holeLeftVerticesIndex.get(shortestHoleVertexIndex));
  }

  /**
   * Get the ith shellvertex in shellCoords[] that the current should add after
   * 
   * @param shellVertex Coordinate of the shell vertex
   * @param holeVertex  Coordinate of the hole vertex
   * @return the ith shellvertex
   */
  private int getShellCoordIndex(Coordinate shellVertex, Coordinate holeVertex) {
    int numSkip = 0;
    ArrayList<Coordinate> newValueList = new ArrayList<Coordinate>();
    newValueList.add(holeVertex);
    if ( cutMap.containsKey(shellVertex) ) {
      for (Coordinate coord : cutMap.get(shellVertex)) {
        if ( coord.y < holeVertex.y ) {
          numSkip++;
        }
      }
      cutMap.get(shellVertex).add(holeVertex);
    } else {
      cutMap.put(shellVertex, newValueList);
    }
    if ( !cutMap.containsKey(holeVertex) ) {
      cutMap.put(holeVertex, new ArrayList<Coordinate>(newValueList));
    }
    return getShellCoordIndexSkip(shellVertex, numSkip);
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
      if ( shellCoords.get(i).equals2D(coord, EPS) ) {
        if ( numSkip == 0 )
          return i;
        numSkip--;
      }
    }
    throw new IllegalStateException("Vertex is not in shellcoords");
  }

  /**
   * Return a list of shell vertices that could be used to join with holeCoord.
   * This list contains only one item if the chosen vertex does not share the same
   * x value with holeCoord
   * 
   * @param holeCoord
   * @return
   */
  private ArrayList<Coordinate> getLeftShellVertex(Coordinate holeCoord) {
    ArrayList<Coordinate> list = new ArrayList<Coordinate>();
    Coordinate closest = orderedCoords.higher(holeCoord);
    while (closest.x == holeCoord.x) {
      closest = orderedCoords.higher(closest);
    }
    do {
      closest = orderedCoords.lower(closest);
    } while (!isJoinable(holeCoord, closest) && !closest.equals(orderedCoords.first()));
    list.add(closest);
    if ( closest.x != holeCoord.x )
      return list;
    double chosenX = closest.x;
    list.clear();
    while (chosenX == closest.x) {
      list.add(closest);
      closest = orderedCoords.lower(closest);
      if ( closest == null )
        return list;
    }
    return list;
  }

  /**
   * Determine if a linestring between two coordinates is covered
   * 
   * @param holeCoord
   * @param shellCoord
   * @return
   */
  private boolean isJoinable(Coordinate holeCoord, Coordinate shellCoord) {
    LineString join = geomFact.createLineString(new Coordinate[] { holeCoord, shellCoord });
    if ( inputPrepGeom.covers(join) ) {
      return true;
    }
    return false;
  }

  /**
   * Add holeCoords to proper position. update ShellCoords and OrderedCoords
   * 
   * @param shellVertexIndex
   * @param holeCoords
   * @param holeVertexIndex
   */
  private void joinHole(int shellVertexIndex, Coordinate[] holeCoords, int holeVertexIndex) {
    List<Coordinate> newCoords = new ArrayList<Coordinate>();
    newCoords.add(new Coordinate(shellCoords.get(shellVertexIndex)));
    final int N = holeCoords.length - 1;
    int i = holeVertexIndex;
    do {
      newCoords.add(new Coordinate(holeCoords[i]));
      i = (i + 1) % N;
    } while (i != holeVertexIndex);
    newCoords.add(new Coordinate(holeCoords[holeVertexIndex]));
    shellCoords.addAll(shellVertexIndex, newCoords);
    orderedCoords.addAll(newCoords);
  }

  /**
   * Ordered the holes by left most vertex's x value. if same x, arrange in
   * top-down order
   * 
   * @param poly polygon that contains all the holes.
   * @return list of ordered hole geometry
   */
  private List<Geometry> getOrderedHoles(final Polygon poly) {
    List<Geometry> holes = new ArrayList<Geometry>();
    if ( poly.getNumInteriorRing() > 0 ) {
      for (int i = 0; i < poly.getNumInteriorRing(); i++) {
        holes.add(poly.getInteriorRingN(i));
      }
      Collections.sort(holes, new EnvelopeComparator());
    }
    return holes;
  }

  /**
   * Get a list of index of the leftmost vertex in hole
   * 
   * @param geom hole
   * @return index of the left most vertex
   */
  private ArrayList<Integer> getLeftMostVertex(Geometry geom) {
    Coordinate[] coords = geom.getCoordinates();
    ArrayList<Integer> list = new ArrayList<Integer>();
    double minX = geom.getEnvelopeInternal().getMinX();
    for (int i = 0; i < coords.length; i++) {
      if ( Math.abs(coords[i].x - minX) < EPS ) {
        list.add(i);
      }
    }
    return list;
  }

  private static class EnvelopeComparator implements Comparator<Geometry> {
    public int compare(Geometry o1, Geometry o2) {
      Envelope e1 = o1.getEnvelopeInternal();
      Envelope e2 = o2.getEnvelopeInternal();
      if ( e1.getMinX() < e2.getMinX() )
        return -1;
      if ( e1.getMinX() > e2.getMinX() )
        return 1;
      // if same x, place in top-down order
      if ( e1.getMinY() < e2.getMinY() )
        return 1;
      if ( e1.getMinY() > e2.getMinY() )
        return -1;
      return 0;
    }
  }
}
