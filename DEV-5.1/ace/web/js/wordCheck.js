// <!-- avoid parsing the following as HTML
/* Checks whether a statement contains unknown words.
 * Requires that the including page contain
 * elements with IDs 'stmt1' and 'mirror1' (or other numbers)
 * whose inner HTML can be replaced.  Another element called
 * 'wordList' is optional.
 * Requires that the including page use stylesheet /includes/epoch.css.
 */

/*jsl:option explicit*/
/*jsl:import jslib.js*/

var goodWords = [];
var addlWordsStr = '';

/* The list of acceptable words. */
var words = [
	'h', 'he', 'li', 'be', 'b', 'c', 'n', 'o', 'f', 'ne',
	'na', 'mg', 'al', 'si', 'p', 's', 'cl', 'ar',
	'k', 'ca', 'sc', 'ti', 'v', 'cr', 'mn', 'fe', 'co', 'ni', 'cu', 'zn',
	'ga', 'ge', 'as', 'se', 'br', 'kr',
	'rb', 'sr', 'y', 'zr', 'nb', 'mo', 'tc', 'ru', 'rh', 'pd', 'ag', 'cd',
	'in', 'sn', 'sb', 'te', 'i', 'xe',
	'cs', 'ba', 'lu', 'hf', 'ta', 'w', 're', 'os', 'ir', 'pt', 'au', 'hg',
	'tl', 'pb', 'bi', 'po', 'rn', 'fr', 'ra', 'ac', 'th', 'pa', 'u',
	'me', 'et', 'ph', 'pr', 'npr', 'n-pr', 'ipr', 'i-pr',
	'bu', 'nbu', 'n-bu', 'ibu', 'i-bu', 'sbu', 's-bu', 'tbu', 't-bu',
	'a', 'am',
	'abbreviated', 'about', 'above', 'absolute', 'absorb', 'absorbed',
	'absorbing', 'absorbs', 'acceptor', 'accompanies', 'accuracy',
	'accurate', 'accurately', 'acetate', 'acetic', 'acetone', 'acid',
	'acids', 'across', 'activated', 'activation', 'activity',
	'actual', 'actually', 'add', 'added', 'adding', 'addition',
	'additional', 'additions', 'adjacent', 'adsorption',
	'affinity', 'after', 'air', 'alcohol', 'alder',
	'aldehyde', 'aldehydes', 'alkali', 'alkaline', 'alkane',
	'alkene', 'alkyne', 'all', 'allotropes', 'allow',
	'alloy', 'alpha', 'also', 'altered', 'although',
	'always', 'amine', 'amines', 'amino', 'ammonia', 'amount',
	'amounts', 'amphiprotic', 'amphoteric', 'an', 'analyses',
	'analysis', 'analytic', 'analytical', 'and',
	'anhydride', 'anode',
	'anomer', 'anomeric', 'anomers', 'another', 'any', 'anymore',
	'anything', 'anyway', 'anywhere', 'apart', 'apparent',
	'apparently', 'aqueous', 'are', 'area', 'areas',
	'arise', 'around', 'arrange', 'arrangement', 'art',
	'assumes', 'assuming', 'assumption',
	'asymmetric', 'asymmetrical', 'at',
	'atm', 'atmosphere', 'atom', 'atomic', 'atoms',
	'attach', 'attached', 'attract', 'attracted', 'aufbau',
	'average', 'axis', 'back', 'backward', 'base', 'bases',
	'basic', 'bathroom', 'battery', 'bead',
	'because', 'become', 'becomes', 'been', 'before',
	'behave', 'being', 'believe', 'below', 'beta',
	'between', 'bidentate', 'big', 'bigger', 'biggest',
	'binary', 'bind', 'binds', 'binding',
	'biological', 'bit', 'bitter', 'blackbody', 'blob',
	'block', 'blocks', 'body', 'boil', 'boiling',
	'bond', 'bonded', 'bonding', 'bonds', 'both',
	'bottle', 'bottom', 'bound', 'branch', 'break', 'breaking',
	'breaks', 'bronsted', 'lowry', 'brother', 'buffer', 'build',
	'building', 'bunch', 'bunsen', 'burn', 'burner',
	'burns', 'but', 'butter', 'butyl', 'butenyl', 'butynyl',
	'by', 'calcium',
	'called', 'calorimetry', 'can', 'cao', 'capacity',
	'carbanion', 'carbanions', 'carbenium', 'carbocation', 'carbocations',
	'carbon', 'carbon-12', 'carbonates', 'carbon-carbon', 'carbonyl',
	'carboxylic', 'careful', 'cars', 'cas', 'catalyst',
	'catalysts', 'catalyzes', 'cathode', 'cations', 'cause',
	'caused', 'cell', 'cells', 'cellular',
	'celsius', 'central', 'certain', 'chain', 'chamber',
	'change', 'changed', 'changes', 'changing', 'charge',
	'chart', 'check', 'chemical', 'chemically', 'chemicals',
	'chemistry', 'chiral', 'chirality', 'chloride', 'chromatography',
	'chunk', 'circuit', 'cis-', 'classes', 'close', 'closed',
	'coagulation', '-coh', 'cold', 'collects', 'colligative', 'colloid',
	'color', 'colorless', 'colors', 'column', 'combine',
	'combines', 'combustion', 'common', 'completely', 'complex',
	'components', 'compound', 'compounds', 'concentrated',
	'concentration', 'concentrations', 'condensation', 'conditions',
	'conduct', 'conductance', 'conductivity', 'conducts',
	'conformation', 'conformational', 'conformations',
	'conformer', 'conformers', 'conjugate',
	'conjugated', 'conservation', 'consists', 'constant', 'contain',
	'containing', 'contains', 'content', 'continuous', 'contraction',
	'convert', 'converted', '-cooh', 'cool', 'correct', 'could',
	'cousin', 'covalent', 'crafts',
	'creatures', 'critical', 'crystal',
	'd', 'dealing', 'decay', 'decomposition', 'defect',
	'definition', 'degenerate', 'degree', 'degrees', 'delocalization',
	'delocalized', 'denature', 'denatured', 'depends', 'der',
	'describe', 'described', 'describes', 'destroy', 'determined',
	'determining', 'diagram', 'diastereoisomer', 'diastereoisomeric',
	'diastereoisomerically', 'diastereoisomerism',
	'diastereoisomers', 'diastereotopic', 'diastereotopicity',
	'diastereomer', 'diastereomeric', 'diastereomerically', 'diastereomers',
	'diels', 'difference', 'different', 'differing',
	'diffusion', 'digits', 'dilution', 'dimethyl', 'dioxide',
	'dipole', 'dipole-dipole', 'dipoles', 'direction', 'directions',
	'directly', 'dispersion', 'dissociate', 'dissociates', 'dissociation',
	'dissolve', 'dissolved', 'dissolves', 'dissolving', 'distance',
	'distillation', 'distributed', 'disturb', 'divided', 'do',
	'does', 'donate', 'done', 'donor', 'double', 'double-displacement',
	'down', 'drawn', 'dressing', 'drops', 'dry',
	'due', 'during', 'e', 'each', 'earth',
	'effect', 'effusion', 'either', 'electricity', 'electrode',
	'electrolysis', 'electrolyte', 'electrolytes', 'electron',
	'electronegative', 'electronegativities', 'electronegativity',
	'electrophile', 'electrophiles', 'electrophilic', 'electrophilicity',
	'electron-pair', 'electrons', 'electropositive', 'element',
	'elements', 'eliminate', 'eliminated',
	'eliminates', 'eliminating', 'elimination', 'eliminations',
	'emission', 'empirical', 'emulsion',
	'enantioisomerism', 'enantiotopic', 'enantiotopicity',
	'enantiomer', 'enantiomeric', 'enantiomerically',
	'enantiomerism', 'enantiomers',
	'end', 'endothermic', 'endpoint', 'energy', 'enough',
	'enthalpy', 'entirely', 'entropy', 'enzyme', 'enzymes',
	'epimer', 'epimeric', 'epimers',
	'equal', 'equally', 'equation', 'equations', 'equilibrium',
	'equivalence', 'equivalent', 'equivalents', 'ester', 'etc',
	'ethyl', 'ethylene', 'ethenyl', 'ethynyl',
	'evaporates', 'eventually', 'ever', 'everything', 'exact',
	'example', 'examples', 'except', 'excess', 'excited',
	'excluding', 'exclusion', 'exist', 'exists', 'exothermic',
	'explain', 'explains', 'exploiting', 'expression', 'extra',
	'fact', 'fall', 'falling', 'falls', 'familiar',
	'families', 'family', 'famous', 'fancy', 'far',
	'farther', 'fast', 'faster', 'fat', 'feet',
	'figure', 'figures', 'figuring', 'find', 'finding',
	'first', 'fission', 'flammability', 'flavor', 'flow',
	'follow', 'followed', 'following', 'follows',
	'for', 'f-orbitals', 'force', 'forced', 'forces', 'forcing',
	'form', 'formed', 'forms', 'formula', 'forward',
	'fraction', 'free', 'free-radical', 'freezing', 'frequently',
	'friedel',
	'from', 'fully', 'function', 'functional', 'functionality', 'fusion',
	'gain', 'gains', 'gamma', 'gas', 'gaseous',
	'gases', 'gasoline', 'general', 'generalizations', 'generally',
	'generic', 'geometrical', 'get', 'gets', 'gibbs',
	'give', 'given', 'gives', 'go', 'goes',
	'going', 'good', 'got', 'grab', 'gram',
	'great', 'greater', 'ground', 'group', 'groups',
	'guess', 'guy', 'h+', 'h2so4',
	'had', 'half', 'half-life', 'half-reaction', 'halogen', 'halide',
	'hand', 'hands', 'handy', 'happen', 'happening',
	'happens', 'happy', 'happier', 'happiest',
	'hard', 'harder', 'hardest', 'has', 'have', 'having',
	'heat', 'heating', 'height', 'held', 'helium',
	'heavy', 'heavier', 'heaviest',
	'heterogeneous', 'high', 'higher',
	'hindrance', 'hint', 'his', 'hold', 'holds',
	'homo', 'homochiral', 'homochirality',
	'homogeneous', 'homogeneity', 'homotopic', 'homotopicity',
	'hot', 'how', 'however', 'human', 'hybrid',
	'hydrate', 'hydrated', 'hydrates', 'hydrating', 'hydration',
	'hydride', 'hydroborate', 'hydroborated',
	'hydroborates', 'hydroborating', 'hydroboration',
	'hydrocarbon', 'hydrogen', 'hydrogenate', 'hydrogenated',
	'hydrogenates', 'hydrogenating', 'hydrogenation',
	'hydrohalogenate', 'hydrohalogenated',
	'hydrohalogenates', 'hydrohalogenating', 'hydrohalogenation',
	'hydronium', 'hydroxide', 'hypophosphorous',
	'ice', 'idea', 'ideal', 'if',
	'ignore', 'ii', 'image', 'images', 'imagine',
	'imine', 'imines',
	'immiscible', 'imply', 'impossible', 'inch',
	'inches', 'include', 'increase', 'increasing', 'indicates',
	'indicator', 'individual', 'induced', 'infinitely', 'info',
	'information', 'informs', 'inhibitor', 'inner', 'inorganic',
	'insoluble', 'instant', 'intents', 'interchangable', 'intermediate',
	'intermolecular', 'into', 'inversely', 'involved', 'involves',
	'ion', 'ionic', 'ionization', 'ions', 'ir',
	'irreversible', 'is', 'isomer', 'isomeric', 'isomerically',
	'isomerism', 'isomers',
	'isotonic', 'isotope', 'isotopes', 'isotopic', 'it', 'its',
	'itself', 'join', 'joins', 'joined', 'joining', 'jump', 'just',
	'k', 'kcal', 'ka', 'kelvin', 'kelvins', 'ketone',
	'kids', 'kilogram', 'kind', 'kinetic', 'kj', 'known',
	'ksp', 'l', 'lab', 'lanthanide', 'lanthanides',
	'large', 'larger', 'largest', 'lattice', 'law', 'le', 'least',
	'left', 'length', 'less', 'letting', 'level',
	'levels', 'lewis', 'life', 'ligand', 'ligands',
	'light', 'like', 'likes', 'limited', 'limiting',
	'lighter', 'lightest', 'leftmost',
	'line', 'lines', 'liquid', 'liquids', 'liquid-vapor',
	'liter', 'liters', 'litmus', 'little', 'live',
	'living', 'loads', 'location', 'london',
	'lone', 'long-lived', 'looks', 'lose', 'loses',
	'lost', 'lot', 'lots', 'low', 'lower',
	'lowest', 'lumo', 'm', 'made', 'magnesium', 'main',
	'main-block', 'make', 'makes', 'making', 'many',
	'mass', 'masses', 'material', 'math', 'mathematical',
	'matter', 'maximum', 'mean', 'means',
	'measure', 'measured', 'measurement', 'measures', 'mechanics',
	'mechanism', 'mechanisms', 'melting', 'membrane', 'metal',
	'metalloids', 'metals', 'methyl', 'methylene', 'methine',
	'might', 'migrate', 'minimum',
	'minute', 'mirror', 'mix', 'mixed', 'mixing',
	'mixture', 'mobile', 'molality', 'molar', 'molarity',
	'mol', 'mole', 'molecular', 'molecule', 'molecules', 'moles',
	'moment', 'momentary', 'monatomic', 'monoxide', 'more',
	'most', 'move', 'movement', 'moves', 'moving', 'ms',
	'much', 'mud', 'multiple', 'name', 'near',
	'nearest', 'need', 'needed', 'needless',
	'negative', 'neutral', 'neutralization', 'neutralizing', 'neutron',
	'neutrons', 'never', 'next', 'nice', 'nitrogen',
	'nmr', 'no', 'noble', 'node', 'none', 'nonpolar',
	'nonsuperimposable', 'normal', 'normality', 'not',
	'nuclear', 'nuclei', 'nucleic', 'nucleon', 'nucleus', 'number',
	'nucleophile', 'nucleophiles', 'nucleophilic', 'nucleophilicity',
	'numbers', 'object', 'occur', 'occurs',
	'octet', 'of', 'off', 'oh', 'oh',
	'oil', 'on', 'one', 'ones', 'only',
	'open', 'opening', 'optical', 'or', 'orbital',
	'orbitals', 'order', 'organic', 'osmosis', 'osmotic',
	'other', 'others', 'out', 'outer', 'outermost',
	'outside', 'over', 'overlap', 'overlapped', 'overlapping',
	'overlaps', 'oxidation', 'oxide', 'oxidize', 'oxidized',
	'oxidizes', 'oxidizing', 'oxime', 'oximes', 'oxygen',
	'p', 'pair', 'pairs', 'paper',
	'part', 'partial', 'partially', 'particle', 'particles',
	'parts', 'past', 'path', 'pauli', 'peanut', 'pentyl',
	'people', 'per', 'percent', 'perfectly', 'period',
	'periodic', 'ph', 'phase', 'phenolphthalein',
	'phenyl', 'phosphoric', 'phosphorous',
	'phosphorus', 'physical', 'pi-bond', 'place', 'places',
	'plane', 'plants', 'plastics', 'point', 'polar', 'polarity',
	'polarities', 'polarized', 'polyatomic', 'polymer', 'polymers',
	'polyprotic', 'poorly', 'possess', 'position', 'positions',
	'positive', 'possibility', 'possible', 'potential', 'pound',
	'pounds', 'power', 'precise', 'precision', 'predicting',
	'prelude', 'present', 'pressure', 'pressures', 'pretty',
	'primary',
	'principle', 'probability', 'probably', 'process', 'product',
	'products', 'properties', 'property', 'proportional',
	'propyl', 'propenyl', 'propynyl', 'protein',
	'proteins', 'proton', 'protons', 'provide', 'pull',
	'pulled', 'pure', 'purposes', 'put',
	'quantity', 'quantum', 'quaternary',
	'radical', 'radioactive', 'radius',
	'rainbow', 'raji', 'random', 'randomness', 'rate',
	'rather', 'ratio', 'ray', 'rcoor', 'rcor', 'rco2r', 'rcho',
	'react', 'reaction', 'reactions', 'reactive', 'reading',
	'readings', 'reagent', 'reagents', 'real', 'reality',
	'really', 'rearrange', 'rearranged', 'rearrangement',
	'rearrangements', 'rearranges', 'rearranging',
	'recipe', 'record', 'red', 'redox',
	'reduce', 'reduced', 'reduction', 'referred', 'reform',
	'reforms', 'relationship', 'relative', 'released', 'remember',
	'repeatable', 'repeated', 'repeating', 'repel', 'replaced',
	'replacement', 'replaces', 'repulsion', 'required', 'resists',
	'resonance', 'result',
	'retrosyntheses', 'retrosynthesis', 'retrosynthetic',
	'reverse', 'reversible', 'right',
	'rightmost', 'ring', 'rms', 'room', 'root',
	'rotate', 'row', 'rules', 's-', 'said',
	'salad', 'salt', 'same', 'sample', 'saturated',
	'say', 'saying', 'says', 'scale', 'second', 'secondary',
	'see', 'semiconductor', 'semiconductors', 'semi-permeable', 'sense',
	'separate', 'separation', 'sequence', 'series', 'set',
	'settle', 'seven', 'shaken', 'shapes', 'share',
	'shared', 'shielding', 'short', 'short-lived', 'should',
	'shower', 'showing', 'shown', 'shows', 'side',
	'sigma', 'significant', 'simplest', 'simply', 'single',
	'single-displacement', 'six', 'size', 'slew', 'slippery',
	'slower', 'slowest', 'slows', 'small', 'smaller',
	'smallest', 'smell', 'smooth', 'so', 'so3',
	'sodium', 'soft', 'softer', 'softest',
	'solid', 'solids', 'solubility', 'soluble',
	'solute', 'solutes', 'solution', 'solutions', 'solvent',
	'solvate', 'solvated', 'solvation',
	'some', 'something', 'sometimes', 'sounds', 'spark',
	'specific', 'spectator', 'spectrum', 'speed', 'speeds',
	'spontaneous', 'square', 'squares', 'stable', 'standard',
	'start', 'started', 'state', 'states', 'stationary',
	'steal', 'step', 'step-by-step', 'steric', 'stick',
	'sticking', 'sticks', 'still', 'stir', 'stoichiometry',
	'stop', 'stp', 'straight', 'strong', 'stronger',
	'structural', 'structure', 'structures', 'stuck', 'study',
	'stuff', 'stupid', 'sublimation', 'subscripts',
	'subsequent', 'substance', 'substances',
	'substituent', 'substituents', 'substitute', 'substituted',
	'substitutes', 'substituting', 'substitution', 'substitutions',
	'such', 'suggests', 'sulfuric', 'sum',
	'sums', 'supercooling', 'supercritical', 'supersaturated', 'surface',
	'surrounded', 'suspended', 'suspension', 'switch',
	'symmetric', 'symmetrical', 'syntheses', 'synthesis', 'synthetic',
	'system', 'table', 'take', 'takes', 'talk',
	'talking', 'tall', 'tell', 'temperature', 'temperatures',
	'tend', 'tendency', 'tends', 'tension', 'term', 'tertiary',
	'than', 'that', 'the', 'them', 'theoretical',
	'theoretically', 'theory', 'there', 'thermodynamics', 'thermonuclear',
	'these', 'they', 'thing', 'things', 'think',
	'third', 'this', 'though', 'thought', 'three',
	'three-dimensional', 'through', 'tightly', 'time', 'times',
	'titration', 'to', 'together', 'told', 'top',
	'total', 'trans-', 'transition', 'travel', 'traveling',
	'tries', 'triple', 'true', 'try', 'trying',
	'turn', 'turning', 'turns', 'tv', 'twelve',
	'twice', 'two', 'type', 'u', 'unbonded',
	'unclear', 'under', 'undergo', 'understand', 'unfortunately',
	'unit', 'units', 'universe', 'unless', 'unpaired',
	'unravels', 'unsaturated', 'unshared', 'unstable', 'up',
	'use', 'used', 'useful', 'usual', 'usually',
	'valence', 'valid', 'value', 'values', 'van',
	'vapor', 'vaporization', 'varies', 'various', 'velocities',
	'velocity', 'very', 'vibrate', 'volatile', 'voltaic',
	'volume', 'vsepr', 'waal', 'want', 'was',
	'water', 'wavelengths', 'way', 'ways', 'we',
	'weak', 'weigh', 'weight', 'weights', 'weird', 'well',
	'what', 'when', 'whenever', 'where', 'whether',
	'which', 'while', 'white', 'who', 'whole',
	'why', 'will', 'with', 'without', 'word',
	'words', 'work', 'works', 'world', 'would',
	'wrong', 'yet', 'yield', 'you', 'your',
	'yourself', 'zero'
];

