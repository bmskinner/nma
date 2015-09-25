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
package datasets;

import gui.components.ColourSelecter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.List;

import no.analysis.AnalysisDataset;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.statistics.HistogramDataset;


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
		
		JFreeChart chart = null;
		if(ds==null){
			chart = ChartFactory.createHistogram(null, xLabel, yLabel, null, PlotOrientation.VERTICAL, true, true, true);
		} else {
			chart = ChartFactory.createHistogram(null, xLabel, yLabel, ds,   PlotOrientation.VERTICAL, true, true, true);
		}
		
		XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.white);
		XYBarRenderer rend = new XYBarRenderer();
		rend.setBarPainter(new StandardXYBarPainter());
		rend.setShadowVisible(false);
		plot.setRenderer(rend);
		return chart;
	}
	
	/**
	 * Create a signal angle histogram
	 * @param ds the histogram dataset
	 * @param dataset the analysis dataset
	 * @return
	 */
	public static JFreeChart createSignalAngleHistogram(HistogramDataset ds, AnalysisDataset dataset){
		JFreeChart chart = createHistogram(ds, "Angle", "Count");
		if(ds!=null && dataset!=null){
			XYPlot plot = chart.getXYPlot();
			plot.getDomainAxis().setRange(0,360);
			for (int j = 0; j < ds.getSeriesCount(); j++) {
				String name = (String) ds.getSeriesKey(j);
				int seriesGroup = MorphologyChartFactory.getIndexFromLabel(name);
				plot.getRenderer().setSeriesVisibleInLegend(j, Boolean.FALSE);
				plot.getRenderer().setSeriesStroke(j, new BasicStroke(2));
				Color colour = dataset.getSignalGroupColour(seriesGroup);
				plot.getRenderer().setSeriesPaint(j, ColourSelecter.getTransparentColour(colour, true, 128));
			}	
		}
		return chart;
	}
	
	/**
	 * Create a signal distance histogram
	 * @param ds the histogram dataset
	 * @param dataset the analysis dataset
	 * @return
	 */
	public static JFreeChart createSignalDistanceHistogram(HistogramDataset ds, AnalysisDataset dataset){
		JFreeChart chart = createHistogram(ds, "Distance", "Count");
		if(ds!=null && dataset!=null){
			XYPlot plot = chart.getXYPlot();
			plot.getDomainAxis().setRange(0,1);
			for (int j = 0; j < ds.getSeriesCount(); j++) {
				plot.getRenderer().setSeriesVisibleInLegend(j, Boolean.FALSE);
				plot.getRenderer().setSeriesStroke(j, new BasicStroke(2));
				int index = MorphologyChartFactory.getIndexFromLabel( (String) ds.getSeriesKey(j));
				Color colour = dataset.getSignalGroupColour(index);
				plot.getRenderer().setSeriesPaint(j, ColourSelecter.getTransparentColour(colour, true, 128));
			}	
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
	public static JFreeChart createNuclearStatsHistogram(HistogramDataset ds, List<AnalysisDataset> list, String xLabel){
		JFreeChart chart = createHistogram(ds, xLabel, "Count");
		
		if(ds!=null && list!=null){
			
			XYPlot plot = chart.getXYPlot();
			
			Number maxX = DatasetUtilities.findMaximumDomainValue(ds);
			Number minX = DatasetUtilities.findMinimumDomainValue(ds);
			plot.getDomainAxis().setRange(minX.doubleValue(), maxX.doubleValue());	
			
			for(AnalysisDataset dataset : list){

				for (int j = 0; j < ds.getSeriesCount(); j++) {
										
					plot.getRenderer().setSeriesVisibleInLegend(j, false);
					plot.getRenderer().setSeriesStroke(j, new BasicStroke(2));
					
					String seriesKey = (String) ds.getSeriesKey(j);
					String seriesName = seriesKey.replace(xLabel+"_", "");
					
					if(seriesName.equals(dataset.getName())){
						
						Color colour = dataset.getDatasetColour()==null
										? ColourSelecter.getSegmentColor(j)
										: dataset.getDatasetColour();
										
						plot.getRenderer().setSeriesPaint(j, colour);
//						plot.getRenderer().setSeriesPaint(j, ColourSelecter.getTransparentColour(colour, true, 128));
						
					}
				}
			}
		}
		return chart;
	}
	
	
	
}
