package com.epoch.assgts.assgtConstants;

/** Holds constants used for assignments. */
public interface AssgtQGroupConstants {

	// public static final is implied by interface
	/** Within each group of questions, separates 
	 * number of random questions to pick from the question ID numbers. */ 
	String PICK_FROM = "@";
		/** When question group is split at <code>PICK_FROM</code>, member of array
		 * containing the number of questions to pick. */
		int PICK_NUM = 0;
		/** When question group is split at <code>PICK_FROM</code>, member of array
		 * containing the ID numbers of the Qs from which to choose. */
		int QIDS = 1;
	/** Within each group of questions, separates ID numbers of questions that
	 * may be chosen at random. */
	String RANDOM_SEP = ";";
	/** Within each group of random questions, separates ID numbers of questions
	 * that should be bundled together. */
	String BUNDLED_SEP = "/";

	/** Value for XML. */
	String Q_GROUP_TAG = "question_group";
	/** Value for XML. */
	String Q_TAG = "question";
	/** Value for XML. */
	String ID_TAG = "id";
	/** Value for XML. */
	String PICK_TAG = "pick";
	/** Value for XML. */
	String BUNDLE_SIZE_TAG = "bundle_size";
	/** Value for XML. */
	String PTS_TAG = "points";
	/** Value for XML. */
	String DEPENDS_TAG = "depends_on";
	
} // AssgtQGroupConstants
