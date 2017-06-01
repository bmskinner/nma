package com.bmskinner.nuclear_morphology.charting.options;

import java.util.Set;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import com.bmskinner.nuclear_morphology.charting.options.DefaultTableOptions.TableType;

/**
 * This interface describes the values that should be checkable by table dataset
 * creators. Implementing classes must provide sensible defaults.
 * 
 * @author bms41
 *
 */
public interface TableOptions extends DisplayOptions {

    /**
     * A renderer is applied to all columns in a table
     */
    static final int ALL_COLUMNS = 0;

    /**
     * A renderer is applied to only the first column in a table
     */
    static final int FIRST_COLUMN = -1;

    /**
     * A renderer is applied to all columns in a table apart from the first
     * column
     */
    static final int ALL_EXCEPT_FIRST_COLUMN = -2;

    /**
     * Get the table type to be drawn
     * 
     * @return
     */
    TableType getType();

    int hashCode();

    boolean equals(Object obj);

    /**
     * Get the table the resulting model should be loaded into. Used by the
     * TableFactoryWorker in a DetailPanel
     * 
     * @return
     */
    JTable getTarget();

    /**
     * Check if a target has been set for the table model created from this
     * options
     * 
     * @return
     */
    boolean hasTarget();

    /**
     * Get the renderer for the given column to apply to the final table model
     * 
     * @return
     */
    TableCellRenderer getRenderer(int i);

    /**
     * Get the columns for which renderers have been set
     * 
     * @return
     */
    Set<Integer> getRendererColumns();

}