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
		if(c!=cell){
			this.cell = c;
			updateViews();
		}
	}
	
	public Cell getCell(){
		return cell;
	}
	
	public boolean hasCell(){
		return cell!=null;
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
