package components;

import java.awt.Rectangle;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import components.generic.BorderTag;
import components.generic.MeasurementScale;
import components.generic.XYPoint;
import components.nuclear.BorderPoint;
import ij.IJ;
import ij.gui.Roi;
import ij.process.FloatPolygon;
import stats.PlottableStatistic;
import stats.Stats;
import utility.Constants;
import utility.Utils;

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
	
	// The points around the border of the object
	private List<BorderPoint> borderList    = new ArrayList<BorderPoint>(0);
	
	public AbstractCellularComponent(){
		this.id = java.util.UUID.randomUUID();
	}
	
	/**
	 * Construct using an roi
	 * @param roi
	 */
	public AbstractCellularComponent(Roi roi){
		this();
		if(roi==null){
			throw new IllegalArgumentException("Constructor argument is null");
		}
		// convert the roi positions to a list of nucleus border points
		FloatPolygon polygon = roi.getInterpolatedPolygon(1,true);

		for(int i=0; i<polygon.npoints; i++){
			BorderPoint point = new BorderPoint( polygon.xpoints[i], polygon.ypoints[i]);

			if(i>0){
				point.setPrevPoint(borderList.get(i-1));
				point.prevPoint().setNextPoint(point);
			}
			borderList.add(point);
		}
		// link endpoints
		borderList.get(borderList.size()-1).setNextPoint(borderList.get(0));
		borderList.get(0).setNextPoint(borderList.get(borderList.size()-1));
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
		this.borderList        = a.getBorderList();
		this.centreOfMass      = new XYPoint(a.getCentreOfMass());
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
	
	/*
	 * 
	 * BORDER POINTS
	 * 
	 */
	
//	public BorderPoint getPoint(int i){
//		return new BorderPoint(this.borderList.get(i));
//	}
	
	public int getBorderLength(){
		return this.borderList.size();
	}


	public BorderPoint getBorderPoint(int i){
		return new BorderPoint(this.borderList.get(i));
	}
	
	public int getBorderIndex(BorderPoint p){
		int i = 0;
		for(BorderPoint n : borderList){
			if( n.getX()==p.getX() && n.getY()==p.getY()){
				return i;
			}
			i++;
		}
		IJ.log("Error: cannot find border point in Nucleus.getIndex()");
		return -1; // default if no match found
	}

	public void updateBorderPoint(int i, double x, double y){
		this.borderList.get(i).setX(x);
		this.borderList.get(i).setY(y);
	}
	
	public void updateBorderPoint(int i, XYPoint p){
		this.updateBorderPoint(i, p.getX(), p.getY());
	}

	public List<BorderPoint> getBorderList(){
		List<BorderPoint> result = new ArrayList<BorderPoint>(0);

		for(BorderPoint n : borderList){
			
			BorderPoint point = new BorderPoint(n);			
			result.add(point);
		}
		
		// Link points
				
		for(int i=0; i<result.size(); i++){
			BorderPoint point = result.get(i);
			
			if(i>0 && i<result.size()-1){
				point.setNextPoint(result.get(i+1));
				point.setPrevPoint(result.get(i-1));
			}
		}
		
		// Set first and last
		BorderPoint first = result.get(0);
		first.setNextPoint(result.get(1));
		first.setPrevPoint(result.get(result.size()-1));
		
		BorderPoint last = result.get(result.size()-1);
		last.setNextPoint(result.get(0));
		last.setPrevPoint(result.get(result.size()-2));
		
		return result;
	}
	
	public List<BorderPoint> getOriginalBorderList(){
		List<BorderPoint> result = new ArrayList<BorderPoint>(0);
		for(BorderPoint p : borderList){
			result.add(new BorderPoint( p.getX() + getPosition()[X_BASE], p.getY() + getPosition()[Y_BASE]));
		}
		return result;
	}
	
	public void setBorderList(List<BorderPoint> list){
		
		// ensure the new border list is linked properly
		for(int i=0; i<list.size(); i++){
			BorderPoint p = list.get(i);
			if(i>0){
				p.setPrevPoint(list.get(i-1));
				p.prevPoint().setNextPoint(p);
			}
		}
		list.get(list.size()-1).setNextPoint(list.get(0));
		list.get(0).setNextPoint(list.get(list.size()-1));
		this.borderList = list;
	}
	
	/**
	 * Check if a given point lies within the nucleus
	 * @param p
	 * @return
	 */
	public boolean containsPoint(XYPoint p){
		if(Utils.createPolygon(this.getBorderList()).contains( (float)p.getX(), (float)p.getY() ) ){
			return true;
		} else { 
			return false;
		}
	}
	
	/**
	 * Check if a given point lies within the nucleus
	 * @param p
	 * @return
	 */
	public boolean containsOriginalPoint(XYPoint p){
		if(Utils.createPolygon(this.getOriginalBorderList()).contains( (float)p.getX(), (float)p.getY() ) ){
			return true;
		} else { 
			return false;
		}
	}
	
	/*
	 * 
	 * GET MAX AND MIN BORDER POSITIONS
	 * 
	 */
	
	public double getMaxX(){
		double d = 0;
		for(int i=0;i<getBorderLength();i++){
			if(this.borderList.get(i).getX()>d){
				d = this.borderList.get(i).getX();
			}
		}
		return d;
	}

	public double getMinX(){
		double d = getMaxX();
		for(int i=0;i<getBorderLength();i++){
			if(this.borderList.get(i).getX()<d){
				d = this.borderList.get(i).getX();
			}
		}
		return d;
	}

	public double getMaxY(){
		double d = 0;
		for(int i=0;i<getBorderLength();i++){
			if(this.borderList.get(i).getY()>d){
				d = this.borderList.get(i).getY();
			}
		}
		return d;
	}

	public double getMinY(){
		double d = getMaxY();
		for(int i=0;i<getBorderLength();i++){
			if(this.borderList.get(i).getY()<d){
				d = this.borderList.get(i).getY();
			}
		}
		return d;
	}
	
	/*
	Flip the X positions of the border points around an X position
	 */
	public void flipXAroundPoint(XYPoint p){

		double xCentre = p.getX();

		for(BorderPoint n : borderList){
			double dx = xCentre - n.getX();
			double xNew = xCentre + dx;
			n.setX(xNew);
		}

	}

	public double getMedianDistanceBetweenPoints(){
		double[] distances = new double[this.borderList.size()];
		for(int i=0;i<this.borderList.size();i++){
			BorderPoint p = this.getBorderPoint(i);
			BorderPoint next = this.getBorderPoint( Utils.wrapIndex(i+1, this.borderList.size()));
			distances[i] = p.getLengthTo(next);
		}
		return Stats.quartile(distances, Constants.MEDIAN);
	}
	
	
	/**
	 * Translate the XY coordinates of each border point so that
	 * the nuclear centre of mass is at the given point
	 * @param point the new centre of mass
	 */
	public void moveCentreOfMass(XYPoint point){

		XYPoint centreOfMass = this.getCentreOfMass();
		
		// get the difference between the x and y positions 
		// of the points as offsets to apply
		double xOffset = point.getX() - centreOfMass.getX();
		double yOffset = point.getY() - centreOfMass.getY();

		// update the centre of mass
		

		/// update each border point
		for(int i=0; i<this.getBorderLength(); i++){
			XYPoint p = this.getBorderPoint(i);

			double x = p.getX() + xOffset;
			double y = p.getY() + yOffset;

			this.updateBorderPoint(i, x, y );
		}
		this.setCentreOfMass(point);
	}
	
	/**
	 * Translate the XY coordinates of each border point so that
	 * the nuclear centre of mass is at the given point
	 * @param point the new centre of mass
	 */
	public void offset(double xOffset, double yOffset){

		// get the existing centre of mass
		XYPoint centreOfMass = this.getCentreOfMass();

		// find the position of the centre of mass after 
		// adding the offsets
		double newX =  centreOfMass.getX() + xOffset;
		double newY =  centreOfMass.getY() + yOffset;

		XYPoint newCentreOfMass = new XYPoint(newX, newY);

		// update the positions
		this.moveCentreOfMass(newCentreOfMass);
	}
	
}
