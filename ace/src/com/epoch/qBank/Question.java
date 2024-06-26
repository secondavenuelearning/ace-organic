package com.epoch.qBank;

import chemaxon.struc.Molecule;
import com.epoch.AppConfig;
import com.epoch.chem.chemConstants.ChemConstants;
import com.epoch.chem.MolString;
import com.epoch.constants.AuthorConstants;
import com.epoch.courseware.User;
import com.epoch.evals.EvalResult;
import com.epoch.evals.Evaluator;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.Subevaluator;
import com.epoch.evals.evalConstants.EvalResultConstants;
import com.epoch.evals.evalConstants.OneEvalConstants;
import com.epoch.exceptions.ParameterException;
import com.epoch.exceptions.ParserException;
import com.epoch.exceptions.ResponseFormatException;
import com.epoch.exceptions.ResponseParseException;
import com.epoch.genericQTypes.Choice;
import com.epoch.genericQTypes.TableQ;
import com.epoch.qBank.qBankConstants.QuestionConstants;
import com.epoch.responses.Response;
import com.epoch.substns.SubstnUtils;
import com.epoch.substns.substnConstants.SubstnConstants;
import com.epoch.translations.PhraseTransln;
import com.epoch.translations.QSetAllTranslns;
import com.epoch.translations.QTranslns;
import com.epoch.translations.TranslnsMap;
import com.epoch.translations.translnConstants.TranslnConstants;
import com.epoch.synthesis.Synthesis;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import com.epoch.xmlparser.XMLUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** An ACE question, formerly called a problem.  */
public class Question implements AuthorConstants, ChemConstants, 
		EvalResultConstants, OneEvalConstants, QuestionConstants,
		SubstnConstants, TranslnConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Distinguishes this question from all others. */
	final private int qId;
	/** Unique ID of QSet in which this Q resides.  */
	private int qSetId;
	/** Author of the question; MASTER_AUTHOR for master database questions. */
	private String authorId;
	/** Type of the question.  */
	private int qType;
	/** Flags of the question.  */
	private long qFlags;
	/** Stored but not current used for anything; will eventually be used to sort
	 * questions by their characteristics.  */
	private String keywords;
	/** The question statement; may or may not include the common question
	 * statement (or header) of the question set to which this question belongs,
	 * depending on the database call when the question was loaded.  */
	private String statement;
	/** A copy of the question statement, used to see if the question statement
	 * has changed before keeping/erasing its translations. */
	public String origStatement;
	/** ID used for translations of the question statement. */
	private int phraseId;
	/** The question figure or figures; may be empty.
	 * @see Figure
	 */
	final private List<Figure> figures = new ArrayList<Figure>();
	/** Contains two lists of data that apply across evaluators.  List 1:
	 * permissible starting materials (multistep syntheses questions);
	 * items to rank or from which to choose (rank, multiple choice, 
	 * fill-in-the-blank); table size and captions, preload table (table 
	 * questions); color of marks (clickable image, draw-vectors); unit names 
	 * (numeric); constants, initial equations, excluded unit names (equations).
	 * List 2: R-group classes from which ACE may choose a specific R group to 
	 * instantiate a generic R group in the question figure (R-group questions).
 	 * @see QDatum
	 */
	final private List<ArrayList<QDatum>> qData = new ArrayList<ArrayList<QDatum>>();
	/** All evaluators, simple or complex, for this question.
	 * @see Evaluator
	 */
	final private List<Evaluator> allEvaluators = new ArrayList<Evaluator>();
	/** Textbook from which this question derives. */
	private String book;
	/** Chapter number of the textbook from which this question derives, or the
	 * initials of the author of the question if the textbook is "Other". */
	private String chapter;
	/** Number of the question in the chapter and textbook.  */
	private String remarks;
	/** Error message to the author if the question could not be saved.
	 * Also used to store the common Q statement for display in preview. */
	transient public String miscMessage;
	/** Serial number of the question within a set.  Used only temporarily in
	 * QuestionRead. */
	private int serialNo;
	/** Status of a locally authored question (SAME, CHANGED, or NEW).  */
	transient private int editType;
	/** Indicates if question could not properly be downloaded from database. */
	private boolean corrupted = false;
	/** Indicates if this question is from the master database. */
	transient private boolean masterQ = true;
	private static final int[] noInts = {};

/* *************** Constructors ******************/

	/** Creates a new question, ready to have details filled in.
	 */
	public Question() {
		qId = 0;
		keywords = "";
		statement = "";
		phraseId = 0;
		for (int listNum = 0; listNum < NUM_QDATA_LISTS; listNum++) {
			qData.add(new ArrayList<QDatum>());
		} // for each list to be added to qData
		qType = MARVIN; // fresh question
		qFlags = 0L; // fresh question
		serialNo = 0;
	} // Question()

	/** Creates a new question, ready to have details filled in.
	 * Only the qId has been set.
	 * @param dbid	the desired question qId
	 */
	public Question(int dbid) {
		qId = dbid;
		statement = "";
		origStatement = "";
		phraseId = 0;
		for (int listNum = 0; listNum < NUM_QDATA_LISTS; listNum++) {
			qData.add(new ArrayList<QDatum>());
		} // for each list to be added to qData
		miscMessage = "";
		qType = MARVIN; // fresh question
		qFlags = 0L; // fresh question
		serialNo = 0;
	} // Question(int)

	/** Copy constructor; gives new ID of 0.
	 * @param copy	question to be copied
	 */
	public Question(Question copy) {
		qId = 0;
		serialNo = 0;
		copyRest(copy);
	} // Question(copy)

	/** Copy constructor; may copy the old ID as well.
	 * @param copy	question to be copied
	 * @param copyId	whether to copy the qId
	 */
	public Question(Question copy, boolean copyId) {
		if (copyId) {
			qId = copy.getQId();
			serialNo = copy.getSerialNo();
		} else {
			qId = 0;
			serialNo = 0;
		} // if making new question
		copyRest(copy);
	} // Question(copy, copyId)

	/** Used by copy constructors.
	 * @param copy	question to be copied
	 */
	private void copyRest(Question copy) {
		qSetId = copy.getQSetId();
		qType = copy.qType;
		qFlags = copy.qFlags;
		corrupted = copy.isCorrupted();
		// Strings
		statement = copy.statement;
		origStatement = statement;
		phraseId = copy.phraseId;
		miscMessage = copy.miscMessage;
		authorId = copy.authorId;
		book = copy.book;
		chapter = copy.chapter;
		remarks = copy.remarks;
		keywords = copy.keywords;
		// complex components
		for (final Figure copyFig : copy.getFigures()) {
			figures.add(new Figure(copyFig));
		} // for each figure
		for (final QDatum[] copyQDataArr : copy.getAllQData()) {
			final ArrayList<QDatum> qdList = new ArrayList<QDatum>();
			for (final QDatum qd : copyQDataArr) {
				qdList.add(qd instanceof EDiagramQDatum
						? new EDiagramQDatum(qd)
						: qd instanceof CaptionsQDatum
						? new CaptionsQDatum(qd) : new QDatum(qd));
			} // for each qDatum in the list
			qData.add(qdList);
		} // for each list to be added to qData
		for (final Evaluator copyEval : copy.getAllEvaluators()) {
			allEvaluators.add(new Evaluator(copyEval));
		} // for each evaluator
	} // copyRest(copy)

/* *************** Get-set methods for simple members ******************/

	/** Gets the unique ID of this question.
	 * @return	unique ID of this question
	 */
	public int getQId()							{ return qId; }
	/** Gets the ID of the author of this question.
	 * @return	ID of the author of this question
	 */
	public String getAuthorId() 				{ return authorId; }
	/** Sets the ID of the author of this question.
	 * @param	theId	ID of the author of this question
	 */
	public void setAuthorId(String theId) 		{ authorId = theId; }
	/** Gets the unique ID of the question set to which this question belongs.
	 * @return	unique ID of the question set to which this question belongs
	 */
	public int getQSetId() 						{ return qSetId; }
	/** Sets the unique ID of the question set to which this question belongs.
	 * @param	theId	unique ID of the question set to which this question belongs
	 */
	public void setQSetId(int theId) 			{ qSetId = theId; }
	/** Gets the serial number of this question within the question set.
	 * @return	the serial number of this question within the question set
	 */
	public int getSerialNo() 					{ return serialNo; }
	/** Sets the serial number of this question within the question set.
	 * @param	num	serial number of this question within the question set
	 */
	public void setSerialNo(int num) 			{ serialNo = num; }
	/** Gets the statement of this question.
	 * @return	statement of this question
	 */
	public String getStatement()				{ return statement; }
	/** Sets the statement of this question.
	 * @param	stmnt	statement of this question
	 */
	public void setStatement(String stmnt) 		{ statement = stmnt; }
	/** Gets the original statement of this question (before editing).
	 * @return	original statement of this question
	 */
	public String getOrigStatement()			{ return origStatement; }
	/** Sets the original statement of this question (before editing).
	 * @param	stmnt	original statement of this question
	 */
	public void setOrigStatement(String stmnt)	{ origStatement = stmnt; }
	/** Gets the phraseId of this question's statement.
	 * @return	phraseId of this question's statement
	 */
	public int getPhraseId()					{ return phraseId; }
	/** Sets the phraseId of this question's statement.
	 * @param	stmntId	phraseId of this question's statement
	 */
	public void setPhraseId(int stmntId) 		{ phraseId = stmntId; }
	/** Gets the keywords of this question.
	 * @return	keywords of this question
	 */
	public String getKeywords() 				{ return keywords; }
	/** Sets the keywords of this question.
	 * @param	keys	keywords of this question
	 */
	public void setKeywords(String keys) 		{ keywords = keys; }
	/** Gets whether this question is locally authored.
	 * @return	true if this question is locally authored
	 */
	public boolean isNew()						{ return editType == NEW; }
	/** Gets whether this question is locally modified.
	 * @return	true if this question is locally modified
	 */
	public boolean isModified()					{ return editType == CHANGED; }
	/** Gets whether this question is master-authored.
	 * @return	true if this question is master-authored
	 */
	public boolean isMaster()					{ return editType == SAME; }
	/** Sets the editing status of this question.
	 * @param	type	editing status of this question
	 */
	public void setEditType(int type) 			{ editType = type; }
	/** Gets whether this question is corrupted.
	 * @return	true if this question is corrupted
	 */
	public boolean isCorrupted()				{ return corrupted; }
	/** Sets that this question is corrupted.  */
	public void setCorrupted()					{ corrupted = true; }
	/** Gets whether this question is a master-authored question.
	 * @return	true if this question is a master-authored question
	 */
	public boolean isMasterQ()					{ return masterQ; }
	/** Sets whether this question is a master-authored question.
	 * @param	mq	whether this question is a master-authored question
	 */
	public void setMasterQ(boolean mq)			{ masterQ = mq; }
	/** Gets the type of this question.
	 * @return	type of this question
	 */
	public int getQType() 						{ return qType; }
	/** Sets the type of this question.
	 * @param	theQType	type of this question
	 */
	public void setQType(int theQType) 			{ qType = theQType; }
	/** Gets the flags of this question.
	 * @return	flags of this question
	 */
	public long getQFlags() 					{ return qFlags; }
	/** Sets the flags of this question.
	 * @param	theQFlags	flags of this question
	 */
	public void setQFlags(long theQFlags) 		{ qFlags = theQFlags; }
	/** Sets the book of this question.
	 * @param	theBook	book of this question
	 */
	public void setBook(String theBook) 		{ book = theBook; }
	/** Sets the chapter of this question.
	 * @param	theChapter	chapter of this question
	 */
	public void setChapter(String theChapter)	{ chapter = theChapter; }
	/** Sets the remarks of this question.
	 * @param	theRemarks	remarks of this question
	 */
	public void setRemarks(String theRemarks)	{ remarks = theRemarks; }
	/** Gets the number of figures in this question.
	 * @return	number of figures in this question
	 */
	public int getNumFigures() 					{ return figures.size(); }
	/** Gets the number of question data in this question.
	 * @param	listNum	0-based number of the list to get
	 * @return	number of question data in this question
	 */
	public int getNumQData(int listNum) 		{ return qData.get(listNum).size(); }
	/** Gets the number of evaluators in this question.
	 * @return	number of evaluators in this question
	 */
	public int getNumEvaluators() 				{ return allEvaluators.size(); }
	/** Gets the question tag constant.
	 * @return	question tag
	 */
	public static String getTag() 				{ return QUESTION_TAG; }

	/** Gets the book of this question.
	 * @return	book of this question
	 */
	public String getBook() {
		return (Utils.isEmpty(book) ? "Other" : book);
	} // getBook()

	/** Gets the chapter of this question.
	 * @return	chapter of this question
	 */
	public String getChapter() {
		return (Utils.isEmpty(chapter) ? "RBG" : chapter);
	} // getChapter()

	/** Gets the remarks of this question.
	 * @return	remarks of this question
	 */
	public String getRemarks() {
		return (Utils.isEmpty(remarks) ? "[None]" : remarks);
	} // getRemarks()

	/** Gets the serial number of the last figure in this question.
	 * @return	serial number of the last figure in this question
	 */
	private int getLastFigureSerialNo() {
		return (figures.isEmpty() ? 0
				: figures.get(getNumFigures() - 1).serialNo);
	} // getLastFigureSerialNo()

	/** Gets if any of the figures of this question are Lewis figures.
	 * @return	true if any of the figures of this question are Lewis figures
	 */
	public boolean hasLewisFigure() {
		for (final Figure fig : figures) if (fig.isLewis()) return true;
		return false;
	} // hasLewisFigure()

	/** Gets if any of the figures of this question are Jmol figures.
	 * @return	true if any of the figures of this question are Jmol figures
	 */
	public boolean hasJmolFigure() {
		for (final Figure fig : figures) if (fig.isJmol()) return true;
		return false;
	} // hasJmolFigure()

	/** Gets the statement of this question for display.
	 * @return	statement of this question for display
	 */
	public String getDisplayStatement() {
		return getDisplayStatement(CONVERT_VARS_TO_VALUES);
	} // getDisplayStatement()

	/** Gets the statement of this question for display.
	 * @param	varsToValues	whether to replace variables with values in
	 * numeric questions
	 * @return	statement of this question for display
	 */
	public String getDisplayStatement(boolean varsToValues) {
		final String SELF = "Question.getDisplayStatement: ";
		if (Utils.isEmpty(statement)) return "";
		String stmt = statement;
		final boolean isNumeric = isNumeric();
		final QDatum[] qDataGen = getQData(GENERAL);
		final QDatum[] qDataSub = getQData(SUBSTNS);
		if (isFillBlank() && !Utils.isEmpty(qDataGen)) {
			final String optsStr = Choice.getOptions(stmt);
			stmt = getDisplayStatement(stmt, new Choice(optsStr));
		} else {
			if (isNumeric && usesSubstns() 
					&& !Utils.isEmpty(qDataSub) && varsToValues) {
				final String[] chosenValues =
						SubstnUtils.chooseSubstnValues(qDataSub, isNumeric);
				stmt = getDisplayStatement(stmt, chosenValues);
			} // if numeric with substitutions
			if (chemFormatting()) stmt = Utils.toDisplay(stmt);
		} // if fillBlank
		return stmt;
	} // getDisplayStatement(boolean)

	/** Gets the statement of this question for display.
	 * @param	stmt	the question statement, perhaps with a header attached
	 * @param	choiceObj	contains a response with ordered options
	 * @return	statement of this question for display
	 */
	public String getDisplayStatement(String stmt, Choice choiceObj) {
		return getDisplayStatement(stmt, choiceObj, !Choice.ADD_MENUS);
	} // getDisplayStatement(String, Choice)

	/** Gets the statement of this question for display.
	 * @param	stmt	the question statement, perhaps with a header attached
	 * @param	choiceObj	contains a response with ordered options
	 * @param	addMenus	when true, put pulldown menus into the statement
	 * @return	statement of this question for display
	 */
	public String getDisplayStatement(String stmt, Choice choiceObj, 
			boolean addMenus) {
		return choiceObj.fillBlankToDisplay(stmt, getQData(GENERAL), 
				addMenus, chemFormatting());
	} // getDisplayStatement(String, Choice, boolean)

	/** Gets the statement of this question for display.
	 * @param	stmt	the question statement, perhaps with a header attached
	 * @param	values	array of values to substitute for variables in the 
	 * statement
	 * @return	statement of this question for display
	 */
	public String getDisplayStatement(String stmt, String[] values) {
		return SubstnUtils.substituteValues(stmt, values, !NUMERIC_VALUE);
	} // getDisplayStatement(String, String[])

