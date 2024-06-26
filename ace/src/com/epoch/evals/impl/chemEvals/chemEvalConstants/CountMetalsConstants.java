package com.epoch.evals.impl.chemEvals.chemEvalConstants;

import com.epoch.chem.chemConstants.ChemConstants;

/** Constants for CountMetals.java. */ 
public interface CountMetalsConstants extends ChemConstants { 

	/** Value for type of metals to count.  */
	public static final int ALL_METALS = 0;
	/** Value for type of metals to count.  */
	public static final int TRANSITION = 1;
	/** Value for type of metals to count.  */
	public static final int COLS_1_2 = 2;
	/** Value for type of metals to count.  */
	public static final int MAIN_GROUP = 3;
	/** Value for type of metals to count.  */
	public static final int NOT_TRANSITION = 4;
	/** Value for type of metals to count.  */
	public static final int METALS_METALLOIDS = 5;
	/** Database values for type of metals to count. */
	public static final String[] DB_METALS = 
			new String[] {"all", "dblock", "cols1+2", "main", "!dblock", "metal+oid"};
	/** English names of type of metals. */
	public static final String[] ENGL_METALS = 
			new String[] {"", " transition", " alkali or alkaline earth",
				" main group", " nontransition", " metalloids or "};

} // CountMetalsConstants
