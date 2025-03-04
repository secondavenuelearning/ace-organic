package com.epoch.textbooks;

import com.epoch.AppConfig;
import com.epoch.chem.MolString;
import com.epoch.db.DBLocalTables;
import com.epoch.db.TextbookRW;
import com.epoch.exceptions.DBException;
import com.epoch.qBank.Question;
import com.epoch.textbooks.textConstants.ContentConstants;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.Arrays;

/** A piece of content in a chapter. */
public class TextContent implements ContentConstants {

	private void debugPrint(Object... msg) {
		// Utils.alwaysPrint(msg);
	}

	/** Content ID number. */
	private int id = 0; 
	/** Type of content. */
	private int contentType = 0; 
	/** The content. */
	private String content = ""; 
	/** The caption. */
	transient private String caption = ""; 
	/** Extra data, e.g., MarvinView display parameters. */
	transient private String extraData = ""; 
	/** Whether this content has been changed. */
	transient private boolean changed = false;

	/** Constructor.  */
	public TextContent() {
		// empty
	}

	/** Constructor. 
	 * @param	typeStr	DB value for type of content
	 * @param	data	the content
	 * @param	cap	a caption
	 * @param	extra	extra data, e.g., MarvinView display parameters
	 */
	public TextContent(String typeStr, String data, String cap, 
			String extra) {
		setContentType(typeStr);
		content = data;
		caption = cap;
		extraData = extra;
	} // TextContent(String, String, String, String)

	/** Constructor. 
	 * @param	contentId	ID number of the content
	 * @param	typeStr	DB value for type of content
	 * @param	data	the content
	 * @param	cap	a caption
	 * @param	extra	extra data, e.g., MarvinView display parameters
	 */
	public TextContent(int contentId, String typeStr, String data, 
			String cap, String extra) {
		id = contentId;
		setContentType(typeStr);
		content = data;
		caption = cap;
		extraData = extra;
	} // TextContent(int, String, String, String, String)

	/** Copy constructor. 
	 * @param	copy	content being copied
	 */
	public TextContent(TextContent copy) {
		contentType = copy.getContentType();
		content = copy.getContent();
		caption = copy.getCaption();
		extraData = copy.getExtraData();
	} // TextContent(TextContent)

	/** Gets the ID number of the content.
	 * @return	ID number of the content
	 */
	public int getId()						{ return id; }
	/** Gets the type of the content.
	 * @return	type of the content
	 */
	public int getContentType()				{ return contentType; }
	/** Gets the type of the content for DB storage.
	 * @return	type of the content
	 */
	public String getDbContentType()		{ return DB_VALUES[contentType]; }
	/** Gets whether the content is text.
	 * @return	true if the content is text
	 */
	public boolean isText()					{ return contentType == TEXT; }
	/** Gets whether the content is from MarvinSketch.
	 * @return	true if the content is from MarvinSketch
	 */
	public boolean isMarvin()				{ return contentType == MARVIN; }
	/** Gets whether the content is from LewisSketch.
	 * @return	true if the content is from LewisSketch
	 */
	public boolean isLewis()				{ return contentType == LEWIS; }
	/** Gets whether the content is an image.
	 * @return	true if the content is an image
	 */
	public boolean isImage()				{ return contentType == IMAGE; }
	/** Gets whether the content is an image URL.
	 * @return	true if the content is an image URL
	 */
	public boolean isImageURL()				{ return contentType == IMAGE_URL; }
	/** Gets whether the content is an ACE question.
	 * @return	true if the content is an ACE question
	 */
	public boolean isACEQuestion()			{ return contentType == ACE_Q; }
	/** Gets whether the content is a Jmol figure.
	 * @return	true if the content is a Jmol figure
	 */
	public boolean isJmol()					{ return contentType == JMOL; }
	/** Gets whether the content is a movie.
	 * @return	true if the content is a movie
	 */
	public boolean isMovie()				{ return contentType == MOVIE; }
	/** Gets the content.
	 * @return	the content
	 */
	public String getContent()				{ return content; }
	/** Gets the caption.
	 * @return	the caption
	 */
	public String getCaption()				{ return caption; }
	/** Gets the extra data.
	 * @return	the extra data
	 */
	public String getExtraData()			{ return extraData; }
	/** Gets whether the content has changed.
	 * @return	true if the content has changed
	 */
	public boolean isChanged()				{ return changed; }
	/** Sets the ID number of the content.
	 * @param	contentId	ID number of the content
	 */
	public void setId(int contentId) 		{ id = contentId; }
	/** Sets the type of the content.
	 * @param	type	type of content
	 */
	public final void setContentType(int type)	{ contentType = type; }
	/** Sets the type of the content.
	 * @param	type	type of content in DB format
	 */
	public final void setContentType(String type)	{ contentType = Utils.indexOf(DB_VALUES, type); }

