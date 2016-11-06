package gui;

/**
 * This is used to signal tab panels should update charts and tables
 * to display the datasets provided in the DatasetUpdateEvent.
 * @author ben
 *
 */
public interface DatasetUpdateEventListener {
	
	void datasetUpdateEventReceived(DatasetUpdateEvent event);
}
