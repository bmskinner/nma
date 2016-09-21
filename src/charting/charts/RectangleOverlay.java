package charting.charts;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;

import logging.Loggable;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.OverlayChangeEvent;
import org.jfree.chart.panel.AbstractOverlay;
import org.jfree.chart.panel.Overlay;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.ui.RectangleEdge;

/**
 * An overlay for a {@link ChartPanel} that draws a rectangle on a plot.
 *
 */
@SuppressWarnings("serial")
public class RectangleOverlay 
	extends AbstractOverlay 
	implements Overlay,
               PropertyChangeListener, 
               Serializable, 
               Loggable {

	private RectangleOverlayObject rectangle = null;

    /**
     * Default constructor.
     */
    public RectangleOverlay() {
        super();
    }
    
    
    /**
     * Construct with a rectangle object
     * @param rectangle
     */
    public RectangleOverlay(RectangleOverlayObject rectangle) {
        super();
        setRectangle(rectangle);
    }
    

    /**
     * Adds a rectangle object to this overlay and fires a 
     * {@link OverlayChangeEvent} to all registered listeners.
     *
     * @param rectangle  the rectangle object (<code>null</code> not permitted).
     *
     * @see #removeRectangle(charting.charts.RectangleOverlayObject)
     */
    public void setRectangle(RectangleOverlayObject rectangle) {

    	if( rectangle==null	){
    		throw new IllegalArgumentException("Rectangle object cannot be null in chart overlay");
    	}
        this.rectangle = rectangle;
        this.rectangle.addPropertyChangeListener(this);
        fireOverlayChanged();
    }
    
    /**
     * Get the current rectangle object in this overlay
     */
    public RectangleOverlayObject getRectangle() {
        return rectangle;
    }

    /**
     * Removes the given rectangle and sends an {@link OverlayChangeEvent}
     * to all registered listeners.
     *
     * @param rectangle the rectangle to remove
     */
    public void removeRectangle(RectangleOverlayObject rectangle) {
        rectangle=null;
        fireOverlayChanged();
        
    }

    /**
     * Receives a property change event (typically a change in one of the
     * crosshairs).
     *
     * @param e  the event.
     */
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        fireOverlayChanged();
    }

    /**
     * Paints the crosshairs in the layer.
     *
     * @param g2  the graphics target.
     * @param chartPanel  the chart panel.
     */
    @Override
    public void paintOverlay(Graphics2D g2, ChartPanel chartPanel) {
    	    	
        Shape savedClip = g2.getClip();
        Rectangle2D dataArea = chartPanel.getScreenDataArea();
        g2.clip(dataArea);
        JFreeChart chart = chartPanel.getChart();
        XYPlot plot = (XYPlot) chart.getPlot();
        ValueAxis xAxis = plot.getDomainAxis();
        ValueAxis yAxis = plot.getRangeAxis();
        RectangleEdge xAxisEdge = plot.getDomainAxisEdge();
        RectangleEdge yAxisEdge = plot.getRangeAxisEdge();
        
        // x rectangle for domain axis
        if(rectangle!=null){
            if (rectangle.isVisible()) {
            	
            	// get the values for the x-axis
                double minx = rectangle.getXMinValue();
                double minxx = xAxis.valueToJava2D(minx, dataArea, xAxisEdge);
                
                double maxx = rectangle.getXMaxValue();
                double maxxx = xAxis.valueToJava2D(maxx, dataArea, xAxisEdge);
                
                // Get the values for the y-axis
                double miny  = rectangle.getYMinValue();
                double minyy = yAxis.valueToJava2D(miny, dataArea, yAxisEdge);
                
                double maxy  = rectangle.getYMaxValue();
                double maxyy = yAxis.valueToJava2D(maxy, dataArea, yAxisEdge);
                
                // Swap y if inverted
                double temp = minyy;
                
                minyy = minyy > maxyy ? maxyy : minyy;
                maxyy = temp  > maxyy ? temp  : maxyy;
                
                
                finest("Chart rectangle x: "+minx+" - "+maxx+"  y: "+miny+" - "+maxy);
                finest("Java2D rectangle x: "+minxx+" - "+maxxx+"  y: "+minyy+" - "+maxyy);
                
                if (plot.getOrientation() == PlotOrientation.VERTICAL) {
                    drawVerticalRectangle(g2, dataArea, minxx, maxxx, minyy, maxyy, rectangle);
                }
                else {
                    drawHorizontalRectangle(g2, dataArea, minxx, maxxx, minyy, maxyy, rectangle);
                }
            }
        }
        g2.setClip(savedClip);
    }

    /**
     * Draws the rectangle horizontally across the plot.
     *
     * @param g2  the graphics target.
     * @param dataArea  the data area.
     * @param y  the y-value in Java2D space.
     * @param crosshair  the crosshair.
     */
    protected void drawHorizontalRectangle(Graphics2D g2, Rectangle2D dataArea,
    		double minx, double maxx, double miny, double maxy, RectangleOverlayObject rectangle) {

//    	log("Drawing horizontal rectangle");
    	
        if (miny >= dataArea.getMinY() && miny <= dataArea.getMaxY()) {
        	
        	double w = maxx - minx;
        	double h = maxy - miny;
        	
//        	Rectangle2D r = new Rectangle2D.Double(dataArea.getMinX(), miny,
//                  dataArea.getMaxX(), h);
        	
        	Rectangle2D r = new Rectangle2D.Double(minx, miny, w, h);

            Paint savedPaint = g2.getPaint();
            Stroke savedStroke = g2.getStroke();
            g2.setPaint(rectangle.getFill());
            g2.setStroke(rectangle.getStroke());
//            g2.draw(r);
            g2.fill(r);

            g2.setPaint(savedPaint);
            g2.setStroke(savedStroke);
        }
    }

    /**
     * Draw the rectangle vertically on the plot.
     *
     * @param g2  the graphics target.
     * @param dataArea  the data area.
     * @param x  the x-value in Java2D space.
     * @param crosshair  the crosshair.
     */
    protected void drawVerticalRectangle(Graphics2D g2, Rectangle2D dataArea,
            double minx, double maxx, double miny, double maxy, RectangleOverlayObject rectangle) {
    	
        if (minx >= dataArea.getMinX() && minx <= dataArea.getMaxX()) {
        	
        	finest("Drawing rectangle");
        	
        	double w = maxx - minx;
        	double h = maxy - miny;
        	
        	Rectangle2D r = new Rectangle2D.Double(minx, miny, w, h);
        	        	
        	Paint savedPaint = g2.getPaint();
            Stroke savedStroke = g2.getStroke();
            
            g2.setPaint(rectangle.getFill());
            g2.setStroke(rectangle.getStroke());
            g2.fill(r);     
            
            g2.setPaint(savedPaint);
            g2.setStroke(savedStroke);
        }
    }
 
    /**
     * Tests this overlay for equality with an arbitrary object.
     *
     * @param obj  the object (<code>null</code> permitted).
     *
     * @return A boolean.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof RectangleOverlay)) {
            return false;
        }
        RectangleOverlay that = (RectangleOverlay) obj;
        if (!this.rectangle.equals(that.rectangle)) {
            return false;
        }
//        if (!this.yRectangle.equals(that.yRectangle)) {
//            return false;
//        }
        return true;
    }

}

