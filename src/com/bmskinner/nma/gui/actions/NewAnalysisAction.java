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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.DefaultInputSupplier;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.dialogs.prober.NucleusImageProber;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.events.UserActionEvent;

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
		// Image directory may have been specified; if not, request from user
		if(folder == null) {
			try {
				folder = new DefaultInputSupplier().requestFolder("Select image directory", GlobalOptions.getInstance().getDefaultDir());
			} catch (RequestCancelledException e) {
				// user cancelled
				this.cancel();
				return;
			}
		}

		// If the user selected file failed, cancel
		if (folder == null) {
			LOGGER.fine("Could not get image directory");
			cancel();
			return;
		}

		// Initialise an options with default values
		IAnalysisOptions options = OptionsFactory
				.makeAnalysisOptions(RuleSetCollection.mouseSpermRuleSetCollection());
		options.setDetectionFolder(CellularComponent.NUCLEUS, this.folder);
		HashOptions nucleusOptions = OptionsFactory.makeNucleusDetectionOptions()
				.build();
		options.setDetectionOptions(CellularComponent.NUCLEUS, nucleusOptions);

		this.setProgressBarIndeterminate();

		LOGGER.fine("Creating for " + folder.getAbsolutePath());

		// Start the image prober to set analysis options
		NucleusImageProber analysisSetup = new NucleusImageProber(folder, options);

		if (analysisSetup.isOk()) {

			// Don't run if the selected folder has been removed
			Optional<File> detectionFolder = options.getDetectionFolder(CellularComponent.NUCLEUS);
			if (!detectionFolder.isPresent()) {
				cancel();
				return;
			}

			LOGGER.info("Directory: " + detectionFolder.get().getName());

			// Get the start time of the analysis to make the output directory
			Instant inst = Instant.ofEpochMilli(options.getAnalysisTime());
			LocalDateTime anTime = LocalDateTime.ofInstant(inst, ZoneId.systemDefault());

			File outputFolder = new File(detectionFolder.get(), anTime
					.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")));

			try {
				IAnalysisMethod m = new NucleusDetectionMethod(outputFolder, options);
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
			LOGGER.fine("New analysis action finished");
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
			LOGGER.log(Level.SEVERE, "Interruption to swing worker", e);
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			LOGGER.warning("Execution error in swing worker");
			LOGGER.log(Level.SEVERE, "Execution error in swing worker", e);
		}

		super.finished();
	}
}
