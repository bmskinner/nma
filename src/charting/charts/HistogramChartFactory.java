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
import ij.IJ;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;

import charting.ChartComponents;
import charting.datasets.NuclearHistogramDatasetCreator;
import charting.datasets.NuclearSignalDatasetCreator;
import analysis.AnalysisDataset;
import components.generic.MeasurementScale;
import components.nuclear.NucleusStatistic;


public class HistogramChartFactory {
		
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
		return chart;
	}
	
	/**
	 * Create a signal angle histogram for a dataset
	 * @param options the ChartOptions
	 * @return
	 */
	public static JFreeChart createSignalAngleHistogram(HistogramChartOptions options){
		
		HistogramDataset ds = options.hasDatasets() 
							? NuclearSignalDatasetCreator.createSignalAngleHistogramDataset(options.getDatasets())
							: null;
				
		JFreeChart chart = createHistogram(ds, "Angle", "Count");
		if(ds!=null && options.hasDatasets()){
			XYPlot plot = chart.getXYPlot();
			plot.getDomainAxis().setRange(0,360);
			setSeriesPropertiesForSignalHistogram(chart, options.firstDataset());	
		}
		return chart;
	}
	
	/**
	 * Create a signal distance histogram
	 * @param ds the histogram dataset
	 * @param dataset the analysis dataset
	 * @return
	 */
	public static JFreeChart createSignalDistanceHistogram(HistogramChartOptions options){
		
		HistogramDataset ds = options.hasDatasets() 
							? NuclearSignalDatasetCreator.createSignalDistanceHistogramDataset(options.getDatasets())
							: null;
		
		JFreeChart chart = createHistogram(ds, "Distance", "Count");
		if(ds!=null && options.hasDatasets()){
			XYPlot plot = chart.getXYPlot();
			plot.getDomainAxis().setRange(0,1);
			setSeriesPropertiesForSignalHistogram(chart, options.firstDataset());
		}
		return chart;
	}
	
	private static void setSeriesPropertiesForSignalHistogram(JFreeChart chart, AnalysisDataset dataset){
		
		if(dataset.getCollection().hasSignals()){
			XYPlot plot = chart.getXYPlot();
			int seriesCount = plot.getDataset().getSeriesCount();
			for (int j = 0; j < seriesCount; j++) {
				String name = (String) plot.getDataset().getSeriesKey(j);
				int seriesGroup = MorphologyChartFactory.getIndexFromLabel(name);
				plot.getRenderer().setSeriesVisibleInLegend(j, false);
				plot.getRenderer().setSeriesStroke(j, ChartComponents.MARKER_STROKE);
				Color colour = dataset.getSignalGroupColour(seriesGroup);
				plot.getRenderer().setSeriesPaint(j, ColourSelecter.getTransparentColour(colour, true, 128));
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
	public static JFreeChart createNuclearStatsHistogram(HistogramChartOptions options) throws Exception{

		HistogramDataset ds = null;
				
		if (options.hasDatasets()){
			ds = NuclearHistogramDatasetCreator.createNuclearStatsHistogramDataset(options);
		}
		
		if(options.hasLogger()){
			options.getLogger().log(Level.FINER, "Creating histogram for "+options.getStat());
		}
		
		
		String xLabel = options.getStat().label(options.getScale());
		
		JFreeChart chart = createHistogram(ds, xLabel, "Nuclei");
		
		if(ds!=null && options.hasDatasets()){
						
			XYPlot plot = chart.getXYPlot();
			
			Number maxX = DatasetUtilities.findMaximumDomainValue(ds);
			Number minX = DatasetUtilities.findMinimumDomainValue(ds);
			plot.getDomainAxis().setRange(minX.doubleValue(), maxX.doubleValue());	
			
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


								if(options.hasLogger()){
									options.getLogger().log(Level.FINEST, "Setting histogram series colour: "+colour.toString());
								}
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
	public static JFreeChart createNuclearDensityStatsChart(HistogramChartOptions options) throws Exception{
		
//	public static JFreeChart createNuclearDensityStatsChart(DefaultXYDataset ds, List<AnalysisDataset> list, NucleusStatistic stat, MeasurementScale scale){
		
		DefaultXYDataset ds = null;
		
		if (options.hasDatasets()){
			ds = NuclearHistogramDatasetCreator.createNuclearDensityHistogramDataset(options.getDatasets(), options.getStat(), options.getScale());
		}

		String xLabel = options.getStat().label(options.getScale());
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
				                xLabel, "Probability", ds, PlotOrientation.VERTICAL, true, true,
				                false);
		
		XYPlot plot = chart.getXYPlot();
		
		plot.setBackgroundPaint(Color.WHITE);
		
		if(ds!=null && options.hasDatasets()){
						
			Number maxX = DatasetUtilities.findMaximumDomainValue(ds);
			Number minX = DatasetUtilities.findMinimumDomainValue(ds);
			plot.getDomainAxis().setRange(minX.doubleValue(), maxX.doubleValue());	
			
			
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
	public static JFreeChart createSegmentLengthHistogram(HistogramChartOptions options, String segName) throws Exception{

		HistogramDataset ds = null;
				
		if (options.hasDatasets()){
			ds = NuclearHistogramDatasetCreator.createSegmentLengthHistogramDataset(options, segName);
		}
		
		if(options.hasLogger()){
			options.getLogger().log(Level.FINER, "Creating histogram for "+segName);
		}
		
		JFreeChart chart = createHistogram(ds, segName+" length ("+options.getScale()+")", "Nuclei" );
		
		if(ds!=null && options.hasDatasets()){
						
			XYPlot plot = chart.getXYPlot();
			
			Number maxX = DatasetUtilities.findMaximumDomainValue(ds);
			Number minX = DatasetUtilities.findMinimumDomainValue(ds);
			plot.getDomainAxis().setRange(minX.doubleValue(), maxX.doubleValue());	
			
			for (int j = 0; j < ds.getSeriesCount(); j++) {

				plot.getRenderer().setSeriesVisibleInLegend(j, false);
				plot.getRenderer().setSeriesStroke(j, ChartComponents.MARKER_STROKE);

				String seriesKey = (String) ds.getSeriesKey(j);
				String seriesName = seriesKey.replaceFirst(segName+"_", "");
				
				for(AnalysisDataset dataset : options.getDatasets()){
					
					if(seriesName.equals(dataset.getName())){
						Color colour = dataset.hasDatasetColour()
								? dataset.getDatasetColour()
										: ColourSelecter.getSegmentColor(j);


								if(options.hasLogger()){
									options.getLogger().log(Level.FINEST, "Setting histogram series colour: "+colour.toString());
								}
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
	public static JFreeChart createSegmentLengthDensityChart(HistogramChartOptions options, String segName) throws Exception{
		
		DefaultXYDataset ds = null;
		
		if (options.hasDatasets()){
			ds = NuclearHistogramDatasetCreator.createSegmentLengthDensityDataset(options.getDatasets(), segName, options.getScale());
		}

		String xLabel = segName+" length ("+options.getScale()+")";
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
				                xLabel, "Probability", ds, PlotOrientation.VERTICAL, true, true,
				                false);
		
		XYPlot plot = chart.getXYPlot();
		
		plot.setBackgroundPaint(Color.WHITE);
		
		if(ds!=null && options.hasDatasets()){
						
			Number maxX = DatasetUtilities.findMaximumDomainValue(ds);
			Number minX = DatasetUtilities.findMinimumDomainValue(ds);
			plot.getDomainAxis().setRange(minX.doubleValue(), maxX.doubleValue());	
			
			
			for (int j = 0; j < ds.getSeriesCount(); j++) {

				plot.getRenderer().setSeriesVisibleInLegend(j, false);
				plot.getRenderer().setSeriesStroke(j, ChartComponents.MARKER_STROKE);

				String seriesKey = (String) ds.getSeriesKey(j);
				String seriesName = seriesKey.replaceFirst(segName+"_", "");

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

}
