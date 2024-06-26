package com.epoch.chem;

import com.epoch.db.FnalGroupDefRW;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.ParameterException;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Defines a functional group.  */
public class FnalGroupDef {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Unique ID of this functional group. */
	transient public int groupId = 0;
	/** Name of this functional group. */
	transient public String name;
	/** SMILES definition of this functional group. */
	transient public String definition;
	/** Category of this functional group. */
	transient public String category;

	/** Used to acquire specific information from database.  */
	public static final int NAMES = 1;
	/** Used to acquire specific information from database.  */
	public static final int CATEGORIES = 2;
	/** Used to acquire specific information from database.  */
	public static final int LC_NAMES = 3;
	/** Parameter for tweakName().  */
	public static final int DISPLAY = 1;
	/** Parameter for tweakName().  */
	public static final int PULLDOWN = 2;
	/** Parameter for getAllGroupNames().  */
	public static final boolean MAKE_LOWER = true;

	/** String used to separate group names from other information when
	 * alphabetizing. */
	private static String DIVIDER = "\t";

	/** Constructor. */
	public FnalGroupDef() {
		// intentionally empty
	}

	/** Constructor used to create a new functional group for storage in
	 * the database.
	 * @param	grpName	name of the functional group
	 * @param	def	SMILES definition of the functional group
	 * @param	grpCategory	category of the functional group
	*/
	public FnalGroupDef(String grpName, String def, String grpCategory) {
		name = grpName;
		definition = def;
		category = grpCategory;
	} // FnalGroupDef(String, String, String)

	/** Constructor used to create a modified version of an existing functional
	 * group for storage in the database.
	 * @param	id	ID number of the functional group
	 * @param	grpName	name of the functional group
	 * @param	def	SMILES definition of the functional group
	 * @param	grpCategory	category of the functional group
	 */
	public FnalGroupDef(int id, String grpName, String def, String grpCategory) {
		groupId = id;
		name = grpName;
		definition = def;
		category = grpCategory;
	} // FnalGroupDef(int, String, String, String)

	/** Copy constructor. 
	 * @param	copy	the definition to be copied
	 */
	public FnalGroupDef(FnalGroupDef copy) {
		groupId = copy.groupId;
		name = copy.name;
		definition = copy.definition;
		category = copy.category;
	} // FnalGroupDef(FnalGroupDef)

	/** Acquires a functional group from the database.
	 * @param	id	ID number of the functional group
	 * @return	a functional group
	 * @throws	ParameterException	if the id doesn't match a functional group
	 * @throws	DBException	if there's a problem reading from the database
	 */
	public static FnalGroupDef getFnalGroupDef(int id)
			throws ParameterException, DBException {
		if (id == 0) return new FnalGroupDef();
		else {
			return FnalGroupDefRW.getFnalGroupDef(id);
		}
	} // getFnalGroupDef(int)

	/** Acquires a functional group from the database.
	 * @param	grpName	name of the functional group
	 * @return	a functional group
	 * @throws	ParameterException	if the id doesn't match a functional group
	 * @throws	DBException	if there's a problem reading from the database
	 */
	public static FnalGroupDef getFnalGroupDef(String grpName)
			throws ParameterException, DBException {
		return FnalGroupDefRW.getFnalGroupDef(grpName);
	} // getFnalGroupDef(String)

	/** Saves (adds or sets) this functional group to the database.
	 * @throws	DBException	when the write operation doesn't work
	 */
	public void saveFnalGroupDef() throws DBException {
		FnalGroupDefRW.saveFnalGroupDef(this);
	} // saveFnalGroupDef()

	/** Gets the name for display: first letter (not counting prefix) is
	 * lower-case.
	 * @return	display name of the functional group
	 */
	public String getDisplayName() {
		return tweakName(DISPLAY);
	} // getDisplayName()

	/** Gets the name for display in pulldown menu: first letter (not
	 * counting prefix) is lower-case.
	 * @return	name of the functional group modified for a pulldown
	 */
	public String getPulldownName() {
		return tweakName(PULLDOWN);
	} // getPulldownName()

