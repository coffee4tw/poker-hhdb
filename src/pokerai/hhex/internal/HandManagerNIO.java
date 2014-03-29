/*
  This code is released under GPL v3.

  @Author: Indiana (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910
 */
package pokerai.hhex.internal;

import pokerai.hhex.PokerHand;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class HandManagerNIO {

  //RandomAccessFile f = null;
  FileChannel fc = null;
  MappedByteBuffer byteBuffer = null;
  int size = 0;
  int pointer = 0;
  int readpointer = 0;
  String root = "";
  String fname = "";

  public long init(String root, String fname, byte site, byte gameType, byte numberOfSeats, byte cWeek) {
    return init(root, fname + site + "_" + gameType + "_" + numberOfSeats + "_w" + cWeek + ".hhex");
  }
  
  // opens DB for usage, returns the size of the DB
  public long init(String root, String fullname) {
    if (true) return init2(root, fullname);  // NOTE: The old init method (That was23 bb/100h. reading the complete file in memory is now deprecated.
    try {
      fc = new FileInputStream(root + fullname).getChannel();
      long tsize = fc.size();
      if (tsize >= Integer.MAX_VALUE) {
        System.out.println("ERROR: DB size is larger than Integer.MAXVALUE, truncated to " + Integer.MAX_VALUE);
        //tsize = Integer.MAX_VALUE - 1;
        tsize = Integer.MAX_VALUE-1;
      }
      try {
        byteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, tsize);
      } catch (Exception e) {
        System.out.println("Error while mapping DB to read, most probably out of memory: " + (root + fullname));
        return 0;
      }
      size = byteBuffer.capacity();
      //fc.close();
      //System.out.println("DEBUG: Size of opened DB is " + size + " bytes.");
      pointer = 0;
      readpointer = 0;
      this.root = root;
      this.fname = fname;
    } catch (FileNotFoundException fn) {
      return 0;
    } catch (Exception e) {
      e.printStackTrace(); // most probably file does not exist
      return 0;
    }
    return size;
  }

  // opens DB for usage, returns the size of the DB
  public long init2(String root, String fullname) {
    try {
      fc = new FileInputStream(root + fullname).getChannel();
      long tsize = fc.size();
      if (tsize >= Integer.MAX_VALUE) {
        System.out.println("ERROR: DB size is larger than Integer.MAXVALUE, truncated to " + Integer.MAX_VALUE);
        //tsize = Integer.MAX_VALUE - 1;
        tsize = Integer.MAX_VALUE-1;
      }
      size = (int)fc.size();
      /*
      try {
        byteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, tsize);
      } catch (Exception e) {
        System.out.println("Error while mapping DB to read, most probably out of memory: " + (root + fullname));
        return 0;
      }
      */
      //size = byteBuffer.capacity();
      //fc.close();
      //System.out.println("DEBUG: Size of opened DB is " + size + " bytes.");
      pointer = 0;
      readpointer = 0;
      this.root = root;
      this.fname = fname;
    } catch (FileNotFoundException fn) {
      return 0;
    } catch (Exception e) {
      e.printStackTrace(); // most probably file does not exist
      return 0;
    }
    return size;
  }

  public void closedb() {
    try {
     if (fc != null) fc.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void reset() {
    if (byteBuffer == null) return;
    byteBuffer.clear();
    readpointer = 0;
  }

  public boolean hasMoreHands() {
    //System.out.println(pointer + " " + size);
    return (pointer < size);
  }

  public long timeRead = 0;
  public long timeParse = 0;
  public PokerHand nextPokerHand() {
    long time1 = System.currentTimeMillis();
    byte[] b = nextHand();
    long time2 = System.currentTimeMillis();
    timeRead += (time2 - time1);
    PokerHand h = RawPokerHand.load(b);
    long time3 = System.currentTimeMillis();
    timeParse += (time3 - time2);
    //System.out.print(h);
    return h;
  }

  private byte[] len = new byte[2];
  public byte[] nextHand() {
    try {
      if (fc != null) {
        getByteArr(len);
        pointer += 2;
        int n = ((int)(len[0]))*100 + len[1];         // TODO: optimize
        byte[] b = new byte[n];
        getByteArr(b);
        pointer += n;
        //f.read(b);
        return b;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  int ln = 10*1024*1024;
  byte[] lastchunk = new byte[ln];
  int cp = -1;

  private void getByteArr(byte[] dest) {
    if (cp == -1) {
      cp = 0;
      readChunk();
    }
    if (cp + dest.length <= ln) {
      System.arraycopy(lastchunk, cp, dest, 0, dest.length);
      cp += dest.length;
      if (cp >= ln) {
        cp = 0;
        readChunk();
      }
    } else {
      // overflow!
      System.arraycopy(lastchunk, cp, dest, 0, ln-cp);
      readChunk();
      int copied = ln-cp;
      int rs = dest.length-copied;
      cp = 0;
      System.arraycopy(lastchunk, cp, dest, copied, rs);
      cp += rs;
    }
  }

  private void readChunk() {
    int readln = ln;
    if (readpointer + ln > size) readln = size - readpointer;
    //byteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, tsize);
    try {
      byteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, readpointer, readln);    // remove this line to rollback to the old init method
    } catch (Exception e) {
      e.printStackTrace();
    }
    byteBuffer.get(lastchunk, 0, readln);
    readpointer += ln;
  }

}

