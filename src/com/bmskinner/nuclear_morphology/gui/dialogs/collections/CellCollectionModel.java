package com.bmskinner.nuclear_morphology.gui.dialogs.collections;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.table.DefaultTableModel;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.datasets.VirtualDataset;
import com.bmskinner.nuclear_morphology.gui.components.SelectableCellIcon;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Track cell selections and allow for creation of new child collections
 * based on selected cells
 * @author ben
 * @since 1.19.2
 *
 */
public class CellCollectionModel extends DefaultTableModel {
	
	private static final Logger LOGGER = Logger.getLogger(CellCollectionModel.class.getName());
	
	/** Default number of columns in the table model */
	public static final int COLUMN_COUNT = 3;
	
	/** Track which cells have been highlighted */
	private Set<UUID> selectedCellIds = new HashSet<>();
	private IAnalysisDataset dataset;
	
	/**
	 * Create a new model based on the given dataset
	 * @param d the dataset to select cells from
	 */
	public CellCollectionModel(IAnalysisDataset d) {
		super();
		
		this.dataset = d;
		int cellCount = d.getCollection().size();

        int remainder = cellCount % COLUMN_COUNT == 0 ? 0 : 1;

        int rows = cellCount / COLUMN_COUNT + remainder;
		
		setRowCount(rows);
		setColumnCount(COLUMN_COUNT);

		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < COLUMN_COUNT; col++) {
				setValueAt(new SelectableCellIcon(), row, col);
			}
		}
	}
	
	
	@Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
	
	/**
	 * Get the cell at the given position in the table
	 * @param r the row
	 * @param c the column
	 * @return the cell at this position
	 */
	public ICell getCell(int r, int c) {
		return ((SelectableCellIcon)getValueAt(r, c)).getCell();
	}
		
	/**
	 * Get the number of currently selected cells
	 * @return
	 */
	public synchronized int selectedCount() {
		return selectedCellIds.size();
	}
	
	/**
	 * Set the selection for all elements
	 * @param b
	 */
	public synchronized void setAllSelected(boolean b) {
		selectedCellIds.addAll(dataset.getCollection().getCellIDs());
		for(int row = 0; row<getRowCount(); row++) {
			for (int col = 0; col < COLUMN_COUNT; col++) {
				SelectableCellIcon icon = (SelectableCellIcon)getValueAt(row, col);
				icon.setSelected(true);
			}
		}
	}
	
	
	/**
	 * Set the selection of the given cell
	 * @param c the cell to update
	 * @param b the selection state
	 */
	private synchronized void setSelected(ICell c, boolean b) {
		if(b) {
			selectedCellIds.add(c.getId());
		} else {
			selectedCellIds.remove(c.getId());
		}
	}
		
	/**
	 * Invert the selection of the given cell
	 * @param r the row containing the cell
	 * @param c the column containing the cell
	 */
	public synchronized void toggleSelected(int r, int c) {
		SelectableCellIcon icon = (SelectableCellIcon)getValueAt(r, c);
		icon.setSelected(!icon.isSelected());
		setSelected(icon.getCell(), !selectedCellIds.contains(icon.getCell().getId()));
	}
	
	/**
	 * Set the selection state of the given cell
	 * @param r the row
	 * @param c the column
	 * @param b the desired selection state
	 */
	public synchronized void setSelected(int r, int c, boolean b) {

		SelectableCellIcon icon = (SelectableCellIcon)getValueAt(r, c);
		if(icon.getCell()==null) // don't select empty table rows
			return;		
		icon.setSelected(b);
		setSelected(icon.getCell(), b);
	}
				
	/**
	 * Get all selected cells
	 * @return
	 */
	public List<ICell> getSelected(){
		LOGGER.fine("There are "+selectedCellIds.size()+" selected cells in curation model");
		return dataset.getCollection().getCells().stream()
				.filter(c->selectedCellIds.contains(c.getId()))
				.collect(Collectors.toList());
	}
	
	/**
	 * Create a new child collection from the currently selected cells
	 * @return the created dataset, or an empty object if no cells were selected
	 */
	public Optional<IAnalysisDataset> makeNewCollectionFromSelected() {
		LOGGER.fine("Creating new collection from selected cells");
		List<ICell> cells = getSelected();

		if(cells.isEmpty()) {
			return Optional.empty();
		}

		ICellCollection newCollection = new VirtualDataset(dataset, dataset.getName() + "_Curated");
		for (ICell c : cells) {
			if(c==null)
				LOGGER.fine("Null cell encountered!");
			else
				newCollection.addCell(c);
		}

		LOGGER.info("Added " + cells.size() + " cells to new collection");

		dataset.addChildCollection(newCollection);

		IAnalysisDataset newDataset = dataset.getChildDataset(newCollection.getId());

		try {
			newCollection.createProfileCollection();
			dataset.getCollection().getProfileManager().copyCollectionOffsets(newCollection);
		} catch (ProfileException e) {
			LOGGER.log(Level.WARNING, "Unable to copy profiles to new child collection");
			LOGGER.log(Loggable.STACK, "Error copying profiles to new child collection", e);
			return Optional.empty();
		}

		return Optional.of(newDataset);       
	}
}
