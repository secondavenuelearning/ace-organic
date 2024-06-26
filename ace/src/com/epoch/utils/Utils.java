package com.epoch.utils;

import chemaxon.struc.DPoint3;
import chemaxon.struc.MDocument;
import chemaxon.struc.MPoint;
import chemaxon.struc.MolAtom;
import chemaxon.struc.Molecule;
import chemaxon.struc.SelectionMolecule;
import chemaxon.struc.graphics.MPolyline;
import chemaxon.marvin.io.MolExportException;
import com.epoch.constants.FormatConstants;
import com.epoch.assgts.Assgt;
import com.epoch.evals.Evaluator;
import com.epoch.chem.ChemUtils;
import com.epoch.chem.MolString;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.unbescape.html.HtmlEscape;

/** Array, List, String manipulation methods. */
public final class Utils implements FormatConstants {

	private static void debugPrint(Object... msg) {
		// printToLog(msg);
	}

	/** Parameter for toValidHrefOrURI(). */
	public static final boolean FOR_URI = true;
	/** Value for convertAmps in toValidTextbox(). */
	public static final int NEVER = 0;
	/** Value for convertAmps in toValidTextbox(). */
	public static final int EXCEPT_CER_NUMS = 1;
	/** Value for convertAmps in toValidTextbox(). */
	public static final int ALWAYS = 2;
	/** Parameter for getRandName(). */
	public static final boolean EXCLUDE1IlO0 = true;
	/** Parameter for removeBadTags().  */
	private static final boolean FOR_JS = true;
	/** Character that starts right-to-left text. */
	private static final String START_RTL = "&#8235;";
	/** Character that forces text to return to left-to-right. */
	private static final String END_RTL = "&#8237;";
	/** Parameter for <code>toDisplay()</code>.  */
	public static final boolean SUPERSCRIPT_RGROUP_NUMS = true;
	/** Parameter for <code>addSpanString()</code>.  */
	public static final boolean TO_DISPLAY = true;
	/** Parameter for <code>join()</code>; separator for lists or arrays */
	public static final String COMMA = ", ";
	/** Separator for Molecules, MolAtoms.  */
	private static final String DOT = ".";

/* *********************** Logging methods ********************/

	/** Prints a series of Strings, Integers, Molecules, etc. to the log.
	 * @param	msg	the series of Strings and other Objects to print
	 */
	public static void alwaysPrint(Object... msg) {
		printToLog(msg, SMILES);
	} // alwaysPrint(Object...)

	/** Prints a series of Strings, Integers, Molecules, etc. to the log.
	 * @param	msg	the series of Strings and other Objects to print
	 */
	public static void alwaysPrintMRV(Object... msg) {
		printToLog(msg, MRV);
	} // alwaysPrint(Object...)

	/** Prints a series of Strings, Integers, Molecules, etc. to the log.
	 * @param	msg	the series of Strings and other Objects to print
	 */
	public static void alwaysPrintSMARTS(Object... msg) {
		printToLog(msg, SMARTS);
	} // alwaysPrint(Object...)

	/** Prints a series of Strings, Integers, Molecules, etc. to the log.
	 * @param	msg	the series of Strings and other Objects to print
	 */
	public static void printToLog(Object... msg) {
		printToLog(msg, SMILES);
	} // alwaysPrint(Object..., String)

	/** Prints a series of Strings, Integers, Molecules, etc. to the log.
	 * @param	msg	the series of Strings and other Objects to print
	 */
	public static void printToLogMRV(Object... msg) {
		printToLog(msg, MRV);
	} // alwaysPrint(Object...)

	/** Prints a series of Strings, Integers, Molecules, etc. to the log.
	 * @param	msg	the series of Strings and other Objects to print
	 */
	public static void printToLogSMARTS(Object... msg) {
		printToLog(msg, SMARTS);
	} // alwaysPrint(Object...)

	/** Prints a series of Strings, Integers, Molecules, etc. to the log.
	 * @param	msg	the series of Strings and other Objects to print
	 * @param	format	format into which to print any Molecules in the array
	 */
	public static void printToLog(Object[] msg, String format) {
		for (final Object obj : msg) printObject(obj, format);
		System.out.println();
		System.out.flush();
	} // printToLog(Object[], String)

	/** Prints a String, Integer, Molecule, etc. to the log.  May be called
	 * recursively.
	 * @param	obj	an Object to print
	 * @param	format	format into which to print any Molecules in the array
	 */
	private static void printObject(Object obj, String format) {
		if (obj == null) {
			System.out.print("null");
		} else if (obj instanceof MolAtom) {
			final MolAtom atom = (MolAtom) obj;
			System.out.print(ChemUtils.isMulticenterAtom(atom) 
					? "[multicenter]" : atom.getSymbol());
			final int chg = atom.getCharge();
			if (chg != 0) {
				System.out.print('[');
				if (chg > 0) System.out.print('+');
				System.out.print(chg);
				System.out.print(']');
			} // if atom is charged
		} else if (obj instanceof Molecule) {
			try {
				System.out.print(MolString.toString((Molecule) obj, format));
			} catch (Exception e1) {
				try {
					System.out.print(MolString.toString((Molecule) obj, SMARTS));
				} catch (Exception e2) {
					System.out.print(MolString.toString((Molecule) obj, MRV));
				} // try
			} // try
		} else if (obj instanceof SelectionMolecule) {
			final SelectionMolecule sm = (SelectionMolecule) obj;
			System.out.print(sm.getFormula());
			final int chg = sm.getTotalCharge();
			if (chg != 0) {
				System.out.print("^"); 
				if (chg > 0) System.out.print("+");
				System.out.print(chg);
			} // if molecule is charged
		} else if (obj instanceof MDocument) {
			try {
				System.out.print(MolString.toString((MDocument) obj, MRV));
			} catch (MolExportException e) {
				System.out.print("[MDocument export exception occurred]");
			} // try
		} else if (obj instanceof DPoint3) {
			System.out.print(((DPoint3) obj).toString());
		} else if (obj instanceof MPoint) {
			// need to copy MPoint to get location to prevent infinite recursion in
			// certain rare instances; as of Marvin 5.11, copy constructor omits
			// location, so use clone instead
			printObject(((MPoint) obj).clone().getLocation(), format); // recursive call
		} else if (obj instanceof MPolyline) {
			printObject(((MPolyline) obj).getPoints(), format); // recursive call
		} else if (obj instanceof Date) {
			final DateFormat df = DateFormat.getDateTimeInstance(
					DateFormat.SHORT, DateFormat.LONG);
			System.out.print(df.format(((Date) obj).getTime()));
		} else if (obj instanceof Assgt) {
			System.out.print(((Assgt) obj).toString());
		} else if (obj instanceof Evaluator) {
			System.out.print(((Evaluator) obj).toString());
		} else if (obj instanceof byte[]) {
			System.out.print(Arrays.toString(bytesToChars((byte[]) obj)));
		} else if (obj instanceof int[]) {
			System.out.print(Arrays.toString((int[]) obj));
		} else if (obj instanceof float[]) {
			System.out.print(Arrays.toString((float[]) obj));
		} else if (obj instanceof double[]) {
			System.out.print(Arrays.toString((double[]) obj));
		} else if (obj instanceof long[]) {
			System.out.print(Arrays.toString((long[]) obj));
		} else if (obj instanceof char[]) {
			System.out.print(Arrays.toString((char[]) obj));
		} else if (obj instanceof boolean[]) {
			System.out.print(Arrays.toString((boolean[]) obj));
		} else if (obj instanceof Object[] || obj instanceof List) { 
			// includes multidimensional lists and arrays, e.g. int[][]
			final boolean isList = obj instanceof List;
			List<?> list = null;
			Object[] array = null;
			int numItems;
			if (isList) {
				list = (List<?>) obj;
				numItems = list.size();
			} else {
				array = (Object[]) obj;
				numItems = array.length;
			} // if is list
			System.out.print('[');
			String divider = null;
			for (int itemNum = 0; itemNum < numItems; itemNum++) {
				final Object item =
						(isList ? list.get(itemNum) : array[itemNum]);
				if (itemNum == 0) divider = getDivider(item);
				else System.out.print(divider);
				printObject(item, format); // recursive call
			} // for each object in the collection
			System.out.print(']');
		} else if (obj instanceof String) {
			System.out.print(unicodeToCERs((String) obj));
		} else System.out.print(obj);
	} // printObject(Object, String)

	/** Returns the divider appropriate to the collection of the object.
	 * @param	item	an object
	 * @return	dot for Molecule or MolAtom, comma-space otherwise
	 */
	private static String getDivider(Object item) {
		return (item instanceof Molecule 
				|| item instanceof MolAtom ? DOT : COMMA);
	} // getDivider(Object)

	/** Determines if any byte sequences in a String are consistent with UTF-8
	 * or extended Latin-1 encoding, and converts them to character entity 
	 * representations.
	 * @param	str	String to be converted
	 * @return	the converted String
	 */
	public static String inputToCERs(String str) {
		final CharReencoder encoder = new CharReencoder(str);
		return encoder.inputToCERs();
	} // inputToCERs(String)

	/** Convert character entity references in an ASCII string into a proper 
	 * Unicode string understood by Java (which is in UCS-2).
	 * @param	str	ASCII String with CERs 
	 * @return	same contents encoded in UCS-2 (Java internal representation)
	 */
	public static String cersToUnicode(String str) {
		final CharReencoder encoder = new CharReencoder(str);
		return encoder.cersToUnicode();
	} // cersToUnicode(String)

	/** Converts a string encoded by Javascript's encodeURIComponent() into
	 * regular characters.
	 * @param	str	string encoded by Javascript's encodeURIComponent()
	 * @return	string with decoded special characters
	 */
	public static String urisToText(String str) {
		final CharReencoder encoder = new CharReencoder(str);
		return encoder.urisToText();
	} // urisToText(String)

	/** Determines whether the language follows different alphabetization rules
	 * from English.
	 * @param	language	the language
	 * @return	true if the language follows different alphabetization rules
	 * from English
	 */
	public static boolean realphabetize(String language) {
		return CharReencoder.realphabetize(language);
	} // realphabetize(String)

	/** Converts character entity references, including accented characters, 
	 * to characters appropriate for alphabetization.
	 * @param	str	ASCII String with CERs 
	 * @return	modified String
	 */
	public static String cersToAlphabetical(String str) {
		final CharReencoder encoder = new CharReencoder(str);
		return encoder.cersToAlphabetical();
	} // cersToAlphabetical(String)

	/** Converts accented Unicode letters to accentless ones.
	 * @param	str	Unicode String
	 * @return	modified String
	 */
	public static String unicodeToAccentless(String str) {
		final CharReencoder encoder = new CharReencoder(str);
		return encoder.unicodeToAccentless();
	} // unicodeToAccentless(String)

