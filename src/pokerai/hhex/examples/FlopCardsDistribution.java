/*
  This code is released under GPL v3.

  @Author: Indiana (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910

  This example calculates which cards (if any) flop more often than others.
 */
package pokerai.hhex.examples;

import pokerai.hhex.Consts;
import pokerai.hhex.PokerHand;
import pokerai.hhex.internal.HandManagerNIO;

import java.io.File;

public class FlopCardsDistribution {

  static int minPlayers = 8;
  static int seats = 9;
  static long cards[] = new long[13];
  static int totalHands = 0;

  public static void main(String[] args) {
    String rootfolder = "C:\\hhdb\\";
    if (args.length > 0) rootfolder = args[0];
    if (!rootfolder.endsWith("\\")) rootfolder += "\\";
    // ---
    File dir = new File(rootfolder);
    String[] all = dir.list();
    // Scan for all files from site "Default", Holdem NL, 6-seats, and aggregate stats for all found DBs
    for (int i = 0; i < all.length; i++) if (all[i].startsWith("pokerai.org.sample" + Consts.SITE_DEFAULT + "_" + Consts.HOLDEM_NL + "_" + seats) && all[i].endsWith(".hhex")) {
      scan(rootfolder, all[i]);
    }
    // Printing results
    System.out.println("Number of hands parsed: " + (long)totalHands);
    for (byte i = 0; i < cards.length; i++) {
      System.out.println(Consts.printRank(i) + ": " + cards[i]);
    }
  }

  public static void scan(String rootfolder, String fullName) {
    HandManagerNIO hm = new HandManagerNIO();
    hm.init(rootfolder, fullName);
    hm.reset();
    while (hm.hasMoreHands()) {
      PokerHand hand = hm.nextPokerHand();
      if (minPlayers > 0) if (hand.getNumberOfActivePlayers() < minPlayers) continue;
      totalHands++;
      if (totalHands % 10000000 == 0) System.out.println((long)totalHands + " hands read.");
      byte[] boardCards = hand.getCommunityCardsBA();
      if (boardCards != null) {
        if (boardCards[0] != Consts.INVALID_CARD) cards[Consts.getCardRank(boardCards[0])]++;
        if (boardCards[1] != Consts.INVALID_CARD) cards[Consts.getCardRank(boardCards[1])]++;
        if (boardCards[2] != Consts.INVALID_CARD) cards[Consts.getCardRank(boardCards[2])]++;
      }
    }
    hm.closedb();
  }

}

