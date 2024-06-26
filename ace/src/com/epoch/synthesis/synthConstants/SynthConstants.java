package com.epoch.synthesis.synthConstants;

import com.epoch.chem.chemConstants.MechSynthConstants;
import com.epoch.synthesis.Synthesis;
import com.epoch.translations.PhraseTransln;

/** Holds constants for synthesis questions. */
public interface SynthConstants extends MechSynthConstants {

	// public static final is implied by interface

/* **** The reaction definition consists of MRV reaction definitions separated by the
following dividers: */

	/** Reaction condition divider:
	 * products and unreacted substrates from the
	 * prior reaction are subjected to the next reaction. */
	String AND_DO = "***AND_DO***";
	/** Reaction condition divider:
	 * the subsequent reactions are done only if
	 * the prior ones produced no products. */
	String IF_NO_PRODS_DO = "***IF_NO_PRODS_DO***";
	/** Reaction condition divider:
	 * the starting materials for the previous
	 * reaction should be applied again to this reaction. */
	String RESUBJECT_TO = "***RESUBJECT_TO***";
	/** Reaction condition divider: * groups reactions. */
	String OPEN_PAREN_STR = "***(***";
	/** Reaction condition divider: * ends grouping of reactions. */
	String CLOSE_PAREN_STR = "***)***";
	/** Reaction condition divider:
	 * should come immediately after any of the preceding dividers and applies to 
	 * all subsequent reactions until turned off by the same sequence with no group 
	 * names listed.  Group numbers can be used in place of group names.
	 */
	String FNAL_GROUPS = "***FNAL_GROUPS{";
	/** Reaction condition divider:
	 * ends the list of functional groups required for this and subsequent
	 * reactions until redefined. */
	String FNAL_GROUPS_END = "}***";
	/** Length of a final String.   */
	int lenOPEN = OPEN_PAREN_STR.length();
	/** Length of a static final String.   */
	int lenCLOSE = CLOSE_PAREN_STR.length();
	/** Length of a static final String.   */
	int lenAND = AND_DO.length();
	/** Length of a static final String.   */
	int lenIF = IF_NO_PRODS_DO.length();
	/** Length of a static final String.   */
	int lenRESUBJ = RESUBJECT_TO.length();
	/** Length of a static final String.   */
	int lenFNAL = FNAL_GROUPS.length();
	/** Length of a static final String.   */
	int lenFNAL_END = FNAL_GROUPS_END.length();

/* **** Reaction definition (RxnMolecule) properties used by SynthSolver: */

	/** Value is integer; shows the number of substrates that must have been 
	 * submitted by the user; e.g., for Grignard addition to an ester, the 
	 * user cannot supply two different Grignard reagents if this property has 
	 * value 2.  */
	String NUM_REACTS = "Number of reactants";
	/** Value is true; the products of the reaction will not be 
	 * resubjected to the reaction.  */
	String STOP_AFTER_1 = "Stop after one reaction";
	/** Value is true; don't end trying different permutations after the first 
	 * successful combination.  Used in aldol reactions, where partners can 
	 * swap places.  */
	String KEEP_PERMUTING = "Keep permuting";
	/** Value is true; don't specify configurations in the reaction products.  */
	String NO_SPECIFY_CONFIGS = "Do not specify configurations";
	/** Value is name of reaction.  */
	String RXN_NAME = "NAME";
	/** Value is true; reaction is asymmetric.  */
	String ASYM = "Asymmetric";
	/** Value is true; allow compounds to react with themselves.  */
	String ALLOW_DIMER = "Allow dimerization";
	/** Value is name of functional group.  */
	String CLONE_FNAL = "Functional group of substrate to clone";
	/** How many copies of the substrate are required by the reaction.  */
	String CLONE_STOICH = "Stoichiometry of substrate to clone";
	/** Value is integer; set by SynthStage to show how many
	 * molecules were originally drawn in the stage.  */
	String ORIG_MOL_COUNT = "Original molecule count";

/* **** Reactor product (Molecule) properties used AND set by SynthSolver: */

