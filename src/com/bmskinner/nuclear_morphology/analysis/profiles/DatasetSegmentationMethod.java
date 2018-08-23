/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.analysis.profiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import com.bmskinner.nuclear_morphology.analysis.DatasetValidator;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.ProgressEvent;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfileCollection;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.SegmentedFloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment.SegmentUpdateException;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.stats.Stats;

public class DatasetSegmentationMethod extends SingleDatasetAnalysisMethod {

    private ICellCollection sourceCollection = null; // a collection to take
                                                     // segments from

    private MorphologyAnalysisMode mode = MorphologyAnalysisMode.NEW;

    public enum MorphologyAnalysisMode {
        NEW, COPY, REFRESH
    }

    /**
     * Segment a dataset with the given mode
     * 
     * @param dataset
     * @param mode
     * @param programLogger
     */
    public DatasetSegmentationMethod(IAnalysisDataset dataset, MorphologyAnalysisMode mode) {
        super(dataset);
        this.mode = mode;
    }

    /**
     * Copy the segmentation pattern from the source collection onto the given
     * dataset
     * 
     * @param dataset
     * @param source
     * @param programLogger
     */
    public DatasetSegmentationMethod(IAnalysisDataset dataset, ICellCollection source) {
        this(dataset, MorphologyAnalysisMode.COPY);
        this.sourceCollection = source;
    }

    @Override
    public IAnalysisResult call() throws Exception {

        try {

            switch (mode) {
            case COPY:
                result = runCopyAnalysis();
                break;
            case NEW:
                result = runNewAnalysis();
                break;
            case REFRESH:
                result = runRefreshAnalysis();
                break;
            default:
                result = null;
                break;
            }

            // Ensure segments are copied appropriately to verticals
            // Ensure hook statistics are generated appropriately
            // log("Updating verticals");
            for (Nucleus n : dataset.getCollection().getNuclei()) {
                // log("Updating "+n.getNameAndNumber());
                n.updateVerticallyRotatedNucleus();
                n.updateDependentStats();

            }
            fine("Updated verticals and stats");

        } catch (Exception e) {
            result = null;
            stack("Error in segmentation analysis", e);
        }

        return result;

    }

    /**
     * This has to do everything: segment the median, appy the segments to
     * nuclei, generate the frankenmedian, and adjust the nuclear segments based
     * on the frankenprofiles.
     * 
     * @return
     * @throws Exception
     */
    private IAnalysisResult runNewAnalysis() throws Exception {
        Tag pointType = Tag.REFERENCE_POINT;
        dataset.getCollection().setConsensus(null); // clear if present
        runSegmentation(dataset.getCollection(), pointType);
        
        if(dataset.hasChildren()){
        	DatasetValidator v = new DatasetValidator();
        	for(IAnalysisDataset child: dataset.getAllChildDatasets()){
        		child.getCollection().setConsensus(null);
        		dataset.getCollection().getProfileManager().copyCollectionOffsets(child.getCollection());
        	}
        	v.validate(dataset);
            for(String s : v.getErrors()){
                warn(s);
            }
        }

        return new DefaultAnalysisResult(dataset);
    }

    private IAnalysisResult runCopyAnalysis() throws Exception {
        if (sourceCollection == null) {
            warn("Cannot copy: source collection is null");
            return null;
        }
        
        if(!sourceCollection.getProfileCollection().hasSegments()) {
        	 fine("Cannot copy segments: source collection has no segments");
        	 dataset.getCollection().createProfileCollection(); // ensure profiles are set
        	 return new DefaultAnalysisResult(dataset);
        }
        
		fine("Copying segmentation pattern");
		reapplyProfiles(dataset.getCollection(), sourceCollection);
		fine("Copying complete");
		return new DefaultAnalysisResult(dataset);
    }

    private IAnalysisResult runRefreshAnalysis() throws Exception {
        refresh(dataset.getCollection());
        return new DefaultAnalysisResult(dataset);
    }

    /*
     * ////////////////////////////////////////////////// 
     * Analysis methods
     * //////////////////////////////////////////////////
     */

    /**
     * When a population needs to be reanalysed do not offset nuclei or
     * recalculate best fits; just get the new median profile
     * 
     * @param collection
     *            the collection of nuclei
     * @param sourceCollection
     *            the collection with segments to copy
     */
    public boolean reapplyProfiles(ICellCollection collection, ICellCollection sourceCollection) throws Exception {

        fine("Applying existing segmentation profile to population");

        sourceCollection.getProfileManager().copyCollectionOffsets(collection);

        // At this point the collection has only a regular profile collections.
        // No Frankenprofile has been copied.

        reviseSegments(collection, Tag.REFERENCE_POINT);

        fine("Re-profiling complete");
        return true;
    }

    /**
     * Refresh the given collection. Assumes that the segmentation in the
     * median profile is correct, and updates all nuclei to match
     * 
     * @param collection
     * @return
     */
    public boolean refresh(ICellCollection collection) throws Exception {
        Tag pointType = Tag.REFERENCE_POINT;
        assignMedianSegmentsToNuclei(collection, pointType);
        return true;
    }

