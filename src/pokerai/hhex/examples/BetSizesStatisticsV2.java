package pokerai.hhex.examples;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import pokerai.hhex.Action;
import pokerai.hhex.Consts;
import pokerai.hhex.PokerHand;
import pokerai.hhex.internal.HandManagerNIO;

public class BetSizesStatisticsV2 {

	static final String folder = "W:\\dev\\";
	static final boolean use_sample_db = false;

	public static void main(String[] args) {
		String rootfolder = folder;
		if (use_sample_db)
			rootfolder += "hhdb_sample\\";
		else
			rootfolder += "hhdb\\";
		if (args.length > 0) rootfolder = args[0];
		if (!rootfolder.endsWith("\\")) rootfolder += "\\";
		// ---
		File dir = new File(rootfolder);
		String[] all = dir.list();
		BetSizesStatisticsV2 stats = new BetSizesStatisticsV2();
		time = System.currentTimeMillis();
		for (int i = 0; i < all.length; i++) if (all[i].startsWith("pokerai.org.sample" + Consts.SITE_PS /*+ "_" + Consts.HOLDEM_NL + "_9"*/) && all[i].endsWith(".hhex")) {
			stats.scan(rootfolder, all[i]);
		}
		stats.printStats();
		stats.createGraphs();
	}

	static final int HU = 0, SH = 1, FR = 2;
	static final int PREFLOP = 0, FLOP = 1, TURN = 2, RIVER = 3;
	static final int CALL = 0, BET = 1, RAISE = 2, _3BET = 3, _4BET = 4, _5BET = 5;
	static final int TOBB = 0, TOPOT = 1, TOSTACK = 2, TOPREVIOUS = 3;

	double[][][][][] sumRatios;
	long[][][][] sumActions;
	int[][][][][][] distributions;

	public BetSizesStatisticsV2() {
		sumRatios = new double[3][][][][];
		sumActions = new long[3][][][];
		distributions = new int[3][][][][][];
		for (int i=0; i<sumRatios.length; i++) {
			int seats = 0;
			if (i == HU) seats = 2;
			else if (i == SH) seats = 6;
			else if (i == FR) seats = 9;
			sumRatios[i] = new double[seats][4][6][4];
			sumActions[i] = new long[seats][4][6];
			distributions[i] = new int[seats][4][6][4][51];
		}
	}


