package gui.components;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.panel.AbstractOverlay;
import org.jfree.chart.panel.Overlay;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.ui.RectangleEdge;
import org.jfree.util.PublicCloneable;

public class ShapeOverlay extends AbstractOverlay implements Overlay,
    PropertyChangeListener, Serializable {
	
	private static final long serialVersionUID = 1L;
	private List<ShapeOverlayObject> shapes;
	
	/**
     * Default constructor.
     */
    public ShapeOverlay() {
        super();
        this.shapes = new ArrayList<ShapeOverlayObject>();
    }
    
    /**
     * Adds a crosshair against the domain axis.
     *
     * @param crosshair  the crosshair.
     */
    public void addShape(ShapeOverlayObject shape) {
        if (shape == null) {
            throw new IllegalArgumentException("Null 'crosshair' argument.");
        }
        this.shapes.add(shape);
//        shape.addPropertyChangeListener(this);
    }

    public void removeshape(ShapeOverlayObject shape) {
        if (shape == null) {
            throw new IllegalArgumentException("Null 'crosshair' argument.");
        }
        if (this.shapes.remove(shape)) {
//        	shape.removePropertyChangeListener(this);
            fireOverlayChanged();
        }
    }

    public void clearShapes() {
        if (this.shapes.isEmpty()) {
            return;  // nothing to do
        }
        List<ShapeOverlayObject> shapes = getShapes();
        for (int i = 0; i < shapes.size(); i++) {
        	Shape c = (Shape) shapes.get(i);
            this.shapes.remove(c);
//            c.removePropertyChangeListener(this);
        }
        fireOverlayChanged();
    }
    
    public List<ShapeOverlayObject> getShapes() {
        return new ArrayList<ShapeOverlayObject>(this.shapes);
    }

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
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
        RectangleEdge xAxisEdge = plot.getDomainAxisEdge();
        
        ValueAxis yAxis = plot.getRangeAxis();
        RectangleEdge yAxisEdge = plot.getRangeAxisEdge();
        
        Iterator<ShapeOverlayObject> iterator = this.shapes.iterator();
        while (iterator.hasNext()) {
        	ShapeOverlayObject ch = (ShapeOverlayObject) iterator.next();
//            if (ch.isVisible()) {
                double x = ch.getShape().getBounds2D().getCenterX();
                double xx = xAxis.valueToJava2D(x, dataArea, xAxisEdge);
                
                double y = ch.getShape().getBounds2D().getCenterY();
                double yy = yAxis.valueToJava2D(y, dataArea, yAxisEdge);
                
                drawShape(g2, dataArea, xx, yy, ch);
//                if (plot.getOrientation() == PlotOrientation.VERTICAL) {
//                    drawVerticalCrosshair(g2, dataArea, xx, ch);
//                }
//                else {
//                    drawHorizontalCrosshair(g2, dataArea, xx, ch);
//                }
//            }
        }
        g2.setClip(savedClip);
    }
	
	/**
     * Draws a shape on the plot.
     *
     * @param g2  the graphics target.
     * @param dataArea  the data area.
     * @param y  the y-value in Java2D space.
     * @param crosshair  the crosshair.
     */
    protected void drawShape(Graphics2D g2, Rectangle2D dataArea,
            double x, double y, ShapeOverlayObject shape) {

        if (y >= dataArea.getMinY() && y <= dataArea.getMaxY()
        		&& x >= dataArea.getMinX() && x <= dataArea.getMaxX()) {

            Paint savedPaint = g2.getPaint();
            Stroke savedStroke = g2.getStroke();
            g2.setPaint(shape.getOutline());
            g2.setStroke(shape.getStroke());
            g2.draw(shape.getShape());
//            if (crosshair.isLabelVisible()) {
//                String label = crosshair.getLabelGenerator().generateLabel(
//                        crosshair);
//                RectangleAnchor anchor = crosshair.getLabelAnchor();
//                Point2D pt = calculateLabelPoint(line, anchor, 5, 5);
//                float xx = (float) pt.getX();
//                float yy = (float) pt.getY();
//                TextAnchor alignPt = textAlignPtForLabelAnchorH(anchor);
//                Shape hotspot = TextUtilities.calculateRotatedStringBounds(
//                        label, g2, xx, yy, alignPt, 0.0, TextAnchor.CENTER);
//                if (!dataArea.contains(hotspot.getBounds2D())) {
//                    anchor = flipAnchorV(anchor);
//                    pt = calculateLabelPoint(line, anchor, 5, 5);
//                    xx = (float) pt.getX();
//                    yy = (float) pt.getY();
//                    alignPt = textAlignPtForLabelAnchorH(anchor);
//                    hotspot = TextUtilities.calculateRotatedStringBounds(
//                           label, g2, xx, yy, alignPt, 0.0, TextAnchor.CENTER);
//                }
//
//                g2.setPaint(crosshair.getLabelBackgroundPaint());
//                g2.fill(hotspot);
//                g2.setPaint(crosshair.getLabelOutlinePaint());
//                g2.draw(hotspot);
//                TextUtilities.drawAlignedString(label, g2, xx, yy, alignPt);
//            }
            g2.setPaint(savedPaint);
            g2.setStroke(savedStroke);
        }
    }
}
