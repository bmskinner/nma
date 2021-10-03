package com.bmskinner.nuclear_morphology.io.xml;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;

import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;

/**
 * Write a ruleset collection to file
 * @author ben
 * @since 1.18.3
 *
 */
public class RulesetCollectionXMLWriter extends XMLWriter {
	
	private static final Logger LOGGER = Logger.getLogger(RulesetCollectionXMLWriter.class.getName());
	
	public void write(@NonNull RuleSetCollection rs, @NonNull File outFile) {
		
		Document doc = new Document(rs.toXmlElement());
		try {
			writeXML(doc, outFile);
		} catch (IOException e) {
			 LOGGER.warning("Cannot export options file");
		}
	}

}
