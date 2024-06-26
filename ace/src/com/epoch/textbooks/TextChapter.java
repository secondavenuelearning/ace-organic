package com.epoch.textbooks;

import com.epoch.textbooks.textConstants.TextbookConstants;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.List;

/** A chapter in a textbook. */
public class TextChapter implements TextbookConstants {

	private void debugPrint(Object... msg) {
		// Utils.alwaysPrint(msg);
	}

	/** Chapter ID number. */
	private int id = 0; 
	/** Chapter name. */
	private String name = "";
	/** List of content in this chapter. */
	final private List<TextContent> contents = new ArrayList<TextContent>();
	/** List of IDs of content deleted from this chapter. */
	final private List<Integer> deletedContentIds = new ArrayList<Integer>();
	/** Whether contents in this chapter have been added, reordered, or 
	 * deleted. */
	transient private boolean changed = false;

	/** Constructor.  */
	public TextChapter() {
		// empty constructor
	}

	/** Constructor. 
	 * @param	chapName	name of the chapter
	 */
	public TextChapter(String chapName) {
		name = chapName;
	} // TextChapter(String)

	/** Constructor. 
	 * @param	chapId	ID number of the chapter
	 * @param	chapName	name of the chapter
	 */
	public TextChapter(int chapId, String chapName) {
		id = chapId;
		name = chapName;
	} // TextChapter(int, String)

	/** Copy constructor. 
	 * @param	copy	chapter being copied
	 */
	public TextChapter(TextChapter copy) {
		id = copy.getId();
		name = copy.getName();
		for (final TextContent content : copy.getContents()) {
			contents.add(new TextContent(content));
		} // for each chapter
		deletedContentIds.addAll(copy.getDeletedContentIds());
	} // TextChapter(TextChapter)

	/** Gets the ID number of the chapter.
	 * @return	ID number of the chapter
	 */
	public int getId()							{ return id; }
	/** Gets the name of the chapter.
	 * @return	name of the chapter
	 */
	public String getName()						{ return name; }
	/** Gets the chapters of the chapter.
	 * @return	list of chapters of the chapter
	 */
	public List<TextContent> getContents()		{ return contents; }
	/** Gets the number of contents of the chapter.
	 * @return	number of contents of the chapter
	 */
	public int getNumContents()					{ return contents.size(); }
	/** Gets the list of IDs of deleted contents.
	 * @return	list of IDs of deleted contents
	 */
	public List<Integer> getDeletedContentIds()	{ return deletedContentIds; }
	/** Gets whether the content has changed.
	 * @return	true if the content has changed
	 */
	public boolean isChanged()					{ return changed; }
	/** Sets the ID number of the chapter.
	 * @param	chapId	ID number of the chapter
	 */
	public void setId(int chapId) 				{ id = chapId; }

	/** Gets if this chapter contains any Jmol figures.
	 * @return	true if this chapter contains any Jmol figures
	 */
	public boolean containsJmol() {
		boolean containsJmol = false;
		for (final TextContent content : contents) {
			if (content.isJmol()) {
				containsJmol = true;
				break;
			}
		} // for each content
		return containsJmol;
	} // containsJmol()

	/** Gets if this chapter contains any Lewis figures.
	 * @return	true if this chapter contains any Lewis figures
	 */
	public boolean containsLewis() {
		boolean containsLewis = false;
		for (final TextContent content : contents) {
			if (content.isLewis()) {
				containsLewis = true;
				break;
			}
		} // for each content
		return containsLewis;
	} // containsLewis()

	/** Sets the name of the chapter.
	 * @param	chapName	name of the chapter
	 */
	public void setName(String chapName) { 
		changed = changed || (Utils.isEmpty(name) && !Utils.isEmpty(chapName))
				|| !name.equals(chapName);
		name = (chapName == null ? "" : chapName); 
	} // setName(String)

	/** Gets a content of the contentter.
	 * @param	contentNum	1-based content number
	 * @return	a content of the contentter
	 */
	public TextContent getContent(int contentNum) { 
		return (MathUtils.inRange(contentNum, new int[] {1, getNumContents()})
				? contents.get(contentNum - 1) : null);
	} // getContent(int)

	/** Adds a piece of content.  Used by DB classes only.
	 * @param	content	the piece of content
	 */
	public void addContent(TextContent content) {
		contents.add(content);
		changed = true;
	} // addContent(TextContent)

	/** Adds a new piece of content.
	 * @return	the new piece of content
	 */
	public TextContent addNewContent() {
		final TextContent content = new TextContent();
		addNewContent(content);
		changed = true;
		return content;
	} // addNewContent()

	/** Adds a new piece of content.
	 * @param	content	the new piece of content
	 */
	public void addNewContent(TextContent content) {
		contents.add(new TextContent(content));
		changed = true;
	} // addNewContent(TextContent)

	/** Inserts new pieces of content at the indicated position.
	 * @param	newContents	the new pieces of content
	 * @param	posn	the 1-based posn into which to insert them
	 */
	public void insertContents(List<TextContent> newContents, int posn) {
		contents.addAll(posn - 1, newContents);
		changed = true;
	} // insertContents(List<TextContent>, int)

	/** Moves a piece of content.
	 * @param	fromPosn	1-based position of content to move
	 * @param	toPosn	1-based position to which to move content
	 */
	public void moveContent(int fromPosn, int toPosn) {
		final TextContent movedContent = contents.remove(fromPosn - 1);
		contents.add(toPosn - 1, movedContent);
		changed = true;
	} // moveContent(int, int)

	/** Deletes a piece of content.
	 * @param	posn	1-based position of content to delete
	 * @return	the deleted piece of content
	 */
	public TextContent deleteContent(int posn) {
		return deleteContent(posn, !MOVING);
	} // deleteContent(int)

	/** Deletes a piece of content.
	 * @param	posn	1-based position of content to delete
	 * @param	moving	when true, simply moving the contents, not deleting them
	 * @return	the deleted piece of content
	 */
	public TextContent deleteContent(int posn, boolean moving) {
		final TextContent content = contents.remove(posn - 1);
		if (!moving) deletedContentIds.add(Integer.valueOf(content.getId()));
		changed = true;
		return content;
	} // deleteContent(int, boolean)

	/** Deletes a range of pieces of content.
	 * @param	range	1-based inclusive range of positions of content to delete
	 * @return	list of the deleted pieces of content
	 */
	public List<TextContent> deleteContents(int[] range) {
		return deleteContents(range, !MOVING);
	} // deleteContents(int[])

	/** Deletes a range of pieces of content.
	 * @param	range	1-based inclusive range of positions of content to delete
	 * @param	moving	when true, simply moving the contents, not deleting them
	 * @return	list of the deleted pieces of content
	 */
	public List<TextContent> deleteContents(int[] range, boolean moving) {
		final List<TextContent> deletedContents = new ArrayList<TextContent>();
		for (int contentNum = range[0]; contentNum <= range[1]; contentNum++) {
			deletedContents.add(deleteContent(range[0], moving));
		} // for each piece of content to delete
		changed = true;
		return deletedContents;
	} // deleteContents(int[], boolean)

} // TextChapter
