package pokerai.hhex;

import pokerai.hhex.internal.HandManagerNIO;
import pokerai.hhex.internal.PlayerHandsDBIndex;

/**
 * 
 * Simple class to make life easier, i.e. to scan all the db files and process the hands. 
 * Just extend, and implement the necessary methods.
 * 
 * @author Indiana, CodeD (http://pokerai.org/pf3)
 *
 */
public abstract class PlayerHandProcessor extends HandProcessor {
	
  private int[] playerIds = null;

  public void setPlayerId(int ids) {
    this.playerIds = new int[1];
    this.playerIds[0] = ids;
  }

  public void setPlayerIds(int[] ids) {
    this.playerIds = ids;
  }

  // "_" to avoid naming conflicts with subclasses
	protected long _totalHands = 0, hmTimeRead = 0, hmTimeParse = 0;

	public void process() {
    // Todo: Filtering by other provided parameters (game, type, blinds, etc.)
    if (playerIds != null) {
      PlayerHandsDBIndex.buildIndexForPlayers(rootFolder, playerIds);
      for (int i = 0; i < playerIds.length; i++) {
        HandManagerNIO h = new HandManagerNIO();
        h.init(rootFolder + "indices\\", "player" + playerIds[i]);
        while (h.hasMoreHands()) {
          PokerHand ph = h.nextPokerHand();
          processHand(ph, playerIds[i]);
        }
        h.closedb();
      }
    }
    printResults();
	}

  protected void processHand(PokerHand hand) {
    // never called. Here just because PlayerHandProcessor extends HandProcessor, but the processing login isn't reused!
  }

  // called for each new hand that fullfills the given criterias
  protected abstract void processHand(PokerHand hand, int PlayerId);

  // called when parsing is complete
  protected abstract void printResults();

}
