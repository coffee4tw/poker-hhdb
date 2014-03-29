/*
  This code is released under GPL v3.

  @Author: CodeD (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910
 */
package pokerai.hhex.hhimport;

import de.schlichtherle.io.File;
import de.schlichtherle.io.rof.ReadOnlyFile;
import de.schlichtherle.util.zip.ZipEntry;
import de.schlichtherle.util.zip.ZipFile;
import pokerai.hhex.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Enumeration;

public class ZipHandImporter {

  public static void handleZipFile(HandImporter h, String zipFileName) {
    File f = new File(zipFileName);
    handleZipFile(h, f);
  }

  // Time statistics
  static long totalTimeSorting = 0;
  static long totalTimeReadingZipFiles = 0;
  static long totalTimeReadingZipEntries = 0;
  static long totalTimeReadingDate = 0;
  static long totalTimeParsing = 0;

  private static void handleZipFile(HandImporter h, File f) {
    MemoryReadOnlyFile memoryFile = null;
    ZipFile zipFile = null;
     try {
       long timeall = System.currentTimeMillis();
       long time = System.currentTimeMillis();
       memoryFile = new MemoryReadOnlyFile(f);
       zipFile = new ZipFile(memoryFile);
       Enumeration enumeration = zipFile.entries();
       totalTimeReadingZipFiles += (System.currentTimeMillis() - time);
       ZipEntry[] entries = new ZipEntry[zipFile.size()];
       int zN = 0;
       time = System.currentTimeMillis();
       // Get all entries
       while(enumeration.hasMoreElements()) {
         ZipEntry entry = (ZipEntry) enumeration.nextElement();
         if (entry.getName().toLowerCase().endsWith(".txt")) {
           entries[zN] = entry; zN++;
         }
       }
       // Get the date for each entry: this requires inflating the archives
       time = System.currentTimeMillis();
       long[] times = new long[zN];
       byte[] tempdata = new byte[1000];
       for (int i = 0; i < zN; i++) {
         InputStream is = null;
         try {
           is = zipFile.getInputStream(entries[i]);
           int r = is.read(tempdata, 0, tempdata.length);
           Logger.parsedFileName = entries[i].getName();
           times[i] = pokerStars_GetZipDate(tempdata);
           //if (times[i] == -1) times[i] = entries[i].getTime();  // These will be skipped!
        } catch (Exception e) {
           System.out.println("Error reading " + f.getName());
           // e.printStackTrace();
         } finally {
           if (is != null) {try { is.close(); } catch(Exception e) { e.printStackTrace(); }}
         }
       }
       totalTimeReadingDate += (System.currentTimeMillis() - time);
       time = System.currentTimeMillis();
       // Sort them by last modified date
       //for (int i = 0; i < zN; i++) times[i] = entries[i].getTime();
       for (int i = 0; i < zN-1; i++)
         for (int j = i+1; j < zN; j++) if (times[i] > times[j]) {
           ZipEntry t = entries[i]; entries[i] = entries[j]; entries[j] = t;
           long ti = times[i]; times[i] = times[j]; times[j] = ti;
         }
       totalTimeSorting += (System.currentTimeMillis() - time);
       // Read all data
       for (int i = 0; i < zN; i++) if (times[i] >= 0) {
         InputStream is = null;
         try {
           time = System.currentTimeMillis();
           is = zipFile.getInputStream(entries[i]);
           int read = 0, r = 0;
           byte[] data = new byte[(int) entries[i].getSize()];
           while (read < data.length && (r = is.read(data,read,data.length-read)) != -1) { read += r; }
           if (read != data.length) { System.err.println("Still, did not read enough... should never happen " + f); }
           totalTimeReadingZipEntries += (System.currentTimeMillis() - time);
           time = System.currentTimeMillis();
           h.parseHandsFromString(entries[i].getName(), data,  read, "PokerStars Game #");
           data = null;
           totalTimeParsing += (System.currentTimeMillis() - time);
        } catch(Exception e) {
          System.out.println("Corrupted archive? " + f.getName());
          //e.printStackTrace();
        } finally {
          if (is != null) {
            try { is.close(); } catch(Exception e) { e.printStackTrace(); }
          }
        }
      }
      double t2 = HandImporter.ds((System.currentTimeMillis() - timeall) / 60000.0);
      if (memoryFile.length() > 90000000) System.out.println("   " + f.getName() + ", time " + t2);           // if ZIP file is >30Mb dump independantly
      zipFile.close();
      memoryFile.close();
      System.gc();
    } catch (Exception e) {
       System.err.println("Problems with zip file: " + f);
       e.printStackTrace();
       try { if (zipFile != null) zipFile.close(); if (memoryFile != null) memoryFile.close(); } catch (Exception e2) {};
    }
  }

