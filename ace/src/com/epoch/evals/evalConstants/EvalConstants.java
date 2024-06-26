package com.epoch.evals.evalConstants;

/** Holds evaluator constants used by the Evaluator class. */
public interface EvalConstants extends EvalImplConstants {

	// public static final is implied by interface

	/** Parameter for getDescription(). */
	boolean PARENT_ONLY = false;

	/** Match codes of evaluators that provide automatic feedback. */
	String[] AUTOFEEDBACK_EVALS = new String[] {
			EVAL_CODES[MAPPED_COUNT],
			EVAL_CODES[IS_2D_CHAIR], 
			EVAL_CODES[CONFORMATION_CHAIR], 
			EVAL_CODES[FORMULA_FORMAT],
			EVAL_CODES[MECH_RULE],
			EVAL_CODES[MECH_FLOWS],
			EVAL_CODES[SYNTH_SCHEME],
			EVAL_CODES[SYNTH_SELEC],
			EVAL_CODES[OED_DIFF],
			EVAL_CODES[RCD_DIFF],
			EVAL_CODES[EQN_IS],
			EVAL_CODES[EQNS_FOLLOW],
			EVAL_CODES[HUMAN_REQD]
			};

	/** Match codes of evaluators that exclude instantiated R groups'
	 * properties when calculating the properties of a molecule. */
	String[] EXCLUDE_R_GROUPS = new String[] {
			EVAL_CODES[NUM_ATOMS],
			EVAL_CODES[NUM_RINGS],
			EVAL_CODES[WEIGHT],
			EVAL_CODES[FUNCTIONAL_GROUP],
			EVAL_CODES[HAS_FORMULA]
			};

	/** Match codes of evaluators that may calculate a grade based on a
	 * response. */
	String[] CALC_GRADE_EVALS = new String[] {
			EVAL_CODES[MAPPED_COUNT],
			EVAL_CODES[MECH_PIECES_COUNT],
			EVAL_CODES[SYNTH_STEPS],
			EVAL_CODES[SYNTH_SCHEME],
			EVAL_CODES[SYNTH_SELEC],
			EVAL_CODES[TABLE_DIFF]
			};

	/** Match codes of evaluators that use permissible starting
	 * materials as defined by question data. */
	String[] USE_PERMISSIBLE_SMS = new String[] {
			EVAL_CODES[SYNTH_SCHEME],
			EVAL_CODES[SYNTH_SM_MADE]
			};

	/** Match codes of evaluators that require at least one image. */
	String[] NEEDS_IMAGE = new String[] {
			EVAL_CODES[CLICK_HERE],
			EVAL_CODES[CLICK_TEXT],
			EVAL_CODES[VECTORS_COMP]
			};

	/** Tag for XML IO.  */
	String EVALUATOR_TAG = "answer";
	/** Tag for XML IO.  */
	String EVAL_ID_TAG = "id";
	/** Tag for XML IO.  */
	String EVAL_TYPE_TAG = "type";
	/** Tag for XML IO.  */
	String EXPRESSION_TAG = "expression";
	/** Tag for XML IO.  */
	String IF_TAG = "if";
	/** Tag for XML IO.  */
	String MOL_TAG = "mol";
	/** Tag for XML IO.  */
	String MOLNAME_TAG = "molname";
	/** Tag for XML IO.  */
	String MOLSTRUCT_TAG = "molstruct";
	/** Tag for XML IO.  */
	String CODEDDATA_TAG = "codeddata";
	/** Tag for XML IO.  */
	String GRADE_TAG = "grade";
	/** Tag for XML IO.  */
	String FEEDBACK_TAG = "feedback";
	/** Attribute value for XML IO.  */
	String PARTIAL_ATTR = "PARTIAL";
	/** Attribute value for XML IO.  */
	String CORRECT_ATTR = "CORRECT";
	/** Attribute value for XML IO.  */
	String WRONG_ATTR = "WRONG";
	
} // EvalConstants
