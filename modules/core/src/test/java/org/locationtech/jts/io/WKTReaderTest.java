/*
 * Copyright (c) 2018 Felix Obermaier
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.locationtech.jts.io;

import java.util.EnumSet;
import java.util.Locale;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import test.jts.GeometryTestCase;


/**
 * Test for {@link WKTReader}
 *
 * @version 1.7
 */
public class WKTReaderTest extends GeometryTestCase {

  // WKT readers used throughout this test
  private final WKTReader readerXY;
  private final WKTReader readerXYOld;
  private final WKTReader readerXYZ;
  private final WKTReader readerXYM;
  private final WKTReader readerXYZM;
  private WKTReader readerXYZCloseRings;

  public static void main(String args[]) {
    TestRunner.run(suite());
  }

  public WKTReaderTest(String name) {
    super(name);

    readerXY = getWKTReader(Ordinate.createXY(), 1d);
    readerXY.setIsOldJtsCoordinateSyntaxAllowed(false);
    
    readerXYOld = getWKTReader(Ordinate.createXY(), 1d);
    // set this explicitly because GeometryTestCase default is false
    readerXYOld.setIsOldJtsCoordinateSyntaxAllowed(true);

    readerXYZ = getWKTReader(Ordinate.createXYZ(), 1d);
    readerXYM = getWKTReader(Ordinate.createXYM(), 1d);
    readerXYZM = getWKTReader(Ordinate.createXYZM(), 1d);
    
    readerXYZCloseRings = getWKTReader(Ordinate.createXYZM(), 1d);
    readerXYZCloseRings.setFixStructure(true);
  }

  public static Test suite() { return new TestSuite(WKTReaderTest.class); }

  public void testPoint() throws Exception {

    // arrange
    double[] coordinates = new double[] {10, 10};
    CoordinateSequence seqPt2D = createSequence(Ordinate.createXY(), coordinates);
    CoordinateSequence seqPt2DE = createSequence(Ordinate.createXY(), new double[0]);
    CoordinateSequence seqPt3D = createSequence(Ordinate.createXYZ(), coordinates);
    CoordinateSequence seqPt2DM = createSequence(Ordinate.createXYM(), coordinates);
    CoordinateSequence seqPt3DM = createSequence(Ordinate.createXYZM(), coordinates);

    // act
    Point pt2D = (Point) readerXY.read("POINT (10 10)");
    Point pt2DE = (Point) readerXY.read("POINT EMPTY");
    Point pt3D = (Point) readerXYZ.read("POINT Z(10 10 10)");
    Point pt2DM = (Point) readerXYM.read("POINT M(10 10 11)");
    Point pt2DM2 = (Point) new WKTReader().read("POINT M(10 10 11)");
    Point pt3DM = (Point) readerXYZM.read("POINT ZM(10 10 10 11)");

    // assert
    assertTrue(isEqual(seqPt2D, pt2D.getCoordinateSequence()));
    assertTrue(isEqual(seqPt2DE, pt2DE.getCoordinateSequence()));
    assertTrue(isEqual(seqPt3D, pt3D.getCoordinateSequence()));
    assertTrue(pt2DM.getCoordinateSequence().hasM());
    assertTrue(isEqual(seqPt2DM, pt2DM.getCoordinateSequence()));
    assertTrue(pt2DM2.getCoordinateSequence().hasM());
    assertTrue(isEqual(seqPt2DM, pt2DM2.getCoordinateSequence()));
    assertTrue(isEqual(seqPt3DM, pt3DM.getCoordinateSequence()));
  }

