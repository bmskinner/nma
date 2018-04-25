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


package com.bmskinner.nuclear_morphology.gui.tabs.cells_detail;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Optional;
import java.util.UUID;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.charting.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.SignalTableCell;
import com.bmskinner.nuclear_morphology.charting.datasets.tables.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.tables.CellTableDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.gui.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.dialogs.CellImageDialog;
import com.bmskinner.nuclear_morphology.gui.tabs.CosmeticHandler;
import com.bmskinner.nuclear_morphology.main.GlobalOptions;

@SuppressWarnings("serial")
public class CellStatsPanel extends AbstractCellDetailPanel {

    private static final String PANEL_TITLE_LBL = "Info";
    
    private ExportableTable table; // individual cell stats

    private JScrollPane scrollPane;

    private JButton scaleButton;
    private JButton sourceButton;
    
    private CosmeticHandler ch = new CosmeticHandler(this);

    private static final String APPLY_SCALE_ALL_MESSAGE   = "Apply this scale to all cells in the dataset?";
    private static final String APPLY_SCALE_ALL_HEADER    = "Apply to all?";
    private static final String APPLY_SCALE_ALL_CELLS_LBL = "Apply to all cells";
    private static final String APPLY_SCALE_ONE_CELLS_LBL = "Apply to only this cell";
    private static final String CHOOSE_NEW_SCALE_LBL      = "Choose the new scale: pixels per micron";

    public CellStatsPanel(CellViewModel model) {
        super(model, PANEL_TITLE_LBL);
        this.setLayout(new BorderLayout());

        scrollPane = new JScrollPane();

        TableModel tableModel = AnalysisDatasetTableCreator.createBlankTable();

        table = new ExportableTable(tableModel);
        table.setEnabled(false);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                JTable table = (JTable) e.getSource();
                int row = table.rowAtPoint((e.getPoint()));
                String rowName = table.getModel().getValueAt(row, 0).toString();

                // double click
                if (e.getClickCount() == 2) {

                    // Look for signal group colour
                    if (rowName.equals("")) {
                        String nextRowName = table.getModel().getValueAt(row + 1, 0).toString();
                        if (nextRowName.equals(Labels.Signals.SIGNAL_GROUP_LABEL)) {

                            SignalTableCell cell = (SignalTableCell) table.getModel().getValueAt(row + 1, 1);

                            changeSignalGroupColour(cell);

                        }
                    }
                }

            }
        });

        scrollPane.setViewportView(table);
        scrollPane.setColumnHeaderView(table.getTableHeader());

        this.add(scrollPane, BorderLayout.CENTER);

        JPanel header = createHeader();
        this.add(header, BorderLayout.NORTH);

        this.setEnabled(false);
    }
    
    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        scaleButton.setEnabled(b);
        sourceButton.setEnabled(b);
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new FlowLayout());

        scaleButton = new JButton("Change scale");
        scaleButton.addActionListener(e -> {
            updateScale();
        });

        sourceButton = new JButton("Show source image");
        sourceButton.addActionListener(e ->  showCellImage() );

        panel.add(scaleButton);
        panel.add(sourceButton);

        return panel;
    }

    private void showCellImage() {
        new CellImageDialog(this.getCellModel().getCell());
    }

    private void changeSignalGroupColour(SignalTableCell signalGroup) {

        UUID id = signalGroup.getID();
        Color oldColour = activeDataset().getCollection().getSignalGroup(id).get().getGroupColour().orElse(Color.YELLOW);
        ch.changeSignalColour(activeDataset(), oldColour, id);
    }

    private void updateScale() {
        SpinnerNumberModel sModel = new SpinnerNumberModel(getCellModel().getCell().getNucleus().getScale(), 
                1, 100000, 1);
        
        JSpinner spinner = new JSpinner(sModel);

        int option = JOptionPane.showOptionDialog(null, spinner, CHOOSE_NEW_SCALE_LBL, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, null, null);
        if (option == JOptionPane.OK_OPTION) {

            Object[] options = { APPLY_SCALE_ALL_CELLS_LBL, APPLY_SCALE_ONE_CELLS_LBL, };
            int applyAllOption = JOptionPane.showOptionDialog(null, APPLY_SCALE_ALL_MESSAGE, APPLY_SCALE_ALL_HEADER,

                    JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,

                    null, options, options[1]);

            double scale = (double) spinner.getModel().getValue();

            if (scale > 0) { // don't allow a scale to cause divide by zero errors

                if (applyAllOption == 0) { // button at index 1

                	activeDataset().getCollection().setScale(scale);
                	Optional<IAnalysisOptions> op = activeDataset().getAnalysisOptions();
                	if(op.isPresent()){
                		Optional<IDetectionOptions> nOp = op.get().getDetectionOptions(IAnalysisOptions.NUCLEUS);
                		if(nOp.isPresent())
                			nOp.get().setScale(scale);
                	}


                } else {
                    finest("Updating scale for single cell");
                    this.getCellModel().getCell().getNuclei().stream().forEach( n-> {  n.setScale(scale);  } );

                }
                finest("Refreshing cache");
                this.refreshTableCache();
                this.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.REFRESH_CACHE, getDatasets());

            } else {
                warn("Cannot set a scale to zero");
            }
        }
    }

    @Override
    public synchronized void refreshTableCache() {
        finest("Preparing to refresh table cache");
        clearTableCache();
        finest("Updating tables after clear");
        this.update();
    }

    public synchronized void update() {

        if (this.isMultipleDatasets() || !this.hasDatasets()) {
            table.setModel(AbstractTableCreator.createBlankTable());
            return;
        }

        TableOptions options = new TableOptionsBuilder().setDatasets(getDatasets())
                .setCell(this.getCellModel().getCell()).setScale(GlobalOptions.getInstance().getScale())
                .setTarget(table).setRenderer(TableOptions.FIRST_COLUMN, new StatsTableCellRenderer()).build();

        try {

            setTable(options);

        } catch (Exception e) {
            warn("Error updating cell stats table");
            fine("Error updating cell stats table", e);
        }
    }

    @Override
    public void setChartsAndTablesLoading() {

        table.setModel(AbstractTableCreator.createLoadingTable());
    }

    @Override
    protected void updateSingle() {
        update();
    }

    @Override
    protected void updateMultiple() {
        updateNull();
    }

    @Override
    protected void updateNull() {
        table.setModel(AbstractTableCreator.createBlankTable());

    }

    @Override
    protected TableModel createPanelTableType(TableOptions options) {

        if (getCellModel().hasCell()) {
            return new CellTableDatasetCreator(options, getCellModel().getCell()).createCellInfoTable();
        } else {
            return AbstractTableCreator.createBlankTable();
        }
    }

    /**
     * Allows for cell background to be coloured based on position in a list.
     * Used to colour the signal stats list
     *
     */
    private class StatsTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            // default cell colour is white
            Color colour = Color.WHITE;

            // get the value in the first column of the row below
            if (row < table.getModel().getRowCount() - 1) {

                int nextRow = row + 1;
                String nextRowHeader = table.getModel().getValueAt(nextRow, 0).toString();

                if (nextRowHeader.equals("Signal group")) {
                    // we want to colour this cell preemptively
                    // get the signal group from the table

                    SignalTableCell tableCell = (SignalTableCell) table.getModel().getValueAt(nextRow, 1);

                    colour = tableCell.getColor();

                }
            }
            // Cells are by default rendered as a JLabel.
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBackground(colour);

            // Return the JLabel which renders the cell.
            return this;
        }
    }

}
