package pokerai.hhex.filter;

import pokerai.hhex.PokerHand;

/**
 * Class for filtering HH's by stack sizes. Use 0 or Integer.MAX_VALUE to leave one boundary open.
 * If playerCount <= 0 <=> All player stacks have to be between the boundaries.
 * Otherwise at least 'playerCount' player's stacks have to be between the boundaries.
 * 
 * @author CodeD (http://pokerai.org/pf3)
 *
 */
public class HHFilterStackSizes extends HHFilter{

	private int min, max;
	private int playerCount;
	
	public HHFilterStackSizes(int minimumStackSizeInBB, int maximumStackSizeInBB, int playerCount) {
		this.description = "HHFilterStackSizes: " + min + " - " + max;
		min = minimumStackSizeInBB;
		max = maximumStackSizeInBB;
		this.playerCount = playerCount;
	}

	@Override
	public boolean validHand(PokerHand hand) {
		int[] stacks = hand.getStacks();
		int count = 0;
		for (int i = 0; i < stacks.length; i++) {
			if(!(min <= stacks[i] && stacks[i] <= max) && playerCount <= 0) {
				return false;
			} else {
				count++;
			}
		}
		return count >= playerCount;
	}

	public static void main(String[] args) {
		HHFilter filter = new HHFilter() {
		
			@Override
			public boolean validHand(PokerHand hand) {
				return false;
			}
		};
	}
}
