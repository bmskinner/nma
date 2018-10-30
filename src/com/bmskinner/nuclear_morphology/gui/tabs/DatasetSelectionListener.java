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
