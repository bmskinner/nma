package charting.charts;

import java.util.List;

import analysis.AnalysisDataset;
import gui.components.ColourSelecter.ColourSwatch;
import gui.components.ProfileAlignmentOptionsPanel.ProfileAlignment;
import components.generic.BorderTag;
import components.generic.ProfileCollectionType;

public class ProfileChartOptions extends ChartOptions {
	
	boolean normalised;
	ProfileAlignment alignment;
	BorderTag tag;
	boolean showMarkers;
	ProfileCollectionType type;
	
	public ProfileChartOptions(List<AnalysisDataset> list, 
			boolean normalised, 
			ProfileAlignment alignment, 
			BorderTag tag, 
			boolean showMarkers, 
			ProfileCollectionType type) {
		
		super(list);
		this.normalised = normalised;
		this.alignment = alignment;
		this.tag = tag;
		this.showMarkers = showMarkers;
		this.type = type;
	}

	public boolean isNormalised() {
		return normalised;
	}

	public ProfileAlignment getAlignment() {
		return alignment;
	}

	public BorderTag getTag() {
		return tag;
	}

	public boolean isShowMarkers() {
		return showMarkers;
	}

	public ProfileCollectionType getType() {
		return type;
	}
	
	@Override
	public boolean equals(Object obj){
				
		if (!this.getClass().equals(obj.getClass())){
			return false;
		}

		ProfileChartOptions b = (ProfileChartOptions) obj;
		
		if(!this.list.equals(b.getDatasets())){
			return false;
		}
		
		if(!this.getAlignment().equals(b.getAlignment())){
			return false;
		}
		
		if(!this.getTag().equals(b.getTag())){
			return false;
		}
		
		if(!this.getType().equals(b.getType())){
			return false;
		}
		
		if(this.isNormalised()!=(b.isNormalised())){
			return false;
		}
		
		if(this.isShowMarkers()!=(b.isShowMarkers())){
			return false;
		}
		
		if(!this.getSwatch().equals(b.getSwatch())){
			return false;
		}
		
		return true;
	}
	
	

}
