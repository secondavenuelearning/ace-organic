package com.epoch.synthesis.synthConstants;

import com.epoch.constants.FormatConstants;

/** Holds constants for setting the stereochemistry of a Diels-Alder reaction. */
public class DielsAlderConstants implements FormatConstants {

	/** Number of atom maps. */
	public static final int NUM_MAPS = 14;
	/** Range of atom maps. */
	public static final int[] MAPS_RANGE = new int[] {1, NUM_MAPS};
	/** Number of ring atoms. */
	public static final int NUM_RING_ATOMS = 6;
	/** Range of ring atom maps. */
	public static final int[] RING_MAPS_RANGE = new int[] {1, NUM_RING_ATOMS};
	/** Map numbers of reactive diene and dienophile termini. */
	public static final int[] TERMINUS_MAPS = new int[] {1, 4, 5, 6};
	/** Map numbers of reactive diene and dienophile termini. */
	public static final int[] DIENE_MAPS = new int[] {1, 2, 3, 4};
	/** Position of first diene terminus in TERMINUS_MAPS. */
	public static final int DIENE_TERMINUS1_POSN = 0;
	/** Position of first dienophile terminus in TERMINUS_MAPS. */
	public static final int PHILE_TERMINUS1_POSN = 2;
	/** Positions of atoms making bonds to one another in TERMINUS_MAPS. */
	public static final int[][] MADE_BOND_ATOM_MAPS = new int[][] {{0, 3}, {1, 2}};
	/** Map numbers of out groups on diene termini or 1st ligands on dienophile 
	 * termini, with positions corresponding to termini's positions in
	 * TERMINUS_MAPS. */
	public static final int[] TERMINUS_LIGANDS = new int[] {7, 9, 11, 13};
	/** Array of the map numbers of the two atoms of each bond from a reacting
	 * dienophile atom to a ligand. */
	public static final int[][] MAPS_PHILE_BONDS = new int[][] 
			{ {5, 11}, {5, 12}, {6, 13}, {6, 14} };
	/** First member of each array in MAPS_PHILE_BONDS, corresponding to
	 * the map number of the dienophile atom. */
	public static final int PHILE_ATOM = 0;
	/** Second member of each array in MAPS_PHILE_BONDS, corresponding to
	 * the map number of the dienophile's ligand. */
	public static final int LIGAND_ATOM = 1;
	/** Unused map number, reserved for in atoms.  An in atom on a diene 
	 * terminus may be assigned to this position in the atoms-by-maps array. */
	public static final int IN_ATOM_MAP = 8;

/* Typical reaction definition for a Diels-Alder reaction.  If the reaction is
 * exo, add a molecule property for "major or minor product".
 
<?xml version="1.0" ?>
<cml>
<MDocument>
	<MChemicalStruct>
		<reaction>
			<arrow type="DEFAULT" x1="-2.3228183382137724" y1="0.6173163748577306" x2="0.5412106157937222" y2="0.56454533723183" />
			<propertyList>
				<property dictRef="NAME" title="NAME">
					<scalar><![CDATA[Diels-Alder (endo)]]></scalar>
				</property>
				<property dictRef="EXPLAIN_SELECTIVITY" title="EXPLAIN_SELECTIVITY">
					<scalar><![CDATA[The most positively charged dienophile atom becomes 6, and the most negatively charged of diene atoms becomes 1.]]></scalar>
				</property>
				<property dictRef="SELECTIVITY" title="SELECTIVITY">
					<scalar><![CDATA[-charge(ratom(1), "pi"); charge(ratom(6)) ]]></scalar>
				</property>
				<property dictRef="Number of reactants" title="Number of reactants">
					<scalar>2</scalar>
				</property>
				<property dictRef="Stop after one reaction" title="Stop after one reaction">
					<scalar>true</scalar>
				</property>
			</propertyList>
			<reactantList>
				<molecule molID="m1">
					<atomArray
						atomID="a1 a2 a3 a4 a5 a6"
						elementType="C C C C C C"
						mrvMap="1 2 3 4 7 9"
						mrvQueryProps="0 0 0 0 0 0"
						mrvPseudo="0 0 0 0 AH AH"
						x2="-9.624999523162842 -10.9586688642111 -10.9586688642111 -9.624999523162842 -9.624999523162842 -9.624999523162842"
						y2="2.1656588656208733 1.3956419250618328 -0.1443919560562481 -0.9144088966152886 3.7056588656208733 -2.4544088966152886"
						/>
					<bondArray>
						<bond atomRefs2="a1 a2" order="2" />
						<bond atomRefs2="a2 a3" order="1" />
						<bond atomRefs2="a3 a4" order="2" />
						<bond atomRefs2="a1 a5" order="1" />
						<bond atomRefs2="a4 a6" order="1" />
					</bondArray>
				</molecule>
				<molecule molID="m2">
					<atomArray
						atomID="a1 a2 a3 a4 a5 a6"
						elementType="C C C C C C"
						mrvMap="5 6 13 14 11 12"
						mrvQueryProps="0 0 0 0 0 0"
						mrvPseudo="0 0 AH AH AH AH"
						x2="-4.826330387154561 -4.826330387154561 -6.160009508982597 -3.4926512653265256 -6.160009508982597 -3.4926512653265256"
						y2="-0.1925169846664776 1.3475168964516033 2.117516896451604 2.1175168964516033 -0.9625169846664782 -0.9625169846664775"
						/>
					<bondArray>
						<bond atomRefs2="a1 a2" order="2" />
						<bond atomRefs2="a2 a3" order="1" />
						<bond atomRefs2="a2 a4" order="1" />
						<bond atomRefs2="a1 a5" order="1" />
						<bond atomRefs2="a1 a6" order="1" />
					</bondArray>
				</molecule>
			</reactantList>
			<productList>
				<molecule molID="m3">
					<atomArray
						atomID="a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12"
						elementType="C C C C C C C C C C C C"
						mrvMap="1 2 3 4 5 6 7 13 14 12 11 9"
						mrvQueryProps="0 0 0 0 0 0 0 0 0 0 0 0"
						mrvPseudo="0 0 0 0 0 0 AH AH AH AH AH AH"
						x2="4.956875165303548 3.6232058242552894 3.6232058242552894 4.956875165303548 6.290544983188965 6.290544983188965 4.186875165303548 7.060544983188965 7.830544983188965 7.830544983188965 7.060544983188965 4.186875165303548"
						y2="2.053367241472186 1.2833503009131455 -0.25668358020493454 -1.0267005207639759 -0.25668358020493454 1.2833503009131455 3.3870463633002217 2.617029422741181 1.2833503009131455 -0.25668358020493454 -1.59036270203297 -2.3603796425920116"
						/>
					<bondArray>
						<bond atomRefs2="a1 a2" order="1" />
						<bond atomRefs2="a6 a1" order="1" />
						<bond atomRefs2="a2 a3" order="2" />
						<bond atomRefs2="a4 a3" order="1" />
						<bond atomRefs2="a5 a4" order="1" />
						<bond atomRefs2="a5 a6" order="1" />
						<bond atomRefs2="a1 a7" order="1" />
						<bond atomRefs2="a6 a8" order="1" />
						<bond atomRefs2="a6 a9" order="1" />
						<bond atomRefs2="a5 a10" order="1" />
						<bond atomRefs2="a5 a11" order="1" />
						<bond atomRefs2="a4 a12" order="1" />
					</bondArray>
				</molecule>
			</productList>
		</reaction>
	</MChemicalStruct>
</MDocument>
</cml>
*/

} // DielsAlderConstants
