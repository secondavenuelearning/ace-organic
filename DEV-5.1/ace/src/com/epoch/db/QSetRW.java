package com.epoch.db;

import static com.epoch.db.dbConstants.QuestionsRWConstants.*;
import com.epoch.constants.AuthorConstants;
import com.epoch.exceptions.DBException;
import com.epoch.qBank.QSetDescr;
import com.epoch.qBank.Topic;
import com.epoch.utils.Utils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.ArrayUtils;

/** All the read operations for topics and question sets. */
public final class QSetRW extends DBCommon 
		implements AuthorConstants {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

	/** Parameter for setQSetDescr(). */
	public static final boolean DELETE_TRANSLNS = true;

/* **************** Methods regarding topics ********************/

	/** Gets all topics in the master question bank.
	 * @return	a list of topic names
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<Topic> getTopics() throws DBException {
		return getTopics(MASTER_AUTHOR);
	} // getTopics()
	
	/** Gets all topics written by a local author.
	 * @param	instructorId	instructor login ID
	 * @return	a list of topic names
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<Topic> getTopics(String instructorId)
			throws DBException {
		Connection con = null;
		try {
			con = getPoolConnection();
			return getTopics(con, instructorId);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // getTopics(String)

	/** Gets all topics with all their question sets.
	 * @param	con	database connection
	 * @param	instructorId	instructor login ID; null
	 * if master author
	 * @return	a list of topics
	 * @throws	SQLException	if there's a problem reading the database
	 */
	private static List<Topic> getTopics(Connection con,
			String instructorId) throws SQLException {
		final String SELF = "QSetRW.getTopics: ";
		final List<Topic> topics = new ArrayList<Topic>();
		final List<Integer> topicIds = new ArrayList<Integer>();
		final String qry = toString(
				SELECT, joinAll(
					TOPIC_ID,
					TOPIC_NAME,
					TOPIC_REMARKS),
				FROM + TOPICS
				+ ORDER_BY + TOPIC_NAME);
		debugPrint(SELF, qry);
		PreparedStatement pstmt = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			if (!rs.next()) return topics;
			do {
				final Topic topic = 
						new Topic(rs.getInt(TOPIC_ID), instructorId);
				topic.name = rs.getString(TOPIC_NAME);
				topic.remarks = rs.getString(TOPIC_REMARKS);		
				if (topic.remarks == null) topic.remarks = "";
				topics.add(topic);
				topicIds.add(Integer.valueOf(topic.id));
			} while (rs.next());
			closeStmtAndRs(stmt, rs);
			final boolean isMasterAuthor = instructorId == MASTER_AUTHOR;
			final SQLWithQMarks sql_vals = new SQLWithQMarks(toString(
					SELECT, joinAll(
						QSET_ID,
						QSET_TOPIC_ID,
						QSET_NAME,
						QSET_BOOKAUTHOR,
						QSET_COMMONQSTATEMENT,
						QSET_REMARKS),
					FROM + QSETS
					+ WHERE + QSET_ID + IS_POSITIVE));
			if (!isMasterAuthor) {
 				sql_vals.addToSql(OR + QSET_AUTHOR + EQUALS + QMARK);
				sql_vals.addValue(instructorId);
			} // if local author
			sql_vals.addToSql(
					ORDER_BY, joinAll(
						QSET_TOPIC_ID, 
						QSET_NUM,
						QSET_ID));
			debugPrint(SELF, sql_vals);
			pstmt = getStatement(con, sql_vals);
			rs = pstmt.executeQuery();
			if (!rs.next()) return topics;
			do {
				final int topicId = rs.getInt(QSET_TOPIC_ID);
				final List<QSetDescr> qSets = new ArrayList<QSetDescr>();
				final List<QSetDescr> localQSets = new ArrayList<QSetDescr>();
				debugPrint(SELF + "loading qSets for topic with ID ", topicId);
				while (true) {
					final QSetDescr qSet = new QSetDescr();
					qSet.id = rs.getInt(QSET_ID);
					qSet.topicId = topicId;
					qSet.name = rs.getString(QSET_NAME);
					qSet.author = rs.getString(QSET_BOOKAUTHOR);
					if (qSet.author == null) qSet.author = "";
					qSet.header = rs.getString(QSET_COMMONQSTATEMENT);
					if (qSet.header == null) qSet.header = "";
					qSet.remarks = rs.getString(QSET_REMARKS);
					if (qSet.remarks == null) qSet.remarks = "";
					if (qSet.id < 0) {
						localQSets.add(0, qSet);
					} else {
						qSets.add(qSet);
					}
					if (!rs.next()) break;
					if (rs.getInt(QSET_TOPIC_ID) != topicId) break;
				}
				qSets.addAll(localQSets);
				debugPrint(SELF + "adding qSets for topic with ID ", topicId);
				final int topicNum = topicIds.indexOf(Integer.valueOf(topicId));
				if (topicNum >= 0) {
					topics.get(topicNum).qSets = qSets;
				}	
			} while (!rs.isAfterLast());
			return topics;
		} finally {
			closeStmtAndRs(stmt, rs);
			closeConnection(null, pstmt, null);
		}
	} // getTopics(Connection, String)

	/** Gets the name of a topic from its unique ID number.
	 * @param	topicId	unique ID of the topic
	 * @return	the topic name
	 */
	public static String getTopicNameById(int topicId) {
		final String SELF = "QSetRW.getTopicNameById: ";
		final String qry = 
				SELECT + TOPIC_NAME 
				+ FROM + TOPICS
				+ WHERE + TOPIC_ID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				topicId);
		debugPrint(SELF, sql_vals);
		String topicName = ""; // default
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) {
				topicName = rs.getString(TOPIC_NAME);
				debugPrint(SELF + "topic ", topicId,
						" has name ", topicName);
			}
		} catch (SQLException e) {
			Utils.alwaysPrint(SELF + "query failed.");
			e.printStackTrace();
		} finally {
			closeConnection(con, stmt, rs);
		}
		return topicName;
	} // getTopicNameById(int)

	/** Gets the ID of the topic in which a question set resides.
	 * @param	qSetId	unique ID of the question set
	 * @return	ID of the topic in which a question set resides
	 */
	public static int getTopicIdByQSetId(int qSetId) {
		final String SELF = "QSetRW.getTopicIdByQSetId: ";
		final String qry = 
				SELECT + QSET_TOPIC_ID
				+ FROM + QSETS
				+ WHERE + QSET_ID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				qSetId);
		debugPrint(SELF, sql_vals);
		int topicId = 0; // default
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) topicId = rs.getInt(QSET_TOPIC_ID);
		} catch (SQLException e) {
			Utils.alwaysPrint(SELF + "query failed.");
			e.printStackTrace();
			topicId = 0;
		} finally {
			closeConnection(con, stmt, rs);
		}
		return topicId;
	} // getTopicIdByQSetId(int)

	/** Gets the ID of the topic in which a question resides.
	 * @param	qId	unique ID of the question
	 * @return	ID of the topic in which a question resides
	 */
	public static int getTopicIdByQId(int qId) {
		return getTopicIdByQSetId(getQSetIDbyQId(qId));
	} // getTopicIdByQId(int)

	/** Adds a new topic.
	 * @param	topic	the topic to be added
	 * @return	unique ID of the new topic
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static int addTopic(Topic topic) throws DBException {
		final String SELF = "QSetRW.addTopic: ";
		int topicId = 1;
		Connection con = null;
		try {
			con = getPoolConnection();
			topicId = nextSequence(con, TOPICS_SEQ);
			final String[] fields = new String[] {
					TOPIC_ID,
					TOPIC_NAME,
					TOPIC_REMARKS};
			final String qry = getInsertIntoValuesQMarksSQL(TOPICS, fields);
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
					topicId,
					topic.name == null ? NULL : topic.name,
					topic.remarks == null ? NULL : topic.remarks);
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
		return topicId;
	} // addTopic(Topic)

	/** Modifies a topic.
	 * @param	topicId	unique ID of the topic (redundant)
	 * @param	topic	the topic to be modified
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setTopic(int topicId, Topic topic) throws DBException {
		final String SELF = "QSetRW.setTopic: ";
		final String name = (topic.name == null ? NULL
				: topic.name);
		final String remarks = (topic.remarks == null ? NULL
				: topic.remarks);
		final String qry = toString(
				UPDATE + TOPICS + SET, equalsJoinQMarks(
					TOPIC_NAME,
					TOPIC_REMARKS),
				WHERE + TOPIC_ID + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				name,
				remarks,
				topicId);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		try {
			con = getPoolConnection();
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // setTopic(int, Topic)

	/** Reorders the question sets within a topic.
	 * @param	topic	the topic
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void reorderQSets(Topic topic) throws DBException {
		final String SELF = "QSetRW.reorderQSets: ";
		final String qry = toString(
				UPDATE + QSETS + SET + QSET_NUM + EQUALS + QMARK
				+ WHERE + QSET_ID + EQUALS + QMARK);
		debugPrint(SELF, qry);
		Connection con = null;
		PreparedStatement stmt = null;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			stmt = con.prepareStatement(qry);
			int qSetNum = 1;
			for (final QSetDescr qSet : topic.qSets) {
				final StringBuilder joinedValues = setValues(stmt, 
						qSetNum++,
						qSet.id);
				debugPrint(SELF, "batch ", qSetNum, ": ", joinedValues); 
				stmt.addBatch();
			} //  for each qSet
			stmt.executeBatch();
			con.commit();
		} catch (SQLException e) {
			rollbackConnection(con);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, null);
		}
	} // reorderQSets(Topic)

/* **************** Methods regarding question sets ********************/

	/** Retrieves the description of a single question set.
	 * @param	qSetId	unique ID of the question set
	 * @return	a question set description
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static QSetDescr getQSetDescr(int qSetId) throws DBException {
		return getQSetDescr(qSetId, MASTER_AUTHOR);
	} // getQSetDescr(int)

	/** Retrieves the description of a single question set.
	 * @param	qSetId	unique ID of the question set
	 * @param	instructorId	instructor login ID
	 * @return	a question set description
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static QSetDescr getQSetDescr(int qSetId,
			String instructorId) throws DBException {
		Connection con = null;
		try {
			con = getPoolConnection();
			return getQSetDescr(con, qSetId, instructorId);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // getQSetDescr(int, String)

	/** Retrieves the description of a single question set.
	 * @param	con	database connection
	 * @param	qSetId	unique ID of the question set
	 * @param	instructorId	instructor login ID
	 * @return	a question set description
	 * @throws	SQLException	if there's a problem reading the database
	 */
	public static QSetDescr getQSetDescr(Connection con,
			int qSetId, String instructorId) throws SQLException {
		final String SELF = "QSetRW.getQSetDescr: ";
		String qry = toString(
				SELECT, joinAll(
					QSET_TOPIC_ID,
					QSET_NAME,
					QSET_BOOKAUTHOR,
					QSET_COMMONQSTATEMENT,
					QSET_REMARKS),
				FROM + QSETS
				+ WHERE + QSET_ID + EQUALS + QMARK);
		SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				qSetId);
		debugPrint(SELF, sql_vals);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		final QSetDescr qSet = new QSetDescr();
		try {
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (!rs.next()) {
				Utils.alwaysPrint(SELF + "ERROR: no information about qSet ", qSetId);
				return null;
			}
			qSet.id = qSetId;
			qSet.topicId = rs.getInt(QSET_TOPIC_ID);
			qSet.name = rs.getString(QSET_NAME);
			qSet.author = rs.getString(QSET_BOOKAUTHOR);
			if (qSet.author == null) qSet.author = "";
			qSet.header = rs.getString(QSET_COMMONQSTATEMENT);
			if (qSet.header == null) qSet.header = "";
			debugPrint(SELF + "qSet.header = ", qSet.header);
			qSet.remarks = rs.getString(QSET_REMARKS);
			if (qSet.remarks == null) qSet.remarks = "";
			closeStmtAndRs(stmt, rs);
			// Fill up topic name
			qry = SELECT + TOPIC_NAME
					+ FROM + TOPICS
					+ WHERE + TOPIC_ID + EQUALS + QMARK;
			sql_vals = new SQLWithQMarks(qry,
					qSet.topicId);
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (!rs.next()) {
				Utils.alwaysPrint(SELF + "ERROR: no name or book for qSet ", qSetId);
				return qSet;
			}
			qSet.topicName = rs.getString(TOPIC_NAME);
		} finally {
			closeStmtAndRs(stmt, rs);
		}
		if (instructorId != MASTER_AUTHOR) {
			// look for locally modified headers for this Qset; RBG 1/2008
			qry = SELECT + MODHEAD_COMMONQSTATEMENT
					+ FROM + MODIFIED_HEADERS
					+ WHERE + MODHEAD_AUTHOR + EQUALS + QMARK
					+ AND + MODHEAD_QSETID + EQUALS + QMARK;
			sql_vals = new SQLWithQMarks(qry,
					instructorId,
					qSetId);
			debugPrint(SELF, sql_vals);
			try {
				stmt = getStatement(con, sql_vals);
				rs = stmt.executeQuery();
				if (rs.next()) {
					qSet.header = rs.getString(MODHEAD_COMMONQSTATEMENT);
					debugPrint(SELF + "locally modified qSet.header = ", qSet.header);
					qSet.headerModifiedLocally = true;
				}
			} catch (SQLException e) {
				Utils.alwaysPrint(SELF + "couldn't get locally modified headers");
				e.printStackTrace();
				Utils.alwaysPrint("Returning unmodified qSetDescr.");
			} finally {
				closeStmtAndRs(stmt, rs);
			}
		}
		return qSet;
	} // getQSetDescr(Connection, int, String)

	/** Gets the author of a question set.
	 * @param	qSetId	unique ID of the question set.
	 * @return	the author's login ID
	 */
	public static String getAuthorIdByQSetId(int qSetId) {
		final String SELF = "QSetRW.getAuthorIdByQSetId: ";
		if (qSetId > 0) return null;
		final String qry = 
				SELECT + QSET_AUTHOR
				+ FROM + QSETS
				+ WHERE + QSET_ID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				qSetId);
		debugPrint(SELF, sql_vals);
		String authorId = null; // default
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) authorId = rs.getString(QSET_AUTHOR);
		} catch (SQLException e) {
			Utils.alwaysPrint(SELF + "query failed.");
			e.printStackTrace();
		} finally {
			closeConnection(con, stmt, rs);
		}
		return authorId;
	} // getAuthorIdByQSetId(int)

	/** Gets the number of master and local questions in a question set.
	 * @param	qSetId	unique ID of the question set
	 * @param	authorId	instructor login ID; null for master author
	 * @return	number of master questions in the question set plus either all
	 * locally authored questions in the set (if author is master author) or
	 * questions locally authored by authorId in the set (if author is local
	 * author)
	 */
	public static int getNumQsInQSet(int qSetId, String authorId) {
		final String SELF = "QSetRW.getNumQsInQSet: ";
		final DBTables masterTables = new DBTables();
		final DBLocalTables localTables = new DBLocalTables();
		final boolean isMasterAuthor = authorId == MASTER_AUTHOR;
		final char c = 'c';
		final StringBuilder qryBld = getBuilder(
				SELECT, sum(c), AS + SRCH_RESULT 
				+ FROM, parensBuild(
					SELECT, count(), AS, c,
					FROM, masterTables.QUESTIONS,
					WHERE + Q_QSET + EQUALS + QMARK
					+ UNION_ALL + SELECT, count(),
					FROM, localTables.QUESTIONS,
					WHERE + Q_QSET + EQUALS + QMARK
					+ AND + Q_QID + LESS_THAN, '0'));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qryBld,
				qSetId, 
				qSetId);
		if (!isMasterAuthor) {
			sql_vals.addToSql(AND, DBLocalTables.Q_AUTHOR, EQUALS + QMARK);
			sql_vals.addValue(authorId);
		} // if not master author
		debugPrint(SELF, sql_vals);
		int numQs = 0;
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) {
				numQs = rs.getInt(SRCH_RESULT);
				if (isMasterAuthor)
					debugPrint(SELF + "found ", numQs,
							" Q(s) in qSet ", qSetId, 
							" including all locally authored Qs");
				else debugPrint(SELF + "found ", numQs,
						" Q(s) in qSet ", qSetId, 
						" including Qs authored by ", authorId);
			} // if there's a result
		} catch (SQLException e) {
			Utils.alwaysPrint("getNumQsInQSet: query failed.");
			e.printStackTrace();
			numQs = -1;
		} finally {
			closeConnection(con, stmt, rs);
		}
		return numQs;
	} // getNumQsInQSet(int)

	/** Gets the ID of the question set in which a question resides.
	 * @param	qId	unique ID of the question
	 * @return	ID of the question set in which a question resides
	 */
	public static int getQSetIDbyQId(int qId) {
		final String SELF = "QSetRW.getQSetIDbyQId: ";
		// if question is locally modified, it will be in same qSet as the
		// master Q, so only locally authored question need be sought in the
		// local tables
		final DBTables tables = getTables(qId < 0);
		final String qry = toString(
				SELECT + Q_QSET 
				+ FROM, tables.QUESTIONS,
				WHERE + Q_QID + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				qId);
		debugPrint(SELF, sql_vals);
		int qSetId = 0; // default
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) qSetId = rs.getInt(Q_QSET);
		} catch (SQLException e) {
			Utils.alwaysPrint(SELF + "query failed.");
			e.printStackTrace();
			qSetId = 0;
		} finally {
			closeConnection(con, stmt, rs);
		}
		return qSetId;
	} // getQSetIDbyQId(int)

	/** Adds a new master question set to a topic.
	 * @param	topicId	unique ID of the topic that will contain the qSet
	 * @param	qSet	the qSet information
	 * @return	unique ID of the new question set
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static int addQSet(int topicId, QSetDescr qSet) throws DBException {
		return addQSet(topicId, qSet, MASTER_AUTHOR);
	} // addQSet(int, QSetDescr)

	/** Adds a new local or master question set to a topic.
	 * @param	topicId	unique ID of the topic that will contain the qSet
	 * @param	qSet	the qSet information
	 * @param	authorId	login ID
	 * @return	unique ID of the new question set
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static int addQSet(int topicId, QSetDescr qSet,
			String authorId) throws DBException {
		final String SELF = "QSetRW.addQSet: ";
		Connection con = null;
		int qSetId = -1;
		try {
			con = getPoolConnection();
			final boolean masterEdit = authorId == MASTER_AUTHOR;
			qSetId = nextSequence(con, QSETS_SEQ);
			if (!masterEdit) qSetId = -qSetId;
			final String name = (qSet.name == null ? NULL
					: qSet.name);
			final String author = (qSet.author == null ? NULL
					: qSet.author);
			final String header = (isEmpty(qSet.header) ? "" 
					: unicodeToCERs(qSet.header));
			final String remarks = (isEmpty(qSet.remarks) ? "" 
					: unicodeToCERs(qSet.remarks));
			String[] fields = new String[] {
					QSET_ID,
					QSET_TOPIC_ID,
					QSET_NAME,
					QSET_BOOKAUTHOR,
					QSET_COMMONQSTATEMENT, // CLOB field
					QSET_REMARKS}; // CLOB field
			if (!masterEdit) {
				fields = (String[]) ArrayUtils.add(fields, QSET_AUTHOR);
			} // if local edit 
			final String qry = getInsertIntoValuesQMarksSQL(QSETS, fields);
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
					qSetId,
					topicId,
					name,
					author,
					header, 
					remarks);
			if (!masterEdit) {
				sql_vals.addValue(authorId);
			} // if local edit 
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
		return qSetId;
	} // addQSet(int, QSetDescr, String)

	/** Modifies a master-created question set description.
	 * @param	qSet	the qSet information
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setQSetDescr(QSetDescr qSet)
			throws DBException {
		setQSetDescr(qSet, MASTER_AUTHOR, DELETE_TRANSLNS);
	} // setQSetDescr(QSetDescr)

	/** Modifies a locally or master-created question set description.
	 * @param	qSet	the qSet information
	 * @param	authorId	login ID
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setQSetDescr(QSetDescr qSet,
			String authorId) throws DBException {
		setQSetDescr(qSet, authorId, DELETE_TRANSLNS);
	} // setQSetDescr(QSetDescr, String)

	/** Modifies a locally or master-created question set description.
	 * @param	qSet	the qSet information
	 * @param	deleteTranslns	whether to delete the translations (if header is
	 * changed)
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setQSetDescr(QSetDescr qSet,
			boolean deleteTranslns) throws DBException {
		setQSetDescr(qSet, MASTER_AUTHOR, deleteTranslns);
	} // setQSetDescr(QSetDescr, boolean)

	/** Modifies a locally or master-created question set description.
	 * @param	qSet	the qSet information
	 * @param	authorId	login ID
	 * @param	deleteTranslns	whether to delete the translations (if header is
	 * changed)
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setQSetDescr(QSetDescr qSet,
			String authorId, boolean deleteTranslns) throws DBException {
		final String SELF = "QSetRW.setQSetDescr: ";
		final String name = (qSet.name == null ? NULL
				: qSet.name);
		final String author = (qSet.author == null ? NULL
				: qSet.author);
		final String header = (isEmpty(qSet.header) ? "" 
				: unicodeToCERs(qSet.header));
		final String remarks = (isEmpty(qSet.remarks) ? ""
				: unicodeToCERs(qSet.remarks));
		Connection con = null;
		try {
			con = getPoolConnection();
			final boolean masterAuthor = authorId == MASTER_AUTHOR;
			if (deleteTranslns) {
				final String qry = toString(
						TranslnWrite.DELETE_TRANSLATIONS_BY_IDS(),
						parens(
							SELECT + QSET_COMMONQSTMT_ID
							+ FROM + QSETS
							+ WHERE + QSET_COMMONQSTMT_ID + IS_NOT_ZERO
							+ AND + QSET_ID + EQUALS + QMARK));
				final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
						qSet.id);
				debugPrint(SELF, sql_vals);
				tryUpdate(con, sql_vals);
			} // if should delete translations
			final String qry = toString(
					UPDATE + QSETS + SET, equalsJoinQMarks(
						QSET_NAME,
						QSET_BOOKAUTHOR,
						QSET_COMMONQSTATEMENT, // CLOB field
						QSET_COMMONQSTMT_ID,
						QSET_REMARKS)); // CLOB field
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
					name,
					author,
					header,
					0,
					remarks);
			if (!masterAuthor) {
				sql_vals.addToSql(addEqualsJoinQMarks(QSET_AUTHOR));
				sql_vals.addValue(authorId);
			} // if local author
			sql_vals.addToSql(WHERE + QSET_ID + EQUALS + QMARK);
			sql_vals.addValue(qSet.id);
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // setQSetDescr(QSetDescr, String, boolean)

	/** Deletes a question set.  Call only if the set contains no questions.
	 * @param	qSetId	unique ID of the qSet to be deleted
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void deleteQSet(int qSetId) throws DBException {
		final String SELF = "QSetRW.deleteQSet: ";
		final String qry = toString(
				TranslnWrite.DELETE_TRANSLATIONS_BY_IDS(),
				parensBuild(
					SELECT + QSET_COMMONQSTMT_ID
					+ FROM + QSETS
					+ WHERE + QSET_COMMONQSTMT_ID + IS_NOT_ZERO
					+ AND + QSET_ID + EQUALS + QMARK));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				qSetId);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		try {
			con = getPoolConnection();
			tryUpdate(con, sql_vals);
			sql_vals.setSql(
					DELETE_FROM + QSETS 
					+ WHERE + QSET_ID + EQUALS + QMARK);
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // deleteQSet(int)

/* ********** Methods regarding common question statements ***************/

	/** Local modification of the common question statement of a master-created
	 * question set description.
	 * @param	authorId	login ID
	 * @param	qSetId	unique ID of the qSet to be modified (redundant)
	 * @param	header	new common question statement
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void addLocalHeader(String authorId, int qSetId,
			String header) throws DBException {
		final String SELF = "QSetRW.addLocalHeader: ";
		if (authorId == MASTER_AUTHOR) {
			Utils.alwaysPrint(SELF + "authorId is null; returning.");
			return;
		}
		final String headerStore = (isEmpty(header) ? "" 
				: unicodeToCERs(header));
		final String[] fields = new String[] {
				MODHEAD_AUTHOR,
				MODHEAD_QSETID,
				MODHEAD_COMMONQSTATEMENT}; // CLOB field
		final String qry = 
				getInsertIntoValuesQMarksSQL(MODIFIED_HEADERS, fields);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				authorId,
				qSetId,
				headerStore);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		try {
			con = getPoolConnection();
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // addLocalHeader(String, int, String)

	/** Reverts the common question statement of a
	 * question set description back to the original.
	 * @param	authorId	login ID
	 * @param	qSetId	unique ID of the qSet to be modified (redundant)
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void removeLocalHeader(String authorId, int qSetId)
			throws DBException {
		final String SELF = "QSetRW.removeLocalHeader: ";
		final String qry = 
				DELETE_FROM + MODIFIED_HEADERS 
				+ WHERE + MODHEAD_AUTHOR + EQUALS + QMARK
				+ AND + MODHEAD_QSETID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				authorId,
				qSetId);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		try {
			con = getPoolConnection();
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // removeLocalHeader(String, int)

	/** Gets all headers where inputToCERs(header) != header.
	 * @param	local	whether to edit headers by local authors
	 * @return	map of header by table:qSetId or table:qSetId:author
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<String, String> getConvertibleHeaders(boolean local) 
			throws DBException {
		final String SELF = "QSetRW.getConvertibleHeaders: ";
		final Map<String, String> headers = new HashMap<String, String>();
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = con.createStatement();
			if (!local) {
				final String qry = toString(
						SELECT, joinAll( 
							QSET_ID, 
							QSET_COMMONQSTATEMENT),
						FROM + QSETS);
				debugPrint(SELF, qry);
				rs = stmt.executeQuery(qry);
				while (rs.next()) {
					final String qSetId = rs.getString(QSET_ID);
					final String header = rs.getString(QSET_COMMONQSTATEMENT);
					if (header != null 
							&& !header.equals(Utils.inputToCERs(header))) {
						headers.put(toString(QSETS, ':', qSetId), header);
					}
				} // if there's a result
			} else {
				final String qry = toString(
						SELECT, joinAll(
							MODHEAD_AUTHOR,
							MODHEAD_QSETID, 
							MODHEAD_COMMONQSTATEMENT),
						FROM + MODIFIED_HEADERS);
				debugPrint(SELF, qry);
				rs = stmt.executeQuery(qry);
				while (rs.next()) {
					final String author = rs.getString(MODHEAD_AUTHOR);
					final String qSetId = rs.getString(MODHEAD_QSETID);
					final String header = 
							rs.getString(MODHEAD_COMMONQSTATEMENT);
					if (header != null 
							&& !header.equals(Utils.inputToCERs(header))) {
						headers.put(toString(MODIFIED_HEADERS, ':', 
								qSetId, ':', author), header);
					}
				} // if there's a result
			} // if local
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return headers;
	} // getConvertibleHeaders(boolean)

	/** Replaces an existing header with a new one.  Used to convert UTF-8
	 * text into ASCII with character entity representations.
	 * @param	key	the table and qSetID of the header and maybe the author, 
	 * colon-separated
	 * @param	header	the new header 
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void putHeader(String key, String header) throws DBException {
		final String SELF = "QSetRW.putHeader: ";
		final String[] pieces = key.split(":");
		final boolean isQSetsTable = QSETS.equals(pieces[0]);
		final String table = (isQSetsTable ? QSETS : MODIFIED_HEADERS);
		final String commonStmt = (isQSetsTable
				? QSET_COMMONQSTATEMENT : MODHEAD_COMMONQSTATEMENT);
		final String id = (isQSetsTable ? QSET_ID : MODHEAD_QSETID);
		final String qry = toString(
				UPDATE, table, 
				SET, commonStmt, EQUALS + QMARK // CLOB field 
				+ WHERE, id, EQUALS + QMARK, isQSetsTable ? ""
					: AND + MODHEAD_AUTHOR + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				header,
				Integer.decode(pieces[1]));
		if (!isQSetsTable) {
			sql_vals.addValue(pieces[2]);
		} // if modified header
		Connection con = null;
		try {
			con = getPoolConnection();
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // putHeader(String, String)

/* **************** Methods regarding Pearson books ********************/

	/** Gets all chapter numbers belonging to a book as written by master
	 * question authors.  NOTE: Why does this method not return multiple copies
	 * of each chapter number?
	 * @param	book	a Pearson book
	 * @return	a list of chapter names
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<String> getBookChapters(String book)
			throws DBException {
		final DBTables masterTables = new DBTables();
		final String qry = toString(
				SELECT + Q_BOOKCHAP + FROM, masterTables.QUESTIONS,
				WHERE + Q_BOOK + EQUALS + QMARK
				+ ORDER_BY + Q_BOOKCHAP);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				book);
		final List<String> chapters = new ArrayList<String>();
		debugPrint("QSetRW.getBookChapters: ", sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) chapters.add(rs.getString(Q_BOOKCHAP));
			return chapters;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}		
	} // getBookChapters(String)
	
	/** Gets all chapter numbers belonging to a book as written by local
	 * question authors.  
	 * @param	book	a Pearson book
	 * @param	instructorId	instructor login ID
	 * @return	a list of chapter names
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<String> getBookChapters(String book,
			String instructorId) throws DBException {
		final DBTables masterTables = new DBTables();
		final DBLocalTables localTables = new DBLocalTables();
		final List<String> chapters = new ArrayList<String>();
		final String qry = toString(
				SELECT + Q_BOOKCHAP 
				+ FROM, parensBuild(
					SELECT + Q_BOOKCHAP 
					+ FROM, masterTables.QUESTIONS,
					WHERE + Q_BOOK + EQUALS + QMARK
					+ UNION + SELECT + Q_BOOKCHAP 
					+ FROM, localTables.QUESTIONS,
					WHERE + Q_BOOK + EQUALS + QMARK
					+ AND, DBLocalTables.Q_AUTHOR, EQUALS + QMARK),
				ORDER_BY + Q_BOOKCHAP);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				book,
				book,
				instructorId);
		debugPrint("QSetRW.getBookChapters: ", sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while(rs.next()) {
				chapters.add(rs.getString(Q_BOOKCHAP));
			}
			return chapters;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}		
	} // getBookChapters(String, String)

	/** Gets all the books mentioned in the master database.
	 * @return	a list of books
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<String> listBooks() throws DBException {
		final DBTables masterTables = new DBTables();
		final String qry = toString(
				SELECT_UNIQUE + Q_BOOK 
				+ FROM, masterTables.QUESTIONS);
		debugPrint("QSetRW.listBooks: ", qry);
		final List<String> books = new ArrayList<String>();
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			while (rs.next()) {
				books.add(rs.getString(Q_BOOK));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return books;
	} // listBooks()

	/** Gets all the chapter numbers in the given book mentioned in the
	 * master database.
	 * @param	book	a Pearson book
	 * @return	a list of chapter numbers
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<String> listChapters(String book) throws DBException {
		final DBTables masterTables = new DBTables();
		final String qry = toString(
				SELECT_UNIQUE + Q_BOOKCHAP 
				+ FROM, masterTables.QUESTIONS,
				WHERE + Q_BOOK + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				book);
		debugPrint("QSetRW.listChapters: ", sql_vals);
		final List<String> chaps = new ArrayList<String>();
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				chaps.add(rs.getString(Q_BOOKCHAP));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return chaps;
	} // listChapters(String)

	/** Gets all the qIds in the given book, given chapter, mentioned in the 
	 * master database.
	 * @param	book	a Pearson book
	 * @param	chapter	a chapter number
	 * @return	a list of question unique IDs
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<Integer> listQuestions(String book, String chapter)
			throws DBException {
		final DBTables masterTables = new DBTables();
		final String qry = toString(
				SELECT_UNIQUE + Q_QID 
				+ FROM, masterTables.QUESTIONS,
				WHERE + Q_BOOK + EQUALS + QMARK
				+ AND + Q_BOOKCHAP + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				book,
				chapter);
		debugPrint("QSetRW.listQuestions: ", sql_vals);
		final List<Integer> qIds = new ArrayList<Integer>();
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) qIds.add(rs.getInt(Q_QID));
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return qIds;
	} // listQuestions(String, String)

	/** Constructor to disable external instantiation. */
	private QSetRW() { }

} // QSetRW
