package com.epoch.synthesis;

import static com.epoch.synthesis.synthConstants.SingleRxnConstants.*;
import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.reaction.ReactionException;
import chemaxon.reaction.Reactor;
import chemaxon.sss.SearchConstants;
import chemaxon.sss.search.MolSearchOptions;
import chemaxon.sss.search.SearchException;
import chemaxon.struc.MolAtom;
import chemaxon.struc.Molecule;
import chemaxon.struc.RxnMolecule;
import chemaxon.struc.StereoConstants;
import com.epoch.chem.ChemUtils;
import com.epoch.chem.FnalGroupDef;
import com.epoch.chem.MolCompareException;
import com.epoch.chem.StereoFunctions;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.chemEvals.FnalGroup;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.ParameterException;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Calculates the products of a single reaction. */
class SingleRxnSolver implements SearchConstants, StereoConstants {

	/** The caller instance that created this instance. */
	transient private SynthSolver caller = null;
	/** JChem's reaction calculator. */
	transient private final Reactor reactor = new Reactor();
	/** MRV reaction definition imported into a molecule. */
	transient private Molecule moleculeOfRxn = null;
	/** The number of substrates required by the reaction. */
	transient private int desiredNumSubstrates;
	/** Describes a product as major or minor, as in exo Diels-Alder. */
	transient private String majorOrMinor = null;
	/** Describes how or why a product is major or minor. */
	transient private String majorOrMinorExplan = null;
	/** Whether to look for products of B + A after A + B has
	 * produced a product. */
	transient private boolean keepPermuting;
	/** Whether to allow substrates to react with themselves. */
	transient private boolean allowDimerization;
	/** Whether the reaction produces just one enantiomer. */
	transient private boolean isAsymmetric;
	/** Whether the enantiomer of an enantiopure starting material was produced
	 * by the reaction, and if so, whether the starting material was part of the 
	 * initial array of substrates or a reaction product. */
	transient private int generatedSMStereo = NO_SM_RACEMIZED;
	/** The number of times the calculations were restarted because the
	 * enantiomer of an enantiopure starting material was produced in the
	 * reaction, and we had to restart the calculations with racemic starting
	 * material. */
	transient private int restartedAlready = 0;
	/** Whether the reaction is a Diels-Alder reaction. */
	transient private boolean isDielsAlder = false;

	/** Substrates submitted by user.  Value obtained from caller.  */
	transient private Molecule[] initArray = null;
	/** List of calculated products.  Value obtained from caller.  */
	transient private List<Molecule> rxnProducts = new ArrayList<Molecule>();

	/** Constructor. */
	SingleRxnSolver() {
		// intentionally empty
	}
	
	/** Constructor.  Members of caller are copied into variables
	 * here merely for convenience's sake.
	 * @param	theCaller	SynthSolver instance that called this instance
	 */
	SingleRxnSolver(SynthSolver theCaller) {
		caller = theCaller;
		initArray = caller.initArray;
		rxnProducts = caller.rxnProducts;
	} // SingleRxnSolver(SynthSolver)
	
