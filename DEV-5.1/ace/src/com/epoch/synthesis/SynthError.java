package com.epoch.synthesis;

import com.epoch.synthesis.synthConstants.SynthConstants;
import com.epoch.utils.Utils;

/** Contains the results of a failure of a synthesis operation. */
public class SynthError extends Exception implements SynthConstants {

	private void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Constant that all exceptions have. */
	private static final long serialVersionUID = 1L;

	//----------------------------------------------------------------------
	//							members
	//----------------------------------------------------------------------
	/** MRV of response modified with color to highlight an error. */
	transient public String modifiedResponse = null;
	/** Products calculated from an incorrect synthetic step. */
	transient public String calcdProds = null;
	/** Whether the error was a verification error that
	 * prevented analysis of the synthesis. */
	transient public boolean verificationError = false;
	/** Indicates what was the nature of the error.  Used to generate automatic
	 * feedback. */
	transient public int errorNumber = UNCHECKED;

	//----------------------------------------------------------------------
	//							constructors
	//----------------------------------------------------------------------
	/** Constructor.  */
	public SynthError() {
		// intentionally empty
	}

	/** Constructor. 
	 * @param	errorNum	number of the error
	 */
	public SynthError(int errorNum) {
		errorNumber = errorNum;
	} // SynthError(int)

	/** Constructor. 
	 * @param	message	a message
	 */
	public SynthError(String message) {
		super(message);
		debugPrint(message);
	} // SynthError(String)

	/** Constructor. 
	 * @param	message	a message
	 * @param	errorNum	number of the error
	 */
	public SynthError(String message, int errorNum) {
		super(message);
		errorNumber = errorNum;
	} // SynthError(String, int)

	/** Constructor. 
	 * @param	errorNum	number of the error
	 * @param	calcdProds	products calculated from incorrect electron-flow
	 * arrows
	 */
	public SynthError(int errorNum, String calcdProds) {
		this.calcdProds = calcdProds;
		errorNumber = errorNum;
	} // SynthError(int, String)

	/** Constructor. 
	 * @param	message	a message
	 * @param	errorNum	number of the error
	 * @param	calcdProds	products calculated from incorrect electron-flow
	 * arrows
	 */
	public SynthError(String message, int errorNum, String calcdProds) {
		super(message);
		this.calcdProds = calcdProds;
		errorNumber = errorNum;
	} // SynthError(String, int, String)

	/** Constructor. 
	 * @param	className	name of class that generated the error
	 * @param	message	a message
	 */
	public SynthError(String className, String message) {
		super(message);
		debugPrint(className, ": ", message);
	} // SynthError(String, String)

	/** Constructor. 
	 * @param	className	name of class that generated the error
	 * @param	message	a message
	 * @param	modResponse	MRV of response modified with color to highlight 
	 * the error
	 */
	public SynthError(String className, String message, String modResponse) {
		super(message);
		modifiedResponse = modResponse;
		debugPrint(className, ": ", message);
	} // SynthError(String, String, String)

	/** Constructor. 
	 * @param	message	a message
	 * @param	isVerifyError	whether the error was a verification error that
	 * prevented analysis of the synthesis
	 */
	public SynthError(String message, boolean isVerifyError) {
		super(message);
		verificationError = isVerifyError;
		debugPrint(message); 
	} // SynthError(String, boolean)

	/** Constructor. 
	 * @param	className	name of class that generated the error
	 * @param	message	a message
	 * @param	isVerifyError	whether the error was a verification error that
	 * prevented analysis of the synthesis
	 */
	public SynthError(String className, String message, boolean isVerifyError) {
		super(message);
		verificationError = isVerifyError;
		debugPrint(className, ": ", message);
	} // SynthError(String, String, boolean)

	/** Constructor. 
	 * @param	className	name of class that generated the error
	 * @param	message	a message
	 * @param	modResponse	MRV of response modified with color to highlight 
	 * the error
	 * @param	isVerifyError	whether the error was a verification error that
	 * prevented analysis of the synthesis
	 */
	public SynthError(String className, String message, String modResponse,
			boolean isVerifyError) {
		super(message);
		verificationError = isVerifyError;
		modifiedResponse = modResponse;
		debugPrint(className, ": ", message);
	} // SynthError(String, String, String, boolean)

	/** Constructor. 
	 * @param	className	name of class that generated the error
	 * @param	message	a message
	 * @param	isVerifyError	whether the error was a verification error that
	 * prevented analysis of the synthesis
	* @param	variablePart	a string to substitute for part of the message
	 */
	public SynthError(String className, String message, boolean isVerifyError, 
			String variablePart) {
		super(message);
		verificationError = isVerifyError;
		calcdProds = variablePart;
		// only one case uses variable phrase: atom symbol will replace ***C***
		debugPrint(className, ": ", 
				message.replaceAll(STARS_REGEX + "C" + STARS_REGEX, variablePart));	  
	} // SynthError(String, String, boolean, String)

