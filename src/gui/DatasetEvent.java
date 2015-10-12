package gui;

import java.util.EventObject;
import java.util.List;

import analysis.AnalysisDataset;

public class DatasetEvent extends EventObject {

	private static final long serialVersionUID = 1L;
	private String sourceName;
	private List<AnalysisDataset> list;
	private DatasetMethod method;
	private AnalysisDataset secondaryDataset = null; // for use in e.g. morphology copying. Optional

	/**
	 * Create an event from a source, with the given message
	 * @param source the source of the datasets
	 * @param message the instruction on what to do with the datasets
	 * @param sourceName the name of the object or component generating the datasets
	 * @param list the datasets to carry
	 */
	public DatasetEvent( Object source, DatasetMethod method, String sourceName, List<AnalysisDataset> list ) {
		super( source );
		this.method = method;
		this.sourceName = sourceName;
		this.list = list;
	}
	
	/**
	 * Create an event from a source, with the given message
	 * @param source the source of the datasets
	 * @param message the instruction on what to do with the datasets
	 * @param sourceName the name of the object or component generating the datasets
	 * @param list the datasets to carry
	 */
	public DatasetEvent( Object source, DatasetMethod method, String sourceName, List<AnalysisDataset> list, AnalysisDataset sourceDataset) {
		this(source, method, sourceName, list);
		this.secondaryDataset = sourceDataset;
	}
	
	/**
	 * The message to carry
	 * @return
	 */
	public DatasetMethod method() {
		return method;
	}
	
	public List<AnalysisDataset> getDatasets(){
		return list;
	}
	
	/**
	 * The name of the component that fired the event
	 * @return
	 */
	public String sourceName(){
		return this.sourceName;
	}
	
	public AnalysisDataset secondaryDataset(){
		return secondaryDataset;
	}
	
	public enum DatasetMethod {
		
		NEW_MORPHOLOGY 		("New morphology"),
		COPY_MORPHOLOGY		("Copy morphology"),
		REFRESH_MORPHOLOGY	("Refresh morphology"),
		REFOLD_CONSENSUS	("Refold consensus"),
		SELECT_DATASETS		("Select datasets"),
		EXTRACT_SOURCE		("Extract source"),
		CLUSTER 			("Cluster");
		
		private final String name;
		
		DatasetMethod(String name){
			this.name = name;
		}
		
		public String toString(){
			return this.name;
		}
		
		
	}

}
