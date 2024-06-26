package com.epoch.synthesis;

import chemaxon.marvin.io.MolExportException;
import chemaxon.struc.MDocument;
import chemaxon.struc.Molecule;
import com.epoch.chem.ChemUtils;
import com.epoch.chem.MolString;
import com.epoch.synthesis.synthConstants.SynthConstants;
import com.epoch.utils.MathUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Methods for coloring synthesis components.  */
public class SynthUtils implements SynthConstants {

	private static void debugPrint(Object... msg) {
		// printToLog(msg);
	}

	//----------------------------------------------------------------------
	//						 members
	//----------------------------------------------------------------------
	/** Data on the synthesis. */
	transient private SynthParser parsedSynth = null;
	
	//----------------------------------------------------------------------
	//						 constructors
	//----------------------------------------------------------------------
	/** Constructor. */
	public SynthUtils() {
		// intentionally empty
	} // SynthUtils()

	/** Constructor. 
	 * @param	synth	a synthesis
	 */
	public SynthUtils(Synthesis synth) {
		parsedSynth = synth.parsedSynth;
	} // SynthUtils(Synthesis)
	
	//----------------------------------------------------------------------
	//						 getPermutations
	//----------------------------------------------------------------------
	/** Get list of <i>N!</i> permutations of Molecules in an array.  We may 
	 * permute ints instead of Molecules so that we can avoid getting 
	 * permutations of identical compounds. This method is adapted from
	 * http://www.cs.princeton.edu/introcs/23recursion/Permutations.java.html.
	 * @param	substrates	an array of Molecules
	 * @return	a list of arrays with all possible orderings of these Molecules
	 */
	public static List<Molecule[]> getPermutations(Molecule... substrates) {
		final String SELF = "SynthUtils.getPermutations: ";
		final List<Molecule[]> allMolPerms = new ArrayList<Molecule[]>();
		final int n = substrates.length;
		if (n == 0) {
			allMolPerms.add(substrates);
		} else if (ChemUtils.getProperty(substrates[0], RESIZE_NUM) == null) {
			permute(substrates, n, allMolPerms);
		} else {
			// some of the Molecules in substrates are duplicates; use ints
			// assigned during duplication to avoid duplicate permutations
			int[] substrateNums = new int[n];
			for (int subNum = 0; subNum < n; subNum++) {
				substrateNums[subNum] = MathUtils.parseInt(
						ChemUtils.getProperty(substrates[subNum], RESIZE_NUM));
			} // for each substrate
			debugPrint(SELF + "permuting ", substrateNums);
			final List<int[]> allIntPerms = new ArrayList<int[]>();
			permute(substrateNums, n, allIntPerms);
			// get Molecule[] corresponding to each unique int[]
			int arrayNum = 0;
			for (final int[] intArray : allIntPerms) {
				boolean include = true;
				for (final int[] intPerm : allIntPerms) {
					if (intArray == intPerm) break; // pointer equality
					else if (Arrays.equals(intArray, intPerm)) {
						include = false;
						break;
					} // if arrays are equal
				} // for each previous array
				if (include) {
					Molecule[] molArray = new Molecule[intArray.length];
					for (int membNum = 0; membNum < intArray.length; 
							membNum++) {
						molArray[membNum] = 
								substrates[intArray[membNum]].clone();
					} // for each member of intArray
					allMolPerms.add(molArray);
					debugPrint(SELF + "Permutation ", ++arrayNum, ": ", 
							molArray);
				} else {
					debugPrint(SELF + "Permutation ", ++arrayNum, ", ", 
							intArray, " same as previous one; skipping.");
				} // if include this permutation
			} // for each intArray
		} // if there are likely to be duplicate permutations
		return allMolPerms;
	} // getPermutations(Molecule...)

	/** Recursively permute progressively shorter collections of int[].
	 * This method is adapted from
	 * http://www.cs.princeton.edu/introcs/23recursion/Permutations.java.html.
	 * @param	substrates	an int array
	 * @param	n	the number of members of each array
	 * @param	allPerms	list of permutations of ints
	 */
	private static void permute(int[] substrates, int n,
			List<int[]> allPerms) {
		if (n == 1) {
			allPerms.add(substrates.clone());
			return;
		} // if n == 1
		for (int itemNum = 0; itemNum < n; itemNum++) {
			swap(substrates, itemNum, n - 1);
			permute(substrates, n - 1, allPerms);
			swap(substrates, itemNum, n - 1);
		} // for each itemNum
	} // permute(int[], int, List<int[]>)

