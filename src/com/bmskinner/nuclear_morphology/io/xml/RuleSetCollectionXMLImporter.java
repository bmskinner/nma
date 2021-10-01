package com.bmskinner.nuclear_morphology.io.xml;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.io.Io.Importer;
import com.bmskinner.nuclear_morphology.io.xml.XMLReader.XMLReadingException;

/**
 * Import XML files containing ruleset descriptions
 * @author ben
 * @since 1.18.3
 */
public class RuleSetCollectionXMLImporter implements Importer {
	
	private static final Logger LOGGER = Logger.getLogger(RuleSetCollectionXMLImporter.class.getName());
	
	private final File file;
	
	public RuleSetCollectionXMLImporter(@NonNull final File f) {
		file = f;
	}

	public RuleSetCollection importRuleset() throws XMLReadingException {
		try {
	         SAXBuilder saxBuilder = new SAXBuilder();
	         Document document = saxBuilder.build(file);
	         
	         RuleSetCollectionXMLReader reader = new RuleSetCollectionXMLReader(document.getRootElement());
	         return reader.read();
	         
		} catch(IOException | JDOMException e) {
			LOGGER.fine("Error reading "+file.getAbsolutePath()+" as a ruleset file");
			throw new XMLReadingException("Unable to read file as ruleset", e);
		}
	}
	
}
