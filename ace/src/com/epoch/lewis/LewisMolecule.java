package com.epoch.lewis;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.struc.MDocument;
import chemaxon.struc.Molecule;
import chemaxon.struc.MolAtom;
import com.epoch.chem.ChemUtils;
import com.epoch.chem.FormulaFunctions;
import com.epoch.chem.MolString;
import com.epoch.exceptions.VerifyException;
import com.epoch.lewis.lewisConstants.LewisConstants;
import com.epoch.utils.Utils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/** Encodes a molecule with unshared electrons associated with each atom.
 * Imports the molecule from an MRV or extended MOL representation.
 * The line in the MOL string
 * that indicates unshared electrons has the format:
 * <br><pre>M  LNE  2   1   4   2   2</pre><br>
 * where the first number indicates the number of atoms with unshared
 * electrons, and each subsequent pair of numbers indicates the 1-based atom
 * index and the number of electrons on that atom.  There can be more than
 * one such line, but they should be consecutive.
 */
public class LewisMolecule implements LewisConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Extended MOL representation of this Lewis molecule. */
	transient private String molStr;
	/** JChem Molecule format of this Lewis molecule. */
	private final Molecule molecule;
	/** JChem MDocument format of this Lewis molecule. */
	transient private MDocument mDoc;
	/** Total number of unshared electrons in this Lewis molecule. */
	transient private int totalUnsharedElecs;

	/** Constructor.
	 * @param	molSt	MOL representation of the Lewis molecule.
	 * @throws	VerifyException	if the molecule can't be imported or some other
	 * error occurs
	 */
	public LewisMolecule(String molSt) throws VerifyException {
		final String SELF = "LewisMolecule: ";
		try {
			molStr = molSt;
			debugPrint(SELF + "molStr:\n", molStr);
			molecule = MolImporter.importMol(molStr);
			mDoc = molecule.getDocument();
			if (mDoc == null) mDoc = new MDocument(molecule);
			if (molStr.indexOf("<?xml") >= 0) {
				setTotalUnsharedElectrons(); // MRV format
			} else {
				setUnsharedElectrons(); // extended MOL format
			} // if is in XML format
			// make word "Lewis" appear in MRV representation
			molecule.setProperty(LEWIS_PROPERTY, TRUE);
		} catch (MolFormatException e) {
			e.printStackTrace();
			throw new VerifyException("LewisMolecule: Unable to import "
					+ "the molecule.\n" + e.getMessage());
		} catch (Exception exe) {
			exe.printStackTrace();
			throw new VerifyException(SELF + exe.getMessage());
		}
	} // LewisMolecule(String)

	/** Calculates the total number of unshared electrons in this Lewis molecule.  */
	final public void setTotalUnsharedElectrons() { 
		totalUnsharedElecs = 0;
		for (final MolAtom atom : getAtoms()) {
			totalUnsharedElecs += ChemUtils.getElectrons(atom);
		} // for each atom
		debugPrint("LewisMolecule.setTotalUnsharedElectrons: Molecule ", 
				molecule, " has ", totalUnsharedElecs, " unshared electrons.");
	} // setTotalUnsharedElectrons()

	/** Gets the number of unshared electrons of each atom from an MRV or 
	 * MOL string and calculates the total number of electrons.
	 * The line in the MOL string
	 * that indicates unshared electrons has the format:
	 * <br><pre>M  LNE  2   4   2  15   4</pre><br>
	 * where the first number indicates the number of atoms with unshared
	 * electrons, and each subsequent pair of numbers indicates the 1-based atom
	 * index and the number of electrons on that atom.  There can be more than
	 * one such line, but they should be consecutive.
	 */
	private final void setUnsharedElectrons() {
		final String SELF = "LewisMolecule.setUnsharedElectrons: ";
		molStr = MolString.decompressMol(molStr);
		final String[] lines = molStr.split("\\n");
		boolean foundLNE = false;
		totalUnsharedElecs = 0;
		for (String line : lines) {
			// shouldn't be leading spaces in the LNE line, but trim in case
			line = line.trim();
			if (line.startsWith("M  LNE")) {
				foundLNE = true;
				line = " " + line.substring("M  LNE  n".length()).trim() + "  ";
						// spaces needed to preserve register
				final int lineLen = line.length();
				int posn = 0;
				try {
					while (posn + MOL_CELL_LEN <= lineLen) {
						final Integer atomIndex = getCellValue(line, posn);
						posn += MOL_CELL_LEN + CELL_SEP_LEN;
						final Integer numUnsharedElecs = getCellValue(line, posn);
						debugPrint(SELF + "atom ", atomIndex, " has ",
								numUnsharedElecs, " unshared electrons.");
						final MolAtom atom = getAtom(atomIndex);
						if (atom != null) {
							ChemUtils.setElectrons(atom, numUnsharedElecs);
						} // if there's an atom
						totalUnsharedElecs += numUnsharedElecs.intValue();
						posn += MOL_CELL_LEN + CELL_SEP_LEN;
					} // while we have atoms with unshared electrons listed
				} catch (IndexOutOfBoundsException x) {
					Utils.alwaysPrint("LewisMolecule.setUnsharedElectrons:"
							+ " malformed numbers bloc of LNE line:\n", line);
				} // try
			} else if (foundLNE) break; // there aren't any more LNE lines
		} // for each line in MOL string
		debugPrint(SELF + "Molecule ", molecule, " has ",
				totalUnsharedElecs, " unshared electrons.");
	} // setUnsharedElectrons()

	/** Utility class for setUnsharedElectrons(). Gets the value of one
	 * cell in the MOL line.
	 * @param	line	line of the MOL file
	 * @param	posn	position of the cell in the line
	 * @return	Integer value of the cell
	 */
	private Integer getCellValue(String line, int posn) {
		final String cell = line.substring(posn, posn + MOL_CELL_LEN);
		debugPrint("LewisMolecule.getCellValue: line.substring(posn) = ",
				line.substring(posn),
				"\nand line.substring(posn, posn + MOL_CELL_LEN) = ",
				line.substring(posn, posn + MOL_CELL_LEN));
		return Integer.decode(cell.trim());
	} // getCellValue(String, int)

	/** Gets the string representation of this Lewis molecule.
	 * @return	the MOL or MRV string
	 */
	public String getMolStr() 				{ return molStr; }
	/** Gets the Molecule representation of this Lewis molecule.
	 * @return	the Molecule
	 */
	public Molecule getMolecule() 			{ return molecule; }
	/** Gets the atoms in this Lewis molecule.
	 * @return	the array of atoms
	 */
	final public MolAtom[] getAtoms()	 	{ return molecule.getAtomArray(); }
	/** Gets an atom of this Lewis molecule.
	 * @param	aNum	1-based atom index
	 * @return	the atom
	 */
	final public MolAtom getAtom(int aNum)	{ return molecule.getAtom(aNum - 1); }
	/** Gets the number of atoms in this Lewis molecule.
	 * @return	the number of atoms
	 */
	public int getNumAtoms() 				{ return molecule.getAtomCount(); }
	/** Gets the number of bonds in this Lewis molecule.
	 * @return	the number of bonds
	 */
	public int getNumBonds() 				{ return molecule.getBondCount(); }
	/** Gets the total charge in this Lewis molecule.
	 * @return	the total charge
	 */
	public int getTotalCharge() 			{ return molecule.getTotalCharge(); }
	/** Gets the total number of unshared electrons in this Lewis molecule.
	 * @return	the total number of unshared electrons
	 */
	public int getUnsharedElectrons() 		{ return totalUnsharedElecs; }

	/** Gets the unshared electrons of an atom in this Lewis molecule.
	 * @param	index	the atom's 1-based index
	 * @return	the atom's count of unshared electrons
	 */
	public int getUnsharedElectrons(int index) {
		return ChemUtils.getElectrons(molecule.getAtom(index - 1));
	} // getUnsharedElectrons(int)

	/** Gets the total number of valence electrons
	 * shown in this Lewis molecule.
	 * @return	the number of valence electrons shown
	 */
	public int getValenceElectrons() {
		return getSumBondOrders() * 2 + totalUnsharedElecs;
	} // getValenceElectrons()

	/** Gets the sum of all the bond orders in this Lewis molecule.
	 * @return	the sum of bond orders
	 */
	public int getSumBondOrders() {
		int sumBondOrders = 0;
		for (int bndNum = 0; bndNum < getNumBonds(); bndNum++) {
			// assume all bonds are single, double, or triple (no aromatics)
			sumBondOrders += molecule.getBond(bndNum).getType();
		} // for each bond
		return sumBondOrders;
	} // getSumBondOrders()

	/** Gets the sum of the bond orders of an atom in this Lewis molecule.
	 * @param	index	the atom's 1-based index
	 * @return	the sum of the bond orders of this atom
	 */
	public int getSumBondOrders(int index) {
		return ChemUtils.getSumBondOrders(molecule.getAtom(index - 1));
	} // getSumBondOrders(int)

	/** Gets the number of atoms of a particular element in a Lewis molecule.
	 * Counts only explicit H atoms.
	 * @param	atom	the element to count
	 * @return	the number of atoms of an element in the Lewis molecule
	 */
	public int getAtomCount(String atom) {
		return FormulaFunctions.countAtoms(molecule, atom);
	} // getAtomCount(atom)

	/** Gets the formula of a Lewis molecule.  Includes only the explicit H
	 * atoms in the calculation.
	 * @return	the formula
	 */
	public String getFormula() {
		String formula = molecule.getFormula();
		final int countH = molecule.getExplicitHcount();
		if (countH == 1) {
			formula = formula.replaceFirst("[H][0-9]*", "H");
		} else if (countH > 1) {
			formula = formula.replaceFirst("[H][0-9]*", "H" + countH);
		} else if (countH == 0) {
			formula = formula.replaceFirst("[H][0-9]*", "");
		}
		return formula;
	} // getFormula()

	/** Gets the index numbers of electron-deficient atoms of
	* the specified element.
	* If element is 'X' then it considers all electron deficient atoms.
	* @param	atomLabel	which element
	* @return	list of 1-based index numbers of electron-deficient atoms
	*/
	public List<Integer> getElectronDeficientAtoms(String atomLabel) {
		final String SELF = "LewisMolecule.getElectronDeficientAtoms: ";
		final String myAtomLabel = atomLabel.trim();
		final List<Integer> atomNums = new ArrayList<Integer>();
		for (int atNum = 1; atNum <= getNumAtoms(); atNum++) {
			final MolAtom atom = molecule.getAtom(atNum - 1);
			final int maxOuterElec = ChemUtils.getNeededOuterElectrons(atom.getAtno());
			final int numUnsharedElectrons = getUnsharedElectrons(atNum);
			final int atomSumBondOrders = getSumBondOrders(atNum);
			final int outerElectrons = numUnsharedElectrons + atomSumBondOrders * 2;
			if (outerElectrons < maxOuterElec
					&& ("X".equalsIgnoreCase(myAtomLabel)
							|| myAtomLabel.equals(atom.getSymbol()))) {
				debugPrint(SELF + "atom ", atom, atNum, " has ",
						numUnsharedElectrons, " unshared electrons, ",
						atomSumBondOrders, " bonding electrons, ",
						outerElectrons, " outer electrons, needs ",
						maxOuterElec, ", is electron-deficient.");
				atomNums.add(Integer.valueOf(atNum));
			} // outerElectrons < maxOuterElec & right element
			else debugPrint(SELF + "atom ", atom, atNum, " has ",
					numUnsharedElectrons, " unshared electrons, ",
					atomSumBondOrders, " bonding electrons, ",
					outerElectrons, " outer electrons, needs ",
					maxOuterElec, ", is not electron-deficient.");
		} // for each atom
		return atomNums;
	} // getElectronDeficientAtoms

	/** Gets the number of electrons in the outer shell of each atom of a
	 * particular element.
	 * @param	atomLabel	which element
	 * @return	list of int[0][1]: (0) 1-based index of atom; (1) number of
	 * outer electrons
	 */
	public List<int[]> getOuterElectronsList(String atomLabel) {
		final List<int[]> outerElecsList = new ArrayList<int[]>();
		for (int atNum = 1; atNum <= molecule.getAtomCount(); atNum++) {
			final MolAtom atom = molecule.getAtom(atNum - 1);
			if (atomLabel.trim().equals(atom.getSymbol().trim())
					|| "allAtoms".equals(atomLabel.trim())) {
				int[] atomNumElecs = new int[2];
				final int numUnsharedElectrons = getUnsharedElectrons(atNum);
	   	  		final int atomSumBondOrders = getSumBondOrders(atNum);
		   		atomNumElecs[OUTER_ELECS] = numUnsharedElectrons
						+ (atomSumBondOrders * 2);
	   	   		atomNumElecs[ATOM_INDEX] = atNum;
				debugPrint("LewisMolecule.getOuterElectronsList: ",
						"atom ", atom, atomNumElecs[ATOM_INDEX],
						" has ", atomNumElecs[OUTER_ELECS], "outer electrons.");
				outerElecsList.add(atomNumElecs);
	   		} // if atom is the desired element
		} // for each atom
	   	return outerElecsList;
	} // getOuterElectronsList(String, String)

	/** Gets the color of the atom.
	 * @param	atomNum	1-based atom index
	 * @return	the color of the atom
	 */
	public Color getColor(int atomNum) {
		return MolString.getColor(molecule.getAtom(atomNum - 1));
	} // getColor(int)

	/** Sets the highlight property of an atom.
	 * @param	atomNum	1-based atom index
	 */
	public void highlight(int atomNum) {
		highlight(molecule.getAtom(atomNum - 1));
	} // highlight(int)

	/** Sets the highlight property of an atom.
	 * @param	atom	the atom
	 */
	public void highlight(MolAtom atom) {
		atom.putProperty(HIGHLIGHT, HIGHLIGHT);
	} // highlight(MolAtom)

	/** Unsets the highlight property of an atom.
	 * @param	atomNum	1-based atom index
	 */
	public void unhighlight(int atomNum) {
		unhighlight(molecule.getAtom(atomNum - 1));
	} // unhighlight(int)

	/** Unsets the highlight property of an atom.
	 * @param	atom	the atom
	 */
	public void unhighlight(MolAtom atom) {
		atom.removeProperty(HIGHLIGHT);
	} // unhighlight(MolAtom)

	/** Clears highlighting from all atoms.  */
	public void unhighlight() {
		for (final MolAtom atom : getAtoms()) unhighlight(atom);
	} // unhighlight()

	/** Sets the color of the atom.
	 * @param	atomNum	1-based atom index
	 * @param	color	the color
	 */
	public void setColor(int atomNum, Color color) {
		setColor(molecule.getAtom(atomNum - 1), color);
	} // setColor(int, Color)

	/** Sets the color of the atom.
	 * @param	atom	the atom
	 * @param	color	the color
	 */
	public void setColor(MolAtom atom, Color color) {
		MolString.setColor(atom, molecule, mDoc, color);
	} // setColor(MolAtom, Color)

	/** Clears the color from the atom.
	 * @param	atomNum	1-based atom index
	 */
	public void clearColor(int atomNum) {
		MolString.clearColor(molecule.getAtom(atomNum - 1));
	} // clearColor(int)

	/** Clears colors from all atoms.  */
	public void clearColors() {
		for (final MolAtom atom : getAtoms()) MolString.clearColor(atom);
	} // clearColors()

	/** Converts this Lewis molecule to a string.
	 * @return	this molecule in MRV format
	 */
	public String toString() {
		return MolString.toString(molecule, MRV);
	} // toString()

} // LewisMolecule
