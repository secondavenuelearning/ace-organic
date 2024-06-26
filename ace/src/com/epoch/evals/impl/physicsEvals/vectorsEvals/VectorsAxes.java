package com.epoch.evals.impl.physicsEvals.vectorsEvals;

import chemaxon.struc.DPoint3;
import com.epoch.chem.VectorMath;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.Counter;
import com.epoch.evals.impl.implConstants.CountConstants;
import com.epoch.evals.impl.physicsEvals.vectorsEvals.vectorsEvalConstants.VectorsImplConstants;
import com.epoch.exceptions.ParameterException;
import com.epoch.physics.DrawVectors;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If {no, the only, exactly one, any, not every, every} vector in the 
 * response lays along the {x-, y-, x- or y-} axis...  */
public class VectorsAxes extends Counter 
		implements CountConstants, EvalInterface, VectorsImplConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether to compare to x- or y-axes or both. */
	private int direction;
		/** Value for direction.  */
		public static final int X_ONLY = 0;
		/** Value for direction.  */
		public static final int Y_ONLY = 1;
		/** Value for direction.  */
		public static final int X_AND_Y = 2;
		/** Database values for direction. */
		static final String[] DIRECTION_DB_VALUES = new String[]
				{"X", "Y", "X_or_Y"};
		/** English values for direction. */
		public static final String[] DIRECTION_ENGL = new String[]
				{"x-", "y-", "x- or y-"};
	/** Tolerance in the angle (in degrees). */
	transient private int tolerance;
	/** Angle of north-pointing vector. */
	private static final double DEG_90 = Math.PI / 2.0;

	/** Constructor. */
	public VectorsAxes() { // default values
		direction = X_ONLY;
		howMany = NONE;
		tolerance = 5;
	} // VectorsAxes()

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>howMany</code>/<code>direction</code>/<code>tolerance</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public VectorsAxes(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 3) {
			howMany = MathUtils.parseInt(splitData[0]);
			direction = Utils.indexOf(DIRECTION_DB_VALUES, splitData[1]);
			tolerance = MathUtils.parseInt(splitData[2]);
		} else {
			throw new ParameterException("VectorsAxes ERROR: unknown input data " 
					+ "'" + data + "'. ");
		}
	} // VectorsAxes(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>  
	 * <code>howMany</code>/<code>direction</code>/<code>tolerance</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(howMany, '/', DIRECTION_DB_VALUES[direction], 
				'/', tolerance);
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
		return Utils.toString("If", HOWMANY_ENGL[howMany - 1],
				" vector lays along the ", DIRECTION_ENGL[direction], "axis");
	} // toEnglish() 

	/** Determines whether the response has the indicated number of the
	 * indicated type of graphical object.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing 
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String authString) {
		final String SELF = "VectorsExes.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		final DrawVectors respArrows = (DrawVectors) response.parsedResp;
		if (respArrows == null) { 
			final String msg = SELF + "respArrows has not "
					+ "been initialized in Response.";
			Utils.alwaysPrint(msg);
			evalResult.verificationFailureString = msg;
			return evalResult;
		} // if no parsed response
		final int[] matches = new int[2];
		int vectorNum = 0;
		setOper(EQUALS);
		for (final DPoint3 respVector : respArrows.getVectors()) {
			final double respAngle = quarterAngle(respVector);
			final double tolRadians = ((double) tolerance) * Math.PI / 180.0;
			final boolean satisfied = 
					(direction != Y_ONLY 
						&& compare(respAngle, 0.0, tolRadians))
					|| (direction != X_ONLY
						&& compare(respAngle, DEG_90, tolRadians));
			if (satisfied) {
				matches[MATCHES]++;
				debugPrint(SELF + "vector ", ++vectorNum, ", ", respVector,
						", satisfies evaluator.");
				if (Utils.among(howMany, ANY, NONE)) break;
			} else {
				matches[NONMATCHES]++;
				debugPrint(SELF + "vector ", ++vectorNum, ", ", respVector,
						", doesn't satisfy evaluator.");
				if (Utils.among(howMany, NOT_ALL, ALL)) break;
			}
		} // for each response vector vectorNum
		debugPrint(SELF + "matches = ", matches[MATCHES],
				", nonmatches = ", matches[NONMATCHES]);
		evalResult.isSatisfied = getIsSatisfied(matches);
		return evalResult;
	} // isResponseMatching(Response, String)

	/** Gets the angle of a vector originating at a center.
	 * @param	vector	the vector
	 * @return	the angle between 0 and &pi; / 2
	 */
	private double quarterAngle(DPoint3 vector) {
		final double anglePos = VectorMath.angle(vector, X_VECTOR);
		return (anglePos > DEG_90 ? Math.PI - anglePos : anglePos);
	} // quarterAngle(DPoint3)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[VECTORS_AXES]; } 
	/** Gets whether to count horizontal, vertical, or either. 
	 * @return	what to count
	 */
	public int getDirection() 				{ return direction; } 
	/** Sets whether to count horizontal, vertical, or either. 
	 * @param	direction	what to count
	 */
	public void setDirection(int direction)	{ this.direction = direction; } 
	/** Gets the tolerance in the angle.
	 * @return	the tolerance in the angle
	 */
	public int getAngleTolerance() 			{ return tolerance; } 
	/** Sets the tolerance in the angle.
	 * @param	tol	the tolerance in the angle
	 */
	public void setAngleTolerance(int tol)	{ tolerance = tol; }

} // VectorsAxes

