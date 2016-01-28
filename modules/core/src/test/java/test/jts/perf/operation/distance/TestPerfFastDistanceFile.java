/*
 * The JTS Topology Suite is a collection of Java classes that
 * implement the fundamental operations required to validate a given
 * geo-spatial data set to a known topological specification.
 * 
 * Copyright (C) 2016 Martin Davis
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Martin Davis BSD
 * License v1.0 (found at the root of the repository).
 * 
 */

package test.jts.perf.operation.distance;

import java.util.*;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.*;
import org.locationtech.jts.util.Stopwatch;


public class TestPerfFastDistanceFile 
{
  public static void main(String[] args) {
    TestPerfFastDistanceFile test = new TestPerfFastDistanceFile();
    try {
      test.test();
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private static final int MAX_GEOMS = 40;

  boolean testFailed = false;

  public TestPerfFastDistanceFile() {
  }


  public void test()
  throws Exception
{
    
//    List geoms = loadWKT("C:\\data\\martin\\proj\\jts\\sandbox\\jts\\testdata\\africa.wkt");
    List geoms = loadWKT("C:\\data\\martin\\proj\\jts\\sandbox\\jts\\testdata\\world.wkt");
    
//  testAllDistances(geoms, 100);

  testAllDistances(geoms, 1);
  testAllDistances(geoms, 2);
  testAllDistances(geoms, 5);
  testAllDistances(geoms, 10);
  testAllDistances(geoms, 20);
  testAllDistances(geoms, 30);
  testAllDistances(geoms, 40);
  testAllDistances(geoms, 50);
}

  static List loadWKT(String filename) throws Exception {
    WKTReader rdr = new WKTReader();
    WKTFileReader fileRdr = new WKTFileReader(filename, rdr);
    return fileRdr.read();
  }

  void testAllDistances(List geoms, int maxToScan)
  {
    Stopwatch sw = new Stopwatch();
    
    computeAllDistances(geoms, maxToScan);
//  computePairDistance(geoms, 1, 3);
//  computePairDistance(geoms, 55, 77);
    
    System.out.println("Count = " + maxToScan
        + "   Finished in " + sw.getTimeString());    
  }

  void computeAllDistances(List geoms, int maxToScan) {
    int numGeoms1 = geoms.size();
    if (numGeoms1 > maxToScan)
      numGeoms1 = maxToScan;

    int numGeoms2 = geoms.size();

    for (int i = 0; i < numGeoms1; i++) {
      // PreparedGeometry pg = PreparedGeometryFactory.prepare((Geometry)
      // geoms.get(i));
      for (int j = 0; j < numGeoms2; j++) {
        // don't compute distance to itself!
        // if (i == j) continue;

        Geometry g1 = (Geometry) geoms.get(i);
        Geometry g2 = (Geometry) geoms.get(j);

        // if (g1.getEnvelopeInternal().intersects(g2.getEnvelopeInternal()))
        // continue;

//         double dist = g1.distance(g2);
//        double dist = BranchAndBoundFacetDistance.distance(g1, g2);
        double dist = CachedBABDistance.getDistance(g1, g2);
        // double distFast = SortedBoundsFacetDistance.distance(g1, g2);

        // pg.intersects(g2);
      }
    }
  }

  
  static final int MAX_ITER = 10;
  
  void computePairDistance(List geoms, int i, int j) 
  {
    for (int n = 0; n < MAX_ITER; n++ ) {
      Geometry g1 = (Geometry) geoms.get(i);
      Geometry g2 = (Geometry) geoms.get(j);

      double dist = g1.distance(g2);
//      double dist = SortedBoundsFacetDistance.distance(g1, g2);
//      double dist = BranchAndBoundFacetDistance.distance(g1, g2);
    }
  }
  
  

}
  
  