	/** Calculates the products of a reaction.
	 * @param	singleRxnDef	MRV of the reaction definition
	 * @return	true if products were obtained, false if not
	 * @throws	SynthError	if products (or lack thereof) could not be calculated
	 */
	boolean calcProducts(String singleRxnDef) throws SynthError {
		final String SELF = "SingleRxnSolver.calcProducts: ";
		if (caller.wrongFnalGroups) {
			caller.debugPrint(SELF + "Not even trying reaction because "
					+ "needed functional groups won't be present.");
			return false;
		} // if should abort right away
		boolean rxnProdsObtained = false;
		RxnMolecule rxnMol = null;
		try {
			// set up the reactor object
			// caller.debugPrint(singleRxnDef);
			moleculeOfRxn = MolImporter.importMol(singleRxnDef.trim());
			if (wrongNumSubstrates()) {
				caller.debugPrint(SELF + "Not even trying reaction because "
							+ "wrong number of substrates was submitted.");
				return false;
			} // if should abort right away
			// If needed functional groups are absent from all rxnProducts and
			// initArray members, can't have a reaction.
			if (!Utils.isEmpty(caller.fnalGrpsStr)) {
				final List<Molecule> allPossibleSubstrates =
						new ArrayList<Molecule>();
				allPossibleSubstrates.addAll(rxnProducts);
				allPossibleSubstrates.addAll(Utils.molArrayToList(initArray));
				if (substratesLackFnalGroup(allPossibleSubstrates)) {
					caller.debugPrint(SELF + "Aborting reaction due to lack "
							+ "of needed functional groups.");
					// if needed functional groups are absent now, they won't
					// appear until the fnal group requirements change
					caller.wrongFnalGroups = true;
					return false;
				} // if needed fnal groups are absent
			} // if there are fnal group restrictions

			final String name = ChemUtils.getProperty(moleculeOfRxn, RXN_NAME);
			caller.debugPrint("\nReaction: ", name);
			isDielsAlder = name != null && name.indexOf("Diels-Alder") >= 0;
			rxnMol = RxnMolecule.getReaction(moleculeOfRxn);
			if (rxnMol == null) {
				Utils.alwaysPrint(SELF + "reaction definition molecule is null.");
				return false;
			} // if rxnMol is null
			reactor.setSearchOptions(getSearchOptions());
			reactor.setReaction(rxnMol);
			if (caller.reverse) {
				reactor.setReverse(true);
				caller.debugPrint(SELF + "Running reaction in reverse!!!");
			} // if should run reaction in reverse
			desiredNumSubstrates = reactor.getReactantCount();
			if (isDielsAlder) reactor.setOutputReactionMappingStyle(
					Reactor.MAPPING_STYLE_MATCHING);
			
			majorOrMinor = ChemUtils.getProperty(moleculeOfRxn, MAJ_MIN_PROD);
			majorOrMinorExplan = ChemUtils.getProperty(moleculeOfRxn, MAJ_MIN_EXPLAN);
			caller.debugPrint("Major or minor = ", majorOrMinor,
					", explanation = ", majorOrMinorExplan);
			keepPermuting = 
					TRUE.equals(ChemUtils.getProperty(moleculeOfRxn, KEEP_PERMUTING));
			allowDimerization = 
					TRUE.equals(ChemUtils.getProperty(moleculeOfRxn, ALLOW_DIMER));
			isAsymmetric = TRUE.equals(ChemUtils.getProperty(moleculeOfRxn, ASYM));
			// some reactions, such as deprotonation, should not have products
			// resubjected to the same reaction conditions
			final boolean doNotResubjectProds = TRUE.equals(
					ChemUtils.getProperty(moleculeOfRxn, STOP_AFTER_1));

			// Loop through substrates and any products through the
			// reaction repeatedly until no more new products are
			// generated.  Important:  We need to avoid resubjecting to the
			// reaction conditions compounds that can react in
			// intramolecular fashion, or we will get polymers, evidenced
			// by an infinite recursive loop.  We can take care of this
			// case by Number of reactants property for intermolecular
			// reactions, so a single bifunctional substrate isn't
			// duplicated and allowed to react with itself repeatedly.
			for (int loopCt = 1; loopCt <= MAX_ONE_RXN_LOOPS; loopCt++) {
				caller.debugPrint(SELF + "Reaction: ", name,
						"; submitting substrates for the ", loopCt,
						(loopCt % 10 == 1 ? "st"
							: loopCt % 10 == 2 ? "nd"
							: loopCt % 10 == 3 ? "rd" : "th"),
						" time. \nThis time, reaction products are: ",
						rxnProducts);
				// Molecules in rxnProducts are modified by this method too
				final List<Molecule> thisRxnLoopProducts =
						doOneRxnLoop(loopCt);
				if (generatedSMStereo == INIT_ARRAY) {
					caller.debugPrint("A stereoisomer of an initial starting "
							+ "material was produced in the reaction; "
							+ "start all over with both stereoisomers of "
							+ "starting material: new initArray is ", initArray);
					if (restartedAlready > NUM_RESTARTS) {
						caller.debugPrint("Sorry, can't restart more than ", 
								NUM_RESTARTS, " times.");
					} else {
						loopCt = 0;
						rxnProducts.clear();
						generatedSMStereo = NO_SM_RACEMIZED;
						restartedAlready++;
						continue;
					} // if haven't already restarted too many times
				} // if one of the initial members of the array was racemized
				final boolean thisLoopRxnProductsObtained =
						!thisRxnLoopProducts.isEmpty();
				if (thisLoopRxnProductsObtained) {
					rxnProdsObtained = true;
					caller.debugPrint(SELF + "For loop ", 
							loopCt, ", thisRxnLoopProducts is: ",
							thisRxnLoopProducts);
				} // if there are novel products from this loop
				// remove reacted rxn products from rxnProducts,
				// add products from this loop.  (Even if no products were
				// obtained, some rxnProducts may have reacted to give products
				// already in the list.)
				updateRxnProducts(rxnProducts, thisRxnLoopProducts);
				if (!thisLoopRxnProductsObtained) {
					caller.debugPrint(SELF + "No more products of this "
							+ "reaction; halting this reaction loop.");
					break;
				} // if there are no products of this loop
				if (doNotResubjectProds) {
					caller.debugPrint(SELF + "Reaction definition prohibits "
							+ "resubjecting products to reaction "
							+ "conditions; halting this reaction loop.");
					break;
				} // if doNotResubjectProds
			} // while the reaction is returning new products
		} catch (MolFormatException e) {
			Utils.alwaysPrint(SELF + "caught MolFormatException " 
					+ "on reaction definition:\n",
					singleRxnDef, "\n", e.getMessage());
			e.printStackTrace();
		} catch (ReactionException e) {
			Utils.alwaysPrint(SELF + "caught "
					+ "ReactionException:\n", e.getMessage());
			Utils.alwaysPrintMRV(
					"Reaction molecule that caused exception:\n", rxnMol);
			e.printStackTrace();
			Utils.alwaysPrint(SELF + "throwing SynthError.");
			throw new SynthError(e.getMessage(), SynthError.BAD_DEFINITION);
		}
		return rxnProdsObtained;
	} // calcProducts(String)

	/** Gets parameters that need to be set for syn-anti double bond
	 * stereochemistry to work.
	 * @return	a string setting the reactor parameters
	 */
	private String getSearchOptions() {
		final MolSearchOptions mso = 
				new MolSearchOptions(MolSearchOptions.SUBSTRUCTURE);
		mso.setOrderSensitiveSearch(true);
		mso.setDoubleBondStereoMatchingMode(DBS_ALL);
		mso.setMarkushEnabled(false);
		mso.setUndefinedRAtom(UNDEF_R_MATCHING_ALL);
		return mso.toString();
	} // getSearchOptions()

	/** Find if the reaction disallows wrong number of substrates and,
	 * if so, if the wrong number has been submitted.
	 * @return	true if the wrong number of substrates has been submitted
	 */
	private boolean wrongNumSubstrates() {
		final String numRgtsStr = ChemUtils.getProperty(moleculeOfRxn, NUM_REACTS);
		if (numRgtsStr != null) {
			try {
				final int numRgts = Integer.parseInt(numRgtsStr);
				if (numRgts != caller.numInitSubstrates) {
					caller.debugPrint("Number of reagents required to be "
							+ "submitted by the reaction property is ", 
							numRgts, "; initial number actually submitted is ",
							caller.numInitSubstrates, "; aborting this reaction.");
					return true;
				} // if numRgts != numInitSubstrates
			} catch (NumberFormatException e) { // due to parseInt
				caller.debugPrint("Bad number for property 'Number of reactants'.");
			} // try
		} // if there is a Number of reactants property
		return false;
	} // wrongNumSubstrates(Molecule, int)
	
