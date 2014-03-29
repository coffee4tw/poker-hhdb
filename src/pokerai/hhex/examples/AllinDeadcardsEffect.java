/*
  This code is released under GPL v3.

  @Author: Indiana (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910

  This example calculates the effect of dead cards (from VPIP-ed players) in preflop all-in situations.
  That is, it displays the expected and actual equity of two all in players for 0,1,2,3,4,5,6 VPIP-ed players

  Only following hand are considered:
    low cards: 22, 32, 33, 42, 43, 44, 52, 53, 54, 55
    high cards: AA, AK, AQ, AJ, KK, KQ, KJ, QQ, QJ, JJ

  See: http://forumserver.twoplustwo.com/showpost.php?p=10470648&postcount=101
 */
package pokerai.hhex.examples;

import pokerai.hhex.Consts;
import pokerai.hhex.PokerHand;
import pokerai.hhex.internal.HandManagerNIO;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.StringTokenizer;

public class AllinDeadcardsEffect {

  static int seats = 6;
  static int totalHands = 0;

  public static void main(String[] args) {
    loadHeadsUpPerc();
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
    dump();
  }

  public static void dump() {
    // Printing results
    System.out.println("Number of hands parsed: " + (long)totalHands);
    System.out.println("Total all-in situations: " + totalAllins + " (Once per " + ds(1/((totalAllins/1.0)/totalHands)) + " hands).");
    System.out.print("VPIP     "); for (int j = 2; j <= seats; j++) System.out.print(j + "           "); System.out.println();
    for (int i = 0; i < handstr.length; i++) {
      System.out.print(handstr[i] + ": ");
      for (int j = 2; j <= seats; j++) {
        double eq1 = equity[j][i] / allins[j][i];
        double eq2 = wins[j][i] / allins[j][i];
        System.out.print(ds(eq1) + "/" + ds(eq2) + "   ");
      }
      System.out.println();
    }
    System.out.println("Aggregated: ");
    //15
    for (int i = 0; i <=15; i++) for (int j = 0; j <= seats; j++)              { awins[j][0] += wins[j][i]; aequity[j][0] += equity[j][i]; aallins[j][0] += allins[j][i]; }
    for (int i = 16; i < handstr.length; i++) for (int j = 0; j <= seats; j++) { awins[j][1] += wins[j][i]; aequity[j][1] += equity[j][i]; aallins[j][1] += allins[j][i]; }
    System.out.print("Big: ");
    for (int j = 2; j <= seats; j++) {
      double eq1 = aequity[j][0] / aallins[j][0];
      double eq2 = awins[j][0] / aallins[j][0];
      System.out.print(ds(eq1) + "/" + ds(eq2) + "   ");
    }
    System.out.println();
    System.out.print("Sma: ");
    for (int j = 2; j <= seats; j++) {
      double eq1 = aequity[j][1] / aallins[j][1];
      double eq2 = awins[j][1] / aallins[j][1];
      System.out.print(ds(eq1) + "/" + ds(eq2) + "   ");
    }

  }

  static int totalAllins = 0;
  static String[] handstr = {"AA", "AK", "AKs", "AQ", "AQs", "AJ", "AJs", "KK", "KQ", "KQs", "KJ", "KJs", "QQ", "QJ", "QJs", "JJ", "22", "32", "32s", "33", "42", "42s", "43", "43s", "44", "52", "52s", "54", "54s", "55"};
  static double[][] wins = new double[seats + 1][handstr.length];
  static double[][] allins = new double[seats + 1][handstr.length];
  static double[][] equity = new double[seats + 1][handstr.length];
  static double[][] awins = new double[seats + 1][2];
  static double[][] aallins = new double[seats + 1][2];
  static double[][] aequity = new double[seats + 1][2];

  public static int getHI(String hand) {
    for (int i = 0; i < handstr.length; i++) if (handstr[i].equals(hand)) return i;
    return -1;
  }

  public static void scan(String rootfolder, String fullName) {
    HandManagerNIO hm = new HandManagerNIO();
    hm.init(rootfolder, fullName);
    hm.reset();
    while (hm.hasMoreHands()) {
      PokerHand hand = hm.nextPokerHand();
      totalHands++;
      if (totalHands % 10000000 == 0) { System.out.println((long)totalHands + " hands read."); dump(); }
      byte[][] cards = new byte[10][];
      boolean[] winner = new boolean[10];
      int n = 0;
      for (byte i = 0; i < hand.getNumberOfSeats(); i++) {
        byte[] h = hand.getCardsOnlyBA(i);
        if (h != null) if (hand.isAllInPreflop(i)) {
          winner[n] = hand.isWinner(i);
          cards[n] = h;
          n++;
        }
      }
      if (n == 2 && winner[0] != winner[1]) {
        String hs1 = Consts.printHand(cards[0]);
        int h1 = getHI(hs1);
        if (h1 < 0) continue;
        String hs2 = Consts.printHand(cards[1]);
        int h2 = getHI(hs2);
        if (h2 < 0) continue;
        // We have two players allin preflop, and one of them won, i.e. they have not split pot etc.
        double eq = headsUp(cards[0], cards[1]);
        int vpIPed = hand.getVPIPed();
        //System.out.println(Consts.printHand(cards[0]) + " vs " +  Consts.printHand(cards[1]) + ": " + equity);
        totalAllins++;
        if (winner[0]) {
          wins[vpIPed][h1]++;
          allins[vpIPed][h1]++;
          allins[vpIPed][h2]++;
          equity[vpIPed][h1] += eq;
          equity[vpIPed][h2] += (1 - eq);
        } else {
          wins[vpIPed][h2]++;
          allins[vpIPed][h1]++;
          allins[vpIPed][h2]++;
          equity[vpIPed][h1] += eq;
          equity[vpIPed][h2] += (1 - eq);
        }
      }
    }
    hm.closedb();
  }

