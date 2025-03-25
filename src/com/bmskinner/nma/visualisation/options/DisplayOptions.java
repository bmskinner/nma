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
package com.bmskinner.nma.visualisation.options;

import java.util.List;
import java.util.UUID;

import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.signals.IShellResult.Aggregation;
import com.bmskinner.nma.components.signals.IShellResult.Normalisation;
import com.bmskinner.nma.components.signals.IShellResult.ShrinkType;
import com.bmskinner.nma.gui.components.ColourSelecter.ColourSwatch;

/**
 * This interface describes the values that should be checkable by chart and
 * table dataset creators. Implementing classes must provide sensible defaults.
 * 
 * @author Ben Skinner
 *
 */
public interface DisplayOptions extends HashOptions {

	/**
	 * Get the datasets to display data for
	 * 
	 * @return
	 */
	List<IAnalysisDataset> getDatasets();

	/**
	 * Get the colour swatch to use when colouring datasets.
	 * 
	 * @return
	 */
	public ColourSwatch getSwatch();

	/**
	 * Check if the dataset list contains datasets
	 * 
	 * @return
	 */
	public boolean hasDatasets();

	/**
	 * Get the number of datasets set.
	 * 
	 * @return the dataset count
	 */
	public int datasetCount();

	/**
	 * Check if the dataset list has one or many datasets
	 * 
	 * @return
	 */
	public boolean isSingleDataset();

	/**
	 * Check if the dataset list has > 1 dataset
	 * 
	 * @return
	 */
	public boolean isMultipleDatasets();

	/**
	 * Fetch the first dataset in the list
	 * 
	 * @return
	 */
	public IAnalysisDataset firstDataset();

	/**
	 * Get the first statistic in the list
	 * 
	 * @return
	 */
	public Measurement getMeasurement();

	/**
	 * Get the saved stats
	 * 
	 * @return
	 */
	public List<Measurement> getStats();

	/**
	 * Get the statistic at the given index
	 * 
	 * @param index
	 * @return
	 */
	public Measurement getStat(int index);

	/**
	 * Get the ID of the segment to display data for
	 * 
	 * @return
	 */
	public UUID getSegID();

	/**
	 * Get the position of the segment to display data for
	 * 
	 * @return
	 */
	public int getSegPosition();

	/**
	 * Get the scale of the data to use in charts or tables
	 * 
	 * @return
	 */
	public MeasurementScale getScale();

	/**
	 * Get the cell data should be drawn for
	 * 
	 * @return
	 */
	ICell getCell();

	/**
	 * Check if a cell is set to display in an outline chart
	 * 
	 * @return
	 */
	boolean hasCell();

	/**
	 * Get the shrinking type used to create shells
	 * 
	 * @return
	 */
	ShrinkType getShrinkType();

	/**
	 * Get the signal aggregation for shell analysis
	 * 
	 * @return
	 */
	Aggregation getAggregation();

	/**
	 * Get the signal normalisation method for shell analysis
	 * 
	 * @return
	 */
	Normalisation getNormalisation();

}
