package com.epoch.xmlparser;

import com.epoch.utils.Utils;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.net.MalformedURLException;

/** Reads an XML file.  Samuel R. Dost, Truth-N-Beauty Software, LLC. */
public class XML {
	
	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}
	
	//-------- SCANNER STATES -----------------------------------------
	/** Value for scanner state.  */ 
	private static final byte START		= 1;
	/** Value for scanner state.  */ 
	private static final byte DONE			= 2;
	/** Value for scanner state.  */ 
	private static final byte START_TAG	= 3;
	/** Value for scanner state.  */ 
	private static final byte DONE_TAG		= 4;
	/** Value for scanner state.  */ 
	private static final byte START_ATTR	= 5;
	/** Value for scanner state.  */ 
	private static final byte DONE_ATTR	= 6;
	//-----------------------------------------------------------------
	
	//-------- CHAR TYPES ---------------------------------------------
	/** Whitespace character.  */ 
	public static final byte CHAR_WHITE		= 0;	
	/** &lt; character.  */ 
	public static final byte CHAR_LT		= 1;
	/** &gt; character.  */ 
	public static final byte CHAR_GT		= 2;
	/** / character.  */ 
	public static final byte CHAR_SLASH		= 3;	
	/** ! character.  */ 
	public static final byte CHAR_BANG		= 4;
	/** - character.  */ 
	public static final byte CHAR_DASH		= 5;
	/** = character.  */ 
	public static final byte CHAR_EQUALS	= 6;
	/** " character.  */ 
	public static final byte CHAR_QUOTE		= 7;
	/** _ character.  */ 
	public static final byte CHAR_UNDER		= 8;
	/** A-Z or a-z.  */ 
	public static final byte CHAR_LETTER	= 9;
	/** 0-9.  */ 
	public static final byte CHAR_DIGIT		= 10;
	/** Any other character.  */ 
	public static final byte CHAR_OTHER		= 11;
	/** End of file character.  */ 
	public static final byte CHAR_EOF		= 12;
	/** ? character.  */ 
	public static final byte CHAR_QUESTION	= 13;
	/** [ character.  */ 
	public static final byte CHAR_LSQUARE	= 14;
	/** ] character.  */ 
	public static final byte CHAR_RSQUARE	= 15;
	/** { character.  */ 
	public static final byte CHAR_LCURLY	= 16;
	/** } character.  */ 
	public static final byte CHAR_RCURLY	= 17;
	//-----------------------------------------------------------------
	
	/** Root node of the XML file. */
	transient public XMLNode rootNode;
	/** The node currently being read. */
	transient private XMLNode currNode;
	
	/** Constructor. */
	public XML() {
		//initialize here
		rootNode = new XMLNode("_ROOT");
	} // end constructor XML(Void)
	
	/** Prepares a file at a particular URL for reading.
	 * @param	url	URL of the file
	 */
	public void loadURL(String url) {
		try {
				final URL xmlURL = new URL(url);
				parseXML(new BufferedReader(
							new InputStreamReader(
								xmlURL.openStream(),
								StandardCharsets.UTF_8
							)
						));
		} catch (MalformedURLException mue) {
				mue.printStackTrace();
		} catch (IOException ioe) {
				ioe.printStackTrace();
		}
	} // loadURL

	/** Reads a file starting at the root node and sets the node's value.
	 * @param	sourceXML	the file to be read
	 */
	public void parseXML(BufferedReader sourceXML) {
		byte state = START;
		int theChar = (int) ' ';
		currNode = rootNode;
		// parse sourceXML
		try {
			StringBuilder longBuffer = new StringBuilder();
			while (state != DONE) {
				sourceXML.mark(1); //so we can back up if we eat too much
				theChar = sourceXML.read();
				switch (charType(theChar)) {
					case CHAR_LT:
						final String longBufferStr = longBuffer.toString();
						if (!longBufferStr.matches("\\s*")) {
							currNode.setValue(longBufferStr);
							debugPrint("Setting value for current node: '", 
									currNode.nodeName, "'");
							debugPrint("Value: '", longBufferStr, "'");
						}
						longBuffer = new StringBuilder();
						parseTag(sourceXML);
						break;
					case CHAR_EOF:
						state = DONE;
						debugPrint("[DONE] Parsing!");
						break;
					default:
						longBuffer.append((char) theChar);
						// catch all other chars from within tags...
						break;
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	} // end void parseXML(BufferedReader)
	
	/** Reads an XML tag's value.  Tags are enclosed in &lt; &gt;. 
	 * @param	sourceXML	the file to be read
	 * @throws	IOException	if the source file can't be read
	 */
	private void parseTag(BufferedReader sourceXML) throws IOException {
		byte state = START_TAG;
		int theChar = (int) ' ';
		String name = "";
		String temp = "";
		// parse XML tag
		while (state != DONE_TAG) {
			theChar = sourceXML.read();
			switch (charType(theChar)) {
				case CHAR_SLASH:
					theChar = sourceXML.read();
					while (charType(theChar) == CHAR_WHITE) {
						theChar = sourceXML.read();
					}
					StringBuilder buffer = new StringBuilder();
					while (!Utils.among(charType(theChar), CHAR_WHITE, CHAR_GT)) {
						buffer.append((char) theChar);
						theChar = sourceXML.read();
					}
					name = buffer.toString();
					debugPrint("Closing tag: '", name, "'");
					while (charType(theChar) != CHAR_GT) {
						theChar = sourceXML.read();
					}
					state = DONE_TAG;
					currNode = currNode.parentNode;
					debugPrint("Tag Closed. Moving up 1 level to: '", 
							currNode.nodeName, "'");
					debugPrint("[DONE] Parsing child tag under: '", 
							currNode.nodeName, "'");
					break;
				case CHAR_BANG:
					buffer = Utils.getBuilder('<');
					while (charType(theChar) != CHAR_WHITE) {
						buffer.append((char) theChar);
						theChar = sourceXML.read();
					}
					temp = buffer.toString();
					if ("<!--".equals(temp)) {
						while (charType(theChar) != CHAR_GT) {
							buffer.append((char) theChar);
							theChar = sourceXML.read();
						}
						buffer.append('>');
						final XMLNode newNode = new XMLNode("Comment");
						newNode.parentNode = currNode;
						newNode.setValue(buffer.toString());
						currNode.addChildNode(newNode);
					} else if ("<!DOCTYPE".equals(temp)) {
						while (charType(theChar) != CHAR_RSQUARE) {
							buffer.append((char) theChar);
							theChar = sourceXML.read();
						}
						theChar = sourceXML.read();
						buffer.append("]>");
					}
					state = DONE_TAG;
					break;
				case CHAR_QUESTION:
					// parse header
					buffer = Utils.getBuilder('<');
					while (charType(theChar) != CHAR_GT) {
						buffer.append((char) theChar);
						theChar = sourceXML.read();
					}
					buffer.append('>');
					state = DONE_TAG;
					break;				
				default:
					debugPrint("[START] Parsing child tag under: '", 
							currNode.nodeName, "'");
					while (charType(theChar) == CHAR_WHITE) {
						theChar = sourceXML.read();
					}
					buffer = new StringBuilder();
					while (!Utils.among(charType(theChar), CHAR_WHITE, CHAR_GT)) {
						buffer.append((char) theChar);
						theChar = sourceXML.read();
					}
					name = buffer.toString();
					debugPrint("Opening tag: '", name, "'");
					final XMLNode newNode = new XMLNode(name);
					newNode.parentNode = currNode;
					currNode.addChildNode(newNode);
					currNode = newNode;
					debugPrint("Tag Opened. Moving down one level to: '", 
							currNode.nodeName, "'");					
					while (charType(theChar) == CHAR_WHITE) {
						theChar = sourceXML.read();
					}
					if (charType(theChar) != CHAR_GT) theChar = parseAttr(sourceXML, theChar);
					while (charType(theChar) != CHAR_GT) {
						theChar = sourceXML.read();
					}
					state = DONE_TAG;
					break;
			}
		}
	} // end void parseTag(BufferedReader)
	
	/** Reads the next character of an XML attribute's value.
	 * @param	sourceXML	the file to be read
	 * @param	sourceCh	integer corresponding to the character at the beginning of
	 * the reading frame
	 * @return	integer corresponding to the character following the end of the 
	 * reading frame
	 * @throws	IOException	if the source file can't be read
	 */
	private int parseAttr(BufferedReader sourceXML, int sourceCh) throws IOException {
		byte state = START_ATTR;
		debugPrint("[START] Parsing attributes for: '", 
				currNode.nodeName, "'");
		int theChar = sourceCh;
		String attr = "";
		String value = "";
		// parse XML tag
		while (state != DONE_ATTR) {
			StringBuilder buffer = new StringBuilder();
			while (!Utils.among(charType(theChar), CHAR_WHITE, CHAR_EQUALS)) {
				buffer.append((char) theChar);
				theChar = sourceXML.read();
			}
			attr = buffer.toString();
			debugPrint("New Attribute: '", attr, "'");
			while (charType(theChar) != CHAR_QUOTE) {
				theChar = sourceXML.read();
			}
			theChar = sourceXML.read();
			buffer = new StringBuilder();
			while (charType(theChar) != CHAR_QUOTE) {
				buffer.append((char) theChar);
				theChar = sourceXML.read();
			}
			value = buffer.toString();
			debugPrint("New Value: '", value, "'");
			currNode.addAttribute(attr, value);
			theChar = sourceXML.read();
			while (charType(theChar) == CHAR_WHITE) {
				theChar = sourceXML.read();
			}
			switch (charType(theChar)) {
				case CHAR_GT:
					state = DONE_ATTR;
					debugPrint("[DONE] Parsing attributes for: '", 
							currNode.nodeName, "'");
					break;
				case CHAR_SLASH:
					state = DONE_ATTR;
					debugPrint("[DONE] Parsing attributes for: '", 
							currNode.nodeName, "'");
					currNode = currNode.parentNode;
					debugPrint("Self-contained tag. Moving up 1 level to: '", 
							currNode.nodeName, "'");
					break;
				default:
					break;		
			}
		}
		return theChar;
	} // end void parseAttr(BufferedReader, int)
	
	/** Gets a byte corresponding to a character. 
	 * @param	theChar	an integer corresponding to a character
	 * @return	byte corresponding to the integer 
	 */
	private byte charType(int theChar) {
		if (theChar == -1) {
			return CHAR_EOF;
		} 
			
		switch ((char) theChar) {
			case ' ': case '\t': case '\n': case (char) 13: 
				return CHAR_WHITE;
			
			case '<': return CHAR_LT;
			case '>': return CHAR_GT;
			case '/': return CHAR_SLASH;
			case '!': return CHAR_BANG;
			case '-': return CHAR_DASH;
			case '=': return CHAR_EQUALS;
			case '\"': return CHAR_QUOTE;
			case '_': return CHAR_UNDER;
			case '?': return CHAR_QUESTION;
			case '[': return CHAR_LSQUARE;
			case ']': return CHAR_RSQUARE;
			case '{': return CHAR_LCURLY;
			case '}': return CHAR_RCURLY;
			
			case 'a': case 'b': case 'c': case 'd': case 'e':
			case 'f': case 'g': case 'h': case 'i': case 'j':
			case 'k': case 'l': case 'm': case 'n': case 'o':
			case 'p': case 'q': case 'r': case 's': case 't':
			case 'u': case 'v': case 'w': case 'x': case 'y': 
			case 'z': case 'A': case 'B': case 'C': case 'D':
			case 'E': case 'F': case 'G': case 'H': case 'I': 
			case 'J': case 'K': case 'L': case 'M': case 'N':
			case 'O': case 'P': case 'Q': case 'R': case 'S':
			case 'T': case 'U': case 'V': case 'W': case 'X':
			case 'Y': case 'Z':
				return CHAR_LETTER;
				
			case '0': case '1': case '2': case '3': case '4': 
			case '5': case '6': case '7': case '8': case '9':  
				return CHAR_DIGIT;
	
			default:
				return CHAR_OTHER;
		}
	} // end void charType(int)	
}
