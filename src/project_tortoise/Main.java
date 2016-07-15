package project_tortoise;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
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
	
	private String outName;
	private String format = "-t";
	private String delimitChar;
	private ArrayList<File> xmlFiles;

	private LinkedHashMap<String, String> outputMap;
	private HashMap<String, String> tagValues;
	private ArrayList<String> columnHeaderArray;
	private HashMap<String, ArrayList<String>> columnHeaderMap;

	private XSDParser xsdParser;
	
	public boolean validateInput(String filename, String type) {
		
		File f = new File(filename);
			
		if (f.exists() && f.isFile()) {
			if (type == "xml")
				this.xmlFiles.add(f);

			return true;
		} else {
			System.out.println(filename + " could not be found... skipping");
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
			this.outputMap = new LinkedHashMap<String, String>();
			this.tagValues = new HashMap<String, String>();
			this.columnHeaderMap = new HashMap<String, ArrayList<String>>();
			this.outName = filenameWithoutExtension(xmlFile);

			if (this.format.equalsIgnoreCase("-h")){
				// html
				convertToHTML(root, this.outName);
			} else {
				// tab or comma delimited
				this.convertXmlToCsv(root);

				StringBuilder delimitOutput = new StringBuilder();
				Iterator iter = outputMap.entrySet().iterator();
				String value ="";
				while (iter.hasNext()) {
					Map.Entry ent = (Map.Entry) iter.next();
					
					value = ent.getValue().toString();
					
					// only write header if it has values
					// items without values contain [d,,,,]
					String strPattern;
					if (this.format.equalsIgnoreCase("-t")) {
						strPattern = ".*\nd\t.+";
					} else {
						strPattern = ".*\nd, .+";
					}

					Pattern pattern = Pattern.compile(strPattern, Pattern.DOTALL);
					Matcher matcher = pattern.matcher(value);
					
					if (matcher.matches()) {
						delimitOutput.append("\n\n" + "h\t" + ent.getKey() + "\n");
						delimitOutput.append(ent.getValue().toString());
					}
				
				}
				
				try {

					File file;
					DateFormat dateFormat = new SimpleDateFormat("MMMdd_HHmmss");
					Date date = new Date();
					String strDate = dateFormat.format(date);
					StringBuilder outFileName = new StringBuilder(this.outName + "_" + strDate);
					if (this.format.equalsIgnoreCase("-c")) {
						outFileName.append(".csv");
						file = new File(outFileName.toString());
					} else {
						outFileName.append(".txt");
						file = new File(outFileName.toString());
					}

					// if file doesnt exists, then create it
					if (!file.exists()) {
						file.createNewFile();
					}

					FileWriter fw = new FileWriter(file.getAbsoluteFile());
					BufferedWriter bw = new BufferedWriter(fw);
					bw.write(delimitOutput.toString());
					bw.close();

					System.out.println("Done!\nOutput saved to " + outFileName);

				} catch (IOException e) {
					e.printStackTrace();
				}

			}


		} catch (Exception e) {
			e.printStackTrace();
		}
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

			} else if (curr.getNodeName().equals("udp_fields")) {
//				nodeNames.addAll(getUdpNameField(curr));
				HashMap<String, String> udpMap = (HashMap<String, String>) getUdpMap(curr);
				nodeNames.addAll(0, udpMap.keySet());	
				this.tagValues.putAll(udpMap);
			} else{
				//nodeNames.add(curr.getNodeName());
				nodeNames.add(0, curr.getNodeName());
			}
		}
		return nodeNames;
	}
	
	public Map<String, String> getUdpMap(Node udp_fields){
		Map<String, String> udp_fields_map = new HashMap<String,String>();
		
		/**
		 * e.g.
		 *<udp_fields>
          	<udp_field xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="decimal_type" name="udefreceivercommissionrate">
            	<value>12.0000</value>
          	</udp_field>
          
          maps as {udefreceivercommissionrate: 12.0000}
		 **/
		
		ArrayList<Node> udps = getSubNodesWithAttributes(udp_fields);
		Node curr;
		String key; 
		String value;
		for(int i=0; i<udps.size(); i++){
			curr = udps.get(i);
			//get "name" attribute 
			key = getAttValue(curr, "name");
			
			// get value
			value = getUdpFieldValue(curr);
			
			udp_fields_map.put(key, value);
		}
		
//		System.out.println(udp_fields_map.toString());
		return udp_fields_map;
	}
	
	
	public static ArrayList<Node> getChildrenWithTextContent(Node node) {
		ArrayList<Node> children = getElementChildNodes(node);
		ArrayList<Node> textChildren = new ArrayList<Node>();
		
		for (int i = 0; i < children.size(); i++) {
			if (!children.get(i).getTextContent().isEmpty() 
//					&& !children.get(i).getNodeName().equalsIgnoreCase("udp_field")
				) {
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
	
	
	public String getUdpFieldValue (Node n){
		
		ArrayList<Node> valueNodes = getSubNodesWithTextContent(n);
		if (valueNodes.size() > 0){
			return valueNodes.get(0).getTextContent();
		} else {
			return "";
		}
	}
	
	public ArrayList<String> getUdpNameField (Node n) {
		ArrayList<Node> udpFields = getAllChildrenWithAttributes(n);
		ArrayList<String> fieldNames = new ArrayList<String>();
	
		for (int i = 0; i < udpFields.size(); i ++ ) {
			NamedNodeMap attributes = udpFields.get(i).getAttributes();
			String att = attributes.getNamedItem("name").toString();
			fieldNames.add(att);
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
	
	//node, attribute name -> returns attribute value
	public String getAttValue(Node n, String attName){
		return n.getAttributes().getNamedItem(attName).getNodeValue();
	}
	
	
	public static String allAttributesAsString(Node n){
		StringBuilder output = new StringBuilder();
		Node currAtt;
		NamedNodeMap attributes = n.getAttributes();
		for (int i=0; i < attributes.getLength(); i++){
			currAtt = attributes.item(i);
			output.append(currAtt.getNodeName() + "=" + currAtt.getNodeValue() + " ");
		}
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
			if (children.item(i).hasAttributes() 
//					&& !children.item(i).getNodeName().equalsIgnoreCase("udp_field")
					) {
				attChildren.add(children.item(i));
			}
		}
		return attChildren;
	}
	
	public static ArrayList<Node> getAllChildrenWithAttributes(Node node) {
		NodeList children = node.getChildNodes();
		ArrayList<Node> attChildren = new ArrayList<Node>();
		
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i).hasAttributes()) {
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
		
		// get children tags (including nullified and UDP fields)
		tags.addAll(this.getCnsmrNodeNames(node));

		return tags;
	}

	public static String filenameWithoutExtension(File f){
		//given file, returns filename as string without extension
		String name = f.getName();
		int pos = name.lastIndexOf(".");
		return name.substring(0, pos);
	}
	
	public int countValues(String[] columnValues) {
		int count = 0;
		for (int i = 0; i < columnValues.length; i++) {
			if ((columnValues[i] != null) && (columnValues[i].compareToIgnoreCase("") != 0)) {
				count++;
			}
		}
		return count;
	}
	
	public void convertXmlToCsv(Node root) {
		String rootName;
		if (root.getNodeName().compareToIgnoreCase("cnsmr_accnt_udp") == 0) { // generate a unique key for each consmr_accnt_udp
			rootName = root.getNodeName() + " " + root.getAttributes().getNamedItem("seq_no").getNodeValue().toString();
			
		} else {
			rootName = root.getNodeName();
		}
		
		if (root.getNodeName().compareToIgnoreCase("nullify_fields") == 0 ||
				root.getNodeName().compareToIgnoreCase("udp_fields") == 0) {
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
			if (!this.outputMap.containsKey(root.getNodeName()) || root.getNodeName().compareToIgnoreCase("cnsmr_accnt_udp") == 0) {
					this.columnHeaderArray = new ArrayList<String>();
					this.columnHeaderArray.add("h");
					this.columnHeaderArray.addAll(this.getAllColumnHeaders(root));
					

					this.columnHeaderMap.put(rootName, this.columnHeaderArray);
					StringBuilder headerString = new StringBuilder();
					for (String s : this.columnHeaderArray) {
						headerString.append(s);
						headerString.append(this.delimitChar);
					}
					this.outputMap.put(rootName, "\n" + headerString + "\n");
//					this.outputMap.put(rootName, "\n" + this.columnHeaderArray.toString().replace("[", "").replace("]", "").replace(",", this.delimitChar) + "\n");
					
			}
			
			// Double check we have the right headers
			this.columnHeaderArray = this.columnHeaderMap.get(rootName);
			String[] columnValues = new String[this.columnHeaderArray.size()];
			
			// Align values with headers
			for (int i = 0; i < this.columnHeaderArray.size(); i++) {
				String columnHeader = this.columnHeaderArray.get(i);
				if (this.tagValues.containsKey(columnHeader)) {
					columnValues[i] = this.tagValues.get(columnHeader);
				} else if (columnHeader.compareToIgnoreCase("h") == 0) {
					columnValues[i] = "d";
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
			if (this.countValues(columnValues) > 1) { //countValues.size() > 0
				//System.out.println(Arrays.toString(columnValues));
				StringBuilder valueString = new StringBuilder();
				for (String s : columnValues) {
					valueString.append(s);
					valueString.append(this.delimitChar);
				}
				this.outputMap.put(rootName, this.outputMap.get(rootName) + valueString + "\n");
//				this.outputMap.put(rootName, 
//						this.outputMap.get(rootName) + Arrays.toString(columnValues).replace("[", "").replace("]", "").replace(",", this.delimitChar) + "\n");
			} else {
				this.outputMap.put(rootName, this.outputMap.get(rootName) + "" + "\n");
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
	
	public void parseXsdTable(String path) {
		File currentDir = new File(path);
	
		this.addDirectoryContents(currentDir);
		xsdParser.writeOutput();

	}
	
	public void addDirectoryContents(File dir) {
		try {
			File[] files = dir.listFiles();
			for (File file : files) {
				if (file.isDirectory()) { // is a sub-directory
					this.addDirectoryContents(file);
				} else { // is a file
					Pattern pattern = Pattern.compile("(?i).*.xsd", Pattern.DOTALL);
					Matcher matcher = pattern.matcher(file.getName());
					if (matcher.matches()) {
						xsdParser.parseFile(file);
					}
				}
			} 
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// input: Main.class schema.xsd file1.xml file2.xml...
	public static void main(String[] args) {
		Main m = new Main();
		m.xmlFiles = new ArrayList<File>();
		
		String help =
				"Parser Help\n=========\n"
				+ "Commands:\n"
				+ "XMLParser parseXSDTable [filepath]\n"
				+ "\tBuilds a table of all xsd elements in the filepath specified (including subdirectories) or the current directory if a filepath is not specified.\n"
				+ "XMLParser [-h,-c] file1.xml file2.xml...\n"
				+ "\tno argument: generate tab-delimited representation of files\n"
				+ "\th: generate HTML representation of files\n"
				+ "\tc: generate comma-separated representation of files\n"
				+ "XMLParser -v schema.xsd file1.xml file2.xml...\n"
				+ "\tv: validates each xml file against the xsd schema\n";

		if (args.length == 0 || args[0].compareToIgnoreCase("help") == 0) {
			System.out.println(help);
			return;
		}

		int fileIndex = 0;
		String arg = args[0];
		switch (arg.toLowerCase()) {
		case "parsexsdtable":
			m.xsdParser = new XSDParser();

			String path;
			
			if (args.length == 2) {
				path = args[1];
				System.out.println("Parsing xsd files in " + path + " and subdirectories");
			} else {
				path = ".";
				System.out.println("Parsing xsd files in current directory and subdirectories");
				
			}
			m.parseXsdTable(path);
			return;
		case "-v":
			if (args.length < 3) {
				System.out.println("Invalid arguments");
				return;
			}
			
			String xsdFileName = args[1];
			if (xsdFileName.matches("(?i).*.xsd")) {
				if (!m.validateInput(xsdFileName, "xsd")) {
					System.out.println("Invalid xsd file: " + xsdFileName);
					return;
				}
				File xsdFile = new File(xsdFileName);
				
				String filename;
				for (int i = 2; i < args.length; i++) {
					filename = args[i];
					if (filename.matches("(?i).*.xml")) {
						m.validateInput(filename, "xml");
					} else {
						System.out.println(filename + " has an invalid file name. Files must be in the format *.xml");
						return;
					}	
				}
				XSDValidator validator = new XSDValidator();
				for (File f: m.xmlFiles) {
					if (!validator.validateXMLAgainstXSD(f, xsdFile)) {
						System.out.println("Validation failed for " + f.getName());
						return;
					}
				}
				System.out.println("All XML files have been validated against " + xsdFileName + " sucecssfully");	
			} else {
				System.out.println("Invalid xsd file provided: " + xsdFileName);
			}
			return;
		case "-h":
			fileIndex = 1;
			m.format="-h";
			break;
		case "-c":
			fileIndex = 1;
			m.format="-c";
			m.delimitChar = ", ";
			break;
		default:
			m.format="-t";
			m.delimitChar = "\t";
			break;
		}
		
		String filename;
		for (int i = fileIndex; i < args.length; i++) {
			filename = args[i];
			if (filename.matches("(?i).*.xml")) {
				m.validateInput(filename, "xml");
			} else {
				System.out.println(filename + " has an invalid file name. Files must be in the format *.xml");
			}	
		}
		
		for (File f: m.xmlFiles) {
			System.out.print("Parsing " + f.getName() + "... ");
			m.parseXML(f);
		}
		
		return;
	}

}