  public void testLineString() throws Exception {

    // arrange
    double[] coordinates = new double[] {10, 10, 20, 20, 30, 40};
    CoordinateSequence seqLs2D = createSequence(Ordinate.createXY(), coordinates);
    CoordinateSequence seqLs2DE = createSequence(Ordinate.createXY(), new double[0]);
    CoordinateSequence seqLs3D = createSequence(Ordinate.createXYZ(), coordinates);
    CoordinateSequence seqLs2DM = createSequence(Ordinate.createXYM(), coordinates);
    CoordinateSequence seqLs3DM = createSequence(Ordinate.createXYZM(), coordinates);

    // act
    LineString ls2D = (LineString) readerXY
            .read("LINESTRING (10 10, 20 20, 30 40)");
    LineString ls2DE = (LineString) readerXY
            .read("LINESTRING EMPTY");
    LineString ls3D = (LineString) readerXYZ
            .read("LINESTRING Z(10 10 10, 20 20 10, 30 40 10)");
    LineString ls2DM = (LineString) readerXYM
            .read("LINESTRING M(10 10 11, 20 20 11, 30 40 11)");
    LineString ls3DM = (LineString) readerXYZM
            .read("LINESTRING ZM(10 10 10 11, 20 20 10 11, 30 40 10 11)");

    // assert
    assertTrue(isEqual(seqLs2D, ls2D.getCoordinateSequence()));
    assertTrue(isEqual(seqLs2DE, ls2DE.getCoordinateSequence()));
    assertTrue(isEqual(seqLs3D, ls3D.getCoordinateSequence()));
    assertTrue(isEqual(seqLs2DM, ls2DM.getCoordinateSequence()));
    assertTrue(isEqual(seqLs3DM, ls3DM.getCoordinateSequence()));
  }

  public void testLinearRing() throws Exception {

    double[] coordinates = new double[] {10, 10, 20, 20, 30, 40, 10, 10};
    CoordinateSequence seqLs2D = createSequence(Ordinate.createXY(), coordinates);
    CoordinateSequence seqLs2DE = createSequence(Ordinate.createXY(), new double[0]);
    CoordinateSequence seqLs3D = createSequence(Ordinate.createXYZ(), coordinates);
    CoordinateSequence seqLs2DM = createSequence(Ordinate.createXYM(), coordinates);
    CoordinateSequence seqLs3DM = createSequence(Ordinate.createXYZM(), coordinates);

    // act
    LineString ls2D = (LineString) readerXY
            .read("LINEARRING (10 10, 20 20, 30 40, 10 10)");
    LineString ls2DE = (LineString) readerXY
            .read("LINEARRING EMPTY");
    LineString ls3D = (LineString) readerXYZ
            .read("LINEARRING Z(10 10 10, 20 20 10, 30 40 10, 10 10 10)");
    LineString ls2DM = (LineString) readerXYM
            .read("LINEARRING M(10 10 11, 20 20 11, 30 40 11, 10 10 11)");
    LineString ls3DM = (LineString) readerXYZM
            .read("LINEARRING ZM(10 10 10 11, 20 20 10 11, 30 40 10 11, 10 10 10 11)");

    // assert
    assertTrue(isEqual(seqLs2D, ls2D.getCoordinateSequence()));
    assertTrue(isEqual(seqLs2DE, ls2DE.getCoordinateSequence()));
    assertTrue(isEqual(seqLs3D, ls3D.getCoordinateSequence()));
    assertTrue(isEqual(seqLs2DM, ls2DM.getCoordinateSequence()));
    assertTrue(isEqual(seqLs3DM, ls3DM.getCoordinateSequence()));
  }

  public void testLinearRingNotClosed() {
    try {
      readerXY.read("LINEARRING (10 10, 20 20, 30 40, 10 99)");
      fail();
    }
    catch (Throwable e) {
      assertTrue(e instanceof IllegalArgumentException);
      assertTrue(e.getMessage().contains("not form a closed linestring"));
    }
  }

