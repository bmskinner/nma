package com.bmskinner.nuclear_morphology.components.profiles;

/**
 * The types of border tag that can be present
 * @author ben
 *
 */
public enum LandmarkType {
	
	/** Core border tags are essential for the software to display or calculate profiles */
    CORE, 
    /** Extended border tags are optional, and can be added as needed */
    EXTENDED
}