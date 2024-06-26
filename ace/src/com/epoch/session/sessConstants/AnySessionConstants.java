package com.epoch.session.sessConstants;

import com.epoch.db.dbConstants.CourseRWConstants;

/** Constants for UserSession and subclasses. */
public interface AnySessionConstants {

	// public static final is implied by interface

	/** Parameter for setStudentNum().  */
	boolean STORE = true;
	/** Return value for selectCourse().  */
	int OK = 0;
	/** Return value for selectCourse().  */
	int DISABLED = -1;
	/** Return value for getCourseNumById().  */
	int NOT_FOUND = -1;

	/** Member of examIds.  */
	int ALL = 0;
	/** Member of examIds.  */
	int UNUSED = 1;
	
	/** Parameter for getExamIds().  */
	boolean UNUSED_ONLY = true;
	/** Constant that front end uses with reference to InstructorSession. */
	public boolean INCLUDE_TAS = CourseRWConstants.INCLUDE_TAS;
	/** Whether to remove inactive users. */
	boolean REMOVE = true;

} // AnySessionConstants
