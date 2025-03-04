package com.epoch.db;

import com.epoch.utils.Utils;

/** Stores the database table and field names for master-authored 
 * questions and textbooks.  */
public class DBTables extends DBCommon {

	/** Whether tables in this class represent locally authored work. */
	boolean local = false;

	/** Table for master-authored questions.  Value changes in DBLocalTables. */
	String QUESTIONS = "questions_v3";
	/** Table for master-authored figures.  Value changes in DBLocalTables. */
	String FIGURES = "figures_v5";
	/** Table for evaluators in master-authored questions.  Value changes in
	 * DBLocalTables. */
	String EVALUATORS = "evaluators_v4";
	/** Table for master-authored question data.  Value changes in
	 * DBLocalTables. */
	String QUESTIONDATA = "question_data_v4";
	/** Table for master-authored captions and pulldown menu labels in 
	 * complete-the-table and energy diagram questions.  Value changes in
	 * DBLocalTables. */
	String CAPTIONS = "captions_v1";
	/** Table for filenames of master-authored figures that are images.  Value 
	 * changes in DBLocalTables. */
	String IMAGES = "images_v2";
	/** Old (ACE 3.4 and prior) table for master-authored figures that are 
	 * images.  Value changes in DBLocalTables. */
	String OLD_IMAGES = "image_table1";

} // DBTables
