package pokerai.hhex.filter;

import pokerai.hhex.PokerHand;

/**
 * Class for filtering HH's by big blind size.
 * 
 * @author CodeD (http://pokerai.org/pf3)
 *
 */
public class HHFilterBigBlindSize  extends HHFilter {

	private int bb;
	
	public HHFilterBigBlindSize(int bb) {
		this.description = "HHFilterBigBlindSize: " + bb;
		this.bb = bb;
	}
	
	@Override
	public boolean validHand(PokerHand hand) {
		return hand.bigBlind() == bb;
	}

}