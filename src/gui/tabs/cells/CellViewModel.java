package gui.tabs.cells;


import java.util.ArrayList;
import java.util.List;

import components.Cell;
import components.CellularComponent;

public class CellViewModel {
	
	private Cell cell = null;
	private CellularComponent component = null;
	
	List<AbstractCellDetailPanel> views = new ArrayList<AbstractCellDetailPanel>();
	
	public CellViewModel(Cell cell, CellularComponent component){
		this.cell = cell;
		this.component = component;
	}
	
	public void setCell(Cell c){
		if(c==null || c!=cell){
			this.cell = c;
			component = null; // component cannot be carried over
			updateViews();
		}
	}
	
	/**
	 * Swap a cell with a new version of the same cell. Used in 
	 * the resegmentation dialog to update the active cell without
	 * triggering a view update before the dialog closes.
	 * @param c the cell. Must have the same ID as the existing cell.
	 */
	public void swapCell(Cell c){
		if(c==null || c.getId().equals(cell.getId())){
			this.cell = c;
			component = null; // component cannot be carried over
			clearChartCache();
		}
	}
	
	public Cell getCell(){
		return cell;
	}
	
	public boolean hasCell(){
		return cell!=null;
	}
	
	/**
	 * Cause all charts with the current active cell
	 * to be redrawn
	 */
	public void clearChartCache(){
		for(AbstractCellDetailPanel d : views){
			d.clearCellCharts();
		}
		updateViews();
		
	}
	
	public void updateComponent(){
		
	}
	
	public void setComponent(CellularComponent component){
		if(this.component!=component){
			this.component = component;
			updateViews();
		}
	}
	
	public CellularComponent getComponent(){
		return this.component;
	}
	
	public void updateViews(){
		for(AbstractCellDetailPanel d : views){
			d.update();
		}
	}
	
	public void addView(AbstractCellDetailPanel d){
		this.views.add(d);
	}
		
	

}
