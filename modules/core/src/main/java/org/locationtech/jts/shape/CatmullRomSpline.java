/*
 * Copyright (c) 2022 Jes Wuilfsberg Nielsen.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.shape;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.util.GeometryMapper;

/**
 * Creates a curved geometry by replacing segments of the input with Catmull-Rom splines.
 * The Coordinates of the input geometry are used as control points for the spline, rolling a window of four coordinates
 * along the geometry to calculate the spline for each line segment (either looping around at the ends of a ring, or
 * extrapolating endpoints linearly from linestrings to get the first and last control point).
 * <p>
 * The implementation uses the "2D coordinates with additional values" approach which JTS generally uses, in the sense
 * that it treats the geometries as 2D, but allows for them to carry Z and/or M values. The Z and M values will be
 * interpolated if they exist, but they are not used to define the shape of the spline.
 * <p>
 * The implementation is based on the example given at <a href="https://stackoverflow.com/a/23980479">https://stackoverflow.com/a/23980479</a>
 * / <a href="https://ideone.com/NoEbVM">https://ideone.com/NoEbVM</a>
 * with a nod to <a href="https://github.com/mrdoob/three.js">https://github.com/mrdoob/three.js</a> for further inspiration.
 */
public class CatmullRomSpline {
  private final boolean includeZ;
  private final boolean includeM;

  /**
   * As {@link #catmullRomSpline(Geometry, int, double, double)} with alpha = 0.5 (centripetal) and
   * endpointExtrapolationWeight = 1.0.
   */
  public static Geometry catmullRomSpline(Geometry g, int numberOfSegments) {
    return catmullRomSpline(g, numberOfSegments, 0.5, 1);
  }

  /**
   * Creates a geometry of linearized Catmull-Rom splines defined by the coordinates of each line segment in the input
   * geometry, with the immediately preceding and following coordinates used as control points. In other words, it
   * uses the input geometry as the control curve for a Catmull-Rom spline.
   * <p>
   * In the case of LinearRings (including interior and exterior rings of polygons), the "preceding" and "following"
   * coordinates at the end of the ring are calculated by looping around to the end/start of the ring.
   * <p>
   * In the case of LineStrings, additional points are linearly extrapolated at the start and end of the geometry to be
   * used as control points.
   *
   * @param geom                        The geometry defining the control curve. (Multigeometries and polygons are
   *                                    valid input. Each constituent geometry will be calculated separately).
   * @param numberOfSegments            The number of spline segments on each segment of the input geometry
   * @param alpha                       Describes the "looseness" of the result, from 0.0 to 1.0.
   *                                    <ul>
   *                                    <li>0.0 (uniform) yields a "stiff" spline which will attempt to follow the input
   *                                    lines tightly,but may overshoot as they pass through control points, twisting
   *                                    sharply towards the next control point.</li>
   *                                    <li>1.0 (chordal) yields sweeping arcs which may diverge further from the input
   *                                    lines, but pass through the control points in smooth curves.</li>
   *                                    <li>0.5 (centripetal) provides a balance, and has the unique property of never
   *                                    creating self-intersections within a segment. (Though it does not guarantee that
   *                                    the different segments do not end up intersecting each other).</li>
   *                                    </ul>
   * @param endpointExtrapolationWeight When extra control points are linearly extrapolated at the start and end of
   *                                    linestrings, this parameter adjusts the length of that extrapolation, affecting
   *                                    the "lead-in" to the actual curve, and hence the shape of the first and last
   *                                    segment (though the impact is fairly minimal).
   * @return the linearized curved geometry
   */
  public static Geometry catmullRomSpline(Geometry geom, int numberOfSegments, double alpha, double endpointExtrapolationWeight) {
    // Pick the best Coordinate type amongst known JTS types:
    Coordinate c = geom.getCoordinate();
    boolean includeZ = !(c instanceof CoordinateXY || c instanceof CoordinateXYM);
    boolean includeM = c instanceof CoordinateXYM || c instanceof CoordinateXYZM;
    return new CatmullRomSpline(includeZ, includeM).catmullRomGeometry(geom, numberOfSegments, alpha, endpointExtrapolationWeight);
  }

  /**
   * As {@link #CatmullRomSpline(boolean, boolean)}, with includeZ = true, includeM = false.
   */
  public CatmullRomSpline() {
    this(true, false);
  }

