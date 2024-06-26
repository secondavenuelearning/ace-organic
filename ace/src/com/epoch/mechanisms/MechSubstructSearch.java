package com.epoch.mechanisms;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.sss.search.MolSearch;
import chemaxon.sss.search.MolSearchOptions;
import chemaxon.sss.SearchConstants;
import chemaxon.sss.search.SearchException;
import chemaxon.struc.Molecule;
import chemaxon.struc.MDocument;
import chemaxon.struc.MObject;
import chemaxon.struc.MolAtom;
import chemaxon.struc.graphics.MEFlow;
import com.epoch.chem.Normalize;
import com.epoch.chem.ChemUtils;
import com.epoch.mechanisms.mechConstants.MechConstants;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;

/** Searches for a structure and associated electron-flow arrows in a stage. */
public class MechSubstructSearch implements MechConstants, SearchConstants {
	
	/** Whether to print certain debugging output. */
	private static final boolean printFlowDetails = false; 
	
	private static void debugPrint(Object... msg) { 
		// Utils.printToLog(msg);
	}

	private static void debugPrintMRV(Object... msg) {
		// Utils.printToLog(msg, MRV);
	}

	//------------------------------------------------------------------------
	//						  	members
	//------------------------------------------------------------------------
	/** The response mechanism. */
	transient Mechanism respMech 		= null;
	/** The response mechanism data. */
	transient MechParser respMechData 	= null;
	/** MRV representation of the structure for which to search. Set locally. */
	transient String authXml		 	= null; 
	/** Molecule for which to search.  Set locally.  */
	transient Molecule authMolecule 	= null; 
	/** Molecule containing all molecules in the current stage of the student's
	 * response mechanism.  Repeatedly reset locally. */
	transient Molecule stageMolecule	= null; 
	/** Result of the search for the author's structure in the stage.  Each
	 * member of searchResult corresponds to an atom in the author's structure;
	 * the value of searchResult[member] corresponds to an atom in the stage. */
	transient int[]  searchResult 	= null;
	/** Array of electron-flow arrows in the current stage of the student's
	 * response mechanism.  Repeatedly reset locally. */
	transient MechFlow[] respFlows	= null;
	/** Array of electron-flow arrows for which to search.  Set locally.  */
	transient MechFlow[] authFlows	= null;
	/** Describes the type, as in, "two electrons, atom to incipient bond,"
	 * of each electron-flow arrow in the current stage.  Parallel 
	 * to respFlows[].  */
	transient int[] respFlowCodes		= null; 
	/** Describes the type, as in, "two electrons, atom to incipient bond,"
	 * of each electron-flow arrow in the author's structure.  Parallel 
	 * to authFlows[].  */
	transient int[] authFlowCodes		= null; 
	
	//------------------------------------------------------------------------
	//						  	constructors
	//------------------------------------------------------------------------
	/** Constructor. */
	public MechSubstructSearch() {
		// intentionally empty
	}

	/** Constructor. 
	 * @param	respMech	the parsed response mechanism
	 */
	public MechSubstructSearch(Mechanism respMech) {
		this.respMech = respMech;
		respMechData = respMech.parsedMech;
	}
	
	/** Constructor. 
	 * @param	respMech	the parsed response mechanism
	 * @param	authXml	the XML describing the substructure and
	 * electron-flow arrows being sought
	 */
	public MechSubstructSearch(Mechanism respMech, String authXml) {
		this.respMech = respMech;
		respMechData = respMech.parsedMech;
		this.authXml = authXml;
	}
	
	//------------------------------------------------------------------------
	//						  hasSubstructure
	//------------------------------------------------------------------------
	/** Gets whether the mechanism contains the substructure and electron flow
	 * arrows.
	 * @return	array of ints containing the atom-to-atom match, plus one more
	 * member containing the index of the stage in which the match was found
	 * @throws	MolFormatException	if a molecule can't be imported
	 */
	public int[] hasSubstructure() throws MolFormatException {
		return hasSubstructure(ALL_MASK);
	} // hasSubstructure()
	
	/** Gets whether the mechanism contains the substructure and electron flow
	 * arrows.
	 * @param	ignoreFlags	flags indicating what features (charge, radical
	 * state, isotope state) in the student's structure to ignore
	 * @return	array of ints containing the atom-to-atom match, plus one more
	 * member containing the index of the stage in which the match was found
	 * @throws	MolFormatException	if a molecule can't be imported
	 */
	public int[] hasSubstructure(int ignoreFlags) throws MolFormatException {
		final String SELF = "MechSubstructSearch.hasSubstructure: ";
		authMolecule = getSearchMolecule(authXml, false); // don't hydrogenize it
		try {
			debugPrintMRV(SELF + "after processing, "
					+ "author molecule is:\n", authMolecule);	
		} catch (Exception e) {
			debugPrint(SELF + "Sorry, can't log processed "
					+ "author molecules, getting exceptions");
		}
		for (int stgIndex = 0; stgIndex < respMechData.getNumStages(); stgIndex++) {
			debugPrint("Checking stage ", stgIndex + 1, " for substructure..."); 
			final MechStage stage = respMechData.getStage(stgIndex);
			final int[] matches = hasSubstructure(stage, ignoreFlags);
			if (!Utils.isEmpty(matches)) { 
				int[] matchAndStageIndex = new int[matches.length + 1];
				for (int matchNum = 0; matchNum < matches.length; matchNum++)
					matchAndStageIndex[matchNum] = matches[matchNum];
				matchAndStageIndex[matches.length] = stgIndex;
				return matchAndStageIndex;
			} // if there's a match
		} // for each stage
		return new int[0]; // should never happen
	} // hasSubstructure(int)
	
