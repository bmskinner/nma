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
package com.bmskinner.nuclear_morphology.analysis;

import java.util.Arrays;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.Statistical;
import com.bmskinner.nuclear_morphology.components.Taggable;
import com.bmskinner.nuclear_morphology.components.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.generic.FloatPoint;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

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
	private ComponentMeasurer() {}

	/**
	 * Calculate a measurement for a component. If the measurement type
	 * is not valid for the component, Statistical.STAT_NOT_CALCULATED
	 * will be returned.
	 * @param m the measurement to make
	 * @param c the component to measure
	 * @return
	 */
	public static double calculate(Measurement m, CellularComponent c) {
		if (Measurement.CIRCULARITY.equals(m))
			return calculateCircularity(c);

		if (Measurement.PERIMETER.equals(m)) {
			return calculatePerimeter(c);
		}

		if (Measurement.AREA.equals(m)) {
			return calculateArea(c);
		}
		
		if(Measurement.RADIUS.equals(m))
			return calculateRadius(c);

		if(!(c instanceof Taggable))
			return Statistical.INVALID_OBJECT_TYPE;

		Taggable t = (Taggable)c;

		if (Measurement.MIN_DIAMETER.equals(m))
			return calculateMinDiameter(t);

		if(!(t instanceof Nucleus))
			return Statistical.INVALID_OBJECT_TYPE;

		Nucleus n = (Nucleus)t;
		
		try {

			if (Measurement.ELLIPTICITY.equals(m))
				return calculateEllipticity(n);

			if (Measurement.ASPECT.equals(m))
				return calculateAspect(n);

			if (Measurement.ELONGATION.equals(m))
				return calculateElongation(n);

			if (Measurement.REGULARITY.equals(m))
				return calculateRegularity(n);

			if (Measurement.BOUNDING_HEIGHT.equals(m))
				return  n.getOrientedNucleus().getHeight();

			if (Measurement.BOUNDING_WIDTH.equals(m))
				return  n.getOrientedNucleus().getWidth();
			
		} catch(MissingLandmarkException e) {
			LOGGER.log(Loggable.STACK, "Error getting consensus", e);
			return Statistical.ERROR_CALCULATING_STAT;
		}

		if (Measurement.BODY_WIDTH.equals(m))
			return calculateBodyWidth(n);

		if (Measurement.HOOK_LENGTH.equals(m))
			return calculateHookLength(n);

		return Statistical.STAT_NOT_CALCULATED;
	}

	/**
    * Go around the border of the object, measuring the angle to the OP. If the
    * angle is closest to target angle, return the distance to the CoM.
    * 
    * @param t the object to measure
    * @param angle the target angle
    * @return the distance from the closest border point at the requested angle
    *         to the CoM
    */
	public static double getDistanceFromCoMToBorderAtAngle(Taggable t, double angle) {

		double bestDiff = 180;
		double bestDistance = 180;

		try {
			for (int i = 0; i < t.getBorderLength(); i++) {
				IPoint p = t.getBorderPoint(i);
				double distance = p.getLengthTo(t.getCentreOfMass());
				double pAngle = t.getCentreOfMass().findSmallestAngle(p, new FloatPoint(0, -10));
				if (p.getX() < 0)
					pAngle = 360 - pAngle;

				if (Math.abs(angle - pAngle) < bestDiff) {
					bestDiff = Math.abs(angle - pAngle);
					bestDistance = distance;
				}
			}
		}catch(UnavailableBorderPointException e) {
			LOGGER.warning("Unable to caculate distance from CoM to border");
		}
		return bestDistance;
	}

	/**
	 * Calculate the circularity of an object
	 * @return
	 */
	private static double calculateCircularity(@NonNull final CellularComponent c) {
		if (c.hasMeasurement(Measurement.PERIMETER) && c.hasMeasurement(Measurement.AREA)) {
			double p = c.getMeasurement(Measurement.PERIMETER);
			double a = c.getMeasurement(Measurement.AREA);
			return (Math.PI*4*a)/(p*p);
		}
		return Statistical.ERROR_CALCULATING_STAT;
	}
	
	/**
	 * Calculate the radius of an object
	 * @return
	 */
	private static double calculateRadius(@NonNull final CellularComponent c) {
		return Math.sqrt(c.getMeasurement(Measurement.AREA) / Math.PI);
	}

	private static double calculateMinDiameter(@NonNull final Taggable c)  {
		try {
			return Arrays.stream(c.getProfile(ProfileType.DIAMETER).toDoubleArray())
					.min()
					.orElse(Statistical.ERROR_CALCULATING_STAT);
		} catch (MissingProfileException | ProfileException | MissingLandmarkException e) {
			LOGGER.log(Loggable.STACK, "Error getting diameter profile", e);
			return Statistical.ERROR_CALCULATING_STAT;
		}
	}

	private static double calculateMaximumDiameter(@NonNull final Taggable c)  {
		try {
			return Arrays.stream(c.getProfile(ProfileType.DIAMETER).toDoubleArray())
					.max()
					.orElse(Statistical.ERROR_CALCULATING_STAT);
		} catch (MissingProfileException | ProfileException | MissingLandmarkException e) {
			LOGGER.log(Loggable.STACK, "Error getting diameter profile", e);
			return Statistical.ERROR_CALCULATING_STAT;
		}
	}

	/**
	 * Calculate the perimeter of the shape based on the border list.
	 * 
	 * @param c the component to measure
	 * @return the perimeter of the component
	 */
	private static double calculatePerimeter(@NonNull final CellularComponent c) {
		double perimeter = 0;
		try {
			for(int i=0; i<c.getBorderLength(); i++) {
				perimeter += c.getBorderPoint(i)
						.getLengthTo(c.getBorderPoint(CellularComponent.wrapIndex(i+1, c.getBorderLength())));
			}
		} catch(UnavailableBorderPointException e) {
			LOGGER.log(Loggable.STACK, "Unable to calculate perimeter of object", e);
			return Statistical.ERROR_CALCULATING_STAT;
		}
		return perimeter;
	}

	/**
	 * Calculate the area of an object.
	 * 
	 * @param c the component to measure
	 * @return the area of the component
	 */
	private static double calculateArea(@NonNull final CellularComponent c) {
		return Stats.area(c.toShape());
	}



	/**
	 * Calculate the elongation of the object 
	 * @return
	 * @throws MissingLandmarkException 
	 */
	private static double calculateElongation(@NonNull final Nucleus n) throws MissingLandmarkException {
		double h = n.getOrientedNucleus().getHeight();
		double w = n.getOrientedNucleus().getWidth();
		return (h-w)/(h+w);
	}


	/**
	 * Calculate the regularity of the object 
	 * @return
	 * @throws MissingLandmarkException 
	 */
	private static double calculateRegularity(@NonNull final Nucleus n) throws MissingLandmarkException {
		double h = n.getOrientedNucleus().getHeight();
		double w = n.getOrientedNucleus().getWidth();
		double a = n.getMeasurement(Measurement.AREA);
		return (Math.PI*h*w)/(4*a);
	}

	/**
	 * Calculate the aspect of the object 
	 * @return
	 * @throws MissingLandmarkException 
	 */
	private static double calculateAspect(@NonNull final Nucleus n) throws MissingLandmarkException {
		return 1d/calculateEllipticity(n);
	}

	/**
	 * Calculate the ellipticity of the object 
	 * @return
	 * @throws MissingLandmarkException 
	 */
	private static double calculateEllipticity(@NonNull final Nucleus n) throws MissingLandmarkException {
		double h = n.getOrientedNucleus().getHeight();
		double w = n.getOrientedNucleus().getWidth();

		return h / w;
	}

	/**
	 * Given a test nucleus, determine the hook length. Uses the oriented
	 * nucleus.
	 * @param n
	 */
	private static double calculateHookLength(@NonNull Nucleus n) {

		if (!n.hasLandmark(Landmark.TOP_VERTICAL) || !n.hasLandmark(Landmark.BOTTOM_VERTICAL)) {
			return Statistical.MISSING_LANDMARK;
		}


		try {
			// Get the oriented nucleus
			Nucleus t = n.getOrientedNucleus();
			double vertX = t.getBorderPoint(Landmark.TOP_VERTICAL).getX();



			/* Find the x values in the bounding box of the vertical nucleus.  */
			FloatPolygon p = t.toPolygon();
			double minX = p.getBounds().getMinX();

			return vertX - minX;
		} catch (MissingLandmarkException e) {
			LOGGER.fine("Unable to calculate hook length: "+e.getMessage());
			return Statistical.ERROR_CALCULATING_STAT;
		}
	}

	/**
	 * Given a test nucleus, determine the body width. Uses the oriented
	 * nucleus.
	 * @param n
	 */
	private static double calculateBodyWidth(@NonNull Nucleus n) {

		if (!n.hasLandmark(Landmark.TOP_VERTICAL) || !n.hasLandmark(Landmark.BOTTOM_VERTICAL)) {
			return Statistical.MISSING_LANDMARK;
		}

		try {
			// Get the oriented nucleus
			Nucleus t = n.getOrientedNucleus();
			double vertX = t.getBorderPoint(Landmark.TOP_VERTICAL).getX();

			/* Find the x values in the bounding box of the vertical nucleus.  */
			FloatPolygon p = t.toPolygon();
			double maxX = p.getBounds().getMaxX();

			return maxX- vertX;
		} catch (MissingLandmarkException e) {
			LOGGER.fine("Unable to calculate body width: "+e.getMessage());
			return Statistical.ERROR_CALCULATING_STAT;
		}
	}

}
