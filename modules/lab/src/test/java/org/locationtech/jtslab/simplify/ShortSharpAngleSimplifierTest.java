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

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import junit.framework.TestCase;

public class ShortSharpAngleSimplifierTest extends TestCase
{
  GeometryFactory _factory;
  
  public ShortSharpAngleSimplifierTest(String name) {
    super(name);
    _factory = new GeometryFactory();
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(ShortSharpAngleSimplifierTest.class);
  }
  
  public void testSimplifyEmptyLineShouldReturnNull() {
    Geometry input = _factory.createLineString(new Coordinate[0]);
    Geometry simplified = ShortSharpAngleSimplifier.simplify(input, 0, 0);

    assertNull(simplified);
  }

  public void testSimplifyVeryShortLineShouldReturnNull()
  {
      Geometry line = _factory.createLineString(new Coordinate[]
      {
          new Coordinate(0,0),
          new Coordinate(10, 0)
      });

      Geometry simplified = ShortSharpAngleSimplifier.simplify(line, 30, Math.PI / 2);

      assertNull(simplified);
  }

  public void testSimplifyLineAlreadySimplifiedShouldReturnItself()
  {
    Geometry line = _factory.createLineString(new Coordinate[]
      {
          new Coordinate(0,0),
          new Coordinate(100, 0)
      });

    Geometry simplified = ShortSharpAngleSimplifier.simplify(line, 30, Math.PI / 2);

    assertNotNull(simplified);
    assertTrue(line.equalsExact(simplified));
    assertTrue(line == simplified);
  } 

  public void testSimplifyLineZigZagShouldReturnRemoveIt()
  {
    Coordinate coordinateThatShouldBeRemoved = new Coordinate(1, 0);
    Geometry line = _factory.createLineString(new Coordinate[]
      {
          new Coordinate(0,0),
          new Coordinate(10, 0),
          coordinateThatShouldBeRemoved,
          new Coordinate(11, 0),
          new Coordinate(0, 0)
      });

      Geometry simplified = ShortSharpAngleSimplifier.simplify(line, 30, Math.PI / 2);

      CoordinateList coordinates = new CoordinateList(simplified.getCoordinates());
      assertTrue(!coordinates.contains(coordinateThatShouldBeRemoved));
  }

  public void testSimplifyLineNoSimplificationNeededShouldReturnSameLine()
  {
      Geometry line = _factory.createLineString(new Coordinate[]
      {
          new Coordinate(0,0),
          new Coordinate(100, 0),
          new Coordinate(200, 0),
          new Coordinate(300, 0),
      });

      Geometry simplified = ShortSharpAngleSimplifier.simplify(line, 30, Math.PI / 2);

      assertNotNull(simplified);
      assertTrue(line.equalsExact(simplified));
  }

  public void testSimplifyLineShouldSimplifyByAngleShouldReturnSimplifiedLine()
  {
    Geometry line = _factory.createLineString(new Coordinate[]
      {
          new Coordinate(0,0),
          new Coordinate(100, 0),
          new Coordinate(100, 20),
          new Coordinate(100, 200)
      });

    Geometry simplified = ShortSharpAngleSimplifier.simplify(line, 30, Math.PI / 2);

    assertEquals(line.getCoordinates().length - 1, simplified.getCoordinates().length);
  }

  public void testSimplifyLineShouldNotSimplifyByAngleDueToDistanceShouldReturnSimplifiedLine()
  {
    Geometry line = _factory.createLineString(new Coordinate[]
      {
          new Coordinate(0,0),
          new Coordinate(100, 0),
          new Coordinate(0, 1),
          new Coordinate(100, 1)
      });

    Geometry simplified = ShortSharpAngleSimplifier.simplify(line, 30, Math.PI / 2);

    assertTrue(line.equalsExact(simplified));
  }

}
