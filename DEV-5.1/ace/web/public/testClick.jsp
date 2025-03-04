<html>
<head>
<script src="../js/position.js" type="text/javascript"></script>
<script src="../js/wz_jsgraphics.js" type="text/javascript"></script>
<script type="text/javascript">
// <!-- >
function mark() {
	// alert("mouseclick");
	var xpos = 20;
	var ypos = 20;
	JSGraphics.fillEllipse(xpos - 2, ypos - 2, xpos + 2, ypos + 2);
} // mark()

function getCoord() {
	//alert("mousemove");	
}
// -->
</script>
</head>
<body>
<table id="testArea" width = "100%">
<tr><td align="center">
<div id="canvas" style="position:relative; left:0px; top:0px;
						width:600px; height:2px; overflow:visible;">
<script type="text/javascript">
// <!-- >
	var JSGraphics = new jsGraphics('canvas');
	JSGraphics.setColor('#FF0000'); //Red
	JSGraphics.setStroke(2);
// -->
</script>
</div>
<img src="img/710_120.png" width="400" height="400" alt="test area" title="test area"
	onclick="mark()" onMouseMove="getCoord()">
</td></tr>
</table>

</body>
