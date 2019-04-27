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
package com.bmskinner.nuclear_morphology.analysis.classification;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.Profileable;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

import weka.attributeSelection.PrincipalComponents;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

/**
 * An implementation of principle component analysis
 * @author ben
 * @since 1.14.0
 *
 */
public class PrincipleComponentAnalysis extends SingleDatasetAnalysisMethod {
	private final HashOptions options;
	
	private final Map<Integer, UUID> cellToInstanceMap = new HashMap<>();
	
	public PrincipleComponentAnalysis(@NonNull IAnalysisDataset dataset, @NonNull HashOptions options) {
		super(dataset);
		this.options = options;
	}

	@Override
	public IAnalysisResult call() throws Exception {		
		Instances inst = createInstances();
		PrincipalComponents pca = new PrincipalComponents();
		pca.buildEvaluator(inst);
		
		for(int i=0; i<inst.numInstances(); i++) {
			Instance instance = inst.instance(i);
			Instance converted = pca.convertInstance(instance);
			double[] values = converted.toDoubleArray();
			log(Arrays.toString(values));
			UUID nucleusId = cellToInstanceMap.get(i);
			Optional<Nucleus> nucl = dataset.getCollection().stream().flatMap(c->c.getNuclei().stream()).filter(n->n.getID().equals(nucleusId)).findFirst();
			if(nucl.isPresent()) {
				nucl.get().setStatistic(PlottableStatistic.PCA_1, values[0]);
				nucl.get().setStatistic(PlottableStatistic.PCA_2, values[1]);
			}
		}


		return new DefaultAnalysisResult(dataset);
	}
	  
	private FastVector createAttributes() {
		
		double profileWindow = Profileable.DEFAULT_PROFILE_WINDOW_PROPORTION;
		if (dataset.hasAnalysisOptions()) 
            profileWindow = dataset.getAnalysisOptions().get().getProfileWindowProportion();

        FastVector attributes = new FastVector();
        
        for(ProfileType t : ProfileType.displayValues()) {
        	if (options.getBoolean(t.toString())) {
        		int nProfileAtttributes = (int) Math.floor(1d / profileWindow);
        		for (int i = 0; i < nProfileAtttributes; i++) {
                    attributes.addElement(new Attribute(t.toString() + i));
                }
        	}
        }

        for (PlottableStatistic stat : PlottableStatistic.getNucleusStats(dataset.getCollection().getNucleusType())) {
            if (options.getBoolean(stat.toString())) {
                Attribute a = new Attribute(stat.toString());
                attributes.addElement(a);
            }
        }
        return attributes;
	}


    private Instances createInstances() throws ClusteringMethodException {

        double windowProportion = Profileable.DEFAULT_PROFILE_WINDOW_PROPORTION;
        if (dataset.hasAnalysisOptions())// Merged datasets may not have options
            windowProportion = dataset.getAnalysisOptions().get().getProfileWindowProportion();


        FastVector attributes = createAttributes();

        Instances instances = new Instances(dataset.getName(), attributes, dataset.getCollection().size());
        
        for(ICell c : dataset.getCollection()) {
        	for(Nucleus n : c.getNuclei()) {
        		try {
    				addNucleus(c, n, attributes, instances,  windowProportion);
    			} catch (UnavailableBorderTagException | UnavailableProfileTypeException | ProfileException e) {
    				warn("Unable to add nucleus to instances: "+e.getMessage());
    			}
        	}
        }
        return instances;

    }
    
    private void addNucleus(ICell c, Nucleus n, FastVector attributes, Instances instances,
            double windowProportion) throws UnavailableBorderTagException, UnavailableProfileTypeException,
            ProfileException {

        Instance inst = new SparseInstance(attributes.size());

        int attNumber = 0;

        int pointsToSample = (int) Math.floor(1d / windowProportion);

        for(ProfileType t : ProfileType.displayValues()) {
        	if (options.getBoolean(t.toString())) {
        		IProfile p = n.getProfile(t, Tag.REFERENCE_POINT);
        		for (int i = 0; i < pointsToSample; i++) {
        			Attribute att = (Attribute) attributes.elementAt(i);
        			inst.setValue(att, p.get(i * windowProportion));
        			attNumber++;
        		}
        	}
        }
        
        for (PlottableStatistic stat : PlottableStatistic.getNucleusStats(dataset.getCollection().getNucleusType())) {
        	if (options.getBoolean(stat.toString())) {
        		Attribute att = (Attribute) attributes.elementAt(attNumber++);
        		inst.setValue(att, n.getStatistic(stat, MeasurementScale.MICRONS));
        	}
        }

        instances.add(inst);
        int index = instances.numInstances();
        cellToInstanceMap.put(index, n.getID());
        fireProgressEvent();
    }
	

}
