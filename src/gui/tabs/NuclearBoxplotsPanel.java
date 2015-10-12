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
import gui.components.MeasurementUnitSettingsPanel.MeasurementScale;
import gui.components.SelectableChartPanel;
import ij.IJ;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import jdistlib.disttest.DistributionTest;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;

import utility.Constants;
import components.Cell;
import components.CellCollection;
import components.CellCollection.NucleusStatistic;
import components.nuclei.Nucleus;
import charting.charts.HistogramChartFactory;
import charting.datasets.NuclearHistogramDatasetCreator;
import charting.datasets.NucleusDatasetCreator;
import analysis.AnalysisDataset;

public class NuclearBoxplotsPanel extends DetailPanel {
	
	private static final long serialVersionUID = 1L;
	
	private BoxplotsPanel 	boxplotPanel;
	private HistogramsPanel histogramsPanel;
	
	private JTabbedPane 	tabPane;

	public NuclearBoxplotsPanel() {
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
		try {
			boxplotPanel.update(list);
			histogramsPanel.update(list);
		} catch (Exception e) {
			error("Error updating nuclear charts", e);
		}
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
			
			this.add(measurementUnitSettingsPanel, BorderLayout.NORTH);
			measurementUnitSettingsPanel.pixelsButton.addActionListener(this);
			measurementUnitSettingsPanel.micronsButton.addActionListener(this);
			
		}
		
		/**
		 * Update the boxplots with the selected options
		 * @param list
		 */
		public void update(List<AnalysisDataset> list){

			MeasurementScale scale  = measurementUnitSettingsPanel.pixelsButton.isSelected()
									? MeasurementScale.PIXELS
									: MeasurementScale.MICRONS;
			
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
            useDensityBox = new JCheckBox("Show probability density");
            useDensityBox.addActionListener(this);
            headerPanel.add(useDensityBox);
            headerPanel.add(measurementUnitSettingsPanel);
            measurementUnitSettingsPanel.pixelsButton.addActionListener(this);
			measurementUnitSettingsPanel.micronsButton.addActionListener(this);
    
            this.add(headerPanel, BorderLayout.NORTH);

			
			Dimension preferredSize = new Dimension(400, 150);
			for(NucleusStatistic stat : NucleusStatistic.values()){
//			for(String chartType : chartStatTypes.keySet()){
				SelectableChartPanel panel = new SelectableChartPanel(HistogramChartFactory.createNuclearStatsHistogram(null, null, stat), stat.toString());
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
			MeasurementScale scale  = measurementUnitSettingsPanel.pixelsButton.isSelected()
					? MeasurementScale.PIXELS
					: MeasurementScale.MICRONS;

			updateWithScale(list, scale);
		}
		
		public void updateWithScale(List<AnalysisDataset> list, MeasurementScale scale) throws Exception {
						
            boolean useDensity = useDensityBox.isSelected();

			
//			Set<String> chartTypes = chartStatTypes.keySet();

			for(NucleusStatistic stat : NucleusStatistic.values()){
//			for(String chartType : chartTypes){
				
				SelectableChartPanel panel = chartPanels.get(stat);
//				int stat = chartStatTypes.get(chartType);
				
				JFreeChart chart = null;
				
				if(useDensity){
//					log("Calculating density: "+chartType);
					DefaultXYDataset ds = NuclearHistogramDatasetCreator.createNuclearDensityHistogramDataset(list, stat, scale);
					chart = HistogramChartFactory.createNuclearDensityStatsChart(ds, list, stat);
					
				} else {
					HistogramDataset ds = NuclearHistogramDatasetCreator.createNuclearStatsHistogramDataset(list, stat, scale);
					chart = HistogramChartFactory.createNuclearStatsHistogram(ds, list, stat);
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
				
				double[] values;
				try {
//					values = NuclearHistogramDatasetCreator.findDatasetValues(dataset, stat);
//					Arrays.sort(values);
//					double[] result = DistributionTest.diptest_presorted(values);
//					
//					/*
//					 * an array of four elements: 
//					 * The first is the test statistic, 
//					 * the second is the p-value, 
//					 * followed by indices for which there are a dip. 
//					 * If there is no dip, the indices will be set to -1.
//					 */
//					IJ.log(dataset.getName()+": Test: "+result[0]);
//					IJ.log(dataset.getName()+": p   : "+result[1]);
//					
//					if(result[1] < Constants.FIVE_PERCENT_SIGNIFICANCE_LEVEL){
//						
//						for(int i=2; i<result.length; i++){
//								int index = (int) result[i];
//								ValueMarker marker = new ValueMarker(values[index], Color.BLACK, new BasicStroke(2.0f));
//								plot.addDomainMarker(marker);
//						}
//					}
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
//				IJ.log("Histo panel has heard a change");
				
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
						
						
//						int stat = chartStatTypes.get(name);
						
						List<AnalysisDataset> newList = new ArrayList<AnalysisDataset>();
						
						switch(stat){
							case AREA:
								for(Cell c : collection.getCells()){
									Nucleus n = c.getNucleus();
									if(n.getArea()>= lower && n.getArea()<= upper){
										subCollection.addCell(c);
									}
								}
								break;
								
							case PERIMETER:
								for(Cell c : collection.getCells()){
									Nucleus n = c.getNucleus();
									if(n.getPerimeter() >= lower && n.getPerimeter()<= upper){
										subCollection.addCell(c);
									}
								}
								break;
								
							case MAX_FERET:
								for(Cell c : collection.getCells()){
									Nucleus n = c.getNucleus();
									if(n.getFeret() >= lower && n.getFeret() <= upper){
										subCollection.addCell(c);
									}
								}
								break;
								
							case MIN_DIAMETER:
								for(Cell c : collection.getCells()){
									Nucleus n = c.getNucleus();
									if(n.getNarrowestDiameter() >= lower && n.getNarrowestDiameter() <= upper){
										subCollection.addCell(c);
									}
								}
								break;
								
							case VARIABILITY:
								
								try {
									for(Cell c : collection.getCells()){
										Nucleus n = c.getNucleus();
										
										double var = collection.calculateVariabililtyOfNucleusProfile(n);

										if(var >= lower && var <= upper){
											subCollection.addCell(c);
										}
									}
								} catch (Exception e){
									error("Cannot calculate variabililty", e);
								}
								break;
								
							case CIRCULARITY:
								for(Cell c : collection.getCells()){
									Nucleus n = c.getNucleus();
									if(n.getCircularity() >= lower && n.getCircularity() <= upper){
										subCollection.addCell(c);
									}
								}
								break;
								
							case ASPECT:
								for(Cell c : collection.getCells()){
									Nucleus n = c.getNucleus();
									if(n.getAspectRatio()>= lower && n.getAspectRatio() <= upper){
										subCollection.addCell(c);
									}
								}
								break;
						}
						
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
