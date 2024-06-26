package com.epoch.energyDiagrams;

import com.epoch.energyDiagrams.diagramConstants.OEDCellConstants;
import com.epoch.exceptions.ParameterException;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** A cell in an orbital energy diagram.  May contain an array of orbitals.  */
public class OEDCell extends DiagramCell implements OEDCellConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Array of orbitals contained in the cell. */
	private Orbital[] orbitals = new Orbital[0];

	/** Constructor.
	 * @param	r	row
	 * @param	c	column
	 */
	public OEDCell(int r, int c) {
		row = r;
		col = c;
	} // OEDCell(int, int)

	/** Constructor.
	 * @param	r	row
	 * @param	c	column
	 * @param	data	String in format 4;2:0:1;3 
	 * representing orbitals of type 4 with occupancies 2, 0, and 1 
	 * labeled with 1-based label 3 (no label is 0).
	 */
	public OEDCell(int r, int c, String data) {
		row = r;
		col = c;
		setContents(data);
	} // OEDCell(int, int, String)

	/** Sets the contents of this table cell.
	 * @param	data	String in format 4;2:0:1;3 
	 * representing orbitals of type 4 with occupancies 2, 0, and 1 
	 * labeled with 1-based label 3 (no label is 0).
	 */
	public final void setContents(String data) {
		if (!Utils.isEmpty(data) && data.contains(CELL_CONTENTS_SEP)) {
			final String[] orbsStr = data.split(CELL_CONTENTS_SEP);
			final int orbType = MathUtils.parseInt(orbsStr[ORBS_TYPE]);
			final String[] occups = orbsStr[OCCUPS].split(OCCUP_SEP);
			setOrbitals(orbType, occups);
			if (orbsStr.length >= LABEL + 1) {
				setLabel(MathUtils.parseInt(orbsStr[LABEL]));
			} // if there is a label
		} // if there are data
	} // setContents(String)

	/** Sets the contents of this table cell from a node derived from XML.
	 * @param	cellNode	node derived from XML describing this cell
	 */
	void parseXML(Node cellNode) {
		final String SELF = "OEDCell.parseXML: ";
		final NodeList contentNodes = cellNode.getChildNodes();
		final int numContentNodes = contentNodes.getLength();
		int orbType = Orbital.UNKNOWN;
		List<String> occups = new ArrayList<String>();
		for (int endNum = 0; endNum < numContentNodes; endNum++) {
			final Node contentNode = contentNodes.item(endNum);
			if (contentNode.getNodeName().equalsIgnoreCase(ORBS_TYPE_TAG)
					&& contentNode.hasAttributes()) {
				final NamedNodeMap attributes = contentNode.getAttributes();
				if (attributes.getNamedItem(ORBS_TYPE_TAG) != null) {
					orbType = MathUtils.parseInt(
							attributes.getNamedItem(ORBS_TYPE_TAG).getNodeValue());
				} // if there's an orbital type attribute
			} else if (contentNode.getNodeName().equalsIgnoreCase(OCCUPS_TAG)) {
				occups = parseOccupancies(contentNode);
			} else if (contentNode.getNodeType() != Node.TEXT_NODE) {
				Utils.alwaysPrint(SELF + "unknown node ",
						contentNode.getNodeName());
			} // if node name or type
		} // for each content node
		setOrbitals(orbType, occups);
		debugPrint(SELF + "returning ", toString());
	} // parseXML(Node, DiagramCell)

	/** Gets the occupancies of the orbitals described by this node. 
	 * @param	contentNode	node containing the occupancies
	 * @return	list of occupancies as strings
	 */
	private static List<String> parseOccupancies(Node contentNode) {
		final String SELF = "OEDCell.parseOccupancies: ";
		final List<String> occups = new ArrayList<String>();
		final NodeList occupNodes = contentNode.getChildNodes();
		final int numOccupNodes = occupNodes.getLength();
		for (int occupNum = 0; occupNum < numOccupNodes; occupNum++) {
			final Node occupNode = occupNodes.item(occupNum);
			final NamedNodeMap attributes = occupNode.getAttributes();
			if (occupNode.hasAttributes()
					&& attributes.getNamedItem(OCCUP_TAG) != null) {
				final String occup = 
						attributes.getNamedItem(OCCUP_TAG).getNodeValue();
				occups.add(occup);
			} else if (occupNode.getNodeType() != Node.TEXT_NODE) {
				Utils.alwaysPrint(SELF + "unknown node ",
						occupNode.getNodeName());
			} // if attribute or node name or type
		} // for each occupancy
		return occups;
	} // parseOccupancies(Node) 

	/** Sets the orbitals of this cell.
	 * @param	orbType	type of orbital
	 * @param	occups	occupancies of the orbitals
	 */
	private void setOrbitals(int orbType, String[] occups) {
		final List<Orbital> orbs = new ArrayList<Orbital>();
		for (final String occup : occups) {
			try {
				orbs.add(new Orbital(orbType, MathUtils.parseInt(occup)));
			} catch (ParameterException e) {
				Utils.alwaysPrint("OEDCell.setOrbitals: "
						+ "bad orbital data ", orbType, ", ", occup);
			} // try
		} // for each orbital
		orbitals = orbs.toArray(new Orbital[orbs.size()]);
	} // setOrbitals(int, String[])

	/** Sets the orbitals of this cell.
	 * @param	orbType	type of orbital
	 * @param	occups	occupancies of the orbitals
	 */
	private void setOrbitals(int orbType, List<String> occups) {
		setOrbitals(orbType, occups.toArray(new String[occups.size()]));
	} // setOrbitals(int, List<String>)