  public void testPolygon() throws Exception {
    double[] shell = new double[] {10, 10, 10, 20, 20, 20, 20, 15, 10, 10};
    double[] ring1 = new double[] {11, 11, 12, 11, 12, 12, 12, 11, 11, 11};
    double[] ring2 = new double[] {11, 19, 11, 18, 12, 18, 12, 19, 11, 19};
    
    CoordinateSequence[] csPoly2D = new CoordinateSequence[] {
            createSequence(Ordinate.createXY(), shell),
            createSequence(Ordinate.createXY(), ring1),
            createSequence(Ordinate.createXY(), ring2)};
    CoordinateSequence csPoly2DE = createSequence(Ordinate.createXY(), new double[0]);
    CoordinateSequence[] csPoly3D = new CoordinateSequence[] {
            createSequence(Ordinate.createXYZ(), shell),
            createSequence(Ordinate.createXYZ(), ring1),
            createSequence(Ordinate.createXYZ(), ring2)};
    CoordinateSequence[] csPoly2DM = new CoordinateSequence[] {
            createSequence(Ordinate.createXYM(), shell),
            createSequence(Ordinate.createXYM(), ring1),
            createSequence(Ordinate.createXYM(), ring2)};
    CoordinateSequence[] csPoly3DM = new CoordinateSequence[] {
            createSequence(Ordinate.createXYZM(), shell),
            createSequence(Ordinate.createXYZM(), ring1),
            createSequence(Ordinate.createXYZM(), ring2)};
    
    WKTReader rdr = readerXY;
    Polygon[] poly2D = new Polygon[]{
            (Polygon) rdr.read("POLYGON ((10 10, 10 20, 20 20, 20 15, 10 10))"),
            (Polygon) rdr.read("POLYGON ((10 10, 10 20, 20 20, 20 15, 10 10), (11 11, 12 11, 12 12, 12 11, 11 11))"),
            (Polygon) rdr.read("POLYGON ((10 10, 10 20, 20 20, 20 15, 10 10), (11 11, 12 11, 12 12, 12 11, 11 11), (11 19, 11 18, 12 18, 12 19, 11 19))")
    };
    Polygon poly2DE = (Polygon) rdr.read("POLYGON EMPTY");
    rdr =  readerXYZ;
    Polygon[] poly3D = new Polygon[]{
            (Polygon) rdr.read("POLYGON Z((10 10 10, 10 20 10, 20 20 10, 20 15 10, 10 10 10))"),
            (Polygon) rdr.read("POLYGON Z((10 10 10, 10 20 10, 20 20 10, 20 15 10, 10 10 10), (11 11 10, 12 11 10, 12 12 10, 12 11 10, 11 11 10))"),
            (Polygon) rdr.read("POLYGON Z((10 10 10, 10 20 10, 20 20 10, 20 15 10, 10 10 10), (11 11 10, 12 11 10, 12 12 10, 12 11 10, 11 11 10), (11 19 10, 11 18 10, 12 18 10, 12 19 10, 11 19 10))")
    };
    rdr =  readerXYM;
    Polygon[] poly2DM = new Polygon[]{
            (Polygon) rdr.read("POLYGON M((10 10 11, 10 20 11, 20 20 11, 20 15 11, 10 10 11))"),
            (Polygon) rdr.read("POLYGON M((10 10 11, 10 20 11, 20 20 11, 20 15 11, 10 10 11), (11 11 11, 12 11 11, 12 12 11, 12 11 11, 11 11 11))"),
            (Polygon) rdr.read("POLYGON M((10 10 11, 10 20 11, 20 20 11, 20 15 11, 10 10 11), (11 11 11, 12 11 11, 12 12 11, 12 11 11, 11 11 11), (11 19 11, 11 18 11, 12 18 11, 12 19 11, 11 19 11))")
    };
    rdr =  readerXYZM;
    Polygon[] poly3DM = new Polygon[]{
            (Polygon) rdr.read("POLYGON ZM((10 10 10 11, 10 20 10 11, 20 20 10 11, 20 15 10 11, 10 10 10 11))"),
            (Polygon) rdr.read("POLYGON ZM((10 10 10 11, 10 20 10 11, 20 20 10 11, 20 15 10 11, 10 10 10 11), (11 11 10 11, 12 11 10 11, 12 12 10 11, 12 11 10 11, 11 11 10 11))"),
            (Polygon) rdr.read("POLYGON ZM((10 10 10 11, 10 20 10 11, 20 20 10 11, 20 15 10 11, 10 10 10 11), (11 11 10 11, 12 11 10 11, 12 12 10 11, 12 11 10 11, 11 11 10 11), (11 19 10 11, 11 18 10 11, 12 18 10 11, 12 19 10 11, 11 19 10 11))")
    };
    // assert
    assertTrue(isEqual(csPoly2D[0], poly2D[2].getExteriorRing().getCoordinateSequence()));
    assertTrue(isEqual(csPoly2D[1], poly2D[2].getInteriorRingN(0).getCoordinateSequence()));
    assertTrue(isEqual(csPoly2D[2], poly2D[2].getInteriorRingN(1).getCoordinateSequence()));
    assertTrue(isEqualDim(csPoly2DE, poly2DE.getExteriorRing().getCoordinateSequence(), 2));

    assertTrue(isEqual(csPoly3D[0], poly3D[2].getExteriorRing().getCoordinateSequence()));
    assertTrue(isEqual(csPoly3D[1], poly3D[2].getInteriorRingN(0).getCoordinateSequence()));
    assertTrue(isEqual(csPoly3D[2], poly3D[2].getInteriorRingN(1).getCoordinateSequence()));
    assertTrue(isEqual(csPoly2DM[0], poly2DM[2].getExteriorRing().getCoordinateSequence()));
    assertTrue(isEqual(csPoly2DM[1], poly2DM[2].getInteriorRingN(0).getCoordinateSequence()));
    assertTrue(isEqual(csPoly2DM[2], poly2DM[2].getInteriorRingN(1).getCoordinateSequence()));
    assertTrue(isEqual(csPoly3DM[0], poly3DM[2].getExteriorRing().getCoordinateSequence()));
    assertTrue(isEqual(csPoly3DM[1], poly3DM[2].getInteriorRingN(0).getCoordinateSequence()));
    assertTrue(isEqual(csPoly3DM[2], poly3DM[2].getInteriorRingN(1).getCoordinateSequence()));
  }

