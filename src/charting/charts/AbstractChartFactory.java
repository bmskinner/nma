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

import java.awt.Color;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;

import logging.Loggable;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;

import charting.ChartComponents;
import components.generic.BorderTag;

public abstract class AbstractChartFactory implements Loggable {

	protected static final ForkJoinPool mainPool = new ForkJoinPool();
	
	/**
	 * Get a series or dataset index for colour selection when drawing charts. The index
	 * is set in the DatasetCreator as part of the label. The format is Name_index_other
	 * @param label the label to extract the index from 
	 * @return the index found
	 */
	public static int getIndexFromLabel(String label){
		String[] names = label.split("_");
		return Integer.parseInt(names[1]);
	}
	
	/**
	 * Find the UUID of a signal group from a label
	 * @param label
	 * @return
	 */
	public static UUID getSignalGroupFromLabel(String label){
		String[] names = label.split("_");
		return UUID.fromString(names[1]);
	}
	

	
	/**
	 * Draw domain markers for the given border tag at the given position
	 * @param plot
	 * @param tag
	 * @param value
	 */
	protected static void addMarkerToXYPlot(XYPlot plot, BorderTag tag, double value){
		Color colour = Color.BLACK;
		if(tag.equals(BorderTag.ORIENTATION_POINT)){
			colour = Color.BLUE;
		}
		if(tag.equals(BorderTag.REFERENCE_POINT)){
			colour = Color.ORANGE;
		}
		plot.addDomainMarker(new ValueMarker(value, colour, ChartComponents.MARKER_STROKE));	
	}
	
	public JFreeChart makeErrorChart(){
		JFreeChart chart = ChartFactory.createXYLineChart(null,
				null, null, null); 
		
		XYPlot plot = chart.getXYPlot();
		
		plot.getDomainAxis().setRange(-10, 10);
		plot.getRangeAxis().setRange(-10, 10);

		for(int i=-100 ; i<=100; i+=20){
			for(int j=-100 ; j<=100; j+=20){
				XYTextAnnotation annotation = new XYTextAnnotation("Error creating chart", i, j);
				annotation.setPaint(Color.BLACK);
				plot.addAnnotation(annotation);
			}
		}
		
		return chart;
	}
		
}
