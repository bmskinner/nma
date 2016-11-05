package gui;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import analysis.IAnalysisDataset;

/**
 * Send a list of datasets to registered listeners to 
 * draw charts and tables
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class DatasetUpdateEvent extends EventObject {

	private final List<IAnalysisDataset> list;

	/**
	 * Get the datasets in the event
	 * @return
	 */
	public List<IAnalysisDataset> getDatasets(){
		return list;
	}

	/**
	 * Construct from an existing event. Use to pass messages on.
	 * @param event
	 */
	public DatasetUpdateEvent(Object source, final List<IAnalysisDataset> list){
		super(source);
		this.list       = new ArrayList<IAnalysisDataset>(list);
	}
}
