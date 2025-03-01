/*
 * Copyright (c) 2016 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.locationtech.jts.triangulate.quadedge;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Triangle;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.math.DD;

/**
 * Algorithms for computing values and predicates
 * associated with triangles.
 * For some algorithms extended-precision
 * implementations are provided, which are more robust
 * (i.e. they produce correct answers in more cases).
 * Also, some more robust formulations of
 * some algorithms are provided, which utilize
 * normalization to the origin.
 * 
 * @author Martin Davis
 *
 */
public class TrianglePredicate 
{
  /**
   * Tests if a point is inside the circle defined by 
   * the triangle with vertices a, b, c (oriented counter-clockwise). 
   * This test uses simple
   * double-precision arithmetic, and thus may not be robust.
   * 
   * @param a a vertex of the triangle
   * @param b a vertex of the triangle
   * @param c a vertex of the triangle
   * @param p the point to test
   * @return true if this point is inside the circle defined by the points a, b, c
   */
  public static boolean isInCircleNonRobust(
      Coordinate a, Coordinate b, Coordinate c, 
      Coordinate p) {
    boolean isInCircle = 
              (a.x * a.x + a.y * a.y) * triArea(b, c, p)
            - (b.x * b.x + b.y * b.y) * triArea(a, c, p)
            + (c.x * c.x + c.y * c.y) * triArea(a, b, p)
            - (p.x * p.x + p.y * p.y) * triArea(a, b, c) 
            > 0;
    return isInCircle;
  }
  
  /**
   * Tests if a point is inside the circle defined by 
   * the triangle with vertices a, b, c (oriented counter-clockwise). 
   * This test uses simple
   * double-precision arithmetic, and thus is not 100% robust.
   * However, by using normalization to the origin
   * it provides improved robustness and increased performance.
   * <p>
   * Based on code by J.R.Shewchuk.
   * 
   * 
   * @param a a vertex of the triangle
   * @param b a vertex of the triangle
   * @param c a vertex of the triangle
   * @param p the point to test
   * @return true if this point is inside the circle defined by the points a, b, c
   */
  public static boolean isInCircleNormalized(
      Coordinate a, Coordinate b, Coordinate c, 
      Coordinate p) {
    double adx = a.x - p.x;
    double ady = a.y - p.y;
    double bdx = b.x - p.x;
    double bdy = b.y - p.y;
    double cdx = c.x - p.x;
    double cdy = c.y - p.y;

    double abdet = adx * bdy - bdx * ady;
    double bcdet = bdx * cdy - cdx * bdy;
    double cadet = cdx * ady - adx * cdy;
    double alift = adx * adx + ady * ady;
    double blift = bdx * bdx + bdy * bdy;
    double clift = cdx * cdx + cdy * cdy;

    double disc = alift * bcdet + blift * cadet + clift * abdet;
    return disc > 0;
  }
  
  /**
   * Computes twice the area of the oriented triangle (a, b, c), i.e., the area is positive if the
   * triangle is oriented counterclockwise.
   * 
   * @param a a vertex of the triangle
   * @param b a vertex of the triangle
   * @param c a vertex of the triangle
   */
  private static double triArea(Coordinate a, Coordinate b, Coordinate c) {
      return (b.x - a.x) * (c.y - a.y) 
           - (b.y - a.y) * (c.x - a.x);
  }

  /**
   * Tests if a point is inside the circle defined by 
   * the triangle with vertices a, b, c (oriented counter-clockwise). 
   * This method uses a fast errorbound filter and falls back to a
   * precise computation if it cannot guarantee correctness.
   * 
   * @param a a vertex of the triangle
   * @param b a vertex of the triangle
   * @param c a vertex of the triangle
   * @param p the point to test
   * @return true if this point is inside the circle defined by the points a, b, c
   */
  public static boolean isInCircleRobust(
      Coordinate a, Coordinate b, Coordinate c, 
      Coordinate p) 
  {
    double adx, bdx, cdx, ady, bdy, cdy;
    double bdxcdy, cdxbdy, cdxady, adxcdy, adxbdy, bdxady;
    double alift, blift, clift;
    double det;
    double errbound, permanent;

    adx = a.x - p.x;
    bdx = b.x - p.x;
    cdx = c.x - p.x;
    ady = a.y - p.y;
    bdy = b.y - p.y;
    cdy = c.y - p.y;

    bdxcdy = bdx * cdy;
    cdxbdy = cdx * bdy;
    alift = adx * adx + ady * ady;

    cdxady = cdx * ady;
    adxcdy = adx * cdy;
    blift = bdx * bdx + bdy * bdy;

    adxbdy = adx * bdy;
    bdxady = bdx * ady;
    clift = cdx * cdx + cdy * cdy;

    det = alift * (bdxcdy - cdxbdy)
        + blift * (cdxady - adxcdy)
        + clift * (adxbdy - bdxady);

    permanent = (Math.abs(bdxcdy) + Math.abs(cdxbdy)) * alift
              + (Math.abs(cdxady) + Math.abs(adxcdy)) * blift
              + (Math.abs(adxbdy) + Math.abs(bdxady)) * clift;
    errbound = iccerrboundA * permanent;
    if (Math.abs(det) >= errbound) {
      return det > 0;
    }
    double[] pa = new double[] {a.x, a.y};
    double[] pb = new double[] {b.x, b.y};
    double[] pc = new double[] {c.x, c.y};
    double[] pd = new double[] {p.x, p.y};
    return incircleadapt(pa, pb, pc, pd, permanent) > 0;
  }

  /**
   * Tests if a point is inside the circle defined by 
   * the triangle with vertices a, b, c (oriented counter-clockwise). 
   * The computation uses {@link DD} arithmetic for robustness.
   * 
   * @param a a vertex of the triangle
   * @param b a vertex of the triangle
   * @param c a vertex of the triangle
   * @param p the point to test
   * @return true if this point is inside the circle defined by the points a, b, c
   */
  public static boolean isInCircleDDSlow(
      Coordinate a, Coordinate b, Coordinate c,
      Coordinate p) {
    DD px = DD.valueOf(p.x);
    DD py = DD.valueOf(p.y);
    DD ax = DD.valueOf(a.x);
    DD ay = DD.valueOf(a.y);
    DD bx = DD.valueOf(b.x);
    DD by = DD.valueOf(b.y);
    DD cx = DD.valueOf(c.x);
    DD cy = DD.valueOf(c.y);

    DD aTerm = (ax.multiply(ax).add(ay.multiply(ay)))
        .multiply(triAreaDDSlow(bx, by, cx, cy, px, py));
    DD bTerm = (bx.multiply(bx).add(by.multiply(by)))
        .multiply(triAreaDDSlow(ax, ay, cx, cy, px, py));
    DD cTerm = (cx.multiply(cx).add(cy.multiply(cy)))
        .multiply(triAreaDDSlow(ax, ay, bx, by, px, py));
    DD pTerm = (px.multiply(px).add(py.multiply(py)))
        .multiply(triAreaDDSlow(ax, ay, bx, by, cx, cy));

    DD sum = aTerm.subtract(bTerm).add(cTerm).subtract(pTerm);
    boolean isInCircle = sum.doubleValue() > 0;

    return isInCircle;
  }

