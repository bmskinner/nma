package com.bmskinner.nma.components;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.Orientable.FlipState;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.rules.PriorityAxis;

/**
 * Calculate rotation angles and flips needed source images
 * 
 * @author ben
 * @since 2.0.0
 *
 */
public class ComponentOrienter {

	private ComponentOrienter() { // not to be used
	}

	/**
	 * Orient the given nucleus using its orientation landmarks
	 * 
	 * @param OrientationMark.TOP
	 * @throws MissingLandmarkException
	 */
	public static FlipState orient(@NonNull Nucleus t) throws MissingLandmarkException {
		// Use the points defined in the RuleSetCollection
		// to determine how to orient the nucleus
		if (PriorityAxis.Y.equals(t.getPriorityAxis())) {
			return alignVerticallyPriorityY(t);

		}
		// Same logic but now if OrientationMark.X axis is the priority
		return alignVerticallyPriorityX(t);
	}

	/**
	 * Calculate the angle required to orient the nucleus vertically according to
	 * the inbuilt rules
	 * 
	 * @param n the nucleus to orient
	 * @return
	 * @throws MissingLandmarkException
	 */
	public static double calcAngleToAlignVertically(@NonNull Nucleus n) throws MissingLandmarkException {
		if (PriorityAxis.Y.equals(n.getPriorityAxis())) {
			return calcAngleToAlignVerticalPriorityY(n);
		}
		// Same logic but now if OrientationMark.X axis is the priority
		return calcAngleToAlignVerticalPriorityX(n) + 180;
	}

	/**
	 * Test if a nucleus needs to be horizontally or vertically flipped after
	 * rotation when making vertical. This should only be used when rotating other
	 * objects such as images to match a nucleus rotation; when rotating the nucleus
	 * itself, this is already handled internally.
	 * 
	 * @param n the nucleus to test
	 * @return true if flipping on the non-priority axis is needed, false otherwise
	 * @throws MissingLandmarkException
	 * @throws ComponentCreationException
	 */
	public static boolean isFlipNeeded(@NonNull Nucleus n) throws MissingLandmarkException, ComponentCreationException {
		// Test on a copy, so we don'OrientationMark.TOP affect the actual nucleus
		// by rotating
		Nucleus test = n.duplicate();
		test.rotate(calcAngleToAlignVertically(test));
		if (PriorityAxis.Y.equals(n.getPriorityAxis()))
			return isHorizontalFlipNeeded(test);

		// Same logic but now if OrientationMark.X axis is the priority
		return isVerticalFlipNeeded(test);
	}

	/**
	 * Calculate the angle required to orient the nucleus vertically according to
	 * the inbuilt rules with OrientationMark.X axis priority.
	 * 
	 * @param n the nucleus to orient
	 * @return
	 * @throws MissingLandmarkException
	 */
	private static double calcAngleToAlignVerticalPriorityX(@NonNull Nucleus n) throws MissingLandmarkException {

		double angle = 0;
		// Check if OrientationMark.LEFT and OrientationMark.RIGHT are present
		if (n.hasLandmark(OrientationMark.LEFT) && n.hasLandmark(OrientationMark.RIGHT)) {
			IPoint leftPoint = n.getBorderPoint(OrientationMark.LEFT);
			IPoint rightPoint = n.getBorderPoint(OrientationMark.RIGHT);

			if (leftPoint != rightPoint) {
				angle = Rotatable.getAngleToRotateHorizontal(leftPoint, rightPoint);
			} else if (n.hasLandmark(OrientationMark.X)) { // if no OrientationMark.LEFT and
															// OrientationMark.RIGHT, fall back to
															// OrientationMark.X
				angle = Rotatable.getAngleToRotateHorizontal(n.getBorderPoint(OrientationMark.X), n.getCentreOfMass());
			}
		} else if (n.hasLandmark(OrientationMark.X)) { // if no OrientationMark.LEFT and
														// OrientationMark.RIGHT, fall back to
														// OrientationMark.X
			angle = Rotatable.getAngleToRotateHorizontal(n.getBorderPoint(OrientationMark.X), n.getCentreOfMass());
		}
		return angle;
	}

	/**
	 * Calculate the angle required to orient the nucleus vertically according to
	 * the inbuilt rules with OrientationMark.X axis priority.
	 * 
	 * @param n the nucleus to orient
	 * @return
	 * @throws MissingLandmarkException
	 */
	private static double calcAngleToAlignVerticalPriorityY(@NonNull Nucleus n) throws MissingLandmarkException {

		double angle = 0;

		if (n.hasLandmark(OrientationMark.TOP) && n.hasLandmark(OrientationMark.BOTTOM)) {
			IPoint topPoint = n.getBorderPoint(OrientationMark.TOP);
			IPoint bottomPoint = n.getBorderPoint(OrientationMark.BOTTOM);
			if (topPoint != bottomPoint) {
				angle = Rotatable.getAngleToRotateVertical(topPoint, bottomPoint);
			} else if (n.hasLandmark(OrientationMark.Y)) {
				angle = Rotatable.getAngleToRotateVertical(n.getCentreOfMass(), n.getBorderPoint(OrientationMark.Y));
			}
		} else if (n.hasLandmark(OrientationMark.Y)) { // if no OrientationMark.TOP and OrientationMark.BOTTOM, fall
														// back to OrientationMark.Y
			angle = Rotatable.getAngleToRotateVertical(n.getCentreOfMass(), n.getBorderPoint(OrientationMark.Y));
		}
		return angle;
	}

