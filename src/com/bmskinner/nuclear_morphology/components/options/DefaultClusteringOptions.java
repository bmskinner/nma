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
package com.bmskinner.nuclear_morphology.components.options;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.io.xml.XMLCreator;

/**
 * Clustering optins using the hash options interface
 * @author ben
 * @since 1.14.0
 *
 */
public class DefaultClusteringOptions extends AbstractHashOptions implements IClusteringOptions {

	private static final long serialVersionUID = 1L;
	
	public DefaultClusteringOptions() {
		this(ClusteringMethod.HIERARCHICAL);
	}

	public DefaultClusteringOptions(@NonNull ClusteringMethod method) {
		setString(CLUSTER_METHOD_KEY, method.name());
		setString(HIERARCHICAL_METHOD_KEY, DEFAULT_HIERARCHICAL_METHOD.name());
		setString(PROFILE_TYPE_KEY, DEFAULT_PROFILE_TYPE.name());

		setBoolean(USE_SIMILARITY_MATRIX_KEY, DEFAULT_USE_SIMILARITY_MATRIX);
		setBoolean(INCLUDE_PROFILE_KEY, DEFAULT_INCLUDE_PROFILE);
		setBoolean(INCLUDE_MESH_KEY, DEFAULT_INCLUDE_MESH);
		setBoolean(USE_TSNE_KEY, DEFAULT_USE_TSNE);

		setInt(EM_ITERATIONS_KEY, DEFAULT_EM_ITERATIONS);
		setInt(MANUAL_CLUSTER_NUMBER_KEY, DEFAULT_MANUAL_CLUSTER_NUMBER);

		for (PlottableStatistic stat : PlottableStatistic.getRoundNucleusStats())
			setBoolean(stat.toString(), false);
	}

	public DefaultClusteringOptions(@NonNull IClusteringOptions oldOptions) {
		setString(CLUSTER_METHOD_KEY, oldOptions.getType().name());
		setString(HIERARCHICAL_METHOD_KEY, oldOptions.getHierarchicalMethod().name());
		setString(PROFILE_TYPE_KEY, oldOptions.getProfileType().name());

		setBoolean(USE_SIMILARITY_MATRIX_KEY, oldOptions.isUseSimilarityMatrix());
		setBoolean(INCLUDE_PROFILE_KEY, oldOptions.isIncludeProfile());
		setBoolean(INCLUDE_MESH_KEY, oldOptions.isIncludeMesh());
		setBoolean(USE_TSNE_KEY, oldOptions.getBoolean(USE_TSNE_KEY));

		setInt(EM_ITERATIONS_KEY, oldOptions.getIterations());
		setInt(MANUAL_CLUSTER_NUMBER_KEY, oldOptions.getClusterNumber());

		for (PlottableStatistic stat : PlottableStatistic.getRoundNucleusStats())
			setBoolean(stat.toString(), oldOptions.isIncludeStatistic(stat));

		for (UUID id : oldOptions.getSegments())
			setBoolean(id.toString(), oldOptions.isIncludeSegment(id));
	}
	
	@Override
	public IClusteringOptions duplicate() {
		return new DefaultClusteringOptions(this);
	}

	@Override
	public boolean isIncludeSegment(UUID i) {
		if(boolMap.containsKey(i.toString()))
			return boolMap.get(i.toString());
		return false;
	}

	@Override
	public Set<UUID> getSegments() {
		Set<UUID> result = new HashSet<>();
		for(String s : boolMap.keySet()) {
			if(XMLCreator.isUUID(s))
				result.add(UUID.fromString(s));
		}
		return result;
	}

	@Override
	public boolean isIncludeStatistic(PlottableStatistic stat) {
		if(boolMap.containsKey(stat.toString()))
			return boolMap.get(stat.toString());
		return false;
	}


	@Override
	public boolean isIncludeProfile() {
		if(boolMap.containsKey(INCLUDE_PROFILE_KEY))
			return boolMap.get(INCLUDE_PROFILE_KEY);
		return false;
	}

