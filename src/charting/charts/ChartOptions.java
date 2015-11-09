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
		this.swatch = list.get(0).getSwatch();
	}
	
	
	public List<AnalysisDataset> getDatasets(){
		return this.list;
	}
	
	public ColourSwatch getSwatch(){
		return this.swatch;
	}
	
	
}
