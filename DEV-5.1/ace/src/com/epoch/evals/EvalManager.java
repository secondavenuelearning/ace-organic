package com.epoch.evals;

import com.epoch.evals.evalConstants.EvalImplConstants;
import com.epoch.exceptions.ParameterException;
import com.epoch.qBank.Question;
import com.epoch.utils.Utils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Constructor;

/** Manages and interfaces with the various classes that actually evaluate
 * responses. */
final public class EvalManager implements EvalImplConstants {
	
	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Gets the generic description of an evaluator for the pulldown menu in
	 * loadEvaluatorX.jsp.
	 * @param	evalConstant	an evaluator constant
	 * @param	qFlags	flags of the question being edited
	 * @return	the generic description of the evaluator
	 */
	public static String getDescription(int evalConstant, long qFlags) {
		switch (evalConstant) {
			// chemistry evaluators
			case IS_OR_HAS_SIGMA_NETWORK:
				return "If the structure (or its enantiomer, "
						+ "resonance form, &sigma;-network)";
			case IS_2D_CHAIR:
				return "If the 2D chair drawing represents";
			case SKELETON_SUBSTRUCTURE:
				return "If the substructure or skeleton of the response";
			case BOND_ANGLE:
				return "If the bond angle is";
			case CHIRAL:
				return "If the chirality of the response is";
			case FUNCTIONAL_GROUP:
				return "If the number of instances of the functional group";
			case HAS_FORMULA:
				return "If the response formula";
			case FORMULA_FORMAT:
				return "If the format of the formula";
			case UNSATURATION:
				return "If the unsaturation index of the formula";
			case NUM_ATOMS:
				return "If the number of atoms of an element";
			case NUM_MOLECULES:
				return "If the number of molecules";
			case NUM_RINGS:
				return "If the number of rings";
			case COUNT_METALS:
				return "If the number of compounds with metal atoms";
			case TOTAL_CHARGE:
				return "If the total charge";
			case WEIGHT:
				return "If the molecular weight or exact mass";
			case FORMULA_WEIGHT:
				return "If the molecular weight or exact mass";
			case CONFORMATION_CHAIR:
				return "If the number of axial/equatorial groups "
						+ (Question.is3D(qFlags) ? "" : "of the 2D chair ");
			case CONFORMATION_ACYCLIC:
				return "If two groups are anti/gauche/eclipsed";
			case MAPPED_ATOMS:
				return "If the selected atoms or mapping of the response";
			case MAPPED_COUNT:
				return "If the number of selected atoms matching the response";
			case LEWIS_ELECTRON_DEFICIENT:
				return "If the number of electron-deficient atoms of an element";
			case LEWIS_FORMAL_CHGS:
				return "If the formal charge of an atom";
			case LEWIS_ISOMORPHIC:
				return "If the Lewis structure";
			case LEWIS_OUTER_SHELL_COUNT:
				return "If the number of outer-shell electrons of";
			case LEWIS_VALENCE_ELECS:
				return "If the total number of valence electrons shown";
			case MECH_EQUALS:
				return "If the mechanism is exactly";
			case MECH_FLOWS:
				return "If the electron-flow arrows of the mechanism";
			case MECH_INIT:
				return "If the initiation part of the radical mechanism";
			case MECH_PIECES_COUNT:
				return "If the number of mechanism components";
			case MECH_PRODS_STARTERS_IS:
				return "If the starting materials or products of the mechanism are";
			case MECH_PRODS_STARTERS_PROPS:
				return "If individual compounds of the mechanism have the property";
			case MECH_RULE:
				return "If the mechanistic rule";
			case MECH_SUBSTRUCTURE:
				return "If the substructure &amp; electron-flow arrows "
						+ "of the mechanism";
			case MECH_TOPOLOGY:
				return "If the topology of the mechanism";
			case SYNTH_EQUALS:
				return "If the synthesis is exactly";
			case SYNTH_ONE_RXN:
				return "If one of the synthetic reactions is";
			case SYNTH_SCHEME:
				return "If the synthetic scheme";
			case SYNTH_TARGET:
				return "If the synthetic target";
			case SYNTH_SELEC:
				return "If the selectivity of the synthesis";
			case SYNTH_SM_MADE:
				return "If permissible starting materials are synthesized";
			case SYNTH_STARTERS:
				return "If the synthetic starting materials";
			case SYNTH_STEPS:
				return "If the number of synthetic steps";
			case OED_DIFF:
				return "If the orbital energy diagram is";
			case OED_ELEC:
				return "If the electron count of the orbitals is";
			case OED_TYPE:
				return "If the types of orbitals are";
			case RCD_DIFF:
				return "If the reaction coordinate diagram is";
			case RCD_STATE_CT:
				return "If the number of maxima and minima is";
			// generic evaluators
			case CHOICE_NUM_CHECKED:
				return "If the number of multiple-choice options chosen";
			case CHOICE_WHICH_CHECKED:
				return "If the multiple-choice options selected";
			case CLICK_HERE:
				return "If the mark is placed";
			case CLICK_NUM:
				return "If the mark's number";
			case CLICK_TEXT:
				return "If the mark's text";
			case CLICK_CT:
				return "If the number of marks";
			case CLICK_LABELS_COMP:
				return "If any marks' labels are identical";
			case HUMAN_REQD:
				return "If human grading is required";
			case NUM_IS:
				return "If the number is";
			case NUM_SIGFIG:
				return "If the number of significant figures is";
			case NUM_UNIT:
				return "If the number's unit is";
			case RANK_ORDER:
				return "If the sequence of the options";
			case RANK_POSITION:
				return "If the sequence number of an option";
			case STMTS_CT:
				return "If the number of statements in the response is";
			case TEXT_CONT:
				return "If the text contains";
			case TEXT_WORDS:
				return "If the number of words in the text is";
			case TEXT_SEMANTICS:
				return "If the text has the same semantics as";
			case TABLE_CT_NUM:
				return "If the number of table cells with a numerical value is";
			case TABLE_CT_TXT:
				return "If the number of table cells with a text value is";
			case TABLE_DIFF:
				return "If the response table is";
			case TABLE_NUM:
				return "If the numerical values of cells in the table are";
			case TABLE_TEXT:
				return "If the text values of cells in the table are";
			case TBL_NUM_NUM:
				return "If, where column x is n, column y has numerical value";
			case TBL_NUM_TXT:
				return "If, where column x is n, column y has text value";
			case TBL_TXT_NUM:
				return "If, where column x is A, column y has numerical value";
			case TBL_TXT_TXT:
				return "If, where column x is A, column y has text value";
			// physics evaluators
			case VECTORS_CT:
				return "If the number of vectors is";
			case VECTORS_COMP:
				return "If the vectors or sum of the vectors";
			case VECTORS_AXES:
				return "If the number of vectors along an axis";
			case EQNS_CT:
				return "If the number of equations is";
			case EQN_IS:
				return "If one of the equations is";
			case EQN_SOLVED:
				return "If the last equation is solved";
			case EQNS_FOLLOW:
				return "If the equations follow logically";
			case EQN_VARS:
				return "If the number of variables in the equation is";
			default:
				return "Unknown evaluator";
		} // switch
	} // getDescription(int, long)

