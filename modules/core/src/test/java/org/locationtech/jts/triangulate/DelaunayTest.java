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
package org.locationtech.jts.triangulate;

import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.operation.overlayng.CoverageUnion;

import junit.textui.TestRunner;
import test.jts.GeometryTestCase;

/**
 * Tests Delaunay Triangulation classes
 * 
 */
public class DelaunayTest extends GeometryTestCase {

  public static void main(String args[]) {
    TestRunner.run(DelaunayTest.class);
  }
  
	private GeometryFactory geomFact = new GeometryFactory();

  public DelaunayTest(String name) { super(name); }

  public void testTriangle()
  {
    String wkt = "MULTIPOINT ((10 10 1), (10 20 2), (20 20 3))";
    String expected = "MULTILINESTRING ((10 20, 20 20), (10 10, 10 20), (10 10, 20 20))";
    checkDelaunayEdges(wkt, expected);
    String expectedTri = "GEOMETRYCOLLECTION (POLYGON ((10 20, 10 10, 20 20, 10 20)))";
    checkDelaunay(wkt, true, expectedTri);
  }
  
  public void testRandom()
  {
  	String wkt = "MULTIPOINT ((50 40), (140 70), (80 100), (130 140), (30 150), (70 180), (190 110), (120 20))";
  	String expected = "MULTILINESTRING ((70 180, 190 110), (30 150, 70 180), (30 150, 50 40), (50 40, 120 20), (190 110, 120 20), (120 20, 140 70), (190 110, 140 70), (130 140, 140 70), (130 140, 190 110), (70 180, 130 140), (80 100, 130 140), (70 180, 80 100), (30 150, 80 100), (50 40, 80 100), (80 100, 120 20), (80 100, 140 70))";
  	checkDelaunayEdges(wkt, expected);
  	String expectedTri = "GEOMETRYCOLLECTION (POLYGON ((30 150, 50 40, 80 100, 30 150)), POLYGON ((30 150, 80 100, 70 180, 30 150)), POLYGON ((70 180, 80 100, 130 140, 70 180)), POLYGON ((70 180, 130 140, 190 110, 70 180)), POLYGON ((190 110, 130 140, 140 70, 190 110)), POLYGON ((190 110, 140 70, 120 20, 190 110)), POLYGON ((120 20, 140 70, 80 100, 120 20)), POLYGON ((120 20, 80 100, 50 40, 120 20)), POLYGON ((80 100, 140 70, 130 140, 80 100)))";
  	checkDelaunay(wkt, true, expectedTri);
  }
  
  public void testGrid()
  {
  	String wkt = "MULTIPOINT ((10 10), (10 20), (20 20), (20 10), (20 0), (10 0), (0 0), (0 10), (0 20))";
  	String expected = "MULTILINESTRING ((10 20, 20 20), (0 20, 10 20), (0 10, 0 20), (0 0, 0 10), (0 0, 10 0), (10 0, 20 0), (20 0, 20 10), (20 10, 20 20), (10 20, 20 10), (10 10, 20 10), (10 10, 10 20), (10 10, 0 20), (10 10, 0 10), (10 0, 10 10), (0 10, 10 0), (10 10, 20 0))";
  	checkDelaunayEdges(wkt, expected);
  	String expectedTri = "GEOMETRYCOLLECTION (POLYGON ((0 20, 0 10, 10 10, 0 20)), POLYGON ((0 20, 10 10, 10 20, 0 20)), POLYGON ((10 20, 10 10, 20 10, 10 20)), POLYGON ((10 20, 20 10, 20 20, 10 20)), POLYGON ((10 0, 20 0, 10 10, 10 0)), POLYGON ((10 0, 10 10, 0 10, 10 0)), POLYGON ((10 0, 0 10, 0 0, 10 0)), POLYGON ((10 10, 20 0, 20 10, 10 10)))";
  	checkDelaunay(wkt, true, expectedTri);
  }
  
