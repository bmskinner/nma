/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.analysis;

import java.util.concurrent.Callable;

import org.eclipse.jdt.annotation.NonNull;

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
     * @param l the listener
     */
    void addProgressListener(ProgressListener l);

    /**
     * Remove a progress listener if present
     * 
     * @param l the listener
     */
    void removeProgressListener(ProgressListener l);
    
    
    /**
     * Call this method and specify a method to be run. It returns the same
     * method that is given, so methods can be chained. The final method in the 
     * chain must have {@code call()} invoked as normal
     * @param nextMethod the next method to be run.
     * @return the next method to be run
     * @throws Exception if an error occurs in the called method
     */
    IAnalysisMethod then(@NonNull IAnalysisMethod nextMethod) throws Exception;
    
    /**
     * Call this method and specify a method to be run if a condition is met. 
     * If the condition is true, it returns the same method that is given.
     * If  the condition is false, it returns this method.
     * @param nextMethod the next method to be run.
     * @return the next method to be run
     * @throws Exception if an error occurs in the called method
     */
    IAnalysisMethod thenIf(boolean condition, @NonNull IAnalysisMethod nextMethod) throws Exception;
    
    /**
     * Attempt to cancel the current task.
     * 
     */
    default void cancel(){
        Thread.currentThread().interrupt();
    }

}
