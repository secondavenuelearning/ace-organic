package com.epoch.db;

import static com.epoch.db.dbConstants.SynthDataConstants.*;
import com.epoch.exceptions.DBException;
import com.epoch.synthesis.RxnCondition;
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

/** Contains all database read and write operations pertaining to
 * definitions of reaction conditions, bad starting materials, 
 * and menu-only reagents. */
public final class SynthDataRW extends DBCommon {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

	/** Saves a reaction condition to the database.
	 * @param	rxnCondn	the reaction condition to be saved
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setRxnCondition(RxnCondition rxnCondn) 
			throws DBException {
		Connection con = null;
		try { 
			con = getPoolConnection();
			setRxnCondition(con, rxnCondn);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // setRxnCondition(RxnCondition)

	/** Saves a reaction condition to the database.
	 * @param	con	database connection
	 * @param	rxnCondn	the reaction condition to be saved
	 * @throws	DBException	if there's a problem writing to the database
	 */
	private static void setRxnCondition(Connection con,
			RxnCondition rxnCondn) throws DBException {
		final String SELF = "SynthDataRW.setRxnCondition: ";
		try { 
			final boolean newRecord = rxnCondn.rxnId == 0;
			debugPrint(SELF + "rxnId = ", rxnCondn.rxnId);
			con.setAutoCommit(false);
			if (newRecord) {
				rxnCondn.rxnId = nextSequence(con, RXN_CONDNS_SEQ); 
			} else try {
				// first, delete products calculated from SMs under the old definition
				ReactorResultsRW.deleteCalcdProducts(con, rxnCondn.rxnId);
			} catch (DBException e) {
				Utils.alwaysPrint(SELF + "caught DBException while trying "
						+ "to delete calculated products from database; "
						+ "continuing to write new definition.");
				e.printStackTrace();
			} catch (Exception e) {
				Utils.alwaysPrint(SELF + "caught unknown exception while trying "
						+ "to delete calculated products from database; "
						+ "continuing to write new definition.");
				e.printStackTrace();
			} // if newRecord
			String qry = "";
			if (newRecord) {
				final String[] fields = new String[] {
						RXNCOND_NAME,
						RXNCOND_CLASS,
						RXNCOND_3COMP,
						RXNCOND_DEF, // CLOB field
						RXNCOND_ID};
				qry = getInsertIntoValuesQMarksSQL(RXN_CONDNS, fields);
			} else {
				qry = toString(
						UPDATE + RXN_CONDNS + SET, equalsJoinQMarks(
							RXNCOND_NAME,
							RXNCOND_CLASS,
							RXNCOND_3COMP, 
							RXNCOND_DEF), // CLOB field
						WHERE + RXNCOND_ID + EQUALS + QMARK); 
			} // if new record
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
						rxnCondn.name,
						rxnCondn.classifn,
						rxnCondn.threeComponent ? 'Y' :'N',
						rxnCondn.reactionDef,
						rxnCondn.rxnId);
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
			con.commit();
		} catch (SQLException e) {
			Utils.alwaysPrint(SELF + "caught SQLException while trying to "
					+ "write reaction definition; rolling back.");
			e.printStackTrace();
			rollbackConnection(con);
			throw new DBException(e.getMessage());
		} 
	} // setRxnCondition(Connection, RxnCondition)

	/** Gets a reaction condition from the database.
	 * @param	rxnId	ID number of the reaction condition to acquire
	 * @return	the requested reaction condition
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static RxnCondition getRxnCondition(int rxnId) 
			throws DBException {
		Connection con = null;
		try { 
			con = getPoolConnection();
			return getRxnCondition(con, rxnId);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // getRxnCondition(int)

	/** Gets a reaction condition from the database.
	 * @param	con	database connection
	 * @param	rxnId	ID number of the reaction condition to acquire
	 * @return	the requested reaction condition
	 * @throws	SQLException	if there's a problem reading the database
	 */
	private static RxnCondition getRxnCondition(Connection con, 
			int rxnId) throws SQLException {
		final String SELF = "SynthDataRW.getRxnCondition: ";
		RxnCondition rxnCondn = null;
		final String qry = toString(
				SELECT, joinAll( 
					RXNCOND_NAME, 
					RXNCOND_DEF, 
					RXNCOND_CLASS,
					RXNCOND_3COMP),
				FROM + RXN_CONDNS 
				+ WHERE + RXNCOND_ID + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				rxnId);
		debugPrint(SELF, sql_vals);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (!rs.next()) {
				throw new SQLException("no results from query " + qry);
			}
			final String name = rs.getString(RXNCOND_NAME);
			final String definition = rs.getString(RXNCOND_DEF);
			final String classifn = rs.getString(RXNCOND_CLASS);
			final boolean threeComp = "Y".equals(rs.getString(RXNCOND_3COMP));
			if (name != null && definition != null && classifn != null)
				rxnCondn = new RxnCondition(rxnId, name, definition, 
						classifn, threeComp);
			debugPrint(SELF, "name = ", name, ", classifn = ", 
					classifn, ", definition = \n", 
					Utils.chopString(definition, 500));
		} catch (SQLException e) {
			System.out.println(SELF +
					"caught SQLException, probably invalid rxnId.");
		} finally {
			closeStmtAndRs(stmt, rs);
		} // try
		return rxnCondn;
	} // getRxnCondition(Connection, int)

	/** Gets the name of a reaction condition from the database.
	 * @param	con	database connection
	 * @param	rxnId	ID number of the reaction condition to acquire
	 * @return	the name of the requested reaction condition
	 */
	static String getRxnConditionName(Connection con, int rxnId) {
		String name = "unknown";
		try { 
			name = getRxnCondition(con, rxnId).name;
		} catch (SQLException e) {
			alwaysPrint("SynthDataRW.getRxnConditionName: couldn't get name");
			e.printStackTrace();
		}
		return name;
	} // getRxnConditionName(int)

	/** Gets names or classifications of all reaction conditions from the database.  
	 * @param	type	whether to get names, classifications in an array
	 * parallel to names, or unique classifications only
	 * @return	array of names or classifications of all reaction conditions
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static String[] getAllReactionsData(int type) throws DBException {
		Connection con = null;
		try { 
			con = getPoolConnection();
			return getAllReactionsData(con, type);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // getAllReactionsData(int)

	/** Gets names or classifications of all reaction conditions from the database.  
	 * @param	con	database connection
	 * @param	type	whether to get names, classifications in an array
	 * parallel to names, or unique classifications only
	 * @return	array of names or classifications of all reaction conditions
	 * @throws	SQLException	if there's a problem reading the database
	 */
	private static String[] getAllReactionsData(Connection con, int type) 
			throws SQLException {
		final String SELF = "SynthDataRW.getAllReactionData: ";
		String[] rxnData = null;
		String qry = null;
		switch (type) {
			case RxnCondition.NAMES: 
				qry = toString(
						SELECT, joinAll(
							RXNCOND_NAME + AS + SRCH_RESULT, 
							RXNCOND_ID),
						FROM + RXN_CONDNS 
						+ ORDER_BY + RXNCOND_ID);
				break;
			case RxnCondition.CLASSIFNS:
				qry = toString(
						SELECT, joinAll(
							RXNCOND_CLASS + AS + SRCH_RESULT, 
							RXNCOND_ID),
						FROM + RXN_CONDNS 
						+ ORDER_BY + RXNCOND_ID);
				break;
			case RxnCondition.CLASSIFNS_UNIQUE:
				qry = SELECT_UNIQUE + RXNCOND_CLASS 
						+ AS + SRCH_RESULT
						+ FROM + RXN_CONDNS;
				break;
			default:
				alwaysPrint(SELF + "bad type");
				break;
		} // switch (type)
		debugPrint(SELF, qry);
		Statement stmt = null;
		ResultSet rs = null;
		final ArrayList<String> data = new ArrayList<String>();
		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			while (rs != null && rs.next()) {
				final String datum = rs.getString(SRCH_RESULT);
				if (datum != null) data.add(datum);
			}
			rxnData = data.toArray(new String[data.size()]);
		} finally {
			closeStmtAndRs(stmt, rs);
		} // try
		return rxnData;
	} // getAllReactionsData(Connection, int)

	/** Gets IDs of all reaction conditions from the database in the same order
	 * as the names.  
	 * @return	array of IDs of all reaction conditions
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static int[] getAllReactionIds() throws DBException {
		Connection con = null;
		try { 
			con = getPoolConnection();
			return getAllReactionIds(con);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // getAllReactionIds()

	/** Gets IDs of all reaction conditions from the database in the same order
	 * as the names.  
	 * @param	con	database connection
	 * @return	array of IDs of all reaction conditions
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static int[] getAllReactionIds(Connection con) throws DBException {
		int[] rxnIds = null;
		final String qry = SELECT + RXNCOND_ID 
				+ FROM + RXN_CONDNS 
				+ ORDER_BY + RXNCOND_ID;
		debugPrint("SynthDataRW.getAllReactionIds: ", qry);
		Statement stmt = null;
		ResultSet rs = null;
		try {
			final ArrayList<Integer> ids = new ArrayList<Integer>();
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			while (rs != null && rs.next()) {
				ids.add(Integer.valueOf(rs.getInt(RXNCOND_ID)));
			}
			rxnIds = Utils.listToIntArray(ids);
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeStmtAndRs(stmt, rs);
		}
		return rxnIds;
	} // getAllReactionsIds(Connection)

	/** Gets names of all reaction conditions keyed by their IDs.  
	 * @return	hashtable of names of all reaction conditions keyed by their IDs
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<Integer, String> getAllReactionNamesKeyedById() 
			throws DBException {
		Connection con = null;
		try { 
			con = getPoolConnection();
			return getAllReactionNamesKeyedById(con);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // getAllReactionNamesKeyedById()

	/** Gets names of all reaction conditions keyed by their IDs.  
	 * @param	con	database connection
	 * @return	hashtable of names of all reaction conditions keyed by their IDs
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static Map<Integer, String> getAllReactionNamesKeyedById(
			Connection con) throws DBException {
		final Map<Integer, String> rxnData = new HashMap<Integer, String>();
		final String qry = toString(
				SELECT, joinAll(
					RXNCOND_ID, 
					RXNCOND_NAME), 
				FROM + RXN_CONDNS 
				+ ORDER_BY + RXNCOND_ID);
		debugPrint("SynthDataRW.getAllReactionNamesKeyedById: ", qry);
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			while (rs != null && rs.next()) {
				final int id = rs.getInt(RXNCOND_ID);
				final String datum = rs.getString(RXNCOND_NAME);
				if (datum != null) rxnData.put(Integer.valueOf(id), datum);
			}
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeStmtAndRs(stmt, rs);
		}
		return rxnData;
	} // getAllReactionNamesKeyedById(Connection)

	/** Gets MRV definitions of all reaction conditions keyed by their IDs.  
	 * @return	hashtable of MRV definitions of all reaction conditions 
	 * keyed by their IDs
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<Integer, String> getAllReactionDefsKeyedById() 
			throws DBException {
		Connection con = null;
		try { 
			con = getPoolConnection();
			return getAllReactionDefsKeyedById(con);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // getAllReactionDefsKeyedById()

	/** Gets MRV definitions of all reaction conditions keyed by their IDs.  
	 * @param	con	database connection
	 * @return	hashtable of MRV definitions of all reaction conditions 
	 * keyed by their IDs
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static Map<Integer, String> getAllReactionDefsKeyedById(
			Connection con) throws DBException {
		final Map<Integer, String> rxnData = new HashMap<Integer, String>();
		final String qry = toString(
				SELECT, joinAll(
					RXNCOND_ID,
					RXNCOND_DEF), 
				FROM + RXN_CONDNS 
				+ ORDER_BY + RXNCOND_ID);
		debugPrint("SynthDataRW.getAllReactionDefsKeyedById: ", qry);
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			while (rs != null && rs.next()) {
				final int id = rs.getInt(RXNCOND_ID);
				final String defn = rs.getString(RXNCOND_DEF);
				if (defn != null) rxnData.put(Integer.valueOf(id), defn);
			}
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeStmtAndRs(stmt, rs);
		}
		return rxnData;
	} // getAllReactionDefsKeyedById(Connection)

/* ****************** Methods for impermissible starting materials ***********/

	/** Saves a bad starting material to the database.
	 * @param	badSM	the bad starting material to be saved
	 * @param	oldName	previous name of bad starting material, or null
	 * if new entry
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void saveBadSM(String[] badSM, String oldName) 
			throws DBException {
		final String SELF = "SynthDataRW.saveBadSM: ";
		final String sortName = Utils.makeSortName(badSM[SM_NAME]);
		final SQLWithQMarks sql_vals = new SQLWithQMarks("",
				badSM[SM_NAME],
				badSM[SM_DEF],
				sortName);
		final String[] fields = new String[] {
				BADSM_NAME,
				BADSM_DEF, // CLOB
				BADSM_SORT};
		if (oldName == NEW_ENTRY) {
			sql_vals.setSql(
					getInsertIntoValuesQMarksSQL(BAD_SYNTH_SM, fields));
		} else {
			sql_vals.setSql(
					UPDATE + BAD_SYNTH_SM 
					+ SET, equalsJoinQMarksArr(fields),
					WHERE + BADSM_NAME + EQUALS + QMARK);
			sql_vals.addValue(oldName);
		} // if a new entry
		debugPrint(SELF, sql_vals);
		Connection con = null;
		try { 
			con = getPoolConnection();
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // saveBadSM(String[], boolean)

	/** Gets all bad starting materials from the database, 
	 * sorted by name.  
	 * @return	array of all bad starting materials, sorted by name
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static String[][] getAllBadSMs() throws DBException {
		final String SELF = "SynthDataRW.getAllBadSMs: ";
		final String qry = toString(
				SELECT, joinAll(
					BADSM_NAME,
					BADSM_DEF),
				FROM + BAD_SYNTH_SM 
				+ ORDER_BY + BADSM_SORT);
		String[][] allBadSMs = null;
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			debugPrint(SELF, qry);
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			final List<String[]> badSMs = new ArrayList<String[]>();
			while (rs != null && rs.next()) {
				final String name = rs.getString(BADSM_NAME);
				final String definition = rs.getString(BADSM_DEF);
				final String[] badSM = new String[] {name, definition};
				badSMs.add(badSM);
			}
			allBadSMs = badSMs.toArray(new String[badSMs.size()][2]);
		} catch (SQLException e) {
			System.out.println(SELF + "SQLException while getting allBadSMs");
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		} // try
		return allBadSMs;
	} // getAllBadSMs()

/* ****************** Methods for menu-only reagents ***********/

	/** Saves a menu-only reagent to the database.
	 * @param	menuOnlyRgt	the bad starting material to be saved
	 * @param	oldName	previous name of bad starting material, or null
	 * if new entry
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void saveMenuOnlyRgt(String[] menuOnlyRgt, String oldName) 
			throws DBException {
		final String SELF = "SynthDataRW.saveMenuOnlyRgt: ";
		final SQLWithQMarks sql_vals = new SQLWithQMarks("",
				menuOnlyRgt[SM_NAME],
				menuOnlyRgt[SM_DEF]);
		final String[] fields = new String[] {
				MENUONLY_NAME,
				MENUONLY_DEF};
		if (oldName == NEW_ENTRY) {
			sql_vals.setSql(
					getInsertIntoValuesQMarksSQL(MENU_ONLY_RGTS, fields));
		} else {
			sql_vals.setSql(
					UPDATE + MENU_ONLY_RGTS 
					+ SET, equalsJoinQMarksArr(fields),
					WHERE + MENUONLY_NAME + EQUALS + QMARK);
			sql_vals.addValue(oldName);
		} // if new entry
		debugPrint(SELF, sql_vals);
		Connection con = null;
		try { 
			con = getPoolConnection();
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // saveMenuOnlyRgt(String[], boolean)

	/** Gets all menu-only reagents from the database, sorted by name.  
	 * @return	array of all menu-only reagents, sorted by name
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static String[][] getAllMenuOnlyRgts() throws DBException {
		final String SELF = "SynthDataRW.getAllMenuOnlyRgts: ";
		final String qry = toString(
				SELECT, joinAll(
					MENUONLY_NAME,
					MENUONLY_DEF),
				FROM + MENU_ONLY_RGTS 
				+ ORDER_BY + MENUONLY_NAME);
		debugPrint(SELF, qry);
		final List<String[]> menuOnlyRgts = new ArrayList<String[]>();
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			while (rs != null && rs.next()) {
				final String name = rs.getString(MENUONLY_NAME);
				final String definition = rs.getString(MENUONLY_DEF);
				final String[] menuOnlyRgt = new String[] {name, definition};
				menuOnlyRgts.add(menuOnlyRgt);
			}
		} catch (SQLException e) {
			System.out.println(SELF + "SQLException while getting allMenuOnlyRgts");
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		} // try
		return menuOnlyRgts.toArray(new String[menuOnlyRgts.size()][2]);
	} // getAllMenuOnlyRgts()

	/** Constructor to disable external instantiation. */
	private SynthDataRW() { }

} // SynthDataRW
