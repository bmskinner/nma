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


package com.bmskinner.nuclear_morphology.gui.tabs.populations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.ListSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.TreeTableModel;

import com.bmskinner.nuclear_morphology.components.ClusterGroup;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.gui.DatasetListManager;
import com.bmskinner.nuclear_morphology.gui.tabs.populations.PopulationsPanel.TreeSelectionHandler;
import com.bmskinner.nuclear_morphology.logging.Loggable;

@SuppressWarnings("serial")
public class PopulationTreeTable extends JXTreeTable implements Loggable {

    /**
     * The column index for the dataset name
     */
    public static final int COLUMN_NAME = 0;

    /**
     * The column index for the dataset cell count
     */
    public static final int COLUMN_CELL_COUNT = 1;

    /**
     * The column index for the dataset chart colour
     */
    public static final int COLUMN_COLOUR = 2;

    /**
     * The default width for the name column
     */
    public static final int DEFAULT_NAME_COLUMN_WIDTH = 120;

    /**
     * The default width for the colour column
     */
    public static final int DEFAULT_COLOUR_COLUMN_WIDTH = 5;

    private TreeSelectionHandler treeListener;

    public PopulationTreeTable() {
        super();
        setDefaults();
    }

    public PopulationTreeTable(TreeTableModel model) {
        super(model);
        setDefaults();
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    private void setDefaults() {
        setEnabled(true);
        setCellSelectionEnabled(false);
        setColumnSelectionAllowed(false);
        getTableHeader().setReorderingAllowed(false);
        setRowSelectionAllowed(true);
        setAutoCreateColumnsFromModel(false);
        getColumnModel().getColumn(PopulationTreeTable.COLUMN_COLOUR)
                .setCellRenderer(new PopulationTableCellRenderer());
        getColumnModel().getColumn(PopulationTreeTable.COLUMN_NAME).setPreferredWidth(DEFAULT_NAME_COLUMN_WIDTH);
        getColumnModel().getColumn(PopulationTreeTable.COLUMN_COLOUR).setPreferredWidth(DEFAULT_COLOUR_COLUMN_WIDTH);
    }

    public void setTreeSelectionListener(TreeSelectionHandler t) {
        treeListener = t;
    }

    /**
     * Get the index of the given dataset in the table model
     * 
     * @return the index
     */
    public int getRowIndex(IAnalysisDataset dataset) {

        for (int row = 0; row < this.getRowCount(); row++) {

            Object rowObject = this.getValueAt(row, COLUMN_NAME);

            if (rowObject instanceof IAnalysisDataset) {
                UUID targetID = ((IAnalysisDataset) rowObject).getUUID();
                if (dataset.getUUID().equals(targetID)) {
                    return row;
                }
            }
        }
        return -1; // no dataset found

    }

    /**
     * Select the given datasets in the tree table
     * 
     * @param dataset
     *            the dataset to select
     */
    public void selectDatasets(List<IAnalysisDataset> list) {
        finer("Selecting list of " + list.size() + " datasets in populations panel");

        PopulationTreeTableModel model = (PopulationTreeTableModel) getTreeTableModel();

        Map<Integer, Integer> selectedIndexes = new HashMap<Integer, Integer>(0);
        int selectedIndexOrder = 0;
        for (IAnalysisDataset dataset : list) {
            int index = getRowIndex(dataset);

            selectedIndexes.put(index, selectedIndexOrder++);

            ListSelectionModel selectionModel = getSelectionModel();

            TreeSelectionModel treeSelectionModel = getTreeSelectionModel();

            finest("Removing tree selection listener");
            treeSelectionModel.removeTreeSelectionListener(treeListener); // if
                                                                          // we
                                                                          // don't
                                                                          // remove
                                                                          // the
                                                                          // listener,
                                                                          // the
                                                                          // clearing
                                                                          // will
                                                                          // trigger
                                                                          // an
                                                                          // update
            finest("Clearing tree selection");
            selectionModel.clearSelection(); // if the new selection is the same
                                             // as the old, the charts will not
                                             // recache
            finest("Restoring tree selection listener");
            treeSelectionModel.addTreeSelectionListener(treeListener);

            finest("Adding index at " + index);
            selectionModel.addSelectionInterval(index, index); // this will
                                                               // trigger a
                                                               // chart update

        }

        PopulationTableCellRenderer rend = (PopulationTableCellRenderer) getColumnModel().getColumn(COLUMN_COLOUR)
                .getCellRenderer();
        rend.update(selectedIndexes);
    }

    /**
     * Get the names of the datasets in this table
     * 
     * @return
     */
    public List<String> getDatasetNames() {
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < getRowCount(); i++) {
            String s = getValueAt(i, PopulationTreeTable.COLUMN_NAME).toString();
            result.add(s);
        }
        return result;
    }

