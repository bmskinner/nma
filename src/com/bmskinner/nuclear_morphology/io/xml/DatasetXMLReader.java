package com.bmskinner.nuclear_morphology.io.xml;

import java.io.File;
import java.io.IOException;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Read serialised XML dataset files
 * @author bms41
 * @since 1.14.0
 *
 */
public class DatasetXMLReader extends XMLReader<IAnalysisDataset> {
	
	/**
	 * Create with a file to be read
	 * @param f
	 */
	public DatasetXMLReader(@NonNull final File f) {
		super(f);
	}
	
	@Override
	public IAnalysisDataset read() {
		
		try {
		
			SAXBuilder saxBuilder = new SAXBuilder();
			Document document = saxBuilder.build(file);
			XMLOutputter xmlOutput = new XMLOutputter();
			xmlOutput.setFormat(Format.getPrettyFormat());
			xmlOutput.output(document, System.out); 
			
		} catch (JDOMException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
}
