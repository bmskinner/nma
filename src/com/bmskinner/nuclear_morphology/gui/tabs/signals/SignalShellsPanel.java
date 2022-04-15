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
package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.nucleus.CellCollectionFilterer.CollectionFilteringException;
import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellResultCellFilterer;
import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellResultCellFilterer.ShellResultFilterOperation;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.datasets.VirtualDataset;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult.Aggregation;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult.Normalisation;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.components.panels.SignalGroupSelectionPanel;
import com.bmskinner.nuclear_morphology.gui.components.panels.WrappedLabel;
import com.bmskinner.nuclear_morphology.gui.dialogs.SettingsDialog;
import com.bmskinner.nuclear_morphology.gui.dialogs.collections.ShellOverviewDialog;
import com.bmskinner.nuclear_morphology.gui.events.UserActionEvent;
import com.bmskinner.nuclear_morphology.gui.events.revamp.NuclearSignalUpdatedListener;
import com.bmskinner.nuclear_morphology.gui.events.revamp.UserActionController;
import com.bmskinner.nuclear_morphology.gui.tabs.ChartDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.TableDetailPanel;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.reports.DemoReportGenerator;
import com.bmskinner.nuclear_morphology.visualisation.charts.ShellChartFactory;
import com.bmskinner.nuclear_morphology.visualisation.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.visualisation.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.visualisation.datasets.tables.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.visualisation.datasets.tables.NuclearSignalTableCreator;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptions;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.visualisation.options.TableOptions;
import com.bmskinner.nuclear_morphology.visualisation.options.TableOptionsBuilder;

