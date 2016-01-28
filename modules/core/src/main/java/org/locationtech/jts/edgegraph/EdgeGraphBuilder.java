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

package org.locationtech.jts.edgegraph;

import java.util.Collection;
import java.util.Iterator;

import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryComponentFilter;
import org.locationtech.jts.geom.LineString;


/**
 * Builds an edge graph from geometries containing edges.
 * 
 * @author mdavis
 *
 */
public class EdgeGraphBuilder 
{
  public static EdgeGraph build(Collection geoms) {
    EdgeGraphBuilder builder = new EdgeGraphBuilder();
    builder.add(geoms);
    return builder.getGraph();
  }

  private EdgeGraph graph = new EdgeGraph();

  public EdgeGraphBuilder()
  {
    
  }
  
  public EdgeGraph getGraph()
  {
    return graph;
  }
  
  /**
   * Adds the edges of a Geometry to the graph. 
   * May be called multiple times.
   * Any dimension of Geometry may be added; the constituent edges are
   * extracted.
   * 
   * @param geometry geometry to be added
   */  
  public void add(Geometry geometry) {
    geometry.apply(new GeometryComponentFilter() {
      public void filter(Geometry component) {
        if (component instanceof LineString) {
          add((LineString)component);
        }
      }      
    });
  }
  /**
   * Adds the edges in a collection of {@link Geometry}s to the graph. 
   * May be called multiple times.
   * Any dimension of Geometry may be added.
   * 
   * @param geometries the geometries to be added
   */
  public void add(Collection geometries) 
  {
    for (Iterator i = geometries.iterator(); i.hasNext(); ) {
      Geometry geometry = (Geometry) i.next();
      add(geometry);
    }
  }
  
  private void add(LineString lineString) {
    CoordinateSequence seq = lineString.getCoordinateSequence();
    for (int i = 1; i < seq.size(); i++) {
      graph.addEdge(seq.getCoordinate(i-1), seq.getCoordinate(i));
    }
  }

  
}
