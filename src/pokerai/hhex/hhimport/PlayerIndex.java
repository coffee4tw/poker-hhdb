/*
  This code is released under GPL v3.

  @Author: Indiana (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910

  This code builds and maintains the mapping from PlayerID -> Real Player Names
 */
package pokerai.hhex.hhimport;

import pokerai.hhex.Consts;
import pokerai.hhex.Logger;
import pokerai.hhex.internal.RawPokerHand;

import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

public class PlayerIndex {

  static int n = 0;
  static Hashtable playerId = null;
  static Hashtable playerName = null;
  static Hashtable err1 = new Hashtable();
  static Hashtable fastHash = new Hashtable();  // small temporary hash table for very fast access and caching. Max 1000 elements. Useful, as most parsing is consequtive over the same table.
  static public long totalTimeGetPlayerId = 0;  // used in HandImporter

  public static int getPlayerId(String name, String line, RawPokerHand hand) {
    long time = System.currentTimeMillis();
    Integer temp = (Integer)fastHash.get(name);
    if (temp != null) {
      totalTimeGetPlayerId += (System.currentTimeMillis()-time);
      return temp.intValue();
    }
    if (playerId == null) load();
    //String nname = new String(name.getBytes()).trim();
    String nname = name.trim();
    if (nname.length() == name.length()) nname = new String(name);
    // check names for correctness, remove ":" at the end if needed
    if (playerId.get(nname + ":") != null) if (err1.get(nname) == null) {
      Logger.writeToErrors("ERROR, column at the end of name parsed wrongly " + nname + ", line = <" + line + ">", hand);
    }
    if (nname.endsWith(":")) {
      String name2 = nname.substring(0, nname.length()-1);
      if (playerId.get(name2) != null) if (err1.get(name2) == null) {
        Logger.writeToErrors("ERROR, column at the end of name parsed wrongly " + nname + ", line = <" + line + ">", hand);
        err1.put(name2, "");
      }
      nname = name2;
    }
    if (nname.length() > Consts.MAX_PLAYERNAME_LENGTH) nname = nname.substring(0, Consts.MAX_PLAYERNAME_LENGTH);
    // if (nname.indexOf("($") >= 0) Logger.writeToErrors("ERROR, wrong player name <" + nname + ">, line = <" + line + ">", hand);
    // add player name and return next unique Id
    Integer l = (Integer)playerId.get(nname);
    if (l == null) {
      n++;
      playerId.put(nname, n);
      playerName.put(n, nname);
      if (fastHash.size() > 1000) fastHash.clear();
      fastHash.put(name, n);
      totalTimeGetPlayerId += (System.currentTimeMillis()-time);
      return n;
    }
    totalTimeGetPlayerId += (System.currentTimeMillis()-time);
    return l.intValue();
  }

  // not used in any public versions
  public static String getPlayerName(int id) {
    if (playerId == null) load();
    Object o = playerName.get(id);
    if (o == null) return "pid" + new Integer(id).toString();
    return (String)o;
  }

  // Loadnig from plain text format
   public static void load() {
      n = 0;
      try {
         RandomAccessFile f = new RandomAccessFile(Consts.rootFolder + Consts.playersIndexFilename, "rw");
         if (f.getFilePointer() == f.length()) {
            playerId = new Hashtable();
            playerName = new Hashtable();
            return; // nothing to load
         }
         byte[] data = new byte[(int) f.length()];
         f.readFully(data);
         StringTokenizer st = new StringTokenizer(new String(data),
               PlayerIndex.enter);
         n = Integer.parseInt(st.nextToken());
         playerId = new Hashtable((int) (n / 0.7)); // create Hashtables with appropriate sizes
         playerName = new Hashtable((int) (n / 0.7));
         while (st.hasMoreElements()) {
            String s = st.nextToken();
            int k = s.indexOf(" ");
            String id = s.substring(0, k);
            String name = s.substring(k + 1, s.length());
            playerId.put(name, new Integer(id));
            playerName.put(new Integer(id), name);
         }

         f.close();
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

  // Saving to plain text and object stream formats
  public static String enter = new String(new byte[]{13, 10});

  public static void save() {
    if (playerId == null) return;
    try {
      File fl = new File(Consts.rootFolder + Consts.playersIndexFilename);
      fl.delete();
      RandomAccessFile f = new RandomAccessFile(Consts.rootFolder + Consts.playersIndexFilename, "rw");
      f.writeBytes(n + enter);
      Enumeration keys = playerId.keys();
      Enumeration elem = playerId.elements();
      while (keys.hasMoreElements()) {
        f.write((elem.nextElement() + " " + keys.nextElement() + enter).getBytes());
      }
      f.close();
      saveToOOS();
    } catch (Exception e) {
       e.printStackTrace();
    }
  }

  // Loading from object stream format 
   public static void loadFromOOS() {
     try {
       ObjectInputStream ois = new ObjectInputStream(new FileInputStream(Consts.rootFolder + Consts.playersIndexFilename + ".oos"));
       playerId = (Hashtable) ois.readObject();
       playerName = (Hashtable) ois.readObject();
       ois.close();
       n = playerId.size();
     } catch (Exception e) {
       e.printStackTrace();
     }
   }

  public static void saveToOOS() {
    File f = new File(Consts.rootFolder + Consts.playersIndexFilename + ".oos");
    f.delete();
    try {
      ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f));
      oos.writeObject(playerId);
      oos.writeObject(playerName);
      oos.close();
    } catch (Exception e) {
       e.printStackTrace();
    }
  }

}