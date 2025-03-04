// <!-- avoid parsing the following as HTML
/*jsl:option explicit*/

// Gets if the string contains the item.
String.prototype.contains = function (substring) {
	"use strict";
	// Return true of substring found in string
	return this.indexOf(substring) >= 0;
}; // String.contains()

// Gets if the string ends with the item.
String.prototype.endsWith = function(substring) {
	"use strict";
	// Return true if substring found at end of string
	var posn = this.indexOf(substring);
	return posn >= 0 && posn === this.length - substring.length; 
}; // String.endsWith()

// Gets if the array contains the item.
Array.prototype.contains = function (searchItem) {
	"use strict";
	return this.indexOf(searchItem) >= 0;
}; // Array.contains()

function fixTheImageSize(img, size) {
	"use strict";
	var newImage;
	if (img.width === 0) {
		newImage = new Image();
		newImage.src = img.src;
		if (newImage.width > size) {
			img.width = size;
		}
	}
	if (img.width > size) {
		img.style.width = String(size) + 'px';
	}
	img.style.visibility = 'visible';
} // fixTheImageSize()

function fixImageSize(img) {
	"use strict";
	fixTheImageSize(img, 250);
} // fixImageSize()

function prepareImage(img, enlargeCell) {
	"use strict";
	if (img.width <= 500) {
		setInnerHTML(enlargeCell, '');
	} // if image is large
	fixImageSize(img);
} // prepareImage()

// obsolete?
function fixAllImageSizes() {
	"use strict";
	var fixThese = document.getElementsByName('fixable'),
		imgNum;
	for (imgNum = 0; imgNum < fixThese.length; imgNum += 1) {
		fixTheImageSize(fixThese[imgNum], 250);
	}
} // fixAllImageSizes()

// Code courtesy: http://www.geocities.com/technofundo/tech/js/showhide.html
function toggleLayer(szDivID, iState) { // 1 visible, 0 hidden
	"use strict";
	var obj;
	if (document.layers) {  //NN4+
		alert(szDivID);
		alert('NNN4');
		// eval('document.layers["' + szDivID + '"].visibility="show"');
		document.layers[szDivID].visibility = "show";
	} else if (document.getElementById) { //gecko(NN6) + IE 5+
		obj = document.getElementById(szDivID);
		obj.style.visibility = iState ? 'visible' : 'hidden';
	} else if (document.all) { // IE 4
		document.all[szDivID].style.visibility = iState ? 'visible' : 'hidden';
	}
} // toggleLayer()

// Function for showing and hiding layers
function showLayer(layerName) {
	"use strict";
	toggleLayer(layerName, 1);
} // showLayer()

function hideLayer(layerName) {
	"use strict";
	toggleLayer(layerName, 0);
} // hideLayer()

// check (val=false) or uncheck (val=true) all checkboxes
function setAllCheckBoxes(opt, val) {
	"use strict";
	var intLoop;
	if (opt) {
		if (opt.length) {
			for (intLoop = 0; intLoop < opt.length; intLoop += 1) {
				opt[intLoop].checked = val;
			}
		} else {
			opt.checked = val;
		}
	}
} // setAllCheckBoxes()

// Return the array of selected checkboxes
function getSelected(opt) {
	"use strict";
	var selected = [],
		index = 0,
		intLoop;
	if (opt.length) {
		for (intLoop = 0; intLoop < opt.length; intLoop += 1) {
			if (opt[intLoop].selected || opt[intLoop].checked) {
				index = selected.length;
				selected[index] = {};
				selected[index].value = opt[intLoop].value;
				selected[index].index = intLoop;
			}
		} // each opt
	} else { // opt is not an array
		if (opt.checked || opt.selected) {
			selected[index] = {};
			selected[index].value = opt.value;
			selected[index].index = 0;
		}
	}
	return selected;
} // getSelected()

// Return the array of values of selected checkboxes
function getSelectedValues(opt) {
	"use strict";
	var selected = [],
		intLoop,
		index = 0;
	if (opt.length) {
		for (intLoop = 0; intLoop < opt.length; intLoop += 1) {
			if (opt[intLoop].selected || opt[intLoop].checked) {
				index = selected.length;
				selected[index] = opt[intLoop].value;
			}
		} // each opt
	} else { // opt is not an array
		if (opt.checked || opt.selected) {
			selected[index] = opt.value;
		}
	}
	return selected;
} // getSelectedValues()

function noValue(s) {
	"use strict";
	return s === null || s === undefined;
} // noValue()

function isEmpty(s) {
	"use strict";
	return noValue(s) || s.length === 0;
} // isEmpty()

// remove all trailing and leading whitespaces
function trim(s) {
	"use strict";
	/*jslint regexp:true*/
	return (isEmpty(s) ? s : s.replace(/^\s+|\s+$/g, ''));
} // trim()

// remove all trailing and leading whitespaces
// If the string is null or empty, then returns empty string
function trimWhiteSpaces(s) {
	"use strict";
	return (isEmpty(s) ? '' : trim(s));
} // trimWhiteSpaces()

// code courtesy: http://developer.netscape.com.
var whitespace = ' \t\n\r';
function isWhiteSpace(s) {
	"use strict";
	var i, c;
	if (isEmpty(s)) {
		return true;
	}
	for (i = 0; i < s.length; i += 1) {
		c = s.charAt(i);
		if (whitespace.indexOf(c) === -1) {
			return false;
		}
	}
	return true;
} // isWhiteSpace()

