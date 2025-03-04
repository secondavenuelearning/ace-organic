package com.epoch.xmlparser;

import com.epoch.AppConfig;
import com.epoch.exceptions.FileFormatException;
import com.epoch.qBank.qBankConstants.TopicQSetConstants;
import com.epoch.utils.Utils;
import com.epoch.xmlparser.xmlConstants.XMLConstants;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.dom4j.DocumentException;
import org.dom4j.io.DOMWriter;
import org.dom4j.io.SAXReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Holds XML utility functions.  */
public final class XMLUtils implements TopicQSetConstants, XMLConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Export XML of topics or question sets or questions to a zip file.
	 * @param	filename	name of the file in which the exported questions
	 * will be stored
	 * @param	xmlAndImageNames	array whose first member is the XML, and
	 * remaining members are filenames of images
	 * @return	the name of the zip file
	 */
	public static String zipXML(String filename, 
			List<String> xmlAndImageNames) {
		final String SELF = "XMLUtils.zipXML: ";
		final String zipFilename = 
				Utils.toString(AppConfig.relTempDir, 
					AppConfig.relTempDir.endsWith("/") ? "" : '/',
				filename, ".zip");
		final byte[] buf = new byte[1024];
		ZipOutputStream out = null;
		try {
			out = new ZipOutputStream(new FileOutputStream(
					Utils.toString(AppConfig.appRoot, 
						AppConfig.appRoot.endsWith("/") ? "" : '/',
						zipFilename)));
			debugPrint(SELF + "writing zip file to ", AppConfig.appRoot, 
					zipFilename);
			// write out the xml to the zip output stream
			out.putNextEntry(new ZipEntry(
					Utils.toString(filename, XML_SUFFIX)));
			final String xml = xmlAndImageNames.remove(0);
			out.write(xml.getBytes(StandardCharsets.UTF_8), 0, xml.length());
			out.closeEntry();
			// add each of the image files to the zip stream
			int imgNum = 0;
			for (final String imagefile : xmlAndImageNames) {
				debugPrint(SELF + "writing image ", ++imgNum, " to ", 
						imagefile);
				out.putNextEntry(new ZipEntry(imagefile));
				final FileInputStream inStream = new FileInputStream(
						Utils.toString(AppConfig.appRoot, 
							AppConfig.appRoot.endsWith("/") ? "" : '/',
							imagefile));
				// Transfer bytes from the file to the ZIP file
				int len;
				try {
					while ((len = inStream.read(buf)) > 0) {
						out.write(buf, 0, len);
					} // while
				} finally {
					inStream.close();
				} // try
				debugPrint(SELF + "closing image file in zip file.");
				out.closeEntry();
			} // each image
		} catch (IOException e) {
			System.out.println("IOException thrown in XMLUtils.zipXML: ");
			e.printStackTrace();
		} finally {
			if (out != null) try {
				debugPrint(SELF + "closing zip file.");
				out.close();
			} catch (IOException e) {
				debugPrint(SELF + "error closing zip file.");
				// but do nothing more
			} // try
		} // try
		return zipFilename;
	} // zipXML(String, String[])

	/** Extract node list from an XML file, saving images to disk.
	 * @param	zipFilename	name and location of zip file containing the questions
	 * @return	a NodeList
	 * @throws	FileFormatException	if the XML cannot be parsed
	 */
	public static NodeList extractNodes(String zipFilename)
			throws FileFormatException {
		final String SELF = "XMLUtils.extractNodes: ";
		debugPrint(SELF + "zipFilename = ", zipFilename);
		NodeList nodeList = null;
		final String xmlFile = extractXmlAndImages(zipFilename);
		debugPrint(SELF + "xmlFilename = ", xmlFile);
		try {
			final DocumentBuilderFactory factory =
					DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setNamespaceAware(true);
			final DocumentBuilder builder = factory.newDocumentBuilder();
			final Document doc = builder.parse(new File(xmlFile));
			final Element root = doc.getDocumentElement();
			debugPrint(SELF + "root elem ", root.getTagName());
			nodeList = root.getChildNodes();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (ParserConfigurationException e2) {
			e2.printStackTrace();
		} catch (SAXException e3) {
			e3.printStackTrace();
		}
		if (nodeList == null) {
			throw new FileFormatException("XML parser error.");
		}
		return nodeList;
	} // extractNodes(String)

	/** Convert a string containing XML to the parent node.
	 * @param	xmlStr	String containing the XML to be parsed
	 * @return	the string converted to a Node
	 */
	public static Node xmlToNode(String xmlStr) {
		final String SELF = "XMLUtils.xmlToNode: ";
		Node node = null;
		if (xmlStr != null) try {
			final StringReader strReader = new StringReader(xmlStr);
			final SAXReader saxReader = new SAXReader();
			final DOMWriter writer = new DOMWriter();
			final Document document = 
					writer.write(saxReader.read(strReader));
			node = (Node) document.getDocumentElement();
		} catch (DocumentException e2) {
			debugPrint(SELF + "couldn't parse document: ",
					e2.getMessage());
		}
		return node;
	} // xmlToNode(String)

	/** Acquire XML and images from a zipped file, saving images to disk.
	 * @param	zipFilename	name and location of zip file containing the questions
	 * @return	the XML file
	 * @throws	FileFormatException	if the file isn't zipped
	 */
	public static String extractXmlAndImages(String zipFilename)
			throws FileFormatException {
		final String SELF = "XMLUtils.extractXmlAndImages: ";
		String xmlFile = null;
		try {
			// unzip the file in the tempfiles folder
			// and get the filenames
			final ZipInputStream inStream = new ZipInputStream(
					new FileInputStream(zipFilename));
			ZipEntry entry = null;
			final List<String> fileNames = new ArrayList<String>();
			boolean foundFile = false;
			while ((entry = inStream.getNextEntry()) != null) {
				foundFile = true;
				final String filename = entry.getName();
				debugPrint(SELF + "filename in zip = ", filename);
				// transfer temp-images to approot and xml files to temp-folder
				final StringBuilder outFileNameBld = 
						Utils.getBuilder(AppConfig.appRoot);
				if (!AppConfig.appRoot.endsWith("/")) {
					outFileNameBld.append('/');
				}
				if (filename.endsWith(XML_SUFFIX)) {
					Utils.appendTo(outFileNameBld, AppConfig.relTempDir,
							Utils.getRandName(), XML_SUFFIX);
				} else {
					outFileNameBld.append(filename);
				}
				final String outFileName = outFileNameBld.toString();
				debugPrint(SELF + "writing to file = ", outFileName);
				final DataOutputStream out = new DataOutputStream(
						new FileOutputStream(outFileName));
				// Transfer bytes from the ZIP file to the output file
				final byte[] buf = new byte[1024];
				int len;
				while ((len = inStream.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				out.close();
				fileNames.add(outFileName);
			} // while inStream.getNextEntry() != null
			if (foundFile) {
				debugPrint(SELF, fileNames.size(), " file(s) found.");
			} else {
				debugPrint(SELF + "no files found.");
			} // if found a file
			inStream.close();
			
			// ensure that only one file is XML and get its name
			final int initialLen = fileNames.size();
			for (int fileNum = initialLen - 1; fileNum >= 0; fileNum--) {
				final String fileName = fileNames.get(fileNum);
				if (fileName.endsWith(XML_SUFFIX)) {
					xmlFile = fileName;
					fileNames.remove(fileNum);
				} // if the current file is XML
			} // for each file
			final int newLen = fileNames.size();
			// final String[] imageFiles = fileNames.toArray(new String[newLen]);
			// there must be exactly one .xml file
			if (xmlFile == null || newLen != initialLen - 1) {
				throw new FileFormatException("Invalid zip file format: ");
			}
			debugPrint(SELF + "xml file = ", xmlFile);
		} catch (IOException e1) {
			e1.printStackTrace();
		} //try
		return xmlFile;
	} // extractXmlAndImages(String)

	/** Wraps a tag with &lt; &gt;.
	 * @param	tag	the name of the XML tag
	 * @return	the tag wrapped in &lt; &gt;
	 */
	public static StringBuilder startTag(String tag) {
		return startTag(tag, NEWLINE);
	} // startTag(String)

	/** Wraps a tag with &lt; &gt;.
	 * @param	tag	the name of the XML tag
	 * @param	newLine	whether to add a new line
	 * @return	the tag wrapped in &lt; &gt;
	 */
	public static StringBuilder startTag(String tag, boolean newLine) {
		return Utils.getBuilder(OPEN, tag, newLine ? CLOSE_NL : CLOSE);
	} // startTag(String, boolean)

	/** Wraps a tag and its attributes with &lt; /&gt;.
	 * @param	tag	the name of the XML tag
	 * @param	attributes	array of 2-member arrays of attributes and 
	 * their values
	 * @return	the tag wrapped in &lt; &gt;
	 */
	public static StringBuilder startTag(String tag, String[]... attributes) {
		return startTag(tag, attributes, NEWLINE, !SELF_CLOSING);
	} // startTag(String, String[][])

	/** Wraps a tag and its attributes with &lt; &gt;.
	 * @param	tag	the name of the XML tag
	 * @param	attributes	array of 2-member arrays of attributes and 
	 * their values
	 * @param	newLine	whether to add a new line
	 * @return	the tag and its attributes wrapped in &lt; &gt;; attribute
	 * values are formatted appropriately
	 */
	public static StringBuilder startTag(String tag, 
			String[][] attributes, boolean newLine) {
		return startTag(tag, attributes, newLine, !SELF_CLOSING);
	} // startTag(String, String[][], boolean)

	/** Wraps a tag and its attributes with &lt; /&gt;.
	 * @param	tag	the name of the XML tag
	 * @param	attributes	any number of 2-member arrays of attributes and 
	 * their values
	 * @return	the tag wrapped in &lt; /&gt;
	 */
	public static StringBuilder startAndCloseTag(String tag, 
			String[]... attributes) {
		return startTag(tag, attributes, NEWLINE, SELF_CLOSING);
	} // startAndCloseTag(String, String[]...)

	/** Wraps a tag and its attributes with &lt; &gt;.
	 * @param	tag	the name of the XML tag
	 * @param	attributes	array of 2-member arrays of attributes and 
	 * their values
	 * @param	newLine	whether to add a new line
	 * @param	selfClosing	whether the tag is self-closing
	 * @return	the tag and its attributes wrapped in &lt; &gt; or 
	 * &lt; /&gt;; attribute values are formatted appropriately
	 */
	public static StringBuilder startTag(String tag, 
			String[][] attributes, boolean newLine, boolean selfClosing) {
		final String SELF = "XMLUtils.startTag: ";
		final StringBuilder bld = Utils.getBuilder(OPEN, tag);
		for (final String[] attribute : attributes) {
			Utils.appendTo(bld, ' ', attribute[0], "=\"",
					Utils.toValidHTMLAttributeValue(attribute[1]), '"');
		} // for each attribute
		return bld.append(selfClosing 
				? SELF_CLOSE : newLine ? CLOSE_NL : CLOSE);
	} // startTag(String, String[][], boolean, boolean)

	/** Wraps a tag with &lt;/ &gt;.
	 * @param	tag	the name of the XML tag
	 * @return	the tag wrapped in &lt;/ &gt;
	 */
	public static StringBuilder endTag(String tag) {
		return endTag(tag, NEWLINE);
	} // endTag(String)

	/** Wraps a tag with &lt;/ &gt;.
	 * @param	tag	the name of the XML tag
	 * @param	newLine	whether to add a new line
	 * @return	the tag wrapped in &lt;/ &gt;
	 */
	public static StringBuilder endTag(String tag, boolean newLine) {
		return Utils.getBuilder(OPEN_END, tag, newLine ? CLOSE_NL : CLOSE);
	} // endTag(String, boolean)

	/** Surround the given subnode with the given XML tags.  
	 * @param	tag	the name of the XML tag
	 * @param	subnode	the subnode of the node
	 * @return	the modified StringBuilder
	 */
	public static StringBuilder wrapNode(String tag, StringBuilder subnode) {
		return subnode.insert(0, startTag(tag)).append('\n')
				.append(endTag(tag));
	} // wrapNode(String, StringBuilder)

	/** Surround the given subnode with the given XML tags.
	 * @param	tag	the name of the XML tag
	 * @param	attributes	array of 2-member arrays of attributes and 
	 * their values
	 * @param	subnode	the subnode
	 * @return	the modified StringBuilder
	 */
	public static StringBuilder wrapNode(String tag, String[][] attributes, 
			StringBuilder subnode) {
		return subnode.insert(0, startTag(tag, attributes)).append('\n')
				.append(endTag(tag));
	} // wrapNode(String, String[][], StringBuilder)

	/** Surround the given text with the given XML tags.  Do NOT call this
	 * method for molecules; instead, use makeNode(tag, text, needsNewLine) and
	 * set needsNewLine to !NEWLINE.
	 * @param	tag	the name of the XML tag
	 * @param	text	the text of the node
	 * @return	the text surrounded by XML tags
	 */
	public static StringBuilder makeNode(String tag, String text) {
		final boolean needsNewLine = text != null && text.indexOf('\n') >= 0;
		return makeNode(tag, text, needsNewLine);
	} // makeNode(String, String)

	/** Surround the given text with the given XML tags.  Call this method for
	 * molecules with needsNewLine set to !NEWLINE, and do not trim the values
	 * obtained after parsing the XML; MOL format requires an opening return 
	 * character, and MRV requires no opening return character.
	 * @param	tag	the name of the XML tag
	 * @param	text	the text of the node
	 * @param	needsNewLine	whether to put a return character after the
	 * opening tag and the data; always make false for molecules
	 * @return	the text surrounded by XML tags
	 */
	public static StringBuilder makeNode(String tag, String text, 
			boolean needsNewLine) {
		final StringBuilder bld = 
				Utils.getBuilder(startTag(tag, needsNewLine), toValidXML(text));
		if (needsNewLine) bld.append('\n');
		return bld.append(endTag(tag));
	} // makeNode(String, String, boolean)

	/** Surround the given value with the given XML tags.
	 * @param	tag	the name of the XML tag
	 * @param	num	the value of the node
	 * @return	the value surrounded by XML tags
	 */
	public static StringBuilder makeNode(String tag, int num) {
		return Utils.getBuilder(startTag(tag, !NEWLINE), num, endTag(tag));
	} // makeNode(String, int)

	/** Surround the given value with the given XML tags.
	 * @param	tag	the name of the XML tag
	 * @param	num	the value of the node
	 * @return	the value surrounded by XML tags
	 */
	public static StringBuilder makeNode(String tag, long num) {
		return Utils.getBuilder(startTag(tag, !NEWLINE), num, endTag(tag));
	} // makeNode(String, long)

	/** Surround the given value with the given XML tags.
	 * @param	tag	the name of the XML tag
	 * @param	num	the value of the node
	 * @return	the value surrounded by XML tags
	 */
	public static StringBuilder makeNode(String tag, double num) {
		return Utils.getBuilder(startTag(tag, !NEWLINE), num, endTag(tag));
	} // makeNode(String, double)

	/** Surround the given value with the given XML tags.
	 * @param	tag	the name of the XML tag
	 * @param	value	the value of the node
	 * @return	the value surrounded by XML tags
	 */
	public static StringBuilder makeNode(String tag, boolean value) {
		return Utils.getBuilder(startTag(tag, !NEWLINE), value, endTag(tag));
	} // makeNode(String, boolean)

	/** Surround the given value with the given XML tags.
	 * @param	tag	the name of the XML tag
	 * @param	num	the value of the node
	 * @return	the value surrounded by XML tags
	 */
	public static StringBuilder makeNode(String tag, Integer num) {
		return Utils.getBuilder(startTag(tag, !NEWLINE), num.toString(), 
				endTag(tag));
	} // makeNode(String, Integer)

	/** Surround a translation with appropriate XML tags.
	 * @param	language	the language
	 * @param	transln	the translation
	 * @return	the translation wrapped in XML tags
	 */
	public static StringBuilder makeTranslnNode(String language, 
			String transln) {
		return makeNode(TRANSLATION_TAG, 
				new String[][] { {LANGUAGE_TAG, language} }, 
				transln);
	} // makeTranslnNode(String, String)

	/** Surround the given text with the given XML tags.
	 * @param	tag	the name of the XML tag
	 * @param	attributes	array of 2-member arrays of attributes and 
	 * their values
	 * @param	text	the text of the node
	 * @return	the text surrounded by XML tags
	 */
	public static StringBuilder makeNode(String tag, 
			String[][] attributes, String text) {
		return makeNode(tag, attributes, text, NEWLINE);
	} // makeNode(String, String[][], String)

	/** Surround the given text with the given XML tags.
	 * @param	tag	the name of the XML tag
	 * @param	attributes	array of 2-member arrays of attributes and 
	 * their values
	 * @param	text	the text of the node
	 * @param	newLine	whether to add a new line after the close tag
	 * @return	the text surrounded by XML tags
	 */
	public static StringBuilder makeNode(String tag, 
			String[][] attributes, String text, boolean newLine) {
		return Utils.getBuilder(startTag(tag, attributes, !NEWLINE),
				toValidXML(text), endTag(tag, newLine));
	} // makeNode(String, String[][], String)

	/** Surround the given text with XML comment tags.
	 * @param	text	the text of the comment
	 * @return	the text surrounded by XML comment tags
	 */
	public static StringBuilder comment(String text) {
		return Utils.getBuilder("<!-- ", toValidXML(text), " -->");
	} // comment(String)

	/** Convert a string for export to XML.
	 * @param	text   a string
	 * @return	XML-compatible string
	 */
	public static String toValidXML(String text) {
		return Utils.toValidTextbox(text, Utils.ALWAYS);
	} // toValidXML(String)

	/** Disables external instantiation. */
	private XMLUtils() { }
	
} // XMLUtils
