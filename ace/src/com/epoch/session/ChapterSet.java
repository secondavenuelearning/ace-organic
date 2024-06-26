package com.epoch.session;

import com.epoch.db.QuestionRW;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.InvalidOpException;
import com.epoch.qBank.Question;
import java.util.ArrayList;
import java.util.List;

/** Contains the questions that derive from a Pearson textbook chapter. 
 * Used in the assignment assembly tool.  */
public class ChapterSet extends QSet { 

	/** Name of this Pearson textbook. */
	transient String book = null;
	/** Number (as a String) of the chapter in the textbook. */
	transient String chapter = null;

	/** Constructor for instructors who are local authors.
	 * @param	bk	name of the textbook
	 * @param	chap	number of the textbook chapter
	 * @param	instructId	login ID of the instructor
	 * @throws	DBException	if there's a problem reading the database
	 */
	public ChapterSet(String bk, String chap, String instructId) 
			throws DBException {
		masterEdit = false;
		instructorId = instructId;
		book = bk;
		chapter = chap;
		initialize();
	} // ChapterSet(String, String, String)

	/** Constructor for instructors who are master authors.
	 * @param	bk	name of the textbook
	 * @param	chap	number of the textbook chapter
	 * @throws	DBException	if there's a problem reading the database
	 */
	public ChapterSet(String bk, String chap) throws DBException {
		masterEdit = true;
		book = bk;
		chapter = chap;
		initialize();
	} // ChapterSet(String, String, String)

	/** Actual initialization routine called by the constructors;
	 * overrides parent class method.  
	 * @throws	DBException	if there's a problem reading the database
	 */
	private void initialize() throws DBException {
		// Load the master table reader 
		setQs = QuestionRW.getQuestions(book, chapter);
		setQIds = new ArrayList<Integer>();
		for (final Question setQ : setQs) {
			setQIds.add(Integer.valueOf(setQ.getQId()));
		}
		if (!masterEdit) { 
			// Load the local reader 
			final List<Question> setLocalQs = 
					QuestionRW.getQuestions(book, chapter, instructorId); 
			initializeSets(setLocalQs);
			debugPrint("ChapterSet.initialize: "
					+ setLocalQs.size() + " local Qs.");
		} // not master edit
	} // initialize()
	
	/** Returns the unique ID of the Q with serial number.
	 * Overrides parent class method -- only the exception language is
	 * different.
	 * @param	index	1-based serial number of the question
	 * @return	unique ID number of the question
	 * @throws	InvalidOpException	if the question serial number is invalid
	 * the chapter set is empty
	 */
	public int getQId(int index) throws InvalidOpException {
		if (setQs.isEmpty())
			throw new InvalidOpException(" Chapter set is empty ");
		if (index <= 0 || index > setQs.size()) 
			throw new InvalidOpException(" Invalid index to getQId() ");
		return setQs.get(index - 1).getQId();
	} // getQId(int)
	
	/** Gets the chapter number (as a String) of this chapter. 
	 * @return	chapter number
	 */
	public String getChapter() 	{ return chapter; } 

	/** Gets the textbook name. 
	 * @return	the textbook name
	 */
	public String getBook() 	{ return book; } 

} // ChapterSet
