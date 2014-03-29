package pokerai.hhex;

import pokerai.hhex.filter.FilteredHandManager;
import pokerai.hhex.internal.HandManagerNIO;

import java.io.File;

/**
 * 
 * Simple class to make life easier, i.e. to scan all the db files and process the hands. 
 * Just extend, and implement the necessary methods.
 * 
 * @author CodeD (http://pokerai.org/pf3)
 *
 */
public abstract class HandProcessor {

  protected boolean debugHandProcessor = false;

  protected String dbName = "pokerai.org.sample" ;
	protected String rootFolder = "C:\\hhdb\\";
	protected byte[] sites = new byte[Consts.sites.length];
	protected byte[] gameTypes = new byte[Consts.game.length];
	protected int[] numberOfPlayers = new int[] {2, 3, 4, 5, 6, 7, 8, 9, 10};
  protected int[] weeks = null;

  public HandProcessor() {
    for (int i = 0; i < sites.length; i++) sites[i] = (byte)i;
    for (int i = 0; i < gameTypes.length; i++) gameTypes[i] = (byte)i;
  }

  public void setDbName(String dbName) {	this.dbName = dbName; }

   public void setRootFolder(String rootFolder) {
      this.rootFolder = rootFolder;
      if (!this.rootFolder.endsWith(File.separator)) // <- ADDED
         this.rootFolder += File.separator;
   }

	public void setSites(byte... sites) {	this.sites = sites; }
	public void setGameTypes(byte... gameTypes) { this.gameTypes = gameTypes; }
	public void setNumberOfPlayers(int... numberOfPlayers) {	this.numberOfPlayers = numberOfPlayers;	}
	public void setWeeks(int... weeks) { this.weeks = weeks;	}

  // "_" to avoid naming conflicts with subclasses
	protected long _totalHands = 0, hmTimeRead = 0, hmTimeParse = 0;

  /*
  public HandProcessor setConfig(int[] weeks) {
		return setConfig(null, null, null, null, null, weeks);
	}

	public HandProcessor setConfig(byte gameType, int numberOfPlayers) {
		return setConfig(null, null, null, new byte[]{gameType}, new int[]{numberOfPlayers}, null);
	}
	
	public HandProcessor setConfig(String rootFolder, byte gameType, int numberOfPlayers) {
		return setConfig(null, rootFolder, null, new byte[]{gameType}, new int[]{numberOfPlayers}, null);
	}

	public HandProcessor setConfig(byte site, byte gameType, int numberOfPlayers) {
		return setConfig(null, null, new byte[]{site}, new byte[]{gameType}, new int[]{numberOfPlayers}, null);
	}

	public HandProcessor setConfig(String rootFolder, byte site, byte gameType, int numberOfPlayers) {
		return setConfig(null, rootFolder, new byte[]{site}, new byte[]{gameType}, new int[]{numberOfPlayers}, null);
	}

	public HandProcessor setConfig(byte site, byte gameType, int numberOfPlayers, int[] weeks) {
		return setConfig(null, null, new byte[]{site}, new byte[]{gameType}, new int[]{numberOfPlayers}, weeks);
	}
	
	public HandProcessor setConfig(byte[] sites, byte[] gameTypes, int[] numberOfPlayers, int[] weeks) {
		return setConfig(null, null, sites, gameTypes, numberOfPlayers, weeks);
	}
	
	public HandProcessor setConfig(String rootFolder, byte[] sites, byte[] gameTypes, int[] numberOfPlayers, int[] weeks) {
		return setConfig(null, rootFolder, sites, gameTypes, numberOfPlayers, weeks);
	}
	
	public HandProcessor setConfig(String rootFolder, byte[] sites, byte[] gameTypes, int[] numberOfPlayers) {
		return setConfig(null, rootFolder, sites, gameTypes, numberOfPlayers, null);
	}

	public HandProcessor setConfig(String dbName, String rootFolder, byte[] sites, byte[] gameTypes, int[] numberOfPlayers, int[] weeks) {
		if (dbName != null)		this.dbName = dbName;
		if (rootFolder != null)	this.rootFolder = rootFolder;
		if (sites != null)		this.sites = sites;
		if (gameTypes != null)	this.gameTypes = gameTypes;
		if (numberOfPlayers != null)		this.numberOfPlayers = numberOfPlayers;
		if (weeks != null)		this.weeks = weeks;

		if (!this.rootFolder.endsWith(File.separator))
			this.rootFolder += File.separator;
		
		return this;
	}
	*/

	public void process() { process(-1); }
	public void process(FilteredHandManager fhm) { process(-1, fhm); }
	public void process(long printResultsEvery) { process(printResultsEvery, null); }
	
	public void process(long printResultsEvery, FilteredHandManager fhm) {
		long time = System.currentTimeMillis();
		String[] files = new File(rootFolder).list();
		for (int i = 0; i < files.length; i++) {
			for (byte site : sites) {
				for (byte gameType : gameTypes) {
					for (int player : numberOfPlayers) {
						if(weeks == null) {
							if (files[i].startsWith(dbName 
									+ site + "_" + 
									gameType + "_" + 
									player)
									&& files[i].endsWith(".hhex")) {
								scan(rootFolder, files[i], printResultsEvery, fhm);
							}
						} else {
							for (int week : weeks) {
								if (files[i].startsWith(dbName 
										+ site + "_" + 
										gameType + "_" + 
										player + "_w" + week)
										&& files[i].endsWith(".hhex")) {
									scan(rootFolder, files[i], printResultsEvery, fhm);
								}
							}
						}
					}
				}
			}
		}

		if (debugHandProcessor) {
      long time2 = System.currentTimeMillis() - time;
      long handsPerSecond = (long) (((1000.0 * _totalHands) / time2));
      System.out.println("Number of hands: " + _totalHands);
      long seconds = (long) (time2 / 1000.0);
      System.out.println("Read & processing time: " + seconds/3600 + "h " + (seconds%3600)/60 + "m " + (seconds%60) + "s");
      System.out.println("Read & processing speed: " + handsPerSecond + " hands/second. ");
        System.out.println("Read time (ms): " + hmTimeRead + ", parse time (ms): " + hmTimeParse);
    }

    printResults();

	}

  private void scan(String rootfolder, String fullName, long printResultsEvery, FilteredHandManager fhm) {
    if (debugHandProcessor) {
      System.out.println("HandProcessor - scanning " + fullName);
    }
    HandManagerNIO hm = new HandManagerNIO();
    hm.init(rootfolder, fullName);
    hm.reset();
    if (fhm != null) {
       fhm.setHandManagerNIO(hm); // <- ADDED
       while (fhm.hasMoreHands()) {
          PokerHand hand = fhm.nextPokerHand();
          _totalHands++;
          if (printResultsEvery > 0 && _totalHands % printResultsEvery == 0) {
             System.out.println((long) _totalHands + " hands read.");
             printResults();
          }
          processHand(hand);
       }
    } else {
       while (hm.hasMoreHands()) {
          PokerHand hand = hm.nextPokerHand();
          _totalHands++;
          if (printResultsEvery > 0 && _totalHands % printResultsEvery == 0) {
             System.out.println((long) _totalHands + " hands read.");
             printResults();
          }
          processHand(hand);
       }
    }
    hmTimeRead += hm.timeRead;
    hmTimeParse += hm.timeParse;
    hm.closedb();
    System.gc(); // <- ADDED
  }


  // called for each new hand that fullfills the given criterias
  protected abstract void processHand(PokerHand hand);

  // called when parsing is complete
  protected abstract void printResults();

}