	static long time = 0, hmTimeRead = 0, hmTimeParse = 0, size = 0;
	long totalHands = 0, matchingHands = 0, errorHands = 0;

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
			if (hand.getGameType() == Consts.HOLDEM_NL) {
				int game = -1;
				if (hand.getNumberOfSeats() == 2)
					game = HU;
				else if (hand.getNumberOfSeats() == 6)
					game = SH;
				else if (hand.getNumberOfSeats() == 9)
					game = FR;

				// new matching hand found
				matchingHands++;

				// initialise needed variables for pot, prevAmount, bets and error
				double pot = 0;
//				boolean error = false;

				// cycle through all four stage (PREFLOP, FLOP, TURN, RIVER)
				for (int stage = 0; stage<4; stage++) {
					// prevAmount and bets are independent from earlier streets
					double prevAmount = 0;
					double[] bets = new double[stacks.length];

					// get actions from the current stage
					Action[] actions = null;
					if (stage == PREFLOP) actions = hand.aactionsPreflop();
					else if (stage == FLOP) actions = hand.aactionsFlop();
					else if (stage == TURN) actions = hand.aactionsTurn();
					else if (stage == RIVER) actions = hand.aactionsRiver();

					// if no actions or error in earlier stages break loop
					if (actions == null /*|| error*/)
						break;

					// firstin value, only needed for preflop
					boolean firstin = false;
					int numRaises = 0;

					// calculate position: preflop bb=0, sb=1... postflop bu=0, c0=1...
					ArrayList<Byte> playerSeatIds = new ArrayList<Byte>();
					for (Action action : actions) {
						if (action.getAction() == Consts.ACTION_FOLD || action.getAction() == Consts.ACTION_CHECK || action.getAction() == Consts.ACTION_CALL || action.getAction() == Consts.ACTION_BET || action.getAction() == Consts.ACTION_RAISE)
						{
							if(playerSeatIds.contains(action.getPlayerSeatId()))
								break;
							else
								playerSeatIds.add(action.getPlayerSeatId());
						} 
					}
					int[] positions = new int[playerSeatIds.size()];
					for (int i=0; i<positions.length; i++) {
						positions[i] = playerSeatIds.get(playerSeatIds.size()-1-i);
					}

					// cycle through all actions in this particular stage
					for (Action action : actions) {

						// get details from the current action
						double amount = action.getAmount();
						byte act = action.getAction();
						byte playerSeatId = action.getPlayerSeatId();
						double moreInPot = amount;

						// get position for this player
						int position = -1;
						for (int i=0; i<positions.length; i++) {
							if(positions[i] == playerSeatId) {
								position = i;
								break;
							}
						}
						if (position == -1) {
							if (action.getAction() == Consts.ACTION_FOLD || action.getAction() == Consts.ACTION_CHECK || action.getAction() == Consts.ACTION_CALL || action.getAction() == Consts.ACTION_BET || action.getAction() == Consts.ACTION_RAISE)
							{	
								System.out.println("ERROR");
								break;
							}
						}

						// if the action is a raise, save the amount that the player put more into the pot
						// e.g. player 1 bets 10, player 2 raises to 20, player 1 reraises to 30 then amount is 30 but more in the pot is only 20
						if (act == Consts.ACTION_RAISE)
							moreInPot -= bets[playerSeatId];
						else if (act == Consts.ACTION_WON)
							moreInPot = 0;

						// if the amount posted by a player is more than he has left in his stack there is an error in the hand
						if (playerSeatId < stacks.length && stacks[playerSeatId] < moreInPot) {
//							error = true;
							errorHands++;
//							break;
							double delta = moreInPot - stacks[playerSeatId];
							amount -= delta;
							moreInPot -= delta;
						}

						// if big blind or both blinds are posted by some player then the next bet will be a firstin bet
						if(act == Consts.ACTION_BIGBLIND || act == Consts.ACTION_BOTHBLINDS)
							firstin = true;
						else if(act == Consts.ACTION_CALL) {
							double stack = stacks[playerSeatId];
							sumRatios[game][position][stage][CALL][TOBB] += amount / 100;
							sumRatios[game][position][stage][CALL][TOPOT] += amount / pot;
							sumRatios[game][position][stage][CALL][TOSTACK] += amount / stack;
							sumActions[game][position][stage][CALL]++;
							distributions[game][position][stage][CALL][TOBB][(amount/100 < 50.5) ? (int)Math.round(amount/100) : 50]++;
							distributions[game][position][stage][CALL][TOPOT][(amount/pot*10 < 50.5) ? (int)Math.round(amount/pot*10) : 50]++;
							distributions[game][position][stage][CALL][TOSTACK][(amount/stack*50 < 50.5) ? (int)Math.round(amount/stack*50) : 50]++;
						} else if(act == Consts.ACTION_BET || (stage == PREFLOP && firstin && act == Consts.ACTION_RAISE)) {
							double stack = stacks[playerSeatId];
							sumRatios[game][position][stage][BET][TOBB] += amount / 100;
							sumRatios[game][position][stage][BET][TOPOT] += amount / pot;
							sumRatios[game][position][stage][BET][TOSTACK] += amount / stack;
							sumActions[game][position][stage][BET]++;
							distributions[game][position][stage][BET][TOBB][(amount/100 < 50.5) ? (int)Math.round(amount/100) : 50]++;
							distributions[game][position][stage][BET][TOPOT][(amount/pot*10 < 50.5) ? (int)Math.round(amount/pot*10) : 50]++;
							distributions[game][position][stage][BET][TOSTACK][(amount/stack*50 < 50.5) ? (int)Math.round(amount/stack*50) : 50]++;
							firstin = false;
						} else if(act == Consts.ACTION_RAISE) {
							int actionIndex = -1;
							if (numRaises == 0) actionIndex = RAISE;
							else if (numRaises == 1) actionIndex = _3BET;
							else if (numRaises == 2) actionIndex = _4BET;
							else if (numRaises > 2) actionIndex = _5BET;
							double stack = stacks[playerSeatId] + bets[playerSeatId];
							sumRatios[game][position][stage][actionIndex][TOBB] += amount / 100;
							sumRatios[game][position][stage][actionIndex][TOPOT] += amount / pot;
							sumRatios[game][position][stage][actionIndex][TOSTACK] += amount / stack;
							sumRatios[game][position][stage][actionIndex][TOPREVIOUS] += amount / prevAmount;
							sumActions[game][position][stage][actionIndex]++;
							distributions[game][position][stage][actionIndex][TOBB][(amount/100 < 50.5) ? (int)Math.round(amount/100) : 50]++;
							distributions[game][position][stage][actionIndex][TOPOT][(amount/pot*10 < 50.5) ? (int)Math.round(amount/pot*10) : 50]++;
							distributions[game][position][stage][actionIndex][TOSTACK][(amount/stack*50 < 50.5) ? (int)Math.round(amount/stack*50) : 50]++;
							distributions[game][position][stage][actionIndex][TOPREVIOUS][(amount/prevAmount*5 < 50.5) ? (int)Math.round(amount/prevAmount*5) : 50]++;
							numRaises++;
						}
						if(act == Consts.ACTION_SMALLBLIND || act == Consts.ACTION_BIGBLIND || act == Consts.ACTION_BOTHBLINDS || act == Consts.ACTION_CALL || act == Consts.ACTION_BET || act == Consts.ACTION_RAISE) {
							if (playerSeatId < stacks.length) {
								pot += moreInPot;
								stacks[playerSeatId] -= moreInPot;
								bets[playerSeatId] += moreInPot;
								prevAmount = bets[playerSeatId];
							}
						}
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

		for (int game = 0; game<sumRatios.length; game++) {
			if (game == HU) System.out.println("\t+++ HU (2max) +++");
			else if (game == SH) System.out.println("\t+++ SH (6max) +++");
			else if (game == FR) System.out.println("\t+++ FR (9max) +++");

			for (int position = 0; position<sumRatios[game].length; position++) {
				System.out.println("Stats for player that has " + position + " players left to act behind him.");

				for (int round = 0; round<sumRatios[game][position].length; round++) {
					if (round == PREFLOP) System.out.println("*** Average Preflop Statistics ***");
					else if (round == FLOP) System.out.println("*** Average Flop Statistics ***");
					else if (round == TURN) System.out.println("*** Average Turn Statistics ***");
					else if (round == RIVER) System.out.println("*** Average River Statistics ***");
					System.out.println("Action\\Ratio\tBB\tPot\tStack\tPrevious\tTotal");

					for (int action = 0; action<sumRatios[game][position][round].length; action++) {
						if (action == CALL) System.out.print("CALL:\t\t");
						else if (action == BET) System.out.print("BET:\t\t");
						else if (action == RAISE) System.out.print("RAISE:\t\t");
						else if (action == _3BET) System.out.print("3BET:\t\t");
						else if (action == _4BET) System.out.print("4BET:\t\t");
						else if (action == _5BET) System.out.print("5+BET:\t\t");

						for (int tox = 0; tox<sumRatios[game][position][round][action].length; tox++) {
							System.out.print(round(sumRatios[game][position][round][action][tox]/sumActions[game][position][round][action]) + "\t");
						}
						System.out.println("\t"+sumActions[game][position][round][action]);
					}
				}
				System.out.println();
			}
			System.out.println();
		}
		System.out.println("Number of hands: " + matchingHands + " (" + round(((float)matchingHands*100)/totalHands) + "% of total " + totalHands + "), " + (size / totalHands) + " bytes/hand");
		System.out.println("Hands with errors: " + errorHands + " (some action amount was higher than the stack left)");
		long time2 = System.currentTimeMillis() - time;
		long handsPerSecond = (long)(((1000.0*totalHands) / time2)); if (handsPerSecond == 0) handsPerSecond++;
		System.out.println("Read & processing speed: " + handsPerSecond + " hands/second. ");
		System.out.println("Read & processing time: " + (long)(time2/1000.0) + " second. ");
		System.out.println("Read time (ms): " + hmTimeRead + ", parse time (ms): " + hmTimeParse);
		System.out.println("Estimated time to read & process 600 million hands: " + (600000000 / handsPerSecond)
				+ " seconds (" + (((600000000 / handsPerSecond)+30)/60) + " minutes). ");
	}

	private void createGraphs() {
		long time = System.currentTimeMillis();
		System.out.println("Creating graphs...");
		final String[] rowKeys = {"to BB", "to Pot", "to Stack", "to Previous"};

		for (int game = 0; game<distributions.length; game++) {
			for (int position = 0; position<distributions[game].length; position++) {
				for (int stage = 0; stage<distributions[game][position].length; stage++) {
					for (int action = 1; action<distributions[game][position][stage].length; action++) {
						DefaultCategoryDataset dataset = new DefaultCategoryDataset();
						for (int tox = 0; tox<distributions[game][position][stage][action].length; tox++) {
							for (int group = 0; group<distributions[game][position][stage][action][tox].length; group++) {
//								String colKey = "";
//								colKey = (group % 5 == 0) ? group+"/"+(double)group/10+"/"+(double)group/2+"/"+(double)group/2 : String.valueOf(group);
								dataset.addValue(
										round((double)distributions[game][position][stage][action][tox][group]/sumActions[game][position][stage][action]), 
										rowKeys[tox], 
										(group==100) ? ">=100" : String.valueOf(group));
							}
						}
						String name = "";
						if (game == HU) name += "HU_";
						else if (game == SH) name += "SH_";
						else if (game == FR) name += "FR_";
						name += position +"_";
						if (stage == PREFLOP) name += "Preflop_";
						else if (stage == FLOP) name += "Flop_";
						else if (stage == TURN) name += "Turn_";
						else if (stage == RIVER) name += "River_";
						if (action == CALL) name += "CALL";
						else if (action == BET) name += "BET";
						else if (action == RAISE) name += "RAISE";
						else if (action == _3BET) name += "3BET";
						else if (action == _4BET) name += "4BET";
						else if (action == _5BET) name += "5BET";

						JFreeChart chart = ChartFactory.createLineChart(name, "bucket of amount", "% of actions", dataset, PlotOrientation.VERTICAL, true, false, false);
						// set the background color for the chart...
						chart.setBackgroundPaint(Color.white);

						// get a reference to the plot for further customisation...
						CategoryPlot plot = chart.getCategoryPlot();
						plot.setBackgroundPaint(Color.white);
						plot.setDomainGridlinePaint(Color.white);
						plot.setRangeGridlinePaint(Color.lightGray);

						// set the range axis to display integers only...
//			        	CategoryAxis rangeAxis = plot.getDomainAxis();
//			        	rangeAxis.setCategoryLabelPositionOffset(0);

						// disable bar outlines...
//						BarRenderer renderer = (BarRenderer) plot.getRenderer();
//						renderer.setDrawBarOutline(false);
//						renderer.setShadowVisible(false);
						try {
							ChartUtilities.saveChartAsJPEG(new File(folder+"graphs\\"+name+".jpg"), chart, 1500, 500);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		System.out.println("Creating Graphs finished in " + (System.currentTimeMillis()-time) + "ms");
	}

	public float round(double num) {
		int precision = 3;
		float p = (float) Math.pow(10, precision);
		num = num * p;
		float tmp = Math.round(num);
		return (float)tmp/p;
	}
}
