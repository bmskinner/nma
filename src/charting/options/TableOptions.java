package charting.options;

import javax.swing.JTable;

import charting.options.DefaultTableOptions.TableType;
import components.ICell;

public interface TableOptions extends DisplayOptions {

	void setType(TableType type);

	TableType getType();

	ICell getCell();

	void setCell(ICell cell);

	int hashCode();

	boolean equals(Object obj);
	
	JTable getTarget();
	
	void setTarget(JTable target);

}