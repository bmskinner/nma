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

import charting.charts.MorphologyChartFactory;
import charting.charts.panels.DraggableOverlayChartPanel;
import charting.options.ChartOptions;
import charting.options.DefaultChartOptions;
import charting.options.ChartOptionsBuilder;
import components.generic.ISegmentedProfile;
import components.generic.ProfileType;

public class SegmentationDualChartPanel extends DualChartPanel{
	
	public SegmentationDualChartPanel(){
		super();
		
		ChartOptions options = new ChartOptionsBuilder()
			.setProfileType(ProfileType.ANGLE)
			.setShowXAxis(false)
			.setShowYAxis(false)
			.build();
		
		JFreeChart profileChart  = new MorphologyChartFactory(options).makeEmptyChart();
		chartPanel = new DraggableOverlayChartPanel(profileChart, null, true);
		((DraggableOverlayChartPanel) chartPanel).addSignalChangeListener(this);
	}
	
	public void setProfile(ISegmentedProfile profile, boolean normalised){
		
		((DraggableOverlayChartPanel) chartPanel).setProfile(profile, normalised);
		this.updateChartPanelRange();
	}
	
	public void setCharts(JFreeChart chart, ISegmentedProfile profile, boolean normalised, JFreeChart rangeChart){
		
		((DraggableOverlayChartPanel) chartPanel).setChart(chart, profile, normalised);
		rangePanel.setChart(rangeChart);
		this.updateChartPanelRange();
	}
	

}
