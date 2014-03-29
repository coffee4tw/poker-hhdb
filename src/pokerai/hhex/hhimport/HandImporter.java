/*
  This code is released under GPL v3.

  @Author: Indiana (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910
 */
package pokerai.hhex.hhimport;

import pokerai.hhex.Consts;
import pokerai.hhex.Logger;
import pokerai.hhex.examples.ExportHandsExample;
import pokerai.hhex.examples.SampleUsage;
import pokerai.hhex.test.Tests;

import java.io.File;
import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.StringTokenizer;

public class HandImporter {
  //static final int BUFFER = 2048;
  int MAX_CHUNK = 30000000;   // max file size for Zip files

  static boolean overwrite = false;  // resume import, or overwrite (delete all files) and then import

  static HandManager hm = new HandManager();
  static long totalHands = 0;
  static String enter = new String(new byte[]{13, 10});

  static long totalTimeArrayCopy = 0, totalTimeBreadLine = 0, totalTimeParsing;

  public static void main(String[] args) {
    System.out.println("Hand Importer v1.5.4");
    System.out.println(new Date());
    Tests.main(args);
    String searchfolder = "C:\\hhdb\\";
    String rootfolder = "C:\\hhdb\\";
    if (args.length > 0) searchfolder = args[0];
    if (args.length > 1) rootfolder = args[1];
    if (!searchfolder.endsWith("\\")) searchfolder += "\\";
    if (!rootfolder.endsWith("\\")) rootfolder += "\\";
    // ---
    System.out.println("* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * ");
    Consts.rootFolder = rootfolder;
    hm.init(rootfolder, "pokerai.org.sample", overwrite);
    if (overwrite) {
      // Delete all files in root
      File dir = new File(rootfolder); String[] all = dir.list();
      for (int i = 0; i < all.length; i++) if (!new File(rootfolder + all[i]).isDirectory()) new File(rootfolder + all[i]).delete();
      // Delete all files in temp
      dir = new File(rootfolder + "temp"); if (!dir.exists()) dir.mkdir(); all = dir.list();
      for (int i = 0; i < all.length; i++) if (!new File(rootfolder + "temp\\" + all[i]).isDirectory()) new File(rootfolder + "temp\\" + all[i]).delete();
    }
    Logger.init(rootfolder);
    HandImporter h = new HandImporter();
    h.scanRecursive(searchfolder, "PokerStars Game #");
    hm.closedb();
    PlayerIndex.save();
    GameIdIndex.flush();
    Logger.closeLogs();
    dumpStatistics(false);
    System.out.println("* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * ");
    //ExportHandsExample.main(new String[]{rootfolder});
    new File(rootfolder + "pokerai.org.players.index").delete(); // Always delete the player index, so it is rebuilt
    if (new File(rootfolder + "pokerai.org.players.index").exists()) System.out.println("Delete of file was unsuccessfull!");
    SampleUsage.main(new String[]{rootfolder}); //This also calls PlayerStatistics.main(new String[]{rootfolder});
  }

  public static void println(String s, boolean log) { if (log) Logger.log(s); else System.out.println(s); }
  public static void dumpStatistics(boolean log) {
    println("+++ Time statistics +++", log);
    println("  |- Total time for reading zips: " + ds(ZipHandImporter.totalTimeReadingZipFiles / 60000.0) + "m", log);
    println("  |- Total time for extracting date: " + ds(ZipHandImporter.totalTimeReadingDate / 60000.0) + "m", log);
    println("  |- Total time for sorting zip entries: " + ds(ZipHandImporter.totalTimeSorting / 60000.0) + "m", log);
    println("  |- Total time for loading zip entries: "+ ds(ZipHandImporter.totalTimeReadingZipEntries / 60000.0) + "m", log);
    println("  |-+ Total time for hand parsing: " + ds(ZipHandImporter.totalTimeParsing / 60000.0) + "m", log);
    println("    |-+ Total time for dealing with gameids: " + ds(GameIdIndex.totalTime / 60000.0) + "m", log);
    println("      +- Total time for loading gameids: " + ds(GameIdIndex.totalTimeLoading / 60000.0) + "m", log);
    println("      +- Total time for  saving gameids: " + ds(GameIdIndex.totalTimeSaving / 60000.0) + "m", log);
    println("    |- Hand verificton (1): " + ds(HandManager.totalTimeVerify1 / 60000.0) + "m", log);
    println("    |- Hand verificton (2): " + ds(HandManager.totalTimeVerify2 / 60000.0) + "m", log);
    println("    |- Array copy         : " + ds(totalTimeArrayCopy / 60000.0) + "m", log);
    println("    |- BReadLine()        : " + ds(totalTimeBreadLine / 60000.0) + "m", log);
    println("    |- Actual hand parsing: " + ds(totalTimeParsing / 60000.0) + "m", log);
    println("          |- getPlayerID(): " + ds(PlayerIndex.totalTimeGetPlayerId / 60000.0) + "m", log);
    println("    |- Time to append hands in DB: " + ds(HandManager.totalTimeAppendHand / 60000.0) + "m", log);
    println("    |- Time for writting to files in Logger: " + ds(Logger.totalTimeLogging / 60000.0) + "m", log);
    println("---", log);
  }


