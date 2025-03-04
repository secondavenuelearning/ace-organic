package com.epoch.db;

import static com.epoch.db.dbConstants.TextbookRWConstants.*;
import static com.epoch.db.dbConstants.UserRWConstants.*;
import com.epoch.courseware.User;
import com.epoch.textbooks.textConstants.ContentConstants;
import com.epoch.textbooks.TextChapter;
import com.epoch.textbooks.TextContent;
import com.epoch.textbooks.Textbook;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.ParameterException;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Contains all methods pertaining to reading and writing textbooks.  */
public class TextbookRW extends DBCommon implements ContentConstants {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

	/** The textbook being saved. */
	final private Textbook book;
	/** Database connection. */
	transient private Connection con = null;

	/** Constructor.
	 * @param	bk	an online textbook
	 */
	TextbookRW(Textbook bk) {
		book = bk;
	} // TextbookRW(Textbook) 

/* *********** Static methods ****************/

	/** Gets authors, names, ID numbers, and flags, but no chapters or content,
	 * of textbooks of an instructor.
	 * @param	authorId	the instructor
	 * @param	others	whether to include textbooks written by others that this
	 * instructor is authorized to see
	 * @return	array of Textbooks containing only superficial information (no
	 * chapters or content)
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Textbook[] getBooks(String authorId, boolean others) 
			throws DBException {
		return getBooks(new SQLWithQMarks(
				getBuilder(
					WHERE + TEXTBK_AUTHOR + EQUALS + QMARK
					+ OR + TEXTBK_ID + IN, parens(
						SELECT + COAUTH_BOOKID
						+ FROM + COAUTHORS
						+ WHERE + COAUTH_COAUTHOR + EQUALS + QMARK),
					!others ? ""
						: getBuilder(OR, bitand(TEXTBK_FLAGS, VISIBLE), 
							IS_NOT_ZERO)),
				authorId,
				authorId));
	} // getBooks(String, boolean)

	/** Gets SQL to get the owners of books coauthored by an instructor
	 * (represented with a question mark).
	 * @param	authorId	the instructor
	 * @return	SQL to get the owners of books coauthored by an instructor
	 */
	static SQLWithQMarks getOwnersOfCoauthoredBooksSQL(String authorId) {
		return new SQLWithQMarks(
				toString(
					SELECT + TEXTBK_AUTHOR
					+ FROM + TEXTBOOKS
					+ WHERE + TEXTBK_ID + IN, parensBuild(
						SELECT + COAUTH_BOOKID
						+ FROM + COAUTHORS
						+ WHERE + COAUTH_COAUTHOR + EQUALS + QMARK)),
				authorId);
	} // getOwnersOfCoauthoredBooksSQL()