	/** Gets whether the stage contains the substructure and electron flow
	 * arrows.
	 * @param	stage	the stage to search
	 * @param	ignoreFlags	flags indicating what features (charge, radical
	 * state, isotope state) in the student's structure to ignore
	 * @return	array of ints containing the atom-to-atom match
	 * @throws	MolFormatException	if a molecule can't be imported
	 */
	private int[] hasSubstructure(MechStage stage, int ignoreFlags) 
			throws MolFormatException {
		final String SELF = "MechSubstructSearch.hasSubstructure: ";
		/* // use stage XML to make sure that flows reference atoms properly
		final String stageXML = stage.getStageXML(); 
		stageMolecule = MolImporter.importMol(stageXML); /**/
		// clone stage document to avoid unnecessary import
		final MDocument stageDoc = stage.getStageMDoc().cloneDocument();
		stageMolecule = stageDoc.getPrimaryMolecule();
		if (stageMolecule == null || authMolecule == null) {
			Utils.alwaysPrint(SELF + "either student or author "
					+ "molecule is null; returning empty array.");
			return new int[0]; 
		} // if missing data
		processSearchMolecule(stageMolecule, true); // hydrogenize it
		try {
			debugPrintMRV(SELF + "after processing, "
					+ "student molecule is:\n", stageMolecule);	
		} catch (Exception e) {
			debugPrint(SELF + "Sorry, can't log processed "
					+ "student molecule, getting exceptions");
		}
		debugPrint(SELF + "getting flow arrays");
		respFlows = getFlowsArray(stageDoc);
		authFlows = getFlowsArray(authMolecule.getDocument());
		debugPrint(SELF + "getting flow types: "
				+ "tens digit of flow type is source type, ones digit is "
				+ "sink type; 1 is atom, 2 is bond, 3 is incipient bond; " 
				+ "negative is two electrons, positive is one electron");
		respFlowCodes = getFlowCodes(respFlows, stageMolecule);
		authFlowCodes = getFlowCodes(authFlows, authMolecule);
		final MolSearch searchObj = setupSearchObject(ignoreFlags); 
		int matchCount = 0;
		try {
			while (true) {
				searchResult = searchObj.findNext(); // global variable
				if (searchResult == null) break;
				debugPrint(SELF + "Search result is ", searchResult); 
				matchCount++;
				if (flowsMatch()) {
					debugPrint(SELF + "flows match, return true;"
							+ " structure match count = ", matchCount);
					// return both match indices and number of matching flow
					return searchResult;
				} else if (matchCount % 100 == 0) {
					debugPrint(SELF + "match count = ", matchCount);
				} // if the flows match
			} // while matches found
		} catch (SearchException e) {
			debugPrint(SELF + "JChem Search Exception caught!");
			e.printStackTrace();
			return new int[0]; 
		} catch (ArrayIndexOutOfBoundsException e) {
			debugPrint(SELF + "JChem ArrayIndexOutOfBoundsException caught!");
			e.printStackTrace();
			return new int[0]; 
		} // try 
		debugPrint(SELF + "no more mechanism substructures "
				+ "found. Match count = ", matchCount, "; return empty result"); 
		return new int[0]; 
	} // hasSubstructure(Stage, int)
	
	//------------------------------------------------------------------------
	//						  getSearchMolecule
	//------------------------------------------------------------------------
	/** Extracts a molecule from the MRV representation and normalizes it.
	 * @param	xml	MRV representation of the substructure and electron-flow
	 * arrows
	 * @param	hydrogenizeIt	whether to add explicit H atoms (student
	 * response only)
	 * @return	the Molecule ready for a JChem search
	 * @throws	MolFormatException	if a molecule can't be imported
	 */
	private Molecule getSearchMolecule(String xml, boolean hydrogenizeIt)  
			throws MolFormatException {
		debugPrint("MechSubstructSearch.getSearchMolecule: "
				+ "search molecule:\n", xml);
		final Molecule molecule = MolImporter.importMol(xml);
		molecule.ungroupSgroups(SHORTCUT_GROUPS);
		// molecule.aromatize(); // not when electron-flows are involved!
		processSearchMolecule(molecule, hydrogenizeIt);		
		return molecule;
	} // getSearchMolecule(String, boolean)
	
	//------------------------------------------------------------------------
	//						  processSearchMolecule
	//------------------------------------------------------------------------
	/** Normalizes a molecule in preparation for searching.
	 * @param	molecule	molecule to be prepared for a JChem search
	 * @param	hydrogenizeIt	whether to add explicit H atoms (student
	 * response only)
	 */
	private void processSearchMolecule(Molecule molecule, boolean hydrogenizeIt) {
		if (hydrogenizeIt) { // applied only to student response 
			ChemUtils.explicitizeHnoClone(molecule);
		}
		Normalize.normalizeRadicals(molecule);
	} // processSearchMolecule(Molecule, boolean)
	
	//------------------------------------------------------------------------
	//							getFlowsArray
	//------------------------------------------------------------------------
	/** Gets an array of electron-flow arrows from an MDocument.
	 * @param	mDoc	MDocument that contains the electron-flow arrows
	 * @return	array of electron-flow arrows
	 */
	private MechFlow[] getFlowsArray(MDocument mDoc) {			
		if (mDoc == null) return null;
		final ArrayList<MechFlow> flowList = new ArrayList<MechFlow>();		
		for (int objIndex = 0; objIndex < mDoc.getObjectCount(); objIndex++) {
			final MObject mObject = mDoc.getObject(objIndex);
			if (mObject instanceof MEFlow) try {
				flowList.add(new MechFlow((MEFlow) mObject));
			} catch (MechError e) {
				debugPrint("MDocument has a two-electron arrow "
						+ "connecting an atom to its ligand.");
			} // if try
		} // for each object in the MDocument
		return flowList.toArray(new MechFlow[flowList.size()]);
	} // getFlowsArray(MDocument)

