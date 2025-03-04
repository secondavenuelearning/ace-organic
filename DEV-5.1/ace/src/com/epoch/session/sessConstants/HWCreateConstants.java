package com.epoch.session.sessConstants;

/** Constants for HWCreateSession.  */
public interface HWCreateConstants {

	// public static final is implied by interface

	/** Value returned by addQuestion() or addRandomQuestion() to indicate
	 * success of assignment action. */
	int OK = 0;
	/** Value returned by addQuestion() to indicate failure of assignment 
	 * action. */
	int QID_0 = -1;
	/** Value returned by addQuestion() to indicate failure of assignment 
	 * action. */
	int RANDOMQsPRESENT = -2;
	/** Value returned by addQuestion() or addRandomQuestion() to indicate
	 * failure of assignment action. */
	int ALREADY = -3;
	/** Value returned by addQuestion() to indicate failure of assignment 
	 * action. */
	int CANNOTFINDQ = -4;
	/** Parameter for save(). */
	boolean EDITOR_PAGE1 = true;

} // HWCreateConstants
