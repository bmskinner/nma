package com.bmskinner.nuclear_morphology.gui.actions;

import java.io.File;

import javax.swing.filechooser.FileNameExtensionFilter;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.components.FileSelector;
import com.bmskinner.nuclear_morphology.io.TPSexporter;

public class ExportTPSAction extends SingleDatasetResultAction {
	
	private static final @NonNull String PROGRESS_LBL = "Exporting TPS data";

	public ExportTPSAction(IAnalysisDataset dataset, ProgressBarAcceptor acceptor, EventHandler eh) {
		super(dataset, PROGRESS_LBL, acceptor, eh);
	}

	@Override
	public void run() {
		File file = FileSelector.chooseSaveFile(dataset.getSavePath().getParentFile(), 
				new FileNameExtensionFilter("TPS file", "tps"),
				dataset.getName());

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
