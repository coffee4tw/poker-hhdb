package pokerai.hhex.filter;

import pokerai.hhex.PokerHand;

/**
 * Class for filtering HH's by player ids.
 * 
 * @author CodeD (http://pokerai.org/pf3)
 *
 */
public class HHFilterPlayer extends HHFilter {

	private int[] id;
	
	public HHFilterPlayer(int[] id) {
		this.description = "HHFilterPlayer: " + id.length + " player ids.";
		this.id = id;
	}
	
	public HHFilterPlayer(int id) {
		this.description = "HHFilterPlayer: " + id;
		this.id = new int[1];
		this.id[0] = id;
	}

	@Override
	public boolean validHand(PokerHand hand) {
		int[] ids = hand.getPlayerIDs();
		for (int i = 0; i < ids.length; i++) {
  	  for (int j = 0; j < id.length; j++) {
        if (ids[i] == id[j]) {
          return true;
        }
		  }
		}
		return false;
	}

}
