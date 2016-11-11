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
	
	public ComponentListCell(final String name, final CellularComponent c){
		if(name == null){
			throw new IllegalArgumentException("Name cannot be null in component list cell");
		}
		
		if(c == null){
			throw new IllegalArgumentException("Component cannot be null in component list cell");
		}
		
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