/* ***************** Get methods ***********************/

	/** Returns whether this cell has orbitals.
	 * @return	true if the cell has orbitals
	 */
	public boolean hasOrbitals() {
		return getNumOrbitals() > 0;
	} // hasOrbitals()

	/** Returns the number of orbitals in this cell.
	 * @return	true if the cell has orbitals
	 */
	public int getNumOrbitals() {
		return getOrbitals().length;
	} // getNumOrbitals()

	/** Returns the orbitals in this cell.
	 * @return	array of Orbitals
	 */
	public Orbital[] getOrbitals() {
		return orbitals;
	} // getOrbitals()

	/** Returns the type of this cell's orbitals.
	 * @return	type of the cell's orbitals
	 */
	public int getOrbitalsType() {
		return (hasOrbitals() ? orbitals[0].getType() : Orbital.UNKNOWN);
	} // getOrbitalsType()

	/** Returns the name of this cell's orbitals.
	 * @return	name of the cell's orbitals
	 */
	public String getOrbitalsName() {
		return (hasOrbitals() ? orbitals[0].getDisplayName() 
				: Orbital.getDisplayName(Orbital.UNKNOWN));
	} // getOrbitalsName()

	/** Returns the occupancy of one of this cell's orbitals.
	 * @param	orbNum	1-based orbital number
	 * @throws	ParameterException	if orbNum is out of range
	 * @return	occupancy of the orbital
	 */
	public int getOccupancy(int orbNum) throws ParameterException {
		if (!MathUtils.inRange(orbNum, new int[] {1, getNumOrbitals()})) {
			throw new ParameterException(
					"OEDCell.getOccupancy: orbital number " 
					+ orbNum + " out of range 1-" + getNumOrbitals() + ".");
		} // if orbNum out of range
		return orbitals[orbNum - 1].getOccupancy();
	} // getOccupancy(int)

	/** Returns the occupancies of orbitals in this cell as a
	 * colon-separated string.
	 * @return	colon-separated string of occupancies
	 */
	public String getOccupancies() {
		return getOccupancies(!SORT);
	} // getOccupancies()

	/** Returns the occupancies of orbitals in this cell as a
	 * colon-separated string.
	 * @param	sort	whether to sort the orbitals by occupancy
	 * @return	colon-separated string of occupancies
	 */
	public String getOccupancies(boolean sort) {
		if (Utils.isEmpty(orbitals)) return "";
		int[] occups = new int[orbitals.length];
		int oNum = 0;
		for (final Orbital orb : orbitals) occups[oNum++] = orb.getOccupancy();
		if (sort) Arrays.sort(occups);
		return Utils.join(occups, OCCUP_SEP);
	} // getOccupancies(boolean)

	/** Returns the total electron count of this cell.
	 * @return	total electron count of this cell
	 */
	public int getNumElectrons() {
		int count = 0;
		for (final Orbital orb : orbitals) count += orb.getOccupancy();
		return count;
	} // getNumElectrons()

/* ***************** comparison and string methods ***********************/

	/** Compares this cell to another one.  They are the same if they have the
	 * same number of the same types of orbitals with the same occupancies.
	 * @param	theOther	the cell to compare
	 * @return	true if they have the same number of the same types of orbitals
	 * with the same occupancies
	 */
	@Override
	public boolean equals(Object theOther) {
		final String SELF = "OEDCell.equals: ";
		boolean isEqual = false;
		if (theOther instanceof OEDCell) {
			final OEDCell cell = (OEDCell) theOther;
			isEqual = getNumOrbitals() == cell.getNumOrbitals();
			if (isEqual) {
				final String orbsStr = toString(SORT);
				final String cellOrbsStr = cell.toString(SORT);
				debugPrint(SELF, orbsStr, " and ", cellOrbsStr,
						" have the same number of orbitals.");
				isEqual = orbsStr.equals(cellOrbsStr);
			} // if orbitals' type and occupancies should be compared
		} // if instanceof
		return isEqual;
	} // equals(Object)

	/** Creates a hash code describing this object.
	 * @return the hash code
	 */
	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37).
			append(getNumOrbitals()).append(toString(SORT)).toHashCode();
	} // hashCode

	/** Converts the orbitals' types and occupancies into a String.
	 * @return	the string encoding the orbitals' types and occupancies
	 */
	public String toString() {
		return toString(!SORT);
	} // toString()

	/** Converts the orbitals' types, occupancies, label into a String.
	 * @param	sort	whether to sort the orbitals by type and occupancy
	 * @return	the string encoding the orbitals' types, occupancies, label
	 */
	public String toString(boolean sort) {
		return (Utils.isEmpty(orbitals) ? ""
				: Utils.toString(getOrbitalsType(), CELL_CONTENTS_SEP, 
					getOccupancies(sort), CELL_CONTENTS_SEP, getLabel()));
	} // toString(boolean)

} // OEDCell

