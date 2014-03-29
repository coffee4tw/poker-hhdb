/*
  This code is released under GPL v3.

  @Author: Indiana (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910
 */
package pokerai.hhex.internal;

import pokerai.hhex.Action;
import pokerai.hhex.Consts;
import pokerai.hhex.PokerHand;

public class RawPokerHand {

  /* STATIC SETTINGS */
  public final static boolean persistGameId = false;  // should gameId be persisted at all
  public final static byte offset = (persistGameId) ? 21 : 13;
  public static long staticGameId = 1; // used for loading when gameId is not persisted, statically ++ed
  public final static boolean obfuscateTime = false;  // if set to true, then time for each hand is semi-obfuscated (seconds are rounded to nearest 10)

  public final static boolean exitOnLoadError = false;  // if set to true, then time for each hand is semi-obfuscated (seconds are rounded to nearest 10)
  // -------

  // Header (16/24 bytes). Update offest above if variables size in the header is changed
  public byte site = Consts.SITE_PS;       // default is PokerStars for any possible hand
  public byte game = Consts.GT_UNKNOWN;    // todo: site + game in single byte
  protected byte numberOfSeats = 0;
  protected long gameId;              // possibly skipped
  public int date;                         // This is getTimeInMillis() / 1000 (hence seconds)
  public int tableId;                      // todo: Encode tabels better to preserve uniquiness, and in 3 bytes only
  public byte blinds = Consts.BLINDS_UNKNOWN;
  public byte button = Consts.BUTTON_UNKNOWN; // button is from 1 ... numberOfSeats (not starting from 0!)

  // Gamedata
  // Mapping from seat -> player
  public int[] players;   // Encoded with 3 bytes only. 3 bytes is 16777216 players

  // Stack in BigBlinds, all are encoded with 2 or 3 bytes (if all stacks fit in short, then use 2 bytes)
  public int[] stacks;

  // Cards for up to X (for now 5) players. PlayerSeat (x Number of Cards, -1 for end). (i.e. first position in the array is the player Seat)
  public static final byte maxCardsPx = 7;
  public byte[][] cardsPx = new byte[maxCardsPx][];   // Todo: only initialize on demand (and adjust save/load, wrapper usages)
  public byte[] board = new byte[]{Consts.INVALID_CARD, Consts.INVALID_CARD, Consts.INVALID_CARD, Consts.INVALID_CARD, Consts.INVALID_CARD};
  
  // Actions - used during loading
  public Action[] aactionsPreflop  = null;
  public Action[] aactionsFlop     = null;
  public Action[] aactionsTurn     = null;
  public Action[] aactionsRiver    = null;
  public Action[] aactionsShowdown = null;

  // NOT PERSISTED (calculated) values, used when writting hands
  public boolean buggyhand = false;
  public boolean sbAlreadyPosted = false;
  public int _BB = -1;  // accessed, set and used when parsing and saving hands. Not persisted.
  public int _SB = -1;  // accessed, set and used when parsing and saving hands. Not persisted.
  public boolean allActionsShort = true; // used in Action

  // NOT PERSISTED, used when reading hands
  private PokerHand phand = null;
  public boolean callAmountsCalculated = false;   // used in Action
  boolean betAmountsCalculated = false;

  public void setPokerHand(PokerHand p) { phand = p; }
  public PokerHand getPokerHand() { return phand; }
  public long getGameId() { return gameId; }
  public void setGameId(long gId) { gameId = gId; }

  public byte getSeatId(int playerId, String playerName) {
    if (players == null) return Consts.PLAYERID_UNKNOWN; // e.g. during parsing hands, if getSeatId is called for action with player that wasn't seated at the beginning (normally indicating error!?)
    for (byte i = 0; i < players.length; i++) if (playerId == players[i]) return i;
    //System.out.println("ERROR: Seat ID not idenfied, handid = " + gameId + ", playerId = " + playerId + ", playerName " + playerName);
    return Consts.PLAYERID_UNKNOWN;
  }

  public byte getNumberOfSeats() { return numberOfSeats; }

  // 20% here, 30-40% in loadNextHand
  public void setNumberOfSeats(byte seats) {
    numberOfSeats = seats;
    players = new int[seats];
    stacks = new int[seats];
  }

  public byte[] data = null; // used in PokerHand
  public boolean handLoaded = false, headerLoaded = false;

  public static PokerHand load(byte[] data) {
    RawPokerHand p = new RawPokerHand();
    p.data = data;
    return new PokerHand(p); 
  }

  // http://java.sun.com/j2se/1.5.0/docs/api/java/io/DataInput.html
  public void _loadHeader() {
    if (data == null) return;
    try {
      int pos = 0;
      site = data[pos]; pos++;
      if (persistGameId) {
        gameId = readLong(data, pos); pos+=8;
      } else {
        gameId = (staticGameId++);
      }
      game    = data[pos]; pos+=1;
      date    = readInt(data, pos); pos+=4;
      tableId = readInt(data, pos); pos+=4;
      setNumberOfSeats(data[pos]); pos++;
      blinds = data[pos];  pos++;
      button = data[pos];  pos++;
      if (numberOfSeats == 0) throw new Exception("numberOfSeats is zero, this is imposible, as such hands are skipped.");
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Nseats: " + numberOfSeats);
      for (int i = 0; i < data.length; i++) {
        System.out.print(data[i] + " ");
        if ((i+1) % 10 == 0) System.out.println();
      }
      System.out.println();
      buggyhand = true;
      if (exitOnLoadError) System.exit(1);
    }
    headerLoaded = true;
  }

