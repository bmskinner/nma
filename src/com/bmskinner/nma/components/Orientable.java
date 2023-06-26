package com.bmskinner.nma.components;

import java.util.List;

import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.components.profiles.Landmark;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.rules.PriorityAxis;

/**
 * Objects that can be oriented via rules
 * 
 * @author ben
 * @since 2.0.0
 *
 */
public interface Orientable {

	public record FlipState(boolean h, boolean v) {
	}

	/**
	 * Get the given landmark for an orientation point
	 * 
	 * @param orientation
	 * @return
	 */
	@Nullable
	Landmark getLandmark(OrientationMark orientation);

	/**
	 * Get the orientation marks currently set in the object
	 * 
	 * @return
	 */
	List<OrientationMark> getOrientationMarks();

	/**
	 * Get the landmarks currently set in the object
	 * 
	 * @return
	 */
	List<Landmark> getLandmarks();

	/**
	 * Orient the object. This will use the landmarks and priorities defined by the
	 * rulesets used when creating the nucleus. This affects this object and does
	 * not create a copy.
	 * 
	 * @throws MissingLandmarkException
	 */
	void orient() throws MissingLandmarkException;

	/**
	 * Get the axis of priority for orientation
	 * 
	 * @return
	 */
	@Nullable
	PriorityAxis getPriorityAxis();

}
