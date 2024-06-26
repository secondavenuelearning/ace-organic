package com.epoch.responses;

import static com.epoch.responses.respConstants.ResponseConstants.*;
import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.marvin.io.MolExportException;
import chemaxon.struc.MDocument;
import chemaxon.struc.Molecule;
import com.epoch.chem.ChemUtils;
import com.epoch.chem.Formula;
import com.epoch.chem.FormulaException;
import com.epoch.chem.MolString;
import com.epoch.chem.StereoFunctions;
import com.epoch.chem.WedgeException;
import com.epoch.constants.FormatConstants;
import com.epoch.energyDiagrams.OED;
import com.epoch.energyDiagrams.RCD;
import com.epoch.evals.EvalResult;
import com.epoch.exceptions.ParameterException;
import com.epoch.exceptions.ValenceException;
import com.epoch.exceptions.VerifyException;
import com.epoch.genericQTypes.TableQ;
import com.epoch.genericQTypes.Choice;
import com.epoch.genericQTypes.Rank;
import com.epoch.genericQTypes.ChooseExplain;
import com.epoch.genericQTypes.Numeric;
import com.epoch.genericQTypes.ClickImage;
import com.epoch.genericQTypes.Logic;
import com.epoch.lewis.LewisMolecule;
import com.epoch.mechanisms.Mechanism;
import com.epoch.mechanisms.MechError;
import com.epoch.physics.DrawVectors;
import com.epoch.physics.Equations;
import com.epoch.qBank.QDatum;
import com.epoch.qBank.Question;
import com.epoch.qBank.qBankConstants.QuestionConstants;
import com.epoch.synthesis.Synthesis;
import com.epoch.synthesis.SynthError;
import com.epoch.translations.PhraseTransln;
import com.epoch.utils.Utils;

/** Stores the student's response and parsed &amp; processed versions of it.
 * The purpose is to avoid repeated execution of the same processing steps for
 * different evaluators.
 */
