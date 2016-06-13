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
package gui.tabs.profiles;

import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import charting.charts.MorphologyChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import charting.options.TableOptions;
import components.generic.BorderTag;
import components.generic.ProfileType;

@SuppressWarnings("serial")
public class ProfileDisplayPanel extends AbstractProfileDisplayPanel {
	
	private ProfileType type;
		
		public ProfileDisplayPanel(ProfileType type){
			super();
			this.type = type;
			
			JFreeChart chart = MorphologyChartFactory.makeEmptyProfileChart(type);
			chartPanel.setChart(chart);
			
			if(this.type==ProfileType.FRANKEN){
				this.profileAlignmentOptionsPanel.setEnabled(false);
			}
		}
		
		@Override
		protected void updateSingle() {
			super.updateSingle();
			updateChart();
			
		}
		
		@Override
		protected void updateMultiple() {
			super.updateMultiple();
			updateChart();
		}
		
		@Override
		protected void updateNull() {
			super.updateNull();
			JFreeChart chart = MorphologyChartFactory.makeEmptyProfileChart(type);
			chartPanel.setChart(chart);

		}
		
		@Override
		protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
			return MorphologyChartFactory.createProfileChart( options );
		}
		
		@Override
		protected TableModel createPanelTableType(TableOptions options) throws Exception{
			return null;
		}
		
		private void updateChart() {
			ChartOptions options = makeOptions();
			JFreeChart   chart   = getChart(options);
			chartPanel.setChart(chart);			
		}
		
		private ChartOptions makeOptions(){

			boolean normalised         = profileAlignmentOptionsPanel.isNormalised();
			ProfileAlignment alignment = normalised ?  ProfileAlignment.LEFT : profileAlignmentOptionsPanel.getSelected();
			BorderTag tag              = borderTagOptionsPanel.getSelected();
			boolean showMarkers        = profileMarkersOptionsPanel.showMarkers();
			boolean hideProfiles       = profileMarkersOptionsPanel.isHideProfiles();
			
			ChartOptions options = new ChartOptionsBuilder()
				.setDatasets(getDatasets())
				.setNormalised(normalised)
				.setAlignment(alignment)
				.setTag(tag)
				.setShowMarkers(showMarkers)
				.setHideProfiles(hideProfiles)
				.setProfileType(type)
				.build();
			return options;
		}
}
		