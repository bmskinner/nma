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
package com.bmskinner.nuclear_morphology.analysis.profiles;

import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

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
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment.SegmentUpdateException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * Run the segmentation of datasets. This allows for the median profile
 * of a collection to be segmented, and the segments to be assigned to 
 * each nucleus in the dataset.
 * 
 * The complete analysis pipeline operates via the following pseudocode:
 * 1) From the collection median, find the representative median
 * 2) Segment the representative median
 * 3) For each nucleus, use iterative segment fitting to map the segments 
 * 
 * The below refers to a previous version
 * The complete analysis pipeline operates via the following pseudocode:<br>
 * <br>
 * 	1) Profile the cell collection<br>
 * 		- identify the RP via dataset RuleSets<br>
 *      - generate a median profile <br>
 * 	2) Find the best fit of each nucleus to the median to refine RP and other Tags<br>
 * 	3) Segment the median profile<br>
 *  4) Fit the segments to the nuclei using best-fit alignments<br>
 *  5) Generate frankenprofiles of each nucleus to the median<br>
 *  6) Profile the frankencollection - generate a new frankenmedian<br>
 *  7) Segment the frankenmedian; new segments may emerge from the reduced variation<br>
 *  8) Fit the frankensegments to the nuclei<br>
 *  9) Measure the sum of profile differences between the nuclei and the frankenmedian<br>
 *  <br>
 *   While this value is decreasing, repeat steps 5-9<br>
 *   <br>
 *   This class contains the methods to carry out steps 3-9.
 * 
 * @author bms41
 *
 */
public class DatasetSegmentationMethod extends SingleDatasetAnalysisMethod {
	
	private static final Logger LOGGER = Logger.getLogger(DatasetSegmentationMethod.class.getName());

	private ICellCollection sourceCollection = null;
	
	private final ICellCollection collection;

	private MorphologyAnalysisMode mode = MorphologyAnalysisMode.NEW;

	public enum MorphologyAnalysisMode {
		
		/** Segment the median and update nuclei to match */
		NEW, 
		
		/** Copy the segments from another dataset and update nuclei to match */
		COPY,
		
		/** Keep the median segmentation, and update nuclei to match */
		REFRESH
	}

	/**
	 * Segment a dataset with the given mode
	 * 
	 * @param dataset the dataset to be segmented
	 * @param mode the analysis mode to run
	 */
	public DatasetSegmentationMethod(@NonNull IAnalysisDataset dataset, @NonNull MorphologyAnalysisMode mode) {
		super(dataset);
		collection = dataset.getCollection();
		this.mode = mode;
	}

	/**
	 * Copy the segmentation pattern from the source collection onto the given
	 * dataset
	 * 
	 * @param dataset the dataset to be segmented
	 * @param source the collection to copy segment patterns from
	 */
	public DatasetSegmentationMethod(@NonNull IAnalysisDataset dataset, @NonNull ICellCollection source) {
		this(dataset, MorphologyAnalysisMode.COPY);
		this.sourceCollection = source;
	}

	@Override
	public IAnalysisResult call() throws Exception {
		LOGGER.fine("-----------------------------");
    	LOGGER.fine("Beginning segmentation method");
    	LOGGER.fine("-----------------------------");
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
			for (Nucleus n : dataset.getCollection().getNuclei()) {
				n.updateVerticallyRotatedNucleus();
				n.updateDependentStats();
			}
			LOGGER.fine("Updated verticals and stats");
			
			
			DatasetValidator dv = new DatasetValidator();
			if(!dv.validate(dataset)) {
				LOGGER.warning("Segmentation failed; resulting dataset did not validate");
			}
		} catch (Exception e) {
			result = null;
			LOGGER.warning("Error in segmentation: "+e.getMessage());
			LOGGER.log(Loggable.STACK, "Error in segmentation analysis", e);
		}