public class Response implements FormatConstants, QuestionConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Student's response; often an MRV file from Marvin
	 * representing a molecule or a MOL string. */
	transient public String unmodified = null;
	/** Student's response converted to a JChem Molecule. */
	transient public Molecule moleculeObj = null;
	/** Student's response converted to a JChem Molecule and normalized;
	 * populated if and when needed. */
	public Molecule normalized = null;
	/** The molecules of the shortcut groups used to replace R in this student's
	 * view of an R-group question. */
	transient public Molecule[] rGroupMols = new Molecule[0];
	/** Values used to replace variables in the question statement of a numeric
	 * question. */
	transient public String[] values = new String[0];
	/** A response parsed into an Object such as Synthesis or Rank. */
	transient public Object parsedResp = null;
	/** Stores the error message for display on front end if response fails to
	 * initialize. */
	transient public EvalResult initializationErrorResult = new EvalResult();
	/** Whether initialization was successful. */
	transient public boolean initialized = false;
	/** Whether ACE should include calculated products in the feedback to a
	 * synthesis question. */
	transient public boolean maySeeSynthCalcProds = true;
	/** Student's response with coloring removed; stored in
	 * EvalResult.modifiedResponse for display to students if no other colorable
	 * error occurs. */
	transient private String uncolored = null;
	/** Non-English languages of the user submitting this response, in order of
	 * preference. */
	transient private String[] userLangs = new String[0];
	/** Why a response could not be initialized. */
	transient private int errorReason = NONE;
	
	/** Constructor called from <code>Question.parseResponse()</code>.
	 * @param	qType	type of the question
	 * @param	qFlags	flags of the question
	 * @param	qData	question data of the question; required for energy
	 * diagrams and numeric responses
	 * @param	evalResult	contains the student's response from the front end;
	 * uncolored version of student's response may be stored in the
	 * modifiedResponse field
	 * @param	substns	R groups or variable values associated with this user's 
	 * instance of the question; may be null or empty
	 * @param	userLangs	languages chosen by this user in order of preference
	 * @param	maySee	whether the user should be permitted to
	 * see calculated synthesis products in the feedback
	 */
	public Response(int qType, long qFlags, QDatum[] qData, 
			EvalResult evalResult, Object[] substns, String[] userLangs, 
			boolean maySee) {
		unmodified = evalResult.lastResponse;
		this.userLangs = userLangs;
		debugPrint("Response(7): parsing:\n", unmodified);
		initializationErrorResult.lastResponse = unmodified;
		initialized = initializeResponse(qType, qFlags, qData);
		if (Question.isNumeric(qType)) {
			values = (String[]) substns;
		} else {
			rGroupMols = (Molecule[]) substns;
		} // if question type
		maySeeSynthCalcProds = maySee;
		if (uncolored != null) evalResult.lastResponse = uncolored;
	} // Response(int, long, QDatum[], EvalResult, Object[], String[], boolean)

	/** Constructor called from <code>Question.firstEvaluatorMatching()</code>.
	 * @param	qType	type of the question
	 * @param	qFlags	flags of the question
	 * @param	qData	question data of the question; required for energy
	 * diagrams and numeric responses
	 * @param	unmodified	the student's original response encoded as a string
	 */
	public Response(int qType, long qFlags, QDatum[] qData, String unmodified) {
		this.unmodified = unmodified;
		debugPrint("Response(4): parsing:\n", unmodified);
		initializationErrorResult.lastResponse = unmodified;
		initialized = initializeResponse(qType, qFlags, qData);
	} // Response(int, long, QDatum[], String)

	/** Constructor called from <code>evals/impl/genericQEvals/tableEvals/Table*</code> 
	 * and <code>evals/impl/genericQEvals/clickEvals/Click*</code> when it is clear
	 * whether a value should be evaluated as a number or a String.
	 * @param	qType	type of the question
	 * @param	unmodified	the student's original response encoded as a string
	 */
	public Response(int qType, String unmodified) {
		this.unmodified = unmodified;
		debugPrint("Response(2): parsing:\n", unmodified);
		initializationErrorResult.lastResponse = unmodified;
		initialized = initializeResponse(qType, 0L, null);
	} // Response(int, String)

	/** Constructor called from <code>evals/impl/chemEvals/Is2DChair, 
	 * synthesis/SynthStarterRule,
	 * synthesis/SingleRxnSolver,
	 * evals/impl/chemEvals/mechEvals/MechProdStartProps.java</code>, and public pages.
	 * @param	mol	a response molecule
	 */
	public Response(Molecule mol) {
		moleculeObj = mol;
		initialized = true;
	} // Response(Molecule)

	/** Converts response Strings to molecules, parses and stores mechanisms
	 * and syntheses.
	 * @param	qType	type of the question
	 * @param	qFlags	flags of the question
	 * @param	qData	question data of the question; required for energy
	 * diagrams and numeric responses
	 * @return	true if OK; populates initializationErrorResult.feedback if not
	 */
	private boolean initializeResponse(int qType, long qFlags, QDatum[] qData) {
		final String SELF = "Response.initialize: ";
		debugPrint(SELF + "starting.");
		final boolean isMarvin = Question.isMarvin(qType);
		final boolean isMechanism = Question.isMechanism(qType);
		final boolean isSynthesis = Question.isSynthesis(qType);
		if (isMarvin || isMechanism || isSynthesis) {
			try {
				moleculeObj = getMolResponse(unmodified);
				if (moleculeObj == null) {
					errorReason = FORMAT;
					return false;
				} // if molecule could not be imported
				ChemUtils.checkValence(moleculeObj);
				StereoFunctions.checkForWedgesFromNonstereo(moleculeObj,
						Question.disallowSuperfluousWedges(qFlags), 
						ChemUtils.getWhetherFromMarvinJS(moleculeObj));
				if (isMechanism) {
					final Mechanism mechanism = new Mechanism(unmodified);
					final MechError mechError = mechanism.errorObject;
					if (mechError != null) {
						final String englFeedback = mechError.getMessage();
						initializationErrorResult.feedback =
								translate(MECHANISM, englFeedback, 
									mechError.calcdProds,
									mechError.verificationError);
						initializationErrorResult.modifiedResponse =
								mechError.modifiedResponse;
						errorReason = PARSING;
						return false;
					} else {
						parsedResp = mechanism;
						uncolored = toString(mechanism.getMDocCopy());
					} // if there's an error in parsing mechanism
				} else if (isSynthesis) {
					final Synthesis synthesis = new Synthesis(unmodified,
							!Synthesis.EMPTY_BOX_OK);
					final SynthError synthError = synthesis.errorObject;
					if (synthError != null) {
						final int errorNum = synthError.errorNumber;
						final String englFeedback = synthError.getMessage();
						initializationErrorResult.feedback =
								translate(SYNTHESIS, englFeedback, 
									synthError.calcdProds,
									synthError.verificationError
										|| errorNum == SynthError.USE_MENU);
						initializationErrorResult.modifiedResponse =
								synthError.modifiedResponse;
						errorReason = PARSING;
						return false;
					} else {
						parsedResp = synthesis;
						uncolored = toString(synthesis.getMDocCopy());
					} // if there's an error in parsing synthesis
				} // if question type
				if (!isMarvin
						|| !Question.showMapping(qFlags)
						|| Question.usesSubstns(qFlags)
						|| Question.is3D(qFlags)) {
					ungroup(moleculeObj);
				} else {
					debugPrint(SELF + "not ungrouping shortcut groups in ", 
							moleculeObj, " because question is mapping.");
				} // if should ungroup
			} catch (ValenceException e) {
				initializationErrorResult.grade = 0;
				initializationErrorResult.feedback =
						translate(e.getMessage());
				final int[] badAtomNums = e.getBadAtomNums();
				if (!Utils.isEmpty(badAtomNums)) {
					for (final int badAtomNum : badAtomNums) {
						moleculeObj.getAtom(badAtomNum - 1).setSelected(true);
					} // for each bad atom
					initializationErrorResult.modifiedResponse =
							MolString.toString(moleculeObj, MRV);
				} // if bad atoms have been identified
				return false;
			} catch (WedgeException e) {
				initializationErrorResult.grade = 0;
				final int atomNum = e.atomNum;
				initializationErrorResult.feedback = (atomNum < 0
						? translate(e.getMessage())
						: translate(e.getMessage(), atomNum)); 
				errorReason = PARSING;
				return false;
			} catch (Exception e) {
				return formatError(e, PRINT_STACK, Utils.toString(
						isSynthesis ? "synthesis" : 
						isMechanism ? "mechanism" : "response", ". "));
			} // try to parse response
		} else if (Question.isLewis(qType)) {
			// create new Lewis molecule
			try {
				final LewisMolecule lewis = new LewisMolecule(unmodified);
				parsedResp = lewis;
				moleculeObj = lewis.getMolecule();
				lewis.unhighlight();
				uncolored = lewis.toString();
			} catch (Exception e) {
				Utils.alwaysPrint(SELF + "couldn't convert unmodified into "
						+ "Lewis molecule.");
				final String[] feedback = new String[] {
						CANNOT + "response.",
						"Please report this error to the programmers."};
				translate(feedback);
				initializationErrorResult.feedback = Utils.toString(
						feedback[0], ' ', feedback[1], BR, e.getMessage());
				e.printStackTrace();
				errorReason = FORMAT;
				return false;
			}
			// also import into regular Molecule
			// no shortcut groups or valence checks
			moleculeObj = getMolResponse(unmodified);
			if (moleculeObj == null) {
				errorReason = FORMAT;
				return false;
			}
		} else if (Question.isText(qType)) {
			parsedResp = Utils.condenseWhitespace(unmodified);
		} else if (Question.isFormula(qType)) {
			try {
				final Formula formula = new Formula(unmodified);
				parsedResp = formula;
			} catch (FormulaException e) {
				final String[] feedback = new String[] {
						CANNOT + "response.", e.getMessage()};
				translate(feedback);
				initializationErrorResult.feedback = 
						Utils.toString(feedback[0], ' ', feedback[1]);
				e.printStackTrace();
				errorReason = PARSING;
				return false;
			} // try
		} else if (Question.isTable(qType)) {
			final TableQ table = new TableQ(unmodified);
			parsedResp = table;
		} else if (Question.isOED(qType)) {
			try {
				if (Utils.isEmpty(qData)) throw new ParameterException(
						"There are no question data.");
				final OED oed = new OED(qData);
				// ignore Jlint complaint about line above.  Raphael 11/2010
				oed.setOrbitals(unmodified);
				parsedResp = oed;
			} catch (ParameterException e) {
				return formatError(e, !PRINT_STACK, "orbital energy diagram. ");
			} // try
		} else if (Question.isRCD(qType)) {
			try {
				if (Utils.isEmpty(qData)) throw new ParameterException(
						"There are no question data.");
				final RCD rcd = new RCD(qData);
				// ignore Jlint complaint about line above.  Raphael 11/2010
				rcd.setStates(unmodified);
				if (rcd.hasGap()) {
					initializationErrorResult.feedback = "Your reaction "
							+ "coordinate diagram must not have an unoccupied "
							+ "column between two occupied ones.";
					return false;
				} // if there's a gap in the diagram
				parsedResp = rcd;
			} catch (ParameterException e) {
				return formatError(e, !PRINT_STACK, "reaction coordinate diagram. ");
			} catch (VerifyException e) {
				formatError(e, !PRINT_STACK, "reaction coordinate diagram. ");
				errorReason = PARSING;
				return false;
			} // try
		} else if (Question.isRank(qType)) {
			try {
				final Rank rankResp = new Rank(unmodified);
				parsedResp = rankResp;
			} catch (NumberFormatException e) {
				return formatError(e, PRINT_STACK, "response. ");
			} // try
		} else if (Question.isChoice(qType) || Question.isFillBlank(qType)) {
			try {
				final Choice choiceResp = new Choice(unmodified);
				parsedResp = choiceResp;
			} catch (NumberFormatException e) {
				return formatError(e, PRINT_STACK, "response. ");
			} // try
		} else if (Question.isChooseExplain(qType)) {
			try {
				final ChooseExplain chooseExplainResp = 
						new ChooseExplain(unmodified);
				parsedResp = chooseExplainResp;
			} catch (ParameterException e) {
				return formatError(e, PRINT_STACK, "response. ");
			} catch (NumberFormatException e) {
				return formatError(e, PRINT_STACK, "response. ");
			} // try
		} else if (Question.isNumeric(qType)) {
			try {
				final Numeric numberResp = 
						new Numeric(unmodified, qData);
				parsedResp = numberResp;
			} catch (NumberFormatException e) {
				return formatError(e, PRINT_STACK, "response. ");
			} catch (ParameterException e) {
				return formatError(e, PRINT_STACK, "response. ");
			} // try
		} else if (Question.isClickableImage(qType)) {
			final ClickImage clickResp = new ClickImage(unmodified);
			parsedResp = clickResp;
		} else if (Question.isDrawVectors(qType)) {
			try {
				final DrawVectors vectorsResp = new DrawVectors(unmodified);
				parsedResp = vectorsResp;
			} catch (ParameterException e) {
				return formatError(e, PRINT_STACK, "response. ");
			} // try
		} else if (Question.isLogicalStatements(qType)) {
			try {
				final Logic logicResp = new Logic(unmodified);
				parsedResp = logicResp;
			} catch (ParameterException e) {
				return formatError(e, PRINT_STACK, "response. ");
			} // try
		} else if (Question.isEquations(qType)) {
			try {
				final Equations eqnsResp = new Equations(unmodified);
				parsedResp = eqnsResp;
			} catch (ParameterException e) {
				return formatError(e, PRINT_STACK, "response. ");
			} // try
		} else {
			Utils.alwaysPrint(SELF + "UNRECOGNIZED QUESTION TYPE: ",
					qType, "; report this problem to the programmer, please.");
			final String[] feedback = new String[] {
					"ACE doesn't recognize the question type.",
					"Please report this error to the programmers."};
			translate(feedback);
			initializationErrorResult.feedback = 
					Utils.toString(feedback[0], ' ', feedback[1]);
			return false;
		} // if type
		debugPrint(SELF + "parse successful.");
		return true; // success!
	} // initializeResponse(int, long, QDatum[])

	/** Reports a format error.
	 * @param	e	an exception
	 * @param	printStack	whether to print the stack trace
	 * @param	respName	name of the response
	 * @return	false
	 */
	private boolean formatError(Exception e, boolean printStack, 
			String respName) {
		initializationErrorResult.feedback = 
				Utils.toString(translate(CANNOT + respName), e.getMessage());
		if (printStack) e.printStackTrace();
		errorReason = FORMAT;
		return false;
	} // formatError(Exception, boolean, String)

	/** Converts response String to Molecule.
	 * @param	responseMRV	the response in MRV format
	 * @return	the response as a Molecule
	 */
	private Molecule getMolResponse(String responseMRV) {
		try {
			return MolImporter.importMol(responseMRV);
		} catch (MolFormatException e) {
			Utils.alwaysPrint("Response.getMolResponse: error in "
					+ "importing Molecule from response string:\n",
					responseMRV);
			e.printStackTrace();
			initializationErrorResult.grade = 0;
			final String[] feedback = new String[] {
					CANNOT + "response.",
					"Please report this error to the programmers."};
			translate(feedback);
			initializationErrorResult.feedback = Utils.toString(
					feedback[0], ' ', feedback[1], BR, e.getMessage());
			return null;
		} // try importing Molecule
	} // getMolResponse(String)

	/** Removes shortcut groups from a response molecule.
	 * @param	mol	the molecule
	 */
	private void ungroup(Molecule mol) {
		mol.ungroupSgroups(SHORTCUT_GROUPS);
		debugPrint("shortcut groups removed.");
	} // ungroup(Molecule)

	/** Translates feedback, perhaps substituting a variable 
	 * part of a phrase into the feedback.
	 * @param	englFeedback	feedback to be translated
	 * @param	variablePart	number to be substituted into the translated 
	 * feedback
	 * @return	translated feedback
	 */
	private String translate(String englFeedback, int variablePart) {
		final String translated = translate(englFeedback);
		final String[] feedbackPieces = 
				translated.split(PhraseTransln.STARS_REGEX);
		return (feedbackPieces.length <= 1 ? translated
				// splice variable part into feedback
				: Utils.toString(feedbackPieces[0], variablePart,
					feedbackPieces[2]));
	} // translate(String, int)

	/** Translates feedback, perhaps substituting a variable part of a phrase 
	 * into the feedback.
	 * @param	qType	mechanism or synthesis
	 * @param	englFeedback	feedback to be translated
	 * @param	variablePart	phrase to be substituted into the translated 
	 * feedback
	 * @param	spliceable	whether we might expect to substitute 
	 * a variable part: if there's a verification error, or if it's a synthesis 
	 * response that uses reagents that should come off the reagent menu
	 * @return	translated feedback
	 */
	private String translate(long qType, String englFeedback, 
			String variablePart, boolean spliceable) {
		final String[] feedback = new String[] {
				Utils.among(qType, MECHANISM, SYNTHESIS)
					? Utils.toString("An error occurred when ACE was trying "
						+ "to interpret your ", 
						qType == MECHANISM ? "mechanism." 
						: qType == SYNTHESIS ? "synthesis."
						: Utils.toString("response of type ", qType, "."))
					: "",
				englFeedback};
		translate(feedback);
		final String[] feedback1Pieces = 
				feedback[1].split(PhraseTransln.STARS_REGEX);
		if (spliceable && feedback1Pieces.length > 1) {
			// splice variable part into feedback
			feedback[1] = Utils.toString(feedback1Pieces[0],
					variablePart, feedback1Pieces[2]);
		} // if spliceable and there are *** in translated feedback
		return Utils.toString(feedback[0], ' ', feedback[1]);
	} // translate(long, String, String, boolean)

	/** Translates feedback into one of the user's preferred languages.
	 * @param	msg	feedback to translate
	 * @return	translated feedback, or English if no appropriate translation is
	 * available
	 */
	private String translate(String msg) {
		return PhraseTransln.translate(msg, userLangs);
	} // translate(String)

	/** Translates one or more pieces of feedback into one of the user's
	 * preferred languages.
	 * @param	msgs	pieces of feedback to translate
	 */
	private void translate(String[] msgs) {
		PhraseTransln.translate(msgs, userLangs);
	} // translate(String[])

	/** Returns the MRV of an MDocument.
	 * @param	mDoc	the MDocument
	 * @return	the MRV
	 */
	private String toString(MDocument mDoc) {
		try {
			return MolString.toString(mDoc, MRV);
		} catch (MolExportException e) {
			debugPrint("MolExportException");
		} // try
		return null;
	} // toString(MDocument)

	/** Gets whether initialization failed because the format of the response
	 * was not recognized.
	 * @return	true if initialization failed because the format of the response
	 * was not recognized
	 */
	public boolean badFormat() {
		return errorReason == FORMAT;
	} // badFormat()

	/** Gets whether initialization failed because the response could not be
	 * parsed.
	 * @return	true if initialization failed because the response could not be
	 * parsed
	 */
	public boolean badParsing() {
		return errorReason == PARSING;
	} // badParsing()

	/** Gets the parsed Mechanism.
	 * @return	the parsed Mechanism, or null if it is not a mechanism
	 */
	public Mechanism getMechanism() {
		return (parsedResp instanceof Mechanism
				? (Mechanism) parsedResp : null);
	} // getMechanism()

	/** Gets the parsed Synthesis.
	 * @return	the parsed Synthesis, or null if it is not a synthesis
	 */
	public Synthesis getSynthesis() {
		return (parsedResp instanceof Synthesis
				? (Synthesis) parsedResp : null);
	} // getSynthesis()

} // Response
