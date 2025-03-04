package com.epoch.evals.impl.chemEvals.energyEvals;

import com.epoch.energyDiagrams.EDiagram;
import com.epoch.energyDiagrams.RCDCell;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.chemEvals.energyEvals.energyEvalConstants.EnergyDiffConstants;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.math.util.MathUtils;

/** Contains variables and methods common to <code>OEDDiff</code> and 
 * <code>RCDDiff</code>. */
public class EDiagramDiff implements EnergyDiffConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** The parsed response diagram. */
	transient protected EDiagram respDiagram;
	/** The parsed author's diagram. */
	transient protected EDiagram authDiagram;

	/** Whether the orbital is satisfied by match or no match. */
	protected boolean isPositive;
	/** Whether the reference diagram is all of or a subset of the response 
	 * diagram. */
	protected int oper;
	/** How the heights of the orbitals will be assessed. */
	protected int energies;
	/** When <code>energies</code> requires the distances of orbitals or
	 * states to be compared, the tolerance on their distances. */
	protected int tolerance;
	/** To what extent to compare the two diagrams. */
	transient protected int compareExtent;

	/** List of lists of strings representing the occupied cells
	 * of the response diagram. */
	transient protected List<ArrayList<String>> respOccupStrs;
	/**	List of lists of row numbers of the occupied cells of the
	 * response diagram. */
	transient protected List<ArrayList<Integer>> respRowNums;
	/** List of lists of strings representing the occupied cells
	 * of the author's diagram. */
	transient protected List<ArrayList<String>> authOccupStrs;
	/** List of lists of row numbers of the occupied cells of the
	 * author's diagram. */
	transient protected List<ArrayList<Integer>> authRowNums;

	/** Determines whether the row numbers of orbitals or maxima and minima 
	 * in the author diagram match those in the response diagram.
	 * @param	colCt	the number of columns in the diagrams
	 * @param	orbOrState	orbitals or maxima and minima
	 * @return	results of the evaluation
	 */
	protected OneEvalResult fixedHeights(int colCt, String orbOrState) {
		final String SELF = "EDiagramDiff.fixedHeights: ";
		final OneEvalResult evalResult = new OneEvalResult();
		// for each orbital or maximum or minimum in response, can we find 
		// one in author's diagram with matching row number?
		debugPrint(SELF + "respColOccupStrs = ", respOccupStrs,
				", authOccupStrs = ", authOccupStrs);
		for (int col1 = 1; col1 <= colCt; col1++) {
			// need to copy because will modify
			final List<String> respColOccupStrs = 
					new ArrayList<String>(respOccupStrs.get(col1 - 1));
			final List<Integer> respColRowNums = 
					new ArrayList<Integer>(respRowNums.get(col1 - 1));
			final List<String> authColOccupStrs = 
					new ArrayList<String>(authOccupStrs.get(col1 - 1));
			final List<Integer> authColRowNums = 
					new ArrayList<Integer>(authRowNums.get(col1 - 1));
			final int numAuthOrbs = authColOccupStrs.size();
			for (int authNum = 0; authNum < numAuthOrbs; authNum++) {
				final String authOrbStr = authColOccupStrs.get(authNum);
				final int authRowNum = authColRowNums.get(authNum).intValue();
				debugPrint(SELF + "looking for ", authOrbStr, " in row ",
						authRowNum, " in response diagram.");
				final List<String> removedRespPDsOccupStrs = 
						new ArrayList<String>();
				final List<Integer> removedRespPDsRowNums = 
						new ArrayList<Integer>();
				// strings not necessarily unique, so we need to
				// iterate through each item in response diagram with 
				// same name in order to find match
				while (true) {
					final int posnInResp = respColOccupStrs.indexOf(authOrbStr);
					if (posnInResp < 0) {
						debugPrint(SELF + "failed to find ", authOrbStr, 
								" with row ", authRowNum, 
								" in response diagram.");
						evalResult.isSatisfied = !isPositive;
						if (evalResult.isSatisfied && oper == EXACTLY) {
							evalResult.autoFeedback = new String[]
									{"The height of one or more " + orbOrState 
									+ " in your diagram is incorrect."};
						} // if need autofeedback
						return evalResult;
					} // if no match
					final String respOrbStr = 
							respColOccupStrs.remove(posnInResp);
					final Integer respRowNum = 
							respColRowNums.remove(posnInResp);
					final boolean match = 
							compare(authRowNum, respRowNum.intValue());
					if (!match) {
						debugPrint(SELF + "found response ", orbOrState,
								" with same name but mismatched row ", 
								respRowNum, 
								"; setting aside to restore later.");
						removedRespPDsOccupStrs.add(respOrbStr);
						removedRespPDsRowNums.add(respRowNum);
					} else {
						debugPrint(SELF + "found response pair that matched.");
						break;
					} // if match
				} // while looking for a match
				if (!removedRespPDsOccupStrs.isEmpty()) {
					debugPrint(SELF + "restoring ", removedRespPDsOccupStrs.size(),
							" response pairs with name ", authOrbStr, 
							" but mismatching rows ", removedRespPDsRowNums);
					respColOccupStrs.addAll(removedRespPDsOccupStrs);
					respColRowNums.addAll(removedRespPDsRowNums);
				} // if there are items with same name but mismatched rows to restore
			} // for each response item
		} // for each column
		evalResult.isSatisfied = isPositive;
		return evalResult;
	} // fixedHeights(int, String)

	/** Determines whether the relative positions of orbitals or states (to each other
	 * or to the energy levels) in the author's diagram match those in the response
	 * diagram.
	 * @param	colCt	the number of columns in the diagrams
	 * @param	orbOrState	orbitals or maxima and minima
	 * the author's diagram, modified according to the requirement of the calling method
	 * @return	results of the evaluation
	 */
	protected OneEvalResult relativeEnergies(int colCt, String orbOrState) {
		final String SELF = "EDiagramDiff.relativeEnergies: ";
		final OneEvalResult evalResult = new OneEvalResult();
		// all pair distances must match
		debugPrint(SELF + "respOccupStrs: ", respOccupStrs);
		debugPrint(SELF + "authOccupStrs: ", authOccupStrs);
		final PairDistances respPDs = 
				getDistances(colCt, respOccupStrs, respRowNums);
		final PairDistances authPDs = 
				getDistances(colCt, authOccupStrs, authRowNums);
		debugPrint(SELF + "respPDs: ", respPDs);
		debugPrint(SELF + "authPDs: ", authPDs);
		final PairDistances authPDsX = new PairDistances();
		final String X = String.valueOf(RCDCell.UNKNOWN);
		// set aside author pairs with unknown states (applies to RCDs only)
		for (int authNum = authPDs.pairs.size() - 1; authNum >= 0; authNum--) {
			final String authPair = authPDs.pairs.get(authNum);
			if (authPair.indexOf(X) >= 0) {
				authPDs.pairs.remove(authNum);
				authPDsX.pairs.add(authPair.replaceAll(X, "."));
				authPDsX.distances.add(authPDs.distances.remove(authNum));
				continue;
			} // if a cell has an unknown state
		} // for each author pair
		final boolean haveUnknowns = !authPDsX.pairs.isEmpty();
		if (haveUnknowns) {
			debugPrint(SELF + "looking for author pairs in which all "
					+ "author cells have known states: ", authPDs);
		} // if have author cells with unknown states
		boolean pairsMatch = matchPairs(respPDs, authPDs);
		if (pairsMatch && haveUnknowns) {
			debugPrint(SELF + "looking for author pairs in which all "
					+ "author cells have unknown states: ", authPDsX);
			pairsMatch = matchPairs(respPDs, authPDsX);
		} // if need to find
		evalResult.isSatisfied = isPositive == pairsMatch;
		if (!pairsMatch && evalResult.isSatisfied && oper == EXACTLY) {
			evalResult.autoFeedback = new String[]
					{"The relative heights of one or more " + orbOrState 
					+ " in your diagram are incorrect."};
		} // if need autofeedback
		return evalResult;
	} // relativeEnergies(int, String)

	/** For each pair in author's diagram, can we find one in response 
	 * diagram with matching contents and distance?
	 * @param	respPDs	pairs of occupied cells in response and the distances
	 * between them
	 * @param	authPDs	pairs of occupied cells in author's diagram and the 
	 * distances between them
	 * @return	true if all author pairs are found in response at matching
	 * distances
	 */
	private boolean matchPairs(PairDistances respPDs, PairDistances authPDs) {
		final String SELF = "EDiagramDiff.matchPairs: ";
		for (int authNum = 0; authNum < authPDs.pairs.size(); authNum++) {
			final String authPair = authPDs.pairs.get(authNum);
			final int authDistance = authPDs.distances.get(authNum).intValue();
			debugPrint(SELF + "looking for ", authPair, " with distance ",
					authDistance, " in response diagram.");
			final PairDistances removedRespPDs = new PairDistances();
			// pair names not necessarily unique, so we need to
			// iterate through each pair in author's diagram with same name in
			// order to find match
			while (true) {
				int posnInResp = -1;
				for (int respNum = 0; respNum < respPDs.pairs.size(); respNum++) {
					final String respPair = respPDs.pairs.get(respNum);
					if (respPair.matches(authPair)) {
						posnInResp = respNum;
						break;
					} // if authPair regular expression found in respPair
				} // for each response pair
				if (posnInResp < 0) {
					debugPrint(SELF + "failed to find ", authPair, 
							" with distance ", authDistance, 
							" in response diagram.");
					return false;
				} // if no match
				final String respPair = respPDs.pairs.remove(posnInResp);
				final Integer respDistance = 
						respPDs.distances.remove(posnInResp);
				final boolean match = 
						compare(authDistance, respDistance.intValue());
				if (!match) {
					debugPrint(SELF + "found response pair with same name but "
							+ "mismatched distance ", respDistance, 
							"; setting aside to restore later.");
					removedRespPDs.pairs.add(respPair);
					removedRespPDs.distances.add(respDistance);
				} else {
					debugPrint(SELF + "found response pair that matched.");
					break;
				} // if match
			} // while looking for a match
			if (!removedRespPDs.pairs.isEmpty()) {
				debugPrint(SELF + "restoring ", removedRespPDs.pairs.size(),
						" response pairs with name ", authPair, 
						" but mismatching distances ", removedRespPDs.distances);
				respPDs.pairs.addAll(removedRespPDs.pairs);
				respPDs.distances.addAll(removedRespPDs.distances);
			} // if there are pairs with same name but mismatched distances to restore
		} // for each author pair
		return true;
	} // matchPairs(PairDistances, PairDistances)

	/** Creates parallel lists of joined String representations of 
	 * pairs of orbital groups or maxima/minima and their mutual distances.
	 * @param	colCt	the number of columns in the diagrams
	 * @param	occupStrs	list of lists of strings representing the occupied 
	 * cells of the diagram
	 * @param	rowNums	list of lists of row numbers of the occupied cells of 
	 * the diagram
	 * @return	distances between the pairs of occupied cells
	 */
	private PairDistances getDistances(int colCt, 
			List<ArrayList<String>> occupStrs,
			List<ArrayList<Integer>> rowNums) {
		final List<String> pairs = new ArrayList<String>();
		final List<Integer> distances = new ArrayList<Integer>();
		for (int col1 = 1; col1 <= colCt; col1++) {
			final List<String> colOccupStrs1 = occupStrs.get(col1 - 1);
			final List<Integer> colRowNums1 = rowNums.get(col1 - 1);
			final int numColOccupStrs1 = colOccupStrs1.size();
			for (int cellNum1 = 0; cellNum1 < numColOccupStrs1; cellNum1++) {
				final String cell1 = colOccupStrs1.get(cellNum1);
				for (int col2 = col1; col2 <= colCt; col2++) {
					final List<String> colOccupStrs2 = occupStrs.get(col2 - 1);
					final List<Integer> colRowNums2 = rowNums.get(col2 - 1);
					final int numColOccupStrs2 = colOccupStrs2.size();
					final int start = (col2 == col1 ? cellNum1 + 1 : 0);
					for (int cellNum2 = start; cellNum2 < numColOccupStrs2; cellNum2++) {
						final String cell2 = colOccupStrs2.get(cellNum2);
						final String pair = Utils.toString("col", col1, '_',
								cell1, "_to_", "col", col2, '_', cell2);
						final int distance = colRowNums1.get(cellNum1).intValue() 
								- colRowNums2.get(cellNum2).intValue();
						pairs.add(pair);
						distances.add(Integer.valueOf(distance));
					} // for each orbital group or maximum/minimum in the column
				} // for each column
			} // for each orbital group or maximum/minimum in the column
		} // for each column
		return new PairDistances(pairs, distances);
	} // getDistances(int, List<ArrayList<String>>, List<ArrayList<Integer>>)

	/** Determines whether two orbital-orbital or state-state distances match 
	 * according to the matching algorithm defined by <code>energies</code>.
	 * @param	respNum	the response orbital or state row number (FIXED_HEIGHT) or the 
	 * distance between a pair of response orbitals or states
	 * @param	authNum	the author's orbital or state row number (FIXED_HEIGHT) or the 
	 * distance between a pair of author's orbitals or states
	 * @return	true if the distances match
	 */
	private boolean compare(int respNum, int authNum) {
		boolean match = false;
		if (energies == FIXED_HEIGHT) {
			match = (Math.abs(respNum - authNum) <= tolerance);
		} else {
			final int respSign = MathUtils.sign(respNum);
			final int authSign = MathUtils.sign(authNum);
			match = (respSign == authSign);
			if (energies == RELATIVE_HEIGHT && match) {
				final int diff = respNum * respSign - authNum * authSign;
				match = (Math.abs(diff) <= tolerance);
			} // if energies and same sign 
		} // if energies
		debugPrint("EDiagramDiff.compare: ",
				(energies == FIXED_HEIGHT 
					? "row numbers " : "intradiagram distances "),
				respNum, " and ", authNum, (match ? " are " : " are not "),
				(energies == SIGNUMS ? "of the same sign"
					: "equal within the tolerance " + tolerance), ".");
		return match;
	} // compare(int, int)

	/** Determines whether the lines that connect the occupied cells in the
	 * two diagrams are the same.  "Same" here means that the energy order of
	 * the occupied cells that are connected are the same within each column; 
	 * e.g., the second-highest energy cell in column 2 is connected to the 
	 * highest energy cell in column 3 in both diagrams.
	 * @return	true if the lines connecting occupied cells are the same
	 */
	protected boolean correlationsOK() {
		final String SELF = "EDiagramDiff.correlationsOK: ";
		final List<String> respLines = respDiagram.getCompareLines();
		final List<String> authLines = authDiagram.getCompareLines();
		final int respSize = respLines.size();
		final int authSize = authLines.size();
		if (respSize != authSize) {
			debugPrint(SELF + "respLines ", respLines, " and authLines ",
					authLines, " are of different sizes; "
					+ "correlations don't match.");
			return false;
		}
		final String[] respLinesArr = respLines.toArray(new String[respSize]);
		final String[] authLinesArr = authLines.toArray(new String[authSize]);
		Arrays.sort(respLinesArr);
		Arrays.sort(authLinesArr);
		debugPrint(SELF + "response sorted flattened lines = ", respLinesArr);
		debugPrint(SELF + "author's sorted flattened lines = ", authLinesArr);
		return Arrays.equals(respLinesArr, authLinesArr);
	} // correlationsOK()

	/* *************** Get-set methods *****************/

	/** Gets whether the evaluator is satisfied by the response matching or not
	 * matching the author's diagram.
	 * @return	true if the evaluator is satisfied by the response matching the
	 * author's diagram.
	 */
	public boolean getIsPositive() 					{ return isPositive; }
	/** Sets whether the evaluator is satisfied by the response matching or not
	 * matching the author's diagram.
	 * @param	isPositive	true if the evaluator is satisfied by the response
	 * matching the author's diagram.
	 */
	public void setIsPositive(boolean isPositive)	{ this.isPositive = isPositive; }
	/** Gets how to evaluate the energies of the diagram.
	 * @return	how to evaluate the energies of the diagram
	 */
	public int getEnergies() 						{ return energies; } 
	/** Sets how to evaluate the energies of the diagram.
	 * @param	energies	how to evaluate the energies of the diagram
	 */
	public void setEnergies(int energies) 			{ this.energies = energies; } 
	/** Gets the tolerance of the energies.
	 * @return	tolerance of the energies
	 */
	public int getTolerance() 						{ return tolerance; } 
	/** Sets the tolerance of the energies.
	 * @param	tol	tolerance of the energies
	 */
	public void setTolerance(int tol)				{ tolerance = tol; }
	/** Gets to what extent to evaluate the diagram.
	 * @return	to what extent to evaluate the diagram
	 */
	public int getExtent() 							{ return compareExtent; } 
	/** Sets to what extent to evaluate the diagram.
	 * @param	extent	to what extent to evaluate the diagram
	 */
	public void setExtent(int extent) 				{ compareExtent = extent; } 
	/** Gets whether the author's diagram is all of or a subset of the response 
	 * diagram.
	 * @return	how to compare the response diagram to the author's diagram
	 */
	public int getOper() 							{ return oper; }
	/** Sets whether the author's diagram is all of or a subset of the response 
	 * diagram.
	 * @param	theOper	how to compare the response diagram to the
	 * author's diagram
	 */
	public void setOper(int theOper) 				{ oper = theOper; }
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 					{ return false; }
	/** Not used.  Required by interface.
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName)			{ /* intentionally empty */ }

} // EDiagramDiff

