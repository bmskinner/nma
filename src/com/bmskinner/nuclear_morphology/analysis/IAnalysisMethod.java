package com.bmskinner.nuclear_morphology.analysis;

/**
 * Describes the basics of all analyses on datasets
 * @author ben
 * @since 1.13.3
 *
 */
public interface IAnalysisMethod {
	
	/**
	 * Begin the analysis 
	 */
	void run();
	
	/**
	 * Add a listener for progress through an analysis. 
	 * Use e.g. to update progress bars
	 * @param l the listener
	 */
	void addProgressListener( ProgressListener l);
	
	/**
	 * Remove a progress listener if present
	 * @param l the listener
	 */
	void removeProgressListener( ProgressListener l);

}
