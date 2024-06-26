package com.epoch.db;

import static com.epoch.db.dbConstants.UserRWConstants.*;
import chemaxon.formats.MdlCompressor;
import com.epoch.exceptions.DBException;
import com.epoch.utils.DateUtils;
import com.epoch.utils.Utils;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/** Stores common SQL words and provides
 * utility functions for running a single update query, to get a pool 
 * connection, and to format SQL expressions.
 * <P>This class acts as superclass to all db classes so they can access its
 * constants and methods without using a prefix.  */
public class DBCommon {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

	/** Commonly used constant in SQL commands. */
	protected static final String NULL = "null";
	/** Parameter for getTables(). */
	protected static final boolean LOCAL = true;
	/** Parameter for getStrGroups(). */
	protected static final boolean ENQUOTE = true;
	/** Maximum number of values in an IN clause in an Oracle SQL statement.
	 * Actual limit is 1000, but playing it safe. */
	private static final int MAX_ORACLE_IN = 900; 

	/** Alternate name for search results. */
	protected static final String SRCH_RESULT = "srch_result";
	/** Commonly used constant in SQL commands. */
	protected static final String CALL = "CALL ";
	/** Commonly used constant in SQL commands. */
	protected static final String SELECT = "SELECT ";
	/** Commonly used constant in SQL commands. */
	protected static final String SELECT_UNIQUE = SELECT + "UNIQUE ";
	/** Commonly used constant in SQL commands. */
	protected static final String SELECT_DISTINCT = SELECT + "DISTINCT ";
	/** Commonly used constant in SQL commands. */
	protected static final String FROM = " FROM ";
	/** Commonly used constant in SQL commands. */
	protected static final String AS = " AS ";
	/** Commonly used constant in SQL commands. */
	protected static final String INSERT_INTO = "INSERT INTO ";
	/** Commonly used constant in SQL commands. */
	protected static final String UPDATE = "UPDATE ";
	/** Commonly used constant in SQL commands. */
	protected static final String FOR_UPDATE = " FOR " + UPDATE;
	/** Commonly used constant in SQL commands. */
	protected static final String DELETE_FROM = "DELETE" + FROM;
	/** Commonly used constant in SQL commands. */
	protected static final String WHERE = " WHERE ";
	/** Commonly used constant in SQL commands. */
	protected static final String SET = " SET ";
	/** Commonly used constant in SQL commands. */
	protected static final String ADD = " ADD ";
	/** Commonly used constant in SQL commands. */
	protected static final String LIKE = " LIKE ";
	/** Commonly used constant in SQL commands. */
	protected static final String IN = " IN ";
	/** Commonly used constant in SQL commands. */
	protected static final String NOT = " NOT ";
	/** Commonly used constant in SQL commands. */
	protected static final String AND = " AND ";
	/** Commonly used constant in SQL commands. */
	protected static final String OR = " OR ";
	/** Commonly used constant in SQL commands. */
	protected static final String IS_NULL = " IS " + NULL;
	/** Commonly used constant in SQL commands. */
	protected static final String IS_NOT_NULL = " IS" + NOT + NULL;
	/** Commonly used constant in SQL commands. */
	protected static final String EXISTS = " EXISTS ";
	/** Commonly used constant in SQL commands. */
	protected static final String NOT_EXISTS = NOT + EXISTS;
	/** Commonly used constant in SQL commands. */
	protected static final String ORDER_BY = " ORDER BY ";
	/** Commonly used constant in SQL commands. */
	protected static final String DESCENDING = " DESC ";
	/** Commonly used constant in SQL commands. */
	protected static final String GROUP_BY = " GROUP BY ";
	/** Commonly used constant in SQL commands. */
	protected static final String ROW_NUM = " ROWNUM ";
	/** Commonly used constant in SQL commands. */
	protected static final String HAVING = " HAVING ";
	/** Commonly used constant in SQL commands. */
	protected static final String UNION = " UNION ";
	/** Commonly used constant in SQL commands. */
	protected static final String UNION_ALL = UNION + "ALL ";
	/** Commonly used constant in SQL commands. */
	protected static final String JOIN = " JOIN ";
	/** Commonly used constant in SQL commands. */
	protected static final String ON = " ON ";
	/** Commonly used constant in SQL commands. */
	protected static final String BLOB = "BLOB";
	/** Commonly used constant in SQL commands. */
	protected static final String EQUALS = " = ";
	/** Commonly used constant in SQL commands. */
	protected static final String NOT_EQUALS = " != ";
	/** Commonly used constant in SQL commands. */
	protected static final String LESS_THAN = " < ";
	/** Commonly used constant in SQL commands. */
	protected static final String NOT_LESS_THAN = " >= ";
	/** Commonly used constant in SQL commands. */
	protected static final String MORE_THAN = " > ";
	/** Commonly used constant in SQL commands. */
	protected static final String NOT_MORE_THAN = " <= ";
	/** Commonly used constant in SQL commands. */
	protected static final String PLUS = " + ";
	/** Commonly used constant in SQL commands. */
	protected static final String MINUS = " - ";
	/** Commonly used constant in SQL commands. */
	protected static final String TIMES = " * ";
	/** Commonly used constant in SQL commands. */
	protected static final String IS_1 = EQUALS + "1 ";
	/** Commonly used constant in SQL commands. */
	protected static final String IS_ZERO = EQUALS + "0 ";
	/** Commonly used constant in SQL commands. */
	protected static final String IS_NOT_ZERO = NOT_EQUALS + "0 ";
	/** Commonly used constant in SQL commands. */
	protected static final String IS_POSITIVE = MORE_THAN + "0 ";
	/** Commonly used constant in SQL commands. */
	protected static final String IS_NEGATIVE = LESS_THAN + "0 ";
	/** Commonly used constant in SQL commands. */
	protected static final String COMMA = ", ";
	/** Commonly used constant in SQL commands. */
	protected static final String Y = quotes('Y');
	/** Commonly used constant in SQL commands. */
	protected static final String N = quotes('N');
	/** Commonly used constant in SQL commands. */
	protected static final String EMPTY = quotes("");
	/** Commonly used constant in SQL commands. */
	protected static final char DOT = '.';
	/** Commonly used constant in SQL commands. */
	protected static final char ALL = '*';
	/** Commonly used constant in SQL commands. */
	protected static final char QMARK = '?';
	/** Commonly used constant in SQL commands. */
	protected static final String UNITE = " || ";
	/** Parameter for Blob.setBinaryStream(). */
	protected static final long BLOB_BEGIN = 1L;

	/** Member of foreign key array. */
	protected static final int TABL_NUM = 0;
	/** Member of foreign key array. */
	protected static final int COL_NUM = 1;
	/** Member of foreign key array. */
	protected static final int FK_NAME = 2;

	/** Oracle's built-in dual table. */
	protected static final String DUMMY_TABLE = " dual ";

/* *************** Database query methods ******************/

	/** Gets master- or locally authored tables.
	 * @param	local	whether to get the locally authored tables or the master
	 * tables
	 * @return	a class member containing tables
	 */
	protected static DBTables getTables(boolean local) {
		return (local ? new DBLocalTables() : new DBTables());
	} // getTables(boolean)

	/** Executes a database update (INSERT INTO, UPDATE, DELETE FROM).
	 * Use only when there are no pieces of data included in the SQL; if there
	 * are, use ? in the SQL and tryUpdate() with variable arguments.
	 * @param	con	database connection
	 * @param	doThis	SQL statement
	 * @return	number of rows affected
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	protected static int tryUpdate(Connection con, String doThis)
			throws SQLException {
		final Statement stmt = con.createStatement();
		debugPrint("DBCommon.tryUpdate: ", doThis);
		int numRowsAffected = 0;
		try {
			numRowsAffected = stmt.executeUpdate(doThis);
		} finally {
			closeConnection(null, stmt, null);
		}
		return numRowsAffected;
	} // tryUpdate(Connection, String)

	/** Executes a database update (INSERT INTO, UPDATE, DELETE FROM)
	 * with one or more pieces of data to be inserted into the SQL.  
	 * @param	con	database connection
	 * @param	qry	database query beginning:
	 * <br>UPDATE table_name SET ... field_name = ?, ...
	 * <br>with any number of fields set to ?.
	 * <br>Can also be used with INSERT INTO and DELETE FROM statements.
	 * @param	data	data to store
	 * @return	number of rows affected
	 * @throws	SQLException	if there's a problem writing to the database
	 */ 
	protected static int tryUpdate(Connection con, String qry, 
			Object... data) throws SQLException {
		return tryUpdateWithArr(con, qry, data);
	} // tryUpdate(Connection, String, Object...)

	/** Executes a database update (INSERT INTO, UPDATE, DELETE FROM)
	 * with one or more pieces of data to be inserted into the SQL.  
	 * @param	con	database connection
	 * @param	sql_vals	SQL to execute, and values to
	 * substitute for question marks
	 * @return	number of rows affected
	 * @throws	SQLException	if there's a problem writing to the database
	 */ 
	protected static int tryUpdate(Connection con, 
			SQLWithQMarks sql_vals) throws SQLException {
		return tryUpdateWithArr(con, sql_vals.getSql(), sql_vals.getValues());
	} // tryUpdate(Connection, SQLWithQMarks)

	/** Executes a database update (INSERT INTO, UPDATE, DELETE FROM)
	 * with one or more pieces of data to be inserted into the SQL.  
	 * @param	con	database connection
	 * @param	qry	database query beginning:
	 * <br>UPDATE table_name SET ... field_name = ?, ...
	 * <br>with any number of fields set to ?.
	 * <br>Can also be used with INSERT INTO and DELETE FROM statements.
	 * @param	data	data to store
	 * @return	number of rows affected
	 * @throws	SQLException	if there's a problem writing to the database
	 */ 
	private static int tryUpdateWithArr(Connection con, String qry, 
			Object[] data) throws SQLException {
		final String SELF = "DBCommon.tryUpdate: ";
		final PreparedStatement preparedStmt = con.prepareStatement(qry);
		int numRowsAffected = 0;
		try {
			debugPrint(SELF, qry, "; ", data);
			setValuesWithArr(preparedStmt, 1, data);
			numRowsAffected = preparedStmt.executeUpdate();
		} finally {
			closeConnection(null, preparedStmt, null);
		}
		return numRowsAffected;
	} // tryUpdateWithArr(Connection, String, Object[])

