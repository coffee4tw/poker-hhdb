package pokerai.hhex.filter;

import pokerai.hhex.Consts;
import pokerai.hhex.PokerHand;
import pokerai.hhex.internal.HandManagerNIO;

import java.text.DecimalFormat;
import java.util.LinkedList;

/**
 * Class for counting hh's which apply to given filters.
 * 
 * @author CodeD (http://pokerai.org/pf3)
 *
 */
public class CountFilteredHands {

	public static int[] countFilteredHandHistories(HandManagerNIO hm, HHFilter... filters) {
		int[] count = new int[filters.length];
		while(hm.hasMoreHands()) {
			PokerHand hand = hm.nextPokerHand();
			for (int i = 0; i < filters.length; i++) {
				if(filters[i].validHand(hand)) {
					count[i]++;
				}
			}
		}
		return count;
	}
	
	
	
	/**
	 * Short example
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String rootfolder = "C:\\hhdb\\";
		if (args.length > 0)
			rootfolder = args[0];
		if (!rootfolder.endsWith("\\"))
			rootfolder += "\\";
		
		System.out.println("START");
	    HandManagerNIO hm = new HandManagerNIO();
	    hm.init(rootfolder, "pokerai.org.sample", Consts.SITE_PS, Consts.HOLDEM_NL, (byte)9, (byte) 8);
	    hm.reset();
	    
	    int[] blindSizesToTest = new int[]{50,100,200,400,600,1000,2000};
	    
	    LinkedList<HHFilter> filtersList = new LinkedList<HHFilter>();
	    for (int bb : blindSizesToTest) {
			filtersList.add(new HHANDFilter("Overall (NL" + bb + ")",new HHFilterPlayerCount(7,10),
	    		new HHFilterBigBlindSize(bb)));
			filtersList.add(new HHANDFilter("PreFlop (NL" + bb + ")",new HHFilterPlayerCount(7,10),
		    		new HHFilterLastStreet(Consts.STREET_PREFLOP),
		    		new HHFilterBigBlindSize(bb)));
			filtersList.add(new HHANDFilter("Flop (NL" + bb + ")",new HHFilterPlayerCount(7,10),
		    		new HHFilterLastStreet(Consts.STREET_FLOP),
		    		new HHFilterBigBlindSize(bb)));
			filtersList.add(new HHANDFilter("Turn (NL" + bb + ")",new HHFilterPlayerCount(7,10),
		    		new HHFilterLastStreet(Consts.STREET_TURN),
		    		new HHFilterBigBlindSize(bb)));
			filtersList.add(new HHANDFilter("River (NL" + bb + ")",new HHFilterPlayerCount(7,10),
		    		new HHFilterLastStreet(Consts.STREET_RIVER),
		    		new HHFilterBigBlindSize(bb)));
			filtersList.add(new HHANDFilter("Showdown (NL" + bb + ")",new HHFilterPlayerCount(7,10),
		    		new HHFilterLastStreet(Consts.STREET_SHOWDOWN),
		    		new HHFilterBigBlindSize(bb)));
		}
	    HHFilter[] filters = filtersList.toArray(new HHFilter[filtersList.size()]);
	    
	    int[] counts = countFilteredHandHistories(hm, filters);
	    hm.closedb();

		DecimalFormat df = new DecimalFormat("0.##");
	    System.out.println("Counts (where HHs ended):");
	    for (int i = 0; i < counts.length; i++) {
			if((i%6) == 0) {
				System.out.println();
			}
			System.out.print(fillSpaces(filters[i].toString() + ":", 19) + " " + fillSpaces("" + counts[i], 8));
			if((i%6) != 0) {
				System.out.print(" \t%:" + df.format(counts[i]*100.0/counts[6*(i/6)]));
			}
			System.out.println();
		}
	}
	
	private static String fillSpaces(String s, int desiredLength) {
		if(s.length() < desiredLength) {
			StringBuilder sb = new StringBuilder(desiredLength);
			sb.append(s);
			for (int i = s.length(); i < desiredLength; i++) {
				sb.append(' ');
			}
			return sb.toString();
		}
		return s;
	}
}
