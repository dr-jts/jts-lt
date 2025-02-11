package org.locationtech.jts.shape;

import junit.textui.TestRunner;
import org.locationtech.jts.geom.*;
import test.jts.GeometryTestCase;

public class CatmullRomSplineTest extends GeometryTestCase {

  public static void main(String[] args) {
    TestRunner.run(CatmullRomSplineTest.class);
  }

  public CatmullRomSplineTest(String name) {
    super(name);
  }

  public void testSimpleLineUniform() {
    checkCurve("LINESTRING(-1 0, -2 1, 2 1, 1 0)", 10, 0.0, 1,
      "LINESTRING (-1 0, -1.12 0.1, -1.28 0.22, -1.46 0.33, -1.64 0.45, -1.81 0.56, -1.96 0.67, -2.07 0.77, -2.12 0.86, -2.1 0.94, -2 1, -1.78 1.05, -1.44 1.08, -1.01 1.11, -0.52 1.12, 0 1.13, 0.52 1.12, 1.01 1.11, 1.44 1.08, 1.78 1.05, 2 1, 2.1 0.94, 2.12 0.86, 2.07 0.77, 1.96 0.67, 1.81 0.56, 1.64 0.45, 1.46 0.33, 1.28 0.22, 1.12 0.1, 1 0)");
  }

  public void testSimpleCentripetal() {
    checkCurve("LINESTRING(-1 0, -2 1, 2 1, 1 0)", 10, 0.5, 1,
      "LINESTRING (-1 0, -1.11 0.1, -1.24 0.21, -1.38 0.32, -1.52 0.44, -1.66 0.55, -1.78 0.65, -1.89 0.75, -1.96 0.85, -2 0.93, -2 1, -1.86 1.09, -1.54 1.17, -1.1 1.22, -0.57 1.25, 0 1.26, 0.57 1.25, 1.1 1.22, 1.54 1.17, 1.86 1.09, 2 1, 2 0.93, 1.96 0.85, 1.89 0.75, 1.78 0.65, 1.66 0.55, 1.52 0.44, 1.38 0.32, 1.24 0.21, 1.11 0.1, 1 0)");
  }

  public void testSimpleLineChordal() {
    checkCurve("LINESTRING(-1 0, -2 1, 2 1, 1 0)", 10, 1.0, 1,
      "LINESTRING (-1 0, -1.11 0.1, -1.22 0.21, -1.34 0.32, -1.46 0.43, -1.58 0.53, -1.69 0.64, -1.79 0.74, -1.88 0.83, -1.95 0.92, -2 1, -1.96 1.19, -1.68 1.33, -1.22 1.44, -0.64 1.5, 0 1.52, 0.64 1.5, 1.22 1.44, 1.68 1.33, 1.96 1.19, 2 1, 1.95 0.92, 1.88 0.83, 1.79 0.74, 1.69 0.64, 1.58 0.53, 1.46 0.43, 1.34 0.32, 1.22 0.21, 1.11 0.1, 1 0)");
  }

  public void testPolygonWithHoles() {
    checkCurve("POLYGON ((0 0, 0 10, 6 10, 6 0, 0 0), (3 4, 1 8, 3 9, 5 8, 3 4), (3 1, 2 2, 3 3, 4 2, 3 1))", 10, 0.5, 1,
      "POLYGON ((-0.39 0.59, -0.7 1.46, -0.92 2.53, -1.05 3.73, -1.09 5, -1.05 6.27, -0.92 7.47, -0.7 8.54, -0.39 9.41, 0 10, 0.41 10.3, 0.95 10.54, 1.58 10.71, 2.27 10.81, 3 10.85, 3.73 10.81, 4.42 10.71, 5.05 10.54, 5.59 10.3, 6 10, 6.39 9.41, 6.7 8.54, 6.92 7.47, 7.05 6.27, 7.09 5, 7.05 3.73, 6.92 2.53, 6.7 1.46, 6.39 0.59, 6 0, 5.59 -0.3, 5.05 -0.54, 4.42 -0.71, 3.73 -0.81, 3 -0.85, 2.27 -0.81, 1.58 -0.71, 0.95 -0.54, 0.41 -0.3, 0 0, -0.39 0.59), (3.23 4.09, 3.49 4.34, 3.78 4.71, 4.07 5.17, 4.35 5.69, 4.61 6.23, 4.82 6.77, 4.96 7.27, 5.03 7.69, 5 8, 4.91 8.17, 4.78 8.33, 4.61 8.47, 4.4 8.61, 4.18 8.72, 3.94 8.82, 3.69 8.89, 3.45 8.95, 3.21 8.99, 3 9, 2.79 8.99, 2.55 8.95, 2.31 8.89, 2.06 8.82, 1.82 8.72, 1.6 8.61, 1.39 8.47, 1.22 8.33, 1.09 8.17, 1 8, 0.97 7.69, 1.04 7.27, 1.18 6.77, 1.39 6.23, 1.65 5.69, 1.93 5.17, 2.22 4.71, 2.51 4.34, 2.77 4.09, 3 4, 3.23 4.09), (3.11 1.02, 3.23 1.07, 3.36 1.15, 3.5 1.26, 3.63 1.38, 3.74 1.5, 3.85 1.64, 3.93 1.77, 3.98 1.89, 4 2, 3.98 2.11, 3.93 2.23, 3.85 2.36, 3.74 2.5, 3.63 2.63, 3.5 2.74, 3.36 2.85, 3.23 2.93, 3.11 2.98, 3 3, 2.89 2.98, 2.77 2.93, 2.64 2.85, 2.5 2.74, 2.38 2.63, 2.26 2.5, 2.15 2.36, 2.07 2.23, 2.02 2.11, 2 2, 2.02 1.89, 2.07 1.77, 2.15 1.64, 2.26 1.5, 2.38 1.38, 2.5 1.26, 2.64 1.15, 2.77 1.07, 2.89 1.02, 3 1, 3.11 1.02))");
  }

