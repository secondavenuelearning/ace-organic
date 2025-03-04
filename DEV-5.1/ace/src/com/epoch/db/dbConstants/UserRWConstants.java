package com.epoch.db.dbConstants;

import com.epoch.courseware.courseConstants.UserConstants;

/** The USERS, COINSTRUCTORS, EXAM_IDS, and INSTITUTIONS tables and their fields. */
public class UserRWConstants implements UserConstants {

	/** Parameter for getOneUserData().  */
	public static final boolean GET_DETAILS = true;

	/** Table describing ACE users, populated when a user registers with ACE. */
	public static final String USERS = "users_v4";
		/** Field in USERS.  Login ID, a.k.a. username. */
		public static final String USER_ID = "user_id"; 
		/** Field in USERS. */
		public static final String USER_PWDHASH = "password_hash";
		/** Field in USERS.  */
		public static final String USER_ROLE = "role";
		/** Field in USERS.  */
		public static final String USER_FLAGS = "flags";
		/** Field in USERS.  */
		public static final String USER_GIVENNAME = "first_name"; // CLOB
		/** Field in USERS.  */
		public static final String USER_SURNAME = "last_name"; // CLOB
		/** Field in USERS.  */
		public static final String USER_MIDNAME = "middle_name"; // CLOB
		/** Field in USERS.  */
		public static final String USER_STUDENTNUM = "student_num"; // CLOB
		/** Field in USERS.  */
		public static final String USER_EMAIL = "email"; // CLOB
		/** Field in USERS.  */
		public static final String USER_TXT_MSG = "text_msg_number"; // CLOB
		/** Field in USERS.  */
		public static final String USER_SCHOOLID = "institution_id";
		/** Field in USERS.  */
		public static final String USER_ADDRESS = "contact_address"; // CLOB
		/** Field in USERS.  */
		public static final String USER_PHONE = "phone";
		/** Field in USERS.  */
		public static final String USER_REGD = "registration_date";
		/** Field in USERS.  */
		public static final String USER_LASTLOGIN = "last_login_date";
		/** Field in USERS.  */
		public static final String USER_PAYMENT = "payment_transaction_num";
		/** Field in USERS.  */
		public static final String USER_SEC_QS = "security_questions";
		/** Field in USERS.  */
		public static final String USER_SEC_ANS1 = "security_answer1"; // CLOB
		/** Field in USERS.  */
		public static final String USER_SEC_ANS2 = "security_answer2"; // CLOB
		/** Field in USERS.  */
		public static final String USER_SORT = "sortkey";
		/** Length of the USER_SORT field in USERS.  */
		public static final int USER_SORT_LEN = 400;
		/** Parameter for getAllNonstudents().  */
		public static final boolean INSTN_TOO = true;
	/** Table containing coinstructors of courses. */
	public static final String COINSTRUCTORS = "coinstructors_v2";
		/** Field in COINSTRUCTORS.  &rarr; CRS_ID */
		public static final String COINSTR_CRS = "course_id";
		/** Field in COINSTRUCTORS.  &rarr; USER_ID */
		public static final String COINSTR_INSTR = "instructor_id"; 
		/** Field in COINSTRUCTORS.  Unique identifier.  */
		public static final String COINSTR_SERIALNO = "serial_no";
	/** Table containing temporary logins for exams. */
	public static final String EXAM_IDS = "exam_students_v2";
		/** Field in EXAM_IDS.  &rarr; USER_ID */
		public static final String EXAM_STUDENT = "user_id"; 
		/** Field in EXAM_IDS.  &rarr; CRS_ID */
		public static final String EXAM_CRS = "course_id";
		/** Field in EXAM_IDS.  */
		public static final String EXAM_CREATED = "created";
	/** Table containing names of institutions. */
	public static final String INSTITUTIONS = "institutions_v1";
		/** Field in INSTITUTIONS. */
		public static final String INSTN_ID = "id";
		/** Field in INSTITUTIONS. */
		public static final String INSTN_NAME = "name"; // CLOB
		/** Field in INSTITUTIONS.  */
		public static final String INSTN_STUDENTNUMLABEL = "uniq_id_label"; // CLOB
		/** Field in INSTITUTIONS. */
		public static final String INSTN_LANG = "primary_language";
		/** Field in INSTITUTIONS. */
		public static final String INSTN_GRACE = "grace_days";
	/** Generates unique IDs for institutions. */
	public static final String INSTITUTIONS_SEQ = "institutions_seq";

} // UserRWConstants
