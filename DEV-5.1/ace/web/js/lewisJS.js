/* Methods for drawing a Lewis structure.

CC BY Robert B. Grossman and Raphael Finkel.

Calling page must: 
		include ajax.js, jslib.js, wz_jsgraphics.js, svgGraphics.js, offsets.js, 
			position.js, xmlLib.js
		have div elements to set toolbars, drawing canvas, and options; names of
			toolbars and options should begin identically and end with Toolbars 
			and Options, respectively
		have an element whose ID is 'scrollableDiv' *if* clickable image in 
			calling page is embedded in a DIV that can be scrolled
			independently from the window (as in 
			authortool/evaluators/loadEvaluatorGeneric.jsp.h) 
Calling page *may* have element whose ID is 'lewisJSMsgs' (for debugging).
Calling page should call the following method upon body onload:

	function initLewis() {
		initLewisConstants(
				[<%= LewisMolecule.CANVAS_WIDTH %>, 
					<%= LewisMolecule.CANVAS_HEIGHT %>],
				<%= LewisMolecule.MARVIN_WIDTH %>, 
				['<%= LewisMolecule.PAIRED_ELECS %>',
					'<%= LewisMolecule.UNPAIRED_ELECS %>',
					'<%= LewisMolecule.UNSHARED_ELECS %>' ],
				'<%= LewisMolecule.LEWIS_PROPERTY %>',
				'<%= LewisMolecule.HIGHLIGHT %>',
				'Enter an element symbol.',
				'There is no such element.  Please try again.',
				'Other');
		initLewisGraphics('<%= pathToRoot %>', 
				'lewisJSCanvas', 
				'lewisJSToolbars');
		parseLewisMRV(mol);
	} // initLewis()

or, if the Java class LewisMolecule is not available:

		var CANVAS_WIDTH = 450; 
		var CANVAS_HEIGHT = 260;
		var MARVIN_WIDTH = 25;
		var PAIRED_ELECS = 'paired electrons';
		var UNPAIRED_ELECS = 'unpaired electrons';
		var UNSHARED_ELECS = 'unshared electrons';
		var LEWIS_PROPERTY = 'is Lewis Structure?';
		var HIGHLIGHT = 'highlighted';

		function init() {
			setHeight('lewisJSCanvas', '' + CANVAS_HEIGHT + 'px');
			setWidth('LewisSketch_canvas', '' + CANVAS_WIDTH + 'px');
			initLewisConstants(
					[CANVAS_WIDTH, CANVAS_HEIGHT],
					MARVIN_WIDTH, 
					[PAIRED_ELECS, UNPAIRED_ELECS, UNSHARED_ELECS],
					LEWIS_PROPERTY,
					HIGHLIGHT,
					'Enter an element symbol.',
					'There is no such element.  Please try again.',
					'Other');
			initLewisGraphics('../', 
					'lewisJSCanvas', 
					'lewisJSToolbars');
			parseLewisMRV(mol);
		} // init()

If displaying but not editing multiple Lewis JS structure images, calling 
page needs to include only jslib.js, offsets.js, and xmlLib.js and should 
call the following method upon body onload:

	function initLewis() {
		initLewisConstants(
				[<%= LewisMolecule.CANVAS_WIDTH %>, 
					<%= LewisMolecule.CANVAS_HEIGHT %>],
				<%= LewisMolecule.MARVIN_WIDTH %>, 
				['<%= LewisMolecule.PAIRED_ELECS %>',
					'<%= LewisMolecule.UNPAIRED_ELECS %>',
					'<%= LewisMolecule.UNSHARED_ELECS %>' ],
				'<%= LewisMolecule.LEWIS_PROPERTY %>',
				'<%= LewisMolecule.HIGHLIGHT %>');
		var figuresData = [];
		// code to get the Lewis JS figures to be displayed on this page,
		// e.g:
		<% for (int qNum = 1; qNum <= lightQs.length; qNum++) {
			final Question lightQ = lightQs[qNum - 1];
			if (lightQ.getNumFigures() > 0) {
				final Figure figure = lightQ.getFigure(1);
				if (figure.isLewis()) {
					final String[] figData = figure.getDisplayData();
		%>
					figuresData.push(['<%= Utils.toValidJS(
								figData[Figure.STRUCT]) %>',
							<%= lightQ.getQFlags() %>,
							<%= qNum %>]); // where cell ID is fig<%= qNum %>
		<%		} // if 1st figure is Lewis
			} // if the question has a figure
		} // for each question
		%>
		if (!isEmpty(figuresData)) {
			loadLewisInlineImages('<%= pathToRoot %>', figuresData); }
	} // initLewis()

*/

/*jsl:option explicit*/
/*jsl:import jslib.js*/
/*jsl:import wz_jsgraphics.js*/
/*jsl:import svgGraphics.js*/
/*jsl:import position.js*/
/*jsl:import xmlLib.js*/
/*jsl:import ajax.js*/
/*jsl:import openwindows.js*/

/* ******************* Constants & global variables *****************/

var CANVAS_DIMS; // dimensions of drawing canvas
var MARGINS = [[10, 20], [15, 15]]; // margin on inside of canvas so atoms 
				// and lone pairs don't exceed the boundary
var MARVIN_WIDTH; // dimensions of Marvin window, in internal Marvin units
var PAIRED_TITLE; // title of atom property
var UNPAIRED_TITLE; // title of atom property
var UNSHARED_TITLE; // title of atom property
var HIGHLIGHT; // value of atom property attributes
var LEWIS_PROPERTY; // value of molecule property attributes
var ENTER_ELEMENT; // possibly translated phrase
var NO_SUCH_ELEMENT; // possibly translated phrase
var OTHER_BUTTON_LABEL; // possibly translated phrase
var TASKS = [
		['H', 'C', 'N', 'O', 'F', 'Bond', 'Menu', 
			'IncreaseCharge', 'Add1Elec', 'Add2Elec', 'Cut', 'Move', 'Grow', 'Shrink'],
		['P', 'S', 'Cl', 'Br', 'I', 'Si', 'Other', '',
			'DecreaseCharge', 'Del1Elec', 'Del2Elec', 'Paste', 'New', 'Undo', 'Redo']
		]; // toolbar buttons representing different tasks
var C_BUTTON = TASKS[0][1]; // the C button
var BOND_BUTTON = TASKS[0][5]; // the bond button
var ANY_ELEM_ROW = 1; // 0-based row of the changeable element button
var ANY_ELEM_COL = 5; // 0-based column of the changeable element button
var BOLD = '_bold'; // ID suffix to distinguish a cell and the bold element within it
var BOND_LENGTH = 40; // typical bond length in pixels
var BOND_SPACING = 5; // spacing between lines in double and triple bonds
var WEDGE_WIDTH = 3; // width of the wide end of a wedge
var ATOM_BOUND = 10; // pixels to clear around an atom label
var MIN_BOND_LENGTH = 15; // minimum bond length
var STEREOS = ['Bond', 'BoldBond', 'HashBond', 'WavyBond']; // values for stereo
var STEREO_MRVS = ['', 'W', 'H', '']; // MRV values for bond stereochemistry
var BOND_TYPE_MENU = 'bondTypeMenu';
var PLAIN = 0; // member of stereo
var BOLD = 1; // member of stereo
var HASH = 2; // member of stereo
var WAVY = 3; // member of stereo
var SELECT_DISTANCE = 15; // how close a click needs to be to an object to select it
var ADD_TO_STACK = true; // whether to add the painted molecule to the stack
var CENTER = true; // whether to center the imported molecule
var ATOM = 0; // array member
var BOND = 1; // array member
var ATOM_NUM = 1; // array member
var NONE = -1; // value for movingAtomNum, nearest atom or bond
var BLACK = 'black'; // border, image color
var GRAY = '#d3d3d3'; // border, image color -- do not change 
		// without changing names of files in ./lewisImg/
var ATOM_COLORS = { 
		'H': '#333333',
		'C': '#000000',
		'N': '#333399',
		'O': '#FF0000',
		'F': '#996600',
		'P': '#996600',
		'S': '#996600',
		'Cl': '#009900',
		'Br': '#663333',
		'I': '#660099'
		};
var DEFAULT_ATOM_COLOR = '#666666'; // silver
var ELEMENT = 'elementType'; // MRV attribute name
var FORMAL_CHARGE = 'formalCharge'; // MRV attribute name
var COLOR_SET = 'atomSetRGB'; // MRV attribute name
var COLOR_NUM = 'mrvSetSeq'; // MRV attribute name
var X_COORD = 'x2'; // MRV attribute name
var Y_COORD = 'y2'; // MRV attribute name
var MDOCUMENT = 'MDocument'; // MRV attribute name
var ATOM_ARRAY = 'atomArray'; // MRV attribute name
var BOND_ARRAY = 'bondArray'; // MRV attribute name
var BOND_STEREO = 'bondStereo'; // MRV attribute name
var ATOMS_OF_BOND = 'atomRefs2'; // MRV attribute name
var FOR_IMAGE = true; // parameter for toMRV()
var ADD_CLICK = true; // parameter for loadLewisInlineImages()

var lewisMol = new Molecule(); // molecule object
var pathToRoot; // the path to the root directory from the page calling this one
var canvas; // HTML element where drawing occurs
var canvasName; // id of HTML element where drawing occurs
var isMOZ; // whether the browser is Mozilla-based or IE
var useSVG = false; // whether to use wz_graphics or SVG graphics
// var canvasOffsets = [0, 0]; // accounts for position of browser window on screen
// var prevCanvasOffsets = [0, 0]; // previous position of browser window on screen
var divOffsets = [0, 0]; // accounts for position of div containing image inside window
// var scrollOffsets = [0, 0]; // accounts for position of scrolling of div
var jsGraphics; // object that draws lines on the screen
var browserName; // name of user's browser
var mouseDownPosn = [0, 0]; // used to see if mouse has moved
var ELECTRON_OFFSETS = [2, -1]; // adjusts positions of electrons to align with text
var currentTask = ''; // the current active task
var currentStereo = STEREOS[PLAIN]; // the current active bond type
var undoMolStack = []; // record of molecules for undo
var redoMolStack = []; // record of molecules for redo
var movingAtomNum = NONE; // the atom being moved 
var drawingBondOtherTerminusNum = NONE; // if drawing a new bond, the other end 
		// of the bond being moved from the moving atom
var drawingBondOtherTerminusIsNew = false; // if drawing a new bond, whether the 
		// other end of the bond being moved is a new atom
var lastChangedBondNum = NONE; // bond whose order was changed and mouse has not
		// since moved away from it
var showAtomNumbers = false;

// Initiates the drawing object, sets the button options and the mouse actions.
function initLewisGraphics(toRoot, canvas_name, toolbarsCellName) {
	pathToRoot = toRoot;
	isMOZ = !document.all;
	canvasName = canvas_name;
	canvas = getCell(canvasName);
	initTheGraphics();
	browserName = whichBrowser();
	setToolbars(toolbarsCellName);
	setLewisOptions(toolbarsCellName);
	getMoleculeOffsets();
	changeActiveTask(C_BUTTON);
	offsets.textOffsets = [-3, -5]; // corrects the coordinate at which text is 
							// displayed so it is vertically
							// centered around the bond endpoint.
							// changed in SVG graphics
	offsets.canvasOffsets = [0, 0];
		// accounts for position of browser window on screen
	offsets.prevCanvasOffsets = [0, 0];
	offsets.scrollOffsets = [0, 0]; // accounts for position of scrolling of div
} // initLewisGraphics()

function setSVGGraphics(shouldUseSVG) {
	if (useSVG !== shouldUseSVG) {
		useSVG = shouldUseSVG;
		if (pathToRoot) {
			jsGraphics.clear();
			offsets.textOffsets = (useSVG ? [-5, 4] : [-3, -5]);
			ELECTRON_OFFSETS = (useSVG ? [2, 1] : [2, -1]);
			initTheGraphics();
			paintMol(!ADD_TO_STACK);
		} // if graphics had already been initiated
	} // if need to change type of graphics
} // setSVGGraphics()

function initTheGraphics() {
	jsGraphics = (useSVG ? new SVGGraphics(canvasName) :
		new JSGraphics(canvasName));
	setGraphicsToDefault(jsGraphics);
} // initTheGraphics()

// Sets graphics parameters to default values.
function setGraphicsToDefault() {
	setGraphics('12px', BLACK);
} // setGraphicsToDefault()

// Sets graphics parameters.
function setGraphics(size, color, stroke) {
	jsGraphics.setFont('Helvetica', size, Font.BOLD);
	jsGraphics.setColor(color);
	jsGraphics.setStroke(!stroke ? 2 : stroke);
} // setGraphicsToDefault()

// Initiates the constants.  
function initLewisConstants(dims, _MARVIN_WIDTH, ELECTRON_MRV_TITLES, 
		_LEWIS_PROPERTY, _HIGHLIGHT, translatedENTER_ELEMENT, 
		translatedNO_SUCH_ELEMENT, translatedOTHER_BUTTON_LABEL) {
	offsets.X = 0;
	offsets.Y = 1;
	offsets.X1 = 0; // not used, but required for offsets.js
	offsets.Y1 = 1; // not used, but required for offsets.js
	offsets.X2 = 2; // not used, but required for offsets.js
	offsets.Y2 = 3; // not used, but required for offsets.js
	CANVAS_DIMS = dims;
	MARVIN_WIDTH = _MARVIN_WIDTH;
	PAIRED_TITLE = ELECTRON_MRV_TITLES[0];
	UNPAIRED_TITLE = ELECTRON_MRV_TITLES[1];
	UNSHARED_TITLE = ELECTRON_MRV_TITLES[2];
	LEWIS_PROPERTY = _LEWIS_PROPERTY;
	HIGHLIGHT = _HIGHLIGHT;
	ENTER_ELEMENT = translatedENTER_ELEMENT;
	NO_SUCH_ELEMENT = translatedNO_SUCH_ELEMENT;
	OTHER_BUTTON_LABEL = translatedOTHER_BUTTON_LABEL;
} // initLewisConstants()

/* *********** Methods for interconverting formats and drawing ************/

// Parses the MRV code of the Lewis structure, storing the resulting Molecule 
// in the global variable lewisMol, and paints it on the canvas.
function parseLewisMRV(mrv) {
	lewisMol = getParsedLewisMol(mrv);
	paintMol(ADD_TO_STACK);
} // parseLewisMRV()

// Static method to parse the MRV code of the Lewis structure into a 
// Molecule object, which is returned.
function getParsedLewisMol(mrv) {
	var newLewisMol = new Molecule();
	newLewisMol.parseMRV(mrv, CENTER);
	return newLewisMol;
} // getParsedLewisMol()

// Returns MRV of the molecule.
function getLewisMRV() {
	return lewisMol.toMRV(!FOR_IMAGE);
} // getLewisMRV()

