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

package com.bmskinner.nuclear_morphology.components.options;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.options.ClusteringOptions.ClusteringMethod;
import com.bmskinner.nuclear_morphology.components.options.ClusteringOptions.HierarchicalClusterMethod;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

/**
 * This interface describes the options available for clustering
 * cells within datasets.
 * @author bms41
 *
 */
public interface IClusteringOptions extends Serializable {

	/**
	 * Check if the given segment is to be included in the clustering
	 * @param stat
	 * @return
	 */
	boolean isIncludeSegment(UUID i);

	boolean useSegments();

	/**
	 * Get all the segments that are saved in this options object
	 * @return
	 */
	Set<UUID> getSegments();

	/**
	 * Check if the given statistic is to be included in the clustering
	 * @param stat
	 * @return
	 */
	boolean isIncludeStatistic(PlottableStatistic stat);

	/**
	 * Get all the statistics that are saved in this options object
	 * @return
	 */
	Set<PlottableStatistic> getSavedStatistics();

	boolean isIncludeProfile();

	boolean isUseSimilarityMatrix();

	ClusteringMethod getType();

	/**
	 * Get the desired number of hierarchical clusters
	 * @return
	 */
	int getClusterNumber();

	/**
	 * Get the chosen hierarchical method of clustering
	 * @return
	 */
	HierarchicalClusterMethod getHierarchicalMethod();

	int getIterations();

	ProfileType getProfileType();

	boolean isIncludeMesh();

	/**
	 * Get a string array of the options set here suitable
	 * for the Weka HierarchicalClusterer
	 * @return
	 */
	String[] getOptions();

}