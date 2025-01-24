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
package org.locationtech.jts.densify;

import java.util.Collection;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTFileReader;
import org.locationtech.jts.io.WKTReader;

import junit.framework.TestCase;

/**
 * @author Julien Gaffuri
 *
 */
public class LittleThumblingDensifierTest extends TestCase {
	private final WKTReader wr = new WKTReader();

	public LittleThumblingDensifierTest(String name) { super(name); }


	public static void main(String[] args) {
		junit.textui.TestRunner.run(LittleThumblingDensifierTest.class);
	}

	public void test0() throws Exception{
		Geometry g = wr.read("LINESTRING(0 0, 1 0)");
		for(double step : new double[] {0.1, 0.05, 0.5, 0.7, 0.005}) {
			Geometry g_ = LittleThumblingDensifier.densify(g, step);
			assertEquals(g.getLength(), g_.getLength());
			assertEquals((int)(1.0/step)+1, g_.getNumPoints());
		}
	}

	public void test1() throws Exception{
		Geometry g = LittleThumblingDensifier.densify(wr.read("LINESTRING(0 0, 1 1)"), 0.1);
		assertEquals(Math.sqrt(2), g.getLength());
		assertEquals(15, g.getNumPoints());
	}

	public void test2() throws Exception{
		WKTFileReader wfr = new WKTFileReader("src/test/resources/testdata/plane.wkt", new WKTReader());
		Collection<?> gs = wfr.read();
		Geometry g = (Geometry) gs.iterator().next();
		Geometry g_ = LittleThumblingDensifier.densify(g, 0.1);

		assertTrue(g.getLength()>g_.getLength());
		assertEquals(2612, g_.getNumPoints());
	}

}
