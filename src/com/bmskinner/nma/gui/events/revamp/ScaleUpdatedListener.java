package com.bmskinner.nma.gui.events.revamp;

import java.util.List;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

/**
 * Notify the listener that scale has changed in the given datasets.
 * 
 * @author bs19022
 *
 */
public interface ScaleUpdatedListener {
	/**
	 * Inform the listener that the display scale in the given datasets has changed
	 * 
	 * @param datasets
	 */
	void scaleUpdated(List<IAnalysisDataset> datasets);

	/**
	 * Inform the listener that the display scale in the given dataset have changed
	 * 
	 * @param datasets
	 */
	void scaleUpdated(IAnalysisDataset dataset);

	/**
	 * Inform the listener that the display scale globally has changed/ This occurs
	 * when switching from MeasurementScale.PIXEL to MeasurementScale.MICRONS and
	 * vice versa.
	 */
	void scaleUpdated();
}
