package com.epoch.db;

import static com.epoch.db.dbConstants.UserRWConstants.*;
import com.epoch.exceptions.DBException;
import com.epoch.utils.Utils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Database read-write operations related to the database schema. */
public final class DBSchema extends DBCommon {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

	/** Gets the foreign keys that depend on USERS.USER_ID.
	 * @param	con	active database connection
	 * @return	a list of 3-membered arrays containing the names of the 
	 * dependent tables and columns and the constraint with which they are
	 * associated
	 * @throws	DBException	if there's a problem reading the database
	 */
	static List<String[]> getDepTablesCols(Connection con) throws DBException {
		final String SELF = "DBSchema.getDepTablesCols: ";
		final String[] DEP_COLUMNS = new String[] 
				{"dependent_table", 
				"dependent_column"};
		final String qry = toString(
				SELECT, joinAll(
					"con.table_name" + AS + DEP_COLUMNS[TABL_NUM],
					"con.column_name" + AS + DEP_COLUMNS[COL_NUM],
					"con.constraint_name"),
				FROM + "user_cons_columns con"
					+ JOIN + "user_constraints"
						+ ON + "con.constraint_name" 
							+ EQUALS + "user_constraints.constraint_name"
					+ JOIN + "user_cons_columns rel"
						+ ON + "user_constraints.r_constraint_name" 
							+ EQUALS + "rel.constraint_name"
							+ AND + "con.position" + EQUALS + "rel.position"
				+ WHERE + "user_constraints.constraint_type" + EQUALS + QMARK
					+ AND + "rel.table_name" + EQUALS + QMARK
					+ AND + "rel.column_name" + EQUALS + QMARK
				+ ORDER_BY + DEP_COLUMNS[TABL_NUM]);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				'R',
				USERS.toUpperCase(),
				USER_ID.toUpperCase());
		debugPrint(SELF, sql_vals);
		final List<String[]> depTableCols = new ArrayList<String[]>();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final String[] depTableCol = new String[3];
				depTableCol[FK_NAME] = rs.getString("constraint_name");
				depTableCol[TABL_NUM] = rs.getString(DEP_COLUMNS[TABL_NUM]);
				depTableCol[COL_NUM] = rs.getString(DEP_COLUMNS[COL_NUM]);
				depTableCols.add(depTableCol);
			} // while there are results
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(null, stmt, rs);
		}
		debugPrint(SELF, depTableCols.size(), " foreign key(s) found.");
		return depTableCols;
	} // getDepTablesCols(Connection)

	/** So DBSchema has no constructor. */
	public DBSchema() { 
		// intentionally empty
	} 

} // DBSchema
