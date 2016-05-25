package gui.components;

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

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.chart.panel.Overlay;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.XYPlot;
import org.jfree.ui.RectangleEdge;



import charting.ChartComponents;


/**
 * This class takes a chart and adds a single draggable domain
 * rectangle overlay. The overlay moves with the mouse when dragged,
 * and fires a SignalChangeEvent when the overlay is released.
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class PositionSelectionChartPanel extends ExportableChartPanel {
	

	private List<Object> listeners = new ArrayList<Object>();
	
	protected Overlay overlay = null;
	protected RectangleOverlayObject xRectangle;

	
	protected volatile boolean mouseIsDown = false;
	
	protected volatile boolean isRunning = false;
	
	protected int rangeWidth = DEFAULT_RANGE_WIDTH;
	
	private static final int DEFAULT_RANGE_WIDTH = 20;
			

	public PositionSelectionChartPanel(final JFreeChart chart){
		super(chart);
		
		this.setRangeZoomable(false);
		this.setDomainZoomable(false);		

		overlay = new RectangleOverlay();
		xRectangle = new RectangleOverlayObject(40, 60);
		((RectangleOverlay) overlay).setDomainRectangle(xRectangle);
		
		
		this.addOverlay(overlay);
	}
		
	public RectangleOverlayObject getDomainRectangleOverlay(){
		return xRectangle;
	}
	
	/**
	 * Set the width of the rectangle overlay, if present
	 * @param i
	 */
	public void setRangeWidth(int i){
		this.rangeWidth = i;
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		
		final int x = e.getX();
		final int y = e.getY();
		
	    if (e.getButton() == MouseEvent.BUTTON1) {

	    	if(xRectangle!=null){
//	    		IJ.log("Mouse down : Running :"+checkRunning()); 
//	    		log("Mouse down");
	    		mouseIsDown = true;
	    		
	    		if (cursorIsOverRectangle(x, y)) {
//	    			log("Cursor is over rectangle");
	    		
	    			initThread();
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
	    	fireSignalChangeEvent("UpdatePosition");
	    }
	    
	}
	
	@Override
	public void mouseMoved(MouseEvent e){
		
		final int x = e.getX();
		final int y = e.getY();
				
		if( ! mouseIsDown){ // Mouse is up

			if (cursorIsOverRectangle(x, y)) {

				this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				
			} else {
				this.setCursor(Cursor.getDefaultCursor());
			}
		}
	}
	
	private void updateDomainRectangleLocation(int x){

		Rectangle2D dataArea = getScreenDataArea();
		JFreeChart chart = getChart();
		XYPlot plot = (XYPlot) chart.getPlot();
		ValueAxis xAxis = plot.getDomainAxis();
		
		double xUpper = xAxis.getUpperBound();
		
		
		int halfRange = rangeWidth / 2;
		
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
	                    int y = MouseInfo.getPointerInfo().getLocation().y - getLocationOnScreen().y;
	                	
//	        				log("Updating rectangle location: "+x+" "+y);
	                    
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
