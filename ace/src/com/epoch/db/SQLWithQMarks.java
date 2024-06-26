package com.epoch.db;

import com.epoch.utils.Utils; 
import java.util.List;

/** Class that contains an SQL statement containing question marks plus the
 * number of question marks. Useful when SQL gets passed among methods. */
public final class SQLWithQMarks extends DBCommon {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

	/** The SQL containing question marks. */
	private String sql;
	/** The values to substitute for the question marks in the SQL. */
	private Object[] values;

	/** Constructor for empty SQL.  */
	SQLWithQMarks() { 
		sql = "";
		values = new Object[0];
	} // SQLWithQMarks()

	/** Constructor used when the data to be substituted is already in an array;
	 * use in conjunction with setValues().
	 * @param	sqlStr	string containing the SQL
	 */
	SQLWithQMarks(String sqlStr) { 
		sql = sqlStr;
		values = new Object[0];
	} // SQLWithQMarks(String)

	/** Constructor used when the data to be substituted is already in an array;
	 * use in conjunction with setValues().
	 * @param	sqlBld	StringBuilder containing the SQL
	 */
	SQLWithQMarks(StringBuilder sqlBld) { 
		sql = sqlBld.toString();
		values = new Object[0];
	} // SQLWithQMarks(StringBuilder)

	/** Constructor used when the data to be substituted is already in an array;
	 * use in conjunction with setValues().
	 * @param	sqlCh	char containing the SQL
	 */
	SQLWithQMarks(char sqlCh) { 
		sql = String.valueOf(sqlCh);
		values = new Object[0];
	} // SQLWithQMarks(char)

	/** Constructor when the data to be substituted is stored in separate
	 * arguments. 
	 * @param	sqlCh	character containing the SQL
	 * @param	vals	the values to substitute into the SQL
	 */
	SQLWithQMarks(char sqlCh, Object... vals) { 
		sql = String.valueOf(sqlCh);
		initValues(vals);
	} // SQLWithQMarks(char, Object...)

	/** Constructor when the data to be substituted is stored in separate
	 * arguments. 
	 * @param	sqlStr	string containing the SQL
	 * @param	vals	the values to substitute into the SQL
	 */
	SQLWithQMarks(String sqlStr, Object... vals) { 
		sql = sqlStr;
		initValues(vals);
	} // SQLWithQMarks(String, Object...)

	/** Constructor when the data to be substituted is stored in separate
	 * arguments. 
	 * @param	sqlBld	StringBuilder containing the SQL
	 * @param	vals	the values to substitute into the SQL
	 */
	SQLWithQMarks(StringBuilder sqlBld, Object... vals) { 
		sql = (sqlBld != null ? sqlBld.toString() : "");
		initValues(vals);
	} // SQLWithQMarks(StringBuilder, Object...)

	/** Initializes the array containing the values to substitute into the SQL
	 * from a constructor.
	 * @param	vals	the values to substitute into the SQL
	 */
	private void initValues(Object[] vals) {
		values = (vals.length != 1 ? vals
				: vals[0] instanceof int[] ? Utils.toObject((int[]) vals[0])
				: vals[0] instanceof List ? ((List) vals[0]).toArray()
				: vals[0] instanceof SQLWithQMarks 
					? ((SQLWithQMarks) vals[0]).getValues()
				: vals[0] instanceof Object[] ? (Object[]) vals[0]
				: vals);
	} // initValues(Object[])

	/** Gets the SQL.
	 * @return	the SQL
	 */
	String getSQL()						{ return sql; }
	/** Gets the SQL.
	 * @return	the SQL
	 */
	String getSql()						{ return sql; }
	/** Gets the values to substitute into the SQL.
	 * @return	the values to substitute into the SQL
	 */
	Object[] getValues()				{ return values; }
	/** Sets the values to substitute into the SQL.
	 * @param	vals	the values to substitute into the SQL
	 */
	void setValues(Object... vals)		{ setValuesArray(vals); }
	/** Sets the values to substitute into the SQL.
	 * @param	vals	the values to substitute into the SQL
	 */
	void setValuesArray(Object[] vals)	{ initValues(vals); }

