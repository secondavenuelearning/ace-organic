package com.epoch.session;

import com.epoch.db.QSetRW;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.FileFormatException;
import com.epoch.exceptions.InvalidOpException;
import com.epoch.exceptions.ParameterException;
import com.epoch.exceptions.ParserException;
import com.epoch.qBank.QSetDescr;
import com.epoch.qBank.Topic;
import com.epoch.qBank.qBankConstants.TopicQSetConstants;
import com.epoch.utils.Utils;
import com.epoch.xmlparser.XMLUtils;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Initiates an authoring session. */
public class QuestionBank implements TopicQSetConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Topics in the question bank. */
	transient List<Topic> topics;
	/** Login ID of the instructor authoring. */
	transient String instructorId;
	/** Whether this authoring session is master or local. */
	transient boolean masterEdit;

	/** Constructor for master-authoring.
	 * @throws	DBException	if there's a problem reading the database
	 */
	public QuestionBank() throws DBException {
		masterEdit = true;
		initialize();
	} // QuestionBank()
	
	/** Constructor for local authoring.
	 * @param	instructId	login ID of the instructor
	 * @throws	DBException	if there's a problem reading the database
	 */
	public QuestionBank(String instructId) throws DBException {
		masterEdit = false;
		instructorId = instructId;	
		initialize();
	} // QuestionBank(String)

	/** Gets whether this authoring session is master or local.
	 * @return	true if the authoring session is master
	 */
	public boolean isMasterEdit() {
		return masterEdit;
	} // isMasterEdit()

	/** Returns the login ID of the instructor
	 * authoring in this question set.
	 * @return	the ID of the instructor authoring in this question set
	 */
	public String getInstructorId() {
		return instructorId;
	} // getInstructorId()

	/** Loads the topics (topics) of the data bank. 
	 * @throws	DBException	if there's a problem reading the database
	 */
	private void initialize() throws DBException {
		topics = (masterEdit ?  QSetRW.getTopics()
				: QSetRW.getTopics(instructorId));
	} // initialize()

	/** Gets the topics.
	 * @return	array of topics
	 */
	public Topic[] getTopics() {
		return topics.toArray(new Topic[topics.size()]);
	} // getTopics()

	/** Gets a topic.
	 * @param	index	1-based serial number of the topic
	 * @return	the topic
	 * @throws	ParameterException	if the serial number is out of range
	 */
	public Topic getTopic(int index) throws ParameterException {
		if (index <= 0 || index > topics.size())
			throw new ParameterException("Invalid index to getTopic(): "
					+ index + "; topics.size() = " + topics.size());
		return topics.get(index - 1);
	} // getTopic(int)

	/** Modifies a topic.
	 * @param	index	1-based serial number of the topic
	 * @param	topic	new data of the topic
	 * @throws	DBException	if there's a problem writing to the database
	 * @throws	ParameterException	if the serial number is out of range
	 */
	public void setTopic(int index, Topic topic) throws ParameterException,
			DBException {
		if (index <= 0 || index > topics.size())
			throw new ParameterException("Invalid index to setTopic(): "
					+ index + "; topics.size() = " + topics.size());
		if (!masterEdit) {
			throw new ParameterException("A local author cannot modify a topic.");
		}
		final Topic oldTopic = topics.get(index - 1);
		QSetRW.setTopic(oldTopic.id, topic);
		topic.id = oldTopic.id;	
		// qSets won't be modified !
		topic.qSets = oldTopic.qSets;
		// Utils.alwaysPrint(" setting ", index - 1, " to ", topic);
		topics.set(index - 1, topic);
	} // setTopic(int, Topic)

	/** Adds a new topic.
	 * @param	topic	data of the topic to be added
	 * @return	the new topic's ID number
	 * @throws	DBException	if there's a problem writing to the database
	 * @throws	InvalidOpException	if the author doesn't have authority to
	 * add a topic
	 */
	public int addTopic(Topic topic) throws InvalidOpException, DBException  {
		if (!masterEdit) {
			throw new InvalidOpException("A local author cannot add topics.");
		}
		topic.id = QSetRW.addTopic(topic);
		topics.add(topic);
		return topic.id;
	} // addTopic(Topic)

	/** Gets the number of questions in a question set.
	 * @param	qSetId	ID of the question set whose questions to count
	 * @return	the number of questions in the set
	 */
	public int getNumQsInQSet(int qSetId) {
		return QSetRW.getNumQsInQSet(qSetId, instructorId);
	} // getNumQsInQSet(int)
	
	/** Gets the descriptions of the question sets in a topic.
	 * @param	topicIndex	1-based serial number of the topic
	 * @return	array of descriptions of question sets
	 * @throws	DBException	if there's a problem reading the database
	 * @throws	ParameterException	if the serial number is out of range
	 */
	public QSetDescr[] getQSetDescrs(int topicIndex)
			throws ParameterException, DBException {
		if (topicIndex <= 0 || topicIndex > topics.size())
			throw new ParameterException("Invalid index for topicIndex: "
					+ topicIndex + "; topics.size() = " + topics.size());
		final Topic topic = topics.get(topicIndex - 1);
		return topic.getQSetDescrs();
	} // getQSetDescrs(int)

	/** Gets the description of a question set in a topic.
	 * @param	topicIndex	1-based serial number of the topic
	 * @param	index	1-based serial number of the question set
	 * @return	description of the question set
	 * @throws	ParameterException	if either index is invalid
	 */
	public QSetDescr getQSetDescr(int topicIndex, int index)
			throws ParameterException {
		if (topicIndex <= 0 || topicIndex > topics.size())
			throw new ParameterException("Invalid index for topicIndex: "
					+ topicIndex + "; topics.size() = " + topics.size());
		final Topic topic = topics.get(topicIndex - 1);
		return topic.getQSetDescr(index);
	} // getQSetDescr(int, int)

	/** Modifies the description of a question set in a topic.
	 * @param	topicIndex	1-based serial number of the topic
	 * @param	index	1-based serial number of the question set
	 * @param	newQSetDescr	new description of the question set
	 * @throws	DBException	if there's a problem writing to the database
	 * @throws	InvalidOpException	if the author doesn't have authority to
	 * modify the question set or add a topic
	 * @throws	ParameterException	if the serial number is out of range
	 */
	public void setQSetDescr(int topicIndex, int index,
			QSetDescr newQSetDescr) throws InvalidOpException, 
			ParameterException, DBException {
		if (topicIndex <= 0 || topicIndex > topics.size())
			throw new ParameterException("Invalid index for topicIndex: "
					+ topicIndex + "; topics.size() = " + topics.size());
		final Topic topic = topics.get(topicIndex - 1);
		if (index <= 0 || index > topic.qSets.size())
			throw new ParameterException("Invalid index for qSet index: "
					+ index + "; qSets.size() = " + topic.qSets.size());
		final QSetDescr oldQSet = topic.getQSetDescr(index);
		newQSetDescr.id = oldQSet.id;
		final boolean deleteTranslns = newQSetDescr.header == null
				|| !newQSetDescr.header.equals(oldQSet.header);
		if (masterEdit) {
			QSetRW.setQSetDescr(newQSetDescr, deleteTranslns);
		} else {
			if (oldQSet.id > 0) throw new InvalidOpException(
					"A local author cannot modify a master-database "
					+ "question set.");
			QSetRW.setQSetDescr(newQSetDescr, instructorId, 
					deleteTranslns);
		}
		topic.qSets.set(index - 1, newQSetDescr);
	} // setQSetDescr(int, int, QSetDescr)

	/** Deletes a question set from a topic.  Called only if the question set
	 * contains no questions.
	 * @param	topicIndex	1-based serial number of the topic
	 * @param	index	1-based serial number of the question set
	 * @throws	DBException	if there's a problem writing to the database
	 * @throws	ParameterException	if the serial numbers are out of range
	 */
	public void deleteQSet(int topicIndex, int index)
			throws ParameterException, DBException {
		if (topicIndex <= 0 || topicIndex > topics.size())
			throw new ParameterException("Invalid index for topicIndex: "
					+ topicIndex + "; topics.size() = " + topics.size());
		final Topic topic = topics.get(topicIndex - 1);
		if (index <= 0 || index > topic.qSets.size())
			throw new ParameterException("Invalid index for qSet index: "
					+ index + "; qSets.size() = " + topic.qSets.size());
		final QSetDescr terminalQSet = topic.qSets.get(index - 1);
		debugPrint("QuestionBank.deleteQSet: deleting qSet ",
				terminalQSet.name, " with id ", terminalQSet.id,
				" from topic ", topic.name);
		QSetRW.deleteQSet(terminalQSet.id);
		topic.qSets.remove(index - 1);
	} // deleteQSet(int, int)

	/** Adds a new question set to a topic.
	 * @param	topicIndex	1-based serial number of the topic
	 * @param	newQSetDescr	new description of the question set
	 * @return	unique ID number of the new question set
	 * @throws	DBException	if there's a problem writing to the database
	 * @throws	ParameterException	if the serial number is out of range
	 */
	public int addQSet(int topicIndex, QSetDescr newQSetDescr)
			throws ParameterException, DBException {
		if (topicIndex <= 0 || topicIndex > topics.size())
			throw new ParameterException("Invalid index for topicIndex: "
					+ topicIndex + "; topics.size() = " + topics.size());
		final Topic topic = topics.get(topicIndex - 1);
		return addQSet(topic, newQSetDescr);
	} // addQSet(int, QSetDescr)

	/** Adds a new question set to a topic.
	 * @param	topic	the topic
	 * @param	newQSetDescr	new description of the question set
	 * @return	unique ID number of the new question set
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public int addQSet(Topic topic, QSetDescr newQSetDescr) throws DBException {
		newQSetDescr.id = (masterEdit ?
				QSetRW.addQSet(topic.id, newQSetDescr)
				: QSetRW.addQSet(topic.id,
						newQSetDescr, instructorId));
		topic.qSets.add(newQSetDescr);
		return newQSetDescr.id;
	} // addQSet(Topic, QSetDescr)

	/** Moves a question set within a topic.
	 * @param	topicNum	1-based number of the topic
	 * @param	moveQSetFrom	1-based number of the question set to move
	 * @param	moveTo	new 1-based number of the question set
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void moveQSet(int topicNum, int moveQSetFrom, int moveTo) 
			throws DBException {
		final Topic topic = topics.get(topicNum - 1);
		topic.qSets.add(moveTo - 1, topic.qSets.remove(moveQSetFrom - 1));
		QSetRW.reorderQSets(topic);
	} // moveQSet(int, int, int)

	/** Export all topics, question sets, and questions to a zip file.
	 * @param	filename	location and name of the file in which the exported
	 * questions will be stored
	 * @return	name of the zip file
	 * @throws	DBException	if there's a problem reading the database
	 */
	public String exportQuestionBank(String filename) throws DBException {
		return XMLUtils.zipXML(filename, toXML());
	} // exportQuestionBank(String, String)
	
	/** Convert all topics, question sets, and questions to XML.
	 * @return	list where the first member is XML, remaining are file names of
	 * images
	 * @throws	DBException	if there's a problem reading the database
	 */
	private List<String> toXML() throws DBException {
		final List<String> xmlAndImageNames = new ArrayList<String>();
		final StringBuilder opXML = 
				Utils.getBuilder(XMLUtils.startTag(QBANK_TAG));
		int topicNum = 0;
		for (final Topic topic : topics) {
			final List<String> topicXmlAndImageNames = topic.toXML();
			debugPrint("QuestionBank.toXML: topic ", ++topicNum, 
					" converted to xml.");
			Utils.appendTo(opXML, 
					topicXmlAndImageNames.remove(0), "\n\n");
			xmlAndImageNames.addAll(topicXmlAndImageNames);
		} // for each setNum
		opXML.append(XMLUtils.endTag(QBANK_TAG));
		xmlAndImageNames.add(0, opXML.toString());
		return xmlAndImageNames;
	} // toXML(String)

	/** Import topics, etc. from a zipped file, saving images to disk.
	 * @param	zipFilename	name and location of zip file containing the questions
	 * @return	how the import process went
	 * @throws	FileFormatException	if the file can't be found
	 * @throws	DBException	if there's a problem writing to the database
	 * @throws	ParserException	if the XML cannot be parsed
	 * @throws	InvalidOpException	if the author doesn't have authority to
	 * add a topic
	 */
	public String importTopics(String zipFilename) throws FileFormatException,
			InvalidOpException, ParserException, DBException {
		return parseXML(XMLUtils.extractNodes(zipFilename));
	} // importTopics(String)

	/** Parse questions XML, saving them in entirely new topics.
	 * @param	nodeList	nodes obtained from the XML
	 * @return	how the import process went
	 * @throws	DBException	if there's a problem writing to the database
	 * @throws	ParserException	if the XML cannot be parsed
	 * @throws	InvalidOpException	if the author doesn't have authority to
	 * add a topic
	 */
	public String parseXML(NodeList nodeList) throws ParserException,
			InvalidOpException, DBException {
		final StringBuilder parserOutput = new StringBuilder();
		debugPrint("QuestionBank.parseXML: nodes length = ", 
				nodeList.getLength());
		Topic topic = null;
		boolean foundTopic = false;
		for (int nodeNum = 0; nodeNum < nodeList.getLength(); nodeNum++) {
			final Node n = nodeList.item(nodeNum);
			if (n.getNodeType() == Node.TEXT_NODE) continue;
			else if (n.getNodeName().equalsIgnoreCase(TOPIC_TAG)) {
				if (foundTopic)
					parserOutput.append("<P class=\"boldtext\">"
							+ "*** End of topic ***</P>");
				foundTopic = true;
				parserOutput.append("<P class=\"boldtext\">"
						+ "*** Start of new topic ***</P>");
				topic = new Topic();
				topic.instructorId = instructorId;
				final int topicId = addTopic(topic);
				debugPrint("QuestionBank.parseXML: Entering a new topic, "
						+ "stored and gave ID ", topicId);
				parserOutput.append(topic.parseXML(n.getChildNodes(),
						STORE_DATA));
				QSetRW.setTopic(topicId, topic);
			} else if (n.getNodeName().equalsIgnoreCase(QSET_TAG)) {
				throw new ParserException("Can't import questions without "
						+ "having chosen a topic first.");
			} // if it's a question
		} // for each node
		if (foundTopic)
			parserOutput.append("<P class=\"boldtext\">"
					+ "*** End of topic ***</P>");
		return parserOutput.toString();
	} // parseXML(String)

} // QuestionBank