	/** Gets a PreparedStatement with query and values for question marks already set.
	 * @param	con	database connection
	 * @param	sql_vals	SQL to execute, and values to
	 * substitute for question marks
	 * @return	the PreparedStatement with values set for question marks in the
	 * SQL
	 * @throws	SQLException	if there's a problem getting the statement
	 */
	protected static PreparedStatement getStatement(Connection con,
			SQLWithQMarks sql_vals) throws SQLException {
		return getStatementWithArr(con, sql_vals.getSql(), 
				sql_vals.getValues());
	} // getStatement(Connection, SQLWithQMarks)

	/** Gets a PreparedStatement with query and values for question marks already set.
	 * @param	con	database connection
	 * @param	qry	database query, possibly containing question marks
	 * @param	values	the values to set for the question marks
	 * @return	the PreparedStatement with values set for question marks in the
	 * SQL
	 * @throws	SQLException	if there's a problem getting the statement
	 */
	protected static PreparedStatement getStatement(Connection con,
			String qry, Object... values) throws SQLException {
		return getStatementWithArr(con, qry, values);
	} // getStatement(Connection, String, Object...)

	/** Gets a PreparedStatement with query and values for question marks already set.
	 * @param	con	database connection
	 * @param	qry	database query, possibly containing question marks
	 * @param	values	the values to set for the question marks
	 * @return	the PreparedStatement with values set for question marks in the
	 * SQL
	 * @throws	SQLException	if there's a problem getting the statement
	 */
	private static PreparedStatement getStatementWithArr(Connection con,
			String qry, Object[] values) throws SQLException {
		final PreparedStatement stmt = con.prepareStatement(qry);
		setValuesWithArr(stmt, 1, values);
		return stmt;
	} // getStatementWithArr(Connection, String, Object[])

	/** Sets the values for the question marks in a PreparedStatement; values
	 * may be an indefinite list of parameters or arrays of values.
	 * @param	stmt	the PreparedStatement
	 * @param	values	the values to set
	 * @return	the values joined by space-comma, for debug-printing
	 * @throws	SQLException	if there's a problem setting the value
	 */
	protected static StringBuilder setValues(PreparedStatement stmt, 
			Object... values) throws SQLException {
		setValuesWithArr(stmt, 1, values);
		return join(values);
	} // setValues(PreparedStatement, Object...)

	/** Sets the values for the question marks in a PreparedStatement.
	 * @param	stmt	the PreparedStatement
	 * @param	firstQMarkNum	where to start the numbering of the question
	 * marks
	 * @param	values	the values to set
	 * @throws	SQLException	if there's a problem setting the value
	 */
	private static void setValuesWithArr(PreparedStatement stmt, 
			int firstQMarkNum, Object[] values) throws SQLException {
		int qMarkNum = firstQMarkNum;
		for (final Object value : values) {
			qMarkNum = setValue(stmt, qMarkNum, value);
		} // for each value for a question mark
	} // setValuesWithArr(PreparedStatement, int, Object[])

	/** Sets a datum for a question mark in a PreparedStatement when the datum
	 * is of an unknown type.
	 * @param	stmt	the PreparedStatement
	 * @param	dataNum	which question mark will be substituted; 1-based
	 * @param	datum	the datum, as an Object
	 * @return	the next datum number, usually dataNum +1, but may be larger if
	 * the datum is an array
	 * @throws	SQLException	if there's a problem setting the value
	 */
	protected static int setValue(PreparedStatement stmt, int dataNum, 
			Object datum) throws SQLException {
		int nextDataNum = dataNum;
		if (datum instanceof Integer) {
			stmt.setInt(nextDataNum++, ((Integer) datum).intValue());
		} else if (datum instanceof Long) {
			stmt.setLong(nextDataNum++, ((Long) datum).longValue());
		} else if (datum instanceof Double) {
			stmt.setDouble(nextDataNum++, ((Double) datum).doubleValue());
		} else if (datum instanceof Character) {
			stmt.setString(nextDataNum++, 
					String.valueOf(((Character) datum).charValue()));
		} else if (datum instanceof byte[]) {
			stmt.setBytes(nextDataNum++, (byte[]) datum);
		} else if (datum instanceof int[]) {
			final int[] arr = (int[]) datum;
			for (final int elem : arr) {
				nextDataNum = setValue(stmt, nextDataNum, elem);
			} // for each int in the array
		} else if (datum instanceof Object[]) {
			final Object[] arr = (Object[]) datum;
			for (final Object elem : arr) {
				nextDataNum = setValue(stmt, nextDataNum, elem);
			} // for each element in the array
		} else if (datum instanceof List) {
			for (final Object elem : (List) datum) {
				nextDataNum = setValue(stmt, nextDataNum, elem);
			} // for each element in the array
		} else if (datum instanceof SQLWithQMarks) {
			final Object[] arr = ((SQLWithQMarks) datum).getValues();
			for (final Object elem : arr) {
				nextDataNum = setValue(stmt, nextDataNum, elem);
			} // for each element in the array
		} else if (datum instanceof StringBuilder) {
			stmt.setString(nextDataNum++, datum.toString());
		} else {
			stmt.setString(nextDataNum++, (String) datum);
		} // if type
		return nextDataNum;
	} // setValue(PreparedStatement, int, Object)

	/** Closes a database statement and results.
	 * No exceptions are expected, so all exceptions are just logged and
	 * ignored.
	 * @param	stmt	SQL statement
	 * @param	rs	result of the SQL statement 
	 */
	protected static void closeStmtAndRs(Statement stmt, ResultSet rs) {
		try { if (rs != null) rs.close(); }
		catch (Exception e) { e.printStackTrace(); }
		try { if (stmt != null) stmt.close(); }
		catch (Exception e) { e.printStackTrace(); }
	} // closeStmtAndRs(Statement, ResultSet)

	/** Closes a database connection, statement, results.
	 * No exceptions are expected, so all exceptions are just logged and
	 * ignored.
	 * @param	con	database connection
	 * @param	stmt	SQL statement
	 * @param	rs	result of the SQL statement 
	 */
	protected static void closeConnection(Connection con, 
			Statement stmt, ResultSet rs) {
		try { if (rs != null) rs.close(); }
		catch (Exception e) { e.printStackTrace(); }
		try { if (stmt != null) stmt.close(); }
		catch (Exception e) { e.printStackTrace(); }
		closeConnection(con);
	} // closeConnection(Connection, Statement, ResultSet)

	/** Closes a database connection.  No exceptions are expected, so all 
	 * exceptions are just logged and ignored.
	 * @param	con	database connection
	 */
	protected static void closeConnection(Connection con) {
		try { if (con != null) con.close(); }
		catch (Exception e) { e.printStackTrace(); }
	} // closeConnection(Connection)

	/** Rolls back a connection.  No exceptions are expected, so all 
	 * exceptions are just logged and ignored.
	 * @param	con	database connection
	 */
	protected static void rollbackConnection(Connection con) {
		alwaysPrint("DBCommon.rollbackConnection: rolling back");
		if (con != null) {
			try { con.rollback(); }
			catch (Exception e) { e.printStackTrace(); }
		}
	} // rollbackConnection(Connection)

	/** Uses connection pooling to return a DB connection that has autocommit
	 * set to true.
	 * @return	database connection
	 * @throws	SQLException	if there's a problem getting a connection from
	 * the pool
	 */
	protected static Connection getPoolConnection() throws SQLException {
		Connection con = null;
		try {
			final Context initContext = new InitialContext();
			final Context envContext = 
					(Context) initContext.lookup("java:/comp/env");
			final DataSource ds = 
					(DataSource) envContext.lookup("jdbc/acepool");
			con = ds.getConnection();
			con.setAutoCommit(true);
		} catch (NamingException e) {
			alwaysPrint("DBCommon.getPoolConnection: naming exception.");
		} 
		return con;
	} // getPoolConnection()

	/** Returns a new ID number from a sequencer. 
	 * @param	con	database connection
	 * @param	sequencer	sequencer for a table
	 * @return	next larger number 
	 */
	protected static int nextSequence(Connection con, String sequencer) {
		final String SELF = "DBCommon.nextSequence: ";
		int number = 0;
		final String query = toString(SELECT, sequencer, ".NEXTVAL" 
				+ AS + SRCH_RESULT 
				+ FROM + "TAB" 
				+ WHERE + ROW_NUM + IS_1);
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery(query);
			if (rs.next()) number = rs.getInt(SRCH_RESULT); 
			debugPrint(SELF + "next value of sequencer ",
					sequencer, " is ", number);
		} catch (SQLException e) {
			alwaysPrint(SELF + "can't get sequence number for ",
					sequencer);
			alwaysPrint(e.getMessage());
		} finally {
			closeStmtAndRs(stmt, rs);
		}
		return number;
	} // nextSequence(Connection, String)

