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

import gui.components.ColourSelecter;
import stats.NucleusStatistic;
import stats.PlottableStatistic;
import stats.SegmentStatistic;
import stats.SignalStatistic;

import java.awt.Color;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import charting.ChartComponents;
import charting.datasets.ChartDatasetCreationException;
import charting.datasets.NuclearHistogramDatasetCreator;
import charting.datasets.NuclearSignalDatasetCreator;
import charting.options.ChartOptions;
import analysis.AnalysisDataset;


public class HistogramChartFactory extends AbstractChartFactory {

	public HistogramChartFactory(ChartOptions o){
		super(o);
	}
	
	/**
	 * Create a histogram from a histogram dataset and
	 * apply basic formatting
	 * @param ds the dataset to use
	 * @param xLabel the label of the x axis
	 * @param yLabel the label of the y axis
	 * @return a chart
	 */
	public static JFreeChart makeEmptyChart(){
		
		JFreeChart chart = ChartFactory.createHistogram(null, null, null, null, PlotOrientation.VERTICAL, true, true, true);
		
		XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.white);
		XYBarRenderer rend = new XYBarRenderer();
		rend.setBarPainter(new StandardXYBarPainter());
		rend.setShadowVisible(false);
		plot.setRenderer(rend);
		return chart;
	}
	
	/**
	 * Create a histogram from a histogram dataset and
	 * apply basic formatting
	 * @param ds the dataset to use
	 * @param xLabel the label of the x axis
	 * @param yLabel the label of the y axis
	 * @return a chart
	 */
	public static JFreeChart createHistogram(HistogramDataset ds, String xLabel, String yLabel){
		
		JFreeChart chart = ChartFactory.createHistogram(null, xLabel, yLabel, ds, PlotOrientation.VERTICAL, true, true, true);
		
		
		XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.white);
		XYBarRenderer rend = new XYBarRenderer();
		rend.setBarPainter(new StandardXYBarPainter());
		rend.setShadowVisible(false);
		plot.setRenderer(rend);
		if(ds!=null && ds.getSeriesCount()>0){
			for (int j = 0; j < ds.getSeriesCount(); j++) {
				plot.getRenderer().setSeriesVisibleInLegend(j, false);
			}
		}
		return chart;
	}
	
	public JFreeChart createStatisticHistogram() throws Exception{
		
		if(!options.hasDatasets()){
			return makeEmptyChart();
		}
		
		PlottableStatistic stat = options.getStat();
		
		if(stat.getClass()==NucleusStatistic.class){
			return createNuclearStatsHistogram();
		}
		
		if(stat.getClass()==SignalStatistic.class){
			return createSignalStatisticHistogram();
		}
		
		if(stat.getClass()==SegmentStatistic.class){
			return createSegmentStatisticHistogram();
		}
		
		return makeEmptyChart();
		
	}
	
	/**
	 * Create a histogram from a list of values
	 * @param list
	 * @return
	 * @throws Exception
	 */
	public static JFreeChart createRandomSampleHistogram(List<Double> list) throws Exception{
		HistogramDataset ds = NuclearHistogramDatasetCreator.createHistogramDatasetFromList(list);
		JFreeChart chart = createHistogram(ds, "Magnitude difference between populations", "Observed instances");
		chart.getXYPlot().addDomainMarker(new ValueMarker(1, Color.BLACK, ChartComponents.MARKER_STROKE));
		return chart;
	}
	
	
	/**
	 * Create a density chart from a list of values
	 * @param list
	 * @return
	 * @throws Exception
	 */
	public static JFreeChart createRandomSampleDensity(List<Double> list) throws Exception{
		XYDataset ds = NuclearHistogramDatasetCreator.createDensityDatasetFromList(list, 0.0001);
		String xLabel = "Magnitude difference between populations";
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
				                xLabel, "Probability", ds, PlotOrientation.VERTICAL, true, true,
				                false);
		XYPlot plot = chart.getXYPlot();
		for (int j = 0; j < ds.getSeriesCount(); j++) {
			plot.getRenderer().setSeriesVisibleInLegend(j, false);
		}
		
		plot.setBackgroundPaint(Color.WHITE);
		chart.getXYPlot().addDomainMarker(new ValueMarker(1, Color.BLACK, ChartComponents.MARKER_STROKE));
		
		return chart;
	}
		
	
	
	/*
	 * 
	 * PRIVATE METHODS
	 * 
	 */
	
	/**
	 * Create a signal angle histogram for a dataset
	 * @param options the ChartOptions
	 * @return
	 * @throws Exception 
	 */
	private JFreeChart createSignalStatisticHistogram() throws Exception{
		
		if(options.isUseDensity()){
			return createSignalDensityStatsChart();
		}
		
		SignalStatistic stat = (SignalStatistic) options.getStat();
		
		
		List<HistogramDataset> list = options.hasDatasets() 
									? NuclearSignalDatasetCreator.getInstance().createSignaStatisticHistogramDataset(options.getDatasets(), stat, options.getScale())
									: null;
									
									
				
		JFreeChart chart = createHistogram(list.get(0), stat.label(options.getScale()), "Count");
		
		
		if(list!=null && options.hasDatasets()){
			
			XYPlot plot = chart.getXYPlot();
			if(stat.equals(SignalStatistic.ANGLE)){
				plot.getDomainAxis().setRange(0,360);
			}
			
			int datasetCount = 0;
			for(HistogramDataset ds : list){
			
				plot.setDataset(datasetCount, ds);
				
				AnalysisDataset d = options.getDatasets().get(datasetCount);
				
				XYBarRenderer rend = new XYBarRenderer();
				rend.setBarPainter(new StandardXYBarPainter());
				rend.setShadowVisible(false);
				
				plot.setRenderer(datasetCount, rend);

				int seriesCount = ds.getSeriesCount();
				
				for (int j = 0; j < seriesCount; j++) {
					
					String name = ds.getSeriesKey(j).toString();

					UUID signalGroup = getSignalGroupFromLabel(name);
					
					
					
					rend.setSeriesVisibleInLegend(j, false);
					rend.setSeriesStroke(j, ChartComponents.MARKER_STROKE);
					
					Color colour = d.getCollection().getSignalGroup(signalGroup).hasColour()
								 ? d.getCollection().getSignalGroup(signalGroup).getGroupColour()
								 : ColourSelecter.getColor(j);


					rend.setSeriesPaint(j, colour);
				}	
				datasetCount++;
			}
			
		}
		return chart;
	}
	
	/**
	 * Create a density line chart with nuclear statistics. It is used to replace the histograms
	 * when the 'Use density' box is ticked in the Nuclear chart histogram panel
	 * @param ds the histogram dataset
	 * @param list the analysis datasets used to create the histogrom
	 * @param xLabel the x axis label
	 * @return
	 * @throws Exception 
	 */
	private JFreeChart createSignalDensityStatsChart() throws Exception{
	
		if( ! options.hasDatasets()){
			return this.makeEmptyChart();
		}
		
		SignalStatistic stat = (SignalStatistic) options.getStat();
		
		List<DefaultXYDataset> list = NuclearSignalDatasetCreator.getInstance().createSignalDensityHistogramDataset(options.getDatasets(), stat, options.getScale());


		String xLabel = options.getStat().label(options.getScale());
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
				                xLabel, "Probability", list.get(0), PlotOrientation.VERTICAL, true, true,
				                false);
		
		XYPlot plot = chart.getXYPlot();
		
		plot.setBackgroundPaint(Color.WHITE);
		
		if( !list.isEmpty()){
				
			int datasetCount = 0;
			for(DefaultXYDataset ds : list){
				
				plot.setDataset(datasetCount, ds);
				XYLineAndShapeRenderer rend = new XYLineAndShapeRenderer();
				rend.setBaseLinesVisible(true);
				rend.setBaseShapesVisible(false);
				plot.setRenderer(datasetCount, rend);
						
				for (int j = 0; j < ds.getSeriesCount(); j++) {
	
					rend.setSeriesVisibleInLegend(j, false);
					rend.setSeriesStroke(j, ChartComponents.MARKER_STROKE);
	
					String seriesKey = ds.getSeriesKey(j).toString();
	                UUID signalGroup = getSignalGroupFromLabel(seriesKey);
	
//					String seriesName = seriesKey.replacseFirst(options.getStat().toString()+"_", "");
	
					Color colour = ColourSelecter.getColor(j);
					
					AnalysisDataset d = options.getDatasets().get(datasetCount);

                    colour  = d.getCollection().getSignalGroup(signalGroup).hasColour()
                            ? d.getCollection().getSignalGroup(signalGroup).getGroupColour()
                            : ColourSelecter.getColor(j);


                    rend.setSeriesPaint(j, colour);

	
				}
				datasetCount++;
			}
			setDomainRange(plot, list);
		}
		
		
		
		return chart;
	}
	
	


	/**
	 * Create a histogram with nuclear statistics
	 * @param ds the histogram dataset
	 * @param list the analysis datasets used to create the histogrom
	 * @param xLabel the x axis label
	 * @return
	 */
	private JFreeChart createNuclearStatsHistogram() throws ChartCreationException {

		if(options.isUseDensity()){
			return createNuclearDensityStatsChart();
		}
		
		if( ! options.hasDatasets()){
			return makeEmptyChart();
		}
		
		HistogramDataset ds;
				

		try {
			ds = new NuclearHistogramDatasetCreator(options).createNuclearStatsHistogramDataset();
		} catch (ChartDatasetCreationException e) {
			throw new ChartCreationException("Cannot get data for nuclear stats", e);
		}

		
		
		finer("Creating histogram for "+options.getStat());
		
		
		
		String xLabel = options.getStat().label(options.getScale());
		
		JFreeChart chart = createHistogram(ds, xLabel, "Nuclei");



		XYPlot plot = chart.getXYPlot();

		setDomainRange(plot, ds);

		for (int j = 0; j < ds.getSeriesCount(); j++) {

			plot.getRenderer().setSeriesVisibleInLegend(j, false);
			plot.getRenderer().setSeriesStroke(j, ChartComponents.MARKER_STROKE);

			String seriesKey = (String) ds.getSeriesKey(j);
			String seriesName = seriesKey.replaceFirst(options.getStat().toString()+"_", "");

			for(AnalysisDataset dataset : options.getDatasets()){

				if(seriesName.equals(dataset.getName())){
					Color colour = dataset.hasDatasetColour()
							? dataset.getDatasetColour()
									: ColourSelecter.getColor(j);



							finest("Setting histogram series colour: "+colour.toString());

							plot.getRenderer().setSeriesPaint(j, colour);

				}
			}


		}
		return chart;
	}
	
	/**
	 * Create a density line chart with nuclear statistics. It is used to replace the histograms
	 * when the 'Use density' box is ticked in the Nuclear chart histogram panel
	 * @param ds the histogram dataset
	 * @param list the analysis datasets used to create the histogrom
	 * @param xLabel the x axis label
	 * @return
	 * @throws Exception 
	 */
	private JFreeChart createNuclearDensityStatsChart() throws ChartCreationException {
		
		if( ! options.hasDatasets()){
			return makeEmptyChart();
		}
		
		XYDataset ds;
		try {
			ds = new NuclearHistogramDatasetCreator(options).createNuclearDensityHistogramDataset();
		} catch (ChartDatasetCreationException e) {
			throw new ChartCreationException("Cannot get data for nuclear stats density", e);
		}


		String xLabel = options.getStat().label(options.getScale());
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
				                xLabel, "Probability", ds, PlotOrientation.VERTICAL, true, true,
				                false);
		
		XYPlot plot = chart.getXYPlot();
		
		plot.setBackgroundPaint(Color.WHITE);
		
		setDomainRange(plot, ds);

		for (int j = 0; j < ds.getSeriesCount(); j++) {

			plot.getRenderer().setSeriesVisibleInLegend(j, false);
			plot.getRenderer().setSeriesStroke(j, ChartComponents.MARKER_STROKE);

			String seriesKey = (String) ds.getSeriesKey(j);
			String seriesName = seriesKey.replaceFirst(options.getStat().toString()+"_", "");

			Color colour = ColourSelecter.getColor(j);
			for(AnalysisDataset dataset : options.getDatasets()){

				if(seriesName.equals(dataset.getName())){

					colour = dataset.hasDatasetColour()
							? dataset.getDatasetColour()
									: colour;


							plot.getRenderer().setSeriesPaint(j, colour);

				}
			}

			

		}
		return chart;
	}
	
	/**
	 * Create a histogram with segment lengths
	 * @param options the HistogramOptions. 
	 * @param segName the segment to plot
	 * @return
	 */
	private JFreeChart createSegmentStatisticHistogram() throws ChartCreationException {

		if(options.isUseDensity()){
			return createSegmentLengthDensityChart();
		}
		
		if( ! options.hasDatasets()){
			return makeEmptyChart();
		}
		
		
		HistogramDataset ds;
				

		try {
			ds = new NuclearHistogramDatasetCreator(options).createSegmentLengthHistogramDataset();
		} catch (ChartDatasetCreationException e) {
			throw new ChartCreationException("Cannot get data for segment lengths", e);
		}


		
		finer("Creating histogram for Seg_"+options.getSegPosition());
		
		
		JFreeChart chart = createHistogram(ds, "Seg_"+options.getSegPosition()+" length ("+options.getScale()+")", "Nuclei" );
		
		if(ds!=null && options.hasDatasets()){
						
			XYPlot plot = chart.getXYPlot();
			
			setDomainRange(plot, ds);
			
			for (int j = 0; j < ds.getSeriesCount(); j++) {

				plot.getRenderer().setSeriesVisibleInLegend(j, false);
				plot.getRenderer().setSeriesStroke(j, ChartComponents.MARKER_STROKE);

				String seriesKey = (String) ds.getSeriesKey(j);
				String seriesName = seriesKey.replaceFirst("Seg_"+options.getSegPosition()+"_", "");
				
				for(AnalysisDataset dataset : options.getDatasets()){
					
					if(seriesName.equals(dataset.getName())){
						Color colour = dataset.hasDatasetColour()
								? dataset.getDatasetColour()
										: ColourSelecter.getColor(j);


								
								finest("Setting histogram series colour: "+colour.toString());
								
						plot.getRenderer().setSeriesPaint(j, colour);

					}
				}
				
			}
		}
		return chart;
	}
	
	/**
	 * Create a density line chart with nuclear statistics. It is used to replace the histograms
	 * when the 'Use density' box is ticked in the Nuclear chart histogram panel
	 * @param ds the histogram dataset
	 * @param list the analysis datasets used to create the histogrom
	 * @param xLabel the x axis label
	 * @return
	 * @throws Exception 
	 */
	private JFreeChart createSegmentLengthDensityChart() throws ChartCreationException {
				
		if( ! options.hasDatasets()){
			
			return makeEmptyChart();
			
		}
		
		XYDataset ds;
		

		try {
			ds = new NuclearHistogramDatasetCreator(options).createSegmentLengthDensityDataset();
		} catch (ChartDatasetCreationException e) {

			throw new ChartCreationException("Cannot get data for segment length density", e);
		}
		

		String xLabel = "Seg_"+options.getSegPosition()+" length ("+options.getScale()+")";
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
				                xLabel, "Probability", ds, PlotOrientation.VERTICAL, true, true,
				                false);
		
		XYPlot plot = chart.getXYPlot();
		
		plot.setBackgroundPaint(Color.WHITE);
		
		if(ds!=null && options.hasDatasets()){
						
			setDomainRange(plot, ds);
			
			
			for (int j = 0; j < ds.getSeriesCount(); j++) {

				plot.getRenderer().setSeriesVisibleInLegend(j, false);
				plot.getRenderer().setSeriesStroke(j, ChartComponents.MARKER_STROKE);

				String seriesKey = (String) ds.getSeriesKey(j);
				String seriesName = seriesKey.replaceFirst("Seg_"+options.getSegPosition()+"_", "");

				Color colour = ColourSelecter.getColor(j);
				for(AnalysisDataset dataset : options.getDatasets()){

					if(seriesName.equals(dataset.getName())){

						colour = dataset.hasDatasetColour()
								? dataset.getDatasetColour()
								: colour;


						plot.getRenderer().setSeriesPaint(j, colour);

					}
				}

			}

		}
		return chart;
	}
	
	/**
	 * Update the range of the plot domain axis to the min and max
	 * values within the given dataset
	 * @param plot
	 * @param ds
	 */
	private void setDomainRange(XYPlot plot, XYDataset ds){

			Number max = DatasetUtilities.findMaximumDomainValue(ds);
			Number min = DatasetUtilities.findMinimumDomainValue(ds);
			if(max.doubleValue()>min.doubleValue()){ // stop if 0 and 0 or no values found
				plot.getDomainAxis().setRange(min.doubleValue(), max.doubleValue());
			}		
	}
	
	/**
	 * Update the range of the plot domain axis to the min and max
	 * values within the given dataset
	 * @param plot
	 * @param ds
	 */
	private void setDomainRange(XYPlot plot, List<DefaultXYDataset> list){
	
		Range r = plot.getDomainAxis().getRange();
		
		for(XYDataset ds : list){
			Number maxX = DatasetUtilities.findMaximumDomainValue(ds);
			Number minX = DatasetUtilities.findMinimumDomainValue(ds);
			
			Range sub = new Range(minX.doubleValue(), maxX.doubleValue());
			
			r = Range.combine(sub, r);
			
		}
		
		plot.getDomainAxis().setRange(r);

	}

}
