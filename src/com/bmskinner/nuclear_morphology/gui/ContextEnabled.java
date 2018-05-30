package com.bmskinner.nuclear_morphology.gui;

/**
 * Interface for components that have their enabled state driven 
 * by the number of selected objects
 * @author bms41
 * @since 1.14.0
 *
 */
public interface ContextEnabled {
	
	 /**
     * Tell the menu items to update their state based on the number of selected items
     * @param nItems the number of selected items
     */
	void updateSelectionContext(int nObjects);
	
	void updateSelectionContext(ActiveTypeContext type);
		
	/**
     * Track when a menu item should be active.
     * Objects can be datasets, cluster groups, workspaces,
     * or anything else in the populations menu
     * @author bms41
     * @since 1.14.0
     *
     */
    public enum ActiveCountContext {
    	SINGLE_OBJECT_ONLY,
    	MULTIPLE_OBJECTS_ONLY,
    	SINGLE_AND_MULTIPLE_OBJECTS
    }
    
    public enum ActiveTypeContext {
    	DATASET,
    	CLUSTER_GROUP,
    	WORKSPACE
    }

}
