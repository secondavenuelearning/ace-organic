// <!-- avoid parsing the following as HTML
/*
Code for starting an instance of Marvin JS on a jsp page.

Page must contain the following:

<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/3DTemplatesMJS.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<script src="https://marvinjs.chemicalize.com/v1/<%= 
		AppConfig.marvinJSLicense %>/client-settings.js"></script>
<script src="https://marvinjs.chemicalize.com/v1/client.js"></script>
<script src="<%= pathToRoot %>js/marvinjslauncher.js"></script>
<script src="<%= pathToRoot %>js/promise.min.js"></script>
<script src="<%= pathToRoot %>js/lib/jquery.min.js"
		type="text/javascript"></script> // for images only

<td [or div] id="<%= APPLET_NAME %>"
		style="text-align:center; width:<%= dims[0] %>px; height:<%= dims[1] %>px;">
	<script type="text/javascript">
		startMarvinJS('[molecule]',
				<%= qTypeRaw %>, <%= qFlags %>, '<%= APPLET_NAME %>' [optional], method);
	</script>
</td>

where method is a method to run when the sketcher's contents change.

Calling page should also have a method loadSelections(); it can be empty.

To retrieve the MRV of the drawing, modify callServer() as follows.

Place the following lines at the beginning of callServer():

	function callServer(evaluate) {
		marvinSketcherInstances[appletName].exportStructure('mrv').then(function (source) {

And place the following lines at the end:

		}, function (error) {
			alert('Molecule export failed:' + error);
		});
	} // callServer

The variable source contains the MRV.

*/
/*jsl:option explicit*/
/*jsl:import jslib.js*/

// we use an array because some pages have more than one sketcher
var marvinSketcherInstances = [];
var lonePairCalculnEnabled = true;

/* Gets whether to show C atoms.  */
function allCVisible(flags) {
	"use strict";
	return (flags & SHOWALLC) !== 0;
} // allCVisible()

/* Gets whether to show atom mapping.  */
function atomMapsVisible(flags) {
	"use strict";
	return (flags & SHOWMAPPING) !== 0;
} // atomMapsVisible()

/* Gets whether to show R/S configurations. DOESN'T WORK IN MARVINJS. */
function configsRSVisible(flags) {
	"use strict";
	return ((flags & SHOWRSLABELS) !== 0 ? 'ALL' : 'OFF');
} // configsRSVisible()

/* Gets whether to show R/S configurations in a MarvinJS-generated image. */
function configsRSVisibleForJS(flags) {
	"use strict";
	return (flags & SHOWRSLABELS) !== 0;
} // configsRSVisible()

/* Gets whether to show H atoms.  */
function getHVisibilityJS(flags) {
	"use strict";
	return ((flags & IMPLICITHMASK) === SHOWNOH ? 'OFF' :
			(flags & IMPLICITHMASK) === SHOWHETEROH ? 'HETERO' :
					(flags & IMPLICITHMASK) === SHOWALLH ? 'ALL' :
							'TERMINAL_AND_HETERO');
} // getHVisibilityJS()

/* Gets whether to display 3D templates.  */
function is3D(flags) {
	"use strict";
	return (flags & THREEDIM) !== 0;
} // is3D()

/* Gets whether to show lone pairs.  */
function lonePairsVisible(flags) {
	"use strict";
	return (flags & SHOWLONEPAIRS) !== 0;
} // lonePairsVisible()

/* Gets whether to show valence errors.  */
function valenceErrorVisible(flags) {
	"use strict";
	return (flags & BADVALENCEINVISIBLE) === 0;
} // valenceErrorVisible()
 
// Uses AJAX to call a JChem method that puts a molecule property into the MRV.
// Not currently used because of asynchronicity issues; calling method
// doesn't wait for result before continuing to execute.
function modifyMrvPropertyAJAX(mrv, propertyName, propertyValue, pathToRoot) {
	"use strict";
	var toSend = new String.builder().
			append('mrv=').
			append(encodeURIComponent(mrv)).
			append('&propertyName=').
			append(propertyName);
	if (!isEmpty(propertyValue)) {
		toSend.append('&propertyValue=').
				append(encodeURIComponent(propertyValue));
	} // if there is a value
	callAJAX(pathToRoot + 'includes/modifyProperty.jsp', 
			toSend.toString(), finishModifyProperty);
} // modifyMrvPropertyAJAX()

