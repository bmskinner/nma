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
package com.bmskinner.nuclear_morphology.gui.events;

import java.util.EventObject;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;

/**
 * Carries instructions on analyses to be performed on a list of datasets.
 * 
 * @author ben
 *
 */
public class DatasetEvent extends EventObject {

	/** Profile and segment */
	public static final String MORPHOLOGY_ANALYSIS_ACTION = "Morphology analysis action";
	
    public static final String PROFILING_ACTION = "Profiling action";
    
    /** Run segmentation on the datasets using existing tagged profiles */
    public static final String SEGMENTATION_ACTION   = "Segment dataset";

    /**
     * Signal that segments should be copied from the secondary dataset in the
     * action to all primary datasets listed
     */
    public static final String COPY_PROFILE_SEGMENTATION = "Copy profile segmentation";

    /**
     * Rerun the segmentation on the given datasets, without trying to add as a new
     * population.
     */
    public static final String REFRESH_MORPHOLOGY = "Refresh morphology";
    
    /**
     * Run new profiling and segmentation on the given datasets. Do not attempt to add
     * as a new population.
     */
    public static final String REFPAIR_SEGMENTATION = "Repair segmentation";

    public static final String REFOLD_CONSENSUS   = "Refold consensus";
    public static final String SELECT_DATASETS    = "Select multiple datasets";
    public static final String SELECT_ONE_DATASET = "Select single dataset";
    public static final String EXTRACT_SOURCE     = "Extract source";
    public static final String CLUSTER            = "Cluster";
    public static final String MANUAL_CLUSTER     = "Manual cluster";
    public static final String CLUSTER_FROM_FILE  = "ClusterFromFile";
    public static final String BUILD_TREE         = "Build tree";
    public static final String TRAIN_CLASSIFIER   = "Train classifier";
        
    /**
     * Clear charts containing the associated datasets from caches, and recreate them. 
     */
    public static final String RECACHE_CHARTS      = "Refresh caches"; // signal the chart cache should be refreshed for given datasets
    
    /**
     * Clear charts containing the associated datasets from caches, but do not recreate them. 
     */
    public static final String CLEAR_CACHE        = "Clear caches";
    public static final String SAVE               = "Save selected";
    public static final String SAVE_AS            = "Save as new file";
    public static final String ADD_DATASET        = "Add dataset";
    public static final String ADD_WORKSPACE      = "Add workspace";
    public static final String RESEGMENT          = "Resegment dataset";
    
    /** Rerun the profiling action to generate new median profiles. Also recalculates
     * the positions of border tags using built in rules. Does not recache charts. */
    public static final String RECALCULATE_MEDIAN = "Recalculate median profiles";
    public static final String RUN_SHELL_ANALYSIS = "Run shell analysis";
    
    public static final String RUN_GLCM_ANALYSIS  = "Run GLCM analysis";
       

    private static final long      serialVersionUID = 1L;
    private String                 sourceName;
    private List<IAnalysisDataset> list;
    private String                 method;
    private IAnalysisDataset       secondaryDataset = null; // for use in e.g.
                                                            // morphology
                                                            // copying. Optional

    /**
     * Create an event from a source, with the given message
     * 
     * @param source
     *            the source of the datasets
     * @param message
     *            the instruction on what to do with the datasets
     * @param sourceName
     *            the name of the object or component generating the datasets
     * @param list
     *            the datasets to carry
     */
    public DatasetEvent(Object source, String method, String sourceName, List<IAnalysisDataset> list) {
        super(source);
        this.method = method;
        this.sourceName = sourceName;
        this.list = list;
    }

    /**
     * Create an event from a source, with the given message
     * 
     * @param source
     *            the source of the datasets
     * @param message
     *            the instruction on what to do with the datasets
     * @param sourceName
     *            the name of the object or component generating the datasets
     * @param sourceDataset
     *            a secondary dataset to use when handling the list
     * @param list
     *            the datasets to carry
     */
    public DatasetEvent(Object source, String method, String sourceName, List<IAnalysisDataset> list,
            IAnalysisDataset sourceDataset) {
        this(source, method, sourceName, list);
        this.secondaryDataset = sourceDataset;
    }

    /**
     * Construct from an existing event. Use to pass messages on.
     * 
     * @param event
     */
    public DatasetEvent(Object source, DatasetEvent event) {
        super(source);
        this.method = event.method();
        this.sourceName = event.sourceName();
        this.list = event.getDatasets();
        this.secondaryDataset = event.secondaryDataset();
    }

    /**
     * The message to carry
     * 
     * @return
     */
    public String method() {
        return method;
    }

    /**
     * Get the datasets in the event
     * 
     * @return
     */
    public List<IAnalysisDataset> getDatasets() {
        return list;
    }

    /**
     * Get the first dataset in the list. Use if only one dataset is present.
     * 
     * @return
     */
    public IAnalysisDataset firstDataset() {
        return list.get(0);
    }

    /**
     * Check if any datasets are present
     * 
     * @return
     */
    public boolean hasDatasets() {
    	return list != null && !list.isEmpty();
    }

    public boolean hasSecondaryDataset() {
        return secondaryDataset != null;
    }

    /**
     * The name of the component that fired the event
     * 
     * @return
     */
    public String sourceName() {
        return this.sourceName;
    }

    /**
     * Get the secondary dataset, or null if not set
     * 
     * @return
     */
    public IAnalysisDataset secondaryDataset() {
        return secondaryDataset;
    }
}
