package com.epoch.utils;

import static com.epoch.utils.utilConstants.CharConstants.*;
import com.epoch.db.TranslnRead;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;

/** Methods for interconverting UTF-8 and Latin-1 characters, character entity 
 * references, and UCS-2 characters. */
class CharReencoder {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** String to be reencoded, converting non-ASCII characters to character
	 * entity references or character entity references to UCS-2 characters. */
	transient private String s;
	/** Contains the growing converted string. */
	transient private StringBuilder bld;

	/** Constructor.
	 * @param	str	String to be converted
	 */
	CharReencoder(String str) {
		s = str;
	} // CharReencoder(String)

	/** Converts an array of bytes representing text in the UTF-8 encoding 
	 * method into a string containing UCS-2 (Unicode) characters.
	 * @param	bts	array of bytes
	 * @return	a Unicode-containing String
	 */
	static String bytesToUnicodeString(byte[] bts) {
		String str = "";
		try {
			str = new String(bts, UTF8);
		} catch (UnsupportedEncodingException e) {
			str = new String(bts);
		}
		return str;
	} // bytesToUnicodeString(byte[])

	/** Converts a string inputted by a user from UTF-8 encoding
	 * to UCS-2 (Unicode) characters.  
	 * We assume that the front end has collected any input in UTF-8 
	 * format, not Latin-1 or MS-1252; forms acquiring such input should 
	 * include the attribute,
	 * <blockquote>accept-charset="UTF-8"</blockquote>
	 * and the action page should contain the line,
	 * <blockquote>request.setCharacterEncoding("UTF-8");</blockquote>
	 * @return	the converted String
	 */
	String inputToUnicode() {
		if (s == null) return s;
		final String SELF = "CharReencoder.inputToUnicode: ";
		final String mod = bytesToUnicodeString(s.getBytes());
		debugPrint(SELF + "converted: ", s, "\nto: ", mod);
		return mod;
	} // inputToUnicode()

	/** Converts non-ASCII UCS-2 characters in a string into the 
	 * corresponding character entity references.
	 * @return	string with only ASCII characters and CERs
	 */
	String unicodeToCERs() {
		if (s == null) return s;
		final String SELF = "CharReencoder.unicodeToCERs: ";
		bld = new StringBuilder(s.length());
		for (final char theChar : s.toCharArray()) {
			if (theChar >= 128) {
				bld.append("&#").append((int) theChar).append(';');
			} else bld.append(theChar);
		} // for each character
		return bld.toString();
	} // unicodeToCERs()

	/** Converts a string inputted by a user from UTF-8 encoding
	 * to character entity references.  
	 * We assume that the front end has collected any input in UTF-8 
	 * format, not Latin-1 or MS-1252; forms acquiring such input should 
	 * include the attribute,
	 * <blockquote>accept-charset="UTF-8"</blockquote>
	 * and the action page should contain the line,
	 * <blockquote>request.setCharacterEncoding("UTF-8");</blockquote>
	 * @return	the converted String
	 */
	String inputToCERs() {
		final CharReencoder encoder = new CharReencoder(inputToUnicode());
		return encoder.unicodeToCERs();
	} // inputToCERs()

	/** Convert character entity references in a string into a proper 
	 * Unicode string understood by Java (which is in UCS-2).
	 * @return	same contents encoded in UCS-2 (Java internal representation)
	 */
	String cersToUnicode() {
		if (s == null) return s;
		bld = new StringBuilder(s.length());
		final String SELF = "CharReencoder.cersToUnicode: ";
		final Pattern cerPattern = Pattern.compile("&#(\\d+);");
		final Matcher matcher = cerPattern.matcher(s);
		int prevEnd = 0;
		while (true) {
			if (!matcher.find()) break;
			final int start = matcher.start();
			final int end = matcher.end();
			bld.append(s.substring(prevEnd, start));
			final String refNumStr = matcher.group(1);
			final char unicodeChar = (char) Integer.parseInt(refNumStr);
			debugPrint(SELF + "refNumStr = ", refNumStr, ", unicodeChar = ",
					unicodeChar);
			bld.append(unicodeChar);
			prevEnd = end;
		}
		if (prevEnd < s.length()) bld.append(s.substring(prevEnd));
		final String unicode = bld.toString();
		debugPrint(SELF + "old string: ", s, "\nconverted to ", 
				unicode.length(), "-character string: ", unicode);
		return unicode;
	} // cersToUnicode()

