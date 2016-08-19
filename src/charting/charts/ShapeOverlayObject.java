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
package charting.charts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

public class ShapeOverlayObject {
	
	private Shape shape;
	private Stroke stroke;
	private Paint fill;
	private Paint outline;
	private boolean isVisible;
	private PropertyChangeSupport pcs;
	
//	List<Object> propertyChangeListeners = new ArrayList<Object>();
	
	public ShapeOverlayObject(Shape shape){
		this(shape, new BasicStroke(1f),  Color.BLACK, null);
	}
	
	public ShapeOverlayObject(Shape shape, Stroke stroke, Paint outline){
		this(shape, stroke, outline, null);
	}
	
	public ShapeOverlayObject(Shape shape, Stroke stroke, Paint outline, Paint fill){
		this.shape = shape;
		this.stroke = stroke;
		this.outline = outline;
		this.fill = fill;
		this.pcs = new PropertyChangeSupport(this);
	}

	public Shape getShape() {
		return shape;
	}

	public void setShape(Shape shape) {
		this.shape = shape;
	}

	public Stroke getStroke() {
		return stroke;
	}

	public void setStroke(Stroke stroke) {
		this.stroke = stroke;
	}

	public Paint getFill() {
		return fill;
	}

	public void setFill(Paint fill) {
		this.fill = fill;
	}

	public Paint getOutline() {
		return outline;
	}

	public void setOutline(Paint outline) {
		this.outline = outline;
	}
	

	public boolean isVisible() {
		return isVisible;
	}

	public void setVisible(boolean isVisible) {
		this.isVisible = isVisible;
	}
	
	 /**
     * Adds a property change listener.
     *
     * @param l  the listener.
     */
    public void addPropertyChangeListener(PropertyChangeListener l) {
        this.pcs.addPropertyChangeListener(l);
    }

    /**
     * Removes a property change listener.
     *
     * @param l  the listener.
     */
    public void removePropertyChangeListener(PropertyChangeListener l) {
        this.pcs.removePropertyChangeListener(l);
    }
	
	

}