	/** Finds one set of substrates that, when submitted to Reactor, generates
	 * products.  Adds properties to the products.
	 * @param	loopCt	how many times the substrates have been subjected to
	 * this reaction
	 * @return	products as a list of molecules
	 * @throws	ReactionException	if the reaction products can't be calculated
	 */
	private List<Molecule> doOneRxnLoop(int loopCt) throws ReactionException {
		final List<Molecule> thisRxnLoopProducts = new ArrayList<Molecule>();
		int numPriorRxnProds = rxnProducts.size();
		// If there are prior rxn products, replace each member of the original
		// array of substrates in turn with one of the rxn products.
		for (int rxnProdIdx = -1; rxnProdIdx < numPriorRxnProds; rxnProdIdx++) {
			// start at -1 because need to do at least once
			for (int initSub = 0; initSub < initArray.length; initSub++) {
				Molecule[] substArray = initArray.clone();
				if (numPriorRxnProds > 0) {
					if (rxnProdIdx == -1) rxnProdIdx = 0;
					substArray[initSub] = rxnProducts.get(rxnProdIdx);
				} else { // don't loop, just submit original once
					initSub = initArray.length - 1;
				} // if there are already rxn products
				// If needed functional groups are absent from this substrate
				// array, skip to next.
				if (!Utils.isEmpty(caller.fnalGrpsStr)
						&& substratesLackFnalGroup(substArray))
					continue;
				caller.debugPrint("After substituting reaction product ",
						rxnProdIdx + 1, ", substArray is ", substArray, ";");
				for (final Molecule subst : substArray) {
					final boolean flagged = 
							StereoFunctions.isFlaggedRacemic(subst);
					if (flagged) caller.debugPrint("\t", subst, 
								" is flagged as racemic.");
				} // for each molecule in the substrate array
				// if a starting material can be dimerized, e.g. in crossed
				// aldol reaction, duplicate the substrates before resizing the
				// arrays so that we can get A + A and B + B as well as A + B
				if (allowDimerization) {
					final Molecule[] substArray2 = 
							new Molecule[2 * substArray.length];
					for (int arrayNum = 0;
							arrayNum < substArray.length; arrayNum++) {
						substArray2[arrayNum] = substArray[arrayNum];
						substArray2[arrayNum + substArray.length] =
								substArray[arrayNum];
					} // for each element of substArray
					// substArray = new Molecule[substArray2.length];
					substArray = substArray2;
				} // if dimerization is allowed
				// Make sure that the number of submitted substrates is the
				// number needed; if not, duplicate or remove substrates.  We
				// don't know which ones to duplicate or remove, so generate
				// all possible combinations.  Note: This step is not redundant
				// to wrongNumSubstrates.  For example, for rxn of a Grignard
				// with an ester, only two substrates should be submitted (ester
				// plus a single Grignard, not two), but the Grignard needs to
				// be duplicated for Reactor.
				final List<Molecule[]> resizedArrays = 
						getResizedArrays(substArray);
				int eachResizedArray = 0;
				for (final Molecule[] resizedArray : resizedArrays) {
					eachResizedArray++;
					/* had to comment out next few lines so that degenerately
					 * produced SM would be removed from reaction products
					 try {
						if ((clauseHasProds || loopCt > 1)
								&& SynthSet.subset(resizedArray, initArray)) {
							caller.debugPrint("Substrate array ", eachResizedArray,
									" is merely a subset of the originally "
									+ "submitted substrates; continuing to "
									+ "next array.");
							continue;
						}
					} catch (SearchException e) {
						Utils.alwaysPrint("SingleRxnSolver.getProducts: caught "
								+ "SearchException while looking for subsets.");
					} // try
					/**/ 
					caller.debugPrint("Submitting substrate array ",
							eachResizedArray, " to Reactor: ", resizedArray);
					submitResizedArray(resizedArray, thisRxnLoopProducts, 
							numPriorRxnProds, rxnProdIdx, substArray.length);
					if (generatedSMStereo == INIT_ARRAY) return thisRxnLoopProducts;
					else if (generatedSMStereo == RXN_PRODS) {
						caller.debugPrint("A stereoisomer of a previous reaction "
								+ "product that may have been used as a starting "
								+ "material was produced in the reaction; "
								+ "start over with substituting reaction "
								+ "products into the array of reactants; "
								+ "new rxnProducts is ", rxnProducts);
						if (restartedAlready > NUM_RESTARTS) {
							caller.debugPrint("Sorry, can't restart more than ", 
									NUM_RESTARTS, " times.");
						} else {
							rxnProdIdx = -1;
							numPriorRxnProds = rxnProducts.size();
							generatedSMStereo = NO_SM_RACEMIZED;
							restartedAlready++;
							break;
						} // if haven't already restarted too many times
					} // if a starting material was racemized
				} // for each member of resizedArrays
				if (generatedSMStereo != NO_SM_RACEMIZED) break;
			} // for each element initSub of the initial substrates
		} // for each element rxnProdIdx of the prior rxn products
		return thisRxnLoopProducts;
	} // doOneRxnLoop(int)
	