	//------------------------------------------------------------------------
	//							getFlowCodes
	//------------------------------------------------------------------------
	/** Gets the codes, as in, "two electrons, atom to incipient bond,"
	 * of an array of electron-flow arrows.  
	 * @param	flows	an array of electron-flow arrows
	 * @param	molecule	molecule containing the atoms of the electron-flow
	 * arrows
	 * @return	array of ints; each member contains the code of the 
	 * corresponding member of flows
	 */
	private int[] getFlowCodes(MechFlow[] flows, Molecule molecule) {
		final String SELF = "MechSubstructSearch.getFlowCodes: ";
		if (flows == null) return null;
		debugPrint(SELF + "There are ", flows.length, " flows.");
		int[] flowCodes = new int[flows.length];
		int flowIndex = 0;
		for (final MechFlow flow : flows) {
			flowCodes[flowIndex++] = flow.getFlowCode();
			debugPrint(SELF + "flow ", flowIndex, 
					" has code ", flowCodes[flowIndex - 1]);
			if (printFlowDetails) flow.print(molecule);
		} // for each flow
		return flowCodes;
	} // getFlowCodes(MechFlow[], Molecule)

	//------------------------------------------------------------------------
	//						  setupSearchObject
	//------------------------------------------------------------------------	
	/** Sets up the JChem search object. 
	 * @param	ignoreFlags	flags indicating what features (charge, radical
	 * state, isotope state) in the student's structure to ignore
	 * @return	the JChem search object for finding the author's structure 
	 * in the response stage
	 */
	private MolSearch setupSearchObject(int ignoreFlags) {
		final String SELF = "MechSubstructSearch.setupSearchObject: ";
		final MolSearchOptions searchOpts = 
				new MolSearchOptions(SUBSTRUCTURE);
		searchOpts.setOrderSensitiveSearch(true);
		final boolean ignoreChg = (ignoreFlags & CHARGE_MASK) != 0;
		final boolean ignoreIso = (ignoreFlags & ISOTOPES_MASK) != 0;
		final boolean ignoreRad = (ignoreFlags & RADSTATE_MASK) != 0;
		searchOpts.setChargeMatching(ignoreChg 
				? CHARGE_MATCHING_IGNORE : CHARGE_MATCHING_EXACT);
		searchOpts.setIsotopeMatching(ignoreIso 
				? ISOTOPE_MATCHING_IGNORE : ISOTOPE_MATCHING_EXACT);
		searchOpts.setRadicalMatching(ignoreRad 
				? RADICAL_MATCHING_IGNORE : RADICAL_MATCHING_EXACT);
		debugPrint(SELF, ignoreChg ? "ignore" : "exact",
				" charge matching, ", ignoreIso ? "ignore" : "exact",
				" isotope matching, ", ignoreRad ? "ignore" : "exact",
				" radical matching set for search.");
		searchOpts.setValenceMatching(false);
		searchOpts.setVagueBondLevel(VAGUE_BOND_OFF);
		final MolSearch searchObj = new MolSearch();
		searchObj.setSearchOptions(searchOpts);
		// molecule comparison must account for aromatic/Kekule forms
		final Molecule authCopy = authMolecule.clone();
		final Molecule stageCopy = stageMolecule.clone();
		authCopy.aromatize();
		stageCopy.aromatize();
		try {
			debugPrintMRV(SELF + "after readying for search, "
					+ "author molecule (query) is:\n", authCopy);	
			debugPrintMRV(SELF + "after readying for search, "
					+ "student molecule (target) is:\n", stageCopy);	
		} catch (Exception e) {
			debugPrint(SELF + "Sorry, can't log processed "
					+ "student molecule, getting exceptions");
		}
		searchObj.setQuery(authCopy);
		searchObj.setTarget(stageCopy); 
		return searchObj;
	} // setupSearchObject(int)
	
	//------------------------------------------------------------------------
	//						  	flowsMatch
	//------------------------------------------------------------------------
	/** Determines whether every author electron-flow arrow matches a response 
	 * electron-flow arrow in the stage.
	 * @return	true if every author electron-flow arrow matches a response 
	 * electron-flow arrow in the stage
	 */
	private boolean flowsMatch() {
		final String SELF = "MechSubstructSearch.flowsMatch: ";
		// if the author gave no flows, then a structural match is enough
		if (Utils.isEmpty(authFlows)) return true; 
		// if the response has no flows, but the author does, then no match.
		if (Utils.isEmpty(respFlows)) return false; 
		for (int authIndex = 0; authIndex < authFlows.length; authIndex++) {
			debugPrint(SELF + "Checking author flow ", authIndex + 1);
			boolean respHasAuthorFlow = false;
			for (int respIndex = 0; respIndex < respFlows.length; respIndex++) {
				debugPrint(SELF + "Checking response flow ", respIndex + 1);
				if (authFlowCodes[authIndex] == respFlowCodes[respIndex]) {
					if (flowsMatch(authFlows[authIndex], respFlows[respIndex])) {
						debugPrint(SELF + "author flow ", authIndex + 1, 
								" of code ", authFlowCodes[authIndex], 
								" matches response flow ", respIndex + 1, 
								" of code ", respFlowCodes[respIndex]);
						respHasAuthorFlow = true;
						break;
					}  else debugPrint("	-- no match");
				} else debugPrint("	-- flow codes don't match");
			} // for each response flow
			if (!respHasAuthorFlow) {
				debugPrint(SELF + "no response flows match author's flow ", 
						authIndex + 1, " -- return false");
				return false;
			} 
		} // for each author flow
		debugPrint(SELF + "all author flows are found in response -- return true.");
		return true;
	} // flowsMatch()
	