// returns true if string is empty or contains space, tab, or new line
// characters
function containsWhiteSpace(s) {
	"use strict";
	if (isEmpty(s)) {
		return true;
	}
	var i, c;
	for (i = 0; i < whitespace.length; i += 1) {
		c = whitespace.charAt(i);
		if (s.indexOf(c) != -1) {
			return true;
		}
	}
	return false;
} // containsWhiteSpace()

// returns true if string should be in a <![CDATA[ ]]> field in an XML document.
function needsToBeInCDATA(s) {
	"use strict";
	if (isEmpty(s)) {
		return true;
	}
	var i, c, cdataChars = '&<>;/';
	for (i = 0; i < cdataChars.length; i += 1) {
		c = cdataChars.charAt(i);
		if (s.indexOf(c) != -1) {
			return true;
		}
	}
	return false;
} // needsToBeInCDATA()

// returns true if string contains only letters
function isAlphabetical(s) {
	"use strict";
	/*jslint regexp:true*/
	return !isEmpty(s) && !(/[^a-zA-Z]/).test(s);
} // isAlphabetical()

// returns true if string contains only letters and numbers
function isAlphaNumeric(s) {
	"use strict";
	/*jslint regexp:true*/
	return !isEmpty(s) && !(/[^a-zA-Z0-9]/).test(s);
} // isAlphaNumeric()

// returns the sign of a number
function signum(x) {
	"use strict";
	return (x > 0 ? 1 : x < 0 ? -1 : 0);
} // signum()

// checks if an entire trimmed string, not just the beginning, can be parsed to
// a float
function canParseToFloat(s) {
	"use strict";
	return canParseToNum(s, false);
} // canParseToFloat()

// checks if an entire trimmed string, not just the beginning, can be parsed to
// an int
function canParseToInt(s) {
	"use strict";
	return canParseToNum(s, true);
} // canParseToInt()

// parses a string to a float, even if it begins with en dash or minus
function parseToFloat(s) {
	"use strict";
	return (isWhiteSpace(s) ? 0 : parseFloat(processNumStr(s)));
} // parseToFloat()

// parses a string to an integer, even if it begins with en dash or minus
function parseToInt(s) {
	"use strict";
	return (isWhiteSpace(s) ? 0 : parseInt(processNumStr(s), 10));
} // parseToInt()

// returns true if string represents a positive integer or 0
function isNonnegativeInteger(s) {
	"use strict";
	return !isWhiteSpace(s) && canParseToInt(s) && parseToInt(s) >= 0;
} // isNonnegativeInteger()

// returns true if string represents a positive integer
function isPositiveInteger(s) {
	"use strict";
	return !isWhiteSpace(s) && canParseToInt(s) && parseToInt(s) >= 1;
} // isPositiveInteger()

// converts leading en dash or minus to -, removes leading +
function processNumStr(s) {
	"use strict";
	var str = trim(s),
		enDash_minus = String.fromCharCode(8211) + String.fromCharCode(8722),
		firstChar = str.charAt(0);
	if (enDash_minus.indexOf(firstChar) >= 0) {
		str = '-' + str.substring(1);
	} else if (firstChar === '+') {
		str = str.substring(1);
	} // if starts with en dash or minus or plus sign
	return str;
} // processNumStr()

// checks if an entire trimmed string, not just the beginning, can be parsed to
// a float or int
function canParseToNum(s, toInt) {
	"use strict";
	if (isWhiteSpace(s)) {
		return false;
	}
	var str = processNumStr(s),
		codePeriod = '.'.charCodeAt(0),
		code0 = '0'.charCodeAt(0),
		code9 = '9'.charCodeAt(0),
		start = (str.charAt(0) == '-' && str.length > 1 ? 1 : 0),
		foundPeriod = false,
		posn,
		code;
	for (posn = start; posn < str.length; posn += 1) {
		code = str.charCodeAt(posn);
		if (code == codePeriod) {
			if (foundPeriod || toInt) {
				return false;
			}
			foundPeriod = true;
		} else if (code < code0 || code > code9) {
			return false;
		}
	} // for each character
	return true;
} // canParseToNum()

// Gets the approximate length of the string in pixels.
function getTextSize(str) {
	"use strict";
	var px = 0, text, ch, posn;
	if (!isEmpty(str)) {
		text = cerToUnicode(str);
		for (posn = 0; posn < text.length; posn += 1) {
			ch = text.charAt(posn);
			if (text.charCodeAt(posn) === 176) {
				px += 5;
			} else {
				switch (ch) {
				case 'i':
				case 'j':
					px += 3;
					break;
				case ' ':
					if (posn > 0 && text.charAt(posn - 1) != ' ') {
						px += 3;
					}
					break;
				case 'f':
				case 't':
				case 'r':
					px += 4;
					break;
				case 'm':
					px += 11;
					break;
				default:
					px += 7;
					break;
				} // switch
			} // not 176
		} // for each character
	} // if not empty
	return px;
} // getTextSize()

// code courtesy:
// http://insights.iwarp.com/advanced/javascript/validate/formemail.html
// checking if 1) @ is present  2) 3 or 4 chars after .
function isValidEmail(checkEmail) {
	"use strict";
	return (checkEmail.indexOf('@') > 0 &&
			(checkEmail.charAt(checkEmail.length - 4) == '.' ||
				checkEmail.charAt(checkEmail.length - 3) == '.'));
} // isValidEmail

// used in conjunction with makeButton()
function lighten(button) {
	"use strict";
	button.className = 'button button-over';
} // lighten

