package com.epoch.synthesis;

import chemaxon.formats.MolImporter;
import chemaxon.formats.MolFormatException;
import chemaxon.sss.search.SearchException;
import chemaxon.struc.Molecule; 
import com.epoch.chem.ChemUtils;
import com.epoch.chem.MolString;
import com.epoch.chem.Normalize;
import com.epoch.chem.StereoFunctions;
import com.epoch.db.ReactorResultsRW;
import com.epoch.exceptions.DBException;
import com.epoch.synthesis.synthConstants.SynthConstants;
import com.epoch.translations.PhraseTransln;
import com.epoch.utils.Utils; 
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Calculates the products of a stage and the indicated reaction conditions. */
public class SynthSolver implements SynthConstants {

	void debugPrint(Object... msg) { 
		 Utils.printToLog(msg, SMARTS); 
	}

	void debugPrintMRV(Object... msg) { 
		 Utils.printToLog(msg, MRV); 
	}

	// members not called in SingleRxnSolver
	/** Positive when reaction definition chosen from ACE's list.  */
	private transient int rxnId = -1;
	/** Reaction condition.  */
	private transient RxnCondition rxnCondn = null;
	/** Reaction conditions definition.  Contains multiple reactions separated by
	 * dividers.  */
	private transient String reactionDef = null;
	/** List of original rxnProducts in case of subsequent RESUBJECT_TO.  */
	private transient final List<Molecule> preserveOrigRxnProducts = new ArrayList<Molecule>();
	/** List of most recent products in case of subsequent
	 * RESUBJECT_TO so that rxnProducts can be reset.  */
	private transient final List<Molecule> preserveNewRxnProducts = new ArrayList<Molecule>();
	/** Whether any of the reactions since the most recent OPEN_PAREN have produced
	 * products.  Allows a depth of nested reactions of only 1.  */
	private transient boolean clauseHasProds = false;
	/** Whether there were any products before a recent RESUBJECT statementin
	 * the reaction condition definition.  Allows a depth of nested reactions of
	 * only 1.  */
	private transient boolean beforeRESUBJclauseHasProds = false;
	/** Whether there have been any products since a recent RESUBJECT statementin
	 * the reaction condition definition.  Allows a depth of nested reactions of
	 * only 1.  */
	private transient boolean duringRESUBJclauseHasProds = false;
	
	// members called in SingleRxnSolver
	/** True when called from a public jsp page.  */
	transient boolean fromJSP = false;
	/** Substrates submitted by user.  */
	transient Molecule[] initArray = null;
	/** Number of substrates submitted by user.  */
	transient int numInitSubstrates = 0;
	/** Gets the starting materials from products.  Used by one of the public jsp
	 * pages only.  */
	transient boolean reverse = false;
	/** Sets where to begin the reaction index in cases of resubjecting starting
	 * materials to a new reaction condition. */
	transient int resubjReactorIndex = 0;
	
	// members whose values are set in SingleRxnSolver
	/** List of calculated products.  */
	transient List<Molecule> rxnProducts = new ArrayList<Molecule>();
	/** The required functional groups for the next group of reactions. */
	transient String fnalGrpsStr = null;
	/** Whether the current set of reactants does not contain required
	 * functional groups. */
	transient boolean wrongFnalGroups = false;
	/** Whether a reaction in the list was asymmetric. */
	boolean didAsymmetricRxn = false;

	//----------------------------------------------------------------------
	//			constructor
	//----------------------------------------------------------------------
	/** Constructor.  */
	public SynthSolver() {
		// intentionally empty
	}

