package com.epoch.mechanisms;

import chemaxon.marvin.io.MolExportException;
import chemaxon.struc.MDocument;
import com.epoch.chem.MolString;
import com.epoch.mechanisms.mechConstants.MechConstants;
import java.awt.Color;
import java.util.List;

/** Methods for coloring mechanism components.  */
public class MechUtils implements MechConstants {

	//----------------------------------------------------------------------
	//						 members
	//----------------------------------------------------------------------
	/** Data on the mechanism. */
	transient private MechParser parsedMech = null;
	
	//----------------------------------------------------------------------
	//						 constructors
	//----------------------------------------------------------------------
	/** Constructor. */
	public MechUtils() {
		// intentionally empty
	}

	/** Constructor. 
	 * @param	mech	a mechanism
	 */
	public MechUtils(Mechanism mech) {
		parsedMech = mech.parsedMech;
	}
	
	//----------------------------------------------------------------------
	//						getXmlWithColoredArrow
	//----------------------------------------------------------------------
	/** Colors the box of this stage, the arrow pointing to the next stage, and
	 * the next stage.
	 * @param	stage	stage whose arrow pointing to the next stage will be
	 * colored
	 * @return	MRV representation of the mechanism with the arrow and boxes 
	 * colored
	 */
	public String getXmlWithColoredArrow(MechStage stage) {
		final String SELF = "MechUtils.getXmlWithColoredArrow: ";
		if (stage == null) {
			System.out.println(SELF + "no stage found.");
			return parsedMech.getMRV();
		} // if no stage
		final MechArrow arrow = stage.getArrowToNext();
		final int arrowObjIndex = arrow.getObjectNumber();
		// need to color stages as well as long as MarvinJS can't color arrows
		final int stageBoxIndex = stage.getBoxIndex();
		final int nextStageBoxIndex = arrow.getNextStage().getBoxIndex();
		final int[] objIndices = 
				new int[] {arrowObjIndex, stageBoxIndex, nextStageBoxIndex};
		final MDocument newMDoc = parsedMech.getMDocCopy();
		MolString.colorMObjects(newMDoc, objIndices, Color.RED);
		try {
			return MolString.toString(newMDoc, MRV);
		} catch (MolExportException e) {
			System.out.println(SELF + "caught MolExportException; "
					+ "returning original XML");
		}
		return parsedMech.getMRV();
	} // getXmlWithColoredArrow(MechStage)

	//----------------------------------------------------------------------
	//							colorStages, colorStage
	//----------------------------------------------------------------------
	/** Colors a set of stage boxes.
	 * @param	stagesToColor	list of indices of stages to color
	 * @return	MRV representation of the mechanism with boxes colored
	 */
	public String colorStages(List<Integer> stagesToColor) {
		final String SELF = "MechUtils.colorStages: ";
		try {
			final MDocument newMDoc = parsedMech.getMDocCopy();
			for (final Integer stageNum : stagesToColor) {
				final MechStage stage = parsedMech.getStage(stageNum.intValue());
				final int boxToColor = stage.getBoxIndex();
				MolString.colorMObject(newMDoc, boxToColor, Color.RED);
			} // for each stage to color
			return MolString.toString(newMDoc, MRV);
		} catch (IndexOutOfBoundsException e) {
			System.out.println(SELF + "IndexOutOfBoundsException; "
					+ " couldn't get a stage to color.");
		} catch (MolExportException e) {
			System.out.println(SELF + "MolExportException; "
					+ "cannot export modified document.");
		}
		return parsedMech.getMRV();
	} // colorStages(List<Integer>)

	/** Colors a stage's box.
	 * @param	stageToColor	index of stage to color
	 * @return	MRV representation of the mechanism with box colored
	 */
	public String colorStage(int stageToColor) {
		final String SELF = "MechUtils.colorStage: ";
		try {
			final MDocument newMDoc = parsedMech.getMDocCopy();
			final MechStage stage = parsedMech.getStage(stageToColor);
			final int boxToColor = stage.getBoxIndex();
			MolString.colorMObject(newMDoc, boxToColor, Color.RED);
			return MolString.toString(newMDoc, MRV);
		} catch (IndexOutOfBoundsException e) {
			System.out.println(SELF + "IndexOutOfBoundsException; "
					+ " couldn't get a stage to color.");
		} catch (MolExportException e) {
			System.out.println(SELF + "MolExportException; "
					+ "cannot export modified document.");
		}
		return parsedMech.getMRV();
	} // colorStage(int)
	
} // MechUtils
