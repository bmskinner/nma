package com.bmskinner.nuclear_morphology.gui.tabs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

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

		private final List<IAnalysisDataset> datasets = new ArrayList<>();
		
		/**
		 * Create with datasets to display in the ui
		 * @param source
		 * @param list
		 */
		public DatasetSelectionEvent(Object source, @NonNull final Collection<IAnalysisDataset> list) {
			super(source);
			datasets.addAll(list);
		}
		
		public List<IAnalysisDataset> getDatasets(){
			return datasets;
		}
	}
}
