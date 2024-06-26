package com.epoch.db;

import static com.epoch.db.dbConstants.UserRWConstants.*;
import com.epoch.courseware.Institution;
import com.epoch.exceptions.DBException;
import com.epoch.utils.Utils; 

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Contains all database read and write operations pertaining to 
 * institutions.   All methods are static.  */
public final class InstitutionRW extends DBCommon {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

	/** Parameter for getInstitutions().  */
	private static final boolean VERIFIED_ONLY = true;

	/** Get institution with the given name.
	 * @param	name	the given name (must match exactly!)
	 * @return	the institution, or null if there is no institution by that name
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Institution getInstitution(String name) throws DBException {
		final String SELF = "InstitutionRW.getInstitution: ";
		final String qry = toString(
				SELECT, joinAll(
					INSTN_ID,
					INSTN_LANG,
					INSTN_GRACE,
					INSTN_STUDENTNUMLABEL),
				FROM + INSTITUTIONS 
				+ WHERE + INSTN_NAME + LIKE + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				name);
		Institution institution = null;
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) {
				final int id = rs.getInt(INSTN_ID);
				final String instnLang = rs.getString(INSTN_LANG);
				final String instnStudentNumLabel = 
						rs.getString(INSTN_STUDENTNUMLABEL);
				final int gracePeriod = rs.getInt(INSTN_GRACE);
				debugPrint(SELF + "institution ", name, " has primary language ",
						instnLang);
				institution = new Institution(id, name, instnLang, 
						instnStudentNumLabel, gracePeriod);
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
		return institution;
	} // getInstitution(String)

	/** Get institution with the given ID.
	 * @param	id	the given id
	 * @return	the institution
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Institution getInstitution(int id) throws DBException {
		final String SELF = "InstitutionRW.getInstitution: ";
		final String qry = toString(
				SELECT, joinAll(
					INSTN_NAME,
					INSTN_LANG,
					INSTN_GRACE,
					INSTN_STUDENTNUMLABEL),
				FROM + INSTITUTIONS 
				+ WHERE + INSTN_ID + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				id);
		Institution institution = new Institution(id);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) {
				final String instnName = rs.getString(INSTN_NAME);
				final String instnLang = rs.getString(INSTN_LANG);
				final String instnStudentNumLabel = 
						rs.getString(INSTN_STUDENTNUMLABEL);
				final int gracePeriod = rs.getInt(INSTN_GRACE);
				debugPrint(SELF + "institution ", instnName, 
						" has primary language ", instnLang);
				institution = new Institution(id, instnName, 
						instnLang, instnStudentNumLabel, gracePeriod);
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
		return institution;
	} // getInstitution(int)

	/** Gets the number of days students at the institution can avoid paying 
	 * for ACE.
	 * @param	id	the given id
	 * @return	the number of days, or -1 if indefinite
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static int getGracePeriod(int id) throws DBException {
		final String SELF = "InstitutionRW.getGracePeriod: ";
		final String qry = 
				SELECT + INSTN_GRACE
				+ FROM + INSTITUTIONS 
				+ WHERE + INSTN_ID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				id);
		int gracePeriod = 0;
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) {
				gracePeriod = rs.getInt(INSTN_GRACE);
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
		return gracePeriod;
	} // getGracePeriod()

	/** Get all institutions of all instructors.
	 * @return	an array of institutions
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Institution[] getAllInstitutions() throws DBException {
		return getInstitutions(!VERIFIED_ONLY);
	} // getAllInstitutions()

	/** Get all institutions of verified instructors.
	 * @return	an array of institutions
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Institution[] getVerifiedInstitutions() throws DBException {
		return getInstitutions(VERIFIED_ONLY);
	} // getVerifiedInstitutions()

	/** Get institutions of instructors.
	 * @param	verifiedOnly	whether to get only institutions of verified
	 * instructores
	 * @return	an array of institutions
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static Institution[] getInstitutions(boolean verifiedOnly) 
			throws DBException {
		final String SELF = "InstitutionRW.getInstitutions: ";
		final StringBuilder qryBld = getBuilder(
				SELECT, joinAll(
					INSTN_ID,
					INSTN_NAME,
					INSTN_LANG,
					INSTN_GRACE,
					INSTN_STUDENTNUMLABEL),
				FROM + INSTITUTIONS);
		if (verifiedOnly) {
			qryBld.append(getBuilder(
					WHERE + INSTN_ID + IN, parens(getBuilder(
						SELECT + USER_SCHOOLID
						+ FROM + USERS
						+ WHERE + USER_ROLE + EQUALS, quotes(INSTRUCTOR),
						getBuilder(AND, bitand(USER_FLAGS, ENABLED), 
							IS_NOT_ZERO)))));
		} // if institutions of verified instructors only
		final String qry = toString(qryBld, 
				ORDER_BY, clobToString(INSTN_NAME));
		debugPrint(SELF, qry);
		final List<Institution> institutions = new ArrayList<Institution>();
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			while (rs.next()) {
				final int instnId = rs.getInt(INSTN_ID);
				final String instnName = rs.getString(INSTN_NAME);
				final String instnLang = rs.getString(INSTN_LANG);
				final String instnStudentNumLabel = 
						rs.getString(INSTN_STUDENTNUMLABEL);
				final int gracePeriod = rs.getInt(INSTN_GRACE);
				final Institution instn = new Institution(instnId, 
						instnName, instnLang, instnStudentNumLabel, gracePeriod);
				institutions.add(instn);
			} // while there's a result
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return institutions.toArray(new Institution[institutions.size()]);
	} // getInstitutions(boolean)

	/** Adds a new institution to the database if it's not already in it.
	 * If the language is English, records null.
	 * @param	con	database connection
	 * @param	instn	institution to be saved, containing a name and an ID
	 * number of 0; method puts new ID number back into this object
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	static void addInstitution(Connection con, Institution instn) 
			throws SQLException {
		final String SELF = "InstitutionRW.addInstitution: ";
		final String instnName = instn.getName();
		String qry = 
				SELECT + INSTN_ID
				+ FROM + INSTITUTIONS
				+ WHERE + INSTN_NAME + LIKE + QMARK;
		SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				instnName);
		int instnId = 0;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) instnId = rs.getInt(INSTN_ID);
			if (instnId == 0) {
				instnId = nextSequence(con, INSTITUTIONS_SEQ);
				final String instnLang = instn.getPrimaryLanguage();
				final boolean toEnglish = 
						Utils.isEmpty(instnLang) || ENGLISH.equals(instnLang);
				final String[] fields = new String[] {
						INSTN_ID,
						INSTN_NAME,
						INSTN_STUDENTNUMLABEL};
				if (!toEnglish) {
					Utils.add(fields, INSTN_LANG);
				} // if not to English
				qry = getInsertIntoValuesQMarksSQL(INSTITUTIONS, fields);
				sql_vals = new SQLWithQMarks(qry, 
						instnId,
						instnName,
						instn.getStudentNumLabel());
				if (!toEnglish) {
					sql_vals.addValue(instnLang);
				} // if not to English
				debugPrint(SELF, sql_vals);
				tryUpdate(con, sql_vals);
			} // if institution is not already in DB
			instn.setId(instnId);
		} finally {
			closeStmtAndRs(stmt, rs);
		} // try
	} // addInstitution(Connection, Institution)

	/** Sets the primary language of instruction of an institution, or null if
	 * it is English.
	 * @param	instnId	the institution's ID
	 * @param	language	the institution's primary language of instruction
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setPrimaryLanguage(int instnId, String language) 
			throws DBException {
		final String SELF = "InstitutionRW.setPrimaryLanguage: ";
		final boolean toEnglish = 
				Utils.isEmpty(language) || ENGLISH.equals(language);
		final String qry = toString(
				UPDATE + INSTITUTIONS 
				+ SET + INSTN_LANG + EQUALS, toEnglish ? NULL : QMARK,
				WHERE + INSTN_ID + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry);
		if (toEnglish) {
			sql_vals.addValue(instnId);
		} else {
			sql_vals.addValues(language, instnId); 
		} // if toEnglish
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
	} // setPrimaryLanguage(int, String)

	/** Changes an institution's name for the student ID number,
	 * an institution's name for the systemwide username/login ID,
	 * and an institution's email domain.
	 * @param	con	database connection
	 * @param	instn	institution whose name for the student ID number is
	 * being changed
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	static void setInstitutionStudentNumLabel(Connection con,
			Institution instn) throws SQLException {
		final String SELF = "InstitutionRW.setInstitutionStudentNumLabel: ";
		if (instn.getId() < 0) instn.setId(-instn.getId());
		final String qry = 
				UPDATE + INSTITUTIONS 
				+ SET + INSTN_STUDENTNUMLABEL + EQUALS + QMARK
				+ WHERE + INSTN_ID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				instn.getStudentNumLabel(),
				instn.getId());
		debugPrint(SELF, sql_vals);
		tryUpdate(con, sql_vals);
	} // setInstitutionStudentNumLabel(Connection, Institution)

	/** Sets an institution as being exempt from paying to use ACE.
	 * @param	instn	the institution
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setExempt(Institution instn) throws DBException {
		setExempt(instn.getId());
	} // setExempt(Institution)

	/** Sets an institution as being exempt from paying to use ACE.
	 * @param	id	unique ID of the institution
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setExempt(int id) throws DBException {
		setGracePeriod(id, -1);
	} // setExempt(int)

	/** Sets the number of days students at an institution can avoid paying to 
	 * use ACE.
	 * @param	instn	the institution
	 * @param	gracePeriod	the grace period in days
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setGracePeriod(Institution instn, int gracePeriod) 
			throws DBException {
		setGracePeriod(instn.getId(), gracePeriod);
	} // setGracePeriod(Institution, int)

	/** Sets the number of days students at an institution can avoid paying to 
	 * use ACE.
	 * @param	id	unique ID of the institution
	 * @param	gracePeriod	the grace period in days
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setGracePeriod(int id, int gracePeriod) 
			throws DBException {
		final String SELF = "InstitutionRW.setGracePeriod: ";
		final String qry = 
				UPDATE + INSTITUTIONS 
				+ SET + INSTN_GRACE + EQUALS + QMARK
				+ WHERE + INSTN_ID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				gracePeriod,
				id); 
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
	} // setGracePeriod(int, int)

	/** Sets the name of an institution, allowing correction of abbreviations
	 * and capitalizations. 
	 * @param	id	unique ID of the institution
	 * @param	name	the new name of the institution
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setName(int id, String name) throws DBException {
		final String SELF = "InstitutionRW.setName: ";
		final String qry = 
				UPDATE + INSTITUTIONS 
				+ SET + INSTN_NAME + EQUALS + QMARK
				+ WHERE + INSTN_ID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				name,
				id); 
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
	} // setName(int, String)

	/** Constructor to disable external instantiation. */
	private InstitutionRW() { }

} // InstitutionRW
