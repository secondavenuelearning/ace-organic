package com.epoch.genericQTypes.genericQConstants;

import com.epoch.constants.FormatConstants;

/** Holds constants for the multiple-choice question type. */
public interface ChoiceConstants extends FormatConstants {

	// public static final is implied by interface

	/** Separates the options.  */
	String SEPARATOR = ":";
	/** Separates the options.  */
	String OLD_SEPARATOR = ";";
	/** Indicates whether an option was chosen.  */
	String CHOSEN = "+";
	/** Parameter for getChosenOptions().  */
	boolean SORT = true;
	/** Number of bits in a long. */
	int BITS_IN_LONG = 64;
	/** Start of a pulldown menu in the question statement of a
	 * fill-in-the-blank question. */
	String PD_START = "[[";
	/** Regular expression for the boundaries of a pulldown menu in the question 
	 * statement of a fill-in-the-blank question. */
	String PD_BOUNDS = "(\\[\\[|]])";
	/** Parameter for fillblankToDisplay(). */
	boolean ADD_MENUS = true;
	/** Name of HTML element. */
	String NUM_MENUS = "numMenus";
	/** Name of HTML element. */
	String MENU = "menu";
	/** Name of HTML element. */
	String MENU_ITEM = "menuItem";
	/** Name of HTML element. */
	String BLANK = "blank";

} // ChoiceConstants
