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
package com.bmskinner.nuclear_morphology.charting.datasets.tables;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.signals.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult;
import com.bmskinner.nuclear_morphology.components.signals.SignalManager;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * Generate tables to go with scatter charts
 * @author Ben Skinner
 *
 */
public class ScatterTableDatasetCreator extends AbstractTableCreator {
	
	private static final Logger LOGGER = Logger.getLogger(ScatterTableDatasetCreator.class.getName());
	
    public ScatterTableDatasetCreator(@NonNull final TableOptions options) {
        super(options);
    }

    /**
     * Create a table model for the Spearman's Rank correlation coefficients
     * between the selected statisics
     * 
     * @return
     */
    public TableModel createSpearmanCorrlationTable(String component) {

        if (!options.hasDatasets()) {
            return createBlankTable();
        }

        if (options.getStats().size() != 2) {
            return createBlankTable();
        }

        Measurement firstStat = options.getStat();

        for (Measurement stat : options.getStats()) {
            if (!stat.getClass().equals(firstStat.getClass())) {
                LOGGER.fine("Statistic classes are different");
                return createBlankTable();
            }
        }

        if (CellularComponent.NUCLEUS.equals(component)) {
            return createNucleusSpearmanCorrlationTable();
        }

        if (CellularComponent.NUCLEAR_SIGNAL.equals(component)) {
            return createSignalSpearmanCorrlationTable();
        }

        return createBlankTable();
    }

    /*
     * 
     * PRIVATE METHODS
     * 
     * 
     */

    private TableModel createNucleusSpearmanCorrlationTable() {

        if (!options.hasDatasets()) {
            return createBlankTable();
        }

        if (options.getStats().size() != 2) {
            return createBlankTable();
        }

        DefaultTableModel model = new DefaultTableModel();

        Vector<Object> names = new Vector<>();
        Vector<Object> rho = new Vector<>();

        List<IAnalysisDataset> datasets = options.getDatasets();

        List<Measurement> stats = options.getStats();

        MeasurementScale scale = options.getScale();

        Measurement statA = stats.get(0);
        Measurement statB = stats.get(1);
        DecimalFormat df = new DecimalFormat(DEFAULT_DECIMAL_FORMAT);
        for (int i = 0; i < datasets.size(); i++) {

            ICellCollection c = datasets.get(i).getCollection();

            // draw the segment itself
            double[] xpoints = new double[c.getNucleusCount()];
            double[] ypoints = new double[c.getNucleusCount()];

            int j = 0;

            for (Nucleus n : c.getNuclei()) {

                double statAValue;
                double statBValue;

                try {

                    if (statA.equals(Measurement.VARIABILITY)) {
                        statAValue = c.getNormalisedDifferenceToMedian(Landmark.REFERENCE_POINT, n);
                    } else {
                        statAValue = n.getMeasurement(statA, scale);
                    }

                    if (statB.equals(Measurement.VARIABILITY)) {
                        statBValue = c.getNormalisedDifferenceToMedian(Landmark.REFERENCE_POINT, n);
                    } else {
                        statBValue = n.getMeasurement(statB, scale);
                    }
                } catch (MissingLandmarkException e) {
                    LOGGER.warning("Cannot get stats for cell");
                    LOGGER.log(Loggable.STACK, "Tag RP not present in cell", e);
                    statAValue = 0;
                    statBValue = 0;
                }

                xpoints[j] = statAValue;
                ypoints[j] = statBValue;
                j++;
            }
            // }
            names.add(c.getName());

            double rhoValue = Stats.getSpearmansCorrelation(xpoints, ypoints);
            rho.add(df.format(rhoValue));
        }

        model.addColumn(Labels.DATASET, names);
        model.addColumn(Labels.Stats.SPEARMANS_RHO, rho);
        return model;

    }

    private TableModel createSignalSpearmanCorrlationTable() {

        DefaultTableModel model = new DefaultTableModel();

        Vector<Object> names = new Vector<>();
        Vector<Object> rho = new Vector<>();

        List<IAnalysisDataset> datasets = options.getDatasets();

        List<Measurement> stats = options.getStats();

        MeasurementScale scale = options.getScale();

        Measurement statA = stats.get(0);
        Measurement statB = stats.get(1);
        DecimalFormat df = new DecimalFormat(DEFAULT_DECIMAL_FORMAT);
        for (int i = 0; i < datasets.size(); i++) {

            ICellCollection c = datasets.get(i).getCollection();
            SignalManager m = c.getSignalManager();

            Set<UUID> groups = m.getSignalGroupIDs();

            for (UUID id : groups) {

                if (id.equals(IShellResult.RANDOM_SIGNAL_ID)) {
                    continue;
                }

                int signalCount = m.getSignalCount(id);

				double[] xpoints = new double[signalCount];
				double[] ypoints = new double[signalCount];

				List<INuclearSignal> list = m.getSignals(id);

				for (int j = 0; j < signalCount; j++) {

				    xpoints[j] = list.get(j).getMeasurement(statA, scale);
				    ypoints[j] = list.get(j).getMeasurement(statB, scale);

				}
				names.add(c.getName() + "_" + m.getSignalGroupName(id));

				double rhoValue = 0;

				if (xpoints.length > 0) { // If a collection has signal
				                          // group, but not signals
				    rhoValue = Stats.getSpearmansCorrelation(xpoints, ypoints);
				}

				rho.add(df.format(rhoValue));
            }

        }

        model.addColumn(Labels.DATASET, names);
        model.addColumn(Labels.Stats.SPEARMANS_RHO, rho);
        return model;

    }
}
