package com.epoch.db.dbConstants;

/** Stores maximum string lengths of certain database table fields. */
public class DBLimits {

	/** Maximum length of a functional group's name, limited by database
	 * varchar2 field. */
	public static final int MAX_FNAL_GRP_NAME = 80;
	/** Maximum length of an impossible starting material's name, limited by 
	 * database varchar2 field. */
	public static final int MAX_IMPOSSIBLE_SM_NAME = 80;
	/** Maximum length of a reaction condition's name, limited by database
	 * varchar2 field. */
	public static final int MAX_RXNCOND_NAME = 200;
	/** Maximum length of an R group collection's name, limited by database
	 * varchar2 field. */
	public static final int MAX_R_GROUP_COLL_NAME = 100;
	/** Maximum length of a measurement unit's name, limited by database 
	 * varchar2 field. */
	public static final int MAX_UNIT_NAME = 20;

} // DBLimits
