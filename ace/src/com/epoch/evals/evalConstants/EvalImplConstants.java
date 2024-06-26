package com.epoch.evals.evalConstants;

import com.epoch.xmlparser.xmlConstants.XMLConstants;

/** Holds internal constants and database match codes of evaluators.  Do NOT
 * change the names or values of the constants corresponding to the different
 * evaluators. */
public interface EvalImplConstants extends XMLConstants {

	// public static final is implied by interface

	/** Constant corresponding to Atoms. */
	int NUM_ATOMS = 0;
	/** Constant corresponding to Rings. */
	int NUM_RINGS = 1;
	/** Constant corresponding to NumMols. */
	int TOTAL_CHARGE = 2;
	/** Constant corresponding to Charge. */
	int NUM_MOLECULES = 3;
	/** Constant corresponding to ConformChair. */
	int CONFORMATION_CHAIR = 4;
	/** Constant corresponding to ConformBond. */
	int CONFORMATION_ACYCLIC = 5;
	/** Constant corresponding to LewisValenceTotal. */
	int LEWIS_VALENCE_ELECS = 6;
	/** Constant corresponding to LewisFormalCharge. */
	int LEWIS_FORMAL_CHGS = 7;
	/** Constant corresponding to LewisOuterNumber. */
	int LEWIS_OUTER_SHELL_COUNT = 8;
	/** Constant corresponding to LewisElecDeficCt. */
	int LEWIS_ELECTRON_DEFICIENT = 9;
	/** Constant corresponding to MechShape. */
	int MECH_TOPOLOGY = 10;
	/** Constant corresponding to MechRule. */
	int MECH_RULE = 11;
	/** Constant corresponding to MechCounter. */
	int MECH_PIECES_COUNT = 12;
	/** Constant corresponding to Weight. */
	int WEIGHT = 13;
	/** Constant corresponding to FnalGroup. */
	int FUNCTIONAL_GROUP = 14;
	/** Constant corresponding to HasFormula. */
	int HAS_FORMULA = 15;
	/** Constant corresponding to MultipleCheck. */
	int CHOICE_WHICH_CHECKED = 16;
	/** Constant corresponding to RankOrderCheck. */
	int RANK_ORDER = 17;
	/** Constant corresponding to RankPositionCheck. */
	int RANK_POSITION = 18;
	/** Constant corresponding to MultipleNumChosen. */
	int CHOICE_NUM_CHECKED = 19;
	/** Constant corresponding to LewisIsomorph. */
	int LEWIS_ISOMORPHIC = 20;
	/** Constant corresponding to MechFlowsValid. */
	int MECH_FLOWS = 21;
	/** Constant corresponding to MechProdStartIs. */
	int MECH_PRODS_STARTERS_IS = 22;
	/** Constant corresponding to MapProperty. */
	int MAPPED_ATOMS = 23;
	/** Constant corresponding to MechSubstructure. */
	int MECH_SUBSTRUCTURE = 24;
	/** Constant corresponding to Contains. */
	int SKELETON_SUBSTRUCTURE = 25;
	/** Constant corresponding to Is. */
	int IS_OR_HAS_SIGMA_NETWORK = 26;
	/** Constant corresponding to SynthScheme. */
	int SYNTH_SCHEME = 27;
	/** Constant corresponding to SynthTarget. */
	int SYNTH_TARGET = 28;
	/** Constant corresponding to SynthSteps. */
	int SYNTH_STEPS = 29;
	/** Constant corresponding to SynthStart. */
	int SYNTH_STARTERS = 30;
	/** Constant corresponding to SynthEfficiency. */
	int SYNTH_SM_MADE = 31;
	/** Constant corresponding to SynthSelective. */
	int SYNTH_SELEC = 32;
	/** Constant corresponding to NumberIs. */
	int NUM_IS = 33;
	/** Constant corresponding to NumberSigFigs. */
	int NUM_SIGFIG = 34;
	/** Constant corresponding to NumberUnit. */
	int NUM_UNIT = 35;
	/** Constant corresponding to TextContains. */
	int TEXT_CONT = 36;
	/** Constant corresponding to TextWords. */
	int TEXT_WORDS = 37;
	/** Constant corresponding to BondAngle. */
	int BOND_ANGLE = 38;
	/** Constant corresponding to HumanReqd. */
	int HUMAN_REQD = 39;
	/** Constant corresponding to HumanReqd. */
	int CHIRAL = 40;
	/** Constant corresponding to TableTextVal. */
	int TABLE_TEXT = 41;
	/** Constant corresponding to TableNumVal. */
	int TABLE_NUM = 42;
	/** Constant corresponding to TableTextText. */
	int TBL_TXT_TXT = 43;
	/** Constant corresponding to TableTextNum. */
	int TBL_TXT_NUM = 44;
	/** Constant corresponding to TableNumText. */
	int TBL_NUM_TXT = 45;
	/** Constant corresponding to TableNumNum. */
	int TBL_NUM_NUM = 46;
	/** Constant corresponding to SynthOneRxn. */
	int SYNTH_ONE_RXN = 47;
	/** Constant corresponding to TableDiff. */
	int TABLE_DIFF = 48;
	/** Constant corresponding to OEDDiff. */
	int OED_DIFF = 49;
	/** Constant corresponding to OEDElectronCount. */
	int OED_ELEC = 50;
	/** Constant corresponding to OEDOrbType. */
	int OED_TYPE = 51;
	/** Constant corresponding to RCDDiff. */
	int RCD_DIFF = 52;
	/** Constant corresponding to RCDStateCt. */
	int RCD_STATE_CT = 53;
	/** Constant corresponding to TableCellNumCt. */
	int TABLE_CT_NUM = 54;
	/** Constant corresponding to TableCellTextCt. */
	int TABLE_CT_TXT = 55;
	/** Constant corresponding to Is2DChair. */
	int IS_2D_CHAIR = 56;
	/** Constant corresponding to ClickHere. */
	int CLICK_HERE = 58;
	/** Constant corresponding to MechInitiation. */
	int MECH_INIT = 59;
	/** Constant corresponding to MechEquals. */
	int MECH_EQUALS = 60;
	/** Constant corresponding to SynthEquals. */
	int SYNTH_EQUALS = 61;
	/** Constant corresponding to MechProdStartProps. */
	int MECH_PRODS_STARTERS_PROPS = 62;
	/** Constant corresponding to MechProdStartProps. */
	int TEXT_SEMANTICS = 63;
	/** Constant corresponding to LogicStmtsCt. */
	int STMTS_CT = 64;
	/** Constant corresponding to VectorsCt. */
	int VECTORS_CT = 65;
	/** Constant corresponding to VectorsCompare. */
	int VECTORS_COMP = 66;
	/** Constant corresponding to ClickText. */
	int CLICK_TEXT = 67;
	/** Constant corresponding to ClickNumber. */
	int CLICK_NUM = 68;
	/** Constant corresponding to VectorsAxes. */
	int VECTORS_AXES = 69;
	/** Constant corresponding to EqnsCt. */
	int EQNS_CT = 70;
	/** Constant corresponding to EqnIs. */
	int EQN_IS = 71;
	/** Constant corresponding to EqnsFollow. */
	int EQNS_FOLLOW = 72;
	/** Constant corresponding to EqnSolved. */
	int EQN_SOLVED = 73;
	/** Constant corresponding to EqnVariables. */
	int EQN_VARS = 74;
	/** Constant corresponding to ClickCount. */
	int CLICK_CT = 75;
	/** Constant corresponding to ClickLabelsCompare. */
	int CLICK_LABELS_COMP = 76;
	/** Constant corresponding to CountMetals. */
	int COUNT_METALS = 77;
	/** Constant corresponding to FormulaFormat. */
	int FORMULA_FORMAT = 78;
	/** Constant corresponding to FormulaFormat. */
	int FORMULA_WEIGHT = 79;
	/** Constant corresponding to UnsaturationIndex. */
	int UNSATURATION = 80;
	/** Constant corresponding to MapSelectionsCounter. */
	int MAPPED_COUNT = 81;

