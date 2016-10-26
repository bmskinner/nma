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
package gui;

import java.util.EventObject;
import java.util.List;

import analysis.AnalysisDataset;
import analysis.IAnalysisDataset;

public class DatasetEvent extends EventObject {
	
	public static final String PROFILING_ACTION    = "Profiling action";
	public static final String NEW_MORPHOLOGY      = "New morphology";
	public static final String COPY_MORPHOLOGY     = "Copy morphology";
	public static final String REFRESH_MORPHOLOGY  = "Refresh morphology";
	public static final String REFOLD_CONSENSUS    = "Refold consensus";
	public static final String SELECT_DATASETS     = "Select multiple datasets";
	public static final String SELECT_ONE_DATASET  = "Select single dataset";
	public static final String EXTRACT_SOURCE      = "Extract source";
	public static final String CLUSTER             =  "Cluster";
	public static final String BUILD_TREE          = "Build tree";
	public static final String TRAIN_CLASSIFIER    = "Train classifier";
	public static final String REFRESH_CACHE       = "Refresh caches";
	public static final String CLEAR_CACHE         = "Clear caches";
	public static final String SAVE	               = "Save selected";
	public static final String SAVE_AS             = "Save as new file";
	public static final String ADD_DATASET         = "Add dataset";
	public static final String RESEGMENT           = "Resegment dataset";
	public static final String RECALCULATE_MEDIAN  = "Recalculate median profiles";
	

	private static final long serialVersionUID = 1L;
	private String sourceName;
	private List<IAnalysisDataset> list;
	private String method;
	private IAnalysisDataset secondaryDataset = null; // for use in e.g. morphology copying. Optional

	/**
	 * Create an event from a source, with the given message
	 * @param source the source of the datasets
	 * @param message the instruction on what to do with the datasets
	 * @param sourceName the name of the object or component generating the datasets
	 * @param list the datasets to carry
	 */
	public DatasetEvent( Object source, String method, String sourceName, List<IAnalysisDataset> list ) {
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
	 * @param sourceDataset a secondary dataset to use when handling the list
	 * @param list the datasets to carry
	 */
	public DatasetEvent( Object source, String method, String sourceName, List<IAnalysisDataset> list, IAnalysisDataset sourceDataset) {
		this(source, method, sourceName, list);
		this.secondaryDataset = sourceDataset;
	}
	
	/**
	 * Construct from an existing event. Use to pass messages on.
	 * @param event
	 */
	public DatasetEvent(Object source, DatasetEvent event){
		super(source);
		this.method     = event.method();
		this.sourceName = event.sourceName();
		this.list       = event.getDatasets();
		this.secondaryDataset = event.secondaryDataset();
	}
	
	/**
	 * The message to carry
	 * @return
	 */
	public String method() {
		return method;
	}
	
	/**
	 * Get the datasets in the event
	 * @return
	 */
	public List<IAnalysisDataset> getDatasets(){
		return list;
	}
	
	/**
	 * Get the first dataset in the list. Use if only
	 * one dataset is present.
	 * @return
	 */
	public IAnalysisDataset firstDataset(){
		return list.get(0);
	}
	
	/**
	 * Check if any datasets are present
	 * @return
	 */
	public boolean hasDatasets(){
		if(list==null || list.isEmpty()){
			return false;
		}
		return true;
	}
	
	public boolean hasSecondaryDataset(){
		if(secondaryDataset==null){
			return false;
		}
		return true;
	}
	
	/**
	 * The name of the component that fired the event
	 * @return
	 */
	public String sourceName(){
		return this.sourceName;
	}
	
	
	/**
	 * Get the secondary dataset, or null if not set
	 * @return
	 */
	public IAnalysisDataset secondaryDataset(){
		return secondaryDataset;
	}
	
//	public enum DatasetMethod {
//		
//		PROFILING_ACTION    ("Profiling action"),
//		NEW_MORPHOLOGY 		("New morphology"),
//		COPY_MORPHOLOGY		("Copy morphology"),
//		REFRESH_MORPHOLOGY	("Refresh morphology"),
//		REFOLD_CONSENSUS	("Refold consensus"),
//		SELECT_DATASETS		("Select multiple datasets"),
//		SELECT_ONE_DATASET	("Select single dataset"),
//		EXTRACT_SOURCE		("Extract source"),
//		CLUSTER 			("Cluster"),
//		BUILD_TREE			("Build tree"),
//		TRAIN_CLASSIFIER	("Train classifier"),
//		REFRESH_CACHE	    ("Refresh caches"),
//		CLEAR_CACHE	        ("Clear caches"),
//		SAVE				("Save selected"),
//		SAVE_AS				("Save as new file"),
//		ADD_DATASET			("Add dataset"),
//		RESEGMENT			("Resegment dataset"),
//		RECALCULATE_MEDIAN  ("Recalculate median profiles");
//		
//		private final String name;
//		
//		DatasetMethod(String name){
//			this.name = name;
//		}
//		
//		public String toString(){
//			return this.name;
//		}
//		
//		
//	}

}
