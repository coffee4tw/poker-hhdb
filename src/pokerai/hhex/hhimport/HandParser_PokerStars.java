/*
  This code is released under GPL v3.

  @Author: Indiana (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910
 */
package pokerai.hhex.hhimport;

/*

  Notes:
  - raise amounts are the total amount after the raise
  - call amounts are the delta (amountToCall)
  - players muck cards, even when both players are all-in --- not sure ?!?! sometimes only!
 */


import pokerai.hhex.Action;
import pokerai.hhex.Consts;
import pokerai.hhex.Logger;

import java.util.Calendar;
import java.util.Hashtable;

public class HandParser_PokerStars {

  public static String enter = new String(new byte[]{13,10});

  public static RawPokerHandSaver parseHand(String[] vhand, int n, String fileName) {
    RawPokerHandSaver hand = new RawPokerHandSaver();
    String lastLine = "";
    try {
      for (int i = 0; i < n; i++) {
        lastLine = vhand[i];
        parseLine(vhand[i], hand, fileName);
      }
    } catch (Exception e) {
      System.out.println("Uncaught exception during parsing (HandParser_PokerStars): ");
      e.printStackTrace();
      Logger.writeToErrors("ERROR: Uncaught exception during parsing! <" + lastLine + "> ", hand);
    }
    return hand;
  }

