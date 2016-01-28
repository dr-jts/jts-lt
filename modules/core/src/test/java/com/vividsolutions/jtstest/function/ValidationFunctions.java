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
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.operation.valid.IsValidOp;
import com.vividsolutions.jts.operation.valid.TopologyValidationError;

public class ValidationFunctions
{
  /**
   * Validates all geometries in a collection independently.
   * Errors are returned as points at the invalid location
   * 
   * @param g
   * @return the invalid locations, if any
   */
  public static Geometry invalidLocations(Geometry g)
  {
    List invalidLoc = new ArrayList();
    for (int i = 0; i < g.getNumGeometries(); i++) {
      Geometry geom = g.getGeometryN(i);
      IsValidOp ivop = new IsValidOp(geom);
      TopologyValidationError err = ivop.getValidationError();
      if (err != null) {
        invalidLoc.add(g.getFactory().createPoint(err.getCoordinate()));
      }
    }
    return g.getFactory().buildGeometry(invalidLoc);
  }
  
  public static Geometry invalidGeoms(Geometry g)
  {
    List invalidGeoms = new ArrayList();
    for (int i = 0; i < g.getNumGeometries(); i++) {
      Geometry geom = g.getGeometryN(i);
      IsValidOp ivop = new IsValidOp(geom);
      TopologyValidationError err = ivop.getValidationError();
      if (err != null) {
        invalidGeoms.add(geom);
      }
    }
    return g.getFactory().buildGeometry(invalidGeoms);
  }
  
  public static boolean isValidAllowSelfTouchingRingFormingHole(Geometry g) {
    IsValidOp validOp = new IsValidOp(g);
    validOp.setSelfTouchingRingFormingHoleValid(true);
    return validOp.isValid();     
  }

}
