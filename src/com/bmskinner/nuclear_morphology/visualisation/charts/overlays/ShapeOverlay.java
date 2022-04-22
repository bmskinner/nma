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
package com.bmskinner.nuclear_morphology.visualisation.charts.overlays;

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
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleEdge;

public class ShapeOverlay extends AbstractOverlay implements Overlay, PropertyChangeListener, Serializable {

	private static final long serialVersionUID = 1L;
	protected List<ShapeOverlayObject> shapes;

	/**
	 * Default constructor.
	 */
	public ShapeOverlay() {
		super();
		this.shapes = new ArrayList<>();
	}

	/**
	 * Default constructor.
	 */
	public ShapeOverlay(ShapeOverlayObject shape) {
		this();
		addShape(shape);
	}

	/**
	 * Adds a shape
	 *
	 * @param shape the shape.
	 */
	public void addShape(ShapeOverlayObject shape) {
		if (shape == null) {
			throw new IllegalArgumentException("Null 'shape' argument.");
		}
		this.shapes.add(shape);
		shape.addPropertyChangeListener(this);
		fireOverlayChanged();
	}

	public void removeshape(ShapeOverlayObject shape) {
		if (shape == null) {
			throw new IllegalArgumentException("Null 'shape' argument.");
		}
		if (this.shapes.remove(shape)) {
			shape.removePropertyChangeListener(this);
			fireOverlayChanged();
		}
	}

	public synchronized void clearShapes() {
		if (this.shapes.isEmpty()) {
			return; // nothing to do
		}
		List<ShapeOverlayObject> shapes = getShapes();
		for (int i = 0; i < shapes.size(); i++) {
			ShapeOverlayObject c = shapes.get(i);
			this.shapes.remove(c);
			c.removePropertyChangeListener(this);
		}
		fireOverlayChanged();
	}

	public synchronized List<ShapeOverlayObject> getShapes() {
		return new ArrayList<>(this.shapes);
	}

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		fireOverlayChanged();
	}

	/**
	 * Paints the crosshairs in the layer.
	 *
	 * @param g2         the graphics target.
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

		Iterator<ShapeOverlayObject> iterator = this.shapes.iterator();

		while (iterator.hasNext()) {
			ShapeOverlayObject object = iterator.next();

			if (object.isVisible()) {

				// Need to find the coordinates to draw the shape

				double x = object.getShape().getBounds2D().getCenterX();
				double xx = xAxis.valueToJava2D(x, dataArea, xAxisEdge);

				double y = object.getShape().getBounds2D().getCenterY();
				double yy = yAxis.valueToJava2D(y, dataArea, yAxisEdge);

				// Need to scale the shape as well
				// We include a division by the original width to preserve
				// aspect ratios in the final rendered shape
				double w = object.getShape().getBounds2D().getWidth();
				double ww = xAxis.lengthToJava2D(w, dataArea, xAxisEdge) / w;

				double h = object.getShape().getBounds2D().getHeight();
				double hh = yAxis.lengthToJava2D(h, dataArea, yAxisEdge) / h;

//				System.out.println("Input correct :" + x + ", " + y);
//				System.out.println("Java2D correct: " + xx + ", " + yy);

				// The xx and yy positions are correct at this point
//				Shape s = ShapeOverlayObject.createDiamond((float) ww, xx, yy);
//				g2.setPaint(Color.orange);
//				g2.fill(s);

				// Running this confirms that the affine transforms are correct
				// so long as the input shape has the correct xx and yy coordinates
//				Shape s2 = getJavaCoordinatesShape(xx, yy, ww, hh, s);
//				System.out.println("Java2D affine :" + s2.getBounds2D().getCenterX() + ", "
//						+ s2.getBounds2D().getCenterY() + " w: " + s2.getBounds2D().getWidth());

//				g2.setPaint(Color.BLUE);
//				g2.fill(s2);

				drawShape(g2, dataArea, xx, yy, ww, hh, object);
			}
		}
		g2.setClip(savedClip);
	}

	/**
	 * Draws a shape on the plot.
	 *
	 * @param g2       the graphics target.
	 * @param dataArea the data area.
	 * @param x        the x-value in Java2D space.
	 * @param y        the y-value in Java2D space.
	 * @param w        the width of the object in Java2D space.
	 * @param h        the height of the object in Java2D space.
	 * @param shape    the overlay object
	 */
	protected void drawShape(Graphics2D g2, Rectangle2D dataArea, double x, double y, double w, double h,
			ShapeOverlayObject shape) {

		Paint savedPaint = g2.getPaint();
		Stroke savedStroke = g2.getStroke();
//		System.out.println("Draw pre affine :" + shape.getShape().getBounds2D().getCenterX() + ", "
//				+ shape.getShape().getBounds2D().getCenterY());

		Shape s = getJavaCoordinatesShape(x, y, w, h, shape.getShape());
//		System.out.println("Draw post affine :" + s.getBounds2D().getCenterX() + ", " + s.getBounds2D().getCenterY());

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

	}

	/**
	 * Convert the given shape for drawing on a Graphics object. Flips the shape
	 * horizontally, rotate 180 degrees, then scale according to the given width and
	 * height.
	 * 
	 * @param xCentre
	 * @param yCentre
	 * @param w
	 * @param h
	 * @param shape
	 * @return
	 */
	protected Shape getJavaCoordinatesShape(double xCentre, double yCentre, double w, double h, Shape s) {

		// transforms are performed 'last in, first out'
		AffineTransform aft = new AffineTransform();

//		// Move back to original position after rotation and scaling
		aft.concatenate(AffineTransform.getTranslateInstance(xCentre, yCentre));
//
//		// Ensure the objects are scaled appropriately
		aft.concatenate(AffineTransform.getScaleInstance(w, h));
//
//		// 180 degree rotate
		aft.concatenate(AffineTransform.getRotateInstance(Math.PI));
//
//		// Flip horizontal
		aft.concatenate(AffineTransform.getScaleInstance(-1, 1));
//
		// Move to origin
		aft.concatenate(
				AffineTransform.getTranslateInstance(-s.getBounds2D().getCenterX(), -s.getBounds2D().getCenterY()));

		return aft.createTransformedShape(s);
	}
}
