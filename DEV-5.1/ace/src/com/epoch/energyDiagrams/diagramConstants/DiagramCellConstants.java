package com.epoch.energyDiagrams.diagramConstants;

/** Constants regarding cells of energy diagrams. */
public interface DiagramCellConstants extends EDiagramConstants {

	// public static final is implied by interface

	/** Member of return value of directionsTo(). */
	int HORIZONTAL = 0;
	/** Member of return value of directionsTo(). */
	int VERTICAL = 1;
	/** Row of one cell with respect to another. */
	int DOWN = -1;
	/** Row of one cell with respect to another. */
	int UP = 1;
	/** Column of one cell with respect to another. */
	int LEFT = -1;
	/** Column of one cell with respect to another. */
	int RIGHT = 1;
	/** Row or column of one cell with respect to another. */
	int SAME = 0;

	/** XML tag for row. */
	String ROW_TAG = "row";
	/** XML tag for column. */
	String COLUMN_TAG = "column";
	/** XML tag for label. */
	String LABEL_TAG = "label";

} // DiagramCellConstants
