package com.bmskinner.nuclear_morphology.visualisation.datasets;

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
public class ExportableBoxAndWhiskerCategoryDataset extends DefaultBoxAndWhiskerCategoryDataset {

    /**
     * The raw chart data, stored under the row and column keys
     */
    private Map<Comparable<?>, Map<Comparable<?>, List<?>>> rawData = new HashMap<Comparable<?>, Map<Comparable<?>, List<?>>>();

    public ExportableBoxAndWhiskerCategoryDataset() {
        super();
    }

    @Override
    public void add(List list, Comparable rowKey, Comparable columnKey) {
        super.add(list, rowKey, columnKey);
        Map<Comparable<?>, List<?>> row;
        if (rawData.get(rowKey) == null) {
            row = new HashMap<Comparable<?>, List<?>>();
            rawData.put(rowKey, row);
        }
        row = rawData.get(rowKey);
        row.put(columnKey, list);
    }

    public List getRawData(Comparable rowKey, Comparable columnKey) {
        if (rawData.get(rowKey) == null) {
            return null;
        } else {
            return rawData.get(rowKey).get(columnKey);
        }
    }

}
