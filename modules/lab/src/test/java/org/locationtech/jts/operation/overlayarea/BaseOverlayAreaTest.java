/*
 * Copyright (c) 2022 Martin Davis
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.operation.overlayarea;

import org.locationtech.jts.geom.Geometry;
import test.jts.GeometryTestCase;

/**
 * Base class for testing overlay area algorithms
 * by comparing them to the standard overlay intersection area.
 * This is a parameterized test class.
 * Subclasses provide the overlay area algorithm to test.
 */
public abstract class BaseOverlayAreaTest extends GeometryTestCase {

  public BaseOverlayAreaTest(String name) {
    super(name);
  }

  public void testDisjoint() {
    checkIntersectionAreaSymmetric(
        "POLYGON ((10 90, 40 90, 40 60, 10 60, 10 90))",
        "POLYGON ((90 10, 50 10, 50 50, 90 50, 90 10))");
  }

  public void testTouching() {
    checkIntersectionAreaSymmetric(
        "POLYGON ((10 90, 50 90, 50 50, 10 50, 10 90))",
        "POLYGON ((90 10, 50 10, 50 50, 90 50, 90 10))");
  }

  public void testTouchingNonPerpendicular() {
    checkIntersectionAreaSymmetric(
            "POLYGON ((10 90, 49 90, 50 50, 10 53, 10 90))",
            "POLYGON ((90 10, 47 10, 50 50, 90 54, 90 10))");
  }

  public void testRectangleAContainsB() {
    checkIntersectionAreaSymmetric(
        "POLYGON ((100 300, 300 300, 300 100, 100 100, 100 300))",
        "POLYGON ((150 250, 250 250, 250 150, 150 150, 150 250))");
  }

  public void testTriangleAContainsB() {
    checkIntersectionAreaSymmetric(
        "POLYGON ((60 170, 270 370, 380 60, 60 170))",
        "POLYGON ((200 250, 245 155, 291 195, 200 250))");
  }

  public void testRectangleOverlap() {
    checkIntersectionAreaSymmetric(
        "POLYGON ((100 200, 200 200, 200 100, 100 100, 100 200))",
        "POLYGON ((250 250, 250 150, 150 150, 150 250, 250 250))");
  }

  public void testRectangleTriangleOverlap() {
    checkIntersectionAreaSymmetric(
        "POLYGON ((100 200, 200 200, 200 100, 100 100, 100 200))",
        "POLYGON ((300 200, 150 150, 300 100, 300 200))");
  }

  public void testSawOverlap() {
    checkIntersectionAreaSymmetric(
        "POLYGON ((100 300, 305 299, 150 200, 300 150, 150 100, 300 50, 100 50, 100 300))",
        "POLYGON ((400 350, 150 250, 350 200, 200 150, 350 100, 180 50, 400 50, 400 350))");
  }

  public void testVertexIntersectionContained() {
    checkIntersectionAreaSymmetric(
        "POLYGON ((10 10, 40 50, 90 10, 10 10))",
            "POLYGON ((30 20, 40 50, 60 20, 30 20))");
  }

  public void testVertexIntersectionContained2() {
    checkIntersectionAreaSymmetric(
            "POLYGON ((10 10, 40 50, 90 10, 10 10))",
            "POLYGON ((30 10, 40 50, 60 10, 30 10))");
  }

  public void testVertexIntersectionContained3() {
    checkIntersectionAreaSymmetric(
            "POLYGON ((10 10, 40 50, 90 10, 10 10))",
            "POLYGON ((10 10, 40 50, 60 20, 10 10))");
  }

  public void testVertexIntersectionContained4() {
    checkIntersectionAreaSymmetric(
            "POLYGON ((10 10, 10 50, 50 50, 50 10, 10 10))",
            "POLYGON ((10 10, 30 30, 50 10, 10 10))");
  }

  public void testVertexIntersectionOverlapping() {
    checkIntersectionAreaSymmetric(
            "POLYGON ((10 10, 40 50, 90 10, 10 10))",
            "POLYGON ((30 -20, 40 50, 60 -20, 30 -20))");
  }

  public void testVertexIntersectionOverlapping2() {
    checkIntersectionAreaSymmetric(
            "POLYGON ((10 10, 40 50, 90 10, 10 10))",
            "POLYGON ((30 20, 40 50, 90 20, 30 20))");
  }

  public void testVertexIntersectionTwoAreas() {
    checkIntersectionAreaSymmetric(
            "POLYGON ((10 10, 30 50, 60 10, 10 10))",
            "POLYGON ((20 60, 40 60, 40 20, 30 50, 20 20, 20 60))");
  }

  public void testVertexIntersectionOnEdge() {
    checkIntersectionAreaSymmetric(
            "POLYGON ((10 10, 10 40, 40 40, 40 10, 10 10))",
            "POLYGON ((20 30, 20 40, 70 50, 20 30))");
  }

  public void testVertexIntersectionOnEdge2() {
    checkIntersectionAreaSymmetric(
            "POLYGON ((10 30, 10 60, 40 60, 40 30, 10 30))",
            "POLYGON ((40 10, 20 30, 40 50, 50 30, 40 10))");
  }

  public void testCollinearOverlappingEdgesPartial() {
    checkIntersectionAreaSymmetric(
        "POLYGON ((10 30, 30 30, 30 10, 10 10, 10 30))",
        "POLYGON ((20 30, 40 30, 40 10, 20 10, 20 30))"
    );
  }

  public void testCollinearOverlappingEdgesFull() {
    checkIntersectionAreaSymmetric(
        "POLYGON ((10 30, 50 30, 50 10, 10 10, 10 30))",
        "POLYGON ((20 30, 40 30, 40 10, 20 10, 20 30))"
    );
  }

  protected final void checkIntersectionArea(String wktA, String wktB) {
    Geometry a = read(wktA);
    Geometry b = read(wktB);
    
    double intAreaFull = a.intersection(b).getArea();
    
    assertEquals(intAreaFull, computeOverlayArea(a, b), 0.0001);
    assertEquals(intAreaFull, computeOverlayArea(a.reverse(), b), 0.0001);
    assertEquals(intAreaFull, computeOverlayArea(a, b.reverse()), 0.0001);
    assertEquals(intAreaFull, computeOverlayArea(a.reverse(), b.reverse()), 0.0001);
  }

  protected final void checkIntersectionAreaSymmetric(String wktA, String wktB) {
    checkIntersectionArea(wktA, wktB);
    checkIntersectionArea(wktB, wktA);
  }

  abstract protected double computeOverlayArea(Geometry a, Geometry b);
}