// used in conjunction with makeButton()
function darken(button) {
	"use strict";
	button.className = 'button button-normal';
} // darken

// used in conjunction with makeButton()
function depress(button) {
	"use strict";
	button.className = 'button button-active';
	return false; // prevent selections
} // depress

// Code courtesy: Webmonkey (http://hotwired.lycos.com/webmonkey/98/29/index1a_page11.html)
function setCookie(name, cookieval) {
	"use strict";
	var val = trimWhiteSpaces(cookieval),
		add_cookie,
		exdate = new Date();
	if (trimWhiteSpaces(name) === '') {
		return;
	}
	if (val === '') {
		val = '[]';
	}
	val = encodeURIComponent(val);
	// alert('jslib.js: saving cookie ' + val);
	add_cookie = name + '=' + val;
	// added by RBG
	exdate.setDate(exdate.getDate() + 1000 * 60 * 60 * 12); // 12 hours
	add_cookie += ';expires=' + exdate.toGMTString();
	// end RBG addition
	document.cookie = add_cookie;
} // setCookie()

// Code courtesy: Webmonkey (http://hotwired.lycos.com/webmonkey/98/29/index1a_page11.html)
function getCookie(name) {
	"use strict";
	//alert(' getst called' );
	var val = '',
		theBigCookie = document.cookie,
		lastChar,
		firstChar;
	if (theBigCookie === '') {
		return val;
	}
	firstChar = theBigCookie.indexOf(name);
	// find the start of 'name'
	if (firstChar != -1) {
		// if you found the cookie
		firstChar += name.length + 1;
		// skip 'name' and '='
		lastChar = theBigCookie.indexOf(';', firstChar);
		// Find the end of the value string (i.e. the next ';').
		if (lastChar == -1) {
			lastChar = theBigCookie.length;
		}
		val = decodeURIComponent(theBigCookie.substring(firstChar, lastChar));
	}
	if (val == '[]') {
		val = '';
	}
	return val;
} // getCookie

// defines String.builder() object
if (!String.builder) {
	String.builder = function () {
		"use strict";
		var buffer = [];
		this.size = function () {
			return buffer.length;
		};
		this.length = function () {
			return buffer.join('').length;
		};
		this.append = function (str) {
			buffer.push(str);
			return this;
		};
		this.prepend = function (str) {
			buffer.splice(0, 0, str);
			return this;
		};
		this.clear = function () {
			buffer = [];
		};
		this.insert = function (index, str) {
			if (buffer[index]) {
				buffer[index] = str;
			} else {
				return;
			}
		};
		this.replace = function (find, replace) {
			var exp, i;
			for (i = 0; i < buffer.length; i += 1) {
				exp = new RegExp(find, 'gm');
				buffer[i] = buffer[i].replace(exp, replace);
			}
		};
		this.remove = function (first, second) {
			var i, index, length;
			if (typeof first == 'string') {
				for (i = 0; i < buffer.length; i += 1) {
					if (first == buffer[i]) {
						buffer.splice(i, 1);
					}
				}
			} else {
				index = first;
				// length = (arguments.length > 1 ? arguments[1] : 1);
				length = second || 1;
				buffer.splice(index, length);
			}
		};
		this.toString = function () {
			return buffer.join('');
		};
	}; // String.builder
} // if !String.builder

// get a random string of numbers
function getRandString() {
	"use strict";
	var random_str = new String.builder(), i;
	for (i = 1; i <= 20; i += 1) {
		random_str.append(String(Math.round((Math.random() * 9))));
	}
	return random_str.toString();
} // getRandString()

// makes an alert from text that may contain CERs
function toAlert(out) {
	"use strict";
	alert(cerToUnicode(out));
} // toAlert()

// opens up a new page to display a long alert
function toBigAlert(pathToRoot, out) {
	"use strict";
	var urlBld = new String.builder().append(pathToRoot).
			append('includes/showAlert.jsp?text=').
			append(encodeURIComponent(out));
	openBigAlertWindow(urlBld.toString());
} // toBigAlert()

// makes a confirm popup from text that may contain CERs
function toConfirm(out) {
	"use strict";
	return confirm(cerToUnicode(out));
} // toConfirm

// gets an HTML element
function getCell(cellName) {
	"use strict";
	return document.getElementById(cellName);
} // getCell()

// gets if a cell exists
function cellExists(cellName) {
	"use strict";
	return document.getElementById(cellName);
} // cellExists()

// sets the inner HTML of a cell to a value
function setInnerHTML(cellName, value) {
	"use strict";
	var cell = document.getElementById(cellName);
	if (cell) {
		cell.innerHTML = value;
	}
} // setInnerHTML()

// clears the inner HTML of a cell
function clearInnerHTML(cellName) {
	"use strict";
	setInnerHTML(cellName, '');
} // setInnerHTML()

// gets the inner HTML of a cell
function getInnerHTML(cellName) {
	"use strict";
	var cell = document.getElementById(cellName);
	return (cell ? cell.innerHTML : '');
} // getInnerHTML()

// sets the source of an element to a value
function getSrc(cellName) {
	"use strict";
	var cell = document.getElementById(cellName);
	return (cell ? cell.src : '');
} // getSrc()

// sets the source of an element to a value
function setSrc(cellName, value) {
	"use strict";
	var cell = document.getElementById(cellName);
	if (cell) {
		cell.src = value;
	}
} // setSrc()

