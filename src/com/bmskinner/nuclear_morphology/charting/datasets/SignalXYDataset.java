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
import java.util.Map;
import java.util.UUID;

import org.jfree.data.xy.DefaultXYDataset;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;

public class SignalXYDataset extends DefaultXYDataset {

    private Map<Comparable, ISignalGroup>     groupNames;
    private Map<Comparable, UUID>             groupIds;
    private Map<Comparable, IAnalysisDataset> datasets;

    public SignalXYDataset() {
        super();
        datasets = new HashMap<Comparable, IAnalysisDataset>();
        groupNames = new HashMap<Comparable, ISignalGroup>();
        groupIds = new HashMap<Comparable, UUID>();
    }

    public void addDataset(IAnalysisDataset group, Comparable seriesKey) {
        datasets.put(seriesKey, group);
    }

    public IAnalysisDataset getDataset(Comparable seriesKey) {
        return datasets.get(seriesKey);
    }

    public void addSignalGroup(ISignalGroup group, Comparable seriesKey) {
        groupNames.put(seriesKey, group);
    }

    public ISignalGroup getSignalGroup(Comparable seriesKey) {
        return groupNames.get(seriesKey);
    }

    public void addSignalId(UUID group, Comparable seriesKey) {
        groupIds.put(seriesKey, group);
    }

    public UUID getSignalId(Comparable seriesKey) {
        return groupIds.get(seriesKey);
    }

}