  public void testCircle()
  {
    String wkt = "POLYGON ((42 30, 41.96 29.61, 41.85 29.23, 41.66 28.89, 41.41 28.59, 41.11 28.34, 40.77 28.15, 40.39 28.04, 40 28, 39.61 28.04, 39.23 28.15, 38.89 28.34, 38.59 28.59, 38.34 28.89, 38.15 29.23, 38.04 29.61, 38 30, 38.04 30.39, 38.15 30.77, 38.34 31.11, 38.59 31.41, 38.89 31.66, 39.23 31.85, 39.61 31.96, 40 32, 40.39 31.96, 40.77 31.85, 41.11 31.66, 41.41 31.41, 41.66 31.11, 41.85 30.77, 41.96 30.39, 42 30))";
    String expected = "MULTILINESTRING ((38 30, 38.04 29.61), (38 30, 38.04 30.39), (38.04 29.61, 38.04 30.39), (38.04 29.61, 38.15 29.23), (38.04 29.61, 38.34 28.89), (38.04 29.61, 38.59 28.59), (38.04 30.39, 38.15 30.77), (38.04 30.39, 38.34 31.11), (38.04 30.39, 38.59 28.59), (38.04 30.39, 38.59 31.41), (38.15 29.23, 38.34 28.89), (38.15 30.77, 38.34 31.11), (38.34 28.89, 38.59 28.59), (38.34 31.11, 38.59 31.41), (38.59 28.59, 38.59 31.41), (38.59 28.59, 38.89 28.34), (38.59 28.59, 39.61 28.04), (38.59 28.59, 40.39 28.04), (38.59 28.59, 41.41 28.59), (38.59 31.41, 38.89 31.66), (38.59 31.41, 39.61 31.96), (38.59 31.41, 40.39 31.96), (38.59 31.41, 41.41 28.59), (38.59 31.41, 41.41 31.41), (38.89 28.34, 39.23 28.15), (38.89 28.34, 39.61 28.04), (38.89 31.66, 39.23 31.85), (38.89 31.66, 39.61 31.96), (39.23 28.15, 39.61 28.04), (39.23 31.85, 39.61 31.96), (39.61 28.04, 40 28), (39.61 28.04, 40.39 28.04), (39.61 31.96, 40 32), (39.61 31.96, 40.39 31.96), (40 28, 40.39 28.04), (40 32, 40.39 31.96), (40.39 28.04, 40.77 28.15), (40.39 28.04, 41.11 28.34), (40.39 28.04, 41.41 28.59), (40.39 31.96, 40.77 31.85), (40.39 31.96, 41.11 31.66), (40.39 31.96, 41.41 31.41), (40.77 28.15, 41.11 28.34), (40.77 31.85, 41.11 31.66), (41.11 28.34, 41.41 28.59), (41.11 31.66, 41.41 31.41), (41.41 28.59, 41.41 31.41), (41.41 28.59, 41.66 28.89), (41.41 28.59, 41.96 29.61), (41.41 31.41, 41.66 31.11), (41.41 31.41, 41.96 29.61), (41.41 31.41, 41.96 30.39), (41.66 28.89, 41.85 29.23), (41.66 28.89, 41.96 29.61), (41.66 31.11, 41.85 30.77), (41.66 31.11, 41.96 30.39), (41.85 29.23, 41.96 29.61), (41.85 30.77, 41.96 30.39), (41.96 29.61, 41.96 30.39), (41.96 29.61, 42 30), (41.96 30.39, 42 30))";
    checkDelaunayEdges(wkt, expected);
  }
  
  public void testPolygonWithChevronHoles()
  {
    String wkt = "POLYGON ((0 0, 0 200, 180 200, 180 0, 0 0), (20 180, 160 180, 160 20, 152.625 146.75, 20 180), (30 160, 150 30, 70 90, 30 160))";
    String expected = "MULTILINESTRING ((0 200, 180 200), (0 0, 0 200), (0 0, 180 0), (180 200, 180 0), (152.625 146.75, 180 0), (152.625 146.75, 180 200), (152.625 146.75, 160 180), (160 180, 180 200), (0 200, 160 180), (20 180, 160 180), (0 200, 20 180), (20 180, 30 160), (30 160, 0 200), (0 0, 30 160), (30 160, 70 90), (0 0, 70 90), (70 90, 150 30), (150 30, 0 0), (150 30, 160 20), (0 0, 160 20), (160 20, 180 0), (152.625 146.75, 160 20), (150 30, 152.625 146.75), (70 90, 152.625 146.75), (30 160, 152.625 146.75), (30 160, 160 180))";
    checkDelaunayEdges(wkt, expected);
  }
  
