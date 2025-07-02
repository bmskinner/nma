package com.bmskinner.nma.gui.actions;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.image.CellHistogramCalculationMethod;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.OptionsBuilder;
import com.bmskinner.nma.components.signals.ISignalGroup;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.dialogs.SubAnalysisSetupDialog;
import com.bmskinner.nma.gui.events.UIController;

/**
 * Create an action to run histogram calculation and signal the UI
 * 
 * @author Ben Skinner
 *
 */
public class CalculateCellHistogramAction extends SingleDatasetResultAction {

	private static final @NonNull String PROGRESS_BAR_LABEL = "Calculating histograms";

	public CalculateCellHistogramAction(@NonNull List<IAnalysisDataset> datasets,
			@NonNull CountDownLatch latch,
			@NonNull ProgressBarAcceptor acceptor) {
		super(datasets, PROGRESS_BAR_LABEL, acceptor);
		this.setLatch(latch);
	}

	public CalculateCellHistogramAction(@NonNull IAnalysisDataset dataset,
			@NonNull ProgressBarAcceptor acceptor) {
		super(dataset, dataset.getName() + ": " + PROGRESS_BAR_LABEL, acceptor);
	}

	@Override
	public void run() {

		final SubAnalysisSetupDialog optionsPanel = new HistogramOptionsDialog(dataset);
		if (optionsPanel.isReadyToRun()) {
			final IAnalysisMethod m = optionsPanel.getMethod();
			worker = new DefaultAnalysisWorker(m, dataset.getCollection().size());
			worker.addPropertyChangeListener(this);
			ThreadManager.getInstance().submit(worker);
		}
	}

	@Override
	public void finished() {
		if (!hasRemainingDatasetsToProcess()) {
			super.finished();
			countdownLatch();
			UIController.getInstance().fireNuclearSignalUpdated(dataset);
		} else {
			// otherwise analyse the next item in the list
			cancel(); // remove progress bar
			new CalculateCellHistogramAction(getRemainingDatasetsToProcess(), getLatch().get(),
					progressAcceptors.get(0)).run();

		}

	}

	/**
	 * Configure the exported parameters
	 * 
	 * @author Ben Skinner
	 * @since 1.18.4
	 *
	 */
	private class HistogramOptionsDialog extends SubAnalysisSetupDialog {

		private final HashOptions options = new OptionsBuilder().build();

		public HistogramOptionsDialog(@NonNull final IAnalysisDataset dataset) {
			super(dataset, "Histogram calculation options");
			setDefaults();
			createUI();
			packAndDisplay();
		}

		@Override
		public IAnalysisMethod getMethod() {
			return new CellHistogramCalculationMethod(dataset, options);
		}

		@Override
		public HashOptions getOptions() {
			return options;
		}

		@Override
		protected void createUI() {
			final JPanel panel = new JPanel();
			final GridBagLayout layout = new GridBagLayout();
			panel.setLayout(layout);
			final List<JLabel> labels = new ArrayList<>();
			final List<Component> fields = new ArrayList<>();

			final JCheckBox nucleusBox = new JCheckBox("Pixels in the nucleus channel", true);

			nucleusBox.addActionListener(e -> options.setBoolean(CellularComponent.NUCLEUS, nucleusBox.isSelected()));
			labels.add(new JLabel(CellularComponent.NUCLEUS));
			fields.add(nucleusBox);
			
			
			for(final ISignalGroup sg : dataset.getCollection().getSignalGroups()) {
				final JCheckBox sgBox = new JCheckBox("All pixels in this signal channel within the nucleus", true);
				sgBox.addActionListener(e -> options
						.setBoolean(CellularComponent.NUCLEAR_SIGNAL + sg.getId(), sgBox.isSelected()));
				labels.add(new JLabel(sg.getGroupName()));
				fields.add(sgBox);
			}

			addLabelTextRows(labels, fields, layout, panel);
			add(panel, BorderLayout.CENTER);
			add(createFooter(), BorderLayout.SOUTH);
		}

		@Override
		protected void setDefaults() {
			// Nucleus channel by default
			options.setBoolean(CellularComponent.NUCLEUS, true);

			// Any signal channels by default
			for (final ISignalGroup sg : dataset.getCollection().getSignalGroups()) {

				options
						.setBoolean(CellularComponent.NUCLEAR_SIGNAL + sg.getId(), true);

			}
		}

	}
}
