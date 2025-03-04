package com.epoch.courseware;

import com.epoch.utils.Utils;

/** A user's name. */
public class Name {

	/** User's first (given) name. */
	transient public String givenName; 
	/** User's middle name or initial. */
	transient public String middleName;
	/** User's last (family) name. */
	transient public String familyName; 
	
	/** Constructor. */
	public Name() {
		givenName = "";
		middleName = "";
		familyName = "";
	} // Name()
	
	/** Constructor. 
	 * @param	fn	first name
	 * @param	middle	middle name or initial 
	 * @param	ln	 last name
	 */
	public Name(String fn, String middle, String ln) {
		givenName = (fn == null) ? "" : fn;
		middleName = (middle == null) ? "" : middle;
		familyName = (ln == null) ? "" : ln;
	} // Name(String, String, String)

	/** Constructor. 
	 * @param	fn	first name
	 * @param	ln	 last name
	 */
	public Name(String fn, String ln) {
		givenName = (fn == null) ? "" : fn;
		middleName = "";
		familyName = (ln == null) ? "" : ln;
	} // Name(String, String)

	/** Converts a Name to a string: family name, given name middle initial.
	 * @return	String representation of the user's name
	 */
	public String toString() {
		final StringBuilder nameBld = 
				Utils.getBuilder(familyName, ", ", givenName);
		if (!Utils.isEmptyOrWhitespace(middleName)) {
			Utils.appendTo(nameBld, ' ', middleName);
			if (middleName.length() == 1) nameBld.append('.');
		} // if there's a middle name or initial
		return nameBld.toString();
	} // toString()

	/** Converts a Name to a string: given name middle initial family name.
	 * @return	String representation of the user's name
	 */
	public String toString1stName1st() {
		return toString1stName1st(false);
	} // toString1stName1st()

	/** Converts a Name to a string: given name middle initial family name.
	 * @param	prefersFamilyName1st	prefers the family name to come first,
	 * as in east Asian names
	 * @return	String representation of the user's name
	 */
	public String toString1stName1st(boolean prefersFamilyName1st) {
		final StringBuilder nameBld = (prefersFamilyName1st
				? Utils.getBuilder(familyName, ' ')
				: new StringBuilder());
		nameBld.append(givenName);
		if (!Utils.isEmptyOrWhitespace(middleName)) {
			Utils.appendTo(nameBld, ' ', middleName);
			if (middleName.length() == 1) nameBld.append('.');
		} // if there's a middle name or initial
		if (!prefersFamilyName1st) {
			Utils.appendTo(nameBld, ' ', familyName);
		} // if family name comes last
		return nameBld.toString();
	} // toString1stName1st(boolean)

} // Name
