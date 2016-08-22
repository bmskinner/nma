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
package gui.tabs.nuclear;

import gui.GlobalOptions;
import gui.SignalChangeEvent;
import gui.SignalChangeListener;
import gui.InterfaceEvent.InterfaceMethod;
import gui.components.HistogramsTabPanel;
import gui.components.panels.MeasurementUnitSettingsPanel;

import java.awt.Dimension;
import java.text.DecimalFormat;
import java.util.logging.Level;

import org.jfree.chart.JFreeChart;

import stats.NucleusStatistic;
import analysis.AnalysisDataset;
import charting.charts.SelectableChartPanel;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import components.CellCollection;
import components.generic.MeasurementScale;

@SuppressWarnings("serial")
public class NuclearHistogramsPanel extends HistogramsTabPanel implements SignalChangeListener {
		
		public NuclearHistogramsPanel(){
			super();

			try {

				MeasurementScale scale  = GlobalOptions.getInstance().getScale();
				Dimension preferredSize = new Dimension(400, 150);
				for(NucleusStatistic stat : NucleusStatistic.values()){
					
					ChartOptions options = new ChartOptionsBuilder()
						.addStatistic(stat)
						.setScale(scale)
						.setUseDensity(false)
						.build();
					
					JFreeChart chart = getChart(options);
					
					SelectableChartPanel panel = new SelectableChartPanel(chart, stat.toString());
					panel.setPreferredSize(preferredSize);
					panel.addSignalChangeListener(this);
					chartPanels.put(stat.toString(), panel);
					mainPanel.add(panel);

				}

			} catch(Exception e){
				log(Level.SEVERE, "Error creating histogram panel", e);
			}

		}
		
		protected void updateSingle() {
			updateMultiple();
		}
		
		protected void updateMultiple() {
			this.setEnabled(true);
//			MeasurementScale scale  = measurementUnitSettingsPanel.getSelected();
			boolean useDensity = useDensityPanel.isSelected();


			for(NucleusStatistic stat : NucleusStatistic.values()){
				SelectableChartPanel panel = chartPanels.get(stat.toString());

				ChartOptionsBuilder builder = new ChartOptionsBuilder();
				ChartOptions options = builder.setDatasets(getDatasets())
					.addStatistic(stat)
					.setScale(GlobalOptions.getInstance().getScale())
					.setSwatch(GlobalOptions.getInstance().getSwatch())
					.setUseDensity(useDensity)
					.build();
				
				JFreeChart chart = getChart(options);

				panel.setChart(chart);
			}
		}
		
		protected void updateNull() {
			this.setEnabled(false);
		}


		@Override
		public void signalChangeReceived(SignalChangeEvent event) {

			if(event.type().equals("MarkerPositionUpdated")){
				
				SelectableChartPanel panel = (SelectableChartPanel) event.getSource();
				filterByChartSelection(panel);
				
			}

		}

		private NucleusStatistic getPanelStatisticFromName(String name){
			NucleusStatistic stat = null;
			for (NucleusStatistic n : NucleusStatistic.values()){
				if(n.toString().equals(name)){
					stat = n;
				}
			}
			return stat;
		}
				
		/**
		 * Filter the selected populations based on the region outlined on a histogram panel
		 * @param panel
		 */
		public void filterByChartSelection(SelectableChartPanel panel){
			// check the scale to use for selection
			MeasurementScale scale  = GlobalOptions.getInstance().getScale();

			// get the parameters to filter on
			Double lower = panel.getGateLower();
			Double upper = panel.getGateUpper();
			DecimalFormat df = new DecimalFormat("#.##");

			// check the boxplot that fired
			NucleusStatistic stat = getPanelStatisticFromName(panel.getName());

			if(    !lower.isNaN() && !upper.isNaN()     ){
				
				// Make a dialog to ask if a filter should be performed
				int result = getFilterDialogResult(lower, upper);

				if(result==0){ // button at index 0 - continue
					
//					List<AnalysisDataset> newList = new ArrayList<AnalysisDataset>();

					// create a new sub-collection with the given parameters for each dataset
					for(AnalysisDataset dataset : getDatasets()){
						CellCollection collection = dataset.getCollection();
						try {
							
							log(Level.INFO, "Filtering on "
									+stat.toString()
									+": "
									+df.format(lower)
									+" - "+df.format(upper));
							
							CellCollection subCollection = collection.filterCollection(stat, scale, lower, upper);

							if(subCollection.hasCells()){

								log(Level.INFO, "Filtered "+subCollection.getNucleusCount()+" nuclei");
								dataset.addChildCollection(subCollection);
								try {
									dataset.getCollection().getProfileManager().copyCollectionOffsets(subCollection);
								} catch (Exception e1) {
									log(Level.SEVERE, "Error applying segments", e1);
								}
//								newList.add(  dataset.getChildDataset(subCollection.getID() ));
							}

						} catch (Exception e) {
							log(Level.SEVERE, "Error filtering", e);
							
						}
					}
					log(Level.FINEST, "Firing population update request");
					fireInterfaceEvent(InterfaceMethod.REFRESH_POPULATIONS);
//					fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
				}
			} 
		}
	}