  static double[][] mpCoords = new double[][] { 
    new double[] {10, 10}, 
    new double[] {20, 20}};
  
  public void testMultiPointXY() throws Exception {
    MultiPoint mp = (MultiPoint) readerXY.read("MULTIPOINT ((10 10), (20 20))");
    CoordinateSequence[] cs = createSequences(Ordinate.createXY(), mpCoords);
    checkCS(cs[0], mp.getGeometryN(0));
    checkCS(cs[1], mp.getGeometryN(1));
  }
  
  public void testMultiPointXYOldSyntax() throws Exception {
    MultiPoint mp = (MultiPoint) readerXY.read("MULTIPOINT (10 10, 20 20)");
    CoordinateSequence[] cs = createSequences(Ordinate.createXY(), mpCoords);
    checkCS(cs[0], mp.getGeometryN(0));
    checkCS(cs[1], mp.getGeometryN(1));
  }
    
  public void testMultiPointXY_Empty() throws Exception {
    MultiPoint mp = (MultiPoint) readerXY.read("MULTIPOINT EMPTY");
    checkEmpty(mp);
  }
  
  public void testMultiPointXY_WithEmpty() throws Exception {
    MultiPoint mp = (MultiPoint) readerXY.read("MULTIPOINT ((10 10), EMPTY, (20 20))");
    CoordinateSequence[] cs = createSequences(Ordinate.createXY(), mpCoords);
    checkCS(cs[0], mp.getGeometryN(0));
    checkEmpty(mp.getGeometryN(1));
    checkCS(cs[1], mp.getGeometryN(2));
  }

  public void testMultiPointXYM() throws Exception {
    MultiPoint mp = (MultiPoint) readerXYM.read("MULTIPOINT M((10 10 11), (20 20 11))");
    CoordinateSequence[] cs = createSequences(Ordinate.createXYM(), mpCoords);
    checkCS(cs[0], mp.getGeometryN(0));
    checkCS(cs[1], mp.getGeometryN(1));
  }

  public void testMultiPointXYZ() throws Exception {
    MultiPoint mp = (MultiPoint) readerXYZ.read("MULTIPOINT Z((10 10 10), (20 20 10))");
    CoordinateSequence[] cs = createSequences(Ordinate.createXYZ(), mpCoords);
    checkCS(cs[0], mp.getGeometryN(0));
    checkCS(cs[1], mp.getGeometryN(1));
  }

  public void testMultiPointXYZM() throws Exception {
    MultiPoint mp = (MultiPoint) readerXYZM.read("MULTIPOINT ZM((10 10 10 11), (20 20 10 11))");
    CoordinateSequence[] cs = createSequences(Ordinate.createXYZM(), mpCoords);
    checkCS(cs[0], mp.getGeometryN(0));
    checkCS(cs[1], mp.getGeometryN(1));
  }

  double[][] mLcoords = new double[][] { 
    new double[] {10, 10, 20, 20}, 
    new double[] {15, 15, 30, 15}};

