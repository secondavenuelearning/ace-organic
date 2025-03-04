package com.epoch.physics.physicsConstants;

/** Holds constants for EquationFunctions. */
public interface EquationFunctionsConstants {

	// public static final is implied by interface

	/** Unit prefixes that indicate magnitudes, and their magnitudes. */
	String[][] PREFIXES_MAGNITUDES = new String[][] {
			{"Y", "10^24"},
			{"Z", "10^21"},
			{"E", "10^18"},
			{"P", "10^15"},
			{"T", "10^12"},
			{"G", "10^9"},
			{"M", "10^6"},
			{"k", "10^3"},
			{"h", "10^2"},
			{"da", "10^1"},
			{"d", "10^-1"},
			{"c", "10^-2"},
			{"m", "10^-3"},
			{"u", "10^-6"},
			{String.valueOf((char) 181), "10^-6"},
			{String.valueOf((char) 956), "10^-6"},
			{"n", "10^-9"},
			{"p", "10^-12"},
			{"f", "10^-15"},
			{"a", "10^-18"},
			{"z", "10^-21"},
			{"y", "10^-24"}
			};
	/** Units that begin with letters that can indicate magnitudes. */
	String[] PREFIX_EXCEPTIONS = new String[] {
			"amu",
			"atm",
			"cal",
			"cc",
			"cd",
			"dyn",
			"ft",
			"mi",
			"min", 
			"mol",
			"psi",
			"yd",
			"yr"
			};
	/** Index of regular expression and replacement string for inserting 
	 * a * symbol between numbers and letters or parentheses. */
	int TIMES_SIGNS = 0;
	/** Index of regular expression and replacement string for extending 
	 * superscripts or subscripts to all nonwhitespace characters, not just one, 
	 * after ^ or _.  */
	int EXTEND_SUPER_SUB = 1;
	/** Index of regular expression and replacement string for replacing mu
	 * CERs with u. */
	int MICRO = 2;
	/** Regular expressions. */
	String[] PATTERNS = new String[] {
			"(^|[^A-Za-z0-9])(\\d+[ ]*)(\\p{L}|\\(|&#)",
			"(?<!int)(\\^|_)(\\S)(\\S+)",
			"&(#181|#956|mu|micro);"
			};
	/** Substitutions for regular expressions. */
	String[] REPLACEMENTS = new String[] {
			"$1$2* $3",
			"$1{$2$3}", 
			"u"
			};

} // EquationFunctionsConstants