	/** Sets the content.  Called by DB only!
	 * @param	data	the content
	 */
	public void setContent(String data) { 
		content = data; 
	} // setContent(String)

	/** Sets the content and caption.
	 * @param	data	the content
	 * @param	cap	the caption
	 */
	public void setContent(String data, String cap) { 
		setContent(data, cap, "");
	} // setContent(String, String)

	/** Sets content, caption, and any extra data, e.g., MarvinView display 
	 * parameters. 
	 * @param	data	the content
	 * @param	cap	the caption
	 * @param	extra	the extra data
	 */
	public void setContent(String data, String cap, String extra) { 
		changed = true;
		content = data; 
		caption = cap;
		extraData = extra; 
	} // setContent(String, String, String)

	/** Gets a Javascript expression that will generate this figure's image
	 * in a Web page.
	 * @param	pathToRoot	path to application root
	 * @param	prefersPNG	prefers PNG over SVG graphics
	 * @return	the image's HTML
	 */
	public String getImage(String pathToRoot, boolean prefersPNG) {
		return getImage(pathToRoot, prefersPNG, "1");
	} // getImage(String, boolean)

	/** Gets a Javascript expression that will generate this figure's image
	 * in a Web page.
	 * @param	pathToRoot	path to application root
	 * @param	prefersPNG	prefers PNG over SVG graphics
	 * @param	figNum	serial number of the figure
	 * @return	the image's HTML
	 */
	public String getImage(String pathToRoot, boolean prefersPNG, int figNum) {
		return getImage(pathToRoot, prefersPNG, String.valueOf(figNum));
	} // getImage(String, boolean, int)

	/** Gets a Javascript expression that will generate this figure's image
	 * in a Web page.
	 * @param	pathToRoot	path to application root
	 * @param	prefersPNG	prefers PNG over SVG graphics
	 * @param	figIdStr	unique identifier of the figure on the Web page
	 * @return	the image's HTML
	 */
	public String getImage(String pathToRoot, boolean prefersPNG, 
			String figIdStr) {
		String imageStr = null;
		if (isMarvin()) {
			final String molStr = getFormattedContent();
			final long flags = (MathUtils.parseLong(extraData) 
					| Question.BADVALENCEINVISIBLE);
			imageStr = MolString.getImage(pathToRoot, molStr, flags, 
					figIdStr, prefersPNG);
		} // if is Marvin
		return imageStr;
	} // getImage(String, boolean, String)

	/** Makes a filename that contains the image, movie, or molecule image.
	 * @return	the new filename, which is also stored as the content
	 * in case of images or movies (but not for Marvin or Lewis images)
	 * @throws	DBException	if there's a problem reading the database
	 */
	public String makeImageFileName() throws DBException {
		final int imgId = MathUtils.parseInt(getContent());
		return makeImageFileName(TextbookRW.getImageExtension(imgId));
	} // makeImageFileName()

	/** Makes a filename that contains the image, movie, or molecule image.
	 * @param	extension	extension to add to the filename; empty String if
	 * should not add one
	 * @return	the new filename, which is also stored as the content
	 * in case of images or movies (but not for Marvin or Lewis images)
	 */
	public String makeImageFileName(String extension) {
		final boolean isImageOrMovie = isImage() || isMovie();
		if (!isImageOrMovie) return null;
		final String relFileName = 
				getRelImageFileName(extension, isImageOrMovie);
		setContent(relFileName);
		return relFileName;
	} // makeImageFileName(String)

