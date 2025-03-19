package com.bmskinner.nma.gui.actions;

import java.io.File;

import javax.swing.filechooser.FileNameExtensionFilter;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.DefaultOptions;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.components.FileSelector;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.gui.runnables.SegmentAndRefold;
import com.bmskinner.nma.io.DatasetKeypointImportMethod;
import com.bmskinner.nma.io.Io;

public class ImportKeypointsAction extends SingleDatasetResultAction {

	private static final @NonNull String PROGRESS_LBL = "Importing keypoints";

	public ImportKeypointsAction(@NonNull IAnalysisDataset dataset, @NonNull final ProgressBarAcceptor acceptor) {
		super(dataset, PROGRESS_LBL, acceptor);
		setProgressBarIndeterminate();
	}

	@Override
	public void run() {
		/*
		 * Get the file to search
		 */

		File file = FileSelector.chooseFile(dataset.getSavePath().getParentFile(), 
				new FileNameExtensionFilter("Text file", Io.TAB_FILE_EXTENSION_NODOT),
				"Select keypoint file");

		if (file != null) {

			IAnalysisMethod m = new DatasetKeypointImportMethod(dataset, file, new DefaultOptions());
			worker = new DefaultAnalysisWorker(m);

			worker.addPropertyChangeListener(this);

			this.setProgressMessage("Updating landmarks...");
			ThreadManager.getInstance().submit(worker);
		} else {
			cancel();
		}
	}

	@Override
	public void finished() {
		
		UIController.getInstance().fireProfilesUpdated(dataset);
		Runnable r = new SegmentAndRefold(dataset, progressAcceptors.get(0));
		ThreadManager.getInstance().execute(r);
		super.finished();
	}

}