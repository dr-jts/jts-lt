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
package org.locationtech.jts.densify;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.util.GeometryTransformer;

/**
 * Densify a geometry using the "Little Thumbling" strategy: Add a vertex for each step along the line.
 * 
 * @author julien Gaffuri
 *
 */
public class LittleThumblingDensifier {

	/**
	 * Densify a geometry: Walk along the line step by step and record the positions at each step
	 * (like the "Little Thumbling" would do).
	 * Return the line composed of the recorded positions.
	 * The vertices of the input geometry are not kept, which can result in an output line with shorter length.
	 * The result is different from org.locationtech.jts.densify.Densifier
	 * 
	 * @param geom
	 * @param stepLength the step length
	 * @return the densified geometry
	 */
	public static Geometry densify(Geometry geom, double stepLength) {
		LittleThumblingDensifier densifier = new LittleThumblingDensifier(geom);
		densifier.setStepLength(stepLength);
		return densifier.getResultGeometry();
	}

	/**
	 * Densifies a coordinate sequence.
	 * 
	 * @param pts
	 * @param stepLength the step length
	 * @return the densified coordinate sequence
	 */
	private static Coordinate[] densifyPoints(Coordinate[] pts, double stepLength, PrecisionModel precModel) {
		//out coords
		LineString line = new GeometryFactory(precModel).createLineString(pts);
		int nb = (int) (line.getLength()/stepLength);
		Coordinate[] out = new Coordinate[nb+1];

		double d=0.0, a=0.0, dTot;
		int densIndex=0;
		for(int i=0; i<pts.length-1; i++) {
			Coordinate c0=pts[i], c1=pts[i+1];
			dTot = c0.distance(c1);
			if (d<=dTot) a = Math.atan2( c1.y-c0.y, c1.x-c0.x );
			while(d <= dTot){
				//use LineSegment.pointAlong instead ?
				Coordinate c = new Coordinate(c0.x+d*Math.cos(a), c0.y+d*Math.sin(a));
				precModel.makePrecise(c);
				out[densIndex] = c;
				densIndex++;
				d+=stepLength;
			}
			d-=dTot;
		}
		out[nb] = pts[pts.length-1];
		return out;
	}


	private Geometry inputGeom;
	private double stepLength;

	public LittleThumblingDensifier(Geometry inputGeom) {
		this.inputGeom = inputGeom;
	}

	public void setStepLength(double stepLength) {
		if (stepLength <= 0.0)
			throw new IllegalArgumentException("Step length must be positive");
		this.stepLength = stepLength;
	}

	/**
	 * Gets the densified geometry.
	 * 
	 * @return the densified geometry
	 */
	public Geometry getResultGeometry() {
		return (new LittleThumblingDensifyTransformer(stepLength)).transform(inputGeom);
	}


	static class LittleThumblingDensifyTransformer extends GeometryTransformer {
		double stepLength;

		LittleThumblingDensifyTransformer(double stepLength) {
			this.stepLength = stepLength;
		}

		protected CoordinateSequence transformCoordinates(CoordinateSequence coords, Geometry parent) {
			Coordinate[] inputPts = coords.toCoordinateArray();
			Coordinate[] newPts = LittleThumblingDensifier.densifyPoints(inputPts, stepLength, parent.getPrecisionModel());
			// prevent creation of invalid linestrings
			if (parent instanceof LineString && newPts.length == 1) {
				newPts = new Coordinate[0];
			}
			return factory.getCoordinateSequenceFactory().create(newPts);
		}

		protected Geometry transformPolygon(Polygon geom, Geometry parent) {
			Geometry roughGeom = super.transformPolygon(geom, parent);
			// don't try and correct if the parent is going to do this
			if (parent instanceof MultiPolygon) {
				return roughGeom;
			}
			return createValidArea(roughGeom);
		}

		protected Geometry transformMultiPolygon(MultiPolygon geom, Geometry parent) {
			Geometry roughGeom = super.transformMultiPolygon(geom, parent);
			return createValidArea(roughGeom);
		}

		/**
		 * Creates a valid area geometry from one that possibly has bad topology
		 * (i.e. self-intersections). Since buffer can handle invalid topology, but
		 * always returns valid geometry, constructing a 0-width buffer "corrects"
		 * the topology. Note this only works for area geometries, since buffer
		 * always returns areas. This also may return empty geometries, if the input
		 * has no actual area.
		 * 
		 * @param roughAreaGeom
		 *          an area geometry possibly containing self-intersections
		 * @return a valid area geometry
		 */
		private Geometry createValidArea(Geometry roughAreaGeom) {
			return roughAreaGeom.buffer(0.0);
		}
	}

}
