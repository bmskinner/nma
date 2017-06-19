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


package com.bmskinner.nuclear_morphology.charting.options;

import java.util.List;
import java.util.UUID;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import com.bmskinner.nuclear_morphology.analysis.signals.ShellCounter.CountType;
import com.bmskinner.nuclear_morphology.charting.options.DefaultTableOptions.TableType;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter.ColourSwatch;

/**
 * Builder for a TableOptions object. This simplifies the creation of the
 * options when not all parameters need to be set.
 * 
 * @author bms41
 *
 */
public class TableOptionsBuilder {

    private DefaultTableOptions options;

    public TableOptionsBuilder() {
        options = new DefaultTableOptions(null);
    }

    public TableOptionsBuilder setDatasets(List<IAnalysisDataset> list) {
        options.setDatasets(list);
        return this;
    }

    public TableOptionsBuilder setType(TableType type) {
        options.setType(type);
        return this;
    }

    public TableOptionsBuilder addStatistic(PlottableStatistic s) {
        options.addStat(s);
        return this;
    }

    public TableOptionsBuilder setSegID(UUID segID) {
        options.setSegID(segID);
        return this;
    }

    public TableOptionsBuilder setSegPosition(int segPosition) {
        options.setSegPosition(segPosition);
        return this;
    }

    public TableOptionsBuilder setScale(MeasurementScale s) {
        options.setScale(s);
        return this;
    }

    public TableOptionsBuilder setCell(ICell cell) {
        options.setCell(cell);
        return this;
    }

    public TableOptionsBuilder setSwatch(ColourSwatch swatch) {
        options.setSwatch(swatch);
        return this;
    }

    public TableOptionsBuilder setTarget(JTable target) {
        options.setTarget(target);
        return this;
    }

    /**
     * Set the table renderer to use
     * 
     * @param column
     *            the column to apply the renderer to
     * @param r
     *            the renderer
     * @return
     */
    public TableOptionsBuilder setRenderer(int column, TableCellRenderer r) {
        options.setRenderer(column, r);
        return this;
    }

    public TableOptionsBuilder setCountType(CountType type) {
        options.setCountType(type);
        return this;
    }
    
    public TableOptionsBuilder setNormalised(boolean b){
    	options.setNormalised(b);
    	return this;
    }

    public TableOptions build() {
        return options;

    }
}
