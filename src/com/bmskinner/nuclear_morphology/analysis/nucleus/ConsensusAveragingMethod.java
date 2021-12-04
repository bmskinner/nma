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
package com.bmskinner.nuclear_morphology.analysis.nucleus;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.ComponentMeasurer;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.MissingComponentException;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.nuclei.Consensus;
import com.bmskinner.nuclear_morphology.components.nuclei.DefaultConsensusNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusFactory;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.UnprofilableObjectException;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * This method refolds the consensus nucleus based on averaging the positions of
 * equally spaced points around the perimeter of each vertical nucleus in the
 * dataset.
 * 
 * @author ben
 * @since 1.13.5
 *
 */
public class ConsensusAveragingMethod extends SingleDatasetAnalysisMethod {
	
	private static final Logger LOGGER = Logger.getLogger(ConsensusAveragingMethod.class.getName());
	
	private static final String EMPTY_FILE = "Empty";

	/** This length was chosen to avoid issues copying segments */
    private static final double PROFILE_LENGTH = 1000d;

    public ConsensusAveragingMethod(@NonNull final IAnalysisDataset dataset) {
        super(dataset);
    }

    @Override
    public IAnalysisResult call() throws Exception {
        run();
        return new DefaultAnalysisResult(dataset);
    }

    private void run() throws MissingComponentException, UnprofilableObjectException, 
    ComponentCreationException, ProfileException, MissingOptionException {
    	LOGGER.fine("Running consensus averaging on "+dataset.getName());
    	List<IPoint> border = getPointAverage();
    	Consensus refoldNucleus = makeConsensus(border);
    	dataset.getCollection().setConsensus(refoldNucleus);
    }
    
    /**
     * If scale is given in the dataset options, get it
     * @param n
     */
    private double getScale() {
    	// Set the nucleus scale if available
        Optional<IAnalysisOptions> analysisOptions =  dataset.getAnalysisOptions();
        if(analysisOptions.isPresent()) {
        	Optional<HashOptions> nucleusOptions = analysisOptions.get().getNucleusDetectionOptions();
        	if(nucleusOptions.isPresent()) {
        		return nucleusOptions.get().getDouble(HashOptions.SCALE);
        	} else {
        		LOGGER.fine("No nucleus detection options present, unable to find pixel scale for consensus");
        	}
        } else {
        	LOGGER.fine("No analysis options present, unable to find pixel scale for consensus");
        }
        return -1;
    }

   
    /**
     * Set the landmarks from the profile collection
     * to the nucleus.
     * @param n
     * @throws ProfileException 
     * @throws MissingProfileException 
     * @throws MissingLandmarkException 
     */
    private void setLandmarks(Nucleus n) throws MissingLandmarkException, MissingProfileException, ProfileException {
    	// Add all landmarks from the profile collection
    	// This will include any not present inthe ruleset collection that
    	// were added manually
    	for (Landmark l : dataset.getCollection().getProfileCollection().getLandmarks()) {
    		IProfile median = dataset.getCollection()
    				.getProfileCollection()
    				.getProfile(ProfileType.ANGLE, l, Stats.MEDIAN);
			int newIndex = n.getProfile(ProfileType.ANGLE).findBestFitOffset(median);
			LOGGER.finer(()->String.format("Setting %s in consensus to %s ", l, newIndex));
			n.setLandmark(l, newIndex);
    	}
    	
    }
    
