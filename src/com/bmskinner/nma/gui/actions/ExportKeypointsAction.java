package com.bmskinner.nma.gui.actions;

import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.DefaultOptions;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.io.DatasetKeypointExportMethod;

/**
 * Export keypoints and bounding boxes of nuclei in images in JSON format
 * 
 * @author bs19022
 *
 */
public class ExportKeypointsAction extends MultiDatasetResultAction {

	private static final Logger LOGGER = Logger
			.getLogger(ExportSingleCellImagesAction.class.getName());

	private static final String PROGRESS_LBL = "Exporting keypoints";

	public ExportKeypointsAction(@NonNull List<IAnalysisDataset> datasets,
			@NonNull final ProgressBarAcceptor acceptor) {
		super(datasets, PROGRESS_LBL, acceptor);
	}

	@Override
	public void run() {
		setProgressBarIndeterminate();

		// No options set yet, leave for future expansion
		IAnalysisMethod m = new DatasetKeypointExportMethod(datasets, new DefaultOptions());

		int nFiles = datasets.stream().mapToInt(d -> d.getCollection().getImageFiles().size())
				.sum();

		worker = new DefaultAnalysisWorker(m, nFiles);
		worker.addPropertyChangeListener(this);
		ThreadManager.getInstance().submit(worker);
	}
}
