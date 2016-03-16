/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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
package gui.components;

import java.util.logging.Level;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYDataset;

@SuppressWarnings("serial")
public class FixedAspectRatioChartPanel extends ExportableChartPanel {


	public FixedAspectRatioChartPanel(JFreeChart chart){
		super(chart);
	}
						
	@Override
	public void restoreAutoBounds() {
		
		try {
			XYPlot plot = (XYPlot) this.getChart().getPlot();
			if(plot.getDatasetCount()==0){
				return;
			}

			double chartWidth = this.getWidth();
			double chartHeight = this.getHeight();
			double aspectRatio = chartWidth / chartHeight;
			
			log(Level.FINEST, "Plot w: "+chartWidth+"; h: "+chartHeight+"; asp: "+aspectRatio);

			// start with impossible values
			double xMin = chartWidth;
			double yMin = chartHeight;
			//		
			double xMax = 0;
			double yMax = 0;

			log(Level.FINEST, "Plot has "+plot.getDatasetCount()+" datasets");
			// get the max and min values of the chart
			for(int i = 0; i<plot.getDatasetCount();i++){
				XYDataset dataset = plot.getDataset(i);

				if(dataset==null){
					log(Level.FINEST, "Null dataset "+i);
					super.restoreAutoBounds();
					return;
				}

				xMax = DatasetUtilities.findMaximumDomainValue(dataset).doubleValue() > xMax
						? DatasetUtilities.findMaximumDomainValue(dataset).doubleValue()
								: xMax;

						xMin = DatasetUtilities.findMinimumDomainValue(dataset).doubleValue() < xMin
								? DatasetUtilities.findMinimumDomainValue(dataset).doubleValue()
										: xMin;

								yMax = DatasetUtilities.findMaximumRangeValue(dataset).doubleValue() > yMax
										? DatasetUtilities.findMaximumRangeValue(dataset).doubleValue()
												: yMax;

										yMin = DatasetUtilities.findMinimumRangeValue(dataset).doubleValue() < yMin
												? DatasetUtilities.findMinimumRangeValue(dataset).doubleValue()
														: yMin;
			}
			
//			IJ.log("Found min max range");


			// find the ranges they cover
			double xRange = xMax - xMin;
			double yRange = yMax - yMin;

			//		double aspectRatio = xRange / yRange;

			double newXRange = xRange;
			double newYRange = yRange;

			// test the aspect ratio
			log(Level.FINEST, "Old range: "+xMax+"-"+xMin+", "+yMax+"-"+yMin);
			if( (xRange / yRange) > aspectRatio){
				// width is not enough
				//			IJ.log("Too narrow: "+xRange+", "+yRange+":  aspect ratio "+aspectRatio);
				newXRange = xRange * 1.1;
				newYRange = newXRange / aspectRatio;
			} else {
				// height is not enough
				//			IJ.log("Too short: "+xRange+", "+yRange+":  aspect ratio "+aspectRatio);
				newYRange = yRange * 1.1; // add some extra x space
				newXRange = newYRange * aspectRatio; // get the new Y range
			}


			// with the new ranges, find the best min and max values to use
			double xDiff = (newXRange - xRange)/2;
			double yDiff = (newYRange - yRange)/2;

			xMin -= xDiff;
			xMax += xDiff;
			yMin -= yDiff;
			yMax += yDiff;
			log(Level.FINEST, "New range: "+xMax+"-"+xMin+", "+yMax+"-"+yMin);

			plot.getRangeAxis().setRange(yMin, yMax);
			plot.getDomainAxis().setRange(xMin, xMax);
//			IJ.log("Set min max range");
		} catch (Exception e){
			logError("Error restoring auto bounds", e);
			super.restoreAutoBounds();
		}
	
	} 	
}

