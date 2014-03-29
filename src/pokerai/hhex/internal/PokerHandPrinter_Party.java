/*
  This code is released under GPL v3.

  @Author: Indiana (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910
 */
package pokerai.hhex.internal;

import pokerai.hhex.*;
import java.util.Vector;

import java.util.Date;


public class PokerHandPrinter_Party {

  public static String enter = "\r\n";

  static Vector winEvents = new Vector();

  public static String printHand(PokerHand hand) {
    winEvents.clear();
    String shand = "";
    double bb = hand.bigBlind() / 100.0;
    double sb = hand.smallBlind() / 100.0;
    int numberOfSeats = hand.getNumberOfSeats();
    int[] stacks = hand.getStacks();
    int[] _players = hand.getPlayerIDs();
    String[] players = new String[numberOfSeats];
    for (int i = 0; i < players.length; i++) players[i] = "PId" + _players[i];
    String date = new Date(hand.getDate()).toString();
    // Reversing blinds to Big Bets for limit games
    String dumpBlinds = "$" + blindsNorm(bb*100);
    if (hand.getGameType() == Consts.HOLDEM_FL) {
      int[] r = Consts.getBlindsRowLB(hand.smallBlind(), hand.bigBlind());
      if (r == null) {
        System.out.println("ERROR: PokerHandPrinter/ Big Bets not found in transition from limit blinds, bb = " + hand.bigBlind() + ", sb = " + hand.smallBlind());
      } else {
        dumpBlinds = "$" + blindsNorm(r[1]);
      }
    }
    // #Game No : 141945403
    // ***** Hand History for Game 141945403 *****
    shand += "#Game No : " + hand.getGameID() + enter;
    shand += "***** Hand History for Game " + hand.getGameID() + " *****" + enter;
    /*
    $25 NL Texas Hold'em - Tuesday, August 21, 18:10:26 ET 2007
    Table East India (Real Money)
    Seat 10 is the button
    Total number of players : 9
     */
    shand += dumpBlinds + " " + Consts.game_party[hand.getGameType()] + " - " + date + enter;
    shand += "Table " + hand.getTableID() + " (Real Money)" + enter;
    shand += "Seat " + (hand.getButton()+1) + " is the button" + enter;
    shand += "Total number of players : " + numberOfSeats + enter;

    // Seat 10: pitbossn ( $23.5 )        --- skip non-existing seats
    // Party: all seats must be present
    boolean[] seatOut = new boolean[numberOfSeats];
    for (int i = 0; i < numberOfSeats; i++) if (stacks[i] > 0) {
      shand += "Seat " + (i+1) + ": " + players[i] + " ( $" + inDollars(stacks[i], bb) + " )" + enter;
    } else {
      shand += "Seat " + (i+1) + ": pidxx" + i + " ( $" + inDollars(10000, bb) + " )" + enter;   // put fake seated player
      seatOut[i] = true;
    }
    
    // Tschlo posts small blind [$0.12].
    // moglet1 posts big blind [$0.25].
    // cronbag posts big blind + dead [$0.37].
    Action[] pa = hand.aactionsPreflop();
    if (pa == null) pa = new Action[0];
    for (int i = 0; i < pa.length; i++) {
      byte action = pa[i].getAction();
      byte seatId = pa[i].getPlayerSeatId();
      String name = "UNKNOWN";
      if (seatId < players.length) name = players[seatId];
      if (action == Consts.ACTION_SMALLBLIND) shand += name + " posts small blind [$" + blindsNorm(sb) + "]."+ enter;
      if (action == Consts.ACTION_BIGBLIND) shand += name + " posts big blind [$" + blindsNorm(bb) + "]."+ enter;
      if (action == Consts.ACTION_DEAD_SB) shand += name + " posts small blind [$" + blindsNorm(sb) + "]."+ enter;
      if (action == Consts.ACTION_BOTHBLINDS) shand += name + " posts big blind + dead [$" + blindsNorm(bb+sb) + "]."+ enter;
    }
    shand += "** Dealing down cards **" + enter;
    shand += doRound(hand, pa, players, bb);
    byte[] board = hand.getCommunityCardsBA();
    // *** FLOP *** [Qc 2d 9c]
    // *** TURN *** [Qc 2d 9c] [3h]
    // *** RIVER *** [Qc 5h 7c 2c] [Ah]
    if (board != null) if (board[0] != Consts.INVALID_CARD) {
      String flop = "[ " + Consts.printCard(board[0]) + ", " + Consts.printCard(board[1]) + ", " + Consts.printCard(board[2]);
      shand += "** Dealing Flop ** " + flop + " ]" + enter;
      shand += doRound(hand, hand.aactionsFlop(), players, bb);
      if (board[3] != Consts.INVALID_CARD) {
        shand += "** Dealing Turn ** [ " + Consts.printCard(board[3]) + " ]" + enter;
        shand += doRound(hand, hand.aactionsTurn(), players, bb);
        if (board[4] != Consts.INVALID_CARD) {
          shand += "** Dealing River ** [ " + Consts.printCard(board[4]) + " ]" + enter;
          shand += doRound(hand, hand.aactionsRiver(), players, bb);
          Action[] shdown = hand.aactionsShowdown();
          if (shdown != null) {
            //shand += "*** SHOW DOWN ***" + enter;
            //sarah45: shows [Qh 4h] (a flush, Ace high)
            for (byte i = 0; i < numberOfSeats; i++) if (stacks[i] > 0) {
              String cards = hand.getCards(i);
              if (cards != null) if (!cards.equals("")) {
                // TODO: 1) determine hand strength  2) determine mucked hands
                shand += players[i] + " shows [ " + cards.substring(0, 2) + " " + cards.substring(2, 4) + " ]. " + enter;
              }
            }
            shand += doRound(hand, shdown, players, bb);
          }
        }
      }
    }
    // win events
    for (int i = 0; i < winEvents.size(); i++) {
      shand += ((String)winEvents.elementAt(i));
    }
    shand += enter;
    return shand;
  }

