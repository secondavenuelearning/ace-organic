// <!-- avoid parsing the following as HTML
/*jsl:option explicit*/
/*jsl:import jslib.js*/
/*jsl:import lewisJS.js*/

var logBld = new String.builder();
var verbose = false;
var chopped = false;
var ALWAYS_CALCULATE = true;
var storedPermutationsOf1ToN = [];

function CompBond(theAtoms, theOrder, theStereo, theParentMol) {
	"use strict";
	/* these are the members */
	var atoms = theAtoms,
		order = theOrder,
		stereo = theStereo,
		parentMol = theParentMol;
	/* simple methods */
	this.getAtoms = function () { return atoms; };
	this.getOrder = function () { return order; };
	this.getStereo = function () { return stereo; };
	this.getBondNum = function () { return parentMol.getNumOfBond(this); };
	this.superficiallyMatches = function (compBond) {
		return this.getOrder() === compBond.getOrder();
	}; // superficiallyMatches()
	/* log output */
	this.toString = function () {
		var bld = new String.builder().
				append(atoms[0].getElement()).
				append(parentMol.getNumOfAtom(atoms[0])).
				append(order === 1 ? '-' : order === 2 ? '=' : '#').
				append(atoms[1].getElement()).
				append(parentMol.getNumOfAtom(atoms[1]));
		if (stereo !== PLAIN) {
			bld.append(stereo === HASH ? ' (hashed)' :
					stereo === BOLD ? ' (bold)' :
					stereo === WAVY ? ' (wavy)' :
					' (unknown: ' + stereo + ')');
		} // if stereo
		bld.append(' [').append(parentMol.getNumOfBond(this)).append(']');
		return bld.toString();
	}; // toString()
} // CompBond

// Gets the atomic number of an element.
function getAtomicNumber(element) {
	"use strict";
	var atomicNumbers = {
		H: 1,
		He: 2,
		Li: 3,
		Be: 4,
		B: 5,
		C: 6,
		N: 7,
		O: 8,
		F: 9,
		Ne: 10,
		Na: 11,
		Mg: 12,
		Al: 13,
		Si: 14,
		P: 15,
		S: 16,
		Cl: 17,
		Ar: 18,
		K: 19,
		Ca: 20,
		Sc: 21,
		Ti: 22,
		V: 23,
		Cr: 24,
		Mn: 25,
		Fe: 26,
		Co: 27,
		Ni: 28,
		Cu: 29,
		Zn: 30,
		Ga: 31,
		Ge: 32,
		As: 33,
		Se: 34,
		Br: 35,
		Kr: 36,
		Rb: 37,
		Sr: 38,
		Y: 39,
		Zr: 40,
		Nb: 41,
		Mo: 42,
		Tc: 43,
		Ru: 44,
		Rh: 45,
		Pd: 46,
		Ag: 47,
		Cd: 48,
		In: 49,
		Sn: 50,
		Sb: 51,
		Te: 52,
		I: 53,
		Xe: 54,
		Cs: 55,
		Ba: 56,
		La: 57,
		Ce: 58,
		Pr: 59,
		Nd: 60,
		Pm: 61,
		Sm: 62,
		Eu: 63,
		Gd: 64,
		Tb: 65,
		Dy: 66,
		Ho: 67,
		Er: 68,
		Tm: 69,
		Yb: 70,
		Lu: 71,
		Hf: 72,
		Ta: 73,
		W: 74,
		Re: 75,
		Os: 76,
		Ir: 77,
		Pt: 78,
		Au: 79,
		Hg: 80,
		Tl: 81,
		Pb: 82,
		Bi: 83,
		Po: 84,
		At: 85,
		Rn: 86,
		Fr: 87,
		Ra: 88,
		Ac: 89,
		Th: 90,
		Pa: 91,
		U: 92,
		Np: 93,
		Pu: 94,
		Am: 95,
		Cm: 96,
		Bk: 97,
		Cf: 98,
		Es: 99,
		Fm: 100,
		Md: 101,
		No: 102,
		Lr: 103,
		Rf: 104,
		Db: 105,
		Sg: 106,
		Bh: 107,
		Hs: 108,
		Mt: 109,
		Ds: 110,
		Rg: 111,
		Cn: 112,
		Uut: 113,
		Fl: 114
	};
	return atomicNumbers[element];
} // getAtomicNumber()

// Adds to the log, maybe prints it.
function log(arr) {
	"use strict";
	var i;
	for (i = 0; i < arr.length; i += 1) {
		logBld.append(arr[i]);
	}
	if (chopped) {
		alert(logBld.toString());
		logBld.clear();
	} // if chopped
} // log()

/* Gets all ligands of an atom at a particular depth.  Called recursively! */
function getLigandsToDepth(currentDepth, currentDepthLigands, targetDepth) {
	"use strict";
	if (currentDepth < targetDepth) {
		var allNextDepthLigands = [];
		var msgArr = ['getLigandsToDepth: currentDepth = ', currentDepth,
				', targetDepth = ', targetDepth, ', currentDepthLigands = '];
		var ligNum,
			deeperNum;
		for (ligNum = 0; ligNum < currentDepthLigands.length; ligNum += 1) {
			var currentDepthLigand = currentDepthLigands[ligNum];
			if (ligNum > 0) {
				msgArr.push(', ');
			}
			msgArr.push(currentDepthLigand.toString());
			var nextDepthLigands = currentDepthLigand.getLigands();
			var numDeeperLigs = nextDepthLigands.length;
			for (deeperNum = 0; deeperNum < numDeeperLigs; deeperNum += 1) {
				allNextDepthLigands.push(nextDepthLigands[deeperNum]);
			} // for each ligand of the current ligand
		} // for each ligand
		currentDepthLigands.length = 0;
		msgArr.push(', replacing them with next-depth ligands: ');
		for (ligNum = 0; ligNum < allNextDepthLigands.length; ligNum += 1) {
			currentDepthLigands.push(allNextDepthLigands[ligNum]);
			if (ligNum > 0) {
				msgArr.push(', ');
			}
			msgArr.push(allNextDepthLigands[ligNum].toString());
		} // for each ligand
		/* if (verbose) {
			log(msgArr);
		} */
		getLigandsToDepth(currentDepth + 1, currentDepthLigands,
				targetDepth); // recursive call to next level
	} // if not yet at sufficient depth
} // getLigandsToDepth()

/* Borrowed from http://jsfiddle.net/q62Dr/127/.
Call as follows:
	var toPermute = [],
		num;
	for (num = 1; num <= maxNum; num += 1) {
		toPermute.push(num);
	}
	var permutations = [];
	permute(toPermute, function (a) { permutations.push(a.slice(0)); });
*/
function permute(array, callback) {
	"use strict";
	// Does the actual permutation work on array[], starting at index
	function p(array, index, callback) {
		// Swap elements i1 and i2 in array a[]
		var i, count;
		function swap(a, i1, i2) {
			var t = a[i1];
			a[i1] = a[i2];
			a[i2] = t;
		} // swap()
		if (index === array.length - 1) {
			callback(array);
			return 1;
		} else {
			count = p(array, index + 1, callback);
			for (i = index + 1; i < array.length; i += 1) {
				swap(array, i, index);
				count += p(array, index + 1, callback);
				swap(array, i, index);
			} // for each remaining variable in array
			return count;
		} // if index
	} // p()
	return (!array || array.length === 0 ? 0 : p(array, 0, callback));
} // permute()

// Gets array of all permutations of numbers 1...n, storing it to retrieve
// later.
function getPermutations(n) {
	"use strict";
	if (storedPermutationsOf1ToN.length < n ||
			isEmpty(storedPermutationsOf1ToN[n - 1])) {
		var nums1ToN = [],
			num;
		for (num = 1; num <= n; num += 1) {
			nums1ToN.push(num);
		}
		var permutationsOf1ToN = [];
		permute(nums1ToN, function (a) { permutationsOf1ToN.push(a.slice(0)); });
		storedPermutationsOf1ToN[n - 1] = permutationsOf1ToN;
	} // if need to generate new permutations
	return storedPermutationsOf1ToN[n - 1];
} // getPermutations()

