package components;

import java.awt.Rectangle;
import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import components.generic.MeasurementScale;
import components.generic.XYPoint;
import stats.PlottableStatistic;

public class AbstractCellularComponent implements CellularComponent, Serializable {

	private static final long serialVersionUID = 1L;
	private final UUID id;
	
	/**
	 * The original position in the source image of the component.
	 */

	private double[] position;
	
	private XYPoint centreOfMass;
	
	private Map<PlottableStatistic, Double> statistics = new HashMap<PlottableStatistic, Double>();
		
	/**
	 * The bounding rectangle for an oriented component.
	 * This is different to the original position bounds
	 * for nuclei and any other components with an orientation point
	 */
	private Rectangle boundingRectangle;
	
	
	/**
	 * The folder containing the sourceFile. This is detected
	 * on dataset loading as a relative path from the .nmd
	 */
	private File sourceFolder;
	

	/**
	 * The name of the image which the component was detected
	 */
	private String sourceFileName;
	
	private int channel; // the RGB channel in which the signal was seen
	
	private double scale = 1; // allow conversion between pixels and SI units. The length of a pixel in microns
	
	public AbstractCellularComponent(){
		this.id = java.util.UUID.randomUUID();
	}
	
	
	/**
	 * Duplicate a component. The ID is kept consistent.
	 * @param a
	 */
	public AbstractCellularComponent(CellularComponent a){
		this.id = a.getID();
		this.position = a.getPosition();
		
		for(PlottableStatistic stat : a.getStatistics() ){
			try {
				this.setStatistic(stat, a.getStatistic(stat, MeasurementScale.PIXELS));
			} catch (Exception e) {
				this.setStatistic(stat, 0);
			}
		}

		this.boundingRectangle = a.getBounds();
		this.sourceFolder      = a.getSourceFolder();
		this.sourceFileName    = a.getSourceFileName();
		this.channel           = a.getChannel();
		this.scale 			   = a.getScale();
	}
	
	public UUID getID() {
		return this.id;
	}
	

	public double[] getPosition() {
		return this.position;
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

	public void setPosition(double[] position) {
		this.position = position;
	}

	public void setBoundingRectangle(Rectangle boundingRectangle) {
		this.boundingRectangle = boundingRectangle;
	}


	public void setSourceFileName(String name) {
		this.sourceFileName = name;
	}
	
	public void setSourceFolder(File sourceFolder) {
		this.sourceFolder = sourceFolder;
	}
	
	public void setSourceFile(File sourceFile){
		setSourceFolder(  sourceFile.getParentFile());
		setSourceFileName(sourceFile.getName()      );
	}
	


	public int getChannel() {
		return channel;
	}


	public void setChannel(int channel) {
		this.channel = channel;
	}
	
	public double getScale(){
		return this.scale;
	}
	
	public void setScale(double scale){
		this.scale = scale;
	}


	@Override
	public boolean equals(CellularComponent c) {
		return false;
	}

	@Override
	public double getStatistic(PlottableStatistic stat, MeasurementScale scale) throws Exception {
		if(this.statistics.containsKey(stat)){
			double result = this.statistics.get(stat);
			result = stat.convert(result, this.getScale(), scale);
			return result;
		} else {
			double result = calculateStatistic(stat);
			setStatistic(stat, result);
			return result;
		}
	}
	
	// For subclasses to override
	protected double calculateStatistic(PlottableStatistic stat) throws Exception{
		return 0;
	}
	
	@Override
	public double getStatistic(PlottableStatistic stat) throws Exception {
		return this.getStatistic(stat, MeasurementScale.PIXELS);
	}


	@Override
	public void setStatistic(PlottableStatistic stat, double d) {
		this.statistics.put(stat, d);
	}


	@Override
	public PlottableStatistic[] getStatistics() {
		return this.statistics.keySet().toArray(new PlottableStatistic[0]);
	}
	
	public XYPoint getCentreOfMass() {
		return centreOfMass;
	}


	public void setCentreOfMass(XYPoint centreOfMass) {
		this.centreOfMass = centreOfMass;
	}

}
