package com.epoch.evals;

import com.epoch.responses.Response;
import com.epoch.evals.evalConstants.EvalImplConstants;

/** Interface for implementing all evaluators.  All classes in the impl
 * directory must include all of the methods below.  */
public interface EvalInterface extends EvalImplConstants {

	/** Gets the code for identifying this evaluator's type in the database.
	 * Used for backend writing and front end classification.
	 * Maintain uniqueness !!!! 
	 * @return	short string describing the type of this evaluator
	 */
	String getMatchCode();

	/** Returns an English-language description of this evaluator.  Expressed
	 * as a conditional phrase, e.g., 
	 * "If the number of C atoms is greater than 3 ..."
	 * @param	qDataTexts	the text of question data of this question, if any
	 * @param	forPermissibleSM	whether to word the English to describe a
	 * permissible starting material in a multistep synthesis question
	 * @return	short string describing this evaluator in English
	 */
	String toEnglish(String[] qDataTexts, boolean forPermissibleSM);

	/** Return a string representation of data (other than a molecule) that this
	 * evaluator uses to evaluate a response.  Expressed as a slash-separated
	 * string of short strings and numbers, such as "Y&gt;/3"..  
	 * @return	the coded data
	 */
	String getCodedData(); 
	
	/** Determines whether the response satisfies the evaluator.  
	 * @param	response	a parsed response
	 * @param	authStruct	String representation of a molecule that the
	 * evaluator compares to the response; may be null
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied; may also contain automatically generated
	 * feedback or a message describing an inability to evaluate the response
	 * because it was malformed.
	 */
	OneEvalResult isResponseMatching(Response response, String authStruct);

	/** Sets the molecule's name if there is one.  
	 * @param	molName	name of the molecule
	 */
	void setMolName(String molName);

	/** Gets whether this evaluator calculates the grade of the response from
	 * the response.
	 * @return	true if this evaluator calculates the grade of the response
	 */
	boolean getCalcGrade();

} // EvalInterface
