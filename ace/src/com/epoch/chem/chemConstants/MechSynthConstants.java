package com.epoch.chem.chemConstants;

import chemaxon.struc.graphics.MPolyline;

/** Holds constants common to mechanisms and syntheses. */
public interface MechSynthConstants extends ChemConstants {

	// public static final is implied by interface

	/** Name of a Javascript method. */
	String OPEN_CALCD_PRODS = "openCalcProds";

	/** Value for getPoint(). */
	int TAIL = MPolyline.TAIL;
	/** Value for getPoint(). */
	int HEAD = MPolyline.HEAD;
	/** Value for getPoint(). */
	int MIDPT = Math.max(HEAD, TAIL) + 1;

} // MechSynthConstants