  public void testMultiLineStringXY() throws Exception {
    MultiLineString mp = (MultiLineString) readerXY.read("MULTILINESTRING ((10 10, 20 20), (15 15, 30 15))");
    CoordinateSequence[] cs = createSequences(Ordinate.createXY(), mLcoords);
    checkCS(cs[0], mp.getGeometryN(0));
    checkCS(cs[1], mp.getGeometryN(1));
  }
  
  public void testMultiLineStringXY_Empty() throws Exception {
    MultiLineString mp = (MultiLineString) readerXY.read("MULTILINESTRING EMPTY");
    checkEmpty(mp);
  }
  
  public void testMultiLineStringXY_WithEmpty() throws Exception {
    MultiLineString mp = (MultiLineString) readerXY.read("MULTILINESTRING ((10 10, 20 20), EMPTY, (15 15, 30 15))");
    CoordinateSequence[] cs = createSequences(Ordinate.createXY(), mLcoords);
    checkCS(cs[0], mp.getGeometryN(0));
    checkEmpty(mp.getGeometryN(1));
    checkCS(cs[1], mp.getGeometryN(2));
  }

  public void testMultiLineStringXYM() throws Exception {
    MultiLineString mp = (MultiLineString) readerXYM.read("MULTILINESTRING M((10 10 11, 20 20 11), (15 15 11, 30 15 11))");
    CoordinateSequence[] cs = createSequences(Ordinate.createXYM(), mLcoords);
    checkCS(cs[0], mp.getGeometryN(0));
    checkCS(cs[1], mp.getGeometryN(1));
  }

  public void testMultiLineStringXYZ() throws Exception {
    MultiLineString mp = (MultiLineString) readerXYZ.read("MULTILINESTRING Z((10 10 10, 20 20 10), (15 15 10, 30 15 10))");
    CoordinateSequence[] cs = createSequences(Ordinate.createXYZ(), mLcoords);
    checkCS(cs[0], mp.getGeometryN(0));
    checkCS(cs[1], mp.getGeometryN(1));
  }

  public void testMultiLineStringYZM() throws Exception {
    MultiLineString mp = (MultiLineString) readerXYZM.read("MULTILINESTRING ZM((10 10 10 11, 20 20 10 11), (15 15 10 11, 30 15 10 11))");
    CoordinateSequence[] cs = createSequences(Ordinate.createXYZM(), mLcoords);
    checkCS(cs[0], mp.getGeometryN(0));
    checkCS(cs[1], mp.getGeometryN(1));
  }

  double[][] mAcoords = new double[][] { 
    new double[] {10, 10, 10, 20, 20, 20, 20, 15, 10, 10}, 
    new double[] {11, 11, 12, 11, 12, 12, 12, 11, 11, 11},
    new double[] {60, 60, 70, 70, 80, 60, 60, 60}
    };

  public void testMultiPolygonXY() throws Exception {
    MultiPolygon mp = (MultiPolygon) readerXY.read(
        "MULTIPOLYGON (((10 10, 10 20, 20 20, 20 15, 10 10), (11 11, 12 11, 12 12, 12 11, 11 11)), ((60 60, 70 70, 80 60, 60 60)))");
    CoordinateSequence[] cs = createSequences(Ordinate.createXY(), mAcoords);
    checkCS(cs[0], ((Polygon)mp.getGeometryN(0)).getExteriorRing());
    checkCS(cs[1], ((Polygon)mp.getGeometryN(0)).getInteriorRingN(0));
    checkCS(cs[2], ((Polygon)mp.getGeometryN(1)).getExteriorRing());
  }
  
  public void testMultiPolygonXY_Empty() throws Exception {
    MultiPolygon mp = (MultiPolygon) readerXY.read("MULTIPOLYGON EMPTY");
    checkEmpty(mp);
  }
  
  public void testMultiPolygonXY_WithEmpty() throws Exception {
    MultiPolygon mp = (MultiPolygon) readerXY.read(
        "MULTIPOLYGON (((10 10, 10 20, 20 20, 20 15, 10 10), (11 11, 12 11, 12 12, 12 11, 11 11)), EMPTY, ((60 60, 70 70, 80 60, 60 60)))");
    CoordinateSequence[] cs = createSequences(Ordinate.createXY(), mAcoords);
    checkCS(cs[0], ((Polygon)mp.getGeometryN(0)).getExteriorRing());
    checkCS(cs[1], ((Polygon)mp.getGeometryN(0)).getInteriorRingN(0));
    checkEmpty(((Polygon)mp.getGeometryN(1)));
    checkCS(cs[2], ((Polygon)mp.getGeometryN(2)).getExteriorRing());
  }

