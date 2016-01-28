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
package org.locationtech.jtstest.testbuilder.ui.tools;

import org.locationtech.jtstest.testbuilder.model.*;


/**
 * @version 1.7
 */
public class LineStringTool extends AbstractStreamDrawTool 
{
    private static LineStringTool singleton = null;

    public static LineStringTool getInstance() {
        if (singleton == null)
            singleton = new LineStringTool();
        return singleton;
    }

    private LineStringTool() 
    {
    }
    
    protected int getGeometryType()
    {
    	return GeometryType.LINESTRING;
    }
 }
