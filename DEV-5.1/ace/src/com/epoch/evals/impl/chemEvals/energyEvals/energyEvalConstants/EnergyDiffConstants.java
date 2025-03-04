package com.epoch.evals.impl.chemEvals.energyEvals.energyEvalConstants;

/** Contains constants common to OEDDiff and RCDDiff. */
public interface EnergyDiffConstants {

	/** Value for oper.  */
	public static final int ATLEAST = 1;
	/** Value for oper.  */
	public static final int EXACTLY = 2;

	/** Value for energies. */
	public static final int SIGNUMS = 0;
	/** Value for energies. */
	public static final int RELATIVE_HEIGHT = 1;
	/** Value for energies. */
	public static final int FIXED_HEIGHT = 2;
	/** Value for energies. */
	public static final int ANY_E = 3;
	/** Database values for energies. */
	public static final String[] HOW_DB = new String[] {
			"SIGNUMS", "REL_HTS", "FIXED", "ANY"};

} // EnergyDiffConstants
