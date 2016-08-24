/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
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
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package gui.tabs.cells;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import org.jfree.chart.JFreeChart;

import charting.charts.MorphologyChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import components.generic.BorderTagObject;
import components.generic.ProfileType;
import gui.DatasetEvent.DatasetMethod;
import gui.components.BorderTagEvent;
import gui.components.panels.BorderTagDualChartPanel;
import gui.components.panels.ProfileTypeOptionsPanel;
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

/**
 * Editing panel for the border tags of a single cell. 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class CellBorderTagPanel extends AbstractCellDetailPanel  {
		
		private BorderTagDualChartPanel dualPanel;
		
		private ProfileTypeOptionsPanel profileOptions  = new ProfileTypeOptionsPanel();
		
		private JPanel buttonsPanel;
		
		public CellBorderTagPanel(CellViewModel model) {
			super(model);
			
			this.setLayout(new BorderLayout());
			
			buttonsPanel = makeButtonPanel();
			this.add(buttonsPanel, BorderLayout.NORTH);
			setButtonsEnabled(false);
			
			dualPanel = new BorderTagDualChartPanel();
			dualPanel.addBorderTagEventListener(this);
			
			this.add(dualPanel, BorderLayout.CENTER);

			this.setBorder(null);

			
			setButtonsEnabled(false);
		}
		

		
		private JPanel makeButtonPanel(){
			
			JPanel panel = new JPanel(new FlowLayout()){
				@Override
				public void setEnabled(boolean b){
					super.setEnabled(b);
					for(Component c : this.getComponents()){
						c.setEnabled(b);
					}
				}
			};
			
			panel.add(profileOptions);
			profileOptions.addActionListener(  e -> update()   );
			
			
			return panel;
			
			
		}
		
		public void setButtonsEnabled(boolean b){
			profileOptions.setEnabled(b);
		}
		
		public void update(){

			try{
				
				ProfileType type = profileOptions.getSelected();

				if( ! this.getCellModel().hasCell()){
					JFreeChart chart = MorphologyChartFactory.getInstance().makeEmptyChart();
					
					dualPanel.setCharts(chart, chart);
					profileOptions.setEnabled(false);

				} else {
					
					ChartOptions options = new ChartOptionsBuilder()
						.setDatasets(getDatasets())
						.setCell(this.getCellModel().getCell())
						.setNormalised(false)
						.setAlignment(ProfileAlignment.LEFT)
						.setTag(BorderTagObject.REFERENCE_POINT)
						.setShowMarkers(true)
						.setProfileType( type)
						.setSwatch(activeDataset().getSwatch())
						.setShowAnnotations(false)
						.setShowPoints(true)
						.setTarget(dualPanel.getMainPanel())
						.build();
					
					setChart(options);		

					/*
					 * Create the chart for the range panel
					 */
					
					ChartOptions rangeOptions = new ChartOptionsBuilder()
						.setDatasets(getDatasets())
						.setCell(this.getCellModel().getCell())
						.setNormalised(false)
						.setAlignment(ProfileAlignment.LEFT)
						.setTag(BorderTagObject.REFERENCE_POINT)
						.setShowMarkers(true)
						.setProfileType( type)
						.setSwatch(activeDataset().getSwatch())
						.setShowPoints(false)
						.setShowAnnotations(false)
						.setTarget(dualPanel.getRangePanel())
						.build();
					
					setChart(rangeOptions);

					profileOptions.setEnabled(true);	
					

				}

			} catch(Exception e){
				error("Error updating cell panel", e);
				JFreeChart chart = MorphologyChartFactory.getInstance().makeEmptyChart();
				dualPanel.setCharts(chart, chart);
				profileOptions.setEnabled(false);
			}

		}
		
		@Override
		protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
			return MorphologyChartFactory.getInstance().makeIndividualNucleusProfileChart( options);
		}
		
		@Override
		protected void setBorderTagAction(BorderTagObject tag, int newTagIndex){

			if(tag==null){
				fine("Tag is null");
				return;
			}
			
			log("Updating nucleus "+tag+" to index "+newTagIndex);
			this.setAnalysing(true);

			boolean wasLocked = this.getCellModel().getCell().getNucleus().isLocked();
			this.getCellModel().getCell().getNucleus().setLocked(false);
			this.getCellModel().getCell().getNucleus().setBorderTag(BorderTagObject.REFERENCE_POINT, tag, newTagIndex);
			this.getCellModel().getCell().getNucleus().updateVerticallyRotatedNucleus();
			this.getCellModel().getCell().getNucleus().setLocked(wasLocked);

			if(tag.equals(BorderTagObject.REFERENCE_POINT)){
				// Update the profile aggregate to use the new RP
				activeDataset().getCollection().getProfileManager().createProfileCollections(true);
			}
			this.setAnalysing(false);
			this.refreshChartCache();
			this.fireDatasetEvent(DatasetMethod.REFRESH_CACHE, getDatasets());


		}
		
		@Override
		public void refreshChartCache(){
			clearChartCache();
			finest("Updating chart after clear");
			this.update();
		}
		
		@Override
		public void borderTagEventReceived(BorderTagEvent event) {
			if(event.getSource() instanceof JMenuItem){
				setBorderTagAction(event.getTag(), event.getIndex());
			}
		}

}