	/** Converts non-ASCII Unicode characters in a string into their CERs.
	 * @param	str	a string
	 * @return	string with only ASCII characters and CERs
	 */
	public static String unicodeToCERs(String str) {
		final CharReencoder encoder = new CharReencoder(str);
		return encoder.unicodeToCERs();
	} // unicodeToCERs(String)

	/** Capitalizes the first letter of a string, even if accented.
	 * @param	str	a string
	 * @return	string with first letter capitalized
	 */
	public static String capitalize(String str) {
		final CharReencoder encoder = new CharReencoder(str);
		return encoder.capitalize();
	} // capitalize(String)

	/** Capitalizes all the letters of the string.
	 * @param	str	a string
	* @return	string with all letters capitalized
	*/
	public static String toUpperCase(String str) {
		return (str == null ? str : str.toUpperCase());
	} // toUpperCase(String)

	/** Converts character entity references, including accented characters, 
	/** Determines whether a string contains only ASCII characters.
	 * @param	str	a string
	 * @return	true if the string contains only ASCII characters
	 */
	public static boolean isAsciiOnly(String str) {
		boolean result = true;
		if (str != null) {
			result = str.equals(unicodeToCERs(str));
		} 
		return result;
	} // isAsciiOnly(String)

/* ************ Array-List-String interconversion methods ***************/

	/** Gets whether a character represents a letter or digit. 
	 * @param	ch	a character
	 * @return	true if the character represents a letter or digit
	 */
	public static boolean isAlphanumeric(char ch) {
		return isLetter(ch) || isDigit(ch);
	} // isAlphanumeric(char)

	/** Gets whether a character represents a letter. 
	 * @param	ch	a character
	 * @return	true if the character represents a letter
	 */
	public static boolean isLetter(char ch) {
		return isLowerCaseLetter(ch) || isUpperCaseLetter(ch);
	} // isLetter(char)

	/** Gets whether a character represents a lower-case letter. 
	 * @param	ch	a character
	 * @return	true if the character represents a lower-case letter
	 */
	public static boolean isLowerCaseLetter(char ch) {
		return MathUtils.inRange(ch, new int[] {'a', 'z'});
	} // isLetter(char)

	/** Gets whether a character represents an upper-case letter. 
	 * @param	ch	a character
	 * @return	true if the character represents an upper-case letter
	 */
	public static boolean isUpperCaseLetter(char ch) {
		return MathUtils.inRange(ch, new int[] {'A', 'Z'});
	} // isLetter(char)

	/** Gets whether a character represents a digit. 
	 * @param	ch	a character
	 * @return	true if the character represents a digit
	 */
	public static boolean isDigit(char ch) {
		return MathUtils.inRange(ch, new int[] {'0', '9'});
	} // isDigit(char)

	/** Gets the ordinal form of an integer.
	 * @param	num	an integer
	 * @return	the integer followed by the suffix st, nd, rd, or th
	 */
	public static String toOrdinal(int num) {
		return toString(num, num % 10 == 1 && num != 11 ? "st"
					: num % 10 == 2 && num != 12 ? "nd"
					: num % 10 == 3 && num != 13 ? "rd" : "th");
	} // toOrdinal(int)

	/** Converts an array of bytes to their int representations.
	 * @param	bts	an array of bytes
	 * @return	an array of the bytes' int representations
	 */
	private static int[] bytesToInts(byte[] bts) {
		final int numBytes = bts.length;
		int[] ints = new int[numBytes];
		for (int bNum = 0; bNum < numBytes; bNum++)
			ints[bNum] = byteToInt(bts[bNum]);
		return ints;
	} // bytesToInts(byte[])

	/** Converts an array of bytes to their int representations.
	 * @param	bts	an array of bytes
	 * @return	an array of the bytes' int representations
	 */
	private static char[] bytesToChars(byte[] bts) {
		final int numBytes = bts.length;
		char[] chars = new char[numBytes];
		for (int bNum = 0; bNum < numBytes; bNum++)
			chars[bNum] = (char) byteToInt(bts[bNum]);
		return chars;
	} // bytesToChars(byte[])

	/** Converts an array of bytes representing text into a String, with 
	 * Unicode characters converted to their character entity references 
	 * (CERs).
	 * @param	bts	array of bytes
	 * @return	String containing CERs
	 */
	public static String bytesToUnicodeString(byte[] bts) {
		return unicodeToCERs(CharReencoder.bytesToUnicodeString(bts));
	} // bytesToUnicodeString(byte[])

	/** Converts a byte to its int representation.
	 * @param	bt	a byte
	 * @return	the byte's int representation
	 */
	static int byteToInt(byte bt) {
		return (0xFF & (int) bt);
	} // byteToInt(byte)

	/** Pads a String by adding leading zeroes.
	 * @param	str	a String
	 * @param	len	desired length of the String
	 * @return	the padded String
	 */
	public static String padBinary(String str, int len) {
		return StringUtils.leftPad(str, len, '0');
	} // padBinary(String, int)

	/** Gets a more conventional String representation of a number.
	 * @param	number	a number
	 * @return	string representation of number with conversions of 
	 * - to &minus;, 0 before decimal, 
	 * &times; 10<sup><i>n</i></sup> instead of E<i>n</i>
	 */
	public static String doubleToStr(double number) {
		final StringBuilder bld = new StringBuilder();
		final String numStr = String.valueOf(number);
		final String[] coeffExpon = numStr.split("E");
		final String posCoeff = addMinusIfNeg(bld, coeffExpon[0]);
		if (posCoeff.charAt(0) == '.') bld.append('0');
		bld.append(posCoeff.endsWith(".0") 
				? rightChop(posCoeff, 2) : posCoeff);
		final boolean haveExponent = coeffExpon.length > 1;
		if (haveExponent) {
			final String posExpon = addMinusIfNeg(bld, coeffExpon[1]);
 			appendTo(bld, " &times; 10<sup>", posExpon, "</sup>");
		} // if there's an exponent
		return bld.toString();
	} // doubleToStr(double)

	/** If a string representing a number begins with -, appends a
	 * minus sign to the growing StringBuilder and removes the - from 
	 * the beginning of the string.
	 * @param	bld	a growing StringBuilder
	 * @param	num	a string representing a number
	 * @return	the number without any leading -
	 */
	private static String addMinusIfNeg(StringBuilder bld, String num) {
		String numStr = num;
		if (numStr.charAt(0) == '-') {
			bld.append("&minus;");
			numStr = numStr.substring(1);
		} // if number begins with negative sign
		return numStr;
	} // addMinusIfNeg(StringBuilder, String)

	/** Gets the length of the string.
	 * @param	str	a string
	 * @return	0 if string is null, otherwise its length
	 */
	public static int getLength(String str) {
		return (str == null ? 0 : str.length());
	} // getLength(String)

	/** Gets the length of the array.
	 * @param	arr	an array
	 * @return	0 if array is null, otherwise its length
	 */
	public static int getLength(int[] arr) {
		return (arr == null ? 0 : arr.length);
	} // getLength(int[])

	/** Gets the length of the array.
	 * @param	arr	an array
	 * @return	0 if array is null, otherwise its length
	 */
	public static int getLength(boolean[] arr) {
		return (arr == null ? 0 : arr.length);
	} // getLength(boolean[])

	/** Gets the length of the array.
	 * @param	arr	an array
	 * @return	0 if array is null, otherwise its length
	 */
	public static int getLength(int[][] arr) {
		return (arr == null ? 0 : arr.length);
	} // getLength(int[][])

	/** Gets the length of the array.
	 * @param	arr	an array
	 * @return	0 if array is null, otherwise its length
	 */
	public static int getLength(Object[] arr) {
		return (arr == null ? 0 : arr.length);
	} // getLength(Object[])

	/** Gets the size of the list.
	 * @param	<L>	type of Object being counted
	 * @param	list	a list
	 * @return	0 if list is null, otherwise its size
	 */
	public static <L> int getSize(List<L> list) {
		return (list == null ? 0 : list.size());
	} // getSize(List<L>)

	/** Gets the size of the map.
	 * @param	<K>	type of Object of the keys
	 * @param	<V>	type of Object of the values
	 * @param	map	a map
	 * @return	0 if map is null, otherwise its size
	 */
	public static <K,V> int getSize(Map<K,V> map) {
		return (map == null ? 0 : map.size());
	} // getSize(Map<K,V>)

	/** Determines whether the array is null or has no members.
	 * @param	arr	an array
	 * @return	true if array is null or has no members
	 */
	public static boolean isEmpty(Object[] arr) {
		return ArrayUtils.isEmpty(arr);
	} // isEmpty(Object[])

	/** Determines whether the array is null or has no members.
	 * @param	arr	an array
	 * @return	true if array is null or has no members
	 */
	public static boolean isEmpty(boolean[] arr) {
		return ArrayUtils.isEmpty(arr);
	} // isEmpty(boolean[])

	/** Determines whether the array is null or has no members.
	 * @param	arr	an array
	 * @return	true if array is null or has no members
	 */
	public static boolean isEmpty(int[] arr) {
		return ArrayUtils.isEmpty(arr);
	} // isEmpty(int[])

	/** Determines whether the array is null or has no members.
	 * @param	arr	an array
	 * @return	true if array is null or has no members
	 */
	public static boolean isEmpty(double[] arr) {
		return ArrayUtils.isEmpty(arr);
	} // isEmpty(double[])

	/** Determines whether the array is null or has no members.
	 * @param	arr	an array
	 * @return	true if array is null or has no members
	 */
	public static boolean isEmpty(float[] arr) {
		return ArrayUtils.isEmpty(arr);
	} // isEmpty(float[])

	/** Determines whether the array is null or has no members.
	 * @param	arr	an array
	 * @return	true if array is null or has no members
	 */
	public static boolean isEmpty(byte[] arr) {
		return ArrayUtils.isEmpty(arr);
	} // isEmpty(byte[])

	/** Determines whether the List is null or has no members.
	 * @param	<L>	type of Object in the list
	 * @param	list	a List
	 * @return	true if List is null or has no members
	 */
	public static <L> boolean isEmpty(List<L> list) {
		return list == null || list.isEmpty();
	} // isEmpty(List<L>)

	/** Determines whether the Map is null or has no members.
	 * @param	<K>	type of Object of the keys
	 * @param	<V>	type of Object of the values
	 * @param	table	a Map
	 * @return	true if Map is null or has no members
	 */
	public static <K,V> boolean isEmpty(Map<K,V> table) {
		return table == null || table.isEmpty();
	} // isEmpty(Map<K,V>)

