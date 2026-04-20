package com.bmskinner.nma.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.annotations.XYBoxAnnotation;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.ui.Layer;

import com.bmskinner.nma.analysis.classification.NonunimodalRegionClusteringMethod.BarcodeElement;
import com.bmskinner.nma.analysis.classification.NonunimodalRegionClusteringMethod.ProfileBarcodingRegion;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.cells.UnavailableBorderPointException;
import com.bmskinner.nma.components.datasets.HammingClusterGroup;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.gui.components.ExportableTable;
import com.bmskinner.nma.gui.components.panels.ExportableChartPanel;
import com.bmskinner.nma.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import com.bmskinner.nma.gui.components.panels.ProfileTypeOptionsPanel;
import com.bmskinner.nma.visualisation.ChartComponents;
import com.bmskinner.nma.visualisation.charts.AbstractChartFactory;
import com.bmskinner.nma.visualisation.charts.ConsensusNucleusChartFactory;
import com.bmskinner.nma.visualisation.charts.ExportableLegendChart;
import com.bmskinner.nma.visualisation.charts.ProfileChartFactory;
import com.bmskinner.nma.visualisation.options.ChartOptions;
import com.bmskinner.nma.visualisation.options.ChartOptionsBuilder;

/**
 * Visualise Hamming clusters. We need to show the regions of profiles that were
 * clustered on, and the range of morphotypes within those regions.
 * 
 */
public class HammingClusterPlotDialog extends MessagingDialog {

	private static final Logger LOGGER = Logger.getLogger(HammingClusterPlotDialog.class.getName());

	private final IAnalysisDataset dataset;
	private final HammingClusterGroup group;

	private final JPanel mainPanel = new JPanel();

	final ProfileTypeOptionsPanel profileTypeSelectionPanel = new ProfileTypeOptionsPanel();

	private final ExportableChartPanel profileFullChartPanel = new ExportableChartPanel(
			AbstractChartFactory.createEmptyChart());

	private final ExportableChartPanel profileRegionChartPanel = new ExportableChartPanel(
			AbstractChartFactory.createEmptyChart());

	private final ExportableChartPanel consensusChartPanel = new ExportableChartPanel(
			AbstractChartFactory.createEmptyChart());


	public HammingClusterPlotDialog(final @NonNull IAnalysisDataset dataset,
			final @NonNull IClusterGroup group) {
		this.dataset = dataset;

		if (!(group instanceof final HammingClusterGroup))
			throw new IllegalArgumentException("Cannot display cluster group as a hamming group");
		this.group = (HammingClusterGroup) group;

		updateTitle();
		final GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 1;
		c.fill = GridBagConstraints.BOTH; // fill both axes of container
		c.weightx = 1.0; // maximum weighting
		c.weighty = 1.0;
		c.gridx = 0;
		c.gridy = 0;
		mainPanel.setLayout(new GridBagLayout());
		mainPanel.add(createTopPanel(), c);
		c.gridy = 1;
		c.weighty = 0.5;
		mainPanel.add(createBottomPanel(), c);

		setLayout(new BorderLayout());

		add(createHeader(), BorderLayout.NORTH);
		add(mainPanel, BorderLayout.CENTER);
		setPreferredSize(new Dimension(1080, 804));

		pack();
		setLocationRelativeTo(null);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		setVisible(true);
	}

	/**
	 * Create the upper panel that holds the list of regions and per-region
	 * information
	 * 
	 * @return
	 */
	private JPanel createTopPanel() {
		final JPanel topPanel = new JPanel(new GridBagLayout());
		final GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 1;
		c.fill = GridBagConstraints.BOTH; // fill both axes of container
		c.weightx = 0.5; // maximum weighting
		c.weighty = 1.0;
		c.gridx = 0;
		c.gridy = 0;

		topPanel.add(createTablePanel(), c);

		c.gridx = 1;
		c.weightx = 1;
		topPanel.add(createProfileRegionPanel(), c);

		return topPanel;
	}

	/**
	 * Create the bottom panel that holds the overview profile and consensus
	 * 
	 * @return
	 */
	private JPanel createBottomPanel() {
		final GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 1;
		c.fill = GridBagConstraints.BOTH; // fill both axes of container
		c.weightx = 1.0; // maximum weighting
		c.weighty = 1.0;
		c.gridx = 0;
		c.gridy = 0;
		final JPanel bottomPanel = new JPanel(new GridBagLayout());
		bottomPanel.add(createFullProfileChartPanel(), c);

		c.gridx = 1;
		c.weightx = 0.5;
		bottomPanel.add(createConsensusPanel(), c);
		return bottomPanel;

	}

