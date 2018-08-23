package com.bmskinner.nuclear_morphology.gui.tabs;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;

/**
 * Listener for events setting which datasets are selected
 * @author bms41
 * @since 1.14.0
 *
 */
public interface DatasetSelectionListener {
	void datasetSelectionEventReceived(DatasetSelectionEvent e);

	/**
	 * An event instructing the UI to select the datasets contained within.
	 * @author bms41
	 * @since 1.14.0
	 *
	 */
	public class DatasetSelectionEvent extends EventObject {

		List<IAnalysisDataset> datasets = new ArrayList<>();
		public DatasetSelectionEvent(Object source, List<IAnalysisDataset> list) {
			super(source);
			datasets.addAll(list);
		}
		
		public List<IAnalysisDataset> getDatasets(){
			return datasets;
		}
	}
}