  /**
   * Computes twice the area of the oriented triangle (a, b, c), i.e., the area
   * is positive if the triangle is oriented counterclockwise.
   * The computation uses {@link DD} arithmetic for robustness.
   * 
   * @param ax the x ordinate of a vertex of the triangle
   * @param ay the y ordinate of a vertex of the triangle
   * @param bx the x ordinate of a vertex of the triangle
   * @param by the y ordinate of a vertex of the triangle
   * @param cx the x ordinate of a vertex of the triangle
   * @param cy the y ordinate of a vertex of the triangle
   */
  public static DD triAreaDDSlow(DD ax, DD ay,
      DD bx, DD by, DD cx, DD cy) {
    return (bx.subtract(ax).multiply(cy.subtract(ay)).subtract(by.subtract(ay)
        .multiply(cx.subtract(ax))));
  }

  public static boolean isInCircleDDFast(
      Coordinate a, Coordinate b, Coordinate c,
      Coordinate p) {
    DD aTerm = (DD.sqr(a.x).selfAdd(DD.sqr(a.y)))
        .selfMultiply(triAreaDDFast(b, c, p));
    DD bTerm = (DD.sqr(b.x).selfAdd(DD.sqr(b.y)))
        .selfMultiply(triAreaDDFast(a, c, p));
    DD cTerm = (DD.sqr(c.x).selfAdd(DD.sqr(c.y)))
        .selfMultiply(triAreaDDFast(a, b, p));
    DD pTerm = (DD.sqr(p.x).selfAdd(DD.sqr(p.y)))
        .selfMultiply(triAreaDDFast(a, b, c));

    DD sum = aTerm.selfSubtract(bTerm).selfAdd(cTerm).selfSubtract(pTerm);
    boolean isInCircle = sum.doubleValue() > 0;

    return isInCircle;
  }

  public static DD triAreaDDFast(
      Coordinate a, Coordinate b, Coordinate c) {
    
    DD t1 = DD.valueOf(b.x).selfSubtract(a.x)
          .selfMultiply(
              DD.valueOf(c.y).selfSubtract(a.y));
    
    DD t2 = DD.valueOf(b.y).selfSubtract(a.y)
          .selfMultiply(
              DD.valueOf(c.x).selfSubtract(a.x));
    
    return t1.selfSubtract(t2);
  }

  public static boolean isInCircleDDNormalized(
      Coordinate a, Coordinate b, Coordinate c,
      Coordinate p) {
    DD adx = DD.valueOf(a.x).selfSubtract(p.x);
    DD ady = DD.valueOf(a.y).selfSubtract(p.y);
    DD bdx = DD.valueOf(b.x).selfSubtract(p.x);
    DD bdy = DD.valueOf(b.y).selfSubtract(p.y);
    DD cdx = DD.valueOf(c.x).selfSubtract(p.x);
    DD cdy = DD.valueOf(c.y).selfSubtract(p.y);

    DD abdet = adx.multiply(bdy).selfSubtract(bdx.multiply(ady));
    DD bcdet = bdx.multiply(cdy).selfSubtract(cdx.multiply(bdy));
    DD cadet = cdx.multiply(ady).selfSubtract(adx.multiply(cdy));
    DD alift = adx.multiply(adx).selfAdd(ady.multiply(ady));
    DD blift = bdx.multiply(bdx).selfAdd(bdy.multiply(bdy));
    DD clift = cdx.multiply(cdx).selfAdd(cdy.multiply(cdy));

    DD sum = alift.selfMultiply(bcdet)
    .selfAdd(blift.selfMultiply(cadet))
    .selfAdd(clift.selfMultiply(abdet));
    
    boolean isInCircle = sum.doubleValue() > 0;

    return isInCircle;
  }

  /**
   * Computes the inCircle test using distance from the circumcentre. 
   * Uses standard double-precision arithmetic.
   * <p>
   * In general this doesn't
   * appear to be any more robust than the standard calculation. However, there
   * is at least one case where the test point is far enough from the
   * circumcircle that this test gives the correct answer. 
   * <pre>
   * LINESTRING
   * (1507029.9878 518325.7547, 1507022.1120341457 518332.8225183258,
   * 1507029.9833 518325.7458, 1507029.9896965567 518325.744909031)
   * </pre>
   * 
   * @param a a vertex of the triangle
   * @param b a vertex of the triangle
   * @param c a vertex of the triangle
   * @param p the point to test
   * @return true if this point is inside the circle defined by the points a, b, c
   */
  public static boolean isInCircleCC(Coordinate a, Coordinate b, Coordinate c,
      Coordinate p) {
    Coordinate cc = Triangle.circumcentre(a, b, c);
    double ccRadius = a.distance(cc);
    double pRadiusDiff = p.distance(cc) - ccRadius;
    return pRadiusDiff <= 0;
  }
  
