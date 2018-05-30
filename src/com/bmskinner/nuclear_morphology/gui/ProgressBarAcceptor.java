package com.bmskinner.nuclear_morphology.gui;

import java.util.List;

import javax.swing.JProgressBar;

/**
 * Defines components that can have a progress bar added to them
 * @author bms41
 * @since 1.14.0
 *
 */
public interface ProgressBarAcceptor {
	
	/**
	 * Add the given progress bar to the component
	 * @param bar the bar to add
	 */
	void addProgressBar(JProgressBar bar);
	
	/**
	 * Remove the given progress bar from the component
	 * @param bar the bar to remove
	 */
	void removeProgressBar(JProgressBar bar);
	
	/**
	 * Get all progress bars in this component
	 * @return the bars in the component
	 */
	List<JProgressBar> getProgressBars();

}
