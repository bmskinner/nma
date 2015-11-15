package components;

import java.awt.Rectangle;
import java.io.Serializable;
import java.util.UUID;

public class AbstractCellularComponent implements CellularComponent, Serializable {

	private UUID id;
	private double[] position;
	private double area;
	private double perimeter;
	private Rectangle boundingRectangle;
	
	public UUID getID() {
		return this.id;
	}

	public double[] getPosition() {
		return this.position;
	}

	public double getArea() {
		return this.area;
	}
	
	public double getPerimeter() {
		return this.perimeter;
	}
	
	public Rectangle getBounds() {
		return this.boundingRectangle;
	}

}
