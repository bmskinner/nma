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
package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.events.revamp.NuclearSignalUpdatedListener;
import com.bmskinner.nuclear_morphology.gui.tabs.AbstractScatterChartPanel;

@SuppressWarnings("serial")
public class SignalScatterChartPanel extends AbstractScatterChartPanel implements NuclearSignalUpdatedListener {

	public SignalScatterChartPanel(@NonNull InputSupplier context) {
		super(context, CellularComponent.NUCLEAR_SIGNAL);
		uiController.addNuclearSignalUpdatedListener(this);
	}

	@Override
	public void nuclearSignalUpdated(List<IAnalysisDataset> datasets) {
		refreshChartCache(datasets);
	}

	@Override
	public void nuclearSignalUpdated(IAnalysisDataset dataset) {
		refreshChartCache(dataset);
	}

	@Override
	public void scaleUpdated(List<IAnalysisDataset> datasets) {
		refreshChartCache(datasets);
	}

	@Override
	public void scaleUpdated(IAnalysisDataset dataset) {
		refreshChartCache(dataset);
	}

	@Override
	public void scaleUpdated() {
		update(getDatasets());
	}

	@Override
	public void swatchUpdated() {
		update(getDatasets());
	}

}