	/** Determines whether a response electron-flow arrow matches an author's 
	 * electron-flow arrow.
	 * @param	authFlow	author's electron-flow arrow
	 * @param	respFlow	response electron-flow arrow
	 * @return	true if the electron-flow arrows' sources and sinks match
	 */
	private boolean flowsMatch(MechFlow authFlow, MechFlow respFlow) {
		final String SELF = "MechSubstructSearch.flowsMatch: ";
		final boolean SOURCE = true;
		final boolean sourcesMatch = terminiMatch(authFlow, respFlow, SOURCE);
		final boolean sinksMatch = terminiMatch(authFlow, respFlow, !SOURCE);
		debugPrint(SELF + "sourcesMatch = ", sourcesMatch, 
				" and sinksMatch = ", sinksMatch);
		return (sinksMatch && sourcesMatch);
	} // flowsMatch(MechFlow, MechFlow)

	//------------------------------------------------------------------------
	//						  terminiMatch
	//------------------------------------------------------------------------
	/** Given sinks or sources (termini) of electron-flow arrows from the author 
	 * and response, do they match?
	 * <P>First get a list indices of the atoms for both termini.
	 * <br>Next translate the author's indices into terms of the student's
	 *	  indices using the searchResult as a map.
	 * <br>Finally, check to see that all the values in the translated list are
	 *	  exist in the student's list.
	 * <P>EXAMPLE: Say this example IS A MATCH, both are sources which are bonds.
	 *		  The numbers in square brackets are the atom Indices
	 *		  within their respective molecules. The ?'s in the result
	 *		  just mean we don't care what they are for this example
	 *		  (we are SURE they are not 9 or 12, since there can be no
	 *		  repeates in the result). We are only interested in the two
	 *		  atoms involved in the source of each.
	 *	<P>GIVEN:
	 *	 <br>Author Structure:	 C[2] ------ Si[0]	(authTerm, bond is source)	
	 *	 <br>Response Structure:	C[9] ------ Si[12]	(respTerm, bond is source)
	 *	 <br>searchResult = [12,?,9,?,?,?,...] (means authAtom[0] matches
	 *										studentAtom[12], and authAtom[2]
	 *										matches studentAtom[9], the
	 *										result of the last MolSearch.find)
	 *	<P>CALCULATE:
	 *	 <br>authList	 = [0,2]	 (indices of pertinent atoms in stud. mol.)
	 *	 <br>respList	 = [9,12]	(indices of pertinent atoms in auth. mol.)
	 *	 <br>transList	= [2,0]	 (respList[] mapped/translated using 
	 *								searchResult[])
	 *	 <P>So YES the transList[] contains the same values as the authList[],
	 *	 therefore these two sources are a match!
	 * @param	authFlow	the author's electron-flow arrow
	 * @param	respFlow	the response's electron-flow arrow
	 * @param	isSrc	true if we are looking at sources
	 * @return	true if they match
	 */
	private boolean terminiMatch(MechFlow authFlow, MechFlow respFlow, 
			boolean isSrc) {
		final String SELF = "MechSubstructSearch.terminiMatch: ";
		final int[] authList = getTerminusAtomIndices(authMolecule, 
				authFlow, isSrc);
		debugPrint(SELF + "authList atom indices: ", authList);
		final int[] respList = getTerminusAtomIndices(stageMolecule, 
				respFlow, isSrc);
		debugPrint(SELF + "respList atom indices: ", respList);
		final int numAtoms = authList.length; // #atoms are the same for both
		
		// translate the author's indices into the student's indices
		int transList[] = new int[numAtoms];
		Arrays.fill(transList, NOT_FOUND);
		for (int alIndex = 0; alIndex < numAtoms; alIndex++) {
			for (int srIndex = 0; srIndex < searchResult.length; srIndex++) {
				if (authList[alIndex] == srIndex) { // found a mapping!
					debugPrint(SELF + "Atom ", authList[alIndex] + 1,
							" in author molecule corresponds to ",
							searchResult[srIndex] + 1,
							" in student molecule.");
					transList[alIndex] = searchResult[srIndex];
				}
			} // for each value in the searchResult[]
		} // for each index in authList[]
		debugPrint(SELF + "transList atom indices: ", transList);
		// now check that the "translated" list is the same as the student's
		// list, though perhaps not in the same order
		Arrays.sort(transList);
		Arrays.sort(respList);
		final boolean result = Arrays.equals(transList, respList);
		debugPrint(SELF + "transList and respList are ",
				(result ? "" : "not "), "equal to one another.");
		return result;
	} // terminiMatch(MechFlow, MechFlow, boolean)
	
	//------------------------------------------------------------------------
	//						  getTerminusAtomIndices
	//------------------------------------------------------------------------
	/** Gets the atom indices of an electron-flow arrow sink or source.
	 * A sink or source is made up of 
	 * one or two atoms, so the returned value will be of length 1 or 2.
	 * Example: if the source (or sink) is a bond between a Carbon with
	 * index 15 and a Bromine with index 33, returns [15,33] or [33,15]
	 * (no order guaranteed).
	 * @param	mol	a molecule
	 * @param	flow	an electron-flow arrow
	 * @param	getSrc	true if should get the source atoms
	 * @return	array of ints representing the index or indices of the atoms of
	 * the sink or source
	 */
	private int[] getTerminusAtomIndices(Molecule mol, MechFlow flow, 
			boolean getSrc) {
		final MolAtom[] atoms = (getSrc ? flow.getSrcAtoms() 
				: flow.getSinkAtoms());
		final int numAtoms = atoms.length;
		final int[] ssAtomIndices = new int[numAtoms];
		for (int atNum = 0; atNum < numAtoms; atNum++) {
			ssAtomIndices[atNum] = mol.indexOf(atoms[atNum]);
		}			
		return ssAtomIndices;	
	} // getTerminusAtomIndices(Molecule, MechFlow, boolean)
	
