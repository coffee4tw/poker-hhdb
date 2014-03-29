
/*
  This code is released under GPL v3.
  See: http://pokerftp.com/index_files/about.htm

  @Author: spadebidder@earthlink.net
  
  Based on work by Indiana but modified to use showdown equity enumerator
  from http://www.brecware.com/Software/software.html
  Also modified scan method and output to properly account for ties.

  Calculates preflop equity for all 2-player allins using full enumeration of boards
  to calculate exact equity, and compares to actual wins.  This is slow and on a
  1.7Ghz Dell Laptop 1GB RAM w/ WinXP it will do about 6 headsup allins per second.
  At an average of 1 allin /180 hands that's about 1100 hand histories/second, or
  4+ million hands per hour.  A hundred million hand histories might take 24 hours
  on a similar processor.  A better computer could probably do it a lot faster.
  Without implementing magic shortcuts like those used in PokerStove, the enumeration
  code probably can't go faster.  A preflop headsup allin requires 1.71 million deals.
  
  The brecware enumerator can handle multi-player allins and allins after the flop with
  known board cards, so this class will be expanded for a more complete equity
  evaluation.  Coming soon.

  Note: This example needs HoldEmShowdown_spade.jar (which is a modded version of Steve Brecher's
  HoldemShowdown (also included in this distribution) in order to work.

 ----
 */

package pokerai.hhex.examples;

