package pokerai.hhex.filter;

import pokerai.hhex.Consts;
import pokerai.hhex.PokerHand;

/**
 * 
 * Filter for last betting round, i.e. Consts.STREET_...
 * 
 * @author CodeD (http://pokerai.org/pf3)
 *
 */
public class HHFilterLastStreet extends HHFilter {

	private int street;
	
	public HHFilterLastStreet(int street) {
		switch(street) {
		case Consts.STREET_PREFLOP:
			this.description = "HHFilterLastStreet: PreFlop"; 
			break;
		case Consts.STREET_FLOP:
			this.description = "HHFilterLastStreet: Flop"; 
			break;
		case Consts.STREET_TURN:
			this.description = "HHFilterLastStreet: Turn"; 
			break;
		case Consts.STREET_RIVER:
			this.description = "HHFilterLastStreet: River"; 
			break;
		case Consts.STREET_SHOWDOWN:
			this.description = "HHFilterLastStreet: Showdown"; 
			break;
		}
		this.street = street;
	}
	
	@Override
	public boolean validHand(PokerHand hand) {
		switch(street) {
		case Consts.STREET_PREFLOP:
			return hand.aactionsPreflop() != null && hand.aactionsFlop() == null;
		case Consts.STREET_FLOP:
			return hand.aactionsFlop() != null && hand.aactionsTurn() == null;
		case Consts.STREET_TURN:
			return hand.aactionsTurn() != null && hand.aactionsRiver() == null;
		case Consts.STREET_RIVER:
			return hand.aactionsRiver() != null && hand.aactionsShowdown() == null;
		case Consts.STREET_SHOWDOWN:
			return hand.aactionsShowdown() != null;
		}
		return false;
	}

}
