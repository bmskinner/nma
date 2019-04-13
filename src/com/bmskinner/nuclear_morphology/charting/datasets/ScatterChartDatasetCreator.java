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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nuclear_morphology.analysis.signals.SignalManager;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.Statistical;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.gui.dialogs.TsneDialog.ColourByType;

/**
 * Create scatter chart datasets
 * 
 * @author ben
 *
 */
public class ScatterChartDatasetCreator extends AbstractDatasetCreator<ChartOptions> {
	
    /**
     * Construct with an options
     * 
     * @param options the chart options
     */
    public ScatterChartDatasetCreator(@NonNull final ChartOptions options) {
        super(options);
    }

    /**
     * Create a scatter dataset for the given statistics for each analysis
     * dataset
     * 
     * @return a charting dataset
     * @throws ChartDatasetCreationException
     */
    public XYDataset createScatterDataset(String component) throws ChartDatasetCreationException {

        if (CellularComponent.NUCLEUS.equals(component)) {
            return createNucleusScatterDataset();
        }

        if (CellularComponent.NUCLEAR_SIGNAL.equals(component)) {
            return createSignalScatterDataset();
        }

        throw new ChartDatasetCreationException("Component not recognised: " + component);

    }

    /**
     * Get a boxplot dataset for the given statistic for each collection
     * 
     * @param options the charting options
     * @return
     * @throws ChartDatasetCreationException
     */
    private XYDataset createNucleusScatterDataset() throws ChartDatasetCreationException {

        DefaultXYDataset ds = new DefaultXYDataset();

        if (!options.hasDatasets())
            return ds;

        List<IAnalysisDataset> datasets = options.getDatasets();

        MeasurementScale scale = options.getScale();

        PlottableStatistic statA = options.getStat(0);
        PlottableStatistic statB = options.getStat(1);

        for (int i = 0; i < datasets.size(); i++) {

            ICellCollection c = datasets.get(i).getCollection();
            
            // to make charts more responsive, only take n nuclei
            int count = Math.min(c.getNucleusCount(), MAX_SCATTER_CHART_ITEMS);
            double[] xpoints = new double[count];
            double[] ypoints = new double[count];
            
            List<Nucleus> nuclei = new ArrayList<>();
            nuclei.addAll(c.getNuclei());
            Collections.shuffle(nuclei);
            
            for(int j=0; j<count; j++) {
            	Nucleus n = nuclei.get(j);
                double statAValue;
                double statBValue;

                try {

                    if (statA.equals(PlottableStatistic.VARIABILITY))
                        statAValue = c.getNormalisedDifferenceToMedian(Tag.REFERENCE_POINT, n);
                    else
                        statAValue = n.getStatistic(statA, scale);
                    

                    if (statB.equals(PlottableStatistic.VARIABILITY))
                        statBValue = c.getNormalisedDifferenceToMedian(Tag.REFERENCE_POINT, n);
                    else
                        statBValue = n.getStatistic(statB, scale);
                    
                } catch (UnavailableBorderTagException e) {
                    stack("Tag not present in cell", e);
                    statAValue = Statistical.ERROR_CALCULATING_STAT;
                    statBValue = Statistical.ERROR_CALCULATING_STAT;
                }

                xpoints[j] = statAValue;
                ypoints[j] = statBValue;
            }

            double[][] data = { xpoints, ypoints };
            ds.addSeries(c.getName(), data);

        }

        return ds;
    }

    /**
     * Get a boxplot dataset for the given statistic for each collection
     * 
     * @param options the charting options
     * @return
     * @throws ChartDatasetCreationException
     * @throws Exception
     */
    private SignalXYDataset createSignalScatterDataset() throws ChartDatasetCreationException {
        List<IAnalysisDataset> datasets = options.getDatasets();

        List<PlottableStatistic> stats = options.getStats();

        MeasurementScale scale = options.getScale();

        PlottableStatistic statA = stats.get(0);
        PlottableStatistic statB = stats.get(1);

        SignalXYDataset ds = new SignalXYDataset();

        for (int i = 0; i < datasets.size(); i++) {

            ICellCollection c = datasets.get(i).getCollection();
            SignalManager m = c.getSignalManager();

            for (UUID id : m.getSignalGroupIDs()) {

                ISignalGroup gp = c.getSignalGroup(id).get();

                int signalCount = m.getSignalCount(id);

                double[] xpoints = new double[signalCount];
                double[] ypoints = new double[signalCount];

                List<INuclearSignal> list = m.getSignals(id);

                for (int j = 0; j < signalCount; j++) {

                    xpoints[j] = list.get(j).getStatistic(statA, scale);
                    ypoints[j] = list.get(j).getStatistic(statB, scale);

                }

                double[][] data = { xpoints, ypoints };

                String seriesKey = c.getName() + "_" + gp.getGroupName();
                ds.addSeries(seriesKey, data);
                ds.addDataset(datasets.get(i), seriesKey);
                ds.addSignalGroup(gp, seriesKey);
                ds.addSignalId(id, seriesKey);

            }

        }

        return ds;
    }

    /**
     * A temporary method to create tSNE plots
     * @param r
     * @return
     * @throws ChartDatasetCreationException
     */
    public static XYDataset createTsneScatterDataset(IAnalysisDataset d, ColourByType type, IClusterGroup group) throws ChartDatasetCreationException {
    	ComponentXYDataset<Nucleus> ds = new ComponentXYDataset<>();
    	
    	if(type.equals(ColourByType.CLUSTER) && group==null)
    		type = ColourByType.NONE;
    	
    	if(type.equals(ColourByType.MERGE_SOURCE)) {
    		for(IAnalysisDataset mergeSource : d.getMergeSources()) {
    			List<Nucleus> nuclei = new ArrayList<>(mergeSource.getCollection().getNuclei());
    			double[][] data = createTsneValues(nuclei);
    	    	ds.addSeries(mergeSource.getName()+" tSNE", data, nuclei);
    		}
    		return ds;
    	}
    	
    	if(type.equals(ColourByType.NONE)) {
    		List<Nucleus> nuclei = new ArrayList<>(d.getCollection().getNuclei());
    		double[][] data = createTsneValues(nuclei);
    		ds.addSeries("tSNE", data, nuclei);
    		return ds;
    	}
    	
    	if(type.equals(ColourByType.CLUSTER)) {
    		for(UUID childId : group.getUUIDs()) {
    			IAnalysisDataset childDataset = d.getChildDataset(childId);
    			List<Nucleus> nuclei = new ArrayList<>(childDataset.getCollection().getNuclei());
    			double[][] data = createTsneValues(nuclei);
    	    	ds.addSeries(childDataset.getName()+" tSNE", data, nuclei);
    		}
    		return ds;
    	}
    	return ds;
    }
    
    private static double[][] createTsneValues(List<Nucleus> nuclei ){
    	double[] xpoints = new double[nuclei.size()];
        double[] ypoints = new double[nuclei.size()];
    	// need to transpose the matrix
    	for(int i=0; i<nuclei.size(); i++) {
    		Nucleus n = nuclei.get(i);
    		xpoints[i] =n.getStatistic(PlottableStatistic.TSNE_X);
    		ypoints[i] =n.getStatistic(PlottableStatistic.TSNE_Y);
    	}
    	
    	double[][] data = { xpoints, ypoints };
    	return data;
    }
}