	/** Gets the name of the evaluator class.
	 * @param	evalConstant	the evaluator constant
	 * @return	the name of the evaluator class
	 */
	private static String getClass(int evalConstant) {
		final String PATH = "com.epoch.evals.impl.";
		final String CHEM_EVALS = PATH + "chemEvals.";
		final String ENERGY_EVALS = CHEM_EVALS + "energyEvals.";
		final String LEWIS_EVALS = CHEM_EVALS + "lewisEvals.";
		final String MECH_EVALS = CHEM_EVALS + "mechEvals.";
		final String SYNTH_EVALS = CHEM_EVALS + "synthEvals.";
		final String GENERIC_Q_EVALS = PATH + "genericQEvals.";
		final String CLICK_EVALS = GENERIC_Q_EVALS + "clickEvals.";
		final String MULT_EVALS = GENERIC_Q_EVALS + "multEvals.";
		final String NUMERIC_EVALS = GENERIC_Q_EVALS + "numericEvals.";
		final String RANK_EVALS = GENERIC_Q_EVALS + "rankEvals.";
		final String TABLE_EVALS = GENERIC_Q_EVALS + "tableEvals.";
		final String TEXT_EVALS = GENERIC_Q_EVALS + "textEvals.";
		final String PHYSICS_EVALS = PATH + "physicsEvals.";
		final String VECTORS_EVALS = PHYSICS_EVALS + "vectorsEvals.";
		final String EQNS_EVALS = PHYSICS_EVALS + "eqnsEvals.";
		switch (evalConstant) {
			// chemistry evaluators
			case IS_OR_HAS_SIGMA_NETWORK: 	return CHEM_EVALS + "Is";
			case IS_2D_CHAIR: 				return CHEM_EVALS + "Is2DChair";
			case SKELETON_SUBSTRUCTURE: 	return CHEM_EVALS + "Contains";
			case FUNCTIONAL_GROUP: 			return CHEM_EVALS + "FnalGroup";
			case BOND_ANGLE: 				return CHEM_EVALS + "BondAngle";
			case CHIRAL: 					return CHEM_EVALS + "Chiral";
			case HAS_FORMULA: 				return CHEM_EVALS + "HasFormula";
			case FORMULA_FORMAT: 			return CHEM_EVALS + "FormulaFormat";
			case UNSATURATION: 				return CHEM_EVALS + "UnsaturIndex";
			case NUM_ATOMS: 				return CHEM_EVALS + "Atoms";
			case NUM_MOLECULES: 			return CHEM_EVALS + "NumMols";
			case NUM_RINGS: 				return CHEM_EVALS + "Rings";
			case TOTAL_CHARGE: 				return CHEM_EVALS + "Charge";
			case COUNT_METALS: 				return CHEM_EVALS + "CountMetals";
			case WEIGHT: 					return CHEM_EVALS + "Weight";
			case FORMULA_WEIGHT: 			return CHEM_EVALS + "FormulaWeight";
			case CONFORMATION_ACYCLIC: 		return CHEM_EVALS + "ConformBond";
			case CONFORMATION_CHAIR: 		return CHEM_EVALS + "ConformChair";
			case MAPPED_ATOMS: 				return CHEM_EVALS + "MapProperty";
			case MAPPED_COUNT: 				return CHEM_EVALS + "MapSelectionsCounter";
			case LEWIS_ELECTRON_DEFICIENT: 	return LEWIS_EVALS + "LewisElecDeficCt";
			case LEWIS_FORMAL_CHGS: 		return LEWIS_EVALS + "LewisFormalCharge";
			case LEWIS_ISOMORPHIC: 			return LEWIS_EVALS + "LewisIsomorph";
			case LEWIS_OUTER_SHELL_COUNT: 	return LEWIS_EVALS + "LewisOuterNumber";
			case LEWIS_VALENCE_ELECS: 		return LEWIS_EVALS + "LewisValenceTotal";
			case MECH_EQUALS: 				return MECH_EVALS + "MechEquals";
			case MECH_FLOWS: 				return MECH_EVALS + "MechFlowsValid";
			case MECH_INIT: 				return MECH_EVALS + "MechInitiation";
			case MECH_PIECES_COUNT: 		return MECH_EVALS + "MechCounter";
			case MECH_PRODS_STARTERS_IS: 	return MECH_EVALS + "MechProdStartIs";
			case MECH_PRODS_STARTERS_PROPS:	return MECH_EVALS + "MechProdStartProps";
			case MECH_RULE: 				return MECH_EVALS + "MechRule";
			case MECH_SUBSTRUCTURE: 		return MECH_EVALS + "MechSubstructure";
			case MECH_TOPOLOGY: 			return MECH_EVALS + "MechShape";
			case SYNTH_EQUALS: 				return SYNTH_EVALS + "SynthEquals";
			case SYNTH_ONE_RXN: 			return SYNTH_EVALS + "SynthOneRxn";
			case SYNTH_SCHEME: 				return SYNTH_EVALS + "SynthScheme";
			case SYNTH_SELEC: 				return SYNTH_EVALS + "SynthSelective";
			case SYNTH_SM_MADE: 			return SYNTH_EVALS + "SynthEfficiency";
			case SYNTH_STARTERS: 			return SYNTH_EVALS + "SynthStart";
			case SYNTH_STEPS: 				return SYNTH_EVALS + "SynthSteps";
			case SYNTH_TARGET: 				return SYNTH_EVALS + "SynthTarget";
			case OED_DIFF: 					return ENERGY_EVALS + "OEDDiff";
			case OED_ELEC: 					return ENERGY_EVALS + "OEDElecCt";
			case OED_TYPE: 					return ENERGY_EVALS + "OEDOrbType";
			case RCD_DIFF: 					return ENERGY_EVALS + "RCDDiff";
			case RCD_STATE_CT: 				return ENERGY_EVALS + "RCDStateCt";
			// generic evaluators
			case CHOICE_NUM_CHECKED: 		return MULT_EVALS + "MultipleNumChosen";
			case CHOICE_WHICH_CHECKED: 		return MULT_EVALS + "MultipleCheck";
			case CLICK_HERE: 				return CLICK_EVALS + "ClickHere";
			case CLICK_NUM: 				return CLICK_EVALS + "ClickNumber";
			case CLICK_TEXT: 				return CLICK_EVALS + "ClickText";
			case CLICK_CT: 					return CLICK_EVALS + "ClickCount";
			case CLICK_LABELS_COMP:			return CLICK_EVALS + "ClickLabelsCompare";
			case HUMAN_REQD: 				return GENERIC_Q_EVALS + "HumanReqd";
			case NUM_IS: 					return NUMERIC_EVALS + "NumberIs";
			case NUM_SIGFIG: 				return NUMERIC_EVALS + "NumberSigFigs";
			case NUM_UNIT: 					return NUMERIC_EVALS + "NumberUnit";
			case RANK_ORDER: 				return RANK_EVALS + "RankOrder";
			case RANK_POSITION: 			return RANK_EVALS + "RankPosition";
			case TABLE_CT_NUM: 				return TABLE_EVALS + "TableCellNumCt";
			case TABLE_CT_TXT: 				return TABLE_EVALS + "TableCellTextCt";
			case TABLE_DIFF: 				return TABLE_EVALS + "TableDiff";
			case TABLE_NUM: 				return TABLE_EVALS + "TableNumVal";
			case TABLE_TEXT: 				return TABLE_EVALS + "TableTextVal";
			case TBL_NUM_NUM: 				return TABLE_EVALS + "TableNumNum";
			case TBL_NUM_TXT: 				return TABLE_EVALS + "TableNumText";
			case TBL_TXT_NUM: 				return TABLE_EVALS + "TableTextNum";
			case TBL_TXT_TXT: 				return TABLE_EVALS + "TableTextText";
			case STMTS_CT: 					return TEXT_EVALS + "LogicStmtsCt";
			case TEXT_CONT: 				return TEXT_EVALS + "TextContains";
			case TEXT_SEMANTICS: 			return TEXT_EVALS + "TextSemantics";
			case TEXT_WORDS: 				return TEXT_EVALS + "TextWordCount";
			// physics evaluators
			case VECTORS_CT: 				return VECTORS_EVALS + "VectorsCt";
			case VECTORS_COMP: 				return VECTORS_EVALS + "VectorsCompare";
			case VECTORS_AXES: 				return VECTORS_EVALS + "VectorsAxes";
			case EQNS_CT: 					return EQNS_EVALS + "EqnsCt";
			case EQN_IS: 					return EQNS_EVALS + "EqnIs";
			case EQN_SOLVED: 				return EQNS_EVALS + "EqnSolved";
			case EQNS_FOLLOW: 				return EQNS_EVALS + "EqnsFollow";
			case EQN_VARS: 					return EQNS_EVALS + "EqnVariables";
			default: 						return null;
		} // switch evalConstant
	} // getClass(int)

