package com.bmskinner.nuclear_morphology.gui.actions;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.nucleus.CellRelocationMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.components.FileSelector;

/**
 * Creates child datasets from a .cell mapping file
 * 
 * @author ben
 *
 */
public class RelocateFromFileAction extends SingleDatasetResultAction {

    private static final String PROGRESS_LBL = "Relocating cells";

    public RelocateFromFileAction(IAnalysisDataset dataset, MainWindow mw, CountDownLatch latch) {
        super(dataset, PROGRESS_LBL, mw);
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

            /*
             * Make the worker
             */
            IAnalysisMethod m = new CellRelocationMethod(dataset, file);
            worker = new DefaultAnalysisWorker(m);

            // worker = new CellRelocator(dataset, file);
            worker.addPropertyChangeListener(this);

            this.setProgressMessage("Locating cells...");
            ThreadManager.getInstance().submit(worker);
        } else {
            fine("Cancelled");
            cancel();
        }
    }

    @Override
    public void finished() {
        fine("Firing refresh of populations");
        fireInterfaceEvent(InterfaceMethod.REFRESH_POPULATIONS);
        this.countdownLatch();
        super.finished();
    }

}
