package com.epoch.db;

/** Stores the database table and field names for locally 
 * authored questions and textbooks.  */
public final class DBLocalTables extends DBTables {

	/** Prefix for tables of locally authored questions. */
	public static final String LOCAL_PREFIX = "user_";
	/** Field in user_* tables. */
	public static final String Q_AUTHOR = "user_id";

	/** Constructor. */  
	DBLocalTables() {
		/** Table for locally authored questions. */
		QUESTIONS = LOCAL_PREFIX + super.QUESTIONS;
		/** Table for locally authored figures. */
		FIGURES = LOCAL_PREFIX + super.FIGURES;
		/** Table for locally authored evaluators. */
		EVALUATORS = LOCAL_PREFIX + super.EVALUATORS;
		/** Table for locally authored question data. */
		QUESTIONDATA = LOCAL_PREFIX + super.QUESTIONDATA;
		/** Table for locally authored captions and pulldown menu labels in 
		 * complete-the-table and energy diagram questions. */
		CAPTIONS = LOCAL_PREFIX + super.CAPTIONS;
		/** Table for filenames of locally authored figures that are images. */
		IMAGES = LOCAL_PREFIX + super.IMAGES;
		/** Old (ACE 3.4 and prior) table for  locally authored figures that 
		 * are images. */
		OLD_IMAGES = LOCAL_PREFIX + super.OLD_IMAGES;
		/** Whether tables in this class represent locally authored work. */
		local = true;
	} // DBLocalTables()

	/** Gets the name of the corresponding master table if the given name is of 
	 * a local table, and vice versa.
	 * @param	table	name of a master or local table
	 * @return	name of the corresponding table
	 */
	static String getAntitable(String table) {
		return (table.startsWith(LOCAL_PREFIX)
				? table.substring(LOCAL_PREFIX.length())
				: toString(LOCAL_PREFIX, table));
	} // getAntitable(String)

} // DBLocalTables
