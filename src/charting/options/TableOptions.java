package charting.options;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

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
	
	void setRenderer(TableCellRenderer r);
	
	/**
	 * Get the renderer to apply to the final table model
	 * @return
	 */
	TableCellRenderer getRenderer();

}