  public void scanRecursive(String dirName, String searchString) {
    long time = System.currentTimeMillis();
    scanRecursive(dirName, searchString, 0);
    double time2 = (System.currentTimeMillis()-time) / 1000.0;
    System.out.println("Parsing completed for " + ds(time2 / 60.0) + " minutes.");
    System.out.println(totalHands + " hands imported, " + Logger.errorsN + " errors, " +
            Logger.truncated + " truncated hands, " + Logger.duplicated + " duplicated hands.");
    System.out.println("Imported with speed " + (long)(totalHands / time2) + " hands/second.");
    System.out.println(ds(hm.totalSize() / totalHands) + " bytes per hand. ");
  }

  public void scanRecursive(String dirName, String searchString, int level) {
    File f = new File(dirName);
    String[] list = f.list();
    long time = System.currentTimeMillis();
    for (int i = 0; i < list.length; i++) {
      File f2 = new File(dirName + list[i]);
      if (f2.isDirectory()) {
        scanRecursive(dirName + list[i] + "\\", searchString, level + 2);
      } else if (list[i].endsWith(".txt")) {
        try {
          parseHands(dirName + list[i], searchString);
        } catch (Exception e) { System.out.println("Uncaught exception while parsing (HandImporter) " + dirName + list[i]); e.printStackTrace(); }
      } else if (list[i].endsWith(".zip")) {
        try {
          ZipHandImporter.handleZipFile(this, dirName + list[i]);
        } catch (java.lang.NoClassDefFoundError e) {
          System.out.println("ZIP library not found, use \"java -cp ./libs/truezip-6.jar;\" for your classpath. ");
          System.exit(1);
        }
      }
      //if (list.length > 10) { // leaf directory, dump percentage of completion
      //  if (i % (list.length / 20) == 0) System.out.print( Math.round(i*100.0 / list.length) + "% ");
      //}
    }
    System.out.println(dirName.substring(dirName.lastIndexOf("\\", dirName.length() - 3), dirName.length()) + " finished for " + ds((System.currentTimeMillis() - time)/60000) + " minutes. ");
  }

  public final String newline = "\n";
  public final int maxH = 1000;
  public final String[] hand = new String[maxH];

  public void parseHands(String fileName, String searchString) {
    //System.out.println("Parsing " + fileName + " ... ");
    initFile(fileName);
    String s = readLine();
    while (!EOF()) {
      while (!s.startsWith(searchString) && !EOF()) s = readLine();
      if (s.startsWith(searchString)) {
        int handN = 1;
        hand[0] = s;
        while (!EOF()) {
          String s2 = readLine(); // 90% performance spent here (and other readLines) if RandomAccessFile is used
          if (s2.startsWith(searchString) || EOF()) {
            s = s2;
            totalHands++;
            if (totalHands % 1000000 == 0) {
              Logger.log("Hands " + totalHands + "/" + Logger.duplicated + ", current memory levels, total "
                      + Runtime.getRuntime().totalMemory()/1048576 + ", free " + Runtime.getRuntime().freeMemory()/1048576);
              if (totalHands % 5000000 == 0) GameIdIndex.flush();
              dumpStatistics(true);
            }
            //System.out.println(handN);
            RawPokerHandSaver h = HandParser_PokerStars.parseHand(hand, handN, fileName); // 2%
            Logger.dumpHand = false;
            hm.appendHand(h);
            if (Logger.dumpHand) Logger.dumpHandToErrors(hand, handN);
            //System.out.print(h);
            break;
          }
          hand[handN] = s2; handN++;
          if (handN == maxH) { Logger.writeToErrors("ERROR: " + maxH + " lines not enough to parse hand. ", null); return; }   // skip the whole file, otherwise corrupted hands
        }
      }
    }
    //closeFile();
    for (int i = 0; i < hand.length; i++) hand[i] = null;
  }