/* *************** Question types and flags: dynamic methods  ******************/

	/** Gets a string describing the type of this question.
	 * @return	a description of the type of this question
	 */
	public String getQTypeDescription() {
		return getQTypeDescription(qType, qFlags, new User());
	} // getQTypeDescription()

	/** Gets a string describing the type of this question.
	 * @param	user	a user
	 * @return	a description of the type of this question
	 */
	public String getQTypeDescription(User user) {
		return getQTypeDescription(qType, qFlags, user);
	} // getQTypeDescription(User)

	/** Gets whether the question is the given major question type.
	 * @param	theQType	the type being queried
	 * @return	true if the major type of the question is the queried type
	 */
	public boolean isMajorQType(int theQType) { 
		return theQType == qType; 
	} // isMajorQType(int)

	/** Gets the major question type of this question.
	 * @return	the major question type of this question
	 */
	public int getMajorQType() 			{ return qType; }
	/** Determines whether this question is a skeletal question.
	 * @return	true if this question is a skeletal question
	 */
	public boolean isMarvin() 			{ return isMarvin(qType); }
	/** Determines whether this question is a Lewis structure question.
	 * @return	true if this question is a Lewis structure question
	 */
	public boolean isLewis() 			{ return isLewis(qType); }
	/** Determines whether this question is a mechanism question.
	 * @return	true if this question is a mechanism question
	 */
	public boolean isMechanism() 		{ return isMechanism(qType); }
	/** Determines whether this question is a multiple-choice question.
	 * @return	true if this question is a multiple-choice question
	 */
	public boolean isChoice() 			{ return isChoice(qType); }
	/** Determines whether this question is a ranking or numbering question.
	 * @return	true if this question is a ranking or numbering question
	 */
	public boolean isRank() 			{ return isRank(qType); }
	/** Determines whether this question is a multistep synthesis question.
	 * @return	true if this question is a multistep synthesis question
	 */
	public boolean isSynthesis() 		{ return isSynthesis(qType); }
	/** Determines whether this question is a fill-in-the-blank
	 * (multiple-multiple-choice) question.
	 * @return	true if this question is a fill-in-the-blank
	 * (multiple-multiple-choice) question
	 */
	public boolean isFillBlank() 		{ return isFillBlank(qType); }
	/** Determines whether this question is a numerical question.
	 * @return	true if this question is a numerical question
	 */
	public boolean isNumeric() 			{ return isNumeric(qType); }
	/** Determines whether this question is an orbital energy diagram
	 * question.
	 * @return	true if this question is an orbital energy diagram
	 * question
	 */
	public boolean isOED() 				{ return isOED(qType); }
	/** Determines whether this question is a reaction coordinate diagram
	 * question.
	 * @return	true if this question is a reaction coordinate diagram
	 * question
	 */
	public boolean isRCD() 				{ return isRCD(qType); }
	/** Determines whether this question is an energy diagram question.
	 * @return	true if this question is an energy diagram question
	 */
	public boolean isED() 				{ return isOED() || isRCD(); }
	/** Determines whether this question is a text question.
	 * @return	true if this question is a text question
	 */
	public boolean isText() 			{ return isText(qType); }
	/** Determines whether this question is a complete-the-table question.
	 * @return	true if this question is a complete-the-table question
	 */
	public boolean isTable() 			{ return isTable(qType); }
	/** Determines whether this question is a choose-and-explain question.
	 * @return	true if this question is a choose-and-explain question
	 */
	public boolean isChooseExplain() 	{ return isChooseExplain(qType); }
	/** Determines whether this question is a clickable image question.
	 * @return	true if this question is a clickable image question
	 */
	public boolean isClickableImage() 	{ return isClickableImage(qType); }
	/** Determines whether this question is a logical statements question.
	 * @return	true if this question is a logical statements question
	 */
	public boolean isLogicalStatements() { return isLogicalStatements(qType); }
	/** Determines whether this question is a draw-vectors question.
	 * @return	true if this question is a draw-vectors question
	 */
	public boolean isDrawVectors() 		{ return isDrawVectors(qType); }
	/** Determines whether this question is an equations question.
	 * @return	true if this question is an equations question
	 */
	public boolean isEquations() 		{ return isEquations(qType); }
	/** Determines whether this question is a formula question.
	 * @return	true if this question is a formula question
	 */
	public boolean isFormula() 			{ return isFormula(qType); }
	/** Determines whether this question is a physics/math question.
	 * @return	true if this question is a physics/math question
	 */
	public boolean isPhysics() 			{ return isPhysics(qType); }
	/** Determines whether this question should be hidden from other authors.
	 * @return	true if this question should be hidden from other authors
	 */
	public boolean hide() 				{ return hide(qFlags); }
	/** Determines whether a structure should be preloaded into the applet
	 * for this question.
	 * @return	true if a structure should be preloaded into the applet for
	 * this question
	 */
	public boolean preload() 			{ return preload(qFlags); }
	/** Determines whether values should be preloaded into the table 
	 * for this question.
	 * @return	true if values should be preloaded into the table for
	 * this question
	 */
	public boolean preloadTable() 		{ return getNumQData(GENERAL) > TableQ.MIN_QDATA; }
	/** Determines whether atom mapping should be shown for this question.
	 * @return	true if atom mapping should be shown for this question
	 */
	public boolean showMapping() 		{ return showMapping(qFlags); }
	/** Determines whether this question uses three-dimensional structures.
	 * @return	true if this question uses three-dimensional structures
	 */
	public boolean is3D() 				{ return is3D(qFlags); }
	/** Determines whether this question uses R groups or variable values
	 * (algorithmic generation of questions).
	 * @return	true if this question uses R groups or variable values
	 */
	public boolean usesSubstns() 		{ return usesSubstns(qFlags); }
	/** Determines whether lone pairs should be shown for this question.
	 * @return	true if lone pairs should be shown for this question
	 */
	public boolean showLonePairs() 		{ return showLonePairs(qFlags); }
	/** Determines whether no implicit H atoms should be shown for this question.
	 * @return	true if no implicit H atoms should be shown for this question
	 */
	public boolean showNoHydrogens() 	{ return showNoHydrogens(qFlags); }
	/** Determines whether only implicit H atoms attached to heteroatoms should
	 * be shown for this question.
	 * @return	true if only implicit H atoms attached to heteroatoms should be
	 * shown for this question
	 */
	public boolean showHeteroHydrogens() { return showHeteroHydrogens(qFlags); }
	/** Determines whether all implicit H atoms should be shown for this question.
	 * @return	true if all implicit H atoms should be shown for this question
	 */
	public boolean showAllHydrogens() 	{ return showAllHydrogens(qFlags); }
	/** Determines whether all C atoms should be shown for this question.
	 * @return	true if all C atoms should be shown for this question
	 */
	public boolean showAllCarbons() 	{ return showAllCarbons(qFlags); }
	/** Determines whether R and S labels should be shown for this question.
	 * @return	true if R and S labels should be shown for this question
	 */
	public boolean showRSLabels() 		{ return showRSLabels(qFlags); }
	/** Determines whether to highlight valence errors in MarvinSketch.
	 * @return	true if should highlight valence errors in MarvinSketch
	 */
	public boolean badValenceInvisible() { return badValenceInvisible(qFlags); }
	/** Determines whether to use pulldown menus for labeling orbitals
	 * @return	true if should use pulldown menus for labeling orbitals
	 */
	public boolean labelOrbitals() 		{ return labelOrbitals(qFlags); }
	/** Determines whether choosing multiple options should be disallowed
	 * for this question.
	 * @return	true if choosing multiple options should be disallowed
	 * for this question
	 */
	public boolean disallowMult() 		{ return disallowMult(qFlags); }
	/** Determines whether unranked or unnumbered items should be allowed
	 * for this question.
	 * @return	true if unranked or unnumbered items should be allowed
	 * for this question
	 */
	public boolean allowUnranked() 		{ return allowUnranked(qFlags); }
	/** Determines whether options should be scrambled for this question.
	 * @return	true if options should be scrambled for this question
	 */
	public boolean scrambleOptions() 	{ return scrambleOptions(qFlags); }
	/** Determines whether to exclude the last option from scrambling when 
	 * scrambling options.
	 * @return	true if should exclude the last option from scrambling when 
	 * scrambling options
	 */
	public boolean exceptLast() 		{ return exceptLast(qFlags); }
	/** Determines whether this question should use scientific notation.
	 * @return	true if this question should use scientific notation
	 */
	public boolean useSciNotn() 		{ return useSciNotn(qFlags); }
	/** Determines whether the numeric response to this question must be
	 * integral.
	 * @return	true if the numeric response to this question must be integral
	 */
	public boolean requireInt() 		{ return requireInt(qFlags); }
	/** Determines whether this question's table entries must be numerical.
	 * @return	true if this question's table entries must be numerical
	 */
	public boolean numbersOnly() 		{ return numbersOnly(qFlags); }
	/** Determines whether chemistry formatting should not be applied.
	 * @return	true if chemistry formatting should not be applied
	 */
	public boolean noChemFormatting() 	{ return noChemFormatting(qType, qFlags); }
	/** Determines whether chemistry formatting should be applied.
	 * @return	true if chemistry formatting should be applied
	 */
	public boolean chemFormatting() 	{ return !noChemFormatting(); }
	/** Determines whether to omit the field for constants.
	 * @return	true if should omit the field for constants
	 */
	public boolean omitConstantsField() { return omitConstantsField(qFlags); }
	/** Gets the figure number to preload into the applet for this question.
	 * @return	the figure number to preload into the applet for this question
	 */
	public int getPreloadFig() 			{ return getPreloadFig(qFlags); }

	/** Determines whether wedge bonds starting at nonstereocenters should be 
	 * disallowed for this question.
	 * @return	true if superfluous wedge bonds should be disallowed
	 * for this question
	 */
	public boolean disallowSuperfluousWedges() { 
		return disallowSuperfluousWedges(qFlags); 
	} // disallowSuperfluousWedges()

	/** Determines whether this question's response uses structures
	 * @return	true if this question's response uses structures
	 */
	public boolean usesStructures() {
		return isMarvin() || isLewis() || isMechanism() || isSynthesis();
	} // usesStructures()

	/** Gets whether display of a response to this question requires that a
	 * figure be displayed as well.
	 * @return	true if display of a response to this question requires a figure
	 */
	public boolean responseDisplayRequiresFigure() {
		return isClickableImage() || isDrawVectors();
	} // responseDisplayRequiresFigure()

