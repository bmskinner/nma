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
package gui.tabs;

import gui.DatasetEvent.DatasetMethod;
import gui.SignalChangeEvent;
import gui.SignalChangeListener;
import gui.components.ColourSelecter;
import gui.components.MeasurementUnitSettingsPanel;
import gui.components.SelectableChartPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;

import analysis.AnalysisDataset;
import charting.charts.HistogramChartFactory;
import charting.datasets.NuclearHistogramDatasetCreator;
import charting.datasets.NucleusDatasetCreator;

import components.Cell;
import components.CellCollection;
import components.generic.MeasurementScale;
import components.nuclear.NucleusStatistic;
import components.nuclei.Nucleus;

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
		
		histogramsPanel = new HistogramsPanel();
		tabPane.addTab("Histograms", histogramsPanel);
		
		this.add(tabPane, BorderLayout.CENTER);
	}

	public void update(List<AnalysisDataset> list){	
		this.list = list;
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				try {
					programLogger.log(Level.FINEST, "Updating nuclear boxplots panel");
					boxplotPanel.update(NuclearBoxplotsPanel.this.list);
					programLogger.log(Level.FINEST, "Updated nuclear boxplots panel");
					histogramsPanel.update(NuclearBoxplotsPanel.this.list);
					programLogger.log(Level.FINEST, "Updated nuclear histograms panel");
				} catch (Exception e) {
					error("Error updating nuclear charts", e);
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
				
				JFreeChart boxplot = ChartFactory.createBoxAndWhiskerChart(	null, 
						null, 
						null, 
						new DefaultBoxAndWhiskerCategoryDataset(), 
						false);	
				formatBoxplotChart(boxplot);
				
				ChartPanel panel = new ChartPanel(boxplot);
				panel.setPreferredSize(preferredSize);
				chartPanels.put(stat, panel);
				mainPanel.add(panel);
				
			}
			
			// add the scroll pane to the tab
			scrollPane  = new JScrollPane(mainPanel);
			this.add(scrollPane, BorderLayout.CENTER);
			
			measurementUnitSettingsPanel.addActionListener(this);
			this.add(measurementUnitSettingsPanel, BorderLayout.NORTH);
			
		}
		
		/**
		 * Update the boxplots with the selected options
		 * @param list
		 */
		public void update(List<AnalysisDataset> list){

			MeasurementScale scale  = this.measurementUnitSettingsPanel.getSelected();
			
			updateWithScale(list, scale);
			
		}
		
		/**
		 * Update with the given measurement scale
		 * @param list
		 * @param scale
		 */
		private void updateWithScale(List<AnalysisDataset> list, MeasurementScale scale){
			try {

				for(NucleusStatistic stat : NucleusStatistic.values()){

					ChartPanel panel = chartPanels.get(stat);
					BoxAndWhiskerCategoryDataset ds = NucleusDatasetCreator.createBoxplotDataset(list, stat, scale);
					String yLabel = scale.yLabel(stat);

					JFreeChart boxplotChart = ChartFactory.createBoxAndWhiskerChart(null, null, yLabel, ds, false); 
					formatBoxplotChart(boxplotChart, list);

					panel.setChart(boxplotChart);
				}

			} catch (Exception e) {
				error("Error updating boxplots", e);
			}
		}
				
		/**
		 * Apply the default formatting to a boxplot with list
		 * @param boxplot
		 */
		private void formatBoxplotChart(JFreeChart boxplot, List<AnalysisDataset> list){
			formatBoxplotChart(boxplot);
			CategoryPlot plot = boxplot.getCategoryPlot();
			BoxAndWhiskerRenderer renderer = (BoxAndWhiskerRenderer) plot.getRenderer();
			
			for(int i=0;i<plot.getDataset().getRowCount();i++){
				
				AnalysisDataset d = list.get(i);

				Color color = d.getDatasetColour() == null 
							? ColourSelecter.getSegmentColor(i)
							: d.getDatasetColour();
							
							renderer.setSeriesPaint(i, color);
			}
			renderer.setMeanVisible(false);
		}
		
		/**
		 * Apply basic formatting to the charts, without any series added
		 * @param boxplot
		 */
		private void formatBoxplotChart(JFreeChart boxplot){
			CategoryPlot plot = boxplot.getCategoryPlot();
			plot.setBackgroundPaint(Color.WHITE);
			BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
			plot.setRenderer(renderer);
			renderer.setUseOutlinePaintForWhiskers(true);   
			renderer.setBaseOutlinePaint(Color.BLACK);
			renderer.setBaseFillPaint(Color.LIGHT_GRAY);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			
			update(list);
			
		}
		
	}
	
	protected class HistogramsPanel extends JPanel implements ActionListener, SignalChangeListener {
		
		private static final long serialVersionUID = 1L;
		
		private Map<NucleusStatistic, SelectableChartPanel> chartPanels = new HashMap<NucleusStatistic, SelectableChartPanel>();

//		private Map<String, Integer> chartStatTypes = new HashMap<String, Integer>();

		private JPanel 		mainPanel; // hold the charts
        private JPanel         headerPanel; // hold buttons
        private JCheckBox    useDensityBox; 
        private MeasurementUnitSettingsPanel measurementUnitSettingsPanel = new MeasurementUnitSettingsPanel();

		private JScrollPane scrollPane; // hold the main panel

		public HistogramsPanel(){
			this.setLayout(new BorderLayout());
						
			mainPanel = new JPanel();
			mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
			
            headerPanel = new JPanel(new FlowLayout());
            useDensityBox = new JCheckBox("Probability density function");
            useDensityBox.addActionListener(this);
            headerPanel.add(useDensityBox);
            headerPanel.add(measurementUnitSettingsPanel);
            measurementUnitSettingsPanel.addActionListener(this);
            
    
            this.add(headerPanel, BorderLayout.NORTH);

            MeasurementScale scale  = this.measurementUnitSettingsPanel.getSelected();
			Dimension preferredSize = new Dimension(400, 150);
			for(NucleusStatistic stat : NucleusStatistic.values()){
//			for(String chartType : chartStatTypes.keySet()){
				SelectableChartPanel panel = new SelectableChartPanel(HistogramChartFactory.createNuclearStatsHistogram(null, null, stat, scale), stat.toString());
				panel.setPreferredSize(preferredSize);
				panel.addSignalChangeListener(this);
				chartPanels.put(stat, panel);
				mainPanel.add(panel);
				
			}
			
			// add the scroll pane to the tab
			scrollPane  = new JScrollPane(mainPanel);
			this.add(scrollPane, BorderLayout.CENTER);
			
		}
		
		public void update(List<AnalysisDataset> list) throws Exception {
			MeasurementScale scale  = this.measurementUnitSettingsPanel.getSelected();
			updateWithScale(list, scale);
		}
		
		public void updateWithScale(List<AnalysisDataset> list, MeasurementScale scale) throws Exception {
						
            boolean useDensity = useDensityBox.isSelected();


			for(NucleusStatistic stat : NucleusStatistic.values()){
				
				SelectableChartPanel panel = chartPanels.get(stat);
				
				JFreeChart chart = null;
				
				if(useDensity){
					DefaultXYDataset ds = NuclearHistogramDatasetCreator.createNuclearDensityHistogramDataset(list, stat, scale);
					chart = HistogramChartFactory.createNuclearDensityStatsChart(ds, list, stat, scale);
					
				} else {
					HistogramDataset ds = NuclearHistogramDatasetCreator.createNuclearStatsHistogramDataset(list, stat, scale);
					chart = HistogramChartFactory.createNuclearStatsHistogram(ds, list, stat, scale);
				}
//				detectModes(chart, list, stat);
				XYPlot plot = (XYPlot) chart.getPlot();
		        plot.setDomainPannable(true);
		        plot.setRangePannable(true);

				panel.setChart(chart);
			}
		}
				
		private void detectModes(JFreeChart chart, List<AnalysisDataset> list, int stat){
			
			XYPlot plot = chart.getXYPlot();
			
			
			for(AnalysisDataset dataset : list){
				
//				double[] values;
				try {
//					
				} catch (Exception e) {
					error("Unable to detect modes", e);
				}
				
			}
			
		}
		
        @Override
        public void actionPerformed(ActionEvent e) {

            try {
                this.update(list);
            } catch (Exception e1) {
                error("Error updating histogram panel from action listener", e1);
            }
            
            
        }

		@Override
		public void signalChangeReceived(SignalChangeEvent event) {

			if(event.type().equals("MarkerPositionUpdated")){
//				
				// check the scale to use for selection
				MeasurementScale scale  = this.measurementUnitSettingsPanel.getSelected();
				
				// get the parameters to filter on
				SelectableChartPanel panel = (SelectableChartPanel) event.getSource();
				Double lower = panel.getGateLower();
				Double upper = panel.getGateUpper();
				
				// check the boxplot that fired
				String name = panel.getName();
				NucleusStatistic stat = null;
				for (NucleusStatistic n : NucleusStatistic.values()){
					if(n.toString().equals(name)){
						stat = n;
					}
				}
				
				DecimalFormat df = new DecimalFormat("#.##");
				
				if( !(lower.isNaN() && upper.isNaN())  ){
					
					// create a new sub-collection with the given parameters for each dataset
					for(AnalysisDataset dataset : list){
						CellCollection collection = dataset.getCollection();
						CellCollection subCollection = new CellCollection(dataset, "Filtered_"+name+"_"+df.format(lower)+"-"+df.format(upper));
						
						
						for(Cell c : collection.getCells()){
							Nucleus n = c.getNucleus();
							double value = n.getStatistic(stat, scale);
							
							// variability must be calculated from the collection, not the nucleus
							if(stat.equals(NucleusStatistic.VARIABILITY)){
								try{ 
									value = collection.calculateVariabililtyOfNucleusProfile(n);
								} catch (Exception e){
									error("Cannot calculate variabililty", e);
								}
							}
							
							if(value>= lower && value<= upper){
								subCollection.addCell(c);
							}
						}
						
						List<AnalysisDataset> newList = new ArrayList<AnalysisDataset>();
												
						if(subCollection.getNucleusCount()>0){
							
							
							log("Filtering on "+name+": "+df.format(lower)+" - "+df.format(upper));
							log("Filtered "+subCollection.getNucleusCount()+" nuclei");
							dataset.addChildCollection(subCollection);
							newList.add(  dataset.getChildDataset(subCollection.getID() ));
						}
						fireDatasetEvent(DatasetMethod.NEW_MORPHOLOGY, newList);
						
					}
				} else {
					log("Error: "+name+": "+df.format(lower)+" - "+df.format(upper));
				}
				
			}
			
		}

		
	}

}