	/** Recursively permute progressively shorter collections of Molecule[].
	 * This method is adapted from
	 * http://www.cs.princeton.edu/introcs/23recursion/Permutations.java.html.
	 * @param	substrates	an array of Molecules
	 * @param	n	the number of members of each array of Molecules
	 * @param	allPerms	list of permutations of Molecules
	 */
	private static void permute(Molecule[] substrates, int n,
			List<Molecule[]> allPerms) {
		if (n == 1) {
			allPerms.add(substrates.clone());
			return;
		} // if n == 1
		for (int itemNum = 0; itemNum < n; itemNum++) {
			swap(substrates, itemNum, n - 1);
			permute(substrates, n - 1, allPerms);
			swap(substrates, itemNum, n - 1);
		} // for each itemNum
	} // permute(Molecule[], int, List<Molecule[]>)

	/** Swap the ints at substrates pos1 and pos2.
	 * This method is adapted from
	 * http://www.cs.princeton.edu/introcs/23recursion/Permutations.java.html.
	 * @param	substrates	an int array
	 * @param	pos1	position of int to swap
	 * @param	pos2	position of other int to swap
	 */
	private static void swap(int[] substrates, int pos1, int pos2) {
		final int tmp1 = substrates[pos1];
		substrates[pos1] = substrates[pos2];
		substrates[pos2] = tmp1;
	} // swap(int[], int, int)

	/** Swap the Molecules at substrates pos1 and pos2.
	 * This method is adapted from
	 * http://www.cs.princeton.edu/introcs/23recursion/Permutations.java.html.
	 * @param	substrates	an array of Molecules
	 * @param	pos1	position of Molecule to swap
	 * @param	pos2	position of other Molecule to swap
	 */
	private static void swap(Molecule[] substrates, int pos1, int pos2) {
		final Molecule tmp1 = substrates[pos1];
		substrates[pos1] = substrates[pos2];
		substrates[pos2] = tmp1;
	} // swap(Molecule[], int, int)

	//----------------------------------------------------------------------
	//							getXmlWithColoredArrow
	//----------------------------------------------------------------------
	/** Colors the arrow pointing to the next stage.
	 * @param	stage	stage who arrow pointing to the next stage will be
	 * colored
	 * @return	MRV representation of the synthesis with the arrow colored
	 */
	public String getXmlWithColoredArrow(SynthStage stage) {
		final String SELF = "SynthUtils.getXmlWithColoredArrow: ";
		if (stage == null) {
			System.out.println(SELF + "no stage found.");
			return parsedSynth.getResponse();
		} // if no stage
		final int objIndex = stage.getArrowToNext().getObjectNumber();
		final MDocument newMDoc = parsedSynth.getMDocCopy();
		MolString.colorMObject(newMDoc, objIndex, Color.RED);
		try {
			return MolString.toString(newMDoc, MRV);
		} catch (MolExportException e) {
			System.out.println(SELF + "caught "
					+ "MolExportException; returning original XML");
		}
		return parsedSynth.getResponse();
	} // getXmlWithColoredArrow()

	//----------------------------------------------------------------------
	//							colorBoxes, colorBox
	//----------------------------------------------------------------------
	/** Colors a set of boxes.
	 * @param	boxesToColor	list of indices of boxes to color
	 * @return	MRV representation of the synthesis with boxes colored
	 */
	public String colorBoxes(List<Integer> boxesToColor) {
		try {
			final MDocument newMDoc = parsedSynth.getMDocCopy();
			for (final Integer box : boxesToColor) {
				MolString.colorMObject(newMDoc, box.intValue(), Color.RED);
			} // for each box
			return MolString.toString(newMDoc, MRV);
		} catch (MolExportException e) {
			System.out.println("SynthUtils.colorBoxes: "
					+ "MolExportException; "
					+ "cannot export modified document.");
		}
		return parsedSynth.getResponse();
	} // colorBoxes(List<Integer>)

	/** Colors a stage's box.
	 * @param	boxToColor	index of box to color
	 * @return	MRV representation of the synthesis with box colored
	 */
	public String colorBox(int boxToColor) {
		try {
			final MDocument newMDoc = parsedSynth.getMDocCopy();
			MolString.colorMObject(newMDoc, boxToColor, Color.RED);
			return MolString.toString(newMDoc, MRV);
		} catch (MolExportException e) {
			System.out.println("SynthUtils.colorBoxes: "
					+ "MolExportException; "
					+ "cannot export modified document.");
		}
		return parsedSynth.getResponse();
	} // colorBox(int)
	
} // SynthUtils
