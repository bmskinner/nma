package gui.components;

import gui.SignalChangeEvent;
import gui.SignalChangeListener;
import ij.IJ;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.MouseInfo;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.chart.panel.Overlay;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.XYPlot;
import org.jfree.ui.RectangleEdge;

import charting.ChartComponents;
import components.generic.SegmentedProfile;
import components.nuclear.NucleusBorderSegment;

@SuppressWarnings("serial")
public class DraggableOverlayChartPanel extends ChartPanel {
	

	private List<Object> listeners = new ArrayList<Object>();
	
	private SegmentedProfile profile = null;
	
//	private Map<CrosshairOverlay, NucleusBorderSegment> lines = new HashMap<CrosshairOverlay, NucleusBorderSegment>(); // drwaing lines on the chart
	private Map<Crosshair, NucleusBorderSegment> lines = new HashMap<Crosshair, NucleusBorderSegment>(); // drwaing lines on the chart
	
	
	private CrosshairOverlay activeOverlay = null;
	private Crosshair activeCrosshair = null;
	
	private volatile boolean mouseIsDown = false;
	

	public DraggableOverlayChartPanel(JFreeChart chart, SegmentedProfile profile){
		super(chart);
		this.profile = profile;
		updateOverlays();
		this.setRangeZoomable(false);
		this.setDomainZoomable(false);		
	}
	
	private void clearOverlays(){
		if(activeOverlay!=null){
			this.removeOverlay(activeOverlay);
		}
//		this.activeCrosshair = null;
	}
	
	private void updateOverlays(){
		/*
		 * Create an x-axis overlay for each segment start
		 */
		
		clearOverlays();
		
		if(profile!=null){
			try {

				activeOverlay = new CrosshairOverlay();
				int i=0;
				for(NucleusBorderSegment seg : profile.getSegments()){

					Color colour = ColourSelecter.getSegmentColor(i++);

					Crosshair xCrosshair = new Crosshair(Double.NaN, colour, ChartComponents.MARKER_STROKE);
					xCrosshair.setLabelVisible(false);
					xCrosshair.setValue(seg.getStartIndex());
					lines.put(xCrosshair, seg);
					
					activeOverlay.addDomainCrosshair(xCrosshair);
					
				}
				
				this.addOverlay(activeOverlay);
				
			} catch (Exception e1) {
				IJ.log("Error sending signal: "+e1.getMessage());
    			for(StackTraceElement e2 : e1.getStackTrace()){
    				IJ.log(e2.toString());
    			}
			}


		}
	}
	
	public void setChart(JFreeChart chart, SegmentedProfile profile){
		super.setChart(chart);
		this.profile = profile;
		IJ.log("Set chart   : Running :"+checkRunning()); 
		updateOverlays();
		
		
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
	    if (e.getButton() == MouseEvent.BUTTON1) {

	    	if(activeCrosshair!=null){
	    		IJ.log("Mouse down : Running :"+checkRunning()); 
	    		mouseIsDown = true;
	    		initThread();
	    	}
	    }
	}

	public void mouseReleased(MouseEvent e) {
		
		final int x = e.getX();
		final int y = e.getY();
		
	    if (e.getButton() == MouseEvent.BUTTON1) {
	    	mouseIsDown = false;
	    	IJ.log("Mouse up   : Running :"+checkRunning()); 
//	    	isRunning = false;
	    	/*
	    	 * Get the location on the chart, and send a signal to update the profile
	    	 */
	    	
	    	if(activeCrosshair!=null){
	    		IJ.log("Mouse up on active crosshair");
	    		
	    		for(Crosshair c : lines.keySet()){
	    			IJ.log(lines.get(c).toString());
	    		}
	    		
	    		try {
	    			Rectangle2D dataArea = getScreenDataArea();
	    			JFreeChart chart = getChart();
	    			XYPlot plot = (XYPlot) chart.getPlot();
	    			ValueAxis xAxis = plot.getDomainAxis();
	    			int xValue = (int) xAxis.java2DToValue(x, dataArea, 
	    					RectangleEdge.BOTTOM);

	    			NucleusBorderSegment seg = lines.get(activeCrosshair);

	    			if(seg!=null){
	    				IJ.log("UpdateSegment|"+seg.getName()+"|"+xValue);
	    				//	    			isRunning = false;
	    				fireSignalChangeEvent("UpdateSegment|"+seg.getName()+"|"+xValue);
	    			}

	    		} catch(Exception e1){
	    			IJ.log("Error sending signal: "+e1.getMessage());
	    			for(StackTraceElement e2 : e1.getStackTrace()){
	    				IJ.log(e2.toString());
	    			}
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
				this.setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));
			} else {
				this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
		}
	}
	
	private void updateActiveCrosshairLocation(int x, int y){

		if(activeCrosshair!=null){

			//					NucleusBorderSegment seg = lines.get(activeCrosshair);
			//					lines.remove(activeCrosshair);

			Rectangle2D dataArea = getScreenDataArea();
			JFreeChart chart = getChart();
			XYPlot plot = (XYPlot) chart.getPlot();
			ValueAxis xAxis = plot.getDomainAxis();
			double movex = xAxis.java2DToValue(x, dataArea, 
					RectangleEdge.BOTTOM);
			activeCrosshair.setValue(movex);
			//					activeOverlay.fireOverlayChanged();
			//			activeOverlay.paintOverlay(arg0, arg1);

			//					lines.put(activeCrosshair, seg);
		}
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
	            	IJ.log("Thread start : Running :"+checkRunning()); 
	                do {
	                	
	                	/*
	            		 * Make the overlay under the mouse follow the mouse
	            		 */
	                	
	                	int x = MouseInfo.getPointerInfo().getLocation().x - getLocationOnScreen().x;
	                    int y = MouseInfo.getPointerInfo().getLocation().y - getLocationOnScreen().y;
	                    updateActiveCrosshairLocation(x, y);

	                	
	                } while (mouseIsDown);
	                isRunning = false;
	                IJ.log("Thread end : Running :"+checkRunning()); 
	            }
	        }.start();
	    } else {
	    	IJ.log("Not starting thread");
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
		activeCrosshair = null;

		boolean isOverLine = false;

		// only display a hand if the cursor is over the items
		for(Crosshair c :  lines.keySet()){


			// Turn the chart coordinates into panel coordinates
			double rectangleX = xAxis.valueToJava2D(c.getValue(), dataArea, 
					RectangleEdge.BOTTOM);


			final Rectangle bounds = new Rectangle( (int)rectangleX-2, 
					(int) dataArea.getMinY(), 
					(int) 4,   
					(int) dataArea.getHeight() );


			if (bounds != null && bounds.contains(x, y)) {
				isOverLine = true;
				activeCrosshair = c;
				if(c==null){
					IJ.log("Error: c is null");
				}
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
