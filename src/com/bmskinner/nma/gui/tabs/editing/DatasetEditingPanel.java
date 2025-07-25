package com.bmskinner.nma.gui.tabs.editing;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleEdge;

import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.Landmark;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.gui.components.panels.ConsensusNucleusChartPanel;
import com.bmskinner.nma.gui.dialogs.AngleWindowSizeExplorer;
import com.bmskinner.nma.gui.events.ConsensusUpdatedListener;
import com.bmskinner.nma.gui.events.LandmarkUpdateEvent;
import com.bmskinner.nma.gui.events.ProfileWindowProportionUpdateEvent;
import com.bmskinner.nma.gui.events.ProfilesUpdatedListener;
import com.bmskinner.nma.gui.events.ScaleUpdatedListener;
import com.bmskinner.nma.gui.events.SegmentMergeEvent;
import com.bmskinner.nma.gui.events.SegmentSplitEvent;
import com.bmskinner.nma.gui.events.SegmentStartIndexUpdateEvent;
import com.bmskinner.nma.gui.events.SegmentUnmergeEvent;
import com.bmskinner.nma.gui.events.SwatchUpdatedListener;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.events.UserActionEvent;
import com.bmskinner.nma.gui.tabs.ChartDetailPanel;
import com.bmskinner.nma.stats.Stats;
import com.bmskinner.nma.visualisation.ChartComponents;
import com.bmskinner.nma.visualisation.charts.ConsensusNucleusChartFactory;
import com.bmskinner.nma.visualisation.charts.overlays.EllipticalOverlay;
import com.bmskinner.nma.visualisation.charts.overlays.EllipticalOverlayObject;
import com.bmskinner.nma.visualisation.charts.overlays.ShapeOverlay;
import com.bmskinner.nma.visualisation.charts.overlays.ShapeOverlayObject;
import com.bmskinner.nma.visualisation.options.ChartOptions;
import com.bmskinner.nma.visualisation.options.ChartOptionsBuilder;

