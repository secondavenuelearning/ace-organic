package com.epoch.synthesis;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import com.epoch.db.SynthDataRW;
import com.epoch.exceptions.DBException;
import com.epoch.synthesis.synthConstants.SynthConstants;
import com.epoch.utils.Utils;

/** Gets and stores the SMILES definitions and names of
 * reagents that students cannot draw, but must pull from the reagents 
 * menu that ACE provides.
 */
public final class SynthMenuOnlyRgts implements SynthConstants {

	/** Saves this menu-only reagent to the database.  
	 * @param	menuOnlyRgt	name and definition of menu-only reagent
	 * @param	oldName	previous name of menu-only reagent, or null
	 * if this is a new entry
	 * @throws	MolFormatException	when the SMILES can't be imported
	 * @throws	DBException	when the write operation doesn't work
	 */
	public static void saveMenuOnlyRgt(String[] menuOnlyRgt, String oldName) 
			throws DBException, MolFormatException {
		// just validate the definition:
		MolImporter.importMol(menuOnlyRgt[SM_DEF]);
		// store
		SynthDataRW.saveMenuOnlyRgt(menuOnlyRgt, oldName);
	} // saveMenuOnlyRgt()

	/** Gets all menu-only reagents from the database.  
	 * @return	array of all menu-only reagents; each menu-only reagent 
	 * is an array with two members
	 */
	public static String[][] getMenuOnlyRgtsStrs() {
		String[][] menuOnlyRgts = new String[0][0];
		try {
			menuOnlyRgts = SynthDataRW.getAllMenuOnlyRgts();
		} catch (DBException e) {
			Utils.alwaysPrint("SynthMenuOnlyRgts.getMenuOnlyRgtsStrs: "
					+ "DBException: ", e.getMessage());
		}
		return menuOnlyRgts;
	} // getMenuOnlyRgtsStrs()

	/** Gets the bad starting materials with the imported molecules and names.
	 * @return	array of bad starting materials
	 */
	static NamedCompound[] getMenuOnlyRgts() {
		final String[][] menuOnlyRgts = getMenuOnlyRgtsStrs();
		final NamedCompound[] mols = new NamedCompound[menuOnlyRgts.length];
		int menuOnlyRgtNum = 0;
		for (final String[] menuOnlyRgt : menuOnlyRgts) {
			mols[menuOnlyRgtNum++] = new NamedCompound(menuOnlyRgt);
		} // for each bad starting material
		return mols;
	} // getMenuOnlyRgts()

	/** Disables external instantiation. */
	private SynthMenuOnlyRgts() { }

} // SynthMenuOnlyRgts
