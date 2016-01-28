
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
package org.locationtech.jts.precision;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.precision.GeometryPrecisionReducer;

import junit.framework.TestCase;
import junit.textui.TestRunner;


/**
 * @version 1.12
 */
public class GeometryPrecisionReducerTest
    extends TestCase
{
  private PrecisionModel pmFloat = new PrecisionModel();
  private PrecisionModel pmFixed1 = new PrecisionModel(1);
  private GeometryPrecisionReducer reducer = new GeometryPrecisionReducer(pmFixed1);
  private GeometryPrecisionReducer reducerKeepCollapse
  = new GeometryPrecisionReducer(pmFixed1);

  private GeometryFactory gfFloat = new GeometryFactory(pmFloat, 0);
  WKTReader reader = new WKTReader(gfFloat);

  public static void main(String args[]) {
    TestRunner.run(GeometryPrecisionReducerTest.class);
  }

  public GeometryPrecisionReducerTest(String name)
  {
    super(name);
    reducerKeepCollapse.setRemoveCollapsedComponents(false);
  }

  public void testSquare()
      throws Exception
  {
    Geometry g = reader.read("POLYGON (( 0 0, 0 1.4, 1.4 1.4, 1.4 0, 0 0 ))");
    Geometry g2 = reader.read("POLYGON (( 0 0, 0 1, 1 1, 1 0, 0 0 ))");
    Geometry gReduce = reducer.reduce(g);
    assertEqualsExactAndHasSameFactory(gReduce, g2);
  }
  public void testTinySquareCollapse()
      throws Exception
  {
    Geometry g = reader.read("POLYGON (( 0 0, 0 .4, .4 .4, .4 0, 0 0 ))");
    Geometry g2 = reader.read("POLYGON EMPTY");
    Geometry gReduce = reducer.reduce(g);
    assertEqualsExactAndHasSameFactory(gReduce, g2);
  }
  
  public void testSquareCollapse()
      throws Exception
  {
    Geometry g = reader.read("POLYGON (( 0 0, 0 1.4, .4 .4, .4 0, 0 0 ))");
    Geometry g2 = reader.read("POLYGON EMPTY");
    Geometry gReduce = reducer.reduce(g);
    assertEqualsExactAndHasSameFactory(gReduce, g2);
  }
  
  public void testSquareKeepCollapse()
      throws Exception
  {
    Geometry g = reader.read("POLYGON (( 0 0, 0 1.4, .4 .4, .4 0, 0 0 ))");
    Geometry g2 = reader.read("POLYGON EMPTY");
    Geometry gReduce = reducerKeepCollapse.reduce(g);
    assertEqualsExactAndHasSameFactory(gReduce, g2);
  }
  
  public void testLine()
      throws Exception
  {
    Geometry g = reader.read("LINESTRING ( 0 0, 0 1.4 )");
    Geometry g2 = reader.read("LINESTRING (0 0, 0 1)");
    Geometry gReduce = reducer.reduce(g);
    assertEqualsExactAndHasSameFactory(gReduce, g2);
  }
  
  public void testLineRemoveCollapse()
      throws Exception
  {
    Geometry g = reader.read("LINESTRING ( 0 0, 0 .4 )");
    Geometry g2 = reader.read("LINESTRING EMPTY");
    Geometry gReduce = reducer.reduce(g);
    assertEqualsExactAndHasSameFactory(gReduce, g2);
  }
  
  public void testLineKeepCollapse()
      throws Exception
  {
    Geometry g = reader.read("LINESTRING ( 0 0, 0 .4 )");
    Geometry g2 = reader.read("LINESTRING ( 0 0, 0 0 )");
    Geometry gReduce = reducerKeepCollapse.reduce(g);
    assertEqualsExactAndHasSameFactory(gReduce, g2);
  }
  
  public void testPolgonWithCollapsedLine() throws Exception {
		Geometry g  = reader.read("POLYGON ((10 10, 100 100, 200 10.1, 300 10, 10 10))");
		Geometry g2 = reader.read("POLYGON ((10 10, 100 100, 200 10, 10 10))");
		Geometry gReduce = reducer.reduce(g);
    assertEqualsExactAndHasSameFactory(gReduce, g2);
	}
  
  public void testPolgonWithCollapsedLinePointwise() throws Exception {
		Geometry g  = reader.read("POLYGON ((10 10, 100 100, 200 10.1, 300 10, 10 10))");
		Geometry g2 = reader.read("POLYGON ((10 10, 100 100, 200 10,   300 10, 10 10))");
		Geometry gReduce = GeometryPrecisionReducer.reducePointwise(g, pmFixed1);
    assertEqualsExactAndHasSameFactory(gReduce, g2);
	}
  
  public void testPolgonWithCollapsedPoint() throws Exception {
		Geometry g = reader.read("POLYGON ((10 10, 100 100, 200 10.1, 300 100, 400 10, 10 10))");
		Geometry g2 = reader.read("MULTIPOLYGON (((10 10, 100 100, 200 10, 10 10)), ((200 10, 300 100, 400 10, 200 10)))");
		Geometry gReduce = reducer.reduce(g);
    assertEqualsExactAndHasSameFactory(gReduce, g2);
	}

  public void testPolgonWithCollapsedPointPointwise() throws Exception {
		Geometry g  = reader.read("POLYGON ((10 10, 100 100, 200 10.1, 300 100, 400 10, 10 10))");
		Geometry g2 = reader.read("POLYGON ((10 10, 100 100, 200 10,   300 100, 400 10, 10 10))");
		Geometry gReduce = GeometryPrecisionReducer.reducePointwise(g, pmFixed1);
    assertEqualsExactAndHasSameFactory(gReduce, g2);
	}

  private void assertEqualsExactAndHasSameFactory(Geometry a, Geometry b)
  {
    assertTrue(a.equalsExact(b));
    assertTrue(a.getFactory() == b.getFactory());
  }


}
