/*
  This code is released under GPL v3.

  @Author: Indiana (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910
 */
package pokerai.hhex.internal;

import pokerai.hhex.Action;
import pokerai.hhex.Consts;
import pokerai.hhex.PokerHand;

import java.util.Date;


public class PokerHandPrinter {

  public static String enter = "\r\n";

  public static String printHand(PokerHand hand) {
    String shand = "";
    double bb = hand.bigBlind() / 100.0;
    double sb = hand.smallBlind() / 100.0;
    int numberOfSeats = hand.getNumberOfSeats();
    int[] stacks = hand.getStacks();
    int[] _players = hand.getPlayerIDs();
    String[] players = new String[numberOfSeats];
    for (int i = 0; i < players.length; i++) players[i] = "PId" + _players[i];
    // TODO: Date
    String date = new Date(hand.getDate()).toString();
    // Reversing blinds to Big Bets for limit games
    String dumpBlinds = blindsNorm(sb) + "/$" + blindsNorm(bb);
    if (hand.getGameType() == Consts.HOLDEM_FL) {
      int[] r = Consts.getBlindsRowLB(hand.smallBlind(), hand.bigBlind());
      if (r == null) {
        System.out.println("ERROR: PokerHandPrinter/ Big Bets not found in transition from limit blinds, bb = " + hand.bigBlind() + ", sb = " + hand.smallBlind());
      } else {
        dumpBlinds = blindsNorm(r[0]/100.0) + "/$" + blindsNorm(r[1]/100.0);
      }
    }
    // PokerStars Game #24481522335:  Hold'em No Limit ($1/$2) - 2009/01/31 22:01:10 ET
    shand += "PokerAI_HHDb Game #" + hand.getGameID() + ": " + Consts.game[hand.getGameType()]
           + " ($" + dumpBlinds + ") - " + date + enter;
    // Table 'Medea' 9-max Seat #4 is the button
    shand += "Table \'" + hand.getTableID() + "\' " + numberOfSeats + "-max Seat #" + hand.getButton() + " is the button" + enter;
    // Seat 1: fesar ($174 in chips)        --- skip non-existing seats
    for (int i = 0; i < numberOfSeats; i++) if (stacks[i] > 0) {
      shand += "Seat " + i + ": " + players[i] + " ($" + inDollars(stacks[i], bb) + " in chips)" + enter;
    }
    // Seven Curses: posts small blind $1
    // alih99999: posts big blind $2
    Action[] pa = hand.aactionsPreflop();
    if (pa == null) pa = new Action[0];
    for (int i = 0; i < pa.length; i++) {
      byte action = pa[i].getAction();
      byte seatId = pa[i].getPlayerSeatId();
      String name = "UNKNOWN";
      if (seatId < players.length) name = players[seatId];
      if (action == Consts.ACTION_SMALLBLIND) shand += name + ": posts small blind $" + blindsNorm(sb) + enter;
      if (action == Consts.ACTION_BIGBLIND) shand += name + ": posts big blind $" + blindsNorm(bb) + enter;
      if (action == Consts.ACTION_DEAD_SB) shand += name + ": posts small blind $" + blindsNorm(sb) + enter;
      if (action == Consts.ACTION_BOTHBLINDS) shand += name + ": posts small & big blinds $" + blindsNorm(bb+sb) + enter;
    }
    shand += "*** HOLE CARDS ***" + enter;
    shand += doRound(pa, players, bb);
    byte[] board = hand.getCommunityCardsBA();
    // *** FLOP *** [Qc 2d 9c]
    // *** TURN *** [Qc 2d 9c] [3h]
    // *** RIVER *** [Qc 5h 7c 2c] [Ah]
    if (board != null) if (board[0] != Consts.INVALID_CARD) {
      String flop = "[" + Consts.printCard(board[0]) + " " + Consts.printCard(board[1]) + " " + Consts.printCard(board[2]);
      shand += "*** FLOP *** " + flop + "] " + enter;
      shand += doRound(hand.aactionsFlop(), players, bb);
      if (board[3] != Consts.INVALID_CARD) {
        shand += "*** TURN *** " + flop + "] [" + Consts.printCard(board[3]) + "]" + enter;
        shand += doRound(hand.aactionsTurn(), players, bb);
        if (board[4] != Consts.INVALID_CARD) {
          shand += "*** RIVER *** " + flop + " " + Consts.printCard(board[3]) + "] [" + Consts.printCard(board[4]) + "]" + enter;
          shand += doRound(hand.aactionsRiver(), players, bb);
          Action[] shdown = hand.aactionsShowdown();
          if (shdown != null) {
            shand += "*** SHOW DOWN ***" + enter;
            //sarah45: shows [Qh 4h] (a flush, Ace high)
            for (byte i = 0; i < numberOfSeats; i++) if (stacks[i] > 0) {
              String cards = hand.getCards(i);
              if (cards != null) if (!cards.equals("")) {
                // TODO: 1) determine hand strength  2) determine mucked hands
                shand += players[i] + ": shows [" + cards.substring(0, 2) + " " + cards.substring(2, 4) + "] (a XXX)" + enter;
              }
            }
            shand += doRound(shdown, players, bb);
          }
        }
      }
    }
    shand += "*** SUMMARY ***" + enter;
    shand += "Total pot $XXX | Rake $XXX" + enter; // TODO: Pot and Rake
    for (byte i = 0; i < numberOfSeats; i++) if (stacks[i] > 0) {
      shand += "Seat " + i + ": " + players[i] + " hand balance is ($" + inDollars(hand.getMoneyMade(i), bb) + ")" + enter;
    }
    // Seat 1: elslugo folded before Flop
    // Seat 4: ex-superhero folded before Flop (didn't bet)
    // Seat 9: illinichet (button) folded before Flop (didn't bet)
    // Seat 3: cajunfury (big blind) folded on the Turn
    shand += enter;
    return shand;
  }