/* *************** join() and related methods ******************/
/* join() works on a single array or a List to give a single 
 * StringBuilder made up of the array's or list's members. 
 * Use joinAll() to join an indeterminate number of items. */

	/** Joins an array of ints into a single StringBuilder
	 * joined by comma-spaces. 
	 * @param	items	ints to be joined
	 * @return	StringBuilder of joined ints and comma-spaces 
	 */
	protected static StringBuilder join(int[] items) {
		return join(items, COMMA);
	} // join(int[])

	/** Joins an array of ints into a single StringBuilder
	 * joined by separators. 
	 * @param	items	ints to be joined
	 * @param	separator	string to separate each pair of strings
	 * @return	StringBuilder of joined ints and separators
	 */
	protected static StringBuilder join(int[] items, String separator) {
		return join(Utils.toObject(items), separator);
	} // join(int[], String)

	/** Joins an array of chars into a single StringBuilder
	 * joined by comma-spaces. 
	 * @param	items	chars to be joined
	 * @return	StringBuilder of joined chars and comma-spaces 
	 */
	private static StringBuilder join(char[] items) {
		return join(items, COMMA);
	} // join(char[])

	/** Joins an array of chars into a single StringBuilder
	 * joined by separators. 
	 * @param	items	chars to be joined
	 * @param	separator	string to separate each pair of strings
	 * @return	StringBuilder of joined chars and separators
	 */
	private static StringBuilder join(char[] items, String separator) {
		return join(Utils.toObject(items), separator);
	} // join(char[], String)

	/** Joins a list of items into a single string joined by
	 * comma-spaces. 
	 * @param	<L>	type of Object in the list
	 * @param	items	list of items to be joined
	 * @return	string of joined items and comma-spaces 
	 */
	protected static <L> String join(List<L> items) {
		return Utils.join(items);
	} // join(List<L>)

	/** Joins a list of items into a single string. 
	 * @param	<L>	type of Object in the list
	 * @param	items	list of items to be joined
	 * @param	separator	string to separate each pair of items
	 * @return	string of joined items and separators 
	 */
	protected static <L> String join(List<L> items, String separator) {
		return Utils.join(items, separator);
	} // join(List<L>, String)

	/** Joins an array of objects into a single StringBuilder
	 * joined by comma-spaces. 
	 * @param	items	objects to be joined
	 * @return	StringBuilder of joined objects and comma-spaces 
	 */
	protected static StringBuilder join(Object[] items) {
		return join(items, COMMA);
	} // join(Object[])

	/** Joins an array of objects into a single StringBuilder
	 * joined by separators. 
	 * @param	items	objects to be joined
	 * @param	separator	string to separate each pair of strings
	 * @return	StringBuilder of joined objects and separators
	 */
	protected static StringBuilder join(Object[] items, String separator) {
		final StringBuilder bld = new StringBuilder();
		boolean first = true;
		for (final Object item : items) {
			if (first) first = false;
			else bld.append(separator);
			bld.append(item);
		} // for each item
		return bld;
	} // join(Object[], String)

	/** Joins an indeterminate number of objects into a single StringBuilder
	 * joined by comma-spaces. 
	 * @param	items	objects to be joined
	 * @return	StringBuilder of joined objects and comma-spaces 
	 */
	protected static StringBuilder joinAll(Object... items) {
		return join(items);
	} // joinAll(Object...)

	/** Joins an indeterminate number of objects into a single StringBuilder
	 * joined by comma-spaces and followed by comma-space. 
	 * @param	items	objects to be joined
	 * @return	StringBuilder of joined objects and comma-spaces 
	 */
	protected static StringBuilder prejoin(Object... items) {
		return join(items).append(COMMA);
	} // prejoin(Object...)

	/** Joins an indeterminate number of objects into a single StringBuilder
	 * led by comma-space and joined by comma-spaces. 
	 * @param	items	objects to be joined
	 * @return	StringBuilder of joined objects and comma-spaces 
	 */
	protected static StringBuilder postjoin(Object... items) {
		return join(items).insert(0, COMMA);
	} // postjoin(Object...)

	/** Joins an indeterminate number of objects into a single StringBuilder 
	 * joined by comma-spaces and surrounds it with (). 
	 * @param	items	strings to be joined
	 * @return	StringBuilder of joined objects and comma-spaces surrounded by ()
	 */
	protected static StringBuilder parensJoin(Object... items) {
		return parens(items);
	} // parensJoin(Object...)

	/** Joins a certain number of question marks into a single StringBuilder 
	 * joined by comma-spaces
	 * @param	numQMarks	number of question marks to be joined
	 * @return	StringBuilder of joined question marks and comma-spaces
	 */
	protected static StringBuilder getQMarks(int numQMarks) {
		final char[] qMarks = new char[numQMarks];
		Arrays.fill(qMarks, QMARK);
		return join(qMarks);
	} // getQMarks(int)

	/** Joins a certain number of question marks into a single StringBuilder 
	 * joined by comma-spaces
	 * @param	arr	values that will be represented by question marks
	 * @return	StringBuilder of joined question marks and comma-spaces
	 */
	protected static StringBuilder getQMarks(Object[] arr) {
		return getQMarks(arr == null ? 0 : arr.length);
	} // getQMarks(Object[])

	/** Joins a certain number of question marks into a single StringBuilder 
	 * joined by comma-spaces and surrounded by (). 
	 * @param	numQMarks	number of question marks to be joined
	 * @return	StringBuilder of joined question marks and comma-spaces
	 * surrounded by ()
	 */
	protected static StringBuilder parensQMarks(int numQMarks) {
		return parens(getQMarks(numQMarks));
	} // parensQMarks(int)

	/** Joins a certain number of question marks into a single StringBuilder 
	 * joined by comma-spaces and surrounded by (). 
	 * @param	arr	values that will be represented by question marks
	 * @return	StringBuilder of joined question marks and comma-spaces
	 * surrounded by ()
	 */
	protected static StringBuilder parensQMarks(int[] arr) {
		return parensQMarks(arr == null ? 0 : arr.length);
	} // parensQMarks(int[])

	/** Joins a certain number of question marks into a single StringBuilder 
	 * joined by comma-spaces and surrounded by (). 
	 * @param	arr	values that will be represented by question marks
	 * @return	StringBuilder of joined question marks and comma-spaces
	 * surrounded by ()
	 */
	protected static StringBuilder parensQMarks(Object[] arr) {
		return parensQMarks(arr == null ? 0 : arr.length);
	} // parensQMarks(Object[])

	/** Joins a certain number of question marks into a single StringBuilder 
	 * joined by comma-spaces and surrounded by (). 
	 * @param	list	values that will be represented by question marks
	 * @return	StringBuilder of joined question marks and comma-spaces
	 * surrounded by ()
	 */
	protected static StringBuilder parensQMarks(List<? extends Object> list) {
		return parensQMarks(list == null ? 0 : list.size());
	} // parensQMarks(List<? extends Object>)

	/** Joins an indeterminate number of objects into a single StringBuilder 
	 * joined by comma-spaces and surrounds it with VALUES (). 
	 * @param	items	objects to be joined
	 * @return	StringBuilder of joined objects and comma-spaces surrounded by 
	 * VALUES ()
	 */
	protected static StringBuilder valuesJoin(Object... items) {
		return values(items);
	} // valuesJoin(Object...)

	/** Joins a certain number of question marks into a single StringBuilder 
	 * joined by comma-spaces and surrounds it with VALUES (). 
	 * @param	numQMarks	number of question marks to be joined
	 * @return	StringBuilder of joined question marks and comma-spaces 
	 * surrounded by VALUES ()
	 */
	protected static StringBuilder valuesQMarks(int numQMarks) {
		final char[] qMarks = new char[numQMarks];
		Arrays.fill(qMarks, QMARK);
		return values(qMarks);
	} // valuesQMarks(int)

	/** Joins a certain number of question marks into a single StringBuilder 
	 * joined by comma-spaces and surrounds it with VALUES (). 
	 * @param	fields	array of fields corresponding to the question marks 
	 * to be joined
	 * @return	StringBuilder of joined question marks and comma-spaces 
	 * surrounded by VALUES ()
	 */
	protected static StringBuilder valuesQMarks(String[] fields) {
		return valuesQMarks(fields == null ? 0 : fields.length);
	} // valuesQMarks(String[])

	/** Joins a certain number of question marks into a single StringBuilder 
	 * joined by comma-spaces and surrounds it with VALUES (). 
	 * @param	fields	list of fields corresponding to the question marks 
	 * to be joined
	 * @return	StringBuilder of joined question marks and comma-spaces 
	 * surrounded by VALUES ()
	 */
	protected static StringBuilder valuesQMarks(List<String> fields) {
		return valuesQMarks(fields == null ? 0 : fields.size());
	} // valuesQMarks(List<String>)

	/** Joins an even number of objects into a single StringBuilder joined 
	 * by EQUALS and comma-spaces.
	 * @param	items	objects to be joined
	 * @return	String consisting of items[0] EQUALS items[1], ...
	 */
	protected static StringBuilder equalsJoin(Object... items) {
		final int numItems = items.length;
		final StringBuilder[] updateBlds = new StringBuilder[numItems / 2];
		for (int posn = 0; posn < numItems; posn += 2) {
			updateBlds[posn / 2] = 
					getBuilder(items[posn], EQUALS, items[posn + 1]);
		} // for each pair of values
		return join(updateBlds);
	} // equalsJoin(Object...)

	/** Joins an even number of objects in a List into a single StringBuilder 
	 * joined by EQUALS and comma-spaces.
	 * @param	items	objects to be joined
	 * @return	StringBuilder consisting of items[0] EQUALS items[1], ...
	 */
	protected static StringBuilder equalsJoin(List<Object> items) {
		return equalsJoin(items.toArray(new Object[items.size()]));
	} // equalsJoin(List<Object>)

	/** Joins objects into a single StringBuilder joined 
	 * by EQUALS ? and comma-spaces.
	 * @param	items	objects to be joined
	 * @return	StringBuilder consisting of items EQUALS ?, ...
	 */
	protected static StringBuilder equalsJoinQMarks(Object... items) {
		return equalsJoinQMarksArr(items);
	} // equalsJoinQMarks(Object...)

	/** Joins objects into a single StringBuilder joined 
	 * by EQUALS ? and comma-spaces.
	 * @param	items	objects to be joined
	 * @return	StringBuilder consisting of items EQUALS ?, ...
	 */
	protected static StringBuilder equalsJoinQMarksArr(Object[] items) {
		final int numItems = items.length;
		final StringBuilder[] updateBlds = new StringBuilder[numItems];
		for (int posn = 0; posn < numItems; posn++) {
			updateBlds[posn] = getBuilder(items[posn], EQUALS + QMARK);
		} // for each pair of values
		return join(updateBlds);
	} // equalsJoinQMarksArr(Object[])

	/** Joins field names in a List into a single StringBuilder 
	 * by EQUALS ? and comma-spaces.
	 * @param	items	field names to be joined
	 * @return	StringBuilder consisting of items EQUALS ?, ...
	 */
	protected static StringBuilder equalsJoinQMarksList(List<String> items) {
		return equalsJoinQMarksArr(items.toArray(new String[items.size()]));
	} // equalsJoinQMarksList(List<String>)

	/** Joins an indeterminate but even number of objects into a single 
	 * StringBuilder joined by EQUALS and comma-spaces with a leading
	 * comma-space.
	 * @param	items	objects to be joined
	 * @return	StringBuilder consisting of , items[0] EQUALS items[1], ...
	 */
	protected static StringBuilder addEqualsJoin(Object... items) {
		return getBuilder(COMMA, equalsJoin(items));
	} // addEqualsJoin(Object...)

	/** Joins an indeterminate number of objects into a single 
	 * StringBuilder joined by EQUALS ? and comma-spaces with a leading
	 * comma-space.
	 * @param	items	objects to be joined
	 * @return	StringBuilder consisting of , items[0] EQUALS ?, ...
	 */
	protected static StringBuilder addEqualsJoinQMarks(Object... items) {
		return getBuilder(COMMA, equalsJoinQMarksArr(items));
	} // addEqualsJoinQMarks(Object...)