	/** Array of codes used to identify evaluators in database.
	 * Maximum of 10 characters.
	 * <p>DO NOT CHANGE THESE CODES or we may have incompatibilities between
	 * development and production versions of ACE during Q import/export.
	 * </p><p>Order of array constants corresponds to constants of impl 
	 * classes, so do NOT rearrange the order of classes in this list!
	 */
	String[] EVAL_CODES = {
			"ATOMS", // 0
			"CYCLES", // 1
			"CHARGE", // 2
			"NUMMOLS", // 3
			"CONF_AXEQ", // 4
			"CONF_SH", // 5
			"LEW_VAL", // 6
			"LEW_FML", // 7
			"LEW_OUT", // 8
			"LEW_DEF", // 9
			"MEC_TOPOL", // 10
			"MEC_RULE", // 11
			"MEC_CNT", // 12
			"WT", // 13
			"GROUP", // 14
			"FORMULA", // 15
			"MUL_CHK", // 16
			"RANK_ORD", // 17
			"RANK_POS", // 18
			"MUL_NUM", // 19
			"LEW_ISO", // 20
			"MEC_ARROW", // 21
			"MEC_PRST", // 22
			"MAP_PRP", // 23
			"MEC_SUB", // 24
			"CONTAIN", // 25
			"IS", // 26
			"SYN_SCHEME", // 27
			"SYN_TARGET", // 28
			"SYN_STEPS", // 29
			"SYN_START", // 30
			"SYN_EFFIC", // 31
			"SYN_SELECT", // 32
			"NUM_IS", // 33
			"NUM_SIGFIG", // 34
			"NUM_UNIT", // 35
			"TXT_CONT", // 36
			"TXT_WRDS", // 37
			"ANGL", // 38
			"HUMAN", // 39
			"CHIRAL", // 40
			"TAB_TXT", // 41
			"TAB_NUM", // 42
			"TAB_TXTREL", // 43
			"TAB_NUMREL", // 44
			"TAB_NUMTXT", // 45
			"TAB_NUMNUM", // 46
			"SYN_RXN", // 47
			"TAB_DIFF", // 48
			"OED_DIFF", // 49
			"OED_ELEC", // 50
			"OED_TYPE", // 51
			"RCD_DIFF", // 52
			"RCD_STATE", // 53
			"TAB_CT_NUM", // 54
			"TAB_CT_TXT", // 55
			"CHAIR2D_IS", // 56
			"FBD_VSTATE", // 57 -- not used anymore
			"CLICK_HERE", // 58
			"MECH_INIT", // 59
			"MECH_EQ", // 60
			"SYN_EQ", // 61
			"MEC_PRST_P", // 62
			"TXT_SEMANT", // 63
			"STMTS_CT", // 64
			"ARROWS_CT", // 65
			"ARROWS_SUM", // 66
			"CLICK_TEXT", // 67
			"CLICK_NUM", // 68
			"VEC_AXES", // 69
			"EQNS_CT", // 70
			"EQN_IS", // 71
			"EQNS_FOLLW", // 72
			"EQN_SOLVED", // 73
			"EQN_VARS", // 74
			"CLICK_CT", // 75
			"CLICK_LC", // 76
			"METALS_CT", // 77
			"FORMU_FORM", // 78
			"FORMULA_WT", // 79
			"UNSATUR", // 80
			"MAP_CT" // 81
			};
	
} // EvalImplConstants