	//----------------------------------------------------------------------
	//			getProducts
	//----------------------------------------------------------------------
	/** Gets the products from a group of reactants subject to particular
	 * reaction conditions.   Called from public/synthTest.jsp.
	 * @param	reactants	MRV of reactants
	 * @param	aRxnId	ID number of reaction conditions
	 * @return	array of molecules that were calculated to be products
	 * @throws	SynthError	if products could not be calculated
	 */
	public Molecule[] getProducts(String reactants, int aRxnId)
			throws SynthError {
		final String SELF = "SynthSolver.getProducts: ";
		fromJSP = true;
		try {
			final Molecule substrate = MolImporter.importMol(reactants);
			Normalize.normalizeNoClone(substrate);
			if (substrate == null) {
				Utils.alwaysPrint(SELF + "substrate is null!");
			} else return getProducts(substrate, aRxnId);
		} catch (MolFormatException e) {
			Utils.alwaysPrint(SELF + "caught MolFormatException on:\n",
				reactants, "\n", e.getMessage());
		}
		return new Molecule[0];
	} // getProducts(String, int)

	/** Gets the products from a group of reactants subject to particular
	 * reaction conditions.   Called from public/pasteSynth.jsp.
	 * @param	reactants	MRV of reactants
	 * @param	reactionDef	a reaction definition in MRV format
	 * @param	reverse	whether to run the reaction in reverse (calculate the
	 * starting materials from the products)
	 * @return	array of molecules that were calculated to be products
	 * @throws	SynthError	if products could not be calculated
	 */
	public Molecule[] getProducts(String reactants, String reactionDef,
			boolean reverse) throws SynthError {
		final String SELF = "SynthSolver.getProducts: ";
		fromJSP = true;
		try {
			final Molecule substrate = MolImporter.importMol(reactants);
			Normalize.normalizeNoClone(substrate);
			this.reactionDef = reactionDef.trim();
			this.reverse = reverse;
			return getProducts(substrate);
		} catch (MolFormatException e) {
			Utils.alwaysPrint(SELF + "caught MolFormatException on:\n",
					reactants, "\n", e.getMessage());
		}
		return new Molecule[0];
	} // getProducts(String, String, boolean)

	/** Gets the products from a group of reactants subject to particular
	 * reaction conditions.   Called from SynthStage and from above.
	 * Substrates have already been normalized.
	 * @param	substrate	reactants as a single Molecule
	 * @param	aRxnId	ID number of reaction conditions
	 * @return	array of molecules that were calculated to be products
	 * @throws	SynthError	if products could not be calculated
	 */
	Molecule[] getProducts(Molecule substrate, int aRxnId) throws SynthError {
		final String SELF = "SynthSolver.getProducts: ";
		rxnId = aRxnId;
		if (rxnId <= 0) {
			debugPrint(SELF + "invalid rxnId ", rxnId, ", changing to "
					+ "default NO_REAGENTS.");
			rxnId = NO_REAGENTS;
		} // if invalid rxnId
		rxnCondn = RxnCondition.getRxnCondition(rxnId);
		if (rxnCondn == null || rxnCondn.reactionDef == null) {
			debugPrint(SELF + "obtained reaction conditions "
					+ "are null, changing to default NO_REAGENTS.");
			rxnId = NO_REAGENTS;
			rxnCondn = RxnCondition.getRxnCondition(rxnId);
		} else {
			debugPrint(SELF + "reaction conditions name is ", rxnCondn.name);
		} // if unknown reaction
		reactionDef = rxnCondn.reactionDef.trim();
		final String substrateStr = MolString.toString(substrate, SMILES);
		String[] calcdProdStrs = null;
		if (!fromJSP) try {
			debugPrint(SELF + "looking in database for products previously "
					+ "calculated from ", substrateStr, 
					" for reaction conditions ", rxnCondn.name);
			calcdProdStrs = 
					ReactorResultsRW.getCalcdProducts(substrateStr, rxnId);
		} catch (DBException e) { 
			debugPrint(SELF + "could not get calculated products "
					+ "from database."); 
			e.printStackTrace();
		} // try
		Molecule[] calcdProds = null;
		if (Utils.isEmpty(calcdProdStrs)) {
			debugPrint(SELF + "did not get calculated products from "
					+ "database; calculating them instead.");
			calcdProds = getProducts(substrate.clone());
			if (!fromJSP) try {
				debugPrint(SELF + "adding to database ",
						calcdProds.length, " newly "
						+ "calculated product(s): ", calcdProds);
				calcdProdStrs = convertForStorage(calcdProds);
				ReactorResultsRW.addCalcdProducts(substrateStr, 
						rxnId, calcdProdStrs);
			} catch (DBException e) { 
				debugPrint(SELF + "could not add calculated products ",
						calcdProds, " to database."); 
				e.printStackTrace();
			} // try
		} else if (Utils.membersAreEmpty(calcdProdStrs)) {
			debugPrint(SELF + "calculated products from database "
					+ "are empty; returning empty array.");
			calcdProds = new Molecule[0];
		} else try {
			debugPrint(SELF + "got calculated products from database:\n",
					calcdProdStrs);
			final List<Molecule> calcdProdsList = new ArrayList<Molecule>();
			for (final String calcdProdStr : calcdProdStrs) {
				final Molecule mol = MolImporter.importMol(calcdProdStr);
				calcdProds = mol.clone().convertToFrags();
				if (calcdProds.length == 1) {
					calcdProds = new Molecule[] {mol};
				} // if there is only one calculated product
				for (final Molecule calcdProd : calcdProds) {
					// calculated products in database may be in SMILES 
					// format, need to be converted to 2D
					StereoFunctions.convert0DTo2D(calcdProd);
					calcdProdsList.add(calcdProd);
				} // for each fragment 
			} // for each calculated product
			calcdProds = calcdProdsList.toArray(
					new Molecule[calcdProdsList.size()]);
			debugPrint(SELF + "got calculated products from database: ",
					calcdProds);
		} catch (MolFormatException e) {
			Utils.alwaysPrint(SELF + "caught MolFormatException "
					+ "when trying to convert ", calcdProdStrs, 
					" to molecules; calculating anew.");
			calcdProds = getProducts(substrate.clone());
		} // try
		debugPrintMRV(SELF + "calcdProds:\n", calcdProds);
		return calcdProds;
	} // getProducts(Molecule, int)

