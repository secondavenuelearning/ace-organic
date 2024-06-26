package com.epoch.energyDiagrams.diagramConstants;

/** Holds constants for RCDCell. */
public interface RCDCellConstants extends DiagramCellConstants {

	// public static final is implied by interface

	/** Value for state.  Indicates an unoccupied cell.  */
	int UNKNOWN = -1;
	/** Value for state. */
	int MINIMUM = 0;
	/** Value for state. */
	int MAXIMUM = 1;
	/** Value for state. */
	int INFLECTION = 2;
	/** Value for state. */
	int INCONSISTENT = 3;

	/** Names of states for display. */
	String[] NAMES = new String[] {
			"minimum", 
			"maximum",
			"inflection point",
			"inconsistent"
			};
	/** Range of states. */
	final int[] STATE_RANGE = new int[] {0, NAMES.length - 1};

	/** Separates the state and the label number.  Used to describe cell
	 * contents, but not in encoding of string describing diagram.  */
	String STATE_LABEL_SEP = ":";
		/** Member of array from splitting string describing cell. */
		int STATE = 0;
		/** Member of array from splitting string describing cell. */
		int LABEL = 1;

	/** XML for whether state is maximum or minimum. */
	String MAX_OR_MIN_TAG = "maxOrMin";

	/** Error message for when a state is higher than one connected state in an
	 * adjacent column and lower than another one in the same column. */
	String HEIGHT_ERROR = "If a state is connected to "
			+ "two other states in an adjacent column, it cannot be higher in "
			+ "energy than one and lower in energy than the other."; 

} // RCDCellConstants