	/** Trims a string without throwing a NullPointerException.
	 * @param	str	the string
	 * @return	the trimmed string, or null if null
	 */
	public static String trim(String str) {
		return (str == null ? str : str.trim());
	} // trim(String)

	/** Trims a string, returning empty if null.
	 * @param	str	a string
	 * @return	the trimmed string, or "" if null
	 */
	public static String trimNullToEmpty(String str) {
		return (str == null ? "" : str.trim());
	} // trimNullToEmpty(String)

	/** Gets whether a string is empty or white space. 
	 * @param	str	a string
	 * @return	true if the string is empty or white space
	 */
	public static boolean isEmptyOrWhitespace(String str) {
		return isEmpty(str) || isWhitespace(str);
	} // isEmptyOrWhitespace(String)

	/** Gets whether a string is white space. 
	 * @param	str	a string
	 * @return	true if the string is white space
	 */
	public static boolean isWhitespace(String str) {
		return StringUtils.isWhitespace(str);
	} // isWhitespace(String)

	/** Determines if the String array is null or all of its
	 * members are empty or null. 
	 * @param	arr	a String array
	 * @return	true if the array's members are all empty or null
	 */
	public static boolean membersAreEmpty(String[] arr) {
		boolean areEmpty = true;
		if (arr != null) {
			for (final String member : arr) {
				areEmpty = isEmpty(member);
				if (!areEmpty) break;
			} // for each member
		}
		return areEmpty;
	} // membersAreEmpty(String[])

	/** Determines if the list is null or all of its members are null. 
	 * @param	list	a list
	 * @return	true if the list's members are all null
	 */
	public static boolean allMembersAreNull(List<String> list) {
		boolean areNull = true;
		if (list != null) for (final String member : list) {
			areNull = member == null;
			if (!areNull) break;
		} // for each member
		return areNull;
	} // allMembersAreNull(List<String>)

	/** Determines if the array is null or any of its members are null. 
	 * @param	arr	an array
	 * @return	true if any of the array's members are null
	 */
	public static boolean anyMembersAreNull(Object[] arr) {
		boolean anyAreNull = arr == null;
		if (!anyAreNull) {
			for (final Object member : arr) {
				anyAreNull = member == null;
				if (anyAreNull) break;
			} // for each member
		}
		return anyAreNull;
	} // anyMembersAreNull(Object[])

	/** Gets a copy of an array with a new reference.
	 * @param	arr	an array
	 * @return	a copy of the array
	 */
	public static String[] getCopy(String[] arr) {
		return Arrays.copyOf(arr, arr.length);
	} // getCopy(String[])

	/** Gets a copy of an array with a new reference.
	 * @param	arr	an array
	 * @return	a copy of the array
	 */
	public static int[] getCopy(int[] arr) {
		return Arrays.copyOf(arr, arr.length);
	} // getCopy(int[])

	/** Gets a copy of an array with a new reference.
	 * @param	arr	an array
	 * @return	a copy of the array
	 */
	public static boolean[] getCopy(boolean[] arr) {
		return Arrays.copyOf(arr, arr.length);
	} // getCopy(boolean[])

	/** Gets a copy of an array with a new reference.
	 * @param	arr	an array
	 * @return	a copy of the array
	 */
	public static byte[] getCopy(byte[] arr) {
		return Arrays.copyOf(arr, arr.length);
	} // getCopy(byte[])

	/** Gets the index of the first occurrence of an int in an array.
	 * @param	arr	the array
	 * @param	srch	the int we're looking for
	 * @return	the index of the first occurrence of the int in the 
	 * array; -1 if not found or array is null
	 */
	public static int indexOf(int[] arr, int srch) {
		return ArrayUtils.indexOf(arr, srch);
	} // indexOf(int[], int)

	/** Gets if an int is in an array.
	 * @param	arr	the array
	 * @param	srch	the int we're looking for
	 * @return	true if the int is in the array
	 */
	public static boolean contains(int[] arr, int srch) {
		return ArrayUtils.contains(arr, srch);
	} // contains(int[], int)

	/** Gets the index of the first occurrence of a double in an array.
	 * @param	arr	the array
	 * @param	srch	the double we're looking for
	 * @return	the index of the first occurrence of the double in the 
	 * array; -1 if not found or array is null
	 */
	public static int indexOf(double[] arr, double srch) {
		return ArrayUtils.indexOf(arr, srch);
	} // indexOf(double[], double)

	/** Gets if a double is in an array.
	 * @param	arr	the array
	 * @param	srch	the double we're looking for
	 * @return	true if the double is in the array
	 */
	public static boolean contains(double[] arr, double srch) {
		return ArrayUtils.contains(arr, srch);
	} // contains(double[], double)

	/** Gets the index of the first occurrence of a byte in an array.
	 * @param	arr	the array
	 * @param	srch	the byte we're looking for
	 * @return	the index of the first occurrence of the byte in the 
	 * array; -1 if not found or array is null
	 */
	public static int indexOf(byte[] arr, byte srch) {
		return ArrayUtils.indexOf(arr, srch);
	} // indexOf(byte[], byte)

	/** Gets if a byte is in an array.
	 * @param	arr	the array
	 * @param	srch	the byte we're looking for
	 * @return	true if the byte is in the array
	 */
	public static boolean contains(byte[] arr, byte srch) {
		return ArrayUtils.contains(arr, srch);
	} // contains(byte[], byte)

	/** Gets the index of the first occurrence of a long in an array.
	 * @param	arr	the array
	 * @param	srch	the long we're looking for
	 * @return	the index of the first occurrence of the long in the 
	 * array; -1 if not found or array is null
	 */
	public static int indexOf(long[] arr, long srch) {
		return ArrayUtils.indexOf(arr, srch);
	} // indexOf(long[], long)

	/** Gets if a long is in an array.
	 * @param	arr	the array
	 * @param	srch	the long we're looking for
	 * @return	true if the long is in the array
	 */
	public static boolean contains(long[] arr, long srch) {
		return ArrayUtils.contains(arr, srch);
	} // contains(long[], long)

	/** Gets the index of the first occurrence of a char in an array.
	 * @param	arr	the array
	 * @param	srch	the char we're looking for
	 * @return	the index of the first occurrence of the char in the 
	 * array; -1 if not found or array is null
	 */
	public static int indexOf(char[] arr, char srch) {
		return ArrayUtils.indexOf(arr, srch);
	} // indexOf(char[], char)

	/** Gets if a char is in an array.
	 * @param	arr	the array
	 * @param	srch	the char we're looking for
	 * @return	true if the char is in the array
	 */
	public static boolean contains(char[] arr, char srch) {
		return ArrayUtils.contains(arr, srch);
	} // contains(char[], char)

	/** Gets the index of the first occurrence of a String in an array.
	 * @param	arr	the array
	 * @param	srch	the String we're looking for
	 * @return	the index of the first occurrence of the String in the 
	 * array; -1 if not found or array is null
	 */
	public static int indexOf(String[] arr, String srch) {
		return ArrayUtils.indexOf(arr, srch);
	} // indexOf(String[], String)

	/** Gets if a String is in an array.
	 * @param	arr	the array
	 * @param	srch	the String we're looking for
	 * @return	true if the String is in the array
	 */
	public static boolean contains(String[] arr, String srch) {
		return ArrayUtils.contains(arr, srch);
	} // contains(String[], String)

	/** Gets if a double is in an array of indefinite length. Need to cast to
	 * Double to avoid compile errors for an unknown reason.
	 * @param	srch	the double we're looking for
	 * @param	arr	the array of indefinite length
	 * @return	true if the double is in the array
	 */
	public static boolean among(Double srch, double... arr) {
		return contains(arr, srch.doubleValue());
	} // among(Double, double...)

	/** Gets if an int is in an array of indefinite length. Need to cast to
	 * Integer to avoid compile errors for an unknown reason.
	 * @param	srch	the int we're looking for
	 * @param	arr	the array of indefinite length
	 * @return	true if the int is in the array
	 */
	public static boolean among(Integer srch, int... arr) {
		return contains(arr, srch.intValue());
	} // among(Integer, int...)

	/** Gets if a byte is in an array of indefinite length. Need to cast to
	 * Byte to avoid compile errors for an unknown reason.
	 * @param	srch	the byte we're looking for
	 * @param	arr	the array of indefinite length
	 * @return	true if the byte is in the array
	 */
	public static boolean among(Byte srch, byte... arr) {
		return contains(arr, srch.byteValue());
	} // among(Byte, byte...)

	/** Gets if a char is in an array of indefinite length. Need to cast to
	 * Character to avoid compile errors for an unknown reason.
	 * @param	srch	the char we're looking for
	 * @param	arr	the array of indefinite length
	 * @return	true if the char is in the array
	 */
	public static boolean among(Character srch, char... arr) {
		return contains(arr, srch.charValue());
	} // among(Character, char...)

	/** Gets if a long is in an array of indefinite length. Need to cast to
	 * Long to avoid compile errors for an unknown reason.
	 * @param	srch	the long we're looking for
	 * @param	arr	the array of indefinite length
	 * @return	true if the long is in the array
	 */
	public static boolean among(Long srch, long... arr) {
		return contains(arr, srch.longValue());
	} // among(Long, long...)

	/** Gets if a String is in an array of indefinite length.
	 * @param	srch	the String we're looking for
	 * @param	arr	the array of indefinite length
	 * @return	true if the String is in the array
	 */
	public static boolean among(String srch, String... arr) {
		return contains(arr, srch);
	} // among(String, String...)

	/** Converts an int array into a List of Integers.
	 * @param	array	an int array
	 * @return	a list of Integers
	 */
	public static List<Integer> intArrayToList(int[] array) {
		final List<Integer> list = new ArrayList<Integer>();
		if (array != null)
			for (final int memb : array) list.add(memb);
		return list;
	} // intArrayToList(int[])

	/** Converts a char array into a List of Characters.
	 * @param	array	a char array
	 * @return	a list of Characters
	 */
	public static List<Character> charArrayToList(char[] array) {
		final List<Character> list = new ArrayList<Character>();
		if (array != null)
			for (final char memb : array) list.add(Character.valueOf(memb));
		return list;
	} // charArrayToList(char[])

	/** Converts a list of Integers into an int array.
	 * @param	list	a list of Integers
	 * @return	an int array
	 */
	public static int[] listToIntArray(List<Integer> list) {
		if (list == null) return null;
		return toPrimitive(list.toArray(new Integer[list.size()]));
	} // listToIntArray(List<Integer>)

	/** Converts a list of Characters into a char array.
	 * @param	list	a list of Characters
	 * @return	a char array
	 */
	public static char[] listToCharArray(List<Character> list) {
		if (list == null) return null;
		return toPrimitive(list.toArray(new Character[list.size()]));
	} // listToCharArray(List<Character>)

