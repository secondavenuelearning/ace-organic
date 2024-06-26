package com.epoch.qBank.qBankConstants;

import com.epoch.xmlparser.xmlConstants.XMLConstants;

/** Constants belonging to the Question class. */
public interface QuestionConstants extends XMLConstants {

	// public static final is implied by interface

	/** Value for qFlags: skeletal structure question.  Uses MarvinSketch. */
	int MARVIN = 1;
	/** Value for qFlags: Lewis structure question.  Uses LewisSketch.  */
	int LEWIS = 2;
	/** Value for qFlags: mechanism question. Uses MarvinSketch. */
	int MECHANISM = 3;
	/** Value for qFlags: multiple-choice question.  Uses check boxes or radio 
	 * buttons. */
	int CHOICE = 4;
	/** Value for qFlags: ranking or numbering question.  Uses pulldown menus 
	 * with numbers.  */
	int RANK = 5;
	/** Value for qFlags: multistep synthesis question.  Uses both MarvinSketch 
	 * plus popup menus for selecting reaction conditions.  */
	int SYNTHESIS = 6;
	/** Value for qFlags: fill-in-the-blank question.
	 * Uses pulldown menus with words in the question statement.  */
	int FILLBLANK = 7;
	/** Value for qFlags: numerical question. Uses one or two
	 * small textboxes, maybe a pulldown menu.  */
	int NUMERIC = 8;
	/** Value for qFlags: text-based question. Uses a large textbox.  */
	int TEXT = 9;
	/** Value for qFlags: user completes a table with text and
	 * numbers. Uses many textboxes.  */
	int TABLE = 10;
	/** Value for qFlags: orbital energy diagram question. 
	 * Uses table whose cells may contain one or more pulldown menus.  */
	int ORB_E_DIAGRAM = 11;
	/** Value for qFlags: reaction coordinate diagram question. 
	 * Uses table whose cells may contain one or more pulldown menus.  */
	int RXN_COORD = 12;
	/** Value for qFlags: multiple-choice combined with text. */
	int CHOOSE_EXPLAIN = 14;
	/** Value for qFlags: clickable image. */
	int CLICK_IMAGE = 15;
	/** Value for qFlags: logical statements. */
	int LOGIC = 16;
	/** Value for qFlags: draw vectors. */
	int DRAW_VECTORS = 17;
	/** Value for qFlags: equations. */
	int EQUATIONS = 18;
	/** Value for qFlags: chemical formula. Uses a text input field. */
	int FORMULA = 19;
	/** Value for qFlags: undefined question type. Uses MarvinSketch. */
	int OTHER = 0;
	/** Names of question types. */
	String[] QTYPE_NAMES = new String[] {
			"Skeletal structure",
			"Lewis structure",
			"Mechanism",
			"Multiple choice",
			"Ordering/Ranking",
			"Multistep synthesis",
			"Select to fill in the blanks",
			"Numeric",
			"Text",
			"Complete the table",
			"Orbital energy diagram",
			"Reaction coordinate diagram",
			"", // no longer used
			"Choose and explain",
			"Clickable image",
			"Logical statements",
			"Draw vectors",
			"Equations",
			"Formula"
			};
	/** Database values of question types. */
	String[] DB_QTYPES = new String[] {
			"OTHER",
			"SKELETAL",
			"LEWIS",
			"MECHANISM",
			"MULT_CHOICE",
			"ORDERING",
			"SYNTHESIS",
			"FILL_BLANK",
			"NUMERIC",
			"TEXT",
			"TABLE",
			"ORBITAL_DIAG",
			"RXN_COORD",
			"", // no longer used
			"CHOOSE_EXPLN",
			"CLICK_IMG",
			"LOGIC_STMTS",
			"VECTORS",
			"EQUATIONS",
			"FORMULA"
			};

	/** Mask for question type (bits 0-5); needed for legacy purposes only. */
	long MAJORTYPEMASK = (long) (Math.pow(2, 6) - 1);
	/** Mask for flags (bits 6-29); needed for legacy purposes only. */
	long FLAGSMASK = (long) (Math.pow(2, 30) - Math.pow(2, 6));

	/* OK to add additional flags at bits 0-5. */

