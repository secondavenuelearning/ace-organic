package com.epoch.mechanisms;

import com.epoch.mechanisms.mechConstants.MechErrorConstants;
import com.epoch.utils.Utils;

/** Contains the results of a failure of a mechanism operation. */
public class MechError extends Exception implements MechErrorConstants {

	private static void debugPrint(Object... msg) { 
		// Utils.printToLog(msg);
	}

	//----------------------------------------------------------------------
	//							members
	//----------------------------------------------------------------------
	/** MRV of response modified with color to highlight an error. */
	transient public String modifiedResponse = null;
	/** Products calculated from incorrect electron-flow arrows. */
	transient public String calcdProds = null;
	/** Compound(s) that a student drew to trigger this MechError. */
	transient public String offendingCpds = null;
	/** Whether the error was a verification error that
	 * prevented analysis of the mechanism. */
	transient public boolean verificationError = false;
	/** Indicates what was the nature of the error.  Used to generate automatic
	 * feedback. */
	transient public int errorNumber = UNCHECKED;
	/** The object indices (in the parent MDocument) of the electron-flow arrow
	 * or arrows causing the error. */
	transient public int[] highlightObjIndices = new int[0];

	/** Constant that all exceptions have. */
	private static final long serialVersionUID = 1L;

	//----------------------------------------------------------------------
	//							constructors
	//----------------------------------------------------------------------
	/** Constructor. */
	public MechError() {
		// intentionally empty
	} // MechError()

	/** Constructor. 
	 * @param	message	a message
	 */
	public MechError(String message) {
		super(message);
		debugPrint(message);
	} // MechError(String)

	/** Constructor. 
	 * @param	num	number of the error
	 */
	public MechError(int num) {
		errorNumber = num;
	} // MechError(int)

	/** Constructor. 
	 * @param	message	a message
	 * @param	num	number of the error
	 */
	public MechError(String message, int num) {
		super(message);
		errorNumber = num;
	} // MechError(String, int)

	/** Constructor. 
	 * @param	message	a message
	 * @param	num	number of the error
	 * @param	calcdProds	products calculated from incorrect electron-flow
	 * arrows
	 */
	public MechError(String message, int num, String calcdProds) {
		super(message);
		this.calcdProds = calcdProds;
		errorNumber = num;
		debugPrint("MechError: message: ", message);
	} // MechError(String, int, String)

	/** Constructor. 
	 * @param	message	a message
	 * @param	num	number of the error
	 * @param	objIndices	the object indices of the electron-flow arrows 
	 * or atoms causing the error
	 */
	public MechError(String message, int num, int[] objIndices) {
		super(message);
		errorNumber = num;
		highlightObjIndices = objIndices;
		debugPrint("MechError: message: ", message);
	} // MechError(String, int, int[])

	/** Constructor. 
	 * @param	message	a message
	 * @param	num	number of the error
	 * @param	calcdProds	products calculated from incorrect electron-flow
	 * arrows
	 * @param	offendingCpds	compound(s) that the student drew to trigger
	 * this MechError
	 */
	public MechError(String message, int num, String calcdProds, 
			String offendingCpds) {
		super(message);
		errorNumber = num;
		this.calcdProds = calcdProds;
		this.offendingCpds = offendingCpds;
		debugPrint("MechError: message: ", message, "; offendingCpds = ", 
				offendingCpds);
	} // MechError(String, int, String, String)

	/** Constructor. 
	 * @param	message	a message
	 * @param	num	number of the error
	 * @param	calcdProds	products calculated from incorrect electron-flow
	 * arrows
	 * @param	objIndices	the object indices of the electron-flow arrows 
	 * or atoms causing the error
	 */
	public MechError(String message, int num, String calcdProds, 
			int[] objIndices) {
		super(message);
		errorNumber = num;
		this.calcdProds = calcdProds;
		highlightObjIndices = objIndices;
		debugPrint("MechError: message: ", message);
	} // MechError(String, int, String, int[])

	/** Constructor. 
	 * @param	message	a message
	 * @param	num	number of the error
	 * @param	calcdProds	products calculated from incorrect electron-flow
	 * arrows
	 * @param	offendingCpds	compound(s) that the student drew to trigger
	 * this MechError
	 * @param	objIndices	the object indices of the electron-flow arrows 
	 * or atoms causing the error
	 */
	public MechError(String message, int num, String calcdProds, 
			String offendingCpds, int[] objIndices) {
		super(message);
		errorNumber = num;
		this.calcdProds = calcdProds;
		this.offendingCpds = offendingCpds;
		highlightObjIndices = objIndices;
		debugPrint("MechError: message: ", message);
	} // MechError(String, int, String, String, int[])

