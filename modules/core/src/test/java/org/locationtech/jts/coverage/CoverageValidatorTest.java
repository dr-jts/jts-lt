/*
 * Copyright (c) 2022 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.coverage;

import org.locationtech.jts.geom.Geometry;

import junit.textui.TestRunner;
import test.jts.GeometryTestCase;

public class CoverageValidatorTest extends GeometryTestCase 
{
  
  public static void main(String args[]) {
    TestRunner.run(CoverageValidatorTest.class);
  }
  
  public CoverageValidatorTest(String name) {
    super(name);
  }
  
  //========  Invalid cases   =============================

  public void testCollinearUnmatchedEdge() {
    checkInvalid(readArray(
        "POLYGON ((100 200, 200 200, 200 100, 100 100, 100 200))",
        "POLYGON ((100 300, 180 300, 180 200, 100 200, 100 300))"),
        readArray(
            "LINESTRING (100 200, 200 200)",
            null)
            );
  }
  
  public void testOverlappingSquares() {
    checkInvalid(readArray(
        "POLYGON ((1 9, 6 9, 6 4, 1 4, 1 9))",
        "POLYGON ((9 1, 4 1, 4 6, 9 6, 9 1))"),
        readArray(
            "LINESTRING (6 9, 6 4, 1 4)",
            "LINESTRING (4 1, 4 6, 9 6)")
            );
  }
  
  //========  Valid cases   =============================

  public void testChessboard() {
    checkInvalid(readArray(
        "POLYGON ((1 9, 5 9, 5 5, 1 5, 1 9))",
        "POLYGON ((9 9, 9 5, 5 5, 5 9, 9 9))",
        "POLYGON ((1 1, 1 5, 5 5, 5 1, 1 1))",
        "POLYGON ((9 1, 5 1, 5 5, 9 5, 9 1))" ),
        readArray(null, null, null, null)
            );
  }

  //------------------------------------------------------------
  
  private void checkInvalid(Geometry[] coverage, Geometry[] expected) {
    Geometry[] actual = CoverageValidator.validate(coverage);
    checkEqual(expected, actual);
  }

  private Geometry[] readArray(String... wkt) {
    Geometry[] geoms = new Geometry[wkt.length];
    for (int i = 0; i < wkt.length; i++) {
      geoms[i] = wkt[i] == null ? null : read(wkt[i]);
    }
    return geoms;
  }
}
