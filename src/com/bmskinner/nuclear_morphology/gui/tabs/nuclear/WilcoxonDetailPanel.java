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

import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.components.renderers.WilcoxonTableCellRenderer;
import com.bmskinner.nuclear_morphology.gui.tabs.AbstractPairwiseDetailPanel;
import com.bmskinner.nuclear_morphology.visualisation.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.visualisation.datasets.tables.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.visualisation.options.TableOptions;
import com.bmskinner.nuclear_morphology.visualisation.options.TableOptionsBuilder;

@SuppressWarnings("serial")
public class WilcoxonDetailPanel extends AbstractPairwiseDetailPanel {

    private static final String PANEL_TITLE_LBL = "Wilcoxon stats";
    public WilcoxonDetailPanel() {
        super();
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

        for (Measurement stat : Measurement.getNucleusStats()) {

            ExportableTable table = new ExportableTable(AbstractTableCreator.createLoadingTable());

            TableOptions options = new TableOptionsBuilder()
            		.setDatasets(getDatasets())
            		.addStatistic(stat)
                    .setTarget(table)
                    .setColumnRenderer(TableOptions.ALL_EXCEPT_FIRST_COLUMN, new WilcoxonTableCellRenderer())
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
