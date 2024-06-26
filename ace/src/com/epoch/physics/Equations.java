package com.epoch.physics;

import com.epoch.exceptions.EquationFormatException;
import com.epoch.exceptions.ParameterException;
import com.epoch.exceptions.ProcessExecutionException;
import com.epoch.physics.physicsConstants.CanonicalizedUnitConstants;
import com.epoch.physics.physicsConstants.EquationsConstants;
import com.epoch.xmlparser.XMLUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Holds a response to an equations question. */
public class Equations 
		implements CanonicalizedUnitConstants, EquationsConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** An array of entries, each consisting of an expression or one or more 
	 * comma-separated equations. */
	transient private String[] entries = null;
	/** Comma-separated equations assigning numerical values to variables. */
	transient private String constants = null;
	/** When true, disable the cell containing the constants. */
	transient private boolean disableConstantsField = false;
	/** Names of variables that should not be considered to be units, as a
	 * string. */
	transient private String variablesNotUnitsStr = null;
	/** Names of variables that should not be considered to be units, as a list,
	 * including names of constants. */
	transient private String[] variablesNotUnits = null;
	/** The entries in TeX format. */
	transient private String[] texEntries = null;
	/** Stores the result of calculating whether the entries follow logically.  
	 * First member, entry property, can be UNCALCULATED, DOESNT_FOLLOW, 
	 * INCOMPARABLE, or FORMAT_EXCEPTION.  Second member is the 1-based number
	 * of the entry that is misformatted, is incomparable to the previous
	 * entry, or doesn't follow from the previous entry.
	 */
	transient private int[] storedEntryPropertyAndNum = 
			new int[] {UNCALCULATED, UNCALCULATED};

	/* **************** Constructors ****************/

	/** Constructor. 
	 */
	public Equations() {
		// empty
	} // Equations()

	/** Constructor. 
	 * @param	response	a response in XML format. Must have ", &amp;, 
	 * &lt;, and &gt; encoded as &amp;quot;, &amp;amp;, &amp;lt;, and 
	 * &amp;gt;, and non-ASCII characters encoded as numerical CERs
	 * @throws	ParameterException	if cannot parse the response
	 */
	public Equations(String response) throws ParameterException {
		final String SELF = "Equations: ";
		debugPrint(SELF + "response:\n", response);
		boolean success = true;
		if (Utils.isEmpty(response)) entries = new String[0];
		else success = 
				parseXML(XMLUtils.xmlToNode(Utils.cersToUnicode(response)));
		if (!success) {
			throw new ParameterException(SELF + "could not parse XML:\n"
					+ response);
		} // if couldn't parse XML
		debugPrint(SELF + "entries = ", entries);
	} // Equations(String)

	/** Converts a node derived from XML into the array of entry strings
	 * @param	node	node derived from XML describing the equatins
	 * @return	true if a node was found
	 */
	private boolean parseXML(Node node) {
		final String SELF = "Equations.parseXML: ";
		boolean success = false;
		final List<String> entriesList = new ArrayList<String>();
		if (node == null) {
			debugPrint(SELF + "node is null, returning false.");
		} else if (node.getNodeName().equalsIgnoreCase(XML_TAG)) {
			success = true;
			if (node.hasChildNodes()) {
				final NodeList children = node.getChildNodes();
				final int numChildren = children.getLength();
				debugPrint(SELF + "XML node has ", numChildren, 
						" child(ren).");
				for (int childNum = 0; childNum < numChildren; childNum++) {
					final Node child = children.item(childNum);
					final String nodeName = child.getNodeName();
					if (nodeName.equalsIgnoreCase(EQUATION_TAG)) {
						debugPrint(SELF + "found entry node.");
						final String entry = parseEntry(child);
						if (entry != null) {
							entriesList.add(entry.trim());
						} // if have an entry
					} else if (nodeName.equalsIgnoreCase(CONSTANTS_TAG)) {
						debugPrint(SELF + "found constants node.");
						constants = parseEntry(child);
						if (child.hasAttributes()) {
							final NamedNodeMap attributes = 
									child.getAttributes();
							final Node disableAttr = 
									attributes.getNamedItem(DISABLED_ATTR_TAG);
							if (disableAttr != null) {
								disableConstantsField = Utils.isPositive(
										disableAttr.getNodeValue());
							} // if attribute has value 
						} // if there are attributes
					} else if (nodeName.equalsIgnoreCase(VARS_NOT_UNITS_TAG)) {
						variablesNotUnitsStr = parseEntry(child);
						debugPrint(SELF + "variablesNotUnitsStr = ",
								variablesNotUnitsStr);
					} // if node is an entry or the constants
				} // for each child node
			} // if there are child nodes
		} else debugPrint(SELF + "node ", node.getNodeName(),
				" is unknown, returning false.");
		entries = entriesList.toArray(new String[entriesList.size()]);
		return success;
	} // parseXML(Node)

	/** Parses a node representing a entry.
	 * @param	node	the node representing the entry
	 * @return	an expression or one or more comma-separated equations
	 */
	private String parseEntry(Node node) {
		final String SELF = "Equations.parseEntry: ";
		final NodeList children = node.getChildNodes();
		final int numChildren = children.getLength();
		debugPrint(SELF + "entry node has ", numChildren, " child(ren).");
		final StringBuilder eqnBld = new StringBuilder();
		for (int childNum = 0; childNum < numChildren; childNum++) {
			eqnBld.append(children.item(childNum).getNodeValue());
		} // for each child node
		return eqnBld.toString();
	} // parseEntry(Node)

	/* **************** Get/set methods ****************/

	/** Gets the nature of the stored entry from a previous calculation of
	 * the logical sequence of equations.  
	 * @return	entry property: UNCALCULATED, DOESNT_FOLLOW, INCOMPARABLE, or
	 * FORMAT_EXCEPTION
	 */
	public int getStoredEntryProperty()	{ return storedEntryPropertyAndNum[PROPERTY]; }
	/** Gets the 1-based number of the stored entry, the first entry in
	 * the sequence that doesn't follow from the previous, is incomparable to
	 * the previous, or has a bad format.
	 * @return	1-based entry number
	 */
	public int getStoredEntryNum()		{ return storedEntryPropertyAndNum[EQN_NUMBER]; }
	/** Sets the 1-based number of the stored entry, the first entry in
	 * the sequence that doesn't follow from the previous, is incomparable to
	 * the previous, or has a bad format, and which of these properties it has.
	 * @param	prop	entry property
	 * @param	eqnNum	1-based entry number 
	 */
	public void storeEntryPropertyAndNum(int prop, int eqnNum) { 
		storedEntryPropertyAndNum = new int[] {prop, eqnNum}; 
	} // storeEntryPropertyAndNum(int, int)

	/** Gets the entries.
	 * @return	the entries
	 */
	public String[] getEntries() 			{ return entries; }
	/** Gets the last entry.
	 * @return	the last entry, or null if there are none
	 */
	public String getLastEntry()			{ return getEntry(entries.length); }
	/** Gets the constants.
	 * @return	the constants
	 */
	public String getConstants()			{ return constants; }
	/** Gets whether the constants field is disabled.
	 * @return	true if the constants field is disabled
	 */
	public boolean getDisableConstants()	{ return disableConstantsField; }

	/** Gets the variable names that will not be considered units, with all
	 * Unicode converted to CERs.
	 * @return	string containing variable names that will not be considered
	 * units
	 */
	public String getVariablesNotUnitsStr()	{ 
		return Utils.unicodeToCERs(variablesNotUnitsStr); 
	} // getVariablesNotUnitsStr()

	/** Gets an entry.
	 * @param	entryNum	1-based number of the entry
	 * @return	the entry, or null if number is out of range
	 */
	public String getEntry(int entryNum) {
		return (entryNum >= 1 && entryNum <= entries.length 
				? entries[entryNum - 1] : null);
	} // getEntry(int)

	/** Gets the entries formatted for Maxima, with implicit multiplication made
	 * explicit and &micro; and &mu; converted to u.  
	 * @return	the entries formatted for Maxima
	 */
	public String[] getFormattedEntries() { 
		final List<String> formattedEntries = new ArrayList<String>();
		for (final String entry : entries) {
			formattedEntries.add(formatEntry(entry));
		} // for each entry
		return formattedEntries.toArray(new String[formattedEntries.size()]);
	} // getFormattedEntries()

	/** Gets an entry formatted for Maxima, with implicit multiplication made
	 * explicit and &micro; and &mu; converted to u.  
	 * @param	entryNum	1-based number of the entry
	 * @return	the entry formatted for Maxima, or null if number is out of 
	 * range
	 */
	public String getFormattedEntry(int entryNum) {
		return formatEntry(getEntry(entryNum));
	} // getEntry(int)

	/** Gets an entry formatted for Maxima, with implicit multiplication made
	 * explicit and &micro; and &mu; converted to u.  
	 * @param	entryNum	1-based number of the entry
	 * @param	canonicaliznExtent	to what extent to convert units to their 
	 * canonicalized form
	 * @return	the entry formatted for Maxima, or null if number is out of 
	 * range
	 */
	public String getFormattedEntry(int entryNum, int canonicaliznExtent) {
		return formatEntry(getEntry(entryNum), canonicaliznExtent);
	} // getEntry(int, int)

	/** Gets the last entry formatted for Maxima, with implicit multiplication 
	 * made explicit and &micro; and &mu; converted to u.  
	 * @return	the last entry, formatted for Maxima
	 */
	public String getFormattedLastEntry() {
		return formatEntry(getLastEntry());
	} // getFormattedLastEntry()

	/** Gets the last entry formatted for Maxima, with implicit multiplication 
	 * made explicit and &micro; and &mu; converted to u.  
	 * @param	canonicaliznExtent	to what extent to convert units to their 
	 * canonicalized form
	 * @return	the last entry, formatted for Maxima
	 */
	public String getFormattedLastEntry(int canonicaliznExtent) {
		return formatEntry(getLastEntry(), canonicaliznExtent);
	} // getFormattedLastEntry(int)

	/** Formats the entry for Maxima, with implicit multiplication made
	 * explicit and &micro; and &mu; converted to u.  
	 * @param	entry	an entry
	 * @return	the entry formatted for Maxima
	 */
	private String formatEntry(String entry) {
		return formatEntry(entry, FULL_CANONICALZN);
	} // formatEntry(String)

	/** Formats the entry for Maxima, with implicit multiplication made
	 * explicit and &micro; and &mu; converted to u.  
	 * @param	entry	an entry
	 * @param	canonicaliznExtent	to what extent to convert units to their 
	 * canonicalized form
	 * @return	the entry formatted for Maxima
	 */
	private String formatEntry(String entry, int canonicaliznExtent) {
		return EquationFunctions.formatExpression(entry, 
				getVariablesNotUnits(), canonicaliznExtent);
	} // formatEntry(String, int)

	/** Gets the constants formatted for Maxima, with implicit multiplication made
	 * explicit, &micro; and &mu; converted to u, units canonicalized, 
	 * = converted to :, and , converted to ;. 
	 * @return	the constants formatted for Maxima
	 */
	public String getFormattedConstants() { 
		if (Utils.isEmpty(constants)) return null;
		final String constantsStr = 
				constants.replace(':', '=').replace(',', ';');
		final String[] constantsEqns = constantsStr.split(";");
		final StringBuilder bld = new StringBuilder();
		for (final String constantsEqn : constantsEqns) {
			if (bld.length() > 0) bld.append("; ");
			bld.append(formatEntry(constantsEqn).replace('=', ':'));
		} // for each constant
		return bld.toString();
	} // getFormattedConstants()

	/** Gets an array of names of variables that will not be considered to be 
	 * units, including the constants.  (We assume that constants' names are 
	 * on the left side of each equation.)
	 * @return	array of names of variables that will not be considered to be
	 * units
	 */
	public String[] getVariablesNotUnits() { 
		final String SELF = "Equations.getVariablesNotUnits: ";
		if (variablesNotUnits == null) {
			final List<String> variablesNotUnitsList = new ArrayList<String>();
			if (!Utils.isEmpty(variablesNotUnitsStr)) {
				final String varNamesStr = 
						variablesNotUnitsStr.replace(',', ';');
				final String[] varNamesArr = varNamesStr.split(";");
				for (final String varName : varNamesArr) {
					variablesNotUnitsList.add(varName.trim());
				} // for each reserved variable
				final String MICRO = String.valueOf((char) 181);
				final String MU = String.valueOf((char) 956);
				if (variablesNotUnitsList.contains(MICRO) 
						&& !variablesNotUnitsList.contains(MU)) {
					variablesNotUnitsList.add(MU);
				} else if (variablesNotUnitsList.contains(MU) 
						&& !variablesNotUnitsList.contains(MICRO)) {
					variablesNotUnitsList.add(MICRO);
				} // if contains micro and not mu or vice versa
			} // if there are reserved variables
			final int numFromList = variablesNotUnitsList.size();
			if (!Utils.isEmpty(constants)) {
				final String constantsStr = 
						constants.replace(':', '=').replace(',', ';');
				final String[] constantsEqns = 
						Utils.splitTrim(constantsStr, ";");
				for (final String constantEqn : constantsEqns) {
					final String[] eqnHalves = constantEqn.split("=");
					if (eqnHalves.length > 1) {
						final String constant = eqnHalves[0].trim();
						if (!variablesNotUnitsList.contains(constant)) {
							variablesNotUnitsList.add(constant);
						} // if constant is not already in list
					} // for each constant name
				} // for each constant equation
			} // if there are constants
			debugPrint(SELF + "reserved names of variables not to be treated "
					+ "as units (", numFromList, " from author-specified list, "
					+ "the rest from constants field): ", 
					variablesNotUnitsList); 
			variablesNotUnits = variablesNotUnitsList.toArray(
					new String[variablesNotUnitsList.size()]);
		} // if variablesNotUnits hasn't been initialized
		return variablesNotUnits;
	} // getVariablesNotUnits()

	/** Gets the list of entries as a single string in TeX for MathJax format.
	 * @return	the list of entries formatted in TeX for MathJax
	 */
	public String getEntriesForMathJax() {
		final StringBuilder bld = new StringBuilder();
		if (!Utils.isEmpty(constants)) {
			Utils.appendTo(bld, "$$", constants.replace(':', '='), "$$");
		} // if there are constants
		final String[] texEqns = getEqnsArrayForMathJax();
		for (final String texEqn : texEqns) {
			Utils.appendTo(bld, "\\begin{equation}\n", texEqn, 
					"\n\\end{equation}\n");
		} // for each entry
		return bld.toString();
	} // getEntriesForMathJaxDisplay()

	/** Gets the array of entries in TeX for MathJax format.
	 * @return	array of entries formatted in TeX for MathJax
	 */
	public String[] getEqnsArrayForMathJax() {
		if (texEntries == null) {
			final List<String> texEqns = new ArrayList<String>();
			for (final String entry : entries) {
				texEqns.add(EquationFunctions.toTeX(entry));
			} // for each entry
			texEntries = texEqns.toArray(new String[texEqns.size()]);
		} // if they haven't yet been converted
		return texEntries;
	} // getEqnsArrayForMathJax()

	/* **************** Calculations ****************/

	/** Checks the validity of each constant definition and entry.
	 * @throws	EquationFormatException	if an entry is malformatted
	 * @throws	ProcessExecutionException	if the process can't be executed
	 * properly
	 */
	public void checkForValidity() 
			throws EquationFormatException, ProcessExecutionException {
		if (!Utils.isEmpty(constants)) {
			final String constantsStr = constants.replace(':', '=');
			final String[] constantEqns = constantsStr.split("[;,]");
			for (final String constantEqn : constantEqns) {
				EquationFunctions.isValidExpression(constantEqn, SAVE_EQN);
			} // for each constant definition
		} // if there are constants
		if (!Utils.isEmpty(entries)) for (final String entry : entries) {
			EquationFunctions.isValidExpression(entry, SAVE_EQN);
		} // for each entry
	} // checkForValidity()

} // Equations
