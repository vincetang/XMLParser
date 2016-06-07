package project_tortoise;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

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

	private ArrayList<File> xmlFiles;
	private File xsdSchemaFile;
	
	private ArrayList<Node> tagNodes;
	private ArrayList<String> xmlValues;
	private String previousTags;
	
	private StringBuilder output;
	
	private ArrayList<String> labels = new ArrayList<String>();
	
	
	public boolean validateInput(String filename, String type) {
		
		File f = new File(filename);
			
		if (f.exists() && f.isFile()) {
			if (type == "xml")
				this.xmlFiles.add(f);
			else
				this.xsdSchemaFile = f;

			System.out.println("Read in file: " + filename);
			return true;
		} else {
			System.out.println(filename + " not found. Omitting file.");
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
	
	public void convertXmlToHtml(Node root) {
		/*
		if have children nodes (elements)
			write <table><tr>
			
			for each child {
				write <th>child.getNodeName</th>
			}
			write </tr>
			write <tr>
			for each child {
				if child has no children
					write <td>value</td>
				else:
					write <table><tr> convertXmlToHtml(child) </tr></table>
			write </tr>
			<rite </table>
		else (no children)
			return <td>value</td>
		*/
		
		if (!this.hasChildNodes(root)) {
			this.output.append("<td>" + root.getTextContent() + "</td>");
		} else {
			this.output.append("<table><tr>");
			NodeList children = root.getChildNodes();
			if (children.toString().compareToIgnoreCase(this.previousTags) != 0) {
				
				for (int i = 0; i < children.getLength(); i++) {
					if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
						this.output.append("<th>" + children.item(i).getNodeName() + "</th>");
					}
				}
			} else {
				//this.output.append("<tr>");
			}
			
			this.output.append("</tr><tr>");
			
			for (int i = 0; i <children.getLength(); i++) {
				if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
					if (this.hasChildNodes(children.item(i))) {
						//this.output.append("<td><table><tr>");
						//this.output.append("<table>");
						this.output.append("<td>");
						this.convertXmlToHtml(children.item(i));
						this.output.append("</td>");
						//this.output.append("</td></tr></table><br/>");
						//this.output.append("</table>");
						
					} else {
						this.output.append("<td>" + children.item(i).getTextContent() + "</td>");
						
					}
				}
			}
			this.output.append("</tr></table><br/>");
		}
	}
	
	public void parseXML(File xmlFile) {

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			
			Document doc = builder.parse(xmlFile);
			doc.getDocumentElement().normalize();

			this.output = new StringBuilder();
			
			Element root = doc.getDocumentElement();
			this.tagNodes = new ArrayList<Node>();
			this.xmlValues = new ArrayList<String>();
			this.previousTags = "";

			
			//this.convertXmlToCsv(root);
			//System.out.println("Root element: " + root.getNodeName());
			this.convertXmlToHtml(root);
			//allNodes(root);
			
			//NodeList nList = root.getFirstChild();
			

			this.convertXmlToCsv(root);
			System.out.println("Root element: " + root.getNodeName());
			
			getSubNodesWithAttributes(root);
			
//			System.out.println(labels);
//
//			System.out.println(this.output.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	public void makeTable(Node root){
		
		//look for first text node
		//ArrayList<Node> elementChildren = getElementChildNodes(root);
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
		
		for (int i = 0; i < subNodesWithAtt.size(); i ++){
			System.out.println(subNodesWithAtt.get(i).getNodeName());
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
	
	//return NodeList of children that are elements
	public static ArrayList<Node> getElementChildNodes(Node node) {
		NodeList children = node.getChildNodes();
		ArrayList<Node> elementChildren = new ArrayList<Node>();
		
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
				elementChildren.add(children.item(i));
			}
		}
		return elementChildren;
	}
	
	public void getLabels(Node root){
		//if has an element as a child, it is a label
		//if has text as child, it is value
	}
	
	
	
	
	public void convertXmlToCsv(Node root) {
		if (!(this.hasChildNodes(root))) {
			this.tagNodes.add(root);
//			this.xmlValues.add(root.getTextContent());
			this.output.append(root.getTextContent());
//			System.out.println(root.getNodeName() + ":" + root.getTextContent());
		} else {
			this.output.append("<table>");
			NodeList child = root.getChildNodes();

			for (int i=0; i<child.getLength(); i++) {
				Node iNode = child.item(i);
				if (iNode.getNodeType() == Node.ELEMENT_NODE) { // Gets rid of nodes created by whitespace
					
					this.convertXmlToCsv(iNode);
				}
			}
			
			if (this.previousTags.compareToIgnoreCase(this.tagNodes.toString()) != 0) {
				//System.out.println(root.getNodeName());

				this.output.append(root.getNodeName());
				//System.out.println("Outputting pairs - tags:" + this.tagNodes.size() + " values: " + this.xmlValues.size());
				for (int i = 0; i < this.tagNodes.size(); i++) {
					//System.out.print(this.tagNodes.get(i).getNodeName());
					this.output.append(this.tagNodes.get(i).getNodeName());
//					if (i < this.tagNodes.size()-1) {
//						//System.out.print(", ");
//						this.output.append(", ");
//					}
				}
				this.previousTags = this.tagNodes.toString();
				//System.out.print('\n');
				this.output.append("\n");
			}
			
			for (int i = 0; i < this.xmlValues.size(); i++) {
//				System.out.print(this.xmlValues.get(i));
				this.output.append(this.xmlValues.get(i));
//				if (i < this.xmlValues.size()-1) {
////					System.out.print(", ");
//					this.output.append(", ");
//				}
			}
//			System.out.print('\n');
			this.tagNodes.clear();
			this.xmlValues.clear();
			this.output.append("</table>");
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
	
	public static void allNodes(Node curr2){
		
		NodeList children = curr2.getChildNodes();
		Node curr; 
		
		for (int i =0; i<children.getLength(); i++) {
			if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
				curr = children.item(i);
				System.out.println(String.format("%s: %-50s %s %s" , "child", curr.getNodeName(), "parent: ", curr.getParentNode().getNodeName() )) ;
				if(curr.hasChildNodes()){
					allNodes(curr);
				}
			}
		}
		
	}

	public static void parse(){
		
		
	}
	
	// input: Main.class schema.xsd file1.xml file2.xml...
	public static void main(String[] args) {
		Main m = new Main();
		m.xmlFiles = new ArrayList<File>();
		
		if (args.length < 2) {
			System.out.println("Too few arguments. Must be called in the following format Main.class *.xsd *.xml...");
			return;
		}
		
		String xsdFileName = args[0];
		if (xsdFileName.matches("(?i).*.xsd")) {
			if (m.validateInput(xsdFileName,  "xsd")) {
				System.out.println("xsd found");
			} else {
				return;
			}
		} else {
			System.out.println(xsdFileName + " is invalid. First argument must be in the format *.xsd.");
			return;
		}
		
		String filename;
		for (int i = 1; i < args.length; i++) {
			filename = args[i];
			if (filename.matches("(?i).*.xml")) {
				m.validateInput(filename, "xml");
			} else if (filename.matches("(?i).*.xsd")) {
				System.out.println("Error with " + filename + ": Only 1 xsd can be passed and must be the first argument");
			} else {
				System.out.println(filename + " has an invalid file name. Files must be in the format *.xml.");
			}	
		}
		
		System.out.println("xml files: " + m.xmlFiles.size());
		System.out.println("xsd scehma: " + m.xsdSchemaFile.getName());
		
		for (File f: m.xmlFiles) {
			//boolean result = m.validateXMLAgainstXSD(f, m.xsdSchemaFile);
			
			//TODO: parse validated XML files
			m.parseXML(f);

			//TODO: Output formatted CVS
			
		}
		return;
	}

}
