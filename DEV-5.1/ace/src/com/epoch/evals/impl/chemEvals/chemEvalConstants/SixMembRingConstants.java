package com.epoch.evals.impl.chemEvals.chemEvalConstants;

public class SixMembRingConstants implements ChairConstants {

	/** Value for orientation; used as return value for
	 * SixMembRing.getOrientation().  */
	public static final int INDETERMINATE = -1;

	/** Member of ringAngles with angles values between -180 and 180 
	 * degrees. */
	public static final int POS_NEG = 0;
	/** Member of ringAngles with angles values between 0 and 180 
	 * degrees. */
	public static final int POS = 1;
	/** Member of angleSizeRingNums with the most acute angles. */
	public static final int ACUTE = 0;
	/** Member of angleSizeRingNums with the most obtuse angles. */
	public static final int INTERMEDIATE = 1;
	/** Member of angleSizeRingNums with the intermediate angles. */
	public static final int OBTUSE = 2;
	/** Array of types of angle: acute, intermediate, and obtuse. */
	public static final int[] ANGLE_TYPES = 
			new int[] {ACUTE, INTERMEDIATE, OBTUSE};
	/** Member of return value of getOrientations(). */
	public static final int RING_ATOM_NUM = 0;
	/** Member of return value of getOrientations(). */
	public static final int LIG_MOL_INDEX = 1;
	/** Member of return value of getOrientations(). */
	public static final int ORIENTN = 2;

	/** Dihedral angle that the bond to an axial substituent makes to a ring
	 * bond. */
	public static final double AXIAL_ANGLE = 60.0;
	/** Dihedral angle that the bond to an equatorial substituent makes to a ring
	 * bond. */
	public static final double EQUATORIAL_ANGLE = 180.0;
	/** Amount by which response dihedral angles can diverge from axial/equatorial
	 * dihedral angles and still be considered equal. */
	public static final double TOLERANCE3D = 15.0;
	/** Amount by which response angles can diverge from axial/equatorial
	 * angles and still be considered equal. */
	public static final int TOLERANCE2D = 10;
	/** Flag for getAngle(). */
	public static final int NOT_SIGNED = 0;

} // SixMembRingConstants
