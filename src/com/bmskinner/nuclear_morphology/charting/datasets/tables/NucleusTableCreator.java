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


package com.bmskinner.nuclear_morphology.charting.datasets.tables;

import java.util.List;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.charting.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions;
import com.bmskinner.nuclear_morphology.components.options.IHoughDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

/**
 * This is for tables about nuclei at the dataset level
 * 
 * @author bms41
 * @since 1.13.4
 *
 */
public class NucleusTableCreator extends AbstractTableCreator {

    public NucleusTableCreator(final TableOptions options) {
        super(options);
    }

    /**
     * Create a table model for the Hough detection options for lobes
     * 
     * @return
     */
    public TableModel createLobeDetectionOptionsTable() {

        if (!options.hasDatasets()) {
            return AnalysisDatasetTableCreator.createBlankTable();
        }

        DefaultTableModel model = new DefaultTableModel();

        Vector<Object> rowNames = new Vector<Object>();

        // Make the row names for the options
        IDetectionSubOptions op = null;
        try {

            if (options.firstDataset().hasAnalysisOptions()) {
                IAnalysisOptions an = options.firstDataset().getAnalysisOptions();

                IDetectionOptions ido = an.getDetectionOptions(CellularComponent.NUCLEUS);

                if (ido.hasSubOptions(IDetectionSubOptions.HOUGH_OPTIONS)) {
                    op = ido.getSubOptions(IDetectionSubOptions.HOUGH_OPTIONS);
                }
            }

            if (op == null) {
                return createBlankTable();
            }

            rowNames.addAll(op.getKeys());

            model.addColumn("Option", rowNames);

            List<IAnalysisDataset> datasets = options.getDatasets();

            for (int i = 0; i < datasets.size(); i++) {

                IAnalysisDataset d = datasets.get(i);
                Vector<Object> values = new Vector<Object>();

                if (!d.hasAnalysisOptions()) {
                    return createBlankTable();
                }

                IHoughDetectionOptions hough = (IHoughDetectionOptions) d.getAnalysisOptions()
                        .getDetectionOptions(CellularComponent.NUCLEUS)
                        .getSubOptions(IDetectionSubOptions.HOUGH_OPTIONS);

                if (hough == null) {
                    return createBlankTable();
                }

                for (String s : op.getKeys()) {
                    values.add(hough.getValue(s));
                }

                model.addColumn(d.getName(), values);
            }

        } catch (MissingOptionException e) {
            fine("Missing detection options in dataset");
            stack(e.getMessage(), e);
            return createBlankTable();
        }
        return model;
    }

}
