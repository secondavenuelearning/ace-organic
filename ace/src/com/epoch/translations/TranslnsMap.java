package com.epoch.translations;

import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Stores a map of a phrase's translations, keyed by languages. */
public class TranslnsMap {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Map of translations keyed by language.  */
	transient private Map<String, String> map = new HashMap<String, String>();

	/** Constructor. */
	public TranslnsMap() { 
		// empty constructor
	}

	/** Constructor. 
	 * @param	aMap	an existing map of translations keyed by language
	 */
	public TranslnsMap(Map<String, String> aMap) { 
		map = new HashMap<String, String>(aMap);
	} // TranslnsMap(Map<String, String>)

	/** Gets a translation into a language.
	 * @param	language	the language
	 * @return	the translation
	 */
	public String get(String language) 	{ return map.get(language); }
	/** Gets whether the map is empty.
	 * @return	true if the map is empty
	 */
	public boolean isEmpty() 			{ return map.isEmpty(); }
	/** Gets the map's size.
	 * @return	the map's size
	 */
	public int size() 					{ return map.size(); }
	/** Gets the list of languages of the map.
	 * @return	list of languages of the map
	 */
	public List<String> getLanguages() 	{ return new ArrayList<String>(map.keySet()); }

	/** Puts a language/translation pair into the map.
	 * @param	language	the language
	 * @param	translation	the translation
	 */
	public void put(String language, String translation) {
		map.put(language, translation);
	} // put(String, String)

} // TranslnsMap
