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
package org.locationtech.jtstest.geomop;

import java.util.Collection;

import org.locationtech.jts.densify.Densifier;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.util.LinearComponentExtracter;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.locationtech.jts.operation.overlayng.OverlayNG;
import org.locationtech.jts.operation.polygonize.Polygonizer;
import org.locationtech.jts.precision.MinimumClearance;

/**
 * Geometry functions which
 * augment the existing methods on {@link Geometry},
 * for use in XML Test files.
 * This is the default used in the TestRunner, 
 * and thus all the operations 
 * in this class should be named differently to the Geometry methods
 * (otherwise they will shadow the real Geometry methods).
 * <p>
 * If replacing a Geometry method is desired, this
 * can be done via the -geomfunc argument to the TestRunner.
 * 
 * @author Martin Davis
 *
 */
public class TestCaseGeometryFunctions 
{
	public static Geometry bufferMitredJoin(Geometry g, double distance)	
	{
    BufferParameters bufParams = new BufferParameters();
    bufParams.setJoinStyle(BufferParameters.JOIN_MITRE);
    
    return BufferOp.bufferOp(g, distance, bufParams);
	}

  public static Geometry densify(Geometry g, double distance) 
  {
    return Densifier.densify(g, distance);
  }

  public static double minClearance(Geometry g) 
  {
    return MinimumClearance.getDistance(g);
  }

  public static Geometry minClearanceLine(Geometry g) 
  {
    return MinimumClearance.getLine(g);
  }

  private static Geometry polygonize(Geometry g, boolean extractOnlyPolygonal) {
    Collection lines = LinearComponentExtracter.getLines(g);
    Polygonizer polygonizer = new Polygonizer(extractOnlyPolygonal);
    polygonizer.add(lines);
    return polygonizer.getGeometry();
  }
  
  public static Geometry polygonize(Geometry g) {
    return polygonize(g, false);
  }
  
  public static Geometry polygonizeValidPolygonal(Geometry g) {
    return polygonize(g, true);
  }
  
  public static Geometry intersectionNG(Geometry geom0, Geometry geom1) {
    return OverlayNG.overlay(geom0, geom1, OverlayNG.INTERSECTION);
  }
  public static Geometry unionNG(Geometry geom0, Geometry geom1) {
    return OverlayNG.overlay(geom0, geom1, OverlayNG.UNION);
  }
  public static Geometry differenceNG(Geometry geom0, Geometry geom1) {
    return OverlayNG.overlay(geom0, geom1, OverlayNG.DIFFERENCE);
  }
  public static Geometry symDifferenceNG(Geometry geom0, Geometry geom1) {
    return OverlayNG.overlay(geom0, geom1, OverlayNG.SYMDIFFERENCE);
  }
  
  public static Geometry intersectionSR(Geometry geom0, Geometry geom1, double scale) {
    PrecisionModel pm = new PrecisionModel(scale);
    return OverlayNG.overlay(geom0, geom1, OverlayNG.INTERSECTION, pm);
  }
  public static Geometry unionSr(Geometry geom0, Geometry geom1, double scale) {
    PrecisionModel pm = new PrecisionModel(scale);
    return OverlayNG.overlay(geom0, geom1, OverlayNG.UNION, pm);
  }
  public static Geometry differenceSR(Geometry geom0, Geometry geom1, double scale) {
    PrecisionModel pm = new PrecisionModel(scale);
    return OverlayNG.overlay(geom0, geom1, OverlayNG.DIFFERENCE, pm);
  }
  public static Geometry symDifferenceSR(Geometry geom0, Geometry geom1, double scale) {
    PrecisionModel pm = new PrecisionModel(scale);
    return OverlayNG.overlay(geom0, geom1, OverlayNG.SYMDIFFERENCE, pm);
  }

}
;