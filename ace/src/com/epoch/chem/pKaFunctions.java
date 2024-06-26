package com.epoch.chem;

import com.epoch.chem.chemConstants.ChemConstants;
import com.epoch.utils.Utils;
import chemaxon.struc.MolAtom;
import chemaxon.struc.MolBond;
import chemaxon.struc.Molecule;
import chemaxon.struc.MoleculeGraph;
import chemaxon.struc.PeriodicSystem;
import chemaxon.marvin.calculations.pKaPlugin;
import chemaxon.marvin.plugin.PluginException;
import java.text.NumberFormat;

/** Contains methods for calculating p<i>K<sub>a</sub></i>s.  */
public final class pKaFunctions implements ChemConstants {

	private static void debugPrint(Object... msg) { 
		// Utils.printToLog(msg);
	}

	private static void debugPrintMRV(Object... msg) {
		// Utils.printToLog(msg, MRV);
	}

	/** Smallest pKa to calculate.   */
	public static final double SMALLEST_PKA = -15;
	/** Largest pKa to calculate.   */
	public static final double LARGEST_PKA = 70;
	/** Member representing the pKa in pKapKb and related arrays. */
	private static final int ACIDIC = 0;
	/** Member representing the pKb in pKapKb and related arrays. */
	private static final int BASIC = 1;
	/** Value of pKa or pKb. */
	private static double NO_VALUE = Double.NaN;
	/** The element C. */
	private static final int C = PeriodicSystem.C;
	/** The element H. */
	private static final int H = PeriodicSystem.H;
	/** The element N. */
	private static final int N = PeriodicSystem.N;
	/** The element O. */
	private static final int O = PeriodicSystem.O;
	/** The element Si. */
	private static final int Si = PeriodicSystem.Si;

	/** Gets the smallest p<i>K<sub>a</sub></i> and largest 
	 * p<i>K<sub>b</sub></i> of a molecule.  The molecule is modified; clone it 
	 * before sending it here.
	 * @param	molecule	a molecule
	 * @return	an array in which the first value is the smallest 
	 * p<i>K<sub>a</sub></i>, and the second is the largest 
	 * p<i>K<sub>b</sub></i> (defined as the p<i>K<sub>a</sub></i> of the 
	 * conjugate acid)
	 * @throws	PluginException	when the plugin can't calculate the pKa
	 */
	public static double[] pKapKbMolecule(Molecule molecule)
			throws PluginException {
		return pKapKbMolecule(molecule, -1);
	} // pKapKbMolecule(Molecule)

	/** Gets the smallest p<i>K<sub>a</sub></i> and largest 
	 * p<i>K<sub>b</sub></i> of a molecule.  The molecule is modified; clone it 
	 * before sending it here.
	 * @param	molecule	a molecule
	 * @param	stageIndex	if molecule is part of a mechanism, index of the
	 * stage to which it belongs; provided just for the log
	 * @return	an array in which the first value is the smallest
	 * p<i>K<sub>a</sub></i>, and the second is the largest 
	 * p<i>K<sub>b</sub></i> (defined as the p<i>K<sub>a</sub></i> of the 
	 * conjugate acid)
	 * @throws	PluginException	when the plugin can't calculate the pKa
	 */
	public static double[] pKapKbMolecule(Molecule molecule, int stageIndex)
			throws PluginException {
		final String SELF = "pKaFunctions.pKapKbMolecule: ";
		double molMostBasic = SMALLEST_PKA;
		double molMostAcidic = LARGEST_PKA;
		final double[][] allPKaPKbs = pKapKbAtoms(molecule, stageIndex);
		for (int atmIdx = 0; atmIdx < molecule.getAtomCount(); atmIdx++) {
			final double[] pKaPKb = allPKaPKbs[atmIdx];
			if (!noValue(pKaPKb[ACIDIC]) && pKaPKb[ACIDIC] < molMostAcidic)
				molMostAcidic = pKaPKb[ACIDIC];
			if (!noValue(pKaPKb[BASIC]) && pKaPKb[BASIC] > molMostBasic)
				molMostBasic = pKaPKb[BASIC];
		} // for each atom in the molecule
		debugPrint(SELF + "for ", molecule, ", molMostAcidic = ",
				format(molMostAcidic), ", molMostBasic = ", 
				format(molMostBasic));
		return new double[] {molMostAcidic, molMostBasic};
	} // pKapKbMolecule(Molecule, int)

