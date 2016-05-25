package analysis.nucleus;

import utility.Constants;
import logging.Loggable;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileType;
import components.generic.XYPoint;
import components.nuclei.Nucleus;
import components.nuclei.sperm.PigSpermNucleus;
import components.nuclei.sperm.RodentSpermNucleus;

/**
 * Offset the profiles of individual nuclei within a CellCollection
 * based on the similarities of the profile to the collection median
 * @author bms41
 *
 */
public class ProfileOffsetter implements Loggable {
	
	final private CellCollection collection;
	
	public ProfileOffsetter(final CellCollection collection){
		this.collection = collection;
	}
	
	private void calculateOffsetsInRoundNuclei() throws Exception {

		Profile medianToCompare = collection.getProfileCollection(ProfileType.REGULAR).getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN); // returns a median profile with head at 0

		for(Nucleus n : collection.getNuclei()){

			// returns the positive offset index of this profile which best matches the median profile
			int newHeadIndex = n.getProfile(ProfileType.REGULAR).getSlidingWindowOffset(medianToCompare);
			n.setBorderTag(BorderTag.REFERENCE_POINT, newHeadIndex);

			// check if flipping the profile will help

			double differenceToMedian1 = n.getProfile(ProfileType.REGULAR,BorderTag.REFERENCE_POINT).absoluteSquareDifference(medianToCompare);
			n.reverse();
			double differenceToMedian2 = n.getProfile(ProfileType.REGULAR,BorderTag.REFERENCE_POINT).absoluteSquareDifference(medianToCompare);

			if(differenceToMedian1<differenceToMedian2){
				n.reverse(); // put it back if no better
			}

			// also update the orientation position
			n.setBorderTag(BorderTag.ORIENTATION_POINT, n.getBorderIndex(BorderTag.REFERENCE_POINT));
			
		}
	}

	
	private void calculateOffsetsInRodentSpermNuclei() throws Exception {

		// Get the median profile starting from the orientation point
		Profile median = collection.getProfileCollection(ProfileType.REGULAR).getProfile(BorderTag.ORIENTATION_POINT, Constants.MEDIAN); // returns a median profile

		// go through each nucleus
		for(Nucleus n : collection.getNuclei()){

			// ensure the correct class is chosen
			RodentSpermNucleus nucleus = (RodentSpermNucleus) n;

			// get the offset for the best fit to the median profile
			int newTailIndex = nucleus.getProfile(ProfileType.REGULAR).getSlidingWindowOffset(median);

			// add the offset of the tail to the nucleus
			nucleus.setBorderTag(BorderTag.ORIENTATION_POINT, newTailIndex);


			// also update the head position (same as round reference point)
			// - the point opposite the tail through the CoM
			int headIndex = nucleus.getBorderIndex(nucleus.findOppositeBorder( nucleus.getBorderPoint(newTailIndex) ));
			nucleus.setBorderTag(BorderTag.REFERENCE_POINT, headIndex);
			nucleus.splitNucleusToHeadAndHump();

		}			

	}
	
	public void assignFlatRegionToMouseNuclei() throws Exception{
//		Profile median = collection.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(BorderTag.ORIENTATION_POINT, 50); // returns a median profile

//		{
			/*
			 * TODO: This will not work: the segmnets and frankenprofiling has not yet been performed
			 * Find the median profile segment with the flat region
			 */
		
		/*
		 * Franken profile method: segment proportionality
		 */
//			int verticalTopIndex = collection.getProfileCollection(ProfileCollectionType.REGULAR)
//					.getOffset(BorderTag.TOP_VERTICAL); 
//
//			int verticalBottomIndex = collection.getProfileCollection(ProfileCollectionType.REGULAR)
//					.getOffset(BorderTag.BOTTOM_VERTICAL); 
//
//			String topSegName = collection.getProfileCollection(ProfileCollectionType.REGULAR)
//					.getSegmentContaining(BorderTag.TOP_VERTICAL).getName();
//
//			String bottomSegName = collection.getProfileCollection(ProfileCollectionType.REGULAR)
//					.getSegmentContaining(BorderTag.BOTTOM_VERTICAL).getName();
//
//			SegmentedProfile profile = collection.getProfileCollection(ProfileCollectionType.REGULAR)
//					.getSegmentedProfile(BorderTag.REFERENCE_POINT);
//
//			NucleusBorderSegment topSegFromRef    = profile.getSegment(topSegName);
//			NucleusBorderSegment bottomSegFromRef = profile.getSegment(bottomSegName);
//
//			/*
//			 * Get the proportion of the indexes through the segment
//			 */
//			double topProportion = topSegFromRef.getIndexProportion(verticalTopIndex);
//			double bottomProportion = bottomSegFromRef.getIndexProportion(verticalBottomIndex);
//		}
		
		/*
		 * Regular profile method: offsetting
		 */
		{
			Profile verticalTopMedian;
			Profile verticalBottomMedian;
			try{
				verticalTopMedian = collection.getProfileCollection(ProfileType.REGULAR)
						.getProfile(BorderTag.TOP_VERTICAL, Constants.MEDIAN); 

				verticalBottomMedian = collection.getProfileCollection(ProfileType.REGULAR)
						.getProfile(BorderTag.BOTTOM_VERTICAL, Constants.MEDIAN); 


			} catch (IllegalArgumentException e){
				logError("Error assigning vertical in dataset "+collection.getName(), e);
				// This occurs when the median profile did not have detectable verticals. Return quietly.
				return;
			}
			for(Nucleus n : collection.getNuclei()){

				RodentSpermNucleus nucleus = (RodentSpermNucleus) n;


				int newIndexOne = nucleus.getProfile(ProfileType.REGULAR).getSlidingWindowOffset(verticalTopMedian);
				int newIndexTwo = nucleus.getProfile(ProfileType.REGULAR).getSlidingWindowOffset(verticalBottomMedian);

				XYPoint p0 = nucleus.getBorderPoint(newIndexOne);
				XYPoint p1 = nucleus.getBorderPoint(newIndexTwo);

				if(p0.getLengthTo(nucleus.getBorderTag(BorderTag.REFERENCE_POINT))> p1.getLengthTo(nucleus.getBorderTag(BorderTag.REFERENCE_POINT)) ){

					// P0 is further from the reference point than p1

					nucleus.setBorderTag(BorderTag.TOP_VERTICAL, newIndexTwo);
					nucleus.setBorderTag(BorderTag.BOTTOM_VERTICAL, newIndexOne);

				} else {

					nucleus.setBorderTag(BorderTag.TOP_VERTICAL, newIndexOne);
					nucleus.setBorderTag(BorderTag.BOTTOM_VERTICAL, newIndexTwo);

				}
			}


		}
	}
	
	private void calculateOffsetsInPigSpermNuclei() throws Exception {

		// get the median profile zeroed on the orientation point
		Profile medianToCompare = collection.getProfileCollection(ProfileType.REGULAR).getProfile(BorderTag.ORIENTATION_POINT, 50); 

		for(Nucleus nucleus : collection.getNuclei()){
			PigSpermNucleus n = (PigSpermNucleus) nucleus;

			// returns the positive offset index of this profile which best matches the median profile
			int tailIndex = n.getProfile(ProfileType.REGULAR).getSlidingWindowOffset(medianToCompare);

			n.setBorderTag(BorderTag.ORIENTATION_POINT, tailIndex);


			// also update the head position
			int headIndex = n.getBorderIndex(n.findOppositeBorder( n.getBorderPoint(tailIndex) ));
			n.setBorderTag(BorderTag.REFERENCE_POINT, headIndex);
		}

	}

	/**
	 * Offset the position of the tail in each nucleus based on the difference to the median.
	 * Also updates the top and bottom verticals.
	 * @param collection the nuclei
	 * @param nucleusClass the class of nucleus
	 */
	public void calculateOffsets() throws Exception {

		switch(collection.getNucleusType()){

			case PIG_SPERM:
				calculateOffsetsInPigSpermNuclei();
				break;
			case RODENT_SPERM:
				calculateOffsetsInRodentSpermNuclei();
				assignFlatRegionToMouseNuclei();
				break;
			default:
				calculateOffsetsInRoundNuclei();
				break;
		}
	}
	
	/**
	 * Offset the position of the tail in each nucleus based on the difference to the median
	 * @param collection the nuclei
	 * @param nucleusClass the class of nucleus
	 */
	public void calculateOPOffsets() throws Exception {

		switch(collection.getNucleusType()){

			case PIG_SPERM:
				calculateOffsetsInPigSpermNuclei();
				break;
			case RODENT_SPERM:
				calculateOffsetsInRodentSpermNuclei();
				break;
			default:
				calculateOffsetsInRoundNuclei();
				break;
		}
	}
	
	public void calculateVerticals()  throws Exception {
		switch(collection.getNucleusType()){
		case RODENT_SPERM:
			assignFlatRegionToMouseNuclei();
			break;
		default:
			break;
		}
	}
	

}
