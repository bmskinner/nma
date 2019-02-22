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
package com.bmskinner.nuclear_morphology.gui.tabs.cells;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JScrollPane;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.MorphologyChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ViolinChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.tabs.BoxplotsTabPanel;

/**
 * Display boxplots for whole cell data
 * 
 * @author bms41
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class CellsBoxplotsPanel extends BoxplotsTabPanel implements ActionListener {

    public CellsBoxplotsPanel(@NonNull InputSupplier context) {
        super(context, CellularComponent.WHOLE_CELL);

        Dimension preferredSize = new Dimension(200, 300);

        for (PlottableStatistic stat : PlottableStatistic.getCellStats()) {

            JFreeChart chart = AbstractChartFactory.createEmptyChart();
            ViolinChartPanel panel = new ViolinChartPanel(chart);

            panel.setPreferredSize(preferredSize);
            chartPanels.put(stat.toString(), panel);
            mainPanel.add(panel);

        }

        // add the scroll pane to the tab
        scrollPane = new JScrollPane(mainPanel);
        this.add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        update(getDatasets());

    }

    @Override
    protected synchronized void updateSingle() {
        super.updateSingle();
        finest("Passing to update multiple in " + this.getClass().getName());
        updateMultiple();

    }

    @Override
    protected synchronized void updateMultiple() {
        super.updateMultiple();

        for (PlottableStatistic stat : PlottableStatistic.getCellStats()) {

            ExportableChartPanel panel = chartPanels.get(stat.toString());

            ChartOptions options = new ChartOptionsBuilder()
            		.setDatasets(getDatasets())
            		.addStatistic(stat)
                    .setScale(GlobalOptions.getInstance().getScale())
                    .setSwatch(GlobalOptions.getInstance().getSwatch())
                    .setTarget(panel)
                    .build();

            setChart(options);
        }

    }

    @Override
    protected synchronized void updateNull() {
        super.updateNull();
        updateMultiple();
    }

    @Override
    public void setChartsAndTablesLoading() {
        super.setChartsAndTablesLoading();

        for (PlottableStatistic stat : PlottableStatistic.getCellStats()) {
            ExportableChartPanel panel = chartPanels.get(stat.toString());
            panel.setChart(MorphologyChartFactory.createLoadingChart());

        }
    }
}