	/** Submits permutations of an array of substrates of the right size to the
	 * reactor, and adds the products to the growing list.
	 * @param	resizedArray	an array of substrates of the right size
	 * @param	thisRxnLoopProducts	growing list of reaction products
	 * @param	numPriorRxnProds	number of reaction products before entering
	 * this reaction loop
	 * @param	rxnProdIdx	index of one of the prior reaction products that may
	 * have been substituted into the array of substrates now being submitted to
	 * the reactor
	 * @param	lenSubstArray	the original number of substrates before
	 * resizing the array
	 * @throws	ReactionException	if the reaction products can't be calculated
	 */
	private void submitResizedArray(Molecule[] resizedArray,
			List<Molecule> thisRxnLoopProducts, int numPriorRxnProds,
			int rxnProdIdx, int lenSubstArray) throws ReactionException {
		final String SELF = "SingleRxnSolver.submitResizedArray: ";
		// Substrates need to be submitted to Reactor in the order
		// specified in the reaction definition, so get all
		// permutations of each resized array of substrates.
		final List<Molecule[]> allPerms =
				SynthUtils.getPermutations(resizedArray);
		final int numPerms = allPerms.size();
		caller.debugPrint("Generated ", numPerms, " permutations.");
		// Submit each permutation to Reactor.
		Molecule[] reactorProds = null;
		boolean gotProducts = false;
		int perm = 0;
		for (final Molecule[] starters : allPerms) {
			perm++;
			caller.debugPrint("Permutation ", perm, " of ", numPerms, 
					": ", starters);
			reactor.setReactants(starters);
			boolean anySMsAreChiralNonracemic = false;
			try {
				anySMsAreChiralNonracemic = 
						StereoFunctions.anyAreChiralNonracemic(starters);
			} catch (MolCompareException e) {
				Utils.alwaysPrint(SELF + "caught MolCompareException "
						+ "trying to get whether any of ",
						starters, " are chiral.");
			} // try
			// Reactor will give further products with each call
			for (int reactCt = 0; reactCt < MAX_REACT; reactCt++) {
				reactorProds = reactor.react();
				if (Utils.isEmpty(reactorProds)) {
					caller.debugPrint("Product set ", reactCt + 1,
							" from Reactor for permutation ", perm, 
							" is empty; breaking.");
					break;
				} // if there are no products
				gotProducts = true;
				caller.debugPrint("Product set ", reactCt + 1, 
						" obtained from Reactor for permutation ", perm, ".");
				final int numReactorProds = reactorProds.length;
				convert0DTo2D(reactorProds);
				if (isDielsAlder) {
					final String rxnName = 
							ChemUtils.getProperty(moleculeOfRxn, RXN_NAME);
					final DielsAlderStereo stereofixer =
							new DielsAlderStereo(reactorProds[0], 
								starters, rxnName);
					stereofixer.setDielsAlderStereo();
				} // if the reaction is a Diels-Alder reaction
				if (numPriorRxnProds > 0) {
					final Molecule reacted = rxnProducts.get(rxnProdIdx);
					final int diff = desiredNumSubstrates - lenSubstArray;
					try {
						final int reactedAmongSMs = (diff >= 0 ? 0
								: SynthSet.molInArray(reacted, resizedArray));
						if (diff >= 0 // reacted must be among starters
								|| found(reactedAmongSMs)) {
							final int reactedAmongProds =
									SynthSet.molInArray(reacted, reactorProds);
							if (!found(reactedAmongProds)) {
								caller.debugPrint("Prior reaction product ",
										reacted, " reacted further to give ", 
										numReactorProds, " product(s): ",
										reactorProds);
								reacted.setProperty(REACTED, TRUE);
							} else {
								caller.debugPrint("Prior reaction product ",
										reacted, " reacted degenerately.");
							} // if the reaction is degenerate
						} else caller.debugPrint("Initial substrates reacted "
								+ "to give ", numReactorProds, " product(s): ",
								reactorProds);
					} catch (SearchException e) {
						caller.debugPrint("SearchException caught while "
								+ "checking for degeneracy of reaction.");
						reacted.setProperty(REACTED, TRUE);
					} // try
				} else caller.debugPrint("Initial substrates reacted to give ", 
						numReactorProds, " product(s): ", reactorProds);
				implicitizeHAtoms(reactorProds);
				caller.debugPrint("After implicitizing H atoms, ",
						"Reactor products are: ", reactorProds);
				// add info on relative yields, whether product
				// should be racemized
				int prodNum = 0;
				for (final Molecule reactorProd : reactorProds) {
					prodNum++;
					caller.debugPrint("Setting reactor index property "
							+ "for reactor product ", prodNum, ", ",
							reactorProd, ", to ", reactCt + 1);
					reactorProd.setProperty(REACTOR_INDEX,
							Integer.toString(reactCt
									+ caller.resubjReactorIndex + 1));
					if (majorOrMinor != null) {
						caller.debugPrint("Setting major/minor property "
								+ "for reactor product ", prodNum, ", ",
								reactorProd, ", to ", majorOrMinor);
						reactorProd.setProperty(MAJ_MIN_PROD, majorOrMinor);
						if (majorOrMinorExplan != null)
							reactorProd.setProperty(MAJ_MIN_EXPLAN, 
									majorOrMinorExplan);
					} // if rxn def has major/minor info
					try {
						// for diastereospecific reactions with achiral
						// or racemic compounds, need to denote that 
						// diastereopure product should be racemic
						if (!anySMsAreChiralNonracemic
								&& StereoFunctions.isChiral(reactorProd)) {
							caller.debugPrint("Setting racemize property "
									+ "for reactor product ", prodNum, 
									", ", reactorProd, ", to true.");
							reactorProd.setProperty(RACEMIZE, TRUE);
						} // if chiral product produced from achiral SM
					} catch (MolCompareException e) {
						Utils.alwaysPrint(SELF + "got MolCompareException "
								+ "when trying to see if ", reactorProd,
								" is chiral; doing nothing.");
					} // try
				} // for each reactor prod
				// Add each product to this loop's product list unless 
				// it's already in the list or is a starting material.
				addToRxnLoopProducts(reactorProds, thisRxnLoopProducts);
				if (generatedSMStereo != NO_SM_RACEMIZED) return;
			} // while still getting products from reactor
			if (gotProducts && !keepPermuting) {
				// if keepPermuting, get B + A as well as A + B
				break; // no need to try more permutations
			}
		} // for each permutation of the substrates
		if (!gotProducts)
			caller.debugPrint("No products from this array.");
	} // submitResizedArray(Molecule[], List<Molecule>, int, int, int)

/* ***************** Functional group methods *******************/

