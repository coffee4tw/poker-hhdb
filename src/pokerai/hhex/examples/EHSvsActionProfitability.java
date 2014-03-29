package pokerai.hhex.examples;

import java.io.File;
import java.util.ArrayList;

import pokerai.hhex.Action;
import pokerai.hhex.Consts;
import pokerai.hhex.PokerHand;
import pokerai.hhex.internal.HandManagerNIO;
import PSim.PSim;
import PSim.SimResults;

public class EHSvsActionProfitability {
	public static void main(String[] args) throws InterruptedException {
		String rootfolder = Data.folder+"hhdb_sample\\";
		if (args.length > 0) rootfolder = args[0];
		if (!rootfolder.endsWith("\\")) rootfolder += "\\";
		File dir = new File(rootfolder);
		String[] all = dir.list();
		ArrayList<ProcessingThread> runningThreads = new ArrayList<ProcessingThread>();
		Data data = new Data(System.currentTimeMillis());
		for (int i = 0; i < all.length; i++) 
			if (all[i].startsWith("pokerai.org.sample" + Consts.SITE_PS) && all[i].endsWith(".hhex")) {
				ProcessingThread thread = new ProcessingThread(data, rootfolder, all[i]);
				thread.start();
				runningThreads.add(thread);
				if (runningThreads.size() >= Runtime.getRuntime().availableProcessors()) {
					while (runningThreads.size() >= Runtime.getRuntime().availableProcessors()) {
						Thread.sleep(100);
						for (Thread t : runningThreads)
							if (!t.isAlive()) {
								runningThreads.remove(t);
								break;
							}
					}
				}
			}
		for (ProcessingThread t : runningThreads) {
			t.join();
		}
		data.printStats();
	}
}

/** Data structure to synchronize access of threads on the data **/
class Data {
	/** ----- CONFIG VALUES -------- **/
	static final String folder = "C:\\Dokumente und Einstellungen\\Thorsten\\Desktop\\HHEX\\";
	static final int eHSsteps = 2;
	/** ----------------------------- **/
	
	static final int HU = 0, SH = 1, FR = 2;
	static final int PREFLOP = 0, FLOP = 1, TURN = 2, RIVER = 3;
	static final int CALL = 0, BET = 1, RAISE = 2, _3BET = 3, _4BET = 4, _5BET = 5;
	
	int[][][][][] sumProfits = new int[3][][][][];
	int[][][][][] sumActions = new int[3][][][][];
	
	long time = 0, hmTimeRead = 0, hmTimeParse = 0, size = 0;
	long totalHands = 0, matchingHands = 0;
	Data(long time) {
		this.time = time;
		for (int i=0; i<sumProfits.length; i++) {
			int seats = 0;
			if (i == HU) seats = 2;
			else if (i == SH) seats = 6;
			else if (i == FR) seats = 9;
			sumProfits[i] = new int[seats][4][6][100/eHSsteps+1];
			sumActions[i] = new int[seats][4][6][100/eHSsteps+1];
		}
	}
	synchronized void incTotalHands() { 
		totalHands++; 
	}
	synchronized void incMatchingHands() { 
		matchingHands++; 
	}
	synchronized void addSize(long value) { 
		size += value; 
	}
	synchronized void addHmTimeRead(long value) { 
		hmTimeRead += value; 
	}
	synchronized void addHmTimeParse(long value) { 
		hmTimeParse += value; 
	}
	synchronized void addSumProfits(int game, int seat, int stage, int action, int ehs, int value) { 
		sumProfits[game][seat][stage][action][ehs] += value; 
	}
	synchronized void addSumActions(int game, int seat, int stage, int action, int ehs) { 
		sumActions[game][seat][stage][action][ehs]++; 
	}
	
	public float round(double num) {
		int precision = 3;
		float p = (float) Math.pow(10, precision);
		num = num * p;
		float tmp = Math.round(num);
		return (float)tmp/p;
	}

