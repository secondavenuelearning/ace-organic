package com.epoch.session;

import com.epoch.AppConfig;
import com.epoch.courseware.User;
import com.epoch.db.UserRead;
import com.epoch.db.UserWrite;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.StudentEmailExistsException;
import com.epoch.exceptions.StudentNumConstraintException;
import com.epoch.exceptions.UniquenessException;
import com.epoch.session.sessConstants.AnySessionConstants;
import com.epoch.utils.Utils;
import java.io.IOException;

/** A user's work session.  Superclass for instructor, student, admin,
 * and anon sessions.  */
public class UserSession implements AnySessionConstants {

	protected static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}
	
	/** Information about this user.  */
	transient protected User me; 

	/** Constructor.
	 * @param	user1	the user for whom to create this session
	 */
	public UserSession(User user1) {
		me = user1;
	} // UserSession(User)

	/** Required by InstructorSession(int, User).  */
	UserSession() {
		// intentionally empty
	} // UserSession(int, User)

	/** Modify this user's information.
	 * @param	newValues	contains new information about this user
	 * @throws	UniquenessException	when more than one user has this user's
	 * login ID
	 * @throws	StudentNumConstraintException	when another user at the same 
	 * school as this user already has that ID number
	 * @throws	StudentEmailExistsException	when another student at the same 
	 * school as this user already has that email 
	 * @throws	DBException	if the database can't be written to
	 */
	public void setProfile(User newValues) throws DBException, 
			UniquenessException, StudentNumConstraintException,
			StudentEmailExistsException {
		// take only editable fields of user
		me.copyEditables(newValues);
		UserWrite.setUser(me);
	} // setProfile(User)

	/** Modify this user's password.
	 * @throws	DBException	if the database can't be written to
	 */
	public void setPassword() throws DBException {
		UserWrite.setPassword(me);
	} // setPassword()

	/** Dummy method to be overridden by subclass versions. 
	 * @return	0
	 */
	public int getSelectedCourseNum() {
		return 0;
	} // getSelectedCourseNum()

	/** Get this user's information.
	 * @return	this user's information
	 */
	public User getUser() {
		return me;
	} // getUser()

	/** Sends the text message to the listed email addresses of the users with
	 * the given IDs.
	 * @param	userIds	login IDs of users to whom to send messages so they appear
	 * as text messages on the users' phones
	 * @param	msg	the message
	 * @throws	DBException	if the users with the given IDs cannot be read from 
	 * the database
	 */
	public static void sendTextMessagesToIds(String[] userIds, String msg) 
			throws DBException {
		final String SELF = "UserSession.sendTextMessagesToIds: ";
		final User[] users = UserRead.getUsers(userIds);
		final int numUsers = users.length;
		final String[] userTxtMsgIds = new String[numUsers];
		for (int userNum = 0; userNum < numUsers; userNum++) {
			userTxtMsgIds[userNum] = users[userNum].getTextMessageEmail();
			debugPrint(SELF + "got text message email for user ", userNum + 1,
					": ", userTxtMsgIds[userNum]);
		} // for each user
		sendTextMessages(userTxtMsgIds, msg);
	} // sendTextMessagesToIds(String[], String)

	/** Sends the text message to the email address.
	 * @param	email	email address to which to send message so it appears
	 * as a text message on the user's phone
	 * @param	msg	the message, modified to escape ' characters
	 */
	private static void sendTextMessageNow(String email, String msg) {
		final String SELF = "UserSession.sendTextMessageNow: ";
		final ProcessBuilder builder = new ProcessBuilder("/bin/sh", 
				"-c", Utils.toString("echo '", msg, "' | ", 
					AppConfig.muttProgram, ' ', email));
		debugPrint(SELF, builder.command()); // debugging
		try {
			builder.start();
		} catch (IOException e) {
			Utils.alwaysPrint(SELF + "IOException; command failed:\n", 
					"echo ", msg, " | /usr/bin/mutt '", email, '\'');
		} // try
	} // sendTextMessageNow(String, String)

	/** Sends the text message to the email address.
	 * @param	email	email address to which to send message so it appears
	 * as a text message on the user's phone
	 * @param	msg	the message
	 */
	public static void sendTextMessage(String email, String msg) {
		final String SELF = "UserSession.sendTextMessage: ";
		debugPrint(SELF + "sending email to ", email);
		sendTextMessageNow(email, msg.replace("'", "'\"'\"'"));
	} // sendTextMessage(String, String)

	/** Sends the text message to the listed email addresses.
	 * @param	emails	email addresses to which to send message so it appears
	 * as a text message on the users' phones
	 * @param	msg	the message
	 */
	public static void sendTextMessages(String[] emails, String msg) {
		final String SELF = "UserSession.sendTextMessages: ";
		final String modMsg = msg.replace("'", "'\"'\"'");
		for (final String email : emails) {
			debugPrint(SELF + "sending email to ", email);
			sendTextMessageNow(email, modMsg);
		} // for each email
	} // sendTextMessages(String[], String)

} // UserSession
