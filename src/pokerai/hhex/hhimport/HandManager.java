/*
  This code is released under GPL v3.

  @Author: Indiana (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910
 */
package pokerai.hhex.hhimport;

import pokerai.hhex.Consts;

import java.io.File;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;

public class HandManager {

  Hashtable dbs = new Hashtable();
  String root = "";
  String fname = "";

  static long totalTimeVerify1 = 0;
  static long totalTimeVerify2 = 0;
  static long totalTimeAppendHand = 0;

  public void init(String root, String fname, boolean overwrite) {
    // overwrite -> delete all files
    if (overwrite) {
      File f = new File(root); String[] all = f.list();
      if (all == null) {
        System.out.println("ERROR: Supplied directory (" + root + ") seems incorrect!");
        throw new NullPointerException();
      }
      for (int i = 0; i < all.length; i++) if (all[i].startsWith(fname)) { File f2 = new File(root + all[i]); f2.delete(); }
      File f2 = new File(root + Consts.playersIndexFilename); f2.delete();
    }
    this.root = root; this.fname = fname;
  }

  static Calendar c = Calendar.getInstance();
  public void appendHand(RawPokerHandSaver h) {
    long t = System.currentTimeMillis();
    h.verifyHandOnImport1();
    totalTimeVerify1 += (System.currentTimeMillis() - t);
    if (h.buggyhand) return;
    t = System.currentTimeMillis();
    c.setTimeInMillis(((long)h.date) * 1000);
    String findex = h.site + "_" + h.game + "_" + h.getNumberOfSeats() + "_w" + c.get(Calendar.WEEK_OF_YEAR);
    checkFile(findex);
    byte[] b = ((SingleFileHandManager)dbs.get(findex)).appendHand(h);
    totalTimeAppendHand += (System.currentTimeMillis() - t);
    t = System.currentTimeMillis();
    h.verifyHandOnImport2(b);
    totalTimeVerify2 += (System.currentTimeMillis() - t);
  }

  public void closedb() {
    Enumeration e = dbs.elements();
    while (e.hasMoreElements()) ((SingleFileHandManager)e.nextElement()).closedb();
  }

  // total size of all database files
  public long totalSize() {
    Enumeration e = dbs.elements();
    long n = 0; while (e.hasMoreElements()) n += ((SingleFileHandManager)e.nextElement()).size();
    return n;
  }

  // check if the respective DB is already initalized, initalize it if not so
  public void checkFile(String findex) {
    if (dbs.get(findex) == null) {
      SingleFileHandManager sfm = new SingleFileHandManager();
      sfm.init(root, fname + findex + ".hhex", false);
      dbs.put(findex, sfm);
    }
  }

}

