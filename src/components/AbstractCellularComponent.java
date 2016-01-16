package components;

import java.awt.Rectangle;
import java.io.File;
import java.io.Serializable;
import java.util.UUID;

public class AbstractCellularComponent implements CellularComponent, Serializable {

	private static final long serialVersionUID = 1L;
	protected UUID id;
	
	/**
	 * The original position in the source image of the component.
	 */
	protected double[] position;
	
	protected double area;
	protected double perimeter;
	
	/**
	 * The bounding rectangle for an oriented component.
	 * This is different to the original position bounds
	 * for nuclei and any other components with an orientation point
	 */
	protected Rectangle boundingRectangle;
	
	
	/**
	 * The folder containing the sourceFile. This is detected
	 * on dataset loading as a relative path from the .nmd
	 */
	private transient File sourceFolder;
	
	/**
	 * The name of the image which the component was detected
	 */
	private String sourceFileName;
	
	public AbstractCellularComponent(){
		this.id = java.util.UUID.randomUUID();
	}
	
	
	/**
	 * Duplicate a component. The ID is kept consistent.
	 * @param a
	 */
	public AbstractCellularComponent(AbstractCellularComponent a){
		this.id = a.getID();
		this.position = a.getPosition();
		this.area = a.getArea();
		this.perimeter = a.getPerimeter();
		this.boundingRectangle = a.getBounds();
		this.sourceFolder = a.getSourceFolder();
		this.sourceFileName = a.getSourceFileName();
	}
	
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
	
	/**
	 * Get the source folder for images
	 * @return
	 */
	public File getSourceFolder(){
		return this.sourceFolder;
	}
	
	/**
	 * Get the absolute path to the source image on the current
	 * computer. Merges the dynamic image folder with the image name
	 * @return
	 */
	public File getSourceFile(){
		return new File(this.sourceFolder.getAbsolutePath()+File.separator+this.getSourceFileName());
	}
	
	public String getSourceFileName(){
		return this.sourceFileName;
	}


	public void setId(UUID id) {
		this.id = id;
	}


	public void setPosition(double[] position) {
		this.position = position;
	}


	public void setArea(double area) {
		this.area = area;
	}


	public void setPerimeter(double perimeter) {
		this.perimeter = perimeter;
	}


	public void setBoundingRectangle(Rectangle boundingRectangle) {
		this.boundingRectangle = boundingRectangle;
	}


	public void setSourceFolder(File sourceFolder) {
		this.sourceFolder = sourceFolder;
	}


	@Override
	public boolean equals(CellularComponent c) {
		return false;
	}
	
	

}
