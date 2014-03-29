/*
  This code is released under GPL v3.

  @Author: Indiana, CodeD (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910
 */
package pokerai.hhex;

import pokerai.hhex.internal.*;

public class PokerHand {

  private RawPokerHand r = null;
  private Action[] allActions = null; // All Actions

  public PokerHand(RawPokerHand rg) {
    r = rg;
    r.setPokerHand(this);
  }

  public void loadHeader() { if (!r.headerLoaded) r._loadHeader(); }
  public void load() { if (!r.handLoaded) r._loadGameData(); }   
  public byte[] getRawData() { return r.data; }

  // Header
  public byte getSite() {
    if (!r.headerLoaded) r._loadHeader();
    return r.site;
  }

  public byte getGameType() {
    if (!r.headerLoaded) r._loadHeader();
    return r.game;
  }

  public byte getNumberOfSeats() {
    if (!r.headerLoaded) r._loadHeader();
    return r.getNumberOfSeats();
  }

  // date when the hand was played in milliseconds
  public long getDate() {
    if (!r.headerLoaded) r._loadHeader();
    return ((long)r.date)*1000; // as it is truncated on save
  }

  public int getTableID() {
    if (!r.headerLoaded) r._loadHeader();
    return r.tableId;
  }

  public long getGameID() {
    if (!r.headerLoaded) r._loadHeader();
    return r.getGameId();
  }

  // return the button, from 0 to numberOfSeats-1 
  public byte getButton() {
    if (!r.headerLoaded) r._loadHeader();
    return (byte)(r.button - 1);
  }

  // HandData
  public int[] getStacks() {
    if (!r.handLoaded) r._loadGameData();
    return r.stacks;
  }

  // HandData
  public int[] getPlayerIDs() {
    if (!r.handLoaded) r._loadGameData();
    return r.players;
  }

  public byte getSeat(int playerID) {
    int[] playerIDs = getPlayerIDs();
    for (int i = 0; i < playerIDs.length; i++) if (playerIDs[i] == playerID) return (byte)i;
    return -1;
  }

  public int smallBlind() {
    if (!r.headerLoaded) r._loadHeader();
    return Consts.BLINDS[r.blinds][0];
  }

  // this is the actual Big blind, e.g. 25 for 0.25. Sometimes this is used wrrongly instead of 100 (e.g. stacks are in big blinds) not in money.
  public int bigBlind() {
    if (!r.headerLoaded) r._loadHeader();
    return Consts.BLINDS[r.blinds][1];
  }

  public byte getLimitsIndex() {
    if (!r.headerLoaded) r._loadHeader();
    return r.blinds;
  }

  public Action[] aactionsPreflop() {
    if (!r.handLoaded) r._loadGameData();
    return r.aactionsPreflop;
  }

  public Action[] aactionsFlop() {
    if (!r.handLoaded) r._loadGameData();
    return r.aactionsFlop;
  }
  
  public Action[] aactionsTurn() {
    if (!r.handLoaded) r._loadGameData();
    return r.aactionsTurn;
  }

  public Action[] aactionsRiver() {
    if (!r.handLoaded) r._loadGameData();
    return r.aactionsRiver;
  }

  public Action[] aactionsShowdown() {
    if (!r.handLoaded) r._loadGameData();
    return r.aactionsShowdown;
  }

  public Action[] getAllActions() {
    if (allActions != null) return allActions;
    if (!r.handLoaded) r._loadGameData();
    Action[][] a = new Action[5][];
    a[0] = aactionsPreflop();
    a[1] = aactionsFlop();
    a[2] = aactionsTurn();
    a[3] = aactionsRiver();
    a[4] = aactionsShowdown();
    int l = 0;
    for (int i = 0; i < a.length; i++) if (a[i] != null) l+= a[i].length;
    allActions = new Action[l];
    int n = 0;
    for (int i = 0; i < a.length; i++) if (a[i] != null)
     for (int j = 0; j < a[i].length; j++) { allActions[n] = a[i][j]; n++; }
    return allActions;
  }

  public String getCards(byte seat) {
    if (!r.handLoaded) r._loadGameData();
    for (int i = 0; i < r.cardsPx.length; i++) if (r.cardsPx[i] != null) if (r.cardsPx[i][0] == seat) return Consts.printCardsExcludingFirst(r.cardsPx[i]);
    return "";
  }

