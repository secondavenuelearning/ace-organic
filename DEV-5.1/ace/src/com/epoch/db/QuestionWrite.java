package com.epoch.db;

import static com.epoch.db.dbConstants.QuestionsRWConstants.*;
import static com.epoch.db.dbConstants.QuestionWriteConstants.*;
import static com.epoch.db.dbConstants.ResponsesConstants.*;
import com.epoch.constants.AuthorConstants;
import com.epoch.evals.Evaluator;
import com.epoch.evals.Subevaluator;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.ParameterException;
import com.epoch.genericQTypes.genericQConstants.TableQConstants;
import com.epoch.qBank.qBankConstants.FigConstants;
import com.epoch.qBank.qBankConstants.CaptionsQDatumConstants;
import com.epoch.qBank.Figure;
import com.epoch.qBank.QDatum;
import com.epoch.qBank.Question;
import com.epoch.qBank.CaptionsQDatum;
import com.epoch.qBank.EDiagramQDatum;
import com.epoch.utils.Utils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Instantiable class containing methods to write a question to the database.
 * <P>Notes about figures:
 * Serial numbers are set by the order in which figures appear in a Q.
 * The order of existing figures can't be changed, but if a figure is
 * deleted, when the Q is stored, the other figures' serial numbers are
 * resequenced.  The figureId is set to negative if a figure has been
 * modified.
 */
