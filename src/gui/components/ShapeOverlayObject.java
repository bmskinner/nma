package gui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public class ShapeOverlayObject implements PropertyChangeListener {
	
	private Shape shape;
	private Stroke stroke;
	private Paint fill;
	private Paint outline;
	
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

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		// TODO Auto-generated method stub
		
	}
	
	

}
