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
package org.locationtech.jts.simplify;

import org.locationtech.jts.densify.LittleThumblingDensifier;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

/**
 * Line gaussian smoothing.
 * 
 * @author julien Gaffuri
 *
 */
public class GaussianLineSmoothing {

	/**
	 * @param line
	 * @param sigmaM
	 * @return
	 */
	public static LineString get(LineString line, double sigmaM){ return get(line, sigmaM, -1); }

	/**
	 * Line gaussian smoothing.
	 * The position of each point is the average position of its neighbors, weighted by a gaussian kernel.
	 * For non-closed lines, the initial and final points are preserved.
	 * 
	 * @param line The input line
	 * @param sigmaM The standard deviation of the gaussian kernel. The larger, the more smoothed.
	 * @param resolution The target resolution of the geometry. This parameter is used to filter the final geometry.
	 * @return
	 */
	public static LineString get(LineString line, double sigmaM, double resolution){
		if(line.getCoordinates().length <= 2) return (LineString) line.copy();

		boolean isClosed = line.isClosed();
		double length = line.getLength();
		double densifiedResolution = sigmaM/3;

		//handle extreme cases: too large sigma resulting in too large densified resolution.
		if(densifiedResolution > 0.25*length ) {
			if(isClosed){
				//return tiny triangle nearby center point
				Coordinate c = line.getCentroid().getCoordinate();
				length *= 0.01;
				return line.getFactory().createLineString(new Coordinate[]{ new Coordinate(c.x-length,c.y-length), new Coordinate(c.x,c.y+length), new Coordinate(c.x+length,c.y-length), new Coordinate(c.x-length,c.y-length) });
			} else {
				//return segment
				return line.getFactory().createLineString(new Coordinate[]{ line.getCoordinateN(0), line.getCoordinateN(line.getNumPoints()-1) });
			}
		}

		//compute densified line
		Coordinate[] densifiedCoords = LittleThumblingDensifier.densify(line, densifiedResolution).getCoordinates();

		//build ouput line structure
		int nb = (int) (length/densifiedResolution);
		Coordinate[] out = new Coordinate[nb+1];

		//prepare gaussian coefficients
		int n = 7*3; //it should be: E(7*sigma/densifiedResolution), which is 7*3;
		double gcs[] = new double[n+1];
		{
			double a = sigmaM*Math.sqrt(2*Math.PI);
			double b = sigmaM*sigmaM*2;
			double d = densifiedResolution*densifiedResolution;
			for(int i=0; i<n+1; i++) gcs[i] = Math.exp(-i*i*d/b) /a;
		}

		Coordinate c0 = densifiedCoords[0];
		Coordinate cN = densifiedCoords[nb];
		for(int i=0; i<nb; i++) {
			if(!isClosed && i==0) continue;

			//compute coordinates of point i of the smoothed line (gauss mean)
			double x=0.0, y=0.0;
			for(int j=-n; j<=n; j++) {
				//index of the point to consider on the original densified line
				int q = i+j;
				//find coordinates (xq,yq) of point q
				double xq, yq;
				if(q<0) {
					if(isClosed) {
						//make loop to get the right point
						q = q%nb; if(q<0) q+=nb;
						Coordinate c = densifiedCoords[q];
						xq = c.x;
						yq = c.y;
					} else {
						//get symetric point
						q = (-q)%nb; if(q==0) q=nb;
						Coordinate c = densifiedCoords[q];
						xq = 2*c0.x-c.x;
						yq = 2*c0.y-c.y;
					}
				} else if (q>nb) {
					if(isClosed) {
						//make loop to get the right point
						q = q%nb; if(q==0) q=nb;
						Coordinate c = densifiedCoords[q];
						xq = c.x;
						yq = c.y;
					} else {
						//get symetric point
						q = nb-q%nb; if(q==nb) q=0;
						Coordinate c = densifiedCoords[q];
						xq = 2*cN.x-c.x;
						yq = 2*cN.y-c.y;
					}
				} else {
					//general case (most frequent)
					Coordinate c = densifiedCoords[q];
					xq = c.x;
					yq = c.y;
				}
				//get gaussian coefficient
				double gc = gcs[j>=0?j:-j];
				//add contribution of point q to new position of point i
				x += xq*gc;
				y += yq*gc;
			}
			//assign smoothed position of point i
			out[i] = new Coordinate(x*densifiedResolution, y*densifiedResolution);
		}

		//handle start and end points
		if(isClosed) {
			//ensure start and end locations are the same
			out[nb] = out[0];
		} else {
			//ensure start and end points are at the same position as the initial geometry
			out[0] = densifiedCoords[0];
			out[nb] = densifiedCoords[densifiedCoords.length-1];
		}

		//prepare final line, applying some filtering
		LineString lsOut = line.getFactory().createLineString(out);
		if(resolution<0) resolution = densifiedResolution /3;
		lsOut = (LineString) DouglasPeuckerSimplifier.simplify( lsOut , resolution);

		return lsOut;
	}

}
