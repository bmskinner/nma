/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package charting.charts.overlays;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;

public class ShapeOverlayObject extends OverlayObject{

	Shape shape = null;
	
	public ShapeOverlayObject(Shape shape){
		this(shape, new BasicStroke(1f),  Color.BLACK, null);
	}
	
	public ShapeOverlayObject(Shape shape, Stroke stroke, Paint outline){
		this(shape, stroke, outline, null);
	}
	
	public ShapeOverlayObject(Shape shape, Stroke stroke, Paint outline, Paint fill){
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
