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
package com.bmskinner.nuclear_morphology.gui.main;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Handler;
import java.util.logging.Logger;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.gui.actions.ExportDatasetAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExportWorkspaceAction;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * This window adapter is used to check if datasets have changed when the main
 * window is closed, and offers to save changed datasets before completing the
 * close.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class MainWindowCloseAdapter extends WindowAdapter implements Loggable {

    private MainView mw;

    public MainWindowCloseAdapter(MainView mw) {
        super();
        this.mw = mw;
    }

    @Override
    public void windowClosing(WindowEvent e) {
        fine("Checking dataset state");

        if (DatasetListManager.getInstance().hashCodeChanged()) {
            fine("Found changed hashcode");
            String[] options = { "Save and exit", "Exit without saving", "Do not exit" };

			try {
				int save = mw.getInputSupplier().requestOptionAllVisible(options, 0, "Datasets or workspaces have changed since last save!", "Save datasets and workspaces?");
				
				switch(save) {
	            	case 0: saveAndClose(); return;
	            	case 1: close(); return;
	            	case 2: return;
	            	default: return;
            }
				
			} catch (RequestCancelledException e1) {
				return;
			}
        }
		fine("No change found");
		close();
    }

    @Override
	public void windowClosed(WindowEvent e) {
        close();
    }

    public void close() {
        DatasetListManager.getInstance().clear();
        GlobalOptions.getInstance().setDefaults();

        for (Handler h : Logger.getLogger(Loggable.ROOT_LOGGER).getHandlers()) {
            h.close();
        }

        mw.dispose();
        if (mw.isStandalone())
            System.exit(0);
    }

    /**
     * Save the root datasets, then dispose the frame.
     * TODO: Rework to use the thread manager
     */
    private void saveAndClose() {
    	
    	final CountDownLatch latch = new CountDownLatch(1);
    	
    	// Run saves
    	Runnable r = () ->{
    		
    		for (IAnalysisDataset root : DatasetListManager.getInstance().getRootDatasets()) {
    			final CountDownLatch cl = new CountDownLatch(1);
    			Runnable task = new ExportDatasetAction(root, mw.getProgressAcceptor(), mw.getEventHandler(), cl, false, GlobalOptions.getInstance().getExportFormat());
//    			ThreadManager.getInstance().execute(task);
    			new Thread(task).start();
    			try {
    				cl.await();
    			} catch (InterruptedException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
    		}
    		log("All root datasets saved");

    		for (IWorkspace w : DatasetListManager.getInstance().getWorkspaces()) {
    			Runnable wrkTask = new ExportWorkspaceAction(w, mw.getProgressAcceptor(), mw.getEventHandler());
    			wrkTask.run();
    		}
    		log("All workspaces saved");
    		latch.countDown();
    	};

    	// Wait for saves to complete, then close the window
    	Runnable s = () ->{
    		try {
    			latch.await();
    		} catch (InterruptedException e) {
    			stack(e);
    		}
    		close();
    	};
    	
    	new Thread(r).start();
    	new Thread(s).start();
    }

}
