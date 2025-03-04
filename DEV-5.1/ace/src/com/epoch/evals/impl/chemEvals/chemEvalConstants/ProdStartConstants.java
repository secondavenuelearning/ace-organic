package com.epoch.evals.impl.chemEvals.chemEvalConstants;

/** Contains constants used by MechProdStartIs and SynthStart. */
public interface ProdStartConstants {

	/** Value for combination.  */
	public static final int OVERLAP_NULL = 0; // S ^ A = null
	/** Value for combination.  */
	public static final int NOT_OVERLAP_NULL = 1; // S ^ A != null
	/** Value for combination.  */
	public static final int SUPERSET = 2; // S >= A
	/** Value for combination.  */
	public static final int NOT_SUPERSET = 3; // S !>= A
	/** Value for combination.  */
	public static final int SUBSET = 4; // S <= A
	/** Value for combination.  */
	public static final int NOT_SUBSET = 5; // S !<= A
	/** Value for combination.  */
	public static final int IDENTICAL = 6; // S = A
	/** Value for combination.  */
	public static final int NOT_IDENTICAL = 7; // S != A

	/** Value for prodOrStart or cpdsType.  */
	public static final int PRODUCT = 1;
	/** Value for prodOrStart or cpdsType.  */
	public static final int START = 2;
	/** Value for prodOrStart or cpdsType.  */
	public static final int INTERMED = 3;
	/** English versions of values for prodOrStart or cpdsType.  */
	public static final String[] PROD_START_ENGL = new String[]
			{"", "product", "starting material", "intermediate"};

	/** English versions of values for combination. */
	public static final String[] COMB_ENGL = new String[] 
			{"none",
			"any",
			"all",
			"not all",
			"are all among",
			"are not all among",
			"match one-to-one with",
			"do not match one-to-one with"};

} // ProdStartConstants
