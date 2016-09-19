package charting.charts;

import gui.SegmentEvent;
import gui.SegmentEventListener;
import gui.SignalChangeEvent;
import gui.SignalChangeListener;
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
import org.jfree.chart.panel.Overlay;
import org.jfree.chart.plot.XYPlot;
import org.jfree.ui.RectangleEdge;


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
	
	private double domainWidth;
	private double domainPct = DEFAULT_DOMAIN_PCT;
	
	private double rangeWidth; // the width of the range box in chart units
	private double rangePct = DEFAULT_RANGE_PCT; // the width of the range box as a percent of the total
	
	private static final int DEFAULT_DOMAIN_PCT = 10; // 10% of x-range
	private static final int DEFAULT_RANGE_PCT  = 100; // 100% of y-range		

	public PositionSelectionChartPanel(final JFreeChart chart){
		super(chart);
		chart.getXYPlot().getDomainAxis().setVisible(false);
		chart.getXYPlot().getRangeAxis().setVisible(false);
		
		this.setRangeZoomable(false);
		this.setDomainZoomable(false);	
		
		updateDomainWidth();
		updateRangeWidth();

		overlay = new RectangleOverlay();
		xRectangle = new RectangleOverlayObject(0, domainWidth, 0, rangeWidth);
		((RectangleOverlay) overlay).setDomainRectangle(xRectangle);
		
		
		this.addOverlay(overlay);
	}
		
	public RectangleOverlay getRectangleOverlay(){
		return (RectangleOverlay) overlay;
	}
	
	@Override
	public void setChart(final JFreeChart chart){
		chart.getXYPlot().getDomainAxis().setVisible(true);
		chart.getXYPlot().getRangeAxis().setVisible(true);
		finest("Setting new chart");
		double oldXPct = 0;
		double oldYPct = 0;
		
		if(xRectangle!=null){
			
			double maxX = getChart().getXYPlot().getDomainAxis().getUpperBound();
			double minX = getChart().getXYPlot().getDomainAxis().getLowerBound();
			double fullXRange = maxX - minX;
			
			double maxY = getChart().getXYPlot().getRangeAxis().getUpperBound();
			double minY = getChart().getXYPlot().getRangeAxis().getLowerBound();
			double fullYRange = maxY - minY;
//			finest("Chart range "+fullXRange+": "+minX+" - "+maxX);
			finest("Rectangle is x: "+xRectangle.getXMinValue()+" - "+xRectangle.getXMaxValue()+"; y: "+xRectangle.getYMidValue()+" - "+xRectangle.getYMaxValue());
			oldXPct = (xRectangle.getXMidValue()-minX) / fullXRange;
			oldYPct = (xRectangle.getYMidValue()-minY) / fullYRange;
			
			finest("Existing rectangle overlay midpoint ("+xRectangle.getXMidValue()+") at fraction "+oldXPct);
		}
		super.setChart(chart);
		updateDomainWidth();
		updateRangeWidth();
		
		if(xRectangle!=null){
			double maxX = getChart().getXYPlot().getDomainAxis().getUpperBound();
			double minX = getChart().getXYPlot().getDomainAxis().getLowerBound();
			double fullXRange = maxX - minX;
			finest("New chart range "+fullXRange+": "+minX+" - "+maxX);
			
			double maxY = getChart().getXYPlot().getRangeAxis().getUpperBound();
			double minY = getChart().getXYPlot().getRangeAxis().getLowerBound();
			double fullYRange = maxY - minY;
			
			double newXPosition = minX + (oldXPct*fullXRange);
			double newYPosition = minY + (oldYPct*fullYRange);
			
			double halfXRange = domainWidth / 2;
			xRectangle.setXMinValue(newXPosition-halfXRange);
			xRectangle.setXMaxValue(newXPosition+halfXRange);
			
			double halfYRange = rangeWidth / 2;
			xRectangle.setYMinValue(newYPosition-halfYRange);
			xRectangle.setYMaxValue(newYPosition+halfYRange);
			
			finest("Restoring rectangle overlay midpoint to "+newXPosition+": "+xRectangle.getXMinValue()+" - "+xRectangle.getXMaxValue());
			fireSignalChangeEvent("UpdatePosition");
		}
	}
	
	private void updateDomainWidth(){
		
		// Get the x bounds of the plot
		double max = getChart().getXYPlot().getDomainAxis().getUpperBound();
		double min = getChart().getXYPlot().getDomainAxis().getLowerBound();
		
		// Get the size of the total X range
		double fullRange = max - min;
		
		// Find 10% of that (in chart units)
		domainWidth = fullRange * (domainPct /100);
	}
	
	
	private void updateRangeWidth(){
		
		// Get the bounds of the plot
		double max = getChart().getXYPlot().getRangeAxis().getUpperBound();
		double min = getChart().getXYPlot().getRangeAxis().getLowerBound();
		
		// Get the size of the total range
		double fullRange = max - min;
		
		// Find % of that (in chart units)
		rangeWidth = fullRange * (rangePct /100);
	}
	
	/**
	 * Set the width of the rectangle overlay if present,
	 * as a percent of the total x-range
	 * @param i
	 */
	public void setDomainPct(double i){
		this.domainPct = i;
		updateDomainWidth();
	}
	
	/**
	 * Set the width of the rectangle overlay if present,
	 * as a percent of the total x-range
	 * @param i
	 */
	public void setRangePct(double i){
		this.rangePct = i;
		updateRangeWidth();
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		
		final int x = e.getX();
		final int y = e.getY();
		
	    if (e.getButton() == MouseEvent.BUTTON1) {

	    	if(xRectangle!=null){

	    		mouseIsDown = true;

	    		if(cursorIsOverXMinEdge(x, y)){
	    			initMinXThread();
	    		} else {
	    			if(cursorIsOverXMaxEdge(x, y)){
	    				initMaxXThread();
	    			} else {

	    				if (cursorIsOverRectangle(x, y)) {
	    					//	    			log("Cursor is over rectangle");

	    					initThread();
	    				} else {

	    					// Move the rectangle directly over the mouse
	    					updateRectangleLocation(x, y);

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
	    	setDomainPct( getDomainCurrentPercent());
	    	setRangePct(  getRangeCurrentPercent());
	    	
	    	fireSignalChangeEvent("UpdatePosition");
	    }
	    
	}
	
	@Override
	public void mouseMoved(MouseEvent e){
		
		final int x = e.getX();
		final int y = e.getY();
				
		if( ! mouseIsDown){ // Mouse is up
			
			if(cursorIsOverXMinEdge(x, y)  || cursorIsOverXMaxEdge(x, y) ){
				this.setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
			} else {
				
				if(cursorIsOverYMinEdge(x, y)  || cursorIsOverYMaxEdge(x, y) ){
					this.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
				} else {

					if (cursorIsOverRectangle(x, y)) {
	
						this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	
					} else {
						this.setCursor(Cursor.getDefaultCursor());
					}
				}
			}
		}
			
	}
	
	private void updateRectangleLocation(int x, int y){

		Rectangle2D dataArea = getScreenDataArea();
		JFreeChart  chart    = getChart();
		XYPlot      plot     = (XYPlot) chart.getPlot();
		ValueAxis   xAxis    = plot.getDomainAxis();
		ValueAxis   yAxis    = plot.getRangeAxis();
		
		double xUpper = xAxis.getUpperBound();
		double yUpper = yAxis.getUpperBound();
				
		double halfXRange = domainWidth / 2;
		double halfYRange = rangeWidth / 2;
		
		double xValue = xAxis.java2DToValue(x, dataArea, 
				RectangleEdge.BOTTOM);
		
		double yValue = yAxis.java2DToValue(y, dataArea, 
				RectangleEdge.LEFT);
		
		
		// Find the chart values to use as the rectangle x range 
		double moveMin = xValue - halfXRange;
		moveMin = moveMin < 0 ? 0 : moveMin; // correct for zero end
		
		moveMin = moveMin > xUpper - domainWidth
				? xUpper - domainWidth
				: moveMin; // correct for upper end
		
		double moveMax = xValue + halfXRange;
		moveMax = moveMax < domainWidth ? domainWidth : moveMax; // correct for zero end
		moveMax = moveMax > xUpper ? xUpper	: moveMax; // correct for upper end
						
		// Set the values in chart units
		xRectangle.setXMinValue(moveMin);
		xRectangle.setXMaxValue(moveMax);
		
		// Find the chart values to use as the rectangle y range 
		double yMoveMin = yValue - halfYRange;
		yMoveMin =yMoveMin < 0 ? 0 : yMoveMin; // correct for zero end
		
		yMoveMin = yMoveMin > yUpper - rangeWidth
				? yUpper - rangeWidth
				: yMoveMin; // correct for upper end
		
		double yMoveMax = yValue + halfYRange;
		yMoveMax = yMoveMax < rangeWidth ? rangeWidth : yMoveMax; // correct for zero end
		yMoveMax = yMoveMax > xUpper ? xUpper	: yMoveMax; // correct for upper end
						
		// Set the values in chart units
		xRectangle.setYMinValue(yMoveMin);
		xRectangle.setYMaxValue(yMoveMax);
		
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

			xValue = xValue >= xRectangle.getXMaxValue() ? xRectangle.getXMaxValue() - (xRange/100) : xValue; // correct for max end

			// Set the values in chart units
			xRectangle.setXMinValue(xValue);
		} else {
			
			xValue = xValue >= xUpper ? xUpper : xValue; // correct for upper end

			xValue = xValue <= xRectangle.getXMinValue() ? xRectangle.getXMinValue() + (xRange/100) : xValue; // correct for min end

			// Set the values in chart units
			xRectangle.setXMaxValue(xValue);
			
			
		}

		fireSignalChangeEvent("UpdatePosition");
	}
	
	private void updateRangeRectangleSize(int y, boolean isMin){

		Rectangle2D dataArea = getScreenDataArea();
		JFreeChart  chart    = getChart();
		XYPlot      plot     = (XYPlot) chart.getPlot();
		ValueAxis   yAxis    = plot.getRangeAxis();
		double yUpper        = yAxis.getUpperBound();
		double yLower        = yAxis.getLowerBound();
		double yRange        = yUpper - yLower;
				
		double yValue = yAxis.java2DToValue(y, dataArea, 
				RectangleEdge.LEFT);
		

		if(isMin){
			yValue = yValue < 0 ? 0 : yValue; // correct for zero end

			yValue = yValue >= xRectangle.getYMaxValue() ? xRectangle.getYMaxValue() - (yRange/100) : yValue; // correct for max end

			// Set the values in chart units
			xRectangle.setXMinValue(yValue);
		} else {
			
			yValue = yValue >= yUpper ? yUpper : yValue; // correct for upper end

			yValue = yValue <= xRectangle.getYMinValue() ? xRectangle.getYMinValue() + (yRange/100) : yValue; // correct for min end

			// Set the values in chart units
			xRectangle.setXMaxValue(yValue);
			
			
		}

		fireSignalChangeEvent("UpdatePosition");
	}
	
	private double getDomainCurrentPercent(){
		double range = xRectangle.getXMaxValue() - xRectangle.getXMinValue();
		JFreeChart  chart    = getChart();
		XYPlot      plot     = (XYPlot) chart.getPlot();
		ValueAxis   xAxis    = plot.getDomainAxis();
		double xUpper        = xAxis.getUpperBound();
		double xLower        = xAxis.getLowerBound();
		double xRange        = xUpper - xLower;
		double newPct = (range / xRange) *100;
		
		return newPct;
	}
	
	private double getRangeCurrentPercent(){
		double range = xRectangle.getYMaxValue() - xRectangle.getYMinValue();
		JFreeChart  chart    = getChart();
		XYPlot      plot     = (XYPlot) chart.getPlot();
		ValueAxis   yAxis    = plot.getRangeAxis();
		double yUpper        = yAxis.getUpperBound();
		double yLower        = yAxis.getLowerBound();
		double yRange        = yUpper - yLower;
		double newPct = (range / yRange) *100;
		
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
	                	int y = MouseInfo.getPointerInfo().getLocation().y - getLocationOnScreen().y;
	                    
	                    // Move the box with the mouse
	                    updateRectangleLocation(x, y);

	                } while (mouseIsDown);
	                isRunning = false;
//	                IJ.log("Thread end : Running :"+checkRunning()); 
	            }
	        }.start();
	    } else {
//	    	IJ.log("Not starting thread: Running is "+checkRunning());
	    }
	}
	
	protected void initMinXThread( ) {
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
	
	protected void initMaxXThread( ) {
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
		ValueAxis yAxis = this.getChart().getXYPlot().getRangeAxis();
		
		boolean isOverLine = false;

		// Turn the chart coordinates into screen coordinates
		double rectangleMinX = xAxis.valueToJava2D(xRectangle.getXMinValue(), dataArea, 
				RectangleEdge.BOTTOM);

		double rectangleMaxX = xAxis.valueToJava2D(xRectangle.getXMaxValue(), dataArea, 
				RectangleEdge.BOTTOM);

		double rectangleW = rectangleMaxX - rectangleMinX;
		
		double rectangleMinY = yAxis.valueToJava2D(xRectangle.getYMinValue(), dataArea, 
				RectangleEdge.LEFT);

		double rectangleMaxY = yAxis.valueToJava2D(xRectangle.getYMaxValue(), dataArea, 
				RectangleEdge.LEFT);

		double rectangleH = rectangleMaxY - rectangleMinY;


		final Rectangle bounds = new Rectangle( (int)rectangleMinX, 
				(int) rectangleMinY, 
				(int) rectangleW,   
				(int) rectangleH );


		if (bounds != null && bounds.contains(x, y)) {
			isOverLine = true;
		}
//		
		return isOverLine;
	}
	
	public boolean cursorIsOverXMinEdge(int x, int y){
		Rectangle2D dataArea = this.getScreenDataArea(); 
		ValueAxis xAxis = this.getChart().getXYPlot().getDomainAxis();
		ValueAxis yAxis = this.getChart().getXYPlot().getRangeAxis();
		boolean isOverEdge = false;
		
		// Turn the chart coordinates into screen coordinates
		double rectangleMinX = xAxis.valueToJava2D(xRectangle.getXMinValue(), dataArea, 
						RectangleEdge.BOTTOM)-2;
		
		double rectangleMaxX = xAxis.valueToJava2D(xRectangle.getXMinValue(), dataArea, 
				RectangleEdge.BOTTOM)+2;
		
		double rectangleMinY = yAxis.valueToJava2D(xRectangle.getYMinValue(), dataArea, 
				RectangleEdge.LEFT);

		double rectangleMaxY = yAxis.valueToJava2D(xRectangle.getYMaxValue(), dataArea, 
				RectangleEdge.LEFT);

		double rectangleW = rectangleMaxX - rectangleMinX;
		double rectangleH = rectangleMaxY - rectangleMinY;
		
		final Rectangle bounds = new Rectangle( (int)rectangleMinX, 
				(int) rectangleMinY, 
				(int) rectangleW,   
				(int) rectangleH );

		if (bounds != null && bounds.contains(x, y)) {
			isOverEdge = true;
		}
		return isOverEdge;
		
	}
	
	public boolean cursorIsOverYMinEdge(int x, int y){
		Rectangle2D dataArea = this.getScreenDataArea(); 
		ValueAxis xAxis = this.getChart().getXYPlot().getDomainAxis();
		ValueAxis yAxis = this.getChart().getXYPlot().getRangeAxis();
		boolean isOverEdge = false;
		
		// Turn the chart coordinates into screen coordinates
		double rectangleMinX = xAxis.valueToJava2D(xRectangle.getXMinValue(), dataArea, 
						RectangleEdge.BOTTOM);
		
		double rectangleMaxX = xAxis.valueToJava2D(xRectangle.getXMaxValue(), dataArea, 
				RectangleEdge.BOTTOM);
		
		double rectangleMinY = yAxis.valueToJava2D(xRectangle.getYMinValue(), dataArea, 
				RectangleEdge.LEFT)-2;

		double rectangleMaxY = yAxis.valueToJava2D(xRectangle.getYMinValue(), dataArea, 
				RectangleEdge.LEFT)+2;

		double rectangleW = rectangleMaxX - rectangleMinX;
		double rectangleH = rectangleMaxY - rectangleMinY;
		
		final Rectangle bounds = new Rectangle( (int)rectangleMinX, 
				(int) rectangleMinY, 
				(int) rectangleW,   
				(int) rectangleH );

		if (bounds != null && bounds.contains(x, y)) {
			isOverEdge = true;
		}
		return isOverEdge;
		
	}
	
	public boolean cursorIsOverXMaxEdge(int x, int y){
		Rectangle2D dataArea = this.getScreenDataArea(); 
		ValueAxis xAxis = this.getChart().getXYPlot().getDomainAxis();
		ValueAxis yAxis = this.getChart().getXYPlot().getRangeAxis();
		boolean isOverEdge = false;
		
		// Turn the chart coordinates into screen coordinates
		double rectangleMinX = xAxis.valueToJava2D(xRectangle.getXMaxValue(), dataArea, 
						RectangleEdge.BOTTOM)-2;
		
		double rectangleMaxX = xAxis.valueToJava2D(xRectangle.getXMaxValue(), dataArea, 
				RectangleEdge.BOTTOM)+2;
		
		double rectangleMinY = yAxis.valueToJava2D(xRectangle.getYMinValue(), dataArea, 
				RectangleEdge.LEFT);

		double rectangleMaxY = yAxis.valueToJava2D(xRectangle.getYMaxValue(), dataArea, 
				RectangleEdge.LEFT);

		double rectangleW = rectangleMaxX - rectangleMinX;
		double rectangleH = rectangleMaxY - rectangleMinY;
		
		final Rectangle bounds = new Rectangle( (int)rectangleMinX, 
				(int) rectangleMinY, 
				(int) rectangleW,   
				(int) rectangleH );

		if (bounds != null && bounds.contains(x, y)) {
			isOverEdge = true;
		}
		return isOverEdge;
		
	}
	
	public boolean cursorIsOverYMaxEdge(int x, int y){
		Rectangle2D dataArea = this.getScreenDataArea(); 
		ValueAxis xAxis = this.getChart().getXYPlot().getDomainAxis();
		ValueAxis yAxis = this.getChart().getXYPlot().getRangeAxis();
		boolean isOverEdge = false;
		
		// Turn the chart coordinates into screen coordinates
		double rectangleMinX = xAxis.valueToJava2D(xRectangle.getXMaxValue(), dataArea, 
						RectangleEdge.BOTTOM);
		
		double rectangleMaxX = xAxis.valueToJava2D(xRectangle.getXMaxValue(), dataArea, 
				RectangleEdge.BOTTOM);
		
		double rectangleMinY = yAxis.valueToJava2D(xRectangle.getYMinValue(), dataArea, 
				RectangleEdge.LEFT)-2;

		double rectangleMaxY = yAxis.valueToJava2D(xRectangle.getYMaxValue(), dataArea, 
				RectangleEdge.LEFT)+2;

		double rectangleW = rectangleMaxX - rectangleMinX;
		double rectangleH = rectangleMaxY - rectangleMinY;
		
		final Rectangle bounds = new Rectangle( (int)rectangleMinX, 
				(int) rectangleMinY, 
				(int) rectangleW,   
				(int) rectangleH );

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
