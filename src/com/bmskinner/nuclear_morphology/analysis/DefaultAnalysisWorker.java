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

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

/**
 * The default implementation of IAnalysisWorker, using a SwingWorker.
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class DefaultAnalysisWorker extends SwingWorker<IAnalysisResult, Long> implements IAnalysisWorker {

    private long progressTotal;     // the maximum value for the progress bar
    private long progressCount = 0; // the current value for the progress bar

    protected IAnalysisMethod method;

    /**
     * Construct with a method. The progress bar total will be set to -1 - i.e.
     * the bar will remain indeterminate until the method completes
     * 
     * @param m
     *            the method to run
     */
    public DefaultAnalysisWorker(IAnalysisMethod m) {
        this(m, -1);
    }

    /**
     * Construct with a method and a total for the progress bar.
     * 
     * @param m
     *            the method to run
     * @param progress
     *            the length of the progress bar
     */
    public DefaultAnalysisWorker(IAnalysisMethod m, long progress) {
        method = m;
        method.addProgressListener(this);
        progressTotal = progress;
    }

    @Override
    protected IAnalysisResult doInBackground() throws Exception {

        // Set the bar
        fireIndeterminate();

        // do the analysis and wait for the result
        IAnalysisResult r = method.call();

        fireIndeterminate();
        return r;
    }

    @Override
    public void progressEventReceived(ProgressEvent event) {

        if(this.isCancelled()){
            method.cancel();
        }
        
        if (event.getMessage() == ProgressEvent.SET_TOTAL_PROGRESS) {
        	progressTotal = event.getValue();
        	return;
        }

        if (event.getMessage() == ProgressEvent.SET_INDETERMINATE) {
        	fireIndeterminate();
        	return;
        }

        if(event.getMessage()==ProgressEvent.INCREASE_BY_VALUE){
        	progressCount=event.getValue();
        } else {
        	progressCount++;
        }

        if (progressTotal >= 0) {
        	publish(progressCount);
        }


    }

    @Override
    protected void process(List<Long> integers) {
        long amount = integers.get(integers.size() - 1);
        int percent = (int) ((double) amount / (double) progressTotal * 100);
        System.out.println("Amount= "+amount+"; total="+progressTotal+"; Percent "+percent);
        if (percent >= 0 && percent <= 100) {
            setProgress(percent); // the integer representation of the percent
        }
    }

    private void fireIndeterminate() {
        firePropertyChange(IAnalysisWorker.INDETERMINATE_MSG, getProgress(), IAnalysisWorker.INDETERMINATE);
    }

    @Override
    public void done() {

//        fine("Worker completed task");

        try {

            if (this.get() != null) {
//                fine("Firing trigger for sucessful task");
                firePropertyChange(FINISHED_MSG, getProgress(), IAnalysisWorker.FINISHED);

            } else {
//                fine("Firing trigger for failed task");
                firePropertyChange(ERROR_MSG, getProgress(), IAnalysisWorker.ERROR);
            }

        } catch (StackOverflowError e) {
            warn("Stack overflow detected");
            stack("Stack overflow in worker", e);
            firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
        } catch (InterruptedException e) {
            warn("Interruption to swing worker: "+e.getMessage());
            stack("Interruption to swing worker", e);
            firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
        } catch (ExecutionException e) {
            warn("Execution error in swing worker: "+e.getMessage());
            stack("Execution error in swing worker", e);
            Throwable cause = e.getCause();
            warn("Causing error: "+cause.getMessage());
            stack("Causing error: ", cause);
            firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
        }

    }

}