  public byte[] getCardsBA(byte seat) {
    if (!r.handLoaded) r._loadGameData();
    for (int i = 0; i < r.cardsPx.length; i++) if (r.cardsPx[i] != null) if (r.cardsPx[i][0] == seat) return r.cardsPx[i]; // Note: The first byte here is the player seat!!!!
    return null;
  }

  public byte[] getCardsOnlyBA(byte seat) {
    byte[] c = getCardsBA(seat); if (c == null) return null;
    byte[] cards = new byte[c.length-1];
    System.arraycopy(c, 1, cards, 0, cards.length);
    return cards;
  }  
 
  public String getCommunityCards() {
    if (!r.handLoaded) r._loadGameData();
    return Consts.printCards(r.board);
  }

  public byte[] getCommunityCardsBA() {
    if (!r.handLoaded) r._loadGameData();
    return r.board;
  }

  // ************************************************************************************************************************
  // ------------------------------------------------------------------------------------------------------------------------
  // Helper methods
  // ------------------------------------------------------------------------------------------------------------------------
  // ************************************************************************************************************************

  // Returns the last round where some action occured (action could be also moneywon).
  // E.g. if two players go allin on the flop, the last round will be STREET_SHOWDOWN, and not STREET_FLOP
  public int getLastRound() {
    if (aactionsShowdown() != null) return Consts.STREET_SHOWDOWN;
    if (aactionsRiver() != null) return Consts.STREET_RIVER;
    if (aactionsTurn() != null) return Consts.STREET_TURN;
    if (aactionsFlop() != null) return Consts.STREET_FLOP;
    return Consts.STREET_PREFLOP;
  }

  // Get amount of money spend during the hand. The amount is in BigBlinds
  public long getMoneyMade(byte seat) {
    long money = 0;
    money += getMoneyMadeForRound(aactionsPreflop(), seat, false);
    money += getMoneyMadeForRound(aactionsFlop(), seat, false);
    money += getMoneyMadeForRound(aactionsTurn(), seat, false);
    money += getMoneyMadeForRound(aactionsRiver(), seat, false);
    money += getMoneyMadeForRound(aactionsShowdown(), seat, false);
    return money;
  }

  public long getMoneySpent(byte seat) {
    long money = 0;
    money += getMoneyMadeForRound(aactionsPreflop(), seat, true);
    money += getMoneyMadeForRound(aactionsFlop(), seat, true);
    money += getMoneyMadeForRound(aactionsTurn(), seat, true);
    money += getMoneyMadeForRound(aactionsRiver(), seat, true);
    money += getMoneyMadeForRound(aactionsShowdown(), seat, true);
    return money;
  }

  // This is buggy: Uncalled bets are attributed as won money
  public long getMoneyMadeForRound(Action[] actions, byte seat, boolean spentMoneyOnly) {
	  if (actions == null) return 0;
	  long money = 0, lastAmount = 0;
	  for (int i = 0; i < actions.length; i++) {
		  byte act = actions[i].getAction();
		  if (actions[i].getPlayerSeatId() == seat) 
			  if (!spentMoneyOnly || act != Consts.ACTION_WON) {
				  //System.out.println(seat + ": " + actions[i].getAmountWithSign());
				  long am = actions[i].getAmountWithSign();  // return -X if we spend X money, or +X if we won some money
				  if (act == Consts.ACTION_RAISE /*|| actions[i].getAction() == Consts.ACTION_ALLIN*/) {
					  money += (am - lastAmount);
					  lastAmount = am;
				  } else if (act == Consts.ACTION_CALL) {
					  money += am;
					  lastAmount = lastAmount + am;
				  } else { // bet, postBB, etc.
					  money += am;
					  if (act == Consts.ACTION_BOTHBLINDS) lastAmount = -100;
					  else if (act == Consts.ACTION_DEAD_SB) lastAmount = 0;
					  else lastAmount = am;
				  }
			  }
	  }
	  return money;
  }

