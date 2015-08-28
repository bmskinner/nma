package no.gui;

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

import no.analysis.AnalysisDataset;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;

import datasets.HistogramChartFactory;
import datasets.NucleusDatasetCreator;

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
		private ChartPanel areaBoxplotChartPanel;
		private ChartPanel perimBoxplotChartPanel;
		private ChartPanel maxFeretBoxplotChartPanel;
		private ChartPanel minFeretBoxplotChartPanel;
		private ChartPanel differenceBoxplotChartPanel;

		public BoxplotsPanel() {
			
			this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			
			JFreeChart areaBoxplot = ChartFactory.createBoxAndWhiskerChart(	null, 
																			null, 
																			null, 
																			new DefaultBoxAndWhiskerCategoryDataset(), 
																			false);	
			formatBoxplotChart(areaBoxplot);
			
			JFreeChart perimBoxplot = ChartFactory.createBoxAndWhiskerChart(null, null, null, new DefaultBoxAndWhiskerCategoryDataset(), false);	        
			formatBoxplotChart(perimBoxplot);
			
			JFreeChart maxFeretBoxplot = ChartFactory.createBoxAndWhiskerChart(null, null, null, new DefaultBoxAndWhiskerCategoryDataset(), false);	        
			formatBoxplotChart(maxFeretBoxplot);
			
			JFreeChart minFeretBoxplot = ChartFactory.createBoxAndWhiskerChart(null, null, null, new DefaultBoxAndWhiskerCategoryDataset(), false);	        
			formatBoxplotChart(minFeretBoxplot);
			
			JFreeChart differenceBoxplot = ChartFactory.createBoxAndWhiskerChart(null, null, null, new DefaultBoxAndWhiskerCategoryDataset(), false);	
			formatBoxplotChart(differenceBoxplot);
			
			areaBoxplotChartPanel = new ChartPanel(areaBoxplot);
			this.add(areaBoxplotChartPanel);
			
			perimBoxplotChartPanel = new ChartPanel(perimBoxplot);
			this.add(perimBoxplotChartPanel);
			
			maxFeretBoxplotChartPanel = new ChartPanel(maxFeretBoxplot);
			this.add(maxFeretBoxplotChartPanel);
			
			minFeretBoxplotChartPanel = new ChartPanel(minFeretBoxplot);
			this.add(minFeretBoxplotChartPanel);
			
			differenceBoxplotChartPanel = new ChartPanel(differenceBoxplot);
			this.add(differenceBoxplotChartPanel);
			
		}
		
		public void update(List<AnalysisDataset> list){
			
			try {
				updateAreaBoxplot(list);
				updatePerimBoxplot(list);
				updateMaxFeretBoxplot(list);
				updateMinFeretBoxplot(list);
				updateDifferenceBoxplot(list);
			} catch (Exception e) {
				error("Error updating boxplots", e);
			}
		}
		
		/**
		 * Update the boxplot panel for areas with a list of NucleusCollections
		 * @param list
		 */
		private void updateAreaBoxplot(List<AnalysisDataset> list){
			BoxAndWhiskerCategoryDataset ds = NucleusDatasetCreator.createAreaBoxplotDataset(list);
			JFreeChart boxplotChart = ChartFactory.createBoxAndWhiskerChart(null, null, "Pixels", ds, false); 
			formatBoxplotChart(boxplotChart, list);
			areaBoxplotChartPanel.setChart(boxplotChart);
		}
		
		/**
		 * Update the boxplot panel for perimeters with a list of NucleusCollections
		 * @param list
		 */
		private void updatePerimBoxplot(List<AnalysisDataset> list){
			BoxAndWhiskerCategoryDataset ds = NucleusDatasetCreator.createPerimBoxplotDataset(list);
			JFreeChart boxplotChart = ChartFactory.createBoxAndWhiskerChart(null, null, "Pixels", ds, false); 
			formatBoxplotChart(boxplotChart, list);
			perimBoxplotChartPanel.setChart(boxplotChart);
		}
		
		/**
		 * Update the boxplot panel for longest diameter across CoM with a list of NucleusCollections
		 * @param list
		 */
		private void updateMaxFeretBoxplot(List<AnalysisDataset> list){
			BoxAndWhiskerCategoryDataset ds = NucleusDatasetCreator.createMaxFeretBoxplotDataset(list);
			JFreeChart boxplotChart = ChartFactory.createBoxAndWhiskerChart(null, null, "Pixels", ds, false); 
			formatBoxplotChart(boxplotChart, list);
			maxFeretBoxplotChartPanel.setChart(boxplotChart);
		}

		/**
		 * Update the boxplot panel for shortest diameter across CoM with a list of NucleusCollections
		 * @param list
		 */
		private void updateMinFeretBoxplot(List<AnalysisDataset> list){
			BoxAndWhiskerCategoryDataset ds = NucleusDatasetCreator.createMinFeretBoxplotDataset(list);
			JFreeChart boxplotChart = ChartFactory.createBoxAndWhiskerChart(null, null, "Pixels", ds, false); 
			formatBoxplotChart(boxplotChart, list);
			minFeretBoxplotChartPanel.setChart(boxplotChart);
		}
		
		/**
		 * Update the boxplot panel for shortest diameter across CoM with a list of NucleusCollections
		 * @param list
		 * @throws Exception 
		 */
		private void updateDifferenceBoxplot(List<AnalysisDataset> list) throws Exception{
			BoxAndWhiskerCategoryDataset ds = NucleusDatasetCreator.createDifferenceBoxplotDataset(list);
			JFreeChart boxplotChart = ChartFactory.createBoxAndWhiskerChart(null, null, "Degrees per perimeter unit", ds, false); 
			formatBoxplotChart(boxplotChart, list);
			differenceBoxplotChartPanel.setChart(boxplotChart);
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
			
			this.setLayout(new BorderLayout());
						
			mainPanel = new JPanel();
			mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
			
			Dimension preferredSize = new Dimension(400, 100);
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
					ds = NucleusDatasetCreator.createNuclearAreaHistogramDataset(list);
				}
				
				if(chartType.equals("Perimeter")){
					ds = NucleusDatasetCreator.createNuclearPerimeterHistogramDataset(list);
				}
				
				if(chartType.equals("Max feret")){
					ds = NucleusDatasetCreator.createNuclearMaxFeretHistogramDataset(list);
				}
				
				if(chartType.equals("Min diameter")){
					ds = NucleusDatasetCreator.createNuclearMinDiameterHistogramDataset(list);
				}
				
				if(chartType.equals("Variability")){
					ds = NucleusDatasetCreator.createNuclearVariabilityHistogramDataset(list);
				}

				JFreeChart chart = HistogramChartFactory.createNuclearStatsHistogram(ds, list, chartType);
				panel.setChart(chart);
			}
		}
		
	}

}
