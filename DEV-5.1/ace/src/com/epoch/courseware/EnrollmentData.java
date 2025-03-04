package com.epoch.courseware;

import com.epoch.exceptions.FileFormatException;
import com.epoch.utils.DateUtils;
import com.epoch.utils.Utils;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

/** Information about a student in a course. */
public class EnrollmentData {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Name in English. */
	private String name = "";
	/** Email address. */
	private String email = "";
	/** Email address for receiving text messages by phone. */
	private String textMessageEmail = "";
	/** Login ID. */
	private String userId = "";
	/** Institution. */
	private Institution institution = new Institution(); 
	/** Institutional ID number. */
	private String studentNum = ""; 
	/** Whether this student has been anointed a TA. */
	transient private boolean isTA = false;
	/** User's registration date. */
	transient private Date registrationDate = null;

	/** Constructor. */
	public EnrollmentData() { 
		// empty
	}

	/** Constructor. 
	 * @param	user	a user
	 */
	public EnrollmentData(User user) { 
		userId = user.getUserId();
		studentNum = user.getStudentNum();
		email = user.getEmail();
		textMessageEmail = user.getTextMessageEmail();
		institution = user.getInstitution();
		name = user.getName().toString();
		isTA = user.getRole() == User.TA;
	} // EnrollmentData(User)

	/** Gets the user's login ID.
	 * @return	the user's login ID
	 */
	public String getUserId()						{ return userId; }
	/** Sets the user's login ID.
	 * @param	id	the user's login ID
	 */
	public void setUserId(String id)				{ userId = id; }
	/** Gets the user's name.
	 * @return	the user's name
	 */
	public String getName()							{ return name; }
	/** Sets the user's name.
	 * @param	nm	the user's name
	 */
	public void setName(String nm)					{ name = nm; }
	/** Gets the user's student ID number.
	 * @return	the user's student ID number
	 */
	public String getStudentNum()					{ return studentNum; }
	/** Sets the user's student ID number.
	 * @param	num	the user's student ID number
	 */
	public void setStudentNum(String num)			{ studentNum = num; }
	/** Gets the user's email address.
	 * @return	the user's email address
	 */
	public String getEmail()						{ return email; }
	/** Sets the user's email address.
	 * @param	eml	the user's email address
	 */
	public void setEmail(String eml)				{ email = eml; }
	/** Gets the user's email address for receiving text messages by phone.
	 * @return	the user's email address for receiving text messages by phone
	 */
	public String getTextMessageEmail()				{ return textMessageEmail; }
	/** Sets the user's email address for receiving text messages by phone.
	 * @param	tme	the user's email address for receiving text messages by phone
	 */
	public void setTextMessageEmail(String tme)		{ textMessageEmail = tme; }
	/** Gets the user's institution.
	 * @return	the user's institution
	 */
	public Institution getInstitution()				{ return institution; }
	/** Gets the ID number of the user's institution.
	 * @return	the ID number of the user's institution
	 */
	public int getInstitutionId()					{ return institution.getId(); }
	/** Gets the name of the user's institution.
	 * @return	the name of the user's institution
	 */
	public String getInstitutionName()				{ return institution.getName(); }
	/** Gets the name of the student ID number at the user's institution.
	 * @return	the name of the student ID number at the user's institution
	 */
	public String getInstitutionStudentNumLabel()	{ return institution.getStudentNumLabel(); }
	/** Sets the user's institution.
	 * @param	inst	the user's institution
	 */
	public void setInstitution(Institution inst)	{ institution = inst; }
	/** Sets the user's institution.
	 * @param	instId	ID number of the user's institution
	 */
	public void setInstitution(int instId)			{ institution = new Institution(instId); }
	/** Gets if the user is a TA.
	 * @return	true if the user is a TA
	 */
	public boolean isTA()							{ return isTA; }
	/** Sets if the user is a TA
	 * @param	is	whether the user is a TA
	 */
	public void setTA(boolean is)					{ isTA = is; }
	/** Gets if the user is registered.
	 * @return	true if the user is registered
	 */
	public boolean isRegistered()					{ return registrationDate != null; }
	/** Sets the user's registration date.  Called by EnrollmentRW and UserRead
	 * only.
	 * @param	date	date of registration
	 */
	public void setRegDate(Date date)				{ registrationDate = date; }

	/** Determines whether this student is a temporary exam-student.
	 * @return	true if this student is a temporary exam-student
	 */
	public boolean isUnusedExamStudent() { 
		return User.isExamStudent(userId) 
				&& name.startsWith(User.RANDOM_SURNAME); 
	} // isUnusedExamStudent()

	/** Gets the registration date as a string.
	 * @param	timeZone	a time zone
	 * @return	date this student registered (MMM d, yyyy, h:mm aa)
	 */
	public String getRegDateStr(TimeZone timeZone) { 
		return (registrationDate == null ? null 
				: DateUtils.getStringNoTimeZone(registrationDate, timeZone)); 
	} // getRegDateStr(TimeZone)

	/** Converts a file with student ID numbers and names into enrollment data.
	 * @param	tablFile	name of the file containing the enrollment data
	 * @return	an array of EnrollmentData
	 * @throws	FileFormatException	if the file can't be read
	 */
	public static EnrollmentData[] tablFileToList(String tablFile)  
			throws FileFormatException  {
		final String SELF = "EnrollmentData.tablFileToList: ";
		final ArrayList<EnrollmentData> list = new ArrayList<EnrollmentData>();
		try {
			debugPrint(SELF + "uploading data from ", tablFile);
			final BufferedReader rdr = new BufferedReader(
				new InputStreamReader(
					new FileInputStream(tablFile), StandardCharsets.UTF_8
				)
			);
			String line = null;
			while ((line = rdr.readLine()) != null) {
				// some times, there are empty last lines
				if (line.trim().length() == 0) continue;
				debugPrint(SELF, line);
				final String[] tkns = line.split("\t");
				debugPrint(SELF, tkns.length, " token(s) after splitting at tabs.");
				if (tkns.length < 2) continue;
				final EnrollmentData edata = new EnrollmentData();
				edata.studentNum = tkns[0];
				edata.name = Utils.unicodeToCERs(tkns[1]);
				debugPrint(SELF + "student ID number = ", edata.studentNum,
						", name = ", edata.name);
				list.add(edata);
			} // while there are more lines
			rdr.close();
		} catch (IOException e) {
			throw new FileFormatException(" Error in reading file; " 
				+ e.getMessage());
		} // try
		return (list.toArray(new EnrollmentData[list.size()]));
	} // tablFileToList(String)

} // EnrollmentData
