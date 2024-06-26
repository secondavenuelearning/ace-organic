package com.epoch.courseware;

import com.epoch.db.UserRead;
import com.epoch.exceptions.DBException;
import com.epoch.utils.DateUtils;
import com.epoch.utils.Utils;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/** A forum topic. */
public class ForumTopic {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}
	
	/** Unique ID of the topic. */
	private int id;
	/** Unique ID of the course to which this topic belongs. */
	private int courseId;
	/** Login ID of the user who created this topic. */
	final private String creatorId;
	/** Whether the user who created this topic chose to be anonymous. */
	transient private boolean creatorIsAnon;
	/** Creation date of this topic. */
	final private Date dateCreated;
	/** Last time a post in this topic was written or edited. */
	private Date dateLastChanged;
	/** Login ID of the last user to write or edit a post in this topic. */
	private String lastUserId;
	/** Title of this topic. */
	final private String title;
	/** Whether this topic is sticky. */
	final private boolean sticky;
	/** Number of posts in this topic. */
	transient private int numPosts;
	/** ID numbers of assignment and question to which this topic is linked. */
	final private int[] linkedAssgtQ;
	/** Flag to indicate whether a particular user is watching this topic. */
	private boolean watched = false;

	/** Constructor.  Called from forum/addPost.jsp.
	 * @param	crsId	unique ID of the course to which this topic belongs
	 * @param	userId	login ID of creator of this topic
	 * @param	link	ID numbers of the assignment and question to which 
	 * this topic is linked
	 */
	public ForumTopic(int crsId, String userId, int[] link) {
		id = 0;
		courseId = crsId;
		creatorId = userId;
		dateCreated = new Date();
		lastUserId = userId;
		dateLastChanged = new Date();
		title = "";
		sticky = false;
		linkedAssgtQ = link;
	} // ForumTopic(int, String, int[])

	/** Constructor.  Called from forum/savePost.jsp.
	 * @param	crsId	unique ID of the course to which this topic belongs
	 * @param	userId	login ID of creator of this topic
	 * @param	topicTitle	title of this topic
	 * @param	isSticky	whether this topic is sticky
	 * @param	link	ID numbers of the assignment and question to which 
	 * this topic is linked
	 */
	public ForumTopic(int crsId, String userId, String topicTitle,
			boolean isSticky, int[] link) {
		id = 0;
		courseId = crsId;
		creatorId = userId;
		dateCreated = new Date();
		lastUserId = userId;
		dateLastChanged = new Date();
		title = topicTitle;
		sticky = isSticky;
		linkedAssgtQ = link;
	} // ForumTopic(int, String, String, boolean, int[])

	/** Constructor.  Called from ForumRW.getTopics().
	 * @param	topicId	unique ID of this topic
	 * @param	crsId	unique ID of the course to which this topic belongs
	 * @param	userId	login ID of creator of this topic
	 * @param	created	date this topic was created
	 * @param	topicTitle	title of this topic
	 * @param	isSticky	whether this topic is sticky
	 * @param	postsCt	number of posts associated with this topic
	 * @param	link	ID numbers of the assignment and question to which 
	 * this topic is linked
	 */
	public ForumTopic(int topicId, int crsId, String userId, Date created, 
			String topicTitle, boolean isSticky, int[] link, int postsCt) {
		id = topicId;
		courseId = crsId;
		creatorId = userId;
		dateCreated = created;
		lastUserId = userId;
		dateLastChanged = created;
		title = topicTitle;
		sticky = isSticky;
		linkedAssgtQ = link;
		numPosts = postsCt;
	} // ForumTopic(int, int, String, Date, Date, String, boolean, int[], int)

	/** Gets the topic ID number.
	 * @return	the topic ID number
	 */
	public int getId()							{ return id; }
	/** Gets the topic's course ID number.
	 * @return	the topic's course ID number
	 */
	public int getCourseId()					{ return courseId; }
	/** Gets the login ID of the topic's creator.
	 * @return	the login ID of the topic's creator
	 */
	public String getCreatorId()				{ return creatorId; }
	/** Gets the login ID of the last user to write or edit a post in this
	 * topic.
	 * @return	the login ID of the last user to write or edit a post in this 
	 * topic
	 */
	public String getLastUserId()				{ return lastUserId; }
	/** Gets the topic's title.
	 * @return	the topic's title
	 */
	public String getTitle()					{ return title; }
	/** Gets the topic's stickiness.
	 * @return	true if the topic is sticky
	 */
	public boolean isSticky()					{ return sticky; }
	/** Gets whether the creator of this topic chose to be anonymous.
	 * @return	true if the creator of this topic chose to be anonymous
	 */
	public boolean isCreatorAnon()				{ return creatorIsAnon; }
	/** Sets whether the creator of this topic chose to be anonymous.
	 * @param	an	whether the creator of this topic chose to be anonymous
	 */
	public void setCreatorAnon(boolean an)		{ creatorIsAnon = an; }
	/** Gets whether the topic is being watched.
	 * @return	true if the topic is being watched
	 */
	public boolean isWatched()					{ return watched; }
	/** Sets whether the topic is being watched.
	 * @param	wtchd	whether the topic is being watched
	 */
	public void setWatched(boolean wtchd)		{ watched = wtchd; }
	/** Gets the number of posts in the topic.
	 * @return	the number of posts in the topic
	 */
	public int getNumPosts()					{ return numPosts; }
	/** Gets the ID numbers of the assignment and question to which this 
	 * topic is linked.
	 * @return	the assignment and question ID numbers
	 */
	public int[] getLinkedAssgtQ()				{ return linkedAssgtQ; }
	/** Sets the topic ID number.
	 * @param	topicId	the topic ID number
	 */
	public void setId(int topicId)				{ id = topicId; }
	/** Sets the topic's course ID number.
	 * @param	crsId	the topic's course ID number
	 */
	public void setCourseId(int crsId)			{ courseId = crsId; }
	/** Sets the date a post in this topic was last written or edited.
	 * @param date	the date a post in this topic was last written or edited
	 */
	public void setDateLastChanged(Date date)	{ dateLastChanged = date; }
	/** Sets the user who last wrote or edited a post in this topic
	 * @param userId	the user who last wrote or edited a post in this topic
	 */
	public void setLastUserId(String userId)	{ lastUserId = userId; }

	/** Gets a string describing the date this topic was created.
	 * @param	zone	the time zone
	 * @return	string describing the date this topic was created
	 */
	public String getDateCreated(TimeZone zone) {
		return getDateString(dateCreated, zone);
	} // getDateCreated(TimeZone)

	/** Gets a string describing the date this topic was last edited.
	 * @param	zone	the time zone
	 * @return	string describing the date this topic was last edited
	 */
	public String getDateLastChanged(TimeZone zone) {
		return getDateString(dateLastChanged, zone);
	} // getDateLastChanged(TimeZone)

	/** Gets a string describing a date.
	 * @param	date	a date
	 * @param	zone	the time zone
	 * @return	string describing a date
	 */
	public static String getDateString(Date date, TimeZone zone) {
		final Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return DateUtils.getStringDate(cal.getTime(), zone) + ", "
				+ DateUtils.getStringTime(cal.getTime(), zone);
	} // getDateString(Date, TimeZone)

	/** Gets the name of the creator of this topic.
	 * @return	the name of the creator of this topic
	 */
	public String getCreatorName() {
		return getName(creatorId);
	} // getCreatorName()

	/** Gets the name of the last user to write or edit a post in this topic.
	 * @return	the name of the last user to write or edit a post in this topic
	 */
	public String getLastUserName() {
		return getName(lastUserId);
	} // getLastUserName()

	/** Gets the name of a user from the login ID.
	 * @param	userId	the login ID
	 * @return	the name of the user
	 */
	static String getName(String userId) {
		String name = "?";
		try {
			final User user = UserRead.getUser(userId);
			if (user != null) name = user.getName().toString();
		} catch (DBException e) {
			debugPrint("DBException in getName");
		}
		return name;
	} // getName(String)

} // ForumTopic

