package com.epoch.energyDiagrams.diagramConstants;

/** Constants for an atomic or molecular orbital. */
public interface OrbitalConstants {

	// public static final is implied by interface

	/** Value for type.  Represents an unknown type of orbital. */
	int UNKNOWN = 0;
	/** Value for type.  Represents an <i>s</i> orbital. */
	int S = 1;
	/** Value for type.  Represents a <i>p</i> orbital. */
	int P = 2;
	/** Value for type.  Represents an <i>sp</i> orbital. */
	int SP = 3;
	/** Value for type.  Represents an <i>sp</i><sup>2</sup> orbital. */
	int SP2 = 4;
	/** Value for type.  Represents an <i>sp</i><sup>3</sup> orbital. */
	int SP3 = 5;
	/** Value for type.  Represents a <i>&sigma;</i> orbital. */
	int SIGMA = 6;
	/** Value for type.  Represents a <i>&pi;</i> orbital. */
	int PI = 7;
	/** Value for type.  Represents a <i>&sigma;*</i> orbital. */
	int SIGMA_STAR = 8;
	/** Value for type.  Represents a <i>&pi;*</i> orbital. */
	int PI_STAR = 9;

	/** Value for type.  Represents any orbital. */
	int ANY = 0;
	/** Value for type.  Represents an atomic orbital. */
	int ATOMIC = -1;
	/** Value for type.  Represents a hybrid orbital. */
	int HYBRID = -2;
	/** Value for type.  Represents a molecular orbital. */
	int MOLECULAR = -3;
	/** Value for type.  Represents a bonding molecular orbital. */
	int BONDING = -4;
	/** Value for type.  Represents an antibonding molecular orbital. */
	int ANTIBONDING = -5;

	/** Names of individual orbitals for display. */
	String[] INDIV_NAMES = new String[] {
			"unknown", 
			"s", 
			"p",
			"sp",
			"sp2", 
			"sp3", 
			"#sigma",
			"#pi", 
			"#sigma*",
			"#pi*" 
			};
	/** Names of groups of orbitals for display. */
	String[] GRP_NAMES = new String[] { 
			"any type of", 
			"atomic", 
			"hybrid", 
			"molecular", 
			"bonding", 
			"antibonding"
			};

	/** Range for type of individual orbital. */
	public final int[] TYPE_RANGE = new int[] {1, INDIV_NAMES.length - 1};

	/** Range of acceptable occupancies. */
	public final int[] OCCUP_RANGE = new int[] {0, 2};

} // OrbitalConstants
