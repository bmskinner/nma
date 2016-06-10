package charting.options;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import logging.Loggable;
import components.generic.MeasurementScale;
import stats.PlottableStatistic;
import analysis.AnalysisDataset;

/**
 * @author bms41
 *
 */
public abstract class AbstractOptions implements Loggable {
	
	private List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
	private PlottableStatistic stat    = null;
	private UUID segID                 = null; // the id of the segment (not consistent between datasets)
	private int segPosition            = 0;    // the position of the segment in the profile (consistent between datasets)
	private MeasurementScale scale     = MeasurementScale.PIXELS;
			
	public AbstractOptions(List<AnalysisDataset> list){
		this.list = list;
	}

	protected void setDatasets(List<AnalysisDataset> list){
		if(list==null){
			return;
		}
		this.list = list;
	}

	public List<AnalysisDataset> getDatasets(){
		return this.list;
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
		if(list.size()==1){
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isMultipleDatasets(){
		if(list.size()>1){
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Fetch the first dataset in the list
	 * @return
	 */
	public AnalysisDataset firstDataset(){
		return this.list.get(0);
	}
	
	public PlottableStatistic getStat() {
		return stat;
	}

	public void setStat(PlottableStatistic stat) {
		this.stat = stat;
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
		result = prime * result + ((stat == null) ? 0 : stat.hashCode());
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
		if (stat == null) {
			if (other.stat != null)
				return false;
		} else if (!stat.equals(other.stat))
			return false;
		return true;
	}
}
