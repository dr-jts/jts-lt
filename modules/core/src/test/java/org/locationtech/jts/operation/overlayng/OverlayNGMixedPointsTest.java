package org.locationtech.jts.operation.overlayng;

import org.locationtech.jts.geom.Geometry;

import junit.textui.TestRunner;
import test.jts.GeometryTestCase;

public class OverlayNGMixedPointsTest extends GeometryTestCase {
  
  public static void main(String args[]) {
    TestRunner.run(OverlayNGMixedPointsTest.class);
  }

  public OverlayNGMixedPointsTest(String name) { super(name); }
  
  public void testSimpleLineIntersection() {
    Geometry a = read("LINESTRING (1 1, 9 1)");
    Geometry b = read("POINT (5 1)");
    Geometry expected = read("POINT (5 1)");
    checkEqual(expected, OverlayNGTest.intersection(a, b, 1));
  }
  public void testLinePointInOutIntersection() {
    Geometry a = read("LINESTRING (1 1, 9 1)");
    Geometry b = read("MULTIPOINT ((5 1), (15 1))");
    Geometry expected = read("POINT (5 1)");
    checkEqual(expected, OverlayNGTest.intersection(a, b, 1));
  }
  public void testSimpleLineUnion() {
    Geometry a = read("LINESTRING (1 1, 9 1)");
    Geometry b = read("POINT (5 1)");
    Geometry expected = read("LINESTRING (1 1, 9 1)");
    checkEqual(expected, OverlayNGTest.union(a, b, 1));
  }
  public void testSimpleLineDifference() {
    Geometry a = read("LINESTRING (1 1, 9 1)");
    Geometry b = read("POINT (5 1)");
    Geometry expected = read("LINESTRING (1 1, 9 1)");
    checkEqual(expected, OverlayNGTest.difference(a, b, 1));
  }
  public void testSimpleLineSymDifference() {
    Geometry a = read("LINESTRING (1 1, 9 1)");
    Geometry b = read("POINT (5 1)");
    Geometry expected = read("LINESTRING (1 1, 9 1)");
    checkEqual(expected, OverlayNGTest.symDifference(a, b, 1));
  }
  public void testLinePointSymDifference() {
    Geometry a = read("LINESTRING (1 1, 9 1)");
    Geometry b = read("POINT (15 1)");
    Geometry expected = read("GEOMETRYCOLLECTION (POINT (15 1), LINESTRING (1 1, 9 1))");
    checkEqual(expected, OverlayNGTest.symDifference(a, b, 1));
  }
  
  public void testPolygonInsideIntersection() {
    Geometry a = read("POLYGON ((4 2, 6 2, 6 0, 4 0, 4 2))");
    Geometry b = read("POINT (5 1)");
    Geometry expected = read("POINT (5 1)");
    checkEqual(expected, OverlayNGTest.intersection(a, b, 1));
  }
  public void testPolygonDisjointIntersection() {
    Geometry a = read("POLYGON ((4 2, 6 2, 6 0, 4 0, 4 2))");
    Geometry b = read("POINT (15 1)");
    Geometry expected = read("POINT EMPTY");
    checkEqual(expected, OverlayNGTest.intersection(a, b, 1));
  }

}
