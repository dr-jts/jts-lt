/*
 * Copyright (c) 2016 Martin Davis.
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

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXYZM;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import junit.textui.TestRunner;
import test.jts.GeometryTestCase;

/**
 * Tests for WKB which test output explicitly.
 * 
 * @author Martin Davis
 *
 */
public class WKBWriterTest extends GeometryTestCase {

  public static void main(String args[]) {
    TestRunner.run(WKBWriterTest.class);
  }
  
  public WKBWriterTest(String name) {
      super(name);
  }
  
  public void testSRID() throws Exception {
      GeometryFactory gf = new GeometryFactory();
      Point p1 = gf.createPoint(new Coordinate(1,2));
      p1.setSRID(1234);
      
      //first write out without srid set
      WKBWriter w = new WKBWriter();
      byte[] wkb = w.write(p1);
      
      //check the 3rd bit of the second byte, should be unset
      byte b = (byte) (wkb[1] & 0x20);
      assertEquals(0, b);
      
      //read geometry back in
      WKBReader r = new WKBReader(gf);
      Point p2 = (Point) r.read(wkb);
      
      assertTrue(p1.equalsExact(p2));
      assertEquals(0, p2.getSRID());
      
      //not write out with srid set
      w = new WKBWriter(2, true);
      wkb = w.write(p1);
      
      //check the 3rd bit of the second byte, should be set
      b = (byte) (wkb[1] & 0x20);
      assertEquals(0x20, b);
      
      int srid = ((int) (wkb[5] & 0xff) << 24) | ( (int) (wkb[6] & 0xff) << 16)
          | ( (int) (wkb[7] & 0xff) << 8) | (( int) (wkb[8] & 0xff) );
     
      assertEquals(1234, srid);
      
      r = new WKBReader(gf);
      p2 = (Point) r.read(wkb);
      
      //read the geometry back in
      assertTrue(p1.equalsExact(p2));
      assertEquals(1234, p2.getSRID());
  }
    
  public void testPointEmpty2D() {
    checkWKB("POINT EMPTY", 2, "0101000000000000000000F87F000000000000F87F" );    
  }
  
  public void testPointEmpty3D() {
    checkWKB("POINT EMPTY", 3, "0101000080000000000000F87F000000000000F87F000000000000F87F" );    
  }
  
  public void testPolygonEmpty2DSRID() {
    checkWKB("POLYGON EMPTY", 2, ByteOrderValues.LITTLE_ENDIAN, 4326, "0103000020E610000000000000" );    
  }
  
  public void testPolygonEmpty2D() {
    checkWKB("POLYGON EMPTY", 2, "010300000000000000" );    
  }
  
  public void testPolygonEmpty3D() {
    checkWKB("POLYGON EMPTY", 3, "010300008000000000" );    
  }

  public void testMultiPolygonEmpty2D() {
    checkWKB("MULTIPOLYGON EMPTY", 2, "010600000000000000");
  }

  public void testMultiPolygonEmpty3D() {
    checkWKB("MULTIPOLYGON EMPTY", 3, "010600008000000000");
  }

  public void testMultiPolygonEmpty2DSRID() {
    checkWKB("MULTIPOLYGON EMPTY", 2, ByteOrderValues.LITTLE_ENDIAN, 4326, "0106000020E610000000000000");
  }

  public void testMultiPolygon() {
    checkWKB(
        "MULTIPOLYGON(((0 0,0 10,10 10,10 0,0 0),(1 1,1 9,9 9,9 1,1 1)),((-9 0,-9 10,-1 10,-1 0,-9 0)))",
        2,
        ByteOrderValues.LITTLE_ENDIAN,
        4326,
        "0106000020E61000000200000001030000000200000005000000000000000000000000000000000000000000000000000000000000000000244000000000000024400000000000002440000000000000244000000000000000000000000000000000000000000000000005000000000000000000F03F000000000000F03F000000000000F03F0000000000002240000000000000224000000000000022400000000000002240000000000000F03F000000000000F03F000000000000F03F0103000000010000000500000000000000000022C0000000000000000000000000000022C00000000000002440000000000000F0BF0000000000002440000000000000F0BF000000000000000000000000000022C00000000000000000");
  }

