package pokerai.hhex.examples;

import static pokerai.hhex.helper.GeneralHelper.*;

import pokerai.hhex.Action;
import pokerai.hhex.Consts;
import pokerai.hhex.HandProcessor;
import pokerai.hhex.PokerHand;

/**
 * 
 * Simple class to evaluate in which circumstances continuation bets work, and in which they don't.
 * Easy to extend with own "BoardProperties".
 * 
 * @author CodeD (http://pokerai.org/pf3)
 *
 */
public class ProcessContinuationBets extends HandProcessor {

	public static void main(String[] args) {
		ProcessContinuationBets pcb = new ProcessContinuationBets();
		if(args.length > 0) {
			pcb.setRootFolder(args[0]);
		}
		pcb.setGameTypes(Consts.HOLDEM_NL);
		pcb.setNumberOfPlayers(9);
		pcb.process(-1);
	}
	

	protected void processHand(PokerHand hand) {
		if(hand.aactionsPreflop() == null) {
			return;
		}
		
		// check for preflop betlevel of at least 2
		byte aggressor = -1;
		for (Action action : hand.aactionsPreflop()) {
			if(action.getAction() == Consts.ACTION_RAISE) {
				aggressor = action.getPlayerSeatId();
//					break;
			}
		}

		if(aggressor != -1 && hand.aactionsFlop() != null) {
			for (int i = 0; i < boardProperties.length; i++) {
				if(boardProperties[i].boardHasProperty(hand.getCommunityCardsBA())) {
					propertyTotalCount[i]++;
				}
			}
			
			boolean processedAggressor = false;
			boolean handProceededToTurn = hand.getCommunityCardsBA()[3] != 100;
			boolean contiBetWorked = !handProceededToTurn;
			boolean triedContiBet = false;
			for (Action action : hand.aactionsFlop()) {
				if(action.getPlayerSeatId() == aggressor) {
					if(!processedAggressor) {
						if(action.getAction() == Consts.ACTION_BET) {
							for (int i = 0; i < boardProperties.length; i++) {
								if(boardProperties[i].boardHasProperty(hand.getCommunityCardsBA())) {
									propertyContiBetCount[i]++;
								}
							}
							triedContiBet = true;
						} else if(action.getAction() == Consts.ACTION_CHECK) {
							for (int i = 0; i < boardProperties.length; i++) {
								if(boardProperties[i].boardHasProperty(hand.getCommunityCardsBA())) {
									propertyCheckCount[i]++;
								}
							}
						}
						processedAggressor = true;
					} else {
						if(!handProceededToTurn && action.getAction() == Consts.ACTION_FOLD) {
							// aggressor folded to raise/bet
							contiBetWorked = false;
						}
					}
				}
			}
			
			if(triedContiBet && contiBetWorked) {
				for (int i = 0; i < boardProperties.length; i++) {
					if(boardProperties[i].boardHasProperty(hand.getCommunityCardsBA())) {
						propertyContiBetsWorked[i]++;
					}
				}
			}
			
		}
	}


	
	protected void printResults()  {
		for (int i = 0; i < boardProperties.length; i++) {
			System.out.println(fillSpaces(boardProperties[i].propertyDescription(),28, false) + " " + fillSpaces("#: " + propertyTotalCount[i],10,false) +
					" \tContiBets: " + propertyContiBetCount[i] 
					+ " (" + decimalString(propertyContiBetCount[i]*100.0/propertyTotalCount[i], true) + "%)" + 
					" \tChecks: " + propertyCheckCount[i] 
					+ " (" + decimalString(propertyCheckCount[i]*100.0/propertyTotalCount[i], true) + "%)" + 
					" \tBet Into Aggressor: " + (propertyTotalCount[i] - propertyContiBetCount[i] - propertyCheckCount[i]) 
					+ " (" + decimalString((propertyTotalCount[i] - propertyContiBetCount[i] - propertyCheckCount[i])*100.0/propertyTotalCount[i], true) + "%)" +
					"   \tWorked " + propertyContiBetsWorked[i] + "/" + propertyContiBetCount[i] + " (" 
					+ decimalString(propertyContiBetsWorked[i]*100.0 / propertyContiBetCount[i], true) + "%)");
		}
	}
	
	
	
	
	/*
	 * 
	 * Board Properties
	 * 
	 * 
	 */
	
	
	
