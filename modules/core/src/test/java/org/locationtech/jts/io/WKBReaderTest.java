/*
 * Copyright (c) 2016 Vivid Solutions.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.io;

import org.locationtech.jts.geom.CoordinateSequenceComparator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import junit.framework.TestCase;
import junit.textui.TestRunner;


/**
 * Tests for reading WKB.
 * 
 * @author Martin Davis
 *
 */
public class WKBReaderTest  extends TestCase
{
  public static void main(String args[]) {
    TestRunner.run(WKBReaderTest.class);
  }

  private GeometryFactory geomFactory = new GeometryFactory();
  private WKTReader rdr = new WKTReader(geomFactory);

  public void testShortPolygons() throws ParseException
  {
    // one point
    checkWKBGeometry("0000000003000000010000000140590000000000004069000000000000", "POLYGON ((100 200, 100 200, 100 200, 100 200))");
    // two point
    checkWKBGeometry("000000000300000001000000024059000000000000406900000000000040590000000000004069000000000000", "POLYGON ((100 200, 100 200, 100 200, 100 200))");
  }

  public WKBReaderTest(String name) { super(name); }

  public void testSinglePointLineString() throws ParseException
  {
    checkWKBGeometry("00000000020000000140590000000000004069000000000000", "LINESTRING (100 200, 100 200)");
  }

   /**
    * After removing the 39 bytes of MBR info at the front, and the
    * end-of-geometry byte, * Spatialite native BLOB is very similar
    * to WKB, except instead of a endian marker at the start of each
    * geometry in a multi-geometry, it has a start marker of 0x69.
    * Endianness is determined by the endian value of the multigeometry.
    *
    * @throws ParseException
    */
  public void testSpatialiteMultiGeometry() throws ParseException
  {
     //multipolygon
     checkWKBGeometry("01060000000200000069030000000100000004000000000000000000444000000000000044400000000000003440000000000080464000000000008046400000000000003E4000000000000044400000000000004440690300000001000000040000000000000000003E40000000000000344000000000000034400000000000002E40000000000000344000000000000039400000000000003E400000000000003440",
           "MULTIPOLYGON (((40 40, 20 45, 45 30, 40 40)), ((30 20, 20 15, 20 25, 30 20)))'");

     //multipoint
     checkWKBGeometry("0104000000020000006901000000000000000000F03F000000000000F03F690100000000000000000000400000000000000040",
           "MULTIPOINT(1 1,2 2)'");

     //multiline
     checkWKBGeometry("010500000002000000690200000003000000000000000000244000000000000024400000000000003440000000000000344000000000000024400000000000004440690200000004000000000000000000444000000000000044400000000000003E400000000000003E40000000000000444000000000000034400000000000003E400000000000002440",
           "MULTILINESTRING ((10 10, 20 20, 10 40), (40 40, 30 30, 40 20, 30 10))");

     //geometrycollection
     checkWKBGeometry(
           "010700000002000000690100000000000000000010400000000000001840690200000002000000000000000000104000000000000018400000000000001C400000000000002440",
           "GEOMETRYCOLLECTION(POINT(4 6),LINESTRING(4 6,7 10))"
     );
  }

