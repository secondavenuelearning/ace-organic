package com.epoch.courseware;

import com.epoch.utils.Utils;

/** A user's institution. */
public class Institution {

	/** Institution ID number. */
	private int id = 0; 
	/** Institution name. */
	private String name = "";
	/** Primary language of instruction. */
	transient private String primaryLanguage = null;
	/** Institution's name for student ID numbers. */
	private String studentNumLabel = "student ID number"; 
	/** Number of days students at this institution are exempt from paying to 
	 * use ACE. */
	private int graceDays = 0;

	/** Constructor.  */
	public Institution() {
		// empty
	}

	/** Constructor. 
	 * @param	instnId	ID number of the institution
	 */
	public Institution(int instnId) {
		id = instnId;
	} // Institution(int)

	/** Constructor. 
	 * @param	instnId	ID number of the institution
	 * @param	instnNumLabel	institution's name for the student ID number
	 */
	public Institution(int instnId, String instnNumLabel) {
		id = instnId;
		studentNumLabel = instnNumLabel;
	} // Institution(int, String)

	/** Constructor. 
	 * @param	instnName	name of the institution
	 * @param	instnNumLabel	institution's name for the student ID number
	 */
	public Institution(String instnName, String instnNumLabel) {
		name = instnName;
		studentNumLabel = instnNumLabel;
	} // Institution(String, String)

	/** Constructor. 
	 * @param	instnId	ID number of the institution
	 * @param	instnName	name of the institution
	 * @param	instnNumLabel	institution's name for the student ID number
	 */
	public Institution(int instnId, String instnName, String instnNumLabel) {
		id = instnId;
		name = instnName;
		studentNumLabel = instnNumLabel;
	} // Institution(int, String, String)

	/** Constructor. 
	 * @param	instnName	name of the institution
	 * @param	instnPrimaryLang	primary language of instruction
	 * @param	instnNumLabel	institution's name for the student ID number
	 */
	public Institution(String instnName, String instnPrimaryLang, 
			String instnNumLabel) {
		name = instnName;
		primaryLanguage = instnPrimaryLang;
		studentNumLabel = instnNumLabel;
	} // Institution(String, String, String)

	/** Constructor. 
	 * @param	instnId	ID number of the institution
	 * @param	instnName	name of the institution
	 * @param	instnPrimaryLang	primary language of instruction
	 * @param	instnNumLabel	institution's name for the student ID number
	 * @param	instnGraceDays	number of days before students here have to pay 
	 * for ACE
	 */
	public Institution(int instnId, String instnName, String instnPrimaryLang,
			String instnNumLabel, int instnGraceDays) {
		id = instnId;
		name = instnName;
		primaryLanguage = instnPrimaryLang;
		studentNumLabel = instnNumLabel;
		graceDays = instnGraceDays;
	} // Institution(int, String, String, String, int)

	/** Copy constructor. 
	 * @param	copy	institution being copied
	 */
	public Institution(Institution copy) {
		id = copy.getId();
		name = copy.getName();
		primaryLanguage = copy.getPrimaryLanguage();
		studentNumLabel = copy.getStudentNumLabel();
	} // Institution(Institution)

	/** Gets the ID number of the institution.
	 * @return	ID number of the institution
	 */
	public int getId()							{ return id; }
	/** Sets the ID number of the institution.
	 * @param	instnId	ID number of the institution
	 */
	public void setId(int instnId) 				{ id = instnId; }
	/** Gets the name of the institution.
	 * @return	name of the institution
	 */
	public String getName()						{ return name; }
	/** Sets the name of the institution.
	 * @param	instnName	name of the institution
	 */
	public void setName(String instnName)		{ name = instnName; }
	/** Gets the primary language of instruction.
	 * @return	primary language of instruction
	 */
	public String getPrimaryLanguage()			{ return primaryLanguage; }
	/** Gets the name of the student ID number.
	 * @return	name of the student ID number.
	 */
	public String getStudentNumLabel() 			{ return studentNumLabel; }
	/** Sets the name of the student ID number.
	 * @param	snl	name of the student ID number.
	 */
	public void setStudentNumLabel(String snl)	{ studentNumLabel = snl; }
	/** Gets the number of days students at the institution may avoid paying 
	 * for ACE.
	 * @return	number of days grace period
	 */
	public int getGraceDays()					{ return graceDays; }
	/** Sets the number of days students at the institution may avoid paying 
	 * for ACE.
	 * @param	instnGD	number of days grace period
	 */
	public void setGraceDays(int instnGD)		{ graceDays = instnGD; }
	/** Gets whether students at the institution may avoid paying for ACE
	 * indefinitely.
	 * @return	true if students at the institution may avoid paying for ACE 
	 * indefinitely
	 */
	public boolean isExempt()					{ return graceDays < 0; }
	/** Sets that students at the institution may avoid paying for ACE
	 * indefinitely.
	 */
	public void setExempt()						{ setGraceDays(-1); }

} // Institution

