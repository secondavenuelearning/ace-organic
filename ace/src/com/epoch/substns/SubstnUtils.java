package com.epoch.substns;

import chemaxon.formats.MolImporter;
import chemaxon.formats.MolFormatException;
import chemaxon.struc.CTransform3D;
import chemaxon.struc.DPoint3;
import chemaxon.struc.Molecule;
import chemaxon.struc.SelectionMolecule;
import chemaxon.struc.graphics.MPolyline;
import com.epoch.chem.ChemUtils;
import com.epoch.chem.MolString;
import com.epoch.db.RGroupCollectionRW;
import com.epoch.exceptions.DBException;
import com.epoch.qBank.QDatum;
import com.epoch.substns.substnConstants.SubstnConstants;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Contains methods that select the shortcut groups that will substitute
 * for generic R groups in the molecule of Figure 1 in a particular student's
 * view of an R-group question, and that replace those generic R groups with
 * the shortcut groups; also contains methods that select the values that will
 * substitute for variables in the question statement or NumberIs evaluator 
 * of a particular student's view of a numeric question.
 */
final public class SubstnUtils implements SubstnConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	private static void debugPrintMRV(Object... msg) {
		// Utils.printToLog(msg, ChemUtils.MRV);
	}

	/** From each set of R-group collections or numerical values, choose one 
	 * shortcut group to substitute each generic R group in a 
	 * Figure, or one value to substitute each variable in a numeric question 
	 * statement.
	 * @param	substnStrs	array of text QDatum corresponding to each
	 * generic R group or variable, each containing a colon-separated list of 
	 * either all the collections from which each R group may be selected or 
	 * all the values that may be selected for that variable
	 * @param	isNumeric	if the question is numeric, so we are choosing
	 * numerical values
	 * @return	array of shortcut groups or values, each to replace a 
	 * generic R group or variable
	 */
	public static String[] chooseSubstnValues(QDatum[] substnStrs, 
			boolean isNumeric) {
		final String SELF = "SubstnUtils.chooseSubstnValues: ";
		final List<String> chosenSubstns = new ArrayList<String>();
		final Random rgen = new Random();
		int genericROrValueNum = 0;
		final char varName = (isNumeric ? 'x' : 'R');
		for (final QDatum substnStr : substnStrs) {
			genericROrValueNum++;
			try {
				final String[] valuesOrRGroupCollIDs = 
						substnStr.data.split(SUBSTNS_SEP);
				final String[] substnOpts = 
						(isNumeric ? valuesOrRGroupCollIDs
						: RGroupCollectionRW.getRGroups(valuesOrRGroupCollIDs));
				final int substnNum = rgen.nextInt(substnOpts.length);
				final String substn = Utils.trim(substnOpts[substnNum]);
				chosenSubstns.add(substn);
				debugPrint(SELF + "chose ",
						isNumeric ? "value " : "R group ", 
						substn, " for ", varName, genericROrValueNum, 
						" from among ", substnOpts, 
						isNumeric ? "" : Utils.toString(
							" that are in collections with IDs [", 
							Utils.join(valuesOrRGroupCollIDs), "]"));
			} catch (DBException e) {
				final String defaultValue = (isNumeric ? "0" : "Me");
				Utils.alwaysPrint(SELF + "caught DBException; "
						+ "setting ", varName, genericROrValueNum, " to ", 
						defaultValue);
				e.printStackTrace();
				chosenSubstns.add(defaultValue);
			} // try
		} // for each substnStr
		debugPrint(SELF + "selected ", isNumeric ? "value" : "R group", 
				"(s) ", chosenSubstns);
		return chosenSubstns.toArray(
				new String[chosenSubstns.size()]);
	} // chooseSubstnValues(QDatum[], boolean)

	/** Replace generic R groups in a molecule with shortcut groups.
	 * @param	mrvDef	an MRV representation of a molecule
	 * @param	substSGroups	array of shortcut groups as strings
	 * @return	a Molecule in which the generic R groups are replaced with
	 * the specified shortcut groups
	 */
	public static Molecule substituteRGroups(String mrvDef,
			String[] substSGroups) {
		return substituteRGroups(mrvDef, substSGroups, !ADD_H_ATOMS);
	} // substituteRGroups(String, String[])
	
	/** Replace generic R groups in a molecule with shortcut groups.
	 * @param	mrvDef	an MRV representation of a molecule
	 * @param	substSGroups	array of shortcut groups as strings
	 * @param	addHAtoms	whether to add explicit H atoms to the shortcut
	 * groups
	 * @return	a Molecule in which the generic R groups are replaced with
	 * the specified shortcut groups
	 */
	public static Molecule substituteRGroups(String mrvDef,
			String[] substSGroups, boolean addHAtoms) {
		return substituteRGroups(mrvDef, getRGroupMols(substSGroups), addHAtoms);
	} // substituteRGroups(String, String[], boolean)
	
	/** Replace generic R groups in a molecule with shortcut groups.
	 * @param	mrvDef	an MRV representation of a molecule
	 * @param	rgMols	array of the shortcut groups as Molecules
	 * @return	a Molecule in which the generic R groups are replaced with
	 * the specified shortcut groups
	 */
	public static Molecule substituteRGroups(String mrvDef,
			Molecule[] rgMols) {
		return substituteRGroups(mrvDef, rgMols, !ADD_H_ATOMS);
	} // substituteRGroups(String, Molecule[])

	/** Replace generic R groups in a molecule with shortcut groups.
	 * @param	mrvDef	an MRV representation of a molecule
	 * @param	rgMols	array of the shortcut groups as Molecules
	 * @param	addHAtoms	whether to add explicit H atoms to the shortcut
	 * groups
	 * @return	a Molecule in which the generic R groups are replaced with
	 * the specified shortcut groups
	 */
	public static Molecule substituteRGroups(String mrvDef,
			Molecule[] rgMols, boolean addHAtoms) {
		Molecule authorMol = new Molecule();
		try {
			authorMol = MolImporter.importMol(mrvDef);
			substituteRGroups(authorMol, rgMols, addHAtoms);
		} catch (MolFormatException e) {
			Utils.alwaysPrint("SubstnUtils.substituteRGroups: "
					+ "MolFormatException: ", e.getMessage());
		}
		return authorMol;
	} // substituteRGroups(String, Molecule[], boolean)

	/** Replace generic R groups in a molecule with shortcut groups.  Modifies
	 * the original!
	 * @param	authorMol	a molecule
	 * @param	rgMols	array of the shortcut groups as Molecules
	 */
	public static void substituteRGroups(Molecule authorMol,
			Molecule[] rgMols) {
		substituteRGroups(authorMol, rgMols, !ADD_H_ATOMS);
	} // substituteRGroups(Molecule, Molecule[])

	/** Replace generic R groups in a molecule with shortcut groups.  Modifies
	 * the original!
	 * @param	authorMol	a molecule
	 * @param	rgMols	array of the shortcut groups as Molecules
	 * @param	addHAtoms	whether to add explicit H atoms to the shortcut
	 * groups
	 */
	public static void substituteRGroups(Molecule authorMol,
			Molecule[] rgMols, boolean addHAtoms) {
		final String SELF = "SubstnUtils.substituteRGroups: ";
		final RGroupReplacer replacer = 
				new RGroupReplacer(authorMol, rgMols, addHAtoms);
		final MPolyline rxnArrow = MolString.getReactionArrow(authorMol);
		if (rxnArrow == null) replacer.replaceRGroups();
		else {
			final SelectionMolecule[] fragMols =
					authorMol.findBasicFrags(SelectionMolecule.class);
			debugPrint(SELF + "we have a reaction arrow and ",
					fragMols.length, " fragment molecule(s).");
			debugPrintMRV(SELF + "molecule is:\n", authorMol);
			int fragMolNum = 0;
			for (final SelectionMolecule fragMol : fragMols) {
				fragMolNum++;
				final DPoint3 oldCtr = fragMol.calcOutRectCenter();
				final DPoint3 oldDims = fragMol.calcOutRect();
				debugPrint(SELF + "before substituting R groups, fragment ", 
						fragMolNum, " with formula ", fragMol, " has center ", 
						oldCtr, " and dimensions ", oldDims);
				if (replacer.replaceRGroups(fragMol)) {
					debugPrintMRV(SELF + "after substituting R groups and 2D "
							+ "cleaning of fragment ", fragMolNum, 
							" with formula ", fragMol, " but before translation, "
							+ "whole molecule is:\n", authorMol);
					final DPoint3 translnVector = getTranslationVector(
							fragMol, oldCtr, oldDims, rxnArrow);
					debugPrint(SELF + "translation vector is ", 
							translnVector);
					final CTransform3D moveFragMol = new CTransform3D();
					moveFragMol.setTranslation(translnVector);
					fragMol.transform(moveFragMol);
					debugPrint(SELF + "after moving fragment, center "
							+ "of fragment ", fragMolNum, " is ", 
							fragMol.calcOutRectCenter(), ", dimensions are ", 
							fragMol.calcOutRect());
					debugPrintMRV(SELF + "after substituting some R groups "
							+ "in fragment ", fragMolNum, " and moving the "
							+ "fragment, molecule is:\n", authorMol);
				} else debugPrint(SELF + "no R groups in fragment ", fragMolNum,
						"; no need to move fragment.");
			} // for each fragment
		} // if there is a reaction arrow in this document
	} // substituteRGroups(Molecule, Molecule[], boolean)

	/** Gets the translation vector to put a 2D-cleaned fragment with
	 * instantiated R groups back to its original distance from a reaction arrow
	 * (which is assumed to be horizontal or close to it and pointing to right).
	 * @param	fragMol	the fragment
	 * @param	oldCtr	former position of the center of the fragment
	 * @param	oldDims	former dimensions of the fragment
	 * @param	rxnArrow	the reaction arrow
	 * @return	a translation vector
	 */
	private static DPoint3 getTranslationVector(SelectionMolecule fragMol, 
			DPoint3 oldCtr, DPoint3 oldDims, MPolyline rxnArrow) {
		final String SELF = "SubstnUtils.getTranslationVector: ";
		final DPoint3 arrowHeadLocn = 
				rxnArrow.getPoint(MPolyline.HEAD).getLocation();
		final DPoint3 arrowTailLocn = 
				rxnArrow.getPoint(MPolyline.TAIL).getLocation();
		final DPoint3 newCtr = fragMol.calcOutRectCenter();
		final DPoint3 newDims = fragMol.calcOutRect();
		debugPrint(SELF + "reaction arrow has left and right x values of ", 
				arrowTailLocn.x, " and ", arrowHeadLocn.x, 
				"; fragment's new center is ", newCtr, 
				", new dimensions are ", newDims);
		final boolean isLeftOfArrow = oldCtr.x < arrowTailLocn.x;
		final boolean isRightOfArrow = oldCtr.x > arrowHeadLocn.x;
		return (isLeftOfArrow || isRightOfArrow
				? getTranslationVector(oldCtr, oldDims, newCtr, newDims, 
						isLeftOfArrow)
				: getTranslationVector(oldCtr, oldDims, newCtr, newDims, 
						(arrowHeadLocn.y + arrowTailLocn.y) / 2.0));
	} // getTranslationVector(SelectionMolecule, DPoint3, DPoint3, MPolyline)

	/** Gets the translation vector to put a 2D-cleaned fragment with
	 * instantiated R groups that used to be before or after a reaction arrow
	 * back to its original distance from the reaction arrow
	 * (which is assumed to be horizontal or close to it and pointing to right).
	 * @param	oldCtr	former position of the center of the fragment
	 * @param	oldDims	former dimensions of the fragment
	 * @param	newCtr	new position of the center of the fragment
	 * @param	newDims	new dimensions of the fragment
	 * @param	isLeftOfArrow true if fragment began left of the arrow
	 * @return	a translation vector
	 */
	private static DPoint3 getTranslationVector(DPoint3 oldCtr, DPoint3 oldDims, 
			DPoint3 newCtr, DPoint3 newDims, boolean isLeftOfArrow) {
		final String SELF = "SubstnUtils.getTranslationVector: ";
		final double dimFactor = (isLeftOfArrow ? 0.5 : -0.5);
		final double oldEdgeX = oldCtr.x + oldDims.x * dimFactor;
		final double newEdgeX = newCtr.x + newDims.x * dimFactor;
		debugPrint(SELF + "fragment was ", isLeftOfArrow ? "left" : "right", 
				" of reaction arrow; former ", isLeftOfArrow ? "right" : "left", 
				"most x coord is ", oldEdgeX, ", new one is ", newEdgeX);
		return new DPoint3(
				oldEdgeX - newEdgeX, // edge goes back to original x position 
				oldCtr.y - newCtr.y, // center goes back to original y and z positions
				oldCtr.z - newCtr.z);
	} // getTranslationVector(DPoint3, DPoint3, DPoint3, DPoint3, boolean)

	/** Gets the translation vector to put a 2D-cleaned fragment with
	 * instantiated R groups that used to be above or below a reaction arrow
	 * back to its original distance from the reaction arrow
	 * (which is assumed to be horizontal or close to it and pointing to right).
	 * @param	oldCtr	former position of the center of the fragment
	 * @param	oldDims	former dimensions of the fragment
	 * @param	newCtr	new position of the center of the fragment
	 * @param	newDims	new dimensions of the fragment
	 * @param	arrowY	average Y coordinate of the arrow
	 * @return	a translation vector
	 */
	private static DPoint3 getTranslationVector(DPoint3 oldCtr, DPoint3 oldDims, 
			DPoint3 newCtr, DPoint3 newDims, double arrowY) {
		final String SELF = "SubstnUtils.getTranslationVector: ";
		// Note: Y values decrease as you move south
		final boolean isAboveArrow = oldCtr.y > arrowY;
		final double dimFactor = (isAboveArrow ? -0.5 : 0.5);
		final double oldEdgeY = oldCtr.y + oldDims.y * dimFactor;
		final double newEdgeY = newCtr.y + newDims.y * dimFactor;
		debugPrint(SELF + "fragment was ", isAboveArrow ? "above" : "below", 
				" of reaction arrow; former ", isAboveArrow ? "bottom" : "top", 
				"most y coord is ", oldEdgeY, ", new one is ", newEdgeY);
		return new DPoint3(
				oldCtr.x - newCtr.x, // center goes back to original x and z positions
				oldEdgeY - newEdgeY, // edge goes back to original y position 
				oldCtr.z - newCtr.z);
	} // getTranslationVector(DPoint3, DPoint3, DPoint3, DPoint3, double)

	/** Gets Molecule versions of shortcut groups.
	 * @param	rGroupStrs	array of shortcut group names
	 * @return	array of Molecule versions of the shortcut groups, with attachment
	 * points stored in each Molecule
	 */
	public static Molecule[] getRGroupMols(String[] rGroupStrs) {
		if (Utils.isEmpty(rGroupStrs)) return null;
		final String SELF = "SubstnUtils.getRGroupMols: ";
		debugPrint(SELF + "rGroupStrs = ", rGroupStrs);
		final List<Molecule> rGroupMolsArr = new ArrayList<Molecule>();
		for (final String rGroupStr : rGroupStrs) {
			try {
				final Molecule rGroupMol = 
						ChemUtils.getSGroupMolecule(rGroupStr);
				if (rGroupMol != null) {
					rGroupMolsArr.add(rGroupMol);
				} // if we found an OK R group
			} catch (MolFormatException e) {
				Utils.alwaysPrint(SELF + "unable to import shortcut group ", 
						rGroupStr, "; MolFormatException: ", e.getMessage());
			} // try
		} // for each R group
		debugPrint(SELF + "rGroupMols = ", rGroupMolsArr);
		return Utils.molListToArray(rGroupMolsArr);
	} // getRGroupMols(String[])

	/** Converts an array of R groups to a string for display in HTML.
	 * @param	rGroups	array of R groups
	 * @return	HTML string
	 */
	public static String displayRGroups(String[] rGroups) {
		final StringBuilder rGroupsOut = new StringBuilder();
		if (!Utils.isEmpty(rGroups)) {
			rGroupsOut.append("<tr><td style=\"text-align:left;\">");
			int rgNum = 1;
			for (final String rGroup : rGroups) {
				if (rgNum > 1) rGroupsOut.append("; ");
				Utils.appendTo(rGroupsOut, Utils.toDisplay("R" + rgNum, 
						Utils.SUPERSCRIPT_RGROUP_NUMS), " = ", Utils.toDisplay(
						rGroup.replaceAll("([stompin])-", "<i>$1</i>-")));
				rgNum++;
			} // for each R group
			rGroupsOut.append("</td></tr>");
		} // if Q has R groups 
		return rGroupsOut.toString();
	} // displayRGroups(String[])

	/** Substitutes values into a string with [[x1]], etc.
	 * @param	str	the string
	 * @param	values	array of values to substitute for variables in the 
	 * statement
	 * @param	numeric	if the values are in the format word = value, substitute
	 * the value
	 * @return	statement of this question for display
	 */
	public static String substituteValues(String str, String[] values, 
			boolean numeric) {
		final String SELF = "SubstnUtils.substituteValues: ";
		String modStr = Utils.toString(str, " ");
		int valueNum = 1;
		for (final String valueRaw : values) {
			String value = valueRaw;
			final String[] valueParts = valueRaw.split(WORD_VALUE_SEP);
			if (valueParts.length > 1) {
				value = Utils.trim(valueParts[numeric ? 1 : 0]);
			} // if the value is both word and value
			final String variable = Utils.toString("\\[\\[x", valueNum++, "]]");
			final String[] strParts = modStr.split(variable);
			debugPrint(SELF + "after splitting at ", variable,
					", modStr = ", strParts);
			final StringBuilder modStrBld = new StringBuilder();
			int partNum = 1;
			final int maxPartNum = strParts.length;
			for (final String part : strParts) {
				modStrBld.append(part);
				if (partNum++ < maxPartNum) {
					modStrBld.append(value);
				} // if this part of the statement isn't the last
			} // for each part of the statement
			modStr = modStrBld.toString();
			debugPrint(SELF + "after reuniting, modStr = ", modStr);
		} // for each raw value
		debugPrint(SELF + "with values ", values, ", converted:\n",
				str, "\nto:\n", modStr);
		return Utils.trim(modStr);
	} // substituteValues(String, String[], boolean)

	/** Converts an array of chosen values to a string for display in HTML.
	 * @param	values	array of values
	 * @return	HTML string
	 */
	public static String displayValues(String[] values) {
		final StringBuilder valuesBld = new StringBuilder();
		if (!Utils.isEmpty(values)) {
			valuesBld.append("<tr><td style=\"text-align:left;\">");
			int rgNum = 1;
			for (final String value : values) {
				if (rgNum > 1) valuesBld.append("; ");
				final String[] valueParts = value.split(WORD_VALUE_SEP);
				Utils.appendTo(valuesBld, "<i>x<sub>", rgNum++, 
						"</sub></i> = ", valueParts[0]);
			} // for each value
			valuesBld.append("</td></tr>");
		} // if Q has values 
		return valuesBld.toString();
	} // displayValues(String[])

	/** Constructor, disabled to prevent instantiation. */
	private SubstnUtils() {
		// empty
	}

} // SubstnUtils