@SuppressWarnings("serial")
public class DatasetEditingPanel extends ChartDetailPanel
		implements ConsensusUpdatedListener, ScaleUpdatedListener,
		SwatchUpdatedListener, ProfilesUpdatedListener, ChartMouseListener {
	private static final Logger LOGGER = Logger.getLogger(DatasetEditingPanel.class.getName());

	private static final String PANEL_TITLE_LBL = "Editing";
	private static final String PANEL_DESC_LBL = "Change landmarks or segmentation patterns";

	private final JLabel buttonStateLbl = new JLabel(" ", SwingConstants.CENTER);

	private JButton segmentButton;
	private JButton mergeButton;
	private JButton unmergeButton;
	private JButton splitButton;
	private JButton windowSizeButton;
	private JButton updatewindowButton;

	private final ConsensusNucleusChartPanel chartPanel;

	private final ShapeOverlay lmOverlay = new ShapeOverlay();

	private final EllipticalOverlay bOverlay = new EllipticalOverlay(new EllipticalOverlayObject(
			Double.NaN, 2, Double.NaN, 2,
			ChartComponents.MARKER_STROKE, Color.decode("#0066CC"), Color.decode("#0066CC")));

	private static final String STR_SEGMENT_PROFILE = "Resegment all cells";
	private static final String STR_MERGE_SEGMENT = "Merge segments";
	private static final String STR_UNMERGE_SEGMENT = "Unmerge segments";
	private static final String STR_SPLIT_SEGMENT = "Split segment";
	private static final String STR_SET_WINDOW_SIZE = "Set window size";
	private static final String STR_SHOW_WINDOW_SIZES = "Explore window size";

	/**
	 * Create the editing panel
	 */
	public DatasetEditingPanel() {
		super(PANEL_TITLE_LBL, PANEL_DESC_LBL);
		this.setLayout(new BorderLayout());

		final JFreeChart chart = ConsensusNucleusChartFactory.createEmptyChart();
		chartPanel = new ConsensusNucleusChartPanel(chart);
		chartPanel.addOverlay(lmOverlay);
		chartPanel.addOverlay(bOverlay);
		chartPanel.addChartMouseListener(this);
		this.add(chartPanel, BorderLayout.CENTER);

		this.add(createHeader(), BorderLayout.NORTH);

		setButtonsEnabled(false);

		uiController.addConsensusUpdatedListener(this);
		uiController.addScaleUpdatedListener(this);
		uiController.addSwatchUpdatedListener(this);
		uiController.addProfilesUpdatedListener(this);
	}

	private JPanel createHeader() {

		final JPanel headerPanel = new JPanel();
		headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
		final JPanel txtPanel = new JPanel(new FlowLayout());
		txtPanel.add(buttonStateLbl);
		headerPanel.add(txtPanel);

		final JPanel panel = new JPanel(new FlowLayout()) {
			@Override
			public void setEnabled(boolean b) {
				super.setEnabled(b);
				for (final Component c : this.getComponents()) {
					c.setEnabled(b);
				}
			}
		};

		segmentButton = new JButton(STR_SEGMENT_PROFILE);
		segmentButton.addActionListener(e -> {
			try {
				final boolean ok = getInputSupplier().requestApproval(
						"This will resegment the dataset. Manually updated segments will be lost. Continue?",
						"Continue?");
				if (ok) {
					activeDataset().getCollection().getProfileManager()
							.setLockOnAllNucleusSegments(false);
					UserActionController.getInstance().userActionEventReceived(
							new UserActionEvent(this, UserActionEvent.SEGMENTATION_ACTION,
									List.of(activeDataset())));
				}
			} catch (final RequestCancelledException e1) {
				// user cancelled, no action
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

		updatewindowButton = new JButton(STR_SET_WINDOW_SIZE);
		updatewindowButton.addActionListener(e -> updateCollectionWindowSize());
		panel.add(updatewindowButton);

		windowSizeButton = new JButton(STR_SHOW_WINDOW_SIZES);
		windowSizeButton.addActionListener(e -> new AngleWindowSizeExplorer(activeDataset()));
		panel.add(windowSizeButton);

		headerPanel.add(panel);
		return headerPanel;

	}

	@Override
	protected synchronized void updateSingle() {
		super.updateSingle();

		final ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets())
				.setScale(GlobalOptions.getInstance().getDisplayScale())
				.setSwatch(GlobalOptions.getInstance().getSwatch())
				.setShowAnnotations(false)
				.setShowXAxis(false)
				.setShowYAxis(false)
				.setTarget(chartPanel).build();

		configureButtons(options);
		setChart(options);

		if (activeDataset() == null)
			return;

		chartPanel.restoreAutoBounds();
	}

	@Override
	protected synchronized void updateMultiple() {
		updateNull();
		buttonStateLbl.setText("Cannot update segments across multiple datasets");
	}

	@Override
	protected synchronized void updateNull() {
		buttonStateLbl.setText("No dataset selected");
		chartPanel.setChart(ConsensusNucleusChartFactory.createEmptyChart());
		chartPanel.restoreAutoBounds();
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

		if (!options.hasDatasets()) {
			setButtonsEnabled(false);
			return;
		}

		final ICellCollection collection = options.firstDataset().getCollection();
		setButtonsEnabled(true);
		if (!collection.getProfileCollection().hasSegments()) {
			unmergeButton.setEnabled(false);
			mergeButton.setEnabled(false);
			return;
		}

		if (!options.firstDataset().isRoot()) { // only allow resegmentation of root datasets
			segmentButton.setEnabled(false);
		}

		try {
			final ISegmentedProfile medianProfile = collection.getProfileCollection().getSegmentedProfile(
					ProfileType.ANGLE,
					OrientationMark.REFERENCE, Stats.MEDIAN);

			// Don't allow merging below 2 segments
			mergeButton.setEnabled(medianProfile.getSegmentCount() > 2);

			// Check if there are any merged segments
			// If there are no merged segments, don't allow unmerging
			unmergeButton.setEnabled(
					medianProfile.getSegments().stream()
							.anyMatch(IProfileSegment::hasMergeSources));

			// set child dataset options
			if (!options.firstDataset().isRoot()) {
				mergeButton.setEnabled(false);
				unmergeButton.setEnabled(false);
				splitButton.setEnabled(false);
				updatewindowButton.setEnabled(false);
				windowSizeButton.setEnabled(false);
				buttonStateLbl.setText(
						"Cannot merge, unmerge or split child dataset segments - try the root dataset");
			} else {
				buttonStateLbl
						.setText("Click a point on the border to update landmarks or segments");
			}

		} catch (MissingDataException | SegmentUpdateException e) {
			LOGGER.log(Level.SEVERE, "Error getting profile", e);
			setButtonsEnabled(false);
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
			final ISegmentedProfile medianProfile = activeDataset().getCollection().getProfileCollection()
					.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE,
							Stats.MEDIAN);

			final List<SegMergeItem> names = new ArrayList<>();

			// Put the names of the mergable segments into a list

			final List<IProfileSegment> segList = medianProfile.getSegments();
			for (int i = 0; i < segList.size() - 1; i++) { // Do not allow merges across the RP
				final IProfileSegment seg = segList.get(i);
				final SegMergeItem item = new SegMergeItem(seg, seg.nextSegment());
				names.add(item);
			}

			final String[] nameArray = names.stream().map(SegMergeItem::toString).toArray(String[]::new);

			final int mergeOption = getInputSupplier().requestOption(nameArray,
					"Choose segments to merge", "Merge");
			final SegMergeItem item = names.get(mergeOption);
			this.setAnalysing(true);
			UserActionController.getInstance().segmentMergeEventReceived(
					new SegmentMergeEvent(this, activeDataset(), item.one.getID(),
							item.two.getID()));

		} catch (final RequestCancelledException e) {
			LOGGER.fine("User cancelled segment merge request");
		} catch (MissingDataException | SegmentUpdateException e1) {
			LOGGER.warning(() -> "Unable to get median profile: %s".formatted(e1.getMessage()));
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
			final ISegmentedProfile medianProfile = activeDataset().getCollection().getProfileCollection()
					.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE,
							Stats.MEDIAN);

			final List<IProfileSegment> names = new ArrayList<>();

			// Put the names of the mergable segments into a list
			for (final IProfileSegment seg : medianProfile.getSegments()) {
				if (seg.hasMergeSources()) {
					names.add(seg);
				}
			}
			final IProfileSegment[] nameArray = names.toArray(new IProfileSegment[0]);
			final String[] options = Arrays.stream(nameArray).map(IProfileSegment::getName)
					.toArray(String[]::new);

			final int option = getInputSupplier().requestOption(options,
					"Choose merged segment to unmerge",
					"Unmerge segment");
			this.setAnalysing(true);
			UserActionController.getInstance().segmentUnmergeEventReceived(
					new SegmentUnmergeEvent(this, activeDataset(), nameArray[option].getID()));
		} catch (final RequestCancelledException e) {
			LOGGER.fine("User cancelled segment unmerge request");
		} catch (MissingDataException | SegmentUpdateException e1) {
			LOGGER.warning("Unable to get median profile: " + e1.getMessage());
		}
	}

	private void splitAction() {
		try {
			final ISegmentedProfile medianProfile = activeDataset().getCollection().getProfileCollection()
					.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE,
							Stats.MEDIAN);
			final IProfileSegment[] nameArray = medianProfile.getSegments()
					.toArray(new IProfileSegment[0]);

			final String[] options = Arrays.stream(nameArray).map(IProfileSegment::getName)
					.toArray(String[]::new);

			final int option = getInputSupplier().requestOptionAllVisible(options,
					"Choose segment to split",
					STR_SPLIT_SEGMENT);
			this.setAnalysing(true);
			UserActionController.getInstance()
					.segmentSplitEventReceived(new SegmentSplitEvent(this, activeDataset(),
							nameArray[option].getID()));
		} catch (final RequestCancelledException e) {
			LOGGER.fine("User cancelled segment split request");
		} catch (MissingDataException | SegmentUpdateException e1) {
			LOGGER.warning("Unable to get median profile: " + e1.getMessage());
		}
	}

	private void updateCollectionWindowSize() {

		double windowSizeActual = HashOptions.DEFAULT_PROFILE_WINDOW;
		final Optional<IAnalysisOptions> op = activeDataset().getAnalysisOptions();
		if (op.isPresent()) {
			windowSizeActual = op.get().getProfileWindowProportion();
		}

		try {
			final double windowSize = getInputSupplier().requestDouble("Select new window size",
					windowSizeActual, 0.01, 0.1,
					0.01);
			this.setAnalysing(true);
			UserActionController.getInstance().profileWindowProportionUpdateEventReceived(
					new ProfileWindowProportionUpdateEvent(this, activeDataset(), windowSize));
		} catch (final RequestCancelledException e) {
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
	public void globalPaletteUpdated() {
		update();
	}

	@Override
	public void colourUpdated(IAnalysisDataset dataset) {
		// No action - we never display dataset colours in this chart
	}

	@Override
	public void profilesUpdated(List<IAnalysisDataset> datasets) {
		refreshCache(datasets);

	}

	@Override
	public void profilesUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}

	@Override
	public void chartMouseClicked(ChartMouseEvent event) {

		if (!activeDataset().getCollection().hasConsensus())
			return;

		// Get the mouse location on the chart
		final Rectangle2D dataArea = this.chartPanel.getScreenDataArea();
		final JFreeChart chart = event.getChart();
		final XYPlot plot = (XYPlot) chart.getPlot();
		final ValueAxis xAxis = plot.getDomainAxis();
		final ValueAxis yAxis = plot.getRangeAxis();
		final double x = xAxis.java2DToValue(event.getTrigger().getX(), dataArea, RectangleEdge.BOTTOM);
		final double y = yAxis.java2DToValue(event.getTrigger().getY(), dataArea, RectangleEdge.LEFT);

		final double range = Math.min(xAxis.getRange().getLength(), yAxis.getRange().getLength());
		try {

			// Get the closest border point, and set the overlay if close enough
			final Nucleus n = activeDataset().getCollection().getConsensus();

			final IPoint clicked = new FloatPoint(x, y);

			final Optional<IPoint> bp = n.getBorderList().stream()
					.filter(p -> p.getLengthTo(clicked) < range / 50)
					.min((p1, p2) -> p1.getLengthTo(clicked) < p2.getLengthTo(clicked) ? -1 : 1);

			if (bp.isPresent()) {

				// Make the popup to change segments or landmarks
				final JPopupMenu popup = createPopup(bp.get());
				popup.show(chartPanel, event.getTrigger().getX(), event.getTrigger().getY());
			}

		} catch (final Exception e) {
			LOGGER.fine("Unable to create popup menu: " + e.getMessage());
		}
	}

	private synchronized JPopupMenu createPopup(IPoint point) {
		final JPopupMenu popupMenu = new JPopupMenu("Popup");

		addSegmentsToPopup(popupMenu, point);

		popupMenu.addSeparator();

		addLandmarksToPopup(popupMenu, point);

		return popupMenu;
	}

	/**
	 * Add segment update options to the popup menu, coloured by segment
	 * 
	 * @param popupMenu
	 * @param point
	 */
	private void addSegmentsToPopup(JPopupMenu popupMenu, IPoint point) {
		try {

			final Nucleus n = activeDataset().getCollection().getConsensus();

			// Indexes in the consensus
			final int rawIndex = n.getBorderIndex(point);
			final int rpIndex = n.getBorderIndex(OrientationMark.REFERENCE);

			// Get the index of the clicked point in the RP-indexed consensus profile
			final int index = n.wrapIndex(rawIndex - rpIndex);

			// Convert to the index in the dataset median profile (may have different
			// length)
			final double fIndex = index / (float) n.getBorderLength();
			final int medianIndex = (int) (activeDataset().getCollection().getMedianArrayLength()
					* fIndex);

			// Get the relevant segments
			final IProfileSegment seg = n.getProfile(ProfileType.ANGLE).getSegmentContaining(rawIndex);
			final IProfileSegment prev = seg.prevSegment();
			final IProfileSegment next = seg.nextSegment();

			final JMenuItem prevItem = new JMenuItem("Extend " + prev.getName() + " to here");
			prevItem.setBorder(
					BorderFactory.createLineBorder(ColourSelecter.getColor(prev.getPosition()), 3));
			prevItem.setBorderPainted(true);

			prevItem.addActionListener(e -> {
				setAnalysing(true);
				LOGGER.fine(
						String.format("Updating segment %s start to %d", seg.getID(), medianIndex));
				UserActionController.getInstance().segmentStartIndexUpdateEventReceived(
						new SegmentStartIndexUpdateEvent(this, activeDataset(), seg.getID(),
								medianIndex - 1));
			});
			popupMenu.add(prevItem);

			popupMenu.add(Box.createVerticalStrut(2)); // stop borders touching

			final JMenuItem nextItem = new JMenuItem("Extend " + next.getName() + " to here");
			nextItem.setBorder(
					BorderFactory.createLineBorder(ColourSelecter.getColor(next.getPosition()), 2));
			nextItem.setBorderPainted(true);

			nextItem.addActionListener(e -> {
				LOGGER.fine(String.format("Updating segment %s start to %d", next.getID(),
						medianIndex));
				setAnalysing(true);
				UserActionController.getInstance().segmentStartIndexUpdateEventReceived(
						new SegmentStartIndexUpdateEvent(this, activeDataset(), next.getID(),
								medianIndex - 1));

			});
			popupMenu.add(nextItem);
		} catch (MissingDataException
				| ComponentCreationException | SegmentUpdateException e) {
			LOGGER.log(Level.SEVERE, "Cannot create segment popup", e);
		}
	}

	/**
	 * Add tags to the popup menu
	 * 
	 * @param popupMenu
	 */
	private void addLandmarksToPopup(JPopupMenu popupMenu, IPoint point) {
		try {
			final Nucleus n = activeDataset().getCollection().getConsensus();

			// Indexes in the consensus
			final int rawIndex = n.getBorderIndex(point);
			final int rpIndex = n.getBorderIndex(OrientationMark.REFERENCE);

			// Get the index of the clicked point in the RP-indexed consensus profile
			final int index = n.wrapIndex(rawIndex - rpIndex);

			// Convert to the index in the dataset median profile (may have different
			// length)
			final double fIndex = index / (float) n.getBorderLength();
			final int medianIndex = (int) (activeDataset().getCollection().getMedianArrayLength()
					* fIndex);

			final List<Landmark> tags = activeDataset().getCollection().getProfileCollection()
					.getLandmarks();

			Collections.sort(tags);

			for (final Landmark lm : tags) {

				// Colour the menu item by tag colour
				final JMenuItem item = new JMenuItem("Move " + lm.toString().toLowerCase() + " here");
				item.setBackground(Color.DARK_GRAY);
				item.setBorderPainted(true);
				item.setForeground(Color.WHITE);
				item.setOpaque(true);

				item.addActionListener(a -> {
					LOGGER.fine(String.format("Request update of %s to %d (%d)", lm, index,
							medianIndex));
					setAnalysing(true);
					UserActionController.getInstance().landmarkUpdateEventReceived(
							new LandmarkUpdateEvent(this, activeDataset(), lm, medianIndex - 1));
				});
				popupMenu.add(item);
				popupMenu.add(Box.createVerticalStrut(2)); // stop borders touching
			}
		} catch (MissingLandmarkException | ComponentCreationException e) {
			LOGGER.log(Level.SEVERE, "Cannot create landmark popup", e);
		}
	}

	@Override
	public void chartMouseMoved(ChartMouseEvent event) {

		if (this.isMultipleDatasets() || !this.hasDatasets())
			return;

		setBorderPointHighlight(event);
		setLandmarkHighlight(event);

	}

	private void setBorderPointHighlight(ChartMouseEvent event) {

		if (!activeDataset().getCollection().hasConsensus())
			return;

		// Get the mouse location on the chart
		final Rectangle2D dataArea = this.chartPanel.getScreenDataArea();
		final JFreeChart chart = event.getChart();
		final XYPlot plot = (XYPlot) chart.getPlot();
		final ValueAxis xAxis = plot.getDomainAxis();
		final ValueAxis yAxis = plot.getRangeAxis();
		final double x = xAxis.java2DToValue(event.getTrigger().getX(), dataArea, RectangleEdge.BOTTOM);
		final double y = yAxis.java2DToValue(event.getTrigger().getY(), dataArea, RectangleEdge.LEFT);

		final double range = Math.min(xAxis.getRange().getLength(), yAxis.getRange().getLength());
		final double size = range / 100;
		try {

			// Get the closest border point, and set the overlay if close enough
			final Nucleus n = activeDataset().getCollection().getConsensus();

			final IPoint clicked = new FloatPoint(x, y);

			final Optional<IPoint> bp = n.getBorderList().stream()
					.filter(p -> p.getLengthTo(clicked) < range / 50)
					.min((p1, p2) -> p1.getLengthTo(clicked) < p2.getLengthTo(clicked) ? -1 : 1);

			if (bp.isPresent()) {
				bOverlay.getEllipse().setXValue(bp.get().getX());
				bOverlay.getEllipse().setYValue(bp.get().getY());

			} else {
				bOverlay.getEllipse().setXValue(Double.NaN);
				bOverlay.getEllipse().setYValue(Double.NaN);
				bOverlay.getEllipse().setXRadius(size);
				bOverlay.getEllipse().setYRadius(size);
			}

		} catch (final Exception e) {
			LOGGER.fine("Unable to draw border highlights: " + e.getMessage());
		}
	}

	private void setLandmarkHighlight(ChartMouseEvent event) {

		if (!activeDataset().getCollection().hasConsensus())
			return;

		// Get the mouse location on the chart
		final Rectangle2D dataArea = this.chartPanel.getScreenDataArea();
		final JFreeChart chart = event.getChart();
		final XYPlot plot = (XYPlot) chart.getPlot();
		final ValueAxis xAxis = plot.getDomainAxis();
		final ValueAxis yAxis = plot.getRangeAxis();
		final double x = xAxis.java2DToValue(event.getTrigger().getX(), dataArea, RectangleEdge.BOTTOM);
		final double y = yAxis.java2DToValue(event.getTrigger().getY(), dataArea, RectangleEdge.LEFT);

		final double range = Math.min(xAxis.getRange().getLength(), yAxis.getRange().getLength());
		final int textSize = (int) (range / 30);
		final double distanceLimit = range / 20;
		try {

			// Get the closest landmarks, and set the overlay if close enough
			final Nucleus n = activeDataset().getCollection().getConsensus();

			final IPoint clicked = new FloatPoint(x, y);
			lmOverlay.clearShapes();
			for (final OrientationMark lm : n.getOrientationMarks()) {
				final IPoint lmPoint = n.getBorderPoint(lm);
				final Landmark l = n.getLandmark(lm);

				if (clicked.getLengthTo(lmPoint) < distanceLimit && l != null) {
					changeLandmarkOverlay(l.toString(), lmPoint.getX(), lmPoint.getY(), textSize);
				}
			}
		} catch (final Exception e) {
			LOGGER.fine("Unable to draw landmark highlights: " + e.getMessage());
		}
	}

	private void changeLandmarkOverlay(String text, double x, double y, int size) {

		lmOverlay.clearShapes();
		lmOverlay.addShape(new ShapeOverlayObject(ShapeOverlayObject.createDiamond(1, x, y),
				ChartComponents.MARKER_STROKE, Color.DARK_GRAY, Color.DARK_GRAY));

		final Graphics2D g = (Graphics2D) chartPanel.getGraphics();
		final Font font = new Font(Font.SANS_SERIF, Font.PLAIN, size);
		final FontRenderContext frc = g.getFontRenderContext();
		final TextLayout layout = new TextLayout(text, font, frc);

		final AffineTransform txt = new AffineTransform();

		txt.concatenate(
				AffineTransform.getTranslateInstance(x + layout.getBounds().getWidth() / 1.5, y));
		txt.concatenate(AffineTransform.getScaleInstance(1, -1));
		txt.concatenate(AffineTransform.getTranslateInstance(-layout.getBounds().getCenterX(),
				-layout.getBounds().getCenterY()));

		lmOverlay.addShape(
				new ShapeOverlayObject(layout.getOutline(txt), new BasicStroke(0), Color.DARK_GRAY,
						Color.DARK_GRAY));

	}

}
