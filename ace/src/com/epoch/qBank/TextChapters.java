package com.epoch.qBank;

import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Singleton class that stores the names of textbook chapters.
 */
public final class TextChapters {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Stores the names of each chapter; keyed by author name + edition
	 * number. */
	private static Map<String, ArrayList<String>> allTextChapters =
			new HashMap<String, ArrayList<String>>();
	
	/** Stores default edition numbers for each text; keyed by author name. */
	private static Map<String, String> defaultEditions =
			new HashMap<String, String>();
		
	/** Resets the table of chapters to 0 members before rereading from the
	 * text file. */
	public static void resetTextChapters() {
		allTextChapters.clear();
	} // resetTextChapters()

	/** Resets the table of chapters to 0 members before rereading from the
	 * text file. */
	public static void resetDefaultEditions() {
		defaultEditions.clear();
	} // resetDefaultEditions()

	/** Returns the chapter names in the order in which they were stored.
	 * @param	authorEdition	author name + edition number; if the edition
	 * number is missing, a default edition number is retrieved and appended.
	 * @return	array of chapter names in the order in which they were stored
	 */
	public static String[] getTextChapterNames(final String authorEdition) {
		final String SELF = "TextChapters.getTextChapterNames: ";
		String edition = authorEdition;
		debugPrint(SELF + "retrieving chapter names for ",
				edition);
		ArrayList<String> chapNamesList = allTextChapters.get(edition);
		if (chapNamesList == null) {
			final String defaultEdition = defaultEditions.get(edition);
			if (defaultEdition == null) {
				debugPrint(SELF 
						+ "couldn't get default edition for ", edition);
			} else {
				debugPrint(SELF + "for ", edition,
						", default edition is ", defaultEdition);
				edition += defaultEdition;
				chapNamesList = allTextChapters.get(edition);
			}
		}
		String[] anArray = new String[0];
		if (chapNamesList != null)
			anArray = chapNamesList.toArray(anArray);
		debugPrint(SELF + "for ", edition,
				", returning: ", Arrays.toString(anArray));
		return anArray;
	} // getTextChapterNames(String)

	/** Stores a chapter name in the appropriate list.
	 * @param	authorEdition	author name + edition number
	 * @param	chapName	the name of the chapter
	 * Called by servlets/GroupLoader.java each time the code is
	 * recompiled; it does not need to be called from anywhere else.
	 */
	public static void addChapName(String authorEdition, String chapName) {
		debugPrint("TextChapters.addChapName: adding to ",
				authorEdition, " chapter ", chapName);
		if (authorEdition != null && chapName != null) {
			ArrayList<String> chapNamesList =
					allTextChapters.get(authorEdition);
			if (chapNamesList == null) {
				chapNamesList = new ArrayList<String>();
			}
			chapNamesList.add(chapName);
			allTextChapters.put(authorEdition, chapNamesList);
		}
	} // addChapName(String)

	/** Stores a default edition for this textbook.
	 * @param	author	the author's name
	 * @param	edition	the edition number
	 * Called by servlets/GroupLoader.java each time the code is
	 * recompiled; it does not need to be called from anywhere else.
	 */
	public static void addDefaultEdition(String author, String edition) {
		if (author != null && edition != null) {
			debugPrint("TextChapters.addChapName: adding ",
					author, " with default edition ", edition);
			defaultEditions.put(author, edition);
		}
	} // addChapName(String)

	/** Disables external instantiation.  */
	private TextChapters() { }

} // TextChapters
