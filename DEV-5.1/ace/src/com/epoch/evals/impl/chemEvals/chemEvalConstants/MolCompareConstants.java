package com.epoch.evals.impl.chemEvals.chemEvalConstants;

/** Contains constants used by both <code>Is</code> and 
 * <code>NumMols</code>. */
public interface MolCompareConstants {

	/** Value for flags.  */
	public static final int EITHER_ENANTIOMER = (1 << 0);
	/** Value for flags.  */
	public static final int NO_NORMALIZATION = (1 << 1);
	/** Value for flags.  */
	public static final int RESONANCE_PERMISSIVE = (1 << 2);
	/** Value for flags.  */
	public static final int SIGMA_NETWORK = (1 << 3);
	/** Value for flags.  */
	public static final int ISOTOPE_LENIENT = (1 << 4);

} // MolCompareConstants

