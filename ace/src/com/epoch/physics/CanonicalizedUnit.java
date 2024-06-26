package com.epoch.physics;

import com.epoch.physics.physicsConstants.CanonicalizedUnitConstants;
import com.epoch.utils.Utils;

/** Holds a unit reduced to SI units.  (We use g instead of kg.)  */
public class CanonicalizedUnit implements CanonicalizedUnitConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** The symbol of the unit. */
	final private String symbol; // set in constructor
	/** The name of the unit. */
	final private String name; // set in constructor
	/** What the unit measures. */
	final private String whatMeasures; // set in constructor
	/** The coefficient of the factor that converts the unit to SI units. */
	final private double coeff; // set in constructor
	/** The power-of-ten exponent of the factor that converts the unit to SI units. */
	final private int power10; // set in constructor
	/** The exponents of each of the fundamental SI units. */
	transient final private int[] unitSIPowers; // set in constructor
	/** Parameter for toCanonicalForm(). */
	final static private boolean FOR_DISPLAY = true;

	/* **************** Constructors ****************/

	/** Constructor. 
	 * @param	unitSymbol	the unit's symbol
	 * @param	unitName	the unit's name
	 * @param	measures	what the unit measures
	 * @param	factorCoeff	the coefficient of the conversion factor
	 * @param	factorPower	the exponent of ten of the conversion factor
	 * @param	powers	array of the powers of each fundamental SI unit
	 * in the definition of this unit, in the order of the units in
	 * SI_UNIT_SYMBOLS
	 */
	public CanonicalizedUnit(String unitSymbol, String unitName, 
			String measures, double factorCoeff, int factorPower, 
			int[] powers) {
		symbol = unitSymbol;
		name = unitName;
		whatMeasures = measures;
		coeff = factorCoeff;
		power10 = factorPower;
		unitSIPowers = powers;
	} // CanonicalizedUnit(String, String, String, double, int, int[])

	/** Gets the unit's symbol.
	 * @return	the unit's symbol
	 */
	public String getSymbol()			{ return symbol; }
	/** Gets the unit's name.
	 * @return	the unit's name
	 */
	public String getName()				{ return name; }
	/** Gets what the unit measures.
	 * @return	what the unit measures
	 */
	public String getWhatMeasures()		{ return whatMeasures; }
	/** Gets the unit's factor's coefficient.
	 * @return	the unit's factor's coefficient
	 */
	public double getCoeff()			{ return coeff; }
	/** Gets the unit's factor's power of 10.
	 * @return	the unit's factor's power of 10
	 */
	public int getPower10()				{ return power10; }
	/** Gets the powers of the SI units that make up this unit.
	 * @return	the powers of the SI units that make up this unit
	 */
	public int[] getSIUnitPowers()		{ return unitSIPowers; }

	/** Gets the canonicalized unit suitable for substitution into a formula.
	 * @return	the canonicalized unit suitable for substitution into a formula
	 */
	public String toFormula() {
		return toFormula(FULL_CANONICALZN);
	} // toFormula()
	
	/** Gets the canonicalized unit suitable for substitution into a formula.
	 * @param	canonicaliznExtent	to what extent to convert units to their 
	 * canonicalized form
	 * @return	the canonicalized unit suitable for substitution into a formula
	 */
	public String toFormula(int canonicaliznExtent) {
		return toCanonicalForm(!FOR_DISPLAY, canonicaliznExtent);
	} // toFormula(int)
	
	/** Gets the canonicalized unit formatted for display.
	 * @return	the canonicalized unit formatted for display
	 */
	public String toDisplay() {
		return toCanonicalForm(FOR_DISPLAY, FULL_CANONICALZN);
	} // toDisplay()
	
	/** Gets a unit converted into a product of a number and the fundamental SI 
	 * units raised to appropriate powers.
	 * @param	forDisplay	whether to format it for display
	 * @param	canonicaliznExtent	to what extent to convert units to their 
	 * canonicalized form
	 * @return	the unit converted into its canonicalized form
	 */
	private String toCanonicalForm(boolean forDisplay, int canonicaliznExtent) {
		final StringBuilder numerBld = new StringBuilder();
		final StringBuilder denomBld = new StringBuilder();
		final int numSIUnits = SI_UNIT_SYMBOLS.length;
		int numSIUnitsInDenom = 0;
		for (int unitSINum = 0; unitSINum < numSIUnits; unitSINum++) {
			final int unitSIPower = unitSIPowers[unitSINum];
			if (unitSIPower > 0 || (unitSIPower < 0 && !forDisplay)) {
				if (numerBld.length() > 0) numerBld.append(forDisplay 
						? " &middot; " : " * ");
				numerBld.append(SI_UNIT_SYMBOLS[unitSINum]);
				if (unitSIPower != 1) {
					numerBld.append(forDisplay ? Utils.getBuilder(numerBld, 
								"<sup>", unitSIPower, "</sup>")
							: Utils.getBuilder(numerBld, '^', unitSIPower));
				} // if exponent is not 1
			} else if (unitSIPower < 0) {
				numSIUnitsInDenom++;
				if (numSIUnitsInDenom > 1) denomBld.append(" &middot; ");
				denomBld.append(SI_UNIT_SYMBOLS[unitSINum]);
				if (unitSIPower != -1) {
					Utils.appendTo(denomBld, "<sup>", -unitSIPower, "</sup>");
				} // if exponent is not -1
			} // if there's a power for this fundamental unit
		} // for each fundamental unit
		final StringBuilder unitBld = new StringBuilder();
		if (canonicaliznExtent == FULL_CANONICALZN) {
			if (coeff != 1.0) unitBld.append(coeff);
			if (power10 != 0) {
				if (unitBld.length() > 0) unitBld.append(forDisplay
						? " &times; " : " * ");
				unitBld.append("10");
				if (power10 != 1) {
					unitBld.append(forDisplay ? Utils.getBuilder("<sup>", 
								Utils.formatNegative(power10), "</sup>")
							: Utils.getBuilder('^', power10));
				} // if exponent is not 1
			} // if there's a power of 10
		} // if including factors
		if (numerBld.length() > 0) {
			if (unitBld.length() > 0) unitBld.append(forDisplay ? ' ' : " * ");
			unitBld.append(numerBld);
		} // if there are numerators
		if (numSIUnitsInDenom > 0) {
			unitBld.append(" / ");
			if (numSIUnitsInDenom > 1) denomBld.insert(0, '(').append(')');
			unitBld.append(denomBld);
		} // if there are numerators
		return unitBld.toString();
	} // toCanonicalForm(boolean, int)

} // CanonicalizedUnit