// Gets the MRV with the molecule property inserted.
function finishModifyPropertyAJAX() {
	"use strict";
	if (xmlHttp.readyState === 4) {
		var responsePage = xmlHttp.responseText;
		var mrvValue = extractField(responsePage, 'mrvValue');
		console.log('marvinStart.js:finishModifyProperty: ' +
				'extracted mrvValue = ' + mrvValue);
		return mrvValue;
	}
} // finishModifyPropertyAJAX()

/* Puts a molecule property into the MRV. Assumes tree in order
 * <molecule><propertyList><property><scalar>value 
 * or 
 * <molecule><propertyList><property><scalar><![CDATA[value]]>
 * There are several cases:
 * * The property value is not empty.
 * 		* There are no properties
 * 			-- Add a property list and the property.
 * 		* There is at least one property, but not the target one.
 * 			-- Add the property.
 * 		* The target property is already present.
 * 			-- Modify the value of the existing property.
 * * The property value is empty.
 * 		* There is no existing property with the same name.
 * 			-- Return the MRV unchanged.
 * 		* There is an existing property with the same name.
 * 			-- Remove it from the property list. If it is the only property in the
 * 			list, remove the property list as well.
 * Uses CDATA field iff value contains any <>&;/ characters. Not clear why the ;
 * character triggers CDATA, but that's what JChem does.
 */
function modifyMrvProperty(mrv, propertyName, propertyValue) {
	"use strict";
	var nodeNum, 
		numNodes,
		attrs = [],
		moleculeNodeChild,
		aPropertyNode = null,
		targetPropertyNode = null,
		scalarNode = null,
		scalarChildNode = null;
	var xmlDoc = (new DOMParser()).parseFromString(mrv, 'text/xml');
	var moleculeNode = getFirstNode(xmlDoc, 'molecule');
	var propertyListNode = getFirstNode(xmlDoc, 'propertyList');
	// if not already one, make propertyList node, add to molecule node
	if (propertyListNode == null) {
		if (isEmpty(propertyValue)) return makeReadable(mrv); // no change
		else { // did not find existing propertyList; making one
			if (moleculeNode.childNodes.length === 0) {
				propertyListNode = addNewNode(xmlDoc, propertyListNode, 
						'propertyList');
			} else {
				moleculeNodeChild = moleculeNode.childNodes[0];
				propertyListNode = insertNewNode(xmlDoc, moleculeNode, 
						moleculeNodeChild, 'propertyList');
			} // if there are children nodes of the molecule
		} // if propertyValue is empty
	} // if need to add a property list node
	// find the target property node
	numNodes = propertyListNode.childNodes.length;
	for (nodeNum = 0; nodeNum < numNodes; nodeNum++) {
		aPropertyNode = propertyListNode.childNodes[nodeNum];
		if (aPropertyNode.title === propertyName) {
			targetPropertyNode = aPropertyNode;
			if (isEmpty(propertyValue)) {
				propertyListNode.removeChild(targetPropertyNode);
				// see if there are any other properties
				numNodes = propertyListNode.childNodes.length;
				for (nodeNum = 0; nodeNum < numNodes; nodeNum++) {
					aPropertyNode = propertyListNode.childNodes[nodeNum];
					if (aPropertyNode.nodeName === 'property') {
						return getXML(xmlDoc, MAKE_READABLE);
					} // if nodeName
				} // for each propertyList node child
				// no other properties; remove propertyListNode, return MRV
				moleculeNode.removeChild(propertyListNode);
				return getXML(xmlDoc, MAKE_READABLE);
			} // if propertyValue is empty
			break;
		} // if title
	} // for each propertyList node child
	// if not already one, make target property node, add 
	// scalar node as child, add to propertyListNode
	if (targetPropertyNode == null) {
		// did not find existing reactionIds property; making one
		attrs = [];
		attrs.push(['dictRef', propertyName]);
		attrs.push(['title', propertyName]);
		targetPropertyNode = addNewNode(xmlDoc, propertyListNode, 
				'property', attrs);
		addNewNode(xmlDoc, targetPropertyNode, 'scalar');
	} // if need to add the target property node
	// get scalar node
	numNodes = targetPropertyNode.childNodes.length;
	for (nodeNum = 0; nodeNum < numNodes; nodeNum++) {
		scalarNode = targetPropertyNode.childNodes[nodeNum];
		if (scalarNode.nodeName === 'scalar') break;
	} // for each targetPropertyNode child
	// remove existing child of scalar node that has old value
	numNodes = scalarNode.childNodes.length;
	for (nodeNum = 0; nodeNum < numNodes; nodeNum++) {
		scalarChildNode = scalarNode.childNodes[nodeNum];
		if (scalarChildNode.nodeName === '#text'
				|| scalarChildNode.nodeName === '#cdata-section') {
			scalarNode.removeChild(scalarChildNode);
			break;
		} // if found a child node with old value
	} // for each scalar node child
	// make node with new value, add back into scalar node
	scalarNode.appendChild(needsToBeInCDATA(propertyValue)
			? getCDataNode(xmlDoc, propertyValue)
			: getTextNode(xmlDoc, propertyValue));
	return getXML(xmlDoc, MAKE_READABLE);
} // modifyMrvProperty()