	private static BoardProperty[] boardProperties = new BoardProperty[]{
			
		new BoardProperty() {
			
			@Override
			public boolean boardHasProperty(byte[] board) {
				return true;
			}

			@Override
			public String propertyDescription() {return "Overall";}
		},

		new BoardProperty() {
			
			@Override
			public boolean boardHasProperty(byte[] board) {
				return board[0]/4 == 12 || board[1]/4 == 12 || board[2]/4 == 12;
			}

			@Override
			public String propertyDescription() {return "Ace Flopped";}
		},

		new BoardProperty() {
			
			@Override
			public boolean boardHasProperty(byte[] board) {
				return !(board[0]/4 == 12 || board[1]/4 == 12 || board[2]/4 == 12);
			}

			@Override
			public String propertyDescription() {return "No Ace Flopped";}
		},

		new BoardProperty() {
		
			@Override
			public boolean boardHasProperty(byte[] board) {
				int c = (board[0]/4 >= 9?1:0) + (board[1]/4 >= 9?1:0) + (board[2]/4 >= 9?1:0);
				return c == 1;
			}

			@Override
			public String propertyDescription() {return "1 Card >= Jack";}
		},
		
		new BoardProperty() {
			
			@Override
			public boolean boardHasProperty(byte[] board) {
				int c = (board[0]/4 >= 9?1:0) + (board[1]/4 >= 9?1:0) + (board[2]/4 >= 9?1:0);
				return c == 2;
			}

			@Override
			public String propertyDescription() {return "2 Card >= Jack";}
		},

		new BoardProperty() {
			
			@Override
			public boolean boardHasProperty(byte[] board) {
				int c = (board[0]/4 >= 9?1:0) + (board[1]/4 >= 9?1:0) + (board[2]/4 >= 9?1:0);
				return c == 3;
			}

			@Override
			public String propertyDescription() {return "All Cards >= Jack";}
		},
		
		new BoardProperty() {
			
			@Override
			public boolean boardHasProperty(byte[] board) {
				int c = (board[0]/4 >= 9?1:0) + (board[1]/4 >= 9?1:0) + (board[2]/4 >= 9?1:0);
				return c == 0;
			}

			@Override
			public String propertyDescription() {return "All Cards < Jack";}
		},
		
		new BoardProperty() {
			
			@Override
			public boolean boardHasProperty(byte[] board) {
				int c = (board[0]/4 >= 6?1:0) + (board[1]/4 >= 6?1:0) + (board[2]/4 >= 6?1:0);
				return c == 0;
			}

			@Override
			public String propertyDescription() {return "All Cards < 8";}
		},
		
		new BoardProperty() {
			
			@Override
			public boolean boardHasProperty(byte[] board) {
				int c = (board[0]/4 == board[1]/4?1:0) + (board[1]/4 == board[2]/4?1:0);
				return c == 0;
			}

			@Override
			public String propertyDescription() {return "UnPaired Flop";}
		},

		new BoardProperty() {
			
			@Override
			public boolean boardHasProperty(byte[] board) {
				int c = (board[0]/4 == board[1]/4?1:0) + (board[1]/4 == board[2]/4?1:0);
				return c == 1;
			}

			@Override
			public String propertyDescription() {return "Paired Flop";}
		},

		new BoardProperty() {
			
			@Override
			public boolean boardHasProperty(byte[] board) {
				int c = (board[0]/4 == board[1]/4?1:0) + (board[1]/4 == board[2]/4?1:0);
				return c == 2;
			}

			@Override
			public String propertyDescription() {return "3oaK Flop";}
		},

		new BoardProperty() {
			
			@Override
			public boolean boardHasProperty(byte[] board) {
				
				return board[0]%4 == board[1]%4 && board[1]%4 == board[2]%4;
			}

			@Override
			public String propertyDescription() {return "1 Suit Flop";}
		},

		new BoardProperty() {
			
			@Override
			public boolean boardHasProperty(byte[] board) {
				
				return board[0]%4 != board[1]%4 && board[1]%4 != board[2]%4;
			}

			@Override
			public String propertyDescription() {return "Rainbow Flop";}
		},

		new BoardProperty() {
			
			@Override
			public boolean boardHasProperty(byte[] board) {
				int c = (board[0]/4 == board[1]/4?1:0) + (board[1]/4 == board[2]/4?1:0);
				if(c == 0) {
					int r1 = board[0]/4, r2 = board[1]/4, r3 = board[2]/4, t;
					if(r3 > r2) {
						t = r2; r2 = r3; r3 = t;
					}
					if(r2 > r1) {
						t = r1; r1 = r2; r2 = t;
					}
					if(r3 > r2) {
						t = r2; r2 = r3; r3 = t;
					}
					int hd = (r1-r2), ld = (r2-r3);
					if(r1 == r2) { // special case: ace
						int a_hd = (r2-r3), a_ld = (r3 - (-1)); // -1 as low ace
						if(hd + ld > a_hd + a_ld) {
							hd = a_hd;
							ld = a_ld;
						}
					}
					return 1 <= hd && 1 <= ld && hd < 2 && ld < 2;
				}
				return false;
			}

			@Override
			public String propertyDescription() {return "Straight Factor: 1|1 (High)";}
		},

		new BoardProperty() {
			
			@Override
			public boolean boardHasProperty(byte[] board) {
				int c = (board[0]/4 == board[1]/4?1:0) + (board[1]/4 == board[2]/4?1:0);
				if(c == 0) {
					int r1 = board[0]/4, r2 = board[1]/4, r3 = board[2]/4, t;
					if(r3 > r2) {
						t = r2; r2 = r3; r3 = t;
					}
					if(r2 > r1) {
						t = r1; r1 = r2; r2 = t;
					}
					if(r3 > r2) {
						t = r2; r2 = r3; r3 = t;
					}
					int hd = (r1-r2), ld = (r2-r3);
					if(r1 == r2) { // special case: ace
						int a_hd = (r2-r3), a_ld = (r3 - (-1)); // -1 as low ace
						if(hd + ld > a_hd + a_ld) {
							hd = a_hd;
							ld = a_ld;
						}
					}
					return 4 <= hd && 4 <= ld;
				}
				return false;
			}

			@Override
			public String propertyDescription() {return "Straight Factor: 4|* (Low)";}
		},

	};
	
	
	private static long[] propertyTotalCount = new long[boardProperties.length];
	private static long[] propertyCheckCount = new long[boardProperties.length];
	private static long[] propertyContiBetCount = new long[boardProperties.length];
	private static long[] propertyContiBetsWorked = new long[boardProperties.length];
}
