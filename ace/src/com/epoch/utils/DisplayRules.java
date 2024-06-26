package com.epoch.utils;

import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
// import java.io.FileReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Singleton class that holds the regular expressions for converting 
			text to HTML display. 
	Scope: loaded by the GroupLoader servlet at the time of app deployment.
*/
public final class DisplayRules {

	/** Stores the rules for converting lightly formatted organic chemistry 
	 * text to HTML in the order in which the rules should be applied.  The
	 * rules are stored as a list of arrays of Strings.
	 * In each array, the first member is a regular expression, and the second
	 * is the String which should replace it.
	 */
	private static List<String[]> allDisplayRules = 
			new ArrayList<String[]>();
		
	/** Gets a copy of the rules for converting lightly formatted organic 
	 * chemistry text to HTML in the order in which the rules should be 
	 * applied.  
	 * @return	a list of arrays of Strings; 
	 * in each array, the first member is a regular expression, and the second
	 * is the String which should replace it.
	 */
	public static List<String[]> getAllDisplayRules() {
		return new ArrayList<String[]>(allDisplayRules);
	} // getAllDisplayRules()

	/** Resets the list to 0 members before rereading from the text file. */
	public static void reset() {
		allDisplayRules.clear();
	} // reset()

	/** Adds an array of two search-and-replace Strings, read from a file, to 
	 * the list. 
	 * This method is called by servlets/GroupLoader.java each time the code is
	 * recompiled; it does not need to be called from anywhere else.
	 * @param	rule	an array in which the first member is a regular
	 * expression, and the second is the String which should replace it
	 */
	public static void addRule(String[] rule) {
		if (rule == null || rule.length != 2 
				|| rule[0] == null || rule[1] == null) {
			return;
		}
		allDisplayRules.add(rule);
	} // addRule(String[])
	
	/** Loads from a text file the rules for converting lightly formatted 
	 * organic chemistry text to HTML in the order in which the rules should 
	 * be applied.  
	 * @param	file	text file containing the rules; format is:
	 * <br>regEx [tab] replacement
	 * @return	the number of rules loaded
	 * @throws	IOException	if the file containing the rules can't be read
	 */
	public static int loadRules(String file) throws IOException {
		final BufferedReader rdr = new BufferedReader(
				new InputStreamReader(
					new FileInputStream(file), StandardCharsets.UTF_8));
		int ruleCt = 0;
		while (true) {
			final String line = rdr.readLine();
			if (line == null) break;
			if (Utils.isWhitespace(line)) continue;
			if (line.charAt(0) == '*') continue;
			String[] rule = line.split("\t");
			if (rule.length == 1) {
				rule = new String[] {rule[0], ""};
			}
			if (rule.length == 2) {
				allDisplayRules.add(rule);
				ruleCt++;
			} else {
				System.out.println("DisplayRules.loadRules: malformed rule:\n"
						+ "'" + line + "'");
			}
		} // while 
		rdr.close();
		return ruleCt;
	} // loadRules(String)

	/** Disables external instantiation. */
	private DisplayRules() { }

} // DisplayRules