	/** Converts a string encoded by Javascript's encodeURIComponent() into
	 * regular characters.
	 * @return	string with decoded special characters
	 */
	String urisToText() {
	String mod = s;
	if (s != null) try {
			mod = URLDecoder.decode(s.replace("+", "%2B"), UTF8)
					.replace("%2B", "+");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} // try
		return mod;
	} // urisToText()

	/** Returns a collator for a particular location based on a language,
	* modifying the default (incorrect) behavior to place spaces before other
	* characters; ref: http://jan.baresovi.cz/dr/en/java-collator-spaces
	* @param	language	the language
	* @return	a collator based on the language's location, modified to have
	* spaces sort before characters
	*/
	static RuleBasedCollator getCollator(String language) {
		final String code = TranslnRead.getCode(language);
		RuleBasedCollator collator = 
				(RuleBasedCollator) Collator.getInstance(
					Utils.isEmpty(code) ? Locale.US : new Locale(code));
		final String rules = collator.getRules();
		try {
			collator = new RuleBasedCollator(rules.replaceAll(
					"<'\u005f'", "<' '<'\u005f'"));
		} catch (ParseException e) {
			debugPrint("parseException in getCollator");
		}
		return collator;
	} // getCollator(String)

	/** Determines whether the language follows different alphabetization rules
	* from English.
	* @param	language	the language
	* @return	true if the language follows different alphabetization rules
	* from English
	*/
	static boolean realphabetize(String language) {
		return !Utils.isEmpty(TranslnRead.getCode(language));
	} // realphabetize(String)

	/** Capitalizes the first letter of the string, even if accented.
	* @return	string with first letter capitalized
	*/
	String capitalize() {
		if (s == null) return s;
		s = StringUtils.capitalize(cersToUnicode());
		return unicodeToCERs();
	} // capitalize()

	/** Converts character entity references, including accented characters, 
	* to characters appropriate for alphabetization.
	* Called by UserWrite to make sort keys of names for Oracle.
	* @return	modified String
	*/
	String cersToAlphabetical() {
		if (s == null) return s;
		bld = new StringBuilder();
		final String SELF = "CharReencoder.cersToAlphabetical: ";
		final Pattern cerPattern = Pattern.compile("&#(\\d+);");
		final Matcher matcher = cerPattern.matcher(s);
		int prevEnd = 0;
		while (true) {
			if (!matcher.find()) break;
			final int start = matcher.start();
			final int end = matcher.end();
			bld.append(s.substring(prevEnd, start));
			final int refNum = MathUtils.parseInt(matcher.group(1));
			final String accentless = removeCharAccent(refNum);
			bld.append(Utils.isEmpty(accentless) ? '*' : accentless); 
			prevEnd = end;
		} // while true
		if (prevEnd < s.length()) bld.append(s.substring(prevEnd));
		final String modStr = bld.toString();
		debugPrint(SELF + "old string: ", s, "\nconverted to ", 
				modStr.length(), "-character string: ", modStr);
		return modStr;
	} // cersToAlphabetical()

	/** Converts accented Unicode letters to accentless ones.  Called by 
	* SortIgnoreCase.
	* @return	modified String
	*/
	String unicodeToAccentless() {
		if (s == null) return s;
		final String SELF = "CharReencoder.unicodeToAccentless: ";
		bld = new StringBuilder();
		for (int posn = 0; posn < s.length(); posn++) {
			final char ch = s.charAt(posn);
			final int chNum = (int) ch;
			final String accentless = removeCharAccent(chNum);
			bld.append(Utils.isEmpty(accentless) ? ch : accentless); 
		} // for each character
		final String modStr = bld.toString();
		debugPrint(SELF + "old string: ", s, "\nconverted to ", 
				modStr.length(), "-character string: ", modStr);
		return modStr;
	} // unicodeToAccentless()