/**
 * Holds information on shell analysis results, and allows new shell analyses to
 * be run
 * 
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class SignalShellsPanel extends DetailPanel implements ActionListener {

	private static final Logger LOGGER = Logger.getLogger(SignalShellsPanel.class.getName());

	private static final String PANEL_TITLE_LBL = "Shells";
	private static final String WITHIN_SIGNALS_LBL = "Within signals";
	private static final String WITHIN_NUCLEI_LBL = "Within nuclei";
	private static final String RUN_ANALYSIS_LBL = "Run new";
	private static final String DAPI_NORM_LBL = "DAPI normalise";
	private static final String SHOW_RANDOM_LBL = "Show random";
	private static final String SHOW_SHELLS_LBL = "Show nuclei";
	private static final String FILTER_LBL = "Filter nuclei";

	private static final String RUN_ANALYSIS_TOOLTIP = "Run a shell analysis on all signal groups, replacing any existing analysis";
	private static final String WITHIN_SIGNALS_TOOLTIP = "Analyse only pixels that are within defined signals";
	private static final String WITHIN_NUCLEI_TOOLTIP = "Analyse any pixels that are within the nucleus";
	private static final String DAPI_NORM_TOOLTIP = "Apply a correction for nuclear flattening based on the DNA counterstain";
	private static final String SHOW_RANDOM_TOOLTIP = "Show a random distribution of signals in the consensus nucleus";
	private static final String SHOW_NUCLEI_TOOLTIP = "Show nuclei in the dataset with shells annotated";

	private JRadioButton withinSignalsBtn = new JRadioButton(WITHIN_SIGNALS_LBL);
	private JRadioButton withinNucleiBtn = new JRadioButton(WITHIN_NUCLEI_LBL);
	private ButtonGroup coverageGroup = new ButtonGroup();

	private JButton newAnalysis = new JButton(RUN_ANALYSIS_LBL);
	private JButton showNuclei = new JButton(SHOW_SHELLS_LBL);

	private JButton filterBtn = new JButton(FILTER_LBL);
	private JButton reportBtn = new JButton("Report");

	private JCheckBox dapiNormalise = new JCheckBox(DAPI_NORM_LBL, true);
	private JCheckBox showRandomCheckbox = new JCheckBox(SHOW_RANDOM_LBL, false);

	private ShellChartPanel shellChartPanel;
	private ShellOverallTablePanel shellOverallTablePanel;
	private ShellPairwiseTablePanel shellPairwiseTablePanel;

	public SignalShellsPanel() {
		super(PANEL_TITLE_LBL);
		this.setLayout(new BorderLayout());

		JPanel header = createHeader();
		JPanel mainPanel = createMainPanel();

		this.add(mainPanel, BorderLayout.CENTER);
		this.add(header, BorderLayout.NORTH);

		this.updateSize();
	}

	@Override
	public void setEnabled(boolean b) {
		newAnalysis.setEnabled(b);
		withinNucleiBtn.setEnabled(b);
		withinSignalsBtn.setEnabled(b);
		dapiNormalise.setEnabled(b);
		showRandomCheckbox.setEnabled(b);
		showNuclei.setEnabled(b);
		filterBtn.setEnabled(b);
	}

	/**
	 * Create the main display panel, containing all elements except the header
	 * 
	 * @return
	 */
	private JPanel createMainPanel() {

		JPanel panel = new JPanel(new GridBagLayout());

		shellChartPanel = new ShellChartPanel();
		JPanel westPanel = createWestPanel();

		// Set layout for west panel
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.BOTH;
		constraints.gridx = 0; // Start at left
		constraints.gridy = 0; // Start at top
		constraints.gridheight = GridBagConstraints.REMAINDER;
		constraints.gridwidth = 3; // Take up 1 cell in width
		constraints.weightx = 0.5;
		constraints.weighty = 1;
		constraints.anchor = GridBagConstraints.CENTER;

		panel.add(westPanel, constraints);

		// Set layout for bar chart panel
		constraints.gridx = 3; // Start after centre
		constraints.gridy = 0; // Start at top
		constraints.gridheight = GridBagConstraints.REMAINDER;
		constraints.gridwidth = 4;
		constraints.weightx = 0.5;

		panel.add(shellChartPanel, constraints);

		return panel;
	}

	/**
	 * Create the panel containing the table and consensus chart
	 * 
	 * @return
	 */
	private JPanel createWestPanel() {
		JPanel panel = new JPanel(new BorderLayout());

		shellOverallTablePanel = new ShellOverallTablePanel();
		shellPairwiseTablePanel = new ShellPairwiseTablePanel();

		panel.add(shellOverallTablePanel, BorderLayout.NORTH);
		panel.add(shellPairwiseTablePanel, BorderLayout.CENTER);

		return panel;

	}

	/**
	 * Create the header panel
	 * 
	 * @return
	 */
	private JPanel createHeader() {
		JPanel panel = new JPanel();

		newAnalysis.addActionListener(e -> UserActionController.getInstance()
				.userActionEventReceived(new UserActionEvent(this, UserActionEvent.RUN_SHELL_ANALYSIS, getDatasets())));

		newAnalysis.setToolTipText(RUN_ANALYSIS_TOOLTIP);

		panel.add(newAnalysis);

		// Add the coverage options

		coverageGroup.add(withinSignalsBtn);
		coverageGroup.add(withinNucleiBtn);
		withinSignalsBtn.addActionListener(this);
		withinNucleiBtn.addActionListener(this);
		withinSignalsBtn.setToolTipText(WITHIN_SIGNALS_TOOLTIP);
		withinNucleiBtn.setToolTipText(WITHIN_NUCLEI_TOOLTIP);

		withinSignalsBtn.setSelected(true);

		panel.add(withinSignalsBtn);
		panel.add(withinNucleiBtn);

		// Add the DAPI normalisation box

		dapiNormalise.addActionListener(this);
		dapiNormalise.setToolTipText(DAPI_NORM_TOOLTIP);
		panel.add(dapiNormalise);

		showRandomCheckbox.addActionListener(this);
		showRandomCheckbox.setToolTipText(SHOW_RANDOM_TOOLTIP);
		panel.add(showRandomCheckbox);

		showNuclei.addActionListener(e ->

		{
			new ShellOverviewDialog(activeDataset());
		});
		showNuclei.setToolTipText(SHOW_NUCLEI_TOOLTIP);
		panel.add(showNuclei);

		filterBtn.addActionListener(e -> {
			if (activeDataset() != null) {
				ShellFilteringSetupDialog dialog = new ShellFilteringSetupDialog(activeDataset());
				if (dialog.isReadyToRun())
					dialog.filter();
			}
		});
		panel.add(filterBtn);

		reportBtn.addActionListener(e -> {
			try {
				new DemoReportGenerator().generateShellReport(activeDataset());
			} catch (IOException | MissingOptionException e1) {
				LOGGER.log(Level.WARNING, "Unable to generate report");
			}
		});
//        panel.add(reportBtn);

		setEnabled(false);

		return panel;
	}

	@Override
	protected synchronized void updateSingle() {
		setEnabled(false);
		if (activeDataset() == null)
			return;
		if (activeDataset().getCollection().getSignalManager().hasSignals()) {
			newAnalysis.setEnabled(true);
			if (activeDataset().getCollection().getSignalManager().hasShellResult())
				setEnabled(true);
		}

	}

	@Override
	protected synchronized void updateMultiple() {
		showRandomCheckbox.setEnabled(false);
		showNuclei.setEnabled(false);
		filterBtn.setEnabled(false);
		reportBtn.setEnabled(false);
	}

	@Override
	protected synchronized void updateNull() {
		setEnabled(false);
	}

	@Override
	public synchronized void actionPerformed(ActionEvent arg0) {
		shellChartPanel.update();
		shellOverallTablePanel.update();
		shellPairwiseTablePanel.update();
	}

	private class ShellFilteringSetupDialog extends SettingsDialog {

		private static final String DIALOG_TITLE = "Shell filtering options";

		private final IAnalysisDataset dataset;

		private double proportion = 0.5d;
		private int shell = 0;
		private ShellResultFilterOperation op = ShellResultFilterOperation.SPECIFIC_SHELL_IS_LESS_THAN;
		private SignalGroupSelectionPanel groupPanel;

		protected JPanel headingPanel;
		protected JPanel optionsPanel;
		protected JPanel footerPanel;

		public ShellFilteringSetupDialog(@NonNull final IAnalysisDataset dataset) {
			this(dataset, DIALOG_TITLE);
		}

		/**
		 * Constructor that does not make panel visible
		 * 
		 * @param mw
		 * @param title
		 */
		protected ShellFilteringSetupDialog(@NonNull final IAnalysisDataset dataset, @NonNull final String title) {
			super(true);
			this.dataset = dataset;
			createUI();
			setTitle(title);
			pack();
			setVisible(true);
		}

		@Override
		protected JPanel createHeader() {
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			return panel;
		}

		public void filter() {
			try {
				LOGGER.info("Filtering " + dataset.getName());
				ICellCollection filtered = new ShellResultCellFilterer(groupPanel.getSelectedID())
						.setFilter(op, shell, proportion).filter(dataset.getCollection());
				if (!filtered.hasCells()) {
					LOGGER.info("No cells found");
					return;
				}

				LOGGER.info("Found " + filtered.size() + " cells");
				ICellCollection virt = new VirtualDataset(dataset, "Filtered_on_shell");
				filtered.getCells().forEach(virt::addCell);
				dataset.getCollection().getProfileManager().copySegmentsAndLandmarksTo(virt);
				dataset.addChildCollection(virt);

				// TODO: alert populations panel that there is a new dataset
			} catch (ProfileException | CollectionFilteringException | MissingProfileException e1) {
				LOGGER.log(Loggable.STACK, "Unable to filter collection for " + dataset.getName(), e1);
			}
		}

		protected void createUI() {

			setLayout(new BorderLayout());
			setBorder(new EmptyBorder(5, 5, 5, 5));

			headingPanel = createHeader();
			getContentPane().add(headingPanel, BorderLayout.NORTH);

			footerPanel = createFooter();
			getContentPane().add(footerPanel, BorderLayout.SOUTH);

			ICellCollection collection = dataset.getCollection();

			optionsPanel = new JPanel();
			GridBagLayout layout = new GridBagLayout();
			optionsPanel.setLayout(layout);

			List<JLabel> labels = new ArrayList<>();
			List<Component> fields = new ArrayList<>();

			groupPanel = new SignalGroupSelectionPanel(dataset);

			labels.add(new JLabel("Signal group"));
			fields.add(groupPanel);

			JComboBox<ShellResultFilterOperation> typeBox = new JComboBox<>(ShellResultFilterOperation.values());
			typeBox.setSelectedItem(ShellResultFilterOperation.SPECIFIC_SHELL_IS_LESS_THAN);
			typeBox.addActionListener(e -> op = (ShellResultFilterOperation) typeBox.getSelectedItem());

			labels.add(new JLabel("Filtering operation"));
			fields.add(typeBox);

			SpinnerNumberModel sModel = new SpinnerNumberModel(0, 0, collection.getSignalManager().getShellCount() - 1,
					1);
			JSpinner spinner = new JSpinner(sModel);
			spinner.addChangeListener(e -> shell = (int) sModel.getValue());

			labels.add(new JLabel("Shell"));
			fields.add(spinner);

			SpinnerNumberModel pModel = new SpinnerNumberModel(0.5d, 0d, 1d, 0.1d);
			JSpinner pSpinner = new JSpinner(pModel);
			pSpinner.addChangeListener(e -> proportion = (double) pModel.getValue());

			labels.add(new JLabel("Proportion"));
			fields.add(pSpinner);

			this.addLabelTextRows(labels, fields, layout, optionsPanel);
			getContentPane().add(optionsPanel, BorderLayout.CENTER);
		}
	}

	public class ShellChartPanel extends ChartDetailPanel implements NuclearSignalUpdatedListener {

		private ExportableChartPanel chartPanel;

		public ShellChartPanel() {
			super(PANEL_TITLE_LBL);
			this.setLayout(new BorderLayout());
			add(createShellBarPanel(), BorderLayout.CENTER);

			uiController.addNuclearSignalUpdatedListener(this);
		}

		/**
		 * Create the panel holding the shell bar chart
		 * 
		 * @return
		 */
		private JPanel createShellBarPanel() {
			JPanel panel = new JPanel(new BorderLayout());
			ChartOptions options = new ChartOptionsBuilder().build();
			JFreeChart chart = new ShellChartFactory(options).createEmptyShellChart();
			chartPanel = new ExportableChartPanel(chart);

			panel.add(chartPanel, BorderLayout.CENTER);

			return panel;
		}

		@Override
		protected synchronized void updateSingle() {
			updateChart();
			setEnabled(false);

		}

		@Override
		protected synchronized void updateMultiple() {
			updateChart();

		}

		@Override
		protected synchronized void updateNull() {
			setEnabled(false);
			updateChart();

		}

		private synchronized void updateChart() {
			Aggregation agg = withinNucleiBtn.isSelected() ? Aggregation.BY_NUCLEUS : Aggregation.BY_SIGNAL;
			Normalisation norm = dapiNormalise.isSelected() ? Normalisation.DAPI : Normalisation.NONE;
			boolean showRandom = showRandomCheckbox.isSelected();

			ChartOptions barChartOptions = new ChartOptionsBuilder().setDatasets(getDatasets()).setTarget(chartPanel)
					.setShowAnnotations(showRandom) // proxy fpr random
					.setAggregation(agg).setNormalisation(norm).build();

			setChart(barChartOptions);
		}

		@Override
		protected JFreeChart createPanelChartType(@NonNull ChartOptions options) {
			return new ShellChartFactory(options).createShellChart();
		}

		@Override
		public void nuclearSignalUpdated(List<IAnalysisDataset> datasets) {
			refreshCache(datasets);
		}

		@Override
		public void nuclearSignalUpdated(IAnalysisDataset dataset) {
			refreshCache(dataset);
		}
	}

	public class ShellOverallTablePanel extends TableDetailPanel {

		protected ExportableTable table;

		public ShellOverallTablePanel() {
			super(PANEL_TITLE_LBL);
			this.setLayout(new BorderLayout());
			add(createOverallTablePanel(), BorderLayout.CENTER);
		}

		/**
		 * Create the table panel
		 * 
		 * @return
		 */
		private JPanel createOverallTablePanel() {
			JPanel tablePanel = new JPanel(new BorderLayout());

			JTextArea textArea = new WrappedLabel(
					"Comparisons to random distribution by chi-square with Bonferroni correction");

			tablePanel.add(textArea, BorderLayout.NORTH);
			TableModel model = AnalysisDatasetTableCreator.createBlankTable();
			table = new ExportableTable(model);
			table.setEnabled(false);

			JScrollPane scrollPane = new JScrollPane();
			scrollPane.setViewportView(table);
			scrollPane.setColumnHeaderView(table.getTableHeader());
			Dimension size = new Dimension(200, 150);
			tablePanel.setMinimumSize(size);
			tablePanel.setPreferredSize(size);
			tablePanel.add(scrollPane, BorderLayout.CENTER);
			return tablePanel;
		}

		@Override
		protected TableModel createPanelTableType(@NonNull TableOptions options) {
			return new NuclearSignalTableCreator(options).createShellChiSquareTable();
		}

		@Override
		protected synchronized void updateSingle() {
			updateTable();
			setEnabled(false);

		}

		@Override
		protected synchronized void updateMultiple() {
			updateTable();

		}

		@Override
		protected synchronized void updateNull() {
			setEnabled(false);
			updateTable();

		}

		@Override
		public synchronized void setLoading() {
			super.setLoading();
			table.setModel(AbstractTableCreator.createLoadingTable());
		}

		private synchronized void updateTable() {
			Aggregation agg = withinNucleiBtn.isSelected() ? Aggregation.BY_NUCLEUS : Aggregation.BY_SIGNAL;
			Normalisation norm = dapiNormalise.isSelected() ? Normalisation.DAPI : Normalisation.NONE;
			TableOptions tableOptions = new TableOptionsBuilder().setDatasets(getDatasets()).setAggregation(agg)
					.setNormalisation(norm).setTarget(table).build();

			setTable(tableOptions);
		}

	}

	public class ShellPairwiseTablePanel extends TableDetailPanel {

		protected ExportableTable table;

		public ShellPairwiseTablePanel() {
			super(PANEL_TITLE_LBL);
			this.setLayout(new BorderLayout());
			add(createPairwiseTablePanel(), BorderLayout.CENTER);
		}

		/**
		 * Create the table panel
		 * 
		 * @return
		 */
		private JPanel createPairwiseTablePanel() {
			JPanel tablePanel = new JPanel(new BorderLayout());
			Dimension size = new Dimension(200, 150);

			JTextArea textArea = new WrappedLabel(
					"Pairwise comparisons of shell results by chi-square with Bonferroni correction");

			tablePanel.add(textArea, BorderLayout.NORTH);
			TableModel model = AnalysisDatasetTableCreator.createBlankTable();
			table = new ExportableTable(model);
			table.setEnabled(false);

			JScrollPane scrollPane = new JScrollPane();
			scrollPane.setViewportView(table);
			scrollPane.setColumnHeaderView(table.getTableHeader());

			tablePanel.setMinimumSize(size);
			tablePanel.setPreferredSize(size);
			tablePanel.add(scrollPane, BorderLayout.CENTER);
			return tablePanel;
		}

		@Override
		protected TableModel createPanelTableType(@NonNull TableOptions options) {
			return new NuclearSignalTableCreator(options).createPairwiseShellChiSquareTable();
		}

		@Override
		protected synchronized void updateSingle() {
			updateTable();
			setEnabled(false);

		}

		@Override
		protected synchronized void updateMultiple() {
			updateTable();

		}

		@Override
		protected synchronized void updateNull() {
			setEnabled(false);
			updateTable();
		}

		@Override
		public synchronized void setLoading() {
			super.setLoading();
			table.setModel(AbstractTableCreator.createLoadingTable());
		}

		private synchronized void updateTable() {
			Aggregation agg = withinNucleiBtn.isSelected() ? Aggregation.BY_NUCLEUS : Aggregation.BY_SIGNAL;
			Normalisation norm = dapiNormalise.isSelected() ? Normalisation.DAPI : Normalisation.NONE;

			TableOptions pairwiseOptions = new TableOptionsBuilder().setDatasets(getDatasets()).setAggregation(agg)
					.setNormalisation(norm).setTarget(table).build();

			setTable(pairwiseOptions);
		}

	}
}