  public void testMultiPolygonXYM() throws Exception {
    MultiPolygon mp = (MultiPolygon) readerXYM.read(
        "MULTIPOLYGON M(((10 10 11, 10 20 11, 20 20 11, 20 15 11, 10 10 11), (11 11 11, 12 11 11, 12 12 11, 12 11 11, 11 11 11)), ((60 60 11, 70 70 11, 80 60 11, 60 60 11)))");
    CoordinateSequence[] cs = createSequences(Ordinate.createXYM(), mAcoords);
    checkCS(cs[0], ((Polygon)mp.getGeometryN(0)).getExteriorRing());
    checkCS(cs[1], ((Polygon)mp.getGeometryN(0)).getInteriorRingN(0));
    checkCS(cs[2], ((Polygon)mp.getGeometryN(1)).getExteriorRing());
  }

  public void testMultiPolygonXYZ() throws Exception {
    MultiPolygon mp = (MultiPolygon) readerXYZ.read(
        "MULTIPOLYGON Z(((10 10 10, 10 20 10, 20 20 10, 20 15 10, 10 10 10), (11 11 10, 12 11 10, 12 12 10, 12 11 10, 11 11 10)), ((60 60 10, 70 70 10, 80 60 10, 60 60 10)))");
    CoordinateSequence[] cs = createSequences(Ordinate.createXYZ(), mAcoords);
    checkCS(cs[0], ((Polygon)mp.getGeometryN(0)).getExteriorRing());
    checkCS(cs[1], ((Polygon)mp.getGeometryN(0)).getInteriorRingN(0));
    checkCS(cs[2], ((Polygon)mp.getGeometryN(1)).getExteriorRing());
  }

  public void testMultiPolygonYZM() throws Exception {
    MultiPolygon mp = (MultiPolygon) readerXYZM.read(
        "MULTIPOLYGON ZM(((10 10 10 11, 10 20 10 11, 20 20 10 11, 20 15 10 11, 10 10 10 11), (11 11 10 11, 12 11 10 11, 12 12 10 11, 12 11 10 11, 11 11 10 11)), ((60 60 10 11, 70 70 10 11, 80 60 10 11, 60 60 10 11)))");
    CoordinateSequence[] cs = createSequences(Ordinate.createXYZM(), mAcoords);
    checkCS(cs[0], ((Polygon)mp.getGeometryN(0)).getExteriorRing());
    checkCS(cs[1], ((Polygon)mp.getGeometryN(0)).getInteriorRingN(0));
    checkCS(cs[2], ((Polygon)mp.getGeometryN(1)).getExteriorRing());
  }

  public void testGeometryCollection() throws Exception {

    // arrange
    double[][] coordinates = new double[][] { new double[] {10, 10}, new double[] {30, 30},
            new double[] {15, 15, 20, 20}, new double[0], new double[] {10, 10, 20, 20, 30, 40, 10, 10}  };

    CoordinateSequence[] css = new CoordinateSequence[] {
            createSequence(Ordinate.createXY(), coordinates[0]),
            createSequence(Ordinate.createXY(), coordinates[1]),
            createSequence(Ordinate.createXY(), coordinates[2]),
            createSequence(Ordinate.createXY(), coordinates[3]),
            createSequence(Ordinate.createXY(), coordinates[4]),
    };

    // arrange
    WKTReader rdr = getWKTReader(Ordinate.createXY(), 1);
    GeometryCollection gc0 = (GeometryCollection)rdr.read("GEOMETRYCOLLECTION (POINT (10 10), POINT (30 30), LINESTRING (15 15, 20 20))");
    GeometryCollection gc1 = (GeometryCollection)rdr.read("GEOMETRYCOLLECTION (POINT (10 10), LINEARRING EMPTY, LINESTRING (15 15, 20 20))");
    GeometryCollection gc2 = (GeometryCollection)rdr.read("GEOMETRYCOLLECTION (POINT (10 10), LINEARRING (10 10, 20 20, 30 40, 10 10), LINESTRING (15 15, 20 20))");
    GeometryCollection gc3 = (GeometryCollection)rdr.read("GEOMETRYCOLLECTION EMPTY");

    // assert
    assertTrue(isEqual(css[0], ((Point)gc0.getGeometryN(0)).getCoordinateSequence()));
    assertTrue(isEqual(css[1], ((Point)gc0.getGeometryN(1)).getCoordinateSequence()));
    assertTrue(isEqual(css[2], ((LineString)gc0.getGeometryN(2)).getCoordinateSequence()));
    assertTrue(isEqual(css[0], ((Point)gc1.getGeometryN(0)).getCoordinateSequence()));
    assertTrue(isEqual(css[3], ((LinearRing)gc1.getGeometryN(1)).getCoordinateSequence()));
    assertTrue(isEqual(css[2], ((LineString)gc1.getGeometryN(2)).getCoordinateSequence()));
    assertTrue(isEqual(css[0], ((Point)gc2.getGeometryN(0)).getCoordinateSequence()));
    assertTrue(isEqual(css[4], ((LinearRing)gc2.getGeometryN(1)).getCoordinateSequence()));
    assertTrue(isEqual(css[2], ((LineString)gc2.getGeometryN(2)).getCoordinateSequence()));
    assertTrue(gc3.isEmpty());
  }