// Returns MRV suitable for converting to an image. Can operate on the global
// lewisMol or statically on a given MRV.
function getLewisImageMRV(mrv) {
	var imgMol = (isEmpty(mrv) ? lewisMol : getParsedLewisMol(mrv));
	return imgMol.toMRV(FOR_IMAGE);
} // getLewisImageMRV()

// Sends MRVs to the back end for conversion to images.  figuresData
// is an array of arrays; either one array of [mrv, qFlags] where mrv has 
// already been subjected to getLewisImageMRV() or doesn't need to be, or 
// one or more arrays of [mrv, qFlags, figId] where mrv has not yet been 
// subjected to getLewisImageMRV().
function loadLewisInlineImages(pathToRoot, figuresData, addClick, figCellPrefix) {
	"use strict";
	var toSend = new String.builder();
	var first = true;
	var numFigs = figuresData.length;
	var manyFigs = numFigs !== 1 || figuresData[0].length === 3;
	toSend.append('pathToRoot=').append(encodeURIComponent(pathToRoot));
	if (!isEmpty(addClick)) {
		toSend.append('&addClick=').append(addClick);
	} // if an addClick variable is provided
	if (!isEmpty(figCellPrefix)) {
		toSend.append('&figCellPrefix=').append(figCellPrefix);
	} // if a figCellPrefix variable is provided
	for (var figNum = 1; figNum <= numFigs; figNum++) {
		var figureData = figuresData[figNum - 1];
		toSend.append('&qFlags');
		if (manyFigs) toSend.append(figNum);
		toSend.append('=').append(figureData[1]);
		if (manyFigs) {
			toSend.append('&figId').append(figNum).append('=').
					append(figureData[2]);
		} // if manyFigs
		var toEncode = (manyFigs ?
				getLewisImageMRV(figureData[0]) : figureData[0]);
		toSend.append('&molStr');
		if (manyFigs) toSend.append(figNum);
		toSend.append('=').append(encodeURIComponent(toEncode));
	} // for each figure
	callAJAX(pathToRoot + 'includes/updateFigure.jsp', 
			toSend.toString(), finishLoadingLewisInlineImages);
} // loadLewisInlineImages()

// Sets the Lewis structure image elements to their new values.  If there's just
// one image, its element ID is fig; otherwise, it's fig1, fig1_1, or other.
function finishLoadingLewisInlineImages() {
	"use strict";
	var responsePage,
		figNum = 0,
		figNumStr,
		addClick,
		figCellPrefix,
		imgData,
		figId,
		imageBld;
	if (xmlHttp.readyState === 4) { // ready to continue
		var responsePage = xmlHttp.responseText;
		addClick = extractField(responsePage, 'addClickValue');
		figCellPrefix = extractField(responsePage, 'figCellPrefixValue');
		if (isEmpty(figCellPrefix)) figCellPrefix = 'fig';
		while (true) {
			figNumStr = (figNum === 0 ? '' : new String(figNum));
			imgData = extractField(responsePage, 
					new String.builder().append('imageXMLValue').
						append(figNumStr).toString());
			if (!isWhiteSpace(imgData)) {
				figId = extractField(responsePage, 
						new String.builder().append('figId').
							append(figNumStr).toString());
				if (addClick === 'true') {
					imageBld = new String.builder();
					imageBld.append('<a onclick="showSource(');
					if (figNum > 0) {
						imageBld.append(figId);
					} // if not the 0th figure
					imageBld.append(');">').append(imgData).append('<\/a>');
					imgData = imageBld.toString();
				} // if add a hyperlink
				setInnerHTML(new String.builder().append(figCellPrefix).
						append(figId).toString(), imgData);
			} else if (figNum > 0) {
				break;
			} // if there's a figure
			figNum++;
		} // while there are figures to acquire
	} // if ready to continue
} // finishLoadingLewisInlineImages()

// Draw the molecule on the canvas.
function paintMol(addToStack) {
	jsGraphics.clear();
	lewisMol.draw(jsGraphics);
	jsGraphics.paint();
	if (addToStack) {
		undoMolStack.push(lewisMol.toMRV(!FOR_IMAGE));
		if (undoMolStack.length > 1) showActive('Undo');
		redoMolStack = [];
		showInactive('Redo');
	} // if should add the molecule to the undo stack
} // paintMol()

/* ******************* Toolbar methods *****************/

// Makes the toolbars, displays them in the HTML page in the appropriate place.
function setToolbars(toolbarsCellName) {
	var STYLE = 'style',
		attrs,
		styleProps,
		trNode, 
		tdNode, 
		textNode,
		toolbar,
		toolbarNum,
		buttonNum,
		task,
		isChargeButton,
		is2ElecButton,
		isMenu,
		isBlank,
		isAtomTask,
		popup;
	var xmlDoc = getParentXMLDoc('table');
	var tableNode = getFirstNode(xmlDoc, 'table');
	for (toolbarNum = 0; toolbarNum < TASKS.length; toolbarNum++) {
		attrs = [];
		attrs.push([STYLE, 'height:30px;']);
		trNode = addNewNode(xmlDoc, tableNode, 'tr', attrs);
		toolbar = TASKS[toolbarNum];
		for (buttonNum = 0; buttonNum < toolbar.length; buttonNum++) {
			task = toolbar[buttonNum];
			isChargeButton = taskIsChargeChange(task);
			is2ElecButton = task.indexOf('2Elec') === 3;
			isMenu = task === 'Menu';
			isBlank = task === '';
			isAtomTask = taskIsAtom(task);
			// make the style of the button cell
			styleProps = [];
			styleProps.push(['margin', '5px']);
			styleProps.push(['width', isBlank || isMenu ? '8px' : '25px']);
			styleProps.push(['text-align', 'center']);
			if (isBlank) {
				styleProps.push(['border-bottom-style', 'none']);
			}
			if (isAtomTask) {
				styleProps.push(['color', isEmpty(ATOM_COLORS[task]) ?
						DEFAULT_ATOM_COLOR : ATOM_COLORS[task]]);
			} // if is atom
			// make list of attributes of the button cell
			attrs = [];
			attrs.push(['id', task]);
			if (!isBlank) attrs.push(['class', 'whiteTable']);
			attrs.push([STYLE, getStyleString(styleProps)]);
			attrs.push(isMenu ? ['onmousedown', 'selectBondType();'] :
					['onclick', 
						task === 'Other' ? 'setOtherElement();' :
						task === 'Grow' ? 'resizeMol(10);' :
						task === 'Shrink' ? 'resizeMol(-10);' :
						task === 'Paste' ? 'editMRV();' :
						task === 'New' ? 'clearMol();' :
						task === 'Undo' ? 'undoMe();' :
						task === 'Redo' ? 'redoMe();' :
						isBlank ? '' :
						'changeActiveTask(TASKS[' + toolbarNum + 
							'][' + buttonNum + ']);']);
			if (task === 'Bond') attrs.push(['colspan', '2']);
			// make td node, add contents: image, text, or bold text
			tdNode = addNewNode(xmlDoc, trNode, 'td', attrs);
			if (isChargeButton) {
				textNode = getTextNode(xmlDoc, toolbarNum === 0 ? 
						'+' : cerToUnicode('&minus;'));
			} else if (task === 'Other') {
				textNode = getTextNode(xmlDoc, OTHER_BUTTON_LABEL);
			} else if (isAtomTask) {
				attrs = [];
				if (task === 'Si') attrs.push(['id', task + BOLD]);
				textNode = getNewNode(xmlDoc, 'b', attrs);
				textNode.appendChild(getTextNode(xmlDoc, task));
			} else {
				attrs = [];
				attrs.push(['id', task + 'Img']);
				attrs.push(['src', getButtonSrc(task)]);
				popup = (task === 'New' ? 'Clear'  :
						task === 'Paste' ? 'Source' :
						task);
				attrs.push(['title', popup]);
				attrs.push(['alttext', popup]);
				textNode = getNewNode(xmlDoc, 'img', attrs);
			} // if task
			tdNode.appendChild(textNode); 
			if (isChargeButton || is2ElecButton || isMenu || isBlank) {
				styleProps = [];
				styleProps.push(['width', '10px']);
				styleProps.push(['border-bottom-style', 'none']);
				attrs = [];
				attrs.push([STYLE, getStyleString(styleProps)]);
				addNewNode(xmlDoc, trNode, 'td', attrs);
			} // if at button break
		} // for each button in the toolbar
	} // for each toolbar
	var xml = getXML(xmlDoc);
	setInnerHTML(toolbarsCellName, xml);
	showInactive('Undo');
	showInactive('Redo');
} // setToolbars()

function taskIsAtom(task) { return task.length <= 2; }
function taskIsChargeChange(task) { return task.indexOf('crease') === 2; }
function taskIsElectronsChange(task) { return task.indexOf('Elec') === 4; }
function taskIsElectronsDecrease(task) {
	return taskIsElectronsChange(task) && task.charAt(0) === 'D';
} // taskIsElectronsDecrease() 

// These methods require that an image of the inactive button named 
// ButName_GRAY.GIF, where GRAY is the value of the variable GRAY 
// (e.g., ButRedo_d3d3d3.GIF), is present in ./lewisImg/.
function showActive(buttonName) { setButtonColor(buttonName, BLACK); }
function showInactive(buttonName) { setButtonColor(buttonName, GRAY); }

// Sets a button's border's color and changes the button image.
function setButtonColor(buttonName, color) {
	var button = getCell(buttonName);
	if (button) button.style.borderColor = color;
	button = getCell(buttonName + 'Img');
	if (button) {
		var srcName = (color === BLACK ? buttonName : buttonName + color);
		button.src = getButtonSrc(srcName); 
		button.style.color = color;
	} // if the button exists
} // setButtonColor()

function getButtonSrc(buttonName) {
	var srcBld = new String.builder().
			append(pathToRoot).append('js\/lewisImg\/But').
			append(buttonName.replace('#', '_')).
			append('.GIF');
	return srcBld.toString();
} // getButtonSrc()

function changeActiveTask(newTask) {
	var currentButton = getCell(currentTask);
	if (currentButton) {
		currentButton.style.border = 'solid 1px black';
		currentButton.style.margin = '5px';
	} // if there is a current button
	var newButton = getCell(newTask);
	newButton.style.border = 'solid 3px black';
	newButton.style.margin = '3px';
	currentTask = newTask;
	setMouseActions();
} // changeActiveTask()

function setOtherElement() {
	var repeatNum = 1;
	var ANY_ELEM_TASK = TASKS[ANY_ELEM_ROW][ANY_ELEM_COL];
	var element = ANY_ELEM_TASK;
	var changeOtherElementCell = true;
	var msg = ENTER_ELEMENT;
	while (true) {
		element = window.prompt(msg);
		if (isEmpty(element)) return;
		var arr = ['D', 'T', 'He', 'Li', 'Be', 'B', 'Ne',
				'Na', 'Mg', 'Al', 'Si', 'Ar',
				'K', 'Ca', 'Sc', 'Ti', 'V', 'Cr', 'Mn', 'Fe', 
				'Co', 'Ni', 'Cu', 'Zn', 'Ga', 'Ge', 'As', 'Se', 'Kr',
				'Rb', 'Sr', 'Y', 'Zr', 'Nb', 'Mo', 'Tc', 'Ru', 
				'Rh', 'Pd', 'Ag', 'Cd', 'In', 'Sn', 'Sb', 'Te', 'Xe',
				'Cs', 'Ba', 'La', 'Ce', 'Pr', 'Nd', 'Pm', 'Sm', 
				'Eu', 'Gd', 'Tb', 'Dy', 'Ho', 'Er', 'Tm', 'Yb', 'Lu',
				'Hf', 'Ta', 'W', 'Re', 'Os', 'Ir', 'Pt', 'Au', 'Hg',
				'Tl', 'Pb', 'Bi', 'Po', 'At', 'Rn', 
				'Fr', 'Ra', 'Ac', 'Th', 'Pa', 'U', 'Np', 'Pu', 'Am',
				'Cm', 'Bk', 'Cf', 'Es', 'Fm', 'Md', 'No', 'Lr', 'Rf',
				'Db', 'Sg', 'Bh', 'Hs', 'Mt', 'Ds', 'Rg', 'Cn', 'Fl', 'Lv'];
		if (arr.contains(element)) break;
		arr = ['H', 'C', 'N', 'O', 'F', 'P', 'S', 'Cl', 'Br', 'I'];
		if (arr.contains(element)) {
			changeOtherElementCell = false;
			break;
		} // if chose an element that has a button
		msg = NO_SUCH_ELEMENT;
	} // while true
	if (changeOtherElementCell) {
		getCell(ANY_ELEM_TASK + BOLD).innerHTML = element;
		getCell(ANY_ELEM_TASK).id = element;
		TASKS[ANY_ELEM_ROW][ANY_ELEM_COL] = element;
	} // if changeOtherElementCell
	changeActiveTask(element);
} // setOtherElement()

// Replaces the bond icon with a select menu.
function selectBondType() {
	var attrs = [],
		label,
		optionNode;
	attrs.push(['id', BOND_TYPE_MENU]);
	attrs.push(['onclick', 'setBondType();']);
	var xmlDoc = getParentXMLDoc('select', attrs);
	var selectNode = getFirstNode(xmlDoc, 'select');
	for (var bondTypeNum = 0; bondTypeNum < STEREOS.length; bondTypeNum++) {
		attrs = [['value', bondTypeNum]];
		optionNode = addNewNode(xmlDoc, selectNode, 'option', attrs);
		var label = (bondTypeNum === PLAIN ? 'Plain' :
				bondTypeNum === HASH ? 'Hash' :
				bondTypeNum === BOLD ? 'Bold' :
				'Wavy');
		optionNode.appendChild(getTextNode(xmlDoc, label));
	} // for each bond type
	setInnerHTML(BOND_BUTTON, getXML(xmlDoc));
	getCell(BOND_TYPE_MENU).value = STEREOS.indexOf(currentStereo);
} // selectBondType()

// Sets a new bond type.
function setBondType() {
	currentStereo = STEREOS[parseToInt(getCell(BOND_TYPE_MENU).value)];
	var attrs = [];
	attrs.push(['id', 'BondImg']);
	attrs.push(['src', getButtonSrc(currentStereo)]);
	var xmlDoc = getParentXMLDoc('img', attrs);
	setInnerHTML(BOND_BUTTON, getXML(xmlDoc));
} // setBondType()

function setAtomNumberDisplay(opt) {
	showAtomNumbers = opt;
	paintMol(!ADD_TO_STACK);
} // setAtomNumberDisplay()

function toggleAtomNumberDisplay(opt) {
	showAtomNumbers = !showAtomNumbers;
	paintMol(!ADD_TO_STACK);
} // toggleAtomNumberDisplay()

function setLewisOptions(toolbarsCellName) {
	var posn = toolbarsCellName.indexOf('Toolbar');
	var optionsCellName = toolbarsCellName.substring(0, posn) + 'Options';
	setInnerHTML(optionsCellName, 
			'<table style="width:100%;">'+
			'<tr><td style="border-bottom-style:none;">'+
			'<input type="checkbox" name="showAtomNumbers" '+
			'onchange="toggleAtomNumberDisplay();">'+
			'Show atom numbers<\/td><td style="border-bottom-style:none;">'+
			'<input type="checkbox" name="useSVG" '+
			'onchange="setSVGGraphics(this.checked);">'+
			'Use SVG graphics<\/td><\/tr><\/table>');
} // setLewisOptions()

