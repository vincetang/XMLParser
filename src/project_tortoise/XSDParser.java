package project_tortoise;

import java.io.File;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



// filename, elementType, name, <attributes..>, parent element, parent complexType

public class XSDParser {
		
	private StringBuilder output;
	private ArrayList<String> headers;
	private ArrayList<String> values;
	
	
	/**
	 * @param s The name of the header to add
	 * @return The index of the header that was just added
	 */
	public int addToHeaders(String s) {
		this.headers.add(s);
		return (this.headers.indexOf(s));
	}
	
	public void addToOutput(String s) {
		this.output.append(s);
	}
	
	public void parseFile(File f) {
		try {
			DocumentBuilderFactory docBuilderFactory = 
					DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document document = docBuilder.parse(f);
			this.convertXsdToTabDelimited(document.getDocumentElement());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	/** 
	 * Recursively looks at every element and stores its name, attribute, and type
	 * for output
	 * @param root Root node of XSD/XML document
	 * @return
	 */
	public String convertXsdToTabDelimited(Node node) {
		System.out.println(node.getNodeName());
		
		NodeList nodeList = node.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node currentNode = nodeList.item(i);
			if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
				convertXsdToTabDelimited(currentNode);
			}
		}

		return "";
	}
}
