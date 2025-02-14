/*
 * Copyright (c) 2020 Martin Davis
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

import junit.textui.TestRunner;
import org.locationtech.jts.geom.Polygon;
import test.jts.GeometryTestCase;

public class OverlayAreaTest extends BaseOverlayAreaTest {

  public static void main(String args[]) {
    TestRunner.run(OverlayAreaTest.class);
  }
  
  public OverlayAreaTest(String name) {
    super(name);
  }

  public void testAOverlapBWithHole() {
    checkIntersectionArea(
        "POLYGON ((100 300, 305 299, 150 200, 300 150, 150 100, 300 50, 100 50, 100 300))",
        "POLYGON ((185 206, 350 206, 350 100, 185 100, 185 206), (230 190, 310 190, 310 120, 230 120, 230 190))");
  }

  public void testAOverlapBMulti() {
    checkIntersectionArea(
        "POLYGON ((50 250, 250 250, 250 50, 50 50, 50 250))",
        "MULTIPOLYGON (((100 200, 100 100, 0 100, 0 200, 100 200)), ((200 200, 300 200, 300 100, 200 100, 200 200)))");
  }

  public void testAOverlapBMultiHole() {
    checkIntersectionArea(
        "POLYGON ((60 200, 250 280, 111 135, 320 120, 50 40, 30 120, 60 200))",
        "MULTIPOLYGON (((55 266, 150 150, 170 290, 55 266)), ((100 0, 70 130, 260 160, 291 45, 100 0), (150 40, 125 98, 220 110, 150 40)))");
  }

  @Override
  protected double computeOverlayArea(Geometry a, Geometry b) {
    return new OverlayArea(a).intersectionArea(b);
  }
}
