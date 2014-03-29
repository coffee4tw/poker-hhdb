package pokerai.hhex;

import pokerai.hhex.internal.RawPokerHand;

public class Consts {
  public static String playersIndexFilename = "players.mapping.index";
  public static String gameidIndexFilenames = "temp\\gameid.index";
  public static String rootFolder = "C:\\hhdb\\";
  public static String enter = new String(new byte[]{13, 10});

  // Options during hand import
  public static final boolean loadImportedHandsAfterSave = true;         // make sure hands can be loaded immediately after save
  public static final boolean detailedVerificationDuringImport = true;   // detailed verification checks after import of hand
  public static final boolean saveChatEvents = false;                    // if false, the chat events are not saved and are lost. Chat phrases are still saved in chat.log
  public static final boolean saveTableJoinEvents = false;               // if false, then these events are not saved and are lost
  public static final boolean saveCallAmounts = true;                    // if false then call event is not saved but only calculated. If false then some import verifications won't work.
  public static final boolean saveLimitBetAmounts = true;                // not implemented.

  // Other options
  public static final boolean indexReadOnlyMode = true;                  // internal


  // PokerSites
  public final static String[] sites = {"PS", "FTP", "PARTY", "IPN", "BOSS", "UB", "HHDb"};
  public final static byte SITE_DEFAULT  =  0;
  public final static byte SITE_PS       =  0;
  public final static byte SITE_FTP      =  1;
  public final static byte SITE_PARTY    =  2;
  public final static byte SITE_IPN      =  3;
  public final static byte SITE_BOSS     =  4;
  public final static byte SITE_UB       =  5;
  public final static byte SITE_HHDB     =  6;

  // Games
  public final static String[] game       = {"Unknown", "Hold\'em No Limit", "Hold\'em Pot Limit", "Hold\'em Limit", "Omaha No Limit", "Omaha Pot Limit", "Omaha Limit"};
  public final static String[] game_party = {"Unknown",  "NL Texas Hold'em",   "PL Texas Hold'em", "Texas Hold'em",  "NL Omaha", "PL Omaha", "FL Omaha"};
  public final static byte GT_UNKNOWN    =  0;
  public final static byte HOLDEM_NL     =  1;
  public final static byte HOLDEM_PL     =  2;
  public final static byte HOLDEM_FL     =  3;
  public final static byte OMAHA_NL      =  4;
  public final static byte OMAHA_PL      =  5;
  public final static byte OMAHA_FL      =  6;

  // Actions
  public final static String[] streets = {"Preflop", "Flop", "Turn", "River", "Summary"};
  public final static byte STREET_UNKNOWN         = -1;
  public final static byte STREET_PREFLOP         =  0;
  public final static byte STREET_FLOP            =  1;
  public final static byte STREET_TURN            =  2;
  public final static byte STREET_RIVER           =  3;
  public final static byte STREET_SHOWDOWN        =  4;
  //public final static byte STREET_SUMMARY         =  5;

  public final static byte MAX_ACTIONS = 125;  

  // Max 16 actions (0 - 15), as this is encoded in half byte
  public final static String[] actions = {"posts BB", "posts SB", "posts DB", "posts DB", "folds", "checks", "calls", "bets", "raises to", "is allin", "leaves table", "joinstable", "sits out", "sits in", "collects", "said"};
  public final static byte ACTION_NONE             = -1;  // not used for save/load, but for logic
  public final static byte ACTION_BIGBLIND         =  0;
  public final static byte ACTION_SMALLBLIND       =  1;
  public final static byte ACTION_BOTHBLINDS       =  2;
  public final static byte ACTION_DEAD_SB          =  3;
  public final static byte ACTION_FOLD             =  4;
  public final static byte ACTION_CHECK            =  5;
  public final static byte ACTION_CALL             =  6;   // amounts are not saved but calculated (automatically on call). Call amounts are the delta that has to be called (e.g. if player spent 3 and there is raise to 6, then call 3)
  public final static byte ACTION_BET              =  7;   // amounts are saved only for this
  public final static byte ACTION_RAISE            =  8;   // amounts are saved only for this, semantics is RAISE *TO* the amount
  public final static byte ACTION_ALLIN_DEPRECATED =  9;   // ALLIN events are deprecated: these are not parsed due to bet/raise & all-in ambiquity
  public final static byte ACTION_LEAVETABLE       = 10;
  public final static byte ACTION_JOINTABLE        = 11;
  public final static byte ACTION_SITTINGOUT       = 12;
  public final static byte ACTION_SITTINGIN        = 13;
  public final static byte ACTION_WON              = 14;   // Pot awarded to player, also used for uncalled bets
  //public final static byte ACTION_SAID            = 15;

