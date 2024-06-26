package com.epoch.chem;

import chemaxon.struc.MolAtom;
import chemaxon.struc.Molecule;
import chemaxon.struc.PeriodicSystem;
import com.epoch.chem.chemConstants.FormulaConstants; 
import com.epoch.evals.impl.chemEvals.chemEvalConstants.WtConstants;
import com.epoch.utils.MathUtils; 
import com.epoch.utils.Utils; 
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Holds a formula.  */
public final class Formula implements FormulaConstants, WtConstants {

	private static void debugPrint(Object... msg) { 
		// Utils.printToLog(msg);
	}

	/** The string representing the formula. */
	private final String formulaStr;
	/** Map containing count of each element keyed by element.
	 * A count of -1 means any number of that element.  */
	transient private final Map<String, Integer> formulaMap;
	/** Stores whether an element is listed twice. */
	transient private boolean elementTwice = false;

/* ***************** Constructors ******************/

	/** Constructor from a molecule. Parses a molecule's formula into a map of 
	 * element counts keyed by element symbols.  D and T are the only isotopes 
	 * considered.
	 * @param	mol	a molecule
	 * @param	isLewis	whether this molecule is a Lewis structure
	 */
	public Formula(Molecule mol, boolean isLewis) {
		final String SELF = "Formula: ";
		formulaStr = mol.getFormula();
		formulaMap = new LinkedHashMap<String, Integer>();
		for (final MolAtom atom : mol.getAtomArray()) {
			if (!ChemUtils.isMulticenterAtom(atom)) {
				String elem = atom.getSymbol();
				if ("H".equals(elem)) {
					final int massNo = atom.getMassno();
					if (massNo == 2) elem = "D";
					else if (massNo == 3) elem = "T";
				} // if it's H
				put(elem, Integer.valueOf(getNumberOf(elem) + 1));
			} // if atom is not multicenter attachment point
		} // for each atom
		// add implicit H atoms if not a Lewis structure
		if (!isLewis) {
			final int numH = mol.getImplicitHcount() + getNumberOf("H");
			debugPrint(SELF + "found ", numH, " H atoms");
			if (numH > 0) put("H", Integer.valueOf(numH));
		} // if not a Lewis structure
		debugPrint(SELF + "formula map for ", mol, ": ", formulaMap);
	} // Formula(Molecule, boolean)

	/** Constructor from a String. Parses a text formula into a map of element 
	 * counts hashed by element symbols. The parser tolerates incorrect 
	 * capitalization, any order of elements, repeated elements, and parentheses
	 * (as long as they are matched). It tolerates neither invalid element 
	 * symbols nor nonalphanumeric characters (except parentheses, and * 
	 * for any number of an element in an author's formula). D and T are the 
	 * only isotopes that can be handled.
	 * @param	formulaOrig	a chemical formula
	 * @throws	FormulaException	if doesn't match the pattern for a formula
	 * or if an element symbol is invalid
	 */
	public Formula(String formulaOrig) throws FormulaException {
		formulaStr = formulaOrig;
		final int flags = FIX_CASE; // and don't allow asterisk
		checkAllCharsAllowed(flags); 
		formulaMap = parseFormulaStr(formulaStr, flags);
	} // Formula(String)

	/** Constructor from a String. Parses a text formula into a map of element 
	 * counts hashed by element symbols. The parser tolerates incorrect 
	 * capitalization, any order of elements, repeated elements, and parentheses
	 * (as long as they are matched). It tolerates neither invalid element 
	 * symbols nor nonalphanumeric characters (except parentheses, and * 
	 * for any number of an element in an author's formula). D and T are the 
	 * only isotopes that can be handled.
	 * @param	flags	whether to fix the case of the element symbols, allow 
	 * asterisk
	 * @param	formulaOrig	a chemical formula
	 * @throws	FormulaException	if doesn't match the pattern for a formula
	 * or if an element symbol is invalid
	 */
	public Formula(String formulaOrig, int flags) throws FormulaException {
		formulaStr = formulaOrig;
		checkAllCharsAllowed(flags); // throws exception if not
		formulaMap = parseFormulaStr(formulaStr, flags);
	} // Formula(String, int)

