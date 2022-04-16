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
package com.bmskinner.nuclear_morphology.gui.tabs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;
import org.jfree.data.Range;

import com.bmskinner.nuclear_morphology.analysis.nucleus.CellCollectionFilterBuilder;
import com.bmskinner.nuclear_morphology.analysis.nucleus.CellCollectionFilterer;
import com.bmskinner.nuclear_morphology.analysis.nucleus.CellCollectionFilterer.CollectionFilteringException;
import com.bmskinner.nuclear_morphology.analysis.nucleus.FilteringOptions;
import com.bmskinner.nuclear_morphology.analysis.nucleus.FilteringOptions.FilterMatchType;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.datasets.VirtualDataset;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.components.panels.WrappedLabel;
import com.bmskinner.nuclear_morphology.gui.events.revamp.ScaleUpdatedListener;
import com.bmskinner.nuclear_morphology.gui.events.revamp.SwatchUpdatedListener;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.visualisation.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.visualisation.charts.ScatterChartFactory;
import com.bmskinner.nuclear_morphology.visualisation.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptions;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.visualisation.options.TableOptions;
import com.bmskinner.nuclear_morphology.visualisation.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.visualisation.tables.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.visualisation.tables.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.visualisation.tables.ScatterTableDatasetCreator;

/**
 * An abstract class implementing the plottable statistic header on a detail
 * panel for drawing scatter charts
 * 
 * @author ben
 *
 */
@SuppressWarnings("serial")
public abstract class AbstractScatterPanel extends DetailPanel {

	private static final Logger LOGGER = Logger.getLogger(AbstractScatterPanel.class.getName());

	private static final String PANEL_TITLE_LBL = "Scatter";
	private static final String FILTER_BTN_LBL = "Filter visible";
	private static final String FILTER_BTN_TOOLTIP = "Create a sub-population based on the visible values";

	private static final String X_AXIS_LBL = "X axis";
	private static final String Y_AXIS_LBL = "Y axis";

	protected JPanel headerPanel; // hold buttons

	private AbstractScatterChartPanel chartPanel;
	private AbstractScatterCorrelationPanel rhoPanel;

	protected JButton gateButton;

	protected JComboBox<Measurement> statABox, statBBox;
	protected String component;

	protected AbstractScatterPanel(String component) {
		super();
		this.component = component;

		this.setLayout(new BorderLayout());

		headerPanel = createHeader();

		this.add(headerPanel, BorderLayout.NORTH);
		chartPanel = new AbstractScatterChartPanel(component);
		rhoPanel = new AbstractScatterCorrelationPanel(component);
		add(chartPanel, BorderLayout.CENTER);
		add(rhoPanel, BorderLayout.WEST);
	}

	@Override
	public String getPanelTitle() {
		return PANEL_TITLE_LBL;
	}

	private JPanel createHeader() {
		statABox = new JComboBox<>(Measurement.getStats(component));
		statBBox = new JComboBox<>(Measurement.getStats(component));

		statABox.setSelectedItem(
				component.equals(CellularComponent.NUCLEAR_SIGNAL) ? Measurement.FRACT_DISTANCE_FROM_COM
						: Measurement.VARIABILITY); // default if present

		statABox.addActionListener(e -> update(getDatasets()));
		statBBox.addActionListener(e -> update(getDatasets()));

		statABox.setEnabled(false);
		statBBox.setEnabled(false);

		gateButton = new JButton(FILTER_BTN_LBL);
		gateButton.setToolTipText(FILTER_BTN_TOOLTIP);
		gateButton.addActionListener(e -> chartPanel.gateOnVisible());
		gateButton.setEnabled(false);

		JPanel panel = new JPanel(new FlowLayout());

		panel.add(new JLabel(X_AXIS_LBL));
		panel.add(statABox);
		panel.add(new JLabel(Y_AXIS_LBL));
		panel.add(statBBox);

		panel.add(gateButton);
		return panel;
	}

	@Override
	public synchronized void refreshCache() {
		rhoPanel.refreshCache();
		chartPanel.refreshCache();
	}

	@Override
	public synchronized void refreshCache(IAnalysisDataset d) {
		rhoPanel.refreshCache(d);
		chartPanel.refreshCache(d);
	}

	@Override
	public synchronized void refreshCache(List<IAnalysisDataset> l) {
		rhoPanel.refreshCache(l);
		chartPanel.refreshCache(l);
	}