/* ******************* Methods handling user actions *****************/

// Sets the methods to be executed upon certain mouse actions, depending on 
// current task.
function setMouseActions() {
	if (canvas) {
		if (taskIsAtom(currentTask) || ['Bond', 'Move'].contains(currentTask)) {
			canvas.onclick = '';
			canvas.onmousedown = (isMOZ ? mouseDownMOZ : mouseDownIE);
			canvas.onmousemove = (isMOZ ? moveAtomMOZ : moveAtomIE);
			canvas.onmouseup = (isMOZ ? mouseUpMOZ : mouseUpIE);
			canvas.addEventListener('touchstart', mouseDownTouch, false);
			canvas.removeEventListener('touchmove', moveCursorTouch);
			canvas.addEventListener('touchmove', moveAtomTouch, false);
			canvas.addEventListener('touchend', mouseUpTouch, false);
		} else { // charge, electrons, cut
			canvas.onclick = (isMOZ ? changeNearestMOZ : changeNearestIE);
			canvas.onmousedown = '';
			canvas.onmousemove = (isMOZ ? moveCursorMOZ : moveCursorIE);
			canvas.onmouseup = '';
			canvas.removeEventListener('touchstart', mouseDownTouch);
			canvas.removeEventListener('touchmove', moveAtomTouch);
			canvas.addEventListener('touchmove', moveCursorTouch, false);
			canvas.removeEventListener('touchend', mouseUpTouch);
			lastChangedBondNum = NONE;
		} // if currentTask
	} // if the canvas exists
} // setMouseActions()

// Recalculates offsets after window is resized.
// We need to run this method anew for each click in case window is resized.
function getMoleculeOffsets() {
	var canvasPos = Position.get(canvas);
	if (browserName !== 'Internet Explorer') {
		offsets.canvasOffsets = [canvasPos.left, canvasPos.top];
	} // anything other than IE
	recalculateOffsets();
} // getMoleculeOffsets()

/* *************** Molecule-modifying methods ********************/

function resizeMol(percent) {
	lewisMol.resize(percent);
	paintMol(ADD_TO_STACK);
} // resizeMol()

function clearMol() {
	lewisMol = new Molecule();
	paintMol(ADD_TO_STACK);
} // resizeMol()

function editMRV() {
	var urlBld = new String.builder().
			append(pathToRoot).
			append('includes/showSourceCode.jsp?editable=true&sourceCode=').
			append(encodeURIComponent(lewisMol.toMRV(!FOR_IMAGE)));
	openSourceCodeWindow(urlBld.toString());
} // editMRV()

function undoMe() {
	if (undoMolStack.length > 1) {
		redoMolStack.push(undoMolStack.pop());
		showActive('Redo');
		if (undoMolStack.length <= 1) showInactive('Undo');
		lewisMol = new Molecule();
		lewisMol.parseMRV(undoMolStack[undoMolStack.length - 1], !CENTER);
		paintMol(!ADD_TO_STACK);
	} // if there are things to undo
} // undoMe()

function redoMe() {
	if (redoMolStack.length >= 1) {
		undoMolStack.push(redoMolStack.pop());
		showActive('Undo');
		if (redoMolStack.length === 0) showInactive('Redo');
		lewisMol = new Molecule();
		lewisMol.parseMRV(undoMolStack[undoMolStack.length - 1], !CENTER);
		paintMol(!ADD_TO_STACK);
	} // if there are things to redo
} // redoMe()

// Selects and holds the atom or bond nearest to where the user clicked.
function mouseDown(pt, type) {
	var nearestNums = lewisMol.getNearest(canvasUnset(keepInBounds(pt)), type);
	mouseDownPosn = pt;
	if (currentTask === 'Move' && nearestNums[ATOM] !== NONE) {
		movingAtomNum = nearestNums[ATOM];
	} else if (currentTask === 'Bond' || taskIsAtom(currentTask)) {
		var element = (taskIsAtom(currentTask) ? currentTask : 'C');
		var drawingBondOtherTerminus;
		var atomAndNum;
		if (nearestNums[ATOM] === NONE) {
			atomAndNum = makeAndAddAtom(element, pt);
			drawingBondOtherTerminus = atomAndNum[ATOM];
			drawingBondOtherTerminusNum = atomAndNum[ATOM_NUM];
			drawingBondOtherTerminusIsNew = true;
		} else {
			drawingBondOtherTerminusNum = nearestNums[ATOM];
			drawingBondOtherTerminus = 
					lewisMol.getAtom(drawingBondOtherTerminusNum);
			drawingBondOtherTerminusIsNew = false;
		} // if a nearby atom was found
		atomAndNum = makeAndAddAtom(element, pt);
		var movingAtom = atomAndNum[ATOM];
		movingAtomNum = atomAndNum[ATOM_NUM];
		makeAndAddBond([drawingBondOtherTerminus, movingAtom]);
	} // if currentTask
} // mouseDown()

// Actions upon moving an atom.
function moveAtom(pt) {
	moveCursor(pt);
	if (movingAtomNum !== NONE) lewisMol.moveAtom(movingAtomNum, pt);
	paintMol(!ADD_TO_STACK);
} // moveAtom()

// Actions upon moving the cursor.
function moveCursor(pt) {
	keepInBounds(pt);
	getMoleculeOffsets();
	var nearestNums = lewisMol.getNearest(canvasUnset(pt));
	if (lastChangedBondNum !== nearestNums[BOND]) lastChangedBondNum = NONE;
} // moveCursor()

// Actions upon mouse up.
function mouseUp(pt) {
	keepInBounds(pt);
	var addToStack = true;
	if (currentTask !== 'Move') {
		var newBondLength = getDistance(
				lewisMol.getAtom(drawingBondOtherTerminusNum).getPosn(),
				lewisMol.getAtom(movingAtomNum).getPosn());
		if (newBondLength < MIN_BOND_LENGTH) {
			lewisMol.removeAtom(movingAtomNum);
			if (drawingBondOtherTerminusIsNew) {
				lewisMol.removeAtom(drawingBondOtherTerminusNum);
			} // if other atom of bond is new
			addToStack = false;
			changeNearest(pt);
		} else {
			var nearestNums = 
					lewisMol.getNearest(canvasUnset(pt), movingAtomNum);
			if ((currentTask === 'Bond' || taskIsAtom(currentTask)) &&
					nearestNums[ATOM] !== NONE) {
				lewisMol.removeAtom(movingAtomNum);
				makeAndAddBond(
						[lewisMol.getAtom(drawingBondOtherTerminusNum),
						lewisMol.getAtom(nearestNums[ATOM])]);
			} // if drew bond to an existing atom
		} // if the atoms are too close
	} // if not just moving atoms
	movingAtomNum = NONE;
	drawingBondOtherTerminusNum = NONE;
	paintMol(addToStack);
} // mouseUp()

// Selects the atom or bond nearest to where the user clicked.
function changeNearest(pt, type) {
	keepInBounds(pt);
	var nearestNums = lewisMol.getNearest(canvasUnset(pt), type);
	if (nearestNums[ATOM] !== NONE) { // nearby atom
		var delta;
		if (taskIsChargeChange(currentTask)) {
			delta = (currentTask.charAt(0) === 'I' ? 1 : -1);
			lewisMol.changeCharge(nearestNums[ATOM], delta);
		} else if (taskIsElectronsChange(currentTask)) {
			delta = parseInt(currentTask.charAt(3), 10);
			if (taskIsElectronsDecrease(currentTask)) {
				delta *= -1;
			} // if decreasing number of electrons
			lewisMol.changeElectrons(nearestNums[ATOM], delta);
		} else if (currentTask === 'Cut') {
			lewisMol.removeAtom(nearestNums[ATOM]);
		} else if (taskIsAtom(currentTask)) {
			var nearestAtom = lewisMol.getAtom(nearestNums[ATOM]);
			if (nearestAtom.getElement() !== currentTask) {
				lewisMol.setElement(nearestNums[ATOM], currentTask);
			} else return;
		} // if currentTask
	} else if (nearestNums[BOND] !== NONE) { // nearby bond
		var nearestBondStereo;
		if (currentTask === 'Cut') {
			lewisMol.removeBond(nearestNums[BOND]);
		} else if (currentTask === 'Bond') {
			nearestBondStereo = lewisMol.getStereo(nearestNums[BOND]);
			if (currentStereo === nearestBondStereo) {
				if (currentStereo === STEREOS[PLAIN]) {
					if (nearestNums[BOND] === lastChangedBondNum) {
						lewisMol.increaseBondOrder(nearestNums[BOND]);
					} else {
						lewisMol.toggleBondOrder(nearestNums[BOND]);
					} // if nearest bond is same as last changed bond
				} else {
					lewisMol.switchBondDirection(nearestNums[BOND]);
				} // if currentStereo is plain bond tool
			} else {
				lewisMol.setBondOrder(nearestNums[BOND], 1);
				lewisMol.setBondStereo(nearestNums[BOND], currentStereo);
			} // if nearest bond already has currentStereo
			lastChangedBondNum = nearestNums[BOND];
		} else if (taskIsAtom(currentTask) ||
				taskIsElectronsChange(currentTask) ||
				taskIsChargeChange(currentTask)) {
			nearestBondStereo = lewisMol.getStereo(nearestNums[BOND]);
			if (nearestBondStereo !== STEREOS[PLAIN]) {
				lewisMol.setBondStereo(nearestNums[BOND], STEREOS[PLAIN]);
			} else if (nearestNums[BOND] === lastChangedBondNum) {
				lewisMol.increaseBondOrder(nearestNums[BOND]);
			} else {
				lewisMol.toggleBondOrder(nearestNums[BOND]);
			} // if nearestNums[BOND]
			lastChangedBondNum = nearestNums[BOND];
		} // if currentTask
	} else { // no nearby bond or atom
		if (taskIsAtom(currentTask)) {
			makeAndAddAtom(currentTask, pt);
		} else if (currentTask === 'Bond') {
			var atoms = [];
			for (var atomNum = -1; atomNum <= 1; atomNum += 2) {
				var posn = [pt[offsets.X] + (atomNum * BOND_LENGTH) / 2,
				pt[offsets.Y]];
				atoms.push(makeAndAddAtom('C', posn)[ATOM]);
			} // for each new atom
			makeAndAddBond(atoms);
		} // if currentTask
	} // if there's a nearby bond
	paintMol(ADD_TO_STACK);
} // changeNearest()

// Don't let the cursor move off the canvas.  Modifies the original array AND
// returns it.
function keepInBounds(pt) {
	if (canvasUnsetX(pt[offsets.X]) < MARGINS[offsets.X][0]) {
		pt[offsets.X] = canvasSetX(MARGINS[offsets.X][0]);
	} // left
	if (canvasUnsetX(pt[offsets.X]) > CANVAS_DIMS[offsets.X] - MARGINS[offsets.X][1]) {
		pt[offsets.X] = canvasSetX(CANVAS_DIMS[offsets.X] - MARGINS[offsets.X][1]);
	} // right
	if (canvasUnsetY(pt[offsets.Y]) < MARGINS[offsets.Y][0]) {
		pt[offsets.Y] = canvasSetY(MARGINS[offsets.Y][0]);
	} // top
	if (canvasUnsetY(pt[offsets.Y]) > CANVAS_DIMS[offsets.Y] - MARGINS[offsets.Y][1]) {
		pt[offsets.Y] = canvasSetY(CANVAS_DIMS[offsets.Y] - MARGINS[offsets.Y][1]);
	} // bottom
	return pt;
} // keepInBounds()

// Makes an atom and adds it to the molecule. Returns the atom and the atom
// number as an array.
function makeAndAddAtom(element, posn) {
	var atom = new Atom(lewisMol);
	atom.setElement(element);
	atom.setPosn(posn);
	return [atom, lewisMol.addAtom(atom)];
} // makeAndAddAtom()

// Makes a bond and adds it to the molecule if there isn't already a bond
// between the two atoms (prevents "parallel" bonds which MolImporter can't
// handle). Returns the bond number.
function makeAndAddBond(bondAtoms) {
	var bondNum = 0;
	if (!lewisMol.alreadyHasBond(bondAtoms)) {
		var bond = new Bond(lewisMol);
		bond.setAtoms(bondAtoms);
		bond.setOrder(1);
		bond.setStereo(currentStereo);
		bondNum = lewisMol.addBond(bond);
		bondAtoms[0].reportAddedBond(bondAtoms[1].getPosn());
		bondAtoms[1].reportAddedBond(bondAtoms[0].getPosn());
	} // if bond between atoms doesn't already exist
	return bondNum;
} // makeAndAddBond()

/**************************************************************************/
/************************** Classes ***************************************/
/**************************************************************************/


/* A molecule. */

