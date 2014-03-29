/*
  This code is released under GPL v3.

  @Author: Indiana (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910
 */
package pokerai.hhex.helper;

import pokerai.hhex.Action;
import pokerai.hhex.Consts;
import pokerai.hhex.PokerHand;

public class HandHelper {

  PokerHand hand = null;
  int playerID = 0;
  byte seatHero = 0;

  public void setHand(PokerHand handd, int playerid) {
    hand = handd;
    playerID = playerid;
    seatHero = handd.getSeat(playerid);
  }

  // Returns relative or absolute distance to button
  // Hands:  128 124 115 113 108 109 109 115 117 0 0  (absolute distance)
  // Hands:  128 128 125 122 125 125 121 102 62 0 0   (relative distance, skipping empty seats)
  public byte getDistanceToButton(boolean relative) {
    byte dist = 0, curr = seatHero;
    int[] stacks = hand.getStacks();
    while (curr != hand.getButton()) {
      curr++;
      if (curr >= hand.getNumberOfSeats()) curr = 0;
      if (relative) {
        if (stacks[curr]>0) dist++; // not sitting out, etc.
      } else dist++;
      if (curr == seatHero) { dist = 10; break; }
    }
    //System.out.println("seat = "  +seatHero + ", button = "+ hand.getButton() + ", dist = " + dist);
    return dist;
  }

  // Null, if for some reason there is no first action preflop!
  public Action getFirstActionPreflop() {
    Action[] a = hand.aactionsPreflop();
    if (a == null) return null; // No preflop actions (buggy hand) -> no first action preflop
    for (int i = 0; i < a.length; i++) if (a[i].getPlayerSeatId() == seatHero) return a[i];
    return null;
  }


  public String getActionSequence(Action act) {
    if (act == null) return "x";
    Action[] a = hand.aactionsPreflop();
    if (a == null) return null; // No preflop actions (buggy hand) -> no first action preflop
    String s = "";
    for (int i = 0; i < a.length; i++) {
      if (a[i].equals(act)) return s;
      if (a[i].getAction() == Consts.ACTION_BET) s += "r";
      if (a[i].getAction() == Consts.ACTION_RAISE) s += "r";
      //if (a[i].getAction() == Consts.ACTION_ALLIN) s += "a";
      if (a[i].getAction() == Consts.ACTION_CALL) s += "c";
    }
    return s;
  }

  public static String removeSuits(String hand) {
    if (hand.length() != 4) return "";
    if (hand.charAt(1) == hand.charAt(3)) return sortHand(hand.charAt(0), hand.charAt(2)) + "s";
      else return sortHand(hand.charAt(0), hand.charAt(2));
  }

  public static String sortHand(char c1, char c2) {
    if (c1 < c2) return c2 + "" + c1;
     else return c1 + "" + c2;
  }

}

