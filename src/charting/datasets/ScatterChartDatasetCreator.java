package charting.datasets;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import analysis.AnalysisDataset;
import analysis.signals.SignalManager;
import charting.charts.ScatterChartFactory;
import charting.options.ChartOptions;
import charting.options.TableOptions;
import components.AbstractCellularComponent;
import components.Cell;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.BorderTagObject;
import components.generic.MeasurementScale;
import components.nuclear.BorderPoint;
import components.nuclear.NuclearSignal;
import logging.Loggable;
import stats.NucleusStatistic;
import stats.PlottableStatistic;
import stats.SignalStatistic;
import stats.Stats;

public class ScatterChartDatasetCreator implements Loggable {
	
private static ScatterChartDatasetCreator instance = null;
	
	protected ScatterChartDatasetCreator(){}
	
	/**
	 * Fetch an instance of the factory
	 * @return
	 */
	public static ScatterChartDatasetCreator getInstance(){
		if(instance==null){
			instance = new ScatterChartDatasetCreator();
		}
		return instance;
	}


	/**
	 * Get a boxplot dataset for the given statistic for each collection
	 * @param options the charting options
	 * @return
	 * @throws Exception
	 */
	public XYDataset createNucleusScatterDataset(ChartOptions options) {
		List<AnalysisDataset> datasets = options.getDatasets();
		
		List<PlottableStatistic> stats =  options.getStats();
		
		MeasurementScale scale = options.getScale();
		
		NucleusStatistic statA = (NucleusStatistic) stats.get(0);
		NucleusStatistic statB = (NucleusStatistic) stats.get(1);

		DefaultXYDataset ds = new DefaultXYDataset();

		for (int i=0; i < datasets.size(); i++) {
			
			CellCollection c = datasets.get(i).getCollection();

			// draw the segment itself
			double[] xpoints = new double[c.getNucleusCount()];
			double[] ypoints = new double[c.getNucleusCount()];
			
			List<Cell> cells = c.getCells();
			// go through each index in the segment.
			for(int j=0; j<cells.size();j++){
				
				if(statA.equals(NucleusStatistic.VARIABILITY)){
					xpoints[j] = c.getNormalisedDifferenceToMedian(BorderTagObject.REFERENCE_POINT, cells.get(j));
				} else {
					xpoints[j] = cells.get(j).getNucleus().getStatistic(statA, scale);
				}
				
				if(statB.equals(NucleusStatistic.VARIABILITY)){
					ypoints[j] = c.getNormalisedDifferenceToMedian(BorderTagObject.REFERENCE_POINT, cells.get(j));
				} else {
					ypoints[j] = cells.get(j).getNucleus().getStatistic(statB, scale);
				}


			}

			double[][] data = { xpoints, ypoints };
			ds.addSeries(c.getName(), data);
		}

		return ds;
	}
	
	public TableModel createSpearmanCorrlationTable(TableOptions options){
		
		if( ! options.hasDatasets()){
			return NucleusTableDatasetCreator.getInstance().createBlankTable();
		}
		
		if(options.getStats().size()!=2){
			return NucleusTableDatasetCreator.getInstance().createBlankTable();
		}
		
		PlottableStatistic firstStat = options.getStat();
		
		for(PlottableStatistic stat : options.getStats()){
			if( ! stat.getClass().equals(firstStat.getClass())){
				fine("Statistic classes are different");
				NucleusTableDatasetCreator.getInstance().createBlankTable();
			}
		}
		
		if(firstStat.getClass().equals(NucleusStatistic.class)){
			return createNucleusSpearmanCorrlationTable(options);
		}
		
		if(firstStat.getClass().equals(SignalStatistic.class)){
			return createSignalSpearmanCorrlationTable(options);
		}
		
		return NucleusTableDatasetCreator.getInstance().createBlankTable();
	}
	