function Molecule() {
	/* these are the members */
	var atoms = [];
	var bonds = [];
	var colorSet = [];
	var bondsOfAtomsSet = false;

	/* get methods */
	this.getNumAtoms = function() { return atoms.length; };
	this.getNumBonds = function() { return bonds.length; };
	this.getAtom = function(atomNum) { return atoms[atomNum - 1]; }; // 1-based
	this.getBond = function(bondNum) { return bonds[bondNum - 1]; }; // 1-based
	this.getStereo = function(bondNum) {
		return this.getBond(bondNum).getStereo();
	};
	this.getNumOfAtom = function(atom) { return atoms.indexOf(atom) + 1; }; // 1-based
	this.getNumOfBond = function(bond) { return bonds.indexOf(bond) + 1; }; // 1-based

	/* editing methods */
	this.addAtom = function(atom) { 
		atoms.push(atom); 
		return atoms.length;
	}; // addAtom()
	this.addBond = function(bond) { 
		bonds.push(bond); 
		return bonds.length;
	}; // addBond()
	this.addColor = function(color) { colorSet.push(color); };
	this.setElement = function(atomNum, elem) { 
		this.getAtom(atomNum).setElement(elem); 
	}; // setElement()
	this.changeCharge = function(atomNum, delta) {
		this.getAtom(atomNum).changeCharge(delta); 
	}; // changeCharge()
	this.changeElectrons = function(atomNum, delta) {
		this.getAtom(atomNum).changeElectrons(delta);
	}; // changeElectrons()
	this.setBondOrder = function(bondNum, newOrder) {
		this.getBond(bondNum).setOrder(newOrder);
	}; // setBondOrder()
	this.increaseBondOrder = function(bondNum) {
		this.getBond(bondNum).increaseOrder();
	}; // increaseBondOrder()
	this.toggleBondOrder = function(bondNum) {
		this.getBond(bondNum).toggleOrder();
	}; // toggleBondOrder()
	this.switchBondDirection = function(bondNum) {
		this.getBond(bondNum).switchDirection();
	}; // switchBondDirection()
	this.setBondStereo = function(bondNum, newStereo) {
		this.getBond(bondNum).setStereo(newStereo);
	}; // setBondStereo()

	this.moveAtom = function(atomNum, posn) {
		var movingAtom = this.getAtom(atomNum);
		var bondedAtoms = this.getBondedAtoms(movingAtom);
		movingAtom.move(posn, bondedAtoms);
	}; // moveAtom()

	this.getBondedAtoms = function(atom) {
		var bondedAtoms = [];
		for (var bondNum = 1; bondNum <= bonds.length; bondNum++) {
			var bond = this.getBond(bondNum);
			if (bond.hasAtom(atom)) {
				var atoms = bond.getAtoms();
				bondedAtoms.push(atoms[0] === atom ? atoms[1] : atoms[0]);
			} // if bond has atom
		} // for each bond
		return bondedAtoms;
	}; // getBondedAtoms()

	this.alreadyHasBond = function(atomsOfNewBond) {
		for (var bondNum = 1; bondNum <= bonds.length; bondNum++) {
			var bond = this.getBond(bondNum);
			if (bond.hasAtom(atomsOfNewBond[0]) &&
					bond.hasAtom(atomsOfNewBond[1])) {
				return true;
			} // if bond already has both atoms
		} // for each bond
		return false;
	}; // alreadyHasBond()

	this.removeAtom = function(atomNum) {
		var atomToRemove = this.getAtom(atomNum);
		for (var bondNum = bonds.length; bondNum >= 1; bondNum--) {
			if (this.getBond(bondNum).hasAtom(atomToRemove)) {
				this.removeBond(bondNum);
			} // if bond has atom
		} // for each bond
		atoms.splice(atomNum - 1, 1);
	}; // removeAtom()

	this.removeBond = function(bondNum) {
		bonds.splice(bondNum - 1, 1);
	}; // removeBond()

	// Finds the nearest atom or bond to a point.
	this.getNearest = function(pt, excludeAtomNum) {
		var nearestNums = [NONE, NONE];
		var nearestDists = [-1, -1];
		var distance, nearestDist;
		for (var atomNum = 1; atomNum <= atoms.length; atomNum++) {
			var atomPosn = atoms[atomNum - 1].getPosn();
			distance = getDistance(atomPosn, pt);
			nearestDist = nearestDists[ATOM];
			if (distance < SELECT_DISTANCE &&
					(nearestDist === -1 || distance < nearestDist)&&
					(isEmpty(excludeAtomNum) ||
						atomNum !== excludeAtomNum)) {
				nearestNums[ATOM] = atomNum;
				nearestDists[ATOM] = distance;
			} // if atom is nearer point than other atoms are
		} // for each atom
		for (var bondNum = 1; bondNum <= bonds.length; bondNum++) {
			var bondAtoms = bonds[bondNum - 1].getAtoms();
			distance = getDistanceSegmentToPoint(
					bondAtoms[0].getPosn(), bondAtoms[1].getPosn(), pt);
			nearestDist = nearestDists[BOND];
			if (distance < SELECT_DISTANCE &&
					(nearestDist === -1 || distance < nearestDist)) {
				nearestNums[BOND] = bondNum;
				nearestDists[BOND] = distance;
			} // if bond is nearer point than other bonds are
		} // for each bond
		return nearestNums;
	}; // getNearest()

	/* sizing and position methods */
	this.center = function() {
		var mins = [CANVAS_DIMS[offsets.X], CANVAS_DIMS[offsets.Y]]; // new object
		var maxs = [0, 0];
		for (var atomNum = 0; atomNum < atoms.length; atomNum++) {
			var posn = atoms[atomNum].getPosn();
			if (mins[offsets.X] > posn[offsets.X]) mins[offsets.X] =
			posn[offsets.X];
			if (maxs[offsets.X] < posn[offsets.X]) maxs[offsets.X] =
			posn[offsets.X];
			if (mins[offsets.Y] > posn[offsets.Y]) mins[offsets.Y] =
			posn[offsets.Y];
			if (maxs[offsets.Y] < posn[offsets.Y]) maxs[offsets.Y] =
			posn[offsets.Y];
		} // for each atom
		var molCenter = [getAverage(mins[offsets.X], maxs[offsets.X]),
				getAverage(mins[offsets.Y], maxs[offsets.Y])];
		var canvasCenter = arrayRound(scalarProd(CANVAS_DIMS, 0.5)); 
		var shift = arraysDiff(canvasCenter, molCenter);
		this.move(shift);
	}; // center()

	this.move = function(shift) {
		for (var atomNum = 0; atomNum < atoms.length; atomNum++) {
			var atom = atoms[atomNum];
			atom.setPosn(arraysSum(atom.getPosn(), shift));
		} // for each atom
	}; // move()

	this.resize = function(percent) {
		var mins = [CANVAS_DIMS[offsets.X], CANVAS_DIMS[offsets.Y]]; // new object
		var atomNum;
		for (atomNum = 0; atomNum < atoms.length; atomNum++) {
			var posn = atoms[atomNum].getPosn();
			if (mins[offsets.X] > posn[offsets.X])
				mins[offsets.X] = posn[offsets.X];
			if (mins[offsets.Y] > posn[offsets.Y]) mins[offsets.Y] =
			posn[offsets.Y];
		} // for each atom
		var moveAmt = scalarProd(mins, -1);
		var newPosns = [];
		var inBounds = true;
		for (atomNum = 0; atomNum < atoms.length; atomNum++) {
			var atom = atoms[atomNum];
			var originPosn = arraysSum(atom.getPosn(), moveAmt);
			var newPosn = arrayRound(scalarProd(originPosn, 1 + percent / 100));
			if (newPosn[offsets.X] < 0 || newPosn[offsets.X] >
				CANVAS_DIMS[offsets.X] ||
					newPosn[offsets.Y] < 0 || newPosn[offsets.Y] >
					CANVAS_DIMS[offsets.Y]) {
				inBounds = false;
				break;
			} // if new position is out of bounds
			newPosns.push(newPosn);
		} // for each atom
		if (inBounds) {
			for (atomNum = 0; atomNum < atoms.length; atomNum++) {
				atoms[atomNum].setPosn(newPosns[atomNum]);
			} // for each atom
			this.center();
		} // if structure is not out of bounds
	}; // resize()

	// Changes the position of the atoms when the window is resized.
	this.canvasReset = function() {
		for (var atomNum = 0; atomNum < atoms.length; atomNum++) {
			atoms[atomNum].canvasReset();
		} // for each atom
	}; // canvasReset()

	/* MRV methods */
	this.parseMRV = function(mrv, center) {
		if (isEmpty(mrv)) return;
		var parser = new DOMParser();
		var xmlDoc = parser.parseFromString(mrv.replace(/\\n/g, ''), 'text/xml');
		this.parseColors(xmlDoc);
		this.parseAtoms(xmlDoc);
		this.parseBonds(xmlDoc);
		if (center) this.center();
		this.adjustAllAtomOctants(); // not sure why this is needed, but it is
	}; // parseMRV()

	this.adjustAllAtomOctants = function() {
		for (var atomNum = 1; atomNum <= atoms.length; atomNum++) {
			var atom = this.getAtom(atomNum);
			var bondedAtoms = this.getBondedAtoms(atom);
			var bondedAtomsPosns = [];
			for (var bondedNum = 0; bondedNum < bondedAtoms.length; bondedNum++) {
				bondedAtomsPosns.push(bondedAtoms[bondedNum].getPosn());
			} // for each bonded atom
			atom.redistributeOctants(bondedAtomsPosns);
		} // for each bonded atom
	}; // adjustAllAtomOctants()

	this.parseColors = function(xmlDoc) {
		this.addColor(BLACK);
		var mDocNode = xmlDoc.getElementsByTagName(MDOCUMENT)[0];
		if (mDocNode) {
			var colorSetStr = mDocNode.getAttribute(COLOR_SET);
			if (!isEmpty(colorSetStr)) {
				var colors = colorSetStr.split(',');
				for (var colorNum = 0; colorNum < colors.length; colorNum++) {
					var colorNumAndDefn = colors[colorNum].split(':');
					if (colorNumAndDefn[0] !== '0') {
						this.addColor(colorNumAndDefn[1]);
					}
				} // for each color
			} // if there are colors
		} // if there is a document
	}; // parseColors()

	this.parseAtoms = function(xmlDoc) {
		var atomArray = xmlDoc.getElementsByTagName(ATOM_ARRAY);
		var atomNum, atom;
		if (!isEmpty(atomArray)) {
			var atomIDsAttribute = atomArray[0].getAttribute('atomID');
			if (isEmpty(atomIDsAttribute)) {
				var atomNodes = atomArray[0].childNodes;
				for (atomNum = 0; atomNum < atomNodes.length; atomNum++) {
					var atomNode = atomNodes[atomNum];
					if (atomNode.getAttribute) {
						atom = new Atom(this);
						var colorNum = atom.parseMRV(atomNode);
						if (colorNum !== 0) atom.setColor(colorSet[colorNum]);
						this.addAtom(atom);
					} // if this node is really a node
				} // for each atom
			} else {
				var elementTypesStr = atomArray[0].getAttribute(ELEMENT);
				var xCoordsStr = atomArray[0].getAttribute(X_COORD);
				var yCoordsStr = atomArray[0].getAttribute(Y_COORD);
				var formalChargesStr = atomArray[0].getAttribute(FORMAL_CHARGE);
				var haveCharges = !isEmpty(formalChargesStr);
				var colorsStr = atomArray[0].getAttribute(COLOR_NUM);
				var haveColors = !isEmpty(colorsStr);
				var elementTypes = elementTypesStr.split(' ');
				var xCoords = xCoordsStr.split(' ');
				var yCoords = yCoordsStr.split(' ');
				var formalCharges = (haveCharges ? formalChargesStr.split(' ') : []);
				var colors = (haveColors ? colorsStr.split(' ') : []);
				for (atomNum = 0; atomNum < elementTypes.length; atomNum++) {
					atom = new Atom(this);
					atom.setElement(elementTypes[atomNum]);
					atom.setPosn([parseFloat(xCoords[atomNum]), 
							parseFloat(yCoords[atomNum])]);
					if (haveCharges) atom.setCharge(formalCharges[atomNum]);
					if (haveColors) atom.setColor(colorSet[colors[atomNum]]);
					this.addAtom(atom);
				} // for each atom
			} // if the atoms are listed as an attribute
		} // if the atom array exists
	}; // parseAtoms()

	this.parseBonds = function(xmlDoc) {
		var bondArray = xmlDoc.getElementsByTagName(BOND_ARRAY);
		if (!isEmpty(bondArray)) {
			var bondNodes = bondArray[0].childNodes;
			for (var bondNum = 0; bondNum < bondNodes.length; bondNum++) {
				var bondNode = bondNodes[bondNum];
				if (bondNode.getAttribute) {
					var bond = new Bond(this);
					var bondAtomNums = bond.parseMRVForAtomNums(bondNode);
					if (bondAtomNums.length === 2) {
						var atom0Num = bondAtomNums[0];
						var atom1Num = bondAtomNums[1];
						var atom0 = this.getAtom(atom0Num);
						var atom1 = this.getAtom(atom1Num);
						bond.parseMRVFinish(bondNode, [atom0, atom1]);
						this.addBond(bond);
						atom0.reportAddedBond(atom1.getPosn());
						atom1.reportAddedBond(atom0.getPosn());
						var stereoNodes = 
								bondNode.getElementsByTagName(BOND_STEREO);
						if (!isEmpty(stereoNodes)) {
							var stereoNode = stereoNodes[0];
							if (stereoNode.getAttribute('convention')) {
								bond.setStereo(STEREOS[WAVY]);
							} else {
								var letterValue = 
										stereoNode.childNodes[0].nodeValue;
								bond.setStereo(STEREOS[
										STEREO_MRVS.indexOf(letterValue)]);
							} // if stereo node has an attribute
						} // if there is a stereochemistry node
					} // if the bond has two atoms
				} // if this node is really a node
			} // for each bond
		} // if the bond array exists
	}; // parseBonds()

	this.toMRV = function(forImage) {
		var CML_TAG = 'cml';
		var xml = '';
		var xmlDoc = getParentXMLDoc(CML_TAG);
		if (this.getNumAtoms() > 0) {
			var cmlNode = getFirstNode(xmlDoc, CML_TAG);
			// add MDocument node to cml node
			var mDocNode = addNewNode(xmlDoc, cmlNode, MDOCUMENT, 
					this.getMDocumentAttrs());
			// add MChemicalStruct node to MDocument node
			var mChemStructNode = addNewNode(xmlDoc, mDocNode, 
					'MChemicalStruct');
			this.addToMChemicalStructNode(xmlDoc, mChemStructNode, forImage);
			if (forImage) {
				for (var atomNum = 0; atomNum < atoms.length; atomNum++) {
					// add textboxes with electrons of atom
					atoms[atomNum].addElectronImageNodes(xmlDoc, mDocNode);
				} // for each atom
			} // if for image
			xml = new String.builder().
					append('<?xml version="1.0" encoding="UTF-8"?>\n').
					append(getXML(xmlDoc, MAKE_READABLE)).toString();
		} // if there are atoms
		return xml;
	}; // toMRV()

	this.getMDocumentAttrs = function() {
		var attrs = [];
		if (colorSet.length > 1) {
			var colorBld = new String.builder().append('0:N');
			for (var colorNum = 1; colorNum < colorSet.length; colorNum++) {
				colorBld.append(',').
						append(colorNum).append(':').
						append(colorSet[colorNum]);
			} // for each color
			attrs.push([COLOR_SET, colorBld.toString()]);
		} // if there are colors
		return attrs;
	} // getMDocumentAttrs()

	this.addToMChemicalStructNode = function(xmlDoc, mChemStructNode, 
			forImage) {
		// add molecule node to MChemicalStructure node
		var attrs = [];
		attrs.push(['molID', 'm1']);
		var moleculeNode = addNewNode(xmlDoc, mChemStructNode, 'molecule', attrs);
		// add property list node to molecule node
		var propListNode = addNewNode(xmlDoc, moleculeNode, 'propertyList');
		// add property node to property list node
		attrs = [];
		attrs.push(['dictRef', LEWIS_PROPERTY]);
		attrs.push(['title', LEWIS_PROPERTY]);
		var lewisPropertyNode = addNewNode(xmlDoc, propListNode, 'property', 
				attrs);
		var scalarNode = addNewNode(xmlDoc, lewisPropertyNode, 'scalar');
		scalarNode.appendChild(getTextNode(xmlDoc, 'true'));
		// add atom array node to molecule node
		var atomArrayNode = addNewNode(xmlDoc, moleculeNode, ATOM_ARRAY);
		// add atom nodes to atomArray node
		for (var atomNum = 0; atomNum < atoms.length; atomNum++) {
			atomArrayNode.appendChild(atoms[atomNum].toNode(
					xmlDoc, atomNum + 1, colorSet, forImage));
		} // for each atom
		// add bond array node to molecule node
		var bondArrayNode = addNewNode(xmlDoc, moleculeNode, BOND_ARRAY);
		// add bond nodes to bondArray node
		for (var bondNum = 0; bondNum < bonds.length; bondNum++) {
			bondArrayNode.appendChild(bonds[bondNum].toNode(xmlDoc));
		} // for each bond
	}; // addToMChemicalStructNode()

	/* drawing methods */
	this.draw = function(drawingBuffer) {
		for (var atomNum = 0; atomNum < atoms.length; atomNum++) {
			atoms[atomNum].draw(drawingBuffer);
		} // for each atom
		for (var bondNum = 0; bondNum < bonds.length; bondNum++) {
			bonds[bondNum].draw(drawingBuffer);
		} // for each bond
	}; // draw()

	this.toString = function() {
		var bld = new String.builder().
				append('Lewis molecule has ').
				append(atoms.length).append(' atom(s) and ').
				append(bonds.length).append(' bond(s):\n');
		for (var atomNum = 0; atomNum < atoms.length; atomNum++) {
			bld.append(atoms[atomNum].toString()).append('\n');
		} // for each atom
		for (var bondNum = 0; bondNum < bonds.length; bondNum++) {
			bld.append(bonds[bondNum].toString()).append('\n');
		} // for each bond
		return bld.toString();
	}; // toString()

} // Molecule()