/* *************** simple parens() methods ******************/

	/** Encloses an integer in parentheses. 
	 * @param	item	integer to be enclosed in parentheses
	 * @return	the integer enclosed in parentheses
	 */
	protected static String parens(int item) { 
		return parens(String.valueOf(item));
	} // parens(int)

	/** Encloses a char in parentheses. 
	 * @param	item	char to be enclosed in parentheses
	 * @return	the char enclosed in parentheses
	 */
	protected static String parens(char item) { 
		return parens(new StringBuilder().append(item)).toString();
	} // parens(char)

	/** Encloses a string in parentheses. 
	 * @param	item	string to be enclosed in parentheses
	 * @return	the string enclosed in parentheses
	 */
	protected static String parens(String item) { 
		return (item == null ? "()"
				: parens(new StringBuilder().append(item)).toString());
	} // parens(String)

	/** Encloses a StringBuilder in parentheses.  Modifies the StringBuilder!
	 * @param	item	StringBuilder to be enclosed in parentheses
	 * @return	reference to the modified StringBuilder enclosed in parentheses
	 */
	protected static StringBuilder parens(StringBuilder item) { 
		return item.insert(0, " (").append(')');
	} // parens(StringBuilder)

	/** Combines an indeterminate number of objects into a new StringBuilder 
	 * and surrounds it with parentheses; identical in effect to 
	 * parens(getBuilder()). Not to be confused with parens() which joins 
	 * items and separates them with commas.
	 * @param	items	the items to be combined into a string
	 * @return	the combined StringBuilder surrounded by parens
	 */
	protected static StringBuilder parensBuild(Object... items) {
		return parens(getBuilder(items));
	} // parensBuild(Object...)

/* *************** parens() methods that join multiple items ******************/

	/** Joins an array of objects into a single StringBuilder 
	 * joined by comma-spaces and surrounds it with (). 
	 * @param	items	array of objects to be joined
	 * @return	StringBuilder of joined objects and comma-spaces surrounded by ()
	 */
	protected static StringBuilder parens(Object[] items) {
		return parens(join(items));
	} // parens(Object[])

	/** Joins an array of ints into a single StringBuilder 
	 * joined by comma-spaces and surrounds it with (). 
	 * @param	items	array of ints to be joined
	 * @return	StringBuilder of joined ints and comma-spaces surrounded by ()
	 */
	protected static StringBuilder parens(int[] items) {
		return parens(join(Utils.toObject(items)));
	} // parens(int[])

	/** Joins an array of chars into a single StringBuilder 
	 * joined by comma-spaces and surrounds it with (). 
	 * @param	items	array of chars to be joined
	 * @return	StringBuilder of joined chars and comma-spaces surrounded by ()
	 */
	protected static StringBuilder parens(char[] items) {
		return parens(join(Utils.toObject(items)));
	} // parens(char[])

	/** Joins a List of objects into a single StringBuilder 
	 * joined by comma-spaces and surrounds it with (). 
	 * @param	<L>	type of Object in the list
	 * @param	items	List of objects to be joined
	 * @return	StringBuilder of joined objects and comma-spaces surrounded by ()
	 */
	protected static <L> StringBuilder parens(List<L> items) {
		return parens(join(items.toArray()));
	} // parens(List<L>)

	/** Joins a Set of objects into a single StringBuilder 
	 * joined by comma-spaces and surrounds it with (). 
	 * @param	<L>	type of Object in the set
	 * @param	items	Set of objects to be joined
	 * @return	StringBuilder of joined objects and comma-spaces surrounded by ()
	 */
	protected static <L> StringBuilder parens(Set<L> items) {
		return parens(join(items.toArray()));
	} // parens(Set<L>)

	/** Joins a collection of objects into a single StringBuilder 
	 * joined by comma-spaces and surrounds it with (). 
	 * @param	items	collection of objects to be joined
	 * @return	StringBuilder of joined objects and comma-spaces surrounded by ()
	 */
	protected static StringBuilder parens(Collection<Object> items) {
		return parens(join(items.toArray()));
	} // parens(Collection<Object>)

	/** Joins an array of strings into a single StringBuilder with each 
	 * array member surrounded by quotes and separated by comma-spaces. 
	 * @param	items	array of strings to be joined
	 * @return	string of joined strings and comma-spaces
	 */
	protected static StringBuilder parensQuotes(Object[] items) {
		return parens(join(toValidSQL(items), quotes(COMMA))
				.insert(0, '\'').append('\''));
	} // parensQuotes(Object[])

	/** Joins a list of strings into a single StringBuilder with each
	 * member surrounded by quotes and separated by comma-spaces. 
	 * @param	items	list of strings to be joined
	 * @return	string of joined strings and comma-spaces
	 */
	protected static StringBuilder parensQuotes(List<String> items) {
		return parensQuotes(items.toArray());
	} // parensQuotes(List<String>)

	/** Joins an indeterminate number of strings into a single StringBuilder 
	 * with each array member surrounded by quotes and separated by 
	 * comma-spaces. 
	 * @param	items	array of strings to be joined
	 * @return	string of joined quoted-strings and comma-spaces
	 */
	protected static StringBuilder parensJoinQuotes(Object... items) {
		return parensQuotes(items);
	} // parensJoinQuotes(Object...)

/* *********** Other methods that enclose in single characters ***********/

	/** Encloses a string in braces. 
	 * @param	item	string to be enclosed in braces
	 * @return	the string enclosed in braces
	 */
	protected static String braces(String item) { 
		return (item == null ? "{}" : toString(" {", item, '}'));
	} // braces(String)

	/** Encloses a StringBuilder in braces.  Modifies the StringBuilder!
	 * @param	item	StringBuilder to be enclosed in braces
	 * @return	reference to the modified StringBuilder enclosed in braces
	 */
	protected static StringBuilder braces(StringBuilder item) { 
		return item.insert(0, " {").append('}');
	} // braces(StringBuilder)

	/** Encloses an integer in percents. 
	 * @param	item	integer to be enclosed in percents
	 * @return	the integer enclosed in percents
	 */
	protected static String percent(int item) { 
		return percent(String.valueOf(item));
	} // percent(int)

	/** Encloses a string in percents. 
	 * @param	item	string to be enclosed in percents
	 * @return	the string enclosed in percents
	 */
	protected static String percent(String item) { 
		return (item == null ? "%%" : toString(" %", item, '%'));
	} // percent(String)

	/** Encloses a StringBuilder in percents.  Modifies the StringBuilder!
	 * @param	item	StringBuilder to be enclosed in percents
	 * @return	reference to the modified StringBuilder enclosed in percents
	 */
	protected static StringBuilder percent(StringBuilder item) { 
		return item.insert(0, '%').append('%');
	} // percent(StringBuilder)

	/** Encloses a character in single quotes. 
	 * @param	item	character to be enclosed in single quotes
	 * @return	the character enclosed in single quotes
	 */
	protected static String quotes(char item) { 
		return (item == '\'' ? "''''" : toString('\'', item, '\''));
	} // quotes(char)

	/** Encloses an integer in single quotes. 
	 * @param	item	integer to be enclosed in single quotes
	 * @return	the integer enclosed in single quotes
	 */
	protected static String quotes(int item) { 
		return toString('\'', item, '\'');
	} // quotes(int)

	/** Returns NULL for a null string, otherwise encloses a string in single 
	 * quotes. 
	 * @param	item	string to be enclosed in single quotes
	 * @return	the string enclosed in single quotes
	 */
	protected static String quotes(String item) { 
		return (item == null ? NULL 
				: quotes(new StringBuilder(item)).toString());
	} // quotes(String)

	/** Encloses a StringBuilder in single quotes.  Modifies the StringBuilder!
	 * @param	item	StringBuilder to be enclosed in single quotes
	 * @return	reference to the modified StringBuilder enclosed in single
	 * quotes
	 */
	protected static StringBuilder quotes(StringBuilder item) { 
		toValidSQL(item);
		return item.insert(0, '\'').append('\'');
	} // quotes(StringBuilder)