	/**
	 * Create the header panel with options
	 * 
	 * @return
	 */
	private JPanel createHeader() {

		final JPanel panel = new JPanel(new FlowLayout());
		profileTypeSelectionPanel.addActionListener(e -> updateGlobalCharts());
		profileTypeSelectionPanel.setEnabled(true);
		panel.add(profileTypeSelectionPanel);
		return panel;
	}

	/**
	 * Create the table listing the regions of interest for barcoding
	 * 
	 * @return
	 */
	private JPanel createTablePanel() {
		final DefaultTableModel model = new DefaultTableModel(
				new Object[] { "", "Profile type", "Start", "End", "Length", "N clusters" }, 0);

		final List<ProfileBarcodingRegion> pbrs = group.getBarcodingRegions().stream()
				.sorted((p, q) -> Integer.compare(p.startIndex(), q.startIndex())).collect(Collectors.toList());
		for (final ProfileBarcodingRegion pbr : pbrs) {

			// Find the number of clusters in this region
			final Map<Integer, Long> clusterCounts = group.getNucleusBarcodes().values().stream()
					.flatMap(e -> e.elements().stream().filter(b -> b.pbr().equals(pbr)))
					.map(BarcodeElement::cluster)
					.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

			model.addRow(new Object[] { pbr, pbr.type(), pbr.startIndex(), pbr.endIndex(), pbr.length(),
					clusterCounts.size() });
		}

		final JTable table = new ExportableTable(model);
		table.setEnabled(true);
		table.setRowSelectionAllowed(true);
		table.setColumnSelectionAllowed(false);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


		table.getSelectionModel().addListSelectionListener(e -> {
			if (e.getValueIsAdjusting())
				return;
			final ListSelectionModel lsm = (ListSelectionModel) e.getSource();
			final ProfileBarcodingRegion pbr = (ProfileBarcodingRegion) table.getValueAt(lsm.getMaxSelectionIndex(), 0);
			LOGGER.fine("Updating region chart at index %s to %s".formatted(lsm.getMaxSelectionIndex(), pbr));
			updateRegionCharts(pbr);
		});

		final JPanel tablePanel = new JPanel(new BorderLayout());
		tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
		tablePanel.add(table.getTableHeader(), BorderLayout.NORTH);

		final JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(tablePanel, BorderLayout.CENTER);
		return panel;
	}

	private void updateRegionCharts(ProfileBarcodingRegion pbr) {
		if (pbr == null)
			return;

		// Update the profile chart
		final ChartOptions profileOptions = new ChartOptionsBuilder().setDatasets(dataset)
				.setNormalised(true)
				.setAlignment(ProfileAlignment.LEFT).setLandmark(OrientationMark.REFERENCE)
				.setShowAnnotations(false)
				.setShowProfiles(false)
				.setSwatch(GlobalOptions.getInstance().getSwatch())
				.setProfileType(pbr.type()).build();

		// Start with a basic profile chart
		final ExportableLegendChart elc = new ProfileChartFactory(profileOptions).createProfileChart();

		try {

		for (final Nucleus n : dataset.getCollection().getNuclei()) {

			// Get the barcode for this nucleus and find teh cluster it belongs to
			final BarcodeElement el = group.getNucleusBarcodes().get(n.getId()).elements().stream()
					.filter(e -> e.pbr().equals(pbr)).findFirst().get();

			final Color clusterColour = ColourSelecter.getColor(el.cluster());

			// Draw an annotation of the nucleus profile
			final IProfile np = n.getProfile(pbr.type())
					.interpolate(dataset.getCollection().getMedianArrayLength());
			
			for (int i = pbr.startIndex() - 1; i < pbr.endIndex() + 2; i++) {
				final double value = np.get(i);
				final double nextValue = np.get(i + 1);
				final double domain = ((double) (i)) / dataset.getCollection().getMedianArrayLength() * 100;
				final double nextDomain = ((double) (i + 1)) / dataset.getCollection().getMedianArrayLength() * 100;
				elc.getXYPlot().getRenderer().addAnnotation(new XYLineAnnotation(domain, value,
						nextDomain, nextValue, ChartComponents.PROFILE_STROKE, clusterColour),
						Layer.BACKGROUND);
			}
			


		}
	} catch (final Exception e) {
		LOGGER.log(Level.FINE, "Unable to draw nucleus profile: %s".formatted(e.getMessage()), e);
	}

		profileRegionChartPanel.setChart(elc);

		// Constrain to region of interest
		final double medianProfileLength = dataset.getCollection().getMedianArrayLength();
		final double normIndexStart = (pbr.startIndex() - 1) / medianProfileLength * 100;
		final double normIndexEnd = (pbr.endIndex() + 1) / medianProfileLength * 100;

		elc.getXYPlot().getDomainAxis().setRange(normIndexStart, normIndexEnd);

	}

