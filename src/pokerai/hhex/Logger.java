/*
  This code is released under GPL v3.

  @Author: Indiana (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910
 */
package pokerai.hhex;

import pokerai.hhex.internal.RawPokerHand;
import java.io.RandomAccessFile;


public class Logger {

  static RandomAccessFile log = null;
  static RandomAccessFile chat = null;
  static RandomAccessFile errors = null;
  static RandomAccessFile dupl = null;
  static RandomAccessFile zeropl = null;
  static String enter = new String(new byte[]{13, 10});
  public static int errorsN = 0;
  public static int duplicated = 0;
  public static int truncated = 0;

  static boolean logsOpen = false;

  //Statistics
  public static long totalTimeLogging = 0;

  public static void init(String rootfolder) {
    try { log = new RandomAccessFile(rootfolder + "log.log", "rw"); log.seek(log.length()); } catch (Exception e) { e.printStackTrace(); }
    try { errors = new RandomAccessFile(rootfolder + "errors.log", "rw"); errors.seek(errors.length()); } catch (Exception e) { e.printStackTrace(); }
    try { chat = new RandomAccessFile(rootfolder + "chats.log", "rw"); chat.seek(chat.length()); } catch (Exception e) { e.printStackTrace(); }
    try { dupl = new RandomAccessFile(rootfolder + "errors-d.log", "rw"); dupl.seek(dupl.length()); } catch (Exception e) { e.printStackTrace(); }
    try { zeropl = new RandomAccessFile(rootfolder + "errors-0pl.log", "rw"); zeropl.seek(zeropl.length()); } catch (Exception e) { e.printStackTrace(); }
    logsOpen = true;
  }

  public static void closeLogs() {
    try { log.close(); chat.close(); errors.close(); dupl.close(); zeropl.close(); } catch (Exception e) {};
    logsOpen = false;
  }

  // -------- Logging normal log
  public static void log(String s) {
    try { log.writeBytes(s + enter); } catch (Exception e) { e.printStackTrace(); }
  }

  // -------- Logging chat
  public static int chatN = 100;
  public static int writeToChat(String s) {
    long t = System.currentTimeMillis();
    chatN++;
    try { chat.writeBytes(s + enter); } catch (Exception e) { e.printStackTrace(); }
    totalTimeLogging += (System.currentTimeMillis() - t);
    return chatN;
  }

  // -------- Logging buggy hands
  public static boolean dumpHand = false;
  public static String parsedFileName = "unknown";
  public static void writeToErrors(String s, RawPokerHand hand) { writeToErrors(s, hand, false); }
  public static void writeToErrors(String s, RawPokerHand hand, boolean dumpHand) {
    Logger.dumpHand = dumpHand;
    errorsN++;
    writeTo("#" + errorsN + " " + s, hand, errors, true);
  }

  public static void writeToDupl(String s, RawPokerHand hand) { writeTo(s, hand, dupl, false); duplicated++; }
  public static void writeTo0pl(String s, RawPokerHand hand)  { writeTo(s, hand, zeropl, true); truncated++; }

  private static void writeTo(String s, RawPokerHand hand, RandomAccessFile f, boolean saveError) {
    if (hand != null) hand.buggyhand = true; // means hand will not be saved
    if (!logsOpen) return;
    if (!saveError) return;
    long t = System.currentTimeMillis();
    try { f.writeBytes(s + " - - - <in " + parsedFileName + "> " + enter); } catch (Exception e) { e.printStackTrace(); }
    totalTimeLogging += (System.currentTimeMillis() - t);
  }

  public static void dumpHandToErrors(String[] hand, int handN) {
    long t = System.currentTimeMillis();
    for (int i = 0; i < handN; i++) {
      try { errors.writeBytes("   " + hand[i] + enter); } catch (Exception e) { e.printStackTrace(); }
    }
    totalTimeLogging += (System.currentTimeMillis() - t);
  }

}


