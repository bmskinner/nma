package com.bmskinner.nma.gui.actions;

import java.io.File;

import javax.swing.filechooser.FileNameExtensionFilter;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.components.FileSelector;
import com.bmskinner.nma.io.TPSexporter;

public class ExportTPSAction extends SingleDatasetResultAction {

	private static final @NonNull String PROGRESS_LBL = "Exporting TPS data";

	public ExportTPSAction(IAnalysisDataset dataset, ProgressBarAcceptor acceptor) {
		super(dataset, PROGRESS_LBL, acceptor);
	}

	@Override
	public void run() {
		File file = FileSelector.chooseSaveFile(dataset.getSavePath().getParentFile(),
				new FileNameExtensionFilter("TPS file", "tps"), dataset.getName());

		if (file == null) {
			cancel();
			return;
		}

		IAnalysisMethod m = new TPSexporter(file, dataset);
		worker = new DefaultAnalysisWorker(m, 1);
		worker.addPropertyChangeListener(this);
		this.setProgressMessage("Exporting outlines");
		ThreadManager.getInstance().submit(worker);

	}

}
