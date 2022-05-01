/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.gui.actions;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
import com.bmskinner.nma.analysis.nucleus.NucleusDetectionMethod;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.options.OptionsFactory;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.dialogs.prober.NucleusImageProber;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.events.UserActionEvent;
import com.bmskinner.nma.logging.Loggable;

/**
 * Run a new analysis
 */
public class NewAnalysisAction extends VoidResultAction {

	private static final Logger LOGGER = Logger.getLogger(NewAnalysisAction.class.getName());

	private File folder = null;

	public static final int NEW_ANALYSIS = 0;

	private static final @NonNull String PROGRESS_BAR_LABEL = "Nucleus detection";

	/**
	 * Create a new analysis. The folder of images to analyse will be requested by a
	 * dialog.
	 * 
	 * @param mw the main window to which a progress bar will be attached
	 */
	public NewAnalysisAction(@NonNull ProgressBarAcceptor acceptor) {
		this(acceptor, null);
	}

	/**
	 * Create a new analysis, specifying the initial directory of images
	 * 
	 * @param mw     the main window to which a progress bar will be attached
	 * @param folder the folder of images to analyse
	 */
	public NewAnalysisAction(@NonNull ProgressBarAcceptor acceptor, final File folder) {
		super(PROGRESS_BAR_LABEL, acceptor);
		this.folder = folder;
	}

	@Override
	public void run() {
		if (folder == null && !getImageDirectory()) {
			LOGGER.fine("Could not get image directory");
			cancel();
			return;
		}

		IAnalysisOptions options = OptionsFactory
				.makeAnalysisOptions(RuleSetCollection.mouseSpermRuleSetCollection());
		options.setDetectionFolder(CellularComponent.NUCLEUS, this.folder);
		HashOptions nucleusOptions = OptionsFactory.makeNucleusDetectionOptions()
				.build();
		options.setDetectionOptions(CellularComponent.NUCLEUS, nucleusOptions);

		this.setProgressBarIndeterminate();

		LOGGER.fine("Creating for " + folder.getAbsolutePath());

		NucleusImageProber analysisSetup = new NucleusImageProber(folder, options);

		if (analysisSetup.isOk()) {

			Optional<HashOptions> op = options.getDetectionOptions(CellularComponent.NUCLEUS);
			if (!op.isPresent()) {
				cancel();
				return;
			}

			File directory = new File(op.get().getString(HashOptions.DETECTION_FOLDER));

			LOGGER.info("Directory: " + directory.getName());

			Instant inst = Instant.ofEpochMilli(options.getAnalysisTime());
			LocalDateTime anTime = LocalDateTime.ofInstant(inst, ZoneOffset.systemDefault());
			String outputFolderName = anTime
					.format(DateTimeFormatter.ofPattern("YYYY-MM-dd_HH-mm-ss"));

			try {
				IAnalysisMethod m = new NucleusDetectionMethod(outputFolderName, options);
				worker = new DefaultAnalysisWorker(m);
				worker.addPropertyChangeListener(this);
				ThreadManager.getInstance().submit(worker);
			} catch (AnalysisMethodException e) {
				LOGGER.info("Unable to run detection: " + e.getMessage());
			} finally {
				analysisSetup.dispose();
			}

		} else {
			analysisSetup.dispose();
			LOGGER.fine("Analysis cancelled");
			this.cancel();
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
						new UserActionEvent(this, UserActionEvent.MORPHOLOGY_ANALYSIS_ACTION,
								datasets));
			}

		} catch (InterruptedException e) {
			LOGGER.warning("Interruption to swing worker");
			LOGGER.log(Loggable.STACK, "Interruption to swing worker", e);
		} catch (ExecutionException e) {
			LOGGER.warning("Execution error in swing worker");
			LOGGER.log(Loggable.STACK, "Execution error in swing worker", e);
		}

		super.finished();
	}

	private boolean getImageDirectory() {

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
