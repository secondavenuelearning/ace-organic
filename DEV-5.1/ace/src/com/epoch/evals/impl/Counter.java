package com.epoch.evals.impl;

import com.epoch.evals.impl.implConstants.CountConstants;

/** Contains fields and methods used by methods that count the proportion of
 * items in a response that have a particular property. */
public class Counter extends TextAndNumbers implements CountConstants {

	/** How many molecules in the response should or should not match. */ 
	protected int howMany;

	/** Name of the structure for which to search. */
	protected String molName = null;
	
	/** Determines whether the number of matches and nonmatches is in accordance
	 * with <code>howMany</code>.
	 * @param	matches	array containing the number of matches and nonmatches
	 * @return	true if the number of matches and nonmatches is in accordance 
	 * with <code>howMany</code>
	 */
	protected boolean getIsSatisfied(int[] matches) {
		boolean satisfied = false;
		switch (howMany) {
		case NONE: 		satisfied = (matches[MATCHES] == 0); break;
		case ONLY: 		satisfied = (matches[MATCHES] == 1 
										&& matches[NONMATCHES] == 0); break;
		case ONE: 		satisfied = (matches[MATCHES] == 1); break;
		case ANY: 		satisfied = (matches[MATCHES] >= 1); break;
		case NOT_ALL: 	satisfied = (matches[NONMATCHES] >= 1); break;
		case ALL: 		satisfied = (matches[NONMATCHES] == 0); break;
		default: System.out.println("Counter: bad howMany"); break;
		} // switch (howMany)
		return satisfied;
	} // getIsSatisfied(int[])
		
	/** Gets how many molecules in the response should or should not have the 
	 * property. 
	 * @return	how many molecules
	 */
	public int getHowMany() 				{ return howMany; } 
	/** Sets how many molecules in the response should or should not have the 
	 * property. 
	 * @param	howMany	how many molecules
	 */
	public void setHowMany(int howMany) 	{ this.howMany = howMany; } 
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 			{ return false; }

} // Counter