    /**
     * Get the nodes for all selected datasets
     * 
     * @return
     */
    public List<PopulationTreeTableNode> getSelectedNodes() {

        List<PopulationTreeTableNode> result = new ArrayList<PopulationTreeTableNode>();
        TreePath[] paths = getTreeSelectionModel().getSelectionPaths();
        for (TreePath p : paths) {
            PopulationTreeTableNode n = (PopulationTreeTableNode) p.getLastPathComponent();
            result.add(n);
        }
        return result;
    }

    private List<Integer> getSelectedDatasetIndexes() {
        List<Integer> result = new ArrayList<Integer>();
        List<IAnalysisDataset> datasets = getSelectedDatasets();
        PopulationTreeTableModel model = (PopulationTreeTableModel) getTreeTableModel();

        for (IAnalysisDataset d : datasets) {
            // result.add( model.getRowIndex(d));
            result.add(getRowIndex(d));
        }
        return result;

    }

    private List<IAnalysisDataset> getSelectedDatasets() {
        List<IAnalysisDataset> datasets = new ArrayList<IAnalysisDataset>();

        for (int row = 0; row < getRowCount(); row++) {

            Object ob = getModel().getValueAt(row, PopulationTreeTable.COLUMN_NAME);
            if (ob instanceof IAnalysisDataset) {
                datasets.add((IAnalysisDataset) ob);
            }

        }
        return datasets;
    }

    /**
     * Find the datasets which are collapsed in the tree
     * 
     * @return
     */
    public List<Object> getCollapsedRows() {
        List<Object> collapsedRows = new ArrayList<Object>();
        for (int row = 0; row < getRowCount(); row++) {
            if (!isExpanded(row)) {

                Object columnOneObject = getModel().getValueAt(row, PopulationTreeTable.COLUMN_NAME);
                collapsedRows.add(columnOneObject);
            }
        }
        finest("Got all collapsed rows");
        return collapsedRows;
    }

    /**
     * Set the dataset rows with the given IDs to be collapsed
     * 
     * @param collapsedRows
     */
    public void setCollapsedRows(List<Object> collapsedRows) {
        if (DatasetListManager.getInstance().hasDatasets()) {

            finest("Expanding rows");
            for (int row = 0; row < getRowCount(); row++) {

                Object columnOneObject = getModel().getValueAt(row, PopulationTreeTable.COLUMN_NAME);

                if (collapsedRows.contains(columnOneObject)) {
                    collapseRow(row);
                } else {
                    expandRow(row);
                }
            }
        }
    }

    /**
     * Test if the given row of the table has a dataset
     * 
     * @param rowIndex
     * @return
     */
    public boolean isDataset(int rowIndex) {
        Object columnOneObject = getModel().getValueAt(rowIndex, PopulationTreeTable.COLUMN_NAME);
        if (columnOneObject instanceof IAnalysisDataset) {
            return true;
        }
        return false;
    }

    /**
     * Get the dataset at the given row, or null if no dataset is present
     * 
     * @param rowIndex
     * @return
     */
    public IAnalysisDataset getDatasetAtRow(int rowIndex) {
        Object columnOneObject = getModel().getValueAt(rowIndex, PopulationTreeTable.COLUMN_NAME);

        if (columnOneObject instanceof IAnalysisDataset) {
            return (IAnalysisDataset) columnOneObject; // row i, column 0
        }
        return null;
    }

    /**
     * Get the ClusterGroup at the given row, or null if no group is present
     * 
     * @param rowIndex
     * @return
     */
    public IClusterGroup getClusterGroupAtRow(int rowIndex) {
        Object columnOneObject = getModel().getValueAt(rowIndex, PopulationTreeTable.COLUMN_NAME);

        if (columnOneObject instanceof ClusterGroup) {
            return (IClusterGroup) columnOneObject; // row i, column 0
        }
        return null;
    }

}
