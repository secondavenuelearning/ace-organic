package com.epoch.qBank;

import chemaxon.struc.Molecule;
import com.epoch.chem.MolString;
import com.epoch.exceptions.ParserException;
import com.epoch.qBank.qBankConstants.FigConstants;
import com.epoch.substns.SubstnUtils;
import com.epoch.synthesis.Synthesis;
import com.epoch.utils.Utils;
import com.epoch.xmlparser.xmlConstants.XMLConstants;
import com.epoch.xmlparser.XMLUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

/** Refers to a single figure (structure, reaction or image)
 * used by a question.
 * Scope: UI, Session, Data.
 * Sessions: QSet, HWSession.
 */
public class Figure implements FigConstants, XMLConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Unique ID of figure. */
	transient public int figureId;
	/** Unique ID of question to which this figure belongs. */
	transient public int questionId;
	/** Sequence order in the question.  Not necessarily consecutive. */
	transient public int serialNo;
	/** Type of figure. */
	transient public int type;
	/** String representation of this Figure, usually a molecule, for all types 
	 * except IMAGE.  */
	transient public String data;
	/** When the question is an R-group question and this figure's molecule
	 * contains R groups, front end populates this field with a student's
	 * particular instantiation of this figure. */
	transient public String instantiatedMolstruct;
	/** For REACTION figures, text above and below the arrow;
	 * for JMOL figures, scripts and Javascript commands. */
	transient public String addlData;
	/** Holds the file location of the image in case of IMAGE. */
	transient public String bufferedImage;

	/** Constructor. */
	public Figure() {
		type = 0;
		serialNo = 0;
	} // Figure()

	/** Copy constructor. 
	 * @param	copy	the figure to be copied
	 */
	public Figure(Figure copy) {
		figureId = copy.figureId;
		serialNo = copy.serialNo;
		type = copy.type;
		data = copy.data;
		addlData = copy.addlData;
		bufferedImage = copy.bufferedImage;
	} // Figure(Figure)

/* ************* short get methods *******************/

	/** Gets if this figure is type REACTION.
	 * @return true if this figure is type REACTION
	 */
	public boolean isReaction() 		{ return type == REACTION; }
	/** Gets if this figure is type MOLECULE.
	 * @return true if this figure is type MOLECULE
	 */
	public boolean isMarvinOnly() 		{ return type == MOLECULE; }
	/** Gets if this figure is type SYNTHESIS.
	 * @return true if this figure is type SYNTHESIS
	 */
	public boolean isSynthesis() 		{ return type == SYNTHESIS; }
	/** Gets if this figure is type LEWIS.
	 * @return true if this figure is type LEWIS
	 */
	public boolean isLewis() 			{ return type == LEWIS; }
	/** Gets if this figure is type IMAGE.
	 * @return true if this figure is type IMAGE
	 */
	public boolean isImage() 			{ return type == IMAGE; }
	/** Gets if this figure is type JMOL.
	 * @return true if this figure is type JMOL
	 */
	public boolean isJmol() 			{ return type == JMOL; }
	/** Gets if this figure is type MRV_TXT.
	 * @return true if this figure is type MRV_TXT
	 */
	public boolean isMRVText() 			{ return type == MRV_TXT; }
	/** Gets if this figure is type IMAGE_AND_VECTORS.
	 * @return true if this figure is type IMAGE_AND_VECTORS
	 */
	public boolean isImageAndVectors() 	{ return type == IMAGE_AND_VECTORS; }
	/** Gets if this figure has an image associated with it.
	 * @return true if this figure is type IMAGE or IMAGE_AND_VECTORS
	 */
	public boolean hasImage() 			{ return isImage() || isImageAndVectors(); }
	/** Gets whether this figure is an IMAGE.
	 * @return	true if this figure is NOT an image
	 */
	public boolean isUsablePreload() 	{ return type != IMAGE; }
	/** Gets the XML tag for figures.
	 * @return	the XML tag for figures
	 */
	public static String getTag() 		{ return FIGURE_TAG; }

	/** Gets if this figure is an unknown type.
	 * @return true if this figure is an unknown type
	 */
	public boolean isUnknownType() {
		return !isMarvinOnly() && !isReaction() && !isSynthesis()
				&& !isLewis() && !isImage() && !isJmol() && !isMRVText()
				&& !isImageAndVectors();
	} // isUnknownType()

	/** Gets if this figure type uses the additional data field
	 * @return true if this figure type uses the additional data field
	 */
	public boolean usesAddlData() {
		return isJmol() || isSynthesis() || isReaction();
	} // usesAddlData()

	/** Converts database figure value to corresponding internal type value.
	 * @param	figureType	database value for type of figure
	 * @return	internal value for type of figure
	 */
	public int getFigureTypeIntValue(String figureType) {
		int returnType = UNKNOWN;
		if (figureType == null) {
			Utils.alwaysPrint("ERROR: null figureType for figure ", figureId, 
					" with serialNo ", serialNo, " in Q ", questionId);
		} else {
			returnType = Utils.indexOf(DBVALUES, figureType);
		} // if figureType is null
		return returnType;
	} // getFigureTypeIntValue(String)

	/** Resets the instantiated molecule to null.  */
	public void resetInstantiatedMol() {
		instantiatedMolstruct = null;
	} // resetInstantiatedMol()

