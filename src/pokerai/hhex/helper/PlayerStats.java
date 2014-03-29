/*
  This code is released under GPL v3.

  @Author: Indiana (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910
 */
package pokerai.hhex.helper;

import java.io.*;

public class PlayerStats {
  // update when change persisted values!
  public final static int REC_LENGTH = 20;

  // persisted
  public int hands = 0;
  public int intfiled1 = 0;
  public int intfiled2 = 0;
  public long totalWinnings = 0;            // total winnings in BB, multiplied by 100

  // calculated values
  public int playerId = 0;
  public double bb100 = 0;

  // returns the total winnings in BB units
  public double getTotalWinnings() {
    return totalWinnings / 100.0;
  }

  public byte[] save() {
    try {
      ByteArrayOutputStream b = new ByteArrayOutputStream();
      DataOutputStream d = new DataOutputStream(b);
      d.writeInt(hands);
      d.writeInt(intfiled1);
      d.writeInt(intfiled2); 
      d.writeLong(totalWinnings);
      calculateData();
      return b.toByteArray();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public void load(int playerID, byte[] data) {
    this.playerId = playerID;
    try {
      ByteArrayInputStream b = new ByteArrayInputStream(data);
      DataInputStream d = new DataInputStream(b);
      hands = d.readInt();
      intfiled1 = d.readInt();
      intfiled2 = d.readInt();
      totalWinnings = d.readLong();
      calculateData();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void calculateData() {
    // mult 100 due to 100 hands samples, div by 100 due to format of totalWinnings, so finally just * 1.0
    bb100 = (totalWinnings * 1.0) / hands;
  }

}