	/** Converts character entity references for accented letters
	* to accentless letters.
	* @param	num	the Unicode number of the CER
	* @return	the corresponding accentless character, or empty string if the
	* character is not an accented letter
	*/
	private String removeCharAccent(int num) {
		switch (num) {
			case 193:
			case 194:
			case 195:
			case 196:
			case 197: return "A";
			case 198: return "AE";
			case 199: return "C";
			case 200:
			case 201:
			case 202:
			case 203: return "E";
			case 204:
			case 205:
			case 206:
			case 207: return "I";
			case 208: return "D~";
			case 209: return "N";
			case 210:
			case 211:
			case 212:
			case 213:
			case 214: return "O";
			case 217:
			case 218:
			case 219:
			case 220: return "U";
			case 221:
			case 376: return "Y";
			case 222: return "T~";
			case 223: return "ss";
			case 224:
			case 225:
			case 226:
			case 227:
			case 228:
			case 229: return "a";
			case 230: return "ae";
			case 231: return "c";
			case 232:
			case 233:
			case 234:
			case 235: return "e";
			case 236:
			case 237:
			case 238:
			case 239: return "i";
			case 240: return "d~";
			case 241: return "n";
			case 242:
			case 243:
			case 244:
			case 245:
			case 246:
			case 248: return "o";
			case 249:
			case 250:
			case 251:
			case 252: return "u";
			case 253:
			case 255: return "y";
			case 254: return "t~";
			case 338: return "OE";
			case 339: return "oe";
			case 352: return "S";
			case 353: return "s";
			default: return "";
		} // switch
	} // removeCharAccent()

} // CharReencoder

	/** Determines whether a byte represents an ASCII character.
	* @param	bt	a byte
	* @return	true if the byte represents an ASCII character.
	private boolean isASCII(byte bt) {
		return (bt & BITS[7]) == 0;
	} // isASCII(byte)

	/** Determines whether a byte may be the 1st byte in a
	* 2-byte UTF-8 sequence representing a character.
	* Such a byte has the sequence 110xxxxx.
	* @param	bt	a byte
	* @return	true if the byte may be the 1st byte in a 2-byte
	* UTF-8 sequence representing a character
	private boolean fitsByte1Of2(byte bt) {
		return (bt & BITS[7]) != 0 && (bt & BITS[6]) != 0 
				&& (bt & BITS[5]) == 0;
	} // fitsByte1Of2(byte)

	/** Determines whether a byte may be the 1st byte in a
	* 3-byte UTF-8 sequence representing a character.
	* Such a byte has the sequence 1110xxxx.
	* @param	bt	a byte
	* @return	true if the byte may be the 1st byte in a 3-byte
	* UTF-8 sequence representing a character
	private boolean fitsByte1Of3(byte bt) {
		return (bt & BITS[7]) != 0 && (bt & BITS[6]) != 0 
				&& (bt & BITS[5]) != 0 && (bt & BITS[4]) == 0;
	} // fitsByte1Of3(byte)

	/** Determines whether a byte may be the 1st byte in a
	* 4-byte UTF-8 sequence representing a character.
	* Such a byte has the sequence 11110xxx.
	* @param	bt	a byte
	* @return	true if the byte may be the 1st byte in a 4-byte
	* UTF-8 sequence representing a character
	private boolean fitsByte1Of4(byte bt) {
		return (bt & BITS[7]) != 0 && (bt & BITS[6]) != 0 
				&& (bt & BITS[5]) != 0 && (bt & BITS[4]) != 0 
				&& (bt & BITS[3]) == 0;
	} // fitsByte1Of4(byte)

	/** Appends a character entity representation of a UTF-8
	* character to a growing StringBuilder.
	* @param	bts	array of 2-4 bytes representing the UTF-8 character,
	* starting with the leftmost (first) byte, with the highest bits of the
	* code point
	private void appendUTF8(byte... bts) {
		final int numBytes = bts.length;
		final int codePt = getCodePt(bts);
		debugPrint("CharReencoder.appendUTF8: converting ", numBytes, 
				"-byte UTF char ", bytesToBins(bts), " to &#", codePt, ";");
		bld.append("&#").append(codePt).append(';');
	} // appendUTF8(byte...)

	/** Gets a Unicode code point from 2-4 bytes representing a
	* character in the UTF-8 format.  The bytes will have one of the
	* following sequences:
	* <ul>
	* <li>1xxxxxxx</li>
	* <li>110xxxxx 10xxxxxx</li>
	* <li>1110xxxx 10xxxxxx 10xxxxxx</li>
	* <li>11110xxx 10xxxxxx 10xxxxxx 10xxxxxx</li>
	* </ul>
	* @param	bts	array of 2-4 bytes representing the UTF-8 character,
	* starting with the leftmost (first) byte, with the highest bits of the
	* code point
	* @return	the Unicode code point
	private int getCodePt(byte[] bts) {
		final int numBytes = bts.length;
		final int numSigBitsBytes2To4 = 6;
		int codePt = 0;
		if (numBytes == 1) {
			codePt = 128 + (int) getSmallBits(bts[0], 7);
		} else {
			// concatenate smallest 6 bits of all bytes but leftmost
			for (int btNum = 0; btNum < numBytes - 1; btNum++) {
				final int whichByte = numBytes - btNum;
				final byte sigBits = 
						getSmallBits(bts[whichByte - 1], numSigBitsBytes2To4);
				final int bitsShift = numSigBitsBytes2To4 * btNum;
				codePt |= (Utils.byteToInt(sigBits) << bitsShift);
			} // for each byte except leftmost in bts
			// concatenate appropriate number of bits of leftmost byte
			final int numSigBitsByte1 = 7 - numBytes;
			final byte sigBits = getSmallBits(bts[0], numSigBitsByte1);
			final int bitsShift = numSigBitsBytes2To4 * (numBytes - 1);
			codePt |= (Utils.byteToInt(sigBits) << bitsShift);
		} // if only one byte
		return codePt;
	} // getCodePt(byte[])

	/** Gets the smallest <i>n</i> bits of a byte.
	* @param	bt	the byte 
	* @param	numBits	the number of bits to get
	* @return	the smallest <i>n</i> bits of the byte 
	private byte getSmallBits(byte bt, int numBits) {
		byte smallBitMask = 0;
		final int howMany = (numBits > 8) ? 8 : numBits;
		for (int bitNum = 0; bitNum < howMany; bitNum++) {
			smallBitMask |= (1 << bitNum);
		} // for each bit to be obtained
		return (byte) (bt & smallBitMask);
	} // getSmallBits(byte, int)

	/** Converts an array of bytes to their binary representations.
	* @param	bts	an array of bytes
	* @return	an array of the bytes' binary representations
	private String[] bytesToBins(byte[] bts) {
		final int numBytes = bts.length;
		String[] bins = new String[numBytes];
		for (int bNum = 0; bNum < numBytes; bNum++) {
			bins[bNum] = byteToBin(bts[bNum]);
		} // for each byte
		return bins;
	} // bytesToBins(byte[])

	/** Converts a byte to its binary representation.
	* @param	bt	a byte
	* @return	the byte's binary representation
	private String byteToBin(byte bt) {
		return Utils.padBinary(Integer.toBinaryString(Utils.byteToInt(bt)), 8);
	} // byteToBin(byte)

	/** Converts a string inputted by a user from UTF-8 encoding
	* to character entity references.  
	* <P>A <a href="http://en.wikipedia.org/wiki/UTF-8">UTF-8</a>  
	* byte sequence must be one of the following: 
	* <ul>
	* <li>110xxxxx 10xxxxxx</li>
	* <li>1110xxxx 10xxxxxx 10xxxxxx</li>
	* <li>11110xxx 10xxxxxx 10xxxxxx 10xxxxxx</li>
	* </ul>
	* <p>We assume that the front end has collected any input in UTF-8 
	* format, not Latin-1 or MS-1252; forms acquiring such input should 
	* include the attribute,
	* <blockquote>accept-charset="UTF-8"</blockquote>
	* @return	the converted String
	String inputToCERs() {
		if (s == null) return s;
		final String SELF = "CharReencoder.inputToCERs: ";
		debugPrint(SELF + "converting ", s);
		String mod = s;
		try {
			final byte[] bytes = s.getBytes(LATIN1_SET);
			final int numBytes = bytes.length;
			bld = new StringBuilder(numBytes);
			int bNum = 0;
			while (bNum < numBytes) {
				final byte b1 = bytes[bNum];
				debugPrint(SELF + "byte ", bNum + 1, " is ", b1, " or ",
						byteToBin(b1));
				if (isASCII(b1)) {
					bld.append((char) b1);
					bNum++;
					continue;
				} // if cannot be first byte of UTF-8
				// could be 1-byte non-ASCII character
				if (bNum + 1 >= numBytes) {
					// last byte in sequence
					debugPrint(SELF + "ran out of bytes; this byte must be "
							+ "one-byte non-ASCII character.");
					appendUTF8(b1);
					bNum++;
					continue;
				} // if no more bytes for UTF-8
				// byte 1 is 110xxxxx, 1110xxxx, or 11110xxx 
				final byte b2 = bytes[bNum + 1];
				// byte 1 could be 1-byte non-ASCII character
				if (isASCII(b2)) {
					debugPrint(SELF + "next byte is ASCII, so byte ",
							bNum + 1, " is a one-byte non-ASCII character.");
					appendUTF8(b1);
					bNum++;
					continue;
				} // if b2 is ASCII
				debugPrint(SELF + "byte ", bNum + 2, " is ", b2, " or ",
						byteToBin(b2));
				// could be 1st byte of 2-, 3-, or 4-byte char
				if (fitsByte1Of2(b1)) {
					// byte 1 is 110xxxxx, byte 2 is 10xxxxxx
					appendUTF8(b1, b2);
					bNum += 2;
					continue;
				} // if byte 2 is conclusive
				if (bNum + 2 >= numBytes) {
					// second-to-last byte in sequence, no point looking further
					debugPrint(SELF + "next byte is non-ASCII, but there is no "
						+ "byte after that, so this byte must be "
						+ "one-byte non-ASCII character.");
					appendUTF8(b1);
					bNum++;
					continue;
				} // if no more bytes for UTF-8
				// byte 1 is 1110xxxx or 11110xxx, byte 2 is 10xxxxxx, 
				// could be part of 3- or 4-byte char
				final byte b3 = bytes[bNum + 2];
				// byte 1 could still be 1-byte non-ASCII character
				if (isASCII(b3)) {
					debugPrint(SELF + "next byte is non-ASCII, but next byte "
							+ "is ASCII, so byte ",
							bNum + 1, " is a one-byte non-ASCII character.");
					appendUTF8(b1);
					bNum++;
					continue;
				} // if b3 is ASCII
				debugPrint(SELF + "byte ", bNum + 3, " is ", b3, " or ",
						byteToBin(b3));
				if (fitsByte1Of3(b1)) { 
					// byte 1 is 1110xxxx, bytes 2 and 3 are 10xxxxxx
					appendUTF8(b1, b2, b3);
					bNum += 3;
					continue;
				} // if byte 3 is conclusive
				// byte 1 is 11110xxx, bytes 2 and 3 are 10xxxxxx, 
				// must be part of 4-byte char
				final byte b4 = bytes[bNum + 3];
				debugPrint(SELF + "byte ", bNum + 4, " is ", b4, " or ",
						byteToBin(b4));
				appendUTF8(b1, b2, b3, b4);
				bNum += 4;
			} // while there are bytes
			mod = bld.toString();
			if (mod.equals(s)) debugPrint(SELF, "string remains unchanged");
			else debugPrint(SELF, "converted to ", mod);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} // try
		return mod;
	} // inputToCERs()

	/** The Latin-1 character set that we use to convert Strings to bytes and
	 * vice versa. 
	public static final String LATIN1_SET = "ISO-8859-1";
	/** Masks for bits 0-7.
	public static final byte[] BITS = new byte[] {
			1 << 0, 1 << 1, 1 << 2, 1 << 3, 
			1 << 4, 1 << 5, 1 << 6, (byte) (1 << 7)}; 

	 */
