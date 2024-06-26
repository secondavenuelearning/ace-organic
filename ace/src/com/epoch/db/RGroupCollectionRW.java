package com.epoch.db;

import com.epoch.exceptions.DBException;
import com.epoch.substns.RGroupCollection;
import com.epoch.utils.Utils; 

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Contains all database read and write operations pertaining to R-group
 * collections.   All methods are static.  */
public final class RGroupCollectionRW extends DBCommon {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

	/** Table for classes of R groups.  */
	private static final String R_GROUP_CLASS = "r_group_class_v3";
		/** Field in R_GROUP_CLASS.  Unique ID.  */
		private static final String RGRP_ID = "id";
		/** Field in R_GROUP_CLASS. */
		private static final String RGRP_NAME = "name";
		/** Field in R_GROUP_CLASS. */
		private static final String RGRP_NUM = "member_num";
		/** Field in R_GROUP_CLASS. */
		private static final String RGRP_MEMBER = "member";
	/** Sequencer for classes of R groups.  */
	private static final String R_GROUP_CLASS_SEQ = "r_group_class_seq";

	/** Gets the R groups of one or more collections from the database.
	 * @param	rGroupCollIds	ID numbers of the collections whose R groups 
	 * to get
	 * @return	the requested R groups with no duplicates
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static String[] getRGroups(String[] rGroupCollIds) 
			throws DBException {
		final String SELF = "RGroupCollectionRW.getRGroups: ";
		final List<String> rGrpsList = new ArrayList<String>();
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		if (!Utils.isEmpty(rGroupCollIds)) try { 
			final String qry = toString(
					SELECT_UNIQUE + RGRP_MEMBER
						+ FROM + R_GROUP_CLASS 
						+ WHERE + RGRP_ID + IN, parensQMarks(rGroupCollIds));
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry);
			sql_vals.setValuesArray(rGroupCollIds);
			debugPrint(SELF, sql_vals);
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) rGrpsList.add(rs.getString(RGRP_MEMBER).trim());
		} catch (SQLException e) {
			System.out.println(SELF + "caught SQLException");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		} // try
		return rGrpsList.toArray(new String[rGrpsList.size()]);
	} // getRGroups(String[])

	/** Gets an R-group collection from the database.
	 * @param	rGroupColId	ID number of the R-group collection
	 * @return	the requested R-group collection
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static RGroupCollection getRGroupCollection(int rGroupColId) 
			throws DBException {
		final String SELF = "RGroupCollectionRW.getRGroupCollection: ";
		final String qry = toString(
				SELECT, joinAll( 
					RGRP_NAME, 
					RGRP_MEMBER),
				FROM + R_GROUP_CLASS 
				+ WHERE + RGRP_ID + EQUALS + QMARK
				+ ORDER_BY + RGRP_NUM);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				rGroupColId);
		RGroupCollection rGroupCol = null;
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try { 
			con = getPoolConnection();
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (!rs.next()) {
				throw new DBException("no results from query " + qry);
			}
			final String name = rs.getString(RGRP_NAME);
			final List<String> rGrpsList = new ArrayList<String>();
			while (!rs.isAfterLast()) {
				rGrpsList.add(rs.getString(RGRP_MEMBER).trim());
				rs.next();
			} // while there are more members
			debugPrint(SELF + "name = ", name, ", R groups = ", rGrpsList);
			rGroupCol = new RGroupCollection(rGroupColId, name, 
					rGrpsList.toArray(new String[rGrpsList.size()]));
		} catch (SQLException e) {
			System.out.println(SELF +
					"caught SQLException, probably invalid rGroupColId.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		} // try
		return rGroupCol;
	} // getRGroupCollection(int)

	/** Gets names of all R-group collections from the database in order of ID
	 * numbers.  
	 * @return	array of names of all R-group collections
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static String[] getAllRGroupCollectionNames() throws DBException {
		final Map<Integer, String> namesByIds = 
				getAllRGroupCollectionNamesKeyedById(); 
		final ArrayList<String> names = 
				new ArrayList<String>(namesByIds.values());
		debugPrint("RGroupCollectionRW.getAllRGroupCollectionNames: map = ",
				namesByIds, ", names = ", names);
		return names.toArray(new String[names.size()]);
	} // getAllRGroupCollectionNames()

	/** Gets all R-group collection ID numbers in array parallel to names.
	 * @return	array of IDs of all R-group collections
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static int[] getAllRGroupCollectionIds() throws DBException {
		final Map<Integer, String> namesByIds = 
				getAllRGroupCollectionNamesKeyedById(); 
		final ArrayList<Integer> ids = 
				new ArrayList<Integer>(namesByIds.keySet());
		debugPrint("RGroupCollectionRW.getAllRGroupCollectionIds: map = ",
				namesByIds, ", ids = ", ids);
		return Utils.listToIntArray(ids);
	} // getAllRGroupCollectionIds()

	/** Gets names of all R-group collections keyed by their IDs.  
	 * @return	hashtable of names of all R-group collections keyed by their IDs
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<Integer, String> 
			getAllRGroupCollectionNamesKeyedById() throws DBException {
		final String SELF = "RGroupCollectionRW."
				+ "getAllRGroupCollectionNamesKeyedById: ";
		final String qry = toString(
				SELECT_UNIQUE, joinAll( 
					RGRP_ID,
					RGRP_NAME), 
				FROM + R_GROUP_CLASS 
				+ ORDER_BY + RGRP_ID);
		debugPrint(SELF + qry);
		final Map<Integer, String> rGroupColData = 
				new LinkedHashMap<Integer, String>();
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			while (rs != null && rs.next()) {
				final int id = rs.getInt(RGRP_ID);
				final String datum = rs.getString(RGRP_NAME);
				if (datum != null) {
					rGroupColData.put(Integer.valueOf(id), datum);
				} // if there is such a collection
			} // while
		} catch (SQLException e) {
			System.out.println(SELF
					+ "SQL exception while getting R-Group names");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		} // try
		return rGroupColData;
	} // getAllRGroupCollectionNamesKeyedById()

	/** Gets definitions of all R-group collections keyed by their IDs.  
	 * @return	hashtable of definitions of all R-group collections keyed 
	 * by their IDs
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<Integer, String[]> 
			getAllRGroupCollectionDefsKeyedById() throws DBException {
		final String SELF = "RGroupCollectionRW."
				+ "getAllRGroupCollectionDefsKeyedById: ";
		final String qry = toString(
				SELECT, joinAll( 
					RGRP_ID,
					RGRP_MEMBER), 
				FROM + R_GROUP_CLASS 
				+ ORDER_BY, joinAll(
					RGRP_ID,
					RGRP_NUM));
		debugPrint(SELF + qry);
		final Map<Integer, String[]> rGroupColData = 
				new HashMap<Integer, String[]>();
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try { 
			con = getPoolConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			if (rs.next()) while (!rs.isAfterLast()) {
				final int id = rs.getInt(RGRP_ID);
				final List<String> members = new ArrayList<String>();
				while (!rs.isAfterLast()
						&& id == rs.getInt(RGRP_ID)) {
					members.add(rs.getString(RGRP_MEMBER).trim());
					rs.next();
				} // while more members in this collection
				rGroupColData.put(Integer.valueOf(id), 
						members.toArray(new String[members.size()]));
			} // while there are more collections
		} catch (SQLException e) {
			System.out.println(SELF
					+ "SQL exception while getting R-Group collections");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		} // try
		return rGroupColData;
	} // getAllRGroupCollectionDefsKeyedById()

	/** Saves an R-group collection to the database.
	 * @param	rGroupCol	the R-group collection to save
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setRGroupCollection(RGroupCollection rGroupCol) 
			throws DBException {
		final String SELF = "RGroupCollectionRW.setRGroupCollection: ";
		Connection con = null;
		PreparedStatement stmt = null;
		try { 
			con = getPoolConnection();
			con.setAutoCommit(false);
			//	if id negative, change record
			//	if id 0, add record
			int rGroupColId = rGroupCol.id;
			if (rGroupColId == 0) { // new recod
				rGroupColId = nextSequence(con, R_GROUP_CLASS_SEQ); 
			} else { // existing record
				// Delete the old rec	
				final String qry = 
						DELETE_FROM + R_GROUP_CLASS 
						+ WHERE + RGRP_ID + EQUALS + QMARK;
				final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
						rGroupColId);
				debugPrint(SELF, sql_vals);
				tryUpdate(con, sql_vals);
			} // if rGroupColId
			final String[] fields = new String[] {
					RGRP_ID,
					RGRP_NAME,
					RGRP_NUM,
					RGRP_MEMBER};
			final String qry = 
					getInsertIntoValuesQMarksSQL(R_GROUP_CLASS, fields);
			debugPrint(SELF, qry); 
			stmt = con.prepareStatement(qry); 
			int grpNum = 0;
			for (final String rGroup : rGroupCol.rGroups) {
				if (!Utils.isEmptyOrWhitespace(rGroup)) {
					++grpNum;
					final String rGrp = rGroup.trim();
					final StringBuilder joinedValues = setValues(stmt, 
							rGroupColId, 
							rGroupCol.name, 
							grpNum,
							rGrp);
					debugPrint(SELF, "batch ", grpNum, ": ", joinedValues); 
					stmt.addBatch();
				} // if there's an R group
			} // for each R group
			stmt.executeBatch();
			con.commit();
		} catch (SQLException e) {
			System.out.println(SELF + "SQL exception while "
					+ "saving RGroupCollection");
			rollbackConnection(con);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, null);
		}
	} // setRGroupCollection(RGroupCollection)

	/** Constructor to disable external instantiation. */
	private RGroupCollectionRW() { }

} // RGroupCollectionRW