/* An atom. */

function Atom(myParent) {
	/* these are the members */
	var element = 'C';
	var posn = [100, 100];
	var charge = 0;
	var color = BLACK;
	var myOctants = new Octants();
	var parentMol = myParent;
	var highlighted = false;

	/* get methods */
	this.getElement = function() { return element; };
	this.getPosn = function() { return posn; };
	this.getCharge = function() { return charge; };
	this.getColor = function() { return color; };
	this.isHighlighted = function() { return highlighted; };
	this.getNumPairedElectrons = function() { 
		return myOctants.getNumPairedElectrons(); 
	}; // getNumPairedElectrons()
	this.getNumUnpairedElectrons = function() { 
		return myOctants.getNumUnpairedElectrons(); 
	}; // getNumUnpairedElectrons()

	// Gets the number of items in an octant of this atom other than bonds.
	this.getOctantNumItems = function(whichOctant) {
		return myOctants.getNumItemsInOctant(whichOctant);
	}; // getOctantNumItems()

	// Gets the octant of this atom in which a point lies.
	this.getOctantForPoint = function(coords)  {
		return myOctants.getOctant(posn, coords);
	}; // getOctantForPoint()

	/* editing methods */
	this.setColor = function(col) { color = col; };
	this.setPosn = function(arr) { posn = arr; };
	this.changeCharge = function(amt) { this.setCharge(charge + Math.round(amt)); };
	this.setHighlighted = function(hl) { highlighted = hl; };

	this.setElement = function(elem) { 
		element = elem;
		color = (isEmpty(ATOM_COLORS[element]) ?
			DEFAULT_ATOM_COLOR : ATOM_COLORS[element]);
		this.setHighlighted(false);
	}; // setElement()

	this.setCharge = function(newChg) { 
		var newCharge = Math.round(newChg);
		if (newCharge !== charge) {
			this.reportChargeChange(charge, newCharge);
			charge = newCharge;
		} // if charge has changed
		this.setHighlighted(false);
	}; // setCharge()

	this.move = function(newPosn, bondedAtoms) {
		this.setPosn(newPosn);
		var bondedAtomsPosns = [];
		for (var atomNum = 0; atomNum < bondedAtoms.length; atomNum++) {
			var bondedAtom = bondedAtoms[atomNum];
			var bondedAtoms2 = parentMol.getBondedAtoms(bondedAtom);
			var bondedAtomPosns2 = [];
			for (var atomNum2 = 0; atomNum2 < bondedAtoms2.length; atomNum2++) {
				var bondedAtom2 = bondedAtoms2[atomNum2];
				bondedAtomPosns2.push(bondedAtom2.getPosn());
			} // for each atom attached to the bonded atom
			bondedAtom.redistributeOctants(bondedAtomPosns2);
			bondedAtomsPosns.push(bondedAtom.getPosn());
		} // for each bonded atom
		this.redistributeOctants(bondedAtomsPosns);
	}; // move()

	this.setLoneElectrons = function(pairedUnpairedArr) {
		myOctants.setLoneElectrons(pairedUnpairedArr);
	}; // setLoneElectrons()
	this.changeElectrons = function(delta) {
		myOctants.changeElectrons(delta);
		this.setHighlighted(false);
	}; // changeElectrons()

	// Report changes in bonds, charges, or position in case electrons need 
	// to rearrange themselves. 
	this.reportAddedBond = function(bondedAtomPosn) {
		myOctants.reportAddedBond(posn, bondedAtomPosn);
	}; // reportAddedBond()
	this.reportDeletedBond = function(bondedAtomPosn) {
		myOctants.reportDeletedBond(posn, bondedAtomPosn);
	}; // reportDeletedBond()
	this.reportChargeChange = function(oldCharge, newCharge) {
		myOctants.reportChargeChange(oldCharge, newCharge);
	}; // reportChargeChange()
	this.redistributeOctants = function(bondedAtomPosns) {
		myOctants.redistribute(posn, bondedAtomPosns, charge);
	}; // redistributeOctants()

	/* MRV methods */
	this.parseMRV = function(node) {
		this.setElement(node.getAttribute(ELEMENT));
		this.setCharge(parseToInt(node.getAttribute(FORMAL_CHARGE)));
		this.setPosn([parseToInt(node.getAttribute(X_COORD)),
				parseToInt(node.getAttribute(Y_COORD))]);
		var electronsArr = [0, 0];
		var scalarNodes = node.getElementsByTagName('scalar');
		for (var nodeNum = 0; nodeNum < scalarNodes.length; nodeNum++) {
			var scalarNode = scalarNodes[nodeNum];
			var title = scalarNode.getAttribute('title');
			if ([PAIRED_TITLE, UNPAIRED_TITLE, UNSHARED_TITLE].contains(title)) {
				var numElectrons = (scalarNode.childNodes &&
							scalarNode.childNodes.length > 0 ?
						parseToInt(scalarNode.childNodes[0].nodeValue) :
						parseToInt(scalarNode.getAttribute('value')));
				if (title === PAIRED_TITLE) {
					electronsArr[0] = numElectrons;
				} else if (title === UNPAIRED_TITLE) {
					electronsArr[1] = numElectrons;
				} else {
					var numUnpaired = numElectrons % 2;
					electronsArr = [numElectrons - numUnpaired, numUnpaired];
				} // if title
			} else if (title === HIGHLIGHT) {
				var highlightValue = (scalarNode.childNodes &&
							scalarNode.childNodes.length > 0 ?
						scalarNode.childNodes[0].nodeValue :
						scalarNode.getAttribute('value'));
				this.setHighlighted(highlightValue === HIGHLIGHT);
			} // if title
		} // for each scalar node
		this.setLoneElectrons(electronsArr);
		return parseToInt(node.getAttribute(COLOR_NUM));
	}; // parseMRV()

	this.toNode = function(xmlDoc, atomNum, colorSet, forImage) {
		var ATOM_TITLE = 'atom',
			PAIRED = 0,
			UNPAIRED = 1,
			numElecs = [this.getNumPairedElectrons(),
				this.getNumUnpairedElectrons()],
			atomAttrs = [],
			elecType,
			scalarNode,
			scalarAttrs;
		atomAttrs.push(['id', atomId(atomNum)]);
		atomAttrs.push([ELEMENT, element]);
		if (charge !== 0 && !forImage) {
			atomAttrs.push([FORMAL_CHARGE, charge]);
		} // if formal charge should be added
		var colorNum = colorSet.indexOf(color);
		if (colorNum >= 1) {
			atomAttrs.push([COLOR_NUM, colorNum]);
		} // if atom is colored
		if (numElecs[UNPAIRED] > 0 && !forImage) {
			atomAttrs.push(['radical', 
					numElecs[UNPAIRED] === 3 ? 'trivalent' :
					numElecs[UNPAIRED] === 2 ? 'divalent' :
					'monovalent']);
		} // if radicals
		var scale = (forImage ? MARVIN_WIDTH / CANVAS_DIMS[offsets.X] : 1);
		atomAttrs.push([X_COORD, posn[offsets.X] * scale]);
		atomAttrs.push([Y_COORD, posn[offsets.Y] * scale * (forImage ? -1 : 1)]);
		var atomNode = getNewNode(xmlDoc, ATOM_TITLE, atomAttrs);
		if (!forImage && !arraysMatch(numElecs, [0, 0])) {
			for (elecType = 0; elecType < 2; elecType++) {
				if (numElecs[elecType] !== 0) {
					scalarAttrs = [];
					scalarAttrs.push(['id', atomId(atomNum) + ':prop1']);
					scalarAttrs.push(['title', elecType === PAIRED  ?
							PAIRED_TITLE : UNPAIRED_TITLE]);
					scalarAttrs.push(['convention', 'marvin:atomprop']);
					scalarAttrs.push(['dataType', 'xsd:integer']);
					scalarAttrs.push(['value', numElecs[elecType]]);
					addNewNode(xmlDoc, atomNode, 'scalar', scalarAttrs);
				} // if there are electrons of this type
			} // for each type of unshared electron
		} // if there are unshared electrons
		if (this.isHighlighted()) {
			scalarAttrs = [];
			scalarAttrs.push(['id', atomId(atomNum) + ':prop1']);
			scalarAttrs.push(['title', HIGHLIGHT]);
			scalarAttrs.push(['convention', 'marvin:atomprop']);
			scalarAttrs.push(['dataType', 'xsd:string']);
			scalarNode = addNewNode(xmlDoc, atomNode, 'scalar', scalarAttrs);
			scalarNode.appendChild(getTextNode(xmlDoc, HIGHLIGHT));
		} // if atom is highlighted
		return atomNode;
	}; // toNode()

	// Gets the MRV for the charge and electrons, for display in an image.
	this.addElectronImageNodes = function(xmlDoc, mDocNode) {
		var elementDisplay = element;
		if (showAtomNumbers) elementDisplay += parentMol.getNumOfAtom(this);
		var halfStringLen = floor(getTextSize(elementDisplay) / 2) - 3;
		myOctants.addImageNodes(xmlDoc, mDocNode, posn, charge, color, 
				floor(halfStringLen / 2));
	}; // addElectronImageNodes()

	/* drawing methods */
	this.draw = function(drawingBuffer) {
		var atomColor = BLACK;
		if (isEmpty(color)) {
			atomColor = ATOM_COLORS[element];
			if (isEmpty(atomColor)) atomColor = DEFAULT_ATOM_COLOR;
		} else atomColor = color;
		setGraphics('12px', atomColor);
		if (this.isHighlighted()) {
			drawingBuffer.setBackgroundColor('cyan');
		} // if highlighted
		var elementDisplay = element;
		if (showAtomNumbers) elementDisplay += parentMol.getNumOfAtom(this);
		var halfStringLen = floor(getTextSize(elementDisplay) / 2) - 3;
		drawingBuffer.drawString(elementDisplay,
				offsetTextX(posn[offsets.X] - halfStringLen), 
				offsetTextY(posn[offsets.Y]));
		drawingBuffer.setBackgroundColor('none');
		if (charge !== 0) {
			myOctants.drawCharge(drawingBuffer, charge, posn);
		}
		setGraphics('14px', BLACK);
		myOctants.drawElectrons(drawingBuffer, posn, halfStringLen);
		setGraphicsToDefault();
	}; // draw()

	// Changes the position of the atom when the window is resized.
	this.canvasReset = function() {
		this.setPosn(canvasReset(this.getPosn()));
	}; // canvasReset()

	this.toString = function() {
		var bld = new String.builder().
				append('Atom ').append(element).
				append(parentMol.getNumOfAtom(this)).
				append(' has position [').append(posn).
				append('], charge of ').append(charge);
		if (color !== BLACK) {
			bld.append(', color ').append(color);
		} // if the atom is colored
		bld.append(', and ').append(this.getNumPairedElectrons()).
				append(' paired and ').append(this.getNumUnpairedElectrons()).
				append(' unpaired electrons.');
		return bld.toString();
	}; // toString()

} // Atom()


/* A bond. */