	/** Bit 6 of qFlags:  Preload applet with Figure. */
	long PRELOAD = (1 << 6);
	/** Bit 7 of qFlags:  Show mapping. */
	long SHOWMAPPING = (1 << 7);
	/** Bit 8 of qFlags:  Turn on 3D. */
	long THREEDIM = (1 << 8);
	/** Bit 9 of qFlags:  Turn on lone pairs. */
	long SHOWLONEPAIRS = (1 << 9);
	/** Bit 10 of qFlags; paired with bit 18 for how to show implicit 
	 * H atoms: 0 = on heteratoms and terminal C atoms, 1 = on all atoms,
	 * 2 = show none, 3 = on heteroatoms only. */
	long IMPLICIT_H_0 = (1 << 10);
	/** Bit 11 of qFlags:  Show RS chirality. */
	long SHOWRSLABELS = (1 << 11);
	/** Bit 12 of qFlags:  Scramble student options. */
	long SCRAMBLE = (1 << 12);
	/** Bit 13 of qFlags:  Substitute R groups in Figure 1 with
	 * shortcut groups or variables in question statement with values. */
	long USES_SUBSTNS = (1 << 13);
	/** Bit 14 of qFlags:  Disallow multiple responses in choice Qs. */
	long DISALLOW_MULT = (1 << 14);
	/** Bit 15 of qFlags:  Allow unranked items in rank Qs. */
	long ALLOW_UNRANKED = (1 << 15);
	/** Bit shift of qFlags for which of figures[0-3] to preload. */
	long FIG_TO_PRELOAD_SHIFT = 16;
	/** Mask for bits 16 and 17 of qFlags, which indicate which of
	 * figures[0-3] to preload. */
	long FIG_TO_PRELOAD_MASK =
			(1 << FIG_TO_PRELOAD_SHIFT) | (1 << (FIG_TO_PRELOAD_SHIFT + 1));
	/** Bit 18 of qFlags; paired with bit 10 for how to show implicit 
	 * H atoms: 0 = on heteratoms and terminal C atoms, 1 = on all atoms,
	 * 2 = show none, 3 = on heteroatoms only. */
	long IMPLICIT_H_1 = (1 << 18);
	/** Bit 19 of qFlags:  Show only to the master author. */
	long HIDE = (1 << 19);
	/** Bit 20 of qFlags:  Use scientific notation. */
	long USE_SCI_NOTN = (1 << 20);
	/** Bit 21 of qFlags:  Require numbers for table entries. */
	long NUMS_ONLY = (1 << 21);
	/** Bit 22 of qFlags:  Allow labeling of orbitals. */
	long LABEL_ORBS = (1 << 22);
	/** Bit 23 of qFlags:  Show all C. */
	long SHOWALLC = (1 << 23);
	/** Bit 24 of qFlags:  Don't show bad valences in MarvinSketch. */
	long BADVALENCEINVISIBLE = (1 << 24);
	/** Bit 25 of qFlags:  Require integers in numeric responses. */
	long REQUIRE_INT = (1 << 25);
	/** Bit 26 of qFlags:  Disallow wedge bonds starting at nonstereocenters. */
	long DISALLOW_SUPERFLUOUS_WEDGES = (1 << 26);
	/** Bit 27 of qFlags:  Eschew chemistry formatting. */
	long NO_CHEM_FORMATTING = (1 << 27);
	/** Bit 28 of qFlags:  Omit constants field. */
	long NO_CONSTANTS_FIELD = (1 << 28);
	/** Bit 29 of qFlags:  If scrambling options, leave last option last. */
	long EXCEPT_LAST = (1 << 29);
	/** Bit 30 of qFlags:  Show coordinate bond button. Deprecated. */
	// long SHOW_COORD_BOND = (1 << 30);

	/* Don't use more than 45 bits, because we store qFlags in JavaScript 
	 * integers also.  We can now use the first 6 bits of qFlags. */

	/** Mask to find value of bits 10 and 18. */
	long IMPLICITHMASK = IMPLICIT_H_0 | IMPLICIT_H_1;
	/** Value of bits 10 and 18: Show no implicit H atoms. */
	long SHOWNOH = IMPLICIT_H_1;
	/** Value of bits 10 and 18: Show implicit H atoms attached to heteroatoms. */
	long SHOWHETEROH = IMPLICIT_H_0 | IMPLICIT_H_1;
	/** Value of bits 10 and 18: Show all implicit H atoms. */
	long SHOWALLH = IMPLICIT_H_0;

	/** Member of qData representing all data other than R-group data. */
	int GENERAL = 0;
	/** Member of qData representing R groups or variables to be substituted
	 * into a question figure or statement. */
	int SUBSTNS = 1;
	/** Number of lists in qData. */
	int NUM_QDATA_LISTS = 2;

	/** Parameter for QuestionRW.getQuestion[s].  Indicates full download of
	 * question.  Alternative is super-light download (qFlags and qData only). */
	boolean FULL_LOAD = true;
	/** Parameter for QuestionRW.getQuestion[s].  */
	boolean ADD_HEADER = true;
	/** Parameter for Question() and QuestionWrite.addQuestion(). */
	boolean PRESERVE_ID = true;
	/** Parameter for makeFileName().  */
	String NO_EXTENSION = "";
	/** Parameter for setEvaluator(). */
	boolean TRANSLATING = true;
	/** Parameter for getQuestions(). */
	String[] NO_LANGUAGES = null;
	/** Parameter for getQuestions(). */
	String ANY_ORDER = "";
	/** Parameter for getOrderSql(). */
	boolean BY_REMARKS = true;
	/** Parameter for addQDatum(). */
	boolean CONVERT_NESTED = true;
	/** Parameter for getDisplayStatement(). */
	boolean CONVERT_VARS_TO_VALUES = true;

	/** Value for editType.  Indicates question is from the master database. */
	int SAME = 0;
	/** Value for editType.  Indicates master database question that has been
	 * locally modified. */
	int CHANGED = 1;
	/** Value for editType.  Indicates question has been locally authored. */
	int NEW = 2;

	/** Return value of findSynthCombExpr(). */
	int NO_EXPR = 0;
	/** Return value of findSynthCombExpr(). */
	int LAST_QDATUM = 1;
	/** Return value of findSynthCombExpr(). */
	int NOT_LAST_QDATUM = 2;
	/** Return value of findSynthCombExpr(). */
	int TWO_EXPRNS = 3;

	/** Tag used in xml IO. */
	String QUESTION_TAG = "problem";
	/** Tag used in xml IO. */
	String QID_TAG = "id";
	/** Tag used in xml IO. */
	String STATEMENT_TAG = "statement";
	/** Tag used in xml IO (legacy). */
	String FLAGS_TAG = "problemType";
	/** Tag used in xml IO. */
	String QTYPE_TAG = "questionType";
	/** Tag used in xml IO. */
	String QFLAGS_TAG = "flags";
	/** Tag used in xml IO. */
	String BOOK_TAG = "book";
	/** Tag used in xml IO. */
	String CHAPTER_TAG = "chapter";
	/** Tag used in xml IO. */
	String Q_REMARKS_TAG = "remarks";
	/** Tag used in xml IO. */
	String KEYWORDS_TAG = "keywords";

} // QuestionConstants