/* *************** Question types and flags: static methods  ******************/

	/** Gets a string describing the type of question.
	 * @param	theQType	the type of a question
	 * @param	theQFlags	the flags of a question
	 * @return	a description of the type of this question
	 */
	public static String getQTypeDescription(int theQType, long theQFlags) {
		return getQTypeDescription(theQType, theQFlags, new User());
	} // getQTypeDescription(int, long)

	/** Gets a string describing the type of question.
	 * @param	theQType	the type of a question
	 * @param	theQFlags	the flags of a question
	 * @param	user	a user
	 * @return	a description of the type of question
	 */
	public static String getQTypeDescription(int theQType, long theQFlags, 
			User user) {
		return (isMarvin(theQType) && showMapping(theQFlags) 
					? user.translate("Mapping")
				: isMarvin(theQType) && is3D(theQFlags) 
					? user.translate("3D structure")
				: MathUtils.inRange(theQType, 
						new int[] {1, QTYPE_NAMES.length})
					? user.translate(QTYPE_NAMES[theQType - 1])
				: theQType == OTHER ? user.translate("Other")
				: user.translate("Unknown question type") 
						+ " " + theQType);
	} // getQTypeDescription(int, long, User)

	/** Determines whether a question is a skeletal question.
	 * @param	theQType	the flags of the question
	 * @return	true if a question is a skeletal question
	 */
	public static boolean isMarvin(int theQType) {
		return theQType == MARVIN;
	} // isMarvin(int)

	/** Determines whether a question is a Lewis structure question.
	 * @param	theQType	the flags of the question
	 * @return	true if a question is a Lewis structure question
	 */
	public static boolean isLewis(int theQType) {
		return theQType == LEWIS;
	} // isLewis(int)

	/** Determines whether a question is a mechanism question.
	 * @param	theQType	the flags of the question
	 * @return	true if a question is a mechanism question
	 */
	public static boolean isMechanism(int theQType) {
		return theQType == MECHANISM;
	} // isMechanism(int)

	/** Determines whether a question is a multiple-choice question.
	 * @param	theQType	the flags of the question
	 * @return	true if a question is a multiple-choice question
	 */
	public static boolean isChoice(int theQType) {
		return theQType == CHOICE;
	} // isChoice(int)

	/** Determines whether a question is a ranking or numbering question.
	 * @param	theQType	the flags of the question
	 * @return	true if a question is a ranking or numbering question
	 */
	public static boolean isRank(int theQType) {
		return theQType == RANK;
	} // isRank(int)

	/** Determines whether a question is a synthesis question.
	 * @param	theQType	the flags of the question
	 * @return	true if a question is a synthesis question
	 */
	public static boolean isSynthesis(int theQType) {
		return theQType == SYNTHESIS;
	} // isSynthesis(int)

	/** Determines whether a question is a fill-in-the-blank
	 * @param	theQType	the flags of the question
	 * (multiple-multiple-choice) question.
	 * @return	true if a question is a fill-in-the-blank
	 * (multiple-multiple-choice) question
	 */
	public static boolean isFillBlank(int theQType) {
		return theQType == FILLBLANK;
	} // isFillBlank(int)

	/** Determines whether a question is a numerical question.
	 * @param	theQType	the flags of the question
	 * @return	true if a question is a numerical question
	 */
	public static boolean isNumeric(int theQType) {
		return theQType == NUMERIC;
	} // isNumeric(int)

	/** Determines whether a question is an orbital energy diagram question.
	 * @param	theQType	the flags of the question
	 * @return	true if a question is an orbital energy diagram question
	 */
	public static boolean isOED(int theQType) {
		return theQType == ORB_E_DIAGRAM;
	} // isOED(int)

	/** Determines whether a question is a reaction coordinate diagram question.
	 * @param	theQType	the flags of the question
	 * @return	true if a question is a reaction coordinate diagram question
	 */
	public static boolean isRCD(int theQType) {
		return theQType == RXN_COORD;
	} // isRCD(int)

	/** Determines whether a question is an energy diagram question.
	 * @param	theQType	the flags of the question
	 * @return	true if a question is an energy diagram question
	 */
	public static boolean isED(int theQType) {
		return isOED(theQType) || isRCD(theQType);
	} // isED(int)

	/** Determines whether a question is a text question.
	 * @param	theQType	the flags of the question
	 * @return	true if a question is a text question
	 */
	public static boolean isText(int theQType) {
		return theQType == TEXT;
	} // isText(int)

	/** Determines whether a question is a complete-the-table question.
	 * @param	theQType	the flags of the question
	 * @return	true if a question is a complete-the-table question
	 */
	public static boolean isTable(int theQType) {
		return theQType == TABLE;
	} // isTable(int)

	/** Determines whether a question is a choose-and-explain question.
	 * @param	theQType	the flags of the question
	 * @return	true if a question is a choose-and-explain question
	 */
	public static boolean isChooseExplain(int theQType) {
		return theQType == CHOOSE_EXPLAIN;
	} // isChooseExplain(int)

	/** Determines whether a question is a clickable image question.
	 * @param	theQType	the flags of the question
	 * @return	true if a question is a clickable image question
	 */
	public static boolean isClickableImage(int theQType) {
		return theQType == CLICK_IMAGE;
	} // isClickableImage(int)

	/** Determines whether a question is a logical statements question.
	 * @param	theQType	the flags of the question
	 * @return	true if a question is a logical statements question
	 */
	public static boolean isLogicalStatements(int theQType) {
		return theQType == LOGIC;
	} // isLogicalStatements(int)

	/** Determines whether a question is a draw-vectors question.
	 * @param	theQType	the flags of the question
	 * @return	true if a question is a draw-vectors question
	 */
	public static boolean isDrawVectors(int theQType) {
		return theQType == DRAW_VECTORS;
	} // isDrawVectors(int)

	/** Determines whether a question is an equations question.
	 * @param	theQType	the flags of the question
	 * @return	true if a question is an equations question
	 */
	public static boolean isEquations(int theQType) {
		return theQType == EQUATIONS;
	} // isEquations(int)

	/** Determines whether a question is a formula question.
	 * @param	theQType	the flags of the question
	 * @return	true if a question is a formula question
	 */
	public static boolean isFormula(int theQType) {
		return theQType == FORMULA;
	} // isFormula(int)

	/** Determines whether a question is a physics/math question.
	 * @param	theQType	the flags of the question
	 * @return	true if a question is a physics/math question
	 */
	public static boolean isPhysics(int theQType) {
		return isDrawVectors(theQType) || isEquations(theQType);
	} // isPhysics(int)

	/** Determines whether a question's response uses structures
	 * @param	theQType	the flags of the question
	 * @return	true if the question's response uses structures
	 */
	public static boolean usesStructures(int theQType) {
		return isMarvin(theQType) || isLewis(theQType)
				|| isMechanism(theQType) || isSynthesis(theQType);
	} // usesStructures(int)

	/** Determines whether a question should be hidden from other authors.
	 * @param	theQFlags	the flags of the question
	 * @return	true if a question should be hidden from other authors
	 */
	public static boolean hide(long theQFlags) {
		return (theQFlags & HIDE) != 0;
	} // hide(long)

	/** Determines whether a structure should be preloaded into the applet
	 * for a question.
	 * @param	theQFlags	the flags of the question
	 * @return	true if a structure should be preloaded into the applet for
	 * the question
	 */
	public static boolean preload(long theQFlags) {
		return (theQFlags & PRELOAD) != 0;
	} // preload(long)

	/** Determines whether atom mapping should be shown for a question.
	 * @param	theQFlags	the flags of the question
	 * @return	true if atom mapping should be shown for a question
	 */
	public static boolean showMapping(long theQFlags) {
		return (theQFlags & SHOWMAPPING) != 0;
	} // showMapping(long)

	/** Determines whether a question uses three-dimensional structures.
	 * @param	theQFlags	the flags of the question
	 * @return	true if a question uses three-dimensional structures
	 */
	public static boolean is3D(long theQFlags) {
		return (theQFlags & THREEDIM) != 0;
	} // is3D(long)

	/** Determines whether a question uses R groups or variable values
	 * (algorithmic generation of questions).
	 * @param	theQFlags	the flags of the question
	 * @return	true if a question uses R groups or variable values
	 */
	public static boolean usesSubstns(long theQFlags) {
		return (theQFlags & USES_SUBSTNS) != 0;
	} // usesSubstns(long)

	/** Determines whether lone pairs should be shown for a question.
	 * @param	theQFlags	the flags of the question
	 * @return	true if lone pairs should be shown for a question
	 */
	public static boolean showLonePairs(long theQFlags) {
		return (theQFlags & SHOWLONEPAIRS) != 0;
	} // showLonePairs(long)

	/** Determines whether no implicit H atoms should be shown for a question.
	 * @param	theQFlags	the flags of the question
	 * @return	true if no implicit H atoms should be shown for a question
	 */
	public static boolean showNoHydrogens(long theQFlags) {
		return (theQFlags & IMPLICITHMASK) == SHOWNOH;
	} // showNoHydrogens(long)

	/** Determines whether only implicit H atoms attached to heteroatoms should
	 * be shown for a question.
	 * @param	theQFlags	the flags of the question
	 * @return	true if only implicit H atoms attached to heteroatoms should be
	 * shown for a question
	 */
	public static boolean showHeteroHydrogens(long theQFlags) {
		return (theQFlags & IMPLICITHMASK) == SHOWHETEROH;
	} // showHeteroHydrogens(long)

	/** Determines whether all implicit H atoms should be shown for a question.
	 * @param	theQFlags	the flags of the question
	 * @return	true if all implicit H atoms should be shown for a question
	 */
	public static boolean showAllHydrogens(long theQFlags) {
		return (theQFlags & IMPLICITHMASK) == SHOWALLH;
	} // showAllHydrogens(long)

	/** Determines whether all C atoms should be shown for a question.
	 * @param	theQFlags	the flags of the question
	 * @return	true if all C atoms should be shown for a question
	 */
	public static boolean showAllCarbons(long theQFlags) {
		return (theQFlags & SHOWALLC) != 0;
	} // showAllCarbons(long)

	/** Determines whether R and S labels should be shown for a question.
	 * @param	theQFlags	the flags of the question
	 * @return	true if R and S labels should be shown for a question
	 */
	public static boolean showRSLabels(long theQFlags) {
		return (theQFlags & SHOWRSLABELS) != 0;
	} // showRSLabels(long)

	/** Determines whether to highlight valence errors in MarvinSketch.
	 * @param	theQFlags	the flags of the question
	 * @return	true if should highlight valence errors in MarvinSketch
	 */
	public static boolean badValenceInvisible(long theQFlags) {
		return (theQFlags & BADVALENCEINVISIBLE) != 0;
	} // badValenceInvisible(long)

	/** Determines whether to use pulldown menus for labeling orbitals
	 * @param	theQFlags	the flags of the question
	 * @return	true if should use pulldown menus for labeling orbitals
	 */
	public static boolean labelOrbitals(long theQFlags) {
		return (theQFlags & LABEL_ORBS) != 0;
	} // labelOrbitals(long)

	/** Determines whether choosing multiple options should be disallowed
	 * for a question.
	 * @param	theQFlags	the flags of the question
	 * @return	true if choosing multiple options should be disallowed
	 * for a question
	 */
	public static boolean disallowMult(long theQFlags) {
		return (theQFlags & DISALLOW_MULT) != 0;
	} // disallowMult(long)

	/** Determines whether unranked or unnumbered items should be allowed
	 * for a question.
	 * @param	theQFlags	the flags of the question
	 * @return	true if unranked or unnumbered items should be allowed
	 * for a question
	 */
	public static boolean allowUnranked(long theQFlags) {
		return (theQFlags & ALLOW_UNRANKED) != 0;
	} // allowUnranked(long)

	/** Determines whether options should be scrambled for a question.
	 * @param	theQFlags	the flags of the question
	 * @return	true if options should be scrambled for a question
	 */
	public static boolean scrambleOptions(long theQFlags) {
		return (theQFlags & SCRAMBLE) != 0;
	} // scrambleOptions(long)

	/** Determines whether to exclude the last option from scrambling when 
	 * scrambling options.
	 * @param	theQFlags	the question flags
	 * @return	true if should exclude the last option from scrambling when 
	 * scrambling options
	 */
	public static boolean exceptLast(long theQFlags) {
		return (theQFlags & EXCEPT_LAST) != 0;
	} // exceptLast(long)

	/** Determines whether a question should use scientific notation.
	 * @param	theQFlags	the flags of the question
	 * @return	true if a question should use scientific notation
	 */
	public static boolean useSciNotn(long theQFlags) {
		return (theQFlags & USE_SCI_NOTN) != 0;
	} // useSciNotn(long)

	/** Determines whether the numeric response to a question must be
	 * integral.
	 * @param	theQFlags	the flags of the question
	 * @return	true if the numeric response to the question must be integral
	 */
	public static boolean requireInt(long theQFlags) {
		return (theQFlags & REQUIRE_INT) != 0;
	} // requireInt(long)

	/** Determines whether a question's table entries must be numerical.
	 * @param	theQFlags	the flags of the question
	 * @return	true if a question's table entries must be numerical
	 */
	public static boolean numbersOnly(long theQFlags) {
		return (theQFlags & NUMS_ONLY) != 0;
	} // numbersOnly(long)

	/** Determines whether chemistry formatting should not be applied.
	 * @param	theQType	the flags of the question
	 * @param	theQFlags	the flags of the question
	 * @return	true if chemistry formatting should not be applied
	 */
	public static boolean noChemFormatting(int theQType, long theQFlags) {
		return (theQFlags & NO_CHEM_FORMATTING) != 0 
				|| isPhysics(theQType);
	} // noChemFormatting(int, long)

	/** Determines whether chemistry formatting should be applied.
	 * @param	theQType	the flags of the question
	 * @param	theQFlags	the flags of the question
	 * @return	true if chemistry formatting should be applied
	 */
	public static boolean chemFormatting(int theQType, long theQFlags) {
		return !noChemFormatting(theQType, theQFlags);
	} // chemFormatting(int, long)

	/** Determines whether to omit the field for constants.
	 * @param	theQFlags	the question flags
	 * @return	true if should omit the field for constants
	 */
	public static boolean omitConstantsField(long theQFlags) {
		return (theQFlags & NO_CONSTANTS_FIELD) != 0;
	} // omitConstantsField(long)

	/** Determines whether wedge bonds starting at nonstereocenters should be 
	 * disallowed for a question.
	 * @param	theQFlags	the flags of the question
	 * @return	true if superfluous wedge bonds should be disallowed
	 * for a question
	 */
	public static boolean disallowSuperfluousWedges(long theQFlags) {
		return (theQFlags & DISALLOW_SUPERFLUOUS_WEDGES) != 0;
	} // disallowSuperfluousWedges(long)

	/** Gets the figure number to preload into the applet for a question.
	 * @param	theQFlags	the question flags
	 * @return	the figure number to preload into the applet for a question
	 */
	public static int getPreloadFig(long theQFlags) {
		return (int) (theQFlags & FIG_TO_PRELOAD_MASK) >> FIG_TO_PRELOAD_SHIFT;
	} // getPreloadFig(long)

