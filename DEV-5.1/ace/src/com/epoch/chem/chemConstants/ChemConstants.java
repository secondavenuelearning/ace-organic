package com.epoch.chem.chemConstants;

import chemaxon.struc.MolBond;
import chemaxon.struc.SgroupType;
import com.chemaxon.calculations.stereoisomers.StereoisomerSettings.StereoisomerType;
import com.epoch.constants.FormatConstants;
import java.util.EnumSet;

/** Holds constants used in chemical calculation methods. */
public interface ChemConstants extends FormatConstants {

	// public static final is implied by interface
/* ******** ChemUtils ******/

	/** Parameter for MolAtom.twicesumbonds().  */
	boolean INCLUDE_EXPLICIT_H = true;
	/** Parameter for MolAtom.twicesumbonds().  */
	boolean USE_BOND_ORDERS = false;
	/** A molecule property storing where a fragment was attached to the rest of
	 * a molecule. */
	String ATTACH_PT = "attachPt";
	/** A molecule property storing whether it was drawn in MarvinJS. */
	String FROM_MARVIN_JS = "fromMarvinJS";
	/** Parameter for ungroupSgroups(). */
	EnumSet<SgroupType> SHORTCUT_GROUPS = 
			EnumSet.of(SgroupType.SUPERATOM);
	/** Divider of selection numbers used by MarvinJS. */
	String MJS_SELECTIONS_DIVIDER = ",";

/* ******** MolCompare ******/

	/** Value for bit 0 of matchFlags parameter for matchExact(), matchPerfect(),
	 * matchSigmaNetwork(), or areResonanceOrIdentical(). */
	int WAVY_XOR = 0;
	/** Value for bit 0 of matchFlags parameter for matchExact() or matchPerfect(). */
	int WAVY_AND = 1;
	/** Value for bit 1 of matchFlags parameter for matchExact() or matchPerfect(). */
	int IGNORE_DBL_BOND_STEREO = (1 << 1);
	/** Value for bit 2 of matchFlags parameter for matchExact() or matchPerfect(). */
	int IGNORE_TETRAHEDRAL_STEREO = (1 << 2);
	/** Value for bits 1 and 2 of matchFlags parameter for matchExact() or 
	 * matchPerfect(). */
	int IGNORE_STEREO =
			(IGNORE_DBL_BOND_STEREO | IGNORE_TETRAHEDRAL_STEREO);
	/** Value for bit 4 of matchFlags parameter for matchExact() or matchPerfect(). */
	int CONSIDER_ENANT = (1 << 4);
	/** Value for bit 5 of matchFlags parameter for matchPerfect(). */
	int EXACT_EXPLICIT_H = (1 << 5);
	/** Value for bit 6 of matchFlags parameter for matchPerfect(). */
	int ELECTRONS = (1 << 6);
	/** Value for bit 7 of matchFlags parameter for matchExact(), matchPerfect(),
	 * matchSigmaNetwork(), or areResonanceOrIdentical(). */
	int ISOTOPE_PERMISSIVE = (1 << 7);
	/** Value for chgRadIso of containsSubstruct(). */
	int EXACT_CHG_RAD_ISO = 0;
	/** Value for chgRadIso of containsSubstruct(). */
	int DEFAULT_CHG_RAD_ISO = 1;
	/** Value for chgRadIso of containsSubstruct(). */
	int IGNORE_CHG_RAD_ISO = 2;
	/** Common value of atom or molecule property. */
	String TRUE = "true"; 

/* ******** StereoFunctions ******/

	/** Value of flag representing a stereorandom double bond. */
	int CRISSCROSS = (MolBond.CIS | MolBond.TRANS);
	/** Property set for products of Reactor calculation if starting materials
	 * are achiral but products are not.  */
	String RACEMIZE = "Racemize";
	/** Parameter for generating stereoisomers. */
	EnumSet<StereoisomerType> BOTH_STEREO_TYPES = 
			EnumSet.of(StereoisomerType.CISTRANS, StereoisomerType.TETRAHEDRAL);
	/** Bit for flag in getAngle(). */
	int RADIANS = (1 << 0);
	/** Bit for flag in getAngle(). */
	int SIGNED = (1 << 1);

} // ChemConstants