  public void test2dSpatialiteWKB() throws ParseException {
    // Point
    checkWKBGeometry("0101000020E6100000000000000000F03F0000000000000040",
        "POINT(1 2)");
    // LineString
    checkWKBGeometry("0102000020E610000002000000000000000000F03F000000000000004000000000000008400000000000001040",
        "LINESTRING(1 2, 3 4)");
    // Polygon
    checkWKBGeometry("0103000020E61000000200000005000000000000000000000000000000000000000000000000000000000000000000244000000000000024400000000000002440000000000000244000000000000000000000000000000000000000000000000005000000000000000000F03F000000000000F03F000000000000F03F0000000000002240000000000000224000000000000022400000000000002240000000000000F03F000000000000F03F000000000000F03F",
        "POLYGON((0 0,0 10,10 10,10 0,0 0),(1 1,1 9,9 9,9 1,1 1))");
    // MultiPoint
    checkWKBGeometry("0104000020E61000000200000001010000000000000000000000000000000000F03F010100000000000000000000400000000000000840",
        "MULTIPOINT(0 1,2 3)");
    // MultiLineString
    checkWKBGeometry("0105000020E6100000020000000102000000020000000000000000000000000000000000F03F000000000000004000000000000008400102000000020000000000000000001040000000000000144000000000000018400000000000001C40",
        "MULTILINESTRING((0 1,2 3),(4 5,6 7))");
    // MultiPolygon
    checkWKBGeometry("0106000020E61000000200000001030000000200000005000000000000000000000000000000000000000000000000000000000000000000244000000000000024400000000000002440000000000000244000000000000000000000000000000000000000000000000005000000000000000000F03F000000000000F03F000000000000F03F0000000000002240000000000000224000000000000022400000000000002240000000000000F03F000000000000F03F000000000000F03F0103000000010000000500000000000000000022C0000000000000000000000000000022C00000000000002440000000000000F0BF0000000000002440000000000000F0BF000000000000000000000000000022C00000000000000000",
        "MULTIPOLYGON(((0 0,0 10,10 10,10 0,0 0),(1 1,1 9,9 9,9 1,1 1)),((-9 0,-9 10,-1 10,-1 0,-9 0)))");
    // GeometryCollection
    checkWKBGeometry("0107000020E61000000900000001010000000000000000000000000000000000F03F01010000000000000000000000000000000000F03F01010000000000000000000040000000000000084001020000000200000000000000000000400000000000000840000000000000104000000000000014400102000000020000000000000000000000000000000000F03F000000000000004000000000000008400102000000020000000000000000001040000000000000144000000000000018400000000000001C4001030000000200000005000000000000000000000000000000000000000000000000000000000000000000244000000000000024400000000000002440000000000000244000000000000000000000000000000000000000000000000005000000000000000000F03F000000000000F03F000000000000F03F0000000000002240000000000000224000000000000022400000000000002240000000000000F03F000000000000F03F000000000000F03F01030000000200000005000000000000000000000000000000000000000000000000000000000000000000244000000000000024400000000000002440000000000000244000000000000000000000000000000000000000000000000005000000000000000000F03F000000000000F03F000000000000F03F0000000000002240000000000000224000000000000022400000000000002240000000000000F03F000000000000F03F000000000000F03F0103000000010000000500000000000000000022C0000000000000000000000000000022C00000000000002440000000000000F0BF0000000000002440000000000000F0BF000000000000000000000000000022C00000000000000000",
        "GEOMETRYCOLLECTION(POINT(0 1),POINT(0 1),POINT(2 3),LINESTRING(2 3,4 5),LINESTRING(0 1,2 3),LINESTRING(4 5,6 7),POLYGON((0 0,0 10,10 10,10 0,0 0),(1 1,1 9,9 9,9 1,1 1)),POLYGON((0 0,0 10,10 10,10 0,0 0),(1 1,1 9,9 9,9 1,1 1)),POLYGON((-9 0,-9 10,-1 10,-1 0,-9 0)))");
    }

