package com.epoch.mechanisms;

import chemaxon.struc.graphics.MPolyline;
import chemaxon.struc.MPoint;
import com.epoch.mechanisms.mechConstants.MechConstants;
import org.apache.commons.lang.builder.HashCodeBuilder;

/** Describes a straight arrow (reaction or resonance) connecting two stages of
 * a mechanism. */
class MechArrow implements MechConstants {
	//----------------------------------------------------------------------
	//							members
	//----------------------------------------------------------------------
	/** Straight arrow from the MDoc. */
	private MPolyline arrow;
	/** Stage at which this straight arrow originates. */ 
	transient private MechStage prevStage;
	/** Stage to which this straight arrow points. */ 
	transient private MechStage nextStage;
	/** 0-Based index of this object in the MDoc. */
	private int objectNumber;
	
	//----------------------------------------------------------------------
	//							constructors
	//----------------------------------------------------------------------	
	/** Constructor. */
	MechArrow() {
		// intentionally empty
	} // MechArrow()

	/** Constructor. 
	 * @param	newArrow	the arrow from the MDoc
	 * @param	objNum	0-based index of this object in the MDoc
	 */
	MechArrow(MPolyline newArrow, int objNum) { 
		arrow = newArrow;
		objectNumber = objNum;
	} // MechArrow(MPolyline, int)

	//----------------------------------------------------------------------
	//							setStagesPrevNext
	//----------------------------------------------------------------------	
	/** Sets the previous and next stages connected by this straight arrow.
	 * @param	prev	the previous stage
	 * @param	next	the next stage
	 */
	public void setStagesPrevNext(MechStage prev, MechStage next) { 
		prevStage = prev;
		nextStage = next;
	} // setStages()
	
	//----------------------------------------------------------------------
	//							short get-set methods
	//----------------------------------------------------------------------	
	/** Gets the arrow object from the MDoc.
	 * @return	arrow from the MDoc
	 */
	public MPolyline getArrow() 			{ return arrow; }
	/** Sets the arrow object from the MDoc.
	 * @param	newArr	arrow from the MDoc
	 */
	public void setArrow(MPolyline newArr)	{ arrow = newArr; }
	/** Gets the object number of this straight arrow in the MDoc. 
	 * @return	0-based number of this arrow in the MDoc
	 */
	public int getObjectNumber()			{ return objectNumber; }
	/** Sets the object number of this straight arrow in the MDoc. 
	 * @param	objNum	0-based number of this arrow in the MDoc
	 */
	public void setObjectNumber(int objNum) { objectNumber = objNum; } 
	/** Gets whether this straight arrow is a resonance arrow. 
	 * @return	true if this arrow is a resonance arrow
	 */
	public boolean isResonant() 			{ return hasWedgeAt(TAIL); }
	/** Gets either end of the arrow or the midpoint.
	 * @param	kind	which point to get
	 * @return	a point of the arrow
	 */
	public MPoint getPoint(int kind) 		{ return arrow.getPointRef(kind, null); } 
	/** Gets a clone of either end of the arrow or the midpoint.
	 * @param	kind	which point to get
	 * @return	a clone of the point of the arrow
	 */
	public MPoint getPointClone(int kind)	{ return arrow.getPoint(kind); } 
	/** Gets the stage at which this straight arrow originates.
	 * @return	the previous stage
	 */
	public MechStage getPrevStage()			{ return prevStage; }
	/** Gets the stage to which this straight arrow points.
	 * @return	the next stage
	 */
	public MechStage getNextStage() 		{ return nextStage; }
	/** Gets whether there is a wedge at both ends of a straight arrow. 
	 * Used to determine whether the arrow is a resonance arrow.
	 * @param	end	HEAD or TAIL
	 * @return	true if there is a wedge
	 */
	public boolean hasWedgeAt(int end) 		{ return (arrow.getArrowLength(end) != 0); }

	//----------------------------------------------------------------------
	//							equals
	//----------------------------------------------------------------------	
	/** Gets whether an arrow is of the same kind as this one.
	 * @param	other	another arrow
	 * @return	true if the arrow equals this one
	 */
	@Override
	public boolean equals(Object other) {
		return other instanceof MechArrow && other != null 
				&& isResonant() == ((MechArrow) other).isResonant();
	} // equals(Object)

	/** Creates a hash code summarizing this object.
	 * @return	the hash code
	 */
	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37).append(isResonant()).toHashCode();
	} // hashCode()

} // MechArrow