	/** Converts an Integer to an int without throwing a NullPointerException.
	 * @param	anInt	the Integer
	 * @return	the Integer's value, or 0 if null
	 */
	public static int toPrimitive(Integer anInt) {
		return (anInt == null ? 0 : anInt.intValue());
	} // toPrimitive(Integer)

	/** Converts an array of Integer objects to an int array.
	 * @param	intArray	an array of Integer objects
	 * @return	an int array
	 */
	public static int[] toPrimitive(Integer[] intArray) {
	 	return ArrayUtils.toPrimitive(intArray);
	} // toPrimitive(Integer[])

	/** Converts an array of Character objects to a char array.
	 * @param	charArray	an array of Character objects
	 * @return	a char array
	 */
	public static char[] toPrimitive(Character[] charArray) {
	 	return ArrayUtils.toPrimitive(charArray);
	} // toPrimitive(Character[])

	/** Converts an array of ints to an array of Integer objects.
	 * @param	intArray	an array of ints 
	 * @return	an array of Integer objects
	 */
	public static Integer[] toObject(int[] intArray) {
	 	return ArrayUtils.toObject(intArray);
	} // toObject(int[])

	/** Converts an array of chars to an array of Character objects.
	 * @param	charArray	an array of chars 
	 * @return	an array of Character objects
	 */
	public static Character[] toObject(char[] charArray) {
	 	return ArrayUtils.toObject(charArray);
	} // toObject(char[])

	/** Converts a list of Doubles into a double array.
	 * @param	list	a list of Doubles
	 * @return	a double array
	 */
	public static double[] listToDoubleArray(List<Double> list) {
		if (list == null) return null;
		final int size = list.size();
		final double[] array = new double[size];
		for (int memb = 0; memb < size; memb++) {
			array[memb] = list.get(memb).doubleValue();
		}
		return array;
	} // listToDoubleArray(List<Double>)

	/** Converts an array of Strings representing integers into an int array.
	 * @param	strArray	an array of Strings representing integers
	 * @return	an int array
	 */
	public static int[] stringToIntArray(String[] strArray) {
		return stringToIntArray(strArray, 0);
	} // stringToIntArray(String[])
	
	/** Converts an array of Strings representing integers into an int array.
	 * @param	strArray	an array of Strings representing integers
	 * @param	errorValue	value to assign to member if parseInt() fails
	 * @return	an int array
	 */
	public static int[] stringToIntArray(String[] strArray, int errorValue) {
		if (strArray == null) return null;
		final int size = strArray.length;
		final int[] intArray = new int[size];
		for (int memb = 0; memb < size; memb++) {
			intArray[memb] = MathUtils.parseInt(strArray[memb].trim(), errorValue);
		}
		return intArray;
	} // stringToIntArray(String[], int)

	/** Converts a list of Strings representing integers into a list of
	 * Integers.
	 * @param	strList	a list of Strings representing integers
	 * @return	a list of Integers
	 */
	public static List<Integer> stringToIntList(List<String> strList) {
		return stringToIntList(strList.toArray(new String[strList.size()]), 0);
	} // stringToIntList(List<String>)

	/** Converts a list of Strings representing integers into a list of
	 * Integers.
	 * @param	strList	a list of Strings representing integers
	 * @param	errorValue	value to assign to member if parseInt() fails
	 * @return	a list of Integers
	 */
	public static List<Integer> stringToIntList(List<String> strList,
			int errorValue) {
		return stringToIntList(strList.toArray(new String[strList.size()]),
				errorValue);
	} // stringToIntList(List<String>, int)

	/** Converts an array of Strings representing integers into a list of
	 * Integers.
	 * @param	strArray	an array of Strings representing integers
	 * @return	a list of Integers
	 */
	public static List<Integer> stringToIntList(String[] strArray) {
		return stringToIntList(strArray, 0);
	} // stringToIntList(String[])
	
	/** Converts an array of Strings representing integers into a list of
	 * Integers.
	 * @param	strArray	an array of Strings representing integers
	 * @param	errorValue	value to assign to member if parseInt() fails
	 * @return	a list of Integers
	 */
	public static List<Integer> stringToIntList(String[] strArray,
			int errorValue) {
		if (strArray == null) return null;
		final List<Integer> intList = new ArrayList<Integer>();
		final Integer errorVal = errorValue;
		for (final String str : strArray) {
			try {
				intList.add(Integer.decode(str.trim()));
			} catch (NumberFormatException e) {
				intList.add(errorVal);
			}
		} // for each string
		return intList;
	} // stringToIntList(String[], int)

	/** Converts a Molecule array into a List.
	 * Conventional method for converting arrays to lists:
	 * 		<br>List&lt;Molecule&gt; list =
	 * 				new ArrayList&lt;Molecule&gt;(Arrays.asList(array));
	 * <br>doesn't work for Molecules.
	 * @param	molArray	a Molecule array
	 * @return	a list of Molecules
	 */
	public static List<Molecule> molArrayToList(Molecule[] molArray) {
		if (molArray == null) return null;
		final List<Molecule> list = new ArrayList<Molecule>();
		for (final Molecule mol : molArray) list.add(mol);
		return list;
	} // molArrayToList()
	
	/** Converts a Molecule list into an array.
	 * @param	molList	a list of Molecules
	 * @return	a Molecule array
	 */
	public static Molecule[] molListToArray(List<Molecule> molList) {
		return (molList == null ? null
				: molList.toArray(new Molecule[molList.size()]));
	} // molListToArray(List<Molecule>)
	
	/** Converts an array of Molecules into a SMILES representation.
	 * @param	molArray	an array of Molecules
	 * @return	the SMILES representation of the molecules in the array
	 */
	public static String molArrayToString(Molecule[] molArray) {
		return molArrayToString(molArray, SMILES);
	} // molArrayToString(Molecule[])
	
	/** Converts an array of Molecules into a string representation.
	 * @param	molArray	an array of Molecules
	 * @param	format	format of the representation
	 * @return	the string representation of the molecules in the array
	 */
	public static String molArrayToString(Molecule[] molArray, String format) {
		String out = "";
		final int len = getLength(molArray);
		if (len > 0) {
			final Molecule fused = new Molecule();
			for (final Molecule mol : molArray) fused.fuse(mol.clone());
			out = MolString.toString(fused, format);
		} // if there are molecules in the array
		return out;
	} // molArrayToString(Molecule[], String)

	/** Converts an int array to a String with comma-space separators.
	 * @param	items	an array of ints
	 * @return	a single string containing ints separated by comma-space 
	 */
	public static String join(int[] items) {
		return StringUtils.join(toObject(items), COMMA);
	} // join(int[])

	/** Converts an int array to a String with a given separator.
	 * @param	items	an array of ints
	 * @param	separator	the String to put between the ints, often
	 * comma-space or colon
	 * @return	a single string containing ints separated by the separator
	 */
	public static String join(int[] items, String separator) {
		return StringUtils.join(toObject(items), separator);
	} // join(int[], String)

	/** Converts an Object array to a String with comma-space separators.
	 * @param	items	an array of Objects
	 * @return	a single string containing Objects separated by comma-space
	 */
	public static String join(Object[] items) {
		return StringUtils.join(items, COMMA);
	} // join(Object[])

	/** Converts an Object array to a String with a given separator.
	 * @param	items	an array of Objects
	 * @param	separator	the String to put between the Objects, often
	 * comma-space or colon
	 * @return	a single string containing Objects separated by the separator
	 */
	public static String join(Object[] items, String separator) {
		return StringUtils.join(items, separator);
	} // join(Object[], String)

	/** Converts a list to a String with comma-space separators.
	 * @param	<L>	type of Object in the list
	 * @param	items	a list
	 * @return	a single string containing numbers separated by comma-space
	 */
	public static <L> String join(List<L> items) {
		return StringUtils.join(items.iterator(), COMMA);
	} // join(List<L>)

	/** Converts a list to a String with a given separator.
	 * @param	<L>	type of Object in the list
	 * @param	items	a list
	 * @param	separator	the String to put between the numbers, often
	 * comma-space or colon
	 * @return	a single string containing numbers separated by the separator
	 */
	public static <L> String join(List<L> items, String separator) {
		return StringUtils.join(items.iterator(), separator);
	} // join(List<L>, String)

	/** Converts an int array to a String separated by comma-spaces while
	 * incrementing each int.
	 * @param	items	an array of ints
	 * @param	increment	an increment for each int
	 * @return	a single string containing incremented ints separated by
	 * comma-spaces
	 */
	public static String join(int[] items, int increment) {
		return join(items, COMMA, increment);
	} // join(int[], int)

	/** Converts an int array to a String with a given separator while
	 * incrementing each int.
	 * @param	items	an array of ints
	 * @param	separator	the String to put between the ints, often
	 * comma-space or colon
	 * @param	increment	an increment for each int
	 * @return	a single string containing incremented ints separated by
	 * the separator
	 */
	public static String join(int[] items, String separator, int increment) {
		if (isEmpty(items)) return "";
		final StringBuilder out = new StringBuilder();
		boolean first = true;
		for (final int item : items) {
			if (first) first = false;
			else out.append(separator);
			out.append(item + increment);
		}
		return out.toString();
	} // join(int[], String, int)

	/** Converts a list of Integers to a String separated by comma-spaces while
	 * incrementing each Integer.
	 * @param	items	a list of Integers
	 * @param	increment	an increment for each Integer
	 * @return	a single string containing incremented Integers separated by
	 * comma-spaces
	 */
	public static String join(List<Integer> items, int increment) {
		return join(items, COMMA, increment);
	} // join(List<Integer>, int)

	/** Converts a list of Integers to a String with a given separator while
	 * incrementing each Integer.
	 * @param	items	a list of Integers
	 * @param	separator	the String to put between the Integers, often
	 * comma-space or colon
	 * @param	increment	an increment for each Integer
	 * @return	a single string containing incremented Integers separated by
	 * the separator
	 */
	public static String join(List<Integer> items, String separator, 
			int increment) {
		if (isEmpty(items)) return "";
		final StringBuilder out = new StringBuilder();
		boolean first = true;
		for (final Integer item : items) {
			if (first) first = false;
			else out.append(separator);
			out.append(item.intValue() + increment);
		}
		return out.toString();
	} // join(List<Integer>, String, int)

	/** Creates a single String consisting of consecutive numbers from 
	 * base to limit, separated by separator.
	 * @param	base	starting int
	 * @param	limit	ending int
	 * @param	separator	the separator, often comma-space or colon
	 * @return	a single String of consecutive joined numbers
	 */
	public static String joinInts(int base, int limit, String separator) {
		final StringBuilder out = joinIntsBld(base, limit, separator);
		return out.toString();
	} // joinInts(int, int, String)