	/** Determines whether the functional groups required by this reaction are
	 * present. Any set of reaction conditions could involve many different kinds of
	 * functional groups. Before calling Reactor, see if the reactants contain
	 * the functional groups required for this particular reaction definition
	 * -- should save time.
	 * @param	substList	list of substrates for the reaction
	 * @return	true if substrates contain all the functional groups
	 */
	private boolean substratesLackFnalGroup(List<Molecule> substList) {
		return substratesLackFnalGroup(Utils.molListToArray(substList));
	} // substratesLackFnalGroup(List<Molecule>)

	/** Determines whether the functional groups required by this reaction are
	 * present. Any set of reaction conditions could involve many different kinds of
	 * functional groups. Before calling Reactor, see if the reactants contain
	 * the functional groups required for this particular reaction definition
	 * -- should save time.
	 * @param	substArray	array of substrates for the reaction
	 * @return	true if substrates contain all the functional groups
	 */
	private boolean substratesLackFnalGroup(Molecule[] substArray) {
		final String SELF = "substratesLackFnalGroup: ";
		final String[] fnalGrps = caller.fnalGrpsStr.split(";");
		for (final String fnalGrp : fnalGrps) {
			final String[] grpNameNum = getGroupNameNumber(fnalGrp);
			if (grpNameNum == null) continue;
			final int[] containGrp = 
					substratesWithFnalGroup(substArray, grpNameNum);
			if (Utils.isEmpty(containGrp)) {
				caller.debugPrint(SELF, substArray, " lacks a ", grpNameNum[0]);
				return true;
			} // if the group wasn't found
		} // for each required functional group
		final int grpsLen = fnalGrps.length;
		caller.debugPrint(SELF, substArray, " contains ", 
				(grpsLen > 1 ? "all " : ""), "required fnal group", 
				(grpsLen > 1 ? "s " : " "), caller.fnalGrpsStr);
		return false;
	} // substratesLackFnalGroup(Molecule[])

	/** Gets a functional group's name.
	 * @param	grpLoc	ID number or name of the functional group
	 * @return	array of Strings: first is the name, second is the ID number
	 */
	private String[] getGroupNameNumber(String grpLoc) {
		final String SELF = "SingleRxnSolver.getGroupNameNumber: ";
		String[] grpParts = null;
		if (grpLoc != null) try {
			final String grpLocator = grpLoc.trim();
			try {
				// see if grpLocator is a number: don't use MathUtils.parseInt()!
				final int grpId = Integer.parseInt(grpLocator);
				final FnalGroupDef group = FnalGroupDef.getFnalGroupDef(grpId);
				if (group != null) {
					grpParts = new String[] {group.name, grpLocator};
				} else Utils.alwaysPrint(SELF 
						+ "no functional group found for ", grpLoc);
			} catch (NumberFormatException e) {
				// grpLocator is a name, not a number
				final FnalGroupDef group =
						FnalGroupDef.getFnalGroupDef(grpLocator);
				if (group != null) {
					grpParts = new String[]
						{group.name, String.valueOf(group.groupId)};
				} else Utils.alwaysPrint(SELF 
						+ "no functional group found for ", grpLoc);
			}
		} catch (DBException e) {
			Utils.alwaysPrint(SELF + "no functional group found for ", grpLoc);
		} catch (ParameterException e) {
			Utils.alwaysPrint(SELF + "no functional group found for ", grpLoc);
		} // try
		return grpParts;
	} // getGroupNameNumber(String)
	
	/** Determines whether the functional groups required by this reaction are
	 * present. Any set of reaction conditions could involve many different kinds of
	 * functional groups. Before calling Reactor, see if the reactants contain
	 * the functional groups required for this particular reaction definition
	 * -- should save time.
	 * @param	substArray	array of substrates for the reaction
	 * @param	grpNameNum	name and group number of functional group
	 * @return	indices of substrates containing the functional group
	 */
	private int[] substratesWithFnalGroup(Molecule[] substArray, 
			String[] grpNameNum) {
		final String SELF = "substratesWithFnalGroup: ";
		final List<Integer> containGrp =  new ArrayList<Integer>();
		if (grpNameNum != null) {
			caller.debugPrint(SELF + "looking for group ", grpNameNum[NAME]);
			try {
				final FnalGroup fgrp = new FnalGroup();
				fgrp.setGroup(Integer.parseInt(grpNameNum[NUMBER]));
				fgrp.setGroupOper(FnalGroup.GREATER);
				fgrp.setNumGroups(0);
				for (int substNum = 0; substNum < substArray.length; substNum++) {
					final Molecule substrate = substArray[substNum];
					final Response resp = new Response(substrate);
					final OneEvalResult evalResult =
							fgrp.isResponseMatching(resp, NO_AUTHSTRING);
					if (evalResult.isSatisfied) {
						caller.debugPrint("   " + SELF, substrate,
								" has a ", grpNameNum[NAME]);
						containGrp.add(Integer.valueOf(substNum));
					} else if (evalResult.verificationFailureString != null) {
						caller.debugPrint("   " + SELF, substrate,
								" may or may not have a ", grpNameNum[NAME],
								"; take no chances and assume it does.");
						containGrp.add(Integer.valueOf(substNum));
					} else caller.debugPrint("   " + SELF, substrate,
							" lacks a ", grpNameNum[0]);
				} // for each substrate array member
			} catch (Exception e) {
				e.printStackTrace();
			} // try
			if (containGrp.isEmpty()) {
				caller.debugPrint(SELF, substArray, " lacks a ", grpNameNum[NAME]);
			} else {
				caller.debugPrint(SELF, containGrp.size(), " member(s) of ", 
						substArray, " contain a ", grpNameNum[NAME]);
			} // if the group wasn't found
		} else caller.debugPrint(SELF, "no functional group to look for");
		return Utils.listToIntArray(containGrp);
	} // substratesWithFnalGroup(Molecule[], String[])

/* ***************** Substrate enumeration methods *******************/

