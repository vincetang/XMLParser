package project_tortoise;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

public class Main {

	private ArrayList<File> xmlFiles;
	private ArrayList<File> xsdFiles;
	
	public boolean validateInput(String filename, String type) {
		File f = new File(filename);
			
		if (f.exists() && f.isFile()) {
			if (type == "xml")
				this.xmlFiles.add(f);
			else
				this.xsdFiles.add(f);
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
			System.out.println(xmlFile.getName() + " is NOT valid when checked against " + xsdFile.getName());
			System.out.println("Reason: " + e.getLocalizedMessage());
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			System.out.println("IOException when validating " + xmlFile.getName());
			System.out.println("Reason: " + e.getLocalizedMessage());
			e.printStackTrace();
			return false;
			
		}
	}
	
	public static void parse(){
		
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Main m = new Main();
		m.xmlFiles = new ArrayList<File>();
		m.xsdFiles = new ArrayList<File>();
		
		for (String filename: args) {
			System.out.println("Read in file: " + filename);
			if (filename.matches("(?i).*.xml")) {
				m.validateInput(filename, "xml");
			} else if (filename.matches("(?i).*.xsd")) {
				m.validateInput(filename,  "xsd");
			} else {
				System.out.println(filename + " has an invalid file name. Files must be in the format *.xml or *.xsd.");
				
			}	
		}
		System.out.println("xml files: " + m.xmlFiles.size());
		System.out.println("xsd files: " + m.xsdFiles.size());
	}

}