  public void testEmptyLineDimOldSyntax() throws ParseException {
    WKTReader wktReader = new WKTReader();
    LineString geom = (LineString) wktReader.read("LINESTRING EMPTY");
    int dim = geom.getCoordinateSequence().getDimension();
    checkCSDim(geom.getCoordinateSequence(), 3);
  }
  
  public void testEmptyLineDim() throws ParseException {
    WKTReader wktReader = new WKTReader();
    wktReader.setIsOldJtsCoordinateSyntaxAllowed(false);
    LineString geom = (LineString) wktReader.read("LINESTRING EMPTY");
    checkCSDim(geom.getCoordinateSequence(), 2);
  }
  
  public void testEmptyPolygonDim() throws ParseException {
    WKTReader wktReader = new WKTReader();
    wktReader.setIsOldJtsCoordinateSyntaxAllowed(false);
    Polygon geom = (Polygon) wktReader.read("POLYGON EMPTY");
    checkCSDim(geom.getExteriorRing().getCoordinateSequence(), 2);
  }
  
  public void testNaN() throws Exception {

    // arrange
    CoordinateSequence seq = createSequence(Ordinate.createXYZ(), new double[] {10, 10});
    seq.setOrdinate(0, CoordinateSequence.Z, Double.NaN);

    // act
    Point pt1 = (Point)readerXYOld.read("POINT (10 10 NaN)");
    Point pt2 = (Point)readerXYOld.read("POINT (10 10 nan)");
    Point pt3 = (Point)readerXYOld.read("POINT (10 10 NAN)");

    // assert
    assertTrue(isEqual(seq, pt1.getCoordinateSequence()));
    assertTrue(isEqual(seq, pt2.getCoordinateSequence()));
    assertTrue(isEqual(seq, pt3.getCoordinateSequence()));
  }

  public void testDimensionConsistence() throws Exception {
    //test 2-d point with XY coordinate
    CoordinateSequence seq1 = createSequence(Ordinate.createXY(), new double[] {10, 10});
    Point pt1 = (Point)readerXYOld.read("POINT (10 10)");
    assertTrue(isEqual(seq1, pt1.getCoordinateSequence()));

    //test 2-d point with XYZ coordinate
    CoordinateSequence seq2 = createSequence(Ordinate.createXYZ(), new double[] { 10, 10 });
    seq2.setOrdinate(0, 2, Double.NaN);
    Point pt2 = (Point)this.readerXYOld.read("POINT (10 10 NaN)");
    assertTrue(isEqual(seq2, pt2.getCoordinateSequence()));

    //test points sequence
    CoordinateSequence seq3 = createSequence(Ordinate.createXYZ(), new double[] { 10, 10, 20, 20, 30, 30 });
    seq3.setOrdinate(0, 2, Double.NaN);
    seq3.setOrdinate(1, 2, 25);
    seq3.setOrdinate(2, 2, Double.NaN);
    LineString ls = (LineString)this.readerXYOld.read("LINESTRING (10 10 NaN, 20 20 25, 30 30 NaN)");
    assertTrue(isEqual(seq3, ls.getCoordinateSequence()));
  }

