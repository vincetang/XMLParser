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
	private File xsdSchemaFile;
	
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
			boolean result = m.validateXMLAgainstXSD(f, m.xsdSchemaFile);
		}
		return;
	}

}
