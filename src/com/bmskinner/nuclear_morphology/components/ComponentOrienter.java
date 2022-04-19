package com.bmskinner.nuclear_morphology.components;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.Orientable.FlipState;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.cells.Nucleus;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.rules.OrientationMark;
import com.bmskinner.nuclear_morphology.components.rules.PriorityAxis;

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
	 * @param t
	 * @throws MissingLandmarkException
	 */
	public static FlipState orient(@NonNull Nucleus t) throws MissingLandmarkException {
		// Use the points defined in the RuleSetCollection
		// to determine how to orient the nucleus
		if (PriorityAxis.Y.equals(t.getPriorityAxis())) {
			return alignVerticallyPriorityY(t);

		}
		// Same logic but now if X axis is the priority
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
		// Same logic but now if X axis is the priority
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
		// Test on a copy, so we don't affect the actual nucleus
		// by rotating
		Nucleus test = n.duplicate();
		test.rotate(calcAngleToAlignVertically(test));
		if (PriorityAxis.Y.equals(n.getPriorityAxis()))
			return isHorizontalFlipNeeded(test);

		// Same logic but now if X axis is the priority
		return isVerticalFlipNeeded(test);
	}

	/**
	 * Calculate the angle required to orient the nucleus vertically according to
	 * the inbuilt rules with X axis priority.
	 * 
	 * @param n the nucleus to orient
	 * @return
	 * @throws MissingLandmarkException
	 */
	private static double calcAngleToAlignVerticalPriorityX(@NonNull Nucleus n) throws MissingLandmarkException {
		;
		Landmark r = n.getLandmark(OrientationMark.RIGHT);
		Landmark l = n.getLandmark(OrientationMark.LEFT);
		Landmark x = n.getLandmark(OrientationMark.X);
		double angle = 0;
		// Check if l and r are present
		if (l != null && n.hasLandmark(l) && r != null && n.hasLandmark(r)) {
			IPoint leftPoint = n.getBorderPoint(l);
			IPoint rightPoint = n.getBorderPoint(r);

			if (leftPoint != rightPoint) {
				angle = Rotatable.getAngleToRotateHorizontal(leftPoint, rightPoint);
			} else if (x != null && n.hasLandmark(x)) { // if no l and r, fall back to x
				angle = Rotatable.getAngleToRotateHorizontal(n.getBorderPoint(x), n.getCentreOfMass());
			}
		} else if (x != null && n.hasLandmark(x)) { // if no l and r, fall back to x
			angle = Rotatable.getAngleToRotateHorizontal(n.getBorderPoint(x), n.getCentreOfMass());
		}
		return angle;
	}

	/**
	 * Calculate the angle required to orient the nucleus vertically according to
	 * the inbuilt rules with X axis priority.
	 * 
	 * @param n the nucleus to orient
	 * @return
	 * @throws MissingLandmarkException
	 */
	private static double calcAngleToAlignVerticalPriorityY(@NonNull Nucleus n) throws MissingLandmarkException {
		Landmark t = n.getLandmark(OrientationMark.TOP);
		Landmark b = n.getLandmark(OrientationMark.BOTTOM);
		Landmark y = n.getLandmark(OrientationMark.Y);

		double angle = 0;

		if (t != null && n.hasLandmark(t) && b != null && n.hasLandmark(b)) {
			IPoint topPoint = n.getBorderPoint(t);
			IPoint bottomPoint = n.getBorderPoint(b);
			if (topPoint != bottomPoint) {
				angle = Rotatable.getAngleToRotateVertical(topPoint, bottomPoint);
			} else if (y != null && n.hasLandmark(y)) {
				angle = Rotatable.getAngleToRotateVertical(n.getCentreOfMass(), n.getBorderPoint(y));
			}
		} else if (y != null && n.hasLandmark(y)) { // if no t and b, fall back to y
			angle = Rotatable.getAngleToRotateVertical(n.getCentreOfMass(), n.getBorderPoint(y));
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
		Landmark r = n.getLandmark(OrientationMark.RIGHT);
		Landmark l = n.getLandmark(OrientationMark.LEFT);
		Landmark x = n.getLandmark(OrientationMark.X);

		boolean shouldFlip = false;
		if (l != null && n.hasLandmark(l) && r != null && n.hasLandmark(r)) {
			IPoint leftPoint = n.getBorderPoint(l);
			IPoint rightPoint = n.getBorderPoint(r);
			if (leftPoint.isRightOf(rightPoint)) {
				shouldFlip = true;
			}
		} else if (l != null && n.hasLandmark(l)) {
			IPoint leftPoint = n.getBorderPoint(l);
			if (leftPoint.isRightOf(n.getCentreOfMass()))
				shouldFlip = true;
		} else if (r != null && n.hasLandmark(r)) {
			IPoint rightPoint = n.getBorderPoint(r);
			if (rightPoint.isLeftOf(n.getCentreOfMass()))
				shouldFlip = true;
		} else if (x != null && n.hasLandmark(x)) {
			IPoint leftPoint = n.getBorderPoint(x);
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
		Landmark t = n.getLandmark(OrientationMark.TOP);
		Landmark b = n.getLandmark(OrientationMark.BOTTOM);
		Landmark y = n.getLandmark(OrientationMark.Y);

		boolean shouldFlip = false;

		if (t != null && n.hasLandmark(t) && b != null && n.hasLandmark(b)) {
			IPoint topPoint = n.getBorderPoint(t);
			IPoint bottomPoint = n.getBorderPoint(b);
			if (topPoint.isBelow(bottomPoint)) {
				shouldFlip = true;
			}
		} else if (t != null && n.hasLandmark(t)) {
			IPoint topPoint = n.getBorderPoint(t);
			if (topPoint.isBelow(n.getCentreOfMass()))
				shouldFlip = true;
		} else if (b != null && n.hasLandmark(b)) {
			IPoint bottomPoint = n.getBorderPoint(b);
			if (bottomPoint.isAbove(n.getCentreOfMass()))
				shouldFlip = true;
		} else if (y != null && n.hasLandmark(y)) {
			IPoint bottomPoint = n.getBorderPoint(y);
			if (bottomPoint.isAbove(n.getCentreOfMass()))
				shouldFlip = true;
		}
		return shouldFlip;
	}

	/**
	 * Align the nucleus according to the available orientation points, prioritising
	 * the X axis
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
	 * the Y axis
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
