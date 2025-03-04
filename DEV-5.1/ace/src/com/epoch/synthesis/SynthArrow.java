package com.epoch.synthesis;

import chemaxon.struc.graphics.MPolyline;
import chemaxon.struc.MPoint;

/** Describes an arrow connecting two stages of a synthesis. */
class SynthArrow {	
	//----------------------------------------------------------------------
	//							members
	//----------------------------------------------------------------------
	/** Arrow from the MDoc. */
	private MPolyline  arrow;
	/** Stage at which this arrow originates. */ 
	transient private SynthStage prevStage;
	/** Stage to which this arrow points. */ 
	transient private SynthStage nextStage;
	/** 0-Based index of this object in the MDoc. */
	private int		objectNumber;
	
	//----------------------------------------------------------------------
	//							constructors
	//----------------------------------------------------------------------	
	/** Constructor. */
	SynthArrow() {
		// intentionally empty
	} // SynthArrow()

	/** Constructor. 
	 * @param	newArrow	the arrow from the MDoc
	 * @param	objNum	0-based index of this object in the MDoc
	 */
	SynthArrow(MPolyline newArrow, int objNum) { 
		arrow = newArrow;
		objectNumber = objNum;
	} // SynthArrow(MPolyline, int)

	//----------------------------------------------------------------------
	//							setStagesPrevNext
	//----------------------------------------------------------------------	
	/** Sets the previous and next stages connected by this arrow.
	 * @param	prev	the previous stage
	 * @param	next	the next stage
	 */
	public void setStagesPrevNext(SynthStage prev, SynthStage next) { 
		prevStage = prev;
		nextStage = next;
	} // setStagesPrevNext(SynthStage, SynthStage)
	
	//----------------------------------------------------------------------
	//							short get-set methods
	//----------------------------------------------------------------------	
	/** Sets the object number of this straight arrow in the MDoc. 
	 * @param	objNum	0-based number of this arrow in the MDoc
	 */
	public void setObjectNumber(int objNum)		{ objectNumber = objNum; } 
	/** Sets the arrow object from the MDoc.
	 * @param	newArrow	arrow from the MDoc
	 */
	public void setArrow(MPolyline newArrow)	{ arrow = newArrow; }
	/** Gets the object number of this arrow in the MDoc. 
	 * @return	0-based number of this arrow in the MDoc
	 */
	public int getObjectNumber()				{ return objectNumber; }
	/** Gets the arrow object from the MDoc.
	 * @return	arrow from the MDoc
	 */
	public MPolyline getArrow()					{ return arrow; }
	/** Gets either end of the arrow or the midpoint.
	 * @param	kind	which point to get
	 * @return	a point of the arrow
	 */
	public MPoint getPoint(int kind)			{ return arrow.getPoint(kind); } 
	/** Gets the stage at which this arrow originates.
	 * @return	the previous stage
	 */
	public SynthStage getPrevStage()			{ return prevStage; }
	/** Gets the stage to which this straight arrow points.
	 * @return	the next stage
	 */
	public SynthStage getNextStage()			{ return nextStage; }
	/** Gets whether there is a wedge at both ends of a straight arrow. 
	 * Used to determine whether the arrow is a resonance arrow.
	 * @param	end	head or tail (MPolyline constant)
	 * @return	true if there is a wedge
	 */
	public boolean hasWedgeAt(int end) 			{ return (arrow.getArrowLength(end) != 0); }

} // SynthArrow

