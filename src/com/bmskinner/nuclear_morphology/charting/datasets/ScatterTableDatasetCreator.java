package com.bmskinner.nuclear_morphology.charting.datasets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.analysis.signals.ShellRandomDistributionCreator;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalManager;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.components.stats.NucleusStatistic;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.components.stats.SignalStatistic;
import com.bmskinner.nuclear_morphology.stats.Stats;

public class ScatterTableDatasetCreator extends AbstractDatasetCreator<TableOptions> {
	
	public ScatterTableDatasetCreator(final TableOptions options){
		super(options);
	}
	
	/**
	 * Create a table model for the Spearman's Rank correlation coefficients between the 
	 * selected statisics
	 * @return
	 */
	public TableModel createSpearmanCorrlationTable(){
		
		if( ! options.hasDatasets()){
			return AnalysisDatasetTableCreator.createBlankTable();
		}
		
		if(options.getStats().size()!=2){
			return AnalysisDatasetTableCreator.createBlankTable();
		}
		
		PlottableStatistic firstStat = options.getStat();
		
		for(PlottableStatistic stat : options.getStats()){
			if( ! stat.getClass().equals(firstStat.getClass())){
				fine("Statistic classes are different");
				createBlankTable();
			}
		}
		
		if(firstStat instanceof NucleusStatistic){
			return createNucleusSpearmanCorrlationTable();
		}
		
		if(firstStat instanceof SignalStatistic){
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
	
	
	private TableModel createNucleusSpearmanCorrlationTable(){

		if( ! options.hasDatasets()){
			return createBlankTable();
		}
		
		if(options.getStats().size()!=2){
			return createBlankTable();
		}
		
		DefaultTableModel model = new DefaultTableModel();

		Vector<Object> names 	= new Vector<Object>();
		Vector<Object> rho   	= new Vector<Object>();


		List<IAnalysisDataset> datasets = options.getDatasets();

		List<PlottableStatistic> stats =  options.getStats();

		MeasurementScale scale = options.getScale();

		NucleusStatistic statA = (NucleusStatistic) stats.get(0);
		NucleusStatistic statB = (NucleusStatistic) stats.get(1);
		
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
			names.add(c.getName());
			
			double rhoValue = Stats.getSpearmansCorrelation(xpoints, ypoints);
			rho.add( DEFAULT_DECIMAL_FORMAT.format( rhoValue ));
			
//			double p = Stats.getSpearmanPValue(rhoValue, cells.size());
//			pValue.add( df.format( p ));
		}


		model.addColumn("Dataset", names);
		model.addColumn("Spearman's Rho", rho);
//		model.addColumn("p", pValue);
		return model;

	}
	
	private TableModel createSignalSpearmanCorrlationTable(){

		DefaultTableModel model = new DefaultTableModel();

		Vector<Object> names 	= new Vector<Object>();
		Vector<Object> rho   	= new Vector<Object>();


		List<IAnalysisDataset> datasets = options.getDatasets();

		List<PlottableStatistic> stats =  options.getStats();

		MeasurementScale scale = options.getScale();

		SignalStatistic statA = (SignalStatistic) stats.get(0);
		SignalStatistic statB = (SignalStatistic) stats.get(1);
				
		for (int i=0; i < datasets.size(); i++) {
			
			ICellCollection c = datasets.get(i).getCollection();
			SignalManager m = c.getSignalManager();
			
			Set<UUID> groups = m.getSignalGroupIDs();
			
			for(UUID id : groups){

				if(id.equals(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)){
					continue;
				}

				try {

					int signalCount = m.getSignalCount(id);

					double[] xpoints = new double[signalCount];
					double[] ypoints = new double[signalCount];

					List<INuclearSignal> list = m.getSignals(id);

					for(int j=0; j<signalCount;j++){

						xpoints[j] = list.get(j).getStatistic(statA, scale);
						ypoints[j] = list.get(j).getStatistic(statB, scale);

					}
					names.add(c.getName()+"_"+m.getSignalGroupName(id));

					double rhoValue = 0;

					if(xpoints.length>0){ // If a collection has signal group, but not signals
						rhoValue = Stats.getSpearmansCorrelation(xpoints, ypoints);
					}

					rho.add( DEFAULT_DECIMAL_FORMAT.format( rhoValue ) );

				} catch (UnavailableSignalGroupException e){
					fine("Signal group "+id+" is not present in collection", e);
				}
			}

			
		}

		model.addColumn("Dataset", names);
		model.addColumn("Spearman's Rho", rho);
		return model;

	}
}
