package com.epoch.evals.impl.chemEvals.energyEvals;

import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.List;

/** Utility class for <code>EDiagramDiff</code>.  Contains two parallel Lists; 
 * the first contains the joined string representations of pairs of 
 * orbitals or states in a diagram, and the second contains the distances 
 * between the pairs. */
class PairDistances {

	/** Stores joined string representations of pairs of orbitals or 
	 * states in a diagram. */
	transient List<String> pairs;
	/** Stores distances between pairs of orbitals or states, parallel to
	 * <code>pairs</code>. */
	transient List<Integer> distances;

	/** Constructor.  */
	PairDistances() {
		pairs = new ArrayList<String>();
		distances = new ArrayList<Integer>();
	} // PairDistances()

	/** Constructor.
	 * @param	prs	joined string representations of pairs of 
	 * orbitals or states in a diagram
	 * @param	dists	distances between pairs of orbitals or states, 
	 * parallel to <code>pairs</code>
	 */
	PairDistances(List<String> prs, List<Integer> dists) {
		pairs = prs;
		distances = dists;
	} // PairDistances(List<String>, List<Integer>)

	/** Makes a string representation of this class.
	 * @return a string representation of the members of this class.
	 */
	public String toString() {
		return Utils.toString("Orbital or state pairs: ", pairs.toString(),
				"\nDistances between each pair: ", distances.toString());
	} // toString()

} // PairDistances