  public final static boolean[] awoa = new boolean[16];
  static { for (int i = 0; i < 16; i++) awoa[i] = true;
    awoa[ACTION_BET] = false; awoa[ACTION_RAISE] = false; awoa[ACTION_WON] = false;  if (saveCallAmounts) awoa[ACTION_CALL] = false; }
  public final static boolean isActionWithoutAmount(byte action) { return awoa[action]; }

  private static boolean[] amMask = new boolean[300];
  public final static boolean isActionWithoutAmountEncoded(byte jointbyte) { return amMask[jointbyte + 128]; }
  static {
    for (byte b1 = 0; b1 < 16; b1++)
      for (byte b2 = 0; b2 < 16; b2++) {
        byte b = RawPokerHand.twoHalfBytes(b1, b2);
        if (isActionWithoutAmount(b2)) amMask[b + 128] = true; else amMask[b + 128] = false;
      }
  }

  private final static boolean[] vgda = new boolean[16]; // voluntaryGameDependantActions
  private final static boolean[] vpip = new boolean[16]; // VPIPactions
  private final static boolean[] spac = new boolean[16]; // Any Play Action from seated player
  static { vgda[ACTION_FOLD] = true; vgda[ACTION_CHECK] = true; vgda[ACTION_CALL] = true;
           vgda[ACTION_BET] = true; vgda[ACTION_RAISE] = true;
           vpip[ACTION_BET] = true; vpip[ACTION_RAISE] = true; vpip[ACTION_CALL] = true;
           spac[ACTION_BET] = true; spac[ACTION_RAISE] = true;  spac[ACTION_CALL] = true; spac[ACTION_BIGBLIND] = true;
           spac[ACTION_SMALLBLIND] = true; spac[ACTION_BOTHBLINDS] = true; spac[ACTION_FOLD] = true;
           spac[ACTION_DEAD_SB] = true; spac[ACTION_CHECK] = true; spac[ACTION_WON] = true;
  }

  public static boolean isVoluntaryGameAction(byte action) { return vgda[action]; }
  public static boolean isVPIPAction(byte action) { return vpip[action]; }
  public static boolean isPlayActionBySeatedPlayer(byte action) { return spac[action]; }

  public static boolean isVoluntaryGameAction(Action action) { return vgda[action.getAction()]; }
  public static boolean isVPIPAction(Action action) { return vpip[action.getAction()]; }
  public static boolean isAggressiveAction(Action action) { return action.getAction() == ACTION_BET || action.getAction() == ACTION_RAISE; }
  public static boolean isBlindAction(Action action) { return action.getAction() <= 3; }

  public final static byte BUTTON_UNKNOWN         = 12;
  public final static byte PLAYERID_UNKNOWN       = 12;
  public final static byte SEATID_UNKNOWN         = 12;
  public final static byte MAX_PLAYERNAME_LENGTH  = 25;

  public final static byte BLINDS_UNKNOWN       = 0;

  // pos 0 and 1 are the blinds, pos 3 and 4 are the limit mapping
  public final static int[][] BLINDS =
                  {
                          {0,      0,       -1,     -1,      },
                          {1,      2,       -1,     -1,      },
                          {2,      5,       -1,     -1,      },
                          {5,      10,       2,      5,      },
                          {10,     20,       5,     10,      },
                          {10,     25,      -1,     -1,      },
                          {12,     25,      -1,     -1,      },
                          {25,     50,      10,     25,      },        // these limit games are rare in HH, but still. PS mapping
                          {50,     100,     25,     50       },
                          {100,    200,     50,     100      },
                          {100,    300,     -1,     -1,      },
                          {200,    400,     100,    200      },
                          {200,    500,     -1,     -1,      },
                          {250,    500,     -1,     -1,      },
                          {300,    600,     100,    300,     },
                          {500,    1000,    200,    500,     },
                          {1000,   1500,    -1,     -1,      },
                          {1000,   2000,    500,    1000,    },
                          {1500,   3000,    1000,   1500,    },
                          {2500,   5000,    -1,     -1,      },
                          {3000,   6000,    1500,   3000,    },
                          {5000,   10000,   2500,   5000,    },
                          {10000,  20000,   5000,   10000,   },
                          {20000,  40000,   10000,  20000,   },
                          {25000,  50000,   -1,     -1,      },
                          {50000,  100000,  25000,  50000,   },
                          {100000, 200000,  50000,  100000,  },
                  };


  public final static int[] SB_NORM = new int[BLINDS.length];
  static { for (int i = 0; i < BLINDS.length; i++) SB_NORM[i] = (int)Math.round((BLINDS[i][0]*100.0)/ BLINDS[i][1]); }

