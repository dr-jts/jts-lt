/*
 * Copyright (c) 2017 LocationTech (www.locationtech.org).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jtslab.simplify;

import org.locationtech.jts.algorithm.Angle;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFactory;
import org.locationtech.jts.geom.CoordinateSequences;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Lineal;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;


/**
 * This class simplifies a line string by removing points that create a sharp turn within a radius from previous point.
 * This class follows the JTS simplifier pattern
 * <p>
 * The code was first developed by Harel Mazor for <a href="https://github.com/IsraelHikingMap">IsrealHikingMap</a>
 * based on <a href="https://github.com/NetTopologySuite/NetTopologySuite">NetTopologySuite</a>.
 * <p>
 * Remarks
 * <ul>
 * <li>This simplifier works best after a {@link DouglasPeuckerSimplifier} simplification.</li>
 * </ul>
 * @author 
 *   Harel Mazor (https://github.com/harelm), concept and idea, <br/>
 *   FObermaier, adaption for JTS 
 * @since 1.15  
 * @see <a href="https://github.com/IsraelHikingMap/Site/blob/master/IsraelHiking.API/Services/RadialDistanceByAngleSimplifier.cs/">Orginal code</a>
 * @see <a href="http://israelhiking.osm.org.il/">Isreal hiking map</a>
 */
public class ShortSharpAngleSimplifier
{
    private Geometry _geometry;
    private double _distanceTolerance;
    private double _angleTolerance;

    /**
     * Sets the radial distance tolerance. The lower it is the less simplified the line will be.
     * The default is {@code 0}.
     */
    public void setDistanceTolerance(double value) { 
      _distanceTolerance = value; 
    }
    
    /**
     * The angle tolerance - the lower the number will be the less simplified the line will be.
     * Unit is in radiant.
     */
    public void setAngleTolerance(double value) { 
      _angleTolerance = Angle.normalizePositive(value); 
    }

    /**
     * This simplifies a line by getting the radial distance tolerance and angle distance tolerance
     * 
     * @param geometry The geometry to simplify
     * @param distanceTolerance The radial distance tolerance.
     * @param angleTolerace The angle tolerance in radiant.
     * 
     * @return A simplified {@link Geometry}
     */
    public static Geometry simplify(Geometry geometry, double distanceTolerance, double angleTolerace)
    {
      ShortSharpAngleSimplifier simplifier = new ShortSharpAngleSimplifier(geometry);
      simplifier.setDistanceTolerance(distanceTolerance);
      simplifier.setAngleTolerance(angleTolerace);

      return simplifier.getResultGeometry();
    }

    /**
     * Creates an instance of this simplifier class for the given {@code geometry}.
     * 
     * @param geometry The geometry to simplify. 
     * @exception IllegalArgumentException Thrown if {@code geometry} is {@code null} or not a {@link Lineal} geometry.
     */
    public ShortSharpAngleSimplifier(Geometry geometry)
    {
      if (geometry == null)
        throw new IllegalArgumentException("geometry must not be null");
      if (!(geometry instanceof Lineal))
        throw new IllegalArgumentException("geometry has to be lineal");

      _geometry = geometry;
    }

    /**
     * Simplifies the geometry, always keeps the first and last point.
     *
     * @returns A simplified {@link Geometry}
     */
    public Geometry getResultGeometry()
    {
      LineString[] lineStrings = 
          new LineString[_geometry.getNumGeometries()];
      
      for (int i = 0; i < _geometry.getNumGeometries(); i++)
        lineStrings[i] = simplify((LineString)_geometry.getGeometryN(i));
      
      if (lineStrings.length == 1)
        return lineStrings[0];
      
      return _geometry.getFactory().createMultiLineString(lineStrings);
      
    }

