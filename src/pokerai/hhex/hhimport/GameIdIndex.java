/*
  This code is released under GPL v3.

  @Author: Indiana (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910
 */
package pokerai.hhex.hhimport;

import pokerai.hhex.Consts;

import java.io.*;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

// This helps to avoid importing duplicated hands
public class GameIdIndex {

  // configurable
  private static final int dayslice = 1;
  //private static final long MAX_MEMORY = 1024;  // Max memory to be used in MB

  // internal
  private static boolean initdone = false;
  //private static final int daym = 60*60*24 / dayslice;
  private static final int daym = 60*60*24 / dayslice;
  //private static final long MAX_IDS = (long)((1048576*MAX_MEMORY) / (2*Integer.SIZE));
  //private static final long MAX_IDS = 10000000;
  private static final int maxint = Integer.MAX_VALUE;
  private static Hashtable days = new Hashtable();
  private static Hashtable daysLastAccessTime = new Hashtable();

  // Time statistics
  static long totalTimeLoading = 0;
  static long totalTimeSaving = 0;
  static long totalTime = 0;

  static long loadedElements = 0;
  static long counter = 0;
  static int lastLoaded = -1;

  // gameId is long, date is in seconds (System.currentTimeMillis % 1000)
  // Call this method in HandParser_SiteName after both gameId and date has been parsed
  public static boolean isHandAlreadyImported(long lgameId, int date) {
    counter++;
    long t = System.currentTimeMillis();
    if (!initdone) {
      initialize();
    }
    int gameId = (int)(lgameId % maxint);
    int day = date / daym;
    HashSet ids = (HashSet) days.get(day);
    // -----
    while (ids == null && (/*loadedElements > MAX_IDS ||*/ days.size() > 3)) {
      Enumeration e = daysLastAccessTime.keys();
      long oldest = Long.MAX_VALUE;
      int oldestday = 0;
      while (e.hasMoreElements()) {
        int dd = ((Integer)e.nextElement()).intValue();
        if (dd == day) continue; // don't offload what we look for
        long lat = ((Long)daysLastAccessTime.get(dd)).longValue();
        if (lat < oldest) { oldest = lat; oldestday = dd; }
      }
      if (oldestday == 0) {
        System.out.println("ERROR: oldest day is zero!");
        System.out.println(oldest);
        e = daysLastAccessTime.keys();
        while (e.hasMoreElements()) {
          int dd = ((Integer)e.nextElement()).intValue();
          long lat = ((Long)daysLastAccessTime.get(dd)).longValue();
          System.out.println("  " + dd + " " + lat);
        }
        return false;
      }
      HashSet h = (HashSet)days.get(oldestday);
      saveToOOS(oldestday, h);
      loadedElements -= h.size();
        //System.out.println(days.size() + " " + h.size());
      h = null;
      days.remove(oldestday);
      daysLastAccessTime.remove(oldestday);
      //System.gc();
    }
    if (ids == null) {
      // day not loaded
      if (lastLoaded == day) {
        System.out.println("ERROR: Attempt to load twice the same day!");
        flush();
      }
      ids = loadFromOOS(day);
      loadedElements += ids.size();
      lastLoaded = day;
      days.put(day, ids);
      daysLastAccessTime.put(day, counter);
    }
    daysLastAccessTime.put(day, counter);
    boolean alreadyIn = ids.contains(gameId);
    totalTime += (System.currentTimeMillis() - t);
    if (alreadyIn) return true;
    ids.add(gameId);
    loadedElements++;
    //System.out.println(ids.size());
    return false;
  }

  public static void flush() {
    Enumeration e = days.keys();
    while (e.hasMoreElements()) {
      int key = ((Integer)e.nextElement()).intValue();
      HashSet h = (HashSet)days.get(key);
      saveToOOS(key, h);
    }
    days.clear();
    daysLastAccessTime.clear();
    System.gc();
  }

  private static void initialize() {
    initdone = true;
    // System.out.println("TRACE: Max IDs: " + MAX_IDS);
    File tempdir = new File(Consts.rootFolder + "temp");
    if (!tempdir.exists()) tempdir.mkdir();
  }

  // Loading from object stream format
   public static HashSet loadFromOOS(int day) {
     long time = System.currentTimeMillis();
     try {
       File f = new File(Consts.rootFolder + Consts.gameidIndexFilenames + day + ".oos");
       if (!f.exists()) { return new HashSet(); }
       FileInputStream f1 = new FileInputStream(f);
       DataInputStream di = new DataInputStream(f1);
       HashSet h = new HashSet();
       while (di.available() > 0) {
         h.add(di.readInt());
       }
       //ObjectInputStream ois = new ObjectInputStream(f1);
       //HashSet h = (HashSet) ois.readObject();
       //ois.close();
       di.close();
       f1.close();
       totalTimeLoading += (System.currentTimeMillis() - time);
       return h;
     } catch (Exception e) {
       e.printStackTrace();
     }
     totalTimeLoading += (System.currentTimeMillis() - time);
     return new HashSet();
   }

  public static void saveToOOS(int day, HashSet h) {
    long time = System.currentTimeMillis();
    File f = new File(Consts.rootFolder + Consts.gameidIndexFilenames + day + ".oos");
    f.delete();
    try {
      FileOutputStream f1 = new FileOutputStream(f);
      BufferedOutputStream bf = new BufferedOutputStream(f1);
      DataOutputStream di = new DataOutputStream(bf);
      Iterator<Integer> s = h.iterator();
      while (s.hasNext()) di.writeInt(s.next());
      //ObjectOutputStream oos = new ObjectOutputStream(f1);
      //oos.writeObject(h);
      //oos.close();
      bf.close();
      di.close();
      f1.close();
    } catch (Exception e) {
       e.printStackTrace();
    }
    totalTimeSaving += (System.currentTimeMillis() - time);
  }
}