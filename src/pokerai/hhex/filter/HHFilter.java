package pokerai.hhex.filter;

import pokerai.hhex.PokerHand;

/**
 * 
 * Abstract class for filtering out unwanted HandHistories.
 * 
 * @author CodeD (http://pokerai.org/pf3)
 *
 */
public abstract class HHFilter {

	protected String description = "HHFilter";
	
	public abstract boolean validHand(PokerHand hand);
	
	public String toString() {
		return description;
	}
}
