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
package com.bmskinner.nuclear_morphology.io.xml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
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
public class OptionsXMLReader extends XMLReader<IAnalysisOptions> {
	
	/**
	 * Create with a file to be read
	 * @param f
	 */
	public OptionsXMLReader(@NonNull final File f) {
		super(f);
	}

	@Override
	public IAnalysisOptions read() {

		try {
			Document document =  readDocument();

			IAnalysisOptions op = OptionsFactory.makeAnalysisOptions();

			Element rootElement = document.getRootElement();

			if(!rootElement.getName().equals(XMLCreator.DETECTION_SETTINGS_KEY))
				return op;

			NucleusType type = NucleusType.valueOf(rootElement.getChild(XMLCreator.NUCLEUS_TYPE_KEY).getText());
			op.setNucleusType(type);
			double windowSize = Double.parseDouble(rootElement.getChild(XMLCreator.PROFILE_WINDOW_KEY).getText());
			op.setAngleWindowProportion(windowSize);

			// should be single elements with options class
			for(Element component : rootElement.getChildren(XMLCreator.DETECTION_METHOD_KEY))
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
			Element clusters = rootElement.getChild(OptionsXMLCreator.CLUSTERS);
			if(clusters!=null) { // may not be present
				for(Element component : clusters.getChildren(OptionsXMLCreator.CLUSTER_GROUP)) {
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
	
	/**
	 * Read the signal group names in the file, and map them to the
	 * signal group ids
	 * @return
	 */
	public Map<UUID, String> readSignalGroupNames(){
		Map<UUID, String> result = new HashMap<>();
		SAXBuilder saxBuilder = new SAXBuilder();
		Document document;
		try {
			document = saxBuilder.build(file);
			Element rootElement = document.getRootElement();
			for(Element signal : rootElement.getChildren(XMLCreator.DETECTION_METHOD_KEY)) {
				if(signal.getAttribute(XMLCreator.DETECTED_OBJECT_KEY).getValue().equals(IAnalysisOptions.NUCLEAR_SIGNAL)) {
					Element idElement = signal.getChild(OptionsXMLCreator.SIGNAL_ID);
					UUID id =idElement==null?UUID.randomUUID(): UUID.fromString(idElement.getText());		
					
					Element nameElement = signal.getChild(OptionsXMLCreator.SIGNAL_NAME);
					String name = nameElement.getText();
					result.put(id, name);
				}
			}
		} catch (JDOMException | IOException e) {
			fine("Unable to read signal names", e);
			return result;
		}
		return result;
	}
	
	private IClusteringOptions buildClusteringOptions(@NonNull Element e) {
		IClusteringOptions o = new DefaultClusteringOptions();
		addKeyedValues(e, o);
		return o;
	}

}
