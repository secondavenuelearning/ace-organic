package com.epoch.evals.impl.chemEvals;

import chemaxon.struc.Molecule;
import com.epoch.chem.MolCompare;
import com.epoch.chem.MolCompareException;
import com.epoch.chem.Normalize;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.CompareNums;
import com.epoch.evals.impl.chemEvals.chemEvalConstants.MolCompareConstants;
import com.epoch.exceptions.ParameterException;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;

/** If the {total number of compounds, number of distinct compounds} 
 * in the response {is, is not} {=, &lt;, &gt;} <i>n</i> ... */
public class NumMols extends CompareNums 
		implements EvalInterface, MolCompareConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg, Utils.MRV);
	}

	/** Number of compounds against which to compare. */
	transient private int authNumMols;
	/** Whether to count distinct compounds only. */
	private boolean distinct;
	/** If counting distinct compounds, flags to determine how to count 
	 * compounds as distinct: treat enantiomers as identical, eschew normalization,
	 * treat resonance structures as equivalent, look at sigma-bond network only. */
	private int flags;

	/** Constructor. */
	public NumMols() {
		authNumMols = 1;
		setOper(NOT_EQUALS); // inherited from CompareNums
		distinct = false;
		flags = 0;
	} // NumMols()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>oper</code>/<code>authNumMols</code>/<code>distinct</code>/<code>flags</code>
	 * @throws	ParameterException	if the coded data is inappropriate
	 * for this evaluator
	 */
	public NumMols(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		final int numData = splitData.length;
		if (numData >= 4) {
			setOper(Utils.indexOf(SYMBOLS, splitData[0]));
			authNumMols = MathUtils.parseInt(splitData[1]);
			distinct = Utils.isPositive(splitData[2]);
			flags = MathUtils.parseInt(splitData[3]);
		}
		if (numData < 4 || getOper() == -1) {
			throw new ParameterException("NumMols ERROR: "
					+ "unknown input data '" + data + "'. ");
		} // if there are not two tokens
	} // NumMols(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>oper</code>/<code>authNumMols</code>/<code>distinct</code>/<code>flags</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(SYMBOLS[getOper()], '/', authNumMols,
				distinct ? "/Y/" : "/N/", flags);
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
		final StringBuilder words = Utils.getBuilder("If the ",
				distinct ? "number of distinct" : "total number of",
				" compounds is", OPER_ENGLISH[FEWER][getOper()],
				authNumMols);
		if (distinct && flags != 0) {
			final boolean noNorm = noNormalization();
			final boolean resPerm = resonancePermissive();
			final boolean ent = eitherEnantiomer();
			words.append(" (");
			if (noNorm) words.append("without normalization");
			if (resPerm || ent) {
				if (noNorm) words.append(", ");
				words.append("treating ");
				if (resPerm) words.append("resonance structures");
				if (resPerm && ent) words.append(" and ");
				if (ent) words.append("enantiomers");
				words.append(" as identical");
			}
			if (checkSigmaNetwork()) {
				if (noNorm || resPerm || ent) words.append(", ");
				words.append("comparing #sigma-bond networks only");
			}
			words.append(')');
		 } // if comparing molecules and there are flags
		return words.toString();
	 } // toEnglish()

	/** Determines whether the response contains the indicated number of
	 * compounds.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final String SELF = "NumMols.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		if (distinct && response.normalized == null) {
			response.normalized = Normalize.normalize(response.moleculeObj);
		} // if haven't already normalized
		final Molecule respMol = (distinct && !noNormalization() 
				? response.normalized : response.moleculeObj).clone();
		debugPrint(SELF + "\n", respMol);
		final Molecule[] mols = respMol.convertToFrags();
		debugPrint(SELF + "after converting to fragments:\n", mols);
		final int respCount = (distinct 
				? countDistinctMols(mols) : mols.length);
		evalResult.isSatisfied = compare(respCount, authNumMols);
		debugPrint(SELF + "actual numMols = ", respCount, ", expected = ", 
				authNumMols, ", result = ", evalResult.isSatisfied);
		return evalResult;	
	} // isResponseMatching(Response, String)

	/** Counts the number of distinct molecules in an array.  If one molecule is a
	 * more general version of another (e.g., 2-butanol and (R)-2-butanol), it
	 * counts them as one.
	 * @param	mols	an array of molecules
	 * @return	the number of distinct molecules in the array
	 */
	private int countDistinctMols(Molecule[] mols) {
		final String SELF = "NumMols.countDistinctMols: ";
		final boolean sigma = checkSigmaNetwork();
		final boolean resPerm = resonancePermissive();
		final boolean ent = eitherEnantiomer();
		final boolean IS_LEWIS = true;
		debugPrint(SELF + "looking for number of distinct molecules "
				+ "among ", mols, "; no normalization = ", noNormalization(), 
				", compare sigma network = ", sigma, 
				", resonance-permissive = ", resPerm, 
				", enantiomer-permissive = ", ent);
		final ArrayList<Molecule> distinctMols = new ArrayList<Molecule>();
		for (final Molecule mol : mols) {
			boolean foundMatch = false;
			for (int dNum = 0; dNum < distinctMols.size(); dNum++) {
				final Molecule distinctMol = distinctMols.get(dNum);
				try {
					// need to match both ways because matches aren't diagonal
					if ((sigma && MolCompare.matchSigmaNetwork(
								distinctMol, mol, !IS_LEWIS, ent))
							|| (resPerm && MolCompare.areResonanceOrIdentical(
								distinctMol, mol, !IS_LEWIS, ent))
							|| MolCompare.matchExact(distinctMol, mol, ent)
							) {
						debugPrint(SELF + "target distinctMol ", 
								distinctMol, " matches to query mol ", 
								mol, "; replacing former with latter");
						distinctMols.set(dNum, mol);
						foundMatch = true;
						break;
					} else if ((sigma && MolCompare.matchSigmaNetwork(
								mol, distinctMol, !IS_LEWIS, ent))
							|| (resPerm && MolCompare.areResonanceOrIdentical(
									mol, distinctMol, !IS_LEWIS, ent))
							|| MolCompare.matchExact(mol, distinctMol, ent)
							) {
						debugPrint(SELF + "target mol ", mol,
								" matches to query distinctMol ", 
								distinctMol, "; going to next mol");
						foundMatch = true;
					} // if distinctMol matches mol
				} catch (MolCompareException e) {
					debugPrint(SELF + "MolCompareException comparing ",
							mol, " to ", distinctMol);
				} // try
			} // for each already found distinct molecule
			if (!foundMatch) {
				debugPrint(SELF + "adding ", mol, " to distinctMols");
				distinctMols.add(mol);
			} // if mol not found in distinctMols
		} // for each response molecule
		final int count = distinctMols.size();
		debugPrint(SELF + "distinctMols are ", distinctMols,
				"; returning count of ", count);
		return count;
	} // countDistinctMols(Molecule[])

	/** Gets if the "&sigma;-network" flag is set.
	 * @return	true if the "&sigma;-network" flag is set
	 */
	private boolean checkSigmaNetwork() {
		return (flags & SIGMA_NETWORK) != 0;
	} // checkSigmaNetwork()

	/** Gets if the "either enantiomer" flag is set.
	 * @return	true if the "either enantiomer" flag is set
	 */
	private boolean eitherEnantiomer() {
		return (flags & EITHER_ENANTIOMER) != 0;
	} // eitherEnantiomer()

	/** Gets if the "no normalization" flag is set.
	 * @return	true if the "no normalization" flag is set
	 */
	private boolean noNormalization() {
		return (flags & NO_NORMALIZATION) != 0;
	} // noNormalization()

	/** Gets if the "any resonance structure" flag is set.
	 * @return	true if the "any resonance structure" flag is set
	 */
	private boolean resonancePermissive() {
		return (flags & RESONANCE_PERMISSIVE) != 0;
	} // resonancePermissive()

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[NUM_MOLECULES]; } 
	/** Gets the number of compounds against which to compare.
	 * @return	the number of compounds against which to compare
	 */
	public int getNumMols() 				{ return authNumMols; }
	/** Sets the number of compounds against which to compare.
	 * @param	n	the number of compounds against which to compare
	 */
	public void setNumMols(int n) 			{ authNumMols = n; }
	/** Gets whether to compare total number of compounds or number of distinct
	 * compounds.
	 * @return	true if should compare number of distinct compounds, false if
	 * should compare total number of compounds
	 */
	public boolean getDistinct() 			{ return distinct; }
	/** Sets whether to compare total number of compounds or number of distinct
	 * compounds.
	 * @param	d	whether to compare total number of compounds or number of
	 * distinct compounds
	 */
	public void setDistinct(boolean d) 		{ distinct = d; }
	/** Gets the flags for this search.
	 * @return	flags for this search
	 */
	public int getFlags() 					{ return flags; }
	/** Sets the flags for this search.
	 * @param	flags	flags for this search
	 */
	public void setFlags(int flags) 		{ this.flags = flags; }

} // NumMols