	@Override
	public boolean isUseSimilarityMatrix() {
		if(boolMap.containsKey(DEFAULT_USE_SIMILARITY_MATRIX))
			return boolMap.get(DEFAULT_USE_SIMILARITY_MATRIX);
		return false;
	}

	@Override
	public ClusteringMethod getType() {
		if(stringMap.containsKey(CLUSTER_METHOD_KEY))
			return ClusteringMethod.valueOf(stringMap.get(CLUSTER_METHOD_KEY));
		return null;
	}

	@Override
	public int getClusterNumber() {
		if(intMap.containsKey(MANUAL_CLUSTER_NUMBER_KEY))
			return intMap.get(MANUAL_CLUSTER_NUMBER_KEY);
		return 0;
	}

	@Override
	public HierarchicalClusterMethod getHierarchicalMethod() {
		if(stringMap.containsKey(HIERARCHICAL_METHOD_KEY))
			return HierarchicalClusterMethod.valueOf(stringMap.get(HIERARCHICAL_METHOD_KEY));
		return null;
	}

	@Override
	public int getIterations() {
		if(intMap.containsKey(EM_ITERATIONS_KEY))
			return intMap.get(EM_ITERATIONS_KEY);
		return 0;
	}

	@Override
	public ProfileType getProfileType() {
		if(stringMap.containsKey(PROFILE_TYPE_KEY))
			return ProfileType.valueOf(stringMap.get(PROFILE_TYPE_KEY));
		return null;
	}

	@Override
	public boolean isIncludeMesh() {
		if(boolMap.containsKey(INCLUDE_MESH_KEY))
			return boolMap.get(INCLUDE_MESH_KEY);
		return false;
	}

	@Override
	public String[] getOptions() {
		String[] options = null;

        if (this.getType().equals(ClusteringMethod.EM)) {
            options = new String[2];
            options[0] = "-I"; // max. iterations
            options[1] = String.valueOf(getIterations());
        }

        if (this.getType().equals(ClusteringMethod.HIERARCHICAL)) {
            options = new String[4];
            options[0] = "-N"; // number of clusters
            options[1] = String.valueOf(getClusterNumber());
            options[2] = "-L"; // algorithm
            options[3] = getHierarchicalMethod().code();
        }

        return options;
	}

	@Override
	public void setClusterNumber(int defaultManualClusterNumber) {
		setInt(MANUAL_CLUSTER_NUMBER_KEY, defaultManualClusterNumber);
	}

	@Override
	public void setHierarchicalMethod(HierarchicalClusterMethod defaultHierarchicalMethod) {
		setString(HIERARCHICAL_METHOD_KEY, defaultHierarchicalMethod.name());
	}

	@Override
	public void setIterations(int defaultEmIterations) {
		setInt(EM_ITERATIONS_KEY, defaultEmIterations);
	}

	@Override
	public void setUseSimilarityMatrix(boolean defaultUseSimilarityMatrix) {
		setBoolean(USE_SIMILARITY_MATRIX_KEY, defaultUseSimilarityMatrix);
	}

	@Override
	public void setIncludeProfile(boolean defaultIncludeProfile) {
		setBoolean(INCLUDE_PROFILE_KEY, defaultIncludeProfile);
	}

	@Override
	public void setProfileType(ProfileType defaultProfileType) {
		setString(PROFILE_TYPE_KEY, defaultProfileType.name());
	}

	@Override
	public void setIncludeMesh(boolean defaultIncludeMesh) {
		setBoolean(INCLUDE_MESH_KEY, defaultIncludeMesh);
	}

	@Override
	public void setIncludeStatistic(PlottableStatistic stat, boolean selected) {
		setBoolean(stat.toString(), selected);
	}

	@Override
	public void setIncludeSegment(UUID id, boolean selected) {
		setBoolean(id.toString(), selected);
	}

	@Override
	public void setType(ClusteringMethod hierarchical) {
		setString(CLUSTER_METHOD_KEY, hierarchical.name());
	}


}
