package project_tortoise;

import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

public class Main {

	static boolean validateAgainstXSD(InputStream xml, InputStream xsd) {
		try {
			SchemaFactory factory =
					SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			
					Schema schema = factory.newSchema(new StreamSource(xsd));
					Validator validator = schema.newValidator();
					validator.validate(new StreamSource(xml));;
					return true;
		} catch (Exception ex) {
			return false;
		}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		for (String s: args) {
			System.out.println(s);
		}
		System.out.println("Hello World!");
	}

}
