package pokerai.hhex.filter;

import pokerai.hhex.Consts;
import pokerai.hhex.PokerHand;
import pokerai.hhex.internal.HandManagerNIO;

/**
 * 
 * Class for encapsulating HandManagerNIO and filtering the provided hands
 * by given filters.
 * 
 * @author CodeD (http://pokerai.org/pf3)
 *
 */
public class FilteredHandManager {

	private HandManagerNIO hm;
	private HHFilter[] filters;
	private PokerHand nextHand;
	private int matchingHandCount = 0;
	private int handCount = 0;
	
	public FilteredHandManager(HandManagerNIO handManager, HHFilter... filters) {
		hm = handManager;
		this.filters = filters;
		
		// init first hand
		searchNextValidHand();
	}
	
	public void setHandManagerNIO(HandManagerNIO hm) {
		this.hm = hm;
		searchNextValidHand();
	}

	public boolean hasMoreHands() {
		return nextHand != null;
	}

	public PokerHand nextPokerHand() {
		PokerHand tmp = nextHand;
		searchNextValidHand();
		return tmp;
	}
	
	public int getHandCount() {
		return handCount;
	}
	
	public int getMatchingHandCount() {
		return matchingHandCount;
	}

	private void searchNextValidHand() {
		if (hm == null) {
			return;
		}
		mainLoop:
		while(hm.hasMoreHands()) {
			PokerHand hand = hm.nextPokerHand();
			handCount++;
			for (int i = 0; i < filters.length; i++) {
				if(!filters[i].validHand(hand)) {
					continue mainLoop;
				}
			}
			nextHand = hand;
			matchingHandCount++;
			return;
		}
		nextHand = null;
	}
	

	/**
	 * 
	 * Example for FilteredHandManager
	 * 
	 */
	public static void main(String[] args) {
		String rootfolder = "C:\\hhdb\\";
		if (args.length > 0)
			rootfolder = args[0];
		if (!rootfolder.endsWith("\\"))
			rootfolder += "\\";
		
		System.out.println("START");
		long time = System.currentTimeMillis();
	    HandManagerNIO hm = new HandManagerNIO();
	    hm.init(rootfolder, "pokerai.org.sample", Consts.SITE_PS, Consts.HOLDEM_NL, (byte)9, (byte) 8);
	    hm.reset();
	    
	    // 7 - 10 Players, Hand ended either on turn or river (no showdown!), blindsizes 50-200
	    FilteredHandManager fhm = new FilteredHandManager(hm,
	    		new HHFilterPlayerCount(7,10),
	    		new HHORFilter(new HHFilterLastStreet(Consts.STREET_TURN),new HHFilterLastStreet(Consts.STREET_RIVER)),
	    		new HHORFilter(new HHFilterBigBlindSize(50),new HHFilterBigBlindSize(100),new HHFilterBigBlindSize(200))
	    );
	    
	    while (fhm.hasMoreHands()) {
	        processHand(fhm.nextPokerHand());
	    }
	    
	    hm.closedb();
	    
	    long time2 = System.currentTimeMillis() - time;
	    long handsPerSecond = (long)(((1000.0*fhm.getHandCount()) / time2));
	    System.out.println("Number of hands: " + fhm.getHandCount() + "(Overall) / " + fhm.getMatchingHandCount() + " (Matched Filters)");
	    System.out.println("Read & processing time: " + (long)(time2/1000.0) + " second. ");
	    System.out.println("Read & processing speed: " + handsPerSecond + " hands/second. ");
	}

	private static void processHand(PokerHand hand) {
//		System.out.println(hand);
	}
}