/* *************** Evaluation methods ******************/

	/** Evaluates the response to a question.  Called from
	 * RegradeSession.doRegrade().
	 * @param	allEvalsResult	contains response string from front-end
	 * response collector, R groups for instantiation.  Modified with
	 * feedback, grade, modified response.
	 * @param	substns	R groups or variable values associated with this user's 
	 * instance of the question; may be null or empty
	 * @throws	ResponseFormatException	when the response format is incorrect
	 * @throws	ResponseParseException	when the synthesis or mechanism response
	 * cannot be parsed or the formula is malformed
	 */
	public void evaluateResponse(EvalResult allEvalsResult, Object[] substns)
			throws ResponseParseException, ResponseFormatException {
		final boolean MAY_SEE_SYNTH_CALCD_PRODS = true;
		evaluateResponse(allEvalsResult, substns, new String[0], 
				MAY_SEE_SYNTH_CALCD_PRODS);
	} // evaluateResponse(EvalResult, Object[])

	/** Evaluates the response to a question.  Called from
	 * HWSession.submitResponse().
	 * @param	allEvalsResult	contains response string from front-end
	 * response collector, R groups for instantiation.  Modified with
	 * feedback, grade, modified response.
	 * @param	substns	R groups or variable values associated with this user's 
	 * instance of the question; may be null or empty
	 * @param	userLangs	non-English languages chosen by this user
	 * in order of preference
	 * @param	maySeeSynthCalcProds	whether the user should be permitted to
	 * see calculated synthesis products in the feedback
	 * @throws	ResponseFormatException	when the response format is incorrect
	 * @throws	ResponseParseException	when the synthesis or mechanism response
	 * cannot be parsed or the formula is malformed
	 */
	public void evaluateResponse(EvalResult allEvalsResult, Object[] substns,
			String[] userLangs, boolean maySeeSynthCalcProds)
			throws ResponseParseException, ResponseFormatException {
		final String SELF = "Question.evaluateResponse: ";
		final Response response = parseResponse(allEvalsResult, substns, 
				userLangs, maySeeSynthCalcProds);
		if (response == null) return; // couldn't be initialized
		final boolean shouldHaveMolecule =
				isMarvin() || isLewis() || isMechanism() || isSynthesis();
		if (shouldHaveMolecule && response.moleculeObj.getAtomCount() == 0) {
			allEvalsResult.feedback = translate("Please submit a response "
					+ "that contains a compound.", userLangs);
			allEvalsResult.grade = 0;
			return;
		} // if no molecule submitted where needed
		// in synthesis Qs, question data may contain information about
		// permissible starting materials
		if (isSynthesis()) {
			final Synthesis synthResp = (Synthesis) response.parsedResp;
			synthResp.setStarterRules(getQData(GENERAL), (Molecule[]) substns);
		} // if synthesis
		// create molecules corresponding to instantiated R groups
		try {
			boolean evaluatorSatisfied = false;
			Evaluator matchingEvaluator = null;
			OneEvalResult oneEvalResult = null;
			for (int evalNum = 0; evalNum < getNumEvaluators(); evalNum++) {
				debugPrint(SELF + "Checking evaluator ", evalNum + 1, ".");
				matchingEvaluator = getEvaluator(evalNum + 1);
				oneEvalResult = matchingEvaluator.matchResponse(response);
				if (oneEvalResult.isSatisfied) {
					debugPrint(SELF + "evaluator satisfied.");
					evaluatorSatisfied = true;
					break;
				} else if (oneEvalResult.verificationFailureString != null) {
					debugPrint(SELF + "response malformed.");
					if (isTable()) {
						modifyFailureString(oneEvalResult);
					} // if complete-the-table question
					allEvalsResult.grade = 0;
					allEvalsResult.feedback =
							(oneEvalResult.autoFeedback != null
							? translate(oneEvalResult, userLangs)
							: oneEvalResult.verificationFailureString);
					modifyResponse(oneEvalResult, allEvalsResult, response);
					return;
				} else {
					debugPrint(SELF + "evaluator not satisfied.");
				} // if evaluator is satisfied or verification error occurred
			} // for each evaluator
			if (evaluatorSatisfied) {
				allEvalsResult.status = (oneEvalResult.humanRequired
						? HUMAN_NEEDED : EVALUATED);
				allEvalsResult.grade = (oneEvalResult.calcScore >= 0
						? oneEvalResult.calcScore : matchingEvaluator.grade);
				// set feedback
				final StringBuilder feedbackBld = new StringBuilder();
				if (oneEvalResult.autoFeedback != null
						&& allEvalsResult.grade < 1) {
					// only incorrect responses generate autofeedback
					Utils.appendTo(feedbackBld, 
							translate(oneEvalResult, userLangs), ' ');
				} // if there is autofeedback
				if (matchingEvaluator.feedback != null) {
					feedbackBld.append(matchingEvaluator.feedback);
				} // if there is author-written feedback
				allEvalsResult.feedback = feedbackBld.toString();
				// set modified response
				if (allEvalsResult.grade < 1 || isTable()) {
					// only incorrect responses or table responses are modified
					modifyResponse(oneEvalResult, allEvalsResult, response);
				} // if response is incorrect
			} else { // no evaluator satisfied; treat it as wrong.
				debugPrint(SELF + "no evaluator satisfied.");
				allEvalsResult.status = EVALUATED;
				allEvalsResult.grade = 0;
			} // no evaluator satisfied
		} catch (Exception e) {
			e.printStackTrace();
		} // exceptions
		debugPrint(SELF + "allEvalsResult.status = ",
				allEvalsResult.status, ", grade = ",
				allEvalsResult.grade, ", feedback = ",
				allEvalsResult.feedback);
	} // evaluateResponse(EvalResult, Object[], String[], boolean)

	/** Parses the response to a question.  Called from
	 * evaluateResponse() and HWSession.submitResponse().
	 * @param	allEvalsResult	contains response string from front-end
	 * response collector, R groups for instantiation.  Modified with
	 * feedback, grade, modified response.
	 * @param	substns	R groups or variable values associated with this user's 
	 * instance of the question; may be null or empty
	 * @param	userLangs	non-English languages chosen by this user
	 * in order of preference
	 * @param	maySeeSynthCalcProds	whether the user should be permitted to
	 * see calculated synthesis products in the feedback
	 * @return	the parsed response
	 * @throws	ResponseFormatException	when the response format is incorrect
	 * @throws	ResponseParseException	when the synthesis or mechanism response
	 * cannot be parsed
	 */
	public Response parseResponse(EvalResult allEvalsResult, Object[] substns,
			String[] userLangs, boolean maySeeSynthCalcProds)
			throws ResponseParseException, ResponseFormatException {
		final String SELF = "Question.parseResponse: ";
		debugPrint(SELF + "parsing response to Q ", qId, ".");
		Response response = new Response(qType, qFlags, getQData(GENERAL),
				allEvalsResult, substns, userLangs, maySeeSynthCalcProds);
		allEvalsResult.status = EVALUATED;
		if (!response.initialized) {
			allEvalsResult.grade = 0;
			allEvalsResult.feedback =  // already translated
					response.initializationErrorResult.feedback;
			debugPrint(SELF + "could not initialize response; feedback = ", 
					allEvalsResult.feedback);
			allEvalsResult.modifiedResponse =
					response.initializationErrorResult.modifiedResponse;
			if (response.badFormat()) {
				throw new ResponseFormatException(allEvalsResult.feedback);
			} else if (response.badParsing()) {
				throw new ResponseParseException(allEvalsResult.feedback);
			} else response = null;
		} // if couldn't initialize response
		debugPrint(SELF + "parsing successful.");
		return response;
	} // parseResponse(EvalResult, Object[], String[], boolean)

	/** Translates autoFeedback into one of the user's preferred languages.
	 * @param	autoFeedback	autoFeedback to translate
	 * @param	userLangs	languages preferred by the user in order of
	 * preference
	 * @return	translated autoFeedback, or English if no appropriate translation
	 * is available
	 */
	private String translate(String autoFeedback, String[] userLangs) {
		debugPrint("Entering Question.translate with:\n\t", autoFeedback);
		final String newFeedback =
				PhraseTransln.translate(autoFeedback, userLangs);
		debugPrint("Question.translate: returning:\n\t", newFeedback);
		return newFeedback;
	} // translate(String, String[])

	/** Translates array of autoFeedback into one of the user's preferred
	 * languages.  Modifies the original.
	 * @param	autoFeedback	array of autoFeedback to translate
	 * @param	userLangs	languages preferred by the user in order of
	 * preference
	 */
	private void translate(String[] autoFeedback, String[] userLangs) {
		PhraseTransln.translate(autoFeedback, userLangs);
	} // translate(String[], String[])

	/** Translates autofeedback into one of the user's preferred languages.
	 * May incorporate a variable part into the autofeedback.
	 * Variable parts may be substituted for the demarcated part of a phrase or
	 * inserted around it; they may be translated or not.
	 * @param	oneEvalResult	contains the autofeedback; may also contain
	 * variable parts to substitute into it and flags for how to handle them
	 * @param	userLangs	languages preferred by the user in order of
	 * preference
	 * @return	translated autoFeedback, or English if no appropriate translation
	 * is available
	 */
	private String translate(OneEvalResult oneEvalResult, String[] userLangs) {
		final String SELF = "Question.translate: ";
		// get the phrases to be translated
		final String[] autoFeedbackPhrs = oneEvalResult.autoFeedback;
		final boolean insertAroundDemarcated = // alternative is substitution
				((oneEvalResult.howHandleVarParts & INSERT) != 0);
		final boolean translateVarParts =
				((oneEvalResult.howHandleVarParts & TRANSLATE) != 0);
		debugPrint("Entering " + SELF + " with:\n\t", autoFeedbackPhrs,
				", insertAroundDemarcated = ", insertAroundDemarcated,
				", translateVarParts = ", translateVarParts);
		translate(autoFeedbackPhrs, userLangs);
		debugPrint(SELF + "translated autoFeedbackPhrs:\n", autoFeedbackPhrs);
		// pad with spaces so demarcated phrases have odd indices after splitting
		StringBuilder autoFeedbackBld = Utils.getBuilder(' ');
		// join the translated phrases
		for (final String phrase : autoFeedbackPhrs) {
			Utils.appendTo(autoFeedbackBld, phrase, ' ');
		} // for each translated phrase
		final String autoFeedbackJoined = autoFeedbackBld.toString();
		debugPrint(SELF + "translated autoFeedbackJoined = ",
				autoFeedbackJoined);
		// split the joined, translated feedback at *** for insertion or
		// substitution of variable parts
		final String[] varParts = oneEvalResult.autoFeedbackVariableParts;
		if (translateVarParts) translate(varParts, userLangs);
		debugPrint(SELF, (translateVarParts ? "translated " : ""),
				"variable parts are:\n", varParts);
		final String[] autoFeedbackParts = autoFeedbackJoined.split(STARS_REGEX);
		// autoFeedbackParts has form:
		// 		phr1	demarc1	phr2	demarc2	phr3...
		// if insertAroundDemarcated preserve demarcated phrases and convert to:
		// 		phr1	var1	demarc1	var2	phr2	var3	demarc2	var4	phr3...
		// otherwise discard demarcated phrases and convert to:
		// 		phr1	var1	phr2	var2	phr3...
		autoFeedbackBld = Utils.getBuilder(autoFeedbackParts[0]);
		int varNum = 0;
		for (int fdbckNum = 1; fdbckNum < autoFeedbackParts.length; fdbckNum += 2) {
			if (insertAroundDemarcated) {
				if (varNum < varParts.length)
					autoFeedbackBld.append(varParts[varNum++]);
				autoFeedbackBld.append(autoFeedbackParts[fdbckNum]);
				if (varNum < varParts.length)
					autoFeedbackBld.append(varParts[varNum++]);
			} else { // substitute
				autoFeedbackBld.append(varNum < varParts.length
						? varParts[varNum++]
						: autoFeedbackParts[fdbckNum]);
			} // if insertAroundDemarcated
			if (fdbckNum + 1 < autoFeedbackParts.length)
				autoFeedbackBld.append(autoFeedbackParts[fdbckNum + 1]);
		} // for each piece of feedback
		final String autoFeedback = autoFeedbackBld.toString().trim();
		debugPrint(SELF + "returning: ", autoFeedback);
		return autoFeedback;
	} // translate(OneEvalResult, String[])

	/** Modifies the verification failure message to include the row or column
	 * caption.  This method relies on the message having a format where the
	 * word "row" or "column" is followed by a space, a number, and then another
	 * space.  If any exception is thrown, never mind.
	 * @param	oneEvalResult	the OneEvalResult containing the verification
	 * error message.
	 */
	private void modifyFailureString(OneEvalResult oneEvalResult) {
		try {
			String failureStr = oneEvalResult.verificationFailureString;
			for (int rowCol = 0; rowCol < TableQ.MIN_QDATA; rowCol++) {
				final String rowColStr = (rowCol == TableQ.ROW_DATA
						? "row " : "column ");
				int numPosn = failureStr.indexOf(rowColStr);
				if (numPosn >= 0) {
					numPosn += rowColStr.length();
					final StringBuilder numStr = new StringBuilder();
					while (numPosn < failureStr.length()) {
						final char oneChar = failureStr.charAt(numPosn);
						if (oneChar == ' ') break;
						numStr.append(oneChar);
						numPosn++;
					} // while
					final int num = Integer.parseInt(numStr.toString());
					final String[] captions = ((CaptionsQDatum) 
							getQDatum(GENERAL, rowCol + 1)).captions;
					if (rowCol == TableQ.ROW_DATA) {
						if (num > 0 && num <= captions.length) {
							failureStr = Utils.toString(
									failureStr.substring(0, numPosn), " (", 
									captions[num - 1].trim(), ")", 
									failureStr.substring(numPosn));
						} // if the row number has a caption
					} else { // TableQ.COL_DATA:
						if (num >= 0 && num < captions.length) {
							failureStr = Utils.toString(
									failureStr.substring(0, numPosn), " (", 
									captions[num].trim(), ")", 
									failureStr.substring(numPosn));
						} // if the column number has a caption
					} // if row or Col
				} // if there's a row/column number
			} // for rows, then columns
			oneEvalResult.verificationFailureString = failureStr;
		} catch (Exception e) {
			Utils.alwaysPrint("Question.modifyFailureString: Caught exception "
					+ "while trying to modify verification failure error "
					+ "message; doing nothing.");
			e.printStackTrace();
		} // try
	} // modifyFailureString(OneEvalResult)

	/** Stores a modified response to highlight an aspect of it.
	 * @param	oneEvalResult	the result of a single top-level evaluator
	 * @param	allEvalsResult	the result of overall evaluation
	 * @param	response	the response
	 */
	private void modifyResponse(OneEvalResult oneEvalResult,
			EvalResult allEvalsResult, Response response) {
		if (oneEvalResult.modifiedResponse != null) {
			allEvalsResult.modifiedResponse = oneEvalResult.modifiedResponse;
		} else if (isTable()) {
			// use question data to modify the response
			final boolean FOR_STUDENT = false;
			final TableQ tableResp = (TableQ) response.parsedResp;
			try {
				final QDatum[] qdArr = getQData(GENERAL);
				final int numQD = qdArr.length;
				final CaptionsQDatum[] capsQDArr = new CaptionsQDatum[numQD];
				for (int qdNum = 0; qdNum < numQD; qdNum++) {
					capsQDArr[qdNum] = new CaptionsQDatum(qdArr[qdNum]);
				} // for each qDatum
				allEvalsResult.modifiedResponse =
						tableResp.convertToHTMLInput(
							capsQDArr, chemFormatting(), FOR_STUDENT, 
							oneEvalResult.toModifyResponse);
			} catch (ParameterException e) {
				Utils.alwaysPrint("Question.modifyResponse: Caught exception "
						+ "while trying to modify table with color; "
						+ "doing nothing.");
				e.printStackTrace();
			}
		} // if response has been or needs to be modified
	} // modifyResponse(OneEvalResult, EvalResult, Response)

	/** Finds the first evaluator that returns true.
	 * @param	respStr	response String from front end collector
	 * @return	major and minor IDs of first subevaluator that returns true, 
	 * null if none
	 * @throws	ParameterException	never
	 */
	public int[] firstEvaluatorMatching(String respStr) 
			throws ParameterException {
		final String SELF = "Question.firstEvaluatorMatching: ";
		debugPrint(SELF + "allEvaluators size = ", getNumEvaluators());
		debugPrint(SELF + "parsing response to Q ", qId, ":\n",
				respStr);
		final Response response = new Response(qType, qFlags, 
				getQData(GENERAL), respStr);
		debugPrint(SELF + "parsing successful.");
		if (!response.initialized) {
			Utils.alwaysPrint(SELF + "could not initialize response.");
			return noInts;
		} // if couldn't initialize response
		for (int evalNum = 1; evalNum <= getNumEvaluators(); evalNum++) {
			final Evaluator eval = getEvaluator(evalNum);
			final int numSubevals = eval.getNumSubevaluators();
			debugPrint(SELF + "Checking evaluator ", evalNum, 
					" with ", numSubevals, " subevaluators.");
			for (int subNum = 1; subNum <= numSubevals; subNum++) {
				final Subevaluator subeval = eval.getSubevaluator(subNum);
				final OneEvalResult oneEvalResult =
						subeval.isResponseMatching(response);
				if (oneEvalResult != null) {
					if (oneEvalResult.isSatisfied) {
						debugPrint(SELF + "evaluator ", evalNum, 
								" and subevaluator ", subNum, " are satisfied.");
						if (!eval.isComplex()) subNum = 0;
						return new int[] {evalNum, subNum};
					} else if (oneEvalResult.verificationFailureString != null) {
						debugPrint(SELF + "evaluator ", evalNum, 
								" and subevaluator ", subNum, " returned "
								+ "verification failure; continuing to look.");
					} // if oneEvalResult
				} // if evaluator doesn't return null
			} // for each subevaluator
		} // for each evaluator
		return noInts;
	} // firstEvaluatorMatching(String)

