package com.bmskinner.nma.gui.events;

import java.util.EventObject;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

public class ProfileWindowProportionUpdateEvent extends EventObject {

	private static final long serialVersionUID = 1L;
	public final double window;
	public final IAnalysisDataset dataset;

	/**
	 * Create an event from a source, with the given message
	 * 
	 * @param source  the source of the event
	 * @param dataset the dataset to merge within
	 * @param id1     a segment to be merged
	 * @param id2     a segment to be merged
	 */
	public ProfileWindowProportionUpdateEvent(final Object source, final IAnalysisDataset dataset, final double ws) {
		super(source);
		this.window = ws;
		this.dataset = dataset;
	}

}
