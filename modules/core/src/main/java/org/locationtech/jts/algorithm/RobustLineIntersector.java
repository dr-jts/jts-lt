/*
 * Copyright (c) 2016 Vivid Solutions, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.algorithm;

/**
 *@version 1.7
 */
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.CoordinateXYM;
import org.locationtech.jts.geom.CoordinateXYZM;
import org.locationtech.jts.geom.Coordinates;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.ZInterpolating;

/**
 * A robust version of {@link LineIntersector}.
 *
 * @version 1.7
 */
public class RobustLineIntersector
    extends LineIntersector
{
  public RobustLineIntersector() {
  }

  public void computeIntersection(Coordinate p, Coordinate p1, Coordinate p2) {
    isProper = false;
    // do between check first, since it is faster than the orientation test
    if (Envelope.intersects(p1, p2, p)) {
      if ((Orientation.index(p1, p2, p) == 0)
          && (Orientation.index(p2, p1, p) == 0)) {
        isProper = true;
        if (p.equals(p1) || p.equals(p2)) {
          isProper = false;
        }
        result = POINT_INTERSECTION;
        return;
      }
    }
    result = NO_INTERSECTION;
  }

  protected int computeIntersect(
                Coordinate p1, Coordinate p2,
                Coordinate q1, Coordinate q2  ) {
    isProper = false;

    // first try a fast test to see if the envelopes of the lines intersect
    if (! Envelope.intersects(p1, p2, q1, q2))
      return NO_INTERSECTION;

    // for each endpoint, compute which side of the other segment it lies
    // if both endpoints lie on the same side of the other segment,
    // the segments do not intersect
    int Pq1 = Orientation.index(p1, p2, q1);
    int Pq2 = Orientation.index(p1, p2, q2);

    if ((Pq1>0 && Pq2>0) || (Pq1<0 && Pq2<0)) {
      return NO_INTERSECTION;
    }

    int Qp1 = Orientation.index(q1, q2, p1);
    int Qp2 = Orientation.index(q1, q2, p2);

    if ((Qp1>0 && Qp2>0) || (Qp1<0 && Qp2<0)) {
        return NO_INTERSECTION;
    }
    /**
     * Intersection is collinear if each endpoint lies on the other line.
     */
    boolean collinear = Pq1 == 0
         && Pq2 == 0
         && Qp1 == 0
         && Qp2 == 0;
    if (collinear) {
      return computeCollinearIntersection(p1, p2, q1, q2);
    }
    
    /**
     * At this point we know that there is a single intersection point
     * (since the lines are not collinear).
     */
    
    /**
     *  Check if the intersection is an endpoint. If it is, copy the endpoint as
     *  the intersection point. Copying the point rather than computing it
     *  ensures the point has the exact value, which is important for
     *  robustness. It is sufficient to simply check for an endpoint which is on
     *  the other line, since at this point we know that the inputLines must
     *  intersect.
     */
    Coordinate p = null;
    double z = Double.NaN;
    if (Pq1 == 0 || Pq2 == 0 || Qp1 == 0 || Qp2 == 0) {
      isProper = false;
      
      /**
       * Check for two equal endpoints.  
       * This is done explicitly rather than by the orientation tests
       * below in order to improve robustness.
       * 
       * [An example where the orientation tests fail to be consistent is
       * the following (where the true intersection is at the shared endpoint
       * POINT (19.850257749638203 46.29709338043669)
       * 
       * LINESTRING ( 19.850257749638203 46.29709338043669, 20.31970698357233 46.76654261437082 ) 
       * and 
       * LINESTRING ( -48.51001596420236 -22.063180333403878, 19.850257749638203 46.29709338043669 )
       * 
       * which used to produce the INCORRECT result: (20.31970698357233, 46.76654261437082, NaN)
       * 
       */
      if (p1.equals2D(q1)) {
        p = p1;
        z = zGet(p1, q1);
      }
      else if (p1.equals2D(q2)) {
        p = p1;
        z = zGet(p1, q2);
      }
      else if (p2.equals2D(q1)) {
        p = p2;
        z = zGet(p2, q1);        
      }
      else if (p2.equals2D(q2)) {
        p = p2;
        z = zGet(p2, q2); 
      }
      /**
       * Now check to see if any endpoint lies on the interior of the other segment.
       */
      else if (Pq1 == 0) {
        p = q1;
        z = zGetOrInterpolate(q1, p1, p2);
      }
      else if (Pq2 == 0) {
        p = q2;
        z = zGetOrInterpolate(q2, p1, p2);
      }
      else if (Qp1 == 0) {
        p = p1;
        z = zGetOrInterpolate(p1, q1, q2);
      }
      else if (Qp2 == 0) {
        p = p2;
        z = zGetOrInterpolate(p2, q1, q2);
      }
    }
    else {
      isProper = true;
      p = intersection(p1, p2, q1, q2);
      z = zInterpolate(p, p1, p2, q1, q2);
    }
    intPt[0] = copyWithZ(p, z);
    return POINT_INTERSECTION;
  }

  private int computeCollinearIntersection(Coordinate p1, Coordinate p2,
      Coordinate q1, Coordinate q2) {
    boolean q1inP = Envelope.intersects(p1, p2, q1);
    boolean q2inP = Envelope.intersects(p1, p2, q2);
    boolean p1inQ = Envelope.intersects(q1, q2, p1);
    boolean p2inQ = Envelope.intersects(q1, q2, p2);

    if (q1inP && q2inP) {
      intPt[0] = copyWithZInterpolate(q1, p1, p2);
      intPt[1] = copyWithZInterpolate(q2, p1, p2);
      return COLLINEAR_INTERSECTION;
    }
    if (p1inQ && p2inQ) {
      intPt[0] = copyWithZInterpolate(p1, q1, q2);
      intPt[1] = copyWithZInterpolate(p2, q1, q2);
      return COLLINEAR_INTERSECTION;
    }
    if (q1inP && p1inQ) {
      // if pts are equal Z is chosen arbitrarily
      intPt[0] = copyWithZInterpolate(q1, p1, p2);
      intPt[1] = copyWithZInterpolate(p1, q1, q2);
      return q1.equals(p1) && !q2inP && !p2inQ ? POINT_INTERSECTION : COLLINEAR_INTERSECTION;
    }
    if (q1inP && p2inQ) {
      // if pts are equal Z is chosen arbitrarily
      intPt[0] = copyWithZInterpolate(q1, p1, p2);
      intPt[1] = copyWithZInterpolate(p2, q1, q2);
      return q1.equals(p2) && !q2inP && !p1inQ ? POINT_INTERSECTION : COLLINEAR_INTERSECTION;
    }
    if (q2inP && p1inQ) {
      // if pts are equal Z is chosen arbitrarily
      intPt[0] = copyWithZInterpolate(q2, p1, p2);
      intPt[1] = copyWithZInterpolate(p1, q1, q2);
      return q2.equals(p1) && !q1inP && !p2inQ ? POINT_INTERSECTION : COLLINEAR_INTERSECTION;
    }
    if (q2inP && p2inQ) {
      // if pts are equal Z is chosen arbitrarily
      intPt[0] = copyWithZInterpolate(q2, p1, p2);
      intPt[1] = copyWithZInterpolate(p2, q1, q2);
      return q2.equals(p2) && !q1inP && !p1inQ ? POINT_INTERSECTION : COLLINEAR_INTERSECTION;
    }
    return NO_INTERSECTION;
  }

  private static Coordinate copyWithZInterpolate(Coordinate p, Coordinate p1, Coordinate p2) {
    return copyWithZ(p, zGetOrInterpolate(p, p1, p2));
  }

  private static Coordinate copyWithZ(Coordinate p, double z) {
    Coordinate pCopy = copy(p);
    if (! Double.isNaN(z) && Coordinates.hasZ(pCopy)) {
      pCopy.setZ(z);
    }
    return pCopy;
  }
  
  private static Coordinate copy(Coordinate p) {
    return p.copy();
  }
  
  /**
   * This method computes the actual value of the intersection point.
   * It is rounded to the precision model if being used.
   */
  private Coordinate intersection(
    Coordinate p1, Coordinate p2, Coordinate q1, Coordinate q2)
  {
    Coordinate intPt = intersectionSafe(p1, p2, q1, q2);

    if (! isInSegmentEnvelopes(intPt)) {
//      System.out.println("Intersection outside segment envelopes: " + intPt);
      
      // compute a safer result
      // copy the coordinate, since it may be rounded later
      intPt = copy(nearestEndpoint(p1, p2, q1, q2));
      
//      System.out.println("Segments: " + this);
//      System.out.println("Snapped to " + intPt);
//      checkDD(p1, p2, q1, q2, intPt);
    }
    if (precisionModel != null) {
      precisionModel.makePrecise(intPt);
    }
    return intPt;
  }

  /*
  private void checkDD(Coordinate p1, Coordinate p2, Coordinate q1,
      Coordinate q2, Coordinate intPt)
  {
    Coordinate intPtDD = CGAlgorithmsDD.intersection(p1, p2, q1, q2);
    boolean isIn = isInSegmentEnvelopes(intPtDD);
    Debug.println(   "DD in env = " + isIn + "  --------------------- " + intPtDD);
    if (intPt.distance(intPtDD) > 0.0001) {
      Debug.println("Distance = " + intPt.distance(intPtDD));
    }
  }
  */
  
  /**
   * Computes a segment intersection.
   * Round-off error can cause the raw computation to fail, 
   * (usually due to the segments being approximately parallel).
   * If this happens, a reasonable approximation is computed instead.
   * 
   * @param p1 a segment endpoint
   * @param p2 a segment endpoint
   * @param q1 a segment endpoint
   * @param q2 a segment endpoint
   * @return the computed intersection point
   */
  private Coordinate intersectionSafe(Coordinate p1, Coordinate p2, Coordinate q1, Coordinate q2)
  {
    Coordinate intPt = Intersection.intersection(p1, p2, q1, q2);
    if (intPt == null)
      intPt = nearestEndpoint(p1, p2, q1, q2);
 //     System.out.println("Snapped to " + intPt);
    return intPt;
  }

  /**
   * Tests whether a point lies in the envelopes of both input segments.
   * A correctly computed intersection point should return <code>true</code>
   * for this test.
   * Since this test is for debugging purposes only, no attempt is
   * made to optimize the envelope test.
   *
   * @return <code>true</code> if the input point lies within both input segment envelopes
   */
  private boolean isInSegmentEnvelopes(Coordinate intPt)
  {
    Envelope env0 = new Envelope(inputLines[0][0], inputLines[0][1]);
    Envelope env1 = new Envelope(inputLines[1][0], inputLines[1][1]);
    return env0.contains(intPt) && env1.contains(intPt);
  }

  /**
   * Finds the endpoint of the segments P and Q which 
   * is closest to the other segment.
   * This is a reasonable surrogate for the true 
   * intersection points in ill-conditioned cases
   * (e.g. where two segments are nearly coincident,
   * or where the endpoint of one segment lies almost on the other segment).
   * <p>
   * This replaces the older CentralEndpoint heuristic,
   * which chose the wrong endpoint in some cases
   * where the segments had very distinct slopes 
   * and one endpoint lay almost on the other segment.
   * 
   * @param p1 an endpoint of segment P
   * @param p2 an endpoint of segment P
   * @param q1 an endpoint of segment Q
   * @param q2 an endpoint of segment Q
   * @return the nearest endpoint to the other segment
   */
  private static Coordinate nearestEndpoint(Coordinate p1, Coordinate p2,
      Coordinate q1, Coordinate q2)
  {
    Coordinate nearestPt = p1;
    double minDist = Distance.pointToSegment(p1, q1, q2);
    
    double dist = Distance.pointToSegment(p2, q1, q2);
    if (dist < minDist) {
      minDist = dist;
      nearestPt = p2;
    }
    dist = Distance.pointToSegment(q1, p1, p2);
    if (dist < minDist) {
      minDist = dist;
      nearestPt = q1;
    }
    dist = Distance.pointToSegment(q2, p1, p2);
    if (dist < minDist) {
      minDist = dist;
      nearestPt = q2;
    }
    return nearestPt;
  }

  /**
   * Gets the Z value of the first argument if present, 
   * otherwise the value of the second argument.
   * 
   * @param p a coordinate, possibly with Z
   * @param q a coordinate, possibly with Z
   * @return the Z value if present
   */
  private static double zGet(Coordinate p, Coordinate q) {
    double z = p.getZ();
    if (Double.isNaN(z)) {
      z = q.getZ(); // may be NaN
    }
    return z;
  }
  
  /**
   * Gets the Z value of a coordinate if present, or
   * interpolates it from the segment it lies on.
   * If the segment Z values are not fully populate
   * NaN is returned.
   * 
   * @param p a coordinate, possibly with Z 
   * @param p1 a segment endpoint, possibly with Z
   * @param p2 a segment endpoint, possibly with Z
   * @return the extracted or interpolated Z value (may be NaN)
   */
  private static double zGetOrInterpolate(Coordinate p, Coordinate p1, Coordinate p2) {
    double z = p.getZ();
    if (! Double.isNaN(z)) 
      return z;
    return zInterpolate(p, p1, p2); // may be NaN
  }

  /**
   * Interpolates a Z value for a point along 
   * a line segment between two points.
   * The Z value of the interpolation point (if any) is ignored.
   * If either segment point is missing Z, 
   * returns NaN.
   * 
   * @param p a coordinate
   * @param p1 a segment endpoint, possibly with Z
   * @param p2 a segment endpoint, possibly with Z
   * @return the interpolated Z value (may be NaN)
   */
  private static double zInterpolate(Coordinate p, Coordinate p1, Coordinate p2) {
    if (!ZInterpolating.getZInterpolating())
      return Double.NaN;
    double p1z = p1.getZ();
    double p2z = p2.getZ();
    if (Double.isNaN(p1z)) {
      return p2z; // may be NaN
    }
    if (Double.isNaN(p2z)) {
      return p1z; // may be NaN
    }
    if (p.equals2D(p1)) {
      return p1z; // not NaN
    }
    if (p.equals2D(p2)) {
      return p2z; // not NaN
    }
    double dz = p2z - p1z;
    if (dz == 0.0) {
      return p1z;
    }
    // interpolate Z from distance of p along p1-p2
    double dx = (p2.x - p1.x);
    double dy = (p2.y - p1.y);
    // seg has non-zero length since p1 < p < p2 
    double seglen = (dx * dx + dy * dy); 
    double xoff = (p.x - p1.x);
    double yoff = (p.y - p1.y);
    double plen = (xoff * xoff + yoff * yoff);
    double frac = Math.sqrt(plen / seglen);
    double zoff = dz * frac;
    double zInterpolated = p1z + zoff;
    return zInterpolated;
  }

  /**
   * Interpolates a Z value for a point along 
   * two line segments and computes their average.
   * The Z value of the interpolation point (if any) is ignored.
   * If one segment point is missing Z that segment is ignored
   * if both segments are missing Z, returns NaN.
   * 
   * @param p a coordinate
   * @param p1 a segment endpoint, possibly with Z
   * @param p2 a segment endpoint, possibly with Z
   * @param q1 a segment endpoint, possibly with Z
   * @param q2 a segment endpoint, possibly with Z
   * @return the averaged interpolated Z value (may be NaN)
   */
  private static double zInterpolate(Coordinate p, Coordinate p1, Coordinate p2, Coordinate q1, Coordinate q2) {
    double zp = zInterpolate(p, p1, p2);
    double zq = zInterpolate(p, q1, q2);
    if (Double.isNaN(zp)) {
      return zq; // may be NaN
    }
    if (Double.isNaN(zq)) {
      return zp; // may be NaN
    }
    // both Zs have values, so average them
    return (zp + zq) / 2.0;
  }
  

}
