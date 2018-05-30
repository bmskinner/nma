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


package com.bmskinner.nuclear_morphology.gui.actions;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.CountDownLatch;

import javax.swing.JProgressBar;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisWorker;
import com.bmskinner.nuclear_morphology.gui.DatasetEventHandler;
import com.bmskinner.nuclear_morphology.gui.InterfaceEventHandler;
import com.bmskinner.nuclear_morphology.gui.LogPanel;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.main.EventHandler;

/**
 * The base of all progressible actions. Handles progress bars and workers
 * 
 * @author bms41
 * @since 1.13.6
 *
 */
public abstract class VoidResultAction implements PropertyChangeListener, Loggable, Runnable {

    private JProgressBar progressBar = null;

    protected IAnalysisWorker worker   = null;
    protected int downFlag = 0;   // flags for next action

    protected ProgressBarAcceptor logPanel;
    protected EventHandler eh;
    private CountDownLatch latch = null; // allow threads to wait
    private final DatasetEventHandler   dh = new DatasetEventHandler(this);
    private final InterfaceEventHandler ih = new InterfaceEventHandler(this);

    /**
     * Constructor
     * 
     * @param barMessage the message to display in the progress bar
     * @param mw the main window
     */
    protected VoidResultAction(@NonNull String barMessage, @NonNull ProgressBarAcceptor acceptor, @NonNull EventHandler eh) {
    	logPanel = acceptor;
    	
    	progressBar = new JProgressBar(0, 100);
        progressBar.setString(barMessage);
        progressBar.setStringPainted(true);
        progressBar.setIndeterminate(true);
        progressBar.addMouseListener( new MouseAdapter(){
            
        	@Override
        	public void mouseClicked(MouseEvent e) {
        		if (e.getSource() == progressBar && e.getClickCount() == 2) {
        			worker.cancel(true);
        			cleanup();
        		}
        	}
        });
        
        logPanel.addProgressBar(this.progressBar);
        this.eh = eh;
        ih.addInterfaceEventListener(eh);
        dh.addDatasetEventListener(eh);
    }
    
//    /**
//     * Constructor
//     * 
//     * @param barMessage the message to display in the progress bar
//     * @param mw the main window
//     */
//    protected VoidResultAction(@NonNull String barMessage, @NonNull MainWindow mw) {
//
//        progressBar = new JProgressBar(0, 100);
//        progressBar.setString(barMessage);
//        progressBar.setStringPainted(true);
//        progressBar.setIndeterminate(true);
//        progressBar.addMouseListener( new MouseAdapter(){
//            
//        	@Override
//        	public void mouseClicked(MouseEvent e) {
//        		if (e.getSource() == progressBar && e.getClickCount() == 2) {
//        			worker.cancel(true);
//        			cleanup();
//        		}
//        	}
//        });
//
//        this.mw = mw;
//        this.logPanel = mw.getLogPanel();
//
//        logPanel.addProgressBar(this.progressBar);
//        ih.addInterfaceEventListener(mw.getEventHandler());
//        dh.addDatasetEventListener(mw.getEventHandler());
//
//    }

    protected void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }

    protected void countdownLatch() {
        if (latch != null) {
            latch.countDown();
        }
    }

    /**
     * Change the progress message from the default in the constructor
     * 
     * @param messsage the string to display
     */
    public void setProgressMessage(String messsage) {
        this.progressBar.setString(messsage);
    }

    private void removeProgressBar() {
        logPanel.removeProgressBar(this.progressBar);
    }

    /**
     * Remove the progress bar and dataset and interface listeners
     */
    public void cancel() {
        removeProgressBar();
        dh.removeDatasetEventListener(eh);
        ih.removeInterfaceEventListener(eh);
    }

    protected void setProgressBarVisible(boolean b) {
        this.progressBar.setVisible(b);
    }

    /**
     * Use to manually remove the progress bar after an action is complete
     */
    public void cleanup() {
        if (this.worker.isDone() || this.worker.isCancelled()) {
            this.worker.removePropertyChangeListener(this);
            this.removeProgressBar();
        }
    }

    @Override
    public synchronized void propertyChange(PropertyChangeEvent evt) {

        int value = 0;

        Object newValue = evt.getNewValue();

        if (newValue instanceof Integer) {
            value = (int) newValue;
        }

        if (value >= 0 && value <= 100) {

            if (this.progressBar.isIndeterminate()) {
                this.progressBar.setIndeterminate(false);
            }
            this.progressBar.setValue(value);
        }

        if (evt.getPropertyName().equals(IAnalysisWorker.FINISHED_MSG)) {
            finished();
        }

        if (evt.getPropertyName().equals(IAnalysisWorker.ERROR_MSG)) {
            removeProgressBar();
        }

        if (evt.getPropertyName().equals(IAnalysisWorker.INDETERMINATE_MSG)) {
            setProgressBarIndeterminate();
        }

    }

    /**
     * The method run when the analysis has completed
     */
    public void finished() {
        this.worker.removePropertyChangeListener(this);
        cancel();

    }

    /**
     * Runs if a cooldown signal is received. Use to set progress bars to an
     * indeterminate state when no reliable progress metric is available
     */
    public void setProgressBarIndeterminate() {
        progressBar.setIndeterminate(true);
    }

    public synchronized boolean isDone() {
        return worker.isDone();
    }
    
    protected DatasetEventHandler getDatasetEventHandler(){
        return dh;
    }
    
    protected InterfaceEventHandler getInterfaceEventHandler(){
        return ih;
    }

}
