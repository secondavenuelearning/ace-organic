package com.epoch.energyDiagrams.diagramConstants;

/** Constants used by OEDCell. */
public interface OEDCellConstants extends DiagramCellConstants {

	// public static final is implied by interface

	/** Separates the parts of the description of the orbitals in the cell. */
	String CELL_CONTENTS_SEP = ";";
		/** Member of cell contents. */
		int ORBS_TYPE = 0;
		/** Member of cell contents. */
		int OCCUPS = 1;
		/** Member of cell contents. */
		int LABEL = 2;
	/** Separates the occupancies of the orbitals. */
	String OCCUP_SEP = ":";

	/** Parameter for toString(). */
	boolean SORT = true;

	/** XML tag for orbitalType. */
	String ORBS_TYPE_TAG = "orbitalType";
	/** XML tag for occupancies. */
	String OCCUPS_TAG = "occupancies";
	/** XML tag for occupancy. */
	String OCCUP_TAG = "occupancy";

} // OEDCellConstants