/* *************** Authoring tool methods ******************/

/* ************** Figure-related operations **************/

	/** Gets the figures of this question.
	 * @return	array of figures
	 */
	public Figure[] getFigures() {
		debugPrint("Question.getFigures: returning array of ",
				getNumFigures(), " figures.");
		return figures.toArray(new Figure[getNumFigures()]);
	} // getFigures

	/** Gets a figure of this question.
	 * @param	index	1-based serial number of the figure
	 * @return	a figure
	 * @throws	ParameterException	if the serial number is invalid
	 */
	public Figure getFigure(int index) throws ParameterException {
		if (index <= 0 || index > getNumFigures()) {
			throw new ParameterException("Invalid index to getFigure "
					+ index);
		}
		return figures.get(index - 1);
	} // getFigure(index)

	/** Returns a value with which to preload the response collector, e.g., an
	 * empty molecule, a figure, or a partly filled table.
	 * @return	a partly filled table, an empty molecule, or a figure
	 * @throws	ParameterException	if the question datum serial number is 
	 * invalid
	 */
	public String getPreloadMol() throws ParameterException {
		final String SELF = "Question.getPreloadMol: ";
		/*Utils.alwaysPrint(SELF + "isTable() = ", isTable(), 
				", getNumQData(GENERAL) = ", getNumQData(GENERAL), 
				", TableQ.MIN_QDATA = ", TableQ.MIN_QDATA); /**/
		final String preloadMol = (isTable() && preloadTable()
					? getQDatum(GENERAL, TableQ.PRELOAD_DATA + 1).data
				: preload() || isEquations() ? getPreloadMolRegardless()
				: usesStructures() ? EMPTY_MRV
				: "");
		/* Utils.alwaysPrint(SELF + "preload:\n", preloadMol); /**/
		return preloadMol;
	} // getPreloadMol()

	/** Returns the first figure that can be preloaded.
	 * @return	the first figure that can be preloaded; null if none
	 * @throws	ParameterException	if the question datum serial number is 
	 * invalid
	 */
	public String getPreloadMolRegardless() throws ParameterException {
		String preloadMol = "";
		if (isDrawVectors() && getNumFigures() >= 1) {
			final Figure fig1 = figures.get(0);
			if (fig1.isImageAndVectors()) {
				final String[] displayData = fig1.getDisplayData();
				preloadMol = displayData[Figure.COORDS];
			} // if contains vectors
		} else if (isEquations() && getNumQData(GENERAL) > 0) {
			preloadMol = getQDatum(GENERAL, 1).data;
		} else if (usesStructures()) {
			preloadMol = EMPTY_MRV;
			final int numFigs = getNumFigures();
			final int preloadFig = getPreloadFig();
			if (preloadFig < numFigs) {
				final Figure fig = figures.get(preloadFig);
				if (fig.isUsablePreload()) preloadMol = fig.data;
				else for (final Figure figure : figures) {
					if (figure.isUsablePreload()) {
						return figure.data;
					} // if figure is usable
				} // for each figure
			} // if number of figure to preload is OK
		} // if question type
		return preloadMol;
	} // getPreloadMolRegardless()

	/** Sets a figure of this question.
	 * @param	index 1-based serial number of the figure
	 * @param	figure	the figure
	 * @throws	ParameterException	if the figure serial number is invalid
	 */
	public void setFigure(int index, Figure figure) throws ParameterException {
		final String SELF = "Question.setFigure: ";
		debugPrint(SELF + "setting index ", index, " of type ", figure.type);
		if (index <= 0 || index > getNumFigures()) {
			throw new ParameterException("Invalid index to setFigure "
					+ index);
		}
		final Figure oldFigure = figures.get(index - 1);
		figure.figureId = oldFigure.figureId;
		if (figure.figureId > 0) figure.figureId *= -1;
		figures.set(index - 1, figure);
	} // setFigure(int, Figure)

	/** Adds a new figure to this question.
	 * @param	figure	the figure
	 */
	public void addNewFigure(Figure figure) {
		final String SELF = "Question.addNewFigure: ";
		debugPrint(SELF + "adding figure of type ", figure.type);
		debugPrint(SELF + "figure data = \n", figure.data);
		figure.serialNo = getLastFigureSerialNo() + 1;
		figures.add(figure);
	} // addNewFigure(Figure)

	/** Adds a new figure to this question.  Used by db to populate a
	 * question.
	 * @param	figure	the figure
	 */
	public void addFigure(Figure figure) {
		figure.serialNo = getLastFigureSerialNo() + 1;
		figures.add(figure);
	} // addFigure(Figure)

	/** Deletes a figure of this question.
	 * @param	index	1-based serial number of the figure
	 * @throws	ParameterException	if the figure serial number is invalid
	 */
	public void deleteFigure(int index) throws ParameterException {
		if (index <= 0 || index > getNumFigures()) {
			throw new ParameterException("Invalid index to getFigure "
					+ index);
		}
		final Figure fig = figures.remove(index - 1);
		if (fig.hasImage()) MolString.deleteImage(fig.bufferedImage);
	} // deleteFigure(int)

