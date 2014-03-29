package pokerai.hhex.examples;

/**
 * Interface for tagging boards with properties.
 * 
 * @author CodeD (http://pokerai.org/pf3)
 *
 */
public interface BoardProperty {
	
	boolean boardHasProperty(byte[] board);
	
	String propertyDescription();
}
