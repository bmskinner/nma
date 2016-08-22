package gui.tabs.cells;

import gui.tabs.editing.AbstractEditingPanel;

@SuppressWarnings("serial")
public abstract class AbstractCellDetailPanel extends AbstractEditingPanel {
		
	private   CellViewModel   model;
	
	public AbstractCellDetailPanel(CellViewModel model){
		super();
		this.model = model;
	}
	
	/**
	 * Update the charts and tables for the current cell
	 * and component
	 */
	public abstract void update();
	
		
	/**
	 * Get the current cell view 
	 * @return
	 */
	public CellViewModel getCellModel(){
		return model;
	}
		

}
