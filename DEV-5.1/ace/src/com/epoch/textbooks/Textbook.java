package com.epoch.textbooks;

import com.epoch.courseware.User;
import com.epoch.db.TextbookRW;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.ParameterException;
import com.epoch.textbooks.textConstants.TextbookConstants;
import com.epoch.utils.MathUtils;
import java.util.ArrayList;
import java.util.List;

/** An online textbook. */
public class Textbook implements TextbookConstants {

	private void debugPrint(Object... msg) {
		// Utils.alwaysPrint(msg);
	}

	/** Textbook ID number. */
	private int id = 0; 
	/** Textbook name. */
	private String name = "";
	/** Textbook owner. */
	private String ownerId = MASTER_AUTHOR;
	/** Flags of this book. */
	transient private int flags = 0;
	/** List of chapters in this book. */
	final private List<TextChapter> chapters = new ArrayList<TextChapter>(); 
	/** List of IDs of chapters deleted from this book. */
	final private List<Integer> deletedChapterIds = new ArrayList<Integer>();
	/** Whether chapters in this textbook have been added, reordered, or 
	 * deleted. */
	transient private boolean changed = false;
	/** The author or coauthor of this book who has authoring privileges at this
	 * time. */
	private String lockHolder = MASTER_AUTHOR;

	/** Constructor. 
	 * @param	userId	owner of the textbook
	 * @throws	ParameterException	if userId is null
	 */
	public Textbook(String userId) throws ParameterException {
		if (userId == MASTER_AUTHOR) { // MASTER_AUTHOR happens to be null
			throw new ParameterException("Textbook: cannot have null "
					+ "as a textbook author.");
		} // if userId
		ownerId = userId;
	} // Textbook(String)

	/** Constructor.   Called by DB class.
	 * @param	bookId	ID number of the textbook
	 * @param	bookName	name of the textbook
	 * @param	userId	owner of the textbook
	 * @param	fl	flags
	 * @throws	ParameterException	if userId is null
	 */
	public Textbook(int bookId, String userId, String bookName, int fl) 
			throws ParameterException {
		if (userId == MASTER_AUTHOR) { // MASTER_AUTHOR happens to be null
			throw new ParameterException("Textbook: cannot have null "
					+ "as a textbook author.");
		} // if userId
		id = bookId;
		ownerId = userId;
		name = bookName;
		flags = fl;
	} // Textbook(int, String, String, int)

	/** Copy constructor. 
	 * @param	copy	textbook being copied
	 */
	public Textbook(Textbook copy) {
		id = copy.getId();
		ownerId = copy.getOwnerId();
		name = copy.getName();
		flags = copy.getFlags();
		for (final TextChapter chap : copy.getChapters()) {
			chapters.add(new TextChapter(chap));
		} // for each chapter
		deletedChapterIds.addAll(copy.getDeletedChapterIds());
	} // Textbook(Textbook)

	/** Copy constructor; if userId is not same as ownerId, will set ID to 0 
	 * so it is written as a new textbook if it is saved. 
	 * @param	copy	textbook being copied
	 * @param	userId	owner of the copy of the textbook
	 * @throws	ParameterException	if userId is null
	 */
	public Textbook(Textbook copy, String userId) throws ParameterException {
		if (userId == MASTER_AUTHOR) { // MASTER_AUTHOR happens to be null
			throw new ParameterException("Textbook: cannot have null "
					+ "as a textbook author.");
		} // if userId
		id = (copy.getOwnerId().equals(userId) ? copy.getId() : 0);
		name = copy.getName();
		ownerId = userId;
		for (final TextChapter chap : copy.getChapters()) {
			chapters.add(new TextChapter(chap));
		} // for each chapter
		deletedChapterIds.addAll(copy.getDeletedChapterIds());
	} // Textbook(Textbook, String)