  public static String doRound(PokerHand hand, Action[] pa, String[] players, double bb) {
    if (pa == null) return "";
    // TODO: illinichet: raises $2 to $4; Seven Curses: raises $191.55 to $243.55 and is all-in
    // fesar: folds
    // Seven Curses: calls $3
    // Seven Curses: checks
    // illinichet: bets $4
    // Seven Curses collected $24.75 from pot
    String shand = "";
    for (int i = 0; i < pa.length; i++) {
      byte action = pa[i].getAction();
      byte seatId = pa[i].getPlayerSeatId();
      String name = "UNKNOWN";
      if (seatId < players.length) name = players[seatId];
      if (action == Consts.ACTION_CALL) shand += name + " calls [$" + inDollars(pa[i].getAmount(), bb) + "]."+ enter;
      if (action == Consts.ACTION_BET) shand += name + " bets [$" + inDollars(pa[i].getAmount(), bb) + "]."+ enter;
      if (action == Consts.ACTION_FOLD) shand += name + " folds." + enter;
      if (action == Consts.ACTION_CHECK) shand += name + " checks." + enter;
      // PARTY SEMANTICS is RAISING BY, not RAISING TO; i.e. raiseTo minus money alreadyIn
      if (action == Consts.ACTION_RAISE) {
        long inPot = pa[i].getAmount() - hand.internal_getAmountInPot(pa, seatId, i);
        //shand += name + " raises [$" + inDollars(inPot, bb) + "].     // "+ inDollars(pa[i].getAmount(), bb) + enter;
        shand += name + " raises [$" + inDollars(inPot, bb) + "]. " + enter;
      }
//      if (action == Consts.ACTION_ALLIN) shand += name + ": raises to $" + inDollars(pa[i].getAmount(), bb) + " and is all-in" + enter;  // All-in event was depricated. TODO: Calculate when player is allin.
      // Party win events must always be at the end (before showdown)
      if (action == Consts.ACTION_WON) {
        winEvents.add(name + " wins $" + inDollars(pa[i].getAmount(), bb) + "." + enter);
      }
      //if (action == Consts.ACTION_LEAVETABLE) shand += name + " leaves the table" + enter;
      //if (action == Consts.ACTION_JOINTABLE) shand += name + " joins the table at seat #" + pa[i].getPlayerSeatId() + enter;
      //if (action == Consts.ACTION_SITTINGOUT) shand += name + ": is sitting out" + enter;
      //if (action == Consts.ACTION_SITTINGIN) shand += name + ": is sitting in" + enter;  // TODO: are there such events at all, and how do the look.
    }
    return shand;
  }

  public static String blindsNorm(double bb) {
    if (bb >= 0.1 && bb < 1) {
      String s = bb + "";
      if (s.length() < 4) s += "0";
      return s;
    }
    if (bb >= 1) return new Integer((int)bb).toString();
    return "--";
  }

  public static String inDollars(long amount, double bb) {
    String res = ds((amount / 100.0) * bb) + "";
    while (res.endsWith("0")) res = res.substring(0, res.length() - 1);
    if (res.endsWith(".")) res = res.substring(0, res.length() - 1);
    return res; 
  }

  public static double ds(double x) { return Math.round(x*100)/100.0; }

/*
#Game No : 141945403
***** Hand History for Game 141945403 *****
$25 NL Texas Hold'em - Tuesday, August 21, 18:10:26 ET 2007
Table East India (Real Money)
Seat 10 is the button
Total number of players : 9
Seat 10: pitbossn ( $23.5 )
Seat 2: Tschlo ( $4.01 )
Seat 3: moglet1 ( $28.63 )
Seat 4: acedaced ( $19.45 )
Seat 5: cronbag ( $38.79 )
Seat 6: josten12 ( $5.4 )
Seat 7: LaszloB1 ( $11.62 )
Seat 8: bicdye7 ( $24.72 )
Seat 9: Gekas22 ( $9.56 )
Tschlo posts small blind [$0.12].
moglet1 posts big blind [$0.25].
** Dealing down cards **
acedaced calls [$0.25].
cronbag folds.
josten12 folds.
LaszloB1 raises [$1.25].
bicdye7 folds.
Gekas22 calls [$1.25].
pitbossn calls [$1.25].
Tschlo folds.
moglet1 folds.
acedaced calls [$1].
** Dealing Flop ** [ 2h, 8s, Th ]
acedaced bets [$1].
LaszloB1 raises [$3.5].
Gekas22 folds.
pitbossn folds.
acedaced calls [$2.5].
** Dealing Turn ** [ Qd ]
acedaced bets [$1.25].
LaszloB1 calls [$1.25].
** Dealing River ** [ Jh ]
acedaced bets [$13.45].
LaszloB1 calls [$5.62].
acedaced shows [ Js 9s ].
LaszloB1 shows [ Jc Jd ].
acedaced shows [ Js 9s ].
acedaced wins $24.81.
acedaced wins $7.83.
 */

}


