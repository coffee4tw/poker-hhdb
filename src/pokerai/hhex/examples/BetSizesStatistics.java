package pokerai.hhex.examples;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import pokerai.hhex.Action;
import pokerai.hhex.Consts;
import pokerai.hhex.PokerHand;
import pokerai.hhex.internal.HandManagerNIO;

public class BetSizesStatistics {

	public static void main(String[] args) {
		String rootfolder = "W:\\dev\\hhdb_sample\\";
		if (args.length > 0) rootfolder = args[0];
		if (!rootfolder.endsWith("\\")) rootfolder += "\\";
		// ---
		File dir = new File(rootfolder);
		String[] all = dir.list();
		BetSizesStatistics stats = new BetSizesStatistics();
		time = System.currentTimeMillis();
		for (int i = 0; i < all.length; i++) if (all[i].startsWith("pokerai.org.sample" + Consts.SITE_PS /*+ "_" + Consts.HOLDEM_NL + "_9"*/) && all[i].endsWith(".hhex")) {
			stats.scan(rootfolder, all[i]);
		}
		stats.printStats();
	}

	static long time = 0, hmTimeRead = 0, hmTimeParse = 0, size = 0;
	long totalHands = 0, matchingHands = 0, errorHands = 0;
	// calls
	long sumPreCalls = 0, sumFlopCalls = 0, sumTurnCalls = 0, sumRiverCalls = 0;
	double sumPreCallToStackRatio = 0, sumPreCallToBBRatio = 0, sumPreCallToPotRatio = 0;
	double sumFlopCallToStackRatio = 0, sumFlopCallToBBRatio = 0, sumFlopCallToPotRatio = 0;
	double sumTurnCallToStackRatio = 0, sumTurnCallToBBRatio = 0, sumTurnCallToPotRatio = 0;
	double sumRiverCallToStackRatio = 0, sumRiverCallToBBRatio = 0, sumRiverCallToPotRatio = 0;
	// bets
	long sumPreBets = 0, sumFlopBets = 0, sumTurnBets = 0, sumRiverBets = 0;
	double sumPreBetToStackRatio = 0, sumPreBetToBBRatio = 0, sumPreBetToPotRatio = 0;
	double sumFlopBetToStackRatio = 0, sumFlopBetToBBRatio = 0, sumFlopBetToPotRatio = 0;
	double sumTurnBetToStackRatio = 0, sumTurnBetToBBRatio = 0, sumTurnBetToPotRatio = 0;
	double sumRiverBetToStackRatio = 0, sumRiverBetToBBRatio = 0, sumRiverBetToPotRatio = 0;
	// raises
	long sumPreRaises = 0, sumFlopRaises = 0, sumTurnRaises = 0, sumRiverRaises = 0;
	double sumPreRaiseToStackRatio = 0, sumPreRaiseToBBRatio = 0, sumPreRaiseToPotRatio = 0, sumPreRaiseToPrevRatio = 0;
	double sumFlopRaiseToStackRatio = 0, sumFlopRaiseToBBRatio = 0, sumFlopRaiseToPotRatio = 0, sumFlopRaiseToPrevRatio = 0;
	double sumTurnRaiseToStackRatio = 0, sumTurnRaiseToBBRatio = 0, sumTurnRaiseToPotRatio = 0, sumTurnRaiseToPrevRatio = 0;
	double sumRiverRaiseToStackRatio = 0, sumRiverRaiseToBBRatio = 0, sumRiverRaiseToPotRatio = 0, sumRiverRaiseToPrevRatio = 0;