function Bond(myParent) {
	/* these are the members */
	var atoms = [];
	var order = 1;
	var stereo = STEREOS[PLAIN];
	var parentMol = myParent;

	/* get methods */
	this.getAtoms = function() { return this.atoms; };
	this.getOrder = function() { return order; };
	this.getStereo = function() { return stereo; };
	this.hasAtom = function(atom) { 
		return atom === this.atoms[0] || atom === this.atoms[1];
	}; // hasAtom()

	/* editing methods */
	this.setAtoms = function(ats) { this.atoms = ats; };
	this.setOrder = function(newOrder) { order = newOrder; };
	this.setStereo = function(newStereo) { stereo = newStereo; };
	this.increaseOrder = function() { 
		order = (order === 3 ? 1 : order + 1); 
		stereo = STEREOS[PLAIN];
	}; // increaseOrder()
	this.toggleOrder = function() { 
		order = (order === 3 ? 1 : 3 - order); 
		stereo = STEREOS[PLAIN];
	}; // toggleOrder()
	this.switchDirection = function() {
		var switched = [this.atoms[1], this.atoms[0]];
		this.setAtoms(switched);
	}; // switchDirection()

	/* MRV methods */
	this.parseMRVForAtomNums = function(node) {
		var atomNumsStr = node.getAttribute(ATOMS_OF_BOND);
		var atomNumStrs = atomNumsStr.split(' ');
		var bondAtomNums = [];
		for (var bndAtNum = 0; bndAtNum < atomNumStrs.length; bndAtNum++) {
			var atomNum = parseToInt(atomNumStrs[bndAtNum].substring(1));
			bondAtomNums.push(atomNum);
		} // for each atom of the bond
		return bondAtomNums;
	}; // parseMRVForAtomNums()

	this.parseMRVFinish = function(node, ats) {
		this.setAtoms(ats);
		this.setOrder(parseInt(node.getAttribute('order'), 10));
	}; // this.parseMRVFinish()

	this.makeAtomsOfBondXML = function() {
		var bld = new String.builder().
				append(atomId(parentMol.getNumOfAtom(this.atoms[0]))).
				append(' ').
				append(atomId(parentMol.getNumOfAtom(this.atoms[1])));
		return bld.toString();
	}; // makeAtomsOfBondXML()

	this.toNode = function(xmlDoc) {
		var attrs = [],
			isWavy,
			stereoNode,
			letterValue;
		attrs.push([ATOMS_OF_BOND, this.makeAtomsOfBondXML()]);
		attrs.push(['order', order]);
		var bondNode = getNewNode(xmlDoc, 'bond', attrs);
		var stereo = this.getStereo();
		if (stereo !== STEREOS[PLAIN]) {
			isWavy = stereo === STEREOS[WAVY];
			attrs = [];
			if (isWavy) {
				attrs.push(['convention', 'MDL']);
				attrs.push(['conventionValue', 4]);
			} // if wavy
			stereoNode = addNewNode(xmlDoc, bondNode, BOND_STEREO, attrs);
			if (!isWavy) {
				letterValue = STEREO_MRVS[STEREOS.indexOf(stereo)];
				stereoNode.appendChild(getTextNode(xmlDoc, letterValue));
			} // if not wavy
		} // if there is stereochemistry
		return bondNode;
	} // toNode()

	/* drawing methods */
	this.draw = function(drawingBuffer) {
		var atoms = this.atoms;
		var atom0Element = atoms[0].getElement();
		var atom1Element = atoms[1].getElement();
		var atom0Len = getTextSize(atom0Element +
				(showAtomNumbers ? parentMol.getNumOfAtom(atoms[0]) : ''));
		var atom1Len = getTextSize(atom1Element +
				(showAtomNumbers ? parentMol.getNumOfAtom(atoms[1]) : ''));
		var atom0Posn = atoms[0].getPosn();
		var atom1Posn = atoms[1].getPosn();
		// end the lines near the elements, not in their center
		var vector = getVector(atom0Posn, atom1Posn);
		var bondAngle = getAngle(vector);
		var startAdjustment = 
				[Math.cos(bondAngle) * ATOM_BOUND * atom0Len / 10,
				-Math.sin(bondAngle) * ATOM_BOUND];
		var atom0Start = arraysSum(atom0Posn, arrayRound(startAdjustment));
		startAdjustment[offsets.X] = 
				round(startAdjustment[offsets.X] * atom1Len / atom0Len);
		var atom1Start = arraysDiff(atom1Posn, startAdjustment);
		var midpoint1, midpoint2, sign;
		if (order === 3 || (order === 1 && stereo === STEREOS[PLAIN])) {
			this.drawLine(atom0Start, atom1Start, drawingBuffer);
		} else if (order === 1) { // stereobond
			// calculate vertices of a wedge between the atoms
			var wedgeVertices = [atom0Start];
			for (sign = -1; sign <= 1; sign += 2) {
				var offsetAngle = bondAngle + sign * Math.PI / 2;
				var offset = scalarProd([Math.cos(offsetAngle),
						-Math.sin(offsetAngle)], WEDGE_WIDTH);
				var atom1Vertex = arraysSum(atom1Start, offset);
				wedgeVertices.push(arrayRound(atom1Vertex));
			} // for second and third vertices
			if (stereo === STEREOS[BOLD]) {
				// draw and fill in the wedge
				var wedgeVerticesX = [];
				var wedgeVerticesY = [];
				for (var vectorNum = 0; vectorNum < 3; vectorNum++) {
					wedgeVerticesX.push(wedgeVertices[vectorNum][offsets.X]);
					wedgeVerticesY.push(wedgeVertices[vectorNum][offsets.Y]);
				} // for each vertex
				this.setLineColor(atom1Element);
				drawingBuffer.fillPolygon(wedgeVerticesX, wedgeVerticesY);
				midpoint1 = getMidpoint(wedgeVertices[0], wedgeVertices[1]);
				midpoint2 = getMidpoint(wedgeVertices[0], wedgeVertices[2]);
				wedgeVerticesX[1] = midpoint1[offsets.X];
				wedgeVerticesY[1] = midpoint1[offsets.Y];
				wedgeVerticesX[2] = midpoint2[offsets.X];
				wedgeVerticesY[2] = midpoint2[offsets.Y];
				this.setLineColor(atom0Element);
				drawingBuffer.fillPolygon(wedgeVerticesX, wedgeVerticesY);
			} else { // HASH or WAVY
				// get vectors from atom0 to each vertex near atom1
				var vector1 = getVector(wedgeVertices[0], wedgeVertices[1]);
				var vector2 = getVector(wedgeVertices[0], wedgeVertices[2]);
				var angle1 = getAngle(vector1);
				var angle2 = getAngle(vector2);
				var point1 = [wedgeVertices[0][offsets.X],
				wedgeVertices[0][offsets.Y]]; 
				var point2 = [wedgeVertices[0][offsets.X],
				wedgeVertices[0][offsets.Y]]; 
				// calculate change in coordinates to move points along each vector
				var increment1 = [Math.cos(angle1) * WEDGE_WIDTH,
						-Math.sin(angle1) * WEDGE_WIDTH];
				var increment2 = [Math.cos(angle2) * WEDGE_WIDTH,
						-Math.sin(angle2) * WEDGE_WIDTH];
				this.setLineColor(atom0Element, 1);
				var colorHasChanged = false;
				if (stereo === STEREOS[HASH]) {
					// draw lines from points along vector1 with corresponding
					// points along vector2
					while (true) {
						drawingBuffer.drawLine(
								round(point1[offsets.X]), 
								round(point1[offsets.Y]), 
								round(point2[offsets.X]), 
								round(point2[offsets.Y]));
						if (!colorHasChanged &&
								(getDistance(wedgeVertices[0], point1) >
									getDistance(point1, wedgeVertices[1]))) {
							this.setLineColor(atom1Element, 1);
							colorHasChanged = true;
						} // if color should change
						point1 = arraysSum(point1, increment1);
						point2 = arraysSum(point2, increment2);
						if (!isBetween(wedgeVertices[0], 
									wedgeVertices[1], point1) ||
								!isBetween(wedgeVertices[0], 
									wedgeVertices[2], point2)) {
							break;
						} // if point is not between endpoints
					} // while point is between the two endpoints
				} else { // WAVY
					// get alternating points along the two vectors,
					// then draw lines connecting them
					var vectorOfPoint = 2;
					var pointsX = [wedgeVertices[0][offsets.X]];
					var pointsY = [wedgeVertices[0][offsets.Y]];
					point1 = arraysSum(point1, increment1);
					increment1 = scalarProd(increment1, 2);
					increment2 = scalarProd(increment2, 2);
					midpoint1 = getMidpoint(wedgeVertices[0], wedgeVertices[1]);
					midpoint2 = getMidpoint(wedgeVertices[0], wedgeVertices[2]);
					for (var colorNum = 0; colorNum < 2; colorNum++) {
						while (true) {
							if (vectorOfPoint === 1) {
								pointsX.push(round(point2[offsets.X]));
								pointsY.push(round(point2[offsets.Y]));
								point1 = arraysSum(point1, increment1);
							} else {
								pointsX.push(round(point1[offsets.X]));
								pointsY.push(round(point1[offsets.Y]));
								point2 = arraysSum(point2, increment2);
							} // if toggle
							if (!isBetween(wedgeVertices[0], 
										wedgeVertices[1], point1) ||
									!isBetween(wedgeVertices[0], 
										wedgeVertices[2], point2)) {
								break;
							} // if point is not between endpoints
							vectorOfPoint = 3 - vectorOfPoint;
							if (colorNum === 0 &&
									(getDistance(wedgeVertices[0], point1) >
										getDistance(point1,
										wedgeVertices[1]) ||
									getDistance(wedgeVertices[0], point2) >
										getDistance(point2, wedgeVertices[2]))) 
							{
								break;
							} // if time to change color
						} // while point is between the two endpoints
						drawingBuffer.drawPolyline(pointsX, pointsY);
						pointsX = [pointsX.pop()];
						pointsY = [pointsY.pop()];
						this.setLineColor(atom1Element, 1);
					} // for each color
				} // if stereo
			} // if stereo
		} // if bond is triple or plain single
		// no "else" here!
		if (order > 1) {
			var moveDistance = BOND_SPACING / (4 - order);
			for (sign = -1; sign <= 1; sign += 2) {
				var moveAngle = bondAngle + sign * Math.PI / 2;
				var move = arrayRound(scalarProd([Math.cos(moveAngle),
						-Math.sin(moveAngle)], moveDistance));
				var atom0NewStart = arraysSum(atom0Start, move);
				var atom1NewStart = arraysSum(atom1Start, move);
				this.drawLine(atom0NewStart, atom1NewStart, drawingBuffer);
			} // +/-
		} // if bond is multiple
		setGraphicsToDefault(jsGraphics);
	}; // draw()

	// Draws a line representing a bond (or part of one) in the colors of the
	// atoms of the bond.
	this.drawLine = function(atom0Start, atom1Start, drawingBuffer) {
		var midPoint = getMidpoint(atom0Start, atom1Start);
		this.setLineColor(this.atoms[0].getElement());
		drawingBuffer.drawLine(
				atom0Start[offsets.X], 
				atom0Start[offsets.Y], 
				midPoint[offsets.X], 
				midPoint[offsets.Y]);
		this.setLineColor(this.atoms[1].getElement());
		drawingBuffer.drawLine(
				atom1Start[offsets.X], 
				atom1Start[offsets.Y], 
				midPoint[offsets.X], 
				midPoint[offsets.Y]);
	}; // drawLine()

	// Changes the line color to that of the element.
	this.setLineColor = function(element, stroke) {
		var bondColor = ATOM_COLORS[element];
		if (isEmpty(bondColor)) bondColor = DEFAULT_ATOM_COLOR;
		setGraphics('12px', bondColor, stroke);
	}; // setLineColor()

	this.toString = function() {
		var bld = new String.builder().
				append('Bond ').append(parentMol.getNumOfBond(this)).
				append(' connects ').append(this.atoms[0].getElement()).
				append(parentMol.getNumOfAtom(this.atoms[0])).
				append(' and ').append(this.atoms[1].getElement()).
				append(parentMol.getNumOfAtom(this.atoms[1])).
				append(' with order ').append(order);
		return bld.toString();
	}; // toString()

} // Bond()


/* The area around an atom is divided into directional octants (N, S, E, W,
 * etc.) into which bonds, lone electrons and a charge may be placed. The purpose of this
 * class is to keep track of where bonds are placed, determine where electrons
 * and the charge are placed, and display the electrons and charge for an atom.  */

