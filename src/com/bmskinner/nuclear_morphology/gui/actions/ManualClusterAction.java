package com.bmskinner.nuclear_morphology.gui.actions;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.classification.ClusteringMethod;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.DefaultClusterGroup;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.datasets.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.datasets.VirtualDataset;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsBuilder;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.components.AnnotatedNucleusPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.SubAnalysisSetupDialog;
import com.bmskinner.nuclear_morphology.gui.events.UserActionEvent;
import com.bmskinner.nuclear_morphology.gui.events.revamp.UIController;
import com.bmskinner.nuclear_morphology.gui.events.revamp.UserActionController;
import com.bmskinner.nuclear_morphology.logging.Loggable;

public class ManualClusterAction extends SingleDatasetResultAction {

	private static final Logger LOGGER = Logger.getLogger(ManualClusterAction.class.getName());

	private static final @NonNull String PROGRESS_BAR_LABEL = "Clustering cells";

	public ManualClusterAction(IAnalysisDataset dataset, @NonNull ProgressBarAcceptor acceptor) {
		super(dataset, PROGRESS_BAR_LABEL, acceptor);
	}

	@Override
	public void run() {

		try {
			int maxGroups = dataset.getCollection().getCells().size() - 1; // more would be silly, fewer restrictive
			int groups = is.requestInt("Number of groups", 2, 2, maxGroups, 1);

			List<String> groupNames = new ArrayList<>();

			for (int i = 1; i <= groups; i++) {
				String name = is.requestString("Name for group " + i);
				groupNames.add(name);
			}

			ManualClusteringDialog mc = new ManualClusteringDialog(dataset, groupNames);

			// blocks until closed
			if (mc.isReadyToRun()) {
				UserActionController.getInstance()
						.userActionEventReceived(new UserActionEvent(this, UserActionEvent.SAVE, List.of(dataset)));

				UIController.getInstance().fireClusterGroupsUpdated(dataset);
			}
			cancel();
		} catch (RequestCancelledException e1) {
			cancel();
			return;
		}
	}

	private class ManualClusteringDialog extends SubAnalysisSetupDialog {

		private AnnotatedNucleusPanel panel;

		public class ManualGroup {

			private List<ICell> selectedCells = new ArrayList<>(96);
			public final String groupName;

			public ManualGroup(String name) {
				groupName = name;
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

			/**
			 * Create a new virtual collection from the cells in the group
			 * 
			 * @param name
			 * @return
			 */
			public ICellCollection toCollection(String name) {
				ICellCollection coll = new VirtualDataset(dataset, name);

				for (ICell c : selectedCells) {
					coll.addCell(c);
				}
				return coll;
			}
		}

		/** Nuclei assigned to groups */
		private List<ManualGroup> groups = new ArrayList<>();
		List<String> groupNames = new ArrayList<>();
		private List<JButton> buttons = new ArrayList<>();
		private int cellNumber = 0;

		private final List<ICell> cells;

		public ManualClusteringDialog(@NonNull final IAnalysisDataset dataset, List<String> groupNames) {
			super(dataset, "Manual clustering");
			this.groupNames = groupNames;
			cells = new ArrayList<>(dataset.getCollection().getCells());
			Collections.shuffle(cells); // random ordering
			createGroups();
			createUI();
			packAndDisplay();
		}

		@Override
		public IAnalysisMethod getMethod() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public HashOptions getOptions() {
			return null;
		}

		@Override
		protected void createUI() {
			this.panel = new AnnotatedNucleusPanel();
			openCell(0);
			this.setLayout(new BorderLayout());
			this.add(panel, BorderLayout.CENTER);
			this.add(createGroupPanel(groupNames), BorderLayout.SOUTH);
		}

		@Override
		protected void setDefaults() {
			// TODO Auto-generated method stub
		}

		protected void createGroups() {
			for (int i = 0; i < groupNames.size(); i++) {
				groups.add(new ManualGroup(groupNames.get(i)));
			}
		}

		private void addCollections() {

			// Save the clusters to the dataset
			int clusterNumber = dataset.getMaxClusterGroupNumber() + 1;

			HashOptions op = new OptionsBuilder()
					.withValue(HashOptions.CLUSTER_METHOD_KEY, ClusteringMethod.MANUAL.toString())
					.withValue(HashOptions.CLUSTER_INCLUDE_PROFILE_KEY, false).build();

			IClusterGroup group = new DefaultClusterGroup(IClusterGroup.CLUSTER_GROUP_PREFIX + "_" + clusterNumber, op);

			for (int i = 0; i < groups.size(); i++) {

				ICellCollection coll = groups.get(i)
						.toCollection("Manual_cluster_" + i + "_" + groups.get(i).groupName);

				if (coll.hasCells()) {

					try {
						dataset.getCollection().getProfileManager().copySegmentsAndLandmarksTo(coll);
					} catch (ProfileException | MissingProfileException e) {
						LOGGER.warning("Error copying collection offsets");
						LOGGER.log(Loggable.STACK, "Error in offsetting", e);
					}

					group.addDataset(coll);
					coll.setName(group.getName() + "_" + coll.getName());

					dataset.addChildCollection(coll);

					// attach the clusters to their parent collection
					IAnalysisDataset clusterDataset = dataset.getChildDataset(coll.getId());

					// set shared counts
					coll.setSharedCount(dataset.getCollection(), coll.size());
					dataset.getCollection().setSharedCount(coll, coll.size());
				}

			}
			dataset.addClusterGroup(group);
			UIController.getInstance().fireClusterGroupAdded(dataset, group);
			readyToRun = true;
		}

		private void openCell(int i) {

			if (i == cells.size()) {
				LOGGER.fine("Finished manual clustering");
				addCollections();

//        		fireInterfaceEvent(InterfaceMethod.REFRESH_POPULATIONS);
				dispose();
				return;
			}

			ICell c = cells.get(i);
			try {
				boolean annotateCellImage = false;
				panel.showOnlyCell(c, annotateCellImage);
			} catch (Exception e) {
				e.printStackTrace();
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
