/*
 * Copyright (c) 2016 Vivid Solutions.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jtstest.function;

import org.locationtech.jts.algorithm.Angle;
import org.locationtech.jts.algorithm.MaximumInscribedCircle;
import org.locationtech.jts.algorithm.MinimumBoundingCircle;
import org.locationtech.jts.algorithm.MinimumDiameter;
import org.locationtech.jts.densify.Densifier;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.OctagonalEnvelope;
import org.locationtech.jtstest.geomfunction.Metadata;

public class ConstructionFunctions {
  public static Geometry octagonalEnvelope(Geometry g) { return OctagonalEnvelope.octagonalEnvelope(g); }
  
  public static Geometry minimumDiameter(Geometry g) {      return (new MinimumDiameter(g)).getDiameter();  }
  public static double minimumDiameterLength(Geometry g) {      return (new MinimumDiameter(g)).getDiameter().getLength();  }

  public static Geometry minimumRectangle(Geometry g) { return (new MinimumDiameter(g)).getMinimumRectangle();  }
  
  public static Geometry minimumBoundingCircle(Geometry g) { return (new MinimumBoundingCircle(g)).getCircle();  }
  public static double minimumBoundingCircleDiameterLen(Geometry g) {      return 2 * (new MinimumBoundingCircle(g)).getRadius();  }

  public static Geometry maximumDiameter(Geometry g) {      return (new MinimumBoundingCircle(g)).getMaximumDiameter();  }
  public static double maximumDiameterLength(Geometry g) {  
    return (new MinimumBoundingCircle(g)).getMaximumDiameter().getLength();
  }
  
  public static Geometry boundary(Geometry g) {      return g.getBoundary();  }
  public static Geometry convexHull(Geometry g) {      return g.convexHull();  }
  public static Geometry centroid(Geometry g) {      return g.getCentroid();  }
  public static Geometry interiorPoint(Geometry g) {      return g.getInteriorPoint();  }

  public static Geometry densify(Geometry g, double distance) { return Densifier.densify(g, distance); }
  
  @Metadata(description="Constructs the center point of the Maximum Inscribed Circle")
  public static Geometry maximumInscribedCircleCenter(Geometry g,
      @Metadata(title="Distance tolerance")
      double tolerance) { 
    return MaximumInscribedCircle.getCenter(g, tolerance); 
  }
  
  @Metadata(description="Constructs the boundary point of the Maximum Inscribed Circle ")
  public static Geometry maximumInscribedCircleBoundaryPoint(Geometry g,
      @Metadata(title="Distance tolerance")
      double tolerance) { 
    MaximumInscribedCircle mic = new MaximumInscribedCircle(g, tolerance); 
    return mic.getBoundaryPoint(); 
  }
  
  @Metadata(description="Constructs the Maximum Inscribed Circle of a Polygon")
  public static Geometry maximumInscribedCircle(Geometry g,
      @Metadata(title="Distance tolerance")
      double tolerance) { 
    MaximumInscribedCircle mic = new MaximumInscribedCircle(g, tolerance); 
    Coordinate center = mic.getCenter().getCoordinate();
    Coordinate radiusPt = mic.getBoundaryPoint().getCoordinate();
    LineString radiusLine = g.getFactory().createLineString(new Coordinate[] { center, radiusPt });
    return circleByRadiusLine(radiusLine, 60);
  }
  
  @Metadata(description="Computes the radius length of the Maximum Inscribe Circle")
  public static double maximumInscribedCircleRadius(Geometry g, 
      @Metadata(title="Distance tolerance")
      double tolerance) { 
    MaximumInscribedCircle mic = new MaximumInscribedCircle(g, tolerance); 
    Geometry center = mic.getCenter();
    Geometry radiusPt = mic.getBoundaryPoint();
    return center.distance(radiusPt);
  }

  @Metadata(description="Constructs an n-point circle from a 2-point line giving the radius")
  public static Geometry circleByRadiusLine(Geometry radiusLine,
      @Metadata(title="Number of vertices")
      int nPts) {
    Coordinate[] radiusPts = radiusLine.getCoordinates();
    Coordinate center = radiusPts[0];
    Coordinate radiusPt = radiusPts[1];
    double dist = radiusPt.distance(center);
    
    double angInc = 2 * Math.PI / (nPts - 1);
    Coordinate[] circlePts = new Coordinate[nPts + 1];
    circlePts[0] = radiusPt.copy();
    circlePts[nPts] = radiusPt.copy();
    double angStart = Angle.angle(center, radiusPt);
    for (int i = 1; i < nPts; i++) {
      double x = center.getX() + dist * Math.cos(angStart + i * angInc);
      double y = center.getY() + dist * Math.sin(angStart + i * angInc);
      circlePts[i] =  new Coordinate(x,y);
    }
    return radiusLine.getFactory().createPolygon(circlePts);
  }
}
