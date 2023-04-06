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
package com.bmskinner.nma.gui.tabs.signals;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nma.analysis.nucleus.CellCollectionFilterBuilder;
import com.bmskinner.nma.analysis.nucleus.CellCollectionFilterer;
import com.bmskinner.nma.analysis.nucleus.CellCollectionFilterer.CollectionFilteringException;
import com.bmskinner.nma.analysis.nucleus.FilteringOptions;
import com.bmskinner.nma.analysis.nucleus.FilteringOptions.FilterMatchType;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.datasets.VirtualDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.gui.components.panels.SignalGroupSelectionPanel;
import com.bmskinner.nma.gui.dialogs.SettingsDialog;
import com.bmskinner.nma.gui.events.NuclearSignalUpdatedListener;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.gui.tabs.ChartDetailPanel;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.visualisation.charts.AbstractChartFactory;
import com.bmskinner.nma.visualisation.charts.ViolinChartFactory;
import com.bmskinner.nma.visualisation.charts.panels.ExportableChartPanel;
import com.bmskinner.nma.visualisation.options.ChartOptions;
import com.bmskinner.nma.visualisation.options.ChartOptionsBuilder;

/**
 * Plot the number of signals per nucleus/per cell
 * 
 * @author ben
 * @since 1.14.0
 *
 */
@SuppressWarnings("serial")
public class SignalCountsPanel extends ChartDetailPanel implements NuclearSignalUpdatedListener {

	private static final Logger LOGGER = Logger.getLogger(SignalCountsPanel.class.getName());

	private static final String FILTER_LBL = "Filter nuclei";

	private static final String PANEL_TITLE_LBL = "Signal counts";
	private static final String PANEL_DESC_LBL = "Number of signals per cell";

	private JButton filterBtn = new JButton(FILTER_LBL);

	private ExportableChartPanel chartPanel;

	public SignalCountsPanel() {
		super(PANEL_TITLE_LBL, PANEL_DESC_LBL);
		createUI();
		uiController.addNuclearSignalUpdatedListener(this);
	}

	private void createUI() {
		this.setLayout(new BorderLayout());
		JPanel header = createHeader();
		add(header, BorderLayout.NORTH);

		chartPanel = new ExportableChartPanel(AbstractChartFactory.createEmptyChart());
		add(chartPanel, BorderLayout.CENTER);
	}

	/**
	 * Create the header panel
	 * 
	 * @return
	 */
	private JPanel createHeader() {
		JPanel panel = new JPanel();
		filterBtn.addActionListener(e -> {
			SignalCountFilteringSetupDialog dialog = new SignalCountFilteringSetupDialog(
					activeDataset());
			if (dialog.isReadyToRun())
				dialog.filter();
		});
		panel.add(filterBtn);
		filterBtn.setEnabled(false);
		return panel;
	}

	@Override
	protected synchronized void updateSingle() {
		updateMultiple();
		if (activeDataset() != null
				&& activeDataset().getCollection().getSignalManager().hasSignals())
			filterBtn.setEnabled(true);
	}

	@Override
	protected synchronized void updateMultiple() {
		filterBtn.setEnabled(false);
		ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets())
				.addStatistic(Measurement.NUCLEUS_SIGNAL_COUNT)
				.setScale(GlobalOptions.getInstance().getScale())
				.setSwatch(GlobalOptions.getInstance().getSwatch()).setTarget(chartPanel).build();

