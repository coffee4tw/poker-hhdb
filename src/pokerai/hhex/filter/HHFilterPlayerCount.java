package pokerai.hhex.filter;

import pokerai.hhex.PokerHand;

/**
 * Class for filtering HH's by number of occupied seats.
 * 
 * @author CodeD (http://pokerai.org/pf3)
 *
 */
public class HHFilterPlayerCount extends HHFilter{

	private byte min, max;
	
	public HHFilterPlayerCount(int min, int max) {
		this.description = "HHFilterPlayerCount: " + min + " - " + max;
		this.min = (byte)min;
		this.max = (byte)max;
	}
	
	
	@Override
	public boolean validHand(PokerHand hand) {
		int[] players = hand.getPlayerIDs();
		int c = 0;
		for (int i = 0; i < players.length; i++) {
			if(players[i] > 0) {
				c++;
			}
		}
		
		return min <= c && c <= max;
	}

}
