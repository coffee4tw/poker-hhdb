package pokerai.hhex.examples;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import pokerai.hhex.Action;
import pokerai.hhex.Consts;
import pokerai.hhex.PokerHand;
import pokerai.hhex.internal.HandManagerNIO;

public class BetSizesGraphs {

	public static void main(String[] args) {
		String rootfolder = "C:\\hhdb\\";
		if (args.length > 0) rootfolder = args[0];
		if (!rootfolder.endsWith("\\")) rootfolder += "\\";
		// ---
		File dir = new File(rootfolder);
		String[] all = dir.list();
		BetSizesGraphs stats = new BetSizesGraphs();
		time = System.currentTimeMillis();
		for (int i = 0; i < all.length; i++) if (all[i].startsWith("pokerai.org.sample" + Consts.SITE_PS /*+ "_" + Consts.HOLDEM_NL + "_9"*/) && all[i].endsWith(".hhex")) {
			stats.scan(rootfolder, all[i]);
		}
		stats.printStats();
		stats.createCharts();
	}

	static long time = 0, hmTimeRead = 0, hmTimeParse = 0, size = 0;
	double totalHands = 0, matchingHands = 0;
	// calls
	double sumPreCalls = 0, sumPreCallToStackRatio = 0, sumPreCallToBBRatio = 0, sumPreCallToPotRatio = 0;
	double sumFlopCalls = 0, sumFlopCallToStackRatio = 0, sumFlopCallToBBRatio = 0, sumFlopCallToPotRatio = 0;
	double sumTurnCalls = 0, sumTurnCallToStackRatio = 0, sumTurnCallToBBRatio = 0, sumTurnCallToPotRatio = 0;
	double sumRiverCalls = 0, sumRiverCallToStackRatio = 0, sumRiverCallToBBRatio = 0, sumRiverCallToPotRatio = 0;
	XYSeries preCallSeries = new XYSeries("Preflop: Call/Stack Ratio vs. Call/Pot Ratio");
	XYSeries flopCallSeries = new XYSeries("Flop: Call/Stack Ratio vs. Call/Pot Ratio");
	XYSeries turnCallSeries = new XYSeries("Turn: Call/Stack Ratio vs. Call/Pot Ratio");
	XYSeries riverCallSeries = new XYSeries("River: Call/Stack Ratio vs. Call/Pot Ratio");
	// bets
	double sumPreBets = 0, sumPreBetToStackRatio = 0, sumPreBetToBBRatio = 0, sumPreBetToPotRatio = 0;
	double sumFlopBets = 0, sumFlopBetToStackRatio = 0, sumFlopBetToBBRatio = 0, sumFlopBetToPotRatio = 0;
	double sumTurnBets = 0, sumTurnBetToStackRatio = 0, sumTurnBetToBBRatio = 0, sumTurnBetToPotRatio = 0;
	double sumRiverBets = 0, sumRiverBetToStackRatio = 0, sumRiverBetToBBRatio = 0, sumRiverBetToPotRatio = 0;
	XYSeries preBetToStackSeries = new XYSeries("Preflop: Bet To Stack Ratio");
	XYSeries preBetToBBSeries = new XYSeries("Preflop: Bet To BB Ratio");
	XYSeries preBetToPotSeries = new XYSeries("Preflop: Bet To Pot Ratio");
	XYSeries preBetSeries = new XYSeries("Preflop: Bet/Stack Ratio vs. Bet/Pot Ratio");
	XYSeries flopBetSeries = new XYSeries("Flop: Bet/Stack Ratio vs. Bet/Pot Ratio");
	XYSeries turnBetSeries = new XYSeries("Turn: Bet/Stack Ratio vs. Bet/Pot Ratio");
	XYSeries riverBetSeries = new XYSeries("River: Bet/Stack Ratio vs. Bet/Pot Ratio");
	// raises
	double sumPreRaises = 0, sumPreRaiseToStackRatio = 0, sumPreRaiseToBBRatio = 0, sumPreRaiseToPotRatio = 0;
	double sumFlopRaises = 0, sumFlopRaiseToStackRatio = 0, sumFlopRaiseToBBRatio = 0, sumFlopRaiseToPotRatio = 0;
	double sumTurnRaises = 0, sumTurnRaiseToStackRatio = 0, sumTurnRaiseToBBRatio = 0, sumTurnRaiseToPotRatio = 0;
	double sumRiverRaises = 0, sumRiverRaiseToStackRatio = 0, sumRiverRaiseToBBRatio = 0, sumRiverRaiseToPotRatio = 0;
	XYSeries preRaiseSeries = new XYSeries("Preflop: Raise/Stack Ratio vs. Raise/Pot Ratio");
	XYSeries flopRaiseSeries = new XYSeries("Flop: Raise/Stack Ratio vs. Raise/Pot Ratio");
	XYSeries turnRaiseSeries = new XYSeries("Turn: Raise/Stack Ratio vs. Raise/Pot Ratio");
	XYSeries riverRaiseSeries = new XYSeries("River: Raise/Stack Ratio vs. Raise/Pot Ratio");