  /**
   * Instantiate a version of the {@link CatmullRomSpline} to use specific functionality from it if the static
   * {@link #catmullRomSpline(Geometry, int, double, double)} does not suffice.
   *
   * @param includeZ Whether the generated coordinates include the Z ordinate. ({@link Coordinate} or {@link CoordinateXYZM})
   * @param includeM Whether the generated coordinates include the M ordinate. ({@link CoordinateXYM} or {@link CoordinateXYZM})
   */
  public CatmullRomSpline(boolean includeZ, boolean includeM) {
    this.includeZ = includeZ;
    this.includeM = includeM;
  }

  /**
   * Creates a geometry of linearized Catmull-Rom splines defined by the coordinates of each line segment in the input
   * geometry.
   * <p>
   * Parameters as per {@link #catmullRomSpline(Geometry, int, double, double)}
   */
  public Geometry catmullRomGeometry(Geometry g, int numberOfSegments, double alpha, double endpointExtrapolationWeight) {
    return GeometryMapper.flatMap(g, 1, geom -> {
      if (geom instanceof LinearRing) {
        return catmullRomLinearRing((LinearRing) geom, numberOfSegments, alpha);
      }
      if (geom instanceof LineString) {
        return catmullRomLineString((LineString) geom, numberOfSegments, alpha, endpointExtrapolationWeight);
      }
      if (geom instanceof Polygon) {
        return catmullRomPolygon((Polygon) geom, numberOfSegments, alpha);
      }
      return geom.copy();
    });
  }

  /**
   * Creates a LineString of linearized Catmull-Rom splines defined by the coordinates of each line segment in the input
   * geometry. This is a specialized form with stronger type signature, but parameters are otherwise as
   * {@link #catmullRomSpline(Geometry, int, double, double)}
   */
  public LineString catmullRomLineString(LineString l, int numberOfSegments, double alpha, double endpointExtrapolationWeight) {
    return l.getFactory().createLineString(this.calculateSplineCoordinates(l.getCoordinates(), numberOfSegments, alpha, false, endpointExtrapolationWeight));
  }

  /**
   * Creates a LinearRing of linearized Catmull-Rom splines defined by the coordinates of each line segment in the input
   * geometry. The created geometry will loop around, with the last point of the input connecting smoothly to the first.
   * <p>
   * The input does not need to be a valid LinearRing. If the last point of the input equals the first (per
   * {@link Coordinate#equals2D(Coordinate, double)}), the last point will be skipped to avoid duplicate coordinates.
   * <p>
   * Parameters as per {@link #catmullRomSpline(Geometry, int, double, double)}, except endpointExtrapolationWeight is
   * not needed for loops.
   */
  public LinearRing catmullRomLinearRing(LineString l, int numberOfSegments, double alpha) {
    return l.getFactory().createLinearRing(this.calculateSplineCoordinates(l.getCoordinates(), numberOfSegments, alpha, true, Double.NaN));
  }

  /**
   * Creates a Polygon of linearized Catmull-Rom splines defined by the coordinates of each line segment in the input
   * geometry.
   * <p>
   * This method will calculate a {@link #catmullRomLinearRing(LineString, int, double)} for the exterior and
   * interior rings of the polygon. Note that there is no guarantee that the generated polygon will be valid, as e.g.
   * the spline interpolation of holes could make them overlap or cross outside the exterior hull.
   */
  public Polygon catmullRomPolygon(Polygon poly, int numberOfSegments, double alpha) {
    LinearRing shell = catmullRomLinearRing(poly.getExteriorRing(), numberOfSegments, alpha);
    LinearRing[] holes = null;
    if (poly.getNumInteriorRing() > 0) {
      holes = new LinearRing[poly.getNumInteriorRing()];
      for (int i = 0; i < poly.getNumInteriorRing(); i++) {
        holes[i] = catmullRomLinearRing(poly.getInteriorRingN(i), numberOfSegments, alpha);
      }
    }
    return poly.getFactory().createPolygon(shell, holes);
  }

