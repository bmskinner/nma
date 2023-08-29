package com.bmskinner.nma.visualisation.venn;

import java.awt.Color;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.annotations.XYShapeAnnotation;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

public class VennShape {

	public enum VennShapeType {
		CIRCLE, HALF_CIRCLE_LEFT, HALF_CIRCLE_RIGHT, THIRD_CIRCLE_UPPER, THIRD_CIRCLE_LEFT,
		THIRD_CIRCLE_RIGHT;
	}

	private IAnalysisDataset dataset;
	private double xCentre;
	private double yCentre;
	private double rx;
	private double ry;
	private VennShapeType shape;
	private double rot;

	/**
	 * @param dataset the dataset being displayed
	 * @param xCentre the x centre position
	 * @param yCentre the y centre position
	 * @param rx      the x radius
	 * @param ry      the y radius
	 * @param shape   the shape to draw
	 * @param rot     the rotation angle in radians
	 * @author ben
	 *
	 */
	public VennShape(IAnalysisDataset dataset, double xCentre, double yCentre, double rx,
			double ry,
			VennShapeType shape, double rot) {
		this.dataset = dataset;
		this.xCentre = xCentre;
		this.yCentre = yCentre;
		this.rot = rot;
		this.rx = rx;
		this.ry = ry;

	}

	public VennShape(IAnalysisDataset dataset, double xCentre, double yCentre, double rx,
			double ry) {
		this(dataset, xCentre, yCentre, rx, ry, VennShapeType.CIRCLE, 0);
	}

	public VennShape(IAnalysisDataset dataset, double xCentre, double yCentre, double rx,
			double ry, VennShapeType shape) {
		this(dataset, xCentre, yCentre, rx, ry, shape, 0);
	}

	public IAnalysisDataset dataset() {
		return dataset;
	}

	public double xCentre() {
		return xCentre;
	}

	public double yCentre() {
		return yCentre;
	}

	public double yBottom() {
		return toShape().getBounds2D().getMinY() * 1.1;
	}

	public double yTop() {
		return toShape().getBounds2D().getMaxY() * 1.1;
	}

	public double xRadius() {
		return xDiameter() / 2;
	}

	public double yRadius() {
		return yDiameter() / 2;
	}

	public double xDiameter() {
		return xMax() - xMin();
	}

	public double yDiameter() {
		return yMax() - yMin();
	}

	public double xMax() {
		return toShape().getBounds2D().getMaxX();
	}

	public double xMin() {
		return toShape().getBounds2D().getMinX();
	}

	public double yMin() {
		return toShape().getBounds2D().getMinY();
	}

	public double yMax() {
		return toShape().getBounds2D().getMaxY();
	}

	/**
	 * Get the x coordinate at given fraction of the diameter
	 * 
	 * @param f
	 * @return
	 */
	public double xFraction(double f) {
		return (xDiameter() * f) + xMin();
	}

	public double yFraction(double f) {
		return (yDiameter() * f) + yMin();
	}

	private Shape toShape() {
		Area s = new Area(new Ellipse2D.Double(xCentre - rx, yCentre - ry, rx + rx,
				ry + ry));

		if (VennShapeType.HALF_CIRCLE_LEFT.equals(shape)) {
			Area ra = new Area(new Rectangle2D.Double(xCentre, yCentre - ry, rx, ry + ry));
			s.subtract(ra);
		}

		if (VennShapeType.HALF_CIRCLE_RIGHT.equals(shape)) {
			Area ra = new Area(new Rectangle2D.Double(xCentre - rx, yCentre - ry, rx, ry + ry));
			s.subtract(ra);
		}

		if (VennShapeType.THIRD_CIRCLE_UPPER.equals(shape)) {
			s = new Area(new Arc2D.Double(xCentre - rx, yCentre - rx, rx + rx, ry + ry, -150,
					120, Arc2D.PIE));
		}

		if (VennShapeType.THIRD_CIRCLE_LEFT.equals(shape)) {
			s = new Area(new Arc2D.Double(xCentre - rx, yCentre - rx, rx + rx, ry + ry, -270,
					120, Arc2D.PIE));
		}

		if (VennShapeType.THIRD_CIRCLE_RIGHT.equals(shape)) {
			s = new Area(new Arc2D.Double(xCentre - rx, yCentre - rx, rx + rx, ry + ry, -30,
					120, Arc2D.PIE));
		}

		if (rot != 0)
			s.transform(AffineTransform.getRotateInstance(rot));

		return s;
	}

	public XYShapeAnnotation toAnnotation(Color fill, Color outline,
			Stroke stroke) {
		return new XYShapeAnnotation(toShape(), stroke, outline, fill);
	}

}