	//----------------------------------------------------------------------
	//							getErrorFeedback
	//----------------------------------------------------------------------
	/** Gets automatically generated feedback for various kinds of errors
	 * that a student might have made. 
	 * @return	automatically generated feedback 
	 */
	public String getErrorFeedback() {
		switch (errorNumber) {
		case NO_PRODS_IN_NEXT_STAGE:
			return "The reaction of the highlighted stage produces "
				+ "one or more products, but not any that appear "
				+ "in the subsequent stage.";
		case CONTAINS_IMPERMISSIBLE_SM:
			return "The highlighted stage contains a compound that "
				+ "is neither produced by the reactions of the "
				+ "previous stage or stages, nor has the "
				+ "characteristics of a permissible starting "
				+ "material for this synthesis.";
		case START_STAGE_HAS_IMPERMISSIBLE_SM:
			return "The highlighted stage contains a compound that "
				+ "does not have the "
				+ "characteristics of a permissible starting "
				+ "material for this synthesis.";
		case LAST_NOT_RXN_PRODUCT:
			return "The highlighted stage contains a compound that "
				+ "is not produced by the reactions of the "
				+ "previous stage or stages.";
		case SEARCH_EXCEPTION:
			return "ACE can't determine whether your starting "
				+ "materials are permissible.  Please report this "
				+ "error to the programmers.";
		case NO_RXN_PRODUCTS:
			return "The reaction of the highlighted stage gives no "
				+ "products."; 
		case WRONG_ENANTIOMER:
			return "The reaction of the highlighted stage produces an "
				+ "enantiomer of at least one compound in the subsequent "
				+ "stage, but not the compound itself.";
		case WRONG_DIASTEREOMER:
			return "The reaction of the highlighted stage produces a "
				+ "diastereomer of at least one compound in the "
				+ "subsequent stage, but not the compound itself.";
		case MINOR_PRODUCT:
			return "The reaction of the highlighted stage produces a "
				+ "compound that appears in the subsequent stage, but this "
				+ "compound is only a minor product of the reaction.";
		case UNSPECIFIED_STEREOISOMERS:
			return "The highlighted stage contains a compound that has "
				+ "several different (unspecified) stereoisomers, some "
				+ "of which are produced in a previous step, and some of "
				+ "which are not.  Be more specific about which particular "
				+ "stereoisomer is formed."; 
		case UNSELECTIVE: 
			return "The reaction of the highlighted stage produces a "
				+ "compound found in the subsequent stage, but it also "
				+ "produces other compounds in equal abundance, so "
				+ "the reaction is unselective.";
		case UNDIASTEREOSELECTIVE_NOT_SHOWN: 
			return "The reaction of the highlighted stage produces a "
				+ "compound found in the subsequent stage, but it also "
				+ "produces other diastereomers (which you have not "
				+ "shown) in equal abundance, so the reaction is not "
				+ "diastereoselective.";
		case UNDIASTEREOSELECTIVE_NOT_DISTING: 
			return "The reaction of the highlighted stage produces a "
				+ "compound found in the subsequent stage, but this "
				+ "compound exists as multiple diastereomers, so the "
				+ "reaction is not diastereoselective.";
		case UNDIASTEREOSELECTIVE_SHOWN:
			return "The reaction of the highlighted stage produces "
				+ "multiple diastereomers, as you have shown, so the "
				+ "reaction is not diastereoselective.";
		case UNENANTIOSELECTIVE_NOT_SHOWN: 
			return "The reaction of the highlighted stage produces a "
				+ "compound found in the subsequent stage, but it also "
				+ "produces its enantiomer (which you have not "
				+ "shown), so the reaction is not enantioselective, "
				+ "as it is supposed to be.";
		case UNENANTIOSELECTIVE_NOT_DISTING: 
			return "The reaction of the highlighted stage produces a "
				+ "compound found in the subsequent stage, but this "
				+ "compound is racemic, so the reaction is not "
				+ "enantioselective, as it is supposed to be.";
		case UNENANTIOSELECTIVE_SHOWN:
			return "The reaction of the highlighted stage produces "
				+ "both enantiomers, as you have shown, so the "
				+ "reaction is not enantioselective, as it is "
				+ "supposed to be.";
		case USE_MENU: 
			return "The highlighted stage contains a compound, "
				+ "***AlCl3***, that you should not write as one "
				+ "of the reagents, but should choose from the "
				+ "reaction conditions menu.";
		case TOO_MANY_REACTANTS: 
			return "The highlighted stage contains too many reactants "
				+ "for the reaction condition you chose.  Try "
				+ "separating this single synthetic step into two "
				+ "or more synthetic steps executed sequentially.";
		case BAD_DEFINITION:
			return "ACE could not understand the definition of the "
				+ "reaction that you chose for the highlighted step. "
				+ "Please report this error to the programmers.";
		case IS_RXN:
			break;
		case BAD_SM:
			// calcdProds is SMILESofOffendingCpd [tab] badSMName
			return "The highlighted stage contains a "
				+ "***cyclopentadienone***; such compounds are too "
				+ "unstable to use as starting materials.";
		default:
			Utils.alwaysPrint("SynthError: unknown kindOfError ", errorNumber);
			break;
		} // switch kindOfError
		return "";
	} // getErrorFeedback() 

} // SynthError
