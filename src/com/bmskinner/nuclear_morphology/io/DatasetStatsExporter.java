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

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.Taggable;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
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
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private boolean includeProfiles = true;
    private boolean includeSegments = false;
    private int segCount = 0;

    /**
     * Create specifying the folder stats will be exported into
     * 
     * @param folder
     */
    public DatasetStatsExporter(@NonNull File file, @NonNull List<IAnalysisDataset> list) {
        super(file, list);
        segCount = list.get(0).getCollection().getProfileManager().getSegmentCount();
        if(list.size()==1){
            includeSegments = true;
        } else {
            includeSegments = list.stream().allMatch(d->d.getCollection().getProfileManager().getSegmentCount()==segCount);
        }
    }

    /**
     * Create specifying the folder stats will be exported into
     * 
     * @param folder
     */
    public DatasetStatsExporter(@NonNull File file, @NonNull IAnalysisDataset dataset) {
        super(file, dataset);
        includeSegments = true;
    }

    /**
     * Write a column header line to the StringBuilder. Only nuclear stats for
     * now
     * 
     * @param outLine
     */
    @Override
	protected void appendHeader(@NonNull StringBuilder outLine) {

        outLine.append("Dataset\tCellID\tComponent\tFolder\tImage\tCentre_of_mass\t");

        for (PlottableStatistic s : PlottableStatistic.getNucleusStats()) {

            String label = s.label(MeasurementScale.PIXELS).replace(" ", "_").replace("\\(", "_")
                    .replace("\\)", "").replace("__", "_");
            outLine.append(label + TAB);

            if (!s.isDimensionless() && !s.isAngle()) { // only give micron
                                                        // measurements when
                                                        // length or area

                label = s.label(MeasurementScale.MICRONS).replace(" ", "_").replace("\\(", "_")
                        .replace("\\)", "").replace("__", "_");

                outLine.append(label + TAB);
            }

        }

        if (includeProfiles) {
            for (ProfileType type : ProfileType.exportValues()) {
                String label = type.toString().replace(" ", "_");
                for (int i = 0; i < 100; i++) {
                    outLine.append(label + "_" + i + TAB);
                }
            }
            // Frankenprofile separately
            for (int i = 0; i < 100; i++) {
                outLine.append("Franken_profile_" + i + TAB);
            }
        }
        
        if(includeSegments){
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
     * @throws UnavailableBorderTagException
     * @throws UnavailableProfileTypeException
     * @throws ProfileException
     */
    @Override
	protected void append(@NonNull IAnalysisDataset d, @NonNull StringBuilder outLine) throws Exception {
    	ISegmentedProfile medianProfile = d.getCollection().getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN); 
        for (ICell cell : d.getCollection().getCells()) {

            if (cell.hasNucleus()) {

                for (Nucleus n : cell.getNuclei()) {

                    outLine.append(d.getName() + TAB)
                        .append(cell.getId() + TAB)
                        .append(CellularComponent.NUCLEUS+"_" + n.getNameAndNumber() + TAB)
                        .append(n.getSourceFolder() + TAB)
                        .append(n.getSourceFileName() + TAB)
                        .append(n.getOriginalCentreOfMass().toString() + TAB);
                    appendNucleusStats(outLine, d, n);

                    if (includeProfiles) {
                        appendProfiles(outLine, n);
                        appendFrankenProfiles(outLine, n, medianProfile);
                    }
                    
                    if(includeSegments){
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

        for (PlottableStatistic s : PlottableStatistic.getNucleusStats()) {
            double varP = 0;
            double varM = 0;

            if (s.equals(PlottableStatistic.VARIABILITY)) {

                try {
                    varP = d.getCollection().getNormalisedDifferenceToMedian(Tag.REFERENCE_POINT, (Taggable) c);
                    varM = varP;
                } catch (UnavailableBorderTagException e) {
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
    }

    
    /**
     * Generate and append profiles for a component
     * @param outLine the string builder to append to
     * @param c the component to export
     * @throws UnavailableBorderTagException
     * @throws UnavailableProfileTypeException
     * @throws ProfileException
     */
    private void appendProfiles(StringBuilder outLine, Taggable c)
            throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException {
        for (ProfileType type : ProfileType.exportValues()) {

            IProfile p = c.getProfile(type, Tag.REFERENCE_POINT);

            for (int i = 0; i < 100; i++) {
                double idx = ((double) i) / 100d;

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
     * @throws UnavailableBorderTagException
     * @throws UnavailableProfileTypeException
     * @throws ProfileException
     */
    private void appendFrankenProfiles(StringBuilder outLine, Taggable c, ISegmentedProfile median)
            throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException {

            ISegmentedProfile s = c.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
            ISegmentedProfile f = s.frankenNormaliseToProfile(median);
            
            for (int i = 0; i < 100; i++) {
                double idx = ((double) i) / 100d;

                double value = f.get(idx);
                outLine.append(value + TAB);
            }
    }
    
    private void appendSegments(StringBuilder outLine, Taggable c)
            throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException {
        
        double varP = 0;
        double varM = 0;
                
        ISegmentedProfile p = c.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
        ISegmentedProfile normalisedProfile = p.interpolate(1000); // Allows point indexes
        List<IBorderSegment> segs = p.getOrderedSegments();
        
        for(IBorderSegment segment : segs){
            if (segment != null) {
            	// Add the length of the segment
                int indexLength = segment.length();
                double fractionOfPerimeter = (double) indexLength / (double) segment.getProfileLength();
                varP = fractionOfPerimeter * c.getStatistic(PlottableStatistic.PERIMETER, MeasurementScale.PIXELS);
                varM = fractionOfPerimeter * c.getStatistic(PlottableStatistic.PERIMETER, MeasurementScale.MICRONS);
                outLine.append(varP + TAB);
                outLine.append(varM + TAB);
                
                // Add the index of the segment start and end in the normalised profile.
                try {
                	IBorderSegment normalisedSeg = normalisedProfile.getSegment(segment.getID());
                	int start = normalisedSeg.getStartIndex();
                	int end   = normalisedSeg.getEndIndex();
                	outLine.append(start + TAB);
                    outLine.append(end + TAB);
				} catch (UnavailableComponentException e) {
					outLine.append("NA" + TAB);
                    outLine.append("NA" + TAB);
				}
            }
        }
    }
}