	/** Value is "major" or "minor" (in practice, only "minor" will be used).  
	 * When USED, does this reaction definition give minor products only?
	 * When SET, is this compound produced by a reaction that gives minor products 
	 * only?  */
	String MAJ_MIN_PROD = "Major or minor product";
		/** Value of MAJ_MIN_PROD. */
		String MINOR = "minor";
	/** Value is any String; provides feedback to student as to why this reaction 
	 * definition gives minor products, e.g., "The Diels-Alder reaction provides 
	 * mostly the endo product." for the exo reaction definition.  */
	String MAJ_MIN_EXPLAN = "Major or minor explanation";

/* **** Reactor product (Molecule) properties set by SynthSolver: */

	/** Value is integer; shows how many times Reactor was called for a 
	 * particular substrate combination before this product was obtained. */
	String REACTOR_INDEX = "Reactor index";
	/** Value is true; denotes products of prior reactions that have reacted 
	 * further and should be removed from the list of reaction products.  */
	String REACTED = "Reacted";
	/** Value is integer; when an array of substrates is resized, gives the 
	 * position of the substrate in the original array.  */
	String RESIZE_NUM = "Resize number";

/* **** Other properties: */

	/** Value is m:n, where m and n are integers; if a
	 * student submits a compound with ambiguous tetrahedral stereochemistry,
	 * SynthParser generates all stereoisomers before storing them in the SynthStage.
	 * It gives each stereoisomer this property, where m is the number of molecules
	 * already in the stage (plus one) and n is the stereoisomer number.  Used to
	 * identify whether a student ran a reaction producing certain isomers but not
	 * others, and failed to show which isomers were produced.  */
	String EXPANDED_STEREOISOMER = "Expanded stereoisomer";
	/** Value is integer; the object index of the box
	 * of the stage whose reaction produces the compound.  Set by
	 * Synthesis.checkValidRxnProducts() when a product of the current stage is
	 * found to be a calculated product of a previous stage.  */
	String PRODUCER_BOX = "Producing stage box index";

	/** Value for stage index of an arrow or an atom,
	 * and error return value when search molecule is null.  */
	int NOT_FOUND = -1;
	/** Value for stage index of an arrow or an atom.  */
	int FOUND_IN_2 = -2;
	/** Parameter for SynthError().  */
	boolean VERIFICATION_ERROR = true;

	/** Value for errorNumber.  */
	int BAD_DEFINITION = -2;
	/** Value for errorNumber.  */
	int UNCHECKED = -1;
	/** Value for errorNumber.  */
	int RXNS_OK = 0;
	/** Value for errorNumber.  */
	int NO_PRODS_IN_NEXT_STAGE = 1;
	/** Value for errorNumber.  */
	int CONTAINS_IMPERMISSIBLE_SM = 2;
	/** Value for errorNumber.  */
	int START_STAGE_HAS_IMPERMISSIBLE_SM = 3;
	/** Value for errorNumber.  */
	int LAST_NOT_RXN_PRODUCT = 4;
	/** Value for errorNumber.  */
	int SEARCH_EXCEPTION = 5;
	/** Value for errorNumber.  */
	int NO_RXN_PRODUCTS = 6;
	/** Value for errorNumber.  */
	int RXN_PROD_IS_OK_SM = 7;
	/** Value for errorNumber.  */
	int WRONG_ENANTIOMER = 8;
	/** Value for errorNumber.  */
	int WRONG_DIASTEREOMER = 9;
	/** Value for errorNumber.  */
	int MINOR_PRODUCT = 10;
	/** Value for errorNumber.  */
	int UNSPECIFIED_STEREOISOMERS = 11;
	/** Value for errorNumber.  */
	int UNSELECTIVE = 12;
	/** Value for errorNumber.  */
	int UNDIASTEREOSELECTIVE_NOT_SHOWN = 13;
	/** Value for errorNumber.  */
	int UNENANTIOSELECTIVE_NOT_SHOWN = 14;
	/** Value for errorNumber.  */
	int USE_MENU = 15; 
	/** Value for errorNumber.  */
	int TOO_MANY_REACTANTS = 16; 
	/** Value for errorNumber.  */
	int IS_RXN = 17; 
	/** Value for errorNumber.  */
	int BAD_SM = 18;
	/** Value for errorNumber.  */
	int UNDIASTEREOSELECTIVE_SHOWN = 19;
	/** Value for errorNumber.  */
	int UNDIASTEREOSELECTIVE_NOT_DISTING = 20;
	/** Value for errorNumber.  */
	int UNENANTIOSELECTIVE_SHOWN = 21;
	/** Value for errorNumber.  */
	int UNENANTIOSELECTIVE_NOT_DISTING = 22;

