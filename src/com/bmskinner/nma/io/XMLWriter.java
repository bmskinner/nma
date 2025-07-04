/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * Base class for XML writers
 * @author ben
 * @since 1.14.0
 *
 */
public abstract class XMLWriter {
	
	private static final Logger LOGGER = Logger.getLogger(XMLWriter.class.getName());
	
	/**
	 * Write the given XML element to a file
	 * @param e the xml element
	 * @param outputFile the file to write to
	 * @throws IOException if the write fails
	 */
	public static void writeXML(@NonNull Element e, @NonNull File outputFile) throws IOException {
		writeXML(new Document(e), outputFile);
	}
	
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

		try(
				OutputStream os = new FileOutputStream(outputFile);
				CountedOutputStream cos = new CountedOutputStream(os);
			){
			XMLOutputter xmlOutput = new XMLOutputter();
			
			xmlOutput.setFormat(Format.getPrettyFormat());
			xmlOutput.output(doc, cos);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Unable to write file: "+outputFile.getAbsolutePath(), e);
		}
	}
	
}
