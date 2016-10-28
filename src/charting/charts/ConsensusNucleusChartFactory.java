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
package charting.charts;

import gui.GlobalOptions;
import gui.components.ColourSelecter;
import gui.components.ColourSelecter.ColourSwatch;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;

import analysis.IAnalysisDataset;
import analysis.mesh.NucleusMesh;
import charting.ChartComponents;
import charting.datasets.NucleusDatasetCreator;
import charting.options.DefaultChartOptions;
import components.CellCollection;
import components.ICellCollection;

/**
 * Methods to make charts with a consensus nucleus
 */
public class ConsensusNucleusChartFactory extends AbstractChartFactory {
	
	public ConsensusNucleusChartFactory(DefaultChartOptions o){
		super(o);
	}
	
	/**
	 * Create an empty chart as a placeholder for nucleus outlines
	 * and consensus chart panels
	 * @return
	 */
	public static JFreeChart makeEmptyChart(){
		
		JFreeChart chart = ChartFactory.createXYLineChart(null,
						null, null, null, PlotOrientation.VERTICAL, true, true,
						false);
		
		chart.getPlot().setBackgroundPaint(Color.WHITE);
		chart.getXYPlot().getDomainAxis().setVisible(false);
		chart.getXYPlot().getRangeAxis().setVisible(false);
		chart.getXYPlot().addRangeMarker(ChartComponents.CONSENSUS_ZERO_MARKER);
		chart.getXYPlot().addDomainMarker(ChartComponents.CONSENSUS_ZERO_MARKER);
		
		int range = DEFAULT_EMPTY_RANGE;
		chart.getXYPlot().getDomainAxis().setRange(-range, range);
		chart.getXYPlot().getRangeAxis().setRange( -range, range);
		return chart;
	}
	
	
	/**
	 * Test if any of the datasets have a consensus nucleus folded
	 * @param AnalysisDataset
	 * @return
	 */
	public boolean hasConsensusNucleus(){
		for (IAnalysisDataset dataset : options.getDatasets()){
			if(dataset.getCollection().hasConsensusNucleus()){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Craete a consensus chart from the given dataset. Gives an
	 * empty chart if null.
	 * @param ds
	 * @return
	 */
	private JFreeChart makeConsensusChart(XYDataset ds){
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
	private void formatConsensusChart(JFreeChart chart){
		chart.getPlot().setBackgroundPaint(Color.WHITE);
		chart.getXYPlot().getDomainAxis().setVisible(false);
		chart.getXYPlot().getRangeAxis().setVisible(false);
		chart.getXYPlot().addRangeMarker(ChartComponents.CONSENSUS_ZERO_MARKER);
		chart.getXYPlot().addDomainMarker(ChartComponents.CONSENSUS_ZERO_MARKER);
		
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
	public JFreeChart makeNucleusOutlineChart(){
		
		IAnalysisDataset dataset = options.firstDataset();
		
		if( ! dataset.getCollection().hasConsensusNucleus()){
			return makeEmptyChart();
		}

		XYDataset ds = new NucleusDatasetCreator().createBareNucleusOutline(dataset);
		JFreeChart chart = makeConsensusChart(ds);

		double max = getconsensusChartRange(dataset);

		XYPlot plot = chart.getXYPlot();

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
	private double getconsensusChartRange(IAnalysisDataset dataset){
		ICellCollection collection = dataset.getCollection();
		double maxX = Math.max( Math.abs(collection.getConsensusNucleus().getMinX()) , Math.abs(collection.getConsensusNucleus().getMaxX() ));
		double maxY = Math.max( Math.abs(collection.getConsensusNucleus().getMinY()) , Math.abs(collection.getConsensusNucleus().getMaxY() ));

		// ensure that the scales for each axis are the same
		double max = Math.max(maxX, maxY);

		// ensure there is room for expansion of the target nucleus due to IQR
		max *=  1.25;	
		return max;
	}
	
	/**
	 * Get the maximum absolute range of the axes of the chart. The minimum
	 * returned value will be 1
	 * @param list the datasets to test
	 * @return
	 */
	public double getconsensusChartRange(){
		
		double max = 1;
		for (IAnalysisDataset dataset : options.getDatasets()){
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
	private JFreeChart makeSegmentedConsensusChart(IAnalysisDataset dataset) throws Exception {
		
		if( ! dataset.getCollection().hasConsensusNucleus()){
			return makeEmptyChart();
		}
		XYDataset ds = null;
		
		ICellCollection collection = dataset.getCollection();
		ds = new NucleusDatasetCreator().createSegmentedNucleusOutline(collection);
			
		JFreeChart chart = makeConsensusChart(ds);
		double max = getconsensusChartRange(dataset);

		XYPlot plot = chart.getXYPlot();
		plot.setDataset(0, ds);
		plot.getDomainAxis().setRange(-max,max);
		plot.getRangeAxis().setRange(-max,max);
				
		ColourSwatch swatch = GlobalOptions.getInstance().getSwatch();
		
		formatConsensusChartSeries(plot, true, swatch);
		
		return chart;
	}
	
	/**
	 * Format the series colours for a consensus nucleus
	 * @param plot
	 */
	private void formatConsensusChartSeries(XYPlot plot, boolean showIQR, ColourSwatch swatch){
		
		XYDataset ds = plot.getDataset();
		int seriesCount = plot.getSeriesCount();

		for (int i = 0; i < seriesCount; i++) {
			plot.getRenderer().setSeriesVisibleInLegend(i, false);
			String name = (String) ds.getSeriesKey(i);
			
			// colour the segments
			if(name.startsWith("Seg_")){

				plot.getRenderer().setSeriesStroke(i, ChartComponents.MARKER_STROKE);
				plot.getRenderer().setSeriesPaint(i, Color.BLACK);
			} 
			
			// colour the quartiles
			if(name.startsWith("Q")){
				
				// get the segment component
				// The dataset series name is Q25_Seg_1 etc
				String segmentName = name.replaceAll("Q[2|7]5_", "");
				int segIndex = MorphologyChartFactory.getIndexFromLabel(segmentName);
								
				if(showIQR){
					plot.getRenderer().setSeriesStroke(i, ChartComponents.PROFILE_STROKE);
					Color colour = ColourSelecter.getColor(segIndex);
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
	private JFreeChart makeMultipleConsensusChart() throws Exception {
		// multiple nuclei
		XYDataset ds = new NucleusDatasetCreator().createMultiNucleusOutline(options.getDatasets(), options.getScale());
		JFreeChart chart = makeConsensusChart(ds);
		
		formatConsensusChart(chart);
		
		XYPlot plot = chart.getXYPlot();
		
		double max = getconsensusChartRange();
		
		plot.getDomainAxis().setRange(-max,max);
		plot.getRangeAxis().setRange(-max,max);

		int seriesCount = plot.getSeriesCount();

		for (int i = 0; i < seriesCount; i++) {
			plot.getRenderer().setSeriesVisibleInLegend(i, false);
			String name = (String) ds.getSeriesKey(i);
			plot.getRenderer().setSeriesStroke(i, new BasicStroke(2));

			int index = MorphologyChartFactory.getIndexFromLabel(name);
			IAnalysisDataset d = options.getDatasets().get(index);

			// in this context, segment colour refers to the entire
			// dataset colour (they use the same pallates in ColourSelecter)
			Color color = d.getDatasetColour() == null 
						? ColourSelecter.getColor(i)
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
	
	/**
	 * Create the consensus chart for the given options.
	 * @param options
	 * @return
	 * @throws Exception
	 */
	public JFreeChart makeConsensusChart() throws Exception {
		
		if(! options.hasDatasets()){
			finest("No datasets: creating empty consensus chart");
			return makeEmptyChart();
		}
		
		if(options.isMultipleDatasets()){
			
			boolean oneHasConsensus = false;
			for(IAnalysisDataset d : options.getDatasets()){
				if (d.getCollection().hasConsensusNucleus()){
					oneHasConsensus= true;
				}
			}
			
			if(oneHasConsensus){
				finest("Creating multiple consensus chart");
				return makeMultipleConsensusChart();
			} else {
				finest("No dataset with consensus: creating empty consensus chart");
				return makeEmptyChart();
			}
		}
		
		if(options.isSingleDataset()){
			finest("Creating single consensus chart");
			
			if(options.isShowMesh()){ 
				
				NucleusMesh mesh = new NucleusMesh(options.firstDataset()
						.getCollection()
						.getConsensusNucleus(), options.getMeshSize());
				
				
				if(options.isStraightenMesh()){
					mesh = mesh.straighten();
				}

				return new OutlineChartFactory(options).createMeshChart(mesh, 0.5 );
				
			} else {
				return makeSegmentedConsensusChart(options.firstDataset());
			}
			
			
		}
		finest("Options failed to match: creating empty consensus chart");
		return makeEmptyChart();
	}

}
