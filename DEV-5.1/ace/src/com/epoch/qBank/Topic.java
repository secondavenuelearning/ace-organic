package com.epoch.qBank;

import com.epoch.exceptions.DBException;
import com.epoch.exceptions.FileFormatException;
import com.epoch.exceptions.ParameterException;
import com.epoch.exceptions.ParserException;
import com.epoch.qBank.qBankConstants.TopicQSetConstants;
import com.epoch.session.QSet;
import com.epoch.utils.Utils;
import com.epoch.xmlparser.XMLUtils;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Holds topic information while viewing/editing/adding topic. 
	Topics contain question sets, which contain questions.
	Scope: loaded by DB layer, used by UI layer (view/edit);
		   created by UI layer, written by DB layer (add). 
	Sessions: QuestionBank.
*/
public class Topic implements TopicQSetConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Unique ID number. */
	transient public int id;
	/** Name of the topic, such as "Alkenes". */
	transient public String name;
	/** Miscellaneous remarks about the topic. */
	transient public String remarks;
	/** Descriptions of the question sets in this topic. */
	transient public List<QSetDescr> qSets;
	/** Login ID of the instructor authoring. */
	transient public String instructorId;
	/** Constructor.
	 */
	public Topic() {
		id = 0;
		instructorId = MASTER_AUTHOR;
		qSets = new ArrayList<QSetDescr>();
	} // Topic()

	/** Constructor.
	 * @param	topicId	unique ID of this topic
	 */
	public Topic(int topicId) {
		id = topicId;
		instructorId = MASTER_AUTHOR;
		qSets = new ArrayList<QSetDescr>();
	} // Topic(int)

	/** Constructor.
	 * @param	topicId	unique ID of this topic
	 * @param	instructId	login ID of the instructor; null if master edit 
	 */
	public Topic(int topicId, String instructId) {
		id = topicId;
		instructorId = instructId;
		qSets = new ArrayList<QSetDescr>();
	} // Topic(int)

	/** Gets the descriptions of the question sets in the topic.  Assumes the
	 * topic has already been initialized in session/QuestionBank.
	 * @return	array of descriptions of question sets
	 */
	public QSetDescr[] getQSetDescrs() {
		final QSetDescr[] result = new QSetDescr[qSets.size()];
		qSets.toArray(result);
		return result;
	} // getQSetDescrs()

	/** Gets the description of a question set in the topic.
	 * @param	index	1-based serial number of the question set
	 * @return	description of the question set
	 * @throws	ParameterException	if the serial number is out of range
	 */
	public QSetDescr getQSetDescr(int index) throws ParameterException {
		if (index <= 0 || index > qSets.size())
			throw new ParameterException("Invalid index ");
		return qSets.get(index - 1);
	} // getQSetDescr(int)

	/** Export all question sets in this topic to a zip file.
	 * @param	filename	location and name of the file in which the exported 
	 * questions will be stored
	 * @return	the name of the zip file
	 * @throws	DBException	if the question sets can't be populated
	 */
	public String exportTopic(String filename) throws DBException {
		return XMLUtils.zipXML(filename, toXML());
	} // exportTopic(String, String) 

	/** Convert all question sets in this topic to XML.
	 * @return	list where the first member is XML, remaining are file names of
	 * images
	 * @throws	DBException	if the question sets can't be populated
	 */
	public List<String> toXML() throws DBException {
		final String SELF = "Topic.toXML: "; 
		final List<String> xmlAndImageNames = new ArrayList<String>();
		final StringBuilder opXML = Utils.getBuilder(startTopic(), 
				makeNode(NAME_TAG, name), makeNode(REMARKS_TAG, remarks));
		int setNum = 0;
		for (final QSetDescr setDescr : qSets) {
			try {
				final QSet qSet = (instructorId == MASTER_AUTHOR
						? new QSet(setDescr.id)
						: new QSet(setDescr.id, instructorId));
				final List<String> qSetXmlAndImageNames = qSet.toXML();
				debugPrint(SELF + "qSet ", ++setNum, " converted to xml.");
				Utils.appendTo(opXML, 
						qSetXmlAndImageNames.remove(0), "\n\n");
				xmlAndImageNames.addAll(qSetXmlAndImageNames);
			} catch (ParameterException e) {
				Utils.alwaysPrint(SELF + "no question set with id ",
						setDescr.id);
			} // try
		} // for each setNum
		opXML.append(endTopic());
		xmlAndImageNames.add(0, opXML.toString());
		return xmlAndImageNames;
	} // toXML(String)

	/** Wraps a tag with &lt; &gt;.
	 * @return	the tag wrapped in &lt; &gt;
	 */
	private StringBuilder startTopic() {
		return XMLUtils.startTag(TOPIC_TAG, new String[][] {
					{"id", String.valueOf(id)}
				});
	} // startTopic()

	/** Surround the given text with the given XML tags.
	 * @param	tag	the name of the XML tag
	 * @param	text	the text of the node
	 * @return	the text surrounded by XML tags
	 */
	private StringBuilder makeNode(String tag, String text) {
		return XMLUtils.makeNode(tag, text);
	} // makeNode(String, String)

	/** Wraps a tag with &lt;/ &gt;.
	 * @return	the tag wrapped in &lt;/ &gt;
	 */
	private StringBuilder endTopic() {
		return XMLUtils.endTag(TOPIC_TAG);
	} // endTopic()

	/** Import questions from a zipped file, saving images to disk. 
	 * @param	zipFilename	name and location of zip file containing the questions
	 * @return	how the import process went
	 * @throws	FileFormatException	if the XML cannot be parsed
	 * @throws	ParserException	if a node can't be parsed
	 */ 
	public String importSets(String zipFilename) throws FileFormatException,
			ParserException {
		return parseXML(XMLUtils.extractNodes(zipFilename), STORE_DATA);
	} // importSets(String)

	/** Parse topic XML, saving question sets as new question sets within this
	 * topic.
	 * @param	nodeList	nodes obtained from the XML
	 * @param	storeData	whether to store name and remarks in this topic
	 * @return	how the import process went
	 * @throws	ParserException	if a node can't be parsed or a question set
	 * hasn't been chosen
	 */ 
	public String parseXML(NodeList nodeList, boolean storeData) 
			throws ParserException {
		final String SELF = "Topic.parseXML: "; 
		final StringBuilder parserOutput = new StringBuilder();
		debugPrint(SELF + "nodes length = ", nodeList.getLength());
		final String questionTag = Question.getTag();
		boolean foundQSet = false;
		final QSet qSet = new QSet(instructorId);
		for (int nodeNum = 0; nodeNum < nodeList.getLength(); nodeNum++) {
			final Node n = nodeList.item(nodeNum);
			if (n.getNodeType() == Node.TEXT_NODE) {
				continue;
			} else if (n.getNodeName().equalsIgnoreCase(TOPIC_TAG)) {
				Utils.alwaysPrint(SELF + "encountered new topic "
						+ "unexpectedly; calling parseXML() recursively.");
				parserOutput.append(parseXML(n.getChildNodes(), !STORE_DATA));
			} else if (n.getNodeName().equalsIgnoreCase(NAME_TAG) && storeData) {
				final Node child = n.getFirstChild();
				name = (child == null ? "" : child.getNodeValue().trim());
				debugPrint(SELF + "name = ", name);
			} else if (n.getNodeName().equalsIgnoreCase(REMARKS_TAG) && storeData) {
				final Node child = n.getFirstChild();
				remarks = (child == null ? "" : child.getNodeValue().trim());
				debugPrint(SELF + "remarks = ", remarks);
			} else if (n.getNodeName().equalsIgnoreCase(QSet.QSET_TAG)) {
				if (foundQSet) parserOutput.append(
						"<P class=\"boldtext\">--- End of question set ---</P>");
				foundQSet = true;
				Utils.appendTo(parserOutput, "<P class=\"boldtext\">"
							+ "--- Start of new question set ---</P>",
						qSet.parseXML(n.getChildNodes(), this));
				// setDescr is added to topic in qSet.parseXML() when it
				// is written to the database
			} else if (n.getNodeName().equalsIgnoreCase(questionTag)) {
				throw new ParserException("Can't import questions without "
						+ "having chosen a question set first.");
			} // if it's a question
		} // for each node
		if (foundQSet) 
			parserOutput.append("<P class=\"boldtext\">"
					+ "--- End of question set ---</P>");
		return parserOutput.toString();
	} // parseXML(String)

} // Topic
