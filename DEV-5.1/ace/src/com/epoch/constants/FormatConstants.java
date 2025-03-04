package com.epoch.constants;

/** Holds constants used for molecule formats. */
public interface FormatConstants {

	/** Parameter for MolExporter.exportToFormat(). P formats the MRV with 
	 * indents and return characters; S stores atom selection information. */
	public static final String MRV = "mrv:PS";
	/** Parameter for MolExporter.exportToFormat(). -A turns off the export of
	 * atom properties, which would otherwise generate an exception. */
	public static final String SMILES = "cxsmiles:-A";
	/** Parameter for MolExporter.exportToFormat(). -A turns off the export of
	 * atom properties, which would otherwise generate an exception. */
	public static final String SMARTS = "cxsmarts:-A";
	/** Parameter for MolExporter.exportToFormat(). */
	public static final String MOL = "mol";
	/** Parameter for MolExporter constructor. */
	public static final String PNG = "png";
	/** Parameter for MolExporter constructor. */
	public static final String SVG = "svg";
	/** Parameter for MolExporter constructor. */
	public static final String IMG = "img";
	/** MRV representation of an empty molecule. */
	public static final String EMPTY_MRV =
			"<?xml version=\"1.0\" ?>\n<MDocument>\n</MDocument>\n";
	/** Whether to format text according to chemistry conventions. */
	public static final boolean CHEM_FORMATTING = true;

} // FormatConstants
