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
package com.bmskinner.nuclear_morphology.charting.datasets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.ViolinPlots.ViolinCategoryDataset;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclear.Colocalisation;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;

public class SignalViolinDatasetCreator extends ViolinDatasetCreator {

    public SignalViolinDatasetCreator(@NonNull final ChartOptions options) {
        super(options);
    }
    
    public synchronized ViolinCategoryDataset createSignalCountViolinDataset() throws ChartDatasetCreationException {
    	return new ViolinDatasetCreator(options).createPlottableStatisticViolinDataset(CellularComponent.NUCLEAR_SIGNAL);
    	
    }

    public synchronized ViolinCategoryDataset createSignalColocalisationViolinDataset() throws ChartDatasetCreationException {
        if (options.isSingleDataset())
            return createSingleSignalColocalisationViolinDataset();
        if (options.isMultipleDatasets())
            return createMultipleSignalColocalisationViolinDataset();
        return new ViolinCategoryDataset();

    }

    /**
     * Create a signal colocalisation dataset for signals in a single analysis
     * dataset
     * 
     * @return
     * @throws ChartDatasetCreationException
     */
    private synchronized ViolinCategoryDataset createSingleSignalColocalisationViolinDataset() throws ChartDatasetCreationException {

        ViolinCategoryDataset ds = new ViolinCategoryDataset();

        if (!options.isSingleDataset())
            return ds;
        
        IAnalysisDataset dataset = options.firstDataset();

		MeasurementScale scale = options.getScale();
		ICellCollection c = dataset.getCollection();

		Set<UUID> signalGroups = c.getSignalGroupIDs();

		Set<UUID> done = new HashSet<UUID>();

		for (UUID id1 : signalGroups) {

		    String rowKey = c.getSignalGroup(id1).get().getGroupName();

		    for (UUID id2 : signalGroups) {

		        if (id1.equals(id2)) 
		            continue;

		        if (done.contains(id2))
		            continue;

		        String colKey = c.getSignalGroup(id2).get().getGroupName() + " vs " + rowKey;

		        // Find the colocalising signal pairs
		        List<Colocalisation<INuclearSignal>> coloc = c.getSignalManager().getColocalisingSignals(id1, id2);

		        if (coloc.isEmpty())
		            continue;

		        List<Number> list = new ArrayList<Number>();
		        for (Colocalisation<INuclearSignal> lc : coloc) {
		            list.add(lc.getDistance(scale));
		        }

		        ds.add(list, "Default", colKey); // Don't use a rowKey - it  will cause an empty box space
//		        addProbabilities(ds, list, "Default", colKey);

		    }

		    done.add(id1);

		}

        return ds;
    }

    /**
     * Create a signal colocalisation dataset for signals in multiple analysis
     * datasets
     * 
     * @return
     * @throws ChartDatasetCreationException
     */
    private synchronized ViolinCategoryDataset createMultipleSignalColocalisationViolinDataset()
            throws ChartDatasetCreationException {

        ViolinCategoryDataset ds = new ViolinCategoryDataset();

        if (!options.isMultipleDatasets()) {
            return ds;
        }

        for (IAnalysisDataset dataset : options.getDatasets()) {

		    MeasurementScale scale = options.getScale();
		    ICellCollection c = dataset.getCollection();

		    Set<UUID> signalGroups = c.getSignalGroupIDs();

		    Set<UUID> done = new HashSet<UUID>();

		    for (UUID id1 : signalGroups) {

		        String rowKey = c.getSignalGroup(id1).get().getGroupName();

		        for (UUID id2 : signalGroups) {

		            if (id1.equals(id2)) {
		                continue;
		            }

		            if (done.contains(id2)) {
		                continue;
		            }

		            String colKey = dataset.getName() + ": " + c.getSignalGroup(id2).get().getGroupName() + " vs "
		                    + rowKey;

		            // Find the colocalising signal pairs
		            List<Colocalisation<INuclearSignal>> coloc = c.getSignalManager().getColocalisingSignals(id1,
		                    id2);

		            if (coloc.isEmpty()) {
		                continue;
		            }

		            List<Number> list = new ArrayList<Number>();
		            for (Colocalisation<INuclearSignal> lc : coloc) {
		                list.add(lc.getDistance(scale));
		            }

		            ds.add(list, "Default", colKey);
//		            addProbabilities(ds, list, "Default", colKey);

		        }

		        done.add(id1);

		    }

		}

        return ds;
    }
}
