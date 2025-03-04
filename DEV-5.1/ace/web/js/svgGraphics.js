// <!-- avoid parsing the following as HTML
// Raphael Finkel 12/2013 © 2013 Creative Commons
// This file is meant as a replacement for wz_jsgraphics.js.
// It uses SVG instead of carefully placed <div> elements.

/*jsl:option explicit*/
/*jsl:import jslib.js*/
/*jsl:import xmlLib.js*/
/*jsl:import offsets.js*/

var Font, Stroke;

function SVGGraphics(canvas) {
	"use strict";
	var color = 'black',
		backgroundColor = 'white',
		stroke = 0,
		jg_onclick,
		jg_onmousemove,
		jg_onmousedown,
		jg_onmouseup,
		jg_onmouseover;
	var xmlDoc = getParentXMLDoc('svg');
	var svgNode = getFirstNode(xmlDoc, 'svg');
	this.canvas = document.getElementById(canvas);

	this.paint = function () {
		this.canvas.innerHTML = getXML(xmlDoc);
	}; // paint()

	this.setColor = function (newColor) {
		color = newColor;
	}; // setColor()

	this.setBackgroundColor = function (newColor) {
		backgroundColor = newColor.toLowerCase();
	}; // setBackgroundColor()

	this.clear = function () {
		xmlDoc = getParentXMLDoc('svg');
		svgNode = getFirstNode(xmlDoc, 'svg');
		this.canvas.innerHTML = '';
	}; // clear()

	this.drawLine = function (x1, y1, x2, y2) {
		var styleBld = new String.builder().
				append('stroke:').append(color).
				append(stroke == -1 ? ';stroke-dasharray:3,4' :
						stroke > 1 ? ';stroke-width:' + stroke : '').
				append(','),
			lineAttrs = [];
		lineAttrs.push(['x1', x1]);
		lineAttrs.push(['y1', y1]);
		lineAttrs.push(['x2', x2]);
		lineAttrs.push(['y2', y2]);
		lineAttrs.push(['style', styleBld.toString()]);
		addNewNode(xmlDoc, svgNode, 'line', lineAttrs);
	}; // drawLine()

	// -1: dotted
	// 2: higher stroke width
	// 0, 1: normal
	this.setStroke = function (x) { stroke = x; };

	this.fillPolygon = function (array_x, array_y) {
		var pointsBld = new String.builder(),
			ptNum;
		for (ptNum = 0; ptNum < array_x.length; ptNum += 1) {
			if (ptNum > 0) {
				pointsBld.append(' ');
			}
			pointsBld.append(array_x[ptNum]).
				append(',').
				append(array_y[ptNum]);
		} // for each point
		var styleAttrs = [['fill', color],
				['stroke', color],
				['stroke-width', '1']];
		var polygonAttrs = [['points', pointsBld.toString()],
				['style', getStyleString(styleAttrs)]];
		addNewNode(xmlDoc, svgNode, 'polygon', polygonAttrs);
	}; // fillPolygon()

	this.drawPolyline = function (array_x, array_y) {
		var pointsBld = new String.builder(),
			ptNum;
		for (ptNum = 0; ptNum < array_x.length; ptNum += 1) {
			if (ptNum > 0) {
				pointsBld.append(' ');
			}
			pointsBld.append(array_x[ptNum]).
					append(',').
					append(array_y[ptNum]);
		} // for each point
		var styleAttrs = [['fill', 'none'],
				['stroke', color]];
		if (stroke == -1) {
			styleAttrs.push(['stroke-dasharray', '3,4']);
		}
		var polylineAttrs = [['points', pointsBld.toString()],
				['style', getStyleString(styleAttrs)]];
		addNewNode(xmlDoc, svgNode, 'polyline', polylineAttrs);
	}; // drawPolyline()

	// from here on I take the functions from wz_jsgraphics.js
	// with some modification

	function JsgStroke() { this.DOTTED = -1; }
	Stroke = new JsgStroke();

	this.setFont = function (fam, sz, sty) {
		this.ftFam = fam;
		this.ftSz = sz;
		this.ftSty = sty || Font.PLAIN;
	}; // setFont()

	this.drawString = function (text, x, y) {
		var BG_OFFSETS = [-4, -12], // determined empirically
			pxPosn = this.ftSz.indexOf('px'),
			fontSizeStr = this.ftSz.substring(0, pxPosn),
			fontSize = parseToInt(fontSizeStr),
			gNode,
			textNode;
		var styleAttrs = [['position', 'absolute'],
				['white-space', 'nowrap'],
				['left', new String.builder().append(x).append('px').toString()],
				['top', new String.builder().append(y).append('px').toString()],
				['font-family', this.ftFam],
				['font-size', this.ftSz],
				['cursor', 'default'],
				['fill', color],
				[this.ftSty]
				];
		var bgAttrs = [];
		bgAttrs.push(['x', new String.builder().append(x + BG_OFFSETS[x]).append('px').toString()]);
		var textAttrs = [];
		textAttrs.push(['x', new String.builder().append(x).append('px').toString()]);
		textAttrs.push(['y', new String.builder().append(y).append('px').toString()]);
		textAttrs.push(['style', styleBld.toString()]);
		if (jg_onclick) {
			textAttrs.push(['onclick', jg_onclick]);
		}
		if (jg_onmousemove) {
			textAttrs.push(['onmousemove', jg_onmousemove]);
		}
		if (jg_onmousedown) {
			textAttrs.push(['onmousedown', jg_onmousedown]);
		}
		if (jg_onmouseup) {
			textAttrs.push(['onmouseup', jg_onmouseup]);
		}
		if (jg_onmouseover) {
			textAttrs.push(['onmouseover', jg_onmouseover]);
		}
		var gNode, textNode;
		if (backgroundColor !== 'white') {
			bgAttrs.push(['x', new String.builder().
					append(x + BG_OFFSETS[x]).append('px').toString()]);
			bgAttrs.push(['y', new String.builder().
					append(y + BG_OFFSETS[y]).append('px').toString()]);
			bgAttrs.push(['width', new String.builder().
					append(getTextSize(text) + 10).append('px').toString()]);
			bgAttrs.push(['height', new String.builder().
					append(fontSize + 4).append('px').toString()]);
			bgAttrs.push(['fill', backgroundColor]);
			gNode = addNewNode(xmlDoc, svgNode, 'g');
			addNewNode(xmlDoc, gNode, 'rect', bgAttrs);
			textNode = addNewNode(xmlDoc, gNode, 'text', textAttrs);
			textNode.appendChild(getTextNode(xmlDoc, text));
		} else {
			textNode = addNewNode(xmlDoc, svgNode, 'text', textAttrs);
			textNode.appendChild(getTextNode(xmlDoc, text));
		} // if there's a background color
		/*
		textBld.append('<div class="unselectable" style="position:absolute;white-space:nowrap;')
				.append('left:').append(x).append('px;')
				.append('top:').append(y).append('px;')
				.append('font-family:').append( this.ftFam).append(';')
				.append('font-size:').append(this.ftSz).append(';')
				.append('cursor:default;')
				.append('color:').append(color).append(';')
				.append('backgroundColor:').append(backgroundColor).append(';')
				.append(this.ftSty).append('"')
				.append(jg_onclick? ' onclick="' + jg_onclick + '"': '')
				.append(jg_onmousemove? ' onmousemove="' + jg_onmousemove + '"': '')
				.append(jg_onmousedown? ' onmousedown="' + jg_onmousedown + '"': '')
				.append(jg_onmouseup? ' onmouseup="' + jg_onmouseup + '"': '')
				.append(jg_onmouseover? ' onmouseover="' + jg_onmouseover + '"': '')
				.append('>')
				.append(txt)
				.append('<\/div>');
			// alert(html);
			// this.paint(); */
	}; // drawString()

	this.setOnclick = function (fn) {
		jg_onclick = fn;
	}; // setOnclick()

	this.setOnmousemove = function (fn) {
		jg_onmousemove = fn;
	}; // setOnmousemove()

	this.setOnmousedown = function (fn) {
		jg_onmousedown = fn;
	}; // setOnmousedown()

	this.setOnmouseup = function (fn) {
		jg_onmouseup = fn;
	}; // setOnmousedown()

	this.setOnmouseover = function (fn) {
		jg_onmouseover = fn;
	}; // setOnmouseover()

} // SVGGraphics

function JsgFont() {
	"use strict";
	this.PLAIN = 'font-weight:normal;';
	this.BOLD = 'font-weight:bold;';
	this.ITALIC = 'font-style:italic;';
	this.ITALIC_BOLD = this.ITALIC + this.BOLD;
	this.BOLD_ITALIC = this.ITALIC_BOLD;
}
Font = new JsgFont();

// --> end HTML comment
