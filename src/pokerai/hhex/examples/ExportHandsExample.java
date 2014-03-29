/*
  This code is released under GPL v3.

  @Author: Indiana (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910
 */
package pokerai.hhex.examples;

import pokerai.hhex.Consts;
import pokerai.hhex.PokerHand;
import pokerai.hhex.internal.HandManagerNIO;

import java.io.File;
import java.io.RandomAccessFile;

public class ExportHandsExample {

  public static void main(String[] args) {
    String rootfolder = "C:\\hhdb\\";
    if (args.length > 0) rootfolder = args[0];
    if (!rootfolder.endsWith("\\")) rootfolder += "\\";
    // ---
    File dir = new File(rootfolder);
    String[] all = dir.list();
    // Scan for all files from site PS, Holde NL, 9-seats, and agregate stats for all found DBs
    for (int i = 0; i < all.length; i++) if (all[i].startsWith("pokerai.org.sample" + Consts.SITE_PS + "_" + Consts.HOLDEM_NL + "_9") && all[i].endsWith(".hhex")) {
      exportHands(rootfolder, all[i]);
    }
  }

  public static void exportHands(String rootfolder, String fullName) {
    RandomAccessFile f = null;
    int hands = 0;
    try {
      HandManagerNIO hm = new HandManagerNIO();
      hm.init(rootfolder, fullName);
      hm.reset();
      f = new RandomAccessFile(rootfolder + fullName + ".exported.txt", "rw");
      while (hm.hasMoreHands()) {
        hands++; if (hands > 1000) break;
        PokerHand hand = hm.nextPokerHand();
        if (hand.getNumberOfSeats() > 6) {
          f.writeBytes(hand.toString());
        }
      }
      f.close();
      hm.closedb();
    } catch (Exception e) {
      e.printStackTrace();
      if (f != null) try { f.close(); } catch (Exception e2) {};
    }
  }

}

