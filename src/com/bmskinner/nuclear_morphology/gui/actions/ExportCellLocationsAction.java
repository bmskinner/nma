package com.bmskinner.nuclear_morphology.gui.actions;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.io.CellFileExporter;

/**
 * The action for exporting cell locations from datasets
 * 
 * @author bms41
 * @since 1.14.0
 *
 */
public class ExportCellLocationsAction extends MultiDatasetResultAction {
	
	private static final @NonNull String PROGRESS_LBL = "Exporting cell location";

    public ExportCellLocationsAction(@NonNull final List<IAnalysisDataset> datasets, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
        super(datasets, PROGRESS_LBL, acceptor, eh);
    }


    @Override
    public void run() {
    	IAnalysisMethod m = new CellFileExporter(datasets);
    	worker = new DefaultAnalysisWorker(m, datasets.size());
    	worker.addPropertyChangeListener(this);
    	this.setProgressMessage("Exporting stats");
    	ThreadManager.getInstance().submit(worker);
    }


}