	/** Creates a single String consisting of consecutive numbers from 
	 * base to limit, separated by separator.
	 * @param	base	starting int
	 * @param	limit	ending int
	 * @param	separator	the separator, often comma-space or colon
	 * @return	a StringBuilder containing consecutive joined numbers
	 */
	public static StringBuilder joinIntsBld(int base, int limit, String separator) {
		final StringBuilder out = new StringBuilder();
		boolean first = true;
		for (int num = base; num < limit + base; num++) {
			if (first) first = false;
			else out.append(separator);
			out.append(num);
		}
		return out;
	} // joinIntsBld(int, int, String)

	/** Combines the two arrays into a single new array.
	 * @param	arr	an array
	 * @param	values	values to add
	 * @return	a new array with all the new elements added
	 */
	public static int[] add(int[] arr, int... values) {
		return addAll(arr, values);
	} // add(int[], int...)

	/** Adds elements to the end of an array.
	 * @param	array	the array
	 * @param	values	values to be inserted into the array
	 * @return	a new, modified array
	 */
	public static Object[] add(Object[] array, Object... values) {
		return insertArray(array.length, array, values);
	} // add(Object[], Object...)

	/** Combines the two arrays into a single new array.
	 * @param	arr1	an array
	 * @param	arr2	another array
	 * @return	a new array with all the elements of the original arrays
	 */
	public static int[] addAll(int[] arr1, int[] arr2) {
		return insertArray(arr1.length, arr1, arr2);
	} // addAll(int[], int[])

	/** Combines the two arrays into a single new array.
	 * @param	arr1	an array
	 * @param	arr2	another array
	 * @return	a new array with all the elements of the original arrays
	 */
	public static Object[] addAll(Object[] arr1, Object[] arr2) {
		return insertArray(arr1.length, arr1, arr2);
	} // addAll(Object[], Object[])

	/** Inserts elements of an array into an array before the element with the 
	 * given index, so that the first element to be added has the given index 
	 * in the new array.
	 * @param	index	0-based index of insertion position
	 * @param	origArr	the original array
	 * @param	values	array containing values to be inserted into the array
	 * @return	a new, modified array
	 */
	public static int[] insertArray(int index, int[] origArr, int[] values) {
		final int[] newArr = new int[origArr.length + values.length];
		System.arraycopy(origArr, 0, newArr, 0, index);
		System.arraycopy(values, 0, newArr, index, values.length);
		System.arraycopy(origArr, index, newArr, index + values.length,
				origArr.length - index);
		return newArr;
	} // insertArray(int, int[], int[])

	/** Inserts values into an array before the element with the 
	 * given index, so that the first element to be added has the given index 
	 * in the new array.
	 * @param	index	0-based index of insertion position
	 * @param	arr	the original array
	 * @param	values	array containing values to be inserted into the array
	 * @return	a new, modified array
	 */
	public static Object[] insert(int index, Object[] arr, Object... values) {
		return insertArray(index, arr, values);
	} // insert(int, Object[], Object...)

	/** Inserts elements of an array into an array before the element with the 
	 * given index, so that the first element to be added has the given index 
	 * in the new array.
	 * @param	index	0-based index of insertion position
	 * @param	origArr	the original array
	 * @param	values	array containing values to be inserted into the array
	 * @return	a new, modified array
	 */
	public static Object[] insertArray(int index, Object[] origArr, 
			Object[] values) {
		final Object[] newArr = new Object[origArr.length + values.length];
		System.arraycopy(origArr, 0, newArr, 0, index);
		System.arraycopy(values, 0, newArr, index, values.length);
		System.arraycopy(origArr, index, newArr, index + values.length,
				origArr.length - index);
		return newArr;
	} // insertArray(int, Object[], Object[])

	/** Removes a string from the end of a given string, otherwise returns the
	 * original string unchanged.
	 * @param	str	string to be shortened
	 * @param	remove	string to be removed
	 * @return	the shortened (or not) string
	 */
	public static String removeFromEnd(String str, String remove) {
		return StringUtils.removeEnd(str, remove);
	} // removeFromEnd(String, String)

	/** Trims the string and converts all whitespace to single spaces.
	 * @param	text	the string
	 * @return	the string with whitespace condensed to single spaces
	 */
	public static String condenseWhitespace(String text) {
		return (text == null ? text : text.trim().replaceAll("\\str+", " "));
	} // condenseWhitespace(String)

	/** Determines whether the string is null or "".
	 * @param	str	a string
	 * @return	true if string is null or "".
	 */
	public static boolean isEmpty(String str) {
		return StringUtils.isEmpty(str);
	} // isEmpty(String)

	/** Determines whether the string is null or whitespace.
	 * @param	str	a string
	 * @return	true if string is null or whitespace.
	 */
	public static boolean isBlank(String str) {
		return StringUtils.isBlank(str);
	} // isBlank(String)

	/** Determines whether the string is "true" or "Y" or "1".
	 * @param	str	a string
	 * @return	true if string is "true" or "Y" or "1".
	 */
	public static boolean isPositive(String str) {
		return Utils.among(str, "true", "Y", "1");
	} // isPositive(String)

	/** Counts the number of times a character appears in a string.
	 * @param	str	the string
	 * @param	item	the character
	 * @return	the count
	 */
	public static int countChar(String str, char item) {
		if (str == null) return 0;
		int count = 0;
		for (final char theChar : str.toCharArray())
			if (theChar == item) count++;
		return count;
	} // countChar(String, char)

	/** Splits a string and trims the segments.
	 * @param	str	the string
	 * @param	bound	where to split the string
	 * @return	array of trimmed segments of the string
	 */
	public static String[] splitTrim(String str, String bound) {
		final String[] segms = str.split(bound);
		for (int segmNum = 0; segmNum < segms.length; segmNum++) {
			segms[segmNum] = segms[segmNum].trim();
		} // for each segment 
		return segms;
	} // splitTrim(String, String)

	/** Combines all the objects into a string.
	 * @param	items	the items to be combined into a string
	 * @return	the combined string
	 */
	public static String toString(Object... items) {
		final StringBuilder bld = new StringBuilder();
		for (final Object item : items) bld.append(item);
		return bld.toString();
	} // toString(Object...)

	/** Combines all the objects into a StringBuilder.
	 * @param	items	the items to be combined into a StringBuilder
	 * @return	the combined StringBuilder
	 */
	public static StringBuilder getBuilder(Object... items) {
		final StringBuilder bld = new StringBuilder();
		for (final Object item : items) bld.append(item);
		return bld;
	} // getBuilder(Object...)

	/** Combines all the objects into the given StringBuilder, modifying it.
	 * @param	bld	the StringBuilder
	 * @param	items	the items to be added to the StringBuilder
	 */
	public static void appendTo(StringBuilder bld, Object... items) {
		for (final Object item : items) bld.append(item);
	} // appendTo(StringBuilder, Object...)

	/** Maps the members of an array to themselves.
	 * @param	arr	the array
	 * @return	a map of each member of the array to itself
	 */
	public static Map<String, String> mapToSelf(String[] arr) {
		final Map<String, String> map = new LinkedHashMap<String, String>();
		for (int itemNum = 0; itemNum < arr.length; itemNum++) {
			map.put(arr[itemNum], arr[itemNum]);
		} // for each phrase
		return map;
	} // mapToSelf(String[])

	/** Merges two maps of int arrays keyed by Objects. If the two maps have 
	 * the same key, their respective values are combined into a single, sorted 
	 * array with all duplicates eliminated. The keys are sorted as well before 
	 * being returned to the first original map. Modifies the first map!
	 * @param	map0	the first map (becomes modified)
	 * @param	<K>	generic representing the class of the keys
	 * @param	<V>	generic representing the class of the values
	 * @param	map1Orig	the second map
	 */
	public static <K, V> void mergeMaps(Map<K, int[]> map0, 
			Map<K, int[]> map1Orig) {
		final Map<K, int[]> map1 = new HashMap<K, int[]>(map1Orig);
		final Map<K, int[]> newMap = new HashMap<K, int[]>();
		final Set<K> map0Keys = map0.keySet();
		for (final K map0Key : map0Keys) {
			final int[] map0Arr = map0.get(map0Key);
			final List<Integer> valuesList = intArrayToList(map0Arr);
			// remove from second map any array keyed by key from first map
			// and combine it with array from first map
			final int[] map1Arr = map1.remove(map0Key);
			valuesList.addAll(intArrayToList(map1Arr));
			sortAndRemoveDuplicates(valuesList);
			newMap.put(map0Key, listToIntArray(valuesList));
		} // for each key in first map
		// get keys remaining in second map that were not in first map
		final Set<K> map1Keys = map1.keySet();
		for (final K map1Key : map1Keys) {
			final int[] map1Arr = map1.get(map1Key);
			final List<Integer> valuesList = intArrayToList(map1Arr);
			sortAndRemoveDuplicates(valuesList);
			newMap.put(map1Key, listToIntArray(valuesList));
		} // for each key in first map
		// sort the keys in case map0 is LinkedHashMap
		final List<K> newMapKeys = new ArrayList<K>(newMap.keySet());
		newMapKeys.sort(null);
		// repopulate map0 with newMap contents, with keys ordered
		map0.clear();
		for (final K newMapKey : newMapKeys) {
			map0.put(newMapKey, newMap.get(newMapKey));
		} // for each map key
	} // mergeMaps(Map<K, int[]>, Map<K, int[]>)

	/** Sorts and removes duplicates from a list of Objects.
	 * @param	list	the list
	 * @param	<L>	generic representing the class of the list items
	 */
	private static <L> void sortAndRemoveDuplicates(List<L> list) {
		list.sort(null);
		final int size = list.size();
		for (int num = size - 1; num >= 1; num--) {
			if (list.get(num).equals(list.get(num - 1))) {
				list.remove(num);
			} // if the list item matches the previous list item
		} // for each member of the list
	} // sortAndRemoveDuplicates(List<L>)

/* ************ Java-Javascript-HTML String interconversion methods ***************/

	/** Convert line breaks to \n so variable can be passed in AJAX.
	 * @param	str	string to modify
	 * @return	string with line breaks converted to \n
	 */
	public static String lineBreaksToJS(String str) {
		if (str == null) return "";
		final StringBuilder out = new StringBuilder(str.length());
		for (final char theChar : str.toCharArray()) {
			switch (theChar) {
				case '\n': out.append("\\n"); break;
				case '\r': /* out.append("\\r"); */ break;
				default: 
					out.append(theChar); break;
			} // switch theChar
		} // for each character
		return removeBadTags(out.toString(), FOR_JS);
	} // lineBreaksToJS(String)

