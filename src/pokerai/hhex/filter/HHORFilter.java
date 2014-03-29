package pokerai.hhex.filter;

import pokerai.hhex.PokerHand;

/**
 * 
 * Filter which returns true for valid hand if at least one of
 * the given filters returns true.
 * 
 * @author CodeD (http://pokerai.org/pf3)
 *
 */
public class HHORFilter extends HHFilter{

	private HHFilter[] filters;
	
	public HHORFilter(HHFilter... filters) {
		this.description = "HHORFilter";
		this.filters = filters;
	}
	
	public HHORFilter(String description, HHFilter... filters) {
		this.description = description;
		this.filters = filters;
	}

	@Override
	public boolean validHand(PokerHand hand) {
		for (int i = 0; i < filters.length; i++) {
			if(filters[i].validHand(hand)) {
				return true;
			}
		}
		return false;
	}

}
