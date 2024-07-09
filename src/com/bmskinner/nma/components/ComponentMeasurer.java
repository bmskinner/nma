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
package com.bmskinner.nma.components;

import java.util.Arrays;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.cells.UnavailableBorderPointException;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MissingMeasurementException;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.stats.Stats;

import ij.gui.Roi;
import ij.process.FloatPolygon;

/**
 * Calculate basic stats for cellular components. Used to fill in missing values
 * when objects were not created using a detector
 *
 * @author ben
 * @since 1.13.5
 *
 */
public final class ComponentMeasurer {

	private static final Logger LOGGER = Logger.getLogger(ComponentMeasurer.class.getName());

	/**
	 * We only use static methods here. Don't allow a public constructor.
	 */
	private ComponentMeasurer() {
	}

	/**
	 * Calculate a measurement for a cell. If the measurement type is not valid for
	 * the component, Statistical.STAT_NOT_CALCULATED will be returned.
	 * 
	 * @param m the measurement to make
	 * @param c the cell to measure
	 * @return
	 * @throws SegmentUpdateException
	 * @throws ComponentCreationException
	 * @throws MissingDataException
	 */
	public static double calculate(Measurement m, ICell c)
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {
		if (Measurement.CELL_NUCLEUS_COUNT.equals(m))
			return c.getNuclei().size();

		if (Measurement.CELL_NUCLEAR_AREA.equals(m))
			return calculateCellNuclearArea(c);

		if (Measurement.CELL_NUCLEAR_RATIO.equals(m))
			return calculateCellNuclearRatio(c);
		throw new IllegalArgumentException(
				"Measurement is not appropriate for a cell: " + m.toString());
	}

	/**
	 * Calculate a measurement for a component. If the measurement type is not valid
	 * for the component an IllegalArgumnentException will be thrown
	 * 
	 * @param m the measurement to make
	 * @param c the component to measure
	 * @return
	 * @throws MissingDataException
	 * @throws ComponentCreationException
	 * @throws SegmentUpdateException
	 */
	public static double calculate(@NonNull Measurement m, @NonNull CellularComponent c)
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {
		if (Measurement.CIRCULARITY.equals(m))
			return calculateCircularity(c);

		if (Measurement.PERIMETER.equals(m)) {
			return calculatePerimeter(c);
		}

		if (Measurement.AREA.equals(m)) {
			return calculateArea(c);
		}

		if (Measurement.RADIUS.equals(m))
			return calculateRadius(c);

		if (!(c instanceof Taggable))
			throw new IllegalArgumentException(
					"Measurement is not appropriate for a taggable component: '%s'"
							.formatted(m.toString()));

		Taggable t = (Taggable) c;

		if (Measurement.MIN_DIAMETER.equals(m))
			return calculateMinDiameter(t);

		if (!(t instanceof Nucleus))
			throw new IllegalArgumentException(
					"Measurement is not appropriate for a nucleus: '%s'"
							.formatted(m.toString()));

		Nucleus n = (Nucleus) t;

		if (Measurement.ELLIPTICITY.equals(m))
			return calculateEllipticity(n);

		if (Measurement.ASPECT.equals(m))
			return calculateAspect(n);

		if (Measurement.ELONGATION.equals(m))
			return calculateElongation(n);

		if (Measurement.REGULARITY.equals(m))
			return calculateRegularity(n);

		if (Measurement.BOUNDING_HEIGHT.equals(m))
			return n.getOrientedNucleus().getHeight();

		if (Measurement.BOUNDING_WIDTH.equals(m))
			return n.getOrientedNucleus().getWidth();

		if (Measurement.BODY_WIDTH.equals(m))
			return calculateBodyWidth(n);

		if (Measurement.HOOK_LENGTH.equals(m))
			return calculateHookLength(n);

		if (Measurement.VARIABILITY.equals(m))
			return -1; // calculated at the dataset level, not per cell

		throw new IllegalArgumentException(
				"Measurement is not appropriate for a cellular component: '%s'"
						.formatted(m.toString()));
	}