	public void printStats() {
		// print out results here	

		for (int game = 0; game<sumProfits.length; game++) {
			if (game == HU) System.out.println("\t+++ HU (2max) +++");
			else if (game == SH) System.out.println("\t+++ SH (6max) +++");
			else if (game == FR) System.out.println("\t+++ FR (9max) +++");

			for (int seat = 0; seat < sumProfits[game].length; seat++) {
				System.out.println("Seat " + seat);

				for (int round = 0; round<sumProfits[game][seat].length; round++) {
					if (round == PREFLOP) System.out.println("*** Preflop eHS Statistics ***");
					else if (round == FLOP) System.out.println("*** Flop eHS Statistics ***");
					else if (round == TURN) System.out.println("*** Turn eHS Statistics ***");
					else if (round == RIVER) System.out.println("*** River eHS Statistics ***");
					System.out.print("EHS:\t\t");
					for (int i=0; i<100/eHSsteps+1; i++) 
						System.out.print(i*eHSsteps + "\t");
					System.out.println();

					for (int action = 0; action<sumProfits[game][seat][round].length; action++) {
						if (action == CALL) System.out.print("CALL:\t\t");
						else if (action == BET) System.out.print("BET:\t\t");
						else if (action == RAISE) System.out.print("RAISE:\t\t");
						else if (action == _3BET) System.out.print("3BET:\t\t");
						else if (action == _4BET) System.out.print("4BET:\t\t");
						else if (action == _5BET) System.out.print("5+BET:\t\t");

						for (int ehs = 0; ehs<sumProfits[game][seat][round][action].length; ehs++) {
							if (sumActions[game][seat][round][action][ehs] > 0)
								System.out.print(round(sumProfits[game][seat][round][action][ehs]/sumActions[game][seat][round][action][ehs]) + "\t");
							else
								System.out.print("-\t");
						}
						System.out.println();
					}
				}
				System.out.println();
			}
		}
		System.out.println("Number of hands: " + matchingHands + " (" + round(((float)matchingHands*100)/totalHands) + "% of total " + totalHands + "), " + (size / totalHands) + " bytes/hand");
		long time2 = System.currentTimeMillis() - time;
		long handsPerSecond = (long)(((1000.0*totalHands) / time2)); if (handsPerSecond == 0) handsPerSecond++;
		System.out.println("Read & processing speed: " + handsPerSecond + " hands/second. ");
		System.out.println("Read & processing time: " + (long)(time2/1000.0) + " second. ");
		System.out.println("Read time (ms): " + hmTimeRead + ", parse time (ms): " + hmTimeParse);
		System.out.println("Estimated time to read & process 600 million hands: " + (600000000 / handsPerSecond)
				+ " seconds (" + (((600000000 / handsPerSecond)+30)/60) + " minutes). ");
	}
}

class ProcessingThread extends Thread {
	private Data data;
	private String rootfolder;
	private String fileName;

	ProcessingThread(Data data, String rootfolder, String filename) {
		this.data = data;
		this.rootfolder = rootfolder;
		this.fileName = filename;
	}

	public void run() {
		System.out.println("Started new thread with: " + this.rootfolder + ", " + this.fileName);
		scan(this.rootfolder, this.fileName);
	}

