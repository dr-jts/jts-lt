/*
 * Copyright (c) 2016 Vivid Solutions.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.simplify;

import java.util.Collection;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTFileReader;
import org.locationtech.jts.io.WKTReader;

import junit.framework.TestCase;

/**
 * @author Julien Gaffuri
 *
 */
public class GaussianLineSmoothingTest extends TestCase {
	private final WKTReader wr = new WKTReader();

	public GaussianLineSmoothingTest(String name) { super(name); }

	public static void main(String[] args) {
		junit.textui.TestRunner.run(GaussianLineSmoothingTest.class);
	}

	public void testEmptyLine() throws Exception {
		LineString gIn = new GeometryFactory().createLineString();
		LineString gOut = GaussianLineSmoothing.get(gIn, 10, 0.1 );
		assertTrue(gOut.isEmpty());
	}

	public void test1() throws Exception{
		WKTFileReader wfr = new WKTFileReader("src/test/resources/testdata/plane.wkt", wr);
		Collection<?> gs = wfr.read();
		LineString ls = (LineString) gs.iterator().next();
		for(double sigmaM : new double[]{1,2,4,6,8,10,20,30,40,50,60,70,80,90,100,150,200,100000}){
			LineString ls_ = GaussianLineSmoothing.get(ls, sigmaM, 0.1);
			assertTrue(ls_.getLength()<ls.getLength());
			assertTrue(ls_.getCoordinateN(0).distance(ls.getCoordinateN(0)) == 0);
			assertTrue(ls_.getCoordinateN(ls_.getNumPoints()-1).distance(ls.getCoordinateN(ls.getNumPoints()-1)) == 0);
		}
	}

	public void test2() throws Exception{
		WKTFileReader wfr = new WKTFileReader("src/test/resources/testdata/world.wkt", wr);
		Collection<?> gs = wfr.read();
		for(Object g_ : gs) {
			LineString ls = null;
			if(g_ instanceof Polygon)
				ls = ((Polygon)g_).getExteriorRing();
			else if(g_ instanceof MultiPolygon)
				ls = ((Polygon)((MultiPolygon)g_).getGeometryN(0)).getExteriorRing();
			else
				continue;
			for(double sigmaM : new double[]{0.01, 0.05, 0.1, 0.5, 1, 100, 100000}){
				LineString ls_ = GaussianLineSmoothing.get(ls, sigmaM, 0.1);
				assertTrue(ls_.getLength()<ls.getLength());
			}
		}
	}

}