	public void scan(String rootfolder, String fullName) {
		HandManagerNIO hm = new HandManagerNIO();
		size += hm.init(rootfolder, fullName);
		hm.reset();
		while (hm.hasMoreHands()) {
			PokerHand hand = hm.nextPokerHand();
			totalHands++;
			if (totalHands % 10000000 == 0) 
				System.out.println((long)totalHands + " hands read. " + (long)matchingHands + " matching hands so far.");
			int[] stacks = hand.getStacks().clone();
			if (hand.getGameType() == Consts.HOLDEM_NL && hand.getNumberOfSeats() == 2) {
				matchingHands++;
				double pot = 0;
				boolean error = false;
				double prevAmount = 0;
				if(hand.aactionsPreflop() != null) {
					// process preflop actions
					boolean firstin = false;
					for (Action action : hand.aactionsPreflop()) {
						double amount = action.getAmount();
						byte act = action.getAction();
						byte playerSeatId = action.getPlayerSeatId();
						if (playerSeatId < stacks.length && stacks[playerSeatId] < amount) {
							error = true;
							errorHands++;
							break;
						}
						if(act == Consts.ACTION_BIGBLIND || act == Consts.ACTION_BOTHBLINDS)
							firstin = true;
						if(act == Consts.ACTION_CALL) {
							double stack = stacks[playerSeatId];
							sumPreCallToStackRatio += (amount/stack);
							sumPreCallToBBRatio += amount / 100;
							sumPreCallToPotRatio += amount / pot;
							sumPreCalls++;
						} else if(firstin && act == Consts.ACTION_RAISE) {
							// consider firstin raise preflop as bet as there are no bets preflop otherwise!
							double stack = stacks[playerSeatId];
							sumPreBetToStackRatio += amount / stack;
							sumPreBetToBBRatio += amount / 100;
							sumPreBetToPotRatio += amount / pot;
							sumPreBets++;
							firstin = false;
						} else if(!firstin && act == Consts.ACTION_RAISE) {
							double stack = stacks[playerSeatId];
							sumPreRaiseToStackRatio += amount / stack;
							sumPreRaiseToBBRatio += amount / 100;
							sumPreRaiseToPotRatio += amount / pot;
							sumPreRaiseToPrevRatio += amount / prevAmount;
							sumPreRaises++;
						}
						if(act == Consts.ACTION_SMALLBLIND || act == Consts.ACTION_BIGBLIND || act == Consts.ACTION_BOTHBLINDS || act == Consts.ACTION_CALL || act == Consts.ACTION_BET || act == Consts.ACTION_RAISE) {
							pot += amount;
							prevAmount = amount;
							if (playerSeatId < stacks.length)
								stacks[playerSeatId] -= amount;
						}
					}
				}
				if(!error && hand.aactionsFlop() != null) {
					// process flop actions
					for (Action action : hand.aactionsFlop()) {
						double amount = action.getAmount();
						byte act = action.getAction();
						byte playerSeatId = action.getPlayerSeatId();
						if (playerSeatId < stacks.length && stacks[playerSeatId] < amount) {
							error = true;
							errorHands++;
							break;
						}
						if(act == Consts.ACTION_CALL) {
							double stack = stacks[playerSeatId];
							sumFlopCallToStackRatio += amount / stack;
							sumFlopCallToBBRatio += amount / 100;
							sumFlopCallToPotRatio += amount / pot;
							sumFlopCalls++;
						} else if(act == Consts.ACTION_BET) {
							double stack = stacks[playerSeatId];
							sumFlopBetToStackRatio += amount / stack;
							sumFlopBetToBBRatio += amount / 100;
							sumFlopBetToPotRatio += amount / pot;
							sumFlopBets++;
						} else if(act == Consts.ACTION_RAISE) {
							double stack = stacks[playerSeatId];
							sumFlopRaiseToStackRatio += amount / stack;
							sumFlopRaiseToBBRatio += amount / 100;
							sumFlopRaiseToPotRatio += amount / pot;
							sumFlopRaiseToPrevRatio += amount / prevAmount;
							sumFlopRaises++;
						}
						if(act == Consts.ACTION_CALL || act == Consts.ACTION_BET || act == Consts.ACTION_RAISE) {
							pot += amount;
							prevAmount = amount;
							if (playerSeatId < stacks.length)
								stacks[playerSeatId] -= amount;
						}
					}
				}
				if(!error && hand.aactionsTurn() != null) {
					// process turn actions
					for (Action action : hand.aactionsTurn()) {
						double amount = action.getAmount();
						byte act = action.getAction();
						byte playerSeatId = action.getPlayerSeatId();
						if (playerSeatId < stacks.length && stacks[playerSeatId] < amount) {
							error = true;
							errorHands++;
							break;
						}
						if(act == Consts.ACTION_CALL) {
							double stack = stacks[playerSeatId];
							sumTurnCallToStackRatio += amount / stack;
							sumTurnCallToBBRatio += amount / 100;
							sumTurnCallToPotRatio += amount / pot;
							sumTurnCalls++;
						} else if(act == Consts.ACTION_BET) {
							double stack = stacks[playerSeatId];
							sumTurnBetToStackRatio += amount / stack;
							sumTurnBetToBBRatio += amount / 100;
							sumTurnBetToPotRatio += amount / pot;
							sumTurnBets++;
						} else if(act == Consts.ACTION_RAISE) {
							double stack = stacks[playerSeatId];
							sumTurnRaiseToStackRatio += amount / stack;
							sumTurnRaiseToBBRatio += amount / 100;
							sumTurnRaiseToPotRatio += amount / pot;
							sumTurnRaiseToPrevRatio += amount / prevAmount;
							sumTurnRaises++;
						}
						if(act == Consts.ACTION_CALL || act == Consts.ACTION_BET || act == Consts.ACTION_RAISE) {
							pot += amount;
							prevAmount = amount;
							if (playerSeatId < stacks.length)
								stacks[playerSeatId] -= amount;
						}
					}
				}
				if(!error && hand.aactionsRiver() != null) {
					// process river actions
					for (Action action : hand.aactionsRiver()) {
						double amount = action.getAmount();
						byte act = action.getAction();
						byte playerSeatId = action.getPlayerSeatId();
						if (playerSeatId < stacks.length && stacks[playerSeatId] < amount) {
							error = true;
							errorHands++;
							break;
						}
						if(act == Consts.ACTION_CALL) {
							double stack = stacks[playerSeatId];
							sumRiverCallToStackRatio += amount / stack;
							sumRiverCallToBBRatio += amount / 100;
							sumRiverCallToPotRatio += amount / pot;
							sumRiverCalls++;
						} else if(act == Consts.ACTION_BET) {
							double stack = stacks[playerSeatId];
							sumRiverBetToStackRatio += amount / stack;
							sumRiverBetToBBRatio += amount / 100;
							sumRiverBetToPotRatio += amount / pot;
							sumRiverBets++;
						} else if(act == Consts.ACTION_RAISE) {
							double stack = stacks[playerSeatId];
							sumRiverRaiseToStackRatio += amount / stack;
							sumRiverRaiseToBBRatio += amount / 100;
							sumRiverRaiseToPotRatio += amount / pot;
							sumRiverRaiseToPrevRatio += amount / prevAmount;
							sumRiverRaises++;
						}
						if(act == Consts.ACTION_CALL || act == Consts.ACTION_BET || act == Consts.ACTION_RAISE) {
							pot += amount;
							prevAmount = amount;
							if (playerSeatId < stacks.length)
								stacks[playerSeatId] -= amount;
						}
					}
				}
				
				// TODO print error hands to file!
				if (error) {
					try {
						FileWriter fw = new FileWriter("errorhands\\Game"+hand.getGameID()+".txt", true);
						fw.append(hand.toString());
						fw.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		hmTimeRead += hm.timeRead;
		hmTimeParse += hm.timeParse;
		hm.closedb();
	}

	public void printStats() {
		// print out results here
		System.out.println("*** Average Call To X Statistics ***");
		System.out.println("Round\\Ratio\tStack\tPot\tBB");
		System.out.println("Pre:\t\t" + round(sumPreCallToStackRatio/sumPreCalls) + "\t" + round(sumPreCallToPotRatio/sumPreCalls) + "\t" + round(sumPreCallToBBRatio/sumPreCalls));
		System.out.println("Flop:\t\t" + round(sumFlopCallToStackRatio/sumFlopCalls) + "\t" + round(sumFlopCallToPotRatio/sumFlopCalls) + "\t" + round((sumFlopCallToBBRatio/sumFlopCalls)));
		System.out.println("Turn:\t\t" + round(sumTurnCallToStackRatio/sumTurnCalls) + "\t" + round(sumTurnCallToPotRatio/sumTurnCalls) + "\t" + round(sumTurnCallToBBRatio/sumTurnCalls));
		System.out.println("River:\t\t" + round(sumRiverCallToStackRatio/sumRiverCalls) + "\t" + round(sumRiverCallToPotRatio/sumRiverCalls) + "\t" + round(sumRiverCallToBBRatio/sumRiverCalls));
		System.out.println("*** Average Bet To X Statistics ***");
		System.out.println("Round\\Ratio\tStack\tPot\tBB");
		System.out.println("Pre:\t\t" + round(sumPreBetToStackRatio/sumPreBets) + "\t" + round(sumPreBetToPotRatio/sumPreBets) + "\t" + round(sumPreBetToBBRatio/sumPreBets));
		System.out.println("Flop:\t\t" + round(sumFlopBetToStackRatio/sumFlopBets) + "\t" + round(sumFlopBetToPotRatio/sumFlopBets) + "\t" + round(sumFlopBetToBBRatio/sumFlopBets));
		System.out.println("Turn:\t\t" + round(sumTurnBetToStackRatio/sumTurnBets) + "\t" + round(sumTurnBetToPotRatio/sumTurnBets) + "\t" + round(sumTurnBetToBBRatio/sumTurnBets));
		System.out.println("River:\t\t" + round(sumRiverBetToStackRatio/sumRiverBets) + "\t" + round(sumRiverBetToPotRatio/sumRiverBets) + "\t" + round(sumRiverBetToBBRatio/sumRiverBets));
		System.out.println("*** Average Raise To X Statistics ***");
		System.out.println("Round\\Ratio\tStack\tPot\tBB\tPrevious");
		System.out.println("Pre:\t\t" + round(sumPreRaiseToStackRatio/sumPreRaises) + "\t" + round(sumPreRaiseToPotRatio/sumPreRaises) + "\t" + round(sumPreRaiseToBBRatio/sumPreRaises) + "\t" + round(sumPreRaiseToPrevRatio/sumPreRaises));
		System.out.println("Flop:\t\t" + round(sumFlopRaiseToStackRatio/sumFlopRaises) + "\t" + round(sumFlopRaiseToPotRatio/sumFlopRaises) + "\t" + round(sumFlopRaiseToBBRatio/sumFlopRaises) + "\t" + round(sumFlopRaiseToPrevRatio/sumFlopRaises));
		System.out.println("Turn:\t\t" + round(sumTurnRaiseToStackRatio/sumTurnRaises) + "\t" + round(sumTurnRaiseToPotRatio/sumTurnRaises) + "\t" + round(sumTurnRaiseToBBRatio/sumTurnRaises) + "\t" + round(sumTurnRaiseToPrevRatio/sumTurnRaises));
		System.out.println("River:\t\t" + round(sumRiverRaiseToStackRatio/sumRiverRaises) + "\t" + round(sumRiverRaiseToPotRatio/sumRiverRaises) + "\t" + round(sumRiverRaiseToBBRatio/sumRiverRaises) + "\t" + round(sumRiverRaiseToPrevRatio/sumRiverRaises));
		
		System.out.println("Number of hands: " + (long)matchingHands + " (of total " + (long)totalHands + "), " + (size / totalHands) + " bytes/hand");
		System.out.println("Hands with errors: " + (long)errorHands + " (some action amount was higher than the stack left)");
	    long time2 = System.currentTimeMillis() - time;
	    long handsPerSecond = (long)(((1000.0*totalHands) / time2)); if (handsPerSecond == 0) handsPerSecond++;
	    System.out.println("Read & processing speed: " + handsPerSecond + " hands/second. ");
	    System.out.println("Read & processing time: " + (long)(time2/1000.0) + " second. ");
	    System.out.println("Read time (ms): " + hmTimeRead + ", parse time (ms): " + hmTimeParse);
	    System.out.println("Estimated time to read & process 600 million hands: " + (600000000 / handsPerSecond)
	            + " seconds (" + (((600000000 / handsPerSecond)+30)/60) + " minutes). ");
	}
	
	public float round(double num) {
		int precision = 4;
		float p = (float) Math.pow(10, precision);
		num = num * p;
		float tmp = Math.round(num);
		return (float)tmp/p;
	}
}