  /**
   * Checks if the computed value for isInCircle is correct, using
   * double-double precision arithmetic.
   * 
   * @param a a vertex of the triangle
   * @param b a vertex of the triangle
   * @param c a vertex of the triangle
   * @param p the point to test
   */
  /*
  private static void checkRobustInCircle(Coordinate a, Coordinate b, Coordinate c,
      Coordinate p) 
  {
    boolean nonRobustInCircle = isInCircleNonRobust(a, b, c, p);
    boolean isInCircleDD = TrianglePredicate.isInCircleDDSlow(a, b, c, p);
    boolean isInCircleCC = TrianglePredicate.isInCircleCC(a, b, c, p);
  
    Coordinate circumCentre = Triangle.circumcentre(a, b, c);
    System.out.println("p radius diff a = "
        + Math.abs(p.distance(circumCentre) - a.distance(circumCentre))
        / a.distance(circumCentre));
  
    if (nonRobustInCircle != isInCircleDD || nonRobustInCircle != isInCircleCC) {
      System.out.println("inCircle robustness failure (double result = "
          + nonRobustInCircle 
          + ", DD result = " + isInCircleDD
          + ", CC result = " + isInCircleCC + ")");
      System.out.println(WKTWriter.toLineString(new CoordinateArraySequence(
          new Coordinate[] { a, b, c, p })));
      System.out.println("Circumcentre = " + WKTWriter.toPoint(circumCentre)
          + " radius = " + a.distance(circumCentre));
      System.out.println("p radius diff a = "
          + Math.abs(p.distance(circumCentre)/a.distance(circumCentre) - 1));
      System.out.println("p radius diff b = "
          + Math.abs(p.distance(circumCentre)/b.distance(circumCentre) - 1));
      System.out.println("p radius diff c = "
          + Math.abs(p.distance(circumCentre)/c.distance(circumCentre) - 1));
      System.out.println();
    }
  }
*/

  /**
   * Expansion arithmetic methods from Shewchuk for exact IsInCircle Predicate.
   *
   * This is ported directly from the public domain implementation at 
   * https://www.cs.cmu.edu/afs/cs/project/quake/public/code/predicates.c .
   *
   * Code is intentionally kept as close to the original as possible to allow
   * reasonable debugging by comparison to the original C code. No documentation
   * of these private methods, expect what is copied from the original.
   */
  private final static double epsilon = Math.ulp(0.5);
  private final static double resulterrbound = (3.0 + 8.0 * epsilon) * epsilon;
  private final static double iccerrboundA = (10.0 + 96.0 * epsilon) * epsilon;
  private final static double iccerrboundB = (4.0 + 48.0 * epsilon) * epsilon;
  private final static double iccerrboundC = (44.0 + 576.0 * epsilon) * epsilon * epsilon;

  private static double[] Fast_Two_Sum(double a, double b)
  {
    double x = a + b;
    double bvirt = x - a;
    double y = b - bvirt;
    return new double[] {x, y};
  }

  private static double[] Two_Sum(double a, double b)
  {
    double x = a + b;
    double bvirt = x - a;
    double avirt = x - bvirt;
    double bround = b - bvirt;
    double around = a - avirt;
    double y = around + bround;
    return new double[] {x, y};
  }

  private static double Two_Diff_Tail(double a, double b, double x)
  {
    double bvirt = a - x;
    double avirt = x + bvirt;
    double bround = bvirt - b;
    double around = a - avirt;
    return around + bround;
  }

  private static double[] Two_Diff(double a, double b)
  {
    double x = a - b;
    double y = Two_Diff_Tail(a, b, x);
    return new double[] {x, y};
  }

  private static double[] Two_Product(double a, double b) {
    double x = a * b;
    double y = Math.fma(a, b, -x);
    return new double[] {x, y};
  }

  private static double[] Two_One_Sum(double a1, double a0, double b)
  {
    double[] _1x0 = Two_Sum(a0, b);
    double[] x2x1 = Two_Sum(a1, _1x0[0]);
    return new double[] {x2x1[0], x2x1[1], _1x0[1]};
  }

  private static double[] Two_One_Diff(double a1, double a0, double b)
  {
    double[] _1x0 = Two_Diff(a0, b);
    double[] x2x1 = Two_Sum( a1, _1x0[0]);
    return new double[] {x2x1[0], x2x1[1], _1x0[1]};
  }

  private static double[] Two_Two_Sum(double a1, double a0, double b1, double b0)
  {
    double[] _j_0x0 = Two_One_Sum(a1, a0, b0);
    double[] x3x2x1 = Two_One_Sum(_j_0x0[0], _j_0x0[1], b1);
    return new double[] {x3x2x1[0], x3x2x1[1], x3x2x1[2], _j_0x0[2]};
  }

  private static double[] Two_Two_Diff(double a1, double a0, double b1, double b0)
  {
    double[] _j_0x0 = Two_One_Diff(a1, a0, b0);
    double[] x3x2x1 = Two_One_Diff(_j_0x0[0], _j_0x0[1], b1);
    return new double[] {x3x2x1[0], x3x2x1[1], x3x2x1[2], _j_0x0[2]};
  }

  /*****************************************************************************/
  /*                                                                           */
  /*  fast_expansion_sum_zeroelim()   Sum two expansions, eliminating zero     */
  /*                                  components from the output expansion.    */
  /*                                                                           */
  /*  Sets h = e + f.  See the long version of my paper for details.           */
  /*                                                                           */
  /*  If round-to-even is used (as with IEEE 754), maintains the strongly      */
  /*  nonoverlapping property.  (That is, if e is strongly nonoverlapping, h   */
  /*  will be also.)  Does NOT maintain the nonoverlapping or nonadjacent      */
  /*  properties.                                                              */
  /*                                                                           */
  /*****************************************************************************/

  private static double[] fast_expansion_sum_zeroelim(double[] e, double[] f)  /* h cannot be e or f. */
  {
    double Q;
    double Qnew;
    double hh;
    int eindex, findex, hindex;
    double enow, fnow;
    
    double[] h = new double[e.length + f.length];

    enow = e[0];
    fnow = f[0];
    eindex = findex = 0;
    if ((fnow > enow) == (fnow > -enow)) {
      Q = enow;
      if(++eindex < e.length)
      {
          enow = e[eindex];
      }
    } else {
      Q = fnow;
      if(++findex < f.length)
      {
          fnow = f[findex];
      }
    }
    hindex = 0;
    if ((eindex < e.length) && (findex < f.length)) {
      if ((fnow > enow) == (fnow > -enow)) {
        double[] Qnhh = Fast_Two_Sum(enow, Q);
        Qnew = Qnhh[0];
        hh = Qnhh[1];
        if(++eindex < e.length)
        {
          enow = e[eindex];
        }
      } else {
        double[] Qnhh = Fast_Two_Sum(fnow, Q);
        Qnew = Qnhh[0];
        hh = Qnhh[1];
        if(++findex < f.length)
        {
          fnow = f[findex];
        }
      }
      Q = Qnew;
      if (hh != 0.0) {
        h[hindex++] = hh;
      }
      while ((eindex < e.length) && (findex < f.length)) {
        if ((fnow > enow) == (fnow > -enow)) {
          double[] Qnhh = Two_Sum(Q, enow);
          Qnew = Qnhh[0];
          hh = Qnhh[1];
          if(++eindex < e.length)
          {
            enow = e[eindex];
          }
        } else {
          double[] Qnhh = Two_Sum(Q, fnow);
          Qnew = Qnhh[0];
          hh = Qnhh[1];
          if(++findex < f.length)
          {
            fnow = f[findex];
          }
        }
        Q = Qnew;
        if (hh != 0.0) {
          h[hindex++] = hh;
        }
      }
    }
    while (eindex < e.length) {
      double[] Qnhh = Two_Sum(Q, enow);
      Qnew = Qnhh[0];
      hh = Qnhh[1];
      if(++eindex < e.length)
      {
        enow = e[eindex];
      }
      Q = Qnew;
      if (hh != 0.0) {
        h[hindex++] = hh;
      }
    }
    while (findex < f.length) {
      double[] Qnhh = Two_Sum(Q, fnow);
      Qnew = Qnhh[0];
      hh = Qnhh[1];
      if(++findex < f.length)
      {
        fnow = f[findex];
      }
      Q = Qnew;
      if (hh != 0.0) {
        h[hindex++] = hh;
      }
    }
    if ((Q != 0.0) || (hindex == 0)) {
      h[hindex++] = Q;
    }
    
    double[] h_out = new double[hindex];
    System.arraycopy(h, 0, h_out, 0, hindex);
    return h_out;
  }