	/** Convert line breaks to \r so variable can be passed in AJAX;
	 * This routine is most likely not called.  
	 * @param	str	string to modify
	 * @return	string with line breaks converted to \r
	 */
	public static String lineBreaksToJSr(String str) {
		if (str == null) return "";
		final StringBuilder out = new StringBuilder(str.length());
		for (final char theChar : str.toCharArray()) {
			switch (theChar) {
				case '\n': out.append("\\r"); break;
				case '\r': /* out.append("\\r"); */ break;
				default: 
					out.append(theChar); break;
			} // switch theChar
		} // for each character
		return removeBadTags(out.toString(), FOR_JS);
	} // lineBreaksToJS(String)

	/** Converts \r and \n back to line breaks.
	 * @param	str	string to modify
	 * @return	string with line breaks restored
	 */
	public static String restoreLineBreaks(String str) {
		if (str == null) return "";
		final String mod = str.replaceAll("\\\\r", "\r").replaceAll("\\\\n", "\n");
		debugPrint("Utils.restoreLineBreaks: converted:\n",
				str, "\nto:\n", mod);
		return mod;
	} // restoreLineBreaks(String)

	/** Converts a Java String to a Javascript-compatible String.
	 * E.g.,
	 * <br>var str = '&lt;%= Utils.toValidJS(str) %&gt;';
	 * <P>IMPORTANT: assumes that the Javascript String is inside single quotes.
	 * @param	str	a string
	 * @return	Javascript-compatible string
	 */
	public static String toValidJS(String str) {
		if (str == null) return "";
		final StringBuilder out = new StringBuilder(str.length());
		for (final char theChar : str.toCharArray()) {
			switch (theChar) {
				case '\n': out.append("\\n"); break;
				case '\r': out.append("\\r"); break;
				case '\'': out.append("\\'"); break;
				case '\\': out.append("\\\\"); break;
				/* The following case converts '</foo>' => '<\/foo>'
				 * It is necessary so that closing tags disappear from the view
				 * of the HTML parser so the page will pass HTML validation.
				 * Closing tags are parsed even inside <!-- HTML comments -->.
				 */
				case '/': out.append("\\/"); break;
				default:  out.append(theChar); break;
			} // switch theChar
		} // for each character
		return removeBadTags(out.toString(), FOR_JS);
	} // toValidJS(String)

	/** Convert a string so that the raw data can be displayed and edited in
	 * input textboxes and textareas.  Character entity references representing 
	 * foreign language characters will not be converted and will display as 
	 * the foreign language characters.
	 * @param	str   a string
	 * @return	textbox-compatible string
	 */
	public static String toValidTextbox(String str) {
		return toValidTextbox(str, EXCEPT_CER_NUMS);
	} // toValidTextbox(String)

	/** Convert a string so that the raw data can be displayed and edited in
	 * input textboxes and textareas.
	 * @param	str   a string
	 * @param	convertAmps	whether to convert ampersands; if EXCEPT_CER_NUMS 
	 * (usual case), ampersands followed by # will not be converted to 
	 * &amp;amp; and, as a result, character entity references will be 
	 * displayed as the characters.
	 * @return	converted string
	 */
	public static String toValidTextbox(String str, int convertAmps) {
		if (str == null) return "";
		final char[] array = str.toCharArray();
		final int len = array.length;
		final StringBuilder out = new StringBuilder();
		final Pattern CER_PATTERN = Pattern.compile("&#\\d+;.*");
		for (int charNum = 0; charNum < len; charNum++) {
			final char theChar = array[charNum];
			if (!tryAppend(out, theChar)) {
				// theChar is \n, \r, \, or &; append unchanged unless 
				// convertAmps is EXCEPT_CER_NUMS and theChar begins &#n;
				boolean appendAmpCER = theChar == '&' && convertAmps != NEVER;
				if (appendAmpCER && convertAmps == EXCEPT_CER_NUMS) {
					final Matcher matcher = 
							CER_PATTERN.matcher(str.substring(charNum));
					appendAmpCER = (!matcher.find() || matcher.start() != 0);
				} // if maybe should not append &amp;
				out.append(appendAmpCER ? "&amp;" : theChar); 
			} // if not standard treatment
		} // for each character
		return out.toString();
	} // toValidTextbox(String, int)

	/** Converts a Java String to a String that can be assigned to a value of
	 * an option or hidden input HTML attribute.
	 * E.g.,
	 * <br>&lt;input type="hidden" 
	 * 		value="&lt;%= Utils.toValidHTMLAttributeValue(str) %&gt;" /&gt;
	 * @param	str	a string
	 * @return	attribute-compatible string
	 */
	public static String toValidHTMLAttributeValue(String str) {
		if (str == null) return "";
		final StringBuilder out = new StringBuilder(str.length());
		for (final char theChar : str.toCharArray()) {
			if (!tryAppend(out, theChar)) switch (theChar) {
				case '\n': out.append("\\n"); break;
				case '\r': out.append("\\r"); break;
				case '\\': out.append("&#92;"); break;
				case '&': out.append("&amp;"); break;
				default: out.append(theChar); break;
			} // switch theChar if not standard treatment
		} // for each character
		return out.toString();
	} // toValidHTMLAttributeValue(String)

	/** Utility method for toValidTextbox(), toValidHTMLAttributeValue(),
	 * and XMLUtils.toValidXML().  Throws \n, \r, \, and &amp; back to 
	 * calling methods because they treat them differently; 
	 * appends character entity references for &#39;, &quot;, &lt;, and &gt; 
	 * to a growing Stringbuilder; appends other characters unchanged.
	 * @param	out	a StringBuilder
	 * @param	theChar	the character to be inspected
	 * @return	true if we have already appended a character to the
	 * StringBuilder
	 */
	private static boolean tryAppend(StringBuilder out, char theChar) {
		final boolean standard = theChar != '\n' 
				&& theChar != '\r' 
				&& theChar != '\\' 
				&& theChar != '&';
		if (standard) switch (theChar) {
			case '\'': out.append("&#39;"); break;
			case '"': out.append("&quot;"); break;
			case '<': out.append("&lt;"); break;
			case '>': out.append("&gt;"); break;
			default: out.append(theChar); break;
		} // switch theChar
		return standard;
	} // tryAppend(StringBuilder, char)

	/** Convert a string so that the raw data can be displayed as HTML.
	 * Use toValidTextbox() for displaying in textboxes.
	 * @param	str   a string
	 * @return	string formatted for HTML
	 */
	public static String toValidHTML(String str) {
		if (str == null) return "";
		String out = toValidTextbox(str);
		out = out.replaceAll("\\n\\r", "<br/>");
		out = out.replaceAll("(\\r)?\\n", "<br/>"); 
		out = out.replaceAll("\\r", "<br/>");
		return out;
	} // toValidHTML(String)

	/** Convert the string so that it can be used as a valid file name.
	 * Convert whitespace and '/' to '_', remove quotes.
	 * @param	str   a string
	 * @return	string that can be used as a filename
	 */
	public static String toValidFileName(String str) {
		if (str == null) return "_";
		final StringBuilder out = new StringBuilder(str.length());
		for (final char theChar : str.toCharArray()) {
			switch (theChar) {
				case ' ':
				case '\t':
				case '\r':		 
				case '\n':		 
				case '/':		 
						  out.append('_'); break;
				case '\'':		 
				case '"':		 
						  out.append(""); break;
				default: out.append(theChar); break;
			} // switch theChar
		} // for each character
		return out.toString();
	} // toValidFileName(String)

	/** Convert the string so that it can be used as information in a URI.
	 * Supposed to be equivalent to Javascript encodeURIComponent().
	 * Similar to java.net.URLEncoder.encode(), but not identical.
	 * @param	str   a string
	 * @return	string that can be used as a URI
	 */
	public static String toValidURI(String str) {
		return toValidHrefOrURI(str, FOR_URI);
	} // toValidURI(String)

	/** Convert a complete pathname so that it can be used as an href.
	 * Do not use on filename that already includes URIs (parameters).
	 * Same as encodeURIComponent(), but omits /.
	 * @param	str   a string
	 * @return	string that can be used as an href
	 */
	public static String toValidHref(String str) {
		return toValidHrefOrURI(str, !FOR_URI);
	} // toValidHref(String)

	/** Convert a complete pathname so that it can be used as an href.
	 * Do not use on filename that already includes URIs (parameters).
	 * If forURI is false, same as encodeURIComponent().
	 * @param	str   a string
	 * @param	forURI	whether this string will be used as part of a URI
	 * @return	string that can be used as an href
	 */
	private static String toValidHrefOrURI(String str, boolean forURI) {
		if (str == null) return "";
		final StringBuilder out = new StringBuilder(str.length());
		for (final char theChar : str.toCharArray()) {
			switch (theChar) {
				case '"': out.append("%22"); break;
				case '#': out.append("%23"); break;
				case '$': out.append("%24"); break;
				case '%': out.append("%25"); break;
				case '&': out.append("%26"); break;
				case '+': out.append("%2B"); break;
				case ',': out.append("%2C"); break;
				case ':': out.append("%3A"); break;
				case ';': out.append("%3B"); break;
				case '<': out.append("%3C"); break;
				case '=': out.append("%3D"); break;
				case '>': out.append("%3E"); break;
				case '?': out.append("%3F"); break;
				case '@': out.append("%40"); break;
				case '[': out.append("%5B"); break;
				case '\\': out.append("%5C"); break;
				case ']': out.append("%5D"); break;
				case '^': out.append("%5E"); break;
				case '`': out.append("%60"); break;
				case '{': out.append("%7B"); break;
				case '|': out.append("%7C"); break;
				case '}': out.append("%7D"); break;
				case ' ':
				case '\t':
				case '\r':
				case '\n':
						  out.append("%20"); break;
				case '/': out.append(forURI ? "%2F" : '/'); break;
				default: 
					out.append(theChar); break;
			} // switch theChar
		} // for each character
		return out.toString();
	} // toValidHrefOrURI(String, boolean)

	/** Find the extension of a file.
	 * @param	filepath	name of file
	 * @return	the extension
	 */
	public static String getExtension(String filepath) {
		final int posn = filepath.lastIndexOf('.') + 1;
		return (posn > 0 && posn < filepath.length() ?
				filepath.substring(posn) : "");
	} // getExtension(String)

