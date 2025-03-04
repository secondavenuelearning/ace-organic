package com.epoch.db;

import static com.epoch.db.dbConstants.CourseRWConstants.*;
import static com.epoch.db.dbConstants.ForumRWConstants.*;
import static com.epoch.db.dbConstants.UserRWConstants.*;
import com.epoch.AppConfig;
import com.epoch.courseware.EnrollmentData;
import com.epoch.courseware.ForumPost;
import com.epoch.courseware.ForumTopic;
import com.epoch.courseware.User;
import com.epoch.exceptions.DBException;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/** Handles database operations involving forums. */
public final class ForumRW extends DBCommon {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

	/** Parameter for getPosts(). */
	private static final boolean REVERSE = true;
	/** Parameter for getNumPostImages(). */
	private static final boolean YEAR_OLD = true;
	/** Name for database field. */
	private static final String SRCH_RESULT1 = SRCH_RESULT + '1';
	/** Name for database field. */
	private static final String SRCH_RESULT2 = SRCH_RESULT + '2';
	/** Name for database field. */
	private static final String SRCH_RESULT3 = SRCH_RESULT + '3';

/* ***************** Methods to get topics ************************/

	/** Gets a topic by its ID number.
	 * @param	topicId	ID number of the topic
	 * @return	the topic
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static ForumTopic getTopic(int topicId) throws DBException {
		final String where = WHERE + FTOPIC_ID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(where, topicId);
		final ForumTopic[] topics = getTopics(sql_vals);
		return (!Utils.isEmpty(topics) ? topics[0] : null);
	} // getTopic(int)

	/** Gets topics by their ID numbers.
	 * @param	topicIds	ID numbers of the topics
	 * @return	the topics
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static ForumTopic[] getTopics(List<Integer> topicIds) 
			throws DBException {
		if (Utils.isEmpty(topicIds)) return new ForumTopic[0];
		final String where = toString(
				WHERE + FTOPIC_ID + IN, parensQMarks(topicIds));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(where, topicIds);
		return getTopics(sql_vals);
	} // getTopics(List<Integer>)

	/** Gets topics.
	 * @param	sql_vals	how to select the topics to get, and values to
	 * substitute for question marks
	 * @return	array of topics
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static ForumTopic[] getTopics(SQLWithQMarks sql_vals) 
			throws DBException {
		final String SELF = "ForumRW.getTopics: ";
		final String qry = toString(
				SELECT, joinAll(
					FTOPIC_ID,
					FTOPIC_COURSE,
					FTOPIC_CREATOR,
					FTOPIC_DATE,
					FTOPIC_TITLE,
					FTOPIC_STICKY,
					FTOPIC_HWID,
					FTOPIC_QID,
					SRCH_RESULT2,
					ROW_NUM + AS + SRCH_RESULT3),
				FROM, joinAll(
					FORUM_TOPICS,
					parensBuild(
						SELECT, joinAll(
							POST_TOPIC + AS + SRCH_RESULT1,
							getBuilder(count(), AS + SRCH_RESULT2)),
						FROM + POSTS 
						+ GROUP_BY + POST_TOPIC)),
				sql_vals.getSql(),
				AND + FTOPIC_ID + EQUALS + SRCH_RESULT1,
				ORDER_BY, joinAll(
					FTOPIC_STICKY + DESCENDING,
					FTOPIC_ID + DESCENDING));
		final SQLWithQMarks newSql_vals = 
				new SQLWithQMarks(qry, sql_vals);
		return getTopicsFromDB(newSql_vals);
	} // getTopics(SQLWithQMarks)

	/** Gets all of the topics in the forum of a course. 
	 * @param	courseId	ID number of the course
	 * @return	array of forum topics in this course
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static ForumTopic[] getTopics(int courseId) throws DBException {
		final int[] ALL_TOPICS = null;
		return getTopics(courseId, ALL_TOPICS);
	} // getTopics(int)

	/** Gets the topics of a course, perhaps in a range. 
	 * @param	courseId	ID number of the course
	 * @param	range	inclusive upper and lower numbers of the topics to get
	 * @return	array of topics
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static ForumTopic[] getTopics(int courseId, int[] range) 
			throws DBException {
		final String SELF = "ForumRW.getTopics: ";
		final int FIRST50 = 0;
		final int ALL_TOPICS = 1;
		final int LATER50 = 2;
		final int condition = (Utils.isEmpty(range) ? ALL_TOPICS 
				: range[0] == 1 ? FIRST50 : LATER50);
		final StringBuilder selectBld = getBuilder(
				SELECT, joinAll(
					FTOPIC_ID,
					FTOPIC_COURSE,
					FTOPIC_CREATOR,
					FTOPIC_DATE,
					FTOPIC_TITLE,
					FTOPIC_STICKY,
					FTOPIC_HWID,
					FTOPIC_QID,
					SRCH_RESULT2));
		final String qry = toString(
				SELECT + ALL + FROM, parensBuild(
					selectBld, postjoin(ROW_NUM + AS + SRCH_RESULT3),
					FROM, parensBuild(
						selectBld,
						FROM, joinAll(
							FORUM_TOPICS,
							parensBuild(
								SELECT, joinAll(
									POST_TOPIC + AS + SRCH_RESULT1,
									getBuilder(count(), AS + SRCH_RESULT2)), 
								FROM + POSTS 
								+ GROUP_BY + POST_TOPIC)),
						WHERE + FTOPIC_COURSE + EQUALS + QMARK
						+ AND + FTOPIC_ID + EQUALS + SRCH_RESULT1,
						ORDER_BY, joinAll(
							FTOPIC_STICKY + DESCENDING,
							FTOPIC_ID + DESCENDING))));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				courseId);
		if (condition == FIRST50) {
			sql_vals.addToSql(
					WHERE, parensBuild(
						FTOPIC_STICKY + EQUALS + Y + OR, parensBuild(
							SRCH_RESULT3 + NOT_LESS_THAN, range[0], 
								PLUS, parensBuild( 
									SELECT, count(), 
									FROM + FORUM_TOPICS 
									+ WHERE + FTOPIC_COURSE + EQUALS + QMARK
									+ AND + FTOPIC_STICKY + EQUALS + Y), 
							AND + SRCH_RESULT3 + NOT_MORE_THAN, range[1]))); 
			sql_vals.addValue(courseId);
		} else if (condition == LATER50) {
			sql_vals.addToSql(
					WHERE, SRCH_RESULT3 + NOT_LESS_THAN, range[0], 
					AND + SRCH_RESULT3 + NOT_MORE_THAN, range[1]); 
		} // if condition
		sql_vals.addToSql(ORDER_BY + SRCH_RESULT3);
		return getTopicsFromDB(sql_vals);
	} // getTopics(int, int[])

	/** Gets topics.
	 * @param	getTopicIdsSql_vals	how to select the topics to get, and values 
	 * to substitute for question marks
	 * @return	the topics
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static ForumTopic[] getTopicsFromDB(
			SQLWithQMarks getTopicIdsSql_vals) throws DBException {
		final String SELF = "ForumRW.getTopicsFromDB: ";
		final List<ForumTopic> topics = new ArrayList<ForumTopic>();
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		int numTopics = 0;
		try { 
			con = getPoolConnection();
			debugPrint(SELF, getTopicIdsSql_vals);
			stmt = getStatement(con, getTopicIdsSql_vals);
			rs = stmt.executeQuery();
			final Map<Integer, ForumTopic> topicsById = 
					new HashMap<Integer, ForumTopic>();
			while (rs.next()) {
				final int topicId = rs.getInt(FTOPIC_ID);
				final int courseId = rs.getInt(FTOPIC_COURSE);
				final String creator = rs.getString(FTOPIC_CREATOR);
				final Date created = toDate(rs.getString(FTOPIC_DATE));
				final String title = rs.getString(FTOPIC_TITLE);
				final String stickyStr = rs.getString(FTOPIC_STICKY);
				final boolean sticky = 
						stickyStr != null && stickyStr.charAt(0) == 'Y';
				final int[] linkedAssgtQ = new int[] {
						rs.getInt(FTOPIC_HWID), rs.getInt(FTOPIC_QID)};
				final int numPosts = rs.getInt(SRCH_RESULT2);
				final ForumTopic topic = new ForumTopic(topicId, courseId, 
						creator, created, title, sticky, linkedAssgtQ, 
						numPosts);
				topicsById.put(Integer.valueOf(topicId), topic); 
				topics.add(topic);
			} // while there are more results
			closeStmtAndRs(stmt, rs);
			numTopics = topics.size();
			debugPrint(SELF + "got ", numTopics, " topics.");
			if (!topics.isEmpty()) {
				debugPrint(SELF + "getting most recent edit date and author "
						+ "for each topic.");
				final String qry = toString(
						SELECT, joinAll(
							POST_TOPIC,
							POST_EDITED,
							POST_AUTHOR,
							POST_FLAGS),
						FROM + POSTS 
						+ WHERE, parensJoin(
							POST_TOPIC,
							POST_EDITED),
						IN, parensBuild(
							SELECT, joinAll(
								POST_TOPIC,
								max(POST_EDITED)),
							FROM + POSTS
							+ WHERE + POST_TOPIC + IN, parensBuild(
								SELECT + FTOPIC_ID 
								+ FROM, parens(getTopicIdsSql_vals.getSql())),
							GROUP_BY + POST_TOPIC));
				SQLWithQMarks sql_vals = 
						new SQLWithQMarks(qry, getTopicIdsSql_vals);
				debugPrint(SELF, sql_vals);
				stmt = getStatement(con, sql_vals);
				rs = stmt.executeQuery();
				while (rs.next()) {
					final int topicId = rs.getInt(POST_TOPIC);
					final ForumTopic topic = 
							topicsById.get(Integer.valueOf(topicId));
					topic.setDateLastChanged(
							toDate(rs.getString(POST_EDITED)));
					topic.setLastUserId(
							ForumPost.isAnon(rs.getInt(POST_FLAGS))
								? null : rs.getString(POST_AUTHOR));
				} // while there are more results
				debugPrint(SELF + "getting first post in each topic in order "
						+ "to find whether each topic creator is anonymous.");
				final List<Integer> topicIds = new ArrayList<Integer>(
						topicsById.keySet());
				final String where = toString(
						WHERE + POST_ID + IN, parensBuild(
							SELECT, min(POST_ID), 
							FROM + POSTS
							+ WHERE + POST_TOPIC + IN, parensQMarks(topicIds),
							GROUP_BY + POST_TOPIC));
				sql_vals = new SQLWithQMarks(where, topicIds);
				final ForumPost[] firstPosts = getPosts(con, sql_vals);
				for (final ForumPost firstPost : firstPosts) {
					final int topicId = firstPost.getTopicId();
					final ForumTopic topic = 
							topicsById.get(Integer.valueOf(topicId));
					topic.setCreatorAnon(firstPost.isAnon());
				} // while there are more posts
			} // if there are topics
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return topics.toArray(new ForumTopic[numTopics]);
	} // getTopicsFromDB(SQLWithQMarks)

	/** Gets IDs of all sticky topics in a list of courses.
	 * @param	con	database connection
	 * @param	courseIds	ID numbers of the courses
	 * @return	the topic IDs
	 * @throws	SQLException	if there's a problem reading the database
	 */
	private static List<Integer> getStickyTopicIds(Connection con, 
			List<Integer> courseIds) throws SQLException {
		if (Utils.isEmpty(courseIds)) return new ArrayList<Integer>();
		final String where = toString(
				WHERE + FTOPIC_COURSE + IN, parensQMarks(courseIds),
				AND + FTOPIC_STICKY + EQUALS + Y);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(where, courseIds);
		return getTopicIds(con, sql_vals);
	} // getStickyTopicIds(Connection, List<Integer>)

