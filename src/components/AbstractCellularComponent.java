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
package components;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.ui.Layer;

import logging.Loggable;
import components.generic.MeasurementScale;
import components.generic.XYPoint;
import components.nuclear.BorderPoint;
import components.nuclei.Nucleus;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import io.ImageExporter;
import io.ImageImporter;
import stats.PlottableStatistic;
import stats.Stats;
import utility.Constants;
import utility.Utils;

public class AbstractCellularComponent implements CellularComponent, Serializable, Loggable {

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
	
	private double scale = 1; // allow conversion between pixels and SI units. The length of a micron in pixels
	
	// The points around the border of the object
	private List<BorderPoint> borderList    = new ArrayList<BorderPoint>(0);
	
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
	public AbstractCellularComponent(Roi roi, File f, int channel, double[] position){
		this(roi, f, channel);
		this.position = position;
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
		borderList.get(0).setPrevPoint(borderList.get(borderList.size()-1));
		
		this.boundingRectangle = new Rectangle(polygon.getBounds());
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
	
	public ImageProcessor getImage(){
		
		ImageProcessor ip = imageRef.get();
		if(ip !=null){
			return ip;
		}
		

		if(getSourceFile().exists()){
			
			// Get the stack, make greyscale and invert
			int stack = Constants.rgbToStack(getChannel());
						
			ImageStack imageStack = ImageImporter.getInstance().importImage(getSourceFile());
			ip = ImageExporter.getInstance().makeGreyRGBImage(imageStack, stack).getProcessor();
			ip.invert();	
			
			imageRef = new SoftReference<ImageProcessor>(ip);
			
			return ip;
		} else {
			return null;
		}
	}
	

	public ImageProcessor getComponentImage(){
		ImageProcessor ip = getImage();

		if(ip==null){	
			return null;	
		}
		
		double[] positions = getPosition();
		
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
	public double getStatistic(PlottableStatistic stat, MeasurementScale scale) {
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
	
	@Override
	public double getSafeStatistic(PlottableStatistic stat, MeasurementScale scale){
		try {
			return getStatistic(stat, scale);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	// For subclasses to override
	protected double calculateStatistic(PlottableStatistic stat){
		return 0;
	}
	
	@Override
	public double getStatistic(PlottableStatistic stat) {
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
	

	public int getBorderLength(){
		return this.borderList.size();
	}


	public BorderPoint getBorderPoint(int i){
		return this.borderList.get(i);
	}
	
	public BorderPoint getOriginalBorderPoint(int i){
//		return this.borderList.get(i);
		BorderPoint p = getBorderPoint(i);
		return new BorderPoint( p.getX() + getPosition()[X_BASE], p.getY() + getPosition()[Y_BASE]);
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
		list.get(0).setPrevPoint(list.get(list.size()-1));
		this.borderList = list;
	}
	
	/**
	 * Check if a given point lies within the nucleus
	 * @param p
	 * @return
	 */
	public boolean containsPoint(XYPoint p){
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
	public boolean containsOriginalPoint(XYPoint p){
		if(this.createOriginalPolygon().contains( (float)p.getX(), (float)p.getY() ) ){
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
			BorderPoint next = this.getBorderPoint( wrapIndex(i+1, this.borderList.size()));
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
		
		// Update the bounding rectangle
		this.boundingRectangle = this.createPolygon().getBounds();
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
	
	/**
	 * Turn the list of border points into a polygon. The points are at the original
	 * position in a source image.
	 * @see Nucleus.getPosition
	 * @return
	 */
	public FloatPolygon createOriginalPolygon(){

		return createOffsetPolygon(  (float) position[CellularComponent.X_BASE],
				                     (float) position[CellularComponent.Y_BASE]);
	}

	/**
	 * Turn the list of border points into a closed polygon. 
	 * @return
	 */
	public FloatPolygon createPolygon(){
		return createOffsetPolygon(0, 0);
	}

	/**
	 * Make an offset polygon from 
	 * @return
	 */
	private FloatPolygon createOffsetPolygon(float xOffset, float yOffset){
		float[] xpoints = new float[borderList.size()+1];
		float[] ypoints = new float[borderList.size()+1];

		for(int i=0;i<borderList.size();i++){
			BorderPoint p = borderList.get(i);
			xpoints[i] = (float) p.getX() + xOffset;
			ypoints[i] = (float) p.getY() + yOffset;
		}

		// Ensure the polygon is closed
		xpoints[borderList.size()] = (float) borderList.get(0).getX() + xOffset;
		ypoints[borderList.size()] = (float) borderList.get(0).getY() + yOffset;

		return new FloatPolygon(xpoints, ypoints);
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
		 
//		 BigDecimal x = new BigDecimal( i );
//		 BigDecimal bdVal = x.subtract( x.remainder( new BigDecimal( length ) ) );
//		 
//		 return bdVal.doubleValue();
		 
		 
//		 if(Math.floor(i / length)>0) // if i is greater than array length 
//			 i = i - (  (      (int)Math.floor(i / length) )    * length  );
//
//		 return i;
	 }
	
	/**
	 * Create a boolean mask, in which 1 is within the nucleus and 0 is outside
	 * the nucleus, for an image centred on the nuclear centre of mass, of the
	 * given size
	 * @param height
	 * @param width
	 * @return
	 */
	public boolean[][] getBooleanMask(int height, int width){
		
		int halfX = width >> 1;
		int halfY = height >> 1;	
			
		boolean[][] result = new boolean[height][width];
					
		for(int x= -halfX, aX=0; aX<width; x++, aX++ ){

			for(int y=-halfY, aY=0; aY<height; y++, aY++ ){

				result[aY][aX] = this.containsPoint( new XYPoint(x, y) );

			}
			
		}
			
		return result;
	}
	
	
	/*
	For two NucleusBorderPoints in a Nucleus, find the point that lies halfway between them
	Used for obtaining a consensus between potential tail positions. Ensure we choose the
	smaller distance
	 */
	public int getPositionBetween(BorderPoint pointA, BorderPoint pointB){

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
		int mid1 = AbstractCellularComponent.wrapIndex( (int)Math.floor( (difference1/2)+minIndex ),
				this.getBorderLength() );

		int mid2 = AbstractCellularComponent.wrapIndex( (int)Math.floor( (difference2/2)+maxIndex ), 
				this.getBorderLength() );

		return difference1 < difference2 ? mid1 : mid2;
	}

	public BorderPoint findOppositeBorder(BorderPoint p){

		int minDeltaYIndex = 0;
		double minAngle = 180;

		for(int i = 0; i<this.getBorderLength();i++){
			double angle = Utils.findAngleBetweenXYPoints(p, this.getCentreOfMass(), this.getBorderPoint(i));
			if(Math.abs(180 - angle) < minAngle){
				minDeltaYIndex = i;
				minAngle = 180 - angle;
			}
		}
		return this.getBorderPoint(minDeltaYIndex);
	}

	/*
		From the point given, create a line to the CoM. Measure angles from all 
		other points. Pick the point closest to 90 degrees. Can then get opposite
		point. Defaults to input point if unable to find point.
	*/
	public BorderPoint findOrthogonalBorderPoint(BorderPoint a){

		BorderPoint orthgonalPoint = a;
		double bestAngle = 0;

		for(int i=0;i<this.getBorderLength();i++){

			BorderPoint p = this.getBorderPoint(i);
			double angle = Utils.findAngleBetweenXYPoints(a, this.getCentreOfMass(), p); 
			if(Math.abs(90-angle)< Math.abs(90-bestAngle)){
				bestAngle = angle;
				orthgonalPoint = p;
			}
		}
		return orthgonalPoint;
	}
	

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		finest("\tReading abstract cellular component");
		
		// id is final, so cannot be assigned normally. 
		// Reflect around the problem by making the field
		// temporarily assignable
		try {
			Class type = AbstractCellularComponent.class;

	        // use getDeclaredField as the field is non public
	        Field idField = type.getDeclaredField("id");
			idField.setAccessible(true);
			UUID id = (UUID) in.readObject();
			idField.set(this, id );
			idField.setAccessible(false);
			
			
		} catch (NoSuchFieldException e) {
			error("No field", e);
		} catch (SecurityException e) {
			error("Security error", e);
		} catch (IllegalArgumentException e) {
			error("Illegal argument", e);
		} catch (IllegalAccessException e) {
			error("Illegal access", e);
		} catch(Exception e){
			error("Unexpected exception", e);
		}

		finest("\tSet final id field");
		
		// Read the standard fields
		
		position     = (double[]) in.readObject();
		centreOfMass = (XYPoint) in.readObject();
		statistics   = (Map<PlottableStatistic, Double>) in.readObject();
		boundingRectangle = (Rectangle) in.readObject();
		sourceFolder      = (File) in.readObject();
		sourceFileName    = (String) in.readObject();
		channel           = in.readInt();
		scale             = in.readDouble();
		
		List<BorderPoint> list  = new ArrayList<BorderPoint>();

		boolean isNextAvailable = in.readBoolean();
		
    	while (isNextAvailable) {
    		BorderPoint next = new BorderPoint(0, 0);
    		
    		next.setX(in.readDouble());
    		next.setY(in.readDouble());
    		isNextAvailable = in.readBoolean();
    		
    		list.add(next);
    	}
    	this.setBorderList(list);
    	
    	imageRef = new SoftReference<ImageProcessor>(null); 

		finest("\tRead abstract cellular component");
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
		finest("\tWriting abstract cellular component");
		out.writeObject(id);
		out.writeObject(position);
		out.writeObject(centreOfMass);
		out.writeObject(statistics);
		out.writeObject(boundingRectangle);
		out.writeObject(sourceFolder);
		out.writeObject(sourceFileName);
		out.writeInt(channel);
		out.writeDouble(scale);
		
		// Now ensure we don't recurse over the BorderList
		
				
		for(BorderPoint p : borderList){
			out.writeBoolean(true); // Another point awaits
			out.writeDouble(p.getX());
			out.writeDouble(p.getY());
		}
		out.writeBoolean(false);
		finest("\tWrote abstract cellular component");	
	}
	
}