function Octants() {
	
	/* ************ Constants *************/

	// Number of octants around an atom.
	var NUM_OCTANTS = 8;
	// Directional constants used to index bondCount[] and electronCount[]
	// arrays as well as identify a particular octant.  N is down on the
	// MarvinJS canvas!
	var N = 0;
	var NE = 1;
	var E = 2;
	var SE = 3;
	var S = 4;
	var SW = 5;
	var W = 6;
	var NW = 7;
	var UNDEFINED_OCTANT = -1;
	// Octants are filled with electrons by finding a starting octant,
	// then proceeding according to the order given in this array, whose values
	// are offsets from the start octant. The first offset should always be
	// 0 so that filling actually starts in the start octant.
	// example: if fillStartOctant = 3, then the order for filling
	//		  octants is:			  [3,5,1,7,4,2,6,0]
	//
	var FILL_ORDER_OFFSETS = [0, 2, -2, 4, 1, -1, 3, -3];
	// Constants for drawing lone electrons. A "shell" is a circle of electrons
	// around an atom. When more than one shell is needed, they are concentric
	// circles with an increasing radius. Constant values were determined
	// empirically (they look good when displayed).
	//
	var ELECTRONS_PER_SHELL = 2;
	var ELECTRON_SEPARATION = 3;
	var BASE_SHELL_RADIUS = 12;
	var SHELL_SEPARATION = 5;

	// Constant for making MRV of charge and lone electrons.
	var TEXT_BOX_WIDTH = 1;

	/* these are the members */
	var loneElectrons = new Electrons(); 
			// holds coordinates of the electrons of this atom's octants
	var bondCount = getArray(9); // counts of number of bonds in each octant 
	var electronCount = getArray(9); // counts of number of electrons in each octant 
	var chargeOctant = UNDEFINED_OCTANT; // Octant in which to display charge
	var allOctantsFull = false;	// Are all octants full of bonds? 
	var fillOrderIndex = 0;	// Current index of FILL_ORDER_OFFSETS[] array
	
	/* Short methods */
	this.getChargeOctant = function() { return chargeOctant; };
	this.getNumPairedElectrons = function() { 
		return loneElectrons.getPaired(); 
	}; // getNumPairedElectrons()
	this.getNumUnpairedElectrons = function() { 
		return loneElectrons.getUnpaired(); 
	}; // getNumUnpairedElectrons()
	this.incrementOct = function(octNum) { 
		return (octNum + 1) % NUM_OCTANTS; 
	}; // incrementOct()
	this.decrementOct = function(octNum) { 
		return (octNum === 0 ? NUM_OCTANTS : octNum) - 1; 
	}; // decrementOct()

	this.borderSlope = function(octant1, octant2) {
		return (octant1 === N && octant2 === S ? 2.4143 :
				octant1 === NE && octant2 === SW ? 0.4143 :
				octant1 === E && octant2 === W ? -0.4143 :
				-2.4143);
	}; // borderSlope()

	// Given an atom's position and a point, returns octant of the given 
	// point relative to the atom's position.
	this.getOctant = function(posn, point) {
		// calc slope of line from atom to given point
		var slope = point[offsets.X] === posn[offsets.X] ?
				0 : // won't use in this case, but avoid divide error
				(point[offsets.Y] - posn[offsets.Y]) /
					(point[offsets.X] - posn[offsets.X]);
		// determine octant based on slope and x,y coordinates
		// check against "border slopes" in descending order
		return (posn[offsets.X] === point[offsets.X] ||
			Math.abs(slope) > this.borderSlope(N, S) ?
					(point[offsets.Y] >= posn[offsets.Y] ? N : S) :
				slope > this.borderSlope(NE, SW) ?
					(point[offsets.X] >= posn[offsets.X] ? NE : SW) :
				slope > this.borderSlope(E, W) ?
					(point[offsets.X] >= posn[offsets.X] ? E : W) :
				point[offsets.Y] >= posn[offsets.Y] ? NW : SE);
	}; // getOctant()

	// Determines the octant where charge is placed/removed and redistributes
	// the lone electrons if necessary. 
	this.reportChargeChange = function(oldCharge, newCharge) {
		if (oldCharge === 0) {	// going from no charge to charge
			bondCount[this.findChargeOct()]++; // treat charge as bond for distr. elec.
			this.distributeElectrons();
		} else if (newCharge === 0) { // if charge is disappearing
			bondCount[chargeOctant]--;	// treat as a removed bond
			chargeOctant = UNDEFINED_OCTANT;
			this.distributeElectrons();
		} // else charge is disappearing
	}; // reportChargeChange()

	// Given x,y coordinates of center of this atom and the atom being bonded,
	// along with the number of lone electrons for this atom, the octant of
	// the new bond relative to this atom is determined, and the corresponding
	// bond count is incremented. In case a bond is being added to an octant
	// where none was before, also redistributes electrons and charge.
	this.reportAddedBond = function(myPosn, bondedAtomPosn) {
		var bondOctant = this.getOctant(myPosn, bondedAtomPosn);
		bondCount[bondOctant]++;	// count the new bond
		// if the new bond collides with the charge, move the charge
		if (bondOctant === chargeOctant) {		// must move charge now
			bondCount[chargeOctant]--;	 // don't count charge in this oct
			chargeOctant = this.findChargeOct();// find new octant for charge
			bondCount[chargeOctant]++;	 // count charge in this oct now
		} // if new bond collides with charge
		this.distributeElectrons();
	}; // reportAddedBond()

	// Given x,y coordinates of center of this atom and the atom being unbonded,
	// along with the number of lone electrons for this atom, the octant of
	// the new bond relative to this atom is determined, and the corresponding
	// bond count is decremented. In case a bond is being added to an octant
	// where none was before, also redistributes electrons and charge.
	this.reportDeletedBond = function(myPosn, bondedAtomPosn) {
		var bondOctant = this.getOctant(myPosn, bondedAtomPosn);
		bondCount[bondOctant]--;	// one less bond in this octant now
		if (chargeOctant !== UNDEFINED_OCTANT) {	// if a charge exists
			bondCount[chargeOctant]--;		// take charge out of count
			chargeOctant = this.findChargeOct();  // find new charge octant
			bondCount[chargeOctant]++;		// add charge to bond count
		} // if there even is a charge present
		this.distributeElectrons();
	}; // reportDeletedBond()

	// Completely reconfigure octants given array of (x,y) coordinates of all
	// atoms bonded to this atom, this atom's center, #lone electrons, and
	// charge.
	this.redistribute = function(posn, bondedAtomPosns, charge) {
		zero(bondCount);
		zero(electronCount);
		// for each bond reported, determine which octant it is in and
		// increment the bond count for that octant
		var numBondedAtoms = bondedAtomPosns.length;
		for (var atomIndex = 0; atomIndex < numBondedAtoms; atomIndex++) {
			var octantIndex = this.getOctant(posn, bondedAtomPosns[atomIndex]);
			bondCount[octantIndex]++;
		} // for each bonded atom
		// if there is a charge, "place" it in the correct octant
		if (charge !== 0) {
			chargeOctant = this.findChargeOct();
			bondCount[chargeOctant]++;	// charge counts as a bond
		} else {
			chargeOctant = UNDEFINED_OCTANT;  // there is no charge
		}
		// now ready to distribute lone electrons
		this.distributeElectrons();
	}; // redistribute()

	// Determines the octant where charge is placed (if there is a charge).
	// Place in octant with no bonds if possible.  Upper right octant (SE) 
	// is preferred and default.  NEVER place in left, right, or lower right 
	// (E, W, or NE) octant by order of the gods of Chemistry.
	this.findChargeOct = function() {
		return (bondCount[SE] === 0 ? SE :
				bondCount[SW] === 0 ? SW :
				bondCount[S] === 0 ? S :
				bondCount[N] === 0 ? N :
				bondCount[NW] === 0 ? NW :
				SE); // conflicts with bond, but oh well!
	}; // findChargeOct()

	// Sets the number of unshared electrons in the octants, distributes them
	// into the various octants. 
	this.setLoneElectrons = function(pairedUnpaired) {
		loneElectrons.setPaired(pairedUnpaired[0]);
		loneElectrons.setUnpaired(pairedUnpaired[1]);
		this.distributeElectrons();
	}; // setLoneElectrons()

	// Adds or removes unshared electrons, distributes them
	// into the various octants. 
	this.changeElectrons = function(delta) {
		var MAX_UNPAIRED = 4;
		var currentPaired = this.getNumPairedElectrons();
		var currentUnpaired = this.getNumUnpairedElectrons();
		if (currentPaired + currentUnpaired === 0 && delta < 0) return;
		if (delta % 2 === 0) {
			if (delta > 0 || currentPaired >= -delta) {
				loneElectrons.setPaired(delta + currentPaired);
			} else {
				loneElectrons.setUnpaired(delta + currentUnpaired);
			} // if delta
		} else {
			if (currentUnpaired === MAX_UNPAIRED && delta === 1) {
				// must pair one unpaired electron
				loneElectrons.setUnpaired(MAX_UNPAIRED - 1);
				loneElectrons.setPaired(currentPaired + 2);
			} else if (currentUnpaired === 0 && delta === -1) {
				// must remove a paired electron
				loneElectrons.setUnpaired(1);
				loneElectrons.setPaired(currentPaired - 2);
			} else {
				loneElectrons.setUnpaired(currentUnpaired + delta);
			} // if currentUnpaired
		} // if adding even/odd number
		this.distributeElectrons();
	}; // changeElectrons()

	// Gets the number of bonds and the charge in an octant (not the number of
	// unshared electrons). 
	this.getNumItemsInOctant = function(octantIndex) {
		return (octantIndex < 0 || octantIndex >= NUM_OCTANTS ?
				UNDEFINED_OCTANT : bondCount[octantIndex]);
	}; // getNumItemsInOctant()

	// Distributes current number of lone electrons into octants around the atom.
	// Basic Algorithm:
	// 1. If all octants have bonds, simply place electrons starting at S,
	// two at a time in each octant, until all are full. Repeat placing pairs
	// clockwise until all electrons are placed
	// 2. If at least one octant has no bond, place electrons in non-bond octants
	// as far away from bonds as possible. Fill all available octants with
	// pairs before adding more than one pair per available octant. Never
	// place lone electrons in same octant as a bond.
	//
	// Start with octant "furthest" from bonds (middle of biggest set of
	// octants without bonds - a "gap"). 
	this.distributeElectrons = function() {
		var numPairedElecs = loneElectrons.getPaired();
		var numUnpairedElecs = loneElectrons.getUnpaired();
		var numElectrons = numUnpairedElecs + numPairedElecs;
		if (numElectrons === 0) return; // nothing to do
		allOctantsFull = this.allOctantsFull();
		zero(electronCount); // clear current electron octant assignments
		var electronsPerOctant = 2; // start with at most 2 electrons per octant
		// find the best octant to start placing electrons
		var fillStartOctant = 
				(allOctantsFull ? N : this.middleOfWidestOctantGap());
		// init values for use by findNextOctant
		var currentOctant = fillStartOctant;
		fillOrderIndex = 0; // reset fill-order index to start filling octants
		// place all electrons in some octant
		for (var elecNum = 0; elecNum < numElectrons; elecNum++) {
			var placingUnpaired = elecNum >= numPairedElecs;
			if (electronCount[currentOctant] >= electronsPerOctant ||
					(placingUnpaired &&
						electronCount[currentOctant] % 2 === 1)) {
				// find the next suitable octant
				currentOctant = UNDEFINED_OCTANT;
				// search for next available octant according to 
				// order given in FILL_ORDER_OFFSETS[], whose values 
				// are offsets from the start octant.
				while (currentOctant === UNDEFINED_OCTANT) {
					// get next offset to determine next octant to check
					fillOrderIndex++;
					if (fillOrderIndex >= NUM_OCTANTS) {
						// all octants in current shell are full
						electronsPerOctant += 2; // start filling next shell
						fillOrderIndex = 0; // start over in order list
					} // if shell full
					// apply fill-order offset to start octant and ensure new
					// octantIndex is available
					var octantIndex = (fillStartOctant +
							FILL_ORDER_OFFSETS[fillOrderIndex] +
							NUM_OCTANTS) % NUM_OCTANTS;
					// check if this octant is available and there is room
					if ((bondCount[octantIndex] === 0 || allOctantsFull) &&
							electronCount[octantIndex] < electronsPerOctant) {
						currentOctant = octantIndex;
					}
				} // while new current octant not found
			} // if can't fit more electrons in the current octant
			electronCount[currentOctant]++;
		} // for each lone electron
	}; // distributeElectrons()

	// Makes a string naming the empty octants.
	this.printEmptyOctants = function() {
		var bld = new String.builder();
		for (var octNum = 0; octNum < NUM_OCTANTS; octNum++) {
			if (bondCount[octNum] === 0) {
				bld.append(this.getOctantName(octNum)).append(' ');
			} // if there are no bonds or charge in this octant
		} // for each octant
		return bld.toString();
	}; // printEmptyOctants

	// Do ALL eight octants have bonds in them? 
	this.allOctantsFull = function() {
		var allFull = true;
		for (var octIndex = 0; octIndex < NUM_OCTANTS; octIndex++) {
			allFull = bondCount[octIndex] !== 0;
			if (!allFull) break;
		} // for each octant
		return allFull;
	}; // allOctantsFull()

	// Find the middle of the widest GAP (no bonds) of octants around the atom. 
	this.middleOfWidestOctantGap = function() {
		// starting in octant 0 (N), find an octant with a bond by
		// skipping over octants without bonds. If there are no bonds,
		// just return south
		var octIndex = 0;
		while (octIndex < NUM_OCTANTS && bondCount[octIndex] === 0) {
			octIndex++;
		} // while
		if (octIndex >= NUM_OCTANTS) return S;
		var firstOctantWithBond = octIndex;	// remember this octant

		// find next octant withOUT a bond by skipping successive octants
		// which do have bonds. Index may lap from 7 to 0 (hence the modulus)
		octIndex = (octIndex + 1) % NUM_OCTANTS;	  // start with next oct
		while (octIndex !== firstOctantWithBond && bondCount[octIndex] !== 0) {
			octIndex = this.incrementOct(octIndex);
		} // while
		if (octIndex === firstOctantWithBond) {	// if we stopped where we started
			return S;				// then all octants have bonds!!
		} // if we stopped where we started
		var gapBegin = octIndex;				 // gap begins HERE!

		// keep going through gap (empty octants) until end of gap found.
		octIndex = this.incrementOct(octIndex);		 // start with next octant
		while (bondCount[octIndex] === 0) {			  // skip empty octants
			octIndex = this.incrementOct(octIndex);
		} // while octants are empty
		var gapEnd = this.decrementOct(octIndex);		// gap ends at previous oct

		// A gap has now been found, from gapBegin to gapEnd! Now look for
		// a bigger gap! Search until firstOctantWithBond is reached again.
		while (octIndex !== firstOctantWithBond) {
			// find beginning of next gap
			octIndex = this.incrementOct(octIndex);  // start with next octant
			while (octIndex !== firstOctantWithBond &&
					bondCount[octIndex] !== 0) {		// skip octants with bonds
				octIndex = this.incrementOct(octIndex);
			} // while empty octant not yet found
			if (bondCount[octIndex] === 0) {		// start of new gap found
				var candidateGapBegin = octIndex; // remember new gap start
				octIndex = this.incrementOct(octIndex);	// begin @ next oct
				while (bondCount[octIndex] === 0) { // skip empty octs
					octIndex = this.incrementOct(octIndex);
				} // while octants are empty
				var candidateGapEnd = this.decrementOct(octIndex);
				// check if new gap is bigger than old gap
				if (this.gapSize(candidateGapBegin, candidateGapEnd) >
						this.gapSize(gapBegin, gapEnd)) {
					gapBegin = candidateGapBegin;
					gapEnd = candidateGapEnd;
				} // if new biggest gap found
			} // if new gap found
		} // while haven't checked all octants yet (back to first octant)
		return this.middleOfGap(gapBegin, gapEnd);
	}; // middleOfWidestOctantGap()

	// Return the size of a gap, given the begin and end gap indices.
	this.gapSize = function(startIndex, stopIndex) {
		return (startIndex <= stopIndex ? stopIndex - startIndex + 1 :
				NUM_OCTANTS + 1 - startIndex + stopIndex);
				// otherwise gap overlaps north
	}; // gapSize()

	// Returns the index of the middle octant between two octants. 
	this.middleOfGap = function(startOctant, stopOctant) {
		var middleOctant = startOctant;
		if (startOctant < stopOctant) {
			middleOctant = round((stopOctant + startOctant) / 2);
		} else if (startOctant > stopOctant) {
			var gapSize = this.gapSize(startOctant, stopOctant);
			var offset = round(gapSize / 2);
			middleOctant = (startOctant + offset - 1) % NUM_OCTANTS;
		} 
		return middleOctant;
	}; // middleOfGap()

	// Adds the charge to the drawing buffer.
	this.drawCharge = function(drawingBuffer, charge, posn) {
		var chargeOffset = this.getChargeOffset();
		var chargePosn = arraysSum(posn, chargeOffset);
		var chargeStr = this.getChargeString(charge, !FOR_IMAGE);
		drawingBuffer.drawString(chargeStr,
				offsetTextX(chargePosn[offsets.X]), 
				offsetTextY(chargePosn[offsets.Y]));
	}; // drawCharge()

	// Gets the display string for a charge as a Stringbuilder.
	this.getChargeString = function(charge, forImage) {
		var bld = new String.builder();
		if (charge !== 0) {
			bld.append(charge > 1 ? charge : charge < -1 ? -charge : '');
			if (forImage) bld.append(charge < 0 ? '{size=18}' : '{size=14}');
			bld.append(charge > 0 ? '+' : forImage ? '\\u00af' : '&minus;').
			// bld.append(charge > 0 ? '+' : forImage ? ' -' : '&minus;').
					append('   ');
		} // if there is a charge
		return bld.toString();
	}; // getChargeString()

	// Returns offsets to use when drawing charge. Determined empirically.
	this.getChargeOffset = function() {
		switch (chargeOctant) {
 			case SW: return [-10,  -11];
			case S:	 return [ 0, -14];
			case N:	 return [ 0,  12];
			case NW: return [-8,  8];
			default: return [ 11,  -11]; // SE,W,E,NE
		} // switch
	}; // getChargeOffset()

	// Draws the lone electrons on the graph.
	this.drawElectrons = function(drawingBuffer, atomPosn, elemWidthOffset) {
		// determine the x,y coordinates for lone electrons in loneElectrons
		this.placeElectrons(atomPosn, elemWidthOffset); // set up loneElectrons object
		// draw each electron that was placed
		var numElectrons = loneElectrons.getCount();
		for (var elecIndex = 1; elecIndex <= numElectrons; elecIndex++) {
			var coords = loneElectrons.getCoords(elecIndex);
			var elecStr = '&bull;';
			drawingBuffer.drawString(elecStr,
					offsetTextX(coords[offsets.X] - elemWidthOffset), 
					offsetTextY(coords[offsets.Y]));
		} // for each electron
	}; // drawElectrons()

	// Adds nodes for the images of the charge and unshared electrons to the
	// MDocument node.
	this.addImageNodes = function(xmlDoc, mDocNode, atomPosn, charge, color, 
			elemWidthOffset) {
		// determine the x,y coordinates for electrons
		this.placeElectrons(atomPosn, elemWidthOffset); // set up loneElectrons object
		if (charge !== 0) {
			this.appendChargeImageNode(xmlDoc, mDocNode, atomPosn, charge, 
					color);
		} // if charged
		this.appendElectronsImageNodes(xmlDoc, mDocNode, atomPosn, color);
	}; // addImageNodes()

	// Adds a node for the image of the charge.
	this.appendChargeImageNode = function(xmlDoc, mDocNode, atomPosn, charge, color) {
		var attrs = [],
			textBld = new String.builder(),
			ptNum,
			xPosn,
			yPosn;
		attrs.push(['fontScale', '10.0']);
		attrs.push(['halign', 'LEFT']);
		attrs.push(['valign', 'TOP']);
		var textboxNode = addNewNode(xmlDoc, mDocNode, 'MTextBox', attrs);
		attrs = [];
		attrs.push(['name', 'text']);
		var fieldNode = addNewNode(xmlDoc, textboxNode, 'Field', attrs);
		if (color !== BLACK) {
			textBld.append('{fg=').append(color).append('}');
		} // if there's a color
		textBld.append(this.getChargeString(charge, FOR_IMAGE));
		fieldNode.appendChild(getCDataNode(xmlDoc, textBld.toString()));
		var scale = MARVIN_WIDTH / CANVAS_DIMS[offsets.X];
		var chargeLen = getTextSize(this.getChargeString(charge, !FOR_IMAGE));
		var CHARGE_OFFSETS = [0.5, 0.3]; // determined empirically
		var MINUS_OFFSET_Y = -0.35; // determined empirically
		for (ptNum = 0; ptNum < 4; ptNum++) {
			xPosn = atomPosn[offsets.X] * scale + CHARGE_OFFSETS[offsets.X];
			yPosn = atomPosn[offsets.Y] * -scale + CHARGE_OFFSETS[offsets.Y];
			if (ptNum % 3 !== 0) {
				xPosn += chargeLen * 2 * TEXT_BOX_WIDTH * scale;
			} // if at point 1 or 2
			if (ptNum < 2) yPosn += TEXT_BOX_WIDTH;
			if (charge === -1) yPosn += MINUS_OFFSET_Y;
			attrs = [];
			attrs.push(['x', xPosn]);
			attrs.push(['y', yPosn]);
			addNewNode(xmlDoc, textboxNode, 'MPoint', attrs);
		} // for each point of the textbox
	}; // appendChargeImageNode()

	// Adds a node for the image of each electron.
	this.appendElectronsImageNodes = function(xmlDoc, mDocNode, atomPosn, color) {
		var numElectrons = loneElectrons.getCount(),
			elecIndex,
			elecXMLBld,
			coords,
			textboxNode,
			fieldNode,
			attrs,
			isBlack,
			ptNum,
			scale = MARVIN_WIDTH / CANVAS_DIMS[offsets.X],
			xPosn,
			yPosn;
		var ELEC_OFFSET = [0.85, 0], // determined empirically
			ELECTRON_IMG = '\\u2022  ';
		for (elecIndex = 1; elecIndex <= numElectrons; elecIndex++) {
			coords = loneElectrons.getCoords(elecIndex);
			textboxNode = addNewNode(xmlDoc, mDocNode, 'MTextBox');
			attrs = [];
			attrs.push(['name', 'text']);
			fieldNode = addNewNode(xmlDoc, textboxNode, 'Field', attrs);
			isBlack = color === BLACK;
			if (!isBlack) {
				elecXMLBld = new String.builder();
				elecXMLBld.append('{fg=').append(color).append('}').
						append(ELECTRON_IMG);
				fieldNode.appendChild(getCDataNode(xmlDoc, 
						elecXMLBld.toString()));
			} else {
				fieldNode.appendChild(getTextNode(xmlDoc, ELECTRON_IMG));
			} // if color isn't black 
			for (ptNum = 0; ptNum < 4; ptNum++) {
				xPosn = coords[offsets.X] * scale + ELEC_OFFSET[offsets.X] +
						TEXT_BOX_WIDTH * (ptNum % 3 === 0 ? -1 : 1);
				yPosn = coords[offsets.Y] * -scale + ELEC_OFFSET[offsets.Y] +
						TEXT_BOX_WIDTH * (ptNum <= 1 ? 1 : -1);
				attrs = [];
				attrs.push(['x', xPosn]);
				attrs.push(['y', yPosn]);
				addNewNode(xmlDoc, textboxNode, 'MPoint', attrs);
			} // for each point of the textbox
		} // for each electron
	}; // appendElectronsImageNodes()

	// Given the x,y coordinates of the atom, place the lone
	// electrons around the atom ("place" means "determine their x,y coord.")
	this.placeElectrons = function(atomPosn, elemWidthOffset) {
		loneElectrons.initPlacement();
		this.placeVerticalOctants(atomPosn);
		this.placeHorizontalOctants(atomPosn, elemWidthOffset);
		this.placeCrossOctant(atomPosn, NE);
		this.placeCrossOctant(atomPosn, SE);
		this.placeCrossOctant(atomPosn, SW);
		this.placeCrossOctant(atomPosn, NW);
	}; // placeElectrons()

	// Given an atom's position, place electrons in north and south
	// octants (determine their x,y coordinates). Since drawing electrons in
	// these octants is almost identical, these two are combined.
	// Electrons are actually placed in alternating
	// octant order (one north, one south, one north, one south) as
	// "mirrored pairs", the only difference being their Y coordinate.
	this.placeVerticalOctants = function(atomPosn) {
		// determine #iterations of loop by which octant has the most electrons
		var numElectrons = Math.max(electronCount[N], electronCount[S]);
		// for each electron in each octant
		for (var elecNum = 0; elecNum < numElectrons; elecNum++) {
			// determine in which shell electrons are to be placed
			var shellNumber = floor(elecNum / ELECTRONS_PER_SHELL); 
			var shellRadius = BASE_SHELL_RADIUS +
					SHELL_SEPARATION * shellNumber;
			// add separation between two electrons of a pair
			// determine Y values of "mirrored" pairs for each octant
			// if the "mirrored pair" electron exists, place it
			var elec = [0, 0];
			elec[offsets.X] = atomPosn[offsets.X] +
					ELECTRON_SEPARATION * (elecNum % 2 === 0 ? 1 : -1);
			if (elecNum < electronCount[N]) {
				elec[offsets.Y] = atomPosn[offsets.Y] + shellRadius;
				loneElectrons.placeElectron(elec, N);
			} // if should place electron in north
			if (elecNum < electronCount[S]) {
				elec[offsets.Y] = atomPosn[offsets.Y] - shellRadius;
				loneElectrons.placeElectron(elec, S);
			} // if should place electron in south
		} // for each electron, whichever (north or south) has the most
	}; // placeVerticalOctants()

	// Given an atom's position, place electrons in east and west
	// octants. Since placing electrons in these octants is almost identical,
	// these two are combined. Electrons are actually placed in alternating
	// octant order (one east, one west, one east, one west) as
	// "mirrored pairs", the only difference being their X coordinate.
	this.placeHorizontalOctants = function(atomPosn, elemWidthOffset) {
		// determine #iterations of loop by which octant has the most electrons
		var numElectrons = Math.max(electronCount[E], electronCount[W]);
		// for each electron in each octant
		for (var elecNum = 0; elecNum < numElectrons; elecNum++) {
			var shellNumber = floor(elecNum / ELECTRONS_PER_SHELL); 
			var shellRadius = BASE_SHELL_RADIUS + SHELL_SEPARATION * shellNumber;
			// determine X values of "mirrored" pairs for each octant
			var elec = [0, 0];
			elec[offsets.Y] = atomPosn[offsets.Y] +
					ELECTRON_SEPARATION * (elecNum % 2 === 0 ? 1 : -1);
			if (elecNum < electronCount[E]) {
				elec[offsets.X] = 
						atomPosn[offsets.X] + shellRadius + elemWidthOffset * 2;
				loneElectrons.placeElectron(elec, E);
			} // if should place electron in east
			if (elecNum < electronCount[W]) {
				elec[offsets.X] = atomPosn[offsets.X] - shellRadius;
				loneElectrons.placeElectron(elec, W);
			} // if should place electron in west
		} // for each electron, which ever (east/west) has the most
	}; // placeHorizontalOctants()

	// Place the electrons in a non-vertical, non-horizontal octant 
	// (NE, SE, NW, SW)
	// given the an atom's position (x,y), the number of electrons to be placed in the
	// octant, the octant#, and a scale factor.
	// The x/y factors should be 1 or -1 and are multiplied against
	// distance offsets (so a distance is changed in a positive or negative
	// direction). Here is a handy table for values of the factors:
	//
	// xFact	yFact	Octant
	//  1		 1		NE
	//  1		-1		SE
	// -1		-1		SW
	// -1		 1		NW
	this.placeCrossOctant = function(atomPosn, octantNum) {
		var SQRT2 = Math.sqrt(2);
		// determine factors based on which of the four cross octants
		var factors = [octantNum === NW || octantNum === SW ? -1 : 1,
				octantNum === SE || octantNum === SW ? -1 : 1];
		var numElectrons = electronCount[octantNum];
		for (var elecNum = 0; elecNum < numElectrons; elecNum++) {
			// find a point [shellX, shellY] along the median of the octant
			// at distance shellRadius from center
			var shellNumber = floor(elecNum / ELECTRONS_PER_SHELL); 
			var shellRadius = BASE_SHELL_RADIUS +
					SHELL_SEPARATION * shellNumber;
			var offset = shellRadius / SQRT2;		// a right triangle, 45deg
			var shell = [atomPosn[offsets.X] + offset * factors[offsets.X],
					atomPosn[offsets.Y] + offset * factors[offsets.Y]];
			// point on "shell" at octant median
			// now find point on perpendicular of median at distance
			// ELECTRON_SEPARATION from (shellX,shellY), direction
			// determined by factors. ELECTRON_SEPARATION is the
			// distance from the octant median to the electron.
			offset = ELECTRON_SEPARATION / SQRT2;
			var sign = (elecNum % 2 === 0 ? 1 : -1);
			var elec = [shell[offsets.X] + factors[offsets.X] * offset * sign,
					shell[offsets.Y] - factors[offsets.Y] * offset * sign];
			// place the electron
			loneElectrons.placeElectron(elec, octantNum);
		} // for each electron
	}; // placeCrossOctant()

	// Gets the English name of an octant.
	this.getOctantName = function(octNum) {
		switch (octNum) {
			case N:	return 'N ';
			case S:	return 'S ';
			case E:	return 'E ';
			case W:	return 'W ';
			case NE: return 'NE';
			case NW: return 'NW';
			case SE: return 'SE';
			case SW: return 'SW';
			default: return 'undefined';
		} // switch
	}; // getOctantName()

} // Octants


