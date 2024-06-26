package com.epoch.db;

import static com.epoch.db.dbConstants.ForumRWConstants.*;
import static com.epoch.db.dbConstants.HWRWConstants.*;
import static com.epoch.db.dbConstants.ImageConstants.*;
import static com.epoch.db.dbConstants.QuestionsRWConstants.*;
import static com.epoch.db.dbConstants.ResponsesConstants.*;
import static com.epoch.db.dbConstants.SynthDataConstants.*;
import static com.epoch.db.dbConstants.TextbookRWConstants.*;
import static com.epoch.db.dbConstants.TranslationsConstants.*;
import static com.epoch.db.dbConstants.UserRWConstants.*;
import chemaxon.util.DatabaseTools;
import com.epoch.assgts.Assgt;
import com.epoch.assgts.assgtConstants.AssgtConstants;
import com.epoch.assgts.AssgtQGroup;
import com.epoch.chem.MolString;
import com.epoch.courseware.ForumPost;
import com.epoch.energyDiagrams.diagramConstants.EnergyQDatumConstants;
import com.epoch.evals.CombineExpr;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.EvalManager;
import com.epoch.evals.evalConstants.EvalImplConstants;
import com.epoch.evals.impl.chemEvals.Atoms;
import com.epoch.evals.impl.genericQEvals.clickEvals.ClickHere;
import com.epoch.evals.impl.genericQEvals.tableEvals.TableTextText;
import com.epoch.exceptions.DBException;
import com.epoch.genericQTypes.TableQ;
import com.epoch.genericQTypes.genericQConstants.ClickConstants;
import com.epoch.lewis.LewisMolecule;
import com.epoch.qBank.Figure;
import com.epoch.qBank.QDatum;
import com.epoch.qBank.Question;
import com.epoch.qBank.qBankConstants.QuestionConstants;
import com.epoch.qBank.qBankConstants.CaptionsQDatumConstants;
import com.epoch.synthesis.Synthesis;
import com.epoch.textbooks.TextContent;
import com.epoch.utils.DateUtils;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import com.epoch.xmlparser.XMLUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Contains one-time data conversion methods. */
public final class DataConversion extends DBCommon 
		implements AssgtConstants, CaptionsQDatumConstants, ClickConstants, 
		EnergyQDatumConstants, EvalImplConstants, QuestionConstants {

	/** Commonly used constant in SQL commands. */
	private static final String CLOB = "CLOB";
	/** Commonly used constant in SQL commands. */
	private static final String ALTER_TABLE = " ALTER TABLE ";
	/** Commonly used constant in SQL commands. */
	private static final String DROP = " DROP ";
	/** Common SQL phrase representing two strings separated by "/". */
	private static final String PERC2 = quotes("%/%");
	/** Common SQL phrase representing three strings separated by "/". */
	private static final String PERC3 = quotes("%/%/%");
	/** Common SQL phrase representing four strings separated by "/". */
	private static final String PERC4 = quotes("%/%/%/%");
	/** Common SQL phrase representing five strings separated by "/". */
	private static final String PERC5 = quotes("%/%/%/%/%");
	/** Commonly used phrase in log output. */
	private static final String BATCH_FAILED_HWSET = "batch insert failed on assgt ";
	
	/** Field in OLD_IMAGES. */
	static final String IMG_DATA = "image"; // BLOB
	/** Field in OLD_IMAGES. */
	static final String IMG_TYPE = "extension";

	private static void debugPrint(Object... msg) {
		 alwaysPrint(msg);
	}

/* *************** One-time data conversion methods *********************/

	/** Makes foreign keys pointing to users_v4.user_id deferrable.
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void makeForeignKeysDeferrable() throws DBException {
		final String SELF = "DataConversion.makeForeignKeysDeferrable: ";
		final String[] indepTableCol = new String[] {USERS, USER_ID};
		Connection con = null;
		int numDependent = 0;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			final List<String[]> depTablesCols = 
					DBSchema.getDepTablesCols(con);
			for (final String[] depTableCol : depTablesCols) {
				// drop the constraint that is not deferrable
				String updateStr = toString(
						ALTER_TABLE, depTableCol[TABL_NUM],
						DROP + " CONSTRAINT ", depTableCol[FK_NAME]);
				debugPrint(SELF + updateStr);
				tryUpdate(con, updateStr);
				// add back the constraint but now deferrable
				updateStr = toString(
						ALTER_TABLE, depTableCol[TABL_NUM],
						ADD + " CONSTRAINT ", depTableCol[FK_NAME], 
						" FOREIGN KEY ", parens(depTableCol[COL_NUM]),
						" REFERENCES " + USERS, parens(USER_ID),
						" DEFERRABLE ");
				debugPrint(SELF + updateStr);
				tryUpdate(con, updateStr);
				numDependent++;
			} // for each dependent table and column
			con.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		} // try
		debugPrint(SELF, numDependent, 
				" constraint(s) pointing to USERS.USER_ID set to deferrable");
	} // makeForeignKeysDeferrable()

	/** Gets all translated phrases in a language.  Used to find phrases with 
	 * non-ASCII characters so they can be converted into character entity 
	 * representations.
	 * @param	language a language
	 * @return	map of translated phrase by ID
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<Integer, String> getAllTranslations(String language) 
			throws DBException {
		final String SELF = "DataConversion.getAllTranslations: ";
		final Map<Integer, String> all = new HashMap<Integer, String>();
		final String qry = toString(
				SELECT, joinAll(
					PHRASE_ID,
					PHRASE_TRANSLN), 
				FROM + TRANSLATIONS
				+ WHERE + PHRASE_LANG + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				language);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final int id = rs.getInt(PHRASE_ID);
				final String xlatn = lobToString(rs, PHRASE_TRANSLN);
				all.put(Integer.valueOf(id), xlatn);
			} // if there's a result
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return all;
	} // getAllTranslations(String)

	/** Replaces an existing translation with a new one.  Used to convert
	 * non-ASCII characters into character entity representations.
	 * @param	phraseId	the ID of the phrase
	 * @param	xlatn	the new translation 
	 * @param	language	the language of translation
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void putOne(int phraseId, String xlatn, String language)
			throws DBException {
		final String SELF = "DataConversion.putOne: ";
		final String qry = toString(
				UPDATE + TRANSLATIONS
				+ SET + PHRASE_TRANSLN + EQUALS, empty(BLOB), 
				WHERE + PHRASE_ID + EQUALS + QMARK 
				+ AND + PHRASE_LANG + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				phraseId,
				language);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			tryUpdate(con, sql_vals);
			closeConnection(null, stmt, null);
			sql_vals.setSql(SELECT + PHRASE_TRANSLN
					+ FROM + TRANSLATIONS
					+ WHERE + PHRASE_ID + EQUALS + QMARK 
					+ AND + PHRASE_LANG + EQUALS + QMARK 
					+ FOR_UPDATE);
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) {
				final Blob blob = rs.getBlob(PHRASE_TRANSLN);
				debugPrint(SELF + "setting value of BLOB with phraseId ", 
						phraseId, " and language ", language, " to: ", xlatn);
				OutputStream dataOut = null;
				try {
					dataOut = blob.setBinaryStream(BLOB_BEGIN);
					dataOut.write(
						xlatn.getBytes(StandardCharsets.UTF_8)
					);
				} catch (IOException e) {
					Utils.alwaysPrint(SELF + "unable to write to BLOB "
							+ "for phraseId ", phraseId, " and language ", 
							language, " with translation ", xlatn);
				} finally {
					try {
						if (dataOut != null) dataOut.close();
					} catch (IOException e) {
						debugPrint(SELF + "IOException");
					}
				} // try
			} // for each BLOB to update
			con.commit();
		} catch (SQLException e) {
			rollbackConnection(con);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			rollbackConnection(con);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
	} // putOne(int, String, String)

	/** Converts whole instances of 1H and 2H to ^1H and ^2H. 
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void convert1H2H() throws DBException {
		final String SELF = "DataConversion.convert1H2H: ";
		Connection con = null;
		Statement stmt = null;
		PreparedStatement batchStmt = null;
		ResultSet rs = null;
		final DBTables masterTables = new DBTables();
		final DBLocalTables localTables = new DBLocalTables();
		final String[] tables = 
				{QSETS, 
				MODIFIED_HEADERS, 
				masterTables.QUESTIONS, 
				localTables.QUESTIONS,
				masterTables.EVALUATORS, 
				localTables.EVALUATORS};
		final String[][] fieldsOfTable = {
				{QSET_COMMONQSTATEMENT, QSET_ID},
				{MODHEAD_COMMONQSTATEMENT, MODHEAD_QSETID, MODHEAD_AUTHOR},
				{Q_STATEMENT, Q_QID},
				{Q_STATEMENT, Q_QID, DBLocalTables.Q_AUTHOR},
				{EVAL_FEEDBACK, EVAL_QID, EVAL_MAJORID},
				{EVAL_FEEDBACK, EVAL_QID, EVAL_MAJORID, DBLocalTables.Q_AUTHOR}
				};
		final int[] numConversions = new int[tables.length];
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			for (int tableNum = 0; tableNum < tables.length; tableNum++) {
				final String table = tables[tableNum];
				final String[] fields = fieldsOfTable[tableNum];
				final int numFields = fields.length;
				final String qry = toString(
						SELECT, join(fields), FROM, table, 
						WHERE, fields[0], IS_NOT_NULL);
				stmt = con.createStatement();
				alwaysPrint(qry);
				rs = stmt.executeQuery(qry);
				final StringBuilder updateBld = getBuilder(UPDATE, table, 
						SET, fields[0], EQUALS + QMARK 
						+ WHERE, fields[1], EQUALS + QMARK);
				for (int fldNum = 2; fldNum < numFields; fldNum++) {
 					appendTo(updateBld, AND, fields[fldNum], EQUALS + QMARK);
				} // for each additional field
				batchStmt = con.prepareStatement(updateBld.toString());
				while (rs.next()) {
					final String origStr = rs.getString(fields[0]);
					final String str = 
							origStr.replaceAll("(\\s)(1|2)H", "$1^$2H")
								.replaceAll("^(1|2)H", "^$1H");
					if (!origStr.equals(str)) {
						batchStmt.setString(1, str);
						batchStmt.setInt(2, rs.getInt(fields[1]));
						for (int fldNum = 2; fldNum < numFields; fldNum++) {
							if (tableNum % 2 != 0 && fldNum == numFields - 1) {
								batchStmt.setString(fldNum, 
										rs.getString(fields[fldNum]));
							} else {
								batchStmt.setInt(fldNum, 
										rs.getInt(fields[fldNum]));
							} // if last field of every 2nd table
						} // for each additional field
						batchStmt.addBatch();
						numConversions[tableNum]++;
					} // if there was a substitution
				} // for each entry in the table
				batchStmt.executeUpdate();
			} // for each table
			con.commit();
		} catch (SQLException e) {
			rollbackConnection(con);
			alwaysPrint(SELF + "couldn't delete English phrase and translations");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			rollbackConnection(con);
			alwaysPrint(SELF + "couldn't get phrase in non-English languages");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(null, batchStmt, null);
			closeConnection(con, stmt, rs);
		} // try
		for (int tableNum = 0; tableNum < numConversions.length; tableNum++) {
			alwaysPrint(SELF, numConversions[tableNum], 
					(tableNum % 2 == 0 ? " master-" : " locally "), "authored ",
					(tableNum <= 1 ? "common question statement(s)"
						: tableNum >= 4 ? "feedback(s)"
						: "question statement(s)"), " modified.");
		} // for each table
	} // convert1H2H()

	/** Moves into the molstruct and molname fields the test string in 
	 * evaluators that compare a text response to strings. 
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void convertTextCont() throws DBException {
		final String SELF = "DataConversion.convertTextCont: ";
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		int numConversions = 0;
		try {
			con = getPoolConnection();
			final boolean[] authorTypes = new boolean[] {false, true};
			final int[] evalTypes = new int[] {
					TEXT_CONT, TABLE_TEXT, TABLE_CT_TXT, 
					TBL_TXT_TXT, TBL_TXT_NUM, TBL_NUM_TXT};
			final String[] evalCodes = new String[evalTypes.length];
			int evalNum = 0;
			for (final int evalType : evalTypes) {
				evalCodes[evalNum++] = EVAL_CODES[evalType];
			} // for each evaluator type
			for (final boolean local : authorTypes) {
				final DBTables tables = getTables(local);
				final String Q_AUTHOR = (local 
						? DBLocalTables.Q_AUTHOR : null);
				String qry = toString(
						SELECT, joinAll(
							EVAL_QID,
							EVAL_MAJORID,
							EVAL_MINORID,
							EVAL_CODEDDATA,
							EVAL_TYPE),
						!local ? "" :  postjoin(Q_AUTHOR),
 						FROM, tables.EVALUATORS, 
						WHERE + EVAL_TYPE + IN, parensQuotes(evalCodes));
				alwaysPrint(qry);
				stmt = con.createStatement();
				rs = stmt.executeQuery(qry);
				while (rs.next()) {
					final int qId = rs.getInt(EVAL_QID);
					final int majorId = rs.getInt(EVAL_MAJORID);
					final int minorId = rs.getInt(EVAL_MINORID);
					final String storedCD = rs.getString(EVAL_CODEDDATA);
					final String matchCode = rs.getString(EVAL_TYPE);
					final String authorId = (local ? rs.getString(Q_AUTHOR) : "");
					final EvalInterface eval = EvalManager.loadEvaluatorImpl(
							matchCode, storedCD, "");
					final String newCD = eval.getCodedData();
					if (storedCD.equals(newCD)) continue; // already converted
					final String[] splitStored = storedCD.split("/");
					final int numHave = splitStored.length;
					final int numKept = newCD.split("/").length;
					final StringBuilder strBld = new StringBuilder();
					for (int i = numKept; i < numHave; i++) {
						if (strBld.length() > 0) strBld.append('/');
						strBld.append(splitStored[i]);
					} // for each part of the string
					String testString = strBld.toString();
					if (Utils.indexOf(EVAL_CODES, matchCode) == TBL_TXT_TXT) {
						final String OLD_TWO_STR_SEP = "\\|";
						testString = testString.replaceAll(OLD_TWO_STR_SEP, 
								TableTextText.TWO_STR_SEP);
					} // if the evaluator has two strings
					final int MAX_CPD_NAME_CHARS = 1000; // obsolete from ACE 3.1
					final String molName = Utils.chopCERString(testString,
							MAX_CPD_NAME_CHARS);
					final StringBuilder whereBld = getBuilder(
							WHERE + EVAL_QID + EQUALS, qId, 
							AND + EVAL_MAJORID + EQUALS, majorId, 
							AND + EVAL_MINORID + EQUALS, minorId);
					if (local) appendTo(whereBld, 
							AND + Q_AUTHOR + EQUALS, quotes(authorId));
					qry = toString(UPDATE, tables.EVALUATORS, 
							SET, equalsJoin(
								EVAL_CODEDDATA, quotes(newCD),
								EVAL_MOLSTRUCT, empty(BLOB),
								EVAL_MOLNAME, quotes(molName)), 
							whereBld);
					alwaysPrint(qry);
					tryUpdate(con, qry);
					qry = toString(
							SELECT + EVAL_MOLSTRUCT 
							+ FROM, tables.EVALUATORS, 
							whereBld, FOR_UPDATE);
					alwaysPrint(qry, " [with '", testString, "']");
					numConversions++;
					stringIntoBlob(con, qry, testString);
				} // while there are more evaluators
			} // for master and local tables
		} catch (SQLException e) {
			alwaysPrint(SELF + "couldn't convert text evaluator");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			alwaysPrint(SELF + "couldn't convert text evaluator");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		} // try
		alwaysPrint(SELF, numConversions, " evaluator(s) updated.");
	} // convertTextCont()

	/** Writes BLOBs into a new CLOB field in the same table.  
	 * @return	message describing number of conversions
	 */
	public static String writeBLOBsAsCLOBs() {
		final String SELF = "DataConversion.writeBLOBsAsCLOBs: ";
		final DBTables masterTables = new DBTables();
		final DBLocalTables localTables = new DBLocalTables();
		// tables containing BLOBs to be converted
		final String[] tables = {
				RXN_CONDNS, 
				TRANSLATIONS, 
				masterTables.FIGURES, 
				localTables.FIGURES,
				masterTables.QUESTIONDATA, 
				localTables.QUESTIONDATA,
				masterTables.EVALUATORS, 
				localTables.EVALUATORS
				};
		// first field in each array is BLOB to be converted to CLOB;
		// other fields are NUMBERs or VARCHAR2s that together identify the
		// record uniquely
		final String[][] fieldsOfTable = {
				{RXNCOND_DEF, RXNCOND_ID},
				{PHRASE_TRANSLN, PHRASE_LANG, PHRASE_ID},
				{FIG_MAIN_DATA, FIG_FIGID},
				{FIG_MAIN_DATA, FIG_FIGID, DBLocalTables.Q_AUTHOR},
				{QD_DATA, QD_DATUMID},
				{QD_DATA, QD_DATUMID, DBLocalTables.Q_AUTHOR},
				{EVAL_MOLSTRUCT, EVAL_QID, EVAL_MAJORID, EVAL_MINORID},
				{EVAL_MOLSTRUCT, EVAL_QID, EVAL_MAJORID, EVAL_MINORID,
					DBLocalTables.Q_AUTHOR}
				};
		// convert field names to upper case so can compare them more easily to
		// names obtained from user_table_columns
		for (final String[] fields : fieldsOfTable)
			for (int fldNum = 0; fldNum < fields.length; fldNum++)
				fields[fldNum] = fields[fldNum].toUpperCase(Locale.US);
		final StringBuilder returnBld = new StringBuilder();
		for (int tableNum = 0; tableNum < tables.length; tableNum++) {
			try {
				final String msg = writeBLOBsAsCLOBs(tables[tableNum], 
						fieldsOfTable[tableNum]);
				if (msg != null) {
					returnBld.append(msg);
					dropAndRename(tables[tableNum], fieldsOfTable[tableNum][0]);
				} else {
 					appendTo(returnBld, "Field ", fieldsOfTable[tableNum][0], 
							" of table ", tables[tableNum], 
							" is already a CLOB.");
				} // if msg is null
				returnBld.append('\n');
			} catch (DBException e) {
 				appendTo(returnBld, SELF + "conversion of ", 
						tables[tableNum], " failed.\n");
			} // try
		} // for each table
		try {
			updateCodedData();
		} catch (DBException e) {
			returnBld.append(SELF + "update of coded data failed.");
		}
		return returnBld.toString();
	} // writeBLOBsAsCLOBs()

	/** Suffix for temporary name of CLOB field. */
	private static final String _CLOB = '_' + CLOB;

	/** Writes BLOBs into a new CLOB field in the same table.  
	 * @param	table	the table
	 * @param	fields	fields of the table needed for the query
	 * @return	message describing number of conversions
	 * @throws	DBException	if there's a problem writing to the database
	 */
	private static String writeBLOBsAsCLOBs(String table, String[] fields) 
			throws DBException {
		final String SELF = "DataConversion.writeBLOBsAsCLOBs: ";
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		final int numFields = fields.length;
		String returnMsg = null;
		final String TABLE_COLS = "user_tab_columns";
		final String TAB_NAME = "table_name";
		final String COL_NAME = "column_name";
		final String DATA_TYPE = "data_type";
		try {
			con = getPoolConnection();
			// see if column is already CLOB
			final String qry = toString(
					SELECT, joinAll(
						COL_NAME, 
						DATA_TYPE), 
					FROM + TABLE_COLS 
					+ WHERE, toUpper(TAB_NAME), EQUALS, 
						quotes(table.toUpperCase(Locale.US)));
			stmt = con.createStatement();
			alwaysPrint(qry);
			rs = stmt.executeQuery(qry);
			final boolean[] isInt = new boolean[fields.length];
			while (rs.next()) {
				// convert to upper case to compare to field names, which are
				// already in upper case
				final String colName = 
						rs.getString(COL_NAME).toUpperCase(Locale.US);
				final String dataType = 
						rs.getString(DATA_TYPE).toUpperCase(Locale.US);
				if (colName.equals(fields[0]) && dataType.equals(CLOB)) {
					return returnMsg;
				} // if field is already a CLOB
				final int posn = Utils.indexOf(fields, colName);
				if (posn >= 0) {
					// no relevant number fields are floats
					isInt[posn] = "NUMBER".equals(dataType); 
				} // if field is of interest
			} // for each new record
			alwaysPrint(SELF + "for table ", table, ", fields = ", fields, 
					", parallel isInt = ", isInt);
			// add the new CLOB column
			String update = toString(
					ALTER_TABLE, table, ADD, fields[0], _CLOB, ' ', CLOB);
			debugPrint(SELF, update);
			tryUpdate(con, update);
			// fill the new CLOB column with EMPTY_CLOB() values
			update = toString(
					UPDATE, table, SET, fields[0], _CLOB, EQUALS, empty(CLOB));
			debugPrint(SELF, update);
			tryUpdate(con, update);
			closeConnection(con, stmt, rs);
			// build prepared statement query with ? for values
			final StringBuilder updateBld = getBuilder(UPDATE, table, 
					SET, fields[0], _CLOB, EQUALS + QMARK + WHERE);
			for (int fldNum = 1; fldNum < numFields; fldNum++) {
				if (fldNum > 1) updateBld.append(AND);
 				appendTo(updateBld, fields[fldNum], EQUALS + QMARK);
			} // for each field that is a key
			update = updateBld.toString();
			// do data conversion
			con = getPoolConnection();
			returnMsg = writeBLOBsAsCLOBs(con, table, fields, isInt, update);
		} catch (SQLException e) {
			alwaysPrint(SELF + "couldn't convert BLOB to CLOB");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			alwaysPrint(SELF + "couldn't convert BLOB to CLOB");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		} // try
		return returnMsg;
	} // writeBLOBsAsCLOBs(String, String[])

	/** Writes BLOBs into a new CLOB field in the same table.  
	 * @param	con	database connection
	 * @param	table	the table
	 * @param	fields	fields of the table needed for the query
	 * @param	isInt	whether each field is an integer field
	 * @param	update	update query with ? for values; to be used by
	 * PreparedStatement
	 * @return	message describing number of conversions
	 * @throws	DBException	if there's a problem writing to the database
	 */
	private static String writeBLOBsAsCLOBs(Connection con, String table, 
			String[] fields, boolean[] isInt, String update) throws DBException {
		final String SELF = "DataConversion.writeBLOBsAsCLOBs: ";
		Connection conUpdate = null;
		PreparedStatement stmtUpdate = null;
		Statement stmt = null;
		ResultSet rs = null;
		int numConversions = 0;
		final int numFields = fields.length;
		try {
			// get all BLOBs to be converted
			final String qry = toString(
					SELECT, join(fields), 
					FROM, table, WHERE, fields[0], IS_NOT_NULL);
			stmt = con.createStatement();
			alwaysPrint(qry);
			rs = stmt.executeQuery(qry);
			conUpdate = getPoolConnection();
			conUpdate.setAutoCommit(false);
			stmtUpdate = conUpdate.prepareStatement(update);
			while (rs.next()) {
				for (int fldNum = 0; fldNum < numFields; fldNum++) {
					final String field = fields[fldNum];
					if (isInt[fldNum]) {
						stmtUpdate.setInt(fldNum + 1, rs.getInt(field));
					} else {
						final String value = (fldNum == 0
								? lobToString(rs, field) 
								: rs.getString(field));
						stmtUpdate.setString(fldNum + 1, value);
					} // if field is integer
				} // for each field
				stmtUpdate.addBatch();
				numConversions++;
				if (numConversions % 100 == 0) {
					stmtUpdate.executeBatch();
					stmtUpdate.clearBatch();
				} // if time to run batch
			} // for each entry in the table
			if (numConversions % 100 != 0) {
				stmtUpdate.executeBatch();
			} // if need to run batch
			conUpdate.commit();
		} catch (SQLException e) {
			alwaysPrint(SELF + "couldn't convert BLOB to CLOB");
			e.printStackTrace();
			rollbackConnection(conUpdate);
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			alwaysPrint(SELF + "couldn't convert BLOB to CLOB");
			e.printStackTrace();
			try {
				if (conUpdate != null) conUpdate.rollback();
			} catch (SQLException e2) {
				debugPrint(SELF + "SQLException");
			}
			throw new DBException(e.getMessage());
		} finally {
			closeStmtAndRs(stmt, rs);
			closeConnection(conUpdate, stmtUpdate, null);
		} // try
		final String returnMsg = toString(numConversions, " record(s) in field ", 
				fields[0], " of table ", table, " converted.");
		alwaysPrint(returnMsg);
		return returnMsg;
	} // writeBLOBsAsCLOBs(Connection, String, String[], boolean[], String)

	/** Drops the BLOB column and renames the CLOB column to the name of the
	 * original BLOB column.  If this method is combined with
	 * writeBLOBsAsCLOBs(), we get an Oracle error, so keep it separate.
	 * @param	table	the table
	 * @param	field	field of the table needed for the query
	 * @throws	DBException	if there's a problem writing to the database
	 */
	private static void dropAndRename(String table, String field) 
			throws DBException {
		final String SELF = "DataConversion.dropAndRename: ";
		Connection con = null;
		try {
			con = getPoolConnection();
			// drop old BLOB column
			String alter = toString(ALTER_TABLE, table, " DROP COLUMN ", field);
			debugPrint(SELF, alter);
			tryUpdate(con, alter);
			closeConnection(con);
			// give new CLOB column same name as old BLOB column
			con = getPoolConnection();
			alter = toString(
					ALTER_TABLE, table, 
					" RENAME COLUMN ", field, _CLOB, " TO ", field);
			debugPrint(SELF, alter);
			tryUpdate(con, alter);
		} catch (SQLException e) {
			alwaysPrint(SELF + "couldn't drop ", field, " or rename ",
					field, _CLOB, " column in table ", table);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			alwaysPrint(SELF + "couldn't drop ", field, " or rename ",
					field, _CLOB, " column in table ", table);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		} // try
	} // dropAndRename(String, String)

	/** Updates evaluators' coded data in archaic formats to make it uniform,  
	 * removes _Y from match codes of certain evaluators; also updates the 
	 * coded data of evaluators stored in question data of synthesis 
	 * questions.
	 * <br><code>Atoms</code>: add flag for contiguity of counted atoms
	 * <br><code>Contains</code>: add flag for exact, default, or ignoring 
	 * of charges, radicals, and isotopes; exact for substructures, 
	 * ignore for skeletons
	 * <br><code>FnalGroup</code>: convert isPos/groupId to 
	 * groupId/=&lt;&gt;number/number
	 * (also in synthesis question data beginning with 14/)
	 * <br><code>Formula</code>: convert isPos/formula to 
	 * =&lt;&gt;/number/countEach/formula
	 * (also in synthesis question data beginning with 15/)
	 * <br><code>MapProperty</code>: change 1|2 and true|false to Y|N, 
	 * add patternOnly flags
	 * <br><code>MechRule</code>: convert from 3 to 4 data by appending 
	 * integer of last (float) flag
	 * <br><code>MechFlowsValid</code>: convert leniency flag from Y|N to 
	 * integer so can handle both resonance and double bond stereochemistry
	 * <br><code>MechCounter</code>: convert =&lt;&gt; number to symbol, 
	 * add decrement
	 * <br><code>MechProdStartIs</code>: convert leniency flag from Y|N to 
	 * integer so can handle both resonance and double bond stereochemistry
	 * <br><code>MechSubstructure</code>: change true|false to Y|N, add 
	 * flag to ignore or consider charge, radicals, isotopes
	 * <br><code>MultipleCheck</code>: convert old 8-member operator to 
	 * isPos and 4-member operator
	 * <br><code>MultipleNumChosen</code>: convert true|false to Y|N, 
	 * convert old (nonstandard) numerical =&lt;&gt; operators to symbols
	 * <br><code>NumMols</code>: add flags for whether to count distinct 
	 * molecules and what to consider when deciding whether they are distinct
	 * <br><code>Rings</code>: convert ringsOper/numRings to
	 * countEach/ringsOper/numRings/molsOper/numMols (also in synthesis 
	 * question data beginning with 1/)
	 * <br><code>SynthOneRxn</code>: add flag to look for whole cpds or 
	 * substructures
	 * <br><code>SynthSteps</code>: convert numerical =&lt;&gt; operators 
	 * to symbols, add decrement
	 * <br><code>Weight</code>: convert numerical =&lt;&gt; operators to 
	 * symbols, convert last flag from number to word
	 * @see	com.epoch.evals.impl.chemEvals.Atoms
	 * @see	com.epoch.evals.impl.chemEvals.Contains
	 * @see	com.epoch.evals.impl.chemEvals.FnalGroup
	 * @see	com.epoch.evals.impl.chemEvals.HasFormula
	 * @see	com.epoch.evals.impl.chemEvals.MapProperty
	 * @see	com.epoch.evals.impl.chemEvals.NumMols
	 * @see	com.epoch.evals.impl.chemEvals.Rings
	 * @see	com.epoch.evals.impl.chemEvals.mechEvals.MechRule
	 * @see	com.epoch.evals.impl.chemEvals.mechEvals.MechFlowsValid
	 * @see	com.epoch.evals.impl.chemEvals.mechEvals.MechCounter
	 * @see	com.epoch.evals.impl.chemEvals.mechEvals.MechProdStartIs
	 * @see	com.epoch.evals.impl.chemEvals.mechEvals.MechSubstructure
	 * @see	com.epoch.evals.impl.chemEvals.synthEvals.SynthOneRxn
	 * @see	com.epoch.evals.impl.chemEvals.synthEvals.SynthSteps
	 * @see	com.epoch.evals.impl.chemEvals.Weight
	 * @see	com.epoch.evals.impl.genericQEvals.multEvals.MultipleCheck
	 * @see	com.epoch.evals.impl.genericQEvals.multEvals.MultipleNumChosen
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void updateCodedData() throws DBException {
		final String SELF = "DataConversion.updateCodedData: ";
		final DBTables masterTables = new DBTables();
		final DBLocalTables localTables = new DBLocalTables();
		final String UNITE_SLASH_0 = UNITE + quotes("/0");
		final String trueToY = replace(EVAL_CODEDDATA, "true", "Y");
		final String falseToN = replace(EVAL_CODEDDATA, "false", "N");
		final int numSymbols = Atoms.SYMBOLS.length;
		final String[] numToSymbol = new String[numSymbols];
		for (int num = 0; num < numSymbols; num++) {
			numToSymbol[num] = 
					replace(EVAL_CODEDDATA, slash(num), slash(Atoms.SYMBOLS[num]));
		} // for each number and symbol
		final String[] evalTables = 
				{masterTables.EVALUATORS, localTables.EVALUATORS};
		/* UPDATE table SET EVAL_CODEDDATA = [1st array member]
		   WHERE EVAL_TYPE = '[2nd array member, match code]'
		   [optional] AND EVAL_CODEDDATA [3rd array member, LIKE/NOT LIKE]
		*/
		final String[][] EVAL_SQL_PHRASE_ARRS = {
				// Atoms: add flag for contiguity of counted atoms
				{EVAL_CODEDDATA + UNITE + quotes("/N"), 
					EVAL_CODES[NUM_ATOMS], NOT + LIKE + PERC4},
				// Contains: add flag for exact, default, or ignoring of
				// charges, radicals, and isotopes when data has only
				// two members; exact for substructures, ignore for skeletons
				{EVAL_CODEDDATA + UNITE_SLASH_0, 
					EVAL_CODES[SKELETON_SUBSTRUCTURE], LIKE + quotes("%/1") 
						+ AND + EVAL_CODEDDATA + NOT + LIKE + PERC3},
				{EVAL_CODEDDATA + UNITE + quotes("/2"), 
					EVAL_CODES[SKELETON_SUBSTRUCTURE], LIKE + quotes("%/2")
						+ AND + EVAL_CODEDDATA + NOT + LIKE + PERC3},
				// FnalGroup: convert isPos/groupId to groupId/=<>number/number
				{EVAL_CODEDDATA + UNITE + quotes("/Y=/0"), 
					EVAL_CODES[FUNCTIONAL_GROUP], LIKE + quotes("false/%")},
				{replace(EVAL_CODEDDATA, "false/", ""), 
					EVAL_CODES[FUNCTIONAL_GROUP], LIKE + quotes("false/%")},
				{EVAL_CODEDDATA + UNITE + quotes("/N=/0"), 
					EVAL_CODES[FUNCTIONAL_GROUP], LIKE + quotes("true/%")},
				{replace(EVAL_CODEDDATA, "true/", ""), 
					EVAL_CODES[FUNCTIONAL_GROUP], LIKE + quotes("true/%")},
				// FnalGroup: convert numerical =<> operators to (Y|N)(=|<|>)
				{numToSymbol[0], EVAL_CODES[FUNCTIONAL_GROUP], 
					LIKE + quotes("%/0/%")},
				{numToSymbol[1], EVAL_CODES[FUNCTIONAL_GROUP], 
					LIKE + quotes("%/1/%")},
				{numToSymbol[2], EVAL_CODES[FUNCTIONAL_GROUP], 
					LIKE + quotes("%/2/%")},
				{numToSymbol[3], EVAL_CODES[FUNCTIONAL_GROUP], 
					LIKE + quotes("%/3/%")},
				{numToSymbol[4], EVAL_CODES[FUNCTIONAL_GROUP], 
					LIKE + quotes("%/4/%")},
				{numToSymbol[5], EVAL_CODES[FUNCTIONAL_GROUP], 
					LIKE + quotes("%/5/%")},
				// Formula: convert isPos/formula 
				// to (Y|N)(=|<|>)/number/countEach/formula
				{replace(EVAL_CODEDDATA, "false/", "Y=/0/N/"), 
					EVAL_CODES[HAS_FORMULA], LIKE + quotes("false/%")},
				{replace(EVAL_CODEDDATA, "N/", "Y=/0/N/"), 
					EVAL_CODES[HAS_FORMULA], LIKE + quotes("N/%")},
				{replace(EVAL_CODEDDATA, "true/", "Y>/0/N/"), 
					EVAL_CODES[HAS_FORMULA], LIKE + quotes("true/%")},
				{replace(EVAL_CODEDDATA, "Y/", "Y>/0/N/"), 
					EVAL_CODES[HAS_FORMULA], LIKE + quotes("Y/%")},
				// MapProperty: change 1|2 and true|false to Y|N, 
				// add patternOnly flags
				{regexp_replace(EVAL_CODEDDATA, "^1", "Y"), 
					EVAL_CODES[MAPPED_ATOMS]},
				{regexp_replace(EVAL_CODEDDATA, "^2", "N"), 
					EVAL_CODES[MAPPED_ATOMS]},
				{falseToN, EVAL_CODES[MAPPED_ATOMS]},
				{trueToY, EVAL_CODES[MAPPED_ATOMS]},
				{EVAL_CODEDDATA + UNITE + quotes("/N"), 
					EVAL_CODES[MAPPED_ATOMS], NOT + LIKE + PERC3},
				// MechRule: convert from 3 to 4 data by appending integer of last
				// (float) flag
				{EVAL_CODEDDATA + UNITE 
						+ regexp_replace(EVAL_CODEDDATA, ".*(\\d+)\\.\\d$", "/\\1"), 
					EVAL_CODES[MECH_RULE], NOT + LIKE + PERC4},
				// MechFlowsValid: convert leniency flag from Y|N to integer so can
				// handle both resonance and double bond stereochemistry
				{replace(EVAL_CODEDDATA, "/N", "/0"), EVAL_CODES[MECH_FLOWS]},
				{replace(EVAL_CODEDDATA, "/Y", "/1"), EVAL_CODES[MECH_FLOWS]},
				// MechCounter: convert numerical =<> operators to (Y|N)(=|<|>), 
				// add decrement
				{numToSymbol[0], EVAL_CODES[MECH_PIECES_COUNT], 
					NOT + LIKE + PERC4},
				{numToSymbol[1], EVAL_CODES[MECH_PIECES_COUNT], 
					NOT + LIKE + PERC4},
				{numToSymbol[2], EVAL_CODES[MECH_PIECES_COUNT], 
					NOT + LIKE + PERC4},
				{numToSymbol[3], EVAL_CODES[MECH_PIECES_COUNT], 
					NOT + LIKE + PERC4},
				{numToSymbol[4], EVAL_CODES[MECH_PIECES_COUNT], 
					NOT + LIKE + PERC4},
				{numToSymbol[5], EVAL_CODES[MECH_PIECES_COUNT], 
					NOT + LIKE + PERC4},
				{EVAL_CODEDDATA + UNITE + quotes("/0.0"), 
					EVAL_CODES[MECH_PIECES_COUNT], NOT + LIKE + PERC4},
				// MechProdStartIs: convert leniency flag from Y|N to integer so
				// can handle both resonance and double bond stereochemistry
				{replace(EVAL_CODEDDATA, "N", "0"), 
					EVAL_CODES[MECH_PRODS_STARTERS_IS]},
				{replace(EVAL_CODEDDATA, "Y", "1"), 
					EVAL_CODES[MECH_PRODS_STARTERS_IS]},
				// MechSubstructure: change true|false to Y|N, add flag to
				// ignore or consider charge, radicals, isotopes
				{trueToY, EVAL_CODES[MECH_SUBSTRUCTURE]},
				{falseToN, EVAL_CODES[MECH_SUBSTRUCTURE]},
				{EVAL_CODEDDATA + UNITE + quotes("/7"), 
					EVAL_CODES[MECH_SUBSTRUCTURE], NOT + LIKE + PERC2},
				// MultipleCheck: convert old 8-member operator to isPos and
				// 4-member operator
				{trueToY, EVAL_CODES[CHOICE_WHICH_CHECKED]},
				{falseToN, EVAL_CODES[CHOICE_WHICH_CHECKED]},
				{regexp_replace(EVAL_CODEDDATA, "^1", "Y/1"), 
					EVAL_CODES[CHOICE_WHICH_CHECKED], NOT + LIKE + PERC3},
				{regexp_replace(EVAL_CODEDDATA, "^3", "Y/2"), 
					EVAL_CODES[CHOICE_WHICH_CHECKED], NOT + LIKE + PERC3},
				{regexp_replace(EVAL_CODEDDATA, "^5", "Y/3"), 
					EVAL_CODES[CHOICE_WHICH_CHECKED], NOT + LIKE + PERC3},
				{regexp_replace(EVAL_CODEDDATA, "^7", "Y/4"), 
					EVAL_CODES[CHOICE_WHICH_CHECKED], NOT + LIKE + PERC3},
				{regexp_replace(EVAL_CODEDDATA, "^2", "N/1"), 
					EVAL_CODES[CHOICE_WHICH_CHECKED], NOT + LIKE + PERC3},
				{regexp_replace(EVAL_CODEDDATA, "^4", "N/2"), 
					EVAL_CODES[CHOICE_WHICH_CHECKED], NOT + LIKE + PERC3},
				{regexp_replace(EVAL_CODEDDATA, "^6", "N/3"), 
					EVAL_CODES[CHOICE_WHICH_CHECKED], NOT + LIKE + PERC3},
				{regexp_replace(EVAL_CODEDDATA, "^8", "N/4"), 
					EVAL_CODES[CHOICE_WHICH_CHECKED], NOT + LIKE + PERC3},
				// MultipleNumChosen: convert true|false to Y|N, convert old
				// (nonstandard) numerical =<> operators to (Y|N)(=|<|>)
				{trueToY, EVAL_CODES[CHOICE_NUM_CHECKED]},
				{falseToN, EVAL_CODES[CHOICE_NUM_CHECKED]},
				{regexp_replace(EVAL_CODEDDATA, "^(Y|N)/1", "\\1/Y="), 
					EVAL_CODES[CHOICE_NUM_CHECKED]},
				{regexp_replace(EVAL_CODEDDATA, "^(Y|N)/2", "\\1/Y<"), 
					EVAL_CODES[CHOICE_NUM_CHECKED]},
				{regexp_replace(EVAL_CODEDDATA, "^(Y|N)/3", "\\1/Y>"), 
					EVAL_CODES[CHOICE_NUM_CHECKED]},
				{regexp_replace(EVAL_CODEDDATA, "^(Y|N)/4", "\\1/N="), 
					EVAL_CODES[CHOICE_NUM_CHECKED]},
				{regexp_replace(EVAL_CODEDDATA, "^(Y|N)/5", "\\1/N<"), 
					EVAL_CODES[CHOICE_NUM_CHECKED]},
				{regexp_replace(EVAL_CODEDDATA, "^(Y|N)/6", "\\1/N>"), 
					EVAL_CODES[CHOICE_NUM_CHECKED]},
				// NumMols: add flags for whether to count distinct molecules
				// and what to consider when deciding whether they are distinct
				{EVAL_CODEDDATA + UNITE + quotes("/N"), 
					EVAL_CODES[NUM_MOLECULES], NOT + LIKE + PERC3},
				{EVAL_CODEDDATA + UNITE_SLASH_0, 
					EVAL_CODES[NUM_MOLECULES], NOT + LIKE + PERC4},
				// Rings: convert ringsOper/numRings to
				// countEach/ringsOper/numRings/molsOper/numMols
				{quotes("N/") + UNITE + EVAL_CODEDDATA + UNITE + quotes("/N=/0"), 
					EVAL_CODES[NUM_RINGS], NOT + LIKE + PERC3},
				// SynthOneRxn: add flag to look for whole cpds or substructures
				{EVAL_CODEDDATA + UNITE + quotes("/is"), 
					EVAL_CODES[SYNTH_ONE_RXN], NOT + LIKE + PERC2},
				// SynthSteps: convert numerical =<> operators to (Y|N)(=|<|>), 
				// add decrement
				{numToSymbol[0] + UNITE_SLASH_0, EVAL_CODES[SYNTH_STEPS]},
				{numToSymbol[1] + UNITE_SLASH_0, EVAL_CODES[SYNTH_STEPS]},
				{numToSymbol[2] + UNITE_SLASH_0, EVAL_CODES[SYNTH_STEPS]},
				{numToSymbol[3] + UNITE_SLASH_0, EVAL_CODES[SYNTH_STEPS]},
				{numToSymbol[4] + UNITE_SLASH_0, EVAL_CODES[SYNTH_STEPS]},
				{numToSymbol[5] + UNITE_SLASH_0, EVAL_CODES[SYNTH_STEPS]},
				// TableDiff: add flags to calculate grade from response,
				// highlight cells with wrong values
				{EVAL_CODEDDATA + UNITE + quotes("/Y"), 
					EVAL_CODES[TABLE_DIFF], NOT + LIKE + PERC4},
				{EVAL_CODEDDATA + UNITE + quotes("/Y"), 
					EVAL_CODES[TABLE_DIFF], NOT + LIKE + PERC5},
				// Weight: convert numerical =<> operators to (Y|N)(=|<|>), 
				// convert last flag from number to word
				{regexp_replace(EVAL_CODEDDATA, "0/(0|1)$", "Y=/\\1"), 
					EVAL_CODES[WEIGHT]},
				{regexp_replace(EVAL_CODEDDATA, "1/(0|1)$", "Y>/\\1"), 
					EVAL_CODES[WEIGHT]},
				{regexp_replace(EVAL_CODEDDATA, "2/(0|1)$", "Y</\\1"), 
					EVAL_CODES[WEIGHT]},
				{regexp_replace(EVAL_CODEDDATA, "3/(0|1)$", "N=/\\1"), 
					EVAL_CODES[WEIGHT]},
				{regexp_replace(EVAL_CODEDDATA, "4/(0|1)$", "N>/\\1"), 
					EVAL_CODES[WEIGHT]},
				{regexp_replace(EVAL_CODEDDATA, "5/(0|1)$", "N</\\1"), 
					EVAL_CODES[WEIGHT]},
				{regexp_replace(EVAL_CODEDDATA, "0$", "exact"), 
					EVAL_CODES[WEIGHT]},
				{regexp_replace(EVAL_CODEDDATA, "1$", "average"), 
					EVAL_CODES[WEIGHT]}
			};
		final String[] qdTables = 
				{masterTables.QUESTIONDATA, localTables.QUESTIONDATA};
		/* UPDATE table SET QD_DATA = [1st array member]
		   WHERE QD_TYPE = 'QDatum.DBVALUES[QDatum.SYNTH_OK_SM]'
		   AND QD_DATA LIKE [2nd array member]
		   [optional] AND QD_DATA NOT LIKE [3rd array member]
		*/
		final String[][] QD_SQL_PHRASE_ARRS = {
				// FnalGroup: convert 14/isPos/groupId 
				// to 14/groupId/=<>number/number
				{QD_DATA + UNITE + quotes("/Y=/0"), quotes("14/N/%")},
				{replace(QD_DATA, "14/N/", "14/"), quotes("14/N/%")},
				{QD_DATA + UNITE + quotes("/N=/0"), quotes("14/Y/%")},
				{replace(QD_DATA, "14/Y/", "14/"), quotes("14/Y/%")},
				// Formula: convert 15/isPos/formula 
				// to 15/(Y|N)(=|<|>)/number/countEach/formula
				{replace(QD_DATA, "15/false/", "15/Y=/0/N/"), 
					quotes("15/false/%")},
				{replace(QD_DATA, "15/N/", "15/Y=/0/N/"), quotes("15/N/%")},
				{replace(QD_DATA, "15/true/", "15/Y>/0/N/"), 
					quotes("15/true/%")},
				{replace(QD_DATA, "15/Y/", "15/Y>/0/N/"), quotes("15/Y/%")},
				// Rings: convert 1/ringsOper/numRings to
				// 1/countEach/ringsOper/numRings/molsOper/numMols
				{replace(QD_DATA, "1/", "1/N/"), quotes("1/%"), PERC4},
				{QD_DATA + UNITE + quotes("/N=/0"), quotes("1/%"), PERC5}
			};
		Connection con = null;
		Statement stmt = null;
		PreparedStatement stmtBatch = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			for (final String evalTable : evalTables) {
				// first convert eval match codes ending in _Y
				String qry = toString(
						UPDATE, evalTable, 
						SET + EVAL_TYPE + EQUALS, 
							regexp_replace(EVAL_TYPE, "(.*)_Y$", "\\1"), 
						WHERE, regexp_instr(EVAL_TYPE, quotes(".*_Y$")), 
							IS_NOT_ZERO);
				debugPrint(SELF, qry);
				tryUpdate(con, qry);
				// ready to update coded data
				final StringBuilder setCodedDataTo = getBuilder(
						UPDATE, evalTable, SET + EVAL_CODEDDATA + EQUALS);
				for (final String[] EVAL_SQL_PHRASES : EVAL_SQL_PHRASE_ARRS) {
					qry = toString(
							setCodedDataTo, EVAL_SQL_PHRASES[0], 
							WHERE + EVAL_TYPE + EQUALS, 
								quotes(EVAL_SQL_PHRASES[1]),
							EVAL_SQL_PHRASES.length <= 2 ? "" : getBuilder(
 								AND + EVAL_CODEDDATA, EVAL_SQL_PHRASES[2]));
					debugPrint(SELF, qry);
					tryUpdate(con, qry);
				} // for each update
			} // for master and local evalTables
			con.setAutoCommit(false);
			for (final String qdTable : qdTables) {
				final StringBuilder setQDataTo = getBuilder(
						UPDATE, qdTable, SET + QD_DATA + EQUALS);
				for (final String[] QD_SQL_PHRASES : QD_SQL_PHRASE_ARRS) {
					final String qry = toString(
							setQDataTo, QD_SQL_PHRASES[0], 
							WHERE + QD_TYPE + EQUALS, 
								quotes(QDatum.DBVALUES[QDatum.SYNTH_OK_SM]), 
							AND + QD_DATA + LIKE, QD_SQL_PHRASES[1],
							QD_SQL_PHRASES.length <= 2 ? "" : getBuilder(
 								AND, clobToString(QD_DATA), NOT + LIKE, 
									QD_SQL_PHRASES[2]));
								// apparently LIKE works directly on CLOBs, 
								// but NOT LIKE doesn't (nor does ORDER)
					debugPrint(SELF, qry);
					tryUpdate(con, qry);
				} // for each update
				// now change leading evaluator numbers to match codes
				String qry = toString(
						SELECT, joinAll(
							QD_DATUMID,
							QD_DATA), 
						FROM, qdTable, 
						WHERE + QD_TYPE + EQUALS, 
							quotes(QDatum.DBVALUES[QDatum.SYNTH_OK_SM]), 
						AND, ascii(clobToString(QD_DATA)), 
							NOT_LESS_THAN, '0', 
						AND, ascii(clobToString(QD_DATA)), NOT_MORE_THAN, '9');
				stmt = con.createStatement();
				alwaysPrint(qry);
				rs = stmt.executeQuery(qry);
				qry = toString(
						UPDATE, qdTable, 
						SET + QD_DATA + EQUALS + QMARK
						+ WHERE + QD_DATUMID + EQUALS + QMARK);
				alwaysPrint(qry);
				stmtBatch = con.prepareStatement(qry);
				while (rs.next()) {
					final int dataId = rs.getInt(QD_DATUMID);
					final String data = rs.getString(QD_DATA);
					final String[] dataParts = data.split("/");
					if (MathUtils.isInt(dataParts[0])) { // probably unnecessary
						final int evalNum = MathUtils.parseInt(dataParts[0]);
						dataParts[0] = EVAL_CODES[evalNum];
						alwaysPrint(SELF + "dataId ", dataId, 
								" data set to ", dataParts);
						stmtBatch.setString(1, join(dataParts, "/").toString());
						stmtBatch.setInt(2, dataId);
						stmtBatch.addBatch();
					} // if the evaluator match code is numeric
				} // while there are results
				stmtBatch.executeBatch();
			} // for master and local qdTables
			con.commit();
		} catch (SQLException e) {
			alwaysPrint(SELF + "couldn't update coded data");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			alwaysPrint(SELF + "couldn't update coded data");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(null, stmtBatch, null);
			closeConnection(con, stmt, rs);
		} // try
	} // updateCodedData()

	/** Converts syntheses from old format (reaction ID numbers appended to MRV)
	 * to new format (reaction ID numbers stored as a molecule property). 
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void convertSynthesisFormat() throws DBException {
		final String SELF = "DataConversion.convertSynthesisFormats: ";
		final DBTables masterTables = new DBTables();
		final DBLocalTables localTables = new DBLocalTables();
		final String[] tables = 
				{RESPONSES,
				masterTables.FIGURES, 
				localTables.FIGURES,
				masterTables.EVALUATORS, 
				localTables.EVALUATORS};
		final String[][] fieldsOfTable = {
				{RESP_LASTRESPONSE, RESP_HWID, RESP_QID, RESP_TRIES, RESP_STUDENT},
				{FIG_MAIN_DATA, FIG_ADDL_DATA, FIG_FIGID},
				{FIG_MAIN_DATA, FIG_ADDL_DATA, FIG_FIGID,
					DBLocalTables.Q_AUTHOR},
				{EVAL_MOLSTRUCT, EVAL_QID, EVAL_MAJORID, EVAL_MINORID},
				{EVAL_MOLSTRUCT, EVAL_QID, EVAL_MAJORID, EVAL_MINORID,
					DBLocalTables.Q_AUTHOR}
				};
		// WHERE clause for RESPONSES is complex
		final StringBuilder localQBld = getBuilder(
				SELECT + Q_QID 
				+ FROM, localTables.QUESTIONS, 
				WHERE + Q_TYPE + EQUALS + Question.SYNTHESIS 
				+ AND, DBLocalTables.Q_AUTHOR, IN, parensBuild(
					CourseRW.getCourseInstructorByCourseId(), 
					parens(SELECT + HW_COURSE 
						+ FROM + HWSETS
						+ WHERE + HW_ID + EQUALS + RESPONSES + DOT + RESP_HWID)));
		final StringBuilder masterNotLocalQBld = getBuilder(
				RESP_QID + NOT + IN, parens(localQBld), 
				AND + RESP_QID + IN, parensBuild(
					SELECT + Q_QID + FROM, masterTables.QUESTIONS, 
					WHERE + Q_TYPE + EQUALS + Question.SYNTHESIS));
		final String respCondn = toString(
				RESP_QID + IN, parens(localQBld), 
				OR, parens(masterNotLocalQBld));
		// WHERE clauses for FIGURES and EVALUATORS are simple
		final String figCondn = toString(
				FIG_TYPE + EQUALS, quotes(Figure.DBVALUES[Figure.SYNTHESIS]));
		final String evalCondn = toString(
				EVAL_TYPE + IN, parensJoinQuotes(
					EVAL_CODES[SYNTH_ONE_RXN], 
					EVAL_CODES[SYNTH_SELEC]));
		final String[] conditions = {
				respCondn, figCondn, figCondn, evalCondn, evalCondn};
		final int[] numConversions = new int[tables.length];
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		PreparedStatement stmtUpdate = null;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			for (int tableNum = 0; tableNum < tables.length; tableNum++) {
				final boolean isFigure = Utils.among(tableNum, 1, 2);
				final String table = tables[tableNum];
				final String[] fields = fieldsOfTable[tableNum];
				final int numFields = fields.length;
				final String qry = toString(
						SELECT, join(fields), 
						FROM, table, 
						WHERE, conditions[tableNum]);
				stmt = con.createStatement();
				alwaysPrint(qry);
				rs = stmt.executeQuery(qry);
				int fldNumStart = 1;
				final StringBuilder updateBld = getBuilder(
						UPDATE, table, SET, equalsJoin(fields[0], QMARK));
				if (isFigure) {
					updateBld.append(addEqualsJoin(fields[1], EMPTY));
					fldNumStart = 2;
				} // if is figure
				for (int fldNum = fldNumStart; fldNum < numFields; fldNum++) {
 					appendTo(updateBld, fldNum == fldNumStart 
							? WHERE : AND, fields[fldNum], EQUALS + QMARK);
				} // for each field
				final String update = updateBld.toString();
				alwaysPrint(SELF, update);
				stmtUpdate = con.prepareStatement(update);
				while (rs.next()) {
					final String origSyn = rs.getString(fields[0]);
					String newSyn = origSyn;
					boolean convert = origSyn != null;
					if (convert && isFigure) {
						final String addlData = rs.getString(fields[1]);
						convert = !Utils.isEmpty(addlData);
						if (convert) {
							newSyn = Synthesis.addRxnIds(origSyn, addlData);
						} // if should convert the data
					} else if (convert && !origSyn.trim().endsWith(">")) {
						newSyn = Synthesis.convertSynthesisFormat(origSyn);
						convert = !origSyn.equals(newSyn);
					} // if looking at figure, synthesis has changed
					if (convert) {
						int qMarkNum = 1;
						stmtUpdate.setString(qMarkNum, newSyn);
						for (int fldNum = fldNumStart; fldNum < numFields; 
								fldNum++) {
							qMarkNum++;
							if (tableNum % 2 == 0 && fldNum == numFields - 1) { 
								stmtUpdate.setString(qMarkNum, 
										rs.getString(fields[fldNum]));
							} else {
								stmtUpdate.setInt(qMarkNum, 
										rs.getInt(fields[fldNum]));
							} // if last field in alternate tables
						} // for each field
						stmtUpdate.addBatch();
						numConversions[tableNum]++;
						if (numConversions[tableNum] % 100 == 0) {
							stmtUpdate.executeBatch();
							stmtUpdate.clearBatch();
						} // if time to submit
					} // if there was a substitution
				} // for each entry in the table
				if (numConversions[tableNum] % 100 != 0) {
					stmtUpdate.executeBatch();
				} // if need to submit
				con.commit();
			} // for each table
		} catch (SQLException e) {
			rollbackConnection(con);
			alwaysPrint(SELF + "couldn't set new value for synthesis");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			rollbackConnection(con);
			alwaysPrint(SELF + "couldn't set new value for synthesis");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(null, stmtUpdate, null);
			closeConnection(con, stmt, rs);
		} // try
		for (int tableNum = 0; tableNum < numConversions.length; tableNum++) {
			alwaysPrint(SELF, numConversions[tableNum], 
					(tableNum == 0 ? " synthesis responses"
						: tableNum == 1 ? " master-authored synthesis figures"
						: tableNum == 2 ? " locally authored synthesis figures"
						: tableNum == 3 ? " master-authored synthesis evaluators"
						: " locally authored synthesis evaluators"),
						" modified.");
		} // for each table
	} // convertSynthesisFormat()

	/** Converts Lewis structure from old MOL format to new MRV format. 
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void convertLewisFormat() throws DBException {
		final String SELF = "DataConversion.convertLewisFormat: ";
		final DBTables masterTables = new DBTables();
		final DBLocalTables localTables = new DBLocalTables();
		final String[] tables = 
				{RESPONSES,
				masterTables.FIGURES, 
				localTables.FIGURES,
				masterTables.EVALUATORS, 
				localTables.EVALUATORS};
		final String[][] fieldsOfTable = {
				{RESP_LASTRESPONSE, RESP_HWID, RESP_QID, RESP_TRIES, 
					RESP_STUDENT},
				{FIG_MAIN_DATA, FIG_FIGID},
				{FIG_MAIN_DATA, FIG_FIGID, DBLocalTables.Q_AUTHOR},
				{EVAL_MOLSTRUCT, EVAL_QID, EVAL_MAJORID, EVAL_MINORID},
				{EVAL_MOLSTRUCT, EVAL_QID, EVAL_MAJORID, EVAL_MINORID,
					DBLocalTables.Q_AUTHOR}
				};
		// WHERE clause for RESPONSES is complex
		final StringBuilder localQBld = getBuilder(
				SELECT + Q_QID + FROM, localTables.QUESTIONS, 
				WHERE, Q_TYPE, EQUALS + Question.LEWIS 
				+ AND, DBLocalTables.Q_AUTHOR, IN, parensBuild(
					CourseRW.getCourseInstructorByCourseId(), parens(
						SELECT + HW_COURSE + FROM + HWSETS
						+ WHERE + HW_ID + EQUALS 
							+ RESPONSES + DOT + RESP_HWID)));
		final String respCondn = toString(
				RESP_QID + IN, parens(localQBld), OR, parensBuild(
					RESP_QID + NOT + IN, parens(localQBld), 
					AND + RESP_QID + IN, parensBuild(
						SELECT + Q_QID + FROM, masterTables.QUESTIONS, 
						WHERE, Q_TYPE, EQUALS + Question.LEWIS)));
		// WHERE clauses for FIGURES and EVALUATORS are simple
		final String figCondn = toString(
				FIG_TYPE + EQUALS, quotes(Figure.DBVALUES[Figure.LEWIS]));
		final String evalCondn = toString(
				EVAL_TYPE + EQUALS, quotes(EVAL_CODES[LEWIS_ISOMORPHIC]));
		final String[] conditions = {
				respCondn, figCondn, figCondn, evalCondn, evalCondn};
		final int[] numConversions = new int[tables.length];
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		PreparedStatement stmtUpdate = null;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			for (int tableNum = 0; tableNum < tables.length; tableNum++) {
				final String table = tables[tableNum];
				final String[] fields = fieldsOfTable[tableNum];
				final int numFields = fields.length;
				final String qry = toString(
						SELECT, join(fields), 
						FROM, table, 
						WHERE, conditions[tableNum]);
				alwaysPrint(qry);
				stmt = con.createStatement();
				rs = stmt.executeQuery(qry);
				final StringBuilder updateBld = getBuilder(
						UPDATE, table, SET, equalsJoin(fields[0], QMARK));
				for (int fldNum = 1; fldNum < numFields; fldNum++) {
 					appendTo(updateBld, fldNum == 1 ? WHERE : AND, 
							fields[fldNum], EQUALS + QMARK);
				} // for each field
				final String updateQry = updateBld.toString();
				alwaysPrint(updateQry);
				stmtUpdate = con.prepareStatement(updateQry);
				while (rs.next()) {
					final String origLewis = rs.getString(fields[0]);
					final boolean convert = (origLewis != null
							&& origLewis.indexOf("<?xml") < 0);
					if (convert) {
						final LewisMolecule lewisMol = 
								new LewisMolecule(origLewis);
						final String newLewis = 
								MolString.toString(lewisMol.getMolecule(), MRV);
						stmtUpdate.setString(1, newLewis);
						final StringBuilder bld = getBuilder(
								SELF + "converting table ", table);
						for (int fldNum = 1; fldNum < numFields; fldNum++) {
							if (tableNum % 2 == 0 && fldNum == numFields - 1) {
								final String datum = 
										rs.getString(fields[fldNum]);
								stmtUpdate.setString(fldNum + 1, datum);
 								appendTo(bld, ", ", fields[fldNum], ' ', datum);
							} else {
								final int datum = rs.getInt(fields[fldNum]);
								stmtUpdate.setInt(fldNum + 1, datum);
 								appendTo(bld, ", ", fields[fldNum], ' ', datum);
							} // last field of alternate tables
						} // for each field
						alwaysPrint(bld.toString());
						stmtUpdate.addBatch();
						numConversions[tableNum]++;
						if (numConversions[tableNum] % 100 == 0) {
							stmtUpdate.executeBatch();
							stmtUpdate.clearBatch();
						} // if time to submit
					} // if there was a substitution
				} // for each entry in the table
				if (numConversions[tableNum] % 100 != 0) {
					stmtUpdate.executeBatch();
				} // if need to submit
			} // for each table
			con.commit();
		} catch (SQLException e) {
			rollbackConnection(con);
			alwaysPrint(SELF + "couldn't set new value for Lewis structure");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			rollbackConnection(con);
			alwaysPrint(SELF + "couldn't set new value for Lewis structure");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(null, stmtUpdate, null);
			closeConnection(con, stmt, rs);
		} // try
		for (int tableNum = 0; tableNum < numConversions.length; tableNum++) {
			alwaysPrint(SELF, numConversions[tableNum], 
					(tableNum == 0 ? " Lewis structure responses"
						: tableNum == 1 ? " master-authored Lewis structure figures"
						: tableNum == 2 ? " locally authored Lewis structure figures"
						: tableNum == 3 ? " master-authored Lewis structure evaluators"
						: " locally authored Lewis structure evaluators"),
						" modified.");
		} // for each table
	} // convertLewisFormat()

	/** Populates a new table with questions definitions of the assignments.
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void makeNewHWQsTable() throws DBException {
		final String SELF = "DataConversion.makeNewHWQsTable: ";
		Connection con = null;
		PreparedStatement pstmt = null;
		Statement stmt = null;
		ResultSet rs = null;
		int numRowsInserted = 0;
		int hwNum = 0;
		int grpNum = 0;
		final String HW_QSTRING = "qlist";
		try {
			con = getPoolConnection();
			final String qry = toString(
					SELECT, joinAll(
						HW_ID,
						HW_QSTRING), 
					FROM + HWSETS);
			debugPrint(SELF, qry);
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			final List<Integer> hwIds = new ArrayList<Integer>();
			final List<String> hwQLists = new ArrayList<String>();
			while (rs.next()) {
				hwIds.add(Integer.valueOf(rs.getInt(HW_ID)));
				hwQLists.add(rs.getString(HW_QSTRING));
			} // while there are results
			closeStmtAndRs(stmt, rs);
			final List<String> fields = Arrays.asList(
					HWQS_HWID,
					HWQS_GRP_NUM,
					HWQS_GRP_PICK,
					HWQS_BUNDLE_SIZE,
					HWQS_QNUM,
					HWQS_QID);
			final String insert = getInsertIntoValuesQMarksSQL(HW_QS, fields);
			pstmt = con.prepareStatement(insert);
			con.setAutoCommit(false);
			for (final String hwQList : hwQLists) {
				final int hwId = hwIds.get(hwNum).intValue();
				final Assgt assgt = new Assgt(hwId, hwQList);
				final List<AssgtQGroup> qGrps = assgt.getQGroups();
				hwNum++;
				grpNum = 0;
				for (final AssgtQGroup qGrp : qGrps) {
					grpNum++;
					final int pick = qGrp.getPick();
					final int bundleSize = qGrp.getBundleSize();
					final List<Integer> grpQIds = qGrp.getQIds();
					int qNumInQGrp = 0;
					for (final Integer grpQId : grpQIds) {
						debugPrint(SELF + "Record ", numRowsInserted + 1, ": ",
								hwId, ", ", grpNum, ", ", pick, ", ", 
								bundleSize, ", ", qNumInQGrp + 1, ", ", grpQId);
						setValues(pstmt,
								hwId,
								grpNum,
								pick,
								bundleSize,
								++qNumInQGrp,
								grpQId);
						pstmt.addBatch();
						numRowsInserted++;
						if (numRowsInserted % 100 == 0) {
							pstmt.executeBatch();
							pstmt.clearBatch();
						} // if time to submit
					} // for each qId
				} // for each group of questions in the assignment
			} // for each assigned list of questions
			pstmt.executeBatch();
			con.commit();
		} catch (SQLException e) {
			alwaysPrint(SELF + BATCH_FAILED_HWSET, hwNum, ", with ", 
					grpNum, " groups, on last row.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			alwaysPrint(SELF + BATCH_FAILED_HWSET, hwNum, ", with ", 
					grpNum, " groups, on last row.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
			closeConnection(null, pstmt, null);
		}
	} // makeNewHWQsTable()

	/** Populates a new table with question dependencies of the assignments.
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void makeNewQDependenciesTable() throws DBException {
		final String SELF = "DataConversion.makeNewQDependenciesTable: ";
		Connection con = null;
		PreparedStatement pstmt = null;
		Statement stmt = null;
		ResultSet rs = null;
		int numRowsInserted = 0;
		int hwNum = 0;
		final String Q_DEPENDENCIES = "hwset_dependencies_v1"; 
		final String QDEP_HWID = "hw_id";
		final String QDEP_DEPENDENT = "dependent_pb_id";
		final String QDEP_INDEPENDENT = "independent_pb_id";
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			final Map<Integer, String> qDependStrs = 
					new HashMap<Integer, String>();
			final String HW_DEPENDS = "dependencies"; // obsolete field
			final String qry = toString(
					SELECT, joinAll(
						HW_ID,
						HW_DEPENDS), 
					FROM + HWSETS + WHERE, length(HW_DEPENDS), IS_NOT_ZERO);
			debugPrint(SELF, qry);
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			while (rs.next()) {
				final int hwId = rs.getInt(HW_ID);
				final String params = rs.getString(HW_DEPENDS);
				qDependStrs.put(Integer.valueOf(hwId), params);
			} // while there are results
			closeStmtAndRs(stmt, rs);
			final List<String> fields = Arrays.asList(
					QDEP_HWID,
					QDEP_DEPENDENT,
					QDEP_INDEPENDENT);
			final String insert = 
					getInsertIntoValuesQMarksSQL(Q_DEPENDENCIES, fields);
			debugPrint(SELF, insert);
			pstmt = con.prepareStatement(insert);
			final List<Integer> hwIds = 
					new ArrayList<Integer>(qDependStrs.keySet());
			for (final Integer hwIdObj : hwIds) {
				hwNum++;
				final String dependStr = qDependStrs.get(hwIdObj);
				final int hwId = hwIdObj.intValue();
				final String[] dependPairs = dependStr.split(";");
				for (final String dependPair : dependPairs) {
					final String[] qPairStrs = dependPair.split(":");
					final int[] qPair = Utils.stringToIntArray(qPairStrs);
					debugPrint(SELF + "Record ", numRowsInserted + 1, ": ",
							hwId, ", ", qPair);
					setValues(pstmt,
							hwId,
							qPair[0],
							qPair[1]);
					pstmt.addBatch();
					numRowsInserted++;
					if (numRowsInserted % 100 == 0) {
						pstmt.executeBatch();
						pstmt.clearBatch();
					} // if time to submit
				} // for each dependency
			} // for each assignment with question dependencies
			pstmt.executeBatch();
			con.commit();
		} catch (SQLException e) {
			alwaysPrint(SELF + BATCH_FAILED_HWSET, hwNum, " on last row.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			alwaysPrint(SELF + BATCH_FAILED_HWSET, hwNum, " on last row.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
			closeConnection(null, pstmt, null);
		}
	} // makeNewQDependenciesTable()

	/** Converts old, linear (nested) expressions for combining expressions of 
	 * permissible starting materials into same postfix notation as used by
	 * complex evaluators.
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void convertSynthSMExprs() throws DBException {
		final String SELF = "DataConversion.convertSynthSMExprs: ";
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		final int exprNum = 0;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			final DBTables masterTables = new DBTables();
			final DBLocalTables localTables = new DBLocalTables();
			final String[] tables = 
					{masterTables.QUESTIONDATA, localTables.QUESTIONDATA};
			final String SM_EXPR = quotes(QDatum.DBVALUES[QDatum.SM_EXPR]);
			final int NESTED = 0;
			final int POSTFIX = 1;
			final char OLD_AND = '&';
			// final char BLANK = ' '; // unused 11/6/2012
			for (final String table : tables) {
				String qry = toString(
						UPDATE, table, 
						SET + QD_DATA + EQUALS, translate(
							regexp_replace(
								regexp_replace(
									regexp_replace(QD_DATA, "[()]: :"), 
									"[px-]:"), 
								"[ :]"),
							OLD_AND, CombineExpr.AND), 
						WHERE + QD_TYPE + EQUALS, SM_EXPR);
				debugPrint(SELF, qry);
				tryUpdate(con, qry);
				qry = toString(
						SELECT_UNIQUE, clobToString(QD_DATA), AS + QD_DATA 
						+ FROM, table, 
						WHERE + QD_TYPE + EQUALS, SM_EXPR);
				final List<String[]> combExprs = new ArrayList<String[]>();
				debugPrint(SELF, qry);
				stmt = con.createStatement();
				rs = stmt.executeQuery(qry);
				while (rs.next()) {
					final String[] pair = new String[2];
					pair[NESTED] = rs.getString(QD_DATA);
					final List<String> nestedList = 
							CombineExpr.getNestedList(pair[NESTED]);
					// convert 0-based to 1-based, & to @
					int chNum = 0;
					for (final String token : nestedList) {
						if (Utils.isDigit(token.charAt(0))) {
							nestedList.set(chNum, 
									String.valueOf(MathUtils.parseInt(token) + 1));
						} // if token is a number
						chNum++;
					} // for each token
					pair[POSTFIX] = 
							CombineExpr.nestedToPostfix(nestedList).toString();
					debugPrint(SELF + "converted expression ", pair[NESTED], 
							" to ", pair[POSTFIX]);
					combExprs.add(pair);
				} // while there are results
				for (final String[] pair : combExprs) {
					qry = toString(
							UPDATE, table, 
							SET + QD_DATA + EQUALS + QMARK
							+ WHERE + QD_DATA + LIKE, quotes(pair[NESTED]), 
							AND + QD_TYPE + EQUALS, SM_EXPR);
					debugPrint(SELF, qry);
					tryUpdate(con, qry, pair[POSTFIX]);
				} // for each record to update
			} // for each table
			con.commit();
		} catch (SQLException e) {
			alwaysPrint(SELF, exprNum, " update statements failed.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			alwaysPrint(SELF, exprNum, " update statements failed.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
	} // convertSynthSMExprs()

	/** Converts /;:-separated expressions for coordinates into XML.
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void convertClickHereCoordsToXML() throws DBException {
		final String SELF = "DataConversion.convertClickHereCoordsToXML: ";
		Connection con = null;
		Statement stmt = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		final DBTables masterTables = new DBTables();
		final DBLocalTables localTables = new DBLocalTables();
		final String[] tables = 
				{masterTables.EVALUATORS, 
				localTables.EVALUATORS};
		final String[][] fieldsOfTable = {
				{EVAL_MOLSTRUCT, EVAL_QID, EVAL_MAJORID, EVAL_MINORID},
				{EVAL_MOLSTRUCT, EVAL_QID, EVAL_MAJORID, EVAL_MINORID,
					DBLocalTables.Q_AUTHOR}
				};
		final String SHAPES_SEP = "/";
		final String SHAPE_SEP = ";";
		final String XY_SEP = ":";
		final String NONE_OF_SHAPE = "none";
		int exprNum = 0;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			for (int tableNum = 0; tableNum < tables.length; tableNum++) {
				final String table = tables[tableNum];
				final String[] fields = fieldsOfTable[tableNum];
				String qry = toString(
						SELECT, join(fields), 
						FROM, table, 
						WHERE + EVAL_TYPE + EQUALS, 
							quotes(EVAL_CODES[CLICK_HERE]), 
						AND + EVAL_MOLSTRUCT + NOT + LIKE, quotes("<%"));
				debugPrint(SELF, qry);
				stmt = con.createStatement();
				rs = stmt.executeQuery(qry);
				final List<String[]> results = new ArrayList<String[]>();
				while (rs.next()) {
					final List<String> result = new ArrayList<String>();
					final String shapesStr = rs.getString(EVAL_MOLSTRUCT);
					final StringBuilder xmlBld = 
							getBuilder(XMLUtils.startTag(XML_TAG));
					final String[] shapeStrs = shapesStr.split(SHAPES_SEP);
					final int numShapes = shapeStrs.length;
					for (int shapeNum = 0; shapeNum < numShapes; shapeNum++) {
						final String regionsStr = shapeStrs[shapeNum];
						if (!NONE_OF_SHAPE.equals(regionsStr)) {
							final String[] regions = 
									regionsStr.split(SHAPE_SEP);
							for (final String region : regions) {
								final String[] coordStrs = region.split(XY_SEP);
								xmlBld.append(XMLUtils.startAndCloseTag(
									ClickHere.SHAPE_TAGS[shapeNum],
									new String[] {X_TAG, coordStrs[X]},
									new String[] {Y_TAG, coordStrs[ClickConstants.Y]},
									new String[] {WIDTH_TAG, coordStrs[WIDTH]},
									new String[] {HEIGHT_TAG, coordStrs[HEIGHT]}
								));
							} // for each region
						} // if there are regions of this shape
					} // for each shape
					xmlBld.append(XMLUtils.endTag(XML_TAG));
					result.add(xmlBld.toString());
					result.add(String.valueOf(rs.getInt(EVAL_QID)));
					result.add(String.valueOf(rs.getInt(EVAL_MAJORID)));
					result.add(String.valueOf(rs.getInt(EVAL_MINORID)));
					if (tableNum == 1) {
						result.add(rs.getString(DBLocalTables.Q_AUTHOR));
					} // if local table
					results.add(result.toArray(new String[result.size()]));
				} // while there are results
				closeStmtAndRs(stmt, rs);
				debugPrint(SELF, results);
				qry = toString(UPDATE, table, 
						SET + EVAL_MOLSTRUCT + EQUALS + QMARK
						+ WHERE + EVAL_QID + EQUALS + QMARK
						+ AND + EVAL_MAJORID + EQUALS + QMARK
						+ AND + EVAL_MINORID + EQUALS + QMARK,
						tableNum != 1 ? "" : getBuilder(
							AND, DBLocalTables.Q_AUTHOR, EQUALS + QMARK));
				debugPrint(SELF, qry);
				pstmt = con.prepareStatement(qry);
				int numRowsInserted = 0;
				for (final String[] result : results) {
					pstmt.setString(1, result[0]);
					pstmt.setInt(2, MathUtils.parseInt(result[1]));
					pstmt.setInt(3, MathUtils.parseInt(result[2]));
					pstmt.setInt(4, MathUtils.parseInt(result[3]));
					if (tableNum == 1) {
						pstmt.setString(5, result[4]);
					} // if local table
					pstmt.addBatch();
					numRowsInserted++;
					exprNum++;
					if (numRowsInserted % 100 == 0) {
						pstmt.executeBatch();
						pstmt.clearBatch();
					} // if time to submit
				} // for each record to update
				if (numRowsInserted % 100 != 0) {
					pstmt.executeBatch();
				} // if need to submit
				closeConnection(null, pstmt, null);
			} // for each table
			con.commit();
			debugPrint(SELF, exprNum, " record(s) converted to XML.");
		} catch (SQLException e) {
			alwaysPrint(SELF, exprNum, " update statements failed.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			alwaysPrint(SELF, exprNum, " update statements failed.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(null, pstmt, null);
			closeConnection(con, stmt, rs);
		}
	} // convertClickHereCoordsToXML()

	/** Writes all image figures to the figures directories.
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void writeImageFiles() throws DBException {
		final String SELF = "DataConversion.writeImageFiles: ";
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		final int exprNum = 0;
		try {
			con = getPoolConnection();
			final DBTables masterTables = new DBTables();
			final DBLocalTables localTables = new DBLocalTables();
			final String Q_AUTHOR = DBLocalTables.Q_AUTHOR;
			final DBTables[] tables = {masterTables, localTables};
			for (final DBTables table : tables) {
				final String qry = toString(
						SELECT, joinAll(
							Q_QID,
							IMG_ID), 
						FROM, table.QUESTIONS, JOIN, table.FIGURES, 
							ON + Q_QID + EQUALS + FIG_QID,
						!table.local ? "" : getBuilder(
 							AND, table.QUESTIONS, DOT + Q_AUTHOR 
								+ EQUALS, table.FIGURES, DOT + Q_AUTHOR),
 						JOIN, table.OLD_IMAGES, 
							ON + FIG_FIGID + EQUALS + IMG_ID, 
						WHERE + FIG_TYPE + EQUALS, 
							quotes(Figure.DBVALUES[Figure.IMAGE]), 
						ORDER_BY, joinAll(
							Q_QID,
							IMG_ID));
				debugPrint(SELF, qry);
				stmt = con.createStatement();
				rs = stmt.executeQuery(qry);
				final String absFiguresDir = 
						ImageRW.getAbsFigsDirBld(table.local).toString();
				final Map<Integer, String> filenamesByPicIds = 
						new HashMap<Integer, String>();
				while (rs.next()) {
					final int qId = rs.getInt(Q_QID);
					final int imgId = rs.getInt(IMG_ID);
					final String imgfile = 
							toString(absFiguresDir, qId, '_', imgId);
					filenamesByPicIds.put(Integer.valueOf(imgId), imgfile);
					debugPrint(SELF + "image ", imgId, " from ",
							table.OLD_IMAGES, " of Q", qId, " from ",
							table.QUESTIONS, " will be written to ", imgfile);
				} // while there are results
				closeStmtAndRs(stmt, rs);
				final List<Integer> picIds = 
						new ArrayList<Integer>(filenamesByPicIds.keySet());
				for (final Integer picId : picIds) {
					final String fullName = filenamesByPicIds.get(picId);
					extractImage(con, table.local, picId.intValue(), fullName);
				} // for each image to write to disk
			} // for each table
			String qry = toString(
					SELECT, joinAll(
						POST_ID,
						IMG_ID), 
					FROM, POSTS, JOIN, masterTables.OLD_IMAGES, 
					ON, toNumber(POST_FIGURE), EQUALS + IMG_ID
					+ WHERE + POST_FIGTYPE + EQUALS, 
						quotes(ForumPost.DBVALUES[ForumPost.IMAGE]), 
					ORDER_BY, joinAll(
						POST_ID,
						IMG_ID));
			debugPrint(SELF, qry);
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			String absFiguresDir = ImageRW.getAbsFigsDirBld(!LOCAL).toString();
			final Map<Integer, String> filenamesByPicIds = 
					new HashMap<Integer, String>();
			while (rs.next()) {
				final int postId = rs.getInt(POST_ID);
				final int imgId = rs.getInt(IMG_ID);
				final String imgfile = toString(absFiguresDir, 
						POST_FILENAME, postId, '_', imgId);
				filenamesByPicIds.put(Integer.valueOf(imgId), imgfile);
				debugPrint(SELF + "image ", imgId, " from ",
						masterTables.OLD_IMAGES, " of post ", postId, 
						" from ", POSTS, " will be written to ", imgfile);
			} // while there are results
			List<Integer> picIds = 
					new ArrayList<Integer>(filenamesByPicIds.keySet());
			for (final Integer picId : picIds) {
				final String fullName = filenamesByPicIds.get(picId);
				extractImage(con, masterTables.local, picId.intValue(), 
						fullName);
			} // for each image to write to disk
			closeStmtAndRs(stmt, rs);
			filenamesByPicIds.clear();
			qry = toString(
					SELECT, joinAll(
						TEXTCONTENT_ID,
						IMG_ID), 
					FROM, TEXTCONTENT, JOIN, localTables.OLD_IMAGES, 
					ON, toNumber(TEXTCONTENT_DATA), EQUALS + IMG_ID
					+ WHERE + TEXTCONTENT_TYPE + EQUALS, 
						quotes(TextContent.DB_VALUES[TextContent.IMAGE]), 
					ORDER_BY, joinAll(
						TEXTCONTENT_ID,
						IMG_ID));
			debugPrint(SELF, qry);
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			absFiguresDir = ImageRW.getAbsFigsDirBld(LOCAL).toString();
			while (rs.next()) {
				final int contentId = rs.getInt(TEXTCONTENT_ID);
				final int imgId = rs.getInt(IMG_ID);
				final String imgfile = toString(absFiguresDir, 
						TextContent.CONTENT_FILENAME, imgId);
				filenamesByPicIds.put(Integer.valueOf(imgId),
						imgfile);
				debugPrint(SELF + "image ", imgId, " from ",
						masterTables.OLD_IMAGES, " of textcontent ", 
						contentId, " from ", TEXTCONTENT, 
						" will be written to ", imgfile);
			} // while there are results
			picIds = new ArrayList<Integer>(filenamesByPicIds.keySet());
			for (final Integer picId : picIds) {
				final String fullName = filenamesByPicIds.get(picId);
				extractImage(con, localTables.local, picId.intValue(), 
						fullName);
			} // for each image to write to disk
		} catch (SQLException e) {
			alwaysPrint(SELF, exprNum, " update statements failed.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			alwaysPrint(SELF, exprNum, " update statements failed.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
	} // writeImageFiles()

	/** Gets the image from the database, writes it to a file.
	 * @param	con	database connection
	 * @param	isLocal	true when should use local tables
	 * @param	imageId	unique ID of this image
	 * @param	filepath	file to contain the image, minus the extension
	 * @return	extension of the image
	 * @throws	SQLException	if there's a problem reading the database
	 */
	private static String extractImage(Connection con, boolean isLocal, 
			int imageId, String filepath) throws SQLException {
		final String SELF = "DataConversion.extractImage: ";
		final DBTables tables = getTables(isLocal);
		String extension = null;
		final String qry = toString(
				SELECT + IMG_TYPE 
				+ FROM, tables.OLD_IMAGES, 
				WHERE + IMG_ID + EQUALS, imageId);
		debugPrint(SELF, qry);
		Statement stmt = null;
		ResultSet rs = null;
		try {  
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry) ;
			if (rs.next()) extension = rs.getString(IMG_TYPE);
		} finally {
			closeStmtAndRs(stmt, rs);
		}
		if (extension != null) try {  
			final String fullpath = toString(filepath, DOT, extension);
			try {
				final File file = new File(fullpath);
				if (file.exists()) {
					debugPrint(SELF, fullpath, 
							" already exists, not rewriting image");
					return extension;
				} // if file exists already
			} catch (Exception e) {
				alwaysPrint(SELF + "cannot tell if ",
						fullpath, " exists or not");
			}
			final Blob blob = getImageBlob(con, tables, imageId);
			debugPrint(SELF + "got image from table, writing to ", fullpath);
			final FileOutputStream outFile =
					new FileOutputStream(new File(fullpath));
			final InputStream instream = blob.getBinaryStream();
			final byte[] buffer = new byte[100];
			int length;
			// fetch data
			while ((length = instream.read(buffer)) != -1) {
				outFile.write(buffer, 0, length);
			}
			// close input and output streams
			instream.close();
			outFile.close();
			debugPrint(SELF + "constructed ", fullpath);
		} catch (IOException e) {
			alwaysPrint("ERROR: " + SELF + " cannot write to file ");
			e.printStackTrace();
			extension = null;
		} catch (ClassCastException e) {
			alwaysPrint("ERROR: " + SELF + "cast exception: ",
					e.getMessage(),
					"\n\tMake sure epoch-plugin/lib does not duplicate jar files " +
					"\n\tin CATALINA_HOME/common/lib");
			extension = null;
		} // try
		return extension;
	} // extractImage(Connection, boolean, int, String)

	/** Gets the image from the database as a Blob.
	 * @param	con	database connection
	 * @param	tables	database tables
	 * @param	imageId	unique ID of this image
	 * @return	the image
	 * @throws	SQLException	if there's a problem reading the database
	 */
	private static Blob getImageBlob(Connection con, DBTables tables, 
			int imageId) throws SQLException {
		final String SELF = "ImageRW.getImageBlob: ";
		Blob blob = null;
		final String qry = toString(
				SELECT + IMG_DATA 
				+ FROM, tables.OLD_IMAGES, 
				WHERE + IMG_ID + EQUALS, imageId);
		debugPrint(SELF, qry);
		Statement stmt = null;
		ResultSet rs = null;
		try {  
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry) ;
			if (rs.next()) blob = rs.getBlob(IMG_DATA);
		} finally {
			closeStmtAndRs(stmt, rs);
		}
		return blob;
	} // getImageBlob(Connection, int)

	/** Adds points per question to the assignment questions table. */
	public static void convertHWQs() {
		final String SELF = "DataConversion.convertHWQs: ";
		final String qry = 
				UPDATE + HW_QS
				+ SET + HWQS_PTS + EQUALS + QMARK + COMMA 
					+ HWQS_PTS_STR + EQUALS + QMARK
				+ WHERE + HWQS_HWID + EQUALS + QMARK
				+ AND + HWQS_GRP_NUM + EQUALS + QMARK;
		alwaysPrint(SELF + qry);
		Connection con = null;
		PreparedStatement batchStmt = null;
		int numBatches = 0;
		try {
			final List<Assgt> allHWs = HWRead.getHWs(new SQLWithQMarks());
			alwaysPrint(SELF, allHWs.size(), " assignments found.");
			con = getPoolConnection();
			batchStmt = con.prepareStatement(qry);
			con.setAutoCommit(false);
			for (final Assgt hw : allHWs) {
				final List<AssgtQGroup> qGrps = hw.getQGroups();
				alwaysPrint(SELF + "assignment ", hw.id, " has ",
						qGrps.size(), " question group(s).");
				int grpNum = 0;
				for (final AssgtQGroup qGrp : qGrps) {
					grpNum++;
					final String ptsStr = qGrp.getPts();
					alwaysPrint(SELF + "group ", grpNum, 
							" has ", qGrp.getNumQs(), " total Q(s) ",
							" and ", qGrp.getNumQsSeen(), 
							" seen Q(s), each worth ", ptsStr);
					final double pts = MathUtils.parseDouble(ptsStr, 1);
					batchStmt.setDouble(1, pts);
					batchStmt.setString(2, ptsStr);
					batchStmt.setInt(3, hw.id);
					batchStmt.setInt(4, grpNum);
					batchStmt.addBatch();
					numBatches++;
					if (numBatches % 100 == 0) {
						batchStmt.executeBatch();
						batchStmt.clearBatch();
					} // if time to submit
				} // for each question group
			} // for each assignment
			if (numBatches % 100 != 0) {
				batchStmt.executeBatch();
			} // if need to submit
			con.commit();
			alwaysPrint(SELF, numBatches, " queries executed.");
		} catch (SQLException e) {
			alwaysPrint(SELF + "caught exception writing new data to table "
					+ "at batch ", numBatches);
			e.printStackTrace();
		} catch (DBException e) {
			alwaysPrint(SELF + "caught exception reading assignments.");
			e.printStackTrace();
		} finally {
			closeConnection(con, batchStmt, null);
		} // try
	} // convertHWQs()

	/** Splits into individual fields:
	 * <ul><li>
	 * for complete-the-table questions,
	 * |-separated strings representing row and column captions 
	 * </li><li>
	 * for orbital energy diagram questions,
	 * tab-separated strings representing 3 column captions and pulldown menu labels
	 * </li><li>
	 * for reaction coordinate diagram questions,
	 * tab-separated strings representing pulldown menu labels 
	 * </li><li>
	 * for orbital energy and reaction coordinate diagram questions,
	 * tab-separated strings representing y-axis scale information
	 * </li></ul>
	 * numbering the individual fields in the order in which they appear in 
	 * the strings.
	 * <p>Serial numbers of each entry need to be set to -1 or -2 before this
	 * method is run. For tables, rows and column captions should start 
	 * in two separate entries with the
	 * type field set to R or C; for each energy diagram question, there is one
	 * entry with the type field set to C, and maybe another entry
	 * with type field set to Y.
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void splitCaptions() throws DBException {
		final String SELF = "DataConversion.splitCaptions: ";
		Connection con = null;
		Statement stmt = null;
		PreparedStatement batchStmt = null;
		ResultSet rs = null;
		final int[] qTypes = new int[] {TABLE, ORB_E_DIAGRAM, RXN_COORD};
		final String COLUMN = CAPTS_TYPE_DBVALUES[TableQ.COL_DATA];
		final String LABEL = CAPTS_TYPE_DBVALUES[LABEL_DATA];
		int numInserts = 0;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			final DBTables masterTables = new DBTables();
			final DBLocalTables localTables = new DBLocalTables();
			final String[] captionsTables = 
					{masterTables.CAPTIONS, localTables.CAPTIONS};
			final String[] questionsTables = 
					{masterTables.QUESTIONS, localTables.QUESTIONS};
			final String Q_AUTHOR = DBLocalTables.Q_AUTHOR;
			for (final int qType : qTypes) {
				final boolean isTable = qType == TABLE;
				final boolean isOED = qType == ORB_E_DIAGRAM;
				boolean local = false;
				for (final String captionsTable : captionsTables) {
					final String questionsTable = questionsTables[
							Utils.indexOf(captionsTables, captionsTable)];
					final String qry = toString(
							SELECT + ALL + FROM, captionsTable,
							WHERE + CAPTS_QID + IN, parensBuild(
								SELECT + Q_QID + FROM, questionsTable,
								WHERE + Q_TYPE + EQUALS, 
									quotes(DB_QTYPES[qType])),
							AND + CAPTS_NUM + LESS_THAN + "0"
							+ ORDER_BY, joinAll(
								CAPTS_QID,
								CAPTS_TYPE));
					alwaysPrint(SELF, qry);
					stmt = con.createStatement();
					rs = stmt.executeQuery(qry);
					final List<String> fields = new ArrayList<String>(
							Arrays.asList(
								CAPTS_QID,
								CAPTS_TYPE,
								CAPTS_NUM,
								CAPTS_TEXT));
					if (local) fields.add(Q_AUTHOR);
					final String insertQry = 
							getInsertIntoValuesQMarksSQL(captionsTable, fields);
					alwaysPrint(SELF, insertQry);
					batchStmt = con.prepareStatement(insertQry);
					while (rs.next()) {
						final int qId = rs.getInt(CAPTS_QID);
						final String joinedCaptions = rs.getString(CAPTS_TEXT);
						if (!Utils.isEmptyOrWhitespace(joinedCaptions)) {
							final String[] splitCaptions = 
									joinedCaptions.split(isTable
										? TableQ.CAPTION_SEP_REGEX : QDATA_SEP);
							String captionType = rs.getString(CAPTS_TYPE);
							if (isOED && LABEL.equals(captionType)) {
								captionType = COLUMN;
							} // if OED captions/labels entry
							int captionNum = (isTable 
									&& COLUMN.equals(captionType) ? 0 : 1);
							final String qAuthor = 
									(local ? rs.getString(Q_AUTHOR) : "");
							alwaysPrint(SELF + "inserting ", 
									splitCaptions.length, " captions/labels"
									+ "/y-axis data for question ", qId, 
									" of question type ", DB_QTYPES[qType],
									local ? toString(" by author ", qAuthor) 
										: "",
									" and initial caption type ", captionType,
									" into table ", captionsTable,
									"; starting caption numbers at ",
									captionNum);
							for (final String caption : splitCaptions) {
								setValues(batchStmt,
										qId,
										captionType,
										captionNum,
										caption.trim());
								if (local) {
									setValue(batchStmt, fields.size(),
											qAuthor);
								} // if local
								alwaysPrint(SELF + "adding data {",
										qId, ", ", captionType, ", ",
										captionNum, ", ", caption, local 
										? toString(", ", qAuthor) : "", '}');
								batchStmt.addBatch();
								numInserts++;
								if (numInserts % 100 == 0) {
									batchStmt.executeBatch();
									batchStmt.clearBatch();
								} // if 100 inserts
								if (isOED && captionNum == 3 
										&& COLUMN.equals(captionType)) {
									alwaysPrint(SELF 
											+ "changing caption type from ", 
											captionType, " to ", LABEL,
											" and restarting caption "
											+ "numbering at 1");
									captionType = LABEL;
									captionNum = 1;
								} else captionNum++;
							} // for each caption
						} // if there are captions
					} // while there are results
					if (numInserts % 100 != 0) {
						batchStmt.executeBatch();
					} // if need to submit
					closeStmtAndRs(stmt, rs);
					closeConnection(null, batchStmt, null);
					local = !local;
				} // for each table
			} // for each energy diagram type
			con.commit();
			alwaysPrint(SELF, numInserts, " inserts executed.");
		} catch (SQLException e) {
			rollbackConnection(con);
			alwaysPrint(SELF, " insert statements failed.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			rollbackConnection(con);
			alwaysPrint(SELF, " insert statements failed.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(null, batchStmt, null);
			closeConnection(con, stmt, rs);
		}
	} // splitCaptions()

/* ******** Methods for interconverting between BLOBs and strings **********/

	/** Reads a BLOB or CLOB from the result of a database call into a String.
	 * @param	rs	result of a database call
	 * @param	field	which field of the result set contains the BLOB or CLOB
	 * @return	string containing the value of the BLOB or CLOB
	 */ 
	private static String lobToString(ResultSet rs, String field) {
		final String SELF = "DataConversion.lobToString: ";
		final String CHAR_SET = "UTF-8";
		String data = "";
		try {
			final byte[] bytes = DatabaseTools.readBytes(rs, field);
			if (!isEmpty(bytes)) {
				data = decompressIfLewis(new String(bytes, CHAR_SET));
				debugPrint(SELF + "returning:\n", data);
			} // if there was something to read
		} catch (Exception ex) {
			alwaysPrint(SELF + "Caught exception reading LOB.");
			ex.printStackTrace();
		} // try
		return data;
	} // lobToString(ResultSet, String)

	/** Stores a string in the database as a BLOB.  Note: The database field
	 * must be readied before this method is called by setting it to EMPTY_BLOB(), 
	 * because a shorter string will not overwrite a longer string.
	 * @param	con	database connection
	 * @param	query	retrieves the BLOB field
	 * @param	dataIn	data to store in the BLOB
	 * @throws	SQLException	if there are no results from the query
	 * @throws	IOException	if there are problems opening the BLOB
	 */ 
	private static void stringIntoBlob(Connection con, String query, 
			String dataIn) throws SQLException, IOException {
		final String SELF = "DataConversion.stringIntoBlob: ";
		final String data = (dataIn == null ? "" : dataIn);
		Statement stmt = null;
		ResultSet rs = null;
		OutputStream dataOut = null;
		try {
			debugPrint(SELF, query);
			stmt = con.createStatement();
			rs = stmt.executeQuery(query);
			if (!rs.next()) {
				throw new SQLException(SELF + "no results from query " + query);
			}
			final Blob blob = rs.getBlob(1);
			dataOut = blob.setBinaryStream(BLOB_BEGIN);
			debugPrint(SELF + "writing data:\n", data);
			dataOut.write(data.getBytes(StandardCharsets.UTF_8));
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (dataOut != null) dataOut.close();
			closeStmtAndRs(stmt, rs);
		}
	} // stringIntoBlob(Connection, String, String)