  public void testLargeNumbers() throws Exception {
    PrecisionModel precisionModel = new PrecisionModel(1E9);
    GeometryFactory geometryFactory = new GeometryFactory(precisionModel, 0);
    WKTReader reader = new WKTReader(geometryFactory);
    CoordinateSequence point1 = ((Point)reader.read("POINT (123456789.01234567890 10)")).getCoordinateSequence();
    CoordinateSequence point2 = geometryFactory.createPoint(new Coordinate(123456789.01234567890, 10)).getCoordinateSequence();
    assertEquals(point1.getOrdinate(0, CoordinateSequence.X), point2.getOrdinate(0, CoordinateSequence.X), 1E-7);
    assertEquals(point1.getOrdinate(0, CoordinateSequence.Y), point2.getOrdinate(0, CoordinateSequence.Y), 1E-7);
  }

  public void testTurkishLocale() throws Exception {
      Locale original = Locale.getDefault();
      try {
          Locale.setDefault(Locale.forLanguageTag("tr"));
          Point point = (Point) readerXY.read("point (10 20)");
          assertEquals(10.0, point.getX(), 1E-7);
          assertEquals(20.0, point.getY(), 1E-7);
      } finally {
          Locale.setDefault(original);
      }
  }
  


  private void checkCS(CoordinateSequence cs, Geometry geom) {
    assertTrue( isEqual( cs, extractCS(geom)));
  }

  private CoordinateSequence extractCS(Geometry geom) {
    if (geom instanceof Point) return ((Point)geom).getCoordinateSequence();
    if (geom instanceof LineString) return ((LineString)geom).getCoordinateSequence();
    throw new IllegalArgumentException("Can't extract coordinate sequence from geometry of type " + geom.getGeometryType());
  }
  
  private void checkEmpty(Geometry geom) {
    assertTrue(geom.isEmpty());
    if (geom instanceof GeometryCollection) {
      assertTrue(geom.getNumGeometries() == 0);
    }
  }
  
  private void checkCSDim(CoordinateSequence cs, int expectedCoordDim) {
    int dim = cs.getDimension();
    assertEquals(expectedCoordDim, dim);
  }
  
  private static CoordinateSequence[] createSequences(EnumSet<Ordinate> ordinateFlags, double[][] xyarray) {
    CoordinateSequence[] csarray = new CoordinateSequence[xyarray.length];
    for (int i = 0; i < xyarray.length; i++) {
      csarray[i] = createSequence(ordinateFlags, xyarray[i]);
    }
    return csarray;
  }

  private static CoordinateSequence createSequence(EnumSet<Ordinate> ordinateFlags, double[] xy) {

    // get the number of dimension to verify size of provided ordinate values array
    int dimension = requiredDimension(ordinateFlags);

    // inject additional values
    double[] ordinateValues = injectZM(ordinateFlags, xy);
    
    if ((ordinateValues.length % dimension) != 0)
      throw new IllegalArgumentException("ordinateFlags and number of provided ordinate values don't match");

    // get the required size of the sequence
    int size = ordinateValues.length / dimension;

    // create a sequence capable of storing all ordinate values.
    CoordinateSequence res = getCSFactory(ordinateFlags)
            .create(size, requiredDimension(ordinateFlags));

    // fill in values
    int k = 0;
    for(int i = 0; i < ordinateValues.length; i+= dimension) {
      for (int j = 0; j < dimension; j++)
        res.setOrdinate(k, j, ordinateValues[i+j]);
      k++;
    }

    return res;
  }

  private static int requiredDimension(EnumSet<Ordinate> ordinateFlags)
  {
    return ordinateFlags.size();
  }


  private static double[] injectZM(EnumSet<Ordinate> ordinateFlags, double[] xy) {
    int size = xy.length / 2;
    int dimension = requiredDimension(ordinateFlags);
    double[] res = new double[size * dimension];
    int k = 0;
    for (int i = 0; i < xy.length; i+=2) {
      res[k++] = xy[i];
      res[k++] = xy[i+1];
      if (ordinateFlags.contains(Ordinate.Z)) res[k++] = 10;
      if (ordinateFlags.contains(Ordinate.M)) res[k++] = 11;
    }
    return res;
  }
}
