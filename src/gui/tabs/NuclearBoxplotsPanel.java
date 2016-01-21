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
package gui.tabs;

import gui.DatasetEvent.DatasetMethod;
import gui.SignalChangeEvent;
import gui.SignalChangeListener;
import gui.components.HistogramsTabPanel;
import gui.components.SelectableChartPanel;
import gui.components.panels.MeasurementUnitSettingsPanel;
import stats.NucleusStatistic;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;

import analysis.AnalysisDataset;
import charting.charts.BoxplotChartFactory;
import charting.charts.BoxplotChartOptions;
import charting.charts.HistogramChartFactory;
import charting.charts.HistogramChartOptions;
import components.CellCollection;
import components.generic.MeasurementScale;

public class NuclearBoxplotsPanel extends DetailPanel {
	
	private static final long serialVersionUID = 1L;
	
	private BoxplotsPanel 	boxplotPanel;
	private HistogramsPanel histogramsPanel;
	
	private JTabbedPane 	tabPane;

	public NuclearBoxplotsPanel(Logger programLogger) {
		super(programLogger);
		this.setLayout(new BorderLayout());
		tabPane = new JTabbedPane(JTabbedPane.TOP);
		
		boxplotPanel = new BoxplotsPanel();
		tabPane.addTab("Boxplots", boxplotPanel);
		
		histogramsPanel = new HistogramsPanel(programLogger);
		tabPane.addTab("Histograms", histogramsPanel);
		
		this.add(tabPane, BorderLayout.CENTER);
	}
	
