package gui.tabs.cells;

import gui.tabs.editing.AbstractEditingPanel;

@SuppressWarnings("serial")
public abstract class AbstractCellDetailPanel extends AbstractEditingPanel {
		
	private final CellViewModel model;
	
	public AbstractCellDetailPanel(final CellViewModel model){
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
	
	/**
	 * Remove any charts that contain the current active cell,
	 * causing them to redraw on the next refresh
	 */
	public void clearCellCharts(){
		this.getChartCache().clear(model.getCell());
	}
		

}
