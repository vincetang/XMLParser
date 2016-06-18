package project_tortoise;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
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
			Node root = document.getDocumentElement();
			
			Node seq = getSequenceNode(root);
			
			if (seq == null){
				return;
			}
			
			ArrayList<Node> xsElementNodes = getChildrenWithName(seq, "xs:element");
			
			if(xsElementNodes.isEmpty()){
				return;
			}
			
			for (int i=0; i<xsElementNodes.size();i++){
				System.out.print(getXsdNodeData(xsElementNodes.get(i)).toString() + "\n");
			}
			
			//this.convertXsdToTabDelimited(document.getDocumentElement());
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
			if (currentNode.getNodeType() == Node.ELEMENT_NODE ) {				
				convertXsdToTabDelimited(currentNode);
			}
		}

		return "";
	}
	
	
	
	private ArrayList<String> xsdHeaders = new ArrayList<String>();
	
	
	public Map<String, String> getXsdNodeData(Node n){
		
		/** 
		 * Node -> HashMap of all node data
		 * e.g. 
		 * INPUT:  <xs:element name="asst_id" type="xs:long" minOccurs="0" />
		 * OUTPUT: {elt_type: "xs:element", 
		 * 				name: "asst_id", 
		 * 				type: "xs:long", 
		 * 		   minOccurs: "0"}
		 * xsdHeaders: Also creates list of all headers encountered and stores in xsdHeaders
		 */
		
		Map<String, String> data = new LinkedHashMap<String, String>();
		
		//add elt_type
		data.put("element type", n.getNodeName());
		
		//add attributes 
		NamedNodeMap atts = n.getAttributes();
		Node att;
		String attName;
		
		for (int i=0; i < atts.getLength(); i++){
			att = atts.item(i);
			attName = att.getNodeName();
			data.put(attName, att.getNodeValue());
			
			if (!xsdHeaders.contains(attName)){
				xsdHeaders.add(attName);
			}
		}
		
		// get child attributes
		Node curr;
		ArrayList<Node> subnodesWithAtts = Main.getSubNodesWithAttributes(n);
		for (int i=0; i < subnodesWithAtts.size(); i++){
			curr = subnodesWithAtts.get(i);
			atts = curr.getAttributes();

			data.put(curr.getNodeName(), atts.item(0).getNodeValue());
		}
		
		
		return data;
	}
	
	public Node getSequenceNode(Node root){
		/** 
		 * Root -> Node named "Sequence"
		 * All xs:elements are children of "Sequence".
		 * Get Sequence from root so we can later get xs:element nodes from Sequence
		 */
		
		Node complex = getChildWithName(root, "xs:complexType");
		Node sequence = getChildWithName(complex, "xs:sequence");
			
		return sequence;
	}
	
	
	public Node getChildWithName(Node n, String name){
		NodeList children = n.getChildNodes();
		Node curr; 
		
		for (int i = 0; i< children.getLength(); i++){
			curr = children.item(i);
			if (curr.getNodeName().equals(name)){
				return curr;
			}
		}
		return null;
	}

	public ArrayList<Node> getChildrenWithName(Node n, String name){
		NodeList children = n.getChildNodes();
		ArrayList<Node> result = new ArrayList<Node>(); 
		Node curr; 
		
		for (int i = 0; i< children.getLength(); i++){
			curr = children.item(i);
			if (curr.getNodeName().equals(name)){
				result.add(curr);
			}
		}
		return result;
	}

	
}
