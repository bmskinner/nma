package charting.options;

import gui.components.ColourSelecter.ColourSwatch;
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import stats.PlottableStatistic;
import components.generic.BorderTag;
import components.generic.MeasurementScale;
import components.generic.ProfileType;
import analysis.AnalysisDataset;

public class ChartOptionsBuilder {
	private List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
	private ColourSwatch swatch        = ColourSwatch.REGULAR_SWATCH;
	private Logger programLogger       = null;
	private boolean normalised         = false;
	private ProfileAlignment alignment = ProfileAlignment.LEFT;
	private BorderTag tag              = BorderTag.REFERENCE_POINT;
	private boolean showMarkers        = false;
	private ProfileType type           = ProfileType.REGULAR;
	private int signalGroup            = 1;
	private boolean useDensity         = false;
	private PlottableStatistic stat    = null;
	private MeasurementScale scale     = MeasurementScale.PIXELS;
	
	public ChartOptionsBuilder(){
		
	}
	
	public ChartOptionsBuilder setDatasets(List<AnalysisDataset> list){
		this.list = list;
		if(list!=null && !list.isEmpty()){
			if(list.get(0).getSwatch()!=null){
				this.swatch = list.get(0).getSwatch();
			} else {
				this.swatch = ColourSwatch.REGULAR_SWATCH;
				list.get(0).setSwatch(swatch);
			}
		} else {
			this.swatch = ColourSwatch.REGULAR_SWATCH;
		}
		return this;
	}
	
	public ChartOptionsBuilder setSwatch(ColourSwatch swatch){
		this.swatch = swatch;
		return this;
	}
	
	public ChartOptionsBuilder setLogger(Logger logger){
		this.programLogger = logger;
		return this;
	}
	
	public ChartOptionsBuilder setNormalised(boolean b){
		this.normalised = b;
		return this;
	}
	
	public ChartOptionsBuilder setAlignment(ProfileAlignment alignment){
		this.alignment = alignment;
		return this;
	}
	
	public ChartOptionsBuilder setTag(BorderTag tag){
		this.tag = tag;
		return this;
	}
	
	public ChartOptionsBuilder setShowMarkers(boolean b){
		this.showMarkers = b;
		return this;
	}
	
	public ChartOptionsBuilder setProfileType(ProfileType type){
		this.type = type;
		return this;
	}
	
	public ChartOptionsBuilder setSignalGroup(int group){
		this.signalGroup = group;
		return this;
	}
	
	public ChartOptionsBuilder setUseDensity(boolean b){
		this.useDensity = b;
		return this;
	}
	
	public ChartOptionsBuilder setStatistic(PlottableStatistic s){
		this.stat = s;
		return this;
	}
	
	public ChartOptionsBuilder setScale(MeasurementScale s){
		this.scale = s;
		return this;
	}
	
	public ChartOptions build(){
		ChartOptions result =  new ChartOptions(list, programLogger);
		result.setSwatch(swatch);
		result.setAlignment(alignment);
		result.setNormalised(normalised);
		result.setScale(scale);
		result.setShowMarkers(showMarkers);
		result.setSignalGroup(signalGroup);
		result.setStat(stat);
		result.setTag(tag);
		result.setType(type);
		result.setUseDensity(useDensity);
		return result;
	}
	
}