  public void testSpatialiteWKB_Z() throws ParseException {
    // PointZ
    checkWKBGeometry("01010000A0E6100000000000000000F03F00000000000000400000000000000840",
        "POINT Z(1 2 3)");
    // LineStringZ
    checkWKBGeometry("01020000A0E610000002000000000000000000F03F00000000000000400000000000000840000000000000104000000000000014400000000000001840",
        "LINESTRING Z(1 2 3, 4 5 6)");
    // PolygonZ
    checkWKBGeometry("01030000A0E6100000020000000500000000000000000000000000000000000000000000000000594000000000000000000000000000002440000000000000594000000000000024400000000000002440000000000000594000000000000024400000000000000000000000000000594000000000000000000000000000000000000000000000594005000000000000000000F03F000000000000F03F0000000000005940000000000000F03F000000000000224000000000000059400000000000002240000000000000224000000000000059400000000000002240000000000000F03F0000000000005940000000000000F03F000000000000F03F0000000000005940",
        "POLYGON Z((0 0 100,0 10 100,10 10 100,10 0 100,0 0 100),(1 1 100,1 9 100,9 9 100,9 1 100,1 1 100))");
    // MultiPointZ
    checkWKBGeometry("01040000A0E61000000200000001010000800000000000000000000000000000F03F00000000000000400101000080000000000000084000000000000010400000000000001440",
        "MULTIPOINTS Z(0 1 2, 3 4 5)");
    // MultiLineStringZ
    checkWKBGeometry("01050000A0E6100000020000000102000080020000000000000000000000000000000000F03F000000000000004000000000000008400000000000001040000000000000144001020000800200000000000000000018400000000000001C400000000000002040000000000000224000000000000024400000000000002640",
        "MULTILINESTRING Z((0 1 2,3 4 5),(6 7 8,9 10 11))");
    // MultiPolygonZ
    checkWKBGeometry("01060000A0E6100000020000000103000080020000000500000000000000000000000000000000000000000000000000594000000000000000000000000000002440000000000000594000000000000024400000000000002440000000000000594000000000000024400000000000000000000000000000594000000000000000000000000000000000000000000000594005000000000000000000F03F000000000000F03F0000000000005940000000000000F03F000000000000224000000000000059400000000000002240000000000000224000000000000059400000000000002240000000000000F03F0000000000005940000000000000F03F000000000000F03F00000000000059400103000080010000000500000000000000000022C00000000000000000000000000000494000000000000022C000000000000024400000000000004940000000000000F0BF00000000000024400000000000004940000000000000F0BF0000000000000000000000000000494000000000000022C000000000000000000000000000004940",
        "MULTIPOLYGON Z(((0 0 100,0 10 100,10 10 100,10 0 100,0 0 100),(1 1 100,1 9 100,9 9 100,9 1 100,1 1 100)),((-9 0 50,-9 10 50,-1 10 50,-1 0 50,-9 0 50)))");
    // GeometryCollectionZ
  }


  public void testSpatialiteWKB_M() throws ParseException {
    // PointM
    checkWKBGeometry("0101000060E6100000000000000000F03F00000000000000400000000000000840",
        "POINT M(1 2 3)");
    // LineStringM
    checkWKBGeometry("0102000060E610000002000000000000000000F03F00000000000000400000000000000840000000000000104000000000000014400000000000001840",
        "LINESTRING M(1 2 3,4 5 6)");
    // PolygonM
    checkWKBGeometry("0103000060E6100000020000000500000000000000000000000000000000000000000000000000594000000000000000000000000000002440000000000000594000000000000024400000000000002440000000000000594000000000000024400000000000000000000000000000594000000000000000000000000000000000000000000000594005000000000000000000F03F000000000000F03F0000000000005940000000000000F03F000000000000224000000000000059400000000000002240000000000000224000000000000059400000000000002240000000000000F03F0000000000005940000000000000F03F000000000000F03F0000000000005940",
        "POLYGON M((0 0 100,0 10 100,10 10 100,10 0 100,0 0 100),(1 1 100,1 9 100,9 9 100,9 1 100,1 1 100))");
    // MultiPointM
    checkWKBGeometry("01040000A0E61000000200000001010000800000000000000000000000000000F03F00000000000000400101000080000000000000084000000000000010400000000000001440",
        "MULTIPOINT M(0 1 2,3 4 5)");
    // MultiLineStringM
    checkWKBGeometry("0105000060E6100000020000000102000040020000000000000000000000000000000000F03F000000000000004000000000000008400000000000001040000000000000144001020000400200000000000000000018400000000000001C400000000000002040000000000000224000000000000024400000000000002640",
        "MULTILINESTRING M((0 1 2,3 4 5),(6 7 8,9 10 11))");
    // MultiPolygonM
    checkWKBGeometry("0106000060E6100000020000000103000040020000000500000000000000000000000000000000000000000000000000594000000000000000000000000000002440000000000000594000000000000024400000000000002440000000000000594000000000000024400000000000000000000000000000594000000000000000000000000000000000000000000000594005000000000000000000F03F000000000000F03F0000000000005940000000000000F03F000000000000224000000000000059400000000000002240000000000000224000000000000059400000000000002240000000000000F03F0000000000005940000000000000F03F000000000000F03F00000000000059400103000040010000000500000000000000000022C00000000000000000000000000000494000000000000022C000000000000024400000000000004940000000000000F0BF00000000000024400000000000004940000000000000F0BF0000000000000000000000000000494000000000000022C000000000000000000000000000004940",
        "MULTIPOLYGON M(((0 0 100,0 10 100,10 10 100,10 0 100,0 0 100),(1 1 100,1 9 100,9 9 100,9 1 100,1 1 100)),((-9 0 50,-9 10 50,-1 10 50,-1 0 50,-9 0 50)))");
    // GeometryCollectionM
    //checkWKBGeometry("0107000020E61000000900000001010000000000000000000000000000000000F03F01010000000000000000000000000000000000F03F01010000000000000000000040000000000000084001020000000200000000000000000000400000000000000840000000000000104000000000000014400102000000020000000000000000000000000000000000F03F000000000000004000000000000008400102000000020000000000000000001040000000000000144000000000000018400000000000001C4001030000000200000005000000000000000000000000000000000000000000000000000000000000000000244000000000000024400000000000002440000000000000244000000000000000000000000000000000000000000000000005000000000000000000F03F000000000000F03F000000000000F03F0000000000002240000000000000224000000000000022400000000000002240000000000000F03F000000000000F03F000000000000F03F01030000000200000005000000000000000000000000000000000000000000000000000000000000000000244000000000000024400000000000002440000000000000244000000000000000000000000000000000000000000000000005000000000000000000F03F000000000000F03F000000000000F03F0000000000002240000000000000224000000000000022400000000000002240000000000000F03F000000000000F03F000000000000F03F0103000000010000000500000000000000000022C0000000000000000000000000000022C00000000000002440000000000000F0BF0000000000002440000000000000F0BF000000000000000000000000000022C00000000000000000",
    //    "MULTIPOLYGONM(((0 0 100,0 10 100,10 10 100,10 0 100,0 0 100),(1 1 100,1 9 100,9 9 100,9 1 100,1 1 100)),((-9 0 50,-9 10 50,-1 10 50,-1 0 50,-9 0 50)))");
  }

