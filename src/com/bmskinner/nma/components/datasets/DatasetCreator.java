package com.bmskinner.nma.components.datasets;

import java.util.logging.Logger;

import org.jdom2.Element;

import com.bmskinner.nma.analysis.ProgressEvent;
import com.bmskinner.nma.analysis.ProgressListener;
import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.Version.UnsupportedVersionException;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ProfileException;

/**
 * Handle dataset creation from XML to ensure any linking of child datasests and
 * profile collections is properly handled
 * 
 * @author Ben Skinner
 * @since 2.0.0
 *
 */
public class DatasetCreator {

	private static final Logger LOGGER = Logger.getLogger(DatasetCreator.class.getName());

	private DatasetCreator() {
	}

	public static IAnalysisDataset createRoot(Element e)
			throws UnsupportedVersionException, ComponentCreationException {
		return createRoot(e, null);
	}

	/**
	 * Create from a root XML element
	 * 
	 * @param e
	 * @return
	 * @throws ComponentCreationException
	 * @throws UnsupportedVersionException
	 * @throws ProfileException
	 */
	public static IAnalysisDataset createRoot(Element e, ProgressListener l)
			throws ComponentCreationException, UnsupportedVersionException {

		// This is the timeconsuming part
		IAnalysisDataset d = new DefaultAnalysisDataset(e, l);

		// Signal listeners we are nearly done
		if (l != null)
			l.progressEventReceived(new ProgressEvent(e, ProgressEvent.SET_INDETERMINATE, 0));

		try {
			d.getCollection().getProfileCollection().calculateProfiles();

			for (IAnalysisDataset c : d.getAllChildDatasets())
				c.getCollection().getProfileCollection().calculateProfiles();

		} catch (NullPointerException | MissingDataException
				| SegmentUpdateException e1) {
			throw new ComponentCreationException(e1);
		}

		return d;
	}

}
