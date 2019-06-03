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
package com.bmskinner.nuclear_morphology.gui.tabs.nuclear;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.BoxplotChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.MorphologyChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.ViolinChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ViolinChartPanel;
import com.bmskinner.nuclear_morphology.charting.datasets.tables.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.tables.NucleusTableCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;

@SuppressWarnings("serial")
public class NuclearLobesPanel extends DetailPanel {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static final String PANEL_TITLE_LBL = "Lobes";

    private ExportableChartPanel chartPanel;
    private ExportableTable      table;

    public NuclearLobesPanel(@NonNull InputSupplier context) {
    	super(context);
        this.setLayout(new BorderLayout());

        JPanel header = createHeader();
        this.add(header, BorderLayout.NORTH);

        chartPanel = createMainPanel();
        this.add(chartPanel, BorderLayout.CENTER);

        this.add(createTablePanel(), BorderLayout.WEST);

    }
    
    @Override
    public String getPanelTitle(){
        return PANEL_TITLE_LBL;
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new FlowLayout());
        return panel;
    }

    private ExportableChartPanel createMainPanel() {
    	JFreeChart chart = AbstractChartFactory.createEmptyChart();
        return new ViolinChartPanel(chart);
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        table = new ExportableTable(AbstractTableCreator.createBlankTable());

        JScrollPane pane = new JScrollPane(table);

        panel.add(pane);
        return panel;
    }

    @Override
    protected void updateSingle() {
        super.updateSingle();

        LOGGER.finest( "Passing to update multiple in " + this.getClass().getName());
        updateMultiple();
        // runLobeDetectionBtn.setEnabled(true);
    }

    @Override
    protected void updateMultiple() {
        super.updateMultiple();
        // runLobeDetectionBtn.setEnabled(false);
        ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets())
                .addStatistic(PlottableStatistic.LOBE_COUNT).setScale(GlobalOptions.getInstance().getScale())
                .setSwatch(GlobalOptions.getInstance().getSwatch()).setTarget(chartPanel).build();

        TableOptions tableOptions = new TableOptionsBuilder().addStatistic(PlottableStatistic.LOBE_COUNT)
                .setDatasets(getDatasets()).setTarget(table).build();

        setChart(options);
        setTable(tableOptions);

    }

    @Override
    protected void updateNull() {
        super.updateNull();
        LOGGER.finest( "Passing to update multiple in " + this.getClass().getName());
        updateMultiple();
    }

    @Override
    public void setChartsAndTablesLoading() {
        super.setChartsAndTablesLoading();
        chartPanel.setChart(MorphologyChartFactory.createLoadingChart());
        table.setModel(AbstractTableCreator.createLoadingTable());
    }

    @Override
    protected JFreeChart createPanelChartType(ChartOptions options) {
        if (GlobalOptions.getInstance().isViolinPlots()) {
            return new ViolinChartFactory(options).createStatisticPlot(CellularComponent.NUCLEUS);
        } else {
            return new BoxplotChartFactory(options).createStatisticBoxplot(CellularComponent.NUCLEUS);
        }
    }

    @Override
    protected TableModel createPanelTableType(TableOptions options) {
        return new NucleusTableCreator(options).createLobeDetectionOptionsTable();
    }

}
