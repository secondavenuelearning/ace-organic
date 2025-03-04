package com.epoch.synthesis;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import com.epoch.db.SynthDataRW;
import com.epoch.exceptions.DBException;
import com.epoch.synthesis.synthConstants.SynthConstants;
import com.epoch.utils.Utils;

/** Gets and stores compounds and substructures that students may try to use 
 * in synthesis responses but that cannot exist.  */
public final class SynthBadSMs implements SynthConstants {

	/** Saves this bad starting material to the database.  
	 * @param	badSM	name and definition of bad starting material
	 * @param	oldName	previous name of bad starting material, or null
	 * if this is a new entry
	 * @throws	MolFormatException	when the SMILES can't be imported
	 * @throws	DBException	when the write operation doesn't work
	 */
	public static void saveBadSM(String[] badSM, String oldName) 
			throws DBException, MolFormatException {
		// just validate the definition:
		MolImporter.importMol(badSM[SM_DEF]);
		// store
		SynthDataRW.saveBadSM(badSM, oldName);
	} // saveBadSM()

	/** Gets all bad starting materials from the database.  
	 * @return	array of all bad starting materials; each bad
	 * starting material is an array with two members
	 */
	public static String[][] getBadSMsStrs() {
		String[][] badSMs = new String[0][0];
		try {
			badSMs = SynthDataRW.getAllBadSMs();
		} catch (DBException e) {
			Utils.alwaysPrint("SynthBadSMs.getBadSMsStrs: "
					+ "DBException: ", e.getMessage());
		}
		return badSMs;
	} // getBadSMsStrs()

	/** Gets the bad starting materials with the imported molecules and names.
	 * @return	array of bad starting materials
	 */
	static NamedCompound[] getBadSMs() {
		final String[][] badSMs = getBadSMsStrs();
		final NamedCompound[] mols = new NamedCompound[badSMs.length];
		int badSMNum = 0;
		for (final String[] badSM : badSMs) {
			mols[badSMNum++] = new NamedCompound(badSM);
		} // for each bad starting material
		return mols;
	} // getBadSMs()

	/** Disables external instantiation. */
	private SynthBadSMs() { }

} // SynthBadSMs
