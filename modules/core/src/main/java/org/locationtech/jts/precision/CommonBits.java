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
package org.locationtech.jts.precision;

/**
 * Determines the maximum number of common most-significant
 * bits in the mantissa of one or numbers.
 * Can be used to compute the double-precision number which
 * is represented by the common bits.
 * If there are no common bits, the number computed is 0.0.
 *
 * @version 1.7
 */
public class CommonBits {

  /**
   * Computes the bit pattern for the sign and exponent of a
   * double-precision number.
   * 
   * @param num
   * @return the bit pattern for the sign and exponent
   */
  public static long signExpBits(long num)
  {
    return num >> 52;
  }

  /**
 * This computes the number of common most-significant bits in the mantissas
 * of two double-precision numbers.
 * It does not count the hidden bit, which is always 1.
 * It does not determine whether the numbers have the same exponent - if they do
 * not, the value computed by this function is meaningless.
 * 
 * @param num1 the first number
 * @param num2 the second number
 * @return the number of common most-significant mantissa bits
 */
  public static int numCommonMostSigMantissaBits(long num1, long num2)
  {
    int count = 0;
    for (int i = 51; i >= 0; i--)
    {
      if (getBit(num1, i) != getBit(num2, i))
        return count;
      count++;
    }
    return 52;
  }

  /**
   * Zeroes the lower n bits of a bitstring.
   * 
   * @param bits the bitstring to alter
   * @return the zeroed bitstring
   */
  public static long zeroLowerBits(long bits, int nBits)
  {
    long invMask = (1L << nBits) - 1L;
    long mask = ~ invMask;
    long zeroed = bits & mask;
    return zeroed;
  }

  /**
   * Extracts the i'th bit of a bitstring.
   * 
   * @param bits the bitstring to extract from
   * @param i the bit to extract
   * @return the value of the extracted bit
   */
  public static int getBit(long bits, int i)
  {
    long mask = (1L << i);
    return (bits & mask) != 0 ? 1 : 0;
  }

  private boolean isFirst = true;
  private int commonMantissaBitsCount = 53;
  private long commonBits = 0;
  private long commonSignExp;

  public CommonBits() {
  }

  public void add(double num)
  {
    long numBits = Double.doubleToLongBits(num);
    if (isFirst) {
      commonBits = numBits;
      commonSignExp = signExpBits(commonBits);
      isFirst = false;
      return;
    }

    long numSignExp = signExpBits(numBits);
    if (numSignExp != commonSignExp) {
      commonBits = 0;
      return;
    }

//    System.out.println(toString(commonBits));
//    System.out.println(toString(numBits));
    commonMantissaBitsCount = numCommonMostSigMantissaBits(commonBits, numBits);
    commonBits = zeroLowerBits(commonBits, 64 - (12 + commonMantissaBitsCount));
//    System.out.println(toString(commonBits));
  }

  public double getCommon()
  {
    return Double.longBitsToDouble(commonBits);
  }
  /**
   * A representation of the Double bits formatted for easy readability
   */
  public String toString(long bits)
  {
    double x = Double.longBitsToDouble(bits);
    String numStr = Long.toBinaryString(bits);
    String padStr = "0000000000000000000000000000000000000000000000000000000000000000" + numStr;
    String bitStr = padStr.substring(padStr.length() - 64);
    String str = bitStr.substring(0, 1) + "  "
        + bitStr.substring(1, 12) + "(exp) "
        + bitStr.substring(12)
        + " [ " + x + " ]";
    return str;
  }

}
