package gui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class RectangleOverlayObject {

	private Stroke stroke;
	private Paint fill;
	private Paint outline;
	private boolean isVisible;
	private PropertyChangeSupport pcs;
	private double minValue;
	private double maxValue;
		
	public RectangleOverlayObject(double min, double max){
		this(min, max, new BasicStroke(1f),  Color.BLACK, new Color(128, 128, 128, 128));
	}
	
	public RectangleOverlayObject(double min, double max, Stroke stroke, Paint outline){
		this(min, max, stroke, outline, new Color(100, 100, 100, 128));
	}
	
	public RectangleOverlayObject(double min, double max, Stroke stroke, Paint outline, Paint fill){
		this.minValue = min;
		this.maxValue = max;
		this.stroke   = stroke;
		this.outline  = outline;
		this.fill     = fill;
		this.isVisible = true;
		this.pcs      = new PropertyChangeSupport(this);
	}


	public double getMinValue() {
		return minValue;
	}

	public void setMinValue(double minValue) {
		double oldValue = this.minValue;
		this.minValue = minValue;
		this.pcs.firePropertyChange("minValue", oldValue, minValue);
	}

	public double getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(double maxValue) {
		double oldValue = this.maxValue;
		this.maxValue = maxValue;
		this.pcs.firePropertyChange("maxValue", oldValue, maxValue);
	}
	
	public double getMidValue(){
		return (maxValue - minValue) /2;
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