	/** Gets authors, names, ID numbers, and flags, but no chapters or content,
	 * of textbooks of which an instructor is author or coauthor but he or she
	 * may not edit.
	 * @param	authorId	the instructor
	 * @return	array of Textbooks containing only superficial information (no
	 * chapters or content)
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Textbook[] getLockedBooks(String authorId) 
			throws DBException {
		return getBooks(new SQLWithQMarks(
				getBuilder(
					WHERE, parensBuild(
						TEXTBK_ID + IN, parens(
							SELECT + COAUTH_BOOKID
							+ FROM + COAUTHORS
							+ WHERE + COAUTH_COAUTHOR + EQUALS + QMARK),
						OR + TEXTBK_AUTHOR + EQUALS + QMARK),
					AND + TEXTBK_LOCKHOLDER + NOT_EQUALS + QMARK
					+ AND + TEXTBK_LOCKHOLDER + IS_NOT_NULL),
				authorId,
				authorId,
				authorId));
	} // getBooks(String, boolean)

	/** Gets authors, names, ID numbers, and flags, but no chapters or content,
	 * of textbooks of an instructor.
	 * @param	whereSql_vals	condition on getting the textbooks
	 * @return	array of Textbooks containing only superficial information (no
	 * chapters or content)
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static Textbook[] getBooks(SQLWithQMarks whereSql_vals) 
			throws DBException {
		final String SELF = "TextbookRW.getBooks: ";
		final String qry = toString(
				SELECT, joinAll(
					TEXTBK_ID,
					TEXTBK_NAME,
					TEXTBK_AUTHOR,
					TEXTBK_FLAGS,
					TEXTBK_LOCKHOLDER),
				FROM + TEXTBOOKS, 
				whereSql_vals.getSql(),
				ORDER_BY, joinAll(
					TEXTBK_AUTHOR,
					clobToString(TEXTBK_NAME)));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, whereSql_vals);
		debugPrint(SELF, sql_vals);
		final List<Textbook> books = new ArrayList<Textbook>();
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final int bookId = rs.getInt(TEXTBK_ID);
				final String name = rs.getString(TEXTBK_NAME);
				final String ownerId = rs.getString(TEXTBK_AUTHOR);
				final int flags = rs.getInt(TEXTBK_FLAGS);
				final Textbook book = 
						new Textbook(bookId, ownerId, name, flags);
				book.setLockHolder(rs.getString(TEXTBK_LOCKHOLDER));
				books.add(book);
			}  // while there are results
		} catch (SQLException e) {
			alwaysPrint(SELF + "SQLException caught.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (ParameterException e) {
			alwaysPrint(SELF + "ParameterException caught.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		} // try
		final int numBooks = books.size();
		debugPrint(SELF + "returning ", numBooks, " books.");
		return books.toArray(new Textbook[numBooks]);
	} // getBooks(SQLWithQMarks)

	/** Gets a textbook.
	 * @param	bookId	ID number of the book to get
	 * @return	the textbook
	 * @throws	ParameterException	if no book has this ID number
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Textbook getBook(int bookId) 
			throws DBException, ParameterException {
		final String SELF = "TextbookRW.getBook: ";
		Textbook book = null;
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			final String qry = toString(
					SELECT, joinAll(
						TEXTBK_NAME,
						TEXTBK_AUTHOR,
						TEXTBK_FLAGS,
						TEXTBK_LOCKHOLDER),
					FROM + TEXTBOOKS
					+ WHERE + TEXTBK_ID + EQUALS + QMARK);
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
					bookId);
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) {
				final String name = rs.getString(TEXTBK_NAME);
				final String ownerId = rs.getString(TEXTBK_AUTHOR);
				final int flags = rs.getInt(TEXTBK_FLAGS);
				book = new Textbook(bookId, ownerId, name, flags);
				book.setLockHolder(rs.getString(TEXTBK_LOCKHOLDER));
			} else throw new ParameterException(SELF
					+ "no book with ID " + bookId);
			closeStmtAndRs(stmt, rs);
			sql_vals.setSql( // values don't change
					SELECT, joinAll(
						TEXTCHAP_ID,
						TEXTCHAP_NAME),
					FROM + TEXTCHAPS
					+ WHERE + TEXTCHAP_BOOKID + EQUALS + QMARK
					+ ORDER_BY + TEXTCHAP_NUM); 
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			int ct = 0;
			final Map<Integer, TextChapter> chapsByIds =
					new HashMap<Integer, TextChapter>();
			while (rs.next()) {
				final int chapId = rs.getInt(TEXTCHAP_ID);
				final String name = rs.getString(TEXTCHAP_NAME);
				final TextChapter chap = new TextChapter(chapId, name);
				book.addChapter(chap);
				chapsByIds.put(Integer.valueOf(chapId), chap);
				debugPrint(SELF + "got chapter ", ++ct, " with ID ",
						chapId, ": ", name);
			} // while there are chapters
			closeStmtAndRs(stmt, rs);
			sql_vals.setSql( // values don't change
					SELECT, joinAll(
						TEXTCONTENT_ID,
						TEXTCONTENT_CHAPID,
						TEXTCONTENT_TYPE,
						TEXTCONTENT_DATA,
						TEXTCONTENT_CAPTION,
						TEXTCONTENT_EXTRA),
					FROM + TEXTCONTENT
					+ WHERE + TEXTCONTENT_CHAPID + IN, parens(
						SELECT + TEXTCHAP_ID
						+ FROM + TEXTCHAPS
						+ WHERE + TEXTCHAP_BOOKID + EQUALS + QMARK),
					ORDER_BY, joinAll(
						TEXTCONTENT_CHAPID,
						TEXTCONTENT_NUM));
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) while (!rs.isAfterLast()) {
				final int chapId = rs.getInt(TEXTCONTENT_CHAPID);
				final TextChapter chap = chapsByIds.get(Integer.valueOf(chapId));
				ct = 0;
				if (chap != null) while (!rs.isAfterLast()
						&& chapId == rs.getInt(TEXTCONTENT_CHAPID)) {
					final int contentId = rs.getInt(TEXTCONTENT_ID);
					final String typeStr = rs.getString(TEXTCONTENT_TYPE);
					final String data = rs.getString(TEXTCONTENT_DATA);
					final String caption = rs.getString(TEXTCONTENT_CAPTION);
					final String extraData = rs.getString(TEXTCONTENT_EXTRA);
					final TextContent content = new TextContent(contentId, 
							typeStr, data, caption, extraData);
					debugPrint(SELF + "got content ", ++ct, " with ID ",
							contentId, " of type ", typeStr, 
							" in chapter with ID ", chapId);
					if (content.isImage() || content.isMovie()) {
						final boolean okay = 
								makeImageFileName(con, book, content);
						if (okay) debugPrint(SELF + "content ", ct, " is ",
									content.isImage() ? "an image" : "a movie", 
								" with ID ", data, " and stored in file ", 
								content.getContent());
						else {
							Utils.alwaysPrint(SELF + "ERROR: cannot find ",
										content.isImage() ? "image" : "movie", 
									data, " of content with ID ",
									contentId, " of chapter with ID ", 
									chapId, " of book with ID ", bookId);
							continue;
						} // if writing of image proceeded
					} // if is image 
					chap.addContent(content);
					rs.next();
				} // if chapter exists, while more content in this chapter
			} // while there is content 
		} catch (SQLException e) {
			alwaysPrint(SELF + "SQLException caught.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		} // try
		return book;
	} // getBook(int)

	/** Saves the filename of an image or movie in the content.
	 * @param	con	database connection
	 * @param	book	the textbook
	 * @param	content	the content containing the image
	 * @return	true if the procedure was successful
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static boolean makeImageFileName(Connection con, Textbook book, 
			TextContent content) throws DBException {
		final int imgId = MathUtils.parseInt(content.getContent());
		final String ext = getImageExtension(imgId);
		content.makeImageFileName(ext);
		return ext != null;
	} // makeImageFileName(Connection, Textbook, TextContent)

	/** Gets the extension of an image or movie.
	 * @param	imgId	the image ID number
	 * @return	the extension
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static String getImageExtension(int imgId) throws DBException {
		final String SELF = "TextbookRW.getImageExtension: ";
		Connection con = null;
		try {
			con = getPoolConnection();
			final ImageRW imgReader = new ImageRW(con, LOCAL);
			return imgReader.getExtension(imgId);
		} catch (SQLException e) {
			alwaysPrint(SELF + "SQLException caught.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		} // try
	} // getImageExtension(int)

	/** Writes a new textbook.
	 * @param	bk	an online textbook
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void writeBook(Textbook bk) throws DBException {
		final TextbookRW bookWriter = new TextbookRW(bk);
		bookWriter.writeBook();
	} // writeBook(Textbook)

	/** Deletes books by their authors.
	 * @param	con	connection to database
	 * @param	authorIds	login IDs of the authors 
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	static void deleteBooks(Connection con, String[] authorIds) 
			throws SQLException {
		final String SELF = "TextbookRW.deleteBooks: ";
		if (Utils.isEmpty(authorIds)) return;
		runUpdates(con, SELF, getDeleteBooksSQLs_Vals(authorIds));
	} // deleteBooks(Connection, String[])

	/** Gets queries to delete books written by more than one instructor plus
	 * values to substitute for question marks in the queries.
	 * @param	authorIds	login IDs of the book authors, parenthesized
	 * @return	queries and values to delete the books
	 */
	private static SQLWithQMarks[] getDeleteBooksSQLs_Vals(
			String[] authorIds) {
		final String SELF = "TextbookRW.getDeleteBooksSQLs_Vals: ";
		final int CONTENT = 0;
		final int CHAPS = 1;
		final int CRSES = 2;
		final int COAUTHS = 3;
		final int BOOKS = 4;
		final StringBuilder[] qryBlds = getStringBuilders(5);
		final StringBuilder bookIdsSqlBld = parensBuild(
				SELECT + TEXTBK_ID
				+ FROM + TEXTBOOKS,
				WHERE + TEXTBK_AUTHOR + IN, parensQMarks(authorIds));
 		appendTo(qryBlds[CONTENT], 
				DELETE_FROM + TEXTCONTENT
				+ WHERE + TEXTCONTENT_CHAPID + IN, parensBuild(
					SELECT + TEXTCHAP_ID
					+ FROM + TEXTCHAPS
					+ WHERE + TEXTCHAP_BOOKID + IN, bookIdsSqlBld));
 		appendTo(qryBlds[CHAPS], 
				DELETE_FROM + TEXTCHAPS
				+ WHERE + TEXTCHAP_BOOKID + IN, bookIdsSqlBld);
 		appendTo(qryBlds[CRSES], 
				CourseRW.SET_CRS_BKID_TO_0_WHERE_BKID(), IN, bookIdsSqlBld);
 		appendTo(qryBlds[COAUTHS], 
				DELETE_FROM + COAUTHORS
				+ WHERE + COAUTH_BOOKID + IN, bookIdsSqlBld);
 		appendTo(qryBlds[BOOKS], 
				DELETE_FROM + TEXTBOOKS
				+ WHERE + TEXTBK_AUTHOR + IN, parensQMarks(authorIds));
		final SQLWithQMarks[] sqls_vals = new SQLWithQMarks[5];
		for (int qryNum = 0; qryNum < 5; qryNum++) {
			sqls_vals[qryNum] = new SQLWithQMarks(qryBlds[qryNum]);
			sqls_vals[qryNum].addValuesArray(authorIds);
		} // for each query & values
		return sqls_vals;
	} // getDeleteBooksSQLs_Vals(String[])