  static Hashtable temp = new Hashtable();
  public static void parseLine(String line, RawPokerHandSaver hand, String fileName) {
    Logger.parsedFileName = fileName;
    if (line.length() == 0) return;
    //System.out.println(line);
    // Pptimization prescans
    // ********************************* Header Line 1
    if (hand.getGameId() == Consts.GT_UNKNOWN) if (line.startsWith("PokerStars Game")) {
      try {
        hand.site = Consts.SITE_PS;
        hand.setGameId(new Long(gets(line, "Game #", ":")).longValue());
      } catch (Exception e) {
        Logger.writeToErrors("ERROR reading Header/GameId, line <" + line + ">", hand);
      }
      try {
        // Limits
             if (line.indexOf("Hold'em Limit") >= 0)     hand.game = Consts.HOLDEM_FL;
        else if (line.indexOf("Hold'em No Limit") >= 0)  hand.game = Consts.HOLDEM_NL;
        else if (line.indexOf("Hold'em Pot Limit") >= 0) hand.game = Consts.HOLDEM_PL;
        else if (line.indexOf("Omaha Pot Limit") >= 0)   hand.game = Consts.OMAHA_PL;
        else if (line.indexOf("Omaha Limit") >= 0)       hand.game = Consts.OMAHA_FL;
        else if (line.indexOf("HOLD'EM NO LIMIT") >= 0)  hand.game = Consts.HOLDEM_NL;  // Stars 2007 format
        else Logger.writeToErrors("ERROR: GameType Not identified! <" + line + "> ", hand);
        // Blinds
        double sb = Double.parseDouble(gets(line, "($", "/"));
        double bb = Double.parseDouble(gets(line, "/$", ")"));
        if (hand.game == Consts.HOLDEM_FL || hand.game == Consts.OMAHA_FL) {
          int[] bl = Consts.getBlindsRow((int)(sb * 100), (int)(bb * 100));
          if (bl == null) {
            System.out.println("ERROR: LIMIT BLINDS NOT FOUND, sb= " + sb + ", bb = " + bb);
            Logger.writeToErrors("ERROR: LIMIT BLINDS NOT FOUND, sb= " + sb + ", bb = " + bb, hand);
            return;
          }
          if (bl[2] == -1) {
            System.out.println("ERROR: LIMIT BLINDS NOT SET, sb= " + sb + ", bb = " + bb);
            Logger.writeToErrors("ERROR: LIMIT BLINDS NOT SET, sb= " + sb + ", bb = " + bb, hand);
            return;
          }
          hand._SB = bl[2];
          hand._BB = bl[3];
        } else { // PL or NL games
          hand._SB = (int)(sb * 100);
          hand._BB = (int)(bb * 100);
        }

        String s = hand._SB + " " + hand._BB;
        if (temp.get(s) == null) {
          //System.out.println(s);
          temp.put(s, s);
        }
      } catch (Exception e) {
        Logger.writeToErrors("ERROR reading Header/Limits, line <" + line + ">", hand);
      }
      String date = "";
      try {
        // date
        hand.date = 0;
        int k = line.indexOf("- ");
        if (k > 0) {
          date = line.substring(k + 2, line.length());
          hand.date = getDate(date);
          if (GameIdIndex.isHandAlreadyImported(hand.getGameId(), hand.date)) {
            Logger.writeToDupl("INFO duplicated hand gameID <" + hand.getGameId() + ">, date <" + date + ">", hand);
          }
        }
      } catch (Exception e) {
        Logger.writeToErrors("ERROR reading Header/Date, date <" + date + ">, line <" + line + ">", hand);
      }
      return;
    }
    // ********************************* Header Line 2
    if (line.startsWith("Table '")) {
      try {
        hand.tableId = getMD5hash(gets(line, "'", "'"));
        byte seats = new Byte(gets(line, "' ", "-max")).byteValue();
        if (seats == 0) { Logger.writeToErrors("ERROR: Zero seats, line <" + line + ">", hand); }
        hand.setNumberOfSeats(seats);
        String buttonStr = gets(line, "Seat #", " is the button");
        if (!buttonStr.equals("")) {
          hand.button = new Byte(buttonStr).byteValue();
        } else {
          hand.button = Consts.BUTTON_UNKNOWN;
          // System.out.println("ERROR: Button seat not found, line: " + line + ", GameId:" + hand.gameId);
          // --> If there is no button, then hand was most probably canceled --> "Hand cancelled" dumped afterwards
        }
      } catch (Exception e) {
        Logger.writeToErrors("ERROR reading table and button, line <" + line + ">", hand);
      }
      return;
    }
    boolean startsWithSeat = line.startsWith("Seat ");
    // ********************************* SEATS - PlayerNames and Stacks
    if (startsWithSeat && line.indexOf("in chips") > 0 && (hand.lastround < Consts.STREET_SHOWDOWN)) {
      byte seat = 0;
      try {
        seat = Byte.parseByte(gets(line, "Seat ", ": "));
      } catch (Exception e) { Logger.writeToErrors("ERROR: Exception while parsing Player Seat: <" + gets(line, "Seat ", ": ") + ">, line: <" + line + ">", hand);  return; }
      String player = gets(line, ": ", " ($");
      if (player.length() > Consts.MAX_PLAYERNAME_LENGTH) { Logger.writeToErrors("ERROR: Too long player name (1): <" + player + ">, line: <" + line + ">", hand);  return; }
      int playerId = PlayerIndex.getPlayerId(player, line, hand);
      double std = 0;
      try { std = ((Double.parseDouble(gets(line, "($", " in chips", true)))*100.0)/hand._BB;
          } catch (Exception e) { Logger.writeToErrors("ERROR: Player's stack in chips parsed wrongly: <" + player + ">, line: <" + line + ">", hand);  return; }
      int stack = (int)Math.round(std*100);
      if (hand.players == null) { Logger.writeToErrors("ERROR: Player's array and stack not initialized (null): <" + player + ">, line: <" + line + ">", hand);  return; }
      hand.players[seat-1] = playerId;
      hand.stacks[seat-1] = stack;
      //System.out.println(seat + " : " + playerId + " : " + stack);
      return;
    }
    // ********************************* BOARD, HOLE CARDS
    if (line.charAt(0) == '*') {
      if (line.startsWith("*** HOLE CARDS ***")) {
        if (hand.lastround >= Consts.STREET_PREFLOP) { Logger.writeTo0pl("ERROR: Twice seen STREET_PREFLOP, mixed up hand!", hand); return; }
        hand.lastround = Consts.STREET_PREFLOP;
        for (int i = 0; i < 5; i++) hand.board[i] = Consts.INVALID_CARD;
        return;
      }
      if (line.startsWith("*** FLOP ***")) {
        if (hand.lastround >= Consts.STREET_FLOP) { Logger.writeTo0pl("ERROR: Twice seen STREET_FLOP, mixed up hand!", hand); return; }
        hand.lastround = Consts.STREET_FLOP;
        String card1 = gets(line, "* [", " ").trim();
        String card2 = gets(line, card1, " ").trim();
        String card3 = gets(line, card2, "]").trim();
        //System.out.println(card1 + " --- " + card2 + " --- " + card3);
        if (card1.length() == 2 && card2.length() == 2 && card3.length() == 2) {
          hand.board[0] = Consts.getCard(card1);
          hand.board[1] = Consts.getCard(card2);
          hand.board[2] = Consts.getCard(card3);
        } else {
          Logger.writeToErrors("ERROR reading flop cards, line = <" + line + ">", hand);
        }
        return;
      }
      if (line.startsWith("*** TURN ***")) {
        if (hand.lastround >= Consts.STREET_TURN) { Logger.writeTo0pl("ERROR: Twice seen STREET_TURN, mixed up hand!", hand); return; }
        hand.lastround = Consts.STREET_TURN;
        String card4 = gets(line, "] [", "]");
        if (card4.length() == 2) {
          hand.board[3] = Consts.getCard(card4);
        } else {
          Logger.writeToErrors("ERROR reading turn card, line = <" + line + ">", hand);
        }
        return;
      }
      if (line.startsWith("*** RIVER ***")) {
        if (hand.lastround >= Consts.STREET_RIVER) { Logger.writeTo0pl("ERROR: Twice seen STREET_RIVER, mixed up hand!", hand); return; }
        hand.lastround = Consts.STREET_RIVER;
        String card5 = gets(line, "] [", "]");
        if (card5.length() == 2) {
          hand.board[4] = Consts.getCard(card5);
        } else {
          Logger.writeToErrors("ERROR reading river card, line = <" + line + ">", hand);
        }
        return;
      }
      if (line.startsWith("*** SHOW DOWN ***")) {
        if (hand.lastround >= Consts.STREET_SHOWDOWN) { Logger.writeTo0pl("ERROR: Twice seen STREET_SHOWDOWN, mixed up hand!", hand); return; }
        hand.lastround = Consts.STREET_SHOWDOWN;
        return;
      }
      if (line.startsWith("*** SUMMARY ***")) {
        if (hand.handComplete) { Logger.writeTo0pl("ERROR: Twice seen SUMMARY, mixed up hand!", hand); return; }
        hand.handComplete = true;
      }
    }
    // ********************************* ACTIONS
    int k = line.indexOf(": ");
    // k <15 means to avoid isses for "Uncalled bet ($0.25) returned to :THM: HiHo", player names are max 12 characters
    if (k > 0 && k < 15 && !startsWithSeat) {
      int k1 = line.indexOf(": ", k+1); if (k1 >= 0) k = k1;   // Handle player IDs like this: "ID: The Game:"
      Action a = null;
      String name = line.substring(0, k);
      if (name.length() > Consts.MAX_PLAYERNAME_LENGTH) { Logger.writeToErrors("ERROR: Too long player name (1): <" + name + ">, line: <" + line + ">", hand);  return; }
      String action = line.substring(k + 1, line.length());
      int k2 = action.lastIndexOf("$");
      double amount = -1;
      if (k2 > 0) {
        int k3 = action.indexOf(" ", k2+1);
        if (k3 < 0) k3 = action.length();
        String parseStr = action.substring(k2+1, k3);
        while (parseStr.endsWith(")")) parseStr = parseStr.substring(0, parseStr.length()-1);
        try {
          amount = Double.parseDouble(parseStr);
        } catch (Exception e) {
          Logger.writeToErrors("ERROR: Failed to parse amount " + parseStr + ", line: <" + line + ">", hand);
        }
      }
      int playerid = PlayerIndex.getPlayerId(name, line, hand);
      if (action.startsWith(" posts small blind")) {
        if (!hand.sbAlreadyPosted) {
          a = new Action(hand, playerid, name, line, Consts.ACTION_SMALLBLIND, amount);
          hand.sbAlreadyPosted = true;
        } else {
          a = new Action(hand, playerid, name, line, Consts.ACTION_DEAD_SB, amount);
        }
      }
      else if (action.startsWith(" posts big blind")) a = new Action(hand, playerid, name, line, Consts.ACTION_BIGBLIND, amount);
      else if (action.startsWith(" posts small & big blinds")) a = new Action(hand, playerid, name, line, Consts.ACTION_BOTHBLINDS, amount);
      //else if (action.startsWith(" posts dead blind")) a = new Action(hand, name, line, Consts.ACTION_DEADBLIND, amount);
      else if (action.startsWith(" folds")) a = new Action(hand, playerid, name, line, Consts.ACTION_FOLD, -1);
      else if (action.startsWith(" checks")) a = new Action(hand, playerid, name, line, Consts.ACTION_CHECK, -1);
//      else if (action.endsWith(" is all-in")) a = new Action(hand, name, line, Consts.ACTION_ALLIN, amount);      // All-ins are not parsed due to ambiquity
      else if (action.startsWith(" calls")) a = new Action(hand, playerid, name, line, Consts.ACTION_CALL, amount);
      else if (action.startsWith(" bets")) a = new Action(hand, playerid, name, line, Consts.ACTION_BET, amount);
      else if (action.startsWith(" raises")) a = new Action(hand, playerid, name, line, Consts.ACTION_RAISE, amount);
      else if (action.startsWith(" sits out")) a = new Action(hand, playerid, name, line, Consts.ACTION_SITTINGOUT, amount);
      else if (action.startsWith(" is sitting out")) a = new Action(hand, playerid, name, line, Consts.ACTION_SITTINGOUT, amount);     // seen twice, with and without column after player's name. Alias like ":maSkraP:" leads to ambiguities, e.g. ":maSkraP: is sitting out"
      else if (action.startsWith(" shows")) { // special cards parsing
        try {
          //System.out.println(action);
          byte c1 = Consts.getCard(action.charAt(8), action.charAt(9));
          byte c2 = Consts.getCard(action.charAt(11), action.charAt(12));
          byte c3 = -1, c4 = -1;
          int len = 3;
          if (hand.game == Consts.OMAHA_NL || hand.game == Consts.OMAHA_PL || hand.game == Consts.OMAHA_FL) {
            c3 = Consts.getCard(action.charAt(14), action.charAt(15));
            c4 = Consts.getCard(action.charAt(17), action.charAt(18));
            len = 5;
          }
          int i = 0;
          while (i < hand.maxCardsPx && hand.cardsPx[i] != null) i++;
          if (i == hand.maxCardsPx) {
            Logger.writeToErrors("ERROR: More than " + hand.maxCardsPx + " cards at showdown, some info is lost.", hand);
          } else {
            hand.cardsPx[i] = new byte[len];
            hand.cardsPx[i][0] = (byte)((len-1)*10 + hand.getSeatId(playerid, name));
            hand.cardsPx[i][1] = c1; hand.cardsPx[i][2] = c2;
            if (len > 3) hand.cardsPx[i][3] = c3;
            if (len > 4) hand.cardsPx[i][4] = c4;
          }
        } catch (Exception e) {
          //e.printStackTrace();
          Logger.writeToErrors("ERROR: Error parsing cards, line: <" + line + ">, action: <" + action + ">", hand);
        }
      } else {
         int k3 = line.indexOf(" is sitting out");
         if (k3 > 0) a = new Action(hand, playerid, line.substring(0, k3), line, Consts.ACTION_SITTINGOUT, amount);
         if (Consts.saveTableJoinEvents) {
           k3 = line.indexOf(" has returned");
           if (k3 > 0) a = new Action(hand, playerid, line.substring(0, k3), line, Consts.ACTION_SITTINGIN, amount);
           k3 = line.indexOf(" leaves the table");
           if (k3 > 0) a = new Action(hand, playerid, line.substring(0, k3), line, Consts.ACTION_LEAVETABLE, amount);
           k3 = line.indexOf(" joins the table");
           if (k3 > 0) a = new Action(hand, playerid, line.substring(0, k3), line, Consts.ACTION_JOINTABLE, amount);
         }
      }

      if (a != null) {
        hand.addAction(a);
      } else {
        // show is the type of hand - 2 pairs, etc; "collected" is here by mistake; "will be allowed" - player can't play the button after skipping blinds
        if (line.indexOf("show") < 0 && line.indexOf("muck") < 0 && line.indexOf("said") < 0 &&
                line.indexOf("collected") < 0 && line.indexOf("will be allowed") < 0 && line.indexOf("has timed out") < 0 &&
                line.indexOf("is connected") < 0 && line.indexOf("is disconnected") < 0) {
          Logger.writeToErrors("Unrecognized action, line <" + line + ">, action <" + action + ">", hand);
        } 
      }
      return;
    }
    /*
      Uncalled bet ($44.80) returned to yugor
      yugor collected $6 from pot
     */
    if (!startsWithSeat) {
      if (eventParse(line, " collected $"     , 0, 3, Consts.ACTION_WON, true, hand, true)) return;
      if (eventParse(line, "Uncalled bet ($"  , 1, 4, Consts.ACTION_WON, true, hand, true)) return;
      if (eventParse(line, " is sitting out"  , 0, 2, Consts.ACTION_SITTINGOUT, false, hand, true)) return;
      if (eventParse(line, " said, "          , 0, 1, Consts.ACTION_NONE, true, hand, false)) return;
      if (Consts.saveTableJoinEvents) {
        if (eventParse(line, " has returned"    , 0, 2, Consts.ACTION_SITTINGIN, false, hand, true)) return;
        if (eventParse(line, " leaves the table", 0, 2, Consts.ACTION_LEAVETABLE, false, hand, true)) return;
        if (eventParse(line, " joins the table" , 0, 2, Consts.ACTION_JOINTABLE, false, hand, true)) return;
      }
    }
  }

