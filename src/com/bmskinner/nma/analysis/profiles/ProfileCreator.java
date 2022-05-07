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

import com.bmskinner.nma.components.MissingLandmarkException;
import com.bmskinner.nma.components.Taggable;
import com.bmskinner.nma.components.UnavailableBorderPointException;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.measure.DoubleEquation;
import com.bmskinner.nma.components.measure.LineEquation;
import com.bmskinner.nma.components.profiles.DefaultProfile;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.stats.Stats;
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
	 * @throws ProfileException
	 */
	public static IProfile createProfile(@NonNull Taggable target, @NonNull ProfileType type)
			throws ProfileException {
		try {
			switch (type) {
			case ANGLE:
				return calculateAngleProfile(target);
			case DIAMETER:
				return calculateDiameterProfile(target);
			case RADIUS:
				return calculateRadiusProfile(target);
			case ZAHN_ROSKIES:
				return calculateZahnRoskiesProfile(target);
			default:
				return calculateAngleProfile(target);
			}
		} catch (UnavailableBorderPointException e) {
			LOGGER.warning("Cannot create profile " + e.getMessage());
			throw new ProfileException("Cannot create profile " + type, e);
		}
	}

	/**
	 * Calculate an angle profile for the given object. The profile starts at the
	 * first border index in the object
	 * 
	 * @param target
	 * @return
	 * @throws UnavailableBorderPointException
	 * @throws ProfileException
	 */
	private static IProfile calculateAngleProfile(@NonNull Taggable target)
			throws UnavailableBorderPointException, ProfileException {

		float[] angles = new float[target.getBorderLength()];

		Shape s = target.toShape();

		List<IPoint> borderList = target.getBorderList();

		if (borderList == null)
			throw new UnavailableBorderPointException("Null border list in target");

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
		int index = 0;

		Iterator<IPoint> it = target.getBorderList().iterator();
		while (it.hasNext()) {

			IPoint point = it.next();

			IPoint prev = target.getBorderList()
					.get(CellularComponent.wrapIndex(index + 1, target.getBorderLength()));
			IPoint next = target.getBorderList()
					.get(CellularComponent.wrapIndex(index - 1, target.getBorderLength()));

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

			profile[index++] = (float) angle;

		}

		// invert if needed

		if (profile[0] < 0) {
			for (int i = 0; i < profile.length; i++) {
				profile[i] = 0 - profile[i];
			}
		}

		// Make a new profile. If possible, use the internal segmentation type of the
		// component
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

		// Normalise to the the max diameter
		double max = Stats.max(profile);
		IProfile p = new DefaultProfile(profile);

		return new DefaultProfile(p.divide(max));
	}

	private static IProfile calculateRadiusProfile(@NonNull Taggable target)
			throws ProfileException {

		float[] profile = new float[target.getBorderLength()];

		int index = 0;
		Iterator<IPoint> it = target.getBorderList().iterator();
		while (it.hasNext()) {

			IPoint point = it.next();
			profile[index++] = (float) point.getLengthTo(target.getCentreOfMass());

		}

		// Normalise to the the max diameter
		double max = Stats.max(profile);
		IProfile p = new DefaultProfile(profile);
		return new DefaultProfile(p.divide(max));
	}

	/**
	 * Calculate the distance between points separated by the window size
	 * 
	 * @return
	 * @throws UnavailableBorderPointException
	 * @throws ProfileException
	 */
	public static IProfile calculatePointToPointDistanceProfile(@NonNull Taggable target)
			throws UnavailableBorderPointException {
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
