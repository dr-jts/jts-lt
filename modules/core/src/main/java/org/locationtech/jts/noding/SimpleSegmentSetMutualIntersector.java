/*
 * The JTS Topology Suite is a collection of Java classes that
 * implement the fundamental operations required to validate a given
 * geo-spatial data set to a known topological specification.
 * 
 * Copyright (C) 2016 Vivid Solutions
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Vivid Solutions BSD
 * License v1.0 (found at the root of the repository).
 * 
 */
package org.locationtech.jts.noding;

import java.util.*;

import org.locationtech.jts.geom.Coordinate;



/**
 * Intersects two sets of {@link SegmentString}s using 
 * brute-force comparison.
 *
 * @version 1.7
 */
public class SimpleSegmentSetMutualIntersector implements SegmentSetMutualIntersector
{
  private final Collection baseSegStrings;

  /**
   * Constructs a new intersector for a given set of {@link SegmentStrings}.
   * 
   * @param baseSegStrings the base segment strings to intersect
   */
  public SimpleSegmentSetMutualIntersector(Collection segStrings)
  {
	  this.baseSegStrings = segStrings;
  }

  /**
   * Calls {@link SegmentIntersector#processIntersections(SegmentString, int, SegmentString, int)} 
   * for all <i>candidate</i> intersections between
   * the given collection of SegmentStrings and the set of base segments. 
   * 
   * @param a set of segments to intersect
   * @param the segment intersector to use
   */
  public void process(Collection segStrings, SegmentIntersector segInt) {
    for (Iterator i = baseSegStrings.iterator(); i.hasNext(); ) {
    	SegmentString baseSS = (SegmentString) i.next();
    	for (Iterator j = segStrings.iterator(); j.hasNext(); ) {
	      	SegmentString ss = (SegmentString) j.next();
	      	intersect(baseSS, ss, segInt);
	        if (segInt.isDone()) 
	        	return;
    	}
    }
  }

  /**
   * Processes all of the segment pairs in the given segment strings
   * using the given SegmentIntersector.
   * 
   * @param ss0 a Segment string
   * @param ss1 a segment string
   * @param segInt the segment intersector to use
   */
  private void intersect(SegmentString ss0, SegmentString ss1, SegmentIntersector segInt)
  {
    Coordinate[] pts0 = ss0.getCoordinates();
    Coordinate[] pts1 = ss1.getCoordinates();
    for (int i0 = 0; i0 < pts0.length - 1; i0++) {
      for (int i1 = 0; i1 < pts1.length - 1; i1++) {
        segInt.processIntersections(ss0, i0, ss1, i1);
        if (segInt.isDone()) 
        	return;
      }
    }

  }

}
