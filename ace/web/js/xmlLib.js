// <!-- avoid parsing the following as HTML
/*jsl:option explicit*/
/*jsl:import jslib.js*/

var LT = '<';
var GT = '>';
var NL = '\n';
var MAKE_READABLE = true;

/* ************* Methods using XML DOM specification ***************/

// makes the XML document with the parent node and attributes, returns the XML
// document
function getParentXMLDoc(parentNodeName, attrs) {
	var bld = new String.builder().append(LT).append(parentNodeName).
			append(GT + LT + '\/').append(parentNodeName).append(GT);
	var xmlDoc = (new DOMParser()).parseFromString(bld.toString(), 'text/xml');
	if (attrs != null) {
		setAttributes(getFirstNode(xmlDoc, parentNodeName), attrs);
	} // if there are attributes
	return xmlDoc;
} // getParentXMLDoc()

// gets the first node of the XML document with the given name; 
// can be used to return the parent node of the XML document
function getFirstNode(xmlDoc, nodeName) {
	var nodes = xmlDoc.getElementsByTagName(nodeName);
	return (nodes.length > 0 ? nodes[0] : null);
} // getFirstNode()

// makes and returns a string from a list of style properties, so the style
// attribute can be set to the returned value of this method
function getStyleString(props) {
	var bld = new String.builder(),
		propNum,
		prop,
		first = true;
	for (propNum = 0; propNum < props.length; propNum++) {
		prop = props[propNum];
		if (first) first = false;
		else bld.append(' ');
		bld.append(prop[0]);
		if (prop.length > 1) {
			bld.append(':').append(prop[1]);
		}
		bld.append(';');
	} // for each property
	return bld.toString();
} // getStyleString()

// sets attributes of a node or cell, returns it
function setAttributes(node, attrs) {
	"use strict";
	var attr, attrNum;
	if (attrs != null) {
		for (attrNum = 0; attrNum < attrs.length; attrNum += 1) {
			attr = attrs[attrNum];
			node.setAttribute(toValidXMLAttribute(attr[0]), 
					toValidXMLAttribute(new String(attr[1])));
		} // for each attribute
	} // if there are attributes
	return node;
} // setAttributes()

// makes a new node, returns it
function getNewNode(xmlDoc, childNodeName, attrs) {
	"use strict";
	var newNode = xmlDoc.createElement(childNodeName);
	setAttributes(newNode, attrs);
	return newNode;
} // getNewNode()

// makes a new node, appends it as child to existing node, returns new node
function addNewNode(xmlDoc, parentNode, childNodeName, attrs) {
	"use strict";
	var newNode = getNewNode(xmlDoc, childNodeName, attrs);
	parentNode.appendChild(newNode);
	return newNode;
} // addNewNode()

// makes a new node, inserts it before sibling as child to existing node, 
// returns new node
function insertNewNode(xmlDoc, parentNode, siblingNode, childNodeName, attrs) {
	"use strict";
	var newNode;
	if (siblingNode == null) {
		newNode = addNewNode(xmlDoc, parentNode, childNodeName, attrs);
	} else {
		newNode = getNewNode(xmlDoc, childNodeName, attrs);
		parentNode.insertBefore(newNode, siblingNode);
	} // if no sibling node
	return newNode;
} // insertNewNode()

function getTextNode(xmlDoc, text) {
	return xmlDoc.createTextNode(text);
} // getTextNode()

function getCDataNode(xmlDoc, text) {
	return xmlDoc.createCDATASection(text);
} // getCDataNode()

function getXML(xmlDoc, readable) {
	var xml = (new XMLSerializer()).serializeToString(xmlDoc);
	return (readable != null && readable ? makeReadable(xml) : xml);
} // getXML()

function makeReadable(xml) {
	return xml.replace(/></g, GT + NL + LT);
} // makeReadable()

/* ************* Methods for XML compatibility with certain characters ***************/

// Converts a keyCode to a character acceptable to the XML parser.
function keyCodeToXMLChar(keyCode) {
	"use strict";
	return (keyCode === 160 ? ' ' 
			: keyCode === 34 ? '&quot;' 
			: keyCode === 38 ? '&amp;' 
			: keyCode === 60 ? '&lt;' 
			: keyCode === 62 ? '&gt;' 
			: keyCode >= 128 ? '&#' + keyCode + ';'
			: String.fromCharCode(keyCode));
} // keyCodeToXMLChar()

// Converts a character to one acceptable to the XML parser.
function charToXMLChar(ch) {
	"use strict";
	return keyCodeToXMLChar(ch.charCodeAt(0));
} // charToXMLChar()

// Converts a string to XML acceptable to the parser.
function toValidXML(str) {
	"use strict";
	var strBld = new String.builder(),
		chNum;
	for (chNum = 0; chNum < str.length; chNum += 1) {
		strBld.append(charToXMLChar(str.charAt(chNum)));
	} // for each character in the string
	return strBld.toString();
} // toValidXML()

function toValidXMLAttribute(s) {
	"use strict";
	return toValidXML(s).replace(/"/g, '&quot;');
} // toValidXMLAttribute()

function toValidTextbox(s) {
	"use strict";
	return toValidXMLAttribute(s);
} // toValidTextbox()

// --> end HTML comment