  /**
   * The "raw" Catmull-Rom spline interpolation of coordinates. The various geometry-generating methods use this
   * internally.
   *
   * @param coords                      An array of coordinates defining the control points of the spline.
   * @param numberOfSegments            The number of spline segments between each coordinate.
   * @param alpha                       Describes the "looseness" of the result, from 0.0 to 1.0.
   *                                    <ul>
   *                                    <li>0.0 (uniform) yields a "stiff" spline which will attempt to follow the input
   *                                    lines tightly,but may overshoot as they pass through control points, twisting
   *                                    sharply towards the next control point.</li>
   *                                    <li>1.0 (chordal) yields sweeping arcs which may diverge further from the input
   *                                    lines, but pass through the control points in smooth curves.</li>
   *                                    <li>0.5 (centripetal) provides a balance, and has the unique property of never
   *                                    creating self-intersections within a segment. (Though it does not guarantee that
   *                                    the different segments do not end up intersecting each other).</li>
   *                                    </ul>
   * @param loop                        Whether the spline forms a closed loop (ring) or a linestring.
   * @param endpointExtrapolationWeight If loop is false, a "phantom" control point will be linearly extrapolated at
   *                                    each end of the input line, to function as control points for the lead-in and
   *                                    -out to the spline. The endpointExtrapolationWeight determines the length of
   *                                    this extrapolation, as a factor of the distance between the two points used for
   *                                    the extrapolation. 1 is a sensible default, keeping the same distance as the
   *                                    existing points. A larger number will make the spline run at a flatter angle
   *                                    to the control line for a longer while before it turns away towards the next
   *                                    control point.
   * @return An array of interpolated coordinates.
   */
  public Coordinate[] calculateSplineCoordinates(Coordinate[] coords, int numberOfSegments, double alpha, boolean loop, double endpointExtrapolationWeight) {
    CoordinateList coordList = this.cleanInput(coords, loop, endpointExtrapolationWeight);
    if (coordList == null) {
      return coords;
    }

    // Initialize the polynomial structure for each ordinate:
    CubicPolynomial polyX, polyY, polyZ = null, polyM = null;
    polyX = new CubicPolynomial();
    polyY = new CubicPolynomial();
    if (this.includeZ) {
      polyZ = new CubicPolynomial();
    }
    if (this.includeM) {
      polyM = new CubicPolynomial();
    }

    // Prep the result array with the known length:
    Coordinate[] resultArray = new Coordinate[(coordList.size() - 3) * numberOfSegments + 1];
    int resultIndex = 0;

    // Spin over each segment in the input, possibly looping back to the start point:
    int len = coordList.size() - 2;
    for (int i = 1; i < len; i++) {
      Coordinate c0 = coordList.get(i - 1);
      Coordinate c1 = coordList.get(i);
      Coordinate c2 = coordList.get(i + 1);
      Coordinate c3 = coordList.get(i + 2);
      // A somewhat non-Java-idiomatic way, reusing allocated structures rather than creating new instances:
      this.initializePolynomialsForSegment(c0, c1, c2, c3, alpha, polyX, polyY, polyZ, polyM);

      // Spin over the wanted number of steps on each input segment:
      for (int j = 0; j < numberOfSegments; j++) {
        double t = j / (double) numberOfSegments;
        resultArray[resultIndex++] = newCoord(
          polyX.eval(t),
          polyY.eval(t),
          polyZ == null ? Double.NaN : polyZ.eval(t),
          polyM == null ? Double.NaN : polyM.eval(t)
        );
      }
    }
    // And drop the final control point in:
    Coordinate lastCoord = coordList.get(coordList.size() - 2);
    resultArray[resultIndex] = newCoord(lastCoord.x, lastCoord.y, lastCoord.getZ(), lastCoord.getM());
    return resultArray;
  }

  /**
   * Cleans the input of duplicate coordinates, as well as inserts the "phantom" control points at the start and end of
   * the coordinate list.
   *
   * @return A CoordinateList with the full list of control points to iterate over, or null if cleaning it resulted in
   * too few coordinates to be able to interpolate.
   */
  private CoordinateList cleanInput(Coordinate[] coords, boolean loop, double endpointExtrapolationWeight) {
    // Can't interpolate less than a line:
    if (coords == null || coords.length < 2) {
      return null;
    }

    CoordinateList coordList = new CoordinateList();

    // Placeholder for the first coord. We defer actually picking it until we have weeded out duplicates, since we may
    // need to extrapolate from input points.
    coordList.add(new Coordinate(Double.NaN, Double.NaN, Double.NaN));

    // The input coords, non-repeating, and possibly skipping the last one if it's equal to the first.
    // If we have a ring (touching endpoints), ignore the last, duplicated point.
    int numberOfPoints = loop && coords[0].equals2D(coords[coords.length - 1])
      ? coords.length - 1
      : coords.length;
    for (int i = 0; i < numberOfPoints; i++) {
      coordList.add(coords[i], false);
    }

    // If we do not have at least two unique coordinates (plus the placeholder), we cannot interpolate:
    if (coordList.size() < 3) {
      return null;
    }

    if (loop) {
      // The first control point in a loop is the last point in the (cleaned) input
      coordList.set(0, coordList.get(coordList.size() - 1));
      // And for the end, we loop back to the first (input) point, and pick the next as control point:
      coordList.add(coordList.getCoordinate(1));
      coordList.add(coordList.get(2));
    } else {
      // Extrapolate backwards along the line to find first control point
      coordList.set(0, this.extrapolate(coordList.get(2), coordList.get(1), endpointExtrapolationWeight));
      // ...and forward beyond the end:
      coordList.add(this.extrapolate(coordList.get(coordList.size() - 2), coordList.get(coordList.size() - 1), endpointExtrapolationWeight));
    }
    return coordList;
  }

