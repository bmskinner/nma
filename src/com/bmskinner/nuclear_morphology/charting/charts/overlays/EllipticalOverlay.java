/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.charting.charts.overlays;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.logging.Logger;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.OverlayChangeEvent;
import org.jfree.chart.panel.AbstractOverlay;
import org.jfree.chart.panel.Overlay;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.ui.RectangleEdge;

import com.bmskinner.nuclear_morphology.logging.Loggable;

@SuppressWarnings("serial")
public class EllipticalOverlay extends AbstractOverlay
        implements Overlay, PropertyChangeListener, Serializable{
	
	private static final Logger LOGGER = Logger.getLogger(Loggable.ROOT_LOGGER);

    private EllipticalOverlayObject ellipse = null;

    /**
     * Default constructor.
     */
    public EllipticalOverlay() {
        super();
    }

    /**
     * Construct with a rectangle object
     * 
     * @param rectangle
     */
    public EllipticalOverlay(EllipticalOverlayObject ellipse) {
        super();
        setEllipse(ellipse);
    }

    /**
     * Adds a rectangle object to this overlay and fires a
     * {@link OverlayChangeEvent} to all registered listeners.
     *
     * @param rectangle the rectangle object (<code>null</code> not permitted).
     *
     * @see #removeRectangle(charting.charts.RectangleOverlayObject)
     */
    public void setEllipse(EllipticalOverlayObject ellipse) {

        if (ellipse == null) {
            throw new IllegalArgumentException("Ellipse object cannot be null in chart overlay");
        }
        this.ellipse = ellipse;
        this.ellipse.addPropertyChangeListener(this);
        fireOverlayChanged();
    }

    /**
     * Get the current rectangle object in this overlay
     */
    public EllipticalOverlayObject getEllipse() {
        return ellipse;
    }

    /**
     * Removes the given rectangle and sends an {@link OverlayChangeEvent} to
     * all registered listeners.
     *
     * @param rectangle the rectangle to remove
     */
    public void removeEllipse(EllipticalOverlayObject ellipse) {
        if (ellipse == this.ellipse) {
            this.ellipse = null;
            fireOverlayChanged();
        }
    }

    /**
     * Receives a property change event (typically a change in one of the
     * crosshairs).
     *
     * @param e the event.
     */
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        fireOverlayChanged();
    }

    /**
     * Paints the crosshairs in the layer.
     *
     * @param g2 the graphics target.
     * @param chartPanel the chart panel.
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
        if (ellipse != null && ellipse.isVisible()) {

        	// get the values for the x-axis
        	double minx = ellipse.getXValue() - ellipse.getXRadius();// .getXMinValue();
        	double minxx = xAxis.valueToJava2D(minx, dataArea, xAxisEdge);

        	double maxx = ellipse.getXValue() + ellipse.getXRadius();
        	double maxxx = xAxis.valueToJava2D(maxx, dataArea, xAxisEdge);

        	// Get the values for the y-axis
        	double miny = ellipse.getYValue() - ellipse.getYRadius();
        	double minyy = yAxis.valueToJava2D(miny, dataArea, yAxisEdge);

        	double maxy = ellipse.getYValue() + ellipse.getYRadius();
        	double maxyy = yAxis.valueToJava2D(maxy, dataArea, yAxisEdge);

        	// Swap y if inverted
        	double temp = minyy;

        	minyy = minyy > maxyy ? maxyy : minyy;
        	maxyy = temp > maxyy ? temp : maxyy;

        	LOGGER.finest( "Chart rectangle x: " + minx + " - " + maxx + "  y: " + miny + " - " + maxy);
        	LOGGER.finest( "Java2D rectangle x: " + minxx + " - " + maxxx + "  y: " + minyy + " - " + maxyy);

        	if (plot.getOrientation() == PlotOrientation.VERTICAL) {
        		drawVerticalEllipse(g2, dataArea, minxx, maxxx, minyy, maxyy, ellipse);
        	} else {
        		drawHorizontalEllipse(g2, dataArea, minxx, maxxx, minyy, maxyy, ellipse);
        	}

        }
        g2.setClip(savedClip);
    }

    /**
     * Draws the rectangle horizontally across the plot.
     *
     * @param g2 the graphics target.
     * @param dataArea the data area.
     * @param y the y-value in Java2D space.
     * @param crosshair the crosshair.
     */
    protected void drawHorizontalEllipse(Graphics2D g2, Rectangle2D dataArea, double minx, double maxx, double miny,
            double maxy, EllipticalOverlayObject ellipse) {

        if (miny >= dataArea.getMinY() && miny <= dataArea.getMaxY()) {

            double w = maxx - minx;
            double h = maxy - miny;

            Ellipse2D e = new Ellipse2D.Double(minx, miny, w, h);

            Paint savedPaint = g2.getPaint();
            Stroke savedStroke = g2.getStroke();
            g2.setPaint(ellipse.getFill());
            g2.setStroke(ellipse.getStroke());

            g2.fill(e);

            g2.setPaint(savedPaint);
            g2.setStroke(savedStroke);
        }
    }

    /**
     * Draw the rectangle vertically on the plot.
     *
     * @param g2 the graphics target.
     * @param dataArea the data area.
     * @param x the x-value in Java2D space.
     * @param crosshair the crosshair.
     */
    protected void drawVerticalEllipse(Graphics2D g2, Rectangle2D dataArea, double minx, double maxx, double miny,
            double maxy, EllipticalOverlayObject ellipse) {

        double w = maxx - minx;
        double h = maxy - miny;

        Ellipse2D e = new Ellipse2D.Double(minx, miny, w, h);

        Paint savedPaint = g2.getPaint();
        Stroke savedStroke = g2.getStroke();
        g2.setPaint(ellipse.getFill());
        g2.setStroke(ellipse.getStroke());

        g2.fill(e);

        g2.setPaint(savedPaint);
        g2.setStroke(savedStroke);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ellipse == null) ? 0 : ellipse.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EllipticalOverlay other = (EllipticalOverlay) obj;
        if (ellipse == null) {
            if (other.ellipse != null)
                return false;
        } else if (!ellipse.equals(other.ellipse))
            return false;
        return true;
    }

}
