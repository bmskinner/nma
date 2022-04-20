package com.bmskinner.nuclear_morphology.gui.tabs.editing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.gui.dialogs.AngleWindowSizeExplorer;
import com.bmskinner.nuclear_morphology.gui.events.ProfileWindowProportionUpdateEvent;
import com.bmskinner.nuclear_morphology.gui.events.SegmentMergeEvent;
import com.bmskinner.nuclear_morphology.gui.events.SegmentSplitEvent;
import com.bmskinner.nuclear_morphology.gui.events.SegmentUnmergeEvent;
import com.bmskinner.nuclear_morphology.gui.events.UserActionEvent;
import com.bmskinner.nuclear_morphology.gui.events.revamp.ConsensusUpdatedListener;
import com.bmskinner.nuclear_morphology.gui.events.revamp.ProfilesUpdatedListener;
import com.bmskinner.nuclear_morphology.gui.events.revamp.ScaleUpdatedListener;
import com.bmskinner.nuclear_morphology.gui.events.revamp.SwatchUpdatedListener;
import com.bmskinner.nuclear_morphology.gui.events.revamp.UIController;
import com.bmskinner.nuclear_morphology.gui.events.revamp.UserActionController;
import com.bmskinner.nuclear_morphology.gui.tabs.ChartDetailPanel;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;
import com.bmskinner.nuclear_morphology.visualisation.charts.ConsensusNucleusChartFactory;
import com.bmskinner.nuclear_morphology.visualisation.charts.panels.ConsensusNucleusChartPanel;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptions;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptionsBuilder;

