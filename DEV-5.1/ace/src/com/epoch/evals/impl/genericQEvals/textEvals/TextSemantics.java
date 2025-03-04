package com.epoch.evals.impl.genericQEvals.textEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.TextAndNumbers;
import com.epoch.evals.impl.genericQEvals.textEvals.textEvalConstants.TextConstants;
import com.epoch.exceptions.ParameterException;
import com.epoch.genericQTypes.ChooseExplain;
import com.epoch.genericQTypes.Logic;
import com.epoch.responses.Response;
import com.epoch.utils.Utils;

import edu.stanford.nlp.ling.CoreLabel;  
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.Tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// New evaluator, 10/2011 Raphael

/** If the response has the same semantics as ... */
public class TextSemantics extends TextAndNumbers 
		implements EvalInterface, TextConstants {

	// constants describing result of semantic comparison
	private static final int SAME = 0;
	// private static final int OPPOSITE = 1; // unused 11/6/2012
	private static final int UNSURE = 2;

    private static LexicalizedParser lp;
	private static boolean parserInitialized = false;

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Constructor. */
	public TextSemantics() {
		isPositive = false;
		ignoreCase = true;
	} // TextSemantics()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public TextSemantics(String data) throws ParameterException {
		debugPrint("TextSemantics.java: data = ", data);
		final String[] splitData = data.split("/");
		if (splitData.length >= 1) { 
			isPositive = Utils.isPositive(splitData[0]);
		}
		if (splitData.length < 1) {
			throw new ParameterException("TextSemantics ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // TextSemantics(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>isPositive</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return (isPositive ? "Y" : "N");
	} // getCodedData()

	/** Gets an English-language description of this evaluator.
	 * @param	qDataTexts	the text of question data of this question, if any;
	 * not used, but required by interface
	 * @param	forPermissibleSM	whether to word the English to describe a
	 * permissible starting material in a multistep synthesis question
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish(String[] qDataTexts, boolean forPermissibleSM) {
		return toEnglish();
	} // toEnglish(String[], boolean)

	/** Gets an English-language description of this evaluator.
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish() {
		final StringBuilder words = Utils.getBuilder("If the response does");
		if (!isPositive) words.append(" not");
		words.append(" have the same semantics as ");
		Utils.addSpanString(words, strName, !TO_DISPLAY);
		debugPrint("TextSemantics.toEnglish: ", words);
		return words.toString();
	 } // toEnglish()

	/** Determines whether the response contains the indicated text.
	 * @param	response	a parsed response
	 * @param	authString	string to which to compare the response
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final String SELF = "TextSemantics.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		final String resp = (response.parsedResp instanceof String
					? (String) response.parsedResp
				: response.parsedResp instanceof Logic
					? ((Logic) response.parsedResp).getParagraph()
				: ((ChooseExplain) response.parsedResp).text);
		debugPrint(SELF + "original strings:\nresp = ", resp, 
				"\nref = ", authString);
		String respMod = Utils.cersToUnicode(resp);
		String ref = Utils.cersToUnicode(
				authString.trim().replaceAll("\\s+", " "));
		if (ignoreCase) {
			ref = ref.toLowerCase(Locale.US);
				respMod = respMod.toLowerCase(Locale.US);
		}
		int found = UNSURE;
		found = sameSemantics(respMod, ref);
		evalResult.isSatisfied = found == SAME;
		if (found == UNSURE) {
			evalResult.verificationFailureString = 
				"ACE cannot parse your response.";
		}
		debugPrint(SELF + "respMod = ", respMod,
				", author string = ", ref, 
				", found = ", found, 
				", isPositive = ", isPositive, 
				", returning ", evalResult.isSatisfied);
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() {
		return EVAL_CODES[TEXT_SEMANTICS];
	}
	/** Required by interface.  Sets a possibly shortened version of the 
	 * test string for display.
	 * @param	str	the test string (maybe shortened)
	 */
	public void setMolName(String str) 		{ strName = str; } 

	/* actual work of comparing semantics */

	public void initParser() {
		if (parserInitialized) return;
		debugPrint("initializing parser ...");
		// the following writes to lp, a static field, but we choose to delay
		// this action until now instead of initializing where lp is declared,
		// because we generally don't use lp at all.
	  	lp = new LexicalizedParser("/home/aceorg/aceorg/stanford-parser-2011-09-14/grammar/englishPCFG.ser.gz");
		lp.setOptionFlags("-maxLength", "80",
			"-retainTmpSubcategories");        
		debugPrint("... initialized");
		parserInitialized = true;
	} // initParser

	/** Returns a string representation of the parse of a string.
	 * @param	text	the unparsed string
	 * @return  String: represents the parse of the string
	 */
	private List<Tree> parseString(String text) {
		// partially copied from ParserDemo.java of Stanford nlp
		initParser();
		final String[] sentences = text.split("\\. *");
		final List<Tree> result = new ArrayList<Tree>();
		for (String sentence : sentences) {
			sentence = sentence.replaceAll("[;,:?]", "").
				toLowerCase(Locale.ENGLISH); // cleanup
			final String[] afterSplit = sentence.split(" +");
			final List<CoreLabel> rawWords = new ArrayList<CoreLabel>();
			for (final String word : afterSplit) {
				final CoreLabel lab = new CoreLabel();
				lab.setWord(word);
				rawWords.add(lab);
			}
			Tree topLevel = lp.apply(rawWords);
			// look for a sentence within the parse tree, typically 1 level down.
			while ((topLevel != null) &&
					(!topLevel.label().toString().
						contains("CategoryAnnotation=S "))){
				final Tree[] children = topLevel.children();
				topLevel = children[children.length - 1]; // the last child
			}
			result.add(topLevel);
		} // each sentence
		return result;
	} // parseString

	/** Returns whether two text strings have the same English semantics.
	 * @param	response	the response string
	 * @param	author	the author's string
	 * @return  int: SAME, OPPOSITE, UNSURE
	 */
	private int sameSemantics(String response, String author) {
		final List<Tree> responseTrees = parseString(response);
		debugPrint(responseTrees.size() + " responses:");
		for (final Tree tree : responseTrees) {
			final String responsePenn = tree.pennString();
			debugPrint("\t", responsePenn);
			debugPrint("\tlabel of root: ", tree.label().toString());
		}
		return UNSURE;
	} // sameSemantics

  /* below here is to mesh in.

    // This option shows loading and using an explicit tokenizer
    String sent2 = "This is another sentence.";
    TokenizerFactory<CoreLabel> tokenizerFactory = 
      PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
    List<CoreLabel> rawWords2 = 
      tokenizerFactory.getTokenizer(new StringReader(sent2)).tokenize();
    parse = lp.apply(rawWords2);

    TreebankLanguagePack tlp = new PennTreebankLanguagePack();
    GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
    GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
    List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
    System.out.println(tdl);
    System.out.println();

    TreePrint tp = new TreePrint("penn,typedDependenciesCollapsed");
    tp.printTree(parse);
  }

  private ParserDemo() {} // static methods only

}
*/

} // TextSemantics