	/** Gets the products from a group of reactants subject to this instance's
	 * reaction conditions.
	 * @param	substrate	reactants as a single Molecule
	 * @return	array of molecules that were calculated to be products
	 * @throws	SynthError	if products could not be calculated
	 */
	private Molecule[] getProducts(Molecule substrate) throws SynthError {
		final String SELF = "SynthSolver.getProducts: ";
		final String numInitSubstratesStr =
				ChemUtils.getProperty(substrate, ORIG_MOL_COUNT);
		initArray = substrate.convertToFrags();
		numInitSubstrates = (numInitSubstratesStr == null
				? initArray.length : Integer.parseInt(numInitSubstratesStr));
		if (numInitSubstrates > 2 
				&& rxnCondn != null 
				&& !rxnCondn.threeComponent) {
			Utils.alwaysPrint(SELF + "too many reactants (",
					numInitSubstrates, ") initially; throwing SynthError.");
			throw new SynthError(TOO_MANY_REACTANTS);
		}
		final StringBuilder out = new StringBuilder();
		boolean first = true;
		for (final Molecule starter : initArray) {
			if (first) first = false;
			else out.append('.');
			if (rxnId > 0 && fromJSP) { // otherwise, checked upon parsing
				try {
					Synthesis.checkForMenuReagent(starter);
				} catch (SynthError e) {
					// replace ***AlCl3*** in feedback with actual reagent
					final String[] pieces = e.getMessage().split(
							PhraseTransln.STARS_REGEX);
					throw new SynthError(Utils.toString(pieces[0],
							e.calcdProds, pieces[2]), e.errorNumber);
				} // try
			} // if from public JSP page, reaction chosen from menu
			ChemUtils.stripMetalsNoClone(starter);
			out.append(MolString.toString(starter, SMARTS));
		} // for each substrate
		debugPrint(SELF + "Initial substrates are ", initArray.length, 
				": ", out.length() == 0 ? "[none]" : out.toString());
		// debugPrint(reactionDef);
		getProducts();
		debugPrint(SELF + "After all reactions, ",
				rxnProducts.size(), " product",
				(rxnProducts.size() == 1 ? " is: " : "s are: "),
				rxnProducts);
		// enumerate stereoisomers at unspecified configurations
		final Molecule[] products = Utils.molListToArray(
				StereoFunctions.enumerateStereo(rxnProducts));
		if (Utils.isEmpty(products))
			debugPrint(SELF + "After enumerating "
					+ "stereoisomers, returning no products.");
		else debugPrint(SELF + "After enumerating "
				+ "stereoisomers, returning ", products.length, " product",
				(products.length == 1 ?  ": " : "s: "), products);
		return products;
	} // getProducts(Molecule)

