package com.bmskinner.nuclear_morphology.io.xml;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.options.DefaultCannyHashOptions;
import com.bmskinner.nuclear_morphology.components.options.DefaultHoughOptions;
import com.bmskinner.nuclear_morphology.components.options.DefaultShellOptions;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.ICannyOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions.IPreprocessingOptions;
import com.bmskinner.nuclear_morphology.components.options.IHoughDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.components.options.IShellOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.options.PreprocessingOptions;
import com.bmskinner.nuclear_morphology.components.profiles.LandmarkType;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;

/**
 * Base class for XML readers
 * @author bms41
 * @since 1.14.0
 *
 * @param <T> the type of object to be read
 */
public abstract class XMLReader<T> {
	
	/** A placeholder value for missing files */
	public static final File EMPTY_FILE = new File("empty");
	
	protected final Element rootElement;
	
	/**
	 * Create a reader with a root element
	 * @param e
	 */
	public XMLReader(@NonNull final Element e){
		rootElement = e;
	}
	
	
	/**
	 * Thrown when an xml file cannot be read
	 * @author bms41
	 * @since 1.14.0
	 *
	 */
	public static class XMLReadingException extends Exception {
		private static final long serialVersionUID = 1L;

	    public XMLReadingException() { super(); }
	    
	    public XMLReadingException(String message) {super(message); }

	    public XMLReadingException(String message, Throwable cause) { super(message, cause); }

	    public XMLReadingException(Throwable cause) { super(cause); }
	}
	
	/**
	 * Read the XML representation and create the object
	 * @return
	 */
	public abstract T read() throws XMLReadingException;
	
	/**
	 * Read the given file as an XML document
	 * @param file the file to read
	 * @return the XML representation of the file content
	 * @throws XMLReadingException if the document could not be read or was not XML
	 */
	public static Document readDocument(File file) throws XMLReadingException {
		SAXBuilder saxBuilder = new SAXBuilder();
		try {
			return saxBuilder.build(file);
		} catch(JDOMException | IOException e) {
			throw new XMLReadingException(String.format("Unable to read file %s as XML: %s", file.getAbsolutePath(), e.getMessage()), e);
		}
	}
	
	/**
	 * Parse the given element as an X coordinate
	 * @param e the element to parse
	 * @return
	 */
	protected int readX(Element e) {
		return Integer.valueOf(e.getChildText(XMLCreator.X));
	}
	
	/**
	 * Parse the given element as a Y coordinate
	 * @param e the element to parse
	 * @return
	 */
	protected int readY(Element e) {
		return Integer.valueOf(e.getChildText(XMLCreator.Y));
	}
	
	/**
	 * Parse the given element as a file
	 * @param e the element to parse
	 * @return
	 */
	protected File readFile(Element e, String key) {
		return new File(e.getChildText(key));
	}

	/**
	 * Parse the given element as an XY point
	 * @param e the element to parse
	 * @return a point with the XY coordinates in the element
	 */
	protected IPoint readPoint(Element e) {
		float x = Float.parseFloat(e.getChildText(XMLCreator.X));
		float y = Float.parseFloat(e.getChildText(XMLCreator.Y));
		return IPoint.makeNew(x, y);
	}
		
	/**
	 * Parse the given element as a measured statistic
	 * @param e the element to parse
	 * @return
	 */
	protected Measurement readStat(Element e) {
		String name = e.getChildText(XMLCreator.NAME_KEY);
		return Measurement.of(name);
	}
	
	/**
	 * Parse the given element as a tag
	 * @param e the element to parse
	 * @return
	 */
	protected Landmark readTag(Element e) {
		String name = e.getChildText(XMLCreator.NAME_KEY);
		String type = e.getChildText(XMLCreator.TYPE_KEY);
		return Landmark.of(name, LandmarkType.valueOf(type));
	}
	
	/**
	 * Parse the given element as a UUID
	 * @param e the element to parse
	 * @return
	 */
	protected UUID readUUID(Element e) {
		return UUID.fromString(e.getChildText(XMLCreator.ID_KEY));
	}
	
	/**
	 * Parse the given element as an integer
	 * @param e the element to parse
	 * @return
	 */
	protected int readInt(Element e, String name) {
		return Integer.valueOf(e.getChildText(name));
	}
	
	/**
	 * Parse the given element as a double
	 * @param e the element to parse
	 * @return
	 */
	protected double readDouble(Element e, String name) {
		return Double.valueOf(e.getChildText(name));
	}
	
