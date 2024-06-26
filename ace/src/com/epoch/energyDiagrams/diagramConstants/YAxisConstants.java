package com.epoch.energyDiagrams.diagramConstants;

/** Constants regarding the y-axis scale of an energy diagram. */
public interface YAxisConstants extends EnergyQDatumConstants {

	// public static final is implied by interface

	/** Number of data on the y-axis stored in a question datum. */
	int NUM_SCALE_DATA = 5;
	/** Position of the initial row number in the question datum that stores the 
	 * y-axis scale information. */
	int ROW_INIT = 0;
	/** Position of the row increment in the question datum that stores the 
	 * y-axis scale information. */
	int ROW_INCREMENT = 1;
	/** Position of the initial quantity in the question datum that stores the 
	 * y-axis scale information. */
	int QUANT_INIT = 2;
	/** Position of the quantity increment in the question datum that stores the 
	 * y-axis scale information. */
	int QUANT_INCREMENT = 3;
	/** Position of the unit in the question datum that stores the y-axis scale
	 * information. */
	int UNIT = 4;

} // YAxisConstants
