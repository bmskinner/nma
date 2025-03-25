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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.signals.shells.ShellAnalysisMethod;
import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.OptionsBuilder;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.signals.IShellResult.ShrinkType;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.dialogs.SubAnalysisSetupDialog;
import com.bmskinner.nma.gui.events.UIController;

/**
 * Prepare and run a shell analysis on the provided dataset.
 * 
 * @author Ben Skinner
 *
 */
public class ShellAnalysisAction extends SingleDatasetResultAction {

	private static final Logger LOGGER = Logger.getLogger(ShellAnalysisAction.class.getName());

	private static final String CIRC_ERROR_MESSAGE = "Min nucleus circularity is too low to make shells";
	private static final String AREA_ERROR_MESSAGE = "Min nucleus area is too small to break into shells";

	private static final @NonNull String PROGRESS_BAR_LABEL = "Shell analysis";

	/**
	 * Construct with a dataset and main event window
	 * 
	 * @param dataset
	 * @param mw
	 */
	public ShellAnalysisAction(@NonNull final IAnalysisDataset dataset,
			@NonNull final ProgressBarAcceptor acceptor) {
		super(dataset, PROGRESS_BAR_LABEL, acceptor);
	}

	@Override
	public void run() {

		ShellAnalysisSetupDialog sd = new ShellAnalysisSetupDialog(dataset);
		if (sd.isReadyToRun()) {

			HashOptions op = sd.getOptions();
			if (!datasetParametersOk(op.getInt(HashOptions.SHELL_COUNT_INT))) {
				this.cancel();
				return;
			}

			IAnalysisMethod m = sd.getMethod();
			worker = new DefaultAnalysisWorker(m);
			worker.addPropertyChangeListener(this);
			ThreadManager.getInstance().submit(worker);

		} else {
			this.cancel();
		}
	}

	@Override
	public void finished() {
		cleanup(); // remove the property change listener

		// Update the signal charts
		UIController.getInstance().fireNuclearSignalUpdated(dataset);

		super.finished();
	}

	/**
	 * Check if the nuclei in the dataset are suitable for shell analysis
	 * 
	 * @param shells
	 * @return
	 */
	private boolean datasetParametersOk(int shells) {

		try {

			double area = dataset.getCollection().getMin(Measurement.AREA,
					CellularComponent.NUCLEUS,
					MeasurementScale.PIXELS);
			double minArea = ShellAnalysisMethod.MINIMUM_AREA_PER_SHELL * (double) shells;
			if (area < minArea) {
				JOptionPane.showMessageDialog(null, AREA_ERROR_MESSAGE);
				return false;
			}

			double circ = dataset.getCollection().getMin(Measurement.CIRCULARITY,
					CellularComponent.NUCLEUS,
					MeasurementScale.PIXELS);

			if (circ < ShellAnalysisMethod.MINIMUM_CIRCULARITY) {
				JOptionPane.showMessageDialog(null, CIRC_ERROR_MESSAGE);
				return false;
			}

			return true;
		} catch (MissingDataException | SegmentUpdateException e) {
			LOGGER.log(Level.SEVERE, "Missing measurement in dataset", e);
			return false;
		}

	}

	@SuppressWarnings("serial")
	private class ShellAnalysisSetupDialog extends SubAnalysisSetupDialog {

		private static final String DIALOG_TITLE = "Shell analysis options";

		HashOptions o = new OptionsBuilder().build();

		public ShellAnalysisSetupDialog(final @NonNull IAnalysisDataset dataset) {
			this(dataset, DIALOG_TITLE);
		}

		/**
		 * Constructor that does not make panel visible
		 * 
		 * @param dataset the dataset
		 * @param title
		 */
		protected ShellAnalysisSetupDialog(final @NonNull IAnalysisDataset dataset,
				final String title) {
			super(dataset, title);
			setDefaults();
			createUI();
			packAndDisplay();
		}

		@Override
		public HashOptions getOptions() {
			return o;
		}

		@Override
		public IAnalysisMethod getMethod() {
			return new ShellAnalysisMethod(dataset, o);
		}

		@Override
		protected void createUI() {

			getContentPane().add(createHeader(), BorderLayout.NORTH);
			getContentPane().add(createFooter(), BorderLayout.SOUTH);

			JPanel optionsPanel = new JPanel();
			GridBagLayout layout = new GridBagLayout();
			optionsPanel.setLayout(layout);

			List<JLabel> labels = new ArrayList<>();
			List<Component> fields = new ArrayList<>();

			JComboBox<ShrinkType> typeBox = new JComboBox<>(ShrinkType.values());
			typeBox.setSelectedItem(HashOptions.DEFAULT_EROSION_METHOD);
			typeBox.addActionListener(
					e -> o.setString(HashOptions.SHELL_EROSION_METHOD_KEY,
							typeBox.getSelectedItem().toString()));

			labels.add(new JLabel("Erosion method"));
			fields.add(typeBox);

			SpinnerNumberModel sModel = new SpinnerNumberModel(HashOptions.DEFAULT_SHELL_COUNT, 2,
					20, 1);
			JSpinner spinner = new JSpinner(sModel);
			spinner.addChangeListener(e -> {
				try {
					spinner.commitEdit();
					o.setInt(HashOptions.SHELL_COUNT_INT, (int) sModel.getValue());
				} catch (ParseException e1) {
					LOGGER.fine("Error parsing shell count");
				}

			});

			labels.add(new JLabel("Number of shells"));
			fields.add(spinner);

			this.addLabelTextRows(labels, fields, layout, optionsPanel);

			getContentPane().add(optionsPanel, BorderLayout.CENTER);
		}

		@Override
		protected void setDefaults() {
			o.setInt(HashOptions.SHELL_COUNT_INT, 5);
			o.setString(HashOptions.SHELL_EROSION_METHOD_KEY, ShrinkType.AREA.toString());
		}
	}
}
