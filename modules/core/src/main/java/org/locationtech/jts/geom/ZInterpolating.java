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
package org.locationtech.jts.geom;

/**
 * Internal class which encapsulates the runtime switch to Z-interpolate when applicable.
 * <p>
 * <ul>
 * <li><code>jts.zinterpolating=false</code> - (default) do not Z-interpolate
 * <li><code>jts.zinterpolating=true</code> - Z-interpolate
 * </ul>
 * 
 * @author bjornharrtell
 *
 */
public class ZInterpolating {
    public static String ZINTERPOLATING_PROPERTY_NAME = "jts.zinterpolating";
    public static String ZINTERPOLATING_PROPERTY_VALUE_TRUE = "true";
    public static String ZINTERPOLATING_PROPERTY_VALUE_FALSE = "false";
    public static boolean ZINTERPOLATING_DEFAULT = true;
    private static boolean isZInterpolating = ZINTERPOLATING_DEFAULT;

    static {
        setZInterpolatingImpl(System.getProperty(ZINTERPOLATING_PROPERTY_NAME));
    }

    public static boolean getZInterpolating() {
        return isZInterpolating;
    }

    public static void setZInterpolating(boolean zInterpolating) {
        isZInterpolating = zInterpolating;
    }

    private static void setZInterpolatingImpl(String isZInterpolatingCode) {
        if (isZInterpolatingCode == null)
            return;
            isZInterpolating = ZINTERPOLATING_DEFAULT;
        if (ZINTERPOLATING_PROPERTY_VALUE_TRUE.equalsIgnoreCase(isZInterpolatingCode))
            isZInterpolating = true;
    }
}
