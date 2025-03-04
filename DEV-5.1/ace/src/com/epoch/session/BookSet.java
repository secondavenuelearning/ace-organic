package com.epoch.session;

import com.epoch.db.QSetRW;
import com.epoch.exceptions.DBException;
import com.epoch.qBank.TextChapters;
import com.epoch.utils.Utils;
import java.util.Collections;
import java.util.List;

/** Describes the chapters of a Pearson textbook. Used in the assignment
 * assembly process.  */
public class BookSet {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Name of this Pearson textbook. */
	transient String book;
	/** List of numbers (as Strings) corresponding to the textbook chapters
	 * for which there are Qs in the database. */
	transient List<String> chapters;
	/** Login ID of the instructor. */
	transient String instructorId;
	/** Whether the instructor is a master author. */
	transient boolean masterEdit;
	
	/** Constructor.
	 * @param	bk	name of the textbook
	 * @throws	DBException	if the textbook can't be populated with chapters,
	 * and chapters with questions
	 */
	public BookSet(String bk) throws DBException {
		masterEdit = true;
		book = bk;
		initialize();
	}
	
	/** Constructor.
	 * @param	bk	name of the textbook
	 * @param	instructorId1	login ID of the instructor
	 * @throws	DBException	if the textbook can't be populated with chapters,
	 * and chapters with questions
	 */
	public BookSet(String bk, String instructorId1) throws DBException {
		masterEdit = false;
		book = bk;
		instructorId = instructorId1;	
		initialize();
	}
	
	/** Gets the name of the Pearson textbook.
	 * @return	name of the Pearson textbook
	 */
	public String getBook() 			{ return book; }
	/** Gets the login ID of the instructor.
	 * @return	ID of the instructor
	 */
	public String getInstructorId() 	{ return instructorId; }
	/** Gets whether the instructor is a master author.
	 * @return	true if the instructor is a master author
	 */
	public boolean isMasterEdit() 		{ return masterEdit; }
	
	/** Finds numbers of textbook chapters for which there are Qs in the
	 * database, orders them.
	 * @throws	DBException	if the textbook can't be populated with chapters,
	 * and chapters with questions
	 */
	private void initialize() throws DBException {
		final String SELF = "BookSet.initialize: ";
		chapters = (masterEdit ? QSetRW.getBookChapters(book)
				: QSetRW.getBookChapters(book, instructorId));
		final int numChaps = chapters.size();
		debugPrint(SELF + "got ", numChaps, " chapters for ", book);
		for (int chapNum = 0; chapNum < numChaps; chapNum++) {
			final String chapter = chapters.get(chapNum);
			try {
				if (Integer.parseInt(chapter) < 10)
					chapters.set(chapNum, "0" + chapter);
			} catch (NumberFormatException e) {
				if (!"Other".equals(book))
					Utils.alwaysPrint(SELF + "chapter ", chapNum, " has "
							+ "name that is not just a number: ", chapter);
			}
		} // for each chapter
		Collections.sort(chapters);
		for (int chapNum = 0; chapNum < numChaps; chapNum++) {
			final String chapter = chapters.get(chapNum);
			if ("0".equals(chapter.substring(0, 1)))
				chapters.set(chapNum, chapter.substring(1));
		} // for each chapter
	} // initialize()

	/** Returns numbers (as Strings) corresponding to the textbook chapters
	 * for which there are Qs in the database.
	 * @param	nowBook	name of the Pearson book for which to get values
	 * @return	numbers of the textbook chapters
	 * @throws	DBException	if the textbook can't be populated with chapters,
	 * and chapters with questions
	 */
	public String[] getChapters(String nowBook) throws DBException {
		if (!"book".equals(nowBook)) {
			debugPrint("BookSet.java: book has changed; reinitializing");
			book = nowBook;
			initialize();
		}
		return getChapters();
	}

	/** Returns numbers (as Strings) corresponding to the textbook chapters
	 * for which there are Qs in the database.
	 * @return	numbers of the textbook chapters
	 */
	public String[] getChapters() {
		return (chapters.toArray(new String[chapters.size()]));
	}

	/** Returns names of the chapters of this textbook in order.
	 * @param	nowBook	name of the Pearson book for which to get values
	 * @return	names of the textbook chapters
	 */
	public String[] getChapterNames(String nowBook) {
		return TextChapters.getTextChapterNames(nowBook);
	} // getChapterNames(String)

} // BookSet
