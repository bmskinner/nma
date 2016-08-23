/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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

package gui.components.panels;

import org.jfree.chart.JFreeChart;

import components.generic.SegmentedProfile;

@SuppressWarnings("serial")
public class SegmentationDualChartPanel extends DualChartPanel{
	
	public SegmentationDualChartPanel(){
		super();		
	}
	
	public void setProfile(SegmentedProfile profile, boolean normalised){
		chartPanel.setProfile(profile, normalised);
		this.updateChartPanelRange();
	}
	
	public void setCharts(JFreeChart chart, SegmentedProfile profile, boolean normalised, JFreeChart rangeChart){
		
		chartPanel.setChart(chart, profile, normalised);
		rangePanel.setChart(rangeChart);
		this.updateChartPanelRange();
	}
	

}
