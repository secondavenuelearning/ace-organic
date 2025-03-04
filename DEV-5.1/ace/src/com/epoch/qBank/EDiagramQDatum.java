package com.epoch.qBank;

import com.epoch.energyDiagrams.YAxisScale;
import com.epoch.energyDiagrams.diagramConstants.RCDConstants;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.Arrays;

/** Stores pulldown menu options and y-axis scale parameters in energy diagram 
 * questions. */
public class EDiagramQDatum extends CaptionsQDatum implements RCDConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Labels in pulldown menus. */
	public transient String[] labels = new String[0];
	/** Data about the y-axis scale. */
	public transient YAxisScale yAxisScale = new YAxisScale();

	/** Constructor. */
	public EDiagramQDatum() {
		dataType = TEXT;
		dataId = 0;
		serialNo = 1;
		phraseId = 0;
	} // EDiagramQDatum()

	/** Copy constructor. 
	 * @param	copy	the question datum to be copied
	 */
	public EDiagramQDatum(QDatum copy) {
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
			if (copy instanceof EDiagramQDatum) {
				final EDiagramQDatum copyCast2 = (EDiagramQDatum) copy;
				labels = Arrays.copyOf(copyCast2.labels, 
						copyCast2.labels.length);
				yAxisScale = new YAxisScale(copyCast2.yAxisScale);
			} // if copying an EDiagramQDatum
		} // if copying a CaptionsQDatum
	} // EDiagramQDatum(QDatum)

	/** Gets the number of rows in this diagram.
	 * @return	the number of rows
	 */
	public int getNumRows() {
		return MathUtils.parseInt(data.split(QDATA_SEP)[0]);
	} // getNumRows()

	/** Gets the number of columns in this diagram.
	 * @return	the number of columns
	 */
	public int getNumColumns() {
		final String[] dataParts = data.split(QDATA_SEP);
		return (dataParts.length > 1 ? MathUtils.parseInt(dataParts[1]) : 0);
	} // getNumColumns()

	/** Sets the y-axis scale.
	 * @param	yAxisData	array of strings presenting the y-axis scale
	 */
	public void setYAxisScale(String[] yAxisData) {
		yAxisScale = (Utils.isEmpty(yAxisData) ? null 
				: new YAxisScale(yAxisData));
	} // setYAxisScale(String[])

	/** Converts the number of rows/columns, captions, and labels to a view 
	 * suitable for display in the question authoring tool.
	 * @return	string view of the data
	 */
	public String toDisplay() {
		final int numRows = getNumRows();
		final int numColumns = getNumColumns();
		final boolean isRCD = numColumns > 0;
		final StringBuilder bld = Utils.getBuilder(numRows, " row");
		if (numRows != 1) bld.append('s');
		if (isRCD) {
			Utils.appendTo(bld, " and ", numColumns, " column");
			if (numColumns != 1) bld.append('s');
		} // if there are columns as well
		Utils.appendTo(bld, ' ', captionsToDisplay(CHEM_FORMATTING));
		if (!Utils.membersAreEmpty(labels)) {
			Utils.appendTo(bld, "<br/>", isRCD ? "Energy maximum/minimum"
					: "Molecular orbital", " descriptions: ");
			boolean first = true;
			for (final String label : labels) {
				if (first) first = false;
				else bld.append(", ");
				bld.append(Utils.spanString(Utils.toDisplay(label)));
			} // for each label
		} // if there are labels
		if (yAxisScale != null && yAxisScale.haveLabels()) {
			Utils.appendTo(bld, "<br/>", yAxisScale.toEnglish());
		} // if there is a scale for the y-axis
		return bld.toString();
	} // toDisplay()

} // EDiagramQDatum
