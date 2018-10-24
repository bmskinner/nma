package com.bmskinner.nuclear_morphology.io.xml;

import java.io.File;
import java.io.IOException;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Read serialised XML dataset files
 * @author bms41
 * @since 1.14.0
 *
 */
public class DatasetXMLReader implements Loggable {
	
	private final File file;
	
	/**
	 * Create with a file to be read
	 * @param f
	 */
	public DatasetXMLReader(@NonNull final File f) {
		file=f;
	}
	
	/**
	 * Read the file and build a dataset
	 * @return
	 */
	public IAnalysisDataset read() {
		
		try {
		
			SAXBuilder saxBuilder = new SAXBuilder();
			Document document = saxBuilder.build(file);
			
			
		} catch (JDOMException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
}
