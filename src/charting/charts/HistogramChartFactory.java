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
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import charting.ChartComponents;
import charting.datasets.NuclearHistogramDatasetCreator;
import charting.datasets.NuclearSignalDatasetCreator;
import charting.options.ChartOptions;
import analysis.AnalysisDataset;


public class HistogramChartFactory extends AbstractChartFactory {

	/**
	 * Create a histogram from a histogram dataset and
	 * apply basic formatting
	 * @param ds the dataset to use
	 * @param xLabel the label of the x axis
	 * @param yLabel the label of the y axis
	 * @return a chart
	 */
	public static JFreeChart createEmptyHistogram(){
		
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
	
	public static JFreeChart createStatisticHistogram(ChartOptions options) throws Exception{
		
		if(!options.hasDatasets()){
			return createEmptyHistogram();
		}
		
		PlottableStatistic stat = options.getStat();
		
		if(stat.getClass()==NucleusStatistic.class){
			return createNuclearStatsHistogram(options);
		}
		
		if(stat.getClass()==SignalStatistic.class){
			return createSignalStatisticHistogram(options);
		}
		
		if(stat.getClass()==SegmentStatistic.class){
			return createSegmentStatisticHistogram(options);
		}
		
		return createEmptyHistogram();
		
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
	private static JFreeChart createSignalStatisticHistogram(ChartOptions options) throws Exception{
		
		if(options.isUseDensity()){
			return createSignalDensityStatsChart(options);
		}
		
		SignalStatistic stat = (SignalStatistic) options.getStat();
		
		HistogramDataset ds = options.hasDatasets() 
							? NuclearSignalDatasetCreator.getInstance().createSignaStatisticHistogramDataset(options.getDatasets(), stat, options.getScale())
							: null;
				
		JFreeChart chart = createHistogram(ds, stat.label(options.getScale()), "Count");
		if(ds!=null && options.hasDatasets()){
			XYPlot plot = chart.getXYPlot();
			if(stat.equals(SignalStatistic.ANGLE)){
				plot.getDomainAxis().setRange(0,360);
			}
			setSeriesPropertiesForSignalHistogram(chart, options.firstDataset());	
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
	private static JFreeChart createSignalDensityStatsChart(ChartOptions options) throws Exception{
	
		DefaultXYDataset ds = null;
		
		if (options.hasDatasets()){
			
			SignalStatistic stat = (SignalStatistic) options.getStat();
			ds = NuclearSignalDatasetCreator.getInstance().createSignalDensityHistogramDataset(options.getDatasets(), stat, options.getScale());
		}

		String xLabel = options.getStat().label(options.getScale());
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
                UUID signalGroup = getSignalGroupFromLabel(seriesKey);

				String seriesName = seriesKey.replaceFirst(options.getStat().toString()+"_", "");

				Color colour = ColourSelecter.getSegmentColor(j);
				for(AnalysisDataset dataset : options.getDatasets()){

					if(seriesName.equals(dataset.getName())){
                        colour = dataset.getCollection().getSignalGroup(signalGroup).hasColour()
                                ? dataset.getCollection().getSignalGroup(signalGroup).getGroupColour()
                                : ColourSelecter.getSegmentColor(j);


						plot.getRenderer().setSeriesPaint(j, colour);

					}
				}

			}

		}
		return chart;
	}
	
	
	private static void setSeriesPropertiesForSignalHistogram(JFreeChart chart, AnalysisDataset dataset){
		
		if(dataset.getCollection().getSignalManager().hasSignals()){
			XYPlot plot = chart.getXYPlot();
			int seriesCount = plot.getDataset().getSeriesCount();
			for (int j = 0; j < seriesCount; j++) {
				String name = (String) plot.getDataset().getSeriesKey(j);
//				int seriesGroup = getIndexFromLabel(name);
				UUID signalGroup = getSignalGroupFromLabel(name);
				plot.getRenderer().setSeriesVisibleInLegend(j, false);
				plot.getRenderer().setSeriesStroke(j, ChartComponents.MARKER_STROKE);
                Color colour = dataset.getCollection().getSignalGroup(signalGroup).hasColour()
                        ? dataset.getCollection().getSignalGroup(signalGroup).getGroupColour()
                        : ColourSelecter.getSegmentColor(j);


				plot.getRenderer().setSeriesPaint(j, colour);
			}	
		}
	}
	

	/**
	 * Create a histogram with nuclear statistics
	 * @param ds the histogram dataset
	 * @param list the analysis datasets used to create the histogrom
	 * @param xLabel the x axis label
	 * @return
	 */
	private static JFreeChart createNuclearStatsHistogram(ChartOptions options) throws Exception{

		if(options.isUseDensity()){
			return createNuclearDensityStatsChart(options);
		}
		
		HistogramDataset ds = null;
				
		if (options.hasDatasets()){
			ds = NuclearHistogramDatasetCreator.createNuclearStatsHistogramDataset(options);
		}
		
		
		options.log(Level.FINER, "Creating histogram for "+options.getStat());
		
		
		
		String xLabel = options.getStat().label(options.getScale());
		
		JFreeChart chart = createHistogram(ds, xLabel, "Nuclei");
		
		if(ds!=null && options.hasDatasets()){
						
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
										: ColourSelecter.getSegmentColor(j);


								
									options.log(Level.FINEST, "Setting histogram series colour: "+colour.toString());
								
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
	private static JFreeChart createNuclearDensityStatsChart(ChartOptions options) throws Exception{
	
		DefaultXYDataset ds = null;
		
		if (options.hasDatasets()){
			
			NucleusStatistic stat = (NucleusStatistic) options.getStat();
			ds = NuclearHistogramDatasetCreator.createNuclearDensityHistogramDataset(options.getDatasets(), stat, options.getScale());
		}

		String xLabel = options.getStat().label(options.getScale());
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
				String seriesName = seriesKey.replaceFirst(options.getStat().toString()+"_", "");

				Color colour = ColourSelecter.getSegmentColor(j);
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
	 * Create a histogram with segment lengths
	 * @param options the HistogramOptions. 
	 * @param segName the segment to plot
	 * @return
	 */
	private static JFreeChart createSegmentStatisticHistogram(ChartOptions options) throws Exception{

		if(options.isUseDensity()){
			return createSegmentLengthDensityChart(options);
		}
		
		HistogramDataset ds = null;
				
		if (options.hasDatasets()){
			ds = NuclearHistogramDatasetCreator.createSegmentLengthHistogramDataset(options);
		}
		
		
		options.log(Level.FINER, "Creating histogram for Seg_"+options.getSegPosition());
		
		
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
										: ColourSelecter.getSegmentColor(j);


								
									options.log(Level.FINEST, "Setting histogram series colour: "+colour.toString());
								
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
	private static JFreeChart createSegmentLengthDensityChart(ChartOptions options) throws Exception{
		
		DefaultXYDataset ds = null;
		
		if (options.hasDatasets()){
			ds = NuclearHistogramDatasetCreator.createSegmentLengthDensityDataset(options);
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

				Color colour = ColourSelecter.getSegmentColor(j);
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
	private static void setDomainRange(XYPlot plot, XYDataset ds){
		Number maxX = DatasetUtilities.findMaximumDomainValue(ds);
		Number minX = DatasetUtilities.findMinimumDomainValue(ds);
		if(maxX.doubleValue()>minX.doubleValue()){ // stop if 0 and 0
			plot.getDomainAxis().setRange(minX.doubleValue(), maxX.doubleValue());
		}
	}

}