	/** Copy constructor.
	 * @param	formula	the formula to be copied
	 */
	public Formula(Formula formula) {
		formulaStr = formula.getFormulaStr();
		formulaMap = new LinkedHashMap<String, Integer>(formula.getMap());
		elementTwice = formula.getElementTwice();
	} // Formula(Formula)

/* ***************** private methods for constructors ******************/

	/** Throws an exception if the formula contains an impermissible character.
	 * @param	flags	second bit says whether to allow an asterisk character
	 * @throws	FormulaException	if the formula contains an impermissible
	 * character
	 */
	private void checkAllCharsAllowed(int flags) throws FormulaException {
		final String SELF = "Formula.checkAllCharsAllowed: ";
		final String allowedCharsPattern = Utils.toString(
				".*[^A-Za-z0-9()", allowAsterisks(flags) ? "*" : "", "].*");
		if (formulaStr.matches(allowedCharsPattern)) {
			throw new FormulaException("A formula may contain only "
					+ "letters, numbers, and parentheses.");
		} // if formula contains impermissible character
	} // checkAllCharsAllowed(int)

	/** Parses a text formula into a map of element counts 
	 * hashed by element symbols. The parser tolerates incorrect
	 * capitalization, any order of elements, repeated elements, and parentheses
	 * (as long as they are matched). It tolerates neither invalid element 
	 * symbols nor nonalphanumeric characters (except parentheses, and * 
	 * for any number of an element in an author's formula). D and T are the 
	 * only isotopes that can be handled.
	 * @param	fStr	the formula String
	 * @param	flags	whether to fix the case of the element symbols, allow 
	 * asterisk
	 * @return	the formula map
	 * @throws	FormulaException	if doesn't match the pattern for a formula
	 * or if an element symbol is invalid
	 */
	private Map<String, Integer> parseFormulaStr(String fStr, int flags) 
			throws FormulaException {
		final String SELF = "Formula.parseFormulaStr: ";
		final Map<String, Integer> fMap = new LinkedHashMap<String, Integer>();
		final char[] formulaChars = fStr.toCharArray();
		final int numChars = formulaChars.length;
		final FormulaException exception = new FormulaException(
				"The formula contains an invalid element symbol.");
		int posn = 0;
		while (posn < numChars) {
			// get a map of the element or the elements of the parenthesized group
			final StringBuilder elemBld = new StringBuilder();
			final char ch1 = formulaChars[posn++];
			Map<String, Integer> subformulaMap;
			if (ch1 == ')') {
				throw new FormulaException("The formula contains an "
						+ "unmatched closed parenthesis.");
			} else if (ch1 == '(') {
				int depth = 1;
				while (posn < numChars && depth > 0) {
					final char ch2 = formulaChars[posn++];
					if (ch2 == '(') depth++;
					else if (ch2 != ')') elemBld.append(ch2);
					else depth--;
				} // while in subformula
				if (depth != 0) throw new FormulaException(
						"The formula contains an unmatched open parenthesis.");
				subformulaMap = parseFormulaStr(elemBld.toString(), flags); // recurse!
				debugPrint(SELF + "subformula map: ", subformulaMap);
			} else {
				if (!Utils.isLetter(ch1)) throw exception;
				elemBld.append(ch1);
				if (!isValidElement(ch1)) {
					if (posn >= numChars) throw exception;
					final char ch2 = formulaChars[posn++];
					if (!Utils.isLetter(ch2) || !isValidElement(ch1, ch2)) {
						throw exception;
					} // if ch2 is a digit or ch1-ch2 is not a symbol
					elemBld.append(ch2);
				} else if (posn < numChars) {
					// ch1 alone is valid, but should ch1 be read with ch2?
					final char ch2 = formulaChars[posn];
					boolean addCh2 = Utils.isLowerCaseLetter(ch2) 
								&& isValidElement(ch1, ch2); 
					if (!addCh2 && Utils.isUpperCaseLetter(ch2)
							&& isValidElement(ch1, ch2) && !isValidElement(ch2)) {
						addCh2 = posn + 1 >= numChars;
						if (!addCh2) {
							final char ch3 = formulaChars[posn + 1];
							addCh2 = Utils.isLetter(ch3) 
									&& !isValidElement(ch2, ch3);
						} // if there's a 3rd char to examine
					} // if like BRO or BRa
					if (addCh2) {
						elemBld.append(ch2);
						posn++;
					} // if next character should be part of element symbol
				} // if ch1 is valid symbol, posn is before the end
				if (fixCase(flags)) {
					for (int chNum = 0; chNum < elemBld.length(); chNum++) {
						final char elemChar = elemBld.charAt(chNum);
						elemBld.setCharAt(chNum, chNum == 0
								? Character.toUpperCase(elemChar)
								: Character.toLowerCase(elemChar));
					} // for each character in the element's symbol
				} // if should fix the case of the element characters
				final String element = elemBld.toString();
				debugPrint(SELF + "found element ", element);
				subformulaMap = new HashMap<String, Integer>();
				subformulaMap.put(element, Integer.valueOf(1));
			} // if open paren
			// get the number of atoms or groups
			int groupCount = 1;
			if (posn < numChars) {
				char charOfCount = formulaChars[posn];
				if (charOfCount == ANY_NUMBER_IN_FORMULA) {
					groupCount = -1;
					posn++;
				} else if (Utils.isDigit(charOfCount)) {
					final StringBuilder countBld = new StringBuilder();
					while (posn < numChars) {
						charOfCount = formulaChars[posn];
						if (Utils.isDigit(charOfCount)) {
							countBld.append(charOfCount);
							posn++;
						} else break;
					} // while posn
					groupCount = MathUtils.parseInt(countBld.toString());
				} // if next character
			} // if there are subsequent characters
			debugPrint(SELF + "found ", groupCount, " atoms/groups");
			// add the element(s) and its (their) count to the map
			final List<String> elements = 
					new ArrayList<String>(subformulaMap.keySet());
			for (final String element : elements) {
				final int subformulaAtomCount = 
						subformulaMap.get(element).intValue();
				final int atomCount = (groupCount < 0 || subformulaAtomCount < 0
						? -1 : subformulaAtomCount * groupCount);
				if (fMap.containsKey(element)) { // atom already counted
					elementTwice = true;
					if (atomCount != -1) {
						final int alreadyCounted = fMap.get(element).intValue();
						if (alreadyCounted != -1) { // skip if previous asterisk
							final int newCount = atomCount + alreadyCounted;
							fMap.put(element, Integer.valueOf(newCount));
						} // if the map already has a count for that element
					} // if stored count is not wild card
				} else { // new entry or asterisked value
					fMap.put(element, Integer.valueOf(atomCount));
				} // if the map already has the element
			} // for each element
		} // while there are still characters to parse
		debugPrint(SELF + "final formula map: ", fMap);
		return fMap;
	} // parseFormulaStr(int)