	/**
	 * Go around the border of the object, measuring the angle to the OP. If the
	 * angle is closest to target angle, return the distance to the CoM.
	 * 
	 * @param t     the object to measure
	 * @param angle the target angle
	 * @return the distance from the closest border point at the requested angle to
	 *         the CoM
	 * @throws UnavailableBorderPointException
	 */
	public static IPoint getDistanceFromCoMToBorderAtAngle(Taggable t, double angle)
			throws UnavailableBorderPointException {

		double bestDiff = Double.MAX_VALUE;
		IPoint com = t.getCentreOfMass();
		IPoint ref = new FloatPoint(com.getX(), com.getY() - 10);

		IPoint bestPoint = com;

		for (int i = 0; i < t.getBorderLength(); i++) {
			IPoint p = t.getBorderPoint(i);
			double pAngle = com.findAbsoluteAngle(ref, p);

			if (Math.abs(angle - pAngle) < bestDiff) {
				bestDiff = Math.abs(angle - pAngle);
				bestPoint = p;

			}
		}
		return bestPoint;

	}

	private static double calculateCellNuclearArea(ICell c)
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {
		double i = 0;
		for (Nucleus n : c.getNuclei()) {
			i += n.getMeasurement(Measurement.AREA);
		}
		return i;
	}

	private static double calculateCellNuclearRatio(ICell c)
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {
		if (c.hasCytoplasm()) {
			double cy = c.getCytoplasm().getMeasurement(Measurement.AREA);
			double n = c.getMeasurement(Measurement.CELL_NUCLEAR_AREA);
			return n / cy;
		}
		throw new MissingMeasurementException("Cell does not have cytoplasm");
	}

	/**
	 * Calculate the circularity of an object
	 * 
	 * @return
	 * @throws SegmentUpdateException
	 * @throws ComponentCreationException
	 * @throws MissingDataException
	 */
	private static double calculateCircularity(@NonNull final CellularComponent c)
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {
		double p = c.getMeasurement(Measurement.PERIMETER);
		double a = c.getMeasurement(Measurement.AREA);
		return (Math.PI * 4 * a) / (p * p);
	}

	/**
	 * Calculate the radius of an object if it were a circle with the same area
	 * 
	 * @return
	 * @throws SegmentUpdateException
	 * @throws ComponentCreationException
	 * @throws MissingDataException
	 */
	private static double calculateRadius(@NonNull final CellularComponent c)
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {
		return Math.sqrt(c.getMeasurement(Measurement.AREA) / Math.PI);
	}

	private static double calculateMinDiameter(@NonNull final Taggable c)
			throws MissingDataException, SegmentUpdateException {
		return Arrays.stream(c.getProfile(ProfileType.DIAMETER).toDoubleArray()).min()
				.getAsDouble();
	}

	private static double calculateMaximumDiameter(@NonNull final Taggable c)
			throws MissingDataException, SegmentUpdateException {
		return Arrays.stream(c.getProfile(ProfileType.DIAMETER).toDoubleArray()).max()
				.getAsDouble();
	}

	/**
	 * Calculate the perimeter of the shape based on the border list.
	 * 
	 * @param c the component to measure
	 * @return the perimeter of the component
	 * @throws UnavailableBorderPointException
	 */
	private static double calculatePerimeter(@NonNull final CellularComponent c)
			throws UnavailableBorderPointException {
		double perimeter = 0;

		IPoint p0 = c.getBorderPoint(0);
		IPoint prev = p0;
		for (IPoint p : c.getBorderList()) {
			perimeter += prev.getLengthTo(p);
			prev = p;
		}
		return perimeter + prev.getLengthTo(p0);
	}

