package com.epoch.mechanisms.mechConstants;

import com.epoch.chem.chemConstants.MechSynthConstants;
import com.epoch.translations.PhraseTransln;

/** Constants for mechanism classes. */
public interface MechErrorConstants extends MechSynthConstants {

	// public static final is implied by interface

	/** Value for MechParser.topology, allMoleculesClassified, 
	 * numReactionArrows, numResonanceArrows, 
	 * firstCyclicPhysicalStageIndex, and firstCyclicStageIndex, 
	 * and MechError.errorNumber. */
	int	UNCHECKED = -1;
	/** Value for errorNumber.  */
	int ARROWS_OK = 0;
	/** Value for errorNumber.  */
	int ODD_ELECTRON_BOND = 1;
	/** Value for errorNumber.  */
	int TWO_E_ARROW_ATOM_TO_ATOM = 2;
	/** Value for errorNumber.  */
	int VALENCE_ERROR = 3;
	/** Value for errorNumber.  */
	int NO_FLOW_ARROWS = 4;
	/** Value for errorNumber.  */
	int FLOWS_GIVE_NO_PRODS = 5;
	/** Value for errorNumber.  */
	int NOT_PROD_NOR_STARTER = 6;
	/** Value for errorNumber.  */
	int RULE_VIOLATION = 7;
	/** Value for errorNumber.  */
	int BAD_PKA_OMITTED_COPRODUCTS = 8;
	/** Value for errorNumber.  */
	int TOO_MANY_OUTER_ELECTRONS = 9;
	/** Value for errorNumber.  */
	int NOT_STARTER = 10;
	/** Value for errorNumber.  */
	int MALFORMED_CHAIN = 11;
	/** Value for errorNumber.  */
	int NEGATIVE_BOND = 12;
	/** Value for errorNumber.  */
	int NOT_PROD_NOR_STARTER_1ST_CYCLIC = 13;
	/** Value for errorNumber.  */
	int NO_INITIATION = 14;
	/** Value for errorNumber.  */
	int INITIATOR_IN_PROPAGATION = 15;
	/** Value for errorNumber.  */
	int NEGATIVE_UNSHARED_ELECTRONS = 16;

	/** Delimiter for variable phrases (to be substituted with other
	 * phrases) inside of translations. */ 
	String STARS = PhraseTransln.STARS_SIMPLE;
	/** Delimiter for variable phrases (to be substituted with other
	 * phrases) inside of translations; regular expression form to be used, 
	 * e.g., in split() and replaceAll(). */
	String STARS_REGEX = PhraseTransln.STARS_REGEX;

	/** Name of a Javascript method. */
	String OPEN_OFFENDERS = "openOffendingCpds";

} // MechErrorConstants

