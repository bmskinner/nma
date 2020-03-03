package com.bmskinner.nuclear_morphology.gui.dialogs.collections;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.table.DefaultTableModel;

import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.gui.components.SelectableCellIcon;

/**
 * Display cell images in a collection. Tracks cell selections.  
 * @author bms41
 * @since 1.15.0
 *
 */
public class CellCollectionOverviewModel extends DefaultTableModel {
	
	private static final Logger LOGGER = Logger.getLogger(CellCollectionOverviewModel.class.getName());
	
	/** Track which cells have been highlighted */
	private transient Set<Key> selected = new HashSet<>();
	
	/**
	 * Combine row and column to track selected cells 
	 * @author ben
	 *
	 */
	private final class Key {
		int r, c;
		public Key(int r, int c) {
			this.r = r;
			this.c = c;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + c;
			result = prime * result + r;
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Key other = (Key) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (c != other.c)
				return false;
			if (r != other.r)
				return false;
			return true;
		}
		private CellCollectionOverviewModel getOuterType() {
			return CellCollectionOverviewModel.this;
		}
		
		
	}
	
	/**
	 * Create a model with a desired number of rows and columns
	 * @param rows
	 * @param cols
	 */
	public CellCollectionOverviewModel(int rows, int cols) {
		super();
		setRowCount(rows);
		setColumnCount(cols);

		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				setValueAt(new SelectableCellIcon(), row, col);
			}
		}
	}
		
	@Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
	
	public ICell getCell(int r, int c) {
		return ((SelectableCellIcon)getValueAt(r, c)).getCell();
	}
	
	public synchronized int selectedCount() {
		return selected.size();
	}
	
	/**
	 * Set the selection for all elements
	 * @param b
	 */
	public synchronized void setAllSelected(boolean b) {
		for (int r = 0; r < getRowCount(); r++) 
            for (int c = 0; c <getColumnCount(); c++) 
                setSelected(r, c, b);
	}
	
	
	/**
	 * Set the selection of the given cell
	 * @param c the cell to update
	 * @param b the selection state
	 */
	public synchronized void setSelected(ICell c, boolean b) {
		for (int row = 0; row < getRowCount(); row++) {
			for (int col = 0; col < getColumnCount(); col++) {
				if(getValueAt(row, col)==c) {
					setSelected(row, col, b);
					return;
				}
			}
		}
	}
	
	/**
	 * Invert the selection of the given cell
	 * @param r
	 * @param c
	 */
	public synchronized void toggleSelected(int r, int c) {
		Object o = getValueAt(r, c);
		setSelected(r, c, !selected.contains(new Key(r, c)));
	}
	
	/**
	 * Set the selection state of the given cell
	 * @param r
	 * @param c
	 * @param b
	 */
	public synchronized void setSelected(int r, int c, boolean b) {

		SelectableCellIcon icon = (SelectableCellIcon)getValueAt(r, c);
		if(icon.getCell()==null) // don't select empty table rows
			return;
		
		icon.setSelected(b);
		
		Key k = new Key(r,c);
		if(b)
			selected.add(k);
		else
			selected.remove(k);
	}
				
	/**
	 * Get all selected cells
	 * @return
	 */
	public List<ICell> getSelected(){
		LOGGER.fine("There are "+selected.size()+" selected keys in curation model");
		return selected.stream()
				.map(k -> getCell(k.r, k.c))
				.collect(Collectors.toList());
	}
	
	

}
