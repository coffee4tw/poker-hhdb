/*
  This code is released under GPL v3.

  @Author: Indiana (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910
 */
package pokerai.hhex.helper;

import pokerai.hhex.Action;
import pokerai.hhex.Consts;
import pokerai.hhex.PokerHand;
import pokerai.hhex.internal.HandManagerNIO;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Enumeration;
import java.util.Hashtable;

public class PlayersDB {
  // TODO: Use NIO here, or in-memory operations, and the dump all at once

  String rootfolder = "C:\\hhdb\\";
  RandomAccessFile f = null;

  // Used for building playerIndex
  Hashtable playerInfos = new Hashtable();

  public void init(String root) {
    rootfolder = root;
    try {
      boolean toinit = (!(new File(root + "pokerai.org.players.index").exists()));
      f = new RandomAccessFile(root + "pokerai.org.players.index", "rw");
      this.rootfolder = root;
      if (toinit) initialize();
    } catch (Exception e) { e.printStackTrace(); }
  }

  public void close() { try { f.close(); } catch (Exception e) { e.printStackTrace(); }}
  public long getNumberOfPlayers() { try { return (f.length() / PlayerStats.REC_LENGTH); } catch (Exception e) { e.printStackTrace(); } return 0; }

  public void savePlayerInfo(int playerId, PlayerStats pinfo) {
    try {
      byte[] data = pinfo.save();
      if (data.length != PlayerStats.REC_LENGTH) {
        System.out.println("Players REC_LENGTH is different that data.length (" + data.length + ")");
        System.exit(1);
      }
      f.seek(playerId * PlayerStats.REC_LENGTH);
      f.write(data);
    } catch (Exception e) { e.printStackTrace(); }
  }

  // doesn't work for some reason!
  public void savePlayerIfDoesntExist(int playerId, PlayerStats pl) {
    try {
      if (f.length() <= playerId * PlayerStats.REC_LENGTH) savePlayerInfo(playerId, pl);
    } catch (Exception e) { e.printStackTrace(); }
  }

  public PlayerStats getPlayerInfo(int playerId) {
    PlayerStats pl = new PlayerStats();
    //savePlayerIfDoesntExist(playerId, pl);
    try {
      f.seek(playerId * PlayerStats.REC_LENGTH);
      byte[] data = new byte[PlayerStats.REC_LENGTH];
      f.read(data);
      pl.load(playerId, data);
    } catch (Exception e) { e.printStackTrace(); }
    return pl;
  }

  /*
   -------  Add player information  -------
   */

  long totalHands = 0;
  private void initialize() {
    System.out.println("Initializing players index (this is done only the first time, then persisted), please wait ...");
    long time = System.currentTimeMillis();
    File dir = new File(rootfolder);
    String[] all = dir.list();
    // TODO: Enable Omaha and Limit after all bugs with money are fixed
    String use1 = "pokerai.org.sample" + Consts.SITE_PS + "_" + Consts.HOLDEM_NL;
    String use2 = "pokerai.org.sample" + Consts.SITE_PS + "_" + Consts.HOLDEM_PL;

    for (int i = 0; i < all.length; i++)
//      if (all[i].startsWith(use1) || all[i].startsWith(use2))
        if (all[i].endsWith(".hhex")) {
          scan(rootfolder, all[i]);
        }
    flushAll();
    double time2 = (System.currentTimeMillis() - time) / 60000.0;
    System.out.println("A total of " + totalHands + " hands has been indexed, for " + GeneralHelper.ds(time2) + " min.");
  }

  private void scan(String rootfolder, String fullName) {
    HandManagerNIO hm = new HandManagerNIO();
    hm.init(rootfolder, fullName);
    hm.reset();
    while (hm.hasMoreHands()) {
      PokerHand hand = hm.nextPokerHand();
      try {
        addPlayerStats(hand);
      } catch (Exception e) {
        System.out.println("ERROR: Uncaught exception while building player index!");
        e.printStackTrace();
      }
    }
    hm.closedb();
  }

  private PlayerStats getPlayerInfoInMemory(int playerId) {
    PlayerStats p = (PlayerStats) playerInfos.get(playerId);
    if (p == null) {
      p = getPlayerInfo(playerId);
      playerInfos.put(playerId, p);
    }
    return p;
  }

  private void flushAll() {
    Enumeration keys = playerInfos.keys();
    Enumeration elem = playerInfos.elements();
    while (keys.hasMoreElements()) {
      savePlayerInfo(((Integer)keys.nextElement()).intValue(), (PlayerStats)elem.nextElement());
    }
  }

  private void addPlayerStats(PokerHand h) {
    int[] playerIds = h.getPlayerIDs();
    int[] stacks = h.getStacks();
    HandHelper hh = new HandHelper();
    totalHands++;
    if (totalHands % 10000000 == 0) System.out.println(totalHands + " hands indexed.");
    for (byte seat = 0; seat < h.getNumberOfSeats(); seat++) if (stacks[seat] > 0) {
      //PlayerStats p = getPlayerInfo(playerIds[seat]);  // this is used for direct disk read/write
      PlayerStats p = getPlayerInfoInMemory(playerIds[seat]);   // this is used for in memory parsing
      p.hands++;
      if (stacks[seat] < 3000) p.intfiled1++;
      hh.setHand(h, playerIds[seat]);
      Action a = hh.getFirstActionPreflop();
      if (a != null) {
        if ((a.getAction() == Consts.ACTION_RAISE || a.getAction() == Consts.ACTION_BET) && a.getAmount() > 1000) p.intfiled2++;
      }
      p.totalWinnings += h.getMoneyMade(seat);
      //savePlayerInfo(playerIds[seat], p);   // this is used for direct disk read/write
    }
  }

}