	/** Gets the products from this instance's substrates and
	 * reaction conditions.   Called recursively.
	 * @return	true if products were obtained
	 * @throws	SynthError	if products could not be calculated
	 */
	private boolean getProducts() throws SynthError {
		final String SELF = "SynthSolver.getProducts: ";
		debugPrint("Entering getProducts().");
		boolean prevRxnDividerRESUBJECT_TO = false;
		if (reactionDef.substring(0, lenRESUBJ).equals(RESUBJECT_TO)) {
			prevRxnDividerRESUBJECT_TO = true;
			reactionDef = reactionDef.substring(lenRESUBJ).trim();
		} // if the reaction definition begins with RESUBJECT_TO
		// look for functional group requirements for this and subsequent
		// reactions (until functional groups have changed)
		if (reactionDef.substring(0, lenFNAL).equals(FNAL_GROUPS)) {
			wrongFnalGroups = false; // new functional group definition
			reactionDef = reactionDef.substring(lenFNAL).trim();
			final int fnalGrpsEnd = reactionDef.indexOf(FNAL_GROUPS_END);
			fnalGrpsStr = (fnalGrpsEnd <= 0 ? ""
					: reactionDef.substring(0, fnalGrpsEnd).trim());
			if ("".equals(fnalGrpsStr))
				debugPrint("No functional groups required for this "
						+ "and subsequent reactions.");
			else debugPrint(fnalGrpsStr ,
					(fnalGrpsStr.indexOf(';') < 0 ? " is" : " are"),
					" required for this and subsequent reactions.");
			reactionDef = reactionDef.substring(fnalGrpsEnd + lenFNAL_END).trim();
		}
		if (reactionDef.substring(0, lenOPEN).equals(OPEN_PAREN_STR)) {
			// remove OPEN_PAREN from top
			debugPrint("Have arrived at OPEN_PAREN.");
			reactionDef = reactionDef.substring(lenOPEN);
			final boolean rxnProdsObtained = getProducts();
			if (rxnProdsObtained) clauseHasProds = true;
			// remove up to and including appropriate CLOSE_PAREN;
			// need to account for intervening parens
			int numCloseParensToRemove = 1;
			while (numCloseParensToRemove > 0) {
				final int closeIdx = reactionDef.indexOf(CLOSE_PAREN_STR);
				final int openIdx = reactionDef.indexOf(OPEN_PAREN_STR);
				if (openIdx >= 0 && openIdx < closeIdx) {
					numCloseParensToRemove++;
					reactionDef =
							reactionDef.substring(openIdx + lenOPEN);
				} else if (closeIdx >= 0) {
					if (closeIdx + lenCLOSE + 1 < reactionDef.length()) {
						reactionDef =
								reactionDef.substring(closeIdx + lenCLOSE).trim();
						numCloseParensToRemove--;
					} else {
						reactionDef = null;
						numCloseParensToRemove = 0;
					}
				} else {
					Utils.alwaysPrint(SELF + "couldn't find close parenthesis after open "
							+ "parenthesis in reaction definition; aborting.");
					return clauseHasProds;
				} // if closeIdx >= 0
			} // while there are CLOSE_PARENS left to remove
		} else { // string must begin with reaction definition
			debugPrint("At beginning of new reaction definition.");
			// find end of current definition
			final int closeIdx = reactionDef.indexOf(CLOSE_PAREN_STR);
			final int andIdx = reactionDef.indexOf(AND_DO);
			final int ifIdx = reactionDef.indexOf(IF_NO_PRODS_DO);
			final int resubjIdx = reactionDef.indexOf(RESUBJECT_TO);
			final int[] indices = {closeIdx, andIdx, ifIdx, resubjIdx};
			Arrays.sort(indices);
			int first = -1;
			for (final int index : indices)
				if (index != -1) {
					first = index;
					break;
				}
			// submit current reaction definition for calculation
			boolean rxnProdsObtained = false;
			final SingleRxnSolver singleSolver = new SingleRxnSolver(this);
			if (first >= 0) { // there's more reaction definition after this one
				debugPrint("There are more reaction definitions "
						+ "after this one; next divider is ",
						(first == closeIdx ? CLOSE_PAREN_STR
							: first == andIdx ? AND_DO
							: first == ifIdx ? IF_NO_PRODS_DO
							: RESUBJECT_TO), ".");
				if (first == resubjIdx)
					beforeRESUBJclauseHasProds = clauseHasProds;
				final String singleRxnDef = reactionDef.substring(0, first);
				debugPrint("Submitting for calculation.\n\n");
				rxnProdsObtained = singleSolver.calcProducts(singleRxnDef);
				if (first == resubjIdx) resubjReactorIndex++;
				else resubjReactorIndex = 0;
				reactionDef = reactionDef.substring(first).trim();
			} else { // last reaction definition
				debugPrint("The current reaction definition is the last one.");
				debugPrint("Submitting for calculation.\n\n");
				rxnProdsObtained = singleSolver.calcProducts(reactionDef);
				reactionDef = null;
			} // if first < 0
			if (rxnProdsObtained) clauseHasProds = true;
		} // if there is/isn't a paren at the top
		debugPrint("\n\nBack from calculation: rxnProducts is ",
				rxnProducts, " and clauseHasProds is now ", clauseHasProds);
		// restore rxn products from preserveNewRxnProducts
		if (prevRxnDividerRESUBJECT_TO && (reactionDef == null
				|| !reactionDef.substring(0, lenRESUBJ).equals(RESUBJECT_TO))) {
			clauseHasProds = duringRESUBJclauseHasProds || clauseHasProds;
			duringRESUBJclauseHasProds = false;
			debugPrint("Previous divider was RESUBJECT_TO, next one "
					+ "is not; setting clauseHasProds to ", clauseHasProds, " ...");
			restoreRxnProducts();
			preserveNewRxnProducts.clear();
		} // if the previous divider was RESUBJECT_TO and next one is not
		// what closes this reaction definition?
		if (reactionDef != null) {
			if (reactionDef.substring(0, lenCLOSE).equals(CLOSE_PAREN_STR)) {
				debugPrint("The next divider is CLOSE_PAREN.");
				return clauseHasProds; // will strip the CLOSE_PAREN upon return
			} else if (reactionDef.substring(0, lenRESUBJ).equals(RESUBJECT_TO)) {
				duringRESUBJclauseHasProds =
						duringRESUBJclauseHasProds || clauseHasProds;
				clauseHasProds = beforeRESUBJclauseHasProds;
				debugPrint("The next divider is RESUBJECT_TO; "
						+ "setting clauseHasProds to ", clauseHasProds,
						" and duringRESUBJclauseHasProds to ",
						duringRESUBJclauseHasProds,
						"; preserving products of previous reaction in "
						+ "preserveNewRxnProducts, resetting rxnProducts "
						+ "to previous value.");
				preserveRxnProducts();
				return getProducts();
			} else if (reactionDef.substring(0, lenAND).equals(AND_DO)) {
				debugPrint("The next divider is AND_DO.");
				debugPrint("Clearing preserveOrigRxnProducts, resetting "
						+ "to current value of rxnProducts.");
				preserveOrigRxnProducts.clear();
				preserveOrigRxnProducts.addAll(rxnProducts);
				debugPrint("preserveOrigRxnProducts is now: ",
						preserveOrigRxnProducts);
				reactionDef = reactionDef.substring(lenAND).trim();
				return getProducts();
			} else if (reactionDef.substring(0, lenIF).equals(IF_NO_PRODS_DO)) {
				debugPrint("The next divider is IF_NO_PRODS_DO.");
				if (!clauseHasProds) {
					debugPrint("No products in this clause so far; keep trying.");
					// keep trying for products
					reactionDef = reactionDef.substring(lenIF).trim();
					return getProducts();
				} else {
					debugPrint("We have products in this clause; "
							+ "end our efforts in this clause.");
					// strip to CLOSE_PAREN or end of file
					final int closeIdx = reactionDef.indexOf(CLOSE_PAREN_STR);
					debugPrint("Stripping reactionDef to ",
							(closeIdx > 0 ? "close parenthesis."
								: "end of file."));
					reactionDef = (closeIdx > 0 ?
							reactionDef.substring(closeIdx).trim() : null);
					return clauseHasProds;
				} // if there are no rxnProducts
			} else {
				Utils.alwaysPrint(SELF
						+ "unknown reaction definition divider: \n",
						Utils.chopString(reactionDef, 50));
			} // what is the next divider?
		} else { // reactionDef is null
			debugPrint("No more reaction definitions.");
		} // if reactionDef is or isn't null
		return clauseHasProds;
	} // getProducts()
	
