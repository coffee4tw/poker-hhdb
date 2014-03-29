/*
  This code is released under GPL v3.

  @Author: Indiana (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910
 */
package pokerai.hhex.examples;

import pokerai.hhex.Consts;
import pokerai.hhex.PlayerHandProcessor;
import pokerai.hhex.PokerHand;
import pokerai.hhex.helper.GeneralHelper;
import pokerai.hhex.helper.PlayerStats;
import pokerai.hhex.helper.PlayersDB;
import pokerai.hhex.internal.HandManagerNIO;

import java.io.File;
import java.util.Hashtable;

public class PlayerStatistics {

  static boolean doscan = false;
  static PlayersDB playersdb = null;

  // TODO: Align thse both ranges, as having two different ranges is confusing
  static long[] ranges = {1, 1000, 10000, 50000, 100000, 200000, 500000, 1000000, 999999999};
  static long[] wranges = {1, 100, 500, 1000, 5000, 10000, 50000, 100000, 500000};

  public static void main(String[] args) {
    String rootfolder = "C:\\hhdb\\";
    if (args.length > 0) rootfolder = args[0];
    if (!rootfolder.endsWith("\\")) rootfolder += "\\";
    Consts.rootFolder = rootfolder;
    // ---
    playersdb = new PlayersDB();
    playersdb.init(rootfolder);
    // Scan the complete database
    if (doscan) {
      System.out.println("Reading hands ...");
      File dir = new File(rootfolder);
      String[] all = dir.list();
      for (int i = 0; i < all.length; i++) if (all[i].startsWith("pokerai.org.sample0_1_9") && all[i].endsWith(".hhex"))  {
      //for (int i = 0; i < all.length; i++) if (all[i].endsWith(".hhex")) {
        scan(rootfolder, all[i]);
      }
      System.out.println("Calculating ranges ...");
      // Summarizes results
      calculateRanges();
      // Compared to saved players index
      System.out.println("Comparing to PlayerIndex for correctness ...");
      if (maxPlayerID != (playersdb.getNumberOfPlayers() - 1)) {
        System.out.println("ERROR, different number of players, n1 = " + maxPlayerID + ", n2 = " + playersdb.getNumberOfPlayers());
        return;
      }
    } else {
      maxPlayerID = (int)playersdb.getNumberOfPlayers() - 1;
      calculateRanges2();
    }
    // Print resutls
    for (int i = 0; i < ranges.length-1; i++) {
      System.out.println("Number of players that played " + ranges[i] + " - " + ranges[i+1] + " hands: " + npr[i]);
    }
    int[] winners = new int[wranges.length];
    int[] losers = new int[wranges.length];
    int[] breakev = new int[wranges.length];
    double totalWinnings = 0, totalLosings = 0, winningsTopPlayers = 0;
    double bestev = 0, mosthands = 0;
    int bestPlayerId = 0, mostHandsId = 0;
    for (int i = 0; i <= maxPlayerID; i++) {
      PlayerStats player = playersdb.getPlayerInfo(i);
      //System.out.println("ERROR, Player " + i + ", h1 = " + handsPerPlayer[i] + ", h2 = " + player.hands + ", winnings = " + ds(player.getTotalWinnings()) + ", (" + ds(player.bb100) + " bb/100h)");
      if (doscan) {
        if (handsPerPlayer[i] != player.hands) {
          System.out.println("ERROR, Player " + i + ", h1 = " + handsPerPlayer[i] + ", h2 = " + player.hands + ", winnings = " + ds(player.getTotalWinnings()) + ", (" + ds(player.bb100) + " bb/100h)");
        }
      }
      for (int j = 0; j < wranges.length; j++) if (player.hands > wranges[j]) {
        if (player.bb100 > 0) {
          winners[j]++;
          //totalWinnings += player.totalWinnings;
          //if (totalWinnings > Long.MAX_VALUE /2) System.out.println("warning");
        } else if (player.bb100 < 0) {
          //totalLosings += player.totalWinnings;
          losers[j]++;
        } else {
          breakev[j]++;
        }
      }
      if (player.bb100 > bestev && player.hands > 10000) { bestev = player.bb100; bestPlayerId = i; } // save who is the player with the best EV
      if (player.hands > mosthands) { mosthands = player.hands; mostHandsId = i; } // save who is the player with most hands 
    }

    // Dump overal player statistics
    totalWinnings = totalWinnings / 100;
    totalLosings = totalLosings / 100;
    winningsTopPlayers = winningsTopPlayers / 100;
    System.out.print("Number of  winners  per hand range: "); for (int j = 0; j < wranges.length; j++) System.out.print(winners[j] + " "); System.out.println();
    System.out.print("Number of   losers  per hand range: "); for (int j = 0; j < wranges.length; j++) System.out.print(losers[j]+ " "); System.out.println();
    System.out.print("Number of breakeven per hand range: "); for (int j = 0; j < wranges.length; j++) System.out.print(breakev[j]+ " "); System.out.println();
    System.out.print("Total players per hand range: "); for (int j = 0; j < wranges.length; j++) System.out.print((winners[j]+losers[j]) + " "); System.out.println();
    System.out.print("Percentage winnners per hand range: "); for (int j = 0; j < wranges.length; j++) System.out.print(ds((winners[j]*100.0)/(winners[j]+losers[j])) + "% "); System.out.println();
    playersdb.close();

    // Dump overal player statistics
    System.out.println();
    System.out.println("Building and dumping information for the best player with over 50k hands, and the player with most hands:");
    System.out.println("Best player EV: " + GeneralHelper.ds(bestev) + " bb/100");
    BestPlayerProcessor bp = new BestPlayerProcessor();
    bp.setRootFolder(Consts.rootFolder);
    bp.setPlayerIds(new int[]{bestPlayerId, mostHandsId});
    bp.pid1 = bestPlayerId;
    bp.pid2 = mostHandsId;
    bp.process();
    System.out.println();
  }

