package com.epoch.physics.physicsConstants;

/** Holds constants for the equations question type. */
public interface EquationsConstants {

	// public static final is implied by interface

	/** Member of eqnPropertyAndNum. */
	int PROPERTY = 0;
	/** Member of eqnPropertyAndNum. */
	int EQN_NUMBER = 1;
	/** Value for eqnPropertyAndNum[PROPERTY]. */
	int UNCALCULATED = 0;
	/** Value for eqnPropertyAndNum[PROPERTY]. */
	int DOESNT_FOLLOW = 1;
	/** Value for eqnPropertyAndNum[PROPERTY]. */
	int INCOMPARABLE = 2;
	/** Value for eqnPropertyAndNum[PROPERTY]. */
	int FORMAT_EXCEPTION = 3;
	/** Tag for XML format of equations response. */
	String XML_TAG = "xml";
	/** Tag for XML format of equations response. */
	String EQUATION_TAG = "equation";
	/** Tag for XML format of equations response. */
	String CONSTANTS_TAG = "constants";
	/** Tag for XML format of equations response. */
	String VARS_NOT_UNITS_TAG = "variables_not_units";
	/** Tag for XML format of equations response. */
	String DISABLED_ATTR_TAG = "disabled";
	/** Parameter for isValidExpression(). */
	boolean SAVE_EQN = true;

} // EquationsConstants
