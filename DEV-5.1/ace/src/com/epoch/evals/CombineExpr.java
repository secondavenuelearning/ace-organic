package com.epoch.evals;

import chemaxon.struc.Molecule;
import com.epoch.evals.evalConstants.CombineConstants;
import com.epoch.responses.Response;
import com.epoch.synthesis.Synthesis;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Methods for combining evaluators or rules on permissible synthetic starting
 * materials.  Expressions for combining evaluators/rules can be in nested or
 * postfix format.
 * <br>example postfix expression: 1:2:3:4:5:2/3/4:@:6:7:|3
 * <br>same in nested format: (1@(2-3/2@3@4@5))|6|7
 * <br>same in English format: (<i>1</i> and (2 to 3 of 
 * {<i>2</i>, <i>3</i>, <i>4</i>, <i>5</i>})) or <i>6</i> or <i>7</i>
 */
public final class CombineExpr implements CombineConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether an Evaluator or Synthesis established this class. */
	transient private boolean fromEval = true;
	/** The Evaluator that established this class, if any. */
	transient private Evaluator parentEval = null;
	/** The response being evaluated. */
	transient private Response response = null;
	/** The Synthesis that established this class, if any. */
	transient private Synthesis parentSynth = null;
	/** The response starting material being evaluated for permissibility. */
	transient private Molecule mol = null;

	/** Constructor. 
	 * @param	eval	Evaluator with subevaluators to be combined
	 * @param	resp	response being evaluated
	 */
	public CombineExpr(Evaluator eval, Response resp) { 
		parentEval = eval;
		response = resp;
	} // CombineExpr(Evaluator, Response)

	/** Constructor. 
	 * @param	synth	synthesis containing rules about permissible starting
	 * materials
	 * @param	sm	response starting material to be evaluated for permissibility
	 */
	public CombineExpr(Synthesis synth, Molecule sm) { 
		parentSynth = synth;
		mol = sm;
		fromEval = false;
	} // CombineExpr(Synthesis, Molecule)

/* **************** Short methods *************/

	/** Converts a postfix expression to a list of tokens.
	 * @param	postfix	the postfix expression
	 * @return	the tokens
	 */
	public static List<String> toTokens(String postfix) {
		return new ArrayList<String>(
				Arrays.asList(postfix.split(POSTFIX_SEP)));
	} // toTokens(String)

	/** Gets a default postfix code for combining expressions.
	 * @param	numRules	number of rules to combine into the code
	 * @return	a String in postfix format
	 */
	public static String getDefaultCode(int numRules) {
		return Utils.join(getDefaultTokens(numRules), POSTFIX_SEP);
	} // getDefaultCode(int)

	/** Gets a default list of tokens.
	 * @param	numRules	number of rules to combine into the tokens
	 * @return	a list of tokens in postfix format
	 */
	public static List<String> getDefaultTokens(int numRules) {
		final List<String> exprCodeTokens = new ArrayList<String>();
		for (int ruleNum = 1; ruleNum <= numRules; ruleNum++) {
			exprCodeTokens.add(String.valueOf(ruleNum));
		} // for each rule
		if (numRules > 1) {
			final String conjxn = Utils.toString(OR,
					numRules <= 2 ? "" : numRules);
			exprCodeTokens.add(conjxn);
		} // if more than one rule
		return exprCodeTokens;
	} // getDefaultTokens(int)