  private static long pokerStars_GetZipDate(byte[] data) {
    long d = 0;
    int k = -1, enter = -1;
    for (int i = 0; i < data.length - 4; i++) {
      if (data[i] == ' ') if (data[i+1] == '2') if (data[i+2] == '0') if (data[i+3] == '0') {
        k = i;
      }
      if((data[i] == 13 || data[i] == 10) && k > 0) {
        enter = i;
        break;
      }
    }
    if (k > 0 && enter > 0) {
      String date = new String(data, k + 1, enter - k - 1);
      //System.out.println(date);
      try {
        d = HandParser_PokerStars.getDate(date);
      } catch (Exception e) {
        Logger.writeToErrors("ERROR: Couldn't parse date (1): " + date, null);
        return -1;
      }
    } else {
      Logger.writeToErrors("ERROR: Couldn't parse date (2): " + new String(data).substring(0, 100), null);
      return -1;
    }
    return d;
  }


}

class MemoryReadOnlyFile implements ReadOnlyFile {

  byte[] data;
  int pointer = 0;

  public MemoryReadOnlyFile(java.io.File f) throws IOException {
     try {
       java.io.FileInputStream f1 = new java.io.FileInputStream(f);
       FileChannel fc = f1.getChannel();
       data = new byte[(int) fc.size()];
       MappedByteBuffer b = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
       b.get(data);
       b.clear();
       fc.close();
       f1.close();
     } catch (FileNotFoundException e) {
        e.printStackTrace();
     }
  }

  @Override
  public void close() throws IOException {
    data = null;
  }

  @Override
  public long getFilePointer() throws IOException {
     return pointer;
  }

  @Override
  public long length() throws IOException {
     return data.length;
  }

  @Override
  public int read() throws IOException {
     if(pointer < data.length) {
        return data[pointer++];
     }
     return -1;
  }

  @Override
  public int read(byte[] arg0) throws IOException {
     if (pointer < data.length) {
        if(pointer + arg0.length <= data.length) {
           System.arraycopy(data, pointer, arg0, 0, arg0.length);
           pointer += arg0.length;
           return arg0.length;
        } else {
           int tmp = data.length-pointer;
           System.arraycopy(data, pointer, arg0, 0, tmp);
           pointer = data.length;
           return tmp;
        }
     }
     return -1;
  }

  @Override
  public int read(byte[] arg0, int off, int len) throws IOException {
     if(pointer < data.length) {
        if(pointer + len <= data.length) {
           System.arraycopy(data, pointer, arg0, off, len);
           pointer += len;
           return len;
        } else {
           int tmp = data.length-pointer;
           System.arraycopy(data, pointer, arg0, off, tmp);
           pointer = data.length;
           return tmp;
        }
     }
     return -1;
  }

  @Override
  public void readFully(byte[] arg0) throws IOException {
     read(arg0);
  }

  @Override
  public void readFully(byte[] arg0, int arg1, int arg2)
        throws IOException {
     read(arg0,arg1,arg2);
  }

  @Override
  public void seek(long arg0) throws IOException {
     pointer = (int) arg0;
  }

  @Override
  public int skipBytes(int arg0) throws IOException {
     if(pointer + arg0 < data.length) {
        pointer += arg0;
        return arg0;
     } else {
        int tmp = data.length-pointer;
        pointer = data.length;
        return tmp;
     }
  }

}