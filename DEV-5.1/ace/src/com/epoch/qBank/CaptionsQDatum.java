package com.epoch.qBank;

import com.epoch.genericQTypes.TableQ;
import com.epoch.qBank.qBankConstants.CaptionsQDatumConstants;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.Arrays;

/** Stores captions of rows or columns in table questions of columns in orbital
 * energy diagram questions. */
public class CaptionsQDatum extends QDatum implements CaptionsQDatumConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Captions. In complete-the-table Qs, up to n for n rows, n+1 for n 
	 * columns. In orbital energy diagrams, 3. */
	public transient String[] captions = new String[0];

	/** Constructor. */
	public CaptionsQDatum() {
		dataType = TEXT;
		dataId = 0;
		serialNo = 1;
		phraseId = 0;
	} // CaptionsQDatum()

	/** Copy constructor. 
	 * @param	copy	the question datum to be copied
	 */
	public CaptionsQDatum(QDatum copy) {
		dataId = copy.dataId;
		dataType = copy.dataType;
		serialNo = copy.serialNo;
		questionId = copy.questionId;
		data = copy.data;
		name = copy.name;
		phraseId = copy.phraseId;
		if (copy instanceof CaptionsQDatum) {
			final CaptionsQDatum copyCast = (CaptionsQDatum) copy;
			captions = Arrays.copyOf(copyCast.captions, 
					copyCast.captions.length);
		} // if copying a CaptionsQDatum
	} // CaptionsQDatum(QDatum)

	/** Gets whether this datum represents a row or a column.
	 * @return	true if it is a row
	 */
	public boolean isRow()				{ return serialNo == TableQ.ROW_DATA + 1; }
	/** Gets the number of rows or columns.  
	 * @return	the number of rows or columns
	 */
	public int getNumRowsOrCols()		{ return MathUtils.parseInt(data); }

	/** Converts the row or column number and captions to a view suitable for 
	 * display in the question authoring tool.
	 * @param	chemFormatting	whether to eschew chemistry formatting
	 * @return	string view of the captions
	 */
	public String toDisplay(boolean chemFormatting) {
		return Utils.toString(
				rowsColumnsToDisplay(chemFormatting),
				captionsToDisplay(chemFormatting));
	} // toDisplay(boolean)

	/** Converts the row or column number and captions to a view suitable for 
	 * display in the question authoring tool.
	 * @param	chemFormatting	whether to eschew chemistry formatting
	 * @return	string view of the captions
	 */
	private StringBuilder rowsColumnsToDisplay(boolean chemFormatting) {
		final int numRowsOrCols = getNumRowsOrCols();
		final StringBuilder bld = Utils.getBuilder(
				numRowsOrCols, isRow() ? " row" : " column");
		if (numRowsOrCols != 1) bld.append('s');
		return bld;
	} // captionsToDisplay(boolean)

	/** Converts the row or column number and captions to a view suitable for 
	 * display in the question authoring tool.
	 * @param	chemFormatting	whether to eschew chemistry formatting
	 * @return	string view of the captions
	 */
	protected StringBuilder captionsToDisplay(boolean chemFormatting) {
		final StringBuilder bld = new StringBuilder();
		if (!Utils.membersAreEmpty(captions)) {
			bld.append(" with captions: ");
			boolean first = true;
			for (final String caption : captions) {
				if (first) first = false;
				else bld.append(", ");
				bld.append(Utils.isEmpty(caption) ? "[<i>none</i>]"
						: chemFormatting 
						? Utils.spanString(Utils.toDisplay(caption))
						: Utils.spanString(caption));
			} // for each caption
		} // if there are captions
		return bld;
	} // captionsToDisplay(boolean)

} // CaptionsQDatum