final class QuestionWrite extends DBCommon 
		implements AuthorConstants, FigConstants, 
			CaptionsQDatumConstants, TableQConstants {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

	/** Contains the data of the question being written to the database;
	 * some component ID numbers may be negative to indicate that they are new. */
	transient private Question question;
	/** The question as written to the database. */
	transient private Question changedQ;
	/** ID of the question set containing the question being written to the 
	 * database. */
	transient private int setId;
	/** Login ID of the question author (null if master). */
	transient private String authorId = MASTER_AUTHOR;
	/** Connection to the database (pooled). */
	transient private Connection con;
	/** Master or local database tables. */
	transient private DBTables tables;
	/** Contains oft-used SQL phrase. */
	transient private SQLWithQMarks ifLocalAndQAuthorIs;

/* *********** Constructors ****************/

	/** Constructor. 
	 * @param	authId	login ID of the question author
	 */
	QuestionWrite(String authId) { 
		initiate(authId);
	} // QuestionWrite(String)

	/** Constructor.  
	 * @param	connxn	an established database connection (pooled)
	 */
	QuestionWrite(Connection connxn) { 
		con = connxn;
		initiate(MASTER_AUTHOR);
	} // QuestionWrite(Connection)

	/** Constructor.  
	 * @param	connxn	an established database connection (pooled)
	 * @param	authId	login ID of the question author
	 */
	QuestionWrite(Connection connxn, String authId) { 
		con = connxn;
		initiate(authId);
	} // QuestionWrite(Connection, String)

	/** Constructor. 
	 * @param	oneQ	question to be written
	 * @param	authId	login ID of the question author
	 */
	QuestionWrite(Question oneQ, String authId) { 
		question = oneQ;
		initiate(authId);
	} // QuestionWrite(Question, String)

	/** Constructor. 
	 * @param	oneQ	question to be written
	 * @param	qSetId	set into which the question will be written
	 * @param	authId	login ID of the question author
	 */
	QuestionWrite(Question oneQ, int qSetId, String authId) { 
		question = oneQ;
		setId = qSetId;
		initiate(authId);
	} // QuestionWrite(Question, int, String)

	/** Initiates some global values. 
	 * @param	authId	login ID of the question author
	 */
	private void initiate(String authId) {
		authorId = authId;
		tables = getTables(authorId != MASTER_AUTHOR);
		if (tables.local) {
			ifLocalAndQAuthorIs = new SQLWithQMarks(toString(
					AND, Q_AUTHOR(), EQUALS + QMARK), authorId);
		} else {
			ifLocalAndQAuthorIs = new SQLWithQMarks();
		} // if local
	} // initiate()

/* *********** Add and set methods ****************/

	/** Add a new master-authored question to a question set,
	 * falling back under failure.
	 * @return	question that was written
	 * @throws	DBException	if there's a problem writing to the database
	 */
	Question addQuestion() throws DBException {
		return addQuestion(!PRESERVE_ID);
	} // addQuestion()

	/** Add a new master- or locally authored question to a question set,
	 * falling back under failure.
	 * @param	preserveId	whether to preserve the ID number of the question
	 * being added
	 * @return	question that was written
	 * @throws	DBException	if there's a problem writing to the database
	 */
	Question addQuestion(boolean preserveId) throws DBException {
		final String SELF = "QuestionWrite.addQuestion: ";
		PreparedStatement stmtBatch = null;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			int qId = question.getQId();
			if (!preserveId) {
				qId = nextSequence(con, QUESTIONS_SEQ);
				if (tables.local) qId = -qId;
			} else if (qId == 0) throw new DBException(
					"Question ID must be nonzero when preserveId is true");
			// populate the changedQ object for return
			changedQ = new Question(qId);
			// add statement, other singleton values
			processSingletonData(qId, setId);
			/* ****************** Evaluators ***************************/
			final Evaluator[] evaluators = question.getAllEvaluators();
			stmtBatch = getPreparedStmt(tables.EVALUATORS);
			int majorId = 0;
			for (final Evaluator eval : evaluators) {
				eval.qId = qId;
				eval.majorId = ++majorId;
				debugPrint(SELF + "added new evaluator with majorId ", majorId);
				writeEvaluatorRecs(stmtBatch, eval);
				changedQ.addEvaluator(new Evaluator(eval));
				if (eval.miscMessage != null) {
					changedQ.miscMessage = toString(changedQ.miscMessage,
							eval.miscMessage);
				} // if there's a message from the evaluator
			} // each evaluator
			stmtBatch.executeBatch();
			closeConnection(null, stmtBatch, null);
			/* ****************** Figures ***************************/
			final Figure[] figures = question.getFigures();
			final List<String[]> renameImages = new ArrayList<String[]>();
			stmtBatch = getPreparedStmt(tables.FIGURES);
			int figNum = 0;
			for (final Figure figure : figures) {
				figure.questionId = qId;
				figure.figureId = nextSequence(con, FIGURES_SEQ);
				figure.serialNo = ++figNum;
				final String oldLocation = figure.bufferedImage;
				if (figure.hasImage()) {
					figure.bufferedImage = getFileName(figure);
					renameImages.add(new String[] {oldLocation, 
							figure.bufferedImage});
					addNewFigure(stmtBatch, figure);
				} else addNewFigure(stmtBatch, figure);
				debugPrint(SELF + "added new figure with figureId = ", 
						figure.figureId, "; new serial number = ", 
						figure.serialNo, ", bufferedImage = ", 
						figure.bufferedImage);
				changedQ.addFigure(new Figure(figure));
				figure.bufferedImage = oldLocation;
			} // each figure
			stmtBatch.executeBatch();
			closeConnection(null, stmtBatch, null);
			/* ****************** Question data ***************************/
			int qdNum = 0;
			for (final QDatum[] qData : question.getAllQData()) {
				stmtBatch = getPreparedStmt(tables.QUESTIONDATA);
				for (final QDatum qDatum : qData) {
					qDatum.questionId = qId;
					qDatum.dataId = nextSequence(con, QUESTIONDATA_SEQ);
					qDatum.serialNo = ++qdNum;
					addNewQDatum(stmtBatch, qDatum);
					debugPrint(SELF + "added new qDatum with dataId = ", 
							qDatum.dataId, "; new serial number = ", 
							qDatum.serialNo);
					changedQ.populateQDatum(qDatum instanceof EDiagramQDatum
								? new EDiagramQDatum(qDatum) 
							: qDatum instanceof CaptionsQDatum
								? new CaptionsQDatum(qDatum) 
							: new QDatum(qDatum));
				} // each question datum
				stmtBatch.executeBatch();
				closeConnection(null, stmtBatch, null);
			} // for each set of question data
			if (question.isTable() || question.isED()) {
				stmtBatch = getPreparedStmt(tables.CAPTIONS);
				final int numQData = (question.isTable() ? 2 : 1);
				for (qdNum = 0; qdNum < numQData; qdNum++) {
					final QDatum qDatum = 
							question.getQDatum(GENERAL, qdNum + 1);
					addNewCapsandLabels(stmtBatch, question.isED()
							? new EDiagramQDatum(qDatum)
							: new CaptionsQDatum(qDatum));
				} // for each set of question data
				stmtBatch.executeBatch();
				closeConnection(null, stmtBatch, null);
			} // if is table Q
			con.commit();
			ImageRW.renameImageFiles(con, renameImages);
		} catch (SQLException e) {
			e.printStackTrace();
			rollbackConnection(con);
			throw new DBException(e.getMessage());
		} catch (ParameterException e) {
			e.printStackTrace();
			rollbackConnection(con);
			throw new DBException(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			rollbackConnection(con);
			throw new DBException(e.getMessage());
		} catch (DBException e) {
			e.printStackTrace();
			rollbackConnection(con);
			throw e;
		} finally {
			closeConnection(con);
		} // try
		return changedQ;
	} // addQuestion(boolean)

	/** Rewrite the question to the master table or the locally authored 
	 * table, falling back under failure.
	 * The question passed in here still refers to the stored qBuffer in
	 * session memory. So directly changing any of its values can in fact
	 * affect the original buffered question.  So be sure that the returned
	 * question is a fresh instance, at every level:  Don't put any instance of
	 * question in changedQ.  Instead, use the the copy constructors in
	 * Evaluator and Figure.
	 * @return	question that was written
	 * @throws	DBException	if there's a problem writing to the database
	 */
	Question setQuestion() throws DBException {
		final String SELF = "QuestionWrite.setQuestion: ";
		final int qId = question.getQId();
		changedQ = new Question(qId);
		changedQ.miscMessage = "";
		// If a figure image has been added or edited, 
		// filename of new image needs to be changed.  Wait 
		// until save of question is committed before changing 
		// the filenames.
		final List<String[]> renameImages = new ArrayList<String[]>();
		PreparedStatement stmtBatch = null;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			processSingletonData(qId, CHANGE_Q);
			/* ****************** Evaluators ***************************/
			// Change evaluators
			/* Semantics of majorId (x > 0, = serial number)
					  x - unchanged
					  -x - changed
					  0 - new
			1) CONDITION: the zero ids must come after all others
					 x,....0,0,0
			so all evaluators after a 0 will become 0
			2) Delete all records whose ids are not present in question
			3) Start writing all the records
			 */
			final Evaluator[] evaluators = question.getAllEvaluators();
			// Create a list of positive majorIds up until the first 0.
			// Delete all other ids (changed versions will be reinserted later).
			// Also get list of oldMajorIds so as not to delete their
			// translations.
			final List<Integer> keepMajorIds = new ArrayList<Integer>();
			final List<Integer> oldMajorIdsOfKeepPhraseIds = new ArrayList<Integer>();
			final List<Evaluator> writeEvals = new ArrayList<Evaluator>();
			final Map<Integer, Integer> movedEvalIds = 
					new LinkedHashMap<Integer, Integer>();
			int evalNum = 0;
			for (final Evaluator eval : evaluators) {
				eval.qId = qId;
				final boolean isNew = eval.majorId == 0;
				final boolean isModified = eval.majorId < 0;
				final boolean isMoved = eval.majorId != ++evalNum;
				if (eval.phraseId != 0) {
					oldMajorIdsOfKeepPhraseIds.add(Integer.valueOf(eval.majorId));
				} // if evaluator translation has been stored
				debugPrint(SELF + "evaluator ", evalNum, 
						": old major ID = ", eval.majorId, ", isNew = ", isNew, 
						", isModified = ", isModified, ", isMoved = ", isMoved);
				if (isNew || isModified) {
					eval.oldMajorId = eval.majorId;
					if (isNew) {
						debugPrint(SELF + "will add new evaluator ", evalNum);
					} else {
						debugPrint(SELF + "will delete evaluator ",
								evalNum, " with previous majorId ", eval.oldMajorId, 
								" in preparation for rewriting.");
					} // if isNew
					writeEvals.add(eval);
				} else {
					final Integer evalMajorIdObj = Integer.valueOf(eval.majorId);
					keepMajorIds.add(evalMajorIdObj);
					if (isMoved) {
						movedEvalIds.put(evalMajorIdObj, Integer.valueOf(evalNum));
						debugPrint(SELF, "evaluator ", evalNum, 
								" currently has majorId = ", eval.majorId,
								", so need to move.");
					} else debugPrint(SELF, "evaluator ", evalNum, 
							" remains unchanged.");
				} // if new or modified
				eval.majorId = evalNum;
				changedQ.addEvaluator(new Evaluator(eval));
				if (eval.miscMessage != null)
					changedQ.miscMessage += eval.miscMessage;
			} // for each evaluator
			// remove translations of deleted or modified evaluators of this Q
			deletePhraseIdsNotOf(tables.EVALUATORS, oldMajorIdsOfKeepPhraseIds);
			// remove obsolete, old versions of modified and moved evaluators
			deleteOtherThan(tables.EVALUATORS, keepMajorIds);
			// change major IDs of moved evaluators
			renumberMoved(tables.EVALUATORS, movedEvalIds);
			// add new and rewrite moved and modified evaluators
			if (!writeEvals.isEmpty()) {
				stmtBatch = getPreparedStmt(tables.EVALUATORS);
				for (final Evaluator eval : writeEvals) {
					writeEvaluatorRecs(stmtBatch, eval);
				} // for each evaluator to write
				stmtBatch.executeBatch();
				closeConnection(null, stmtBatch, null);
			} // if there are evaluators to write
			/* ****************** Figures ***************************/
			// if figure.figureId < 0, change existing figure
			// if 0, add figure (always grouped at the end)
			// figures can be moved by deletion of earlier figures
			final Figure[] figures = question.getFigures();
			final List<Integer> keepFigIds = new ArrayList<Integer>();
			final List<Integer> unchangedImages = new ArrayList<Integer>();
			final List<Figure> writeFigs = new ArrayList<Figure>();
			final Map<Integer, Integer> movedFigIds = 
					new HashMap<Integer, Integer>();
			int figNum = 0;
			for (final Figure figure : figures) {
				figure.questionId = qId;
				final int figureId = figure.figureId;
				final boolean isNew = figureId == 0;
				final boolean isModified = figureId < 0;
				final boolean isMoved = figure.serialNo != ++figNum;
				String oldLocation = null;
				if (isNew || isModified) {
					figure.figureId = (isModified ? -figureId
							: nextSequence(con, FIGURES_SEQ));
					figure.serialNo = figNum;
					if (figure.hasImage()) {
						oldLocation = figure.bufferedImage;
						figure.bufferedImage = getFileName(figure);
						debugPrint(SELF + "oldLocation = ", oldLocation,
								", figure.bufferedImage = ",
								figure.bufferedImage);
						// If a figure image has been added or edited, 
						// filename of new image may need to be changed.  Wait 
						// until save of question is committed before changing 
						// the filenames.
						// Here, store old and new filenames to rename images 
						// later.
						if (figure.bufferedImage.equals(oldLocation)) {
							unchangedImages.add(Integer.valueOf(
								figure.figureId));
							debugPrint(SELF + "figure filename is unchanged, "
									+ "so keeping the original image file.");
						} else {
							final String[] names = new String[] 
									{oldLocation, figure.bufferedImage};
							renameImages.add(names);
						} // if figure doesn't need to be renamed
					} // if image
					writeFigs.add(figure);
					debugPrint(SELF, isModified ? "changed" : "added",
							" figure with figureId = ", figure.figureId,
							"; new serial number = ", figure.serialNo,
							", bufferedImage = ", figure.bufferedImage);
				} else { // moved or unchanged
					final Integer figureIdObj = Integer.valueOf(figureId);
					keepFigIds.add(figureIdObj);
					if (isMoved) {
						movedFigIds.put(figureIdObj, Integer.valueOf(figNum));
						debugPrint(SELF, "will move figure ", figNum, 
								"; figureId = ", figureId,
								", stored serialNo = ", figure.serialNo,
								", new serialNo = ", figNum);
					} else debugPrint(SELF, "figure ", figNum, 
							" with figureId = ", figureId,
							", stored serialNo = ", figure.serialNo,
							" remains unchanged.");
				} // if new or modified
				final Figure newFigure = new Figure(figure);
				changedQ.addFigure(newFigure);
			} // each figure of question
			// delete old versions of modified and obsolete figures;
			// save file names for deletion from disk after commit
			final List<String> deleteImageFileNames = 
					deleteOtherThan(tables.FIGURES, keepFigIds, 
						unchangedImages);
			// write new, rewrite modified figures
			if (!writeFigs.isEmpty()) {
				stmtBatch = getPreparedStmt(tables.FIGURES);
				for (final Figure figure : writeFigs) {
					addNewFigure(stmtBatch, figure); 
				} // for each figure to write
				stmtBatch.executeBatch();
				closeConnection(null, stmtBatch, null);
			} // if there are figures to write
			// change serial numbers of moved figures
			renumberMoved(tables.FIGURES, movedFigIds);
			/* ****************** Question Data ***************************/
			int qdNum = 0;
			final List<Integer> keepDataIds = new ArrayList<Integer>();
			final List<Integer> dataIdsOfKeepPhraseIds = 
					new ArrayList<Integer>();
			final List<QDatum> writeQData = new ArrayList<QDatum>();
			final Map<Integer, Integer> movedDataIds = 
					new HashMap<Integer, Integer>();
			for (final QDatum[] qData : question.getAllQData()) {
				// if question data.dataId < 0, change record; if 0, add record;
				// make list of dataIds of current qData (unmodified and moved)
				// and list of phraseIds (unmodified only)
				for (final QDatum qDatum : qData) {
					qDatum.questionId = qId;
					final int datumId = qDatum.dataId;
					final boolean isNew = datumId == 0;
					final boolean isModified = datumId < 0;
					final boolean isMoved = qDatum.serialNo != ++qdNum;
					if (isNew || isModified) {
						qDatum.serialNo = qdNum;
						if (isNew) {
							qDatum.dataId = nextSequence(con, QUESTIONDATA_SEQ);
							debugPrint(SELF, "will add qDatum with dataId = ", 
									qDatum.dataId, ", serial number = ", 
									qDatum.serialNo);
						} else if (isModified) {
							qDatum.dataId = -datumId;
							debugPrint(SELF + "will delete qDatum with "
									+ "dataId = ", qDatum.dataId, 
									", serial number = ", qDatum.serialNo, 
									", textId = ", qDatum.phraseId,
									" in preparation for rewriting.");
						} // if new or modified
						writeQData.add(qDatum);
					} else { // moved or unchanged
						final Integer datumIdObj = Integer.valueOf(datumId);
						keepDataIds.add(datumIdObj);
						if (isMoved) {
							movedDataIds.put(datumIdObj, 
									Integer.valueOf(qdNum));
							debugPrint(SELF, "will move qDatum with dataId = ", 
									qDatum.dataId, ", current serial number = ", 
									qDatum.serialNo, ", new serial number ", 
									qdNum);
						} else debugPrint(SELF, "qDatum with dataId = ", 
								qDatum.dataId, ", serial number = ", 
								qDatum.serialNo, " is unchanged.");
					} // is new or modified
					if (qDatum.phraseId != 0) {
						dataIdsOfKeepPhraseIds.add(
								Integer.valueOf(qDatum.dataId));
					} // if phrase has been translated
					changedQ.populateQDatum(qDatum instanceof EDiagramQDatum
							? new EDiagramQDatum(qDatum) 
							: qDatum instanceof CaptionsQDatum
							? new CaptionsQDatum(qDatum) : new QDatum(qDatum));
				} // for each qDatum
			} // for each list of question data
			debugPrint(SELF + "we are keeping qData ", keepDataIds,
					" and translations ", dataIdsOfKeepPhraseIds);
			// remove any translations of deleted or modified qData
			deletePhraseIdsNotOf(tables.QUESTIONDATA, dataIdsOfKeepPhraseIds);
			// remove modified or deleted qData
			deleteCapsAndLabelsOtherThan(keepDataIds);
			deleteOtherThan(tables.QUESTIONDATA, keepDataIds);
			// write new and modifed qData
			if (!writeQData.isEmpty()) {
				stmtBatch = getPreparedStmt(tables.QUESTIONDATA);
				for (final QDatum qDatum : writeQData) {
					addNewQDatum(stmtBatch, qDatum);
				} // for each qDatum to write
				stmtBatch.executeBatch();
				closeConnection(null, stmtBatch, null);
				if (question.isTable() || question.isED()) {
					stmtBatch = getPreparedStmt(tables.CAPTIONS);
					for (final QDatum qDatum : writeQData) {
						addNewCapsandLabels(stmtBatch, question.isED()
								? new EDiagramQDatum(qDatum)
								: new CaptionsQDatum(qDatum));
					} // for each qDatum to write
					stmtBatch.executeBatch();
					closeConnection(null, stmtBatch, null);
				} // if is table Q
			} // if there are qData to write
			// change serial numbers of moved qData
			renumberMoved(tables.QUESTIONDATA, movedDataIds);
			// move the names of figure image files from random names
			// to conventional names; existing files with same new name 
			// can be safely overwritten now that the
			// database write is complete
			con.commit();
			ImageRW.deleteImagesFromDisk(deleteImageFileNames);
			ImageRW.renameImageFiles(con, renameImages);
		} catch (SQLException e) {
			e.printStackTrace();
			rollbackConnection(con);
			throw new DBException(e.getMessage());
		} catch (ParameterException e) {
			e.printStackTrace();
			rollbackConnection(con);
			throw new DBException(e.getMessage());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			rollbackConnection(con);
			throw new DBException(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			rollbackConnection(con);
			throw new DBException(e.getMessage());
		} catch (DBException e) {
			e.printStackTrace();
			rollbackConnection(con);
			throw e;
		} finally {
			closeConnection(con, stmtBatch, null);
		}
		return changedQ;
	} // setQuestion()

	/** Gets the filename for a figure.
	 * @param	figure	the figure
	 * @return	the filename for the figure
	 */
	private String getFileName(Figure figure) {
		return changedQ.makeImageFileName(figure.figureId,
				Utils.getExtension(figure.bufferedImage),
				tables.local ? DBLocalTables.LOCAL_PREFIX : "");
	} // getFileName(Figure)

	/** Adds statement, provenance, flags, serial number, keywords of the question
	 * to the table.  Deletes translations of the question statement if no other
	 * question statement is identical.
	 * @param	qId	unique ID of question
	 * @param	setId	unique ID of question set
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private void processSingletonData(int qId, int setId) throws SQLException {
		final String SELF = "QuestionWrite.processSingletonData: ";
		String statement = question.getStatement();
		statement = (statement == null ? ""
				: unicodeToCERs(statement.trim()));
		String chapter = question.getChapter();
		if (chapter != null) chapter = chapter.trim();
		String remarks = question.getRemarks();
		if (remarks != null) remarks = remarks.trim();
		final String keywords = question.getKeywords();
		final DateFormat df = DateFormat.getDateTimeInstance();
		final String nowStr = df.format(new Date(System.currentTimeMillis()));
		int stmtId = question.getPhraseId();
		debugPrint(SELF + "qId = ", qId, ", setId = ", setId, 
				", stmtId = ", stmtId);
		final int qType = question.getQType();
		final long qFlags = question.getQFlags();
		final SQLWithQMarks sql_vals = new SQLWithQMarks("",
					statement,
					stmtId,
					question.getBook(),
					chapter,
					remarks,
					DB_QTYPES[qType],
					qFlags,
					question.getSerialNo(),
					nowStr,
					keywords,
					qId);
		if (setId == CHANGE_Q) { // setId not used when changing existing Q; CHANGE_Q = 0
			// if statement has changed, delete translations
			final String origStmt = question.getOrigStatement();
			if (stmtId != 0 && origStmt != null
					&& !origStmt.trim().equals(statement)) {
				debugPrint(SELF + "statement changing; maybe delete translation");
				deletePhraseIdsNotOf(tables.QUESTIONS, new ArrayList<Integer>());
				stmtId = getStmtIdOfIdenticalQStatements(qId, statement);
				debugPrint(SELF + "got new phrase ID ", stmtId, 
						" for new statement");
			} // if statement is new and has translations
 			sql_vals.setSql(
					UPDATE, tables.QUESTIONS, SET, equalsJoinQMarks(
						Q_STATEMENT, // CLOB field
						Q_STMT_ID,
						Q_BOOK,
						Q_BOOKCHAP,
						Q_REMARKS, // CLOB field
						Q_TYPE, 
						Q_FLAGS, 
						Q_NUM, 
						Q_LAST_MODIFIED,
						Q_KEYWORDS), // CLOB field
 					WHERE + Q_QID + EQUALS + QMARK, 
					ifLocalAndQAuthorIs.getSql());
			sql_vals.addValuesFrom(ifLocalAndQAuthorIs);
		} else { // add Q uses setId
			// see if statement is already translated
			if (stmtId == 0 && !Utils.isEmpty(statement)) {
				stmtId = getStmtIdOfIdenticalQStatements(qId, statement);
			} // if there is not already a phrase
			final List<String> fields = new ArrayList<String>(Arrays.asList(
					Q_STATEMENT, // CLOB field
					Q_STMT_ID,
					Q_BOOK,
					Q_BOOKCHAP,
					Q_REMARKS, // CLOB field
					Q_TYPE,
					Q_FLAGS,
					Q_NUM,
					Q_LAST_MODIFIED, 
					Q_KEYWORDS, // CLOB field
					Q_QID,
					Q_QSET,
					Q_CREATED));
			sql_vals.addValues(
					setId, 
					nowStr);
			if (tables.local) {
				fields.add(Q_AUTHOR());
				sql_vals.addValue(authorId);
			} // if local table
 			sql_vals.setSql(
					getInsertIntoValuesQMarksSQL(tables.QUESTIONS, fields));
		} // if change or add
		debugPrint(SELF, sql_vals);
		tryUpdate(con, sql_vals);
		changedQ.setStatement(statement);
		changedQ.setOrigStatement(statement);
		changedQ.setPhraseId(stmtId);
		changedQ.setQType(qType);
		changedQ.setQFlags(qFlags);
		changedQ.setQSetId(question.getQSetId());
		changedQ.setBook(question.getBook());
		changedQ.setChapter(question.getChapter());
		changedQ.setRemarks(question.getRemarks());
		changedQ.setSerialNo(question.getSerialNo());
		changedQ.setKeywords(keywords);
	} // processSingletonData(int, int)

	/** Gets the statement ID of other questions' identical question statements
	 * that have already been translated.
	 * @param	qId	question ID number
	 * @param	statement	the new statement being stored that may be identical
	 * to existing translated question statements
	 * @return	the ID of the already existing identical question statement that
	 * has already been translated
	 */
	private int getStmtIdOfIdenticalQStatements(int qId, String statement) {
		final String SELF = "QuestionWrite.getStmtIdOfIdenticalQStatements: ";
		final String qry = toString(
				SELECT_UNIQUE + Q_STMT_ID 
				+ FROM, tables.QUESTIONS,
				WHERE, clobToString(Q_STATEMENT), EQUALS + QMARK
				+ AND + Q_QID + NOT_EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				statement,
				qId);
		debugPrint(SELF, sql_vals);
		int stmtId = 0;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) {
				stmtId = rs.getInt(Q_STMT_ID);
				debugPrint(SELF + "got phraseId ", stmtId, 
						" from another question's statement identical to: ", 
						statement);
			} // if there's an identical question statement
		} catch (SQLException e) {
			debugPrint(SELF + "caught SQLException while trying to find "
					+ "identical question statement; continuing.");
		} finally {
			closeStmtAndRs(stmt, rs);
		} // try
		return stmtId;
	} // getStmtIdOfIdenticalQStatements(int, String)

	/** Writes out an evaluator record with given majorId and, if it is complex,
	 * its subevaluators.
	 * @param	pstmt	prepared statement containing the insert SQL
	 * @param	eval	evaluator to be stored
	 * @throws	DBException	if there's a problem writing to the database
	 * @throws	SQLException	if there's a problem writing to the database
	 * @throws	ParameterException	if there is not a subevaluator with that
	 * number
	 */
	private void writeEvaluatorRecs(PreparedStatement pstmt, Evaluator eval) 
			throws DBException, ParameterException, SQLException {
		final String SELF = "QuestionWrite.writeEvaluatorRecs: ";
		debugPrint(SELF + "writing evaluator:\n", eval);
		String matchCode;
		String codedData;
		String molStruct;
		String molName;
		String exprCode;
		final String feedback = (eval.feedback == null ? ""
				: unicodeToCERs(eval.feedback.trim()));
		if (eval.phraseId == 0 && !Utils.isEmpty(feedback)) {
			PreparedStatement stmt = null;
			ResultSet rs = null;
			final String qry = toString(
					SELECT_UNIQUE + EVAL_FEEDBACKID 
					+ FROM, tables.EVALUATORS,
					WHERE, clobToString(EVAL_FEEDBACK), EQUALS + QMARK 
					+ AND + EVAL_MINORID + IS_ZERO 
					+ AND + EVAL_FEEDBACKID + IS_NOT_ZERO
					+ AND, parens(
						EVAL_QID + NOT_EQUALS + QMARK
						+ OR + EVAL_MAJORID + NOT_EQUALS + QMARK));
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
					feedback,
					eval.qId,
					eval.majorId);
			debugPrint(SELF, sql_vals);
			try {
				stmt = getStatement(con, sql_vals);
				rs = stmt.executeQuery();
				if (rs.next()) {
					eval.phraseId = rs.getInt(EVAL_FEEDBACKID);
					debugPrint(SELF + "got phraseId ", eval.phraseId, 
							" from another evaluator's feedback identical to: ", 
							feedback);
				} // if there's an identical feedback
			} catch (SQLException e) {
				debugPrint(SELF + "caught SQLException while trying to find "
						+ "identical feedback; continuing.");
			} finally {
				closeStmtAndRs(stmt, rs);
			} // try
		} // if there is not already a phrase ID
		if (eval.isComplex()) {
			final int numSubevals = eval.getNumSubevaluators();
			debugPrint(SELF + "evaluator ", eval.majorId, 
					" is complex; has ", numSubevals, " subevaluators");
			for (int minorId = 1; minorId <= numSubevals; minorId++) {
				final Subevaluator subeval = eval.getSubevaluator(minorId);
				debugPrint(SELF + "writing subevaluator ", minorId, 
						":\n", subeval);
				molStruct = subeval.getMolStruct();
				if (molStruct == null) molStruct = "";
				molName = subeval.getMolName();
				if (molName == null) molName = "";
				final List<String> fields = getFields(tables.EVALUATORS);
				pstmt.setInt(fields.indexOf(EVAL_QID) + 1, eval.qId);
				pstmt.setInt(fields.indexOf(EVAL_MAJORID) + 1, eval.majorId);
				pstmt.setInt(fields.indexOf(EVAL_MINORID) + 1, minorId);
				pstmt.setString(fields.indexOf(EVAL_SUBEXPR) + 1, ""); // no expression code
				pstmt.setString(fields.indexOf(EVAL_TYPE) + 1, 
						subeval.matchCode); 
				pstmt.setString(fields.indexOf(EVAL_FEEDBACK) + 1, ""); // no feedback
				pstmt.setInt(fields.indexOf(EVAL_FEEDBACKID) + 1, 0); // no feedbackId
				pstmt.setDouble(fields.indexOf(EVAL_GRADE) + 1, 0); // no grade
				pstmt.setString(fields.indexOf(EVAL_CODEDDATA) + 1, 
						subeval.codedData);
				pstmt.setString(fields.indexOf(EVAL_MOLSTRUCT) + 1, molStruct);
				pstmt.setString(fields.indexOf(EVAL_MOLNAME) + 1, molName);
				if (tables.local) {
					pstmt.setString(fields.indexOf(Q_AUTHOR()) + 1, authorId);
				} // if locally authored
				debugPrint(SELF + "adding subevaluator ", eval.majorId, 
						'.', minorId, " to the batch for Q ", eval.qId);
				pstmt.addBatch();
			} // each subevaluator
			// prepare for parent evaluator
			matchCode = "";
			codedData = "";
			molStruct = "";
			molName = "";
			exprCode = eval.getExpressionCode();
			debugPrint(SELF + "complex parent evaluator ", eval.majorId, 
					" has combination expression ", exprCode);
		} else {
			debugPrint(SELF + "evaluator with majorId ", eval.majorId, 
					" is simple.");
			final Subevaluator subeval = eval.getSubevaluator(1);
			matchCode = subeval.matchCode;
			codedData = subeval.codedData;
			molStruct = subeval.molStruct;
			molName = subeval.molName;
			exprCode = "";
		} // if evaluator is simple
		if (exprCode.length() > MAX_EVAL_SUBEXPR) {
			throw new DBException("Too many evaluators joined together.");
		} // if expression won't fit in database
		// write out parent evaluator
		final List<String> fields = getFields(tables.EVALUATORS);
		pstmt.setInt(fields.indexOf(EVAL_QID) + 1, eval.qId);
		pstmt.setInt(fields.indexOf(EVAL_MAJORID) + 1, eval.majorId);
		pstmt.setInt(fields.indexOf(EVAL_MINORID) + 1, 0); // parent evaluator
		pstmt.setString(fields.indexOf(EVAL_SUBEXPR) + 1, exprCode); // e.g., "1:2:3:|:@"
		pstmt.setString(fields.indexOf(EVAL_TYPE) + 1, matchCode);
		pstmt.setString(fields.indexOf(EVAL_FEEDBACK) + 1, feedback);
		pstmt.setInt(fields.indexOf(EVAL_FEEDBACKID) + 1, eval.phraseId);
		pstmt.setDouble(fields.indexOf(EVAL_GRADE) + 1, eval.grade);
		pstmt.setString(fields.indexOf(EVAL_CODEDDATA) + 1, codedData);
		pstmt.setString(fields.indexOf(EVAL_MOLSTRUCT) + 1, molStruct);
		pstmt.setString(fields.indexOf(EVAL_MOLNAME) + 1, molName);
		if (tables.local) {
			pstmt.setString(fields.indexOf(Q_AUTHOR()) + 1, authorId);
		}  // if local
		pstmt.addBatch();
	} // writeEvaluatorRecs(PreparedStatement, Evaluator)

	/** Adds a figure to the table.
	 * @param	stmt	prepared statement containing the insert SQL
	 * @param	figure	figure to be written
	 * @throws	SQLException	if there's a problem writing to the database
	 * @throws	IOException	if there's a problem writing a figure to disk
	 */
	private void addNewFigure(PreparedStatement stmt, Figure figure) 
			throws SQLException, IOException {
		addNewFigure(stmt, figure, NO_UNCHANGED_IMAGES);
	} // addNewFigure(PreparedStatement, Figure)

	/** Adds a figure to the table.
	 * @param	stmt	prepared statement containing the insert SQL
	 * @param	figure	figure to be written
	 * @param	unchangedImages	IDs of image-and-vector figures whose images
	 * haven't changed
	 * @throws	SQLException	if there's a problem writing to the database
	 * @throws	IOException	if there's a problem writing a figure to disk
	 */
	private void addNewFigure(PreparedStatement stmt, Figure figure,
			List<Integer> unchangedImages) throws SQLException, IOException {
		final String SELF = "QuestionWrite.addNewFigure: ";
		String figureTypeStr = "";
		final int figureId = figure.figureId;
		try {
			figureTypeStr = Figure.DBVALUES[figure.type];
		} catch (ArrayIndexOutOfBoundsException e) {
			Utils.alwaysPrint(SELF + "unrecognized figure type ",
					figure.type, ", should be between 1 and ",
					Figure.DBVALUES.length - 1);
		} // try
		final String addlData = (figure.usesAddlData()
				? figure.addlData : "");
		final String mainData = (figure.isImage() 
				|| figure.data == null ? "" : figure.data);
		final List<String> fields = getFields(tables.FIGURES);
		stmt.setInt(fields.indexOf(FIG_QID) + 1, figure.questionId);
		stmt.setInt(fields.indexOf(FIG_FIGID) + 1, figureId);
		stmt.setInt(fields.indexOf(FIG_NUM) + 1, figure.serialNo);
		stmt.setString(fields.indexOf(FIG_TYPE) + 1, figureTypeStr);
		stmt.setString(fields.indexOf(FIG_ADDL_DATA) + 1, addlData); // CLOB
		stmt.setString(fields.indexOf(FIG_MAIN_DATA) + 1, mainData); // CLOB
		if (tables.local) {
			stmt.setString(fields.indexOf(Q_AUTHOR()) + 1, authorId);
		} // if locally authored
		debugPrint(SELF + "adding figure ", figure.serialNo, 
				" with ID ", figureId, " to the batch for Q ", 
				figure.questionId);
		stmt.addBatch();
		if (figure.hasImage() && (Utils.isEmpty(unchangedImages)
				|| !unchangedImages.contains(Integer.valueOf(figureId)))) {
			try {
				final ImageRW imgWriter = new ImageRW(con, authorId);
				imgWriter.addImage(figureId, figure.bufferedImage);
			} catch (SQLException e) {
				Utils.alwaysPrint(SELF + "Exception caught when "
						+ "trying to save figure ", figure.serialNo);
				e.printStackTrace();
			} // try
		} // if an image
	} // addNewFigure(PreparedStatement, Figure, List<Integer>)

	/** Adds a question datum to the table.
	 * @param	pstmt	prepared statement containing the insert SQL
	 * @param	qDatum	question datum to be written
	 * @throws	SQLException	if there's a problem writing to the database
	 * @throws	IOException	if there's a problem writing a figure to disk
	 */
	private void addNewQDatum(PreparedStatement pstmt, QDatum qDatum) 
			throws SQLException, IOException {
		final String SELF = "QuestionWrite.addNewQDatum: ";
		final String data = (qDatum.data == null ? "" 
				: qDatum.isText() ? unicodeToCERs(qDatum.data.trim())
				: qDatum.data.trim());
		if (qDatum.phraseId == 0 && !Utils.isEmpty(data)) {
			PreparedStatement stmt = null;
			ResultSet rs = null;
			final String qry = toString(
					SELECT_UNIQUE + QD_TEXT_ID 
					+ FROM, tables.QUESTIONDATA,
					WHERE, clobToString(QD_DATA), EQUALS + QMARK
					+ AND + QD_TEXT_ID + IS_NOT_ZERO
					+ AND, parens(
						QD_QID + NOT_EQUALS + QMARK
						+ OR + QD_DATUMID + NOT_EQUALS + QMARK));
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
					data,
					qDatum.questionId,
					qDatum.dataId);
			debugPrint(SELF, sql_vals);
			try {
				stmt = getStatement(con, sql_vals);
				rs = stmt.executeQuery();
				if (rs.next()) {
					qDatum.phraseId = rs.getInt(QD_DATUMID);
					debugPrint(SELF + "got phraseId ", qDatum.phraseId, 
							" from another question datum's text identical to: ", 
							data);
				} // if there's an identical feedback
			} catch (SQLException e) {
				debugPrint(SELF + "caught SQLException while trying to find "
						+ "identical question datum text; continuing.");
			} finally {
				closeStmtAndRs(stmt, rs);
			} // try
		} // if there is not already a phrase ID
		String dataTypeStr = "";
		try {
			dataTypeStr = QDatum.DBVALUES[qDatum.dataType];
		} catch (ArrayIndexOutOfBoundsException e) {
			Utils.alwaysPrint(SELF + "unrecognized figure type ",
					qDatum.dataType, ", should be between 1 and ",
					QDatum.DBVALUES.length - 1);
		} // try
		final List<String> fields = getFields(tables.QUESTIONDATA);
		pstmt.setInt(fields.indexOf(QD_QID) + 1, qDatum.questionId);
		pstmt.setInt(fields.indexOf(QD_DATUMID) + 1, qDatum.dataId);
		pstmt.setInt(fields.indexOf(QD_NUM) + 1, qDatum.serialNo);
		pstmt.setString(fields.indexOf(QD_TYPE) + 1, dataTypeStr);
		pstmt.setString(fields.indexOf(QD_DATA) + 1, data);
		pstmt.setString(fields.indexOf(QD_NAME) + 1, 
				qDatum.isMarvin() ? qDatum.name : "");
		pstmt.setInt(fields.indexOf(QD_TEXT_ID) + 1, qDatum.phraseId);
		if (tables.local) {
			pstmt.setString(fields.indexOf(Q_AUTHOR()) + 1, authorId);
		} // if locally authored
		debugPrint(SELF + "adding qDatum ", qDatum.serialNo, 
				" with ID ", qDatum.dataId, " to the batch for Q ",
				qDatum.questionId);
		pstmt.addBatch();
	} // addNewQDatum(PreparedStatement, QDatum)

	/** Adds row/column captions, pulldown menu labels, and y-axis scales
	 * associated with a complete-the-table or energy diagram question.
	 * @param	pstmt	prepared statement containing the insert SQL
	 * @param	qDatum	question datum whose captions/labels will be written
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private void addNewCapsandLabels(PreparedStatement pstmt, 
			CaptionsQDatum qDatum) throws SQLException {
		final String SELF = "QuestionWrite.addNewCapsandLabels: ";
		final int qId = qDatum.questionId;
		final int qdNum = qDatum.serialNo;
		final List<String> fields = getFields(tables.CAPTIONS);
		if (!Utils.membersAreEmpty(qDatum.captions)) {
			// column caption serial nums start at 0 in complete-the-table Qs
			int captionNum = (question.isTable() 
					&& qdNum == COL_DATA + 1 ? 0 : 1);
			final String captionType = CAPTS_TYPE_DBVALUES[
					question.isTable() ? qdNum - 1 : COL_DATA];
			debugPrint(SELF + "writing captions ", qDatum.captions, 
					" of qDatum ", qdNum, " in Q", qId,
					" starting at caption number ", captionNum,
					" with type ", captionType);
			for (final String caption : qDatum.captions) {
				pstmt.setInt(fields.indexOf(CAPTS_QID) + 1, qId);
				pstmt.setString(fields.indexOf(CAPTS_TYPE) + 1, 
						captionType);
				pstmt.setInt(fields.indexOf(CAPTS_NUM) + 1, captionNum);
				pstmt.setString(fields.indexOf(CAPTS_TEXT) + 1, caption);
				if (tables.local) {
					pstmt.setString(fields.indexOf(Q_AUTHOR()) + 1, authorId);
				} // if locally authored
				pstmt.addBatch();
				captionNum++;
			} // for each caption
		} else debugPrint(SELF + "there are no captions to write associated "
				+ "with qDatum ", qdNum, " in Q", qId);
		if (qDatum instanceof EDiagramQDatum) {
			final EDiagramQDatum eqDatum = (EDiagramQDatum) qDatum;
			if (!Utils.membersAreEmpty(eqDatum.labels)) {
				final String labelType = CAPTS_TYPE_DBVALUES[LABEL_DATA];
				debugPrint(SELF + "writing labels ", eqDatum.labels, 
						" of eqDatum ", qdNum, " in Q", qId);
				int labelNum = 1;
				for (final String label : eqDatum.labels) {
					pstmt.setInt(fields.indexOf(CAPTS_QID) + 1, qId);
					pstmt.setString(fields.indexOf(CAPTS_TYPE) + 1, 
							labelType);
					pstmt.setInt(fields.indexOf(CAPTS_NUM) + 1, labelNum);
					pstmt.setString(fields.indexOf(CAPTS_TEXT) + 1, label);
					if (tables.local) {
						pstmt.setString(fields.indexOf(Q_AUTHOR()) + 1, authorId);
					} // if locally authored
					pstmt.addBatch();
					labelNum++;
				} // for each label
			} else debugPrint(SELF + "there are no labels to write associated "
					+ "with eqDatum ", qDatum.serialNo, " in Q", qId);
			if (eqDatum.yAxisScale != null && eqDatum.yAxisScale.haveLabels()) {
				final String yAxisType = CAPTS_TYPE_DBVALUES[Y_AXIS_DATA];
				debugPrint(SELF + "writing y-axis scale of eqDatum ", qdNum, 
						" in Q", qId, ": ", eqDatum.yAxisScale.toEnglish());
				int yDatumNum = 1;
				final String[] yAxisData = eqDatum.yAxisScale.getData();
				for (final String yDatum : yAxisData) {
					pstmt.setInt(fields.indexOf(CAPTS_QID) + 1, qId);
					pstmt.setString(fields.indexOf(CAPTS_TYPE) + 1, 
							yAxisType);
					pstmt.setInt(fields.indexOf(CAPTS_NUM) + 1, yDatumNum);
					pstmt.setString(fields.indexOf(CAPTS_TEXT) + 1, yDatum);
					if (tables.local) {
						pstmt.setString(fields.indexOf(Q_AUTHOR()) + 1, authorId);
					} // if locally authored
					pstmt.addBatch();
					yDatumNum++;
				} // for each piece of data for the y-axis
			} else debugPrint(SELF + "there is no y-axis to write associated "
					+ "with eqDatum ", qDatum.serialNo, " in Q", qId);
		} else debugPrint(SELF + "qDatum ", qDatum.serialNo, " in Q", qId,
				" is not an EDiagramQDatum; no pulldown menu labels or "
				+ "y-axis data to write.");
	} // addNewCapsandLabels(PreparedStatement, CaptionsQDatum)

/* *********** Other write methods ****************/

	/** Gets the name of the Q_AUTHOR field in local tables.
	 * @return	Q_AUTHOR value
	 */
	String Q_AUTHOR() 						{ return DBLocalTables.Q_AUTHOR; }

	/** Sets serial numbers of questions in a set.  Set the numbers negative 
	 * so that questions added later will be at the end of the list.
	 * @param	qIds	unique ID numbers of questions in order of their serial
	 * numbers
	 * @throws	DBException	if there's a problem writing to the database
	 */
	void setQSerialNos(int[] qIds) throws DBException {
		final String SELF = "QuestionWrite.setQSerialNos: ";
		if (Utils.isEmpty(qIds)) return;
		debugPrint(SELF + "setting serial numbers of ", (!tables.local 
					? "master Qs" : "Qs authored by " + authorId),
				" in the following order: ", qIds);
		final String qry = toString(
				UPDATE, tables.QUESTIONS,
				SET + Q_NUM + EQUALS + QMARK 
				+ WHERE + Q_QID + EQUALS + QMARK,
				ifLocalAndQAuthorIs.getSql());
		debugPrint(SELF, qry); 
		PreparedStatement stmt = null;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			stmt = con.prepareStatement(qry);
			int serialNo = -qIds.length;
			int batchNum = 1;
			for (final int qId : qIds) {
				final StringBuilder joinedValues = setValues(stmt, 
						serialNo++, 
						qId,
						ifLocalAndQAuthorIs.getValues());
				debugPrint(SELF, "batch ", batchNum++, ": ", joinedValues); 
				stmt.addBatch(); 
			} // for each qId
			stmt.executeBatch();
			con.commit();
		} catch (SQLException e) {
			rollbackConnection(con);
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, null);
		}
	} // setQSerialNos(int[])

	/** Delete a question from the master table or a user's private table.
	 * ACE allows users to delete questions only when they are NOT part of a
	 * random group of questions in an assignment.
	 * @param	qId	unique ID of question to be deleted
	 * @throws	DBException	if there's a problem writing to the database
	 */
	void deleteQuestion(int qId) throws DBException {
		final String SELF = "QuestionWrite.deleteQuestion: ";
		debugPrint(SELF + " ****** About to delete Q with unique id ", qId);
		final List<String> imagesToDeleteFromDisk = new ArrayList<String>();
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			String[][] fieldSets;
			if (!tables.local) {
				// delete locally modified versions of this question
				final DBLocalTables localTables = new DBLocalTables();
				// delete images
				String qry = toString(
						SELECT + FIG_FIGID 
						+ FROM, localTables.FIGURES,
						WHERE, figTypeHasImage(),
						AND + FIG_QID + EQUALS + QMARK);
				final ImageRW imgWriter = new ImageRW(con);
				imagesToDeleteFromDisk.addAll(
						imgWriter.deleteImages(new SQLWithQMarks(qry, qId)));
				// delete figures, question data, evaluators, questions 
				fieldSets = new String[][] {
						{localTables.FIGURES, FIG_QID},
						{localTables.CAPTIONS, CAPTS_QID},
						{localTables.QUESTIONDATA, QD_QID},
						{localTables.EVALUATORS, EVAL_QID},
						{localTables.QUESTIONS, Q_QID}
						};
				for (final String[] fields : fieldSets) {
					qry = toString(
							DELETE_FROM, fields[0],
							WHERE, fields[1], EQUALS + QMARK);
					final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
							qId);
					debugPrint(SELF, sql_vals);
					tryUpdate(con, sql_vals);
				} // for each table
			} // if master-authored
			// delete images for this local question
			String qry = toString(
					SELECT + FIG_FIGID 
					+ FROM, tables.FIGURES,
					WHERE, figTypeHasImage(),
					AND + FIG_QID + EQUALS + QMARK,
					ifLocalAndQAuthorIs.getSql());
			SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
					qId);
			sql_vals.addValuesFrom(ifLocalAndQAuthorIs);
			final ImageRW imgWriter = new ImageRW(con, authorId);
			imagesToDeleteFromDisk.addAll(imgWriter.deleteImages(sql_vals));
			fieldSets = new String[][] {
					{tables.FIGURES, FIG_QID},
					{tables.CAPTIONS, CAPTS_QID},
					{tables.QUESTIONDATA, QD_QID},
					{tables.EVALUATORS, EVAL_QID},
					{tables.QUESTIONS, Q_QID}
					};
			for (final String[] fields : fieldSets) {
				qry = toString(
						DELETE_FROM, fields[0],
						WHERE, fields[1], EQUALS + QMARK,
						ifLocalAndQAuthorIs.getSql());
				sql_vals = new SQLWithQMarks(qry, 
						qId);
				sql_vals.addValuesFrom(ifLocalAndQAuthorIs);
				debugPrint(SELF, sql_vals);
				tryUpdate(con, sql_vals);
			} // for each table
			if (!tables.local || qId < 0) { // not locally modified
				// remove this question from assignments, assuming that this 
				// question does not appear in any randomized groups (delete 
				// button is not displayed on front end if it does)
				// (Note: any questions that are deleted would belong to a
				// nonrandom group, hence would be the only question in its
				// group, so we don't need to worry about a group lacking one
				// record where pb_num_in_group = 1.)
				qry = UPDATE + HW_QS
						+ SET + HWQS_DEPENDS + EQUALS + NULL
						+ WHERE + HWQS_DEPENDS + EQUALS + QMARK;
				sql_vals = new SQLWithQMarks(qry, 
						qId);
				debugPrint(SELF, sql_vals);
				tryUpdate(con, sql_vals);
				fieldSets = new String[][] {
						{RESP_SUBSTNS, RESP_SUBS_QID},
						{RESPONSES, RESP_QID},
						{ASSIGNED_QS, ASSGND_QS_QID},
						{HW_QS, HWQS_QID}
						};
				for (final String[] fields : fieldSets) {
					qry = toString(
							DELETE_FROM, fields[0],
							WHERE, fields[1], EQUALS + QMARK);
					sql_vals = new SQLWithQMarks(qry, 
							qId);
					debugPrint(SELF, sql_vals);
					tryUpdate(con, sql_vals);
				} // for each table
				// remove any grading parameters, extensions, 
				// allowed reaction conditions for assignments 
				// that have no more questions;
				// finally, remove any assignments that have no more questions
				fieldSets = new String[][] {
						{GRADING_PARAMS, GRADING_HWID},
						{EXTENSIONS, EXT_HWID},
						{HWRXNCONDNS, HWRXNCONDN_HWID},
						{HWSETS, HW_ID}
						};
				for (final String[] fields : fieldSets) {
					qry = toString(
							DELETE_FROM, fields[0],
							WHERE, fields[1], NOT + IN,
								parens(SELECT + HWQS_HWID + FROM + HW_QS));
					debugPrint(SELF, qry);
					tryUpdate(con, qry);
				} // for each table
			} // if deleted Q is master- or locally authored, not locally modified
			con.commit();
			ImageRW.deleteImagesFromDisk(imagesToDeleteFromDisk);
		} catch (SQLException e) {
			e.printStackTrace();
			rollbackConnection(con);
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // deleteQuestion(int)

	/** Gets the SQL phrase, FIG_TYPE = 'IMG' OR FIG_TYPE = 'VEC'. 
	 * @return	StringBuilder containing the phrase
	 */
	private StringBuilder figTypeHasImage() {
		return parens(getBuilder(
				FIG_TYPE + EQUALS, quotes(Figure.DBVALUES[Figure.IMAGE]),
				OR + FIG_TYPE + EQUALS,
					quotes(Figure.DBVALUES[Figure.IMAGE_AND_VECTORS])));
	} // figTypeHasImage()

	/** Gets the fields in a table.  The first three fields must be the 
	 * question ID number, the unique identifier of the record, and
	 * the serial number of the record, and the last field must
	 * be the phrase ID number.
	 * @param	table	the table
	 * @return	the fields in the table
	 */
	private List<String> getFields(String table) {
		final List<String> fields = new ArrayList<String>(
				tables.QUESTIONS.equals(table) ? Arrays.asList(
					Q_QID,
					Q_QID,
					Q_NUM,
					Q_STMT_ID)
				: tables.EVALUATORS.equals(table) ? Arrays.asList(
					EVAL_QID,
					EVAL_MAJORID,
					EVAL_MINORID,
					EVAL_SUBEXPR,
					EVAL_TYPE,
					EVAL_FEEDBACK, // CLOB field
					EVAL_GRADE,
					EVAL_CODEDDATA, // CLOB field
					EVAL_MOLSTRUCT, // CLOB field
					EVAL_MOLNAME, // CLOB field
					EVAL_FEEDBACKID)
				: tables.QUESTIONDATA.equals(table) ? Arrays.asList(
					QD_QID,
					QD_DATUMID,
					QD_NUM,
					QD_TYPE,
					QD_DATA, // CLOB field
					QD_NAME,
					QD_TEXT_ID)
				: tables.CAPTIONS.equals(table) ? Arrays.asList(
					CAPTS_QID,
					CAPTS_TYPE,
					CAPTS_NUM,
					CAPTS_TEXT) // CLOB field
				: Arrays.asList( // FIGURES
					FIG_QID,
					FIG_FIGID,
					FIG_NUM,
					FIG_TYPE,
					FIG_ADDL_DATA, // CLOB field
					FIG_MAIN_DATA) // CLOB field
				); 
		if (tables.local) fields.add(fields.size() - 1, Q_AUTHOR());
		return fields;
	} // getFields(String)

	/** Gets a prepared statement for inserting evaluators, figures, or question
	 * data.
	 * @param	table	the table
	 * @return	prepared statement for inserting values
	 * @throws	SQLException	if there's a problem preparing the
	 * PreparedStatement
	 */
	private PreparedStatement getPreparedStmt(String table) throws SQLException {
		final String SELF = "QuestionWrite.getPreparedStmt: ";
		final List<String> fields = getFields(table);
		final String qry = getInsertIntoValuesQMarksSQL(table, fields);
		debugPrint(SELF, qry);
		return con.prepareStatement(qry);
	} // getPreparedStmt(String)

	/** Deletes translations of certain question statements, evaluator feedbacks,
	 * or question data. Deletes all the translations of the question's
	 * (statement, feedbacks, queston data) that are not specifically flagged to
	 * be saved AND that have phraseIds that are not duplicated in another
	 * record in this table or in the corresponding table for the other kind of
	 * authoring (nmaster vs. local).
	 * @param	table	name of table containing the untranslated phrase
	 * @param	keepUniqueIds	unique IDs of records whose translations 
	 * should NOT be deleted
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private void deletePhraseIdsNotOf(String table, List<Integer> keepUniqueIds)
			throws SQLException {
		final String SELF = "QuestionWrite.deletePhraseIdsNotOf: ";
		final List<String> fields = getFields(table);
		final String phraseIdField = fields.get(fields.size() - 1);
		final String antitable = DBLocalTables.getAntitable(table);
		final String qry = toString(
				TranslnWrite.DELETE_TRANSLATIONS_BY_IDS(),
				parens(getBuilder(
					SELECT + SRCH_RESULT 
					+ FROM, parens(getBuilder(
						SELECT, phraseIdField, AS + SRCH_RESULT
						+ FROM, table,
						WHERE, fields.get(QID_FIELD_NUM), EQUALS + QMARK, 
						keepUniqueIds.isEmpty() ? "" : getBuilder(
							AND, fields.get(UNIQUE_ID_FIELD_NUM),
								NOT + IN, parensQMarks(keepUniqueIds)))),
					// delete translation only if phraseId not duplicated
					WHERE, parens(getBuilder(
						SELECT, count(),
						FROM, table,
						WHERE, phraseIdField, EQUALS + SRCH_RESULT)),
					PLUS, parens(getBuilder(
						SELECT, count(),
						FROM, antitable,
						WHERE, phraseIdField, EQUALS + SRCH_RESULT)),
					IS_1))); 
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				question.getQId());
		if (!keepUniqueIds.isEmpty()) {
			sql_vals.addValuesArray(keepUniqueIds);
		} // if there are unique IDs to keep
		debugPrint(SELF, sql_vals);
		tryUpdate(con, sql_vals);
	} // deletePhraseIdsNotOf(String, List<Integer>)

	/** Deletes evaluators, figures, or question data other than those listed.
	 * @param	table	name of table containing the records
	 * @param	keepUniqueIds	unique IDs of records that should NOT be deleted
	 * @return	list of names of files containing images that need to be deleted
	 * from disk after commit
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private List<String> deleteOtherThan(String table, 
			List<Integer> keepUniqueIds) throws SQLException {
		return deleteOtherThan(table, keepUniqueIds, NO_UNCHANGED_IMAGES);
	} // deleteOtherThan(String, List<Integer>)

	/** Deletes evaluators, figures, or question data other than those listed.
	 * @param	table	name of table containing the records
	 * @param	keepUniqueIds	unique IDs of records that should NOT be deleted
	 * @param	unchangedImages	IDs of image-and-vector figures whose images
	 * haven't changed
	 * @return	list of names of files containing images that need to be deleted
	 * from disk after commit
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private List<String> deleteOtherThan(String table, 
			List<Integer> keepUniqueIds, List<Integer> unchangedImages) 
			throws SQLException {
		final String SELF = "QuestionWrite.deleteOtherThan: ";
		final List<String> fields = getFields(table);
		final List<String> deleteImageFileNames = (table.equals(tables.FIGURES)
				? cleanUpImages(keepUniqueIds, unchangedImages) : null);
		final String qry = toString(
				DELETE_FROM, table,
				WHERE, fields.get(QID_FIELD_NUM), EQUALS + QMARK,
				ifLocalAndQAuthorIs.getSql(),
				keepUniqueIds.isEmpty() ? "" : getBuilder(
					AND, fields.get(UNIQUE_ID_FIELD_NUM),
						NOT + IN, parensQMarks(keepUniqueIds)));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				question.getQId());
		sql_vals.addValuesFrom(ifLocalAndQAuthorIs);
		if (!keepUniqueIds.isEmpty()) {
			sql_vals.addValuesArray(keepUniqueIds);
		} // if there are unique IDs to keep
		debugPrint(SELF, sql_vals);
		tryUpdate(con, sql_vals);
		return deleteImageFileNames;
	} // deleteOtherThan(String, List<Integer>, List<Integer>)

	/** Deletes row/column captions and labels associated with question data 
	 * other than those listed.
	 * @param	keepUniqueIds	unique IDs of records that should NOT be deleted
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private void deleteCapsAndLabelsOtherThan(List<Integer> keepUniqueIds) 
			throws SQLException {
		final String SELF = "QuestionWrite.deleteCapsAndLabelsOtherThan: ";
		final int qId = question.getQId();
		final List<String> fields = getFields(tables.QUESTIONDATA);
		final SQLWithQMarks whereQIdEqualsSql_vals = new SQLWithQMarks(
				toString(WHERE, fields.get(QID_FIELD_NUM), EQUALS + QMARK),
				qId);
		final SQLWithQMarks uniqueIdsSql_vals = new SQLWithQMarks(
				getBuilder(AND, fields.get(UNIQUE_ID_FIELD_NUM),
					NOT + IN, parensQMarks(keepUniqueIds)),
				keepUniqueIds);
		final SQLWithQMarks subqrySql_vals = new SQLWithQMarks();
		// erase the right kinds of data, referring to the qDatum serial numbers
		final int[][] valArrs = (question.isTable()
				? new int[][] {{ROW_DATA, ROW_DATA + 1}, 
					{COL_DATA, COL_DATA + 1}}
				: new int[][] {{COL_DATA, 1}, 
					{LABEL_DATA, 1}, 
					{Y_AXIS_DATA, 1}});
		final StringBuilder selectBld = new StringBuilder();
		boolean first = true;
		for (final int[] valArr : valArrs) {
			appendTo(selectBld, first ? "" : UNION,
					SELECT, quotes(CAPTS_TYPE_DBVALUES[valArr[0]]), 
					FROM, tables.QUESTIONDATA,
					whereQIdEqualsSql_vals.getSql(),
					ifLocalAndQAuthorIs.getSql(),
					AND + QD_NUM + EQUALS, valArr[1]);
			subqrySql_vals.addValuesFrom(whereQIdEqualsSql_vals);
			subqrySql_vals.addValuesFrom(ifLocalAndQAuthorIs);
			if (!Utils.isEmpty(keepUniqueIds)) {
				selectBld.append(uniqueIdsSql_vals.getSql());
				subqrySql_vals.addValuesFrom(uniqueIdsSql_vals);
			} // if have unique Ids to keep
			first = false;
		} // for each kind of data to be deleted
		final String qry = toString(
				DELETE_FROM, tables.CAPTIONS,
				whereQIdEqualsSql_vals.getSql(),
				ifLocalAndQAuthorIs.getSql(),
				AND + CAPTS_TYPE + IN, parens(selectBld));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry);
		sql_vals.addValuesFrom(whereQIdEqualsSql_vals);
		sql_vals.addValuesFrom(ifLocalAndQAuthorIs);
		sql_vals.addValuesFrom(subqrySql_vals);
		debugPrint(SELF, sql_vals);
		tryUpdate(con, sql_vals);
	} // deleteCapsAndLabelsOtherThan(List<Integer>)

	/** Gets names of images that need to be deleted after commit.
	 * @param	keepFigIds	unique IDs of images that should NOT be deleted
	 * @param	unchangedImages	IDs of image-and-vector figures whose images
	 * haven't changed
	 * @return	list of names of files containing images that need to be deleted
	 * from disk after commit
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private List<String> cleanUpImages(List<Integer> keepFigIds,
			List<Integer> unchangedImages) throws SQLException {
		final String SELF = "QuestionWrite.cleanUpImages: ";
		final List<Integer> keepImgIds = new ArrayList<Integer>(keepFigIds);
		if (!Utils.isEmpty(unchangedImages)) keepImgIds.addAll(unchangedImages);
		final StringBuilder qryBld = getBuilder(
				SELECT + FIG_FIGID 
				+ FROM, tables.FIGURES,
				WHERE + FIG_QID + EQUALS + QMARK
				+ AND, figTypeHasImage(),
				ifLocalAndQAuthorIs.getSql());
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qryBld, 
				question.getQId());
		sql_vals.addValuesFrom(ifLocalAndQAuthorIs);
		if (!keepImgIds.isEmpty()) {
			sql_vals.addToSql(AND + FIG_FIGID + NOT + IN, parensQMarks(keepImgIds));
			sql_vals.addValuesArray(keepImgIds);
		} // if there are additional values to add
		final ImageRW imageDeleter = new ImageRW(con, authorId);
		return imageDeleter.deleteImages(sql_vals); 
	} // cleanUpImages(List<Integer>, List<Integer>)

	/** Renumbers out-of-sequence evaluators, figures, or question data.
	 * Because evaluators' unique IDs are also their serial numbers, we change
	 * their serial numbers to negative numbers first to avoid a primary key 
	 * error, then change all the negative to positive. 
	 * @param	table	name of table containing the records
	 * @param	movedIds	new serial numbers of records that need to be
	 * renumbered, mapped by their unique IDs
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private void renumberMoved(String table, Map<Integer, Integer> movedIds)
			throws SQLException {
		final String SELF = "QuestionWrite.renumberMoved: ";
		if (movedIds.isEmpty()) return;
		final List<String> fields = getFields(table);
		final boolean isEval = table.equals(tables.EVALUATORS);
		final int serialNumFieldNum = (isEval
				? UNIQUE_ID_FIELD_NUM : SERIAL_NUM_FIELD_NUM);
		final int qId = question.getQId();
		final String qry = toString(
				UPDATE, table,
				SET, fields.get(serialNumFieldNum), EQUALS + QMARK 
				+ WHERE, fields.get(QID_FIELD_NUM), EQUALS + QMARK
				+ AND, fields.get(UNIQUE_ID_FIELD_NUM), EQUALS + QMARK,
				ifLocalAndQAuthorIs.getSql());
		debugPrint(SELF, qry); 
		PreparedStatement stmt = null;
		try {
			stmt = con.prepareStatement(qry);
			final List<Integer> uniqueIds = 
					new ArrayList<Integer>(movedIds.keySet());
			final int factor = (isEval ? -1 : 1);
			int batchNum = 1;
			for (final Integer uniqueId : uniqueIds) {
				final Integer newSerialNo = movedIds.get(uniqueId);
				final StringBuilder joinedValues = setValues(stmt,
						factor * newSerialNo.intValue(),
						qId,
						uniqueId,
						ifLocalAndQAuthorIs);
				debugPrint(SELF, "batch ", batchNum++, ": ", joinedValues); 
				stmt.addBatch();
			} // while there are items with changed serial numbers
			stmt.executeBatch();
			if (isEval) {
				final String update = toString(
						UPDATE, tables.EVALUATORS,
						SET + EVAL_MAJORID + EQUALS + '-' + EVAL_MAJORID 
						+ WHERE + EVAL_MAJORID + LESS_THAN + '0' 
						+ AND + EVAL_QID + EQUALS + QMARK,
						ifLocalAndQAuthorIs.getSql());
				final SQLWithQMarks sql_vals = new SQLWithQMarks(update,
						qId,
						ifLocalAndQAuthorIs);
				debugPrint(SELF, sql_vals);
				tryUpdate(con, sql_vals);
			} // if updating evaluators
		} finally {
			closeConnection(null, stmt, null);
		} // try
	} // renumberMoved(String, Map<Integer, Integer>)

} // QuestionWrite
