/*
 * Copyright (c) 2024 Kristin Cowalcijk.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.io;

import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFilter;

import java.util.EnumSet;

/**
 * A filter implementation to test if a coordinate sequence actually has meaningful values for an
 * ordinate bit-pattern
 */
class CheckOrdinatesFilter implements CoordinateSequenceFilter {

  private final EnumSet<Ordinate> checkOrdinateFlags;
  private final EnumSet<Ordinate> outputOrdinates;

  /**
   * Creates an instance of this class
   *
   * @param checkOrdinateFlags the index for the ordinates to test.
   */
  CheckOrdinatesFilter(EnumSet<Ordinate> checkOrdinateFlags) {

    this.outputOrdinates = EnumSet.of(Ordinate.X, Ordinate.Y);
    this.checkOrdinateFlags = checkOrdinateFlags;
  }

  /**
   * @see CoordinateSequenceFilter#isGeometryChanged
   */
  public void filter(CoordinateSequence seq, int i) {

    if (checkOrdinateFlags.contains(Ordinate.Z) && !outputOrdinates.contains(Ordinate.Z)) {
        if (!Double.isNaN(seq.getZ(i))) {
            outputOrdinates.add(Ordinate.Z);
        }
    }

    if (checkOrdinateFlags.contains(Ordinate.M) && !outputOrdinates.contains(Ordinate.M)) {
        if (!Double.isNaN(seq.getM(i))) {
            outputOrdinates.add(Ordinate.M);
        }
    }
  }

  /**
   * @see CoordinateSequenceFilter#isGeometryChanged
   */
  public boolean isGeometryChanged() {
    return false;
  }

  /**
   * @see CoordinateSequenceFilter#isDone
   */
  public boolean isDone() {
    return outputOrdinates.equals(checkOrdinateFlags);
  }

  /**
   * Gets the evaluated ordinate bit-pattern
   *
   * @return A bit-pattern of ordinates with valid values masked by {@link #checkOrdinateFlags}.
   */
  EnumSet<Ordinate> getOutputOrdinates() {
    return outputOrdinates;
  }
}