/* ******** Miscellaneous methods **********/

	/** Subjects three strings to replace().
	 * @param	field	field containing the values to be searched
	 * @param	srchStr	string for which to search
	 * @param	replaceStr	replacement string
	 * @return	the three strings enclosed in replace(), the latter two in
	 * quotes
	 */
	private static String replace(String field, String srchStr, 
			String replaceStr) { 
		final StringBuilder bld = 
				joinAll(field, quotes(srchStr), quotes(replaceStr));
		return fn("REPLACE", bld).toString(); 
	} // replace(String, String, String)

	/** Surrounds a number with slash marks.
	 * @param	num	a number
	 * @return	the number surrounded by slash marks
	 */
	private static String slash(int num) {
		return toString('/', num, '/');
	} // slash(int)

	/** Surrounds a string with slash marks.
	 * @param	str	a string
	 * @return	the string surrounded by slash marks
	 */
	private static String slash(String str) {
		return toString('/', str, '/');
	} // slash(String)

	/** Subjects a string to ASCII().
	 * @param	bld	a string in a StringBuilder
	 * @return	the string surrounded by ASCII() in a StringBuilder
	 */
	private static StringBuilder ascii(StringBuilder bld) {
		return fn("ASCII", bld);
	} // ascii(StringBuilder)

	/** Constructor. */
	private DataConversion() {
		// empty constructor; this class is not instantiable
	}

} // DataConversion
