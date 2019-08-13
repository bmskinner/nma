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
package com.bmskinner.nuclear_morphology.gui.tabs.populations;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;

/**
 * A node in the populations tree table.
 * @author ben
 *
 */
public class PopulationTreeTableNode extends DefaultMutableTreeTableNode {

    Object[] columnData = new Object[3];

    IAnalysisDataset dataset = null; // the dataset in the node
    IClusterGroup    group   = null;
    IWorkspace          ws   = null;
    
    /**
     * Create with no content. This should be used only for the root node 
     */
    public PopulationTreeTableNode(){
        super();
    }
    
    /**
     * Create to hold an analysis dataset
     * @param dataset
     */
    public PopulationTreeTableNode(IAnalysisDataset dataset) {
        super(dataset.getId().toString());
        this.dataset = dataset;
        columnData[0] = dataset;
        columnData[1] = dataset.getCollection().size();
    }

    /**
     * Create to hold a cluster group
     * @param dataset
     */
    public PopulationTreeTableNode(IClusterGroup group) {
        super(UUID.randomUUID().toString());
        this.group = group;
        columnData[0] = group;
        columnData[1] = "";

    }
    
    /**
     * Create to hold a workspace
     * @param dataset
     */
    public PopulationTreeTableNode(IWorkspace ws) {
        super(UUID.randomUUID().toString());
        this.ws = ws;
        columnData[0] = ws;
        columnData[1] = "";

    }

    public boolean hasDataset() {
        return dataset != null;
    }

    public boolean hasClusterGroup() {
        return group != null;
    }
    
    public boolean hasWorkspace() {
        return ws != null;
    }

    public IAnalysisDataset getDataset() {
        return dataset;
    }

    public IClusterGroup getGroup() {
        return group;
    }
    
    public IWorkspace getWorkspace() {
        return ws;
    }
    
    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Object getValueAt(int column) {
        return columnData[column];
    }

    @Override
    public void setValueAt(Object aValue, int column) {
        columnData[column] = aValue;
    }

    @Override
    public String toString() {
        if (this.hasDataset()) {
            return dataset.getName();
        }
        if (this.hasClusterGroup()) {
            return group.toString();
        }
        
        if (this.hasWorkspace()) {
            return ws.toString();
        }
        return "No name found";
    }

    /**
     * This method recursively (or not) sorts the nodes, ascending, or
     * descending by the specified column.
     * 
     * @param sortColumn Column to sort by.
     * @param sortAscending Should sorting be ascending (true) or descending (false).
     * @param recursive Should sorting be recursively applied to children nodes.
     * @author Alex Burdu Burdusel
     */
    public void sortNode(int sortColumn, boolean sortAscending, boolean recursive) {

        int childCount = this.getChildCount();

        TreeMap<Object, PopulationTreeTableNode> nodeData = new TreeMap(String.CASE_INSENSITIVE_ORDER);

        for (int i = 0; i < childCount; i++) {
            PopulationTreeTableNode child = (PopulationTreeTableNode) this.getChildAt(i);
            if (child.getChildCount() > 0 && recursive) {
                child.sortNode(sortColumn, sortAscending, recursive);
            }
            Object key = child.getValueAt(sortColumn);
            nodeData.put(key, child);
        }

        Iterator<Map.Entry<Object, PopulationTreeTableNode>> nodesIterator;
        if (sortAscending) {
            nodesIterator = nodeData.entrySet().iterator();
        } else {
            nodesIterator = nodeData.descendingMap().entrySet().iterator();
        }

        while (nodesIterator.hasNext()) {
            Map.Entry<Object, PopulationTreeTableNode> nodeEntry = nodesIterator.next();
            this.add(nodeEntry.getValue());
        }
    }
}
