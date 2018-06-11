/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.components;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.detection.BooleanMask;
import com.bmskinner.nuclear_morphology.analysis.detection.Mask;
import com.bmskinner.nuclear_morphology.analysis.image.ImageConverter;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.stats.NucleusStatistic;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.components.stats.SignalStatistic;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;
import com.bmskinner.nuclear_morphology.stats.Stats;
import com.bmskinner.nuclear_morphology.utility.AngleTools;

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

    private static final long serialVersionUID = 1L;
    private final UUID        id;

    /**
     * The original position in the source image of the component. Values are
     * stored at the indexes in {@link CellularComponent.X_BASE},
     * {@link CellularComponent.Y_BASE}, {@link CellularComponent.WIDTH} and
     * {@link CellularComponent.HEIGHT}
     * 
     * @see AbstractCellularComponent#getPosition()
     */
    private final int[] position;

    /**
     * The current centre of the object.
     */
    private IPoint centreOfMass;

    /**
     * The original centre of the object in its source image.
     */
    private final IPoint originalCentreOfMass;

    /**
     * The statistical values stored for this object
     */
    private Map<PlottableStatistic, Double> statistics = new HashMap<PlottableStatistic, Double>();

    /**
     * The source file the component was detected in. This is detected on
     * dataset loading as a relative path from the .nmd e.g.
     * C:\MyImageFolder\MyImage.tiff
     */
    private File sourceFile;

    /**
     * The RGB channel in which this component was detected
     */
    private int channel;

    /**
     * The length of a micron in pixels. Allows conversion between pixels and SI
     * units. Set to 1 by default.
     * 
     * @see AbstractCellularComponent#setScale()
     */
    private double scale = 1;

    /**
     * The points within the Roi from which the object was detected.
     */
    private int[] xpoints, ypoints;

    /*
     * TRANSIENT FILEDS
     */

    /**
     * The complete border list, offset to an appropriate position for the
     * object
     */
    private transient List<IBorderPoint> borderList = new ArrayList<IBorderPoint>(0);

    private transient SoftReference<ImageProcessor> imageRef = new SoftReference<ImageProcessor>(null); // allow
                                                                                                        // caching
                                                                                                        // of
                                                                                                        // images
                                                                                                        // while
                                                                                                        // memory
                                                                                                        // is
                                                                                                        // available

    private transient ShapeCache shapeCache = new ShapeCache();

    private transient Rectangle2D bounds;

    /**
     * Construct with an ROI, a source image and channel, and the original
     * position in the source image. It sets the immutable original centre of
     * mass, and the mutable current centre of mass. It also assigns a random ID
     * to the component.
     * 
     * @param roi
     *            the roi of the object
     * @param centerOfMass
     *            the original centre of mass of the component
     * @param source
     *            the image file the component was found in
     * @param channel
     *            the RGB channel the component was found in
     * @param position
     *            the bounding position of the component in the original image
     */
    public DefaultCellularComponent(Roi roi, IPoint centreOfMass, File source, int channel, int[] position) {
        if (centreOfMass == null) {
            throw new IllegalArgumentException("Centre of mass cannot be null");
        }

        if (roi == null) {
            throw new IllegalArgumentException("Roi cannot be null");
        }

        this.originalCentreOfMass = IPoint.makeNew(centreOfMass);
        this.centreOfMass = IPoint.makeNew(centreOfMass);
        this.id = java.util.UUID.randomUUID();
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
            throw new IllegalArgumentException("The centre of mass X (" + centreOfMass.getX() + ")"
                    + ") must be within the roi bounds (x = " + minX + "-" + maxX + ")");
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
            fine("Centre of mass is not inside the object. You may have a doughnut.");
        }

        this.xpoints = new int[polygon.npoints];
        this.ypoints = new int[polygon.npoints];

        // Discard empty indices left in polygon array
        for (int i = 0; i < polygon.npoints; i++) {
            this.xpoints[i] = polygon.xpoints[i];
            this.ypoints[i] = polygon.ypoints[i];
            // log("\tPoint at "+i+": "+this.xpoints[i]+", "+this.ypoints[i]);
        }

        // convert the roi positions to a list of nucleus border points
        // Only smooth the points for large objects like nuclei
        // log("Int array in constructor : "+this.xpoints[0]+",
        // "+this.ypoints[0]);
        makeBorderList();

    }

    /**
     * Create the border list from the stored int[] points. Move the centre of
     * mass to any stored position.
     * 
     * @param roi
     */
    private void makeBorderList() {

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

        borderList = new ArrayList<IBorderPoint>(0);

        // convert the roi positions to a list of border points
        // Each object decides whether it should be smoothed.
        boolean isSmooth = isSmoothByDefault();
        FloatPolygon smoothed = roi.getInterpolatedPolygon(1, isSmooth);

        for (int i = 0; i < smoothed.npoints; i++) {
            IBorderPoint point = IBorderPoint.makeNew(smoothed.xpoints[i], smoothed.ypoints[i]);

            if (i > 0) {
                point.setPrevPoint(borderList.get(i - 1));
                point.prevPoint().setNextPoint(point);
            }
            borderList.add(point);
        }
        // link endpoints
        borderList.get(borderList.size() - 1).setNextPoint(borderList.get(0));
        borderList.get(0).setPrevPoint(borderList.get(borderList.size() - 1));

        moveCentreOfMass(oldCoM);
        calculateBounds();

    }

    private void calculateBounds() {
        double xMax = -Double.MAX_VALUE;
        double xMin = Double.MAX_VALUE;
        double yMax = -Double.MAX_VALUE;
        double yMin = Double.MAX_VALUE;

        for (IBorderPoint p : borderList) {
            xMax = p.getX() > xMax ? p.getX() : xMax;
            xMin = p.getX() < xMin ? p.getX() : xMin;
            yMax = p.getY() > yMax ? p.getY() : yMax;
            yMin = p.getY() < yMin ? p.getY() : yMin;
        }

        double w = xMax - xMin;
        double h = yMax - yMin;

        // Constructor for rectangle - upper-left point plus w and h;
        // Note that the coordinates have y increasing downwards, so the upper
        // point is yMin
        bounds = new Rectangle2D.Double(xMin, yMin, w, h);

    }

    /**
     * Duplicate a component. The ID is kept consistent.
     * 
     * @param a
     *            the template component
     */
    protected DefaultCellularComponent(CellularComponent a) {
        this.id = a.getID();
        this.position = a.getPosition();
        this.originalCentreOfMass = a.getOriginalCentreOfMass();
        this.centreOfMass = IPoint.makeNew(a.getCentreOfMass());
        this.sourceFile = a.getSourceFile();
        this.channel = a.getChannel();
        this.scale = a.getScale();

        for (PlottableStatistic stat : a.getStatistics()) {
            try {
                this.setStatistic(stat, a.getStatistic(stat, MeasurementScale.PIXELS));
            } catch (Exception e) {
                stack("Error getting " + stat + " from template", e);
                this.setStatistic(stat, 0);
            }
        }

        if (a instanceof DefaultCellularComponent) {

            DefaultCellularComponent comp = (DefaultCellularComponent) a;

            this.xpoints = Arrays.copyOf(comp.xpoints, comp.xpoints.length);
            this.ypoints = Arrays.copyOf(comp.ypoints, comp.ypoints.length);
            makeBorderList();

        } else {
            duplicateBorderList(a);
        }
        finest("Created border list");

    }

    private void duplicateBorderList(CellularComponent c) {
        // Duplicate the border points
        this.borderList = new ArrayList<IBorderPoint>(c.getBorderLength());

        for (IBorderPoint p : c.getBorderList()) {
            borderList.add(IBorderPoint.makeNew(p));
        }

        // Link points

        for (int i = 0; i < borderList.size(); i++) {
            IBorderPoint point = borderList.get(i);

            if (i > 0 && i < borderList.size() - 1) {
                point.setNextPoint(borderList.get(i + 1));
                point.setPrevPoint(borderList.get(i - 1));
            }
        }

        // Set first and last
        IBorderPoint first = borderList.get(0);
        first.setNextPoint(borderList.get(1));
        first.setPrevPoint(borderList.get(borderList.size() - 1));

        IBorderPoint last = borderList.get(borderList.size() - 1);
        last.setNextPoint(borderList.get(0));
        last.setPrevPoint(borderList.get(borderList.size() - 2));
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

        // return new Rectangle( (int) bounds.getX(), (int)bounds.getY(),
        // (int)bounds.getWidth(), (int)bounds.getHeight());
        return bounds;
        // return this.toShape().getBounds();
    }

    /**
     * Get the source folder for images
     * 
     * @return
     */
    @Override
    public File getSourceFolder() {
        return this.sourceFile.getParentFile();
    }

    @Override
    public void updateSourceFolder(File newFolder) {
        File oldFile = sourceFile;
        String oldName = oldFile.getName();
        File newFile = new File(newFolder + File.separator + oldName);
        if (newFile.exists()) {
            this.setSourceFile(newFile);
        } else {
            throw new IllegalArgumentException(
                    "Cannot find file " + oldName + " in folder " + newFolder.getAbsolutePath());
        }

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
        if (ip != null) {
            return ip;
        }

        if (getSourceFile().exists()) {

            // Get the stack, make greyscale and invert
            int stack = ImageImporter.rgbToStack(getChannel());

            try {
                ImageStack imageStack = new ImageImporter(getSourceFile()).importToStack();
                ip = new ImageConverter(imageStack).convertToGreyscale(stack).toProcessor();
                ip.invert();

                imageRef = new SoftReference<ImageProcessor>(ip);

                return ip;

            } catch (ImageImportException e) {
                stack("Error importing source image " + this.getSourceFile().getAbsolutePath(), e);
                throw new UnloadableImageException("Source image is not available");
            }

        } else {
            throw new UnloadableImageException("Source image is not available: "+getSourceFile().getAbsolutePath());
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
            stack("Error importing source image " + this.getSourceFile().getAbsolutePath(), e);
            throw new UnloadableImageException("Source image is not available");
        }

    }

    @Override
    public ImageProcessor getRGBImage() throws UnloadableImageException {

        ImageProcessor ip = imageRef.get();
        if (ip != null) {
            return ip;
        }

        if (getSourceFile().exists()) {

            try {
                ip = new ImageImporter(getSourceFile()).importToColorProcessor();

                imageRef = new SoftReference<ImageProcessor>(ip);

                return ip;

            } catch (ImageImportException e) {
                stack("Error importing source image " + this.getSourceFile().getAbsolutePath(), e);
                throw new UnloadableImageException("Source image is not available");
            }

        } else {
            throw new UnloadableImageException("Source image is not available");
        }
    }

    @Override
	public ImageProcessor getComponentImage() throws UnloadableImageException {
        ImageProcessor ip = getImage().duplicate();

        if (ip == null) {
            throw new UnloadableImageException("Source image is not available");
        }

        int[] positions = getPosition();

        int padding = CellularComponent.COMPONENT_BUFFER; // a border of pixels
                                                          // beyond the cell
                                                          // boundary
        int wideW = (int) (positions[CellularComponent.WIDTH] + (padding * 2));
        int wideH = (int) (positions[CellularComponent.HEIGHT] + (padding * 2));
        int wideX = (int) (positions[CellularComponent.X_BASE] - padding);
        int wideY = (int) (positions[CellularComponent.Y_BASE] - padding);

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
            throw new UnloadableImageException("Source image is not available");
        }

        int[] positions = getPosition();

        int padding = CellularComponent.COMPONENT_BUFFER; // a border of pixels
                                                          // beyond the cell
                                                          // boundary
        int wideW = (positions[CellularComponent.WIDTH] + (padding * 2));
        int wideH = (positions[CellularComponent.HEIGHT] + (padding * 2));
        int wideX = (positions[CellularComponent.X_BASE] - padding);
        int wideY = (positions[CellularComponent.Y_BASE] - padding);

        wideX = wideX < 0 ? 0 : wideX;
        wideY = wideY < 0 ? 0 : wideY;

        ip.setRoi(wideX, wideY, wideW, wideH);
        ip = ip.crop();

        return ip;
    }

    // public void setSourceFileName(String name) {
    // this.sourceFileName = name;
    // }

    @Override
	public void setSourceFolder(File sourceFolder) {

        File newFile = new File(sourceFolder + File.separator + sourceFile.getName());

        this.sourceFile = newFile;
    }

    @Override
	public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    @Override
	public int getChannel() {
        return channel;
    }

    @Override
	public void setChannel(int channel) {
        this.channel = channel;
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
	public synchronized boolean hasStatistic(PlottableStatistic stat) {
        return this.statistics.containsKey(stat);
    }

    @Override
    public synchronized double getStatistic(PlottableStatistic stat) {
        return this.getStatistic(stat, MeasurementScale.PIXELS);
    }

    @Override
    public synchronized double getStatistic(PlottableStatistic stat, MeasurementScale scale) {

        if (this.statistics.containsKey(stat)) {
            double result = statistics.get(stat);
            return stat.convert(result, this.scale, scale);
        }
        
        double result = calculateStatistic(stat);
        setStatistic(stat, result);
        return result;
    }

    protected double calculateStatistic(PlottableStatistic stat) {
        double result = ERROR_CALCULATING_STAT;

        // Do not add getters for values added at creation time
        // or you'll get infinite loops when things break
        if (PlottableStatistic.CIRCULARITY.equals(stat))
            return this.getCircularity();
        return result;
    }

    /**
     * 
     * @return
     */
    private double getCircularity() {
        if (this.hasStatistic(PlottableStatistic.PERIMETER) && this.hasStatistic(PlottableStatistic.AREA)) {
            double perim2 = Math.pow(this.getStatistic(PlottableStatistic.PERIMETER, MeasurementScale.PIXELS), 2);
            return (4 * Math.PI) * (this.getStatistic(PlottableStatistic.AREA, MeasurementScale.PIXELS) / perim2);
        } else {
            return ERROR_CALCULATING_STAT;
        }

    }

    @Override
    public synchronized void setStatistic(PlottableStatistic stat, double d) {
        this.statistics.put(stat, d);
    }

    @Override
    public PlottableStatistic[] getStatistics() {
        return this.statistics.keySet().toArray(new PlottableStatistic[0]);
    }

    /**
     * If any stats are listed as uncalcualted, attempt to calculate them
     */
    @Override
	public void updateDependentStats() {
        for (PlottableStatistic stat : this.getStatistics()) {
            if (this.getStatistic(stat) == STAT_NOT_CALCULATED)
                this.setStatistic(stat, calculateStatistic(stat));
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
        return this.borderList.size();
    }

    @Override
	public IBorderPoint getBorderPoint(int i) {
        return this.borderList.get(i);
    }

    @Override
	public IBorderPoint getOriginalBorderPoint(int i) {
        IBorderPoint p = getBorderPoint(i);

        double diffX = p.getX() - centreOfMass.getX();
        double diffY = p.getY() - centreOfMass.getY();

        // Offset to the original position
        IBorderPoint ip = IBorderPoint.makeNew(originalCentreOfMass.getX() + diffX,
                originalCentreOfMass.getY() + diffY);

        return ip;
    }

    @Override
	public int getBorderIndex(IBorderPoint p) {
        int i = 0;
        for (IBorderPoint n : borderList) {
            if (n.getX() == p.getX() && n.getY() == p.getY()) {
                return i;
            }
            i++;
        }
        return -1; // default if no match found
    }

    @Override
	public void updateBorderPoint(int i, IPoint p) {
        this.updateBorderPoint(i, p.getX(), p.getY());
    }

    @Override
	public void updateBorderPoint(int i, double x, double y) {
        borderList.get(i).setX(x);
        borderList.get(i).setY(y);
    }

    @Override
	public List<IBorderPoint> getBorderList() {
        return this.borderList;
    }

    @Override
	public List<IBorderPoint> getOriginalBorderList() {
        List<IBorderPoint> result = new ArrayList<IBorderPoint>(borderList.size());

        double diffX = originalCentreOfMass.getX() - centreOfMass.getX();
        double diffY = originalCentreOfMass.getY() - centreOfMass.getY();

        // Offset to the original position
        for (IBorderPoint p : borderList) {
            result.add(IBorderPoint.makeNew(p.getX() + diffX, p.getY() + diffY));
        }
        return result;
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
        if (!bounds.contains(p.toPoint2D())) {
            return false;
        }

        // Check detailed position
        if (this.toPolygon().contains((float) p.getX(), (float) p.getY())) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * Check if a given point lies within the nucleus
     * 
     * @param p
     * @return
     */
    @Override
	public boolean containsPoint(int x, int y) {
        // Check detailed position

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

        // Check detailed position
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

        if (!r.contains(x, y)) {
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

    /*
     * (non-Javadoc)
     * 
     * @see
     * components.CellularComponent#flipXAroundPoint(components.generic.IPoint)
     */
    @Override
	public void flipXAroundPoint(IPoint p) {

        double xCentre = p.getX();

        for (IBorderPoint n : borderList) {
            double dx = xCentre - n.getX();
            double xNew = xCentre + dx;
            n.setX(xNew);
        }

    }

    @Override
	public double getMedianDistanceBetweenPoints() {
        double[] distances = new double[this.borderList.size()];
        for (int i = 0; i < this.borderList.size(); i++) {
            IBorderPoint p = borderList.get(i);
            IBorderPoint next = borderList.get(wrapIndex(i + 1));
            distances[i] = p.getLengthTo(next);
        }
        return Stats.quartile(distances, Stats.MEDIAN);
    }

    /**
     * Translate the XY coordinates of each border point so that the nuclear
     * centre of mass is at the given point
     * 
     * @param point
     *            the new centre of mass
     */
    @Override
	public void moveCentreOfMass(IPoint point) {

        // get the difference between the x and y positions
        // of the points as offsets to apply
        double xOffset = point.getX() - centreOfMass.getX();
        double yOffset = point.getY() - centreOfMass.getY();

        offset(xOffset, yOffset);
    }

    /**
     * Translate the XY coordinates of the object
     * 
     * @param xOffset
     *            the amount to move in the x-axis
     * @param yOffset
     *            the amount to move in the y-axis
     */
    @Override
	public void offset(double xOffset, double yOffset) {

        /// update each border point
        for (int i = 0; i < borderList.size(); i++) {
            IBorderPoint p = borderList.get(i);
            p.offset(xOffset, yOffset);
        }

        this.centreOfMass.offset(xOffset, yOffset);
        calculateBounds();

    }

    @Override
	public void reverse() {

        int[] newXpoints = new int[xpoints.length], newYpoints = new int[xpoints.length];

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
            IBorderPoint p = borderList.get(i);
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

        // Converts whatever coordinates are in the border
        // to a shape
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
        // TODO: offset to current position
        return new PolygonRoi(xpoints, ypoints, xpoints.length, Roi.POLYGON);
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

        if (borderList.size() == 0) {
            throw new IllegalArgumentException("Border list is empty");
        }

        if (shapeCache.has(xOffset, yOffset, scale)) {
            return shapeCache.get(xOffset, yOffset, scale);
        }

        double sc = scale.equals(MeasurementScale.MICRONS) ? this.scale : 1;

        Path2D.Double path = new Path2D.Double();

        IBorderPoint first = borderList.get(0);
        path.moveTo((first.getX() + xOffset) / sc, (first.getY() + yOffset) / sc);

        for (IBorderPoint b : this.borderList) {
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
            warn("Cannot load source image");

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
	public int getPositionBetween(IBorderPoint pointA, IBorderPoint pointB) {

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
        int mid1 = this.wrapIndex((int) Math.floor((difference1 / 2) + minIndex));
        int mid2 = this.wrapIndex((int) Math.floor((difference2 / 2) + maxIndex));

        return difference1 < difference2 ? mid1 : mid2;
    }

    @Override
	public IBorderPoint findOppositeBorder(IBorderPoint p) {
        // Find the point that is closest to 180 degrees across the CoM
        return borderList.stream()
            .min(Comparator.comparing(point->180-centreOfMass.findAngle(p, point) ))
            .get();
    }

    @Override
    public IBorderPoint findOrthogonalBorderPoint(IBorderPoint a) {
        
        return borderList.stream()
                .min(Comparator.comparing(point-> Math.abs(90-centreOfMass.findAngle(a, point)) ))
                .get();
    }

    @Override
    public IBorderPoint findClosestBorderPoint(IPoint p) {
        
        return borderList.stream()
                .min(Comparator.comparing(point->point.getLengthTo(p) ))
                .get();
    }
    
    
    

    /*
     * 
     * SERIALIZATION METHODS
     * 
     */

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((centreOfMass == null) ? 0 : centreOfMass.hashCode());
		result = prime * result + channel;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((originalCentreOfMass == null) ? 0 : originalCentreOfMass.hashCode());
		result = prime * result + Arrays.hashCode(position);
		long temp;
		temp = Double.doubleToLongBits(scale);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((sourceFile == null) ? 0 : sourceFile.hashCode());
		result = prime * result + ((statistics == null) ? 0 : statistics.hashCode());
		result = prime * result + Arrays.hashCode(xpoints);
		result = prime * result + Arrays.hashCode(ypoints);
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
		if (centreOfMass == null) {
			if (other.centreOfMass != null)
				return false;
		} else if (!centreOfMass.equals(other.centreOfMass))
			return false;
		if (channel != other.channel)
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (originalCentreOfMass == null) {
			if (other.originalCentreOfMass != null)
				return false;
		} else if (!originalCentreOfMass.equals(other.originalCentreOfMass))
			return false;
		if (!Arrays.equals(position, other.position))
			return false;
		if (Double.doubleToLongBits(scale) != Double.doubleToLongBits(other.scale))
			return false;
		if (sourceFile == null) {
			if (other.sourceFile != null)
				return false;
		} else if (!sourceFile.equals(other.sourceFile))
			return false;
		if (statistics == null) {
			if (other.statistics != null)
				return false;
		} else if (!statistics.equals(other.statistics))
			return false;
		if (!Arrays.equals(xpoints, other.xpoints))
			return false;
		if (!Arrays.equals(ypoints, other.ypoints))
			return false;
		return true;
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {

        in.defaultReadObject();

        // Fill the transient fields
        imageRef = new SoftReference<ImageProcessor>(null);
        shapeCache = new ShapeCache();

        // needs to be traced to allow interpolation into the border list
        makeBorderList(); // This will update the border to the original CoM
                          // saved

        Set<PlottableStatistic> set = new HashSet<PlottableStatistic>(statistics.keySet());
        Iterator<PlottableStatistic> it = set.iterator();

        // Update any old stats to generic plottable statistics
        // TODO - this should be removed for 1.14.0, as it was added in 1.13.4
        // for compatibility
        while (it.hasNext()) {
            PlottableStatistic stat = it.next();
            double value = statistics.get(stat);

            if (stat.equals(NucleusStatistic.AREA)) {
                statistics.put(PlottableStatistic.AREA, value);
            }

            if (stat.equals(NucleusStatistic.PERIMETER)) {
                statistics.put(PlottableStatistic.PERIMETER, value);
            }

            if (stat.equals(NucleusStatistic.MAX_FERET)) {
                statistics.put(PlottableStatistic.MAX_FERET, value);
            }

            if (stat.equals(NucleusStatistic.MIN_DIAMETER)) {
                statistics.put(PlottableStatistic.MIN_DIAMETER, value);
            }

            if (stat.equals(NucleusStatistic.ASPECT)) {
                statistics.put(PlottableStatistic.ASPECT, value);
            }

            if (stat.equals(NucleusStatistic.BOUNDING_HEIGHT)) {
                statistics.put(PlottableStatistic.BOUNDING_HEIGHT, value);
            }

            if (stat.equals(NucleusStatistic.BOUNDING_WIDTH)) {
                statistics.put(PlottableStatistic.BOUNDING_WIDTH, value);
            }

            if (stat.equals(NucleusStatistic.OP_RP_ANGLE)) {
                statistics.put(PlottableStatistic.OP_RP_ANGLE, value);
            }

            if (stat.equals(NucleusStatistic.HOOK_LENGTH)) {
                statistics.put(PlottableStatistic.HOOK_LENGTH, value);
            }

            if (stat.equals(NucleusStatistic.BODY_WIDTH)) {
                statistics.put(PlottableStatistic.BODY_WIDTH, value);
            }

            if (stat.equals(SignalStatistic.ANGLE)) {
                statistics.put(PlottableStatistic.ANGLE, value);
            }

            if (stat.equals(SignalStatistic.AREA)) {
                statistics.put(PlottableStatistic.AREA, value);
            }

            if (stat.equals(SignalStatistic.DISTANCE_FROM_COM)) {
                statistics.put(PlottableStatistic.DISTANCE_FROM_COM, value);
            }

            if (stat.equals(SignalStatistic.FRACT_DISTANCE_FROM_COM)) {
                statistics.put(PlottableStatistic.FRACT_DISTANCE_FROM_COM, value);
            }

            if (stat.equals(SignalStatistic.MAX_FERET)) {
                statistics.put(PlottableStatistic.MAX_FERET, value);
            }

            if (stat.equals(SignalStatistic.MIN_DIAMETER)) {
                statistics.put(PlottableStatistic.MIN_DIAMETER, value);
            }

            if (stat.equals(SignalStatistic.PERIMETER)) {
                statistics.put(PlottableStatistic.PERIMETER, value);
            }

            if (stat.equals(SignalStatistic.RADIUS)) {
                statistics.put(PlottableStatistic.RADIUS, value);
            }

        }

    }

    private synchronized void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    /*
     * ############################################# Methods implementing the
     * Rotatable interface #############################################
     */

    /**
     * Rotate the nucleus so that the given point is directly below the centre
     * of mass
     * 
     * @param bottomPoint
     */
    @Override
	public void rotatePointToBottom(IPoint bottomPoint) {
        this.alignPointsOnVertical(centreOfMass, bottomPoint);
    }

    @Override
	public abstract void alignVertically();

    @Override
    public void rotate(double angle) {
        if (angle != 0) {

            for (IPoint p : borderList) {

                IPoint newPoint = AngleTools.rotateAboutPoint(p, centreOfMass, angle);
                // IPoint newPoint = getPositionAfterRotation(p, angle);
                p.set(newPoint);
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

        private Map<Key, Shape> cache = new HashMap<Key, Shape>();

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
