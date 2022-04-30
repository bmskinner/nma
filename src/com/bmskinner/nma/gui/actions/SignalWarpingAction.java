package com.bmskinner.nma.gui.actions;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.signals.SignalWarpingMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.signals.SignalManager;
import com.bmskinner.nma.core.DatasetListManager;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.components.panels.DatasetSelectionPanel;
import com.bmskinner.nma.gui.components.panels.SignalGroupSelectionPanel;
import com.bmskinner.nma.gui.dialogs.SubAnalysisSetupDialog;
import com.bmskinner.nma.gui.events.revamp.UIController;
import com.bmskinner.nma.gui.tabs.signals.warping.SignalWarpingRunSettings;

public class SignalWarpingAction extends SingleDatasetResultAction {

	private static final Logger LOGGER = Logger.getLogger(SignalWarpingAction.class.getName());

	private static final @NonNull String PROGRESS_BAR_LABEL = "Warping signals";

	public SignalWarpingAction(@NonNull IAnalysisDataset dataset, @NonNull ProgressBarAcceptor acceptor) {
		super(dataset, PROGRESS_BAR_LABEL, acceptor);
	}

	@Override
	public void run() {

		SubAnalysisSetupDialog setup = new SignalWarpingSetupDialog(dataset);

		if (setup.isReadyToRun()) { // if dialog was cancelled, skip
			worker = new DefaultAnalysisWorker(setup.getMethod());
			worker.addPropertyChangeListener(this);
			ThreadManager.getInstance().submit(worker);
		} else {
			this.cancel();
		}
		setup.dispose();
	}

	@Override
	public void finished() {
		cleanup();
		UIController.getInstance().fireNuclearSignalUpdated(dataset);
		cancel();
	}

	public class SignalWarpingSetupDialog extends SubAnalysisSetupDialog {

		private static final String DIALOG_TITLE = "Signal warping options";
		private static final String TARGET_DATASET_LBL = "Target dataset";
		private static final String SIGNAL_GROUP_LBL = "Signal group";
		private static final String INCLUDE_CELLS_LBL = "Only include cells with signals";
		private static final String MIN_THRESHOLD_LBL = "Min threshold";
		private static final String BINARISE_LBL = "Binarise";
		private static final String NORMALISE_LBL = "Normalise to counterstain";

		private static final String TARGET_DATASET_TOOLTIP = "Which dataset consensus should we warp onto?";
		private static final String SIGNAL_GROUP_TOOLTIP = "Which signal group to warp?";
		private static final String INCLUDE_CELLS_TOOLTIP = "Tick to use only cells with explicit signals detected";
		private static final String MIN_THRESHOLD_TOOLTIP = "Threshold images to this value before warping";
		private static final String BINARISE_TOOLTIP = "Binarise images so intra-image intensities are not included";
		private static final String NORMALISE_TOOLTIP = "Normalise signal against the counterstain before warping";

		private static final String SOURCE_HELP = "Choose the signals to be warped:";
		private static final String IMAGE_HELP = "Choose how to pre-process images:";
		private static final String TARGET_HELP = "Choose the shape to warp images onto:";

		private DatasetSelectionPanel datasetBoxTwo;

		private SignalGroupSelectionPanel signalBox;

		private JCheckBox cellsWithSignalsBox;
		private JSpinner minThresholdSpinner;
		private JCheckBox binariseBox;
		private JCheckBox normaliseBox;

		public SignalWarpingSetupDialog(final @NonNull IAnalysisDataset dataset) {
			super(dataset, DIALOG_TITLE);
			createUI();
			packAndDisplay();
		}

		/**
		 * Set the default options
		 */
		@Override
		protected void setDefaults() {
			// handled by the options factory
		}

		@Override
		public IAnalysisMethod getMethod() {
			return new SignalWarpingMethod(getFirstDataset(), getOptions());
		}

		@Override
		public SignalWarpingRunSettings getOptions() {
			SignalWarpingRunSettings options = new SignalWarpingRunSettings(dataset, datasetBoxTwo.getSelectedDataset(),
					signalBox.getSelectedID());

			options.setBoolean(SignalWarpingRunSettings.IS_BINARISE_SIGNALS_KEY, binariseBox.isSelected());
			options.setBoolean(SignalWarpingRunSettings.IS_NORMALISE_TO_COUNTERSTAIN_KEY, normaliseBox.isSelected());
			options.setBoolean(SignalWarpingRunSettings.IS_ONLY_CELLS_WITH_SIGNALS_KEY,
					cellsWithSignalsBox.isSelected());
			options.setInt(SignalWarpingRunSettings.MIN_THRESHOLD_KEY, (int) minThresholdSpinner.getValue());

			return options;
		}