  public static String doRound(Action[] pa, String[] players, double bb) {
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
      if (action == Consts.ACTION_CALL) shand += name + ": calls $" + inDollars(pa[i].getAmount(), bb) + enter;
      if (action == Consts.ACTION_BET) shand += name + ": bets $" + inDollars(pa[i].getAmount(), bb) + enter;
      if (action == Consts.ACTION_FOLD) shand += name + ": folds" + enter;
      if (action == Consts.ACTION_CHECK) shand += name + ": checks" + enter;
      if (action == Consts.ACTION_RAISE) shand += name + ": raises to $" + inDollars(pa[i].getAmount(), bb) + enter;
//      if (action == Consts.ACTION_ALLIN) shand += name + ": raises to $" + inDollars(pa[i].getAmount(), bb) + " and is all-in" + enter;  // All-in event was depricated. TODO: Calculate when player is allin.
      if (action == Consts.ACTION_WON) {
        shand += name + " collected $" + inDollars(pa[i].getAmount(), bb) + "" + enter;  // TODO: This is either return of uncalled bet, or won money from Pot
      }
      if (action == Consts.ACTION_LEAVETABLE) shand += name + " leaves the table" + enter;
      if (action == Consts.ACTION_JOINTABLE) shand += name + " joins the table at seat #" + pa[i].getPlayerSeatId() + enter;
      if (action == Consts.ACTION_SITTINGOUT) shand += name + ": is sitting out" + enter;
      if (action == Consts.ACTION_SITTINGIN) shand += name + ": is sitting in" + enter;  // TODO: are there such events at all, and how do the look.
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
PokerStars Game #24481522335:  Hold'em No Limit ($1/$2) - 2009/01/31 22:01:10 ET
Table 'Medea' 9-max Seat #4 is the button
Seat 1: fesar ($174 in chips) 
Seat 2: madpanther ($191.20 in chips) 
Seat 4: ex-superhero ($201 in chips) 
Seat 5: Seven Curses ($210.85 in chips) 
Seat 6: alih99999 ($203 in chips) 
Seat 7: whitespur ($207.30 in chips) 
Seat 8: QueenOvettes ($203 in chips) 
Seat 9: illinichet ($227.80 in chips) 
Seven Curses: posts small blind $1
alih99999: posts big blind $2
*** HOLE CARDS ***
whitespur: folds 
QueenOvettes: folds 
illinichet: raises $2 to $4
fesar: folds 
madpanther: folds 
ex-superhero: folds 
Seven Curses: calls $3
alih99999: folds 
*** FLOP *** [Qc 2d 9c]
Seven Curses: checks 
illinichet: bets $4
Seven Curses: calls $4
*** TURN *** [Qc 2d 9c] [3h]
Seven Curses: checks 
illinichet: bets $4
Seven Curses: calls $4
*** RIVER *** [Qc 2d 9c 3h] [7s]
Seven Curses: checks 
illinichet: checks 
*** SHOW DOWN ***
Seven Curses: shows [Qd Kc] (a pair of Queens)
illinichet: mucks hand 
Seven Curses collected $24.75 from pot
*** SUMMARY ***
Total pot $26 | Rake $1.25 
Board [Qc 2d 9c 3h 7s]
Seat 1: fesar folded before Flop (didn't bet)
Seat 2: madpanther folded before Flop (didn't bet)
Seat 4: ex-superhero (button) folded before Flop (didn't bet)
Seat 5: Seven Curses (small blind) showed [Qd Kc] and won ($24.75) with a pair of Queens
Seat 6: alih99999 (big blind) folded before Flop
Seat 7: whitespur folded before Flop (didn't bet)
Seat 8: QueenOvettes folded before Flop (didn't bet)
Seat 9: illinichet mucked


Seven Curses: raises $191.55 to $243.55 and is all-in
ex-superhero: calls $148 and is all-in
Uncalled bet ($43.55) returned to Seven Curses

--
Uncalled bet ($5) returned to Seven Curses
Seven Curses collected $5 from pot
Seven Curses is sitting out

---
*** SHOW DOWN ***
sarah45: shows [Qh 4h] (a flush, Ace high)
nine10suit3d: shows [2c Tc] (a full house, Deuces full of Tens)
nine10suit3d collected $59.50 from pot

 */

}