  public void testGeometryCollection() {
    checkWKB(
        "GEOMETRYCOLLECTION(POINT(0 1),POINT(0 1),POINT(2 3),LINESTRING(2 3,4 5),LINESTRING(0 1,2 3),LINESTRING(4 5,6 7),POLYGON((0 0,0 10,10 10,10 0,0 0),(1 1,1 9,9 9,9 1,1 1)),POLYGON((0 0,0 10,10 10,10 0,0 0),(1 1,1 9,9 9,9 1,1 1)),POLYGON((-9 0,-9 10,-1 10,-1 0,-9 0)))",
        2,
        ByteOrderValues.LITTLE_ENDIAN,
        4326,
        "0107000020E61000000900000001010000000000000000000000000000000000F03F01010000000000000000000000000000000000F03F01010000000000000000000040000000000000084001020000000200000000000000000000400000000000000840000000000000104000000000000014400102000000020000000000000000000000000000000000F03F000000000000004000000000000008400102000000020000000000000000001040000000000000144000000000000018400000000000001C4001030000000200000005000000000000000000000000000000000000000000000000000000000000000000244000000000000024400000000000002440000000000000244000000000000000000000000000000000000000000000000005000000000000000000F03F000000000000F03F000000000000F03F0000000000002240000000000000224000000000000022400000000000002240000000000000F03F000000000000F03F000000000000F03F01030000000200000005000000000000000000000000000000000000000000000000000000000000000000244000000000000024400000000000002440000000000000244000000000000000000000000000000000000000000000000005000000000000000000F03F000000000000F03F000000000000F03F0000000000002240000000000000224000000000000022400000000000002240000000000000F03F000000000000F03F000000000000F03F0103000000010000000500000000000000000022C0000000000000000000000000000022C00000000000002440000000000000F0BF0000000000002440000000000000F0BF000000000000000000000000000022C00000000000000000");
  }

  public void testWkbLineStringM() {
    checkWKB(
        "LINESTRING M(1 2 3, 5 6 7)",
        4,
        ByteOrderValues.LITTLE_ENDIAN,
        -1,
        "010200004002000000000000000000F03F00000000000000400000000000000840000000000000144000000000000018400000000000001C40");
  }
  
  public void testWkbLineStringZM() throws ParseException {
      LineString lineZM = new GeometryFactory().createLineString(new Coordinate[]{new CoordinateXYZM(1,2,3,4), new CoordinateXYZM(5,6,7,8)});
      byte[] write = new WKBWriter(4).write(lineZM);

      LineString lineZMRead = (LineString) new WKBReader().read(write);

      assertEquals(lineZM, lineZMRead);

      assertEquals(1.0, lineZMRead.getPointN(0).getCoordinate().getX());
      assertEquals(2.0, lineZMRead.getPointN(0).getCoordinate().getY());
      assertEquals(3.0, lineZMRead.getPointN(0).getCoordinate().getZ());
      assertEquals(4.0, lineZMRead.getPointN(0).getCoordinate().getM());

      assertEquals(5.0, lineZMRead.getPointN(1).getCoordinate().getX());
      assertEquals(6.0, lineZMRead.getPointN(1).getCoordinate().getY());
      assertEquals(7.0, lineZMRead.getPointN(1).getCoordinate().getZ());
      assertEquals(8.0, lineZMRead.getPointN(1).getCoordinate().getM());
  }

  void checkWKB(String wkt, int dimension, String expectedWKBHex) {
    checkWKB(wkt, dimension, ByteOrderValues.LITTLE_ENDIAN, -1, expectedWKBHex);
  }
    
  void checkWKB(String wkt, int dimension, int byteOrder, int srid, String expectedWKBHex) {
    Geometry geom = read(wkt);
    
    // set SRID if not -1
    boolean includeSRID = false;
    if (srid >= 0) {
      includeSRID = true;
      geom.setSRID(srid);
    }
    
    WKBWriter wkbWriter = new WKBWriter(dimension, byteOrder, includeSRID);
    byte[] wkb = wkbWriter.write(geom);
    String wkbHex = WKBWriter.toHex(wkb);
    
    assertEquals(expectedWKBHex, wkbHex);
  }
}
