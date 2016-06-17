package project_tortoise;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;

public class Main {
	
	private String outname;
	private String format = "-t";
	private String delimitChar;
	private ArrayList<File> xmlFiles;
	private File xsdSchemaFile;
	
	private Map<String, String> outputMap;
	private HashMap<String, String> tagValues;
	private ArrayList<String> columnHeaderArray;
	private HashMap<String, ArrayList<String>> columnHeaderMap;
	
	public boolean validateInput(String filename, String type) {
		
		File f = new File(filename);
			
		if (f.exists() && f.isFile()) {
			if (type == "xml")
				this.xmlFiles.add(f);
			else
				this.xsdSchemaFile = f;

			//System.out.println("Read in file: " + filename);
			return true;
		} else {
			//System.out.println(filename + " not found. Omitting file.");
			return false;
		}
	}
	
	static boolean validateXMLAgainstXSD(File xmlFile, File xsdFile) {
		try {
			Source xmlSource = new StreamSource(xmlFile);
			SchemaFactory factory =
					SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
					Schema schemaFactory = factory.newSchema(new StreamSource(xsdFile));
					Validator validator = schemaFactory.newValidator();
					validator.validate(xmlSource);
					System.out.println(xmlFile.getName() + " has been validated against " + xsdFile.getName() + " successfully.");
					return true;
		} catch (SAXException e) {
			e.printStackTrace();
			System.out.println(xmlFile.getName() + " is NOT valid when checked against " + xsdFile.getName());
			System.out.println("Reason: " + e.getLocalizedMessage());
	
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("IOException when validating " + xmlFile.getName());
			System.out.println("Reason: " + e.getLocalizedMessage());
			return false;
			
		}
	}