/* *************** Methods that apply SQL methods ******************/

	/** Subjects a string to greatest().
	 * @param	item	string to be enclosed
	 * @return	the string enclosed in greatest()
	 */
	protected static String greatest(String item) { 
		return fn("GREATEST", item);
	} // greatest(String)

	/** Subjects a StringBuilder to greatest(). Modifies the StringBuilder!
	 * @param	bld	StringBuilder to be enclosed
	 * @return	the StringBuilder enclosed in greatest()
	 */
	protected static StringBuilder greatest(StringBuilder bld) { 
		return fn("GREATEST", bld);
	} // greatest(StringBuilder)

	/** Subjects a char to max().
	 * @param	item	char to be enclosed
	 * @return	the char enclosed in max()
	 */
	protected static String max(char item) { 
		return fn("MAX", item);
	} // max(char)

	/** Subjects a string to max().
	 * @param	item	string to be enclosed
	 * @return	the string enclosed in max()
	 */
	protected static String max(String item) { 
		return fn("MAX", item);
	} // max(String)

	/** Subjects a StringBuilder to max(). Modifies the StringBuilder!
	 * @param	bld	StringBuilder to be enclosed
	 * @return	the StringBuilder enclosed in max()
	 */
	protected static StringBuilder max(StringBuilder bld) { 
		return fn("MAX", bld);
	} // max(StringBuilder)

	/** Subjects a string to min().
	 * @param	item	string to be enclosed
	 * @return	the string enclosed in min()
	 */
	protected static String min(String item) { 
		return fn("MIN", item);
	} // min(String)

	/** Subjects a StringBuilder to min(). Modifies the StringBuilder!
	 * @param	bld	StringBuilder to be enclosed
	 * @return	the StringBuilder enclosed in min()
	 */
	protected static StringBuilder min(StringBuilder bld) { 
		return fn("MIN", bld);
	} // min(StringBuilder)

	/** Gets count(1).
	 * @return	count(1)
	 */
	protected static String count() { 
		return count("1"); 
	} // count()

	/** Counts the number of values found in the column
	 * or the number of records meeting the criteria.
	 * @param	item	the field's name, or * or 1 to count records 
	 * @return	count(item)
	 */
	protected static String count(String item) { 
		return fn("COUNT", item);
	} // count(String)

	/** Subjects a char to sum().
	 * @param	item	char to be enclosed
	 * @return	the char enclosed in sum()
	 */
	protected static String sum(char item) { 
		return fn("SUM", item);
	} // sum(char)

	/** Subjects a string to sum().
	 * @param	item	string to be enclosed
	 * @return	the string enclosed in sum()
	 */
	protected static String sum(String item) { 
		return fn("SUM", item);
	} // sum(String)

	/** Subjects a StringBuilder to sum(). Modifies the StringBuilder!
	 * @param	bld	StringBuilder to be enclosed
	 * @return	the StringBuilder enclosed in sum()
	 */
	protected static StringBuilder sum(StringBuilder bld) { 
		return fn("SUM", bld);
	} // sum(StringBuilder)

	/** Subjects a string to length().  Can be applied to CLOBs.
	 * @param	item	string to be enclosed
	 * @return	the string enclosed in length()
	 */
	protected static String length(String item) { 
		return fn("LENGTH", item);
	} // length(String)

	/** Subjects a StringBuilder to length().  Can be applied to CLOBs.
	 * @param	item	StringBuilder to be enclosed
	 * @return	the StringBuilder enclosed in length()
	 */
	protected static StringBuilder length(StringBuilder item) { 
		return fn("LENGTH", item);
	} // length(StringBuilder)

	/** Subjects a string to trim().
	 * @param	item	string to be enclosed
	 * @return	the string enclosed in trim()
	 */
	protected static String trim(String item) { 
		return fn("TRIM", item);
	} // trim(String)

	/** Subjects a StringBuilder to trim(). Modifies the StringBuilder!
	 * @param	bld	StringBuilder to be enclosed
	 * @return	the StringBuilder enclosed in trim()
	 */
	protected static StringBuilder trim(StringBuilder bld) { 
		return fn("TRIM", bld);
	} // trim(StringBuilder)

	/** Subjects a string to to_number().  Cannot be applied to CLOBs.
	 * @param	item	string to be enclosed
	 * @return	the string enclosed in to_number()
	 */
	protected static String toNumber(String item) { 
		return fn("TO_NUMBER", item);
	} // toNumber(String)

	/** Subjects a stringbuilder to to_number().  Cannot be applied to CLOBs.
	 * @param	item	string to be enclosed
	 * @return	the string enclosed in to_number()
	 */
	protected static StringBuilder toNumber(StringBuilder item) { 
		return fn("TO_NUMBER", item);
	} // toNumber(StringBuilder)

	/** Subjects a string to to_char().  Cannot be applied to CLOBs.
	 * @param	item	string to be enclosed
	 * @return	the string enclosed in to_char()
	 */
	protected static String toVarChar(String item) { 
		return fn("TO_CHAR", item);
	} // toVarChar(String)

	/** Subjects a stringbuilder to to_char().  Cannot be applied to CLOBs.
	 * @param	item	string to be enclosed
	 * @return	the string enclosed in to_char()
	 */
	protected static StringBuilder toVarChar(StringBuilder item) { 
		return fn("TO_CHAR", item);
	} // toVarChar(StringBuilder)

	/** Subjects a string to nlssort().  Not useful at this time because we
	 * store all non-ASCII characters as character entity references. Cannot 
	 * be applied to CLOBs.
	 * @param	item	string to be enclosed
	 * @return	the string enclosed in nlssort() along with a default 
	 * linguistic sorting method that ignores diacritic marks
	 */
	protected static StringBuilder ignoreDiacritics(String item) { 
		return fn("NLSSORT", joinAll(item, "'NLS_SORT=BINARY_AI'"));
	} // ignoreDiacritics(String)

	/** Subjects a stringbuilder to nlssort().  Not useful at this time because we
	 * store all non-ASCII characters as character entity references. Cannot 
	 * be applied to CLOBs.
	 * @param	item	string to be enclosed
	 * @return	the string enclosed in nlssort() along with a default 
	 * linguistic sorting method that ignores diacritic marks
	 */
	protected static StringBuilder ignoreDiacritics(StringBuilder item) { 
		return fn("NLSSORT", joinAll(item, "'NLS_SORT=BINARY_AI'"));
	} // ignoreDiacritics(StringBuilder)

	/** Subjects a string to to_date().
	 * @param	dateStr	date in the format of a string
	 * @param	dateFormat	format of the date string, e.g., 
	 * 'YYYY/DD/MM HH24:MI:SS'
	 * @return	the string enclosed in to_date()
	 */
	protected static StringBuilder toDate(String dateStr, String dateFormat) { 
		return fn("TO_DATE", joinAll(dateStr, dateFormat));
	} // toDate(String, String)

	/** Subjects a character to nls_upper().  Cannot be applied to CLOBs: use
	 * clobToUpper() instead.
	 * @param	item	character to be enclosed
	 * @return	the character enclosed in nls_upper()
	 */
	protected static String toUpper(char item) { 
		return toUpper(String.valueOf(item));
	} // toUpper(char)

	/** Subjects a string to nls_upper().  Cannot be applied to CLOBs: use
	 * clobToUpper() instead.
	 * @param	item	string to be enclosed
	 * @return	the string enclosed in nls_upper()
	 */
	protected static String toUpper(String item) { 
		return fn("NLS_UPPER", item);
	} // toUpper(String)

	/** Subjects a string to abs() (absolute value).  
	 * @param	item	string to be enclosed
	 * @return	the string enclosed in abs()
	 */
	protected static String abs(String item) { 
		return fn("ABS", item);
	} // abs(String)

	/** Subjects a StringBuilder to nls_upper(). Modifies the StringBuilder!
	 * @param	bld	StringBuilder to be enclosed
	 * @return	the StringBuilder enclosed in nls_upper()
	 */
	protected static StringBuilder toUpper(StringBuilder bld) { 
		return fn("NLS_UPPER", bld);
	} // toUpper(StringBuilder)

	/** Subjects a string and mask to bitand().
	 * @param	item	string to be enclosed
	 * @param	mask	the mask
	 * @return	StringBuilder with the string and mask enclosed in bitand()
	 */
	protected static StringBuilder bitand(String item, int mask) { 
		return fn("BITAND", joinAll(item, mask));
	} // bitand(String, int)

	/** Subjects a string and mask to bitand().
	 * @param	item	string to be enclosed
	 * @param	mask	the mask
	 * @return	StringBuilder with the string and mask enclosed in bitand()
	 */
	protected static StringBuilder bitand(String item, long mask) { 
		return fn("BITAND", joinAll(item, mask));
	} // bitand(String, long)

	/** Subjects a string and mask to decode().
	 * @param	expr	expression whose value is calculated
	 * @param	mayEqual	value to compare to the expression for equality
	 * @param	ifYes	value to return if the expression equals mayEqual
	 * @param	ifNo	value to return if the expression doesn't equal mayEqual
	 * @return	StringBuilder with the string and mask enclosed in decode()
	 */
	protected static StringBuilder decode(StringBuilder expr, double mayEqual,
			double ifYes, double ifNo) { 
		return fn("DECODE", joinAll(expr, mayEqual, ifYes, ifNo));
	} // decode(StringBuilder, double, double, double)

	/** Subjects a string and mask to decode().
	 * @param	expr	expression whose value is calculated
	 * @param	mayEqual	value to compare to the expression for equality
	 * @param	ifYes	field's value to return if the expression equals mayEqual
	 * @param	ifNo	value to return if the expression doesn't equal mayEqual
	 * @return	StringBuilder with the string and mask enclosed in decode()
	 */
	protected static StringBuilder decode(StringBuilder expr, double mayEqual,
			String ifYes, double ifNo) { 
		return fn("DECODE", joinAll(expr, mayEqual, ifYes, ifNo));
	} // decode(StringBuilder, double, String, double)

	/** Surrounds a string (usually BLOB or CLOB) with empty_ and ().
	 * @param	item	string to be surrounded
	 * @return	the string surrounded in empty_ and ()
	 */
	protected static String empty(String item) { 
		return toString("EMPTY_", item, "()");
	} // empty(String)

	/** Joins an array of characters and surrounds them with values ().
	 * @param	items	array of characters
	 * @return	the array members separated by commas and surrounded 
	 * in values ()
	 */
	protected static StringBuilder values(char[] items) { 
		return values(join(items));
	} // values(char[])

	/** Joins an array of objects and surrounds them with values ().
	 * @param	items	array of objects
	 * @return	the array members separated by commas and surrounded 
	 * in values ()
	 */
	protected static StringBuilder values(Object[] items) { 
		return values(join(items));
	} // values(Object[])

	/** Joins a list of objects and surrounds them with values ().
	 * @param	items	list of objects
	 * @return	the list members separated by commas and surrounded 
	 * in values ()
	 */
	protected static String values(List<String> items) { 
		return values(join(items));
	} // values(List)

	/** Surrounds a String with values ().
	 * @param	bld	String to be surrounded
	 * @return	the String surrounded in values ()
	 */
	protected static String values(String bld) { 
		return fn("VALUES ", bld);
	} // values(String)

	/** Surrounds a StringBuilder with values (). Modifies the StringBuilder!
	 * @param	bld	StringBuilder to be surrounded
	 * @return	the StringBuilder surrounded in values ()
	 */
	protected static StringBuilder values(StringBuilder bld) { 
		return fn("VALUES ", bld);
	} // values(StringBuilder)

	/** Returns an SQL expression that gets a substring of a field's value.
	 * @param	field	field containing the string
	 * @param	start	the position where the substring begins
	 * @param	length	the length of the substring to return
	 * @return	the SQL expression: SUBSTR(field, start, length)
	 */
	protected static StringBuilder substr(String field, int start, int length) { 
		return fn("SUBSTR", joinAll(field, start, length));
	} // substr(String, int, int)

	/** Returns an SQL expression that casts a string value to an integer 
	 * value.
	 * @param	field	field containing the string
	 * @return	the SQL expression:  CAST(field AS INTEGER)
	 */
	protected static StringBuilder castAsInt(String field) { 
		return fn("CAST", parensBuild(field, " AS INTEGER"));
	} // castAsInt(String)

	/** Returns an SQL expression that casts a string value to an integer 
	 * value.
	 * @param	field	field containing the string
	 * @return	the SQL expression:  CAST(field AS INTEGER)
	 */
	protected static StringBuilder castAsInt(StringBuilder field) { 
		return fn("CAST", parensBuild(field, " AS INTEGER"));
	} // castAsInt(StringBuilder)

	/** Gets a substring of a field, which may be a CLOB field.
	 * @param	field	a field
	 * @param	leftTrim	number of characters to trim from the left
	 * @param	rightTrim	number of characters to trim from the  right
	 * @return	a StringBuilder containing 
	 * dbms_lob.substr(field, length(field) - (leftTrim + rightTrim), leftTrim + 1) 
	 * which has the effect of returning the value trimmed by the appropriate
	 * number of characters on the left and right
	 */
	protected static StringBuilder trim(String field, int leftTrim, 
			int rightTrim) {
		final StringBuilder strLen = 
				getBuilder(length(field), " - ", leftTrim + rightTrim);
		return fn("DBMS_LOB.SUBSTR", joinAll(field, strLen, leftTrim + 1));
	} // trim(String, int, int)

	/** Returns an SQL expression that returns the position of a regular
	 * expression in a field, where the regular expression is represented by
	 * a question mark.  The method can be used on CLOBs.
	 * @param	field	field containing the values to be searched
	 * @return	the SQL expression: REGEXP_INSTR(field, ?)
	 */
	protected static StringBuilder regexp_instrQMark(String field) { 
		return regexp_instr(field, new StringBuilder(QMARK));
	} // regexp_instrQMark(String)

	/** Returns an SQL expression that returns the position of a regular
	 * expression in a field.  The method can be used on CLOBs.
	 * @param	field	field containing the values to be searched
	 * @param	regExp	regular expression for which to search
	 * @return	the SQL expression: REGEXP_INSTR(field, regExp)
	 */
	protected static StringBuilder regexp_instr(String field, String regExp) { 
		return regexp_instr(field, new StringBuilder(regExp));
	} // regexp_instr(String, String)

	/** Returns an SQL expression that returns the position of a regular
	 * expression in a field.  The method can be used on CLOBs.
	 * @param	field	field containing the values to be searched
	 * @param	regExp	regular expression for which to search
	 * @return	the SQL expression: REGEXP_INSTR(field, regExp)
	 */
	protected static StringBuilder regexp_instr(String field, 
			StringBuilder regExp) { 
		return fn("REGEXP_INSTR", joinAll(field, regExp));
	} // regexp_instr(String, StringBuilder)

	/** Returns an SQL expression that returns a substring corresponding to 
	 * a regular expression in a field.  The method can be used on CLOBs.
	 * @param	field	field containing the values to be searched
	 * @param	regExp	regular expression for which to search
	 * @param	start	the position at which to start the search
	 * @param	instanceFld	the field containing the value of which instance 
	 * of the substring to return
	 * @return	the SQL expression: REGEXP_SUBSTR(field, regExp, start,
	 * instanceFld)
	 */
	protected static StringBuilder regexp_substr(String field, String regExp, 
			int start, String instanceFld) { 
		return fn("REGEXP_SUBSTR", 
				joinAll(field, quotes(regExp), start, instanceFld));
	} // regexp_substr(String, String, int, String)

	/** Returns an SQL expression that returns a string in which a
	 * regular expression in the value of a field is deleted.
	 * The method can be used on CLOBs.
	 * @param	field	field containing the values to be searched
	 * @param	regExp	regular expression for which to search
	 * @return	the SQL expression: REGEXP_REPLACE(field, 'regExp')
	 */
	protected static StringBuilder regexp_replace(String field, 
			String regExp) { 
		return fn("REGEXP_REPLACE", joinAll(field, quotes(regExp)));
	} // regexp_replace(String, String)

	/** Returns an SQL expression that returns a string in which a
	 * regular expression in the value of a field is deleted.
	 * The method can be used on CLOBs.
	 * @param	field	field containing the values to be searched
	 * @param	regExp	regular expression for which to search
	 * @return	the SQL expression: REGEXP_REPLACE(field, 'regExp')
	 */
	protected static StringBuilder regexp_replace(StringBuilder field, 
			String regExp) { 
		return fn("REGEXP_REPLACE", joinAll(field, quotes(regExp)));
	} // regexp_replace(StringBuilder, String)

	/** Returns an SQL expression that returns a string in which a
	 * regular expression in the value of a field is replaced by another
	 * string.  The method can be used on CLOBs.
	 * @param	field	field containing the values to be searched
	 * @param	regExp	regular expression for which to search
	 * @param	replaceStr	replacement string
	 * @return	the SQL expression: 
	 * REGEXP_REPLACE(field, 'regExp', 'replaceStr')
	 */
	protected static String regexp_replace(String field, String regExp, 
			String replaceStr) { 
		return fn("REGEXP_REPLACE", 
				joinAll(field, quotes(regExp), quotes(replaceStr))).toString();
	} // regexp_replace(String, String, String)

	/** Returns an SQL expression that returns a string in which
	 * one character in a field's value is replaced by another.
	 * The method can be used on CLOBs.
	 * @param	field	field containing the values to be searched
	 * @param	origChar	character to replace
	 * @param	newChar	replacement character
	 * @return	the SQL expression: TRANSLATE(field, 'origChar', 'newChar')
	 */
	protected static String translate(String field, char origChar, 
			char newChar) { 
		return fn("TRANSLATE", 
				joinAll(field, quotes(origChar), quotes(newChar))).toString();
	} // translate(String, char, char)

	/** Returns an SQL expression that returns a string in which
	 * one group of characters in a field's value is replaced by another.
	 * The method can be used on CLOBs.
	 * @param	field	field containing the values to be searched
	 * @param	origChars	characters to replace
	 * @param	newChars	replacement characters
	 * @return	the SQL expression: TRANSLATE(field, 'origChars', 'newChars')
	 */
	protected static String translate(String field, String origChars, 
			String newChars) { 
		return fn("TRANSLATE", 
				joinAll(field, quotes(origChars), quotes(newChars))).toString();
	} // translate(String, String, String)

	/** Returns an SQL expression that returns a string in which
	 * one character in a field's value is replaced by another.
	 * The method can be used on CLOBs.
	 * @param	field	field containing the values to be searched
	 * @param	origChar	character to replace
	 * @param	newChar	replacement character
	 * @return	the SQL expression: TRANSLATE(field, 'origChar', 'newChar')
	 */
	protected static StringBuilder translate(StringBuilder field, 
			char origChar, char newChar) { 
		return fn("TRANSLATE", 
				joinAll(field, quotes(origChar), quotes(newChar)));
	} // translate(StringBuilder, char, char)

	/** Returns an SQL expression that returns a string in which
	 * one group of characters in a field's value is replaced by another.
	 * The method can be used on CLOBs.
	 * @param	field	field containing the values to be searched
	 * @param	origChars	characters to replace
	 * @param	newChars	replacement characters
	 * @return	the SQL expression: TRANSLATE(field, 'origChars', 'newChars')
	 */
	protected static StringBuilder translate(StringBuilder field, 
			String origChars, String newChars) { 
		return fn("TRANSLATE", 
				joinAll(field, quotes(origChars), quotes(newChars)));
	} // translate(StringBuilder, String, String)

	/** Converts a CLOB field to a string of maximum length 4000.
	 * @param	field	a CLOB field
	 * @return	a StringBuilder containing the string surrounded by 
	 * dbms_lob.substr() 
	 */
	protected static StringBuilder clobToString(String field) {
		return fn("DBMS_LOB.SUBSTR", joinAll(field, length(field), 1));
	} // clobToString(String)

	/** Converts a CLOB field to a string of maximum length 4000.
	 * @param	field	a CLOB field
	 * @return	a StringBuilder containing the string surrounded by 
	 * dbms_lob.substr() 
	 */
	protected static StringBuilder clobToString(StringBuilder field) {
		return fn("DBMS_LOB.SUBSTR", joinAll(field, length(field), 1));
	} // clobToString(StringBuilder)

	/** Converts a CLOB field to a string of maximum length 4000 with the same
	 * field name as the original field.
	 * @param	field	a CLOB field
	 * @return	a StringBuilder containing the string surrounded by 
	 * dbms_lob.substr() 
	 */
	protected static StringBuilder clobToStringAs(String field) {
		return getBuilder(clobToString(field), AS, field);
	} // clobToStringAs(String)

	/** Converts a CLOB field to a string of maximum length 100,000 and subjects
	 * it to nls_upper().
	 * @param	field	a CLOB field
	 * @return	a StringBuilder containing the string surrounded by 
	 * nls_upper(dbms_lob.substr()) 
	 */
	protected static StringBuilder clobToUpper(String field) {
		return toUpper(clobToString(field));
	} // clobToUpper(String)

	/** Returns an SQL expression that returns true if a string 
	 * equals the entire value of a CLOB.
	 * @param	field	field containing the values to be searched
	 * @param	str	string for which to search
	 * @return	the SQL expression: REGEXP_INSTR(field, '^str$') = 1
	 */
	protected static StringBuilder clobEquals(String field, String str) { 
		return getBuilder(
				regexp_instr(field, 
					quotes(getBuilder('^', escapeMetachars(str), '$'))), 
				IS_1);
	} // clobEquals(String, String)

	/** Returns an SQL expression that returns true if a regular expression, 
	 * whose value will be substituted for a question mark, is found in a CLOB.
	 * @param	field	field containing the values to be searched
	 * @return	the SQL expression: REGEXP_INSTR(field, ?) &gt;= 1
	 */
	protected static StringBuilder clobContainsQMark(String field) { 
		return clobContains(field, new StringBuilder(QMARK));
	} // clobContainsQMark(String)

	/** Returns an SQL expression that returns true if a regular expression 
	 * is found in a CLOB.
	 * @param	field	field containing the values to be searched
	 * @param	regExp	regular expression for which to search
	 * @return	the SQL expression: REGEXP_INSTR(field, regExp) &gt;= 1
	 */
	protected static StringBuilder clobContains(String field, String regExp) { 
		return clobContains(field, new StringBuilder(regExp));
	} // clobContains(String, String)

	/** Returns an SQL expression that returns true if a regular expression 
	 * is found in a CLOB.
	 * @param	field	field containing the values to be searched
	 * @param	regExp	regular expression for which to search
	 * @return	the SQL expression: REGEXP_INSTR(field, regExp) &gt;= 1
	 */
	protected static StringBuilder clobContains(String field, 
			StringBuilder regExp) { 
		return getBuilder(regexp_instr(field, regExp), NOT_LESS_THAN + "1 ");
	} // clobContains(String, StringBuilder)

	/** Returns an SQL phrase that will order records by the value of a CLOB.
	 * @param	field	field containing the sort key
	 * @return	the SQL expression: 
	 * ORDER BY DBMS_LOB.SUBSTR(field, length(field), 1)
	 */
	protected static StringBuilder orderByClob(String field) { 
		return getBuilder(ORDER_BY, clobToString(field));
	} // orderByClob(String)

	/** Subjects a string to a function().
	 * @param	fn	name of the function
	 * @param	item	string to be enclosed
	 * @return	the string enclosed in fn()
	 */
	protected static String fn(String fn, String item) { 
		return fn(fn, new StringBuilder().append(item)).toString();
	} // fn(String, String)

	/** Subjects a char to a function().
	 * @param	fn	name of the function
	 * @param	item	char to be enclosed
	 * @return	the char enclosed in fn()
	 */
	protected static String fn(String fn, char item) { 
		return fn(fn, new StringBuilder().append(item)).toString();
	} // fn(String, char)

	/** Subjects an int to a function().
	 * @param	fn	name of the function
	 * @param	item	int to be enclosed
	 * @return	the int enclosed in fn()
	 */
	protected static String fn(String fn, int item) { 
		return fn(fn, getBuilder(item)).toString();
	} // fn(String, int)

	/** Subjects a long to a function().
	 * @param	fn	name of the function
	 * @param	item	long to be enclosed
	 * @return	the long enclosed in fn()
	 */
	protected static String fn(String fn, long item) { 
		return fn(fn, getBuilder(item)).toString();
	} // fn(String, long)

	/** Subjects a StringBuilder to a function(). Modifies the StringBuilder!
	 * @param	fn	name of the function
	 * @param	bld	StringBuilder to be enclosed
	 * @return	the StringBuilder enclosed in fn()
	 */
	protected static StringBuilder fn(String fn, StringBuilder bld) { 
		return bld.insert(0, '(').insert(0, fn).insert(0, ' ').append(") ");
	} // fn(String, StringBuilder)