/* **************** Postfix <--> Nested <--> English *************/

	/** Converts a nested expression into an array of its constituent
	 * components (numbers, parentheses, conjunctions).
	 * @param	nested	a nested expression to be parsed
	 * @return	array of Strings of numbers and (@|/-)
	 */
	public static String[] getNestedArray(String nested) {
		final List<String> list = getNestedList(nested);
		return list.toArray(new String[list.size()]);
	} // getNestedArray(String)

	/** Converts a nested expression into a list of its constituent
	 * components (numbers, parentheses, conjunctions).
	 * @param	nested	a nested expression to be parsed
	 * @return	list of Strings of numbers and (@|/-)
	 */
	public static List<String> getNestedList(String nested) {
		final String SELF = "CombineExpr.getNestedList: ";
		debugPrint(SELF + "nested = ", nested);
		int pointer = 0;
		final List<String> tokens = new ArrayList<String>();
		while (pointer < nested.length()) {
			final char ch = nested.charAt(pointer);
			if (Utils.isDigit(ch)) {
				final StringBuilder numBld = Utils.getBuilder(ch);
				pointer++;
				while (pointer < nested.length()) {
					final char nextCh = nested.charAt(pointer);
					if (Utils.isDigit(nextCh)) {
						numBld.append(nextCh);
						pointer++;
					} else break;
				} // while there are more characters
				final String num = numBld.toString();
				debugPrint(SELF + "pointer = ", pointer, ", num = ", num);
				tokens.add(num);
			} else {
				debugPrint(SELF + "pointer = ", pointer, ", char = ", ch);
				tokens.add(String.valueOf(ch));
				pointer++;
			} // if the character is a digit
		} // while there are more characters
		debugPrint(SELF + "tokens = ", tokens);
		return tokens;
	} // getNestedList(String)

	/** Converts a nested expression to a postfix expression.
	 * @param	nested	a nested expression to be parsed
	 * @return	the postfix expression
	 */
	public static String nestedToPostfix(String nested) {
		final String SELF = "CombineExpr.nestedToPostfix: ";
		final String postfix = nestedToPostfix(getNestedList(nested)).toString();
		debugPrint(SELF + "converted ", nested, " to ", postfix);
		return postfix;
	} // nestedToPostfix(String)

	/** Converts a nested expression to a postfix expression.
	 * @param	origTokens	nested expression split into characters and
	 * numbers
	 * @return	StringBuilder containing the postfix expression
	 */
	public static StringBuilder nestedToPostfix(List<String> origTokens) {
		final String SELF = "CombineExpr.nestedToPostfix: ";
		final List<String> tokens = new ArrayList<String>(origTokens);
		tokens.add(String.valueOf(CLOSE_PAREN));
		return nestedToPostfixRecurse(tokens);
	} // nestedToPostfix(List<String>)

	/** Recursively converts a nested expression to a postfix expression.
	 * We traverse the expression left to right.  If we come to an open
	 * parenthesis, we recursively call this method.  As we come to digits, we
	 * append them to the expression.  When we reach a close parenthesis or the
	 * end of the list, we add the conjunction and other necessary data.
	 * @param	tokens	nested expression split into characters and
	 * numbers
	 * @return	StringBuilder containing the postfix expression
	 */
	private static StringBuilder nestedToPostfixRecurse(List<String> tokens) {
		final String SELF = "CombineExpr.nestedToPostfixRecurse: ";
		debugPrint(SELF + "entering with tokens ", tokens);
		final StringBuilder bld = new StringBuilder();
		char oper = AND;
		int numNums = 0;
		String minOf = "0";
		String maxOf = "1";
		while (!tokens.isEmpty()) {
			debugPrint(SELF + "looking at token ", tokens.get(0), 
					" of ", tokens);
			final String token = tokens.remove(0);
			final char token1st = token.charAt(0);
			switch (token1st) {
				case AND:
				case OR: 
					if (oper != OF) oper = token1st; 
					break;
				case OF: 
					oper = token1st; 
					break;
				case TO: 
					break;
				case OPEN_PAREN:
					if (bld.length() > 0) bld.append(POSTFIX_SEP);
					bld.append(nestedToPostfixRecurse(tokens)); // recursive call
					numNums++;
					break;
				case CLOSE_PAREN:
					Utils.appendTo(bld, POSTFIX_SEP, oper);
					if (oper != OF && numNums > 2) bld.append(numNums);
					else if (oper == OF) {
						Utils.appendTo(bld, minOf, OF, maxOf, OF, numNums);
						oper = AND;
					} // if oper
					debugPrint(SELF + "returning ", bld);
					return bld;
				default: // number
					if (bld.length() > 0) bld.append(POSTFIX_SEP);
					final boolean moreTokens = !tokens.isEmpty();
					final char next1stChar = (moreTokens 
							? tokens.get(0).charAt(0) : ' ');
					if (next1stChar == TO) minOf = token; 
					else if (next1stChar == OF) maxOf = token; 
					else {
						bld.append(token);
						numNums++;
					} // if next token is OF
					break;
			} // switch
			debugPrint(SELF + "bld = ", bld);
		} // while there are more characters
		return bld; // won't be reached
	} // nestedToPostfixRecurse(List<String>)

	/** Converts a postfix expression into English. 
	 * @param	postfix	the postfix expression
	 * @return	an English expression
	 */
	public static String postfixToEnglish(String postfix) {
		return postfixToNested(postfix, "", ENGLISH);
	} // postfixToEnglish(String)

	/** Converts a postfix expression into English. 
	 * We traverse the postfix expression from left to right until we come to a
	 * conjunction; then we traverse back to the left as many steps as the
	 * conjunction indicates to find the subexpression.
	 * @param	tokens	the postfix expression split into tokens
	 * @return	an English expression
	 */
	public static String postfixToEnglish(List<String> tokens) {
		return postfixToNested(tokens, "", ENGLISH);
	} // postfixToEnglish(List<String>)

	/** Converts a postfix expression into English. 
	 * We traverse the postfix expression from left to right until we come to a
	 * conjunction; then we traverse back to the left as many steps as the
	 * conjunction indicates to find the subexpression.
	 * @param	postfix	the postfix expression
	 * @param	prefix	a prefix to place before every number in the expression
	 * @return	an English expression
	 */
	public static String postfixToEnglish(String postfix, int prefix) {
		return postfixToNested(postfix, String.valueOf(prefix), ENGLISH);
	} // postfixToEnglish(String, int)

	/** Converts a postfix expression into a nested expression. 
	 * We traverse the postfix expression from left to right until we come to a
	 * conjunction; then we traverse back to the left as many steps as the
	 * conjunction indicates to find the subexpression.
	 * @param	postfix	the postfix expression
	 * @return	an English expression
	 */
	public static String postfixToNested(String postfix) {
		return postfixToNested(postfix, "", !ENGLISH);
	} // postfixToNested(String)

	/** Converts a postfix expression into a nested expression. 
	 * We traverse the postfix expression from left to right until we come to a
	 * conjunction; then we traverse back to the left as many steps as the
	 * conjunction indicates to find the subexpression.
	 * @param	postfix	the postfix expression
	 * @param	prefix	a prefix to place before every number in the expression
	 * @param	english	whether to convert to an English expression or a coded
	 * expression
	 * @return	an English expression
	 */
	private static String postfixToNested(String postfix, String prefix, 
			boolean english) {
		if (Utils.isEmpty(postfix)) return "";
		final List<String> tokens = toTokens(postfix);
		return postfixToNested(tokens, prefix, english);
	} // postfixToNested(String, String, boolean)

	/** Converts a postfix expression into a nested expression. 
	 * We traverse the postfix expression from left to right until we come to a
	 * conjunction; then we traverse back to the left as many steps as the
	 * conjunction indicates to find the subexpression.
	 * @param	origTokens	the postfix expression split into tokens
	 * @param	prefix	a prefix to place before every number in the expression
	 * @param	english	whether to convert to an English expression or a coded
	 * expression
	 * @return	an English expression
	 */
	private static String postfixToNested(List<String> origTokens, String prefix, 
			boolean english) {
		final String SELF = "CombineExpr.postfixToNested: ";
		final List<String> tokens = new ArrayList<String>(origTokens);
		debugPrint(SELF + "tokens = ", tokens);
		final String and = (english ? " and " : String.valueOf(AND));
		final String or = (english ? " or " : String.valueOf(OR));
		final String of = (english ? " of " : OF_STR);
		final String to = (english ? " to " : String.valueOf(TO));
		int ptr = 0;
		while (ptr < tokens.size() && tokens.size() > 1) {
			String token = tokens.get(ptr);
			final char oper = token.charAt(0);
			if (Utils.contains(OPERS, oper)) {
				tokens.remove(ptr);
				int back = 2;
				String[] ofNums = null;
				if (token.length() > 1) {
					final String after1st = token.substring(1);
					if (oper == OF) {
						ofNums = after1st.split(OF_STR);
						back = MathUtils.parseInt(ofNums[OUT_OF]);
					} else back = MathUtils.parseInt(after1st);
				} // if combining more than two evaluators
				final int dest = ptr - back;
				debugPrint(SELF + "found oper ", oper, ", ptr = ", ptr,
						", dest = ", dest);
				final StringBuilder joinedBld = new StringBuilder();
				while (ptr > dest) {
					token = tokens.remove(--ptr);
					if (joinedBld.length() > 0) {
						joinedBld.insert(0, oper == OR ? or : and); // even if OF
					} else if (english && oper == OF) {
						joinedBld.append('}');
					} // if other tokens have been inserted already
					if (english && token.charAt(0) != OPEN_PAREN) {
						joinedBld.insert(0, "</i>");
						joinedBld.insert(0, token);
						if (!Utils.isEmpty(prefix)) {
							joinedBld.insert(0, '.');
							joinedBld.insert(0, prefix);
						} // if have prefix for expression number
						joinedBld.insert(0, "<i>");
					} else joinedBld.insert(0, token);
				} // while combining more individual expressions
				if (oper == OF) {
					if (english) joinedBld.insert(0, '{');
					joinedBld.insert(0, of).insert(0, ofNums[MAX_TRUE])
							.insert(0, to).insert(0, ofNums[MIN_TRUE]);
				} // if oper is OF
				if (!tokens.isEmpty()) {
					joinedBld.insert(0, OPEN_PAREN).append(CLOSE_PAREN);
				} // if should parenthesize
				tokens.add(ptr, joinedBld.toString());
			} // if arrived at a conjunction
			ptr++;
		} // while there are more tokens to combine
		return tokens.get(0);
	} // postfixToNested(List<String>, String, boolean)

