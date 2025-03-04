<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.chem.FnalGroupDef,
	com.epoch.chem.ChemUtils,
	com.epoch.chem.MolString,
	com.epoch.evals.impl.chemEvals.*,
	com.epoch.evals.impl.chemEvals.energyEvals.*,
	com.epoch.evals.impl.chemEvals.lewisEvals.*,
	com.epoch.evals.impl.chemEvals.mechEvals.*,
	com.epoch.evals.impl.chemEvals.synthEvals.*,
	com.epoch.lewis.LewisMolecule,
	com.epoch.mechanisms.MechSet,
	com.epoch.mechanisms.MechSubstructSearch,
	com.epoch.synthesis.RxnCondition,
	com.epoch.synthesis.Synthesis,
	java.util.LinkedHashMap,
	java.util.List,
	java.util.Map"
%>

<% final String SELF = "loadEvaluatorChem.jsp"; %>

<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="loadEvaluatorJava.jsp.h" %>
<%@ include file="/js/rxnCondsJava.jsp.h" %> <% // defines rcPhrases %>
<%@ include file="/js/edJava.jsp.h" %> <% // imports energyDiagram.*  and defines edPhrases %>

<%
	request.setCharacterEncoding("UTF-8");
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	final int SKELETON_SUBSTRUCTURE = EvalManager.SKELETON_SUBSTRUCTURE;
	final int IS_OR_HAS_SIGMA_NETWORK = EvalManager.IS_OR_HAS_SIGMA_NETWORK;
	final int IS_2D_CHAIR = EvalManager.IS_2D_CHAIR;
	final int BOND_ANGLE = EvalManager.BOND_ANGLE;
	final int CHIRAL = EvalManager.CHIRAL;
	final int FUNCTIONAL_GROUP = EvalManager.FUNCTIONAL_GROUP;
	final int WEIGHT = EvalManager.WEIGHT;
	final int FORMULA_WEIGHT = EvalManager.FORMULA_WEIGHT;
	final int UNSATURATION = EvalManager.UNSATURATION;
	final int HAS_FORMULA = EvalManager.HAS_FORMULA;
	final int FORMULA_FORMAT = EvalManager.FORMULA_FORMAT;
	final int NUM_ATOMS = EvalManager.NUM_ATOMS;
	final int NUM_RINGS = EvalManager.NUM_RINGS;
	final int TOTAL_CHARGE = EvalManager.TOTAL_CHARGE;
	final int NUM_MOLECULES = EvalManager.NUM_MOLECULES;
	final int MAPPED_ATOMS = EvalManager.MAPPED_ATOMS;
	final int MAPPED_COUNT = EvalManager.MAPPED_COUNT;
	final int CONFORMATION_CHAIR = EvalManager.CONFORMATION_CHAIR;
	final int CONFORMATION_ACYCLIC = EvalManager.CONFORMATION_ACYCLIC;
	final int LEWIS_VALENCE_ELECS = EvalManager.LEWIS_VALENCE_ELECS;
	final int LEWIS_FORMAL_CHGS = EvalManager.LEWIS_FORMAL_CHGS;
	final int LEWIS_OUTER_SHELL_COUNT = EvalManager.LEWIS_OUTER_SHELL_COUNT;
	final int LEWIS_ELECTRON_DEFICIENT = EvalManager.LEWIS_ELECTRON_DEFICIENT;
	final int LEWIS_ISOMORPHIC = EvalManager.LEWIS_ISOMORPHIC;
	final int MECH_TOPOLOGY = EvalManager.MECH_TOPOLOGY;
	final int MECH_RULE = EvalManager.MECH_RULE;
	final int MECH_PIECES_COUNT = EvalManager.MECH_PIECES_COUNT;
	final int MECH_FLOWS = EvalManager.MECH_FLOWS;
	final int MECH_INIT = EvalManager.MECH_INIT;
	final int MECH_EQUALS = EvalManager.MECH_EQUALS;
	final int MECH_PRODS_STARTERS_IS = EvalManager.MECH_PRODS_STARTERS_IS;
	final int MECH_PRODS_STARTERS_PROPS = EvalManager.MECH_PRODS_STARTERS_PROPS;
	final int MECH_SUBSTRUCTURE = EvalManager.MECH_SUBSTRUCTURE;
	final int SYNTH_SCHEME = EvalManager.SYNTH_SCHEME;
	final int SYNTH_TARGET = EvalManager.SYNTH_TARGET;
	final int SYNTH_STEPS = EvalManager.SYNTH_STEPS;
	final int SYNTH_STARTERS = EvalManager.SYNTH_STARTERS;
	final int SYNTH_SM_MADE = EvalManager.SYNTH_SM_MADE;
	final int SYNTH_SELEC = EvalManager.SYNTH_SELEC;
	final int SYNTH_ONE_RXN = EvalManager.SYNTH_ONE_RXN;
	final int SYNTH_EQUALS = EvalManager.SYNTH_EQUALS;
	final int OED_DIFF = EvalManager.OED_DIFF;
	final int OED_ELEC = EvalManager.OED_ELEC;
	final int OED_TYPE = EvalManager.OED_TYPE;
	final int RCD_DIFF = EvalManager.RCD_DIFF;
	final int RCD_STATE_CT = EvalManager.RCD_STATE_CT;
	final int[] evalsUsingApplet = new int[] {
			SKELETON_SUBSTRUCTURE,
			IS_OR_HAS_SIGMA_NETWORK,
			BOND_ANGLE,
			MAPPED_ATOMS,
			MAPPED_COUNT,
			MECH_FLOWS,
			MECH_INIT,
			MECH_EQUALS,
			MECH_PRODS_STARTERS_IS,
			MECH_SUBSTRUCTURE,
			SYNTH_TARGET,
			SYNTH_STARTERS,
			SYNTH_SELEC,
			SYNTH_ONE_RXN,
			SYNTH_EQUALS};
	final boolean usesApplet = Utils.contains(evalsUsingApplet, evalConstant);

	final String rGroupsWarning = (!Question.usesSubstns(qFlags)
				? "" 
			: evalConstant == SKELETON_SUBSTRUCTURE
				? "<tr><td style=\"color:green;\">"
					+ "In an R-group question, the result of a substructure "
					+ "or skeleton search may depend on the nature of the "
					+ "particular R groups that ACE chose for the student. "
					+ "Use care in writing this evaluator.<br/><br/>"
					+ "</td></tr>"
			: evalConstant == HAS_FORMULA 
				? "<p>Omit R groups from the formula.</P>" 
			: Utils.among(evalConstant, NUM_RINGS, NUM_ATOMS)
				? "<p style=\"color:green;\">ACE assumes that every molecule "
					+ "in the response contains one instance of each R group.</P>" 
			: evalConstant == FUNCTIONAL_GROUP 
				? "<tr><td style=\"color:green;\">"
					+ "In an R-group question, the number of functional "
					+ "groups may depend on the nature of the "
					+ "particular R groups that ACE chose for the student. "
					+ "Use care in writing this evaluator.<br/><br/>"
					+ "</td></tr>"
			: "");

	final FnalGroupDef[] sortedGroups = FnalGroupDef.getAllGroups();

	// synthesis-specific variables
	Map<Integer, String> reactionNamesByIds = null;
	String rxnIdsStr = null;
	String[] chosenRxns = new String[0];
	final boolean onlyOneRxnCondn = 
			Utils.among(evalConstant, SYNTH_ONE_RXN, SYNTH_SELEC);
	if (Utils.among(evalConstant, SYNTH_ONE_RXN, SYNTH_SELEC, SYNTH_EQUALS)) {
		rxnIdsStr = Synthesis.getRxnConditions(inputSubeval.molStruct);
		if (!Utils.isEmpty(rxnIdsStr)) {
			chosenRxns = rxnIdsStr.split(Synthesis.RXN_ID_SEP);
		} // if there are selected synthesis reactions
		reactionNamesByIds = RxnCondition.getRxnNamesKeyedByIds();
	} // if may use a synthesis
	final boolean usesRxnConds = 
			Utils.among(evalConstant, SYNTH_SELEC, SYNTH_ONE_RXN, SYNTH_EQUALS);
	final int[] synthErrorCodes = 
			SynthPartCredits.getSynthErrorCodes(evalConstant);

	// energy-diagram-specific variables
	final boolean labelOrbitals = Question.labelOrbitals(qFlags); 
	final OED oed = (!Question.isOED(qType) ? null
			: Utils.isEmpty(qData) ? new OED() : new OED(qData));
	final RCD rcd = (!Question.isRCD(qType) ? null
			: Utils.isEmpty(qData) ? new RCD() : new RCD(qData));
	final boolean isED = Utils.among(evalConstant, OED_DIFF, RCD_DIFF);

	if (usesApplet) {
		inputSubeval.molStruct = MolString.updateFormat(inputSubeval.molStruct);
	} // if data is a molecule

	final String[] examples = new String[] {
			"<?xml version=\"1.0\" ?>\n"
			+ "<cml>\n"
			+ "<MDocument>\n"
			+ "  <MEFlow id=\"o1\" arcAngle=\"-229.1494584109317\" headSkip=\"0.25\"\n"
			+ "          headLength=\"0.5\" headWidth=\"0.4\" tailSkip=\"0.15\">\n"
			+ "    <MAtomSetPoint atomRefs=\"m1.a1 m1.a5\" />\n"
			+ "    <MAtomSetPoint atomRefs=\"m1.a1\" />\n"
			+ "  </MEFlow>\n"
			+ "  <MEFlow id=\"o2\" arcAngle=\"150.0\" headSkip=\"0.15\" headLength=\"0.5\"\n"
			+ "          headWidth=\"0.4\" tailSkip=\"0.25\">\n"
			+ "    <MEFlowBasePoint atomRef=\"m1.a3\" />\n"
			+ "    <MAtomSetPoint atomRefs=\"m1.a3 m1.a8\" weights=\"0.25 0.75\" />\n"
			+ "  </MEFlow>\n"
			+ "  <MPolyline id=\"o3\" headLength=\"0.8\" headWidth=\"0.5\">\n"
			+ "    <MPoint x=\"-0.9900000095367432\" y=\"3.134999990463257\" />\n"
			+ "    <MPoint x=\"1.1557050450366873\" y=\"3.134999990463256\" />\n"
			+ "  </MPolyline>\n"
			+ "  <MChemicalStruct>\n"
			+ "    <molecule molID=\"m1\">\n"
			+ "      <atomArray\n"
			+ "          atomID=\"a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13\"\n"
			+ "          elementType=\"O C O C H C C H R C O C C\"\n"
			+ "          formalCharge=\"1 0 0 0 0 0 0 1 0 0 1 0 0\"\n"
			+ "          sgroupRef=\"0 0 0 0 0 0 0 0 sg1 0 0 0 0\"\n"
			+ "          x2=\"-5.389999866485596 -4.619999866485595 -3.849999866485595 -6.929999866485596 -5.389999866485596 -5.95367898831363 -3.2863207446575595 -1.649999976158142 3.4650003147125243 4.235000314712525 5.005000314712525 2.90132119288449 5.5686794365405605\"\n"
			+ "          y2=\"3.960000038146972 2.626320916318937 3.960000038146972 3.960000038146972 5.500000038146972 1.8563209163189365 1.856320916318937 4.840000152587891 3.926576547152637 2.5928974253246015 3.926576547152637 1.822897425324601 1.8228974253246015\"\n"
			+ "          />\n"
			+ "      <bondArray>\n"
			+ "        <bond atomRefs2=\"a1 a2\" order=\"1\" />\n"
			+ "        <bond atomRefs2=\"a2 a3\" order=\"1\" />\n"
			+ "        <bond atomRefs2=\"a1 a4\" order=\"1\" />\n"
			+ "        <bond atomRefs2=\"a1 a5\" order=\"1\" />\n"
			+ "        <bond atomRefs2=\"a2 a6\" order=\"1\" />\n"
			+ "        <bond atomRefs2=\"a2 a7\" order=\"1\" />\n"
			+ "        <bond atomRefs2=\"a9 a10\" order=\"1\" />\n"
			+ "        <bond atomRefs2=\"a10 a11\" order=\"1\" />\n"
			+ "        <bond atomRefs2=\"a10 a12\" order=\"1\" />\n"
			+ "        <bond atomRefs2=\"a10 a13\" order=\"1\" />\n"
			+ "      </bondArray>\n"
			+ "      <molecule id=\"sg1\" role=\"SuperatomSgroup\" title=\"MeO\" rightName=\"OMe\" molID=\"m2\">\n"
			+ "        <atomArray\n"
			+ "            atomID=\"a14 a15\"\n"
			+ "            elementType=\"O C\"\n"
			+ "            attachmentPoint=\"1 0\"\n"
			+ "            sgroupAttachmentPoint=\"1 0\"\n"
			+ "            x2=\"8.635000486373901 10.175000486373902\"\n"
			+ "            y2=\"7.919999837875366 7.919999837875366\"\n"
			+ "            />\n"
			+ "        <bondArray>\n"
			+ "          <bond atomRefs2=\"a15 a14\" order=\"1\" />\n"
			+ "        </bondArray>\n"
			+ "      </molecule>\n"
			+ "    </molecule>\n"
			+ "  </MChemicalStruct>\n"
			+ "</MDocument>\n"
			+ "</cml>\n", 
			"<?xml version=\"1.0\" ?>\n"
			+ "<cml>\n"
			+ "<MDocument atomSetRGB=\"0:D\" bondSetRGB=\"0:N,1:N\">\n"
			+ "  <MEFlow id=\"o1\" arcAngle=\"171.0\" headSkip=\"0.15\" headLength=\"0.5\"\n"
			+ "          headWidth=\"0.4\" tailSkip=\"0.15\">\n"
			+ "    <MAtomSetPoint atomRefs=\"m1.a1 m1.a3\" />\n"
			+ "    <MAtomSetPoint atomRefs=\"m1.a3 m1.a2\" weights=\"0.25 0.75\" />\n"
			+ "  </MEFlow>\n"
			+ "  <MEFlow id=\"o2\" arcAngle=\"221.14222574300464\" headSkip=\"0.15\"\n"
			+ "          headLength=\"0.5\" headWidth=\"0.4\" tailSkip=\"0.15\">\n"
			+ "    <MAtomSetPoint atomRefs=\"m1.a2 m1.a4\" />\n"
			+ "    <MAtomSetPoint atomRefs=\"m1.a4 m1.a1\" weights=\"0.25 0.75\" />\n"
			+ "  </MEFlow>\n"
			+ "  <MPolyline id=\"o3\" headLength=\"0.8\" headWidth=\"0.5\">\n"
			+ "    <MPoint x=\"2.309999942779541\" y=\"2.859999895095825\" />\n"
			+ "    <MPoint x=\"4.125\" y=\"2.859999895095825\" />\n"
			+ "  </MPolyline>\n"
			+ "  <MChemicalStruct>\n"
			+ "    <molecule molID=\"m1\">\n"
			+ "      <atomArray\n"
			+ "          atomID=\"a1 a2 a3 a4 a5 a6 a7 a8\"\n"
			+ "          elementType=\"C C C C C C C C\"\n"
			+ "          mrvAlias=\"B C A D C B A D\"\n"
			+ "          x2=\"-1.2100000381469727 0.3299999618530274 -1.9800000381469722 1.0999999618530276 7.42500020980835 5.88500020980835 8.195000209808349 5.1150002098083505\"\n"
			+ "          y2=\"2.75 2.75 4.083679121828036 1.4163208781719645 2.9700000286102295 2.9700000286102295 4.303679150438265 1.636320906782194\"\n"
			+ "          />\n"
			+ "      <bondArray>\n"
			+ "        <bond atomRefs2=\"a1 a2\" order=\"1\" />\n"
			+ "        <bond atomRefs2=\"a1 a3\" order=\"1\" />\n"
			+ "        <bond atomRefs2=\"a2 a4\" order=\"1\" />\n"
			+ "        <bond atomRefs2=\"a5 a6\" order=\"1\" mrvSetSeq=\"1\" />\n"
			+ "        <bond atomRefs2=\"a5 a7\" order=\"1\" mrvSetSeq=\"1\" />\n"
			+ "        <bond atomRefs2=\"a6 a8\" order=\"1\" mrvSetSeq=\"1\" />\n"
			+ "      </bondArray>\n"
			+ "    </molecule>\n"
			+ "  </MChemicalStruct>\n"
			+ "</MDocument>\n"
			+ "</cml>\n", 
			"<?xml version=\"1.0\" ?>\n"
			+ "<cml>\n"
			+ "<MDocument>\n"
			+ "  <MPolyline id=\"o1\" headLength=\"0.8\" headWidth=\"0.5\">\n"
			+ "    <MPoint x=\"6.0500006675720215\" y=\"9.569999694824219\" />\n"
			+ "    <MPoint x=\"7.8650007247924805\" y=\"9.569999694824219\" />\n"
			+ "  </MPolyline>\n"
			+ "  <MEFlow id=\"o2\" arcAngle=\"-117.54325450126886\" headSkip=\"0.15\"\n"
			+ "          headLength=\"0.5\" headWidth=\"0.4\" tailSkip=\"0.15\">\n"
			+ "    <MAtomSetPoint atomRefs=\"m1.a1 m1.a3\" />\n"
			+ "    <MAtomSetPoint atomRefs=\"m1.a3 m1.a4\" weights=\"0.25 0.75\" />\n"
			+ "  </MEFlow>\n"
			+ "  <MEFlow id=\"o3\" arcAngle=\"-141.47948059838973\" headSkip=\"0.15\"\n"
			+ "          headLength=\"0.5\" headWidth=\"0.4\" tailSkip=\"0.15\">\n"
			+ "    <MAtomSetPoint atomRefs=\"m1.a2 m1.a4\" />\n"
			+ "    <MAtomSetPoint atomRefs=\"m1.a2 m1.a1\" weights=\"0.25 0.75\" />\n"
			+ "  </MEFlow>\n"
			+ "  <MChemicalStruct>\n"
			+ "    <molecule molID=\"m1\">\n"
			+ "      <atomArray\n"
			+ "          atomID=\"a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16\"\n"
			+ "          elementType=\"C C C C C C C C C C C C C C C C\"\n"
			+ "          mrvAlias=\"B D A C B A D C B C A D C B A D\"\n"
			+ "          x2=\"2.310000568628311 5.005000569820403 1.3750005471706395 4.0700005483627315 9.9550007635355 9.020000742077828 11.93500108897686 11.000001067519188 2.4337500810623167 3.9737500810623168 1.6637500810623171 4.743750081062317 11.06875032901764 9.52875032901764 11.838750329017639 8.75875032901764\"\n"
			+ "          y2=\"8.690000057220459 8.690000057220459 10.628679198121981 10.628679198121981 8.655660667967819 10.594339808869341 8.655660667967819 10.594339808869341 3.3550001084804535 3.3550001084804535 4.688679230308489 2.0213209866524178 3.575000137090683 3.575000137090683 4.908679258918719 2.2413210152626473\"\n"
			+ "          />\n"
			+ "      <bondArray>\n"
			+ "        <bond atomRefs2=\"a1 a3\" order=\"1\" />\n"
			+ "        <bond atomRefs2=\"a2 a4\" order=\"1\" />\n"
			+ "        <bond atomRefs2=\"a6 a8\" order=\"1\" />\n"
			+ "        <bond atomRefs2=\"a5 a7\" order=\"1\" />\n"
			+ "        <bond atomRefs2=\"a9 a10\" order=\"1\" />\n"
			+ "        <bond atomRefs2=\"a9 a11\" order=\"1\" />\n"
			+ "        <bond atomRefs2=\"a10 a12\" order=\"1\" />\n"
			+ "        <bond atomRefs2=\"a13 a14\" order=\"1\" />\n"
			+ "        <bond atomRefs2=\"a13 a15\" order=\"1\" />\n"
			+ "        <bond atomRefs2=\"a14 a16\" order=\"1\" />\n"
			+ "      </bondArray>\n"
			+ "    </molecule>\n"
			+ "  </MChemicalStruct>\n"
			+ "  <MEFlow id=\"o5\" arcAngle=\"171.0\" headSkip=\"0.15\" headLength=\"0.5\"\n"
			+ "          headWidth=\"0.4\" tailSkip=\"0.15\">\n"
			+ "    <MAtomSetPoint atomRefs=\"m1.a9 m1.a11\" />\n"
			+ "    <MAtomSetPoint atomRefs=\"m1.a11 m1.a10\" weights=\"0.25 0.75\" />\n"
			+ "  </MEFlow>\n"
			+ "  <MEFlow id=\"o6\" arcAngle=\"221.14222574300464\" headSkip=\"0.15\"\n"
			+ "          headLength=\"0.5\" headWidth=\"0.4\" tailSkip=\"0.15\">\n"
			+ "    <MAtomSetPoint atomRefs=\"m1.a10 m1.a12\" />\n"
			+ "    <MAtomSetPoint atomRefs=\"m1.a12 m1.a9\" weights=\"0.25 0.75\" />\n"
			+ "  </MEFlow>\n"
			+ "  <MPolyline id=\"o7\" headLength=\"0.8\" headWidth=\"0.5\">\n"
			+ "    <MPoint x=\"5.95375006198883\" y=\"3.4650000035762787\" />\n"
			+ "    <MPoint x=\"7.768750119209289\" y=\"3.4650000035762787\" />\n"
			+ "  </MPolyline>\n"
			+ "  <MTextBox id=\"o8\" toption=\"NOROT\" fontScale=\"10.0\" halign=\"LEFT\"\n"
			+ "            valign=\"TOP\" autoSize=\"false\">\n"
			+ "    <Field name=\"text\"><![CDATA[sigma-bond metathesis  ]]></Field>\n"
			+ "    <MPoint x=\"3.3550004959106445\" y=\"7.425000190734863\" />\n"
			+ "    <MPoint x=\"10.870200495910643\" y=\"7.425000190734869\" />\n"
			+ "    <MPoint x=\"10.870200495910643\" y=\"6.62420019073487\" />\n"
			+ "    <MPoint x=\"3.355000495910641\" y=\"6.624200190734863\" />\n"
			+ "  </MTextBox>\n"
			+ "  <MTextBox id=\"o9\" toption=\"NOROT\" fontScale=\"10.0\" halign=\"LEFT\"\n"
			+ "            valign=\"TOP\" autoSize=\"false\">\n"
			+ "    <Field name=\"text\"><![CDATA[dyotropic rearrangement]]></Field>\n"
			+ "    <MPoint x=\"3.5199999809265137\" y=\"0.9900000244379044\" />\n"
			+ "    <MPoint x=\"10.973599980926515\" y=\"0.9900000244379044\" />\n"
			+ "    <MPoint x=\"10.973599980926515\" y=\"0.18920002443790374\" />\n"
			+ "    <MPoint x=\"3.5199999809265137\" y=\"0.18920002443790374\" />\n"
			+ "  </MTextBox>\n"
			+ "</MDocument>\n"
			+ "</cml>\n", 
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
			+ "<cml xmlns=\"http://www.chemaxon.com\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.chemaxon.com/marvin/schema/mrvSchema_16_02_15.xsd\" version=\"ChemAxon file format v16.02.15, generated by v16.8.8.0\">"
			+ "<MDocument>"
			+ "  <MEFlow id=\"o1\" arcAngle=\"-246.27769585641795\" headLength=\"0.6\" headWidth=\"0.5\" baseElectronContainerIndex=\"-1\" baseElectronIndexInContainer=\"0\">"
			+ "    <MAtomSetPoint atomRefs=\"m1.a1 m1.a3\"/>"
			+ "    <MAtomSetPoint atomRefs=\"m1.a1 m1.a2\"/>"
			+ "  </MEFlow>"
			+ "  <MEFlow id=\"o2\" arcAngle=\"-274.7563948387693\" headLength=\"0.6\" headWidth=\"0.5\" baseElectronContainerIndex=\"-1\" baseElectronIndexInContainer=\"0\">"
			+ "    <MAtomSetPoint atomRefs=\"m1.a2 m1.a8\"/>"
			+ "    <MAtomSetPoint atomRefs=\"m1.a8 m1.a3\"/>"
			+ "  </MEFlow>"
			+ "  <MEFlow id=\"o3\" arcAngle=\"270.0\" headSkip=\"0.6\" headLength=\"0.6\" headWidth=\"0.5\" tailSkip=\"0.38\" baseElectronContainerIndex=\"-1\" baseElectronIndexInContainer=\"0\">"
			+ "    <MAtomSetPoint atomRefs=\"m1.a6 m1.a7\"/>"
			+ "    <MAtomSetPoint atomRefs=\"m1.a6\"/>"
			+ "  </MEFlow>"
			+ "  <MEFlow id=\"o4\" arcAngle=\"270.0\" headLength=\"0.6\" headWidth=\"0.5\" baseElectronContainerIndex=\"0\" baseElectronIndexInContainer=\"-1\">"
			+ "    <MEFlowBasePoint atomRef=\"m1.a4\"/>"
			+ "    <MAtomSetPoint atomRefs=\"m1.a4 m1.a7\"/>"
			+ "  </MEFlow>"
			+ "  <MChemicalStruct>"
			+ "    <molecule molID=\"m1\">"
			+ "      <atomArray atomID=\"a1 a2 a3 a4 a5 a6 a7 a8\" elementType=\"O C H O C O H C\" formalCharge=\"0 0 0 0 0 1 0 0\" lonePair=\"2 0 0 2 0 2 0 0\" x2=\"-10.677170134985541 -9.343491013157506 -9.677170134985541 -4.635503468318875 -3.3967090011928995 -2.0745812007335918 -3.043734602950341 -8.00981189132947\" y2=\"8.444955081456587 7.674955081456588 9.984955081456587 8.542176773859744 7.731067075957888 8.62551010719308 9.82231488783681 8.444955081456587\"/>"
			+ "      <bondArray>"
			+ "        <bond id=\"b1\" atomRefs2=\"a1 a2\" order=\"1\"/>"
			+ "        <bond id=\"b2\" atomRefs2=\"a1 a3\" order=\"1\"/>"
			+ "        <bond id=\"b3\" atomRefs2=\"a4 a5\" order=\"1\"/>"
			+ "        <bond id=\"b4\" atomRefs2=\"a5 a6\" order=\"1\"/>"
			+ "        <bond id=\"b5\" atomRefs2=\"a6 a7\" order=\"1\"/>"
			+ "        <bond id=\"b6\" atomRefs2=\"a2 a8\" order=\"2\"/>"
			+ "      </bondArray>"
			+ "    </molecule>"
			+ "  </MChemicalStruct>"
			+ "</MDocument>"
			+ "</cml>"
			};
	final String APPLET_NAME = "evalApplet";
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>

