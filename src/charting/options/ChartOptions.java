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
package charting.options;

import gui.components.ColourSelecter.ColourSwatch;
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import stats.PlottableStatistic;
import components.generic.BorderTag;
import components.generic.MeasurementScale;
import components.generic.ProfileType;
import analysis.AnalysisDataset;

/*
 * Hold the drawing options for a chart. Can store options for profile
 * charts, boxplots, histograms and signal charts. The appropriate options
 * are retrieved on chart generation.
 */
public class ChartOptions extends AbstractOptions {
	
	private ColourSwatch swatch        = ColourSwatch.REGULAR_SWATCH;
	private boolean normalised         = false;
	private ProfileAlignment alignment = ProfileAlignment.LEFT;
	private BorderTag tag              = BorderTag.REFERENCE_POINT;
	private boolean showMarkers        = false;
	private ProfileType type           = ProfileType.REGULAR;
	private int signalGroup            = 1;
	private boolean useDensity         = false;
	
	public ChartOptions(List<AnalysisDataset> list){
		this(list, null);
	}
	
	public ChartOptions(List<AnalysisDataset> list, Logger l){
		super(list, l);
		if(hasDatasets()){
			if(firstDataset().getSwatch()!=null){
				this.swatch = firstDataset().getSwatch();
			} else {
				this.swatch = ColourSwatch.REGULAR_SWATCH;
				firstDataset().setSwatch(swatch);
			}
		} else {
			this.swatch = ColourSwatch.REGULAR_SWATCH;
		}
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
	
	public boolean isNormalised() {
		return normalised;
	}

	public void setNormalised(boolean normalised) {
		this.normalised = normalised;
	}

	public ProfileAlignment getAlignment() {
		return alignment;
	}

	public void setAlignment(ProfileAlignment alignment) {
		this.alignment = alignment;
	}

	public BorderTag getTag() {
		return tag;
	}

	public void setTag(BorderTag tag) {
		this.tag = tag;
	}

	public boolean isShowMarkers() {
		return showMarkers;
	}

	public void setShowMarkers(boolean showMarkers) {
		this.showMarkers = showMarkers;
	}

	public ProfileType getType() {
		return type;
	}

	public void setType(ProfileType type) {
		this.type = type;
	}

	public int getSignalGroup() {
		return signalGroup;
	}

	public void setSignalGroup(int signalGroup) {
		this.signalGroup = signalGroup;
	}

	public boolean isUseDensity() {
		return useDensity;
	}

	public void setUseDensity(boolean useDensity) {
		this.useDensity = useDensity;
	}
	
}
