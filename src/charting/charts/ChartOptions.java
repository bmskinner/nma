package charting.charts;

import gui.components.ColourSelecter.ColourSwatch;

import java.util.ArrayList;
import java.util.List;

import analysis.AnalysisDataset;

/*
 * Hold options for drawing a chart
 */
public abstract class ChartOptions {
	
	protected List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
	protected ColourSwatch swatch;


//	// Profiles
//	private boolean normalised;
//	private ProfileAlignment alignment;
	
	public ChartOptions(List<AnalysisDataset> list){
		this.list = list;
		if(list!=null && !list.isEmpty()){
			if(list.get(0).getSwatch()!=null){
				this.swatch = list.get(0).getSwatch();
			} else {
				this.swatch = ColourSwatch.REGULAR_SWATCH;
			}
		}
		
	}
	
	
	/**
	 * Fetch all the datasets
	 * @return
	 */
	public List<AnalysisDataset> getDatasets(){
		return this.list;
	}
	
	/**
	 * Get the segmentation colour swatch
	 * @return
	 */
	public ColourSwatch getSwatch(){
		return this.swatch;
	}
	
	/**
	 * Fetch the first dataset in the list
	 * @return
	 */
	public AnalysisDataset firstDataset(){
		return this.list.get(0);
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
	
	
}
