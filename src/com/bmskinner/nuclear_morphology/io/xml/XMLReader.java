package com.bmskinner.nuclear_morphology.io.xml;

import java.io.File;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;

import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Base class for XML readers
 * @author bms41
 * @since 1.14.0
 *
 * @param <T> the type of object to be read
 */
public abstract class XMLReader<T> implements Loggable {
	
	protected final File file;
	
	public XMLReader(@NonNull final File f) {
		this.file = f;
	}
	
	/**
	 * Read the XML representation and create the object
	 * @return
	 */
	public abstract T read();

}
