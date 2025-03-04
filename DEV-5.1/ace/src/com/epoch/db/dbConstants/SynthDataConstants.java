package com.epoch.db.dbConstants;

import com.epoch.synthesis.synthConstants.SynthConstants;

/** Tables with rarely altered data for multistep synthesis questions, 
 * and their fields. */
public class SynthDataConstants implements SynthConstants {

	/** Table for reaction condition definitions for synthesis questions. */
	public static final String RXN_CONDNS = "reaction_conditions_v3";
		/** Field in RXN_CONDNS.  Unique ID.  */
		public static final String RXNCOND_ID = "rxn_cond_id";
		/** Field in RXN_CONDNS. */
		public static final String RXNCOND_NAME = "name";
		/** Field in RXN_CONDNS. */
		public static final String RXNCOND_DEF = "definition"; // CLOB
		/** Field in RXN_CONDNS. */
		public static final String RXNCOND_CLASS = "class";
		/** Field in RXN_CONDNS. */
		public static final String RXNCOND_3COMP = "threeComponent";
	/** Sequencer for reaction condition definitions.  */
	public static final String RXN_CONDNS_SEQ = "reaction_conditions_seq";
	/** Table for bad starting material definitions for synthesis questions. */
	public static final String BAD_SYNTH_SM = "impossible_SMs_v2";
		/** Field in BAD_SYNTH_SM. */
		public static final String BADSM_NAME = "name";
		/** Field in BAD_SYNTH_SM. */
		public static final String BADSM_DEF = "definition"; // CLOB
		/** Field in BAD_SYNTH_SM. */
		public static final String BADSM_SORT = "sortkey";
	/** Table for menu-only reagents in synthesis questions. */
	public static final String MENU_ONLY_RGTS = "menu_only_reagents_v1";
		/** Field in MENU_ONLY_RGTS. */
		public static final String MENUONLY_NAME = "name";
		/** Field in MENU_ONLY_RGTS. */
		public static final String MENUONLY_DEF = "definition";
	/** Value for oldName when adding a new entry. */ 
	public static final String NEW_ENTRY = null;

} // SynthDataConstants
