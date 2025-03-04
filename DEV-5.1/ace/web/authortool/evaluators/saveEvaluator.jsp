<%@ page language="java" %>
<%@ page import="
	com.epoch.chem.ChemUtils,
	com.epoch.evals.EvalManager,
	com.epoch.evals.Evaluator,
	com.epoch.evals.Subevaluator,
	com.epoch.evals.impl.chemEvals.*,
	com.epoch.evals.impl.chemEvals.energyEvals.*,
	com.epoch.evals.impl.chemEvals.lewisEvals.*,
	com.epoch.evals.impl.chemEvals.mechEvals.*,
	com.epoch.evals.impl.chemEvals.synthEvals.*,
	com.epoch.evals.impl.genericQEvals.*,
	com.epoch.evals.impl.genericQEvals.clickEvals.*,
	com.epoch.evals.impl.genericQEvals.multEvals.*,
	com.epoch.evals.impl.genericQEvals.numericEvals.*,
	com.epoch.evals.impl.genericQEvals.rankEvals.*,
	com.epoch.evals.impl.genericQEvals.tableEvals.*,
	com.epoch.evals.impl.genericQEvals.textEvals.*,
	com.epoch.evals.impl.physicsEvals.*,
	com.epoch.evals.impl.physicsEvals.eqnsEvals.*,
	com.epoch.evals.impl.physicsEvals.vectorsEvals.*,
	com.epoch.exceptions.EquationFormatException,
	com.epoch.genericQTypes.TableQ,
	com.epoch.mechanisms.MechSet,
	com.epoch.mechanisms.MechSubstructSearch,
	com.epoch.physics.EquationFunctions,
	com.epoch.qBank.Question,
	com.epoch.synthesis.Synthesis,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%!
	public boolean isTrue(HttpServletRequest request, String param) {
		return "true".equals(request.getParameter(param));
	} // isTrue(String)

	public int getInt(HttpServletRequest request, String param) {
		return MathUtils.parseInt(request.getParameter(param));
	} // getInt(String)

	public double getDouble(HttpServletRequest request, String param) {
		return MathUtils.parseDouble(request.getParameter(param));
	} // getDouble(String)

	public double getDouble(HttpServletRequest request, String param, double val) {
		return MathUtils.parseDouble(request.getParameter(param), val);
	} // getDouble(String)

	public String toCERs(HttpServletRequest request, String param) {
		return Utils.inputToCERs(request.getParameter(param));
	} // toCERs(String)
