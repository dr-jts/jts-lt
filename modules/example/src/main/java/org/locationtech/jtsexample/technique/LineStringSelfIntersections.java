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

package org.locationtech.jtsexample.technique;

import java.util.*;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKTReader;

/**
 * Shows a technique for identifying the location of self-intersections
 * in a non-simple LineString.
 *
 * @version 1.7
 */

public class LineStringSelfIntersections {

  public static void main(String[] args)
      throws Exception
  {
    WKTReader rdr = new WKTReader();

    LineString line1 = (LineString) (rdr.read("LINESTRING (0 0, 10 10, 20 20)"));
    showSelfIntersections(line1);
    LineString line2 = (LineString) (rdr.read("LINESTRING (0 40, 60 40, 60 0, 20 0, 20 60)"));
    showSelfIntersections(line2);

  }

  public static void showSelfIntersections(LineString line)
  {
    System.out.println("Line: " + line);
    System.out.println("Self Intersections: " + lineStringSelfIntersections(line));
  }

  public static Geometry lineStringSelfIntersections(LineString line)
  {
    Geometry lineEndPts = getEndPoints(line);
    Geometry nodedLine = line.union(lineEndPts);
    Geometry nodedEndPts = getEndPoints(nodedLine);
    Geometry selfIntersections = nodedEndPts.difference(lineEndPts);
    return selfIntersections;
  }

  public static Geometry getEndPoints(Geometry g)
  {
    List endPtList = new ArrayList();
    if (g instanceof LineString) {
      LineString line = (LineString) g;

      endPtList.add(line.getCoordinateN(0));
      endPtList.add(line.getCoordinateN(line.getNumPoints() - 1));
    }
    else if (g instanceof MultiLineString) {
      MultiLineString mls = (MultiLineString) g;
      for (int i = 0; i < mls.getNumGeometries(); i++) {
        LineString line = (LineString) mls.getGeometryN(i);
        endPtList.add(line.getCoordinateN(0));
        endPtList.add(line.getCoordinateN(line.getNumPoints() - 1));
      }
    }
    Coordinate[] endPts = CoordinateArrays.toCoordinateArray(endPtList);
    return (new GeometryFactory()).createMultiPoint(endPts);
  }


}