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
package com.bmskinner.nuclear_morphology.gui.components;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.io.Io.Exporter;

/**
 * An extension to a JTable that allows the table contents to be exported
 * to file, or copied to the clipboard.
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class ExportableTable extends JTable {
	
	private boolean isGlobalEditable = true;
	
    /**
     * Create a table with a provided model. Specify a global editing override.
     * @param model the table model
     * @param isGlobalEditable true if some or all cells are to be editable, false otherwise
     */
    public ExportableTable(TableModel model, boolean isGlobalEditable) {
        super(model);
        setComponentPopupMenu(new TablePopupMenu());
        this.isGlobalEditable = isGlobalEditable;
    }
    
    /**
     * Create an empty table
     */
    public ExportableTable() {
        super();
    }

    /**
     * Create with a table model
     * @param model the table model
     */
    public ExportableTable(TableModel model) {
        this(model, true);
        setComponentPopupMenu(new TablePopupMenu());
    }
    
    @Override
    public boolean isCellEditable(int row, int column) {
    	if(isGlobalEditable)
    		return super.isCellEditable(row, column);
    	else
    		return false;
    }
    
    /**
     * The popup menu for the table
     * @author bms41
     *
     */
    private class TablePopupMenu extends JPopupMenu {
    	
    	private static final String EXPORT_LBL = "Export";
    	private static final String COPY_LBL   = "Copy";

        public TablePopupMenu() {
            super("Popup");
            createButtons();
           
        }
        
        private void createButtons() {
        	JMenuItem exportItem = new JMenuItem(EXPORT_LBL);
        	exportItem.addActionListener(e->export());
        	
        	JMenuItem copyItem = new JMenuItem(COPY_LBL);
        	copyItem.addActionListener(e->copy());
        	
        	add(copyItem);
        	add(exportItem);
        }

        private void export() {

            File saveFile = FileSelector.chooseTableExportFile();

            if(saveFile!=null){
                String string = makeExportString();
                Exporter.writeString(string,saveFile);
            }

        }
        
        private void copy() {
        	 String string = makeExportString();
        	 StringSelection stringSelection = new StringSelection(string);
        	 Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        	 clipboard.setContents(stringSelection, null);
        }

        private String makeExportString() {
            StringBuilder builder = new StringBuilder();
            TableModel model = getModel();
            for (int col = 0; col < model.getColumnCount(); col++) {
                builder.append(model.getColumnName(col) + Io.TAB);
                ;
            }
            builder.append(Io.NEWLINE);
            for (int row = 0; row < model.getRowCount(); row++) {

                for (int col = 0; col < model.getColumnCount(); col++) {
                    Object value = model.getValueAt(row, col);
                    builder.append(value + Io.TAB);
                }
                builder.append(Io.NEWLINE);
            }
            return builder.toString();
        }

    }

}
