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

import java.io.File;
import java.util.concurrent.CountDownLatch;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.nucleus.CellRelocationMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.components.FileSelector;
import com.bmskinner.nuclear_morphology.gui.main.MainWindow;
import com.bmskinner.nuclear_morphology.main.EventHandler;
import com.bmskinner.nuclear_morphology.main.ThreadManager;

/**
 * Creates child datasets from a .cell mapping file
 * 
 * @author ben
 *
 */
public class RelocateFromFileAction extends SingleDatasetResultAction {

    private static final String PROGRESS_LBL = "Relocating cells";

    public RelocateFromFileAction(IAnalysisDataset dataset, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh, CountDownLatch latch) {
        super(dataset, PROGRESS_LBL, acceptor, eh);
        this.setLatch(latch);
        setProgressBarIndeterminate();
    }

    @Override
    public void run() {
        /*
         * Get the file to search
         */

        File file = FileSelector.chooseRemappingFile(dataset);
        if (file != null) {

            IAnalysisMethod m = new CellRelocationMethod(dataset, file);
            worker = new DefaultAnalysisWorker(m);

            worker.addPropertyChangeListener(this);

            this.setProgressMessage("Locating cells...");
            ThreadManager.getInstance().submit(worker);
        } else {
            cancel();
        }
    }

    @Override
    public void finished() {
        getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.REFRESH_POPULATIONS);
        this.countdownLatch();
        super.finished();
    }

}
