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
public class DefaultAnalysisWorker extends SwingWorker<IAnalysisResult, Integer> implements IAnalysisWorker {

    private int progressTotal;     // the maximum value for the progress bar
    private int progressCount = 0; // the current value for the progress bar

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
    public DefaultAnalysisWorker(IAnalysisMethod m, int progress) {
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

        if (event.hasMessage()) {
            if (event.getMessage() == ProgressEvent.SET_TOTAL_PROGRESS) {
                progressTotal = event.getValue();
            }

            if (event.getMessage() == ProgressEvent.SET_INDETERMINATE) {
                fireIndeterminate();
            }

        } else {

            if (progressTotal >= 0) {
                publish(++progressCount);
            }
        }

    }

    @Override
    protected void process(List<Integer> integers) {
        int amount = integers.get(integers.size() - 1);
        int percent = (int) ((double) amount / (double) progressTotal * 100);

        if (percent >= 0 && percent <= 100) {
            setProgress(percent); // the integer representation of the percent
        }
    }

    private void fireIndeterminate() {
        firePropertyChange("Cooldown", getProgress(), IAnalysisWorker.INDETERMINATE);
    }

    @Override
    public void done() {

        fine("Worker completed task");

        try {

            if (this.get() != null) {
                fine("Firing trigger for sucessful task");
                firePropertyChange("Finished", getProgress(), IAnalysisWorker.FINISHED);

            } else {
                fine("Firing trigger for failed task");
                firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
            }

        } catch (StackOverflowError e) {
            warn("Stack overflow detected");
            stack("Stack overflow in worker", e);
            firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
        } catch (InterruptedException e) {
            warn("Interruption to swing worker");
            stack("Interruption to swing worker", e);
            firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
        } catch (ExecutionException e) {
            warn("Execution error in swing worker");
            stack("Execution error in swing worker", e);
            firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
        }

    }

}