/* ************* display methods *******************/

	/** Gets all the data required to display this figure.
	 * @return array of strings; length and values depend on figure type:
	 * <ul>
	 * <li>MOLECULE: structure only;</li>
	 * <li>REACTION: structure, text above arrow, text below arrow;</li>
	 * <li>LEWIS: MOL value, MRV value for display;</li>
	 * <li>SYNTHESIS: MRV value, HTML describing reaction conditions;</li>
	 * <li>JMOL: structure, Jmol scripts, Jmol Javascript commands;</li>
	 * <li>MRV_TXT: structure(s) only;</li>
	 * <li>IMAGE: image filename;</li>
	 * <li>IMAGE_AND_VECTORS: image filename and vector coordinates.</li>
	 * </ul>
	 */
	public String[] getDisplayData() {
		return getDisplayData(NO_RGROUPS, SYN_PHRASES);
	} // getDisplayData()

	/** Gets all the data required to display this figure with generic R groups
	 * replaced with instantiated R groups.
	 * @param	rGroups	specific R groups used to replace generic R groups in a
	 * figure; may be empty
	 * @return array of strings; length and values depend on figure type:
	 * <ul>
	 * <li>MOLECULE: structure only;</li>
	 * <li>REACTION: structure, text above arrow, text below arrow;</li>
	 * <li>LEWIS: MOL value, MRV value for display;</li>
	 * <li>SYNTHESIS: MRV value, HTML describing reaction conditions;</li>
	 * <li>JMOL: structure, Jmol scripts, Jmol Javascript commands;</li>
	 * <li>MRV_TXT: structure(s) only;</li>
	 * <li>IMAGE: image filename;</li>
	 * <li>IMAGE_AND_VECTORS: image filename and vector coordinates.</li>
	 * </ul>
	 */
	public String[] getDisplayData(Molecule[] rGroups) {
		return getDisplayData(rGroups, SYN_PHRASES);
	} // getDisplayData(String[])

	/** Gets all the data required to display this synthesis figure.
	 * @param	phrases	translations of phrases "Reaction conditions", "ID
	 * numbers", and "No reactions chosen" in user's language
	 * @return array of strings; length and values depend on figure type:
	 * <ul>
	 * <li>MOLECULE: structure only;</li>
	 * <li>REACTION: structure, text above arrow, text below arrow;</li>
	 * <li>LEWIS: MOL value, MRV value for display;</li>
	 * <li>SYNTHESIS: MRV value, HTML describing reaction conditions;</li>
	 * <li>JMOL: structure, Jmol scripts, Jmol Javascript commands;</li>
	 * <li>MRV_TXT: structure(s) only;</li>
	 * <li>IMAGE: image filename;</li>
	 * <li>IMAGE_AND_VECTORS: image filename and vector coordinates.</li>
	 * </ul>
	 */
	public String[] getDisplayData(String[] phrases) {
		return getDisplayData(NO_RGROUPS, phrases);
	} // getDisplayData(String[])

	/** Gets all the data required to display this figure.
	 * @param	rGroups	specific R groups used to replace generic R groups in a
	 * figure; may be empty
	 * @param	phrases	translations of phrases "Reaction conditions", "ID
	 * numbers", and "No reactions chosen" in user's language
	 * @return array of strings; length and values depend on figure type:
	 * <ul>
	 * <li>MOLECULE: structure only;</li>
	 * <li>REACTION: structure, text above arrow, text below arrow;</li>
	 * <li>LEWIS: MRV value for display, MOL value;</li>
	 * <li>SYNTHESIS: MRV value, HTML describing reaction conditions;</li>
	 * <li>JMOL: structure, Jmol scripts, Jmol Javascript commands;</li>
	 * <li>MRV_TXT: structure(s) only;</li>
	 * <li>IMAGE: image filename;</li>
	 * <li>IMAGE_AND_VECTORS: image filename and vector coordinates.</li>
	 * </ul>
	 */
	public String[] getDisplayData(Molecule[] rGroups, String[] phrases) {
		final String[] components = 
				(isReaction() ? getReactionElements() 
				: isJmol() ? getJmolScripts() 
				: null);
		final String[] allData = 
				(isReaction() || isJmol()
						? new String[] {data, components[0], components[1]}
				: isSynthesis() 
						? new String[] {data,
							Synthesis.getRxnsDisplay(data, phrases)}
				: isImage() ? new String[] {bufferedImage}
				: isImageAndVectors() ? new String[] {bufferedImage, data}
				: new String[] {data});
		if (!Utils.isEmpty(rGroups)) {
			if (instantiatedMolstruct == null) {
				instantiatedMolstruct = MolString.toString(
						SubstnUtils.substituteRGroups(allData[STRUCT], 
							rGroups) , MRV);
			} // if need to instantiate instantiatedMolstruct
			allData[STRUCT] = instantiatedMolstruct;
		} // if there are R groups to substitute
		return allData;
	} // getDisplayData(Molecule[], String[])

	/** Gets the text above and below the arrow for REACTION figures.
	 * @return	array containing the text
	 */
	public String[] getReactionElements() {
		String[] components = new String[2];
		Arrays.fill(components, "");
		if (!Utils.isEmpty(addlData)) {
			final String[] texts = addlData.split(RXN_TEXT_SEP);
			if (texts.length > 1) { // separator is present
				components = new String[] {
						texts[0],
						texts[1]};
			} else if (texts.length == 1) { // no separator
				components[0] = texts[0];
			} // if addlData could be split 
		} // if there is additional data
		return components;
	} // getReactionElements()

	/** Returns the Jmol scripts in a JMOL data.
	 * @return	array of Jmol scripts, Jmol Javascript commands
	 */
	public String[] getJmolScripts() {
		final String[] scripts = new String[2];
		Arrays.fill(scripts, "");
		if (!Utils.isEmpty(addlData)) {
			final String[] components = addlData.split(JMOL_SEP);
			if (components.length >= 1) scripts[0] = components[0];
			if (components.length >= 2) scripts[1] = components[1];
		} // if addlData is not null
		return scripts;
	} // getJmolScripts()

	/** Gets a Javascript expression that will generate this figure's image
	 * in a Web page.
	 * @param	pathToRoot	path to application root
	 * @param	prefersPNG	prefers PNG over SVG graphics
	 * @param	qFlags	the question's display flags
	 * @return	the HTML and Javascript expression
	 */
	public String getImage(String pathToRoot, boolean prefersPNG, long qFlags) {
		return getImage(pathToRoot, prefersPNG, qFlags, NO_RGROUP_STRS, 1);
	} // getImage(String, boolean, long)

	/** Gets a Javascript expression that will generate this figure's image
	 * in a Web page.
	 * @param	pathToRoot	path to application root
	 * @param	prefersPNG	prefers PNG over SVG graphics
	 * @param	qFlags	the question's display flags
	 * @param	figNum	serial number of the figure
	 * @return	the HTML and Javascript expression
	 */
	public String getImage(String pathToRoot, boolean prefersPNG, long qFlags,
			int figNum) {
		return getImage(pathToRoot, prefersPNG, qFlags, NO_RGROUP_STRS, figNum);
	} // getImage(String, boolean, long, int)

	/** Gets a Javascript expression that will generate a molecule's image
	 * in a Web page.
	 * @param	pathToRoot	path to application root
	 * @param	prefersPNG	prefers PNG over SVG graphics
	 * @param	qFlags	the question's display flags
	 * @param	figIdStr	unique identifier of the figure on the Web page
	 * @return	the HTML and Javascript expression
	 */
	public String getImage(String pathToRoot, boolean prefersPNG, long qFlags, 
			String figIdStr) {
		return getImage(pathToRoot, prefersPNG, qFlags, NO_RGROUP_STRS, figIdStr);
	} // getImage(String, boolean, long, String)

	/** Gets a Javascript expression that will generate this figure's image
	 * in a Web page.
	 * @param	pathToRoot	path to application root
	 * @param	prefersPNG	prefers PNG over SVG graphics
	 * @param	qFlags	the question's display flags
	 * @param	rGroupStrs	the names of the R groups used to construct an
	 * instantiated molecule
	 * @param	figNum	serial number of the figure
	 * @return	the HTML and Javascript expression
	 */
	public String getImage(String pathToRoot, boolean prefersPNG, long qFlags, 
			String[] rGroupStrs, int figNum) {
		return getImage(pathToRoot, prefersPNG, qFlags, rGroupStrs, 
				String.valueOf(figNum));
	} // getImage(String, boolean, long, String[], int)

	/** Gets a Javascript expression that will generate a molecule's image
	 * in a Web page.
	 * @param	pathToRoot	path to application root
	 * @param	prefersPNG	prefers PNG over SVG graphics
	 * @param	qFlags	the question's display flags
	 * @param	rGroupStrs	the names of the R groups used to construct an
	 * instantiated molecule
	 * @param	figIdStr	unique identifier of the figure on the Web page
	 * @return	the HTML and Javascript expression
	 */
	public String getImage(String pathToRoot, boolean prefersPNG, long qFlags, 
			String[] rGroupStrs, String figIdStr) {
		final String SELF = "Figure.getImage: ";
		// final boolean isLewis = isLewis();
		final String mrvStr = (!Utils.isEmpty(rGroupStrs) 
				? instantiatedMolstruct : data);
		debugPrint(SELF, mrvStr);
		return MolString.getImage(pathToRoot, mrvStr, qFlags, figIdStr, 
				prefersPNG);
	} // getImage(String, boolean, long, String[], String)

