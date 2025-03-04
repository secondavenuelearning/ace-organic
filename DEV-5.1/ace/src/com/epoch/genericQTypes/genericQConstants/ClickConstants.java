package com.epoch.genericQTypes.genericQConstants;

/** Constants of the clickable image question type. */
public interface ClickConstants {

	// public static final is implied by interface

	/** X coordinate. */
	int X = 0;
	/** Y coordinate. */
	int Y = 1;
	/** Member of parameters defining a rectangle, circle, or ellipse. */
	int WIDTH = 2;
	/** Member of parameters defining a rectangle, circle, or ellipse. */
	int HEIGHT = 3;
	/** Position of rectangle shapes in <code>authString</code> and Javascript
	 * shapes array. */
	int RECT = 0;
	/** Position of ellipse shapes in <code>authString</code> and Javascript
	 * shapes array. */
	int ELLIP = 1;
	/** Position of circle shapes in <code>authString</code> and Javascript
	 * shapes array. */
	int CIRC = 2;
	/** Position of marked points in Javascript shapes array. */
	int MARK = 3;
	/** Maximum number of question data for this question type. */
	int MAX_QDATA = 1;
	/** Separates the color and the number of marks in the question data. */
	String QD_SEP = "\t";
	/** The position of the color in the question data. */
	int COLOR = 0;
	/** The position of the number of marks in the question data. */
	int NUM_MARKS = 1;
	/** Default values for color and number of marks. */
	String[] DEFAULT_QD = new String[] {"red", "1"};

	/** Tag for XML format of click-image response. */
	String XML_TAG = "xml";
	/** Tag for XML format of click-image response. */
	String MARK_TAG = "mark";
	/** Tag for XML format of click-image response. */
	String X_TAG = "x";
	/** Tag for XML format of click-image response. */
	String Y_TAG = "y";
	/** Tag for XML format of click-image response. */
	String WIDTH_TAG = "width";
	/** Tag for XML format of click-image response. */
	String HEIGHT_TAG = "height";
	/** Tag for XML format of click-image response. */
	String TEXT_TAG = "text";

} // ClickConstants