	/** Gets a sort key from a chemical name.
	 * Locants i and stereochemistry descriptors (numbers, letters before a hyphen)
	 * are moved to the end of the sort key.
	 * @param	name	the chemical name
	 * @return	key for sorting the name
	 */
	public static String makeSortName(String name) {
		String sortName = name;
		final int posn = sortName.indexOf('-');
		if (posn > 0) {
			final char ch0 = sortName.charAt(0);
			final String start = sortName.substring(0, posn);
			if (('1' <= ch0 && ch0 <= '9')
					|| Utils.among(ch0, '#', '&')
					|| start.length() == 1
					|| start.indexOf(',') >= 0
					|| start.startsWith("cis")
					|| start.startsWith("trans")) {
				sortName = sortName.substring(posn + 1) + "-" + start;
			} // if the name begins with a locant or stereochemical descriptor
		} // if the name contains a hyphen
		return sortName;
	} // makeSortName(String)

	/** Formats lightly formatted organic chemistry statements, feedback, and
	 * others into strings for display as HTML, excluding HTML tag attributes.
	 * Rules are stored in WEB-INF/displayRules.txt.
	 * @param	str	a string
	 * @return	organic-chemistry-related string formatted for display
	 */
	public static String toDisplay(String str) {
		return toDisplay(str, !SUPERSCRIPT_RGROUP_NUMS);
	} // toDisplay(String)

	/** Formats lightly formatted organic chemistry statements, feedback, and
	 * others into strings for display as HTML, excluding HTML tag attributes.
	 * Rules are stored in WEB-INF/displayRules.txt.
	 * @param	str	a string
	 * @param	haveRGroups	when true, superscript numbers following R
	 * @return	organic-chemistry-related string formatted for display
	 */
	public static String toDisplay(String str, boolean haveRGroups) {
		if (isEmpty(str)) return "";
		final String SELF = "Utils.toDisplay: ";
		final List<String[]> displayRules = 
				DisplayRules.getAllDisplayRules();
		final StringBuilder bld = new StringBuilder();
		final Pattern TAG_PATTERN = 
				Pattern.compile("<[^>]*=(\\str)*\"[^\"]*\"[^>]*>");
		final Matcher matcher = TAG_PATTERN.matcher(str);
		int prevEnd = 0;
		while (true) {
			if (!matcher.find()) break;
			final int start = matcher.start();
			final int end = matcher.end();
 			appendTo(bld, toDisplay(str.substring(prevEnd, start), 
					haveRGroups, displayRules), str.substring(start, end));
			prevEnd = end;
		} // until pattern not found
		if (prevEnd < str.length()) {
 			appendTo(bld, toDisplay(str.substring(prevEnd), haveRGroups, 
					displayRules));
		} // if last pattern was not at end of string
		final String converted = bld.toString();
		// debugPrint(SELF + "old string: ", str, "\nconverted to: ", converted);
		return converted;
	} // toDisplay(String, boolean)

	/** Formats lightly formatted organic chemistry statements, feedback, and
	 * others into strings for display as HTML.
	 * @param	str	a string
	 * @param	haveRGroups	when true, superscript numbers following R
	 * @param	displayRules	regular expressions for formatting
	 * @return	organic-chemistry-related string formatted for display
	 */
	private static String toDisplay(String str, boolean haveRGroups,
			List<String[]> displayRules) {
		if (isEmpty(str)) return "";
		String result = str;
		// find & characters and convert them to &amp; if they do not begin
		// character entity references
		final StringBuilder bld = new StringBuilder();
		final String[] pieces = result.split("&", -1);
		bld.append(pieces[0]);
		// some named CERs have numbers; must insert carets to preserve them
		final String[] NUMERIC_CERS = new String[] 
				{"frac12", "frac14", "frac34", "sup2", "sup3", "there4"};
		for (int pNum = 1; pNum < pieces.length; pNum++) {
			bld.append(ampBeginsCER('&' + pieces[pNum]) ? '&' : "&amp;");
			for (final String numericCER : NUMERIC_CERS) {
				if (pieces[pNum].startsWith(numericCER)) {
					pieces[pNum] = pieces[pNum].replaceFirst("([a-z]+)", "$1^");
					break;
				} // if the CER contains a number
			} // for each named CER that contains a number
			bld.append(pieces[pNum]);
		} // for each piece of the string
		result = bld.toString();
		if (displayRules.isEmpty())
			System.out.println("Utils.toDisplay: No rules.");
		else for (final String[] rule : displayRules) {
			if (rule[0].startsWith("(H|B")) { // X[n]-Y to X[n]&ndash;Y
				String prevResult = result;
				while (true) {
					result = result.replaceAll(rule[0], rule[1]);
					if (result.equals(prevResult)) break;
					else prevResult = result;
				} // while the string is still changing
			} else if (rule[0].charAt(0) != 'R' || haveRGroups) {
				result = result.replaceAll(rule[0], rule[1]);
			} // if should apply the rule nonce, once (usually), or repeatedly
		} // for each rule
		return removeBadTags(result, !FOR_JS);
	} // toDisplay(String, boolean, List<String[]>)

	/** Determines whether a string begins with a character entity reference
	 * understood by HTML5.
	 * @param	str	a string beginning with its only &amp;
	 * @return	true if the string begins with a character entity reference
	 * understood by HTML5.
	 */
	private static boolean ampBeginsCER(String str) {
		final int semicolonPosn = str.indexOf(';');
		if (semicolonPosn < 0) return false;
		final String possibleCER = str.substring(0, semicolonPosn + 1);
		final String possibleUnicode = HtmlEscape.unescapeHtml(possibleCER);
		return !possibleCER.equals(possibleUnicode);
	} // ampBeginsCER(String)

	/** Replaces numbers and +, -, &ndash;, and &minus; characters inside HTML
	 * subscript and superscript tags with Character Entity References.
	 * @param	src	a string
	 * @return	organic-chemistry-related string formatted for a popup menu
	 */
	public static String toPopupMenuDisplay(String src) {
		if (src == null) return "";
		final int TAG_ON_LEN = "<sup>".length();
		final int TAG_OFF_LEN = "</sup>".length();
		final int KIND_POSN = "<sup>".indexOf('p'); 
				// posn of char differentiating <sub> and <sup>
		final char SUPER = "<sup>".charAt(KIND_POSN); 
				// char identifying <sup>
		final String NDASH = "&ndash;";
		final String MINUS = "&minus;";
		final int NDASH_LEN = NDASH.length();
		final int MINUS_LEN = MINUS.length();
		final int SUPER_CER = 8304; 
				// where superscripts start in Character Entity Reference set
		final int SUB_CER = 8320; 
				// where subscripts start in Character Entity Reference set
		String result = toDisplay(src).replaceAll("<(i|/i)>", "");
		while (true) {
			final int tagOnPosn = result.indexOf("<su");
			if (tagOnPosn < 0) break; // no more tags
			final int oldLen = result.length();
			final char kindOfTag = result.charAt(tagOnPosn + KIND_POSN);
			final int startCER = (kindOfTag == SUPER ? SUPER_CER : SUB_CER);
			int tagOffPosn = result.indexOf("</su");
			if (tagOffPosn < 0) // unlikely
				tagOffPosn = oldLen;
			final StringBuilder replacement = new StringBuilder();
			for (int posn = tagOnPosn + TAG_ON_LEN; posn < tagOffPosn; posn++) {
				final char oneChar = result.charAt(posn);
				if (Character.isDigit(oneChar)) {
					final int intValue = Character.getNumericValue(oneChar);
					if (intValue >= 1 && intValue <= 3 && kindOfTag == SUPER) {
 						replacement.append(getBuilder("&sup", intValue, ';'));
					} else {
 						replacement.append(getBuilder("&#", startCER + intValue, 
								';'));
					}
				} else if (oneChar == '+') {
 					replacement.append(getBuilder("&#", startCER + 10, ';'));
				} else {
					final String substr = result.substring(posn);
					final boolean startsMINUS = substr.startsWith(MINUS);
					final boolean startsNDASH = substr.startsWith(NDASH);
					if (oneChar == '-' || startsMINUS || startsNDASH) {
 						replacement.append(getBuilder(
								"&#", startCER + 11, ';'));
						if (startsMINUS) posn += MINUS_LEN - 1;
						else if (startsNDASH) posn += NDASH_LEN - 1;
					} else replacement.append(oneChar);
				} // if oneChar
			} // for each character in sup or sub tags
			final int nextTextPosn = tagOffPosn + TAG_OFF_LEN;
			final StringBuilder newResult = getBuilder(
					result.substring(0, tagOnPosn), replacement);
			if (nextTextPosn < oldLen)
 				newResult.append(result.substring(nextTextPosn));
			result = newResult.toString();
		} // while there are still <sup> and <sub>
		return result;
	} // toPopupMenuDisplay(String)

	/** Converts a hyphen to a minus sign.
	 * @param	num	the number
	 * @return	the number with a leading minus sign if it is negative
	 */
	public static String formatNegative(int num) {
		return formatNegative(String.valueOf(num));
	} // formatNegative(int)

	/** Converts a hyphen to a minus sign.
	 * @param	num	the number
	 * @return	the number with a leading minus sign if it is negative
	 */
	public static String formatNegative(double num) {
		return formatNegative(String.valueOf(num));
	} // formatNegative(double)

	/** Converts a hyphen to a minus sign.
	 * @param	num	the number as a string
	 * @return	the number with a leading minus sign if it is negative
	 */
	public static String formatNegative(String num) {
		return (num == null ? num : num.replaceAll("-", "&minus;"));
	} // formatNegative(String)

	/** Limits a string to the given number of characters and adds ...
	 * @param	str	a string
	 * @param	maxLen	limit to length of the string
	 * @return	the truncated string
	 */
	public static String chopString(String str, int maxLen) {
		if (str == null || str.length() < maxLen - 3) return str;
		else return str.substring(0, maxLen - 4) + "...";
	} // chopString(String, int)

	/** Limits a string to the given number of characters without breaking up
	 * character entity references.
	 * @param	str	a string
	 * @param	maxLen	limit to length of the string
	 * @return	the truncated string
	 */
	public static String chopCERString(String str, int maxLen) {
		if (str == null) return str;
		// cut string to max length in Unicode format
		String mod = cersToUnicode(str);
		final int len = mod.length();
		if (len > maxLen) mod = mod.substring(0, maxLen); 
		mod = unicodeToCERs(mod);
		// if CER format is still too long, cut string to max length, one
		// Unicode character at a time
		while (mod.length() > maxLen) {
			final String unicode = cersToUnicode(mod);
			mod = unicodeToCERs(rightChop(unicode, 1));
		} // while the string is too long
		return mod;
	} // chopCERString(String, int)

	/** Removes the last characters from a string.
	 * @param	str	a string
	 * @param	trim	the number of characters to remove from the right
	 * @return	the string with characters removed
	 */
	public static String rightChop(String str, int trim) {
		if (str == null || trim == 0) return str;
		return str.substring(0, str.length() - trim);
	} // rightChop(String, int)