// shows or hides a cell
function setVisibility(cellName, value) {
	"use strict";
	var cell = document.getElementById(cellName);
	if (cell) {
		cell.style.visibility = value;
	}
} // setVisibility()

// sets a cell's height
function setHeight(cellName, value) {
	"use strict";
	var cell = document.getElementById(cellName);
	if (cell) {
		cell.style.height = value;
	}
} // setHeight()

// sets a cell's width
function setWidth(cellName, value) {
	"use strict";
	var cell = document.getElementById(cellName);
	if (cell) {
		cell.style.width = value;
	}
} // setWidth()

// hides a cell
function hideCell(cellName) {
	"use strict";
	setVisibility(cellName, 'hidden');
} // hideCell()

// shows a cell
function showCell(cellName) {
	"use strict";
	setVisibility(cellName, 'visible');
} // showCell()

// gets the value of an input element
function getValue(cellName) {
	"use strict";
	var cell = document.getElementById(cellName);
	return (cell ? cell.value : null);
} // getValue()

// sets the value of an input element
function setValue(cellName, value) {
	"use strict";
	var cell = document.getElementById(cellName);
	if (cell) {
		cell.value = value;
	}
} // setValue()

// disables or enables an input element
function setDisabled(cellName, value) {
	"use strict";
	var cell = document.getElementById(cellName);
	if (cell) {
		cell.disabled = value;
	}
} // setDisabled()

// disables an input element
function disableCell(cellName) {
	"use strict";
	setDisabled(cellName, true);
} // disableCell()

// enables an input element
function enableCell(cellName) {
	"use strict";
	setDisabled(cellName, false);
} // enableCell()

// checks or unchecks a checkbox
function setFormBoxChecked(box, value) {
	"use strict";
	if (box) {
		box.checked = value;
	}
} // setFormBoxChecked()

// checks or unchecks a checkbox
function setChecked(cellName, value) {
	"use strict";
	var cell = document.getElementById(cellName);
	setFormBoxChecked(cell, value);
} // setChecked()

// checks a checkbox
function checkCell(cellName) {
	"use strict";
	setChecked(cellName, true);
} // checkCell()

// unchecks a checkbox
function uncheckCell(cellName) {
	"use strict";
	setChecked(cellName, false);
} // uncheckCell()

// checks a checkbox
function checkFormBox(box) {
	"use strict";
	setFormBoxChecked(box, true);
} // checkFormBox()

// unchecks a checkbox
function uncheckFormBox(box) {
	"use strict";
	setFormBoxChecked(box, false);
} // uncheckFormBox()

// gets whether a form component that is a checkbox is checked
function isChecked(formElem) {
	"use strict";
	return formElem && formElem.checked;
} // isChecked()

// gets whether a document element that is a checkbox is checked
function getChecked(cellName) {
	"use strict";
	return isChecked(document.getElementById(cellName));
} // getChecked()

// selects an option in a pulldown menu
function selectCell(cellName) {
	"use strict";
	var cell = document.getElementById(cellName);
	if (cell) {
		cell.selected = true;
	}
} // selectCell()

// selects text in an element
function selectText(elem) {
	var range = document.createRange();
	range.selectNodeContents(elem);
	var sel = window.getSelection();
	sel.removeAllRanges();
	sel.addRange(range);
} // selectText()

/* Makes a new form that will invoke the given target page in a target 
 * window with the given name. */
function prepareForm(targetPage, targetPageName) {
	"use strict";
	var newForm = document.createElement('form');
	newForm.setAttribute('method', 'post');
	newForm.setAttribute('action', targetPage);
	// target name must be exactly the same as in openwindows.js
	newForm.setAttribute('target', targetPageName);
	return newForm;
} // prepareForm()

/* Makes a new hidden input field with the given name and value. */
function prepareField(name, value) {
	"use strict";
	var newField = document.createElement('input');              
	newField.setAttribute('name', name);
	newField.setAttribute('type', 'hidden');
	newField.setAttribute('value', value);
	return newField;
} // prepareField()

/* Copies a string to the clipboard. Must be called from within an 
 * event handler such as click. May return false if it failed, but
 * this is not always possible. Browser support for Chrome 43+, 
 * Firefox 42+, Safari 10+, Edge and IE 10+.
 * IE: The clipboard feature may be disabled by an administrator. By
 * default a prompt is shown the first time the clipboard is 
 * used (per session). */
function copyToClipboard(text) {
	var returnValue = false;
	if (window.clipboardData && window.clipboardData.setData) {
		// IE specific code path to prevent textarea being shown while dialog is visible.
		returnValue = clipboardData.setData('Text', text); 
		if (!returnValue) console.warn('Copy to clipboard with window.clipboardData failed.');
	} else if (document.queryCommandSupported 
			&& document.queryCommandSupported('copy')) {
		var textarea = document.createElement('textarea');
		textarea.textContent = text;
		textarea.style.position = 'fixed'; // Prevent scrolling to bottom of page in MS Edge.
		document.body.appendChild(textarea);
		textarea.select();
		try {
			returnValue = document.execCommand('copy'); // Security exception may be thrown
			if (!returnValue) console.warn('Copy to clipboard failed.');
		} catch (ex) {
			console.warn('Copy to clipboard failed with exception.', ex);
		} finally {
			document.body.removeChild(textarea);
		} // try
	} // if can copy to clipboard
	return returnValue;
} // copyToClipboard()

// ***** Unicode functions

