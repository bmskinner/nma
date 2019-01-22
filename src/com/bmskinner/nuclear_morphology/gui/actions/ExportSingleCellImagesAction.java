package com.bmskinner.nuclear_morphology.gui.actions;

import java.io.File;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.components.FileSelector;
import com.bmskinner.nuclear_morphology.io.CellImageExportMethod;
import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.io.xml.OptionsXMLWriter;

public class ExportSingleCellImagesAction extends MultiDatasetResultAction {

	private static final String PROGRESS_LBL = "Exporting single cells";

	public ExportSingleCellImagesAction(@NonNull List<IAnalysisDataset> datasets, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
		super(datasets, PROGRESS_LBL, acceptor, eh);
	}

	@Override
	public void run() {
		setProgressBarIndeterminate();
		IAnalysisMethod m = new CellImageExportMethod(datasets);
		
		int nNuclei = datasets.stream().mapToInt(d->d.getCollection().getNucleusCount()).sum();
		
		worker = new DefaultAnalysisWorker(m, nNuclei);
		worker.addPropertyChangeListener(this);
		ThreadManager.getInstance().submit(worker);
	}
}
