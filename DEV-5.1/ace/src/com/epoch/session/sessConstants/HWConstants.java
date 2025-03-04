package com.epoch.session.sessConstants;


/** Constants for HWSession.  */
public interface HWConstants {

	// public static final is implied by interface

	/** Value for mode in homework/answerframe.jsp. */
	int SOLVE = 0;
	/** Value for mode in homework/answerframe.jsp. */
	int VIEW = 1;
	/** Value for mode in homework/answerframe.jsp. */
	int PRACTICE = 2;
	/** Value for mode in homework/answerframe.jsp. */
	int PREVIEW = 3;
	/** Value for mode in homework/answerframe.jsp.
	 * Used for R-group questions only. */
	int SIMILAR = 4;
	/** Value for mode in homework/answerframe.jsp. */
	int GRADEBOOK_VIEW = 5;
	/** Value for mode in homework/answerframe.jsp. */
	int TEXTBOOK = 6;

	/** Parameter for initializeStudentView().  */
	boolean STORE_RESULT = true;
	/** Parameter for init(). */
	boolean LAST_ONLY = true;
	/** Parameter for submitResponse().  */
	boolean EVALUATE = true;

	/** Feedback when human grading is required. */
	String BEFORE_HUMAN_1 = "Your instructor will "
			+ "evaluate your response.";
	/** Further feedback for responses requiring human grading. */
	String BEFORE_HUMAN_2 =
			"  ACE cannot provide further feedback at this time.";
	/** Feedback after human grading has occurred. */
	String AFTER_HUMAN_1 = "Your instructor has "
			+ "graded your response as shown above.";
	/** Further feedback for human-graded responses. */
	String AFTER_HUMAN_2 =
			"  See your instructor for further feedback.";

} // HWConstants