		setChart(options);
	}

	@Override
	protected synchronized void updateNull() {
		updateMultiple();
		filterBtn.setEnabled(false);
	}

	@Override
	public synchronized void setLoading() {
		super.setLoading();
		chartPanel.setChart(AbstractChartFactory.createLoadingChart());

	}

	@Override
	protected JFreeChart createPanelChartType(@NonNull ChartOptions options) {
		return new ViolinChartFactory(options)
				.createStatisticPlot(CellularComponent.NUCLEAR_SIGNAL);
	}

	private class SignalCountFilteringSetupDialog extends SettingsDialog {

		private static final String DIALOG_TITLE = "Signal filtering options";

		private final transient IAnalysisDataset dataset;

		private int minSignals;
		private int maxSignals;
		private SignalGroupSelectionPanel groupPanel;

		protected JPanel headingPanel;
		protected JPanel optionsPanel;
		protected JPanel footerPanel;

		public SignalCountFilteringSetupDialog(final @NonNull IAnalysisDataset dataset) {
			this(dataset, DIALOG_TITLE);
		}

		/**
		 * Constructor that does not make panel visible
		 * 
		 * @param mw
		 * @param title
		 */
		protected SignalCountFilteringSetupDialog(final @NonNull IAnalysisDataset dataset,
				final String title) {
			super(true);
			this.dataset = dataset;
			setTitle(title);
			createUI();
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

			FilteringOptions options = new CellCollectionFilterBuilder()
					.setMatchType(FilterMatchType.ALL_MATCH)
					.add(Measurement.NUCLEUS_SIGNAL_COUNT, CellularComponent.NUCLEUS,
							groupPanel.getSelectedID(),
							minSignals, maxSignals)
					.build();

			try {
				ICellCollection filtered = CellCollectionFilterer.filter(dataset.getCollection(),
						options);
				ICellCollection virt = new VirtualDataset(dataset, filtered.getName());
				filtered.getCells().forEach(virt::addCell);
				virt.setName(
						"Filtered_signal_count_" + groupPanel.getSelectedGroup().getGroupName());

				dataset.getCollection().getProfileManager().copySegmentsAndLandmarksTo(virt);
				dataset.addChildCollection(virt);

				// Alert new dataset has been added
				UIController.getInstance().fireDatasetAdded(dataset.getChildDataset(virt.getId()));

			} catch (CollectionFilteringException | ProfileException | MissingProfileException
					| MissingLandmarkException e1) {
				LOGGER.log(Loggable.STACK,
						"Unable to filter collection for %s".formatted(dataset.getName()),
						e1);
			}
		}

		protected void createUI() {

			setLayout(new BorderLayout());
			setBorder(new EmptyBorder(5, 5, 5, 5));

			headingPanel = createHeader();
			getContentPane().add(headingPanel, BorderLayout.NORTH);

			footerPanel = createFooter();
			getContentPane().add(footerPanel, BorderLayout.SOUTH);

			optionsPanel = new JPanel();
			GridBagLayout layout = new GridBagLayout();
			optionsPanel.setLayout(layout);

			List<JLabel> labels = new ArrayList<>();
			List<Component> fields = new ArrayList<>();

			groupPanel = new SignalGroupSelectionPanel(dataset);
			labels.add(new JLabel("Signal group to filter"));
			fields.add(groupPanel);

			int max = dataset.getCollection().getNuclei().stream()
					.flatMap(n -> n.getSignalCollection().getSignals().stream())
					.mapToInt(List::size).max()
					.orElse(0);

			maxSignals = max;

			SpinnerNumberModel minSignalModel = new SpinnerNumberModel(0, 0, max, 1);
			JSpinner minSpinner = new JSpinner(minSignalModel);
			minSpinner.addChangeListener(e -> minSignals = (int) minSignalModel.getValue());

			labels.add(new JLabel("Min signals per nucleus"));
			fields.add(minSpinner);

			SpinnerNumberModel maxSignalModel = new SpinnerNumberModel(max, 0, max, 1);
			JSpinner maxSpinner = new JSpinner(maxSignalModel);
			maxSpinner.addChangeListener(e -> maxSignals = (int) maxSignalModel.getValue());

			labels.add(new JLabel("Max signals per nucleus"));
			fields.add(maxSpinner);

			this.addLabelTextRows(labels, fields, layout, optionsPanel);
			getContentPane().add(optionsPanel, BorderLayout.CENTER);
		}
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
