package com.bmskinner.nuclear_morphology.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Base class for XML writers
 * @author ben
 * @since 1.14.0
 *
 */
public abstract class XMLWriter implements Loggable {
	
	/**
	 * Write the given XML document to a file
	 * @param doc the xml document
	 * @param outputFile the file to write to
	 * @throws IOException if the write fails
	 */
	public static void writeXML(@NonNull Document doc, @NonNull File outputFile) throws IOException {
		if(outputFile.isDirectory())
			throw new IllegalArgumentException(String.format("File %s is a directory", outputFile.getName()));
		if(outputFile.getParentFile()==null)
			throw new IllegalArgumentException(String.format("Parent directory is null", outputFile.getAbsolutePath()));
		
		if(!outputFile.getParentFile().canWrite())
			throw new IllegalArgumentException(String.format("Parent directory %s is not writable", outputFile.getParentFile().getName()));
			
		try(OutputStream os = new FileOutputStream(outputFile)){
			XMLOutputter xmlOutput = new XMLOutputter();
			xmlOutput.setFormat(Format.getPrettyFormat());
			xmlOutput.output(doc, System.out); 
			xmlOutput.output(doc, os);
		} catch (IOException e) {
//			warn(String.format("Unable to write to file %s: %s", outputFile.getAbsolutePath(), e.getMessage()));
			e.printStackTrace();
		}
	}

}
