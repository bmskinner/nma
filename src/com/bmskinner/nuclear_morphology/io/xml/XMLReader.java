package com.bmskinner.nuclear_morphology.io.xml;

import java.io.File;
import java.io.IOException;

import javax.xml.XMLConstants;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.datasets.DatasetCreator;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.DefaultAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.DefaultOptions;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;

/**
 * Base class for XML readers
 * @author bms41
 * @since 1.14.0
 */
public abstract class XMLReader {
	
	private XMLReader() {}
	
	/**
	 * Read the given file as an XML document
	 * @param file the file to read
	 * @return the XML representation of the file content
	 * @throws XMLReadingException if the document could not be read or was not XML
	 */
	protected static Document readDocument(File file) throws XMLReadingException {
		SAXBuilder saxBuilder = new SAXBuilder();
		saxBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		saxBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

		try {
			return saxBuilder.build(file);
		} catch(JDOMException | IOException e) {
			throw new XMLReadingException(String.format("Unable to read file %s as XML: %s", file.getAbsolutePath(), e.getMessage()), e);
		}
	}
	
	
	public static IAnalysisDataset readDataset(File f) throws XMLReadingException, ComponentCreationException {
		Document d = readDocument(f);
		return DatasetCreator.createRoot(d.getRootElement());
	}
	
	public static RuleSetCollection readRulesetCollection(File f) throws XMLReadingException {
		Document d = readDocument(f);
		return new RuleSetCollection(d.getRootElement());
	}
	
	public static HashOptions readOptions(File f) throws XMLReadingException {
		Document d = readDocument(f);
		return new DefaultOptions(d.getRootElement());
	}
	
	public static IAnalysisOptions readAnalysisOptions(File f) throws XMLReadingException {
		Document d = readDocument(f);
		return new DefaultAnalysisOptions(d.getRootElement());
	}
	
	/**
	 * Thrown when an xml file cannot be read
	 * @author bms41
	 * @since 1.14.0
	 *
	 */
	public static class XMLReadingException extends Exception {
		private static final long serialVersionUID = 1L;

	    public XMLReadingException() { super(); }
	    
	    public XMLReadingException(String message) {super(message); }

	    public XMLReadingException(String message, Throwable cause) { super(message, cause); }

	    public XMLReadingException(Throwable cause) { super(cause); }
	}	
}
