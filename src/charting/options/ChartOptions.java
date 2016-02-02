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
public class ChartOptions {
	
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
	private String segName             = "Seg_0";
	
	public ChartOptions(List<AnalysisDataset> list){
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
		
	}
	
	public ChartOptions(List<AnalysisDataset> list, Logger l){
		this(list);
		this.programLogger = l;
	}
	
	public String toString(){
		StringBuilder builder = new StringBuilder();
		builder.append("Chart options:\n");
		builder.append("\tDatasets: "+list.size()+"\n");
		builder.append("\tSwatch: "+swatch.toString()+"\n");
		return builder.toString();

	}
	
	
	
	public void setSwatch(ColourSwatch swatch) {
		this.swatch = swatch;
	}

	public void setLogger(Logger l){
		this.programLogger = l;
	}
	
	public Logger getLogger(){
		return this.programLogger;
	}
	
	public boolean hasLogger(){
		if(this.programLogger==null){
			return false;
		} else {
			return true;
		}
	}
		
	
	public String getSegName() {
		return segName;
	}

	public void setSegName(String segName) {
		this.segName = segName;
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

	public PlottableStatistic getStat() {
		return stat;
	}

	public void setStat(PlottableStatistic stat) {
		this.stat = stat;
	}

	public MeasurementScale getScale() {
		return scale;
	}

	public void setScale(MeasurementScale scale) {
		this.scale = scale;
	}
	
	
	
	
}