	/** Removes the first and last characters from a string.
	 * @param	str	a string
	 * @param	left	the number of characters to remove from the left
	 * @param	right	the number of characters to remove from the right
	 * @return	the string with characters removed
	 */
	public static String endsChop(String str, int left, int right) {
		if (str == null) return str;
		final int len = str.length();
		if (left >= len || right >= len || left > len - right) return "";
		return str.substring(left, len - right);
	} // endsChop(String, int, int)

	/** Limits a string to the given number of characters after it has been
	 * displayed, and adds an ellipsis "...".  Accounts for HTML tags and 
	 * character entity references. 
	 * @param	str	a string
	 * @param	maxLen	limit to length of the string
	 * @return	the truncated string
	 */
	public static String chopDisplayStr(String str, int maxLen) {
		if (str == null || str.length() <= maxLen) return str;
		final String SELF = "Utils.chopDisplayStr: ";
		final Pattern CER_OR_TAG = Pattern.compile("<[^>]+>|&[^;]+;");
		Matcher matcher = CER_OR_TAG.matcher(str);
		final int wholeLen = str.length();
		int lengthSoFar = 0;
		int preMatchBegin = 0;
		boolean matchFound = false;
		final List<Integer> lengthAtChar = new ArrayList<Integer>();
		final int CER = 0;
		final int TAG = 1;
		final int TEXT = 2;
		while (true) {
			// deal with text up to the next CER/tag or end of string
			matchFound = matcher.find();
			final int nextMatchBegin = 
					(matchFound ? matcher.start() : wholeLen);
			for (int posn = preMatchBegin; posn < nextMatchBegin; posn++) {
				lengthAtChar.add(++lengthSoFar);
			}
			if (!matchFound || lengthSoFar > maxLen) break;
			// deal with CER/tag's contribution to length
			final String match = matcher.group();
			final int matchType = (match.charAt(0) == '<' ? TAG 
					: ampBeginsCER(match) ? CER : TEXT);
			// CERs add 1 to length, HTML tags add 0
			if (matchType == CER) lengthSoFar++;
			for (final char dummy : match.toCharArray()) {
				if (matchType == TEXT) lengthSoFar++;
				lengthAtChar.add(lengthSoFar);
			} // for each character in the CER or tag
			preMatchBegin = matcher.end();
			matchFound = (preMatchBegin < wholeLen);
			if (!matchFound || lengthSoFar > maxLen) break;
		} // while we still have matches 
		// see if reached end without overflowing
		if (!matchFound && lengthSoFar <= maxLen) {
			return removeBadTags(str, !FOR_JS);
		} // if string is no longer than desired
		final String ELLIPSIS = "...";
		final int targetLen = maxLen - ELLIPSIS.length();
		// truncate after last character at length targetLen
		final StringBuilder bld = new StringBuilder();
		for (int posn = 0; posn < lengthAtChar.size(); posn++) {
			if (lengthAtChar.get(posn).intValue() > targetLen) {
				bld.append(str.substring(0, posn));
				break;
			} // if the character is past the desired length
		} // for each character in str
		// find any tags left open by truncation
		final List<String> openTags = new ArrayList<String>();
		final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
		matcher = HTML_TAG.matcher(bld.toString());
		while (matcher.find()) {
			final String match = matcher.group();
			if ((match.startsWith("</") || match.startsWith("<\\/"))
					&& !openTags.isEmpty()) {
				openTags.remove(openTags.size() - 1);
			} else if (!match.endsWith("/>")) {
				openTags.add(match);
			} // nature of tag
		} // while more tags are found
		// close any open tags
		final Pattern SPACES = Pattern.compile("\\str");
		while (!openTags.isEmpty()) {
			final String openTag = openTags.remove(openTags.size() - 1);
			bld.append("</");
			matcher = SPACES.matcher(openTag);
			if (matcher.find()) {
 				appendTo(bld, openTag.substring(1, matcher.start()), '>');
			} else bld.append(openTag.substring(1));
		} // while there are unclosed tags
		bld.append(ELLIPSIS);
		return removeBadTags(bld.toString(), !FOR_JS);
	} // chopDisplayStr(String, int)

	/** Gets the substring of a string up to and including a substring.
	 * @param	str	the parent string
	 * @param	sub	the substring
	 * @return	the substring of str up to and including sub
	 */
	public static String substringToWith(String str, String sub) {
		final int posn = str.indexOf(sub);
		return posn < 0 ? str : str.substring(0, posn + sub.length());
	} // substringToWith(String, String)

	/** Generates a random string of width 15.
	 * @return	a random string of width 15
	 */
	public static String getRandName() {
		final StringBuilder str = new StringBuilder();
		final int stringWidth = 15;
		final String firstChar = "abcdefghijkmnopqrstuvwxyz";
		final String otherChars = "abcdefghijkmnopqrstuvwxyz0123456789";
		final int otherLength = otherChars.length();
		final Random random = new Random();
		str.append(otherChars.charAt(random.nextInt(firstChar.length())));
		for (int charNum = 2; charNum <= stringWidth; charNum++) {
			// generate a random character
			str.append(otherChars.charAt(random.nextInt(otherLength)));
		} // for each character
		return str.toString();
	} // getRandName(int, boolean)

	/** Generates a random string of numbers and letters. 
	 * @param	stringWidth	the length of the string to generate
	 * @return	a random string
	 */
	public static String getRandName(int stringWidth) {
		return getRandName(15, !EXCLUDE1IlO0);
	} // getRandName(int)

	/** Generates a random string of numbers and letters. 
	 * @param	stringWidth	the length of the string to generate
	 * @param	exclude1IlO0	whether to exclude the characters 1, I, l, O, 0
	 * @return	a random string
	 */
	public static String getRandName(int stringWidth, boolean exclude1IlO0) {
		final String firstChar = exclude1IlO0
				? "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ"
				: "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		final String otherChars = exclude1IlO0 
				? "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
				: "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		final int otherLength = otherChars.length();
		final StringBuilder str = new StringBuilder(stringWidth + 1);
		final Random random = new Random();
		str.append(firstChar.charAt(random.nextInt(firstChar.length())));
		for (int charNum = 2; charNum <= stringWidth; charNum++) {
			// generate a random character
			str.append(otherChars.charAt(random.nextInt(otherLength)));
		} // for each character
		return str.toString();
	} // getRandName(int, boolean)

	/** Get the entire HTTP URL of this request up to and including the
	 * context of the application.
	 * @param	request	an HTTP servlet request
	 * @return	the URL
	 */
	public static String getFullURL(HttpServletRequest request) {
		final int HTTP_DEFAULT_PORT = 80;
		final int HTTPS_DEFAULT_PORT = 443;
		final StringBuilder answer = new StringBuilder();
		String protocol =
			request.getProtocol().split("/")[0].toLowerCase(Locale.ENGLISH);
		final int port = request.getServerPort();
		if ("http".equals(protocol) && port == HTTPS_DEFAULT_PORT) {
			protocol = "https";
		}
 		answer.append(getBuilder(protocol, "://", request.getServerName()));
		if (("https".equals(protocol) && port != HTTPS_DEFAULT_PORT)
				|| (!"https".equals(protocol) && port != HTTP_DEFAULT_PORT)) {
 			answer.append(getBuilder(':', port));
		}
		return answer.append(request.getContextPath()).toString();
	} // getFullURL(HttpServletRequest)

	/** Trims new-line (\n and \r) characters from the end of a string.
	 * @param	str	the string
	 * @return	the trimmed string
	 */
	public static String trimTrailingNewLines(String str) {
		if (str == null) return str;
		String answer = str;
		while (true) {
			final int len = answer.length();
			if (len > 1) {
				final char lastChar = answer.charAt(len - 1);
				if (Utils.among(lastChar, '\n', '\r'))
					answer = answer.substring(0, len - 1);
				else break;
			} else if (len == 1) {
				answer = "";
			} else {
				break;
			}
		} // while
		return answer;
	} // trimTrailingNewLines(String)
	
	/** Modifies &lt;/script&gt; in text so it won't mess up display.
	 * Designed so that it is easily expanded if necessary.  Proper display
	 * requires that JavaScript variables be enclosed in single quotes!
	 * @param	str	a string
	 * @param	forJS	whether the conversion is for a Javascript variable
	 * @return	string containing a display-compatible version of
	 * &lt;/script&gt;
	 */
	private static String removeBadTags(String str, boolean forJS) {
		if (str == null) return "";
		final String[] badTags = new String[]
				{"</script>", "</head>", "</body>", "</html>"};
		String result = str;
		for (final String badTag : badTags) {
			final String subst = forJS
				? badTag.replaceFirst("</", "</' + '")
				: badTag.replaceFirst("</", "<&#47;");
			result = result.replaceAll(badTag, subst);
		} // for each bad tag
		return result;
	} // removeBadTags(String, boolean)

	/** Forces a String to return to LTR directionality at the end. 
	 * @param	str	a String that may be RTL
	 * @return	the String appended with the LTR override character
	 */
	public static String group(String str) {
		String answer = str;
		if (str != null && str.indexOf(START_RTL) >= 0) {
			answer += END_RTL;
		}
		return answer;
	} // group(String)

	/** Adds span tags around a string.
	 * @param	str	the string
	 * @return	the spanned string
	 */
	public static String spanString(String str) {
		final StringBuilder bld = new StringBuilder();
		addSpanString(bld, str, !TO_DISPLAY);
		return bld.toString();
	} // spanString(String)

	/** Adds span tags and a string to a growing StringBuilder.
	 * @param	bld	the StringBuilder
	 * @param	str	the string
	 */
	public static void addSpanString(StringBuilder bld, String str) {
		addSpanString(bld, str, TO_DISPLAY);
	} // addSpanString(StringBuilder, String)

	/** Adds span tags and a string to a growing StringBuilder.
	 * @param	bld	the StringBuilder
	 * @param	str	the string
	 * @param	doToDisplay	whether to format the text according to chemistry
	 * rules
	 */
	public static void addSpanString(StringBuilder bld, String str, 
			boolean doToDisplay) {
 		appendTo(bld, "<span class=\"string\">", 
				doToDisplay ? toDisplay(str) : str, "</span>");
	} // addSpanString(StringBuilder, String, boolean)

	/** Strips the path from a file or directory name. 
	 * @param	fullname	full name of the file or directory
	 * @return	the name of the bottommost file or directory
	 */
	public static String stripFilePath(String fullname) {
		if (fullname == null) return fullname;
		final String[] parts = fullname.split("/");
		return parts[parts.length - 1];
	} // stripFilePath(String)

	/** Disables external instantiation. */
	private Utils() { }

} // Utils
