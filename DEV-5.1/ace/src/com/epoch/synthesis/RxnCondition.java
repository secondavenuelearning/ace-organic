package com.epoch.synthesis;

import com.epoch.db.SynthDataRW;
import com.epoch.synthesis.synthConstants.SynthConstants;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Defines a reaction condition used by a Synthesis.
	Scope: UI, Session, Data
	Sessions: QSet, HWSession
*/
public class RxnCondition implements SynthConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Unique ID of this reaction condition. */
	transient public int rxnId = 0;
	/** Name of this reaction condition. */
	transient public String name;
	/** MRV definition of this reaction condition. */
	transient public String reactionDef;
	/** Classification of this reaction condition. */
	transient public String classifn;
	/** Whether this reaction condition takes three reactants. */
	transient public boolean threeComponent = false;

	/** Stores the molecules that should only be chosen from the reagents
	 * menu. */
	// private static Molecule[] menuRgts = null; // unused as of 11/6/2012

	/** Constructor. */
	public RxnCondition() {
		// intentionally empty
	} // RxnCondition()

	/** Constructor.
	 * @param	id	ID number of the reaction condition
	 * @param	rxnName	name of the reaction condition
	 * @param	def	MRV definition of the reaction condition
	 * @param	rxnClassifn	classification of the reaction condition
	 * @param	threeCompon	whether the reaction condition takes three reactants
	*/
	public RxnCondition(int id, String rxnName, String def, String rxnClassifn,
			boolean threeCompon) {
		rxnId = id;
		name = rxnName;
		reactionDef = def;
		classifn = rxnClassifn;
		threeComponent = threeCompon;
	} // RxnCondition(int, String, String, String, boolean)

	/** Constructor.
	 * @param	id	ID number of the reaction condition
	 * @param	rxnName	name of the reaction condition
	 * @param	def	MRV definition of the reaction condition
	 * @param	rxnClassifn	classification of the reaction condition
	*/
	public RxnCondition(int id, String rxnName, String def, String rxnClassifn) {
		rxnId = id;
		name = rxnName;
		reactionDef = def;
		classifn = rxnClassifn;
	} // RxnCondition(int, String, String, String)

	/** Constructor.
	 * @param	rxnName	name of the reaction condition
	 * @param	def	MRV definition of the reaction condition
	 * @param	rxnClassifn	classification of the reaction condition
	 * @param	threeCompon	whether the reaction condition takes three reactants
	*/
	public RxnCondition(String rxnName, String def, String rxnClassifn,
			boolean threeCompon) {
		name = rxnName;
		reactionDef = def;
		classifn = rxnClassifn;
		threeComponent = threeCompon;
	} // RxnCondition(String, String, String, boolean)

	/** Constructor.
	 * @param	rxnName	name of the reaction condition
	 * @param	def	MRV definition of the reaction condition
	 * @param	rxnClassifn	classification of the reaction condition
	*/
	public RxnCondition(String rxnName, String def, String rxnClassifn) {
		name = rxnName;
		reactionDef = def;
		classifn = rxnClassifn;
	} // RxnCondition(String, String, String)

	/** Copy constructor. 
	 * @param	copy	the copy
	*/
	public RxnCondition(RxnCondition copy) {
		rxnId = copy.rxnId;
		name = copy.name;
		reactionDef = copy.reactionDef;
		classifn = copy.classifn;
		threeComponent = copy.threeComponent;
	}

	/** Saves a reaction condition to the database.  */
	public void setRxnCondition() {
		try {
			SynthDataRW.setRxnCondition(this);
		} catch (Exception e) {
			Utils.alwaysPrint("RxnCondition: exception "
					+ "thrown when writing reaction condition to db:\n",
					e.getMessage());
		}
	} // setRxnCondition()

	/** Gets a reaction condition from the database.
	 * @param	rxnNum	ID number of the reaction condition to acquire
	 * @return	the requested reaction condition
	 */
	public static RxnCondition getRxnCondition(int rxnNum) {
		RxnCondition rxnCondn = new RxnCondition();
		try {
			rxnCondn = SynthDataRW.getRxnCondition(rxnNum);
		} catch (Exception e) {
			Utils.alwaysPrint("RxnCondition.getRxnCondition: "
					+ "Exception: ", e.getMessage());
		}
		return rxnCondn;
	} // getRxnCondition(int)

	/** Gets names of all reaction conditions from the database.  If there are
	 * none, returns a dummy value.
	 * @return	array of names of all reaction conditions
	 */
	public static String[] getAllReactionNames() {
		String[] reactions = null;
		try {
			reactions = SynthDataRW.getAllReactionsData(NAMES);
		} catch (Exception e) {
			Utils.alwaysPrint("RxnCondition.getAllReactionNames: "
					+ "Exception: ", e.getMessage());
		}
		return reactions;
	} // getAllReactionNames()

	/** Gets IDs of all reaction conditions from the database in the same order
	 * as the names.  If there are none, returns a dummy value.
	 * @return	array of IDs of all reaction conditions
	 */
	public static int[] getAllReactionIds() {
		int[] rxnIds = null;
		try {
			rxnIds = SynthDataRW.getAllReactionIds();
		} catch (Exception e) {
			Utils.alwaysPrint("RxnCondition.getAllReactionIds: "
					+ "Exception: ", e.getMessage());
		}
		return rxnIds;
	} // getAllReactionIds()

	/** Gets IDs of all reaction conditions from the database in the order
	 * of alphabetized names.  If there are none, returns a dummy value.
	 * @param	reactionNamesByIds	hashtable of reaction condition names keyed
	 * by their IDs
	 * @return	array of IDs of all reaction conditions in alphabetical order of
	 * their corresponding names
	 */
	public static int[] getAllReactionIdsAlphabetized(
			Map<Integer, String> reactionNamesByIds) {
		final int[] reactionIds = getAllReactionIds();
		String[] reactions;
		if (reactionIds.length == 0) {
			Utils.alwaysPrint("getAllReactionIdsAlphabetized: " +
				"no reactionIds");
			reactions = new String[0];
		} else {
			reactions = new String[reactionIds.length - 1];
			// ignore Jlint complaint about line above.  Raphael 11/2010
		}
		for (int rxnNum = 0; rxnNum < reactionIds.length - 1; rxnNum++) {
			final int rxnId = reactionIds[rxnNum + 1];
			reactions[rxnNum] =
					reactionNamesByIds.get(rxnId).toLowerCase(Locale.ENGLISH)
					+ DIVIDER + reactionIds[rxnNum + 1];
		} // for each reaction
		Arrays.sort(reactions);
		int[] alphabetizedReactionIds = new int[reactionIds.length];
		alphabetizedReactionIds[0] = reactionIds[0];
		for (int rxnNum = 0; rxnNum < reactionIds.length - 1; rxnNum++) {
			final String numberStr = reactions[rxnNum].substring(
					reactions[rxnNum].indexOf(DIVIDER) + DIVIDER.length());
			alphabetizedReactionIds[rxnNum + 1] = Integer.parseInt(numberStr);
		}
		return alphabetizedReactionIds;
	} // getAllReactionIdsAlphabetized(Map<Integer, String>)
	
	/** Gets all classifications of reaction conditions from the database.
	 * @param	unique	whether to return just one instance of each
	 * classification, or to return an array of classifications that is parallel
	 * to the names of reaction conditions
	 * @return	array of classifications of reaction conditions
	 */
	public static String[] getAllClassifns(boolean unique) {
		String[] allClassifns = null;
		try {
			allClassifns = SynthDataRW.getAllReactionsData(
					unique ? CLASSIFNS_UNIQUE : CLASSIFNS);
		} catch (Exception e) {
			Utils.alwaysPrint("RxnCondition.getAllClassifns: "
					+ "Exception: ", e.getMessage());
		}
		if (unique) {
			// some classifications could be comma-separated lists of others
			final ArrayList<String> allClassifnsList = new ArrayList<String>();
			for (final String classifnStr : allClassifns) {
				final String[] classifns = classifnStr.split(",");
				for (final String classifn : classifns) {
					if (!allClassifnsList.contains(classifn))
						allClassifnsList.add(classifn);
				} // for each classification in the list
			} // for each reaction's calssifications
			allClassifns =
				allClassifnsList.toArray(new String[allClassifnsList.size()]);
		} // if we want only unique classifications
		return allClassifns;
	} // getAllClassifns()

	/** Gets names of all reaction conditions keyed by their IDs.
	 * @return	hashtable of names of all reaction conditions keyed by their IDs
	 */
	public static Map<Integer, String> getRxnNamesKeyedByIds() {
		Map<Integer, String> namesByIds =
				new HashMap<Integer, String>();
		try {
			namesByIds = SynthDataRW.getAllReactionNamesKeyedById();
		} catch (Exception e) {
			Utils.alwaysPrint("RxnCondition.getAllReactionNamesKeyedById: "
					+ "Exception: ", e.getMessage());
		}
		return namesByIds;
	} // getRxnNamesKeyedByIds()

	/** Gets MRV definitions of all reaction conditions keyed by their IDs.
	 * @return	hashtable of MRV definitions of all reaction conditions
	 * keyed by their IDs
	 */
	public static Map<Integer, String> getRxnDefsKeyedByIds() {
		Map<Integer, String> defsByIds =
				new HashMap<Integer, String>();
		try {
			defsByIds = SynthDataRW.getAllReactionDefsKeyedById();
		} catch (Exception e) {
			Utils.alwaysPrint("RxnCondition.getAllReactionDefsKeyedById: "
					+ "Exception: ", e.getMessage());
		}
		return defsByIds;
	} // getRxnDefsKeyedByIds()

	/** Rearranges a colon-separated String of IDs of reaction conditions into
	 * alphabetical order of their corresponding names.
	 * @param	allowedRxnsStr	colon-separated String of IDs of reaction
	 * conditions in numerical order
	 * @param	reactionNamesByIds	hashtable of reaction condition names keyed
	 * by their IDs
	 * @return	colon-separated String of IDs of reaction conditions in
	 * alphabetical order of their names; the default, "no reagents", comes
	 * first
	 */
	public static String alphabetize(String allowedRxnsStr,
			Map<Integer, String> reactionNamesByIds) {
		return alphabetize(allowedRxnsStr, reactionNamesByIds,
				DEFAULT_NO_RGTS);
	} // alphabetize(String, Map<Integer, String>)
	
	/** Rearranges a colon-separated String of IDs of reaction conditions into
	 * alphabetical order of their corresponding names.
	 * @param	allowedRxnsStr	colon-separated String of IDs of reaction
	 * conditions in numerical order
	 * @param	reactionNamesByIds	hashtable of reaction condition names keyed
	 * by their IDs
	 * @param	skip1st	whether the first ID in allowedRxnsStr is for the
	 * default, "no reagents", which should always come first
	 * @return	colon-separated String of IDs of reaction conditions in
	 * alphabetical order of their names; the default, "no reagents", comes
	 * first
	 */
	public static String alphabetize(String allowedRxnsStr,
			Map<Integer, String> reactionNamesByIds, boolean skip1st) {
		final int start = (skip1st ? 1 : 0);
		final String[] allowedRxnNums = allowedRxnsStr.split(":");
		final int numRxns = allowedRxnNums.length;
		String[] allowedRxnNames;
		if (numRxns - start < 0) {
			Utils.alwaysPrint("alphabetize: too few reactions");
			allowedRxnNames = new String[0];
		} else {
			allowedRxnNames = new String[numRxns - start];
			// ignore Jlint complaint about line above.  Raphael 11/2010
		}
		for (int rxnNum = start; rxnNum < numRxns; rxnNum++) {
			final int rxnId = Integer.parseInt(allowedRxnNums[rxnNum]);
			allowedRxnNames[rxnNum - start] =
					reactionNamesByIds.get(rxnId).toLowerCase(Locale.ENGLISH)
					+ DIVIDER + allowedRxnNums[rxnNum];
		} // for each reaction
		Arrays.sort(allowedRxnNames);
		final StringBuilder newAllowedRxnsBld = new StringBuilder();
		if (skip1st) {
			Utils.appendTo(newAllowedRxnsBld, NO_REAGENTS, ':');
		}
		if (numRxns > start) {
			boolean first = true;
			for (final String allowedRxnName : allowedRxnNames) {
				if (first) first = false;
				else newAllowedRxnsBld.append(':');
				final String[] pieces = allowedRxnName.split(DIVIDER);
				newAllowedRxnsBld.append(pieces[1]);
			} // for each reaction
		}
		return newAllowedRxnsBld.toString();
	} // alphabetize(String, Map<Integer, String>, boolean)

} // RxnCondition
