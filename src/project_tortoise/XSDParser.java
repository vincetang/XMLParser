package project_tortoise;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
	private String outName;
	private ArrayList<Map<String, String>> xsdNodeData = new ArrayList<Map<String,String>>();

	
	
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
			
			this.outName = Main.filenameWithoutExtension(f);

			DocumentBuilderFactory docBuilderFactory = 
					DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document document = docBuilder.parse(f);
			Node root = document.getDocumentElement();
			
			Node seq = getSequenceNode(root);
			
			if (seq == null){
				return;
			}
			
			
			if (!xsdHeaders.contains("filename")){
				xsdHeaders.add("filename");
			}
			
			if (!xsdHeaders.contains("element type")){
				xsdHeaders.add("element type");
			}
			ArrayList<Node> xsElementNodes = getChildrenWithName(seq, "xs:element");
			
			if(xsElementNodes.isEmpty()){
				return;
			}
			
			for (int i=0; i<xsElementNodes.size();i++){
				xsdNodeData.add(getXsdNodeData(xsElementNodes.get(i)));
			}
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public void writeOutput(){
		/**TO DO: write output
		 * 		- first row headers
		 * 		- get length of headers
		 * 		- create array for new row
		 * 		- for each in datamap, look up key index in headers, insert value into row at that index
		**/

		StringBuilder str = new StringBuilder(); 
		
		str.append(xsdHeaders.toString().replace("[", "").replace("]", "").replace(",", "\t") + "\n");
		
		Map<String,String> currMap; 
		for (int i=0; i<xsdNodeData.size();i++){
			currMap = xsdNodeData.get(i);
			String[] newRow = new String[xsdHeaders.size()];
			
			//iterate through each entry in map
			
		    Iterator it = currMap.entrySet().iterator();
		    while (it.hasNext()) {
		    	Map.Entry e = (Map.Entry) it.next();
   	
		    	newRow[xsdHeaders.indexOf(e.getKey())] = (String) e.getValue();
		    	it.remove(); // avoids a ConcurrentModificationException
		    }
		    
			str.append(String.join("\t", newRow)+ "\n");
			
		}

		try {
			
			DateFormat dateFormat = new SimpleDateFormat("MMMdd_HHmmss");
			Date date = new Date();
			String strDate = dateFormat.format(date);
			File file = new File(this.outName + "_" + strDate + ".txt");

			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(str.toString());
			bw.close();

			System.out.println("Done");

		} catch (IOException e) {
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
		
		data.put("filename", this.outName);
		
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
			if (!xsdHeaders.contains(curr.getNodeName())){
				xsdHeaders.add(curr.getNodeName());
			}
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
