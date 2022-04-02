package com.bmskinner.nuclear_morphology.analysis;

import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.rules.OrientationMark;
import com.bmskinner.nuclear_morphology.components.rules.PriorityAxis;

/**
 * Calculate rotation angles and flips needed
 * source images
 * @author ben
 * @since 2.0.0
 *
 */
public class ComponentOrienter {
	
	private ComponentOrienter() { // not to be used
	}
	
	/**
	 * Orient the given nucleus using its orientation 
	 * landmarks
	 * @param t
	 * @throws MissingLandmarkException
	 */
	public static void orient(Nucleus t) throws MissingLandmarkException {
		// Use the points defined in the RuleSetCollection
    	// to determine how to orient the nucleus
    	if(PriorityAxis.Y.equals(t.getPriorityAxis())) {
    		alignVerticallyPriorityY(t);

    	} else {
    		// Same logic but now if X axis is the priority
    		alignVerticallyPriorityX(t);
    	}
	}
	
	 /**
     * Align the nucleus according to the available
     * orientation points, prioritising the X axis
     * @throws MissingLandmarkException
     */
    private static void alignVerticallyPriorityX(Nucleus n) throws MissingLandmarkException {
    	
    	Landmark t = n.getLandmark(OrientationMark.TOP);
    	Landmark b = n.getLandmark(OrientationMark.BOTTOM);
    	Landmark y = n.getLandmark(OrientationMark.Y);
    	Landmark r = n.getLandmark(OrientationMark.RIGHT);
    	Landmark l = n.getLandmark(OrientationMark.LEFT);
    	Landmark x = n.getLandmark(OrientationMark.X);
    	
    	// Check if l and r are present
		if(l!=null && n.hasLandmark(l) && r!=null && n.hasLandmark(r)) {
			IPoint leftPoint  = n.getBorderPoint(l);
			IPoint rightPoint = n.getBorderPoint(r);
			if(leftPoint != rightPoint) {
				n.alignPointsOnHorizontal(leftPoint, rightPoint);
			} else if(x!=null && n.hasLandmark(x)) { // if no l and r, fall back to x
				n.rotatePointToLeft(n.getBorderPoint(x));
			}
		} else if(x!=null && n.hasLandmark(x)) { // if no l and r, fall back to x
			n.rotatePointToLeft(n.getBorderPoint(x));
		}
		
		// Now check y, and flip as needed
		if(t!=null && n.hasLandmark(t) && b!=null && n.hasLandmark(b)) {
			IPoint topPoint  = n.getBorderPoint(t);
			IPoint bottomPoint = n.getBorderPoint(b);
			if(topPoint.isBelow(bottomPoint)) {
				n.flipVertical();
			}
		} else if(t!=null && n.hasLandmark(t)) {
			IPoint topPoint  = n.getBorderPoint(t);
			if(topPoint.isBelow(n.getCentreOfMass()))
				n.flipVertical();
		} else if(b!=null && n.hasLandmark(b)) {
			IPoint bottomPoint = n.getBorderPoint(b);
			if(bottomPoint.isAbove(n.getCentreOfMass()))
				n.flipVertical();
		} else if(y!=null && n.hasLandmark(y)) {
			IPoint bottomPoint = n.getBorderPoint(y);
			if(bottomPoint.isAbove(n.getCentreOfMass()))
				n.flipVertical();
		}
    }
	
	/**
     * Align the nucleus according to the available
     * orientation points, prioritising the Y axis
     * @throws MissingLandmarkException
     */
    private static void alignVerticallyPriorityY(Nucleus o) throws MissingLandmarkException {
    	Landmark t = o.getLandmark(OrientationMark.TOP);
    	Landmark b = o.getLandmark(OrientationMark.BOTTOM);
    	Landmark y = o.getLandmark(OrientationMark.Y);
    	Landmark r = o.getLandmark(OrientationMark.RIGHT);
    	Landmark l = o.getLandmark(OrientationMark.LEFT);
    	Landmark x = o.getLandmark(OrientationMark.X);

		if(t!=null && o.hasLandmark(t) && b!=null && o.hasLandmark(b)) {
			IPoint topPoint    = o.getBorderPoint(t);
			IPoint bottomPoint = o.getBorderPoint(b);
			if(topPoint != bottomPoint) {
				o.alignPointsOnVertical(topPoint, bottomPoint);
			} else if(y!=null && o.hasLandmark(y)) {
				o.rotatePointToBottom(o.getBorderPoint(y));
			}
		} else if(y!=null && o.hasLandmark(y)) { // if no t and b, fall back to y
			o.rotatePointToBottom(o.getBorderPoint(y));
		}
		
		// Now check x, and flip as needed
		if(l!=null && o.hasLandmark(l) && r!=null && o.hasLandmark(r)) {
			IPoint leftPoint  = o.getBorderPoint(l);
			IPoint rightPoint = o.getBorderPoint(r);
			if(leftPoint.isRightOf(rightPoint)) {
				o.flipHorizontal();
			}
		} else if(l!=null && o.hasLandmark(l)) {
			IPoint leftPoint  = o.getBorderPoint(l);
			if(leftPoint.isRightOf(o.getCentreOfMass()))
				o.flipHorizontal();
		} else if(r!=null && o.hasLandmark(r)) {
			IPoint rightPoint = o.getBorderPoint(r);
			if(rightPoint.isLeftOf(o.getCentreOfMass()))
				o.flipHorizontal();
		} else if(x!=null && o.hasLandmark(x)) {
			IPoint leftPoint = o.getBorderPoint(x);
			if(leftPoint.isRightOf(o.getCentreOfMass()))
				o.flipHorizontal();
		}
    }

}
