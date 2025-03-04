package com.epoch.physics.physicsConstants;

import com.epoch.genericQTypes.genericQConstants.ClickConstants;

/** Holds constants for the draw-vectors question type. */
public interface DrawVectorsConstants extends ClickConstants {

	// public static final is implied by interface

	/** X coordinate of vector origin. */
	int X1 = 0;
	/** Y coordinate of vector origin. */
	int Y1 = 1;
	/** X coordinate of vector target. */
	int X2 = 2;
	/** Y coordinate of vector target. */
	int Y2 = 3;
	/** Position of vector shapes in Javascript shapes array. */
	int ARROW = 4;
	/** Position of vector origin in array of DPoint3s describing the vector. */
	int ORIGIN = 0;
	/** Position of vector target in array of DPoint3s describing the vector. */
	int TARGET = 1;

	/** Tag for XML format of draw-vectors response. */
	String VECTOR_TAG = "vector";
	/** Tag for XML format of draw-vectors response. */
	String ORIGIN_TAG = "origin";
	/** Tag for XML format of draw-vectors response. */
	String TARGET_TAG = "target";

} // DrawVectorsConstants
