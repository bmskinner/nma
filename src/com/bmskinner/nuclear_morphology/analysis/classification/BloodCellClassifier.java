/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.analysis.classification;

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.bmskinner.nuclear_morphology.analysis.image.ColourMeasurometer;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Attempt to classify the cells from an H&E stained blood smear.
 * 
 * @author bms41
 * @since 1.13.5
 *
 */
public class BloodCellClassifier implements CellClassifier, Loggable {

    public static final String ERYTHROCYTE = "Erythrocyte";
    public static final String NEUTROPHIL  = "Neutrophil";
    public static final String BASOPHIL    = "Basophil";
    public static final String EOSINOPHIL  = "Eosinophil";
    public static final String LYMPHOCYTE  = "Lymphocyte";

    private final IAnalysisDataset dataset;

    private Map<String, Set<ICell>> groups = new HashMap<String, Set<ICell>>();

    /**
     * Construct with a dataset to be classified
     * 
     * @param dataset
     */
    public BloodCellClassifier(IAnalysisDataset dataset) {
        this.dataset = dataset;
    }

    @Override
    public void classify() {

        Set<ICell> eryth = new HashSet<>();
        Set<ICell> neutro = new HashSet<>();
        Set<ICell> lympho = new HashSet<>();

        // Go through the difference blood cell types we can look for

        for (ICell c : dataset.getCollection().getCells()) {

            if (!c.hasNucleus()) {
                eryth.add(c);
                continue;
            }

            if (c.getStatistic(PlottableStatistic.CELL_NUCLEUS_COUNT) == 1
                    || c.getStatistic(PlottableStatistic.LOBE_COUNT) == 1) {
                lympho.add(c);
                continue;
            }

            // Now look for the differences between granulocytes
            // Pass RGB data to a clusterer and group on cytoplasm intensity
            // Requires user confirmation of cells

            ColourMeasurometer cm = new ColourMeasurometer();
            try {

                Color colour = cm.calculateAverageRGB(c, CellularComponent.CYTOPLASM);

            } catch (UnloadableImageException e) {
                stack(e);
            }

        }

    }

    @Override
    public Set<String> getGroups() {
        return groups.keySet();
    }

    @Override
    public Set<ICell> getCells(String group) {
        return groups.get(group);
    }

}
