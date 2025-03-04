package com.epoch.db;

import com.epoch.chem.FnalGroupDef;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.ParameterException;
import com.epoch.utils.Utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Contains all database read and write operations pertaining to
 * functional group definitions.  All methods are static.  */
public final class FnalGroupDefRW extends DBCommon {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

	/** Table for functional group definitions.  */
	private static final String FNAL_GRPS = "functional_groups_v2";
		/** Field in FNAL_GRPS.  Unique ID.  */
		private static final String FNAL_ID = "group_id";
		/** Field in FNAL_GRPS. */
		private static final String FNAL_NAME = "name";
		/** Field in FNAL_GRPS. */
		private static final String FNAL_DEF = "definition"; // CLOB 
		/** Field in FNAL_GRPS. */
		private static final String FNAL_CATEG = "category";
		/** Field in FNAL_GRPS. */
		private static final String FNAL_SORT = "sortkey";
	/** Sequencer for functional group definitions.  */
	private static final String FNAL_GRPS_SEQ = "functional_groups_seq";
	
	/** Gets a functional group from the database by ID number.
	 * @param	groupId	ID number of the functional group to acquire
	 * @return	the requested functional group
	 * @throws	ParameterException	if there is no such group
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static FnalGroupDef getFnalGroupDef(int groupId)
			throws DBException, ParameterException {
		final String where = WHERE + FNAL_ID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(where, groupId);
		return getFnalGroupFromDb(sql_vals);
	} // getFnalGroupDef(int)

	/** Gets a functional group from the database by name.
	 * @param	name	name of the functional group to acquire
	 * @return	the requested functional group
	 * @throws	DBException	if there's a problem reading the database
	 * @throws	ParameterException	if there is no such group
	 */
	public static FnalGroupDef getFnalGroupDef(String name)
			throws DBException, ParameterException {
		final String where = toString(
				WHERE, toUpper(FNAL_NAME), EQUALS + QMARK);
		final SQLWithQMarks sql_vals = 
				new SQLWithQMarks(where, name.toUpperCase(Locale.US));
		return getFnalGroupFromDb(sql_vals);
	} // getFnalGroupDef(String)

	/** Gets a functional group from the database by name or ID number.
	 * @param	whereSql_vals	the WHERE clause of the SELECT statement that 
	 * will be used to select the group
	 * @return	the requested functional group
	 * @throws	DBException	if there's a problem reading the database
	 * @throws	ParameterException	if there is no such group
	 */
	private static FnalGroupDef getFnalGroupFromDb(SQLWithQMarks whereSql_vals)
			throws DBException, ParameterException {
		final String SELF = "FnalGroupDefRW.getFnalGroupFromDb: ";
		final String qry = toString(
				SELECT, joinAll(
					FNAL_ID,
					FNAL_NAME,
					FNAL_DEF,
					FNAL_CATEG), 
				FROM + FNAL_GRPS, 
				whereSql_vals.getSql());
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, whereSql_vals);
		FnalGroupDef fnalGroup = null;
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (!rs.next()) {
				throw new ParameterException(SELF + "No functional group found"
						+ sql_vals.getSql());
			}
			fnalGroup = makeGroup(rs);
		} catch (SQLException e) {
			System.out.println(SELF 
					+ "caught SQLException, probably invalid group name.");
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return fnalGroup;
	} // getFnalGroupFromDb(SQLWithQMarks)

	/** Makes a functional group definition from the result of a search.
	 * @param	rs	result of search
	 * @return	a functional group definition
	 * @throws	SQLException	if there's a problem reading the results
	 */
	private static FnalGroupDef makeGroup(ResultSet rs) throws SQLException {
		final int groupId = rs.getInt(FNAL_ID);
		final String name = rs.getString(FNAL_NAME);
		final String definition = rs.getString(FNAL_DEF);
		final String category = rs.getString(FNAL_CATEG);
		debugPrint("FnalGroupDefRW.makeGroup: name = ", name,
				", category = ", category, ", definition = ", definition);
		return new FnalGroupDef(groupId, name, definition, category);
	} // makeGroup(ResultSet)

	/** Saves a functional group to the database.
	 * @param	fnalGrpDef	the functional group to be saved
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void saveFnalGroupDef(FnalGroupDef fnalGrpDef)
			throws DBException {
		final String SELF = "FnalGroupDefRW.getAllGroups: ";
		Connection con = null;
		try {
			con = getPoolConnection();
			String qry = "";
			if (fnalGrpDef.groupId == 0) {
				fnalGrpDef.groupId = nextSequence(con, FNAL_GRPS_SEQ);
				final String[] fields = new String[] {
						FNAL_NAME,
						FNAL_DEF, // CLOB
						FNAL_CATEG,
						FNAL_SORT,
						FNAL_ID};
				qry = getInsertIntoValuesQMarksSQL(
 						FNAL_GRPS, fields);
			} else {
				qry = toString(
						UPDATE + FNAL_GRPS + SET, equalsJoinQMarks(
							FNAL_NAME,
							FNAL_DEF, // CLOB
							FNAL_CATEG,
							FNAL_SORT),
						WHERE + FNAL_ID + EQUALS + QMARK);
			} // if new record
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
					fnalGrpDef.name,
					fnalGrpDef.definition,
					fnalGrpDef.category,
					fnalGrpDef.getSortKey(),
					fnalGrpDef.groupId);
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals); 
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // saveFnalGroupDef(FnalGroupDef)

	/** Gets all functional groups from the database, sorted by category and
	 * name.
	 * @return	array of all functional groups, sorted by category and name
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static FnalGroupDef[] getAllGroups() throws DBException {
		final String qry = toString(
				SELECT, joinAll(
					FNAL_ID,
					FNAL_NAME,
					FNAL_DEF,
					FNAL_CATEG),
				FROM + FNAL_GRPS,
				ORDER_BY, joinAll(
					FNAL_CATEG, 
					FNAL_SORT));
		debugPrint("FnalGroupDefRW.getAllGroups: ", qry);
		FnalGroupDef[] groups = null;
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			final List<FnalGroupDef> grps = new ArrayList<FnalGroupDef>();
			while (rs.next()) grps.add(makeGroup(rs));
			groups = grps.toArray(new FnalGroupDef[grps.size()]);
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return groups;
	} // getAllGroups()

	/** Gets names, lower-case names or categories of all functional groups
	 * from the database, in alphabetical order.
	 * @param	type	whether to get names, lower-case names,
	 * or unique categories
	 * @return	array of names or categories of all functional groups
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<String> getAllGroupsData(int type)
			throws DBException {
		final String SELF = "FnalGroupDefRW.getAllGroupsData: ";
		final String qry = (type == FnalGroupDef.CATEGORIES
				? SELECT_UNIQUE + FNAL_CATEG
					+ AS + SRCH_RESULT
					+ FROM + FNAL_GRPS
					+ ORDER_BY + FNAL_CATEG
				: SELECT + FNAL_NAME
					+ AS + SRCH_RESULT
					+ FROM + FNAL_GRPS
					+ ORDER_BY + FNAL_SORT);
		debugPrint(SELF, qry);
		final List<String> grpData = new ArrayList<String>();
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			while (rs.next()) {
				String datum = rs.getString(SRCH_RESULT);
				if (type == FnalGroupDef.LC_NAMES)
					datum = datum.toLowerCase(Locale.US);
				grpData.add(datum);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return grpData;
	} // getAllGroupsData(int)

	/** Constructor to disable external instantiation. */
	private FnalGroupDefRW() { }

} // FnalGroupDefRW
