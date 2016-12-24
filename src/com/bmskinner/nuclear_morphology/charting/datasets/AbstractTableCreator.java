package com.bmskinner.nuclear_morphology.charting.datasets;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.gui.Labels;

/**
 * Abstract class for making tables
 * @author ben
 * @since 1.13.4
 *
 */
public abstract class AbstractTableCreator extends AbstractDatasetCreator<TableOptions> {
	
	/**
	 * Create with a set of table options
	 */
	public AbstractTableCreator(final TableOptions o){
		super(o);
	}
	
	/**
	 * Create an empty table declaring no data is loaded
	 * @return
	 */
	public static TableModel createBlankTable(){
		DefaultTableModel model = new DefaultTableModel();
		model.addColumn(Labels.NO_DATA_LOADED);
		return model;
	}
	
	/**
	 * Create an empty table declaring no data is loaded
	 * @return
	 */
	public static TableModel createLoadingTable(){
		DefaultTableModel model = new DefaultTableModel();
		model.addColumn(Labels.LOADING_DATA);
		return model;
	}

}
