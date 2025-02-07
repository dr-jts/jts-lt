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

package org.locationtech.jts.precision;

import junit.framework.TestCase;
import junit.textui.TestRunner;

/**
 * Tests for counting number of most-signigicant mantissa bits of two doubles
 *
 * @author Taro Murao
 */
public class CommonBitsTest extends TestCase {
	public CommonBitsTest(String name) {
		super(name);
	}

	public static void main(String args[]) {
		TestRunner.run(CommonBitsTest.class);
	}

	public void testNumCommonMostSigMantissaBits() throws Exception {
		// 0 10000000000 0000000000000000000000000000000000000000000000000000
		Long a = Double.doubleToRawLongBits(2D);

		// 0 10000000000 1000000000000000000000000000000000000000000000000000
		Long b = Double.doubleToRawLongBits(3D);

		// 0 10000000000 1100000000000000000000000000000000000000000000000000
		Long c = Double.doubleToRawLongBits(3.5D);

		// Make sure to fix the bug, in which numCommonMostSigMantissaBits
		// took into account also the least significant exponent digit
		assertEquals(CommonBits.numCommonMostSigMantissaBits(a, b), 0);

		assertEquals(CommonBits.numCommonMostSigMantissaBits(b, c), 1);

		assertEquals(CommonBits.numCommonMostSigMantissaBits(a, a), 52);
	}
}
