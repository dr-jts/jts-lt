/*
 * Copyright (c) 2019 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jtstest.function;

import static org.locationtech.jts.operation.overlayng.OverlayNG.DIFFERENCE;
import static org.locationtech.jts.operation.overlayng.OverlayNG.INTERSECTION;
import static org.locationtech.jts.operation.overlayng.OverlayNG.UNION;
import static org.locationtech.jts.operation.overlayng.OverlayNG.SYMDIFFERENCE;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.overlayng.OverlayNGSnapIfNeeded;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.locationtech.jts.operation.union.UnionFunction;

public class OverlayNGSnapIfNeededFunctions {
  
  private static Geometry overlay(Geometry a, Geometry b, int opcode) {
    return OverlayNGSnapIfNeeded.overlay(a, b, opcode );
  }
  
  public static Geometry difference(Geometry a, Geometry b) {
    return overlay(a, b, DIFFERENCE );
  }

  public static Geometry differenceBA(Geometry a, Geometry b) {
    return overlay(b, a, DIFFERENCE );
  }

  public static Geometry intersection(Geometry a, Geometry b) {
    areatest(a, b);
    //System.out.println(areaDelta(a, b));
    return overlay(a, b, INTERSECTION );
  }

  public static Geometry union(Geometry a, Geometry b) {
    areatest(a, b);
    //System.out.println(areaDelta(a, b));
    return overlay(a, b, UNION );
  }

  public static Geometry symDifference(Geometry a, Geometry b) {
    //System.out.println(areaDelta(a, b));
    return overlay(a, b, SYMDIFFERENCE );
  }
  
  public static Geometry unaryUnion(Geometry a) {
    UnionFunction unionSRFun = new UnionFunction() {

      public Geometry union(Geometry g0, Geometry g1) {
         return overlay(g0, g1, UNION );
      }
      
    };
    UnaryUnionOp op = new UnaryUnionOp(a);
    op.setUnionFunction(unionSRFun);
    return op.union();
  }
  
  public static double areatest(Geometry a, Geometry b) {
    double areaDelta = areaDelta(a, b);
    if (areaDelta > 1e-6) 
      throw new RuntimeException("areatest delta too large: "+ areaDelta);
    
    return areaDelta;
  }
  
  /**
   * Computes the maximum area delta value
   * resulting from identity equations over the overlay operations.
   * The delta value is normalized to the total area of the geometries.
   * If the overlay operations are computed correctly 
   * the area delta is expected to be very small (e.g. < 1e-6).
   *  
   * @param a a geometry
   * @param b a geometry
   * @return the computed maximum area delta
   */
  public static double areaDelta(Geometry a, Geometry b) {
    
    double areaA = a.getArea();
    double areaB = b.getArea();
    
    // if an input is non-polygonal delta is 0
    if (areaA == 0 || areaB == 0)
      return 0;
    
    double areaU   = overlay( a, b, UNION ).getArea();    
    double areaI   = overlay( a, b, INTERSECTION ).getArea();
    double areaDab = overlay( a, b, DIFFERENCE ).getArea();
    double areaDba = overlay( b, a, DIFFERENCE ).getArea();
    double areaSD  = overlay( a, b, SYMDIFFERENCE ).getArea();
    
    double maxDelta = 0;

    // & : intersection
    // - : difference
    // + : union
    // ^ : symdifference


    // A = ( A & B ) + ( A - B )
    double delta = Math.abs( areaA - areaI - areaDab );
    if (delta > maxDelta) {
        maxDelta = delta;
    }

    // B = ( A & B ) + ( B - A )
    delta = Math.abs( areaB - areaI - areaDba );
    if (delta > maxDelta) {
        maxDelta = delta;
    }

    //  ( A ^ B ) = ( A - B ) + ( B - A )
    delta = Math.abs( areaDab + areaDba - areaSD );
    if (delta > maxDelta) {
        maxDelta = delta;
    }

    //  ( A + B ) = ( A & B ) + ( A ^ B )
    delta = Math.abs( areaI + areaSD - areaU );
    if (delta > maxDelta) {
        maxDelta = delta;
    }

    //  ( A + B ) = ( A & B ) + ( A - B ) + ( A - B )
    delta = Math.abs( areaU - areaI - areaDab - areaDba );
    if (delta > maxDelta) {
        maxDelta = delta;
    }
    
    // normalize the area delta value
    return maxDelta / (areaA + areaB);
  }
}
