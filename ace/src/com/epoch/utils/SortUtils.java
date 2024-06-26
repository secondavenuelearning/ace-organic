package com.epoch.utils;

import com.epoch.constants.AppConstants;
import com.epoch.exceptions.ParameterException;
import java.text.RuleBasedCollator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Methods for sorting objects that are not comparable into an order given by
 * one or more String or Integer keys that are themselves comparable. */
public final class SortUtils implements AppConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Orders an array of nonunique objects in the order given by keys
	 * (Comparables) that may also not be unique.  Modifies both original
	 * arrays by sorting them both, the keys according to their natural order
	 * and the objects according to the order of the keys.
	 * @param	toSort	list of objects to sort
	 * @param	keys	parallel list of keys to use to sort the objects
	 * @throws ParameterException	if the lists don't have the same length
	 */
	public static void sort(Object[] toSort, Comparable<?>[] keys) 
			throws ParameterException {
		sort(toSort, keys, ENGLISH);
	} // sort(Object[], Comparable<?>[])

	/** Orders an array of nonunique objects in the order given by keys
	 * (Comparables) that may also not be unique.  Modifies both original
	 * arrays by sorting them both, the keys according to their natural order
	 * and the objects according to the order of the keys.
	 * @param	toSort	list of objects to sort
	 * @param	keys	parallel list of keys to use to sort the objects
	 * @param	language	language to use for alphabetization
	 * @throws ParameterException	if the lists don't have the same length
	 */
	public static void sort(Object[] toSort, Comparable<?>[] keys, String language) 
			throws ParameterException {
		final String SELF = "SortUtils.sort: ";
		final List<Comparable<?>> keysList = 
				new ArrayList<Comparable<?>>(Arrays.asList(keys));
		final List<Object> toSortList = 
				new ArrayList<Object>(Arrays.asList(toSort));
		sort(toSortList, keysList, language);
		for (int num = 0; num < toSort.length; num++) {
			keys[num] = keysList.get(num);
			toSort[num] = toSortList.get(num);
		} // for each key and object
	} // sort(Object[], Comparable<?>[], String)

	/** Orders a list of nonunique objects in the order given by keys
	 * (Comparables) that may also not be unique.  Modifies both original
	 * lists by sorting them both, the keys according to their natural order
	 * and the objects according to the order of the keys.
	 * @param	<T>	type of Object being sorted
	 * @param	toSort	list of objects to sort
	 * @param	keys	parallel list of keys to use to sort the objects
	 * @throws ParameterException	if the lists don't have the same length
	 */
	public static <T> void sort(List<T> toSort, List<Comparable<?>> keys) 
			throws ParameterException {
		sort(toSort, keys, ENGLISH);
	} // sort(List<T>, List<Comparable<?>>)

	/** Orders a list of nonunique objects in the order given by keys
	 * (Comparables) that may also not be unique.  Modifies both original
	 * lists by sorting them both, the keys according to their natural order
	 * and the objects according to the order of the keys.
	 * @param	<T>	type of Object being sorted
	 * @param	toSort	list of objects to sort
	 * @param	keys	parallel list of keys to use to sort the objects
	 * @param	language	language to use for alphabetization
	 * @throws ParameterException	if the lists don't have the same length
	 */
	public static <T> void sort(List<T> toSort, List<Comparable<?>> keys, 
			String language) throws ParameterException {
		final String SELF = "SortUtils.sort: ";
		final int numObjs = toSort.size();
		if (numObjs != keys.size()) throw new ParameterException(SELF
				+ "lists of objects and keys must be same length");
		checkNulls(keys);
		// make map of keys hashed by original (unique) position
		final Map<Integer, Comparable<?>> map = 
				new HashMap<Integer, Comparable<?>>();
		for (int keyNum = 0; keyNum < numObjs; keyNum++) {
			map.put(Integer.valueOf(keyNum), keys.get(keyNum));
		} // for each key
		sort(keys, getCollator(language));
		final List<Comparable<?>> keysCopy = new ArrayList<Comparable<?>>(keys);
		final List<T> sorted = new ArrayList<T>();
		for (final T dummy : toSort) sorted.add(null);
		// get original position of each key from map, new position from sorted
		// list, and place corresponding object into new position in new list
		final List<Integer> oldPosns = new ArrayList<Integer>(map.keySet());
		for (final Integer oldPosn : oldPosns) {
			final Comparable<?> key = map.get(oldPosn);
			final int newPosn = keys.indexOf(key);
			keys.set(newPosn, null); // in case of duplicates
			final T obj = toSort.get(oldPosn);
			sorted.set(newPosn, obj);
		} // for each item
		keys.clear();
		keys.addAll(keysCopy);
		toSort.clear();
		toSort.addAll(sorted);
	} // sort(List<T>, List<Comparable<?>>, String)

	/** Orders an array of nonunique objects in the order given by an array of
	 * two or more keys (Comparables) that may also not be unique.  Modifies 
	 * both original arrays.
	 * @param	toSort	list of objects to sort
	 * @param	keys	parallel list of arrays of keys to use to sort 
	 * the objects
	 * @throws ParameterException	if the object and key arrays don't have 
	 * the same length or some keys don't have the same length
	 */
	public static void sort(Object[] toSort, Comparable<?>[][] keys) 
			throws ParameterException {
		sort(toSort, keys, ENGLISH);
	} // sort(Object[], Comparable<?>[][])

	/** Orders an array of nonunique objects in the order given by an array of
	 * two or more keys (Comparables) that may also not be unique.  Modifies 
	 * both original arrays.
	 * @param	toSort	list of objects to sort
	 * @param	keys	parallel list of arrays of keys to use to sort 
	 * the objects
	 * @param	language	language to use for alphabetization
	 * @throws ParameterException	if the object and key arrays don't have 
	 * the same length or some keys don't have the same length
	 */
	public static void sort(Object[] toSort, Comparable<?>[][] keys, 
			String language) throws ParameterException {
		final String SELF = "SortUtils.sort: ";
		final List<Comparable<?>[]> keysList = 
				new ArrayList<Comparable<?>[]>(Arrays.asList(keys));
		final List<Object> toSortList = 
				new ArrayList<Object>(Arrays.asList(toSort));
		sortByArrays(toSortList, keysList, language);
		for (int num = 0; num < toSort.length; num++) {
			keys[num] = keysList.get(num);
			toSort[num] = toSortList.get(num);
		} // for each key and object
	} // sort(Object[], Comparable<?>[][], String)

	/** Orders a list of nonunique objects in the order given by arrays of 
	 * two or more keys
	 * (Comparables) that may also not be unique.  Modifies both original
	 * lists, preserving the original key arrays.
	 * @param	<T>	type of Object being sorted
	 * @param	toSort	list of objects to sort
	 * @param	keys	parallel list of keys to use to sort the objects
	 * @throws ParameterException	if the lists don't have the same length
	 * or some key arrays don't have the same length
	 */
	public static <T> void sortByArrays(List<T> toSort, 
			List<Comparable<?>[]> keys) throws ParameterException {
		sortByArrays(toSort, keys, ENGLISH);
	} // sortByArrays(List<T>, List<Comparable<?>[]>)

	/** Orders a list of nonunique objects in the order given by arrays of 
	 * two or more keys
	 * (Comparables) that may also not be unique.  Modifies both original
	 * lists, preserving the original key arrays.
	 * @param	<T>	type of Object being sorted
	 * @param	toSort	list of objects to sort
	 * @param	keys	parallel list of keys to use to sort the objects
	 * @param	language	language to use for alphabetization
	 * @throws ParameterException	if the lists don't have the same length
	 * or some key arrays don't have the same length
	 */
	public static <T> void sortByArrays(List<T> toSort, 
			List<Comparable<?>[]> keys, String language) 
			throws ParameterException {
		final String SELF = "SortUtils.sortByArrays: ";
		final int numObjs = toSort.size();
		if (numObjs != keys.size()) throw new ParameterException(SELF
				+ "lists of objects and keys must be same length");
		checkLengthsAndNulls(keys);
		// make map of keys hashed by original (unique) position
		final Map<Integer, ArrayList<Comparable<?>>> map =
				new HashMap<Integer, ArrayList<Comparable<?>>>();
		final ArrayList<ArrayList<Comparable<?>>> keysLists = 
				new ArrayList<ArrayList<Comparable<?>>>();
		for (int keyNum = 0; keyNum < numObjs; keyNum++) {
			final Comparable<?>[] keyArr = keys.get(keyNum);
			final ArrayList<Comparable<?>> keyAsList = 
					new ArrayList<Comparable<?>>(Arrays.asList(keyArr));
			keysLists.add(keyAsList);
			map.put(Integer.valueOf(keyNum), keyAsList);
		} // for each key
		sortLists(keysLists, getCollator(language));
		final List<T> sortedObjs = new ArrayList<T>();
		final List<Comparable<?>[]> sortedKeys = new ArrayList<Comparable<?>[]>();
		for (final T dummy : toSort) {
			sortedObjs.add(null);
			sortedKeys.add(null);
		} // for each key/object position
		// get original position of each key from map, new position from sorted
		// list, and place corresponding key, object into new position in new lists
		final List<Integer> oldPosns = new ArrayList<Integer>(map.keySet());
		for (final Integer oldPosn : oldPosns) {
			final ArrayList<Comparable<?>> keyAsList = map.get(oldPosn);
			final int newPosn = keysLists.indexOf(keyAsList);
			keysLists.set(newPosn, null); // in case of duplicates
			sortedObjs.set(newPosn, toSort.get(oldPosn));
			sortedKeys.set(newPosn, keys.get(oldPosn));
		} // for each item
		toSort.clear();
		toSort.addAll(sortedObjs);
		keys.clear();
		keys.addAll(sortedKeys);
	} // sortByArrays(List<T>, List<Comparable<?>[]>, String)

	/** Sorts the list of lists of values.  Each list is sorted by each element
	 * in turn.  If each list has three or more members, and at least two lists
	 * have a common first member, this method will be called recursively.
	 * @param	lists	list of list of comparable values
	 * @param	localized	a collator for a location given by the sorting
	 * language
	 */
	private static void sortLists(List<ArrayList<Comparable<?>>> lists, 
			RuleBasedCollator localized) {
		final String SELF = "SortUtils.sortStringLists: ";
		// make list of unique first values of each list
		final List<Comparable<?>> uniqueFirsts = new ArrayList<Comparable<?>>();
		// make map of unique first values to list of all subsequent values
		final Map<Comparable<?>, List<ArrayList<Comparable<?>>>> subsequentsByFirst = 
				new HashMap<Comparable<?>, List<ArrayList<Comparable<?>>>>();
		for (final ArrayList<Comparable<?>> list : lists) {
			final Comparable<?> first = list.remove(0);
			if (!uniqueFirsts.contains(first)) uniqueFirsts.add(first);
			List<ArrayList<Comparable<?>>> subsequents = 
					subsequentsByFirst.get(first);
			if (subsequents == null) {
				subsequents = new ArrayList<ArrayList<Comparable<?>>>();
			}
			debugPrint(SELF, (subsequents.isEmpty() ? "new " : ""), "first "
					+ "value ", first, " accompanies subsequent values ", list);
			subsequents.add(list);
			subsequentsByFirst.put(first, subsequents);
		} // for each list to be sorted
		sort(uniqueFirsts, localized);
		debugPrint(SELF + "sorted unique first values are: ", uniqueFirsts);
		lists.clear();
		for (final Comparable<?> first : uniqueFirsts) {
			final List<ArrayList<Comparable<?>>> subsequents = 
					subsequentsByFirst.get(first);
			debugPrint(SELF + "unique value ", first, " associated with "
					+ "following subsequent values: ", subsequents);
			// sort the list of subsequent values (if there's more than one)
			if (subsequents.size() > 1) {
				if (subsequents.get(0).size() == 1) {
					sortOneMemberLists(subsequents, localized);
				} else {
					sortLists(subsequents, localized);
				} // if we are at last key to sort
			} // if there is more than one group of subsequent values to sort
			debugPrint(SELF + "sorted subsequent values: ", subsequents);
			// add back the first value to each member of list
			for (final ArrayList<Comparable<?>> subsequent : subsequents) {
				subsequent.add(0, first);
				debugPrint(SELF + "reconstituted complete list: ", subsequent);
			} // for each subsequent set of values
			// put sorted members back in original list
			lists.addAll(subsequents);
		} // for each unique first value
	} // sortLists(ArrayList<ArrayList<Comparable<?>>>, RuleBasedCollator)

	/** Sorts a list of one-member lists according to the values of their
	 * single members.  We can't change the values of the lists or make new 
	 * ones; we need to reorder all the original objects. 
	 * @param	oneElemLists	a list of one-member lists
	 * @param	localized	a collator for a location given by the sorting
	 * language
	 */
	private static void sortOneMemberLists(
			List<ArrayList<Comparable<?>>> oneElemLists, 
			RuleBasedCollator localized) {
		final ArrayList<Comparable<?>> sortList = new ArrayList<Comparable<?>>();
		for (final ArrayList<Comparable<?>> oneElemList : oneElemLists) {
			sortList.add(oneElemList.get(0));
		} // for each one-member list in oneElemLists
		sort(sortList, localized);
		// make a temporary list with occupied positions
		final ArrayList<ArrayList<Comparable<?>>> tempLists = 
				new ArrayList<ArrayList<Comparable<?>>>();
		for (int lNum = 0; lNum < oneElemLists.size(); lNum++) {
			tempLists.add(null);
		} // for each list needed
		// copy each one-element list into appropriate position in tempLists
		for (final ArrayList<Comparable<?>> oneElemList : oneElemLists) {
			final Comparable<?> elem = oneElemList.get(0);
			final int index = sortList.indexOf(elem);
			tempLists.set(index, oneElemList);
			// void element in sortList while preserving position
			sortList.set(index, null);
		} // for each one-member list in oneElemLists
		// put original one-element lists back into original list in correct order
		oneElemLists.clear();
		oneElemLists.addAll(tempLists);
	} // sortOneMemberLists(ArrayList<ArrayList<Comparable<?>>>, RuleBasedCollator)

	/** Sorts a list of Comparables that are Strings or Integers, modifying the
	 * original object.
	 * @param	list	a list of Comparables
	 * @param	localized	a collator for a location given by the sorting
	 * language
	 */
	private static void sort(List<Comparable<?>> list, 
			RuleBasedCollator localized) {
		final String SELF = "SortUtils.sort: ";
		if (list.isEmpty()) return;
		if (list.get(0) instanceof String) {
			final List<String> temp = new ArrayList<String>();
			for (final Comparable<?> key : list) temp.add((String) key);
			Collections.sort(temp, localized);
			list.clear();
			list.addAll(temp);
		} else if (list.get(0) instanceof Integer) {
			final List<Integer> temp = new ArrayList<Integer>();
			for (final Comparable<?> key : list) temp.add((Integer) key);
			Collections.sort(temp);
			list.clear();
			list.addAll(temp);
		} // if class is comparable
	} // sort(List<Comparable<?>>, RuleBasedCollator)

	/** Checks that every member of a list is not null.
	 * @param	list	a list
	 * @throws ParameterException	if a member is null
	 */
	private static void checkNulls(List<Comparable<?>> list)
			throws ParameterException {
		final String SELF = "SortUtils.checkNulls: ";
		for (final Comparable<?> item : list) {
			if (item == null) throw new ParameterException(SELF
					+ "all keys must have a value (not null)");
		} // for each item
	} // checkNulls(List<Comparable<?>>)

	/** Checks that every array in a list has the same length and that no
	 * member of any array is null.
	 * @param	list	a list of arrays
	 * @throws ParameterException	if some arrays don't have the same length
	 * or a member is null
	 */
	private static void checkLengthsAndNulls(List<Comparable<?>[]> list)
			throws ParameterException {
		final String SELF = "SortUtils.sortByArrays: ";
		final int len0 = list.get(0).length;
		for (final Comparable<?>[] array : list) {
			if (array == null) throw new ParameterException(SELF
					+ "all objects must have keys (not null)");
			if (array.length != len0) throw new ParameterException(SELF
					+ "all key arrays must have the same length");
			for (final Comparable<?> item : array) {
				if (item == null) throw new ParameterException(SELF
						+ "all keys must have a value (not null)");
			} // for each item
		} // for each array
	} // checkLengthsAndNulls(List<Comparable<?>[]>)

	/** Returns a collator for a language, based on a particular location given
	 * by a language.
	 * @param	language	the language
	 * @return	a collator based on the language's location
	 */
	private static RuleBasedCollator getCollator(String language) {
		final RuleBasedCollator localized = 
				CharReencoder.getCollator(language);
		localized.setStrength(RuleBasedCollator.SECONDARY);
		return localized;
	} // getCollator(String)

	/** Disables external instantiation. */
	private SortUtils() { }
	
} // SortUtils

