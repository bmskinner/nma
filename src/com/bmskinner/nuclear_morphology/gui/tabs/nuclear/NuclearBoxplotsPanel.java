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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

@SuppressWarnings("serial")
public class NuclearBoxplotsPanel extends BoxplotsTabPanel implements ActionListener {

    public NuclearBoxplotsPanel(@NonNull InputSupplier context) {
        super(context, CellularComponent.NUCLEUS);

        Dimension preferredSize = new Dimension(200, 300);
        
        int values = PlottableStatistic.getNucleusStats().size();


        for (PlottableStatistic stat : PlottableStatistic.getNucleusStats()) {

        	JFreeChart chart = AbstractChartFactory.createEmptyChart();
            ViolinChartPanel panel = new ViolinChartPanel(chart);
            panel.getChartRenderingInfo().setEntityCollection(null);
            panel.setPreferredSize(preferredSize);
            chartPanels.put(stat.toString(), panel);
            mainPanel.add(panel);
        }

        // add the scroll pane to the tab
//        scrollPane = new JScrollPane(mainPanel);
        
//        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
//        Dimension preferredFloatingDimension = new Dimension( (int) (screenSize.getWidth()*0.25), (int) (screenSize.getHeight()*0.25) );
//        scrollPane.setPreferredSize(preferredFloatingDimension);
        
        this.add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        update(getDatasets());

    }

    @Override
    protected void updateSingle() {
        super.updateSingle();
        finest("Passing to update multiple in " + this.getClass().getName());
        updateMultiple();

    }

    @Override
    protected void updateMultiple() {
        super.updateMultiple();

        for (PlottableStatistic stat : PlottableStatistic.getNucleusStats()) {

            ExportableChartPanel panel = chartPanels.get(stat.toString());

            ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets()).addStatistic(stat)
                    .setScale(GlobalOptions.getInstance().getScale()).setSwatch(GlobalOptions.getInstance().getSwatch())
                    .setTarget(panel).build();

            setChart(options);
        }

    }

    @Override
    protected void updateNull() {
        super.updateNull();
        finest("Passing to update multiple in " + this.getClass().getName());
        updateMultiple();
    }

    @Override
    public void setChartsAndTablesLoading() {
        super.setChartsAndTablesLoading();

        for (PlottableStatistic stat : PlottableStatistic.getNucleusStats()) {
            ExportableChartPanel panel = chartPanels.get(stat.toString());
            panel.setChart(MorphologyChartFactory.createLoadingChart());

        }
    }

}
