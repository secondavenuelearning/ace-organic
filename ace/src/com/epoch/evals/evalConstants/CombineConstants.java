package com.epoch.evals.evalConstants;

/** Holds constants used for expressions that combine various components. */
public interface CombineConstants {

	// public static final is implied by interface

	/** Open parenthesis. */
	char OPEN_PAREN = '(';
	/** Close parenthesis. */
	char CLOSE_PAREN = ')';
	/** Character representing "and". */
	char AND = '@';
	/** Character representing "or". */
	char OR = '|';
	/** Character representing "of". */
	char OF = '/';
	/** Character representing "to". */
	char TO = '-';
	/** String representing "of". */
	String OF_STR = "/";
	/** Characters that represent conjunctions.  */
	char[] OPERS = new char[] {AND, OR, OF};
	/** Character that separates indices and conjunctions in postfix expressions.  */
	String POSTFIX_SEP = ":";
	/** Character to substitute for expressions evaluated to be true. */
	String EXPR_TRUE = "T";
	/** Character to substitute for expressions evaluated to be false. */
	String EXPR_FALSE = "F";
	/** Parameter for postfixToNested(). */
	boolean ENGLISH = true;
	/** Member of array obtained by splitting final token in OF expression. */
	int MIN_TRUE = 0;
	/** Member of array obtained by splitting final token in OF expression. */
	int MAX_TRUE = 1;
	/** Member of array obtained by splitting final token in OF expression. */
	int OUT_OF = 2;

} // CombineConstants
