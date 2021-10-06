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
import java.io.ObjectInputStream;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
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

import com.bmskinner.nuclear_morphology.analysis.detection.BooleanMask;
import com.bmskinner.nuclear_morphology.analysis.detection.Mask;
import com.bmskinner.nuclear_morphology.analysis.image.ImageConverter;
import com.bmskinner.nuclear_morphology.components.Imageable;
import com.bmskinner.nuclear_morphology.components.Statistical;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;
import com.bmskinner.nuclear_morphology.io.XmlSerializable;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;

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
	
	private static final String SOURCE_IMAGE_IS_NOT_AVAILABLE = "Source image is not available";

	private static final Logger LOGGER = Logger.getLogger(DefaultCellularComponent.class.getName());
    
    /** The pixel spacing between border points after roi interpolation */
	private static final int INTERPOLATION_INTERVAL_PIXELS = 1;
	
	/** The default pixel scale */
	private static final double DEFAULT_SCALE = 1;
	
	private static final long serialVersionUID = 1L;
    private final UUID        id;

    /**
     * The original position in the source image of the component. Values are
     * stored at the indexes in {@link Imageable.X_BASE},
     * {@link Imageable.Y_BASE}, {@link Imageable.WIDTH} and
     * {@link Imageable.HEIGHT}
     * 
     * @see CellularComponent#getPosition()
     */
    private final int[] position;

    /** The current centre of the object. */
    private IPoint centreOfMass;

    /** The original centre of the object in its source image. */
    private final IPoint originalCentreOfMass;

    /**
     * The statistical values stored for this object
     */
    private Map<Measurement, Double> statistics = new HashMap<>();

    /**
     * The source file the component was detected in. This is detected on
     * dataset loading as a relative path from the .nmd e.g.
     * C:\MyImageFolder\MyImage.tiff
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
    private double scale = DEFAULT_SCALE;

    /** The points within the Roi from which the object was detected  */
    private int[] xpoints; 
    private int[] ypoints;
    
    

    /*
     * TRANSIENT FILEDS
     */

	/**
     * The complete border list, offset to an appropriate position for the
     * object
     */
    private transient List<IPoint> borderList = new ArrayList<>();

    /** Cache images while memory is available. */
    private transient WeakReference <ImageProcessor> imageRef = new WeakReference <>(null);

    /** Cached object shapes. */
    private transient ShapeCache shapeCache = new ShapeCache();

    private transient Rectangle2D bounds;

    
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
    protected DefaultCellularComponent(@NonNull Roi roi, @NonNull IPoint centreOfMass, File source, int channel, int[] position) {
    	this(roi, centreOfMass, source, channel, position, UUID.randomUUID() );
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
    		File source, int channel, int[] position, @Nullable UUID id) {

        this.originalCentreOfMass = IPoint.makeNew(centreOfMass);
        this.centreOfMass = IPoint.makeNew(centreOfMass);
        this.id = id==null ? UUID.randomUUID() : id;
        this.sourceFile = source;
        this.channel = channel;
        this.position = position;

        // Store the original points. From these, the smooth polygon can be
        // reconstructed.
        double epsilon = 1;
        Polygon polygon = roi.getPolygon();
        Rectangle2D bounds = polygon.getBounds().getFrame();

        // // since small signals can have imprecision on the CoM that puts them
        // on the border of the
        // // object, add a small border to consider OK

        double minX = bounds.getX();
        double maxX = minX + bounds.getWidth();

        minX -= epsilon;
        maxX += epsilon;

        if (centreOfMass.getX() < minX || centreOfMass.getX() > maxX) {
            throw new IllegalArgumentException(String.format("The centre of mass X (%d)"
                    + " must be within the roi x bounds (x = %d-%d)", centreOfMass.getX(), minX, maxX));
        }

        double minY = bounds.getY();
        double maxY = minY + bounds.getHeight();
        minY -= epsilon;
        maxY += epsilon;

        if (centreOfMass.getY() < minY || centreOfMass.getY() > maxY) {
            throw new IllegalArgumentException("The centre of mass Y (" + centreOfMass.getY() + ")"
                    + ") must be within the roi bounds (y = " + minY + "-" + maxY + ")");
        }

        if (!polygon.contains(centreOfMass.getX(), centreOfMass.getY())) {
            LOGGER.fine("Centre of mass is not inside the object. You may have a doughnut.");
        }

        this.xpoints = new int[polygon.npoints];
        this.ypoints = new int[polygon.npoints];

        // Discard empty indices left in polygon array
        for (int i = 0; i < polygon.npoints; i++) {
            this.xpoints[i] = polygon.xpoints[i];
            this.ypoints[i] = polygon.ypoints[i];
        }

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
    	
    	String[] posString = e.getChildText("Position")
    			.replace("[", "")
    			.replace("]", "")
    			.replace(" ", "")
    			.split(",");
    	position = new int[posString.length];
    	for(int i=0; i<posString.length; i++)
    		position[i] = Integer.parseInt(posString[i]);
    	
    	String[] comString = e.getChildText("CentreOfMass").split(",");
    	centreOfMass = IPoint.makeNew(Float.parseFloat(comString[0]), Float.parseFloat(comString[1]));
    	
    	String[] comString2 = e.getChildText("OriginalCentreOfMass").split(",");
    	originalCentreOfMass = IPoint.makeNew(Float.parseFloat(comString2[0]), Float.parseFloat(comString2[1]));

    	// Add measurements
    	for(Element el : e.getChildren("Measurement")) {
    		Measurement m = Measurement.of(el.getAttributeValue("name"));
    		statistics.put(m, Double.parseDouble(el.getText()));
    	}
    	
    	sourceFile = new File(e.getChildText("SourceFile"));
    	channel = Integer.parseInt(e.getChildText("Channel"));
    	scale   = Double.parseDouble(e.getChildText("Scale"));
    	
    	String[] xp = e.getChildText("xpoints").replace("[", "").replace("]", "").replace(" ", "").split(",");
    	String[] yp = e.getChildText("ypoints").replace("[", "").replace("]", "").replace(" ", "").split(",");

    	xpoints = new int[xp.length];
    	ypoints = new int[xp.length];
    	for(int i=0; i<xp.length; i++) {
    		xpoints[i] = Integer.parseInt(xp[i]); 
    		ypoints[i] = Integer.parseInt(yp[i]); 
    	}
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
        PolygonRoi roi = new PolygonRoi(xcopy, ycopy, xpoints.length, Roi.TRACED_ROI);

        // Creating the border list will set everything to the original image
        // position.
        // Move the border list back over the CoM if needed.
        IPoint oldCoM = centreOfMass.duplicate();
        centreOfMass = originalCentreOfMass.duplicate();

        borderList = new ArrayList<>();

        // convert the roi positions to a list of border points
        boolean isSmooth = isSmoothByDefault();
        roi.fitSplineForStraightening(); // this prevents the resulting border differing in length between invokations

        FloatPolygon smoothed = roi.getInterpolatedPolygon(1, isSmooth);

        LOGGER.finest( "Interpolated integer list to smoothed list of "+smoothed.npoints);
        for (int i = 0; i < smoothed.npoints; i++) {
            IPoint point = IPoint.makeNew(smoothed.xpoints[i], smoothed.ypoints[i]);
            borderList.add(point);
        }
        moveCentreOfMass(oldCoM);
        calculateBounds();
    }

    private void calculateBounds() {
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

        double w = xMax - xMin;
        double h = yMax - yMin;

        // Constructor for rectangle: upper-left point plus w and h
        // Note that the coordinates have y increasing downwards, so the upper
        // point is yMin
        bounds = new Rectangle2D.Double(xMin, yMin, w, h);

    }

    /**
     * Defensively dDuplicate a component. The ID is kept consistent.
     * 
     * @param a the template component
     */
    protected DefaultCellularComponent(CellularComponent a) {
    	LOGGER.finest( "Constructing a new component from existing template component");
        this.id = UUID.fromString(a.getID().toString());
        this.position = Arrays.copyOf(a.getPosition(), a.getPosition().length);
        this.originalCentreOfMass = IPoint.makeNew(a.getOriginalCentreOfMass());
        this.centreOfMass = IPoint.makeNew(a.getCentreOfMass());
        this.sourceFile = new File(a.getSourceFile().getPath());
        this.channel = a.getChannel();
        this.scale = a.getScale();

        for (Measurement stat : a.getStatistics()) {
            try {
                this.setStatistic(stat, a.getStatistic(stat, MeasurementScale.PIXELS));
            } catch (Exception e) {
                LOGGER.log(Loggable.STACK, "Error getting " + stat + " from template", e);
                this.setStatistic(stat, Statistical.ERROR_CALCULATING_STAT);
            }
        }

        if (a instanceof DefaultCellularComponent) {

            DefaultCellularComponent comp = (DefaultCellularComponent) a;

            this.xpoints = Arrays.copyOfRange(comp.xpoints, 0, comp.xpoints.length);
            this.ypoints = Arrays.copyOf(comp.ypoints, comp.ypoints.length);
            makeBorderList();

        } else {
            duplicateBorderList(a);
        }
    }

    private void duplicateBorderList(CellularComponent c) {
        // Duplicate the border points
    	LOGGER.finest( "Duplicating border list from template component");
        this.borderList = new ArrayList<>(c.getBorderLength());

        for (IPoint p : c.getBorderList()) {
            borderList.add(IPoint.makeNew(p));
        }
    }

    @Override
    public boolean isSmoothByDefault() {
        return true;
    }

    @Override
    public @NonNull UUID getID() {
        return this.id;
    }

    @Override
    public int[] getPosition() {
        return this.position;
    }

    @Override
    public IPoint getOriginalBase() {
        return IPoint.makeNew(position[X_BASE], position[Y_BASE]);
    }
    
    @Override
    public IPoint getBase() {
        return IPoint.makeNew(bounds.getMinX(), bounds.getMinY());
    }

    @Override
    public Rectangle2D getBounds() {
        return bounds;
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
	public ImageProcessor getImage() throws UnloadableImageException {
        ImageProcessor ip = imageRef.get();
        if (ip != null)
            return ip;
        if (!getSourceFile().exists())
        	throw new UnloadableImageException("Source image is not available: "+getSourceFile().getAbsolutePath());

        try {
        	ip = new ImageImporter(getSourceFile()).importImage(getChannel());
        	ip = new ImageConverter(ip).convertToRGBGreyscale().invert().toProcessor();

        	imageRef = new WeakReference<>(ip);
        	return ip;
        } catch (ImageImportException e) {
        	LOGGER.log(Loggable.STACK, "Error importing source image " + this.getSourceFile().getAbsolutePath(), e);
        	throw new UnloadableImageException(SOURCE_IMAGE_IS_NOT_AVAILABLE);
        }
    }
    
    @Override
    public ImageProcessor getGreyscaleImage() throws UnloadableImageException {

        if (!getSourceFile().exists())
            throw new UnloadableImageException("Source image is not available: "+getSourceFile().getAbsolutePath());

        // Get the stack, make greyscale and invert
        int stack = ImageImporter.rgbToStack(getChannel());

        try {
            ImageStack imageStack = new ImageImporter(getSourceFile()).importToStack();
            return imageStack.getProcessor(stack);
        } catch (ImageImportException e) {
            LOGGER.log(Loggable.STACK, "Error importing source image " + this.getSourceFile().getAbsolutePath(), e);
            throw new UnloadableImageException(SOURCE_IMAGE_IS_NOT_AVAILABLE);
        }

    }

    @Override
    public ImageProcessor getRGBImage() throws UnloadableImageException {
        if (!getSourceFile().exists())
            throw new UnloadableImageException("Source image is not available: "+getSourceFile().getAbsolutePath());
        return new ImageImporter(getSourceFile()).importToColorProcessor();
    }

    @Override
	public ImageProcessor getComponentImage() throws UnloadableImageException {
        ImageProcessor ip = getImage().duplicate();

        if (ip == null) {
            throw new UnloadableImageException(SOURCE_IMAGE_IS_NOT_AVAILABLE);
        }

        int[] positions = getPosition();

        int padding = Imageable.COMPONENT_BUFFER; // a border of pixels
                                                          // beyond the cell
                                                          // boundary
        int wideW = positions[Imageable.WIDTH] + (padding * 2);
        int wideH = positions[Imageable.HEIGHT] + (padding * 2);
        int wideX = positions[Imageable.X_BASE] - padding;
        int wideY = positions[Imageable.Y_BASE] - padding;

        wideX = wideX < 0 ? 0 : wideX;
        wideY = wideY < 0 ? 0 : wideY;

        ip.setRoi(wideX, wideY, wideW, wideH);
        ip = ip.crop();

        return ip;
    }
    
    @Override
    public ImageProcessor getGreyscaleComponentImage() throws UnloadableImageException {
    	ImageProcessor ip = getGreyscaleImage().duplicate();
    	if (ip == null) {
            throw new UnloadableImageException(SOURCE_IMAGE_IS_NOT_AVAILABLE);
        }

        int[] positions = getPosition();

        int padding = Imageable.COMPONENT_BUFFER; // a border of pixels
                                                          // beyond the cell
                                                          // boundary
        int wideW = positions[Imageable.WIDTH] + (padding * 2);
        int wideH = positions[Imageable.HEIGHT] + (padding * 2);
        int wideX = positions[Imageable.X_BASE] - padding;
        int wideY = positions[Imageable.Y_BASE] - padding;

        wideX = wideX < 0 ? 0 : wideX;
        wideY = wideY < 0 ? 0 : wideY;

        ip.setRoi(wideX, wideY, wideW, wideH);
        ip = ip.crop();

        return ip;
    }

    @Override
	public ImageProcessor getComponentRGBImage() throws UnloadableImageException {
        ImageProcessor ip = getRGBImage().duplicate();

        if (ip == null) {
            throw new UnloadableImageException(SOURCE_IMAGE_IS_NOT_AVAILABLE);
        }

        int[] positions = getPosition();

        int padding = Imageable.COMPONENT_BUFFER; // a border of pixels
                                                          // beyond the cell
                                                          // boundary
        int wideW = (positions[Imageable.WIDTH] + (padding * 2));
        int wideH = (positions[Imageable.HEIGHT] + (padding * 2));
        int wideX = (positions[Imageable.X_BASE] - padding);
        int wideY = (positions[Imageable.Y_BASE] - padding);

        wideX = wideX < 0 ? 0 : wideX;
        wideY = wideY < 0 ? 0 : wideY;

        ip.setRoi(wideX, wideY, wideW, wideH);
        ip = ip.crop();

        return ip;
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
	public synchronized boolean hasStatistic(final Measurement stat) {
        return this.statistics.containsKey(stat);
    }

    @Override
    public synchronized double getStatistic(final Measurement stat) {
        return this.getStatistic(stat, MeasurementScale.PIXELS);
    }

    @Override
    public synchronized double getStatistic(final Measurement stat, final MeasurementScale measurementScale) {

        if (this.statistics.containsKey(stat)) {
            double result = statistics.get(stat);
            return stat.convert(result, this.scale, measurementScale);
        }
        
        calculateStatistic(stat);
        return stat.convert(statistics.get(stat), this.scale, measurementScale);
    }

    protected void calculateStatistic(final Measurement stat) {

        // Do not add getters for values added at creation time
        // or you'll get infinite loops when things break
        if (Measurement.CIRCULARITY.equals(stat))
        	setStatistic(stat, calculateCircularity());
    }
    
    /**
     * Calculate the circularity of the object
     * @return
     */
    private double calculateCircularity() {
    	if (hasStatistic(Measurement.PERIMETER) && hasStatistic(Measurement.AREA)) {
    		double p = getStatistic(Measurement.PERIMETER);
    		double a = getStatistic(Measurement.AREA);
    		return (Math.PI*4*a)/(p*p);
    	}
    	return ERROR_CALCULATING_STAT;
    }

    @Override
    public synchronized void setStatistic(final Measurement stat, double d) {
        statistics.put(stat, d);
    }
    
    @Override
    public synchronized void clearStatistic(final Measurement stat) {
    	statistics.remove(stat);
    }

    @Override
    public Measurement[] getStatistics() {
        return this.statistics.keySet().toArray(new Measurement[0]);
    }

    @Override
	public void updateDependentStats() {
        for (Measurement stat : statistics.keySet()) {
            if (this.getStatistic(stat) == Statistical.STAT_NOT_CALCULATED)
                calculateStatistic(stat);
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
        return borderList.size();
    }

    @Override
	public IPoint getBorderPoint(int i) {
        return borderList.get(i);
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
        for(int i=0; i<borderList.size(); i++) {
        	IPoint n = borderList.get(i);
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
        borderList.get(i).setX(x);
        borderList.get(i).setY(y);
    }

    @Override
	public List<IPoint> getBorderList() {
        return this.borderList;
    }

    @Override
	public List<IPoint> getOriginalBorderList() {
        List<IPoint> result = new ArrayList<>(borderList.size());

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
        Rectangle r = new Rectangle(position[X_BASE], position[Y_BASE], position[WIDTH], position[HEIGHT]);
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

    @Override
	public double getMedianDistanceBetweenPoints() {
        double[] distances = new double[this.borderList.size()];
        for (int i = 0; i < this.borderList.size(); i++) {
            IPoint p = borderList.get(i);
            IPoint next = borderList.get(wrapIndex(i + 1));
            distances[i] = p.getLengthTo(next);
        }
        return Stats.quartile(distances, Stats.MEDIAN);
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
        for (int i = 0; i < borderList.size(); i++) {
            IPoint p = borderList.get(i);
            p.offset(xOffset, yOffset);
        }

        this.centreOfMass.offset(xOffset, yOffset);
        calculateBounds();

    }

    @Override
	public void reverse() {
    	LOGGER.fine("Reversing component border list");
        int[] newXpoints = new int[xpoints.length];
        int[] newYpoints = new int[xpoints.length];

        for (int i = xpoints.length - 1, j = 0; j < xpoints.length; i--, j++) {
            newXpoints[j] = xpoints[i];
            newYpoints[j] = ypoints[i];
        }
        xpoints = newXpoints;
        ypoints = newYpoints;

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
        float[] xpoints = new float[borderList.size() + 1];
        float[] ypoints = new float[borderList.size() + 1];

        for (int i = 0; i < borderList.size(); i++) {
            IPoint p = borderList.get(i);
            xpoints[i] = (float) p.getX() + xOffset;
            ypoints[i] = (float) p.getY() + yOffset;
        }

        // Ensure the polygon is closed
        xpoints[borderList.size()] = (float) borderList.get(0).getX() + xOffset;
        ypoints[borderList.size()] = (float) borderList.get(0).getY() + yOffset;

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

        if (borderList.isEmpty())
            throw new IllegalArgumentException("Border list is empty");

        if (shapeCache.has(xOffset, yOffset, scale)) {
            return shapeCache.get(xOffset, yOffset, scale);
        }

        double sc = scale.equals(MeasurementScale.MICRONS) ? this.scale : 1;

        Path2D.Double path = new Path2D.Double();

        IPoint first = borderList.get(0);
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

    /**
     * Create a boolean mask, in which 1 is within the nucleus and 0 is outside
     * the nucleus, for an image centred on the nuclear centre of mass, of the
     * given size
     * 
     * @param height
     * @param width
     * @return
     */
    @Override
	public Mask getBooleanMask(int height, int width) {

        int halfX = width >> 1;
        int halfY = height >> 1;

        boolean[][] result = new boolean[height][width];

        for (int x = -halfX, aX = 0; aX < width; x++, aX++) {
            for (int y = -halfY, aY = 0; aY < height; y++, aY++) {
                result[aY][aX] = this.containsPoint(IPoint.makeNew(x, y));
            }
        }
        return new BooleanMask(result);
    }

    /**
     * Create a boolean mask, in which true is within the nucleus and false is
     * outside the component, for the original source image of the component
     * 
     * @return a mask
     */
    @Override
	public Mask getSourceBooleanMask() {
        boolean[][] result;
        try {

            int width = this.getImage().getWidth();
            int height = this.getImage().getHeight();

            result = new boolean[height][width];

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    result[x][y] = this.containsPoint(x, y);
                }
            }

        } catch (UnloadableImageException e) {
            LOGGER.warning("Cannot load source image");
            result = new boolean[10][10];
        }
        return new BooleanMask(result);
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
    	return borderList.stream()
    			.filter(point->point.getLengthTo(p)>distToCom)
    			.min(Comparator.comparing(point->180-centreOfMass.findSmallestAngle(p, point) ))
    			.orElse(null); // TODO: backup solution?
    }

    @Override
    public IPoint findOrthogonalBorderPoint(@NonNull IPoint a) {
        return borderList.stream()
                .min(Comparator.comparing(point-> Math.abs(90-centreOfMass.findSmallestAngle(a, point)) ))
                .get();
    }

    @Override
    public IPoint findClosestBorderPoint(@NonNull IPoint p) {
        return borderList.stream()
                .min(Comparator.comparing(point->point.getLengthTo(p) ))
                .get();
    }
    
    @Override
    public String toString() {
    	return String.format("Bounds: x: %s-%s y: %s-%s", this.getBase().getX(), this.getBase().getX()+this.getBounds().getWidth(), 
    			this.getBase().getY(), this.getBase().getY()+this.getBounds().getHeight());
    }
    
    @Override
	public Element toXmlElement() {
    	Element e = new Element("Component").setAttribute("id", id.toString());
    	
    	e.addContent(new Element("Position").setText(Arrays.toString(position)));
    	e.addContent(new Element("CentreOfMass").setText(centreOfMass.getX()+","+centreOfMass.getY()));
    	e.addContent(new Element("OriginalCentreOfMass").setText(originalCentreOfMass.getX()+","+originalCentreOfMass.getY()));
    	
    	for(Entry<Measurement, Double> entry : statistics.entrySet()) {
    		e.addContent(new Element("Measurement")
    				.setAttribute("name", entry.getKey().toString())
    				.setText(entry.getValue().toString()));
    	}
    	
    	e.addContent(new Element("SourceFile").setText(sourceFile.getAbsolutePath()));
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
		result = prime * result + Arrays.hashCode(position);
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
						scale, statistics);
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
				&& Arrays.equals(position, other.position)
				&& Double.doubleToLongBits(scale) == Double.doubleToLongBits(other.scale)
				&& Objects.equals(statistics, other.statistics)
				&& Arrays.equals(xpoints, other.xpoints) 
				&& Arrays.equals(ypoints, other.ypoints);
	}

	/**
     * Create the border list from the stored int[] points. Mimics makeBorderList
     * but adds a check that the created border list does not affect tags
     * 
     * @param roi
     */
    @Override
	public void refreshBorderList(boolean useSplineFitting) {

    	LOGGER.finest(()->"Creating border list from "+xpoints.length+" integer points");
    	
        // Make a copy of the int[] points otherwise creating a polygon roi
        // will reset them to 0,0 coordinates
        int[] xcopy = Arrays.copyOf(xpoints, xpoints.length);
        int[] ycopy = Arrays.copyOf(ypoints, ypoints.length);
        PolygonRoi roi = new PolygonRoi(xcopy, ycopy, xpoints.length, Roi.TRACED_ROI);

        // Creating the border list will set everything to the original image
        // position.
        // Move the border list back over the CoM if needed.
        IPoint oldCoM = IPoint.makeNew(centreOfMass);
        centreOfMass = IPoint.makeNew(originalCentreOfMass);

        borderList = new ArrayList<>();

        // convert the roi positions to a list of border points
        // Each object decides whether it should be smoothed.
        boolean isSmooth = isSmoothByDefault();
        
        if(useSplineFitting)
        	roi.fitSplineForStraightening(); // this prevents the resulting border differing in length between invokations
                
        FloatPolygon smoothed = roi.getInterpolatedPolygon(INTERPOLATION_INTERVAL_PIXELS, isSmooth);

        LOGGER.finest( "Interpolated integer list to smoothed list of "+smoothed.npoints);
        for (int i = 0; i < smoothed.npoints; i++) {
            IPoint point = IPoint.makeNew(smoothed.xpoints[i], smoothed.ypoints[i]);
            borderList.add(point);
        }

        moveCentreOfMass(oldCoM);
        calculateBounds();
        LOGGER.finest( "Component has "+getBorderLength()+" border points");

    }

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {

        in.defaultReadObject();

        // Fill the transient fields
        imageRef = new WeakReference<>(null);
        shapeCache = new ShapeCache();

        // needs to be traced to allow interpolation into the border list
        makeBorderList();
    }

    private synchronized void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
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
        calculateBounds();
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
