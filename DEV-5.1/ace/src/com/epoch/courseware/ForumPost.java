package com.epoch.courseware;

import com.epoch.courseware.courseConstants.ForumConstants;
import com.epoch.chem.MolString;
import com.epoch.qBank.Question;
import com.epoch.utils.Utils;
import java.util.Date;
import java.util.TimeZone;

/** A forum post. */
public class ForumPost implements ForumConstants {

	private static void debugPrint(Object... msg) {
		Utils.printToLog(msg);
	}
	
	/** Unique ID of the post. */
	private int id;
	/** Unique ID of the topic to which this post belongs. */
	private int topicId;
	/** Login ID of the user who authored this post. */
	final private String authorId;
	/** Creation date of this post. */
	transient private Date dateCreated;
	/** Last time this post was edited. */
	transient private Date dateLastEdited;
	/** Text of this post. */
	private String text;
	/** Figure associated with this post. */
	private String figure;
	/** Type of figure associated with this post. */
	transient private int figType;
	/** Flags associated with this post. */
	private int flags;

	/** Constructor.  Called from forum/addPost.jsp.
	 * @param	topId	unique ID of the topic to which this post belongs
	 * @param	writerId	login ID of the user who created this post
	 */
	public ForumPost(int topId, String writerId) {
		id = 0;
		topicId = topId;
		authorId = writerId;
		text = "";
		figure = "";
		figType = UNKNOWN;
		flags = 0;
	} // ForumPost(int, String)

	/** Constructor.  Called from forum/savePost.jsp.
	 * @param	topId	unique ID of the topic to which this post belongs
	 * @param	writerId	login ID of the user who created this post
	 * @param	postText	text of this post
	 * @param	fig	figure associated with this post
	 * @param	figureType	type of figure (MARVIN, LEWIS, etc.)
	 * @param	flgs	flags associated with this post
	 */
	public ForumPost(int topId, String writerId, String postText, String fig,
			int figureType, int flgs) {
		id = 0;
		topicId = topId;
		authorId = writerId;
		dateCreated = new Date();
		dateLastEdited = new Date();
		text = postText;
		figure = fig;
		figType = figureType;
		flags = flgs;
	} // ForumPost(int, String, String, String, int, int)

	/** Constructor.  Called from ForumRW.getPosts().
	 * @param	postId	unique ID of this post
	 * @param	topId	unique ID of the topic to which this post belongs
	 * @param	writerId	login ID of the user who created this post
	 * @param	created	date this post was first written
	 * @param	edited	date this post was last edited
	 * @param	postText	text of this post
	 * @param	fig	figure associated with this post
	 * @param	figureType	type of figure (MARVIN, LEWIS, etc.)
	 * @param	flgs	flags associated with this post
	 */
	public ForumPost(int postId, int topId, String writerId, Date created, 
			Date edited, String postText, String fig, int figureType, 
			int flgs) {
		id = postId;
		topicId = topId;
		authorId = writerId;
		dateCreated = created;
		dateLastEdited = edited;
		text = postText;
		figure = fig;
		figType = figureType;
		flags = flgs;
	} // ForumPost(int, int, String, Date, Date, String, String, int, int)