function cerToAscii(str) {
	"use strict";
	/*jslint regexp:true*/
	var regex = /&([A-Za-z])(grave|acute|circ|tilde|uml|ring|cedil|slash|caron),/g,
		s = str.replace(regex, '$1');
	return s.replace(/&[^;]*;/, '');
} // cerToAscii()

function cerToUnicode(str) {
	"use strict";
	var entity_table = {
		'&quot;': String.fromCharCode(34), // Quotation mark. Not required
		'&amp;': String.fromCharCode(38), // Ampersand
		'&lt;': String.fromCharCode(60), // Less-than sign
		'&gt;': String.fromCharCode(62), // Greater-than sign
		'&nbsp;': String.fromCharCode(160), // Non-breaking space
		'&iexcl;': String.fromCharCode(161), // Inverted exclamation mark
		'&cent;': String.fromCharCode(162), // Cent sign
		'&pound;': String.fromCharCode(163), // Pound sign
		'&curren;': String.fromCharCode(164), // Currency sign
		'&yen;': String.fromCharCode(165), // Yen sign
		'&brvbar;': String.fromCharCode(166), // Broken vertical bar
		'&sect;': String.fromCharCode(167), // Section sign
		'&uml;': String.fromCharCode(168), // Diaeresis
		'&copy;': String.fromCharCode(169), // Copyright sign
		'&ordf;': String.fromCharCode(170), // Feminine ordinal indicator
		'&laquo;': String.fromCharCode(171), // Left-pointing double angle quotation mark
		'&not;': String.fromCharCode(172), // Not sign
		'&shy;': String.fromCharCode(173), // Soft hyphen
		'&reg;': String.fromCharCode(174), // Registered sign
		'&macr;': String.fromCharCode(175), // Macron
		'&deg;': String.fromCharCode(176), // Degree sign
		'&plusmn;': String.fromCharCode(177), // Plus-minus sign
		'&sup2;': String.fromCharCode(178), // Superscript two
		'&sup3;': String.fromCharCode(179), // Superscript three
		'&acute;': String.fromCharCode(180), // Acute accent
		'&micro;': String.fromCharCode(181), // Micro sign
		'&para;': String.fromCharCode(182), // Pilcrow sign
		'&middot;': String.fromCharCode(183), // Middle dot
		'&cedil;': String.fromCharCode(184), // Cedilla
		'&sup1;': String.fromCharCode(185), // Superscript one
		'&ordm;': String.fromCharCode(186), // Masculine ordinal indicator
		'&raquo;': String.fromCharCode(187), // Right-pointing double angle quotation mark
		'&frac14;': String.fromCharCode(188), // Vulgar fraction one-quarter
		'&frac12;': String.fromCharCode(189), // Vulgar fraction one-half
		'&frac34;': String.fromCharCode(190), // Vulgar fraction three-quarters
		'&iquest;': String.fromCharCode(191), // Inverted question mark
		'&Agrave;': String.fromCharCode(192), // A with grave
		'&Aacute;': String.fromCharCode(193), // A with acute
		'&Acirc;': String.fromCharCode(194), // A with circumflex
		'&Atilde;': String.fromCharCode(195), // A with tilde
		'&Auml;': String.fromCharCode(196), // A with diaeresis
		'&Aring;': String.fromCharCode(197), // A with ring above
		'&AElig;': String.fromCharCode(198), // AE
		'&Ccedil;': String.fromCharCode(199), // C with cedilla
		'&Egrave;': String.fromCharCode(200), // E with grave
		'&Eacute;': String.fromCharCode(201), // E with acute
		'&Ecirc;': String.fromCharCode(202), // E with circumflex
		'&Euml;': String.fromCharCode(203), // E with diaeresis
		'&Igrave;': String.fromCharCode(204), // I with grave
		'&Iacute;': String.fromCharCode(205), // I with acute
		'&Icirc;': String.fromCharCode(206), // I with circumflex
		'&Iuml;': String.fromCharCode(207), // I with diaeresis
		'&ETH;': String.fromCharCode(208), // Eth
		'&Ntilde;': String.fromCharCode(209), // N with tilde
		'&Ograve;': String.fromCharCode(210), // O with grave
		'&Oacute;': String.fromCharCode(211), // O with acute
		'&Ocirc;': String.fromCharCode(212), // O with circumflex
		'&Otilde;': String.fromCharCode(213), // O with tilde
		'&Ouml;': String.fromCharCode(214), // O with diaeresis
		'&times;': String.fromCharCode(215), // Multiplication sign
		'&Oslash;': String.fromCharCode(216), // O with stroke
		'&Ugrave;': String.fromCharCode(217), // U with grave
		'&Uacute;': String.fromCharCode(218), // U with acute
		'&Ucirc;': String.fromCharCode(219), // U with circumflex
		'&Uuml;': String.fromCharCode(220), // U with diaeresis
		'&Yacute;': String.fromCharCode(221), // Y with acute
		'&THORN;': String.fromCharCode(222), // Thorn
		'&szlig;': String.fromCharCode(223), // Sharp s. Also known as ess-zed
		'&agrave;': String.fromCharCode(224), // a with grave
		'&aacute;': String.fromCharCode(225), // a with acute
		'&acirc;': String.fromCharCode(226), // a with circumflex
		'&atilde;': String.fromCharCode(227), // a with tilde
		'&auml;': String.fromCharCode(228), // a with diaeresis
		'&aring;': String.fromCharCode(229), // a with ring above
		'&aelig;': String.fromCharCode(230), // ae. Also known as ligature ae
		'&ccedil;': String.fromCharCode(231), // c with cedilla
		'&egrave;': String.fromCharCode(232), // e with grave
		'&eacute;': String.fromCharCode(233), // e with acute
		'&ecirc;': String.fromCharCode(234), // e with circumflex
		'&euml;': String.fromCharCode(235), // e with diaeresis
		'&igrave;': String.fromCharCode(236), // i with grave
		'&iacute;': String.fromCharCode(237), // i with acute
		'&icirc;': String.fromCharCode(238), // i with circumflex
		'&iuml;': String.fromCharCode(239), // i with diaeresis
		'&eth;': String.fromCharCode(240), // eth
		'&ntilde;': String.fromCharCode(241), // n with tilde
		'&ograve;': String.fromCharCode(242), // o with grave
		'&oacute;': String.fromCharCode(243), // o with acute
		'&ocirc;': String.fromCharCode(244), // o with circumflex
		'&otilde;': String.fromCharCode(245), // o with tilde
		'&ouml;': String.fromCharCode(246), // o with diaeresis
		'&divide;': String.fromCharCode(247), // Division sign
		'&oslash;': String.fromCharCode(248), // o with stroke. Also known as o with slash
		'&ugrave;': String.fromCharCode(249), // u with grave
		'&uacute;': String.fromCharCode(250), // u with acute
		'&ucirc;': String.fromCharCode(251), // u with circumflex
		'&uuml;': String.fromCharCode(252), // u with diaeresis
		'&yacute;': String.fromCharCode(253), // y with acute
		'&thorn;': String.fromCharCode(254), // thorn
		'&yuml;': String.fromCharCode(255), // y with diaeresis
		'&OElig;': String.fromCharCode(338), // Latin capital ligature OE
		'&oelig;': String.fromCharCode(339), // Latin small ligature oe
		'&Scaron;': String.fromCharCode(352), // Latin capital letter S with caron
		'&scaron;': String.fromCharCode(353), // Latin small letter s with caron
		'&Yuml;': String.fromCharCode(376), // Latin capital letter Y with diaeresis
		'&fnof;': String.fromCharCode(402), // Latin small f with hook, function, florin
		'&circ;': String.fromCharCode(710), // Modifier letter circumflex accent
		'&tilde;': String.fromCharCode(732), // Small tilde
		'&Alpha;': String.fromCharCode(913), // Alpha
		'&Beta;': String.fromCharCode(914), // Beta
		'&Gamma;': String.fromCharCode(915), // Gamma
		'&Delta;': String.fromCharCode(916), // Delta
		'&Epsilon;': String.fromCharCode(917), // Epsilon
		'&Zeta;': String.fromCharCode(918), // Zeta
		'&Eta;': String.fromCharCode(919), // Eta
		'&Theta;': String.fromCharCode(920), // Theta
		'&Iota;': String.fromCharCode(921), // Iota
		'&Kappa;': String.fromCharCode(922), // Kappa
		'&Lambda;': String.fromCharCode(923), // Lambda
		'&Mu;': String.fromCharCode(924), // Mu
		'&Nu;': String.fromCharCode(925), // Nu
		'&Xi;': String.fromCharCode(926), // Xi
		'&Omicron;': String.fromCharCode(927), // Omicron
		'&Pi;': String.fromCharCode(928), // Pi
		'&Rho;': String.fromCharCode(929), // Rho
		'&Sigma;': String.fromCharCode(931), // Sigma
		'&Tau;': String.fromCharCode(932), // Tau
		'&Upsilon;': String.fromCharCode(933), // Upsilon
		'&Phi;': String.fromCharCode(934), // Phi
		'&Chi;': String.fromCharCode(935), // Chi
		'&Psi;': String.fromCharCode(936), // Psi
		'&Omega;': String.fromCharCode(937), // Omega
		'&alpha;': String.fromCharCode(945), // alpha
		'&beta;': String.fromCharCode(946), // beta
		'&gamma;': String.fromCharCode(947), // gamma
		'&delta;': String.fromCharCode(948), // delta
		'&epsilon;': String.fromCharCode(949), // epsilon
		'&zeta;': String.fromCharCode(950), // zeta
		'&eta;': String.fromCharCode(951), // eta
		'&theta;': String.fromCharCode(952), // theta
		'&iota;': String.fromCharCode(953), // iota
		'&kappa;': String.fromCharCode(954), // kappa
		'&lambda;': String.fromCharCode(955), // lambda
		'&mu;': String.fromCharCode(956), // mu
		'&nu;': String.fromCharCode(957), // nu
		'&xi;': String.fromCharCode(958), // xi
		'&omicron;': String.fromCharCode(959), // omicron
		'&pi;': String.fromCharCode(960), // pi
		'&rho;': String.fromCharCode(961), // rho
		'&sigmaf;': String.fromCharCode(962), // sigmaf
		'&sigma;': String.fromCharCode(963), // sigma
		'&tau;': String.fromCharCode(964), // tau
		'&upsilon;': String.fromCharCode(965), // upsilon
		'&phi;': String.fromCharCode(966), // phi
		'&chi;': String.fromCharCode(967), // chi
		'&psi;': String.fromCharCode(968), // psi
		'&omega;': String.fromCharCode(969), // omega
		'&thetasym;': String.fromCharCode(977), // Theta symbol
		'&upsih;': String.fromCharCode(978), // Greek upsilon with hook symbol
		'&piv;': String.fromCharCode(982), // Pi symbol
		'&ensp;': String.fromCharCode(8194), // En space
		'&emsp;': String.fromCharCode(8195), // Em space
		'&thinsp;': String.fromCharCode(8201), // Thin space
		'&zwnj;': String.fromCharCode(8204), // Zero width non-joiner
		'&zwj;': String.fromCharCode(8205), // Zero width joiner
		'&lrm;': String.fromCharCode(8206), // Left-to-right mark
		'&rlm;': String.fromCharCode(8207), // Right-to-left mark
		'&ndash;': String.fromCharCode(8211), // En dash
		'&mdash;': String.fromCharCode(8212), // Em dash
		'&lsquo;': String.fromCharCode(8216), // Left single quotation mark
		'&rsquo;': String.fromCharCode(8217), // Right single quotation mark
		'&sbquo;': String.fromCharCode(8218), // Single low-9 quotation mark
		'&ldquo;': String.fromCharCode(8220), // Left double quotation mark
		'&rdquo;': String.fromCharCode(8221), // Right double quotation mark
		'&bdquo;': String.fromCharCode(8222), // Double low-9 quotation mark
		'&dagger;': String.fromCharCode(8224), // Dagger
		'&Dagger;': String.fromCharCode(8225), // Double dagger
		'&bull;': String.fromCharCode(8226), // Bullet
		'&hellip;': String.fromCharCode(8230), // Horizontal ellipsis
		'&permil;': String.fromCharCode(8240), // Per mille sign
		'&prime;': String.fromCharCode(8242), // Prime
		'&Prime;': String.fromCharCode(8243), // Double Prime
		'&lsaquo;': String.fromCharCode(8249), // Single left-pointing angle quotation
		'&rsaquo;': String.fromCharCode(8250), // Single right-pointing angle quotation
		'&oline;': String.fromCharCode(8254), // Overline
		'&frasl;': String.fromCharCode(8260), // Fraction Slash
		'&euro;': String.fromCharCode(8364), // Euro sign
		'&weierp;': String.fromCharCode(8472), // Script capital
		'&image;': String.fromCharCode(8465), // Blackletter capital I
		'&real;': String.fromCharCode(8476), // Blackletter capital R
		'&trade;': String.fromCharCode(8482), // Trade mark sign
		'&alefsym;': String.fromCharCode(8501), // Alef symbol
		'&larr;': String.fromCharCode(8592), // Leftward arrow
		'&uarr;': String.fromCharCode(8593), // Upward arrow
		'&rarr;': String.fromCharCode(8594), // Rightward arrow
		'&darr;': String.fromCharCode(8595), // Downward arrow
		'&harr;': String.fromCharCode(8596), // Left right arrow
		'&crarr;': String.fromCharCode(8629), // Downward arrow with corner leftward. Also known as carriage return
		'&lArr;': String.fromCharCode(8656), // Leftward double arrow. ISO 10646 does not say that lArr is the same as the 'is implied by' arrow but also does not have any other character for that function. So ? lArr can be used for 'is implied by' as ISOtech suggests
		'&uArr;': String.fromCharCode(8657), // Upward double arrow
		'&rArr;': String.fromCharCode(8658), // Rightward double arrow. ISO 10646 does not say this is the 'implies' character but does not have another character with this function so ? rArr can be used for 'implies' as ISOtech suggests
		'&dArr;': String.fromCharCode(8659), // Downward double arrow
		'&hArr;': String.fromCharCode(8660), // Left-right double arrow
		// Mathematical Operators
		'&forall;': String.fromCharCode(8704), // For all
		'&part;': String.fromCharCode(8706), // Partial differential
		'&exist;': String.fromCharCode(8707), // There exists
		'&empty;': String.fromCharCode(8709), // Empty set. Also known as null set and diameter
		'&nabla;': String.fromCharCode(8711), // Nabla. Also known as backward difference
		'&isin;': String.fromCharCode(8712), // Element of
		'&notin;': String.fromCharCode(8713), // Not an element of
		'&ni;': String.fromCharCode(8715), // Contains as member
		'&prod;': String.fromCharCode(8719), // N-ary product. Also known as product sign. Prod is not the same character as U+03A0 'greek capital letter pi' though the same glyph might be used for both
		'&sum;': String.fromCharCode(8721), // N-ary summation. Sum is not the same character as U+03A3 'greek capital letter sigma' though the same glyph might be used for both
		'&minus;': String.fromCharCode(8722), // Minus sign
		'&lowast;': String.fromCharCode(8727), // Asterisk operator
		'&radic;': String.fromCharCode(8730), // Square root. Also known as radical sign
		'&prop;': String.fromCharCode(8733), // Proportional to
		'&infin;': String.fromCharCode(8734), // Infinity
		'&ang;': String.fromCharCode(8736), // Angle
		'&and;': String.fromCharCode(8743), // Logical and. Also known as wedge
		'&or;': String.fromCharCode(8744), // Logical or. Also known as vee
		'&cap;': String.fromCharCode(8745), // Intersection. Also known as cap
		'&cup;': String.fromCharCode(8746), // Union. Also known as cup
		'&int;': String.fromCharCode(8747), // Integral
		'&there4;': String.fromCharCode(8756), // Therefore
		'&sim;': String.fromCharCode(8764), // tilde operator. Also known as varies with and similar to. The tilde operator is not the same character as the tilde, U+007E, although the same glyph might be used to represent both
		'&cong;': String.fromCharCode(8773), // Approximately equal to
		'&asymp;': String.fromCharCode(8776), // Almost equal to. Also known as asymptotic to
		'&ne;': String.fromCharCode(8800), // Not equal to
		'&equiv;': String.fromCharCode(8801), // Identical to
		'&le;': String.fromCharCode(8804), // Less-than or equal to
		'&ge;': String.fromCharCode(8805), // Greater-than or equal to
		'&sub;': String.fromCharCode(8834), // Subset of
		'&sup;': String.fromCharCode(8835), // Superset of. Note that nsup, 'not a superset of, U+2283' is not covered by the Symbol font encoding and is not included.
		'&nsub;': String.fromCharCode(8836), // Not a subset of
		'&sube;': String.fromCharCode(8838), // Subset of or equal to
		'&supe;': String.fromCharCode(8839), // Superset of or equal to
		'&oplus;': String.fromCharCode(8853), // Circled plus. Also known as direct sum
		'&otimes;': String.fromCharCode(8855), // Circled times. Also known as vector product
		'&perp;': String.fromCharCode(8869), // Up tack. Also known as orthogonal to and perpendicular
		'&sdot;': String.fromCharCode(8901), // Dot operator. The dot operator is not the same character as U+00B7 middle dot
		// Miscellaneous Technical
		'&lceil;': String.fromCharCode(8968), // Left ceiling. Also known as an APL upstile
		'&rceil;': String.fromCharCode(8969), // Right ceiling
		'&lfloor;': String.fromCharCode(8970), // left floor. Also known as APL downstile
		'&rfloor;': String.fromCharCode(8971), // Right floor
		'&lang;': String.fromCharCode(9001), // Left-pointing angle bracket. Also known as bra. Lang is not the same character as U+003C 'less than'or U+2039 'single left-pointing angle quotation mark'
		'&rang;': String.fromCharCode(9002), // Right-pointing angle bracket. Also known as ket. Rang is not the same character as U+003E 'greater than' or U+203A 'single right-pointing angle quotation mark'
		// Geometric Shapes
		'&loz;': String.fromCharCode(9674), // Lozenge
		// Miscellaneous Symbols
		'&spades;': String.fromCharCode(9824), // Black (filled) spade suit
		'&clubs;': String.fromCharCode(9827), // Black (filled) club suit. Also known as shamrock
		'&hearts;': String.fromCharCode(9829), // Black (filled) heart suit.
		'&diams;': String.fromCharCode(9830) // Black (filled) diamond suit
	};
	str = str.replace(/&#(\d+);/g,
			function (matched, capture1) {
				/*jslint unparam:true*/
				return (capture1 == '38' ? '&amp;' : String.fromCharCode(capture1));
			});
	/*jslint regexp:true*/
	str = str.replace(/&[^;]*;/g,
			function (matched) {
				return entity_table[matched];
			});
	return str;
} // cerToUnicode()

