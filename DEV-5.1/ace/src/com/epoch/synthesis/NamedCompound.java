package com.epoch.synthesis;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.struc.Molecule;
import com.epoch.synthesis.synthConstants.SynthConstants;
import com.epoch.utils.Utils;

/** Name and imported molecule of a compound.  Used for impossible starting
 * materials and for menu-only reagents.  */
final class NamedCompound implements SynthConstants {

	/** Imported molecule representing the compound. */
	public transient Molecule mol;
	/** Name of the compound. */
	public transient String name;

	/** Constructor. 
	 * @param	compound	String definition and name
	 */
	NamedCompound(String[] compound) {
		name = compound[SM_NAME];
		try {
			mol = MolImporter.importMol(compound[SM_DEF]);
		} catch (MolFormatException e) { // highly unlikely
			Utils.alwaysPrint("NamedCompound: MolFormatException "
					+ "importing ", name, ": ", e.getMessage());
		} // try
	} // NamedCompound(String[])

} // NamedCompound
