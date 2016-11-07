package charting.options;

import java.util.List;
import java.util.UUID;

import analysis.IAnalysisDataset;
import components.ICell;
import components.generic.MeasurementScale;
import gui.components.ColourSelecter.ColourSwatch;
import stats.PlottableStatistic;

/**
 * This interface describes the values that should be checkable
 * by chart and table dataset creators. Implementing classes must 
 * provide sensible defaults. 
 * @author bms41
 *
 */
public interface DisplayOptions {
	
	/**
	 * Get the datasets to display data for
	 * @return
	 */
	List<IAnalysisDataset> getDatasets();
	
	/**
	 * Get the colour swatch to use when colouring datasets.
	 * @return
	 */
	public ColourSwatch getSwatch();
	
	/**
	 * Check if the dataset list contains datasets
	 * @return
	 */
	public boolean hasDatasets();
	
	/**
	 * Get the number of datasets set.
	 * @return the dataset count
	 */
	public int datasetCount();
	
	/**
	 * Check if the dataset list has one or many
	 * datasets
	 * @return
	 */
	public boolean isSingleDataset();
	
	/**
	 * Check if the dataset list has > 1 dataset
	 * @return
	 */
	public boolean isMultipleDatasets();
	
	/**
	 * Fetch the first dataset in the list
	 * @return
	 */
	public IAnalysisDataset firstDataset();
	
	/**
	 * Get the first statistic in the list
	 * @return
	 */
	public PlottableStatistic getStat();

		
	/**
	 * Get the saved stats
	 * @return
	 */
	public List<PlottableStatistic> getStats();
	
	/**
	 * Get the statistic at the given index
	 * @param index
	 * @return
	 */
	public PlottableStatistic getStat(int index);

	/**
	 * Get the ID of the segment to display data for
	 * @return
	 */
	public UUID getSegID();

	/**
	 * Get the position of the segment to display data for
	 * @return
	 */
	public int getSegPosition();

	
	/**
	 * Get the scale of the data to use in charts or tables
	 * @return
	 */
	public MeasurementScale getScale();
	
	/**
	 * Get the cell data should be drawn for
	 * @return
	 */
	ICell getCell();
	
	/**
	 * Check if a cell is set to display in an outline chart
	 * @return
	 */
	boolean hasCell();

}
