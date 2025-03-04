package com.epoch.session;

import com.epoch.assgts.Assgt;
import com.epoch.assgts.assgtConstants.AssgtConstants;
import com.epoch.assgts.AssgtQGroup;
import com.epoch.db.HWRead;
import com.epoch.db.HWWrite;
import com.epoch.db.QuestionRW;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.FileFormatException;
import com.epoch.exceptions.NonExistentException;
import com.epoch.utils.Utils;
import com.epoch.xmlparser.XMLUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Session to extract homework information into a string which can be
  stored externally, and vice versa.
*/
public final class ExportImportSession implements AssgtConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Tag for XML.  */
	private final static String HWS_TAG = "assignments";

	/** Exports one or more assignment descriptions to an XML file.
	 * @param	hwIds	unique ID numbers of assignments to export
	 * @return	the XML
	 * @throws	DBException	if there's a problem reading the database
	 * @throws	NonExistentException	if one of the HWs doesn't exist
	 */
	public static String exportHWSets(int[] hwIds)
			throws NonExistentException, DBException {
		final Assgt[] assgts = HWRead.getHWs(hwIds);
		debugPrint("ExportImportSession.exportHWSets: "
				+ "assgts length = ", assgts.length);
		final StringBuilder opXML = 
				Utils.getBuilder(XMLUtils.startTag(HWS_TAG));
		for (int hwNum = 0; hwNum < assgts.length; hwNum++) {
			opXML.append(assgts[hwNum].toXML());
		}
		return Utils.toString(opXML, XMLUtils.endTag(HWS_TAG));
	} // exportHWSets(int[])

	/** Imports one or more assignments from an XML file.
	 * @param	assgntXML	XML describing the assignments
	 * @param	instructorId	instructor importing these assignments
	 * @param	courseId	course into which the assignments are being imported
	 * @return	the number of assignments imported
	 * @throws	DBException	if there's a problem writing to the database
	 * @throws	FileFormatException	if a file can't be opened or read
	 */
	public static int importHWSets(String assgntXML, String instructorId, 
			int courseId) throws FileFormatException, DBException {
		final String SELF = "ExportImportSession.importHWSets: ";
		final ArrayList<Assgt> assgts = new ArrayList<Assgt>();
		try {
			final DocumentBuilderFactory factory = 
					DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setNamespaceAware(true);
			final DocumentBuilder builder = factory.newDocumentBuilder();
			final Document document = builder.parse(
					new ByteArrayInputStream(
						assgntXML.getBytes(StandardCharsets.UTF_8))
					);
			final Element root = document.getDocumentElement();
			debugPrint(SELF + "root elem ", root.getTagName());
			final NodeList nodeList = root.getChildNodes();
			debugPrint(SELF + "nodes length = ", nodeList.getLength());
			final String descrTag = Assgt.getTag();
			for (int nodeNum = 0; nodeNum < nodeList.getLength(); nodeNum++) {
				final Node node = nodeList.item(nodeNum);
				if (node.getNodeType() == Node.TEXT_NODE) {
					continue;
				} else if (node.getNodeName().equalsIgnoreCase(descrTag)) {
					// one assignment description
					final Assgt assgt = Assgt.parseXML(node);
					assgt.instructorId = instructorId;
					assgt.courseId = courseId;
					assgt.clearExtensions();
					debugPrint(SELF + "parsed assgt ", assgts.size() + 1,
							": ", assgt.toString());
					assgts.add(assgt);
				} // if node is assignment description
			} // for all nodes
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new FileFormatException("XML parser error: "
					+ e1.getMessage());
		} catch (ParserConfigurationException e2) {
			e2.printStackTrace();
			throw new FileFormatException("XML parser error: "
					+ e2.getMessage());
		} catch (SAXException e3) {
			e3.printStackTrace();
			throw new FileFormatException("XML parser error: "
					+ e3.getMessage());
		}
		int numNewHWs = assgts.size();
		debugPrint(SELF, instructorId + " has imported ", numNewHWs, " assignments.");
		for (int hwNum = numNewHWs - 1; hwNum >= 0; hwNum--) {
			final Assgt assgt = assgts.get(hwNum);
			final int numGroups = assgt.getNumGroups();
			debugPrint(SELF + "assignment ", hwNum + 1, " has ", numGroups,
					" groups of Qs, getGradingParams(ATTEMPT) = ",
					assgt.getGradingParams(ATTEMPT),
					", getGradingParams(TIME) = ",
					assgt.getGradingParams(TIME));
			for (int grpNum = numGroups; grpNum > 0; grpNum--) {
				final AssgtQGroup qGroup = assgt.getQGroup(grpNum);
				final int numGroupQs = qGroup.getNumQs();
				for (int qNum = numGroupQs; qNum > 0; qNum--) {
					final int hwQId = qGroup.getQId(qNum);
					if (hwQId < 0) { // locally authored
						final String authorId =
								QuestionRW.getAuthorIdByQId(hwQId);
						debugPrint(SELF + "local Q ", grpNum, ".", 
								qNum, " with ID ", hwQId, 
								 (authorId == null ? " not found in DB" 
								 	: " locally authored by " + authorId));
						if (authorId == null // Q not found
								|| !authorId.equals(instructorId)) {
							// local Q not authored by the importer
							debugPrint(SELF + "removing Q from assignment.");
							qGroup.removeQ(qNum);
						} // if Q author != instructorId
					} // if Q is locally authored
				} // for each question in a group
				if (qGroup.isEmpty()) {
					assgt.removeGroup(grpNum);
				} // if no Qs left in group
			} // for each group of questions
			if (assgt.noQGroups()) {
				debugPrint(SELF + "after removing local Qs not belonging to ",
						instructorId, ", no questions in assignment ", 
						hwNum + 1, "; removing.");
				assgts.remove(hwNum);
				numNewHWs--;
			} // if no Qs left in assignment
		} // for each assignment
		debugPrint(SELF, "attempting to add ", numNewHWs, " assignments.");
		HWWrite.addHWs(assgts.toArray(new Assgt[numNewHWs]));
		return numNewHWs;
	} // importHWSets(String, String, int, int)

	/** Clones one or more existing assignments.  Used when a course is being
	 * cloned.
	 * @param	hwIds	unique IDs of the assignments to be cloned
	 * @param	newCrsId	ID number of the new course
	 * @param	addDays	number of days to add to the due dates of the
	 * assignments
	 * @param	makeVisible	true if should make the assignments visible
	 */
	public static void cloneHWSets(int[] hwIds, int newCrsId, int addDays,
			boolean makeVisible) {
		final String SELF = "ExportImportSession.cloneHWSets: ";
		try {
			debugPrint(SELF + "getting assignments ", hwIds, "; newCrsId = ",
					newCrsId, ", addDays = ", addDays, ", makeVisible = ",
					makeVisible);
			final Assgt[] assgts = HWRead.getHWs(hwIds);
			int num = 1;
			for (final Assgt assgt : assgts) {
				assgt.courseId = newCrsId;
				assgt.clearExtensions();
				assgt.setCreationDate();
				assgt.setVisible(makeVisible);
				final Calendar dueCal = Calendar.getInstance();
				dueCal.setTime(assgt.getDueDate());
				dueCal.add(Calendar.DATE, addDays);
				assgt.setDueDate(dueCal.getTime());
				debugPrint(SELF + "assgt ", num++, ": ", assgt.toString());
			} // for each assignment
			HWWrite.addHWs(assgts);
		} catch (DBException e1) {
			Utils.alwaysPrint(SELF
					+ "DBException caught when trying to save assignments.");
			e1.printStackTrace();
		}
	} // cloneHWSets(int[], int, int, boolean)

	/** Disables external instantiation. */
	private ExportImportSession() { }

} // ExportImportSession

