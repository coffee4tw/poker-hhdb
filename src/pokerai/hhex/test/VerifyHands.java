/*
  This code is released under GPL v3.

  @Author: Indiana (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910
 */
package pokerai.hhex.test;

import pokerai.hhex.Action;
import pokerai.hhex.Consts;
import pokerai.hhex.PokerHand;
import pokerai.hhex.internal.HandManagerNIO;

import java.io.File;
import java.util.Hashtable;

public class VerifyHands {

  public static void main(String[] args) {
    String rootfolder = "C:\\hhdb\\";
    if (args.length > 0) rootfolder = args[0];
    if (!rootfolder.endsWith("\\")) rootfolder += "\\";
    // ---
    File dir = new File(rootfolder);
    String[] all = dir.list();
    System.out.println("Verifying all hands for errors ...");
    // Scan for all files from site PS, Holde NL, 9-seats, and agregate stats for all found DBs
    for (int i = 0; i < all.length; i++) if (all[i].startsWith("pokerai.org.sample") && all[i].endsWith(".hhex")) {
      scan(rootfolder, all[i]);
    }
    printResults();
  }

  static double totalHands = 0;
  static int noActionsPreflop = 0, sbnp = 0, bbnp = 0, sbflop = 0, bbflop = 0;

  public static void scan(String rootfolder, String fullName) {
    HandManagerNIO hm = new HandManagerNIO();
    hm.init(rootfolder, fullName);
    hm.reset();
    while (hm.hasMoreHands()) {
      PokerHand hand = hm.nextPokerHand();
      totalHands++;
      if (totalHands % 10000000 == 0) System.out.println((long)totalHands + " hands read.");
      // Check if there is post SB/BB preflop
      Action[] actions = hand.aactionsPreflop();
      if (actions == null) {
        noActionsPreflop++;
        actions = new Action[0];
      }
      boolean sb = false, bb = false;
      for (int i = 0; i < actions.length; i++) {
        if (actions[i].getAction() == Consts.ACTION_SMALLBLIND) sb = true;
        if (actions[i].getAction() == Consts.ACTION_BIGBLIND) bb = true;
      }
      if (!sb) {
        //System.out.println("WARNING: Hand without small blind posted!");
        sbnp++;
      }
      if (!bb) {
        //System.out.println("WARNING: Hand without big blind posted!");
        bbnp++;
      }
      // Post SB/BB on flop must not happen
      actions = hand.aactionsFlop(); if (actions == null) actions = new Action[0];
      sb = false; bb = false;
      for (int i = 0; i < actions.length; i++) {
        if (actions[i].getAction() == Consts.ACTION_SMALLBLIND) sb = true;
        if (actions[i].getAction() == Consts.ACTION_BIGBLIND) bb = true;
      }
      if (sb) {
        //System.out.println("ERROR: Small blind events on flop!");
        sbflop++;
      }
      if (bb) {
        //System.out.println("ERROR: Big blind events on flop!");
        bbflop++;
      }
      int[] stacks = hand.getStacks();
    }
    hm.closedb();
  }

  public static void printResults() {
    System.out.println("Number of hands: " + (long)totalHands);
    System.out.println("  | WARNING: Small blinds not posted: " + sbnp);
    System.out.println("  | WARNING: Big blinds not posted: " + bbnp);
    System.out.println("  | ERROR: No actions preflop: " + noActionsPreflop);
    System.out.println("  | ERROR: Small blind posted on flop: " + sbflop);
    System.out.println("  | ERROR: Big blind posted on flop: " + bbflop);
  }

  public static void inc(Hashtable h, String key) { if (h.get(key) == null) h.put(key, 1); else h.put(key, ((Integer)h.get(key)).intValue()+1); };
  public static double ds(double x) { return Math.round(x*100)/100.0; }

  public static int nt(Hashtable h, String c1, String c2) {
    int n = 0; String[] ss = {"h", "d", "c", "s"};
    for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) { Integer io = (Integer)(h.get(c1 + ss[i] + c2 + ss[j])); if (io != null) n += io; } return n;
  }
}

