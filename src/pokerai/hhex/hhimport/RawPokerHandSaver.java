/*
  This code is released under GPL v3.

  @Author: Indiana (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910
 */
package pokerai.hhex.hhimport;

import pokerai.hhex.Action;
import pokerai.hhex.Consts;
import pokerai.hhex.Logger;
import pokerai.hhex.PokerHand;
import pokerai.hhex.internal.RawPokerHand;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Vector;

public class RawPokerHandSaver extends RawPokerHand {

  public boolean handComplete = false; // must be set to TRUE during parsing to denote that the hand is parsed completely

  // Actions - used during parsing for saving. These are initialized from the parser (if actions == null)
  private Vector actions         = null; // new Vector();   // NOT PERSISTED
  private Vector actionsPreflop  = null; // new Vector();
  private Vector actionsFlop     = null; // new Vector();
  private Vector actionsTurn     = null; // new Vector();
  private Vector actionsRiver    = null; // new Vector();
  private Vector actionsShowdown = null; // new Vector();

  public byte lastround = Consts.STREET_UNKNOWN;

  public void addAction(Action a) {
    if (actions == null) {
      actions = new Vector();
      actionsPreflop = new Vector();
      actionsFlop = new Vector();
      actionsTurn = new Vector();
      actionsRiver = new Vector();
      actionsShowdown = new Vector();
    }
    actions.add(a);
    if (lastround == Consts.STREET_UNKNOWN) if (actionsPreflop.size() < Consts.MAX_ACTIONS) actionsPreflop.add(a);
    if (lastround == Consts.STREET_PREFLOP) if (actionsPreflop.size() < Consts.MAX_ACTIONS) actionsPreflop.add(a);
    if (lastround == Consts.STREET_FLOP) if (actionsFlop.size() < Consts.MAX_ACTIONS) actionsFlop.add(a); //System.out.println("adding flop hand");
    if (lastround == Consts.STREET_TURN) if (actionsTurn.size() < Consts.MAX_ACTIONS) actionsTurn.add(a);
    if (lastround == Consts.STREET_RIVER) if (actionsRiver.size() < Consts.MAX_ACTIONS) actionsRiver.add(a);
    if (lastround == Consts.STREET_SHOWDOWN) if (actionsShowdown.size() < Consts.MAX_ACTIONS) actionsShowdown.add(a);
  }

  private byte getBlindIndex() {
    for (byte i = 0; i < Consts.BLINDS.length; i++) if (_SB == Consts.BLINDS[i][0] && _BB == Consts.BLINDS[i][1]) return i;
    System.out.println("ERROR: Blinds index not found, SB = " + _SB + ", BB = " + _BB);
    return 0;
  }

  public void verifyHandOnImport1() {
    if (numberOfSeats <= 0 || numberOfSeats > 10) {
      Logger.writeTo0pl("ERROR: Wrong number of seats: " + numberOfSeats, this);
      return;
    }
    if (!handComplete) {
      Logger.writeTo0pl("ERROR: Incomplete (truncated) hand, handComplete flag was not set! ", this);
      return;
    }
    if (site != Consts.SITE_DEFAULT) {
      Logger.writeToErrors("ERROR: Site different than default/PokerStars, error for now.", this);
      return;
    }
    for (int i = 0; i < numberOfSeats; i++) if (stacks[i] >= 16777216) {
      Logger.writeToErrors("Limitation: Stack size is bigger than 3 bytes, hand is skipped. ", this);
      return;
    }
  }


  public void verifyHandOnImport2(byte[] data) {
    if (Consts.detailedVerificationDuringImport) {
      PokerHand p = load(data);
      int n = p.getNumberOfSeats();
      int[] stacks = p.getStacks();
      // 1 - negative rake
      long[] moneyMade = new long[n];
      long total = 0;
      for (byte i = 0; i < n; i++) {
        moneyMade[i] = p.getMoneyMade(i);
        total += moneyMade[i];
      }
      if (total > 5) {     // more than 0.05 difference
        Logger.writeToErrors("ERROR: Sum of all money made by players is positive => rake is negative, impossible! " + Consts.enter + p.toString(), this, true);
        return;
      }
      // 1b - too much or less raked
      double moneyWon = 0;
      double moneyLost = 0;
      for (byte i = 0; i < n; i++) {
        if (moneyMade[i] > 0) moneyWon += moneyMade[i]; else moneyLost += (-moneyMade[i]);
      }
      double rake = moneyLost - moneyWon;
      rake = rake / moneyWon;
      //System.out.println("rake = " + rake + " mw " + moneyWon + ", ml " + moneyLost);
      // 2 - lost more money than stack
      for (byte i = 0; i < n; i++) if (moneyMade[i] < 0) {
        if (stacks[i]+100 < -moneyMade[i]) {        // TODO: +100 to tolerate a small error. Not clear why we have these small error at all?
          Logger.writeToErrors("ERROR: Player " + i + " lost more money than his initial stack!" + Consts.enter + p.toString(), this, true);
          return;
        }
      }
      // 3 - First round call actions bigger than BB
      Action[] a = p.aactionsPreflop();
      if (a != null) for (int i = 0; i < a.length; i++) {
        byte b = a[i].getAction();
        if (b != Consts.ACTION_FOLD) {
          if (b != Consts.ACTION_CALL) break;
          if (a[i].getAmount() > p.bigBlind()) {   // TODO: Replace here bigBlind with 100
            Logger.writeToErrors("ERROR: First call is more than the BB -> impossible!" + Consts.enter + p.toString(), this, true);
            return;
          }
        }
        // 4 - play action with unknown playerSeatId
        if (Consts.isPlayActionBySeatedPlayer(b)) {
          if (a[i].getPlayerSeatId() == Consts.SEATID_UNKNOWN) {
            Logger.writeToErrors("ERROR: Play action by seated player with seatId=12 (SEATID_UNKNOWN) recorded!" + Consts.enter + p.toString(), this, true);
            return;
          }
        }
      }

    }
  }
  