  public static String ds(double x) {
    String s = Math.round(x*100)/100.0 + "";
    int k = s.indexOf(".");
    if (k >=0 ) if (s.length()-k < 3) s += "0";
    return s;
  }

  public static double ds3(double x) {
    return Math.round(x*1000)/1000.0;
  }

  /// *** HEADS UP STUFF ***************************************
  /// *** 2 way headsup is here
  public static double[][] headsUpPerc = null;
  /// *** 2 way headsup is here
  public static double headsUp(byte[] cards1, byte[] cards2) { return headsUp(Consts.printHand(cards1), Consts.printHand(cards2)); }
  public static double headsUp(String hand1, String hand2) { return headsUpPerc[getHandIndex(hand1)][getHandIndex(hand2)]; }

  public static void loadHeadsUpPerc() {
    headsUpPerc = new double[169][169];
    try {
      RandomAccessFile f = new RandomAccessFile("math\\headsupExt.txt", "r");
      while (f.getFilePointer() < f.length()) {
        String s = f.readLine();
        if (s.equals("")) break;
        StringTokenizer st = new StringTokenizer(s);
        String h1 = (String) st.nextElement();
        String h2 = (String) st.nextElement();
        double wp = new Double((String) st.nextElement()).doubleValue();
        //System.out.println(h1 + " " + h2 + " " + wp);
        int i1 = getHandIndex(h1);
        int i2 = getHandIndex(h2);
        headsUpPerc[i1][i2] = wp;
        headsUpPerc[i2][i1] = 1 - wp;
        headsUpPerc[i1][i1] = 0.5; headsUpPerc[i2][i2] = 0.5;
      }
      f.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Must not be modified - and currently aren't
  public static String[] hands = {"AA", "KK", "QQ", "JJ", "AKs", "TT", "AK", "AQs", "99", "88", "AJs", "AQ", "77", "ATs", "66",
                                  "AJ", "KQs", "55", "A9s", "AT", "A8s", "44", "KJs", "A7s", "A5s", "33", "KTs", "A6s", "A9",
                                  "KQ", "A4s", "A3s", "QJs", "22", "A2s", "A8", "K9s", "QTs", "KJ", "A7", "A5", "JTs", "KT",
                                  "K8s", "A4", "A6", "K7s", "Q9s", "A3", "K6s", "T9s", "J9s", "QJ", "A2", "K5s", "K9", "K4s",
                                  "Q8s", "98s", "QT", "T8s", "J8s", "K3s", "87s", "JT", "K2s", "97s", "76s", "Q7s", "K8", "Q6s",
                                  "T7s", "86s", "J7s", "65s", "K7", "Q5s", "Q9", "K6", "54s", "96s", "Q4s", "T9", "J9", "75s",
                                  "K5", "T6s", "Q3s", "J6s", "85s", "64s", "J5s", "Q8", "K4", "Q2s", "98", "95s", "J4s", "T8",
                                  "53s", "74s", "J8", "87", "K3", "T5s", "J3s", "43s", "T4s", "K2", "84s", "97", "76", "J2s",
                                  "63s", "Q7", "Q6", "T3s", "94s", "T7", "65", "86", "52s", "J7", "93s", "Q5", "T2s", "73s",
                                  "54", "96", "42s", "92s", "75", "Q4", "83s", "62s", "82s", "T6", "32s", "85", "Q3", "J6",
                                  "64", "J5", "72s", "Q2", "95", "J4", "53", "74", "T5", "J3", "43", "T4", "84", "63", "J2",
                                  "94", "T3", "52", "93", "73", "T2", "42", "92", "83", "62", "82", "32", "72"};

  //public static int[][] handsi = new int[169][];
  public static char getCardIndex(char a) {
    if (a == 'A') return 14;  if (a == 'K') return 13;
    if (a == 'Q') return 12;  if (a == 'J') return 11; if (a == 'T') return 10;
    return (char)(a - '0');
  }
  private static int[] index = new int[500];
  private static int _getHandIndex(String hand) {
    int modif = 0;
    if (hand.length() > 2) modif = 225;
    modif += 15*getCardIndex(hand.charAt(0));
    modif += getCardIndex(hand.charAt(1));
    return modif;
  }

  public static int getHandIndex(String hand) {
    return index[_getHandIndex(hand)];
  }

  static {
    for (int i = 0; i<hands.length; i++) {
      index[_getHandIndex(hands[i])] = i;
      if (hands[i].length() <= 2) index[_getHandIndex(""+hands[i].charAt(1)+hands[i].charAt(0))] = i;
        else index[_getHandIndex(""+hands[i].charAt(1)+hands[i].charAt(0)+"s")] = i;
    }
    ///for (int i = 0; i<hands.length; i++) {
    //  handsi[i] = Consts.getCardsFromAbr_fast(hands[i]);
    //}
    // init int hands
  }
}