/* ************** Question datum-related operations **************/

	/** Gets all (both lists) of the question data of this question.
	 * @return	array of two arrays of question data
	 */
	public QDatum[][] getAllQData() {
		final QDatum[][] allQData = new QDatum[NUM_QDATA_LISTS][];
		for (int listNum = 0; listNum < NUM_QDATA_LISTS; listNum++) {
			allQData[listNum] = getQData(listNum);
		} // for each list of qData
		return allQData;
	} // getAllQData()

	/** Gets one of the lists of question data of this question.
	 * @param	listNum	which list of question data to get
	 * @return	array of question data
	 */
	public QDatum[] getQData(int listNum) {
		final String SELF = "Question.getQData: ";
		final List<QDatum> qDataList = qData.get(listNum);
		return qDataList.toArray(new QDatum[qDataList.size()]);
	} // getQData(int)

	/** Gets a question datum of this question.
	 * @param	listNum	which list of question data from which to get the 
	 * question datum 
	 * @param	index	1-based serial number of the question datum
	 * @return	a question datum
	 * @throws	ParameterException	if index is invalid
	 */
	public QDatum getQDatum(int listNum, int index) throws ParameterException {
		final List<QDatum> qdList = qData.get(listNum);
		if (index <= 0 || index > qdList.size()) {
			throw new ParameterException("Invalid index to getQDatum(): "
					+ index);
		}
		return qdList.get(index - 1);
	} // getQDatum(int, int)

	/** Populates this question with a question datum retrieved from the
	 * database or XML.  
	 * @param	qd	the question datum
	 */
	public void populateQDatum(QDatum qd) {
		qData.get(qd.isSubstitution() ? SUBSTNS : GENERAL).add(qd);
	} // populateQDatum(QDatum)

	/** Adds a new question datum to this question.
	 * @param	qd	the question datum to add
	 */
	public void addQDatum(QDatum qd) {
		addQDatum(qd, CONVERT_NESTED);
	} // addQDatum(QDatum)

	/** Adds a new question datum to this question.
	 * @param	qd	the question datum to add
	 * @param	convertNested	whether qData need to be converted from nested
	 * to postfix (yes for most editing sessions, no for importing from XML)
	 */
	public void addQDatum(QDatum qd, boolean convertNested) {
		final String SELF = "Question.addQDatum: ";
		final int listNum = (qd.isSubstitution() ? SUBSTNS : GENERAL);
		final List<QDatum> qdList = qData.get(listNum);
		final int oldNumQData = qdList.size();
		qd.serialNo = oldNumQData + 1;
		debugPrint(SELF + "new qDatum, ",
				qd instanceof EDiagramQDatum ? "an EDiagramQDatum" 
					: qd instanceof CaptionsQDatum ? "a CaptionsQDatum" 
					: "neither a CaptionsQDatum nor an EDiagramQDatum",
				" instance: dataId = ", qd.dataId, 
				" question id = ", qd.questionId, " serialNo = ", 
				qd.serialNo, " datatype = ", QDatum.DBVALUES[qd.dataType],
				" existing num qData = ", oldNumQData);
		if (qd.isSynSMExpression() && convertNested) qd.nestedToPostfix();
		qdList.add(qd);
	} // addQDatum(QDatum, boolean)

	/** Replaces a question datum of this question with a new question datum.
	 * @param	index 1-based serial number of the question datum to be replaced
	 * @param	qd	the new question datum
	 * @throws	ParameterException	if index is invalid
	 */
	public void setQDatum(int index, QDatum qd) throws ParameterException {
		final String SELF = "Question.setQDatum: ";
		final boolean substnList = qd.isSubstitution();
		final List<QDatum> qdList = 
				qData.get(substnList ? SUBSTNS : GENERAL);
		if (index <= 0 || index > qdList.size()) {
			throw new ParameterException("Invalid index to setQDatum(): "
					+ index);
		}
		debugPrint(SELF + "setting index ", index, " of qData, which is ",
				qd instanceof EDiagramQDatum ? "an EDiagramQDatum" 
					: qd instanceof CaptionsQDatum ? "a CaptionsQDatum" 
					: "neither a CaptionsQDatum nor an EDiagramQDatum",
				" instance.");
		if (qd instanceof EDiagramQDatum) {
			final EDiagramQDatum eqd = (EDiagramQDatum) qd;
			debugPrint(SELF + "captions = ", eqd.captions,
					", labels = ", eqd.labels); /**/
		} // if an eDiagram qDatum
		final QDatum oldqd = qdList.get(index - 1);
		/* In QuestionRead, dataId is changed back to positive.  The negation is
		a mark to show that the question's question data is modified and should 
		be saved in db. */
		qd.dataId = -oldqd.dataId;
		qd.serialNo = oldqd.serialNo; // old serial number should be retained
		qd.phraseId = 0; // changed qData should have translations removed
		if (qd.isSynSMExpression()) qd.nestedToPostfix();
		qdList.set(index - 1, qd);
	} // setQDatum(int, QDatum)

	/** Moves a question datum of this question.
	 * @param	listNum	which list of question data to modify
	 * @param	from	1-based serial number of the question datum
	 * @param	to	new 1-based serial number of the question datum
	 * @throws	ParameterException	if from or to is out of range
	 */
	public void moveQDatum(int listNum, int from, int to) 
			throws ParameterException {
		final String SELF = "Question.moveQDatum: ";
		debugPrint(SELF + "listNum = ", listNum, 
				", from = ", from, " to = ", to);
		final List<QDatum> qdList = qData.get(listNum);
		final int[] range = new int[] {1, qdList.size()};
		if (!MathUtils.inRange(from, range) || !MathUtils.inRange(to, range)) {
			throw new ParameterException("Invalid indices to moveQDatum(): " 
					+ from + ", " + to);
		}
		qdList.add(to - 1, qdList.remove(from - 1));
		// renumber qData between old and new positions,
		// and change dataID to < 0 to record in database
		final int offset = (listNum == GENERAL ? 0 : getNumQData(GENERAL));
		final int start = (to < from ? to : from) - 1;
		final int end = (to > from ? to : from) - 1;
		for (int qdNum = start; qdNum <= end; qdNum++) {
			final QDatum qDatum = qdList.get(qdNum);
			qDatum.serialNo = qdNum + 1 + offset;
			if (qDatum.dataId > 0) qDatum.dataId = -qDatum.dataId;
			debugPrint(SELF + "qDatum ", qdNum + 1, 
					" has serialNo = ", qDatum.serialNo,
					", dataId = ", qDatum.dataId,
					", data = ", qDatum.data);
		} // for each qDatum whose serial number changes
	} // moveQDatum(int, int, int)

	/** Deletes a question datum of this question.
	 * @param	listNum	which list of question data to modify
	 * @param	index	1-based serial number of the question datum
	 * @throws	ParameterException	if the question datum serial number is 
	 * invalid
	 */
	public void deleteQDatum(int listNum, int index) throws ParameterException {
		final String SELF = "Question.deleteQDatum: ";
		debugPrint(SELF + "listNum = ", listNum, ", index = ", index);
		final List<QDatum> qdList = qData.get(listNum);
		final int numQData = qdList.size();
		if (index <= 0 || index > numQData) {
			throw new ParameterException("Invalid index to deleteQDatum(): "
					+ index);
		}
		final int offset = (listNum == GENERAL ? 0 : getNumQData(GENERAL));
		for (int qdNum = index; qdNum < numQData; qdNum++) {
			final QDatum qd = qdList.get(qdNum);
			qd.serialNo = qdNum + offset;
			if (qd.dataId >= 0) qd.dataId = -qd.dataId; // to indicate modification
		} // for each qDatum whose serial number changes
		qdList.remove(index - 1);
		// for debugging
		/* int qdNum = 0;
		for (final QDatum qDatum : qdList) {
			debugPrint(SELF + "qDatum ", ++qdNum, " has serialNo = ", 
					qDatum.serialNo, ", dataId = ", qDatum.dataId,
					", dataType = ", QDatum.DBVALUES[qDatum.dataType],
					", data = ", qDatum.data);
		} // for each qDatum /**/
	} // deleteQDatum(int, int)

	/** Gets the display text of the general question data.
	 * @return	the display text of the question data
	 * @throws	ParameterException	never
	 */
	public String[] getQDataTexts() throws ParameterException {
		final String SELF = "Question.getQDataTexts: ";
		final List<QDatum> qdList = qData.get(GENERAL);
		final int numQData = qdList.size();
		String[] texts = null;
		if (numQData > 0) {
			if (isNumeric() || isRank() || isChoice() 
					|| isFillBlank() || isChooseExplain()) {
				debugPrint(SELF + "returning a value for major Q type = ", 
						getQTypeDescription());
				texts = getQDataDisplay();
			} else if (isRCD()) {
				debugPrint(SELF + "returning a value for RCD.");
				texts = ((EDiagramQDatum) qdList.get(0)).labels;
			} // if question type
		} // if there are question data
		if (texts == null) debugPrint(SELF + "returning null for qData texts; "
				+ "major Q type = ", getQTypeDescription(), ", numQData = ", 
				numQData);
		return texts;
	} // getQDataTexts()

	/** Gets the short display text of the general question data of this 
	 * question.
	 * @return	array of short display texts of the question data
	 */
	private String[] getQDataDisplay() {
		final String SELF = "Question.getQDataDisplay: ";
		final List<QDatum> qdList = qData.get(GENERAL);
		final String[] texts = new String[qdList.size()];
		int qdNum = 0;
		for (final QDatum qDatum : qdList) {
			texts[qdNum] = qDatum.toShortDisplay(chemFormatting(),
					!QDatum.ADD_CHEM_STRUCT);
			debugPrint(SELF + "qDatum ", qdNum + 1, " display: ", texts[qdNum]);
			qdNum++;
		} // for each qDatum
		return texts;
	} // getQDataDisplay()

	/** Finds whether there is a question datum describing how to combine the
	 * conditions on the permissible starting materials, and, if so, whether it 
	 * is the last question datum or whether there is more than one such datum.
	 * Called by authortool/question.jsp.
	 * @return	NO_EXPR, LAST_QDATUM, NOT_LAST_QDATUM, or TWO_EXPRNS
	 */
	public int findSynthCombExpr() {
		int exprFound = NO_EXPR;
		int qdNum = 0;
		final int numQDataGeneral = getNumQData(GENERAL);
		for (final QDatum qDatum : getQData(GENERAL)) {
			if (qDatum.isSynSMExpression()) {
				exprFound = (exprFound != NO_EXPR ? TWO_EXPRNS
						: qdNum == numQDataGeneral - 1 ? LAST_QDATUM
						: NOT_LAST_QDATUM);
			} // if found an expression for combining rules
			qdNum++;
		} // for each qdNum
		return exprFound;
	} // findSynthCombExpr()

	/** Gets whether this question is of the type to have translatable question 
	 * data.
	 * @return	true if this question is of the type to have translatable
	 * question data.
	 */
	public boolean hasTranslatableQData() {
		return isChoice() || isChooseExplain() || isFillBlank() || isRank(); 
	} // hasTranslatableQData()