	/**
	 * Test if a rotated nucleus needs to be flipped to match the orientation rules
	 * 
	 * @param n
	 * @return
	 * @throws MissingLandmarkException
	 */
	public static boolean isHorizontalFlipNeeded(@NonNull Nucleus n) throws MissingLandmarkException {
		boolean shouldFlip = false;
		if (n.hasLandmark(OrientationMark.LEFT) && n.hasLandmark(OrientationMark.RIGHT)) {
			IPoint leftPoint = n.getBorderPoint(OrientationMark.LEFT);
			IPoint rightPoint = n.getBorderPoint(OrientationMark.RIGHT);
			if (leftPoint.isRightOf(rightPoint)) {
				shouldFlip = true;
			}
		} else if (n.hasLandmark(OrientationMark.LEFT)) {
			IPoint leftPoint = n.getBorderPoint(OrientationMark.LEFT);
			if (leftPoint.isRightOf(n.getCentreOfMass()))
				shouldFlip = true;
		} else if (n.hasLandmark(OrientationMark.RIGHT)) {
			IPoint rightPoint = n.getBorderPoint(OrientationMark.RIGHT);
			if (rightPoint.isLeftOf(n.getCentreOfMass()))
				shouldFlip = true;
		} else if (n.hasLandmark(OrientationMark.X)) {
			IPoint leftPoint = n.getBorderPoint(OrientationMark.X);
			if (leftPoint.isRightOf(n.getCentreOfMass()))
				shouldFlip = true;
		}
		return shouldFlip;
	}

	/**
	 * Test if a rotated nucleus needs to be flipped to match the orientation rules
	 * 
	 * @param n
	 * @return
	 * @throws MissingLandmarkException
	 */
	public static boolean isVerticalFlipNeeded(@NonNull Nucleus n) throws MissingLandmarkException {
		boolean shouldFlip = false;

		if (n.hasLandmark(OrientationMark.TOP) && n.hasLandmark(OrientationMark.BOTTOM)) {
			IPoint topPoint = n.getBorderPoint(OrientationMark.TOP);
			IPoint bottomPoint = n.getBorderPoint(OrientationMark.BOTTOM);
			if (topPoint.isBelow(bottomPoint)) {
				shouldFlip = true;
			}
		} else if (n.hasLandmark(OrientationMark.TOP)) {
			IPoint topPoint = n.getBorderPoint(OrientationMark.TOP);
			if (topPoint.isBelow(n.getCentreOfMass()))
				shouldFlip = true;
		} else if (n.hasLandmark(OrientationMark.BOTTOM)) {
			IPoint bottomPoint = n.getBorderPoint(OrientationMark.BOTTOM);
			if (bottomPoint.isAbove(n.getCentreOfMass()))
				shouldFlip = true;
		} else if (n.hasLandmark(OrientationMark.Y)) {
			IPoint bottomPoint = n.getBorderPoint(OrientationMark.Y);
			if (bottomPoint.isAbove(n.getCentreOfMass()))
				shouldFlip = true;
		}
		return shouldFlip;
	}

	/**
	 * Align the nucleus according to the available orientation points, prioritising
	 * the OrientationMark.X axis
	 * 
	 * @throws MissingLandmarkException
	 */
	private static FlipState alignVerticallyPriorityX(@NonNull Nucleus n) throws MissingLandmarkException {
		double angle = calcAngleToAlignVerticalPriorityX(n);
		n.rotate(angle);

		if (isVerticalFlipNeeded(n)) {
			n.flipVertical();
			return new FlipState(false, true);
		}
		return new FlipState(false, false);
	}

	/**
	 * Align the nucleus according to the available orientation points, prioritising
	 * the OrientationMark.Y axis
	 * 
	 * @throws MissingLandmarkException
	 */
	private static FlipState alignVerticallyPriorityY(@NonNull Nucleus o) throws MissingLandmarkException {

		double angle = calcAngleToAlignVerticalPriorityY(o);
		o.rotate(angle);

		if (isHorizontalFlipNeeded(o)) {
			o.flipHorizontal();
			return new FlipState(true, false);
		}
		return new FlipState(false, false);
	}

}
