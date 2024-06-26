package com.epoch.evals.impl.chemEvals.chemEvalConstants;

import com.epoch.chem.chemConstants.ChemConstants;

/** Constants for Weight.java. */ 
public interface WtConstants extends ChemConstants { 

	/** Value for type of mass determination (exact or average).  */
	public static final int EXACT_MASS = 0;
	/** Value for type of mass determination (exact or average).  */
	public static final int AVERAGE_WT = 1;
	/** Database values for type of mass determination. */
	public static final String[] WT_TYPE = new String[] {"exact", "average"};
	
} // WtConstants
