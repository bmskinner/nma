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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.DefaultOptions;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.OptionsBuilder;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.components.FileSelector;
import com.bmskinner.nma.gui.dialogs.SubAnalysisSetupDialog;
import com.bmskinner.nma.io.DatasetOutlinesExporter;
import com.bmskinner.nma.io.DatasetProfileExporter;
import com.bmskinner.nma.io.DatasetShellsExporter;
import com.bmskinner.nma.io.DatasetSignalsExporter;
import com.bmskinner.nma.io.DatasetStatsExporter;
import com.bmskinner.nma.io.Io;

/**
 * The base action for exporting stats from datasets
 * 
 * @author bms41
 * @since 1.13.8
 *
 */
public abstract class ExportStatsAction extends MultiDatasetResultAction {

	private static final Logger LOGGER = Logger.getLogger(ExportStatsAction.class.getName());

	protected ExportStatsAction(@NonNull final List<IAnalysisDataset> datasets,
			@NonNull final String label,
			@NonNull final ProgressBarAcceptor acceptor) {
		super(datasets, label, acceptor);
	}

	/**
	 * The action for exporting nuclear stats from datasets
	 * 
	 * @author bms41
	 * @since 1.13.4
	 *
	 */
	public static class ExportNuclearStatsAction extends ExportStatsAction {

		private static final @NonNull String PROGRESS_LBL = "Exporting nuclear stats";

		public ExportNuclearStatsAction(@NonNull final List<IAnalysisDataset> datasets,
				@NonNull final ProgressBarAcceptor acceptor) {
			super(datasets, PROGRESS_LBL, acceptor);
		}

		@Override
		public void run() {

			SubAnalysisSetupDialog optionsPanel = new ExportOptionsDialog(datasets);
			if (optionsPanel.isReadyToRun()) {

				File file = FileSelector.chooseStatsExportFile(datasets, "stats");

				if (file == null) {
					cancel();
					return;
				}

				IAnalysisMethod m = new DatasetStatsExporter(file, datasets,
						optionsPanel.getOptions());
				worker = new DefaultAnalysisWorker(m, datasets.size());
				worker.addPropertyChangeListener(this);
				this.setProgressMessage("Exporting stats");
				ThreadManager.getInstance().submit(worker);
			} else {
				cancel();
			}
		}

		/**
		 * Configure the exported parameters
		 * 
		 * @author ben
		 * @since 1.18.4
		 *
		 */
		private class ExportOptionsDialog extends SubAnalysisSetupDialog {

			private HashOptions options = new OptionsBuilder().build();
			private static final String PROFILE_SAMPLE_LBL = "Profile samples";

			public ExportOptionsDialog(final List<IAnalysisDataset> datasets) {
				super(datasets, "Export options");
				setDefaults();
				createUI();
				packAndDisplay();
			}

			@Override
			public IAnalysisMethod getMethod() {
				// Not used here, we need to select a file first
				return null;
			}

			@Override
			public HashOptions getOptions() {
				return options;
			}

			@Override
			protected void createUI() {
				JPanel panel = new JPanel();
				GridBagLayout layout = new GridBagLayout();
				panel.setLayout(layout);
				List<JLabel> labels = new ArrayList<>();
				List<Component> fields = new ArrayList<>();
				SpinnerModel model = new SpinnerNumberModel(100, // initial
						100, // min
						1000, // max
						1); // step
				JSpinner spinner = new JSpinner(model);

				spinner.addChangeListener(
						e -> addIntToOptions(spinner, options, Io.PROFILE_SAMPLES_KEY));
				labels.add(new JLabel(PROFILE_SAMPLE_LBL));
				fields.add(spinner);
				addLabelTextRows(labels, fields, layout, panel);
				add(panel, BorderLayout.CENTER);
				add(createFooter(), BorderLayout.SOUTH);
			}

			@Override
			protected void setDefaults() {
				options.setInt(Io.PROFILE_SAMPLES_KEY, 100);
			}

		}

	}

