// <!-- avoid parsing the following as HTML
// Functions for opening new windows

/*jsl:option explicit*/
/*jsl:import jslib.js*/

// Evaluator IO
function openEvaluatorWindow(url) {
	"use strict";
	var w = window.open(url, 'Evaluator',
		'width=600,height=800,left=200,top=30,resizable=yes,scrollbars=yes,status=yes');
	w.focus();
}

// Figure editing IO
function openFigureEditingWindow(url) {
	"use strict";
	var w = window.open(url, 'Figure',
		'width=900,height=800,left=200,top=30,resizable=yes,scrollbars=yes,status=yes');
	w.focus();
}

// Reaction Condition IO
function openReactionWindow(url) {
	"use strict";
	var w = window.open(url, 'Reaction_Condition', // IE bug: no space in window name
		'width=600,height=680,left=200,top=30,resizable=yes,scrollbars=yes,status=yes');
	w.focus();
}

// R-Group IO
function openRGroupWindow(url) {
	"use strict";
	var w = window.open(url, 'R_Group', // IE bug: no space in window name
		'width=600,height=680,left=200,top=30,resizable=yes,scrollbars=yes,status=yes');
	w.focus();
}

// Figure IO
function openFigureWindow(url) {
	"use strict";
	var w = window.open(url, 'Figures',
		'width=620,height=625,left=200,top=70,resizable=yes,scrollbars=yes,status=yes');
	w.focus();
}

// Topic IO
function openTopicWindow(url) {
	"use strict";
	var w = window.open(url, 'Topics',
		'width=520,height=240,left=200,top=70,resizable=yes,scrollbars=no,status=yes');
	w.focus();
}

// QSetDescr IO
function openQSetWindow(url) {
	"use strict";
	var w = window.open(url, 'Question_Set',
		'width=500,height=500,left=200,top=70,resizable=yes,scrollbars=no,status=yes');
	w.focus();
}

// QSetDescr IO
function openQSetWindow2(url) {
	"use strict";
	var w = window.open(url, 'Question_Set',
		'width=480,height=200,left=200,top=70,resizable=yes,scrollbars=no,status=yes');
	w.focus();
}

// MarvinLive 
function openMarvinLiveWindow(url) {
	"use strict";
	var w = window.open(url, 'MarvinLive',
		'width=900,height=750,left=200,top=70,resizable=yes,scrollbars=yes,status=yes');
	w.focus();
}

// MarvinJS sketcher window
function openSketcherWindow(url) {
	"use strict";
	var w = window.open(url, 'MarvinJS sketcher window',
		'resizable=yes,scrollbars=yes,status=yes');
	w.focus();
}

// Jmol window
function openJmolWindow(url) {
	"use strict";
	var w = window.open(url, 'Jmol',
		'resizable=yes,scrollbars=yes,status=yes');
	w.focus();
}

// Elemental analysis window
function openAnalysisWindow(url) {
	"use strict";
	// second parameter for window.open() must be identical to second
	// parameter for prepareForm() in js/marvinJSStart.js 
	var w = window.open(url, 'Elemental Analysis',
		'width=400,height=200,left=200,top=70,resizable=yes,scrollbars=yes,status=yes');
	w.focus();
}

// Window showing description of a drawing for BLV people 
function openBLVDescriptionWindow(url) {
	// second parameter for window.open() must be identical to second
	// parameter for prepareForm() in js/marvinJSStart.js 
	"use strict";
	var w = window.open(url, 'Description for BLV People',
		'width=400,height=200,left=200,top=70,resizable=yes,scrollbars=yes,status=yes');
	w.focus();
}

// Measure distance/angle/dihedral window
function openMeasureWindow(url) {
	// second parameter for window.open() must be identical to second
	// parameter for prepareForm() in js/marvinJSStart.js 
	"use strict";
	var w = window.open(url, 'Measure',
		'width=400,height=200,left=200,top=70,resizable=yes,scrollbars=yes,status=yes');
	w.focus();
}

