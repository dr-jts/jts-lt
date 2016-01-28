/*
 * The JTS Topology Suite is a collection of Java classes that
 * implement the fundamental operations required to validate a given
 * geo-spatial data set to a known topological specification.
 * 
 * Copyright (C) 2016 Martin Davis
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Martin Davis BSD
 * License v1.0 (found at the root of the repository).
 * 
 */
package org.locationtech.jts.io;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;

import junit.framework.TestCase;

public class WKBWriterTest extends TestCase {

    public WKBWriterTest(String name) {
        super(name);
    }
    
    public void testSRID() throws Exception {
        GeometryFactory gf = new GeometryFactory();
        Point p1 = gf.createPoint(new Coordinate(1,2));
        p1.setSRID(1234);
        
        //first write out without srid set
        WKBWriter w = new WKBWriter();
        byte[] wkb = w.write(p1);
        
        //check the 3rd bit of the second byte, should be unset
        byte b = (byte) (wkb[1] & 0x20);
        assertEquals(0, b);
        
        //read geometry back in
        WKBReader r = new WKBReader(gf);
        Point p2 = (Point) r.read(wkb);
        
        assertTrue(p1.equalsExact(p2));
        assertEquals(0, p2.getSRID());
        
        //not write out with srid set
        w = new WKBWriter(2, true);
        wkb = w.write(p1);
        
        //check the 3rd bit of the second byte, should be set
        b = (byte) (wkb[1] & 0x20);
        assertEquals(0x20, b);
        
        int srid = ((int) (wkb[5] & 0xff) << 24) | ( (int) (wkb[6] & 0xff) << 16)
            | ( (int) (wkb[7] & 0xff) << 8) | (( int) (wkb[8] & 0xff) );
       
        assertEquals(1234, srid);
        
        r = new WKBReader(gf);
        p2 = (Point) r.read(wkb);
        
        //read the geometry back in
        assertTrue(p1.equalsExact(p2));
        assertEquals(1234, p2.getSRID());
    }
}
