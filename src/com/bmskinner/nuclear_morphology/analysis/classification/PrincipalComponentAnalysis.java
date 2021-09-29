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
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.Taggable;
import com.bmskinner.nuclear_morphology.components.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.UnavailableProfileTypeException;

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
public class PrincipalComponentAnalysis extends SingleDatasetAnalysisMethod {
	
	private static final Logger LOGGER = Logger.getLogger(PrincipalComponentAnalysis.class.getName());
	
	private final HashOptions options;	
	
	public static final String PROPORTION_VARIANCE_KEY = "Variance";
	
	private final Map<Integer, UUID> nucleusToInstanceMap = new HashMap<>();
	
	public PrincipalComponentAnalysis(@NonNull IAnalysisDataset dataset, @NonNull HashOptions options) {
		super(dataset);
		this.options = options;
	}

	@Override
	public IAnalysisResult call() throws Exception {		
		Instances inst = createInstances();
		PrincipalComponents pca = new PrincipalComponents();
		pca.setVarianceCovered(options.getDouble(PROPORTION_VARIANCE_KEY));
		pca.buildEvaluator(inst);
		double var = pca.getVarianceCovered();
		LOGGER.fine("Variance covered: "+var);
		
		int expectedPcs = 0;
		
		double[] eigenValues = pca.getEigenValues();
		
		
		double totalEigenValues = Arrays.stream(eigenValues).sum();
		double[] varianceExplained = Arrays.stream(eigenValues)
				.map(d->d/totalEigenValues)
				.sorted()
				.toArray();
		LOGGER.fine("Variance explained by each eigenvector: "+Arrays.toString(varianceExplained));

		for(int i=0; i<inst.numInstances(); i++) {
			Instance instance = inst.instance(i);
			Instance converted = pca.convertInstance(instance);
			double[] values = converted.toDoubleArray();
			UUID nucleusId = nucleusToInstanceMap.get(i);
			Optional<Nucleus> nucl = dataset.getCollection().getNucleus(nucleusId);

			if(nucl.isPresent()) {
				nucl.get().setStatistic(Measurement.PCA_N, values.length); // Store the number of expected PCs
				// Store in the generic stats pool until assigned a cluster id by a clustering method
				for(int pc=0; pc<values.length; pc++) {
					int readableName = pc+1;
					Measurement stat = Measurement.makePrincipalComponent(readableName);
					nucl.get().setStatistic(stat, values[pc]);
				}
				if(i==0) {
					expectedPcs = values.length;
					LOGGER.fine("Detected number of PCs: "+expectedPcs);
				} else {
					if(values.length!=expectedPcs)
						LOGGER.fine("Different number of PCs: "+values.length);
				}
			} else 
				LOGGER.fine("No nucleus in collection for instance "+i+" with id "+nucleusId);
		}

		options.setInt(IClusteringOptions.NUM_PCS_KEY, expectedPcs);

		return new DefaultAnalysisResult(dataset);
	}
	  
	private FastVector createAttributes() {
		
		double profileWindow = Taggable.DEFAULT_PROFILE_WINDOW_PROPORTION;
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

        for (Measurement stat : Measurement.getNucleusStats()) {
            if (options.getBoolean(stat.toString())) {
                Attribute a = new Attribute(stat.toString());
                attributes.addElement(a);
            }
        }
        return attributes;
	}


    private Instances createInstances() throws ClusteringMethodException {

        double windowProportion = Taggable.DEFAULT_PROFILE_WINDOW_PROPORTION;
        if (dataset.hasAnalysisOptions())// Merged datasets may not have options
            windowProportion = dataset.getAnalysisOptions().get().getProfileWindowProportion();


        FastVector attributes = createAttributes();

        Instances instances = new Instances(dataset.getName(), 
        		attributes, 
        		dataset.getCollection().size());
        
        for(ICell c : dataset.getCollection()) {
        	for(Nucleus n : c.getNuclei()) {
        		try {
    				addNucleus(n, attributes, instances,  windowProportion);
    			} catch (UnavailableBorderTagException
    					| UnavailableProfileTypeException
    					| ProfileException e) {
    				LOGGER.log(Level.SEVERE, "Unable to add nucleus to instances",e);
    			}
        	}
        }
        return instances;

    }
    
    private void addNucleus(Nucleus n, FastVector attributes, Instances instances,
            double windowProportion) throws UnavailableBorderTagException, UnavailableProfileTypeException,
            ProfileException {

        Instance inst = new SparseInstance(attributes.size());

        int attNumber = 0;

        int pointsToSample = (int) Math.floor(1d / windowProportion);

        for(ProfileType t : ProfileType.displayValues()) {
        	if (options.getBoolean(t.toString())) {
        		IProfile p = n.getProfile(t, Landmark.REFERENCE_POINT);
        		for (int i = 0; i < pointsToSample; i++) {
        			Attribute att = (Attribute) attributes.elementAt(i);
        			inst.setValue(att, p.get(i * windowProportion));
        			attNumber++;
        		}
        	}
        }
        
        for (Measurement stat : Measurement.getNucleusStats()) {
        	
        	if (options.getBoolean(stat.toString())) {
        		Attribute att = (Attribute) attributes.elementAt(attNumber++);
        		inst.setValue(att, n.getStatistic(stat, MeasurementScale.MICRONS));
        	}
        }

        instances.add(inst);
        int index = instances.numInstances();
        nucleusToInstanceMap.put(index-1, n.getID());
        fireProgressEvent();
    }
	

}