/* Sets up an array of known words. */
function wordsInit(moreWordsStr) {
	"use strict";
	goodWords = [];
	if (moreWordsStr) {
		addlWordsStr = moreWordsStr;
	}
	var wordNum, addlWords;
	for (wordNum = 0; wordNum < words.length; wordNum += 1) {
		goodWords[words[wordNum]] = 1;
	}
	if (addlWordsStr.length > 0) {
		addlWords = addlWordsStr.split(/ +/);
		for (wordNum = 0; wordNum < addlWords.length; wordNum += 1) {
			goodWords[addlWords[wordNum]] = 1;
		}
	} // if there are additional words
} // wordsInit()

/* Shows the acceptable words. */
function getWords() {
	"use strict";
	var wordsStr = words.join(', ');
	if (addlWordsStr.length > 0) {
		wordsStr += ', ' + addlWordsStr.replace(/ +/g, ', ');
	} // if there are additional words
	return wordsStr;
} // getWords()

/* Returns the statement with unknown words highlighted in red and
 * struck through.  */
function checkText(stmt) {
	"use strict";
	var wordNum, theWord,
		noCERs = stmt.replace(/&[^,]*;/g, ''),
		splitVersion = noCERs.split(/[ \-]+/),
		newWords = [];
	for (wordNum = 0; wordNum < splitVersion.length; wordNum += 1) {
		theWord = splitVersion[wordNum].replace(/[^a-z]/ig, '');
		if (goodWords[theWord.toLowerCase()]) {
			newWords.push(theWord);
		} else if (!isWhiteSpace(theWord)) {
			newWords.push('<span class="unknownWord">' +
					theWord + '<\/span>');
		} // if the lower-case word is known
	} // each word
	return newWords.join(' ');
} // checkText()

/* Shows the user the phrase with unknown words highlighted in red and
 * struck through.  */
function updateText(stmtNum) {
	"use strict";
	var stmt = getValue('stmt' + stmtNum),
		checkedStmt = checkText(stmt);
	setInnerHTML('mirror' + stmtNum, checkedStmt);
} // updateText()

// --> end HTML comment
