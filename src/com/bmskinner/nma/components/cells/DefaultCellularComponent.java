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
package com.bmskinner.nma.components.cells;

import java.awt.Polygon;
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

import com.bmskinner.nma.components.ComponentMeasurer;
import com.bmskinner.nma.components.Imageable;
import com.bmskinner.nma.components.MissingComponentException;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.measure.DefaultMeasurement;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.io.XMLReader;
import com.bmskinner.nma.io.XmlSerializable;
import com.bmskinner.nma.utility.ArrayUtils;

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

	private static final String XML_REVERSE = "reverse";

	private static final String XML_YPOINTS = "ypoints";

	private static final String XML_XPOINTS = "xpoints";

	private static final String XML_SCALE = "Scale";

	private static final String XML_CHANNEL = "Channel";

	private static final String XML_SOURCE_FILE = "SourceFile";

	private static final String XML_COMPONENT = "Component";

	private static final String XML_ORIGINAL_CENTRE_OF_MASS = "OriginalCentreOfMass";

	private static final String XML_Y = "y";

	private static final String XML_X = "x";

	private static final String XML_ID = "id";

	private static final String XML_COM = "CoM";

	private static final Logger LOGGER = Logger.getLogger(DefaultCellularComponent.class.getName());

	/** The pixel spacing between border points after roi interpolation */
	private static final int INTERPOLATION_INTERVAL_PIXELS = 1;

	private final UUID id;

	/** The current centre of the object. */
	private IPoint centreOfMass;

	/** The original centre of the object in its source image. */
	private final IPoint originalCentreOfMass;

	/** The measurements stored for this object */
	private Map<Measurement, Double> measurements = new HashMap<>();

	/**
	 * The source file the component was detected in. This is detected on dataset
	 * loading as a relative path from the nmd
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

	/** The points within the roi from which the object was detected */
	private final int[] xpoints;
	private final int[] ypoints;

	/** Whether the x and y points should be reversed when making the border */
	private boolean isReversed = false;

	/** The complete border list interpolated from the roi */
	private IPoint[] borderList = new IPoint[0];

	/** The object bounding box */
	private Rectangle2D bounds;

	private static UUID makeUUID(Roi roi, IPoint centreOfMass) {
		int[] xpoints = Arrays.copyOfRange(roi.getPolygon().xpoints, 0, roi.getPolygon().npoints);
		int[] ypoints = Arrays.copyOfRange(roi.getPolygon().ypoints, 0, roi.getPolygon().npoints);
		return new UUID(Arrays.hashCode(xpoints) * Arrays.hashCode(ypoints), centreOfMass.hashCode());
	}

	/**
	 * Construct with an ROI, a source image and channel, and the original position
	 * in the source image. It sets the immutable original centre of mass, and the
	 * mutable current centre of mass. It also assigns a random ID to the component.
	 * 
	 * @param roi          the roi of the object
	 * @param centerOfMass the original centre of mass of the component
	 * @param source       the image file the component was found in
	 * @param channel      the RGB channel the component was found in
	 * @param position     the bounding position of the component in the original
	 *                     image
	 */
	protected DefaultCellularComponent(@NonNull Roi roi, @NonNull IPoint centreOfMass, File source,
			int channel) {

		// If we have no UUID given, can we create a deterministic UUID from the roi and
		// CoM?
		this(roi, centreOfMass, source, channel, makeUUID(roi, centreOfMass));
//		this(roi, centreOfMass, source, channel, UUID.randomUUID());
	}

	/**
	 * Construct with an ROI, a source image and channel, and the original position
	 * in the source image. It sets the immutable original centre of mass, and the
	 * mutable current centre of mass. It also assigns a random ID to the component.
	 * 
	 * @param roi          the roi of the object
	 * @param centerOfMass the original centre of mass of the component
	 * @param source       the image file the component was found in
	 * @param channel      the RGB channel the component was found in
	 * @param position     the bounding position of the component in the original
	 *                     image
	 * @param id           the id of the component. Only use when deserialising!
	 */
	protected DefaultCellularComponent(@NonNull Roi roi, @NonNull IPoint centreOfMass, File source,
			int channel, @Nullable UUID id) {

		// Sanity check: is the CoM inside the roi
		if (!doesRoiMatchCom(roi, centreOfMass))
			throw new IllegalArgumentException("Centre of mass is not inside ROI");

		this.originalCentreOfMass = new FloatPoint(centreOfMass);
		this.centreOfMass = new FloatPoint(centreOfMass);
//		this.id = id == null ? UUID.randomUUID() : id;
		this.id = id == null ? makeUUID(roi, centreOfMass) : id;

		this.sourceFile = source;
		this.channel = channel;

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
	 * Defensively duplicate a component. The ID is kept consistent.
	 * 
	 * @param a the template component
	 */
	protected DefaultCellularComponent(@NonNull CellularComponent a) {

		if (!(a instanceof DefaultCellularComponent))
			throw new IllegalArgumentException(
					"Input is incorrect class: " + a.getClass().getName());
		DefaultCellularComponent other = (DefaultCellularComponent) a;

		this.id = UUID.fromString(a.getID().toString());
		this.originalCentreOfMass = a.getOriginalCentreOfMass().duplicate();
		this.centreOfMass = a.getCentreOfMass().duplicate();
		this.sourceFile = new File(a.getSourceFile().getPath());
		this.channel = a.getChannel();
		this.scale = a.getScale();

		for (Measurement stat : a.getMeasurements()) {
			setMeasurement(stat, a.getMeasurement(stat, MeasurementScale.PIXELS));
		}

		this.xpoints = Arrays.copyOf(other.xpoints, other.xpoints.length);
		this.ypoints = Arrays.copyOf(other.ypoints, other.ypoints.length);
		this.isReversed = a.isReversed();

		borderList = new IPoint[other.borderList.length];

		for (int i = 0; i < borderList.length; i++)
			borderList[i] = other.borderList[i].duplicate();

		updateBounds();
	}

	/**
	 * Construct from an XML element. Use for unmarshalling. The element should
	 * conform to the specification in {@link XmlSerializable}.
	 * 
	 * @param e the XML element containing the data.
	 */
	protected DefaultCellularComponent(Element e) {
		id = UUID.fromString(e.getAttributeValue(XML_ID));

		centreOfMass = new FloatPoint(
				Float.parseFloat(e.getChild(XML_COM).getAttributeValue(XML_X)),
				Float.parseFloat(e.getChild(XML_COM).getAttributeValue(XML_Y)));

		originalCentreOfMass = new FloatPoint(
				Float.parseFloat(e.getChild(XML_ORIGINAL_CENTRE_OF_MASS).getAttributeValue(XML_X)),
				Float.parseFloat(e.getChild(XML_COM).getAttributeValue(XML_Y)));

		// Add measurements
		for (Element el : e.getChildren("Measurement")) {
			Measurement m = new DefaultMeasurement(el);
			measurements.put(m, Double.parseDouble(el.getAttributeValue("value")));
		}

		sourceFile = new File(e.getChildText(XML_SOURCE_FILE));
		channel = Integer.parseInt(e.getChildText(XML_CHANNEL));
		scale = Double.parseDouble(e.getChildText(XML_SCALE));

		xpoints = XMLReader.parseIntArray(e.getChildText(XML_XPOINTS));
		ypoints = XMLReader.parseIntArray(e.getChildText(XML_YPOINTS));
		isReversed = e.getChild(XML_XPOINTS).getAttributeValue(XML_REVERSE) != null;

		makeBorderList();
	}

	/**
	 * Create the border list from the stored int[] points. Move the centre of mass
	 * to any stored position.
	 * 
	 * @param roi
	 */
	private void makeBorderList() {

		// Make a copy of the int[] points otherwise creating a polygon roi
		// will reset them to 0,0 coordinates
		int[] xcopy = Arrays.copyOf(xpoints, xpoints.length);
		int[] ycopy = Arrays.copyOf(ypoints, ypoints.length);

		if (isReversed) {
			xcopy = ArrayUtils.reverse(xcopy);
			ycopy = ArrayUtils.reverse(ycopy);
		}

		PolygonRoi roi = new PolygonRoi(xcopy, ycopy, xcopy.length, Roi.TRACED_ROI);

		// Creating the border list will set everything to the original image
		// position.
		// Move the border list back over the CoM if needed.
//		IPoint oldCoM = centreOfMass.duplicate();
//		centreOfMass = originalCentreOfMass.duplicate();

		// convert the roi positions to border points
		roi.fitSplineForStraightening(); // this prevents the resulting border differing in length
											// between invokations

		FloatPolygon smoothed = roi.getInterpolatedPolygon(INTERPOLATION_INTERVAL_PIXELS, true);

		borderList = new IPoint[smoothed.npoints];
		for (int i = 0; i < smoothed.npoints; i++)
			borderList[i] = new FloatPoint(smoothed.xpoints[i], smoothed.ypoints[i]);

//		moveCentreOfMass(oldCoM);
		updateBounds();
	}

	/**
	 * Test if the provided centre of mass is within the the given roi
	 * 
	 * @param roi
	 * @param com
	 * @return
	 */
	private boolean doesRoiMatchCom(Roi roi, IPoint com) {
		Polygon polygon = roi.getPolygon();
		Rectangle2D rect = polygon.getBounds().getFrame();
		return rect.contains(com.getX(), com.getY());
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

		double w = xMax - xMin;
		double h = yMax - yMin;

		bounds = new Rectangle2D.Double(xMin, yMin, w, h);
	}

	@Override
	public @NonNull UUID getID() {
		return id;
	}

	@Override
	public int getXBase() {
		return (int) bounds.getMinX();
	}

	@Override
	public int getYBase() {
		return (int) bounds.getMinY();
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
		int x = Arrays.stream(xpoints).min().getAsInt();
		int y = Arrays.stream(ypoints).min().getAsInt();
		return new FloatPoint(x, y);
	}

	@Override
	public IPoint getBase() {
		return new FloatPoint(bounds.getMinX(), bounds.getMinY());
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
	 * Get the absolute path to the source image on the current computer. Merges the
	 * dynamic image folder with the image name
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
	public synchronized double getMeasurement(@NonNull final Measurement stat) {
		return this.getMeasurement(stat, MeasurementScale.PIXELS);
	}

	@Override
	public synchronized double getMeasurement(@NonNull final Measurement stat,
			@NonNull final MeasurementScale measurementScale) {
		if (!this.measurements.containsKey(stat)) {
			setMeasurement(stat, ComponentMeasurer.calculate(stat, this));
		}
		return stat.convert(measurements.get(stat), this.scale, measurementScale);
	}

	@Override
	public synchronized void setMeasurement(@NonNull final Measurement stat, double d) {
		measurements.put(stat, d);
	}

	@Override
	public synchronized void clearMeasurement(@NonNull final Measurement stat) {
		measurements.remove(stat);
	}

	@Override
	public List<Measurement> getMeasurements() {
		List<Measurement> result = new ArrayList<>();
		result.addAll(measurements.keySet());
		return result;
	}

	@Override
	public void clearMeasurements() {
		measurements.clear();
	}

	@Override
	public IPoint getCentreOfMass() {
		return centreOfMass;
	}

	@Override
	public IPoint getOriginalCentreOfMass() {
		return new FloatPoint(originalCentreOfMass);
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
		return new FloatPoint(originalCentreOfMass.getX() + diffX,
				originalCentreOfMass.getY() + diffY);
	}

	@Override
	public boolean isReversed() {
		return isReversed;
	}

	@Override
	public int getBorderIndex(@NonNull IPoint p) {
		for (int i = 0; i < borderList.length; i++) {
			IPoint n = borderList[i];
			if (n.overlapsPerfectly(p))
				return i;
		}
		return -1; // default if no match found
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
			result.add(new FloatPoint(p.getX() + diffX, p.getY() + diffY));
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
		if (!bounds.contains(p.toPoint2D()))
			return false;
		// Check detailed position
		return (this.toPolygon().contains((float) p.getX(), (float) p.getY()));
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
	 * Check if a given point in the original source image lies within the nucleus
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
		return this.toOriginalPolygon().contains(x, y);
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

		// Also update the CoM
		double dx = xCentre - centreOfMass.getX();
		double xNew = xCentre + dx;
		centreOfMass.setX(xNew);
		updateBounds();
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

		// Also update the CoM
		double dy = yCentre - centreOfMass.getY();
		double yNew = yCentre + dy;
		centreOfMass.setY(yNew);
		updateBounds();
	}

	/**
	 * Translate the XY coordinates of each border point so that the nuclear centre
	 * of mass is at the given point
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

		centreOfMass.offset(xOffset, yOffset);
		updateBounds();

	}

	@Override
	public void reverse() throws MissingComponentException, ProfileException {
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
	 * Turn the list of border points into a polygon. The points are at the original
	 * position in a source image.
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
		float[] xp = new float[borderList.length + 1];
		float[] yp = new float[borderList.length + 1];

		for (int i = 0; i < borderList.length; i++) {
			IPoint p = borderList[i];
			xp[i] = (float) p.getX() + xOffset;
			yp[i] = (float) p.getY() + yOffset;
		}

		// Ensure the polygon is closed
		xp[borderList.length] = (float) borderList[0].getX() + xOffset;
		yp[borderList.length] = (float) borderList[0].getY() + yOffset;

		return new FloatPolygon(xp, yp);
	}

	@Override
	public Shape toShape() {
		// Converts whatever coordinates are in the border to a shape
		return toOffsetShape(0, 0);
	}

	@Override
	public Shape toShape(MeasurementScale sc) {
		// Converts whatever coordinates are in the border to a shape
		return toOffsetShape(0, 0, sc);
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
	 * Create a shape from the border of the object with the given translation and
	 * at the given scale
	 * 
	 * @param xOffset
	 * @param yOffset
	 * @param ms
	 * @return
	 */
	private Shape toOffsetShape(double xOffset, double yOffset, MeasurementScale ms) {

		if (borderList.length == 0)
			throw new IllegalArgumentException("Border list is empty");

		double sc = MeasurementScale.MICRONS.equals(ms) ? this.scale : 1;

		Path2D.Double path = new Path2D.Double();

		IPoint first = borderList[0];
		path.moveTo((first.getX() + xOffset) / sc, (first.getY() + yOffset) / sc);

		for (int i = 1; i < borderList.length; i++)
			path.lineTo((borderList[i].getX() + xOffset) / sc,
					(borderList[i].getY() + yOffset) / sc);

		path.closePath();
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
	 * For two NucleusBorderPoints in a Nucleus, find the point that lies halfway
	 * between them Used for obtaining a consensus between potential tail positions.
	 * Ensure we choose the smaller distance
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
		int mid1 = this.wrapIndex((int) Math.floor(((double) difference1 / 2) + minIndex));
		int mid2 = this.wrapIndex((int) Math.floor(((double) difference2 / 2) + maxIndex));

		return difference1 < difference2 ? mid1 : mid2;
	}

	@Override
	public IPoint findOppositeBorder(@NonNull IPoint p) {

		double distToCom = p.getLengthTo(centreOfMass);

		int mini = 0;
		double l = Double.MAX_VALUE;

		for (int i = 0; i < borderList.length; i++) {
			// Look for the points at which the direct distance between the two points is
			// closest to the sum of their respective distances to the centre of mass
			IPoint point = borderList[i];

			double p2p = point.getLengthTo(p);

			if (p2p <= distToCom)
				continue;

			double d = Math.abs(point.getLengthTo(centreOfMass)
					+ distToCom
					- p2p);

			if (d < l) {
				l = d;
				mini = i;
			}
		}
		return borderList[mini];
	}

	@Override
	public IPoint findOrthogonalBorderPoint(@NonNull IPoint a) {
		return Arrays.stream(borderList)
				.min(Comparator.comparing(
						point -> Math.abs(90 - centreOfMass.findSmallestAngle(a, point))))
				.get();
	}

	@Override
	public IPoint findClosestBorderPoint(@NonNull IPoint p) {
		return Arrays.stream(borderList).min(Comparator.comparing(point -> point.getLengthTo(p)))
				.get();
	}

	@Override
	public String toString() {
		String newLine = System.getProperty("line.separator");
		StringBuilder builder = new StringBuilder("ID: " + id.toString() + newLine);
		builder.append(
				String.format("X bounds: %s-%s", this.getBase().getX(),
						this.getBase().getX() + this.getWidth()));
		builder.append(newLine);
		builder.append(
				String.format("Y bounds: %s-%s", this.getBase().getY(),
						this.getBase().getY() + this.getHeight()));
		builder.append(newLine);

		builder.append("Border " + borderList.length + ": ");
		builder.append(Arrays.deepToString(borderList));

		builder.append(newLine);

		builder.append("CoM: " + centreOfMass);
		builder.append(newLine);
		builder.append("Original CoM: " + originalCentreOfMass);
		builder.append(newLine);
		builder.append("Source file: " + sourceFile);
		builder.append(newLine);
		builder.append("Channel: " + channel);
		builder.append(newLine);
		builder.append("Scale: " + scale);
		builder.append(newLine);
		builder.append("isReversed: " + isReversed);
		builder.append(newLine);
		builder.append("xpoints: " + Arrays.toString(xpoints));
		builder.append(newLine);
		builder.append("ypoints: " + Arrays.toString(ypoints));
		builder.append(newLine);

		// Sort by measurement name
		builder.append("Measurements: " + newLine);
		List<Measurement> mes = new ArrayList<>(measurements.keySet());
		mes.sort((c1, c2) -> c1.name().compareTo(c2.name()));

		for (Measurement entry : mes) {
			builder.append(entry.toString() + ": " + measurements.get(entry).toString() + newLine);
		}

		return builder.toString();
	}

	@Override
	public Element toXmlElement() {
		Element e = new Element(XML_COMPONENT).setAttribute(XML_ID, id.toString());

		e.addContent(new Element(XML_COM).setAttribute(XML_X, String.valueOf(centreOfMass.getX()))
				.setAttribute(XML_Y,
						String.valueOf(centreOfMass.getY())));

		e.addContent(new Element(XML_ORIGINAL_CENTRE_OF_MASS)
				.setAttribute(XML_X, String.valueOf(originalCentreOfMass.getX()))
				.setAttribute(XML_Y, String.valueOf(originalCentreOfMass.getY())));

		for (Entry<Measurement, Double> entry : measurements.entrySet()) {
			e.addContent(entry.getKey().toXmlElement().setAttribute("value",
					entry.getValue().toString()));
		}

		e.addContent(new Element(XML_SOURCE_FILE).setText(sourceFile.toString()));
		e.addContent(new Element(XML_CHANNEL).setText(String.valueOf(channel)));
		e.addContent(new Element(XML_SCALE).setText(String.valueOf(scale)));

		Element xEl = new Element(XML_XPOINTS).setText(Arrays.toString(xpoints));
		if (isReversed)
			xEl.setAttribute(XML_REVERSE, "true"); // don't waste space
		e.addContent(xEl);

		e.addContent(new Element(XML_YPOINTS).setText(Arrays.toString(ypoints)));
		return e;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(xpoints);
		result = prime * result + Arrays.hashCode(ypoints);

		result = prime * result
				+ Objects.hash(centreOfMass, sourceFile, channel, id, originalCentreOfMass, scale,
						measurements);
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

		if (measurements.size() != other.measurements.size())
			return false;
		for (Entry<Measurement, Double> e : measurements.entrySet()) {
			if (!e.getValue().equals(other.measurements.get(e.getKey()))) {
				return false;
			}
		}

		return Objects.equals(centreOfMass, other.centreOfMass) && isSameFile
				&& channel == other.channel
				&& Objects.equals(id, other.id)
				&& Objects.equals(originalCentreOfMass, other.originalCentreOfMass)
				&& Double.doubleToLongBits(scale) == Double.doubleToLongBits(other.scale)
				&& Arrays.equals(xpoints, other.xpoints) && Arrays.equals(ypoints, other.ypoints);
	}

	/*
	 * ############################################# Methods implementing the
	 * Rotatable interface #############################################
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
		rotate(angle, centreOfMass);
	}

	@Override
	public void rotate(double angle, IPoint anchor) {
		if (angle != 0) {
			double rad = Math.toRadians(-angle);
			AffineTransform tf = AffineTransform.getRotateInstance(rad, anchor.getX(),
					anchor.getY());
			for (IPoint p : borderList) {
				Point2D result = tf.transform(p.toPoint2D(), null);
				p.set(result);
			}
			Point2D newCoM = tf.transform(centreOfMass.toPoint2D(), null);
			centreOfMass.set(newCoM);
		}
		updateBounds();
	}
}
