package com.bmskinner.nuclear_morphology.components;

import java.util.List;

import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.rules.OrientationMark;
import com.bmskinner.nuclear_morphology.components.rules.PriorityAxis;

/**
 * Objects that can be oriented via rules
 * @author ben
 * @since 2.0.0
 *
 */
public interface Orientable {
	
    /**
     * Get the given landmark for an orientation point
     * @param orientation
     * @return
     */
    @Nullable Landmark getLandmark(OrientationMark orientation);
    
    
    /**
     * Get the orientation marks currently set
     * in the object
     * @return
     */
    List<OrientationMark> getOrientationMarks();
	
    /**
     * Orient the object. This will use 
     * the landmarks and priorities defined by the rulesets
     * used when creating the nucleus. This affects this object
     * and does not create a copy.
     * @throws MissingLandmarkException 
     */
    void orient() throws MissingLandmarkException;
    
    /**
     * Get the axis of priority for orientation
     * @return
     */
    @Nullable PriorityAxis getPriorityAxis();

}
