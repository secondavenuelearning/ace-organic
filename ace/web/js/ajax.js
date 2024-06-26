/* Methods for creating the AJAX object and using it.  The page that includes it
 should also have:
	<!--
		dataValue = @@@@<%= data %>@@@@
	-->
	// <!-- avoid parsing the following as HTML
 	function callServer() {
		if (xmlHttp) {
			var url = [this jsp page's name];
			var toSend = [URI components];
			callAJAX(url, toSend);
		} // if the xmlHttp object exists
	} // callServer()

 	function updatePage() {
		if (xmlHttp.readyState == 4) { // ready to continue
			var response = xmlHttp.responseText;
			var dataValue = extractField(response, 'dataValue');
			setInnerHTML('data', dataValue);
			document.marvin.setMol(dataValue);
			...
		} // if ready to continue
	} // updatePage()
*/

/*jsl:option explicit*/

/* Create a new XMLHttpRequest object to talk to the Web server
* Code from
* http://www-128.ibm.com/developerworks/web/library/wa-ajaxintro1.html
* and
* http://www.w3schools.com/ajax/ajax_xmlhttprequest_create.asp
*/
var xmlHttp; // global variable; always set a value
var ajaxURIArr = [];

if (window.XMLHttpRequest) {
	xmlHttp = new XMLHttpRequest();
} else try {
	xmlHttp = new ActiveXObject('Microsoft.XMLHTTP');
} catch (e) {
	try {
		xmlHttp = new ActiveXObject('Msxml2.XMLHTTP');
	} catch (e2) {
		alert('Unable to create xmlHttp object for AJAX; please ' +
			'use a different browser.');
	} // try
} // if browser accepts XMLHttpRequest()

// send the response to the server
function callAJAX(url, toSend, method) {
	"use strict";
	xmlHttp.open('POST', url, true);
	xmlHttp.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
	// set follow-up method when the server returns the page at URL
	xmlHttp.onreadystatechange = (!method ? updatePage : method);
	xmlHttp.setRequestHeader('Accept', 'message/x-jl-formresult');
	xmlHttp.send(toSend);
} // callAJAX()

// extract a field from a web page.  We assume it follows ACE conventions,
// which is to have a line (most likely in a comment) that says
// field = @@@@value@@@@
function extractField(page, field) {
	"use strict";
	var DEMARCATOR = '@@@@',
		value = '',
		posn = page.search(field + ' = ' + DEMARCATOR);
	if (posn >= 0) {
		value = page.substring(posn);
		value = value.substring(value.search(DEMARCATOR) + 4);
		value = value.substring(0, value.search(DEMARCATOR));
		value = value.replace(/\\\\S\+\\\\n/g, 'S+'); // MOL needs this
		value = value.replace(/\\n/g, '\n'); // MOL needs this
		// value = value.replace(/\\r/g, '\n'); // MOL and MRV need this
		value = value.replace(/S\+/g, '\\S+\\n'); // MOL needs this
		value = value.replace(/\\"/g, '"'); // MRV needs this
		value = value.replace(/\\'/g, '\''); // text needs this
	} // if the field was found
	return value;
} // extractField()

function getURIParam(tag, value) {
	"use strict";
	var uriBld = new String.builder().
			append('&').append(tag).append('=').append(value);
	return uriBld.toString();
} // getURIParam()

function getAjaxURI() {
	"use strict";
	var toSend = new String.builder();
	var paramNum;
	for (paramNum = 0; paramNum < ajaxURIArr.length; paramNum += 1) {
		if (!isEmpty(ajaxURIArr[paramNum][0])) {
			if (paramNum !== 0) { toSend.append('&'); }
			toSend.append(ajaxURIArr[paramNum][0]).append('=');
		}
		toSend.append(ajaxURIArr[paramNum][1]);
	} // for each parameter
	return toSend.toString();
} // getAjaxURI()

// --> end HTML comment
