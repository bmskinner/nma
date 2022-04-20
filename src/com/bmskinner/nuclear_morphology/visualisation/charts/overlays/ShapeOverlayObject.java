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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;

public class ShapeOverlayObject extends OverlayObject {

	private double xValue;
	private double yValue;
	Shape shape = null;

	public ShapeOverlayObject(Shape shape, double xValue, double yValue) {
		this(shape, xValue, yValue, new BasicStroke(1f), Color.BLACK, null);
	}

	public ShapeOverlayObject(Shape shape, double xValue, double yValue, Stroke stroke, Paint outline) {
		this(shape, xValue, yValue, stroke, outline, null);
	}

	public ShapeOverlayObject(Shape shape, double xValue, double yValue, Stroke stroke, Paint outline, Paint fill) {
		super(stroke, outline, fill);
		this.shape = shape;
		this.xValue = xValue;
		this.yValue = yValue;
	}

	public Shape getShape() {
		return shape;
	}

	public void setShape(Shape shape) {
		this.shape = shape;
	}

	@Override
	public boolean contains(int x, int y) {
		return shape.contains(y, y);
	}

	@Override
	public boolean contains(double x, double y) {
		return shape.contains(y, y);
	}

	public double getXValue() {
		return xValue;
	}

	public void setXValue(double value) {
		double oldValue = this.xValue;
		this.xValue = value;
		this.pcs.firePropertyChange("xValue", oldValue, value);
	}

	public double getYValue() {
		return yValue;
	}

	public void setYValue(double value) {
		double oldValue = this.yValue;
		this.yValue = value;
		this.pcs.firePropertyChange("yValue", oldValue, value);
	}

}