// Preview
function openPreviewWindow(url) {
	"use strict";
	var w = window.open(url, 'Preview',
		'width=800,height=600,left=200,top=70,resizable=yes,scrollbars=yes,status=yes');
	w.focus();
}

// Tab-delimited grades
function openTabGradesWindow(url) {
	"use strict";
	// url may contain username; need to encode for special characters
	var w = window.open(encodeURIComponent(url), 'Tab-delimited_grades',
		'width=720,height=450,left=200,top=70,resizable=yes,scrollbars=yes,status=yes');
	w.focus();
}

// Homework
function openHWWindow(url) {
	"use strict";
	var w = window.open(url, 'Homework',
		'width=930,height=600,left=200,top=70,resizable=yes,scrollbars=yes,status=yes');
	w.focus();
}

// General help items
function openHelpWindow(url) {
	"use strict";
	var w = window.open(url, 'Help',
		'width=450,height=220,left=200,top=70,resizable=yes,scrollbars=yes,status=yes');
	w.focus();
}

// exporting questions or assignments
function openExportWindow(url) {
	"use strict";
	var w = window.open(url, 'Export',
		'width=600,height=600,left=200,top=70,resizable=yes,scrollbars=yes,status=yes');
	w.focus();
}

// showing molecules
function openMolShowWindow(url) {
	"use strict";
	var w = window.open(url, 'ShowMol',
		'width=400,height=420,left=200,top=70,resizable=yes,scrolllbars=yes,status=yes');
	w.focus();
}

// enlarged image figure
function enlargeImage(imageurl, vectorXML, color) {
	"use strict";
	var w, varurl = "/ace/homework/imageLarge.jsp?imageurl=" + imageurl;
	if (!isEmpty(vectorXML)) {
		varurl += "&vectorXML=" + vectorXML;
	}
	if (!isEmpty(color)) {
		varurl += "&color=" + color;
	}
	w = window.open(varurl, 'Image',
		'width=600,height=400,left=300,top=100,resizable=yes,scrollbars=yes,status=yes');
	w.focus();
}

// regrading
function openResetWindow(url) {
	"use strict";
	var w = window.open(url, 'Alterer',
		'width=600,height=800,left=200,top=70,resizable=yes,scrollbars=yes,status=yes');
	w.focus();
}

// output after regrading
function openRegradeWindow(url) {
	"use strict";
	var w = window.open(url, 'Regrader',
		'width=600,height=300,left=200,top=70,resizable=yes,scrollbars=yes,status=yes');
	w.focus();
}

// printable list of questions in an assignment
function openPrintableList(url) {
	"use strict";
	var w = window.open(url, 'PrintList', // IE bug: no space in window name
		'width=930,height=600,left=200,top=70,resizable=yes,scrollbars=yes,status=yes,menubar=yes');
	w.focus();
}

// source code
function openSourceCodeWindow(url) {
	"use strict";
	var w = window.open(url, 'SourceCode',
		'width=600,height=500,left=200,top=70,resizable=yes,scrollbars=yes,status=yes');
	w.focus();
}

// big alerts
function openBigAlertWindow(url) {
	"use strict";
	var w = window.open(url, 'Alert',
		'width=600,height=500,left=200,top=70,resizable=yes,scrollbars=yes,status=yes');
	w.focus();
}

// course notes
function openNotesWindow(url) {
	"use strict";
	var w = window.open(url, 'Notes',
		'width=600,height=500,left=200,top=70,resizable=yes,scrollbars=yes,status=yes');
	w.focus();
}

// you need to pay window
function openPaymentWindow(url) {
	"use strict";
	var w = window.open(url, 'ACE Payment Window',
		'resizable=yes,scrollbars=yes,status=no');
	w.focus();
}

// acceptable words
function openAcceptableWordsWindow(url) {
	"use strict";
	var w = window.open(url, 'AcceptableWords',
		'width=600,height=500,left=200,top=70,resizable=yes,scrollbars=yes,status=yes');
	w.focus();
}

// --> end HTML comment