	/** Sets the SQL.
	 * @param	sqlBits	the SQL
	 */
	void setSql(Object... sqlBits) {
		sql = "";
		addToSqlArray(sqlBits);
	} // setSql(Object...)

	/** Adds SQL to the existing SQL.
	 * @param	sqlBits	the additional SQL as a series of strings or
	 * StringBuilders
	 */
	void addToSql(Object... sqlBits) { 
		addToSqlArray(sqlBits); 
	} // addToSql(Object...)

	/** Adds SQL to the existing SQL.
	 * @param	sqlBits	the additional SQL as an array of strings or
	 * StringBuilders
	 */
	void addToSqlArray(Object[] sqlBits) { 
		final StringBuilder sqlBld = new StringBuilder(sql);
		for (final Object sqlBit : sqlBits) {
			sqlBld.append(sqlBit);
		} // for each value
		sql = sqlBld.toString();
	} // addToSqlArray(Object[])

	/** Sets the values to substitute into the SQL.
	 * @param	vals	the values to substitute into the SQL
	 */
	void setValues(List<? extends Object> vals) { 
		if (vals != null) values = vals.toArray(new Object[vals.size()]);
	} // setValues(List<? extends Object>)

	/** Adds a value to substitute into the SQL.
	 * @param	val	the additional value to substitute into the SQL
	 */
	void addValue(int val) { 
		addValuesArray(new Object[] {Integer.valueOf(val)}); 
	} // addValue(int)

	/** Adds a value to substitute into the SQL.
	 * @param	val	the additional value to substitute into the SQL
	 */
	void addValue(Object val) { 
		addValuesArray(new Object[] {val}); 
	} // addValue(Object)

	/** Adds values to substitute into the SQL.
	 * @param	vals	the additional values to substitute into the SQL
	 */
	void addValues(Object... vals) { 
		addValuesArray(vals); 
	} // addValues(Object...)

	/** Adds values to substitute into the SQL.
	 * @param	sql_vals	contains the additional values to substitute into 
	 * the SQL
	 */
	void addValuesFrom(SQLWithQMarks sql_vals) { 
		addValuesArray(sql_vals.getValues()); 
	} // addValuesFrom(SQLWithQMarks)

	/** Adds an array of int values to substitute into the SQL.
	 * @param	vals	the array of int values to substitute into the SQL
	 */
	void addValuesArray(int[] vals) { 
		addValuesArray(Utils.toObject(vals)); 
	} // addValuesArray(int[])

	/** Adds an array of values to substitute into the SQL.
	 * @param	vals	the array of values to substitute into the SQL
	 */
	void addValuesArray(Object[] vals) { 
		values = Utils.addAll(values, vals); 
	} // addValuesArray(Object[])

	/** Adds a list of values to substitute into the SQL.
	 * @param	vals	the list of values to substitute into the SQL
	 */
	void addValuesArray(List<? extends Object> vals) {
		values = Utils.addAll(values, vals.toArray()); 
	} // addValuesArray(List<? extends Object>)

	/** Returns a string representation of this object, showing both the SQL and
	 * the data to be substituted for the question marks.
	 * @return	string representation of this object
	 */
	public String toString() {
		final StringBuilder bld = new StringBuilder().append(sql).append("; ");
		final boolean[] first = new boolean[] {true};
		for (final Object value : values) {
			addToString(bld, value, first);
		} // for each value
		return bld.toString();
	} // toString()

	/** Auxiliary method to convert this object into a string; called
	 * recursively when the value is an Object[].
	 * @param	bld	StringBuilder, modified by this method
	 * @param	value	the value to be added
	 * @param	first	whether this value is the first one to be added; stored
	 * in an array so calling method's value is also altered
	 */
	private void addToString(StringBuilder bld, Object value, boolean[] first) {
		if (value instanceof Object[]) {
			final Object[] array = (Object[]) value;
			for (final Object subvalue : array) {
				addToString(bld, subvalue, first);
			} // for each value in the array
		} else {
			if (first[0]) first[0] = false; else bld.append(", ");
			bld.append(value);
		} // if value is array
	} // addToString(StringBuilder, Object, boolean[])

} // SQLWithQMarks