/* *************** Methods for building SQL queries ******************/

	/** Converts all single quotes in a string into a pair of single quotes.
	 * @param	s	a string
	 * @return	a string with all ' replaced with ''
	 */
	protected static String toValidSQL(String s) {
		return (s == null ? "" 
				: toValidSQL(new StringBuilder(s)).toString());
	} // toValidSQL(String)

	/** Converts all single quotes in a StringBuilder into a pair of single 
	 * quotes. Modifies the StringBuilder!
	 * @param	s	a StringBuilder
	 * @return	reference to the modified StringBuilder with all ' replaced 
	 * with ''
	 */
	protected static StringBuilder toValidSQL(StringBuilder s) {
		int posn = 0;
		while (posn < s.length()) {
			if (s.charAt(posn) == '\'') {
				s.insert(posn, '\'');
				posn += 2;
			} else posn++;
		} // until the end
		return s;
	} // toValidSQL(StringBuilder)

	/** Converts all single quotes in an array of strings into a pair of single 
	 * quotes.
	 * @param	strs	an array of Strings, as an Object[]
	 * @return	array of strings, as an Object[], with all ' replaced with ''
	 */
	private static Object[] toValidSQL(Object[] strs) {
		if (isEmpty(strs) || !(strs instanceof String[])) return strs;
		for (int strNum = 0; strNum < strs.length; strNum++) {
			strs[strNum] = toValidSQL((String) strs[strNum]);
		} // for each string
		return strs;
	} // toValidSQL(Object[])

	/** Escapes regular expression metacharacters in a string that is being
	 * searched.
	 * @param	str	the string
	 * @return	StringBuilder with all regular expression metacharacters
	 * preceded by \
	 */
	private static StringBuilder escapeMetachars(String str) {
		final StringBuilder bld = new StringBuilder();
		if (str == null) return bld;
		final String METACHARS = ".+?*[({|\\^$";
		for (final char ch : str.toCharArray()) {
			if (METACHARS.indexOf(ch) >= 0) bld.append('\\');
			bld.append(ch);
		} // for each character in string
		return bld;
	} // escapeMetachars(String)

	/** Returns the SQL statement INSERT INTO table (field1, field2, ...)
	 * VALUES (?, ?, ...).
	 * @param	table	the name of the table
	 * @param	fields	the names of the fields
	 * @return	the SQL statement
	 */
	protected static String getInsertIntoValuesQMarksSQL(String table, 
			String[] fields) {
		return toString(
				INSERT_INTO, table, parens(fields),
				valuesQMarks(fields));
	} // getInsertIntoValuesQMarks(String, String[])

	/** Returns the SQL statement INSERT INTO table (field1, field2, ...)
	 * VALUES (?, ?, ...).
	 * @param	table	the name of the table
	 * @param	fields	the names of the fields
	 * @return	the SQL statement
	 */
	protected static String getInsertIntoValuesQMarksSQL(String table, 
			List<String> fields) {
		return getInsertIntoValuesQMarksSQL(table, 
				fields.toArray(new String[fields.size()]));
	} // getInsertIntoValuesQMarks(String, List<String>)

	/** Gets SQL to insert into a table if a record doesn't already exist:
	 * <pre>
	 * 	INSERT INTO table (fields[0], fields[1], ...)
	 * 		SELECT values[0], values[1], ... FROM dual
	 * 		WHERE NOT EXISTS 
	 * 			(SELECT '1' FROM table 
	 * 			WHERE fields[0] = values[0] 
	 * 			AND fields[1] = values[1]
	 * 			AND ...);
	 * </pre>
	 * @param	table	the table into which to insert
	 * @param	fields	the table's fields
	 * @param	insertValues	the values to be inserted, as strings with
	 * string values already enquoted
	 * @param	numConditionals	the number of inserted values that need to
	 * be checked for already being in the table
	 * @return	the SQL
	 */
	protected static StringBuilder insertWhereNotAlready(String table,
			String[] fields, String[] insertValues, int numConditionals) {
		final StringBuilder existsBld = 
				getBuilder(SELECT, quotes(1), FROM, table);
		for (int fieldNum = 0; fieldNum < numConditionals; fieldNum++) {
 			appendTo(existsBld, fieldNum == 0 ? WHERE : AND, 
					fields[fieldNum], EQUALS, insertValues[fieldNum]);
		} // for each conditional
		return getBuilder(
				INSERT_INTO, table, parens(fields), 
				SELECT, join(insertValues), 
				FROM + DUMMY_TABLE 
				+ WHERE + NOT_EXISTS, parens(existsBld));
	} // insertWhereNotAlready(String, String[], String[], int)

