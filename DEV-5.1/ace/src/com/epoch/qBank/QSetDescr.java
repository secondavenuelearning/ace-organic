package com.epoch.qBank;

/** The descriptive attributes of a question set; for display and editing.
	Scope: UI layer.
	Sessions: QSet, HWCreateSession.
*/
public class QSetDescr {

	/** Unique ID of this question set. */
	public int id;
	/** Unique ID of the topic containing this question set. */
	public int topicId;
	/** Name of this question set. */
	public String name;
	/** Author of this question set (not used). */
	public String author; 
	/** Common question statement of this question set; appended to the
	 * beginning of the statement of every question in this set. */
	public String header; 
	/** Remarks about this question set; often describes provenance. */
	public String remarks;
	/** Whether the instructor has modified the common Q statement locally. */
	public boolean headerModifiedLocally = false; 

	/** Name of the topic containing this question set. 
	 * It is not always filled or used. */
	public String topicName;

} // QSetDescr
