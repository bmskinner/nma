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

import gui.components.ColourSelecter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;

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
		try {
			boxplotPanel.update(list);
			histogramsPanel.update(list);
		} catch (Exception e) {
			error("Error updating nuclear charts", e);
		}
	}
	
	protected class BoxplotsPanel extends JPanel {

		private static final long serialVersionUID = 1L;
		
		private JPanel 		mainPanel; // hold the charts
		private JScrollPane scrollPane; // hold the main panel
		private List<String> chartTypes = new ArrayList<String>();
		private Map<String, ChartPanel> chartPanels = new HashMap<String, ChartPanel>();

		public BoxplotsPanel() {
			
			chartTypes.add("Area");
			chartTypes.add("Perimeter");
			chartTypes.add("Max feret");
			chartTypes.add("Min diameter");
			chartTypes.add("Variability");
			chartTypes.add("Circularity");
			chartTypes.add("Aspect");
//			this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			
			this.setLayout(new BorderLayout());
			
			mainPanel = new JPanel();
			mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
			
			Dimension preferredSize = new Dimension(200, 300);
			for(String chartType : chartTypes){
				
				JFreeChart boxplot = ChartFactory.createBoxAndWhiskerChart(	null, 
						null, 
						null, 
						new DefaultBoxAndWhiskerCategoryDataset(), 
						false);	
				formatBoxplotChart(boxplot);
				
				ChartPanel panel = new ChartPanel(boxplot);
				panel.setPreferredSize(preferredSize);
				chartPanels.put(chartType, panel);
				mainPanel.add(panel);
				
			}
			
			// add the scroll pane to the tab
			scrollPane  = new JScrollPane(mainPanel);
			this.add(scrollPane, BorderLayout.CENTER);
			
		}
		
		public void update(List<AnalysisDataset> list){

			try {
				for(String chartType : chartTypes){

					ChartPanel panel = chartPanels.get(chartType);
					BoxAndWhiskerCategoryDataset ds = null;
					String yLabel = "Pixels";

					if(chartType.equals("Area")){
						ds = NucleusDatasetCreator.createAreaBoxplotDataset(list);
					}

					if(chartType.equals("Perimeter")){
						ds = NucleusDatasetCreator.createPerimBoxplotDataset(list);
					}

					if(chartType.equals("Max feret")){
						ds = NucleusDatasetCreator.createMaxFeretBoxplotDataset(list);
					}

					if(chartType.equals("Min diameter")){
						ds = NucleusDatasetCreator.createMinFeretBoxplotDataset(list);
					}

					if(chartType.equals("Variability")){
						ds = NucleusDatasetCreator.createDifferenceBoxplotDataset(list);
						yLabel = "Degrees per perimeter unit";
					}
					
					if(chartType.equals("Circularity")){
						ds = NucleusDatasetCreator.createCircularityBoxplotDataset(list);
						yLabel = "Circularity";
					}
					
					if(chartType.equals("Aspect")){
						ds = NucleusDatasetCreator.createAspectBoxplotDataset(list);
						yLabel = "Aspect ratio (feret / min diameter)";
					}

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
		
	}
	
	protected class HistogramsPanel extends JPanel {
		
		private static final long serialVersionUID = 1L;
		
		private Map<String, ChartPanel> chartPanels = new HashMap<String, ChartPanel>();
				
		private List<String> chartTypes = new ArrayList<String>();

		private JPanel 		mainPanel; // hold the charts
		
		private JScrollPane scrollPane; // hold the main panel

		public HistogramsPanel(){
			
			chartTypes.add("Area");
			chartTypes.add("Perimeter");
			chartTypes.add("Max feret");
			chartTypes.add("Min diameter");
			chartTypes.add("Variability");
			chartTypes.add("Circularity");
			chartTypes.add("Aspect");
			
			this.setLayout(new BorderLayout());
						
			mainPanel = new JPanel();
			mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
			
			Dimension preferredSize = new Dimension(400, 150);
			for(String chartType : chartTypes){
				ChartPanel panel = new ChartPanel(HistogramChartFactory.createNuclearStatsHistogram(null, null, chartType));
				panel.setPreferredSize(preferredSize);
				chartPanels.put(chartType, panel);
				mainPanel.add(panel);
				
			}
			
			// add the scroll pane to the tab
			scrollPane  = new JScrollPane(mainPanel);
			this.add(scrollPane, BorderLayout.CENTER);
			
		}
		
		public void update(List<AnalysisDataset> list) throws Exception {
			
			for(String chartType : chartTypes){
				
				ChartPanel panel = chartPanels.get(chartType);
				HistogramDataset ds = null;
				
				if(chartType.equals("Area")){
					ds = NuclearHistogramDatasetCreator.createNuclearAreaHistogramDataset(list);
				}
				
				if(chartType.equals("Perimeter")){
					ds = NuclearHistogramDatasetCreator.createNuclearPerimeterHistogramDataset(list);
				}
				
				if(chartType.equals("Max feret")){
					ds = NuclearHistogramDatasetCreator.createNuclearMaxFeretHistogramDataset(list);
				}
				
				if(chartType.equals("Min diameter")){
					ds = NuclearHistogramDatasetCreator.createNuclearMinDiameterHistogramDataset(list);
				}
				
				if(chartType.equals("Variability")){
					ds = NuclearHistogramDatasetCreator.createNuclearVariabilityHistogramDataset(list);
				}
				
				if(chartType.equals("Circularity")){
					ds = NuclearHistogramDatasetCreator.createNuclearCircularityHistogramDataset(list);
				}
				
				if(chartType.equals("Aspect")){
					ds = NuclearHistogramDatasetCreator.createNuclearAspectRatioHistogramDataset(list);
				}

				JFreeChart chart = HistogramChartFactory.createNuclearStatsHistogram(ds, list, chartType);
				panel.setChart(chart);
			}
		}
		
	}

}
