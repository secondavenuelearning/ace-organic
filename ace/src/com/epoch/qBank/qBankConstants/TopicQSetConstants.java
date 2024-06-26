package com.epoch.qBank.qBankConstants;

import com.epoch.constants.AuthorConstants;

/** Constants for the QuestionBank, Topic, and QSet classes. */
public interface TopicQSetConstants extends AuthorConstants {

	// public static final is implied by interface

	/** Parameter for QSet.parseXML(). */
	boolean STORE_DATA = true;
	/** Parameter for XMLUtils.extractNodes(). */
	boolean FILE = true;

	/** Tag for question bank in XML. */
	String QBANK_TAG = "questionBank";
	/** Tag for topic in XML. */
	String TOPIC_TAG = "topic";
	/** Tag for question set in XML. */
	String QSET_TAG = "questionSet";
	/** Tag for question set in XML. */
	String QSET_ID_TAG = "id";

	/** Tag for topic or question set name in XML. */
	String NAME_TAG = "name";
	/** Tag for topic or question set remarks in XML. */
	String REMARKS_TAG = "remarks";
	/** Tag of question set author in XML output. */
	String AUTHOR_TAG = "author";
	/** Tag of question set header in XML output. */
	String HEADER_TAG = "header";
	
} // TopicQSetConstants
