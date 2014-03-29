/*
  This code is released under GPL v3.

  @Author: Indiana, CodeD (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910
 */
package pokerai.hhex;

import pokerai.hhex.internal.RawPokerHand;

import java.util.Arrays;

public class Action {
  //--
  RawPokerHand h = null;
  public byte jointbyte = -1;  // econdes playerSeatId + actionType in single byte
  private boolean decoded = false;
  private byte playerSeatId; // seatID
  private byte actionType;  // NOT PERSISTED
  //--
  //byte street; // Not persisted, derived from the context
  private int amount;  // 3 bytes, amount in BigBlinds. -1 if action does not imply amount, e.g. check, fold, call.

  public byte getPlayerSeatId() {
    if (!decoded) decode();
    return playerSeatId;
  }

  public byte getAction() {
    if (!decoded) decode();
    return actionType;
  }

  public int getAmount() {
    if (!decoded) decode();
    if (actionType == Consts.ACTION_SMALLBLIND) return Consts.SB_NORM[h.blinds];
    if (actionType == Consts.ACTION_DEAD_SB) return Consts.SB_NORM[h.blinds];
    if (actionType == Consts.ACTION_BIGBLIND) return 100;
    if (actionType == Consts.ACTION_BOTHBLINDS) return 100 + Consts.SB_NORM[h.blinds];
    if (actionType == Consts.ACTION_CALL && !h.callAmountsCalculated) {
      setAllCallAmounts(h.getPokerHand());
      h.callAmountsCalculated = true;
    }
    // TODO: Bets for limit games are still saved (could be all-in and then there is some amount)
    /*
    if (actionType == Consts.ACTION_BET && h.game == Consts.HOLDEM_FL) if (!h.betAmountsCalculated) {
      setAllBetAmounts(h.getPokerHand()); h.betAmountsCalculated = true;
    } */
    if (amount == -1) return 0;
    return amount;
  }

  public int getAmountWithSign() {
    if (!decoded) decode();
    if (actionType == Consts.ACTION_WON) return getAmount(); else return -getAmount();
  }

  // Used only in HHImport save(). Do NOT use elsewhere.
  public int getAmountAsIs() {
    return amount;
  }

  public void setAmount(int amount) {
    //if (amount >= 16777216) System.out.println("DEBUG: Amount = " + amount); // useless here, as this is called only on read (of 3 bytes)
    this.amount = amount;
  }

  private void decode() {
    playerSeatId = RawPokerHand.byte1(jointbyte);
    actionType = RawPokerHand.byte2(jointbyte);
    decoded = true;
  }

  public Action(RawPokerHand hand) { h = hand; }

  // used only when parsing hands
  public Action(RawPokerHand h, int playerId, String playerName, String line, byte action, double amountDouble) {
    if (playerName.length() > Consts.MAX_PLAYERNAME_LENGTH) { Logger.writeToErrors("ERROR: Too long player name (1): <" + playerName + ">, line: <unknown>", h);  return; }
    //int playerId = PlayerIndex.getPlayerId(playerName, line, h);
    this.playerSeatId = h.getSeatId(playerId, playerName);
    //this.street = h.lastround;
    this.actionType = action;
    this.jointbyte = RawPokerHand.twoHalfBytes(playerSeatId, action);
    if (amountDouble >= 0) {
      amount = (int)(Math.round(((amountDouble*100.0) / h._BB) * 100));
      if (amount >= Short.MAX_VALUE) h.allActionsShort = false;
      if (amount >= 16777216) System.out.println("DEBUG: Amount to big = " + amount); 
      //System.out.println("Action " + (playerSeatId + 1) + "(" + playerName + ")" + " "+ Consts.actions[action] + ", Amount saved = " + amount + ", amountd = " + amountDouble);
      //System.out.println(amountDouble + " --> " + ((amountDouble*100.0) / h.BB)  + " --> " + amount);
    } else {
      if (Consts.isVPIPAction(action)) {
        Logger.writeToErrors("ERROR: Bet/Raise/Call action without amount! line <" + line + ">", h);
        return;
      }
      amount = -1;
    }
    //if (actionType == Consts.ACTION_CALL && amount < h._SB) {
    //  System.out.println("ERROR: Call with less than SB, line <"  + line + ">, gameid = " + h.getGameId());   // not usable, happens often people are all-in with < SB
    //}
    decoded = true;
  }

