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
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.ui.RectangleEdge;

import com.bmskinner.nuclear_morphology.components.CellularComponent;

@SuppressWarnings("serial")
public class ComponentOverlay extends ShapeOverlay {

    private List<CellularComponent> nuclei;

    /**
     * Default constructor.
     */
    public ComponentOverlay() {
        super();
        this.nuclei = new ArrayList<CellularComponent>();
    }

    /**
     * Adds a shape and a nucleus
     *
     * @param shape the shape.
     */
    public void addShape(ShapeOverlayObject shape, CellularComponent n) {
        if (shape == null || n == null) {
            throw new IllegalArgumentException("Null shape or component argument.");
        }
        this.shapes.add(shape);
        shape.addPropertyChangeListener(this);
        this.nuclei.add(n);
        fireOverlayChanged();
    }

    @Override
    public void removeshape(ShapeOverlayObject shape) {
        if (shape == null) {
            throw new IllegalArgumentException("Null 'shape' argument.");
        }

        int i = shapes.indexOf(shape);
        if (i > -1) {
            shapes.remove(i);
            nuclei.remove(i);
            shape.removePropertyChangeListener(this);
            fireOverlayChanged();
        }
    }

    public void clearShapes() {
        super.clearShapes();

        nuclei.clear();
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
        Rectangle2D dataArea = chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea();
        g2.clip(dataArea);

        JFreeChart chart = chartPanel.getChart();
        XYPlot plot = (XYPlot) chart.getPlot();
        ValueAxis xAxis = plot.getDomainAxis();
        ValueAxis yAxis = plot.getRangeAxis();

        RectangleEdge xAxisEdge = plot.getDomainAxisEdge();
        RectangleEdge yAxisEdge = plot.getRangeAxisEdge();

        for (int i = 0; i < shapes.size(); i++) {

            ShapeOverlayObject object = shapes.get(i);
            CellularComponent n = nuclei.get(i);
            if (object.isVisible()) {

                // Need to find the coordinates to draw the shape

                // double x = object.getShape().getBounds2D().getX();
                double x = n.getCentreOfMass().getX(); // get the x midpoint
                double xx = xAxis.valueToJava2D(x, dataArea, xAxisEdge);

                // double y = object.getShape().getBounds2D().getY();
                double y = n.getCentreOfMass().getY(); // get the y midpoint
                double yy = yAxis.valueToJava2D(y, dataArea, yAxisEdge);

                // Need to scale the shape as well
                double w = object.getShape().getBounds2D().getWidth();
                double ww = xAxis.lengthToJava2D(w, dataArea, xAxisEdge) / w;

                double h = object.getShape().getBounds2D().getHeight();
                double hh = yAxis.lengthToJava2D(h, dataArea, yAxisEdge) / h;

                drawShape(g2, dataArea, xx, yy, ww, hh, object);
            }

        }

        g2.setClip(savedClip);
    }

    /**
     * Draws a shape on the plot.
     *
     * @param g2
     *            the graphics target.
     * @param dataArea
     *            the data area.
     * @param y
     *            the y-value in Java2D space.
     * @param shape
     *            the overlay object
     */
    protected void drawShape(Graphics2D g2, Rectangle2D dataArea, double x, double y, double w, double h,
            ShapeOverlayObject shape) {

        // if (y >= dataArea.getMinY() && y <= dataArea.getMaxY()
        // && x >= dataArea.getMinX() && x <= dataArea.getMaxX()) {

        Paint savedPaint = g2.getPaint();
        Stroke savedStroke = g2.getStroke();

        Shape s = getJavaCoordinatesShape(x, y, w, h, shape);

        if (shape.getOutline() != null) {
            g2.setPaint(shape.getOutline());
            g2.setStroke(shape.getStroke());
            g2.draw(s);
        }

        if (shape.getFill() != null) {
            g2.setPaint(shape.getFill());
            g2.fill(s);
        }

        g2.setPaint(savedPaint);
        g2.setStroke(savedStroke);
        // }
    }

}
