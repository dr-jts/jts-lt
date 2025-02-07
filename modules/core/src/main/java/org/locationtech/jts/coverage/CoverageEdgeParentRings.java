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

/**
* Contains the adjacent indices of a polygonal coverage according to their relative position
* when compared to a coverage edge. This data structure is utilized by a {@link CoverageEdgeExtractor }
* to set the user data of each extracted coverage edge line geometry.
*
* @author Nick Bowsher
*/
public class CoverageEdgeParentRings {
    /**
     * Construct the pair of Parent ring indices for an edge
     * @param leftIndex the index of the coverage polygon that lies left of the coverage edge
     * @param rightIndex the index of the coverage polygon that lies right of the coverage edge
     */
    public CoverageEdgeParentRings(int leftIndex, int rightIndex){
        this.leftParentIndex = leftIndex;
        this.rightParentIndex = rightIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CoverageEdgeParentRings)) {
            return false;
        }
        CoverageEdgeParentRings casted = (CoverageEdgeParentRings) o;
        return this.leftParentIndex == casted.leftParentIndex && this.rightParentIndex == casted.rightParentIndex;
    }

    public int leftIndex() {
        return leftParentIndex;
    }

    public int rightIndex() {
        return rightParentIndex;
    }
    private int leftParentIndex = -1;
    private int rightParentIndex = -1;
}