  // ***********************************************************************************************************************
  // Load / Save
  // ***********************************************************************************************************************
  public byte[] save() {
    try {
      ByteArrayOutputStream b = new ByteArrayOutputStream(100);
      DataOutputStream d = new DataOutputStream(b);
      d.writeByte(site);
      if (persistGameId) d.writeLong(gameId);
      d.writeByte(game);
      d.writeInt(date);
      d.writeInt(tableId);
      d.writeByte(numberOfSeats);
      d.writeByte(getBlindIndex());       //d.writeInt(BB); d.writeInt(SB);
      d.writeByte(button);
      for (int i = 0; i < numberOfSeats; i++) { byte[] bp = writeInt3Bytes(players[i]); d.writeByte(bp[0]); d.writeByte(bp[1]); d.writeByte(bp[2]); }
      // Stacks
      boolean allshorts = true;
      for (int i = 0; i < numberOfSeats; i++) if (stacks[i] > Short.MAX_VALUE) { allshorts = false; break; }
      if (!allshorts) for (int i = 0; i < numberOfSeats; i++) if (stacks[i] >= 16777216) { System.out.println("ERROR: Stacks is >3 bytes, must never be here!"); stacks[i] = 16777215; }
      if (allshorts) {
        if (allActionsShort) d.writeByte(Consts.SIZE_STACKS2_ACTIONS2);
                        else d.writeByte(Consts.SIZE_STACKS2_ACTIONS3);
      } else {
        if (allActionsShort) d.writeByte(Consts.SIZE_STACKS3_ACTIONS2);
                        else d.writeByte(Consts.SIZE_STACKS3_ACTIONS3);
      }
      if (allshorts) {
        for (int i = 0; i < numberOfSeats; i++) d.writeShort(stacks[i]);
      } else {
        for (int i = 0; i < numberOfSeats; i++) { byte[] bp = writeInt3Bytes(stacks[i]); d.writeByte(bp[0]); d.writeByte(bp[1]); d.writeByte(bp[2]); }
      }
      // cards for upto 3 players. First byte is -1 for end, or MASK (numberOfCards*10 + seat)
      for (int z = 0; z < maxCardsPx; z++) {
         if (cardsPx[z] == null) { d.writeByte((byte)-1); break; }
         else { for (int i = 0; i < cardsPx[z].length; i++) d.writeByte(cardsPx[z][i]); }
      }
      // board, stop writing when invalid card is reached
      for (int i = 0; i < 5; i++) { d.writeByte(board[i]); if (board[i] == Consts.INVALID_CARD) break; }
      write(d, actionsPreflop);
      write(d, actionsFlop);
      write(d, actionsTurn);
      write(d, actionsRiver);
      write(d, actionsShowdown);
      //System.out.println("---");
      return b.toByteArray();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public void write(DataOutputStream d, Vector actions) throws Exception {
    //System.out.println(actions.size());
    if (actions == null) actions = new Vector(); // should never reach here -> means there were NO actions
    if (actions.size() > Consts.MAX_ACTIONS) System.out.println("ERROR: Too long action sequence!"); 
    d.writeByte(actions.size());
    for (int i = 0; i < actions.size(); i++) {
      Action a = (Action) actions.elementAt(i);
      if (a.getAction() > Consts.actions.length) System.out.println("ERROR: Player action integer is outside actions array boundaries!");
      byte b = twoHalfBytes(a.getPlayerSeatId(), a.getAction());
      d.writeByte(b);
      //if (!Consts.isActionWithoutAmount(a.getAction())) {
      if (!Consts.isActionWithoutAmountEncoded(b)) {
        if (allActionsShort) {
          byte[] bp = writeShort(a.getAmountAsIs());
          d.writeByte(bp[0]); d.writeByte(bp[1]);
        } else {
          byte[] bp = writeInt3Bytes(a.getAmountAsIs());
          d.writeByte(bp[0]); d.writeByte(bp[1]); d.writeByte(bp[2]);
        }
        //d.writeInt(a.getAmountAsIs());
      }
    }
  }

  public static byte[] writeInt3Bytes(int v) {
    byte[] b = new byte[3];
    b[0] = (byte)(0xff & (v >> 16));
    b[1] = (byte)(0xff & (v >> 8));
    b[2] = (byte)(0xff & (v));
    return b;
  }

  public static byte[] writeShort(int v) {
    byte[] b = new byte[2];
    b[0] = (byte)(0xff & (v >> 8));
    b[1] = (byte)(0xff & (v));
    return b;
  }
   
}

