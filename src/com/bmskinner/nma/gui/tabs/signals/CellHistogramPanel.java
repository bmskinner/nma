package com.bmskinner.nma.gui.tabs.signals;

import java.awt.BorderLayout;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.gui.components.panels.ExportableChartPanel;
import com.bmskinner.nma.gui.events.NuclearSignalUpdatedListener;
import com.bmskinner.nma.gui.tabs.ChartDetailPanel;
import com.bmskinner.nma.visualisation.charts.AbstractChartFactory;
import com.bmskinner.nma.visualisation.charts.PixelIntensityChartFactory;
import com.bmskinner.nma.visualisation.options.ChartOptions;
import com.bmskinner.nma.visualisation.options.ChartOptionsBuilder;

public class CellHistogramPanel extends ChartDetailPanel
		implements NuclearSignalUpdatedListener {

	private static final String PANEL_TITLE_LBL = "Cell pixel intensities";
	private static final String PANEL_DESC_LBL = "Number of pixels per cell at each intensity";

	private ExportableChartPanel chartPanel;

	/**
	 * Create with an input supplier
	 * 
	 * @param inputSupplier the input supplier
	 */
	public CellHistogramPanel() {
		super(PANEL_TITLE_LBL, PANEL_DESC_LBL);

		setLayout(new BorderLayout());
		createUI();
		uiController.addNuclearSignalUpdatedListener(this);

	}

	@Override
	public String getPanelTitle() {
		return PANEL_TITLE_LBL;
	}

	private void createUI() {
		this.setLayout(new BorderLayout());
//		final JPanel header = createHeader();
//		add(header, BorderLayout.NORTH);

		chartPanel = new ExportableChartPanel(AbstractChartFactory.createEmptyChart());
		add(chartPanel, BorderLayout.CENTER);
	}

	@Override
	protected synchronized void updateSingle() {
		updateMultiple();
	}

	@Override
	protected synchronized void updateMultiple() {
		final ChartOptions options = new ChartOptionsBuilder()
				.setDatasets(getDatasets())
				.setSwatch(GlobalOptions.getInstance().getSwatch())
				.setTarget(chartPanel)
				.build();

		setChart(options);
	}

	@Override
	protected synchronized void updateNull() {
		updateMultiple();
	}

	@Override
	public synchronized void setLoading() {
		super.setLoading();
		chartPanel.setChart(AbstractChartFactory.createLoadingChart());

	}

	@Override
	protected JFreeChart createPanelChartType(@NonNull ChartOptions options) {
		return new PixelIntensityChartFactory(options)
				.createPixelIntensityHistogram();
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
