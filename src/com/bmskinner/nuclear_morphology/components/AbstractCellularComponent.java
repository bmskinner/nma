/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
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
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.components;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import com.bmskinner.nuclear_morphology.analysis.detection.BooleanMask;
import com.bmskinner.nuclear_morphology.analysis.detection.Mask;
import com.bmskinner.nuclear_morphology.analysis.image.ImageConverter;
import com.bmskinner.nuclear_morphology.components.generic.DefaultBorderPoint;
import com.bmskinner.nuclear_morphology.components.generic.FloatPoint;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.XYPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.NuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;
import com.bmskinner.nuclear_morphology.stats.Quartile;
import com.bmskinner.nuclear_morphology.utility.AngleTools;

import ij.ImageStack;
import ij.gui.Roi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;


/**
 * @author bms41
 * @deprecated from version 1.13.3. Only active components should be used to create new
 *             objects. The class is retained to allow old format datasets to be read 
 *             and converted. 
 */
@Deprecated
public abstract class AbstractCellularComponent 
	implements CellularComponent,
	           Rotatable {

	private static final long serialVersionUID = 1L;
	private final UUID id;
	
	/**
	 * The original position in the source image of the component.
	 * Values are stored at the indexes in {@link CellularComponent.X_BASE}, 
	 * {@link CellularComponent.Y_BASE}, {@link CellularComponent.WIDTH} 
	 * and {@link CellularComponent.HEIGHT}
	 * @see AbstractCellularComponent#getPosition()
	 */
	private int[] position;
	
	/**
	 * The centre of the object.
	 */
	private IPoint centreOfMass = IPoint.makeNew(0,0);
	
	/**
	 * The statistical values stored for this object, which should
	 * be an enum implementing {@link PlottableStatistic}
	 */
	private Map<PlottableStatistic, Double> statistics = new HashMap<PlottableStatistic, Double>();
			
	
	/**
	 * The folder containing the sourceFile. This is detected
	 * on dataset loading as a relative path from the .nmd
	 * e.g. C:\MyImageFolder\
	 */
	private File sourceFolder;
	

	/**
	 * The name of the image which the component was detected
	 * e.g. MyImage.tiff
	 */
	private String sourceFileName;
	
	/**
	 * The RGB channel in which the signal was seen
	 */
	private int channel;
	
	/**
	 * The length of a micron in pixels. Allows conversion between pixels and SI units. 
	 * Set to 1 by default. 
	 * @see AbstractCellularComponent#setScale()
	 */
	private double scale = 1; 
	
	/**
	 * The points around the border of the object. 
	 */
	private List<IBorderPoint> borderList    = new ArrayList<IBorderPoint>(0);
	
	/*
	 * TRANSIENT FILEDS
	 */
	
	private transient SoftReference<ImageProcessor> imageRef = new SoftReference<ImageProcessor>(null); // allow caching of images while memory is available
	
	
	public AbstractCellularComponent(){
		this.id = java.util.UUID.randomUUID();
	}
	
	/**
	 * Construct with an ROI, and a source image and channel
	 * @param roi
	 * @param f
	 * @param channel
	 */
	public AbstractCellularComponent(Roi roi, File f, int channel){
		this(roi);
		this.setSourceFile(f);
		this.setChannel(channel);
	}
	
	/**
	 * Construct with an ROI, a source image and channel, and the original position in the source image
	 * @param roi
	 * @param f
	 * @param channel
	 * @param position
	 */
	public AbstractCellularComponent(Roi roi, File f, int channel, int[] position){
		this(roi, f, channel);
		this.position = position;
	}
	
	/**
	 * Construct with an ROI, a source image and channel, and the original position in the source image
	 * @param roi
	 * @param f
	 * @param channel
	 * @param position
	 * @param centreOfMass
	 */
	public AbstractCellularComponent(Roi roi, File f, int channel, int[] position, IPoint centreOfMass){
		this(roi, f, channel, position);
		this.centreOfMass = centreOfMass;
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
		
		
		boolean smooth = this instanceof NuclearSignal ? false : true;
		
		// convert the roi positions to a list of nucleus border points
		// Only smooth the points for large objects like nuclei
		// Signals must be kept unsmoothed
		FloatPolygon polygon = roi.getInterpolatedPolygon(1,smooth);

		for(int i=0; i<polygon.npoints; i++){
			IBorderPoint point = new DefaultBorderPoint( polygon.xpoints[i], polygon.ypoints[i]);

			if(i>0){
				point.setPrevPoint(borderList.get(i-1));
				point.prevPoint().setNextPoint(point);
			}
			borderList.add(point);
		}
		// link endpoints
		borderList.get(borderList.size()-1).setNextPoint(borderList.get(0));
		borderList.get(0).setPrevPoint(borderList.get(borderList.size()-1));
		
//		this.boundingRectangle = new Rectangle(polygon.getBounds());
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
				fine("Error setting statistic: "+stat, e);
				this.setStatistic(stat, 0);
			}
		}

