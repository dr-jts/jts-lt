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
package org.locationtech.jts.operation.buffer;

import org.locationtech.jts.geom.LineSegment;

import junit.framework.TestCase;



/**
 * @version 1.7
 */
public class DepthSegmentTest extends TestCase {

  public DepthSegmentTest(String name) {
    super(name);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(DepthSegmentTest.class);
  }

  public void testCompareTipToTail() throws Exception
  {
    SubgraphDepthLocater.DepthSegment ds0 = depthSeg(0.7, 0.2, 1.4, 0.9);
    SubgraphDepthLocater.DepthSegment ds1 = depthSeg(0.7, 0.2, 0.3, 1.1);
    checkCompare(ds0, ds1, 1);
  }

  public void testCompare2() throws Exception
  {
    SubgraphDepthLocater.DepthSegment ds0 = depthSeg(0.5, 1.0, 0.1, 1.9);
    SubgraphDepthLocater.DepthSegment ds1 = depthSeg(1.0, 0.9, 1.9, 1.4);
    checkCompare(ds0, ds1, -1);
  }

  public void testCompareVertical() throws Exception
  {
    SubgraphDepthLocater.DepthSegment ds0 = depthSeg(1, 1, 1, 2);
    SubgraphDepthLocater.DepthSegment ds1 = depthSeg(1, 0, 1, 1);
    checkCompare(ds0, ds1, 1);
  }

  public void testCompareOrientBug() throws Exception
  {
    SubgraphDepthLocater.DepthSegment ds0 = depthSeg(146.268, -8.42361, 146.263, -8.3875);
    SubgraphDepthLocater.DepthSegment ds1 = depthSeg(146.269, -8.42889, 146.268, -8.42361);
    checkCompare(ds0, ds1, -1);
  }
  
  public void testCompareEqual() throws Exception
  {
    SubgraphDepthLocater.DepthSegment ds0 = depthSeg(1, 1, 2, 2);
    checkCompare(ds0, ds0, 0);
  }
  
  private void checkCompare(
     SubgraphDepthLocater.DepthSegment ds0,
     SubgraphDepthLocater.DepthSegment ds1, 
     int expectedComp) 
  {
    assertTrue(ds0.isUpward());
    assertTrue(ds1.isUpward());
    
    // check compareTo contract - should never have ds1 < ds2 && ds2 < ds1
    int comp0 = ds0.compareTo(ds1);
    int comp1 = ds1.compareTo(ds0);
    assertEquals(expectedComp, comp0);
    assertTrue( comp0 == -comp1);
  }

  private SubgraphDepthLocater.DepthSegment depthSeg(double x0, double y0, double x1, double y1) {
    LineSegment seg = new LineSegment(x0,y0,x1,y1);
    // DepthSegment compareTo method assumes upward segments
    if (seg.p0.y > seg.p1.y)
      seg.reverse();
    return new SubgraphDepthLocater.DepthSegment(seg, 0);  
  }

}
