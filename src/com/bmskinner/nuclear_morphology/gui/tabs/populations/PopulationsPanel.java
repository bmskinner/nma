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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import javax.swing.JScrollPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeSelectionModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * The populations panel holds the list of open datasets for selection by the
 * user.
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class PopulationsPanel extends DetailPanel  {
	
	private static final Logger LOGGER = Logger.getLogger(Loggable.ROOT_LOGGER);

    private static final String PANEL_TITLE_LBL = "Populations";
    
    final private PopulationTreeTable treeTable;

    private PopulationListPopupMenu populationPopup;

    /**
     * This tracks which datasets are currently selected, and the order in which
     * they were selected.
     */
    private final Set<IAnalysisDataset> datasetSelectionOrder = new LinkedHashSet<IAnalysisDataset>();

    final private TreeSelectionHandler treeListener = new TreeSelectionHandler();
    
    private boolean ctrlPressed = false;

    public boolean isCtrlPressed() {
        synchronized (PopulationsPanel.class) {
            return ctrlPressed;
        }
    }
  
    public PopulationsPanel(@NonNull InputSupplier context) {
        super(context);
        this.setLayout(new BorderLayout());

        this.setMinimumSize(new Dimension(100, 100));

        populationPopup = new PopulationListPopupMenu();
        populationPopup.setEnabled(false);
        populationPopup.addSignalChangeListener(this);

        treeTable = createTreeTable();

        JScrollPane populationScrollPane = new JScrollPane(treeTable);

        // Track when the Ctrl key is down
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {

            @Override
            public boolean dispatchKeyEvent(KeyEvent ke) {
                synchronized (PopulationsPanel.class) {
                    switch (ke.getID()) {
                    case KeyEvent.KEY_PRESSED:
                        if (ke.getKeyCode() == KeyEvent.VK_CONTROL) {
                            ctrlPressed = true;
                        }
                        break;

                    case KeyEvent.KEY_RELEASED:
                        if (ke.getKeyCode() == KeyEvent.VK_CONTROL) {
                            ctrlPressed = false;
                        }
                        break;
                    default: {
                        break;
                    }
                    }

                    return false;
                }
            }

        });

        this.add(populationScrollPane, BorderLayout.CENTER);

    }

    @Override
    public String getPanelTitle(){
        return PANEL_TITLE_LBL;
    }
    
    @Override
    public void update(final List<IAnalysisDataset> list) {
        this.update();
        treeTable.selectDatasets(list);
        treeTable.repaint();
    }

    public void update(final IAnalysisDataset dataset) {
        List<IAnalysisDataset> list = new ArrayList<IAnalysisDataset>(1);
        list.add(dataset);
        update(list);
    }

    /**
     * Find the populations in memory, and display them in the population
     * chooser. Root populations are ordered according to position in the
     * treeListOrder map.
     */
    public synchronized void update() {

        int nameColWidth = treeTable.getColumnModel().getColumn(PopulationTreeTable.COLUMN_NAME).getWidth();
        int colourColWidth = treeTable.getColumnModel().getColumn(PopulationTreeTable.COLUMN_COLOUR).getWidth();

        /*
         * Determine the ids of collapsed datasets, and store them
         */
        List<Object> collapsedRows = treeTable.getCollapsedRows();

        // TODO: Need to modify the model, not replace it to keep ordering
        PopulationTreeTableModel newModel = new PopulationTreeTableModel();
        treeTable.setTreeTableModel(newModel);

        /*
         * Collapse the same ids as saved earlier
         */
        treeTable.setCollapsedRows(collapsedRows);

        treeTable.getColumnModel().getColumn(PopulationTreeTable.COLUMN_NAME).setWidth(nameColWidth);
        treeTable.getColumnModel().getColumn(PopulationTreeTable.COLUMN_COLOUR).setWidth(colourColWidth);
    }

    @Override
    public void setChartsAndTablesLoading() {
    }

    private PopulationTreeTable createTreeTable() {

        PopulationTreeTableModel treeTableModel = new PopulationTreeTableModel();

        PopulationTreeTable table = new PopulationTreeTable(treeTableModel);

        table.addMouseListener(new MouseAdapter() {
        	
        	private static final int DOUBLE_CLICK = 2;
            @Override
            public void mouseClicked(MouseEvent e) {

                PopulationTreeTable table = (PopulationTreeTable) e.getSource();

                int row = table.rowAtPoint((e.getPoint()));
                int column = table.columnAtPoint(e.getPoint());

                Object o = table.getModel().getValueAt(row, PopulationTreeTable.COLUMN_NAME);

                if (e.getClickCount() == DOUBLE_CLICK) { // double click
                    if (o instanceof IClusterGroup)
                        clusterGroupClicked((IClusterGroup) o, row, column);
                    if (o instanceof IAnalysisDataset)
                        datasetClicked((IAnalysisDataset) o, row, column);                     
                    if (o instanceof IWorkspace)
                        workspaceClicked((IWorkspace) o, row, column);
                }

                
                if (e.getButton() == MouseEvent.BUTTON3) // right click - show the popup
                    populationPopup.show(table, e.getX(), e.getY());
            }

            private void clusterGroupClicked(IClusterGroup g, int row, int column) {
            	cosmeticHandler.renameClusterGroup(g);
            	table.getModel().setValueAt(g, row, column); // ensure column length supports name by triggering update
            }
            
            private void workspaceClicked(IWorkspace w, int row, int column) {
                cosmeticHandler.renameWorkspace(w);
                table.getModel().setValueAt(w, row, column); // ensure column length supports name 
            }

            private void datasetClicked(IAnalysisDataset d, int row, int column) {

                switch (column) {

                case PopulationTreeTable.COLUMN_NAME: {
                    cosmeticHandler.renameDataset(d);
                    table.getModel().setValueAt(d, row, column); // ensure column length supports name 
                    break;
                }

                case PopulationTreeTable.COLUMN_COLOUR: {
                    cosmeticHandler.changeDatasetColour(d);
                    break;
                }

                default: break;

                }
            }
        });

        TreeSelectionModel tableSelectionModel = table.getTreeSelectionModel();
        tableSelectionModel.addTreeSelectionListener(treeListener);
        return table;
    }

    /**
     * Add the given dataset to the main population list Check that the name is
     * valid, and update if needed
     * 
     * @param dataset
     *            the dataset to add
     */
    public void addDataset(@NonNull IAnalysisDataset dataset) {
        if (dataset.isRoot())
            DatasetListManager.getInstance().addDataset(dataset);
    }

    /**
     * Select the given dataset in the tree table
     * 
     * @param dataset
     *            the dataset to select
     */
    public void selectDataset(@NonNull IAnalysisDataset dataset) {
        if (dataset != null) {
            List<IAnalysisDataset> list = new ArrayList<IAnalysisDataset>();
            list.add(dataset);
            treeTable.selectDatasets(list);
        }
    }

    /**
     * Select the given datasets in the tree table
     * 
     * @param dataset
     *            the dataset to select
     */
    public void selectDatasets(List<IAnalysisDataset> list) {
        treeTable.selectDatasets(list);
        DatasetListManager.getInstance().setSelectedDatasets(list);
    }

    public void repaintTreeTable() {
        treeTable.repaint();
    }

    public synchronized void selectDataset(@NonNull UUID id) {
        IAnalysisDataset d = DatasetListManager.getInstance().getDataset(id);
        this.selectDataset(d);
        DatasetListManager.getInstance().setSelectedDataset(d);
    }

    /**
     * Move the selected dataset in the list
     * 
     * @param isDown
     *            move the dataset down (true) or up (false)
     */
    private void moveDataset(boolean isDown) {
        LOGGER.finer( "Move dataset heard");
        List<IAnalysisDataset> datasets = DatasetListManager.getInstance().getSelectedDatasets();
        List<PopulationTreeTableNode> nodes = treeTable.getSelectedNodes();

        if (nodes.isEmpty() || nodes.size() > 1) {
            return;
        }

        // May be a dataset or cluster group selected
        IAnalysisDataset datasetToMove = datasets.isEmpty() ? null : datasets.get(0);

        // Get the node containing the dataset
        PopulationTreeTableModel model = (PopulationTreeTableModel) treeTable.getTreeTableModel();

        if (isDown) {
            model.moveNodesDown(nodes);
        } else {
            model.moveNodesUp(nodes);
        }

        if (datasetToMove != null) {
            selectDataset(datasetToMove);
        }

    }

    private synchronized void deleteSelectedDatasets() {
    	final List<IAnalysisDataset> datasets = DatasetListManager.getInstance().getSelectedDatasets();
    	final List<PopulationTreeTableNode> nodes = treeTable.getSelectedNodes();

    	// Check if cluster groups need removing
    	if (nodes.size() > datasets.size()) {
    		// cluster groups are also selected, add to list
    		for (PopulationTreeTableNode n : treeTable.getSelectedNodes()) {

    			if (n.hasClusterGroup()) {
    				IClusterGroup g = n.getGroup();
    				for (UUID childID : g.getUUIDs()) {
    					IAnalysisDataset child = DatasetListManager.getInstance().getDataset(childID);
    					datasets.add(child);
    				}

    			}
    		}
    	}

    	if (datasets.isEmpty())
    		return;


    	DatasetDeleter deleter = new DatasetDeleter(getInputSupplier());
    	deleter.deleteDatasets(datasets);
    	getDatasetEventHandler().fireDatasetEvent(DatasetEvent.CLEAR_CACHE, datasets);
    	update();  
    	treeTable.getColumnModel().getColumn(PopulationTreeTable.COLUMN_NAME).setHeaderValue("Dataset (0)");
    	treeTable.getColumnModel().getColumn(PopulationTreeTable.COLUMN_CELL_COUNT).setHeaderValue("Cells (0)");
    	getSignalChangeEventHandler().fireSignalChangeEvent(SignalChangeEvent.UPDATE_PANELS_WITH_NULL);
    }

    /**
     * Establish the rows in the population tree that are currently selected.
     * Set the possible menu options accordingly, and call the panel updates
     */
    public class TreeSelectionHandler implements TreeSelectionListener {
    	public void valueChanged(TreeSelectionEvent e) {
    		try {

    			if (!isCtrlPressed())
    				datasetSelectionOrder.clear();

    			// Track the datasets currently selected
    			TreeSelectionModel lsm = (TreeSelectionModel) e.getSource();
    			
    			// Correlate dataset index with the order it was selected in
    			Map<Integer, Integer> selectedIndexes = getSelectedIndexes(lsm);

    			DatasetListManager.getInstance().setSelectedDatasets(datasetSelectionOrder);

    			PopulationTableCellRenderer rend = new PopulationTableCellRenderer(selectedIndexes);

    			// Update the table headers
    			treeTable.getColumnModel().getColumn(PopulationTreeTable.COLUMN_COLOUR).setCellRenderer(rend);
    			treeTable.getColumnModel().getColumn(PopulationTreeTable.COLUMN_NAME).setHeaderValue(String.format("Dataset (%d)", datasetSelectionOrder.size()));
    			treeTable.getColumnModel().getColumn(PopulationTreeTable.COLUMN_CELL_COUNT).setHeaderValue(String.format("Cells (%d)", getCellTotal()));

    			final List<Object> selectedObjects = new ArrayList<>();
    			for(int i : selectedIndexes.keySet()) {
    				selectedObjects.add(treeTable.getValueAt(i, PopulationTreeTable.COLUMN_NAME));
    			}
    			populationPopup.updateSelectionContext(selectedObjects);
    			getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);

    		} catch (Exception ex) {
    			LOGGER.warning("Error in tree selection handler");
    			LOGGER.log(Loggable.STACK, "Error in tree selection handler", ex);
    		}
    	}
    	
    	private int getCellTotal() {
    		int cellTotal = 0;
    		for(IAnalysisDataset d : datasetSelectionOrder) {
    			cellTotal += d.getCollection().size();
    		}
    		return cellTotal;
    	}

    	private Map<Integer, Integer> getSelectedIndexes(TreeSelectionModel lsm){
    		Map<Integer, Integer> selectedIndexes = new HashMap<>(0);
    		List<IAnalysisDataset> datasets = new ArrayList<>(8);
    		
    		if (!lsm.isSelectionEmpty()) {
    			// Find out which indexes are selected.
    			int minIndex = lsm.getMinSelectionRow();
    			int maxIndex = lsm.getMaxSelectionRow();
    			for (int i = minIndex; i <= maxIndex; i++) {
    				if (lsm.isRowSelected(i)) {

    					if (treeTable.isDataset(i)) {

    						IAnalysisDataset d = treeTable.getDatasetAtRow(i);
    						datasets.add(d);
    						datasetSelectionOrder.add(d);

    						int selectionIndex = 0;
    						for (IAnalysisDataset an : datasetSelectionOrder) {

    							if (an == d) {
    								selectedIndexes.put(i, selectionIndex);
    								break;
    							}
    							selectionIndex++;
    						}

    					}

    				}
    			}

    			// Ctrl deselect happened - a dataset has been deselected
    			// and remains in the
    			// datasetSelectionOrder map
    			if (datasetSelectionOrder.size() > datasets.size()) {
    				// Go through tree table and check for deselected dataset
    				Iterator<IAnalysisDataset> it = datasetSelectionOrder.iterator();

    				while (it.hasNext()) {
    					IAnalysisDataset d = it.next();
    					if (!datasets.contains(d)) {
    						it.remove();
    					}
    				}

    				// Adjust the indexes of the remaining datasets
    				fixDiscontinuousPositions(selectedIndexes);

    			}
    		}
    		return selectedIndexes;
    	}

    	private Map<Integer, Integer> fixDiscontinuousPositions(Map<Integer, Integer> selectedIndexes) {
    		// Find a discontinuity in the indexes - one value is missing
    		List<Integer> values = new ArrayList<Integer>(selectedIndexes.values());
    		Collections.sort(values);

    		int prev = -1;
    		for (int i : values) {
    			if (i - prev > 1) {
    				// a value was skipped
    				for (int k : selectedIndexes.keySet()) {
    					int j = selectedIndexes.get(k);
    					if (j == i) { // this is the entry that is too high
    						selectedIndexes.put(k, j - 1); // Move index down by
    						// 1
    					}
    				}
    				fixDiscontinuousPositions(selectedIndexes); // there will
    				// now be a new
    				// discontinuity.
    				// Fix until end
    				// of list
    			}
    			prev = i;
    		}
    		return selectedIndexes;
    	}

    }

    @Override
    public void eventReceived(SignalChangeEvent event) {

        switch (event.type()) {
	        // catch any signals that affect the datasets directly
	        case SignalChangeEvent.MOVE_DATASET_DOWN_ACTION: moveDataset(true); break;
	        case SignalChangeEvent.MOVE_DATASET_UP_ACTION:  moveDataset(false); break;
	        case SignalChangeEvent.DELETE_DATASET: deleteSelectedDatasets(); break;
	        default: {
	            // Pass on events from the popup menu
	            if (event.sourceName().equals(PopulationListPopupMenu.SOURCE_COMPONENT))
	                getSignalChangeEventHandler().fireSignalChangeEvent(event.type());
	            break;
	        }
        }

    }
}
