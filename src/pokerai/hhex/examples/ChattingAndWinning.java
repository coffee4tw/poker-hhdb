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

import java.io.RandomAccessFile;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

public class ChattingAndWinning {

  static PlayersDB playersdb = null;
  static int  minHands = 1000;
  static final int MAXPLAYERS = 700000;
  static int maxPlayerID = 0;

  static long[] totalEV = new long[10];
  static double[] players = new double[10];

  public static void main(String[] args) {
    String rootfolder = "C:\\hhdb\\";
    if (args.length > 0) rootfolder = args[0];
    if (!rootfolder.endsWith("\\")) rootfolder += "\\";
    Consts.rootFolder = rootfolder;
    // --- Calculate chatting
    Hashtable chatAmount = new Hashtable();
    Hashtable chath = new Hashtable();
    double n = 0;
    try {
      RandomAccessFile f = new RandomAccessFile(Consts.rootFolder + "chats.log", "rw");
      while (f.getFilePointer() < f.length()) {
        String s = f.readLine();
        int k = s.indexOf(":");
        int playerId = new Integer(s.substring(0, k)).intValue();
        inci(chatAmount, playerId);
        /*
        StringTokenizer st = new StringTokenizer(s.substring(k+1));
        while (st.hasMoreElements()) {
          String phrase = (String)st.nextElement(); 
          inc(chath, phrase);
        }
        */
        inc(chath, s.substring(k+1));
        n++;
        if (n % 1000000 == 0) System.out.println(n);
      }
      f.close();
    } catch (Exception e) {};
    System.out.println("Reading chat completed.");
    Enumeration<String> e1 = chath.keys();
    Enumeration<Integer> e2 = chath.elements();
    while (e1.hasMoreElements()) {
      String s1 = e1.nextElement();
      int times = e2.nextElement();
      if (times/n > 0.002) System.out.println(s1 + " " + ds(times*100/n) + "%");
    }
    //System.out.println(chath);
    System.out.println("Chatting and Winning calculation: ");
    // -- Calculate winnings
    playersdb = new PlayersDB();
    playersdb.init(rootfolder);
    // Scan the complete database
    maxPlayerID = (int)playersdb.getNumberOfPlayers() - 1;

    for (int i = 0; i <= maxPlayerID; i++) {
      PlayerStats player = playersdb.getPlayerInfo(i);
      if (player.hands > minHands) {
        Integer ii = ((Integer)chatAmount.get(i));
        double chats = 0;
        if (ii != null) chats = ii.intValue();
        int index = 0;
        double chatsPer1000h = chats / (player.hands / 1000.0);
        if (chats == 0) index = 0; // no chat
        else if (chatsPer1000h <   5) index = 1; // little chat
        else if (chatsPer1000h <  10) index = 2; // lots of chat
        else if (chatsPer1000h <  20) index = 3;
        else if (chatsPer1000h <  50) index = 4;
        else index = 5;
        totalEV[index] += player.bb100;
        players[index] ++;
      }
    }
    System.out.println(maxPlayerID + " players checked.");
    // Print Axis
    for (int i = 0; i < totalEV.length; i++) {
      System.out.println(i + " " + (int)players[i] + " " + ds(totalEV[i] / players[i]));
    }
    System.out.println();
    playersdb.close();
  }


  public static void inc(Hashtable h, String key) { if (h.get(key) == null) h.put(key, 1); else h.put(key, ((Integer)h.get(key)).intValue()+1); };
  public static void inci(Hashtable h, int key) { if (h.get(key) == null) h.put(key, 1); else h.put(key, ((Integer)h.get(key)).intValue()+1); };
  public static double ds(double x) { return Math.round(x*100)/100.0; }

  public static int nt(Hashtable h, String c1, String c2) {
    int n = 0; String[] ss = {"h", "d", "c", "s"};
    for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) { Integer io = (Integer)(h.get(c1 + ss[i] + c2 + ss[j])); if (io != null) n += io; } return n;
  }
}

