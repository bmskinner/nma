package com.bmskinner.nuclear_morphology.gui.dialogs.collections;

import java.util.ArrayList;
import java.util.List;
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
	
	private transient List<Object> selected = new ArrayList<>();
	
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
	
	/**
	 * Set the selection for all elements
	 * @param b
	 */
	public void setAllSelected(boolean b) {
		for (int r = 0; r < getRowCount(); r++) 
            for (int c = 0; c <getColumnCount(); c++) 
                setSelected(r, c, b);
	}
	
	
	/**
	 * Set the selection of the given cell
	 * @param c the cell to update
	 * @param b the selection state
	 */
	public void setSelected(ICell c, boolean b) {
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
	public void toggleSelected(int r, int c) {
		Object o = getValueAt(r, c);
		setSelected(r, c, !selected.contains(o));
	}
	
	/**
	 * Set the selection state of the given cell
	 * @param r
	 * @param c
	 * @param b
	 */
	public void setSelected(int r, int c, boolean b) {

		SelectableCellIcon icon = (SelectableCellIcon)getValueAt(r, c);
		if(icon.getCell()==null) // don't select empty table rows
			return;
		
		icon.setSelected(b);
		if(b)
			selected.add(icon);
		else
			selected.remove(icon);
	}
				
	/**
	 * Get all selected cells
	 * @return
	 */
	public List<ICell> getSelected(){
		return selected.stream().map(o -> ((SelectableCellIcon)o).getCell()).collect(Collectors.toList());
	}
	
	

}