<% if (usesApplet) { %>
	<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<% } // if usesApplet %>
<% if (Utils.among(evalConstant, FUNCTIONAL_GROUP, MECH_PRODS_STARTERS_PROPS)) { %>
	<script src="<%= pathToRoot %>js/fnalGroups.js" type="text/javascript"></script>
<% } else if (evalConstant == LEWIS_ISOMORPHIC) { %>
	<script src="<%= pathToRoot %>js/offsets.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/position.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/svgGraphics.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/wz_jsgraphics.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<% } else if (usesRxnConds) { %>
	<script src="<%= pathToRoot %>js/rxnCondsEditor.js" type="text/javascript"></script>
<% } else if (isED) { %>
	<script src="<%= pathToRoot %>js/oedAndRcd.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/<%= evalConstant == OED_DIFF ? "oed" : "rcd" %>.js" 
			type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/position.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/wz_jsgraphics.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<% } // if evalConstant %>
	<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/lewisJS.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/3DTemplatesMJS.js" type="text/javascript"></script>
	<script src="https://marvinjs.chemicalize.com/v1/<%= 
			AppConfig.marvinJSLicense %>/client-settings.js"></script>
	<script src="https://marvinjs.chemicalize.com/v1/client.js"></script>

<%@ include file="loadEvaluatorJS.jsp.h" %>

