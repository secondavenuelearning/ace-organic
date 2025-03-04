package com.epoch.db;

import com.epoch.exceptions.DBException;
import com.epoch.physics.CanonicalizedUnit;
import com.epoch.physics.physicsConstants.CanonicalizedUnitConstants;
import com.epoch.utils.Utils; 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Contains all database read and write operations pertaining to 
 * conversions of one unit to another.   All methods are static.  */
public final class CanonicalizedUnitRW extends DBCommon 
		implements CanonicalizedUnitConstants {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

	/** Table for storing factors for converting numbers in one unit into
	 * another unit. */
	private static final String CANONICALIZED_UNITS = "canonicalized_units_v1";
		/** Field in CANONICALIZED_UNITS. */
		private static final String UNIT_SYMBOL = "unit_symbol";
		/** Field in CANONICALIZED_UNITS. */
		private static final String UNIT_NAME = "unit_name";
		/** Field in CANONICALIZED_UNITS. */
		private static final String UNIT_MEASURES = "what_measures";
		/** Field in CANONICALIZED_UNITS. */
		private static final String UNIT_COEFF = "factor_coefficient"; 
		/** Field in CANONICALIZED_UNITS. */
		private static final String UNIT_POWER10 = "factor_power10"; 
		/** Field in CANONICALIZED_UNITS. */
		private static final String UNIT_METER = "meter_power"; 
		/** Field in CANONICALIZED_UNITS. */
		private static final String UNIT_GRAM = "gram_power"; 
		/** Field in CANONICALIZED_UNITS. */
		private static final String UNIT_SEC = "second_power"; 
		/** Field in CANONICALIZED_UNITS. */
		private static final String UNIT_AMP = "ampere_power"; 
		/** Field in CANONICALIZED_UNITS. */
		private static final String UNIT_DEGK = "kelvin_power"; 
		/** Field in CANONICALIZED_UNITS. */
		private static final String UNIT_MOLE = "mole_power"; 
		/** Field in CANONICALIZED_UNITS. */
		private static final String UNIT_CANDELA = "candela_power"; 

	/** Saves the definition of a unit, unless it's already in the table.
	 * @param	unit	the canonicalized unit
	 * @return	true if the insertion was successful
	 */
	public static boolean setUnit(CanonicalizedUnit unit) {
		final String SELF = "CanonicalizedUnitRW.setUnit: ";
		boolean success = true;
		final int[] unitSIPowers = unit.getSIUnitPowers();
		final String[] fields = new String[] {
					UNIT_SYMBOL,
					UNIT_NAME,
					UNIT_MEASURES,
					UNIT_COEFF,
					UNIT_POWER10,
					UNIT_METER,
					UNIT_GRAM,
					UNIT_SEC,
					UNIT_AMP,
					UNIT_DEGK,
					UNIT_MOLE,
					UNIT_CANDELA};
		final String qry = getInsertIntoValuesQMarksSQL(
				CANONICALIZED_UNITS, fields);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				unit.getSymbol(), 
				unit.getName(),
				unit.getWhatMeasures(),
				unit.getCoeff(),
				unit.getPower10(),
				unitSIPowers[METER],
				unitSIPowers[GRAM],
				unitSIPowers[SEC],
				unitSIPowers[AMP],
				unitSIPowers[DEGK],
				unitSIPowers[MOLE],
				unitSIPowers[CANDELA]);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		try { 
			con = getPoolConnection();
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			e.printStackTrace();
			success = false;
		} finally {
			closeConnection(con);
		}
		return success;
	} // setUnit(CanonicalizedUnit)

	/** Gets canonicalized units represented by a symbol, in order of name, 
	 * ignoring case.
	 * @param	symbol	the symbol of the units to get
	 * @return	an array of units represented by the symbol
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static CanonicalizedUnit[] getUnits(String symbol) 
			throws DBException {
		final String where = WHERE + UNIT_SYMBOL + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(where, symbol);
		return getUnits(sql_vals);
	} // getUnits(String)

	/** Gets canonicalized units that measure certain properties.
	 * @param	properties	measured properties
	 * @return	an array of units that measure the property
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static CanonicalizedUnit[] getUnits(String[] properties) 
			throws DBException {
		final StringBuilder whereBld = new StringBuilder();
		final List<String> percentPropsList = new ArrayList<String>();
		for (final String property : properties) {
 			appendTo(whereBld, whereBld.length() == 0 ? WHERE : OR, 
					UNIT_MEASURES + LIKE + QMARK);
			percentPropsList.add(percent(property));
		} // for each property
		final SQLWithQMarks sql_vals = 
				new SQLWithQMarks(whereBld, percentPropsList);
		return getUnits(sql_vals);
	} // getUnits(String[])

	/** Gets canonicalized units represented by a name, ignoring case.
	 * @param	name	the name of the unit to get
	 * @return	an array of units represented by the name
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static CanonicalizedUnit getUnitByName(String name) 
			throws DBException {
		final String where = WHERE + UNIT_NAME + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(where, name);
		final CanonicalizedUnit[] units = getUnits(sql_vals);
		return (!Utils.isEmpty(units) ? units[0] : null); 
	} // getUnitsByName(String)

	/** Gets all canonicalized units in order of symbol and name, 
	 * ignoring case.
	 * @return	an array of all units
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static CanonicalizedUnit[] getAllUnits() throws DBException {
		return getUnits(new SQLWithQMarks());
	} // getAllUnits()

	/** Gets canonicalized units that meet certain conditions, in order of 
	 * symbol and name, ignoring case.
	 * @param	whereSql_vals	SQL to insert into query and values to set for
	 * question marks
	 * @return	an array of units
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static CanonicalizedUnit[] getUnits(SQLWithQMarks whereSql_vals) 
			throws DBException {
		final String SELF = "CanonicalizedUnitRW.getUnits: ";
		final List<CanonicalizedUnit> units = 
				new ArrayList<CanonicalizedUnit>();
		final String qry = toString(
				SELECT, joinAll(
					UNIT_SYMBOL,
					UNIT_NAME,
					UNIT_MEASURES,
					UNIT_COEFF,
					UNIT_POWER10,
					UNIT_METER,
					UNIT_GRAM,
					UNIT_SEC,
					UNIT_AMP,
					UNIT_DEGK,
					UNIT_MOLE,
					UNIT_CANDELA), 
				FROM + CANONICALIZED_UNITS, 
				whereSql_vals.getSql(), 
				ORDER_BY, joinAll(
					toUpper(UNIT_SYMBOL),
					toUpper(UNIT_NAME)));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, whereSql_vals);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final int[] unitSIPowers = new int[SI_UNIT_SYMBOLS.length];
				unitSIPowers[METER] = rs.getInt(UNIT_METER);
				unitSIPowers[GRAM] = rs.getInt(UNIT_GRAM);
				unitSIPowers[SEC] = rs.getInt(UNIT_SEC);
				unitSIPowers[AMP] = rs.getInt(UNIT_AMP);
				unitSIPowers[DEGK] = rs.getInt(UNIT_DEGK);
				unitSIPowers[MOLE] = rs.getInt(UNIT_MOLE);
				unitSIPowers[CANDELA] = rs.getInt(UNIT_CANDELA);
				units.add(new CanonicalizedUnit(
						rs.getString(UNIT_SYMBOL),
						rs.getString(UNIT_NAME),
						rs.getString(UNIT_MEASURES),
						rs.getDouble(UNIT_COEFF),
						rs.getInt(UNIT_POWER10),
						unitSIPowers));
			} // while there are more units
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		debugPrint(SELF + "got ", units.size(), " canonicalized unit(s)");
		return units.toArray(new CanonicalizedUnit[units.size()]);
	} // getUnits(SQLWithQMarks)

	/** Gets all the measured properties of the canonicalized units.
	 * @return	an array of measured properties
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static String[] getAllUnitProperties() throws DBException {
		final String SELF = "CanonicalizedUnitRW.getAllUnitProperties: ";
		final List<String> properties = new ArrayList<String>();
		final String qry = SELECT_UNIQUE + UNIT_MEASURES
				+ FROM + CANONICALIZED_UNITS
				+ ORDER_BY + UNIT_MEASURES;
		debugPrint(SELF, qry);
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			while (rs.next()) {
				properties.add(rs.getString(UNIT_MEASURES));
			} // while there are more properties
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return properties.toArray(new String[properties.size()]);
	} // getAllUnitProperties()

	/** Removes a unit from the database.
	 * @param	unitSymbol	the symbol of the unit
	 * @param	unitName	the name of the unit
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void removeUnit(String unitSymbol, String unitName) 
			throws DBException {
		final String SELF = "CanonicalizedUnitRW.removeUnit: ";
		final String qry = DELETE_FROM + CANONICALIZED_UNITS 
				+ WHERE + UNIT_SYMBOL + EQUALS + QMARK 
				+ AND + UNIT_NAME + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				unitSymbol,
				unitName);
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
	} // removeUnit(String, String)

	/** Constructor to disable external instantiation. */
	private CanonicalizedUnitRW() { }

} // CanonicalizedUnitRW
