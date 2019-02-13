package com.bmskinner.nuclear_morphology.io.xml;

import java.io.File;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Base class for XML readers that take files as an input
 * @author bms41
 * @since 1.15.1
 *
 * @param <T> the type of object to be read
 */
public abstract class XMLFileReader<T> extends XMLReader<T> {
	
	protected final File file;
	
	/**
	 * Create with a file to be read
	 * @param f
	 * @throws XMLReadingException 
	 */
	public XMLFileReader(@NonNull final File f) throws XMLReadingException {
		super(readDocument(f).getRootElement());
		if(!f.exists())
			throw new IllegalArgumentException("File "+f.getAbsolutePath()+" does not exist");
		this.file = f;
	}
}