	//------------------------------------------------------------------------
	//					  eschewsCatShiftNot1_2
	//------------------------------------------------------------------------	
	/** Finds if there is a 1,<i>n</i>-cationic shift (n &ne; 2) in the response.
	 * @return	true if the shift is not found
	 * @throws	MechError	if the shift is found
	 * @throws	MolFormatException	if the MRV representation of the cationic
	 * shift could not be imported (shouldn't happen)
	 */
	public boolean eschewsCatShiftNot1_2() throws MechError, MolFormatException {
		final String SELF = "MechSubstructSearch.eschewsCatShiftNot1_2: ";
		final String CAT_SHIFT_CPLUS = 
				"<?xml version=\"1.0\" ?>\n"
				+ "<MDocument>\n"
				+ "  <MEFlow id=\"o1\" arcAngle=\"-150.0\" headSkip=\"0.15\" "
				+ "headLength=\"0.5\"\n"
				+ "          headWidth=\"0.4\" tailSkip=\"0.15\">\n"
				+ "    <MAtomSetPoint atomRefs=\"m1.a1 m1.a2\" />\n"
				+ "    <MAtomSetPoint atomRefs=\"m1.a2 m1.a4\" weights=\"0.25 0.75\" />\n"
				+ "  </MEFlow>\n"
				+ "  <MChemicalStruct>\n"
				+ "    <molecule molID=\"m1\">\n"
				+ "      <atomArray\n"
				+ "          atomID=\"a1 a2 a3 a4\"\n"
				+ "          elementType=\"C C C C\"\n"
				+ "          formalCharge=\"0 0 0 1\"\n"
				+ "          mrvPseudo=\"0 AH 0 0\"\n"
				+ "          x2=\"-13.766666412353516 -12.226666412353515 "
				+ "-14.058333396911621 -12.518333396911622\"\n"
				+ "          y2=\"2.3333332538604736 2.3333332538604736 "
				+ "-0.5249999761581421 -0.5249999761581421\"\n"
				+ "          />\n"
				+ "      <bondArray>\n"
				+ "        <bond atomRefs2=\"a1 a2\" order=\"1\" />\n"
				+ "        <bond atomRefs2=\"a3 a4\" order=\"1\" />\n"
				+ "      </bondArray>\n"
				+ "    </molecule>\n"
				+ "  </MChemicalStruct>\n"
				+ "</MDocument>";
		eschewsCatShiftNot1_2(CAT_SHIFT_CPLUS); // throws MechError if found
		final String CAT_SHIFT_LG =
				"<?xml version=\"1.0\" ?>\n"
				+ "<MDocument>\n"
				+ "  <MEFlow id=\"o1\" arcAngle=\"248.39738999999997\" "
				+ "headSkip=\"0.25\"\n"
				+ "          headLength=\"0.5\" headWidth=\"0.4\" tailSkip=\"0.15\">\n"
				+ "    <MAtomSetPoint atomRefs=\"m1.a4 m1.a5\" />\n"
				+ "    <MAtomSetPoint atomRefs=\"m1.a5\" />\n"
				+ "  </MEFlow>\n"
				+ "  <MEFlow id=\"o2\" arcAngle=\"-150.0\" "
				+ 			"headSkip=\"0.15\" headLength=\"0.5\"\n"
				+ "          headWidth=\"0.4\" tailSkip=\"0.15\">\n"
				+ "    <MAtomSetPoint atomRefs=\"m1.a1 m1.a2\" />\n"
				+ "    <MAtomSetPoint atomRefs=\"m1.a2 m1.a4\" "
				+ 						"weights=\"0.25 0.75\" />\n"
				+ "  </MEFlow>\n"
				+ "  <MChemicalStruct>\n"
				+ "    <molecule molID=\"m1\">\n"
				+ "      <atomArray\n"
				+ "          atomID=\"a1 a2 a3 a4 a5\"\n"
				+ "          elementType=\"C C C C O\"\n"
				+ "          mrvQueryProps=\"0 0 0 0 L,N,O,S,Cl,Br,I:\"\n"
				+ "          mrvPseudo=\"0 AH 0 0 0\"\n"
				+ "          x2=\"-3.441666603088379 -1.9016666030883789 "
				+ "-3.674999952316284 -2.134999952316284 -1.3649999523162841\"\n"
				+ "          y2=\"1.3416666984558105 1.3416666984558105 "
				+ "-1.399999976158142 -1.399999976158142 -2.733679097986178\"\n"
				+ "          />\n"
				+ "      <bondArray>\n"
				+ "        <bond atomRefs2=\"a1 a2\" order=\"1\" />\n"
				+ "        <bond atomRefs2=\"a3 a4\" order=\"1\" />\n"
				+ "        <bond atomRefs2=\"a4 a5\" order=\"1\" />\n"
				+ "      </bondArray>\n"
				+ "    </molecule>\n"
				+ "  </MChemicalStruct>\n"
				+ "</MDocument>";
		eschewsCatShiftNot1_2(CAT_SHIFT_LG); // throws MechError if found
		debugPrint(SELF + "no violation of rule.");
		return true; // not found
	} // eschewsCatShiftNot1_2()
	