	/** Gets the internal constant corresponding to an evaluator's match code.
	 * @param	matchCode	an evaluator's match code
	 * @return	internal constant corresponding to the match code
	 */
	public static int getEvalType(String matchCode) {
		return Utils.indexOf(EVAL_CODES, matchCode);
	} // getEvalType(String)

	/** Gets an array of evaluator constants for the question type.  Called by
	 * loadEvaluatorXXX.jsp.
	 * @param	qType	type of the question being edited
	 * @param	qFlags	flags of the question being edited
	 * @return	array of evaluator constants appropriate to the type of
	 * question.
	 */
	public static int[] getAllowedEvaluators(int qType, long qFlags) {
		final boolean usesSubstns = Question.usesSubstns(qFlags);
		final boolean is3D = Question.is3D(qFlags) && !usesSubstns;
		final boolean isMapping = Question.showMapping(qFlags)
				&& !usesSubstns && !is3D;
		final int[] allowedEvalConstants =
				(Question.isText(qType) ? new int[] {
					TEXT_CONT, 
					TEXT_SEMANTICS, 
					TEXT_WORDS,
					HUMAN_REQD}
				: Question.isLogicalStatements(qType) ? new int[] {
					STMTS_CT,
					TEXT_CONT, 
					TEXT_SEMANTICS, 
					TEXT_WORDS,
					HUMAN_REQD}
				: Question.isChoice(qType) ? new int[] {
					CHOICE_WHICH_CHECKED, 
					CHOICE_NUM_CHECKED,
					HUMAN_REQD}
				: Question.isChooseExplain(qType) ? new int[] {
					CHOICE_WHICH_CHECKED, 
					CHOICE_NUM_CHECKED,
					TEXT_CONT, 
					TEXT_SEMANTICS, 
					TEXT_WORDS,
					HUMAN_REQD}
				: Question.isFillBlank(qType) ? new int[] {
					CHOICE_WHICH_CHECKED,
					HUMAN_REQD}
				: Question.isRank(qType) ? new int[] {
					RANK_ORDER, 
					RANK_POSITION,
					HUMAN_REQD}
				: Question.isNumeric(qType) ? new int[] {
					NUM_IS, 
					NUM_SIGFIG, 
					NUM_UNIT,
					HUMAN_REQD}
				: Question.isTable(qType) ? new int[] {
					TABLE_DIFF, 
					TABLE_TEXT, 
					TABLE_CT_TXT,
					TBL_TXT_TXT,
					TBL_NUM_TXT,
					TABLE_NUM,
					TABLE_CT_NUM,
					TBL_TXT_NUM,
					TBL_NUM_NUM,
					HUMAN_REQD}
				: Question.isClickableImage(qType) ? new int[] {
					CLICK_HERE,
					CLICK_TEXT,
					CLICK_NUM,
					CLICK_CT,
					CLICK_LABELS_COMP,
					HUMAN_REQD}
				: Question.isDrawVectors(qType) ? new int[] {
					VECTORS_CT,
					VECTORS_AXES,
					VECTORS_COMP,
					HUMAN_REQD}
				: Question.isEquations(qType) ? new int[] {
					EQN_SOLVED,
					EQN_IS,
					EQNS_FOLLOW,
					EQNS_CT,
					EQN_VARS,
					HUMAN_REQD}
				: Question.isFormula(qType) ? new int[] {
					HAS_FORMULA,
					NUM_ATOMS,
					FORMULA_WEIGHT,
					UNSATURATION,
					FORMULA_FORMAT,
					HUMAN_REQD}
				: Question.isLewis(qType) ? new int[] {
					LEWIS_ISOMORPHIC, 
					LEWIS_VALENCE_ELECS, 
					LEWIS_FORMAL_CHGS, 
					TOTAL_CHARGE, 
					LEWIS_OUTER_SHELL_COUNT, 
					LEWIS_ELECTRON_DEFICIENT, 
					IS_OR_HAS_SIGMA_NETWORK, 
					SKELETON_SUBSTRUCTURE, 
					FUNCTIONAL_GROUP, 
					NUM_ATOMS, 
					HAS_FORMULA,
					BOND_ANGLE,
					HUMAN_REQD}
				: Question.isMechanism(qType) && usesSubstns ? new int[] {
					MECH_EQUALS, 
					MECH_RULE, 
					MECH_PRODS_STARTERS_IS, 
					MECH_PRODS_STARTERS_PROPS,
					MECH_FLOWS, 
					MECH_SUBSTRUCTURE, 
					MECH_TOPOLOGY, 
					MECH_PIECES_COUNT, 
					MECH_INIT, 
					IS_OR_HAS_SIGMA_NETWORK, 
					SKELETON_SUBSTRUCTURE, 
					FUNCTIONAL_GROUP,
					NUM_MOLECULES, 
					NUM_ATOMS, 
					TOTAL_CHARGE, 
					HUMAN_REQD}
				: Question.isMechanism(qType) ? new int[] {
					MECH_EQUALS, 
					MECH_RULE, 
					MECH_PRODS_STARTERS_IS, 
					MECH_PRODS_STARTERS_PROPS,
					MECH_FLOWS, 
					MECH_SUBSTRUCTURE, 
					MECH_TOPOLOGY, 
					MECH_PIECES_COUNT, 
					MECH_INIT, 
					IS_OR_HAS_SIGMA_NETWORK, 
					SKELETON_SUBSTRUCTURE, 
					FUNCTIONAL_GROUP,
					NUM_MOLECULES, 
					NUM_ATOMS, 
					TOTAL_CHARGE, 
					HAS_FORMULA,
					HUMAN_REQD}
				: Question.isSynthesis(qType) ? new int[] {
					SYNTH_EQUALS, 
					SYNTH_TARGET, 
					SYNTH_SCHEME, 
					SYNTH_SELEC, 
					SYNTH_ONE_RXN, 
					SYNTH_SM_MADE, 
					SYNTH_STARTERS, 
					SYNTH_STEPS, 
					IS_OR_HAS_SIGMA_NETWORK, 
					SKELETON_SUBSTRUCTURE, 
					FUNCTIONAL_GROUP,
					NUM_MOLECULES, 
					NUM_ATOMS,
					TOTAL_CHARGE, 
					HAS_FORMULA,
					HUMAN_REQD}
				: Question.isOED(qType) ? new int[] {
					OED_DIFF, 
					OED_ELEC, 
					OED_TYPE,
					HUMAN_REQD}
				: Question.isRCD(qType) ? new int[] {
					RCD_DIFF,
					RCD_STATE_CT,
					HUMAN_REQD}
				: !Question.isMarvin(qType) ? new int[] {
					HUMAN_REQD}
				: is3D ? new int[] {
					IS_OR_HAS_SIGMA_NETWORK, 
					SKELETON_SUBSTRUCTURE, 
					FUNCTIONAL_GROUP, 
					NUM_ATOMS, 
					HAS_FORMULA, 
					WEIGHT, 
					NUM_RINGS, 
					TOTAL_CHARGE, 
					NUM_MOLECULES, 
					CONFORMATION_CHAIR, 
					CONFORMATION_ACYCLIC,
					BOND_ANGLE,
					CHIRAL,
					MAPPED_ATOMS,
					MAPPED_COUNT,
					HUMAN_REQD} 
				: isMapping ? new int[] {
					IS_OR_HAS_SIGMA_NETWORK, 
					SKELETON_SUBSTRUCTURE, 
					FUNCTIONAL_GROUP, 
					NUM_ATOMS, 
					HAS_FORMULA, 
					WEIGHT, 
					NUM_RINGS, 
					TOTAL_CHARGE, 
					NUM_MOLECULES,
					MAPPED_ATOMS,
					MAPPED_COUNT,
					HUMAN_REQD}
				: new int[] {
					IS_OR_HAS_SIGMA_NETWORK, 
					SKELETON_SUBSTRUCTURE, 
					FUNCTIONAL_GROUP, 
					NUM_ATOMS, 
					HAS_FORMULA, 
					WEIGHT, 
					NUM_RINGS, 
					TOTAL_CHARGE, 
					NUM_MOLECULES,
					BOND_ANGLE,
					CHIRAL,
					MAPPED_ATOMS,
					MAPPED_COUNT,
					IS_2D_CHAIR,
					CONFORMATION_CHAIR, 
					HUMAN_REQD}); 
		return allowedEvalConstants;
	} // getAllowedEvaluators(int, long)

