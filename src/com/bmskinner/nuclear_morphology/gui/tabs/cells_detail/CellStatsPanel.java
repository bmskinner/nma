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
package com.bmskinner.nuclear_morphology.gui.tabs.cells_detail;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.charting.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.SignalTableCell;
import com.bmskinner.nuclear_morphology.charting.datasets.tables.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.tables.CellTableDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.components.renderers.ConsistentRowTableCellRenderer;
import com.bmskinner.nuclear_morphology.gui.dialogs.CellImageDialog;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.tabs.CosmeticHandler;

/**
 * Display for overall stats per cell
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class CellStatsPanel extends AbstractCellDetailPanel {
	
	private static final Logger LOGGER = Logger.getLogger(CellStatsPanel.class.getName());

    private static final String PANEL_TITLE_LBL = "Info";
    
    private ExportableTable table; // individual cell stats

    private JScrollPane scrollPane;

    private JButton scaleButton;
    private JButton sourceButton;
    
    private CosmeticHandler ch = new CosmeticHandler(this);

    public CellStatsPanel(@NonNull InputSupplier context, CellViewModel model) {
        super(context, model, PANEL_TITLE_LBL);
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
                            ch.changeSignalColour(activeDataset(), cell.getID());
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

    private void updateScale() {        
    	ch.changeDatasetScale(activeDataset());
        refreshTableCache();
        getDatasetEventHandler().fireDatasetEvent(DatasetEvent.RECACHE_CHARTS, getDatasets());
    }

    @Override
    public synchronized void refreshTableCache() {
        LOGGER.finest("Preparing to refresh table cache");
        clearTableCache();
        LOGGER.finest("Updating tables after clear");
        this.update();
    }

    @Override
    public synchronized void update() {

    	if (this.isMultipleDatasets() || !this.hasDatasets()) {
    		table.setModel(AbstractTableCreator.createBlankTable());
    		return;
    	}

    	TableOptions options = new TableOptionsBuilder().setDatasets(getDatasets())
    			.setCell(this.getCellModel().getCell())
    			.setScale(GlobalOptions.getInstance().getScale())
    			.setTarget(table)
    			.setColumnRenderer(TableOptions.ALL_COLUMNS, new StatsTableCellRenderer())
    			.build();

    	setTable(options);
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
        if (getCellModel().hasCell())
            return new CellTableDatasetCreator(options, getCellModel().getCell()).createCellInfoTable();
        return AbstractTableCreator.createBlankTable();
    }

    /**
     * Allows for cell background to be coloured based on position in a list.
     * Used to colour the signal stats list
     *
     */
    private class StatsTableCellRenderer extends ConsistentRowTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
		public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
        	
        	Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            Color bg = Color.WHITE;
            Color fg = Color.BLACK;
            
            // Highlight missing images
            String header = getFirstColumnText(row, table);
            if(header.equals(Labels.Cells.SOURCE_FILE_LABEL) && column>0) {
            	File f = new File(value.toString());
            	if(!f.exists()) {
            		fg = Color.RED;
            	}
            }
            
            // Colour signal groups
            if (row < table.getModel().getRowCount() - 1 && column==0) {

                int nextRow = row + 1;
                String nextRowHeader = getFirstColumnText(nextRow, table);

                if (nextRowHeader.equals(Labels.Signals.SIGNAL_GROUP_LABEL)) {
                    // colour this cell preemptively based on the signal group in the next row
                    SignalTableCell tableCell = (SignalTableCell) table.getModel().getValueAt(nextRow, 1);
                    bg = tableCell.getColor();
                }
            }
            // Cells are by default rendered as a JLabel.
            
            c.setBackground(bg);
            c.setForeground(fg);
            return c;
        }
    }

}