	/** Finds if there is a 1,<i>n</i>-cationic shift (n &ne; 2) in the response.
	 * @param	shift	MRV representation of a 1,n-cationic shift
	 * @throws	MechError	if the shift is found
	 * @throws	MolFormatException	if the MRV representation of the cationic
	 * shift could not be imported (shouldn't happen)
	 */
	private void eschewsCatShiftNot1_2(String shift) 
			throws MechError, MolFormatException {
		final String SELF = "MechSubstructSearch.eschewsCatShiftNot1_2: ";
		authXml = shift;
		// get atom indices plus stage number of match 
		final int[] searchResult = hasSubstructure(); 
		if (!Utils.isEmpty(searchResult)) {
			final int stageNum = searchResult[searchResult.length - 1];
			debugPrint(SELF + "found shift in stage ", stageNum + 1);
			/* final String stageXML = 
					respMechData.getStage(stageNum).getStageXML(); 
			stageMolecule = MolImporter.importMol(stageXML); /**/
			stageMolecule = respMechData.getStage(stageNum).getFusedMolecule(); 
			final MolAtom origCatC = stageMolecule.getAtom(searchResult[3]);
			final MolAtom newCatC = stageMolecule.getAtom(searchResult[0]);
			// if the old C+ and new C+ atoms in the searchResult 
			// are adjacent, then the cationic shift is 1,2,
			// so the rule is not violated, and valid is true
			final boolean found = origCatC.getBondTo(newCatC) == null;
			if (searchResult.length > 4)
				debugPrint(SELF + "searchResult is {",
						searchResult[0], ", ", searchResult[1], ", ",
						searchResult[2], ", ", searchResult[3], ", ", 
						searchResult[4], "}, found = ", found);
			else debugPrint(SELF + "searchResult is {", searchResult[0], ", ", 
					searchResult[1], ", ", searchResult[2], ", ", 
					searchResult[3], "}, found = ", found);
			if (found)
				respMech.throwMechError(stageNum, RULE_VIOLATION);
			else debugPrint(SELF + "found shift is allowed 1,2-shift.");
		} else debugPrint(SELF + "no shift found.");
	} // eschewsCatShiftNot1_2(String)
	
	//------------------------------------------------------------------------
	//					  eschewsXHBondAsXNucleophile
	//------------------------------------------------------------------------	
	/** Finds if X uses an X-H bond to be a nucleophile.
	 * @return	true if a nucloephilic X-H bond is not found
	 * @throws	MechError	if a nucleophilic X-H bond is found
	 * @throws	MolFormatException	if the MRV representation of a nucleophilic 
	 * X-H bond could not be imported (shouldn't happen)
	 */
	public boolean eschewsXHBondAsXNucleophile() 
			throws MechError, MolFormatException {
		final String SELF = "MechSubstructSearch.eschewsXHBondAsXNucleophile: ";
		authXml = 
				"<?xml version=\"1.0\" ?>\n"
				+ "<MDocument>\n"
				+ "  <MEFlow id=\"o1\" arcAngle=\"150.0\" "
				+ 			"headSkip=\"0.15\" headLength=\"0.6\"\n"
				+ "          headWidth=\"0.5\" tailSkip=\"0.15\">\n"
				+ "    <MAtomSetPoint atomRefs=\"m1.a1 m1.a2\" />\n"
				+ "    <MAtomSetPoint atomRefs=\"m1.a1 m1.a3\" "
				+ 						"weights=\"0.25 0.75\" />\n"
				+ "  </MEFlow>\n"
				+ "  <MChemicalStruct>\n"
				+ "    <molecule molID=\"m1\">\n"
				+ "      <atomArray\n"
				+ "          atomID=\"a1 a2 a3\"\n"
				+ "          elementType=\"C H C\"\n"
				+ "          mrvQueryProps=\"A: 0 0\"\n"
				+ "          mrvPseudo=\"0 0 AH\"\n"
				+ "          x2=\"-4.8125 -3.2725 -6.06374979019165\"\n"
				+ "          y2=\"1.3956249952316284 1.3956249952316284 "
				+ 							"-0.8662499785423279\"\n"
				+ "          />\n"
				+ "      <bondArray>\n"
				+ "        <bond atomRefs2=\"a1 a2\" order=\"1\" />\n"
				+ "      </bondArray>\n"
				+ "    </molecule>\n"
				+ "  </MChemicalStruct>\n"
				+ "</MDocument>\n";
		// get atom indices plus stage number of match 
		final int[] searchResult = hasSubstructure(); 
		if (!Utils.isEmpty(searchResult)) {
			final int stageNum = searchResult[searchResult.length - 1];
			debugPrint(SELF + "found match in stage ", stageNum + 1);
			respMech.throwMechError(stageNum, RULE_VIOLATION);
		}
		debugPrint(SELF + "no matches found, so no violation of rule");
		return true;
	} // eschewsXHBondAsXNucleophile()

	//------------------------------------------------------------------------
	//					  noDyotropicRearrt
	//------------------------------------------------------------------------	
	/** Finds if the stage contains a dyotropic rearrangement (A-B-C-D to D-B-C-A).
	 * @return	true if a dyotropic rearrangement is not found
	 * @throws	MechError	if a dyotropic rearrangement is found
	 * @throws	MolFormatException	if the MRV representation of a dyotropic
	 * rearrangement could not be imported (shouldn't happen)
	 */
	public boolean noDyotropicRearrt() throws MechError, MolFormatException {
		final String SELF = "MechSubstructSearch.noDyotropicRearrt: ";
		authXml = "<?xml version=\"1.0\" ?>\n"
				+ "<cml>\n"
				+ "<MDocument>\n"
				+ "  <MChemicalStruct>\n"
				+ "      <molecule molID=\"m1\">\n"
				+ "        <atomArray\n"
				+ "          atomID=\"a1 a2 a3 a4\"\n"
				+ "            elementType=\"C C C C\"\n"
				+ "          mrvQueryProps=\"0 0 0 0\"\n"
				+ "            mrvPseudo=\"AH AH AH AH\"\n"
				+ "          x2=\"-6.269999980926514 -4.936320859098478 -3.847376416071195 -2.5136972942431592\"\n"
				+ "            y2=\"1.5399999618530273 2.3099999618530274 1.2210555188257441 1.991055518825744\"\n"
				+ "          />\n"
				+ "        <bondArray>\n"
				+ "        <bond atomRefs2=\"a1 a2\" order=\"1\" />\n"
				+ "        <bond atomRefs2=\"a2 a3\" order=\"1\" queryType=\"Any\" />\n"
				+ "        <bond atomRefs2=\"a3 a4\" order=\"1\" />\n"
				+ "      </bondArray>\n"
				+ "      </molecule>\n"
				+ "    </MChemicalStruct>\n"
				+ "  <MEFlow id=\"o2\" arcAngle=\"-182.17399554899995\" headSkip=\"0.15\"\n"
				+ "            headLength=\"0.5\" headWidth=\"0.4\" tailSkip=\"0.15\">\n"
				+ "    <MAtomSetPoint atomRefs=\"m1.a3 m1.a4\" />\n"
				+ "    <MAtomSetPoint atomRefs=\"m1.a4 m1.a2\" weights=\"0.25 0.75\" />\n"
				+ "  </MEFlow>\n"
				+ "    <MEFlow id=\"o3\" arcAngle=\"-182.17399554899995\" headSkip=\"0.15\"\n"
				+ "          headLength=\"0.5\" headWidth=\"0.4\" tailSkip=\"0.15\">\n"
				+ "      <MAtomSetPoint atomRefs=\"m1.a1 m1.a2\" />\n"
				+ "      <MAtomSetPoint atomRefs=\"m1.a1 m1.a3\" weights=\"0.25 0.75\" />\n"
				+ "    </MEFlow>\n"
				+ "</MDocument>\n"
				+ "</cml>\n";
		// get atom indices plus stage number of match
		final int[] searchResult = hasSubstructure(); 
		if (!Utils.isEmpty(searchResult)) {
			final int stageNum = searchResult[searchResult.length - 1];
			debugPrint(SELF + "found match in stage ", stageNum + 1);
			respMech.throwMechError(stageNum, RULE_VIOLATION);
		}
		debugPrint(SELF + "no matches found, so no violation of rule");
		return true;
	} // noDyotropicRearrt()

