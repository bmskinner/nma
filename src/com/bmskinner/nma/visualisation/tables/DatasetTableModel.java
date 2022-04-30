package com.bmskinner.nma.visualisation.tables;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

public abstract class DatasetTableModel extends AbstractTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5043678811874502686L;
	protected static final String EMPTY_STRING = "";
	protected static final String DEFAULT_DECIMAL_FORMAT = "#0.00";

	protected static final DecimalFormat df = new DecimalFormat(DEFAULT_DECIMAL_FORMAT);

	/**
	 * Create an empty table model
	 */
	protected abstract void makeEmptyTable();

	/**
	 * Create column names for the given datasets, with an empty column at the start
	 * to hold row names
	 * 
	 * @param datasets
	 * @return
	 */
	protected String[] makeColNames(@NonNull List<IAnalysisDataset> datasets) {
		List<String> names = new ArrayList<>();
		names.add(EMPTY_STRING);
		names.addAll(datasets.stream().map(IAnalysisDataset::getName).toList());
		return names.toArray(new String[0]);
	}
}
