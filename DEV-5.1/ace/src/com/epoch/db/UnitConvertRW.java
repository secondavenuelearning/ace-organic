package com.epoch.db;

import static com.epoch.constants.UnitConvertConstants.*; 
import com.epoch.exceptions.DBException;
import com.epoch.utils.Utils; 

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

/** Contains all database read and write operations pertaining to 
 * conversions of one unit to another.   All methods are static.  */
public final class UnitConvertRW extends DBCommon {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

	/** Table for storing factors for converting numbers in one unit into
	 * another unit. */
	private static final String UNIT_CONVERSIONS = "unit_conversions_v1";
		/** Field in UNIT_CONVERSIONS. */
		private static final String UNIT_FROM = "unit_from";
		/** Field in UNIT_CONVERSIONS. */
		private static final String UNIT_TO = "unit_to";
		/** Field in UNIT_CONVERSIONS. */
		private static final String UNIT_POWER = "power"; 
		/** Field in UNIT_CONVERSIONS. */
		private static final String UNIT_FACTOR = "factor"; 
	/** Parameter for getUnitConversion(). */
	private static final boolean TRY_HARDER = true;

	/** Saves conversions of one unit to another and vice versa.
	 * @param	unitFrom	the unit to be converted
	 * @param	unitTo	the unit to which to convert
	 * @param	powerFactor	the power to which to raise and the factor 
	 * by which to multiply a number in unitFrom to convert it to a 
	 * number in unitTo
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setUnitConversion(String unitFrom, 
			String unitTo, double[] powerFactor) throws DBException {
		Connection con = null;
		try { 
			con = getPoolConnection();
			setUnitConversion(con, unitFrom, unitTo, powerFactor);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // setUnitConversion(String, String, double[])

	/** Saves conversions of one unit to another and vice versa.
	 * @param	con	database connection
	 * @param	unitFrom	the unit to be converted
	 * @param	unitTo	the unit to which to convert
	 * @param	powerFactor	the power to which to raise and the factor 
	 * by which to multiply a number in unitFrom to convert it to a 
	 * number in unitTo
	 * @throws	DBException	if there's a problem writing to the database
	 */
	private static void setUnitConversion(Connection con, 
			String unitFrom, String unitTo, double[] powerFactor) 
			throws DBException {
		final String SELF = "UnitConvertRW.setUnitConversion: ";
		PreparedStatement stmt = null;
		final double[] existing = 
				getUnitConversion(con, unitFrom, unitTo, !TRY_HARDER);
		if (existing != null && existing[FACTOR] != 0.0) {
			Utils.alwaysPrint(SELF + "already have conversion of ",
					unitFrom, " into ", unitTo, " with power ",
					existing[POWER], " and factor ", existing[FACTOR]);
		} else try { 
			final String[] fields = new String[] {
					UNIT_FROM,
					UNIT_TO,
					UNIT_POWER,
					UNIT_FACTOR};
			final String qry = 
					getInsertIntoValuesQMarksSQL(UNIT_CONVERSIONS, fields);
			debugPrint(SELF, qry);
			con.setAutoCommit(false);
			stmt = con.prepareStatement(qry);
			int batchNum = 1;
			for (int num = 0; num < 2; num++) {
				StringBuilder joinedValues;
				if (num == 0) {
					joinedValues = setValues(stmt,
							quotes(unitFrom),
							quotes(unitTo),
							powerFactor[POWER],
							powerFactor[FACTOR]);
				} else {
					final double inversePower = 1.0 / powerFactor[POWER];
					final double inverseFactor = 1.0 / powerFactor[FACTOR];
					joinedValues = setValues(stmt,
							quotes(unitTo),
							quotes(unitFrom),
							inversePower,
							Math.pow(inverseFactor, inversePower));
				} // if num
				debugPrint(SELF, qry, "; ", joinedValues); 
				debugPrint(SELF, "batch ", batchNum++, ": ", joinedValues); 
				stmt.addBatch();
			} // for each time
			stmt.executeBatch();
			con.commit();
		} catch (SQLException e) {
			System.out.println(SELF + "SQL exception while "
					+ "saving conversion");
			rollbackConnection(con);
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(null, stmt, null);
		}
	} // setUnitConversion(Connection, String, String, double[])

