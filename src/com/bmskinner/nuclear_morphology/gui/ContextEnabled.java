package com.bmskinner.nuclear_morphology.gui;

import java.util.Collection;

/**
 * Interface for components that have their enabled state driven 
 * by the number and type of selected objects
 * @author bms41
 * @since 1.14.0
 *
 */
public interface ContextEnabled {
	
	int ACTIVE_ON_ROOT_DATASET  = 1;
	int ACTIVE_ON_CHILD_DATASET = 2;
	int ACTIVE_ON_CLUSTER_GROUP = 4;
	int ACTIVE_ON_WORKSPACE     = 8;
	int ACTIVE_ON_SINGLE_OBJECT = 16;
	int ACTIVE_ON_MULTI_OBJECTS = 32;
	
	 /**
     * Tell the menu items to update their state based on the selected items
     * @param nItems the number of selected items
     */
	void updateSelectionContext(Collection<Object> objects);
	
//	void updateSelectionContext(ActiveTypeContext type);
		
	/**
     * Track when a menu item should be active.
     * Objects can be datasets, cluster groups, workspaces,
     * or anything else in the populations menu
     * @author bms41
     * @since 1.14.0
     *
     */
//    public enum ActiveCountContext {
//    	SINGLE_OBJECT_ONLY,
//    	MULTIPLE_OBJECTS_ONLY,
//    	SINGLE_AND_MULTIPLE_OBJECTS
//    }
//    
//    public enum ActiveTypeContext {
//    	DATASET,
//    	CLUSTER_GROUP,
//    	WORKSPACE
//    }
}
