package com.epoch.genericQTypes.genericQConstants;

/** Holds constants for the rank question type. */
public interface RankConstants {

	// public static final is implied by interface

	/** Separates each pair of item and rank.  */
	String MAJOR_SEP = ";";
	/** Separates item and rank within each pair.  */
	String MINOR_SEP = ":";
	/** Return value if rank or item not found for item or rank. */
	int NOT_FOUND = -1;

} // RankConstants
