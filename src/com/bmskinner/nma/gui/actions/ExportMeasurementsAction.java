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

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.components.FileSelector;
import com.bmskinner.nma.gui.dialogs.SubAnalysisSetupDialog;
import com.bmskinner.nma.io.DatasetMeasurementsExporter;
import com.bmskinner.nma.io.DatasetOutlinesExporter;
import com.bmskinner.nma.io.DatasetProfileExporter;
import com.bmskinner.nma.io.DatasetShellsExporter;
import com.bmskinner.nma.io.DatasetSignalsExporter;

/**
 * The base action for exporting stats from datasets
 * 
 * @author bms41
 * @since 1.13.8
 *
 */
public abstract class ExportMeasurementsAction extends MultiDatasetResultAction {

	private static final Logger LOGGER = Logger.getLogger(ExportMeasurementsAction.class.getName());

	protected ExportMeasurementsAction(@NonNull final List<IAnalysisDataset> datasets,
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
	public static class ExportNuclearStatsAction extends ExportMeasurementsAction {

		private static final @NonNull String PROGRESS_LBL = "Exporting nuclear stats";

		public ExportNuclearStatsAction(@NonNull final List<IAnalysisDataset> datasets,
				@NonNull final ProgressBarAcceptor acceptor) {
			super(datasets, PROGRESS_LBL, acceptor);
		}

		@Override
		public void run() {

			SubAnalysisSetupDialog optionsPanel = new ExportOptionsDialog(datasets);
			if (optionsPanel.isReadyToRun()) {

				try {
					File file = FileSelector.chooseStatsExportFile(datasets, "stats");
					if (is.fileIsOKForSave(file)) {

						IAnalysisMethod m = new DatasetMeasurementsExporter(file, datasets,
								optionsPanel.getOptions());
						worker = new DefaultAnalysisWorker(m, datasets.size());
						worker.addPropertyChangeListener(this);
						this.setProgressMessage("Exporting stats");
						ThreadManager.getInstance().submit(worker);
					} else {
						cancel();
					}
				} catch (RequestCancelledException e) {
					cancel();
				}
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

			private static final String INCLUDE_MEASUREMENTS_LBL = "Include measurements";
			private static final String INCLUDE_OUTLINES_LBL = "Include outlines";
			private static final String INCLUDE_PROFILES_LBL = "Include profiles";
			private static final String PROFILE_SAMPLE_LBL = "Profile length";
			private static final String OUTLINE_SAMPLE_LBL = "Outline length";
			private static final String OM_LBL = "Orientation mark to start from";

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

				JCheckBox isExportMeasurementsBox = new JCheckBox("", true);
				isExportMeasurementsBox.addChangeListener(e -> {
					options.set(DefaultOptions.EXPORT_MEASUREMENTS_KEY,
							isExportMeasurementsBox.isSelected());
				});

				SpinnerModel profileInterpolationModel = new SpinnerNumberModel(100, // initial
						100, // min
						1000, // max
						1); // step
				JSpinner profileInterpolationSpinner = new JSpinner(profileInterpolationModel);

				profileInterpolationSpinner.addChangeListener(
						e -> addIntToOptions(profileInterpolationSpinner, options,
								HashOptions.EXPORT_PROFILE_INTERPOLATION_LENGTH));

				JCheckBox isExportProfilesBox = new JCheckBox("", true);
				JCheckBox isExportOutlinesBox = new JCheckBox("", false);

				isExportProfilesBox.addChangeListener(e -> {
					options.setBoolean(DefaultOptions.EXPORT_PROFILES_KEY,
							isExportProfilesBox.isSelected());
					profileInterpolationSpinner.setEnabled(
							isExportProfilesBox.isSelected());
				});

				// If downsamping, number of points to export

				SpinnerModel outlineInterpolationModel = new SpinnerNumberModel(100, // initial
						10, // min
						1000, // max
						1); // step
				JSpinner outlineInterpolationSpinner = new JSpinner(outlineInterpolationModel);

				outlineInterpolationSpinner.addChangeListener(
						e -> addIntToOptions(outlineInterpolationSpinner, options,
								DefaultOptions.EXPORT_OUTLINE_N_SAMPLES_KEY));

				// Dropdown for which orientation point to index on
				JComboBox<OrientationMark> omBox = new JComboBox<>(OrientationMark.values());
				omBox.setSelectedItem(OrientationMark.REFERENCE);

				omBox.addActionListener(e -> {
					options.setString(DefaultOptions.EXPORT_OUTLINE_STARTING_LANDMARK_KEY,
							((OrientationMark) omBox.getSelectedItem()).name());
				});

				isExportOutlinesBox.addChangeListener(e -> {
					options.set(DefaultOptions.EXPORT_OUTLINES_KEY,
							isExportOutlinesBox.isSelected());
					outlineInterpolationSpinner.setEnabled(isExportOutlinesBox.isSelected());
					omBox.setEnabled(isExportOutlinesBox.isSelected());

				});

				// Add elements in order

				labels.add(new JLabel(INCLUDE_MEASUREMENTS_LBL));
				fields.add(isExportMeasurementsBox);

				labels.add(new JLabel(INCLUDE_PROFILES_LBL));
				fields.add(isExportProfilesBox);

				labels.add(new JLabel(PROFILE_SAMPLE_LBL));
				fields.add(profileInterpolationSpinner);

				labels.add(new JLabel(INCLUDE_OUTLINES_LBL));
				fields.add(isExportOutlinesBox);

				labels.add(new JLabel(OUTLINE_SAMPLE_LBL));
				fields.add(outlineInterpolationSpinner);

				labels.add(new JLabel(OM_LBL));
				fields.add(omBox);

				addLabelTextRows(labels, fields, layout, panel);
				add(panel, BorderLayout.CENTER);
				add(createFooter(), BorderLayout.SOUTH);
			}

			@Override
			protected void setDefaults() {
				options.set(HashOptions.EXPORT_MEASUREMENTS_KEY, true);
				options.set(HashOptions.EXPORT_PROFILES_KEY, true);
				options.set(HashOptions.EXPORT_OUTLINES_KEY, false);

				options.set(HashOptions.EXPORT_PROFILE_INTERPOLATION_LENGTH, 100);
				options.set(HashOptions.EXPORT_OUTLINE_IS_NORMALISED_KEY, true);
				options.set(HashOptions.EXPORT_OUTLINE_STARTING_LANDMARK_KEY,
						OrientationMark.REFERENCE.name());
				options.set(HashOptions.EXPORT_OUTLINE_N_SAMPLES_KEY, 100);
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
	public static class ExportNuclearProfilesAction extends ExportMeasurementsAction {

		private static final @NonNull String PROGRESS_LBL = "Exporting nuclear stats";

		public ExportNuclearProfilesAction(@NonNull final List<IAnalysisDataset> datasets,
				@NonNull final ProgressBarAcceptor acceptor) {
			super(datasets, PROGRESS_LBL, acceptor);
		}

		@Override
		public void run() {
			try {
				File file = FileSelector.chooseStatsExportFile(datasets, "profiles");
				if (is.fileIsOKForSave(file)) {

					IAnalysisMethod m = new DatasetProfileExporter(file, datasets,
							new DefaultOptions());
					worker = new DefaultAnalysisWorker(m, datasets.size());
					worker.addPropertyChangeListener(this);
					this.setProgressMessage("Exporting profiles");
					ThreadManager.getInstance().submit(worker);
				} else {
					cancel();
				}
			} catch (RequestCancelledException e) {
				cancel();
			}
		}
	}

	/**
	 * The action for exporting cell component outlines from datasets
	 * 
	 * @author bms41
	 * @since 1.17.2
	 *
	 */
	public static class ExportNuclearOutlinesAction extends ExportMeasurementsAction {

		private static final @NonNull String PROGRESS_LBL = "Exporting nuclear stats";

		public ExportNuclearOutlinesAction(@NonNull final List<IAnalysisDataset> datasets,
				@NonNull final ProgressBarAcceptor acceptor) {
			super(datasets, PROGRESS_LBL, acceptor);
		}

		@Override
		public void run() {

			SubAnalysisSetupDialog optionsPanel = new ExportOutlinesOptionsDialog(datasets);
			if (optionsPanel.isReadyToRun()) {
				try {
					File file = FileSelector.chooseStatsExportFile(datasets, "outlines");
					if (is.fileIsOKForSave(file)) {
						IAnalysisMethod m = new DatasetOutlinesExporter(file, datasets,
								optionsPanel.getOptions());
						worker = new DefaultAnalysisWorker(m, datasets.size());
						worker.addPropertyChangeListener(this);
						this.setProgressMessage("Exporting outlines");
						ThreadManager.getInstance().submit(worker);
					} else {
						cancel();
					}

				} catch (RequestCancelledException e) {
					cancel();
				}

			} else {
				cancel();
			}

		}

		/**
		 * Configure the exported parameters
		 * 
		 * @author ben
		 * @since 2.1.1
		 *
		 */
		@SuppressWarnings("serial")
		private class ExportOutlinesOptionsDialog extends SubAnalysisSetupDialog {

			private HashOptions options = new OptionsBuilder().build();
			private static final String IS_SAMPLE_LBL = "Sample fixed number of points";
			private static final String PROFILE_SAMPLE_LBL = "Number of points";
			private static final String OM_LBL = "Orientation mark to start from";

			public ExportOutlinesOptionsDialog(final List<IAnalysisDataset> datasets) {
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

				// If downsamping, number of points to export

				SpinnerModel model = new SpinnerNumberModel(100, // initial
						10, // min
						1000, // max
						1); // step
				JSpinner spinner = new JSpinner(model);

				spinner.addChangeListener(
						e -> addIntToOptions(spinner, options,
								DefaultOptions.EXPORT_OUTLINE_N_SAMPLES_KEY));
				spinner.setEnabled(false);

				// Checkbox for whether to downsample

				JCheckBox useSampling = new JCheckBox("", false);
				useSampling.addChangeListener(e -> {
					options.setBoolean(DefaultOptions.EXPORT_OUTLINE_IS_NORMALISED_KEY,
							useSampling.isSelected());

					spinner.setEnabled(useSampling.isSelected());
				});

				// Dropdown for which orientation point to index on
				JComboBox<OrientationMark> omBox = new JComboBox<>(OrientationMark.values());
				omBox.setSelectedItem(OrientationMark.REFERENCE);

				omBox.addActionListener(e -> {
					options.setString(DefaultOptions.EXPORT_OUTLINE_STARTING_LANDMARK_KEY,
							((OrientationMark) omBox.getSelectedItem()).name());
				});

				// Add elements in order

				labels.add(new JLabel(IS_SAMPLE_LBL));
				fields.add(useSampling);

				labels.add(new JLabel(PROFILE_SAMPLE_LBL));
				fields.add(spinner);

				labels.add(new JLabel(OM_LBL));
				fields.add(omBox);

				addLabelTextRows(labels, fields, layout, panel);
				add(panel, BorderLayout.CENTER);
				add(createFooter(), BorderLayout.SOUTH);
			}

			@Override
			protected void setDefaults() {
				options.set(DefaultOptions.EXPORT_OUTLINE_IS_NORMALISED_KEY, false);
				options.set(DefaultOptions.EXPORT_OUTLINE_STARTING_LANDMARK_KEY,
						OrientationMark.REFERENCE.name());
				options.set(DefaultOptions.EXPORT_OUTLINE_N_SAMPLES_KEY, 100);
			}

		}

	}

	/**
	 * The action for exporting shell data from datasets
	 * 
	 * @author bms41
	 * @since 1.13.8
	 *
	 */
	public static class ExportShellsAction extends ExportMeasurementsAction {

		private static final String PROGRESS_LBL = "Exporting shells";

		public ExportShellsAction(@NonNull final List<IAnalysisDataset> datasets,
				@NonNull final ProgressBarAcceptor acceptor) {
			super(datasets, PROGRESS_LBL, acceptor);
		}

		@Override
		public void run() {

			try {
				File file = FileSelector.chooseStatsExportFile(datasets, "shells");
				if (is.fileIsOKForSave(file)) {

					IAnalysisMethod m = new DatasetShellsExporter(file, datasets,
							new DefaultOptions());
					worker = new DefaultAnalysisWorker(m, datasets.size());
					worker.addPropertyChangeListener(this);
					this.setProgressMessage("Exporting stats");
					ThreadManager.getInstance().submit(worker);
				} else {
					cancel();
				}
			} catch (RequestCancelledException e) {
				cancel();
			}

		}

	}

	/**
	 * The action for exporting shell data from datasets
	 * 
	 * @author bms41
	 * @since 1.13.8
	 *
	 */
	public static class ExportSignalsAction extends ExportMeasurementsAction {

		private static final String PROGRESS_LBL = "Exporting signals";

		public ExportSignalsAction(@NonNull final List<IAnalysisDataset> datasets,
				@NonNull final ProgressBarAcceptor acceptor) {
			super(datasets, PROGRESS_LBL, acceptor);
		}

		@Override
		public void run() {

			try {
				File file = FileSelector.chooseStatsExportFile(datasets, "signals");
				if (is.fileIsOKForSave(file)) {

					IAnalysisMethod m = new DatasetSignalsExporter(file, datasets,
							new DefaultOptions());
					worker = new DefaultAnalysisWorker(m, datasets.size());
					worker.addPropertyChangeListener(this);
					this.setProgressMessage("Exporting stats");
					ThreadManager.getInstance().submit(worker);
				} else {
					cancel();
				}
			} catch (RequestCancelledException e) {
				cancel();
			}
		}

	}

}
