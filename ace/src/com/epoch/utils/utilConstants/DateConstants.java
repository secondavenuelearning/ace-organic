package com.epoch.utils.utilConstants;

/** Constants for manipulating dates. */ 
public class DateConstants {

	/** Numbers of seconds in a minute. */
	public static final double SECS_IN_MIN = 60;
	/** Numbers of minutes in an hour. */
	public static final double MINS_IN_HR = 60;
	/** Numbers of hours in a day. */
	public static final double HRS_IN_DAY = 24;
	/** Arrays of conversions. */
	public static final double[] FACTORS = new double[] 
			{SECS_IN_MIN, MINS_IN_HR, HRS_IN_DAY};
	/** Parameter for convertUnits(). */
	public static final int SECS = 0;
	/** Parameter for convertUnits(). */
	public static final int MINS = 1;
	/** Parameter for convertUnits(). */
	// public static final int HRS = 2; // not used
	/** Parameter for convertUnits(). */
	public static final int DAYS = 3;
	/** Array of unit names. */
	public static final String[] NAMES = new String[] 
			{" s", " min", " h", " d"};

	/** Designates the member of an array of date formats that should be used
	 * in Java methods such as those in SimpleDateFormat but not SQL. */
	public static final int JAVA = 0;
	/** Designates the member of an array of date formats that should be used
	 * in SQL but not in Java methods. */
	public static final int ORACLE = 1;
	/** Format of response date and time in database. Use HH24 instead of HH 
	 * when part of TO_DATE() function in Oracle. */
	public static final String[] DB_DATE_FORMAT = new String[]
			{	"yyyy/MM/dd HH:mm:ss", 
				"YYYY/MM/DD HH24:MI:SS"
			};

} // DateConstants