// Returns a positive number if the first atom has priority, a negative
// number if the second has priority, and 0 if they have equal priority.
function priorityDifference(digraphAtom1, digraphAtom2, configConsiderLevel) {
	"use strict";
	if (!digraphAtom1 && !digraphAtom2) {
		return 0;
	}
	if (!digraphAtom1) {
		return -1;
	}
	if (!digraphAtom2) {
		return 1;
	}
	var atomicNumberDiff =
			digraphAtom1.getAtomicNumber() - digraphAtom2.getAtomicNumber();
	if (atomicNumberDiff !== 0) {
		return atomicNumberDiff;
	}
	var atomNum, digraphAtom, origAtom;
	if (configConsiderLevel > 0) {
		var configs = [];
		for (atomNum = 1; atomNum <= 2; atomNum += 1) {
			digraphAtom = (atomNum === 1 ? digraphAtom1 : digraphAtom2);
			origAtom = digraphAtom.getAtom();
			configs.push(origAtom.getConfiguration(!ALWAYS_CALCULATE));
		} // for each atom whose priority is being compared
		if (configs[0] === 'E' && configs[1] === 'Z') {
			return 1;
		}
		if (configs[0] === 'Z' && configs[1] === 'E') {
			return -1;
		}
		if (configConsiderLevel > 1) {
			if (configs[0] === 'R' && configs[1] === 'S') {
				return 1;
			}
			if (configs[0] === 'S' && configs[1] === 'R') {
				return -1;
			}
			if (configConsiderLevel > 2) {
				var proconfigs = [],
					atom1ProNum,
					atom1Proconfig,
					atom2ProNum,
					atom2Proconfig;
				for (atomNum = 1; atomNum <= 2; atomNum += 1) {
					digraphAtom = (atomNum === 1 ? digraphAtom1 : digraphAtom2);
					origAtom = digraphAtom.getAtom();
					proconfigs.push(origAtom.getProconfigurations());
				} // for each atom whose priority is being compared
				// each atom has a [short] list of proconfigurations with
				// respect to one or more ligands; each proconfiguration is
				// array of [ligandAtomNum, proconfiguration]; we need to
				// compare proconfigurations wrt the same atoms
				for (atom1ProNum = 0;
						atom1ProNum < proconfigs[0].length;
						atom1ProNum += 1) {
					atom1Proconfig = proconfigs[0][atom1ProNum];
					for (atom2ProNum = 0;
							atom2ProNum < proconfigs[1].length;
							atom2ProNum += 1) {
						atom2Proconfig = proconfigs[1][atom2ProNum];
						if (atom1Proconfig[0] === atom2Proconfig[0]) {
							if (atom1Proconfig[1] === 'pro-R' &&
									atom1Proconfig[1] === 'pro-S') {
								return 1;
							} // if proconfigurations wrt same ligand
							if (atom1Proconfig[1] === 'pro-S' &&
									atom2Proconfig[1] === 'pro-R') {
								return -1;
							} // if proconfigurations wrt same ligand
						} // if considering proconfiguration wrt same ligand
					} // for each atom2 proconfiguration
				} // for each atom1 proconfiguration
			} // if should consider proconfiguration as well
		} // if should consider R/S as well
	} // if should consider E/Z as well
	return 0;
} // priorityDifference()

// Sorts an array of atoms by priority, highest to lowest.
function sortByPriority(digraphAtoms, configConsiderLevel) {
	"use strict";
	digraphAtoms.sort(function (atom1, atom2) {
		return priorityDifference(atom2, atom1, configConsiderLevel);
	});
} // sortByPriority()

// Returns a positive number if the first atom has priority, a negative
// number if the second has priority, and 0 if they have equal priority.
// Repeats at further levels of depth if first atoms' priorities are equal.
function deepPriorityDifference(digraphAtom1, digraphAtom2, configConsiderLevel) {
	"use strict";
	// make list of ligands of each atom, excluding the one in the chain back
	// to the original atom whose configuration is being determined
	var depth = 0,
		atomNum,
		digraphAtom,
		atomLigs,
		ligNum;
	while (true) {
		var comparedAtomsLigs = [];
		for (atomNum = 1; atomNum <= 2; atomNum += 1) {
			digraphAtom = (atomNum === 1 ? digraphAtom1 : digraphAtom2);
			atomLigs = [digraphAtom];
			getLigandsToDepth(0, atomLigs, depth);
			comparedAtomsLigs.push(atomLigs);
		} // for each atom whose priority is being compared
		if (comparedAtomsLigs[0].length === 0 &&
				comparedAtomsLigs[1].length === 0) {
			return 0;
		} // if number of ligands is zero
		// sort ligands by priority
		sortByPriority(comparedAtomsLigs[0], configConsiderLevel);
		sortByPriority(comparedAtomsLigs[1], configConsiderLevel);
		/* if (verbose) {
			for (atomNum = 0; atomNum < 2; atomNum += 1) {
				var digraphAtom = (atomNum === 0 ? digraphAtom1 : digraphAtom2);
				var atomLigs = comparedAtomsLigs[atomNum];
				var msgArr = ['deepPriorityDifference: at depth ',
						depth, ', atom ', digraphAtom.toString(),
						' has sorted ligand(s) '];
				for (ligNum = 0; ligNum < atomLigs.length; ligNum += 1) {
					if (ligNum > 0) {
						msgArr.push(', ');
					}
					msgArr.push(atomLigs[ligNum].toString());
				} // for each ligand at the depth
				msgArr.push('.\n');
				log(msgArr);
			} // for each atom whose priority is being compared
		} // if verbose */
		// compare priorities of nth-priority ligand of atom1
		// to nth-priority ligand of atom2
		for (ligNum = 0; ligNum < comparedAtomsLigs[0].length; ligNum += 1) {
			if (ligNum >= comparedAtomsLigs[1].length) {
				return 1;
			} // if atom2 has no more ligands
			var priorityDiff = priorityDifference(
					comparedAtomsLigs[0][ligNum],
					comparedAtomsLigs[1][ligNum],
					configConsiderLevel);
			/* if (verbose) {
				log(['deepPriorityDifference: ligands at depth ',
					depth, ' ', comparedAtomsLigs[0][ligNum].toString(),
					' and ', comparedAtomsLigs[1][ligNum].toString(),
					' have ', priorityDiff === 0 ? 'same' : 'different',
					' priorities.\n']);
				}
			*/
			if (priorityDiff !== 0) {
				return priorityDiff;
			}
		} // for each ligand
		if (comparedAtomsLigs[0].length < comparedAtomsLigs[1].length) {
			return -1;
		} // if atom1 has fewer ligands than atom2
		depth += 1;
		/* if (verbose) {
			log(['no difference found between ', digraphAtom1.toString(),
				', and ', digraphAtom2.toString(), ' at depth ', depth,
				'; moving to next depth.']);
		} */
	} // while true
	return 0;
} // deepPriorityDifference()

// Sorts an array of atoms by priority, highest to lowest.
function sortByDeepPriority(digraphAtoms, configConsiderLevel) {
	"use strict";
	digraphAtoms.sort(function (atom1, atom2) {
		return deepPriorityDifference(atom2, atom1, configConsiderLevel);
	});
} // sortByDeepPriority()

