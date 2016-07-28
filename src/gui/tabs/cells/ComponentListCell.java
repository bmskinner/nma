package gui.tabs.cells;

import components.CellularComponent;

/**
 * This holds CellularComponents for the ComponentListPanel
 * @author bms41
 *
 */
public class ComponentListCell {
	
	private String name;
	private CellularComponent component;
	
	public ComponentListCell(String name, CellularComponent c){
		this.name = name;
		this.component = c;
	}
	
	public String getName(){
		return name;
	}
	
	public String toString(){
		return name;
	}
	
	public CellularComponent getComponent(){
		return component;
	}

}