  /*****************************************************************************/
  /*                                                                           */
  /*  scale_expansion_zeroelim()   Multiply an expansion by a scalar,          */
  /*                               eliminating zero components from the        */
  /*                               output expansion.                           */
  /*                                                                           */
  /*  Sets h = be.  See either version of my paper for details.                */
  /*                                                                           */
  /*  Maintains the nonoverlapping property.  If round-to-even is used (as     */
  /*  with IEEE 754), maintains the strongly nonoverlapping and nonadjacent    */
  /*  properties as well.  (That is, if e has one of these properties, so      */
  /*  will h.)                                                                 */
  /*                                                                           */
  /*****************************************************************************/

  private static double[] scale_expansion_zeroelim(double[] e, double b)
  {
    double Q, sum;
    double hh;
    double product1;
    double product0;
    int eindex, hindex;
    double enow;
    
    double[] h = new double[e.length * 2];

    double[] Qhh = Two_Product(e[0], b);
    Q = Qhh[0];
    hh = Qhh[1];
    hindex = 0;
    if (hh != 0) {
      h[hindex++] = hh;
    }
    for (eindex = 1; eindex < e.length; eindex++) {
      enow = e[eindex];
      double[] p1p0 = Two_Product(enow, b);
      product1 = p1p0[0];
      product0 = p1p0[1];
      double[] sumhh = Two_Sum(Q, product0);
      sum = sumhh[0];
      hh = sumhh[1];
      if (hh != 0) {
        h[hindex++] = hh;
      }
      Qhh = Fast_Two_Sum(product1, sum);
      Q = Qhh[0];
      hh = Qhh[1];
      if (hh != 0) {
        h[hindex++] = hh;
      }
    }
    if ((Q != 0.0) || (hindex == 0)) {
      h[hindex++] = Q;
    }
    double[] h_out = new double[hindex];
    System.arraycopy(h, 0, h_out, 0, hindex);
    return h_out;
  }

  /*****************************************************************************/
  /*                                                                           */
  /*  estimate()   Produce a one-word estimate of an expansion's value.        */
  /*                                                                           */
  /*  See either version of my paper for details.                              */
  /*                                                                           */
  /*****************************************************************************/

  private static double estimate(double[] e)
  {
    double Q;
    int eindex;

    Q = e[0];
    for (eindex = 1; eindex < e.length; eindex++) {
      Q += e[eindex];
    }
    return Q;
  }

  /*****************************************************************************/
  /*                                                                           */
  /*  incircleadapt()   Adaptive exact 2D incircle test.  Robust.              */
  /*                                                                           */
  /*               Return a positive value if the point pd lies inside the     */
  /*               circle passing through pa, pb, and pc; a negative value if  */
  /*               it lies outside; and zero if the four points are cocircular.*/
  /*               The points pa, pb, and pc must be in counterclockwise       */
  /*               order, or the sign of the result will be reversed.          */
  /*                                                                           */
  /*  Only the first and last routine should be used; the middle two are for   */
  /*  timings.                                                                 */
  /*****************************************************************************/