/* *************** Miscellaneous methods ******************/

	/** Converts an array of ints to a list of comma-separated 
	 * numbers containing fewer than 1000 items each.  
	 * @param	items	the array to break up
	 * @return	a list of Strings of comma-separated numbers 
	 */
	protected static List<int[]> getIntGroups(int[] items) {
		return getIntGroupsPriv(Utils.intArrayToList(items));
	} // getIntGroups(int[])

	/** Converts a list of ints to a list of comma-separated 
	 * numbers containing fewer than 1000 items each.  
	 * @param	items	the list to break up
	 * @return	a list of Strings of comma-separated numbers 
	 */
	protected static List<int[]> getIntGroups(List<Integer> items) {
		return getIntGroupsPriv(new ArrayList<Integer>(items));
	} // getIntGroups(List<Integer>)

	/** Converts a list of ints to a list of arrays of ints, 
	 * each array containing fewer than 1000 items each.  Modifies the 
	 * original list!
	 * @param	items	the list to break up
	 * @return	a list of int arrays
	 */
	private static List<int[]> getIntGroupsPriv(List<Integer> items) {
		final List<int[]> bunchStrs = new ArrayList<int[]>();
		final List<Integer> bunch = new ArrayList<Integer>();
		while (!items.isEmpty()) {
			bunch.add(items.remove(0));
			if (bunch.size() >= MAX_ORACLE_IN || items.isEmpty()) { 
				bunchStrs.add(Utils.listToIntArray(bunch));
				bunch.clear();
			} // if there are no more items or bunch limit has been reached
		} // while there are more items 
		return bunchStrs;
	} // getIntGroupsPriv(List<Integer>)

	/** Converts an array of items to a list of arrays of enquoted 
	 * Strings containing fewer than 1000 items each.  
	 * @param	items	the array to break up
	 * @return	a list of comma-separated, enquoted Strings 
	 */
	protected static List<String[]> getStrGroups(String[] items) {
		return getStrGroups(items, !ENQUOTE);
	} // getStrGroups(String[])

	/** Converts an array of items to a list of arrays of perhaps 
	 * enquoted Strings containing fewer than 1000 items each.  
	 * @param	items	the array to break up
	 * @param	enquote	whether to add quotes around each string
	 * @return	a list of comma-separated, perhaps enquoted Strings 
	 */
	protected static List<String[]> getStrGroups(String[] items, 
			boolean enquote) {
		return getStrGroupsPriv(new ArrayList<String>(Arrays.asList(items)),
				enquote);
	} // getStrGroups(String[], boolean)

	/** Converts a list of items to a list of arrays of enquoted 
	 * Strings containing fewer than 1000 items each.  
	 * @param	items	the list to break up
	 * @return	a list of comma-separated, enquoted Strings 
	 */
	protected static List<String[]> getStrGroups(List<String> items) {
		return getStrGroups(items, !ENQUOTE);
	} // getStrGroups(List<String>)

	/** Converts a list of items to a list of arrays of perhaps 
	 * enquoted Strings containing fewer than 1000 items each.  
	 * @param	items	the list to break up
	 * @param	enquote	whether to add quotes around each string
	 * @return	a list of comma-separated, perhaps enquoted Strings 
	 */
	protected static List<String[]> getStrGroups(List<String> items,
			boolean enquote) {
		return getStrGroupsPriv(new ArrayList<String>(items), enquote);
	} // getStrGroups(List<String>, boolean)

	/** Converts a list of items to a list of arrays of Strings, perhaps 
	 * enquoted, each array containing fewer than 1000 items each.  Modifies the 
	 * original list!
	 * @param	items	the list to break up
	 * @param	enquote	whether to add quotes around each string
	 * @return	a list of arrays of perhaps enquoted Strings 
	 */
	private static List<String[]> getStrGroupsPriv(List<String> items,
			boolean enquote) {
		final List<String[]> bunches = new ArrayList<String[]>();
		final List<String> bunch = new ArrayList<String>();
		while (!items.isEmpty()) {
			String item = items.remove(0);
			if (enquote) item = quotes(item);
			bunch.add(item);
			if (bunch.size() >= MAX_ORACLE_IN || items.isEmpty()) { 
				bunches.add(bunch.toArray(new String[bunch.size()]));
				bunch.clear();
			} // if there are items in the bunch
		} // while there are more items 
		return bunches;
	} // getStrGroupsPriv(List<String>, boolean)

	/** Decompresses a newly acquired molecule string if it is in LewisSketch
	 * format.
	 * @param	molStr	a string representing a molecule
	 * @return	the decompressed string if it's a Lewis structure, the original 
	 * string otherwise
	 */
	protected static String decompressIfLewis(String molStr) {
		String decomp = molStr;
		if (molStr != null 
				&& molStr.trim().startsWith("Lewis")
				&& molStr.trim().endsWith("M  END")) try {
			decomp = MdlCompressor.convert(molStr, MdlCompressor.DECOMPRESS);
		} catch (IOException e) {
			debugPrint("IOException");
		}
		return decomp;
	} // decompressIfLewis(String)

	/** Converts a date to a string in format YYYY/MM/DD HH:MM:SS.
	 * @param	date	the date
	 * @return	the date as a string
	 */
	protected static String dateToString(Date date) { 
		return (date == null ? "" : DateUtils.getDbTime(date)); 
	} // dateToString(Date)

	/** Converts a date to a string in format YYYY/MM/DD HH:MM:SS ZZZ.
	 * @param	date	the date
	 * @param	tz	the time zone
	 * @return	the date as a string
	 */
	protected static String dateToString(Date date, TimeZone tz) { 
		return (date == null ? "" : DateUtils.getDbTime(date, tz)); 
	} // dateToString(Date, TimeZone)

	/** Converts a string in format YYYY/MM/DD HH:MM:SS to a date.
	 * @param	dateStr	the date as a string
	 * @return	the date
	 */
	protected static Date toDate(String dateStr) { 
		return (Utils.isEmpty(dateStr) ? null : DateUtils.parseDbDate(dateStr));
	} // toDate(String)

	/** Converts a StringBuilder in format YYYY/MM/DD HH:MM:SS to a date.
	 * @param	dateStr	the date as a StringBuilder 
	 * @return	the date
	 */
	protected static Date toDate(StringBuilder dateStr) { 
		return (dateStr == null ? null 
				: DateUtils.parseDbDate(dateStr.toString()));
	} // toDate(StringBuilder)