	private class AbstractScatterChartPanel extends ChartDetailPanel
			implements ScaleUpdatedListener, SwatchUpdatedListener {

		protected ExportableChartPanel chartPanel; // hold the charts

		public AbstractScatterChartPanel(String component) {
			super();

			this.setLayout(new BorderLayout());

			headerPanel = createHeader();

			this.add(headerPanel, BorderLayout.NORTH);

			JFreeChart chart = ScatterChartFactory.createEmptyChart();

			chartPanel = new ExportableChartPanel(chart);
			chartPanel.getChartRenderingInfo().setEntityCollection(null);

			add(chartPanel, BorderLayout.CENTER);
			uiController.addScaleUpdatedListener(this);

		}

		@Override
		public String getPanelTitle() {
			return PANEL_TITLE_LBL;
		}

		@Override
		protected synchronized JFreeChart createPanelChartType(@NonNull ChartOptions options) {
			return new ScatterChartFactory(options).createScatterChart(component);
		}

		private Range getRangeBounds() {
			return chartPanel.getChart().getXYPlot().getRangeAxis().getRange();
		}

		private Range getDomainBounds() {
			return chartPanel.getChart().getXYPlot().getDomainAxis().getRange();
		}

		private void gateOnVisible() {

			int result;
			try {
				String[] options = { "Do not filter", "Filter collection" };
				result = this.getInputSupplier().requestOptionAllVisible(options,
						"Filter selected datasets on visible values?", "Filter datasets?");
			} catch (RequestCancelledException e2) {
				return;
			}

			if (result == 0)
				return;

			LOGGER.finer("Filtering datasets on " + statABox.getSelectedItem().toString() + " and "
					+ statBBox.getSelectedItem().toString());

			MeasurementScale scale = GlobalOptions.getInstance().getScale();

			Range domain = getDomainBounds();
			Range range = getRangeBounds();
			Measurement statA = (Measurement) statABox.getSelectedItem();
			Measurement statB = (Measurement) statBBox.getSelectedItem();

			FilteringOptions options = new CellCollectionFilterBuilder().setMatchType(FilterMatchType.ALL_MATCH)
					.add(statA, component, scale, domain.getLowerBound(), domain.getUpperBound())
					.add(statB, component, scale, range.getLowerBound(), range.getUpperBound()).build();

			for (IAnalysisDataset d : getDatasets()) {
				try {

					// Get the filtered cells as a real collection
					ICellCollection filtered = CellCollectionFilterer.filter(d.getCollection(), options);

					// Put them into a virtual collection
					ICellCollection virt = new VirtualDataset(d, filtered.getName());
					filtered.getCells().forEach(c -> virt.addCell(c));
					virt.setName("Filtered_" + statA + "_" + statB);

					d.getCollection().getProfileManager().copySegmentsAndLandmarksTo(virt);
					d.getCollection().getSignalManager().copySignalGroupsTo(virt);
					d.addChildCollection(virt);
				} catch (CollectionFilteringException | ProfileException | MissingProfileException e1) {
					LOGGER.log(Loggable.STACK, "Unable to filter collection for " + d.getName(), e1);
				}
			}

			LOGGER.info(String.format("Filtered datasets by %s and %s", statA, statB));
		}

		@Override
		protected synchronized void updateSingle() {

			Measurement statA = (Measurement) statABox.getSelectedItem();
			Measurement statB = (Measurement) statBBox.getSelectedItem();

			ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets()).addStatistic(statA)
					.addStatistic(statB).setScale(GlobalOptions.getInstance().getScale())
					.setSwatch(GlobalOptions.getInstance().getSwatch()).setTarget(chartPanel).build();

			setChart(options);

			// Check if the panel component is present
			boolean isActive = activeDataset() != null;
			if (isActive && component.equals(CellularComponent.NUCLEAR_SIGNAL))
				isActive &= activeDataset().getCollection().getSignalManager().hasSignals();

			gateButton.setEnabled(isActive);
			statABox.setEnabled(isActive);
			statBBox.setEnabled(isActive);
		}

		@Override
		protected synchronized void updateMultiple() {
			updateSingle();
		}

		@Override
		protected synchronized void updateNull() {

			chartPanel.setChart(AbstractChartFactory.createEmptyChart());
			gateButton.setEnabled(false);
			statABox.setEnabled(false);
			statBBox.setEnabled(false);
		}

		@Override
		public synchronized void setLoading() {
			chartPanel.setChart(AbstractChartFactory.createLoadingChart());
		}

		@Override
		public void swatchUpdated() {
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
			refreshCache();
		}
	}

	private class AbstractScatterCorrelationPanel extends TableDetailPanel implements ScaleUpdatedListener {

		private static final String SPEARMAN_LBL = "Spearman's rank correlation coefficients";

		protected ExportableTable rhoTable;

		public AbstractScatterCorrelationPanel(String component) {
			super();

			this.setLayout(new BorderLayout());
			add(createPanel(), BorderLayout.CENTER);
		}

		private JPanel createPanel() {
			JTextArea textArea = new WrappedLabel(SPEARMAN_LBL);

			JPanel panel = new JPanel(new BorderLayout());

			TableModel model = AnalysisDatasetTableCreator.createBlankTable();
			rhoTable = new ExportableTable(model);
			rhoTable.setEnabled(false);

			JScrollPane scrollPane = new JScrollPane();
			scrollPane.setViewportView(rhoTable);
			scrollPane.setColumnHeaderView(rhoTable.getTableHeader());
			Dimension size = new Dimension(300, 200);
			scrollPane.setMinimumSize(size);
			scrollPane.setPreferredSize(size);

			panel.add(textArea, BorderLayout.NORTH);
			panel.add(scrollPane, BorderLayout.CENTER);

			return panel;
		}

		@Override
		public String getPanelTitle() {
			return PANEL_TITLE_LBL;
		}

		@Override
		protected synchronized void updateSingle() {

			Measurement statA = (Measurement) statABox.getSelectedItem();
			Measurement statB = (Measurement) statBBox.getSelectedItem();

			TableOptions tableOptions = new TableOptionsBuilder().setDatasets(getDatasets()).addStatistic(statA)
					.addStatistic(statB).setScale(GlobalOptions.getInstance().getScale()).setTarget(rhoTable).build();

			setTable(tableOptions);
		}

		@Override
		protected synchronized void updateMultiple() {
			updateSingle();
		}

		@Override
		protected synchronized void updateNull() {
			rhoTable.setModel(AbstractTableCreator.createBlankTable());
		}

		@Override
		public synchronized void setLoading() {
			rhoTable.setModel(AbstractTableCreator.createLoadingTable());
		}

		@Override
		protected synchronized TableModel createPanelTableType(@NonNull TableOptions options) {
			return new ScatterTableDatasetCreator(options).createSpearmanCorrlationTable(component);
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
			refreshCache();
		}
	}
}