//		this.boundingRectangle = a.getBounds();
		this.sourceFolder      = a.getSourceFolder();
		this.sourceFileName    = a.getSourceFileName();
		this.channel           = a.getChannel();
		this.scale 			   = a.getScale();
		
		// Duplicate the border points
		this.borderList = new ArrayList<IBorderPoint>(a.getBorderLength());
		
		for(IBorderPoint p : a.getBorderList()){
			borderList.add( IBorderPoint.makeNew(p));
		}
		
		// Link points
				
		for(int i=0; i<borderList.size(); i++){
			IBorderPoint point = borderList.get(i);
			
			if(i>0 && i<borderList.size()-1){
				point.setNextPoint(borderList.get(i+1));
				point.setPrevPoint(borderList.get(i-1));
			}
		}
		
		// Set first and last
		IBorderPoint first = borderList.get(0);
		first.setNextPoint(borderList.get(1));
		first.setPrevPoint(borderList.get(borderList.size()-1));
		
		IBorderPoint last = borderList.get(borderList.size()-1);
		last.setNextPoint(borderList.get(0));
		last.setPrevPoint(borderList.get(borderList.size()-2));
		
		
//		this.borderList        = a.getBorderList();
		this.centreOfMass      = IPoint.makeNew(a.getCentreOfMass());
	}
	

	
	public UUID getID() {
		return this.id;
	}
	

	public int[] getPosition() {
		return this.position;
	}
	
	public IPoint getOriginalBase(){
		return IPoint.makeNew(position[X_BASE], position[Y_BASE]);
	}
	
	public Rectangle getBounds() {
		return this.toShape().getBounds();
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
	
	public ImageProcessor getImage() throws UnloadableImageException{
		
		ImageProcessor ip = imageRef.get();
		if(ip !=null){
			return ip;
		}
		

		if(getSourceFile().exists()){
			
			// Get the stack, make greyscale and invert
			int stack = ImageImporter.rgbToStack(getChannel());
			
			try {
				ImageStack imageStack = new ImageImporter(getSourceFile()).importToStack();
				ip = new ImageConverter(imageStack).convertToGreyscale(stack).toProcessor();
				ip.invert();	
				
				imageRef = new SoftReference<ImageProcessor>(ip);
				
				return ip;
				
			} catch (ImageImportException e) {
				error("Error importing source image "+this.getSourceFile().getAbsolutePath(), e);
				return null;
			}
			
		} else {
			throw new UnloadableImageException("Source image is not available");
//			return null;
		}
	}
	

	public ImageProcessor getComponentImage() throws UnloadableImageException{
		ImageProcessor ip = getImage();

		if(ip==null){	
			return null;	
		}
		
		int[] positions = getPosition();
		
		int padding = CellularComponent.COMPONENT_BUFFER; // a border of pixels beyond the cell boundary
		int wideW = (int) (positions[CellularComponent.WIDTH]+(padding*2));
		int wideH = (int) (positions[CellularComponent.HEIGHT]+(padding*2));
		int wideX = (int) (positions[CellularComponent.X_BASE]-padding);
		int wideY = (int) (positions[CellularComponent.Y_BASE]-padding);

		wideX = wideX<0 ? 0 : wideX;
		wideY = wideY<0 ? 0 : wideY;

		ip.setRoi(wideX, wideY, wideW, wideH);
		ip = ip.crop();

		return ip;
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
	
	public synchronized boolean hasStatistic(PlottableStatistic stat){
		return this.statistics.containsKey(stat);
	}

	@Override
	public synchronized double getStatistic(PlottableStatistic stat, MeasurementScale scale) {
		if(this.statistics.containsKey(stat)){
			finest("Fetching stat "+stat);
			double result = statistics.get(stat);
			result = stat.convert(result, this.getScale(), scale);
			return result;
		} else {
//			finest("Calculating stat "+stat);
			double result = calculateStatistic(stat);
//			finest("Setting stat "+stat+": "+result);
			setStatistic(stat, result);
//			finest("Set stat "+stat+"; returning");
			return result;
		}
	}
	
	
	// For subclasses to override
	protected synchronized double calculateStatistic(PlottableStatistic stat){
//		finest("Abstract method for calculating stat: "+stat);
		return 0;
	}
	
	@Override
	public synchronized double getStatistic(PlottableStatistic stat) {
		return this.getStatistic(stat, MeasurementScale.PIXELS);
	}


	@Override
	public synchronized void setStatistic(PlottableStatistic stat, double d) {
		this.statistics.put(stat, d);
	}


	@Override
	public PlottableStatistic[] getStatistics() {
		return this.statistics.keySet().toArray(new PlottableStatistic[0]);
	}
	
	public IPoint getCentreOfMass() {
		return centreOfMass;
	}
	
	public IPoint getOriginalCentreOfMass() {
		
		double minX = this.getBounds().getX();
		double minY = this.getBounds().getY();
		
		double diffX = position[CellularComponent.X_BASE] - minX;
		double diffY = position[CellularComponent.Y_BASE] - minY;
		
		IPoint com = IPoint.makeNew(centreOfMass.getX()+diffX, centreOfMass.getY()+diffY);
		
		return com;
	}
	
	public void updateDependentStats(){
		
		for(PlottableStatistic stat : this.getStatistics()){
			
			if(this.getStatistic(stat)==STAT_NOT_CALCULATED){
				this.setStatistic(stat, calculateStatistic(stat));
			}
		}
		
	}

	
	
	/**
	 * This is used only when creating nuclei in the NucleusFinder, since we can't
	 * disrupt border positions. It sets the centre of mass without moving any of
	 * the border points
	 * @param centreOfMass
	 */
	public void setCentreOfMassDirectly(IPoint centreOfMass) {
		this.centreOfMass = centreOfMass;
	}

	
	/*
	 * 
	 * BORDER POINTS
	 * 
	 */
	

	public int getBorderLength(){
		return this.borderList.size();
	}


	public IBorderPoint getBorderPoint(int i){
		return this.borderList.get(i);
	}
	
	public IBorderPoint getOriginalBorderPoint(int i){
		IBorderPoint p = getBorderPoint(i);
		return new DefaultBorderPoint( p.getX() + getPosition()[X_BASE], p.getY() + getPosition()[Y_BASE]);
	}
	
	public int getBorderIndex(IBorderPoint p){
		int i = 0;
		for(IBorderPoint n : borderList){
			if( n.getX()==p.getX() && n.getY()==p.getY()){
				return i;
			}
			i++;
		}
		return -1; // default if no match found
	}

	public void updateBorderPoint(int i, double x, double y){
		this.borderList.get(i).setX(x);
		this.borderList.get(i).setY(y);
	}
	
	public void updateBorderPoint(int i, IPoint p){
		this.updateBorderPoint(i, p.getX(), p.getY());
	}

	public List<IBorderPoint> getBorderList(){
		
		return this.borderList;
		
		/*
		 * If key points only are stored, the full border list must be constructed
		 */
//		float[] xpoints = new float[borderList.size()];
//		float[] ypoints = new float[borderList.size()];
//		
//		for(int i=0; i<borderList.size();i++){
//			IBorderPoint point = borderList.get(i);
//			xpoints[i] = (float) point.getX();
//			ypoints[i] = (float) point.getY();
//		}
//				
//		PolygonRoi roi = new PolygonRoi(xpoints, ypoints, Roi.POLYGON);
//		
//		FloatPolygon p = roi.getInterpolatedPolygon(1f, false);
//		
//		List<IBorderPoint> result = new ArrayList<IBorderPoint>(p.npoints);
//		for(int i=0; i<p.npoints ;i++){
//			IBorderPoint point = new DefaultBorderPoint(p.xpoints[i], p.ypoints[i]);	
//			result.add(point);
//		}
		
		/*
		 * Old method
		 */
		
//		List<IBorderPoint> result = new ArrayList<IBorderPoint>(borderList.size());
//		for(IBorderPoint n : borderList){
//			
//			IBorderPoint point = new DefaultBorderPoint(n);			
//			result.add(point);
//		}
//		
//		// Link points
//				
//		for(int i=0; i<result.size(); i++){
//			IBorderPoint point = result.get(i);
//			
//			if(i>0 && i<result.size()-1){
//				point.setNextPoint(result.get(i+1));
//				point.setPrevPoint(result.get(i-1));
//			}
//		}
//		
//		// Set first and last
//		IBorderPoint first = result.get(0);
//		first.setNextPoint(result.get(1));
//		first.setPrevPoint(result.get(result.size()-1));
//		
//		IBorderPoint last = result.get(result.size()-1);
//		last.setNextPoint(result.get(0));
//		last.setPrevPoint(result.get(result.size()-2));
//		
//		return result;
	}
	
	public List<IBorderPoint> getOriginalBorderList(){
		
//		log("Getting original border list");
		
		List<IBorderPoint> result = new ArrayList<IBorderPoint>(borderList.size());
		
		// Get the current position of the object
		double minX = this.getBounds().getX();
		double minY = this.getBounds().getY();

		// Find the difference between the current position and the 
		// original position
		double diffX = position[CellularComponent.X_BASE] - minX;
		double diffY = position[CellularComponent.Y_BASE] - minY;
		
		// Offset to the original position
		for(IBorderPoint p : borderList){
			result.add( IBorderPoint.makeNew(p.getX()+diffX, p.getY()+diffY));
		}
		return result;
		
		
//		List<IBorderPoint> result = new ArrayList<IBorderPoint>(borderList.size());
//		for(IBorderPoint p : borderList){
//			result.add(new DefaultBorderPoint( p.getX() + getPosition()[X_BASE], p.getY() + getPosition()[Y_BASE]));
//		}
//		return result;
	}
	
	public void setBorderList(List<IBorderPoint> list){
//		
//		log("Setting border list");
		// ensure the new border list is linked properly
		for(int i=0; i<list.size(); i++){
			IBorderPoint p = list.get(i);
			if(i>0){
				p.setPrevPoint(list.get(i-1));
				p.prevPoint().setNextPoint(p);
			}
		}
		list.get(list.size()-1).setNextPoint(list.get(0));
		list.get(0).setPrevPoint(list.get(list.size()-1));
		this.borderList = list;
	}
	
	/**
	 * Check if a given point lies within the nucleus
	 * @param p
	 * @return
	 */
	public boolean containsPoint(IPoint p){
		
		// Fast check - is the point within the bounding rectangle?
		if( ! this.getBounds().contains(p.toPoint2D())){
			return false;			
		} 
		
		// Check detailed position
		if(this.createPolygon().contains( (float)p.getX(), (float)p.getY() ) ){
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
	public boolean containsPoint(int x, int y){
		// Check detailed position
		
		return this.toShape().contains(x, y);

		
	}
	
	/**
	 * Check if a given point in the original source image lies within the nucleus
	 * @param p
	 * @return
	 */
	public boolean containsOriginalPoint(IPoint p){

		// Check detailed position
		return this.createOriginalPolygon().contains( (float)p.getX(), (float)p.getY() );
	}
	
	/**
	 * Check if a given point lies within the nucleus
	 * @param p
	 * @return
	 */
	public boolean containsOriginalPoint(int x, int y){
		
		// Fast check - is the point within the bounding rectangle moved to the original position?
				Rectangle2D r = new Rectangle2D.Double(position[X_BASE], position[Y_BASE], position[WIDTH], position[HEIGHT]);
				
				if( ! r.contains(x, y)){
					return false;			
				} 
		
		// Check detailed position
		
		return this.toOriginalShape().contains(x, y);		
	}
		
	/*
	 * 
	 * GET MAX AND MIN BORDER POSITIONS
	 * 
	 */
	
	public double getMaxX(){
		
		return this.toShape().getBounds().getMaxX();
//		double d = 0;
//		for(int i=0;i<getBorderLength();i++){
//			if(this.borderList.get(i).getX()>d){
//				d = this.borderList.get(i).getX();
//			}
//		}
//		return d;
	}

	public double getMinX(){
		return this.toShape().getBounds().getMinX();
//		double d = getMaxX();
//		for(int i=0;i<getBorderLength();i++){
//			if(this.borderList.get(i).getX()<d){
//				d = this.borderList.get(i).getX();
//			}
//		}
//		return d;
	}

	public double getMaxY(){
		return this.toShape().getBounds().getMaxY();
//		double d = 0;
//		for(int i=0;i<getBorderLength();i++){
//			if(this.borderList.get(i).getY()>d){
//				d = this.borderList.get(i).getY();
//			}
//		}
//		return d;
	}

	public double getMinY(){
		return this.toShape().getBounds().getMinY();
//		double d = getMaxY();
//		for(int i=0;i<getBorderLength();i++){
//			if(this.borderList.get(i).getY()<d){
//				d = this.borderList.get(i).getY();
//			}
//		}
//		return d;
	}
	
	/*
	Flip the X positions of the border points around an X position
	 */
	public void flipXAroundPoint(IPoint p){
//		log("Flipping about X");
		double xCentre = p.getX();

		for(IBorderPoint n : borderList){
			double dx = xCentre - n.getX();
			double xNew = xCentre + dx;
			n.setX(xNew);
		}

	}

	public double getMedianDistanceBetweenPoints(){
		double[] distances = new double[this.borderList.size()];
		for(int i=0;i<this.borderList.size();i++){
			IBorderPoint p = this.getBorderPoint(i);
			IBorderPoint next = this.getBorderPoint( wrapIndex(i+1, this.borderList.size()));
			distances[i] = p.getLengthTo(next);
		}
		return new Quartile(distances, Quartile.MEDIAN).doubleValue();
	}
	
	
	/**
	 * Translate the XY coordinates of each border point so that
	 * the nuclear centre of mass is at the given point
	 * @param point the new centre of mass
	 */
	public void moveCentreOfMass(IPoint point){
//		log("Moving CoM");

		// get the difference between the x and y positions 
		// of the points as offsets to apply
		double xOffset = point.getX() - centreOfMass.getX();
		double yOffset = point.getY() - centreOfMass.getY();		

		offset(xOffset, yOffset);
	}
	
	/**
	 * Translate the XY coordinates of the object
	 * @param xOffset the amount to move in the x-axis
	 * @param yOffset the amount to move in the y-axis
	 */
	public void offset(double xOffset, double yOffset){

//		log("Offsetting component by "+xOffset+", "+yOffset);
		// find the position of the centre of mass after 
		// adding the offsets
		double newX =  centreOfMass.getX() + xOffset;
		double newY =  centreOfMass.getY() + yOffset;
//
		IPoint newCentreOfMass = IPoint.makeNew(newX, newY);

		/// update each border point
		for(int i=0; i<this.getBorderLength(); i++){
			IPoint p = this.getBorderPoint(i);

			double x = p.getX() + xOffset;
			double y = p.getY() + yOffset;

			this.updateBorderPoint(i, x, y );
		}

		this.centreOfMass = newCentreOfMass;

		// Update the bounding rectangle
//		this.boundingRectangle = this.createPolygon().getBounds();
	}
	
	/**
	 * Turn the list of border points into a polygon. The points are at the original
	 * position in a source image.
	 * @see Nucleus.getPosition
	 * @return
	 */
	public FloatPolygon createOriginalPolygon(){
		
		double minX = this.getBounds().getX();
		double minY = this.getBounds().getY();
		
		double diffX = position[CellularComponent.X_BASE] - minX;
		double diffY = position[CellularComponent.Y_BASE] - minY;

		return createOffsetPolygon(  (float) diffX,
				                     (float) diffY);
	}

	/**
	 * Turn the list of border points into a closed polygon. 
	 * @return
	 */
	public FloatPolygon createPolygon(){
		return createOffsetPolygon(0, 0);
	}
	
	public Shape toShape(){
		
		// Converts whatever coordinates are in the border
		// to a shape
		return toOffsetShape(0, 0);
		
	}
	
	public Shape toOriginalShape(){
		
		// The object is created, and the border points are
		// against a x=0, y=0 boundary
		
		// Situation: the object has been moved since creation.
		
		// Now the borders cannot be guaranteed to be on the
		// x=0,y=0 lines.
		
		// Moving by the X_BASE and Y_BASE offsets will not help.
		
		// Calculate the difference between the min x and min y of the 
		// borders, and apply this offset
				
		double diffX = position[CellularComponent.X_BASE] - getMinX();
		double diffY = position[CellularComponent.Y_BASE] - getMinY();
		
		return toOffsetShape(diffX, diffY);
	}
	
	public List<IPoint> getPixelsAsPoints(){
			
		// Get a list of all the points within the ROI
		List<IPoint> result = new ArrayList<IPoint>(0);
		
		Rectangle b = this.toShape().getBounds();
		
		// get the bounding box of the roi
		// make a list of all the pixels in the roi
		int minX = (int) b.getMinX();
		int maxX = (int) (minX + b.getWidth());
		
		int minY = (int) b.getMinY();
		int maxY = minY + (int) b.getHeight();
		
		
//		IJ.log("    X base: "+minX
//				+"  Y base: "+minY
//				+"  X max: "+maxX
//				+"  Y max: "+maxY);
		
		for(int x=minX; x<=maxX; x++){
			for(int y=minY; y<=maxY; y++){
				
				if(this.containsPoint(x, y)){
//					IJ.log(x+", "+y);
					result.add(IPoint.makeNew(x, y));
				}
			}
		}
		
		if(result.isEmpty()){
//			IJ.log("    Roi has no pixels");
			log(Level.SEVERE, "No points found in roi");
			log(Level.FINE, "X base: "+minX
					+"  Y base: "+minY
					+"  X max: "+maxX
					+"  Y max: "+maxY);
		} else {
//			IJ.log("    Roi of area "+result.size());
		}
		return result;
	}
	
	private Shape toOffsetShape(double xOffset, double yOffset){
		Path2D.Double path = new Path2D.Double();
		
		IBorderPoint first = borderList.get(0);
		path.moveTo(first.getX()+xOffset, first.getY()+yOffset);
		
		for(IBorderPoint b : this.borderList){
			path.lineTo(b.getX()+xOffset, b.getY()+yOffset);
		}
		path.closePath();

		return path;
	}

	/**
	 * Make an offset polygon from the border list of this object
	 * @return
	 */
	private FloatPolygon createOffsetPolygon(float xOffset, float yOffset){
		float[] xpoints = new float[borderList.size()+1];
		float[] ypoints = new float[borderList.size()+1];

		for(int i=0;i<borderList.size();i++){
			IBorderPoint p = borderList.get(i);
			xpoints[i] = (float) p.getX() + xOffset;
			ypoints[i] = (float) p.getY() + yOffset;
		}

		// Ensure the polygon is closed
		xpoints[borderList.size()] = (float) borderList.get(0).getX() + xOffset;
		ypoints[borderList.size()] = (float) borderList.get(0).getY() + yOffset;

		return new FloatPolygon(xpoints, ypoints);
	}
	
	public int wrapIndex(int i){
		return AbstractCellularComponent.wrapIndex(i, this.getBorderLength());
	}
	
	
	 /**
	  * Wrap arrays. If an index falls of the end, it is returned to the start and vice versa
	 * @param i the index
	 * @param length the array length
	 * @return the index within the array
	 */
	public static int wrapIndex(int i, int length){
		 if(i<0){
			 return length + i; // if i = -1, in a 200 length array,  will return 200-1 = 199
		 }
		 
		 if(i<length){ // if not wrapping
			 return i;
		 }
		 
		 return i%length;
	 }
	
	/**
	 * Wrap arrays for doubles. This is used in interpolation. 
	 * If an index falls of the end, it is returned to the start and vice versa
	 * @param i the index
	 * @param length the array length
	 * @return the index within the array
	 */
	public static double wrapIndex(double i, int length){
		 if(i<0){
			 return length + i; // if i = -1, in a 200 length array,  will return 200-1 = 199
		 }
		 
		 if(i<length){ // if not wrapping
			 return i;
		 }
		 
		// i is greater than array length
		 
		 return i % length;

	 }
	
	/**
	 * Create a boolean mask, in which 1 is within the nucleus and 0 is outside
	 * the nucleus, for an image centred on the nuclear centre of mass, of the
	 * given size
	 * @param height
	 * @param width
	 * @return
	 */
	public Mask getBooleanMask(int height, int width){
		
		int halfX = width >> 1;
		int halfY = height >> 1;	
			
		boolean[][] result = new boolean[height][width];
					
		for(int x= -halfX, aX=0; aX<width; x++, aX++ ){

			for(int y=-halfY, aY=0; aY<height; y++, aY++ ){

				result[aY][aX] = this.containsPoint( IPoint.makeNew(x, y) );

			}
			
		}
			
		return new BooleanMask(result);
	}
	
	
	/*
	For two NucleusBorderPoints in a Nucleus, find the point that lies halfway between them
	Used for obtaining a consensus between potential tail positions. Ensure we choose the
	smaller distance
	 */
	public int getPositionBetween(IBorderPoint pointA, IBorderPoint pointB){

		int a = 0;
		int b = 0;
		// find the indices that correspond on the array
		for(int i = 0; i<this.getBorderLength(); i++){
			if(this.getBorderPoint(i).overlaps(pointA)){
				a = i;
			}
			if(this.getBorderPoint(i).overlaps(pointB)){
				b = i;
			}
		}

		// find the higher and lower index of a and b
		int maxIndex = a > b ? a : b;
		int minIndex = a > b ? b : a;

		// there are two midpoints between any points on a ring; we want to take the 
		// midpoint that is in the smaller segment.

		int difference1 = maxIndex - minIndex;
		int difference2 = this.getBorderLength() - difference1;

		// get the midpoint
		int mid1 = this.wrapIndex( (int)Math.floor( (difference1/2)+minIndex ) );
		int mid2 = this.wrapIndex( (int)Math.floor( (difference2/2)+maxIndex ));

		return difference1 < difference2 ? mid1 : mid2;
	}

	public IBorderPoint findOppositeBorder(IBorderPoint p){

		int minDeltaYIndex = 0;
		double minAngle = 180;

		for(int i = 0; i<this.getBorderLength();i++){
			
			double angle = this.getCentreOfMass().findAngle(p, this.getBorderPoint(i));

			if(Math.abs(180 - angle) < minAngle){
				minDeltaYIndex = i;
				minAngle = 180 - angle;
			}
		}
		return this.getBorderPoint(minDeltaYIndex);
	}
	
	public void reverse(){
		
	}

	/*
		From the point given, create a line to the CoM. Measure angles from all 
		other points. Pick the point closest to 90 degrees. Can then get opposite
		point. Defaults to input point if unable to find point.
	*/
	public IBorderPoint findOrthogonalBorderPoint(IBorderPoint a){

		IBorderPoint orthgonalPoint = a;
		double bestAngle = 0;

		for(int i=0;i<this.getBorderLength();i++){

			IBorderPoint p = this.getBorderPoint(i);
			double angle = this.getCentreOfMass().findAngle(a, p); 
			if(Math.abs(90-angle)< Math.abs(90-bestAngle)){
				bestAngle = angle;
				orthgonalPoint = p;
			}
		}
		return orthgonalPoint;
	}
	
	
	
	public IBorderPoint findClosestBorderPoint(IPoint p){
		
		double minDist = Double.MAX_VALUE;
		IBorderPoint result = null;
		
		for(IBorderPoint bp : this.borderList){
			
			if(bp.getLengthTo(p)< minDist){
				minDist = bp.getLengthTo(p);
				result = bp;
			}
		}
		return result;
	}
	

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
//		finest("\tReading abstract cellular component");
		
		// id is final, so cannot be assigned normally. 
		// Reflect around the problem by making the field
		// temporarily assignable
		try {
			Class<?> type = AbstractCellularComponent.class;

	        // use getDeclaredField as the field is non public
	        Field idField = type.getDeclaredField("id");
			idField.setAccessible(true);
			UUID id = (UUID) in.readObject();
			idField.set(this, id );
			idField.setAccessible(false);
			
			
		} catch (NoSuchFieldException e) {
			fine("No field", e);
			return;
		} catch (SecurityException e) {
			fine("Security error", e);
			return;
		} catch (IllegalArgumentException e) {
			fine("Illegal argument", e);
			return;
		} catch (IllegalAccessException e) {
			fine("Illegal access", e);
			return;
		} catch(Exception e){
			fine("Unexpected exception", e);
			return;
		}
		
		// The position has changed from double[] to int[]  since 1.13.3
		Object posArray = in.readObject();
		
		position = new int[4];
		
		if(posArray instanceof double[]){
			double[] array = (double[]) posArray;
			for(int i=0; i<array.length; i++){
				position[i] =  (int) array[i];
			}
			
		} else {
			int[] array = (int[]) posArray;
			for(int i=0; i<array.length; i++){
				position[i] = array[i];
			}
		}
						
		Object o = in.readObject();
				
		boolean newFormat = false;
		
		// The border points are FloatPoints changed from XYPoints since 1.13.3
		if(o instanceof FloatPoint){
			centreOfMass = (IPoint) o;
			newFormat = true;
		} else {
			centreOfMass = new FloatPoint( ((XYPoint) o));
			newFormat = false;
		}
		
		statistics   = (Map<PlottableStatistic, Double>) in.readObject();
		
		Object nextObject = in.readObject();
		
		// The bounding rectangle is no longer stored since 1.13.3
		if(nextObject instanceof Rectangle){
			sourceFolder      = (File) in.readObject();
		} else {
			sourceFolder      = (File) nextObject;
		}

		sourceFileName    = (String) in.readObject();
		channel           = in.readInt();
		scale             = in.readDouble();
		
		List<IBorderPoint> list  = new ArrayList<IBorderPoint>();

		boolean isNextAvailable = in.readBoolean();
    	while (isNextAvailable) {
    		IBorderPoint next = new DefaultBorderPoint(0, 0);
    		
    		if(newFormat){
//    			log("Detected new format border list");
    			next.setX(in.readFloat());
        		next.setY(in.readFloat());
    			
    		} else {
//    			log("Detected old format border list");
    			// The constructor will convert down to float
    			next.setX(in.readDouble());
        		next.setY(in.readDouble());
    		}

    		isNextAvailable = in.readBoolean();
    		
    		list.add(next);
    	}
    	this.setBorderList(list);
    	    	
    	imageRef = new SoftReference<ImageProcessor>(null); 

//		finest("\tRead abstract cellular component");
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
//		finest("\tWriting abstract cellular component");
//		out.defaultWriteObject();
//		finest("\tWrote abstract cellular component");

/*
	    Java serialization keeps a record of every object written to a stream.
	     If the same object is encountered a second time, only a reference to it is
	      written to the stream, and not a second copy of the object; so circular 
	      references aren't the problem here.

	    But serialization is vulnerable to stack overflow
	     for certain kinds of structures; for example, a long linked 
	     list with no special writeObject() methods will be serialized
	      by recursively writing each link. If you've got a 100,000 links, you're
	       going to try to use 100,000 stack frames, and quite likely fail with a 
	       StackOverflowError.

	    It's possible to define a writeObject() method for such a list class that,
	     when the first link is serialized, simply walks the list and serializes each 
	     link iteratively; this will prevent the default recursive mechanism from 
	     being used.
	     
	The fields to serialise here:
	     
	private final UUID id;
	private double[] position;
	private XYPoint centreOfMass;
	private Map<PlottableStatistic, Double> statistics = new HashMap<PlottableStatistic, Double>();
	private Rectangle boundingRectangle;
	private File sourceFolder;
	private String sourceFileName;
	private int channel; // the RGB channel in which the signal was seen
	private double scale = 1; // allow conversion between pixels and SI units. The length of a pixel in microns
	private List<BorderPoint> borderList    = new ArrayList<BorderPoint>(0);
	
*/
		// Use the default methods to write everything until the borderlist
//		finest("\tWriting abstract cellular component");
		out.writeObject(id);
		out.writeObject(position);
		out.writeObject(centreOfMass);
		out.writeObject(statistics);
//		out.writeObject(boundingRectangle);
		out.writeObject(sourceFolder);
		out.writeObject(sourceFileName);
		out.writeInt(channel);
		out.writeDouble(scale);
		
		// Now ensure we don't recurse over the BorderList
		
				
		for(IBorderPoint p : borderList){
			out.writeBoolean(true); // Another point awaits
			out.writeFloat( (float) p.getX());
			out.writeFloat( (float) p.getY());
		}
		out.writeBoolean(false);
//		finest("\tWrote abstract cellular component");	
	}
	
	/*
	 * #############################################
	 * Methods implementing the Rotatable interface
	 * #############################################
	 */
	
	/**
	 * Rotate the nucleus so that the given point is directly 
	 * below the centre of mass
	 * @param bottomPoint
	 */
	public void rotatePointToBottom(IPoint bottomPoint){

		double angleToRotate 	= 0;
		
		// Calculate the current angle between the point and a vertical line
		
		IPoint currentBottom = IPoint.makeNew(getCentreOfMass().getX(), getMinY());
//		String state = "";
		
		double currentAngle = getCentreOfMass().findAngle(currentBottom, bottomPoint);
//		log(this.getNameAndNumber()+": Initial angle - "+currentAngle);
//		log(this.getNameAndNumber()+": Cur - "+currentBottom.toString());
//		log(this.getNameAndNumber()+": CoM - "+getCentreOfMass().toString());
//		log(this.getNameAndNumber()+": New - "+bottomPoint.toString());
		/*
		 * 
		 * The nucleus is currently rotated such that the desired bottom point (D) makes
		 * an angle <currentAngle> against the line between the centre of mass (M) and 
		 * the current bottom point (C). The possible configurations are shown below:
		 * 
		 *    D            D
		 *     \          /
		 *      M        M               M       M
		 *      |        |             / |       | \
		 *      C        C            D  C       C  D
		 *      
		 *      
		 *   Clockwise   Clock        Clock     Clock
		 *   360 - a     a            360-a      a
		 */
		
		// These calculations are perfectly wrong. They result in the bottomPoint anywhere
		// except the bottom. The working values from trial and error are below
				
		if(bottomPoint.isLeftOf(currentBottom)){
	
			angleToRotate = currentAngle - 90; // Tested working
//			state = "Right of CoM";
		}
		
		if(bottomPoint.isRightOf(currentBottom)){
			
			angleToRotate = 360 - currentAngle - 90; // Tested working
//			state = "Right of CoM";
		}
				
//		log(this.getNameAndNumber()+": State - "+state);
//		log(this.getNameAndNumber()+": Rotation angle - "+angleToRotate);
		this.rotate(angleToRotate);
	}
	
	public abstract void alignVertically();

	@Override
	public void rotate(double angle) {
		if(angle!=0){

			for(int i=0; i<getBorderLength(); i++){
				IPoint p = getBorderPoint(i);

				IPoint newPoint = getPositionAfterRotation(p, angle);

				updateBorderPoint(i, newPoint.getX(), newPoint.getY());
			}
		}
				
	}
	
	/**
	 * Get the position of the given point in this object after the object
	 * has been rotated by the given amount
	 * @param p the point to move
	 * @param angle the angle in degrees
	 * @return
	 */
	protected IPoint getPositionAfterRotation(IPoint p, double angle){
		
		// get the distance from the point to the centre of mass
		double distance = p.getLengthTo(this.getCentreOfMass());

		// get the angle between the centre of mass (C), the point (P) and a
		// point directly under the centre of mass (V)

		/*
		 *      C
		 *      |\  
		 *      V P
		 * 
		 */
		double oldAngle = this.getCentreOfMass().findAngle( p,
				IPoint.makeNew(this.getCentreOfMass().getX(),-10));


		if(p.getX()<this.getCentreOfMass().getX()){
			oldAngle = 360-oldAngle;
		}

		double newAngle = oldAngle + angle;
		double newX = new AngleTools().getXComponentOfAngle(distance, newAngle) + this.getCentreOfMass().getX();
		double newY = new AngleTools().getYComponentOfAngle(distance, newAngle) + this.getCentreOfMass().getY();
		return IPoint.makeNew(newX, newY);
	}
	
	/**
	 * Go around the border of the object, measuring the angle to the OP. 
	 * If the angle is closest to target angle, return the distance to the CoM.
	 * @param angle the target angle
	 * @return the distance from the closest border point at the requested angle to the CoM
	 */
	public double getDistanceFromCoMToBorderAtAngle(double angle){

		double bestDiff = 180;
		double bestDistance = 180;

		for(int i=0;i<getBorderLength();i++){
			IPoint p = getBorderPoint(i);
			double distance = p.getLengthTo(getCentreOfMass());
			double pAngle = getCentreOfMass().findAngle( p, IPoint.makeNew(0,-10));
			if(p.getX()<0){
				pAngle = 360-pAngle;
			}

			if(Math.abs(angle-pAngle) < bestDiff){

				bestDiff = Math.abs(angle-pAngle);
				bestDistance = distance;
			}
		}
		return bestDistance;
	}
		
}
