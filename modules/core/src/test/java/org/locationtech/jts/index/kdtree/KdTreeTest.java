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

package org.locationtech.jts.index.kdtree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateArrays;
import org.locationtech.jts.geom.Envelope;

import junit.framework.TestCase;
import junit.textui.TestRunner;
import test.jts.util.IOUtil;

public class KdTreeTest extends TestCase {
  public static void main(String args[]) {
    TestRunner.run(KdTreeTest.class);
  }

  public KdTreeTest(String name) {
    super(name);
  }

  public void testSinglePoint() {
    KdTree index = new KdTree(.001);

    KdNode node1 = index.insert(new Coordinate(1, 1));
    KdNode node2 = index.insert(new Coordinate(1, 1));

    assertTrue("Inserting 2 identical points should create one node",
        node1 == node2);

    Envelope queryEnv = new Envelope(0, 10, 0, 10);

    List result = index.query(queryEnv);
    assertTrue(result.size() == 1);

    KdNode node = (KdNode) result.get(0);
    assertTrue(node.getCount() == 2);
    assertTrue(node.isRepeated());
  }

  public void testMultiplePoint() {
    testQuery("MULTIPOINT ( (1 1), (2 2) )", 0,
        new Envelope(0, 10, 0, 10), 
        "MULTIPOINT ( (1 1), (2 2) )");
  }

  public void testSubset() {
    testQuery("MULTIPOINT ( (1 1), (2 2), (3 3), (4 4) )", 
        0, 
        new Envelope(1.5, 3.4, 1.5, 3.5),
        "MULTIPOINT ( (2 2), (3 3) )");
  }

  public void testToleranceFailure() {
    testQuery("MULTIPOINT ( (0 0), (-.1 1), (.1 1) )", 
        1, 
        new Envelope(-9, 9, -9, 9),
        "MULTIPOINT ( (0 0), (-.1 1) )");
  }
  
  public void testTolerance2() {
    testQuery("MULTIPOINT ((10 60), (20 60), (30 60), (30 63))", 
        9, 
        new Envelope(0,99, 0, 99),
        "MULTIPOINT ((10 60), (20 60), (30 60))");
  }
  
  public void testTolerance2_perturbedY() {
    testQuery("MULTIPOINT ((10 60), (20 61), (30 60), (30 63))", 
        9, 
        new Envelope(0,99, 0, 99),
        "MULTIPOINT ((10 60), (20 61), (30 60))");
  }
  
  public void testSnapToNearest() {
    testQueryRepeated("MULTIPOINT ( (10 60), (20 60), (16 60))", 
        5, 
        new Envelope(0,99, 0, 99),
        "MULTIPOINT ( (10 60), (20 60), (20 60))");
  }
  
  public void testSizeDepth() {
    KdTree index = build("MULTIPOINT ( (10 60), (20 60), (16 60), (1 1), (23 400))", 
        0);
    int size = index.size();
    assertEquals(5, size);
    int depth = index.depth();
    // these are weak conditions, but depth varies depending on data and algorithm
    assertTrue( depth > 1 );
    assertTrue( depth <= size );
  }
  
  public void testNearestNeighbor() {
      int n = 1000;
      int queries = 500;
      KdTree tree = new KdTree();
      Random rand = new Random(1337);

      for (int i = 0; i < n; i++) {
          double x = rand.nextDouble();
          double y = rand.nextDouble();
          tree.insert(new Coordinate(x, y));
      }

      for (int i = 0; i < queries; i++) {
          double queryX = rand.nextDouble();
          double queryY = rand.nextDouble();
          Coordinate query = new Coordinate(queryX, queryY);

          KdNode nearestNode = tree.nearestNeighbor(query);

          Coordinate bruteForceNearest = bruteForceNearestNeighbor(tree, query);
          
          assertEquals(nearestNode.getCoordinate(), bruteForceNearest);
      }
  }
  