		return result;

	}

	/**
	 * The main analysis method. Segment the median, apply the segments to
	 * nuclei, generate the franken-median, and adjust the nuclear segments based
	 * on comparison of franken-profiles.
	 * 
	 * @return an analysis result with the segmented dataset
	 * @throws Exception
	 */
	private IAnalysisResult runNewAnalysis() throws Exception {
		if(!dataset.isRoot()) {
			LOGGER.fine("Dataset is not root, not segmenting");
			return new DefaultAnalysisResult(dataset);
		}
		
		dataset.getCollection().setConsensus(null); // clear if present
		LOGGER.fine("Before segmentation median length: "+collection.getMedianArrayLength());
		ISegmentedProfile median = createSegmentsInMedian(); // 3 - segment the median profile
		
		if(median.getSegmentCount()<=0) {
			LOGGER.warning("Error finding segments in median profile");
			return new DefaultAnalysisResult(dataset);
		}
		
		if(median.getSegmentCount()==1)
			LOGGER.warning("Unable to segment the median profile");
		
		// Make a new collection and aggregate to invalidate previous cached data, possibly
		// with different profile lengths
		dataset.getCollection().createProfileCollection();
				
		dataset.getCollection().getProfileCollection().addSegments(median.getSegments());
		
		assignSegmentsToNuclei(median);// 4 - fit the segments to nuclei 
		
		// The best segmentation pattern has been found
		// Copy segmentation to child datasets and invalidate  any consensus nuclei
		if(dataset.hasChildren()){
			for(IAnalysisDataset child: dataset.getAllChildDatasets()){
				child.getCollection().setConsensus(null);
				dataset.getCollection().getProfileManager().copyCollectionOffsets(child.getCollection());				
			}
		}
		return new DefaultAnalysisResult(dataset);
	}
	
	private IAnalysisResult runCopyAnalysis() throws Exception {
		if (sourceCollection == null) {
			LOGGER.warning("Cannot copy: source collection is null");
			return null;
		}

		if(!sourceCollection.getProfileCollection().hasSegments()) {
			LOGGER.fine("Cannot copy segments: source collection has no segments");
			dataset.getCollection().createProfileCollection(); // ensure profiles are set
			return new DefaultAnalysisResult(dataset);
		}

		LOGGER.fine("Copying segmentation pattern");
		reapplyProfiles();
		LOGGER.fine("Copying complete");
		return new DefaultAnalysisResult(dataset);
	}

	
	/**
	 * Refresh the nucleus segmentation. Assumes that the segmentation in the
	 * median profile is correct, and updates all nuclei to match.
	 * 
	 * @param collection
	 * @return
	 */
	private IAnalysisResult runRefreshAnalysis() throws Exception {
		ISegmentedProfile median = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		assignSegmentsToNuclei(median);
		return new DefaultAnalysisResult(dataset);
	}


	/**
	 * When a population needs to be reanalysed do not offset nuclei or
	 * recalculate best fits; just get the new median profile
	 * 
	 * @param collection the collection of nuclei
	 * @param sourceCollection the collection with segments to copy
	 */
	private void reapplyProfiles() throws Exception {

		LOGGER.fine("Applying existing segmentation profile to population");

		sourceCollection.getProfileManager().copyCollectionOffsets(collection);

		// At this point the collection has only a regular profile collections.
		// No Frankenprofile has been copied.
		LOGGER.fine("Re-profiling complete");
	}
	
	/**
	 * Use a segmenter to segment the median profile of the
	 * collection starting from the reference point
	 * 
	 * @param collection
	 * @throws ProfileException 
	 * @throws UnavailableProfileTypeException 
	 * @throws UnavailableBorderTagException 
	 */
	private ISegmentedProfile createSegmentsInMedian() throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException {
		
		// choose the best subset of nuclei and make a median profile from them
		LOGGER.fine("Collection median length "+collection.getMedianArrayLength());
		LOGGER.fine("Profile collection length "+collection.getProfileCollection().length());

		RepresentativeMedianFinder finder = new RepresentativeMedianFinder(collection);
		
		IProfile median = finder.findMedian();
		LOGGER.fine("Representative median length "+median.size());

		ProfileSegmenter segmenter = new ProfileSegmenter(median);
		List<IBorderSegment> segments = segmenter.segment();
		return new SegmentedFloatProfile(median, segments);
	}
	
	/**
	 * Assign the segments in the given profile to the nuclei within the
	 * collection. The template profile is assumed to be indexed at the reference
	 * point. This will unlock nuclei as needed to ensure that segments are consistent
	 * through the dataset.
	 * @throws UnavailableComponentException 
	 * @throws SegmentUpdateException 
	 * @throws ProfileException 
	 */
	private void assignSegmentsToNuclei(@NonNull ISegmentedProfile template) throws ProfileException, UnavailableComponentException {
		IterativeSegmentFitter fitter = new IterativeSegmentFitter(template);
		for(Nucleus n : collection.getNuclei()) {
			
			// Ensure new segments can be assigned
			boolean wasLocked = n.isLocked();
			if(wasLocked)
				n.setLocked(false);
			IProfile nucleusProfile  = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
			ISegmentedProfile segProfile = fitter.fit(nucleusProfile);
			n.setProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, segProfile);
			if(segProfile.getSegmentCount()!=template.getSegmentCount())
				throw new ProfileException("Segments could not be fitted to nucleus");
			if(template.getSegmentCount()==1) {
				ISegmentedProfile test  = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
				IBorderSegment seg = test.getSegment(IProfileCollection.DEFAULT_SEGMENT_ID);
				if(seg.getStartIndex()!=0) {
					throw new ProfileException("Single segment does not start at RP in nucleus");
				}
			}
			n.setLocked(wasLocked);
		}
	}
	
	@Override
	public void progressEventReceived(ProgressEvent event) {
		fireProgressEvent();
	}

}