	public void parseXML(File xmlFile) {

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			
			Document doc = builder.parse(xmlFile);
			doc.getDocumentElement().normalize();

			
			Element root = doc.getDocumentElement();
			this.outputMap = new HashMap<String, String>();
			this.tagValues = new HashMap<String, String>();
			this.columnHeaderMap = new HashMap<String, ArrayList<String>>();
							
			if (this.format.equalsIgnoreCase("-h")){
				// html
				convertToHTML(root, filenameWithoutExtension(xmlFile));
			} else {
				// tab or comma delimited
				this.convertXmlToCsv(root);

				StringBuilder delimitOutput = new StringBuilder();
				Iterator iter = outputMap.entrySet().iterator();
				while (iter.hasNext()) {
					Map.Entry ent = (Map.Entry) iter.next();
					delimitOutput.append("\n" + ent.getKey() + "\n");
					delimitOutput.append(ent.getValue().toString());
//					System.out.print("\n" + ent.getKey() + "\n");
//					System.out.println(ent.getValue().toString());
				
				}
				
				try {

					File file;
					DateFormat dateFormat = new SimpleDateFormat("MMMdd_HHmmss");
					Date date = new Date();
					String strDate = dateFormat.format(date);
					if (this.format.equalsIgnoreCase("-c")) {
						file = new File(this.outname + "_" + strDate + ".csv");
					} else {
						file = new File(this.outname + "_" + strDate + ".txt");
					}

					// if file doesnt exists, then create it
					if (!file.exists()) {
						file.createNewFile();
					}

					FileWriter fw = new FileWriter(file.getAbsoluteFile());
					BufferedWriter bw = new BufferedWriter(fw);
					bw.write(delimitOutput.toString());
					bw.close();

					System.out.println("Done");

				} catch (IOException e) {
					e.printStackTrace();
				}

			}


		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//*************************************************************
	//   XSD
	//*************************************************************
	private ArrayList<String> xsdHeaders = new ArrayList<String>();
	
	public Map<String, String> xsdNodeInfo(Node n){
		Map<String, String> info = new HashMap<String, String>();
		
		//get name
		info.put("element type", n.getNodeName());
		
		//get attributes
		NamedNodeMap atts = n.getAttributes();
		Node att;
		String currNodeName;
		
		for (int i=0; i < atts.getLength(); i++){
			att = atts.item(i);
			currNodeName = att.getNodeName();
			info.put(currNodeName, att.getNodeValue());
			
			if (!xsdHeaders.contains(currNodeName)){
				xsdHeaders.add(currNodeName);
			}
		}
		return info;
	}

	//*************************************************************
	//   HTML
	//*************************************************************
	

	private Map mapTypeToCnsmrTables = new HashMap(); // cnsmr type : cnsmr hashmap for table as string
	private Map<String, ArrayList<String>> mapTypeToValuePositions = new HashMap(); //cnsmr type: list of tag names
	
	public void convertToHTML(Node root, String outName){
				
		ArrayList<Node> tableRoots = getSubNodesWithAttributes(root);
		Node curr; 	
		
		for (int i=0; i < tableRoots.size(); i++){
			curr = tableRoots.get(i);
			if(!(mapTypeToValuePositions.containsKey(cnsmrType(curr)))){
				startNewTable(curr);
			}		
			addNewTableRow(curr);	
		}

		closeTables();		
		
		StringBuilder str = new StringBuilder(); 
		Iterator iter = mapTypeToCnsmrTables.entrySet().iterator();
		while(iter.hasNext()){
			Map.Entry e = (Map.Entry) iter.next();
			str.append("<h1>" + e.getKey() + "</h1>" +e.getValue() +"<br><br>");
		}
		
		try {
			
			DateFormat dateFormat = new SimpleDateFormat("MMMdd_HHmmss");
			Date date = new Date();
			String strDate = dateFormat.format(date);
			File file = new File(outName + "_" + strDate + ".html");

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
	
	public void startNewTable(Node n){
		String type = cnsmrType(n);
		StringBuilder table = new StringBuilder();
	
		ArrayList<String> labels = getCnsmrNodeNames(n);
		
		mapTypeToValuePositions.put(type, labels);
	
		table.append("<table border=1px>");
		table.append("<tr>");
		for (int i=0; i < labels.size(); i++){
			table.append("<th>"+labels.get(i)+"</th>");
		}
		table.append("</tr>");
		mapTypeToCnsmrTables.put(type, table);

	}
	

	public void addNewTableRow(Node n){
		StringBuilder row = new StringBuilder();
		String type = cnsmrType(n);
		String currCol;
		String currValue;
		int insertPos;

		//retrieve the HTML for table you are appending row to
		StringBuilder table = (StringBuilder) mapTypeToCnsmrTables.get(type);
		
		//retrieve label list for input order
		ArrayList<String> columnHeaders = (ArrayList<String>) mapTypeToValuePositions.get(type);

		//get elements inside record
		ArrayList<Node> fields = getElementChildNodes(n);
				
		row.append("<tr>");
		
		Object[] values = new Object[columnHeaders.size()];
		
		
		for (int i = 0; i < fields.size() ;i++){
			
			currCol = fields.get(i).getNodeName();
			currValue = fields.get(i).getTextContent();
			
			//get position
			insertPos = columnHeaders.indexOf(currCol);	
			
			if (insertPos < 0){
				continue;
			} else {
				values[insertPos]="<td>"+currValue+"</td>";
			}
		}
		
		for (int i=0; i<values.length; i++){
			if (values[i]==null){
				values[i]="<td>null</td>";
			}
		}
		
		List<Object> listValues= Arrays.asList(values);
		
		row.append(listValues.toString().replace("[", "").replace("]", "").replace(",", ""));
		row.append("</tr>");
		table.append(row);
		mapTypeToCnsmrTables.put(type, table);

	}
	
	
	//*************************************************************
	//   HELPERS
	//*************************************************************
	

	public ArrayList<String> getCnsmrTextNodeValues(Node n){
		ArrayList<String> textNodeValues = new ArrayList<String>();
		ArrayList<Node> nodes = getElementChildNodes(n);
		Node curr;
		for (int i = 0; i < nodes.size() ;i++){
			curr = nodes.get(i);
			if (curr.getNodeName().equals("nullify_fields")){
				//do something 
			}
			textNodeValues.add(curr.getTextContent());
		}
		return textNodeValues;
	}
	
	
	
	public ArrayList<String> getCnsmrNodeNames(Node n){
		ArrayList<String> nodeNames = new ArrayList<String>();
		ArrayList<Node> nodes = getElementChildNodes(n);
		Node curr;
		for (int i = 0; i < nodes.size() ;i++){
			curr = nodes.get(i);
			if (curr.getNodeName().equals("nullify_fields")){
				nodeNames.addAll(getNullifiedFieldNames(curr));
			} else {
				nodeNames.add(curr.getNodeName());
			}
		}
		return nodeNames;
	}
	
	
	public static ArrayList<Node> getChildrenWithTextContent(Node node) {
		ArrayList<Node> children = getElementChildNodes(node);
		ArrayList<Node> textChildren = new ArrayList<Node>();
		
		for (int i = 0; i < children.size(); i++) {
			if (!children.get(i).getTextContent().isEmpty() && 
				!children.get(i).getNodeName().equalsIgnoreCase("udp_field")) {
				textChildren.add(children.get(i));
			}
		}
		return textChildren;
	}

	// takes nullified element
	public ArrayList<String> getNullifiedFieldNames (Node n){
		
		ArrayList<Node> nullifiedFields = getSubNodesWithTextContent(n);
		ArrayList<String> fieldNames = new ArrayList<String>();
		for (int i = 0; i < nullifiedFields.size(); i++){
			fieldNames.add(nullifiedFields.get(i).getTextContent());
		}
		return fieldNames;
	}
	
	public static ArrayList<Node> getSubNodesWithTextContent(Node node){
		ArrayList<Node> subNodesTextContent = new ArrayList<Node>();
		ArrayList<Node> childrenTextContent = new ArrayList<Node>();
		ArrayList<Node> children = getElementChildNodes(node);
		
		if (!node.hasChildNodes()){
			return subNodesTextContent;
		}
		
		childrenTextContent = getChildrenWithTextContent(node);
		subNodesTextContent.addAll(childrenTextContent);
		if (childrenTextContent.size()>0){
			//for each child with text content, look for children that may have text content
			for (int i=0; i < childrenTextContent.size(); i++){
				subNodesTextContent.addAll(getSubNodesWithTextContent(childrenTextContent.get(i)));
			}
		} else {
			for (int i=0; i < children.size(); i++){
				subNodesTextContent.addAll(getSubNodesWithTextContent(children.get(i)));
			}
		}
		return subNodesTextContent;
	}

	public String cnsmrType(Node n){
		return n.getAttributes().getNamedItem("type").getNodeValue();
	}
	
	
	public static String allAttributesAsString(Node n){
		StringBuilder output = new StringBuilder();
		Node currAtt;
		NamedNodeMap attributes = n.getAttributes();
		for (int i=0; i < attributes.getLength(); i++){
			currAtt = attributes.item(i);
			output.append(currAtt.getNodeName() + "=" + currAtt.getNodeValue() + " ");
		}
		System.out.println(output);
		return output.toString();
	}
	
	
	public static ArrayList<Node> getSubNodesWithAttributes(Node node){
		ArrayList<Node> subNodesWithAtt = new ArrayList<Node>();
		ArrayList<Node> childrenWithAtt = new ArrayList<Node>();
		ArrayList<Node> children = getElementChildNodes(node);
		
		if (!node.hasChildNodes()){
			return subNodesWithAtt;
		}
		
		childrenWithAtt = getChildrenWithAttributes(node);
		subNodesWithAtt.addAll(childrenWithAtt);
		if (childrenWithAtt.size()>0){
			//for each child with att, look for children that may have att
			for (int i=0; i < childrenWithAtt.size(); i++){
				subNodesWithAtt.addAll(getSubNodesWithAttributes(childrenWithAtt.get(i)));
			}
		} else {
			for (int i=0; i < children.size(); i++){
				subNodesWithAtt.addAll(getSubNodesWithAttributes(children.get(i)));
			}
		}
		
		return subNodesWithAtt;
	}
	
	
	//Excludes UDP elements
	public static ArrayList<Node> getChildrenWithAttributes(Node node) {
		NodeList children = node.getChildNodes();
		ArrayList<Node> attChildren = new ArrayList<Node>();
		
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i).hasAttributes() && !children.item(i).getNodeName().equalsIgnoreCase("udp_field")) {
				attChildren.add(children.item(i));
			}
		}
		return attChildren;
	}
	
	
	public static ArrayList<Node> getElementChildNodes(Node node) {
		//return ArrayList<Node> children that are elements

		NodeList children = node.getChildNodes();
		ArrayList<Node> elementChildren = new ArrayList<Node>();
		
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
				elementChildren.add(children.item(i));
			}
		}
		return elementChildren;
	}

	public  ArrayList<String> getAllColumnHeaders(Node node) {
		// Returns all of a node's attribute tags, child tags, and nullified tags
		ArrayList<String> tags = new ArrayList<String>();
		NamedNodeMap nodeAttributes = node.getAttributes();
		
		// get attributes
		for (int i = 0; i < nodeAttributes.getLength(); i++) {
			tags.add(nodeAttributes.item(i).getNodeName());
		}
		
		// get children tags (including nullified
		tags.addAll(this.getCnsmrNodeNames(node));

		return tags;
	}

	public String filenameWithoutExtension(File f){
		//given file, returns filename as string without extension
		String name = f.getName();
		int pos = name.lastIndexOf(".");
		return name.substring(0, pos);
	}
	
	
	
	public void convertXmlToCsv(Node root) {
		
		// root has no children
		if (root.getNodeName().compareToIgnoreCase("nullify_fields") == 0) {//!this.hasChildNodes(root)) {
			// do nothing
		} else { // root has children
			ArrayList<Node> children = getElementChildNodes(root);
			
			// iterate children, if child has no children then pull it's tag and value and add to arrays
			for (int i = 0; i < children.size(); i++) {
				Node child = children.get(i);
				
				if (!this.hasChildNodes(child)) {
					this.tagValues.put(child.getNodeName(), child.getTextContent());
					
				}
			}
			// If we haven't seen root type previously, create a key for it and insert the child tags as its values (first row)
			if (!this.outputMap.containsKey(root.getNodeName())) {
					this.columnHeaderArray = this.getAllColumnHeaders(root);
					this.columnHeaderMap.put(root.getNodeName(), this.columnHeaderArray);
					this.outputMap.put(root.getNodeName(), "\n" + this.columnHeaderArray.toString().replace("[", "").replace("]", "").replace(",", this.delimitChar) + "\n");
			}
			
			// Double check we have the right headers
			this.columnHeaderArray = this.columnHeaderMap.get(root.getNodeName());
			String[] columnValues = new String[this.columnHeaderArray.size()];
			
			// Align values with headers
			for (int i = 0; i < this.columnHeaderArray.size(); i++) {
				String columnHeader = this.columnHeaderArray.get(i);
				if (this.tagValues.containsKey(columnHeader)) {
					columnValues[i] = this.tagValues.get(columnHeader);
				} else {
					columnValues[i] = "";
				}
			}
	
			// Attributes
			NamedNodeMap attrs = root.getAttributes();
			
			for (int k = 0; k < attrs.getLength(); k++) {
				Node attribute = attrs.item(k);
				int indexOfAttr = this.columnHeaderArray.indexOf(attribute.getNodeName());
				columnValues[indexOfAttr] = attribute.getTextContent();
			}
			
			// Values of children tags (and fill nullified tags with "")
			if (columnValues.length > 0) {
				this.outputMap.put(root.getNodeName(), 
						this.outputMap.get(root.getNodeName()) + Arrays.toString(columnValues).replace("[", "").replace("]", "").replace(",", this.delimitChar) + "\n");
			} else {
				this.outputMap.put(root.getNodeName(), this.outputMap.get(root.getNodeName()) + "" + "\n");
			}

			// reset values/tags
			this.tagValues.clear();
			
			// iterate children for recursion
			for (int i = 0; i < children.size(); i++) {
				Node child = children.get(i);

				// recurse over child
				if (this.hasChildNodes(child)) {
					this.convertXmlToCsv(child);
				}
			}
		}
	}
	

	public boolean hasChildNodes(Node node) {
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
				return true;
			}
		}
		return false;
	}
	
	public void closeTables(){
		StringBuilder value;
		Iterator iter = mapTypeToCnsmrTables.entrySet().iterator();
		while(iter.hasNext()){
			Map.Entry e = (Map.Entry) iter.next();
			value = (StringBuilder) e.getValue();
			e.setValue(value + "</table>");
		}
	}

	