  static final int MAXPLAYERS = 700000;
  static int[] handsPerPlayer = new int[MAXPLAYERS];
  static int maxPlayerID = 0;
  static long totalHands = 0;

  public static void scan(String rootfolder, String fileName) {
    //System.out.println(fileName);
    long time = System.currentTimeMillis();
    HandManagerNIO hm = new HandManagerNIO();
    long size = hm.init(rootfolder, fileName);
    hm.reset();
    while (hm.hasMoreHands()) {
      PokerHand hand = hm.nextPokerHand();
      totalHands++;
      if (totalHands % 10000000 == 0) System.out.println(totalHands + " hands scanned.");
      if (hand.getStacks() == null) continue;
      //System.out.println(h.gameId);
      for (int i = 0; i<hand.getStacks().length; i++) if (hand.getStacks()[i] > 0) {
        int playerID = hand.getPlayerIDs()[i];
        handsPerPlayer[playerID]++;
        if (playerID > maxPlayerID) maxPlayerID = playerID;
      }
    }
    hm.closedb();
  }

  static long[] npr = new long[ranges.length];
  public static void calculateRanges() {
    for (int i = 0; i < maxPlayerID; i++) {
      for (int j = 0; j < ranges.length - 1; j++) if (handsPerPlayer[i] >= ranges[j] && handsPerPlayer[i] < ranges[j+1]) {
        npr[j]++;
        break;
      }
    }
  }

  public static void calculateRanges2() {
    for (int i = 0; i < maxPlayerID; i++) {
      PlayerStats p = playersdb.getPlayerInfo(i);
      for (int j = 0; j < ranges.length - 1; j++) if (p.hands >= ranges[j] && p.hands < ranges[j+1]) {
        npr[j]++;
        break;
      }
    }
  }

  public static void inc(Hashtable h, String key) { if (h.get(key) == null) h.put(key, 1); else h.put(key, ((Integer)h.get(key)).intValue()+1); };
  public static double ds(double x) { return Math.round(x*100)/100.0; }

  public static int nt(Hashtable h, String c1, String c2) {
    int n = 0; String[] ss = {"h", "d", "c", "s"};
    for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) { Integer io = (Integer)(h.get(c1 + ss[i] + c2 + ss[j])); if (io != null) n += io; } return n;
  }
}

class BestPlayerProcessor extends PlayerHandProcessor {
  int[][][][] hands = new int[2][Consts.sites.length][Consts.game.length][Consts.BLINDS.length];
  public int pid1 = 0;
  public int pid2 = 0;

  // called for each new hand that fullfills the given criterias
  protected void processHand(PokerHand hand, int playerId) {
    //System.out.println("here");
    if (playerId == pid1) hands[0][hand.getSite()][hand.getGameType()][hand.getLimitsIndex()]++;
    if (playerId == pid2) hands[1][hand.getSite()][hand.getGameType()][hand.getLimitsIndex()]++;
  }

  // called when parsing is complete
  protected void printResults() {
    for (int pid = 0; pid < hands.length; pid++) {
      if (pid == 0) System.out.println("Hands played (best player with id " + pid1 + "): ");
      if (pid == 1) System.out.println("Hands played (player with most hands, id " + pid2 + "): ");
      for (int s = 0; s < Consts.sites.length; s++)
       for (int g = 0; g < Consts.game.length; g++)
        for (int b = 0; b < Consts.BLINDS.length; b++)
         if (hands[pid][s][g][b] > 0) {
           System.out.println("  " + Consts.sites[s] + ", " + Consts.game[g] + " " +
                   GeneralHelper.ds(Consts.BLINDS[b][0]/100.0) + "$/" + GeneralHelper.ds(Consts.BLINDS[b][1]/100.0) + "$: " +
                   hands[pid][s][g][b] + " hands.");
         }
    }
  }

}