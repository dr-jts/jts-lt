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
package org.locationtech.jts.geomgraph;

import org.locationtech.jts.geom.Coordinate;


/**
 * @version 1.7
 */
public class NodeFactory {
/**
 * The basic node constructor does not allow for incident edges
 */
  public Node createNode(Coordinate coord)
  {
    return new Node(coord, null);
  }
}
