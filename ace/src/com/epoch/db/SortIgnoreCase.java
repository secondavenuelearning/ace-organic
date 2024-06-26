package com.epoch.db;

import com.epoch.utils.Utils;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
// import java.io.Serializable;

/** Allows strings to be sorted ignoring case and accent. */
public class SortIgnoreCase implements Comparator<String> {

	public static final long serialVersionUID = 42L;
	/* classes that implement Comparator are advised to also be Serializable

	/** Constructor. */
	public SortIgnoreCase() {
		// empty constructor
	}

	/** Compares two strings.
	 * @param	s1	the first string
	 * @param	s2	the second string
	 * @return	-1 if the first sorts earlier than the second, 1 if the
	 * opposite, and 0 if they are equal
	 */
	public int compare(String s1, String s2) {
		if (s1 == null && s2 == null) return 0;
		else if (s1 == null) return 1;
		else if (s2 == null) return -1;
		final String s1Upper = Utils.unicodeToAccentless(
				Utils.cersToUnicode(s1)).toUpperCase(Locale.ENGLISH);
		final String s2Upper = Utils.unicodeToAccentless(
				Utils.cersToUnicode(s2)).toUpperCase(Locale.ENGLISH);
		if (s1Upper.equals(s2Upper)) return 0;
		final String[] strs = new String[] {s1Upper, s2Upper};
		Arrays.sort(strs);
		return (strs[0].equals(s1Upper) ? -1 : 1);
	} // compare(String, String)

} // SortIgnoreCase