	//----------------------------------------------------------------------
	//			storeReaction
	//----------------------------------------------------------------------
	/** Stores a user-written reaction in the database. Used to override faulty
	 * JChem calculations.
	 * @param	prodStage	product stage of a parsed synthesis
	 * @param	rxnId	the reaction ID
	 * @throws	SynthError	if there's a problem writing to the database
	 */
	void storeReaction(SynthStage prodStage, int rxnId) throws SynthError {
		final String SELF = "SynthSolver.storeReaction: ";
		final Molecule[] prods = prodStage.getMoleculeArray(); // could be empty
		final String[] prodStrs = convertForStorage(prods);
		final SynthStage startersStage = prodStage.getPrevStage(0);
		final Molecule startersMol = startersStage.getFusedMolecule();
		final String startersStr = MolString.toString(startersMol, SMILES);
		debugPrint(SELF + "For reaction ID ", rxnId, 
				", storing startersStr:\n", startersStr,
				"\nand products:\n", prodStrs);
		try {
			ReactorResultsRW.deleteCalcdProducts(rxnId, startersStr);
			ReactorResultsRW.addCalcdProducts(startersStr, rxnId, prodStrs);
		} catch (DBException e) {
			e.printStackTrace();
			throw new SynthError(SELF + e.getMessage());
		} // try
	} // storeReaction(SynthStage, int)

