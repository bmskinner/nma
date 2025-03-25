package com.bmskinner.nma.gui.actions;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.image.CellHistogramCalculationMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.OptionsBuilder;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.dialogs.SubAnalysisSetupDialog;
import com.bmskinner.nma.io.ImageImporter;

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

		SubAnalysisSetupDialog optionsPanel = new HistogramOptionsDialog(dataset);
		if (optionsPanel.isReadyToRun()) {
			IAnalysisMethod m = optionsPanel.getMethod();
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

		private HashOptions options = new OptionsBuilder().build();

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
			JPanel panel = new JPanel();
			GridBagLayout layout = new GridBagLayout();
			panel.setLayout(layout);
			List<JLabel> labels = new ArrayList<>();
			List<Component> fields = new ArrayList<>();

			JComboBox<String> channelBox = new JComboBox<>(
					channelOptionStrings);

			channelBox.addActionListener(e -> {

				int channel = 0;
				switch (channelBox.getSelectedItem().toString()) {
				case "Red":
					channel = ImageImporter.RGB_RED;
					break;
				case "Green":
					channel = ImageImporter.RGB_GREEN;
					break;
				default:
					channel = ImageImporter.RGB_BLUE;
				}
				options.set(HashOptions.CHANNEL, channel);
			});

			labels.add(new JLabel("Channel"));
			fields.add(channelBox);

			addLabelTextRows(labels, fields, layout, panel);
			add(panel, BorderLayout.CENTER);
			add(createFooter(), BorderLayout.SOUTH);
		}

		@Override
		protected void setDefaults() {
			options.set(HashOptions.CHANNEL,
					dataset.getAnalysisOptions().get().getNucleusDetectionOptions().get()
							.getInt(HashOptions.CHANNEL));
		}

	}
}
