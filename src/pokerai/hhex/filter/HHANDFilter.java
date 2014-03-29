package pokerai.hhex.filter;

import pokerai.hhex.PokerHand;

/**
 * 
 * Filter which returns true for valid hand if all of
 * the given filters return true.
 * 
 * @author CodeD (http://pokerai.org/pf3)
 *
 */
public class HHANDFilter extends HHFilter{

	private HHFilter[] filters;
	
	public HHANDFilter(HHFilter... filters) {
		this.description = "HHANDFilter";
		this.filters = filters;
	}
	
	public HHANDFilter(String description, HHFilter... filters) {
		this.description = description;
		this.filters = filters;
	}

	@Override
	public boolean validHand(PokerHand hand) {
		for (int i = 0; i < filters.length; i++) {
			if(!filters[i].validHand(hand)) {
				return false;
			}
		}
		return true;
	}
}
