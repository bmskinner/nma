package com.bmskinner.nuclear_morphology.charting.options;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellCounter.CountType;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter.ColourSwatch;
import com.bmskinner.nuclear_morphology.stats.PlottableStatistic;

/**
 * This implements the requirements of the DisplayOptions
 * interface, providing access to options common to charts
 * and tables.
 * @author bms41
 * @since 1.12.0
 *
 */
public abstract class AbstractOptions implements DisplayOptions {
	
	private List<IAnalysisDataset> list     = new ArrayList<IAnalysisDataset>();
	private List<PlottableStatistic> stats  = new ArrayList<PlottableStatistic>();;
	private UUID segID                      = null; // the id of the segment (not consistent between datasets)
	private int segPosition                 = 0;    // the position of the segment in the profile (consistent between datasets)
	private MeasurementScale scale          = MeasurementScale.PIXELS;
	private ColourSwatch swatch             = ColourSwatch.REGULAR_SWATCH;
	private ICell cell                      = null;
	private CountType type                  = CountType.SIGNAL;
			
	/**
	 * Create with a list of datasets.
	 * @param list the datasets to display
	 */
	public AbstractOptions(List<IAnalysisDataset> list){
		if(list==null){
			return;
		}
		this.list = new ArrayList<IAnalysisDataset>(list);
	}

	/**
	 * Set the list of datasets.
	 * @param list the datasets to display
	 */
	protected void setDatasets(List<IAnalysisDataset> list){
		if(list==null){
			return;
		}
		this.list = new ArrayList<IAnalysisDataset>(list);
	}

	/**
	 * Get the list of datasets.
	 * @return the stored datasets
	 */
	public List<IAnalysisDataset> getDatasets(){
		return new ArrayList<IAnalysisDataset>(list);
	}
	
	
	/**
	 * Set the colour swatch to use in the chart or table
	 * @param swatch the colour swatch
	 */
	public void setSwatch(ColourSwatch swatch) {
		this.swatch = swatch;
	}
			
	/**
	 * Get the stored colour swatch
	 * @return a swatch
	 */
	public ColourSwatch getSwatch(){
		return this.swatch;
	}
	
	/**
	 * Check if the dataset list contains datasets
	 * @return
	 */
	public boolean hasDatasets(){
		if(list==null || list.isEmpty()){
			return false;
		} else {
			return true;
		}
	}
	
	@Override
	public int datasetCount(){
		return list.size();
	}
	
	/**
	 * Check if the dataset list has one or many
	 * datasets
	 * @return
	 */
	@Override
	public boolean isSingleDataset(){
		return(list.size()==1);
	}
	
	/* (non-Javadoc)
	 * @see charting.options.DisplayOptions#isMultipleDatasets()
	 */
	@Override
	public boolean isMultipleDatasets(){
		return(list.size()>1);
	}
	
	/**
	 * Fetch the first dataset in the list
	 * @return
	 */
	@Override
	public IAnalysisDataset firstDataset(){
		return this.list.get(0);
	}
	
	/**
	 * Get the first statistic in the list
	 * @return
	 */
	@Override
	public PlottableStatistic getStat() {
		return stats.get(0);
	}

	
	/**
	 * Set the first statistic in the list (for backwards compatibility)
	 * @param stat
	 */
	public void setStat(PlottableStatistic stat) {
		this.stats.set(0, stat);
	}
	
	/**
	 * Replace all internal stats with the given list
	 * @param stats
	 */
	public void setStats(List<PlottableStatistic> stats){
		this.stats = stats;
	}
	
	/**
	 * Append the given stat to the end of the internal list
	 * @param stat
	 */
	public void addStat(PlottableStatistic stat){
		stats.add(stat);
	}
	
	/**
	 * Get the saved stats
	 * @return
	 */
	@Override
	public List<PlottableStatistic> getStats(){
		return stats;
	}
	
	/* (non-Javadoc)
	 * @see charting.options.DisplayOptions#getStat(int)
	 */
	@Override
	public PlottableStatistic getStat(int index){
		return stats.get(index);
	}
	
	@Override
	public UUID getSegID() {
		return segID;
	}

	/**
	 * Set the segment ID
	 * @param segID the ID
	 */
	public void setSegID(UUID segID) {
		this.segID = segID;
	}
	
	@Override
	public int getSegPosition() {
		return segPosition;
	}

	/**
	 * Set the segment position to display
	 * @param segPosition the position, starting from 0
	 */
	public void setSegPosition(int segPosition) {
		this.segPosition = segPosition;
	}
	
	@Override
	public MeasurementScale getScale() {
		return scale;
	}

	/**
	 * Set the scale to display at. Default is pixels.
	 * @param scale the measurement scale
	 */
	public void setScale(MeasurementScale scale) {
		this.scale = scale;
	}
	
	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#getCell()
	 */
	@Override
	public ICell getCell() {
		return cell;
	}


	/**
	 * Set the cell to diaplay
	 * @param cell the cell
	 */
	public void setCell(ICell cell) {
		this.cell = cell;
	}
	

	@Override
	public boolean hasCell(){
		return this.cell!=null;
	}
	
	public CountType getCountType(){
		return type;
	}
	
	public void setCountType(CountType t){
		type = t;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((list == null) ? 0 : list.hashCode());
		result = prime * result + ((scale == null) ? 0 : scale.hashCode());
		result = prime * result + ((segID == null) ? 0 : segID.hashCode());
		result = prime * result + segPosition;
		result = prime * result + ((stats == null) ? 0 : stats.hashCode());
		result = prime * result + ((swatch == null) ? 0 : swatch.hashCode());
		result = prime * result	+ ((cell == null) ? 0 : cell.hashCode());
		result = prime * result	+ ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		AbstractOptions other = (AbstractOptions) obj;
		
		if (list == null) {
			if (other.list != null)
				return false;
		} else if (!list.equals(other.list))
			return false;
		if (scale != other.scale)
			return false;
		if (segID == null) {
			if (other.segID != null)
				return false;
		} else if (!segID.equals(other.segID))
			return false;
		if (segPosition != other.segPosition)
			return false;
		if (stats == null) {
			if (other.stats != null)
				return false;
		} else if (!stats.equals(other.stats))
			return false;
		if (swatch != other.swatch)
			return false;
		
		if (cell == null) {
			if (other.cell != null)
				return false;
		} else if (!cell.equals(other.cell))
			return false;
		
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		
		return true;
	}
}
