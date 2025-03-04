package com.epoch.db.dbConstants;

/** Fields of table IMAGES and local version. */
public class ImageConstants {

	/** Field in IMAGES.  &rarr; FIG_FIGID, POST_FIGURE, or TEXTCONTENT_DATA. */
	public static final String IMG_ID = "pic_id";
	/** Field in IMAGES holding the file name without any directories. */
	public static final String IMG_RELFILENAME = "file_name"; // CLOB
	/** Generates unique IDs for both master- and locally authored figures.  */
	public static final String FIGURES_SEQ = QuestionsRWConstants.FIGURES_SEQ;

} // ImageConstants