	/** Gets an evaluator constant for a new evaluator.  Called by
	 * loadEvaluatorStruct and Nonstruct.jsp.
	 * @param	qType	type of the question being edited
	 * @param	qFlags	flags of the question being edited
	 * @param	haveResp	whether we have a response from View Responses to
	 * load into the evaluator
	 * @return	an appropriate evaluator constant
	 */
	public static int getDefaultEvalConstant(int qType, long qFlags, 
			boolean haveResp) {
		final boolean usesSubstns = Question.usesSubstns(qFlags);
		final boolean is3D = Question.is3D(qFlags) && !usesSubstns;
		final boolean isMapping = Question.showMapping(qFlags)
				&& !usesSubstns && !is3D;
		return (Question.isLewis(qType) ? 
					(haveResp ? LEWIS_ISOMORPHIC : LEWIS_VALENCE_ELECS)
				: Question.isMechanism(qType) ? 
					(haveResp ? MECH_FLOWS : MECH_RULE)
				: Question.isSynthesis(qType) ? SYNTH_SCHEME
				: Question.isFormula(qType) ? HAS_FORMULA
				: Question.isChoice(qType) 
						|| Question.isChooseExplain(qType)
						|| Question.isFillBlank(qType) ? CHOICE_WHICH_CHECKED
				: Question.isRank(qType) ? RANK_ORDER
				: Question.isText(qType) ? TEXT_CONT
				: Question.isNumeric(qType) ? NUM_IS
				: Question.isOED(qType) ? OED_DIFF
				: Question.isRCD(qType) ? RCD_DIFF
				: Question.isTable(qType) ? TABLE_TEXT
				: Question.isClickableImage(qType) ? CLICK_HERE
				: Question.isDrawVectors(qType) ? VECTORS_CT
				: Question.isEquations(qType) ? EQNS_CT
				: Question.isLogicalStatements(qType) ? STMTS_CT
				: isMapping ? MAPPED_ATOMS 
				: haveResp ? IS_OR_HAS_SIGMA_NETWORK
				: Question.isMarvin(qType) ? NUM_ATOMS
				: HUMAN_REQD); 
	} // getDefaultEvalConstant(int, long, boolean)

