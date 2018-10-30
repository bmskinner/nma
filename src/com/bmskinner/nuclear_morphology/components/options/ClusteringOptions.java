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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

/**
 * First development of clustering options
 * @author ben
 * @deprecated since 1.14.0
 *
 */
@Deprecated
public class ClusteringOptions implements IClusteringOptions {

    private static final long         serialVersionUID = 1L;
    private ClusteringMethod          type;
    private int                       clusterNumber;
    private HierarchicalClusterMethod hierarchicalMethod;
    private int                       iterations;
    private boolean                   autoClusterNumber;
    private boolean                   includeMesh;

    private Map<PlottableStatistic, Boolean> statMap    = new HashMap<PlottableStatistic, Boolean>();
    private Map<UUID, Boolean>               segmentMap = new HashMap<UUID, Boolean>();              // which
                                                                                                     // segments
                                                                                                     // should
                                                                                                     // be
                                                                                                     // included

    private boolean includeProfile; // should the nuclear profiles be a part of
                                    // the clustering?

    private ProfileType profileType;

    private transient boolean useSimilarityMatrix;

    /**
     * Create a new set of options based on the given method.
     * 
     * @param type
     */
    public ClusteringOptions(ClusteringMethod type) {
        this.type = type;
        for (PlottableStatistic stat : PlottableStatistic.getRoundNucleusStats()) {
            statMap.put(stat, false);
        }
    }

    /**
     * Copy the options from an existing object
     * 
     * @param oldOptions
     */
    public ClusteringOptions(IClusteringOptions oldOptions) {
        this.type = oldOptions.getType();
        this.hierarchicalMethod = oldOptions.getHierarchicalMethod();
        this.useSimilarityMatrix = oldOptions.isUseSimilarityMatrix();
        this.includeProfile = oldOptions.isIncludeProfile();

        for (PlottableStatistic stat : PlottableStatistic.getRoundNucleusStats()) {
            statMap.put(stat, oldOptions.isIncludeStatistic(stat));
        }

        for (UUID i : oldOptions.getSegments()) {
            segmentMap.put(i, oldOptions.isIncludeSegment(i));
        }

        this.profileType = oldOptions.getProfileType();
        this.includeMesh = oldOptions.isIncludeMesh();

    }
    
