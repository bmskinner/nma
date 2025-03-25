package com.bmskinner.nma.gui.actions;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.DefaultOptions;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.components.FileSelector;
import com.bmskinner.nma.io.DatasetLandmarkExportMethod;

/**
 * Export keypoints and bounding boxes of nuclei in images in JSON format
 * 
 * @author Ben Skinner
 *
 */
public class ExportKeypointsAction extends MultiDatasetResultAction {
	
	private static final Logger LOGGER = Logger.getLogger(ExportKeypointsAction.class.getName());


	private static final String PROGRESS_LBL = "Exporting keypoints";

	public ExportKeypointsAction(@NonNull List<IAnalysisDataset> datasets,
			@NonNull final ProgressBarAcceptor acceptor) {
		super(datasets, PROGRESS_LBL, acceptor);
		
		if(datasets.isEmpty()) {
			LOGGER.warning("No datasets selected for export!");
		}
	}

	@Override
	public void run() {
		setProgressBarIndeterminate();
		
		try {
			File file = FileSelector.chooseStatsExportFile(datasets, "keypoints");
			if (is.fileIsOKForSave(file)) {

				// No options set yet, leave for future expansion
				IAnalysisMethod m = new DatasetLandmarkExportMethod(file, datasets,
						new DefaultOptions());
				worker = new DefaultAnalysisWorker(m, datasets.size());
				worker.addPropertyChangeListener(this);
				this.setProgressMessage(PROGRESS_LBL);
				ThreadManager.getInstance().submit(worker);
			} else {
				cancel();
			}

		} catch (RequestCancelledException e) {
			cancel();
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, "Error in keypoint export", e);
			cancel();
		}
	}
}
