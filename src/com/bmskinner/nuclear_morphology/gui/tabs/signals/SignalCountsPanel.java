package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import java.awt.BorderLayout;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.ViolinChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;

/**
 * Plot the number of signals per nucleus/per cell
 * @author ben
 * @since 1.14.0
 *
 */
@SuppressWarnings("serial")
public class SignalCountsPanel extends DetailPanel {
	
	private static final String PANEL_TITLE_LBL = "Signal counts";
	
	private ExportableChartPanel chartPanel;
	
	public SignalCountsPanel(@NonNull InputSupplier context) {
        super(context, PANEL_TITLE_LBL);
        createUI();
    }

    private void createUI() {
        this.setLayout(new BorderLayout());
        chartPanel = new ExportableChartPanel(AbstractChartFactory.createEmptyChart());
        add(chartPanel, BorderLayout.CENTER);
    }

    @Override
    protected void updateSingle() {
        updateMultiple();
    }

    @Override
    protected void updateMultiple() {
    	ChartOptions options = new ChartOptionsBuilder()
    			.setDatasets(getDatasets())
        		.addStatistic(PlottableStatistic.NUCLEUS_SIGNAL_COUNT)
        		.setScale(GlobalOptions.getInstance().getScale())
        		.setSwatch(GlobalOptions.getInstance().getSwatch())
        		.setTarget(chartPanel)
        		.build();
        
        setChart(options);
    }

    @Override
    protected void updateNull() {
        updateMultiple();
    }
    
    @Override
    public void setChartsAndTablesLoading() {
        super.setChartsAndTablesLoading();
        chartPanel.setChart(AbstractChartFactory.createLoadingChart());

    }

    @Override
    protected JFreeChart createPanelChartType(@NonNull ChartOptions options) {
        return new ViolinChartFactory(options).createStatisticPlot(CellularComponent.NUCLEAR_SIGNAL) ;
    }

}
