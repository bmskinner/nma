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
package charting.charts;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.logging.Level;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleEdge;


@SuppressWarnings("serial")
public class FixedAspectRatioChartPanel extends ExportableChartPanel implements ComponentListener {

	protected static final double DEFAULT_AUTO_RANGE = 10;

	public FixedAspectRatioChartPanel(JFreeChart chart){
		super(chart);
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				restoreAutoBounds();
			}
		});
		
//		this.setZoomAroundAnchor(false);
//		
//		this.addMouseWheelListener(new MouseWheelListener() {
//	        public void mouseWheelMoved(MouseWheelEvent arg0) {
//	        	
//	        	XYPlot plot = (XYPlot) FixedAspectRatioChartPanel.this.getChart().getPlot();
//	    		Rectangle2D plotArea = FixedAspectRatioChartPanel.this.getScreenDataArea();
//	    		ValueAxis domainAxis = plot.getDomainAxis();
//	    		RectangleEdge domainAxisEdge = plot.getDomainAxisEdge();
//	    		ValueAxis rangeAxis = plot.getRangeAxis();
//	    		RectangleEdge rangeAxisEdge = plot.getRangeAxisEdge();
//	        	
//	        	Point2D p = FixedAspectRatioChartPanel.this.translateScreenToJava2D(arg0.getPoint());
//	        	double chartX = domainAxis.java2DToValue(p.getX(), plotArea,
//	        			domainAxisEdge);
//	        	double chartY = rangeAxis.java2DToValue(p.getY(), plotArea,
//	        			rangeAxisEdge);
//	        	
//	        	log("Anchoring at chart location "+chartX+", "+chartY);
//	        	FixedAspectRatioChartPanel.this.setZoomAroundAnchor(true);
//	        	FixedAspectRatioChartPanel.this.setAnchor( new Point2D.Double(chartX, chartY));
//	        	
//	        	
//	            if (arg0.getWheelRotation() > 0) {
//	            	FixedAspectRatioChartPanel.this.zoomOutDomain(0.5, 0.5);
//	            	FixedAspectRatioChartPanel.this.zoomOutRange(0.5, 0.5);
//	            } else if (arg0.getWheelRotation() < 0) {
//	            	FixedAspectRatioChartPanel.this.zoomInDomain(1.5, 1.5);
//	            	FixedAspectRatioChartPanel.this.zoomInRange(1.5, 1.5);
//	            }
//	            FixedAspectRatioChartPanel.this.setZoomAroundAnchor(false);
//	        }
//	    });
		
		restoreAutoBounds();
	}
	
	@Override
	public void setChart(JFreeChart chart){
		super.setChart(chart);
		restoreAutoBounds();
	}
						
	@Override
	public void restoreAutoBounds() {
		
		try {
			XYPlot plot = (XYPlot) this.getChart().getPlot();
			if(plot.getDatasetCount()==0){
				return;
			}

			// Find the aspect ratio of the chart
			double chartWidth  = this.getWidth();
			double chartHeight = this.getHeight();
			
			if(Double.valueOf(chartWidth)==null || Double.valueOf(chartHeight)==null){
				plot.getRangeAxis().setRange(-DEFAULT_AUTO_RANGE, DEFAULT_AUTO_RANGE);
				plot.getDomainAxis().setRange(-DEFAULT_AUTO_RANGE, DEFAULT_AUTO_RANGE);
				return;
			}
			
			
			double aspectRatio = chartWidth / chartHeight;
			
			finest("Plot w: "+chartWidth+"; h: "+chartHeight+"; asp: "+aspectRatio);

			// start with impossible values, before finding the real chart values
			double xMin = Double.MAX_VALUE;
			double yMin = Double.MAX_VALUE;
			//		
			double xMax = Double.MIN_VALUE;
			double yMax = Double.MIN_VALUE;

//			finest("Plot has "+plot.getDatasetCount()+" datasets");
			
			// get the max and min values of each dataset in the chart
			for(int i = 0; i<plot.getDatasetCount();i++){
				XYDataset dataset = plot.getDataset(i);

				if(dataset==null){
					finest("Null dataset "+i);
					continue;
				}
				
				// No values in the dataset, skip
				if(DatasetUtilities.findMaximumDomainValue(dataset)==null){
					continue;
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
			
			// If not datasets were found, set defaults
			if(xMin == Double.MAX_VALUE || yMin == Double.MAX_VALUE){
				xMin = -DEFAULT_AUTO_RANGE;
				yMin = -DEFAULT_AUTO_RANGE;
				xMax = DEFAULT_AUTO_RANGE;
				yMax = DEFAULT_AUTO_RANGE;
			}
			

			// find the ranges they cover
			double xRange = xMax - xMin;
			double yRange = yMax - yMin;

			double newXRange = xRange;
			double newYRange = yRange;

			// test the aspect ratio
			if( (xRange / yRange) > aspectRatio){
				// width is not enough
				newXRange = xRange * 1.1;
				newYRange = newXRange / aspectRatio;
			} else {
				// height is not enough
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
			
			if(yMin>=yMax){
				finer("Min and max are equal");
				xMin = -DEFAULT_AUTO_RANGE;
				yMin = -DEFAULT_AUTO_RANGE;
				xMax = DEFAULT_AUTO_RANGE;
				yMax = DEFAULT_AUTO_RANGE;
			} 
			
			plot.getRangeAxis().setRange(yMin, yMax);
			plot.getDomainAxis().setRange(xMin, xMax);
			
		
			

		} catch (Exception e){
			log(Level.FINER,"Error restoring auto bounds, falling back to default", e);
			super.restoreAutoBounds();
		}
	
	}

	@Override
	public void componentHidden(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void componentMoved(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void componentResized(ComponentEvent arg0) {
		restoreAutoBounds();
		
	}

	@Override
	public void componentShown(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
	} 	
}