	/** Modifies a functional group name for displaying in HTML or pulldown menu.
	 * @param	type	PULLDOWN or DISPLAY
	 * @return	functional group name modified for display as HTML or in a
	 * pulldown menu
	 */
	private String tweakName(int type) {
		String tweakedName;
		String[] parseName = name.split("-");
		if (parseName.length > 1) { // name contains a hyphen
			// change case of initial letter of posthyphen name
			parseName[1] = (type == PULLDOWN
						? parseName[1].substring(0, 1).toUpperCase(Locale.US)
						: parseName[1].substring(0, 1).toLowerCase(Locale.US)) 
					+ parseName[1].substring(1);
			// capitalize prehyphen name if it is one character
			String locant = (parseName[0].length() == 1 ?
					parseName[0].toUpperCase(Locale.US) : parseName[0]);
			// italicize alphabetical locants
			if (type == DISPLAY && locant.matches("[A-Za-z,#]*")) {
				locant = "<i>" + locant + "</i>";
			}
			tweakedName = locant + "-" + parseName[1];
		} else {
			// simply change case of initial letter of name
			tweakedName = (type == PULLDOWN ?
						name.substring(0, 1).toUpperCase(Locale.US)
						: name.startsWith("C(sp") ? name.charAt(0)
						: name.substring(0, 1).toLowerCase(Locale.ENGLISH))
					+ name.substring(1);
		} // if the name contains a hyphen
		debugPrint("FnalGroupDefs.tweakName: tweaked ", name, 
				" to ", tweakedName, " before applying Utils.", 
				(type == PULLDOWN ? "toPopupMenuDisplay()." : "toDisplay()."));
		tweakedName = (type == PULLDOWN ?
				Utils.toPopupMenuDisplay(tweakedName)
				: Utils.toDisplay(tweakedName));
		debugPrint("FnalGroupDefs.tweakName: tweaked ", name, 
				" to ", tweakedName, " for ", 
				(type == PULLDOWN ? "pulldown." : "display."));
		return tweakedName;
	} // tweakName(int)

	/** Makes a string used to sort the groups obtained from the database:
	 * <br>		sortName [tab] groupId
	 * <br>In sortname, leading locants are moved to the end of the name.
	 * @return	key for sorting the functional group
	 */
	public String getSortKey() {
		return (Utils.makeSortName(name) + DIVIDER + groupId);
	} // getSortKey()

/* ************** Static methods ****************** */

	/** Gets all functional groups from the database sorted by category
	 * and name.
	 * @return	array of all functional groups
	 */
	public static FnalGroupDef[] getAllGroups() {
		FnalGroupDef[] groups = null;
		try {
			groups = FnalGroupDefRW.getAllGroups();
		} catch (Exception e) {
			Utils.alwaysPrint("FnalGroupDef.getAllGroupNames: "
					+ "Exception: ", e.getMessage());
		}
		return groups;
	} // getAllGroups()

	/** Gets names of all functional groups from the database.
	 * @return	array of names of all functional groups
	 */
	public static List<String> getAllGroupNames() {
		return getAllGroupNames(!MAKE_LOWER);
	} // getAllGroupNames()

	/** Gets names of all functional groups from the database.
	 * @param	makeLower	make the names lower case
	 * @return	array of names of all functional groups
	 */
	public static List<String> getAllGroupNames(boolean makeLower) {
		List<String> grps = new ArrayList<String>();
		try {
			grps = FnalGroupDefRW.getAllGroupsData(
					makeLower ? LC_NAMES : NAMES);
		} catch (Exception e) {
			Utils.alwaysPrint("FnalGroupDef.getAllGroupNames: "
					+ "Exception: ", e.getMessage());
		}
		return grps;
	} // getAllGroupNames(boolean)

	/** Gets all categories of functional groups from the database.
	 * @return	array of categories of functional groups
	 */
	public static String[] getAllCategories() {
		String[] allCategories = null;
		try {
			final List<String> allCats =
					FnalGroupDefRW.getAllGroupsData(CATEGORIES);
			allCategories = allCats.toArray(new String[allCats.size()]);
		} catch (Exception e) {
			Utils.alwaysPrint("FnalGroupDef.getAllCategories: "
					+ "Exception: ", e.getMessage());
		}
		return allCategories;
	} // getAllCategories()

} // FnalGroupDef
