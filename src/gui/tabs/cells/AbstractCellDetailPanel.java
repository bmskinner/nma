package gui.tabs.cells;

import components.Cell;
import components.CellularComponent;
import gui.tabs.CellDetailPanel;
import gui.tabs.DetailPanel;

@SuppressWarnings("serial")
public abstract class AbstractCellDetailPanel extends DetailPanel {
		
	protected CellDetailPanel parent;
	protected Cell activeCell;
	protected CellularComponent activeComponent;
	
	public AbstractCellDetailPanel(){
		super();
	}
	
	public void update(Cell cell){
		this.activeCell = cell;
	}
	
	public void setParent(CellDetailPanel p){
		this.parent = p;
	}
	
	public void setActiveComponent(CellularComponent activeComponent){
		this.activeComponent = activeComponent;
	}
	
	public CellularComponent getActiveComponent(){
		return this.activeComponent;
	}
	
	public Cell getActiveCell(){
		return this.activeCell;
	}

}