  public void _loadGameData() {
    if (data == null) return;
    if (!headerLoaded) _loadHeader();
    int pos = offset; // 23 or 15
    try {
      //BB = readInt(data, pos); pos+=4; SB = readInt(data, pos); pos+=4;
      for (int i = 0; i < numberOfSeats; i++) { players[i] = readInt3Bytes(data, pos); pos+=3; }
      // Stacks
      boolean allshorts = (data[pos] == Consts.SIZE_STACKS2_ACTIONS2 || data[pos] == Consts.SIZE_STACKS2_ACTIONS3);
      allActionsShort = (data[pos] == Consts.SIZE_STACKS2_ACTIONS2 || data[pos] == Consts.SIZE_STACKS3_ACTIONS2);
      pos++;
      if (allshorts) {
        for (int i = 0; i < numberOfSeats; i++) { stacks[i] = readShort(data, pos); pos+=2; }
      } else {
        for (int i = 0; i < numberOfSeats; i++) { stacks[i] = readInt3Bytes(data, pos); pos+=3; }
      }
      // Read cards for upto X players
      // todo: initialize the cardarray (and not statically), only if have to -> read the first byte
      for (int z = 0; z < maxCardsPx; z++) {  // load cards for upto 3 players
        byte mask = data[pos]; pos++;
        if (mask < 0) break;
        cardsPx[z] = new byte[mask / 10 + 1];  // size
        cardsPx[z][0] = (byte)(mask % 10); // seat
        for (int i = 1; i < cardsPx[z].length; i++) { cardsPx[z][i] = data[pos]; pos++; }
      }
      for (int i = 0; i < 5; i++) { board[i] = data[pos]; pos++; if (board[i] == Consts.INVALID_CARD) break; }   // stop reading when invalid card is reached
      // Read actions
      int n = data[pos]; pos++;
      if (n > 0) {
        if (aactionsPreflop == null) aactionsPreflop = new Action[n];
        pos = read(data, pos, aactionsPreflop, n);
      }
      n = data[pos]; pos++;
      if (n > 0) {
        if (aactionsFlop == null) aactionsFlop = new Action[n];
        pos = read(data, pos, aactionsFlop, n);
      }
      n = data[pos]; pos++;
      if (n > 0) {
        if(aactionsTurn == null) aactionsTurn = new Action[n];
        pos = read(data, pos, aactionsTurn, n);
      }
      n = data[pos]; pos++;
      if (n > 0) {
        if (aactionsRiver == null) aactionsRiver = new Action[n];
        pos = read(data, pos, aactionsRiver, n);
      }
      n = data[pos]; pos++;
      if (n > 0) {
        if (aactionsShowdown == null) aactionsShowdown = new Action[n];
        pos = read(data, pos, aactionsShowdown, n);
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Nseats: " + numberOfSeats);
      for (int i = 0; i < data.length; i++) {
        System.out.print(data[i] + " ");
        if ((i+1) % 10 == 0) System.out.println();
      }
      System.out.println();
      buggyhand = true;
      if (exitOnLoadError) System.exit(1);
    }
    handLoaded = true;
  }

  public int read(byte[] data, int pos, Action[] actions, int n) throws Exception {
    //a. d.writeByte(actions.size());
    for (int i = 0; i < n; i++) {
      Action a = new Action(this);
      a.jointbyte = data[pos]; pos++;
      //if (Consts.isActionWithoutAmount(a.getAction())) {
      if (Consts.isActionWithoutAmountEncoded(a.jointbyte)) {
        a.setAmount(-1);
      } else {
        if (allActionsShort) {
          a.setAmount(readShort(data, pos)); pos+=2;
        } else {
          a.setAmount(readInt3Bytes(data, pos)); pos+=3;
        }
      }
      actions[i] = a;    // add to the respective street
      //p.actions.add(a);  // add to all actions
    }
    return pos;
  }

  public final static byte twoHalfBytes(byte b1, byte b2) {   return (byte)(b1*16 + b2 - 128);  }
  public final static byte byte1(byte b) {    return ((byte)((b+128) / 16));  }
  public final static byte byte2(byte b) {    return ((byte)((b+128) % 16));  }

  private static short readShort(byte[] b, int pos) {
    return (short)((b[pos] << 8) | (b[pos+1] & 0xff));
  }

  private static int readInt(byte[] b, int pos) {
    return (((b[pos] & 0xff) << 24) |
            ((b[pos+1] & 0xff) << 16) |
            ((b[pos+2] & 0xff) << 8) |
            (b[pos+3] & 0xff));
  }

  public static int readInt3Bytes(byte[] b, int pos) {
    return (((b[pos] & 0xff) << 16) |
            ((b[pos+1] & 0xff) << 8) |
            ((b[pos+2] & 0xff)));
  }


  private static long readLong(byte[] b, int pos) {
    return ((long)(b[pos+0] & 0xff) << 56) |
     ((long)(b[pos+1] & 0xff) << 48) |
     ((long)(b[pos+2] & 0xff) << 40) |
     ((long)(b[pos+3] & 0xff) << 32) |
     ((long)(b[pos+4] & 0xff) << 24) |
     ((long)(b[pos+5] & 0xff) << 16) |
     ((long)(b[pos+6] & 0xff) <<  8) |
     ((long)(b[pos+7] & 0xff));
  }

}