// Toggles between manual and automatic addition of lone pairs
function toggleAutomaticLonePairCalculation(appletName) {
	lonePairCalculnEnabled = !lonePairCalculnEnabled;
	marvinSketcherInstances[appletName].setDisplaySettings({
		lonePairsVisible: true,
		lonepaircalculationenabled: lonePairCalculnEnabled
	}); // setDisplaySettings
} // toggleAutomaticLonePairCalculation()

// Adds a button to MarvinJS that allows one to open a large sketcher window.
function addSketcherWindowButton(appletName, qFlags) {
	var url = new String.builder().
				append('\/ace\/includes\/marvinJSWindow.jsp?qFlags=').
				append(qFlags).
				append('&appletName=').
				append(appletName).
				toString(),
		buttonAttrs = {
			'name' : 'full-screen sketcher',
			'imageUrl' : getButtonImgAsDataURL('expand'),
			'toolbar' : 'S'
		};
	marvinSketcherInstances[appletName].addButton(buttonAttrs, function() {
		openSketcherWindow(url);
	});
} // addSketcherWindowButton()

// Adds a button to MarvinJS that copies a synthesis MRV to the clipboard,
// incorporating reaction IDs.
function addCopySynthesisButton(appletName, pathToRoot) {
	var rxnIds,
		modifiedMrv,
		targetPage = '\/ace\/includes\/addRxnCondsToMRV.jsp',
				// target page name must be same as in openwindows.js
		newForm,
		buttonAttrs = {
			'name' : 'copy synthesis with reaction conditions',
			'imageUrl' : getButtonImgAsDataURL('copySyn'),
			'toolbar' : 'S'
		};
	marvinSketcherInstances[appletName].addButton(buttonAttrs, function() {
		marvinSketcherInstances[appletName].exportStructure('mrv').
				then(function(mol) {
			rxnIds = getRxnIds();
			modifiedMrv = modifyMrvWithRxnConditions(mol, rxnIds, pathToRoot);
			if (!copyToClipboard(modifiedMrv)) {
				// display MRV for manual copy
				newForm = prepareForm(targetPage, 'SourceCode');
				newForm.appendChild(prepareField('mrvStr', mol));
				newForm.appendChild(prepareField('rxnIdsStr', 
						isEmpty(rxnIds) ? '' : rxnIds));
				document.body.appendChild(newForm); // Firefox
				openSourceCodeWindow(targetPage);
				newForm.submit();
			} // if can't copy directly to clipboard
		}, function(error) {
			alert('Molecule export to MRV failed: ' + error);	
		});
	});
} // addCopySynthesisButton()

// Adds an elemental analysis button to MarvinJS.
function addElementalAnalysisButton(appletName) {
	var targetPage = '\/ace\/includes\/showElemAnal.jsp',
			// target page name must be exactly the same as in openwindows.js
		newForm,
		buttonAttrs = {
			'name' : 'elemental analysis',
			'imageUrl' : getButtonImgAsDataURL('analysis'),
			'toolbar' : 'S'
		};
	marvinSketcherInstances[appletName].addButton(buttonAttrs, function() {
		marvinSketcherInstances[appletName].exportStructure('mrv').
				then(function(mol) {
			newForm = prepareForm(targetPage, 'Elemental Analysis');
			newForm.appendChild(prepareField('mrvStr', mol));
			document.body.appendChild(newForm); // necessary for Firefox
			openAnalysisWindow(targetPage);
			newForm.submit();
		}, function(error) {
			alert('Molecule export to MRV failed: ' + error);	
		});
	});
} // addElementalAnalysisButton()

