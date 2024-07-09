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
package com.bmskinner.nma.analysis.profiles;

import java.awt.Shape;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.ComponentMeasurer;
import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.Taggable;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.UnavailableBorderPointException;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.measure.DoubleEquation;
import com.bmskinner.nma.components.measure.LineEquation;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.profiles.DefaultProfile;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.utility.AngleTools;

/**
 * Performs angle and distance profiling on Taggable objects
 * 
 * @author bms41
 * @since 1.13.2
 *
 */
public class ProfileCreator {

	private static final Logger LOGGER = Logger.getLogger(ProfileCreator.class.getName());

	private ProfileCreator() {
	} // no constructor, static access only

	/**
	 * Create a profile for the desired profile type on the template object
	 * 
	 * @param type the profile type
	 * @return a profile of the requested type.
	 * @throws SegmentUpdateException
	 * @throws ComponentCreationException
	 * @throws MissingDataException
	 * @throws ProfileException
	 */
	public static IProfile createProfile(@NonNull Taggable target, @NonNull ProfileType type)
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {

		switch (type) {
		case ANGLE:
			return calculateAngleProfile(target);
		case DIAMETER:
			return calculateDiameterProfile(target);
		case RADIUS:
			return calculateRadiusProfile(target);
		default:
			return calculateAngleProfile(target);
		}
	}

	/**
	 * Calculate an angle profile for the given object. The profile starts at the
	 * first border index in the object
	 * 
	 * @param target
	 * @return
	 * @throws SegmentUpdateException
	 * @throws ComponentCreationException
	 * @throws MissingDataException
	 */
	private static IProfile calculateAngleProfile(@NonNull Taggable target)
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {

		float[] angles = new float[target.getBorderLength()];

		Shape s = target.toShape();

		List<IPoint> borderList = target.getBorderList();

		if (borderList == null)
			throw new UnavailableBorderPointException("Null border list in target");

		if (!target.hasMeasurement(Measurement.PERIMETER)) {
			double perimeter = ComponentMeasurer.calculate(Measurement.PERIMETER, target);
			target.setMeasurement(Measurement.PERIMETER, perimeter);
		}

		int windowSize = target.getWindowSize();

		for (int index = 0; index < borderList.size(); index++) {

			IPoint point = borderList.get(index);
			IPoint pointBefore = borderList.get(target.wrapIndex(index + windowSize));
			IPoint pointAfter = borderList.get(target.wrapIndex(index - windowSize));

			// Find the smallest angle between the points
			float angle = (float) point.findSmallestAngle(pointBefore, pointAfter);

			// Is the measured angle is inside or outside the object?
			// Take the midpoint between the before and after points.
			// If it is within the ROI, the angle is the interior angle
			// if no, 360-min is the interior angle
			float midX = (float) ((pointBefore.getX() + pointAfter.getX()) / 2);
			float midY = (float) ((pointBefore.getY() + pointAfter.getY()) / 2);
			angles[index] = s.contains(midX, midY) ? angle : 360 - angle;
		}

		return new DefaultProfile(angles);
	}

	/**
	 * Calculate a modified ZR profile. This uses the same window size as the angle
	 * profile, so is not a true ZR transform.
	 * 
	 * @return
	 * @throws ProfileException
	 * @throws UnavailableBorderPointException
	 * @throws MissingLandmarkException
	 */
	private static IProfile calculateZahnRoskiesProfile(@NonNull Taggable target) {

		float[] profile = new float[target.getBorderLength()];

		List<IPoint> border = target.getBorderList();

		for (int i = 0; i < border.size(); i++) {

			IPoint point = border.get(i);

			IPoint prev = border
					.get(CellularComponent.wrapIndex(i - 1, target.getBorderLength()));
			IPoint next = border
					.get(CellularComponent.wrapIndex(i + 1, target.getBorderLength()));

			if (point.equals(prev)) {
				profile[i] = 0;
				continue;
			}

			// Get the equation between the first two points
			LineEquation eq = new DoubleEquation(prev, point);

			// Move out along line
			IPoint p = eq.getPointOnLine(point, point.getLengthTo(prev));

			// Don't go the wrong way along the line
			if (p.getLengthTo(prev) < point.getLengthTo(prev))
				p = eq.getPointOnLine(point, -point.getLengthTo(prev));

			// Get the angle between the points
			double rad = AngleTools.angleBetweenLines(point, p, point, next);

			double angle = Math.toDegrees(rad);

			if (angle > 180)
				angle = -180 + (angle - 180);

			if (angle < -180)
				angle = 180 + (angle + 180);

			profile[i] = (float) angle;

		}

		// invert if needed
		if (profile[0] < 0) {
			for (int i = 0; i < profile.length; i++) {
				profile[i] = 0 - profile[i];
			}
		}

		// Make a new profile
		return new DefaultProfile(profile);
	}

	private static IProfile calculateDiameterProfile(@NonNull Taggable target)
			throws UnavailableBorderPointException {

		float[] profile = new float[target.getBorderLength()];

		List<IPoint> points = target.getBorderList();
		for (int index = 0; index < points.size(); index++) {
			IPoint point = points.get(index);
			IPoint opp = target.findOppositeBorder(point);
			profile[index] = (float) point.getLengthTo(opp);
		}

		return new DefaultProfile(profile);
	}

	private static IProfile calculateRadiusProfile(@NonNull Taggable target) {

		float[] profile = new float[target.getBorderLength()];

		int index = 0;
		Iterator<IPoint> it = target.getBorderList().iterator();
		while (it.hasNext()) {

			IPoint point = it.next();
			profile[index++] = (float) point.getLengthTo(target.getCentreOfMass());

		}

		return new DefaultProfile(profile);
	}

	/**
	 * Calculate the distance between points separated by the window size
	 * 
	 * @return
	 * @throws SegmentUpdateException
	 * @throws ComponentCreationException
	 * @throws MissingDataException
	 */
	public static IProfile calculatePointToPointDistanceProfile(@NonNull Taggable target)
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {
		float[] profile = new float[target.getBorderLength()];

		int index = 0;
		Iterator<IPoint> it = target.getBorderList().iterator();

		int pointOffset = target.getWindowSize();

		if (pointOffset == 0) {
			throw new UnavailableBorderPointException(
					"Window size has not been set in Profilable object");
		}

		while (it.hasNext()) {

			IPoint point = it.next();

			IPoint prev = target.getBorderList()
					.get(CellularComponent.wrapIndex(index + 1, target.getBorderLength()));
			double distance = point.getLengthTo(prev);

			profile[index] = (float) distance;
			index++;
		}
		return new DefaultProfile(profile);
	}

}
