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
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.core.InputSupplier;
import com.bmskinner.nma.gui.events.revamp.NuclearSignalUpdatedListener;
import com.bmskinner.nma.gui.tabs.ChartDetailPanel;
import com.bmskinner.nma.visualisation.charts.AbstractChartFactory;
import com.bmskinner.nma.visualisation.charts.ViolinChartFactory;
import com.bmskinner.nma.visualisation.charts.panels.ExportableChartPanel;
import com.bmskinner.nma.visualisation.options.ChartOptions;
import com.bmskinner.nma.visualisation.options.ChartOptionsBuilder;

/**
 * Show the minimum distances between signals within a dataset
 * 
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class SignalsColocalisationPanel extends ChartDetailPanel implements NuclearSignalUpdatedListener {

	private static final String PANEL_TITLE_LBL = "Colocalisation";
	private static final String HEADER_LBL = "Pairwise distances between the closest signal pairs centres-of-mass";

	private ExportableChartPanel violinChart;

	public SignalsColocalisationPanel() {
		super();
		this.setLayout(new BorderLayout());

		JPanel header = createHeader();
		JPanel mainPanel = createMainPanel();

		this.add(header, BorderLayout.NORTH);
		this.add(mainPanel, BorderLayout.CENTER);

		uiController.addNuclearSignalUpdatedListener(this);
	}

	@Override
	public String getPanelTitle() {
		return PANEL_TITLE_LBL;
	}

	/**
	 * Create the header panel
	 * 
	 * @return
	 */
	private JPanel createHeader() {
		JPanel panel = new JPanel();

		JLabel label = new JLabel(HEADER_LBL);

		panel.add(label);
		return panel;
	}

	/**
	 * Create the main panel with charts and tables
	 * 
	 * @return
	 */
	private JPanel createMainPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		violinChart = new ExportableChartPanel(AbstractChartFactory.createEmptyChart());
		panel.add(violinChart, BorderLayout.CENTER);

		return panel;
	}

	@Override
	protected synchronized void updateSingle() {
		ChartOptions chartOptions = new ChartOptionsBuilder().setDatasets(getDatasets())
				.setScale(GlobalOptions.getInstance().getScale()).setTarget(violinChart).build();

		setChart(chartOptions);

	}

	@Override
	protected synchronized void updateMultiple() {
		ChartOptions chartOptions = new ChartOptionsBuilder().setDatasets(getDatasets())
				.setScale(GlobalOptions.getInstance().getScale()).setTarget(violinChart).build();

		setChart(chartOptions);
	}

	@Override
	protected synchronized void updateNull() {
		violinChart.setChart(AbstractChartFactory.createEmptyChart());
	}

	@Override
	public synchronized void setLoading() {
		super.setLoading();
		violinChart.setChart(AbstractChartFactory.createLoadingChart());

	}

	@Override
	protected synchronized JFreeChart createPanelChartType(@NonNull ChartOptions options) {
		return new ViolinChartFactory(options).createSignalColocalisationViolinChart();
	}

//	@Override
//	protected synchronized TableModel createPanelTableType(@NonNull TableOptions options) {
//		return new NuclearSignalTableCreator(options).createSignalColocalisationTable();
//	}

	@Override
	public void nuclearSignalUpdated(List<IAnalysisDataset> datasets) {
		refreshCache(datasets);
	}

	@Override
	public void nuclearSignalUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}
}