function CompAtom(atom, theParentMol) {
	"use strict";
	/* these are the members */
	var element = atom.getElement(),
		charge = atom.getCharge(),
		eCounts = [atom.getNumPairedElectrons(),
			atom.getNumUnpairedElectrons()
			],
		posn = atom.getPosn(),
		parentMol = theParentMol,
		bonds = [],
		matchedAtomNum = NONE,
		configuration = (!atom.getConfigurationCopy ||
				!atom.getConfigurationCopy() ? 'unknown' :
					atom.getConfigurationCopy()),
		proconfigurations = (!atom.getProconfigurationsCopy ||
				!atom.getProconfigurationsCopy() ? 'unknown' :
			atom.getProconfigurationsCopy()),
		bondNum,
		roundNum;
	/* construction method */
	this.addBond = function (bond) {
		bonds.push(bond);
	};
	/* simple methods */
	this.getElement = function () {
		return element;
	};
	this.getCharge = function () {
		return charge;
	};
	this.getECounts = function () {
		return eCounts;
	};
	this.getNumPairedElectrons = function () {
		return eCounts[0];
	};
	this.getNumUnpairedElectrons = function () {
		return eCounts[1];
	};
	this.getPosn = function () {
		return posn;
	};
	this.getNumBonds = function () {
		return bonds.length;
	};
	this.getBonds = function () {
		return bonds;
	};
	this.getBond = function (bondNum) {
		return bonds[bondNum - 1]; // 1-based
	};
	this.getAtomNum = function () {
		return parentMol.getNumOfAtom(this);
	};
	this.getMatchedAtomNum = function () {
		return matchedAtomNum;
	};
	this.getAtomicNumber = function () {
		return getAtomicNumber(element);
	};
	this.getConfigurationCopy = function () {
		return configuration;
	};
	this.setConfiguration = function (config) {
		configuration = config;
	};
	this.getProconfigurationsCopy = function () {
		return proconfigurations;
	};
	this.setProconfiguration = function (ligandNum, config) {
		var newProconfiguration, proNum;
		if (proconfigurations === 'unknown') {
			proconfigurations = [];
		}
		newProconfiguration = [ligandNum, config];
		for (proNum = 0; proNum < proconfigurations; proNum += 1) {
			if (arraysMatch(proconfigurations[proNum], newProconfiguration)) {
				return;
			} // if proconfiguration is already calculated
		} // for each existing proconfiguration
		proconfigurations.push(newProconfiguration);
	}; // setProconfiguration()
	/* matching method called recursively! */
	this.matches = function (compAtom, level) {
		var recursionLevel = level + 1,
			startMsgBld = new String.builder().
				append('\nCompAtom: comparing mol1 ').
				append(this.getElement()).append(this.getAtomNum()).
				append(' to mol2 ').
				append(compAtom.getElement()).append(compAtom.getAtomNum()).
				append(', recursion level ').append(recursionLevel).
				append(': '),
			startMsg = startMsgBld.toString(),
			alreadyMatch;
		// has a match or mismatch already been determined for this pair?
		if (this.getMatchedAtomNum() !== NONE) {
			alreadyMatch =
				this.getMatchedAtomNum() === compAtom.getAtomNum();
			if (verbose) {
				log([startMsg, 'Mol1 atom ', this.getAtomNum(),
					' already matches to mol2 atom ', this.getMatchedAtomNum(),
					' (', alreadyMatch ? 'same as' : 'not',
					' this one, returning ', alreadyMatch]);
			}
			return alreadyMatch;
		} // if this atom has already been assigned a match
		// if needed, do these atoms match superficially?
		if (recursionLevel === 1 &&
				!this.superficiallyMatches(compAtom, startMsg)) {
			return false;
		} // if atoms need to be compared and don't match
		if (verbose) {
			log([startMsg, 'Comparing mol1 atom\'s bonds:\n',
				this.getBondsDescription(), 'to mol2 atom\'s bonds:\n',
				compAtom.getBondsDescription()]);
		}
		var myBonds = this.getBonds();
		var compBonds = compAtom.getBonds();
		var numMyBonds = myBonds.length;
		var numCompBonds = compBonds.length;
		// get permutations in which the bond order and stereo and the atoms on
		// the other end match
		var allBondPermutations = getPermutations(numMyBonds);
		var superficialMatchPermutations = [];
		var myBondNum, compBondNum, permNum, bondsToCompare, myBondAtoms;
		var myPosn, compPosn, myBondOtherAtom, compBondOtherAtom, compBondAtoms;
		for (permNum = 0; permNum < allBondPermutations.length; permNum += 1) {
			bondsToCompare = allBondPermutations[permNum];
			var superficiallyMatches = true;
			for (myBondNum = 1; myBondNum <= numMyBonds; myBondNum += 1) {
				compBondNum = bondsToCompare[myBondNum - 1];
				var myBond = myBonds[myBondNum - 1];
				var compBond = compBonds[compBondNum - 1];
				myBondAtoms = myBond.getAtoms();
				compBondAtoms = compBond.getAtoms();
				myPosn = (myBondAtoms[0] === this ? 0 : 1);
				compPosn = (compBondAtoms[0] === compAtom ? 0 : 1);
				myBondOtherAtom = myBondAtoms[1 - myPosn];
				compBondOtherAtom = compBondAtoms[1 - compPosn];
				superficiallyMatches =
						myBond.superficiallyMatches(compBond) &&
						myBondOtherAtom.superficiallyMatches(
							compBondOtherAtom);
				if (!superficiallyMatches) {
					break;
				}
			} // for each bond comparison
			if (superficiallyMatches) {
				superficialMatchPermutations.push(bondsToCompare);
				if (verbose) {
					var msgArr = [];
					msgArr.push('\nSuperficial match:\n');
					for (myBondNum = 1;
							myBondNum <= numMyBonds; myBondNum += 1) {
						compBondNum = bondsToCompare[myBondNum - 1];
						msgArr.push('Bond ');
						msgArr.push(myBonds[myBondNum - 1].toString());
						msgArr.push(' & bond ');
						msgArr.push(compBonds[compBondNum - 1].toString());
						msgArr.push('\n');
					} // for each pair of bonds
					log(msgArr);
				} // if verbose
			} // if the permutation worked
		} // for each permutation
		if (isEmpty(superficialMatchPermutations) && verbose) {
			log([startMsg, 'No superficial matches found between sets ' +
					'of bonds; returning no match of atoms.\n']);
		} // if there are no superfically matching permutations
		// for each superfically matching permutation, recursively compare
		// the atoms at the other ends of each bond
		for (permNum = 0;
				permNum < superficialMatchPermutations.length; permNum += 1) {
			matchedAtomNum = compAtom.getAtomNum();
			bondsToCompare = superficialMatchPermutations[permNum];
			if (verbose) {
				log([startMsg, 'Checking superficial match ',
					permNum + 1, ' ([', bondsToCompare, ']) of ',
					superficialMatchPermutations.length,
					' for recursive match.']);
			}
			var foundMatch = false;
			for (myBondNum = 1; myBondNum <= numMyBonds; myBondNum += 1) {
				compBondNum = bondsToCompare[myBondNum - 1];
				myBondAtoms = myBonds[myBondNum - 1].getAtoms();
				compBondAtoms = compBonds[compBondNum - 1].getAtoms();
				myPosn = (myBondAtoms[0] === this ? 0 : 1);
				compPosn = (compBondAtoms[0] === compAtom ? 0 : 1);
				myBondOtherAtom = myBondAtoms[1 - myPosn];
				compBondOtherAtom = compBondAtoms[1 - compPosn];
				if (verbose) {
					log(['\nComparing bonded atoms ',
						myBondOtherAtom.toString(), ' and ',
						compBondOtherAtom.toString()]);
				}
				foundMatch = myBondOtherAtom.matches(
						compBondOtherAtom, recursionLevel); // recursive call
				if (!foundMatch) {
					break;
				}
			} // for each bond
			if (verbose) {
				log([startMsg, 'Superficial match [', bondsToCompare,
					'] ', foundMatch ? 'gave' : 'didn\'t give',
					' a recursive match', foundMatch ? '!' : '.']);
			}
			if (foundMatch) {
				matchedAtomNum = compAtom.getAtomNum();
				break;
			} else {
				matchedAtomNum = NONE;
			} // if match found
		} // for each permutation of bonds
		return matchedAtomNum !== NONE;
	}; // matches()
	/* superficial matching method, not called recursively */
	this.superficiallyMatches = function (compAtom, startMsg) {
		var match = this.getElement() === compAtom.getElement() &&
				this.getCharge() === compAtom.getCharge() &&
				arraysMatch(this.getECounts(), compAtom.getECounts()) &&
				this.getNumBonds() === compAtom.getNumBonds();
		// calculation of configuration is costly
		var thisConfig = (match ? this.getConfiguration(ALWAYS_CALCULATE) : '');
		var compConfig = (match ? compAtom.getConfiguration(ALWAYS_CALCULATE) : '');
		match = match && thisConfig === compConfig;
		var veryVerbose = verbose && !isEmpty(startMsg);
		if (match && veryVerbose) {
			var charge = this.getCharge();
			log([startMsg, 'Both atoms are ', this.getElement(),
					' with ', charge === 0 ? 'no ' : '', 'charge',
					charge !== 0 ? ' ' : '', charge > 0 ? '+' : '',
					charge !== 0 ? charge : '', ', ', this.getECounts(),
					' e(-) counts, ', this.getNumBonds(),
					' bonds, & configuration ', thisConfig]);
		} else if (veryVerbose) {
			log([startMsg, 'Atom elements ', this.getElement(), ', ',
					compAtom.getElement(), '; charges ', this.getCharge(),
					', ', compAtom.getCharge(), '; e(-) counts [',
					this.getECounts(), '], [', compAtom.getECounts(),
					']; ', this.getNumBonds(), ', ', compAtom.getNumBonds(),
					' bonds; configurations ', thisConfig, ', ', compConfig,
					'; no match.']);
		} // if atoms are equivalent
		return match;
	}; // superficiallyMatches()
	/* Returns the R/S configuration of this atom or the E/Z configuration of
	 * its double bond. */
	this.getConfiguration = function (alwaysCalculate) {
		var stereoBonds = [],
			doubleBonds = [],
			bond,
			myBondAtoms,
			bondNum;
		for (bondNum = 0; bondNum < bonds.length; bondNum += 1) {
			bond = bonds[bondNum];
			if (bond.getOrder() === 2) {
				doubleBonds.push(bond);
			} // if multiple bond
			myBondAtoms = bond.getAtoms();
			if (bond.getStereo() !== PLAIN && myBondAtoms[0] === this) {
				if (bond.getStereo() === WAVY) {
					return 'has wavy bond, no configuration';
				} // if bond is wavy
				stereoBonds.push(bond);
			} // if stereo bond originates at this atom
		} // for each bond
		return (stereoBonds.length > 0 && doubleBonds.length === 0 ?
					this.getRSConfiguration(stereoBonds, alwaysCalculate) :
				doubleBonds.length === 1 ?
					this.getEZConfiguration(doubleBonds[0]) :
				'--');
	}; // getConfiguration()
	/* Returns the R/S configuration of this atom. */
	this.getRSConfiguration = function (stereoBonds, alwaysCalculate) {
		var startMsgBld = new String.builder().
				append('getRSConfiguration for atom ').
				append(this.getElement()).append(this.getAtomNum()).
				append(': ');
		var startMsg = startMsgBld.toString();
		if (configuration === 'calculating') {
			if (!alwaysCalculate) {
				if (verbose) {
					log([startMsg, 'configuration is already ' +
						'being calculated, so returning without further ' +
						'calculation.\n']);
				}
				return configuration; // prevents infinite loops
			} // if don't recalculate a configuration already being calculated
		} else if (configuration !== 'unknown') {
			return configuration;
		}
		configuration = 'calculating';
		var msgArr, bondNum;
		if (verbose) {
			msgArr = [];
			msgArr.push(startMsg);
			msgArr.push(stereoBonds.length);
			msgArr.push(' stereo bond');
			if (stereoBonds.length !== 1) {
				msgArr.push('s');
			}
			msgArr.push(' beginning at this atom:\n');
			for (bondNum = 0; bondNum < stereoBonds.length; bondNum += 1) {
				msgArr.push(stereoBonds[bondNum].toString());
				msgArr.push('\n');
			} // for each stereo bond
			msgArr.push('\namong all bonds:\n');
			msgArr.push(this.getBondsDescription());
			log(msgArr);
		} // if verbose
		var orderedBonds = [];
		var configLevelConsidered = this.prioritizeMyBonds(orderedBonds);
		if (isEmpty(orderedBonds)) {
			if (verbose) {
				log([startMsg, 'bonds could not be prioritized; ' +
					'returning no configuration.\n']);
			}
			return 'not a stereocenter';
		} // if bonds couldn't be ordered
		if (verbose) {
			msgArr = [];
			msgArr.push(startMsg);
			msgArr.push('with');
			if (configLevelConsidered === 0) {
				msgArr.push('out');
			}
			msgArr.push(' consideration of ');
			msgArr.push(configLevelConsidered >= 1 ? 'E/Z ' : '');
			if (configLevelConsidered == 2) {
				msgArr.push('and R/S ');
			}
			msgArr.push('configuration, bonds prioritized in order:\n');
			for (bondNum = 0; bondNum < bonds.length; bondNum += 1) {
				msgArr.push(orderedBonds[bondNum].toString());
				msgArr.push('\n');
			} // for each bond
			log(msgArr);
		} // if verbose
		if (stereoBonds.length === 1) {
			if ((orderedBonds.length === 4 && arraysMatch(eCounts, [0, 0])) ||
					(orderedBonds.length === 3 &&
						arraysMatch(eCounts, [2, 0]))) {
				configuration = this.calculateConfiguration(orderedBonds,
						stereoBonds[0], !isEmpty(startMsg));
			} else {
				if (verbose) {
					log([startMsg, 'wrong number of bonds or ' +
						'unshared electrons to calculate configuration.\n']);
				}
				configuration = 'none';
			} // if calculable
		} else if (stereoBonds.length === 2) {
			if (orderedBonds.length === 4) {
				if (verbose && !isEmpty(startMsg)) {
					log([startMsg,
						'atom has 4 bonds, two of which are stereo\n']);
				}
				configuration = (this.orientationTwoStereoBondsOK(
							orderedBonds, stereoBonds, startMsg) ?
						this.calculateConfiguration(orderedBonds,
							stereoBonds[0], !isEmpty(startMsg)) :
						'ambiguous orientation of stereo bonds');
			} else {
				if (verbose) {
					log([startMsg, 'wrong number of bonds or ' +
						'unshared electrons to calculate configuration.\n']);
				}
				configuration = 'none';
			} // if calculable
		} else {
			if (verbose) {
				log([startMsg, 'too many stereobonds to determine the ' +
						'configuration.\n']);
			} // if verbose
		} // if number of stereobonds and total bonds
		if (verbose) {
			log(['\n']);
		}
		if (configLevelConsidered > 1) {
			configuration = configuration.toLowerCase();
		} // if configLevelConsidered
		return configuration;
	}; // getRSConfiguration()
	// Determines whether two stereobonds are bisected by two regular bonds or are
	// adjacent to one another.
	this.orientationTwoStereoBondsOK = function (orderedBonds, stereoBonds,
			startMsg) {
		// note: splice causes inconsistency in orderedBonds later
		var angleSortedBonds = [];
		var bondNum;
		for (bondNum = 0; bondNum < orderedBonds.length; bondNum += 1) {
			angleSortedBonds.push(orderedBonds[bondNum]);
		} // for each ordered bond
		this.sortByAngleToXAxis(angleSortedBonds, this);
		var msgArr;
		if (verbose && !isEmpty(startMsg)) {
			msgArr = [];
			msgArr.push(startMsg);
			msgArr.push('orderedBonds sorted by angle to X axis:\n');
			for (bondNum = 0; bondNum < angleSortedBonds.length; bondNum += 1) {
				var bond = angleSortedBonds[bondNum];
				var bondAtoms = bond.getAtoms();
				var bondVector =
					(bondAtoms[0].getAtomNum() === this.getAtomNum() ?
						getVector(this.getPosn(), bondAtoms[1].getPosn()) :
						getVector(this.getPosn(), bondAtoms[0].getPosn()));
				msgArr.push('Bond ');
				msgArr.push(bond.toString());
				msgArr.push(' has angle to X axis of ');
				msgArr.push(toDegrees(getAngle(bondVector)));
				msgArr.push('\n');
			} // for each bond
			log(msgArr);
		} // if verbose
		var stereoBondPosnsAmongAngles = [];
		for (bondNum = 0; bondNum < angleSortedBonds.length; bondNum += 1) {
			var angleSortedBondNum = angleSortedBonds[bondNum].getBondNum();
			if (angleSortedBondNum === stereoBonds[0].getBondNum()) {
				stereoBondPosnsAmongAngles[0] = bondNum;
			} else if (angleSortedBondNum === stereoBonds[1].getBondNum()) {
				stereoBondPosnsAmongAngles[1] = bondNum;
			} // if found the bond
		} // for each bond
		var stereoBondsAreAdjacent =
				Math.abs(stereoBondPosnsAmongAngles[0] -
					stereoBondPosnsAmongAngles[1]) !== 2;
		var sameStereo =
				stereoBonds[0].getStereo() === stereoBonds[1].getStereo();
		var orientationOK = stereoBondsAreAdjacent !== sameStereo;
		if (verbose && !isEmpty(startMsg)) {
			msgArr = [];
			msgArr.push(startMsg);
			msgArr.push('stereoBondPosnsAmongAngles = [');
			msgArr.push(stereoBondPosnsAmongAngles);
			msgArr.push(']; stereo bonds pointing in ');
			msgArr.push(sameStereo ? 'same' : 'opposite');
			msgArr.push(' direction');
			if (!sameStereo) {
				msgArr.push('s');
			}
			msgArr.push(orientationOK ? ' and' : ' but');
			msgArr.push(' are ');
			msgArr.push(stereoBondsAreAdjacent ? 'adjacent' :
					'alternating with nonstereobonds');
			msgArr.push(', so can');
			if (!orientationOK) {
				msgArr.push('\'t');
			}
			msgArr.push(' determine configuration.');
			log(msgArr);
		} // if verbose
		return orientationOK;
	}; // orientationTwoStereoBondsOK()
	// Sorts an array of atoms by priority, highest to lowest.
	this.sortByAngleToXAxis = function (bonds, centerAtom) {
		bonds.sort(function (bond1, bond2) {
			var thisPosn = centerAtom.getPosn();
			var bond1Atoms = bond1.getAtoms();
			var bond1Vector =
					(bond1Atoms[0].getAtomNum() === centerAtom.getAtomNum() ?
						getVector(thisPosn, bond1Atoms[1].getPosn()) :
						getVector(thisPosn, bond1Atoms[0].getPosn()));
			var bond2Atoms = bond2.getAtoms();
			var bond2Vector =
					(bond2Atoms[0].getAtomNum() === centerAtom.getAtomNum() ?
						getVector(thisPosn, bond2Atoms[1].getPosn()) :
						getVector(thisPosn, bond2Atoms[0].getPosn()));
			return getAngle(bond1Vector) - getAngle(bond2Vector);
		});
	}; // sortByAngleToXAxis()
	/* Returns the E/Z configuration of this atom's double bond. */
	this.getEZConfiguration = function (doubleBond) {
		var startMsgBld = new String.builder().
				append('getEZConfiguration for atom ').
				append(this.getElement()).append(this.getAtomNum()).
				append(': ');
		var startMsg = startMsgBld.toString();
		var msgArr;
		if (verbose) {
			msgArr = [];
			msgArr.push(startMsg);
			msgArr.push('double bond involving atom ');
			msgArr.push(this.toString());
			msgArr.push(' among all bonds:\n');
			msgArr.push(this.getBondsDescription());
			log(msgArr);
		} // if verbose
		var doubleBondAtoms = doubleBond.getAtoms();
		var highestPriorityBonds = [];
		var highestConfigLevelConsidered = 0;
		var bondAtomNum, doubleBondAtom;
		for (bondAtomNum = 0; bondAtomNum < 2; bondAtomNum += 1) {
			doubleBondAtom = doubleBondAtoms[bondAtomNum];
			var orderedBonds = [];
			var configLevelConsidered =
					doubleBondAtom.prioritizeMyBonds(orderedBonds);
			if (isEmpty(orderedBonds)) {
				msgArr = [];
				var returnVal = '--';
				msgArr.push(startMsg);
				if (typeof configLevelConsidered === 'string') {
					msgArr.push('double bond has wavy bond; ');
					returnVal = configLevelConsidered;
				} else {
					msgArr.push('bonds could not be prioritized; ');
				} // if not stereocenter due to wavy bond
				msgArr.push('returning no E/Z configuration.\n');
				if (verbose) {
					log(msgArr);
				}
				return returnVal;
			} else {
				highestPriorityBonds.push(orderedBonds[0]);
				if (configLevelConsidered > highestConfigLevelConsidered) {
					highestConfigLevelConsidered = configLevelConsidered;
				} // if higher configuration level was considered
				if (verbose) {
					msgArr = [];
					msgArr.push(startMsg);
					msgArr.push('with');
					if (configLevelConsidered === 0) {
						msgArr.push('out');
					}
					msgArr.push(' consideration of ');
					msgArr.push(configLevelConsidered >= 1 ? 'E/Z ' : '');
					if (configLevelConsidered == 2) {
						msgArr.push('and R/S ');
					}
					msgArr.push('configuration, single bonds of atom ');
					msgArr.push(doubleBondAtom.toString());
					msgArr.push(' prioritized in order:\n');
					for (bondNum = 0; bondNum < orderedBonds.length;
							bondNum += 1) {
						msgArr.push(orderedBonds[bondNum].toString());
						msgArr.push('\n');
					} // for each bond
					log(msgArr);
				} // if verbose
			} // if bonds couldn't be ordered
		} // for each double bond atom
		var highestPriorityLigands = [];
		var ligandToDoubleBondAngles = [];
		for (bondAtomNum = 0; bondAtomNum < 2; bondAtomNum += 1) {
			doubleBondAtom = doubleBondAtoms[bondAtomNum];
			var otherDoubleBondAtom = doubleBondAtoms[1 - bondAtomNum];
			var doubleBondVector = getVector(doubleBondAtom.getPosn(),
					otherDoubleBondAtom.getPosn());
			var doubleBondAngleToXAxis = getAngle(doubleBondVector);
			var bondToLigand = highestPriorityBonds[bondAtomNum];
			var bondToLigandAtoms = bondToLigand.getAtoms();
			var ligand = bondToLigandAtoms[
					bondToLigandAtoms[0].getAtomNum() ===
						doubleBondAtom.getAtomNum() ? 1 : 0];
			highestPriorityLigands.push(ligand);
			var ligandVector = getVector(doubleBondAtom.getPosn(),
					ligand.getPosn());
			var ligandAngleToXAxis = getAngle(ligandVector);
			var ligandToDoubleBondAngle =
					anglesDiff(doubleBondAngleToXAxis,
						ligandAngleToXAxis);
			ligandToDoubleBondAngles.push(ligandToDoubleBondAngle);
			/* if (verbose) {
				log([startMsg, 'doubleBondAtom = ',
					doubleBondAtom.toString(), ', otherDoubleBondAtom = ',
					otherDoubleBondAtom, ', doubleBondAngleToXAxis = ',
					toDegrees(doubleBondAngleToXAxis),
					', bond to higher priority ligand = ',
					bondToLigand.toString(),
					', higher priority ligand = ', ligand.toString(),
					', ligandAngleToXAxis = ',
					toDegrees(ligandAngleToXAxis),
					', ligandToDoubleBondAngle = ',
					toDegrees(ligandToDoubleBondAngle), '\n']);
			} */
		} // for each double bond atom
		msgArr = [];
		msgArr.push(startMsg);
		msgArr.push('angle from double bond atom ');
		msgArr.push(doubleBondAtoms[0].toString());
		msgArr.push(' to its higher-priority ligand ');
		msgArr.push(highestPriorityLigands[0].toString());
		msgArr.push(' is ');
		msgArr.push(toDegrees(ligandToDoubleBondAngles[0]));
		msgArr.push(', and from double bond atom ');
		msgArr.push(doubleBondAtoms[1].toString());
		msgArr.push(' to its higher-priority ligand ');
		msgArr.push(highestPriorityLigands[1].toString());
		msgArr.push(' is ');
		msgArr.push(toDegrees(ligandToDoubleBondAngles[1]));
		var linearity0 = Math.PI - Math.abs(ligandToDoubleBondAngles[0]);
		var linearity1 = Math.PI - Math.abs(ligandToDoubleBondAngles[1]);
		var tolerance = toRadians(5);
		var geometry;
		if (linearity0 < tolerance || linearity1 < tolerance) {
			geometry = 'linear ligand, stereochemistry indeterminate';
			msgArr.push('; linear ligand, stereochemistry ' +
					'indeterminate.\n');
		} else {
			geometry = (signum(ligandToDoubleBondAngles[0]) ===
					signum(ligandToDoubleBondAngles[1]) ? 'E' : 'Z');
			msgArr.push('; signs of angles are ');
			msgArr.push(geometry === 'E' ? 'same' : 'different');
			msgArr.push(', so geometry is ');
			msgArr.push(geometry);
			msgArr.push('\n');
		} // if either angle to ligand is 180 deg.
		if (verbose) {
			log(msgArr);
		}
		doubleBondAtoms[0].setConfiguration(geometry);
		doubleBondAtoms[1].setConfiguration(geometry);
		return geometry;
	}; // getEZConfiguration()
	/* Sorts this atom's bonds in order of priorities of the attached atoms,
	 * storing them in the empty array provided as a parameter.  Returns
	 * whether geometry or configuration needed to be considered to
	 * differentiate any atoms' priorities. */
	this.prioritizeMyBonds = function (orderedBonds, excludeAtom) {
		var startMsgBld = new String.builder().
				append('prioritizeMyBonds for atom ').
				append(this.getElement()).append(this.getAtomNum()).
				append(': ');
		var startMsg = startMsgBld.toString();
		// make array of the atoms attached to each bond of this atom,
		// link each atom to its ligands in a hierarchical digraph,
		// and make table of bonds of this atom keyed by ligands
		var myDigraphMol = new CompMolecule(parentMol);
		var digraphAtom = myDigraphMol.getAtom(this.getAtomNum());
		var digraphAtomLigands = [];
		var bondsByLigandsTable = [];
		var bondNum;
		for (bondNum = 1; bondNum <= digraphAtom.getNumBonds(); bondNum += 1) {
			var digraphBond = digraphAtom.getBond(bondNum);
			if (digraphBond.getOrder() === 1) {
				var bondAtoms = digraphBond.getAtoms();
				if (digraphBond.getStereo() === WAVY &&
						bondAtoms[0] === digraphAtom) {
					return 'has wavy bond, no configuration';
				} // if wavy bond originates at this atom
				var chain = [digraphAtom];
				var ligand = bondAtoms[bondAtoms[0] === digraphAtom ? 1 : 0];
				if (!excludeAtom ||
						excludeAtom.getAtomNum() !== ligand.getAtomNum()) {
					var digraphLigand =
							new DigraphAtom(ligand, chain, myDigraphMol);
					/* if (verbose) {
						log([startMsg, 'hierarchical digraph of ' +
							'ligand ', ligand.getElement(),
							ligand.getAtomNum(), ' is: ',
							digraphLigand.toString(true), '\n']);
					} */
					digraphAtomLigands.push(digraphLigand);
					bondsByLigandsTable[ligand] = digraphBond;
				} // if not excluding an atom from consideration
			} // if bond is single
		} // for each bond
		var numGhostAtoms =
				myDigraphMol.getNumAtoms() - parentMol.getNumAtoms();
		/* if (numGhostAtoms > 0 && verbose) {
			log([startMsg, 'hierarchical digraph ' +
				'has ', numGhostAtoms, ' additional ghost atoms added.\n']);
		} */
		// sort the atoms by priority; if no difference is found among some,
		// try considering geometry, then configurations
		var sortedBonds = [];
		var configConsiderLevel = 0;
		for (roundNum = 0; roundNum < 4; roundNum += 1) {
			configConsiderLevel = roundNum;
			if (verbose) {
				log([startMsg, 'sorting ligands by deep priority, ',
					configConsiderLevel === 3 ?
						'considering E/Z, R/S, and pro-R/S configurations' :
					configConsiderLevel === 2 ?
						'considering E/Z and R/S configurations' :
					configConsiderLevel === 1 ?
						'considering E/Z configurations' :
					'disregarding stereochemistry for now', '\n']);
			}
			sortByDeepPriority(digraphAtomLigands, configConsiderLevel);
			sortedBonds = this.getMySortedBonds(digraphAtomLigands,
					bondsByLigandsTable, configConsiderLevel);
			if (!isEmpty(sortedBonds)) {
				if (verbose) {
					log([startMsg, 'successfully sorted ligands ' +
						'by deep priority ', configConsiderLevel === 2 ?
							'considering E/Z and R/S configurations' :
						configConsiderLevel === 1 ?
							'considering E/Z configurations' :
						'disregarding stereochemistry for now', '\n']);
				}
				break;
			} // if bonds were sorted successfully
			if (verbose) {
				log([startMsg, 'unable to sort ligands by deep ' +
					'priority ', configConsiderLevel === 2 ?
						'considering E/Z and R/S configurations' :
					configConsiderLevel === 1 ?
						'considering E/Z configurations' :
					'disregarding stereochemistry for now', '\n']);
			}
		} // if some ligands have same priority
		for (bondNum = 0; bondNum < sortedBonds.length; bondNum += 1) {
			orderedBonds.push(sortedBonds[bondNum]);
		} // for each bond
		return configConsiderLevel;
	}; // prioritizeMyBonds()
	/* From sorted ligands of this atom, returns the corresponding bonds in the
	 * same order.  Returns an empty array if any ligands have the same
	 * priority. */
	this.getMySortedBonds = function (digraphAtomLigands, bondsByLigandsTable,
			configConsiderLevel) {
		var msgArr = [],
			gmsbAtomNum;
		msgArr.push('getMySortedBonds: ligands of atom ');
		msgArr.push(this.getElement());
		msgArr.push(this.getAtomNum());
		msgArr.push(' were prioritized as follows:\n');
		var sortedBonds = [];
		for (gmsbAtomNum = 0; gmsbAtomNum < digraphAtomLigands.length;
				gmsbAtomNum += 1) {
			var thisDigraphAtom = digraphAtomLigands[gmsbAtomNum];
			var prevDigraphAtom = digraphAtomLigands[gmsbAtomNum - 1];
			if (gmsbAtomNum === 0 || deepPriorityDifference(thisDigraphAtom,
					prevDigraphAtom, configConsiderLevel) !== 0) {
				var thisAtom = thisDigraphAtom.getAtom();
				sortedBonds.push(bondsByLigandsTable[thisAtom]);
				msgArr.push(thisAtom.toString());
				msgArr.push('\n');
			} else if (gmsbAtomNum !== 0) {
				msgArr = msgArr.slice(0, 3);
				msgArr.push(': found same priority for:\n');
				msgArr.push(thisDigraphAtom.toString());
				msgArr.push('\nand:\n');
				msgArr.push(digraphAtomLigands[gmsbAtomNum - 1].toString());
				msgArr.push('\n');
				sortedBonds = [];
				break;
			} // if two atoms have same priority
		} // for each ligand
		if (verbose) {
			log(msgArr);
		}
		return sortedBonds;
	}; // getMySortedBonds()
	/* Using one stereobond, calculates the configuration of a stereocenter with
	 * either four bonds or three bonds and one lone pair. */
	this.calculateConfiguration = function (orderedBonds, stereoBond, doLog) {
		var startMsgBld = new String.builder().
				append('calculateConfiguration for atom ').
				append(this.getElement()).append(this.getAtomNum()).
				append(': ');
		var startMsg = startMsgBld.toString();
		var threeBonds = orderedBonds.length === 3;
		if (verbose && doLog) {
			log([startMsg, 'atom has ',
				threeBonds ? 'a lone pair and 3' : '4',
				' bonds; stereo bond is ', stereoBond.toString(),
				', orderedBonds.length = ', orderedBonds.length, '\n']);
		}
		var bondsAngles = [];
		var stereoBondPriority = 0;
		var msgArr = [startMsg];
		for (bondNum = 0; bondNum < orderedBonds.length; bondNum += 1) {
			var bond = orderedBonds[bondNum];
			var isStereoBond = stereoBond.getBondNum() === bond.getBondNum();
			if (isStereoBond) {
				stereoBondPriority = bondNum + 1;
			}
			if (threeBonds || !isStereoBond) {
				var bondAtoms = bond.getAtoms();
				var atom1 = bondAtoms[0];
				var atom2 = bondAtoms[1];
				var bondVector = (atom1.getAtomNum() === this.getAtomNum() ?
						getVector(atom1.getPosn(), atom2.getPosn()) :
						getVector(atom2.getPosn(), atom1.getPosn()));
				var bondAngleToXAxis = getAngle(bondVector);
				bondsAngles.push(bondAngleToXAxis);
				msgArr.push('for ');
				if (!isStereoBond) {
					msgArr.push('non');
				}
				msgArr.push('stereo bond ');
				msgArr.push(bond.toString());
				msgArr.push(' angle to x-axis is ');
				msgArr.push(toDegrees(bondAngleToXAxis));
				msgArr.push('\n');
			} // if this bond is used to determine configuration
		} // for each bond from highest to lowest priority
		if (verbose && doLog && msgArr.length > 1) {
			log(msgArr);
		}
		var angle1To2 = anglesDiff(bondsAngles[1], bondsAngles[0]);
		var angle2To3 = anglesDiff(bondsAngles[2], bondsAngles[1]);
		var direction1To2 = signum(angle1To2);
		var direction2To3 = signum(angle2To3);
		var direction = (angle1To2 === Math.PI ? direction2To3 :
				angle2To3 === Math.PI ? direction1To2 :
				direction1To2 === direction2To3 ? direction1To2 :
				Math.abs(angle2To3) - Math.abs(angle1To2) > 0 ?
					direction1To2 : -direction1To2);
		var isHashed = stereoBond.getStereo() === HASH;
		var evenStereoBondPriority = stereoBondPriority % 2 === 0;
		var asObserved = isHashed === evenStereoBondPriority;
		configuration = (direction > 0 === asObserved ? 'S' : 'R');
		var bondKind = (threeBonds ? '' : 'nonstereo ');
		if (verbose && doLog) {
			log([startMsg,
				'angle from highest-priority ',
				bondKind, 'bond to middle-priority ', bondKind,
				'bond is ', toDegrees(angle1To2),
				', from middle-priority ', bondKind, 'bond to ' +
				'lowest-priority ', bondKind, 'bond is ',
				toDegrees(angle2To3), ', direction of rotation is ',
				direction === 1 ? 'counter' : '', 'clockwise, ' +
				'stereo bond has priority ', stereoBondPriority,
				' and is ', isHashed ? 'hash' : 'bold',
				', therefore configuration is ', configuration]);
		}
		return configuration;
	}; // calculateConfiguration()
	/* Finds out if this atom is pro-R or pro-S due to an adjacent
	 * 'stereocenter'. */
	this.getProconfigurations = function () {
		var ligBondNum;
		if (proconfigurations !== 'unknown') {
			return proconfigurations;
		}
		proconfigurations = [];
		var startMsgBld = new String.builder().
				append('getProconfigurations for atom ').
				append(this.getElement()).append(this.getAtomNum()).
				append(': ');
		var startMsg = startMsgBld.toString();
		for (bondNum = 0; bondNum < bonds.length; bondNum += 1) {
			var bondToLigand = bonds[bondNum];
			if (bondToLigand.getOrder() === 1) {
				var bondAtoms = bondToLigand.getAtoms();
				var ligand = bondAtoms[bondAtoms[0] === this ? 1 : 0];
				var ligBonds = ligand.getBonds();
				if (ligBonds.length < 3) {
					continue;
				}
				var ligLigands = [];
				var ligBondsToLigands = [];
				var ligStereoBonds = [];
				var nextBond = false;
				for (ligBondNum = 0; ligBondNum < ligBonds.length;
						ligBondNum += 1) {
					var ligBond = ligBonds[ligBondNum];
					var ligBondAtoms = ligBond.getAtoms();
					var ligOtherAtom =
							ligBondAtoms[ligBondAtoms[0] === ligand ? 1 : 0];
					if (ligOtherAtom !== this) {
						if (ligBond.getOrder() > 1 ||
								ligBond.getStereo() === WAVY) {
							nextBond = true;
							break;
						} // if this ligand can't be prostereogenic
						ligLigands.push(ligOtherAtom);
						ligBondsToLigands.push(ligBond);
						if (ligBond.getStereo() !== PLAIN &&
								ligBondAtoms[0] === ligand) {
							ligStereoBonds.push(ligBond);
						} // if the bond is stereogenic
					} // if the ligand's ligand is not this atom
				} // for each bond of the ligand
				if (nextBond) {
					continue;
				}
				var ligandECounts = ligand.getECounts();
				var mayBeProstereo = 0;
				var numStereoBonds = ligStereoBonds.length;
				if ((ligLigands.length === 3 &&
							arraysMatch(ligandECounts, [0, 0]) &&
							numStereoBonds > 0) ||
						(ligLigands.length === 2 &&
							arraysMatch(ligandECounts, [2, 0]) &&
							numStereoBonds > 0)) {
					mayBeProstereo = ligLigands.length;
				} // if ligand may be prostereogenic
				if (mayBeProstereo > 0 && numStereoBonds <= 2) {
					var orderedBonds = [];
					ligand.prioritizeMyBonds(orderedBonds, this);
					if (!isEmpty(orderedBonds)) {
						orderedBonds.push(bondToLigand);
						if (numStereoBonds === 1) {
							var proconfiguration =
									ligand.calculateConfiguration(
										orderedBonds, ligStereoBonds[0], false);
							if (['R', 'S'].contains(proconfiguration)) {
								if (verbose) {
									log([startMsg, 'calculated ' +
										'proconfiguration to be pro-',
										proconfiguration,
										' with respect to ligand ',
										ligand.getElement(),
										ligand.getAtomNum(), '.\n']);
								}
								this.setProconfiguration(ligand.getAtomNum(),
										'pro-' + proconfiguration);
							} // if configuration has been calculated
						} else if (numStereoBonds === 2) {
							if (verbose) {
								log([startMsg, 'ligand has two ' +
									'stereobonds; algorithm not yet ' +
									'written.\n']);
							}
						} // if number of stereo bonds
					} // if remaining bonds could be prioritized
				} // if ligand may be prostereogenic
			} // if the ligand is attached by a single bond
		} // for each bond
		return proconfigurations;
	}; // getProconfigurations()
	/* log methods */
	this.getBondsDescription = function () {
		var bld = new String.builder();
		var myBondNum;
		for (myBondNum = 0; myBondNum < bonds.length; myBondNum += 1) {
			bld.append('Bond ').append(myBondNum + 1).append(': ').
					append(bonds[myBondNum].toString()).append('\n');
		} // for each bond
		return bld.toString();
	}; // getBondsDescription()
	this.toString = function () {
		var bld = new String.builder().
				append(element).append(parentMol.getNumOfAtom(this));
		if (charge !== 0) {
			bld.append('[');
			if (charge > 0) {
				bld.append('+');
			}
			bld.append(charge).append(']');
		} // if charge
		if (!arraysMatch(eCounts, [0, 0])) {
			bld.append(':[').append(eCounts).append(']');
		} // if there are electrons on this atom
		return bld.toString();
	}; // toString()
} // CompAtom

