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

package org.locationtech.jtstest.testbuilder.ui.style;

import java.awt.*;
import java.awt.geom.*;

import org.locationtech.jts.geom.*;
import org.locationtech.jtstest.*;
import org.locationtech.jtstest.testbuilder.AppConstants;
import org.locationtech.jtstest.testbuilder.ui.Viewport;


public class VertexStyle  implements Style
{
  private double sizeOver2 = AppConstants.VERTEX_SIZE / 2d;
  
  protected Rectangle shape;
  private Color color;
  
  // reuse point objects to avoid creation overhead
  private Point2D pM = new Point2D.Double();
  private Point2D pV = new Point2D.Double();

  public VertexStyle(Color color) {
    this.color = color;
    // create basic rectangle shape
    shape = new Rectangle(0,
        0, 
        AppConstants.VERTEX_SIZE, 
        AppConstants.VERTEX_SIZE);
  }


  public void paint(Geometry geom, Viewport viewport, Graphics2D g)
  {
    g.setPaint(color);
    Coordinate[] coordinates = geom.getCoordinates();
    
    for (int i = 0; i < coordinates.length; i++) {
        if (! viewport.containsInModel(coordinates[i])) {
            //Otherwise get "sun.dc.pr.PRException: endPath: bad path" exception 
            continue;
        }       
        pM.setLocation(coordinates[i].x, coordinates[i].y);
        viewport.toView(pM, pV);
      	shape.setLocation((int) (pV.getX() - sizeOver2), (int) (pV.getY() - sizeOver2));
        g.fill(shape);
    }
  }
  
}