/* **************** Expression evaluation methods *************/

	/** Evaluates a postfix expression.  When it comes time to evaluate 
	 * individual expressions, it calls back to the object that originally 
	 * created this one.
	 * We traverse the postfix expression from left to right until we come to a
	 * conjunction; then we traverse back to the left as many steps as the
	 * conjunction indicates to find the subexpression, evaluating each
	 * individual expression as we come to it.  When we finish evaluating the
	 * subexpression, we insert T or F into the list of tokens in place of the
	 * subexpression.
	 * @param	postfix	the postfix expression
	 * @return	a OneEvalResult, which includes a boolean (true if the
	 * expression is satisfied).  If the expression is satisfied, it may also 
	 * contain a modified response or automatic feedback.  If not, it may 
	 * contain an error message.  The OneEvalResult returned from an individual
	 * expression will be preserved if the last 
	 * individual expression of an OR expression to be satisfied generated it, 
	 * or if the first individual expression of an AND expression generated it.
	 */
	public OneEvalResult isSatisfied(String postfix) {
		return isSatisfied(toTokens(postfix));
	} // isSatisfied(String)

	/** Evaluates a postfix expression, in the form of tokens.  When it comes
	 * time to evaluate individual expressions, it calls back to the object that
	 * originally created this one.
	 * We traverse the postfix expression from left to right until we come to a
	 * conjunction; then we traverse back to the left as many steps as the
	 * conjunction indicates to find the subexpression, evaluating each
	 * individual expression as we come to it.  When we finish evaluating the
	 * subexpression, we insert T or F into the list of tokens in place of the
	 * subexpression.
	 * @param	origTokens	the postfix expression split into tokens
	 * @return	a OneEvalResult, which includes a boolean (true if the
	 * expression is satisfied).  If the expression is satisfied, it may also 
	 * contain a modified response or automatic feedback.  If not, it may 
	 * contain an error message.  The OneEvalResult returned from an individual
	 * expression will be preserved if the last 
	 * individual expression of an OR expression to be satisfied generated it, 
	 * or if the first individual expression of an AND expression generated it.
	 */
	public OneEvalResult isSatisfied(List<String> origTokens) {
		final String SELF = "CombineExpr.isSatisfied: ";
		OneEvalResult evalResult = new OneEvalResult();
		// make copy of list because we are going to destroy it
		final List<String> tokens = new ArrayList<String>(origTokens);
		debugPrint(SELF + "starting with tokens list: ", tokens); 
		int ptr = 0;
		if (tokens.size() == 1) {
			tokens.add(EXPR_TRUE);
			tokens.add(String.valueOf(AND));
			debugPrint(SELF + "modified tokens to: ", tokens); 
		} // if there's only one token
		while (ptr < tokens.size() && tokens.size() > 1) {
			String token = tokens.get(ptr);
			char token1st = token.charAt(0);
			if (Utils.contains(OPERS, token1st)) {
				final char oper = token1st;
				debugPrint(SELF + "operator is ", oper); 
				boolean isSatisfiedSoFar = oper == AND;
				int numTokensSatisfied = 0;
				tokens.remove(ptr);
				int back = 2;
				int[] ofNums = null;
				if (token.length() > 1) {
					final String after1st = token.substring(1);
					if (oper == OF) {
						ofNums = Utils.stringToIntArray(after1st.split(OF_STR));
						debugPrint(SELF + "ofNums = ", ofNums);
						back = ofNums[OUT_OF];
					} else back = MathUtils.parseInt(after1st);
				} // if combining more than two evaluators
				final int dest = ptr - back;
				while (ptr > dest) {
					token = tokens.remove(--ptr);
					token1st = token.charAt(0);
					boolean tokenSatisfied = true;
					if (Utils.isDigit(token1st)) {
						final int ruleNum = MathUtils.parseInt(token);
						evalResult = (fromEval
								? parentEval.satisfiesRule(ruleNum, response)
								: parentSynth.satisfiesRule(ruleNum, mol));
						tokenSatisfied = evalResult.isSatisfied;
						if (fromEval) debugPrint(SELF + "rule ", ruleNum,
								" is", tokenSatisfied ? "" : " not", 
								" satisfied");
						else debugPrint(SELF + "rule ", ruleNum, " is",
								tokenSatisfied ? "" : " not", 
								" satisfied by ", mol);
					} else {
						tokenSatisfied = EXPR_TRUE.equals(token);
						debugPrint(SELF + "arrived at expression previously "
								+ "evaluated to be ", token,
								"; isSatisfiedSoFar = ", isSatisfiedSoFar);
					} // if at number
					boolean skipToEnd = false;
					if (oper == OF) {
						if (tokenSatisfied) numTokensSatisfied++;
						isSatisfiedSoFar = 
								numTokensSatisfied >= ofNums[MIN_TRUE]
								&& numTokensSatisfied <= ofNums[MAX_TRUE];
						final int numTrueIfRestTrue = 
								ptr - dest + numTokensSatisfied;
						skipToEnd = (isSatisfiedSoFar 
								? numTrueIfRestTrue <= ofNums[MAX_TRUE]
								: numTrueIfRestTrue < ofNums[MIN_TRUE]);
						debugPrint(SELF + "numTokensSatisfied = ", 
								numTokensSatisfied, ", isSatisfiedSoFar = ", 
								isSatisfiedSoFar, ", numTrueIfRestTrue = ", 
								numTrueIfRestTrue, ", skipToEnd = ", 
								skipToEnd);
					} else {
						isSatisfiedSoFar = (oper == OR
								? tokenSatisfied || isSatisfiedSoFar
								: tokenSatisfied && isSatisfiedSoFar);
						debugPrint(SELF + "now isSatisfiedSoFar = ", 
								isSatisfiedSoFar);
						skipToEnd = isSatisfiedSoFar == (oper == OR);
					} // if oper
					if (skipToEnd) {
						debugPrint(SELF + "result won't change; skip to "
								+ "end of this group.");
						while (ptr > dest) tokens.remove(--ptr);
						break;
					} // no need to continue evaluating this group
				} // while combining more expressions
				tokens.add(ptr, isSatisfiedSoFar ? EXPR_TRUE : EXPR_FALSE);
				debugPrint(SELF + "adding ", tokens.get(0), 
						" back to tokens to give ", tokens);
			} // if arrived at a conjunction
			ptr++;
		} // while there are more tokens to combine
		evalResult.isSatisfied = EXPR_TRUE.equals(tokens.get(0));
		debugPrint(SELF + "the combined expression ",
				evalResult.isSatisfied ? "is" : "is not", " satisfied.");
		return evalResult;
	} // isSatisfied(List<String>)

} // CombineExpr