  // see https://github.com/libgeos/geos/issues/719
  public void testFrameTooSmallBug()
  {
    String wkt = "MULTIPOINT ((0 194), (66 151), (203 80), (273 43), (340 0))";
    String expected = "GEOMETRYCOLLECTION (POLYGON ((0 194, 66 151, 203 80, 0 194)), POLYGON ((0 194, 203 80, 273 43, 0 194)), POLYGON ((273 43, 203 80, 340 0, 273 43)), POLYGON ((340 0, 203 80, 66 151, 340 0)))";
    checkDelaunay(wkt, true, expected);
  }
  
  // see https://github.com/libgeos/geos/issues/719
  public void testNarrow_GEOS_719()
  {
    String wkt = "MULTIPOINT ((1139294.6389832513 8201313.534695469), (1139360.8549531854 8201271.189805277), (1139497.5995843115 8201199.995542546), (1139567.7837303514 8201163.348533507), (1139635.3942210067 8201119.902527407))";
    String expected = "GEOMETRYCOLLECTION (POLYGON ((1139294.6389832513 8201313.534695469, 1139360.8549531854 8201271.189805277, 1139497.5995843115 8201199.995542546, 1139294.6389832513 8201313.534695469)), POLYGON ((1139294.6389832513 8201313.534695469, 1139497.5995843115 8201199.995542546, 1139567.7837303514 8201163.348533507, 1139294.6389832513 8201313.534695469)), POLYGON ((1139567.7837303514 8201163.348533507, 1139497.5995843115 8201199.995542546, 1139635.3942210067 8201119.902527407, 1139567.7837303514 8201163.348533507)), POLYGON ((1139635.3942210067 8201119.902527407, 1139497.5995843115 8201199.995542546, 1139360.8549531854 8201271.189805277, 1139635.3942210067 8201119.902527407)))";
    checkDelaunay(wkt, true, expected);
  }

  
  public void testNarrowTriangle()
  {
    String wkt = "MULTIPOINT ((100 200), (200 190), (300 200))";
    String expected = "GEOMETRYCOLLECTION (POLYGON ((100 200, 300 200, 200 190, 100 200)))";
    checkDelaunay(wkt, true, expected);
  }
  
  // seee https://github.com/locationtech/jts/issues/477
  public void testNarrow_GH477_1()
  {
    String wkt = "MULTIPOINT ((0 0), (1 0), (-1 0.05), (0 0))";
    String expected = "GEOMETRYCOLLECTION (POLYGON ((-1 0.05, 1 0, 0 0, -1 0.05)))";
    checkDelaunay(wkt, true, expected);
  }
  
  // see https://github.com/locationtech/jts/issues/477
  public void testNarrow_GH477_2()
  {
    String wkt = "MULTIPOINT ((0 0), (0 486), (1 486), (1 22), (2 22), (2 0))";
    String expected = "GEOMETRYCOLLECTION (POLYGON ((0 0, 0 486, 1 22, 0 0)), POLYGON ((0 0, 1 22, 2 0, 0 0)), POLYGON ((0 486, 1 486, 1 22, 0 486)), POLYGON ((1 22, 1 486, 2 22, 1 22)), POLYGON ((1 22, 2 22, 2 0, 1 22)))";
    checkDelaunay(wkt, true, expected);
  }
  
  // see https://github.com/libgeos/geos/issues/946
  public void testNarrow_GEOS_946()
  {
    String wkt = "MULTIPOINT ((113.56577197798602 22.80081530883069),(113.565723279387 22.800815316487014),(113.56571548761124 22.80081531771092),(113.56571548780202 22.800815317674463),(113.56577197817877 22.8008153088047),(113.56577197798602 22.80081530883069))";
    String expected = "GEOMETRYCOLLECTION (POLYGON ((113.56571548761124 22.80081531771092, 113.565723279387 22.800815316487014, 113.56571548780202 22.800815317674463, 113.56571548761124 22.80081531771092)), POLYGON ((113.56571548780202 22.800815317674463, 113.565723279387 22.800815316487014, 113.56577197817877 22.8008153088047, 113.56571548780202 22.800815317674463)), POLYGON ((113.565723279387 22.800815316487014, 113.56577197798602 22.80081530883069, 113.56577197817877 22.8008153088047, 113.565723279387 22.800815316487014)))";
    checkDelaunay(wkt, true, expected);
  }
  
