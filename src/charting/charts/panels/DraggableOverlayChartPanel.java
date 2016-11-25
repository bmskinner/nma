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
package charting.charts.panels;

import gui.SegmentEvent;
import gui.SegmentEventListener;
import gui.SignalChangeEvent;
import gui.SignalChangeListener;
import gui.components.ColourSelecter;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.MouseInfo;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.chart.panel.Overlay;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.XYPlot;
import org.jfree.ui.RectangleEdge;

import charting.ChartComponents;
import components.generic.ISegmentedProfile;
import components.generic.SegmentedProfile;
import components.nuclear.IBorderSegment;

/**
 * This chart panel provides a list of draggable markers as
 * crosshair overlays, corresponding to the start positions of
 * segments in a segmented profile. A dragged segment fires a 
 * SegmentEvent to registered listeners
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class DraggableOverlayChartPanel extends ExportableChartPanel {
		
	private volatile ISegmentedProfile profile = null;

	private volatile List<SegmentCrosshair> crosses = new ArrayList<SegmentCrosshair>(); // drawing lines on the chart

	protected volatile Crosshair xCrosshair;
	
	private volatile boolean isChartNormalised = false;
	
	protected volatile Overlay overlay = null;
		

	public DraggableOverlayChartPanel(final JFreeChart chart, final ISegmentedProfile profile, boolean normalised){
		super(chart);
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
	public synchronized void setAnalysing(boolean b){
		if(b){
			this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		} else {
			this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}
	
	private synchronized void clearOverlays(){
		if(overlay!=null){
			this.removeOverlay(overlay);
		}
	}

	public synchronized double getDomainCrosshairPosition(){

		if(xCrosshair!=null){
			finest("Domain value is "+xCrosshair.getValue());
			return xCrosshair.getValue();
		} 
		return 0;

	}
	
	private synchronized void updateOverlays(){
		/*
		 * Create an x-axis overlay for each segment start
		 */
		
		clearOverlays();
		
		if(profile!=null){
			try {

				overlay = new CrosshairOverlay();
				int i=0;
				for(IBorderSegment seg : profile.getOrderedSegments()){

					Paint colour = ColourSelecter.getColor(i++);
					if(seg.isLocked()){
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
				error("Error sending signal", e1);
			}

			this.revalidate();
			this.repaint();
		}
	}
	
	public synchronized void setChart(JFreeChart chart, ISegmentedProfile profile, boolean normalised){
		super.setChart(chart);

		setProfile(profile, normalised);
	}
	
	public synchronized void setProfile(ISegmentedProfile profile, boolean normalised){
		
		clearOverlays();
		this.profile = profile;
		this.isChartNormalised = normalised;
		crosses = new ArrayList<SegmentCrosshair>();
		overlay = null;
		updateOverlays();
		finer("Profile has been set");
	}
	
	@Override
	public synchronized void setChart(JFreeChart chart){
		super.setChart(chart);
	}
	
	@Override
	public synchronized void mousePressed(MouseEvent e) {
	    if (e.getButton() == MouseEvent.BUTTON1) {

	    	
	    	if(xCrosshair!=null && ! ((SegmentCrosshair) xCrosshair).getSegment().isLocked()){
//	    		IJ.log("Mouse down : Running :"+checkRunning()); 
	    		mouseIsDown = true;
	    		initThread();
	    	}
	    }
	}

	@Override
	public synchronized void mouseReleased(MouseEvent e) {
		
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
	    		IBorderSegment seg = ((SegmentCrosshair) xCrosshair).getSegment();

	    		    		
	    		// Trigger the update
	    		if(seg!=null){
	    			fireSegmentEvent(seg.getID(), intXValue, SegmentEvent.MOVE_START_INDEX);
	    		}
	    	}
	    }
	}
	
	@Override
	public synchronized void mouseMoved(MouseEvent e){
		
		final int x = e.getX();
		final int y = e.getY();
		
		if(mouseIsDown){

	
		} else {

			if (checkCursorIsOverLine(x, y)) {
				
				if(((SegmentCrosshair) xCrosshair).getSegment().isLocked()){
					this.setCursor(Cursor.getDefaultCursor());
				} else {
					this.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
				}
			} else {
				this.setCursor(Cursor.getDefaultCursor());
			}
		}
	}
	
	private synchronized void updateActiveCrosshairLocation(int x, int y){

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
	
	public synchronized void addSegmentEventListener(SegmentEventListener l){
		listeners.add(l);
	}

	public synchronized void removeSegmentEventListener(SegmentEventListener l){
		listeners.remove(l);
	}

	protected synchronized void fireSegmentEvent(SegmentEvent e){
		for(Object l : listeners){
			((SegmentEventListener) l).segmentEventReceived(e);
		}
	}
	
	/**
	 * Fire a segment event
	 * @param id the segment ID
	 * @param index the new index
	 * @param type the type of change to make
	 */
	protected synchronized void fireSegmentEvent(UUID id, int index, int type){
		SegmentEvent e = new SegmentEvent(this, id, index, type);
		
		for(Object l : listeners){
			((SegmentEventListener) l).segmentEventReceived(e);
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

		
		if(overlay instanceof CrosshairOverlay){
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
		}
		
		return isOverLine;
	}
	
    
    private class SegmentCrosshair extends Crosshair {
    	private IBorderSegment segment;

    	public SegmentCrosshair(double d, Paint paint, Stroke stroke, IBorderSegment segment){
    		super(d, paint, stroke);
    		this.segment = segment;
    	}
		public IBorderSegment getSegment() {
			return segment;
		}
    }
    
    protected synchronized void fireSignalChangeEvent(String message) {
    	
        SignalChangeEvent event = new SignalChangeEvent( this, message, this.getClass().getSimpleName());
        Iterator<Object> iterator = listeners.iterator();
        while( iterator.hasNext() ) {
            ( (SignalChangeListener) iterator.next() ).signalChangeReceived( event );
        }
    }

    public synchronized void addSignalChangeListener( SignalChangeListener l ) {
        listeners.add( l );
    }
    
    public synchronized void removeSignalChangeListener( SignalChangeListener l ) {
        listeners.remove( l );
    }

}
