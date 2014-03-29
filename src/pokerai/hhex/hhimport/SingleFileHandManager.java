/*
  This code is released under GPL v3.

  @Author: Indiana (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910
 */
package pokerai.hhex.hhimport;

import pokerai.hhex.Consts;
import pokerai.hhex.PokerHand;
import pokerai.hhex.internal.RawPokerHand;

import java.io.File;
import java.io.RandomAccessFile;

// ******************************************************
// Manages single file
//
public class SingleFileHandManager {

  RandomAccessFile f = null;
  String root = "";
  String fname = "";

  int CHUNK_SIZE = 65535;
  byte[] data = new byte[CHUNK_SIZE];
  int pos = 0;

  public void init(String root, String fname, boolean overwrite) {
    if (overwrite) {
      File fi = new File(root + fname);
      fi.delete();
    }
    //System.out.println("initializing " + root + fname);
    try {
      f = new RandomAccessFile(root + fname, "rw");
      f.seek(f.length());
      this.root = root;
      this.fname = fname;
      pos = 0;
    } catch (Exception e) { e.printStackTrace(); }
  }

  // must to be called
  public void closedb() {
    try {
      writeData();
      if (f != null) f.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public long size() {
    try { if (f != null) return f.length(); } catch (Exception e) { e.printStackTrace(); }
    return 0;
  }

  // never called with buggy hand
  public byte[] appendHand(RawPokerHandSaver h) {
    try {
      if (f != null) {
        //f.seek(f.length());
        byte[] b = h.save();
        int len1 = b.length / 100;
        int len2 = b.length % 100;
        //System.out.println("TRACE: One hand is being saved, bytes " + b.length + ", " + len);
        //f.writeByte((byte)len1);
        //f.writeByte((byte)len2);
        //f.write(b);
        data[pos++] = (byte)len1;
        data[pos++] = (byte)len2;
        System.arraycopy(b, 0, data, pos, b.length);
        pos += b.length;
        if (pos > CHUNK_SIZE - 1000) writeData();
        // verification for errors
        if (Consts.loadImportedHandsAfterSave) {
          try {
            PokerHand h2 = RawPokerHand.load(b);
            h2.load();
          } catch (Exception e) {
            System.out.println("ERROR: Error during verification of imported hand!");
            e.printStackTrace();
          }
        }
        return b;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public void writeData() {
    try {
      f.write(data, 0, pos);
      pos = 0;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}

