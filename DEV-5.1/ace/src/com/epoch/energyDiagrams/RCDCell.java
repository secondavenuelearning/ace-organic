package com.epoch.energyDiagrams;

import com.epoch.energyDiagrams.diagramConstants.RCDCellConstants;
import com.epoch.exceptions.ParameterException;
import com.epoch.exceptions.VerifyException;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import org.apache.commons.lang.builder.HashCodeBuilder;

/** A cell in a reaction coordinate diagram.  May contain a molecule such as 
 * an intermediate or transition state. */
public class RCDCell extends DiagramCell implements RCDCellConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** The state (maximum or minimum). */
	private int state = UNKNOWN;

	/** Constructor.
	 * @param	r	row
	 * @param	c	column
	 */
	public RCDCell(int r, int c) {
		row = r;
		col = c;
		label = UNKNOWN;
	} // RCDCell(int, int)

	/** Constructor.
	 * @param	r	row
	 * @param	c	column
	 * @param	data	data encoding state
	 */
	public RCDCell(int r, int c, String data) {
		row = r;
		col = c;
		label = UNKNOWN;
		setContents(data);
	} // RCDCell(int, int, String)

	/** Sets the contents of this table cell.
	 * @param	data	data encoding state: consists only of label
	 */
	public final void setContents(String data) {
		final String SELF = "RCDCell.setContents: ";
		String myData = data;
		if (data.indexOf(STATE_LABEL_SEP) >= 0) { // older format
			final String[] stateAndLabel = myData.split(STATE_LABEL_SEP);
			myData = stateAndLabel[LABEL];
		} // if data is in old format
		label = MathUtils.parseInt(myData, UNKNOWN);
	} // setContents(String, int)

	/** Sets the contents of this table cell.
	 * @param	data	data encoding state: consists only of label (old format,
	 * consisted of state : label)
	 * @param	numLabels	number of labels this cell may have
	 * @throws	ParameterException	if the label is out of range
	 */
	public void setContents(String data, int numLabels) throws ParameterException {
		setContents(data);
		if (numLabels > 0 && !MathUtils.inRange(label, new int[] {0, numLabels})) {
			label = 0;
			throw new ParameterException("The label of the state in row "
					+ row + " and column " + col + " is out of range.");
		} // if not in range
	} // setContents(String, int)

	/** Calculates whether the state is a maximum, minimum, or inflection point 
	 * by looking at the states in adjacent columns to which it is connected. 
	 * @throws	ParameterException	if the state is not known 
	 * @throws	VerifyException	if the state can't be calculated from the
	 * connected cells
	 */
	void calcState() throws ParameterException, VerifyException {
		final String SELF = "RCDCell.calcState: ";
		boolean hasLeft = false;
		int leftVertPosn = SAME;
		boolean hasRight = false;
		int rightVertPosn = SAME;
		for (final DiagramCell connectedCell : connectedCells) {
			if (!inAdjacentColumn(connectedCell)) {
				debugPrint(SELF + "current cell ", getLocnString(),
						" is connected to cell ", connectedCell.getLocnString());
				throw new VerifyException("You should connect "
						+ "states with lines only if they are in "
						+ "adjacent columns.");
			} // if connected cell is not in adjacent column
			final int[] directions = directionsTo(connectedCell);
			debugPrint(SELF + "the current cell ", getLocnString(), 
					" is connected to cell ", connectedCell.getLocnString(),
					"; the connected cell is ", 
					(directions[VERTICAL] == UP ? "above"
						: directions[VERTICAL] == DOWN ? "below" 
						: "in same row as"), " and ", 
					(directions[HORIZONTAL] == LEFT ? "left of"
						: directions[HORIZONTAL] == RIGHT ? "right of"
						: "in same column as"), 
					" the current cell.");
			if (directions[VERTICAL] == SAME) {
				debugPrint(SELF + "state in adjacent column "
						+ "has same energy.");
				throw new VerifyException("States in adjacent columns "
						+ "should not have the same energy.");
			} else if (directions[HORIZONTAL] == LEFT) {
				if (hasLeft && leftVertPosn != directions[VERTICAL]) {
					debugPrint(SELF + "two states in adjacent column "
							+ "are in opposite directions.");
					setState(INCONSISTENT);
					throw new VerifyException(HEIGHT_ERROR);
				} else if (!hasLeft) {
					hasLeft = true;
					leftVertPosn = directions[VERTICAL];
				} // if there's already a connected state to the left
			} else if (directions[HORIZONTAL] == RIGHT) {
				if (hasRight && rightVertPosn != directions[VERTICAL]) {
					debugPrint(SELF + "two states in adjacent column "
							+ "are in opposite directions.");
					setState(INCONSISTENT);
					throw new VerifyException(HEIGHT_ERROR);
				} else if (!hasRight) {
					hasRight = true;
					rightVertPosn = directions[VERTICAL];
				} // if there's already a connected state to the right
			} else { // directions[HORIZONTAL] == SAME; shouldn't happen
				debugPrint(SELF + "connected state in same column.");
				throw new VerifyException("States in the same column "
						+ "may not be connected with lines.");
			} // if horizontal relative positions of the cells
		} // for each cell connected to this one
		if (!hasLeft && !hasRight) {
			debugPrint(SELF + "state has no adjacent states on which "
					+ "to base a calculation.");
			throw new VerifyException("ACE cannot determine whether "
					+ "a state is a maximum or minimum if it is not "
					+ "connected to any states in neighboring columns.");
		} else if ((!hasLeft || leftVertPosn == UP)
				&& (!hasRight || rightVertPosn == UP)) {
			setState(MINIMUM);
		} else if ((!hasLeft || leftVertPosn == DOWN)
				&& (!hasRight || rightVertPosn == DOWN)) {
			setState(MAXIMUM);
		} else {
			setState(INFLECTION);
		} // if state positions on either side
		debugPrint(SELF + "calculated state in cell ",
				getLocnString(), " as ", (getState() == MAXIMUM
					? "maximum" : getState() == MINIMUM 
					? "minimum" : "inflection point"));
	} // calcState()

	/** Sets the state. 
	 * @param	aState	the state
	 * @throws	ParameterException	if the state is not known 
	 */
	final private void setState(int aState) throws ParameterException {
		if (!MathUtils.inRange(aState, STATE_RANGE)) {
			Utils.alwaysPrint("RCDCell.setState: "
					+ "can't set state type to ", aState);
			throw new ParameterException(
					"Can't set state type to " + aState);
		} // if state is illegal
		state = aState;
	} // setState(int)

	/** Gets whether this cell is occupied.  Note that an occupied cell with no
	 * label has label 0.
	 * @return	true if this cell is occupied
	 */
	public boolean isOccupied()				{ return label != UNKNOWN; }
	/** Gets this cell's state (maximum or minimum). 
	 * @return	the state
	 */
	public int getState()					{ return state; }
	/** Gets the toDisplay() name of this cell's state (maximum or minimum). 
	 * @return	the name of the state
	 */
	public String getStateName() 			{ return getStateName(state); }
	/** Gets the toDisplay() name of a state (maximum or minimum).
	 * @param	aState	the state
	 * @return	the name of the state
	 */
	public static String getStateName(int aState) { 
		return (aState == UNKNOWN ? "unknown"
				: aState < NAMES.length
				? Utils.toDisplay(NAMES[aState]) : ""); 
	} // getStateName(int)

	/** Compares the contents of this cell to another one.  They are the 
	 * same if they are the same state and have the same label.
	 * @param	theOther	the cell to compare
	 * @return	true if they contain the same state with the same label
	 */
	@Override
	public boolean equals(Object theOther) {
		boolean isEqual = false;
		if (theOther instanceof RCDCell) {
			final RCDCell cell = (RCDCell) theOther;
			isEqual = getState() == cell.getState()
					&& getLabel() == cell.getLabel();
		} // if instanceof
		return isEqual;
	} // equals(Object)

	/** Creates a hash code describing this object.
	 * @return the hash code
	 */
	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37).
			append(getState()).append(getLabel()).toHashCode();
	} // hashCode

	/** Converts the state's label into a String.
	 * @return	the string encoding the state's label
	 */
	public String getDescrip() {
		return Utils.toString(getState(), STATE_LABEL_SEP, getLabel());
	} // getDescrip()

	/** Converts the state's label into a String.
	 * @return	the string encoding the state's label
	 */
	public String toString() {
		return String.valueOf(getLabel());
	} // toString()

} // RCDCell
