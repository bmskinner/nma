package com.bmskinner.nuclear_morphology.components.datasets;

import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;

/**
 * Handle dataset creation from XML to ensure any linking
 * of child datasests and profile collections is properly
 * handled
 * @author ben
 * @since 2.0.0
 *
 */
public class DatasetCreator {
	
	private DatasetCreator() {}
	
	/**
	 * Create from a root XML element
	 * @param e
	 * @return
	 * @throws ComponentCreationException
	 * @throws ProfileException 
	 */
	public static IAnalysisDataset createRoot(Element e) throws ComponentCreationException {
		IAnalysisDataset d = new DefaultAnalysisDataset(e);
		try {
			d.getCollection().getProfileCollection().calculateProfiles();

			for(IAnalysisDataset c : d.getAllChildDatasets())
				c.getCollection().getProfileCollection().calculateProfiles();

		} catch(ProfileException | MissingLandmarkException | MissingProfileException e1) {
			throw new ComponentCreationException(e1);
		}

		return d;
	}

}
