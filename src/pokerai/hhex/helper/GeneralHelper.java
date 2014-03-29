package pokerai.hhex.helper;

import java.text.DecimalFormat;

/**
 * Class for Helper functions, just use
 * 
 * import static pokerai.hhex.helper.GeneralHelper.*;
 * 
 * at the beginning of your class, and you can access the functions just by name without preceding "GeneralHelper." .
 * 
 * @author CodeD, Indiana (http://pokerai.org/pf3)
 *
 */
public class GeneralHelper {

	public static String fillSpaces(String s, int desiredLength, boolean infront) {
		if (s.length() < desiredLength) {
			StringBuilder sb = new StringBuilder(desiredLength);
			sb.append(s);
			for (int i = s.length(); i < desiredLength; i++) {
				if (infront) {
					sb.insert(0, ' ');
				} else {
					sb.append(' ');
				}
			}
			return sb.toString();
		}
		return s;
	}
	
	private static DecimalFormat df = new DecimalFormat("0.##");
	private static DecimalFormat dfForce = new DecimalFormat("00.00");
	public static String decimalString(double d, boolean forceDigits) {
		return forceDigits?dfForce.format(d):df.format(d);
	}

	public static double ds(double x) {
		return Math.round(x * 100) / 100.0;
	}

}