	/** Gets the ID number of the textbook.
	 * @return	ID number of the textbook
	 */
	public int getId()							{ return id; }
	/** Gets the name of the textbook.
	 * @return	name of the textbook
	 */
	public String getName()						{ return name; }
	/** Gets the login ID of the owner of the textbook.
	 * @return	login ID of the owner of the textbook
	 */
	public String getOwnerId()					{ return ownerId; }
	/** Gets the login ID of the author or coauthor who has editing privileges.
	 * @return	login ID of the author or coauthor who has editing privileges
	 */
	public String getLockHolder()				{ return lockHolder; }
	/** Gets the flags of the textbook.
	 * @return	flags of the textbook
	 */
	public int getFlags()						{ return flags; }
	/** Gets whether this book is visible to all instructors.
	 * @return	true if this book is visible to all instructors
	 */
	public boolean isVisibleToAll()				{ return (flags & VISIBLE) != 0; }
	/** Gets the chapters of the textbook.
	 * @return	list of chapters of the textbook
	 */
	public List<TextChapter> getChapters()		{ return chapters; }
	/** Gets the number of chapters of the textbook.
	 * @return	number of chapters of the textbook
	 */
	public int getNumChapters()					{ return chapters.size(); }
	/** Gets the list of IDs of deleted chapters.
	 * @return	list of IDs of deleted chapters
	 */
	public List<Integer> getDeletedChapterIds()	{ return deletedChapterIds; }
	/** Gets whether the content has changed.
	 * @return	true if the content has changed
	 */
	public boolean isChanged()					{ return changed; }
	/** Sets the ID number of the book.
	 * @param	bookId	ID number of the book
	 */
	public void setId(int bookId) 				{ id = bookId; }
	/** Sets the login ID of the author or coauthor who has editing privileges.
	 * @param	lh	login ID of the author or coauthor who has editing privileges
	 */
	public void setLockHolder(String lh)		{ lockHolder = lh; }

	/** Sets the name of the textbook.
	 * @param	bookName	name of the textbook
	 */
	public void setName(String bookName) { 
		changed = changed || !name.equals(bookName);
		name = bookName; 
	} // setName(String)

	/** Gets coauthors of the textbook.  
	 * @return	array of coauthors
	 * @throws	DBException	if there's a problem reading the database
	 */
	public User[] getCoauthors() throws DBException {
		return TextbookRW.getCoauthors(id);
	} // getCoauthors()

	/** Gets names of all authors of the textbook.  
	 * @return	array of all authors' names, first name first
	 * @throws	DBException	if there's a problem reading the database
	 */
	public String[] getAllAuthorNames() throws DBException {
		return TextbookRW.getAllAuthorNames(id);
	} // getAllAuthorNames()

	/** Get all verified instructors at an institution who are not coauthors
	 * of this textbook.
	 * @param	instnId	ID number of the institution
	 * @return	an array of verified instructor Users
	 * @throws	DBException	if there's a problem reading the database
	 */
	public User[] getNoncoauthors(int instnId) throws DBException {
		return TextbookRW.getNoncoauthors(instnId, id);
	} // getNoncoauthors(int)

	/** Adds a coauthor to this textbook.
	 * @param	userId	login ID of the coauthor
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void addCoauthor(String userId) throws DBException {
		TextbookRW.addCoauthor(id, userId);
	} // addCoauthor(int, String)

	/** Removes a coauthor from this textbook.
	 * @param	userId	login ID of the coauthor
	 * @throws	DBException	if there's a problem reading the database
	 */
	public void removeCoauthor(String userId) throws DBException {
		TextbookRW.removeCoauthor(id, userId);
	} // removeCoauthor(String)

	/** Sets the login ID of the owner of the textbook.
	 * @param	userId	login ID of the owner of the textbook
	 */
	public void setOwnerId(String userId) { 
		changed = changed || (ownerId == null && userId != null)
				|| (ownerId != null && !ownerId.equals(userId));
		ownerId = userId; 
	} // setOwnerId(String)

	/** Sets the visibility of the textbook to nonauthors.
	 * @param	visibleToAll visibility of the textbook to nonauthors
	 */
	public void setVisibility(boolean visibleToAll) { 
		changed = changed || visibleToAll != isVisibleToAll();
		if (visibleToAll) flags |= VISIBLE;
		else flags &= ~VISIBLE;
	} // setVisibility(boolean)

