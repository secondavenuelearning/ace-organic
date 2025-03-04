package com.epoch.assgts.assgtConstants;

import java.util.Date;

/** Holds constants used for assignments. */
public interface AssgtConstants {

	// public static final is implied by interface

	/** Mask for bit 0 of flags.  */
	int IS_VISIBLE = (1 << 0);
	/** Mask for bit 1 of flags.  */
	int RECORD_AFTER_DUE = (1 << 1);
	/** Mask for bit 2 of flags.  */
	int EXAM_ASSGT = (1 << 2);
	/** Mask for bit 3 of flags.  */
	int SAVE_PREV_TRIES = (1 << 3);
	/** Mask for bit 4 of flags.  */
	int DELAY_GRADING = (1 << 4);
	/** Mask for bit 5 of flags.  */
	int SHOW_REFS_BEFORE_ANSWERED = (1 << 5);
	/** Mask for bit 6 of flags.  */
	int SHOW_REFS_AFTER_ANSWERED = (1 << 6);
	/** Mask for bit 7 of flags.  */
	int EXCLUDE_FROM_GRADE_AVERAGE = (1 << 7);
	/** Mask for bit 8 of flags.  */
	int MASTERY_ASSGT = (1 << 8);
	/** Mask for bit 9 of flags.  */
	int TIMED = (1 << 9);
	/** Mask for bit 10 of flags.  */
	int LOG_ALL_TO_DISK = (1 << 10);
	/** Mask for bit 11 of flags.  */
	int SAVE_WO_SUBMITTING = (1 << 11);

	/** Value for 1st dimension of gradingParams. */
	int ATTEMPT = 0;
	/** Value for 1st dimension of gradingParams. */
	int TIME = 1;
	/** Value for 2nd dimension of gradingParams. */
	int LIMITS = 0;
	/** Value for 2nd dimension of gradingParams. */
	int FACTORS = 1;
	/** DB values for different types of grading parameters. */
	char[] DB_PARAM_TYPES = new char[] {'A', 'T'};
	/** Third kind of grading parameter, not stored in gradingParams. Needed for
	 * communication with front end and in calculating modified grades. */
	int PTS_PER_Q = 2;

	/** One week.  */	
	long WEEK = 1000 * 60 * 60 * 24 * 7;
	/** Value for getExtension().  */
	double INDEFINITE = -1.0;
	/** No maximum self-granted extension.  */
	String NO_MAX_EXTENSION = "-1";
	/** No limit on tries.  */
	int UNLIMITED = -1;
	/** Value for getDueDate().   */
	Date NO_DATE = null;
	/** Parameter for Assgt(). */
	boolean PRESERVE_ID = true;
	/** Parameter for gradingParamsToDisplay(). */
	public final static boolean PAST_TENSE = true;
	/** Parameter for pointsPerQToDisplay(). */
	public final static int EVERY_Q = 0;
	/** Parameter for getFixedIds(). */
	public final static boolean BY_POSN = true;
	/** Parameter for addGroups(). */
	public final static boolean CHECK_FOR_DUPLICATES = true;

	/** Separates usernames and extensions in the extensionees string. */
	String EXTENSION_SEP = "/";
	/** In assignment description, separates groups of questions.  */
	String GROUP_SEP = ":";
	/** In dependencies of one question's visibility on another's having been
	 * answered correctly, separates pairs of 1-based question numbers. */
	String DEP_PAIRS_SEP = ";";
	/** In each dependency of one question's visibility on another's having been
	 * answered correctly, separates the two 1-based question numbers. */
	String DEP_NUMS_SEP = ":";
		/** When dependencies are expressed as pairs of numbers, the dependent
		 * question. */
		int DEP_Q = 0;
		/** When dependencies are expressed as pairs of numbers, the independent
		 * question. */
		int INDEP_Q = 1;
	/** Separates reaction condition IDs in a string. */
	String RXN_CONDN_ID_SEP = ":";
	/** DB value for last limit in attempt- or time-dependent grading parameters. */
	String NO_MAXIMUM = "9999999999";

	/** Member of changed representing the assignment name, remarks, and
	 * tries. */
	int BASICS = 0;
	/** Member of changed representing the questions. */
	int QUESTIONS = 1;
	/** Member of changed representing the question dependencies. */
	int DEPENDENCIES = 2;
	/** Member of changed representing the allowed reaction conditions. */
	int ALLOWED_RXN_CONDNS = 3;
	/** Member of changed representing the assignment flags. */
	int FLAGS = 4;
	/** Member of changed representing the assignment due date. */
	int DUE_DATE = 5;
	/** Member of changed representing the extensions. */
	int EXTS = 6;
	/** Member of changed representing the attempt- and time-dependent grading 
	 * parameters. */
	int GRADE_PARAMS = 7;
	/** Member of changed representing the maximum extension students may
	 * self-assign. */
	int MAX_EXT = 8;
	/** Member of changed representing the points per question. */
	int PTS_PER_Q_PARAMS = 9;
	/** Member of changed representing the assignment duration. */
	int DURATION = 10;

	/** Value for XML. */
	String HWSET_DESCR_TAG = "assignment";
	/** Value for XML. */
	String NAME_TAG = "name";
	/** Value for XML. */
	String REMARKS_TAG = "remarks";
	/** Value for XML. */
	String FLAGS_TAG = "flags";
	/** Value for XML. */
	String ALLOW_UNLIMITED_TAG = "allow_unlimited";
	/** Value for XML. */
	String MAX_TRIES_TAG = "maxtries";
	/** Value for XML. */
	String INSTRUCTORID_TAG = "instructor";
	/** Value for XML. */
	String ALLOWED_RXNCONDNS_TAG = "permissibleRxnConds";
	/** Value for XML. */
	String ATTEMPT_GRADING_TAG = "attempt_grading";
	/** Value for XML. */
	String TIME_GRADING_TAG = "time_grading";
	/** Value for XML. */
	String Q_GRPS_TAG = "question_groups";
	
} // AssgtConstants