	/** Constructor. 
	 * @param	className	name of class that generated the error
	 * @param	message	a message
	 */
	public MechError(String className, String message) {
		super(message);
		debugPrint(className, ": ", message);
	} // MechError(String, String)

	/** Constructor. 
	 * @param	className	name of class that generated the error
	 * @param	message	a message
	 * @param	modResponse	MRV of response modified with color to highlight 
	 * the error
	 */
	public MechError(String className, String message, String modResponse) {
		super(message);
		modifiedResponse = modResponse;
		debugPrint(className, ": ", message);	   
	} // MechError(String, String, String)

	/** Constructor. 
	 * @param	message	a message
	 * @param	isVerifyError	whether the error was a verification error that
	 * prevented analysis of the mechanism
	 */
	public MechError(String message, boolean isVerifyError) {
		super(message);
		verificationError = isVerifyError;
		debugPrint(message); 
	} // MechError(String, boolean)

	/** Constructor. 
	 * @param	className	name of class that generated the error
	 * @param	message	a message
	 * @param	isVerifyError	whether the error was a verification error that
	 * prevented analysis of the mechanism
	 */
	public MechError(String className, String message, boolean isVerifyError) {
		super(message);
		verificationError = isVerifyError;
		debugPrint(className, ": ", message);
	} // MechError(String, String, boolean)

	/** Constructor. 
	 * @param	className	name of class that generated the error
	 * @param	message	a message
	 * @param	modResponse	MRV of response modified with color to highlight 
	 * the error
	 * @param	isVerifyError	whether the error was a verification error that
	 * prevented analysis of the mechanism
	 */
	public MechError(String className, String message, String modResponse,
			boolean isVerifyError) {
		super(message);
		verificationError = isVerifyError;
		modifiedResponse = modResponse;
		debugPrint(className, ": ", message); 
	} // MechError(String, String, String, boolean)

	/** Constructor. 
	 * @param	className	name of class that generated the error
	 * @param	message	a message
	 * @param	isVerifyError	whether the error was a verification error that
	 * prevented analysis of the mechanism
	 * @param	variablePart	a string to substitute for part of the message
	 */
	public MechError(String className, String message, boolean isVerifyError, 
			String variablePart) {
		super(message);
		verificationError = isVerifyError;
		calcdProds = variablePart;
		debugPrint(className, ": ", message.replaceAll(STARS_REGEX 
				+ "[^\\*]*" + STARS_REGEX, variablePart));	  
	} // MechError(String, String, boolean, String)

