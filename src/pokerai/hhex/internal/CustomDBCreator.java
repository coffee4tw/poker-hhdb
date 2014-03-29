/*
  This code is released under GPL v3.

  @Author: Indiana (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910
 */
package pokerai.hhex.internal;

import java.io.*;

// Creates single DB (e.g. to store all hands of given player, etc.)
public class CustomDBCreator {

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
  public void appendHand(byte[] b) {
    try {
      if (f != null) {
        int len1 = b.length / 100;
        int len2 = b.length % 100;
        data[pos++] = (byte)len1;
        data[pos++] = (byte)len2;
        System.arraycopy(b, 0, data, pos, b.length);
        pos += b.length;
        if (pos > CHUNK_SIZE - 1000) writeData();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
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