	/**
	 * Calculate the area of an object.
	 * 
	 * @param c the component to measure
	 * @return the area of the component
	 * @throws MissingMeasurementException
	 */
	private static double calculateArea(@NonNull final CellularComponent c)
			throws MissingMeasurementException {
		Roi r = c.toOriginalRoi();
		if (r == null)
			throw new MissingMeasurementException("Could not convert component to roi");
		return Stats.area(c.toOriginalRoi());
	}

	/**
	 * Calculate the elongation of the object
	 * 
	 * @return
	 * @throws MissingLandmarkException
	 * @throws ComponentCreationException
	 */
	private static double calculateElongation(@NonNull final Nucleus n)
			throws MissingLandmarkException, ComponentCreationException {
		double h = n.getOrientedNucleus().getHeight();
		double w = n.getOrientedNucleus().getWidth();
		return (h - w) / (h + w);
	}

	/**
	 * Calculate the regularity of the object
	 * 
	 * @return
	 * @throws ComponentCreationException
	 * @throws SegmentUpdateException
	 * @throws MissingDataException
	 */
	private static double calculateRegularity(@NonNull final Nucleus n)
			throws ComponentCreationException,
			MissingDataException, SegmentUpdateException {
		double h = n.getOrientedNucleus().getHeight();
		double w = n.getOrientedNucleus().getWidth();
		double a = n.getMeasurement(Measurement.AREA);

		return (Math.PI * h * w) / (4 * a);
	}

	/**
	 * Calculate the aspect of the object
	 * 
	 * @return
	 * @throws MissingLandmarkException
	 * @throws ComponentCreationException
	 */
	private static double calculateAspect(@NonNull final Nucleus n)
			throws MissingLandmarkException, ComponentCreationException {
		return 1d / calculateEllipticity(n);
	}

	/**
	 * Calculate the ellipticity of the object
	 * 
	 * @return
	 * @throws MissingLandmarkException
	 * @throws ComponentCreationException
	 */
	private static double calculateEllipticity(@NonNull final Nucleus n)
			throws MissingLandmarkException, ComponentCreationException {
		double h = n.getOrientedNucleus().getHeight();
		double w = n.getOrientedNucleus().getWidth();
		return h / w;
	}

	/**
	 * Given a test nucleus, determine the hook length. Uses the oriented nucleus.
	 * 
	 * @param n
	 * @throws ComponentCreationException
	 */
	private static double calculateHookLength(@NonNull Nucleus n)
			throws ComponentCreationException, MissingLandmarkException {

		if (!n.hasLandmark(OrientationMark.TOP)) {
			throw new MissingLandmarkException(
					"Missing required OrientationMark TOP when calculating hook length");
		}

		if (!n.hasLandmark(OrientationMark.Y)) {
			throw new MissingLandmarkException(
					"Missing required OrientationMark Y when calculating hook length");
		}

		// Get the oriented nucleus
		Nucleus t = n.getOrientedNucleus();
		double vertX = t.getBorderPoint(OrientationMark.TOP).getX();

		/* Find the x values in the bounding box of the vertical nucleus. */
		FloatPolygon p = t.toPolygon();
		double minX = p.getBounds().getMinX();

		return vertX - minX;
	}

	/**
	 * Given a test nucleus, determine the body width. Uses the oriented nucleus.
	 * 
	 * @param n
	 * @throws ComponentCreationException
	 */
	private static double calculateBodyWidth(@NonNull Nucleus n)
			throws ComponentCreationException, MissingLandmarkException {

		if (!n.hasLandmark(OrientationMark.TOP) || !n.hasLandmark(OrientationMark.TOP)
				|| !n.hasLandmark(OrientationMark.Y)) {
			throw new MissingLandmarkException(
					"Missing required landmark when calculating body width");
		}

		// Get the oriented nucleus
		Nucleus t = n.getOrientedNucleus();
		double vertX = t.getBorderPoint(OrientationMark.TOP).getX();

		/* Find the x values in the bounding box of the vertical nucleus. */
		FloatPolygon p = t.toPolygon();
		double maxX = p.getBounds().getMaxX();

		return maxX - vertX;

	}

}
