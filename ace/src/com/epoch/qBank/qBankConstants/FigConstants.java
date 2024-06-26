package com.epoch.qBank.qBankConstants;

import chemaxon.struc.Molecule;
import com.epoch.constants.FormatConstants;
import com.epoch.synthesis.Synthesis;

/** Contains constants used by Figure. */
public interface FigConstants extends FormatConstants {

	// public static final is implied by interface

	/** Value for type. */
	int UNKNOWN = -1;
	/** Value for type. */
	int MOLECULE = 1;
	/** Value for type. */
	int REACTION = 2;
	/** Value for type. */
	int IMAGE = 3;
	/** Value for type. */
	int LEWIS = 4;
	/** Value for type. */
	int SYNTHESIS = 5;
	/** Value for type. */
	int JMOL = 6;
	/** Value for type. Can be used to display several molecules in a single
	 * MarvinView instance.  */
	int MRV_TXT = 7;
	/** Value for type. */
	int IMAGE_AND_VECTORS = 8;
	/** Database type values corresponding to internal type values.  */
	String[] DBVALUES = new String[] 
			{"", "MOL", "RXN", "IMG", "LEW", "SYN", "JML", "MRV", "VEC"};

	/** Separates Jmol data, scripts, Javascript commands. */
	String JMOL_SEP = "@@@===@@@";
	/** Separates reaction text above and below the arrow in RXN figures. */
	String RXN_TEXT_SEP = "\t";

	/** Position of molecule data in value from getDisplayData(). */
	int STRUCT = 0;
	/** Position of Jmol scripts in value from getDisplayData(). */
	int JMOL_SCRIPTS = 1;
	/** Position of Jmol Javascript commands in value from getDisplayData(). */
	int JMOL_JS_CMDS = 2;
	/** Position of reaction text above the arrow in value from getDisplayData(). */
	int RXN_ABOVE = 1;
	/** Position of reaction text below the arrow in value from getDisplayData(). */
	int RXN_BELOW = 2;
	/** Position of vector coordinates in value from getDisplayData(). */
	int COORDS = 1;

	/** Parameter for getImage(). */
	String[] NO_RGROUP_STRS = null;
	/** Parameter for getDisplayData(). */
	Molecule[] NO_RGROUPS = new Molecule[0];
	/** Parameter for getDisplayData(). */
	String[] SYN_PHRASES = Synthesis.getRxnsDisplayPhrases();

	/** Tag for XML IO. */
	String FIGURE_TAG = "reference";
	/** Tag for XML IO. */
	String IMAGE_FILE_TAG = "imagefile";
	/** Tag for XML IO. */
	String MOLSTRUCT_TAG = "molstruct";
	/** Tag for XML IO. */
	String ADDL_DATA_TAG = "reactiondata"; // leave unchanged for back-compatibility
	/** Attribute values. */	
	String[] TYPE_ATTRIBUTES = new String[] 
			{"", "MOLECULE", "REACTION", "IMAGE", "LEWIS", 
			"SYNTHESIS", "JMOL", "MRV_TXT", "IMAGE_AND_VECTORS"};
	/** Start of the name of a file that contains the image of a molecule. */
	String MOL_IMG_FILENAME_START = "molImg";

} // FigConstants