	@Override
	protected void updateDetail(){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				try {

					boxplotPanel.update(getDatasets());
					programLogger.log(Level.FINEST, "Updated nuclear boxplots panel");
					histogramsPanel.update(getDatasets());
					programLogger.log(Level.FINEST, "Updated nuclear histograms panel");
				} catch (Exception e) {
					programLogger.log(Level.SEVERE, "Error updating nuclear charts", e);
					NuclearBoxplotsPanel.this.update( (List<AnalysisDataset>) null);
				} finally {
					setUpdating(false);
				}
			}
		});
	}
	
	protected class BoxplotsPanel extends JPanel implements ActionListener {

		private static final long serialVersionUID = 1L;
		
		private JPanel 		mainPanel; // hold the charts
		private JScrollPane scrollPane; // hold the main panel
		private Map<NucleusStatistic, ChartPanel> chartPanels = new HashMap<NucleusStatistic, ChartPanel>();
		private MeasurementUnitSettingsPanel measurementUnitSettingsPanel = new MeasurementUnitSettingsPanel();

		public BoxplotsPanel() {
			
			this.setLayout(new BorderLayout());
			
			mainPanel = new JPanel();
			mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
			
			Dimension preferredSize = new Dimension(200, 300);
			
			for(NucleusStatistic stat : NucleusStatistic.values()){
				
				BoxplotChartOptions options = new BoxplotChartOptions(getDatasets(), stat, MeasurementScale.PIXELS);
				JFreeChart chart = null;
				try {
					chart = BoxplotChartFactory.createNucleusStatisticBoxplot(options);
				} catch (Exception e) {
					programLogger.log(Level.SEVERE, "Error creating boxplots panel", e);
				}
				
				ChartPanel panel = new ChartPanel(chart);
				panel.setPreferredSize(preferredSize);
				chartPanels.put(stat, panel);
				mainPanel.add(panel);
				
			}
			
			// add the scroll pane to the tab
			scrollPane  = new JScrollPane(mainPanel);
			this.add(scrollPane, BorderLayout.CENTER);
			
			measurementUnitSettingsPanel.addActionListener(this);
			measurementUnitSettingsPanel.setEnabled(false);
			this.add(measurementUnitSettingsPanel, BorderLayout.NORTH);
			
		}
		
		/**
		 * Update the boxplots with the selected options
		 * @param list
		 */
		public void update(List<AnalysisDataset> list){

			try {
				
				if(hasDatasets()){
					measurementUnitSettingsPanel.setEnabled(true);
				} else {
					measurementUnitSettingsPanel.setEnabled(false);
				}
				
				MeasurementScale scale  = this.measurementUnitSettingsPanel.getSelected();

				for(NucleusStatistic stat : NucleusStatistic.values()){

					ChartPanel panel = chartPanels.get(stat);

					JFreeChart chart = null;
					BoxplotChartOptions options = new BoxplotChartOptions(list, stat, scale);
					
					if(getChartCache().hasChart(options)){
						programLogger.log(Level.FINEST, "Using cached boxplot chart: "+stat.toString());
						chart = getChartCache().getChart(options);

					} else { // No cache

						chart = BoxplotChartFactory.createNucleusStatisticBoxplot(options);
						getChartCache().addChart(options, chart);
						programLogger.log(Level.FINEST, "Added cached boxplot chart: "+stat.toString());
					}

					panel.setChart(chart);
				}

			} catch (Exception e) {
				programLogger.log(Level.SEVERE, "Error updating boxplots", e);
			}
			
		}
						
		@Override
		public void actionPerformed(ActionEvent e) {
			
			update(getDatasets());
			
		}
		
	}
	
	@SuppressWarnings("serial")
	protected class HistogramsPanel extends HistogramsTabPanel implements SignalChangeListener {
		
		public HistogramsPanel(Logger programLogger){
			super(programLogger);

			try {

				MeasurementScale scale  = this.measurementUnitSettingsPanel.getSelected();
				Dimension preferredSize = new Dimension(400, 150);
				for(NucleusStatistic stat : NucleusStatistic.values()){

					HistogramChartOptions options = new HistogramChartOptions(null, stat, scale, false);
					SelectableChartPanel panel = new SelectableChartPanel(HistogramChartFactory.createNuclearStatsHistogram(options), stat.toString());
					panel.setPreferredSize(preferredSize);
					panel.addSignalChangeListener(this);
					HistogramsPanel.this.chartPanels.put(stat.toString(), panel);
					HistogramsPanel.this.mainPanel.add(panel);

				}

			} catch(Exception e){
				programLogger.log(Level.SEVERE, "Error creating histogram panel", e);
			}

		}

		@Override
		public void updateDetail() {
			
			
			if(hasDatasets()){
				this.setEnabled(true);
			} else {
				this.setEnabled(false);
			}

			MeasurementScale scale  = HistogramsPanel.this.measurementUnitSettingsPanel.getSelected();
			boolean useDensity = HistogramsPanel.this.useDensityPanel.isSelected();

			try{
				for(NucleusStatistic stat : NucleusStatistic.values()){
					SelectableChartPanel panel = HistogramsPanel.this.chartPanels.get(stat.toString());

					JFreeChart chart = null;
					HistogramChartOptions options = new HistogramChartOptions(getDatasets(), stat, scale, useDensity);
					options.setLogger(programLogger);

					if(this.getChartCache().hasChart(options)){
						programLogger.log(Level.FINEST, "Using cached histogram: "+stat.toString());
						chart = HistogramsPanel.this.getChartCache().getChart(options);

					} else { // No cache


						if(useDensity){
							chart = HistogramChartFactory.createNuclearDensityStatsChart(options);
							HistogramsPanel.this.getChartCache().addChart(options, chart);

						} else {
							chart = HistogramChartFactory.createNuclearStatsHistogram(options);
							HistogramsPanel.this.getChartCache().addChart(options, chart);

						}
						programLogger.log(Level.FINEST, "Added cached histogram chart: "+stat);
					}

					XYPlot plot = (XYPlot) chart.getPlot();
					plot.setDomainPannable(true);
					plot.setRangePannable(true);

					panel.setChart(chart);
				}
			} catch(Exception e){
				programLogger.log(Level.SEVERE, "Error updating histogram panel", e);
			} finally {
				HistogramsPanel.this.setUpdating(false);
			}
		}
				
		private void detectModes(JFreeChart chart, List<AnalysisDataset> list, int stat){
			
			XYPlot plot = chart.getXYPlot();
			
			
			for(AnalysisDataset dataset : list){
				
//				double[] values;
				try {
//					
				} catch (Exception e) {
					programLogger.log(Level.SEVERE, "Unable to detect modes", e);
				}
				
			}
			
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
			MeasurementScale scale  = this.measurementUnitSettingsPanel.getSelected();

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
					
					List<AnalysisDataset> newList = new ArrayList<AnalysisDataset>();

					// create a new sub-collection with the given parameters for each dataset
					for(AnalysisDataset dataset : getDatasets()){
						CellCollection collection = dataset.getCollection();
						try {
							
							programLogger.log(Level.INFO, "Filtering on "
									+stat.toString()
									+": "
									+df.format(lower)
									+" - "+df.format(upper));
							
							CellCollection subCollection = collection.filterCollection(stat, scale, lower, upper);

							if(subCollection.hasCells()){

								programLogger.log(Level.INFO, "Filtered "+subCollection.getNucleusCount()+" nuclei");
								dataset.addChildCollection(subCollection);
								newList.add(  dataset.getChildDataset(subCollection.getID() ));
							}

						} catch (Exception e) {
							programLogger.log(Level.SEVERE, "Error filtering", e);
							
						}
					}
					NuclearBoxplotsPanel.this.fireDatasetEvent(DatasetMethod.NEW_MORPHOLOGY, newList);
				}
			} 
		}
	}

}