%>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");

	final String pathToRoot = "../../";
	// a few constants to tighten the code.
	final String ROW = "row";
	final String COLUMN = "column";
	final String OPER = "oper";
	final String ROW_OPER = "rowOper";
	final String MOLS_OPER = "molsOper";
	final String NUM_MOLS = "numMols";
	final String EMPTY_CELL = "emptyCell";
	final String IS_POSITIVE = "isPositive";
	final String IGNORE_CASE = "ignoreCase";
	final String NONNUMERIC = "nonnumeric";
	final String NUMBER = "number";
	final String WHERE = "where";
	final String TOLERANCE = "tolerance";
	final String HOW_MANY = "howMany";
	
	/* Input 
			evalNum 
			subevalNum 
			matchgroup = MOLWT ..
			matchode = IS ...

			feedback
			grade

			molstruct (data of indefinite length, usu. a molecule)
			molname (description of said data

	knows the input parameter from each frame 
				  values to each frame (taken from answerinput)
	*/

	Question question;
	synchronized (session) {
		question = (Question) session.getAttribute("qBuffer");
	}
	final String evalNumStr = request.getParameter("evalNum");
	final String subevalNumStr = request.getParameter("subevalNum");
	final String matchCode = request.getParameter("matchcode");

	/* Utils.alwaysPrint("saveEvaluator.jsp: saved evaluator has ",
			"evalNum ", evalNumStr, 
			"subevalNum ", subevalNumStr, ", match code = ", matchCode);
	*/

	final boolean isComplex = isTrue(request, "iscomplex");
	if (evalNumStr == null || subevalNumStr == null || question == null 
			|| (!isComplex && matchCode == null)) {
		%> <jsp:forward page="../errorParam.jsp"/> <%
	}

	final int evalNum = MathUtils.parseInt(evalNumStr);
	final int subevalNum = MathUtils.parseInt(subevalNumStr);
	final Subevaluator newSubeval = new Subevaluator();
	final Evaluator newEval = new Evaluator(newSubeval);
	newSubeval.matchCode = matchCode;
	newSubeval.molStruct = Utils.inputToCERs(request.getParameter("molstruct"));
	newSubeval.molName = toCERs(request, "molname");
	newEval.grade = getDouble(request, "grade");
	newEval.feedback = toCERs(request, "feedback");
    
	/* Processing specific to the group; set haveError if a number fails to
	 parse */

	final String[] CODES = EvalManager.EVAL_CODES;
	boolean haveError = false;
	try {
		if (isComplex) {
			 // Nothing to be copied
		} else if (Utils.among(matchCode, 
				CODES[EvalManager.IS_OR_HAS_SIGMA_NETWORK],
				CODES[EvalManager.IS_2D_CHAIR])) {
			final Is impl = new Is();
			impl.setHowMany(getInt(request, HOW_MANY));
			impl.setFlags(getInt(request, "flags"));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.SKELETON_SUBSTRUCTURE])) {
			final Contains impl = new Contains();
			impl.setHowMany(getInt(request, HOW_MANY));
			impl.setMethod(getInt(request, "method"));
			impl.setChgRadIso(getInt(request, "chgRadIso"));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.WEIGHT])) {
			final Weight impl = new Weight();
			final String molwt = request.getParameter("molweight").trim();
			String toler = request.getParameter(TOLERANCE).trim();
			Double.parseDouble(molwt);
			if (!"".equals(toler)) Double.parseDouble(toler);
			else toler = " ";
			impl.setMolWeight(molwt);
			impl.setTolerance(toler);
			impl.setWtOper(getInt(request, "wtOper"));
			impl.setWtType(getInt(request, "weightType"));
			impl.setMolsOper(getInt(request, MOLS_OPER));
			impl.setNumMols(getInt(request, NUM_MOLS));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.FORMULA_WEIGHT])) {
			final FormulaWeight impl = new FormulaWeight();
			final String molwt = request.getParameter("molweight").trim();
			String toler = request.getParameter(TOLERANCE).trim();
			Double.parseDouble(molwt);
			if (!"".equals(toler)) Double.parseDouble(toler);
			else toler = " ";
			impl.setMolWeight(molwt);
			impl.setTolerance(toler);
			impl.setWtOper(getInt(request, "wtOper"));
			impl.setWtType(getInt(request, "weightType"));
			newSubeval.codedData = impl.getCodedData();
		 } else if (matchCode.equals(CODES[EvalManager.FUNCTIONAL_GROUP])) {
			final FnalGroup impl = new FnalGroup();
			impl.setGroupId(getInt(request, "fnalGroupId"));
			impl.setGroupOper(getInt(request, "groupOper"));
			impl.setNumGroups(getInt(request, "numGroups"));
			final String countEachStr = request.getParameter("countEach");
			final boolean countEach = "each".equals(countEachStr);
			impl.setCountEach(countEach);
			if (countEach) {
				impl.setMolsOper(getInt(request, MOLS_OPER));
				impl.setNumMols(getInt(request, NUM_MOLS));
			} else {
				impl.setMolsOper(FnalGroup.NOT_EQUALS);
				impl.setNumMols(0);
			} // if countEach
			newSubeval.codedData = impl.getCodedData();
		 } else if (matchCode.equals(CODES[EvalManager.NUM_ATOMS])) {
			final Atoms impl = new Atoms();
			impl.setAtomsOper(getInt(request, "atomsOper"));
			impl.setNumAtoms(getInt(request, "numAtoms"));
			impl.setContiguous(isTrue(request, "contig"));
			impl.setElement(request.getParameter("element"));
			final String countEachStr = request.getParameter("countEach");
			final boolean countEach = "each".equals(countEachStr);
			impl.setCountEach(countEach);
			if (countEach) {
				impl.setMolsOper(getInt(request, MOLS_OPER));
				impl.setNumMols(getInt(request, NUM_MOLS));
			} else {
				impl.setMolsOper(Atoms.NOT_EQUALS);
				impl.setNumMols(0);
			} // if countEach
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.CHIRAL])) {
			final Chiral impl = new Chiral();
			impl.setProportion(getInt(request, "proportion"));
			impl.setKind(getInt(request, "kind"));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.TOTAL_CHARGE])) {
			final Charge impl = new Charge();
			impl.setChgOper(getInt(request, "chgOper"));
			impl.setCharge(getInt(request, "chgValue"));
			final String countEachStr = request.getParameter("countEach");
			final boolean countEach = "each".equals(countEachStr);
			impl.setCountEach(countEach);
			if (countEach) {
				impl.setMolsOper(getInt(request, MOLS_OPER));
				impl.setNumMols(getInt(request, NUM_MOLS));
			} else {
				impl.setMolsOper(Charge.NOT_EQUALS);
				impl.setNumMols(0);
			} // if countEach
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.NUM_MOLECULES])) {
			final NumMols impl = new NumMols();
			impl.setOper(getInt(request, OPER));
			impl.setNumMols(getInt(request, NUMBER));
			impl.setDistinct(isTrue(request, "distinct"));
			impl.setFlags(getInt(request, "flags"));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.UNSATURATION])) {
			final UnsaturIndex impl = new UnsaturIndex();
			impl.setOper(getInt(request, OPER));
			impl.setUI(getInt(request, NUMBER));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.NUM_RINGS])) {
			final Rings impl = new Rings();
			impl.setRingsOper(getInt(request, "ringsOper"));
			impl.setNumRings(getInt(request, "numRings"));
			final String countEachStr = request.getParameter("countEach");
			final boolean countEach = "each".equals(countEachStr);
			impl.setCountEach(countEach);
			if (countEach) {
				impl.setMolsOper(getInt(request, MOLS_OPER));
				impl.setNumMols(getInt(request, NUM_MOLS));
			} else {
				impl.setMolsOper(Rings.NOT_EQUALS);
				impl.setNumMols(0);
			} // if countEach
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.HAS_FORMULA])) {
			final HasFormula impl = new HasFormula();
			impl.setFormula(request.getParameter("formula"));
			final String countEachStr = request.getParameter("countEach");
			final boolean countEach = countEachStr.startsWith("each");
			impl.setCountEach(countEach);
			if (countEach) {
				impl.setMolsOper(getInt(request, MOLS_OPER));
				impl.setNumMols(getInt(request, NUM_MOLS));
				impl.setWithFormula("eachHas".equals(countEachStr));
			} else {
				impl.setMolsOper("allHas".equals(countEachStr)
						? HasFormula.GREATER : HasFormula.EQUALS);
				impl.setNumMols(0);
			} // if countEach
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.FORMULA_FORMAT])) {
			final FormulaFormat impl = new FormulaFormat();
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setRule(getInt(request, "rule"));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.MAPPED_ATOMS])) {
			final MapProperty impl = new MapProperty();
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setOper(getInt(request, OPER));
   			impl.setPatternOnly(isTrue(request, "patternOnly"));
   			impl.setCheckEnant(isTrue(request, "checkEnantiomer"));
   			impl.setAromatize(isTrue(request, "aromatize"));
			newSubeval.codedData = impl.getCodedData();
			final String selectionsStr = request.getParameter("selectionsStr");
			if (!Utils.isEmpty(selectionsStr)) {
				newSubeval.molStruct = ChemUtils.setSelections(
						newSubeval.molStruct, selectionsStr);
			} // if there are selections to set
		} else if (matchCode.equals(CODES[EvalManager.MAPPED_COUNT])) {
			final MapSelectionsCounter impl = new MapSelectionsCounter();
   			impl.setCheckEnant(isTrue(request, "checkEnantiomer"));
   			impl.setAromatize(isTrue(request, "aromatize"));
			impl.setMatchPtsStr(request.getParameter("matchPtsStr"));
			impl.setMismatchPtsStr(request.getParameter("mismatchPtsStr"));
			newSubeval.codedData = impl.getCodedData();
			final String selectionsStr = request.getParameter("selectionsStr");
			if (!Utils.isEmpty(selectionsStr)) {
				newSubeval.molStruct = ChemUtils.setSelections(
						newSubeval.molStruct, selectionsStr);
			} // if there are selections to set
		} else if (matchCode.equals(CODES[EvalManager.CONFORMATION_CHAIR])) {
			final ConformChair impl = new ConformChair();
			impl.setOrientation(getInt(request, "orientation"));
			impl.setFormula(request.getParameter("formula"));
			impl.setOper(getInt(request, OPER));
			impl.setNumber(getInt(request, "molCount"));
			impl.setOverrideFor(isTrue(request, "overrideFor"));
			newSubeval.codedData = impl.getCodedData();
    	} else if (matchCode.equals(CODES[EvalManager.CONFORMATION_ACYCLIC])) {
			final ConformBond impl = new ConformBond();
			impl.setFormula1(request.getParameter("formula1"));
			impl.setFormula2(request.getParameter("formula2"));
			impl.setGroupRelationship(getInt(request, "groupRelation"));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.BOND_ANGLE])) {
			final BondAngle impl = new BondAngle();
			impl.setAuthAngle(getInt(request, "authAngle"));
			impl.setTolerance(getInt(request, TOLERANCE));
			impl.setOper(getInt(request, OPER));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.LEWIS_ISOMORPHIC])) {
			final LewisIsomorph impl = new LewisIsomorph();
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.LEWIS_VALENCE_ELECS])) {
			final LewisValenceTotal impl = new LewisValenceTotal();
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.LEWIS_FORMAL_CHGS])) { 
			final LewisFormalCharge impl = new LewisFormalCharge();
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.LEWIS_OUTER_SHELL_COUNT])) {
			final LewisOuterNumber impl = new LewisOuterNumber();
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setOper(getInt(request, OPER));
			impl.setNumber(getInt(request, NUMBER));
			impl.setElement(request.getParameter("element"));
			impl.setCondNumber(getInt(request, "condition"));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.LEWIS_ELECTRON_DEFICIENT])) {
			final LewisElecDeficCt impl = new LewisElecDeficCt();
			impl.setOper(getInt(request, OPER));
			impl.setNumber(getInt(request, NUMBER));
			impl.setElement(request.getParameter("element"));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.MECH_EQUALS])) {
			final MechEquals impl = new MechEquals();
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.MECH_FLOWS])) {
			final MechFlowsValid impl = new MechFlowsValid();
			final boolean resonanceLenient =
					request.getParameter("resonanceLenient") != null;
			final boolean stereoLenient =
					request.getParameter("stereoLenient") != null;
			int flags = MechSet.NOT_LENIENT;
			if (resonanceLenient) flags |= MechSet.RESON_LENIENT; 
			if (stereoLenient) flags |= MechSet.STEREO_LENIENT;
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setFlags(flags);
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.MECH_INIT])) {
			final MechInitiation impl = new MechInitiation();
			final boolean resonanceLenient =
					request.getParameter("resonanceLenient") != null;
			final boolean stereoLenient =
					request.getParameter("stereoLenient") != null;
			int flags = MechSet.NOT_LENIENT;
			if (resonanceLenient) flags |= MechSet.RESON_LENIENT; 
			if (stereoLenient) flags |= MechSet.STEREO_LENIENT;
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setFlags(flags);
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.MECH_PRODS_STARTERS_IS])) {
			final MechProdStartIs impl = new MechProdStartIs();
			final boolean resonanceLenient =
					request.getParameter("resonanceLenient") != null;
			final boolean stereoLenient =
					request.getParameter("stereoLenient") != null;
			int flags = MechSet.NOT_LENIENT;
			if (resonanceLenient) flags |= MechSet.RESON_LENIENT; 
			if (stereoLenient) flags |= MechSet.STEREO_LENIENT;
			impl.setCombination(getInt(request, "combination"));
			impl.setProductOrStart(getInt(request, "productOrStart"));
			impl.setFlags(flags);
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.MECH_PRODS_STARTERS_PROPS])) {
			final MechProdStartProps impl = new MechProdStartProps();
			impl.setHowMany(getInt(request, HOW_MANY));
			impl.setCpdsType(getInt(request, "cpdsType"));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.MECH_SUBSTRUCTURE])) {
			final MechSubstructure impl = new MechSubstructure();
			final String ignoreCharge = request.getParameter("ignoreCharge");
			final String ignoreRadState = request.getParameter("ignoreRadState");
			final String ignoreIsotopes = request.getParameter("ignoreIsotopes");
			int ignoreFlags = 0;
			if (ignoreCharge != null) 
				ignoreFlags |= MechSubstructSearch.CHARGE_MASK; 
			if (ignoreRadState != null) 
				ignoreFlags |= MechSubstructSearch.RADSTATE_MASK; 
			if (ignoreIsotopes != null) 
				ignoreFlags |= MechSubstructSearch.ISOTOPES_MASK; 
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setIgnoreFlags(ignoreFlags);
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.MECH_TOPOLOGY])) {
			final MechShape impl = new MechShape();
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setTopology(getInt(request, "topology"));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.MECH_RULE])) {
			final MechRule impl = new MechRule();
			final int rule = getInt(request, "rule");
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setRule(rule);
			boolean omit1stStep;
			switch (rule) {
				case MechRule.ACID_PK:
					impl.setPKValue(getDouble(request, "pKvalue"));
					break;
				case MechRule.BASE_PK: 
					impl.setPKValue(getDouble(request, "pKvalue1"));
					break;
				case MechRule.NO_ZWITTERIONS: 
					final boolean posRequiresH = 
							request.getParameter("posRequiresH") != null;
					impl.setFlags(posRequiresH ? MechRule.POS_BEARS_H_MASK : 0);
					break;
				case MechRule.NO_SAME_CHARGE_REACT: 
					final boolean allowHTransfers = 
							request.getParameter("allowHTransfers") != null;
					impl.setFlags(allowHTransfers ? MechRule.ALLOW_PROTON_TRANSFER : 0);
					break;
				case MechRule.RES_CONNECT: 
					omit1stStep = 
							request.getParameter("omitFirstStepRES_CONNECT") != null;
					impl.setFlags(omit1stStep ? MechRule.OMIT_1ST_STEP_MASK : 0);
					break;
				case MechRule.PKA_RULE: 
					omit1stStep = 
							request.getParameter("omitFirstStep") != null;
					final boolean omitLastStep = 
							request.getParameter("omitLastStep") != null;
					final int flag = (omitLastStep ? MechRule.OMIT_LAST_STEP_MASK : 0) 
							| (omit1stStep ? MechRule.OMIT_1ST_STEP_MASK : 0);
					impl.setFlags(flag);
					break;
			} // switch
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.MECH_PIECES_COUNT])) {
			final MechCounter impl = new MechCounter();
			impl.setComponent(getInt(request, "component"));
			impl.setOper(getInt(request, OPER));
			impl.setLimit(getInt(request, "limit"));
			impl.setDecrement(getDouble(request, "decrement"));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.SYNTH_EQUALS])) {
			final SynthEquals impl = new SynthEquals();
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setConsiderRxnCondns(isTrue(request, "considerRxnCondns"));
			final String rxnIdsStr = request.getParameter("rxnIds");
			if (!Utils.isEmpty(rxnIdsStr)) {
				newSubeval.molStruct = 
						ChemUtils.setProperty(newSubeval.molStruct, 
							Synthesis.RXN_IDS, rxnIdsStr);
			} // if there are rxnIds to store
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.SYNTH_SCHEME])) {
			final SynthScheme impl = new SynthScheme();
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setPartCredits(request.getParameter("partCredits"));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.SYNTH_SM_MADE])) {
			final SynthEfficiency impl = new SynthEfficiency();
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.SYNTH_TARGET])) {
			final SynthTarget impl = new SynthTarget();
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setCheckEnantiomer(isTrue(request, "checkEnantiomer"));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.SYNTH_ONE_RXN])) {
			final SynthOneRxn impl = new SynthOneRxn();
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setType(getInt(request, "type"));
			final String rxnIdsStr = request.getParameter("rxnIds");
			if (!Utils.isEmpty(rxnIdsStr)) {
				newSubeval.molStruct = 
						ChemUtils.setProperty(newSubeval.molStruct, 
							Synthesis.RXN_IDS, rxnIdsStr);
			} // if there are rxnIds to store
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.SYNTH_STEPS])) {
			final SynthSteps impl = new SynthSteps();
			impl.setKind(getInt(request, "kind"));
			impl.setOper(getInt(request, OPER));
			impl.setLimit(getInt(request, "limit"));
			impl.setDecrement(getDouble(request, "decrement"));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.SYNTH_SELEC])) {
			final SynthSelective impl = new SynthSelective();
			impl.setKind(getInt(request, "kind"));
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setPartCredits(request.getParameter("partCredits"));
			final String rxnIdsStr = request.getParameter("rxnIds");
			if (!Utils.isEmpty(rxnIdsStr)) {
				newSubeval.molStruct = 
						ChemUtils.setProperty(newSubeval.molStruct, 
							Synthesis.RXN_IDS, rxnIdsStr);
			} // if there are rxnIds to store
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.SYNTH_STARTERS])) {
			final SynthStart impl = new SynthStart();
			impl.setCombination(getInt(request, "combination"));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.RANK_ORDER])) {
			final RankOrder impl = new RankOrder();
			// final String selection = request.getParameter("selection");
			impl.setSelection(request.getParameter("selection"));
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setContiguous(isTrue(request, "contiguous"));
			impl.setIncreasing(isTrue(request, "increasing"));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.RANK_POSITION])) {
			final RankPosition impl = new RankPosition();
			final String selection = Utils.toString(request.getParameter("option"), 
					RankPosition.SEPARATOR, request.getParameter("rank"));
			impl.setSelection(selection);
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.CHOICE_WHICH_CHECKED])) {
			final MultipleCheck impl = new MultipleCheck();
			impl.setOper(getInt(request, OPER));
			impl.setSelection(request.getParameter("selection"));
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.CHOICE_NUM_CHECKED])) {
			final MultipleNumChosen impl = new MultipleNumChosen();
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setOper(getInt(request, OPER));
			impl.setNumChosen(getInt(request, "numChosen"));
			impl.setAmongOptions(request.getParameter("selection"));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.TEXT_WORDS])) {
			final TextWordCount impl = new TextWordCount();
			impl.setOper(getInt(request, OPER));
			impl.setAuthNum(getInt(request, NUMBER));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.TEXT_CONT])) {
			final TextContains impl = new TextContains();
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setIgnoreCase(isTrue(request, IGNORE_CASE));
			impl.setWhere(getInt(request, WHERE));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.TEXT_SEMANTICS])) {
			final TextSemantics impl = new TextSemantics();
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.NUM_IS])) {
			final NumberIs impl = new NumberIs();
			String authNum = request.getParameter("authNum").trim();
			String toler = request.getParameter(TOLERANCE).trim();
			String authExponent = request.getParameter("authExponent").trim();
			authNum = authNum.replaceAll("/", "&#47;"); 
			// Double.parseDouble(authNum);
			if (!"".equals(authExponent)) Integer.parseInt(authExponent);
			else authExponent = " ";
			if (!"".equals(toler)) Double.parseDouble(toler);
			else toler = " ";
			impl.setAuthNum(authNum);
			impl.setAuthExponent(authExponent);
			impl.setTolerance(toler);
			impl.setOper(getInt(request, OPER));
			impl.setAuthUnit(request.getParameter("authUnit"));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.NUM_SIGFIG])) {
			final NumberSigFigs impl = new NumberSigFigs();
			impl.setOper(getInt(request, OPER));
			impl.setAuthNum(getInt(request, NUMBER));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.NUM_UNIT])) {
			final NumberUnit impl = new NumberUnit();
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setUnitNums(request.getParameter("unitNums"));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.STMTS_CT])) {
			final LogicStmtsCt impl = new LogicStmtsCt();
			impl.setOper(getInt(request, OPER));
			impl.setAuthNum(getInt(request, NUMBER));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.OED_DIFF])) {
			final OEDDiff impl = new OEDDiff();
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setOper(getInt(request, OPER));
			impl.setExtent(getInt(request, "extent"));
			impl.setEnergies(getInt(request, "energies"));
			impl.setTolerance(getInt(request, TOLERANCE));
			newSubeval.codedData = impl.getCodedData();
		 } else if (matchCode.equals(CODES[EvalManager.OED_ELEC])) {
			final OEDElecCt impl = new OEDElecCt();
			impl.setOrbType(getInt(request, "orbType"));
			impl.setColumn(getInt(request, COLUMN));
			impl.setOper(getInt(request, OPER));
			impl.setNumber(getInt(request, NUMBER));
			newSubeval.codedData = impl.getCodedData();
		 } else if (matchCode.equals(CODES[EvalManager.OED_TYPE])) {
			final OEDOrbType impl = new OEDOrbType();
			impl.setOrbType(getInt(request, "orbType"));
			impl.setColumn(getInt(request, COLUMN));
			impl.setOper(getInt(request, OPER));
			impl.setNumber(getInt(request, NUMBER));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.RCD_DIFF])) {
			final RCDDiff impl = new RCDDiff();
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setOper(getInt(request, OPER));
			impl.setExtent(getInt(request, "extent"));
			impl.setEnergies(getInt(request, "energies"));
			impl.setTolerance(getInt(request, TOLERANCE));
			newSubeval.codedData = impl.getCodedData();
		 } else if (matchCode.equals(CODES[EvalManager.RCD_STATE_CT])) {
			final RCDStateCt impl = new RCDStateCt();
			impl.setWhich(getInt(request, "which"));
			impl.setLabel(getInt(request, "label"));
			impl.setMode(getInt(request, "mode"));
			impl.setColumnsStr(request.getParameter("columnsStr"));
			impl.setOper(getInt(request, OPER));
			impl.setNumber(getInt(request, NUMBER));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.TABLE_DIFF])) {
			final TableDiff impl = new TableDiff();
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setWhere(getInt(request, WHERE));
			impl.setIgnoreCase(isTrue(request, IGNORE_CASE));
			impl.setCalcGrade(request.getParameter("calcGrade") != null);
			impl.setHighlightWrong(request.getParameter("highlightWrong") != null);
			newSubeval.codedData = impl.getCodedData();
			newSubeval.molStruct = TableQ.responseToXML(request);
		} else if (matchCode.equals(CODES[EvalManager.TABLE_TEXT])) {
			final TableTextVal impl = new TableTextVal();
			impl.setRow(getInt(request, ROW));
			impl.setColumn(getInt(request, COLUMN));
			impl.setEmptyCell(getInt(request, EMPTY_CELL));
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setWhere(getInt(request, WHERE));
			impl.setIgnoreCase(isTrue(request, IGNORE_CASE));
			impl.setColorSatisfying(isTrue(request, "colorSatisfying"));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.TABLE_CT_TXT])) {
			final TableCellTextCt impl = new TableCellTextCt();
			impl.setOper(getInt(request, OPER));
			impl.setNumCells(getInt(request, "numCells"));
			impl.setRow(getInt(request, ROW));
			impl.setColumn(getInt(request, COLUMN));
			impl.setEmptyCell(getInt(request, EMPTY_CELL));
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setWhere(getInt(request, WHERE));
			impl.setIgnoreCase(isTrue(request, IGNORE_CASE));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.TBL_TXT_TXT])) {
			final TableTextText impl = new TableTextText();
			impl.setRowOper(getInt(request, ROW_OPER));
			impl.setColumnRef(getInt(request, "columnRef"));
			impl.setColumnTest(getInt(request, "columnTest"));
			impl.setEmptyCell(getInt(request, EMPTY_CELL));
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setWhere(getInt(request, WHERE));
			impl.setIgnoreCase(isTrue(request, IGNORE_CASE));
			newSubeval.molStruct = Utils.toString(newSubeval.molStruct, 
					TableTextText.TWO_STR_SEP, toCERs(request, "refString"));
			newSubeval.molName = newSubeval.molStruct;
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.TBL_NUM_TXT])) {
			final TableNumText impl = new TableNumText();
			final String authNum = request.getParameter("authNum").trim();
			Double.parseDouble(authNum);
			impl.setAuthNum(authNum);
			String toler = request.getParameter(TOLERANCE).trim();
			if (!"".equals(toler)) Double.parseDouble(toler);
			else toler = " ";
			impl.setTolerance(toler);
			impl.setRowOper(getInt(request, ROW_OPER));
			impl.setColumnRef(getInt(request, "columnRef"));
			impl.setColumnTest(getInt(request, "columnTest"));
			impl.setEmptyCell(getInt(request, EMPTY_CELL));
			impl.setNonnumeric(getInt(request, NONNUMERIC));
			impl.setOper(getInt(request, OPER));
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setWhere(getInt(request, WHERE));
			impl.setIgnoreCase(isTrue(request, IGNORE_CASE));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.TABLE_NUM])) {
			final TableNumVal impl = new TableNumVal();
			final String authNum = request.getParameter("authNum").trim();
			Double.parseDouble(authNum);
			impl.setAuthNum(authNum);
			String toler = request.getParameter(TOLERANCE).trim();
			if (!"".equals(toler)) Double.parseDouble(toler);
			else toler = " ";
			impl.setTolerance(toler);
			impl.setRow(getInt(request, ROW));
			impl.setColumn(getInt(request, COLUMN));
			impl.setEmptyCell(getInt(request, EMPTY_CELL));
			impl.setNonnumeric(getInt(request, NONNUMERIC));
			impl.setOper(getInt(request, OPER));
			impl.setColorSatisfying(isTrue(request, "colorSatisfying"));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.TABLE_CT_NUM])) {
			final TableCellNumCt impl = new TableCellNumCt();
			final String authNum = request.getParameter("authNum").trim();
			Double.parseDouble(authNum);
			impl.setAuthNum(authNum);
			String toler = request.getParameter(TOLERANCE).trim();
			if (!"".equals(toler)) Double.parseDouble(toler);
			else toler = " ";
			impl.setTolerance(toler);
			impl.setOperCells(getInt(request, "operCells"));
			impl.setNumCells(getInt(request, "numCells"));
			impl.setRow(getInt(request, ROW));
			impl.setColumn(getInt(request, COLUMN));
			impl.setEmptyCell(getInt(request, EMPTY_CELL));
			impl.setNonnumeric(getInt(request, NONNUMERIC));
			impl.setOper(getInt(request, OPER));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.TBL_TXT_NUM])) {
			final TableTextNum impl = new TableTextNum();
			final String authNum = request.getParameter("authNum").trim();
			Double.parseDouble(authNum);
			impl.setAuthNum(authNum);
			String toler = request.getParameter(TOLERANCE).trim();
			if (!"".equals(toler)) Double.parseDouble(toler);
			else toler = " ";
			impl.setTolerance(toler);
			impl.setRowOper(getInt(request, ROW_OPER));
			impl.setColumnRef(getInt(request, "columnRef"));
			impl.setColumnTest(getInt(request, "columnTest"));
			impl.setEmptyCell(getInt(request, EMPTY_CELL));
			impl.setNonnumeric(getInt(request, NONNUMERIC));
			impl.setOper(getInt(request, OPER));
			impl.setIgnoreCase(isTrue(request, IGNORE_CASE));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.TBL_NUM_NUM])) {
			final TableNumNum impl = new TableNumNum();
			final String authNumRef = request.getParameter("authNumRef").trim();
			Double.parseDouble(authNumRef);
			impl.setAuthNumRef(authNumRef);
			final String authNum = request.getParameter("authNum").trim();
			Double.parseDouble(authNum);
			impl.setAuthNum(authNum);
			String tolerRef = request.getParameter("toleranceRef").trim();
			if (!"".equals(tolerRef)) Double.parseDouble(tolerRef);
			else tolerRef = " ";
			impl.setToleranceRef(tolerRef);
			String toler = request.getParameter(TOLERANCE).trim();
			if (!"".equals(toler)) Double.parseDouble(toler);
			else toler = " ";
			impl.setTolerance(toler);
			impl.setRowOper(getInt(request, ROW_OPER));
			impl.setColumnRef(getInt(request, "columnRef"));
			impl.setColumnTest(getInt(request, "columnTest"));
			impl.setEmptyCell(getInt(request, EMPTY_CELL));
			impl.setNonnumeric(getInt(request, NONNUMERIC));
			impl.setOperRef(getInt(request, "operRef"));
			impl.setOper(getInt(request, OPER));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.CLICK_HERE])) {
			final ClickHere impl = new ClickHere();
			impl.setHowMany(getInt(request, HOW_MANY));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.CLICK_NUM])) {
			final ClickNumber impl = new ClickNumber();
			impl.setHowMany(getInt(request, HOW_MANY));
			final String authNum = request.getParameter("authNum").trim();
			String toler = request.getParameter(TOLERANCE).trim();
			Double.parseDouble(authNum);
			if (!"".equals(toler)) Double.parseDouble(toler);
			else toler = " ";
			impl.setAuthNum(authNum);
			impl.setTolerance(toler);
			impl.setOper(getInt(request, OPER));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.CLICK_TEXT])) {
			final ClickText impl = new ClickText();
			impl.setHowMany(getInt(request, HOW_MANY));
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setIgnoreCase(isTrue(request, IGNORE_CASE));
			impl.setWhere(getInt(request, WHERE));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.CLICK_LABELS_COMP])) {
			final ClickLabelsCompare impl = new ClickLabelsCompare();
			impl.setHowMany(getInt(request, HOW_MANY));
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setStartChar(MathUtils.parseInt(request.getParameter("startChar")));
			impl.setIgnoreCase(isTrue(request, IGNORE_CASE));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.CLICK_CT])) {
			final ClickCount impl = new ClickCount();
			impl.setOper(getInt(request, OPER));
			impl.setAuthNum(getInt(request, NUMBER));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.EQNS_CT])) {
			final EqnsCt impl = new EqnsCt();
			impl.setOper(getInt(request, OPER));
			impl.setAuthNum(getInt(request, NUMBER));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.EQN_VARS])) {
			final EqnVariables impl = new EqnVariables();
			impl.setOper(getInt(request, OPER));
			impl.setAuthNum(getInt(request, NUMBER));
			impl.setWhich(getInt(request, "which"));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.EQN_IS])) {
			final EqnIs impl = new EqnIs();
			impl.setWhich(getInt(request, "which"));
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setHowManySolutions(getInt(request, "howManySolutions"));
			newSubeval.codedData = impl.getCodedData();
			EquationFunctions.isValidExpression(newSubeval.molStruct);
		} else if (matchCode.equals(CODES[EvalManager.EQN_SOLVED])) {
			final EqnSolved impl = new EqnSolved();
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			impl.setMustBeReduced(isTrue(request, "mustBeReduced"));
			impl.setVariable(request.getParameter("variable"));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.EQNS_FOLLOW])) {
			final EqnsFollow impl = new EqnsFollow();
			impl.setIsPositive(isTrue(request, IS_POSITIVE));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.VECTORS_CT])) {
			final VectorsCt impl = new VectorsCt();
			impl.setOper(getInt(request, OPER));
			impl.setAuthNum(getInt(request, NUMBER));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.VECTORS_COMP])) {
			final VectorsCompare impl = new VectorsCompare();
			impl.setNoAnySum(getInt(request, "noAnySum"));
			impl.setVectorQuant(getInt(request, "vectorQuant"));
			impl.setOper(getInt(request, OPER));
			impl.setLengthTolerance(getInt(request, "lengthTolerance"));
			impl.setLengthToleranceUnit(getInt(request, "lengthToleranceUnit"));
			impl.setAngleTolerance(getInt(request, "angleTolerance"));
			newSubeval.codedData = impl.getCodedData();
		} else if (matchCode.equals(CODES[EvalManager.VECTORS_AXES])) {
			final VectorsAxes impl = new VectorsAxes();
			impl.setHowMany(getInt(request, HOW_MANY));
			impl.setDirection(getInt(request, "direction"));
			impl.setAngleTolerance(getInt(request, TOLERANCE));
			newSubeval.codedData = impl.getCodedData();
			Utils.alwaysPrint("saveEvaluator.jsp: codedData = ", newSubeval.codedData);
		} else if (matchCode.equals(CODES[EvalManager.HUMAN_REQD])) { 
			final HumanReqd impl = new HumanReqd();
			newSubeval.codedData = impl.getCodedData();
		} else {
			Utils.alwaysPrint("saveEvaluator.jsp ERROR: unknown matchCode ",
					matchCode);
			%> <jsp:forward page="../errorParam.jsp"/> <%
		} // if matchCode

		if (evalNum == 0) {
			question.addEvaluator(newEval);
		} else {
			question.setEvaluator(evalNum, subevalNum, newEval);
		}
	} catch (NumberFormatException e) {
		haveError = true;
	} catch (EquationFormatException e) {
		haveError = true;
	} // try

	final int ADD_NEW = 1;
	final int ADD_CLONE = 2;
	final int afterSave = getInt(request, "afterSave");

	final int qType = getInt(request, "qType");