	private TableModel createNucleusSpearmanCorrlationTable(TableOptions options){

		if( ! options.hasDatasets()){
			return NucleusTableDatasetCreator.getInstance().createBlankTable();
		}
		
		if(options.getStats().size()!=2){
			return NucleusTableDatasetCreator.getInstance().createBlankTable();
		}
		
		DefaultTableModel model = new DefaultTableModel();

		Vector<Object> names 	= new Vector<Object>();
		Vector<Object> rho   	= new Vector<Object>();
		Vector<Object> pValue  	= new Vector<Object>();

		List<AnalysisDataset> datasets = options.getDatasets();

		List<PlottableStatistic> stats =  options.getStats();

		MeasurementScale scale = options.getScale();

		NucleusStatistic statA = (NucleusStatistic) stats.get(0);
		NucleusStatistic statB = (NucleusStatistic) stats.get(1);
		
		DecimalFormat df = new DecimalFormat("#0.00"); 

		for (int i=0; i < datasets.size(); i++) {

			CellCollection c = datasets.get(i).getCollection();

			// draw the segment itself
			double[] xpoints = new double[c.getNucleusCount()];
			double[] ypoints = new double[c.getNucleusCount()];

			List<Cell> cells = c.getCells();
			// go through each index in the segment.
			for(int j=0; j<cells.size();j++){

				if(statA.equals(NucleusStatistic.VARIABILITY)){
					xpoints[j] = c.getNormalisedDifferenceToMedian(BorderTagObject.REFERENCE_POINT, cells.get(j));
				} else {
					xpoints[j] = cells.get(j).getNucleus().getStatistic(statA, scale);
				}
				
				if(statB.equals(NucleusStatistic.VARIABILITY)){
					ypoints[j] = c.getNormalisedDifferenceToMedian(BorderTagObject.REFERENCE_POINT, cells.get(j));
				} else {
					ypoints[j] = cells.get(j).getNucleus().getStatistic(statB, scale);
				}

			}
			names.add(c.getName());
			
			double rhoValue = Stats.getSpearmansCorrelation(xpoints, ypoints);
			rho.add( df.format( rhoValue ));
			
//			double p = Stats.getSpearmanPValue(rhoValue, cells.size());
//			pValue.add( df.format( p ));
		}


		model.addColumn("Dataset", names);
		model.addColumn("Spearman's Rho", rho);
//		model.addColumn("p", pValue);
		return model;

	}
		
	/**
	 * Get a boxplot dataset for the given statistic for each collection
	 * @param options the charting options
	 * @return
	 * @throws Exception
	 */
	public XYDataset createSignalScatterDataset(ChartOptions options) {
		List<AnalysisDataset> datasets = options.getDatasets();
		
		List<PlottableStatistic> stats =  options.getStats();
		
		MeasurementScale scale = options.getScale();
		
		SignalStatistic statA = (SignalStatistic) stats.get(0);
		SignalStatistic statB = (SignalStatistic) stats.get(1);

		DefaultXYDataset ds = new DefaultXYDataset();

		for (int i=0; i < datasets.size(); i++) {
			
			CellCollection c = datasets.get(i).getCollection();
			SignalManager m = c.getSignalManager();
			
			Set<UUID> groups = m.getSignalGroupIDs();
			
			for(UUID id : groups){
				
				int signalCount = m.getSignalCount(id);
				
				double[] xpoints = new double[signalCount];
				double[] ypoints = new double[signalCount];
				
				List<NuclearSignal> list = m.getSignals(id);
				
				for(int j=0; j<signalCount;j++){
					
					xpoints[j] = list.get(j).getStatistic(statA, scale);
					ypoints[j] = list.get(j).getStatistic(statB, scale);

				}

				double[][] data = { xpoints, ypoints };
				ds.addSeries(c.getName()+"|"+id.toString(), data);
				
			}

			
		}

		return ds;
	}
	
	private TableModel createSignalSpearmanCorrlationTable(TableOptions options){

		DefaultTableModel model = new DefaultTableModel();

		Vector<Object> names 	= new Vector<Object>();
		Vector<Object> rho   	= new Vector<Object>();
		Vector<Object> pValue  	= new Vector<Object>();


		List<AnalysisDataset> datasets = options.getDatasets();

		List<PlottableStatistic> stats =  options.getStats();

		MeasurementScale scale = options.getScale();

		SignalStatistic statA = (SignalStatistic) stats.get(0);
		SignalStatistic statB = (SignalStatistic) stats.get(1);
		
		DecimalFormat df = new DecimalFormat("#0.00"); 
		
		for (int i=0; i < datasets.size(); i++) {
			
			CellCollection c = datasets.get(i).getCollection();
			SignalManager m = c.getSignalManager();
			
			Set<UUID> groups = m.getSignalGroupIDs();
			
			for(UUID id : groups){
				
				int signalCount = m.getSignalCount(id);
				
				double[] xpoints = new double[signalCount];
				double[] ypoints = new double[signalCount];
				
				List<NuclearSignal> list = m.getSignals(id);
				
				for(int j=0; j<signalCount;j++){
					
					xpoints[j] = list.get(j).getStatistic(statA, scale);
					ypoints[j] = list.get(j).getStatistic(statB, scale);

				}
				names.add(c.getName()+"_"+m.getSignalGroupName(id));
				
				double rhoValue = 0;
//				double p        = 0;
				
				if(xpoints.length>0){ // If a collection has signal group, but not signals
					rhoValue = Stats.getSpearmansCorrelation(xpoints, ypoints);
//					p = Stats.getSpearmanPValue(rhoValue, list.size());
				}
				
				rho.add( df.format( rhoValue ) );
//				pValue.add( df.format( p ));
			}

			
		}

		model.addColumn("Dataset", names);
		model.addColumn("Spearman's Rho", rho);
//		model.addColumn("p", pValue);
		return model;

	}
}