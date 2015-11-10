/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
		CLUSTER 			("Cluster"),
		RECALCULATE_CACHE	("Recalculate caches"),
		SAVE				("Save selected"),
		ADD_DATASET			("Add dataset");
		
		private final String name;
		
		DatasetMethod(String name){
			this.name = name;
		}
		
		public String toString(){
			return this.name;
		}
		
		
	}

}
