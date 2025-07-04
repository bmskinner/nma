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
package com.bmskinner.nma.analysis;

import java.util.List;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.IClusterGroup;

/**
 * An extension to the default analysis result to hold cluster groups
 * 
 * @author ben
 *
 */
public class ClusterAnalysisResult extends DefaultAnalysisResult {

    IClusterGroup group;

    public ClusterAnalysisResult(IAnalysisDataset d, IClusterGroup g) {
        super(d);
        group = g;
    }

    public ClusterAnalysisResult(List<IAnalysisDataset> list, IClusterGroup g) {
        super(list);
        group = g;
    }

    public IClusterGroup getGroup() {
        return group;
    }
}
