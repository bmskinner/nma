package com.bmskinner.nma.gui.events;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.workspaces.IWorkspace;

public interface WorkspaceAddedListener {

	/**
	 * Inform the listener the given woskspace has been added to the global set
	 * 
	 * @param datasets
	 */
	void workspaceAdded(IWorkspace ws);

	/**
	 * Inform the listener the given workspace has been deleted
	 * 
	 * @param datasets
	 */
	void workspaceDeleted(IWorkspace ws);

	/**
	 * Inform the listener the given dataset has been added to the given workspace
	 * 
	 * @param ws
	 * @param d
	 */
	void datasetAdded(IWorkspace ws, IAnalysisDataset d);

	/**
	 * Inform the listener the given dataset has been removed from the given
	 * workspace
	 * 
	 * @param ws
	 * @param d
	 */
	void datasetRemoved(IWorkspace ws, IAnalysisDataset d);

}
