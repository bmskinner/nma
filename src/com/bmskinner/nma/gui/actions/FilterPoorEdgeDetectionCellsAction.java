package com.bmskinner.nma.gui.actions;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.nucleus.CellCollectionFilteringMethod;
import com.bmskinner.nma.analysis.nucleus.PoorEdgeDetectionProfilePredicate;
import com.bmskinner.nma.components.Version;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.OptionsBuilder;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.dialogs.SubAnalysisSetupDialog;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.events.UserActionEvent;

public class FilterPoorEdgeDetectionCellsAction extends SingleDatasetResultAction {

	private static final Logger LOGGER = Logger
			.getLogger(FilterPoorEdgeDetectionCellsAction.class.getName());

	private static final @NonNull String PROGRESS_BAR_LABEL = "Filtering poor edge detection";

	public FilterPoorEdgeDetectionCellsAction(@NonNull List<IAnalysisDataset> datasets,
			@NonNull CountDownLatch latch,
			@NonNull ProgressBarAcceptor acceptor) {
		super(datasets, PROGRESS_BAR_LABEL, acceptor);
		this.setLatch(latch);
	}

	public FilterPoorEdgeDetectionCellsAction(@NonNull IAnalysisDataset dataset,
			@NonNull ProgressBarAcceptor acceptor) {
		super(dataset, dataset.getName() + ": " + PROGRESS_BAR_LABEL, acceptor);
	}

	@Override
	public void run() {

		LOGGER.fine("Filtering on poor edge detection");
		try {

			RuleSetCollection rsc = dataset.getAnalysisOptions().get().getRuleSetCollection();

			if (rsc.getRulesetVersion().isOlderThan(Version.V_2_2_0)) {
				LOGGER.info(
						() -> "The ruleset for dataset '%s' is from version %s, and does not have edge detection values set. Input manually."
								.formatted(dataset.getName(), rsc.getRulesetVersion()));
			}

			// If options exist in the ruleset, use as default. Otherwise use standard
			// default. Always get confirmation from user.
			SubAnalysisSetupDialog optionsDialog = new FilteringOptionsDialog(dataset);
			if (optionsDialog.isReadyToRun()) {
				HashOptions filterOptions = optionsDialog.getOptions();

				Predicate<ICell> profilePredicate = new PoorEdgeDetectionProfilePredicate(
						filterOptions);

				IAnalysisMethod m = new CellCollectionFilteringMethod(dataset, profilePredicate,
						dataset.getName() + "_filtered");
				worker = new DefaultAnalysisWorker(m, dataset.getCollection().size());
				worker.addPropertyChangeListener(this);
				ThreadManager.getInstance().submit(worker);
			} else {
				cancel();
			}
		} catch (IllegalArgumentException | NoSuchElementException e) {
			LOGGER.log(Level.SEVERE, "Unable to create edge filterer: %s".formatted(e.getMessage()),
					e);
			cancel();
		}
	}

	@Override
	public void finished() {

		try {
			IAnalysisResult r = worker.get();

			LOGGER.fine("Dataset has " + dataset.getChildCount() + " children");

			for (IAnalysisDataset d : r.getDatasets()) {
				for (IAnalysisDataset c : d.getAllChildDatasets()) {

					if (!c.getCollection().hasConsensus())
						UserActionController.getInstance().userActionEventReceived(
								new UserActionEvent(this, UserActionEvent.REFOLD_CONSENSUS,
										c));

					UIController.getInstance().fireDatasetAdded(c);
				}
			}

			if (!hasRemainingDatasetsToProcess()) {
				super.finished();
				countdownLatch();
			} else {
				// otherwise analyse the next item in the list
				cancel(); // remove progress bar
				new FilterPoorEdgeDetectionCellsAction(getRemainingDatasetsToProcess(),
						getLatch().get(),
						progressAcceptors.get(0)).run();

			}

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,
					"Unable to filter cells with poor edge detection: " + e.getMessage(), e);
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
	private class FilteringOptionsDialog extends SubAnalysisSetupDialog {

		private HashOptions options = new OptionsBuilder().build();
		private static final String MIN = "Min angle in a profile";
		private static final String MAX = "Max angle in a profile";
		private static final String DELTA = "Max change in angle";

		public FilteringOptionsDialog(final IAnalysisDataset dataset) {
			super(dataset, "Filtering options");
			setDefaults();

			// If the dataset has ruleset options, override the defaults
			options.set(dataset.getAnalysisOptions().get()
					.getRuleSetCollection().getOtherOptions());
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

			SpinnerModel minValue = new SpinnerNumberModel(20f, // initial
					0f, // min
					360f, // max
					1f); // step
			JSpinner minSpinner = new JSpinner(minValue);

			minSpinner.addChangeListener(
					e -> options.setFloat(RuleSetCollection.RULESET_EDGE_FILTER_THRESHOLD_MIN,
							(float) (double) minValue.getValue()));

			SpinnerModel maxValue = new SpinnerNumberModel(270f, // initial
					0f, // min
					360f, // max
					1f); // step
			JSpinner maxSpinner = new JSpinner(maxValue);

			maxSpinner.addChangeListener(
					e -> options.setFloat(RuleSetCollection.RULESET_EDGE_FILTER_THRESHOLD_MAX,
							(float) (double) maxValue.getValue()));

			SpinnerModel deltaValue = new SpinnerNumberModel(40f, // initial
					0f, // min
					360f, // max
					1f); // step
			JSpinner deltaSpinner = new JSpinner(deltaValue);

			deltaValue.addChangeListener(
					e -> options.setFloat(RuleSetCollection.RULESET_EDGE_FILTER_THRESHOLD_DELTA_MAX,
							(float) (double) deltaValue.getValue()));

			// Add elements in order

			labels.add(new JLabel(MIN));
			fields.add(minSpinner);

			labels.add(new JLabel(MAX));
			fields.add(maxSpinner);

			labels.add(new JLabel(DELTA));
			fields.add(deltaSpinner);

			addLabelTextRows(labels, fields, layout, panel);
			add(panel, BorderLayout.CENTER);
			add(createFooter(), BorderLayout.SOUTH);
		}

		@Override
		protected void setDefaults() {
			options.set(RuleSetCollection.RULESET_EDGE_FILTER_PROFILE,
					ProfileType.ANGLE.toString());
			options.set(RuleSetCollection.RULESET_EDGE_FILTER_THRESHOLD_MIN, 20f);
			options.set(RuleSetCollection.RULESET_EDGE_FILTER_THRESHOLD_MAX, 270f);
			options.set(RuleSetCollection.RULESET_EDGE_FILTER_THRESHOLD_DELTA_MAX, 40f);
		}

	}
}
