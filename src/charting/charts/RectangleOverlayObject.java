package charting.charts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class RectangleOverlayObject {

	private Stroke stroke;
	private Paint fill;
	private Paint outline;
	private boolean isVisible;
	private PropertyChangeSupport pcs;
	private double xMinValue;
	private double xMaxValue;
	private double yMinValue;
	private double yMaxValue;
		
	public RectangleOverlayObject(double xMin, double xMax, double yMin, double yMax){
		this(xMin, xMax, yMin, yMax, new BasicStroke(1f),  Color.BLACK, new Color(128, 128, 128, 128));
	}
	
	public RectangleOverlayObject(double xMin, double xMax, double yMin, double yMax, Stroke stroke, Paint outline){
		this(xMin, xMax, yMin, yMax, stroke, outline, new Color(100, 100, 100, 128));
	}
	
	public RectangleOverlayObject(double xMin, double xMax, double yMin, double yMax, Stroke stroke, Paint outline, Paint fill){
		this.xMinValue = xMin;
		this.xMaxValue = xMax;
		this.yMinValue = yMin;
		this.yMaxValue = yMax;
		this.stroke   = stroke;
		this.outline  = outline;
		this.fill     = fill;
		this.isVisible = true;
		this.pcs      = new PropertyChangeSupport(this);
	}


	public double getXMinValue() {
		return xMinValue;
	}

	public void setXMinValue(double minValue) {
		double oldValue = this.xMinValue;
		this.xMinValue = minValue;
		this.pcs.firePropertyChange("minXValue", oldValue, minValue);
	}

	public double getXMaxValue() {
		return xMaxValue;
	}

	public void setXMaxValue(double maxValue) {
		double oldValue = this.xMaxValue;
		this.xMaxValue = maxValue;
		this.pcs.firePropertyChange("maxXValue", oldValue, maxValue);
	}
	
	public double getXMidValue(){
		return ((xMaxValue - xMinValue) /2) +xMinValue;
	}
	
	
	public double getYMinValue() {
		return yMinValue;
	}

	public void setYMinValue(double minValue) {
		double oldValue = this.yMinValue;
		this.yMinValue = minValue;
		this.pcs.firePropertyChange("minYValue", oldValue, minValue);
	}

	public double getYMaxValue() {
		return yMaxValue;
	}

	public void setYMaxValue(double maxValue) {
		double oldValue = this.yMaxValue;
		this.yMaxValue = maxValue;
		this.pcs.firePropertyChange("maxYValue", oldValue, maxValue);
	}
	
	public double getYMidValue(){
		return ((yMaxValue - yMinValue) /2) +yMinValue;
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
