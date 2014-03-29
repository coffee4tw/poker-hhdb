/*
  This code is released under GPL v3.

  @Author: Indiana (http://pokerai.org/pf3)
  See: http://www.pokerai.org/pf3/viewtopic.php?f=3&t=1910

  This code builds and maintains the mapping from PlayerID -> Real Player Names
 */
package pokerai.hhex.internal;

import pokerai.hhex.Consts;
import pokerai.hhex.HandProcessor;
import pokerai.hhex.PokerHand;
import pokerai.hhex.filter.FilteredHandManager;
import pokerai.hhex.filter.HHFilterPlayer;

import java.io.File;

public class PlayerHandsDBIndex extends HandProcessor {
  CustomDBCreator[] db = null;
  int[] playerIds = null;

  public static void buildIndexForPlayers(String root, int[] playerIds) {
    File f = new File(root + "indices\\");
    if (!f.exists()) f.mkdir();
    // If all specified player files exist -> return
    boolean allexist = true;
    for (int i = 0; i < playerIds.length; i++) {
      File f2 = new File(root + "indices\\" + "player" + playerIds[i]);
      if (!f2.exists()) { allexist = false; break; }
    }
    if (allexist) return;
    // If index for at least one player is not built -> delete all files and
    // rebuild indeces for all players (same speed as doing it for just one player)
    for (int i = 0; i < playerIds.length; i++) {
      if (!Consts.indexReadOnlyMode) {
        File f2 = new File(root + "indices\\" + "player" + playerIds[i]);
        f2.delete();
      }
    }
    PlayerHandsDBIndex pi = new PlayerHandsDBIndex();
    pi.setRootFolder(root);
    HHFilterPlayer fp = new HHFilterPlayer(playerIds);
    FilteredHandManager fhm = new FilteredHandManager(null, fp);
    pi.playerIds = playerIds;
    pi.db = new CustomDBCreator[playerIds.length];
    for (int i = 0; i < playerIds.length; i++) {
      pi.db[i] = new CustomDBCreator();
      pi.db[i].init(root + "indices\\", "player" + playerIds[i], true);
    }
    pi.process(fhm);
    for (int i = 0; i < playerIds.length; i++) {
      pi.db[i].closedb();
    }
  }

  protected void processHand(PokerHand hand) {
		int[] ids =  hand.getPlayerIDs();
    for (int i = 0; i < playerIds.length; i++) {
  		for (int j = 0; j < ids.length; j++) {
        if (ids[j] == playerIds[i]) {
          //System.out.println(playerIds[i]);
          db[i].appendHand(hand.getRawData());
          break;
        }
		  }
		}
  }

  protected void printResults() {
  }

}