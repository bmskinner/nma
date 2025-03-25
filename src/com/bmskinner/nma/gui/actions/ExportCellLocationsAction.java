package com.bmskinner.nma.gui.actions;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.io.CellFileExporter;

/**
 * The action for exporting cell locations from datasets
 * 
 * @author Ben Skinner
 * @since 1.14.0
 *
 */
public class ExportCellLocationsAction extends MultiDatasetResultAction {

	private static final @NonNull String PROGRESS_LBL = "Exporting cell location";

	public ExportCellLocationsAction(@NonNull final List<IAnalysisDataset> datasets,
			@NonNull final ProgressBarAcceptor acceptor) {
		super(datasets, PROGRESS_LBL, acceptor);
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
