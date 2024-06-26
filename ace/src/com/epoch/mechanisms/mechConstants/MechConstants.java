package com.epoch.mechanisms.mechConstants;
import chemaxon.struc.RxnMolecule;

/** Constants for mechanism classes. */
public interface MechConstants extends MechErrorConstants {

	// public static final is implied by interface

	/** Parameter for throwMechError().  */
	String NO_CALC_PRODS = null;
	/** Parameter for checkForPrevStageProds(). */
	boolean LAST_STAGE_IS_PREV = true;

	/** Value for MechParser.topology, numReactionArrows, numResonanceArrows, 
	 * firstCyclicPhysicalStageIndex, and firstCyclicStageIndex.  */
	int	INVALID = 0;
	/** Value for MechParser.topology, allMoleculesClassified,
	 * numReactionArrows, numResonanceArrows, firstCyclicPhysicalStageIndex,
	 * and firstCyclicStageIndex.  */
	int	VALID = 1;
	/** Value for MechParser.topology.  */
	int	LINEAR = 2;
	/** Value for MechParser.topology.  */
	int	CYCLIC = 3;

	/** Value for stage index of an arrow or an atom.  
	 Also, error return value when search molecule is null.
	 Also, return value of containsCatShiftNot1_2().  */
	int NOT_FOUND = -1;   
	/** Value for stage index of an arrow or an atom.  */
	final int FOUND_IN_2 = -2;
	/** Value for stage index of an arrow.  */
	final int FOUND_IN_REMOVED_STAGE = -3;
	/** Parameter for MechError().  */
	boolean VERIFICATION_ERROR = true;

	/** An electron-flow arrow's source or sink is unknown.  */
	int UNKNOWN		=  0;
	/** An electron-flow arrow's source or sink is an atom.  */
	int ATOM		=  1;
	/** An electron-flow arrow's source or sink is a bond.  */
	int BOND		=  2;
	/** An electron-flow arrow's source or sink is an incipient bond.  */
	int INCIP_BOND	=  3;

	/** Bit for return value for noCarbocations2().  */
	int NO_CARBOCAT = 0; 
	/** Bit for return value for noCarbocations2().  On when there is a 
	 * carbocation.  */
	int CARBOCAT = (1 << 0); 
	/** Bit for return value for noCarbocations2().  On when there is a resonance 
	 * structure in which C+ undergoes resonance with a negative atom to give a 
	 * structure in which both the C and the other atom are neutral.
	 * */
	int NEGATIVE_RESONANT = (1 << 1);
	/** Bit for return value for noCarbocations2().  On when there is a resonance 
	 * structure in which C+ undergoes resonance with a neutral atom that has a 
	 * lone pair.  */
	int LONE_PAIR_RESONANT = (1 << 2); 
	/** Bit for return value for noCarbocations2().  On when there is a negative 
	 * atom elsewhere in the compound.  */
	int NEG_ELSEWHERE = (1 << 3); 
	/** Bit for return value for noCarbocations2().  On when there is a resonance 
	 * structure in which C+ undergoes resonance with a N atom that has a 
	 * lone pair.  */
	int N_RESONANT = (1 << 4); 
	/** Mask to interpret error from noCarbocations2().  */
	int CARBOCAT_MASK = (1 << 1) | (1 << 0); 
	/** Member for results[] in noCarbocations2().  Stores the nature of the
	 * error.  */ 
	int ERROR = 0;
	/** Member for results[] in noCarbocations2().  Stores the stage index. */ 
	int STAGE = 1;

	/** Return value for noSupplyReceive().  */
	int NO_VIOLATION = 0;

	/** Number of data stored in isPericyclic() return value. */
	int NUM_PERI_DATA = 6;
	/** Array member that stores reaction type in isPericyclic() return value. */
	int RXN_TYPE = 0;
		/** Value of reaction type.  */
		int NOT_PERICYCLIC = 0;
		/** Value of reaction type.  */
		int SIGMATROPIC = 1;
		/** Value of reaction type.  */
		int CYCLOADDN = 2;
		/** Value of reaction type.  */
		int RETROCYCLOADDN = 3;
		/** Value of reaction type.  */
		int ELECTROCYCLIC_OPENING = 4;
		/** Value of reaction type.  */
		int ELECTROCYCLIC_CLOSING = 5;
		/** Value of reaction type.  */
		int ENE = 6;
		/** Value of reaction type.  */
		int RETROENE = 7;
		/** Value of reaction type.  */
		int GROUP_TRANSFER = 8;
		/** Value of reaction type.  */
		int THREE_COMPON_CYCLOADDN = 9;
		/** Value of reaction type.  */
		int THREE_COMPON_RETROCYCLOADDN = 10;
		/** Value of reaction type.  */
		int UNKNOWN_TYPE = 11;
	/** Array member that stores number of electrons in isPericyclic() return value. */
	int ELECS = 1;
	/** Array member that stores number of atoms in isPericyclic() return value. */
	int ATOMS = 2;
	/** Array member that stores number of atoms in one component of pericyclic reaction 
	 * in isPericyclic() return value. */
	int COMPON1 = 3;
	/** Array member that stores number of atoms in other component of pericyclic reaction 
	 * in isPericyclic() return value. */
	int COMPON2 = 4;
	/** Array member that stores stage index in isPericyclic() return value. */
	int STAGE_INDEX = 5;

	/** Error return value when search list is empty.  */
	int SUBSET_NULL = -2;   
	/** The molecule for which to search is from a student's response.  */
	boolean SRCHFRAG_RESP = true;
	/** The molecule for which to search is an author's structure.  */
	boolean SRCHFRAG_AUTH = false;
	/** Value for bit 0 of flags.  Whether to consider resonance structures 
	 * of the given starting materials as the same as the starting materials. */
	int RESON_LENIENT = (1 << 0);
	/** Value for bit 1 of flags.  Whether to consider stereoisomers as equivalent. */
	int STEREO_LENIENT = (1 << 1);
	/** Value for flags; all bits turned off. */
	int NOT_LENIENT = 0;
	
	/** Value for member of MechStage.moleculesClassified[].   */
	int RESPONSE_PRODUCT	  =  1;
	/** Value for member of MechStage.moleculesClassified[].   */
	int RESPONSE_STARTER	  =  2;
	/** Value for member of MechStage.moleculesClassified[].   */
	int RESPONSE_INTERMEDIATE =  3;
	/** Maximum number of arrows linking a stage to another. Improves code
	 * readability.  */
	int MAX_LINKING_ARROWS	  =  3;

	/** Mask for reading whether to consider charge in matches.  */
	int CHARGE_MASK		=  (1 << 0);
	/** Mask for reading whether to consider radical state in matches.  */
	int RADSTATE_MASK	=  (1 << 1);
	/** Mask for reading whether to consider isotope state in matches.  */
	int ISOTOPES_MASK	=  (1 << 2);
	/** Mask for reading whether to consider charge, radical, isotope state in 
	 * matches.  */
	int ALL_MASK = CHARGE_MASK | RADSTATE_MASK | ISOTOPES_MASK;

	/** Parameter for MechParser.getStageForPoint(). */
	boolean POINT_OF_ARROW = true;

	/** Type of RxnMolecule reaction arrow. */
	int RESONANCE = RxnMolecule.RESONANCE;

	/** Atom property. */
	String FROM_INITIATOR = "fromInitiator"; 
	/** Atom or bond property. */
	String ORIG_INDEX = "original index"; 

} // MechConstants

