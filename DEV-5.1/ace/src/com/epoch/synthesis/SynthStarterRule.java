package com.epoch.synthesis;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.struc.Molecule;
import com.epoch.chem.ChemUtils;
import com.epoch.chem.MolString;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.Evaluator;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.Subevaluator;
import com.epoch.evals.evalConstants.EvalImplConstants;
import com.epoch.exceptions.ParameterException;
import com.epoch.qBank.Figure;
import com.epoch.responses.Response;
import com.epoch.substns.SubstnUtils;
import com.epoch.synthesis.synthConstants.SynthConstants;
import com.epoch.utils.Utils;

/** Contains and uses an evaluator that determines whether a compound has a 
 * property that makes it a permissible starting material for a synthesis. */
public class SynthStarterRule implements EvalImplConstants, SynthConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	//----------------------------------------------------------------------
	//							members
	//----------------------------------------------------------------------
	/** Data of an evaluator that corresponds to a rule about the permissible
	 * starting materials. */
	transient private final Subevaluator subeval = new Subevaluator(); 

	//----------------------------------------------------------------------
	//							constructors
	//----------------------------------------------------------------------	
	/** Constructor. */
	public SynthStarterRule() {
		// intentionally empty
	} // SynthStarterRule()

	/** Constructor.  Called by front end to make verbal description of
	 * evaluator. 
	 * @param	codedData	contains evaluator type, coded data
	 */
	public SynthStarterRule(String codedData) {
		init(codedData, Figure.NO_RGROUPS);
	} // SynthStarterRule(String)

	/** Constructor.  Called by Synthesis.
	 * @param	codedData	contains evaluator type, coded data
	 * @param	rgMols	R groups associated with the user's instance of the
	 * question; may be null or empty
	 */
	public SynthStarterRule(String codedData, Molecule... rgMols) {
		init(codedData, rgMols);
	} // SynthStarterRule(String, Molecule...)

	/** Creates the evaluator. 
	 * @param	codedData	contains evaluator type, coded data
	 * @param	rgMols	R groups associated with the user's instance of the
	 * question; may be null or empty
	 */
	private void init(String codedData, Molecule... rgMols) {
		final String SELF = "SynthStarterRule.init: ";
		if (Utils.isEmpty(codedData)) return;
		try {
			final int firstDiv = codedData.indexOf('/');
			subeval.matchCode = codedData.substring(0, firstDiv);
			String modCodedData = codedData.substring(firstDiv + 1);
			if (getEvalType() == IS_OR_HAS_SIGMA_NETWORK) {
				// Data is howMany/flags/molName/molStruct.
				// Separate molName, molStruct from other coded data,
				// and convert data to howMany/flags.
				final String[] parsed = modCodedData.split("/");
				modCodedData = parsed[0] + "/" + parsed[1];
				subeval.molName = parsed[2];
				final StringBuilder molBld = new StringBuilder();
				for (int piece = 3; piece < parsed.length; piece++) {
					if (piece > 3) molBld.append('/');
					molBld.append(parsed[piece]);
				} // for each remaining piece of molecule string
				subeval.molStruct = molBld.toString();
				if (!Utils.isEmpty(rgMols)) {
					final Molecule mol = SubstnUtils.substituteRGroups(
							subeval.molStruct, rgMols);
					debugPrint(SELF + "converted ", subeval.molStruct,
							" to ", mol);
					subeval.molStruct = MolString.toString(mol);
				} // if there are R groups
			} // if evaluator is IS_OR_HAS_SIGMA_NETWORK
			subeval.codedData = modCodedData;
			debugPrint(SELF + "raw data = ", codedData,
					", subeval.codedData = ", subeval.codedData);
		} catch (NumberFormatException e2) {
			Utils.alwaysPrint(SELF + "Caught NumberFormatException for "
					+ "codedData ", codedData);
		} catch (IndexOutOfBoundsException e3) {
			Utils.alwaysPrint(SELF + "Caught IndexOutOfBoundsException for "
					+ "codedData ", codedData);
		} // try
	} // init(String, Molecule...)

	//----------------------------------------------------------------------
	//					short get methods	
	//----------------------------------------------------------------------	
	/** Gets the internal constant corresponding to the evaluator invoked by
	 * this synthesis starting material rule.
	 * @return	the internal constant corresponding to the evaluator
	 */
	final public int getEvalType() {
		return subeval.getEvalType();
	} // getEvalType()

	/** Gets the molecule.
	 * @return	the molecule
	 */
	public String getMolStruct() {
		return subeval.molStruct;
	} // getMolStruct()

	/** Gets the molecule's name.
	 * @return	the molecule's name
	 */
	public String getMolName() {
		return subeval.molName;
	} // getMolName()

	/** Gets the evaluator's specific implementation.
	 * @return	the evaluator's specific implementation
	 */
	public EvalInterface getEvaluator() {
		final String SELF = "SynthStarterRule.getEvaluator: ";
		subeval.setEvaluatorImpl();
		final EvalInterface evalImpl = subeval.getEvalImpl();
		if (evalImpl == null) {
			Utils.alwaysPrint(SELF + "Could not construct evaluator "
					+ "with subeval matchCode = ", subeval.matchCode,
					" and coded data ", subeval.codedData);
		} // evalImpl is null
		return evalImpl;
	} // getEvaluator()

	//----------------------------------------------------------------------
	//						toEnglish
	//----------------------------------------------------------------------	
	/** Generates a description of this rule about permissible starting
	 * materials for display.
	 * @return	description of this rule about permissible starting materials
	 */
	public String toEnglish() {
		return subeval.toEnglish(true);
	} // toEnglish()

	//----------------------------------------------------------------------
	//						evaluate
	//----------------------------------------------------------------------	
	/** Determines whether a response molecule is a permissible starting
	 * material according to this rule.
	 * @param	mol	the response molecule
	 * @return	true if the response molecule is a permissible starting material
	 * according to this rule
	 */
	boolean evaluate(Molecule mol) {
		final String SELF = "SynthStarterRule.evaluate: ";
		final int evalType = getEvalType();
		final Response resp = new Response(evalType == TOTAL_CHARGE 
				? ChemUtils.stripMetals(mol) : mol);
		OneEvalResult evalResult = null;
		try {
			if (evalType == IS_OR_HAS_SIGMA_NETWORK
					&& !Utils.isEmpty(subeval.molStruct)
					&& subeval.molStruct.indexOf(".") >= 0) {
				final Molecule mergedAuthMols = 
						MolImporter.importMol(subeval.molStruct);
				final Molecule[] authMols = mergedAuthMols.convertToFrags();
				int fragNum = 0;
				for (final Molecule authMol : authMols) {
					debugPrint(SELF + "comparing ", mol, " to permissible "
							+ "starting material ", authMol);
					final Subevaluator subevalSingleMol = 
							new Subevaluator(subeval);
					subevalSingleMol.molStruct = MolString.toString(authMol);
					final Evaluator evalIs = new Evaluator(subevalSingleMol);
					evalResult = evalIs.matchResponse(resp);
					final boolean satisfied = evalResult != null 
							&& evalResult.verificationFailureString == null
							&& evalResult.isSatisfied;
					if (satisfied || fragNum++ >= authMols.length - 1) {
						debugPrint(SELF, mol, ' ', satisfied ? "matches" 
								: "doesn't match", " to ", authMol);
						return satisfied;
					} // if there's a match
				} // for each molecule in authMols
			} else if (evalType != IS_OR_HAS_SIGMA_NETWORK
					|| !Utils.isEmpty(subeval.molStruct)) {
				final Evaluator evalGen = new Evaluator(subeval);
				evalResult = evalGen.matchResponse(resp);
			} // if evalType
		} catch (MolFormatException e) {
			Utils.alwaysPrint(SELF + "Could not import ", subeval.molStruct);
			e.printStackTrace();
		} catch (ParameterException e) {
			Utils.alwaysPrint(SELF + "Could not evaluate the response.");
			e.printStackTrace();
		} // try
		return (evalResult != null 
				&& evalResult.verificationFailureString == null
				&& evalResult.isSatisfied);
	} // evaluate(Molecule)

} // SynthStarterRule

