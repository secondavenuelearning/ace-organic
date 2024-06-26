package com.epoch.db.dbConstants;

/** Tables regarding assignments and their fields. */
public class HWRWConstants {

	/** Table describing some assignment properties.  */
	public static final String HWSETS = "hwsets_v5"; 
		/** Field in HWSETS. */
		public static final String HW_ID = "hw_id";
		/** Field in HWSETS.  &rarr; CRS_ID */
		public static final String HW_COURSE = "course_id";
		/** Field in HWSETS.  */
		public static final String HW_NUM = "serial_no";
		/** Field in HWSETS. */
		public static final String HW_NAME = "name"; // CLOB
		/** Field in HWSETS. */
		public static final String HW_REMARKS = "remarks"; // CLOB
		/** Field in HWSETS.  */
		public static final String HW_CREATED = "date_created";
		/** Field in HWSETS.  */
		public static final String HW_DUE = "date_due";
		/** Field in HWSETS.  */
		public static final String HW_DURATION = "duration";
		/** Field in HWSETS. */
		public static final String HW_TRIES = "tries";
		/** Field in HWSETS.  */
		public static final String HW_FLAGS = "flags";
		/** Field in HWSETS.  */
		public static final String HW_MAXEXT = "max_extension_str";
		/** Field in HWSETS.  &rarr; HW_ID */
		public static final String HW_DEPEND = "depends_on";
	/** Generates unique IDs for assignments. */
	public static final String HWSETS_SEQ = "hwsets_seq";
	/** Table describing the questions in an assignment, including those that
	 * may be chosen at random from a group.  */
	public static final String HW_QS = "hwset_qs_v2"; 
		/** Field in HW_QS. */
		public static final String HWQS_HWID = "hw_id";
		/** Field in HW_QS. */
		public static final String HWQS_GRP_NUM = "group_num";
		/** Field in HW_QS. */
		public static final String HWQS_GRP_PICK = "group_pick";
		/** Field in HW_QS. */
		public static final String HWQS_BUNDLE_SIZE = "group_bundle_size";
		/** Field in HW_QS. */
		public static final String HWQS_QNUM = "pb_num_in_group";
		/** Field in HW_QS. */
		public static final String HWQS_QID = "pb_id";
		/** Field in HW_QS. */
		public static final String HWQS_PTS = "points";
		/** Field in HW_QS. */
		public static final String HWQS_PTS_STR = "points_str";
		/** Field in HW_QS. */
		public static final String HWQS_DEPENDS = "depends_on_pb_id";
	/** Table describing the grading parameters of an assignment.  */
	public static final String GRADING_PARAMS = "hwset_grading_params_v1"; 
		/** Field in GRADING_PARAMS. */
		public static final String GRADING_HWID = "hw_id";
		/** Field in GRADING_PARAMS. */
		public static final String GRADING_TYPE = "param_type";
		/** Field in GRADING_PARAMS. */
		public static final String GRADING_MIN = "limit_min";
		/** Field in GRADING_PARAMS. */
		public static final String GRADING_MAX = "limit_max";
		/** Field in GRADING_PARAMS. */
		public static final String GRADING_FACTOR = "factor";
		/** Field in GRADING_PARAMS. */
		public static final String GRADING_MAX_STR = "limit_max_str";
		/** Field in GRADING_PARAMS. */
		public static final String GRADING_FACTOR_STR = "factor_str";
	/** Table describing the extensions of an assignment.  */
	public static final String EXTENSIONS = "hwset_extensions_v1"; 
		/** Field in EXTENSIONS. */
		public static final String EXT_HWID = "hw_id";
		/** Field in EXTENSIONS. */
		public static final String EXT_STUDENT = "student_id";
		/** Field in EXTENSIONS. */
		public static final String EXT_EXTENSION = "extension";
		/** Field in EXTENSIONS. */
		public static final String EXT_EXTENSION_STR = "extension_str";
		/** Field in EXTENSIONS. */
		public static final String EXT_NUM = "serial_no";
		/** Length of varchar2 field in EXTENSIONS. */
		public static final int EXT_EXTENSION_STR_LEN = 10;
	/** Table describing the allowed reaction conditions of an assignment.  */
	public static final String HWRXNCONDNS = "hwset_rxn_condns_v1"; 
		/** Field in HWRXNCONDNS. */
		public static final String HWRXNCONDN_HWID = "hw_id";
		/** Field in HWRXNCONDNS. */
		public static final String HWRXNCONDN_RXNCOND_ID = "rxn_cond_id";

	/** Table for assignment templates for import. */
	public static final String HWSETS_4IMPORT = "hwsets_for_import_v1";
		/** Field in HWSETS_4IMPORT.  Unique ID.  */
		public static final String HWS4IMP_ID = "id";
		/** Field in HWSETS_4IMPORT. */
		public static final String HWS4IMP_NAME = "name"; // CLOB
		/** Field in HWSETS_4IMPORT. */
		public static final String HWS4IMP_XML = "contents"; // CLOB
	/** Sequencer for reaction condition definitions.  */
	public static final String HWSETS_4IMPORT_SEQ = "hwsets_for_import_seq";

} // HWRWConstants
