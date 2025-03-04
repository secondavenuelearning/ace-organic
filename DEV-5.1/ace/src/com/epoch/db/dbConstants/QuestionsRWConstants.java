package com.epoch.db.dbConstants;

/** Tables and fields of tables regarding topics and question sets, 
 * and fields of tables regarding questions.  Names of tables regarding
 * questions are found in DBTables and DBLocalTables.  */
public class QuestionsRWConstants extends ImageConstants {

	/** Table for topics.  */
	public static final String TOPICS = "chapters_v1";
		/** Field in TOPICS.  Unique ID. */
		public static final String TOPIC_ID = "id";
		/** Field in TOPICS. */
		public static final String TOPIC_NAME = "name";
		/** Field in TOPICS. */
		public static final String TOPIC_REMARKS = "remarks";
	/** Generates unique IDs for topics. */
	public static final String TOPICS_SEQ = "chapters_seq";
	/** Table for question sets (subtopics).  */
	public static final String QSETS = "pbsets_v3";
		/** Field in QSETS.  Unique ID. */
		public static final String QSET_ID = "id";
		/** Field in QSETS.  Unique ID. */
		public static final String QSET_NUM = "serial_no";
		/** Field in QSETS. &rarr; TOPIC_ID */
		public static final String QSET_TOPIC_ID = "chap_id";
		/** Field in QSETS. */
		public static final String QSET_NAME = "name";
		/** Field in QSETS.  Not used. */
		public static final String QSET_BOOKAUTHOR = "author"; 
		/** Field in QSETS. */
		public static final String QSET_COMMONQSTATEMENT = "header"; // CLOB
		/** Field in QSETS. &rarr; PHRASE_ID. */
		public static final String QSET_COMMONQSTMT_ID = "header_id";
		/** Field in QSETS. */
		public static final String QSET_REMARKS = "remarks"; // CLOB
		/** Field in QSETS. */
		public static final String QSET_AUTHOR = "user_id";
	/** Generates unique IDs for question sets. */
	public static final String QSETS_SEQ = "pbsets_seq";
	/** Table for locally modified common question statements.  */
	public static final String MODIFIED_HEADERS = "modified_headers_v2";
		/** Field in MODIFIED_HEADERS.  &rarr; USER_ID */
		public static final String MODHEAD_AUTHOR = "user_id";
		/** Field in MODIFIED_HEADERS.  &rarr; QSET_ID */
		public static final String MODHEAD_QSETID = "pbset_id";
		/** Field in MODIFIED_HEADERS. */
		public static final String MODHEAD_COMMONQSTATEMENT = "header"; // CLOB

	// The following fields belong to tables that have both master and local
	// versions.  The table names are defined in DBTables and DBLocalTables.
	/** Field in QUESTIONS.  Unique identifier.  */
	public static final String Q_QID = "id";
	/** Field in QUESTIONS. */
	public static final String Q_NUM = "serial_no";
	/** Field in QUESTIONS. */
	public static final String Q_STATEMENT = "statement"; // CLOB
	/** Field in QUESTIONS. &rarr; PHRASE_ID. */
	public static final String Q_STMT_ID = "statement_id"; 
	/** Field in QUESTIONS. */
	public static final String Q_QSET = "set_id";
	/** Field in QUESTIONS. */
	public static final String Q_BOOK = "book";
	/** Field in QUESTIONS. */
	public static final String Q_BOOKCHAP = "chapter";
	/** Field in QUESTIONS. */
	public static final String Q_REMARKS = "remarks"; // CLOB
	/** Field in QUESTIONS. */
	public static final String Q_TYPE = "q_type";
	/** Field in QUESTIONS. */
	public static final String Q_FLAGS = "q_flags";
	/** Field in QUESTIONS. */
	public static final String Q_KEYWORDS = "keywords"; // CLOB
	/** Field in QUESTIONS. */
	public static final String Q_CREATED = "created";
	/** Field in QUESTIONS. */
	public static final String Q_LAST_MODIFIED = "last_modified";

