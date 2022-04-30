package com.bmskinner.nma.gui.events;

import java.util.EventObject;
import java.util.UUID;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

public class SegmentUnmergeEvent extends EventObject {

	private static final long serialVersionUID = 1L;
	public final UUID id;
	public IAnalysisDataset dataset;

	/**
	 * Create an event from a source, with the given message
	 * 
	 * @param source  the source of the event
	 * @param dataset the dataset to merge within
	 * @param id1     a segment to be merged
	 * @param id2     a segment to be merged
	 */
	public SegmentUnmergeEvent(final Object source, final IAnalysisDataset dataset, final UUID id) {
		super(source);
		this.id = id;
		this.dataset = dataset;
	}

}