	private void updateGlobalCharts() {

		final ProfileType profileType = profileTypeSelectionPanel.getSelected();

		// Update the profile chart
		final ChartOptions profileOptions = new ChartOptionsBuilder().setDatasets(dataset)
				.setNormalised(false)
				.setAlignment(ProfileAlignment.LEFT).setLandmark(OrientationMark.REFERENCE)
				.setShowAnnotations(false)
				.setShowProfiles(false)
				.setSwatch(GlobalOptions.getInstance().getSwatch())
				.setProfileType(profileType).build();

		// Start with a basic profile chart
		final ExportableLegendChart elc = new ProfileChartFactory(profileOptions).createProfileChart();

		// Add rectangle annotations for the ranges covering regions of interest
		for (final ProfileBarcodingRegion pbr : group.getBarcodingRegions()) {
			LOGGER.finer("Adding %s".formatted(pbr));

			final Color lineColour = pbr.type().equals(profileType) ? Color.GRAY : Color.LIGHT_GRAY;
			elc.getXYPlot().getRenderer().addAnnotation(new XYBoxAnnotation(pbr.startIndex(), 0,
					pbr.endIndex(), 360, ChartComponents.PROFILE_STROKE, lineColour, lineColour),
					Layer.BACKGROUND);
		}

		profileFullChartPanel.setChart(elc);

		// Update the consensus chart
		final ChartOptions consensusOptions = new ChartOptionsBuilder().setDatasets(dataset)
				.setScale(GlobalOptions.getInstance().getDisplayScale())
				.setSwatch(GlobalOptions.getInstance().getSwatch())
				.setShowMesh(false)
				.setShowMeshVertices(false)
				.setShowMeshEdges(false)
				.setShowMeshFaces(false)
				.setStraightenMesh(false)
				.setShowAnnotations(false)
				.setFillConsensus(GlobalOptions.getInstance().isFillConsensus())
				.setShowXAxis(false)
				.setShowYAxis(false).build();

		final ExportableLegendChart consensusChart = new ConsensusNucleusChartFactory(consensusOptions)
				.makeConsensusChart();

		try {
			final Nucleus n = dataset.getCollection().getConsensus();

			// Add rectangle annotations for the ranges covering regions of interest
			for (final ProfileBarcodingRegion pbr : group.getBarcodingRegions()) {
				LOGGER.finer("Adding %s".formatted(pbr));
				final Color lineColour = pbr.type().equals(profileType) ? Color.GRAY : Color.LIGHT_GRAY;

//				if (pbr.type().equals(profileType)) {

					final IPoint start = n.getBorderPoint(pbr.startIndex());
					final IPoint end = n.getBorderPoint(pbr.endIndex());

					consensusChart.getXYPlot().getRenderer()
							.addAnnotation(new XYLineAnnotation(start.getX(), start.getY(),
									end.getX(), end.getY(), ChartComponents.LANDMARK_STROKE, lineColour),
							Layer.BACKGROUND);
//				}
			}
			consensusChartPanel.setChart(consensusChart);
		} catch (MissingLandmarkException | ComponentCreationException | UnavailableBorderPointException e) {
			LOGGER.log(Level.SEVERE, "Unable to create consensus: %s".formatted(e.getMessage()), e);
		}

	}

	private JPanel createProfileRegionPanel() {
		profileRegionChartPanel.setPannable(false);

		updateRegionCharts(null);

		final JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(profileRegionChartPanel, BorderLayout.CENTER);
		return panel;

	}

	private JPanel createConsensusPanel() {

		consensusChartPanel.setFixedAspectRatio(true);

		final JPanel panel = new JPanel(new BorderLayout());
		panel.add(consensusChartPanel, BorderLayout.CENTER);

		panel.setPreferredSize(new Dimension(300, 200));
		updateGlobalCharts();
		return panel;
	}

	private JPanel createFullProfileChartPanel() {

		profileFullChartPanel.setPannable(false);

		updateGlobalCharts();
		
		final JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(profileFullChartPanel, BorderLayout.CENTER);
		return panel;
	}


	private void updateTitle() {
		setTitle("Hamming clusters for %s: %s".formatted(dataset.getName(), group.getName()));
	}

}