	//------------------------------------------------------------------------
	//					  noSigmaMetathesis
	//------------------------------------------------------------------------	
	/** Finds if the stage contains a &sigma;-bond metathesis (A-B + C-D to 
	 * D-B + C-A).
	 * @return	true if a &sigma;-bond metathesis is not found
	 * @throws	MechError	if a &sigma;-bond metathesis is found
	 * @throws	MolFormatException	if the MRV representation of a 
	 * &sigma;-bond metathesis could not be imported (shouldn't happen)
	 */
	public boolean noSigmaMetathesis() throws MechError, MolFormatException {
		final String SELF = "MechSubstructSearch.noSigmaMetathesis: ";
		authXml = "<?xml version=\"1.0\" ?>\n"
				+ "<cml>\n"
				+ "<MDocument>\n"
				+ "  <MChemicalStruct>\n"
				+ "    <molecule molID=\"m1\">\n"
				+ "      <atomArray\n"
				+ "          atomID=\"a1 a2 a3 a4\"\n"
				+ "          elementType=\"C C C C\"\n"
				+ "          mrvQueryProps=\"0 0 0 0\"\n"
				+ "          mrvPseudo=\"AH AH AH AH\"\n"
				+ "          x2=\"-4.62000036239624 -5.445000171661377 -2.309999942779541 -2.9700000286102295\"\n"
				+ "          y2=\"3.68500018119812 1.8700000858306884 3.575000047683716 1.7599999523162841\"\n"
				+ "          />\n"
				+ "      <bondArray>\n"
				+ "        <bond atomRefs2=\"a1 a2\" order=\"1\" />\n"
				+ "        <bond atomRefs2=\"a3 a4\" order=\"1\" />\n"
				+ "      </bondArray>\n"
				+ "    </molecule>\n"
				+ "  </MChemicalStruct>\n"
				+ "  <MEFlow id=\"o2\" arcAngle=\"223.9449667989687\" headSkip=\"0.15\"\n"
				+ "          headLength=\"0.5\" headWidth=\"0.4\" tailSkip=\"0.15\">\n"
				+ "    <MAtomSetPoint atomRefs=\"m1.a1 m1.a2\" />\n"
				+ "    <MAtomSetPoint atomRefs=\"m1.a1 m1.a3\" weights=\"0.25 0.75\" />\n"
				+ "  </MEFlow>\n"
				+ "  <MEFlow id=\"o3\" arcAngle=\"221.44931346551908\" headSkip=\"0.15\"\n"
				+ "          headLength=\"0.5\" headWidth=\"0.4\" tailSkip=\"0.15\">\n"
				+ "    <MAtomSetPoint atomRefs=\"m1.a3 m1.a4\" />\n"
				+ "    <MAtomSetPoint atomRefs=\"m1.a4 m1.a2\" weights=\"0.25 0.75\" />\n"
				+ "  </MEFlow>\n"
				+ "</MDocument>\n"
				+ "</cml>\n";
		// get atom indices plus stage number of match
		final int[] searchResult = hasSubstructure(); 
		if (!Utils.isEmpty(searchResult)) {
			final int stageNum = searchResult[searchResult.length - 1];
			debugPrint(SELF + "found match in stage ", stageNum + 1);
			respMech.throwMechError(stageNum, RULE_VIOLATION);
		}
		debugPrint(SELF + "no matches found, so no violation of rule");
		return true;
	} // noSigmaMetathesis()

