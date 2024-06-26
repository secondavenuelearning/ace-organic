package com.epoch.evals.impl.chemEvals.mechEvals;

import chemaxon.marvin.plugin.PluginException;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.evalConstants.OneEvalConstants;
import com.epoch.evals.impl.chemEvals.Contains;
import com.epoch.exceptions.ParameterException;
import com.epoch.mechanisms.Mechanism;
import com.epoch.mechanisms.MechError;
import com.epoch.mechanisms.mechConstants.MechErrorConstants;
import com.epoch.mechanisms.MechRuleFunctions;
import com.epoch.mechanisms.MechSubstructSearch;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the mechanism rule {choose from set} {is, is not} violated ...  */ 
public class MechRule 
		implements EvalInterface, MechErrorConstants, OneEvalConstants {
	
	private void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Which rule to check for violations. */
	private int rule;
		/** Value for rule.   */
		public static final int SN2_SP = 1;
		/** Value for rule.   */
		public static final int SUPPLY_RECEIVE = 2;
		/** Value for rule.   */
		public static final int IONIZE_BOND = 3;
		/** Value for rule.   */
		public static final int ACID_PK = 4;
		/** Value for rule.   */
		public static final int BASE_PK = 5;
		/** Value for rule.   */
		public static final int CARBOCATION = 6;
		/** Value for rule.   */
		public static final int RADICAL = 7;
		/** Value for rule.   */
		public static final int RES_CONNECT = 8;
		/** Value for rule.   */
		public static final int CONNECT_RES = 9;
		/** Value for rule.   */
		public static final int PERICYCLIC = 10;
		/** Value for rule.   */
		public static final int PERI_FOUR = 11;
		/** Value for rule.   */
		public static final int NO_ZWITTERIONS = 12;
		/** Value for rule.   */
		public static final int PKA_RULE = 13;
		/** Value for rule.   */
		public static final int NO_ATOM_MULTIPLE_CHG = 14;
		/** Value for rule.   */
		public static final int CATIONIC_SHIFTS_MUST_BE_1_2 = 15;
		/** Value for rule.   */
		public static final int NO_MOL_MULTIPLE_TOTAL_CHG = 16;
		/** Value for rule.   */
		public static final int NO_ACIDIC_SN2 = 17;
		/** Value for rule.   */
		public static final int PRIMARY_CARBOCAT = 18;
		/** Value for rule.   */
		public static final int NO_X_H_BOND_X_NUC = 19;
		/** Value for rule.   */
		public static final int NO_SAME_CHARGE_REACT = 20;
		/** Value for rule.   */
		public static final int NO_TERMOLECULAR = 21;
		/** Value for rule.   */
		public static final int NO_DYOTROPIC = 22;
		/** Value for rule.   */
		public static final int NO_SIGMA_METATHESIS = 23;
		/** Value for rule.   */
		public static final int NO_FOUR_MEMB_PROTON_TRANSFER = 24;
		/** Value for rule.   */
		public static final int NO_ACIDIC_E2 = 25;
	/** Whether the evaluator is satisfied if the rule is upheld (true) or 
	 * if it is violated (false).  */
	private boolean isPositive; 
	/** For ACID_PK, BASE_PK, the p<i>K</i> to compare against. */
	transient private double pKvalue; 
	/** For NO_ZWITTERIONS, PKA_RULE, RES_CONNECT, flags on which steps to
	 * ignore, whether to consider positive atoms bearing an H; 
	 * For NO_SAME_CHARGE_REACT, if on, allow proton transfers.
	 */ 
	private int flags; 
		/** Bit for flags.   */
		public static final int OMIT_LAST_STEP_MASK = (1 << 0); // for PKA_RULE
		/** Bit for flags.   */
		public static final int OMIT_1ST_STEP_MASK = (1 << 1); // for PKA_RULE, RES_CONNECT
		/** Bit for flags.   */
		public static final int POS_BEARS_H_MASK = (1 << 2); // for NO_ZWITTERIONS
		/** Bit for flags.   */
		public static final int ALLOW_PROTON_TRANSFER = (1 << 3); // for NO_SAME_CHARGE_REACT

	/** Array (1-based) containing English-language versions of each rule. */
	public static final String[] RULES_TEXT = { "",
		"no S<sub>N</sub>2 at an sp- or sp<sup>2</sup>-hybridized atom",
		"no atom simultaneously receives and supplies unshared electrons",
		"no ionization of a bond between a leaving group and an sp- or "
			+ "sp<sup>2</sup>-hybridized atom",
		"no acids have p<i>K</i><sub>a</sub> values less than ",
		"no bases whose conjugate acids have p<i>K</i><sub>a</sub> values "
			+ "greater than ",
		"no carbocations (except iminium ions) under basic reaction conditions",
		"no radicals under polar reaction conditions",
		"boxes containing resonance structures must be connected by a "
			+ "double-headed arrow",
		"boxes connected by a double-headed arrow must contain resonance "
			+ "structures",
		"pericyclic reactions other than electrocyclic reactions must not "
			+ "involve an even number of pairs of electrons",
		"pericyclic reactions other than electrocyclic reactions must not "
			+ "involve four atoms",
		"no intermediate should be a zwitterion",
		"the p<i>K</i><sub>a</sub> rule",
		"no atom may have a total charge of &plusmn;2 or greater", 
		"cationic shifts must be 1,2-shifts", 
		"no compound may have a total charge of &plusmn;2 or greater", 
		"no SN2 substitution under acidic conditions", 
		"neither 1&deg; carbocations not stabilized by resonance nor CH3^+",
		"the electrons in an X&ndash;H bond are not used by X to make a new "
			+ "&sigma; bond",
		"two compounds both charged positively or negatively do not react",
		"no termolecular reactions",
		"no dyotropic rearrangements",
		"no &sigma;-bond metatheses or dyotropic rearrangements",
		"no H^+ transfer by a four-membered transition state", 
		"no E2 elimination under acidic conditions"
	};

	/** Constructor. */
	public MechRule() {
		isPositive = false;
		rule = SN2_SP; // just to be valid
	} // MechRule()

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>/<code>rule</code>/<code>pKvalue</code>/<code>flags</code> 
	 * <br>The pKvalue (a double) and flags (an int) are only used for a few rules, 
	 * and no rule uses both.
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public MechRule(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 4) {
			isPositive = Utils.isPositive(splitData[0]);
			rule = MathUtils.parseInt(splitData[1]);
			pKvalue = MathUtils.parseDouble(splitData[2]);
			flags = MathUtils.parseInt(splitData[3]);
		} else {
			throw new ParameterException("MechRule ERROR: unknown input data " 
					+ "'" + data + "'. "); 
		}
		if (rule == NO_ZWITTERIONS && flags != 0) {
			// convert flag value to desired value
			flags = POS_BEARS_H_MASK;
		}
	} // MechRule(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format:<br>  
	 * <code>isPositive</code>/<code>rule</code>/<code>pKvalue</code>/<code>flags</code> 
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(isPositive ? "Y/" : "N/", rule, '/', pKvalue, 
				'/', flags);
	} // getCodedData()

	/** Gets an English-language description of this evaluator.
	 * @param	qDataTexts	the text of question data of this question, if any;
	 * not used, but required by interface
	 * @param	forPermissibleSM	whether to word the English to describe a
	 * permissible starting material in a multistep synthesis question
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish(String[] qDataTexts, boolean forPermissibleSM) {
		return toEnglish();
	} // toEnglish(String[], boolean)

	/** Gets an English-language description of this evaluator.  
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish() {
		return Utils.toString("If the response mechanism ",
				isPositive ? " upholds " : "violates ",
				rule != PKA_RULE ? "the rule: " : "", RULES_TEXT[rule], 
				Utils.among(rule, ACID_PK, BASE_PK) ? pKvalue 
					: rule == NO_ZWITTERIONS && flags == POS_BEARS_H_MASK
					? " in which the positive atom bears an H atom" 
					: Utils.among(rule, RES_CONNECT, CONNECT_RES)
						&& flags == OMIT_1ST_STEP_MASK
					? ", omitting the first (photochemical) step" 
					: rule == PKA_RULE && flags == OMIT_1ST_STEP_MASK
					? ", omitting the first step (reaction initiation)" 
					: rule == PKA_RULE && flags == OMIT_LAST_STEP_MASK
					? ", omitting the last step (workup)" 
					: rule == PKA_RULE 
						&& flags == (OMIT_1ST_STEP_MASK | OMIT_LAST_STEP_MASK)
					? ", omitting the first and last steps " +
						"(reaction initiation and workup)" 
					: rule == NO_SAME_CHARGE_REACT 
						&& flags == ALLOW_PROTON_TRANSFER
					? ", disregarding proton transfers"
					: "");
	} // toEnglish() 

	/** Determines whether the response violates the indicated rule.  
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a response modified 
	 * with color or automatically generated feedback or a message describing 
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String authString) { 
		final String SELF = "MechRule.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		boolean valid = false;
		final boolean HYB_MATTERS = true;
		final boolean ACIDIC = true;
		final Mechanism mechanism = (Mechanism) response.parsedResp;
		try {
			final MechRuleFunctions mech = new MechRuleFunctions(mechanism); 
			switch (rule) {
				case SN2_SP:		 valid = mech.containsSN2(HYB_MATTERS);	   break;
				case SUPPLY_RECEIVE: valid = mech.noSupplyReceive();		   break;
				case IONIZE_BOND:	 valid = mech.noIonizedBondSpHybridAtom(); break;
				case ACID_PK:		 valid = mech.pKaCheck(pKvalue, ACIDIC);   break;
				case BASE_PK:		 valid = mech.pKaCheck(pKvalue, !ACIDIC);  break;
				case CARBOCATION:	 valid = mech.noCarbocations();			   break;
				case RADICAL:		 valid = mech.noRadicals();				   break;
				case PERI_FOUR:		 valid = mech.noPeriInvolvesFourAtoms();   break; 
				case PERICYCLIC:	 valid = mech.noPeriHasEvenPairsElecs();   break;
				case NO_ACIDIC_SN2:  valid = mech.containsSN2(!HYB_MATTERS);   break;
				case NO_TERMOLECULAR:
									 valid = mech.noTermolecular();			   break;
				case NO_ATOM_MULTIPLE_CHG: 
									 valid = mech.noMultiplyCharged();		   break;
				case NO_MOL_MULTIPLE_TOTAL_CHG: 
									 valid = mech.noMolMultiplyCharged();	   break;
				case NO_SAME_CHARGE_REACT: 
									 valid = mech.noSameChargeReacting(
									 		flags == ALLOW_PROTON_TRANSFER);   break;
				case RES_CONNECT:	 valid = mech.resConnect(
											flags == OMIT_1ST_STEP_MASK);	   break;
				case CONNECT_RES:	 valid = mech.connectRes(
											flags == OMIT_1ST_STEP_MASK);	   break;
				case NO_ZWITTERIONS: 
									 valid = mech.noZwitterions(
											flags == POS_BEARS_H_MASK);        break;
				case CATIONIC_SHIFTS_MUST_BE_1_2:
				case NO_X_H_BOND_X_NUC:
				case NO_DYOTROPIC:
				case NO_SIGMA_METATHESIS:
				case NO_FOUR_MEMB_PROTON_TRANSFER:
				case NO_ACIDIC_E2:
					final MechSubstructSearch mechSub = 
							new MechSubstructSearch(mechanism); 
					switch (rule) {
						case CATIONIC_SHIFTS_MUST_BE_1_2:
							valid = mechSub.eschewsCatShiftNot1_2(); break;
						case NO_X_H_BOND_X_NUC:
							valid = mechSub.eschewsXHBondAsXNucleophile(); break;
						case NO_DYOTROPIC:
							valid = mechSub.noDyotropicRearrt(); break;
						case NO_SIGMA_METATHESIS:
							valid = mechSub.noSigmaMetathesis(); break;
						case NO_FOUR_MEMB_PROTON_TRANSFER:
							valid = mechSub.noFourMembProtonTransfer(); break;
						case NO_ACIDIC_E2:
							valid = mechSub.noAcidicE2(); break;
						default:
							valid = false; break; // shouldn't happen
					} // switch
					break;
				case PKA_RULE: 
					final boolean omitFirstStep = (flags & OMIT_1ST_STEP_MASK) != 0;
					final boolean omitLastStep = (flags & OMIT_LAST_STEP_MASK) != 0;
					valid = mech.pKaRuleCheck(omitFirstStep, omitLastStep);
					break;
				case PRIMARY_CARBOCAT: 
					final String CONTAINS_DATA = 
							Contains.ANY + "/" + Contains.SUBSTRUCT + "/" + Contains.EXACT;
					final Contains mechContains = new Contains(CONTAINS_DATA);
					final String CX3CH2_PLUS = "[H][C+]([H])[#1,CX4]";
					final OneEvalResult containsResult = 
							mechContains.isResponseMatching(response, CX3CH2_PLUS);
					valid = !containsResult.isSatisfied;
					if (!valid && !isPositive) {
						evalResult.modifiedResponse = 
								containsResult.modifiedResponse;
					} // if structure found and evaluator satisfied
					break;
				default: 
					evalResult.verificationFailureString = Utils.toString(
							"MechRule: invalid rule number given: ", rule, 
							" (valid = 1...", RULES_TEXT.length - 1, ")");
					return evalResult;
			} // switch (rule)
		} catch (PluginException e) {
			evalResult.verificationFailureString = 
					"MechRule: error in pKa calculation: " + e.getMessage();
		} catch (MechError e) {
			// MechRules "returned FALSE" by/and threw feedback
			debugPrint(SELF + "caught MechError; isPositive = ", isPositive);
			if (!isPositive) {
				evalResult.modifiedResponse = e.getMessage();
				final String errorFeedback = e.getErrorFeedback();
				debugPrint(SELF + "errorFeedback = ", errorFeedback,
						", e.offendingCpds = ", e.offendingCpds);
				evalResult.autoFeedback = new String[] {errorFeedback};
				if (!Utils.isEmpty(e.offendingCpds) 
						&& errorFeedback.contains(STARS)) {
					final String href = Utils.toString(
							"<a href=\"javascript:", OPEN_OFFENDERS, "('",
							Utils.toValidJS(e.offendingCpds), "')\">");
					evalResult.autoFeedbackVariableParts = 
							new String[] {href, "</a>"};
					evalResult.howHandleVarParts |= INSERT;
					debugPrint(SELF + "evalResult.autoFeedbackVariableParts = ",
							evalResult.autoFeedbackVariableParts);
				} // if there are offendingCpds and a phrase to substitute
			} // if not positive
		} catch (Exception e) {
			evalResult.verificationFailureString = 
					"MechRule: error in mechanism setup: " + e.getMessage();
			e.printStackTrace();
			return evalResult;
		} // try
		evalResult.isSatisfied = isPositive == valid;
		return evalResult;	 
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 				{ return EVAL_CODES[MECH_RULE]; } 
	/** Gets which rule to check for violations. 
	 * @return	which rule to check for violations
	 */
	public int getRule() 						{ return rule; } 
	/** Sets which rule to check for violations. 
	 * @param	rule	which rule to check for violations
	 */
	public void setRule(int rule) 				{ this.rule = rule; } 
	/** Gets whether the evaluator is satisfied if the rule is upheld or 
	 * if it is violated.  
	 * @return	true if the evaluator is satisfied by being upheld
	 */
	public boolean getIsPositive() 				{ return isPositive; } 
	/** Sets whether the evaluator is satisfied if the rule is upheld or 
	 * if it is violated.  
	 * @param	isPos	true if the evaluator is satisfied by being upheld
	 */
	public void setIsPositive(boolean isPos)	{ isPositive = isPos; } 
	/** Gets the p<i>K</i> to compare against (for ACID_PK, BASE_PK). 
	 * @return	the p<i>K</i> value
	 */
	public double getPKValue() 					{ return pKvalue; } 
	/** Sets the p<i>K</i> to compare against (for ACID_PK, BASE_PK). 
	 * @param	pKvalue	the p<i>K</i> value
	 */
	public void setPKValue(double pKvalue) 		{ this.pKvalue = pKvalue; } 
	/** Gets flags for NO_ZWITTERIONS, PKA_RULE, RES_CONNECT.
	 * @return	the flags
	 */
	public int getFlags() 						{ return flags; } 
	/** Sets flags for NO_ZWITTERIONS, PKA_RULE, RES_CONNECT.
	 * @param	flags	the flags
	 */
	public void setFlags(int flags) 			{ this.flags = flags; } 
	/** Not used.  Required by interface. 
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName) 		{ /* intentionally empty */ }
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 				{ return false; }

} // MechRule
