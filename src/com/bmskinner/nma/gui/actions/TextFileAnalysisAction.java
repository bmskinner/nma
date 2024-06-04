package com.bmskinner.nma.gui.actions;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import javax.swing.JFileChooser;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.AnalysisMethodException;
import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.detection.TextFileDetectionMethod;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.DefaultOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.options.OptionsFactory;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.events.UserActionEvent;
import com.bmskinner.nma.logging.Loggable;

/**
 * Analysis of a new dataset from text files (e.g. YOLO segmentation model). See
 * the {@link TextFileDetectionMethod} for details on the expected file format
 * 
 * @author bs19022
 *
 */
public class TextFileAnalysisAction extends VoidResultAction {
	private static final Logger LOGGER = Logger.getLogger(TextFileAnalysisAction.class.getName());

	private File folder = null;

	public static final int NEW_ANALYSIS = 0;

	private static final @NonNull String PROGRESS_BAR_LABEL = "Nucleus detection";

	/**
	 * Create a new analysis. The folder of text files to analyse will be requested
	 * by a dialog.
	 * 
	 * @param mw the main window to which a progress bar will be attached
	 */
	public TextFileAnalysisAction(@NonNull ProgressBarAcceptor acceptor) {
		this(acceptor, null);
	}

	/**
	 * Create a new analysis, specifying the initial directory of images
	 * 
	 * @param mw     the main window to which a progress bar will be attached
	 * @param folder the folder of text files to analyse
	 */
	public TextFileAnalysisAction(@NonNull ProgressBarAcceptor acceptor, final File folder) {
		super(PROGRESS_BAR_LABEL, acceptor);
		this.folder = folder;
	}

	@Override
	public void run() {
		if (folder == null && !getDirectory()) {
			LOGGER.fine("Could not get directory");
			cancel();
			return;
		}

		IAnalysisOptions options = OptionsFactory
				.makeAnalysisOptions(RuleSetCollection.roundRuleSetCollection());

		options.setAngleWindowProportion(0.05);
		options.setDetectionFolder(CellularComponent.NUCLEUS, this.folder);

//		HashOptions nucleusOptions = OptionsFactory.makeNucleusDetectionOptions()
//				.build();
		options.setDetectionOptions(CellularComponent.NUCLEUS, new DefaultOptions());

		this.setProgressBarIndeterminate();

		LOGGER.fine("Creating for " + folder.getAbsolutePath());

		Optional<File> detectionFolder = options.getDetectionFolder(CellularComponent.NUCLEUS);
		if (!detectionFolder.isPresent()) {
			cancel();
			return;
		}

		LOGGER.info("Directory: " + detectionFolder.get().getName());

		Instant inst = Instant.ofEpochMilli(options.getAnalysisTime());
		LocalDateTime anTime = LocalDateTime.ofInstant(inst, ZoneId.systemDefault());

		File outputFolder = new File(detectionFolder.get(), anTime
				.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")));

		try {
			IAnalysisMethod m = new TextFileDetectionMethod(outputFolder, options);
			worker = new DefaultAnalysisWorker(m);
			worker.addPropertyChangeListener(this);
			ThreadManager.getInstance().submit(worker);
		} catch (AnalysisMethodException e) {
			LOGGER.info("Unable to run detection: " + e.getMessage());
		}

	}

	@Override
	public void finished() {

		try {
			IAnalysisResult r = worker.get();
			List<IAnalysisDataset> datasets = r.getDatasets();

			if (datasets == null || datasets.isEmpty()) {
				LOGGER.info("No datasets returned");
			} else {
				UserActionController.getInstance().userActionEventReceived(
						new UserActionEvent(this, UserActionEvent.PROFILE_AND_CONSENSUS_ACTION,
								datasets));
			}

		} catch (InterruptedException e) {
			LOGGER.warning("Interruption to swing worker");
			LOGGER.log(Loggable.STACK, "Interruption to swing worker", e);
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			LOGGER.warning("Execution error in swing worker");
			LOGGER.log(Loggable.STACK, "Execution error in swing worker", e);
		}

		super.finished();
	}

	private boolean getDirectory() {

		File defaultDir = GlobalOptions.getInstance().getDefaultDir();

		JFileChooser fc = new JFileChooser(defaultDir); // if null, will be home
														// dir

		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		int returnVal = fc.showOpenDialog(fc);
		if (returnVal != 0) {
			return false; // user cancelled
		}

		File file = fc.getSelectedFile();

		if (!file.isDirectory())
			return false;

		folder = file;
		return true;
	}
}
