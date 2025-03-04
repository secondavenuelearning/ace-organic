package com.epoch.evals.impl.chemEvals.mechEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.ParameterException;
import com.epoch.mechanisms.Mechanism;
import com.epoch.mechanisms.mechConstants.MechConstants;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the topology of the response mechanism {is, is not} {linear, chain,
 *	linear or chain} ... */
public class MechShape implements EvalInterface, MechConstants {
	
	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether the evaluator is satisfied by the response having (true) or not
	 * having (false) the topology. */
	private boolean isPositive;
	/** What kind of topology the response should or shouldn't have. */
	private int topology;
		/** Value for topology.  */
		public static final int IS_LINEAR = 1;
		/** Value for topology.  */
		public static final int IS_CYCLIC = 2;
		/** Value for topology.  */
		public static final int IS_EITHER = 3;

	/** Constructor. */
	public MechShape() {
		isPositive = true;
		topology = IS_LINEAR;
	} // MechShape()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>/<code>topology</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public MechShape(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 2) {
			isPositive = Utils.isPositive(splitData[0]);
			topology = MathUtils.parseInt(splitData[1]);
		} else {
			throw new ParameterException("MechShape ERROR: unknown input data "
					+ "'" + data + "'. ");
		}
	} // MechShape(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>isPositive</code>/<code>topology</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(isPositive ? "Y/" : "N/", topology);
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
		return Utils.toString("If the topology of the response mechanism is ",
				isPositive ? "" : "not ",
				topology == IS_LINEAR ? "linear"
					: topology == IS_CYCLIC ? "cyclic"
					: topology == IS_EITHER ? "linear or cyclic"
					: "invalid");
	} // toEnglish()

	/** Determines whether the response has the indicated topology.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final OneEvalResult evalResult = new OneEvalResult();
		final Mechanism mech = (Mechanism) response.parsedResp;
		final int respTopology = mech.getTopology();
		debugPrint("MechShape.isResponseMatching: respTopology is ", 
				(respTopology == LINEAR ? "linear" : "cyclic"),
				", isPositive = ", isPositive);
		switch (topology) {
			case IS_LINEAR:
				evalResult.isSatisfied = 
						isPositive == (respTopology == LINEAR);
				break;
			case IS_CYCLIC:
				evalResult.isSatisfied = 
						isPositive == (respTopology == CYCLIC);
				break;
			case IS_EITHER:
				evalResult.isSatisfied = 
						isPositive == Utils.among(respTopology, LINEAR, CYCLIC);
				break;
			default:
				Utils.alwaysPrint("MechShape.isResponseMatching: invalid case!");
				evalResult.isSatisfied = false;
				break;
		}
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 				{ return EVAL_CODES[MECH_TOPOLOGY]; } 
	/** Gets whether the evaluator is satisfied by the response having or not
	 * having the topology.
	 * @return	true if the evaluator is satisfied by the response having the
	 * topology.
	 */
	public boolean getIsPositive() 				{ return isPositive; }
	/** Sets whether the evaluator is satisfied by the response having or not
	 * having the topology.
	 * @param	isPos	true if the evaluator is satisfied by the response
	 * having the topology.
	 */
	public void setIsPositive(boolean isPos)	{ isPositive = isPos; }
	/** Gets what kind of topology the response should or shouldn't have.
	 * @return	the kind of topology
	 */
	public int getTopology() 					{ return topology; }
	/** Sets what kind of topology the response should or shouldn't have.
	 * @param	topology	the kind of topology
	 */
	public void setTopology(int topology) 		{ this.topology = topology; }
	/** Not used.  Required by interface.
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName) 		{ /* intentionally empty */ }
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 				{ return false; }

} // MechShape

