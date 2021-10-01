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

import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.io.xml.XMLCreator;

/**
 * Clustering optins using the hash options interface
 * @author ben
 * @since 1.14.0
 *
 */
public class DefaultClusteringOptions extends DefaultOptions implements IClusteringOptions {

	private static final long serialVersionUID = 1L;
	
	public DefaultClusteringOptions() {
		this(ClusteringMethod.HIERARCHICAL);
	}

	public DefaultClusteringOptions(@NonNull ClusteringMethod method) {
		setString(CLUSTER_METHOD_KEY, method.name());
		setString(HIERARCHICAL_METHOD_KEY, DEFAULT_HIERARCHICAL_METHOD.name());

		setBoolean(USE_SIMILARITY_MATRIX_KEY, DEFAULT_USE_SIMILARITY_MATRIX);
		setBoolean(INCLUDE_MESH_KEY, DEFAULT_INCLUDE_MESH);
		setBoolean(USE_TSNE_KEY, DEFAULT_USE_TSNE);

		setInt(EM_ITERATIONS_KEY, DEFAULT_EM_ITERATIONS);
		setInt(MANUAL_CLUSTER_NUMBER_KEY, DEFAULT_MANUAL_CLUSTER_NUMBER);

		for (Measurement stat : Measurement.getRoundNucleusStats())
			setBoolean(stat.toString(), false);
		
		setBoolean(DEFAULT_PROFILE_TYPE.toString(), DEFAULT_INCLUDE_PROFILE);
	}

	public DefaultClusteringOptions(@NonNull IClusteringOptions oldOptions) {
		set(oldOptions);
	}
	
	@Override
	public IClusteringOptions duplicate() {
		return new DefaultClusteringOptions(this);
	}

	@Override
	public boolean isIncludeSegment(UUID i) {
		if(!hasBoolean(i.toString()))
			return false;
		return getBoolean(i.toString()); // could be present, but false
	}

	@Override
	public Set<UUID> getSegments() {
		Set<UUID> result = new HashSet<>();
		for(String s : getBooleanKeys()) {
			if(XMLCreator.isUUID(s))
				result.add(UUID.fromString(s));
		}
		return result;
	}

	@Override
	public boolean isIncludeStatistic(Measurement stat) {
		if(!hasBoolean(stat.toString()))
			return false;
		return getBoolean(stat.toString());
	}


	@Override
	public boolean isIncludeProfile(ProfileType t) {
		if(!hasBoolean(t.toString()))
			return false;
		return getBoolean(t.toString());
	}

	@Override
	public boolean isUseSimilarityMatrix() {
		if(!hasBoolean(USE_SIMILARITY_MATRIX_KEY))
			return false;
		return getBoolean(USE_SIMILARITY_MATRIX_KEY);
	}

	@Override
	public ClusteringMethod getClusteringMethod() {
		if(!hasString(CLUSTER_METHOD_KEY))
			return null;
		return ClusteringMethod.valueOf(getString(CLUSTER_METHOD_KEY));
	}

	@Override
	public int getClusterNumber() {
		if(!hasInt(MANUAL_CLUSTER_NUMBER_KEY))
			return 0;
		return getInt(MANUAL_CLUSTER_NUMBER_KEY);
	}

	@Override
	public HierarchicalClusterMethod getHierarchicalMethod() {
		if(!hasString(HIERARCHICAL_METHOD_KEY))
			return null;
		return HierarchicalClusterMethod.valueOf(getString(HIERARCHICAL_METHOD_KEY));
	}

	@Override
	public int getIterations() {
		if(!hasInt(EM_ITERATIONS_KEY))
			return 0;
		return getInt(EM_ITERATIONS_KEY);
	}

	@Override
	public boolean isIncludeMesh() {
		if(!hasBoolean(INCLUDE_MESH_KEY))
			return false;
		return getBoolean(INCLUDE_MESH_KEY);
	}

	@Override
	public String[] getOptions() {
		String[] options = null;

        if (this.getClusteringMethod().equals(ClusteringMethod.EM)) {
            options = new String[2];
            options[0] = "-I"; // max. iterations
            options[1] = String.valueOf(getIterations());
        }

        if (this.getClusteringMethod().equals(ClusteringMethod.HIERARCHICAL)) {
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
	public void setIncludeProfileType(ProfileType profileType, boolean b) {
		setBoolean(profileType.toString(), b);
	}

	@Override
	public void setIncludeMesh(boolean defaultIncludeMesh) {
		setBoolean(INCLUDE_MESH_KEY, defaultIncludeMesh);
	}

	@Override
	public void setIncludeStatistic(Measurement stat, boolean selected) {
		setBoolean(stat.toString(), selected);
	}

	@Override
	public void setIncludeSegment(UUID id, boolean selected) {
		setBoolean(id.toString(), selected);
	}

	@Override
	public void setClusteringMethod(ClusteringMethod hierarchical) {
		setString(CLUSTER_METHOD_KEY, hierarchical.name());
	}


}