	/** Gets the p<i>K<sub>a</sub></i> and p<i>K<sub>b</sub></i> of each atom 
	 * in a molecule.  The molecule is modified; clone it before sending it 
	 * here.
	 * @param	molecule	a molecule
	 * @return	an array of arrays; first dimension is the atom index; in second
	 * dimension, first value is the smallest p<i>K<sub>a</sub></i>, and the 
	 * second is the largest p<i>K<sub>b</sub></i> (defined as the 
	 * p<i>K<sub>a</sub></i> of the conjugate acid)
	 * @throws	PluginException	when the plugin can't calculate the pKa
	 */
	public static double[][] pKapKbAtoms(Molecule molecule)
			throws PluginException {
		return pKapKbAtoms(molecule, -1);
	} // pKapKbAtoms(Molecule)

	/** Gets the p<i>K<sub>a</sub></i> and p<i>K<sub>b</sub></i> of each atom 
	 * in a molecule.  The molecule is modified; clone it before sending it 
	 * here.
	 * @param	origMol	a molecule
	 * @param	stageIndex	if molecule is part of a mechanism, index of the
	 * stage to which it belongs; provided just for the log
	 * @return	an array of arrays; first dimension is the atom index; in second
	 * dimension, first value is the smallest p<i>K<sub>a</sub></i>, and the 
	 * second is the largest p<i>K<sub>b</sub></i> (defined as the 
	 * p<i>K<sub>a</sub></i> of the conjugate acid)
	 * @throws	PluginException	when the plugin can't calculate the pKa
	 */
	public static double[][] pKapKbAtoms(Molecule origMol, int stageIndex)
			throws PluginException {
		final String SELF = "pKaFunctions.pKapKbAtoms: ";
		debugPrintMRV(SELF + "getting pKas for:\n", origMol);
		final NumberFormat formatter = getFormatter(); 
		final Molecule molecule = origMol; // we may reassign it. 
		ChemUtils.stripMetalsNoClone(molecule); // replace metals with - charges
		final double[][] specialCasePKs = specialCasePKs(origMol);
		if (!Utils.isEmpty(specialCasePKs)) {
			debugPrint(SELF + "special case of values for ", origMol,
					": ", format(specialCasePKs, formatter));
			return specialCasePKs;
		} // if the molecule is a special case
		final int atmCt = molecule.getAtomCount(); // get after stripping metals
		final double[][] allPKaPKbs = new double[atmCt][2];
		try {
			molecule.aromatize(MoleculeGraph.AROM_BASIC);
			molecule.calcHybridization();
			final pKaPlugin plugin = new pKaPlugin();
			// next line makes where pKa and pKb are
			// stored NOT dependent on charge of atom
			plugin.setMicropKaCalc(true);
			plugin.setpKaPrefixType(pKaPlugin.DYNAMICpKaPREFIX);
			plugin.setMaxIons(8);
			plugin.setBasicpKaLowerLimit(SMALLEST_PKA);
			plugin.setAcidicpKaUpperLimit(LARGEST_PKA);
			plugin.setConsiderTautomerization(true);
			debugPrintMRV(SELF + "setting pKaPlugin molecule to:\n", origMol);
			plugin.setMolecule(molecule);
			plugin.run();
			for (int atmIdx = 0; atmIdx < atmCt; atmIdx++) {
				final MolAtom atom = molecule.getAtom(atmIdx);
				if (ChemUtils.isMulticenterAtom(atom)
						|| couldBeImplicitH(atom)) {
					allPKaPKbs[atmIdx] = new double[] {NO_VALUE, NO_VALUE};
				} else {
					final boolean bears_H = 
							atom.getImplicitHcount() 
							+ atom.getExplicitHcount() > 0;
					final double[] jchemPKas = (!bears_H ? null
							: plugin.getpKaValues(atmIdx, pKaPlugin.ACIDIC));
					final double[] jchemPKbs =
							plugin.getpKaValues(atmIdx, pKaPlugin.BASIC);
					// get first pKa values as calculated by JChem
					allPKaPKbs[atmIdx][ACIDIC] = (Utils.isEmpty(jchemPKas) 
							? NO_VALUE : jchemPKas[0]);
					// ignore Jlint complaint about line above.  Raphael 11/2010
					allPKaPKbs[atmIdx][BASIC] = (Utils.isEmpty(jchemPKbs) 
							? NO_VALUE : jchemPKbs[0]);
					if (atom.getAtno() == C) {
						// if plugin has failed to give value, guess
						if (noValue(allPKaPKbs[atmIdx][ACIDIC]) 
								&& atom.getCharge() == 0 && bears_H) {
							allPKaPKbs[atmIdx][ACIDIC] = 
									getCpK(molecule, atmIdx);
							debugPrint(SELF + "guessed pKa of CH is ",
									format(allPKaPKbs[atmIdx][ACIDIC], formatter));
						} else if (noValue(allPKaPKbs[atmIdx][BASIC]) 
								&& atom.getCharge() == -1) {
							allPKaPKbs[atmIdx][BASIC] = 
									getCpK(molecule, atmIdx);
							debugPrint(SELF + "guessed pKb of C(-) is ",
									format(allPKaPKbs[atmIdx][BASIC], formatter));
						} // if should guess C pKa or pKb
					} // if it's a C atom
				} // if could be implicit H
				debugPrint(SELF, stageIndex >= 0 
							? "stage " + (stageIndex + 1) + " " : "", 
						noValue(allPKaPKbs[atmIdx][ACIDIC]) 
								? " has no acidic pKa" : " has acidic pKa " 
									+ format(allPKaPKbs[atmIdx][ACIDIC], formatter),
						noValue(allPKaPKbs[atmIdx][BASIC]) 
								? ", no basic pKa" : ", basic pKa " 
									+ format(allPKaPKbs[atmIdx][BASIC], formatter));
			} // for each atom in the molecule
			debugPrint(SELF + "returning ", format(allPKaPKbs, formatter));
		} catch (PluginException e) {
			e.printStackTrace();
			throw new PluginException(SELF + "PluginException thrown on "
					+ "molecule:\n" + MolString.toString(molecule, MRV)
					+ "\n" + e.getMessage());
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new RuntimeException(SELF + "RuntimeException thrown on "
					+ "molecule:\n" + MolString.toString(molecule, MRV)
					+ "\n" + e.getMessage());
		} // try
		return allPKaPKbs;
	} // pKapKbAtoms(Molecule, int)