/* *************** Straight calls to Utils *****************/

	/** Combines all the objects in a StringBuilder and converts them to a
	 * string.
	 * @param	items	the items to be combined into a string
	 * @return	the combined string
	 */
	protected static String toString(Object... items) {
		return Utils.toString(items);
	} // toString(Object...)

	/** Combines all the objects into a new StringBuilder.
	 * @param	items	the items to be combined into a string
	 * @return	the combined StringBuilder
	 */
	protected static StringBuilder getBuilder(Object... items) {
		return Utils.getBuilder(items);
	} // getBuilder(Object...)

	/** Combines all the objects into the StringBuilder.
	 * @param	bld	the StringBuilder
	 * @param	items	the items to be combined into a string
	 */
	protected static void appendTo(StringBuilder bld, Object... items) {
		Utils.appendTo(bld, items);
	} // appendTo(StringBuilder, Object...)

	/** Determines whether a string is null or "". 
	 * @param	str	a string
	 * @return	true if the string is null or ""
	 */
	protected static boolean isEmpty(String str) {
		return Utils.isEmpty(str);
	} // isEmpty(String)

	/** Determines whether the array is null or has no members.
	 * @param	arr	an array
	 * @return	true if array is null or has no members.
	 */
	protected static boolean isEmpty(Object[] arr) {
		return Utils.isEmpty(arr);
	} // isEmpty(Object[])

	/** Determines whether the array is null or has no members.
	 * @param	arr	an array
	 * @return	true if array is null or has no members.
	 */
	protected static boolean isEmpty(int[] arr) {
		return Utils.isEmpty(arr);
	} // isEmpty(int[])

	/** Determines whether the array is null or has no members.
	 * @param	arr	an array
	 * @return	true if array is null or has no members.
	 */
	protected static boolean isEmpty(byte[] arr) {
		return Utils.isEmpty(arr);
	} // isEmpty(byte[])

	/** Trims a string, returning empty if null.
	 * @param	str	a string
	 * @return	the trimmed string, or "" if null
	 */
	protected static String trimNullToEmpty(String str) {
		return Utils.trimNullToEmpty(str);
	} // trimNullToEmpty(String)

	/** Converts Unicode to character entity references for storing in VARCHAR
	 * fields.
	 * @param	s	string to modify
	 * @return	string with Unicode converted to CERs
	 */
	protected static String unicodeToCERs(String s) {
		return Utils.unicodeToCERs(s);
	} // unicodeToCERs(String)

	/** Capitalizes the first letter of a string, even if accented.
	 * @param	str	a string
	 * @return	string with first letter capitalized
	 */
	public static String capitalize(String str) {
		return Utils.capitalize(str);
	} // capitalize(String)

	/** Prints a series of Strings, Integers, Molecules, etc. to the log.
	 * @param	msg	the series of Strings and other Objects to print
	 */
	protected static void alwaysPrint(Object... msg) {
		Utils.alwaysPrint(msg);
	} // alwaysPrint(Object...)

	/** Prints a series of Strings, Integers, Molecules, etc. to the log;
	 * molecules will appear in MRV format.
	 * @param	msg	the series of Strings and other Objects to print
	 */
	protected static void alwaysPrintMRV(Object... msg) {
		Utils.alwaysPrintMRV(msg);
	} // alwaysPrintMRV(Object...)

	/** So DBCommon has no constructor. */
	public DBCommon() { 
		// intentionally empty
	} 

} // DBCommon
