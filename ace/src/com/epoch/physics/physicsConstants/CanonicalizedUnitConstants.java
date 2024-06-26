package com.epoch.physics.physicsConstants;

/** Holds constants for the equations question type. */
public interface CanonicalizedUnitConstants {

	// public static final is implied by interface

	/** Position of meter exponent in array of fundamental unit exponents. */
	int METER = 0;
	/** Position of gram exponent in array of fundamental unit exponents. */
	int GRAM = 1;
	/** Position of second exponent in array of fundamental unit exponents. */
	int SEC = 2;
	/** Position of ampere exponent in array of fundamental unit exponents. */
	int AMP = 3;
	/** Position of degrees Kelvin exponent in array of fundamental unit exponents. */
	int DEGK = 4;
	/** Position of mole exponent in array of fundamental unit exponents. */
	int MOLE = 5;
	/** Position of candela exponent in array of fundamental unit exponents. */
	int CANDELA = 6;
	/** Symbols of the fundamental units. */
	String[] SI_UNIT_SYMBOLS = new String[]
			{"m", "g", "s", "A", "K", "mol", "cd"};
	/** Names of the fundamental units. */
	String[] SI_UNIT_NAMES = new String[]
			{"meter", "gram", "second", "ampere", 
			"degrees Kelvin", "mole", "candela"};
	/** Parameter for toFormula(). */
	int FULL_CANONICALZN = 0;
	/** Parameter for toFormula(). */
	int OMIT_FACTORS = 1;
	/** Parameter for toFormula(). */
	int NO_CANONICALZN = 2;

} // CanonicalizedUnitConstants
