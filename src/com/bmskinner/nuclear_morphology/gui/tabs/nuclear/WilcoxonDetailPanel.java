/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.gui.tabs.nuclear;

import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.charting.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.tables.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.components.WilcoxonTableCellRenderer;
import com.bmskinner.nuclear_morphology.gui.tabs.AbstractPairwiseDetailPanel;

@SuppressWarnings("serial")
public class WilcoxonDetailPanel extends AbstractPairwiseDetailPanel {

    private static final String PANEL_TITLE_LBL = "Wilcoxon stats";
    public WilcoxonDetailPanel(@NonNull InputSupplier context) {
        super(context);
    }

    @Override
    protected void updateSingle() {
        scrollPane.setColumnHeaderView(null);
        tablePanel = createTablePanel();

        JPanel panel = new JPanel(new FlowLayout());
        panel.add(new JLabel(Labels.SINGLE_DATASET, JLabel.CENTER));
        tablePanel.add(panel);

        scrollPane.setViewportView(tablePanel);
        tablePanel.repaint();
    }
    
    @Override
    public String getPanelTitle(){
        return PANEL_TITLE_LBL;
    }

    @Override
    protected void updateMultiple() {
        scrollPane.setColumnHeaderView(null);
        tablePanel = createTablePanel();

        for (PlottableStatistic stat : PlottableStatistic.getNucleusStats()) {

            ExportableTable table = new ExportableTable(AbstractTableCreator.createLoadingTable());

            TableOptions options = new TableOptionsBuilder()
            		.setDatasets(getDatasets())
            		.addStatistic(stat)
                    .setTarget(table)
                    .setRenderer(TableOptions.ALL_EXCEPT_FIRST_COLUMN, new WilcoxonTableCellRenderer())
                    .build();

            addWilconxonTable(tablePanel, table, stat.toString());
            scrollPane.setColumnHeaderView(table.getTableHeader());
            setTable(options);

        }
        tablePanel.revalidate();
        scrollPane.setViewportView(tablePanel);
        tablePanel.repaint();

    }

    @Override
    protected void updateNull() {
        scrollPane.setColumnHeaderView(null);
        tablePanel = createTablePanel();

        JPanel panel = new JPanel(new FlowLayout());
        panel.add(new JLabel(Labels.NO_DATA_LOADED, JLabel.CENTER));
        tablePanel.add(panel);
        scrollPane.setViewportView(tablePanel);
        tablePanel.repaint();
    }

    @Override
    protected TableModel createPanelTableType(TableOptions options) {
        return new AnalysisDatasetTableCreator(options).createWilcoxonStatisticTable(CellularComponent.NUCLEUS);
    }
}
