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
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.XYPlot;
import org.jfree.ui.RectangleEdge;


import charting.ChartComponents;


/**
 * This class takes a chart and adds a single draggable domain
 * overlay crosshair
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class PositionSelectionChartPanel extends ExportableChartPanel {
	

	private List<Object> listeners = new ArrayList<Object>();
	
	private CrosshairOverlay overlay = null;
	
	private Crosshair xCrosshair;
	
	private volatile boolean mouseIsDown = false;
			

	public PositionSelectionChartPanel(final JFreeChart chart){
		super(chart);
		
		this.setRangeZoomable(false);
		this.setDomainZoomable(false);		
		
		overlay = new CrosshairOverlay();
		int i=0;


		xCrosshair = new Crosshair(Double.NaN, Color.DARK_GRAY, ChartComponents.MARKER_STROKE);
		xCrosshair.setLabelVisible(false);

		xCrosshair.setValue(50);


		overlay.addDomainCrosshair(xCrosshair);
			
		
		
		this.addOverlay(overlay);
	}
	
	public double getDomainCrosshairPosition(){
		
		List<Crosshair> crosshairs = overlay.getDomainCrosshairs();
		for(Crosshair c : crosshairs){
			return c.getValue();
		}
		return 0;
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
	    if (e.getButton() == MouseEvent.BUTTON1) {

	    	if(xCrosshair!=null){
//	    		IJ.log("Mouse down : Running :"+checkRunning()); 
	    		mouseIsDown = true;
	    		initThread();
	    	}
	    }
	}

	public void mouseReleased(MouseEvent e) {
		
		final int x = e.getX();

		
	    if (e.getButton() == MouseEvent.BUTTON1) {
	    	mouseIsDown = false;
	    		
	    	try {
	    		Rectangle2D dataArea = getScreenDataArea();
	    		JFreeChart chart     = getChart();
	    		XYPlot plot          = (XYPlot) chart.getPlot();
	    		ValueAxis xAxis      = plot.getDomainAxis();

	    		int xValue = (int) xAxis.java2DToValue(x, dataArea, 
	    				RectangleEdge.BOTTOM);

	    		fine("Position selection x="+xValue);

	    		fireSignalChangeEvent("UpdatePosition");
	    		

	    	} catch(Exception e1){
	    		IJ.log("Error sending signal: "+e1.getMessage());
	    		for(StackTraceElement e2 : e1.getStackTrace()){
	    			IJ.log(e2.toString());
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

				this.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
				
			} else {
				this.setCursor(Cursor.getDefaultCursor());
			}
		}
	}
	
	private void updateActiveCrosshairLocation(int x, int y){

		Rectangle2D dataArea = getScreenDataArea();
		JFreeChart chart = getChart();
		XYPlot plot = (XYPlot) chart.getPlot();
		ValueAxis xAxis = plot.getDomainAxis();
		double movex = xAxis.java2DToValue(x, dataArea, 
				RectangleEdge.BOTTOM);
		xCrosshair.setValue(movex);
		
	}
	
	private synchronized boolean checkRunning(){
		if (isRunning) 
			return true;
		else 
			return false;
	}

	volatile private boolean isRunning = false;
	private synchronized boolean checkAndMark() {
	    if (isRunning) return false;
	    isRunning = true;
	    return true;
	}
	
	private void initThread( ) {
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
//	                IJ.log("Thread end : Running :"+checkRunning()); 
	            }
	        }.start();
	    } else {
//	    	IJ.log("Not starting thread: Running is "+checkRunning());
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

		List<Crosshair> crosshairs = overlay.getDomainCrosshairs();
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
				xCrosshair = (Crosshair) c;
			}
		}
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
