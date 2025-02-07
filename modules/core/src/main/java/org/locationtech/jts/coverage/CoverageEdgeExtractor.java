/*
 * Copyright (c) 2024 Nick Bowsher.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.coverage;

import org.locationtech.jts.geom.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts the set of unique coverage edges from a polygonal coverage.
 * The coverage edges are returned as an array of linear geometries.
 * The input coverage should be valid according to {@link CoverageValidator}.
 *
 * @author Nick Bowsher
 */
public class CoverageEdgeExtractor {
    /**
     * Extracts the set of unique coverage edges from a polygonal coverage.
     *
     * @param coverage an array of polygons forming a coverage
     * @return an array of linear geometries representing coverage edges
     */
    public static Geometry[] extract(Geometry[] coverage) {
        CoverageEdgeExtractor e = new CoverageEdgeExtractor(coverage);
        return e.extract();
    }

    /**
     * Creates a new coverage edge extractor
     *
     * @param coverage an array of polygons forming a coverage
     */
    public CoverageEdgeExtractor(Geometry[] coverage) {
        this.coverage = coverage;
    }

    /**
     * Extracts the set of unique coverage edges from a polygonal coverage.
     * The result is an array of the same size as the amount of coverage edges.
     *
     * @return an array of linear geometries representing coverage edges with the {@link CoverageEdgeParentRings} set as user data for each coverage edge line geometry
     */
    public Geometry[] extract() {
        CoverageRingEdges covRings = new CoverageRingEdges(coverage);
        GeometryFactory f = new GeometryFactory(new PrecisionModel(1000));
        List<LineString> lines = new ArrayList<LineString>();
        for (CoverageEdge edge : covRings.getEdges()) {
            LineString line = edge.toLineString(f);
            line.setUserData(parentRings(edge, f));
            lines.add(line);
        }
        return GeometryFactory.toLineStringArray(lines);
    }

    /**
    * Retrieves the indices of the adjacent coverage polygons and verifies if they are to the left or right of the edge
    *
    * @param edge a coverage edge
    * @param f a geometry factory
    *
    * @return the edge parent indices from the coverage
    */
    private CoverageEdgeParentRings parentRings(CoverageEdge edge, GeometryFactory f){
        int index0 = edge.getAdjacentIndex(0);
        int index1 = edge.getAdjacentIndex(1);

        Coordinate start = edge.getStartCoordinate();
        Coordinate end = edge.getCoordinates()[1];
        Coordinate midPoint = LineSegment.midPoint(start, end);

        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double gridSize = f.getPrecisionModel().gridSize();
        Coordinate leftPoint = new Coordinate(midPoint.x - dy * gridSize, midPoint.y + dx * gridSize);

        if (coverage[index0].contains(f.createPoint(leftPoint))){
            return new CoverageEdgeParentRings(index0, index1);
        }
        return new CoverageEdgeParentRings(index1, index0);
    }

    private Geometry[] coverage;
}
