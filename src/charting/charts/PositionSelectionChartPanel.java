package charting.charts;

import gui.SegmentEvent;
import gui.SegmentEventListener;
import gui.SignalChangeEvent;
import gui.SignalChangeListener;
import ij.IJ;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.MouseInfo;
import java.awt.Rectangle;
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
import org.jfree.data.general.DatasetUtilities;
import org.jfree.ui.RectangleEdge;







import charting.ChartComponents;


/**
 * This class takes a chart and adds a single draggable domain
 * rectangle overlay. The overlay moves with the mouse when dragged,
 * and fires a SignalChangeEvent when the overlay is released requesting that
 * listeners update positions based on the new rectangle location.
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class PositionSelectionChartPanel extends ExportableChartPanel {
	

	private List<Object> listeners = new ArrayList<Object>();
	
	protected Overlay overlay = null;
	protected RectangleOverlayObject xRectangle = null;

	
	protected volatile boolean mouseIsDown = false;
	
	protected volatile boolean isRunning = false;
	
	private double rangeWidth;
	private double rangePct = DEFAULT_RANGE_PCT;
	
	private static final int DEFAULT_RANGE_PCT = 10; // 10% of x-range
			

	public PositionSelectionChartPanel(final JFreeChart chart){
		super(chart);
		chart.getXYPlot().getDomainAxis().setVisible(false);
		chart.getXYPlot().getRangeAxis().setVisible(false);
		
		this.setRangeZoomable(false);
		this.setDomainZoomable(false);	
		
		updateRangeWidth();

		overlay = new RectangleOverlay();
		xRectangle = new RectangleOverlayObject(0, rangeWidth);
		((RectangleOverlay) overlay).setDomainRectangle(xRectangle);
		
		
		this.addOverlay(overlay);
	}
		
	public RectangleOverlayObject getDomainRectangleOverlay(){
		return xRectangle;
	}
	
	@Override
	public void setChart(final JFreeChart chart){
		chart.getXYPlot().getDomainAxis().setVisible(false);
		chart.getXYPlot().getRangeAxis().setVisible(false);
		finest("Setting new chart");
		double oldPct = 0;
		if(xRectangle!=null){
			
			double maxX = getChart().getXYPlot().getDomainAxis().getUpperBound();
			double minX = getChart().getXYPlot().getDomainAxis().getLowerBound();
			double fullRange = maxX - minX;
			finest("Chart range "+fullRange+": "+minX+" - "+maxX);
			finest("Rectangle is "+xRectangle.getMinValue()+" - "+xRectangle.getMaxValue());
			oldPct = (xRectangle.getMidValue()-minX) / fullRange;
			finest("Existing rectangle overlay midpoint ("+xRectangle.getMidValue()+") at fraction "+oldPct);
		}
		super.setChart(chart);
		updateRangeWidth();
		if(xRectangle!=null){
			double maxX = getChart().getXYPlot().getDomainAxis().getUpperBound();
			double minX = getChart().getXYPlot().getDomainAxis().getLowerBound();
			double fullRange = maxX - minX;
			finest("New chart range "+fullRange+": "+minX+" - "+maxX);
			
			double newPosition = minX + (oldPct*fullRange);
			
			double halfRange = rangeWidth / 2;
			xRectangle.setMinValue(newPosition-halfRange);
			xRectangle.setMaxValue(newPosition+halfRange);
			finest("Restoring rectangle overlay midpoint to "+newPosition+": "+xRectangle.getMinValue()+" - "+xRectangle.getMaxValue());
			fireSignalChangeEvent("UpdatePosition");
		}
	}
	
	private void updateRangeWidth(){
		
		// Get the x bounds of the plot
		double maxX = getChart().getXYPlot().getDomainAxis().getUpperBound();
		double minX = getChart().getXYPlot().getDomainAxis().getLowerBound();
		
		// Get the size of the total X range
		double fullRange = maxX - minX;
		
		// Find 10% of that (in chart units)
		rangeWidth = fullRange * (rangePct /100);
	}
	
	/**
	 * Set the width of the rectangle overlay if present,
	 * as a percent of the total x-range
	 * @param i
	 */
	public void setRangePct(double i){
		this.rangePct = i;
		updateRangeWidth();
		// get the current rectangle x location
//		if(xRectangle!=null){
//			updateDomainRectangleLocation((int) xRectangle.getMidValue());
//		}
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		
		final int x = e.getX();
		final int y = e.getY();
		
	    if (e.getButton() == MouseEvent.BUTTON1) {

	    	if(xRectangle!=null){

	    		mouseIsDown = true;

	    		if(cursorIsOverMinEdge(x, y)){
	    			initMinThread();
	    		} else {
	    			if(cursorIsOverMaxEdge(x, y)){
	    				initMaxThread();
	    			} else {

	    				if (cursorIsOverRectangle(x, y)) {
	    					//	    			log("Cursor is over rectangle");

	    					initThread();
	    				} else {

	    					// Move the rectangle directly over the mouse
	    					updateDomainRectangleLocation(x);

	    				}
	    			}
	    		}
	    	}
	    }
	}

	public void mouseReleased(MouseEvent e) {
		
	    if (e.getButton() == MouseEvent.BUTTON1) {
//	    	log("Mouse released, updating listening charts");
	    	mouseIsDown = false;
//	    	overlayMoving = false;

	    	// Tell listening charts to update
	    	setRangePct( getRangeCurrentPercent());
	    	
	    	fireSignalChangeEvent("UpdatePosition");
	    }
	    
	}
	
	@Override
	public void mouseMoved(MouseEvent e){
		
		final int x = e.getX();
		final int y = e.getY();
				
		if( ! mouseIsDown){ // Mouse is up
			
			if(cursorIsOverMinEdge(x, y)  || cursorIsOverMaxEdge(x, y) ){
				this.setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
			} else {

				if (cursorIsOverRectangle(x, y)) {

					this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

				} else {
					this.setCursor(Cursor.getDefaultCursor());
				}
			}
		}
	}
	
	private void updateDomainRectangleLocation(int x){

		Rectangle2D dataArea = getScreenDataArea();
		JFreeChart  chart    = getChart();
		XYPlot      plot     = (XYPlot) chart.getPlot();
		ValueAxis   xAxis    = plot.getDomainAxis();
		
		double xUpper = xAxis.getUpperBound();
				
		double halfRange = rangeWidth / 2;
		
		double xValue = xAxis.java2DToValue(x, dataArea, 
				RectangleEdge.BOTTOM);
		
		
		// Find the chart values to use as the rectangle range 
		double moveMin = xValue - halfRange;
		moveMin = moveMin < 0 ? 0 : moveMin; // correct for zero end
		
		moveMin = moveMin > xUpper - rangeWidth
				? xUpper - rangeWidth
				: moveMin; // correct for upper end
		
		double moveMax = xValue + halfRange;
		moveMax = moveMax < rangeWidth ? rangeWidth : moveMax; // correct for zero end
		moveMax = moveMax > xUpper ? xUpper	: moveMax; // correct for upper end
						
		// Set the values in chart units
		xRectangle.setMinValue(moveMin);
		xRectangle.setMaxValue(moveMax);
//		finest("Set rectangle min and max: "+moveMin+" & "+moveMax);
		fireSignalChangeEvent("UpdatePosition");
	}
	
	private void updateDomainRectangleSize(int x, boolean isMin){

		Rectangle2D dataArea = getScreenDataArea();
		JFreeChart  chart    = getChart();
		XYPlot      plot     = (XYPlot) chart.getPlot();
		ValueAxis   xAxis    = plot.getDomainAxis();
		double xUpper        = xAxis.getUpperBound();
		double xLower        = xAxis.getLowerBound();
		double xRange        = xUpper - xLower;
				
		double xValue = xAxis.java2DToValue(x, dataArea, 
				RectangleEdge.BOTTOM);
		

		if(isMin){
			xValue = xValue < 0 ? 0 : xValue; // correct for zero end

			xValue = xValue >= xRectangle.getMaxValue() ? xRectangle.getMaxValue() - (xRange/100) : xValue; // correct for max end

			// Set the values in chart units
			xRectangle.setMinValue(xValue);
		} else {
			
			xValue = xValue >= xUpper ? xUpper : xValue; // correct for upper end

			xValue = xValue <= xRectangle.getMinValue() ? xRectangle.getMinValue() + (xRange/100) : xValue; // correct for min end

			// Set the values in chart units
			xRectangle.setMaxValue(xValue);
			
			
		}
		
		
		

//		finest("Set rectangle min and max: "+moveMin+" & "+moveMax);
		fireSignalChangeEvent("UpdatePosition");
	}
	
	private double getRangeCurrentPercent(){
		double range = xRectangle.getMaxValue() - xRectangle.getMinValue();
		JFreeChart  chart    = getChart();
		XYPlot      plot     = (XYPlot) chart.getPlot();
		ValueAxis   xAxis    = plot.getDomainAxis();
		double xUpper        = xAxis.getUpperBound();
		double xLower        = xAxis.getLowerBound();
		double xRange        = xUpper - xLower;
		double newPct = (range / xRange) *100;
		
		return newPct;
	}
	
		
	protected synchronized boolean checkAndMark() {
	    if (isRunning) return false;
	    isRunning = true;
	    return true;
	}