  public void testSpatialiteWKB_ZM() throws ParseException {
    // PointZM
    checkWKBGeometry("01010000E0E6100000000000000000F03F000000000000004000000000000008400000000000006940",
        "POINT ZM (1 2 3 200)");
    // LineStringZM
    checkWKBGeometry("01020000E0E610000002000000000000000000F03F0000000000000040000000000000084000000000000069400000000000001040000000000000144000000000000018400000000000006940",
        "LINESTRING ZM (1 2 3 200,4 5 6 200)");
    // PolygonZM
    checkWKBGeometry("01030000E0E610000002000000050000000000000000000000000000000000000000000000000059400000000000006940000000000000000000000000000024400000000000005940000000000000694000000000000024400000000000002440000000000000594000000000000069400000000000002440000000000000000000000000000059400000000000006940000000000000000000000000000000000000000000005940000000000000694005000000000000000000F03F000000000000F03F00000000000059400000000000006940000000000000F03F00000000000022400000000000005940000000000000694000000000000022400000000000002240000000000000594000000000000069400000000000002240000000000000F03F00000000000059400000000000006940000000000000F03F000000000000F03F00000000000059400000000000006940",
        "POLYGON ZM ((0 0 100 200,0 10 100 200,10 10 100 200,10 0 100 200,0 0 100 200),(1 1 100 200,1 9 100 200,9 9 100 200,9 1 100 200,1 1 100 200))");
    // MultiPointZM
    checkWKBGeometry("01040000E0E61000000200000001010000C00000000000000000000000000000F03F0000000000000040000000000000694001010000C00000000000000840000000000000104000000000000014400000000000006940",
        "MULTIPOINT ZM (0 1 2 200,3 4 5 200)");
    // MultiLineStringZM
    checkWKBGeometry("01050000E0E61000000200000001020000C0020000000000000000000000000000000000F03F00000000000000400000000000006940000000000000084000000000000010400000000000001440000000000000694001020000C00200000000000000000018400000000000001C40000000000000204000000000000069400000000000002240000000000000244000000000000026400000000000006940",
        "MULTILINESTRING ZM ((0 1 2 200,3 4 5 200),(6 7 8 200,9 10 11 200))");
    // MultiPolygonZM
    checkWKBGeometry("01060000E0E61000000200000001030000C002000000050000000000000000000000000000000000000000000000000059400000000000006940000000000000000000000000000024400000000000005940000000000000694000000000000024400000000000002440000000000000594000000000000069400000000000002440000000000000000000000000000059400000000000006940000000000000000000000000000000000000000000005940000000000000694005000000000000000000F03F000000000000F03F00000000000059400000000000006940000000000000F03F00000000000022400000000000005940000000000000694000000000000022400000000000002240000000000000594000000000000069400000000000002240000000000000F03F00000000000059400000000000006940000000000000F03F000000000000F03F0000000000005940000000000000694001030000C0010000000500000000000000000022C000000000000000000000000000004940000000000000694000000000000022C0000000000000244000000000000049400000000000006940000000000000F0BF000000000000244000000000000049400000000000006940000000000000F0BF00000000000000000000000000004940000000000000694000000000000022C0000000000000000000000000000049400000000000006940",
        "MULTIPOLYGON ZM (((0 0 100 200,0 10 100 200,10 10 100 200,10 0 100 200,0 0 100 200),(1 1 100 200,1 9 100 200,9 9 100 200,9 1 100 200,1 1 100 200)),((-9 0 50 200,-9 10 50 200,-1 10 50 200,-1 0 50 200,-9 0 50 200)))");
    // GeometryCollectionZM
    //checkWKBGeometry("0107000020E61000000900000001010000000000000000000000000000000000F03F01010000000000000000000000000000000000F03F01010000000000000000000040000000000000084001020000000200000000000000000000400000000000000840000000000000104000000000000014400102000000020000000000000000000000000000000000F03F000000000000004000000000000008400102000000020000000000000000001040000000000000144000000000000018400000000000001C4001030000000200000005000000000000000000000000000000000000000000000000000000000000000000244000000000000024400000000000002440000000000000244000000000000000000000000000000000000000000000000005000000000000000000F03F000000000000F03F000000000000F03F0000000000002240000000000000224000000000000022400000000000002240000000000000F03F000000000000F03F000000000000F03F01030000000200000005000000000000000000000000000000000000000000000000000000000000000000244000000000000024400000000000002440000000000000244000000000000000000000000000000000000000000000000005000000000000000000F03F000000000000F03F000000000000F03F0000000000002240000000000000224000000000000022400000000000002240000000000000F03F000000000000F03F000000000000F03F0103000000010000000500000000000000000022C0000000000000000000000000000022C00000000000002440000000000000F0BF0000000000002440000000000000F0BF000000000000000000000000000022C00000000000000000",
    //    "MULTIPOLYGONM(((0 0 100,0 10 100,10 10 100,10 0 100,0 0 100),(1 1 100,1 9 100,9 9 100,9 1 100,1 1 100)),((-9 0 50,-9 10 50,-1 10 50,-1 0 50,-9 0 50)))");
  }

  /**
   * Not yet implemented satisfactorily.
   * 
   * @throws ParseException
   */
  public void XXtestIllFormedWKB() throws ParseException
  {
    // WKB is missing LinearRing entry
    checkWKBGeometry("00000000030000000140590000000000004069000000000000", "POLYGON ((100 200, 100 200, 100 200, 100 200)");
  }


  private static CoordinateSequenceComparator comp2 = new CoordinateSequenceComparator(2);

  private void checkWKBGeometry(String wkbHex, String expectedWKT) throws ParseException
  {
    WKBReader wkbReader = new WKBReader(geomFactory);
    byte[] wkb = WKBReader.hexToBytes(wkbHex);
    Geometry g2 = wkbReader.read(wkb);
    
    Geometry expected = rdr.read(expectedWKT);
    
   boolean isEqual = (expected.compareTo(g2, comp2) == 0);
    if (!isEqual) {System.out.println(g2);System.out.println(expected);}
    assertTrue(isEqual);

 }
}
