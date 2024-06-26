package com.epoch.db.dbConstants;

import com.epoch.textbooks.textConstants.TextbookConstants;

/** Tables associated with ACE textbooks. */
public class TextbookRWConstants implements TextbookConstants {

	/** Table for textbooks. */
	public static final String TEXTBOOKS = "textbooks_v1";
		/** Field in TEXTBOOKS.  Unique identifier.  */
		public static final String TEXTBK_ID = "id";
		/** Field in TEXTBOOKS. */
		public static final String TEXTBK_AUTHOR = "user_id";
		/** Field in TEXTBOOKS. */
		public static final String TEXTBK_NAME = "name"; // CLOB
		/** Field in TEXTBOOKS. */
		public static final String TEXTBK_FLAGS = "flags";
		/** Field in TEXTBOOKS. */
		public static final String TEXTBK_LOCKHOLDER = "lock_holder";
	/** Generates unique IDs for both master- and locally authored textbooks. */
	public static final String TEXTBOOKS_SEQ = "textbooks_seq";
	/** Table for chapters in textbooks. */
	public static final String TEXTCHAPS = "text_chaps_v1";
		/** Field in TEXTCHAPS.  Unique identifier.  */
		public static final String TEXTCHAP_ID = "id";
		/** Field in TEXTCHAPS.  */
		public static final String TEXTCHAP_BOOKID = "text_id";
		/** Field in TEXTCHAPS.  */
		public static final String TEXTCHAP_NUM = "serial_num";
		/** Field in TEXTCHAPS. */
		public static final String TEXTCHAP_NAME = "name"; // CLOB
	/** Generates unique IDs for both master- and locally authored textbook
	 * chapters.  */
	public static final String TEXTCHAPS_SEQ = "text_chaps_seq";
	/** Table for content in textbooks. */
	public static final String TEXTCONTENT = "text_content_v1";
		/** Field in TEXTCONTENT.  Unique identifier.  */
		public static final String TEXTCONTENT_ID = "id";
		/** Field in TEXTCONTENT.  */
		public static final String TEXTCONTENT_CHAPID = "chap_id";
		/** Field in TEXTCONTENT.  */
		public static final String TEXTCONTENT_NUM = "serial_num";
		/** Field in TEXTCONTENT. */
		public static final String TEXTCONTENT_TYPE = "data_type";
		/** Field in TEXTCONTENT. */
		public static final String TEXTCONTENT_DATA = "data"; // CLOB
		/** Field in TEXTCONTENT. */
		public static final String TEXTCONTENT_CAPTION = "caption"; // CLOB
		/** Field in TEXTCONTENT. */
		public static final String TEXTCONTENT_EXTRA = "extra_data"; // CLOB
	/** Generates unique IDs for both master- and locally authored textbook
	 * content. */
	public static final String TEXTCONTENT_SEQ = "text_content_seq";
	/** Table for coauthors of coauthors. */
	public static final String COAUTHORS = "text_coauthors_v1";
		/** Field in COAUTHORS.  Unique identifier.  */
		public static final String COAUTH_BOOKID = "book_id";
		/** Field in COAUTHORS. */
		public static final String COAUTH_COAUTHOR = "user_id";

} // TextbookRWConstants
