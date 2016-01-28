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

package org.locationtech.jts.geom.impl;

import org.locationtech.jts.geom.CoordinateSequenceFactory;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory;

import junit.textui.TestRunner;

/**
 * Test {@link PackedCoordinateSequence}
 * @version 1.7
 */
public class PackedCoordinateSequenceTest
    extends CoordinateSequenceTestBase
{
  public static void main(String args[]) {
    TestRunner.run(PackedCoordinateSequenceTest.class);
  }

  public PackedCoordinateSequenceTest(String name)
  {
    super(name);
  }

  @Override
  CoordinateSequenceFactory getCSFactory() {
    return new PackedCoordinateSequenceFactory();
  }

}