public class DatasetEditingPanel extends ChartDetailPanel
		implements ConsensusUpdatedListener, ScaleUpdatedListener, SwatchUpdatedListener, ProfilesUpdatedListener {
	private static final Logger LOGGER = Logger.getLogger(DatasetEditingPanel.class.getName());

	private static final String PANEL_TITLE_LBL = "Editing";

	private JLabel buttonStateLbl = new JLabel(" ", JLabel.CENTER);

	private JButton segmentButton;
	private JButton mergeButton;
	private JButton unmergeButton;
	private JButton splitButton;
	private JButton windowSizeButton;
	private JButton updatewindowButton;

	private ConsensusNucleusChartPanel consensusChartPanel;

	private static final String STR_SEGMENT_PROFILE = "Resegment all cells";
	private static final String STR_MERGE_SEGMENT = "Merge segments";
	private static final String STR_UNMERGE_SEGMENT = "Unmerge segments";
	private static final String STR_SPLIT_SEGMENT = "Split segment";
	private static final String STR_SET_WINDOW_SIZE = "Set window size";
	private static final String STR_SHOW_WINDOW_SIZES = "Window sizes";

	public DatasetEditingPanel() {
		super(PANEL_TITLE_LBL);
		this.setLayout(new BorderLayout());

		JFreeChart chart = ConsensusNucleusChartFactory.createEmptyChart();
		consensusChartPanel = new ConsensusNucleusChartPanel(chart);
		this.add(consensusChartPanel, BorderLayout.CENTER);

		this.add(createHeader(), BorderLayout.NORTH);

		setButtonsEnabled(false);

		UIController.getInstance().addConsensusUpdatedListener(this);
		UIController.getInstance().addScaleUpdatedListener(this);
		UIController.getInstance().addSwatchUpdatedListener(this);
		UIController.getInstance().addProfilesUpdatedListener(this);
	}

	private JPanel createHeader() {

		JPanel headerPanel = new JPanel();
		headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
		JPanel txtPanel = new JPanel(new FlowLayout());
		txtPanel.add(buttonStateLbl);
		headerPanel.add(txtPanel);

		JPanel panel = new JPanel(new FlowLayout()) {
			@Override
			public void setEnabled(boolean b) {
				super.setEnabled(b);
				for (Component c : this.getComponents()) {
					c.setEnabled(b);
				}
			}
		};

		segmentButton = new JButton(STR_SEGMENT_PROFILE);
		segmentButton.addActionListener(e -> {
			try {
				boolean ok = getInputSupplier().requestApproval(
						"This will resegment the dataset. Manually updated segments will be lost. Continue?",
						"Continue?");
				if (ok) {
					activeDataset().getCollection().getProfileManager().setLockOnAllNucleusSegments(false);
					UserActionController.getInstance().userActionEventReceived(
							new UserActionEvent(this, UserActionEvent.SEGMENTATION_ACTION, List.of(activeDataset())));
				}
			} catch (RequestCancelledException e1) {
			}

		});
		panel.add(segmentButton);

		mergeButton = new JButton(STR_MERGE_SEGMENT);
		mergeButton.addActionListener(e -> mergeAction());
		panel.add(mergeButton);

		unmergeButton = new JButton(STR_UNMERGE_SEGMENT);
		unmergeButton.addActionListener(e -> unmergeAction());
		panel.add(unmergeButton);

		splitButton = new JButton(STR_SPLIT_SEGMENT);
		splitButton.addActionListener(e -> splitAction());
		panel.add(splitButton);

		windowSizeButton = new JButton(STR_SHOW_WINDOW_SIZES);
		windowSizeButton.addActionListener(e -> new AngleWindowSizeExplorer(activeDataset()));
		panel.add(windowSizeButton);

		updatewindowButton = new JButton(STR_SET_WINDOW_SIZE);
		updatewindowButton.addActionListener(e -> updateCollectionWindowSize());
		panel.add(updatewindowButton);

		headerPanel.add(panel);
		return headerPanel;

	}

	@Override
	protected synchronized void updateSingle() {
		super.updateSingle();

		ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets())
				.setScale(GlobalOptions.getInstance().getScale()).setSwatch(GlobalOptions.getInstance().getSwatch())
				.setShowAnnotations(false).setShowXAxis(false).setShowYAxis(false).setTarget(consensusChartPanel)
				.build();

		configureButtons(options);
		setChart(options);

		if (activeDataset() == null) {
			return;
		}

		ICellCollection collection = activeDataset().getCollection();
		consensusChartPanel.restoreAutoBounds();
	}

	@Override
	protected synchronized void updateMultiple() {
		updateNull();
		buttonStateLbl.setText("Cannot update segments across multiple datasets");
	}

	@Override
	protected synchronized void updateNull() {
		buttonStateLbl.setText("No dataset selected");
		consensusChartPanel.setChart(ConsensusNucleusChartFactory.createEmptyChart());
		consensusChartPanel.restoreAutoBounds();
	}

	/**
	 * Enable or disable buttons depending on datasets selected
	 * 
	 * @param options
	 * @throws Exception
	 */
	private synchronized void configureButtons(ChartOptions options) {
		if (options.isMultipleDatasets()) {
			setButtonsEnabled(false);
			return;
		}

		ICellCollection collection = options.firstDataset().getCollection();
		setButtonsEnabled(true);
		if (!collection.getProfileCollection().hasSegments()) {
			unmergeButton.setEnabled(false);
			mergeButton.setEnabled(false);
			return;
		}

		if (!options.firstDataset().isRoot()) // only allow resegmentation of root datasets
			segmentButton.setEnabled(false);

		ISegmentedProfile medianProfile;
		try {
			medianProfile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
					Landmark.REFERENCE_POINT, Stats.MEDIAN);
		} catch (MissingLandmarkException | ProfileException | MissingProfileException e) {
			LOGGER.log(Loggable.STACK, "Error getting profile", e);
			setButtonsEnabled(false);
			return;
		}

		// Don't allow merging below 2 segments
		mergeButton.setEnabled(medianProfile.getSegmentCount() > 2);

		// Check if there are any merged segments
		boolean hasMerges = medianProfile.getSegments().stream().anyMatch(s -> s.hasMergeSources());

		// If there are no merged segments, don't allow unmerging
		unmergeButton.setEnabled(hasMerges);

		// set child dataset options
		if (!options.firstDataset().isRoot()) {
			mergeButton.setEnabled(false);
			unmergeButton.setEnabled(false);
			splitButton.setEnabled(false);
			updatewindowButton.setEnabled(false);
			buttonStateLbl.setText("Cannot alter child dataset segments - try the root dataset");
		} else {
			buttonStateLbl.setText(" ");
		}
	}

	public void setButtonsEnabled(boolean b) {
		segmentButton.setEnabled(b);
		unmergeButton.setEnabled(b);
		mergeButton.setEnabled(b);
		splitButton.setEnabled(b);
		windowSizeButton.setEnabled(b);
		updatewindowButton.setEnabled(b);

	}

	/**
	 * Choose segments to be merged in the given segmented profile
	 * 
	 * @param medianProfile
	 * @throws Exception
	 */
	private void mergeAction() {

		try {
			ISegmentedProfile medianProfile = activeDataset().getCollection().getProfileCollection()
					.getSegmentedProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN);

			List<SegMergeItem> names = new ArrayList<>();

			// Put the names of the mergable segments into a list

			List<IProfileSegment> segList = medianProfile.getOrderedSegments();
			for (int i = 0; i < segList.size() - 1; i++) { // Do not allow merges across the RP
				IProfileSegment seg = segList.get(i);
				SegMergeItem item = new SegMergeItem(seg, seg.nextSegment());
				names.add(item);
			}

			String[] nameArray = names.stream().map(e -> e.toString()).toArray(String[]::new);

			int mergeOption = getInputSupplier().requestOption(nameArray, "Choose segments to merge", "Merge");
			SegMergeItem item = names.get(mergeOption);
//			this.setAnalysing(true);
			LOGGER.fine("User reqested merge of " + item.one().getName() + "(" + item.one.getID() + ") and "
					+ item.two().getName() + "(" + item.two.getID() + ")");
			UserActionController.getInstance().segmentMergeEventReceived(
					new SegmentMergeEvent(this, activeDataset(), item.one.getID(), item.two.getID()));

//			this.setAnalysing(false);

		} catch (RequestCancelledException e) {
			LOGGER.fine("User cancelled segment merge request");
		} catch (MissingLandmarkException | MissingProfileException | ProfileException e1) {
			LOGGER.warning("Unable to get median profile: " + e1.getMessage());
		}
	}

	/**
	 * Unmerge segments in a median profile
	 * 
	 * @param medianProfile
	 * @throws Exception
	 */
	private void unmergeAction() {

		try {
			ISegmentedProfile medianProfile = activeDataset().getCollection().getProfileCollection()
					.getSegmentedProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN);

			List<IProfileSegment> names = new ArrayList<>();

			// Put the names of the mergable segments into a list
			for (IProfileSegment seg : medianProfile.getSegments()) {
				if (seg.hasMergeSources()) {
					names.add(seg);
				}
			}
			IProfileSegment[] nameArray = names.toArray(new IProfileSegment[0]);
			String[] options = Arrays.stream(nameArray).map(s -> s.getName()).toArray(String[]::new);

			int option = getInputSupplier().requestOption(options, "Choose merged segment to unmerge",
					"Unmerge segment");

			UserActionController.getInstance().segmentUnmergeEventReceived(
					new SegmentUnmergeEvent(this, activeDataset(), nameArray[option].getID()));
		} catch (RequestCancelledException e) {
			LOGGER.fine("User cancelled segment unmerge request");
		} catch (MissingLandmarkException | MissingProfileException | ProfileException e1) {
			LOGGER.warning("Unable to get median profile: " + e1.getMessage());
		}
	}

	private void splitAction() {
		try {
			ISegmentedProfile medianProfile = activeDataset().getCollection().getProfileCollection()
					.getSegmentedProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN);
			IProfileSegment[] nameArray = medianProfile.getSegments().toArray(new IProfileSegment[0]);

			String[] options = Arrays.stream(nameArray).map(IProfileSegment::getName).toArray(String[]::new);

			int option = getInputSupplier().requestOptionAllVisible(options, "Choose segment to split",
					STR_SPLIT_SEGMENT);

			UserActionController.getInstance()
					.segmentSplitEventReceived(new SegmentSplitEvent(this, activeDataset(), nameArray[option].getID()));
		} catch (RequestCancelledException e) {
			LOGGER.fine("User cancelled segment split request");
		} catch (MissingLandmarkException | MissingProfileException | ProfileException e1) {
			LOGGER.warning("Unable to get median profile: " + e1.getMessage());
		}
	}

	private void updateCollectionWindowSize() {

		double windowSizeActual = IAnalysisOptions.DEFAULT_WINDOW_PROPORTION;
		Optional<IAnalysisOptions> op = activeDataset().getAnalysisOptions();
		if (op.isPresent())
			windowSizeActual = op.get().getProfileWindowProportion();

		try {
			double windowSize = getInputSupplier().requestDouble("Select new window size", windowSizeActual, 0.01, 0.1,
					0.01);
			UserActionController.getInstance().profileWindowProportionUpdateEventReceived(
					new ProfileWindowProportionUpdateEvent(this, activeDataset(), windowSize));
		} catch (RequestCancelledException e) {
			LOGGER.fine("User cancelled window proportion update request");
		}
	}

	private record SegMergeItem(IProfileSegment one, IProfileSegment two) {
		@Override
		public String toString() {
			return one.getName() + " - " + two.getName();
		}
	}

	@Override
	protected JFreeChart createPanelChartType(@NonNull ChartOptions options) {
		// TODO Auto-generated method stub
		return new ConsensusNucleusChartFactory(options).makeEditableConsensusChart();
	}

	@Override
	public void consensusUpdated(List<IAnalysisDataset> datasets) {
		refreshCache(datasets);
	}

	@Override
	public void consensusUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}

	@Override
	public void consensusFillStateUpdated() {
		refreshCache();
	}

	@Override
	public void scaleUpdated(List<IAnalysisDataset> datasets) {
		refreshCache(datasets);
	}

	@Override
	public void scaleUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}

	@Override
	public void scaleUpdated() {
		update();
	}

	@Override
	public void swatchUpdated() {
		update();
	}

	@Override
	public void profilesUpdated(List<IAnalysisDataset> datasets) {
		refreshCache(datasets);

	}

	@Override
	public void profilesUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}

}