  public void testSimpleCentripetalWithLongExtrapolation() {
    checkCurve("LINESTRING(-1 0, -2 1, 2 1, 1 0)", 10, 0.5, 10,
      "LINESTRING (-1 0, -1.15 0.15, -1.31 0.28, -1.46 0.4, -1.6 0.51, -1.72 0.61, -1.83 0.7, -1.92 0.79, -1.98 0.86, -2.01 0.93, -2 1, -1.86 1.09, -1.54 1.17, -1.1 1.22, -0.57 1.25, 0 1.26, 0.57 1.25, 1.1 1.22, 1.54 1.17, 1.86 1.09, 2 1, 2.01 0.93, 1.98 0.86, 1.92 0.79, 1.83 0.7, 1.72 0.61, 1.6 0.51, 1.46 0.4, 1.31 0.28, 1.15 0.15, 1 0)");
  }

  public void testIgnoresRepeatedCoordsAtStart() {
    checkCurve("LINESTRING (-2 0, -2 0, 0 1, 2 0)", 10, 0.5, 1,
      "LINESTRING (-2 0, -1.8 0.11, -1.6 0.23, -1.4 0.36, -1.2 0.5, -1 0.63, -0.8 0.74, -0.6 0.85, -0.4 0.93, -0.2 0.98, 0 1, 0.2 0.98, 0.4 0.93, 0.6 0.85, 0.8 0.74, 1 0.63, 1.2 0.5, 1.4 0.36, 1.6 0.23, 1.8 0.11, 2 0)");
  }

  public void testIgnoresRepeatedCoordsInMiddle() {
    checkCurve("LINESTRING (-2 0, 0 1, 0 1, 2 0)", 10, 0.5, 1,
      "LINESTRING (-2 0, -1.8 0.11, -1.6 0.23, -1.4 0.36, -1.2 0.5, -1 0.63, -0.8 0.74, -0.6 0.85, -0.4 0.93, -0.2 0.98, 0 1, 0.2 0.98, 0.4 0.93, 0.6 0.85, 0.8 0.74, 1 0.63, 1.2 0.5, 1.4 0.36, 1.6 0.23, 1.8 0.11, 2 0)");
  }

  public void testIgnoresRepeatedCoordsAtEnd() {
    checkCurve("LINESTRING (-2 0, 0 1, 2 0, 2 0)", 10, 0.5, 1,
      "LINESTRING (-2 0, -1.8 0.11, -1.6 0.23, -1.4 0.36, -1.2 0.5, -1 0.63, -0.8 0.74, -0.6 0.85, -0.4 0.93, -0.2 0.98, 0 1, 0.2 0.98, 0.4 0.93, 0.6 0.85, 0.8 0.74, 1 0.63, 1.2 0.5, 1.4 0.36, 1.6 0.23, 1.8 0.11, 2 0)");
  }

  public void testZ() {
    Geometry geom = read("LINESTRINGZ(0 0 0, 1 1 2, 2 0 3)");
    Geometry actual = CatmullRomSpline.catmullRomSpline(geom, 10);
    Coordinate[] actualCoords = actual.getCoordinates();
    // Sanity-check that we have filled out the Z-value
    assertEquals(1.0625, actualCoords[5].getZ(), 0.0001);
    // Check that we handle the "last point" special case correct as well
    assertEquals(3, actualCoords[actualCoords.length - 1].getZ(), 0.0001);
  }