	/** Deletes a book.
	 * @param	bookId	ID of the book
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void deleteBook(int bookId) throws DBException {
		final String SELF = "TextbookRW.deleteBook: ";
		final int CONTENT = 0;
		final int CHAPS = 1;
		final int CRSES = 2;
		final int COAUTHS = 3;
		final int BOOKS = 4;
		final StringBuilder[] qryBlds = getStringBuilders(5);
 		appendTo(qryBlds[CONTENT], 
				DELETE_FROM + TEXTCONTENT
				+ WHERE + TEXTCONTENT_CHAPID + IN, parensBuild(
					SELECT + TEXTCHAP_ID
					+ FROM + TEXTCHAPS
					+ WHERE + TEXTCHAP_BOOKID + EQUALS + QMARK));
 		qryBlds[CHAPS].append( 
				DELETE_FROM + TEXTCHAPS
				+ WHERE + TEXTCHAP_BOOKID + EQUALS + QMARK);
 		appendTo(qryBlds[CRSES], 
				CourseRW.SET_CRS_BKID_TO_0_WHERE_BKID(), EQUALS + QMARK);
 		qryBlds[COAUTHS].append( 
				DELETE_FROM + COAUTHORS
				+ WHERE + COAUTH_BOOKID + EQUALS + QMARK);
 		qryBlds[BOOKS].append( 
				DELETE_FROM + TEXTBOOKS
				+ WHERE + TEXTBK_ID + EQUALS + QMARK);
		final SQLWithQMarks[] sqls_vals = new SQLWithQMarks[5];
		for (int qryNum = 0; qryNum < 5; qryNum++) {
			sqls_vals[qryNum] = new SQLWithQMarks(qryBlds[qryNum],
					bookId);
		} // for each query & values
		runUpdates(SELF, sqls_vals);
	} // deleteBook(int)

	/** Runs a series of updates whose SQL has been written elsewhere.
	 * @param	SELF	name of calling method
	 * @param	sqls_vals	SQL updates to run as well as values to substitute
	 * for question marks in the SQL
	 * @throws	DBException	if there's a problem writing to the database
	 */
	private static void runUpdates(String SELF, SQLWithQMarks[] sqls_vals) 
			throws DBException {
		Connection con = null;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			runUpdates(con, SELF, sqls_vals);
			con.commit();
		} catch (SQLException e) {
			alwaysPrint(SELF + "SQLException caught.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		} // try
	} // runUpdates(String, SQLWithQMarks[])

	/** Runs a series of updates whose SQL has been written elsewhere.
	 * @param	con	connection to database
	 * @param	SELF	name of calling method
	 * @param	sqls_vals	updates to run, plus values to substitute for
	 * question marks in the SQL
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private static void runUpdates(Connection con, String SELF, 
			SQLWithQMarks[] sqls_vals) throws SQLException {
		for (final SQLWithQMarks sql_vals : sqls_vals) {
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
		} // for each query
	} // runUpdates(Connection, String, SQLWithQMarks[])

/* *********** Dynamic methods to write a textbook and its contents ****************/

	/** Writes name and order of chapters in an online textbook.
	 * @throws	DBException	if there's a problem writing to the database
	 */
	private void writeBook() throws DBException {
		final String SELF = "TextbookRW.writeBook: ";
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			final List<String> imagesToDeleteFromDisk = 
					(book.getId() == 0 ? addBook() : setBook());
			con.commit();
			ImageRW.deleteImagesFromDisk(imagesToDeleteFromDisk);
			final List<TextChapter> chapters = book.getChapters();
			int chapNum = 0;
			for (final TextChapter chapter : chapters) {
				chapNum++;
				int contentNum = 0;
				for (final TextContent content : chapter.getContents()) {
					contentNum++;
					debugPrint(SELF + "content ", contentNum, 
							" of chapter ", chapNum, " has content type ", 
							content.getContentType(), " = ",
							content.getDbContentType());
					if (content.isImage() || content.isMovie()) {
						final boolean okay = 
								makeImageFileName(con, book, content);
						if (okay) debugPrint(SELF + "content ", contentNum, 
								" of chapter ", chapNum, " is ", 
									content.isImage() ? "an image" : "a movie",
								" stored in file ", content.getContent());
						else Utils.alwaysPrint(SELF + "ERROR: cannot find ",
									content.isImage() ? "image" : "movie",
								" with ID ", content.getContent(), 
								" of content ", contentNum, " of chapter ", 
								chapNum, " of book with ID ", book.getId());
					} // if content is an image
				} // for each piece of content
			} // for each chapter
		} catch (SQLException e) {
			alwaysPrint(SELF + "SQLException caught.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		} // try
	} // writeBook()

	/** Writes an online textbook.  
	 * @return	an empty list of strings
	 * @throws	DBException	if there's a problem writing to the database
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private List<String> addBook() throws DBException, SQLException {
		final String SELF = "TextbookRW.addBook: ";
		final int CHAP = 0;
		final int CONTENT = 1;
		// write book data
		final int bookId = nextSequence(con, TEXTBOOKS_SEQ);
		String[] fields = new String[] {
				TEXTBK_ID,
				TEXTBK_AUTHOR,
				TEXTBK_NAME, // CLOB
				TEXTBK_FLAGS,
				TEXTBK_LOCKHOLDER};
		final String qry = getInsertIntoValuesQMarksSQL(TEXTBOOKS, fields);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				bookId,
				book.getOwnerId(),
				book.getName(),
				book.getFlags(),
				book.getOwnerId());
		debugPrint(SELF, sql_vals);
		tryUpdate(con, sql_vals);
		book.setId(bookId);
		// prepare to write chapters and content
		final StringBuilder[] qryBlds = getStringBuilders(2);
		fields = new String[] {
				TEXTCHAP_ID,
				TEXTCHAP_BOOKID,
				TEXTCHAP_NUM,
				TEXTCHAP_NAME};
 		qryBlds[CHAP].append(getInsertIntoValuesQMarksSQL(TEXTCHAPS, fields));
		fields = new String[] {
				TEXTCONTENT_ID,
				TEXTCONTENT_CHAPID,
				TEXTCONTENT_NUM,
				TEXTCONTENT_TYPE,
				TEXTCONTENT_DATA, // CLOB
				TEXTCONTENT_CAPTION, // CLOB
				TEXTCONTENT_EXTRA}; // CLOB
 		qryBlds[CONTENT].append(getInsertIntoValuesQMarksSQL( 
				TEXTCONTENT, fields));
		final String[] qrys = new String[2];
		final PreparedStatement[] stmts = new PreparedStatement[2];
		final List<TextChapter> chapters = book.getChapters();
		try {
			prepareStatements(qryBlds, qrys, stmts);
			debugPrint(SELF, qrys[CHAP]); 
			int chapNum = 1;
			for (final TextChapter chapter : chapters) {
				final int chapId = nextSequence(con, TEXTCHAPS_SEQ);
				final StringBuilder joinedValues = setValues(stmts[CHAP],
						chapId,
						book.getId(),
						chapNum++,
						chapter.getName());
				debugPrint(SELF, "batch ", chapNum - 1, ": ", joinedValues); 
				stmts[CHAP].addBatch(); 
				chapter.setId(chapId);
				debugPrint(SELF, qrys[CONTENT]);
				int cnttNum = 1;
				for (final TextContent content : chapter.getContents()) {
					writeContent(stmts[CONTENT], content, chapId, cnttNum++);
				} // for each piece of content
			} // for each chapter
			for (final PreparedStatement stmt : stmts) {
				stmt.executeBatch();
			} // for each batch
		} finally {
			for (final PreparedStatement stmt : stmts) {
				closeConnection(null, stmt, null);
			} // for each statement
		} // try
		return new ArrayList<String>();
	} // addBook()

	/** Rewrites an online textbook.  
	 * @return	list of absolute names of image files that need to be deleted
	 * from disk after commit
	 * @throws	DBException	if there's a problem writing to the database
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private List<String> setBook() throws DBException, SQLException {
		final String SELF = "TextbookRW.setBook: ";
		final StringBuilder[] qryBlds = getStringBuilders(5);
		final int ADD_CHAP = 0;
		final int SET_CHAP = 1;
		final int MOVE_CNTT = 2;
		final int ADD_CNTT = 3;
		final int SET_CNTT = 4;
		// add a new chapter
		String[] fields = new String[] {
				TEXTCHAP_ID,
				TEXTCHAP_BOOKID,
				TEXTCHAP_NUM,
				TEXTCHAP_NAME};
 		qryBlds[ADD_CHAP].append(
				getInsertIntoValuesQMarksSQL(TEXTCHAPS, fields));
		// update name and serial number of chapter
 		appendTo(qryBlds[SET_CHAP], 
				UPDATE + TEXTCHAPS + SET, equalsJoinQMarks(
					TEXTCHAP_NUM,
					TEXTCHAP_NAME),
				WHERE + TEXTCHAP_ID + EQUALS + QMARK);
		// move content, perhaps to another chapter
 		appendTo(qryBlds[MOVE_CNTT], 
				UPDATE + TEXTCONTENT + SET, equalsJoinQMarks(
					TEXTCONTENT_NUM,
					TEXTCONTENT_CHAPID),
				WHERE + TEXTCONTENT_ID + EQUALS + QMARK);
		// add a new piece of content
		fields = new String[] {
				TEXTCONTENT_ID,
				TEXTCONTENT_CHAPID,
				TEXTCONTENT_NUM,
				TEXTCONTENT_TYPE,
				TEXTCONTENT_DATA, // CLOB
				TEXTCONTENT_CAPTION, // CLOB
				TEXTCONTENT_EXTRA}; // CLOB
 		qryBlds[ADD_CNTT].append(
				getInsertIntoValuesQMarksSQL(TEXTCONTENT, fields));
		// update an existing piece of content
 		appendTo(qryBlds[SET_CNTT], 
				UPDATE + TEXTCONTENT + SET, equalsJoinQMarks(
					TEXTCONTENT_CHAPID,
					TEXTCONTENT_NUM,
					TEXTCONTENT_TYPE,
					TEXTCONTENT_DATA, // CLOB
					TEXTCONTENT_CAPTION, // CLOB
					TEXTCONTENT_EXTRA), // CLOB
				WHERE + TEXTCONTENT_ID + EQUALS + QMARK);
		final String[] qrys = new String[5];
		final PreparedStatement[] stmts = new PreparedStatement[5];
		final boolean[] doBatch = new boolean[5];
		Arrays.fill(doBatch, false);
		final List<Integer> deletedContentIds = new ArrayList<Integer>();
		final List<String> imagesToDeleteFromDisk = new ArrayList<String>();
		boolean haveWritten = false;
		try {
			if (book.isChanged()) {
				// update textbook name and flags
				final String qry = toString(
						UPDATE + TEXTBOOKS + SET, equalsJoinQMarks(
							TEXTBK_NAME,
							TEXTBK_FLAGS),
						WHERE + TEXTBK_ID + EQUALS + QMARK);
				final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
						book.getName(),
						book.getFlags(),
						book.getId());
						debugPrint(SELF, sql_vals);
						tryUpdate(con, sql_vals);
						haveWritten = true;
			} // if book has changed
			prepareStatements(qryBlds, qrys, stmts);
			final List<TextChapter> chapters = book.getChapters();
			int chapNum = 1;
			for (final TextChapter chapter : chapters) {
				int chapId = chapter.getId();
				if (chapId == 0) {
					// chapter is new
					chapId = nextSequence(con, TEXTCHAPS_SEQ);
					final StringBuilder joinedValues = 
							setValues(stmts[ADD_CHAP], 
								chapId,
								book.getId(),
								chapNum,
								chapter.getName());
					debugPrint(SELF, qrys[ADD_CHAP], "; ", joinedValues); 
					stmts[ADD_CHAP].addBatch(); 
					chapter.setId(chapId);
					doBatch[ADD_CHAP] = true;
				} else if (book.isChanged() || chapter.isChanged()) {
					// if book changed, chapters may have been reordered;
					// if chapter changed, chapter may have been renamed
					final StringBuilder joinedValues = 
							setValues(stmts[SET_CHAP], 
								chapNum,
								chapter.getName(),
								chapId);
					debugPrint(SELF, qrys[SET_CHAP], "; ", joinedValues); 
					stmts[SET_CHAP].addBatch(); 
					doBatch[SET_CHAP] = true;
				} // if chapter is new, or may be changed or reordered
				int cnttNum = 1;
				for (final TextContent content : chapter.getContents()) {
					final int contentId = content.getId();
					if (contentId == 0 || content.isChanged()) {
						// content is new or modified
						final int which = (contentId == 0
								? ADD_CNTT : SET_CNTT);
						debugPrint(SELF, qrys[which]);
						writeContent(stmts[which], content, chapId, cnttNum);
						doBatch[which] = true;
					} else if (chapter.isChanged()) {
						// content may be renumbered
						final StringBuilder joinedValues = 
								setValues(stmts[MOVE_CNTT],
									cnttNum,
									chapId,
									contentId);
						debugPrint(SELF, qrys[MOVE_CNTT], "; ", joinedValues); 
						stmts[MOVE_CNTT].addBatch();
						doBatch[MOVE_CNTT] = true;
					} // if content is new or modified
					cnttNum++;
				} // for each piece of content in the chapter
				final List<Integer> chapDeletedContentIds = 
						chapter.getDeletedContentIds();
				deletedContentIds.addAll(chapDeletedContentIds);
				chapDeletedContentIds.clear();
				chapNum++;
			} // for each chapter
			int stmtNum = 0;
			for (final PreparedStatement stmt : stmts) {
				if (doBatch[stmtNum]) stmt.executeBatch();
				haveWritten = haveWritten || doBatch[stmtNum];
				stmtNum++;
			} // for each batch
			// now remove deleted chapters and contents
			final List<Integer> deletedChapterIds = book.getDeletedChapterIds();
			if (!deletedChapterIds.isEmpty()) {
				final StringBuilder deletedChapterIdsQMarks =
						parensQMarks(deletedChapterIds);
				final String qry = toString(
						DELETE_FROM + TEXTCONTENT
						+ WHERE + TEXTCONTENT_CHAPID + IN, 
							deletedChapterIdsQMarks);
				final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
						deletedChapterIds);
				debugPrint(SELF, sql_vals);
				tryUpdate(con, sql_vals);
				sql_vals.setSql( // values don't change
						DELETE_FROM + TEXTCHAPS
						+ WHERE + TEXTCHAP_ID + IN, deletedChapterIdsQMarks);
				debugPrint(SELF, sql_vals);
				tryUpdate(con, sql_vals);
				haveWritten = true;
				deletedChapterIds.clear();
			} // if chapters have been deleted
			if (!deletedContentIds.isEmpty()) {
				// split content IDs into bunches to avoid limit of 1000
				final List<int[]> deletedContentIdBunches = 
						getIntGroups(deletedContentIds);
				for (final int[] deletedContentIdBunch 
						: deletedContentIdBunches) {
					// delete images associated with deleted content
					final StringBuilder idsSql = 
							parensQMarks(deletedContentIdBunch);
					final ImageRW imgDeleter = 
							new ImageRW(con, book.getOwnerId());
					final SQLWithQMarks sql_vals = new SQLWithQMarks(
							toString(
								SELECT, toNumber(TEXTCONTENT_DATA),
								FROM + TEXTCONTENT 
								+ WHERE + TEXTCONTENT_TYPE + EQUALS, 
									quotes(DB_VALUES[IMAGE]),
								AND + TEXTCONTENT_ID + IN, idsSql),
							deletedContentIdBunch);
					imagesToDeleteFromDisk.addAll(
							imgDeleter.deleteImages(sql_vals));
					// delete content
					sql_vals.setSql( // values don't change
							DELETE_FROM + TEXTCONTENT
							+ WHERE + TEXTCONTENT_ID + IN, idsSql);
					debugPrint(SELF, sql_vals);
					tryUpdate(con, sql_vals);
				} // for each bunch
				haveWritten = true;
			} // if there are contents to delete
		} finally {
			for (final PreparedStatement stmt : stmts) {
				closeConnection(null, stmt, null);
			} // for each statement
		} // try
		if (!haveWritten) debugPrint(SELF + "nothing changed in book with ID ",
				book.getId(), ", nothing written.");
		return imagesToDeleteFromDisk;
	} // setBook()

	/** Writes a piece of content in a chapter in an online textbook.
	 * @param	stmt	prepared statement for writing the data
	 * @param	content	a piece of content in a chapter in an online textbook
	 * @param	chapId	ID number of the content's chapter
	 * @param	serialNum	serial number of the content in the chapter
	 * @throws	DBException	if there's a problem writing to the database
	 */
	private void writeContent(PreparedStatement stmt, TextContent content, 
			int chapId, int serialNum) throws DBException {
		final String SELF = "TextbookRW.writeContent: ";
		int contentId = content.getId();
		final boolean newContent = contentId == 0;
		if (newContent) contentId = nextSequence(con, TEXTCONTENT_SEQ);
		String contentData = content.getContent();
		try {
			if (content.isImage() || content.isMovie()) {
				// temporary filename stored in data field of content
				final ImageRW imgWriter = new ImageRW(con, book.getOwnerId());
				final int imageId = imgWriter.getNewImageId();
				final String imageIdStr = String.valueOf(imageId);
				final String extension = Utils.getExtension(contentData);
				content.setContent(imageIdStr);
				final String newFileName = 
						content.makeImageFileName(extension);
				imgWriter.addImage(imageId, contentData, newFileName);
				debugPrint(SELF + "got file from ", contentData, 
						" for new ", content.isImage() ? "image" : "movie", 
						" with ID ", imageId, "; new location is ",
						newFileName);
				// store figure ID in figure field of DB
				contentData = imageIdStr;
			} // if content is image
			final String typeStr = content.getDbContentType();
			StringBuilder joinedValues;
			if (newContent) {
				joinedValues = setValues(stmt,
						contentId,
						chapId,
						serialNum,
						typeStr,
						contentData, 
						content.getCaption(),
						content.getExtraData());
				debugPrint(SELF, joinedValues); 
				content.setId(contentId);
			} else {
				joinedValues = setValues(stmt,
						chapId,
						serialNum,
						typeStr,
						contentData, 
						content.getCaption(),
						content.getExtraData(),
						contentId);
			} // if content is new
			debugPrint(SELF, "batch ", serialNum, ": ", joinedValues); 
			stmt.addBatch();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		}
	} // writeContent(PreparedStatement, TextContent, int, int)

	/** Gets an array of initialized StringBuilders.
	 * @param	numQryBlds	number of StringBuilders required
	 * @return	array of initialized StringBuilders
	 */
	private static StringBuilder[] getStringBuilders(int numQryBlds) {
		final StringBuilder[] qryBlds = new StringBuilder[numQryBlds];
		for (int qryNum = 0; qryNum < numQryBlds; qryNum++) {
			qryBlds[qryNum] = new StringBuilder();
		} // for each query builder
		return qryBlds;
	} // getStringBuilders(int)

	/** Initializes arrays of queries and prepared statements from an array of
	 * SQL statements in StringBuilders.
	 * @param	qryBlds	array of StringBuilders containing SQL statements
	 * @param	qrys	array of queries to be initialized
	 * @param	stmts	array of prepared statements to be initialized
	 * @throws	SQLException	if there's a problem preparing the
	 * PreparedStatements
	 */
	private void prepareStatements(StringBuilder[] qryBlds, String[] qrys, 
			PreparedStatement[] stmts) throws SQLException {
		int qryNum = 0;
		for (final StringBuilder qryBld : qryBlds) {
			qrys[qryNum] = qryBld.toString();
			stmts[qryNum] = con.prepareStatement(qrys[qryNum]);
			qryNum++;
		} // for each statement
	} // prepareStatements(StringBuilder[], String[], PreparedStatement[])

/* *********** Coauthor methods ****************/

	/** Gets coauthors of a textbook.
	 * @param	bookId	ID number of the book
	 * @return	array of Users
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static User[] getCoauthors(int bookId) throws DBException {
		final String where = toString(
				WHERE + USER_ID + IN, parens(
					SELECT + COAUTH_COAUTHOR
					+ FROM + COAUTHORS
					+ WHERE + COAUTH_BOOKID + EQUALS + QMARK));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(where, 
				bookId);
		final List<User> users = UserRead.getUsers(sql_vals, !GET_DETAILS);
		return users.toArray(new User[users.size()]);
	} // getCoauthors(int)

	/** Gets names of all authors of a textbook.
	 * @param	bookId	ID number of the book
	 * @return	array of authors' names
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static String[] getAllAuthorNames(int bookId) throws DBException {
		final String where = toString(
				WHERE + USER_ID + IN, parens(
					SELECT + COAUTH_COAUTHOR
					+ FROM + COAUTHORS
					+ WHERE + COAUTH_BOOKID + EQUALS + QMARK
					+ UNION_ALL + SELECT + TEXTBK_AUTHOR
					+ FROM + TEXTBOOKS
					+ WHERE + TEXTBK_ID + EQUALS + QMARK));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(where, 
				bookId, 
				bookId);
		final List<User> authors = UserRead.getUsers(sql_vals, !GET_DETAILS);
		final List<String> authorNames = new ArrayList<String>();
		for (final User author : authors) {
			authorNames.add(author.getName().toString1stName1st(
					author.prefersFamilyName1st()));
		} // for each author
		return authorNames.toArray(new String[authorNames.size()]);
	} // getAllAuthorNames(int)

	/** Get all verified instructors at an institution who are not coauthors
	 * of a particular textbook.
	 * @param	instnId	ID number of the institution
	 * @param	bookId	ID number of the coauthored textbook
	 * @return	array of User objects representing the instructors
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static User[] getNoncoauthors(int instnId, int bookId) 
			throws DBException {
		final String SELF = "TextbookRW.getNoncoauthors: ";
		if (instnId == 0 || bookId == 0) return new User[0];
		final String where = toString(
				WHERE + USER_ROLE + EQUALS, quotes(INSTRUCTOR),
				AND, bitand(USER_FLAGS, ENABLED), IS_NOT_ZERO 
				+ AND + USER_SCHOOLID + EQUALS + QMARK
				+ AND + USER_ID + NOT + IN, parens(
					SELECT + COAUTH_COAUTHOR
					+ FROM + COAUTHORS
					+ WHERE + COAUTH_BOOKID + EQUALS + QMARK));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(where, 
				instnId, 
				bookId);
		final List<User> users = UserRead.getUsers(sql_vals, !GET_DETAILS);
		return (users.toArray(new User[users.size()]));
	} // getNoncoauthors(int, int)

	/** Adds a coauthor to a textbook.
	 * @param	bookId	ID of the textbook
	 * @param	userId	login ID of the coauthor
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void addCoauthor(int bookId, String userId) 
			throws DBException {
		final String SELF = "TextbookRW.addCoauthor: ";
		final String[] fields = new String[] {
				COAUTH_BOOKID,
				COAUTH_COAUTHOR};
		final String qry = getInsertIntoValuesQMarksSQL(COAUTHORS, fields);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				bookId,
				userId);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		try {
			con = getPoolConnection();
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			alwaysPrint(SELF + "SQLException caught.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		} // try
	} // addCoauthor(int, String)

	/** Removes a coauthor from a textbook.
	 * @param	bookId	ID of the textbook
	 * @param	userId	login ID of the coauthor
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void removeCoauthor(int bookId, String userId) 
			throws DBException {
		final String SELF = "TextbookRW.removeCoauthor: ";
		final String qry =
				DELETE_FROM + COAUTHORS 
				+ WHERE + COAUTH_BOOKID + EQUALS + QMARK
				+ AND + COAUTH_COAUTHOR + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				bookId,
				userId);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		try {
			con = getPoolConnection();
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			alwaysPrint(SELF + "SQLException caught.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		} // try
	} // removeCoauthor(int, String)

	/** Sets the lock on a textbook so only one author may edit it, or clears it
	 * so that anyone may.
	 * @param	bookId	ID of the textbook
	 * @param	userId	login ID of the author
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setLockHolder(int bookId, String userId) 
			throws DBException {
		final String SELF = "TextbookRW.setLockHolder: ";
		final SQLWithQMarks sql_vals = new SQLWithQMarks(
				UPDATE + TEXTBOOKS 
				+ SET + TEXTBK_LOCKHOLDER + EQUALS);
		if (userId == null) {
			sql_vals.addToSql(NULL);
		} else {
			sql_vals.addToSql(QMARK);
			sql_vals.addValue(userId);
		} // if a user is specified
		sql_vals.addToSql(WHERE + TEXTBK_ID + EQUALS + QMARK);
		sql_vals.addValue(bookId);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		try {
			con = getPoolConnection();
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			alwaysPrint(SELF + "SQLException caught.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		} // try
	} // setLockHolder(int, String)

} // TextbookRW
