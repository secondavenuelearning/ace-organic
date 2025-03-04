package com.epoch.translations;

import com.epoch.db.TranslnRead;
import com.epoch.db.TranslnWrite;
import com.epoch.exceptions.DBException;
import com.epoch.translations.translnConstants.TranslnConstants;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;

/** Holds the translations of hardwired ACE phrases into a single
 * language.  */
public class PhraseTransln implements TranslnConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Holds all English phrases hardwired into ACE. */
	transient public String[] allPhrases = null;
	/** Holds the phrases' translations during a translation session. */
	public String[] translations = null;
	/** ID of the person doing the translating. */
	private final String translatorId;

	/** Constructor.
	 * @param	translator	userID of the person doing the translating
	 */
	public PhraseTransln(String translator) {
		final String SELF = "PhraseTransln: ";
		translatorId = translator;
		try {
			allPhrases = TranslnRead.getAllPhrases(ENGLISH);
			if (!Utils.isEmpty(allPhrases)) {
				debugPrint(SELF, allPhrases.length,
						" phrases found, first = '", allPhrases[0], "'.");
			} else debugPrint(SELF + "no English phrases found.");
		} catch (DBException e) {
			Utils.alwaysPrint(SELF + "Caught DBException when "
					+ "trying to get translated phrases in English.");
		} // try
	} // PhraseTransln(String)

	/** Constructor.
	 * @param	language	language that translations should be in
	 * @param	translator	userID of the person doing the translating
	 */
	public PhraseTransln(String language, String translator) {
		final String SELF = "PhraseTransln: ";
		translatorId = translator;
		try {
			allPhrases = TranslnRead.getAllPhrases(ENGLISH);
			getTranslations(language);
			if (!Utils.isEmpty(allPhrases)) {
				debugPrint(SELF, allPhrases.length, " phrases in ", 
						language, " found, first = '", allPhrases[0], 
						"', translation is '", translations[0], "'.");
			} else debugPrint(SELF + "no phrases in ", language, " found.");
		} catch (DBException e) {
			Utils.alwaysPrint(SELF + "Caught DBException when "
					+ "trying to get translated phrases in English.");
		} // try
	} // PhraseTransln(String, String)

	/** Gets translations of all phrases for editing.
	 * @param	language	language that translations should be in
	 */
	private void getTranslations(String language) {
		final String SELF = "PhraseTransln.getTranslations: ";
		final int numPhrases = allPhrases.length;
		translations = new String[numPhrases];
		if (numPhrases != 0) try {
			debugPrint(SELF + "getting phrases in ", language, 
					"; phrases[0] = '", allPhrases[0], "'.");
			translations = TranslnRead.getAllPhrases(
					allPhrases, language);
		} catch (DBException e) {
			Utils.alwaysPrint(SELF + "Caught DBException when trying to "
					+ "get translated phrases in " + language + ".");
		} // try
	} // getTranslations(String)

	/** Sets translations of phrases.  Null values are omitted from
	 * consideration.
	 * @param	newTranslations	translations to be stored
	 * @param	language	language of translations
	 */
	public void setTranslations(String[] newTranslations, String language) {
		final String SELF = "PhraseTransln.setTranslations: ";
		// get new translations (not null); empty translations will be erased
		final ArrayList<String[]> translns = new ArrayList<String[]>();
		for (int phraseNum = 0; phraseNum < allPhrases.length; phraseNum++) {
			final String transln = newTranslations[phraseNum];
			if (transln != null) {
				debugPrint(SELF + "\"", allPhrases[phraseNum], "\" in ",
						language, " = \"", transln, "\"");
				translns.add(new String[] {allPhrases[phraseNum], transln});
			} // if the Q statement is new
		} // for each Q
		// store the new translations
		if (!translns.isEmpty()) try {
			final TranslnWrite writer = new TranslnWrite(translatorId);
			writer.setPhrases(translns, language);
		} catch (DBException e) {
			Utils.alwaysPrint(SELF + "Caught DBException when trying to "
					+ "set new phrases in ", language, ".");
		} // try
	} // setTranslations(String[], String)

	/** Gets translation of selected phrase, or returns the original (and stores
	 * it in the database) if the translation is not found.  
	 * @param	phrase	a phrase to translate
	 * @param	languages	languages that user has chosen in order of
	 * preference
	 * @return	translation of the phrase into the most preferred language for
	 * which there is a translation
	 */
	public static String translate(String phrase, String[] languages) {
		return translate(phrase, languages, ADD_NEW_PHRASES_TO_DB);
	} // translate(String, String[])

	/** Gets translation of selected phrase, or returns the original
	 * if the translation is not found.  
	 * @param	phrase	a phrase to translate
	 * @param	languages	languages that user has chosen in order of
	 * preference
	 * @param	addNewPhraseToDB	whether to add a new English phrase to the
	 * database
	 * @return	translation of the phrase into the most preferred language for
	 * which there is a translation
	 */
	public static String translate(String phrase, String[] languages,
			boolean addNewPhraseToDB) {
		if (Utils.isEmpty(languages)) return phrase;
		final String[] phrases = new String[] {phrase};
		translate(phrases, languages, addNewPhraseToDB);
		return phrases[0];
	} // translate(String, String[], boolean)

	/** Gets translations of selected phrases for display into the most
	 * preferred language for which there is a translation.  Modifies the
	 * original, leaving in English (and storing in the database) those 
	 * phrases without translations. 
	 * @param	phrases	phrases to translate
	 * @param	languages	languages that user has chosen in order of
	 * preference
	 */
	public static void translate(String[] phrases, String[] languages) {
		translate(phrases, languages, ADD_NEW_PHRASES_TO_DB);
	} // translate(String[], String[])

	/** Gets translations of selected phrases for display into the most
	 * preferred language for which there is a translation.  Modifies the
	 * original, leaving in English those phrases without translations. 
	 * @param	phrases	phrases to translate
	 * @param	languages	languages that user has chosen in order of
	 * preference
	 * @param	addNewPhrasesToDB	whether to add new English phrases to the
	 * database
	 */
	public static void translate(String[] phrases, String[] languages,
			boolean addNewPhrasesToDB) {
		if (!Utils.isEmpty(languages)) try {
			TranslnRead.translate(phrases, languages, addNewPhrasesToDB);
		} catch (DBException e) {
			System.out.println("PhraseTransln.getTranslations: Caught "
					+ "DBException when trying to translate phrases into "
					+ Arrays.toString(languages) + ".");
		} // try
	} // translate(String[], String[], boolean)

	/** Adds English phrases to the database if they are not already in it.
	 * @param	phrases	English phrases
	 */
	public static void addEnglish(String... phrases) {
		try {
			TranslnWrite.addEnglish(phrases);
		} catch (DBException e) {
			System.out.println("PhraseTransln.getTranslations: Caught "
					+ "DBException when trying to store English phrases.");
		} // try
	} // addEnglish(String[], String[])

	/** Deletes an English phrase and all its translations from the database.
	 * @param	phrase	an English phrase
	 */
	public static void deleteEnglish(String phrase) {
		try {
			TranslnWrite.deleteEnglish(phrase);
		} catch (DBException e) {
			System.out.println("PhraseTransln.getTranslations: Caught "
					+ "DBException when trying to delete an English phrase.");
		} // try
	} // deleteEnglish(String)

	/** Calculates an ID number from a String.
	 * @param	phrase	String to get an ID number from
	 * @return	an ID number
	 */
	public static int getPhraseId(String phrase) {
		return TranslnRead.getPhraseId(phrase).intValue();
	} // getPhraseId(String)

} // PhraseTransln
