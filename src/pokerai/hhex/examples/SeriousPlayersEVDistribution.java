/*
  This code is released under GPL v3.

  @Author: Indiana (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910

  This example finds out the distribution of EV for playes that played over 100,000 (or specified amount X) hands.

 */
package pokerai.hhex.examples;

import pokerai.hhex.Consts;
import pokerai.hhex.helper.PlayerStats;
import pokerai.hhex.helper.PlayersDB;
import java.util.Hashtable;

public class SeriousPlayersEVDistribution {

  static PlayersDB playersdb = null;
  static int  minHands = 100000;
  static final int MAXPLAYERS = 700000;
  static int maxPlayerID = 0;

  static int[] pranges = new int[60];
  static int[] nranges = new int[60];

  public static void main(String[] args) {
    String rootfolder = "C:\\hhdb\\";
    if (args.length > 0) rootfolder = args[0];
    if (!rootfolder.endsWith("\\")) rootfolder += "\\";
    Consts.rootFolder = rootfolder;
    // ---
    playersdb = new PlayersDB();
    playersdb.init(rootfolder);
    // Scan the complete database
    maxPlayerID = (int)playersdb.getNumberOfPlayers() - 1;

    for (int i = 0; i <= maxPlayerID; i++) {
      PlayerStats player = playersdb.getPlayerInfo(i);
      if (player.hands > minHands) {
        //System.out.println("Player " + i + ", hands = " + player.hands + ", winnings = " + ds(player.getTotalWinnings()) + ", (" + ds(player.bb100) + " bb/100h)");
        int rbb = (int)(player.bb100*2);
        if (player.bb100 >= 0) {
          if (rbb >= pranges.length) pranges[pranges.length-1]++; else pranges[rbb]++;
        } else {
          if (-rbb >= nranges.length) nranges[nranges.length-1]++; else nranges[-rbb]++;
        }
      }
    }
    System.out.println(maxPlayerID + " players checked.");
    // Print Axis
    for (int i = nranges.length-1; i >= 0; i--) System.out.print(-i/2.0 + " "); //System.out.println("|");
    for (int i = 0; i < pranges.length; i++) System.out.print(i/2.0 + " ");
    System.out.println();
    for (int i = nranges.length-1; i >= 0; i--) System.out.print(nranges[i] + " "); //System.out.println("|");
    for (int i = 0; i < pranges.length; i++) System.out.print(pranges[i] + " ");
    System.out.println();
    playersdb.close();
  }


  public static void inc(Hashtable h, String key) { if (h.get(key) == null) h.put(key, 1); else h.put(key, ((Integer)h.get(key)).intValue()+1); };
  public static double ds(double x) { return Math.round(x*100)/100.0; }

  public static int nt(Hashtable h, String c1, String c2) {
    int n = 0; String[] ss = {"h", "d", "c", "s"};
    for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) { Integer io = (Integer)(h.get(c1 + ss[i] + c2 + ss[j])); if (io != null) n += io; } return n;
  }
}

