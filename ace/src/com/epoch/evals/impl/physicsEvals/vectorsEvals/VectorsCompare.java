package com.epoch.evals.impl.physicsEvals.vectorsEvals;

import chemaxon.struc.DPoint3;
import com.epoch.chem.VectorMath;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.CompareNums;
import com.epoch.evals.impl.physicsEvals.vectorsEvals.vectorsEvalConstants.VectorsImplConstants;
import com.epoch.exceptions.ParameterException;
import com.epoch.physics.DrawVectors;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the {magnitude, direction, magnitude and direction} of {none, any,
 * the sum} of the response vectors {is/are, is/are not} {=, &lt;, &gt;} 
 * that/those of the author's vector &plusmn; tolerance ... */ 
public class VectorsCompare extends CompareNums 
		implements EvalInterface, VectorsImplConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether to compare the sum or any/no vector. */
	private int noAnySum;
		/** Value of noAnySum. */
		public final static int NO_VECTOR = 0;
		/** Value of noAnySum. */
		public final static int ANY_VECTOR = 1;
		/** Value of noAnySum. */
		public final static int VECTORS_SUM = 2;
		/** Database values for noAnySum. */
		transient private final String[] SUM_OR_ONE_DB_VALUES = new String[]
				{"none", "any", "sum"};
	/** Type of vector quantity to compare. */
	private int vectorQuant;
		/** Value of vectorQuant. */
		public final static int WHOLE_VECTOR = 0;
		/** Value of vectorQuant. */
		public final static int MAGNITUDE = 1;
		/** Value of vectorQuant. */
		public final static int DIRECTION = 2;
		/** Database values for vectorQuant. */
		transient private final String[] VECTOR_QUANT_DB_VALUES = new String[]
				{"whole", "length", "dirn"};
	/** Tolerance in the magnitude. */
	private int lengthTolerance;
	/** Unit of the tolerance in the magnitude. */
	private int lengthToleranceUnit;
		/** Value of lengthToleranceUnit. */
		public final static int PERCENT = 0;
		/** Value of lengthToleranceUnit. */
		public final static int PIXELS = 1;
		/** Database values for lengthToleranceUnit. */
		transient private final String[] LEN_TOL_UNIT_DB_VALUES = new String[] {"%", "px"};
	/** Tolerance in the angle (in degrees). */
	private int angleTolerance;
	/** Name of the structure for which to search. */
	transient private String authVecXML = null;

	/** Constructor. */
	public VectorsCompare() { // default values
		noAnySum = NO_VECTOR;
		vectorQuant = WHOLE_VECTOR;
		setOper(EQUALS); // inherited from CompareNums
		lengthTolerance = 10;
		lengthToleranceUnit = PERCENT;
		angleTolerance = 10;
	} // VectorsCompare()

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>noAnySum</code>/<code>vectorQuant</code>/<code>oper</code>/<code>lengthTolerance</code>/<code>lengthToleranceUnit</code>/<code>angleTolerance</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator or if four coordinates aren't obtained
	 */
	public VectorsCompare(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 6) {
			noAnySum = Utils.indexOf(SUM_OR_ONE_DB_VALUES, splitData[0]);
			vectorQuant = Utils.indexOf(VECTOR_QUANT_DB_VALUES, splitData[1]);
			setOper(Utils.indexOf(SYMBOLS, splitData[2]));
			lengthTolerance = MathUtils.parseInt(splitData[3]);
			lengthToleranceUnit = Utils.indexOf(LEN_TOL_UNIT_DB_VALUES, splitData[4]);
			angleTolerance = MathUtils.parseInt(splitData[5]);
		} else throw new ParameterException("VectorsCompare ERROR: unknown input data " 
				+ "'" + data + "'. ");
	} // VectorsCompare(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>  
	 * <code>noAnySum</code>/<code>vectorQuant</code>/<code>oper</code>/<code>lengthTolerance</code>/<code>lengthToleranceUnit</code>/<code>angleTolerance</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(SUM_OR_ONE_DB_VALUES[noAnySum], '/',
				VECTOR_QUANT_DB_VALUES[vectorQuant], '/',
				SYMBOLS[getOper()], '/', lengthTolerance, '/',
				LEN_TOL_UNIT_DB_VALUES[lengthToleranceUnit], '/', 
				angleTolerance);
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
		final String SELF = "VectorsCompare.toEnglish: ";
		final StringBuilder words = Utils.getBuilder("If the ",
				vectorQuant == WHOLE_VECTOR ? "magnitude and direction"
					: vectorQuant == MAGNITUDE ? "magnitude" : "direction",
				" of ", noAnySum == NO_VECTOR ? "no vector"
					: noAnySum == ANY_VECTOR ? "any vector"
					: "the sum of the vectors",
				" in the response ",
				vectorQuant == WHOLE_VECTOR ? "are" : "is",
				OPER_ENGLISH[FEWER][getOper()]);
		try {
			final DrawVectors authArrows = new DrawVectors(authVecXML);
			DPoint3 authVector = authArrows.getVector(1);
			final double authLength = VectorMath.length(authVector);
			if (authLength == 0) authVector = X_VECTOR;
			final int authAngle = MathUtils.roundToInt(
					toDegrees(angle(authVector, X_VECTOR)));
			if (vectorQuant != DIRECTION) {
				Utils.appendTo(words, MathUtils.roundToInt(authLength),
						" &plusmn; ", lengthToleranceUnit == PERCENT
						? MathUtils.roundToInt(
							authLength * lengthTolerance / 100)
						: lengthTolerance, " px");
			} // if comparing magnitudes
			if (vectorQuant != MAGNITUDE) {
				if (vectorQuant == WHOLE_VECTOR) words.append(" and ");
				Utils.appendTo(words, authAngle >= 0 ? authAngle
							: Utils.getBuilder("&minus;", -authAngle),
						" &plusmn; ", angleTolerance, "&deg;");
			} // if comparing angles
		} catch (ParameterException e) {
			Utils.alwaysPrint(SELF + "caught ParameterException trying to "
					+ "import draw-vectors data:\n", authVecXML);
			words.append("the same in the author's vector");
			if (vectorQuant != DIRECTION) {
				Utils.appendTo(words, " within &plusmn;", 
						lengthTolerance, lengthToleranceUnit == PERCENT 
							? "% of the vector's length" : " px");
			} // if comparing magnitudes
			if (vectorQuant != MAGNITUDE) {
				Utils.appendTo(words, vectorQuant == WHOLE_VECTOR 
						? " and " : " within ", angleTolerance, "&deg;");
			} // if comparing angles
		} // try
		return words.toString();
	} // toEnglish() 

	/** Determines whether the response vectors add up to the author's vector,
	 * or whether no or any of the response vectors equal the author's vector.
	 * @param	response	a parsed response
	 * @param	authString	XML describing author's vector
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing 
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String authString) {
		final String SELF = "VectorsCompare.isResponseMatching: ";
		debugPrint(SELF, toEnglish(), "; noAnySum = ", 
				SUM_OR_ONE_DB_VALUES[noAnySum], ", oper =",
				OPER_ENGLISH[LESSER][getOper()]);
		final OneEvalResult evalResult = new OneEvalResult();
		final DrawVectors respArrows = (DrawVectors) response.parsedResp;
		if (respArrows == null) { 
			final String msg = SELF + "respArrows has not "
					+ "been initialized in Response.";
			Utils.alwaysPrint(msg);
			evalResult.verificationFailureString = msg;
			return evalResult;
		} // if no parsed response
		try {
			final DrawVectors authArrows = new DrawVectors(authString);
			DPoint3 authVector = authArrows.getVector(1);
			final double authLength = VectorMath.length(authVector);
			final boolean authVectorIsNull = authLength == 0;
			if (authVectorIsNull) authVector = X_VECTOR;
			final double authAngle = angle(authVector, X_VECTOR);
			final double authAngleDeg = toDegrees(authAngle);
			debugPrint(SELF + "authVector: ", authVectorIsNull 
					? new DPoint3() : authVector);
			DPoint3[] respVectors = respArrows.getVectors();
			double longestRespLength = 0.0;
			for (final DPoint3 respVector : respVectors) {
				longestRespLength = Math.max(longestRespLength, 
						VectorMath.length(respVector));
			} // for each response vector
			if (noAnySum == VECTORS_SUM) {
				final DPoint3 respVectorsSum = new DPoint3();
				for (final DPoint3 respVector : respVectors) {
					respVectorsSum.add(respVector);
				} // for each response vector
				debugPrint(SELF + "adding vectors ", respVectors, 
						" to give ", respVectorsSum);
				respVectors = new DPoint3[] {respVectorsSum};
			} // if comparing the sum of the vectors
			final boolean wholeVecNotEq = 
					vectorQuant == WHOLE_VECTOR && getOper() == NOT_EQUALS;
			final boolean wholeVecEq = !wholeVecNotEq;
			final boolean noneShouldSatisfy = noAnySum == NO_VECTOR;
			final boolean oneWillSatisfy = !noneShouldSatisfy;
			evalResult.isSatisfied = noneShouldSatisfy;
			for (final DPoint3 respVector : respVectors) {
				boolean magnitudeOK = true;
				boolean directionOK = true;
				final double respLength = VectorMath.length(respVector);
				if (vectorQuant != DIRECTION) {
					final double absLengthTolerance = 
							(lengthToleranceUnit == PIXELS 
							? (double) lengthTolerance
							: Math.max(3.0, // granularity of pixels
								Math.max(authLength, longestRespLength) 
									* ((double) lengthTolerance) / 100.0));
					magnitudeOK = compare(respLength, authLength, 
							absLengthTolerance);
					if (wholeVecNotEq) magnitudeOK = !magnitudeOK;
					debugPrint(SELF + "authVector length = ", authLength,
							", respVector length = ", respLength, 
							", absLengthTolerance = ", absLengthTolerance,
							", oper = ", OPER_ENGLISH[LESSER][getOper()],
							", magnitudeOK = ", magnitudeOK);
				} // if need to compare magnitudes
				final boolean respVectorIsNull = respLength == 0;
				if (authVectorIsNull || respVectorIsNull) {
					debugPrint(SELF + "authVector or respVector is null, "
							+ "so don't need to compare directions.");
				} else if (vectorQuant != MAGNITUDE) {
					final double anglesDiff = angle(authVector, respVector);
					final double anglesDiffTolerance = 
							VectorMath.toRadians(angleTolerance);
					directionOK = compare(anglesDiff, 0.0, 
							anglesDiffTolerance);
					if (wholeVecNotEq) directionOK = !directionOK;
					debugPrint(SELF + "authVector angle = ", authAngleDeg,
							", respVector angle = ", 
							toDegrees(angle(respVector, X_VECTOR)), 
							", anglesDiff = ", toDegrees(anglesDiff),
							", angleTolerance = ", angleTolerance,
							", oper = ", OPER_ENGLISH[LESSER][getOper()],
							", directionOK = ", directionOK);
				} // if need to compare directions
				final boolean vectorOK = magnitudeOK && directionOK;
				final boolean vectorSatisfies = wholeVecEq == vectorOK;
				evalResult.isSatisfied = vectorSatisfies == oneWillSatisfy;
				final boolean breaking = 
						(evalResult.isSatisfied && oneWillSatisfy)
						|| (vectorSatisfies && noneShouldSatisfy);
				debugPrint(SELF + "vectorOK = ", vectorOK,
						", vectorSatisfies = ", vectorSatisfies,
						", oneWillSatisfy = ", oneWillSatisfy,
						", evalResult.isSatisfied = ", evalResult.isSatisfied,
						", breaking = ", breaking);
				if (breaking) break;
			} // for each vector
		} catch (ParameterException e) {
			Utils.alwaysPrint(SELF + "caught ParameterException trying to "
					+ "import draw-vectors data:\n", authString);
			e.printStackTrace();
		} // try
		debugPrint(SELF + "returning ", evalResult.isSatisfied);
		return evalResult;
	} // isResponseMatching(Response, String)

	/** Gets the angle between two vectors originating at a center.
	 * @param	vector1	first vector
	 * @param	vector2	second vector
	 * @return	the angle between -&pi; and &pi;
	 */
	private double angle(DPoint3 vector1, DPoint3 vector2) {
		final double sign = VectorMath.angleSign(vector1, vector2);
		return VectorMath.angle(vector1, vector2) * (sign == 0 ? 1 : sign);
	} // angle(DPoint3, DPoint3)

	/** Converts radians to degrees. 
	 * @param	radians	an angle in radians
	 * @return	the angle in degrees
	 */
	private double toDegrees(double radians) {
		return VectorMath.toDegrees(radians);
	} // toDegrees(double)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[VECTORS_COMP]; } 
	/** Gets whether we are looking for no vector, any vector, or the vector 
	 * sum.
	 * @return	whether we are looking for no vector, any vector, or the vector
	 * sum
	 */
	public int getNoAnySum() 				{ return noAnySum; } 
	/** Sets whether we are looking for no vector, any vector, or the vector 
	 * sum.
	 * @param	s	whether we are looking for no vector, any vector, or the
	 * vector sum
	 */
	public void setNoAnySum(int s) 			{ noAnySum = s; } 
	/** Gets the property or properties of the vectors we are comparing.
	 * @return	property or properties of the vectors we are comparing
	 */
	public int getVectorQuant() 			{ return vectorQuant; } 
	/** Sets the property or properties of the vectors we are comparing.
	 * @param	vq	property or properties of the vectors we are comparing
	 */
	public void setVectorQuant(int vq) 		{ vectorQuant = vq; } 
	/** Gets the tolerance in the magnitude.
	 * @return	the tolerance in the magnitude
	 */
	public int getLengthTolerance() 		{ return lengthTolerance; } 
	/** Sets the tolerance in the magnitude.
	 * @param	tol	the tolerance in the magnitude
	 */
	public void setLengthTolerance(int tol)	{ lengthTolerance = tol; }
	/** Gets the units of the tolerance in the magnitude.
	 * @return	the units of the tolerance in the magnitude
	 */
	public int getLengthToleranceUnit() 	{ return lengthToleranceUnit; } 
	/** Sets the units of the tolerance in the magnitude.
	 * @param	u	the units of the tolerance in the magnitude
	 */
	public void setLengthToleranceUnit(int u)	{ lengthToleranceUnit = u; }
	/** Gets the tolerance in the angle.
	 * @return	the tolerance in the angle
	 */
	public int getAngleTolerance() 			{ return angleTolerance; } 
	/** Sets the tolerance in the angle.
	 * @param	tol	the tolerance in the angle
	 */
	public void setAngleTolerance(int tol)	{ angleTolerance = tol; }
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 			{ return false; }
	/** Sets the value of the author's vector. 
	 * @param	vecXML	XML describing the author's vector
	 */
	public void setMolName(String vecXML) 	{ authVecXML = vecXML; }

} // VectorsCompare

