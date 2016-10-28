package charting.datasets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import analysis.IAnalysisDataset;
import analysis.signals.ShellRandomDistributionCreator;
import analysis.signals.SignalManager;
import charting.options.DefaultChartOptions;
import charting.options.DefaultTableOptions;
import components.ICell;
import components.ICellCollection;
import components.generic.MeasurementScale;
import components.generic.Tag;
import components.nuclear.INuclearSignal;
import components.nuclear.NuclearSignal;
import stats.NucleusStatistic;
import stats.PlottableStatistic;
import stats.SignalStatistic;
import stats.Stats;

public class ScatterChartDatasetCreator extends AbstractDatasetCreator {

	public ScatterChartDatasetCreator(){}


	/**
	 * Get a boxplot dataset for the given statistic for each collection
	 * @param options the charting options
	 * @return
	 * @throws ChartDatasetCreationException
	 */
	public XYDataset createNucleusScatterDataset(DefaultChartOptions options) throws ChartDatasetCreationException {
		
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
				
				if(statA.equals(NucleusStatistic.VARIABILITY)){
					xpoints[j] = c.getNormalisedDifferenceToMedian(Tag.REFERENCE_POINT, cells.get(j));
				} else {
					xpoints[j] = cells.get(j).getNucleus().getStatistic(statA, scale);
				}
				
				if(statB.equals(NucleusStatistic.VARIABILITY)){
					ypoints[j] = c.getNormalisedDifferenceToMedian(Tag.REFERENCE_POINT, cells.get(j));
				} else {
					ypoints[j] = cells.get(j).getNucleus().getStatistic(statB, scale);
				}


			}

			double[][] data = { xpoints, ypoints };
			ds.addSeries(c.getName(), data);
		}

		return ds;
	}
	
	public TableModel createSpearmanCorrlationTable(DefaultTableOptions options){
		
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
			return createNucleusSpearmanCorrlationTable(options);
		}
		
		if(firstStat instanceof SignalStatistic){
			return createSignalSpearmanCorrlationTable(options);
		}
		
		return createBlankTable();
	}
	
	private TableModel createNucleusSpearmanCorrlationTable(DefaultTableOptions options){

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
		
//		DecimalFormat df = new DecimalFormat("#0.00"); 

		for (int i=0; i < datasets.size(); i++) {

			ICellCollection c = datasets.get(i).getCollection();

			// draw the segment itself
			double[] xpoints = new double[c.size()];
			double[] ypoints = new double[c.size()];

			List<ICell> cells = new ArrayList<ICell>(c.getCells());
			// go through each index in the segment.
			for(int j=0; j<cells.size();j++){

				if(statA.equals(NucleusStatistic.VARIABILITY)){
					xpoints[j] = c.getNormalisedDifferenceToMedian(Tag.REFERENCE_POINT, cells.get(j));
				} else {
					xpoints[j] = cells.get(j).getNucleus().getStatistic(statA, scale);
				}
				
				if(statB.equals(NucleusStatistic.VARIABILITY)){
					ypoints[j] = c.getNormalisedDifferenceToMedian(Tag.REFERENCE_POINT, cells.get(j));
				} else {
					ypoints[j] = cells.get(j).getNucleus().getStatistic(statB, scale);
				}

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
		
	/**
	 * Get a boxplot dataset for the given statistic for each collection
	 * @param options the charting options
	 * @return
	 * @throws Exception
	 */
	public XYDataset createSignalScatterDataset(DefaultChartOptions options) {
		List<IAnalysisDataset> datasets = options.getDatasets();
		
		List<PlottableStatistic> stats =  options.getStats();
		
		MeasurementScale scale = options.getScale();
		
		SignalStatistic statA = (SignalStatistic) stats.get(0);
		SignalStatistic statB = (SignalStatistic) stats.get(1);

		DefaultXYDataset ds = new DefaultXYDataset();

		for (int i=0; i < datasets.size(); i++) {
			
			ICellCollection c = datasets.get(i).getCollection();
			SignalManager m = c.getSignalManager();
			
			Set<UUID> groups = m.getSignalGroupIDs();
			
			for(UUID id : groups){
				
				int signalCount = m.getSignalCount(id);
				
				double[] xpoints = new double[signalCount];
				double[] ypoints = new double[signalCount];
				
				List<INuclearSignal> list = m.getSignals(id);
				
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
	
	private TableModel createSignalSpearmanCorrlationTable(DefaultTableOptions options){

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
			}

			
		}

		model.addColumn("Dataset", names);
		model.addColumn("Spearman's Rho", rho);
		return model;

	}
}