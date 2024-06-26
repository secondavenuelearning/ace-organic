package com.epoch.courseware.courseConstants;

import com.epoch.qBank.qBankConstants.FigConstants;

/** Constants related to the forums. */
public interface ForumConstants extends FigConstants {
	
	// public static final is implied by interface

	/** Prefix of filenames of post images. */
	String POST_FILENAME = "forumPost";
	/** Prefix of filenames of post images of molecules. */
	String POST_MOL_IMG = "forumMolImg";

	/** Bit 0 of flags. */
	int ANON = (1 << 0);

} // ForumConstants
