package com.bmskinner.nuclear_morphology.io.xml;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
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
		if(!f.exists())
			throw new IllegalArgumentException("File "+f.getAbsolutePath()+" does not exist");
		this.file = f;
	}

	/**
	 * Read the XML representation and create the object
	 * @return
	 */
	public abstract T read();
	
	public Document readDocument() throws JDOMException, IOException {
		SAXBuilder saxBuilder = new SAXBuilder();
		return saxBuilder.build(file);
	}
	
	protected int readX(Element e) {
		return Integer.valueOf(e.getChildText(XMLCreator.X));
	}
	
	protected int readY(Element e) {
		return Integer.valueOf(e.getChildText(XMLCreator.Y));
	}

	protected IPoint readPoint(Element e) {
		float x = Float.valueOf(e.getChildText(XMLCreator.X));
		float y = Float.valueOf(e.getChildText(XMLCreator.Y));
		return IPoint.makeNew(x, y);
	}
	
	protected PlottableStatistic readStat(Element e) {
		String name = e.getChildText(XMLCreator.NAME_KEY);
		return PlottableStatistic.of(name);
	}
	
	protected Tag readTag(Element e) {
		String name = e.getChildText(XMLCreator.NAME_KEY);
		return Tag.of(name);
	}
	
	protected UUID readUUID(Element e) {
		return UUID.fromString(e.getChildText(XMLCreator.ID_KEY));
	}
	
	protected int readInt(Element e, String name) {
		return Integer.valueOf(e.getChildText(name));
	}

	
}
