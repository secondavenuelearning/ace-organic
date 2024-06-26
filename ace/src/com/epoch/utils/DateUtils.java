package com.epoch.utils;

import static com.epoch.utils.utilConstants.DateConstants.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;

/** For manipulating java.util.Date. */ 
public final class DateUtils {

	/** Date format for display: MM-dd-yyyy HH:mm. */
	private static SimpleDateFormat parseFormat = 
			new SimpleDateFormat("MM-dd-yyyy HH:mm", Locale.US);
	/** Alt date format for display: dd-MM-yyyy HH:mm. */
	private static SimpleDateFormat parseFormatAlt = 
			new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US);
	/** Date format for Java strings that will be written to the database: 
	 * yyyy/MM/dd HH:mm:ss. */
	private static SimpleDateFormat dbFormat = 
			new SimpleDateFormat(DB_DATE_FORMAT[JAVA], Locale.US);
	/** Format for date only: MM-dd-yyyy. */ 
	private static SimpleDateFormat shortDateFormat =
			new SimpleDateFormat("MM-dd-yyyy", Locale.US);
	/** Alt format for date only: dd-MM-yyyy. */ 
	private static SimpleDateFormat shortDateFormatAlt =
			new SimpleDateFormat("dd-MM-yyyy", Locale.US);
	/** Format for approximate time only: HH:mm. */
	private static SimpleDateFormat shortTimeFormat =
			new SimpleDateFormat("HH:mm", Locale.US);
	/** Format for exact time: HH:mm:ss. */
	private static SimpleDateFormat exactTimeFormat =
			new SimpleDateFormat("HH:mm:ss", Locale.US);
	/** Common date format: MMM d, yyyy. */
	private static SimpleDateFormat dateFormat =
			new SimpleDateFormat("MMM d, yyyy", Locale.US);
	/** Alt common date format: d MMM yyyy. */
	private static SimpleDateFormat dateFormatAlt =
			new SimpleDateFormat("d MMM yyyy", Locale.US);
	/** Common time format: h:mm aa, zzz. (aa = am/pm, zzz = time zone) */
	private static SimpleDateFormat timeFormat =
			new SimpleDateFormat("h:mm aa, zzz", Locale.US);
	/** Common date-time format: MMM d, yyyy, h:mm aa.  (aa = am/pm) */
	private static SimpleDateFormat dateTimeFormat =
			new SimpleDateFormat("MMM d, yyyy, h:mm aa", Locale.US);
	/** Alt common date-time format: d MMM yyyy, h:mm aa.  (aa = am/pm) */
	private static SimpleDateFormat dateTimeFormatAlt =
			new SimpleDateFormat("d MMM yyyy, h:mm aa", Locale.US);

	/** Converts a Date into date &amp; time in standard time zone (US Eastern). 
	 * @param	d	a Date
	 * @return	MMM d, yyyy, h:mm aa, zzz
	 */
	public static String getString(Date d) {		
		final TimeZone tZ = TimeZone.getDefault();
		return getStringDate(d, tZ) + ", " + getStringTime(d, tZ);
	} // getString(Date)

	/** Converts a Date into date &amp; time in any time zone. 
	 * @param	d	a Date
	 * @param	tZ	a TimeZone
	 * @return	MMM d, yyyy, h:mm aa, zzz
	 */
	public static String getString(Date d, TimeZone tZ) {		
		return getStringDate(d, tZ) + ", " + getStringTime(d, tZ);
	} // getString(Date, TimeZone)

	/** Converts a Date into date &amp; time in standard time zone (US Eastern). 
	 * @param	d	a Date
	 * @return	MMM d, yyyy, h:mm aa
	 */
	public static String getStringNoTimeZone(Date d) {		
		final TimeZone tZ = TimeZone.getDefault();
		return getStringNoTimeZone(d, tZ);
	} // getStringNoTimeZone(Date)

	/** Converts a Date into date &amp; time in standard time zone (US Eastern). 
	 * @param	d	a Date
	 * @param	day1st	put the day first
	 * @return	MMM d, yyyy, h:mm aa or d MMM, yyyy, h:mm aa
	 */
	public static String getStringNoTimeZone(Date d, boolean day1st) {		
		final TimeZone tZ = TimeZone.getDefault();
		return getStringNoTimeZone(d, tZ, day1st);
	} // getStringNoTimeZone(Date, boolean)

	/** Converts a Date into date &amp; time in any time zone. 
	 * @param	d	a Date
	 * @param	tZ	a TimeZone
	 * @return	MMM d, yyyy, h:mm aa
	 */
	public static String getStringNoTimeZone(Date d, TimeZone tZ) {		
		return getStringNoTimeZone(d, tZ, false);
	} // getStringNoTimeZone(Date, TimeZone)

	/** Converts a Date into date &amp; time in any time zone. 
	 * @param	d	a Date
	 * @param	tZ	a TimeZone
	 * @param	day1st	put the day first
	 * @return	MMM d, yyyy, h:mm aa or d MMM, yyyy, h:mm aa
	 */
	public static String getStringNoTimeZone(Date d, TimeZone tZ, 
			boolean day1st) {		
		if (day1st) {
			synchronized (dateTimeFormatAlt) {
				dateTimeFormatAlt.setTimeZone(tZ);
				return dateTimeFormatAlt.format(d);
			}
		} else {
			synchronized (dateTimeFormat) {
				dateTimeFormat.setTimeZone(tZ);
				return dateTimeFormat.format(d);
			}
		}
	} // getStringNoTimeZone(Date, TimeZone, boolean)

	/** Converts a Date into a date string in standard time zone (US Eastern). 
	 * @param	d	a Date
	 * @return	MMM d, yyyy
	 */
	public static String getStringDate(Date d) {
		final TimeZone tZ = TimeZone.getDefault();
		return getStringDate(d, tZ);
	} // getStringDate(Date)

	/** Converts a Date into a date string in standard time zone (US Eastern). 
	 * @param	d	a Date
	 * @param	day1st	put the day first
	 * @return	MMM d, yyyy or d MMM, yyyy
	 */
	public static String getStringDate(Date d, boolean day1st) {
		final TimeZone tZ = TimeZone.getDefault();
		return getStringDate(d, tZ, day1st);
	} // getStringDate(Date, boolean)

	/** Converts a Date into a date string in any time zone. 
	 * @param	d	a Date
	 * @param	tZ	a TimeZone
	 * @return	MMM d, yyyy
	 */
	public static String getStringDate(Date d, TimeZone tZ) {
		return getStringDate(d, tZ, false);
	} // getStringDate(Date, TimeZone)

	/** Converts a Date into a date string in any time zone. 
	 * @param	d	a Date
	 * @param	tZ	a TimeZone
	 * @param	day1st	put the day first
	 * @return	MMM d, yyyy or d MMM, yyyy
	 */
	public static String getStringDate(Date d, TimeZone tZ, boolean day1st) {
		if (day1st) {
			synchronized (dateFormatAlt) {
				dateFormatAlt.setTimeZone(tZ);
				return dateFormatAlt.format(d);
			}
		} else {
			synchronized (dateFormat) {
				dateFormat.setTimeZone(tZ);
				return dateFormat.format(d);
			}
		}
	} // getStringDate(Date, TimeZone, boolean)

	/** Converts a Date into a time string in standard time zone (US Eastern). 
	 * @param	d	a Date
	 * @return	h:mm aa, zzz
	 */
	public static String getStringTime(Date d) {
		final TimeZone tZ = TimeZone.getDefault();
		return getStringTime(d, tZ);
	} // getStringTime(Date)
	
	/** Converts a Date into a time string in any time zone. 
	 * @param	d	a Date
	 * @param	tZ	a TimeZone
	 * @return	h:mm aa, zzz
	 */
	public static String getStringTime(Date d, TimeZone tZ) {
		synchronized (timeFormat) {
			timeFormat.setTimeZone(tZ);
			return timeFormat.format(d);
		}
	} // getStringTime(Date, TimeZone)
	
	/** Converts a Date into a date string in standard time zone (US Eastern). 
	 * @param	d	a Date
	 * @return	MM-dd-yyyy
	 */
	public static String getShortDate(Date d) {
		final TimeZone tZ = TimeZone.getDefault();
		return getShortDate(d, tZ);
	} // getShortDate(Date)
	
	/** Converts a Date into a date string in standard time zone (US Eastern). 
	 * @param	d	a Date
	 * @param	day1st	put the day first
	 * @return	MM-dd-yyyy or dd-MM-yyyy
	 */
	public static String getShortDate(Date d, boolean day1st) {
		final TimeZone tZ = TimeZone.getDefault();
		return getShortDate(d, tZ, day1st);
	} // getShortDate(Date, boolean)
	
	/** Converts a Date into a date string in a time zone. 
	 * @param	d	a Date
	 * @param	tZ	a TimeZone
	 * @return	MM-dd-yyyy
	 */
	public static String getShortDate(Date d, TimeZone tZ) {
		return getShortDate(d, tZ, false);
	} // getShortDate(Date, TimeZone)

	/** Converts a Date into a date string in a time zone. 
	 * @param	d	a Date
	 * @param	tZ	a TimeZone
	 * @param	day1st	put the day first
	 * @return	MM-dd-yyyy or dd-MM-yyyy
	 */
	public static String getShortDate(Date d, TimeZone tZ, boolean day1st) {
		if (day1st) {
			synchronized (shortDateFormatAlt) {
				shortDateFormatAlt.setTimeZone(tZ);
				return shortDateFormatAlt.format(d);
			}
		} else {
			synchronized (shortDateFormat) {
				shortDateFormat.setTimeZone(tZ);
				return shortDateFormat.format(d);
			}
		}
	} // getShortDate(Date, TimeZone, boolean)

	/** Converts a Date into a time string in standard time zone (US Eastern). 
	 * @param	d	a Date
	 * @return	HH:mm
	 */
	public static String getShortTime(Date d) {
		final TimeZone tZ = TimeZone.getDefault();
		return getShortTime(d, tZ);
	} // getShortTime(Date)
	
	/** Converts a Date into a time string in any time zone. 
	 * @param	d	a Date
	 * @param	tZ	a TimeZone
	 * @return	HH:mm
	 */
	public static String getShortTime(Date d, TimeZone tZ) {
		synchronized (shortTimeFormat) {
			shortTimeFormat.setTimeZone(tZ);
			return shortTimeFormat.format(d);
		}
	} // getShortTime(Date, TimeZone)
	
	/** Converts a Date into a time string in standard time zone (US Eastern). 
	 * @param	d	a Date
	 * @return	HH:mm:ss
	 */
	public static String getExactTime(Date d) {
		final TimeZone tZ = TimeZone.getDefault();
		return getExactTime(d, tZ);
	} // getExactTime(Date)
	
	/** Converts a Date into a time string in any time zone. 
	 * @param	d	a Date
	 * @param	tZ	a TimeZone
	 * @return	HH:mm:ss
	 */
	public static String getExactTime(Date d, TimeZone tZ) {
		synchronized (exactTimeFormat) {
			exactTimeFormat.setTimeZone(tZ);
			return exactTimeFormat.format(d);
		}
	} // getExactTime(Date, TimeZone)
	
	/** Converts a Date into a time string in standard time zone (US Eastern). 
	 * @param	d	a Date
	 * @return	HH:mm:ss
	 */
	public static String getDbTime(Date d) {
		final TimeZone tZ = TimeZone.getDefault();
		return getDbTime(d, tZ);
	} // getDbTime(Date)
	
	/** Converts a Date into a time string in any time zone. 
	 * @param	d	a Date
	 * @param	tZ	a TimeZone
	 * @return	HH:mm:ss
	 */
	public static String getDbTime(Date d, TimeZone tZ) {
		synchronized (dbFormat) {
			dbFormat.setTimeZone(tZ);
			return dbFormat.format(d);
		}
	} // getDbTime(Date, TimeZone)
	
	/** Gets all the names of the available time zones. 
	 * @return	array of all names of all the time zones.
	 */
	public static String[] outputTimeZones() {
		return TimeZone.getAvailableIDs();
	} // outputTimeZones()

	/** Converts mm-dd-yyyy hh:mm to a Date. 
	 * @param	dateString	string describing the date
	 * @param	timeString	string describing the time
	 * @param	tZ	the timezone
	 * @return	a Date, or null if the formats or data are invalid 
	 */
	public static Date parseDate(String dateString, String timeString, 
			TimeZone tZ) {
		return parseDate(dateString, timeString, tZ, false);
	} // parseDate(String, String, TimeZone)

	/** Converts mm-dd-yyyy hh:mm or dd-mm-yyyy hh:mm to a Date. 
	 * @param	dateString	string describing the date
	 * @param	timeString	string describing the time
	 * @param	tZ	the timezone
	 * @param	day1st	put the day first
	 * @return	a Date, or null if the formats or data are invalid 
	 */
	public static Date parseDate(String dateString, String timeString, 
			TimeZone tZ, boolean day1st) {
		try {
			if (day1st) {
				synchronized (parseFormatAlt) {
					parseFormatAlt.setTimeZone(tZ);
					return parseFormatAlt.parse(Utils.toString(
							dateString, ' ', timeString));
				}
			} else {
				synchronized (parseFormat) {
					parseFormat.setTimeZone(tZ);
					return parseFormat.parse(Utils.toString(
							dateString, ' ', timeString));
				}
			}
		} catch (ParseException pe) {
			return null;
		}
	} // parseDate(String, String, TimeZone, boolean)

	/** Converts yyyy/dd/MM HH:mm:ss to a Date. 
	 * @param	dateString	string describing the date
	 * @return	a Date, or null if the formats or data are invalid 
	 */
	public static Date parseDbDate(String dateString) {
		try {
			synchronized (dbFormat) {
				return dbFormat.parse(dateString);
			}
		} catch (ParseException pe) {
			return null;
		}
	} // parseDbDate(String)

	/** Converts MMM d, yyyy, h:mm aa to a Date.
	 * @param	dateString	string describing the date
	 * @param	tz	the timezone
	 * @return	a Date, or null if the formats or data are invalid 
	 */
	public static Date parseStringNoTimeZone(String dateString,
			TimeZone tz) {
		try {
			synchronized (dateTimeFormat) {
				dateTimeFormat.setTimeZone(tz);
				return dateTimeFormat.parse(dateString);
			}
		} catch (ParseException pe) {
			return null;
		}
	} // parseStringNoTimeZone(String, TimeZone)

	/** Converts seconds to days.
	 * @param	secs	the time interval in seconds
	 * @return	the time interval in days
	 */
	public static double secsToDays(long secs) {
		return convertUnits((double) secs, SECS, DAYS);
	} // secsToDays(long)

	/** Converts days to seconds.
	 * @param	days	the time interval in days
	 * @return	the time interval in seconds
	 */
	public static int daysToSecs(double days) {
		return (int) convertUnits(days, DAYS, SECS);
	} // daysToSecs(double)

	/** Converts minutes to seconds.
	 * @param	mins	the time interval in minutes
	 * @return	the time interval in seconds
	 */
	public static int minsToSecs(double mins) {
		return (int) convertUnits(mins, MINS, SECS);
	} // minsToSecs(double)

	/** Converts seconds to minutes.
	 * @param	secs	the time interval in seconds
	 * @return	the time interval in minutes 
	 */
	public static int secsToMins(long secs) {
		return (int) convertUnits(secs, SECS, MINS);
	} // minsToSecs(long)

	/** Converts a time interval among seconds/minutes/hours/days.
	 * @param	time	the time interval
	 * @param	from	the unit of the time interval
	 * @param	to	the desired unit of the time interval
	 * @return	the time interval in the new unit
	 */
	private static double convertUnits(double time, int from, int to) {
		double newTime = time;
		int at = from;
		while (at != to) {
			newTime *= (from > to ? FACTORS[--at]
					: 1.0 / FACTORS[at++]);
		} // while
		return newTime;
	} // convertUnits(double, int, int)

	/** Gets the index of a unit in NAMES.
	 * @param	srch	a unit
	 * @return	index of the unit in NAMES, or -1 if not found
	 */
	private static int nameNum(String srch) {
		int num = 0;
		for (final String name : NAMES) {
			if (name.equals(srch)) return num;
			num++;
		} // for each name
		return -1;
	} // nameNum(String)

	/** Allows to test convertUnits(). 
	 * @param	args	time interval, from unit, to unit; units are 
	 * sec, min, h, d
	 */
	public static void main(String[] args) {
		try {
			final double time = Double.parseDouble(args[0]);
			final int from = nameNum(" " + args[1]);
			final int to = nameNum(" " + args[2]);
			if (Utils.among(-1, from, to)) {
				System.out.println("Usage:\n"
						+ "\tjava com/epoch/DateUtils num from to\n"
						+ "where from and to are among " 
						+ Arrays.toString(NAMES));
			} else {
				final double newTime = convertUnits(time, from, to);
				System.out.println(time + NAMES[from] + " = " 
						+ newTime + NAMES[to]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	} // main

	/** Disables external instantiation. */
	private DateUtils() { }

} // DateUtils
