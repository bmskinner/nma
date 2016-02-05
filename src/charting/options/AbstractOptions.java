package charting.options;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import components.generic.MeasurementScale;

import stats.PlottableStatistic;
import analysis.AnalysisDataset;

/**
 * @author bms41
 *
 */
public abstract class AbstractOptions {
	
	private List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
	private Logger programLogger       = null;
	private PlottableStatistic stat    = null;
	private UUID segID                 = null; // the id of the segment (not consistent between datasets)
	private int segPosition            = 0;    // the position of the segment in the profile (consistent between datasets)
	private MeasurementScale scale     = MeasurementScale.PIXELS;
		
	public AbstractOptions(List<AnalysisDataset> list){
		this(list, null);
	}
	
	public AbstractOptions(List<AnalysisDataset> list, Logger programLogger){
		this.list = list;
		this.programLogger = programLogger;
	}

	protected void setDatasets(List<AnalysisDataset> list){
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
	
	
	public void setLogger(Logger l){
		this.programLogger = l;
	}
	
	public void log(Level level, String message){
		
		if(this.hasLogger()){
			this.getLogger().log(level, message);
		}
	}
	
	public void log(Level level, String message, Throwable e){

		if(this.hasLogger()){
			this.getLogger().log(level, message, e);
		}
	}
	
	public Logger getLogger(){
		return this.programLogger;
	}
	
	private boolean hasLogger(){
		if(this.programLogger==null){
			return false;
		} else {
			return true;
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
}
