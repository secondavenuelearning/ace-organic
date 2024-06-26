package com.epoch.genericQTypes.genericQConstants;

import com.epoch.xmlparser.xmlConstants.XMLConstants;

/** Holds constants for the complete-the-table question type. */
public interface TableQConstants extends XMLConstants {

	// public static final is implied by interface

	/** 0-based index of the qDatum that holds the number of rows
	 * and their captions. */
	int ROW_DATA = 0;
	/** 0-based index of the qDatum that holds the number of columns
	 * and their captions. */
	int COL_DATA = 1;
	/** 0-based index of the qDatum that holds the data to prelaod into the
	 * response table. */
	int PRELOAD_DATA = 2;
	/** Default number of rows/columns if qDatum doesn't contain it.  */
	String[] DEFAULT_SIZE = new String[] {"3"};
	/** The minimum number of questionData in a complete-the-table question. */
	int MIN_QDATA = 2;
	/** The maximum number of questionData in a complete-the-table question. */
	int MAX_QDATA = 3;

	/** Character that separates captions in a complete-the-table qDatum. */
	String CAPTION_SEP = "|";
	/** For use in a regular expression, character that separates captions
	 * in a complete-the-table qDatum. */
	String CAPTION_SEP_REGEX = "\\|";

	/** Tag for XML format of table response. */
	String XML_TAG = "xml";
	/** Tag for XML format of table response. */
	String TABLE_TAG = "table";
	/** Tag for XML format of table response. */
	String NUM_ROWS_TAG = "numRows";
	/** Tag for XML format of table response. */
	String NUM_COLS_TAG = "numCols";
	/** Tag for XML format of table response. */
	String ROW_TAG = "tr";
	/** Tag for XML format of table response. */
	String CELL_TAG = "td";
	/** Tag for XML format of table response. */
	String DISABLED_TAG = "disabled";
	/** Tag for XML format of table response. */
	String DISABLED_VALUE = "on";

	/** Start of name of textbox for input of table values. */
	String CELL_ID_START = "cell";
	/** Start of name of checkbox for input of table values. */
	String CKBOX_ID_START = "ckbox";
	/** Character that separates row and column numbers in names of
	 * input elements. */
	char ROW_COL_SEP = '_';

	/** Parameter for convertToHTML().  */
	int DISPLAY = 0;
	/** Parameter for convertToHTML().  */
	int STUD_INPUT = 1;
	/** Parameter for convertToHTML().  */
	int AUTH_INPUT = 2;
	/** Parameter for convertToHTML().  */
	int AUTH_DISPLAY = 3;
	/** Style for HTML display when cells are disabled. */
	String DISABLED_STYLE = " background-color:#99FFFF;";

	/** Parameter for populateCell(). */
	boolean PART_OF_RESP = true;
	/** Parameter for populateCell(). */
	boolean COL_ENABLED = false;
	/** Parameter for responseToXML(). */
	boolean URIS_ENCODED = true;

	/** Color for background of input cells with incorrect values. */
	public final static String WRONG_BACKGROUND_COLOR = "#FFA0A0";

	/** Parameter for parseXML(). */
	int AT_TABLE = -1;
	/** Parameter for parseXML(). */
	int AT_ROW = 0;

} // TableQConstants