	/**
	 * The action for exporting nuclear stats from datasets
	 * 
	 * @author bms41
	 * @since 1.17.2
	 *
	 */
	public static class ExportNuclearProfilesAction extends ExportStatsAction {

		private static final @NonNull String PROGRESS_LBL = "Exporting nuclear stats";

		public ExportNuclearProfilesAction(@NonNull final List<IAnalysisDataset> datasets,
				@NonNull final ProgressBarAcceptor acceptor) {
			super(datasets, PROGRESS_LBL, acceptor);
		}

		@Override
		public void run() {

			File file = FileSelector.chooseStatsExportFile(datasets, "profiles");

			if (file == null) {
				cancel();
				return;
			}

			IAnalysisMethod m = new DatasetProfileExporter(file, datasets, new DefaultOptions());
			worker = new DefaultAnalysisWorker(m, datasets.size());
			worker.addPropertyChangeListener(this);
			this.setProgressMessage("Exporting profiles");
			ThreadManager.getInstance().submit(worker);

		}

	}

	/**
	 * The action for exporting cell component outlines from datasets
	 * 
	 * @author bms41
	 * @since 1.17.2
	 *
	 */
	public static class ExportNuclearOutlinesAction extends ExportStatsAction {

		private static final @NonNull String PROGRESS_LBL = "Exporting nuclear stats";

		public ExportNuclearOutlinesAction(@NonNull final List<IAnalysisDataset> datasets,
				@NonNull final ProgressBarAcceptor acceptor) {
			super(datasets, PROGRESS_LBL, acceptor);
		}

		@Override
		public void run() {

			File file = FileSelector.chooseStatsExportFile(datasets, "outlines");

			if (file == null) {
				cancel();
				return;
			}

			IAnalysisMethod m = new DatasetOutlinesExporter(file, datasets, new DefaultOptions());
			worker = new DefaultAnalysisWorker(m, datasets.size());
			worker.addPropertyChangeListener(this);
			this.setProgressMessage("Exporting outlines");
			ThreadManager.getInstance().submit(worker);

		}

	}

	/**
	 * The action for exporting shell data from datasets
	 * 
	 * @author bms41
	 * @since 1.13.8
	 *
	 */
	public static class ExportShellsAction extends ExportStatsAction {

		private static final String PROGRESS_LBL = "Exporting shells";

		public ExportShellsAction(@NonNull final List<IAnalysisDataset> datasets,
				@NonNull final ProgressBarAcceptor acceptor) {
			super(datasets, PROGRESS_LBL, acceptor);
		}

		@Override
		public void run() {

			File file = FileSelector.chooseStatsExportFile(datasets, "shells");

			if (file == null) {
				cancel();
				return;
			}

			IAnalysisMethod m = new DatasetShellsExporter(file, datasets, new DefaultOptions());
			worker = new DefaultAnalysisWorker(m, datasets.size());
			worker.addPropertyChangeListener(this);
			this.setProgressMessage("Exporting stats");
			ThreadManager.getInstance().submit(worker);

		}

	}

	/**
	 * The action for exporting shell data from datasets
	 * 
	 * @author bms41
	 * @since 1.13.8
	 *
	 */
	public static class ExportSignalsAction extends ExportStatsAction {

		private static final String PROGRESS_LBL = "Exporting signals";

		public ExportSignalsAction(@NonNull final List<IAnalysisDataset> datasets,
				@NonNull final ProgressBarAcceptor acceptor) {
			super(datasets, PROGRESS_LBL, acceptor);
		}

		@Override
		public void run() {

			File file = FileSelector.chooseStatsExportFile(datasets, "signals");

			if (file == null) {
				cancel();
				return;
			}

			IAnalysisMethod m = new DatasetSignalsExporter(file, datasets, new DefaultOptions());
			worker = new DefaultAnalysisWorker(m, datasets.size());
			worker.addPropertyChangeListener(this);
			this.setProgressMessage("Exporting stats");
			ThreadManager.getInstance().submit(worker);

		}

	}

}
