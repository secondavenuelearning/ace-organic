package com.epoch.lewis.lewisConstants;

import com.epoch.chem.chemConstants.ChemConstants;

/** Constants for LewisMolecule. */
public interface LewisConstants extends ChemConstants {

	// public static final is implied by interface

	/** Number of characters in a single cell in a MOL property line.  */
	int MOL_CELL_LEN = 3;
	/** Number of characters separating cells in a MOL property line.  */
	int CELL_SEP_LEN = 1;
	/** Array member for getOuterElectronsNumber() result.  */
	int ATOM_INDEX = 0;
	/** Array member for getOuterElectronsNumber() result.  */
	int OUTER_ELECS = 1;
	/** Name of molecule property declaring the molecule to be a Lewis
	 * structure. */
	String LEWIS_PROPERTY = "is Lewis Structure?";

	/** Width of the MarvinSketch canvas in internal MarvinSketch units.
	 * Determined empirically. */
	int MARVIN_WIDTH = 25;
	/** Width of the drawing canvas. */
	int CANVAS_WIDTH = 450;
	/** Height of the drawing canvas. */
	int CANVAS_HEIGHT = 260;
	/** Name of atom property containing the number of paired electrons.
	 * Obsolete, but kept for back-compatibility. */
	String UNSHARED_ELECS = "unshared electrons";
	/** Name of atom property containing the number of paired electrons. */
	String PAIRED_ELECS = "paired electrons";
	/** Name of atom property containing the number of unpaired electrons. */
	String UNPAIRED_ELECS = "unpaired electrons";
	/** Name of atom property indicating an atom should be highlighted. */
	String HIGHLIGHT = "highlighted";

} // LewisConstants