	//----------------------------------------------------------------------
	//			preserveRxnProducts
	//----------------------------------------------------------------------
	/** Removes products from recent reactions from the list of reaction
	 * products, puts them in a different list, then restores products from
	 * earlier reactions to the list of reaction products. */
	private void preserveRxnProducts() {
		for (final Molecule prod : rxnProducts) {
			final String reactedProp = ChemUtils.getProperty(prod, REACTED);
			try {
				if ((reactedProp == null || !TRUE.equals(reactedProp))
						&& SynthSet.molInArray(prod, preserveNewRxnProducts)
								== NOT_FOUND) {
					preserveNewRxnProducts.add(prod);
					debugPrint("Product ", prod,
							" from products of previous reaction(s) is "
							+ "not already in preserveNewRxnProducts "
							+ "and has not already reacted to give a "
							+ "new product; adding it.");
				} // if the product is not already in preserveNewRxnProducts
			} catch (SearchException e) {
				Utils.alwaysPrint("SearchException caught while "
						+ "merging previous products into "
						+ "preserveNewRxnProducts.");
			} // try
		} // for each rxnProduct of prior reaction
		rxnProducts.clear();
		rxnProducts.addAll(preserveOrigRxnProducts);
		debugPrint("rxnProducts is now: ", rxnProducts);
	} // preserveRxnProducts()

