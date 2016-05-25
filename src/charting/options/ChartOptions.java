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

import java.util.List;
import java.util.logging.Logger;

import components.generic.BorderTag;
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
	private boolean hideProfiles       = false;
	private ProfileType type           = ProfileType.REGULAR;
	private int signalGroup            = 1;
	private boolean useDensity         = false;
	private double modalityPosition    = 0;
	private boolean showPoints         = false;
	private boolean showLines          = true;
	private boolean showAnnotations    = true;
	
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
	
	

	public double getModalityPosition() {
		return modalityPosition;
	}

	public void setModalityPosition(double modalityPosition) {
		this.modalityPosition = modalityPosition;
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
	
	public boolean isHideProfiles() {
		return hideProfiles;
	}

	public void setHideProfiles(boolean hideProfiles) {
		this.hideProfiles = hideProfiles;
	}
	

	public boolean isShowPoints() {
		return showPoints;
	}

	public void setShowPoints(boolean showPoints) {
		this.showPoints = showPoints;
	}

	public boolean isShowLines() {
		return showLines;
	}

	public void setShowLines(boolean showLines) {
		this.showLines = showLines;
	}
	
	

	public boolean isShowAnnotations() {
		return showAnnotations;
	}

	public void setShowAnnotations(boolean showAnnotations) {
		this.showAnnotations = showAnnotations;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((alignment == null) ? 0 : alignment.hashCode());
		long temp;
		temp = Double.doubleToLongBits(modalityPosition);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + (normalised ? 1231 : 1237);
		result = prime * result + (showMarkers ? 1231 : 1237);
		result = prime * result + (hideProfiles ? 1231 : 1237);
		result = prime * result + (showPoints ? 1231 : 1237);
		result = prime * result + (showLines ? 1231 : 1237);
		result = prime * result + (showAnnotations ? 1231 : 1237);
		result = prime * result + signalGroup;
		result = prime * result + ((swatch == null) ? 0 : swatch.hashCode());
		result = prime * result + ((tag == null) ? 0 : tag.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + (useDensity ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChartOptions other = (ChartOptions) obj;
		if (alignment != other.alignment)
			return false;
		if (Double.doubleToLongBits(modalityPosition) != Double
				.doubleToLongBits(other.modalityPosition))
			return false;
		if (normalised != other.normalised)
			return false;
		if (showMarkers != other.showMarkers)
			return false;
		if (hideProfiles != other.hideProfiles)
			return false;
		if (showPoints != other.showPoints)
			return false;
		if (showLines != other.showLines)
			return false;
		if (showLines != other.showAnnotations)
			return false;
		if (signalGroup != other.signalGroup)
			return false;
		if (swatch != other.swatch)
			return false;
		if (tag != other.tag)
			return false;
		if (type != other.type)
			return false;
		if (useDensity != other.useDensity)
			return false;
		return true;
	}
	
	
	
}
