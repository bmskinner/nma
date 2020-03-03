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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.charting.datasets.tables.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.tables.CellTableDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Display for segments per cell 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class CellSegTablePanel extends AbstractCellDetailPanel {
	
	private static final Logger LOGGER = Logger.getLogger(CellSegTablePanel.class.getName());

    private static final String PANEL_TITLE_LBL = "Segment table";
    
    private JTable table;

    public CellSegTablePanel(@NonNull InputSupplier context, final CellViewModel model) {
        super(context, model, PANEL_TITLE_LBL);

        this.setLayout(new BorderLayout());
        this.setBorder(null);

        TableModel tableModel = AbstractTableCreator.createBlankTable();

        table = new JTable(tableModel);
        JScrollPane sp = new JScrollPane(table);
        sp.setColumnHeaderView(table.getTableHeader());
        add(sp, BorderLayout.CENTER);

    }
    
    @Override
    public synchronized void update() {
        if (this.isMultipleDatasets() || !this.hasDatasets()) {
            table.setModel(AbstractTableCreator.createBlankTable());
            return;
        }

        if (!getCellModel().hasCell()) {
            table.setModel(AbstractTableCreator.createBlankTable());
            return;
        }

        TableOptions options = new TableOptionsBuilder().setDatasets(getDatasets())
                .setCell(this.getCellModel().getCell()).setScale(GlobalOptions.getInstance().getScale())
                .setTarget(table).build();

        try {

            setTable(options);

        } catch (Exception e) {
        	LOGGER.log(Level.WARNING, "Error updating cell segments table");
            LOGGER.log(Loggable.STACK, "Error updating cell segments table", e);
        }

    }

    @Override
    protected TableModel createPanelTableType(TableOptions options) {
        return new CellTableDatasetCreator(options, getCellModel().getCell()).createCellSegmentsTable();
    }

}
