package com.epoch.energyDiagrams.diagramConstants;

/** Constants regarding connections between cells of energy diagrams. */
public interface CellsLineConstants extends DiagramCellConstants {

	// public static final is implied by interface

	/** Separates the two endpoints of each line. */
	String ENDPTS_SEP = ":";

	/** Member of endPoints. */
	int LEFT_END = 0;
	/** Member of endPoints. */
	int RIGHT_END = 1;

	/** XML tag for line endpoint. */
	String ENDPT_TAG = "endpoint";

} // CellsLineConstants
