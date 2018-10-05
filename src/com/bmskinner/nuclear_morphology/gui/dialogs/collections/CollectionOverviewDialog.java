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
package com.bmskinner.nuclear_morphology.gui.dialogs.collections;

import java.awt.Color;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.dialogs.LoadingIconDialog;
import com.bmskinner.nuclear_morphology.gui.tabs.cells_detail.LabelInfo;
import com.bmskinner.nuclear_morphology.io.ImageImportWorker;

/**
 * Display collections of images from a dataset. Uses a SwingWorker
 * to import the images.
 * @author ben
 * @since 1.13.7
 *
 */
public abstract class CollectionOverviewDialog extends LoadingIconDialog implements PropertyChangeListener {

    public static final int COLUMN_COUNT = 3;
    
    protected static final String LOADING_LBL = "Loading";

    protected IAnalysisDataset dataset;
    protected JTable           table;
    protected JProgressBar     progressBar;
    protected ImageImportWorker worker;

    /**
     * Construct with a dataset to display
     * @param dataset
     */
    public CollectionOverviewDialog(IAnalysisDataset dataset) {
        super();
        this.dataset = dataset;

        createUI();
        createWorker();

        this.setModal(false);
        this.pack();
        this.setVisible(true);
    }
    
    /**
     * Create the image import worker needed to import and
     * annotate images.
     */
    protected abstract void createWorker();
    
    /**
     * Create the UI of the dialog
     */
    protected abstract void createUI();
    
    protected abstract JPanel createHeader();
    
    protected TableModel createEmptyTableModel(int rows, int cols) {
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) { // custom
                                                                 // isCellEditable
                                                                 // function
                return false;
            }
        };

        model.setRowCount(rows);
        model.setColumnCount(cols);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {

                LabelInfo l = new LabelInfo(null, null);
                model.setValueAt(l, row, col);
            }
        }

        return model;
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        int value = 0;
        try {
            Object newValue = evt.getNewValue();

            if (newValue.getClass().isAssignableFrom(Integer.class)) {
                value = (int) newValue;

            }
            if (value >= 0 && value <= 100) {
                progressBar.setValue(value);
            }

            if (evt.getPropertyName().equals("Finished")) {
                finest("Worker signaled finished");
                progressBar.setVisible(false);

            }

        } catch (Exception e) {
            error("Error getting value from property change", e);
        }

    }

 // @SuppressWarnings("serial")
    public class LabelInfoRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            LabelInfo info = (LabelInfo) value;
            setIcon(info.getIcon());
            setHorizontalAlignment(JLabel.CENTER);
            setHorizontalTextPosition(JLabel.CENTER);
            setVerticalTextPosition(JLabel.BOTTOM);

            if (info.isSelected()) {
                setBackground(Color.GREEN);
            } else {
                setBackground(Color.WHITE);
            }

            return this;
        }
    }
}