	/** Field in FIGURES.  Unique identifier.  */
	public static final String FIG_FIGID = "fig_id"; 
	/** Field in FIGURES.  &rarr; Q_QID */
	public static final String FIG_QID = "pb_id";
	/** Field in FIGURES. */
	public static final String FIG_NUM = "serial_no";
	/** Field in FIGURES. */
	public static final String FIG_TYPE = "fig_type";
	/** Field in FIGURES. */
	public static final String FIG_MAIN_DATA = "mol_structure"; // CLOB
	/** Field in FIGURES. */
	public static final String FIG_ADDL_DATA = "rxn_data"; // CLOB

	/** Field in EVALUATORS. &rarr; Q_QID */
	public static final String EVAL_QID = "pb_id";
	/** Field in EVALUATORS.  Serial number of the evaluator.  */
	public static final String EVAL_MAJORID = "major_id"; 
	/** Field in EVALUATORS.  Secondary serial number.  0 for a simple
	 * evaluator or the top level of a complex evaluator,
	 * or &ge; 1 for a subevaluator of a complex evaluator.  */
	public static final String EVAL_MINORID = "minor_id";
	/** Field in EVALUATORS.  How subevaluators are joined.  Has a value only 
	 * for the top level of a complex evaluator.  */
	public static final String EVAL_SUBEXPR = "subexp"; 
	/** Field in EVALUATORS.  A.k.a. match code.  Has a value for a simple
	 * evaluator or for the subevaluators of a complex evaluator. */
	public static final String EVAL_TYPE = "match_type"; 
	/** Field in EVALUATORS. Has a value for a simple evaluator or for the top
	 * level of a complex evaluator (i.e., EVAL_MINOR_ID = 0).  */
	public static final String EVAL_FEEDBACK = "feedback"; // CLOB
	/** Field in EVALUATORS. &rarr; PHRASE_ID. */
	public static final String EVAL_FEEDBACKID = "feedback_id";
	/** Field in EVALUATORS. Has a value for a simple evaluator or for the top
	 * level of a complex evaluator (i.e., EVAL_MINOR_ID = 0).  */
	public static final String EVAL_GRADE = "grade";
	/** Field in EVALUATORS. Has a value for a simple evaluator or for the
	 * subevaluators of a complex evaluator. */
	public static final String EVAL_CODEDDATA = "coded_data"; // CLOB
	/** Field in EVALUATORS. */
	public static final String EVAL_MOLNAME = "mol_name"; // CLOB
	/** Field in EVALUATORS. */
	public static final String EVAL_MOLSTRUCT = "mol_structure"; // CLOB

	/** Field in QUESTIONDATA.  Unique identifier.  */
	public static final String QD_DATUMID = "data_id";
	/** Field in QUESTIONDATA.  &rarr; Q_QID */
	public static final String QD_QID = "question_id";
	/** Field in QUESTIONDATA. */
	public static final String QD_NUM = "serial_no";
	/** Field in QUESTIONDATA. */
	public static final String QD_TYPE = "data_type";
	/** Field in QUESTIONDATA. */
	public static final String QD_DATA = "data"; // CLOB
	/** Field in QUESTIONDATA. */
	public static final String QD_NAME = "name";
	/** Field in QUESTIONDATA. &rarr; PHRASE_ID. */
	public static final String QD_TEXT_ID = "text_id";

	/** Field in CAPTIONS.  &rarr; Q_QID */
	public static final String CAPTS_QID = "question_id";
	/** Field in CAPTIONS.  */
	public static final String CAPTS_TYPE = "type";
	/** Field in CAPTIONS.  */
	public static final String CAPTS_NUM = "serial_no";
	/** Field in CAPTIONS.  */
	public static final String CAPTS_TEXT = "text"; // CLOB

	/** Generates unique IDs for both master- and locally authored questions */
	public static final String QUESTIONS_SEQ = "problem_seq";
	/** Generates unique IDs for both master- and locally authored figures and
	 * images.  */
	public static final String FIGURES_SEQ = "reference_seq";
	/** Generates unique IDs for both master- and locally authored question data. */
	public static final String QUESTIONDATA_SEQ = "question_data_seq";

} // QuestionsRWConstants
