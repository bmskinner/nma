package charting.options;

import gui.components.ColourSelecter.ColourSwatch;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import logging.Loggable;
import components.generic.MeasurementScale;
import stats.PlottableStatistic;
import analysis.AnalysisDataset;
import analysis.IAnalysisDataset;

/**
 * @author bms41
 *
 */
/**
 * @author ben
 *
 */
/**
 * @author ben
 *
 */
public abstract class AbstractOptions implements DisplayOptions {
	
	private List<IAnalysisDataset> list      = new ArrayList<IAnalysisDataset>();
	private List<PlottableStatistic> stats  = new ArrayList<PlottableStatistic>();;
	private UUID segID                      = null; // the id of the segment (not consistent between datasets)
	private int segPosition                 = 0;    // the position of the segment in the profile (consistent between datasets)
	private MeasurementScale scale          = MeasurementScale.PIXELS;
	private ColourSwatch swatch             = ColourSwatch.REGULAR_SWATCH;
			
	public AbstractOptions(List<IAnalysisDataset> list){
		this.list = list;
	}

	protected void setDatasets(List<IAnalysisDataset> list){
		if(list==null){
			return;
		}
		this.list = list;
	}

	public List<IAnalysisDataset> getDatasets(){
		return this.list;
	}
	
	public void setSwatch(ColourSwatch swatch) {
		this.swatch = swatch;
	}
			
	/**
	 * Get the segmentation colour swatch
	 * @return
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
	
	public int datasetCount(){
		return list.size();
	}
	
	/**
	 * Check if the dataset list has one or many
	 * datasets
	 * @return
	 */
	public boolean isSingleDataset(){
		return(list.size()==1);
	}
	
	public boolean isMultipleDatasets(){
		return(list.size()>1);
	}
	
	/**
	 * Fetch the first dataset in the list
	 * @return
	 */
	public IAnalysisDataset firstDataset(){
		return this.list.get(0);
	}
	
	/**
	 * Get the first statistic in the list
	 * @return
	 */
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
	public List<PlottableStatistic> getStats(){
		return stats;
	}
	
	public PlottableStatistic getStat(int index){
		return stats.get(index);
	}
	
	public UUID getSegID() {
		return segID;
	}

	public void setSegID(UUID segID) {
		this.segID = segID;
	}
	
	public int getSegPosition() {
		return segPosition;
	}

	public void setSegPosition(int segPosition) {
		this.segPosition = segPosition;
	}
	
	public MeasurementScale getScale() {
		return scale;
	}

	public void setScale(MeasurementScale scale) {
		this.scale = scale;
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
		
		return true;
	}
}
