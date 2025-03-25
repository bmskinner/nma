package com.bmskinner.nma.gui.actions;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.io.XMLLandmarkRemappingMethod;
import com.bmskinner.nma.io.XMLReader;
import com.bmskinner.nma.io.XMLReader.XMLReadingException;

/**
 * Action to remap landmarks between equivalent cells in nmd files
 * 
 * @author Ben Skinner
 *
 */
public class LandmarkRemappingAction extends VoidResultAction {

	private static final Logger LOGGER = Logger
			.getLogger(LandmarkRemappingAction.class.getName());
	private static final @NonNull String PROGRESS_BAR_LABEL = "Remapping landmarks";

	public LandmarkRemappingAction(@NonNull CountDownLatch latch,
			@NonNull ProgressBarAcceptor acceptor) {
		super(PROGRESS_BAR_LABEL, acceptor);
		this.setLatch(latch);
	}

	public LandmarkRemappingAction(@NonNull ProgressBarAcceptor acceptor) {
		super(PROGRESS_BAR_LABEL, acceptor);
	}

	@Override
	public void run() {

		LOGGER.fine("Beginning remapping");

		try {
			File sourceFile = is.requestFile("Choose source file", null,
					Io.NMD_FILE_EXTENSION_NODOT,
					"Nuclear morphology dataset");

			File targetFile = is.requestFile("Choose target file", sourceFile.getParentFile(),
					Io.NMD_FILE_EXTENSION_NODOT,
					"Nuclear morphology dataset");
			File outputFile = is.requestFileSave(targetFile.getParentFile(),
					targetFile.getName() + "_remapped", Io.NMD_FILE_EXTENSION_NODOT);

			Document source = XMLReader.readDocument(sourceFile);
			Document target = XMLReader.readDocument(targetFile);

			IAnalysisMethod m = new XMLLandmarkRemappingMethod(source, target, outputFile);

			worker = new DefaultAnalysisWorker(m);
			worker.addPropertyChangeListener(this);
			ThreadManager.getInstance().submit(worker);
		} catch (RequestCancelledException e) {
			// no action, user cancelled
			this.cancel();
		} catch (XMLReadingException e) {
			LOGGER.log(Level.SEVERE, "Error remapping landmarks: " + e.getMessage(), e);
		}
	}

	@Override
	public void finished() {
		super.finished();
		countdownLatch();
	}

}