    /**
     * Run the segmentation part of the analysis.
     * 
     * @param collection
     * @param pointType
     */
    private void runSegmentation(ICellCollection collection, Tag pointType) throws Exception {
        createSegmentsInMedian(collection);
        assignMedianSegmentsToNuclei(collection, pointType);
    }

    /**
     * Given a cell collection with a segmented median profile, apply the
     * segmentation pattern to the nuclei within the colleciton.
     * 
     * @param collection
     *            the collection
     * @param pointType
     *            the tag to begin from
     * @throws Exception
     */
    private void assignMedianSegmentsToNuclei(ICellCollection collection, Tag pointType) throws Exception {
    	fine("Applying segments from median profile to nuclei");
        // map the segments from the median directly onto the nuclei
        assignMedianSegmentsToNuclei(collection);
        
        int error = collection.getProfileManager().countNucleiNotMatchingMedianSegmentation();
        if(error>0){
            warn(String.format("%d nuclei have a different number of segments to the median before frankenprofiling", error));
        }

        // adjust the segments to better fit each nucleus
        fine("Frankenprofiling for improved segment fitting");
//        try {
            reviseSegments(collection, pointType);
//        } catch (Exception e) {
//            warn("Error revising segments");
//            stack("Error revising segments", e);
//        }
        
        error = collection.getProfileManager().countNucleiNotMatchingMedianSegmentation();
        if(error>0){
            warn(String.format("%d nuclei have a different number of segments to the median after frankenprofiling", error));
        }

        // update the aggregate in case any borders have changed
        collection.getProfileCollection().createProfileAggregate(collection,
                collection.getProfileCollection().length());

    }

    /**
     * Assign the segments in the median profile to the nuclei within the
     * collection
     * 
     * @param collection
     * @throws Exception
     */
    private void assignMedianSegmentsToNuclei(ICellCollection collection) throws Exception {
//        fine("Assigning segments to nuclei");
        IProfileCollection pc = collection.getProfileCollection();

        // find the corresponding point in each Nucleus
        ISegmentedProfile median = pc.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);

        /*
         * NEW CODE
         */

        collection.getNuclei().parallelStream().forEach(n -> {
            try {
                assignSegmentsToNucleus(median, n);
            } catch (ProfileException e) {
                warn("Error setting profile offsets in " + n.getNameAndNumber());
                stack(e.getMessage(), e);
            }
        });

