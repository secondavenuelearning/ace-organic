package com.epoch.courseware.courseConstants;

/** Constants related to courses. */
public interface CourseConstants {
	
	// public static final is implied by interface

	/** Bit 2 of flags.  */
	int HIDE = (1 << 2);
	/** Bit 3 of flags.  */
	int EXAM_CRS = (1 << 3);
	/** Bit 4 of flags.  */
	int TAS_MAY_GRADE = (1 << 4);
	/** Bit shift of flags for how many decimal points to display in the
	 * gradebook. */
	int DECIMAL_SHIFT = 6;
	/** Mask for bits 6 and 7 of flags, which indicate how many decimal points
	 * to display in the gradebook. */
	int DECIMAL_MASK = (1 << DECIMAL_SHIFT) | (1 << (DECIMAL_SHIFT + 1));
	/** Bit 8 of flags.  */
	int FORUM_ON = (1 << 8);
	/** Bit 9 of flags.  */
	int HIDE_SYNTH_CALCD_PRODS = (1 << 9);
	/** Bit 10 of flags.  */
	int SORT_BY_STUDENT_NUM = (1 << 10);

	/** Default name of author. */
	String DEFAULT_AUTHOR = "Other";

} // CourseConstants
