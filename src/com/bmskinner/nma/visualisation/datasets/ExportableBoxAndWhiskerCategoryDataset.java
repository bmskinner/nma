package com.bmskinner.nma.visualisation.datasets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

/**
 * This extension to the default box and whisker dataset stores the raw values,
 * so data can be exported directly from the chart
 * 
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class ExportableBoxAndWhiskerCategoryDataset extends DefaultBoxAndWhiskerCategoryDataset {

	/**
	 * The raw chart data, stored under the row and column keys
	 */
	private transient Map<Comparable<?>, Map<Comparable<?>, List<?>>> rawData = new HashMap<>();

	public ExportableBoxAndWhiskerCategoryDataset() {
		super();
	}

	@Override
	public void add(List list, Comparable rowKey, Comparable columnKey) {
		super.add(list, rowKey, columnKey);
		rawData.computeIfAbsent(rowKey, (k) -> new HashMap<Comparable<?>, List<?>>());
		rawData.get(rowKey).put(columnKey, list);
	}

	public List<?> getRawData(Comparable<?> rowKey, Comparable<?> columnKey) {
		if (rawData.get(rowKey) == null)
			return new ArrayList<>();

		return rawData.get(rowKey).get(columnKey);
	}

}