    private LineString simplify(LineString input)
    {
      CoordinateSequence inputSeq = input.getCoordinateSequence();
      
      if (inputSeq.size() < 2)
        return null;
      
      if (inputSeq.size() == 2)
        return (inputSeq.getCoordinate(0).distance(inputSeq.getCoordinate(1)) < _distanceTolerance)
            ? null : input;
      
      CoordinateSequenceFactory csFactory = input.getFactory().getCoordinateSequenceFactory(); 
      CoordinateSequence outputSeq = csFactory.create(inputSeq.size(), inputSeq.getDimension());
      
      Coordinate c1 = copyCoordinate(inputSeq, 0, outputSeq, 0);
      Coordinate c2 = copyCoordinate(inputSeq, 1, outputSeq, 1);
      int outputIndex = 2;
      for (int i = 2; i < inputSeq.size() - 1; i++) {
        Coordinate current = inputSeq.getCoordinate(i);
        Coordinate future  = inputSeq.getCoordinate(i+1);
        switch (simplifyByAngleDistance(c1, c2, current, future))
        {
          case -1: // ignore current 
            break;
          case 0:  // update last
            c2 = copyCoordinate(inputSeq, i, outputSeq, outputIndex-1);
            break;
          case 1:  // is valid
            c1 = c2;
            c2 = copyCoordinate(inputSeq, i, outputSeq, outputIndex++);;
            break;
        }
      }

      // add last coordinate, no matter what
      copyCoordinate(inputSeq, inputSeq.size()-1, outputSeq, outputIndex++);;

      return _geometry.getFactory().createLineString(
          truncateSequence(outputSeq, outputIndex));
    }

    /**
     * Function to copy a coordinate from one sequence to another, returning the 
     * coordinate copied as result.
     * @param input The input {@link CoordinateSequence}
     * @param inputIndex The index of the input coordinate
     * @param output The output {@link CoordinateSequence}
     * @param outputIndex The index of the output coordinate
     * @return
     */
    private static Coordinate copyCoordinate(
        CoordinateSequence input, int inputIndex,
        CoordinateSequence output, int outputIndex)
    {
      Coordinate res = input.getCoordinate(inputIndex);
      for (int i = 0; i < input.getDimension(); i++)  
        output.setOrdinate(outputIndex, i, input.getOrdinate(inputIndex, i));

      return res;
    }
    
    /**
     * Function to truncate the provided {@link CoordinateSequnce} {@code seq} to the specified {@code size}
     * 
     * @param seq The sequence to truncate.
     * @param size The number of coordinates the sequence can deal with.
     * @return A truncated {@link CoordinateSequence}
     * 
     */
    private CoordinateSequence truncateSequence(CoordinateSequence seq, int size) {
      CoordinateSequence res = _geometry.getFactory().getCoordinateSequenceFactory().create(size, seq.getDimension());
      CoordinateSequences.copy(seq, 0, res, 0, size);
      return res;
    }
    
    /**
     * Ignore the current coordinate
     */
    private final int IGNORE = -1;
    /**
     * Update the last coordinate with the current
     */
    private final int UPDATE = 0;
    /**
     * Add the current coordinate
     */
    private final int ADD = 1;
    
    /**
     * Function to test for the simplification action
     * 
     * @param c1 The pre-previous coordinate 
     * @param c2 The previous coordinate
     * @param c3 The current coordinate
     * @param c4 The next coordinate
     * @return <ul>
     * <li>{@link IGNORE} if {@code c3} should be omitted</li>
     * <li>{@link UPDATE} if {@code c3} should replace {@code c2}</li>
     * <li>{@link ADD} if {@code c3} should be added</li>
     * </ul>
     */
    private int simplifyByAngleDistance(Coordinate c1, Coordinate c2, Coordinate c3, Coordinate c4) {
      
      // get the angle between the three points
      double angle = getAngleDifference(c1, c2, c3);

      // is angle within the tolerance?
      if (isAngleWithinTolerance(angle))
          return ADD;
      // it is acute!
      if (c4 != null) {
        if (c2.distance(c3) < _distanceTolerance ||
            c3.distance(c4) < _distanceTolerance) {
          if (isAngleWithinTolerance(getAngleDifference(c1, c2, c4)))
              return IGNORE;
            
          return UPDATE;
        }
      }
      
      if (c2.distance(c3) >= _distanceTolerance)
        return ADD;

      return IGNORE;
    }

    /**
     * Function to compute the angle difference between the segments c1-c2 and c2-c3.
     * @param c1 The 1st coordinate
     * @param c2 The 2nd coordinate
     * @param c3 The 3rd coordinate
     * @return 
     */
    private static double getAngleDifference(Coordinate c1, Coordinate c2, Coordinate c3) {
      double angle1 = Angle.angle(c1, c2);
      double angle2 = Angle.angle(c2, c3);
      return Math.abs(angle2 - angle1);
    }
    

    /**
     * Function to test if an angle is within {@link _angleTolerance}.
     * @param angle The angle to test
     * @return {@code true} if it does, otherwise {@code false}.
     */
    private boolean isAngleWithinTolerance(double angle)
    {
        return (angle < Math.PI - _angleTolerance) || (angle > Math.PI + _angleTolerance);
    }
    
}
