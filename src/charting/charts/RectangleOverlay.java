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
public class RectangleOverlay extends AbstractOverlay implements Overlay,
        PropertyChangeListener, Serializable, Loggable {

	private RectangleOverlayObject xRectangle = null;
	private RectangleOverlayObject yRectangle = null;

    /**
     * Default constructor.
     */
    public RectangleOverlay() {
        super();

    }

    /**
     * Adds a crosshair against the domain axis and sends an
     * {@link OverlayChangeEvent} to all registered listeners.
     *
     * @param crosshair  the crosshair (<code>null</code> not permitted).
     *
     * @see #removeDomainCrosshair(org.jfree.chart.plot.Crosshair)
     * @see #addRangeCrosshair(org.jfree.chart.plot.Crosshair)
     */
    public void setDomainRectangle(RectangleOverlayObject rectangle) {

        this.xRectangle = rectangle;
        xRectangle.addPropertyChangeListener(this);
        fireOverlayChanged();
    }

    /**
     * Removes a domain axis crosshair and sends an {@link OverlayChangeEvent}
     * to all registered listeners.
     *
     * @param crosshair  the crosshair (<code>null</code> not permitted).
     *
     * @see #addDomainCrosshair(org.jfree.chart.plot.Crosshair)
     */
    public void removeDomainRectangle(RectangleOverlayObject rectangle) {
        xRectangle=null;
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
    	
//    	log("Painting rectangle overlay");
    	
        Shape savedClip = g2.getClip();
        Rectangle2D dataArea = chartPanel.getScreenDataArea();
        g2.clip(dataArea);
        JFreeChart chart = chartPanel.getChart();
        XYPlot plot = (XYPlot) chart.getPlot();
        ValueAxis xAxis = plot.getDomainAxis();
        RectangleEdge xAxisEdge = plot.getDomainAxisEdge();
        
        // x rectangle for domain axis
        if(xRectangle!=null){
            if (xRectangle.isVisible()) {
                double min = xRectangle.getMinValue();
                double minx = xAxis.valueToJava2D(min, dataArea, xAxisEdge);
                
                double max = xRectangle.getMaxValue();
                double maxx = xAxis.valueToJava2D(max, dataArea, xAxisEdge);
                
//                log("Java2D positions for min: "+min+" -> "+minx);
//                log("Java2D positions for max: "+max+" -> "+maxx);
                
                if (plot.getOrientation() == PlotOrientation.VERTICAL) {
                    drawVerticalRectangle(g2, dataArea, minx, maxx, xRectangle);
                }
                else {
                    drawHorizontalRectangle(g2, dataArea, minx, maxx, xRectangle);
                }
            }
        }
        
        ValueAxis yAxis = plot.getRangeAxis();
        RectangleEdge yAxisEdge = plot.getRangeAxisEdge();

        if(yRectangle!=null){
        	if (yRectangle.isVisible()) {
        		double min = yRectangle.getMinValue();
        		double miny = yAxis.valueToJava2D(min, dataArea, yAxisEdge);
        		
        		double max = yRectangle.getMaxValue();
                double maxy = yAxis.valueToJava2D(max, dataArea, yAxisEdge);
        		
        		if (plot.getOrientation() == PlotOrientation.VERTICAL) {
        			drawHorizontalRectangle(g2, dataArea, miny, maxy, yRectangle);
        		}
        		else {
        			drawVerticalRectangle(g2, dataArea, miny, maxy, yRectangle);
        		}
        	}
        }

        g2.setClip(savedClip);
    }

    /**
     * Draws a crosshair horizontally across the plot.
     *
     * @param g2  the graphics target.
     * @param dataArea  the data area.
     * @param y  the y-value in Java2D space.
     * @param crosshair  the crosshair.
     */
    protected void drawHorizontalRectangle(Graphics2D g2, Rectangle2D dataArea,
            double miny, double maxy, RectangleOverlayObject rectangle) {

//    	log("Drawing horizontal rectangle");
    	
        if (miny >= dataArea.getMinY() && miny <= dataArea.getMaxY()) {
        	
        	double h = maxy - miny;
        	
        	Rectangle2D r = new Rectangle2D.Double(dataArea.getMinX(), miny,
                  dataArea.getMaxX(), h);

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
     * Draws a crosshair vertically on the plot.
     *
     * @param g2  the graphics target.
     * @param dataArea  the data area.
     * @param x  the x-value in Java2D space.
     * @param crosshair  the crosshair.
     */
    protected void drawVerticalRectangle(Graphics2D g2, Rectangle2D dataArea,
            double minx, double maxx, RectangleOverlayObject rectangle) {

//    	log("Drawing vertical rectangle");
    	
        if (minx >= dataArea.getMinX() && minx <= dataArea.getMaxX()) {
        	
        	double w = maxx - minx;
        	
        	Rectangle2D r = new Rectangle2D.Double(minx, dataArea.getMinY(), w,
          dataArea.getMaxY());
        	
//        	log(r.toString());
        	
        	Paint savedPaint = g2.getPaint();
            Stroke savedStroke = g2.getStroke();
            g2.setPaint(rectangle.getFill());
            g2.setStroke(rectangle.getStroke());
            g2.fill(r);
//            g2.draw(r);
            
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
        if (!this.xRectangle.equals(that.xRectangle)) {
            return false;
        }
        if (!this.yRectangle.equals(that.yRectangle)) {
            return false;
        }
        return true;
    }

}