	//------------------------------------------------------------------------
	//					  noFourMembProtonTransfer
	//------------------------------------------------------------------------	
	/** Finds if the stage contains a proton transfer occurring by a
	 * four-membered transition state.
	 * @return	true if a proton transfer occurring by a four-membered 
	 * transition state is not found
	 * @throws	MechError	if a proton transfer occurring by a 
	 * four-membered transition state is found
	 * @throws	MolFormatException	if the MRV representation of a 
	 * proton transfer occurring by a four-membered transition state 
	 * could not be imported (shouldn't happen)
	 */
	public boolean noFourMembProtonTransfer() 
			throws MechError, MolFormatException {
		final String SELF = "MechSubstructSearch.noFourMembProtonTransfer: ";
		authXml = "<?xml version=\"1.0\" ?>\n"
				+ "<cml>\n"
				+ "<MDocument>\n"
				+ "  <MChemicalStruct>\n"
				+ "    <molecule molID=\"m1\">\n"
				+ "      <atomArray\n"
				+ "          atomID=\"a1 a2 a3 a4\"\n"
				+ "          elementType=\"C C C H\"\n"
				+ "          mrvQueryProps=\"A: A: A: 0\"\n"
				+ "          x2=\"-7.039999961853027 -5.940000038146972 -4.840000114440917 -6.269999961853027\"\n"
				+ "          y2=\"2.365000009536743 1.0313208877087077 2.31000018119812 3.698679131364779\"\n"
				+ "          />\n"
				+ "      <bondArray>\n"
				+ "        <bond atomRefs2=\"a1 a2\" order=\"1\" />\n"
				+ "        <bond atomRefs2=\"a2 a3\" order=\"1\" />\n"
				+ "        <bond atomRefs2=\"a1 a4\" order=\"1\" />\n"
				+ "      </bondArray>\n"
				+ "    </molecule>\n"
				+ "  </MChemicalStruct>\n"
				+ "  <MEFlow id=\"o2\" arcAngle=\"-150.0\" headSkip=\"0.15\" headLength=\"0.5\"\n"
				+ "          headWidth=\"0.4\" tailSkip=\"0.25\">\n"
				+ "    <MEFlowBasePoint atomRef=\"m1.a3\" />\n"
				+ "    <MAtomSetPoint atomRefs=\"m1.a3 m1.a4\" weights=\"0.25 0.75\" />\n"
				+ "  </MEFlow>\n"
				+ "  <MEFlow id=\"o3\" arcAngle=\"-250.19139621618717\" headSkip=\"0.25\"\n"
				+ "          headLength=\"0.5\" headWidth=\"0.4\" tailSkip=\"0.15\">\n"
				+ "    <MAtomSetPoint atomRefs=\"m1.a1 m1.a4\" />\n"
				+ "    <MAtomSetPoint atomRefs=\"m1.a1\" />\n"
				+ "  </MEFlow>\n"
				+ "</MDocument>\n"
				+ "</cml>\n";
		// get atom indices plus stage number of match
		final int[] searchResult = hasSubstructure(CHARGE_MASK); 
		if (!Utils.isEmpty(searchResult)) {
			final int stageNum = searchResult[searchResult.length - 1];
			debugPrint(SELF + "found match in stage ", stageNum + 1);
			respMech.throwMechError(stageNum, RULE_VIOLATION);
		}
		debugPrint(SELF + "no matches found, so no violation of rule");
		return true;
	} // noFourMembProtonTransfer()

	//------------------------------------------------------------------------
	//					  noAcidicE2
	//------------------------------------------------------------------------	
	/** Finds if the stage contains simultaneous cleavage of an H-A and A-A bond
	 * on adjacent atoms to form a double or triple bond.
	 * @return	true if an E2 elimination is not found
	 * @throws	MechError	if an E2 elimination is found
	 * @throws	MolFormatException	if the MRV representation of an E2
	 * elimination could not be imported (shouldn't happen) 
	 */
	public boolean noAcidicE2() throws MechError, MolFormatException {
		final String SELF = "MechSubstructSearch.noAcidicE2: ";
		authXml = "<?xml version=\"1.0\" ?>\n"
				+ "<cml>\n"
				+ "<MDocument>\n"
				+ "  <MChemicalStruct>\n"
				+ "    <molecule molID=\"m1\">\n"
				+ "      <atomArray\n"
				+ "          atomID=\"a1 a2 a3 a4\"\n"
				+ "          elementType=\"C C H C\"\n"
				+ "          mrvQueryProps=\"A: A: 0 A:\"\n"
				+ "          x2=\"-4.070000171661377 -2.530000171661377 -2.1314188422034945 -4.4685815011192584\"\n"
				+ "          y2=\"2.9700000286102295 2.9700000286102295 1.4824742561250643 1.4824742561250641\"\n"
				+ "          />\n"
				+ "      <bondArray>\n"
				+ "        <bond atomRefs2=\"a1 a2\" order=\"1\" queryType=\"Any\" />\n"
				+ "        <bond atomRefs2=\"a2 a3\" order=\"1\" />\n"
				+ "        <bond atomRefs2=\"a1 a4\" order=\"1\" />\n"
				+ "      </bondArray>\n"
				+ "    </molecule>\n"
				+ "  </MChemicalStruct>\n"
				+ "  <MEFlow id=\"o2\" arcAngle=\"189.9\" headSkip=\"0.15\" headLength=\"0.5\"\n"
				+ "          headWidth=\"0.4\" tailSkip=\"0.15\">\n"
				+ "    <MAtomSetPoint atomRefs=\"m1.a2 m1.a3\" />\n"
				+ "    <MAtomSetPoint atomRefs=\"m1.a1 m1.a2\" />\n"
				+ "  </MEFlow>\n"
				+ "  <MEFlow id=\"o3\" arcAngle=\"248.39738999999997\" headSkip=\"0.25\"\n"
				+ "          headLength=\"0.5\" headWidth=\"0.4\" tailSkip=\"0.15\">\n"
				+ "    <MAtomSetPoint atomRefs=\"m1.a1 m1.a4\" />\n"
				+ "    <MAtomSetPoint atomRefs=\"m1.a4\" />\n"
				+ "  </MEFlow>\n"
				+ "</MDocument>\n"
				+ "</cml>\n";
		// get atom indices plus stage number of match
		final int[] searchResult = hasSubstructure(CHARGE_MASK); 
		if (!Utils.isEmpty(searchResult)) {
			final int stageNum = searchResult[searchResult.length - 1];
			debugPrint(SELF + "found match in stage ", stageNum + 1);
			respMech.throwMechError(stageNum, RULE_VIOLATION);
		}
		debugPrint(SELF + "no matches found, so no violation of rule");
		return true;
	} // noAcidicE2()

} // MechSubstructSearch	
