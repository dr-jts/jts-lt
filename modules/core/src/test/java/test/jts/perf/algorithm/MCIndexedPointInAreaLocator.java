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
package test.jts.perf.algorithm;

import java.util.*;

import org.locationtech.jts.algorithm.*;
import org.locationtech.jts.algorithm.locate.PointOnGeometryLocator;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.util.*;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.chain.*;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.noding.*;


/**
 * Determines the location of {@link Coordinate}s relative to
 * a {@link Polygonal} geometry, using indexing for efficiency.
 * This algorithm is suitable for use in cases where
 * many points will be tested against a given area.
 * 
 * @author Martin Davis
 *
 */
public class MCIndexedPointInAreaLocator 
	implements PointOnGeometryLocator
{
	private Geometry areaGeom;
	private MCIndexedGeometry index;
	private double maxXExtent;
		
	public MCIndexedPointInAreaLocator(Geometry g)
	{
		areaGeom = g;
		if (! (g instanceof Polygonal))
			throw new IllegalArgumentException("Argument must be Polygonal");
		buildIndex(g);
    Envelope env = g.getEnvelopeInternal();
		maxXExtent = env.getMaxX() + 1.0;
	}
	
	private void buildIndex(Geometry g)
	{
		index = new MCIndexedGeometry(g);
	}
		
  /**
   * Determines the {@link Location} of a point in an areal {@link Geometry}.
   * 
   * @param p the point to test
   * @return the location of the point in the geometry  
   */
	public int locate(Coordinate p)
	{
		RayCrossingCounter rcc = new RayCrossingCounter(p);
		MCSegmentCounter mcSegCounter = new MCSegmentCounter(rcc);
		Envelope rayEnv = new Envelope(p.x, maxXExtent, p.y, p.y);
		List mcs = index.query(rayEnv);
		countSegs(rcc, rayEnv, mcs, mcSegCounter);
		
		return rcc.getLocation();
	}
	
	private void countSegs(RayCrossingCounter rcc, Envelope rayEnv, List monoChains, MCSegmentCounter mcSegCounter)
	{
		for (Iterator i = monoChains.iterator(); i.hasNext(); ) {
			MonotoneChain mc = (MonotoneChain) i.next();
			mc.select(rayEnv, mcSegCounter);
			// short-circuit if possible
			if (rcc.isOnSegment()) return;
		}
	}
	
  static class MCSegmentCounter extends MonotoneChainSelectAction
  {
  	RayCrossingCounter rcc;

    public MCSegmentCounter(RayCrossingCounter rcc)
    {
      this.rcc = rcc;
    }

    public void select(LineSegment ls)
    {
      rcc.countSegment(ls.getCoordinate(0), ls.getCoordinate(1));
    }
  }

}

class MCIndexedGeometry
{
  private SpatialIndex index= new STRtree();

	public MCIndexedGeometry(Geometry geom)
	{
		init(geom);
	}
	
	private void init(Geometry geom)
	{
		List lines = LinearComponentExtracter.getLines(geom);
		for (Iterator i = lines.iterator(); i.hasNext(); ) {
			LineString line = (LineString) i.next();
			Coordinate[] pts = line.getCoordinates();
			addLine(pts);
		}
	}
	
	private void addLine(Coordinate[] pts)
	{
			SegmentString segStr = new BasicSegmentString(pts, null);
	    List segChains = MonotoneChainBuilder.getChains(segStr.getCoordinates(), segStr);
	    for (Iterator i = segChains.iterator(); i.hasNext(); ) {
	      MonotoneChain mc = (MonotoneChain) i.next();
	      index.insert(mc.getEnvelope(), mc);
	    }
	}
	
	public List query(Envelope searchEnv)
	{
		return index.query(searchEnv);
	}
}


