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

import junit.textui.TestRunner;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

public class SimpleOverlayAreaTest extends BaseOverlayAreaTest {

  public static void main(String args[]) {
    TestRunner.run(SimpleOverlayAreaTest.class);
  }
  
  public SimpleOverlayAreaTest(String name) {
    super(name);
  }

  @Override
  protected double computeOverlayArea(Geometry a, Geometry b) {
    return SimpleOverlayArea.intersectionArea((Polygon) a, (Polygon) b);
  }
}