    @Override
	public IClusteringOptions duplicate() {
		return new DefaultClusteringOptions(this);
	}

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#
     * isIncludeSegment(java.util.UUID)
     */
    @Override
    public boolean isIncludeSegment(UUID i) {
        if (this.segmentMap.containsKey(i)) {
            return this.segmentMap.get(i);
        }
		return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#
     * useSegments()
     */
//    @Override
//    public boolean useSegments() {
//        if (this.segmentMap.isEmpty()) {
//            return false;
//        } else {
//            return true;
//        }
//    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#
     * getSegments()
     */
    @Override
    public Set<UUID> getSegments() {
        return segmentMap.keySet();
    }

    @Override
	public void setIncludeSegment(UUID id, boolean b) {
        this.segmentMap.put(id, b);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#
     * isIncludeStatistic(com.bmskinner.nuclear_morphology.components.stats.
     * PlottableStatistic)
     */
    @Override
    public boolean isIncludeStatistic(PlottableStatistic stat) {
        if (this.statMap.containsKey(stat)) {
            return this.statMap.get(stat);
        } else {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#
     * getSavedStatistics()
     */
//    @Override
//    public Set<PlottableStatistic> getSavedStatistics() {
//        return statMap.keySet();
//    }

    @Override
	public void setIncludeStatistic(PlottableStatistic stat, boolean b) {
        this.statMap.put(stat, b);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#
     * isIncludeProfile()
     */
    @Override
    public boolean isIncludeProfile() {
        return this.includeProfile;
    }

    @Override
	public void setIncludeProfile(boolean b) {
        this.includeProfile = b;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#
     * isUseSimilarityMatrix()
     */
    @Override
    public boolean isUseSimilarityMatrix() {
        return useSimilarityMatrix;
    }

    @Override
	public void setUseSimilarityMatrix(boolean useSimilarityMatrix) {
        this.useSimilarityMatrix = useSimilarityMatrix;
    }

    /**
     * Set the clustering method
     * 
     * @param type
     */
    @Override
	public void setType(ClusteringMethod type) {
        this.type = type;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#
     * getType()
     */
    @Override
    public ClusteringMethod getType() {
        return type;
    }

    /**
     * Set the number of clusters to find automatically
     * 
     * @param autoClusterNumber
     */
    public void setAutoClusterNumber(boolean autoClusterNumber) {
        this.autoClusterNumber = autoClusterNumber;
    }

    /**
     * Set the number of hierarchical clusters to return. Has no effect if
     * clustering type is EM
     * 
     * @param clusterNumber
     */
    @Override
	public void setClusterNumber(int clusterNumber) {
        this.clusterNumber = clusterNumber;
    }

    @Override
	public void setHierarchicalMethod(HierarchicalClusterMethod hierarchicalMethod) {
        this.hierarchicalMethod = hierarchicalMethod;
    }

    /**
     * Set the number of iterations to run an EM clusterer. Has no effect if
     * type is hierarchical
     * 
     * @param iterations
     */
    @Override
	public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#
     * getClusterNumber()
     */
    @Override
    public int getClusterNumber() {
        return clusterNumber;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#
     * getHierarchicalMethod()
     */
    @Override
    public HierarchicalClusterMethod getHierarchicalMethod() {
        return hierarchicalMethod;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#
     * getIterations()
     */
    @Override
    public int getIterations() {
        return iterations;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#
     * getProfileType()
     */
    @Override
    public ProfileType getProfileType() {
        return profileType;
    }

    @Override
	public void setProfileType(ProfileType profileType) {
        this.profileType = profileType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#
     * isIncludeMesh()
     */
    @Override
    public boolean isIncludeMesh() {
        return includeMesh;
    }

    @Override
	public void setIncludeMesh(boolean includeMesh) {
        this.includeMesh = includeMesh;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#
     * getOptions()
     */
    @Override
    public String[] getOptions() {

        String[] options = null;

        if (this.type.equals(ClusteringMethod.EM)) {
            options = new String[2];
            options[0] = "-I"; // max. iterations
            options[1] = String.valueOf((Integer) iterations);
        }

        if (this.type.equals(ClusteringMethod.HIERARCHICAL)) {
            options = new String[4];
            options[0] = "-N"; // number of clusters
            options[1] = String.valueOf((Integer) clusterNumber);
            options[2] = "-L"; // algorithm
            options[3] = hierarchicalMethod.code();
            // options[4] = "-P"; // print Newick Tree
            // options[5] = "";
        }

        return options;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (statMap == null) {
            statMap = new HashMap<PlottableStatistic, Boolean>();
            for (PlottableStatistic stat : PlottableStatistic.getRoundNucleusStats()) {
                statMap.put(stat, false);
            }
        }

        if (segmentMap == null) {
            segmentMap = new HashMap<UUID, Boolean>();
        }

        if (profileType == null) {
            profileType = ProfileType.ANGLE;
        }

        if (Boolean.valueOf(includeMesh) == null) {
            includeMesh = false;
        }
    }

    @Override
	public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(profileType);
        return sb.toString();
    }

	@Override
	public double getDouble(String s) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getInt(String s) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean getBoolean(String s) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setDouble(String s, double d) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setInt(String s, int i) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBoolean(String s, boolean b) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public float getFloat(String s) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setFloat(String s, float f) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getString(String s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setString(String k, String v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<String> getBooleanKeys() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getIntegerKeys() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getDoubleKeys() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getFloatKeys() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getStringKeys() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getKeys() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getEntries() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getValue(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void set(HashOptions o) {
		// TODO Auto-generated method stub
		
	}
}
