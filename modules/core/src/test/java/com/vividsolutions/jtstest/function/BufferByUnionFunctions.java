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
package com.vividsolutions.jtstest.function;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryCollectionIterator;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.util.LinearComponentExtracter;
import com.vividsolutions.jts.noding.SegmentString;
import com.vividsolutions.jts.operation.buffer.BufferInputLineSimplifier;
import com.vividsolutions.jts.operation.buffer.BufferOp;
import com.vividsolutions.jts.operation.buffer.BufferParameters;
import com.vividsolutions.jts.operation.buffer.OffsetCurveSetBuilder;
import com.vividsolutions.jts.operation.buffer.validate.BufferResultValidator;

public class BufferByUnionFunctions {
	
	public static Geometry componentBuffers(Geometry g, double distance)	
	{		
		List bufs = new ArrayList();
		for (Iterator it = new GeometryCollectionIterator(g); it.hasNext(); ) {
			Geometry comp = (Geometry) it.next();
			if (comp instanceof GeometryCollection) continue;
			bufs.add(comp.buffer(distance));
		}
    return FunctionsUtil.getFactoryOrDefault(g)
    				.createGeometryCollection(GeometryFactory.toGeometryArray(bufs));
	}
	
	public static Geometry bufferByComponents(Geometry g, double distance)	
	{
		return componentBuffers(g, distance).union();
	}
	
	/**
	 * Buffer polygons by buffering the individual boundary segments and
	 * either unioning or differencing them.
	 * 
	 * @param g
	 * @param distance
	 * @return the buffer geometry
	 */
  public static Geometry bufferBySegments(Geometry g, double distance)
  {
    Geometry segs = LineHandlingFunctions.extractSegments(g);
    double posDist = Math.abs(distance);
    Geometry segBuf = bufferByComponents(segs, posDist);
    if (distance < 0.0) 
      return g.difference(segBuf);
    return g.union(segBuf);
  }
  
  public static Geometry bufferByChains(Geometry g, double distance, int maxChainSize)
  {
    if (maxChainSize <= 0)
      throw new IllegalArgumentException("Maximum Chain Size must be specified as an input parameter");
    Geometry segs = LineHandlingFunctions.extractChains(g, maxChainSize);
    double posDist = Math.abs(distance);
    Geometry segBuf = bufferByComponents(segs, posDist);
    if (distance < 0.0) 
      return g.difference(segBuf);
    return g.union(segBuf);
  }
}