  public final static int[] getBlindsRow(int sb, int bb) { for (int i = 0; i < BLINDS.length; i++) if (sb == BLINDS[i][0] && bb == BLINDS[i][1]) return BLINDS[i]; return null; }
  public final static int[] getBlindsRowLB(int sb, int bb) { for (int i = 0; i < BLINDS.length; i++) if (sb == BLINDS[i][2] && bb == BLINDS[i][3]) return BLINDS[i]; return null; }

  public final static byte SIZE_STACKS3_ACTIONS3 = 0;
  public final static byte SIZE_STACKS3_ACTIONS2 = 1;
  public final static byte SIZE_STACKS2_ACTIONS3 = 2;
  public final static byte SIZE_STACKS2_ACTIONS2 = 3;


  // CARDS:
  //  0,  1,  2,  3 - 2c 2d 2s 2h
  //  4,  5,  6,  7 - 3c 3d 3s 3h
  //  8,  9, 10, 11 - 4c 4d 4s 4h
  // 12, 13, 14, 15 - 5
  // 16, 17, 18, 19 - 6
  // 20, 21, 22, 23 - 7
  // 24, 25, 26, 27 - 8
  // 28, 29, 30, 31 - 9
  // 32, 33, 34, 35 - T
  // 36, 37, 38, 39 - Jc Jd Js Jh
  // 40, 41, 42, 43 - Qc Qd Qs Qh
  // 44, 45, 46, 47 - Kc Kd Ks Kh
  // 48, 49, 50, 51 - Ac Ad As Ah
  public final static byte INVALID_CARD = 100;

  public final static byte RANK_ACE = 12;
  public final static byte RANK_KING = 11;
  public final static byte RANK_QUEEN = 10;
  public final static byte RANK_JACK = 9;
  public final static byte RANK_TEN = 8;
  public final static byte RANK_NINE = 7;
  public final static byte RANK_EIGHT = 6;
  public final static byte RANK_SEVEN = 5;
  public final static byte RANK_SIX = 4;
  public final static byte RANK_FIVE = 3;
  public final static byte RANK_FOUR = 2;
  public final static byte RANK_THREE = 1;
  public final static byte RANK_TWO = 0;

  public final static byte getCard(String s) { return getCard(s.charAt(0), s.charAt(1)); };
  public final static byte getCard(char c1, char c2) {
    if (c1 == '-' || c2 == '-') return INVALID_CARD;
    byte size = 0, suit = 0;
    switch (c1) {
      case '2': size = 0; break;
      case '3': size = 1; break;
      case '4': size = 2; break;
      case '5': size = 3; break;
      case '6': size = 4; break;
      case '7': size = 5; break;
      case '8': size = 6; break;
      case '9': size = 7; break;
      case 'T': size = 8; break;
      case 'J': size = 9; break;
      case 'Q': size = 10; break;
      case 'K': size = 11; break;
      case 'A': size = 12; break;
    }
    switch (c2) {
      case 'c': suit = 0; break;
      case 'd': suit = 1; break;
      case 's': suit = 2; break;
      case 'h': suit = 3; break;
    }
    return (byte)(size*4 + suit);
  }

  public final static String printCardsExcludingFirst(byte card[]) {
    String s = ""; for (int i = 1; i < card.length; i++) s+= printCard(card[i]); return s; }
  public final static String printCards(byte card[]) {
    String s = ""; for (int i = 0; i < card.length; i++) s+= printCard(card[i]); return s; }

  public final static String printCard(byte card) {
    if (card == -1) return "--";
    if (card == INVALID_CARD) return "--";

    //if (card == -1) return "";
    byte rank = (byte)(card / 4);
    byte suit = (byte)(card % 4);
    String res = printRank(rank);
    switch (suit) {
      case 0 : return res + "c";
      case 1 : return res + "d";
      case 2 : return res + "s";
      case 3 : return res + "h";
    }
    return "--";
  }

  public final static String printRank(byte rank) {
    if (rank  <  8) return (rank+2) + "";
    if (rank ==  8) return "T";  if (rank ==  9) return "J";
    if (rank == 10) return "Q";  if (rank == 11) return "K";
    if (rank == 12) return "A";
    return "-";
  }

  public final static byte getCardRank(byte card) { return (byte)(card / 4); }
  public final static byte getCardSuit(byte card) { return (byte)(card % 4); }
  public final static String printHand(byte card, byte card2) {  return printHand(new byte[]{card, card2}); }
  public final static String printHand(byte[] cards) { return printHand(printCard(cards[0]), printCard(cards[1])); }
  public final static String printHand(String hand) { return printHand(hand.substring(0,2), hand.substring(2,4)); }
  public final static String printHand(String s1, String s2) {
    if (s1.charAt(1) != s2.charAt(1)) return "" + s1.charAt(0) + s2.charAt(0); else return "" + s1.charAt(0) + s2.charAt(0) + "s"; }

}