//	
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
	                    
	                    // Move the box with the mouse
	                    updateDomainRectangleLocation(x);

	                } while (mouseIsDown);
	                isRunning = false;
//	                IJ.log("Thread end : Running :"+checkRunning()); 
	            }
	        }.start();
	    } else {
//	    	IJ.log("Not starting thread: Running is "+checkRunning());
	    }
	}
	
	protected void initMinThread( ) {
	    if (checkAndMark()) {
	        new Thread() {
	            public void run() {
//	            	IJ.log("Thread start : Running :"+checkRunning()); 
	                do {
	                	
	                	/*
	            		 * Make the overlay under the mouse follow the mouse
	            		 */
	                	
	                	int x = MouseInfo.getPointerInfo().getLocation().x - getLocationOnScreen().x;
	                    
	                    // Move the box with the mouse
	                    updateDomainRectangleSize(x, true);

	                } while (mouseIsDown);
	                isRunning = false;
//	                IJ.log("Thread end : Running :"+checkRunning()); 
	            }
	        }.start();
	    } else {
//	    	IJ.log("Not starting thread: Running is "+checkRunning());
	    }
	}
	
	protected void initMaxThread( ) {
	    if (checkAndMark()) {
	        new Thread() {
	            public void run() {
//	            	IJ.log("Thread start : Running :"+checkRunning()); 
	                do {
	                	
	                	/*
	            		 * Make the overlay under the mouse follow the mouse
	            		 */
	                	
	                	int x = MouseInfo.getPointerInfo().getLocation().x - getLocationOnScreen().x;
	                    
	                    // Move the box with the mouse
	                    updateDomainRectangleSize(x, false);

	                } while (mouseIsDown);
	                isRunning = false;
	            }
	        }.start();
	    } else {
	    	
	    }
	}
	
	/**
	 * Checks if the given cursor position is over the
	 * rectangle overlay
	 * @param x the screen x coordinate
	 * @param y the screen y coordinate
	 * @return
	 */
	private boolean cursorIsOverRectangle(int x, int y){
		Rectangle2D dataArea = this.getScreenDataArea(); 
		ValueAxis xAxis = this.getChart().getXYPlot().getDomainAxis();
//		xCrosshair = null;

		boolean isOverLine = false;



		// Turn the chart coordinates into screen coordinates
		double rectangleMinX = xAxis.valueToJava2D(xRectangle.getMinValue(), dataArea, 
				RectangleEdge.BOTTOM);

		double rectangleMaxX = xAxis.valueToJava2D(xRectangle.getMaxValue(), dataArea, 
				RectangleEdge.BOTTOM);

		double rectangleW = rectangleMaxX - rectangleMinX;


		final Rectangle bounds = new Rectangle( (int)rectangleMinX, 
				(int) dataArea.getMinY(), 
				(int) rectangleW,   
				(int) dataArea.getHeight() );
		

//		log(x+" "+y+" Bounds: "+bounds.toString());


		if (bounds != null && bounds.contains(x, y)) {
			isOverLine = true;
		}
//		
		return isOverLine;
	}
	
	public boolean cursorIsOverMinEdge(int x, int y){
		Rectangle2D dataArea = this.getScreenDataArea(); 
		ValueAxis xAxis = this.getChart().getXYPlot().getDomainAxis();
		boolean isOverEdge = false;
		
		// Turn the chart coordinates into screen coordinates
		double rectangleMinX = xAxis.valueToJava2D(xRectangle.getMinValue(), dataArea, 
						RectangleEdge.BOTTOM)-2;
		
		double rectangleMaxX = xAxis.valueToJava2D(xRectangle.getMinValue(), dataArea, 
				RectangleEdge.BOTTOM)+2;

		double rectangleW = rectangleMaxX - rectangleMinX;
		
		final Rectangle bounds = new Rectangle( (int)rectangleMinX, 
				(int) dataArea.getMinY(), 
				(int) rectangleW,   
				(int) dataArea.getHeight() );

		if (bounds != null && bounds.contains(x, y)) {
			isOverEdge = true;
		}
		return isOverEdge;
		
	}
	
	public boolean cursorIsOverMaxEdge(int x, int y){
		Rectangle2D dataArea = this.getScreenDataArea(); 
		ValueAxis xAxis = this.getChart().getXYPlot().getDomainAxis();
		boolean isOverEdge = false;
		
		// Turn the chart coordinates into screen coordinates
		double rectangleMinX = xAxis.valueToJava2D(xRectangle.getMaxValue(), dataArea, 
						RectangleEdge.BOTTOM)-2;
		
		double rectangleMaxX = xAxis.valueToJava2D(xRectangle.getMaxValue(), dataArea, 
				RectangleEdge.BOTTOM)+2;

		double rectangleW = rectangleMaxX - rectangleMinX;
		
		final Rectangle bounds = new Rectangle( (int)rectangleMinX, 
				(int) dataArea.getMinY(), 
				(int) rectangleW,   
				(int) dataArea.getHeight() );

		if (bounds != null && bounds.contains(x, y)) {
			isOverEdge = true;
		}
		return isOverEdge;
		
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

}
