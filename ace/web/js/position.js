// <!-- avoid parsing the following as HTML
/**
 * Copyright (c)2005-2009 Matt Kruse (javascripttoolbox.com)
 *
 * Dual licensed under the MIT and GPL licenses.
 * This basically means you can use this code however you want for
 * free, but don't claim to have written it yourself!
 * Donations always accepted: http://www.JavascriptToolbox.com/donate/
 *
 * Please do not link to the .js files on javascripttoolbox.com from
 * your site. Copy the files locally to your server instead.
 *
 */

/*jsl:option explicit*/

var Position = (function () {
	"use strict";
	var pos;
	// Resolve a string identifier to an object
	// ========================================
	function resolveObject(s) {
		var index;
		if (document.getElementById && document.getElementById(s) !== null) {
			return document.getElementById(s);
		}
		if (document.all && document.all[s] !== null) {
			return document.all[s];
		}
		if (document.anchors && document.anchors.length &&
				document.anchors.length > 0 && document.anchors[0].x) {
			for (index = 0; index < document.anchors.length; index += 1) {
				if (document.anchors[index].name == s) {
					return document.anchors[index];
				}
			}
			return null; // shouldn't happen
		}
		alert('cannot resolve object; please tell the ACE maintainers');
		return null;
	} // resolveObject(s)

	pos = {};
	pos.$VERSION = 1.0;

	// Set the position of an object
	// =============================
	pos.set = function (o, left, top) {
		if ((typeof o) == "string") {
			o = resolveObject(o);
		}
		if (!o || !o.style) {
			return false;
		}

		// If the second parameter is an object, it is assumed to be the result of getPosition()
		if ((typeof left) == "object") {
			pos = left;
			left = pos.left;
			top = pos.top;
		}

		o.style.left = left + "px";
		o.style.top = top + "px";
		return true;
	};

	// Retrieve the position and size of an object
	// ===========================================
	pos.get = function (o) {
		var fixBrowserQuirks = true,
			left = 0,
			top = 0,
			width = 0,
			height = 0,
			// parentNode = null, // unused: Raphael 7/2015
			offsetParent = null,
			originalObject,
			el,
			considerScroll;
		//	If a string is passed in instead of an object ref, resolve it
		if ((typeof o) == "string") {
			o = resolveObject(o);
		}

		if (!o) {
			return null;
		}

		offsetParent = o.offsetParent;
		originalObject = o;
		el = o; // "el" will be nodes as we walk up, "o" will be saved for
			// offsetParent references
		while (el.parentNode !== null) {
			el = el.parentNode;
			if (el.offsetParent !== null) {
				considerScroll = true;
				/*
				In Opera, if parentNode of the first object is scrollable, then
				offsetLeft/offsetTop already take its scroll position into
				account. If elements further up the chain are scrollable, their
				scroll offsets still need to be added in. And for some reason,
				TR nodes have a scrolltop value which must be ignored.
				*/
				if (fixBrowserQuirks && window.opera) {
					if (el == originalObject.parentNode || el.nodeName == "TR") {
						considerScroll = false;
					}
				}
				if (considerScroll) {
					if (el.scrollTop && el.scrollTop > 0) {
						top -= el.scrollTop;
					}
					if (el.scrollLeft && el.scrollLeft > 0) {
						left -= el.scrollLeft;
					}
				}
			}
			// If this node is also the offsetParent, add on the offsets and reset to the new offsetParent
			if (el == offsetParent) {
				left += o.offsetLeft;
				if (el.clientLeft && el.nodeName != "TABLE") {
					left += el.clientLeft;
				}
				top += o.offsetTop;
				if (el.clientTop && el.nodeName != "TABLE") {
					top += el.clientTop;
				}
				o = el;
				if (o.offsetParent === null) {
					if (o.offsetLeft) {
						left += o.offsetLeft;
					}
					if (o.offsetTop) {
						top += o.offsetTop;
					}
				}
				offsetParent = o.offsetParent;
			}
		}

		if (originalObject.offsetWidth) {
			width = originalObject.offsetWidth;
		}
		if (originalObject.offsetHeight) {
			height = originalObject.offsetHeight;
		}

		return {left: left, top: top, width: width, height: height};
	};

	// Retrieve the position of an object's center point
	// =================================================
	pos.getCenter = function (o) {
		var c = this.get(o);
		if (!c) { return null; }
		c.left = c.left + (c.width / 2);
		c.top = c.top + (c.height / 2);
		return c;
	};

	return pos;
}());
// --> end HTML comment