  // Used in party hands printer. If dead SB+BB is posted, only the BB is counted. If dead SB is posted, not counted
  // Implementation almost identical to getMoneyMadeForRound
  public long internal_getAmountInPot(Action[] actions, byte seat, int actionIndex) {
    if (actions == null) return 0;
    long money = 0, lastAmount = 0;
    for (int i = 0; i < actionIndex; i++) {
      byte act = actions[i].getAction();
      if (actions[i].getPlayerSeatId() == seat) if (act != Consts.ACTION_WON) {
        //System.out.println(seat + ": " + actions[i].getAmountWithSign());
        long am = actions[i].getAmountWithSign();  // return -X if we spend X money, or +X if we won some money
        if (act == Consts.ACTION_RAISE /*|| actions[i].getAction() == Consts.ACTION_ALLIN*/) {
          money += (am - lastAmount);
          lastAmount = am;
        } else if (act == Consts.ACTION_CALL) {
          money += am;
          lastAmount = lastAmount + am;
        } else { // bet, postBB, etc.
          money += am;
          if (act == Consts.ACTION_BOTHBLINDS) lastAmount = -100;
          else if (act == Consts.ACTION_DEAD_SB) lastAmount = 0;
               else lastAmount = am;
        }
      }
    }
    return -money;
  }

  // Determines if given player is all-in preflop (double check for corectness)
  // we check if there are no actions on flop, cards were shown, and we made the largest raise or called it
  public boolean isAllInPreflop(byte seat) { return isAllInAndCalledPreflop(seat); }
  public boolean isAllInAndCalledPreflop(byte seat) {
    Action[] a = aactionsPreflop();
    int[] stacks = getStacks();
    if (stacks == null) return false;
    Action[] af = aactionsFlop();
    if (af != null) return false; // there were actions on flop -> can't be preflop all-in
    byte[] h = getCardsOnlyBA(seat);
    if (h == null) return false; // cards were not shown at showdown -> can't be all-in
    //long mspend = getMoneyMadeForRound(aactionsPreflop(), seat, true);
    double largestRaise = 0;
    for (int i = 0; i < a.length; i++) {
      double amount = a[i].getAmount();
      if (amount > largestRaise) largestRaise = amount;
    }
    // Compares if the money spent preflop are more or equal than largest raise made preflop => if yes, plaeyr is all-in
    return (-getMoneyMadeForRound(aactionsPreflop(), seat, true) >= largestRaise);
  }

  public boolean hasJammed(byte seat) {
    Action[] a = aactionsPreflop();
    int[] stacks = getStacks();
    if (stacks == null) return false;
    Action[] af = aactionsFlop();
    if (af != null) return false; // there were actions on flop -> can't be preflop all-in
    //byte[] h = getCardsOnlyBA(seat);
    //if (h == null) return false; // cards were not shown at showdown -> can't be all-in
    //long mspend = getMoneyMadeForRound(aactionsPreflop(), seat, true);
    for (int i = 0; i < a.length; i++) {
      byte psid = a[i].getPlayerSeatId();
      if (psid == seat) {
        byte act  = a[i].getAction();
        if (act == Consts.ACTION_FOLD) return false;
        int am = a[i].getAmount();
        if (act == Consts.ACTION_BET || act == Consts.ACTION_RAISE || act == Consts.ACTION_CALL) {
        // first action
          //System.out.println(am + " " + stacks[seat]);
          if (am + 100 >= stacks[seat]) return true;
            else return false;
        }
      }
    }
    return false;
  }

  public int getVPIPed() {
    Action[] a = aactionsPreflop();
    int n = 0;
    boolean[] acted = new boolean[getNumberOfSeats()];
    for (int i = 0; i < a.length; i++) {
      byte seat = a[i].getPlayerSeatId();
      if (seat == Consts.SEATID_UNKNOWN) continue;
      if (!acted[seat]) if (Consts.isVPIPAction(a[i].getAction())) {
        n++; acted[seat] = true;
      }
    }
    return n;
  }

  // Helper functions: Careful here! -> return of uncalled bets are also win events!
  public boolean isWinner(byte seat) {
    return (getMoneyMade(seat) > 0);
  }


  // Return number of active players at game start (might be redundant with below implementation)
  public byte getNumberOfActivePlayers() {
    byte activePlayers = 0;
    int[] stacks = getStacks();
    for (int i = 0; i < getNumberOfSeats(); i++) if (stacks[i] > 0) activePlayers++;
    return activePlayers;
  }

  // Return number of active players at given streen. More accurate than above, checks if player has made actions
  public int getNumberOfActivePlayers(byte street) {
     switch (street) {
       case Consts.STREET_PREFLOP : return getNumberOfActivePlayers(aactionsPreflop());
       case Consts.STREET_FLOP : return getNumberOfActivePlayers(aactionsFlop());
       case Consts.STREET_TURN : return getNumberOfActivePlayers(aactionsTurn());
       case Consts.STREET_RIVER : return getNumberOfActivePlayers(aactionsRiver());
     }
     return 0;
   }

