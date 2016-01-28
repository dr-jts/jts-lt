/*
 * The JTS Topology Suite is a collection of Java classes that
 * implement the fundamental operations required to validate a given
 * geo-spatial data set to a known topological specification.
 * 
 * Copyright (C) 2016 Martin Davis
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Martin Davis BSD
 * License v1.0 (found at the root of the repository).
 * 
 */
package org.locationtech.jts.geom.util;

import java.util.*;

import org.locationtech.jts.geom.*;

/**
 * Methods to map various collections 
 * of {@link Geometry}s  
 * via defined mapping functions.
 * 
 * @author Martin Davis
 *
 */
public class GeometryMapper 
{
  /**
   * Maps the members of a {@link Geometry}
   * (which may be atomic or composite)
   * into another <tt>Geometry</tt> of most specific type.
   * <tt>null</tt> results are skipped.
   * In the case of hierarchical {@link GeometryCollection}s,
   * only the first level of members are mapped.
   *  
   * @param geom the input atomic or composite geometry
   * @param op the mapping operation
   * @return a result collection or geometry of most specific type
   */
  public static Geometry map(Geometry geom, MapOp op)
  {
    List mapped = new ArrayList();
    for (int i = 0; i < geom.getNumGeometries(); i++) {
      Geometry g = op.map(geom.getGeometryN(i));
      if (g != null)
        mapped.add(g);
    }
    return geom.getFactory().buildGeometry(mapped);
  }
  
  public static Collection map(Collection geoms, MapOp op)
  {
    List mapped = new ArrayList();
    for (Iterator i = geoms.iterator(); i.hasNext(); ) {
      Geometry g = (Geometry) i.next();
      Geometry gr = op.map(g);
      if (gr != null)
        mapped.add(gr);
    }
    return mapped;
  }
  
  /**
   * An interface for geometry functions used for mapping.
   * 
   * @author Martin Davis
   *
   */
  public interface MapOp 
  {
    /**
     * Computes a new geometry value.
     * 
     * @param g the input geometry
     * @return a result geometry
     */
    Geometry map(Geometry g);
  }
}
