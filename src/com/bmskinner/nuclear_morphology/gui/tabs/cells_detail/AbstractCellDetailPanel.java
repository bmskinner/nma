package com.bmskinner.nuclear_morphology.gui.tabs.cells_detail;

import com.bmskinner.nuclear_morphology.gui.tabs.CellEditingTabPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.editing.AbstractEditingPanel;

@SuppressWarnings("serial")
public abstract class AbstractCellDetailPanel 
	extends AbstractEditingPanel
	implements CellEditingTabPanel {
		
	private CellViewModel model;
	
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
	public synchronized CellViewModel getCellModel(){
		return model;
	}
	
	public synchronized void setCellModel(CellViewModel model){
		this.model = model;
	}
	
	/**
	 * Remove any charts that contain the current active cell,
	 * causing them to redraw on the next refresh
	 */
	public synchronized void clearCellCharts(){
		this.getChartCache().clear(model.getCell());
	}
		

}