	//----------------------------------------------------------------------
	//							getErrorFeedback
	//----------------------------------------------------------------------
	/** Gets automatically generated feedback for various kinds of errors
	 * that a student might have made. 
	 * @return	automatically generated feedback 
	 */
	public String getErrorFeedback() {
		final StringBuilder fdbkBld = new StringBuilder();
		final boolean oneHighlightObj = highlightObjIndices.length == 1;
		switch (errorNumber) {
		case BAD_PKA_OMITTED_COPRODUCTS:
			return "The highlighted stage contains " + STARS + "coproducts"
					+ STARS + " whose structures you have omitted, but the "
					+ "compounds are present nonetheless, and these compounds "
					+ "cannot be generated under the given reaction "
					+ "conditions."; 
		case FLOWS_GIVE_NO_PRODS:
			fdbkBld.append("The electron-flow arrows in the highlighted step "
					+ "do not lead to any of the products drawn in the "
					+ "subsequent step.");
			if (!Utils.isEmpty(highlightObjIndices)) addFlowFeedback(fdbkBld);
			return fdbkBld.toString();
		case INITIATOR_IN_PROPAGATION:
			return "In a chain mechanism, the initiator should not undergo "
					+ "a reaction in the propagation part of the mechanism, "
					+ "nor should any intermediate or product contain "
					+ "atoms that come from the initiator."; 
		case MALFORMED_CHAIN:
			return "Because it is the first step in a cyclic mechanism, the "
					+ "highlighted step should contain a compound that is "
					+ "produced by <i>both</i> of the preceding steps. ";
		case NEGATIVE_BOND:
			fdbkBld.append("More electrons are being removed from the "
					+ "highlighted bond");
			if (!oneHighlightObj) fdbkBld.append('s');
			return Utils.toString(fdbkBld, " than there are electrons in ",
					oneHighlightObj ? "it" : "them",
					", leading to products that have at least one "
						+ "\"bond\" with a negative number of electrons.");
		case NEGATIVE_UNSHARED_ELECTRONS:
			fdbkBld.append("The removal of electrons from the highlighted "
					+ "atom");
			if (!oneHighlightObj) fdbkBld.append('s');
			return Utils.toString(fdbkBld, " leads to products that have ",
					oneHighlightObj ? "an atom" : "atoms",
					" with a negative number of valence electrons.");
		case NO_FLOW_ARROWS:
			return "The highlighted step has no electron-flow arrows."; 
		case NO_INITIATION:
			return "A chain mechanism should have an initiation part; yours "
					+ "does not. ";
		case NOT_PROD_NOR_STARTER:
			fdbkBld.append("The highlighted step contains " + STARS 
					+ "one or more compounds" + STARS 
					+ " that are neither products of the electron-flow arrows "
					+ "in the previous step nor acceptable starting "
					+ "materials.");
			if (!Utils.isEmpty(highlightObjIndices)) addFlowFeedback(fdbkBld);
			return fdbkBld.toString();
		case NOT_PROD_NOR_STARTER_1ST_CYCLIC:
			fdbkBld.append("The highlighted step contains " + STARS 
					+ "one or more compounds" + STARS 
					+ " that are neither products of the electron-flow arrows "
					+ "in either of the previous steps nor acceptable starting "
					+ "materials.");
			if (!Utils.isEmpty(highlightObjIndices)) addFlowFeedback(fdbkBld);
			return fdbkBld.toString();
		case NOT_STARTER:
			return "The highlighted step contains " + STARS 
					+ "one or more compounds" + STARS 
					+ " that are not acceptable starting materials.";
		case ODD_ELECTRON_BOND:
			return "An odd number of electrons is being added to or removed "
					+ "from the highlighted bond, leading to products that "
					+ "have at least one \"bond\" with an odd number of "
					+ "electrons.";
		case TWO_E_ARROW_ATOM_TO_ATOM:
			return "The highlighted step contains a two-electron arrow that "
					+ "points from an atom to an atom.  An electron-flow arrow "
					+ "pointing from an atom to an atom indicates an electron "
					+ "transfer reaction, and a simultaneous transfer of two "
					+ "electrons is exceedingly rare.  If you are intending "
					+ "to form a new bond, the electron-flow arrow should "
					+ "point to <i>between</i> the two atoms."; 
		case TOO_MANY_OUTER_ELECTRONS:
			fdbkBld.append("Too many electrons are being shared with the "
					+ "highlighted atom");
			if (!oneHighlightObj) fdbkBld.append('s');
			return Utils.toString(fdbkBld, ", leading to products that have ",
					oneHighlightObj ? "an atom" : "atoms",
					" with a total electron count more than the maximum.");
		case VALENCE_ERROR:
			fdbkBld.append("The electron-flow arrows in the highlighted step "
					+ "lead to the highlighted atom");
			if (!oneHighlightObj) fdbkBld.append('s');
			return Utils.toString(fdbkBld, " having a valence error. ");
		case RULE_VIOLATION:
			break;
		default:
			Utils.alwaysPrint("MechError: unknown errorNumber ", errorNumber,
					"; no automatically generated feedback.");
			break;
		} // switch kindOfError
		return "";
	} // getErrorFeedback() 

	/** Adds feedback about highlighted flow arrows to the feedback.
	 * @param	fdbkBld	StringBuilder containing the feedback
	 */
	private void addFlowFeedback(StringBuilder fdbkBld) {
		final boolean oneHighlightObj = highlightObjIndices.length == 1;
		Utils.appendTo(fdbkBld, " ACE has noticed ", oneHighlightObj 
					? "an electron flow arrow that is"
					: "electron-flow arrows that are",
				" pointing to ", oneHighlightObj ? "an incipient bond"
					: "incipient bonds");
		if (oneHighlightObj) fdbkBld.append(" and highlighted the "
				+ "atoms that are being connected by that incipient "
				+ "bond");
		Utils.appendTo(fdbkBld, ". Check to make sure that the "
					+ "electron-flow arrow", oneHighlightObj ? " is" : "s are",
				" pointing where you intend to point ",
				oneHighlightObj ? "it" : "them", '.');
	} // addFlowFeedback(StringBuilder)

} // MechError
