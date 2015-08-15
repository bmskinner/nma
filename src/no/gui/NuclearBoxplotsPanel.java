package no.gui;

import ij.IJ;

import java.awt.Color;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import no.analysis.AnalysisDataset;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

import datasets.NucleusDatasetCreator;

public class NuclearBoxplotsPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	private ChartPanel areaBoxplotChartPanel;
	private ChartPanel perimBoxplotChartPanel;
	private ChartPanel maxFeretBoxplotChartPanel;
	private ChartPanel minFeretBoxplotChartPanel;
	private ChartPanel differenceBoxplotChartPanel;

	public NuclearBoxplotsPanel() {
		
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
			IJ.log("Error updating boxplots: "+e.getMessage());
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
	 */
	private void updateDifferenceBoxplot(List<AnalysisDataset> list){
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