	/**
	 * Parse the given element and add all values to the 
	 * given options object
	 * @param e the element to parse
	 * @param o the options to add values to
	 */
	protected void addKeyedValues(@NonNull Element e, @NonNull HashOptions o) {
		// Primary keys
		List<Element> boolContainer = e.getChildren(XMLCreator.BOOLEAN_KEY);
		if(!boolContainer.isEmpty()) {
			for(Element el : boolContainer.get(0).getChildren(XMLCreator.PAIR_KEY)) {
				String key = el.getChild(XMLCreator.KEY_KEY).getText();
				String val = el.getChild(XMLCreator.VALUE_KEY).getText();
				o.setBoolean(key, Boolean.valueOf(val));
			}
		}
		
		List<Element> floatContainer = e.getChildren(XMLCreator.FLOAT_KEY);
		if(!floatContainer.isEmpty()) {
			for(Element el : floatContainer.get(0).getChildren(XMLCreator.PAIR_KEY)) {
				String key = el.getChild(XMLCreator.KEY_KEY).getText();
				String val = el.getChild(XMLCreator.VALUE_KEY).getText();
				o.setFloat(key, Float.valueOf(val));
			}
		}
		
		List<Element> intContainer = e.getChildren(XMLCreator.INT_KEY);
		if(!intContainer.isEmpty()) {
			for(Element el : intContainer.get(0).getChildren(XMLCreator.PAIR_KEY)) {
				String key = el.getChild(XMLCreator.KEY_KEY).getText();
				String val = el.getChild(XMLCreator.VALUE_KEY).getText();
				o.setInt(key, Integer.valueOf(val));
			}
		}
		
		List<Element> doubleContainer = e.getChildren(XMLCreator.DOUBLE_KEY);
		if(!doubleContainer.isEmpty()) {
			for(Element el : doubleContainer.get(0).getChildren(XMLCreator.PAIR_KEY)) {
				String key = el.getChild(XMLCreator.KEY_KEY).getText();
				String val = el.getChild(XMLCreator.VALUE_KEY).getText();
				o.setDouble(key, Double.valueOf(val));
			}
		}
		
		List<Element> stringContainer = e.getChildren(XMLCreator.STRING_KEY);
		if(!stringContainer.isEmpty()) {
			for(Element el : stringContainer.get(0).getChildren(XMLCreator.PAIR_KEY)) {
				String key = el.getChild(XMLCreator.KEY_KEY).getText();
				String val = el.getChild(XMLCreator.VALUE_KEY).getText();
				o.setString(key, val);
			}
		}
	}
	
	/**
	 * Parse the given element as a component and add
	 * to the given options
	 * @param e the element to parse
	 * @param op the options to add values to
	 */
	protected void addComponent(@NonNull Element e, @NonNull IAnalysisOptions op) {
		
		String detectedObject = e.getAttribute(XMLCreator.DETECTED_OBJECT_KEY).getValue();
		
		if(detectedObject.equals(CellularComponent.NUCLEUS)) {			
			addComponentNucleus(e, op);
		}
		
		if(detectedObject.startsWith(CellularComponent.NUCLEAR_SIGNAL) ) {
			addComponentNuclearSignal(e, op);
		}
	}

	/**
	 * Parse the given element as nucleus detection options and add
	 * to the given options
	 * @param e the element to parse
	 * @param op the options to add values to
	 */
	private void addComponentNucleus(@NonNull Element e, @NonNull IAnalysisOptions op) {
		File f = op.getDetectionOptions(CellularComponent.NUCLEUS).isPresent() 
				? op.getDetectionOptions(CellularComponent.NUCLEUS).get().getFolder() : EMPTY_FILE;

		IDetectionOptions o = OptionsFactory.makeNucleusDetectionOptions(f);
		
		// Primary keys
		addKeyedValues(e, o);

		for(Element component : e.getChildren(XMLCreator.SUB_OPTION_KEY)) {
			String subType = component.getAttribute(XMLCreator.SUB_TYPE_KEY).getValue();
			
			if(subType.equals(IDetectionSubOptions.BACKGROUND_OPTIONS)) {
				IPreprocessingOptions pre = new PreprocessingOptions();
				addKeyedValues(component, pre);
				o.setSubOptions(subType, pre);
			}
			
			if(subType.equals(IDetectionSubOptions.HOUGH_OPTIONS)) {
				IHoughDetectionOptions hough = new DefaultHoughOptions();
				addKeyedValues(component, hough);
				o.setSubOptions(subType, hough);
			}
			
			if(subType.equals(IDetectionSubOptions.CANNY_OPTIONS)) {
				ICannyOptions canny = new DefaultCannyHashOptions();
				addKeyedValues(component, canny);
				o.setSubOptions(subType, canny);
			}
		}
		op.setDetectionOptions(CellularComponent.NUCLEUS, o);
	}
	
	/**
	 * Parse the given element as nucleus signal detection options and add
	 * to the given options
	 * @param e the element to parse
	 * @param op the options to add values to
	 */
	private void addComponentNuclearSignal(@NonNull Element e, @NonNull IAnalysisOptions op) {
		try {
			Element idElement = e.getChild(XMLCreator.ID_KEY);
			UUID id = idElement==null ? UUID.randomUUID() : UUID.fromString(idElement.getText());				
			INuclearSignalOptions n = OptionsFactory.makeNuclearSignalOptions(EMPTY_FILE);
			addKeyedValues(e, n);				
			for(Element component : e.getChildren(XMLCreator.SUB_OPTION_KEY)) {
				String subType = component.getAttribute(XMLCreator.SUB_TYPE_KEY).getValue();
				if(subType.equals(IDetectionSubOptions.SHELL_OPTIONS)) {
					IShellOptions s = new DefaultShellOptions();
					addKeyedValues(component, s);
					n.setShellOptions(s);
				}
			}
			op.setDetectionOptions(IAnalysisOptions.SIGNAL_GROUP+id.toString(), n);
		} catch(IllegalArgumentException e1) {
			// it wasn't a uuid
		}
	}
	
}