	public void scan(String rootfolder, String fullName) {
		HandManagerNIO hm = new HandManagerNIO();
		data.addSize(hm.init(rootfolder, fullName));
		hm.reset();
		while (hm.hasMoreHands()) {
			PokerHand hand = hm.nextPokerHand();
			data.incTotalHands();
			if (data.totalHands % 10000000 == 0) 
				System.out.println((long)data.totalHands + " hands read. " + (long)data.matchingHands + " matching hands so far.");

			// check only NL hands that went to showdown
			if (hand.getGameType() == Consts.HOLDEM_NL && hand.getLastRound() == Consts.STREET_SHOWDOWN) {
				int game = -1;
				if (hand.getNumberOfSeats() == 2)
					game = Data.HU;
				else if (hand.getNumberOfSeats() == 6)
					game = Data.SH;
				else if (hand.getNumberOfSeats() == 9)
					game = Data.FR;

				// new matching hand found
				data.incMatchingHands();

				// cycle through all four stage (PREFLOP, FLOP, TURN, RIVER)
				for (int stage = 0; stage<4; stage++) {

					// get actions from the current stage
					Action[] actions = null;
					if (stage == Data.PREFLOP) actions = hand.aactionsPreflop();
					else if (stage == Data.FLOP) actions = hand.aactionsFlop();
					else if (stage == Data.TURN) actions = hand.aactionsTurn();
					else if (stage == Data.RIVER) actions = hand.aactionsRiver();

					// if no actions break loop
					if (actions == null)
						break;

					// firstin value, only needed for preflop
					boolean firstin = false;
					int numRaises = 0;

					// calc eHS values for each player involved until showdown
					int[] eHS = new int[data.sumProfits[game].length];

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
						byte act = action.getAction();
						byte playerSeatId = action.getPlayerSeatId();
						if (playerSeatId > hand.getNumberOfSeats())
							continue;

						// if eHS isnt set yet, calculate it
						if (eHS[playerSeatId] == 0) {
							SimResults r = new SimResults();
							String cards = hand.getCards(playerSeatId);
							if (cards.equals("--") || cards.equals("")) // player that didnt go to showdown acted, we dont know the cards
								continue;
							cards += hand.getCommunityCards();
							String temp = "";
							for(int i=0, j=2; j<cards.length(); i += 2, j +=2)
								temp += cards.substring(i,j) + " ";
							cards = temp.trim();
							PSim.SimulateHand(cards, r, 0, 1, 100);
							eHS[playerSeatId] = Math.round(r.winSd*100);
//							eHS[playerSeatId] = (int) (Math.random()*100);
						}					
						
						// get position for this player
						int position = -1;
						for (int i=0; i<positions.length; i++) {
							if(positions[i] == playerSeatId) {
								position = i;
								break;
							}
						}
						
						// if big blind or both blinds are posted by some player then the next bet will be a firstin bet
						if(act == Consts.ACTION_BIGBLIND || act == Consts.ACTION_BOTHBLINDS)
							firstin = true;
						else if(act == Consts.ACTION_CALL) {
							data.addSumProfits(game, position, stage, Data.CALL, eHS[playerSeatId]/Data.eHSsteps, (int)hand.getMoneyMade(playerSeatId));
							data.addSumActions(game, position, stage, Data.CALL, eHS[playerSeatId]/Data.eHSsteps);
						} else if(act == Consts.ACTION_BET || (stage == Data.PREFLOP && firstin && act == Consts.ACTION_RAISE)) {
							data.addSumProfits(game, position, stage, Data.BET, eHS[playerSeatId]/Data.eHSsteps, (int)hand.getMoneyMade(playerSeatId));
							data.addSumActions(game, position, stage, Data.BET, eHS[playerSeatId]/Data.eHSsteps);
							firstin = false;
						} else if(act == Consts.ACTION_RAISE) {
							int actionIndex = -1;
							if (numRaises == 0) actionIndex = Data.RAISE;
							else if (numRaises == 1) actionIndex = Data._3BET;
							else if (numRaises == 2) actionIndex = Data._4BET;
							else if (numRaises > 2) actionIndex = Data._5BET;
							data.addSumProfits(game, position, stage, actionIndex, eHS[playerSeatId]/Data.eHSsteps, (int)hand.getMoneyMade(playerSeatId));
							data.addSumActions(game, position, stage, actionIndex, eHS[playerSeatId]/Data.eHSsteps);
							numRaises++;
						}
					}
				}
			}
		}
		data.addHmTimeRead(hm.timeRead);
		data.addHmTimeParse(hm.timeParse);
		hm.closedb();
		System.out.println("Thread finished. Total hands parsed so far: " + data.totalHands + ", Matching hands so far: " + data.matchingHands);
	}
}

