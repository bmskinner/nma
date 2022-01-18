/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.components.cells;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.analysis.ComponentMeasurer;
import com.bmskinner.nuclear_morphology.components.Imageable;
import com.bmskinner.nuclear_morphology.components.Statistical;
import com.bmskinner.nuclear_morphology.components.generic.FloatPoint;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.io.XmlSerializable;
import com.bmskinner.nuclear_morphology.io.xml.XMLReader;
import com.bmskinner.nuclear_morphology.utility.ArrayUtils;

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;

/**
 * An abstract implementation of {@link CellularComponent}, which is extended
 * for concrete cellular components. This handles the border, centre of mass and
 * statistics of a component.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public abstract class DefaultCellularComponent implements CellularComponent {
	
//	private static final String SOURCE_IMAGE_IS_NOT_AVAILABLE = "Source image is not available";

	private static final Logger LOGGER = Logger.getLogger(DefaultCellularComponent.class.getName());
    
    /** The pixel spacing between border points after roi interpolation */
	private static final int INTERPOLATION_INTERVAL_PIXELS = 1;
		
    private final UUID        id;
    
    /** The lowest x position in the object bounding box */
    private final int xBase;
    
    /** The lowest y position in the object bounding box */
    private final int yBase;
    
    /** The width of the object bounding box */
    private final double width;
    
    /** The height of the object bounding box  */
    private final double height;

    /** The current centre of the object. */
    private IPoint centreOfMass;

    /** The original centre of the object in its source image. */
    private final IPoint originalCentreOfMass;

    /** The measurements stored for this object */
    private Map<Measurement, Double> measurements = new HashMap<>();

    /**
     * The source file the component was detected in. This is detected on
     * dataset loading as a relative path from the nmd
     */
    private File sourceFile;

    /** The RGB channel in which this component was detected */
    private final int channel;

    /**
     * The length of a micron in pixels. Allows conversion between pixels and SI
     * units. Set to 1 by default.
     * 
     * @see CellularComponent#setScale()
     */
    private double scale = CellularComponent.DEFAULT_SCALE;

    /** The points within the roi from which the object was detected  */
    private final int[] xpoints; 
    private final int[] ypoints;
    
    private boolean isReversed = false;

    /** The complete border list interpolated from the roi */
    private IPoint[] borderList = new IPoint[0];

    /** Cached object shapes. */
    private ShapeCache shapeCache = new ShapeCache();

    /** The object bounding box */
    private Rectangle2D bounds;

    /**
     * Construct with an ROI, a source image and channel, and the original
     * position in the source image. It sets the immutable original centre of
     * mass, and the mutable current centre of mass. It also assigns a random ID
     * to the component.
     * 
     * @param roi the roi of the object
     * @param centerOfMass the original centre of mass of the component
     * @param source the image file the component was found in
     * @param channel the RGB channel the component was found in
     * @param position the bounding position of the component in the original image
     */
    protected DefaultCellularComponent(@NonNull Roi roi, @NonNull IPoint centreOfMass, File source, int channel, 
    		int x, int y, double w, double h) {
    	this(roi, centreOfMass, source, channel, x, y, w, h, UUID.randomUUID() );
    }
    
    /**
     * Construct with an ROI, a source image and channel, and the original
     * position in the source image. It sets the immutable original centre of
     * mass, and the mutable current centre of mass. It also assigns a random ID
     * to the component.
     * 
     * @param roi the roi of the object
     * @param centerOfMass the original centre of mass of the component
     * @param source the image file the component was found in
     * @param channel the RGB channel the component was found in
     * @param position the bounding position of the component in the original image
     * @param id the id of the component. Only use when deserialising!
     */
    protected DefaultCellularComponent(@NonNull Roi roi, @NonNull IPoint centreOfMass, 
    		File source, int channel, int x, int y, double w, double h, @Nullable UUID id) {
        
        // Sanity check: is the CoM inside the roi
    	if(!doesRoiMatchCom(roi, centreOfMass))
    		throw new IllegalArgumentException("Centre of mass is not inside ROI");
        
        this.originalCentreOfMass = IPoint.makeNew(centreOfMass);
        this.centreOfMass = IPoint.makeNew(centreOfMass);
        this.id = id==null ? UUID.randomUUID() : id;
        this.sourceFile = source;
        this.channel = channel;
        this.xBase = x;
        this.yBase = y;
        this.width = w;
        this.height = h;

        // Store the original points of the roi. From these, a smooth polygon can be
        // reconstructed.
        Polygon polygon = roi.getPolygon();

        // The polygon may have empty indices in its arrays
        // so resize these by copying the values to new
        // arrays
        this.xpoints = Arrays.copyOfRange(polygon.xpoints, 0, polygon.npoints);
        this.ypoints = Arrays.copyOfRange(polygon.ypoints, 0, polygon.npoints);

        makeBorderList();
    }
    
    /**
     * Test if the provided centre of mass is within the
     * the given roi
     * @param roi
     * @param com
     * @return
     */
    private boolean doesRoiMatchCom(Roi roi, IPoint com){
    	Polygon polygon = roi.getPolygon();
        Rectangle2D rect = polygon.getBounds().getFrame();
        
        return rect.contains(com.getX(), com.getY());
    }
    

    /**
     * Defensively duplicate a component. The ID is kept consistent.
     * 
     * @param a the template component
     */
    protected DefaultCellularComponent(@NonNull CellularComponent a) {
        this.id = UUID.fromString(a.getID().toString());
        this.xBase = a.getXBase();
        this.yBase = a.getYBase();
        this.width = a.getWidth();
        this.height = a.getHeight();
        this.originalCentreOfMass = a.getOriginalCentreOfMass().duplicate();
        this.centreOfMass = a.getCentreOfMass().duplicate();
        this.sourceFile = new File(a.getSourceFile().getPath());
        this.channel = a.getChannel();
        this.scale = a.getScale();

        for(Measurement stat : a.getStatistics()) {
        	setStatistic(stat, a.getStatistic(stat, MeasurementScale.PIXELS));
        }

        if ( !(a instanceof DefaultCellularComponent))
        	throw new IllegalArgumentException("Input is incorrect class: "+a.getClass().getName());

        DefaultCellularComponent other = (DefaultCellularComponent) a;

        this.xpoints = Arrays.copyOf(other.xpoints, other.xpoints.length);
        this.ypoints = Arrays.copyOf(other.ypoints, other.ypoints.length);
        makeBorderList();
    }
    
    /**
     * Construct from an XML element. Use for 
     * unmarshalling. The element should conform
     * to the specification in {@link XmlSerializable}.
     * @param e the XML element containing the data.
     */
    protected DefaultCellularComponent(Element e) {
    	id = UUID.fromString(e.getAttributeValue("id"));
    	
    	this.xBase = Integer.parseInt(e.getChildText("XBase"));
        this.yBase = Integer.parseInt(e.getChildText("YBase"));
        this.width = Double.parseDouble(e.getChildText("Width"));
        this.height = Double.parseDouble(e.getChildText("Height"));
    	    	
    	String[] comString = e.getChildText("CentreOfMass").split(",");
    	centreOfMass = IPoint.makeNew(Float.parseFloat(comString[0]), Float.parseFloat(comString[1]));
    	
    	String[] comString2 = e.getChildText("OriginalCentreOfMass").split(",");
    	originalCentreOfMass = IPoint.makeNew(Float.parseFloat(comString2[0]), Float.parseFloat(comString2[1]));

    	// Add measurements
    	for(Element el : e.getChildren("Measurement")) {
    		Measurement m = Measurement.of(el.getAttributeValue("name"));
    		measurements.put(m, Double.parseDouble(el.getText()));
    	}
    	
    	sourceFile = new File(e.getChildText("SourceFile"));
    	channel = Integer.parseInt(e.getChildText("Channel"));
    	scale   = Double.parseDouble(e.getChildText("Scale"));
    	
    	xpoints = XMLReader.parseIntArray(e.getChildText("xpoints"));
    	ypoints = XMLReader.parseIntArray(e.getChildText("ypoints"));

    	makeBorderList();
    }

    /**
     * Create the border list from the stored int[] points. Move the centre of
     * mass to any stored position.
     * 
     * @param roi
     */
    private void makeBorderList() {

    	LOGGER.finest(()->"Creating border list from "+xpoints.length+" integer points");

        // Make a copy of the int[] points otherwise creating a polygon roi
        // will reset them to 0,0 coordinates
        int[] xcopy = Arrays.copyOf(xpoints, xpoints.length);
        int[] ycopy = Arrays.copyOf(ypoints, ypoints.length);
        
        if(isReversed) {
        	xcopy = ArrayUtils.reverse(xcopy);
        	ycopy = ArrayUtils.reverse(ycopy);
        }
        
        PolygonRoi roi = new PolygonRoi(xcopy, ycopy, xcopy.length, Roi.TRACED_ROI);

        // Creating the border list will set everything to the original image
        // position.
        // Move the border list back over the CoM if needed.
        IPoint oldCoM = centreOfMass.duplicate();
        centreOfMass = originalCentreOfMass.duplicate();

        // convert the roi positions to a list of border points
        roi.fitSplineForStraightening(); // this prevents the resulting border differing in length between invokations

        FloatPolygon smoothed = roi.getInterpolatedPolygon(INTERPOLATION_INTERVAL_PIXELS, true);

        LOGGER.finest( "Interpolated integer list to smoothed list of "+smoothed.npoints);
        
        borderList = new IPoint[smoothed.npoints];   
        for (int i = 0; i < smoothed.npoints; i++)
            borderList[i] = new FloatPoint(smoothed.xpoints[i], smoothed.ypoints[i]);

        moveCentreOfMass(oldCoM);
        updateBounds();
    }

    private void updateBounds() {
        double xMax = -Double.MAX_VALUE;
        double xMin = Double.MAX_VALUE;
        double yMax = -Double.MAX_VALUE;
        double yMin = Double.MAX_VALUE;

        for (IPoint p : borderList) {
            xMax = p.getX() > xMax ? p.getX() : xMax;
            xMin = p.getX() < xMin ? p.getX() : xMin;
            yMax = p.getY() > yMax ? p.getY() : yMax;
            yMin = p.getY() < yMin ? p.getY() : yMin;
        }

        double w  = xMax - xMin;
        double h = yMax - yMin;
        
        bounds = new Rectangle2D.Double(xMin, yMin, w, h);
    }

    @Override
    public @NonNull UUID getID() {
        return id;
    }

    @Override
	public int getXBase() {
		return xBase;
	}

	@Override
	public int getYBase() {
		return yBase;
	}

	@Override
	public double getWidth() {
		return bounds.getWidth();
	}

	@Override
	public double getHeight() {
		return bounds.getHeight();
	}

	@Override
    public IPoint getOriginalBase() {
        return IPoint.makeNew(xBase, yBase);
    }
    
    @Override
    public IPoint getBase() {
        return IPoint.makeNew(bounds.getMinX(), bounds.getMinY());
    }

    /**
     * Get the source folder for images
     * 
     * @return
     */
    @Override
    public File getSourceFolder() {
        return sourceFile.getParentFile();
    }
    
    @Override
	public void setSourceFolder(@NonNull File sourceFolder) {
        File newFile = new File(sourceFolder, sourceFile.getName());
        sourceFile = newFile;
    }

    /**
     * Get the absolute path to the source image on the current computer. Merges
     * the dynamic image folder with the image name
     * 
     * @return
     */
    @Override
    public File getSourceFile() {
        return sourceFile;
    }
    
    @Override
   	public void setSourceFile(@NonNull File sourceFile) {
           this.sourceFile = sourceFile;
       }

    @Override
    public String getSourceFileName() {
        return sourceFile.getName();
    }

    @Override
    public String getSourceFileNameWithoutExtension() {

        String trimmed = "";

        int i = getSourceFileName().lastIndexOf('.');
        if (i > 0) {
            trimmed = getSourceFileName().substring(0, i);
        }
        return trimmed;
    }

    @Override
	public int getChannel() {
        return channel;
    }

    @Override
	public double getScale() {
        return this.scale;
    }

    @Override
	public void setScale(double scale) {
        this.scale = scale;
    }

    @Override
	public synchronized boolean hasStatistic( @NonNull final Measurement stat) {
        return this.measurements.containsKey(stat);
    }

    @Override
    public synchronized double getStatistic( @NonNull final Measurement stat) {
        return this.getStatistic(stat, MeasurementScale.PIXELS);
    }

    @Override
    public synchronized double getStatistic(@NonNull final Measurement stat,  @NonNull final MeasurementScale measurementScale) {

        if (this.measurements.containsKey(stat)) {
            double result = measurements.get(stat);
            return stat.convert(result, this.scale, measurementScale);
        }
        
        setStatistic(stat, ComponentMeasurer.calculate(stat, this));
        return stat.convert(measurements.get(stat), this.scale, measurementScale);
    }

    @Override
    public synchronized void setStatistic(@NonNull final Measurement stat, double d) {
        measurements.put(stat, d);
    }
    
    @Override
    public synchronized void clearStatistic(@NonNull final Measurement stat) {
    	measurements.remove(stat);
    }

    @Override
    public List<Measurement> getStatistics() {
    	List<Measurement> result = new ArrayList<>();
    	for(Measurement m : measurements.keySet()) {
    		if(m!=null)
    			result.add(m);
    	}
    	return result;
    }

    @Override
	public void updateDependentStats() {
        for (Measurement stat : measurements.keySet()) {
            if (this.getStatistic(stat) == Statistical.STAT_NOT_CALCULATED)
            	setStatistic(stat, ComponentMeasurer.calculate(stat, this));
        }
    }

    @Override
	public IPoint getCentreOfMass() {
        return centreOfMass;
    }

    @Override
	public IPoint getOriginalCentreOfMass() {
        return IPoint.makeNew(originalCentreOfMass);
    }

    @Override
	public int getBorderLength() {
        return borderList.length;
    }

    @Override
	public IPoint getBorderPoint(int i) {
        return borderList[i];
    }

    @Override
	public IPoint getOriginalBorderPoint(int i) {
        IPoint p = getBorderPoint(i);

        double diffX = p.getX() - centreOfMass.getX();
        double diffY = p.getY() - centreOfMass.getY();
        
        // Offset to the original position
        return IPoint.makeNew(originalCentreOfMass.getX() + diffX,
                originalCentreOfMass.getY() + diffY);
    }

    @Override
	public int getBorderIndex(@NonNull IPoint p) {
        for(int i=0; i<borderList.length; i++) {
        	IPoint n = borderList[i];
        	if(n.overlapsPerfectly(p))
        		return i;
        }
        return -1; // default if no match found
    }

    @Override
	public void updateBorderPoint(int i, @NonNull IPoint p) {
        this.updateBorderPoint(i, p.getX(), p.getY());
    }

    @Override
	public void updateBorderPoint(int i, double x, double y) {
        borderList[i].setX(x);
        borderList[i].setY(y);
    }

    @Override
	public List<IPoint> getBorderList() {
        return List.of(borderList);
    }

    @Override
	public List<IPoint> getOriginalBorderList() {
        List<IPoint> result = new ArrayList<>(borderList.length);

        double diffX = originalCentreOfMass.getX() - centreOfMass.getX();
        double diffY = originalCentreOfMass.getY() - centreOfMass.getY();

        // Offset to the original position
        for (IPoint p : borderList) {
            result.add(IPoint.makeNew(p.getX() + diffX, p.getY() + diffY));
        }
        return result;
    }
    
    
    @Override
	public int[][] getUnsmoothedBorderCoordinates(){
    	return new int[][] { xpoints, ypoints};
    }

    /**
     * Check if a given point lies within the nucleus
     * 
     * @param p
     * @return
     */
    @Override
	public boolean containsPoint(IPoint p) {
        // Fast check - is the point within the bounding rectangle?
        if (!bounds.contains(p.toPoint2D()))
            return false;
        // Check detailed position
        return(this.toPolygon().contains((float) p.getX(), (float) p.getY()));
    }

    /**
     * Check if a given point lies within the nucleus
     * 
     * @param p
     * @return
     */
    @Override
	public boolean containsPoint(int x, int y) {
        return this.toShape().contains(x, y);
    }

    /**
     * Check if a given point in the original source image lies within the
     * nucleus
     * 
     * @param p
     * @return
     */
    @Override
	public boolean containsOriginalPoint(IPoint p) {
        return this.toOriginalPolygon().contains((float) p.getX(), (float) p.getY());
    }

    /**
     * Check if a given point lies within the nucleus
     * 
     * @param p
     * @return
     */
    public boolean containsOriginalPoint(int x, int y) {

        // Fast check - is the point within the bounding rectangle moved to the
        // original position?
        Rectangle r = new Rectangle(xBase, yBase, (int)width, (int)height);
        if (!r.contains(x, y))
            return false;
        return this.toOriginalShape().contains(x, y);
    }

    @Override
	public double getMaxX() {
        return bounds.getMaxX();
    }

    @Override
	public double getMinX() {
        return bounds.getMinX();
    }

    @Override
	public double getMaxY() {
        return bounds.getMaxY();
    }

    @Override
	public double getMinY() {
        return bounds.getMinY();
    }
    
    @Override
	public void flipHorizontal() {
    	flipHorizontal(centreOfMass);
    }

    @Override
	public void flipHorizontal(final @NonNull IPoint p) {

        double xCentre = p.getX();

        for (IPoint n : borderList) {
            double dx = xCentre - n.getX();
            double xNew = xCentre + dx;
            n.setX(xNew);
        }
    }
    
    @Override
	public void flipVertical() {
    	flipVertical(centreOfMass);
    }
    
    @Override
	public void flipVertical(final @NonNull IPoint p) {

        double yCentre = p.getY();

        for (IPoint n : borderList) {
            double dy = yCentre - n.getY();
            double yNew = yCentre + dy;
            n.setY(yNew);
        }
    }

    /**
     * Translate the XY coordinates of each border point so that the nuclear
     * centre of mass is at the given point
     * 
     * @param point the new centre of mass
     */
    @Override
	public void moveCentreOfMass(@NonNull IPoint point) {

        // get the difference between the x and y positions
        // of the points as offsets to apply
        double xOffset = point.getX() - centreOfMass.getX();
        double yOffset = point.getY() - centreOfMass.getY();

        offset(xOffset, yOffset);
    }

    /**
     * Translate the XY coordinates of the object
     * 
     * @param xOffset the amount to move in the x-axis
     * @param yOffset the amount to move in the y-axis
     */
    @Override
	public void offset(double xOffset, double yOffset) {

        /// update each border point
        for (int i = 0; i < borderList.length; i++) {
            IPoint p = borderList[i];
            p.offset(xOffset, yOffset);
        }

        this.centreOfMass.offset(xOffset, yOffset);
        updateBounds();

    }

    @Override
	public void reverse() {
    	LOGGER.fine("Reversing component border list");
        isReversed = !isReversed;
        makeBorderList(); // Recreate the border list from the new key points
        
    }

    /**
     * Turn the list of border points into a closed polygon.
     * 
     * @return
     */
    @Override
	public FloatPolygon toPolygon() {
        return toOffsetPolygon(0, 0);
    }

    /**
     * Turn the list of border points into a polygon. The points are at the
     * original position in a source image.
     * 
     * @see Imageable#getPosition()
     * @return
     */
    @Override
	public FloatPolygon toOriginalPolygon() {

        double diffX = originalCentreOfMass.getX() - centreOfMass.getX();
        double diffY = originalCentreOfMass.getY() - centreOfMass.getY();

        return toOffsetPolygon((float) diffX, (float) diffY);
    }

    /**
     * Make an offset polygon from the border list of this object
     * 
     * @return
     */
    private FloatPolygon toOffsetPolygon(float xOffset, float yOffset) {
        float[] xpoints = new float[borderList.length + 1];
        float[] ypoints = new float[borderList.length + 1];

        for (int i = 0; i < borderList.length; i++) {
            IPoint p = borderList[i];
            xpoints[i] = (float) p.getX() + xOffset;
            ypoints[i] = (float) p.getY() + yOffset;
        }

        // Ensure the polygon is closed
        xpoints[borderList.length] = (float) borderList[0].getX() + xOffset;
        ypoints[borderList.length] = (float) borderList[0].getY() + yOffset;

        return new FloatPolygon(xpoints, ypoints);
    }

    @Override
	public Shape toShape() {
        // Converts whatever coordinates are in the border to a shape
        return toOffsetShape(0, 0);
    }

    @Override
	public Shape toShape(MeasurementScale scale) {

        // Converts whatever coordinates are in the border
        // to a shape
        return toOffsetShape(0, 0, scale);
    }

    @Override
	public Shape toOriginalShape() {

        // Calculate the difference between the original CoM and the new CoM
        // and apply this offset

        double diffX = originalCentreOfMass.getX() - centreOfMass.getX();
        double diffY = originalCentreOfMass.getY() - centreOfMass.getY();

        return toOffsetShape(diffX, diffY);
    }

    @Override
    public Roi toRoi() {
    	Roi r = new PolygonRoi(xpoints, ypoints, xpoints.length, Roi.POLYGON);
    	r.setLocation(0, 0);
    	return r;
    }

    @Override
    public Roi toOriginalRoi() {
        return new PolygonRoi(xpoints, ypoints, xpoints.length, Roi.POLYGON);
    }

    private Shape toOffsetShape(double xOffset, double yOffset) {
        return this.toOffsetShape(xOffset, yOffset, MeasurementScale.PIXELS);
    }

    /**
     * Create a shape from the border of the object with the given translation
     * and at the given scale
     * 
     * @param xOffset
     * @param yOffset
     * @param scale
     * @return
     */
    private Shape toOffsetShape(double xOffset, double yOffset, MeasurementScale scale) {

        if (borderList.length==0)
            throw new IllegalArgumentException("Border list is empty");

        if (shapeCache.has(xOffset, yOffset, scale)) {
            return shapeCache.get(xOffset, yOffset, scale);
        }

        double sc = scale.equals(MeasurementScale.MICRONS) ? this.scale : 1;

        Path2D.Double path = new Path2D.Double();

        IPoint first = borderList[0];
        path.moveTo((first.getX() + xOffset) / sc, (first.getY() + yOffset) / sc);

        for (IPoint b : this.borderList) {
            path.lineTo((b.getX() + xOffset) / sc, (b.getY() + yOffset) / sc);
        }
        path.closePath();

        shapeCache.add(xOffset, yOffset, scale, path);

        return path;

    }

    @Override
    public int wrapIndex(int i) {
        return CellularComponent.wrapIndex(i, this.getBorderLength());
    }
    
    @Override
    public double wrapIndex(double d) {
        return CellularComponent.wrapIndex(d, this.getBorderLength());
    }

    /*
     * For two NucleusBorderPoints in a Nucleus, find the point that lies
     * halfway between them Used for obtaining a consensus between potential
     * tail positions. Ensure we choose the smaller distance
     */
    @Override
	public int getPositionBetween(@NonNull IPoint pointA, @NonNull IPoint pointB) {

        int a = 0;
        int b = 0;
        // find the indices that correspond on the array
        for (int i = 0; i < this.getBorderLength(); i++) {
            if (this.getBorderPoint(i).overlaps(pointA)) {
                a = i;
            }
            if (this.getBorderPoint(i).overlaps(pointB)) {
                b = i;
            }
        }

        // find the higher and lower index of a and b
        int maxIndex = a > b ? a : b;
        int minIndex = a > b ? b : a;

        // there are two midpoints between any points on a ring; we want to take
        // the
        // midpoint that is in the smaller segment.

        int difference1 = maxIndex - minIndex;
        int difference2 = this.getBorderLength() - difference1;

        // get the midpoint
        int mid1 = this.wrapIndex((int) Math.floor(((double)difference1 / 2) + minIndex));
        int mid2 = this.wrapIndex((int) Math.floor(((double)difference2 / 2) + maxIndex));

        return difference1 < difference2 ? mid1 : mid2;
    }

    @Override
	public IPoint findOppositeBorder(@NonNull IPoint p) {
        // Find the point that is closest to 180 degrees across the CoM
    	double distToCom = p.getLengthTo(centreOfMass);
    	
    	// Checking the angle of every point via atan2 is expensive.
    	// We can filter out most of the points beforehand.
    	return Arrays.stream(borderList)
    			.filter(point->point.getLengthTo(p)>distToCom)
    			.min(Comparator.comparing(point->180-centreOfMass.findSmallestAngle(p, point) ))
    			.orElse(null); // TODO: backup solution?
    }

    @Override
    public IPoint findOrthogonalBorderPoint(@NonNull IPoint a) {
        return Arrays.stream(borderList)
                .min(Comparator.comparing(point-> Math.abs(90-centreOfMass.findSmallestAngle(a, point)) ))
                .get();
    }

    @Override
    public IPoint findClosestBorderPoint(@NonNull IPoint p) {
        return Arrays.stream(borderList)
                .min(Comparator.comparing(point->point.getLengthTo(p) ))
                .get();
    }
    
    @Override
    public String toString() {
    	 StringBuilder builder = new StringBuilder("\nID: "+id.toString()+"\n");
    	 builder.append(String.format("Bounds: x: %s-%s y: %s-%s", this.getBase().getX(), this.getBase().getX()+
    			 this.getWidth(), this.getBase().getY(), this.getBase().getY()+this.getHeight()));
    	 builder.append("\nMeasurements:\n");
    	 for(Entry<Measurement, Double> entry : measurements.entrySet()) {
    		 builder.append(entry.getKey().toString()+": "+entry.getValue().toString()+"\n");
     	}
    	
    	return builder.toString();
    }
    
    @Override
	public Element toXmlElement() {
    	Element e = new Element("Component").setAttribute("id", id.toString());
    	
    	e.addContent(new Element("XBase").setText(String.valueOf(xBase)));
    	e.addContent(new Element("YBase").setText(String.valueOf(yBase)));
    	e.addContent(new Element("Width").setText(String.valueOf(width)));
    	e.addContent(new Element("Height").setText(String.valueOf(height)));
    	    	
    	e.addContent(new Element("CentreOfMass").setText(centreOfMass.getX()+","+centreOfMass.getY()));
    	e.addContent(new Element("OriginalCentreOfMass").setText(originalCentreOfMass.getX()+","+originalCentreOfMass.getY()));
    	
    	for(Entry<Measurement, Double> entry : measurements.entrySet()) {
    		e.addContent(new Element("Measurement")
    				.setAttribute("name", entry.getKey().toString())
    				.setText(entry.getValue().toString()));
    	}
    	
    	e.addContent(new Element("SourceFile").setText(sourceFile.toString()));
    	e.addContent(new Element("Channel").setText(String.valueOf(channel)));
    	e.addContent(new Element("Scale").setText(String.valueOf(scale)));
    	
    	e.addContent(new Element("xpoints").setText(Arrays.toString(xpoints)));
    	e.addContent(new Element("ypoints").setText(Arrays.toString(ypoints)));
    	
    	return e;
	}
        
    
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(xpoints);
		result = prime * result + Arrays.hashCode(ypoints);
		
		// We use this rather than hashing the file directly in case 
		// the file path does not exist
		int fileHash = 1;
		try {
			fileHash = Objects.hash(sourceFile.getCanonicalFile());
		} catch (IOException e) {
			LOGGER.fine("Unable to get source file hash");
		}
		
		result = prime * result + fileHash;
		
		result = prime * result
				+ Objects.hash(centreOfMass, 
						channel, id, originalCentreOfMass, 
						scale, measurements, xBase, yBase, width, height);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefaultCellularComponent other = (DefaultCellularComponent) obj;
		
		// We use this rather than File.equals in case the file
		// does not exist (which would return false)
		boolean isSameFile = true;
		try {
			isSameFile = sourceFile.getCanonicalFile().equals(other.sourceFile.getCanonicalFile());
		} catch (IOException e) {
			LOGGER.fine("Unable to compare source files");
		}
		
		
		return Objects.equals(centreOfMass, other.centreOfMass) 
				&& isSameFile
				&& channel == other.channel
				&& Objects.equals(id, other.id) 
				&& Objects.equals(originalCentreOfMass, other.originalCentreOfMass)
				&& Objects.equals(xBase, other.xBase)
				&& Objects.equals(yBase, other.yBase)
				&& Objects.equals(width, other.width)
				&& Objects.equals(height, other.height)
				&& Double.doubleToLongBits(scale) == Double.doubleToLongBits(other.scale)
				&& Objects.equals(measurements, other.measurements)
				&& Arrays.equals(xpoints, other.xpoints) 
				&& Arrays.equals(ypoints, other.ypoints);
	}

    /*
     * ############################################# 
     * Methods implementing the Rotatable interface 
     * #############################################
     */


    @Override
	public void rotatePointToBottom(@NonNull IPoint bottomPoint) {
        this.alignPointsOnVertical(centreOfMass, bottomPoint);
    }
    
    @Override
   	public void rotatePointToLeft(IPoint leftPoint) {
           this.alignPointsOnHorizontal(leftPoint, centreOfMass);
       }
    
    
    
    @Override
    public void rotate(double angle) {
        if (angle != 0) {
        	double rad = Math.toRadians(-angle); 
        	AffineTransform tf = AffineTransform.getRotateInstance(rad, centreOfMass.getX(), centreOfMass.getY());
            for (IPoint p : borderList) {
            	Point2D result = tf.transform(p.toPoint2D(), null);
                p.set(result);
            }
        }
        updateBounds();
        shapeCache.clear();
    }

    /**
     * Cache the shapes of the object in various positions
     * 
     * @author bms41
     * @since 1.13.4
     *
     */
    private class ShapeCache {

        public class Key {
            final double           xOffset;
            final double           yOffset;
            final MeasurementScale scale;

            public Key(final double x, final double y, final MeasurementScale s) {
                xOffset = x;
                yOffset = y;
                scale = s;
            }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + getOuterType().hashCode();
                result = prime * result + ((scale == null) ? 0 : scale.hashCode());
                long temp;
                temp = Double.doubleToLongBits(xOffset);
                result = prime * result + (int) (temp ^ (temp >>> 32));
                temp = Double.doubleToLongBits(yOffset);
                result = prime * result + (int) (temp ^ (temp >>> 32));
                return result;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj)
                    return true;
                if (obj == null)
                    return false;
                if (getClass() != obj.getClass())
                    return false;
                Key other = (Key) obj;
                if (!getOuterType().equals(other.getOuterType()))
                    return false;
                if (scale != other.scale)
                    return false;
                if (Double.doubleToLongBits(xOffset) != Double.doubleToLongBits(other.xOffset))
                    return false;
                if (Double.doubleToLongBits(yOffset) != Double.doubleToLongBits(other.yOffset))
                    return false;
                return true;
            }

            private ShapeCache getOuterType() {
                return ShapeCache.this;
            }

        }

        private Map<Key, Shape> cache = new HashMap<>();

        public ShapeCache() {
        }

        public void add(double x, double y, MeasurementScale s, Shape shape) {
            Key key = new Key(x, y, s);
            cache.put(key, shape);
        }

        public Shape get(double x, double y, MeasurementScale s) {
            Key key = new Key(x, y, s);
            return cache.get(key);
        }

        public boolean has(double x, double y, MeasurementScale s) {
            Key key = new Key(x, y, s);
            return cache.containsKey(key);
        }

        public void clear() {
            cache.clear();
        }
    }
}
