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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Stores the basic methods for an IAnalysisMethod. The logic for an analysis
 * should be written within the run() method. To signal progress through the task
 * use a {@code fireProgressEvent()}. This will tell registered {@code ProgressListener}s 
 * to update UI elements accordingly. Typically this will be an {@link IAnalysisWorker}. 
 * Calling {@code fireProgressEvent()} with no parameters indicates that the external worker
 * should increase the progress by one, where the worker is tracking the total amount of 'work'
 * in the task. Calling {@code fireProgressEvent(long l)} will tell the worker to update the progress
 * to the given value out of the total amount of 'work' in the task. 
 * 
 * This base class does not take an input dataset; it serves as the starting point for all 
 * pipelines
 * 
 * @author ben
 *
 */
public abstract class AbstractAnalysisMethod implements IAnalysisMethod, ProgressListener {

//    protected IAnalysisDataset dataset;
    private List<Object>       listeners = new ArrayList<Object>();
    protected IAnalysisResult  result    = null;

    public AbstractAnalysisMethod() {}
        
    @Override
    public void addProgressListener(ProgressListener l) {
        listeners.add(l);
    }

    @Override
    public void removeProgressListener(ProgressListener l) {
        listeners.remove(l);
    }
    
    @Override
    public IAnalysisMethod then(@NonNull IAnalysisMethod nextMethod) throws Exception {
    	call();
    	return nextMethod;
    }
    
    @Override
    public IAnalysisMethod thenIf(boolean b, @NonNull IAnalysisMethod nextMethod) throws Exception {
    	call();
    	if(b)	
    		return nextMethod;
		return this;
    }
    
    /**
     * Update the total number of steps in the task, and alert progress
     * listeners. For example, can set progress bar lengths 
     * @param totalProgress
     */
    protected void fireUpdateProgressTotalLength(int totalProgress) {
    	fireProgressEvent(new ProgressEvent(this, ProgressEvent.SET_TOTAL_PROGRESS, totalProgress));
    }
    
    /**
     * Alert progress listeners that the task length has become indeterminate
     */
    protected void fireIndeterminateState() {
    	fireProgressEvent(new ProgressEvent(this, ProgressEvent.SET_INDETERMINATE, 0));
    }

    /**
     * Fire a progress event to listeners, indicating one step of the task has been completed
     */
    protected void fireProgressEvent() {
        ProgressEvent e = new ProgressEvent(this);
        fireProgressEvent(e);
    }
    
    /**
     * Fire a progress event to listeners, indicating a total number of steps of the task 
     * have been completed
     * @param stepsNewlyCompleted the number of steps of the task completed
     */
    protected void fireProgressEvent(long stepsNewlyCompleted) {
        ProgressEvent e = new ProgressEvent(this, ProgressEvent.INCREASE_BY_VALUE, stepsNewlyCompleted);
        fireProgressEvent(e);
    }

    protected void fireProgressEvent(ProgressEvent e) {
        Iterator<Object> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            ((ProgressListener) iterator.next()).progressEventReceived(e);
        }
    }

    @Override
    public void progressEventReceived(ProgressEvent event) {
        fireProgressEvent(event); // pass upwards
    }
    
    /**
     * Fire a progress event, then sleep for the given number of milliseconds.
     * Repeat for the given number of steps.
     * @param total the total number of steps
     * @param millisToSleep the number of milliseconds to sleep between each step
     */
    protected void spinWheels(int total, int millisToSleep) {
        for (int i = 0; i < total; i++) {
            fireProgressEvent();
            try {
                Thread.sleep(millisToSleep);
            } catch (InterruptedException e) {
                error("Thread interrupted", e);
            }
        }
    }

}
