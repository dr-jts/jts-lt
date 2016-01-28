

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
package org.locationtech.jts.operation.overlay;

/**
 * @version 1.7
 */
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geomgraph.*;

/**
 * Creates nodes for use in the {@link PlanarGraph}s constructed during
 * overlay operations.
 *
 * @version 1.7
 */
public class OverlayNodeFactory
  extends NodeFactory
{
  public Node createNode(Coordinate coord)
  {
    return new Node(coord, new DirectedEdgeStar());
  }
}