/* ************* import/export methods *******************/

	/** Converts the figure to XML, or stores filename if an IMAGE.
	 * @return	 the XML if not an IMAGE, or filenames if it is
	 */
	public List<String> toXML() {
		final List<String> xmlAndImageNames = new ArrayList<String>();
		final StringBuilder xml = Utils.getBuilder(startFigure());
		if (isImage() || isImageAndVectors()) {
			xml.append(makeNode(IMAGE_FILE_TAG, bufferedImage));
			xmlAndImageNames.add(bufferedImage);	
			if (isImageAndVectors() && !Utils.isEmpty(data)) {
				xml.append(makeNode(ADDL_DATA_TAG, data));
			} // if has additional data 
		} else {
			xml.append(makeMolNode(data));
			if ((isReaction() || isJmol()) && !Utils.isEmpty(addlData)) {
				xml.append(makeNode(ADDL_DATA_TAG, addlData));
			} // if has additional data 
		} // if type
		xml.append(endFigure());
		xmlAndImageNames.add(0, xml.toString());
		return xmlAndImageNames;
	} // toXML()

	/** Wraps the opening tag and its id and type attributes with &lt; &gt;.
	 * @return	the tag and its id and type attributes wrapped in &lt; &gt;
	 */
	private StringBuilder startFigure() {
		return XMLUtils.startTag(FIGURE_TAG, new String[][] {
					{"id", String.valueOf(serialNo)},
					{"type", TYPE_ATTRIBUTES[type]}
				});
	} // startFigure()

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
		return XMLUtils.makeNode(MOLSTRUCT_TAG, text, !NEWLINE);
	} // makeNode(String)

	/** Wraps the closing tag with &lt;/ &gt;.
	 * @return	the tag wrapped in &lt;/ &gt;
	 */
	private StringBuilder endFigure() {
		return XMLUtils.endTag(FIGURE_TAG);
	} // endFigure()

	/** Converts XML in an imported question to a Figure.
	 * @param	node	information about the figure
	 * @return	the parsed figure
	 * @throws	ParserException	if the node can't be parsed
	 */
	public static Figure parseXML(Node node) throws ParserException {
		debugPrint(" parsing figure  ---------------------" );
		final Figure figure = new Figure();
		// get the id and type attributes
		final NamedNodeMap nodeMap = node.getAttributes();
		String serialNoAttrVal = "0";
		if (nodeMap.getNamedItem("id") != null) {
			serialNoAttrVal = nodeMap.getNamedItem("id").getNodeValue();
		}
		try {
			figure.serialNo = Integer.parseInt(serialNoAttrVal);
		} catch (NumberFormatException e) {
			throw new ParserException(" invalid serialNo for figure "
					+ figure.serialNo);
		}
		if (nodeMap.getNamedItem("type") == null) {
			throw new ParserException("No type attribute for figure");
		} else {
			final String typeAttrVal = nodeMap.getNamedItem("type")
					.getNodeValue().toUpperCase(Locale.US);
			figure.type = Utils.indexOf(TYPE_ATTRIBUTES, typeAttrVal);
			if (figure.type == -1) {
				// account for obsolete figure type from ACE 3.0 and earlier
				if ("MECHANISM".equals(typeAttrVal)) figure.type = MOLECULE;
				else throw new ParserException("Invalid type attribute for figure");	
			} // if type is invalid
		}
		debugPrint("Found figure ", figure.serialNo, " of type ", 
				TYPE_ATTRIBUTES[figure.type]);
		final NodeList nodeList = node.getChildNodes();
		for (int nodeNum = 0; nodeNum < nodeList.getLength(); nodeNum++) {
			final Node n = nodeList.item(nodeNum);
			if (n.getNodeType() == Node.TEXT_NODE) continue;
			if (n.getNodeName().equalsIgnoreCase(MOLSTRUCT_TAG)
					&& n.getFirstChild() != null) {
				figure.data = n.getFirstChild().getNodeValue(); // do not trim!
				debugPrint("Figure.parseXML: data:\n", figure.data);
			} else if (n.getNodeName().equalsIgnoreCase(ADDL_DATA_TAG)
					&& n.getFirstChild() != null) {
				final String addlData = n.getFirstChild().getNodeValue();
				if (addlData != null) figure.addlData = addlData.trim();
			} else if (n.getNodeName().equalsIgnoreCase(IMAGE_FILE_TAG)) {
				figure.bufferedImage = n.getFirstChild().getNodeValue().trim();
			}
		} // for each node
		debugPrint(" figure parsed ------------ ");
		return figure;
	} // parseXML()

	/** For debugging.
	 * @return	String representation of this figure
	 */
	public String toString() {
		return Utils.toString("serialNo = ", serialNo,
				"\ntype = ", type, " (", TYPE_ATTRIBUTES[type],
				")\ndata = ", data, "\nbuffered image = ", bufferedImage);
	} // toString

} // Figure