	/** Gets a chapter of the textbook.
	 * @param	chapNum	1-based chapter number
	 * @return	a chapter of the textbook, or null if out of range
	 */
	public TextChapter getChapter(int chapNum) { 
		return (MathUtils.inRange(chapNum, new int[] {1, getNumChapters()})
				? chapters.get(chapNum - 1) : null);
	} // getChapter(int)

	/** Adds a chapter.  Used by DB classes only.
	 * @param	chap	the chapter
	 */
	public void addChapter(TextChapter chap) {
		chapters.add(chap);
	} // addChapter(TextChapter)

	/** Adds a new chapter.
	 * @return	the new chapter
	 */
	public TextChapter addNewChapter() {
		return addNewChapter("");
	} // addNewChapter()

	/** Adds a new chapter.
	 * @param	chapName	name of the chapter
	 * @return	the new chapter
	 */
	public TextChapter addNewChapter(String chapName) {
		final TextChapter chap = new TextChapter(chapName);
		addNewChapter(chap);
		return chap;
	} // addNewChapter(String)

	/** Adds a new chapter.
	 * @param	chap	the new chapter
	 */
	public void addNewChapter(TextChapter chap) {
		chapters.add(chap);
		changed = true;
	} // addNewChapter(TextChapter)

	/** Sets the value of a chapter.  Used to cancel editing changes.
	 * @param	chapNum	1-based number of the chapter
	 * @param	chap	the (original) chapter
	 */
	public void setChapter(int chapNum, TextChapter chap) {
		chapters.set(chapNum - 1, chap);
	} // setChapter(int, TextChapter)

	/** Moves a chapter.
	 * @param	fromPosn	1-based position of chapter to move
	 * @param	toPosn	1-based position to which to move chapter
	 */
	public void moveChapter(int fromPosn, int toPosn) {
		final TextChapter movedChap = chapters.remove(fromPosn - 1);
		chapters.add(toPosn - 1, movedChap);
		changed = true;
	} // moveChapter(int, int)

	/** Deletes a chapter.
	 * @param	posn	1-based position of chapter to delete
	 */
	public void deleteChapter(int posn) {
		final TextChapter chapter = chapters.remove(posn - 1);
		deletedChapterIds.add(Integer.valueOf(chapter.getId()));
		changed = true;
	} // deleteChapter(int)

	/** Moves content from one chapter to another or from one position to
	 * another.
	 * @param	startChapNum	1-based number of the chapter from which the
	 * moved contents come
	 * @param	range	1-based range of contents to be moved
	 * @param	targetChapNum	1-based number of the chapter to which the
	 * contents are being moved
	 * @param	posn	1-based position to which the contents are being moved
	 * @param	newChapName	new name of chapter if there is a new chapter
	 */
	public void moveContents(int startChapNum, int[] range, int targetChapNum, 
			int posn, String newChapName) {
		final String SELF = "Textbook.moveContents: ";
		final TextChapter startChapter = getChapter(startChapNum);
		final List<TextContent> toMove = 
				startChapter.deleteContents(range, MOVING);
		if (startChapNum != targetChapNum) {
			final TextChapter targetChapter = (targetChapNum == 0 
					? addNewChapter(newChapName) 
					: getChapter(targetChapNum));
			targetChapter.insertContents(toMove, posn);
		} else {
			int toWhere = posn;
			if (posn > range[1]) {
				toWhere -= (range[1] - range[0] + 1);
			} else if (posn > range[0]) {
				final int numContents = startChapter.getNumContents();
				if (posn > numContents) toWhere = numContents;
			} // if posn
			debugPrint(SELF + "moving within same chapter; moving contents ",
					range, " to posn ", posn, ", possibly modified to ",
					toWhere);
			startChapter.insertContents(toMove, toWhere);
		} // if moving within a chapter
		changed = true;
	} // moveContents(int, int[], int, int)

	/** Saves the textbook. 
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void save() throws DBException {
		TextbookRW.writeBook(this);
	} // save()

} // Textbook
