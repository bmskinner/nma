package com.bmskinner.nma.components.datasets;

import java.util.logging.Logger;

import org.jdom2.Element;

import com.bmskinner.nma.components.MissingLandmarkException;
import com.bmskinner.nma.components.Version.UnsupportedVersionException;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;

/**
 * Handle dataset creation from XML to ensure any linking
 * of child datasests and profile collections is properly
 * handled
 * @author ben
 * @since 2.0.0
 *
 */
public class DatasetCreator {
	
	private static final Logger LOGGER = Logger.getLogger(DatasetCreator.class.getName());
	
	private DatasetCreator() {}
	
	/**
	 * Create from a root XML element
	 * @param e
	 * @return
	 * @throws ComponentCreationException
	 * @throws UnsupportedVersionException 
	 * @throws ProfileException 
	 */
	public static IAnalysisDataset createRoot(Element e) throws ComponentCreationException, UnsupportedVersionException {
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