		@Override
		protected void createUI() {
			getContentPane().add(createHeader(), BorderLayout.NORTH);
			getContentPane().add(createFooter(), BorderLayout.SOUTH);
			getContentPane().add(createOptionsPanel(), BorderLayout.CENTER);
		}

		private JPanel createOptionsPanel() {
			JPanel panel = new JPanel();
			GridBagLayout layout = new GridBagLayout();
			panel.setLayout(layout);
			panel.setBorder(new EmptyBorder(5, 5, 5, 5));

			List<JLabel> labels = new ArrayList<>();
			List<Component> fields = new ArrayList<>();

			List<IAnalysisDataset> targets = chooseCompatibleTargetDatasets();
			datasetBoxTwo = new DatasetSelectionPanel(targets);
			datasetBoxTwo.setToolTipText(TARGET_DATASET_TOOLTIP);
			datasetBoxTwo.setSelectedDataset(targets.get(0));
			datasetBoxTwo.setMinimumSize(new Dimension(50, 10));

			signalBox = new SignalGroupSelectionPanel(dataset);
			signalBox.setMinimumSize(new Dimension(50, 10));
			signalBox.setToolTipText(SIGNAL_GROUP_TOOLTIP);
			if (!signalBox.hasSelection())
				signalBox.setEnabled(false);

			signalBox.addActionListener(e ->

			{
				SignalManager m = dataset.getCollection().getSignalManager();
				if (!m.hasSignals()) {
					setSignalSettingsEnabled(false);
				} else {
					setSignalSettingsEnabled(true);
					int threshold = dataset.getAnalysisOptions().get()
							.getNuclearSignalOptions(signalBox.getSelectedID()).get().getInt(HashOptions.THRESHOLD);
					minThresholdSpinner.setValue(threshold);
				}
			});

			cellsWithSignalsBox = new JCheckBox("", false);
			cellsWithSignalsBox.setToolTipText(INCLUDE_CELLS_TOOLTIP);

			// Set the initial value to the signal detection threshold of the initial
			// selected signal group
			int threshold = dataset.getAnalysisOptions().isPresent()
					? dataset.getAnalysisOptions().get().getNuclearSignalOptions(signalBox.getSelectedID()).get()
							.getInt(HashOptions.THRESHOLD)
					: 0;
			SpinnerModel minThresholdModel = new SpinnerNumberModel(threshold, 0, 255, 1);
			minThresholdSpinner = new JSpinner(minThresholdModel);
			minThresholdSpinner.setToolTipText(MIN_THRESHOLD_TOOLTIP);

			binariseBox = new JCheckBox("", false);
			binariseBox.setToolTipText(BINARISE_TOOLTIP);

			normaliseBox = new JCheckBox("", false);
			normaliseBox.setToolTipText(NORMALISE_TOOLTIP);

			labels.add(new JLabel(SIGNAL_GROUP_LBL));
			fields.add(signalBox);

			labels.add(new JLabel(MIN_THRESHOLD_LBL));
			fields.add(minThresholdSpinner);

			labels.add(new JLabel(BINARISE_LBL));
			fields.add(binariseBox);

			labels.add(new JLabel(NORMALISE_LBL));
			fields.add(normaliseBox);

			labels.add(new JLabel(INCLUDE_CELLS_LBL));
			fields.add(cellsWithSignalsBox);

			labels.add(new JLabel(TARGET_DATASET_LBL));
			fields.add(datasetBoxTwo);

			addLabelTextRows(labels, fields, layout, panel);

			return panel;
		}

		@Override
		public void setEnabled(boolean b) {
			datasetBoxTwo.setEnabled(b);
			signalBox.setEnabled(b);
			cellsWithSignalsBox.setEnabled(b);
			minThresholdSpinner.setEnabled(b);
			binariseBox.setEnabled(b);
			normaliseBox.setEnabled(b);
		}

		/**
		 * Set the signal settings enabled. Use when switching signal sources
		 * 
		 * @param b
		 */
		private void setSignalSettingsEnabled(boolean b) {
			minThresholdSpinner.setEnabled(b);
			cellsWithSignalsBox.setEnabled(b);
			binariseBox.setEnabled(b);
			normaliseBox.setEnabled(b);
		}

		private List<IAnalysisDataset> chooseCompatibleTargetDatasets() {
			return DatasetListManager.getInstance().getAllDatasets().stream()
					.filter(d -> d.getCollection().hasConsensus()).filter(d -> d.getCollection().getProfileManager()
							.getSegmentCount() == dataset.getCollection().getProfileManager().getSegmentCount())
					.toList();
		}
	}

}