  // see https://github.com/shapely/shapely/issues/1873
  public void testNarrow_Shapely_1873()
  {
    String wkt = "MULTIPOINT ((584245.72096874 7549593.72686167), (584251.71398371 7549594.01629478), (584242.72446125 7549593.58214511), (584230.73978847 7549592.9760418), (584233.73581213 7549593.13045099), (584236.7318358 7549593.28486019), (584239.72795377 7549593.43742855), (584227.74314188 7549592.83423486))";
    String expected = "GEOMETRYCOLLECTION (POLYGON ((584227.74314188 7549592.83423486, 584233.73581213 7549593.13045099, 584230.73978847 7549592.9760418, 584227.74314188 7549592.83423486)), POLYGON ((584227.74314188 7549592.83423486, 584236.7318358 7549593.28486019, 584233.73581213 7549593.13045099, 584227.74314188 7549592.83423486)), POLYGON ((584227.74314188 7549592.83423486, 584239.72795377 7549593.43742855, 584236.7318358 7549593.28486019, 584227.74314188 7549592.83423486)), POLYGON ((584230.73978847 7549592.9760418, 584233.73581213 7549593.13045099, 584245.72096874 7549593.72686167, 584230.73978847 7549592.9760418)), POLYGON ((584230.73978847 7549592.9760418, 584245.72096874 7549593.72686167, 584251.71398371 7549594.01629478, 584230.73978847 7549592.9760418)), POLYGON ((584233.73581213 7549593.13045099, 584236.7318358 7549593.28486019, 584242.72446125 7549593.58214511, 584233.73581213 7549593.13045099)), POLYGON ((584233.73581213 7549593.13045099, 584242.72446125 7549593.58214511, 584245.72096874 7549593.72686167, 584233.73581213 7549593.13045099)), POLYGON ((584236.7318358 7549593.28486019, 584239.72795377 7549593.43742855, 584242.72446125 7549593.58214511, 584236.7318358 7549593.28486019)))";
    checkDelaunay(wkt, true, expected);
  }
  
  public void testNarrowPoints()
  {
    String wkt = "MULTIPOINT ((2 204), (3 66), (1 96), (0 236), (3 173), (2 114), (3 201), (0 46), (1 181))";
    checkDelaunayHull(wkt);
  }
  
	static final double COMPARISON_TOLERANCE = 1.0e-7;
	
  void checkDelaunayEdges(String sitesWKT, String expectedWKT)
  {
  	checkDelaunay(sitesWKT, false, expectedWKT);
  }
  	
  void checkDelaunay(String sitesWKT, boolean computeTriangles, String expectedWKT)
  {
  	Geometry sites = read(sitesWKT);
  	DelaunayTriangulationBuilder builder = new DelaunayTriangulationBuilder();
  	builder.setSites(sites);
  	
  	Geometry result = null;
  	if (computeTriangles) {
  		result = builder.getTriangles(geomFact);  		
  	}
  	else {
  		result = builder.getEdges(geomFact);
  	}
  	//System.out.println(result);
  	
  	Geometry expected = read(expectedWKT);
  	result.normalize();
  	expected.normalize();
  	checkEqual(expected, result, COMPARISON_TOLERANCE);
  }
  
  void checkDelaunayHull(String sitesWKT)
  {
    Geometry sites = read(sitesWKT);
    DelaunayTriangulationBuilder builder = new DelaunayTriangulationBuilder();
    builder.setSites(sites);
    
    Geometry result = builder.getTriangles(geomFact);      

    //System.out.println(result);
    
    Geometry union = CoverageUnion.union(result);
    ConvexHull ch = new ConvexHull(result);
    Geometry convexHull = ch.getConvexHull();
    
    //boolean isEqual = union.norm().equalsExact(convexHull.norm());
    boolean isEqual = union.equalsTopo(convexHull);

    assertTrue("hulls do not match", isEqual);
  }
}
