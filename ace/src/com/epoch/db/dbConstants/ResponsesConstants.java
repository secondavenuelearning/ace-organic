package com.epoch.db.dbConstants;

/** Tables regarding assignments and responses, and their fields. */
public class ResponsesConstants extends HWRWConstants {

	/** Table to store students' instantiated assignments. */
	public static final String ASSIGNED_QS = "assigned_questions_v4";
		/** Field in ASSIGNED_QS.  &rarr; USER_ID */
		public static final String ASSGND_QS_STUDENT = "student_id";
		/** Field in ASSIGNED_QS.  &rarr; HW_ID */
		public static final String ASSGND_QS_HWID = "hw_id";
		/** Field in ASSIGNED_QS.  */
		public static final String ASSGND_QS_ENTRY = "entry_date";
		/** Field in ASSIGNED_QS.  */
		public static final String ASSGND_QS_QNUM = "serial_no";
		/** Field in ASSIGNED_QS.  &rarr; Q_QID. */
		public static final String ASSGND_QS_QID = "pb_id";
	/** Table for student responses and evaluation results.  */
	public static final String RESPONSES = "responses_v6"; 
		/** Field in RESPONSES.  &rarr; HW_ID */
		public static final String RESP_HWID = "hw_id";
		/** Field in RESPONSES.  &rarr; USER_ID */
		public static final String RESP_STUDENT = "student_id";
		/** Field in RESPONSES.  &rarr; Q_QID */
		public static final String RESP_QID = "pb_id";
		/** Field in RESPONSES. */
		public static final String RESP_TRIES = "tries";
		/** Field in RESPONSES. */
		public static final String RESP_GRADE = "grade";
		/** Field in RESPONSES. */
		public static final String RESP_MODGRADE = "modified_grade";
		/** Field in RESPONSES. */
		public static final String RESP_LASTRESPONSE = "response"; // CLOB
		/** Field in RESPONSES. */
		public static final String RESP_WHEN = "date_time"; 
		/** Field in RESPONSES. */
		public static final String RESP_LATEST = "most_recent"; 
		/** Field in RESPONSES. */
		public static final String RESP_STATUS = "status"; 
		/** Field in RESPONSES. */
		public static final String RESP_IP = "ip_address";
		/** Field in RESPONSES. */
		public static final String RESP_COMMENT = "instructor_comment"; // CLOB
	/** Instantiated R groups of student responses.  */
	public static final String RESP_SUBSTNS = "response_rgroups_v1"; 
		/** Field in RESP_SUBSTNS.  &rarr; HW_ID */
		public static final String RESP_SUBS_HWID = "hw_id";
		/** Field in RESP_SUBSTNS.  &rarr; USER_ID */
		public static final String RESP_SUBS_STUDENT = "student_id";
		/** Field in RESP_SUBSTNS.  &rarr; Q_QID */
		public static final String RESP_SUBS_QID = "pb_id";
		/** Field in RESP_SUBSTNS. */
		public static final String RESP_SUBS_NUM = "r_group_num";
		/** Field in RESP_SUBSTNS. */
		public static final String RESP_SUBS_SUBSTN = "r_group";
	/** Database function for calculating the factor by which to multiply a
	 * student's grade. */
	public static final String GRADE_FACTOR = "grade_factor_v3";
	/** Database function for calculating the modified grade of an instructor's
	 * response to a question. */
	public static final String INSTRUCTOR_GRADE_FACTOR = "instructor_grade_factor_v2";
	/** Database function for recalculating the modified grades of an assignment
	 * after the instructor has changed grading parameters. */
	public static final String RECALC_MOD_GRADES = "recalculate_modified_grades_v2";
	/** Database function for recalculating the modified grades of one student
	 * in an assignment after the instructor has changed the student's extension. */
	public static final String RECALC_1_STUDENT_MOD_GRADES = "recalc_1_student_grades_v2";

} // ResponsesConstants
