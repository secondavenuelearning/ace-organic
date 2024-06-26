package com.epoch.energyDiagrams;

import com.epoch.energyDiagrams.diagramConstants.OrbitalConstants;
import com.epoch.exceptions.ParameterException;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import org.apache.commons.lang.builder.HashCodeBuilder;

/** An atomic or molecular orbital. */
public class Orbital implements OrbitalConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Type of orbital. */
	private int type;
	/** Occupancy of orbital. */
	private int occupancy;

	/** Constructor.
	 * @param	aType	type of orbital
	 * @throws	ParameterException	if the type of orbital is not known 
	 */
	public Orbital(int aType) throws ParameterException {
		setType(aType);
	} // Orbital(int)

	/** Constructor.
	 * @param	descrip	type and occupancy of orbital
	 * @throws	ParameterException	if the type of orbital is not known 
	 */
	public Orbital(int[] descrip) throws ParameterException {
		setType(descrip[0]);
		setOccupancy(descrip[1]);
	} // Orbital(int)

	/** Constructor.
	 * @param	aType	type of orbital
	 * @param	occup	occupancy of the orbital
	 * @throws	ParameterException	if the type of orbital is not known 
	 * or the occupancy is not 0&ndash;2
	 */
	public Orbital(int aType, int occup) throws ParameterException {
		setType(aType);
		setOccupancy(occup);
	} // Orbital(int, int)

	/** Sets the type of orbital. 
	 * @param	aType	type of orbital
	 * @throws	ParameterException	if the type of orbital is not known 
	 */
	final private void setType(int aType) throws ParameterException {
		if (!MathUtils.inRange(aType, TYPE_RANGE)) {
			Utils.alwaysPrint("Orbital: can't set type to ", aType);
			throw new ParameterException(
					"Can't set orbital type to " + aType);
		} // if orbital type is illegal
		type = aType;
	} // setType(int)

	/** Sets the occupancy of the orbital. 
	 * @param	occup	occupancy of the orbital
	 * @throws	ParameterException	if the occupancy is not 0&ndash;2
	 */
	final private void setOccupancy(int occup) throws ParameterException {
		if (!MathUtils.inRange(occup, OCCUP_RANGE)) {
			Utils.alwaysPrint("Orbital: can't set occupancy to ", occup);
			throw new ParameterException(
					"Can't set orbital occupancy to " + occup);
		} // if occupancy is illegal
		occupancy = occup;
	} // setOccupancy(int)

	/** Gets the type of orbital. 
	 * @return	the type of orbital
	 */
	public int getType()								{ return type; }
	/** Gets the number of types of orbital. 
	 * @return	the number of types of orbital
	 */
	public static int getNumTypes()						{ return INDIV_NAMES.length; }
	/** Gets the orbital occupancy. 
	 * @return	the orbital occupancy
	 */
	public int getOccupancy()							{ return occupancy; }
	/** Gets the toPopupMenuDisplay() name of the orbital. 
	 * @return	the name of the orbital
	 */
	public String getPopupMenuName() 					{ return getPopupMenuName(type); }
	/** Gets the toPopupMenuDisplay() name of an orbital. 
	 * @param	aType	type of orbital
	 * @return	the name of the orbital
	 */
	public static String getPopupMenuName(int aType) 	{ return Utils.toPopupMenuDisplay(
																INDIV_NAMES[aType]); }
	/** Gets the toDisplay() name of the orbital. 
	 * @return	the name of the orbital
	 */
	public String getDisplayName() 						{ return getDisplayName(type); }
	/** Gets the toDisplay() name of an orbital.  Italicizes s and p.
	 * @param	aType	type of orbital
	 * @return	the name of the orbital
	 */
	public static String getDisplayName(int aType) { 
		return (!Utils.among(aType, SP, SP2, SP3) 
				? "<i>" + Utils.toDisplay(INDIV_NAMES[aType]) + "</i>"
				: Utils.toDisplay(INDIV_NAMES[aType])); 
	} // getDisplayName(int)

	/** Changes the orbital occupancy.
	 * @param	change	the increment or decrement
	 * @throws	ParameterException	if the occupancy will go out of the range 
	 * 0&ndash;2
	 */
	public void changeOccupancy(int change) throws ParameterException {
		if (change != 0) {
			final int newOccup = occupancy + change;
			if (MathUtils.inRange(newOccup, OCCUP_RANGE)) {
				occupancy = newOccup;
			} else {
				Utils.alwaysPrint("Orbital.changeOccupancy: "
						+ "occupancy = ", occupancy, ", change = ",
						change, ", request out of range.");
				throw new ParameterException("Orbital occupancy "
						+ occupancy + " can't be " + (change > 0
							? "in" : "de") + "creased by " 
						+ Math.abs(change) + ".");
			} // if change leaves occupancy in range
		} // if occupancy is being changed
	} // changeOccupancy(int)

	/** Compares this orbital to another one.  They are the same if they are the
	 * same type and have the same occupancy.
	 * @param	theOther	the orbital to compare
	 * @return	true if they are the same type of orbital with the same occupancy
	 */
	@Override
	public boolean equals(Object theOther) {
		boolean isEqual = false;
		if (theOther instanceof Orbital) {
			final Orbital orb = (Orbital) theOther;
			isEqual = getType() == orb.getType()
					&& getOccupancy() == orb.getOccupancy();
		} // if instanceof
		return isEqual;
	} // equals(Object)

	/** Creates a hash code describing this object.
	 * @return the hash code
	 */
	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37).
				append(getType()).append(getOccupancy()).toHashCode();
	} // hashCode()

	/** Converts the orbital's type and occupancy into a String.
	 * @return	the string encoding the orbital's type and occupancy
	 */
	public String toString() {
		return Utils.toString(getType(), OEDCell.OCCUP_SEP, getOccupancy());
	} // toString()

} // Orbital