  public void testNearestNeighbors() {
      int n = 2500;
      int numTrials = 50;
      Random rand = new Random(0);

      for (int trial = 0; trial < numTrials; trial++) {
          KdTree tree = new KdTree();

          for (int i = 0; i < n; i++) {
              double x = rand.nextDouble();
              double y = rand.nextDouble();
              tree.insert(new Coordinate(x, y));
          }

          Coordinate query = new Coordinate(rand.nextDouble(), rand.nextDouble());
          int k = rand.nextInt(n/10);
          
          List<KdNode> nearestNodes = tree.nearestNeighbors(query, k);

          List<Coordinate> bruteForceNearest = bruteForceNearestNeighbors(tree, query, k);

          assertEquals(k, nearestNodes.size());
          for (int i = 0; i < k; i++) {
              assertEquals(bruteForceNearest.get(i), nearestNodes.get(i).getCoordinate());
          }
      }
  }
  

  
  public void testCollectNodes() {
      int n = 1000;
      KdTree tree = new KdTree();
      Random rand = new Random(1337);

      for (int i = 0; i < n; i++) {
          double x = rand.nextDouble();
          double y = rand.nextDouble();
          tree.insert(new Coordinate(x, y));
      }
      
      assertEquals(n, tree.getNodes().size());
  }
  
  private void testQuery(String wktInput, double tolerance,
      Envelope queryEnv, String wktExpected) {
    KdTree index = build(wktInput, tolerance);
    testQuery(
        index,
        queryEnv, false,
        IOUtil.read(wktExpected).getCoordinates());
  }

  private void testQueryRepeated(String wktInput, double tolerance,
      Envelope queryEnv, String wktExpected) {
    KdTree index = build(wktInput, tolerance);
    testQuery(
        index,
        queryEnv, true,
        IOUtil.read(wktExpected).getCoordinates());
  }

  private void testQuery(KdTree index,
      Envelope queryEnv, Coordinate[] expectedCoord) {
    Coordinate[] result = KdTree.toCoordinates(index.query(queryEnv));

    Arrays.sort(result);
    Arrays.sort(expectedCoord);
    
    assertTrue("Result count = " + result.length + ", expected count = " + expectedCoord.length,
        result.length == expectedCoord.length);
    
    boolean isMatch = CoordinateArrays.equals(result, expectedCoord);
    assertTrue("Expected result coordinates not found", isMatch);
  }

  private void testQuery(KdTree index,
      Envelope queryEnv, boolean includeRepeated, Coordinate[] expectedCoord) {
    Coordinate[] result = KdTree.toCoordinates(index.query(queryEnv), includeRepeated);

    Arrays.sort(result);
    Arrays.sort(expectedCoord);
    
    assertTrue("Result count = " + result.length + ", expected count = " + expectedCoord.length,
        result.length == expectedCoord.length);
    
    boolean isMatch = CoordinateArrays.equals(result, expectedCoord);
    assertTrue("Expected result coordinates not found", isMatch);
    
    // test queries for points
    for (int i = 0; i < expectedCoord.length; i++) {
      Coordinate p = expectedCoord[i];
      KdNode node = index.query(p);
      assertEquals("Point query not found", node.getCoordinate(), p);
    }
  }
  
  // Helper method to find the nearest neighbor using brute-force
  private Coordinate bruteForceNearestNeighbor(KdTree tree, Coordinate query) {
      List<Coordinate> allPoints = getAllPoints(tree);
      Coordinate nearest = null;
      double minDistance = Double.POSITIVE_INFINITY;

      for (Coordinate point : allPoints) {
          double distance = query.distance(point);
          if (distance < minDistance) {
              minDistance = distance;
              nearest = point;
          }
      }

      return nearest;
  }
  
  private List<Coordinate> bruteForceNearestNeighbors(KdTree tree, Coordinate query, int k) {
      List<Coordinate> allPoints = getAllPoints(tree);

      // Sort all points by distance to the query point
      allPoints.sort(Comparator.comparingDouble(point -> query.distance(point)));

      // Return the first k points (ordered closest first)
      return allPoints.subList(0, Math.min(k, allPoints.size()));
  }
  
  private List<Coordinate> getAllPoints(KdTree tree) {
	  return Arrays.stream(KdTree.toCoordinates(tree.getNodes())).collect(Collectors.toList());
  }

  private KdTree build(String wktInput, double tolerance) {
    final KdTree index = new KdTree(tolerance);
    Coordinate[] coords = IOUtil.read(wktInput).getCoordinates();
    for (int i = 0; i < coords.length; i++) {
      index.insert(coords[i]);
    }
    return index;
  }

}