	/** Load the specific class of EvaluatorImpl according to matchCode.
	 * @param	matchCode	short code unique to an evaluator type
	 * @param	codedData	slash-separated series of short strings and numbers,
	 * such as "Y&gt;/5", that an evaluator uses to evaluate a response
	 * @param	molName	name of molecule (if any) that evaluator compares to the
	 * response
	 * @return	instance of a class for evaluating a response
	 * @throws	ParameterException	if the coded data is malformed
	 */
	public static EvalInterface loadEvaluatorImpl(String matchCode,
			String codedData, String molName) throws ParameterException {
		final String SELF = "EvalManager.loadEvaluatorImpl: ";
		final int evalConstant = Utils.indexOf(EVAL_CODES, matchCode);
		final String evalClassName = getClass(evalConstant);
		debugPrint(SELF + "matchCode = ", matchCode, ", evalConstant = ",
				evalConstant, ", codedData = ", codedData,
				", getClass(", evalConstant, ") = ",
				(evalConstant >= 0 ? evalClassName : "unknown"));
		if (evalClassName == null)  {
			Utils.alwaysPrint(SELF + "ERROR: Unknown match type ", matchCode);
			return null;
		} // if there was no match to a match code
		EvalInterface impl = null;
		if (codedData == null && evalConstant != HUMAN_REQD)
			Utils.alwaysPrint(SELF + "codedData is null.");
		// Create an instance of that class, invoking its constructor with
		// the codedData.
		try {
			final Class<?> implClass = Class.forName(evalClassName);
			implClass.getDeclaredConstructor().newInstance();
			// We never use this instance after we construct it here, but we construct it
			// to make sure the class loader knows about it.
			Class<?>[] paramclass = new Class<?>[1];
			paramclass[0] = Class.forName("java.lang.String");
			final Constructor<?> con = implClass.getConstructor(paramclass);
			Object[] args = new Object[1];
			args[0] = codedData;
			impl = (EvalInterface) con.newInstance(args);
			impl.setMolName(molName);
		/* Why doesn't this method throw a ParameterException when loading the
		 * class?
		} catch (ParameterException e) {
			Utils.alwaysPrint(SELF + "problem with coded data: ", e.getMessage());
			throw new ParameterException(e.getMessage());
		*/
		} catch (ClassNotFoundException e) {
			Utils.alwaysPrint(SELF + "class not found: ", e.getMessage());
		} catch (NoSuchMethodException e) {
			Utils.alwaysPrint(SELF + "no such method: ", e.getMessage());
		} catch (IllegalAccessException e) {
			Utils.alwaysPrint(SELF + "illegal access: ", e.getMessage());
		} catch (InvocationTargetException e) {
			Utils.alwaysPrint(SELF + "initializer threw exception: ",
					e.getMessage(), " because ", e.getCause().getMessage());
			// throw e.getCause(); // that's a throwable, not an exception
		} catch (InstantiationException e) {
			Utils.alwaysPrint(SELF + "InstantiationException: ", e.getMessage());
		}
		return impl;
	} // loadEvaluatorImpl
	
	/** Disables external instantiation. */
	private EvalManager() { }

} // EvalManager
