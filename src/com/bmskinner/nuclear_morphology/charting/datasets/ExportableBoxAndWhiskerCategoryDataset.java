/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.charting.datasets;

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