import com.stevebrecher.showdown.Showdown2;
import pokerai.hhex.PokerHand;
import pokerai.hhex.internal.HandManagerNIO;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class PreflopEquityVsWins {

   static String rootfolder = "C:\\hhdb\\";
   static int	step		= 100; // how many equity brackets
   static int	seats       = 6;
   static int	minseats    = 2;
   static int	maxseats    = 10;
   static int	site        = 0;  // gets cycled in scanner
   static byte	game        = 1;  
		   /*
		    *       sites                                games
		    * DEFAULT  =  0;                  UNKNOWN       =  0;
		    * PS       =  0;                  HOLDEM_NL     =  1;
		    * FTP      =  1;                  HOLDEM_PL     =  2;
		    * PARTY    =  2;                  HOLDEM_FL     =  3;
		    * IPN      =  3;                  OMAHA_NL      =  4;
		    * BOSS     =  4;                  OMAHA_PL      =  5;
		    * UB       =  5;                  OMAHA_FL      =  6;
		    * HHDB     = 50;
		    */

   static long          cards[]         = new long[13];
   private static int[]	index           = new int[500];
   static ArrayList     holeCardList2   = new ArrayList();
   static ArrayList     boardCardList2  = new ArrayList();
   static int[]         allins          = new int[step];
   static int[]         wins            = new int[step];
   static int[]			ties			= new int[step];
   static int           totalAllins     = 0;
   static int           totalHands      = 0;
   static double        deadevens_notie = 0;
   static double        deadevens_tie   = 0;
   static int 			bothlost		= 0;
   static int 			totalTies		= 0;
   static int 			totalWins		= 0;
   static int			dumpPoint		= 1000000000;  // reduce for testing
   static int			pingScreen		= 20000;  // we know its working
   static String viewHand;

  
   public static void main(String[] args) {
      if (args.length > 0)
         rootfolder = args[0];
      if (!rootfolder.endsWith("\\"))
         rootfolder += "\\";
      File     dir = new File(rootfolder);
      String[] all = dir.list();

      // Scan all files and count results as we go
      System.out.println(getDateTime() + "  begin analysing hands...");
      for (site = 0; site < 50; site++) {
         for (seats = minseats; seats < maxseats + 1; seats++) {
            for (int i = 0; i < all.length; i++) {
               if (all[i].startsWith("pokerai.org.sample" + site + "_" + game + "_" + seats)
                       && all[i].endsWith(".hhex")) {
                  scan(rootfolder, all[i]);
               }
            }
         }
	     if (totalHands>0) {
	        dump();
	        totalHands=0;
	     }
      }
   }

   static String formatString, formatString2, headerString;

   public static void dump() {
   	  System.out.println("\n");
   	  System.out.println("Site #" + site);
      System.out.println("Number of hands analysed: " + (long) totalHands);
      System.out.print("Preflop heads-up all-ins: " + totalAllins + " (Once per ");
      System.out.printf("%.0f", (1 / ((totalAllins / 1.0) / totalHands)));
      System.out.println(" hands).");
      if ((totalAllins/step)<10000) {
      	  formatString = " %6d";
      	  formatString2 = "%7.0f";
      	  headerString = "Preflop equity    Hands   Wins   Ties    Win%";
      } else {
      	  formatString = " %10d";
      	  formatString2 = "%11.0f";
      	  headerString = "Preflop equity        Hands       Wins       Ties    Win%";
      }  
  	  System.out.println();
  	  System.out.println(headerString);
      for (byte i = 0; i < step; i++) {
         System.out.print("[" + ds(i * (1.0 / step)) + " - " + ds((i + 1) * (1.0 / step)) + "]:  ");
         System.out.printf(formatString, allins[i]);
         System.out.printf(formatString, wins[i]);
         System.out.printf(formatString, ties[i]);
         if (Double.isNaN( (wins[i]+(ties[i]/2.0)) / (allins[i] * 1.0))) {
         	System.out.printf("%6.0f", 0.0);
         } else {
            System.out.printf("%8.3f",(wins[i]+(ties[i]/2.0)) / (allins[i] * 1.0) );
         }
         System.out.println();
         if (i==(step/2)-1) {
         	System.out.print("[exact 50/50]:  ");
         	System.out.printf(formatString2, (deadevens_notie + deadevens_tie)*2);
         	System.out.printf(formatString2, deadevens_notie);
         	System.out.printf(formatString2, deadevens_tie*2);
         	if ((deadevens_notie+deadevens_tie)>0) {
         	   System.out.printf("%6.1f", (deadevens_notie+(deadevens_tie/2)) / (deadevens_notie+deadevens_tie) );
         	} 
         	System.out.println();
         }
      }
      System.out.print("        TOTALS  ");  // every hand has 2 players
      System.out.printf(formatString2, totalAllins*2.0);
      System.out.printf(formatString2, totalWins*1.0);
      System.out.printf(formatString2, totalTies*2.0);
      System.out.printf("%6.1f",  ((totalTies/2.0) + (totalWins*1.0)) / (totalAllins*2.0)  );
      System.out.println(" avg equity\n");
      if (bothlost>0) {
         System.out.println(bothlost + "  corrupted hands found.\n");
      }
   }


   public static void scan(String rootfolder, String fullName) {
      int		useTies    = 1;  // 1 to count 0 don't count
      double[]  equity2   = new double[0];
      double    TimeNow   = System.currentTimeMillis();
      double    TimeStart = TimeNow;
      double 	getholecardlist;
      int    	type, type2;

      HandManagerNIO hm        = new HandManagerNIO();
      hm.init(rootfolder, fullName);
      hm.reset();
      while (hm.hasMoreHands()) {
         PokerHand hand = hm.nextPokerHand();
         totalHands++;
         if (totalHands % dumpPoint == 0) {
            dump();
         }
         if (totalHands % pingScreen == 0) {
	        System.out.println(getDateTime() + "  " + totalHands + "...");
	     }
         byte[][]  cards  = new byte[10][];
         boolean[] winner = new boolean[10];
         int       n      = 0;

         for (byte i = 0; i < hand.getNumberOfSeats(); i++) {
            byte[] h = hand.getCardsOnlyBA(i);
            if (h != null)
            	
               if (hand.isAllInPreflop(i)) {
                  winner[n] = hand.isWinner(i);
                  cards[n]  = h;
                  n++;
               }
         }

         if ((n == 2) && (winner[0] != winner[1])) {
            // We have two players allin preflop, and one of them won, no split pot
            getholecardlist = headsUp(cards[0], cards[1]);
            try {
               equity2 = new Showdown2().GetResults(holeCardList2, boardCardList2, useTies);
            } catch (Exception e) {}
            holeCardList2.clear();
            type  = (int) (equity2[0] * (1.0 * step));
            type2 = (int) (equity2[1] * (1.0 * step));
            if (equity2[1] == 0.5000000000) {    // special case for 50/50 equity
               deadevens_notie++;
               totalWins++;
               totalAllins++;
            } else {
	            if (winner[0]) {
	               wins[type]++;
	               allins[type]++;
	               allins[type2]++;
	               totalAllins++;
	               totalWins++;
	            } else {
	               wins[type2]++;
	               allins[type]++;
	               allins[type2]++;
	               totalAllins++;
	               totalWins++;
	            }
            }
         }    // end no ties
         
         if ((n == 2) && (winner[0] == true) && (winner[1] == true)) {
            // We have a tie
            totalTies++;
            getholecardlist = headsUp(cards[0], cards[1]);
            try {
               equity2 = new Showdown2().GetResults(holeCardList2, boardCardList2, useTies);
            } catch (Exception e) {}
            holeCardList2.clear();
            type  = (int) (equity2[0] * (1.0 * step));
            type2 = (int) (equity2[1] * (1.0 * step));
            if (equity2[1] == 0.5000000000) {    // special case for 50/50 equity and they tie
               deadevens_tie++;
               totalAllins++;
            } else {
	            ties[type]++;
	            ties[type2]++;
	            allins[type]++;
	            allins[type2]++;
	            totalAllins++;
            }           
	      }  // end ties
         
         if ((n == 2) && (winner[0] == false) && (winner[1] == false)) {
         	bothlost++;  // these are corrupted hands, sent email to Indiana
         }

      }
      hm.closedb();
   }


   public static String ds(double x) {
      String s = Math.round(x * 100) / 100.0 + "";
      int    k = s.indexOf(".");
      if (k >= 0)
         if (s.length() - k < 3)
            s += "0";
      return s;
   }


   public static double headsUp(byte[] cards1, byte[] cards2) {
      printHand(cards1);
      printHand(cards2);
      return 0;
   }

   public static char getCardIndex(char a) {
      if (a == 'A')
         return 14;
      if (a == 'K')
         return 13;
      if (a == 'Q')
         return 12;
      if (a == 'J')
         return 11;
      if (a == 'T')
         return 10;
      return (char) (a - '0');
   }

   private static int _getHandIndex(String hand) {
      int modif = 0;
      if (hand.length() > 2)
         modif = 225;
      modif += 15 * getCardIndex(hand.charAt(0));
      modif += getCardIndex(hand.charAt(1));
      return modif;
   }

   public static int getHandIndex(String hand) {
      return index[_getHandIndex(hand)];
   }

   public final static byte INVALID_CARD    = 100;

   public final static String printCard(byte card) {
      if (card == -1)
         return "--";
      if (card == INVALID_CARD)
         return "--";

      byte   rank = (byte) (card / 4);
      byte   suit = (byte) (card % 4);
      String res  = printRank(rank);

      switch (suit) {
         case 0 :
            return res + "c";
         case 1 :
            return res + "d";
         case 2 :
            return res + "s";
         case 3 :
            return res + "h";
      }
      return "--";
   }

   public final static String printRank(byte rank) {
      if (rank < 8)
         return (rank + 2) + "";
      if (rank == 8)
         return "T";
      if (rank == 9)
         return "J";
      if (rank == 10)
         return "Q";
      if (rank == 11)
         return "K";
      if (rank == 12)
         return "A";
      return "-";
   }

   public final static byte getCardRank(byte card) {
      return (byte) (card / 4);
   }

   public final static byte getCardSuit(byte card) {
      return (byte) (card % 4);
   }

   public final static String printHand(byte card, byte card2) {
      return printHand(new byte[] { card, card2 });
   }

   public final static String printHand(byte[] cards) {
      return printHand(printCard(cards[0]), printCard(cards[1]));
   }

   public final static String printHand(String hand) {
      return printHand(hand.substring(0, 2), hand.substring(2, 4));
   }

   public final static String printHand(String s1, String s2) {
      holeCardList2.add(s1);
      holeCardList2.add(s2);
      return "dummy";    // rather than reworking the old method
   }
   

    public static String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }   
}

