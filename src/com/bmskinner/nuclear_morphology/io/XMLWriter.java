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
			System.out.println(String.format("Unable to write to file %s: %s", outputFile.getAbsolutePath(), e.getMessage()));
			e.printStackTrace();
		}
	}
	
	/**
	 * Test if the given string could be a UUID 
	 * @param s
	 * @return
	 */
	public static boolean isUUID(String s) {
		if(s==null)
			return false;
		if(s.length()!=36)
			return false;
		if(s.matches("[\\w|\\d]{8}-[\\w|\\d]{4}-[\\w|\\d]{4}-[\\w|\\d]{4}-[\\w|\\d]{12}"))
			return true;
		return false;
	}

}