	/** Delimiter for variable phrases (to be substituted with other
	 * phrases) inside of translations.  To be used as a regular
	 * expression, e.g. in split() and replaceAll(). */
	String STARS_REGEX = PhraseTransln.STARS_REGEX;

	/** Parameter for oneStepIs().  */
	boolean RECURSE = true;
	/** Parameter for isSelective().  */
	Synthesis NO_STEPOK = null;
	/** Parameter for Synthesis().  */
	boolean EMPTY_BOX_OK = true;

	/** Molecule property storing the reaction ID numbers for a synthesis. */
	public static String RXN_IDS = "reactionIds";
	/** Character separating reaction ID numbers. */
	public static String RXN_ID_SEP = ";";
	/** Refers to 1st member of synthesisComponents. */
	int STRUC = 0;
	/** Refers to 2nd member of synthesisComponents. */
	int RXNID = 1;
	/** String used to separate reaction names from other information when
	 * alphabetizing reaction conditions. */
	String DIVIDER = "\t";

	/** Member of phrases to be translated for getReactionsDisplay(). */
	int RXN_CONDS = 0;
	/** Member of phrases to be translated for getReactionsDisplay(). */
	int ID_NUMS = 1;
	/** Member of phrases to be translated for getReactionsDisplay(). */
	int NONE_CHOSEN = 2;
	/** Parameter of getReactionsDisplay(). */
	boolean SHOW_ID_NUMS = true;

	/** Member of impossible starting material or menu-only reagents String[].  */ 
	int SM_NAME = 0;
	/** Member of impossible starting material or menu-only reagents String[].  */ 
	int SM_DEF = 1;

	/** Error return value when search molecule is null.  */
	int SUBSET_NULL	= -2;
	/** Value for stereoTolerance.  */
	int EXACT_STEREO = 0;
	/** Value for stereoTolerance.  */
	int TOLERATE_ENANT = 1;
	/** Value for stereoTolerance.  */
	int TOLERATE_DIASTEREO_ONLY	=  2;
	/** Value for stereoTolerance.  */
	int TOLERATE_ANY_STEREO = 3;
	/** The molecule for which to search is from a student's response.  */
	boolean SRCHFRAG_RESP = true;
	/** The molecule for which to search is an author's structure.  */
	boolean SRCHFRAG_AUTH = false;

	/** Parameter for throwSynthError().  */
	String NO_CALC_PRODS = null;

	/** Used to acquire specific information from database.  */
	int NAMES = 1;
	/** Used to acquire specific information from database.  */
	int CLASSIFNS = 2;
	/** Used to acquire specific information from database.  */
	int CLASSIFNS_UNIQUE = 3;
	/** Parameter for RxnCondition.getClassificns() indicating 
	 * whether to get just one copy of each classification from the database. */
	boolean UNIQUE = true;
	/** Parameter for RxnCondition.alphabetize() indicating 
	 * whether the first ID in allowedRxnsStr is for the
	 * default, "no reagents", which should always come first. */
	boolean DEFAULT_NO_RGTS = true;

	/** ID number of default reaction with no reagents.  Hard-wired into the
	 * database.  */
	int NO_REAGENTS = 3;

} // SynthConstants
