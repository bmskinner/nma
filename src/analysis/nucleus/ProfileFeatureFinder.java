package analysis.nucleus;

import java.util.logging.Level;

import utility.Constants;
import logging.Loggable;
import components.AbstractCellularComponent;
import components.CellCollection;
import components.generic.BooleanProfile;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileType;

/**
 * Detect features in a profile. This is designed to identify the 
 * orientaition point (tail) in nuclei, and to identify the flat
 * region in mouse sperm nuclei (the top and bottom verticals)
 * @author bms41
 *
 */
public class ProfileFeatureFinder implements Loggable {
	
	CellCollection collection;
	
	public ProfileFeatureFinder(CellCollection collection){
		this.collection = collection;
	}

	private void findTailInRodentSpermMedian() throws Exception {

		// can't use regular tail detector, because it's based on NucleusBorderPoints
		// get minima in curve, then find the lowest minima / minima furthest from both ends
		collection.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.REFERENCE_POINT, 0);

		Profile medianProfile = collection.getProfileCollection(ProfileType.REGULAR).getProfile(BorderTag.REFERENCE_POINT, 50);

		BooleanProfile minima = medianProfile.smooth(2).getLocalMinima(5); // window size 5

		double minAngle = 180;
		int tailIndex = medianProfile.size() >> 1; // set the default tail at the half way point from the tip. 

		int tipExclusionIndex1 = (int) (medianProfile.size() * 0.2);
		int tipExclusionIndex2 = (int) (medianProfile.size() * 0.6);

		for(int i = 0; i<minima.size();i++){
			if( minima.get(i)==true){
				int index = i;

				double angle = medianProfile.get(index);
				if(angle<minAngle && index > tipExclusionIndex1 && index < tipExclusionIndex2){ // get the lowest point that is not near the tip
					minAngle = angle;
					tailIndex = index;
				}
			}
		}

		collection.getProfileCollection(ProfileType.REGULAR)
			.addOffset(BorderTag.ORIENTATION_POINT, tailIndex);

	}
	
	/**
	 * Find the flat region of the nucleus under the hook in the median profile, and mark the ends of the region
	 * with BorderTags TOP_VERTICAL and BOTTOM_VERTICAL
	 * @param collection
	 * @throws Exception
	 */
	public void assignTopAndBottomVerticalInMouse() throws Exception{
		 
		Profile medianProfile = collection.getProfileCollection(ProfileType.REGULAR).getProfile(BorderTag.REFERENCE_POINT, 50);

		/*
	     * Call to a StraightPointFinder that will find the straight part of the nucleus
	     * Use this to set the BorderTag.TopVertical and BottomVertical
	     */
	    int[] verticalPoints = medianProfile.getConsistentRegionBounds(180, 2, 10);
	    if(verticalPoints[0]!=-1 && verticalPoints[1]!=-1){
	    	collection.getProfileCollection(ProfileType.REGULAR)
	    		.addOffset(BorderTag.TOP_VERTICAL, verticalPoints[0]);
	    	collection.getProfileCollection(ProfileType.REGULAR)
	    	.addOffset(BorderTag.BOTTOM_VERTICAL, verticalPoints[1]);
	    } else {
	    	log(Level.WARNING, "Dataset "+collection.getName()+": Unable to assign vertical positions in median profile");
	    }
		
	}

	private void findTailInPigSpermMedian() throws Exception {
		
		// define the current zero offset at the reference point
		// It does not matter, it just gives an offset key for the ProfileCollection
		collection.getProfileCollection(ProfileType.REGULAR)
			.addOffset(BorderTag.REFERENCE_POINT, 0);
		
		// get the profile
		// This is starting from an arbitrary point?
		// Starting from the head in test data, so the reference point is correct
		Profile medianProfile = collection.getProfileCollection(ProfileType.REGULAR)
				.getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN);


		// find local maxima in the median profile over 180
		BooleanProfile maxima = medianProfile.smooth(2).getLocalMaxima(5, 180); // window size 5, only values over 180


		double minAngle = 180;
		int tailIndex   = 0; // the tail is, by default, the reference point


		if(maxima.size()==0){
			log(Level.WARNING, "Error: no maxima found in median line");
			
			tailIndex = medianProfile.size() / 2; // set to roughly the middle of the array for the moment

		} else{  // Maxima were found

			for(int i = 0; i<maxima.size();i++){
				
				if(maxima.get(i)==true){ // look at local maxima
					int index = i;

					double angle = medianProfile.get(index); // get the angle at this maximum
					
					// look for the highest local maximum
					if(angle>minAngle){
						minAngle = angle;
						tailIndex = index;
					}
				}
			}
		}

		// add this index to be the reference point
		collection.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.REFERENCE_POINT, tailIndex);
		finer("Added RP at index "+tailIndex);
		
		
		// ensure we have teh correct offset 
		tailIndex = collection.getProfileCollection(ProfileType.REGULAR).getOffset(BorderTag.REFERENCE_POINT);
		
		collection.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.ORIENTATION_POINT, tailIndex);
		finer("Added OP at index "+tailIndex);
		/*
		 * Looks like reference point needs to be 0. Check the process aligning the profiles - they must be settling on 
		 * the RP 
		 */
		
		// set the intersection point half way around from the tail
		// find the offset to use
		double length = (double) collection.getProfileCollection(ProfileType.REGULAR).getAggregate().length();		
		int offset    =  (int) Math.ceil(length / 2d); // ceil to ensure offsets are correct
		
		
//		 adjust the intersection point index using the offset
		int headIndex  = AbstractCellularComponent.wrapIndex( tailIndex - offset, collection.getProfileCollection(ProfileType.REGULAR).getAggregate().length());
		collection.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.INTERSECTION_POINT, headIndex);
		finer("Added IP at index "+headIndex);
	}
	

	private void findTailInRoundMedian() throws Exception {
		
		collection.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.REFERENCE_POINT, 0);
		collection.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.ORIENTATION_POINT, 0);
	}

	/**
	 * Identify tail in median profile and offset nuclei profiles. For a 
	 * regular round nucleus, the tail is one of the points of longest
	 *  diameter, and lowest angle
	 * @param collection the nucleus collection
	 */
	public void findTailIndexInMedianCurve() throws Exception {

		switch(collection.getNucleusType()){

			case PIG_SPERM:
				findTailInPigSpermMedian();	
				break;
			case RODENT_SPERM:
				findTailInRodentSpermMedian();
				assignTopAndBottomVerticalInMouse();
				break;
			default:
				findTailInRoundMedian();
				break;
		}
	}
}