  public void testM() {
    CatmullRomSpline catmullRom = new CatmullRomSpline(false, true);
    Coordinate[] coords = new Coordinate[3];
    coords[0] = new CoordinateXYM(0, 0, 0);
    coords[1] = new CoordinateXYM(1, 1, 2);
    coords[2] = new CoordinateXYM(2, 0, 3);
    Geometry geom = new GeometryFactory().createLineString(coords);
    Geometry actual = CatmullRomSpline.catmullRomSpline(geom, 10);
    Coordinate[] actualCoords = actual.getCoordinates();
    // Sanity-check that we have filled out the M-value
    assertEquals(1.0625, actualCoords[5].getM(), 0.0001);
    // Check that we handle the "last point" special case correct as well
    assertEquals(3, actualCoords[actualCoords.length - 1].getM(), 0.0001);
  }

  public void testZMRing() {
    Coordinate[] coords = new Coordinate[4];
    coords[0] = new CoordinateXYZM(0, 0, 0, 0);
    coords[1] = new CoordinateXYZM(1, 1, 10, 10);
    coords[2] = new CoordinateXYZM(2, 0, 20, 0);
    coords[3] = new CoordinateXYZM(0, 0, 0, 0);

    Geometry geom = new GeometryFactory().createLinearRing(coords);
    Geometry actual = CatmullRomSpline.catmullRomSpline(geom, 10);
    Coordinate[] actualCoords = actual.getCoordinates();

    // Sanity check that the Z and M are filled out (with values which match 10 times the corresponding X and Y)
    assertEquals(3.4687, actualCoords[5].getZ(), 0.001);
    assertEquals(5.6790, actualCoords[5].getM(), 0.001);
  }

  public void testXY() {
    Geometry geom = read("LINESTRING(0 0, 1 1, 2 0)");
    // Large lead-in weight to exaggerate the effect into something which is visible within the test tolerance:
    Geometry actual = new CatmullRomSpline(false, false)
      .catmullRomGeometry(geom, 10, 0.5, 10000);

    // This is the curve as it would look with weight 1, and it does indeed differ from the actual.
    // Geometry expected = read( "LINESTRING (0 0, 0.1 0.109, 0.2 0.232, 0.3 0.363, 0.4 0.4960000000000001, 0.5 0.625, 0.6 0.744, 0.7 0.847, 0.8 0.928, 0.9 0.9809999999999999, 1 1, 1.1 0.981, 1.2 0.9279999999999999, 1.3 0.8470000000000001, 1.4 0.744, 1.5 0.625, 1.6 0.496, 1.7 0.363, 1.8 0.2319999999999999, 1.9 0.109, 2 0)");
    // This is the expected with weight 10000:
    Geometry expected = read("LINESTRING (0 0, 0.1793960396039605 0.1883960396039605, 0.3254653465346536 0.3574653465346537, 0.4440891089108913 0.5070891089108913, 0.5411485148514853 0.6371485148514854, 0.6225247524752477 0.7475247524752477, 0.6940990099009903 0.8380990099009903, 0.7617524752475247 0.9087524752475247, 0.8313663366336633 0.9593663366336634, 0.9088217821782177 0.9898217821782177, 1 1, 1.0911782178217821 0.9898217821782178, 1.1686336633663368 0.9593663366336632, 1.2382475247524756 0.9087524752475244, 1.3059009900990102 0.8380990099009896, 1.377475247524753 0.747524752475247, 1.4588514851485157 0.6371485148514845, 1.5559108910891095 0.5070891089108904, 1.6745346534653471 0.3574653465346527, 1.8206039603960398 0.18839603960396, 2 0)");
    checkEqual(expected, actual, 0.01);
    assertEquals(CoordinateXY.class, actual.getCoordinate().getClass());
  }

  public void testCopiesPoints() {
    Geometry geom = read("POINT(10 20)");
    Geometry actual = new CatmullRomSpline().catmullRomGeometry(geom, 10, 0.5, 1);
    checkEqual(geom, actual);
    assertNotSame(geom, actual);
  }

  public void testEarlyOutWhenTooFewCoords() {
    Coordinate[] coords = new Coordinate[1];
    coords[0] = new Coordinate(1, 2, 3);
    Coordinate[] actual = new CatmullRomSpline().calculateSplineCoordinates(coords, 10, 0.5, false, 1);
    assertSame(coords, actual);
  }

  public void testEarlyOutWhenCollapsingIntoTooFewCoords() {
    Coordinate[] coords = new Coordinate[2];
    coords[0] = new Coordinate(1, 2, 3);
    coords[1] = new Coordinate(1, 2, 3);
    Coordinate[] actual = new CatmullRomSpline().calculateSplineCoordinates(coords, 10, 0.5, false, 1);
    assertSame(coords, actual);
  }

  private void checkCurve(String wkt, int numberOfSegments, double alpha, double extrapolationWeight, String wktExpected) {
    Geometry geom = read(wkt);
    Geometry actual = CatmullRomSpline.catmullRomSpline(geom, numberOfSegments, alpha, extrapolationWeight);
    Geometry expected = read(wktExpected);
    checkEqual(expected, actual, 0.01);
  }
}
