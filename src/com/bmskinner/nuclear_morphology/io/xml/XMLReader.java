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

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
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
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

/**
 * Base class for XML readers
 * @author bms41
 * @since 1.14.0
 *
 * @param <T> the type of object to be read
 */
public abstract class XMLReader<T> {
	
	public static final File EMPTY_FILE = new File("empty");
	
	protected final Element rootElement;
	
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
	
	public static Document readDocument(File file) throws XMLReadingException {
		SAXBuilder saxBuilder = new SAXBuilder();
		try {
			return saxBuilder.build(file);
		} catch(JDOMException | IOException e) {
			throw new XMLReadingException(String.format("Unable to read file %s as XML: %s", file.getAbsolutePath(), e.getMessage()), e);
		}
	}
	
	protected int readX(Element e) {
		return Integer.valueOf(e.getChildText(XMLCreator.X));
	}
	
	protected int readY(Element e) {
		return Integer.valueOf(e.getChildText(XMLCreator.Y));
	}
	
	protected File readFile(Element e, String key) {
		return new File(e.getChildText(key));
	}

	protected IPoint readPoint(Element e) {
		float x = Float.parseFloat(e.getChildText(XMLCreator.X));
		float y = Float.parseFloat(e.getChildText(XMLCreator.Y));
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
	
	protected double readDouble(Element e, String name) {
		return Double.valueOf(e.getChildText(name));
	}
	
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
	
	protected void addComponent(@NonNull Element e, @NonNull IAnalysisOptions op) {
		
		String detectedObject = e.getAttribute(XMLCreator.DETECTED_OBJECT_KEY).getValue();
		
		if(detectedObject.equals(CellularComponent.NUCLEUS)) {			
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
		
		if(detectedObject.startsWith(CellularComponent.NUCLEAR_SIGNAL) ) {
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

	
}