	/** If number of submitted substrates not the number needed,
	 * duplicate or remove substrates in all combinations, making new arrays.
	 * @param	substArray	array of substrates
	 * @return	list of molecule arrays, each with the right number of
	 * substrates, formed by removing or duplicating members of substArray
	 */
	private List<Molecule[]> getResizedArrays(Molecule[] substArray) {
		final int numSubstrates = substArray.length;
		int[] startArray = new int[numSubstrates];
		// each integer represents a compound in substArray
		for (int starterNum = 0; starterNum < numSubstrates; starterNum++)
			startArray[starterNum] = starterNum;
		final List<int[]> resizedIntArrays = new ArrayList<int[]>();
		resizedIntArrays.add(startArray);
		final int diff = desiredNumSubstrates - numSubstrates;
		final boolean ADD = true;
		if (diff > 0) {
			// see if the substrate to duplicate can be ID'd by functional group
			final String fnalGrpToClone = 
					ChemUtils.getProperty(moleculeOfRxn, CLONE_FNAL);
			final String stoichOfClone = 
					ChemUtils.getProperty(moleculeOfRxn, CLONE_STOICH);
			boolean resizeByFnalGroup = fnalGrpToClone != null 
					&& stoichOfClone != null;
			if (resizeByFnalGroup) {
				final String[] grpNameNum = getGroupNameNumber(fnalGrpToClone);
				final int[] substsToClone = 
						substratesWithFnalGroup(substArray, grpNameNum);
				final int stoich = MathUtils.parseInt(stoichOfClone);
				resizeByFnalGroup = (grpNameNum != null // good fnal group
						&& substsToClone.length == 1 // just one substrate with group
						&& substsToClone[0] >= 0 // good array number
						&& substsToClone[0] < numSubstrates // good array number
						&& stoich > 0); // good stoichiometry
				if (resizeByFnalGroup) {
					final int newNumSubstrates = numSubstrates + stoich - 1;
					final Molecule[] substrates = new Molecule[newNumSubstrates];
					System.arraycopy(substArray, 0, substrates, 0, numSubstrates);
					/*
					for (int subNum = 0; subNum < numSubstrates; subNum++) {
						substrates[subNum] = substArray[subNum];
					} // for each existing array member
					*/
					for (int subNum = numSubstrates; 
							subNum < newNumSubstrates; subNum++) {
						substrates[subNum] = substArray[substsToClone[0]];
					} // for each additional copy of one of the substrates
					caller.debugPrint("Resized substrate array ", substArray, 
							" by duplicating substrate ", substsToClone[0] + 1,
							" with functional group ", fnalGrpToClone,
							" to stoichiometry ", stoich,
							" to give ", substrates);
					final List<Molecule[]> resizedArrays = 
							new ArrayList<Molecule[]>();
					resizedArrays.add(substrates);
					return resizedArrays;
				} // if should go ahead and resize the array
			} // if clone a substrate with a functional group 
			// if the substrate to duplicate can't be ID'd by functional group,
			// generate all possible arrays
			else { // !resizeByFnalGroup
				caller.debugPrint("The number of substrates (", 
						numSubstrates, ") in this reaction loop is ", 
						diff, " too few for the reaction definition (",
						desiredNumSubstrates,
						"); calling changeMultiSubstrates.");
				changeMultiSubstrateNums(resizedIntArrays, ADD);
			} // if there are instructions about which substrate to clone
		} else if (diff < 0) { // ignore Jlint advice: diff != 0.  Raphael 11/2010
			caller.debugPrint("The number of substrates (", 
					numSubstrates, ") in this reaction loop is ", 
					-diff, " too many for the reaction definition (",
					desiredNumSubstrates,
					"); calling changeMultiSubstrates.");
			changeMultiSubstrateNums(resizedIntArrays, !ADD);
		} else { // no resizing to do; return original submission
			final List<Molecule[]> resizedArrays = new ArrayList<Molecule[]>();
			resizedArrays.add(substArray);
			return resizedArrays;
		} // if diff
		int numResizedArrays = resizedIntArrays.size();
		final int arrayLength = resizedIntArrays.get(0).length;
		// remove duplicates
		int arrayNum = 0;
		while (arrayNum < numResizedArrays) {
			final int[] anIntArray = resizedIntArrays.get(arrayNum);
			for (int compNum = numResizedArrays - 1; compNum > arrayNum; compNum--)
				if (Arrays.equals(anIntArray, resizedIntArrays.get(compNum)))
					resizedIntArrays.remove(compNum);
			numResizedArrays = resizedIntArrays.size();
			arrayNum++;
		} // while there are arrays to compare
		if (numResizedArrays != 1) {
			caller.debugPrint("Resized substrates array has ",
					numResizedArrays, " members, each with ",
					arrayLength, " substrate(s).");
		} // if arrays had to be resized
		// create the Molecule arrays from the integer arrays
		final List<Molecule[]> resizedArrays = new ArrayList<Molecule[]>();
		for (final int[] anIntArray : resizedIntArrays) {
			Molecule[] anArray = new Molecule[arrayLength];
			for (int itemNum = 0; itemNum < arrayLength; itemNum++) {
				anArray[itemNum] = substArray[anIntArray[itemNum]].clone();
				if (diff > 0)
					anArray[itemNum].setProperty(RESIZE_NUM,
							String.valueOf(anIntArray[itemNum]));
			} // for each item in the resized array
			resizedArrays.add(anArray);
		} // for each resized integer array
		return resizedArrays;
	} // getResizedArrays(Molecule[])

