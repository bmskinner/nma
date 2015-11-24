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

import gui.components.ColourSelecter.ColourSwatch;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import analysis.AnalysisDataset;

/*
 * Hold options for drawing a chart
 */
public abstract class ChartOptions {
	
	protected List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
	protected ColourSwatch swatch;
	protected Logger programLogger = null;
	
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