	/** Gets a conversion factor from the database.
	 * @param	unitFrom	the unit to be converted
	 * @param	unitTo	the unit to which to convert
	 * @return	the power to which to raise and the factor by which to 
	 * multiply a number in unitFrom to convert it to a number in unitTo
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static double[] getUnitConversion(String unitFrom,
			String unitTo) throws DBException {
		Connection con = null;
		try { 
			con = getPoolConnection();
			return getUnitConversion(con, unitFrom, unitTo, TRY_HARDER);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // getUnitConversion(String, String)

	/** Gets a conversion factor from the database.
	 * @param	con	database connection
	 * @param	unitFrom	the unit to be converted
	 * @param	unitTo	the unit to which to convert
	 * @param	tryHarder	when tru, if there's no direct conversion, see if 
	 * there's an indirect one
	 * @return	the power to which to raise and the factor by which to 
	 * multiply a number in unitFrom to convert it to a number in unitTo
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static double[] getUnitConversion(Connection con, 
			String unitFrom, String unitTo, boolean tryHarder) 
			throws DBException {
		final String SELF = "UnitConvertRW."
				+ "getUnitConversion: ";
		final double[] powerFactor = new double[2];
		Arrays.fill(powerFactor, 0.0);
		final String qry = toString(
				SELECT, joinAll( 
					UNIT_POWER, 
					UNIT_FACTOR),
				FROM + UNIT_CONVERSIONS 
				+ WHERE + UNIT_FROM + EQUALS + QMARK
				+ AND + UNIT_TO + EQUALS + QMARK);
		SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				unitFrom,
				unitTo);
		debugPrint(SELF, sql_vals);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) {
				powerFactor[POWER] = rs.getDouble(UNIT_POWER);
				powerFactor[FACTOR] = rs.getDouble(UNIT_FACTOR);
				debugPrint(SELF + "unitFrom = ", unitFrom,
						", unitTo = ", unitTo, 
						", power = ", powerFactor[POWER],
						", factor = ", powerFactor[FACTOR]);
			} else if (tryHarder) {
			 	debugPrint(SELF + "no result for converting ",
						unitFrom, " directly to ", unitTo, 
						"; try a two-step conversion.");
				sql_vals.setSql(
						SELECT, joinAll(
							UNIT_TO,
							UNIT_POWER, 
							UNIT_FACTOR),
						FROM + UNIT_CONVERSIONS
						+ WHERE + UNIT_FROM + EQUALS + QMARK
						+ AND + UNIT_TO + IN, parensBuild(
							SELECT + UNIT_FROM
							+ FROM + UNIT_CONVERSIONS
							+ WHERE + UNIT_TO + EQUALS + QMARK));
				debugPrint(SELF, sql_vals);
				PreparedStatement newStmt = null;
				ResultSet newRs = null;
				try {
					newStmt = getStatement(con, sql_vals);
					newRs = newStmt.executeQuery();
					if (newRs.next()) {
						final String interUnit = newRs.getString(UNIT_TO);
						final double power1 = newRs.getDouble(UNIT_POWER);
						final double factor1 = newRs.getDouble(UNIT_FACTOR);
						final String newQry = toString(
								SELECT, joinAll( 
									UNIT_POWER, 
									UNIT_FACTOR),
								FROM + UNIT_CONVERSIONS 
								+ WHERE + UNIT_FROM + EQUALS + QMARK
								+ AND + UNIT_TO + EQUALS + QMARK);
						sql_vals = new SQLWithQMarks(newQry,
								interUnit,
								unitTo);
						debugPrint(SELF, sql_vals);
						newStmt = getStatement(con, sql_vals);
						newRs = newStmt.executeQuery();
						if (newRs.next()) {
							final double power2 = newRs.getDouble(UNIT_POWER);
							final double factor2 = newRs.getDouble(UNIT_FACTOR);
							powerFactor[POWER] = power1 * power2;
							powerFactor[FACTOR] = 
									factor2 * Math.pow(factor1, power2);
							debugPrint(SELF + "unitFrom = ", unitFrom,
									", interUnit = ", interUnit,
									", power1 = ", power1,
									", factor1 = ", factor1,
									", unitTo = ", unitTo, 
									", power2 = ", power2,
									", factor2 = ", factor2,
									", total power = ", powerFactor[POWER],
									", total factor = ", powerFactor[FACTOR]);
						} else debugPrint(SELF + "no result for converting ",
								unitFrom, " to ", interUnit, " and thence to ",
								unitTo); // unlikely at this point
					} else debugPrint(SELF + "no result for converting ",
							unitFrom, " to ", unitTo, " via another unit.");
				} catch (SQLException e) {
					System.out.println(SELF + "caught SQLException.");
					throw new DBException(e.getMessage());
				} finally {
					closeConnection(null, newStmt, newRs);
				} // try
			} // if a simple conversion is present
		} catch (SQLException e) {
			System.out.println(SELF + "caught SQLException.");
			throw new DBException(e.getMessage());
		} finally {
			closeStmtAndRs(stmt, rs);
		} // try
		return powerFactor;
	} // getUnitConversion(Connection, String, String)

	/** Removes a conversion factor from the database.
	 * @param	unitFrom	the unit to be converted
	 * @param	unitTo	the unit to which to convert
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void removeUnitConversion(String unitFrom,
			String unitTo) throws DBException {
		Connection con = null;
		try { 
			con = getPoolConnection();
			removeUnitConversion(con, unitFrom, unitTo);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // removeUnitConversion(String, String)

	/** Removes a conversion factor from the database.
	 * @param	con	database connection
	 * @param	unitFrom	the unit to be converted
	 * @param	unitTo	the unit to which to convert
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private static void removeUnitConversion(Connection con, 
			String unitFrom, String unitTo) throws SQLException {
		final String SELF = "UnitConvertRW."
				+ "removeUnitConversion: ";
		final String qry = toString(
				DELETE_FROM + UNIT_CONVERSIONS 
				+ WHERE, parens(
					UNIT_FROM + EQUALS + QMARK
					+ AND + UNIT_TO + EQUALS + QMARK),
				OR, parens(
					UNIT_FROM + EQUALS + QMARK
					+ AND + UNIT_TO + EQUALS + QMARK));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				unitFrom,
				unitTo,
				unitTo,
				unitFrom);
		debugPrint(SELF, sql_vals);
		tryUpdate(con, sql_vals);
	} // removeUnitConversion(Connection, String, String)

	/** Gets all unit conversions from the database.
	 * @return	all unit conversions as rows of an HTML table
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static String[][] getAllUnitConversions() throws DBException {
		Connection con = null;
		try { 
			con = getPoolConnection();
			return getAllUnitConversions(con);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // getAllUnitConversions()

	/** Gets all unit conversions from the database.
	 * @param	con	database connection
	 * @return	all unit conversions as rows of an HTML table
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private static String[][] getAllUnitConversions(Connection con) 
			throws NullPointerException, SQLException {
		final String SELF = "UnitConvertRW.getAllUnitConversions: ";
		final ArrayList<String[]> conversions = new ArrayList<String[]>();
		final String qry = toString(
				SELECT, joinAll(
					UNIT_FROM,
					UNIT_TO,
					UNIT_POWER,
					UNIT_FACTOR),
				FROM + UNIT_CONVERSIONS,
				ORDER_BY, joinAll(
					toUpper(UNIT_FROM),
					toUpper(UNIT_TO)));
		debugPrint(SELF + qry);
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			while (rs != null && rs.next()) {
				final String unitFrom = rs.getString(UNIT_FROM);
				final String unitTo = rs.getString(UNIT_TO);
				final double power = rs.getDouble(UNIT_POWER);
				final double factor = rs.getDouble(UNIT_FACTOR);
				final String[] conversion = new String[4];
				conversion[0] = unitFrom;
				conversion[1] = unitTo;
				conversion[2] = Utils.doubleToStr(power);
				conversion[3] = Utils.doubleToStr(factor);
				conversions.add(conversion);
			} // while there are more conversions
		} catch (SQLException e) {
			System.out.println(SELF + "SQL exception "
					+ "while getting unit conversions");
			throw e;
		} finally {
			closeStmtAndRs(stmt, rs);
		} // try
		return conversions.toArray(new String[conversions.size()][4]);
	} // getAllUnitConversions(Connection)

	/** Constructor to disable external instantiation. */
	private UnitConvertRW() { }

} // UnitConvertRW