	/** Makes a filename for an image, movie, or molecule image.
	 * @param	extension	extension to add to the filename; empty String if
	 * should not add one
	 * @param	isImageOrMovie	true if the content is an image or a movie
	 * @return	the new filename
	 */
	public String getRelImageFileName(String extension, 
			boolean isImageOrMovie) {
		final String SELF = "TextContent.getRelImageFileName: ";
		final StringBuilder relFileNameBld = Utils.getBuilder(
				DBLocalTables.LOCAL_PREFIX, AppConfig.relFiguresDir);
		if (!AppConfig.relFiguresDir.endsWith("/")) relFileNameBld.append('/');
		Utils.appendTo(relFileNameBld, CONTENT_FILENAME,
				isImageOrMovie ? content : Utils.getBuilder("MolImg", id));
		if (!Utils.isEmpty(extension)) {
			Utils.appendTo(relFileNameBld, '.', extension);
		} // if there's an extension
		final String relFileName = relFileNameBld.toString();
		debugPrint(SELF + "relFileName = ", relFileName);
		return relFileName;
	} // getRelImageFileName(String, boolean)

	/** Returns the Jmol scripts in a Jmol figure.
	 * @return	array of Jmol scripts, Jmol Javascript commands
	 */
	public String[] getJmolScripts() {
		final String[] scripts = new String[2];
		Arrays.fill(scripts, "");
		if (isJmol() && !Utils.isEmpty(extraData)) {
			final String[] components = extraData.split(JMOL_SEP);
			if (components.length >= 1) scripts[0] = components[0];
			if (components.length >= 2) scripts[1] = components[1];
		} // if extraData is not null
		return scripts;
	} // getJmolScripts()

	/** Returns the data, with hyperlinks formatted if it's text, with HTML
	 * formatting if it's a movie, resized if it's a Lewis structure.  
	 * <p>[link="6"]text[/link]
	 * <br>[link="14.22"]text[/link]
	 * </p>for links within and between chapters, respectively, where the
	 * numbers are 1-based for content or chapter and content.
	 * @return	formatted data
	 */
	public String getFormattedContent() {
		return getFormattedContent("", "", 0, 0);
	} // getFormattedContent()

	/** Returns the data, with hyperlinks formatted if it's text, with HTML
	 * formatting if it's a movie, resized if it's a Lewis structure.  
	 * <p>[link="6"]text[/link]
	 * <br>[link="14.22"]text[/link]
	 * </p>for links within and between chapters, respectively, where the
	 * numbers are 1-based for content or chapter and content.
	 * @param	pathToRoot	path to the application root from the page
	 * displaying the content
	 * @return	formatted data
	 */
	public String getFormattedContent(String pathToRoot) {
		return getFormattedContent("", pathToRoot, 0, 0);
	} // getFormattedContent(String)

