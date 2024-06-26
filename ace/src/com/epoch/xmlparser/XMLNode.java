package com.epoch.xmlparser;

	//-----------------------------------------------------------------
	//-------- Samuel R. Dost
	//-------- Truth-N-Beauty Software, LLC.  
	//-------- XML Container Node
	//-----------------------------------------------------------------

import java.util.ArrayList;
import java.util.List;

/** A node in an XML document.  Samuel R. Dost, Truth-N-Beauty Software, LLC. */
public class XMLNode {
	
	// Tree Nodes
	/** Child nodes of this node. */
	transient private List<XMLNode> childNodes = new ArrayList<XMLNode>();
	/** First child of this node. */
	transient public XMLNode firstChild;
	/** Last child of this node. */
	transient public XMLNode lastChild;
	/** Node following this one at the same level. */
	// public XMLNode nextSibling; // unused
	/** Node preceding this one at the same level. */
	// public XMLNode previousSibling; // unused
	/** Parent node. */ 
	public XMLNode parentNode;
	
	// Information about Self
	/** Name of this node. */
	transient public String nodeName = "";
	/** Value of this node. */
	transient public String nodeValue = ""; // apparently never read
	/** Attributes of this node. */
	transient private List<String[]> attributes = new ArrayList<String[]>();
	
	/** Constructor.
	 */
	public XMLNode() {
		// intentionally empty
	} // end constructor XMLNode(Void)
	
	/** Constructor.
	 * @param	name	name of the new node
	 */
	public XMLNode(String name) {
		nodeName = name;
	} // end constructor XMLNode(String)
	
	/** Gets the value of an attribute of this node.
	 * @param	attrName	name of the attribute
	 * @return	value of the attribute, or null if not found
	 */
	public String attribute(String attrName) {
		// search for and return proper attribute
		for (int attrNum = 0; attrNum < attributes.size(); attrNum++) {
			final String[] attribute = attributes.get(attrNum);
			if (attribute[0].equals(attrName)) { 
				return attribute[1]; 
			}
		}
		return null;
	} // end String attribute(String)
	
	/** Adds an attribute and its value to this node.
	 * @param	attrName	name of the attribute
	 * @param	value	value of the attribute
	 */
	public void addAttribute(String attrName, String value) {
		final String[] pairing = {attrName, value};
		attributes.add(pairing);		
	} // end void addAttribute(String, String)
	
	/** Adds a child node to this node.
	 * @param	child	the child node to add
	 */
	public void addChildNode(XMLNode child) {
		if (firstChild == null) {
			firstChild = child;
		/*
		} else {
			lastChild.nextSibling = child; // not used
			child.previousSibling = lastChild; // no used
		*/
		}
		lastChild = child;
		childNodes.add(child);
		/* childNodes.add(child);
		firstChild = childNodes.firstElement();
		lastChild = childNodes.lastElement();
		if (childNodes.size() > 1) {
			for (int x = 0; x < childNodes.size(); x++) {
				if (x == 0) {
					childNodes.get(x).nextSibling = childNodes.get(x + 1);
				} else if (x == childNodes.size() - 1) {
					childNodes.get(x).previousSibling = childNodes.get(x - 1);
				} else {
					childNodes.get(x).nextSibling = childNodes.get(x + 1);
					childNodes.get(x).previousSibling = childNodes.get(x - 1);
				}
			}
		}
		*/
	} // end void addChildNode(XMLNode) 
	
	/** Gets a child node of this node.
	 * @param	name	name of the node to get
	 * @return	the child node
	 */
	public XMLNode getChildNode(String name) {
		for (int nodeNum = 0; nodeNum < childNodes.size(); nodeNum++) {
			final XMLNode childNode = childNodes.get(nodeNum);
			if (childNode.nodeName.equalsIgnoreCase(name)) {
				return childNode;
			}
		} // for each child node
		return null;
	} // end XMLNode getChildNode(String)
	
	/** Sets the value of this node.
	 * @param	value	value of the node
	 */
	public void setValue(String value) {
		nodeValue = value;
	} // end void setValue(String)
	
} // end class XMLNode
