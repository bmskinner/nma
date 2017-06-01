package com.bmskinner.nuclear_morphology.gui;

/**
 * This is used to signal tab panels should update charts and tables to display
 * the datasets provided in the DatasetUpdateEvent.
 * 
 * @author ben
 *
 */
public interface DatasetUpdateEventListener {

    /**
     * Signal that the implementing class should respond to the given dataset
     * update request, and redraw all charts and tables appropriately
     * 
     * @param event
     *            the event to respond to
     */
    void datasetUpdateEventReceived(DatasetUpdateEvent event);
}
