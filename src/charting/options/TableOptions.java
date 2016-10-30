package charting.options;

import java.util.Set;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import charting.options.DefaultTableOptions.TableType;
import components.ICell;

public interface TableOptions extends DisplayOptions {
	
	static final int ALL_COLUMNS = 0;
	static final int FIRST_COLUMN = -1;
	static final int ALL_EXCEPT_FIRST_COLUMN = -2;

	void setType(TableType type);

	TableType getType();

	ICell getCell();

	void setCell(ICell cell);

	int hashCode();

	boolean equals(Object obj);
	
	JTable getTarget();
	
	void setTarget(JTable target);
		
	void setRenderer(int column, TableCellRenderer r);
	
	/**
	 * Get the renderer for the given column to apply to the final table model
	 * @return
	 */
	TableCellRenderer getRenderer(int i);
	
	Set<Integer> getRendererColumns();

}