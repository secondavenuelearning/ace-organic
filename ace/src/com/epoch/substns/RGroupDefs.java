package com.epoch.substns;

import com.epoch.exceptions.GroupDefsException;
import com.epoch.utils.Utils;
import java.util.HashMap;
import java.util.Map;

/** Singleton class that holds names and definitions of shortcut groups that may
 * substitute for generic R groups in Figure 1 of R-group questions.
 * Scope: loaded by the GroupLoader servlet at the time of app deployment.
 */
public final class RGroupDefs {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Holds the group name (e.g., Ph) keying for the group SMILES definition
	 * and attachment point. */
	private static Map<String, String[]> allGroups =
			new HashMap<String, String[]>();
	/** Number of data in the array value in allGroups. */
	private static final int NUMDATA = 2;
		/** Member of an R-group definition.  */
		public static final int DEF = 0;
		/** Member of an R-group definition.  */
		public static final int ATTACH = 1;
		
	/** Gets all the R groups and their definitions.
	 * @return	table of R group definitions (array of strings) keyed by group
	 * name
	 */
	public static Map<String, String[]> getAllGroups() {
		return allGroups;
	}

	/** Resets the table to 0 members before rereading from the text file. */
	public static void reset() {
		allGroups.clear();
	} // reset()

	/** Gets the number of R groups.
	 * @return the number of R groups
	 */
	public static int getGroupsCount() {
		return allGroups.size();
	}
	
	/** Gets an R-group definition.
	 * @param	shortName	name of R group (e.g., Ph)
	 * @return	array of strings containing the SMILES definition and the
	 * attachment point
	 */
	public static String[] getGroup(String shortName) {
		final String[] features = allGroups.get(shortName);
		if (features == null)
			debugPrint("RGroupDefs.getInformation: "
					+ "no such group as '", shortName, "'.");
		return features;
	} // getGroup(String)
	
	/** Gets the SMILES definition or the attachment point of an R group.
	 * @param	shortName	name of R group (e.g., Ph)
	 * @param	selector	DEF or ATTACH
	 * @return	the SMILES definition or the attachment point
	 */
	private static String getInformation(String shortName, int selector) {
		final String[] features = allGroups.get(shortName);
		if (features == null)
			Utils.alwaysPrint("RGroupDefs.getInformation: "
					+ "no such group as '", shortName, "'.");
		return (features == null ? null : features[selector]);
	} // getInformation(String, int)

	/** Gets the SMILES definition of an R group.
	 * @param	shortName	name of R group (e.g., Ph)
	 * @return	the SMILES definition
	 */
	public static String getGroupDefinition(String shortName) {
		return getInformation(shortName, DEF);
	}

	/** Gets the attachment point of an R group.
	 * @param	shortName	name of R group (e.g., Ph)
	 * @return	the attachment point
	 */
	public static String getGroupAttachPoint(String shortName) {
		return getInformation(shortName, ATTACH);
	}
	
	/** Adds an R group to the list.
	 * This method is called by servlets/GroupLoader.java each time the code is
	 * recompiled; it does not need to be called from anywhere else.
	 * @param	shortName	name of R group (e.g., Ph)
	 * @param	smiles	SMILES definition of R group
	 * @param	attachPoint	attachment point of R group
	 * @throws	GroupDefsException	if any parameter is null or empty
	 */
	public static void addGroup(String shortName, String smiles,
			String attachPoint) throws GroupDefsException {
		if (Utils.isEmptyOrWhitespace(shortName) 
				|| Utils.isEmptyOrWhitespace(smiles) 
				|| Utils.isEmptyOrWhitespace(attachPoint)) {
			throw new GroupDefsException(
					"Invalid parameters in R group definition");
		} // if a parameter is null or empty
		String[] features = allGroups.get(shortName);
		if (features != null) {
			debugPrint("RGroupDefs.addGroup: group ",
					shortName, " with definition ",
					features[DEF], " and attachment point ",
					features[ATTACH], ", changing to definition ",
					smiles, " and attachment point ", attachPoint);
			allGroups.remove(shortName);
		} // if the group is already present
		features = new String[NUMDATA];
		features[DEF] = smiles;
		features[ATTACH] = attachPoint;
		allGroups.put(shortName, features);
	} // addGroup(String, String, String)	

	/** Debug output. */
	public static void print() {
		Utils.alwaysPrint("R group defs, size = ", getGroupsCount());
		// for (final String name: allGroups.keySet()) {
		for (final Map.Entry <String,String[]> entry : allGroups.entrySet())
		{
		// 	final String[] features = allGroups.get(name);
			final String[] features = entry.getValue();
			Utils.alwaysPrint("===============================");
			Utils.alwaysPrint(entry.getKey(), ": ", features[DEF], ", ",
				features[ATTACH]);
			Utils.alwaysPrint("===============================");
		} // for each table element
	} // print()
	
	/** Disables external instantiation. */
	private RGroupDefs() { }

} // RGroupDefs
