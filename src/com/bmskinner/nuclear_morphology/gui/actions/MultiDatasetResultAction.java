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


package com.bmskinner.nuclear_morphology.gui.actions;

import java.util.List;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.MainWindow;

/**
 * An action class that allows multiple datasets to be operated on
 * simultaneously
 * 
 * @author bms41
 * @since 1.13.6
 *
 */
public abstract class MultiDatasetResultAction extends VoidResultAction {

    protected final List<IAnalysisDataset> datasets;

    public MultiDatasetResultAction(final List<IAnalysisDataset> datasets, final String barMessage,
            final MainWindow mw) {
        super(barMessage, mw);
        if (datasets == null) {
            throw new IllegalArgumentException("Cannot have null dataset list");
        }
        this.datasets = datasets;
    }

}