  static boolean eventParse(String line, String actionStr, int namePosType, int amountPosType, byte event, boolean hasAmount, RawPokerHandSaver hand, boolean addAction) {
    int k = line.indexOf(actionStr);
    if (k < 0) return false;
    try {
      String name = "";
      if (namePosType == 0) name = line.substring(0, k);
      if (namePosType == 1) {
        int irto = line.indexOf("returned to");
        if (irto < 0) { Logger.writeToErrors("Unrecognized player name in return uncalled bet, line <" + line + ">", hand); return true; }
        name = line.substring(irto + 12, line.length());
      }
      String amounts = ""; double amount = -1;
      if (hasAmount && amountPosType == 3) {  // collected $
        int k2 = line.indexOf(" from main pot");
        if (k2 < 0) k2 = line.indexOf(" from side pot");
        if (k2 < 0) k2 = line.indexOf(" from pot");
        amounts = line.substring(k + actionStr.length(), k2);
        amount = Double.parseDouble(amounts);
      }
      if (hasAmount && amountPosType == 4) {  //Uncalled bet ($
        int k2 = line.indexOf(")");
        amounts = line.substring(k + actionStr.length(), k2);
        amount = Double.parseDouble(amounts);
      }
      if (hasAmount && amountPosType == 1) {
        amounts = line.substring(k + actionStr.length() + 1, line.length()-1);
        amount = Logger.writeToChat(PlayerIndex.getPlayerId(name, line, hand) + ": " + amounts);
        if (!Consts.saveChatEvents) addAction = false;
      }
      //if (event == Consts.ACTION_SAID)
      // System.out.println("adding won, player = " + name + ", amounts = " + amounts + ", amount = " + amount);
      if (addAction) {
        int playerid = PlayerIndex.getPlayerId(name, line, hand);
        Action a = new Action(hand, playerid, name, line, event, amount);
        hand.addAction(a);
      }
    } catch (Exception e) {
      Logger.writeToErrors("Error parsing for \'" + actionStr + "\' amount, line = <" + line + ">, k = " + k, hand);
    }
    return true;
  }

