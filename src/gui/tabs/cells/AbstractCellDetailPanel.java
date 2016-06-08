package gui.tabs.cells;

import components.Cell;
import gui.tabs.CellDetailPanel;
import gui.tabs.DetailPanel;

public abstract class AbstractCellDetailPanel extends DetailPanel {
		
	protected CellDetailPanel parent;
	protected Cell activeCell;
	
	public AbstractCellDetailPanel(){
		super();
	}
	
	public void update(Cell cell){
		this.activeCell = cell;
	}
	
	public void setParent(CellDetailPanel p){
		this.parent = p;
	}

}
