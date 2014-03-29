/*
  This code is released under GPL v3.

  @Author: Indiana (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910
 */

package pokerai.hhex.test;

import pokerai.hhex.Consts;
import pokerai.hhex.internal.RawPokerHand;

public class Tests {

  public static void main(String[] args) {
    /*
    HashSet h = new HashSet();
    System.out.println("Long.SIZE: " + Long.SIZE);
    System.out.println("Long.MAX: " + Long.MAX_VALUE);
    System.out.println("Integer.SIZE: " + Integer.SIZE);
    System.out.println("Integer.MAX: " + Integer.MAX_VALUE);
    System.out.println("Short.SIZE: " + Short.SIZE);
    System.out.println("Short.MAX: " + Short.MAX_VALUE);
    System.out.println("Fits in 1 GB: " + (long)((1048576*1024) / Long.SIZE));
    System.out.println("ExpectedL: " + (2000000*Long.SIZE / 1048576));
    System.out.println("ExpectedI: " + (2000000*Integer.SIZE / 1048576));
    System.out.println("ExpectedS: " + (2000000*Short.SIZE / 1048576));
    /*
    System.out.println((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576);
    for (long i = 0; i < 2000000; i++) h.add(i);
    System.out.println("done");
    System.out.println((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576);
    */
    for (byte b1 = 0; b1 < 16; b1++)
      for (byte b2 = 0; b2 < 16; b2++) {
        byte b = RawPokerHand.twoHalfBytes(b1, b2);
        if (RawPokerHand.byte1(b) != b1 || RawPokerHand.byte2(b) != b2) System.out.println("error1, b1 = " + b1 + ", b2 = " + b2 + ", b = " + b + ", b1'= " + RawPokerHand.byte1(b) + ", b2'= " + RawPokerHand.byte2(b));
        if (Consts.isActionWithoutAmountEncoded(b) != Consts.isActionWithoutAmount(b2)) System.out.println("error2, b1 = " + b1 + ", b2 = " + b2 + ", b = " + b + ", b1'= " + RawPokerHand.byte1(b) + ", b2'= " + RawPokerHand.byte2(b));
      }
    testInt3Bytes();
  }

  public static void testInt3Bytes() {
    for (int i = 0; i < 16777216; i++) {
      byte[] b = writeInt3Bytes(i);
      if (RawPokerHand.readInt3Bytes(b, 0) != i) System.out.println("Error for i = " + i + ", res " + RawPokerHand.readInt3Bytes(b, 0));
    }
  }

// used in test only
  public static byte[] writeInt3Bytes(int v) {
    byte[] b = new byte[3];
    b[0] = (byte)(0xff & (v >> 16));
    b[1] = (byte)(0xff & (v >> 8));
    b[2] = (byte)(0xff & (v));
    return b;
  }

  public static double ds(double x) { return Math.round(x*100)/100.0; }
}

