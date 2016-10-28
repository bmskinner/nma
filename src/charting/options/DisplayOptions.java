package charting.options;

import java.util.List;

import analysis.IAnalysisDataset;
import gui.components.ColourSelecter.ColourSwatch;
import stats.PlottableStatistic;

public interface DisplayOptions {
	
	List<IAnalysisDataset> getDatasets();
	
	public ColourSwatch getSwatch();
	
	/**
	 * Check if the dataset list contains datasets
	 * @return
	 */
	public boolean hasDatasets();
	
	public int datasetCount();
	
	/**
	 * Check if the dataset list has one or many
	 * datasets
	 * @return
	 */
	public boolean isSingleDataset();
	
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
	 * Set the first statistic in the list (for backwards compatibility)
	 * @param stat
	 */
	public void setStat(PlottableStatistic stat);
	
	/**
	 * Replace all internal stats with the given list
	 * @param stats
	 */
	public void setStats(List<PlottableStatistic> stats);
	
	/**
	 * Append the given stat to the end of the internal list
	 * @param stat
	 */
	public void addStat(PlottableStatistic stat);
	
	/**
	 * Get the saved stats
	 * @return
	 */
	public List<PlottableStatistic> getStats();
	
	public PlottableStatistic getStat(int index);

}