	/** Gets if a string of characters is recognizable as an elemental symbol. 
	 * Case is ignored.
	 * @param	chars	indefinite list of characters
	 * @return	true if the string is recognizable as an elemental symbol
	 */
	private boolean isValidElement(char... chars) {
		boolean valid = true;
		try {
			PeriodicSystem.findAtomicNumber(String.valueOf(chars)); 
		} catch (IllegalArgumentException e) {
			valid = false;
		} // try
		return valid;
	} // isValidElement(char...)

	/** Gets whether to fix the case of element symbols.
	 * @param	f	the flags
	 * @return	true if should fix the case of element symbols
	 */
	private boolean fixCase(int f)			{ return (f & FIX_CASE) != 0; }
	/** Gets if asterisks are allowed in the formula.
	 * @param	f	the flags
	 * @return	true if asterisks are allowed in the formula
	 */
	private boolean allowAsterisks(int f)	{ return (f & ALLOW_ASTERISK) != 0; }

/* ***************** Short methods ******************/

	/** Gets the original string representing the formula.
	 * @return	the string representing the formula
	 */
	public String getFormulaStr() 			{ return formulaStr; }
	/** Gets the formula map.
	 * @return	the formula map
	 */
	public Map<String, Integer> getMap()	{ return formulaMap; }
	/** Gets the number of atoms associated with the element.
	 * @param	elem	the element
	 * @return	the number of atoms associated with the element, or null if not
	 * found
	 */
	public Integer get(String elem) 		{ return formulaMap.get(elem); }
	/** Puts the element and number of atoms of that element in the map.
	 * @param	elem	the element
	 * @param	num	the number of atoms of the element
	 */
	public void put(String elem, int num)	{ formulaMap.put(elem, Integer.valueOf(num)); }
	/** Removes the element from the formula map.
	 * @param	elem	the element
	 * @return	the number of atoms associated with the element, or null if not
	 * found
	 */
	public Integer remove(String elem) 		{ return formulaMap.remove(elem); }
	/** Gets all the elements in the formula map.
	 * @return	list of elements in the formula map
	 */
	public List<String> getElements()		{ return new ArrayList<String>(formulaMap.keySet()); }
	/** Gets whether an element is in the formula.
	 * @param	elem	the element
	 * @return	true if the element is present
	 */
	public boolean contains(String elem) 	{ return getNumberOf(elem) != 0; }
	/** Gets whether any element appears twice in the formula.
	 * @return	true if any element is repeated in the formula 
	 */
	public boolean getElementTwice() 		{ return elementTwice; }
	/** Creates a hash code summarizing this object.
	 * @return	the hash code
	 */
	@Override
	public int hashCode() 					{ return formulaMap.hashCode(); }