        if (!checkRPmatchesSegments(collection))
            warn("Segments do not all start on reference point after offsetting");

    }

    /**
     * Assign the median segments to the nucleus, finding the best match of the
     * nucleus profile to the median profile
     * 
     * @param n
     *            the nucleus to segment
     * @param median
     *            the segmented median profile
     */
    private void assignSegmentsToNucleus(ISegmentedProfile median, Nucleus n) throws ProfileException {

        if (n.isLocked()) {
            return;
        }

        // remove any existing segments in the nucleus
        ISegmentedProfile nucleusProfile;
        try {
            nucleusProfile = n.getProfile(ProfileType.ANGLE);
        } catch (UnavailableProfileTypeException e1) {
            warn("Cannot get angle profile for nucleus");
            stack("Profile type angle is not available", e1);
            return;
        }
        nucleusProfile.clearSegments();

        List<IBorderSegment> nucleusSegments = new ArrayList<>();

        // go through each segment defined for the median curve
        IBorderSegment prevSeg = null;

        for (IBorderSegment segment : median.getSegments()) {

            // get the positions the segment begins and ends in the median
            // profile
            int startIndexInMedian = segment.getStartIndex();
            int endIndexInMedian = segment.getEndIndex();

            // find the positions these correspond to in the offset profiles

            // get the median profile, indexed to the start or end point
            IProfile startOffsetMedian = median.offset(startIndexInMedian);
            IProfile endOffsetMedian = median.offset(endIndexInMedian);

            try {

                // find the index at the point of the best fit
                int startIndex = n.getProfile(ProfileType.ANGLE).getSlidingWindowOffset(startOffsetMedian);
                int endIndex = n.getProfile(ProfileType.ANGLE).getSlidingWindowOffset(endOffsetMedian);

                IBorderSegment seg = IBorderSegment.newSegment(startIndex, endIndex, n.getBorderLength(),
                        segment.getID());
                if (prevSeg != null) {
                    seg.setPrevSegment(prevSeg);
                    prevSeg.setNextSegment(seg);
                }

                nucleusSegments.add(seg);

                prevSeg = seg;

            } catch (IllegalArgumentException | UnavailableProfileTypeException e) {
                warn("Cannot make segment");
                stack("Error making segment for nucleus " + n.getNameAndNumber(), e);
                break;

            }

        }

        IBorderSegment.linkSegments(nucleusSegments);
        nucleusProfile.setSegments(nucleusSegments);
        
        if(nucleusProfile.getSegmentCount()!=median.getSegmentCount())
            throw new ProfileException("Nucleus does not have the correct segment count");

        n.setProfile(ProfileType.ANGLE, nucleusProfile);
    }

    /**
     * Check if the reference point of the nuclear profiles is at a segment
     * boundary for all nuclei
     * 
     * @param collection
     * @return
     */
    private boolean checkRPmatchesSegments(ICellCollection collection) {
    	finer("Checking RP is at a segment boundary in all nuclei");
        return collection.getNuclei().stream().allMatch(n -> {
            try {
            	
            	// Profile with RP at zero
            	ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
            	boolean hit = false;
                for (IBorderSegment s : profile.getSegments()) {
                	hit |= s.getStartIndex()==0;
                }
                
                if (!hit) {
                	finer("Moving RP to segment boundary");
                    // The RP is not at the start of a segment
                    // Update the segment start to zero
                	IBorderSegment seg = profile.getSegmentContaining(0);
                	seg.update(0, seg.getEndIndex());
                	finer("Applying profile with updated RP: "+profile.toString());
                    n.setProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, profile);
                }
            	
//                boolean hit = false;
//                for (IBorderSegment s : n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT).getSegments()) {
//                	hit |= s.getStartIndex() == n.getBorderIndex(Tag.REFERENCE_POINT);
//                }
//                
//                if (!hit) {
//                    // The RP is not at the start of a segment
//                    // Update the segment boundary closest to the zero index RP
//                    int end = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT).getSegmentContaining(0).getSegmentAt(0).getEndIndex();
//
//                    ISegmentedProfile p = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
//
//                    p.getSegmentAt(0).update(0, end);
//
//                    n.setProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, p);
//                }
                return n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT).getSegmentAt(0).getStartIndex() == 0;
            } catch (UnavailableComponentException | ProfileException | SegmentUpdateException e) {
                warn("Error updating nucleus segment to RP ");
                stack(e);
                return false;
            }
        });
    }

    /**
     * Use a ProfileSegmenter to segment the regular median profile of the
     * collection starting from the reference point
     * 
     * @param collection
     */
    private void createSegmentsInMedian(ICellCollection collection) throws Exception {

    	fine("Creating segments in median profile");
        IProfileCollection pc = collection.getProfileCollection();

        // the reference point is always index 0, so the segments will match
        // the profile
        IProfile median = pc.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);

        List<Integer> map = new ArrayList<>();

        int opIndex = pc.getIndex(Tag.ORIENTATION_POINT);
        map.add(pc.getIndex(Tag.ORIENTATION_POINT));


        List<IBorderSegment> segments;
        if (!collection.getNucleusType().equals(NucleusType.ROUND)
                && !collection.getNucleusType().equals(NucleusType.NEUTROPHIL)) {

            ProfileSegmenter segmenter = new ProfileSegmenter(median, map);
            segments = segmenter.segment();

        } else {

            segments = new ArrayList<>(2);
            IBorderSegment seg1 = IBorderSegment.newSegment(0, opIndex, median.size());
            IBorderSegment seg2 = IBorderSegment.newSegment(opIndex, median.size() - 1, median.size());
            segments.add(seg1);
            segments.add(seg2);
        }

        pc.addSegments(Tag.REFERENCE_POINT, segments);

    }

    /**
     * Update segment assignments in individual nuclei by stretching each
     * segment to the best possible fit along the median profile
     * 
     * @param collection
     * @param pointType
     * @throws ProfileException 
     */
    private void reviseSegments(ICellCollection collection, Tag pointType) throws ProfileException {

        IProfileCollection pc = collection.getProfileCollection();

        try {
        	List<IBorderSegment> segments = pc.getSegments(pointType);

        	// Get the median profile for the population
        	ISegmentedProfile medianProfile = pc.getSegmentedProfile(ProfileType.ANGLE, pointType, Stats.MEDIAN);

        	SegmentFitter fitter = new SegmentFitter(medianProfile);
        	
        	collection.getNuclei().parallelStream().forEach(n->{

        		try {
        			if (! n.isLocked())
        				fitter.fit(n, pc);
        		} catch (IndexOutOfBoundsException | ProfileException | UnavailableComponentException
        				| UnsegmentedProfileException e) {
        			stack("Could not fit segments for nucleus "+n.getNameAndNumber()+": "+e.getMessage(), e);
        		} finally {
        			fireProgressEvent();
        		}
        		
        	});

        } catch (UnavailableBorderTagException e1) {
        	error("Unavailable border tag in segment recombining task: " + e1.getMessage(), e1);
        } catch (UnavailableProfileTypeException e1) {
			error("Unavailable profile type in segment recombining task: " + e1.getMessage(), e1);
		} catch (UnsegmentedProfileException e1) {
			error("Unsegmented profile in segment recombining task: " + e1.getMessage(), e1);
		}

        pc.createProfileAggregate(collection, pc.length());

        if (!checkRPmatchesSegments(collection))
            warn("Segments do not all start on reference point after recombining");

    }

    @Override
    public void progressEventReceived(ProgressEvent event) {
        fireProgressEvent();
    }

}