//	public static void allNodes(Node curr2){
//		
//		NodeList children = curr2.getChildNodes();
//		Node curr; 
//		
//		for (int i =0; i<children.getLength(); i++) {
//			if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
//				curr = children.item(i);
//				System.out.println(String.format("%s: %-50s %s %s" , "child", curr.getNodeName(), "parent: ", curr.getParentNode().getNodeName() )) ;
//				if(curr.hasChildNodes()){
//					allNodes(curr);
//				}
//			}
//		}	
//	}

	
	
	
	
	
	
	// input: Main.class schema.xsd file1.xml file2.xml...
	public static void main(String[] args) {
		Main m = new Main();
		m.xmlFiles = new ArrayList<File>();
		
		if (args.length < 2) {
			System.out.println("Too few arguments. Must be called in the following format Main.class *.xsd *.xml...");
			return;
		}
		
		
		
		int xsdIndex;
		if (args[0].startsWith("-")){
			m.format = args[0];
			xsdIndex = 2;
		} else {
			xsdIndex = 1;
			String xsdFileName = args[0];
			if (xsdFileName.matches("(?i).*.xsd")) {
				//if (m.validateInput(xsdFileName,  "xsd")) {
					//System.out.println("xsd found");
				//} else {
				//	System.out.println("Error finding or processing XSD.");
				//	return;
				//}
			} else {
				System.out.println(xsdFileName + " is invalid. First argument must be in the format *.xsd.");
				return;
			}
		}
		

		String filename;
		for (int i = xsdIndex; i < args.length; i++) {
			filename = args[i];
			if (filename.matches("(?i).*.xml")) {
				m.validateInput(filename, "xml");
			} else if (filename.matches("(?i).*.xsd")) {
				System.out.println("Error with " + filename + ": Only 1 xsd can be passed and must be the first argument");
			} else {
				System.out.println(filename + " has an invalid file name. Files must be in the format *.xml.");
			}	
		}
		
		//System.out.println("xml files: " + m.xmlFiles.size());
		//System.out.println("xsd scehma: " + m.xsdSchemaFile.getName());
		

		XSDParser xsdParser = new XSDParser();
		if (m.format.equals("-c")){
			m.delimitChar = ",";
		} else {
			m.delimitChar = "\t";
		}
			
		
		for (File f: m.xmlFiles) {
			//boolean result = m.validateXMLAgainstXSD(f, m.xsdSchemaFile);
			
			//TODO: parse validated XML files
			m.parseXML(f);

			//TODO: Output formatted CVS
			
		}
		

		return;
	}

}
