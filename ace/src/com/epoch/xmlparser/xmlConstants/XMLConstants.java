package com.epoch.xmlparser.xmlConstants;

/** Holds constants used for XML functions. */
public interface XMLConstants {

	// public static final is implied by interface

	/** Extension of XML files. */
	String XML_SUFFIX = ".xml";
	/** Parameter for startTag(). */
	boolean NEWLINE = true;
	/** Parameter for startTag(). */
	boolean SELF_CLOSING = true;

	/** Used in XML output. */
	String OPEN = "<";
	/** Used in XML output. */
	String OPEN_END = OPEN + "/";
	/** Used in XML output. */
	String CLOSE = ">";
	/** Used in XML output. */
	String CLOSE_NL = CLOSE + "\n";
	/** Used in XML output. */
	String SELF_CLOSE = "/" + CLOSE_NL;

	/** Tag for XML IO.  */
	String LANGUAGE_TAG = "language";
	/** Tag for XML IO.  */
	String TRANSLATION_TAG = "translation";

} // XMLConstants
