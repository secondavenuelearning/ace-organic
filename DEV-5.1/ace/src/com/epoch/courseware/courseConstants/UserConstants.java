package com.epoch.courseware.courseConstants;

import com.epoch.constants.AppConstants;

/** Constants related to users. */
public interface UserConstants extends AppConstants {
	
	// public static final is implied by interface

	/** Value for role. */
	char ADMINISTRATOR = 'A';
	/** Value for role. */
	char INSTRUCTOR = 'I';
	/** Value for role. */
	char STUDENT = 'S';
	/** Value for role. This value is not stored in the database; it is used in
	 * gradebook operations only.  */
	char TA = 'T';

	/** Bit 0 of flags. */
	int ENABLED = (1 << 0);
	/** Bit 1 of flags. */
	int IS_MASTER_AUTHOR = (1 << 1);
	/** Bit 2 of flags. */
	int IS_TRANSLATOR = (1 << 2);
	/** Bit 3 of flags. */
	int MAYNT_CHANGE_PWD = (1 << 3);
	/** Bit 4 of flags. */
	int DONT_SHOW_CALCD_SYNTH_PRODS = (1 << 4);
	/** Bit 5 of flags. */
	int PREFERS_JAVA = (1 << 5);
	/** Bit 6 of flags. */
	int PREFERS_PNG = (1 << 6);
	/** Bit 7 of flags. */
	int FAMILY_NAME_1ST = (1 << 7);
	/** Bit 8 of flags. */
	int DAY_MON_YR = (1 << 8);

	/** Last name of an exam student. */
	String RANDOM_SURNAME = "ZZZRandomStudent";

	/** Security questions for password reset. */
	String[] SECURITY_QS = {
			"What is your mother's maiden name?",
			"What is the name of your favorite pet?",
			"What is the name of your elementary school?",
			"What is the name of your favorite teacher?",
			"What is the name of your favorite band or musician?",
			"What is the name of your favorite author?",
			"What is the name of your favorite vacation spot?",
			"Where did you experience your first kiss?"
			};
	 boolean BY_USER_ID = true;

} // UserConstants
