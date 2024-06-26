package com.epoch.session;

import com.epoch.db.QuestionRW;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.InvalidOpException;
import com.epoch.qBank.Question;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.List;

/** Contains the questions that share certain keywords.
 * Used in the assignment assembly tool.  */
public class KeywordSet extends QSet { 

	/** Keywords shared by the questions in the set. */
	transient String keywords = "";

	/** Constructor for instructors who are local authors.
	 * @param	kw	the keywords
	 * @param	instructId	login ID of the instructor
	 * @throws	DBException	if the database can't be read
	 */
	public KeywordSet(String kw, String instructId) throws DBException {
		masterEdit = false;
		instructorId = instructId;
		keywords = kw;
		initialize();
	} // KeywordSet(String, String)

	/** Constructor for instructors who are master authors.
	 * @param	kw	the keywords
	 * @throws	DBException	if the database can't be read
	 */
	public KeywordSet(String kw) throws DBException {
		masterEdit = true;
		keywords = kw;
		initialize();
	} // KeywordSet(String)

	/** Actual initialization routine called by the constructors;
	 * overrides parent class method.
	 * @throws	DBException	if the database can't be read
	 */
	private void initialize() throws DBException {
		setQs = new ArrayList<Question>();
		setQIds = new ArrayList<Integer>();
		if (!Utils.isEmpty(keywords)) {
			// Load the master table reader 
			try {
				setQs = QuestionRW.getQuestionsByKeywords(keywords);
				for (final Question setQ : setQs) {
					setQIds.add(Integer.valueOf(setQ.getQId()));
				}
				if (!masterEdit) { 
					// Load the local reader 
					final List<Question> setLocalQs =
							QuestionRW.getQuestionsByKeywords(keywords, 
								instructorId); 
					initializeSets(setLocalQs);
					debugPrint("KeywordSet.initialize: "
							+ setLocalQs.size() + " local Qs.");
				} // not master edit
			} catch (DBException e) {
				throw new DBException("Keyword search failed, probably "
						+ "because the search was malformatted.");
			}
		} // there are keywords
	} // initialize()
	
	/** Returns the unique ID of the Q with serial number.
	 * Overrides parent class method -- only the exception language is
	 * different.
	 * @param	index	1-based serial number of the question
	 * @return	unique ID number of the question
	 */
	public int getQId(int index) throws InvalidOpException {
		if (setQs.isEmpty())
			throw new InvalidOpException(" Keyword set is empty ");
		if (index <= 0 || index > setQs.size()) 
			throw new InvalidOpException(" Invalid index to getQId() ");
		return setQs.get(index - 1).getQId();
	} // getQId(int)
	
	/** Gets the keywords. 
	 * @return	the keywords
	 */
	public String getKeywords() 	{ return keywords; } 

} // KeywordSet