	/** Change the number of substrates in a list to match the number needed 
	 * for the reaction.
	 * @param	substrateNums	list of int arrays, each member of which is
	 * a number corresponding to each substrate
	 * @param	add	whether to add or remove substrates
	 */
	private void changeMultiSubstrateNums(List<int[]> substrateNums, 
			boolean add) {
		final int currentLength = substrateNums.get(0).length;
		if ((add && currentLength < desiredNumSubstrates) 
				|| (!add && currentLength > desiredNumSubstrates)) {
			final List<int[]> allNewSubstrateNumsArrays =
					new ArrayList<int[]>();
			for (final int[] substrateNum : substrateNums) {
				allNewSubstrateNumsArrays.addAll(
						changeOneSubstrateNum(substrateNum, add));
			} // for each substrate starterNum
			substrateNums.clear();
			substrateNums.addAll(allNewSubstrateNumsArrays);
			changeMultiSubstrateNums(substrateNums, add);
		} // if we still have too few substrateNums
	} // changeMultiSubstrateNums(List<int[]>, boolean)

	/** Change the number of substrates in a list to approach the number needed 
	 * for the reaction.
	 * @param	substrateNums	list of int arrays, each member of which is
	 * a number corresponding to each substrate
	 * @param	add	whether to add or remove substrates
	 * @return	list of int arrays, each member of which is
	 * a number corresponding to each substrate
	 */
	private List<int[]> changeOneSubstrateNum(int[] substrateNums, 
			boolean add) {
		final int numSubstrateNums = substrateNums.length;
		final List<int[]> allNewSubstrateNumsArrays = 
				new ArrayList<int[]>();
		for (int starter1Num = 0;
				starter1Num < numSubstrateNums; starter1Num++) {
			final int[] newSubstrateNums = 
					new int[numSubstrateNums + (add ? 1 : -1)];
			final int end = numSubstrateNums + (add ? 1 : 0);
			for (int starter2Num = 0; starter2Num < end; starter2Num++) {
				if (add) {
					newSubstrateNums[starter2Num] =
							(starter2Num < numSubstrateNums
								? substrateNums[starter2Num]
								: substrateNums[starter1Num]);
				} else if (starter2Num < starter1Num) {
					newSubstrateNums[starter2Num] =
							substrateNums[starter2Num];
				} else if (starter2Num > starter1Num) {
					newSubstrateNums[starter2Num - 1] =
							substrateNums[starter2Num];
				} // if add or remove
			} // for each substrate starter2Num
			allNewSubstrateNumsArrays.add(newSubstrateNums);
		} // for each substrate starter1Num
		return allNewSubstrateNumsArrays;
	} // changeOneSubstrateNum(int[], boolean)

/* ***************** Reaction products clean-up methods *******************/

	/** Implicitizes H atoms of calculated products except those required to
	 * indicate stereochemistry.
	 * @param	calcdProds	array of calculated products
	 */
	private void implicitizeHAtoms(Molecule[] calcdProds) {
		for (final Molecule prod : calcdProds) {
			ChemUtils.implicitizeH(prod, MolAtom.ALL_H & ~MolAtom.WEDGED_H);
		} // for each product
	} // implicitizeHAtoms(Molecule[])

	/** Gives 2D coordinates to zero-dimensional Reactor products.
	 * @param	cpds	array of compounds
	 */
	private void convert0DTo2D(Molecule... cpds) {
		final String SELF = "SingleRxnSolver.convert0DTo2D: ";
		for (final Molecule cpd : cpds) StereoFunctions.convert0DTo2D(cpd);
	} // convert0DTo2D(Molecule...)

