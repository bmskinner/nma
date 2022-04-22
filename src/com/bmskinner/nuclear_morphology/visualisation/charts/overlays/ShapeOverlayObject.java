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
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;

public class ShapeOverlayObject extends OverlayObject {

	Shape shape = null;

	public static Shape createDiamond(float s, double x, double y) {
		AffineTransform at = AffineTransform.getTranslateInstance(x, y);
		final GeneralPath p0 = new GeneralPath();
		p0.moveTo(0f, -s);
		p0.lineTo(s, 0f);
		p0.lineTo(0f, s);
		p0.lineTo(-s, 0f);
		p0.closePath();

		return at.createTransformedShape(p0);
	}

	public ShapeOverlayObject(Shape shape) {
		this(shape, new BasicStroke(1f), Color.BLACK, null);
	}

	public ShapeOverlayObject(Shape shape, Stroke stroke, Paint outline) {
		this(shape, stroke, outline, null);
	}

	/**
	 * Create a shape overlay at the given coordinates
	 * 
	 * @param shape   the shape to draw
	 * @param stroke  the stroke
	 * @param outline the outline colour
	 * @param fill    the fill colour
	 */
	public ShapeOverlayObject(Shape shape, Stroke stroke, Paint outline, Paint fill) {
		super(stroke, outline, fill);
		this.shape = shape;
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
}