	public void scan(String rootfolder, String fullName) {
		HandManagerNIO hm = new HandManagerNIO();
		size += hm.init(rootfolder, fullName);
		hm.reset();
		while (hm.hasMoreHands()) {
			PokerHand hand = hm.nextPokerHand();
			totalHands++;
			if (totalHands % 10000000 == 0) 
				System.out.println((long)totalHands + " hands read. " + (long)matchingHands + " matching hands so far.");
			if (hand.getGameType() == Consts.HOLDEM_NL && hand.getNumberOfSeats() == 2) {
				matchingHands++;
				double pot = 0;
				int[] stacks = hand.getStacks();
				if(hand.aactionsPreflop() != null) {
					// process preflop actions
					boolean firstin = false;
					for (Action action : hand.aactionsPreflop()) {
						double amount = action.getAmount();
						byte act = action.getAction();
						byte playerSeatId = action.getPlayerSeatId();
						if(act == Consts.ACTION_BIGBLIND || act == Consts.ACTION_BOTHBLINDS)
							firstin = true;
						if(act == Consts.ACTION_CALL) {
							double stack = stacks[playerSeatId];
							sumPreCallToStackRatio += amount / stack;
							sumPreCallToBBRatio += amount / 100;
							sumPreCallToPotRatio += amount / pot;
							sumPreCalls++;
							
							preCallSeries.add(amount/stack, amount/pot);
						} else if(firstin && act == Consts.ACTION_RAISE) {
							// consider firstin raise preflop as bet as there are no bets preflop otherwise!
							double stack = stacks[playerSeatId];
							sumPreBetToStackRatio += amount / stack;
							sumPreBetToBBRatio += amount / 100;
							sumPreBetToPotRatio += amount / pot;
							sumPreBets++;
							firstin = false;
							
							preBetToStackSeries.add(amount, stack);
							preBetToBBSeries.add(amount, 100);
							preBetToPotSeries.add(amount, pot);
							preBetSeries.add(amount/stack, amount/pot);
						} else if(!firstin && act == Consts.ACTION_RAISE) {
							double stack = stacks[playerSeatId];
							sumPreRaiseToStackRatio += amount / stack;
							sumPreRaiseToBBRatio += amount / 100;
							sumPreRaiseToPotRatio += amount / pot;
							sumPreRaises++;
							
							preRaiseSeries.add(amount/stack, amount/pot);
						}
						if(act == Consts.ACTION_SMALLBLIND || act == Consts.ACTION_BIGBLIND || act == Consts.ACTION_BOTHBLINDS || act == Consts.ACTION_CALL || act == Consts.ACTION_BET || act == Consts.ACTION_RAISE) {
							pot += amount;
							if (playerSeatId < stacks.length)
								stacks[playerSeatId] -= amount;
						}
					}
				}
				if(hand.aactionsFlop() != null) {
					// process flop actions
					for (Action action : hand.aactionsFlop()) {
						double amount = action.getAmount();
						byte act = action.getAction();
						byte playerSeatId = action.getPlayerSeatId();
						if(act == Consts.ACTION_CALL) {
							double stack = stacks[playerSeatId];
							sumFlopCallToStackRatio += amount / stack;
							sumFlopCallToBBRatio += amount / 100;
							sumFlopCallToPotRatio += amount / pot;
							sumFlopCalls++;
							
							flopCallSeries.add(amount/stack, amount/pot);
						} else if(act == Consts.ACTION_BET) {
							double stack = stacks[playerSeatId];
							sumFlopBetToStackRatio += amount / stack;
							sumFlopBetToBBRatio += amount / 100;
							sumFlopBetToPotRatio += amount / pot;
							sumFlopBets++;
							
							flopBetSeries.add(amount/stack, amount/pot);
						} else if(act == Consts.ACTION_RAISE) {
							double stack = stacks[playerSeatId];
							sumFlopRaiseToStackRatio += amount / stack;
							sumFlopRaiseToBBRatio += amount / 100;
							sumFlopRaiseToPotRatio += amount / pot;
							sumFlopRaises++;
							
							flopRaiseSeries.add(amount/stack, amount/pot);
						}
						if(act == Consts.ACTION_CALL || act == Consts.ACTION_BET || act == Consts.ACTION_RAISE) {
							pot += amount;
							if (playerSeatId < stacks.length)
								stacks[playerSeatId] -= amount;
						}
					}
				}
				if(hand.aactionsTurn() != null) {
					// process turn actions
					for (Action action : hand.aactionsTurn()) {
						double amount = action.getAmount();
						byte act = action.getAction();
						byte playerSeatId = action.getPlayerSeatId();
						if(act == Consts.ACTION_CALL) {
							double stack = stacks[playerSeatId];
							sumTurnCallToStackRatio += amount / stack;
							sumTurnCallToBBRatio += amount / 100;
							sumTurnCallToPotRatio += amount / pot;
							sumTurnCalls++;
							
							turnCallSeries.add(amount/stack, amount/pot);
						} else if(act == Consts.ACTION_BET) {
							double stack = stacks[playerSeatId];
							sumTurnBetToStackRatio += amount / stack;
							sumTurnBetToBBRatio += amount / 100;
							sumTurnBetToPotRatio += amount / pot;
							sumTurnBets++;
							
							turnBetSeries.add(amount/stack, amount/pot);
						} else if(act == Consts.ACTION_RAISE) {
							double stack = stacks[playerSeatId];
							sumTurnRaiseToStackRatio += amount / stack;
							sumTurnRaiseToBBRatio += amount / 100;
							sumTurnRaiseToPotRatio += amount / pot;
							sumTurnRaises++;
							
							turnRaiseSeries.add(amount/stack, amount/pot);
						}
						if(act == Consts.ACTION_CALL || act == Consts.ACTION_BET || act == Consts.ACTION_RAISE) {
							pot += amount;
							if (playerSeatId < stacks.length)
								stacks[playerSeatId] -= amount;
						}
					}
				}
				if(hand.aactionsRiver() != null) {
					// process river actions
					for (Action action : hand.aactionsRiver()) {
						double amount = action.getAmount();
						byte act = action.getAction();
						byte playerSeatId = action.getPlayerSeatId();
						if(act == Consts.ACTION_CALL) {
							double stack = stacks[playerSeatId];
							sumRiverCallToStackRatio += amount / stack;
							sumRiverCallToBBRatio += amount / 100;
							sumRiverCallToPotRatio += amount / pot;
							sumRiverCalls++;
							
							riverCallSeries.add(amount/stack, amount/pot);
						} else if(act == Consts.ACTION_BET) {
							double stack = stacks[playerSeatId];
							sumRiverBetToStackRatio += amount / stack;
							sumRiverBetToBBRatio += amount / 100;
							sumRiverBetToPotRatio += amount / pot;
							sumRiverBets++;
							
							riverBetSeries.add(amount/stack, amount/pot);
						} else if(act == Consts.ACTION_RAISE) {
							double stack = stacks[playerSeatId];
							sumRiverRaiseToStackRatio += amount / stack;
							sumRiverRaiseToBBRatio += amount / 100;
							sumRiverRaiseToPotRatio += amount / pot;
							sumRiverRaises++;
							
							riverRaiseSeries.add(amount/stack, amount/pot);
						}
						if(act == Consts.ACTION_CALL || act == Consts.ACTION_BET || act == Consts.ACTION_RAISE) {
							pot += amount;
							if (playerSeatId < stacks.length)
								stacks[playerSeatId] -= amount;
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
		System.out.println("*** Average Call To X Statistics ***");
		System.out.println("Round\\Ratio\tStack\tPot\tBB");
		System.out.println("Pre:\t\t" + round(sumPreCallToStackRatio/sumPreCalls, -3) + "\t" + round(sumPreCallToPotRatio/sumPreCalls,-3) + "\t" + round(sumPreCallToBBRatio/sumPreCalls,-3));
		System.out.println("Flop:\t\t" + round(sumFlopCallToStackRatio/sumFlopCalls, -3) + "\t" + round(sumFlopCallToPotRatio/sumFlopCalls, -3) + "\t" + round(round(sumFlopCallToBBRatio/sumFlopCalls, -3),-3));
		System.out.println("Turn:\t\t" + round(sumTurnCallToStackRatio/sumTurnCalls, -3) + "\t" + round(sumTurnCallToPotRatio/sumTurnCalls, -3) + "\t" + round(sumTurnCallToBBRatio/sumTurnCalls, -3));
		System.out.println("River:\t\t" + round(sumRiverCallToStackRatio/sumRiverCalls, -3) + "\t" + round(sumRiverCallToPotRatio/sumRiverCalls, -3) + "\t" + round(sumRiverCallToBBRatio/sumRiverCalls, -3));
		System.out.println("*** Average Bet To X Statistics ***");
		System.out.println("Round\\Ratio\tStack\tPot\tBB");
		System.out.println("Pre:\t\t" + round(sumPreBetToStackRatio/sumPreBets, -3) + "\t" + round(sumPreBetToPotRatio/sumPreBets,-3) + "\t" + round(sumPreBetToBBRatio/sumPreBets,-3));
		System.out.println("Flop:\t\t" + round(sumFlopBetToStackRatio/sumFlopBets, -3) + "\t" + round(sumFlopBetToPotRatio/sumFlopBets, -3) + "\t" + round(round(sumFlopBetToBBRatio/sumFlopBets, -3),-3));
		System.out.println("Turn:\t\t" + round(sumTurnBetToStackRatio/sumTurnBets, -3) + "\t" + round(sumTurnBetToPotRatio/sumTurnBets, -3) + "\t" + round(sumTurnBetToBBRatio/sumTurnBets, -3));
		System.out.println("River:\t\t" + round(sumRiverBetToStackRatio/sumRiverBets, -3) + "\t" + round(sumRiverBetToPotRatio/sumRiverBets, -3) + "\t" + round(sumRiverBetToBBRatio/sumRiverBets, -3));
		System.out.println("*** Average Raise To X Statistics ***");
		System.out.println("Round\\Ratio\tStack\tPot\tBB");
		System.out.println("Pre:\t\t" + round(sumPreRaiseToStackRatio/sumPreRaises, -3) + "\t" + round(sumPreRaiseToPotRatio/sumPreRaises,-3) + "\t" + round(sumPreRaiseToBBRatio/sumPreRaises,-3));
		System.out.println("Flop:\t\t" + round(sumFlopRaiseToStackRatio/sumFlopRaises, -3) + "\t" + round(sumFlopRaiseToPotRatio/sumFlopRaises, -3) + "\t" + round(round(sumFlopRaiseToBBRatio/sumFlopRaises, -3),-3));
		System.out.println("Turn:\t\t" + round(sumTurnRaiseToStackRatio/sumTurnRaises, -3) + "\t" + round(sumTurnRaiseToPotRatio/sumTurnRaises, -3) + "\t" + round(sumTurnRaiseToBBRatio/sumTurnRaises, -3));
		System.out.println("River:\t\t" + round(sumRiverRaiseToStackRatio/sumRiverRaises, -3) + "\t" + round(sumRiverRaiseToPotRatio/sumRiverRaises, -3) + "\t" + round(sumRiverRaiseToBBRatio/sumRiverRaises, -3));
		
		System.out.println("Number of hands: " + (long)matchingHands + " (of total " + (long)totalHands + "), " + (size / totalHands) + " bytes/hand");
	    long time2 = System.currentTimeMillis() - time;
	    long handsPerSecond = (long)(((1000.0*totalHands) / time2)); if (handsPerSecond == 0) handsPerSecond++;
	    System.out.println("Read & processing speed: " + handsPerSecond + " hands/second. ");
	    System.out.println("Read & processing time: " + (long)(time2/1000.0) + " second. ");
	    System.out.println("Read time (ms): " + hmTimeRead + ", parse time (ms): " + hmTimeParse);
	    System.out.println("Estimated time to read & process 600 million hands: " + (600000000 / handsPerSecond)
	            + " seconds (" + (((600000000 / handsPerSecond)+30)/60) + " minutes). ");
	}
	
	public void createCharts() {
		// preflop charts
		XYDataset xyDataset = new XYSeriesCollection(preBetToBBSeries);
		JFreeChart preBetToBBChart = ChartFactory.createScatterPlot((String)preBetToBBSeries.getKey(), "Amount", "BB", xyDataset, PlotOrientation.VERTICAL, false, false, false);
		preBetToBBChart.setBackgroundPaint(Color.WHITE);
		XYPlot plot = (XYPlot) preBetToBBChart.getPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.setRenderer(new XYDotRenderer());
		
		xyDataset = new XYSeriesCollection(preBetToStackSeries);
		JFreeChart preBetToStackChart = ChartFactory.createScatterPlot((String)preBetToStackSeries.getKey(), "Amount", "Stack", xyDataset, PlotOrientation.VERTICAL, false, false, false);
		preBetToStackChart.setBackgroundPaint(Color.WHITE);
		plot = (XYPlot) preBetToStackChart.getPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.setRenderer(new XYDotRenderer());
				
		xyDataset = new XYSeriesCollection(preBetToPotSeries);
		JFreeChart preBetToPotChart = ChartFactory.createScatterPlot((String)preBetToPotSeries.getKey(), "Amount", "Pot", xyDataset, PlotOrientation.VERTICAL, false, false, false);
		preBetToPotChart.setBackgroundPaint(Color.WHITE);
		plot = (XYPlot) preBetToPotChart.getPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.setRenderer(new XYDotRenderer());
		
		// call charts
		xyDataset = new XYSeriesCollection(preCallSeries);
		JFreeChart preCallChart = ChartFactory.createScatterPlot((String)preCallSeries.getKey(), "Amount/Stack Ratio", "Amount/Pot Ratio", xyDataset, PlotOrientation.VERTICAL, false, false, false);
		preCallChart.setBackgroundPaint(Color.WHITE);
		plot = (XYPlot) preCallChart.getPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.setRenderer(new XYDotRenderer());
		
		xyDataset = new XYSeriesCollection(flopCallSeries);
		JFreeChart flopCallChart = ChartFactory.createScatterPlot((String)flopCallSeries.getKey(), "Amount/Stack Ratio", "Amount/Pot Ratio", xyDataset, PlotOrientation.VERTICAL, false, false, false);
		flopCallChart.setBackgroundPaint(Color.WHITE);
		plot = (XYPlot) flopCallChart.getPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.setRenderer(new XYDotRenderer());
		
		xyDataset = new XYSeriesCollection(turnCallSeries);
		JFreeChart turnCallChart = ChartFactory.createScatterPlot((String)turnCallSeries.getKey(), "Amount/Stack Ratio", "Amount/Pot Ratio", xyDataset, PlotOrientation.VERTICAL, false, false, false);
		turnCallChart.setBackgroundPaint(Color.WHITE);
		plot = (XYPlot) turnCallChart.getPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.setRenderer(new XYDotRenderer());
		
		xyDataset = new XYSeriesCollection(riverCallSeries);
		JFreeChart riverCallChart = ChartFactory.createScatterPlot((String)riverCallSeries.getKey(), "Amount/Stack Ratio", "Amount/Pot Ratio", xyDataset, PlotOrientation.VERTICAL, false, false, false);
		riverCallChart.setBackgroundPaint(Color.WHITE);
		plot = (XYPlot) riverCallChart.getPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.setRenderer(new XYDotRenderer());
		
		// bet charts
		xyDataset = new XYSeriesCollection(preBetSeries);
		JFreeChart preBetChart = ChartFactory.createScatterPlot((String)preBetSeries.getKey(), "Amount/Stack Ratio", "Amount/Pot Ratio", xyDataset, PlotOrientation.VERTICAL, false, false, false);
		preBetChart.setBackgroundPaint(Color.WHITE);
		plot = (XYPlot) preBetChart.getPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.setRenderer(new XYDotRenderer());
		
		xyDataset = new XYSeriesCollection(flopBetSeries);
		JFreeChart flopBetChart = ChartFactory.createScatterPlot((String)flopBetSeries.getKey(), "Amount/Stack Ratio", "Amount/Pot Ratio", xyDataset, PlotOrientation.VERTICAL, false, false, false);
		flopBetChart.setBackgroundPaint(Color.WHITE);
		plot = (XYPlot) flopBetChart.getPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.setRenderer(new XYDotRenderer());
		
		xyDataset = new XYSeriesCollection(turnBetSeries);
		JFreeChart turnBetChart = ChartFactory.createScatterPlot((String)turnBetSeries.getKey(), "Amount/Stack Ratio", "Amount/Pot Ratio", xyDataset, PlotOrientation.VERTICAL, false, false, false);
		turnBetChart.setBackgroundPaint(Color.WHITE);
		plot = (XYPlot) turnBetChart.getPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.setRenderer(new XYDotRenderer());
		
		xyDataset = new XYSeriesCollection(riverBetSeries);
		JFreeChart riverBetChart = ChartFactory.createScatterPlot((String)riverBetSeries.getKey(), "Amount/Stack Ratio", "Amount/Pot Ratio", xyDataset, PlotOrientation.VERTICAL, false, false, false);
		riverBetChart.setBackgroundPaint(Color.WHITE);
		plot = (XYPlot) riverBetChart.getPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.setRenderer(new XYDotRenderer());
		
		// raise charts
		xyDataset = new XYSeriesCollection(preRaiseSeries);
		JFreeChart preRaiseChart = ChartFactory.createScatterPlot((String)preRaiseSeries.getKey(), "Amount/Stack Ratio", "Amount/Pot Ratio", xyDataset, PlotOrientation.VERTICAL, false, false, false);
		preRaiseChart.setBackgroundPaint(Color.WHITE);
		plot = (XYPlot) preRaiseChart.getPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.setRenderer(new XYDotRenderer());
		
		xyDataset = new XYSeriesCollection(flopRaiseSeries);
		JFreeChart flopRaiseChart = ChartFactory.createScatterPlot((String)flopRaiseSeries.getKey(), "Amount/Stack Ratio", "Amount/Pot Ratio", xyDataset, PlotOrientation.VERTICAL, false, false, false);
		flopRaiseChart.setBackgroundPaint(Color.WHITE);
		plot = (XYPlot) flopRaiseChart.getPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.setRenderer(new XYDotRenderer());
		
		xyDataset = new XYSeriesCollection(turnRaiseSeries);
		JFreeChart turnRaiseChart = ChartFactory.createScatterPlot((String)turnRaiseSeries.getKey(), "Amount/Stack Ratio", "Amount/Pot Ratio", xyDataset, PlotOrientation.VERTICAL, false, false, false);
		turnRaiseChart.setBackgroundPaint(Color.WHITE);
		plot = (XYPlot) turnRaiseChart.getPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.setRenderer(new XYDotRenderer());
		
		xyDataset = new XYSeriesCollection(riverRaiseSeries);
		JFreeChart riverRaiseChart = ChartFactory.createScatterPlot((String)riverRaiseSeries.getKey(), "Amount/Stack Ratio", "Amount/Pot Ratio", xyDataset, PlotOrientation.VERTICAL, false, false, false);
		riverRaiseChart.setBackgroundPaint(Color.WHITE);
		plot = (XYPlot) riverRaiseChart.getPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.setRenderer(new XYDotRenderer());
		
		try {
			int width = 700, height = 400;
			ChartUtilities.saveChartAsJPEG(new File("preBetToBBChart.jpg"), preBetToBBChart, width, height);
			ChartUtilities.saveChartAsJPEG(new File("preBetToStackChart.jpg"), preBetToStackChart, width, height);
			ChartUtilities.saveChartAsJPEG(new File("preBetToPotChart.jpg"), preBetToPotChart, width, height);
			
			ChartUtilities.saveChartAsJPEG(new File("preCallChart.jpg"), preCallChart, width, height);
			ChartUtilities.saveChartAsJPEG(new File("flopCallChart.jpg"), flopCallChart, width, height);
			ChartUtilities.saveChartAsJPEG(new File("turnCallChart.jpg"), turnCallChart, width, height);
			ChartUtilities.saveChartAsJPEG(new File("riverCallChart.jpg"), riverCallChart, width, height);
			
			ChartUtilities.saveChartAsJPEG(new File("preBetChart.jpg"), preBetChart, width, height);
			ChartUtilities.saveChartAsJPEG(new File("flopBetChart.jpg"), flopBetChart, width, height);
			ChartUtilities.saveChartAsJPEG(new File("turnBetChart.jpg"), turnBetChart, width, height);
			ChartUtilities.saveChartAsJPEG(new File("riverBetChart.jpg"), riverBetChart, width, height);
			
			ChartUtilities.saveChartAsJPEG(new File("preRaiseChart.jpg"), preRaiseChart, width, height);
			ChartUtilities.saveChartAsJPEG(new File("flopRaiseChart.jpg"), flopRaiseChart, width, height);
			ChartUtilities.saveChartAsJPEG(new File("turnRaiseChart.jpg"), turnRaiseChart, width, height);
			ChartUtilities.saveChartAsJPEG(new File("riverRaiseChart.jpg"), riverRaiseChart, width, height);
			
			
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public double round(double num, int pow) {
		return (Math.round(num*Math.pow(10, -1*pow)))/Math.pow(10, -1*pow);
	}
}
