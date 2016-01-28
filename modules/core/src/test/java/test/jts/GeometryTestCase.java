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

package test.jts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.util.Assert;


import junit.framework.TestCase;

/**
 * A base class for Geometry tests which provides various utility methods.
 * 
 * @author mbdavis
 *
 */
public class GeometryTestCase extends TestCase{

  GeometryFactory geomFactory = new GeometryFactory();
  
  WKTReader reader = new WKTReader(geomFactory);

  public GeometryTestCase(String name) {
    super(name);
  }

  protected void checkEqual(Geometry expected, Geometry actual) {
    Geometry actualNorm = actual.norm();
    Geometry expectedNorm = expected.norm();
    boolean equal = actualNorm.equalsExact(expectedNorm);
    if (! equal) {
      System.out.println("FAIL - Expected = " + expectedNorm
          + " actual = " + actualNorm );
    }
    assertTrue(equal);
  }

  protected void checkEqual(Collection expected, Collection actual) {
    checkEqual(toGeometryCollection(expected),toGeometryCollection(actual) );
  }

  GeometryCollection toGeometryCollection(Collection geoms) {
    return geomFactory.createGeometryCollection(GeometryFactory.toGeometryArray(geoms));
  }
  
  /**
   * Reads a {@link Geometry} from a WKT string using a custom {@link GeometryFactory}.
   *  
   * @param geomFactory the custom factory to use
   * @param wkt the WKT string
   * @return the geometry read
   */
  protected Geometry read(GeometryFactory geomFactory, String wkt) {
    WKTReader reader = new WKTReader(geomFactory);
    try {
       return reader.read(wkt);
    } catch (ParseException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  protected Geometry read(String wkt) {
    try {
       return reader.read(wkt);
    } catch (ParseException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  protected List readList(String[] wkt) {
    ArrayList geometries = new ArrayList();
    for (int i = 0; i < wkt.length; i++) {
      geometries.add(read(wkt[i]));
    }
    return geometries;
  }
}
