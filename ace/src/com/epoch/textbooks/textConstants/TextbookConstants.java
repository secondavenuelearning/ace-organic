package com.epoch.textbooks.textConstants;

import com.epoch.constants.AuthorConstants;

public interface TextbookConstants extends AuthorConstants {

	// public static final implied by interface

	/** Bit 0 of flags: visible to all. */
	int VISIBLE = 1 << 0;
	/** Parameter for TextChapter.deleteContents(). */
	boolean MOVING = true;

} // TextbookConstants
