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
 * rectangle overlay
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class PositionSelectionChartPanel extends ExportableChartPanel {
	

	private List<Object> listeners = new ArrayList<Object>();
	
	protected Overlay overlay = null;
	protected RectangleOverlayObject xRectangle;
	
	
//	protected Crosshair xCrosshair;
	
	protected volatile boolean mouseIsDown = false;
	
	protected volatile boolean isRunning = false;
	
	protected int rangeWidth = 20;
			

	public PositionSelectionChartPanel(final JFreeChart chart){
		super(chart);
		
		this.setRangeZoomable(false);
		this.setDomainZoomable(false);		
		
//		
		
		overlay = new RectangleOverlay();
		xRectangle = new RectangleOverlayObject(40, 60);
		((RectangleOverlay) overlay).setDomainRectangle(xRectangle);
		
		
//		overlay = new CrosshairOverlay();
//
//		xCrosshair = new Crosshair(Double.NaN, Color.DARK_GRAY, ChartComponents.MARKER_STROKE);
//		xCrosshair.setLabelVisible(false);
////
//		xCrosshair.setValue(50);
//
//
//		overlay.addDomainCrosshair(xCrosshair);
			
		
		
		this.addOverlay(overlay);
	}
	
//	public double getDomainCrosshairPosition(){
//		
//		if(xCrosshair!=null){
//			finest("Domain value is "+xCrosshair.getValue());
//			return xCrosshair.getValue();
//		} 
//		return 0;
//
//	}
	
	public RectangleOverlayObject getDomainRectangleOverlay(){
		return xRectangle;
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
	    if (e.getButton() == MouseEvent.BUTTON1) {

	    	if(xRectangle!=null){
//	    		IJ.log("Mouse down : Running :"+checkRunning()); 
	    		mouseIsDown = true;
	    		initThread();
	    	}
	    }
	}

	public void mouseReleased(MouseEvent e) {
		
//		final int x = e.getX();

		
	    if (e.getButton() == MouseEvent.BUTTON1) {
	    	mouseIsDown = false;
//	    		
//	    	try {
//	    		Rectangle2D dataArea = getScreenDataArea();
//	    		JFreeChart chart     = getChart();
//	    		XYPlot plot          = (XYPlot) chart.getPlot();
//	    		ValueAxis xAxis      = plot.getDomainAxis();
//
//	    		int xValue = (int) xAxis.java2DToValue(x, dataArea, 
//	    				RectangleEdge.BOTTOM);
//
//	    		fine("Position selection x="+xValue);

	    		fireSignalChangeEvent("UpdatePosition");
	    		

//	    	} catch(Exception e1){
//	    		IJ.log("Error sending signal: "+e1.getMessage());
//	    		for(StackTraceElement e2 : e1.getStackTrace()){
//	    			IJ.log(e2.toString());
//	    		}
//	    	}

	    		

	    }
	}
	
	@Override
	public void mouseMoved(MouseEvent e){
		
		final int x = e.getX();
		final int y = e.getY();
		
		if(mouseIsDown){

	
		} else {

//			if (checkCursorIsOverLine(x, y)) {
//
//				this.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
//				
//			} else {
//				this.setCursor(Cursor.getDefaultCursor());
//			}
		}
	}
	
	private void updateDomainRectangleLocation(int x){

		Rectangle2D dataArea = getScreenDataArea();
		JFreeChart chart = getChart();
		XYPlot plot = (XYPlot) chart.getPlot();
		ValueAxis xAxis = plot.getDomainAxis();
		
		
		int halfRange = rangeWidth / 2;
		
		double xValue = xAxis.java2DToValue(x, dataArea, 
				RectangleEdge.BOTTOM);
		
		double moveMin = xValue - halfRange;
		moveMin = moveMin < 0 ? 0 : moveMin;
		
		double moveMax = xValue + halfRange;
		
		log("Click centre at "+x+" -> "+xValue);
		log("Setting rectangle from click to "+moveMin+" - "+moveMax );
				
		
		xRectangle.setMinValue(moveMin);
		xRectangle.setMaxValue(moveMax);
		
	}
	
	
	private synchronized boolean checkRunning(){
		if (isRunning) 
			return true;
		else 
			return false;
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
	                    
	                    updateDomainRectangleLocation(x);
//	                    updateActiveCrosshairLocation(x, y);

	                	
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
//		xCrosshair = null;

		boolean isOverLine = false;



			// Turn the chart coordinates into panel coordinates
			double rectangleMinX = xAxis.valueToJava2D(xRectangle.getMinValue(), dataArea, 
					RectangleEdge.BOTTOM);
			
			double rectangleMaxX = xAxis.valueToJava2D(xRectangle.getMaxValue(), dataArea, 
					RectangleEdge.BOTTOM);
			
			double rectangleW = rectangleMaxX - rectangleMinX;


			final Rectangle bounds = new Rectangle( (int)xRectangle.getMinValue()-2, 
					(int) dataArea.getMinY(), 
					(int) rectangleW,   
					(int) dataArea.getHeight() );


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