%>


<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE Evaluator Editor</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<script src="../question.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
	// <!-- these files and ajax.js needed for question.js to work >
	<%@ include file="/js/marvinQuestionConstants.jsp.h" %>
	<%@ include file="/js/otherQuestionConstants.jsp.h" %>

	function finish() {
		var bld = new String.builder().
				append('loadEvaluator').
				append(getLoadEvaluatorSuffix(<%= qType %>)).
				append('.jsp?evalNum=');
		var closeMe = false;
		<% final int evalNumDestn = (evalNum == 0 
				? question.getNumEvaluators() : evalNum);
		if (haveError) {
			final boolean isEqnError = 
					matchCode.equals(CODES[EvalManager.EQN_IS]);
			final StringBuilder msgBld = Utils.getBuilder(isEqnError 
						? "The expression or formula" : "One of the numbers", 
					" you entered was formatted "
						+ "incorrectly.  ACE is returning you to the "
						+ "evaluator-editing page to correct it.");
			if (isEqnError) {
				Utils.appendTo(msgBld, " Here is the incorrectly "
							+ "formatted equation or expression; you may "
							+ "wish to copy it and paste it back into the "
							+ "textbox as a starting point:\n\n", 
						newSubeval.molStruct);
			} // if is error in equation
		%>
			alert('<%= Utils.toValidJS(msgBld.toString()) %>');
			bld.append('<%= evalNumDestn %>&evalConstant=<%= 
					Utils.indexOf(CODES, matchCode) %>)';
		<% } else if (afterSave == ADD_NEW) { %>
			bld.append('0');
		<% } else if (afterSave == ADD_CLONE) { %>
			bld.append('<%= evalNumDestn %>&cloneEdit=true');
		<% } else { %>
			closeMe = true;
		<% } // if haveError %>
		if (closeMe) {
			opener.location.href = '../question.jsp?qId=same';
			self.close();
		} else self.location.href = bld.toString();
	} // finish()
	// -->
	</script>
</head>
<body onload="finish();">
</body>
</html>
