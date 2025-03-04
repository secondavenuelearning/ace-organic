package com.epoch.energyDiagrams;

import com.epoch.energyDiagrams.diagramConstants.YAxisConstants;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.text.NumberFormat;
import java.util.Arrays;

/** Represents the y-axis labels of an energy diagram. */
public class YAxisScale implements YAxisConstants {

	/** The array of data required to construct the y-axis scale. 
	 * Contains the 1-based first row, the first quantity, the row 
	 * increment, the quantity increment, and the name 
	 * of the energy unit.
	 */
	transient private String[] data = new String[NUM_SCALE_DATA];
	/** Whether this class contains data that should be displayed. */
	transient private boolean haveLabels = false;

	private void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Constructor.  */
	public YAxisScale() { 
		Arrays.fill(data, "");
	}

	/** Constructor. 
	 * @param	newData	array containing the 1-based first row, the first 
	 * quantity, the row increment, the quantity increment, and the name 
	 * of the energy unit
	 */
	public YAxisScale(String[] newData) {
		if (newData.length >= NUM_SCALE_DATA) {
			data = Arrays.copyOf(newData, NUM_SCALE_DATA);
			haveLabels = true;
		} else Arrays.fill(data, "");
	} // YAxisScale(String[])

	/** Copy constructor. 
	 * @param	copy	the scale to be copied
	 */
	public YAxisScale(YAxisScale copy) {
		if (copy != null && copy.haveLabels()) {
			data = Arrays.copyOf(copy.getData(), NUM_SCALE_DATA);
			haveLabels = true;
		} else Arrays.fill(data, "");
	} // YAxisScale(YAxisScale)

	/** Gets the data of this YAxisScale.
	 * @return	the array of data
	 */
	public String[] getData()				{ return data; }
	/** Gets whether this class's members have been populated.
	 * @return	true if this member's classes have been populated
	 */
	public boolean haveLabels() 			{ return haveLabels; }
	/** Gets the 1-based first row on which a label should be placed.
	 * @return	1-based first row on which a label should be placed
	 */
	public String getRowInit()				{ return data[ROW_INIT]; }
	/** Gets the quantity of the unit that appears on the initial row.
	 * @return	quantity of the unit that appears on the initial row
	 */
	public String getQuantInit()			{ return data[QUANT_INIT]; }
	/** Gets the interval between labeled rows.
	 * @return	interval between labeled rows
	 */
	public String getRowIncrement()			{ return data[ROW_INCREMENT]; }
	/** Gets the change in the quantity per interval between rows
	 * @return	change in the quantity per interval between rows
	 */
	public String getQuantIncrement()		{ return data[QUANT_INCREMENT]; }
	/** Gets the energy unit.
	 * @return	the energy unit
	 */
	public String getUnit()					{ return data[UNIT]; }

	/** Gets an array of labels for rows of an energy diagram representing the
	 * energy quantities.
	 * @param	numRows	number of rows in the diagram
	 * @return	array of row labels to be used in HTML of the diagram
	 */
	String[] getLabels(int numRows) {
		final String SELF = "YAxisScale.getLabels: ";
		debugPrint(SELF + "getting y-axis labels from data: ", data);
		final String[] labels = new String[numRows];
		Arrays.fill(labels, "");
		if (!haveLabels) return labels;
		final int[] initSigFigs = 
				MathUtils.countPartSigFigs(getQuantInit());
		final int[] incrSigFigs = 
				MathUtils.countPartSigFigs(getQuantIncrement());
		final int fractionSigFigs = Math.max(initSigFigs[1], incrSigFigs[1]);
		final NumberFormat numberFormat = NumberFormat.getInstance();
		numberFormat.setMinimumFractionDigits(fractionSigFigs);
		numberFormat.setMaximumFractionDigits(fractionSigFigs);
		final double quantInit = MathUtils.parseDouble(getQuantInit());
		final double quantIncrement = 
				MathUtils.parseDouble(getQuantIncrement());
		final int rowInit = MathUtils.parseInt(getRowInit()) - 1;
		final int rowIncrement = MathUtils.parseInt(getRowIncrement());
		for (int rowNum = 0; rowNum < numRows; rowNum++) {
			final int fromFirst = rowNum - rowInit;
			if (fromFirst % rowIncrement == 0) {
				final int numIncrements = fromFirst / rowIncrement;
				final double quant = quantInit + quantIncrement * numIncrements;
				final int tableRowNum = numRows - rowNum - 1;
				labels[tableRowNum] = numberFormat.format(quant);
			} // if should add label
		} // for each row
		debugPrint(SELF + "initSigFigs = ", initSigFigs, "\nincrSigFigs = ",
				incrSigFigs, "\nfractionSigFigs = ", fractionSigFigs,
				"\nquantInit = ", quantInit, "\nquantIncrement = ", 
				quantIncrement, "\nlabels = ", labels);
		return labels;
	} // getLabels(int)

	/** Gets an English description of the y-axis scale information. 
	 * @return	an English description
	 */
	public String toEnglish() {
		return Utils.toString("Starting from row ", 
				MathUtils.parseInt(getRowInit()) + 1,
				" as ", getQuantInit(), ' ', getUnit(), ", every ", 
				Utils.toOrdinal(MathUtils.parseInt(getRowIncrement())), 
				" row changes by ", getQuantIncrement(), ' ', getUnit(), '.');
	} // toEnglish()

} // YAxisScale