	/** Gets IDs of topics.
	 * @param	con	database connection
	 * @param	whereSql_vals	how to select the topics to get, and values to
	 * substitute for question marks
	 * @return	the topic IDs
	 * @throws	SQLException	if there's a problem reading the database
	 */
	private static List<Integer> getTopicIds(Connection con, 
			SQLWithQMarks whereSql_vals) throws SQLException {
		final String SELF = "ForumRW.getTopicIds: ";
		final List<Integer> topicIds = new ArrayList<Integer>();
		final String qry = toString(
				SELECT + FTOPIC_ID + FROM + FORUM_TOPICS, 
				whereSql_vals.getSql());
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, whereSql_vals);
		debugPrint(SELF, sql_vals);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try { 
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				topicIds.add(Integer.valueOf(rs.getInt(FTOPIC_ID)));
			} // while there are more results
		} finally {
			closeStmtAndRs(stmt, rs);
		}
		return topicIds;
	} // getTopicIds(Connection, SQLWithQMarks)

	/** Gets the topic of a post.
	 * @param	postId	ID number of the post 
	 * @return	the topic
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static ForumTopic getTopicOfPost(int postId) throws DBException {
		final ForumTopic[] topics = getTopicsOfPosts(new int[] {postId});
		return (Utils.isEmpty(topics) ? null : topics[0]);
	} // getTopicOfPost(int)

	/** Gets the topic of various posts.
	 * @param	postIds	ID numbers of the posts 
	 * @return	the topics
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static ForumTopic[] getTopicsOfPosts(int[] postIds) 
			throws DBException {
		if (Utils.isEmpty(postIds)) return new ForumTopic[0];
		final String where = toString(
				WHERE + FTOPIC_ID + IN, parensBuild(
					SELECT + POST_TOPIC
					+ FROM + POSTS
					+ WHERE + POST_ID + IN, parensQMarks(postIds)));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(where, postIds);
		return getTopics(sql_vals);
	} // getTopicsOfPosts(int[])

	/** Gets a map of topics linked to questions in an assignment.
	 * @param	hwId	ID number of the assignment
	 * @return	map of topic ID numbers, keyed by question ID numbers
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<Integer, Integer> getTopicLinks(int hwId) 
			throws DBException {
		final String SELF = "ForumRW.getTopicLinks: ";
		final String qry = toString(
				SELECT, joinAll(
					FTOPIC_ID,
					FTOPIC_QID),
				FROM + FORUM_TOPICS
				+ WHERE + FTOPIC_HWID + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				hwId);
		debugPrint(SELF, sql_vals);
		final Map<Integer, Integer> topicIdsByQIds =
				new HashMap<Integer, Integer>();
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try { 
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final int topicId = rs.getInt(FTOPIC_ID);
				final int qId = rs.getInt(FTOPIC_QID);
				topicIdsByQIds.put(Integer.valueOf(qId), 
						Integer.valueOf(topicId));
			} // while there are results
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return topicIdsByQIds;
	} // getTopicLinks(int)

	/** Gets the number of topics in a course.
	 * @param	courseId	ID number of the course
	 * @return	the number of topics in the course
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static int getNumTopics(int courseId) throws DBException {
		final String SELF = "ForumRW.getNumTopics: ";
		final String qry = toString(
				SELECT, count(), AS + SRCH_RESULT 
				+ FROM + FORUM_TOPICS
				+ WHERE + FTOPIC_COURSE + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				courseId);
		debugPrint(SELF, sql_vals);
		int numTopics = 0;
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try { 
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) numTopics = rs.getInt(SRCH_RESULT);
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return numTopics;
	} // getNumTopics(int)

/* ***************** Methods to get posts ************************/

	/** Gets a post.
	 * @param	postId	the post to get 
	 * @return	the post
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static ForumPost getPost(int postId) throws DBException {
		final String where = WHERE + POST_ID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(where, postId);
		final ForumPost[] posts = getPosts(sql_vals);
		return (!Utils.isEmpty(posts) ? posts[0] : null);
	} // getPost(int)

	/** Gets all the posts of a topic.
	 * @param	topicId	ID number of the topic
	 * @return	array of all posts
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static ForumPost[] getPosts(int topicId) throws DBException {
		final int[] ALL_POSTS = null;
		return getPosts(topicId, ALL_POSTS);
	} // getPosts(int)

	/** Gets all the posts of a topic in a range.
	 * @param	topicId	ID number of the topic
	 * @param	range	inclusive upper and lower numbers of the posts to get
	 * @return	array of all posts in the range
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static ForumPost[] getPosts(int topicId, int[] range) 
			throws DBException {
		final StringBuilder postBld = getBuilder(
				WHERE + POST_TOPIC + EQUALS + QMARK);
		if (!Utils.isEmpty(range)) {
 			appendTo(postBld, AND + ROW_NUM + NOT_LESS_THAN, range[0], 
					AND + ROW_NUM + NOT_MORE_THAN, range[1]);
		} // if getting within a range
		final SQLWithQMarks sql_vals = new SQLWithQMarks(postBld, topicId);
		return getPosts(sql_vals);
	} // getPosts(int, int[])

	/** Gets posts with certain IDs.
	 * @param	postIds	the posts to get 
	 * @return	the posts
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static ForumPost[] getPosts(int[] postIds) throws DBException {
		if (Utils.isEmpty(postIds)) return new ForumPost[0];
		final StringBuilder postBld = getBuilder(
				WHERE + POST_ID + IN, parensQMarks(postIds));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(postBld, postIds);
		return getPosts(sql_vals);
	} // getPosts(int[])

	/** Gets posts.
	 * @param	sql_vals	SQL for selecting the posts to get, and values to
	 * substitute for question marks
	 * @return	array of posts
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static ForumPost[] getPosts(SQLWithQMarks sql_vals) 
			throws DBException {
		return getPosts(sql_vals, !REVERSE);
	} // getPosts(SQLWithQMarks)

	/** Gets posts.
	 * @param	sql_vals	SQL for selecting the posts to get, and values to
	 * substitute for question marks
	 * @param	reverse	whether to reverse the chronology of the posts
	 * @return	array of posts
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static ForumPost[] getPosts(SQLWithQMarks sql_vals, 
			boolean reverse) throws DBException {
		final String SELF = "ForumRW.getPosts: ";
		Connection con = null;
		try { 
			con = getPoolConnection();
			return getPosts(con, sql_vals, reverse);
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // getPosts(SQLWithQMarks, boolean)

	/** Gets posts.
	 * @param	con	database connection
	 * @param	sql_vals	SQL for selecting the posts to get, and values to
	 * substitute for question marks
	 * @return	array of posts
	 * @throws	SQLException	if there's a problem reading the database
	 */
	private static ForumPost[] getPosts(Connection con, SQLWithQMarks sql_vals) 
			throws SQLException {
		return getPosts(con, sql_vals, !REVERSE);
	} // getPosts(Connection, SQLWithQMarks)

	/** Gets posts.
	 * @param	con	database connection
	 * @param	whereSql_vals	SQL for selecting the posts to get, and values to
	 * substitute for question marks
	 * @param	reverse	whether to reverse the chronology of the posts
	 * @return	array of posts
	 * @throws	SQLException	if there's a problem reading the database
	 */
	private static ForumPost[] getPosts(Connection con, 
			SQLWithQMarks whereSql_vals, boolean reverse) throws SQLException {
		final String SELF = "ForumRW.getPosts: ";
		final String qry = toString(
				SELECT, joinAll(
					POST_ID,
					POST_TOPIC,
					POST_AUTHOR,
					POST_DATE,
					POST_EDITED,
					POST_TEXT,
					POST_FIGURE,
					POST_FIGTYPE,
					POST_FLAGS),
				FROM + POSTS,
				whereSql_vals.getSql(),
				ORDER_BY + POST_ID, reverse ? DESCENDING : "");
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, whereSql_vals);
		debugPrint(SELF, sql_vals);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		final List<ForumPost> posts = new ArrayList<ForumPost>();
		try { 
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final int postId = rs.getInt(POST_ID);
				final int topicId = rs.getInt(POST_TOPIC);
				final String author = rs.getString(POST_AUTHOR);
				final Date created = toDate(rs.getString(POST_DATE));
				final Date edited = toDate(rs.getString(POST_EDITED));
				final int postFlags = rs.getInt(POST_FLAGS);
				final String text = rs.getString(POST_TEXT);
				final int figType = 
						Utils.indexOf(DBVALUES, rs.getString(POST_FIGTYPE));
				String figure = rs.getString(POST_FIGURE);
				if (figType == IMAGE) {
					final int figId = MathUtils.parseInt(figure);
					final ImageRW imgReader = new ImageRW(con);
					final String ext = imgReader.getExtension(figId);
					figure = getPostImageFileName(postId, figId, ext);
				} // if IMAGE
				posts.add(new ForumPost(postId, topicId, author, created, 
						edited, text, figure == null ? "" : figure, figType, 
						postFlags));
			} // while there are more results
		} catch (SQLException e) {
			throw e;
		} finally {
			closeStmtAndRs(stmt, rs);
		}
		return posts.toArray(new ForumPost[posts.size()]);
	} // getPosts(Connection, SQLWithQMarks, boolean)

	/** Gets all posts within topics that are in a course and that contain an 
	 * expression.
	 * @param	courseId	course ID number
	 * @param	searchExp	word fragment, word, phrase, simple boolean
	 * expression, or regular expression for which to search forum posts
	 * @return	linked map of posts that contain the expression, keyed by 
	 * topic; topics will be retrieved in reverse chronological order of 
	 * their most recent posts
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<ForumTopic, ArrayList<ForumPost>> getPosts(
			int courseId, String searchExp) throws DBException {
		final String SELF = "ForumRW.getPosts: ";
		final Map<ForumTopic, ArrayList<ForumPost>> postsByTopics =
				new LinkedHashMap<ForumTopic, ArrayList<ForumPost>>();
		if (Utils.isEmpty(searchExp)) return postsByTopics;
		final StringBuilder postBld = getBuilder(
				WHERE + POST_TOPIC + IN, parens(
					SELECT + FTOPIC_ID 
					+ FROM + FORUM_TOPICS
					+ WHERE + FTOPIC_COURSE + EQUALS + QMARK));
		final String searchExpMod = searchExp.trim();
		final String[] searchExpsAnd = searchExpMod.split(" and ");
		if (searchExpsAnd.length > 1) {
			for (final String searchExpAnd : searchExpsAnd) {
 				appendTo(postBld, AND, regexp_instr(POST_TEXT, 
						quotes(searchExpAnd.trim())), IS_POSITIVE);
			} // for each searchExpAnd
		} else {
			final String[] searchExpsOr = searchExpMod.split(" or ");
			final StringBuilder orBld = new StringBuilder();
			for (final String searchExpOr : searchExpsOr) {
				if (orBld.length() > 0) orBld.append(OR);
 				appendTo(orBld, regexp_instr(POST_TEXT, 
						quotes(searchExpOr.trim())), IS_POSITIVE);
			} // for each searchExpOr
			if (searchExpsOr.length > 1) parens(orBld);
 			appendTo(postBld, AND, orBld);
		} // if expression contains " and "
		final SQLWithQMarks sql_vals = new SQLWithQMarks(postBld, courseId);
		final ForumPost[] posts = getPosts(sql_vals, REVERSE);
		final List<Integer> topicIdsList = new ArrayList<Integer>();
		int postNum = 0;
		final TimeZone tz = TimeZone.getDefault();
		for (final ForumPost post : posts) {
			final Integer topicIdObj = Integer.valueOf(post.getTopicId());
			if (!topicIdsList.contains(topicIdObj)) {
				topicIdsList.add(topicIdObj);
			} // if topic is not already in the list
		} // for each post
		final ForumTopic[] topics = getTopics(topicIdsList);
		final int[] topicIds = new int[topics.length];
		int topicNum = 0;
		for (final ForumTopic topic : topics) {
			topicIds[topicNum++] = topic.getId();
		}
		topicNum = 0;
		postNum = 0;
		for (final ForumPost post : posts) {
			final int topicId = post.getTopicId();
			final int posn = Utils.indexOf(topicIds, topicId);
			if (posn >= 0) {
				final ForumTopic topic = topics[posn];
				debugPrint(SELF + "post ", ++postNum, ": postId ", 
						post.getId(), ", date created = ", 
						post.getDateCreated(tz), " associated with topic ", 
						++topicNum, ": topicId ", topicId, 
						", date created = ", topic.getDateCreated(tz),
						", date last changed = ", 
						topic.getDateLastChanged(tz));
				ArrayList<ForumPost> topicPosts = postsByTopics.get(topic);
				if (topicPosts == null) {
					topicPosts = new ArrayList<ForumPost>();
					postsByTopics.put(topic, topicPosts);
				} // if this is a new list
				topicPosts.add(post);
			} // if the topic was found
		} // for each post
		return postsByTopics;
	} // getPosts(int, String)

	/** Gets the most recent post in a topic.
	 * @param	topicId	ID number of the topic
	 * @return	the most recent post
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static ForumPost getLatestPost(int topicId) throws DBException {
		final String where = toString(
				WHERE + POST_ID + EQUALS, parensBuild(
					SELECT, max(POST_ID), 
					FROM + POSTS
					+ WHERE + POST_TOPIC + EQUALS + QMARK));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(where, topicId);
		final ForumPost[] posts = getPosts(sql_vals);
		return (!Utils.isEmpty(posts) ? posts[0] : null);
	} // getLatestPost(int)

/* *********** Methods to add and edit topics and posts ***************/

	/** Adds a new topic to a course's forum; if it is sticky, sets all students
	 * to watch it. 
	 * @param	topic	the topic to add to the forum
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void addTopic(ForumTopic topic) throws DBException {
		final String SELF = "ForumRW.addTopic: ";
		Connection con = null;
		try { 
			con = getPoolConnection();
			final int topicId = nextSequence(con, FTOPIC_SEQ);
			int[] linkedAssgtQ = topic.getLinkedAssgtQ();
			if (Utils.isEmpty(linkedAssgtQ)) linkedAssgtQ = new int[2];
			final String[] fields = new String[] {
					FTOPIC_ID,
					FTOPIC_COURSE,
					FTOPIC_CREATOR,
					FTOPIC_DATE,
					FTOPIC_TITLE, // CLOB
					FTOPIC_STICKY,
					FTOPIC_HWID,
					FTOPIC_QID};
			final String qry = 
					getInsertIntoValuesQMarksSQL(FORUM_TOPICS, fields);
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
					topicId,
					topic.getCourseId(),
					topic.getCreatorId(),
					dateToString(new Date()),
					topic.getTitle(),
					topic.isSticky() ? 'Y' : 'N',
					linkedAssgtQ[0],
					linkedAssgtQ[1]);
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
			topic.setId(topicId);
			if (topic.isSticky()) {
				setAllStudentsToWatchStickyTopic(con, topicId);
			} // if topic is sticky
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // addTopic(ForumTopic)

	/** Adds a new post to a course's forum. 
	 * @param	post	the post to add to the forum
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void addPost(ForumPost post) throws DBException {
		final String SELF = "ForumRW.addPost: ";
		Connection con = null;
		try { 
			con = getPoolConnection();
			con.setAutoCommit(false);
			final int postId = nextSequence(con, POST_SEQ);
			String figure = post.getFigure();
			if (post.figureIsImage()) {
				debugPrint(SELF + "adding image ", figure);
				final int imageId = addImage(con, postId, figure);
				// store figure ID in figure field of DB
				figure = String.valueOf(imageId);
			} // if image try
			final int figType = post.getFigureType();
			final String[] fields = new String[] {
					POST_ID,
					POST_TOPIC,
					POST_AUTHOR,
					POST_DATE,
					POST_EDITED,
					POST_FLAGS,
					POST_TEXT, // CLOB
					POST_FIGURE, // CLOB
					POST_FIGTYPE};
			final String qry = getInsertIntoValuesQMarksSQL(POSTS, fields);
			final String dateStr = dateToString(new Date());
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
					postId,
					post.getTopicId(),
					post.getAuthorId(),
					dateStr,
					dateStr,
					post.getFlags(),
					post.getText(), 
					figure,
					figType != UNKNOWN ? DBVALUES[figType] : "");
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
			post.setId(postId);
			setWatched(post.getAuthorId(), post.getTopicId(), true);
			con.commit();
		} catch (FileNotFoundException e) { 
			Utils.alwaysPrint(SELF + "FileNotFoundException "
					+ "when trying to write forum post image to DB.");
			throw new DBException(e.getMessage());
		} catch (IOException e) {
			Utils.alwaysPrint(SELF + "IOException "
					+ "when trying to write forum post image to DB.");
			throw new DBException(e.getMessage());
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // addPost(ForumPost)

	/** Changes the text and figure of a post. 
	 * @param	post	the new post
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void editPost(ForumPost post) throws DBException {
		final String SELF = "ForumRW.editPost: ";
		Connection con = null;
		try {
			final int postId = post.getId();
			final ForumPost oldPost = getPost(postId);
			String figure = post.getFigure();
			final boolean changedFig = !figure.equals(oldPost.getFigure());
			debugPrint(SELF + "figure has ", changedFig 
					? "" : "not ", "changed.");
			con = getPoolConnection();
			con.setAutoCommit(false);
			if (oldPost.figureIsImage() && changedFig) {
				final String posts = WHERE + POST_ID + EQUALS + QMARK;
				deletePostImages(con, new SQLWithQMarks(posts, postId));
			} // if old post has an image for its figure
			if (post.figureIsImage() && changedFig) {
				final int imageId = addImage(con, postId, figure);
				// store figure ID in figure field of DB
				figure = String.valueOf(imageId);
			} // if image try
			final int figType = post.getFigureType();
			final String dbFigType = 
					(figType != UNKNOWN ? DBVALUES[figType] : "");
			String[] fields;
			final SQLWithQMarks sql_vals = new SQLWithQMarks();
			final String dateStr = dateToString(new Date());
			if (changedFig) {
				fields = new String[] {
						POST_EDITED,
						POST_FLAGS,
						POST_TEXT, // CLOB
						POST_FIGURE, // CLOB
						POST_FIGTYPE};
				sql_vals.addValues(
						dateStr,
						post.getFlags(),
						post.getText(), 
						figure,
						dbFigType);
			} else {
				fields = new String[] {
						POST_EDITED,
						POST_FLAGS,
						POST_TEXT}; // CLOB
				sql_vals.addValues(
						dateStr,
						post.getFlags(),
						post.getText());
			} // if changed figure
			sql_vals.setSql(
					UPDATE + POSTS + SET, equalsJoinQMarksArr(fields),
					WHERE + POST_ID + EQUALS + QMARK);
			sql_vals.addValue(postId);
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
			con.commit();
		} catch (FileNotFoundException e) { 
			Utils.alwaysPrint(SELF + "FileNotFoundException "
					+ "when trying to write forum post image to DB.");
			throw new DBException(e.getMessage());
		} catch (IOException e) {
			Utils.alwaysPrint(SELF + "IOException "
					+ "when trying to write forum post image to DB.");
			throw new DBException(e.getMessage());
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // editPost(ForumPost)

	/** Gets a filename for a post image and adds it to the database.
	 * @param	con	database connection
	 * @param	postId	ID of the post
	 * @param	currentLocation	original name of the file containing the image
	 * @return	new ID number of the new image
	 * @throws	SQLException	if there's a problem writing to the database
	 * @throws	FileNotFoundException	if the file containing the image can't be
	 * found
	 * @throws	IOException	if the file containing the image can't be read
	 */
	private static int addImage(Connection con, int postId, 
			String currentLocation) 
			throws SQLException, FileNotFoundException, IOException {
		final String SELF = "ForumRW.addImage: ";
		final ImageRW imgWriter = new ImageRW(con);
		final int imageId = imgWriter.getNewImageId();
		final String ext = Utils.getExtension(currentLocation);
		final String fileName = getPostImageFileName(postId, imageId, ext);
		debugPrint(SELF + "getting file from ", currentLocation, 
				" for new image with ID ", imageId, 
				" and storing it in new file ", fileName);
		imgWriter.addImage(imageId, currentLocation, fileName);
		return imageId;
	} // addImage(Connection, int, String)

	/** Sets the stickiness of a forum topic; if it is sticky, sets all students
	 * to watch it. 
	 * @param	topicId	unique ID of the forum topic
	 * @param	title	new title of the topic
	 * @param	stickiness	the new stickiness of the topic
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setTopic(int topicId, String title, boolean stickiness)
			throws DBException {
		final String SELF = "ForumRW.setTopic: ";
		final String qry = toString(
				UPDATE + FORUM_TOPICS + SET,
				equalsJoin(
					FTOPIC_STICKY, stickiness ? Y : N,
					FTOPIC_TITLE, QMARK), 
				WHERE + FTOPIC_ID + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				title, 
				topicId); 
		debugPrint(SELF, sql_vals);
		Connection con = null;
		try { 
			con = getPoolConnection();
			tryUpdate(con, sql_vals); 
			if (stickiness) {
				setAllStudentsToWatchStickyTopic(con, topicId);
			} // if topic is sticky
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // setTopic(int, String, boolean)

/* **** Methods to delete topics and posts *****/

	/** Deletes a topic from a course's forum. 
	 * @param	topicId	ID number of the topic to delete
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void deleteTopic(int topicId) throws DBException {
		deleteTopics(new int[] {topicId});
	} // deleteTopic(int)

	/** Deletes topics from a course's forum. 
	 * @param	topicIds	ID numbers of the topics to delete
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void deleteTopics(int[] topicIds) throws DBException {
		final String SELF = "ForumRW.deleteTopics: ";
		Connection con = null;
		if (!Utils.isEmpty(topicIds)) try { 
			con = getPoolConnection();
			con.setAutoCommit(false);
			// final StringBuilder topicsBld = parens(topicIds);
			final StringBuilder parensQMarks = parensQMarks(topicIds);
			final String where = toString(
					WHERE + POST_TOPIC + IN, parensQMarks);
			SQLWithQMarks sql_vals = new SQLWithQMarks(where, topicIds);
			final List<String> imagesToDeleteFromDisk = 
					deletePosts(con, sql_vals);
			final String[] qries = new String[] {
					toString(DELETE_FROM + WATCHED_TOPICS
						+ WHERE + WATCHED_ID + IN, parensQMarks),
					toString(DELETE_FROM + FORUM_TOPICS
						+ WHERE + FTOPIC_ID + IN, parensQMarks)};
			for (final String qry : qries) {
				sql_vals = new SQLWithQMarks(qry, 
						topicIds);
				debugPrint(SELF, sql_vals);
				tryUpdate(con, sql_vals); 
			} // for each query
			con.commit();
			ImageRW.deleteImagesFromDisk(imagesToDeleteFromDisk);
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // deleteTopics(int[])

	/** Deletes all topics from a course's forum. 
	 * @param	courseId	ID number of the course whose topics to delete
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void deleteAllTopics(int courseId) throws DBException {
		deleteAllTopics(new int[] {courseId});
	} // deleteAllTopics(int)

	/** Deletes all topics from courses' forums. 
	 * @param	courseIds	ID numbers of the courses whose topics to delete
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void deleteAllTopics(int[] courseIds) throws DBException {
		Connection con = null;
		if (!Utils.isEmpty(courseIds)) try { 
			con = getPoolConnection();
			con.setAutoCommit(false);
			final SQLWithQMarks sql_vals = 
					new SQLWithQMarks(parensQMarks(courseIds), courseIds);
			final List<String> imagesToDeleteFromDisk = 
					deleteAllTopics(con, sql_vals);
			con.commit();
			ImageRW.deleteImagesFromDisk(imagesToDeleteFromDisk);
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // deleteAllTopics(int[])

	/** Deletes all topics from courses' forums. 
	 * @param	con	database connection
	 * @param	crsIdsSql_vals	SQL for selecting the courses whose topics to 
	 * delete, and values to substitute for question marks in the SQL
	 * @return	list of absolute names of image files that need to be deleted
	 * from disk after commit
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	static List<String> deleteAllTopics(Connection con, 
			SQLWithQMarks crsIdsSql_vals) throws SQLException {
		final String SELF = "ForumRW.deleteAllTopics: ";
		final StringBuilder whereCrsBld = getBuilder( 
				WHERE + FTOPIC_COURSE + IN, parens(crsIdsSql_vals.getSql()));
		final StringBuilder postsBld = getBuilder(
				WHERE + POST_TOPIC + IN, parensBuild(
					SELECT + FTOPIC_ID
					+ FROM + FORUM_TOPICS, whereCrsBld));
		final SQLWithQMarks topicIdsSql_vals = 
				new SQLWithQMarks(postsBld, crsIdsSql_vals);
		final List<String> imagesToDeleteFromDisk = 
				deletePosts(con, topicIdsSql_vals);
		final String[] qries = new String[] {
				toString(DELETE_FROM + WATCHED_TOPICS
					+ WHERE + WATCHED_ID + IN, parensBuild(
						SELECT + FTOPIC_ID
						+ FROM + FORUM_TOPICS, whereCrsBld)),
				toString(DELETE_FROM + FORUM_TOPICS, whereCrsBld)};
		for (final String qry : qries) {
			final SQLWithQMarks sql_vals = 
					new SQLWithQMarks(qry, crsIdsSql_vals);
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
		} // for each query
		return imagesToDeleteFromDisk;
	} // deleteAllTopics(int[])

	/** Deletes all links from a topic to an assignment; the topic remains in
	 * the forum.
	 * @param	con	database connection
	 * @param	hwIdsSql_vals	SQL for selecting the assignments whose 
	 * courses' topics to delete, and values to substitute for question marks 
	 * in the SQL
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	static void deleteTopicLinks(Connection con, SQLWithQMarks hwIdsSql_vals) 
			throws SQLException {
		final String SELF = "ForumRW.deleteTopicLinks: ";
		final String qry = toString(
				UPDATE + FORUM_TOPICS + SET,
				equalsJoin(
					FTOPIC_HWID, 0,
					FTOPIC_QID, 0), 
				WHERE + FTOPIC_HWID + IN, hwIdsSql_vals.getSql());
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, hwIdsSql_vals);
		debugPrint(SELF, sql_vals);
		tryUpdate(con, sql_vals); 
	} // deleteTopicLinks(Connection, SQLWithQMarks)

	/** Deletes links from a topic to an assignment and question when the
	 * question is no longer in the assignment; the topic remains in the forum.
	 * @param	con	database connection
	 * @param	hwId	assignment ID number
	 * @param	exQIds	ID numbers of questions no longer in the assignment;
	 * if null or empty, remove links to all questions in assignment
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	static void deleteTopicLinks(Connection con, int hwId, 
			List<Integer> exQIds) throws SQLException {
		final String SELF = "ForumRW.deleteTopicLinks: ";
		final boolean noExQIds = Utils.isEmpty(exQIds);
		if (noExQIds) return; 
		final String update = toString(
				UPDATE + FORUM_TOPICS + SET,
				equalsJoin(
					FTOPIC_HWID, 0,
					FTOPIC_QID, 0),
				WHERE + FTOPIC_HWID + EQUALS + QMARK // hwId
 				+ AND + FTOPIC_QID + IN, parensQMarks(exQIds));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(update, 
				hwId);
		sql_vals.addValues(exQIds);
		debugPrint(SELF, sql_vals);
		tryUpdate(con, sql_vals); 
	} // deleteTopicLinks(Connection, int, List<Integer>)

	/** Deletes a post from a course's forum. 
	 * @param	postId	the post to delete
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void deletePost(int postId) throws DBException {
		deletePosts(new int[] {postId});
	} // deletePost(int)

	/** Deletes posts from a course's forum. 
	 * @param	postIds	which posts to delete
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void deletePosts(int[] postIds) throws DBException {
		final String SELF = "ForumRW.deletePosts: ";
		Connection con = null;
		if (!Utils.isEmpty(postIds)) try {
			// final ForumPost[] oldPosts = getPosts(postIds); // not used subsequently
			con = getPoolConnection();
			con.setAutoCommit(false);
			final String where = toString(
					WHERE + POST_ID + IN, parensQMarks(postIds));
			final SQLWithQMarks sql_vals = new SQLWithQMarks(where, postIds);
			final List<String> imagesToDeleteFromDisk = 
					deletePosts(con, sql_vals);
			con.commit();
			ImageRW.deleteImagesFromDisk(imagesToDeleteFromDisk);
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		} // try
	} // deletePosts(int[])

	/** Deletes posts from a course's forum. 
	 * @param	con	database connection
	 * @param	postsSql_vals	SQL for selecting the posts to 
	 * delete, and values to substitute for question marks in the SQL
	 * @return	list of absolute names of image files that need to be deleted
	 * from disk after commit
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private static List<String> deletePosts(Connection con, 
			SQLWithQMarks postsSql_vals) throws SQLException {
		final String SELF = "ForumRW.deletePosts: ";
		final List<String> imagesToDeleteFromDisk =
				deletePostImages(con, postsSql_vals);
		String qry = toString(
				DELETE_FROM + POSTS, 
				postsSql_vals.getSql());
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry);
		sql_vals.addValuesFrom(postsSql_vals);
		debugPrint(SELF, sql_vals);
		tryUpdate(con, sql_vals);
		qry = toString(
				DELETE_FROM + WATCHED_TOPICS 
				+ WHERE + WATCHED_ID + NOT + IN,
					parens(SELECT_UNIQUE + POST_TOPIC + FROM + POSTS));
		debugPrint(SELF, qry);
		tryUpdate(con, qry);
		qry = toString(
				DELETE_FROM + FORUM_TOPICS 
				+ WHERE + FTOPIC_ID + NOT + IN,
					parens(SELECT_UNIQUE + POST_TOPIC + FROM + POSTS));
		debugPrint(SELF, qry);
		tryUpdate(con, qry);
		return imagesToDeleteFromDisk;
	} // deletePosts(Connection, SQLWithQMarks)

	/** Deletes images of posts that are going to be deleted. 
	 * @param	con	database connection
	 * @param	sql_vals	SQL for selecting the posts to 
	 * delete, and values to substitute for question marks in the SQL
	 * @return	list of absolute names of image files that need to be deleted
	 * from disk after commit
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private static List<String> deletePostImages(Connection con, 
			SQLWithQMarks sql_vals) throws SQLException {
		final String SELF = "ForumRW.deletePostImages: ";
		final String where = sql_vals.getSql();
		final StringBuilder imgIdBld = getBuilder(
				SELECT, toNumber(clobToString(POST_FIGURE)),
				FROM + POSTS, 
				where,
				!Utils.isEmpty(where) ? AND : WHERE, POST_FIGTYPE + EQUALS, 
					quotes(DBVALUES[IMAGE]));
		final ImageRW imgWriter = new ImageRW(con);
		return imgWriter.deleteImages(new SQLWithQMarks(imgIdBld, sql_vals));
	} // deletePostImages(Connection, SQLWithQMarks)

	/** Deletes images from forum posts that have not been edited in a year or
	 * more. 
	 * @return	the number of images removed
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static int deleteYearOldImages() throws DBException {
		final String SELF = "ForumRW.deleteYearOldImages: ";
		Connection con = null;
		int numRemoved = 0;
		try {
			con = getPoolConnection();
			numRemoved = getNumPostImages(con);
			final ImageRW imgWriter = new ImageRW(con);
			final SQLWithQMarks sql_vals = new SQLWithQMarks(toString(
					SELECT, toNumber(clobToString(POST_FIGURE)), 
					FROM + POSTS
					+ WHERE + POST_FIGTYPE + EQUALS, quotes(DBVALUES[IMAGE]),
					AND + POST_EDITED + LESS_THAN, getOneYearAgo()));
			imgWriter.deleteImages(sql_vals);
			numRemoved -= getNumPostImages(con);
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		} // try
		debugPrint(SELF + "removed ", numRemoved, " post image(s).");
		return numRemoved;
	} // deleteYearOldImages()

	/** Gets the number of post images that are a year or more old.
	 * @return	the number of post images that are a year or more old
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static int getNumYearOldPostImages() throws DBException {
		int numImages = 0;
		Connection con = null;
		try {
			con = getPoolConnection();
			numImages = getNumYearOldPostImages(con);
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		} // try
		return numImages;
	} // getNumYearOldPostImages()

	/** Gets the number of post images.
	 * @return	the number of post images
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static int getNumPostImages() throws DBException {
		int numImages = 0;
		Connection con = null;
		try {
			con = getPoolConnection();
			numImages = getNumPostImages(con);
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		} // try
		return numImages;
	} // getNumPostImages()

/* *************** Methods for watching forum topics *****************/

	/** Finds out which topics in an array are being watched by a student, and 
	 * sets a flag in each topic to indicate it.
	 * @param	studentId	login ID of the student
	 * @param	topics	array of topics
	 * @throws	DBException	if there's a problem reading from the database
	 */
	public static void getWatched(String studentId, ForumTopic[] topics) 
			throws DBException {
		final String SELF = "ForumRW.getWatched: ";
		if (Utils.isEmpty(topics)) return;
		final Map<Integer, ForumTopic> topicsById = 
				new HashMap<Integer, ForumTopic>();
		final List<Integer> topicIds = new ArrayList<Integer>();
		for (final ForumTopic topic : topics) {
			final Integer topicIdObj = Integer.valueOf(topic.getId());
			topicsById.put(topicIdObj, topic); 
			topicIds.add(topicIdObj);
		} // for each topic
		final String qry = toString(
				SELECT + WATCHED_ID + FROM + WATCHED_TOPICS
				+ WHERE + WATCHED_STUDENT + EQUALS + QMARK
				+ AND + WATCHED_ID + IN, parensQMarks(topicIds));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				studentId,
				topicIds);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try { 
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final int watchedId = rs.getInt(WATCHED_ID);
				final ForumTopic topic = 
						topicsById.get(Integer.valueOf(watchedId));
				topic.setWatched(true);
			} // while more results
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
	} // getWatched(String, ForumTopic[])

	/** Gets the email addresses to send texts to students watching this topic
	 * and TAs, instructors, and coinstructors of the topic's course, excluding 
	 * the person who made the most recent post in the topic.
	 * @param	topicId	the topic's ID number
	 * @param	postAuthorId	the author of the post
	 * @return	array of email addresses for sending texts
	 * @throws	DBException	if there's a problem reading from the database
	 */
	public static String[] getWatcherTextMessageEmails(int topicId,
			String postAuthorId) throws DBException {
		final String SELF = "ForumRW.getWatcherTextMessageEmails: ";
		final String qry = toString(
				SELECT + USER_TXT_MSG + FROM + USERS
				+ WHERE + USER_TXT_MSG + IS_NOT_NULL
				+ AND + USER_ID + NOT_EQUALS + QMARK // postAuthorId
				+ AND, parensBuild(
					USER_ID + IN, parens( // students watching this topic
						SELECT + WATCHED_STUDENT + FROM + WATCHED_TOPICS
						+ WHERE + WATCHED_ID + EQUALS + QMARK), // topicId
					OR + USER_ID + IN, parensBuild( // TAs
						SELECT + ENRL_STUDENT
						+ FROM + ENROLLMENT
						+ WHERE + ENRL_IS_TA + EQUALS + Y
						+ AND + ENRL_COURSE + IN, parens(
							SELECT + FTOPIC_COURSE + FROM + FORUM_TOPICS
							+ WHERE + FTOPIC_ID + EQUALS + QMARK)), // topicId
					OR + USER_ID + IN, parensBuild( // instructor
						SELECT + CRS_INSTRUCTOR
						+ FROM + COURSES
						+ WHERE + CRS_ID + IN, parens(
							SELECT + FTOPIC_COURSE + FROM + FORUM_TOPICS
							+ WHERE + FTOPIC_ID + EQUALS + QMARK)), // topicId
					OR + USER_ID + IN, parensBuild( // coinstructors
						SELECT + COINSTR_INSTR
						+ FROM + COINSTRUCTORS
						+ WHERE + COINSTR_CRS + IN, parens(
							SELECT + FTOPIC_COURSE + FROM + FORUM_TOPICS
							+ WHERE + FTOPIC_ID + EQUALS + QMARK)))); // topicId
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				postAuthorId,
				topicId,
				topicId,
				topicId,
				topicId);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		final List<String> emails = new ArrayList<String>();
		try { 
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				emails.add(rs.getString(USER_TXT_MSG));
			} // while there are more results
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		debugPrint(SELF + "got email addresses for texts for watchers "
				+ "and instructors of topic ", topicId, 
				" other than post author ", postAuthorId, ": ", emails);
		return emails.toArray(new String[emails.size()]);
	} // getWatcherTextMessageEmails(int, String)

	/** Sets a topic to be watched or no longer to be watched by a user, if a 
	 * student.
	 * @param	userId	the user's login ID
	 * @param	topicId	the ID of the topic to be watched or no longer watched
	 * @param	watch	whether to start or stop watching the topic
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setWatched(String userId, int topicId, boolean watch) 
			throws DBException {
		final String SELF = "ForumRW.setWatched: ";
		final User user = UserRead.getUser(userId);
		if (user.getRole() == STUDENT) {
			String qry;
			if (watch) {
				final String[] fields = new String[] {
						WATCHED_ID,
						WATCHED_STUDENT};
				qry = getInsertIntoValuesQMarksSQL(WATCHED_TOPICS, fields);
			} else {
				qry = DELETE_FROM + WATCHED_TOPICS
						+ WHERE + WATCHED_ID + EQUALS + QMARK
						+ AND + WATCHED_STUDENT + EQUALS + QMARK;
			}
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
					topicId,
					userId);
			debugPrint(SELF, sql_vals);
			Connection con = null;
			try { 
				con = getPoolConnection();
				tryUpdate(con, sql_vals);
			} catch (SQLException e) {
				if (!watch) throw new DBException(e.getMessage());
				// expect exception for insert if record already exists
			} finally {
				closeConnection(con);
			}
		} // if user is a student
	} // setWatched(String, int, boolean)

	/** Changes watch flags in a course for students being promoted to or
	 * demoted from TA. Called by EnrollmentRW.modifyTAs().
	 * @param	con	database connection
	 * @param	courseId	the course
	 * @param	studentIds	login IDs of students who are being promoted to or
	 * demoted from TA
	 * @param	promote	whether TAs are being promoted or demoted
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	static void changeTAWatching(Connection con, int courseId, 
			List<String> studentIds, boolean promote) throws SQLException {
		if (promote) ForumRW.unwatchTopics(con, courseId, studentIds);
		else ForumRW.setStudentsToWatchStickyTopics(con, courseId, studentIds);
	} // changeTAWatching(Connection, int, List<String>, boolean)

	/** Turns off all forum topic watch flags for the given students in a course.
	 * @param	con	database connection
	 * @param	courseId	the course
	 * @param	studentIdsArr	login IDs of students who should no longer
	 * watch topics in a course
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	static void unwatchTopics(Connection con, int courseId, 
			String[] studentIdsArr) throws SQLException {
		ForumRW.unwatchTopics(con, courseId, 
				new ArrayList<String>(Arrays.asList(studentIdsArr)));
	} // unwatchTopics(Connection, int, String[])

	/** Turns off all forum topic watch flags for the given students in a course.
	 * @param	con	database connection
	 * @param	courseId	the course
	 * @param	studentIds	login IDs of students who should no longer
	 * watch topics in a course
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private static void unwatchTopics(Connection con, int courseId, 
			List<String> studentIds) throws SQLException {
		final String SELF = "ForumRW.unwatchTopics: ";
		if (!Utils.isEmpty(studentIds)) {
			final String qry = toString(
					DELETE_FROM + WATCHED_TOPICS
					+ WHERE + WATCHED_STUDENT + IN, parensQMarks(studentIds),
					AND + WATCHED_ID + IN, parens(
						SELECT + FTOPIC_ID + FROM + FORUM_TOPICS
						+ WHERE + FTOPIC_COURSE + EQUALS + QMARK));
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
					studentIds,
					courseId);
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
		} // if there are students
	} // unwatchTopics(Connection, int, List<String>)

	/** Sets all sticky topics in specified courses to be watched by the given 
	 * student. Called from EnrollmentRW.enrollInQualified().
	 * @param	con	database connection
	 * @param	courseIds	the courses of the topics
	 * @param	studentId	login ID of student who should watch the sticky
	 * topics in a course
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	public static void setStudentToWatchStickyTopics(Connection con, 
			List<Integer> courseIds, String studentId) throws SQLException {
		final List<String> studentIds = new ArrayList<String>();
		studentIds.add(studentId);
		ForumRW.setStudentsToWatchStickyTopics(con, courseIds, studentIds);
	} // setStudentToWatchStickyTopics(Connection, List<Integer>, String)

	/** Sets all sticky topics in a course to be watched by the given student.
	 * Called from EnrollmentRW.enrollInCourse().
	 * @param	con	database connection
	 * @param	courseId	the course of the topic
	 * @param	studentIdsArr	login IDs of students who should watch the sticky
	 * topics in a course
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	static void setStudentsToWatchStickyTopics(Connection con, 
			int courseId, String[] studentIdsArr) throws SQLException {
		ForumRW.setStudentsToWatchStickyTopics(con, courseId, 
				new ArrayList<String>(Arrays.asList(studentIdsArr)));
	} // setStudentsToWatchStickyTopics(Connection, int, String[])

	/** Sets all sticky topics in a course to be watched by the given student.
	 * Called from EnrollmentRW.enroll() and above.
	 * @param	con	database connection
	 * @param	courseId	the course of the topic
	 * @param	studentIds	login IDs of students who should watch the sticky
	 * topics in a course
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	static void setStudentsToWatchStickyTopics(Connection con, 
			int courseId, List<String> studentIds) throws SQLException {
		final List<Integer> courseIds = new ArrayList<Integer>();
		courseIds.add(courseId);
		ForumRW.setStudentsToWatchStickyTopics(con, courseIds, studentIds);
	} // setStudentsToWatchStickyTopics(Connection, int, List<String>)

	/** Sets all sticky topics in courses to be watched by the given students.
	 * @param	con	database connection
	 * @param	courseIds	list of courses
	 * @param	studentIds	login IDs of students who should watch the sticky
	 * topics in a course
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private static void setStudentsToWatchStickyTopics(Connection con, 
			List<Integer> courseIds, List<String> studentIds) 
			throws SQLException {
		final String SELF = "ForumRW.setStudentsToWatchStickyTopics: ";
		final List<Integer> topicIds = getStickyTopicIds(con, courseIds);
		final String[] fields = new String[] {
				WATCHED_ID,
				WATCHED_STUDENT};
		final String qry = getInsertIntoValuesQMarksSQL(WATCHED_TOPICS, fields);
		debugPrint(SELF, qry); 
		PreparedStatement stmt = null;
		try { 
			stmt = con.prepareStatement(qry);
			int batchNum = 0;
			for (final String studentId : studentIds) {
				for (final Integer topicId : topicIds) {
					final StringBuilder joinedValues = setValues(stmt, 
							topicId, 
							studentId);
					debugPrint(SELF, "batch ", batchNum + 1, ": ", joinedValues); 
					stmt.addBatch();
					batchNum++;
					if (batchNum % 100 == 0) {
						stmt.executeBatch();
						stmt.clearBatch();
					} // if time to submit
				} // for each topic to be watched
			} // for each student who will watch
			if (batchNum % 100 != 0) {
				stmt.executeBatch();
			} // if need to submit
		} finally {
			closeConnection(null, stmt, null);
		}
	} // setStudentsToWatchStickyTopics(Connection, List<Integer>, List<String>)

	/** Sets a sticky topic to be watched by all students in a course, 
	 * excluding TAs.
	 * @param	con	database connection
	 * @param	topicId	the ID of the topic to be watched
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private static void setAllStudentsToWatchStickyTopic(Connection con, 
			int topicId) throws SQLException {
		final String SELF = "ForumRW.setAllStudentsToWatchStickyTopic: ";
		final String usersSql = toString(
				SELECT + ENRL_STUDENT
				+ FROM + ENROLLMENT
				+ WHERE + ENRL_COURSE + IN, parens(
					SELECT + FTOPIC_COURSE + FROM + FORUM_TOPICS
					+ WHERE + FTOPIC_ID + EQUALS + QMARK),
				AND, parensBuild(ENRL_IS_TA + IS_NULL
					+ OR + ENRL_IS_TA + EQUALS + N));
		SQLWithQMarks sql_vals = new SQLWithQMarks(usersSql, topicId);
		final List<EnrollmentData> enrolledStudents = 
				EnrollmentRW.getEnrolledStudents(con, sql_vals);
		PreparedStatement stmt = null;
		try { 
			String qry = DELETE_FROM + WATCHED_TOPICS
					+ WHERE + WATCHED_ID + EQUALS + QMARK;
			sql_vals = new SQLWithQMarks(qry, topicId);
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
			final String[] fields = new String[] {
					WATCHED_ID,
					WATCHED_STUDENT};
			qry = getInsertIntoValuesQMarksSQL(WATCHED_TOPICS, fields);
			debugPrint(SELF, qry);
			stmt = con.prepareStatement(qry);
			int batchNum = 0;
			for (final EnrollmentData student : enrolledStudents) {
				final StringBuilder joinedValues = setValues(stmt, 
						topicId, 
						student.getUserId());
				debugPrint(SELF, "batch ", batchNum + 1, ": ", joinedValues); 
				stmt.addBatch();
				batchNum++;
				if (batchNum % 100 == 0) {
					stmt.executeBatch();
					stmt.clearBatch();
				} // if time to submit
			} // for each student to be blocked
			if (batchNum % 100 != 0) {
				stmt.executeBatch();
			} // if need to submit
		} finally {
			closeConnection(null, stmt, null);
		}
	} // setAllStudentsToWatchStickyTopic(Connection, int)

/* **************** Methods to block users ********************/

	/** Blocks or unblocks users from a course's forums.
	 * @param	courseId	ID number of the course
	 * @param	accessIds	login IDs of students whose access to change
	 * @param	block	when true, block the students
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void blockUsers(int courseId, List<String> accessIds,
			boolean block) throws DBException {
		final String SELF = "ForumRW.blockUsers: ";
		if (Utils.isEmpty(accessIds)) return; 
		debugPrint(SELF + "courseId = ", courseId, ", block = ",
				block, ", accessIds = ", accessIds);
		final String update = toString(
				DELETE_FROM + BLOCKED
				+ WHERE + BLOCKED_COURSE + EQUALS + QMARK
				+ AND + BLOCKED_USER + IN, parensQMarks(accessIds));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(update, 
				courseId,
				accessIds);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		try { 
			con = getPoolConnection();
			con.setAutoCommit(false);
			tryUpdate(con, sql_vals);
			if (block) {
				final String[] fields = new String[] {
						BLOCKED_COURSE,
						BLOCKED_USER};
				final String qry = 
						getInsertIntoValuesQMarksSQL(BLOCKED, fields);
				debugPrint(SELF, qry); 
				stmt = con.prepareStatement(qry);
				int batchNum = 0;
				for (final String accessId : accessIds) {
					final StringBuilder joinedValues = setValues(stmt, 
							courseId, 
							accessId);
					debugPrint(SELF, "batch ", batchNum + 1, ": ", joinedValues); 
					stmt.addBatch();
					batchNum++;
					if (batchNum % 100 == 0) {
						stmt.executeBatch();
						stmt.clearBatch();
					} // if time to submit
				} // for each student to be blocked
				if (batchNum % 100 != 0) {
					stmt.executeBatch();
				} // if need to submit
			} // if block
			con.commit();
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, null);
		}
	} // blockUsers(int, List<String>, boolean)

	/** Gets if a user is blocked from a course's forum.
	 * @param	courseId	ID number of the course
	 * @param	studentId	login ID of the student
	 * @return	true if the user is blocked from the course's forum
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static boolean isBlocked(int courseId, String studentId) 
			throws DBException {
		final String SELF = "ForumRW.isBlocked: ";
		boolean isBlocked = false;
		final String qry = toString(
				SELECT, count(), AS + SRCH_RESULT
				+ FROM + BLOCKED
				+ WHERE + BLOCKED_COURSE + EQUALS + QMARK
				+ AND + BLOCKED_USER + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				courseId,
				studentId);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try { 
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery(); 
			if (rs.next()) isBlocked = rs.getInt(SRCH_RESULT) > 0;
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return isBlocked;
	} // isBlocked(int, String)

/* **************** Miscellaneous methods ****************/

	/** Gets the authors of posts in a topic.
	 * @param	topicId	ID number of the topic
	 * @return	array of users
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static User[] getTopicAuthors(int topicId) throws DBException {
		final String SELF = "ForumRW.getTopicAuthors: ";
		final String where = toString(
				WHERE + USER_ID + IN, parensBuild(
					SELECT + POST_AUTHOR + AS + SRCH_RESULT
						+ FROM + POSTS
						+ WHERE + POST_TOPIC + EQUALS + QMARK 
					+ UNION + SELECT + CRS_INSTRUCTOR + AS + SRCH_RESULT
						+ FROM + COURSES
						+ WHERE + CRS_ID + IN, parens(
							SELECT + FTOPIC_COURSE
							+ FROM + FORUM_TOPICS
							+ WHERE + FTOPIC_ID + EQUALS + QMARK)));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(where, 
				topicId, 
				topicId);
		final List<User> topicAuthors = 
				UserRead.getUsers(sql_vals, !GET_DETAILS);
		return topicAuthors.toArray(new User[topicAuthors.size()]);
	} // getTopicAuthors(int)

	/** Gets the enquoted DB string for one year ago. 
	 * @return	enquoted DB string
	 */
	private static String getOneYearAgo() {
		final Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.YEAR, -1);
		final Date oneYearAgo = calendar.getTime();
		return quotes(dateToString(oneYearAgo));
	} // getOneYearAgo()

	/** Gets the number of post images that are a year or more old.
	 * @param	con	database connection
	 * @return	the number of post images that are a year or more old
	 * @throws	SQLException	if there's a problem reading the database
	 */
	private static int getNumYearOldPostImages(Connection con) 
			throws SQLException {
		return getNumPostImages(con, YEAR_OLD);
	} // getNumYearOldPostImages()

	/** Gets the number of post images.
	 * @param	con	database connection
	 * @return	the number of post images
	 * @throws	SQLException	if there's a problem reading the database
	 */
	private static int getNumPostImages(Connection con) throws SQLException {
		return getNumPostImages(con, !YEAR_OLD);
	} // getNumPostImages(Connection)

	/** Gets the number of post images.
	 * @param	con	database connection
	 * @param	yearOld	whether to count only images that are a year or more old
	 * @return	the number of post images
	 * @throws	SQLException	if there's a problem reading the database
	 */
	private static int getNumPostImages(Connection con, boolean yearOld) 
			throws SQLException {
		final String SELF = "ForumRW.getNumPostImages: ";
		final ImageRW imgWriter = new ImageRW(con);
		final String qry = toString(
				SELECT, count(), AS + SRCH_RESULT 
				+ FROM + POSTS
				+ WHERE + POST_FIGTYPE + EQUALS, quotes(DBVALUES[IMAGE]),
				AND, toNumber(clobToString(POST_FIGURE)),
					IN, parens(imgWriter.getAllImageIds()),
				!yearOld ? "" : getBuilder(
 					AND + POST_EDITED + LESS_THAN, getOneYearAgo()));
		debugPrint(SELF, qry);
		Statement stmt = null;
		ResultSet rs = null;
		int numPostImages = 0;
		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			if (rs.next()) numPostImages = rs.getInt(SRCH_RESULT);
		} finally {
			closeStmtAndRs(stmt, rs);
		}
		debugPrint(SELF + "found ", numPostImages, " post image(s)",
				yearOld ? " a year or more old." : ".");
		return numPostImages;
	} // getNumPostImages(Connection)

	/** Gets the full file name of a post's image.
	 * @param	postId	ID numbers of the post 
	 * @param	figId	ID of the image
	 * @param	ext	extension
	 * @return	the full file name
	 */
	private static String getPostImageFileName(int postId, int figId, 
			String ext) {
		final StringBuilder filenameBld = getBuilder(AppConfig.relFiguresDir);
		if (!AppConfig.relFiguresDir.endsWith("/")) filenameBld.append('/');
 		appendTo(filenameBld, POST_FILENAME, postId, '_', figId);
		if (!Utils.isEmpty(ext)) appendTo(filenameBld, DOT, ext);
		return filenameBld.toString();
	} // getPostImageFileName(int, String)

	/** Constructor to disable external instantiation. */
	private ForumRW() { }

} // ForumRW
