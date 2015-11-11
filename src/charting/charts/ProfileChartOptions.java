/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
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