	/** Gets the number of atoms associated with the element.
	 * @param	elem	the element
	 * @return	the number of atoms associated with the element
	 */
	public int getNumberOf(String elem) { 
		final Integer numObj = get(elem);
		return (numObj == null ? 0 : numObj.intValue());
	} // getNumberOf(String)

	/** Gets whether this formula exactly matches another one, including the 
	 * number of elements and the number of atoms of each element.
	 * @param	theOther	the other formula
	 * @return	true if they match
	 */
	@Override
	public boolean equals(Object theOther) {
		boolean isEqual = false;
		if (theOther instanceof Formula) {
			final Formula otherFormula = (Formula) theOther;
			isEqual = formulaMap.equals(otherFormula.getMap()); 
		}
		return isEqual;
	} // equals(Object)

/* ***************** Other methods ******************/

	/** Throws an exception if any of the elements in this formula have a first 
	 * letter that is lower case or a second letter that is upper case.
	 * @throws	FormulaException	if an element has bad case
	 */
	public void noBadCaseElement() throws FormulaException {
		final Formula respFormula = new Formula(formulaStr, KEEP_CASE);
		for (final String element : respFormula.getElements()) {
			for (int posn = 0; posn < element.length(); posn++) {
				final char elemChar = element.charAt(posn);
				if ((posn == 0 && Utils.isLowerCaseLetter(elemChar))
						|| (posn > 0 && Utils.isUpperCaseLetter(elemChar))) {
					throw new FormulaException("At least one of the elements " 
							+ "in your formula is capitalized incorrectly.");
				} // if first letter is lower case or subsequent letter is upper
			} // for each letter in the element's symbol
		} // for each element
	} // noBadCaseElement()

	/** Throws an exception if a formula contains an explicit 1 for an element.
	 * @throws	FormulaException	if an element is followed by 1
	 */
	public void noExplicit1() throws FormulaException {
		if (formulaStr.matches(".*[A-Z][a-z]*1($|[A-Z].*)")) {
			throw new FormulaException("Do not write the number 1 "
					+ "when there is only one of an element in the formula.");
		} // if formula contains X[x..]1 or X[x..]1Y
	} // noExplicit1()