  private int getNumberOfActivePlayers(Action[] actions) {
    if (actions == null) return 0;
    int c = 0;
    byte lastSeat = -1;
    boolean[] acted = new boolean[getNumberOfSeats()];
    for (int i = 0; i < actions.length; i++) {
       byte seat = actions[i].getPlayerSeatId();
       if (seat != Consts.PLAYERID_UNKNOWN && Consts.isVoluntaryGameAction(actions[i].getAction())) {
          if (!acted[seat]) {
             acted[seat] = true;
             c++;
             lastSeat = seat;
          } else {
             if (lastSeat != seat) { // action returned to a player who already acted (and not consecutively)
                break;            // => no need to check further
             }
          }
       }
    }
    return c;
  }

  /*
    Every bet/raise increases the betting level, preflop the starting betting level is 1, on every other street 0.
    It is a reference how "much" aggressive action was on any street, e.g. a high betting level equals in most cases very strong hands.
  */
  public int getBettingLevel(byte street) {
    switch (street) {
      case Consts.STREET_PREFLOP : return getBettingLevel(aactionsPreflop());
      case Consts.STREET_FLOP : return getBettingLevel(aactionsFlop());
      case Consts.STREET_TURN : return getBettingLevel(aactionsTurn());
      case Consts.STREET_RIVER : return getBettingLevel(aactionsRiver());
    }
    return 0;
  }

  private int getBettingLevel(Action[] actions) {
    if(actions == null) return 0;
    int bettingLevel = 0;
    int lastBetAmount = 0;
    int lastInterval = 100; // BB Size
    for (int i = 0; i < actions.length; i++) {
      if(actions[i].getAction() == Consts.ACTION_BIGBLIND) {
        bettingLevel = 1;
        lastBetAmount = actions[i].getAmount();
      } else if(actions[i].getAction() == Consts.ACTION_BET || actions[i].getAction() == Consts.ACTION_RAISE) {
        bettingLevel++;
        lastInterval = actions[i].getAmount() - lastBetAmount;
        lastBetAmount = actions[i].getAmount();
      } 
      /*
      if(actions[i].getAction() == Consts.ACTION_ALLIN) {
        if(actions[i].getAmount() - lastBetAmount >= lastInterval) { // only increase betlevel if it is a real raise
           bettingLevel++;
           lastInterval = actions[i].getAmount() - lastBetAmount;
        }
        if(actions[i].getAmount() > lastBetAmount) {
           lastBetAmount = actions[i].getAmount();
        }
      }
      */
    }
    return bettingLevel;
  }

  /**
  * Returns the aggressor for the given actions array. Aggressor is the player, who put in the last bet/raise.
  */
  public static byte getAggressorSeat(Action[] actions) {
    byte aggr = -1;
    for (int i = 0; i < actions.length; i++) {
     if(actions[i].getAction() == Consts.ACTION_BET || actions[i].getAction() == Consts.ACTION_RAISE) {
        aggr = actions[i].getPlayerSeatId();
     }
  }
    return aggr;
  }


  /**
  * Returns at which "index" the given seat makes his first action. Index is here not relative
  * to the actions array, but to the game relevant actions inside that array. I.e. 0 == first to act
  * 1 == second to act and so on...
  * @param seat
  * @param actions
  * @return
  */
  public static int getActionIndex(byte seat, Action[] actions) {
    int c = 0;
    for (int i = 0; i < actions.length; i++) {
     if(actions[i].getPlayerSeatId() != Consts.PLAYERID_UNKNOWN) {
        if(Consts.isVoluntaryGameAction(actions[i].getAction())) {
           if(actions[i].getPlayerSeatId() == seat) {
              return c;
           }
           c++;
        }
     }
  }
    return -1;
  }

  public String toString() {
    if (!r.handLoaded) r._loadGameData();
    return PokerHandPrinter.printHand(this);
  }

  public String toString(byte site) {
    if (!r.handLoaded) r._loadGameData();
    if (site == Consts.SITE_PARTY) return PokerHandPrinter_Party.printHand(this);
    if (site == Consts.SITE_PS) return PokerHandPrinter.printHand(this); // todo: set here for PS (siteName printed, etc)
    return PokerHandPrinter.printHand(this);
  }

}