/* ************** Evaluator-related operations **************/

	/** Adds a new evaluator to this question.  Used by authoring tool
	 * and by db to populate a question.
	 * @param	eval	the evaluator
	 */
	public void addEvaluator(Evaluator eval) {
		allEvaluators.add(eval);
	} // addEvaluator(Evaluator)

	/** Gets an evaluator of this question.
	 * @param	index	1-based serial number of the evaluator
	 * @return	an evaluator
	 * @throws	ParameterException	if index is invalid
	 */
	public Evaluator getEvaluator(int index) throws ParameterException {
		if (index <= 0 || index > getNumEvaluators()) {
			throw new ParameterException("Invalid index to getEvaluator: "
					+ index);
		}
		return allEvaluators.get(index - 1);
	} // getEvaluator(int)

	/** Gets the evaluators of this question.
	 * @return	array of evaluators
	 */
	public Evaluator[] getAllEvaluators() {
		return allEvaluators.toArray(new Evaluator[getNumEvaluators()]);
	} // getAllEvaluators()

	/** Deletes an evaluator of this question.
	 * @param	evalNum	1-based serial number of the evaluator
	 * @throws	ParameterException	if index is invalid
	 */
	public void deleteEvaluator(int evalNum) throws ParameterException {
		if (evalNum <= 0 || evalNum > getNumEvaluators())
			throw new ParameterException("Invalid index to deleteEvaluator: "
					+ evalNum);
		allEvaluators.remove(evalNum - 1);
	} // deleteEvaluator(int)

	/** Duplicates an evaluator of this question.
	 * @param	evalNum	1-based serial number of the evaluator
	 * @throws	ParameterException	if index is invalid
	 */
	public void cloneEvaluator(int evalNum) throws ParameterException {
		if (evalNum <= 0 || evalNum > getNumEvaluators())
			throw new ParameterException("Invalid index to cloneEvaluator: "
					+ evalNum);
		final Evaluator eval = getEvaluator(evalNum);
		final Evaluator newEval = new Evaluator(eval);
		newEval.clearIds();
		allEvaluators.add(evalNum, newEval); // after the current evaluator
	} // cloneEvaluator(int)

	/** Splits a complex evaluator into its individual components.
	 * @param	evalNum	1-based serial number of the evaluator
	 * @throws	ParameterException	if index is invalid
	 */
	public void splitEvaluator(int evalNum) throws ParameterException {
		final Evaluator parentEval = getEvaluator(evalNum);
		final List<Subevaluator> subevals = parentEval.getSubevaluators();
		int newEvalNum = evalNum;
		parentEval.exprCode = "";
		while (subevals.size() > 1) {
			final Subevaluator subeval = subevals.remove(1);
			final Evaluator newEval = new Evaluator(parentEval);
			final List<Subevaluator> newSubevals =
					new ArrayList<Subevaluator>();
			newSubevals.add(subeval);
			newEval.setSubevaluators(newSubevals);
			newEval.clearIds(); // new evaluator
			allEvaluators.add(newEvalNum++, newEval);
		} // while the parent evaluator has more than one subevaluator
		// mark that original parent evaluator has changed
		if (parentEval.majorId > 0) parentEval.majorId *= -1; 
	} // splitEvaluator(int)

	/** Create a new evaluator by joining the contents of two or more 
	 * evaluators temporarily.  Does NOT alter the question.
	 * @param	evalNums	1-based evaluator serial numbers to join
	 * @return	the joined evaluator
	 * @throws	ParameterException	if there are not at least two evaluators to
	 * join
	 */
	public Evaluator getJoinedEvaluator(int[] evalNums)
			throws ParameterException  {
		final String SELF = "Question.getJoinedEvaluator: ";
		if (evalNums.length <= 1)
			throw new ParameterException(SELF + "invalid parameters");
		// final int newEvalNum = getNumEvaluators() + 1 - evalNums.length; // unused 11/6/2012
		// make copy of first evaluator in list
		final Evaluator newEval = 
				new Evaluator(getEvaluator(evalNums[0]));
		final List<Subevaluator> newSubevals = 
				newEval.getSubevaluators();
		for (int numNum = 1; numNum < evalNums.length; numNum++) {
			final Evaluator subsumedEval = getEvaluator(evalNums[numNum]);
			newSubevals.addAll(subsumedEval.getSubevaluators());
		} // for each subsequent evaluator to be combined
		newEval.setDefaultExprCode();
		return newEval;
	} // getJoinedEvaluator(int[])

	/** Join one or more evaluators.
	 * @param	evalNums	1-based evaluator serial numbers to join
	 * @param	newEval	the newly joined evaluator containing the expression
	 * code, grade, and feedback as well as subevaluators
	 * @throws	ParameterException	if there are not at least two evaluators to
	 * join
	 */
	public void joinEvaluators(int[] evalNums, Evaluator newEval) 
			throws ParameterException {
		final String SELF = "Question.joinEvaluators: ";
		if (evalNums.length <= 1)
			throw new ParameterException(SELF + "invalid parameters");
		debugPrint(SELF + "joining evaluators ", evalNums,
				" and setting at 1-based position ", evalNums[0]);
		newEval.clearIds();
		allEvaluators.set(evalNums[0] - 1, newEval);
		for (int numNum = evalNums.length - 1; numNum >= 1; numNum--) {
			debugPrint(SELF + "removing evaluator ", evalNums[numNum]);
			allEvaluators.remove(evalNums[numNum] - 1);
		} // for each evaluator to be removed
	} // joinEvaluators(int[], Evaluator)

	/** Moves an evaluator of this question.
	 * @param	from	1-based position of the evaluator
	 * @param	to	new 1-based position of the evaluator
	 * @throws	ParameterException	if from or to is out of range
	 */
	public void moveEvaluator(int from, int to) throws ParameterException {
		final String SELF = "Question.moveEvaluator: ";
		debugPrint(SELF + "from = ", from, " to = ", to);
		final int[] range = new int[] {1, getNumEvaluators()};
		if (!MathUtils.inRange(from, range) || !MathUtils.inRange(to, range))
			throw new ParameterException("Invalid starting indices " + from 
					+ ", " + to + " to moveEvaluator");
		final Evaluator eval = allEvaluators.remove(from - 1);
		eval.majorId = 0;
		allEvaluators.add(to - 1, eval);
		/* int evalNum = 0;
		for (final Evaluator oneEval : allEvaluators) {
			debugPrint(SELF + "eval ", ++evalNum, " has majorId = ",
					oneEval.majorId, ", feedback = " + oneEval.feedback);
		} // for each evaluator /**/
	} // moveEvaluator(int, int)

	/** Sets new data for an evaluator of this question.
	 * @param	evalNum	1-based serial number of the evaluator
	 * @param	newEval	the new evaluator
	 * @throws	ParameterException	if the evaluator index is out of range
	 */
	public void setEvaluator(int evalNum, Evaluator newEval)
			throws ParameterException {
		setEvaluator(evalNum, 0, newEval);
	} // setEvaluator(int, Evaluator)

	/** Sets new data for an evaluator of this question.
	 * @param	evalNum	1-based serial number of the evaluator
	 * @param	subevalNum	1-based serial number of the subevaluator, or 0 if
	 * replacing the whole evaluator
	 * @param	newEval	the new evaluator feedback
	 * @throws	ParameterException	if the evaluator or subevaluator indices 
	 * are out of range
	 */
	public void setEvaluator(int evalNum, int subevalNum, Evaluator newEval) 
			throws ParameterException {
		final String SELF = "Question.setEvaluator: ";
		if (subevalNum == 0) {
			// negate id to denote changed if not already negated
			if (newEval.majorId > 0) newEval.majorId = -newEval.majorId;
			// changed top-level evaluators should have translations removed
			newEval.phraseId = 0;
			allEvaluators.set(evalNum - 1, newEval);
			debugPrint(SELF + "setting new value for evaluator ", evalNum);
		} else {
			final Evaluator oldEval = allEvaluators.get(evalNum - 1);
			oldEval.setSubevaluator(subevalNum, newEval.getSubevaluator(1));
			// negate id to denote changed if not already negated
			if (oldEval.majorId > 0) oldEval.majorId = -oldEval.majorId;
			debugPrint(SELF + "setting new value for subevaluator ", subevalNum,
					" of evaluator ", evalNum);
		} // if subevalNum
	} // setEvaluator(int, int, Evaluator)

/* ************** Miscellaneous authoring operations **************/

	/** Checks whether a question can be saved; puts any error
	 * messages into miscMessages.
	 * @return	whether the evaluators are valid.
	 */
	public boolean validate() {
		miscMessage = "";
		// There must be either a figure, a statement, or, 
		// if rank or choice, question data
		boolean passed = true;
		if (statement.trim().length() == 0 && figures.isEmpty())
			passed = (isRank() || isChoice()) && qData.size() >= 2;
		if (!passed) {
			miscMessage = Utils.toString(miscMessage,
					"The question must have at least one figure",
					isRank() || isChoice() 
						? ", one statement, or two items to order or choose"
						: " or statement",
					". <br />");
		} // if passed
		return passed;
	} // validate()

	/** Makes a filename to contain a figure image.
	 * @param	nameStart	unique ID of preexisting figure, or
	 * serial number for a new Figure
	 * @param	extension	extension to add to the filename; empty String if
	 * should not add one
	 * @return	name of file to which to write figure
	 */
	public String makeImageFileName(int nameStart, String extension) {
		return makeImageFileName(String.valueOf(nameStart), extension);
	} // makeImageFileName(int, String)

	/** Makes a filename to contain a figure or qDatum image.
	 * @param	nameStart	unique ID of preexisting figure or question
	 * datum, or serial number for a new Figure
	 * @param	extension	extension to add to the filename; empty String if
	 * should not add one
	 * @return	name of file to which to write figure
	 */
	public String makeImageFileName(String nameStart, String extension) {
		return makeImageFileName(nameStart, extension,
				masterQ ? "" : "user_", null);
	} // makeImageFileName(String, String)

	/** Makes a filename to contain a figure image.
	 * @param	nameStart	unique ID of preexisting figure, or
	 * serial number for a new Figure
	 * @param	extension	extension to add to the filename; empty String if
	 * should not add one
	 * @param	dirPrefix	the prefix for the name of the directory: "user_"
	 * for local questions, "" for master questions
	 * @return	name of file to which to write figure
	 */
	public String makeImageFileName(int nameStart, String extension, 
			String dirPrefix) {
		return makeImageFileName(String.valueOf(nameStart), extension, 
				dirPrefix, null);
	} // makeImageFileName(int, String, String)

	/** Makes a filename to contain a figure image.
	 * @param	nameStart	unique ID of preexisting figure, or
	 * serial number for a new Figure
	 * @param	extension	extension to add to the filename; empty String if
	 * should not add one
	 * @param	rGroups	names of the R groups associated with this figure
	 * @return	name of file to which to write figure
	 */
	public String makeImageFileName(int nameStart, String extension,
			String[] rGroups) {
		return makeImageFileName(String.valueOf(nameStart), extension, rGroups);
	} // makeImageFileName(int, String, String[])

	/** Makes a filename to contain a figure image.
	 * @param	nameStart	unique ID of preexisting figure, or
	 * serial number for a new Figure
	 * @param	extension	extension to add to the filename; empty String if
	 * should not add one
	 * @param	rGroups	names of the R groups associated with this figure
	 * @return	name of file to which to write figure
	 */
	public String makeImageFileName(String nameStart, String extension,
			String[] rGroups) {
		return makeImageFileName(nameStart, extension,
				masterQ ? "" : "user_", rGroups);
	} // makeImageFileName(String, String, String[])

	/** Makes a filename to contain a figure image.
	 * @param	nameStart	unique ID of preexisting figure, or
	 * serial number for a new Figure
	 * @param	extension	extension to add to the filename; empty String if
	 * should not add one
	 * @param	dirPrefix	the prefix for the name of the directory: "user_"
	 * for local questions, "" for master questions
	 * @param	rGroups	names of the R groups associated with this figure
	 * @return	name of file to which to write figure
	 */
	public String makeImageFileName(int nameStart, String extension, 
			String dirPrefix, String[] rGroups) {
		return makeImageFileName(String.valueOf(nameStart), extension,
				dirPrefix, rGroups);
	} // makeImageFileName(int, String, String, String[])

	/** Makes a filename to contain a figure or qDatum image.
	 * @param	nameStart	unique ID of preexisting figure, or
	 * serial number for a new Figure
	 * @param	extension	extension to add to the filename; empty String if
	 * should not add one
	 * @param	dirPrefix	the prefix for the name of the directory: "user_"
	 * for local questions, "" for master questions
	 * @param	rGroups	names of the R groups associated with this figure
	 * @return	name of file to which to write figure
	 */
	String makeImageFileName(String nameStart, String extension, 
			String dirPrefix, String[] rGroups) {
		final String SELF = "Question.makeImageFileName: ";
		debugPrint(SELF + "dirPrefix = ", dirPrefix, 
				", relFiguresDir = ", AppConfig.relFiguresDir, 
				", nameStart = ", nameStart, ", rGroups = ", rGroups,
				", extension = ", extension);
		final StringBuilder nameBld = new StringBuilder();
		if (!Utils.isEmpty(dirPrefix)) nameBld.append(dirPrefix);
		nameBld.append(AppConfig.relFiguresDir);
		if (!AppConfig.relFiguresDir.endsWith("/")) nameBld.append('/');
		nameBld.append(qId == 0 ? Utils.getRandName()
				: Utils.getBuilder(qId, '_', nameStart));
		if (!Utils.isEmpty(rGroups)) {
			for (final String rGroup : rGroups) {
				Utils.appendTo(nameBld, '_', rGroup);
			} // for each R group
		} // if there are R groups in the name
		if (!Utils.isEmpty(extension)) {
			Utils.appendTo(nameBld, '.', extension);
		} // if extension
		return nameBld.toString();
	} // makeImageFileName(String, String, String, String[])

