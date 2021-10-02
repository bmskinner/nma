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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.options.DefaultClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;

/**
 * Read options XML files into analysis options objects
 * @author ben
 * @since 1.14.0
 *
 */
public class OptionsXMLReader extends XMLFileReader<IAnalysisOptions> {
	
	/**
	 * Create with a file to be read
	 * @param f
	 * @throws XMLReadingException 
	 */
	public OptionsXMLReader(@NonNull final File f) throws XMLReadingException {
		super(f);
	}

	@Override
	public IAnalysisOptions read() throws XMLReadingException {


		IAnalysisOptions op = OptionsFactory.makeAnalysisOptions();

		if(!rootElement.getName().equals(XMLCreator.DETECTION_SETTINGS_KEY))
			return op;

		double windowSize = Double.parseDouble(rootElement.getChildText(XMLCreator.PROFILE_WINDOW_KEY));
		op.setAngleWindowProportion(windowSize);
		
		// should be single elements with options class
		for(Element component : rootElement.getChildren(XMLCreator.DETECTION_METHOD_KEY))
			addComponent(component, op);

		return op;
	}
	

	
	/**
	 * Read the clustering options from file
	 * @return the detected options, or an empty list
	 */
	public List<IClusteringOptions> readClusteringOptions(){
		List<IClusteringOptions> result = new ArrayList<>();

		Element clusters = rootElement.getChild(XMLCreator.CLUSTERS_SECTION_KEY);
		if(clusters!=null) { // may not be present
			for(Element component : clusters.getChildren(XMLCreator.CLUSTER_GROUP)) {
				IClusteringOptions o = buildClusteringOptions(component);
				result.add(o);
			}
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
		for(Element signal : rootElement.getChildren(XMLCreator.DETECTION_METHOD_KEY)) {
			if(signal.getAttribute(XMLCreator.DETECTED_OBJECT_KEY).getValue().equals(CellularComponent.NUCLEAR_SIGNAL)) {
				
				// A specific signal group id may have been specified
				Element idElement = signal.getChild(XMLCreator.ID_KEY);
				UUID id =idElement==null?UUID.randomUUID(): UUID.fromString(idElement.getText());		
				
				Element nameElement = signal.getChild(XMLCreator.NAME_KEY);
				String name = nameElement.getText();
				result.put(id, name);
			}
		}
		return result;
	}
	
	private IClusteringOptions buildClusteringOptions(@NonNull Element e) {
		IClusteringOptions o = new DefaultClusteringOptions();
		addKeyedValues(e, o);
		return o;
	}

}