  // Common routines, bring them in common parent class
  public static String gets(String s, String str1, String str2) {
    int k = s.indexOf(str1);
    if (k<0) return "";
    int k1 = s.indexOf(str2, k + str1.length() + 1);
    if (k1<0) return "";
    return s.substring(k + str1.length(), k1);
  }

  // Common routines, bring them in common parent class
  public static String gets(String s, String str1, String str2, boolean useLastIndex) {
    int k = useLastIndex ? s.lastIndexOf(str1) : s.indexOf(str1);
    if (k<0) return "";
    int k1 = s.indexOf(str2, k + str1.length() + 1);
    if (k1<0) return "";
    return s.substring(k + str1.length(), k1);
  }  

  public static int getMD5hash(String str) {
    int n = 0;
    for (int i = 0; i < str.length(); i++) n += str.charAt(i);
    return n;
  }

  static Calendar c = Calendar.getInstance();
  public static int getDate(String date) {
    //2008/06/29 - 18:58:19 (ET)      ---- this is the format of some Stars 2008 hands, need exceptional handling
    if (date.endsWith("(ET)")) {
      int k = date.indexOf(" - ");
      if (k>=0) date = date.substring(0, k) + " " + date.substring(k+3, date.length());
      if (date.endsWith("(ET)")) date = date.substring(0, date.length()-4) + "ET";
      //System.out.println(date);
    }
    //---------
    //This is the desired format that is parsed: //2009/01/18 3:00:01 ET
    //System.out.println(date);
    int year = Integer.parseInt(date.substring(0, 4));
    //System.out.println(year);
    int month = Integer.parseInt(date.substring(5, 7));
    //System.out.println(month);
    int day = Integer.parseInt(date.substring(8, 10));
    //System.out.println(day);
    date = date.substring(10, date.length()).trim();
    int hourk = date.indexOf(":");
    String hourStr = date.substring(0, hourk);
    int hour = Integer.parseInt(hourStr);
    //System.out.println(hour);
    int min = Integer.parseInt(date.substring(hourk+1, hourk+3));
    //System.out.println(min);
    int sec = Integer.parseInt(date.substring(hourk+4, hourk+6));
    //System.out.println(sec);
    c.set(year, month, day, hour, min, sec);
    long l = c.getTimeInMillis();
    l = l / 1000; // in seconds
    return (int)l;
  }

}