/* *************** Import-Export ******************/

	/** Converts a question to XML.
	 * @return	list of strings: [0] contains the XML, any further ones
	 * contain image filenames
	 * @throws	ParameterException	never
	 */
	public List<String> toXML() throws ParameterException {
		final String SELF = "Question.toXML: ";
		debugPrint(SELF, statement);
		final List<String> xmlAndImageNames = new ArrayList<String>();
		final StringBuilder xml = Utils.getBuilder(startQuestion());
		// xml: statement
		if (!Utils.isEmptyOrWhitespace(statement)) {
			xml.append(makeNode(STATEMENT_TAG, statement));
		}
		final TranslnsMap qStmtTranslns = 
				QSetAllTranslns.getAllQStmtTranslns(qId);
		final List<String> qStmtLangs = qStmtTranslns.getLanguages();
		for (final String language : qStmtLangs) {
			final String translation = qStmtTranslns.get(language);
			if (!ENGLISH.equals(language)) {
				xml.append(makeTranslnNode(language, translation));
			} // if language is not English (shouldn't be)
		} // for each translated statement
		// xml: question flags, book chapter, remarks, keywords
		Utils.appendTo(xml, makeNode(FLAGS_TAG, qFlags | (long) qType), // legacy
				makeNode(QTYPE_TAG, DB_QTYPES[qType]),
				makeNode(QFLAGS_TAG, qFlags), makeNode(BOOK_TAG, book),
				makeNode(CHAPTER_TAG, chapter),
				makeNode(Q_REMARKS_TAG, remarks));
		if (keywords != null && keywords.length() > 0) {
			xml.append(makeNode(KEYWORDS_TAG, keywords));
		}
		xml.append('\n');
		for (final Figure fig : figures) {
			final List<String> figureXML = fig.toXML();
			Utils.appendTo(xml, figureXML.remove(0), "\n\n");
			xmlAndImageNames.addAll(figureXML);
		} // for each figure
		debugPrint(SELF + "exporting question data.");
		final int[] QD_TYPES = new int[] {GENERAL, SUBSTNS};
		for (final int qdType : QD_TYPES) {
			final List<QDatum> qdList = qData.get(qdType);
			final int numQData = qdList.size();
			if (numQData > 0) {
				final Map<Integer, TranslnsMap> qDataTranslns =
						(qdType == GENERAL // R-group collections don't have translations
						? QSetAllTranslns.getAllQDataTranslns(qId)
						: new HashMap<Integer, TranslnsMap>());
				for (int qdNum = 1; qdNum <= numQData; qdNum++) {
					final QDatum qDatum = qdList.get(qdNum - 1);
					final Integer qdNumObj = Integer.valueOf(qdNum);
					final List<String> qDatumXML = 
							qDatum.toXML(qDataTranslns.get(qdNumObj));
					Utils.appendTo(xml, qDatumXML.remove(0), "\n\n");
					xmlAndImageNames.addAll(qDatumXML);
				} // for each qDatum in the list
			} // if there are qData in this list
		} // for each list of qData
		final int numEvals = getNumEvaluators();
		final Map<Integer, TranslnsMap> feedbackTranslns =
				QSetAllTranslns.getAllFeedbackTranslns(qId);
		debugPrint(SELF + "exporting evaluators.");
		for (int evalNum = 1; evalNum <= numEvals; evalNum++) {
			final Evaluator eval = getEvaluator(evalNum);
			final Integer evalNumObj = Integer.valueOf(evalNum);
			Utils.appendTo(xml, 
					eval.toXML(feedbackTranslns.get(evalNumObj)), "\n\n");
		} // for each evaluator
		xmlAndImageNames.add(0, Utils.toString(xml, '\n', endQuestion()));
		return xmlAndImageNames;
	} // toXML()

	/** Wraps the opening tag and its id and type attributes with &lt; &gt;.
	 * @return	the tag and its id and type attributes wrapped in &lt; &gt;
	 */
	private StringBuilder startQuestion() {
		return XMLUtils.startTag(QUESTION_TAG, new String[][] {
					{QID_TAG, String.valueOf(qId)}
				});
	} // startQuestion()

	/** Surround the given text with the given XML tags.
	 * @param	tag	the name of the XML tag
	 * @param	text	the text of the node
	 * @return	the text surrounded by XML tags
	 */
	private StringBuilder makeNode(String tag, String text) {
		return XMLUtils.makeNode(tag, text);
	} // makeNode(String, String)

	/** Surround the given text with the given XML tags.
	 * @param	tag	the name of the XML tag
	 * @param	text	the text of the node
	 * @return	the text surrounded by XML tags
	 */
	private StringBuilder makeNode(String tag, long text) {
		return XMLUtils.makeNode(tag, text);
	} // makeNode(String, long)

	/** Surround a translation with appropriate XML tags.
	 * @param	language	the language
	 * @param	transln	the translation
	 * @return	the translation wrapped in XML tags
	 */
	private StringBuilder makeTranslnNode(String language, String transln) {
		return XMLUtils.makeTranslnNode(language, transln);
	} // makeTranslnNode(String, String)

	/** Wraps the closing tag with &lt;/ &gt;.
	 * @return	the tag wrapped in &lt;/ &gt;
	 */
	private StringBuilder endQuestion() {
		return XMLUtils.endTag(QUESTION_TAG);
	} // endQuestion()

	/** Imports a question from XML.
	 * @param	node	the XML of the question
	 * @return	the question
	 * @throws	ParserException	if the node can't be parsed
	 */
	public static Question parseXML(Node node) throws ParserException {
		final String SELF = "Question.parseXML: ";
		final NodeList nodeList = node.getChildNodes();
		final Question question = new Question();
		final String figureTag = Figure.getTag();
		final String evalTag = Evaluator.getTag();
		final String qDatumTag = QDatum.getTag();
		for (int nodeNum = 0; nodeNum < nodeList.getLength(); nodeNum++) {
			final Node n = nodeList.item(nodeNum);
			if (n.getNodeType() == Node.TEXT_NODE) continue;
			final String nodeName = n.getNodeName();
			debugPrint(SELF + "nodeName ",
					nodeNum + 1, " = ", nodeName);
			if (nodeName == null) continue;
			if (nodeName.equalsIgnoreCase(figureTag)) {
				debugPrint(SELF + "getting parsed Figure.");
				final Figure figure = Figure.parseXML(n);
				debugPrint(SELF + "adding parsed Figure to Q.");
				question.addNewFigure(figure);
			} else if (nodeName.equalsIgnoreCase(evalTag)) {
				debugPrint(SELF + "getting parsed Evaluator.");
				final Evaluator eval = Evaluator.parseXML(n);
				debugPrint(SELF + "adding parsed Evaluator to Q.");
				question.addEvaluator(eval);
			} else if (nodeName.equalsIgnoreCase(qDatumTag)) {
				debugPrint(SELF + "getting parsed Q data.");
				final QDatum qDatum = QDatum.parseXML(n);
				debugPrint(SELF + "adding parsed Q data to Q.");
				question.addQDatum(qDatum, !CONVERT_NESTED);
			} else {
				final Node child = n.getFirstChild();
				final String nodeValue = (child == null ? ""
						: child.getNodeValue().trim());
				if (nodeName.equalsIgnoreCase(STATEMENT_TAG)) {
					question.setStatement(nodeValue);
				} else if (nodeName.equalsIgnoreCase(QTYPE_TAG)) {
					question.setQType(Utils.indexOf(DB_QTYPES, nodeValue));
				} else if (nodeName.equalsIgnoreCase(QFLAGS_TAG)) {
					question.setQFlags(Long.parseLong(nodeValue));
				} else if (nodeName.equalsIgnoreCase(FLAGS_TAG)) { // legacy
					final long qTypeAndFlags = Long.parseLong(nodeValue);
					question.setQType((int) (qTypeAndFlags & MAJORTYPEMASK));
					question.setQFlags(qTypeAndFlags & FLAGSMASK);
				} else if (nodeName.equalsIgnoreCase(BOOK_TAG)) {
					question.setBook(nodeValue);
				} else if (nodeName.equalsIgnoreCase(CHAPTER_TAG)) {
					question.setChapter(nodeValue);
				} else if (nodeName.equalsIgnoreCase(Q_REMARKS_TAG)) {
					question.setRemarks(nodeValue);
				} else if (nodeName.equalsIgnoreCase(KEYWORDS_TAG)) {
					question.setKeywords(nodeValue);
				}
			}
		} // for each node
		return question;
	} // parseXML

	/** Extracts question translations from XML.
	 * @param	node	information about the question
	 * @return	translations of question statement, evaluators, and question
	 * data
	 */
	public QTranslns getAllTranslations(Node node) {
		final String SELF = "Question.getAllTranslns: ";
		final QTranslns translns = new QTranslns();
		final NodeList nodeList = node.getChildNodes();
		final String qDatumTag = QDatum.getTag();
		final String evalTag = Evaluator.getTag();
		int qdNum = 0;
		int evalNum = 0;
		boolean foundTransln = false;
		for (int nodeNum = 0; nodeNum < nodeList.getLength(); nodeNum++) {
			final Node n = nodeList.item(nodeNum);
			if (n.getNodeType() == Node.TEXT_NODE) continue;
			final String nodeName = n.getNodeName();
			if (nodeName == null) continue;
			if (nodeName.equalsIgnoreCase(qDatumTag)) {
				final TranslnsMap qDataTranslns = QDatum.getAllTranslns(n);
				final int numQDataTranslns = qDataTranslns.size();
				translns.putQDatumTranslations(qdNum++, qDataTranslns);
				debugPrint(SELF, numQDataTranslns, " translation(s) found "
						+ "for qdNum ", qdNum, " of ", getNumQData(GENERAL));
				if (numQDataTranslns > 0) foundTransln = true;
			} else if (nodeName.equalsIgnoreCase(evalTag)) {
				final TranslnsMap evalTranslns = Evaluator.getAllTranslns(n);
				final int numEvalTranslns = evalTranslns.size();
				translns.putFeedbackTranslations(evalNum++, evalTranslns);
				debugPrint(SELF, numEvalTranslns, " translation(s) found "
						+ "for evalNum ", evalNum, " of ", getNumEvaluators());
				if (numEvalTranslns > 0) foundTransln = true;
			} else if (nodeName.equalsIgnoreCase(TRANSLATION_TAG)) {
				final Node langNode = 
						n.getAttributes().getNamedItem(LANGUAGE_TAG);
				if (langNode != null) {
					final String language = langNode.getNodeValue();
					final Node child = n.getFirstChild();
					final String transln = (child == null ? ""
							: child.getNodeValue().trim());
					if (language != null && !ENGLISH.equals(language)
							&& !Utils.isEmpty(transln)) {
						translns.getQStmt().put(language, transln);
					} // if we have a good translation
				} // if a language is specified
			} // if nodeName
		} // for each node
		final int numQStmtTranslns = translns.getQStmt().size();
		debugPrint(SELF, numQStmtTranslns, " translation(s) found for qStmt");
		if (numQStmtTranslns > 0) foundTransln = true;
		return (foundTransln ? translns : null);
	} // getAllTranslations(Node)

	/** Gets question translations from the database. Called by
	 * QSet.addQuestion when a cloned question is being added.
	 * @return	translations of question statement, evaluators, and question
	 * data
	 * @throws	ParameterException	never
	 */
	public QTranslns getAllTranslations() throws ParameterException {
		final QTranslns translns = new QTranslns();
		final String qStmt = Utils.trimNullToEmpty(statement);
		if (origStatement != null && origStatement.trim().equals(qStmt)) {
			translns.setQStmtTranslations(
					QSetAllTranslns.getAllQStmtTranslns(qId));
		} // if origStatement
		translns.setFeedbackTranslations(
				QSetAllTranslns.getAllFeedbackTranslns(qId));
		translns.setQDatumTranslations(
				QSetAllTranslns.getAllQDataTranslns(qId));
		int foundTransln = translns.getQStmt().size();
		for (int qdNum = 1; qdNum <= getNumQData(GENERAL); qdNum++) {
			if (qData.get(GENERAL).get(qdNum - 1).phraseId != 0) {
				final TranslnsMap qDatumTranslns =
						translns.getQDatumTranslations(qdNum);
				if (qDatumTranslns != null) {
					foundTransln += qDatumTranslns.size();
				} // if there are qDatum translations
			} // if qDatum has new translations
		} // for each qData's set of translations
		for (int evalNum = 1; evalNum <= getNumEvaluators(); evalNum++) {
			if (getEvaluator(evalNum).phraseId != 0) {
				final TranslnsMap fdbkTranslns =
						translns.getFeedbackTranslations(evalNum);
				if (fdbkTranslns != null) {
					foundTransln += fdbkTranslns.size();
				} // if there are feedback translations
			} // if evaluator has new translations
		} // for each eval's set of translations
		debugPrint("Question.getAllTranslations: found ",
				foundTransln, " new translation(s).");
		return (foundTransln > 0 ? translns : null);
	} // getAllTranslations()

} // Question