<script type="text/javascript">
	// <!-- >

	<% if (usesApplet) { %> 
		function loadSelections() {
			var selectionsJSO = { 'atoms': 
					document.evaluatorForm.selectionsStr
					? document.evaluatorForm.selectionsStr.value : ''};
			marvinSketcherInstances['<%= APPLET_NAME %>'].
					setSelection(selectionsJSO);
		} // loadSelections()
	<% } // if usesApplet %>

	<% if (usesRxnConds) { %>

	function initSynthesis() {
		constants = new Array();
		constants[NO_REAGENTS] = '<%= RxnCondition.NO_REAGENTS %>';
		constants[RXN_ID_SEP] = '<%= Synthesis.RXN_ID_SEP %>';
		constants[RXN_IDS] = '<%= Synthesis.RXN_IDS %>';
		constants[CLICK_HERE] = '<%= Utils.toValidJS(rcPhrases.get(CLICK_RXN)) %>';
		constants[INSERT_HERE] = '';
		constants[ADD_1ST] = '<%= evalConstant == SYNTH_EQUALS
				? Utils.toValidJS(rcPhrases.get(ADD_1ST)) 
				: "Any reaction conditions permissible" %>';
		constants[RXN_CONDN] = '<%= Utils.toValidJS(rcPhrases.get(RXN_CONDN)) %>';
		constants[PATH_TO_CHOOSE_RXN_CONDS_USER] = '<%= pathToChooseRxnCondsUser %>';
		constants[REMOVE] = '<%= Utils.toValidJS(rcPhrases.get(REMOVE)) %>';
		constants[AFTER_HERE] = '<%= rcPhrases.get(AFTER_HERE) == null ? ""
				: Utils.toValidJS(rcPhrases.get(AFTER_HERE)) %>';
		initRxnConds(constants);
		<% final int[] allRxnIds = RxnCondition.getAllReactionIds();
		for (final int rxnId : allRxnIds) { %>
			setRxnName(<%= rxnId %>, '<%= Utils.toValidJS(
					Utils.toDisplay(reactionNamesByIds.get(
						Integer.valueOf(rxnId)))) %>');
		<% } // for each rxn id
		final int numRxns = (onlyOneRxnCondn && chosenRxns.length > 1
				? 1 : chosenRxns.length);
		for (final String chosenRxn : chosenRxns) { %>
			setChosenRxn(<%= chosenRxn %>); 
		<% } // for each initial reaction condition %>
		writeRxnConds(<%= onlyOneRxnCondn %>);
	} // initSynthesis()

	<% } // SYNTH_SELEC or SYNTH_ONE_RXN or SYNTH_EQUALS
	if (evalConstant == LEWIS_ISOMORPHIC) { %>

	function initLewis() {
		initLewisConstants(
				[<%= LewisMolecule.CANVAS_WIDTH %>, 
					<%= LewisMolecule.CANVAS_HEIGHT %>],
				<%= LewisMolecule.MARVIN_WIDTH %>, 
				['<%= LewisMolecule.PAIRED_ELECS %>',
					'<%= LewisMolecule.UNPAIRED_ELECS %>',
					'<%= LewisMolecule.UNSHARED_ELECS %>' ],
				'<%= LewisMolecule.LEWIS_PROPERTY %>',
				'<%= LewisMolecule.HIGHLIGHT %>',
				'Enter an element symbol.',
				'There is no such element. Please try again.',
				'Other');
		initLewisGraphics('<%= pathToRoot %>', 
				'lewisJSCanvas', 
				'lewisJSToolbars');
		parseLewisMRV('<%= Utils.toValidJS(inputSubeval.molStruct) %>');
	} // initLewis()

	<% } // LEWIS_ISOMORPHIC 
	if (evalConstant == NUM_MOLECULES) { %>

	function toggleFlagsSetter() {
		var distinct = document.evaluatorForm.distinct.value;
		setVisibility('flagsSetter', distinct === 'true' ? 'visible' : 'hidden');
	} // toggleFlagsSetter() 

	<% } // NUM_MOLECULES 
	if (evalConstant == UNSATURATION) { %>

	function changeNumberVisibility() {
		var oper = parseInt(document.evaluatorForm.oper.value);
		setVisibility('unsatIndexCell', 
				oper < <%= UnsaturIndex.SYMBOLS.length %> ? 'visible' : 'hidden'); // <!-- >
	} // changeNumberVisibility() 

	<% } // UNSATURATION 
	if (evalConstant == MAPPED_ATOMS) { %>

	function changeSelectText() {
		var SELECTED = '<%= SELECTED %>';
		var isPositiveVal = document.evaluatorForm.isPositive.value;
		var patternOnly = (document.evaluatorForm.patternOnly.value === 'true');
		var verb = (patternOnly ? 'is' : 'are');
		var isPositive = (isPositiveVal === 'true');
		var bld = new String.builder().
				append('<select name="isPositive" '
					+ 'onchange="changeSelectText()">'
					+ '<option value="true" ');
		if (isPositive) bld.append(SELECTED);
		bld.append('>').append(verb).append('<\/option><option value="false" ');
		if (!isPositive) bld.append(SELECTED);
		bld.append('>').append(verb).append(' not<\/option><\/select>');
		setInnerHTML('isPosSelector', bld.toString());
	} // changeSelectText()

	<% } // MAPPED_ATOMS
	if (Utils.among(evalConstant, MECH_PIECES_COUNT, SYNTH_STEPS)) { %>

	function changeDecrementCell() {
		var form = document.evaluatorForm;
		var oper = form.oper.value;
		var calculateGrade = ['<%= CompareNums.GREATER %>', 
				'<%= CompareNums.NOT_LESS %>'].contains(oper);
		setVisibility('decrementCell', calculateGrade ? 'visible' : 'hidden');
		var decrement = parseFloat(form.decrement.value);
		toggleGradeSelector(!calculateGrade || decrement == 0.0);
	} // changeDecrementCell()

	<% } // MECH_PIECES_COUNT or SYNTH_STEPS
	if (Utils.among(evalConstant, SYNTH_SCHEME, SYNTH_SELEC)) { %>

	function populatePartCreditCell() {
		var partCreditBld = new String.builder();
		var kind = document.evaluatorForm.kind;
		<% Map<Integer, Integer> partCreditsMap = 
				new LinkedHashMap<Integer, Integer>();
		if (evalConstant == SYNTH_SELEC) {
			if (useInput) { // existing evaluator
				final SynthSelective impl = 
						new SynthSelective(inputSubeval.codedData);
				partCreditsMap = impl.getPartCreditsMap();
			} // useInput %>
			setInnerHTML('synthSelectiveGradingOpts',
					' <a href="#SynthSelectiveBottom">Grading options.</a>');
			partCreditBld.append('<hr\/>');
		<% } else if (useInput) {
			final SynthScheme impl = 
					new SynthScheme(inputSubeval.codedData);
			partCreditsMap = impl.getPartCreditsMap();
		} // if evalConstant %>
		partCreditBld.append('<span style="color:green;">'
				+ 'Enter partial credit values, as integers between 0 and 100, '
				+ 'that you wish to assign for particular kinds of errors. '
				+ 'Any value entered here overrides any '
				+ 'partial credit value entered above.');
		partCreditBld.append('<\/span><table>');
		<% for (final int errorCode : synthErrorCodes) { 
			final Integer partCreditObj = partCreditsMap.get(errorCode); %>
			if (<%= evalConstant == SYNTH_SCHEME %>
					|| (kind.value === '<%= SynthSelective.ANY %>'
						&& <%= Utils.among(errorCode, 
							SynthPartCredits.TOO_MANY_REACTANTS,
							SynthPartCredits.NO_RXN_PRODUCTS,
							SynthPartCredits.UNSELECTIVE) %>)
					|| (kind.value === '<%= SynthSelective.DIASTEREO %>'
						&& <%= Utils.among(errorCode, 
							SynthPartCredits.TOO_MANY_REACTANTS,
							SynthPartCredits.NO_RXN_PRODUCTS,
							SynthPartCredits.UNDIASTEREOSELECTIVE_SHOWN,
							SynthPartCredits.UNDIASTEREOSELECTIVE_NOT_SHOWN,
							SynthPartCredits.UNDIASTEREOSELECTIVE_NOT_DISTING) %>)
					|| (kind.value === '<%= SynthSelective.ENANTIO %>'
						&& <%= Utils.among(errorCode, 
							SynthPartCredits.TOO_MANY_REACTANTS,
							SynthPartCredits.NO_RXN_PRODUCTS,
							SynthPartCredits.UNENANTIOSELECTIVE_SHOWN,
							SynthPartCredits.UNENANTIOSELECTIVE_NOT_SHOWN,
							SynthPartCredits.UNENANTIOSELECTIVE_NOT_DISTING) %>)) {
				partCreditBld.append('<tr><td style="width:20%;">'
						+ '<input type="text" size="3" '
						+ 'name="creditValue<%= errorCode %>" '
						+ 'value="<%= partCreditObj != null
							? partCreditObj : "" %>"\/>%<\/td>'
						+ '<td><%= Utils.toValidJS(
							SynthPartCredits.getSynthErrorDescription(errorCode)) %>'
						+ '<\/td><\/tr>');
			} // if should display option
		<% } // for each kind of error %>
		partCreditBld.append('<\/table>');
		setInnerHTML('partCreditCell', partCreditBld.toString());
	} // populatePartCreditCell()

	<% } // SYNTH_SCHEME or SYNTH_SELEC
	if (Utils.among(evalConstant, HAS_FORMULA, NUM_ATOMS, NUM_RINGS, 
			FUNCTIONAL_GROUP, TOTAL_CHARGE)) { %>

	function changeCompareSelector() {
		var countEach = document.evaluatorForm.countEach.value;
		setVisibility('compareSelector', countEach.substring(0, 4) === 'each' 
				? 'visible' : 'hidden');
	} // changeCompareSelector()

	<% } // HAS_FORMULA, NUM_ATOMS, NUM_RINGS, FUNCTIONAL_GROUP, TOTAL_CHARGE
	if (Utils.among(evalConstant, FUNCTIONAL_GROUP, MECH_PRODS_STARTERS_PROPS)) { %>
	
		var showCategoryWarning = true;

	<% } // FUNCTIONAL_GROUP or MECH_PRODS_STARTERS_PROPS
	if (evalConstant == MECH_PRODS_STARTERS_IS) { %>

	function changeProdStart() {
		var prodOrStart = document.evaluatorForm.productOrStart.value;
		var options = document.getElementById('combination').options;
		for (var optNum = 0; optNum < options.length; optNum++) { // <!-- >
			var option = options[optNum];
			if (prodOrStart === '<%= MechProdStartIs.START %>') {
				option.text = option.text.replace(
						/<%= MechProdStartIs.PROD_START_ENGL[MechProdStartIs.PRODUCT] %>/g, 
						'<%= MechProdStartIs.PROD_START_ENGL[MechProdStartIs.START] %>');
			} else {
				option.text = option.text.replace(
						/<%= MechProdStartIs.PROD_START_ENGL[MechProdStartIs.START] %>/g, 
						'<%= MechProdStartIs.PROD_START_ENGL[MechProdStartIs.PRODUCT] %>');
			}
		} // for each select option's text
	} // changeProdStart()

	<% } // MECH_PRODS_STARTERS_IS
	if (isED) { %>

	function initED() {
		setEDConstants(new Array('<%= EDiagram.DIAGRAM_TAG %>',
				'<%= EDiagram.IS_OED_TAG %>',
				'<%= EDiagram.CELL_TAG %>',
				'<%= EDiagram.LINE_TAG %>',
				'<%= DiagramCell.ROW_TAG %>',
				'<%= DiagramCell.COLUMN_TAG %>',
				'<%= DiagramCell.LABEL_TAG %>',
				'<%= CellsLine.ENDPT_TAG %>'));
		var phrases = new Array();
		phrases[REFRESH_BUTTON] = '<%= Utils.toValidJS(makeButton(
				edPhrases.get(REFRESH_BUTTON), "updateCanvas();")) %>';
		phrases[DELETE_BUTTON] = '<%= Utils.toValidJS(makeButton(
				edPhrases.get(DELETE_BUTTON), "clearSelected();")) %>';
		phrases[DROP_HERE] = '<%= Utils.toValidJS(edPhrases.get(DROP_HERE)) %>';
		<% final boolean isOED = evalConstant == OED_DIFF;
		int numRows = 0;
		int numCols = 0;
		List<CellsLine> conLines;
		final String[] labels = (isOED ? oed.getLabels() : rcd.getLabels());
		for (int lblNum = 0; lblNum < labels.length; lblNum++) { %>
			setLabel('<%= Utils.toDisplay(labels[lblNum]) %>');
		<% } // for each label
		if (isOED) { 
			numRows = oed.getNumRows();
			numCols = oed.getNumCols();
			conLines = oed.getLines();
			if ((haveResp || useInput) && !Utils.isEmpty(inputSubeval.molStruct)) {
				oed.setOrbitals(inputSubeval.molStruct);
			}
			final int numTypes = Orbital.getNumTypes();
			for (int orbType = 1; orbType < numTypes; orbType++) { %>
				setOrbPopupName('<%= Utils.toValidJS(Orbital.getPopupMenuName(orbType)) %>');
			<% } // for each orbital type %>
			phrases[CLICK_OED] = '<%= Utils.toValidJS(edPhrases.get(CLICK_OED)) %>';
			phrases[ADD] = '<%= Utils.toValidJS(edPhrases.get(ADD)) %> ';
			phrases[ORBS_OF_TYPE] = ' <%= Utils.toValidJS(edPhrases.get(ORBS_OF_TYPE)) %> ';
			var strConstants = new Array();
			strConstants[OCCUP_SEP] = '<%= OEDCell.OCCUP_SEP %>';
			strConstants[ORBS_TYPE_TAG] = '<%= OEDCell.ORBS_TYPE_TAG %>';
			strConstants[OCCUPS_TAG] = '<%= OEDCell.OCCUPS_TAG %>';
			strConstants[OCCUP_TAG] = '<%= OEDCell.OCCUP_TAG %>';
			setOEDConstants(strConstants, phrases, <%= labelOrbitals %>);
		<% } else {
			numRows = rcd.getNumRows();
			numCols = rcd.getNumCols();
			conLines = rcd.getOrigLines();
			if ((haveResp || useInput) && !Utils.isEmpty(inputSubeval.molStruct)) {
				rcd.setStates(inputSubeval.molStruct, !RCD.THROW_IT);
			} %>
			phrases[CLICK_RCD] = '<%= Utils.toValidJS(edPhrases.get(CLICK_RCD)) %>';
			setRCDConstants(phrases);
		<% } // if isOED %>
		initGraphics(0);
		<% for (int rNum = 0; rNum < numRows; rNum++) {
			for (int cNum = 0; cNum < numCols; cNum++) { 
				final int row = numRows - rNum; 
				final int col = cNum + 1; 
				if (isOED) {
					final OEDCell cell = oed.getCell(row, col);
					if (cell.hasOrbitals()) { %>
						drop(<%= row %>, <%= col %>,
								'<%= cell.getOrbitalsType() %>', 
								'<%= Utils.toValidJS(cell.getOccupancies()) %>',
								'<%= cell.getLabel() %>');
					<% } else { %>
						clearMe(<%= row %>, <%= col %>);
					<% } // if there are orbitals in this cell
				} else { // isRCD
					if (rcd.isOccupied(row, col)) { %>
						drop(<%= row %>, <%= col %>, <%= rcd.getLabel(row, col) %>);
					<% } else { %>
						clearMe(<%= row %>, <%= col %>);
					<% } // if there is a state in this cell
				} // if question type
			} // for each column
		} // for each row
		for (int lineNum = 0; lineNum < conLines.size(); lineNum++) {
			final CellsLine line = conLines.get(lineNum);
			if (!Utils.anyMembersAreNull(line.endPoints)) {
				final int ARow = line.endPoints[0].getRow();
				final int ACol = line.endPoints[0].getColumn();
				final int BRow = line.endPoints[1].getRow();
				final int BCol = line.endPoints[1].getColumn(); %>
				initLine('r<%= ARow %>c<%= ACol %>', 'r<%= BRow %>c<%= BCol %>');
		<% 	} // if both endpoints are present
		} // for each line %>
		write<%= isOED ? "OrbSelectors" : "TextAndButtons" %>();
		updateCanvas();
	} // initED()

	function makeTolVisible() {
		var energies = getValue('energies');
		var REL_HT = '<%= EDiagramDiff.RELATIVE_HEIGHT %>';
		var FIX_HT = '<%= EDiagramDiff.FIXED_HEIGHT %>';
		setVisibility('toler', [REL_HT, FIX_HT].contains(energies) 
				? 'visible' : 'hidden');
	} // makeTolVisible()

	<% } // OED_DIFF or RCD_DIFF
	if (evalConstant == OED_ELEC) { %>

	function changeNumSelector() {
		var numType = getValue('num_or_colCt');
		var currentNum = parseInt(document.evaluatorForm.number.value);
		var selBld = new String.builder();
		var feedback = '';
		if (numType === 'num') {
			selBld.append('<input name="number" type="text" size="3" value="').
					append(-currentNum).append('" \/>');
		} else {
			selBld.append('<select name="number" onchange="suggestFeedback();">');
			<% for (int col = -1; col >= -3; col--) { %>
				selBld.append('<option value="<%= col %>"');
				if (-currentNum === <%= col %>) {
					selBld.append(' <%= Utils.toValidJS(SELECTED) %>'); 
				}
				selBld.append('><%= -col %><\/option>');
			<% } // for each column %>
			selBld.append('<\/select>');
		}
		setInnerHTML('num_Input', selBld.toString());
		suggestFeedback();
	} // changeNumSelector()

	function suggestFeedback() {
		var feedbackBox = document.evaluatorForm.feedback;
		var column = document.evaluatorForm.column.value;
		var number = document.evaluatorForm.number.value;
		var suggestion = '<%= Utils.toValidJS(Utils.toValidTextbox(
				dummyUser.translate("The sum of the number of electrons in "
				+ "columns 1 and 3 should equal the number of "
				+ "electrons in column 2."))) %>';
		if (feedbackBox && isEmpty(feedbackBox.value)
				&& column === '<%= OEDElecCt.COLUMNS1AND3 %>'
				&& number === '-2') {
			feedbackBox.value = suggestion;
		} else if (feedbackBox && feedbackBox.value === suggestion) {
			feedbackBox.value = '';
		} // if should suggest feedback
	} // suggestFeedback()

	<% } // OED_ELEC
	if (evalConstant == MECH_PRODS_STARTERS_PROPS) { %>

	function changeEvalType() {
		var form = document.evaluatorForm;
		var go = new String.builder().
				append(getChangeEvaluatorString()).
				append('&subevalNum=').append(form.evalTypeSelector.value).
				append('&howMany=').append(form.howMany.value).
				append('&cpdsType=').append(form.cpdsType.value);
		self.location.href = go.toString();
	} // changeEvalType()

	<% } // MECH_PRODS_STARTERS_PROPS
	if (evalConstant == MECH_RULE) { %>

	var chosenSet = -1;
	var chosenRule = -1;

	var setTitles = new Array(3);
	var GENERAL = 0;
	var CONDNS = 1;
	var CONVENTIONS = 2;
	setTitles[GENERAL] = 'general chemical rule';
	setTitles[CONDNS] = 'reaction-condition-specific rule';
	setTitles[CONVENTIONS] = 'mechanism-drawing convention';

	var sets = new Array(3);
	sets[GENERAL] = new Array(
			<%= MechRule.SN2_SP %>,
			<%= MechRule.IONIZE_BOND %>,
			<%= MechRule.PRIMARY_CARBOCAT %>,
			<%= MechRule.PKA_RULE %>,
			<%= MechRule.CATIONIC_SHIFTS_MUST_BE_1_2 %>,
			<%= MechRule.NO_ZWITTERIONS %>,
			<%= MechRule.NO_ATOM_MULTIPLE_CHG %>,
			<%= MechRule.NO_MOL_MULTIPLE_TOTAL_CHG %>,
			<%= MechRule.NO_SAME_CHARGE_REACT %>,
			<%= MechRule.NO_X_H_BOND_X_NUC %>,
			<%= MechRule.NO_FOUR_MEMB_PROTON_TRANSFER %>,
			<%= MechRule.NO_DYOTROPIC %>,
			<%= MechRule.NO_SIGMA_METATHESIS %>,
			<%= MechRule.NO_TERMOLECULAR %>,
			<%= MechRule.PERI_FOUR %>,
			<%= MechRule.PERICYCLIC %>);
	sets[CONDNS] = new Array(
			<%= MechRule.RADICAL %>,
			<%= MechRule.CARBOCATION %>,
			<%= MechRule.BASE_PK %>,
			<%= MechRule.ACID_PK %>,
			<%= MechRule.NO_ACIDIC_SN2 %>,
			<%= MechRule.NO_ACIDIC_E2 %>);
	sets[CONVENTIONS] = new Array(
			<%= MechRule.SUPPLY_RECEIVE %>,
			<%= MechRule.RES_CONNECT %>, 
			<%= MechRule.CONNECT_RES %>);

	var ruleDescrip = new Array(<%= MechRule.RULES_TEXT.length %>);
	ruleDescrip[<%= MechRule.SN2_SP %>] = 
			'No SN2 at sp- or sp&#178;-hybridized atom.';
	ruleDescrip[<%= MechRule.IONIZE_BOND %>] = 
			'No ionization of sp- or sp&#178;-hybridized atom.';
	ruleDescrip[<%= MechRule.PRIMARY_CARBOCAT %>] = 
			'No CH&#8323;&#8314; or unstabilized 1&deg; carbocations.';
	ruleDescrip[<%= MechRule.PKA_RULE %>] = 
			'Violation of pKa rule (best base pKb &ndash; best acid pKa &le; 8).';
	ruleDescrip[<%= MechRule.PERICYCLIC %>] = 
			'No even-electron pericyclic reactions.';
	ruleDescrip[<%= MechRule.PERI_FOUR %>] = 
			'No four-atom pericyclic reactions.';
	ruleDescrip[<%= MechRule.CATIONIC_SHIFTS_MUST_BE_1_2 %>] = 
			'Cationic shifts must be 1,2-shifts.';
	ruleDescrip[<%= MechRule.NO_ZWITTERIONS %>] = 
			'No zwitterions.';
	ruleDescrip[<%= MechRule.NO_ATOM_MULTIPLE_CHG %>] = 
			'No multiply charged atoms.';
	ruleDescrip[<%= MechRule.NO_MOL_MULTIPLE_TOTAL_CHG %>] = 
			'No multiply charged molecules.';
	ruleDescrip[<%= MechRule.NO_SAME_CHARGE_REACT %>] = 
			'No similarly charged compounds react with one another.';
	ruleDescrip[<%= MechRule.NO_X_H_BOND_X_NUC %>] = 
			'No X&ndash;H bond as nucleophile.';
	ruleDescrip[<%= MechRule.NO_FOUR_MEMB_PROTON_TRANSFER %>] = 
			'No four-membered TS for H&#8314; transfer.';
	ruleDescrip[<%= MechRule.NO_DYOTROPIC %>] = 
			'No dyotropic rearrangements.';
	ruleDescrip[<%= MechRule.NO_SIGMA_METATHESIS %>] = 
			'No &sigma;-bond metatheses or dyotropic rearrangements.';
	ruleDescrip[<%= MechRule.RADICAL %>] = 
			'No radicals under polar conditions.';
	ruleDescrip[<%= MechRule.NO_TERMOLECULAR %>] = 
			'No termolecular steps.';
	ruleDescrip[<%= MechRule.ACID_PK %>] = 
			'No acids under these basic conditions should have pKa &lt;';
	ruleDescrip[<%= MechRule.BASE_PK %>] = 
			'No bases under these acidic conditions should have pKb &gt;';
	ruleDescrip[<%= MechRule.NO_ACIDIC_SN2 %>] = 
			'No SN2 under acidic conditions.'; 
	ruleDescrip[<%= MechRule.NO_ACIDIC_E2 %>] = 
			'No E2 under acidic conditions.'; 
	ruleDescrip[<%= MechRule.CARBOCATION %>] = 
			'No carbocations (except iminium ions) under basic conditions.';
	ruleDescrip[<%= MechRule.SUPPLY_RECEIVE %>] = 
			'No atom should receive &amp; supply electrons.';
	ruleDescrip[<%= MechRule.RES_CONNECT %>] = 
			'No resonance arrow where there should be one.';
	ruleDescrip[<%= MechRule.CONNECT_RES %>] = 
			'Resonance arrow where there shouldn\'t be one.';

	var feedbacks = new Array(<%= MechRule.RULES_TEXT.length %>);
	feedbacks[<%= MechRule.SN2_SP %>] =
			'<%= Utils.toValidJS(Utils.toValidTextbox(dummyUser.translateJS(
			"SN2 substitution does not occur at an "
			+ "sp- or sp2-hybridized atom."))) %>';
	feedbacks[<%= MechRule.IONIZE_BOND %>] = 
			'<%= Utils.toValidJS(Utils.toValidTextbox(dummyUser.translateJS(
			"The bond between a leaving group and an sp- or "
			+ "sp2-hybridized atom does not spontaneously ionize."))) %>'; 
	feedbacks[<%= MechRule.PRIMARY_CARBOCAT %>] = 
			'<%= Utils.toValidJS(Utils.toValidTextbox(dummyUser.translateJS(
			"The CH3^+ ion and primary carbocations that are not stabilized "
			+ "by resonance with a heteroatom or a #pi bond are too high "
			+ "in energy to be proposed as intermediates."))) %>';
	feedbacks[<%= MechRule.PKA_RULE %>] =
			'<%= Utils.toValidJS(Utils.toValidTextbox(dummyUser.translateJS(
			"No step should contain an acid and a base that satisfy the "
			+ "condition, the pKa of the conjugate acid of the "
			+ "base minus the pKa of the acid < 8 (the pKa rule). "))) %>';
	feedbacks[<%= MechRule.PERICYCLIC %>] = 
			'<%= Utils.toValidJS(Utils.toValidTextbox(dummyUser.translateJS(
			"Pericyclic reactions (other than electrocyclic reactions "
			+ "or cheletropic cycloadditions) "
			+ "must not involve an even number of pairs of electrons."))) %>';
	feedbacks[<%= MechRule.PERI_FOUR %>] = 
			'<%= Utils.toValidJS(Utils.toValidTextbox(dummyUser.translateJS(
			"Pericyclic reactions (other than electrocyclic reactions) "
			+ "must not involve four atoms."))) %>';
	feedbacks[<%= MechRule.CATIONIC_SHIFTS_MUST_BE_1_2 %>] = 
			'<%= Utils.toValidJS(Utils.toValidTextbox(dummyUser.translateJS(
			"Cationic shifts are almost never other than 1,2-shifts. "
			+ "Longer-range cationic shifts are best drawn as "
			+ "a series of 1,2-shifts."))) %>';
	feedbacks[<%= MechRule.NO_ZWITTERIONS %>] = 
			'<%= Utils.toValidJS(Utils.toValidTextbox(dummyUser.translateJS(
			"No intermediate in this mechanism should be a zwitterion. "))) %>';
	feedbacks[<%= MechRule.NO_ATOM_MULTIPLE_CHG %>] =
			'<%= Utils.toValidJS(Utils.toValidTextbox(dummyUser.translateJS(
			"No atom in this mechanism should have a total charge "
			+ "of &plusmn;2 or greater."))) %>';
	feedbacks[<%= MechRule.NO_MOL_MULTIPLE_TOTAL_CHG %>] = 
			'<%= Utils.toValidJS(Utils.toValidTextbox(dummyUser.translateJS(
			"No compound in this mechanism should have a total charge "
			+ "of &plusmn;2 or greater."))) %>';
	feedbacks[<%= MechRule.NO_SAME_CHARGE_REACT %>] = 
			'<%= Utils.toValidJS(Utils.toValidTextbox(dummyUser.translateJS(
			"Two compounds that are both positively or "
			+ "both negatively charged should not react with one another."))) %>';
	feedbacks[<%= MechRule.NO_X_H_BOND_X_NUC %>] = 
			'<%= Utils.toValidJS(Utils.toValidTextbox(dummyUser.translateJS(
			"When an atom acts as a nucleophile, it normally uses the electrons "
			+ "in a lone pair, a #pi bond, or a #sigma bond to a metal to make "
			+ "the new bond, not those in a #sigma bond to H. If the atom must "
			+ "use the electrons in the #sigma bond to H, deprotonate the atom "
			+ "first to give it a lone pair that it can use subsequently."))) %>';
	feedbacks[<%= MechRule.NO_FOUR_MEMB_PROTON_TRANSFER %>] = 
			'<%= Utils.toValidJS(Utils.toValidTextbox(dummyUser.translateJS(
			"It is considered bad practice to transfer a proton via a "
			+ "four-membered transition state. Better to protonate the "
			+ "basic atom with a H^+ different from the one obtained by "
			+ "deprotonation of the acidic atom."))) %>';
	feedbacks[<%= MechRule.NO_DYOTROPIC %>] = 
			'<%= Utils.toValidJS(Utils.toValidTextbox(dummyUser.translateJS(
			"Dyotropic rearrangements, in which atoms or groups attached to "
			+ "neighboring atoms switch places in a single, concerted step, "
			+ "are extremely unlikely to occur."))) %>';
	feedbacks[<%= MechRule.NO_SIGMA_METATHESIS %>] = 
			'<%= Utils.toValidJS(Utils.toValidTextbox(dummyUser.translateJS(
			"A &sigma;-bond metathesis, in which the groups attached to two "
			+ "atoms swap places in a single step, does not occur in this "
			+ "reaction."))) %>';
	feedbacks[<%= MechRule.RADICAL %>] = 
			'<%= Utils.toValidJS(Utils.toValidTextbox(dummyUser.translateJS(
			"No radicals should form in this polar mechanism."))) %>';
	feedbacks[<%= MechRule.NO_TERMOLECULAR %>] = 
			'<%= Utils.toValidJS(Utils.toValidTextbox(dummyUser.translateJS(
			"Mechanistic steps that involve three or more separate molecules "
			+ "coming together to react are exceedingly rare. Try "
			+ "rewriting your mechanism so that it involves only pairs of "
			+ "molecules reacting in each step."))) %>';
	feedbacks[<%= MechRule.ACID_PK %>] = 
			'<%= Utils.toValidJS(Utils.toValidTextbox(dummyUser.translateJS(
			"No acids in this mechanism that takes place under basic "
			+ "conditions should have pKa < ."))) %>';
	feedbacks[<%= MechRule.BASE_PK %>] = 
			'<%= Utils.toValidJS(Utils.toValidTextbox(dummyUser.translateJS(
			"No bases in this mechanism that takes place under acidic "
			+ "conditions should have conjugate acids whose pKa > ."))) %>';
	feedbacks[<%= MechRule.NO_ACIDIC_SN2 %>] = 
			'<%= Utils.toValidJS(Utils.toValidTextbox(dummyUser.translateJS(
			"Substitution reactions at C rarely occur by the SN2 "
			+ "mechanism under acidic conditions."))) %>';
	feedbacks[<%= MechRule.NO_ACIDIC_E2 %>] = 
			'<%= Utils.toValidJS(Utils.toValidTextbox(dummyUser.translateJS(
			"Elimination reactions do not occur by the E2 "
			+ "mechanism under acidic conditions."))) %>';
	feedbacks[<%= MechRule.CARBOCATION %>] = 
			'<%= Utils.toValidJS(Utils.toValidTextbox(dummyUser.translateJS(
			"No carbocations (except iminium ions) should be generated "
			+ "under basic reaction conditions."))) %>';
	feedbacks[<%= MechRule.SUPPLY_RECEIVE %>] =
			'<%= Utils.toValidJS(Utils.toValidTextbox(dummyUser.translateJS(
			"No atom should simultaneously receive and supply "
			+ "unshared electrons."))) %>';
	feedbacks[<%= MechRule.RES_CONNECT %>] = 
			'<%= Utils.toValidJS(Utils.toValidTextbox(dummyUser.translateJS(
			"Boxes containing resonance structures must be connected "
			+ "by a double-headed arrow."))) %>';
	feedbacks[<%= MechRule.CONNECT_RES %>] = 
			'<%= Utils.toValidJS(Utils.toValidTextbox(dummyUser.translateJS(
			"Boxes connected by a double-headed arrow must contain "
			+ "resonance structures."))) %>';

	var examples = new Array(<%= MechRule.RULES_TEXT.length %>);
	var exampleRuleNums = [
			<%= MechRule.NO_SAME_CHARGE_REACT %>,
			<%= MechRule.NO_DYOTROPIC %>,
			<%= MechRule.NO_SIGMA_METATHESIS %>,
			<%= MechRule.NO_FOUR_MEMB_PROTON_TRANSFER %>];

	<% for (int exampleNum = 0; exampleNum < 4; exampleNum++) { %>
		examples[exampleRuleNums[<%= exampleNum %>]] = 
				'<%= Utils.toValidJS(MolString.getImage(
						pathToRoot, examples[exampleNum], 
						Question.SHOWLONEPAIRS)) %>';
	<% } // for each example %>

	var addl = new Array(<%= MechRule.RULES_TEXT.length %>);

	function initializeAddl(rule, flags, pKvalue) { // rule is an int
		addl[<%= MechRule.SUPPLY_RECEIVE %>] = '';
		addl[<%= MechRule.RES_CONNECT %>] = '<br/>Check here '
				+ '<input type="checkbox" name="omitFirstStepRES_CONNECT"'
				+ (rule === <%= MechRule.RES_CONNECT %> && flags !== 0 ? 
					' <%= CHECKED %>' : '') + '/> '
				+ 'to omit the first step of a photochemical mechanism.';
		addl[<%= MechRule.CONNECT_RES %>] = ''; 
		addl[<%= MechRule.PKA_RULE %>] =
				'<br/>Check here <input type="checkbox" name="omitFirstStep"'
				+ (rule === <%= MechRule.PKA_RULE %>
					&& (flags & <%= MechRule.OMIT_1ST_STEP_MASK %>) !== 0 ? 
							' <%= CHECKED %>' : '') + '/>'
				+ 'to omit the first step of the mechanism (e.g., '
				+ 'because a strong base initiates the reaction). '
				+ '<br/>Check here <input type="checkbox" name="omitLastStep"'
				+ (rule === <%= MechRule.PKA_RULE %>
					&& (flags & <%= MechRule.OMIT_LAST_STEP_MASK %>) !== 0 ? 
							' <%= CHECKED %>' : '') + '/> '
				+ 'to omit the last step of the mechanism (e.g., because '
				+ 'it is a workup step).';
		addl[<%= MechRule.SN2_SP %>] = '';
		addl[<%= MechRule.IONIZE_BOND %>] = '';
		addl[<%= MechRule.PRIMARY_CARBOCAT %>] = ''; 
		addl[<%= MechRule.PERICYCLIC %>] = '';
		addl[<%= MechRule.PERI_FOUR %>] = '';
		addl[<%= MechRule.CATIONIC_SHIFTS_MUST_BE_1_2 %>] = '';
		addl[<%= MechRule.NO_ZWITTERIONS %>] = 
				'<br/>Check here <input type="checkbox" name="posRequiresH"'
				+ (rule === <%= MechRule.NO_ZWITTERIONS %> && flags !== 0 ? 
					' <%= CHECKED %>' : '') + '/> '
				+ 'if this rule does not apply when the positively '
				+ 'charged atom lacks an H atom.';
		addl[<%= MechRule.NO_ATOM_MULTIPLE_CHG %>] = '';
		addl[<%= MechRule.NO_MOL_MULTIPLE_TOTAL_CHG %>] = ''; 
		addl[<%= MechRule.NO_SAME_CHARGE_REACT %>] = 
				'<br/>Check here <input type="checkbox" name="allowHTransfers"'
				+ (rule === <%= MechRule.NO_SAME_CHARGE_REACT %> && flags !== 0 ? 
					' <%= CHECKED %>' : '') + '/> '
				+ 'to allow proton transfer steps such as:</span>'
				+ '<p id="ruleExample"></p>';
		addl[<%= MechRule.NO_X_H_BOND_X_NUC %>] = '';
		addl[<%= MechRule.NO_FOUR_MEMB_PROTON_TRANSFER %>] = 
				'<br/><span style="color:green;">'
				+ 'Examples of four-membered transition states '
				+ 'for proton transfer:</span>'
				+ '<p id="ruleExample"></p>';
		addl[<%= MechRule.NO_DYOTROPIC %>] = 
				'<br/><span style="color:green;">'
				+ 'An example of a dyotropic rearrangement:</span>'
				+ '<p id="ruleExample"></p>';
		addl[<%= MechRule.NO_SIGMA_METATHESIS %>] =
				'<br/><span style="color:green;">'
				+ 'Examples of a &sigma;-bond metathesis and '
				+ 'and a dyotropic rearrangement:</span>'
				+ '<p id="ruleExample"></p>';
		addl[<%= MechRule.RADICAL %>] = '';
		addl[<%= MechRule.NO_TERMOLECULAR %>] = '';
		addl[<%= MechRule.ACID_PK %>] = 
				'<input type="text" name="pKvalue" size="5"'
				+ (rule === <%= MechRule.ACID_PK %> ? 
					' value="' + pKvalue + '"' : '') + '/>. '
				+ '<br /><br /><span style="color:green;">[ACE may generate '
				+ 'automatic feedback for mechanisms that violate this rule. '
				+ 'If so, ACE will append your feedback.]<\/span>';
		addl[<%= MechRule.BASE_PK %>] = 
				'<input type="text" name="pKvalue1" size="5"'
				+ (rule === <%= MechRule.BASE_PK %> ? 
					' value="' + pKvalue + '"' : '') + '/>. '
				+ '<br /><br /><span style="color:green;">[ACE may generate '
				+ 'automatic feedback for mechanisms that violate this rule. '
				+ 'If so, ACE will append your feedback.]<\/span>';
		addl[<%= MechRule.NO_ACIDIC_SN2 %>] = '';
		addl[<%= MechRule.NO_ACIDIC_E2 %>] = '';
		addl[<%= MechRule.CARBOCATION %>] = '';
	} // initializeAddl()

	function initializeGroupSelectorSet(initRule) {
		for (var setNum = 0; setNum < setTitles.length; setNum++) { // <!-- >
			for (var ruleNum = 0; ruleNum < sets[setNum].length; ruleNum++) { // <!-- >
				if (initRule === sets[setNum][ruleNum]) {
					setSet(setNum, true, initRule);
					return;
				} 
			} // for each rule in this set
		} // for each set
	} // initializeGroupSelectorSet()

	function setSet(newlyChosenSet, initializing, initRule) {
		if (newlyChosenSet === -1) {
			newlyChosenSet = getValue('setSelector');
		}
		if (newlyChosenSet === chosenSet) return;
		chosenSet = newlyChosenSet;
		if (initializing) setValue('setSelector', chosenSet);
		var origRuleHere = false; 
		var out = new String.builder().
				append('<select name="rule" id="rule" '
					+ 'onchange="setRule(parseInt(this.value), false)">');
		var numSetRules = sets[chosenSet].length;
		for (var setRuleNum = 0; setRuleNum < numSetRules; setRuleNum++) { // <!-- >
			out.append('<option value="').
					append(sets[chosenSet][setRuleNum]).append('"');
			if (sets[chosenSet][setRuleNum] === initRule) {
				origRuleHere = true;
				out.append(' selected="selected"');
			}
			out.append(' />').append(ruleDescrip[sets[chosenSet][setRuleNum]]).
					append('<\/option>');
		} // for each rule in the set
		out.append('<\/select><br /><span id="addl"><\/span>');
		setInnerHTML('rules', out.toString());
		setRule(origRuleHere ? initRule : sets[chosenSet][0], initializing);
	} // setSet()

	function setRule(newlyChosenRule, initializing) {
		chosenRule = newlyChosenRule;
		for (var rule = 0; rule < sets[chosenSet].length; rule++) { // <!-- >
			var ruleNum = sets[chosenSet][rule];
			if (ruleNum === newlyChosenRule) {
				setInnerHTML('addl', addl[ruleNum]);
				if (document.getElementById('ruleExample')) {
					setInnerHTML('ruleExample', 
							examples[ruleNum]);
					/*
							'<div id="figJS1" class="left10" '
							+ 'style="display:none; text-align:center;"></div>');
					displayImage('<%= pathToRoot %>',
							examples[ruleNum][0],
							{ flags: SHOWLONEPAIRS,
							imageId: '1',
							width: examples[ruleNum][1][0],
							height: examples[ruleNum][1][1] }); 
					/**/
					document.getElementById('ruleExample').style
							.backgroundColor = 'white';
				} // if need to show a rule example
				var feedbackBox = document.evaluatorForm.feedback;
				if (!initializing || (feedbackBox && isEmpty(feedbackBox.value))) {
					var textBoxHtml = new String.builder().
							append('<textarea id="feedback" name="feedback" '
								+ 'style="overflow:auto; width:100%;" '
								+ 'rows="4">').
							append(feedbacks[ruleNum]).append('<\/textarea>');
					setInnerHTML('feedbackBox', textBoxHtml.toString());
				}
				setValue('rule', ruleNum);
			} // if ruleNum === newlyChosenRule
		} // for each rule
	} // setRule()

	function setSetOptions() {
		var bld = new String.builder();
		for (var setNum = 0; setNum < setTitles.length; setNum++) { // <!-- >
			bld.append('<option value="' + setNum + '"');
			if (setNum === chosenSet) bld.append(' selected="selected"');
			bld.append('>' + setTitles[setNum] + '<\/option>');
		}
		setInnerHTML('setSelector', bld.toString());
	} // setSetOptions()

	<% } // MECH_RULE %>

	function initSelector() {
		var form = document.evaluatorForm;
		var initIsPositive = (form.initIsPositive 
				&& form.initIsPositive.value === 'true');
		<% switch (evalConstant) { 
		case FUNCTIONAL_GROUP: %>
			<% for (int grpNum = 1; grpNum <= sortedGroups.length; grpNum++) { 
				final FnalGroupDef group = sortedGroups[grpNum - 1]; %>
				setArrayValues(<%= grpNum %>,
						'<%= Utils.toValidJS(group.getPulldownName()) %>',
						'<%= Utils.toValidJS(group.category) %>',
						<%= group.groupId %>);
			<% } // for each group %>
			initFnalGroupConstants(parseInt(form.initGroupId.value));
			setCatSelector();
			initializeGroupSelector(form);
		<% break;
		case FORMULA_FORMAT: %>
			writeIsPosSelector(initIsPositive, 'does not violate', 'violates');
		<% break;
		case MAPPED_ATOMS: %>
			var toBe = document.evaluatorForm.toBe.value;
			writeIsPosSelector(initIsPositive, toBe, toBe + ' not'); 
		<% break;
		case LEWIS_ISOMORPHIC: %>
			writeIsPosSelector(initIsPositive, 'exactly', 'not exactly'); 
		<% break;
		case LEWIS_OUTER_SHELL_COUNT: %>
			writeIsPosSelector(initIsPositive, 'every', 'any');
		<% break;
		case MECH_FLOWS: %>
			writeIsPosSelector(initIsPositive, 'each step', 'any step do not');
		<% break;
		case MECH_RULE: %>
			var rule = parseInt(form.initRule.value);
			var flags = parseInt(form.initFlags.value);
			initializeAddl(rule, flags, form.initPKValue.value);
			writeIsPosSelector(initIsPositive, 'does not violate', 'violates');
			setSetOptions();
			initializeGroupSelectorSet(rule);
		<% break;
		case MECH_SUBSTRUCTURE: %>
			writeIsPosSelector(initIsPositive, 'contains', 'does not contain');
		<% break;
		case SYNTH_SM_MADE: %>
			writeIsPosSelector(initIsPositive, 'any', 'no');
		<% break;
		case SYNTH_SCHEME: %>
			writeIsPosSelector(initIsPositive, 'every compound is', 
					'any compound is not');
		<% break;
		case MECH_INIT: 
		case MECH_TOPOLOGY: 
		case SYNTH_ONE_RXN: 
		case SYNTH_SELEC: 
		case SYNTH_TARGET: %>
			writeIsPosSelector(initIsPositive, 'is', 'is not');
		<% break;
		case OED_DIFF:
		case RCD_DIFF: %>
			writeIsPosSelector(initIsPositive, 'match', 'do not match');
		<% break;
		case MECH_EQUALS: 
		case SYNTH_EQUALS: %>
			writeIsPosSelector(initIsPositive, 'matches', 'does not match');
		<% break;
		} // switch evalConstant %>
	} // initSelector()

	function getFlagsValue(form) {
		form.flags.value = 
				(isChecked(form.enantiomer)
					? parseInt(form.enantiomer.value) : 0)
				| (isChecked(form.normalization)
					? parseInt(form.normalization.value) : 0)
				| (isChecked(form.isotopeLenient)
					? parseInt(form.isotopeLenient.value) : 0)
				| (isChecked(form.resonance)
					? parseInt(form.resonance.value) : 0)
				| (isChecked(form.sigmanetwork)
					? parseInt(form.sigmanetwork.value) : 0);
	} // getFlagsValue()

	function submitIt(addNew) {
		var form = document.evaluatorForm;
		if (!gradeOK(form)) {
			alert('Enter a grade between 0 and 1.');
			return;
		}
		<% if (usesApplet) { %>
			marvinSketcherInstances['<%= APPLET_NAME %>'].
					exportStructure('<%= MRV_EXPORT %>').then(function(source) {
				// closing of function occurs after form.submit()
		<% } // if usesApplet
		switch (evalConstant) { 
		case NUM_ATOMS: %>
			var numAtoms = form.numAtoms.value;
			if (!isNonnegativeInteger(numAtoms)) {
				alert('Please enter a nonnegative integer '
						+ 'for the number of atoms.');
				return;
			}
			if (!numMolsOK()) return;
			<% break; 
		case NUM_RINGS: %>
			var numRings = form.numRings.value;
			if (!isNonnegativeInteger(numRings)) {
				alert('Please enter a nonnegative integer '
						+ 'for the number of rings.');
				return;
			}
			if (!numMolsOK()) return;
			<% break; 
		case TOTAL_CHARGE: %> 
			var chgValue = form.chgValue.value;
			if (isWhiteSpace(chgValue) || !canParseToInt(chgValue)) {
				alert('Please enter an integral charge.');
				return;
			}
			if (!numMolsOK()) return;
			<% break; 
		case FUNCTIONAL_GROUP: %>
			if (form.fnalGroupId.value === 0) {
				alert('You must select a functional group.');
				return;
			}
			if (!numMolsOK()) return;
			<% break; 
		case WEIGHT:
		case FORMULA_WEIGHT: %> 
			var molWtStr = form.molweight.value;
			if (isWhiteSpace(molWtStr) || !canParseToFloat(molWtStr)) {
				alert('Enter a numerical molecular weight.');
				return;
			}
			var molWt = parseFloat(molWtStr);
			if (molWt < 0) { // <!-- >
				alert('Enter a positive molecular weight.');
				return;
			}
			var tolStr = form.tolerance.value;
			if (!isWhiteSpace(tolStr)) {
				if (!canParseToFloat(tolStr)) {
					alert('Enter a numerical tolerance, '
							+ 'or leave the tolerance blank (not recommended).');
					return;
				}
				var tol = parseFloat(tolStr);
				if (tol < 0) { // <!-- >
					alert('Enter a positive tolerance, '
							+ 'or leave the tolerance blank (not recommended).');
					return;
				}
			} // if tolerance is not left blank
			if (!numMolsOK()) return;
			<% break; 
		case NUM_MOLECULES: %> 
			getFlagsValue(form);
			<% // no break; 
		case UNSATURATION: %> 
			var number = form.number.value;
			if (!isNonnegativeInteger(number)) {
				alert('Please enter a nonnegative integer.');
				return;
			}
			<% break; 
		case CONFORMATION_CHAIR: %> 
			form.overrideFor.value = form.overrideForIndeterminate.checked;
			<% // no break; 
		case HAS_FORMULA: %> 
			if (isWhiteSpace(form.formula.value)) {
				alert('Please enter a <%= evalConstant == HAS_FORMULA
						? "molecular formula"
						: "valid group abbreviation or SMILES code" %>.');
				return;
			}
			if (!numMolsOK()) return;
			<% break; 
		case CONFORMATION_ACYCLIC: %> 
			if (isWhiteSpace(form.formula1.value)
					|| isWhiteSpace(form.formula2.value)) {
				alert('Enter valid group abbreviations or SMILES codes.');
				return;
			}
			<% break; 
		case BOND_ANGLE: %> 
			var authAngle = form.authAngle.value;
			if (!isNonnegativeInteger(authAngle)) { 
				alert('Enter a bond angle (in degrees) as a nonnegative integer.');
				return;
			}
			var tolerance = form.tolerance.value;
			if (!isNonnegativeInteger(tolerance)) {
				alert('Enter a tolerance (in degrees) as a nonnegative integer.');
				return;
			}
			form.molstruct.value = source;
			<% break; 
		case LEWIS_ISOMORPHIC: %>
			var molname = trimWhiteSpaces(form.inputmolname.value);
			if (isEmpty(molname)) {
				alert('Please enter a name for the molecule.');
				return;
			}
			form.molname.value = molname;
			form.molstruct.value = getLewisMRV();
			<% break; 
		case MAPPED_ATOMS: %>
			form.molstruct.value = source;
			marvinSketcherInstances['<%= APPLET_NAME %>'].getSelection().
					then(function(selection) { 
				form.selectionsStr.value = selection.atoms;
				// closing of function occurs after form.submit()
			<% break; 
		case MAPPED_COUNT: %>
			var matchPtsStr = form.matchPtsStr.value;
			var mismatchPtsStr = form.mismatchPtsStr.value;
			if (isWhiteSpace(matchPtsStr) || !canParseToFloat(matchPtsStr)
					|| isWhiteSpace(mismatchPtsStr) || !canParseToFloat(mismatchPtsStr)) {
				alert('Enter a number for each points value.');
				return;
			}
			form.aromatize.value = !form.skipAromatization.checked;
			form.checkEnantiomer.value = form.checkEnant.checked;
			form.molstruct.value = source;
			marvinSketcherInstances['<%= APPLET_NAME %>'].getSelection().
					then(function(selection) { 
				form.selectionsStr.value = selection.atoms;
				// closing of function occurs after form.submit()
			<% break; 
		case IS_OR_HAS_SIGMA_NETWORK: %>
			getFlagsValue(form);
			<% // no break; 
		case IS_2D_CHAIR: 
		case SKELETON_SUBSTRUCTURE:
		case SYNTH_TARGET: %> 
			var molname = trimWhiteSpaces(form.inputmolname.value);
			if (molname === '') {
				alert('Please enter a name for the molecule.');
				return;
			}
			form.molname.value = molname;
			<% // no break; 
		case MECH_FLOWS: 
		case MECH_INIT: 
		case MECH_PRODS_STARTERS_IS:
		case MECH_SUBSTRUCTURE: 
		case MECH_EQUALS: 
		case SYNTH_STARTERS: %>
			form.molstruct.value = source;
			<% break; 
		case SYNTH_STEPS: 
		case MECH_PIECES_COUNT: %>
			var decrement = form.decrement.value;
			if (!isWhiteSpace(decrement) 
					&& (!canParseToFloat(decrement)
						|| parseFloat(decrement) < 0 // <!-- >
						|| parseFloat(decrement) > 1)) {
				alert('Please enter a value between 0 and 1 for the decrement, '
						+ 'or enter nothing.');
				return;
			}
			<% break; 
		case SYNTH_SCHEME:
		case SYNTH_SELEC: %> 
			var partCreditsBld = new String.builder();
			var havePartCredit = false;
			<% for (final int errorCode : synthErrorCodes) { %>
				var partCreditInput = form.creditValue<%= errorCode %>;
				if (partCreditInput && !isWhiteSpace(partCreditInput.value)) {
					var partCredit = partCreditInput.value;
					if (!canParseToInt(partCredit)
							|| parseInt(partCredit) < 0 // <!-- >
							|| parseInt(partCredit) > 100) {
						alert('Please enter an integral value between 0 and '
								+ '100 for each percent partial credit, or '
								+ 'enter nothing at all.');
						return;
					} else {
						if (havePartCredit) {
							partCreditsBld.append('<%= 
									SynthPartCredits.CREDIT_MAJOR_SEP %>');
						} // if we already have partial credit
						partCreditsBld.append('<%= errorCode %>').
								append('<%= SynthPartCredits.CREDIT_MINOR_SEP %>').
								append(partCredit);
						havePartCredit = true;
					} // if part-credit value is OK
				} // if there is input for this error code
			<% } // for each error code %>
			form.partCredits.value = partCreditsBld.toString();
			<% if (evalConstant == SYNTH_SCHEME) break; 
		case SYNTH_EQUALS: 
		case SYNTH_ONE_RXN: %>
			form.rxnIds.value = getRxnIds();
			form.molstruct.value = source;
			<% break; 
		case LEWIS_OUTER_SHELL_COUNT: %> 
			if (form.condition.value === '<%= LewisOuterNumber.EVERY_NOT_GREATER %>') {
				form.oper.value = <%= LewisOuterNumber.NOT_GREATER %>;
				form.number.value = -1;
				form.element.value = 'X';
				form.isPositive.value = <%= LewisOuterNumber.EVERY %>;
			} else if (form.condition.value === '<%= LewisOuterNumber.ANY_GREATER %>') {
				form.oper.value = <%= LewisOuterNumber.GREATER %>;
				form.number.value = -1;
				form.element.value = 'X';
				form.isPositive.value = <%= LewisOuterNumber.ANY %>;
			} 
			<% break; 
		case MECH_RULE: %> 
			if (chosenRule === '<%= MechRule.ACID_PK %>'
					&& !canParseToFloat(form.pKvalue.value)) {
				alert('Please enter an appropriate value for the pKa.');
				return;
			}
			if (chosenRule === '<%= MechRule.BASE_PK %>'
					&& !canParseToFloat(form.pKvalue1.value)) {
				alert('Please enter an appropriate value for the pKb.');
				return;
			}
			<% break; 
		case OED_DIFF: 
		case RCD_DIFF: %> 
			var numRows = parseInt(document.evaluatorForm.numRows.value);
			var numCols = parseInt(document.evaluatorForm.numCols.value);
			form.molstruct.value = encode<%= evalConstant == OED_DIFF
					? "OED" : "RCD" %>(numRows, numCols);
			<% break; 
		case OED_ELEC: %>
			if (getValue('num_or_colCt') === 'num') {
				var numStr = document.evaluatorForm.number.value;
				if (!isNonnegativeInteger(numStr)) {
					alert('Please enter a nonnegative integer.');
					return;
				} // if not a nonnegative integer
			} // if entering a number
			<% break; 
		case RCD_STATE_CT: %>
			var columnsBld = new String.builder();
			var numCols = parseInt(document.evaluatorForm.numCols.value);
			var first = true;
			for (var cNum = 1; cNum <= numCols; cNum++) { // <!-- >
				if (getChecked('col' + cNum)) {
					if (first) first = false;
					else columnsBld.append('<%= RCDStateCt.COLS_SEP %>');
					columnsBld.append(cNum);
				} // if checked
			} // for each column
			if (first) {
				alert('Please select at least one column.');
				return;
			}
			document.evaluatorForm.columnsStr.value = columnsBld.toString();
			<% // no break; 
		case OED_TYPE: %>
			var numStr = document.evaluatorForm.number.value;
			if (!isNonnegativeInteger(numStr)) {
				alert('Please enter a nonnegative integer.');
				return;
			} // if not a nonnegative integer
			<% break; 
		case MECH_PRODS_STARTERS_PROPS: %>
			var codedData = new String.builder();
			var evalType = parseInt(form.evalTypeSelector.value);
			switch (evalType) {
				case <%= FUNCTIONAL_GROUP %>:
					// matchCode/groupID/!isPos=/0/N/N=/0
					if (form.fnalGroupId.value === '0') {
						alert('Please choose a functional group.');
						return;
					}
					codedData.append('<%= EVAL_CODES[FUNCTIONAL_GROUP] %>/').
							append(form.fnalGroupId.value).append('/').
							append(form.isPositive.value).
							append('/0/N/N=/0');
					break;
				case <%= HAS_FORMULA %>:
					// matchCode/!isPos=/0/N/formula
					if (isEmpty(form.formula.value)) {
						alert('Please enter a formula.');
						return;
					}
					codedData.append('<%= EVAL_CODES[HAS_FORMULA] %>/').
							append(form.isPositive.value).
							append('/0/N/').append(form.formula.value);
					break;
				case <%= NUM_ATOMS %>:
					// matchCode/oper/number/element/contiguous/N/N=/0
					codedData.append('<%= EVAL_CODES[NUM_ATOMS] %>/').
							append(form.atomsOper.value).append('/').
							append(form.numAtoms.value).append('/').
							append(form.element.value).append('/').
							append(form.contiguous.value).append('/N/N=/0');
					break;
				case <%= NUM_RINGS %>:
					// matchCode/N/ringsOper/numRings/N=/0
					codedData.append('<%= EVAL_CODES[NUM_RINGS] %>/N/').
							append(form.ringsOper.value).append('/').
							append(form.numRings.value).append('/N=/0');
					break;
				default: 
					alert('Unrecognized evaluator type ' + evalType
							+ ' parsed from ' + form.evalTypeSelector.value);
					self.close();
			}
			form.molstruct.value = codedData.toString();
			form.molname.value = form.molstruct.value;
			// alert(form.molstruct.value);
		<% break; } // switch %>
		if (form.feedback) {
			form.feedback.value = trimWhiteSpaces(form.feedback.value);
		}
		form.afterSave.value = addNew;
		form.submit();
		<% if (evalConstant == MAPPED_ATOMS || evalConstant == MAPPED_COUNT) { %>
			}, function(error) {
				alert('Failed to acquire selected atoms: ' + error);	
			});
		<% } // if usesApplet %>
		<% if (usesApplet) { %>
			}, function(error) {
				alert('Molecule export failed:' + error);	
			});
		<% } // if usesApplet %>
	} // submitIt()

	function numMolsOK() {
		var form = document.evaluatorForm;
		var valid = true;
		var compareSelector = document.getElementById('compareSelector');
		if (compareSelector && compareSelector.style.visibility === 'visible') {
			var numMols = form.numMols.value;
			if (!isNonnegativeInteger(numMols)) {
				alert('Please enter a nonnegative integer '
						+ 'for the number of compounds.');
			}
		} // if indicating number of compounds
		return valid;
	} // numMolsOK()

	// -->
</script>
</head>
<body class="light" style="margin:0px; margin-top:5px; 
		background-color:<%= bgColor %>; text-align:center; overflow:auto;" 
		onload="initSelector(); initGrade();<%=
			evalConstant == SYNTH_SCHEME ? " populatePartCreditCell();"
			: evalConstant == SYNTH_SELEC ? " initSynthesis(); populatePartCreditCell();"
			: usesRxnConds ? " initSynthesis();" 
			: Utils.among(evalConstant, 
				MECH_PIECES_COUNT, 
				SYNTH_STEPS) ? " changeDecrementCell();"
			: evalConstant == MAPPED_COUNT ? " toggleGradeSelector(false);"
			: evalConstant == LEWIS_ISOMORPHIC ? " initLewis();"
			: isED ? " initED();"
			: "" %>">
<form name="evaluatorForm" action="saveEvaluator.jsp" method="post"
		accept-charset="UTF-8">
	<input type="hidden" name="evalNum" value="<%= cloneEdit ? 0 : evalNum %>" />
	<input type="hidden" name="subevalNum" value="<%= cloneEdit ? 0 : subevalNum %>" />
	<input type="hidden" name="matchcode" value="<%= inputSubeval.matchCode %>" />
	<input type="hidden" name="afterSave" value="<%= RETURN %>" />
	<input type="hidden" name="qType" value="<%= qType %>" />

<%@ include file="selectorFooter.jsp.h" %>

<div id="evalContents">
<table style="width:100%;" summary="">
	<tr><td class="regtext" style="text-align:center;">

		<% if (evalConstant == NUM_ATOMS) { 
			int atomsOper = Atoms.NOT_EQUALS;
			int numAtoms = 0;
			String element = "C";
			boolean contiguous = false;
			boolean countEach = false;
			int molsOper = Atoms.NOT_EQUALS;
			int numMols = 0;
			if (useInput) { // existing evaluator
				final Atoms impl = new Atoms(inputSubeval.codedData);
				atomsOper = impl.getAtomsOper();
				numAtoms = impl.getNumAtoms();
				element = impl.getElement();
				contiguous = impl.getContiguous();
				countEach = impl.getCountEach();
				molsOper = impl.getMolsOper();
				numMols = impl.getNumMols();
			} // if useInput 
			final String[] OPER_ENGLISH = Atoms.OPER_ENGLISH[FEWER];
			%>
			<table style="margin-left:auto; margin-right:auto; width:445px;
					text-align:left;" summary="">
			<tr><td class="regtext">
				If 
				<select name="countEach" onchange="javascript:changeCompareSelector();">
					<option value="all" <%= !countEach ? SELECTED : "" %>>
					the response has</option> 
					<option value="each" <%= countEach ? SELECTED : "" %>>
					the number of compounds in the response that have</option> 
				</select>
				<select name="contig">
					<option value="false" <%= !contiguous 
							? SELECTED : "" %>>a total number of</option> 
					<option value="true" <%= contiguous 
							? SELECTED : "" %>>a largest 
						number of contiguous</option> 
				</select>
				<br/><input name="element" type="text" size="2"
					value="<%= element %>" />
				atoms that is
				<select name="atomsOper">
					<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
						<option value="<%= o %>" 
								<%= o == atomsOper ? SELECTED : "" %> >
						<%= OPER_ENGLISH[o] %></option>
					<% } %>
				</select>
				<input name="numAtoms" type="text" size="2" 
						value="<%= numAtoms %>" />
			</td></tr>
			<tr><td class="regtext" id="compareSelector"
					style="visibility:<%= countEach ? "visible" : "hidden" %>;">
				is
				<select name="molsOper">
					<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
						<option value="<%= o %>" <%= o == molsOper ? SELECTED : "" %> >
						<%= OPER_ENGLISH[o] %></option>
					<% } // for each operator %>
				</select>
				<input name="numMols" type="text" size="2"
					value="<%= numMols %>" />
				<br /><br /><%= rGroupsWarning %>
			</td></tr>
			<tr><td class="regtext" style="color:green; padding-top:10px;">
				<br/>
				You may specify a particular isotope of an element (e.g.,
				"D" or "13C"). If you do not specify a particular isotope,
				ACE will count all isotopes of the element toward the total.
				ACE will count implicit H atoms toward the total if you enter 
				"H" but not if you enter "1H". Use "X" to count every atom.
			</td></tr>
			</table>
		
		<% } else if (evalConstant == TOTAL_CHARGE) { 
			int chgOper = Charge.NOT_EQUALS;
			int chgValue = 0;
			boolean countEach = false;
			int molsOper = Charge.NOT_EQUALS;
			int numMols = 0;
			if (useInput) {
				final Charge impl = new Charge(inputSubeval.codedData);
				chgOper = impl.getChgOper();
				chgValue = impl.getCharge();
				countEach = impl.getCountEach();
				molsOper = impl.getMolsOper();
				numMols = impl.getNumMols();
			} // useInput 
			final String[][] OPER_ENGLISH = Charge.OPER_ENGLISH;
			%>
			<table style="margin-left:auto; margin-right:auto; width:445px;
					text-align:left;" summary="">
			<tr><td class="regtext">
				If 
				<select name="countEach" onchange="javascript:changeCompareSelector();">
					<option value="all" <%= !countEach ? SELECTED : "" %>>
					the response has</option> 
					<option value="each" <%= countEach ? SELECTED : "" %>>
					the number of compounds in the response that have</option> 
				</select>
			</td></tr>
			<tr><td class="regtext">
				a total charge of
				<select name="chgOper">
					<% for (int o = 0; o < OPER_ENGLISH[LESSER].length; o++) { %>
						<option value="<%= o %>" <%= o == chgOper ? SELECTED : "" %> >
						<%= OPER_ENGLISH[LESSER][o] %></option>
					<% } // for each operator %>
				</select>
				<input name="chgValue" type="text" size="2" value="<%= chgValue %>" />
			</td></tr>
			<tr><td class="regtext" id="compareSelector"
					style="visibility:<%= countEach ? "visible" : "hidden" %>;">
				is
				<select name="molsOper">
					<% for (int o = 0; o < OPER_ENGLISH[FEWER].length; o++) { %>
						<option value="<%= o %>" <%= o == molsOper ? SELECTED : "" %> >
						<%= OPER_ENGLISH[FEWER][o] %></option>
					<% } // for each operator %>
				</select>
				<input name="numMols" type="text" size="2"
					value="<%= numMols %>" />
			</td></tr>
			</table>

		<% } else if (evalConstant == NUM_MOLECULES) { 
			int oper = NumMols.GREATER;
			int number = 1; 
			boolean distinct = false;
			int flags = 0;
			if (useInput) { // existing evaluator
				final NumMols impl = new NumMols(inputSubeval.codedData);
				oper = impl.getOper();
				number = impl.getNumMols();
				distinct = impl.getDistinct();
				flags = impl.getFlags();
			} // useInput %>
			<input type="hidden" name="flags" value="" /> 
			<table style="margin-left:auto; margin-right:auto; width:445px;
					text-align:left;" summary="">
				<tr><td class="regtext">
				If the 
				<select name="distinct" onchange="toggleFlagsSetter();">
					<option value="false" <%= distinct ? "" : SELECTED
						%>>total number of compounds</option> 
					<option value="true" <%= distinct ? SELECTED : "" 
						%>>number of distinct compounds</option> 
				</select>
				in the response is
				<select name="oper">
					<% for (int o = 0; o < NumMols.OPER_ENGLISH[FEWER].length; o++) { %>
						<option value="<%= o %>" <%= o == oper ? SELECTED : "" %> >
						<%= NumMols.OPER_ENGLISH[FEWER][o] %></option>
					<% } // for each operator %>
				</select>
				<input name="number" type="text" size="2" value="<%= number %>" />
				</td></tr>
				<tr><td id="flagsSetter" class="regtext" 
						style="vertical-align:middle; visibility:<%=
							distinct ? "visible" : "hidden" %>">
					<br/><input type="checkbox" name="enantiomer"
							value="<%= NumMols.EITHER_ENANTIOMER %>"
							<%= (flags & NumMols.EITHER_ENANTIOMER) != 0 
									? CHECKED : "" %> />
					check both enantiomers
					<br/><input type="checkbox" name="resonance"
							value="<%= NumMols.RESONANCE_PERMISSIVE %>"
							<%= (flags & NumMols.RESONANCE_PERMISSIVE) != 0 
									? CHECKED : "" %> />
					check for resonance structures as well
					<br/><input type="checkbox" name="sigmanetwork"
							value="<%= NumMols.SIGMA_NETWORK %>"
							<%= (flags & NumMols.SIGMA_NETWORK) != 0 
									? CHECKED : "" %> />
					check for identity of &sigma;-bond networks only
					<br/><input type="checkbox" name="normalization"
							value="<%= NumMols.NO_NORMALIZATION %>"
							<%= (flags & NumMols.NO_NORMALIZATION) != 0 
									? CHECKED : "" %> />
					eschew normalization (aromatization and ylide standardization)
					<a name="options"/>&nbsp;
				</td></tr>
			</table>

		<% } else if (evalConstant == UNSATURATION) { 
			int oper = UnsaturIndex.EQUALS;
			int unsatIndex = 0; 
			if (useInput) { // existing evaluator
				final UnsaturIndex impl = new UnsaturIndex(inputSubeval.codedData);
				oper = impl.getOper();
				unsatIndex = impl.getUI();
			} // useInput %>
			<table style="margin-left:auto; margin-right:auto; width:445px;
					text-align:left;" summary="">
				<tr><td class="regtext">
				If the unsaturation index of the response formula is 
				<select name="oper" onchange="changeNumberVisibility();">
					<% for (int o = 0; o < UnsaturIndex.OPER_ENGLISH[FEWER].length; o++) { %>
						<option value="<%= o %>" <%= o == oper ? SELECTED : "" %> >
						<%= UnsaturIndex.OPER_ENGLISH[FEWER][o] %></option>
					<% } // for each operator
					for (int o = 0; o < UnsaturIndex.ADDL_SYMBOLS.length; o++) { 
						final int p = o + UnsaturIndex.OPER_ENGLISH[FEWER].length; %>
						<option value="<%= p %>" <%= p == oper ? SELECTED : "" %> >
						<%= p == UnsaturIndex.BAD_UNSATUR ? "negative or fractional" 
								: p == UnsaturIndex.NEG_UNSATUR ? "negative"
								: p == UnsaturIndex.FRACT_UNSATUR ? "fractional"
								: p == UnsaturIndex.GOOD_UNSATUR ? 
									"neither negative nor fractional"
								: "not acceptable" %></option>
					<% } // for each operator %>
				</select>
				<span name="unsatIndexCell" id="unsatIndexCell" style="visibility:<%= 
						oper < UnsaturIndex.SYMBOLS.length ? "visible" : "hidden" %>;">
				<input name="number" type="text" size="2" value="<%= unsatIndex %>" />
				</span>
				</td></tr>
			</table>

		<% } else if (evalConstant == WEIGHT) { 
			String molweight = "";
			String tolerance = "";
			int wtOper = Weight.EQUALS;
			int wtType = Weight.AVERAGE_WT;
			int molsOper = Weight.NOT_EQUALS;
			int numMols = 0;
			if (useInput) { // existing evaluator
				final Weight impl = new Weight(inputSubeval.codedData);
				molweight = impl.getMolWeight();
				tolerance = impl.getTolerance();
				wtOper = impl.getWtOper();
				wtType = impl.getWtType();
				molsOper = impl.getMolsOper();
				numMols = impl.getNumMols();
			} // useInput 
			final String[][] OPER_ENGLISH = Weight.OPER_ENGLISH; %>
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext">
			 	If the number of molecules that have an 
				<select name="weightType" >
					<option value="<%= Weight.EXACT_MASS %>" 
						<%= wtType == Weight.EXACT_MASS ? SELECTED : "" %> >
						exact mass</option>
					<option value="<%= Weight.AVERAGE_WT %>" 
						<%= wtType == Weight.AVERAGE_WT ? SELECTED : "" %> >
						average molecular weight </option>
				</select>
			</td></tr>
			<tr><td class="regtext">
				that is
				<select name="wtOper">
					<% for (int o = 0; o < OPER_ENGLISH[LESSER].length; o++) { %>
						<option value="<%= o %>" <%= o == wtOper 
								? SELECTED : "" %> >
						<%= OPER_ENGLISH[LESSER][o] %></option>
					<% } %>
				</select>
			</td></tr>
			<tr><td class="regtext">
				<input type="text" name="molweight"
					value="<%= molweight %>" /> &#177;
				<input type="text" name="tolerance"
					value="<%= tolerance %>" />
			</td></tr>
			<tr><td class="regtext" id="compareSelector">
				is
				<select name="molsOper">
					<% for (int o = 0; o < OPER_ENGLISH[FEWER].length; o++) { %>
						<option value="<%= o %>" <%= o == molsOper 
								? SELECTED : "" %> >
						<%= OPER_ENGLISH[FEWER][o] %></option>
					<% } %>
				</select>
				<input name="numMols" type="text" size="2"
					value="<%= numMols %>" />
			</td></tr>
			</table>

		<% } else if (evalConstant == FORMULA_WEIGHT) { 
			String molweight = "";
			String tolerance = "";
			int wtOper = FormulaWeight.EQUALS;
			int wtType = FormulaWeight.AVERAGE_WT;
			if (useInput) { // existing evaluator
				final FormulaWeight impl = 
						new FormulaWeight(inputSubeval.codedData);
				molweight = impl.getMolWeight();
				tolerance = impl.getTolerance();
				wtOper = impl.getWtOper();
				wtType = impl.getWtType();
			} // useInput 
			final String[][] OPER_ENGLISH = FormulaWeight.OPER_ENGLISH; %>
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext">
			 	If the formula has an 
				<select name="weightType" >
					<option value="<%= FormulaWeight.EXACT_MASS %>" 
						<%= wtType == FormulaWeight.EXACT_MASS ? SELECTED : "" %> >
						exact mass</option>
					<option value="<%= FormulaWeight.AVERAGE_WT %>" 
						<%= wtType == FormulaWeight.AVERAGE_WT ? SELECTED : "" %> >
						average molecular weight </option>
				</select>
			</td></tr>
			<tr><td class="regtext">
				that is
				<select name="wtOper">
					<% for (int o = 0; o < OPER_ENGLISH[LESSER].length; o++) { %>
						<option value="<%= o %>" <%= o == wtOper 
								? SELECTED : "" %> >
						<%= OPER_ENGLISH[LESSER][o] %></option>
					<% } %>
				</select>
			</td></tr>
			<tr><td class="regtext">
				<input type="text" name="molweight"
					value="<%= molweight %>" /> &#177;
				<input type="text" name="tolerance"
					value="<%= tolerance %>" />
			</td></tr>
			</table>

		<% } else if (evalConstant == NUM_RINGS) { 
			int ringsOper = Rings.NOT_EQUALS;
			int numRings = 0;
			boolean countEach = false;
			int molsOper = Rings.NOT_EQUALS;
			int numMols = 0;
			if (useInput) { // existing evaluator
				final Rings impl = new Rings(inputSubeval.codedData);
				ringsOper = impl.getRingsOper();
				numRings = impl.getNumRings();
				countEach = impl.getCountEach();
				molsOper = impl.getMolsOper();
				numMols = impl.getNumMols();
			} // useInput 
			final String[] OPER_ENGLISH = Rings.OPER_ENGLISH[FEWER];
			%>
			<table style="margin-left:auto; margin-right:auto; width:445px;
					text-align:left;" summary="">
			<tr><td class="regtext">
				If 
				<select name="countEach" onchange="javascript:changeCompareSelector();">
					<option value="all" <%= !countEach ? SELECTED : "" %>>
					the response has</option> 
					<option value="each" <%= countEach ? SELECTED : "" %>>
					the number of compounds in the response that have</option> 
				</select>
			</td></tr>
			<tr><td class="regtext">
				a number of rings that is
				<select name="ringsOper">
					<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
						<option value="<%= o %>" <%= o == ringsOper ? SELECTED : "" %> >
						<%= OPER_ENGLISH[o] %></option>
					<% } // for each operator %>
				</select>
				<input name="numRings" type="text" size="2" value="<%= numRings %>" />
			</td></tr>
			<tr><td class="regtext" id="compareSelector"
					style="visibility:<%= countEach ? "visible" : "hidden" %>;">
				is
				<select name="molsOper">
					<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
						<option value="<%= o %>" <%= o == molsOper ? SELECTED : "" %> >
						<%= OPER_ENGLISH[o] %></option>
					<% } // for each operator %>
				</select>
				<input name="numMols" type="text" size="2"
					value="<%= numMols %>" />
				<br /><br /><%= rGroupsWarning %>
			</td></tr>
			</table>

		<% } else if (evalConstant == HAS_FORMULA) {
			String formula = ""; 
			int molsOper = HasFormula.GREATER;
			if (!Question.isFormula(qType)) { 
				boolean countEach = false;
				int numMols = 0; 
				boolean withFormula = true;
				if (useInput) { // existing evaluator
					final HasFormula impl = new HasFormula(inputSubeval.codedData);
					formula = impl.getFormula();
					countEach = impl.getCountEach();
					molsOper = impl.getMolsOper();
					numMols = impl.getNumMols();
					withFormula = impl.getWithFormula();
				} // useInput %>
				<table style="margin-left:auto; margin-right:auto; width:445px; 
						text-align:left;" summary="">
				<tr><td class="regtext">
					If 
					<select name="countEach" onchange="javascript:changeCompareSelector();">
						<option value="allHas" <%= !countEach && molsOper == HasFormula.GREATER 
								? SELECTED : "" %>>the response has</option> 
						<option value="allHasnt" <%= !countEach && molsOper != HasFormula.GREATER 
								? SELECTED : "" %>>the response doesn't have</option> 
						<option value="eachHas" <%= countEach && withFormula ? SELECTED : "" %>>
						the number of compounds in the response that have</option> 
						<option value="eachHasnt" <%= countEach && !withFormula ? SELECTED : "" %>>
						the number of compounds in the response that do not have</option> 
					</select>
				</td></tr>
				<tr><td class="regtext">
					the molecular formula&nbsp;
					<input type="text" name="formula" value="<%= formula %>" size="25" />
				</td></tr>
				<tr><td class="regtext" id="compareSelector"
						style="visibility:<%= countEach ? "visible" : "hidden" %>;">
					is
					<select name="molsOper">
						<% for (int o = 0; o < HasFormula.OPER_ENGLISH[FEWER].length; o++) { %>
							<option value="<%= o %>" <%= o == molsOper ? SELECTED : "" %> >
							<%= HasFormula.OPER_ENGLISH[FEWER][o] %></option>
						<% } // for each operator %>
					</select>
					<input name="numMols" type="text" size="2" value="<%= numMols %>" />
					<% if (Question.usesSubstns(qFlags)) { %>
						<br /><br /><p style="color:green;">ACE assumes that 
						every molecule will contain one instance of each R group.</p> 
					<% } // if question uses R groups %>
				</td></tr>
			<% } else {
				if (useInput) { // existing evaluator
					final HasFormula impl = new HasFormula(inputSubeval.codedData);
					formula = impl.getFormula();
					molsOper = impl.getMolsOper();
					// countEach = false, numMols = 0, withFormula = true;
				} // useInput %>
				<table style="margin-left:auto; margin-right:auto; width:445px; 
						text-align:left;" summary="">
				<tr><td class="regtext">
					If 
					<select name="countEach">
						<option value="allHas" <%= molsOper == HasFormula.GREATER 
								? SELECTED : "" %>>the response has</option> 
						<option value="allHasnt" <%= molsOper != HasFormula.GREATER 
								? SELECTED : "" %>>the response doesn't have</option> 
					</select>
				</td></tr>
				<tr><td class="regtext">
					the molecular formula&nbsp;
					<input type="text" name="formula" value="<%= formula %>" size="25" />
				</td></tr>
			<% } // if question type is formula %>
			<tr><td class="regtext" style="color:green; padding-top:10px;">
				<br/>Use the format 
				C<sub><i>m</i></sub>H<sub><i>n</i></sub>XY<sub><i>p</i></sub>, 
				e.g., C7H5ClO2. <%= rGroupsWarning %>
				<p>Use * to match with any number in the response, including zero.</p>
				<p>Do <i>not</i> repeat elements or use parentheses, 
				as in (CH3)3COH.</p>
				<p>ACE will count D and T atoms in a structure as H atoms
				if and only if your formula does not refer to D and T. If you
				want to ensure that all H atoms in a structure are protons,
				use, e.g., CH4D0. ACE does not accept any other isotopes
				in the formula.</p> 
			</td></tr>
			</table>

		<% } else if (evalConstant == CHIRAL) { 
			int proportion = Chiral.NONE;
			// int kind = Chiral.CHIRAL;
			if (useInput) { // existing evaluator
				final Chiral impl = new Chiral(inputSubeval.codedData);
				proportion = impl.getProportion();
				// kind = impl.getKind();
			} // if useInput %>
			<table style="margin-left:auto; margin-right:auto; width:445px;
					text-align:left;" summary="">
			<tr>
			<td class="regtext">If 
				<select name="proportion" >
					<option value="<%= Chiral.NONE %>" 
						<%= proportion == Chiral.NONE ? SELECTED : "" %>>
							none</option>
					<option value="<%= Chiral.ANY %>" 
						<%= proportion == Chiral.ANY ? SELECTED : "" %>>
							any</option>
					<option value="<%= Chiral.SOME_NOT_ALL %>"
						<%= proportion == Chiral.SOME_NOT_ALL ? SELECTED : "" %>>
							some but not all </option>
					<option value="<%= Chiral.ALL %>"
						<%= proportion == Chiral.ALL ? SELECTED : "" %>>
							all</option>
				</select>
				of the enumerated stereoisomers* of the response molecules are
				<select name="kind" >
					<option value="<%= Chiral.CHIRAL %>" 
						<%= proportion == Chiral.CHIRAL ? SELECTED : "" %>>
							chiral</option>
					<option value="<%= Chiral.ACHIRAL %>"
						<%= proportion == Chiral.ACHIRAL ? SELECTED : "" %>>
							achiral</option>
				</select>
			</td>
			</tr>
			<tr><td class="regtext" style="color:green;"><br/>*Enumerated
				stereoisomers are all of the stereoisomers of a compound that can be
				generated by specifying the configurations of stereocenters whose
				configurations are not already specified. 
			</td></tr>
			</table>

		<% } else if (evalConstant == FUNCTIONAL_GROUP) { 
			int initialGroupId = -1;
			int groupOper = FnalGroup.NOT_EQUALS;
			int numGroups = 0;
			boolean countEach = false;
			int molsOper = FnalGroup.NOT_EQUALS;
			int numMols = 0;
			if (useInput) { // existing evaluator
				final FnalGroup impl = new FnalGroup(inputSubeval.codedData);
				initialGroupId = impl.getGroupId();
				groupOper = impl.getGroupOper();
				numGroups = impl.getNumGroups();
				countEach = impl.getCountEach();
				molsOper = impl.getMolsOper();
				numMols = impl.getNumMols();
			} // if useInput 
			final String[] OPER_ENGLISH = FnalGroup.OPER_ENGLISH[FEWER];
			%>
			<input type="hidden" name="initGroupId" value="<%= initialGroupId %>" />
			<table class="regtext" style="margin-left:auto; text-align:left;
					margin-right:auto; width:445px;" summary="">
			<%= rGroupsWarning %>
			<tr><td>
				If 
				<select name="countEach" onchange="javascript:changeCompareSelector();">
					<option value="all" <%= !countEach ? SELECTED : "" %>>
					the response has</option> 
					<option value="each" <%= countEach ? SELECTED : "" %>>
					the number of compounds in the response that have</option> 
				</select>
				<select name="groupOper">
				<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
					<option value="<%= o %>" <%= o == groupOper ? SELECTED : "" %> >
					<%= OPER_ENGLISH[o] %></option>
				<% } %>
				</select>
				<input name="numGroups" type="text" size="2"
						value="<%= numGroups %>" />
			</td></tr>
			<tr><td>
				instance(s) of the functional group chosen below
			</td></tr>
			<tr><td class="regtext" id="compareSelector"
					style="visibility:<%= countEach ? "visible" : "hidden" %>;">
				is
				<select name="molsOper">
					<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
						<option value="<%= o %>" <%= o == molsOper ? SELECTED : "" %> >
						<%= OPER_ENGLISH[o] %></option>
					<% } // for each operator %>
				</select>
				<input name="numMols" type="text" size="2"
					value="<%= numMols %>" />
				<% if (Question.usesSubstns(qFlags)) { %>
					<br /><br /><p style="color:green;">ACE assumes that 
					every molecule will contain one instance of each R group.</p> 
				<% } // if question uses R groups %>
			</td></tr>
			<tr><td class="regtext" style="color:green; padding-top:20px;">
				Choose a functional group from a list of
				<%= sortedGroups.length %>. Each group is found in
				only one category; if you don't find it where you're looking, look
				elsewhere. For example, the aryl bromide functional group is in the
				Halogen-containing category, not the Aromatics category. 
			</td></tr>
			<tr><td style="padding-top:20px;">
				<b>Select a functional group category:</b>
				<select name="categories" id="categories" 
						size="1" style="width:300px;" 
						onchange="loadGroupsInChangedCat(document.evaluatorForm)" >
				</select>
			</td></tr>
			<tr><td id="categoryWarning" style="color:red;">&nbsp;</td></tr>
			<tr><td>
				&nbsp;<b>Select a functional group:</b>
				<span id="groupsPopup">
				<select name="fnalGroupId" size="1" style="width:300px;">
					<option value="0">No group selected</option>
				</select>
				</span>
			</td></tr>
			</table>

		<% } else if (evalConstant == CONFORMATION_ACYCLIC) { 
			String formula1 = ""; 
			String formula2 = "";
			int rel = ConformBond.ANTI; 
			if (useInput) { // existing evaluator
				final ConformBond impl = new ConformBond(inputSubeval.codedData);
				formula1 = new String(impl.getFormula1());
				formula2 = new String(impl.getFormula2());
				rel = impl.getGroupRelationship();
			} // useInput %>
			<table style="margin-left:auto; margin-right:auto; width:445px; 
					text-align:left;" summary="">
			<tr><td class="regtext">
				<br/>Enter the groups in the text boxes below.
				You may use element symbols for simgle atoms; for groups, use 
				standard abbreviations (like <strong>iPr</strong> or 
				<strong>Ph</strong>) or SMILES representations (like 
				<strong>CC(Cl)CC</strong>, with no numerals and no bound H 
				atoms). In the last case, the first atom in the string is
				assumed to be attached to the ring. Omit H atoms in SMILES 
				representations; for example, an OH group should be written as O.
				<br/><br/><br/>
				If the &nbsp; 
				<input type="text" name="formula1"
					value="<%= formula1 %>" size="40"/> &nbsp;
					group and the <br/>
				<input type="text" name="formula2"
					value="<%= formula2 %>" size="40"/> &nbsp;
				group are &nbsp; <br/>
				<select name="groupRelation">
					<% for (int grNum = 1; 
							grNum < ConformBond.DB_VALUES.length; grNum++) { %>
					<option value="<%= grNum %>" <%= rel == grNum ? SELECTED : "" %> >
						<%= ConformBond.DB_VALUES[grNum] %></option>
					<% } // for each relationship %>
				</select>
			</td></tr>
			</table>
				
		<% } else if (evalConstant == CONFORMATION_CHAIR) { 
			String formula = "";
			int oper = ConformChair.EQUALS;
			int orientation = ConformChair.EQUATORIAL;
			int number = 1; 
			boolean overrideForIndeterminate = true;
			if (useInput) { // existing evaluator
				final ConformChair impl = new ConformChair(inputSubeval.codedData);
				formula = new String(impl.getFormula());
				oper = impl.getOper();
				number = impl.getNumber();
				orientation = impl.getOrientation();
				overrideForIndeterminate = impl.getOverrideFor();
			} // useInput %>
			<input type="hidden" name="overrideFor" value="" />
			<table style="margin-left:auto; margin-right:auto; width:445px; 
					text-align:left;" summary="">
			<tr><td class="regtext" style="color:green;">
				Precede this evaluator with one that ascertains that the 
				response has the correct configuration. For a 2D chair 
				projection, use, "If the 2D chair drawing represents ...;" 
				for a 3D chair, use, "If the structure (or its enantiomer, 
				...."
			</td></tr>
			<tr><td class="regtext" style="padding-top:20px;">
				If the number of 
				<select name="orientation">
					<option value="<%= ConformChair.AXIAL %>" 
							<%= orientation == ConformChair.AXIAL 
									? SELECTED : "" %> >axial</option>
					<option value="<%= ConformChair.EQUATORIAL %>" 
							<%= orientation == ConformChair.EQUATORIAL 
									? SELECTED : "" %> >equatorial</option>
				</select> 
				<input type="text" name="formula" value="<%= formula %>" size="25" />
				groups is 
				<select name="oper">
					<% for (int o = 0; o < ConformChair.OPER_ENGLISH[FEWER].length; o++) { %>
						<option value="<%= o %>" <%= o == oper ? SELECTED : "" %> >
						<%= ConformChair.OPER_ENGLISH[FEWER][o] %></option>
					<% } %>
				</select>
				&nbsp;
				<select name="molCount">
				<% for (int idx = 0; idx <= 6; idx++) { %>
					<option value="<%= idx %>" <%= number == idx 
							? SELECTED : "" %> ><%= idx %></option>
				<% } %>
				</select>
				<br/>
			</td></tr>
			<tr><td>
				<table><tr><td style="padding-right:10px; vertical-align:top;">
					<input type="checkbox" name="overrideForIndeterminate" <%=
							overrideForIndeterminate ? CHECKED : "" %> />
				</td><td>
					 Override my feedback with automatically generated feedback
					 if ACE determines that a group is neither axial nor equatorial.
				</td></tr></table>
			</td></tr>
			<tr><td class="regtext" style="padding-top:10px; color:green;">
				You may use element symbols for single atoms; for groups, use 
				standard abbreviations (such as <strong>iPr</strong> or 
				<strong>Ph</strong>) or SMILES representations (such as 
				<strong>CC(Cl)CC</strong>, with no numerals and no bound H 
				atoms). In the case of SMILES representations, ACE assumes
				that the first atom in the string is attached to the ring. 
			</td></tr>
			</table>

		<% } else if (Utils.among(evalConstant, IS_OR_HAS_SIGMA_NETWORK, IS_2D_CHAIR)) { 
			int howMany = Is.ONLY;
			int flags = 0;
			if (useInput) { // an existing evaluator 
				// Is and Is2DChair use same coded data
				final Is impl = new Is(inputSubeval.codedData);
				howMany = impl.getHowMany();
				flags = impl.getFlags();
			} // useInput 
			final boolean is2DChair = evalConstant == IS_2D_CHAIR;
			final String[] HOWMANY_ENGL = (is2DChair
					? Is2DChair.HOWMANY_ENGL : Is.HOWMANY_ENGL); 
			final int colspan = (is2DChair ? 1 : 2);
			%>
			<input type="hidden" name="molstruct" value="" />
			<input type="hidden" name="molname" value="" />
			<% if (!is2DChair) { %>
				<input type="hidden" name="flags" value="" /> 
			<% } // if is not 2D chair evaluator %>
			<table class="regtext" style="margin-left:auto; 
					margin-right:auto; width:445px;" summary="">
			<% if (is2DChair) { %>
				<tr><td class="regtext" style="color:green;">
					This evaluator evaluates a 2D projection of a chair, 
					not a 3D chair, so do not check the "3D mode" box
					of a question that uses this evaluator.
				</td></tr>
			<% } // if is 2D chair evaluator %>
			<tr><td>
				If 
				<select name="howMany">
					<% for (int o = 1; o <= HOWMANY_ENGL.length; o++) { %>
						<option value="<%= o %>" <%= o == howMany 
								? SELECTED : "" %> >
						<%= HOWMANY_ENGL[o - 1] %></option>
					<% } %>
				</select>
				<% if (is2DChair) { %>
					2D chair in the response represents
					<select name="flags">
						<option value="0" <%= (flags & Is.EITHER_ENANTIOMER) == 0
								? SELECTED : "" %> > exactly </option>
						<option value="<%= Is.EITHER_ENANTIOMER %>" <%= 
								(flags & Is.EITHER_ENANTIOMER) != 0
									? SELECTED : "" %> > either enantiomer of </option>
					</select>
				<% } else { %>
					compound in the response is
					</td><td style="text-align:right;">
						<a href="#options" style="color:green;">[options below]</a>
				<% } // if is 2D chair evaluator %>
			</td></tr>
			<tr><td colspan="<%= colspan %>" class="regtext" style="width:90%;">
				<input type="text" name="inputmolname" size="55"
					value="<%= Utils.toValidTextbox(inputSubeval.molName) %>" />
			</td></tr>
			<tr><td colspan="<%= colspan %>" class="boldtext" style="text-align:right; 
					padding-left:10px; padding-right:10px; padding-top:10px; 
					font-style:italic;">
				MarvinJS&trade;
			</td></tr>
			<tr><td colspan="<%= colspan %>" style="text-align:center; 
					padding-left:10px; padding-right:10px;">
				<table class="whiteTable" summary=""><tr><td>
					<div id="<%= APPLET_NAME %>">
					<script type="text/javascript">
					// <!-- >
						startMarvinJS('<%= Utils.toValidJS(
									inputSubeval.molStruct) %>', 
								MARVIN, 0, '<%= APPLET_NAME %>', 
								'<%= pathToRoot %>'); 
					// -->
					</script>
					</div>
				</td></tr></table>
			</td></tr>
			<% if (!is2DChair) { %>
				<tr><td colspan="<%= colspan %>" class="regtext" 
						style="vertical-align:middle;">
					<br/><input type="checkbox" name="enantiomer"
							value="<%= Is.EITHER_ENANTIOMER %>"
							<%= (flags & Is.EITHER_ENANTIOMER) != 0 ? CHECKED : "" %> />
					check both enantiomers
					<br/><input type="checkbox" name="resonance"
							value="<%= Is.RESONANCE_PERMISSIVE %>"
							<%= (flags & Is.RESONANCE_PERMISSIVE) != 0 ? CHECKED : "" %> />
					check for resonance structures as well
					<br/><input type="checkbox" name="sigmanetwork"
							value="<%= Is.SIGMA_NETWORK %>"
							<%= (flags & Is.SIGMA_NETWORK) != 0 ? CHECKED : "" %> />
					check for identity of &sigma;-bond networks only
					<br/><input type="checkbox" name="normalization"
							value="<%= Is.NO_NORMALIZATION %>"
							<%= (flags & Is.NO_NORMALIZATION) != 0 ? CHECKED : "" %> />
					eschew normalization (aromatization and ylide standardization)
					<br/><input type="checkbox" name="isotopeLenient"
							value="<%= Is.ISOTOPE_LENIENT %>"
							<%= (flags & Is.ISOTOPE_LENIENT) != 0 ? CHECKED : "" %> />
					allow isotopes in response to match nonisotopes
					<a name="options"/>&nbsp;
				</td></tr>
			<% } // if is not 2D chair %>
			</table>

		<% } else if (evalConstant == SKELETON_SUBSTRUCTURE) { 
			int howMany = Contains.NONE;
			int method = Contains.SUBSTRUCT;
			int chgRadIso = Contains.EXACT;
			if (useInput) { // existing evaluator 
				final Contains impl = new Contains(inputSubeval.codedData);
				howMany = impl.getHowMany();
				method = impl.getMethod();
				chgRadIso = impl.getChgRadIso();
			} // if useInput %>
			<input type="hidden" name="molstruct" value="" />
			<input type="hidden" name="molname" value="" />
			<table class="regtext" style="margin-left:auto; 
					margin-right:auto; width:445px;" summary="">
			<%= rGroupsWarning %>
			<tr><td>
				If 
				<select name="howMany" >
					<% for (int o = 1; o <= Contains.HOWMANY_ENGL.length; o++) { %>
						<option value="<%= o %>"
							<%= o == howMany ? SELECTED : "" %> >
						<%= Contains.HOWMANY_ENGL[o - 1] %></option>
					<% } %>
				</select>
				compound in the response contains the 
				<select name="method" >
					<option value="<%= Contains.SUBSTRUCT %>"
						<%= method == Contains.SUBSTRUCT ? SELECTED : "" %>
						> substructure</option>
					<option value="<%= Contains.SKELETON %>"
						<%= method == Contains.SKELETON ? SELECTED : "" %>
						> skeleton</option>
				</select>
				<br/>
				<select name="chgRadIso" >
					<option value="<%= Contains.EXACT %>"
						<%= chgRadIso == Contains.EXACT ? SELECTED : "" %>
						> with charge, radical, isotope states matching exactly </option>
					<option value="<%= Contains.IGNORE %>"
						<%= chgRadIso == Contains.IGNORE ? SELECTED : "" %>
						> with charge, radical, isotope states ignored </option>
					<option value="<%= Contains.DEFAULT %>"
						<%= chgRadIso == Contains.DEFAULT ? SELECTED : "" %>
						> with charge, radical, isotope states in author's structure 
						present in response </option>
				</select>
			</td></tr>
			<tr>
				<td class="regtext" style="width:90%;">
					<input type="text" name="inputmolname" size="55"
						value="<%= Utils.toValidTextbox(inputSubeval.molName) %>" />
				</td>
			</tr>
			<tr><td class="boldtext" style="text-align:right; padding-left:10px;
					padding-right:10px; padding-top:10px; font-style:italic;">
				MarvinJS&trade;
			</td></tr>
			<tr><td style="text-align:center; padding-left:10px; padding-right:10px;">
				<table class="whiteTable" summary=""><tr><td>
					<div id="<%= APPLET_NAME %>">
					<script type="text/javascript">
						// <!-- >
						startMarvinJS('<%= Utils.toValidJS(
									inputSubeval.molStruct) %>', 
								MARVIN, SHOWNOH, 
								'<%= APPLET_NAME %>', '<%= pathToRoot %>');
						// -->
					</script>
					</div>
 				</td></tr></table>
			</td></tr>
			</table>
		
		<% } else if (evalConstant == BOND_ANGLE) { 
			int authAngle = 109;
			int tolerance = 15;
			int oper = BondAngle.EQUALS;	
			if (useInput) { // an existing evaluator
				final BondAngle impl = new BondAngle(inputSubeval.codedData);
				authAngle = impl.getAuthAngle();
				tolerance = impl.getTolerance();
				oper = impl.getOper();
			} // if useInput %>
			<input type="hidden" name="molstruct" value="" />
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td>
				<span class="regtext">If the bond angle of mapped atoms 1-2-3 is
				<select name="oper">
				<% for (int o = 0; o < BondAngle.OPER_ENGLISH[LESSER].length; o++) { %>
					<option value="<%= o %>" <%= o == oper ? SELECTED : "" %> >
					<%= BondAngle.OPER_ENGLISH[LESSER][o] %></option>
				<% } %>
				</select>
				</span>
			</td></tr>
			<tr><td class="regtext">
				<input type="text" name="authAngle" size="20"
					value="<%= authAngle %>" /> &#177;
				<input type="text" name="tolerance" size="10"
					value="<%= tolerance %>" /> 
				degrees.
			</td></tr>
			<tr><td class="regtext">&nbsp;</td></tr>
			<tr><td class="regtext" style="padding-top:10px; color:green;">
				Label three contiguous atoms with map numbers 1, 2, and 3, with 
				atom 2 as the atom about which the bond angle should be measured.
			</td></tr>
			<tr><td class="boldtext" style="text-align:right; padding-left:10px;
					padding-right:10px; padding-top:10px; font-style:italic;">
				Marvin JS&trade;
			</td></tr>
			<tr><td style="text-align:center; padding-left:10px; padding-right:10px;">
				<table class="whiteTable" summary=""><tr><td>
					<div id="<%= APPLET_NAME %>">
					<script type="text/javascript">
						// <!-- >
						startMarvinJS('<%= Utils.toValidJS(
									inputSubeval.molStruct) %>', 
								MARVIN, SHOWMAPPING, 
								'<%= APPLET_NAME %>', '<%= pathToRoot %>');
						// -->
					</script>
					</div>
				</td></tr>
				</table>
			</td></tr>
			</table>

		<% } else if (evalConstant == FORMULA_FORMAT) { 
			boolean isPositive = false;
			int rule = FormulaFormat.CAPITALIZATION; 
			if (useInput) { // existing evaluator
				final FormulaFormat impl = new FormulaFormat(inputSubeval.codedData);
				isPositive = impl.getIsPositive();
				rule = impl.getRule();
			} // useInput %>
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<table class="regtext" style="margin-left:auto; 
					margin-right:auto; width:445px;" summary="">
			<tr><td class="regtext" style="color:green;">
				[ACE will generate automatic feedback for formulas that violate 
				the chosen rule. ACE will append your feedback.]
			</td></tr>
			<tr><td>
				<br/>
				If the response formula <span id="isPosSelector"></span> this rule:
				<select id="rule" name="rule">
					<option value="<%= FormulaFormat.CAPITALIZATION %>" 
							<%= rule == FormulaFormat.CAPITALIZATION ? SELECTED : "" %> >
						<%= FormulaFormat.RULES_TEXT[FormulaFormat.CAPITALIZATION] %>
					</option>
					<option value="<%= FormulaFormat.HILL_ORDER %>" 
							<%= rule == FormulaFormat.HILL_ORDER ? SELECTED : "" %> >
						<%= FormulaFormat.RULES_TEXT[FormulaFormat.HILL_ORDER] %>
					</option>
					<option value="<%= FormulaFormat.EXPLICIT_1 %>" 
							<%= rule == FormulaFormat.EXPLICIT_1 ? SELECTED : "" %> >
						<%= FormulaFormat.RULES_TEXT[FormulaFormat.EXPLICIT_1] %>
					</option>
				</select>
			</td></tr>
			</table>

		<% } else if (evalConstant == MAPPED_ATOMS) { 
			boolean isPositive = false;
			int oper = MapProperty.ATLEAST;
			boolean patternOnly = false;
			boolean checkEnantiomer = false;
			boolean aromatize = true;
			if (useInput) { // an existing evaluator
				final MapProperty impl = new MapProperty(inputSubeval.codedData);
				isPositive = impl.getIsPositive();
				oper = impl.getOper();
				patternOnly = impl.getPatternOnly();
				checkEnantiomer = impl.getCheckEnant();
				aromatize = impl.getAromatize();
			} // useInput 
			final String selectionsStr = 
					ChemUtils.getSelectionsStr(inputSubeval.molStruct);
			%>
			<input type="hidden" name="molstruct" value="" />
			<input type="hidden" name="toBe" value="<%= patternOnly ? "is" : "are" %>" />
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<input type="hidden" name="selectionsStr" value="<%= selectionsStr %>" />
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td>
				<span class="regtext">If the atom 
				<select name="patternOnly" onchange="changeSelectText()">
					<option value="false" <%= !patternOnly ? SELECTED : "" %> >
						mapping numbers or selections</option>
					<option value="true" <%= patternOnly ? SELECTED : "" %> >
						mapping pattern</option>
				</select>
				of the
				<select name="checkEnantiomer">
					<option value="false" <%= !checkEnantiomer ? SELECTED : "" %> >
						response</option>
					<option value="true" <%= checkEnantiomer ? SELECTED : "" %> >
						response or its enantiomer</option>
				</select>
				<select name="aromatize">
					<option value="true" <%= aromatize ? SELECTED : "" %> >
						with</option>
					<option value="false" <%= !aromatize ? SELECTED : "" %> >
						without</option>
				</select> 
				aromatization
				<span id="isPosSelector"></span>
				<select name="oper">
					<option value="1" <%= oper == 1 ? SELECTED : "" %> >
						at least</option>
					<option value="2" <%= oper == 2 ? SELECTED : "" %> >
						exactly</option>
				</select></span>:
			</td></tr>
			<tr><td class="boldtext" style="text-align:right; padding-left:10px;
					padding-right:10px; font-style:italic;">
				MarvinJS&trade;
			</td></tr>
			<tr><td style="text-align:center; padding-left:10px; 
					padding-right:10px;">
				<table class="whiteTable" summary=""><tr><td>
					<div id="<%= APPLET_NAME %>">
					<script type="text/javascript">
						// <!-- >
						startMarvinJS('<%= Utils.toValidJS(inputSubeval.molStruct) %>', 
								MARVIN, SHOWMAPPING, 
								'<%= APPLET_NAME %>', '<%= pathToRoot %>');
						// -->
					</script>
					</div>
				</td></tr></table>
			</td></tr>
			</td></tr>
			</table>

		<% } else if (evalConstant == MAPPED_COUNT) { 
			boolean checkEnantiomer = false;
			boolean aromatize = true;
			String matchPtsStr = "0.25";
			String mismatchPtsStr = "0.25";
			if (useInput) { // an existing evaluator
				final MapSelectionsCounter impl = 
						new MapSelectionsCounter(inputSubeval.codedData);
				checkEnantiomer = impl.getCheckEnant();
				aromatize = impl.getAromatize();
				matchPtsStr = impl.getMatchPtsStr();
				mismatchPtsStr = impl.getMismatchPtsStr();
			} // useInput 
			final String selectionsStr = 
					ChemUtils.getSelectionsStr(inputSubeval.molStruct);
			%>
			<input type="hidden" name="molstruct" value="" />
			<input type="hidden" name="selectionsStr" value="<%= selectionsStr %>" />
			<input type="hidden" name="checkEnantiomer" value="" />
			<input type="hidden" name="aromatize" value="" />
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext" style="color:green;">
				[ACE will generate automatic feedback explaining how it
				calculated the student's grade. ACE will append your feedback.]
			</td></tr>
			<tr><td class="regtext" style="padding-bottom:10px;">
				Add <input type="text" name="matchPtsStr" id="matchPtsStr"
						size="5" value="<%= matchPtsStr %>" /> 
				and subtract 
				<input type="text" name="mismatchPtsStr" id="mismatchPtsStr"
						size="5" value="<%= mismatchPtsStr %>" /> 
				points for each selection of the response that matches or 
				does not match the author's selections, respectively 
			</td></tr>
			<tr><td class="boldtext" style="text-align:right; padding-left:10px;
					padding-right:10px; font-style:italic;">
				MarvinJS&trade;
			</td></tr>
			<tr><td style="text-align:center; padding-left:10px; 
					padding-right:10px;">
				<table class="whiteTable" summary=""><tr><td>
					<div id="<%= APPLET_NAME %>">
					<script type="text/javascript">
						// <!-- >
						startMarvinJS('<%= Utils.toValidJS(inputSubeval.molStruct) %>', 
								MARVIN, SHOWMAPPING, 
								'<%= APPLET_NAME %>', '<%= pathToRoot %>');
						// -->
					</script>
					</div>
				</td></tr></table>
			</td></tr>
			<tr><td class="regtext">
				<input type="checkbox" name="checkEnant" <%= checkEnantiomer 
						? CHECKED : "" %> /> Match to the enantiomer as well
				<br/><input type="checkbox" name="skipAromatization" <%= aromatize 
						? "" : CHECKED %> /> Eschew aromatization
			</td></tr>
			</table>

		<% } else if (evalConstant == LEWIS_ELECTRON_DEFICIENT) { 
			int oper = LewisElecDeficCt.NOT_EQUALS;
			int number = 0;
			String element = "";
			if (useInput) { // existing evaluator
				final LewisElecDeficCt impl = 
						new LewisElecDeficCt(inputSubeval.codedData);
				oper = impl.getOper();
				number = impl.getNumber();
				element = impl.getElement();
			} // useInput %>
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
				<tr><td class="regtext" style="color:green;">
					[Enter X for any atom.]<br/>	
				</td></tr>
				<tr><td class="regtext">If the number of electron-deficient	
					<input name="element" type="text" size="2" 
						value="<%= element %>" />
					atoms is <br/>
					<select name="oper">
						<% for (int o = 0; 
								o < LewisElecDeficCt.OPER_ENGLISH[FEWER].length; 
								o++) { %>
							<option value="<%= o %>"
								<%= o == oper ? SELECTED : "" %> >
								<%= LewisElecDeficCt.OPER_ENGLISH[FEWER][o] %>
							</option>
						<% } %>
					</select>
					<input name="number" type="text" size="2"
						value="<%= number %>" />
				</td></tr>
			</table>

		<% } else if (evalConstant == LEWIS_FORMAL_CHGS) { 
			boolean isPositive = false;	
			if (useInput) { 
				final LewisFormalCharge impl = 
						new LewisFormalCharge(inputSubeval.codedData);
				isPositive = impl.getIsPositive();
			} // useInput %> 
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext" colspan="2">
				<br/>
				If the formal charge of
			</td></tr>
			<tr><td class="regtext" width="20">&nbsp;</td><td>
				<input name="isPositive" type="radio" value="true"
					<%= isPositive ? CHECKED : "" %> /> 
					every atom is correct
				<br/>
				<input name="isPositive" type="radio" value="false"
					<%= !isPositive ? CHECKED : "" %> /> 
					any atom is incorrect
			</td></tr>
			</table>

		<% } else if (evalConstant == LEWIS_ISOMORPHIC) { 
			boolean isPositive = false; 
			if (useInput) { // existing evaluator
				final LewisIsomorph impl = new LewisIsomorph(inputSubeval.codedData); 
				isPositive = impl.getIsPositive();		
			} // usesInput %>
			<input type="hidden" name="molstruct" value="" />
			<input type="hidden" name="molname" value="" />
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<table style="margin-left:auto; margin-right:auto; width:445px; 
					text-align:left;" summary="">
			<tr><td class="regtext" colspan="2">
				If the Lewis structure of the response is 
				<span id="isPosSelector"></span>
			</td></tr>
			<tr>
				<td class="regtext" style="width:90%;">
					Name: <input type="text" name="inputmolname" size="55"
						value="<%= Utils.toValidTextbox(inputSubeval.molName) %>" />
				</td>
			</tr>
			<tr><td class="boldtext" style="text-align:right; padding-left:10px;
				padding-right:10px; padding-top:10px; font-style:italic;">
				Lewis JS&trade;
			</td></tr>
			<tr><td style="text-align:center; padding-left:10px; padding-right:10px;">
				<table class="rowsTable" 
						style="margin-left:auto; margin-right:auto; 
							width:<%= LewisMolecule.CANVAS_WIDTH %>px;">
				<tr><td style="width:100%;" id="lewisJSToolbars">
				</td></tr>
				<tr><td style="width:100%;"><div id="lewisJSCanvas" 
						style="position:relative;height:<%= 
							LewisMolecule.CANVAS_HEIGHT %>px;width:100%;"></div>
				</td></tr>
				</table>
			</td></tr>
			</table>
		
		<% } else if (evalConstant == LEWIS_OUTER_SHELL_COUNT) { 
			int oper = LewisOuterNumber.LESS;
			int number = 8;
			String element = "";
			boolean isPositive = false; 
			int condition = LewisOuterNumber.ANY_GREATER; 
			/* 	1 (EVERY_NOT_GREATER): every atom not greater than the maximum
				2 (ANY_GREATER): any atom is greater than the maximum
				3 (ELEMENT_OPER): [any/every] [X] atom is [</>/=/..][number]
			*/
			if (useInput) { // existing evaluator
				final LewisOuterNumber impl = 
						new LewisOuterNumber(inputSubeval.codedData);
				condition = impl.getCondNumber();
				if (condition == LewisOuterNumber.ELEMENT_OPER) {
					oper = impl.getOper();
					number = impl.getNumber();
					element = impl.getElement();
					isPositive = impl.getIsPositive();
				} // condition type ELEMENT_OPER 
			} // useInput %>
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
				<tr><td class="regtext" colspan="2">
					<br/>If the number of electrons in the outer shell of
				</td></tr>
				<tr><td width="20">&nbsp;</td><td>
					<input name="condition" type="radio" 
							value="<%= LewisOuterNumber.EVERY_NOT_GREATER %>"
								<%= (condition == LewisOuterNumber.EVERY_NOT_GREATER) 
									? CHECKED : "" %> /> 
					every atom is not greater than the maximum
					<br/>
						<input name="condition" type="radio" 
							value="<%= LewisOuterNumber.ANY_GREATER %>"
								<%= (condition == LewisOuterNumber.ANY_GREATER) 
									? CHECKED : "" %> /> 
					any atom is greater than the maximum		
					<br/>		
					<input name="condition" type="radio" 
							value="<%= LewisOuterNumber.ELEMENT_OPER %>"
							<%= (condition == LewisOuterNumber.ELEMENT_OPER) 
									? CHECKED : "" %> />
					<span id="isPosSelector"></span>
					<input name="element" type="text" size="2"
						value="<%= element %>" />
					atom is
					<select name="oper" >
						<% for (int o = 0; 
								o < LewisOuterNumber.OPER_ENGLISH[FEWER].length; 
								o++) { %>
							<option value="<%= o %>" <%= o == oper 
									? SELECTED : "" %> >
							<%= LewisOuterNumber.OPER_ENGLISH[FEWER][o] %></option>
						<% } %>
					</select>
					<input name="number" type="text" size="2"
						value="<%= number %>" />
				</td></tr>
			</table>

		<% } else if (evalConstant == LEWIS_VALENCE_ELECS) { 
			boolean isPositive = false;
			if (useInput) { // existing evaluator
				final LewisValenceTotal impl = 
						new LewisValenceTotal(inputSubeval.codedData);
				isPositive = impl.getIsPositive();
			} // useInput %>
			<table style="margin-left:auto; margin-right:auto; width:445px; 
					text-align:left;" summary="">
			<tr><td class="regtext" colspan="2">
				<br/>If the total number of valence electrons shown:
			</td></tr>
			<tr><td class="regtext" width="20">&nbsp;</td><td>
				<input name="isPositive" type="radio" value="true"
						<%= isPositive ? CHECKED : ""%> />
				equals the number indicated by the formula and charge
				<br/>
				<input name="isPositive" type="radio" value="false"
						<%= isPositive ? "" : CHECKED %> />
				does not equal the number indicated by the formula and charge
			</td></tr>
			</table>

		<% } else if (evalConstant == MECH_EQUALS) { 
			boolean isPositive = false; 
			if (useInput) { // existing evaluator
				final MechEquals impl = new MechEquals(inputSubeval.codedData); 
				isPositive = impl.getIsPositive();		
			} // usesInput %>
			<input type="hidden" name="molstruct" value="" />
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<table style="margin-left:auto; margin-right:auto; width:445px; 
					text-align:left;" summary="">
			<tr><td class="regtext" colspan="2">
				If the mechanism of the response
				<span id="isPosSelector"></span>
				the following mechanism exactly and in every respect 
				(electron-flow arrows, resonance structures, presence or 
				absence of bases and coproducts):
			</td></tr>
			<tr><td class="boldtext" style="text-align:right; padding-left:10px;
					padding-right:10px; padding-top:10px; font-style:italic;">
				MarvinJS&trade;
			</td></tr>
			<tr><td style="text-align:center; padding-left:10px; 
					padding-right:10px;">
				<table class="whiteTable" summary=""><tr><td>
					<div id="<%= APPLET_NAME %>">
					<script type="text/javascript">
						// <!-- >
						startMarvinJS('<%= Utils.toValidJS(
									inputSubeval.molStruct) %>', 
								MECHANISM, SHOWLONEPAIRS, '<%= APPLET_NAME %>', 
								'<%= pathToRoot %>');
						// -->
					</script>
					</div>
				</td></tr></table>
			</td></tr>
			</table>

		<% } else if (evalConstant == MECH_FLOWS) { 
			boolean isPositive = false;
			int flags = MechSet.RESON_LENIENT;
			if (useInput) { // existing evaluator
				final MechFlowsValid impl = new MechFlowsValid(inputSubeval.codedData);
				isPositive = impl.getIsPositive();
				flags = impl.getFlags();
			} // useInput %>
			<input type="hidden" name="molstruct" value="" />
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext" style="color:green;">
				[ACE will generate automatic feedback for mechanisms with 
				incorrect electron-flow arrows. ACE will append your feedback.]
			</td></tr>
			<tr><td class="regtext" style="padding-top:10px;">
				If the electron-flow arrows in <span id="isPosSelector"></span>
				lead to the compounds in the following step, 
				excluding the following starting materials:
				<table style="margin-left:auto; margin-right:auto; 
						width:445px; text-align:left;" summary="">
				<tr><td class="boldtext" style="text-align:right; padding-left:10px;
						padding-right:10px; padding-top:10px; font-style:italic;">
					MarvinJS&trade;
				</td></tr>
				<tr><td style="text-align:center; padding-left:10px; 
						padding-right:10px;">
					<table class="whiteTable" summary=""><tr><td>
						<div id="<%= APPLET_NAME %>">
						<script type="text/javascript">
						// <!-- >
							startMarvinJS('<%= Utils.toValidJS(
										inputSubeval.molStruct) %>', 
									-MECHANISM, 0, '<%= APPLET_NAME %>', 
									'<%= pathToRoot %>');
						// -->
						</script>
						</div>
					</td></tr></table>
				</td></tr>
				<tr><td class="regtext">
					<table summary="">
					<tr><td>
					<input type="checkbox" name="resonanceLenient" value="yes"
						<%= (flags & MechSet.RESON_LENIENT) != 0 ? CHECKED : "" %> />
					</td><td>
					response may contain any resonance structure of the given
					starting materials
					</td></tr>
					<tr><td>
					<input type="checkbox" name="stereoLenient" value="yes"
						<%= (flags & MechSet.STEREO_LENIENT) != 0 ? CHECKED : "" %> />
					</td><td>
					ignore stereochemistry errors
					</td></tr>
					</table>
				</td></tr>
				</table>
			</td></tr>
			</table>

		<% } else if (evalConstant == MECH_INIT) { 
			boolean isPositive = false;
			int flags = MechSet.RESON_LENIENT;
			if (useInput) { // existing evaluator
				final MechInitiation impl = new MechInitiation(inputSubeval.codedData);
				isPositive = impl.getIsPositive();
				flags = impl.getFlags();
			} // useInput %>
			<input type="hidden" name="molstruct" value="" />
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext" style="color:green;">
				[ACE will generate automatic feedback for mechanisms with 
				incorrect use of initiators. ACE will append your feedback.]
			</td></tr>
			<tr><td class="regtext" style="padding-top:10px;">
				If the initiation part of the mechanism 
				<span id="isPosSelector"></span>
				drawn correctly 
				<br /><br />
				A correctly drawn initiation part is present and excludes the 
				following initiator and atoms derived from it from the 
				propagation part of the mechanism:
				<table style="margin-left:auto; margin-right:auto; 
						width:445px; text-align:left;" summary="">
				<tr><td class="boldtext" style="text-align:right; padding-left:10px;
						padding-right:10px; padding-top:10px; font-style:italic;">
					MarvinJS&trade;
				</td></tr>
				<tr><td style="text-align:center; padding-left:10px; 
						padding-right:10px;">
					<table class="whiteTable" summary=""><tr><td>
						<div id="<%= APPLET_NAME %>">
						<script type="text/javascript">
						// <!-- >
							startMarvinJS('<%= Utils.toValidJS(
										inputSubeval.molStruct) %>', 
									-MECHANISM, 0, '<%= APPLET_NAME %>', 
									'<%= pathToRoot %>');
						// -->
						</script>
						</div>
					</td></tr></table>
				</td></tr>
				<tr><td class="regtext">
					<table summary="">
					<tr><td>
					<input type="checkbox" name="resonanceLenient" value="yes"
						<%= (flags & MechSet.RESON_LENIENT) != 0 ? CHECKED : "" %> />
					</td><td>
					response may contain any resonance structure of the given
					starting materials
					</td></tr>
					<tr><td>
					<input type="checkbox" name="stereoLenient" value="yes"
						<%= (flags & MechSet.STEREO_LENIENT) != 0 ? CHECKED : "" %> />
					</td><td>
					ignore stereochemistry errors
					</td></tr>
					</table>
				</td></tr>
				</table>
			</td></tr>
			</table>

		<% } else if (evalConstant == MECH_PIECES_COUNT) { 
			int component = MechCounter.REACTION_ARROWS;
			int oper = MechCounter.LESS;
			int limit = 4;
			double decrement = 0;
			if (useInput) { // existing evaluator
				final MechCounter impl = new MechCounter(inputSubeval.codedData);
				component = impl.getComponent();
				oper = impl.getOper();
				limit = impl.getLimit();
				decrement = impl.getDecrement();
			} // useInput %>
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext">
				If the number of
				<select name="component" >
					<% for (int c = 1; 
							c <= MechCounter.NUM_COMPONENTS; c++) { %>
						<option value="<%= c %>" 
								<%= c == component ? SELECTED : "" %> >
							<%= MechCounter.COMPONENT_ENGLISH[c] %>
						</option>
					<% } %>
				</select>
			</td></tr>
			<tr><td class="regtext">
				is
				<select name="oper" onchange="changeDecrementCell();">
					<% for (int o = 0; 
							o < MechCounter.OPER_ENGLISH[FEWER].length; o++) { %>
						<option value="<%= o %>" 
								<%= o == oper ? SELECTED : "" %> >
						<%= MechCounter.OPER_ENGLISH[FEWER][o] %>
						</option>
					<% } %>
				</select>
				<input name="limit" type="text" size="5"
					value="<%= limit %>" />
			</td></tr>
			<tr><td id="decrementCell" class="regtext" style="padding-top:10px;<%=
					!Utils.among(oper, MechCounter.GREATER, MechCounter.NOT_LESS) 
					? " visibility:hidden;" : "" %>">
				Amount by which to decrease the grade for each item over the limit
				(enter 0 to disable this feature, otherwise enter a number between
				0 and 1):
				<input name="decrement" type="text" size="5"
					value="<%= decrement %>" onchange="changeDecrementCell();" />
			</td></tr>
			</table>
		
		<% } else if (evalConstant == MECH_PRODS_STARTERS_IS) { 
			int combination = MechProdStartIs.NOT_SUPERSET;
			int flags = MechSet.RESON_LENIENT;
			int productOrStart = MechProdStartIs.PRODUCT;
			if (useInput) { // existing evaluator
				final MechProdStartIs impl = new MechProdStartIs(inputSubeval.codedData);
				combination = impl.getCombination();
				productOrStart = impl.getProductOrStart();
				flags = impl.getFlags();
			} // useInput 
			final String kind = MechProdStartIs.PROD_START_ENGL[productOrStart];
			%>
			<input type="hidden" name="molstruct" value="" />
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext">
				Considering the possible
				<select name="productOrStart" onchange="changeProdStart();">
					<option value="<%= MechProdStartIs.START %>"
						<%= productOrStart == MechProdStartIs.START ? SELECTED : "" %> >
					<%= MechProdStartIs.PROD_START_ENGL[MechProdStartIs.START] %>s</option>
					<option value="<%= MechProdStartIs.PRODUCT %>"
						<%= productOrStart == MechProdStartIs.PRODUCT ? SELECTED : "" %> >
					<%= MechProdStartIs.PROD_START_ENGL[MechProdStartIs.PRODUCT] %>s</option>
				</select> below, if
			</td></tr>
			<tr><td class="regtext">
				<select name="combination" id="combination">
				<% for (int combNum = 0; 
						combNum < MechProdStartIs.COMB_ENGL.length / 2; combNum++) { %>
					<option value="<%= combNum %>" 
							<%= combination == combNum ? SELECTED : "" %> >
					<%= MechProdStartIs.COMB_ENGL[combNum] %>
					of them 
					<%= MechProdStartIs.COMB_ENGL[combNum].contains("all")
							? "are response " + kind + "s" 
							: "is a response " + kind %> 
					</option>
				<% } // for the first half of the combinations %>
				<% for (int combNum = MechProdStartIs.COMB_ENGL.length / 2; 
						combNum < MechProdStartIs.COMB_ENGL.length; combNum++) { %>
					<option value="<%= combNum %>" 
							<%= combination == combNum ? SELECTED : "" %> >
					the response's <%= kind %>s
					<%= MechProdStartIs.COMB_ENGL[combNum] %>
					them
					</option>
				<% } // for the second half of the combinations %>
				</select>
			</td></tr>
			<tr><td class="regtext">
				<table style="margin-left:auto; margin-right:auto; 
						width:445px; text-align:left;" summary="">
				<tr><td class="boldtext" style="text-align:right; padding-left:10px;
						padding-right:10px; padding-top:10px; font-style:italic;">
					MarvinJS&trade;
				</td></tr>
				<tr><td style="text-align:center; padding-left:10px; 
						padding-right:10px;">
					<table class="whiteTable" summary=""><tr><td>
						<div id="<%= APPLET_NAME %>">
						<script type="text/javascript">
						// <!-- >
							startMarvinJS('<%= Utils.toValidJS(
										inputSubeval.molStruct) %>', 
									-MECHANISM, 0, '<%= APPLET_NAME %>', 
									'<%= pathToRoot %>');
						// -->
						</script>
						</div>
					</td></tr></table>
				</td></tr>
				</table>
			</td></tr>
			<tr><td class="regtext">
				<table summary="">
				<tr><td>
				<input type="checkbox" name="resonanceLenient" value="yes"
					<%= (flags & MechSet.RESON_LENIENT) != 0 ? CHECKED : "" %> />
				</td><td>
				response may contain any resonance structure of the given
				compounds
				</td></tr>
				<tr><td>
				<input type="checkbox" name="stereoLenient" value="yes"
					<%= (flags & MechSet.STEREO_LENIENT) != 0 ? CHECKED : "" %> />
				</td><td>
				ignore stereochemistry
				</td></tr>
				</table>
			</td></tr>
			</table>

		<% } else if (evalConstant == MECH_PRODS_STARTERS_PROPS) { 
			int howMany = MechProdStartProps.ANY;
			int cpdsType = MechProdStartProps.PRODUCT;
			Subevaluator evalGen;
			if (useInput) { // existing evaluator
				final MechProdStartProps impl = 
						new MechProdStartProps(inputSubeval.codedData);
				howMany = impl.getHowMany();
				cpdsType = impl.getCpdsType();
				evalGen = impl.getSubevaluator(inputSubeval.molStruct);
			} else {
				final MechProdStartProps impl = new MechProdStartProps();
				evalGen = impl.getSubevaluator();
			} // useInput 
			int matchNum = EvalManager.getEvalType(evalGen.matchCode);
			final String subevalNumStr = request.getParameter("subevalNum");
			if (subevalNumStr != null) {
				matchNum = MathUtils.parseInt(subevalNumStr);
				howMany = MathUtils.parseInt(request.getParameter("howMany"));
				cpdsType = MathUtils.parseInt(request.getParameter("cpdsType"));
			} // if we have just reloaded a new subevaluator type
			final String[] HOWMANY_ENGL = MechProdStartProps.HOWMANY_ENGL;
			final String[] PROD_START_ENGL = MechProdStartProps.PROD_START_ENGL;
			final int START = MechProdStartProps.START;
			final int INTERMED = MechProdStartProps.INTERMED;
			final int PRODUCT = MechProdStartProps.PRODUCT;
			%>
			<input type="hidden" name="molstruct" value="" />
			<input type="hidden" name="molname" value="" />
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext">
				If 
				<select name="howMany">
					<% for (int o = 1; o <= HOWMANY_ENGL.length; o++) { %>
						<option value="<%= o %>" <%= o == howMany 
								? SELECTED : "" %> >
						<%= HOWMANY_ENGL[o - 1] %></option>
					<% } %>
				</select>
				<select name="cpdsType">
					<option value="<%= START %>"
						<%= cpdsType == START ? SELECTED : "" %> >
					<%= PROD_START_ENGL[START] %></option>
					<option value="<%= INTERMED %>"
						<%= cpdsType == INTERMED ? SELECTED : "" %> >
					<%= PROD_START_ENGL[INTERMED] %></option>
					<option value="<%= PRODUCT %>"
						<%= cpdsType == PRODUCT ? SELECTED : "" %> >
					<%= PROD_START_ENGL[PRODUCT] %></option>
				</select>
			</td></tr>
			<tr><td class="regtext">
				of the response mechanism has the following
				<select name="evalTypeSelector" onchange="changeEvalType()">
					<option value="<%= FUNCTIONAL_GROUP %>" <%= 
							FUNCTIONAL_GROUP == matchNum ? SELECTED : "" 
								%>>functional group</option>
					<option value="<%= HAS_FORMULA %>" <%= 
							HAS_FORMULA == matchNum ? SELECTED : "" 
								%>>formula</option>
					<option value="<%= NUM_ATOMS %>" <%= 
							NUM_ATOMS == matchNum ? SELECTED : "" 
								%>>atom count</option>
					<option value="<%= NUM_RINGS %>" <%= 
							NUM_RINGS == matchNum ? SELECTED : "" 
								%>>ring count</option>
				</select>
				property:
			</td></tr>
				<% final String codedData = evalGen.codedData;
				if (matchNum == FUNCTIONAL_GROUP) {
					int initialGroupId = -1;
					boolean isPositive = true; 
					if (EVAL_CODES[FUNCTIONAL_GROUP].equals(evalGen.matchCode)) {
						final FnalGroup evalFnalGroup = new FnalGroup(codedData);
						initialGroupId = evalFnalGroup.getGroupId();
						isPositive = evalFnalGroup.getGroupOper() == FnalGroup.NOT_EQUALS;
					} // if useInput 
				%>
					<tr><td class="regtext">
						It <select name="isPositive" id="isPositive">
							<option value="N=" <%= isPositive 
									? SELECTED : "" %>>contains</option>
							<option value="Y=" <%= !isPositive 
									? SELECTED : "" %>>does not contain</option>
						</select>
						the functional group:
					</td></tr>
					<tr><td style="text-align:left; padding-top:20px; padding-left:20px;">
						<b>Select a functional group category:</b>
						<select name="categories" id="categories" 
								size="1" style="width:300px;" 
								onchange="loadGroupsInChangedCat(document.evaluatorForm)" >
						</select>
					</td></tr>
					<tr><td style="text-align:left; padding-left:20px;">
						<p><b>Select a functional group:</b>
						<span id="groupsPopup">
						<select name="fnalGroupId" size="1" style="width:300px;">
							<option value="0">No group selected</option>
						</select>
						</span>
						<script type="text/javascript">
							// <!-- >
							<% for (int grpNum = 1; grpNum <= sortedGroups.length; grpNum++) { 
								final FnalGroupDef group = sortedGroups[grpNum - 1]; %>
								setArrayValues(<%= grpNum %>,
										'<%= Utils.toValidJS(group.getPulldownName()) %>',
										'<%= Utils.toValidJS(group.category) %>',
										<%= group.groupId %>);
							<% } // for each group %>
							initFnalGroupConstants(<%= initialGroupId %>);
							setCatSelector();
							initializeGroupSelector(document.evaluatorForm);
							// -->
						</script>
					</td></tr>
				<% } else if (matchNum == HAS_FORMULA) {
					String formula = ""; 
					boolean isPositive = true;
					if (EVAL_CODES[HAS_FORMULA].equals(evalGen.matchCode)) {
						final HasFormula evalForm = new HasFormula(codedData);
						formula = evalForm.getFormula();
						isPositive = evalForm.getIsPositive();
					} // if have previous values to load
				%>
					<tr><td class="regtext">
						It <select name="isPositive">
							<option value="N=" <%= isPositive ? SELECTED : "" %> >
								has</option>
							<option value="Y=" <%= !isPositive ? SELECTED : "" %> >
								does not have</option>
						</select>
						the formula
						<br /><input type="text" name="formula"
								value="<%= formula %>" size="50" />
						<span style="color:green;">
							<p>Use the format 
							C<sub><i>m</i></sub>H<sub><i>n</i></sub>XY<sub><i>p</i></sub>, 
							e.g., C7H5ClO2. </p>
							<p>Use * to match with any number in the response, including zero.</p>
							<p>Do <i>not</i> repeat elements or use parentheses, 
							as in (CH3)3COH.</p>
						</span>
					</td></tr>
				<% } else if (matchNum == NUM_RINGS) {
					int ringsOper = Rings.EQUALS;
					int numRings = 0;
					if (EVAL_CODES[NUM_RINGS].equals(evalGen.matchCode)) {
						final Rings evalRings = new Rings(codedData);
						ringsOper = evalRings.getRingsOper();
						numRings = evalRings.getNumRings();
					} // if have previous values to load
					final String[] OPER_ENGLISH = Rings.OPER_ENGLISH[Rings.FEWER];
				%>
					<tr><td class="regtext">
						It has
						<select name="ringsOper">
							<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
								<option value="<%= Rings.SYMBOLS[o] %>" 
										<%= o == ringsOper ? SELECTED : "" %> >
								<%= OPER_ENGLISH[o] %></option>
							<% } %>
						</select>
						<input name="numRings" type="text" 
								size="2" value="<%= numRings %>" />
						ring(s).
					</td></tr>
				<% } else { // matchNum == NUM_ATOMS) {
					int atomsOper = Atoms.NOT_GREATER;
					int numAtoms = 4;
					String element = "C";
					boolean contiguous = true;
					if (EVAL_CODES[NUM_ATOMS].equals(evalGen.matchCode)) {
						final Atoms evalAtoms = new Atoms(codedData);
						atomsOper = evalAtoms.getAtomsOper();
						numAtoms = evalAtoms.getNumAtoms();
						element = evalAtoms.getElement();
						contiguous = evalAtoms.getContiguous();
					} // if have previous values to load
					final String[] OPER_ENGLISH = Atoms.OPER_ENGLISH[Atoms.FEWER];
				%>
					<tr><td class="regtext">
						It has
						<select name="atomsOper">
							<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
								<option value="<%= Atoms.SYMBOLS[o] %>" 
										<%= o == atomsOper ? SELECTED : "" %> >
								<%= OPER_ENGLISH[o] %></option>
							<% } %>
						</select>
						<input name="numAtoms" type="text" size="2" 
								value="<%= numAtoms %>" />
						<select name="contiguous">
							<option value="Y" <%= contiguous ? 
									"selected" : "" %> > contiguous</option> 
							<option value="N" <%= !contiguous ? 
									"selected" : "" %> > total</option>
						</select>
						<input name="element" type="text" size="2" value="<%= element %>" />
						atom(s).
					</td></tr>
				<% } // if matchNum %>
			<tr><td class="regtext" style="color:green;">
				<ul>
				<li>A compound is a <i>product</i> if none of its atoms or bonds 
				are sources or sinks of electron-flow arrows.
				</li><li>A compound is a <i>starting material</i> if:
					<ul><li>it resides in the first box; or,
					</li><li>at least one of its atoms or bonds is a source or sink
				of an electron-flow arrow, and ACE calculates that no electron-flow 
				arrows in any previous stages of the mechanism lead to the compound.
					</li></ul>
				</li><li>All other compounds in a mechanism are <i>intermediates</i>.
				</li></ul>
			</td></tr>
			</table>

		<% } else if (evalConstant == MECH_RULE) { 
			boolean isPositive = false;
			int rule = MechRule.SN2_SP; 
			double pKvalue = (double) 7.0; 
			int flags = 0;
			if (useInput) { // existing evaluator
				final MechRule impl = new MechRule(inputSubeval.codedData);
				isPositive = impl.getIsPositive();
				rule = impl.getRule();
				pKvalue = impl.getPKValue();
				flags = impl.getFlags();
			} // useInput %>
			<input type="hidden" name="initRule" value="<%= rule %>" />
			<input type="hidden" name="initFlags" value="<%= flags %>" />
			<input type="hidden" name="initPKValue" value="<%= pKvalue %>" />
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<table class="regtext" style="margin-left:auto; 
					margin-right:auto; width:445px;" summary="">
			<tr><td class="regtext" style="color:green;">
				Not all of the rules below will apply to every reaction mechanism. 
				Authors should choose carefully whether to apply any rule to a 
				particular mechanism question. 
			</td></tr>
			<tr><td>
				<br/>
				If the response mechanism <span id="isPosSelector"></span> this 
				<select id="setSelector" onchange="setSet(this.value, false, <%= rule %>);">
				</select>:
				<br/>&nbsp;
			</td><td>
			</td></tr>
			<tr><td id="rules">
			</td></tr>
			</table>

		<% } else if (evalConstant == MECH_SUBSTRUCTURE) { 
			boolean isPositive = false;
			boolean ignoreCharge = true;
			boolean ignoreRadState = true;
			boolean ignoreIsotopes = true;
			if (useInput) { // existing evaluator
				final MechSubstructure impl = new MechSubstructure(inputSubeval.codedData);
				isPositive = impl.getIsPositive();
				final int ignoreFlags = impl.getIgnoreFlags();
				ignoreCharge = (ignoreFlags & MechSubstructSearch.CHARGE_MASK) != 0;
				ignoreRadState = (ignoreFlags & MechSubstructSearch.RADSTATE_MASK) != 0;
				ignoreIsotopes = (ignoreFlags & MechSubstructSearch.ISOTOPES_MASK) != 0;
			} // useInput %>
			<input type="hidden" name="molstruct" value="" />
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<table style="margin-left:auto; margin-right:auto; width:445px; 
					text-align:left;" summary="">
			<tr><td class="regtext" style="color:green;">Note: Both the 
				substructure and the response are aromatized. If the substructure 
				contains an aromatic ring, include the entire aromatic ring
				in the substructure.
				Also, do not include generic R groups in the substructure.
				<br/>
			</td></tr>
			<tr><td class="regtext">
				<br/>If the response <span id="isPosSelector"></span>
				the following substructure and electron-flow arrows 
				(ignoring 
				<input type="checkbox" name="ignoreCharge" value="yes"
					<%= ignoreCharge ? CHECKED : "" %> />&nbsp;charge, 
				<input type="checkbox" name="ignoreRadState" value="yes"
					<%= ignoreRadState ? CHECKED : "" %> />&nbsp;radical state, 
				<input type="checkbox" name="ignoreIsotopes" value="yes"
					<%= ignoreIsotopes ? CHECKED : "" %> />&nbsp;isotope state): 
				<table style="margin-left:auto; margin-right:auto; 
						width:445px; text-align:left;" summary="">
				<tr><td class="boldtext" style="text-align:right; padding-left:10px;
						padding-right:10px; padding-top:10px; font-style:italic;">
					MarvinJS&trade;
				</td></tr>
				<tr><td style="text-align:center; padding-left:10px; 
						padding-right:10px;">
					<table class="whiteTable" summary=""><tr><td>
						<div id="<%= APPLET_NAME %>">
						<script type="text/javascript">
						// <!-- >
							startMarvinJS('<%= Utils.toValidJS(
										inputSubeval.molStruct) %>', 
									-MECHANISM, SHOWLONEPAIRS | SHOWNOH, 
									'<%= APPLET_NAME %>', '<%= pathToRoot %>');
									// show extra tools, no H atoms, normal panel size 
						// -->
						</script>
						</div>
					</td></tr></table>
				</td></tr>
				</table>
				
				(To indicate an "Any" atom, press <b>More</b>, 
				then <b>Any</b>, then close the pop-up window.) 
			</td></tr>
			</table>

		<% } else if (evalConstant == MECH_TOPOLOGY) { 
			boolean isPositive = false;
			int topology = MechShape.IS_CYCLIC; 
			if (useInput) { // existing evaluator
				final MechShape impl = new MechShape(inputSubeval.codedData);
				isPositive = impl.getIsPositive();
				topology = impl.getTopology();
			} // useInput %>
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<table style="margin-left:auto; margin-right:auto; width:445px; 
					text-align:left;" summary="">
			<tr><td>
				<span class="regtext"><br/>
				If the topology of the response mechanism
				<span id="isPosSelector"></span>
				<select name="topology">
					<option value="<%= MechShape.IS_LINEAR %>"
							<%= topology == MechShape.IS_LINEAR 
									? SELECTED : "" %> > linear
					</option>
					<option value="<%= MechShape.IS_CYCLIC %>"
							<%= topology == MechShape.IS_CYCLIC 
									? SELECTED : "" %> > cyclic (chain or catalytic)
					</option>
					<option value="<%= MechShape.IS_EITHER %>"
							<%= topology == MechShape.IS_EITHER 
									? SELECTED : "" %>> linear or cyclic
							(chain or catalytic)
					</option>
				</select>
				</span>
			</td></tr>
			</table>

		<% } else if (evalConstant == SYNTH_EQUALS) { 
			boolean isPositive = true; 
			boolean considerRxnCondns = true; 
			if (useInput) { // existing evaluator
				final SynthEquals impl = new SynthEquals(inputSubeval.codedData); 
				isPositive = impl.getIsPositive();		
				considerRxnCondns = impl.getConsiderRxnCondns();		
			} // usesInput %>
			<input type="hidden" name="molstruct" value="" />
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<input type="hidden" name="rxnIds" value="" />
			<table style="margin-left:auto; margin-right:auto; width:445px; 
					text-align:left;" summary="">
			<tr><td class="regtext" colspan="2">
				If the synthesis of the response
				<span id="isPosSelector"></span>
				the following synthesis exactly,
				<select name="considerRxnCondns">
					<option value="true" <%= considerRxnCondns ? SELECTED : "" %>>
					considering</option>
					<option value="false" <%= considerRxnCondns ? "" : SELECTED %>>
					ignoring</option>
				</select>
				reaction conditions
			</td></tr>
			<tr><td class="boldtext" style="text-align:right; padding-left:10px;
					padding-right:10px; padding-top:10px; font-style:italic;">
				Marvin JS&trade;
			</td></tr>
			<tr><td style="text-align:center; padding-left:10px; 
					padding-right:10px;">
				<table class="whiteTable" summary=""><tr><td>
					<div id="<%= APPLET_NAME %>">
					<script type="text/javascript">
						// <!-- >
						startMarvinJS('<%= Utils.toValidJS(
									inputSubeval.molStruct) %>', 
								SYNTHESIS, 0, '<%= APPLET_NAME %>', 
								'<%= pathToRoot %>'); 
						// -->
					</script>
					</div>
				</td></tr></table>
			</td></tr>
			<tr>
			<td style="text-align:center; padding-left:10px; padding-right:10px;">
				<table class="whiteTable" 
						style="margin-left:auto; margin-right:auto; 
						width:100%; text-align:left;" summary="">
					<tr><td id="reagentTable" class="regtext">
					</td></tr>
				</table>
			</td></tr>
			</table>

		<% } else if (evalConstant == SYNTH_SELEC) { 
			int kind = SynthSelective.ENANTIO;
			boolean isPositive = false;
			if (useInput) { // existing evaluator
				final SynthSelective impl = new SynthSelective(inputSubeval.codedData);
				kind = impl.getKind();
				isPositive = impl.getIsPositive();
			} // useInput 
		%>
			<input type="hidden" name="molstruct" value="" />
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<input type="hidden" name="partCredits" value="" />
			<input type="hidden" name="rxnIds" value="" />
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext" style="color:green;">
				[ACE will generate automatic feedback for unselective 
				synthetic schemes. ACE will append your 
				feedback.<span id="synthSelectiveGradingOpts"></span>]
				<br/><br/>
			</td></tr>
			<tr><td class="regtext">
				If the synthesis <span id="isPosSelector"></span>
				<select name="kind" onchange="populatePartCreditCell();">
					<option value="<%= SynthSelective.ENANTIO %>"
						<%= kind == SynthSelective.ENANTIO 
								? SELECTED : "" %>> enantioselective</option>
					<option value="<%= SynthSelective.DIASTEREO %>"
						<%= kind == SynthSelective.DIASTEREO 
								? SELECTED : "" %>> diastereoselective</option>
					<option value="<%= SynthSelective.ANY %>"
						<%= kind == SynthSelective.ANY 
								? SELECTED : "" %>> structure-selective</option>
				</select>
			</td></tr>
			<tr><td class="regtext">
				excluding from consideration the following synthetic step 
				(optional):
			</td></tr>
			<tr><td class="boldtext" style="text-align:right; 
					padding-left:10px; padding-right:10px; 
					padding-top:10px; font-style:italic;">
				MarvinJS&trade;
			</td></tr>
			<tr><td style="text-align:center; 
					padding-left:10px; padding-right:10px;">
				<table class="whiteTable" summary=""><tr><td>
					<div id="<%= APPLET_NAME %>">
					<script type="text/javascript">
						// <!-- >
						startMarvinJS('<%= Utils.toValidJS(
									inputSubeval.molStruct) %>', 
								-SYNTHESIS, 0, '<%= APPLET_NAME %>', 
								'<%= pathToRoot %>');
						// -->
					</script>
					</div>
				</td></tr></table>
			</td></tr>
			<tr><td id="reagentTable" class="regtext" >
			</td></tr>
			<tr><td style="color:green;">
				If you choose a reaction condition, place the number
				1 in stage 1. 
			</td></tr>
			<tr><td id="partCreditCell" style="padding-top:10px;">
			</td></tr>
			<tr><td><a name="SynthSelectiveBottom"></a>
			</table>

		<% } else if (evalConstant == SYNTH_ONE_RXN) { 
			boolean isPositive = false;
			int type = SynthOneRxn.IS;
			if (useInput) { // an existing evaluator 
				final SynthOneRxn impl = new SynthOneRxn(inputSubeval.codedData);
				isPositive = impl.getIsPositive();
				type = impl.getType();
			} // useInput 
		%>
			<input type="hidden" name="molstruct" value="" />
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<input type="hidden" name="rxnIds" value="" />
			<table class="regtext" style="margin-left:auto; 
					margin-right:auto; width:445px;" summary="">
			<tr><td>
				If the following synthetic step <span id="isPosSelector"></span>
				<select name="type" >
					<option value="<%= SynthOneRxn.IS %>" 
							<%= type == SynthOneRxn.IS 
									? SELECTED : "" %> > identical to </option>
					<option value="<%= SynthOneRxn.CONTAINS %>" 
							<%= type == SynthOneRxn.CONTAINS 
									? SELECTED : "" %> > a substructure of </option>
				</select>
				a synthetic step of the response
			</td></tr>
			<tr><td colspan="2" class="boldtext" style="text-align:right; 
					padding-left:10px; padding-right:10px; 
					padding-top:10px; font-style:italic;">
				MarvinJS&trade;
			</td></tr>
			<tr><td colspan="2" style="text-align:center; 
					padding-left:10px; padding-right:10px;">
				<table class="whiteTable" summary=""><tr><td>
					<div id="<%= APPLET_NAME %>">
					<script type="text/javascript">
						// <!-- >
						startMarvinJS('<%= Utils.toValidJS(
									inputSubeval.molStruct) %>', 
								-SYNTHESIS, 0, '<%= APPLET_NAME %>', 
								'<%= pathToRoot %>');
						// -->
					</script>
					</div>
				</td></tr></table>
			</td></tr>
			<tr><td id="reagentTable" class="regtext">
			</td></tr>
			<tr><td style="color:green;">
				If you choose a reaction condition, place the number
				1 in stage 1. </td></tr>
			</table>

		<% } else if (evalConstant == SYNTH_SM_MADE) { 
			boolean isPositive = true;
			if (useInput) { 
				final SynthEfficiency impl = new SynthEfficiency(inputSubeval.codedData);
				isPositive = impl.getIsPositive();
			} // useInput %>
			<input type="hidden" name="molstruct" value="" />
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext"><br/>
				If <span id="isPosSelector"></span>
				compound that is produced by a reaction of one of the 
				preceding steps is a permissible starting material
			</td></tr>
			</table>
		
		<% } else if (evalConstant == SYNTH_SCHEME) { 
			boolean isPositive = false;
			if (useInput) { 
				final SynthScheme impl = new SynthScheme(inputSubeval.codedData);
				isPositive = impl.getIsPositive();
			} // useInput 
			%>
			<input type="hidden" name="molstruct" value="" />
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<input type="hidden" name="partCredits" value="" />
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext" style="color:green;">
				[ACE will generate automatic feedback for incorrect 
				synthetic schemes. ACE will append your feedback.]
			</td></tr>
			<tr><td class="regtext"><br/>
				If <span id="isPosSelector"></span>
				produced by a reaction of a preceding step, excluding from 
				consideration permissible starting materials 
			</td></tr>
			<tr><td id="partCreditCell" style="padding-top:10px;">
			</td></tr>
			</table>

		<% } else if (evalConstant == SYNTH_STARTERS) { 
			int combination = SynthStart.NOT_SUPERSET;
			if (useInput) { // existing evaluator
				final SynthStart impl = new SynthStart(inputSubeval.codedData);
				combination = impl.getCombination();
			} // useInput %>
			<input type="hidden" name="molstruct" value="" />
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext">
				If&nbsp; 
				<select name="combination" id="combination">
				<% for (int combNum = 0; 
						combNum < SynthStart.COMB_ENGL.length / 2; combNum++) { %>
					<option value="<%= combNum %>" 
							<%= combination == combNum ? SELECTED : "" %> >
					<%= SynthStart.COMB_ENGL[combNum] %>
					of the compounds below 
					<%= SynthStart.COMB_ENGL[combNum].contains("all")
							? "are response starting materials" 
							: "is a response starting material" %> 
					</option>
				<% } // for the first half of the combinations %>
				<% for (int combNum = SynthStart.COMB_ENGL.length / 2; 
						combNum < SynthStart.COMB_ENGL.length; combNum++) { %>
					<option value="<%= combNum %>" 
							<%= combination == combNum ? SELECTED : "" %> >
					the response's starting materials
					<%= SynthStart.COMB_ENGL[combNum] %>
					the compounds:
					</option>
				<% } // for the second half of the combinations %>
				</select>
			</td></tr>
			<tr><td class="regtext">
				<table style="margin-left:auto; margin-right:auto; 
						width:445px; text-align:left;" summary="">
				<tr><td class="boldtext" style="text-align:right; padding-left:10px;
						padding-right:10px; padding-top:10px; font-style:italic;">
					MarvinJS&trade;
				</td></tr>
				<tr><td style="text-align:center; padding-left:10px; 
						padding-right:10px;">
					<table class="whiteTable" summary=""><tr><td>
						<div id="<%= APPLET_NAME %>">
						<script type="text/javascript">
						// <!-- >
							startMarvinJS('<%= Utils.toValidJS(
										inputSubeval.molStruct) %>', 
									-SYNTHESIS, 0, '<%= APPLET_NAME %>', 
									'<%= pathToRoot %>');
						// -->
						</script>
						</div>
					</td></tr></table>
				</td></tr>
				</table>
			</td></tr>
			</table>
		
		<% } else if (evalConstant == SYNTH_STEPS) { 
			int kind = SynthSteps.LINEAR;
			int oper = SynthSteps.GREATER;
			int limit = 4;
			double decrement = 0;
			if (useInput) { 
				final SynthSteps impl = new SynthSteps(inputSubeval.codedData);
				kind = impl.getKind();
				oper = impl.getOper();
				limit = impl.getLimit();
				decrement = impl.getDecrement();
			} // useInput %>
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext">
				If the number of
				<select name="kind" >
					<option value="<%= SynthSteps.LINEAR %>"
						<%= kind == SynthSteps.LINEAR 
								? SELECTED : "" %>>linear</option>
					<option value="<%= SynthSteps.TOTAL %>"
						<%= kind == SynthSteps.TOTAL 
								? SELECTED : "" %>>total</option>
				</select>
				synthetic steps is
			</td></tr>
			<tr><td class="regtext">
				<select name="oper" onchange="changeDecrementCell();">
					<% for (int o = 0; 
							o < SynthSteps.OPER_ENGLISH[FEWER].length; o++) { %>
						<option value="<%= o %>" <%= o == oper ? SELECTED : "" %> >
						<%= SynthSteps.OPER_ENGLISH[FEWER][o] %></option>
					<% } %>
				</select>
				<input name="limit" type="text" size="5"
					value="<%= limit %>" />
			</td></tr>
			<tr><td id="decrementCell" class="regtext" style="padding-top:10px;<%=
					!Utils.among(oper, SynthSteps.GREATER, SynthSteps.NOT_LESS) 
					? " visibility:hidden;" : "" %>">
				Amount by which to decrease the grade for each step over the limit
				(enter 0 to disable this feature):
				<input name="decrement" type="text" size="5"
					value="<%= decrement %>" onchange="changeDecrementCell();" />
			</td></tr>
			</table>

		<% } else if (evalConstant == SYNTH_TARGET) { 
			boolean isPositive = false;
			boolean checkEnantiomer = false;
			if (useInput) { // an existing evaluator 
				final SynthTarget impl = new SynthTarget(inputSubeval.codedData);
				isPositive = impl.getIsPositive();
				checkEnantiomer = impl.getCheckEnantiomer();
			} // useInput %>
			<input type="hidden" name="molstruct" value="" />
			<input type="hidden" name="molname" value="" />
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<table class="regtext" style="margin-left:auto; 
					margin-right:auto; width:445px;" summary="">
			<tr><td>
				If the response target <span id="isPosSelector"></span>
				<select name="checkEnantiomer" >
					<option value="false" <%= !checkEnantiomer 
							? SELECTED : "" %> > exactly</option>
					<option value="true" <%= checkEnantiomer 
							? SELECTED : "" %> > either enantiomer of</option>
				</select>
			</td></tr>
			<tr><td class="regtext" style="width:90%;">
				<input type="text" name="inputmolname" size="55"
					value="<%= Utils.toValidTextbox(inputSubeval.molName) %>" />
			</td></tr>
			<tr><td colspan="2" class="boldtext" style="text-align:right; 
					padding-left:10px; padding-right:10px; 
					padding-top:10px; font-style:italic;">
				MarvinJS&trade;
			</td></tr>
			<tr><td colspan="2" style="text-align:center; 
					padding-left:10px; padding-right:10px;">
				<table class="whiteTable" summary=""><tr><td>
					<div id="<%= APPLET_NAME %>">
					<script type="text/javascript">
						// <!-- >
						startMarvinJS('<%= Utils.toValidJS(
									inputSubeval.molStruct) %>', 
								-SYNTHESIS, 0, '<%= APPLET_NAME %>', 
								'<%= pathToRoot %>');
						// -->
					</script>
					</div>
				</td></tr></table>
			</td></tr>
			</table>

		<% } else if (evalConstant == OED_DIFF) { 
			boolean isPositive = false;
			int oper = OEDDiff.EXACTLY;
			int extent = (labelOrbitals 
					? OEDDiff.TYPE_CT_OCCUP_LINE_LABEL 
					: OEDDiff.TYPE_CT_OCCUP_LINE);
			int energies = OEDDiff.SIGNUMS;
			int tolerance = 0;
			if (useInput) {
				final OEDDiff impl = new OEDDiff(inputSubeval.codedData);
				isPositive = impl.getIsPositive();
				oper = impl.getOper();
				energies = impl.getEnergies();
				extent = impl.getExtent();
				tolerance = impl.getTolerance();
			} // useInput 
			final int numRows = oed.getNumRows();
			final int numCols = oed.getNumCols();
			final String[] captions = oed.getCaptions();
			final boolean haveYAxisScale = oed.haveYAxisScale();
			%>
			<input type="hidden" name="molstruct" value="" />
			<input type="hidden" name="numRows" value="<%= numRows %>" />
			<input type="hidden" name="numCols" value="<%= numCols %>" />
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<table style="margin-left:auto; margin-right:auto; width:95%;
					text-align:left;" summary="">
				<tr><td class="regtext" style="color:green;">
					[If you choose to compare exactly, and a response diagram 
					does not match the reference diagram, ACE will generate 
					automatic feedback and append your feedback.]
				</td></tr>
				<tr><td class="regtext" style="padding-top:10px;">
					If the 
					<select name="extent">
						<% if (labelOrbitals) { %>
						<option value="<%= OEDDiff.TYPE_CT_OCCUP_LINE_LABEL %>" 
								<%= extent == OEDDiff.TYPE_CT_OCCUP_LINE_LABEL 
										? SELECTED : "" 
								%>>orbitals, occupancies, correlations, 
								and labels</option>
						<% } // if labeling orbitals %>
						<option value="<%= OEDDiff.TYPE_CT_OCCUP_LINE %>" 
								<%= extent == OEDDiff.TYPE_CT_OCCUP_LINE 
										? SELECTED : "" 
								%>>orbitals, occupancies, and correlations</option>
						<option value="<%= OEDDiff.TYPE_CT_OCCUP %>" 
								<%= extent == OEDDiff.TYPE_CT_OCCUP 
										? SELECTED : "" 
								%>>orbitals and occupancies</option>
						<option value="<%= OEDDiff.TYPE_CT %>" 
								<%= extent == OEDDiff.TYPE_CT 
										? SELECTED : "" 
								%>>numbers of each type of orbital</option>
						<option value="<%= OEDDiff.TYPE_ONLY %>" 
								<%= extent == OEDDiff.TYPE_ONLY 
										? SELECTED : "" 
								%>>types of orbitals</option>
					</select>
					in the response orbital energy diagram
					<span id="isPosSelector"></span>
					<select name="oper">
						<option value="<%= OEDDiff.EXACTLY %>" 
								<%= oper == OEDDiff.EXACTLY 
										? SELECTED : "" %>>exactly</option>
						<option value="<%= OEDDiff.ATLEAST %>" 
								<%= oper == OEDDiff.ATLEAST 
										? SELECTED : "" %>>at least</option>
					</select>
					those in the author's diagram, where "match" means that
				</td></tr>
				<tr><td>
					<select name="energies" id="energies" onchange="makeTolVisible()">
						<option value="<%= OEDDiff.FIXED_HEIGHT %>"
								<%= energies == OEDDiff.FIXED_HEIGHT 
									? SELECTED : "" %> >
						absolute orbital energies must match
						</option>
						<option value="<%= OEDDiff.RELATIVE_HEIGHT %>"
								<%= energies == OEDDiff.RELATIVE_HEIGHT 
									? SELECTED : "" %> >
						orbital energy differences must match
						</option>
						<option value="<%= OEDDiff.SIGNUMS %>"
								<%= energies == OEDDiff.SIGNUMS 
									? SELECTED : "" %> >
						orbital energy differences must have same arithmetic sign
						</option>
						<option value="<%= OEDDiff.ANY_E %>"
								<%= energies == OEDDiff.ANY_E 
									? SELECTED : "" %> >
						orbital energies are disregarded
						</option>
					</select>
					<span style="visibility:<%= Utils.among(energies, 
							OEDDiff.FIXED_HEIGHT, OEDDiff.RELATIVE_HEIGHT)
							? VISIBLE : HIDDEN %>;" id="toler">
					within
					<input type="text" name="tolerance" id="tolerance" size="5" 
							value="<%= !useInput ? "" : tolerance %>" /> 
					row(s) 
					</span>
				</td></tr>
				<tr><td style="padding-top:10px;">
					<table class="whiteTable" style="width:100%; 
							border-collapse:collapse; border-style:none;" summary="">
						<tr><th colspan="<%= numCols + (haveYAxisScale ? 1 : 0) %>">
							<div id="canvas0" style="position:relative; left:0px; top:0px;
									width:600px; height:2px; overflow:visible;">
							</div>
						</th></tr>
						<tr>
						<% if (haveYAxisScale) { %>
							<th class="boldtext" style="text-align:center; 
									width:30px;
									border-right-style:solid; border-right-width:1px; 
									border-right-color:black;
									border-bottom-style:solid; border-bottom-width:1px; 
									border-bottom-color:black;">
								Energy
								(<%= Utils.toDisplay(oed.getYAxisUnit()) %>)
							</th>
						<% } // if there's a scale to display on the y-axis
						for (final String caption : captions) { %>
							<th class="boldtext" style="text-align:center; 
									border-bottom-style:solid; border-bottom-width:1px; 
									border-bottom-color:black;">
							<%= Utils.toDisplay(caption) %>
							</th>
						<% } // for each caption %>
						</tr>
						<% final String[] yAxisLabels = oed.getYAxisLabels();
						for (int rNum = 0; rNum < numRows; rNum++) { %>
							<tr>
							<% if (haveYAxisScale) { %>
								<td class="boldtext" 
										style="text-align:right; white-space:nowrap;
											width:30px;
											border-right-style:solid; 
											border-right-width:1px; 
											border-right-color:black;
											padding-left:10px; padding-right:10px;">
									<%= yAxisLabels[rNum] %>
								</td>
							<% } // if there's a scale to display on the y-axis
							for (int cNum = 0; cNum < numCols; cNum++) { 
								final int row = numRows - rNum; 
								final int col = cNum + 1; %>
								<td class="regtext" id="r<%= row %>c<%= col %>" 
										style="text-align:center; white-space:nowrap;
											padding-left:10px; padding-right:10px;">
								</td>
							<% } // for each column %>
							</tr>
						<% } // for each row %>
						<tr>
						<% if (haveYAxisScale) { %>
							<td></td>
						<% } %>
						<td colspan="<%= numCols %>" 
								style="width:100%; height:1px;
								border-top-style:solid; 
								border-top-width:1px; border-top-color:black; 
								border-left-style:none; border-right-style:none;">
						</td></tr>
						<tr>
						<td id="textAndButtons" 
								colspan="<%= numCols + (haveYAxisScale ? 1 : 0) %>">
						</td></tr>
					</table>
				</td></tr>
			</table>

		<% } else if (evalConstant == OED_ELEC) { 
			int oper = OEDElecCt.NOT_EQUALS;
			int column = OEDElecCt.COLUMNS1AND3;
			int number = -2;
			int orbType = OEDOrbType.ANY;
			if (useInput) { // existing evaluator
				final OEDElecCt impl = new OEDElecCt(inputSubeval.codedData);
				oper = impl.getOper();
				column = impl.getColumn();
				number = impl.getNumber();
				orbType = impl.getOrbType();
			} // if useInput %>
			<table style="margin-left:auto; margin-right:auto; width:445px;
					text-align:left;" summary="">
				<tr>
				<td class="regtext">If the number of electrons in
					<select name="orbType">
						<% for (int orb = 1; orb < OEDElecCt.INDIV_NAMES.length; orb++) { %>
							<option value="<%= orb %>" <%= orbType == orb 
									? SELECTED : "" %> ><%= 
									Utils.toPopupMenuDisplay(OEDElecCt.INDIV_NAMES[orb]) 
									%></option>
						<% } // for each orbital type %>
						<% for (int orb = 0; orb < OEDElecCt.GRP_NAMES.length; orb++) { %>
							<option value="<%= -orb %>" <%= orbType == -orb 
									? SELECTED : "" %> ><%= OEDElecCt.GRP_NAMES[orb] %></option>
						<% } // for each orbital type %>
					</select>
					orbitals in
					<select name="column" onchange="suggestFeedback();">
						<% for (int col = 1; col <= 3; col++) { %>
							<option value="<%= col %>" <%= column == col 
									? SELECTED : "" %> >column <%= col %></option>
						<% } // for each column %>
						<option value="<%= OEDElecCt.COLUMNS1AND3 %>" <%= 
								column == OEDElecCt.COLUMNS1AND3 
									? SELECTED : "" %> >columns 1 and 3</option>
					</select>
					is
					<select name="oper">
						<% for (int o = 0; o < OEDElecCt.OPER_ENGLISH[FEWER].length; o++) { %>
							<option value="<%= o %>" 
									<%= o == oper ? SELECTED : "" %> >
							<%= OEDElecCt.OPER_ENGLISH[FEWER][o] %></option>
						<% } %>
					</select>
					<select id="num_or_colCt" onchange="changeNumSelector();">
						<option value="num" <%= number >= 0 
								? SELECTED : "" %> >&nbsp;</option>
						<option value="colCt" <%= number < 0 
								? SELECTED : "" %> >the number of electrons in column</option>
					</select>
					<span id="num_Input">
					<% if (number >= 0) { %>
						<input name="number" type="text" size="3"
							value="<%= number %>" />
					<% } else { %>
						<select name="number" onchange="suggestFeedback();">
							<% for (int col = -1; col >= -3; col--) { %>
								<option value="<%= col %>" <%= number == col 
										? SELECTED : "" %> ><%= -col %></option>
							<% } // for each column %>
						</select>
					<% } // if number %>
					</span>
				</td>
				</tr>
			</table>
			<script type="text/javascript">
				// <!-- >
				suggestFeedback();
				// -->
			</script>

		<% } else if (evalConstant == OED_TYPE) { 
			int oper = OEDOrbType.NOT_EQUALS;
			int column = 1;
			int number = 4;
			int orbType = OEDOrbType.SP3;
			if (useInput) { // existing evaluator
				final OEDOrbType impl = new OEDOrbType(inputSubeval.codedData);
				oper = impl.getOper();
				column = impl.getColumn();
				number = impl.getNumber();
				orbType = impl.getOrbType();
			} // if useInput %>
			<table style="margin-left:auto; margin-right:auto; width:445px;
					text-align:left;" summary="">
				<tr>
				<td class="regtext">If the number of 
					<select name="orbType">
						<% for (int orb = 1; orb < OEDOrbType.INDIV_NAMES.length; orb++) { %>
							<option value="<%= orb %>" <%= orbType == orb 
									? SELECTED : "" %> ><%= 
									Utils.toPopupMenuDisplay(OEDOrbType.INDIV_NAMES[orb]) 
									%></option>
						<% } // for each orbital type %>
						<% for (int orb = 0; orb < OEDOrbType.GRP_NAMES.length; orb++) { %>
							<option value="<%= -orb %>" <%= orbType == -orb 
									? SELECTED : "" %> ><%= OEDOrbType.GRP_NAMES[orb] %></option>
						<% } // for each orbital type %>
					</select>
					orbitals in
					<select name="column">
						<% for (int col = 1; col <= 3; col++) { %>
							<option value="<%= col %>" <%= column == col 
									? SELECTED : "" %> >column <%= col %></option>
						<% } // for each column %>
					</select>
					is
					<select name="oper">
						<% for (int o = 0; o < OEDOrbType.OPER_ENGLISH[FEWER].length; o++) { %>
							<option value="<%= o %>" 
									<%= o == oper ? SELECTED : "" %> >
							<%= OEDOrbType.OPER_ENGLISH[FEWER][o] %></option>
						<% } %>
					</select>
					<input name="number" type="text" size="3" value="<%= number %>" />
				</td>
				</tr>
			</table>

		<% } else if (evalConstant == RCD_DIFF) { 
			boolean isPositive = false;
			int oper = RCDDiff.EXACTLY;
			int energies = RCDDiff.SIGNUMS;
			int tolerance = 0;
			int extent = RCDDiff.TYPE_CONNXN_LABEL; 
			if (useInput) {
				final RCDDiff impl = new RCDDiff(inputSubeval.codedData);
				isPositive = impl.getIsPositive();
				oper = impl.getOper();
				energies = impl.getEnergies();
				tolerance = impl.getTolerance();
				extent = impl.getExtent();
			} // useInput 
			final int numRows = rcd.getNumRows();
			final int numCols = rcd.getNumCols();
			final boolean haveYAxisScale = rcd.haveYAxisScale();
			%>
			<input type="hidden" name="molstruct" value="" />
			<input type="hidden" name="numRows" value="<%= numRows %>" />
			<input type="hidden" name="numCols" value="<%= numCols %>" />
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<table style="margin-left:auto; margin-right:auto; width:95%;
					text-align:left;" summary="">
				<tr><td class="regtext" style="color:green;">
					[If you choose to compare exactly, and a response diagram 
					does not match the reference diagram, ACE will generate 
					automatic feedback and append your feedback.]
				</td></tr>
				<tr><td class="regtext" style="padding-top:10px;">
					If the 
					<select name="extent">
						<option value="<%= RCDDiff.TYPE_CONNXN_LABEL %>" 
								<%= extent == RCDDiff.TYPE_CONNXN_LABEL 
										? SELECTED : "" 
								%>>maxima and minima, connections between 
								them, and labels</option>
						<option value="<%= RCDDiff.TYPE_CONNXN %>" 
								<%= extent == RCDDiff.TYPE_CONNXN 
										? SELECTED : "" 
								%>>maxima and minima and connections between
								them</option>
						<option value="<%= RCDDiff.TYPE_ONLY %>" 
								<%= extent == RCDDiff.TYPE_ONLY 
										? SELECTED : "" 
								%>>maxima and minima</option>
					</select>
					of the response reaction coordinate diagram
					<span id="isPosSelector"></span>
					<select name="oper">
						<option value="<%= RCDDiff.EXACTLY %>" 
								<%= oper == RCDDiff.EXACTLY 
										? SELECTED : "" %>>exactly</option>
						<option value="<%= RCDDiff.ATLEAST %>" 
								<%= oper == RCDDiff.ATLEAST 
										? SELECTED : "" %>>at least</option>
					</select>
					those in the author's diagram, where "match" means that:
				</td></tr>
				<tr><td>
					<select name="energies" id="energies" onchange="makeTolVisible()">
						<option value="<%= RCDDiff.FIXED_HEIGHT %>"
								<%= energies == RCDDiff.FIXED_HEIGHT 
									? SELECTED : "" %> >
						absolute maxima and minima energies must match
						</option>
						<option value="<%= RCDDiff.RELATIVE_HEIGHT %>"
								<%= energies == RCDDiff.RELATIVE_HEIGHT 
									? SELECTED : "" %> >
						maxima and minima energy differences must match
						</option>
						<option value="<%= RCDDiff.SIGNUMS %>"
								<%= energies == RCDDiff.SIGNUMS 
									? SELECTED : "" %> >
						maxima and minima energy differences must have same arithmetic sign
						</option>
						<option value="<%= RCDDiff.ANY_E %>"
								<%= energies == RCDDiff.ANY_E 
									? SELECTED : "" %> >
						maxima and minima energies are disregarded
						</option>
					</select>
					<span style="visibility:<%= Utils.among(energies, 
							RCDDiff.FIXED_HEIGHT, RCDDiff.RELATIVE_HEIGHT)
							? VISIBLE : HIDDEN %>;" id="toler">
					within
					<input type="text" name="tolerance" id="tolerance" size="5" 
							value="<%= !useInput ? "" : tolerance %>" /> 
					row(s) 
					</span>
				</td></tr>
				<tr><td style="padding-top:10px;">
					<table class="rcdTable" style="width:100%;" summary="">
						<tr>
						<% if (haveYAxisScale) { %>
							<th class="boldtext" style="text-align:center; 
									border-right-style:solid; border-right-width:1px; 
									border-right-color:black;
									border-bottom-style:solid; border-bottom-width:1px; 
									border-bottom-color:black;">
								Energy
								(<%= Utils.toDisplay(rcd.getYAxisUnit()) %>)
							</th>
						<% } else { %>
							<th style="border-bottom-style:none; width:1px;"></th>
						<% } // if there's a scale to display on the y-axis
						for (int cNum = 0; cNum < numCols; cNum++) { %>
							<th>&nbsp;</th>
						<% } // for each column %>
						</tr>
						<% final String[] yAxisLabels = rcd.getYAxisLabels();
						for (int rNum = 0; rNum < numRows; rNum++) { %>
							<tr>
							<% if (haveYAxisScale) { %>
								<td class="boldtext" 
										style="text-align:right; white-space:nowrap;
											border-right-style:solid; 
											border-right-width:1px; 
											border-right-color:black;
											padding-left:10px; padding-right:10px;">
									<%= yAxisLabels[rNum] %>
								</td>
							<% } else { %>
								<td style="border-right-style:solid; 
										border-right-width:1px; 
										border-right-color:black;">
								</td>
							<% }
							for (int cNum = 0; cNum < numCols; cNum++) { 
								final int row = numRows - rNum; 
								final int col = cNum + 1; 
								%>
								<td class="regtext" id="r<%= row %>c<%= col %>" 
										style="text-align:center; white-space:nowrap;
											padding-left:10px; padding-right:10px;">
								</td>
							<% } // for each column %>
							</tr>
						<% } // for each row %>
						<tr>
						<td style="border-right-style:none; width:1px;"></td>
						<td colspan="<%= numCols %>" style="width:100%;
								border-top-style:solid; border-top-width:1px;
								border-top-color:black; border-left-style:none; 
								border-right-style:none;">
						</td></tr>
						<tr><td id="textAndButtons" colspan="<%= numCols + 1 %>" 
								style="width:100%;">
						</td>
						</tr>
					</table>
					<table style="width:95%; margin-left:auto; margin-right:auto;"
							summary="selectorsAndButtons">
						<tr><th colspan="<%= numCols + 1 %>">
							<div id="canvas0" style="position:relative; left:0px; top:0px;
									width:600px; height:2px; overflow:visible;">
							</div>
						</th></tr>
					</table>
				</td></tr>
			</table>

		<% } else if (evalConstant == RCD_STATE_CT) { 
			final int numCols = rcd.getNumCols();
			int which = RCDStateCt.BOTH;
			int label = 0;
			int mode = RCDStateCt.ANY;
			String columnsStr = "";
			int oper = RCDStateCt.GREATER;
			int number = 1;
			if (useInput) { // existing evaluator
				final RCDStateCt impl = new RCDStateCt(inputSubeval.codedData);
				which = impl.getWhich();
				label = impl.getLabel();
				mode = impl.getMode();
				columnsStr = impl.getColumnsStr();
				oper = impl.getOper();
				number = impl.getNumber();
			} // if useInput 
			final String[] labels = rcd.getLabels();
			final String[] columns = (Utils.isEmpty(columnsStr) ? new String[0]
					: columnsStr.split(RCDStateCt.COLS_SEP));
			%>
			<table style="margin-left:auto; margin-right:auto; width:445px;
					text-align:left;" summary="">
			<tr>
			<td colspan="2" class="regtext">If the number of 
				<input type="hidden" name="columnsStr" value="<%= columnsStr %>" />
				<input type="hidden" name="numCols" value="<%= numCols %>" />
				<select name="which">
					<% for (int w = 0; w < RCDStateCt.WHICH_ENGL.length; w++) { %>
						<option value="<%= w %>" <%= which == w 
								? SELECTED : "" %> > <%= 
							RCDStateCt.WHICH_ENGL[w] %></option>
					<% } // for each kind of state %>
				</select>
				<% if (!Utils.isEmpty(labels)) { %>
					labeled as 
					<select name="label">
						<option value="0" <%= label == 0
								? SELECTED : "" %> > anything </option>
						<% for (int lbl = 1; lbl <= labels.length; lbl++) { %>
							<option value="<%= lbl %>" <%= label == lbl
									? SELECTED : "" %> > <%= labels[lbl - 1] %></option>
						<% } // for each label %>
					</select>
				<% } // if there are labels %>
				<br />in
				<select name="mode">
					<% for (int m = 0; m < RCDStateCt.MODE.length; m++) { %>
						<option value="<%= m %>" <%= mode == m 
								? SELECTED : "" %> > <%= 
							RCDStateCt.MODE[m].toLowerCase() %></option>
					<% } // for each mode %>
				</select>
				of the columns selected below is
				<br /><select name="oper">
					<% for (int o = 0; o < RCDStateCt.OPER_ENGLISH[FEWER].length; o++) { %>
						<option value="<%= o %>" 
								<%= o == oper ? SELECTED : "" %> > <%= 
							RCDStateCt.OPER_ENGLISH[FEWER][o] %></option>
					<% } %>
				</select>
				<input name="number" type="text" size="3" value="<%= number %>" />
			</td>
			</tr>
			<% for (int cNum = 1; cNum <= numCols; cNum++) { %>
				<tr>
				<td><input type="checkbox" id="col<%= cNum %>" <%=
						Utils.contains(columns, String.valueOf(cNum)) ? CHECKED : "" %> />
				</td>
				<td class="regtext" style="width:100%; text-align:left; padding-left:10px;">
					column <%= cNum %>
				</td>
				</tr>
			<% } // for each column %>
			</table>

		<% } else if (evalConstant == HUMAN_REQD) { %> 
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext" style="color:green;">
				[ACE will generate automatic feedback for responses that require 
				the instructor's evaluation. ACE will append your feedback.]
			</td></tr>
			<tr><td class="regtext">
				<br/>If the response must be graded manually by the instructor
			</td></tr>
			</table>

		<% } // if evalConstant %>
	</td></tr>
</table>
</div>
</form>
</body>
</html>