function unicodeToCER(str) {
    return str.replace(/./gm, function(s) {
		var code = s.charCodeAt(0);
		return (code < 128 ? s : '&#' + code + ';');
	});
} // unicodeToCER()

function hasUnicode(str) {
	for (var i = 0; i < str.length(); i++) {
		if (str.charCodeAt(i) >= 128) return true;
	}
	return false;
} // hasUnicode()

// Browser Detection Javascript
// copyright 1 February 2003, by Stephen Chapman, Felgall Pty Ltd
// You have permission to copy and use this javascript provided that
// the content of the script is not changed in any way.
function whichBrowser() {
	"use strict";
	var agt = navigator.userAgent.toLowerCase();
	if (agt.indexOf('opera') != -1) {
		return 'Opera';
	}
	if (agt.indexOf('staroffice') != -1) {
		return 'Star Office';
	}
	if (agt.indexOf('webtv') != -1) {
		return 'WebTV';
	}
	if (agt.indexOf('beonex') != -1) {
		return 'Beonex';
	}
	if (agt.indexOf('chimera') != -1) {
		return 'Chimera';
	}
	if (agt.indexOf('netpositive') != -1) {
		return 'NetPositive';
	}
	if (agt.indexOf('phoenix') != -1) {
		return 'Phoenix';
	}
	if (agt.indexOf('firefox') != -1) {
		return 'Firefox';
	}
	if (agt.indexOf('safari') != -1) {
		return 'Safari';
	}
	if (agt.indexOf('skipstone') != -1) {
		return 'SkipStone';
	}
	if (agt.indexOf('msie') != -1) {
		return 'Internet Explorer';
	}
	if (agt.indexOf('netscape') != -1) {
		return 'Netscape';
	}
	if (agt.indexOf('mozilla/5.0') != -1) {
		return 'Mozilla';
	}
	if (agt.indexOf('\/') != -1) {
		if (agt.substr(0, agt.indexOf('\/')) != 'mozilla') {
			return navigator.userAgent.substr(0, agt.indexOf('\/'));
		}
		return 'Netscape';
	}
	if (agt.indexOf(' ') != -1) {
		return navigator.userAgent.substr(0, agt.indexOf(' '));
	}
	return navigator.userAgent;
} // whichBrowser()

// Gets the version of Internet Explorer, or 0 if not IE.
function getIEVersion() {
	"use strict";
	var ieVersion = 0,
		pattern = /MSIE (\d+\.\d+);/;
	if (pattern.test(navigator.userAgent)) {
		ieVersion = Number(RegExp.$1);
	} // if browser is IE of any version
	return ieVersion;
} // getIEVersion ()
// --> end HTML comment
