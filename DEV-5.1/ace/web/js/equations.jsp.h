	// -->
	</script>
	<style type="text/css">
		input {margin-top: .7em}
		.output {
			border: 1px solid black;
			padding: 1em;
			width: auto;
			position: absolute; top: 0; left: 2em;
			min-width: 20em;
		}
		.box {position: relative}
	</style>
	<script type="text/x-mathjax-config">
	// <!-- >

	MathJax.Hub.Config({
		TeX: {
		}
	});

	// -->
	</script>
	<script type="text/javascript">
	// <!-- >

	function initEqnConstants() {
		initEqnConstants2('<%= Equations.XML_TAG %>',
				'<%= Equations.EQUATION_TAG %>',
				'<%= Equations.CONSTANTS_TAG %>',
				'<%= Equations.VARS_NOT_UNITS_TAG %>',
				'<%= Equations.DISABLED_ATTR_TAG %>');
	} // initEqnConstants()

	(function () {
		var QUEUE = MathJax.Hub.queue; // shorthand for the queue
		var math = null; // contains the element jaxes for the math output
		var mirrorName = null;
	
		// Get the element jax when MathJax has produced it.
		QUEUE.Push(function () {
			hideCell(mirrorName);
			math = MathJax.Hub.getAllJax(mirrorName)[0];
			showCell(mirrorName);
		});
	
		// The onchange event handler that typesets the math entered
		// by the user.  Hide the box, then typeset, then show it again
		// so we don't see a flash as the math is cleared and replaced.
		window.UpdateMath = function (eqnNum, text) {
			var cellName = 'eqn' + eqnNum;
			mirrorName = 'mirror' + eqnNum;
			var eqn = (noValue(text) ? getValue(cellName) : text);
			if (!isEmpty(eqn))
				QUEUE.Push(['Text', math, '\\displaystyle{' + eqn + '}']);
		}
	})();

// vim:filetype=jsp:
