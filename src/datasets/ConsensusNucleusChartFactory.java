package datasets;

import ij.IJ;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.List;

import no.analysis.AnalysisDataset;
import no.collections.CellCollection;
import no.gui.ColourSelecter;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;

/**
 * Methods to make charts with a consensus nucleus
 */
public class ConsensusNucleusChartFactory {
	
	/**
	 * Create an empty chart as a placeholder for nucleus outlines
	 * and consensus chart panels
	 * @return
	 */
	public static JFreeChart makeEmptyNucleusOutlineChart(){
		return makeConsensusChart(null);
	}
	
	/**
	 * Craete a consensus chart from the given dataset. Gives an
	 * empty chart if null.
	 * @param ds
	 * @return
	 */
	private static JFreeChart makeConsensusChart(XYDataset ds){
		JFreeChart chart = null;
		if(ds==null){
			chart = ChartFactory.createXYLineChart(null,
					null, null, null);       

		} else {
			chart = 
					ChartFactory.createXYLineChart(null,
							null, null, ds, PlotOrientation.VERTICAL, true, true,
							false);
		}
		formatConsensusChart(chart);
		return chart;
	}
	
	/**
	 * Apply basic formatting to the chart; set the backgound colour,
	 * add the markers and set the ranges
	 * @param chart
	 */
	private static void formatConsensusChart(JFreeChart chart){
		chart.getPlot().setBackgroundPaint(Color.WHITE);
		chart.getXYPlot().getDomainAxis().setVisible(false);
		chart.getXYPlot().getRangeAxis().setVisible(false);
		chart.getXYPlot().addRangeMarker(new ValueMarker(0, Color.LIGHT_GRAY, new BasicStroke(1.0f)));
		chart.getXYPlot().addDomainMarker(new ValueMarker(0, Color.LIGHT_GRAY, new BasicStroke(1.0f)));
		
		int range = 50;
		chart.getXYPlot().getDomainAxis().setRange(-range,range);
		chart.getXYPlot().getRangeAxis().setRange(-range,range);
	}
		
	/**
	 * Create a consenusus chart for the given nucleus collection. This chart
	 * draws the nucleus border in black. There are no IQRs or segments.
	 * @param collection the NucleusCollection to draw the consensus from
	 * @return the consensus chart
	 */
	public static JFreeChart makeNucleusOutlineChart(AnalysisDataset dataset){
		CellCollection collection = dataset.getCollection();
		XYDataset ds = NucleusDatasetCreator.createBareNucleusOutline(dataset);
		JFreeChart chart = makeConsensusChart(ds);

		double max = getconsensusChartRange(dataset);

		XYPlot plot = chart.getXYPlot();
//		plot.setDataset(0, ds);
		plot.getDomainAxis().setRange(-max,max);
		plot.getRangeAxis().setRange(-max,max);

		int seriesCount = plot.getSeriesCount();

		for (int i = 0; i < seriesCount; i++) {
			plot.getRenderer().setSeriesVisibleInLegend(i, Boolean.FALSE);
			plot.getRenderer().setSeriesStroke(i, new BasicStroke(3));
			plot.getRenderer().setSeriesPaint(i, Color.BLACK);
		}	
		return chart;
	}
	
	
	/**
	 * Get the maximum absolute range of the axes of the chart
	 * @param dataset
	 * @return
	 */
	private static double getconsensusChartRange(AnalysisDataset dataset){
		CellCollection collection = dataset.getCollection();
		double maxX = Math.max( Math.abs(collection.getConsensusNucleus().getMinX()) , Math.abs(collection.getConsensusNucleus().getMaxX() ));
		double maxY = Math.max( Math.abs(collection.getConsensusNucleus().getMinY()) , Math.abs(collection.getConsensusNucleus().getMaxY() ));

		// ensure that the scales for each axis are the same
		double max = Math.max(maxX, maxY);

		// ensure there is room for expansion of the target nucleus due to IQR
		max *=  1.25;	
		return max;
	}
	
	/**
	 * Get the maximum absolute range of the axes of the chart
	 * @param list the datasets to test
	 * @return
	 */
	private static double getconsensusChartRange(List<AnalysisDataset> list){
		
		double max = 0;
		for (AnalysisDataset dataset : list){
			if(dataset.getCollection().hasConsensusNucleus()){
				double datasetMax = getconsensusChartRange(dataset);
				max = datasetMax > max ? datasetMax : max;
			}
		}
		return max;
	}
	
	/**
	 * Create a consensus nucleus chart with IQR and segments drawn on it
	 * @param dataset the dataset to draw
	 * @return
	 */
	public static JFreeChart makeSegmentedConsensusChart(AnalysisDataset dataset) throws Exception {
		XYDataset ds = null;
		
		CellCollection collection = dataset.getCollection();
		ds = NucleusDatasetCreator.createSegmentedNucleusOutline(collection);
			
		JFreeChart chart = makeConsensusChart(ds);
		double max = getconsensusChartRange(dataset);

		XYPlot plot = chart.getXYPlot();
		plot.setDataset(0, ds);
		plot.getDomainAxis().setRange(-max,max);
		plot.getRangeAxis().setRange(-max,max);
		
		formatConsensusChartSeries(plot, true);
		
		return chart;
	}
	
	/**
	 * Format the series colours for a consensus nucleus
	 * @param plot
	 */
	private static void formatConsensusChartSeries(XYPlot plot, boolean showIQR){
		
		XYDataset ds = plot.getDataset();
		int seriesCount = plot.getSeriesCount();

		for (int i = 0; i < seriesCount; i++) {
			plot.getRenderer().setSeriesVisibleInLegend(i, Boolean.FALSE);
			String name = (String) ds.getSeriesKey(i);
			
			// colour the segments
			if(name.startsWith("Seg_")){
//				int colourIndex = MorphologyChartFactory.getIndexFromLabel(name);
				plot.getRenderer().setSeriesStroke(i, new BasicStroke(2));
				plot.getRenderer().setSeriesPaint(i, Color.BLACK);
//				plot.getRenderer().setSeriesPaint(i, ColourSelecter.getSegmentColor(colourIndex));
			} 
			
			// colour the quartiles
			if(name.startsWith("Q")){
				
				// get the segment component
				// The dataset series name is Q25_Seg_1 etc
				String segmentName = name.replaceAll("Q[2|7]5_", "");
				int segIndex = MorphologyChartFactory.getIndexFromLabel(segmentName);
				
//				IJ.log("Drawing IQR for seg "+segmentName + " in "+name);
				
				if(showIQR){
					plot.getRenderer().setSeriesStroke(i, new BasicStroke(1));
//					plot.getRenderer().setSeriesPaint(i, Color.DARK_GRAY);
					Color colour = ColourSelecter.getSegmentColor(segIndex);
					plot.getRenderer().setSeriesPaint(i, colour);
					
				} else {
					plot.getRenderer().setSeriesVisible(i, false);
				}
			} 
		}	
		
	}
	

	/**
	 * Create a chart with multiple consensus nuclei from the given datasets
	 * @param list
	 * @return
	 * @throws Exception
	 */
	public static JFreeChart makeMultipleConsensusChart(List<AnalysisDataset> list) throws Exception {
		// multiple nuclei
		XYDataset ds = NucleusDatasetCreator.createMultiNucleusOutline(list);
		JFreeChart chart = makeConsensusChart(ds);
		
		formatConsensusChart(chart);
		
		XYPlot plot = chart.getXYPlot();
		
		double max = getconsensusChartRange(list);
		
		plot.getDomainAxis().setRange(-max,max);
		plot.getRangeAxis().setRange(-max,max);

		int seriesCount = plot.getSeriesCount();

		for (int i = 0; i < seriesCount; i++) {
			plot.getRenderer().setSeriesVisibleInLegend(i, false);
			String name = (String) ds.getSeriesKey(i);
			plot.getRenderer().setSeriesStroke(i, new BasicStroke(2));

			int index = MorphologyChartFactory.getIndexFromLabel(name);
			AnalysisDataset d = list.get(index);

			// in this context, segment colour refers to the entire
			// dataset colour (they use the same pallates in ColourSelecter)
			Color color = d.getDatasetColour() == null 
						? ColourSelecter.getSegmentColor(i)
						: d.getDatasetColour();

			// get the group id from the name, and make colour
			plot.getRenderer().setSeriesPaint(i, color);
			if(name.startsWith("Q")){
				// make the IQR distinct from the median
				plot.getRenderer().setSeriesPaint(i, color.darker());
			}

		}
		return chart;
	}

}
