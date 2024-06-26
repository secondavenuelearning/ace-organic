package com.epoch.qBank;

import com.epoch.chem.ChemUtils;
import com.epoch.chem.MolString;
import com.epoch.constants.AppConstants;
import com.epoch.courseware.User;
import com.epoch.evals.CombineExpr;
import com.epoch.exceptions.ParserException;
import com.epoch.qBank.qBankConstants.QDatumConstants;
import com.epoch.qBank.qBankConstants.QuestionConstants;
import com.epoch.substns.substnConstants.SubstnConstants;
import com.epoch.translations.TranslnsMap;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import com.epoch.xmlparser.xmlConstants.XMLConstants;
import com.epoch.xmlparser.XMLUtils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Data used by a question that is not associated with any one evaluator.  
 * In multiple-choice or rank questions, may be a molecule or text; 
 * in synthesis questions, must be rules about permissible starting materials;
 * in R-group questions, must be R-group collections from which ACE 
 * chooses a shortcut group to replace each generic R group in the molecule
 * in Figure 1;
 * various kinds of other question types, is text.
 * Scope: UI, Session, Data.
 * Sessions: QSet, HWSession.
 */
public class QDatum implements AppConstants, SubstnConstants, 
		QDatumConstants, XMLConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Unique ID of this question datum. */ 
	transient public int dataId; 
	/** Unique ID of question to which this question datum belongs. */
	transient public int questionId;
	/** Sequence order in the question.  Will be consecutive. */
	transient public int serialNo;
	/** Type of question datum. */
	transient public int dataType; 
	/** Contains the actual datum. */
	transient public String data; 
	/** Name of the molecule if type is MARVIN. */
	transient public String name; 
	/** ID number used to store translations of data or name. */
	transient public int phraseId; 

	/** Constructor. */
	public QDatum() {
		dataType = TEXT;
		dataId = 0;
		serialNo = 1;
	} // QDatum()

	/** Copy constructor. 
	 * @param	copy	the question datum to be copied
	 */
	public QDatum(QDatum copy) {
		dataId = copy.dataId;
		dataType = copy.dataType;
		serialNo = copy.serialNo;
		questionId = copy.questionId;
		data = copy.data;
		name = copy.name;
		phraseId = copy.phraseId;
	} // QDatum(QDatum)

	/** Gets the XML tag for qData. 
	 * @return	the XML tag for qData
	 */
	public static String getTag() 			{ return QUESTION_DATUM_TAG; }
	/** Convert the unique ID to 0.  Used when cloning a qDatum. */
	public void clearIds() 					{ dataId = 0; }
	/** Gets if this datum is type MARVIN.
	 * @return true if this datum is type MARVIN
	 */
	public boolean isMarvin() 				{ return dataType == MARVIN; }
	/** Gets if this datum is type TEXT.
	 * @return true if this datum is type TEXT
	 */
	public boolean isText() 				{ return dataType == TEXT; }
	/** Gets if this datum is type SUBSTN.
	 * @return true if this datum is type SUBSTN
	 */
	public boolean isSubstitution() 		{ return dataType == SUBSTN; }
	/** Gets if this datum is type RXN_CONDN.
	 * @return true if this datum is type RXN_CONDN
	 */
	public boolean isRxnCondn() 			{ return dataType == RXN_CONDN; }
	/** Gets if this datum is type SYNTH_OK_SM.
	 * @return true if this datum is type SYNTH_OK_SM
	 */
	public boolean isSynOkSM() 				{ return dataType == SYNTH_OK_SM; }
	/** Gets if this datum is type SM_EXPR.
	 * @return true if this datum is type SM_EXPR
	 */
	public boolean isSynSMExpression() 		{ return dataType == SM_EXPR; }

	/** Gets if this datum is a textlike type, including TEXT, RXN_CONDN,
	 * SUBSTN, VAR_VALUES, SYNTH_OK_SM, or SM_EXPR.
	 * @return true if this datum is a textlike type
	 */
	public boolean isTextLike() {
		return isText() || isSubstitution() || isRxnCondn() 
				|| isSynOkSM() || isSynSMExpression();
	} // isTextLike()

	/** Gets if this datum is an unknown type.
	 * @return true if this datum is an unknown type
	 */
	public boolean isUnknownType() {
		return !isMarvin() && !isTextLike();
	} // isUnknownType()

	/** Converts database qDatum value to corresponding internal type value. 
	 * @param	qDataType	database value for type of qDatum
	 * @return	internal value for type of qDatum
	 */
	public int getQdataTypeIntValue(String qDataType) {
		int returnType = UNKNOWN;
		if (qDataType == null) {
			System.out.println("ERROR: null qDataType for qDatum " 
					+ dataId + " with serialNo " + serialNo
					+ " in Q " + questionId);
		} else { 
			returnType = Utils.indexOf(DBVALUES, qDataType);
		} // if qDataType isn't null
		return returnType;
	} // getQdataTypeIntValue(String)

	/** Gets text or Marvin structure name or variable values in display format.
	 * @param	chemFormatting	whether to use chemistry formatting
	 * @return	text or Marvin structure name in display format
	 */
	public String toShortDisplay(boolean chemFormatting) {
		return toShortDisplay(chemFormatting, ADD_CHEM_STRUCT);
	} // toShortDisplay(boolean)

	/** Gets text or Marvin structure name or variable values in display format.
	 * @param	addChemStruct	whether to add [chemical structure] to the name
	 * of structures
	 * @param	chemFormatting	whether to omit chemistry formatting
	 * @return	text or Marvin structure name in display format
	 */
	String toShortDisplay(boolean chemFormatting, boolean addChemStruct) {
		String output = data;
		if (isSubstitution()) {
			final StringBuilder valuesBld = new StringBuilder();
			final String[] values = data.split(SUBSTNS_SEP);
			int valueNum = 1;
			for (final String value : values) {
				if (valueNum++ > 1) valuesBld.append(", ");
				final String[] valueParts = value.split(WORD_VALUE_SEP);
				valuesBld.append(Utils.trim(valueParts[0]));
				if (valueParts.length > 1) {
					Utils.appendTo(valuesBld, " (",
							Utils.trim(valueParts[1]), ')');
				} // if there is word and value
			} // for each value
			output = valuesBld.toString();
		} // if isSubstitution()
		if (chemFormatting) {
			output = Utils.toDisplay(isText() || isSubstitution() ? output 
					: Utils.toString(name, addChemStruct 
						? Utils.toString(" [", CHEM_STRUCT, "]") : ""));
		} // if should format expression
		return output;
	} // toShortDisplay(boolean, boolean)

	/** Gets text or Marvin structure name in display format, with CHEM_STRUCT
	 * translated into the user's language.
	 * @param	chemFormatting	whether to omit chemistry formatting
	 * @param	user	the user
	 * @return	text or Marvin structure name in display format
	 */
	public String toShortDisplay(boolean chemFormatting, User user) {
		return (!chemFormatting ? data
				: Utils.toDisplay(isText() ? data 
					: name + " [" + user.translate(CHEM_STRUCT) + "]"));
	} // toDisplay(boolean, User)

	/** Sets MarvinView display options in the MRV.
	 * @param	optsStr	MarvinView display options, as a String
	 */
	public void setDisplayOptions(String optsStr) {
		data = ChemUtils.setProperty(data, DISPLAY_OPTS, optsStr);
	} // setDisplayOptions(String)

	/** Gets MarvinView display options from the MRV.
	 * @return	MarvinView display options
	 */
	public long getDisplayOptions() {
		return MathUtils.parseLong(ChemUtils.getProperty(data, DISPLAY_OPTS));
	} // getDisplayOptions()

	/** Gets the best applet size for displaying a Marvin drawing.
	 * @return	array of dimensions
	 */
	public int[] getBestAppletSize() {
		return MolString.getBestAppletSize(data, 
				Question.showMapping(getDisplayOptions()));
	} // getBestAppletSize()

	/** Converts the data of this synthesis SM expression from a nested format
	 * to postfix. */
	public void nestedToPostfix() {
		final String SELF = "QDatum.nestedToPostfix: ";
		final String origData = data;
		data = CombineExpr.nestedToPostfix(data);
		debugPrint(SELF + "nested data ", origData, " converted to ", data);
	} // nestedToPostfix()

	/** Gets a Javascript command that will generate a molecule's image
	 * in a Web page, or the SVG or PNG representation of the image.
	 * @param	pathToRoot	path to application root
	 * @param	prefersPNG	prefers PNG over SVG graphics
	 * @param	figNum	serial number of the figure
	 * @return	the image's HTML
	 */
	public String getImage(String pathToRoot, boolean prefersPNG, int figNum) {
		return getImage(pathToRoot, prefersPNG, String.valueOf(figNum));
	} // getImage(String, boolean, int)

	/** Gets a Javascript command that will generate a molecule's image
	 * in a Web page, or the SVG or PNG representation of the image.
	 * @param	pathToRoot	path to application root
	 * @param	prefersPNG	prefers PNG over SVG graphics
	 * @param	figIdStr	unique identifier of the figure on the Web page
	 * @return	the image's HTML
	 */
	public String getImage(String pathToRoot, boolean prefersPNG, 
			String figIdStr) {
		final long flags = getDisplayOptions() | QuestionConstants.SHOWMAPPING;
		return MolString.getImage(pathToRoot, data, flags, figIdStr, 
				prefersPNG);
	} // getImage(String, boolean, String)

	/** Converts the qDatum to XML.  
	 * @param	qDatumTranslns	TranslnsMap for the question datum's 
	 * molecule name (type MARVIN) or data (other types)
	 * @return	 the XML as a one-member list (not just a String in case we
	 * want to add images as a type in the future)
	 */
	public List<String> toXML(TranslnsMap qDatumTranslns) { 
		final List<String> xmlAndImageNames = new ArrayList<String>();
		final StringBuilder translns = new StringBuilder();
		if (qDatumTranslns != null) {
			final List<String> languages = qDatumTranslns.getLanguages();
			for (final String language : languages) {
				if (!ENGLISH.equals(language)) {
					translns.append(makeTranslnNode(language, 
							qDatumTranslns.get(language)));
				} // if language is not English (shouldn't be)
			} // for each translated qDatum
		} // if qDatumTranslns
		final StringBuilder xml = startQDatum();
		if (isMarvin()) {
			Utils.appendTo(xml, makeNode(NAME_TAG, name), translns,
					makeMolNode(data));
		} else {
			Utils.appendTo(xml, makeNode(DATA_TAG, data), translns);
		}
		xml.append(endQDatum());
		xmlAndImageNames.add(0, xml.toString());
		debugPrint("QDatum.toXML: The questiondata xml is:\n", xml);
		return xmlAndImageNames;
	} // toXML()

	/** Wraps a tag and attributes with &lt; &gt;.
	 * @return	the tag and attributes wrapped in &lt; &gt;
	 */
	private StringBuilder startQDatum() {
		return XMLUtils.startTag(QUESTION_DATUM_TAG, new String[][] {
					{DATUM_TYPE_TAG, TYPE_ATTRIBUTES[dataType]},
					{DATUM_ID_TAG, String.valueOf(dataId)},
					{QID_TAG, String.valueOf(questionId)},
					{SERIALNO_TAG, String.valueOf(serialNo)}
				});
	} // startQDatum()

	/** Surround the given text with the given XML tags.
	 * @param	tag	the name of the XML tag
	 * @param	text	the text of the node
	 * @return	the text surrounded by XML tags
	 */
	private StringBuilder makeNode(String tag, String text) {
		return XMLUtils.makeNode(tag, text);
	} // makeNode(String, String)

	/** Surround the given molecule description with the given XML tags.
	 * @param	text	the text of the node
	 * @return	the text surrounded by XML tags
	 */
	private StringBuilder makeMolNode(String text) {
		return XMLUtils.makeNode(DATA_TAG, text, !NEWLINE);
	} // makeNode(String)

	/** Surround a translation with appropriate XML tags.
	 * @param	language	the language
	 * @param	transln	the translation
	 * @return	the translation wrapped in XML tags
	 */
	private StringBuilder makeTranslnNode(String language, String transln) {
		return XMLUtils.makeTranslnNode(language, transln);
	} // makeTranslnNode(String, String)

	/** Wraps a tag with &lt;/ &gt;.
	 * @return	the tag wrapped in &lt;/ &gt;
	 */
	private StringBuilder endQDatum() {
		return XMLUtils.endTag(QUESTION_DATUM_TAG);
	} // endQDatum()

	/** Converts XML in an imported question to a qDatum. 
	 * @param	node	information about the qDatum
	 * @return	the parsed qDatum
	 * @throws	ParserException	if the node can't be parsed
	 */
	public static QDatum parseXML(Node node) throws ParserException {
		final String SELF = "QDatum.parseXML: ";
		debugPrint(" parsing question data  ---------------------" );
		final QDatum qd = new QDatum();
		final NamedNodeMap nodeMap = node.getAttributes();
		String idAttrVal = "0";
		if (nodeMap.getNamedItem(DATUM_ID_TAG) != null) {
			idAttrVal = nodeMap.getNamedItem(DATUM_ID_TAG).getNodeValue();
		}
		try {
			qd.dataId = Integer.parseInt(idAttrVal);
		} catch (NumberFormatException e) {
			throw new ParserException(" invalid data id for question data "
					+ idAttrVal);
		}
		if (nodeMap.getNamedItem(DATUM_TYPE_TAG) == null) {
			throw new ParserException("No type attribute for question data");
		} else {
			final String typeAttrVal = 
					nodeMap.getNamedItem(DATUM_TYPE_TAG).getNodeValue();
			qd.dataType = Utils.indexOf(
					TYPE_ATTRIBUTES, typeAttrVal.toUpperCase(Locale.US));
			if (qd.dataType == -1) {
				throw new ParserException("Invalid type attribute for "
						+ "question data");
			}
		}
		if (nodeMap.getNamedItem(QID_TAG) == null) {
			throw new ParserException("No question id attribute for "
					+ "question data");
		} else {
			final String questionIdAttrVal = 
					nodeMap.getNamedItem(QID_TAG).getNodeValue().trim();
			try {
				qd.questionId = Integer.parseInt(questionIdAttrVal);
			} catch (NumberFormatException e) {
				throw new ParserException(" invalid question id for question "
						+ "data " + questionIdAttrVal);
			}
		}
		if (nodeMap.getNamedItem(SERIALNO_TAG) == null) {
			throw new ParserException("No serial no attribute for question "
					+ "data ");
		} else {
			final String serialNoAttrVal = 
					nodeMap.getNamedItem(SERIALNO_TAG).getNodeValue().trim();
			try {
				qd.serialNo = Integer.parseInt(serialNoAttrVal);
			} catch (NumberFormatException e) {
				throw new ParserException(" invalid serial no. for question "
						+ "data " + serialNoAttrVal);
			}
		}
		final NodeList nodeList = node.getChildNodes();
		for (int nodeNum = 0; nodeNum < nodeList.getLength(); nodeNum++) {
			final Node n = nodeList.item(nodeNum);
			if (n.getNodeName().equalsIgnoreCase(DATA_TAG)) {
				final String data = n.getFirstChild().getNodeValue(); 
				qd.data = (qd.isMarvin() || data == null
						? data : data.trim()); // trim only if not molecule!
				debugPrint(SELF + "got data: ", qd.data);
			} else if (n.getNodeName().equalsIgnoreCase(NAME_TAG)) {
				qd.name = n.getFirstChild().getNodeValue().trim();
			}
		} // for node index 
		debugPrint(" question data parsed ------------ ");
		debugPrint(SELF, qd.toString());
		return qd; 
	} // parseXML()

	/** Extracts question datum translations from XML. 
	 * @param	node	information about the question datum
	 * @return	TranslnsMap for question datum
	 */
	public static TranslnsMap getAllTranslns(Node node) {
		final TranslnsMap translns = new TranslnsMap();
		final NodeList nodeList = node.getChildNodes();
		for (int nodeNum = 0; nodeNum < nodeList.getLength(); nodeNum++) {
			final Node n = nodeList.item(nodeNum);
			if (n.getNodeName().equalsIgnoreCase(TRANSLATION_TAG)) {
				final Node langNode = n.getAttributes().getNamedItem(LANGUAGE_TAG);
				if (langNode != null) {
					final String language = langNode.getNodeValue();
					final Node child = n.getFirstChild();
					final String transln = (child == null ? "" 
							: child.getNodeValue().trim());
					if (language != null && !ENGLISH.equals(language)
							&& !Utils.isEmpty(transln)) {
						translns.put(language, transln);
					} // if we have a good translation
				} // if a language is specified
			} // if the current node is a translation
		} // for node index 
		return translns; 
	} // getAllTranslns(Node)

	/** For debugging. 
	 * @return	String representation of this qDatum 
	 */
	public String toString() {
		final String EQUALS = " = ";
		return Utils.toString(DATUM_ID_TAG + EQUALS, dataId,
				'\n' + DATUM_TYPE_TAG + EQUALS, dataType,
				'\n' + QID_TAG + EQUALS, questionId,
				'\n' + SERIALNO_TAG + EQUALS, serialNo,
				'\n' + DATA_TAG + EQUALS, data,
				'\n' + NAME_TAG + EQUALS, name, '\n');
	} // toString()

} // QDatum
