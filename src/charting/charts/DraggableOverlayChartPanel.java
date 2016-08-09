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

import gui.components.ColourSelecter;
import ij.IJ;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.MouseInfo;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.XYPlot;
import org.jfree.ui.RectangleEdge;

import charting.ChartComponents;
import components.generic.SegmentedProfile;
import components.nuclear.NucleusBorderSegment;

/**
 * This chart panel provides a list of draggable markers as
 * crosshair overlays, corresponding to the start positions of
 * segments in a segmented profile
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class DraggableOverlayChartPanel extends PositionSelectionChartPanel {
		
	private SegmentedProfile profile = null;

	private List<SegmentCrosshair> crosses = new ArrayList<SegmentCrosshair>(); // drawing lines on the chart

	protected Crosshair xCrosshair;
	
	private boolean isChartNormalised = false;
		

	public DraggableOverlayChartPanel(final JFreeChart chart, final SegmentedProfile profile, boolean normalised){
		super(chart);
		chart.getXYPlot().getDomainAxis().setVisible(true);
		chart.getXYPlot().getRangeAxis().setVisible(true);
		this.profile = profile;
		this.isChartNormalised = normalised;
		updateOverlays();
		this.setRangeZoomable(false);
		this.setDomainZoomable(false);		
	}
	
	/**
	 * Toggle wait cursor on element
	 * @param b
	 */
	public void setAnalysing(boolean b){
		if(b){
			this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		} else {
			this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}
	
	private void clearOverlays(){
		if(overlay!=null){
			this.removeOverlay(overlay);
		}
	}
	
	public double getDomainCrosshairPosition(){
	
	if(xCrosshair!=null){
		finest("Domain value is "+xCrosshair.getValue());
		return xCrosshair.getValue();
	} 
	return 0;

}
	
	private void updateOverlays(){
		/*
		 * Create an x-axis overlay for each segment start
		 */
		
		clearOverlays();
		
		if(profile!=null){
			try {

				overlay = new CrosshairOverlay();
				int i=0;
				for(NucleusBorderSegment seg : profile.getOrderedSegments()){

					Color colour = ColourSelecter.getColor(i++);
					if(seg.isStartPositionLocked()){
						colour = Color.DARK_GRAY;
					}

					SegmentCrosshair xCrosshair = new SegmentCrosshair(Double.NaN, colour, ChartComponents.MARKER_STROKE, seg);
					xCrosshair.setLabelVisible(false);
					
					double value = isChartNormalised ? profile.getRescaledIndex(seg.getStartIndex(), 100) : seg.getStartIndex();
					
					xCrosshair.setValue(value);
					crosses.add(xCrosshair);
					
					((CrosshairOverlay) overlay).addDomainCrosshair(xCrosshair);
					
				}
				
				this.addOverlay(overlay);
				
			} catch (Exception e1) {
				IJ.log("Error sending signal: "+e1.getMessage());
    			for(StackTraceElement e2 : e1.getStackTrace()){
    				IJ.log(e2.toString());
    			}
			}


		}
	}
	
	public void setChart(JFreeChart chart, SegmentedProfile profile, boolean normalised){
		super.setChart(chart);
		chart.getXYPlot().getDomainAxis().setVisible(true);
		chart.getXYPlot().getRangeAxis().setVisible(true);
		clearOverlays();
		this.profile = profile;
		this.isChartNormalised = normalised;
		crosses = new ArrayList<SegmentCrosshair>();
		overlay = null;
//		xCrosshair = null;
		updateOverlays();
	}
	
	@Override
	public void setChart(JFreeChart chart){
		this.setChart(chart, null, true);
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
	    if (e.getButton() == MouseEvent.BUTTON1) {

	    	
	    	if(xCrosshair!=null && ! ((SegmentCrosshair) xCrosshair).getSegment().isStartPositionLocked()){
//	    		IJ.log("Mouse down : Running :"+checkRunning()); 
	    		mouseIsDown = true;
	    		initThread();
	    	}
	    }
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		
//		final int x = e.getX();
//		final int y = e.getY();
		
	    if (e.getButton() == MouseEvent.BUTTON1) {
	    	mouseIsDown = false;
//	    	IJ.log("Mouse up   : Running :"+checkRunning()); 
//	    	isRunning = false;
	    	/*
	    	 * Get the location on the chart, and send a signal to update the profile
	    	 */
	    	
	    	if(xCrosshair!=null){
	    		
	    		// Get the normalised position
	    		double xValue = this.getDomainCrosshairPosition();
	    		
	    			    		
	    		fine("Double of domain value is "+xValue);
	    		
	    		// Correct for normalisation
	    		if(isChartNormalised){
    				
	    			if(xValue<0){
    					xValue += 100; // Wrap values below 0
    				}
    				
    				if(xValue>=100){ // Wrap values above 100
    					xValue -= 100;
    				}
    				
	    			xValue = profile.size() * ( xValue / 100);

    				fine("Profile position of domain value is "+xValue);
    			}
	    		
	    		// Get the closest integer to the selected point
	    		int intXValue = (int) Math.round(xValue); 
	    		
	    		fine("Integer of domain value is "+intXValue);
	    		
	    		// Get the segment associated with the point
	    		NucleusBorderSegment seg = ((SegmentCrosshair) xCrosshair).getSegment();

	    		    		
	    		// Trigger the update
	    		if(seg!=null){
	    			fireSignalChangeEvent("UpdateSegment|"+seg.getMidpointIndex()+"|"+intXValue);
	    		}
	    	}
	    }
	}
	
	@Override
	public void mouseMoved(MouseEvent e){
		
		final int x = e.getX();
		final int y = e.getY();
		
		if(mouseIsDown){

	
		} else {

			if (checkCursorIsOverLine(x, y)) {
				
				if(((SegmentCrosshair) xCrosshair).getSegment().isStartPositionLocked()){
					this.setCursor(Cursor.getDefaultCursor());
				} else {
					this.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
				}
			} else {
				this.setCursor(Cursor.getDefaultCursor());
			}
		}
	}
	
	private void updateActiveCrosshairLocation(int x, int y){

		if(xCrosshair!=null){

			Rectangle2D dataArea = getScreenDataArea();
			JFreeChart chart = getChart();
			XYPlot plot = (XYPlot) chart.getPlot();
			ValueAxis xAxis = plot.getDomainAxis();
			double movex = xAxis.java2DToValue(x, dataArea, 
					RectangleEdge.BOTTOM);
			xCrosshair.setValue(movex);

		}
	}
	
	protected synchronized boolean checkAndMark() {
		if (isRunning) return false;
		isRunning = true;
		return true;
	}
	
	protected void initThread( ) {
	    if (checkAndMark()) {
	        new Thread() {
	            public void run() {
//	            	IJ.log("Thread start : Running :"+checkRunning()); 
	                do {
	                	
	                	/*
	            		 * Make the overlay under the mouse follow the mouse
	            		 */
	                	
	                	int x = MouseInfo.getPointerInfo().getLocation().x - getLocationOnScreen().x;
	                    int y = MouseInfo.getPointerInfo().getLocation().y - getLocationOnScreen().y;
	                    updateActiveCrosshairLocation(x, y);

	                	
	                } while (mouseIsDown);
	                isRunning = false;

	            }
	        }.start();
	    }
	}
	
	/**
	 * Checks if the given cursor position is over one of the 
	 * crosshair overlays for the chart
	 * @param x
	 * @param y
	 * @return
	 */
	private boolean checkCursorIsOverLine(int x, int y){
		Rectangle2D dataArea = this.getScreenDataArea(); 
		ValueAxis xAxis = this.getChart().getXYPlot().getDomainAxis();
		xCrosshair = null;

		boolean isOverLine = false;

		List<Crosshair> crosshairs = ((CrosshairOverlay) overlay).getDomainCrosshairs();
		// only display a hand if the cursor is over the items
		for(Crosshair c : crosshairs ){


			// Turn the chart coordinates into panel coordinates
			double rectangleX = xAxis.valueToJava2D(c.getValue(), dataArea, 
					RectangleEdge.BOTTOM);


			final Rectangle bounds = new Rectangle( (int)rectangleX-2, 
					(int) dataArea.getMinY(), 
					(int) 4,   
					(int) dataArea.getHeight() );


			if (bounds != null && bounds.contains(x, y)) {
				isOverLine = true;
				xCrosshair = (SegmentCrosshair) c;
			}
		}
		return isOverLine;
	}
	
    
    private class SegmentCrosshair extends Crosshair {
    	private NucleusBorderSegment segment;

    	public SegmentCrosshair(double d, Paint paint, Stroke stroke, NucleusBorderSegment segment){
    		super(d, paint, stroke);
    		this.segment = segment;
    	}
		public NucleusBorderSegment getSegment() {
			return segment;
		}
    }

}