  private static double incircleadapt(double[] pa, double[] pb, double[] pc, double[] pd, double permanent)
  {
    double adx, bdx, cdx, ady, bdy, cdy;
    double det;
    double errbound;

    adx = pa[0] - pd[0];
    bdx = pb[0] - pd[0];
    cdx = pc[0] - pd[0];
    ady = pa[1] - pd[1];
    bdy = pb[1] - pd[1];
    cdy = pc[1] - pd[1];

    double bdxcdy1, cdxbdy1, cdxady1, adxcdy1, adxbdy1, bdxady1;
    double bdxcdy0, cdxbdy0, cdxady0, adxcdy0, adxbdy0, bdxady0;
    double[] bc = new double[4];
    double[] ca = new double[4];
    double[] ab = new double[4];
    double bc3, ca3, ab3;
    double[] axbc, axxbc, aybc, ayybc, adet;
    double[] bxca, bxxca, byca, byyca, bdet;
    double[] cxab, cxxab, cyab, cyyab, cdet;
    double[] abdet;
    double[] fin1, finnow, finother, finswap;

    double adxtail, bdxtail, cdxtail, adytail, bdytail, cdytail;
    double adxadx1, adyady1, bdxbdx1, bdybdy1, cdxcdx1, cdycdy1;
    double adxadx0, adyady0, bdxbdx0, bdybdy0, cdxcdx0, cdycdy0;
    double[] aa = new double[4];
    double[] bb = new double[4];
    double[] cc = new double[4];
    double aa3, bb3, cc3;
    double ti1, tj1;
    double ti0, tj0;
    double[] u = new double[4];
    double[] v = new double[4];
    double u3, v3;
    double[] temp8, temp16a, temp16b, temp16c, temp32a, temp32b, temp48, temp64;
    double[] axtbb, axtcc, aytbb, aytcc;
    double[] bxtaa, bxtcc, bytaa, bytcc;
    double[] cxtaa, cxtbb, cytaa, cytbb;
    double[] axtbc = new double[8];
    double[] aytbc = new double[8];
    double[] bxtca = new double[8];
    double[] bytca = new double[8];
    double[] cxtab = new double[8];
    double[] cytab = new double[8];
    double[] axtbct, aytbct, bxtcat, bytcat, cxtabt, cytabt;
    double[] axtbctt, aytbctt, bxtcatt, bytcatt, cxtabtt, cytabtt;
    double[] abt, bct, cat;
    double[] abtt = new double[4];
    double[] bctt = new double[4];
    double[] catt = new double[4];
    double bctt3, catt3;
    double negate;

    double tmp[] = Two_Product(bdx, cdy);
    bdxcdy1 = tmp[0];
    bdxcdy0 = tmp[1];
    tmp = Two_Product(cdx, bdy);
    cdxbdy1 = tmp[0];
    cdxbdy0 = tmp[1];
    tmp = Two_Two_Diff(bdxcdy1, bdxcdy0, cdxbdy1, cdxbdy0);
    bc3 = tmp[0];
    bc[2] = tmp[1];
    bc[1] = tmp[2];
    bc[0] = tmp[3];
    bc[3] = bc3;
    axbc = scale_expansion_zeroelim(bc, adx);
    axxbc = scale_expansion_zeroelim(axbc, adx);
    aybc = scale_expansion_zeroelim(bc, ady);
    ayybc = scale_expansion_zeroelim(aybc, ady);
    adet = fast_expansion_sum_zeroelim(axxbc, ayybc);

    tmp = Two_Product(cdx, ady);
    cdxady1 = tmp[0];
    cdxady0 = tmp[1];
    tmp = Two_Product(adx, cdy);
    adxcdy1 = tmp[0];
    adxcdy0 = tmp[1];
    tmp = Two_Two_Diff(cdxady1, cdxady0, adxcdy1, adxcdy0);
    ca3 = tmp[0];
    ca[2] = tmp[1];
    ca[1] = tmp[2];
    ca[0] = tmp[3];
    ca[3] = ca3;
    bxca = scale_expansion_zeroelim(ca, bdx);
    bxxca = scale_expansion_zeroelim(bxca, bdx);
    byca = scale_expansion_zeroelim(ca, bdy);
    byyca = scale_expansion_zeroelim(byca, bdy);
    bdet = fast_expansion_sum_zeroelim(bxxca, byyca);

    tmp = Two_Product(adx, bdy);
    adxbdy1 = tmp[0];
    adxbdy0 = tmp[1];
    tmp = Two_Product(bdx, ady);
    bdxady1 = tmp[0];
    bdxady0 = tmp[1];
    tmp = Two_Two_Diff(adxbdy1, adxbdy0, bdxady1, bdxady0);
    ab3 = tmp[0];
    ab[2] = tmp[1];
    ab[1] = tmp[2];
    ab[0] = tmp[3];
    ab[3] = ab3;
    cxab = scale_expansion_zeroelim(ab, cdx);
    cxxab = scale_expansion_zeroelim(cxab, cdx);
    cyab = scale_expansion_zeroelim(ab, cdy);
    cyyab = scale_expansion_zeroelim(cyab, cdy);
    cdet = fast_expansion_sum_zeroelim(cxxab, cyyab);

    abdet = fast_expansion_sum_zeroelim(adet, bdet);
    fin1 = fast_expansion_sum_zeroelim(abdet, cdet);

    det = estimate(fin1);
    errbound = iccerrboundB * permanent;
    if (Math.abs(det) >= errbound) {
      return det;
    }

    adxtail = Two_Diff_Tail(pa[0], pd[0], adx);
    adytail = Two_Diff_Tail(pa[1], pd[1], ady);
    bdxtail = Two_Diff_Tail(pb[0], pd[0], bdx);
    bdytail = Two_Diff_Tail(pb[1], pd[1], bdy);
    cdxtail = Two_Diff_Tail(pc[0], pd[0], cdx);
    cdytail = Two_Diff_Tail(pc[1], pd[1], cdy);
    if ((adxtail == 0.0) && (bdxtail == 0.0) && (cdxtail == 0.0)
        && (adytail == 0.0) && (bdytail == 0.0) && (cdytail == 0.0)) {
      return det;
    }

    errbound = iccerrboundC * permanent + resulterrbound * Math.abs(det);
    det += ((adx * adx + ady * ady) * ((bdx * cdytail + cdy * bdxtail)
                                       - (bdy * cdxtail + cdx * bdytail))
            + 2.0 * (adx * adxtail + ady * adytail) * (bdx * cdy - bdy * cdx))
         + ((bdx * bdx + bdy * bdy) * ((cdx * adytail + ady * cdxtail)
                                       - (cdy * adxtail + adx * cdytail))
            + 2.0 * (bdx * bdxtail + bdy * bdytail) * (cdx * ady - cdy * adx))
         + ((cdx * cdx + cdy * cdy) * ((adx * bdytail + bdy * adxtail)
                                       - (ady * bdxtail + bdx * adytail))
            + 2.0 * (cdx * cdxtail + cdy * cdytail) * (adx * bdy - ady * bdx));
    if (Math.abs(det) >= errbound) {
      return det;
    }

    finnow = fin1;

    if ((bdxtail != 0.0) || (bdytail != 0.0)
        || (cdxtail != 0.0) || (cdytail != 0.0)) {
      tmp = Two_Product(adx, adx);
      adxadx1 = tmp[0];
      adxadx0 = tmp[1];
      tmp = Two_Product(ady, ady);
      adyady1 = tmp[0];
      adyady0 = tmp[1];
      tmp = Two_Two_Sum(adxadx1, adxadx0, adyady1, adyady0);
      aa3 = tmp[0];
      aa[2] = tmp[1];
      aa[1] = tmp[2];
      aa[0] = tmp[3];
      aa[3] = aa3;
    }
    if ((cdxtail != 0.0) || (cdytail != 0.0)
        || (adxtail != 0.0) || (adytail != 0.0)) {
      tmp = Two_Product(bdx, bdx);
      bdxbdx1 = tmp[0];
      bdxbdx0 = tmp[1];
      tmp = Two_Product(bdy, bdy);
      bdybdy1 = tmp[0];
      bdybdy0 = tmp[1];
      tmp = Two_Two_Sum(bdxbdx1, bdxbdx0, bdybdy1, bdybdy0);
      bb3 = tmp[0];
      bb[2] = tmp[1];
      bb[1] = tmp[2];
      bb[0] = tmp[3];
      bb[3] = bb3;
    }
    if ((adxtail != 0.0) || (adytail != 0.0)
        || (bdxtail != 0.0) || (bdytail != 0.0)) {
      tmp = Two_Product(cdx, cdx);
      cdxcdx1 = tmp[0];
      cdxcdx0 = tmp[1];
      tmp = Two_Product(cdy, cdy);
      cdycdy1 = tmp[0];
      cdycdy0 = tmp[1];
      tmp = Two_Two_Sum(cdxcdx1, cdxcdx0, cdycdy1, cdycdy0);
      cc3 = tmp[0];
      cc[2] = tmp[1];
      cc[1] = tmp[2];
      cc[0] = tmp[3];
      cc[3] = cc3;
    }

    if (adxtail != 0.0) {
      axtbc = scale_expansion_zeroelim(bc, adxtail);
      temp16a = scale_expansion_zeroelim(axtbc, 2.0 * adx);

      axtcc = scale_expansion_zeroelim(cc, adxtail);
      temp16b = scale_expansion_zeroelim(axtcc, bdy);

      axtbb = scale_expansion_zeroelim(bb, adxtail);
      temp16c = scale_expansion_zeroelim(axtbb, -cdy);

      temp32a = fast_expansion_sum_zeroelim(temp16a, temp16b);
      temp48 = fast_expansion_sum_zeroelim(temp16c, temp32a);
      finother = fast_expansion_sum_zeroelim(finnow, temp48);
      finswap = finnow; finnow = finother; finother = finswap;
    }
    if (adytail != 0.0) {
      aytbc = scale_expansion_zeroelim(bc, adytail);
      temp16a = scale_expansion_zeroelim(aytbc, 2.0 * ady);

      aytbb = scale_expansion_zeroelim(bb, adytail);
      temp16b = scale_expansion_zeroelim(aytbb, cdx);

      aytcc = scale_expansion_zeroelim(cc, adytail);
      temp16c = scale_expansion_zeroelim(aytcc, -bdx);

      temp32a = fast_expansion_sum_zeroelim(temp16a, temp16b);
      temp48 = fast_expansion_sum_zeroelim(temp16c, temp32a);
      finother = fast_expansion_sum_zeroelim(finnow, temp48);
      finswap = finnow; finnow = finother; finother = finswap;
    }
    if (bdxtail != 0.0) {
      bxtca = scale_expansion_zeroelim(ca, bdxtail);
      temp16a = scale_expansion_zeroelim(bxtca, 2.0 * bdx);

      bxtaa = scale_expansion_zeroelim(aa, bdxtail);
      temp16b = scale_expansion_zeroelim(bxtaa, cdy);

      bxtcc = scale_expansion_zeroelim(cc, bdxtail);
      temp16c = scale_expansion_zeroelim(bxtcc, -ady);

      temp32a = fast_expansion_sum_zeroelim(temp16a, temp16b);
      temp48 = fast_expansion_sum_zeroelim(temp16c, temp32a);
      finother = fast_expansion_sum_zeroelim(finnow, temp48);
      finswap = finnow; finnow = finother; finother = finswap;
    }
    if (bdytail != 0.0) {
      bytca = scale_expansion_zeroelim(ca, bdytail);
      temp16a = scale_expansion_zeroelim(bytca, 2.0 * bdy);

      bytcc = scale_expansion_zeroelim(cc, bdytail);
      temp16b = scale_expansion_zeroelim(bytcc, adx);

      bytaa = scale_expansion_zeroelim(aa, bdytail);
      temp16c = scale_expansion_zeroelim(bytaa, -cdx);

      temp32a = fast_expansion_sum_zeroelim(temp16a, temp16b);
      temp48 = fast_expansion_sum_zeroelim(temp16c, temp32a);
      finother = fast_expansion_sum_zeroelim(finnow, temp48);
      finswap = finnow; finnow = finother; finother = finswap;
    }
    if (cdxtail != 0.0) {
      cxtab = scale_expansion_zeroelim(ab, cdxtail);
      temp16a = scale_expansion_zeroelim(cxtab, 2.0 * cdx);

      cxtbb = scale_expansion_zeroelim(bb, cdxtail);
      temp16b = scale_expansion_zeroelim(cxtbb, ady);

      cxtaa = scale_expansion_zeroelim(aa, cdxtail);
      temp16c = scale_expansion_zeroelim(cxtaa, -bdy);

      temp32a = fast_expansion_sum_zeroelim(temp16a, temp16b);
      temp48 = fast_expansion_sum_zeroelim(temp16c, temp32a);
      finother = fast_expansion_sum_zeroelim(finnow, temp48);
      finswap = finnow; finnow = finother; finother = finswap;
    }
    if (cdytail != 0.0) {
      cytab = scale_expansion_zeroelim(ab, cdytail);
      temp16a = scale_expansion_zeroelim(cytab, 2.0 * cdy);

      cytaa = scale_expansion_zeroelim(aa, cdytail);
      temp16b = scale_expansion_zeroelim(cytaa, bdx);

      cytbb = scale_expansion_zeroelim(bb, cdytail);
      temp16c = scale_expansion_zeroelim(cytbb, -adx);

      temp32a = fast_expansion_sum_zeroelim(temp16a, temp16b);
      temp48 = fast_expansion_sum_zeroelim(temp16c, temp32a);
      finother = fast_expansion_sum_zeroelim(finnow, temp48);
      finswap = finnow; finnow = finother; finother = finswap;
    }

    if ((adxtail != 0.0) || (adytail != 0.0)) {
      if ((bdxtail != 0.0) || (bdytail != 0.0)
          || (cdxtail != 0.0) || (cdytail != 0.0)) {
        tmp = Two_Product(bdxtail, cdy);
        ti1 = tmp[0];
        ti0 = tmp[1];
        tmp = Two_Product(bdx, cdytail);
        tj1 = tmp[0];
        tj0 = tmp[1];
        tmp = Two_Two_Sum(ti1, ti0, tj1, tj0);
        u3 = tmp[0];
        u[2] = tmp[1];
        u[1] = tmp[2];
        u[0] = tmp[3];
        u[3] = u3;
        negate = -bdy;
        tmp = Two_Product(cdxtail, negate);
        ti1 = tmp[0];
        ti0 = tmp[1];
        negate = -bdytail;
        tmp = Two_Product(cdx, negate);
        tj1 = tmp[0];
        tj0 = tmp[1];
        tmp = Two_Two_Sum(ti1, ti0, tj1, tj0);
        v3 = tmp[0];
        v[2] = tmp[1];
        v[1] = tmp[2];
        v[0] = tmp[3];
        v[3] = v3;
        bct = fast_expansion_sum_zeroelim(u, v);

        tmp = Two_Product(bdxtail, cdytail);
        ti1 = tmp[0];
        ti0 = tmp[1];
        tmp = Two_Product(cdxtail, bdytail);
        tj1 = tmp[0];
        tj0 = tmp[1];
        tmp = Two_Two_Diff(ti1, ti0, tj1, tj0);
        bctt3 = tmp[0];
        bctt[2] = tmp[1];
        bctt[1] = tmp[2];
        bctt[0] = tmp[3];
        bctt[3] = bctt3;
      } else {
        bct = new double[] {0.0};
        bctt = new double[] {0.0};
      }

      if (adxtail != 0.0) {
        temp16a = scale_expansion_zeroelim(axtbc, adxtail);
        axtbct = scale_expansion_zeroelim(bct, adxtail);
        temp32a = scale_expansion_zeroelim(axtbct, 2.0 * adx);
        temp48 = fast_expansion_sum_zeroelim(temp16a, temp32a);
        finother = fast_expansion_sum_zeroelim(finnow, temp48);
        finswap = finnow; finnow = finother; finother = finswap;
        if (bdytail != 0.0) {
          temp8 = scale_expansion_zeroelim(cc, adxtail);
          temp16a = scale_expansion_zeroelim(temp8, bdytail);
          finother = fast_expansion_sum_zeroelim(finnow, temp16a);
          finswap = finnow; finnow = finother; finother = finswap;
        }
        if (cdytail != 0.0) {
          temp8 = scale_expansion_zeroelim(bb, -adxtail);
          temp16a = scale_expansion_zeroelim(temp8, cdytail);
          finother = fast_expansion_sum_zeroelim(finnow, temp16a);
          finswap = finnow; finnow = finother; finother = finswap;
        }

        temp32a = scale_expansion_zeroelim(axtbct, adxtail);
        axtbctt = scale_expansion_zeroelim(bctt, adxtail);
        temp16a = scale_expansion_zeroelim(axtbctt, 2.0 * adx);
        temp16b = scale_expansion_zeroelim(axtbctt, adxtail);
        temp32b = fast_expansion_sum_zeroelim(temp16a, temp16b);
        temp64 = fast_expansion_sum_zeroelim(temp32a, temp32b);
        finother = fast_expansion_sum_zeroelim(finnow, temp64);
        finswap = finnow; finnow = finother; finother = finswap;
      }
      if (adytail != 0.0) {
        temp16a = scale_expansion_zeroelim(aytbc, adytail);
        aytbct = scale_expansion_zeroelim(bct, adytail);
        temp32a = scale_expansion_zeroelim(aytbct, 2.0 * ady);
        temp48 = fast_expansion_sum_zeroelim(temp16a, temp32a);
        finother = fast_expansion_sum_zeroelim(finnow, temp48);
        finswap = finnow; finnow = finother; finother = finswap;


        temp32a = scale_expansion_zeroelim(aytbct, adytail);
        aytbctt = scale_expansion_zeroelim(bctt, adytail);
        temp16a = scale_expansion_zeroelim(aytbctt, 2.0 * ady);
        temp16b = scale_expansion_zeroelim(aytbctt, adytail);
        temp32b = fast_expansion_sum_zeroelim(temp16a, temp16b);
        temp64 = fast_expansion_sum_zeroelim(temp32a, temp32b);
        finother = fast_expansion_sum_zeroelim(finnow, temp64);
        finswap = finnow; finnow = finother; finother = finswap;
      }
    }
    if ((bdxtail != 0.0) || (bdytail != 0.0)) {
      if ((cdxtail != 0.0) || (cdytail != 0.0)
          || (adxtail != 0.0) || (adytail != 0.0)) {
        tmp = Two_Product(cdxtail, ady);
        ti1 = tmp[0];
        ti0 = tmp[1];
        tmp = Two_Product(cdx, adytail);
        tj1 = tmp[0];
        tj0 = tmp[1];
        tmp = Two_Two_Sum(ti1, ti0, tj1, tj0);
        u3 = tmp[0];
        u[2] = tmp[1];
        u[1] = tmp[2];
        u[0] = tmp[3];
        u[3] = u3;
        negate = -cdy;
        tmp = Two_Product(adxtail, negate);
        ti1 = tmp[0];
        ti0 = tmp[1];
        negate = -cdytail;
        tmp = Two_Product(adx, negate);
        tj1 = tmp[0];
        tj0 = tmp[1];
        tmp = Two_Two_Sum(ti1, ti0, tj1, tj0);
        v3 = tmp[0];
        v[2] = tmp[1];
        v[1] = tmp[2];
        v[0] = tmp[3];
        v[3] = v3;
        cat = fast_expansion_sum_zeroelim(u, v);

        tmp = Two_Product(cdxtail, adytail);
        ti1 = tmp[0];
        ti0 = tmp[1];
        tmp = Two_Product(adxtail, cdytail);
        tj1 = tmp[0];
        tj0 = tmp[1];
        tmp = Two_Two_Diff(ti1, ti0, tj1, tj0);
        catt3 = tmp[0];
        catt[2] = tmp[1];
        catt[1] = tmp[2];
        catt[0] = tmp[3];
        catt[3] = catt3;
      } else {
        cat = new double[] {0.};
        catt = new double[] {0.};
      }

      if (bdxtail != 0.0) {
        temp16a = scale_expansion_zeroelim(bxtca, bdxtail);
        bxtcat = scale_expansion_zeroelim(cat, bdxtail);
        temp32a = scale_expansion_zeroelim(bxtcat, 2.0 * bdx);
        temp48 = fast_expansion_sum_zeroelim(temp16a, temp32a);
        finother = fast_expansion_sum_zeroelim(finnow, temp48);
        finswap = finnow; finnow = finother; finother = finswap;
        if (cdytail != 0.0) {
          temp8 = scale_expansion_zeroelim(aa, bdxtail);
          temp16a = scale_expansion_zeroelim(temp8, cdytail);
          finother = fast_expansion_sum_zeroelim(finnow, temp16a);
          finswap = finnow; finnow = finother; finother = finswap;
        }
        if (adytail != 0.0) {
          temp8 = scale_expansion_zeroelim(cc, -bdxtail);
          temp16a = scale_expansion_zeroelim(temp8, adytail);
          finother = fast_expansion_sum_zeroelim(finnow, temp16a);
          finswap = finnow; finnow = finother; finother = finswap;
        }

        temp32a = scale_expansion_zeroelim(bxtcat, bdxtail);
        bxtcatt = scale_expansion_zeroelim(catt, bdxtail);
        temp16a = scale_expansion_zeroelim(bxtcatt, 2.0 * bdx);
        temp16b = scale_expansion_zeroelim(bxtcatt, bdxtail);
        temp32b = fast_expansion_sum_zeroelim(temp16a, temp16b);
        temp64 = fast_expansion_sum_zeroelim(temp32a, temp32b);
        finother = fast_expansion_sum_zeroelim(finnow, temp64);
        finswap = finnow; finnow = finother; finother = finswap;
      }
      if (bdytail != 0.0) {
        temp16a = scale_expansion_zeroelim(bytca, bdytail);
        bytcat = scale_expansion_zeroelim(cat, bdytail);
        temp32a = scale_expansion_zeroelim(bytcat, 2.0 * bdy);
        temp48 = fast_expansion_sum_zeroelim(temp16a, temp32a);
        finother = fast_expansion_sum_zeroelim(finnow, temp48);
        finswap = finnow; finnow = finother; finother = finswap;


        temp32a = scale_expansion_zeroelim(bytcat, bdytail);
        bytcatt = scale_expansion_zeroelim(catt, bdytail);
        temp16a = scale_expansion_zeroelim(bytcatt, 2.0 * bdy);
        temp16b = scale_expansion_zeroelim(bytcatt, bdytail);
        temp32b = fast_expansion_sum_zeroelim(temp16a, temp16b);
        temp64 = fast_expansion_sum_zeroelim(temp32a, temp32b);
        finother = fast_expansion_sum_zeroelim(finnow, temp64);
        finswap = finnow; finnow = finother; finother = finswap;
      }
    }
    if ((cdxtail != 0.0) || (cdytail != 0.0)) {
      if ((adxtail != 0.0) || (adytail != 0.0)
          || (bdxtail != 0.0) || (bdytail != 0.0)) {
        tmp = Two_Product(adxtail, bdy);
        ti1 = tmp[0];
        ti0 = tmp[1];
        tmp = Two_Product(adx, bdytail);
        tj1 = tmp[0];
        tj0 = tmp[1];
        tmp = Two_Two_Sum(ti1, ti0, tj1, tj0);
        u3 = tmp[0];
        u[2] = tmp[1];
        u[1] = tmp[2];
        u[0] = tmp[3];
        u[3] = u3;
        negate = -ady;
        tmp = Two_Product(bdxtail, negate);
        ti1 = tmp[0];
        ti0 = tmp[1];
        negate = -adytail;
        tmp = Two_Product(bdx, negate);
        tj1 = tmp[0];
        tj0 = tmp[1];
        tmp = Two_Two_Sum(ti1, ti0, tj1, tj0);
        v3 = tmp[0];
        v[2] = tmp[1];
        v[1] = tmp[2];
        v[0] = tmp[3];
        v[3] = v3;
        abt = fast_expansion_sum_zeroelim(u, v);

        tmp = Two_Product(adxtail, bdytail);
        ti1 = tmp[0];
        ti0 = tmp[1];
        tmp = Two_Product(bdxtail, adytail);
        tj1 = tmp[0];
        tj0 = tmp[1];
        tmp = Two_Two_Diff(ti1, ti0, tj1, tj0);
        abtt = new double[] {tmp[3], tmp[2], tmp[1], tmp[0]};
      } else {
        abt = new double[] {0.0};
        abtt = new double[] {0.0};
      }

      if (cdxtail != 0.0) {
        temp16a = scale_expansion_zeroelim(cxtab, cdxtail);
        cxtabt = scale_expansion_zeroelim(abt, cdxtail);
        temp32a = scale_expansion_zeroelim(cxtabt, 2.0 * cdx);
        temp48 = fast_expansion_sum_zeroelim(temp16a, temp32a);
        finother = fast_expansion_sum_zeroelim(finnow, temp48);
        finswap = finnow; finnow = finother; finother = finswap;
        if (adytail != 0.0) {
          temp8 = scale_expansion_zeroelim(bb, cdxtail);
          temp16a = scale_expansion_zeroelim(temp8, adytail);
          finother = fast_expansion_sum_zeroelim(finnow, temp16a);
          finswap = finnow; finnow = finother; finother = finswap;
        }
        if (bdytail != 0.0) {
          temp8 = scale_expansion_zeroelim(aa, -cdxtail);
          temp16a = scale_expansion_zeroelim(temp8, bdytail);
          finother = fast_expansion_sum_zeroelim(finnow, temp16a);
          finswap = finnow; finnow = finother; finother = finswap;
        }

        temp32a = scale_expansion_zeroelim(cxtabt, cdxtail);
        cxtabtt = scale_expansion_zeroelim(abtt, cdxtail);
        temp16a = scale_expansion_zeroelim(cxtabtt, 2.0 * cdx);
        temp16b = scale_expansion_zeroelim(cxtabtt, cdxtail);
        temp32b = fast_expansion_sum_zeroelim(temp16a, temp16b);
        temp64 = fast_expansion_sum_zeroelim(temp32a, temp32b);
        finother = fast_expansion_sum_zeroelim(finnow, temp64);
        finswap = finnow; finnow = finother; finother = finswap;
      }
      if (cdytail != 0.0) {
        temp16a = scale_expansion_zeroelim(cytab, cdytail);
        cytabt = scale_expansion_zeroelim(abt, cdytail);
        temp32a = scale_expansion_zeroelim(cytabt, 2.0 * cdy);
        temp48 = fast_expansion_sum_zeroelim(temp16a, temp32a);
        finother = fast_expansion_sum_zeroelim(finnow, temp48);
        finswap = finnow; finnow = finother; finother = finswap;


        temp32a = scale_expansion_zeroelim(cytabt, cdytail);
        cytabtt = scale_expansion_zeroelim(abtt, cdytail);
        temp16a = scale_expansion_zeroelim(cytabtt, 2.0 * cdy);
        temp16b = scale_expansion_zeroelim(cytabtt, cdytail);
        temp32b = fast_expansion_sum_zeroelim(temp16a, temp16b);
        temp64 = fast_expansion_sum_zeroelim(temp32a, temp32b);
        finother = fast_expansion_sum_zeroelim(finnow, temp64);
        finswap = finnow; finnow = finother; finother = finswap;
      }
    }

    return finnow[finnow.length - 1];
  }
}