  /**
   * Conceptually, the algorithm is based on distance, but since it's only ever used in an exponential function, we just
   * keep it as squared and divide the exponent by two later.
   * If we want true 3D or 4D interpolation, we simply need to include those coordinates in the distance function here.
   */
  double distSquared(Coordinate p, Coordinate q) {
    double dx = q.x - p.x;
    double dy = q.y - p.y;
    return dx * dx + dy * dy;
  }

  void initializePolynomialsForSegment(Coordinate p0,
                                       Coordinate p1,
                                       Coordinate p2,
                                       Coordinate p3,
                                       double alpha,
                                       CubicPolynomial polyX,
                                       CubicPolynomial polyY,
                                       CubicPolynomial polyZ,
                                       CubicPolynomial polyM) {
    double dt0 = Math.pow(distSquared(p0, p1), alpha * 0.5);
    double dt1 = Math.pow(distSquared(p1, p2), alpha * 0.5);
    double dt2 = Math.pow(distSquared(p2, p3), alpha * 0.5);

    polyX.initNonuniformCatmullRom(p0.x, p1.x, p2.x, p3.x, dt0, dt1, dt2);
    polyY.initNonuniformCatmullRom(p0.y, p1.y, p2.y, p3.y, dt0, dt1, dt2);
    if (polyZ != null) {
      polyZ.initNonuniformCatmullRom(p0.getZ(), p1.getZ(), p2.getZ(), p3.getZ(), dt0, dt1, dt2);
    }
    if (polyM != null) {
      polyM.initNonuniformCatmullRom(p0.getM(), p1.getM(), p2.getM(), p3.getM(), dt0, dt1, dt2);
    }
  }

  Coordinate extrapolate(Coordinate c1, Coordinate c2, double weight) {
    return newCoord(c2.x + (c2.x - c1.x) * weight, c2.y + (c2.y - c1.y) * weight, c2.getZ() + (c2.getZ() - c1.getZ()) * weight, c2.getM() + (c2.getM() - c1.getM()) * weight);
  }

  private Coordinate newCoord(double x, double y, double z, double m) {
    if (includeZ) {
      if (includeM) {
        return new CoordinateXYZM(x, y, z, m);
      } else {
        return new Coordinate(x, y, z);
      }
    } else {
      if (includeM) {
        return new CoordinateXYM(x, y, m);
      } else {
        return new CoordinateXY(x, y);
      }
    }
  }

  /**
   * Reuse the data structure and formula for each ordinate. This includes pre-calculations for each segment, making the
   * interpolation over t simpler.
   */
  private static class CubicPolynomial {
    double c0, c1, c2, c3;
    boolean finite;

    public void init(double c0, double c1, double tangent0, double tangent1) {
      this.c0 = c0;
      this.c1 = tangent0;
      this.c2 = -3 * c0 + 3 * c1 - 2 * tangent0 - tangent1;
      this.c3 = 2 * c0 - 2 * c1 + tangent0 + tangent1;
    }

    public void initNonuniformCatmullRom(double c0, double c1, double c2, double c3, double dt0, double dt1, double dt2) {
      // I don't know if this optimization is worth it, but it seems very likely that the most common use will be
      // "standard" Coordinate objects without actual Z values, so it will probably be a win to early-out those.
      this.finite = Double.isFinite(c0) && Double.isFinite(c1) && Double.isFinite(c2) && Double.isFinite(c3) && Double.isFinite(dt0) && Double.isFinite(dt1) && Double.isFinite(dt2);
      if (this.finite) {
        // Compute tangents
        double t1 = (c1 - c0) / dt0 - (c2 - c0) / (dt0 + dt1) + (c2 - c1) / dt1;
        double t2 = (c2 - c1) / dt1 - (c3 - c1) / (dt1 + dt2) + (c3 - c2) / dt2;

        // Normalize into 0-1 range
        t1 *= dt1;
        t2 *= dt1;

        this.init(c1, c2, t1, t2);
      }
    }

    double eval(double t) {
      if (!this.finite) {
        return Double.NaN;
      }
      double t2 = t * t;
      double t3 = t2 * t;
      return c0 + c1 * t + c2 * t2 + c3 * t3;
    }
  }
}
