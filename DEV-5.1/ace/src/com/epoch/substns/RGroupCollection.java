package com.epoch.substns;

import com.epoch.db.RGroupCollectionRW;
import com.epoch.utils.Utils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Defines an R-group collection used by a Marvin question.
 * An R-group collection is a small collection of shortcut groups such as
 * Me, Et, Pr, etc. that share a characteristic such as having 1-4 C atoms,
 * being aromatic, etc.  The shortcut groups' definitions are stored in RGroupDefs.
 * Authors of R-group questions choose one or more collections from which ACE
 * may choose shortcut groups to substitute for the generic R groups in Figure 1
 * of the R-group question; see RGroupUtils.
 * Scope: UI, Session, Data.
 */
public class RGroupCollection {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Unique ID of the R-group collection. */
	transient public int id = 0;
	/** Name of the R-group collection. */
	transient public String name;
	/** Shortcut groups in the R-group collection. */
	transient public String[] rGroups;

	/** Constructor. */
	public RGroupCollection() {
		// intentionally empty
	} // RGroupCollection()

	/** Constructor.
	 * @param	rgColId	ID number of the R-group collection
	 * @param	rgColName	name of the R-group collection
	 * @param	rGrps	R groups in the collection
	 */
	public RGroupCollection(int rgColId, String rgColName, String[] rGrps) {
		id = rgColId;
		name = rgColName;
		rGroups = rGrps;
	} // RGroupCollection(int, String, String[])

	/** Copy constructor. 
	 * @param	copy	the R-group collection to be copied
	 */
	public RGroupCollection(RGroupCollection copy) {
		id = copy.id;
		name = copy.name;
		rGroups = Utils.getCopy(copy.rGroups);
	} // RGroupCollection(RGroupCollection)

	/** Saves an R-group collection to the database.  */
	public void setRGroupCollection() {
		try {
			RGroupCollectionRW.setRGroupCollection(this);
		} catch (Exception e) {
			Utils.alwaysPrint("RGroupCollection: exception thrown when "
					+ "writing R group to db.");
			Utils.alwaysPrint(e.getMessage());
		}
	} // setRGroupCollection()

	/** Gets an R-group collection from the database.
	 * @param	rGroupColNum	ID number of the R-group collection to acquire
	 * @return	the requested R-group collection
	 */
	public static RGroupCollection getRGroupCollection(int rGroupColNum) {
		RGroupCollection rGroupCol = new RGroupCollection();
		try {
			rGroupCol = RGroupCollectionRW.getRGroupCollection(
					rGroupColNum);
		} catch (Exception e) {
			Utils.alwaysPrint("RGroupCollection.getRGroupCollection: "
					+ "Exception: ", e.getMessage());
		}
		return rGroupCol;
	} // getRGroupCollection(int)

	/** Gets names of all R-group collections from the database.
	 * @return	array of names of all R-group collections
	 */
	public static String[] getAllRGroupCollectionNames() {
		String[] rGroupCols = null;
		try {
			rGroupCols =
					RGroupCollectionRW.getAllRGroupCollectionNames();
		} catch (Exception e) {
			Utils.alwaysPrint("RGroupCollection.getAllRGroupCollectionNames: "
					+ "Exception: ", e.getMessage());
		}
		if (Utils.isEmpty(rGroupCols))
			rGroupCols = new String[] { "none" };
		return rGroupCols;
	} // getAllRGroupCollectionNames()

	/** Gets IDs of all R-group collections from the database in the same order
	 * as the names.
	 * @return	array of IDs of all R-group collections
	 */
	public static int[] getAllRGroupCollectionIds() {
		int[] rGroupColIds = null;
		try {
			rGroupColIds =
					RGroupCollectionRW.getAllRGroupCollectionIds();
		} catch (Exception e) {
			Utils.alwaysPrint("RGroupCollection.getAllRGroupCollectionNames: "
					+ "Exception: ", e.getMessage());
		}
		if (rGroupColIds == null) // never got any
			rGroupColIds = new int[] { };
		return rGroupColIds;
	} // getAllRGroupCollectionIds()

	/** Gets IDs of all R-group collections from the database in the order
	 * of alphabetized names.
	 * @param	rGroupColNamesByIds	map of the names of the R-group collections,
	 * keyed by IDs
	 * @return	array of IDs of all R-group collections in alphabetical order of
	 * their corresponding names
	 */
	public static int[] getAllRGroupCollectionIdsAlphabetized(
			Map<Integer, String> rGroupColNamesByIds) {
		final int[] rGroupColIds = getAllRGroupCollectionIds();
		String[] rGroupCols;
		if (rGroupColIds.length == 0) {
			Utils.alwaysPrint("getAllRGroupCollectionIdsAlphabetized" +
				" no group col ids?");
			rGroupCols = new String[0];
		} else {
			rGroupCols = new String[rGroupColIds.length - 1];
			// ignore Jlint complaint about line above.  Raphael 11/2010
		}
		for (int rgNum = 0; rgNum < rGroupColIds.length - 1; rgNum++)
			rGroupCols[rgNum] =
					rGroupColNamesByIds.get(rGroupColIds[rgNum + 1])
							+ "***" + rGroupColIds[rgNum + 1];
		Arrays.sort(rGroupCols);
		int[] alphabetizedRGroupCollectionIds = new int[rGroupColIds.length];
		alphabetizedRGroupCollectionIds[0] = rGroupColIds[0];
		for (int rgNum = 0; rgNum < rGroupColIds.length - 1; rgNum++) {
			final String numberStr = rGroupCols[rgNum].
					substring(rGroupCols[rgNum].indexOf("***") + 3);
			alphabetizedRGroupCollectionIds[rgNum + 1] =
					Integer.parseInt(numberStr);
		}
		return alphabetizedRGroupCollectionIds;
	} // getAllRGroupCollectionIdsAlphabetized(Map<Integer, String>)

	/** Gets names of all R-group collections keyed by their IDs.
	 * @return	Map of names of all R-group collections keyed by their IDs
	 */
	public static Map<Integer, String> getRGroupCollectionNamesKeyedByIds() {
		Map<Integer, String> namesByIds = new HashMap<Integer, String>();
		try {
			namesByIds = RGroupCollectionRW.
					getAllRGroupCollectionNamesKeyedById();
		} catch (Exception e) {
			Utils.alwaysPrint("RGroupCollection."
					+ "getAllRGroupCollectionNamesKeyedById: "
					+ "Exception: ", e.getMessage());
		}
		return namesByIds;
	} // getRGroupCollectionNamesKeyedByIds()

	/** Gets definitions of all R-group collections keyed by their IDs.
	 * @return	Map of definitions of all R-group collections keyed
	 * by their IDs
	 */
	public static Map<Integer, String[]> getRGroupCollectionDefsKeyedByIds() {
		Map<Integer, String[]> defsByIds = new HashMap<Integer, String[]>();
		try {
			defsByIds = RGroupCollectionRW.
					getAllRGroupCollectionDefsKeyedById();
		} catch (Exception e) {
			Utils.alwaysPrint("RGroupCollection."
					+ "getAllRGroupCollectionDefsKeyedById: "
					+ "Exception: ", e.getMessage());
		}
		return defsByIds;
	} // getRGroupCollectionDefsKeyedByIds()

} // RGroupCollection
