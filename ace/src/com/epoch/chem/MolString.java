package com.epoch.chem;

import chemaxon.formats.MolExporter;
import chemaxon.formats.MolImporter;
import chemaxon.formats.MolFormatException;
import chemaxon.marvin.MolPrinter;
import chemaxon.marvin.io.MolExportException;
import chemaxon.marvin.modules.reaction.ReactionEditUtils;
import chemaxon.marvin.paint.DispOptConsts;
import chemaxon.marvin.paint.constants.RenderingStyle;
import chemaxon.struc.DPoint3;
import chemaxon.struc.MDocument;
import chemaxon.struc.MObject;
import chemaxon.struc.Molecule;
import chemaxon.struc.MoleculeGraph;
import chemaxon.struc.MolAtom;
import chemaxon.struc.MolBond;
import chemaxon.struc.MPoint;
import chemaxon.struc.RxnMolecule;
import chemaxon.struc.graphics.MEFlow;
import chemaxon.struc.graphics.MPolyline;
import chemaxon.struc.graphics.MRectangle;
import chemaxon.struc.graphics.MRectanglePoint;
import chemaxon.struc.graphics.MTextBox;
import com.epoch.chem.chemConstants.ChemConstants;
import com.epoch.chem.chemConstants.MechSynthConstants;
import com.epoch.lewis.LewisMolecule;
import com.epoch.qBank.Question;
import com.epoch.utils.Utils;
import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Holds molecule format interconversions and manipulations of graphical objects
 * in MRV documents. */