// A CompAtom with a chain of linkages back to an atom whose configuration is
// being determined to its ligands (some of which may be "ghost atoms").
// Can't make it a subclass of CompAtom because we compare
// the parent CompAtom object to another CompAtom object, so we need to
// distinguish between the CompAtom and the DigraphAtom.
function DigraphAtom(theAtom, existingChain, theParentMol) {
	"use strict";
	/* these are the members */
	var atom = theAtom; // a CompAtom
	var chain = []; // the chain of atoms from this one back to the one whose
	// configuration is being determined
	var ligands = [];
	var parentMol = theParentMol;
	/* simple methods */
	this.getAtom = function () { return atom; };
	this.getPosn = function () { return atom.getPosn(); };
	this.getAtomicNumber = function () { return atom.getAtomicNumber(); };
	this.getChain = function () { return chain; };
	this.getLigands = function () { return ligands; };
	this.posnInChain = function (atom) { return chain.indexOf(atom); };
	this.isInChain = function (atom) { return this.posnInChain(atom) >= 0; };
	this.isLastInChain = function (atom) {
		return this.posnInChain(atom) === chain.length - 1;
	}; // isLastInChain()
	this.toString = function (recursive) {
		var bld = new String.builder().
				append(atom.toString()).append('; chain [').
				append(chain).append(']'),
			ligNum;
		if (recursive) {
			bld.append(', ligands [');
			for (ligNum = 0; ligNum < ligands.length; ligNum += 1) {
				bld.append(ligands[ligNum].toString(true));
			} // for each ligand
			bld.append(']\n');
		} // if recursive
		return bld.toString();
	}; // toString()
	/* constructor */
	chain = existingChain.slice(0);
	for (bondNum = 1; bondNum <= atom.getNumBonds(); bondNum += 1) {
		var bond = atom.getBond(bondNum);
		var bondAtoms = bond.getAtoms();
		var ligand = bondAtoms[bondAtoms[0] === atom ? 1 : 0];
		var ghostAtom, ligandChain;
		if (!this.isLastInChain(ligand)) { // not previous atom in chain
			ligandChain = chain.slice(0); // makes copy
			ligandChain.push(atom);
			if (this.isInChain(ligand)) {
				ghostAtom = new CompAtom(ligand, parentMol);
				parentMol.addAtom(ghostAtom);
				ligands.push(
						new DigraphAtom(ghostAtom, ligandChain, parentMol));
			} else {
				ligands.push(new DigraphAtom(ligand, ligandChain, parentMol));
			} // if ligand is earlier in chain
		} // if the ligand is not the previous one
		var order = bond.getOrder();
		for (orderNum = 2; orderNum <= order; orderNum += 1) {
			ghostAtom = new CompAtom(ligand, parentMol);
			parentMol.addAtom(ghostAtom);
			ligandChain = chain.slice(0); // makes copy
			ligandChain.push(atom);
			ligands.push(
					new DigraphAtom(ghostAtom, ligandChain, parentMol));
		} // for each pi bond
	} // for each bond
	/* if (verbose) {
		var msgArr = ['created DigraphAtom for ', atom.getElement(),
				atom.getAtomNum(), ' with ', atom.getNumBonds(),
				' bond(s) and chain \n'];
		for (linkNum = 0; linkNum < chain.length; linkNum += 1) {
			if (linkNum > 0) {
				msgArr.push(', ');
			}
			msgArr.push(chain[linkNum].toString());
		} // for each atom in chain
		if (ligands.length >= 1) {
			msgArr.push(' and ligand(s) ');
			for (ligNum = 0; ligNum < ligands.length; ligNum += 1) {
				if (ligNum > 0) {
					msgArr.push(', ');
				}
				msgArr.push(ligands[ligNum].toString());
			} // for each ligand
		} else {
			msgArr.push(' and no ligands.');
		} // if number of ligands
		msgArr.push('\n');
		log(msgArr);
	} // if verbose */
} // DigraphAtom