	/** Add products from Reactor to the list of products of this reaction loop,
	 * unless the products are already among prior reaction products (prevents
	 * redundancy) or among the initial substrates (prevents degenerate
	 * products from being listed).
	 * @param	reactorProds	array of reaction products from one reactor call
	 * @param	thisRxnLoopProducts	list of reaction products of this loop
	 */
	private void addToRxnLoopProducts(Molecule[] reactorProds,
			List<Molecule> thisRxnLoopProducts) {
		for (final Molecule reactorProd : reactorProds) {
			try {
				if (reactorProd != null
						&& reactorProd.getAtomCount() != 0) {
					if (found(SynthSet.molInArray(reactorProd, initArray))) {
						caller.debugPrint("Product ", reactorProd, " already "
								+ " in initial substrate list, ", initArray, 
								"; not adding to thisRxnLoopProducts.");
					} else if (found(SynthSet.molInArray(reactorProd,
							rxnProducts))) {
						caller.debugPrint("Product ", reactorProd, " already "
								+ "in previous products list, ", rxnProducts, 
								"; not adding to thisRxnLoopProducts.");
					} else if (found(SynthSet.molInArray(reactorProd,
							thisRxnLoopProducts))) {
						caller.debugPrint("Product ", reactorProd, " already "
								+ " in list of products from this loop, ",
								thisRxnLoopProducts, 
								"; not adding to thisRxnLoopProducts.");
					} else {
						caller.debugPrint("Product ", reactorProd, " not already "
								+ "in initial substrate or product lists, ", 
								initArray, ", ", rxnProducts, ", ", 
								thisRxnLoopProducts, "; adding it to this list.");
						thisRxnLoopProducts.add(reactorProd);
						if (isAsymmetric) caller.didAsymmetricRxn = true;
						// if the stereoisomer of a SM was formed, start the
						// Reactor calculations over again
						final Molecule prodEnant = 
								ChemUtils.getMirror(reactorProd);
						final int prodEnantInInitArray = 
								SynthSet.molInArray(prodEnant, initArray);
						if (found(prodEnantInInitArray)
								&& !StereoFunctions.isFlaggedRacemic(
									initArray[prodEnantInInitArray])) {
							caller.debugPrint("The enantiomer of product ",
									reactorProd, " was already one of the "
									+ "initial starting materials; flagging "
									+ "the SM as racemic and starting over.");
							initArray[prodEnantInInitArray].setProperty(
									RACEMIZE, TRUE);
							generatedSMStereo = INIT_ARRAY;
							return;
						} // if the reaction racemized an initial SM
						final int prodEnantInRxnProducts =
								SynthSet.molInArray(prodEnant, rxnProducts);
						if (found(prodEnantInRxnProducts)
								&& !StereoFunctions.isFlaggedRacemic(
									rxnProducts.get(prodEnantInRxnProducts))) {
							caller.debugPrint("The enantiomer of product ",
									reactorProd, " was a starting "
									+ "material in this reaction mixture; "
									+ "flagging the SM as racemic and "
									+ "starting over with substituting "
									+ "reaction products into initArray.");
							rxnProducts.get(prodEnantInRxnProducts)
									.setProperty(RACEMIZE, TRUE);
							generatedSMStereo = RXN_PRODS;
							return;
						} // if the reaction racemized a rxn prod used as a SM
						final int prodDiastInInitArray = 
								SynthSet.molInArray(reactorProd, initArray,
									TOLERATE_DIASTEREO_ONLY);
						if (found(prodDiastInInitArray)) {
							caller.debugPrint("A diastereomer of product ",
									reactorProd, " was already one of the "
									+ "initial starting materials; "
									+ "adding it to initial starting materials "
									+ "and starting over.");
							final int initArrayLen = initArray.length;
							final Molecule[] newInitArray = 
									new Molecule[initArrayLen + 1];
							System.arraycopy(initArray, 0, newInitArray, 0,
									initArrayLen);
							newInitArray[initArrayLen] = reactorProd;
							initArray = newInitArray;
							generatedSMStereo = INIT_ARRAY;
							return;
						} // if the reaction racemized an initial SM
						final int prodDiastInRxnProducts =
								SynthSet.molInArray(reactorProd, rxnProducts, 
									TOLERATE_DIASTEREO_ONLY);
						if (found(prodDiastInRxnProducts)) {
							caller.debugPrint("A diastereomer of product ",
									reactorProd, " was a starting "
									+ "material in this reaction mixture; "
									+ "starting over with substituting "
									+ "reaction products into initArray.");
							rxnProducts.add(reactorProd);
							generatedSMStereo = RXN_PRODS;
						} // if the reaction racemized a rxn prod used as a SM
					} // if should add to list of reaction products
				} // if reactorProd is a real molecule
			} catch (SearchException e) {
				Utils.alwaysPrint("SingleRxnSolver.addToRxnLoopProducts: "
						+ "caught SearchException:\n", e.getMessage());
			} // try
		} // for each product
	} // addToRxnLoopProducts(Molecule[], List<Molecule>)

	/** Remove reacted rxn products from rxnProducts, add products from this
	 * loop.
	 * @param	rxnProducts	list of all reaction products thus far
	 * @param	thisRxnLoopProducts	list of reaction products of this loop
	 */
	private void updateRxnProducts(List<Molecule> rxnProducts,
			List<Molecule> thisRxnLoopProducts) {
		caller.debugPrint("updateRxnProducts: rxnProducts is at first: ",
				rxnProducts);
		final int size = rxnProducts.size();
		for (int rxnProdIdx = size - 1; rxnProdIdx >= 0; rxnProdIdx--) {
			final Molecule rxnProd = rxnProducts.get(rxnProdIdx);
			final String reactedProp = ChemUtils.getProperty(rxnProd, REACTED);
			if (TRUE.equals(reactedProp)) {
				rxnProducts.remove(rxnProdIdx);
				caller.debugPrint("Removing reacted rxnProduct ", 
						rxnProdIdx + 1, ", ", rxnProd, ", from rxnProducts.");
			} else caller.debugPrint("Not removing unreacted rxnProduct ", 
					rxnProdIdx + 1, ", ", rxnProd, ", from rxnProducts.");
		} // for each rxnProd
		final int numLoopProds = thisRxnLoopProducts.size();
		caller.debugPrint("Adding ", numLoopProds,
				(numLoopProds == 1 ? " product" : " products"),
				" to list of all products of this reaction.");
		rxnProducts.addAll(thisRxnLoopProducts);
		caller.debugPrint("updateRxnProducts: rxnProducts is now: ", 
				rxnProducts);
	} // updateRxnProducts(List<Molecule>, List<Molecule>)
	
	/** Gets if a molecule has been found in an array or list.
	 * @param	index	index of the molecule in the array or list
	 * @return	true if the index &ge; 0
	 */
	private boolean found(int index) {
		return index != SynthSet.NOT_FOUND;
	} // found(int)

} // SingleRxnSolver