/* The number and coordinates of the unshared electrons of an atom.
 * They are placed in numerical order. */

function Electrons() {

	var coords = []; // coordinates of the unshared electrons, keyed by electron number.
	var octants = []; // octants of the unshared electrons, keyed by electron number.
	var paired = 0; // number of paired electrons of the atom.
	var unpaired = 0; // number of unpaired electrons of the atom.

	// This method should be invoked before placement of electrons begins.
	this.initPlacement = function() { 
		this.coords = []; 
		this.octants = [];
	}; // initPlacement()
	
	this.getUnshared = function() { return paired + unpaired; };
	this.getPaired = function() { return paired; };
	this.getUnpaired = function() { return unpaired; };
	this.getCount = function() { return paired + unpaired; };
	// Gets the number of unshared electrons given coordinates and placed in octants. 
	this.getNumPlaced = function() { return coords.length; };
	
	// Gets the coordinates of an electron. 
	this.getCoords = function(elecNum) {
		var posn = this.coords[elecNum - 1];
		if (isEmpty(posn)) posn = [0, 0];
		return posn;
	}; // getCoords()

	// Gets the octant of an electron. 
	this.getOct = function(elecNum) {
		var oct = this.octants[elecNum - 1];
		return (!oct ? 0 : oct);
	}; // getOct()
	
	// Modifies the coordinates of an already-placed electron. 
	this.setCoords = function(elecNum, newCoords) {
		if (elecNum >= 1 && elecNum <= paired + unpaired - 1) {
			coords[elecNum - 1] = [newCoords[offsets.X], newCoords[offsets.Y]];
		} // if elecNum is in range
	}; // setCoords()

	// Sets the number of paired electrons. 
	this.setPaired = function(newCount) {
		paired = (newCount < 0 ? 0 : newCount);
	}; // setPaired()

	// Sets the number of unpaired electrons. 
	this.setUnpaired = function(newCount) {
		unpaired = (newCount < 0 ? 0 : newCount);
	}; // setUnpaired()

	// Sets the coordinates and octant of the next electron to be placed. 
	this.placeElectron = function(posn, theOct) {
		// var anOctants = new Octants();
		this.coords.push(arrayRound(arraysSum(posn, ELECTRON_OFFSETS)));
		this.octants.push(theOct);
		var numElecs = coords.length;
	}; // placeElectron()

} // Electrons

// Prefixes an atom ID in MRV with the character that indicates it is an atom ID.
function atomId(id) {
	var ATOM_LABEL = 'a';
	return ATOM_LABEL + id;
} // atomId()

// Begin an action where the user pushed down on the mouse.
function mouseDownIE() {
	mouseDown([event.x, event.y]);
} // mouseDownIE()

// Begin an action where the user pushed down on the mouse.
function mouseDownMOZ(e) {
	mouseDown([e.pageX, e.pageY]);
} // mouseDownMOZ()

// Move an atom as the user drags the mouse.
function moveAtomIE() {
	moveAtom([event.x, event.y]);
} // moveAtomIE()

// Move an atom as the user drags the mouse.
function moveAtomMOZ(e) {
	moveAtom([e.pageX, e.pageY]);
} // moveAtomMOZ()

// Move an atom as the user drags the mouse.
function moveCursorIE() {
	moveCursor([event.clientX, event.clientY]);
} // moveCursorIE()

// Move an atom as the user drags the mouse.
function moveCursorMOZ(e) {
	moveCursor([e.pageX, e.pageY]);
} // moveCursorMOZ()

// Finish an action where the user let up on the mouse.
function mouseUpIE() {
	mouseUp([event.x, event.y]);
} // mouseUpAtomIE()

// Finish an action where the user let up on the mouse.
function mouseUpMOZ(e) {
	mouseUp([e.pageX, e.pageY]);
} // mouseUpAtomMOZ()

// Act on the atom or bond nearest to where the user clicked.
function changeNearestIE() {
	changeNearest([event.x, event.y]);
} // changeNearestIE()

// Act on the atom or bond nearest to where the user clicked.
function changeNearestMOZ(e) {
	changeNearest([e.pageX, e.pageY]);
} // changeNearestMOZ()

function mouseDownTouch(e) {
	e.preventDefault();
	mouseDown([e.changedTouches[0].pageX, e.changedTouches[0].pageY]);
} // mouseDownTouch()

function moveAtomTouch(e) {
	e.preventDefault();
	moveAtom([e.changedTouches[0].pageX, e.changedTouches[0].pageY]);
} // moveAtomTouch()

function mouseUpTouch(e) {
	e.preventDefault();
	mouseUp([e.changedTouches[0].pageX, e.changedTouches[0].pageY]);
} // mouseUpTouch()

function moveCursorTouch(e) {
	e.preventDefault();
	moveCursor([e.changedTouches[0].pageX, e.changedTouches[0].pageY]);
} // moveCursorTouch()