	/** Throws an exception if a formula doesn't have the elements in Hill 
	 * order (CHDT then alphabetical).
	 * @throws	FormulaException	if the elements are not in Hill order
	 */
	public void inHillOrder() throws FormulaException {
		if (getElementTwice() || formulaStr.indexOf("(") >= 0) {
			throw new FormulaException("Your formula should list the "
					+ "elements C, H, D, and T first and in that order, "
					+ "followed by the other elements alphabetically.");
		} // if formula contains parentheses
		final List<String> elements = getElements();
		final String[] first4Elements = new String[] {"C", "H", "D", "T"};
		for (final String element : first4Elements) {
			if (elements.get(0).equals(element)) {
				elements.remove(0);
			} else if (contains(element)) {
				throw new FormulaException("Your formula should list the "
						+ "elements C, H, D, and T first and in that order.");
			} // if first element in list
		} // for each of the beginning elements
		final List<String> alphabetized = new ArrayList<String>(elements);
		Collections.sort(alphabetized);
		if (!elements.equals(alphabetized)) {
			throw new FormulaException("Your formula should list elements "
					+ "alphabetically after C, H, D, and T.");
		} // if the alphabetized order doesn't match the actual order
	} // inHillOrder()

	/** Gets whether this formula matches another one, including the 
	 * number of elements and the number of atoms of each element, and
	 * considering wild cards in this formula (but not the other one).
	 * @param	otherFormulaOrig	the other formula
	 * @return	true if they match
	 */
	public boolean matches(Formula otherFormulaOrig) {
		final String SELF = "Formula.matches: ";
		final Formula thisFormula = new Formula(this);
		final Formula otherFormula = new Formula(otherFormulaOrig);
		final List<String> elements = thisFormula.getElements();
		for (final String element : elements) {
			final int numElement = get(element).intValue();
			if (numElement == -1) {
				thisFormula.remove(element);
				otherFormula.remove(element);
			} else if (numElement == 0 // useful to exclude D and T from H count
					&& otherFormula.get(element) == null) {
				thisFormula.remove(element);
			} // if an element can have any or must have zero
		} // for each element in the author's map
		debugPrint(SELF + "comparing response elements ", otherFormula.getMap(),
				" to author elements ", thisFormula.getMap());
		return thisFormula.equals(otherFormula);
	} // matches(Formula)

	/** Calculates the average molecular weight or exact mass of a compound 
	 * with this formula.
	 * @param	wtType	average molecular weight or exact mass
	 * @return	the weight
	 */
	public double getWeight(int wtType) {
		final String SELF = "Formula.getWeight: ";
		double wt = 0.0;
		for (final String element : getElements()) {
			final int atomicNum = PeriodicSystem.findAtomicNumber(element);
			wt += getNumberOf(element) * (wtType == EXACT_MASS
					? PeriodicSystem.getMass(atomicNum, 
						"D".equals(element) ? 2
						: "T".equals(element) ? 3
						: PeriodicSystem.getMostFrequentNaturalIsotope(
							atomicNum))
					: PeriodicSystem.getMass(atomicNum));
		} // for each element
		return wt;
	} // getWeight(int)

	/** Calculates the unsaturation index of the formula.
	 * @return	the formula's unsaturation index
	 * @throws FormulaException	if the unsaturation index is fractional or
	 * negative, or if the formula contains d- or f-block elements
	 */
	public int getUnsaturationIndex() throws FormulaException {
		final String SELF = "Formula.getWeight: ";
		int numCEquivs = 0;
		int numHEquivs = 0;
		for (final String element : getElements()) {
			final int atomicNum = PeriodicSystem.findAtomicNumber(element);
			final int column = PeriodicSystem.getColumn(atomicNum);
			final int numAtoms = getNumberOf(element);
			if (column == 14) numCEquivs += numAtoms;
			else if (column == 1 || column == 17) numHEquivs += numAtoms;
			else if (column == 13 || column == 15) numHEquivs -= numAtoms;
			else if (column != 2 && column != 16) {
				throw new FormulaException("ACE cannot calculate the "
						+ "unsaturation index of a compound containing "
						+ "d- or f-block or noble elements.");
			} // if column
		} // for each element
		if (numHEquivs % 2 != 0) {
			throw new FormulaException("The formula has a fractional "
					+ "unsaturation index.");
		} // if omega is fractional
		final int omega = numCEquivs + 1 - numHEquivs / 2;
		if (omega < 0) {
			throw new FormulaException("The formula has a negative "
					+ "unsaturation index.");
		} // if omega is negative
		return omega;
	} // getUnsaturationIndex()

} // Formula
