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
package com.bmskinner.nuclear_morphology.io;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.image.GLCM.GLCMParameter;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.Taggable;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.MissingComponentException;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * Export all the stats from a dataset to a text file for downstream analysis
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class DatasetStatsExporter extends StatsExporter {
	
	private static final Logger LOGGER = Logger.getLogger(DatasetStatsExporter.class.getName());

    private boolean isIncludeProfiles = true;
    private boolean isIncludeSegments = false;
    private boolean isIncludeGlcm = false;
    
    /** How many samples should be taken from each profile? */
    private int profileSamples = 100;
    private int segCount = 0;
    
    /** The default length to which profiles should be normalised */
    private static final int DEFAULT_PROFILE_LENGTH = 1000;
    
    /** The length to which profiles should be normalised */
    private final int normProfileLength;
    

    /**
     * Create specifying the folder stats will be exported into
     * 
     * @param folder
     */
    public DatasetStatsExporter(@NonNull File file, @NonNull List<IAnalysisDataset> list, HashOptions options) {
        super(file, list);
        segCount = list.get(0).getCollection().getProfileManager().getSegmentCount();
        if(list.size()==1){
            isIncludeSegments = true;
        } else {
            isIncludeSegments = list.stream().allMatch(d->d.getCollection().getProfileManager().getSegmentCount()==segCount);
        }
        profileSamples = options.getInt(Io.PROFILE_SAMPLES_KEY);
        
        // Only include if present in all datasets
        isIncludeGlcm = list.stream()
        		.allMatch(d->d.getCollection().getCells().stream()
        				.allMatch(c->c.getPrimaryNucleus().hasStatistic(GLCMParameter.SUM.toStat())));
        
        normProfileLength = chooseNormalisedProfileLength();
    }

    /**
     * Create specifying the folder stats will be exported into
     * 
     * @param folder
     */
    public DatasetStatsExporter(@NonNull File file, @NonNull IAnalysisDataset dataset, HashOptions options) {
        super(file, dataset);
        segCount = dataset.getCollection().getProfileManager().getSegmentCount();
        isIncludeSegments = true;
        profileSamples = options.getInt(Io.PROFILE_SAMPLES_KEY);
        
        isIncludeGlcm = dataset.getCollection().getCells().stream()
        				.allMatch(c->c.getPrimaryNucleus().hasStatistic(GLCMParameter.SUM.toStat()));
        
        normProfileLength = chooseNormalisedProfileLength();
    }

    /**
     * Write a column header line to the StringBuilder. Only nuclear stats for
     * now
     * 
     * @param outLine
     */
    @Override
	protected void appendHeader(@NonNull StringBuilder outLine) {

        outLine.append("Dataset\tFile\tCellID\tComponent\tFolder\tImage\tCentre_of_mass\t");

        for (Measurement s : Measurement.getNucleusStats()) {

            String label = s.label(MeasurementScale.PIXELS)
            		.replace(" ", "_")
            		.replace("(", "_")
                    .replace(")", "")
                    .replace("__", "_");
            outLine.append(label + TAB);

            if (!s.isDimensionless() && !s.isAngle()) { // only give micron
                                                        // measurements when
                                                        // length or area

                label = s.label(MeasurementScale.MICRONS)
                		.replace(" ", "_")
                		.replace("(", "_")
                        .replace(")", "")
                        .replace("__", "_");

                outLine.append(label + TAB);
            }

        }
        
        if(isIncludeGlcm) {
        	for (Measurement s : Measurement.getGlcmStats()) {
        		String label = s.label(MeasurementScale.PIXELS).replace(" ", "_").replace("__", "_");
                outLine.append("GLCM_"+label + TAB);
        	}
        }

        if (isIncludeProfiles) {
            for (ProfileType type : ProfileType.exportValues()) {
                String label = type.toString().replace(" ", "_");
                for (int i = 0; i < profileSamples; i++) {
                    outLine.append(label + "_" + i + TAB);
                }
            }
            // Frankenprofile separately
            for (int i = 0; i < profileSamples; i++) {
                outLine.append("Franken_profile_" + i + TAB);
            }
        }
        
        if(isIncludeSegments){
            String label = "Length_seg_";
            
            for (int i = 0; i < segCount; i++) { 
                outLine.append(label + i +"_pixels" + TAB);
                outLine.append(label + i +"_microns" + TAB);
                outLine.append("Seg_" + i +"_start" + TAB);
                outLine.append("Seg_" + i +"_end" + TAB);
            }
        }
        
        // remove the final tab character
        if (outLine.length() > 0)
            outLine.setLength(outLine.length() - 1);
        
        outLine.append(NEWLINE);
    }


    /**
     * Append the given dataset stats into the string builder
     * @param d the dataset to export
     * @param outLine the string builder to append to
     * @throws MissingLandmarkException
     * @throws MissingProfileException
     * @throws ProfileException
     */
    @Override
	protected void append(@NonNull IAnalysisDataset d, @NonNull StringBuilder outLine) throws Exception {
    	ISegmentedProfile medianProfile = d.getCollection().getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN); 
        for (ICell cell : d.getCollection().getCells()) {

            if (cell.hasNucleus()) {

                for (Nucleus n : cell.getNuclei()) {

                    outLine.append(d.getName() + TAB)
                    	.append(d.getSavePath() + TAB)
                        .append(cell.getId() + TAB)
                        .append(CellularComponent.NUCLEUS+"_" + n.getNameAndNumber() + TAB)
                        .append(n.getSourceFolder() + TAB)
                        .append(n.getSourceFileName() + TAB)
                        .append(n.getOriginalCentreOfMass().toString() + TAB);
                    appendNucleusStats(outLine, d, n);
                    
                    

                    if (isIncludeProfiles) {
                        appendProfiles(outLine, n);
                        appendFrankenProfiles(outLine, n, medianProfile);
                    }
                    
                    if(isIncludeSegments){
                        appendSegments(outLine, n);
                    }
                    
                    // Remove final tab 
                    if (outLine.length() > 0)
                        outLine.setLength(outLine.length() - 1);

                    outLine.append(NEWLINE);
                }

            }

        }
    }

    private void appendNucleusStats(StringBuilder outLine, IAnalysisDataset d, CellularComponent c) {

        for (Measurement s : Measurement.getNucleusStats()) {
            double varP = 0;
            double varM = 0;
            
            if(!c.hasStatistic(s))
            	continue;

            if (s.equals(Measurement.VARIABILITY)) {

                try {
                    varP = d.getCollection().getNormalisedDifferenceToMedian(Landmark.REFERENCE_POINT, (Taggable) c);
                    varM = varP;
                } catch (MissingLandmarkException e) {
                    LOGGER.log(Loggable.STACK, "Tag not present in component", e);
                    varP = -1;
                    varM = -1;
                }
            } else {
                varP = c.getStatistic(s, MeasurementScale.PIXELS);
                varM = c.getStatistic(s, MeasurementScale.MICRONS);
            }

            outLine.append(varP + TAB);
            if (!s.isDimensionless() && !s.isAngle()) {
                outLine.append(varM + TAB);
            }
        }
        
        if(isIncludeGlcm) {
        	for (Measurement s : Measurement.getGlcmStats()) {
        		outLine.append(c.getStatistic(s) + TAB);
        	}
        }
    }

    
    /**
     * Generate and append profiles for a component
     * @param outLine the string builder to append to
     * @param c the component to export
     * @throws MissingLandmarkException
     * @throws MissingProfileException
     * @throws ProfileException
     */
    private void appendProfiles(StringBuilder outLine, Taggable c)
            throws MissingLandmarkException, MissingProfileException, ProfileException {
        for (ProfileType type : ProfileType.exportValues()) {

            IProfile p = c.getProfile(type, Landmark.REFERENCE_POINT);

            for (int i = 0; i < profileSamples; i++) {
                double idx = ((double) i) / (double)profileSamples;

                double value = p.get(idx);
                outLine.append(value + TAB);
            }
        }
    }
    
    /**
     * Generate and append a franken profile for the given median
     * @param outLine the string builder to append to
     * @param c the component to export
     * @param median the dataset median profile from which the component came
     * @throws MissingLandmarkException
     * @throws MissingProfileException
     * @throws ProfileException
     */
    private void appendFrankenProfiles(StringBuilder outLine, Taggable c, ISegmentedProfile median)
            throws MissingLandmarkException, MissingProfileException, ProfileException {

            ISegmentedProfile s = c.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);
            ISegmentedProfile f = s.frankenNormaliseToProfile(median);
            
            for (int i = 0; i < profileSamples; i++) {
                double idx = ((double) i) / (double)profileSamples;

                double value = f.get(idx);
                outLine.append(value + TAB);
            }
    }
    
    private void appendSegments(StringBuilder outLine, Taggable c)
            throws MissingLandmarkException, MissingProfileException, ProfileException {
        
        double varP = 0;
        double varM = 0;
                
        ISegmentedProfile p = c.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);
        ISegmentedProfile normalisedProfile = p.interpolate(normProfileLength); // Allows point indexes
        List<IProfileSegment> segs = p.getOrderedSegments();
        
        for(IProfileSegment segment : segs){
            if (segment != null) {
            	// Add the length of the segment
                int indexLength = segment.length();
                double fractionOfPerimeter = (double) indexLength / (double) segment.getProfileLength();
                varP = fractionOfPerimeter * c.getStatistic(Measurement.PERIMETER, MeasurementScale.PIXELS);
                varM = fractionOfPerimeter * c.getStatistic(Measurement.PERIMETER, MeasurementScale.MICRONS);
                outLine.append(varP + TAB);
                outLine.append(varM + TAB);
                
                // Add the index of the segment start and end in the normalised profile.
                try {
                	IProfileSegment normalisedSeg = normalisedProfile.getSegment(segment.getID());
                	int start = normalisedSeg.getStartIndex();
                	int end   = normalisedSeg.getEndIndex();
                	outLine.append(start + TAB);
                    outLine.append(end + TAB);
				} catch (MissingComponentException e) {
					outLine.append("NA" + TAB);
                    outLine.append("NA" + TAB);
				}
            }
        }
    }
    
    /**
	 * When handling large objects, the default normalised profile
	 * length may not be sufficient. Ensure the normalised length
	 * is a multiple of DEFAULT_PROFILE_LENGTH and greater than any 
	 * individual profile.
	 * @return
	 * @throws MissingProfileException
	 */
	private int chooseNormalisedProfileLength() {
		int profileLength = DEFAULT_PROFILE_LENGTH;

		try {
			for(IAnalysisDataset d : datasets) {
				for(Nucleus n : d.getCollection().getNuclei()) {
					int l = n.getProfile(ProfileType.ANGLE).size();
					if(l > profileLength)
						profileLength = (int) Math.ceil(l/DEFAULT_PROFILE_LENGTH)*DEFAULT_PROFILE_LENGTH;
				}
			}
		} catch(MissingProfileException e) {
			LOGGER.log(Loggable.STACK, "Unable to get profile: "+e.getMessage(), e);
			LOGGER.fine("Unable to get a profile, defaulting to default profile length of "+DEFAULT_PROFILE_LENGTH);
		}
		return profileLength;
	}
}
