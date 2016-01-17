package gui.components;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
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

import ij.IJ;

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
        shape.addPropertyChangeListener(this);
    }

    public void removeshape(ShapeOverlayObject shape) {
        if (shape == null) {
            throw new IllegalArgumentException("Null 'crosshair' argument.");
        }
        if (this.shapes.remove(shape)) {
        	shape.removePropertyChangeListener(this);
            fireOverlayChanged();
        }
    }

    public void clearShapes() {
        if (this.shapes.isEmpty()) {
            return;  // nothing to do
        }
        List<ShapeOverlayObject> shapes = getShapes();
        for (int i = 0; i < shapes.size(); i++) {
        	ShapeOverlayObject c = (ShapeOverlayObject) shapes.get(i);
            this.shapes.remove(c);
            c.removePropertyChangeListener(this);
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
//        Rectangle2D dataArea = chartPanel.getScreenDataArea();
        Rectangle2D dataArea = chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea();
//        IJ.log(dataArea.toString());
        g2.clip(dataArea);
        JFreeChart chart = chartPanel.getChart();
        XYPlot plot = (XYPlot) chart.getPlot();
        ValueAxis xAxis = plot.getDomainAxis();
        RectangleEdge xAxisEdge = plot.getDomainAxisEdge();
        
        ValueAxis yAxis = plot.getRangeAxis();
        RectangleEdge yAxisEdge = plot.getRangeAxisEdge();
        
        Iterator<ShapeOverlayObject> iterator = this.shapes.iterator();
        while (iterator.hasNext()) {
        	ShapeOverlayObject object = (ShapeOverlayObject) iterator.next();
            if (object.isVisible()) {
                double x = object.getShape().getBounds2D().getX();
                double xx = xAxis.valueToJava2D(x, dataArea, xAxisEdge);
                
//                IJ.log("X: "+x+" at "+xx);
                
                double y = object.getShape().getBounds2D().getY();
                double yy = yAxis.valueToJava2D(y, dataArea, yAxisEdge);
                
//                IJ.log("Y: "+y+" at "+yy);

                
                drawShape(g2, dataArea, xx, yy, object);
            }
        }
        g2.setClip(savedClip);
    }
	
	/**
     * Draws a shape on the plot.
     *
     * @param g2  the graphics target.
     * @param dataArea  the data area.
     * @param y  the y-value in Java2D space.
     * @param shape  the overlay object
     */
    protected void drawShape(Graphics2D g2, Rectangle2D dataArea,
            double x, double y, ShapeOverlayObject shape) {

        if (y >= dataArea.getMinY() && y <= dataArea.getMaxY()
        		&& x >= dataArea.getMinX() && x <= dataArea.getMaxX()) {

            Paint savedPaint = g2.getPaint();
            Stroke savedStroke = g2.getStroke();
            
            Shape s = getJavaCoordinatesShape(x, y, shape);
            
            g2.setPaint(shape.getOutline());
            g2.setStroke(shape.getStroke());
            g2.draw(s);
            
            g2.setPaint(savedPaint);
            g2.setStroke(savedStroke);
        }
    }
    
    protected Shape getJavaCoordinatesShape(double xCentre, double yCentre, ShapeOverlayObject shape){
    	
    	Shape s = shape.getShape();
    	AffineTransform aft = new AffineTransform();
    	aft.translate(xCentre, yCentre);
    	return aft.createTransformedShape(s);
    }
}
