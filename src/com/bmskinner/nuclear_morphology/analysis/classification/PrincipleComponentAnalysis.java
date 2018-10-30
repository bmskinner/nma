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

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.Profileable;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
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
public class PrincipleComponentAnalysis extends CellClusteringMethod {

	public PrincipleComponentAnalysis(@NonNull IAnalysisDataset dataset, @NonNull IClusteringOptions options) {
		super(dataset, options);
	}

	@Override
	public IAnalysisResult call() throws Exception {
		// TODO Auto-generated method stub
		
		Instances inst = makeInstances();
		PrincipalComponents pca = new PrincipalComponents();
		pca.buildEvaluator(inst);

		return null;
	}

	@Override
	protected FastVector makeAttributes() throws ClusteringMethodException{
        // Determine the number of attributes required
        int attributeCount = 0;
        int profileAttributeCount = 0;

        double profileWindow = Profileable.DEFAULT_PROFILE_WINDOW_PROPORTION;

        if (options.isIncludeProfile()) { // An attribute for each angle in the
                                          // median profile, spaced <windowSize>
                                          // apart          
            if (dataset.hasAnalysisOptions()) 
                profileWindow = dataset.getAnalysisOptions().get().getProfileWindowProportion();

            profileAttributeCount = (int) Math.floor(1d / profileWindow);
            attributeCount += profileAttributeCount;
        }

        for (PlottableStatistic stat : PlottableStatistic.getNucleusStats(collection.getNucleusType())) {
            if (options.isIncludeStatistic(stat))
                attributeCount++;
        }

        // Create the attributes
        FastVector attributes = new FastVector(attributeCount);

        if (options.isIncludeProfile()) {
            fine("Including profile " + options.getProfileType());
            for (int i = 0; i < profileAttributeCount; i++) {
                Attribute a = new Attribute("att_" + i);
                attributes.addElement(a);
            }
        }

        for (PlottableStatistic stat : PlottableStatistic.getNucleusStats(collection.getNucleusType())) {
            if (options.isIncludeStatistic(stat)) {
                Attribute a = new Attribute(stat.toString());
                attributes.addElement(a);
            }
        }
        return attributes;
    }

    @Override
	protected Instances makeInstances() throws ClusteringMethodException {

        double windowProportion = Profileable.DEFAULT_PROFILE_WINDOW_PROPORTION;
        if (dataset.hasAnalysisOptions())// Merged datasets may not have options
            windowProportion = dataset.getAnalysisOptions().get().getProfileWindowProportion();


        FastVector attributes = makeAttributes();

        Instances instances = new Instances(collection.getName(), attributes, collection.size());

        final double w = windowProportion;
         
        collection.getCells()
        .forEach( c->c.getNuclei().stream()
        		.forEach( n-> {
        			try {
        				addNucleus(c, n, attributes, instances,  w);
        			} catch (UnavailableBorderTagException | UnavailableProfileTypeException | ProfileException e) {
        				// TODO Auto-generated catch block
        				e.printStackTrace();
        			}
        		}));
        return instances;

    }
    
    private void addNucleus(ICell c, Nucleus n, FastVector attributes, Instances instances,
            double windowProportion) throws UnavailableBorderTagException, UnavailableProfileTypeException,
            ProfileException {

        Instance inst = new SparseInstance(attributes.size());

        int attNumber = 0;

        int pointsToSample = (int) Math.floor(1d / windowProportion);

        if (options.isIncludeProfile()) {

            // Interpolate the profile to the median length
            IProfile p = n.getProfile(options.getProfileType(), Tag.REFERENCE_POINT);

            for (int profileAtt = 0; profileAtt < pointsToSample; profileAtt++) {
                Attribute att = (Attribute) attributes.elementAt(profileAtt);
                inst.setValue(att, p.get(profileAtt * windowProportion));
                attNumber++;
            }

        }

        for (PlottableStatistic stat : PlottableStatistic.getNucleusStats(collection.getNucleusType())) {
            if (options.isIncludeStatistic(stat)) {
                Attribute att = (Attribute) attributes.elementAt(attNumber++);
                inst.setValue(att, n.getStatistic(stat, MeasurementScale.MICRONS));
            }
        }

        instances.add(inst);
        cellToInstanceMap.put(inst, c.getId());
        fireProgressEvent();
    }
	

}