  /*
    Calculates the call amounts for all hands, as they are not save during importing hands
   */
  boolean alreadyIn = false;
  private static int errors = 0;
  private void setAllCallAmounts(Action[] actions, PokerHand hand, int[] remainingStacks, String ph) {
    if (alreadyIn) return;
    if (actions == null) return;
    alreadyIn = true;
    int currentBettingAmount = 0;
    int[] bets = new int[hand.getNumberOfSeats()];
    int[] specialSubtracts = new int[hand.getNumberOfSeats()];
    for (int i = 0; i < actions.length; i++) {
      byte seat = actions[i].getPlayerSeatId();
      byte act = actions[i].getAction();
      if (seat == Consts.PLAYERID_UNKNOWN) continue;
      if (act == Consts.ACTION_CALL) {
        int amountToCall = Math.min(currentBettingAmount - bets[seat], remainingStacks[seat] - bets[seat] - specialSubtracts[seat]);
        if (Consts.saveCallAmounts) {
          if (Math.abs(amountToCall - actions[i].amount) > 3) {
            errors++;
            Logger.writeToErrors("Different calling amount ATC " + amountToCall + " BETS: " + bets[seat]
                    + " REAL:" + actions[i].amount + " CURBET: " + currentBettingAmount + " in phase " + ph + " Player: PId" + hand.getPlayerIDs()[seat]
                    + Consts.enter + hand.toString(), null, true);
            return;
          }
        }
        bets[seat] += amountToCall;
        actions[i].setAmount(amountToCall);
      } else if (act == Consts.ACTION_BET || act == Consts.ACTION_RAISE) {
        bets[seat] = actions[i].getAmount();
        currentBettingAmount = bets[seat];
      } else if (actions[i].getAction() == Consts.ACTION_SMALLBLIND) {
        bets[seat] = Consts.SB_NORM[h.blinds];
        currentBettingAmount = Math.max(Consts.SB_NORM[h.blinds], currentBettingAmount);
      } else if (act == Consts.ACTION_BIGBLIND) {
        bets[seat] = 100;
        currentBettingAmount = 100;
      } else if (act == Consts.ACTION_BOTHBLINDS) {
        bets[seat] = 100;
        remainingStacks[seat] -= Consts.SB_NORM[h.blinds];
        currentBettingAmount = 100;
      } else if (act == Consts.ACTION_DEAD_SB) {
        bets[seat] = 0;
        specialSubtracts[seat] = Consts.SB_NORM[h.blinds];
      }
    }
    for (int i = 0; i < remainingStacks.length; i++) {
      remainingStacks[i] -= bets[i];
      remainingStacks[i] -= specialSubtracts[i];
    }
    alreadyIn = false;
  }

  private void setAllCallAmounts(PokerHand hand) {
    int[] remainingStacks = Arrays.copyOf(hand.getStacks(), hand.getNumberOfSeats());
    setAllCallAmounts(hand.aactionsPreflop(), hand, remainingStacks, "PreFlop");
    setAllCallAmounts(hand.aactionsFlop(), hand, remainingStacks, "Flop");
    setAllCallAmounts(hand.aactionsTurn(), hand, remainingStacks, "Turn");
    setAllCallAmounts(hand.aactionsRiver(), hand, remainingStacks, "River");
  }

  /*
  boolean alreadyIn2 = false;
  private void setAllBetAmounts(Action[] actions, PokerHand hand, int betAmount, String ph) {
    if (alreadyIn2) return;
    if (actions == null) return;
    alreadyIn2 = true;
    for (int i = 0; i < actions.length; i++) {
      if (actions[i].getAction() == Consts.ACTION_BET) {
        if (Consts.saveLimitBetAmounts) {
          if (Math.abs(betAmount - actions[i].amount) > 3) {
            errors++;
            Logger.writeToErrors("Different bet amount AMC " + betAmount + " REAL:" + actions[i].amount + " in phase " + ph +
                    Consts.enter + hand.toString(), null, true);
            return;
          }
        }
        actions[i].setAmount(betAmount);
      }
    }
  }

  private void setAllBetAmounts(PokerHand hand) {
    //setAllBetAmounts(hand.aactionsPreflop(), hand, 0, "PreFlop");
    setAllBetAmounts(hand.aactionsFlop(), hand, 100, "Flop");
    setAllBetAmounts(hand.aactionsTurn(), hand, 200, "Turn");
    setAllBetAmounts(hand.aactionsRiver(), hand, 200, "River");
  }
  */

}