	/** Gets the post ID number.
	 * @return	the post ID number
	 */
	public int getId()						{ return id; }
	/** Gets the post's topic ID number.
	 * @return	the post's topic ID number
	 */
	public int getTopicId()					{ return topicId; }
	/** Gets the login ID of the post's writer.
	 * @return	the login ID of the post's writer
	 */
	public String getAuthorId()				{ return authorId; }
	/** Gets the post's text.
	 * @return	the post's text
	 */
	public String getText()					{ return text; }
	/** Gets the figure associated with this post.
	 * @return	the figure associated with this post
	 */
	public String getFigure()				{ return figure; }
	/** Gets the type of figure associated with this post.
	 * @return	the type of figure associated with this post
	 */
	public int getFigureType()				{ return figType; }
	/** Gets if the figure is a MarvinSketch figure.
	 * @return	true if the figure is a MarvinSketch figure
	 */
	public boolean figureIsMarvin()			{ return figType == MOLECULE; }
	/** Gets if the figure is a MarvinSketch figure plus reaction condition IDs.
	 * @return	true if the figure is a MarvinSketch figure plus reaction
	 * condition IDs
	 */
	public boolean figureIsSynthesis()		{ return figType == SYNTHESIS; }
	/** Gets if the figure is a LewisSketch figure.
	 * @return	true if the figure is a LewisSketch figure
	 */
	public boolean figureIsLewis()			{ return figType == LEWIS; }
	/** Gets if the figure is an image.
	 * @return	true if the figure is an image
	 */
	public boolean figureIsImage()			{ return figType == IMAGE; }
	/** Gets the post flags.
	 * @return	the post flags
	 */
	public int getFlags()					{ return flags; }
	/** Sets the post ID number.
	 * @param	postId	the post ID number
	 */
	public void setId(int postId)			{ id = postId; }
	/** Sets the post's topic ID number.
	 * @param	topId	the post's topic ID number
	 */
	public void setTopicId(int topId)		{ topicId = topId; }
	/** Sets the post's text.
	 * @param	postText	the post's text
	 */
	public void setText(String postText)	{ text = postText; }
	/** Sets the figure associated with this post.
	 * @param	fig	the figure associated with this post
	 */
	public void setFigure(String fig)		{ figure = fig; }
	/** Sets the type of figure associated with this post.
	 * @param	fType	the type of figure associated with this post
	 */
	public void setFigureType(int fType)	{ figType = fType; }
	/** Sets the post flags.
	 * @param	flgs	the post flags
	 */
	public void setFlags(int flgs)			{ flags = flgs; }
	/** Resets the post's date last edited to now.  */
	public void resetDateLastEdited()		{ dateLastEdited = new Date(); }
	/** Gets whether this post is anonymous.
	 * @return	true if this post is anonymous
	 */
	public boolean isAnon()					{ return isAnon(flags); }
	/** Gets whether a post is anonymous.
	 * @param	f	a post's flags
	 * @return	true if the post is anonymous
	 */
	public static boolean isAnon(int f)		{ return (f & ANON) != 0; }

	/** Gets if there's a figure for this post.
	 * @return	true if there's a figure for this post
	 */
	public boolean hasFigure() {
		return !Utils.isEmpty(figure)
				&& ((Utils.among(figType, MOLECULE, LEWIS, SYNTHESIS) 
						&& figure.indexOf("atomArray") >= 0)
					|| (figType == IMAGE
						&& figure.indexOf(POST_FILENAME) >= 0));
	} // hasFigure()

	/** Gets a Javascript expression that will generate this poat molecule's
	 * image in a Web page.
	 * @param	pathToRoot	path to application root
	 * @param	prefersPNG	prefers PNG over SVG graphics
	 * @param	postId	unique identifier of the post
	 * @return	the HTML and Javascript expression
	 */
	public String getImage(String pathToRoot, boolean prefersPNG,
			int postId) {
		return getImage(pathToRoot, prefersPNG, String.valueOf(postId));
	} // getImage(String, boolean, int)

	/** Gets a Javascript expression that will generate this poat molecule's
	 * image in a Web page.
	 * @param	pathToRoot	path to application root
	 * @param	prefersPNG	prefers PNG over SVG graphics
	 * @param	postIdStr	unique identifier of the post
	 * @return	the HTML and Javascript expression
	 */
	public String getImage(String pathToRoot, boolean prefersPNG,
			String postIdStr) {
		final long flags = (figureIsMarvin() 
				? (Question.SHOWMAPPING | Question.SHOWLONEPAIRS)
				: figureIsLewis() 
				? (Question.SHOWNOH | Question.SHOWALLC)
				: 0L); 
		return MolString.getImage(pathToRoot, figure, flags, postIdStr, 
				prefersPNG);
	} // getImage(String, boolean, String)

	/** Gets a string describing the date this post was created.
	 * @param	zone	the time zone
	 * @return	string describing the date this post was created
	 */
	public String getDateCreated(TimeZone zone) {
		return ForumTopic.getDateString(dateCreated, zone);
	} // getDateCreated(TimeZone)

	/** Gets a string describing the date this post was last edited.
	 * @param	zone	the time zone
	 * @return	string describing the date this post was last edited
	 */
	public String getDateLastEdited(TimeZone zone) {
		return ForumTopic.getDateString(dateLastEdited, zone);
	} // getDateLastEdited(TimeZone)

	/** Gets the name of the author of this post.
	 * @return	the name of the author of this post
	 */
	public String getAuthorName() {
		return ForumTopic.getName(authorId);
	} // getAuthorName()

} // ForumPost

