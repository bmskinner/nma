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


package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.charting.datasets.SignalTableCell;
import com.bmskinner.nuclear_morphology.charting.datasets.tables.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.tables.NuclearSignalTableCreator;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.tabs.CosmeticHandler;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;

import ij.io.DirectoryChooser;

@SuppressWarnings("serial")
public class SignalsAnalysisPanel extends DetailPanel {

    private static final String PANEL_TITLE_LBL = "Detection settings";
    private ExportableTable table;     // table for analysis parameters
    private JScrollPane     scrollPane;
    private final CosmeticHandler cosmeticHandler = new CosmeticHandler(this);

    public SignalsAnalysisPanel() {
        super();
        this.setLayout(new BorderLayout());

        table = new ExportableTable(new DefaultTableModel());

        table.setEnabled(false);

        table.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                JTable table = (JTable) e.getSource();

                int row = table.rowAtPoint(e.getPoint());
                int column = table.columnAtPoint(e.getPoint());
                IAnalysisDataset d = getDatasets().get(column - 1);
                // double click
                if (e.getClickCount() == 2) {

                    String rowName = table.getModel().getValueAt(row, 0).toString();

                    if (rowName.equals("Source")) {

                        SignalTableCell signalGroup = getSignalGroupFromTable(table, row - 2, column);
                        updateSignalSource(signalGroup);
                    }

                    if (rowName.equals(Labels.SIGNAL_GROUP_LABEL)) {
                        SignalTableCell signalGroup = getSignalGroupFromTable(table, row, column);
                        cosmeticHandler.renameSignalGroup(d, signalGroup.getID());
                    }

                    String nextRowName = table.getModel().getValueAt(row + 1, 0).toString();
                    if (nextRowName.equals(Labels.SIGNAL_GROUP_LABEL)) {
                        SignalTableCell signalGroup = getSignalGroupFromTable(table, row + 1, column);
                        cosmeticHandler.changeSignalColour(d, signalGroup.getColor(), signalGroup.getID());
                        update(getDatasets());
                        getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
                    }

                }

            }
        });

        scrollPane = new JScrollPane(table);
        this.add(scrollPane, BorderLayout.CENTER);
    }

    
    @Override
    public String getPanelTitle(){
        return PANEL_TITLE_LBL;
    }
    
    private SignalTableCell getSignalGroupFromTable(JTable table, int row, int column) {
        return (SignalTableCell) table.getModel().getValueAt(row, column);
        // return UUID.fromString( table.getModel().getValueAt(row,
        // column).toString() );
    }

    private void updateSignalSource(SignalTableCell signalGroup) {
        if (isSingleDataset()) {
            finest("Updating signal source for signal group " + signalGroup);

            DirectoryChooser openDialog = new DirectoryChooser("Select directory of signal images...");
            String folderName = openDialog.getDirectory();

            if (folderName == null) {
                finest("Folder name null");
                return;
            }

            File folder = new File(folderName);

            if (!folder.isDirectory()) {
                finest("Folder is not directory");
                return;
            }
            if (!folder.exists()) {
                finest("Folder does not exist");
                return;
            }

            activeDataset().getCollection().getSignalManager().updateSignalSourceFolder(signalGroup.getID(), folder);
            // SignalsDetailPanel.this.update(getDatasets());
            refreshTableCache();
            finest("Updated signal source for signal group " + signalGroup + " to " + folder.getAbsolutePath());
        }
    }

    @Override
    protected void updateSingle() {

        TableOptions options = new TableOptionsBuilder().setDatasets(getDatasets()).setTarget(table)
                .setRenderer(TableOptions.ALL_EXCEPT_FIRST_COLUMN, new SignalDetectionSettingsTableCellRenderer())
                .build();

        setTable(options);
    }

    @Override
    protected void updateMultiple() {
        updateSingle();
    }

    @Override
    protected void updateNull() {

        TableOptions options = new TableOptionsBuilder().setDatasets(null).build();

        TableModel model = getTable(options);
        table.setModel(model);
        table.createDefaultColumnsFromModel();

    }

    @Override
    public void setChartsAndTablesLoading() {
        super.setChartsAndTablesLoading();
        table.setModel(AbstractTableCreator.createLoadingTable());

    }

    @Override
    protected TableModel createPanelTableType(TableOptions options) {
        return new NuclearSignalTableCreator(options).createSignalDetectionParametersTable();
    }

}