	/** Guesses p<i>K<sub>a</sub></i> of a C atom that JChem doesn't calculate.
	 * Assumes calling method has already checked that atom is C and bears H.
	 * Doesn't look for carbonyls, etc. because JChem already checks for those.
	 * @param	mol	a molecule
	 * @param	atmIndex	C atom whose p<i>K<sub>a</sub></i> to guess
	 * @return	a p<i>K<sub>a</sub></i> value for the C atom
	 */
	private static double getCpK(Molecule mol, int atmIndex) {
		final String SELF = "pKaFunctions.getCpK: ";
		// pKa values in H2O; obtained largely from
		// http:// daecr1.harvard.edu/pdf/evans_pKa_table.pdf
		double pK = 51; // approximate value for alkane CH2
		final MolAtom atom = mol.getAtom(atmIndex);
		int hybridizn = atom.getHybridizationState();
		if (hybridizn == MolAtom.HS_UNKNOWN) {
			debugPrint(SELF + "Recalculating hybridization");
			mol.calcHybridization();
			hybridizn = atom.getHybridizationState();
		} // if hybridization needs to be recalculated
		if (hybridizn == MolAtom.HS_SP) {
			debugPrint(SELF + "Hybridization is sp");
			pK = 25; // value for unconjugated alkyne
			// if it's a conjugated alkyne, reduce the pKa
			for (final MolAtom lig : atom.getLigands()) {
				final MolBond bond = atom.getBondTo(lig);
				if (bond.getType() == 3) {
					if (lig.getAtno() == C) {
						for (final MolAtom ligNxt : lig.getLigands()) {
							if (ligNxt != atom) { // pointer equality
								hybridizn = ligNxt.getHybridizationState();
								if (ligNxt.getAtno() == C
										&& Utils.among(hybridizn, 2, 3))
									pK = 23; // value for PhCCH
								else if (ligNxt.getAtno() == Si)
									pK = 19; // value for Me3SiCCH (?)
							} // if this is not the starting C(sp)
						} // for each atom attached to lig
					} else { // triple bond to a heteroatom, prob. N
						pK = 9.2; // value for HCN
						// from http:// research.chem.psu.edu/brpgroup/
						// 	pKa_compilation.pdf
					} // if the triply bonded lig is C
				} // if this bond is triple
			} // for each bond attached to atom
		} else if (hybridizn == MolAtom.HS_SP2) {
			debugPrint(SELF + "Hybridization is sp2");
			pK = 43; // value for C6H6
		} else { // sp3 hybridization
			debugPrint(SELF + "Hybridization is sp3");
			// if it's allylic or benzylic, reduce the pKa
			for (final MolAtom lig : atom.getLigands()) {
				hybridizn = lig.getHybridizationState();
				if (lig.getAtno() == C
						&& Utils.among(hybridizn, MolAtom.HS_SP2, 
							MolAtom.HS_SP)) {
					// values for PhCH3, Ph2CH2, Ph3CH, CC=[O,N]
					debugPrint(SELF + "allylic or propargylic");
					pK = (pK == 51? 41 : pK == 41? 33.5 : 31.5);
					for (final MolAtom ligNxt : lig.getLigands()) {
						if (ligNxt != atom) { // pointer equality
							hybridizn = ligNxt.getHybridizationState();
							if (Utils.among(ligNxt.getAtno(), O, N) 
									&& lig.getBondTo(ligNxt).getType() > 1
									&& pK > 25)
								pK = 25;
						} // if this is not the starting C(sp)
					} // for each atom attached to lig
				} // if C(sp3) atom is attached to C(sp2)
			} // for each atom attached to atom
		} // atom's hybridization
		Utils.alwaysPrint(SELF + "JChem did not get pK of C", 
				atmIndex + 1, " of ", mol, "; ACE guesses ", pK); /**/
		return pK;
	} // getCpK(Molecule, int)