	//----------------------------------------------------------------------
	//			restoreRxnProducts
	//----------------------------------------------------------------------
	/** Restores products from previous reactions to the list of reaction
	 * products. */
	private void restoreRxnProducts() {
		debugPrint("... removing previously reacted rxnProducts from the list ...");
		final int size = rxnProducts.size();
		for (int prodIdx = size - 1; prodIdx >= 0; prodIdx--) {
			final String reactedProp =
					ChemUtils.getProperty(rxnProducts.get(prodIdx), REACTED);
			if (TRUE.equals(reactedProp)) {
				final Molecule rxnProd = rxnProducts.remove(prodIdx);
				debugPrint("Product ", rxnProd,
						" has previously reacted; removing it.");
			} // if rxnProd has reacted
		} // for each rxnProd
		debugPrint("... and merging products in preserveNewRxnProducts "
				+ "into rxnProducts....");
		for (final Molecule prod : preserveNewRxnProducts) {
			try {
				if (SynthSet.molInArray(prod, rxnProducts) == NOT_FOUND) {
					rxnProducts.add(prod);
					debugPrint("Product ", prod,
							" from preserved reaction products is "
							+ "not already in initial substrate "
							+ "or product list; adding to "
							+ "rxnProducts.");
				} // if the product is not already in rxnProducts
			} catch (SearchException e) {
				Utils.alwaysPrint("SearchException caught while "
						+ "merging preserved products into rxnProducts.");
			} // try
		} // for each preserved rxnProduct
		debugPrint("rxnProducts is now: ", rxnProducts);
	} // restoreRxnProducts()

	//----------------------------------------------------------------------
	//			convertForStorage
	//----------------------------------------------------------------------
	/** Converts molecules to strings, merging them and converting to SMILES
	 * if they don't have properties that need to be preserved, converting into
	 * MRV otherwise.
	 * @param	calcdProds	calculated products
	 * @return	array of Strinsg representing the calculated products
	 */
	private String[] convertForStorage(Molecule... calcdProds) {
		final String SELF = "SynthSolver.convertForStorage: ";
		final int numCalcdProds = calcdProds.length;
		debugPrintMRV(SELF + "converting to ", calcdProds.length, 
				" string(s):\n", calcdProds);
		final List<String> calcdProdStrs = new ArrayList<String>();
		if (numCalcdProds > 1) {
			final Molecule fused = new Molecule();
			for (final Molecule calcdProd : calcdProds) {
				if (isPropertied(calcdProd)) {
					debugPrint(SELF, calcdProd, " has molecule "
							+ "properties; saving it separately.");
					calcdProdStrs.add(MolString.toString(calcdProd, MRV));
				} else {
					debugPrint(SELF, calcdProd, " has no molecule "
							+ "properties; fusing it with other products.");
					fused.fuse(calcdProd.clone());
				} // if can be merged into single molecule
			} // for each calculated product
			if (fused.getAtomCount() > 0) {
				calcdProdStrs.add(MolString.toString(fused, SMILES));
			} // if any molecules were fused
		} else if (numCalcdProds == 1) {
			calcdProdStrs.add(MolString.toString(calcdProds[0], 
					isPropertied(calcdProds[0]) ? MRV : SMILES));
		} else {
			calcdProdStrs.add("");
		} // if there are calculated products
		debugPrint(SELF + "returning strings:\n", calcdProdStrs);
		return calcdProdStrs.toArray(new String[calcdProdStrs.size()]);
	} // convertForStorage(Molecule...)

	//----------------------------------------------------------------------
	//			isPropertied
	//----------------------------------------------------------------------
	/** Gets whether the molecule has properties that need to be preserved when
	 * converting to string form.
	 * @param	mol	the molecule
	 * @return	true if the molecule has properties that won't be preserved in
	 * SMILES format
	 */
	private boolean isPropertied(Molecule mol) {
		return ChemUtils.getProperty(mol, MAJ_MIN_PROD) != null
				|| ChemUtils.getProperty(mol, REACTOR_INDEX) != null
				|| ChemUtils.getProperty(mol, RACEMIZE) != null;
	} // isPropertied(Molecule)

} // SynthSolver
