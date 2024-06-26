package com.epoch.exceptions;

/** Thrown when ACE cannot verify the response submitted by the user.  */ 
public class EquationFormatException extends java.lang.Exception {

	/** Constant found in all exceptions. */
	private static final long serialVersionUID = 1L;
	/** The equation causing the exception. */
	private final String equation; // initialized in constructor

	/** Constructor.
	 * @param	eqn	the equation that is misformatted
	 */
	public EquationFormatException(String eqn) {
		super(getStandardMessage());
		equation = eqn;
	} // EquationFormatException(String)

	/** Constructor.
	 * @param	message	an error message
	 * @param	eqn	the equation that is misformatted
	 */
	public EquationFormatException(String message, String eqn) {
		super(message);
		equation = eqn;
	} // EquationFormatException(String, String)

	/** Gets the malformatted equation. 
	 * @return	the malformatted equation
	 */
	public String getEquation() {
		return equation;
	} // getEquation()

	/** Gets the message for an EquationFormatException.
	 * @return	the message
	 */
	public static String getStandardMessage() {
		return "ACE is unable to analyze the mathematical "
				+ "expression: ***expression*** "
				+ "If the expression contains implicit multiplication "
				+ "of variables or variables and units, as in "
				+ "\"a(bc + 1)d m/s^2\", "
				+ "please insert the times symbol * to make the "
				+ "multiplication explicit, "
				+ "as in \"a * (b * c + 1) * d * m/s^2\". "
				+ "Contact your instructor for help if you need it.";
	} // getStandardMessage(String)

} // EquationFormatException 
