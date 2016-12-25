package com.bmskinner.nuclear_morphology.charting.datasets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nuclear_morphology.analysis.signals.SignalManager;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.components.stats.NucleusStatistic;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.components.stats.SignalStatistic;

/**
 * Create scatter chart datasets
 * @author ben
 *
 */
public class ScatterChartDatasetCreator extends AbstractDatasetCreator<ChartOptions> {

	/**
	 * Construct with an options
	 * @param options the chart options
	 */
	public ScatterChartDatasetCreator(final ChartOptions options){
		super(options);
	}
	
	/**
	 * Create a scatter dataset for the given statistics for each analysis dataset
	 * @return a charting dataset
	 * @throws ChartDatasetCreationException
	 */
	public XYDataset createScatterDataset() throws ChartDatasetCreationException{
		
		PlottableStatistic stat = options.getStat();
		
		if(stat instanceof NucleusStatistic){
			return createNucleusScatterDataset();
		}
		
		if(stat instanceof SignalStatistic){
			return createSignalScatterDataset();
		}
		
		throw new ChartDatasetCreationException("Stat not recognised: "+stat);
		
	}
	
	
	
	/*
	 * 
	 * PRIVATE METHODS
	 * 
	 * 
	 */

	/**
	 * Get a boxplot dataset for the given statistic for each collection
	 * @param options the charting options
	 * @return
	 * @throws ChartDatasetCreationException
	 */
	private XYDataset createNucleusScatterDataset() throws ChartDatasetCreationException {
		
		DefaultXYDataset ds = new DefaultXYDataset();
		
		if( ! options.hasDatasets()){
			return ds;
		}
		
		if( ! (options.getStat(0) instanceof NucleusStatistic)){
			throw new ChartDatasetCreationException("Stat 0 cannot be cast to NucleusStatistic");
		}
		
		if( ! (options.getStat(1) instanceof NucleusStatistic)){
			throw new ChartDatasetCreationException("Stat 1 cannot be cast to NucleusStatistic");
		}
		
		List<IAnalysisDataset> datasets = options.getDatasets();
				
		MeasurementScale scale = options.getScale();
		
		NucleusStatistic statA = (NucleusStatistic) options.getStat(0);
		NucleusStatistic statB = (NucleusStatistic) options.getStat(1);

		

		for (int i=0; i < datasets.size(); i++) {
			
			ICellCollection c = datasets.get(i).getCollection();

			// draw the segment itself
			double[] xpoints = new double[c.size()];
			double[] ypoints = new double[c.size()];
			
			List<ICell> cells = new ArrayList<ICell>(c.getCells());
			// go through each index in the segment.
			for(int j=0; j<cells.size();j++){
				
				double statAValue;
				double statBValue;
				
				try {
					
					if(statA.equals(NucleusStatistic.VARIABILITY)){
						statAValue = c.getNormalisedDifferenceToMedian(Tag.REFERENCE_POINT, cells.get(j));
					} else {
						statAValue = cells.get(j).getNucleus().getStatistic(statA, scale);
					}
					
					if(statB.equals(NucleusStatistic.VARIABILITY)){
						statBValue = c.getNormalisedDifferenceToMedian(Tag.REFERENCE_POINT, cells.get(j));
					} else {
						statBValue = cells.get(j).getNucleus().getStatistic(statB, scale);
					}
				} catch(UnavailableBorderTagException e){
					warn("Cannot get stats for cell");
					fine("Tag not present in cell", e);
					statAValue = 0;
					statBValue = 0;
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
	 * @param options the charting options
	 * @return
	 * @throws ChartDatasetCreationException 
	 * @throws Exception
	 */
	private SignalXYDataset createSignalScatterDataset() throws ChartDatasetCreationException {
		List<IAnalysisDataset> datasets = options.getDatasets();
		
		List<PlottableStatistic> stats =  options.getStats();
		
		MeasurementScale scale = options.getScale();
		
		SignalStatistic statA = (SignalStatistic) stats.get(0);
		SignalStatistic statB = (SignalStatistic) stats.get(1);

		SignalXYDataset ds = new SignalXYDataset();

		for (int i=0; i < datasets.size(); i++) {
			
			ICellCollection c = datasets.get(i).getCollection();
			SignalManager m = c.getSignalManager();
			
			Set<UUID> groups = m.getSignalGroupIDs();
			
			for(UUID id : groups){
				
				ISignalGroup gp;
				try {
					gp = c.getSignalGroup(id);
				} catch (UnavailableSignalGroupException e) {
					stack("Error getting signal group", e);
					throw new ChartDatasetCreationException("Cannot get signal group", e);					
				}
				
				int signalCount = m.getSignalCount(id);
				
				double[] xpoints = new double[signalCount];
				double[] ypoints = new double[signalCount];
				
				List<INuclearSignal> list = m.getSignals(id);
				
				for(int j=0; j<signalCount;j++){
					
					xpoints[j] = list.get(j).getStatistic(statA, scale);
					ypoints[j] = list.get(j).getStatistic(statB, scale);

				}

				double[][] data = { xpoints, ypoints };
				
				String seriesKey = c.getName()+"_"+gp.getGroupName();
				ds.addSeries(seriesKey, data);
				ds.addDataset(datasets.get(i), seriesKey);
				ds.addSignalGroup(gp, seriesKey);
				ds.addSignalId(id, seriesKey);
				
			}

			
		}

		return ds;
	}
	
	
}