// Adds a button to MarvinJS that allows one to measure length/angle/dihedral.
function addMeasureButton(appletName) {
	var targetPage = '\/ace\/includes\/measure.jsp',
		newForm,
		selectedAtomsStr,
		buttonAttrs = {
			'name' : 'measure length\/angle\/dihedral',
			'imageUrl' : getButtonImgAsDataURL('ruler'),
			'toolbar' : 'S'
		};
	marvinSketcherInstances[appletName].addButton(buttonAttrs, function() {
		marvinSketcherInstances[appletName].exportStructure('mrv').
				then(function(mol) {
			marvinSketcherInstances[appletName].getSelection().
					then(function(selection) { 
				selectedAtomsStr = selection.atoms;
				// target page name must be exactly the same as in openwindows.js
				newForm = prepareForm(targetPage, 'Measure');
				newForm.appendChild(prepareField('mrvStr', mol));
				newForm.appendChild(prepareField('selectedAtomsStr', 
						selectedAtomsStr));
				document.body.appendChild(newForm); // necessary for Firefox
				openMeasureWindow(targetPage);
				newForm.submit();
			}, function(error) {
				alert('Failed to acquire selected atoms: ' + error);	
			});
		}, function(error) {
			alert('Molecule export to MRV failed: ' + error);	
		});
	});
} // addMeasureButton()

// Adds a button to MarvinJS that allows one to toggle between manual and
// calculated lone pair addition
function addManualLonePairButton(appletName) {
		buttonAttrs = {
			'name' : 'toggle manual lone pairs',
			'imageUrl' : getButtonImgAsDataURL('manualLPs'),
			'toolbar' : 'S'
		};
	marvinSketcherInstances[appletName].addButton(buttonAttrs, function() {
		toggleAutomaticLonePairCalculation(appletName);
	});
} // addManualLonePairButton()

// Loads the molecule and atom/bond selections. Called from below and also
// from resetFigure() in homework/answerJS.jsp.h and loadPastedSynthMRV()
// in forum/addPost.jsp.
function loadJSMol(mol, appletName) {
	"use strict";
	if (!isEmpty(mol)) {
		marvinSketcherInstances[appletName].importStructure(null, mol).
				then(function (sketcher) {
			if (loadSelections) {
				loadSelections();
			}
		}, function (error) {
			alert('MarvinJS did not import molecule: ' + error);
		}); // importStructure.then()
	} // if there's a mol to import
} // loadJSMol()

