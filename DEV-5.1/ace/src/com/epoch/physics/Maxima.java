package com.epoch.physics;

import static com.epoch.utils.utilConstants.CharConstants.*;
import com.epoch.AppConfig;
import com.epoch.utils.Utils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Holds and calls a running instance of Maxima. */
public final class Maxima {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Single instance of Maxima. */
	private static Maxima myMaxima = null;
	/** The stream sending commands to Maxima. */
	transient private final OutputStreamWriter toMaxima;
	/** The stream getting output from Maxima. */
	transient private final BufferedReader fromMaxima;

	/* **************** Constructors ****************/

	/** Constructor.  
	 * @throws	IOException	if the output or input stream between Java and
	 * Maxima stops working
	 */
	protected Maxima() throws IOException {
		final ProcessBuilder builder = 
				new ProcessBuilder(AppConfig.maximaProgram, "--very-quiet");
		final Process process = builder.start();
		toMaxima = new OutputStreamWriter(process.getOutputStream(), UTF8);
		fromMaxima = new BufferedReader(new InputStreamReader(
				process.getInputStream(), StandardCharsets.UTF_8));
		sendToMaxima("display2d:false;");
		debugPrint("Maxima: singleton instance created.");
	} // Maxima()

	/** Gets the running instance of Maxima.
	 * @return	the running instance of Maxima
	 * @throws	IOException	if the output or input stream between Java and
	 * Maxima stops working
	 */
	public static synchronized Maxima getMaxima() throws IOException {
		if (myMaxima == null) myMaxima = new Maxima();
		return myMaxima;
	} // getMaxima()

	/** Sends one or more Maxima commands to Maxima and gets the output,
	 * clearing any constant values afterward.
	 * @param	cmd	one or more Maxima commands
	 * @return	the output from the Maxima commands
	 * @throws	IOException	if the output or input stream between Java and
	 * Maxima stops working
	 */
	public String evaluate(String cmd) throws IOException {
		String output = "";
		synchronized (this) {
			output = sendToMaxima(cmd);
			sendToMaxima("kill(all);");
		} // synchronized
		return output;
	} // evaluate(String)

	/** Sends one or more Maxima commands to Maxima and gets the output.  Each
	 * set of commands must be followed by a last token command that is returned
	 * unchanged by Maxima and that signals that the output from this set of 
	 * commands is complete.  However, this token won't be returned if a line 
	 * has incorrect syntax, so, if a line begins with "incorrect syntax", end
	 * of output is signified by line with value "^".
	 * @param	cmd	one or more Maxima commands
	 * @return	the output from the Maxima commands
	 * @throws	IOException	if the output or input stream between Java and
	 * Maxima stops working
	 */
	private String sendToMaxima(String cmd) throws IOException {
		final String SELF = "Maxima.sendToMaxima: ";
		final String END_INPUT = "END_INPUT";
		final String END_BAD_SYNTAX = "^";
		final StringBuilder outputBld = new StringBuilder();
		if (!Utils.isEmpty(cmd)) {
			final StringBuilder cmdBld = Utils.getBuilder(cmd.trim());
			if (cmdBld.charAt(cmdBld.length() - 1) != ';') cmdBld.append(';');
			Utils.appendTo(cmdBld, ' ', END_INPUT, ";\n");
			debugPrint(SELF + "sending: ", cmdBld);
			toMaxima.write(cmdBld.toString());
			toMaxima.flush();
			boolean badSyntax = false;
			while (true) {
				String line = fromMaxima.readLine();
				if (line != null) line = line.trim();
				debugPrint(SELF + "line: ", line);
				if (line == null || line.equals(END_INPUT)
						|| (badSyntax && line.equals(END_BAD_SYNTAX))) {
					break;
				} else if (!line.startsWith("rat:")) {
					if (outputBld.length() > 0) outputBld.append('\n');
					outputBld.append(line);
				} // if line
				if (line.startsWith("incorrect syntax")) badSyntax = true;
			} // while true
		} // if there's a command
		final String output = outputBld.toString().trim();
		debugPrint(SELF + "output: ", output);
		return output;
	} // sendToMaxima(String)

} // Maxima
