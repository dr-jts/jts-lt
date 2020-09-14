/*
 * Copyright (c) 2020 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.operation.overlayng;

import static org.locationtech.jts.operation.overlayng.OverlayNG.UNION;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.noding.ValidatingNoder;
import org.locationtech.jts.noding.snap.SnappingNoder;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.locationtech.jts.operation.union.UnionStrategy;


/**
 * Performs an overlay operation, increasing robustness by using a series of
 * increasingly aggressive (and slower) noding strategies.
 * <p>
 * The noding strategies used are:
 * <ol>
 * <li>A simple, fast noder using FLOATING precision.
 * <li>A {@link SnappingNoder} using an automatically-determined snap tolerance
 * <li>First snapping each geometry to itself, 
 * and then overlaying them using a SnappingNoder.
 * <li>The above two strategies are repeated with increasing snap tolerance, up to a limit.
 * </ol>
 * If the above heuristics still fail to compute a valid overlay, 
 * the original {@link TopologyException} is thrown. 
 * <p>
 * This algorithm relies on each overlay operation execution 
 * throwing a {@link TopologyException} if it is unable
 * to compute the overlay correctly.
 * Generally this occurs because the noding phase does 
 * not produce a valid noding.
 * This requires the use of a {@link ValidatingNoder}
 * in order to check the results of using a floating noder.
 * 
 * @author Martin Davis
 */
public class OverlayNGSnapIfNeeded
{

  public static Geometry intersection(Geometry g0, Geometry g1)
  {
     return overlay(g0, g1, OverlayNG.INTERSECTION);
  }

  public static Geometry union(Geometry g0, Geometry g1)
  {
     return overlay(g0, g1, OverlayNG.UNION);
  }

  public static Geometry difference(Geometry g0, Geometry g1)
  {
     return overlay(g0, g1, OverlayNG.DIFFERENCE);
  }

  public static Geometry symDifference(Geometry g0, Geometry g1)
  {
     return overlay(g0, g1, OverlayNG.SYMDIFFERENCE);
  }
  
  public static Geometry union(Geometry a) {
    UnionStrategy unionSRFun = new UnionStrategy() {

      public Geometry union(Geometry g0, Geometry g1) {
         return overlay(g0, g1, UNION );
      }

      @Override
      public boolean isFloatingPrecision() {
        return true;
      }
      
    };
    UnaryUnionOp op = new UnaryUnionOp(a);
    op.setUnionFunction(unionSRFun);
    return op.union();
  }
  
  private static PrecisionModel PM_FLOAT = new PrecisionModel();

  public static Geometry overlay(Geometry geom0, Geometry geom1, int opCode)
  {
    Geometry result;
    RuntimeException exOriginal;
    
    /**
     * First try overlay with a FLOAT noder, which is fastest and causes least
     * change to geometry coordinates
     * By default the noder is validated, which is required in order
     * to detect certain invalid noding situations which otherwise
     * cause incorrect overlay output.
     */
    try {
      result = OverlayNG.overlay(geom0, geom1, opCode, PM_FLOAT ); 
      
      // Simple noding with no validation
      // There are cases where this succeeds with invalid noding (e.g. STMLF 1608).
      // So currently it is NOT safe to run overlay without noding validation
      //result = OverlayNG.overlay(geom0, geom1, opCode, createFloatingNoValidNoder()); 
      
      return result;
    }
    catch (RuntimeException ex) {
      /**
       * Capture original exception,
       * so it can be rethrown if the remaining strategies all fail.
       */
      exOriginal = ex;
    }
    
    /**
     * On failure retry using snapping noding with a "safe" tolerance.
     * if this throws an exception just let it go,
     * since it is something that is not a TopologyException
     */
    result = overlaySnapTries(geom0, geom1, opCode);
    if (result != null)
      return result;
    
    /**
     * On failure retry using snap-rounding with a heuristic scale factor (grid size).
     */
    result = overlaySR(geom0, geom1, opCode);
    if (result != null)
      return result;
    
    /**
     * Just can't get overlay to work, so throw original error.
     */
    throw exOriginal;
  }

  private static final int NUM_SNAP_TRIES = 5;

  /**
   * Attempt overlay using snapping with repeated tries with increasing snap tolerances.
   * 
   * @param geom0
   * @param geom1
   * @param opCode
   * @return the computed overlay result, or null if the overlay fails
   */
  private static Geometry overlaySnapTries(Geometry geom0, Geometry geom1, int opCode) {
    Geometry result;
    double snapTol = snapTolerance(geom0, geom1);
    
    for (int i = 0; i < NUM_SNAP_TRIES; i++) {
      
      result = overlaySnapping(geom0, geom1, opCode, snapTol);
      if (result != null) return result;
      
      /**
       * Now try snapping each input individually, 
       * and then doing the overlay.
       */
      result = overlaySnapBoth(geom0, geom1, opCode, snapTol);
      if (result != null) return result;
      
      // increase the snap tolerance and try again
      snapTol = snapTol * 10;
    }
    // failed to compute overlay
    return null;
  }

  /**
   * Attempt overlay using a {@link SnappingNoder}.
   * 
   * @param geom0
   * @param geom1
   * @param opCode
   * @param snapTol
   * @return the computed overlay result, or null if the overlay fails
   */
  private static Geometry overlaySnapping(Geometry geom0, Geometry geom1, int opCode, double snapTol) {
    try {
      return overlaySnapTol(geom0, geom1, opCode, snapTol);
    }
    catch (TopologyException ex) {
      //---- ignore exception, return null result to indicate failure
      
      //System.out.println("Snapping with " + snapTol + " - FAILED");
      //log("Snapping with " + snapTol + " - FAILED", geom0, geom1);
    }
    return null;
  }

  /**
   * Attempt overlay with first snapping each geometry individually.
   * 
   * @param geom0
   * @param geom1
   * @param opCode
   * @param snapTol
   * @return the computed overlay result, or null if the overlay fails
   */
  private static Geometry overlaySnapBoth(Geometry geom0, Geometry geom1, int opCode, double snapTol) {
    try {
      Geometry snap0 = overlaySnapTol(geom0, null, OverlayNG.UNION, snapTol);
      Geometry snap1 = overlaySnapTol(geom1, null, OverlayNG.UNION, snapTol); 
      //log("Snapping BOTH with " + snapTol, geom0, geom1);
      
      return overlaySnapTol(snap0, snap1, opCode, snapTol);
    }
    catch (TopologyException ex) {
      //---- ignore exception, return null result to indicate failure
    }
    return null;
  }

  private static Geometry overlaySnapTol(Geometry geom0, Geometry geom1, int opCode, double snapTol) {
    SnappingNoder snapNoder = new SnappingNoder(snapTol);
    return OverlayNG.overlay(geom0, geom1, opCode, snapNoder);
  }
  
  //============================================
  
  /**
   * A factor for a snapping tolerance distance which 
   * should allow noding to be computed robustly.
   */
  private static final double SNAP_TOL_FACTOR = 1e12;

  /**
   * Computes a heuristic snap tolerance distance
   * for overlaying a pair of geometries using a {@link SnappingNoder}.
   * 
   * @param geom0
   * @param geom1
   * @return
   */
  public static double snapTolerance(Geometry geom0, Geometry geom1) {
    double tol0 = snapTolerance(geom0);
    double tol1 = snapTolerance(geom1);
    double snapTol = Math.max(tol0,  tol1);
    return snapTol;
  }
  
  private static double snapTolerance(Geometry geom) {
    double magnitude = ordinateMagnitude(geom);
    return magnitude / SNAP_TOL_FACTOR;
  }
  
  /**
   * Computes the largest magnitude of the ordinates of a geometry,
   * based on the geometry envelope.
   * 
   * @param geom a geometry
   * @return the magnitude of the largest ordinate
   */
  private static double ordinateMagnitude(Geometry geom) {
    if (geom == null) return 0;
    Envelope env = geom.getEnvelopeInternal();
    double magMax = Math.max(
        Math.abs(env.getMaxX()), Math.abs(env.getMaxY()));
    double magMin = Math.max(
        Math.abs(env.getMinX()), Math.abs(env.getMinY()));
    return Math.max(magMax, magMin);
  }
  
  //===============================================
  
  private static void log(String msg, Geometry geom0, Geometry geom1) {
    System.out.println(msg);
    System.out.println(geom0);
    System.out.println(geom1);
  }
  
  /**
   * Attempt Overlay using Snap-Rounding with an automatically-determined
   * scale factor.
   * 
   * @param geom0
   * @param geom1
   * @param opCode
   * @return the computed overlay result, or null if the overlay fails
   */
  private static Geometry overlaySR(Geometry geom0, Geometry geom1, int opCode)
  {
    Geometry result;
    try {
      //System.out.println("OverlaySnapIfNeeded: trying snap-rounding");
      double scaleSafe = PrecisionUtil.safeScale(geom0, geom1);
      PrecisionModel pmSafe = new PrecisionModel(scaleSafe);
      result = OverlayNG.overlay(geom0, geom1, opCode, pmSafe);
      return result;
    }
    catch (TopologyException ex) {
      //---- ignore exception, return null result to indicate failure
    }
    return null;
  }

}
