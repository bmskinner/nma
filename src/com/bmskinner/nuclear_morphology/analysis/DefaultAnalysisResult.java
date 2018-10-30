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
package com.bmskinner.nuclear_morphology.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;

/**
 * The default implementation of IAnalysisResult, which provides access to
 * datasets produced or modified in an IAnalysisMethod
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class DefaultAnalysisResult implements IAnalysisResult {

    List<IAnalysisDataset> datasets = new ArrayList<>();

    // Store boolean options as needed
    Map<Integer, Boolean> booleans = new HashMap<>();

    public DefaultAnalysisResult(IAnalysisDataset d) {
        datasets.add(d);
    }

    public DefaultAnalysisResult(List<IAnalysisDataset> d) {
        datasets.addAll(d);
    }

    @Override
    public List<IAnalysisDataset> getDatasets() {
        return datasets;
    }

    @Override
    public IAnalysisDataset getFirstDataset() {
        return datasets.get(0);
    }

    public void setBoolean(int i, boolean b) {
        this.booleans.put(i, b);
    }

    @Override
    public boolean getBoolean(int i) {
        return booleans.get(i);
    }

}