    /**
     * Set the segments from the profile collection
     * to the nucleus.
     * @param n
     * @throws ProfileException 
     * @throws MissingProfileException 
     * @throws MissingLandmarkException 
     */
    private void setSegments(Nucleus n) throws MissingLandmarkException, MissingProfileException, ProfileException {
    	// Add segments to the new nucleus profile
        if(dataset.getCollection().getProfileCollection().hasSegments()) {
        	ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);
        	List<IProfileSegment> segs = dataset.getCollection().getProfileCollection().getSegments(Landmark.REFERENCE_POINT);
        	List<IProfileSegment> newSegs = IProfileSegment.scaleSegments(segs, profile.size());
        	LOGGER.finest(profile.toString());
        	for(IProfileSegment s : segs)
        		LOGGER.finest(s.toString());
        	for(IProfileSegment s : newSegs)
        		LOGGER.finest(s.toString());
        	profile.setSegments(newSegs);
        	n.setSegments(Landmark.REFERENCE_POINT, profile);
        }
    }
    
    /**
     * Create the consensus nucleus
     * @param n
     * @throws ProfileException 
     * @throws MissingProfileException 
     * @throws MissingLandmarkException 
     */
    private Consensus makeConsensus(List<IPoint> list)
            throws UnprofilableObjectException, ComponentCreationException,
            MissingLandmarkException, ProfileException, MissingProfileException, MissingOptionException {

    	// Create a nucleus with the same rulesets as the dataset
        IAnalysisOptions op = dataset.getAnalysisOptions().orElseThrow(MissingOptionException::new);
        Nucleus n = new NucleusFactory(op.getRuleSetCollection(), op.getProfileWindowProportion(), getScale())
        		.new NucleusBuilder()
        		.fromPoints(list)
        		.withFile(new File(EMPTY_FILE))
        		.withCoM(IPoint.makeNew(0, 0))
        		.withChannel(0)
        		.withMeasurement(Measurement.PERIMETER, ComponentMeasurer.calculatePerimeter(list))
        		.build();

        double area = ComponentMeasurer.calculate(Measurement.AREA, n);
        n.setStatistic(Measurement.AREA, area);

        // Add landmarks and segments from the profile collection
        setLandmarks(n);
        setSegments(n);
        
        // Build a consensus nucleus from the template points
        Consensus cons = new DefaultConsensusNucleus(n);

        // TODO: make this use the new orientation scheme (left or right, up or down)
        if (cons.getBorderPoint(Landmark.REFERENCE_POINT).isRightOf(cons.getCentreOfMass()))
        	cons.flipHorizontal();
        
        // Calculate any other stats that need the vertical alignment
        cons.getOrientedNucleus();
        return cons;
    }

    private List<IPoint> getPointAverage() {

        final Map<Double, List<IPoint>> perimeterPoints = new HashMap<>();

        IPoint zeroCoM = IPoint.makeNew(0, 0);
        dataset.getCollection().getNuclei().forEach(n -> {
            try {
                
                Nucleus v = n.getOrientedNucleus();
                v.moveCentreOfMass(zeroCoM);
                IProfile p = v.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);

                for (int i = 0; i < PROFILE_LENGTH; i++) {

                    double fractionOfPerimeter = i/ PROFILE_LENGTH;

                    if (perimeterPoints.get(fractionOfPerimeter) == null)
                    	perimeterPoints.put(fractionOfPerimeter, new ArrayList<>());
                    List<IPoint> list = perimeterPoints.get(fractionOfPerimeter);

                    int indexInProfile = p.getIndexOfFraction(fractionOfPerimeter);
                    int borderIndex    = v.getIndexRelativeTo(Landmark.REFERENCE_POINT, indexInProfile);
                    IPoint point = v.getBorderPoint(borderIndex);
                    list.add(point);
                }
            } catch (Exception e1) {
                LOGGER.log(Loggable.STACK, "Error getting perimeter point on nucleus " + n.getNameAndNumber(), e1);
            }

        });

        
        // Avoid errors in border calculation due to identical points by
        // checking each average point in the list is different to the 
        // previous. Needed since we have a large profile length.
        List<IPoint> averagedPoints = new ArrayList<>();
        for (int i = 0; i < PROFILE_LENGTH; i++) {
            double d = i/ PROFILE_LENGTH;
            List<IPoint> list = perimeterPoints.get(d);
            IPoint avg = calculateMedianPoint(list);

            if(averagedPoints.isEmpty() || !averagedPoints.get(averagedPoints.size()-1).equals(avg)) {
            	averagedPoints.add(avg);
            }
            fireProgressEvent();
        }
        return averagedPoints;
    }

    /**
     * Find the point with the median x and y coordinate of the given points
     * @param list
     * @return
     */
    private IPoint calculateMedianPoint(List<IPoint> list) {
        double[] xpoints = new double[list.size()];
        double[] ypoints = new double[list.size()];

        for (int i = 0; i < list.size(); i++) {
            IPoint p = list.get(i);

            xpoints[i] = p.getX();
            ypoints[i] = p.getY();
        }

        double xMed = Stats.quartile(xpoints, Stats.MEDIAN);
        double yMed = Stats.quartile(ypoints, Stats.MEDIAN);

        return IPoint.makeNew(xMed, yMed);
    }
}
