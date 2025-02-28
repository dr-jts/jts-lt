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
package org.locationtech.jts.geom.impl;

import java.io.Serializable;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFactory;
import org.locationtech.jts.geom.Coordinates;

/**
 * Builds packed array coordinate sequences. 
 * The array data type can be either
 * <code>double</code> or <code>float</code>, 
 * and defaults to <code>double</code>.
 */
public class PackedCoordinateSequenceFactory implements
    CoordinateSequenceFactory, Serializable
{
  private static final long serialVersionUID = -3558264771905224525L;
  
  /**
   * Type code for arrays of type <code>double</code>.
   */
  public static final int DOUBLE = 0;
  
  /**
   * Type code for arrays of type <code>float</code>.
   */
  public static final int FLOAT = 1;

  /**
   * Type code for dim/measure separated arrays of type <code>double</code>.
   */
  public static final int DOUBLE2 = 1;

  /**
   * A factory using array type {@link #DOUBLE}
   */
  public static final PackedCoordinateSequenceFactory DOUBLE_FACTORY =
      new PackedCoordinateSequenceFactory(DOUBLE);
  
  /**
   * A factory using array type {@link #FLOAT}
   */
  public static final PackedCoordinateSequenceFactory FLOAT_FACTORY =
      new PackedCoordinateSequenceFactory(FLOAT);

  /**
   * A factory using array type {@link #DOUBLE2}
   */
  public static final PackedCoordinateSequenceFactory DOUBLE2_FACTORY =
      new PackedCoordinateSequenceFactory(DOUBLE2);

  private static final int DEFAULT_MEASURES = 0;

  private static final int DEFAULT_DIMENSION = 3;

  private int type = DOUBLE;

  /**
   * Creates a new PackedCoordinateSequenceFactory
   * of type DOUBLE.
   */
  public PackedCoordinateSequenceFactory(){
    this(DOUBLE);
  }

  /**
   * Creates a new PackedCoordinateSequenceFactory
   * of the given type.
   * Acceptable type values are
   * {@linkplain PackedCoordinateSequenceFactory#FLOAT}or
   * {@linkplain PackedCoordinateSequenceFactory#DOUBLE}
   */
  public PackedCoordinateSequenceFactory(int type){
    this.type = type;
  }

  /**
   * Gets the type of packed coordinate sequence this factory builds, either
   * {@linkplain PackedCoordinateSequenceFactory#FLOAT} or
   * {@linkplain PackedCoordinateSequenceFactory#DOUBLE}
   * 
   * @return the type of packed array built
   */
  public int getType() {
    return type;
  }

  /**
   * @see CoordinateSequenceFactory#create(Coordinate[])
   */
  public CoordinateSequence create(Coordinate[] coordinates) {
    int dimension = DEFAULT_DIMENSION;
    int measures = DEFAULT_MEASURES;
    if (coordinates != null && coordinates.length > 0 && coordinates[0] != null) {
      Coordinate first = coordinates[0];
      dimension = Coordinates.dimension(first);
      measures = Coordinates.measures(first);
    }
    if (type == DOUBLE) {
      return new PackedCoordinateSequence.Double(coordinates, dimension, measures);
    } else if (type == FLOAT) {
      return new PackedCoordinateSequence.Float(coordinates,  dimension, measures);
    } else {
      return new PackedCoordinateSequence.Double2(coordinates,  dimension, measures);
    }
  }

  /**
   * @see CoordinateSequenceFactory#create(CoordinateSequence)
   */
  public CoordinateSequence create(CoordinateSequence coordSeq) {
    int dimension = coordSeq.getDimension();
    int measures = coordSeq.getMeasures();
    if (type == DOUBLE) {
      return new PackedCoordinateSequence.Double(coordSeq.toCoordinateArray(), dimension, measures);
    } else if (type == FLOAT) {
      return new PackedCoordinateSequence.Float(coordSeq.toCoordinateArray(), dimension, measures);
    } else {
      return new PackedCoordinateSequence.Double2(coordSeq.toCoordinateArray(), dimension, measures);
    }
  }

  /**
   * Creates a packed coordinate sequence of type {@link #DOUBLE}
   * from the provided array
   * using the given coordinate dimension and a measure count of 0. 
   * 
   * @param packedCoordinates the array containing coordinate values
   * @param dimension the coordinate dimension
   * @return a packed coordinate sequence of type {@link #DOUBLE}
   */
  public CoordinateSequence create(double[] packedCoordinates, int dimension) {
    return create( packedCoordinates, dimension, DEFAULT_MEASURES );
  }
  
  /**
   * Creates a packed coordinate sequence of type {@link #DOUBLE}
   * from the provided array
   * using the given coordinate dimension and measure count. 
   * 
   * @param packedCoordinates the array containing coordinate values
   * @param dimension the coordinate dimension
   * @param measures the coordinate measure count
   * @return a packed coordinate sequence of type {@link #DOUBLE}
   */
  public CoordinateSequence create(double[] packedCoordinates, int dimension, int measures) {
    if (type == DOUBLE) {
      return new PackedCoordinateSequence.Double(packedCoordinates, dimension, measures);
    } else {
      return new PackedCoordinateSequence.Float(packedCoordinates, dimension, measures);
    }
  }

  public CoordinateSequence create(double[] xy, double[] z, double[] m) {
    return new PackedCoordinateSequence.Double2(xy, z, m);
  }

  /**
   * Creates a packed coordinate sequence of type {@link #FLOAT}
   * from the provided array. 
   * 
   * @param packedCoordinates the array containing coordinate values
   * @param dimension the coordinate dimension
   * @return a packed coordinate sequence of type {@link #FLOAT}
   */
  public CoordinateSequence create(float[] packedCoordinates, int dimension) {
    return create( packedCoordinates, dimension, Math.max(DEFAULT_MEASURES, dimension-3) );
  }
  
  /**
   * Creates a packed coordinate sequence of type {@link #FLOAT}
   * from the provided array. 
   * 
   * @param packedCoordinates the array containing coordinate values
   * @param dimension the coordinate dimension
   * @param measures the coordinate measure count
   * @return a packed coordinate sequence of type {@link #FLOAT}
   */
  public CoordinateSequence create(float[] packedCoordinates, int dimension, int measures) {
    if (type == DOUBLE) {
      return new PackedCoordinateSequence.Double(packedCoordinates, dimension, measures);
    } else {
      return new PackedCoordinateSequence.Float(packedCoordinates, dimension, measures);
    }
  }

  /**
   * @see org.locationtech.jts.geom.CoordinateSequenceFactory#create(int, int)
   */
  public CoordinateSequence create(int size, int dimension) {
    if (type == DOUBLE) {
      return new PackedCoordinateSequence.Double(
              size, dimension, Math.max(DEFAULT_MEASURES, dimension-3));
    } else if (type == FLOAT) {
      return new PackedCoordinateSequence.Float(
              size, dimension, Math.max(DEFAULT_MEASURES, dimension-3));
    } else {
      return new PackedCoordinateSequence.Double2(
              size, dimension, Math.max(DEFAULT_MEASURES, dimension-3));
    }
  }
  
  /**
   * @see org.locationtech.jts.geom.CoordinateSequenceFactory#create(int, int, int)
   */
  public CoordinateSequence create(int size, int dimension, int measures) {
    if (type == DOUBLE) {
      return new PackedCoordinateSequence.Double(size, dimension, measures);
    } else if (type == FLOAT) {
      return new PackedCoordinateSequence.Float(size, dimension, measures);
    } else {
      return new PackedCoordinateSequence.Double2(size, dimension, measures);
    }
  }
}