public final class MolString 
		implements ChemConstants, DispOptConsts, MechSynthConstants {

	private static void debugPrint(Object... msg) { 
		// Utils.printToLog(msg);
	}

	private static void debugPrintMRV(Object... msg) {
		// Utils.printToLog(msg, MRV);
	}

	/** Parameter for getBestAppletSize().  */
	public static final boolean SHOW_MAPPING = true;
	/** Parameter for getBestAppletSize().  */
	public static final boolean FIXED_BOND_LENGTH = true;
	/** Preferred width of an image in pixels. */
	public static final int OPT_WIDTH = 250;

	/** Imports and reexports an MRV. Required to make old Marvin formats 
	 * compatible with MarvinJS.
	 * @param	molStruct	the structure of the molecule, probably MRV or MOL
	 * @return	the structure of the molecule in updated MRV
	 */
	public static String updateFormat(String molStruct) {
		String newMolStruct = molStruct;
		try {
			final Molecule tempMol = MolImporter.importMol(newMolStruct);
			newMolStruct = MolString.toString(tempMol, MRV);
		} catch (MolFormatException e) {
			; // do nothing
		} // try
		return newMolStruct;
	} // updateFormat(String)

	/** Convert the molStruct string to a new format (such as "smiles")
	 * unless we can determine that conversion loses information, in which case
	 * return the molStruct unchanged.
	 * @param	molStruct	String representation of a molecule
	 * @param	toFormat	new format of molecule
	 * @return	String representation of the molecule in the new format
	 */
	public static String convertMol(String molStruct, String toFormat) {
		return convertMol(molStruct, toFormat, 1);
	} // convertMol(String, String)

	/** Convert the molStruct string to a new format (such as "smiles")
	 * unless we can determine that conversion loses information, in which case
	 * return the molStruct unchanged.
	 * @param	molStruct	String representation of a molecule
	 * @param	toFormat	new format of molecule
	 * @param	scaleFactor	factor by which to scale the size of the compound
	 * @return	String representation of the molecule in the new format
	 */
	public static String convertMol(String molStruct, String toFormat, 
			double scaleFactor) {
		final String SELF = "MolString.convertMol: ";
		try {
			debugPrint(SELF + "toFormat = ", toFormat, ", scaleFactor = ",
					scaleFactor);
			debugPrint(SELF + "trying to import:\n", molStruct);
			final boolean isLewis = molStruct.contains("Lewis");
			Molecule mrvMolecule;
			if (isLewis) {
				final LewisMolecule lewisMol = new LewisMolecule(molStruct);
				mrvMolecule = lewisMol.getMolecule();
			} else {
				mrvMolecule = MolImporter.importMol(molStruct);
			} // if isLewis
			debugPrint(SELF + "import successful; getting MDocument.");
			final MDocument mDoc = mrvMolecule.getDocument();
			if (mDoc != null) {
				for (int objnum = 0; objnum < mDoc.getObjectCount(); objnum++) {
					final MObject mObject = mDoc.getObject(objnum);
					if (mObject instanceof MPolyline) {
						debugPrint(SELF + "MRV document will lose information in ",
								toFormat, " format; not converting.");
						return molStruct;
					} // if there's an MPolyline object
				} // for each object
			} // if there's an MDocument
			final boolean toSMILES = SMILES.equals(toFormat);
			if (mrvMolecule.getDim() == 0 && !toSMILES) {
				debugPrint(SELF + "2D-cleaning");
				ChemUtils.clean2D(mrvMolecule);
			} // if need to do 2D clean
			if (scaleFactor != 1 && !toSMILES) {
				debugPrint(SELF + "rescaling");
				StereoFunctions.rescale(mrvMolecule, scaleFactor);
			} // if need to rescale
			debugPrint(SELF + "converting to ", toFormat);
			String reformat = toString(mrvMolecule, toFormat);
			debugPrint(SELF + "conversion successful:\n", reformat);
			if (isLewis) reformat = reformat.replaceFirst("Marvin", "Lewis ");
			return reformat;
		} catch (MolFormatException e0) {
			debugPrint(SELF + "MolFormatException when trying to convert to ",
					toFormat, " format; returning original.");
		} catch (Exception e1) {
			debugPrint(SELF + "failed to convert to ",
					toFormat, " format; returning original.");
		}
		return molStruct;
	} // convertMol(String, String, double)

	/** Convert the molStruct string to MOL format
	 * unless we can determine that conversion loses information, in which case
	 * return the molStruct unchanged.
	 * @param	molStruct	String representation of a molecule
	 * @return	the molecule in the new format
	 */
	public static String decompressMol(String molStruct) {
		return convertMol(molStruct, "mol");
	} // decompressMol(String)

	/** Convert the molStruct string to SMILES format
	 * unless we can determine that conversion loses information, in which case
	 * return the molStruct unchanged.
	 * @param	molStruct	String representation of a molecule
	 * @return	the molecule in the new format
	 */
	public static String toSmiles(String molStruct) {
		return convertMol(molStruct, SMILES);
	} // toSmiles(String)

	/** Gets a string representation of the molecule.
	 * @param	mol	the molecule
	 * @return	the string representation of the molecule, or empty 
	 * string if export fails
	 */
	public static String toString(Molecule mol) {
		return toString(mol, SMILES);
	} // toString(Molecule)

	/** Gets a string representation of the molecule.
	 * @param	mol	the molecule
	 * @param	format	format of the string representation
	 * @return	the string representation of the molecule, or empty 
	 * string if export fails
	 */
	public static String toString(Molecule mol, String format) {
		final String SELF = "MolString.toString: ";
		try {
			return MolExporter.exportToFormat(mol, format);
		} catch (Exception e1) { 
			try {
				return MolExporter.exportToFormat(mol, SMILES);
			} catch (Exception e2) { 
				try {
					return MolExporter.exportToFormat(mol, SMARTS);
				} catch (Exception e3) { 
					try {
						return MolExporter.exportToFormat(mol, MRV);
					} catch (Exception e4) { 
						 // do nothing
					} // try
				} // try
			} // try
		} // try
		Utils.alwaysPrint(SELF + "Exception caught while "
				+ "converting molecule to ", format,
				"; returning an empty string instead."); 
		return "";
	} // toString(Molecule, String)

	/** Gets a string representation of the MDocument.
	 * @param	mDoc	the MDocument
	 * @param	format	format of the string representation
	 * @return	the string representation of the MDocument
	 * @throws MolExportException	if export fails
	 */
	public static String toString(MDocument mDoc, String format) 
			throws MolExportException {
		try {
			return MolExporter.exportToFormat(mDoc, format);
		} catch (IOException e) { 
			throw new MolExportException(e); 
		}
	} // toString(MDocument, String)

	/** Gets a binary representation of the molecule.
	 * @param	mol	the molecule
	 * @param	format	format of the representation
	 * @return	the representation of the molecule as an array of bytes, or an
	 * empty array if export fails
	 */
	public static byte[] toBinFormat(Molecule mol, String format) {
		try {
			return MolExporter.exportToBinFormat(mol, format);
		} catch (IOException e) {
			// do nothing
		}
		return new byte[0];
	} // toBinFormat(Molecule, String)

	/** Gets a binary representation of the molecule document.
	 * @param	mDoc	the document
	 * @param	format	format of the representation
	 * @return	the representation of the document as an array of bytes, or an
	 * empty array if export fails
	 */
	public static byte[] toBinFormat(MDocument mDoc, String format) {
		try {
			return MolExporter.exportToBinFormat(mDoc, format);
		} catch (IOException e) {
			// do nothing
		}
		return new byte[0];
	} // toBinFormat(MDocument, String)

	/** Get all compounds (fragments) from a given MRV doc and ungroup, 
	 * hydrogenize and (maybe) aromatize for molSearches.
	 * @param	materials	MRV representation of a Marvin document containing
	 * molecules
	 * @param	aromatize	whether to aromatize
	 * @return	an array of processed molecules
	 * @throws	MolFormatException	if the starting materials can't be imported 
	 * into a Molecule
	 */
	public static Molecule[] getMolArray(String materials, boolean aromatize) 
			throws MolFormatException {
		return getMolArray(MolImporter.importMol(materials), aromatize);
	} // getMolArray(String, boolean)

	/** Get all compounds (fragments) from a Molecule and ungroup, hydrogenize 
	 * and (maybe) aromatize for molSearches.
	 * @param	mol	a Molecule containing fragments
	 * @param	aromatize	whether to aromatize
	 * @return	an array of processed molecules
	 */
	public static Molecule[] getMolArray(Molecule mol, boolean aromatize) {
		mol.ungroupSgroups(SHORTCUT_GROUPS);
		ChemUtils.explicitizeHnoClone(mol);
		if (aromatize) mol.aromatize();
		return mol.convertToFrags();
	} // getMolArray(Molecule, boolean)

	/** Gets the size of a MarvinView when the bond length is fixed.
	 * @param	molStruct	String representation of the molecule to be 
	 * displayed
	 * @return	{width, height}
	 */
	public static int[] getAppletSize(String molStruct) {
		return getBestAppletSize(molStruct, !SHOW_MAPPING, FIXED_BOND_LENGTH);
	} // getAppletSize(String)

	/** Gets the size of a MarvinView when the bond length is fixed.
	 * @param	molStruct	String representation of the molecule to be 
	 * displayed
	 * @param	showMapping	whether atom mapping will be shown
	 * @return	{width, height}
	 */
	public static int[] getAppletSize(String molStruct, boolean showMapping) {
		return getBestAppletSize(molStruct, showMapping, FIXED_BOND_LENGTH);
	} // getAppletSize(String, boolean)

	/** Gets the size of a MarvinView applet that looks best for a particular
	 * structure.
	 * @param	molStruct	String representation of the molecule to be 
	 * displayed
	 * @return	{width, height}
	 */
	public static int[] getBestAppletSize(String molStruct) {
		return getBestAppletSize(molStruct, !SHOW_MAPPING, !FIXED_BOND_LENGTH);
	} // getBestAppletSize(String)

	/** Gets the size of a MarvinView applet that looks best for a particular
	 * structure.
	 * @param	molStruct	String representation of the molecule to be 
	 * displayed
	 * @param	showMapping	whether atom mapping will be shown
	 * @return	{width, height}
	 */
	public static int[] getBestAppletSize(String molStruct, 
			boolean showMapping) {
		return getBestAppletSize(molStruct, showMapping, !FIXED_BOND_LENGTH);
	} // getBestAppletSize(String, boolean)

	/** Gets the size of a MarvinView applet that looks best for a particular
	 * structure.
	 * @param	molStruct	String representation of the molecule to be 
	 * displayed
	 * @param	showMapping	whether atom mapping will be shown
	 * @param	fixedBondLength	whether the bond length should be fixed
	 * @return	{width, height}
	 */
	private static int[] getBestAppletSize(String molStruct, 
			boolean showMapping, boolean fixedBondLength) {
		final String SELF = "MolString.getBestAppletSize: ";
		int[] dims = new int[] {OPT_WIDTH, OPT_WIDTH};
		if (molStruct == null) return dims;
		try {
			final boolean isLewis = molStruct.contains("Lewis ");
			final Molecule mol = MolImporter.importMol(molStruct);
			dims = getBestAppletSize(mol, isLewis, showMapping, 
					fixedBondLength);
		} catch (MolFormatException e1) {
			System.out.println(SELF + "MolFormatException for:\n" + molStruct);
			e1.printStackTrace();
		}
		return dims;
	} // getBestAppletSize(String, boolean, boolean)

	/** Gets the size of a MarvinView applet that looks best for a particular
	 * structure.
	 * @param	mol	the molecule to be displayed
	 * @param	isLewis	true if the molecule is a Lewis structure
	 * @return	{width, height}
	 */
	private static int[] getBestAppletSize(Molecule mol, boolean isLewis) {
		return getBestAppletSize(mol, isLewis, !SHOW_MAPPING, !FIXED_BOND_LENGTH);
	} // getBestAppletSize(Molecule, boolean)

	/** Gets the size of a MarvinView applet that looks best for a particular
	 * structure.
	 * @param	mol	the molecule to be displayed
	 * @param	isLewis	true if the molecule is a Lewis structure
	 * @param	showMapping	whether atom mapping will be shown
	 * @return	{width, height}
	 */
	private static int[] getBestAppletSize(Molecule mol, boolean isLewis,
			boolean showMapping) {
		return getBestAppletSize(mol, isLewis, showMapping, !FIXED_BOND_LENGTH);
	} // getBestAppletSize(Molecule, boolean, boolean)

	/** Gets the size of a MarvinView applet that looks best for a particular
	 * structure.
	 * @param	mol	the molecule to be displayed
	 * @param	isLewis	true if the molecule is a Lewis structure
	 * @param	showMapping	whether atom mapping will be shown
	 * @param	fixedBondLength	whether the bond length should be fixed
	 * @return	{width, height}
	 */
	private static int[] getBestAppletSize(Molecule mol, boolean isLewis,
			boolean showMapping, boolean fixedBondLength) {
		final String SELF = "MolString.getBestAppletSize: ";
		int[] dims = new int[] {OPT_WIDTH, OPT_WIDTH};
		final int MAX_SCALE = fixedBondLength ? 30 : 40; // largest scale that looks OK
		final int MIN_SCALE = 16; // smallest scale that looks OK
		try {
			if (mol.getDim() == 0) ChemUtils.clean2D(mol);
			final MolPrinter printer = new MolPrinter(mol);
			printer.setLonePairsVisible(true);
			// printer.setRendering(WIREFRAME_RENDERING_S); // older JChem
			printer.setRendering(RenderingStyle.WIREFRAME.toString());
					// more recent JChems
			printer.setImplicitH(isLewis ? IMPLICITH_ALL_S 
					: IMPLICITH_HETEROTERM_S);
			printer.setScale(MAX_SCALE); 
			final Rectangle boundingRect = printer.getBoundingRectangle(
					mol.getDocument().getAllMolecules());
			/* debugPrint(SELF + "for:\n", molStruct, "image at scale ", 
					MAX_SCALE, " has width ", boundingRect.width, 
					" and height ", boundingRect.height, "; atom size is ",
					printer.getAtomSize()); /**/
			// determine appropriate width
			if (boundingRect.width < OPT_WIDTH || fixedBondLength) {
				// use width and height at standard scale
				dims[0] = boundingRect.width;
				dims[1] = boundingRect.height;
			} else {
				final double smallerScale = ((double) (MAX_SCALE * OPT_WIDTH)) 
						/ (double) boundingRect.width;
				if (smallerScale > ((double) MIN_SCALE)) {
					debugPrint(SELF + "scaling down to scale ", smallerScale);
					// use optimum width
					dims[0] = OPT_WIDTH;
					dims[1] = (boundingRect.height * OPT_WIDTH) 
							/ boundingRect.width;
				} else {
					debugPrint(SELF + "scaling down to scale ", MIN_SCALE);
					// use width that gives minimum bond length
					dims[0] = (boundingRect.width * MIN_SCALE) / MAX_SCALE;
					dims[1] = (boundingRect.height * MIN_SCALE) / MAX_SCALE;
				} // if scale
			} // if boundingRect.width
			/* debugPrint(SELF + "setting image size to width ",
					dims[0], " and height ", dims[1]); /**/
		} catch (Exception e2) {
			Utils.alwaysPrint(SELF + "got an exception on:\n", mol);
			e2.printStackTrace();
		}
		return dims;
	} // getBestAppletSize(Molecule, boolean, boolean, boolean)

	/** Gets a Javascript command that will generate a molecule's image
	 * in a Web page, or the SVG or PNG representation of the image.
	 * @param	pathToRoot	path to application root
	 * @param	molStr	String representation of a molecule
	 * @return	the Javascript for creating the molecule's image
	 * in a Web page, or the SVG or PNG representation of the image
	 */
	public static String getImage(String pathToRoot, String molStr) {
		return getImage(pathToRoot, molStr, 0L, "figure", false);
	} // getImage(String, String)

	/** Gets a Javascript command that will generate a molecule's image
	 * in a Web page, or the SVG or PNG representation of the image.
	 * @param	pathToRoot	path to application root
	 * @param	molStr	String representation of a molecule
	 * @param	qFlags	the question's display flags
	 * @return	the Javascript for creating the molecule's image
	 * in a Web page, or the SVG or PNG representation of the image
	 */
	public static String getImage(String pathToRoot, String molStr, 
			long qFlags) {
		return getImage(pathToRoot, molStr, qFlags, "figure", false);
	} // getImage(String, String, long)

	/** Gets a Javascript command that will generate a molecule's image
	 * in a Web page, or the SVG or PNG representation of the image.
	 * @param	pathToRoot	path to application root
	 * @param	molStr	String representation of a molecule
	 * @param	qFlags	the question's display flags
	 * @param	imgIdStr	unique identifier of the image on the Web page
	 * @return	the Javascript for creating the molecule's image
	 * in a Web page, or the SVG or PNG representation of the image
	 */
	public static String getImage(String pathToRoot, String molStr, 
			long qFlags, String imgIdStr) {
		return getImage(pathToRoot, molStr, qFlags, imgIdStr, false);
	} // getImage(String, String, long, String)

	/** Gets a Javascript command that will generate a molecule's image
	 * in a Web page, or the SVG or PNG representation of the image.
	 * @param	pathToRoot	path to application root
	 * @param	molStr	String representation of a molecule
	 * @param	qFlags	the question's display flags
	 * @param	imgIdStr	unique identifier of the image on the Web page
	 * @param	prefersPNG	user prefers PNG over SVG graphics
	 * @return	the Javascript for creating the molecule's image
	 * in a Web page, or the SVG or PNG representation of the image
	 */
	public static String getImage(String pathToRoot, String molStr, 
			long qFlags, String imgIdStr, boolean prefersPNG) {
		final String SELF = "MolString.getImage: ";
		String imageStr = "";
		Molecule mol = new Molecule();
		int[] dims = new int[0];
		final String format = (prefersPNG ? PNG : SVG);
		if (!Utils.isEmpty(molStr)) try {
			debugPrintMRV(SELF + "original molStr:\n", molStr);
			mol = ReactionEditUtils.fixReaction(MolImporter.importMol(molStr), 
					false);
			final boolean isLewis = molStr.contains("Lewis ");
			dims = getBestAppletSize(mol, isLewis);
			debugPrint(SELF + "isLewis = ", isLewis, ", imgIdStr = ", imgIdStr, 
					", prefersPNG = ", prefersPNG);
			imageStr = getJChemImage(mol, qFlags, isLewis, prefersPNG, dims);
			debugPrint(SELF + "imageStr:\n", imageStr);
		} catch (MolFormatException e) {
			Utils.alwaysPrintMRV(SELF + "caught MolFormatException with "
					+ "JChem converting to format ", format, " on:\n", mol,
					"\n; trying the MarvinJS method instead.");
			e.printStackTrace();
			imageStr = getMarvinJSImage(pathToRoot, molStr, qFlags, 
					imgIdStr, prefersPNG, dims);
		} catch (NullPointerException e) {
			Utils.alwaysPrintMRV(SELF + "caught NullPointerException with "
					+ "JChem converting this mol to an image:\n", mol,
					"\n; trying the MarvinJS method instead.");
			e.printStackTrace();
			imageStr = getMarvinJSImage(pathToRoot, molStr, qFlags, 
					imgIdStr, prefersPNG, dims);
		} catch (IllegalArgumentException e) {
			Utils.alwaysPrintMRV(SELF + "caught IllegalArgumentException "
					+ "with JChem converting this mol to an image:\n", mol,
					"\n; trying the MarvinJS method instead.");
			e.printStackTrace();
			imageStr = getMarvinJSImage(pathToRoot, molStr, qFlags, 
					imgIdStr, prefersPNG, dims);
		}
		debugPrintMRV(SELF + "image string:\n", imageStr);
		return imageStr;
	} // getImage(String, String, long, String, boolean)

	/** Uses JChem to get the molecule's image.
	 * @param	mol	a molecule
	 * @param	qFlags	flags affecting the image
	 * @param	isLewis	whether it's a Lewis structure
	 * @param	prefersPNG	user prefers PNG over SVG graphics
	 * @param	dims	the calculated best dimensions of the image
	 * @return	the molecule's image
	 */
	private static String getJChemImage(Molecule mol, long qFlags, 
			boolean isLewis, boolean prefersPNG, int[] dims) throws
			NullPointerException, IllegalArgumentException {
		final String SELF = "MolString.getJChemImage: ";
		final String format = (prefersPNG ? PNG : SVG);
		final String opts = 
				getJChemImageOpts(format, dims, qFlags, isLewis);
		MDocument mDoc = mol.getDocument();
		if (mDoc == null) mDoc = new MDocument(mol);
		final String imageStr = (prefersPNG
				? Utils.toString("<img src=\"data:image/", PNG, ";base64,",
					encode(toBinFormat(mDoc, opts)), "\" />")
				: Utils.bytesToUnicodeString(toBinFormat(mDoc, opts))
					.replaceAll("\"clipPath1\"", "\"clipPath2\""));
		debugPrint(SELF + "JChem generated image src URI with options ", opts, 
				"; isLewis = ", isLewis);
		return imageStr;
	} // getJChemImage(Molecule, long, boolean, boolean, int[])

	/** Uses MarvinJS to get the molecule's image.
	 * @param	pathToRoot	path to application root
	 * @param	molStr	String representation of a molecule
	 * @param	qFlags	the question's display flags
	 * @param	imgIdStr	unique identifier of the image on the Web page
	 * @param	prefersPNG	user prefers PNG over SVG graphics
	 * @param	dims	the calculated best dimensions of the image
	 * @return	the Javascript for creating the molecule's image
	 */
	private static String getMarvinJSImage(String pathToRoot, String molStr, 
			long qFlags, String imgIdStr, boolean prefersPNG, int[] dims) {
		final String SELF = "MolString.getMarvinJSImage: ";
		final String imageStr = Utils.toString(
				"<div id=\"figJS", imgIdStr, "\" ",
					"class=\"left10\" style=\"display:none; ",
				"text-align:center;\">\n",
				"<script type=\"text/javascript\">\n",
				"displayImage(",
					"'", Utils.toValidJS(pathToRoot), "', ",
					"'", Utils.toValidJS(molStr), "',\n",
					"{ flags: ", qFlags, ", ",
					"imageId: '", imgIdStr, "', ",
					"width: ", dims[0], ", ",
					"height: ", dims[1], ", ",
					"prefersPNG: ", prefersPNG, " });\n",
				"</script>\n",
				"</div>\n");
		return imageStr;
	} // getMarvinJSImage(String, String, long, String, boolean, int[])

	/** Encodes a byte array into Base64 encoding for display as SVG image.
	 * @param	in	the image as an array of bytes
	 * @return	a string encoded with Base64 methods
	 */
	private static String encode(byte[] in) {
		return new String(Base64.getEncoder().encode(in), 
				StandardCharsets.ISO_8859_1);
	} // encode(byte[])

	/** Prepares a string representing options for image formation.
	 * @param	imgType	the type of image (SVG or PNG)
	 * @param	dims	dimensions of the image
	 * @param	qFlags	flags affecting the image
	 * @param	isLewis	whether the image is a LewisSketch image
	 * @return	the options as a string
	 */
	private static String getJChemImageOpts(String imgType, int[] dims, long qFlags, 
			boolean isLewis) {
		final StringBuilder optsBld = Utils.getBuilder(imgType, ':');
		if (SVG.equals(imgType)) optsBld.append("headless,");
		Utils.appendTo(optsBld, 'w', dims[0], ",h", dims[1]);
		if (!isLewis && !Question.badValenceInvisible(qFlags)) {
			optsBld.append(",valenceErrorVisible");
		} // if should show valence errors
		Utils.appendTo(optsBld, ",H_", 
				Question.showNoHydrogens(qFlags) || isLewis ? "off"
					: Question.showHeteroHydrogens(qFlags) ? "hetero"
					: Question.showAllHydrogens(qFlags) ? "all"
					: "heteroterm",
				",cv_", Question.showAllCarbons(qFlags)
					|| isLewis ? "on" : "inChain");
		if (Question.showLonePairs(qFlags)) optsBld.append(",lp");
		if (Question.showMapping(qFlags)) optsBld.append(",amap");
		if (Question.showRSLabels(qFlags)) optsBld.append(",chiral_all");
		return optsBld.toString();
	} // getJChemImageOpts(String, int[], long, boolean)

	/** Deletes an image file.
	 * @param	absFileName	file to be deleted
	 */
	public static void deleteImage(String absFileName) {
		try {
			final File file = new File(absFileName);
			if (file.exists()) file.delete(); // ignore return value
		} catch (Exception e2) {
			Utils.alwaysPrint("MolString.deleteImage: "
					+ "got the following exception on ", absFileName);
			e2.printStackTrace();
		}
	} // deleteImage(String)

	/** Gets an HTML string describing the charge.
	 * @param	chg	the charge of the atom or molecule
	 * @return	the string describing the charge, formatted for HTML
	 */
	private static final StringBuilder getChgDescrip(int chg) {
		final StringBuilder chgBld = new StringBuilder();
		if (chg != 0) {
			chgBld.append("<sup>");
			final boolean isNeg = chg < 0;
			final int absChg = (isNeg ? -chg : chg);
			if (absChg != 1) chgBld.append(absChg);
			chgBld.append(isNeg ? "&minus;" : '+');
			chgBld.append("</sup>");
		} // if there's a charge 
		return chgBld;
	} // getChgDescrip(int)

	/** Gets a string, formatted for HTML, describing the symbol, index number,
	 * and charge of the atom.
	 * @param	atom	an atom in the molecule
	 * @return	the symbol, index, and charge of the atom, formatted for HTML
	 */
	private static final StringBuilder getAtomDescrip(MolAtom atom) {
		final StringBuilder bld = new StringBuilder()
				.append(atom.getSymbol())
				.append(atom.getParent().indexOf(atom) + 1);
		bld.append(getChgDescrip(atom.getCharge()));
		return bld;
	} // getAtomDescrip(MolAtom)

	/** Gets a string, formatted for HTML, describing the index number and order
	 * of the bond.
	 * @param	bond	a bond in the molecule
	 * @return	the order and index of the bond, formatted for HTML
	 */
	private static final StringBuilder getBondDescrip(MolBond bond) {
		final int order = bond.getType();
		final StringBuilder bld = new StringBuilder()
				.append(getAtomDescrip(bond.getAtom1()))
				.append(order == 3 ? "&#9776;" 
					: order == 2 ? "=" 
					: order == 1 ? "&ndash;" 
					: "[unknown order]")
				.append(getAtomDescrip(bond.getAtom2()));
		return bld;
	} // getBondDescrip(MolBond)

	/** Gets an English description of the Marvin drawing for BLV people.
	 * @param	mrvStr	the MRV of the Marvin drawing
	 * @return	an English description of the drawing, formatted for HTML
	 */
	public static String getBLVDescription(String mrvStr) {
		final String SELF = "MolString.getBLVDescription: ";
		final StringBuilder bld = new StringBuilder();
		try {
			final Molecule wholeMol = (Utils.isEmpty(mrvStr) ? new Molecule()
					: MolImporter.importMol(mrvStr));
			final Molecule[] frags = wholeMol.clone().convertToFrags();
			final int numFrags = frags.length;
			bld.append("<p>The MarvinJS drawing contains ")
					.append(numFrags).append(" molecule")
					.append(numFrags == 1 ? "" : 's').append(".</p>");
			for (int molNum = 0; molNum < frags.length; molNum++) {
				final Molecule mol = frags[molNum];
				// describe formula and charge
				if (numFrags > 1) {
					bld.append("<p>Molecule ").append(molNum + 1);
				} else { 
					bld.append("<p>The molecule");
				} // if numFrags
				bld.append(" has");
				try {
					bld.append(" the IUPAC name ")
						.append(MolExporter.exportToFormat(mol, "name"))
						.append(" and");
				} catch (IOException e) {
					;  // do nothing
				} // try
				bld.append(" the formula ")
						.append(Utils.toDisplay(mol.getFormula()))
						.append(getChgDescrip(mol.getTotalCharge()))
						.append(".</p>");
				// describe all atoms and their implicit H counts
				bld.append("</p><p>The atoms are ");
				final MolAtom[] atoms = mol.getAtomArray();
				final int numAtoms = atoms.length;
				int atomNum = 0;
				for (final MolAtom atom : atoms) {
					bld.append(getAtomDescrip(atom));
					if (!atom.getSymbol().equals("C")) {
						final int implicitHCount = atom.getImplicitHcount();
						if (implicitHCount > 0) {
							bld.append(" (with ").append(implicitHCount)
									.append(" visible H atom");
							if (implicitHCount != 1) bld.append('s');
							bld.append(')');
						} // if implicit H atoms
					} // if not C
					bld.append(atomNum < numAtoms - 2 ? ", "
							: atomNum < numAtoms - 1 ? ", and " 
							: "");
					atomNum++;
				} // for each atom
				bld.append(".</p>");
				// describe all bonds and their orders
				bld.append("</p><p>The bonds connecting the atoms are ");
				final MolBond[] bonds = mol.getBondArray();
				final int numBonds = bonds.length;
				int bondNum = 0;
				for (final MolBond bond : bonds) {
					bld.append(getBondDescrip(bond))
							.append(bondNum < numBonds - 2 ? ", "
							: bondNum < numBonds - 1 ? ", and " 
							: "");
					bondNum++;
				} // for each bond
				bld.append(".</p>");
				// describe number of rings and their sizes
				final int[][] ringsAtomIndices = mol.getSSSR();
				final int numRings = ringsAtomIndices.length;
				if (numRings > 0) {
					bld.append("<p>It has ").append(numRings).append(" ring");
					bld.append(numRings > 1 ? "s.</br>" : " consisting of ");
					int ringNum = 0;
					for (final int[] ringAtomIndices : ringsAtomIndices) {
						final int ringSize = ringAtomIndices.length;
						if (numRings > 1) {
							bld.append("Ring ").append(ringNum + 1)
									.append(" consists of ");
						} // if there are multiple rings
						bld.append(ringSize).append(" atoms: ");
						int ringAtomNum = 0;
						for (int atomIndex : ringAtomIndices) { 
							final MolAtom atom = mol.getAtom(atomIndex);
							bld.append(getAtomDescrip(atom))
									.append(ringAtomNum < ringSize - 2 ? ", "
										: ringAtomNum < ringSize - 1 ? ", and " 
										: "");
							ringAtomNum++;
						} // for each atom in the ring
						bld.append(".</br>");
						ringNum++;
					} // for each ring
				} else {
					bld.append("<p>It has no rings.</p> ");
				} // if there are rings
			} // for each fragment %>
		} catch (MolFormatException e) {
			Utils.alwaysPrint(SELF + "MolFormatException for:\n", mrvStr);
			e.printStackTrace();
		}
		return bld.toString();
	} // getBLVDescription(String)

/* ***************** MRV manipulations *******************/

	/** Color an object red in an MRV string representing a molecule.
	 * @param	molStr	the MRV string
	 * @param	objIndex	index of object to be colored
	 * @return	the MRV string with the object in red
	 */
	public static String colorMObject(String molStr, int objIndex) {
		return colorMObject(molStr, objIndex, Color.RED);
	} // colorMObject(String, int)

	/** Color an object in an MRV string representing a molecule.
	 * @param	molStr	the MRV string
	 * @param	objIndex	index of object to be colored
	 * @param	markColor	color to use
	 * @return	the MRV string with the object colored
	 */
	public static String colorMObject(String molStr, int objIndex,
			Color markColor) {
		final int[] objectIndices = new int[] {objIndex};
		return colorMObjects(molStr, objectIndices, markColor);
	} // colorMObject(String, int, Color)

	/** Color two objects red in an MRV string representing a molecule.
	 * @param	molStr	the MRV string
	 * @param	objIndex1	index of first object to be colored
	 * @param	objIndex2	index of second object to be colored
	 * @return	the MRV string with the objects in red
	 */
	public static String colorMObjects(String molStr,
			int objIndex1, int objIndex2) {
		return colorMObjects(molStr, objIndex1, objIndex2, Color.RED);
	} // colorMObjects(String, int, int)

	/** Color two objects in an MRV string representing a molecule.
	 * @param	molStr	the MRV string
	 * @param	objIndex1	index of first object to be colored
	 * @param	objIndex2	index of second object to be colored
	 * @param	markColor	color to use
	 * @return	the MRV string with the objects colored
	 */
	public static String colorMObjects(String molStr, int objIndex1,
			int objIndex2, Color markColor) {
		final int[] objectIndices = new int[] {objIndex1, objIndex2};
		return colorMObjects(molStr, objectIndices, markColor);
	} // colorMObjects(String, int, int, Color)

	/** Color several objects red in an MRV string representing a molecule.
	 * @param	molStr	the MRV string
	 * @param	indices	array of indices of objects to be colored
	 * @return	the MRV string with the objects in red
	 */
	public static String colorMObjects(String molStr, int[] indices) {
		return colorMObjects(molStr, indices, Color.RED);
	} // colorMObjects(String, int[])

	/** Color several objects in an MRV string representing a molecule.
	 * @param	molStr	the MRV string
	 * @param	indices	array of indices of objects to be colored
	 * @param	markColor	color to use
	 * @return	the MRV string with the objects colored
	 */
	public static String colorMObjects(String molStr, int[] indices,
			Color markColor) {
		try {
			final Molecule mol = MolImporter.importMol(molStr);
			MDocument mDoc = mol.getDocument();
			if (mDoc == null) mDoc = new MDocument(mol);
			colorMObjects(mDoc, indices, markColor);
			final String mrv = toString(mDoc, MRV);
			debugPrint("MolString.colorMObjects: returning:\n", mrv);
			return mrv;
		} catch (MolExportException e) {
			return molStr; // no coloring available
		} catch (MolFormatException e) {
			return molStr; // no coloring available
		} // try
	} // colorMObjects(String, int[], Color)

	/** Color an object in an MDocument.
	 * @param	mDoc	the MDocument
	 * @param	objIndex	index of object to be colored
	 */
	public static void colorMObject(MDocument mDoc, int objIndex) {
		colorMObject(mDoc, objIndex, Color.RED);
	} // colorMObject(MDocument, int)

	/** Color an object in an MDocument.
	 * @param	mDoc	the MDocument
	 * @param	objIndex	index of object to be colored
	 * @param	markColor	color to use
	 */
	public static void colorMObject(MDocument mDoc, int objIndex,
			Color markColor) {
		final MObject object = mDoc.getObject(objIndex);
		if (object != null) {
			object.setColor(markColor);
			object.setLineColor(markColor);
		}
	} // colorMObject(MDocument, int, Color)

	/** Color several objects in an MDocument.
	 * @param	mDoc	the MDocument
	 * @param	objIndex1	index of 1st object to be colored
	 * @param	objIndex2	index of 2nd object to be colored
	 */
	public static void colorMObjects(MDocument mDoc, int objIndex1,
			int objIndex2) {
		colorMObjects(mDoc, new int[] {objIndex1, objIndex2});
	} // colorMObjects(MDocument, int, int)

	/** Color several objects in an MDocument.
	 * @param	mDoc	the MDocument
	 * @param	indices	array of indices of objects to be colored
	 */
	public static void colorMObjects(MDocument mDoc, int[] indices) {
		colorMObjects(mDoc, indices, Color.RED);
	} // colorMObjects(MDocument, int[])

	/** Color several objects in an MDocument.
	 * @param	mDoc	the MDocument
	 * @param	indices	array of indices of objects to be colored
	 * @param	markColor	color to use
	 */
	public static void colorMObjects(MDocument mDoc, int[] indices,
			Color markColor) {
		for (final int objIndex : indices) {
			colorMObject(mDoc, objIndex, markColor);
		} // for each given index
	} // colorMObjects(MDocument, int[], Color)

	/** Sets all MPolylines in the MDocument of a molecule, including 
	 * MRectangles, MEFlows, and single- and double-headed arrows, to 
	 * default color (black).
	 * @param	doc	the MDocument
	 * @return	true if color was removed
	 */
	public static boolean uncolorMPolylines(MDocument doc) {
		boolean haveColor = false;
		if (doc == null) return haveColor;
		for (int objNum = 0; objNum < doc.getObjectCount(); objNum++) {
			final MObject obj = doc.getObject(objNum);
			if (obj instanceof MPolyline) {
				final Color color = ((MPolyline) obj).getColor();
				final Color lcolor = ((MPolyline) obj).getLineColor();
				if ((color != null && !color.equals(Color.BLACK))
						|| (lcolor != null && !lcolor.equals(Color.BLACK))) {
					haveColor = true;
					final MPolyline poly = (MPolyline) obj;
					poly.setColor(null);
					poly.setLineColor(null);
				} // if object is colored
			} // if object is MPolyline
		} // for each object in the MDocument
		return haveColor;
	} // uncolorMPolylines(MDocument)

	/** Gets the color of the atom.
	 * @param	atom	the atom
	 * @return	the color of the atom
	 */
	public static Color getColor(MolAtom atom) {
		Color color = Color.BLACK;
		if (atom != null) {
			final int atomSetSeq = atom.getSetSeq();
			final MoleculeGraph molG = atom.getParent();
			MDocument mDoc = molG.getDocument();
			if (mDoc == null) mDoc = new MDocument(molG);
			final int colorInt = mDoc.getAtomSetRGB(atomSetSeq);
			color = new Color(colorInt);
		} // if the atom isn't null
		return color;
	} // getColor(MolAtom)

	/** Sets the color of the atom.
	 * @param	atom	the atom
	 * @param	color	the color
	 */
	public static void setColor(MolAtom atom, Color color) {
		if (atom == null) return;
		final MoleculeGraph molG = atom.getParent();
		MDocument mDoc = molG.getDocument();
		if (mDoc == null) mDoc = new MDocument(molG);
		setColor(atom, molG, mDoc, color);
	} // setColor(MolAtom, Color)

	/** Sets the color of the atom.
	 * @param	atom	the atom
	 * @param	molG	the atom's molecule (as a graph)
	 * @param	mDoc	the molecule's document
	 * @param	color	the color
	 */
	public static void setColor(MolAtom atom, MoleculeGraph molG, 
			MDocument mDoc, Color color) {
		if (atom == null) return;
		int maxSetNum = 0;
		boolean exists = false;
		for (final MolAtom molAtom : molG.getAtomArray()) {
			if (molAtom == atom) continue; // pointer equality
			final int setNum = molAtom.getSetSeq();
			if (setNum > maxSetNum) maxSetNum = setNum;
			final int colorInt = mDoc.getAtomSetRGB(setNum);
			final Color existingColor = new Color(colorInt);
			if (existingColor.equals(color)) {
				atom.setSetSeq(setNum);
				exists = true;
				break;
			} // if the color is already in the set
		} // for each set
		if (!exists) {
			final int newSetNum = maxSetNum + 1;
			mDoc.setAtomSetColorMode(newSetNum, 
					MDocument.SETCOLOR_SPECIFIED);
			mDoc.setAtomSetRGB(newSetNum, color.getRGB());
			atom.setSetSeq(newSetNum);
		} // if we need to add a new color
	} // setColor(MolAtom, MoleculeGraph, MDocument, Color)

	/** Clears the color from the atom.
	 * @param	atom	the atom
	 */
	public static void clearColor(MolAtom atom) {
		if (atom != null) atom.setSetSeq(0);
	} // clearColor(MolAtom)

	/** Gets if an MDocument object is a straight arrow.
	 * @param	mObject	an MDocument object
	 * @return	true if the object is a reaction or resonance arrow
	 */
	public static boolean isReactionArrow(MObject mObject) {
		boolean isArrow = false;
		if (isLineOrRxnArrow(mObject)) {
			isArrow = ((MPolyline) mObject).isArrow();
			debugPrint("MolString.isReactionArrow: isArrow = ", isArrow);
		} // if is line or rxn arrow
		return isArrow;
	} // isReactionArrow(MObject)

	/** Gets if an MDocument object is a line or arrow but not an electron-flow
	 * arrow.
	 * @param	mObject	an MDocument object
	 * @return	true if the object is a line or arrow but not an electron-flow
	 * arrow
	 */
	public static boolean isLineOrRxnArrow(MObject mObject) {
		final String SELF = "MolString.isLineOrRxnArrow: ";
		final boolean isLineOrRxnArrow =
				(mObject instanceof MPolyline				// object is an MPolyline
						&& !(mObject instanceof MRectangle) // but not a Rectangle
						&& !(mObject instanceof MEFlow));	// and not a flow
		debugPrint(SELF, isLineOrRxnArrow
				? "MPolyline but not MRectangle or MEFlow."
				: "either not MPolyline, or is MRectangle or MEFlow.");
		return isLineOrRxnArrow;
	} // isLineOrRxnArrow(MObject)

	/** Gets the first reaction arrow in a molecule's document.
	 * @param	mol	the molecule
	 * @return	the first reaction arrow, or null if not found
	 */
	public static MPolyline getReactionArrow(Molecule mol) {
		MPolyline rxnArrow = null;
		final MDocument mDoc = mol.getDocument();
		if (mDoc != null) for (final MObject mObj : mDoc.getAllObjects()) {
			if (isReactionArrow(mObj)) {
				rxnArrow = (MPolyline) mObj;
				break;
			} // if the graphical object is an arrow
		} // for each graphical object
		return rxnArrow;
	} // getReactionArrow(Molecule)

	/** When Marvin imports a MarvinJS drawing that contains rectangles, it
	 * moves the rectangles dowward by the height of the rectangle.
	 * @param	mDoc	the document whose rectangles need to be adjusted
	 */
	public static void adjustRectangles(MDocument mDoc) {
		final String SELF = "MolString.adjustRectangles: ";
		if (mDoc == null) return;
		debugPrintMRV(SELF + "original document:\n", mDoc);
		final List<MPolyline> rxnArrows = new ArrayList<MPolyline>();
		for (int objIndex = 0; objIndex < mDoc.getObjectCount(); objIndex++) {
			final MObject mObject = mDoc.getObject(objIndex);
			if (isReactionArrow(mObject)) {
				rxnArrows.add((MPolyline) mObject);
			} // if is reaction arrow
		} // for each object
		for (int objIndex = 0; objIndex < mDoc.getObjectCount(); objIndex++) {
			final MObject mObject = mDoc.getObject(objIndex);
			if (mObject instanceof MRectangle) {
				final MRectangle rect = (MRectangle) mObject;
				final MPoint cornerNW = 
						rect.getPointRef(MRectangle.P_NW, null);
				final MPoint cornerSW = 
						rect.getPointRef(MRectangle.P_SW, null);
				final DPoint3 cornerNWLoc = cornerNW.getLocation();
				final double height = cornerSW.distanceFrom(cornerNWLoc.x, 
						cornerNWLoc.y, null);
				debugPrint(SELF + "rectangle with object index ", objIndex + 1,
						":\n\tNW = ", cornerNWLoc,
						"\n\tNE = ", rect.getPointRef(MRectangle.P_NE, null).getLocation(),
						"\n\tSE = ", rect.getPointRef(MRectangle.P_SE, null).getLocation(),
						"\n\tSW = ", cornerSW.getLocation(),
						"\n\theight = ", height);
				for (final MPolyline arrow : rxnArrows) {
					final MPoint[] arrowPts = arrow.getPoints();
					for (int ptNum = 0; ptNum < 2; ptNum++) {
						final boolean isTail = ptNum == 0;
						final MPoint arrowPt = arrowPts[ptNum];
						if (!(arrowPt instanceof MRectanglePoint)) {
							final DPoint3 arrowPtLoc = arrowPt.getLocation();
							if (VectorMath.pointInRect(arrowPtLoc, rect, 1.03)) {
								debugPrint(SELF + "arrow with object index ",
										mDoc.indexOf(arrow) + 1, " has a ",
										isTail ? "tail" : "head",
										" in or near current rectangle; "
										+ "adjusting endpoint");
								// arrowPtLoc.y += height;
								arrowPts[ptNum] = new MPoint(arrowPtLoc);
							} // if point is in current rectangle
						} // if is not rectangle point
					} // for tail and head of arrow
					arrow.setPoints(arrowPts);
				} // for each object
				final MPoint center = rect.getPointRef(MRectangle.P_CENTER, null);
				final DPoint3 centerLoc = center.getLocation();
				centerLoc.y += height;
				center.setLocation(centerLoc, null);
				for (int ptNum = 0; ptNum <= 2; ptNum++) {
					final int pt = (ptNum == 0 ? MRectangle.P_SW : MRectangle.P_SE);
					final MPoint corner = rect.getPointRef(pt, null);
					final DPoint3 cornerLoc = corner.getLocation();
					cornerLoc.y -= 0.5 * height;
					corner.setLocation(cornerLoc, null);
				} // for each rectangle point
				debugPrint(SELF + "rectangle with object index ", objIndex + 1,
						" after adjusting:",
						"\n\tNW = ", rect.getPointRef(MRectangle.P_NW, null).getLocation(),
						"\n\tNE = ", rect.getPointRef(MRectangle.P_NE, null).getLocation(),
						"\n\tSE = ", rect.getPointRef(MRectangle.P_SE, null).getLocation(),
						"\n\tSW = ", rect.getPointRef(MRectangle.P_SW, null).getLocation());
			} // if is rectangle
		} // for each object in the MDocument
		debugPrintMRV(SELF + "new document:\n", mDoc);
	} // adjustRectangles(MDocument)

	/** When Marvin imports a MarvinJS drawing that contains arrows that start 
	 * or end at rectangle points, it flips north and south, so arrows 
	 * originating at NW corners now originate at SW corners, etc.; this method 
	 * flips them back.
	 * @param	mDoc	the document whose rectangles need flipping
	 */
	public static void adjustArrowsAttachedToRectangles(MDocument mDoc) {
		final String SELF = "MolString.adjustArrowsAttachedToRectangles: ";
		if (mDoc == null) return;
		debugPrintMRV(SELF + "original document:\n", mDoc);
		// some of the values returned by getPositionInRect() don't 
		// correspond to the constant values
		final int[] posnInRectConverter = new int[] {
				MRectangle.P_SW,
				MRectangle.P_SE,
				MRectangle.P_NE,
				MRectangle.P_NW,
				MRectangle.P_S - 1,
				MRectangle.P_E - 1,
				MRectangle.P_N - 1,
				MRectangle.P_W - 1};
		final String[] POSN_STR = new String[] {
			 	"NW", "NE", "SE", "SW", "N", "E", "S", "W"};
		for (int objIndex = 0; objIndex < mDoc.getObjectCount(); objIndex++) {
			final MObject mObject = mDoc.getObject(objIndex);
			if (isReactionArrow(mObject)) {
				final MPolyline arrow = (MPolyline) mObject;
				final MPoint[] arrowPts = arrow.getPoints();
				debugPrint(SELF + "examining arrow with object number ",
						objIndex + 1, " with ", arrowPts.length, " points.");
				for (int ptNum = 0; ptNum < 2; ptNum++) {
					final MPoint arrowPt = arrowPts[ptNum];
					if (arrowPt instanceof MRectanglePoint) {
						final MRectanglePoint arrowRectPt =
								(MRectanglePoint) arrowPt;
						final MRectangle arrowRect = 
								arrowRectPt.getParentRect();
						final int arrowRectPosn =
								arrowRectPt.getPositionInRect();
						final int newArrowRectPosn = 
								posnInRectConverter[arrowRectPosn];
						debugPrint(SELF + "point ", ptNum + 1, 
								" of arrow with index ", objIndex + 1, 
								" has box point ", arrowRectPosn, 
								" = ", POSN_STR[arrowRectPosn],
								"; resetting to ", newArrowRectPosn,
								" = ", POSN_STR[newArrowRectPosn]);
						final MRectanglePoint newArrowRectPt =
								(MRectanglePoint) 
								arrowRect.getPointRef(newArrowRectPosn, null);
						arrowPts[ptNum] = newArrowRectPt;
						arrow.setPoints(arrowPts);
					} // if arrow starts or ends at box
				} // for tail and head
			} // if is reaction arrow
		} // for each object in the MDocument
		debugPrintMRV(SELF + "new document:\n", mDoc);
	} // adjustArrowsAttachedToRectangles(MDocument)

	/** When a user draws a single reaction arrow, MarvinSketch may duplicate
	 * some of the existing objects; this method removes those duplicate objects.
	 * @param	mDoc	the document whose objects may be duplicated
	 */
	public static void removeDuplicateObjects(MDocument mDoc) {
		final String SELF = "MolString.removeDuplicateObjects: ";
		if (mDoc == null) return;
		debugPrintMRV(SELF + "original document:\n", mDoc);
		final int NW = MRectangle.P_NW;
		final int SE = MRectangle.P_SE;
		final List<MObject> mObjects = mDoc.getAllObjects();
		final List<MEFlow> mechFlows = new ArrayList<MEFlow>();
		final List<MRectangle> rectangles = new ArrayList<MRectangle>();
		final List<MPolyline> arrows = new ArrayList<MPolyline>();
		int objNum = 1;
		for (final MObject mObject : mObjects) {
			if (mObject instanceof MEFlow) {
				final MEFlow newFlow = (MEFlow) mObject;
				boolean isDuplicate = false;
				for (final MEFlow prevFlow : mechFlows) {
					isDuplicate = newFlow.getNumElectrons() 
								== prevFlow.getNumElectrons()
							&& newFlow.sourceIsAtom() == prevFlow.sourceIsAtom()
							&& newFlow.sinkIsAtom() == prevFlow.sinkIsAtom()
							&& newFlow.sinkIsBond() == prevFlow.sinkIsBond();
					if (isDuplicate) {
						final MolAtom[] newSrcAtoms = newFlow.getSourceAtoms();
						final MolAtom[] newSinkAtoms = newFlow.getSinkAtoms();
						final MolAtom[] prevSrcAtoms = prevFlow.getSourceAtoms();
						final MolAtom[] prevSinkAtoms = prevFlow.getSinkAtoms();
						isDuplicate = newSrcAtoms[0] == prevSrcAtoms[0]
								&& (newFlow.sourceIsAtom() 
									|| newSrcAtoms[1] == prevSrcAtoms[1])
								&& newSinkAtoms[0] == prevSinkAtoms[0]
								&& (newFlow.sourceIsAtom() 
									|| newSinkAtoms[1] == prevSinkAtoms[1]);
					} // if have same electron count and same kinds of source and sink
					if (isDuplicate) break;
				} // for each flow
				if (isDuplicate) {
					mDoc.removeObject(mObject);
					debugPrint(SELF + "removing e-flow arrow ", objNum,
							" from document as a duplicate.");
				} else mechFlows.add(newFlow);
			} else if (mObject instanceof MRectangle) { // or MTextBox
				final MRectangle newRect = (MRectangle) mObject;
				boolean isDuplicate = false;
				for (final MRectangle prevRect : rectangles) {
					isDuplicate = newRect.getPoint(NW).equals(
								prevRect.getPoint(NW))
							&& newRect.getPoint(SE).equals(
								prevRect.getPoint(SE))
							&& (!(mObject instanceof MTextBox)
								|| ((MTextBox) newRect).getText().equals(
									((MTextBox) prevRect).getText()));
					if (isDuplicate) break;
				} // for each rectangle
				if (isDuplicate) {
					mDoc.removeObject(mObject);
					debugPrint(SELF + "removing rectangle or textbox ", objNum,
							" from document as a duplicate.");
				} else rectangles.add(newRect);
			} else if (mObject instanceof MPolyline) {
				final MPolyline newArrow = (MPolyline) mObject;
				boolean isDuplicate = false;
				for (final MPolyline prevArrow : arrows) {
					isDuplicate = newArrow.getPoint(HEAD).equals(
								prevArrow.getPoint(HEAD))
							&& newArrow.getPoint(TAIL).equals(
								prevArrow.getPoint(TAIL))
							&& newArrow.getPoint(MIDPT).equals(
								prevArrow.getPoint(MIDPT))
							&& newArrow.getArrowLength(HEAD) ==
								prevArrow.getArrowLength(HEAD)
							&& newArrow.getArrowLength(TAIL) ==
								prevArrow.getArrowLength(TAIL);
					if (isDuplicate) break;
				} // for each arrow
				if (isDuplicate) {
					mDoc.removeObject(mObject);
					debugPrint(SELF + "removing arrow ", objNum,
							" from document as a duplicate.");
				} else arrows.add(newArrow);
			} // if mObject
			objNum++;
		} // for each object in the MDocument
		debugPrintMRV(SELF + "new document:\n", mDoc);
	} // removeDuplicateObjects(MDocument)

	/** Disables external instantiation. */
	private MolString() { }

} // MolString
