package com.bmskinner.nuclear_morphology.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.bmskinner.nuclear_morphology.logging.Loggable;

import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.options.DefaultCannyHashOptions;
import com.bmskinner.nuclear_morphology.components.options.DefaultClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.DefaultHoughOptions;
import com.bmskinner.nuclear_morphology.components.options.DefaultShellOptions;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.ICannyOptions;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions.IPreprocessingOptions;
import com.bmskinner.nuclear_morphology.components.options.IHoughDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.components.options.IShellOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.options.PreprocessingOptions;

/**
 * Read options XML files into analysis options objects
 * @author ben
 * @since 1.14.0
 *
 */
public class OptionsXMLReader implements Loggable {
	
	public static final File EMPTY_FILE = new File("empty");
	
	private final File file;
	
	/**
	 * Create with a file to be read
	 * @param f
	 */
	public OptionsXMLReader(@NonNull final File f) {
		file=f;
	}

	/**
	 * Read the analysis options within the file
	 * @return
	 */
	public IAnalysisOptions readAnalysisOptions() {

		try {
			SAXBuilder saxBuilder = new SAXBuilder();
			Document document = saxBuilder.build(file);

			IAnalysisOptions op = OptionsFactory.makeAnalysisOptions();

			Element rootElement = document.getRootElement();

			if(!rootElement.getName().equals(OptionsXMLWriter.DETECTION_LBL))
				return op;

			NucleusType type = NucleusType.valueOf(rootElement.getChild(OptionsXMLWriter.NUCLEUS_TYPE).getText());
			op.setNucleusType(type);
			double windowSize = Double.parseDouble(rootElement.getChild(OptionsXMLWriter.PROFILE_WINDOW).getText());
			op.setAngleWindowProportion(windowSize);

			// should be single elements with options class
			for(Element component : rootElement.getChildren(OptionsXMLWriter.DETECTION_METHOD))
				addComponent(component, op);

			return op;
        
	      } catch(JDOMException e) {
	         e.printStackTrace();
	      } catch(IOException ioe) {
	         ioe.printStackTrace();
	      }
	    return null;
	}
	
	/**
	 * Read the clustering options from file
	 * @return the detected options, or an empty list
	 */
	public List<IClusteringOptions> readClusteringOptions(){
		List<IClusteringOptions> result = new ArrayList<>();
		SAXBuilder saxBuilder = new SAXBuilder();
		Document document;
		try {
			document = saxBuilder.build(file);
			Element rootElement = document.getRootElement();
			Element clusters = rootElement.getChild(OptionsXMLWriter.CLUSTERS);
			if(clusters!=null) { // may not be present
				for(Element component : clusters.getChildren(OptionsXMLWriter.CLUSTER_GROUP)) {
					IClusteringOptions o = buildClusteringOptions(component);
					result.add(o);
				}
			}
		} catch (JDOMException | IOException e) {
			fine("Unable to read clustering options", e);
			return result;
		}
		return result;
	}
	
	private IClusteringOptions buildClusteringOptions(@NonNull Element e) {
		IClusteringOptions o = new DefaultClusteringOptions();
		addKeyedValues(e, o);
		return o;
	}
	
	private String replaceKeyModifications(@NonNull String s) {
		String r = s.replaceAll(OptionsXMLWriter.SPACE_REPLACEMENT, " ")
				.replace(OptionsXMLWriter.UUID_PREFIX, "");
		return r;
	}
	
	private void addKeyedValues(@NonNull Element e, @NonNull HashOptions o) {
		// Primary keys
		List<Element> boolContainer = e.getChildren(OptionsXMLWriter.BOOLEAN_KEY);
		if(!boolContainer.isEmpty()) {
			for(Element el : boolContainer.get(0).getChildren()) {
				System.out.println(el.getName()+": "+el.getText());
				String key = replaceKeyModifications(el.getName());
				boolean b = Boolean.valueOf(el.getText());
				o.setBoolean(key, b);
			}
		}
		
		List<Element> floatContainer = e.getChildren(OptionsXMLWriter.FLOAT_KEY);
		if(!floatContainer.isEmpty()) {
			for(Element el : floatContainer.get(0).getChildren()) {
				System.out.println(el.getName()+": "+el.getText());
				String key = replaceKeyModifications(el.getName());
				float b = Float.valueOf(el.getText());
				o.setFloat(key, b);
			}
		}
		
		List<Element> intContainer = e.getChildren(OptionsXMLWriter.INT_KEY);
		if(!intContainer.isEmpty()) {
			for(Element el : intContainer.get(0).getChildren()) {
				String key = replaceKeyModifications(el.getName());
				System.out.println(el.getName()+": "+el.getText());
				int b = Integer.valueOf(el.getText());
				o.setInt(key, b);
			}
		}
		
		List<Element> doubleContainer = e.getChildren(OptionsXMLWriter.DOUBLE_KEY);
		if(!doubleContainer.isEmpty()) {
			for(Element el : doubleContainer.get(0).getChildren()) {
				System.out.println(el.getName()+": "+el.getText());
				String key = replaceKeyModifications(el.getName());
				double b = Double.valueOf(el.getText());
				o.setDouble(key, b);
			}
		}
		
		List<Element> stringContainer = e.getChildren(OptionsXMLWriter.STRING_KEY);
		if(!stringContainer.isEmpty()) {
			for(Element el : stringContainer.get(0).getChildren()) {
				System.out.println(el.getName()+": "+el.getText());
				String key = replaceKeyModifications(el.getName());
				o.setString(key, el.getText());
			}
		}
	}
	
	private void addComponent(@NonNull Element e, @NonNull IAnalysisOptions op) {
		
		String detectedObject = e.getAttribute(OptionsXMLWriter.DETECTED_OBJECT).getValue();
		
		if(detectedObject.equals(IAnalysisOptions.NUCLEUS)) {			
			File f = op.getDetectionOptions(IAnalysisOptions.NUCLEUS).isPresent() 
					? op.getDetectionOptions(IAnalysisOptions.NUCLEUS).get().getFolder() : EMPTY_FILE;

			IDetectionOptions o = OptionsFactory.makeNucleusDetectionOptions(f);
			
			// Primary keys
			addKeyedValues(e, o);

			for(Element component : e.getChildren(OptionsXMLWriter.SUB_OPTION_KEY)) {
				String subType = component.getAttribute(OptionsXMLWriter.SUB_TYPE_KEY).getValue();
				System.out.println("Component: "+component.getName()+ ": "+subType);
				
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
			op.setDetectionOptions(IAnalysisOptions.NUCLEUS, o);
		}
		
		if(detectedObject.startsWith(IAnalysisOptions.SIGNAL_GROUP) || detectedObject.length()==36 ) { // probably signal group uuid
			try {
				UUID id = UUID.fromString(detectedObject.replace(IAnalysisOptions.SIGNAL_GROUP, ""));
				INuclearSignalOptions n = OptionsFactory.makeNuclearSignalOptions(EMPTY_FILE);
				addKeyedValues(e, n);				
				for(Element component : e.getChildren(OptionsXMLWriter.SUB_OPTION_KEY)) {
					String subType = component.getAttribute(OptionsXMLWriter.SUB_TYPE_KEY).getValue();
					System.out.println("Component: "+component.getName()+ ": "+subType);
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
