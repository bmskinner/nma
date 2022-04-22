/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.visualisation.options;

import java.util.List;
import java.util.UUID;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult.Aggregation;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult.Normalisation;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult.ShrinkType;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
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
		options.setScale(GlobalOptions.getInstance().getScale());
	}

	public TableOptionsBuilder setDatasets(@Nullable List<IAnalysisDataset> list) {
		options.setDatasets(list);
		return this;
	}

	public TableOptionsBuilder addStatistic(Measurement s) {
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
	 * @param column the column to apply the renderer to
	 * @param r      the renderer
	 * @return
	 */
	public TableOptionsBuilder setColumnRenderer(int column, TableCellRenderer r) {
		options.setRenderer(column, r);
		return this;
	}

	public TableOptionsBuilder setAggregation(Aggregation c) {
		options.setAggregation(c);
		return this;
	}

	public TableOptionsBuilder setNormalisation(Normalisation c) {
		options.setNormalisation(c);
		return this;
	}

	public TableOptionsBuilder setNormalised(boolean b) {
		options.setNormalised(b);
		return this;
	}

	public TableOptionsBuilder setShrinkType(ShrinkType t) {
		options.setShrinkType(t);
		return this;
	}

	public TableOptions build() {
		return options;

	}
}