	/** Gets the smallest p<i>K<sub>a</sub></i> and largest 
	 * p<i>K<sub>b</sub></i> of molecules that JChem miscalculates.
	 * Values come from http://daecr1.harvard.edu/pdf/evans_pKa_table.pdf.
	 * @param	molecule	a molecule
	 * @return	pKa and pKb values of each atom if it's a special case, 
	 * null otherwise
	 */
	private static double[][] specialCasePKs(Molecule molecule) {
		final String SELF = "pKaFunctions.specialCasePKs: ";
		final String molStr = MolString.toString(molecule, SMILES);
		if ("[H-]".equals(molStr)) {
			return new double[][] {{NO_VALUE, 36}};
		} else if ("[H][H]".equals(molStr)) {
			return new double[][] {{36, NO_VALUE}, {36, NO_VALUE}};
		} // special cases not handled by JChem
		return new double[][] {};
	} // specialCasePKs(Molecule)

	/** Gets if the number is Double.NaN.
	 * @param	num	the number
	 * @return true if the number is NaN
	 */
	private static boolean noValue(double num) {
		return Double.isNaN(num);
	} // noValue(double)

	/** Gets if this atom is uncharged H.
	 * @param	atom	the atom
	 * @return	true if it is an uncharged H atom
	 */
	private static boolean couldBeImplicitH(MolAtom atom) {
		return atom.getAtno() == H && atom.getCharge() == 0;
	} // couldBeImplicitH(MolAtom)

	/** Truncates a 2D array of doubles to one digit past the decimal point.
	 * @param	nums	the 2D array of doubles
	 * @return	the 2D array of truncated doubles
	 */
	private static String[][] format(double[][] nums) {
		final NumberFormat formatter = getFormatter(); 
		return format(nums, formatter);
	} // format(double[][])

	/** Truncates a 2D array of doubles to one digit past the decimal point.
	 * @param	nums	the 2D array of doubles
	 * @param	formatter	used to format the number
	 * @return	the 2D array of truncated doubles
	 */
	private static String[][] format(double[][] nums, NumberFormat formatter) {
		final int xLen = nums.length;
		final int yLen = (xLen > 0 ? nums[0].length : 0);
		final String[][] formatted = new String[xLen][yLen];
		for (int x = 0; x < xLen; x++) {
			for (int y = 0; y < yLen; y++) {
				formatted[x][y] = format(nums[x][y], formatter);
			} // for each y
		} // for each x
		return formatted;
	} // format(double[][], NumberFormat)

	/** Truncates a double to one digit past the decimal point.
	 * @param	num	the double
	 * @return	the truncated double
	 */
	private static String format(double num) {
		return format(num, getFormatter());
	} // format(double)

	/** Gets a formatter to truncate a double to one digit past the decimal 
	 * point.
	 * @return	a formatter
	 */
	private static NumberFormat getFormatter() {
		final NumberFormat formatter = NumberFormat.getInstance(); 
		formatter.setMaximumFractionDigits(1);
		formatter.setMinimumFractionDigits(1);
		return formatter;
	} // getFormatter()

	/** Truncates a double to one digit past the decimal point.
	 * @param	num	the double
	 * @param	formatter	the formatter
	 * @return	the truncated double
	 */
	private static String format(double num, NumberFormat formatter) {
		return (noValue(num) ? "N" : formatter.format(num));
	} // format(double, NumberFormat)

	/** Disables external instantiation. */
	private pKaFunctions() { }

} // pKaFunctions
