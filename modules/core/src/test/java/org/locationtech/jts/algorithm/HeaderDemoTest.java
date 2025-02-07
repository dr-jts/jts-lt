/*
 * Copyright (c) 2022 Felix Obermaier.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.locationtech.jts.algorithm;

import junit.framework.TestCase;
import junit.textui.TestRunner;

public class HeaderDemoTest extends TestCase
{
  public static void main(String[] args) {
    TestRunner.run(DistanceTest.class);
  }

  public HeaderDemoTest(String name) { super(name); }

  public void testCompute()
  {
    assertTrue(new HeaderDemo().compute());
  }
}
