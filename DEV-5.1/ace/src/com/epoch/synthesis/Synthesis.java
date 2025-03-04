package com.epoch.synthesis;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.sss.search.SearchException;
import chemaxon.standardizer.Standardizer;
import chemaxon.struc.MDocument;
import chemaxon.struc.Molecule;
import com.epoch.chem.ChemUtils;
import com.epoch.chem.MolCompare;
import com.epoch.chem.MolCompareException;
import com.epoch.courseware.User;
import com.epoch.evals.CombineExpr;
import com.epoch.evals.OneEvalResult;
import com.epoch.qBank.QDatum;
import com.epoch.synthesis.synthConstants.SynthConstants;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.List;

/** Wraps a response synthesis, and includes methods for analyzing its validity. */
public class Synthesis implements SynthConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	//--------------------------------------------------------------------
	//					members
	//--------------------------------------------------------------------
	/** The parsed synthesis. */
	public transient SynthParser parsedSynth = null;
	/** Has the synthesis been parsed? */
	public transient boolean initialized = false;
	/** Stores any SynthError that may have been thrown while parsing. */
	public transient SynthError errorObject = null;
	/** Rules about permissible starting materials as stored in the
	 * QDatum.  */
	private transient final List<SynthStarterRule> starterRules =
			new ArrayList<SynthStarterRule>();
	/** Grammatical components (open and close parenthesis, conjunction, rule
	 * number [1-based]) of how the rules on permissible starting materials
	 * will be combined, and their values. */
	private transient List<String> exprCodeTokens = null;
	/** Compounds and substructures that students may try to use 
	 * in synthesis responses but that cannot exist.  */
	private transient NamedCompound[] badSMs = null;
	/** Used to convert aromatic compounds with toilet bowls to alternating
	 * bonds and pentavalent N to ylides. */
	transient Standardizer calcdProdsStdizer = null;

	//--------------------------------------------------------------------
	//					constructors
	//--------------------------------------------------------------------
	/** Constructor. */
	public Synthesis() { 
		// empty constructor
	}

	/** Constructor.
	 * @param	response	MRV representation of the student's response
	 * @throws	MolFormatException	if molecule can't be imported
	 */
	public Synthesis(String response) throws MolFormatException {
		initialize(response, EMPTY_BOX_OK);
	} // Synthesis(String)

	/** Constructor.
	 * @param	response	MRV representation of the student's response
	 * @param	emptyBoxOK	whether it is OK for a box to be empty
	 * @throws	MolFormatException	if molecule can't be imported
	 */
	public Synthesis(String response, boolean emptyBoxOK) 
			throws MolFormatException {
		initialize(response, emptyBoxOK);
	} // Synthesis(String, boolean)

	/** Sets the parentSynth variable of each stage to this Synthesis. 
	 * @param	response	MRV representation of the student's response
	 * @param	emptyBoxOK	whether it is OK for a box to be empty
	 * @throws	MolFormatException	if molecule can't be imported
	 */
	private void initialize(String response, boolean emptyBoxOK) 
			throws MolFormatException {
		final String SELF = "Synthesis.initialize: ";
		debugPrint(SELF + "Entering synthesis initialization...");
		if (initialized) {
			errorObject = new SynthError("Synthesis.initialize",
					"Tried to reinitialize synthesis. Please report this "
					+ "error to the webmaster.\n");
		} else try {
			parsedSynth = new SynthParser(response, emptyBoxOK);
			parsedSynth.setParentSynth(this);
			initialized = true;
			debugPrint(SELF + "initialization complete.");
		} catch (SynthError e) {
			errorObject = e;
			debugPrint(SELF + "initialization failed: ",
					e.getMessage());
		} // try
	} // initialize(String, boolean)

	/** Gets a stage of the synthesis.
	 * @param	stgNum	the stage to get (0-based)
	 * @return a stage in the synthesis
	 */
 	public SynthStage getStage(int stgNum) 	{ return parsedSynth.getStage(stgNum); }
	/** Gets the 0-based index of the target stage of the synthesis.
	 * @return 0-based index of the target stage of the synthesis
	 */
 	public int getTargetStageIndex() 		{ return parsedSynth.targetStageIndex; }
	/** Gets the number of arrows in the synthesis
	 * @return	the number of arrows 
	 */
	public int getNumArrows() 				{ return parsedSynth.getNumArrows(); }
	/** Gets a copy of the MDocument of the mechanism.
	 * @return	a copy of the MDocument of the mechanism
	 */
	public MDocument getMDocCopy()			{ return parsedSynth.getMDocCopy(); }

	/** Gets the impossible starting materials. 
	 * @return	arrays of impossible starting materials
	 */
	public NamedCompound[] getBadSMs() { 
		if (badSMs == null) badSMs = SynthBadSMs.getBadSMs();
		return badSMs; 
	} // getBadSMs()

	//--------------------------------------------------------------------
	//					initStandardizer
	//--------------------------------------------------------------------
	/** Initiates the calcdProdsStdizer. */
	void initStandardizer() {
		final String SELF = "Synthesis.initStandardizer: ";
		final String STDIZER_XML = 
				  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<!-- Standardizer configuration file -->\n"
				+ "\n"
				+ "<StandardizerConfiguration Version =\"0.1\">\n"
				+ "	<Actions>\n"
				+ "		<Transformation ID=\"N_ylide_double\"\n"
				+ "			Structure=\""
				+ "[#34,#16,#8,#7,#6;0:2]=[#7;0:1]=[#34,#16,#8,#7,#6;0:2]"
				+ ">>[#34,#16,#8,#7,#6;0:2]=[#7;+:1]-[#34,#16,#8,#7,#6;-:2]\"/>\n"
				+ "		<Transformation ID=\"N_ylide_triple\"\n"
				+ "			Structure=\""
				+ "[#34,#16,#8,#7,#6;0:2]=[#7;0:1]#[#34,#16,#8,#7,#6;0:2]"
				+ ">>[#34,#16,#8,#7,#6;0:2]=[#7;+:1]=[#34,#16,#8,#7,#6;-:2]\"/>\n"
				+ "		<Dearomatize ID=\"dearomatize\"/>\n"
				+ "	</Actions>\n"
				+ "</StandardizerConfiguration>";
		calcdProdsStdizer = new Standardizer(STDIZER_XML);
	} // initStandardizer()

	//--------------------------------------------------------------------
	//					getRxnConditions
	//--------------------------------------------------------------------
	/** Gets the reaction conditions chosen by the user.
	 * @return colon-separated string of reaction condition IDs
	 */
 	public String getRxnConditions() { 
		return parsedSynth.getRxnConditions();
	} // getRxnConditions()

	/** Gets the reaction conditions chosen by the user from the molecule
	 * property.
	 * @param	synthStr	MRV of the synthesis
	 * @return	reaction ID numbers of this synthesis as a 
	 * semicolon-separated string
	 */
	public static String getRxnConditions(String synthStr) {
		if (synthStr == null) return null;
		try {
			return getRxnConditions(MolImporter.importMol(synthStr));
		} catch (MolFormatException e) {
			// old format of synthesis reaction conditions appended to MRV
			return getSynthesisComponents(synthStr)[RXNID];
		}
	} // getRxnConditions(String)

	/** Gets the reaction conditions chosen by the user from the molecule
	 * property.
	 * @param	mol	an imported synthesis response
	 * @return	reaction ID numbers of this synthesis as a 
	 * semicolon-separated string
	 */
	public static String getRxnConditions(Molecule mol) {
		return (mol == null ? null : ChemUtils.getProperty(mol, RXN_IDS));
	} // getRxnConditions(String)

	//--------------------------------------------------------------------
	//					convertSynthesisFormat
	//--------------------------------------------------------------------
	/** Converts the synthesis description from the form with the reaction ID
	 * numbers appended to the MRV to the reaction IDs embedded in the MRV as a
	 * molecule property.
	 * @param	synMRV	MRV representation of synthesis joined
	 * with reaction ID numbers
	 * @return	new MRV of the synthesis
	 */
	public static String convertSynthesisFormat(String synMRV) {
		final String[] synComponents = getSynthesisComponents(synMRV);
		return (Utils.isEmpty(synComponents[RXNID]) ? synMRV
				: addRxnIds(synComponents[STRUC], synComponents[RXNID]));
	} // convertSynthesisFormat(String)

	/** Adds reaction ID numbers as a molecule property to an MRV describing a
	 * synthesis.
	 * @param	mrv	MRV representation of synthesis
	 * @param	rxnIds	reaction ID numbers
	 * @return	new MRV of the synthesis
	 */
	public static String addRxnIds(String mrv, String rxnIds) {
		return ChemUtils.setProperty(mrv, RXN_IDS, rxnIds);
	} // addRxnIds(String, String)

	/** Adds reaction ID numbers as a molecule property to a molecule 
	 * describing a synthesis.
	 * @param	mol	the imported synthesis
	 * @param	rxnIds	reaction ID numbers
	 */
	public static void addRxnIds(Molecule mol, String rxnIds) {
		mol.setProperty(RXN_IDS, rxnIds);
	} // addRxnIds(Molecule, String)

	//--------------------------------------------------------------------
	//					getSynthesisComponents
	//--------------------------------------------------------------------
	/** Converts a String that contains both an MRV file and a series of
	 * reactions ID numbers (old synthesis format) into a String array 
	 * with two members, the MRV and the reaction ID numbers.
	 * @param	synthStrPlusRxns	MRV representation of response joined
	 * with reaction ID numbers
	 * @return	array of Strings: first member is the MRV; second member
	 * contains reaction ID numbers of this synthesis
	 */
	static String[] getSynthesisComponents(String synthStrPlusRxns) {
		final String SELF = "Synthesis.getSynthesisComponents: ";
		String[] components = new String[2];
		if (!Utils.isEmpty(synthStrPlusRxns)) {
			final int mDocEndLocn = synthStrPlusRxns.lastIndexOf('>');
			if (mDocEndLocn >= 0) {
				final String trimmed = synthStrPlusRxns.trim();
				final int rxnsStart = mDocEndLocn + ">".length();
				if (rxnsStart <= trimmed.length()) {
					components[STRUC] = trimmed.substring(0, rxnsStart);
					components[RXNID] = trimmed.substring(rxnsStart).trim();
					if ("null".equals(components[RXNID]))
						components[RXNID] = null;
				} else {
					components[STRUC] = synthStrPlusRxns;
				} // if there is data past end of XML data
			} else {
				Utils.alwaysPrint(SELF
						+ "can't find final '>' when parsing multistep "
						+ "synthesis response string:\n", synthStrPlusRxns);
				components[STRUC] = synthStrPlusRxns;
			} // if ">" is in synthStrPlusRxns
		} else {
			debugPrint(SELF + "synthStrPlusRxns is null or empty.");
		} // if synthesis Q with previous response
		return components;
	} // getSynthesisComponents(String)

	//--------------------------------------------------------------------
	//  		getRxnsDisplay, getRxnsDisplayPhrases
	//--------------------------------------------------------------------
	/** Gets the phrases that need to be translated for getRxnsDisplay().
	 * @return	array of phrases
	 */
	public static String[] getRxnsDisplayPhrases() {
		final User NO_USER = null;
		return getRxnsDisplayPhrases(NO_USER);
	} // getRxnsDisplayPhrases()

	/** Gets the phrases that need to be translated for getRxnsDisplay().
	 * @param	user	the user
	 * @return	array of phrases
	 */
	public static String[] getRxnsDisplayPhrases(User user) {
		String[] translns = new String[3];
		translns[RXN_CONDS] = "Reaction condition";
		translns[ID_NUMS] = "ID numbers";
		translns[NONE_CHOSEN] = "No reactions chosen.";
		if (user != null) user.translate(translns);
		return translns;
	} // getRxnsDisplayPhrases(User)

	/** Converts reaction ID numbers in a synthesis response to an English
	 * description formatted for HTML.
	 * @param	synMRV	synthesis response
	 * @return	HTML string of the reactions
	 */
	public static String getRxnsDisplay(String synMRV) {
		return getRxnsDisplay(synMRV, getRxnsDisplayPhrases());
	} // getRxnsDisplay(String)

	/** Converts reaction ID numbers in a synthesis response to a
	 * description formatted for HTML.
	 * @param	synMRV	synthesis response
	 * @param	user	the user
	 * @return	HTML string of the reactions
	 */
	public static String getRxnsDisplay(String synMRV, User user) {
		return getRxnsDisplay(synMRV, getRxnsDisplayPhrases(user));
	} // getRxnsDisplay(String, User)

	/** Converts reaction ID numbers in a synthesis response to a
	 * description formatted for HTML.
	 * @param	synMRV	synthesis response
	 * @param	translns	translations into user's language of phrases used in
	 * display of reaction conditions
	 * @return	HTML string of the reactions
	 */
	public static String getRxnsDisplay(String synMRV, String[] translns) {
		final String SELF = "Synthesis.getRxnsDisplay: ";
		final String rxnsStr = getRxnConditions(synMRV);
		final String[] rxnIdStrs = (!Utils.isEmpty(rxnsStr) 
				? rxnsStr.split(RXN_ID_SEP) : new String[0]);
		final StringBuilder rxnsOut = new StringBuilder();
		for (int rxnNum = 0; rxnNum < rxnIdStrs.length; rxnNum++) {
			Utils.appendTo(rxnsOut, "<tr><td style=\"text-align:left;\">",
					translns[RXN_CONDS], ' ', rxnNum + 1, ": ");
			if (rxnIdStrs[rxnNum].endsWith(".")) {
				rxnIdStrs[rxnNum] = Utils.rightChop(rxnIdStrs[rxnNum], 1);
			} // if ends with period
			RxnCondition rxnCondn = RxnCondition.getRxnCondition(
					MathUtils.parseInt(rxnIdStrs[rxnNum])); 
			// ignore Jlint complaint about line above.  Raphael 11/2010
			if (rxnCondn == null) { // shouldn't happen
				rxnCondn = RxnCondition.getRxnCondition(NO_REAGENTS);
			} // if there is such a reaction condition
			Utils.appendTo(rxnsOut, Utils.toDisplay(rxnCondn.name), 
					"</td></tr>");
		} // for each rxnNum
		return rxnsOut.toString();
	} // getRxnsDisplay(String, String[])

	//--------------------------------------------------------------------
	//  				setStarterRules
	//--------------------------------------------------------------------
	/** Sets the rules about permissible starting materials.  Populates
	 * starterRules and exprCodeTokens.
	 * @param	qData	question data containing the rules
	 * @param	rgMols	R groups associated with the user's instance of the
	 * question; may be null or empty
	 */
	public void setStarterRules(QDatum[] qData, Molecule... rgMols) {
		final String SELF = "Synthesis.setStarterRules: ";
		if (qData == null) return;
		boolean noCombRule = true;
		int numRules = 0;
		for (final QDatum qDatum : qData) {
			if (qDatum.isSynOkSM()) {
				starterRules.add(new SynthStarterRule(qDatum.data, rgMols));
				numRules++;
			} else if (qDatum.isSynSMExpression()) {
				exprCodeTokens = CombineExpr.toTokens(qDatum.data);
				noCombRule = false;
			} else starterRules.add(null); // preserve serial nos. of rules
		} // for each qDatum
		if (noCombRule) {
			exprCodeTokens = CombineExpr.getDefaultTokens(numRules);
			for (int ruleNum = starterRules.size(); ruleNum >= 1; ruleNum--) {
				if (starterRules.get(ruleNum - 1) == null) {
					starterRules.remove(ruleNum - 1);
				} // if the rule is invalid
			} // for each rule
		} // if no question datum describes how to combine the rules
		debugPrint(SELF + "Expression for combining rules is: ",
				CombineExpr.postfixToEnglish(exprCodeTokens));
	} // setStarterRules(QDatum[], Molecule...)

	//--------------------------------------------------------------------
	//  				isPermissibleSM, satisfiesRule
	//--------------------------------------------------------------------
	/** Evaluates a postfix expression to determine whether a compound is a
	 * permissible starting material.  
	 * @param	mol	the compound
	 * @return	true if the compound is a permissible starting material
	 */
	boolean isPermissibleSM(Molecule mol) {
		final String SELF = "Synthesis.isPermissibleSM: ";
		debugPrint(SELF + " seeing if ", mol, 
				" is a permissible starting material.");
		final CombineExpr combineExpr = new CombineExpr(this, mol);
		final OneEvalResult evalResult = 
				combineExpr.isSatisfied(exprCodeTokens);
		final boolean isPermissibleSM = evalResult.isSatisfied;
		debugPrint(SELF, mol, " is", isPermissibleSM ? "" : " not",
				" a permissible starting material.");
		return isPermissibleSM;
	} // isPermissibleSM(Molecule)

	/** Evaluates whether a compound is a permissible starting material
	 * according to a single rule.  
	 * Called by CombineExpr.isSatisfied().
	 * @param	ruleNum	1-based number of the rule
	 * @param	mol	the compound
	 * @return	a OneEvalResult containing a boolean that is true if the 
	 * compound is a permissible starting material
	 */
	public OneEvalResult satisfiesRule(int ruleNum, Molecule mol) {
		final SynthStarterRule rule = starterRules.get(ruleNum - 1);
		final OneEvalResult evalResult = new OneEvalResult();
		evalResult.isSatisfied = rule.evaluate(mol);
		return evalResult;
	} // satisfiesRule(int, Molecule)

	//--------------------------------------------------------------------
	//  				getTargetStage
	//--------------------------------------------------------------------
	/** Gets the target stage of the synthesis.
	 * @return	the target stage of the synthesis
	 */
	public SynthStage getTargetStage() {
		return getStage(parsedSynth.targetStageIndex);
	} // getTargetStage()

	//--------------------------------------------------------------------
	//  				getAllResponseStarters
	//--------------------------------------------------------------------
	/** Gets all compounds in the response not produced by a reaction.
	 * @return	array of molecules nor produced by a reaction
	 */
	public Molecule[] getAllResponseStarters() {
		final String SELF = "Synthesis.getAllResponseStarters: ";
		final List<Molecule> starterList = new ArrayList<Molecule>();
		final List<SynthStage> stages = parsedSynth.getStages();
		for (final SynthStage stage : stages) {
			debugPrint(SELF + "Checking stage with box #",
					stage.getBoxIndex() + 1, ".");
			final boolean startersDetermined = stage.areStartersDetermined();
			if (startersDetermined) {
				final List<Molecule> starters = stage.getStarters();
				final int numStarters = starters.size();
				debugPrint(SELF + "Starters already calculated; "
						+ "adding ", numStarters, " starter(s) to the list: ",
						starters);
				starterList.addAll(starters);
			} else {
				/* this else code should not be necessary, but we include it
				 * in case of inexperienced authors.  Normally this method is
				 * called only after the reaction products have been checked,
				 * so the starters have already been calculated.
				 */
				final List<Molecule> stageCpds = stage.getEnumStereos();
				int numNotRxnProds = stageCpds.size();
				final List<Molecule> areRxnProducts = 
						new ArrayList<Molecule>();
				if (numNotRxnProds == 1)
					debugPrint(SELF + "Need to calculate "
							+ "whether this stage's one compound ",
							stageCpds, " is a starter or a product.");
				else debugPrint(SELF + "Need to calculate "
						+ "which of this stage's ", numNotRxnProds,
						" compounds ", stageCpds,
						" are starters and which are products.");
				// use calculated products from each previous stage to find out
				// which compounds in present stage are starting materials
				if (stage.hasPrevStage()) {
					final List<SynthStage> prevStages = 
							stage.getAllPrevStages();
					for (int prevIdx = 0; prevIdx < prevStages.size(); prevIdx++) {
						final SynthStage prevStage = prevStages.get(prevIdx);
						if (prevStage.getRxnStatus() == UNCHECKED) {
							prevStage.calculateRxnProducts();
						}
						final Molecule[] prevStageRxnProducts = 
								prevStage.getRxnProducts();
						debugPrint(SELF + "Previous stage's rxnProducts: ",
								prevStageRxnProducts);
						if (prevStageRxnProducts != null) {
							for (final Molecule prevStageProd : prevStageRxnProducts) {
								int prodArrayIdx = NOT_FOUND;
								try {
									prodArrayIdx = SynthSet.molInArray(
											prevStageProd, stageCpds);
								} catch (SearchException e) {
									debugPrint(SELF + "Threw a SearchException when "
											+ "looking at whether any reaction products "
											+ "from previous stage are present in "
											+ "current stage; keep going anyway.");
								} // try
								if (prodArrayIdx != NOT_FOUND) {
									debugPrint(SELF + "Found product ",
											prodArrayIdx + 1, " of previous stage ",
											prevIdx + 1, ", ", prevStageProd,
											", in current stage.");
									areRxnProducts.add(stageCpds.remove(prodArrayIdx));
								} // if found product of previous stage in current stage
								else debugPrint(SELF + "Did not find product ", 
										prodArrayIdx + 1, " of previous stage ", 
										prevIdx + 1, ", ", prevStageProd, 
										", in current stage.");
							} // for each product of the previous stage
						} // if prevStageRxnProducts isn't null
					} // for each previous stage
				} else {
					debugPrint(SELF + "Stage with box #", stage.getBoxIndex() + 1,
							" has no previous stages; must contain "
							+ "only starting materials.");
				} // if stage has previous stages
				// set the starting materials of this stage
				numNotRxnProds = stageCpds.size();
				debugPrint(SELF + "This stage's ", numNotRxnProds,
						" response starter(s)", stageCpds);
				stage.setStarters(stageCpds);
				final int numAreRxnProds = areRxnProducts.size();
				debugPrint(SELF + "This stage's ", numAreRxnProds,
						" response product(s)", areRxnProducts);
				stage.setNotStarters(areRxnProducts);
				stage.setStartersDetermined(true);
				if (numNotRxnProds > 0) {
					debugPrint(SELF + "Adding the former to the list of all "
							+ "response starters.");
					starterList.addAll(stageCpds);
				}
			} // if starters have not already been calculated
		} // for each stage
		final Molecule[] starters = Utils.molListToArray(starterList);
		final int numStarters = starters.length;
		debugPrint(SELF + "returning ", numStarters, 
				" response starter(s)", starters);
		return starters;
	} // getAllResponseStarters()
		
	//--------------------------------------------------------------------
	//  				getMaxLinearSteps
	//--------------------------------------------------------------------
	/** Gets the longest linear sequence of steps.
	 * @return	longest linear sequence in the synthesis
	 */
	public int getMaxLinearSteps() {
		final String SELF = "Synthesis.getMaxLinearSteps: ";
		final int maxPrevLinearSteps = 
				getTargetStage().getMaxPrevLinearSteps();
		debugPrint(SELF + "synthesis has ", maxPrevLinearSteps, 
				" in the longest linear sequence.");
		return maxPrevLinearSteps;
	} // getMaxLinearSteps()

	//--------------------------------------------------------------------
	//  				checkValidRxnProducts
	//--------------------------------------------------------------------
	/** For all stages, are their contents composed of starting materials
	 * and products of reactions of the previous stages?
	 * @throws	SynthError	if any stage contains contents that are neither
	 * permissible starting materials nor produced by reactions
	 */
	public void checkValidRxnProducts() throws SynthError {
		getTargetStage().checkValidRxnProducts();
	} // checkValidRxnProducts()

	//----------------------------------------------------------------------
	//					noRespProductsArePermissibleSMs
	//----------------------------------------------------------------------
	/** Determines whether any of the response products are permissible starting
	 * materials.
	 * @return	true if none of the response products are permissible starting
	 * materials.
	 * @throws	SynthError	if any of the response products are permissible 
	 * starting  materials.
	 */
	public boolean noRespProductsArePermissibleSMs() throws SynthError {
		return getTargetStage().noRespProductsArePermissibleSMs();
	} // noRespProductsArePermissibleSMs()

	//----------------------------------------------------------------------
	//					isSelective
	//----------------------------------------------------------------------
	/** Gets whether the chosen reactions are selective for the intermediates
	 * and products of the synthesis.  Works its way backwards from the target.
	 * @param	kind	whether to examine the synthesis for structure-,
	 * diastereo-, or enantioselectivity
	 * @throws	SynthError	if any step in the synthesis fails to show the kind
	 * of selectivity being examined
	 */
	public void isSelective(int kind) throws SynthError {
		isSelective(kind, NO_STEPOK);
	} // isSelective(int)
	
	/** Gets whether the chosen reactions are selective for the intermediates
	 * and products of the synthesis.  Works its way backwards from the target.
	 * @param	kind	whether to examine the synthesis for structure-,
	 * diastereo-, or enantioselectivity
	 * @param	stepOK	a reaction whose lack of selectivity should not cause
	 * an error to be thrown
	 * @throws	SynthError	if any step in the synthesis fails to show the kind
	 * of selectivity being examined
	 */
	public void isSelective(int kind, Synthesis stepOK) throws SynthError {
		getTargetStage().isSelective(kind, stepOK);
	} // isSelective(int, Synthesis)

	//----------------------------------------------------------------------
	//							checkForMenuReagent
	//----------------------------------------------------------------------
	/** Checks whether a substrate must instead be called from the menu.  Called
	 * from SynthParser, or from SynthSolver if the synthesis comes from a public 
	 * JSP page.
	 * @param	substrate	a molecule in a student's synthesis
	 * @throws	SynthError	if the substrate should be called from the reagent 
	 * menu
	 */
	static void checkForMenuReagent(Molecule substrate) throws SynthError {
		final NamedCompound[] menuRgts = SynthMenuOnlyRgts.getMenuOnlyRgts();
		for (final NamedCompound menuRgt : menuRgts) {
			try {
				if (MolCompare.matchPrecise(substrate, menuRgt.mol)) {
					throw new SynthError(
							"The highlighted stage contains a compound, "
							+ "***AlCl3***, that you should not write as "
							+ "one of the reagents, but should choose "
							+ "from the pulldown menu.", USE_MENU,
							menuRgt.name);
				} // if there's a match
			} catch (MolCompareException e) {
				debugPrint("can't happen in checkForMenuReagent");
			} // try
		} // for each menu reagent
	} // checkForMenuReagent(Molecule)

	//----------------------------------------------------------------------
	//							oneStepIs
	//----------------------------------------------------------------------
	/** Determines whether one step of this synthesis is a particular
	 * synthetic step.  The reference sequence may contain whole molecules
	 * or substructures.
	 * @param	authSyn	the synthetic sequence that may be contained in this
	 * synthesis; only the last two steps are examined
	 * @param	type	whether to look for entire structures or substructures
	 * @return	false if the synthetic sequence is not found in this synthesis
	 * @throws	SynthError	if the synthetic sequence is found in this synthesis,
	 * or if the structures in a stage can't be compared to
	 * the structures in the author's stage
	 */
	public boolean oneStepIs(Synthesis authSyn, int type) throws SynthError {
		return getTargetStage().oneStepIs(authSyn, type, RECURSE);
	} // oneStepIs(Synthesis, int)

	//----------------------------------------------------------------------
	//				substituteRGroups
	//----------------------------------------------------------------------
	/** Replaces generic numbered R groups in an author's synthesis with 
	 * instantiated R groups.  Must be done after parsing or atoms will stick
	 * outside of boxes.
	 * @param	rgMols	the R groups to be substituted
	 */
	public void substituteRGroups(Molecule[] rgMols) {
		getTargetStage().substituteRGroups(rgMols);
	} // substituteRGroups(Molecule[])

	//----------------------------------------------------------------------
	//				storeReaction
	//----------------------------------------------------------------------
	/** Stores a given reaction in the database, overwriting any existing
	 * reaction. Called by
	 * authortool/universalData/changeReactionResult2.jsp.
	 * @param	rxnId	the reaction ID
	 * @throws	SynthError	if there's not exactly one reaction arrow
	 */
	public void storeReaction(int rxnId) throws SynthError {
		final String SELF = "Synthesis.storeReaction: ";
		if (getNumArrows() != 1) throw new SynthError(SELF
				+ "not one reaction arrow");
		final SynthSolver solver = new SynthSolver();
		solver.storeReaction(getTargetStage(), rxnId);
	} // storeReaction(int)

	//----------------------------------------------------------------------
	//					isEqualTo
	//----------------------------------------------------------------------
	/** Determines whether two syntheses are exactly equal by comparing the
	 * contents and reaction conditions of each stage.  This definition of 
	 * equality is very rigid; for example, syntheses that use similar but 
	 * not identical reaction conditions will be judged different.
	 * @param	compSynth	the synthesis to be compared to this one
	 * @return	true if the syntheses are exactly equal
	 */
	public boolean isEqualTo(Synthesis compSynth) {
		return isEqualTo(compSynth, true);
	} // isEqualTo(Synthesis)

	/** Determines whether two syntheses are exactly equal by comparing the
	 * contents and maybe the reaction conditions of each stage.  
	 * @param	compSynth	the synthesis to be compared to this one
	 * @param	considerRxnConds	whether to consider reaction conditions in
	 * determining whether two syntheses are equal
	 * @return	true if the syntheses are equal
	 */
	public boolean isEqualTo(Synthesis compSynth, boolean considerRxnConds) {
		final String SELF = "Synthesis.isEqualTo: ";
		boolean equality = false;
		if (compSynth != null) {
			final SynthStage targetStage = getTargetStage();
			final SynthStage compTargetStage = compSynth.getTargetStage();
			equality = targetStage.isEqualTo(compTargetStage, considerRxnConds);
			debugPrint(SELF + "The syntheses are ", 
					equality ? "" : "not ", "equal, ", 
					considerRxnConds ? "considering" : "ignoring",
					" reaction conditions.");
		} else debugPrint(SELF + "The synthesis to compare is null.");
		return equality;
	} // isEqualTo(Synthesis, boolean)

} // Synthesis
