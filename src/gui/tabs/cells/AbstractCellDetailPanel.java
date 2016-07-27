package gui.tabs.cells;

import gui.tabs.DetailPanel;

@SuppressWarnings("serial")
public abstract class AbstractCellDetailPanel extends DetailPanel {
		
	private   CellViewModel   model;
	
	public AbstractCellDetailPanel(CellViewModel model){
		super();
		this.model = model;
	}
	
	public abstract void update();
	
//	public void update(Cell cell){
//		model.setCell(cell);
//	}
		
	public CellViewModel getCellModel(){
		return model;
	}
		

}
