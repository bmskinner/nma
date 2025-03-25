package com.bmskinner.nma.gui.actions;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.classification.ClusteringMethod;
import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.datasets.DefaultClusterGroup;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.components.datasets.VirtualDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.OptionsBuilder;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.components.AnnotatedNucleusPanel;
import com.bmskinner.nma.gui.dialogs.SubAnalysisSetupDialog;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.events.UserActionEvent;

public class ClusterManualAction extends SingleDatasetResultAction {

	private static final Logger LOGGER = Logger.getLogger(ClusterManualAction.class.getName());

	private static final @NonNull String PROGRESS_BAR_LABEL = "Clustering cells";

	public ClusterManualAction(@NonNull IAnalysisDataset dataset,
			@NonNull ProgressBarAcceptor acceptor) {
		super(dataset, PROGRESS_BAR_LABEL, acceptor);
	}

	@Override
	public void run() {

		try {
			int maxGroups = dataset.getCollection().getCells().size() - 1; // more would be silly,
																			// fewer restrictive
			int groups = is.requestInt("Number of groups", 2, 2, maxGroups, 1);

			List<String> groupNames = new ArrayList<>();

			for (int i = 1; i <= groups; i++) {
				String name = is.requestString("Name for group " + i);
				groupNames.add(name);
			}

			ManualClusteringDialog mc = new ManualClusteringDialog(dataset, groupNames);

			// blocks until closed
			if (mc.isReadyToRun()) {
				// no action, all handled within the clusterer
			}
			cancel();
		} catch (RequestCancelledException e1) {
			cancel();
		}
	}

	@SuppressWarnings("serial")
	private class ManualClusteringDialog extends SubAnalysisSetupDialog {

		private AnnotatedNucleusPanel panel;
		private JCheckBox isRGBBox = new JCheckBox("All channels");

		public class ManualGroup {

			public final List<ICell> selectedCells = new ArrayList<>();
			public final String clusterName;

			public ManualGroup(String name) {
				clusterName = name;
			}

			/**
			 * Add a new cell to the group
			 * 
			 * @param c
			 * @param time
			 */
			public void addCell(ICell c) {
				selectedCells.add(c);
			}
		}

		/** Nuclei assigned to groups */
		private transient List<ManualGroup> groups = new ArrayList<>();
		private transient List<String> clusterNames = new ArrayList<>();
		private List<JButton> buttons = new ArrayList<>();
		private int cellNumber = 0;

		private final transient List<ICell> cells;

		public ManualClusteringDialog(@NonNull final IAnalysisDataset dataset,
				List<String> groupNames) {
			super(dataset, "Manual clustering");
			this.clusterNames = groupNames;
			cells = new ArrayList<>(dataset.getCollection().getCells());
			Collections.shuffle(cells); // random ordering
			createGroups();
			createUI();
			packAndDisplay();
		}

		@Override
		public IAnalysisMethod getMethod() {
			return null;
		}

		@Override
		public HashOptions getOptions() {
			return null;
		}

		@Override
		protected void createUI() {
			this.setLayout(new BorderLayout());
			this.panel = new AnnotatedNucleusPanel(true);
			openCell(cellNumber);

			this.add(isRGBBox, BorderLayout.NORTH);
			isRGBBox.addChangeListener(e -> openCell(cellNumber)); // reopen on toggle

			this.add(panel, BorderLayout.CENTER);
			this.add(createGroupPanel(clusterNames), BorderLayout.SOUTH);
		}

		@Override
		protected void setDefaults() {
			// no defaults
		}

		protected void createGroups() {
			for (int i = 0; i < clusterNames.size(); i++) {
				groups.add(new ManualGroup(clusterNames.get(i)));
			}
		}

		private void addCollections() {

			// Save the clusters to the dataset
			int clusterNumber = dataset.getMaxClusterGroupNumber() + 1;

			HashOptions op = new OptionsBuilder()
					.withValue(HashOptions.CLUSTER_METHOD_KEY, ClusteringMethod.MANUAL.toString())
					.withValue(HashOptions.CLUSTER_INCLUDE_PROFILE_KEY, false)
					.build();

			IClusterGroup group = new DefaultClusterGroup(
					IClusterGroup.CLUSTER_GROUP_PREFIX + "_" + clusterNumber, op,
					UUID.randomUUID());

			List<IAnalysisDataset> childDatasets = new ArrayList<>();

			for (int i = 0; i < groups.size(); i++) {

				List<ICell> cellList = groups.get(i).selectedCells;

				if (!cellList.isEmpty()) {

					try {

						IAnalysisDataset c = new VirtualDataset(dataset,
								group.getName() + "_" + groups.get(i).clusterName, null,
								cellList);

						IAnalysisDataset d = dataset.addChildDataset(c);

						childDatasets.add(d);

						// set shared counts
						c.getCollection().setSharedCount(dataset.getCollection(),
								c.getCollection().size());
						dataset.getCollection().setSharedCount(c.getCollection(),
								c.getCollection().size());
						LOGGER.fine("Added dataset " + d.getName());

						group.addDataset(d);

					} catch (MissingDataException
							| SegmentUpdateException e) {
						LOGGER.warning("Error copying collection offsets");
						LOGGER.log(Level.SEVERE, "Error in offsetting", e);
					}
				}

			}
			dataset.addClusterGroup(group);

			UserActionController.getInstance().userActionEventReceived(
					new UserActionEvent(this, UserActionEvent.REFOLD_CONSENSUS,
							childDatasets));

			UserActionController.getInstance()
					.userActionEventReceived(
							new UserActionEvent(this, UserActionEvent.SAVE, dataset));

			UIController.getInstance().fireClusterGroupAdded(dataset, group);

			readyToRun = true;
		}

		private void openCell(int i) {

			if (i == cells.size()) {
				LOGGER.fine("Finished manual clustering");
				addCollections();
				dispose();
				return;
			}

			ICell c = cells.get(i);
			try {
				boolean annotateCellImage = false;
				panel.showOnlyCell(c, annotateCellImage, isRGBBox.isSelected());
			} catch (Exception e) {
				LOGGER.fine("Error displaying cell in manual clustering: " + e.getMessage());
			}

			int count = cellNumber + 1;
			setTitle(count + " of " + cells.size());
		}

		private synchronized JPanel createGroupPanel(List<String> groupNames) {
			JPanel p = new JPanel();
			for (int i = 0; i < groupNames.size(); i++) {
				final int index = i;
				JButton b = new JButton(groupNames.get(i));
				buttons.add(b);
				b.addActionListener(e -> {
					groups.get(index).addCell(cells.get(cellNumber));
					if (cellNumber == dataset.getCollection().size())
						addCollections();
					else
						openCell(++cellNumber);
				});
				p.add(b);
			}
			return p;
		}
	}

}