function CompMolecule(lewisMol) {
	"use strict";
	/* these are the members */
	var atoms = [],
		bonds = [],
		atomNum,
		origBond,
		origBondAtoms,
		newBondAtoms,
		atNum,
		origStereo,
		newBond,
		bondNum;
	/* constructor */
	for (atomNum = 1; atomNum <= lewisMol.getNumAtoms(); atomNum += 1) {
		atoms.push(new CompAtom(lewisMol.getAtom(atomNum), this));
	} // for each atom
	for (bondNum = 1; bondNum <= lewisMol.getNumBonds(); bondNum += 1) {
		origBond = lewisMol.getBond(bondNum);
		origBondAtoms = origBond.getAtoms();
		newBondAtoms = [];
		for (atNum = 0; atNum < 2; atNum += 1) {
			atomNum = lewisMol.getNumOfAtom(origBondAtoms[atNum]);
			newBondAtoms.push(atoms[atomNum - 1]);
		} // for each atom in bond
		origStereo = origBond.getStereo();
		if (typeof origStereo === 'string') {
			origStereo = STEREOS.indexOf(origStereo);
		} // if typeof stereo
		newBond = new CompBond(newBondAtoms, origBond.getOrder(),
				origStereo, this);
		bonds.push(newBond);
		for (atNum = 0; atNum < 2; atNum += 1) {
			newBondAtoms[atNum].addBond(newBond);
		} // for each atom in bond
	} // for each bond
	/* simple get methods */
	this.getNumAtoms = function () {
		return atoms.length;
	};
	this.getNumBonds = function () {
		return bonds.length;
	};
	this.getAtom = function (atomNumber) {
		return atoms[atomNumber - 1]; // 1-based
	};
	this.getBond = function (bondNum) {
		return bonds[bondNum - 1];
	}; // 1-based
	this.getNumOfAtom = function (atom) {
		return atoms.indexOf(atom) + 1;
	}; // 1-based
	this.getNumOfBond = function (bond) {
		return bonds.indexOf(bond) + 1;
	}; // 1-based
	this.getTotalCharge = function () {
		var charge = 0;
		for (atomNum = 1; atomNum <= this.getNumAtoms(); atomNum += 1) {
			charge += this.getAtom(atomNum).getCharge();
		} // for each atom
		return charge;
	}; // getTotalCharge()
	this.getECounts = function () {
		var eCounts = [0, 0];
		for (atomNum = 1; atomNum <= this.getNumAtoms(); atomNum += 1) {
			atomECounts = this.getAtom(atomNum).getECounts();
			eCounts[0] += atomECounts[0];
			eCounts[1] += atomECounts[1];
		} // for each atom
		return eCounts;
	}; // getECounts()
	this.displayConfigurations = function () {
		var atom, config, msgArr;
		for (atomNum = 1; atomNum <= this.getNumAtoms(); atomNum += 1) {
			atom = this.getAtom(atomNum);
			config = atom.getConfiguration(ALWAYS_CALCULATE);
			if (config !== '--') {
				msgArr = [];
				msgArr.push('Atom ');
				msgArr.push(atom.getElement());
				msgArr.push(atomNum);
				msgArr.push(': ');
				msgArr.push(config);
				msgArr.push('\n');
				log(msgArr);
			} // if the atom has a nonnull configuration
		} // for each atom
	}; // displayConfigurations()
	this.addAtom = function (atom) { atoms.push(atom); };
	/* matching method */
	this.matches = function (compMol) {
		var startMsg = '\nCompMolecule: ',
			numAtoms = this.getNumAtoms(),
			numBonds = this.getNumBonds(),
			propertyNotMatching = (numAtoms !== compMol.getNumAtoms() ?
				'numbers of atoms' : numBonds !== compMol.getNumBonds() ?
				'numbers of bonds' : this.getTotalCharge() !==
					compMol.getTotalCharge() ?
				'total charges' : !arraysMatch(this.getECounts(),
					compMol.getECounts()) ?
				'number of paired or unpaired electrons' : ''),
			compAtomNum,
			compAtom,
			myAtom,
			foundMatch;
		if (propertyNotMatching !== '') {
			if (verbose) {
				log([startMsg, 'Molecules have different ',
					propertyNotMatching, '; no match.']);
			}
			return false;
		} // if overall counts don't match
		if (verbose) {
			log([startMsg, '\nMolecule 1:\n', this.toString(),
				'Molecule 2:\n', compMol.toString()]);
		}
		myAtom = this.getAtom(1);
		foundMatch = NONE;
		for (compAtomNum = 1; compAtomNum <= numAtoms; compAtomNum += 1) {
			compAtom = compMol.getAtom(compAtomNum);
			if (verbose) {
				log([startMsg, 'Comparing mol1 atom ',
					myAtom.toString(), ' with ', myAtom.getBonds().length,
					' bond', myAtom.getBonds().length !== 1 ? 's' : '',
					' and mol2 atom ', compAtom.toString(), ' with ',
					compAtom.getBonds().length, ' bond',
					compAtom.getBonds().length !== 1 ? 's' : '', '.']);
			}
			if (myAtom.matches(compAtom, 0)) {
				foundMatch = compAtomNum;
				if (verbose) {
					log([startMsg, 'mol1 atom ', myAtom.toString(),
						' matches mol2 atom ', compAtom.toString(),
						', and molecules match!']);
				}
				break;
			} // if atoms match
		} // for each atom in this
		if (foundMatch === NONE && verbose) {
			log([startMsg, 'no matches to mol1 atom ', myAtom.toString(),
					' found; molecules don\'t match.']);
		} // if no match and verbose
		return foundMatch !== NONE;
	}; // matches()
	/* debugging output methods */
	this.toString = function () {
		var bld = new String.builder(),
			tsatomNum,
			atom,
			bondsOfAtom,
			tsbondNum;
		for (tsatomNum = 1; tsatomNum <= atoms.length; tsatomNum += 1) {
			atom = this.getAtom(tsatomNum);
			bondsOfAtom = atom.getBonds();
			bld.append('Atom ').append(atom.toString()).append(' has ').
					append(isEmpty(bondsOfAtom) ? 'no' : bondsOfAtom.length).
					append(' bond');
			if (isEmpty(bondsOfAtom) || bondsOfAtom.length != 1) {
				bld.append('s');
			}
			bld.append('.\n');
		} // for each atom
		for (tsbondNum = 1; tsbondNum <= bonds.length; tsbondNum += 1) {
			bld.append('Bond ').append(tsbondNum).append(': ').
					append(bonds[tsbondNum - 1].toString()).append('\n');
		} // for each bond
		return bld.toString();
	}; // toString()
} // CompMolecule

function LewisMatcher() {
	"use strict";
	var compMols = [];
	this.setCompMols = function (mol1, mol2) {
		compMols = [this.buildCompMolecule(mol1),
				this.buildCompMolecule(mol2)];
	}; // setCompMols()
	this.setCompMol = function (mol, num) {
		compMols[num - 1] = this.buildCompMolecule(mol);
	}; // setCompMol()
	this.buildCompMolecule = function (mol) {
		var lmol = (typeof mol === 'string' ?
				getParsedLewisMol(trim(mol)) : mol);
		return new CompMolecule(lmol);
	}; // buildCompMolecule()
	this.match = function () {
		var isMatch = compMols[0].matches(compMols[1]);
		if (verbose && !chopped) {
			alert(logBld.toString());
			logBld.clear();
		} // if verbose
		return isMatch;
	}; // match()
	this.setVerbosity = function (num) {
		verbose = num > 0;
		chopped = num > 1;
	}; // setVerbosity()
	this.displayConfigurations = function () {
		compMols[0].displayConfigurations();
		alert(logBld.toString());
		logBld.clear();
	}; // displayConfigurations()
} // LewisMatcher

// --> end HTML comment