  public void parseHandsFromString(String fileName, byte[] st, int length, String searchString) {
    //System.out.println("Parsing " + st + " ... ");
    //this.bst = st;
    //this.bst = new char[st.length];
    if (length > MAX_CHUNK) {
      Logger.writeToErrors("ERROR: Too big file to parse (" + length + "), max supported is " + MAX_CHUNK + ". ", null);
      System.out.println("ERROR: Too big file to parse (" + length + "), max supported is " + MAX_CHUNK + ". ");
    }
    bstn = length;
    long time = System.currentTimeMillis();
    for (int i = 0; i < bstn; i++) bst[i] = (char)(st[i]);
    totalTimeArrayCopy += (System.currentTimeMillis()-time);
    pos = 0;
    String s = breadLine();
    while (!bEOF()) {
     while (!s.startsWith(searchString) && !bEOF()) s = breadLine();
     if (s.startsWith(searchString)) {
       int handN = 1;
       hand[0] = s;
       while (!bEOF()) {
         String s2 = breadLine(); // 90% performance spent here (and other readLines) if RandomAccessFile is used
         if (s2.startsWith(searchString) || bEOF()) {
           s = s2;
           totalHands++;
           if (totalHands % 1000000 == 0) {
             Logger.log("Hands " + totalHands + "/" + Logger.duplicated + ", current memory levels, total "
                     + Runtime.getRuntime().totalMemory()/1048576 + ", free " + Runtime.getRuntime().freeMemory()/1048576);
             if (totalHands % 5000000 == 0) GameIdIndex.flush();
             dumpStatistics(true);
           }
           //System.out.println(handN);
           time = System.currentTimeMillis();
           RawPokerHandSaver h = HandParser_PokerStars.parseHand(hand, handN, fileName); // 2%
           totalTimeParsing += (System.currentTimeMillis()-time);
           Logger.dumpHand = false;
           hm.appendHand(h);
           if (Logger.dumpHand) Logger.dumpHandToErrors(hand, handN);
           //System.out.print(h);
           break;
         }
         hand[handN] = s2; handN++;
         if (handN == maxH) { Logger.writeToErrors("ERROR: " + maxH + " lines not enough to parse hand. ", null); return; }   // skip the whole file, otherwise corrupted hands
       }
     }
    }
    //closeFile();
    for (int i = 0; i < hand.length; i++) hand[i] = null;
  }

  // -------- Implementation with Random Access File
  /*
  private static RandomAccessFile f = null;
  private static void initFile(String fileName) {
    try { f = new RandomAccessFile(fileName, "r"); } catch(Exception e) { e.printStackTrace(); if (f != null) { try { f.close(); } catch (Exception e2) {}; } } }
  private static boolean EOF() {
    try { return (f.getFilePointer() >= f.length()); } catch(Exception e) { e.printStackTrace(); if (f != null) { try { f.close(); } catch (Exception e2) {}; }} return true; }
  private static void closeFile() {
    try { f.close(); } catch(Exception e) { e.printStackTrace(); if (f != null) { try { f.close(); } catch (Exception e2) {}; }} }
  private static String readLine() {
    try { return f.readLine(); } catch(Exception e) { e.printStackTrace(); if (f != null) { try { f.close(); } catch (Exception e2) {}; }} return null; }
  */

  // -------- Implementation with StringTokernizer
  private StringTokenizer st = null;
  private void initFile(String fileName) {
    try {
      FileInputStream fs = new FileInputStream(fileName);
      FileChannel fc = null;
      fc = fs.getChannel();
      int size = (int)(new File(fileName)).length();
      MappedByteBuffer byteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
      //byteBuffer.clear();
      byte[] data = new byte[size];
      byteBuffer.get(data, 0, size);
      String s = new String(data);
      fc.close();
      fs.close();
      st = new StringTokenizer(s, enter);
      s = null;
    } catch(Exception e) { e.printStackTrace(); }
  }

  private boolean EOF() { return !st.hasMoreElements(); }
  private String readLine() { return (String)st.nextElement(); }
  public static double ds(double x) { return Math.round(x*100)/100.0; }

  // -------- Implementation of byte[] tokenizer
  //private byte[] bst = null;
  private char[] bst = new char[MAX_CHUNK];
  int bstn = 0;
  int pos = 0;
  private boolean bEOF() { return (pos >= bstn-1); }
  private String breadLine() {
    long time = System.currentTimeMillis();
    int savepos = pos;
    while (true) {
      if (bst[pos] == 10 || bst[pos] == 13) {
        //if (bst[pos+1] == 10)
        //System.out.println("enter");
        String res = new String(bst, savepos, (pos - savepos));
        if (bst[pos+1] == 13 || bst[pos+1] == 10) pos += 2;
                                             else pos++;
        totalTimeBreadLine += (System.currentTimeMillis()-time);
        return res;
      }
      pos++;
      if (pos >= bstn-1) {
        //System.out.println("here");
        totalTimeBreadLine += (System.currentTimeMillis()-time);
        return new String(bst, savepos, (pos - savepos + 1));
      }
    }
  }

}