// Creates and initiates the Marvin JS drawing object.
function startMarvinJS(mol, qTypeRaw, qFlags, appletName, pathToRoot, 
		onChangeMethod) {
	"use strict";
	var qTypeNeg = qTypeRaw < 0,
		qType = Math.abs(qTypeRaw),
		isMechanism = qType === MECHANISM,
		isSynthesis = qType === SYNTHESIS,
		is3DQ = is3D(qFlags),
		sketcherWidth = '500px',
		sketcherHeight = '400px',
		attrs,
		styleProps,
		appletCell,
		editorJson,
		buttons3DTemplates,
		num3DButtons,
		buttonNum,
		button;
	if (((isMechanism || isSynthesis) && !qTypeNeg) ||
			(!isMechanism && !isSynthesis && qTypeNeg)) {
		sketcherWidth = '600px';
		sketcherHeight = '500px';
	} // how big to make canvas
	buttons3DTemplates = (is3DQ ? get3DButtons() : []);
	num3DButtons = buttons3DTemplates.length;
	if (num3DButtons > 4) {
		sketcherWidth = (500 + (num3DButtons - 4) * 25) + 'px';
	} // how big to make canvas
	if (appletName === 'sketcher') {
		sketcherWidth = '95%';
		// sketcherHeight = 0.9 * window.innerHeight + 'px';
		sketcherHeight = 0.9 * document.documentElement.clientHeight + 'px';
	} // if sketcher window
	styleProps = [
			['overflow', 'hidden'],
			['border', '1px solid darkgray'],
			['width', sketcherWidth],
			['height', sketcherHeight],
			['margin-left', 'auto'],
			['margin-right', 'auto']];
	attrs = [
			['id', appletName],
			['name', appletName],
			['class', 'sketcher-frame'],
			['style', getStyleString(styleProps)]];
	appletCell = document.getElementById(appletName);
	setAttributes(appletCell, attrs);
	editorJson = {hidePoweredBy: true,
			'data-reaction': 'OFF'};
	ChemicalizeMarvinJs.createEditor('#' + appletName, editorJson).
			then(function (sketcher) {
		marvinSketcherInstances[appletName] = sketcher;
		if (appletName !== 'sketcher') {
			addSketcherWindowButton(appletName, qFlags);
		} // if not the sketcher window
		if (isSynthesis) {
			addCopySynthesisButton(appletName, pathToRoot);
		} // if a synthesis question
		addElementalAnalysisButton(appletName);
		addMeasureButton(appletName);
		if (!is3DQ) {
			addManualLonePairButton(appletName);
		} // if not 3D
		marvinSketcherInstances[appletName].setDisplaySettings({
			toolbars: 'reporting',
			copyasmrv: true,
			carbonLabelVisible: allCVisible(qFlags),
			implicitHydrogen: getHVisibilityJS(qFlags),
			valenceErrorVisible: valenceErrorVisible(qFlags),
			atomIndicesVisible: appletName === 'hybridMarvin',
			lonePairsVisible: lonePairsVisible(qFlags),
			atomMapsVisible: atomMapsVisible(qFlags),
			chiralFlagVisible: false,
			circledsign: true,
			cpkColoring: true
		});
		if (is3DQ) {
			for (buttonNum = 0; buttonNum < buttons3DTemplates.length; buttonNum++) {
				button = buttons3DTemplates[buttonNum];
				marvinSketcherInstances[appletName].addTemplate(button);
			} // for each button
		} // if is3DQ
		loadJSMol(mol, appletName);
		if (onChangeMethod) {
			marvinSketcherInstances[appletName].on('molchange', onChangeMethod);
		} // if should add a listener to run the method
	}, function (error) {
		alert('Marvin JS did not load: ' + error);
	}); // createEditor.then()
} // startMarvinJS()

/** Uses MarvinJS to generate an SVG or PNG image. 
	Calling method:
	<div id="figJS<%= figId %>" class="left10" style="display:none; text-align:center;">
	<script type="text/javascript">
		// <!-- >
		displayImage(pathToRoot,
				molStr,
				{ flags: number,
				imageId: 'string',
				width: number,
				height: number,
				prefersPNG: boolean });  // optional; false if not specified
		// -->
	</script>
	</div>
	The calling method is usually generated by MolString.getImage().

	Unfortunately, the following code is obsolete since we started using 
	Chemicalize to provide MarvinJS.
 */
function displayImage(pathToRoot, mol, params) {
	setInnerHTML('fig' + params.imageId, 
			'Sorry, an exception occurred when trying to create this image.');
	/* $(document).ready(function handleDocumentReady (e) {
		var marvinPack = pathToRoot + 'js/marvinpack.html';
		var iframeId = 'marvinjs-iframe';
		$('body').append($('<iframe>', 
				{ id: iframeId, 
				src: marvinPack}));
		MarvinJSUtil.getPackage('#' + iframeId).then(function(marvinNameSpace) {
			marvinNameSpace.onReady(function() {
				marvin = marvinNameSpace;
				var exporter = createExporter(params);
				exporter.render(mol).then(function(dataUri) {
					var imgContainerName = '#figJS'	// do not change!
							+ params.imageId; 
					if (params.prefersPNG) {
						$(imgContainerName).empty();
						var img = $('<img>', { src: dataUri}).
								appendTo($(imgContainerName));
					} else {
						$(imgContainerName).html(dataUri);
					} // if prefers PNG
					$(imgContainerName).css('display', 'inline-block');
				});
			});
		}, function(error) {
			alert('Cannot retrieve marvin instance from iframe: ' + error);
		});
	}); /**/
} // displayImage()

// --> end HTML comment
