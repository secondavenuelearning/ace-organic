package com.epoch.energyDiagrams.diagramConstants;

/** Constants for energy diagrams. */
public interface EDiagramConstants extends EnergyQDatumConstants {

	// public static final is implied by interface

	/** Separates the descriptions of table cell contents from those of the
	 * lines connecting the cells. */
	String CELLS_LINES_SEP = "&";
		/** Member of array from splitting string describing cells and lines. */
		int CELLS = 0;
		/** Member of array from splitting string describing cells and lines. */
		int LINES = 1;
	/** Separates the descriptions of table cell contents. */
	String CELLS_SEP = "/";
	/** Separates the descriptions of lines connecting the table cells. */
	String LINES_SEP = "/";
	/** Separates row and column numbers and the cell contents. */
	String LOC_SEP = "=";
		/** Member of array from splitting string describing cell. */
		int ROW = 0;
		/** Member of array from splitting string describing cell. */
		int COL = 1;
		/** Member of array from splitting string describing cell. */
		int CONTENTS = 2;
	/** Words that appear as a user mouses over empty table cells. */
	String DROP_HERE = "Drop it here!";

	/** XML tag for diagram. */
	String DIAGRAM_TAG = "diagram";
	/** XML tag for diagram. */
	String IS_OED_TAG = "isOED";
	/** XML tag for cell. */
	String CELL_TAG = "cell";
	/** XML tag for line. */
	String LINE_TAG = "line";

	/** Parameter for parseXML(). */
	boolean IS_OED = true;

} // EDiagramConstants
