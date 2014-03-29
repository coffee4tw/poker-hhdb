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
import java.util.Hashtable;

public class SampleUsage {

  public static void main(String[] args) {
    String rootfolder = "C:\\hhdb\\";
    if (args.length > 0) rootfolder = args[0];
    if (!rootfolder.endsWith("\\")) rootfolder += "\\";
    // ---
    File dir = new File(rootfolder);
    String[] all = dir.list();
    // Scan for all files from site PS, Holde NL, 9-seats, and agregate stats for all found DBs
    time = System.currentTimeMillis();
    for (int i = 0; i < all.length; i++) if (all[i].startsWith("pokerai.org.sample" + Consts.SITE_PS /*+ "_" + Consts.HOLDEM_NL + "_9"*/) && all[i].endsWith(".hhex")) {
      scan(rootfolder, all[i]);
    }
    printStats();
    PlayerStatistics.main(args);
  }

  static long time = 0, hmTimeRead = 0, hmTimeParse = 0, size = 0;
  static double totalHands = 0, matchingHands = 0, shortStackPlayers = 0, deepStackPlayers = 0, players = 0;
  static double actionsPreflop = 0, actionsFlop = 0, actionsTurn = 0, actionsRiver = 0;
  static Hashtable handsAtShowdown = new Hashtable();
  static int[] numberOfPlayers = new int[11];

  public static void scan(String rootfolder, String fullName) {
    HandManagerNIO hm = new HandManagerNIO();
    size += hm.init(rootfolder, fullName);
    hm.reset();
    while (hm.hasMoreHands()) {
      PokerHand hand = hm.nextPokerHand();
      //System.out.println(h.gameId);
      totalHands++;
      if (totalHands % 10000000 == 0) System.out.println((long)totalHands + " hands read.");
//      if (hand.getGameType() == Consts.HOLDEM_NL && hand.getNumberOfSeats() > 6) {
        matchingHands++;
        int tempPl = 0;
        //System.out.println("BB: " + hand.bigBlind());
        int[] stacks = hand.getStacks();
        for (byte seat = 0; seat < hand.getNumberOfSeats(); seat++) if (stacks[seat] > 0) {
          if (stacks[seat] < 3000) shortStackPlayers++; else deepStackPlayers++;
          players++;
          tempPl++;
          //System.out.print(ds(hand.getMoneyMade(seat)/100.0)+ " ");
        }
        //System.out.println();
        numberOfPlayers[tempPl]++;
        if (hand.aactionsPreflop() != null && hand.aactionsPreflop().length > 0) actionsPreflop += hand.aactionsPreflop().length;
        if (hand.aactionsFlop() != null && hand.aactionsFlop().length > 0) actionsFlop += hand.aactionsFlop().length;
        if (hand.aactionsTurn() != null && hand.aactionsTurn().length > 0) actionsTurn += hand.aactionsTurn().length;
        if (hand.aactionsRiver() != null && hand.aactionsRiver().length > 0) actionsRiver += hand.aactionsRiver().length;
        for (byte i = 0; i < hand.getNumberOfSeats(); i++) {
          String cards = hand.getCards(i); if (!cards.equals("")) inc(handsAtShowdown, cards);
        }
//      }
      //System.out.println();
    }
    hmTimeRead += hm.timeRead;
    hmTimeParse += hm.timeParse;
    hm.closedb();
  }

  public static void printStats() {
    System.out.println("Number of hands: " + (long)matchingHands + " (of total " + (long)totalHands + "), " + ds(size / totalHands) + " bytes/hand");
    System.out.println("Average number of players per table: " + ds((players*1.0)/matchingHands));
    System.out.print("Distribution: "); for (int i = 0; i < numberOfPlayers.length; i++) System.out.print(numberOfPlayers[i] + " "); System.out.println();
    System.out.println("Total times shortstacker participated in a hand: " + shortStackPlayers);
    System.out.println(ds((shortStackPlayers / players)*ds((players*1.0)/matchingHands)) + " (" + ds(100*shortStackPlayers / players) + "%) shortstackers per table.");
    long time2 = System.currentTimeMillis() - time;
    long handsPerSecond = (long)(((1000.0*totalHands) / time2)); if (handsPerSecond == 0) handsPerSecond++;
    System.out.println("Average preflop actions per hand: " + ds(actionsPreflop / matchingHands));
    System.out.println("Average    flop actions per hand: " + ds(actionsFlop / matchingHands));
    System.out.println("Average    turn actions per hand: " + ds(actionsTurn / matchingHands));
    System.out.println("Average   river actions per hand: " + ds(actionsRiver / matchingHands));
    System.out.println("Number of times AA was seen at showdown: " + nt(handsAtShowdown, "A", "A"));
    System.out.println("Number of times KK was seen at showdown: " + nt(handsAtShowdown, "K", "K"));
    System.out.println("Number of times 77 was seen at showdown: " + nt(handsAtShowdown, "7", "7"));
    System.out.println("Number of times 22 was seen at showdown: " + nt(handsAtShowdown, "2", "2"));
    System.out.println("Read & processing speed: " + handsPerSecond + " hands/second. ");
    System.out.println("Read & processing time: " + (long)(time2/1000.0) + " second. ");
    System.out.println("Read time (ms): " + hmTimeRead + ", parse time (ms): " + hmTimeParse);
    System.out.println("Estimated time to read & process 200 million hands: " + (200000000 / handsPerSecond)
            + " seconds (" + ((200000000 / handsPerSecond)/60) + " minutes). ");
  }

  public static void inc(Hashtable h, String key) { if (h.get(key) == null) h.put(key, 1); else h.put(key, ((Integer)h.get(key)).intValue()+1); };
  public static double ds(double x) { return Math.round(x*100)/100.0; }

  public static int nt(Hashtable h, String c1, String c2) {
    int n = 0; String[] ss = {"h", "d", "c", "s"};
    for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) { Integer io = (Integer)(h.get(c1 + ss[i] + c2 + ss[j])); if (io != null) n += io; } return n;
  }
}

