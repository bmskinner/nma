/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.analysis;

import java.beans.PropertyChangeListener;
import java.util.concurrent.RunnableFuture;

import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Describes the methods required for a worker. An analysis worker will contain
 * a method for an analysis, and will communicate progress through the method to
 * the UI via PropertyChangeEvents. Particular states are also specified by the
 * int codes in this interface - e.g. errors, finished, or a switch of the
 * pregress bar to an indeterminate state.
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public interface IAnalysisWorker extends RunnableFuture<IAnalysisResult>, ProgressListener, Loggable {

	String FINISHED_MSG = "Finished";
	String ERROR_MSG    = "Error";
	String INDETERMINATE_MSG    = "Cooldown";
	
    int FINISHED = -1; // signal cleanup of progress bar

    int ERROR = -2; // signal error occurred in analysis

    int INDETERMINATE = -3; // signal switch to indeterminate bar

    /**
     * Add a listener for changes to the analysis progress.
     * 
     * @param l the listener
     */
    void addPropertyChangeListener(PropertyChangeListener l);

    /**
     * Remove a listener for changes to the analysis progress.
     * 
     * @param l the listener
     */
    void removePropertyChangeListener(PropertyChangeListener l);

}
