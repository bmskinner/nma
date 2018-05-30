package com.bmskinner.nuclear_morphology.gui.actions;

import java.io.File;
import java.util.concurrent.ExecutionException;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.ClusterAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.classification.ClusterFileAssignmentMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.components.FileSelector;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.dialogs.ClusteringSetupDialog;
import com.bmskinner.nuclear_morphology.gui.dialogs.SubAnalysisSetupDialog;
import com.bmskinner.nuclear_morphology.main.EventHandler;
import com.bmskinner.nuclear_morphology.main.ThreadManager;

public class ClusterFileAssignmentAction extends SingleDatasetResultAction {

    private static final String PROGRESS_BAR_LABEL = "Assigning clustered cells";

    public ClusterFileAssignmentAction(IAnalysisDataset dataset, @NonNull ProgressBarAcceptor acceptor, @NonNull EventHandler eh) {
        super(dataset, PROGRESS_BAR_LABEL, acceptor, eh);
    }

    @Override
    public void run() {

    	File clusterMapFile = FileSelector.chooseFile(dataset.getSavePath().getParentFile());
    	
    	if(clusterMapFile!=null) {

            IAnalysisMethod m = new ClusterFileAssignmentMethod(dataset, clusterMapFile);
            worker = new DefaultAnalysisWorker(m);

            worker.addPropertyChangeListener(this);
            ThreadManager.getInstance().submit(worker);

        } else {
            this.cancel();
        }
    }

    @Override
    public void finished() {

        this.setProgressBarVisible(false);

        try {
            ClusterAnalysisResult r = (ClusterAnalysisResult) worker.get();
            int size = r.getGroup().size();
            log("Found " + size + " clusters");
        } catch (InterruptedException | ExecutionException e) {
            warn("Error clustering");
            stack("Error clustering", e);
        }
        getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.REFRESH_POPULATIONS);
        super.finished();

    }
}
