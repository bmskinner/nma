package charting;

import java.util.List;

import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import analysis.IAnalysisDataset;
import charting.options.ChartOptions;
import charting.options.TableOptions;
import components.ICell;
import logging.Loggable;

public interface Cache extends Loggable {

	/**
	 * Remove all cached charts
	 */
	void purge();

	/*
	 * Removes all stored entries from the cache
	 */
	void clear();

	/**
	 * Remove caches containing any of the given datasets.
	 * These will be recalculated at next call
	 * @param list
	 */
	void clear(List<IAnalysisDataset> list);
	
	/**
	 * Remove caches containing the given cell
	 * @param cell
	 */
	void clear(ICell cell);
	
	boolean has(TableOptions options);
	
	TableModel get(TableOptions options);
	
	boolean has(ChartOptions options);
	
	JFreeChart get(ChartOptions options);
	
	void add(ChartOptions options, JFreeChart chart);
	
	void add(TableOptions options, TableModel model);
	
	

}