	/** Returns the data, with hyperlinks formatted if it's text, with HTML
	 * formatting if it's a movie, resized if it's a Lewis structure.  
	 * Hyperlinks are in one of the formats:
	 * <p>[link="6"]text[/link]
	 * <br>[link="14.22"]text[/link]
	 * </p>for links within and between chapters, respectively, where the
	 * numbers are 1-based for content or chapter and content.
	 * @param	status	"read" or "write" or "preview"
	 * @param	pathToRoot	path to the application root from the page
	 * displaying the content
	 * @param	width	width of the video window
	 * @param	height	height of the video window
	 * @return	formatted data
	 */
	public String getFormattedContent(String status, String pathToRoot,
			int width, int height) {
		String data = content;
		if (isText()) {
			data = Utils.toDisplay(data);
			while (true) {
				final int linkOpen1 = data.indexOf(LINK_OPEN1);
				if (linkOpen1 < 0) break;
				final int urlNumsStart = linkOpen1 + LINK_OPEN1_LEN;
				final int urlNumsEnd = data.indexOf(LINK_OPEN2);
				final int linkOpen2End = urlNumsEnd + LINK_OPEN2_LEN;
				final String urlNumsStr = 
						data.substring(urlNumsStart, urlNumsEnd);
				final String[] urlNums = urlNumsStr.split("\\.");
				data = Utils.toString(data.substring(0, linkOpen1), 
						"<a href=\"", urlNums.length == 2
							? Utils.getBuilder(status, "Chapter.jsp?chapNum=",
								urlNums[0], "#content", urlNums[1])
							: Utils.getBuilder("#content", urlNums[0]),
						"\">", data.substring(linkOpen2End));
			} // while
			data = data.replaceAll(LINK_CLOSE_REGEX, "</a>");
		} else if (isMovie()) {
			final String[] parts = data.split("\\.");
			final String extension = parts[parts.length - 1];
			final StringBuilder whBld = new StringBuilder();
			if (width > 0) Utils.appendTo(whBld, " width=\"", width, '"');
			if (height > 0) Utils.appendTo(whBld, " height=\"", height, '"');
			final boolean isHtml5 = "mp4".equalsIgnoreCase(extension)
					|| "ogg".equalsIgnoreCase(extension)
					|| "webm".equalsIgnoreCase(extension);
			final StringBuilder bld = new StringBuilder();
			if (isHtml5) {
				Utils.appendTo(bld, "<video", whBld,
						" controls=\"controls\"><source src=\"", pathToRoot, 
						content, "\" type=\"video/", extension, "\" />");
			} // if isHtml5
			Utils.appendTo(bld, "<object", whBld,
					"><param name=\"src\" value=\"", pathToRoot, content,
					"\" /><param name=\"autoplay\" value=\"false\" />"
						+ "<param name=\"scale\" value=\"aspect\" />",
					"<embed src=\"", pathToRoot, content, '"', whBld,
					" autostart=\"false\" scale=\"aspect\">"
						+ "</embed></object>");
			if (isHtml5) bld.append("</video>");
			data = bld.toString();
		} else if (isImageURL()) {
			final String HTTP = "http://";
			if (!data.startsWith(HTTP)) {
				data = HTTP + data;
			} // if URL isn't complete
		} else if (isImage()) {
			data = Utils.toString(pathToRoot, data);
		}
		return data;
	} // getFormattedContent(String, String, int, int)

	/** Gets if H atoms should not be shown in MarvinView/Sketch. 
	 * @return	true if H atoms should not be shown
	 */
	public boolean showNoH() {
		return Question.showNoHydrogens(MathUtils.parseLong(extraData));
	} // showNoH()

	/** Gets if only H atoms on heteroatoms should be shown in
	 * MarvinView/Sketch.
	 * @return	true if only H atoms on heteroatoms should be shown in
	 * MarvinView/Sketch
	 */
	public boolean showHeteroH() {
		return Question.showHeteroHydrogens(MathUtils.parseLong(extraData));
	} // showHeteroH()

	/** Gets if all H atoms should be shown in MarvinView/Sketch. 
	 * @return	true if all H atoms should be shown
	 */
	public boolean showAllH() {
		return Question.showAllHydrogens(MathUtils.parseLong(extraData));
	} // showAllH()

	/** Gets if all C atoms should be shown in MarvinView/Sketch. 
	 * @return	true if all C atoms should be shown
	 */
	public boolean showAllC() {
		return Question.showAllCarbons(MathUtils.parseLong(extraData));
	} // showAllC()

	/** Gets if lone pairs should be shown in MarvinView/Sketch. 
	 * @return	true if lone pairs should be shown
	 */
	public boolean showLonePairs() {
		return Question.showLonePairs(MathUtils.parseLong(extraData));
	} // showLonePairs()

	/** Gets if mapping should be shown in MarvinView/Sketch. 
	 * @return	true if mapping should be shown
	 */
	public boolean showMapping() {
		return Question.showMapping(MathUtils.parseLong(extraData));
	} // showMapping()

	/** Gets if R/S labels should be shown in MarvinView/Sketch. 
	 * @return	true if R/S labels should be shown
	 */
	public boolean showRSLabels() {
		return Question.showRSLabels(MathUtils.parseLong(extraData));
	} // showRSLabels()

	/** Gets the applet size for displaying a Marvin drawing with standard bond
	 * lengths and font size.
	 * @return	array of dimensions
	 */
	public int[] getAppletSize() {
		return (isLewis() ? MolString.getAppletSize(getFormattedContent())
				: MolString.getAppletSize(content, showMapping()));
	} // getAppletSize()

	/** Gets the best applet size for displaying a Marvin drawing.
	 * @return	array of dimensions
	 */
	public int[] getBestAppletSize() {
		return MolString.getBestAppletSize(getFormattedContent(), 
				showMapping());
	} // getBestAppletSize()

} // TextContent
