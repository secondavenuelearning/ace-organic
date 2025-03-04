package com.epoch.db.dbConstants;

import com.epoch.constants.AppConstants;

/** The TRANSLATIONS and LANGUAGE_CODES tables and their fields. */
public class TranslationsConstants implements AppConstants {

	/** Table for storing translations of question statements, etc. into
	 * languages other than English. */
	public static final String TRANSLATIONS = "translations_v2";
		/** Field in TRANSLATIONS. */
		public static final String PHRASE_ID = "phrase_id";
		/** Field in TRANSLATIONS. */
		public static final String PHRASE_LANG = "language";
		/** Field in TRANSLATIONS. */
		public static final String PHRASE_TRANSLN = "translation"; // CLOB
		/** Field in TRANSLATIONS. */
		public static final String PHRASE_TRANSLATOR = "translator";
	/** Sequencer for translations.  */
	public static final String PHRASE_SEQ = "translations_seq";

	/** Table converting users' languages to two-letter codes. This table is
	 * modified only by direct database queries. */
	public static final String LANG_CODES = "language_codes_v1";
		/** Field in LANGUAGES.  */
		public static final String LANG_CODE_NAME = "language";
		/** Field in LANGUAGES.   */
		public static final String LANG_CODE_CODE = "code";

} // TranslationsConstants
