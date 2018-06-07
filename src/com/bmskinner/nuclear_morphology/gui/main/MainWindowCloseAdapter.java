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


package com.bmskinner.nuclear_morphology.gui.main;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Handler;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.actions.SaveDatasetAction;
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
            Object[] options = { "Save datasets", "Exit without saving", "Cancel exit" };
            int save = JOptionPane.showOptionDialog(null, "Datasets have changed since last save!", "Save datasets?",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

            if (save == 0) {
                saveAndClose();

            }

            if (save == 1) {
                fine("Exiting without save");
                close();
            }

            if (save == 2) {
                fine("Ignoring close");
            }
        } else {
            fine("No change found");
            close();
        }
    }

    public void windowClosed(WindowEvent e) {
        close();
    }

    public void close() {
        DatasetListManager.getInstance().clear();
        GlobalOptions.getInstance().setDefaults();

        for (Handler h : Logger.getLogger(Loggable.ERROR_LOGGER).getHandlers()) {
            h.close();
        }

        mw.dispose();
        if (mw.isStandalone()) {
            System.exit(0);
        }
    }

    /**
     * Save the root datasets, then dispose the frame
     */
    private void saveAndClose() {
        Runnable r = () -> {
            for (IAnalysisDataset root : DatasetListManager.getInstance().getRootDatasets()) {
                final CountDownLatch latch = new CountDownLatch(1);

                Runnable task = new SaveDatasetAction(root, mw.getProgressAcceptor(), mw.getEventHandler(), latch, false);
                task.run();
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    error("Interruption to thread", e);
                }
            }
            log("All root datasets saved");
            close();
        };

        ThreadManager.getInstance().execute(r);
    }

}
