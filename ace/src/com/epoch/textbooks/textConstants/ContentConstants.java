package com.epoch.textbooks.textConstants;

import com.epoch.constants.FormatConstants;
import com.epoch.qBank.qBankConstants.FigConstants;

public interface ContentConstants extends FormatConstants {

	// public static final is implied by interface

	/** Type of content: text. */
	int TEXT = 0;
	/** Type of content: MarvinView. */
	int MARVIN = 1;
	/** Type of content: an image. */
	int IMAGE = 2;
	/** Type of content: an ACE question. */
	int ACE_Q = 3;
	/** Type of content: a Jmol figure. */
	int JMOL = 4;
	/** Type of content: a movie. */
	int MOVIE = 5;
	/** Type of content: an image. */
	int IMAGE_URL = 6;
	/** Type of content: a Lewis structure. */
	int LEWIS = 7;
	/** Parameter for TextContent.setContent(). */
	static final boolean CHANGED = true;
	/** Database values for content types. */
	static final String[] DB_VALUES = new String[] 
			{"TXT", "MRV", "IMG", "ACE", "JML", "MOV", "PIC", "LEW"};

	/** Separates Jmol data, scripts, Javascript commands. */
	String JMOL_SEP = FigConstants.JMOL_SEP;
	/** Position of Jmol scripts in value from getJmolScripts(). */
	int JMOL_SCRIPTS = 0;
	/** Position of Jmol Javascript commands in value from getJmolScripts(). */
	int JMOL_JS_CMDS = 1;

	/** Start of opening tag of hyperlink. */
	public static String LINK_OPEN1 = "[link=\"";
	/** End of opening tag of hyperlink. */
	public static String LINK_OPEN2 = "\"]";
	/** Closing tag of hyperlink. */
	public static String LINK_CLOSE = "[/link]";
	/** Regular expression version of closing tag of hyperlink. */
	public static String LINK_CLOSE_REGEX = "\\[/link]";
	/** Length of start of opening tag of hyperlink. */
	public static int LINK_OPEN1_LEN = LINK_OPEN1.length();
	/** Length of end of opening tag of hyperlink. */
	public static int LINK_OPEN2_LEN = LINK_OPEN2.length();

	/** Beginning of name of file containing an image. */
	public static String CONTENT_FILENAME = "textContent_";

} // ContentConstants
