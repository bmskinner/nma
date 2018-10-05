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

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.io.Io;

@SuppressWarnings("serial")
public class ExportableTable extends JTable {

    final private TablePopupMenu popup;

    public ExportableTable() {
        super();
        popup = new TablePopupMenu(this);
        this.setComponentPopupMenu(popup);
    }

    public ExportableTable(TableModel model) {
        super(model);
        popup = new TablePopupMenu(this);
        this.setComponentPopupMenu(popup);
    }

    private class TablePopupMenu extends JPopupMenu {

        final private ExportableTable table;

        final private JMenuItem exportMenuItem = new JMenuItem(new AbstractAction("Export") {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                export();
            }
        });

        public TablePopupMenu(final ExportableTable table) {

            super("Popup");
            this.table = table;
            this.add(exportMenuItem);

        }

        private void export() {

            File saveFile = FileSelector.chooseTableExportFile();

            if(saveFile!=null){

                String string = makeExportString();
                Io.Exporter.writeString(string,saveFile);
            }

        }

        private String makeExportString() {
            StringBuilder builder = new StringBuilder();
            TableModel model = table.getModel();
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
