package com.epoch.db.dbConstants;

/** The COURSES, ENROLLMENT, and PREENROLLMENT tables and their fields. */
public class CourseRWConstants {

	/** Table describing a course. */
	public static final String COURSES = "cw_courses_v3";
		/** Field in COURSES.  Unique identifier.  */
		public static final String CRS_ID = "id";
		/** Field in COURSES.  &rarr; USER_ID */
		public static final String CRS_INSTRUCTOR = "instructor_id";
		/** Field in COURSES.  */
		public static final String CRS_SERIALNO = "serial_no";
		/** Field in COURSES.  */
		public static final String CRS_FLAGS = "flags";
		/** Field in COURSES.  */
		public static final String CRS_NAME = "name"; // CLOB
		/** Field in COURSES.  */
		public static final String CRS_DESCRIP = "description"; // CLOB
		/** Field in COURSES.  */
		public static final String CRS_ENABLE_DATE = "enable_date";
		/** Field in COURSES.  */
		public static final String CRS_HOMEPG = "homepage"; // CLOB
		/** Field in COURSES.  */
		public static final String CRS_NOTES = "notes"; // CLOB
		/** Field in COURSES.  */
		public static final String CRS_BOOK = "book";
		/** Field in COURSES.  */
		public static final String CRS_ACEBOOKID = "ace_text_id";
		/** Field in COURSES.  */
		public static final String CRS_ZONE = "time_zone"; // CLOB
		/** Field in COURSES.  */
		public static final String CRS_PWDHASH = "password_hash";
		/** Field in COURSES.  */
		public static final String CRS_MAXEXT = "max_extensions_str";
		/** Field in COURSES.  */
		public static final String CRS_PORT = "marvin_live_port";
	/** Generates unique ids for courses. */
	public static final String COURSES_SEQ = "cw_courses_seq";
	/** Table describing which students are enrolled in which courses. */
	public static final String ENROLLMENT = "cw_course_enrollment_v3";
		/** Field in ENROLLMENT.  &rarr; CRS_ID */
		public static final String ENRL_COURSE = "course_id";
		/** Field in ENROLLMENT.  &rarr; USER_ID */
		public static final String ENRL_STUDENT = "student_id"; 
		/** Field in ENROLLMENT.  */
		public static final String ENRL_IS_TA = "isTA"; 
		/** Field in ENROLLMENT.  */
		public static final String ENRL_SERIALNO = "serial_no";
		/** Field in ENROLLMENT. */
		public static final String ENRL_1ST_ENTRY = "first_entry"; 
	/** Table containing students whom the instructor has enrolled but who 
	 * have not registered with ACE.  */
	public static final String PREENROLLMENT = "preenrollment_v4";
		/** Field in PREENROLLMENT.  &rarr; CRS_ID */
		public static final String PREENRL_COURSE = "course_id";
		/** Field in PREENROLLMENT.  */
		public static final String PREENRL_STUDENTNUM = "student_num"; // CLOB
		/** Field in PREENROLLMENT.  */
		public static final String PREENRL_SCHOOLID = "institution_id";
		/** Field in PREENROLLMENT.  */
		public static final String PREENRL_STUDENTNAME = "student_name"; // CLOB

	/** Parameter for getEnrolledStudents(). */
	public static final boolean INCLUDE_UNREGISTERED = true;
	/** Parameter for getEnrolledStudents() and getRegisteredUsers(). */
	public static final boolean INCLUDE_TAS = true;
	/** Parameter for getEnrolledStudentNums(). */
	public static final int ANY_INSTITUTION = 0;

} // CourseRWConstants
