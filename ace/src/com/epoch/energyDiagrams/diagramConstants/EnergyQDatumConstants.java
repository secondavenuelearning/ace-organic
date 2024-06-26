package com.epoch.energyDiagrams.diagramConstants;

/** Constants for question data of energy diagrams. */
public interface EnergyQDatumConstants {

	// public static final is implied by interface

	/** Minimum number of question data required by this question type. */
	int MIN_QDATA = 1;
	/** Maximum number of question data required by this question type. */
	int MAX_QDATA = 1;
	/** Default vertical size of a diagram. */
	int NUM_ROWS_DEFAULT = 15;
	/** Separates the rows and columns in the question data of an RCD and the
	 * values for the Y-axis scale. */
	String QDATA_SEP = "\t";

} // EnergyQDatumConstants
