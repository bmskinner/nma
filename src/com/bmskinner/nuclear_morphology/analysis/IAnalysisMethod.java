package com.bmskinner.nuclear_morphology.analysis;

import java.util.concurrent.Callable;

import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Describes the basics of all analyses on datasets. A method has no interaction
 * with the UI - it just carries out the analysis. The method will fire a
 * ProgressEvent to listeners to communicate progress through the task. The
 * listener than can report this progress via a UI update. E.g. a
 * ProgressableAction contains a progress bar, updated by an IAnalysisWorker,
 * which contains the IAnalysisMethod.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public interface IAnalysisMethod extends Callable<IAnalysisResult>, Loggable {

    /**
     * Add a listener for progress through an analysis. Use e.g. to update
     * progress bars
     * 
     * @param l
     *            the listener
     */
    void addProgressListener(ProgressListener l);

    /**
     * Remove a progress listener if present
     * 
     * @param l
     *            the listener
     */
    void removeProgressListener(ProgressListener l);

}
