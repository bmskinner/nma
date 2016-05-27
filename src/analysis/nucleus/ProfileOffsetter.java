package analysis.nucleus;

import java.util.UUID;

import utility.Constants;
import logging.Loggable;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.generic.XYPoint;
import components.nuclear.NucleusBorderSegment;
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
		Profile median = collection.getProfileCollection(ProfileType.REGULAR)
				.getProfile(BorderTag.ORIENTATION_POINT, Constants.MEDIAN); // returns a median profile

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
			nucleus.setBorderTag(BorderTag.INTERSECTION_POINT, headIndex);
//			nucleus.setBorderTag(BorderTag.REFERENCE_POINT, headIndex);
			nucleus.splitNucleusToHeadAndHump();

		}			

	}
	
	
	/**
	 * This method requires the frankenprofiling to be completed
	 * @throws Exception
	 */
	public void assignBorderTagViaFrankenProfile(BorderTag tag) throws Exception{

		int index = collection.getProfileCollection(ProfileType.REGULAR)
				.getOffset(tag); 

		/*
		 * Check that the points exist
		 */
		if( index == -1  ){
			warn("Cannot find "+tag+" index in median");
			return;
		}

		UUID segID = collection.getProfileCollection(ProfileType.REGULAR)
				.getSegmentContaining(tag).getID();
//		String segName = collection.getProfileCollection(ProfileType.REGULAR)
//				.getSegmentContaining(tag)..getName();



		SegmentedProfile profile = collection.getProfileCollection(ProfileType.REGULAR)
				.getSegmentedProfile(BorderTag.REFERENCE_POINT);

		
		NucleusBorderSegment segFromRef    = profile.getSegment(segID);
		
//		NucleusBordersSegment segFromRef    = profile.getSegment(segName);


		/*
		 * Get the proportion of the index through the segment
		 */
		double proportion    = segFromRef.getIndexProportion(index);
		finest("Found "+tag+" at "+proportion+" through median segment "+segFromRef.getID());


		/*
		 * Go through each nucleus and apply the position
		 */
		finer("Updating tag location in nuclei");
		for(Nucleus nucleus : collection.getNuclei()){

			int oldNIndex = nucleus.getBorderIndex(tag);
			if(oldNIndex==-1){
				finer("Border tag does not exist and will be created");
			} else {
				finer("Previous "+tag+" index at "+oldNIndex);
			}
			
//			NucleusBorderSegment nucleusSegment = nucleus.getProfile(ProfileType.REGULAR)
//					.getSegment(segName);
			NucleusBorderSegment nucleusSegment = nucleus.getProfile(ProfileType.REGULAR)
					.getSegment(segID);
			
			if(nucleusSegment==null){
				warn("Error updating nucleus, segment "+segID+" not found");
			} else {
				finest("Using nucleus segment "+nucleusSegment.getID());
			}
			
			int newIndex = nucleusSegment.getProportionalIndex(proportion); // find the index in the segment closest to the proportion 
			
			if(newIndex==-1){
				warn("Cannot find "+tag+" index in nucleus profile at proportion "+proportion);
				continue;
			}


			nucleus.setBorderTag(tag, newIndex);		
			finest("Set border tag in nucleus to "+newIndex+ " from "+oldNIndex);
		}
		
	}
	
	/**
	 * This method requires the frankenprofiling to be completed
	 * @throws Exception
	 */
	private void assignTopAndBottomVerticalsViaFrankenProfile() throws Exception{
				
		
		/*
		 * Franken profile method: segment proportionality
		 */
		
		assignBorderTagViaFrankenProfile(BorderTag.TOP_VERTICAL);
		assignBorderTagViaFrankenProfile(BorderTag.BOTTOM_VERTICAL);
		
		
		for(Nucleus nucleus : collection.getNuclei()){			
			nucleus.updateVerticallyRotatedNucleus();
		}	
		
	}
	
	private void assignTopAndBottomVerticalsToMouseViaOffsetting() throws Exception{
		
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
				warn("Error assigning vertical in dataset "+collection.getName());
				warn("No vertical points detected");
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
		Profile medianToCompare = collection.getProfileCollection(ProfileType.REGULAR)
				.getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN); 

		for(Nucleus nucleus : collection.getNuclei()){
			PigSpermNucleus n = (PigSpermNucleus) nucleus;

			// returns the positive offset index of this profile which best matches the median profile
			int tailIndex = n.getProfile(ProfileType.REGULAR).getSlidingWindowOffset(medianToCompare);

			n.setBorderTag(BorderTag.ORIENTATION_POINT, tailIndex);
			n.setBorderTag(BorderTag.REFERENCE_POINT, tailIndex);

			// also update the head position
			int headIndex = n.getBorderIndex(n.findOppositeBorder( n.getBorderPoint(tailIndex) ));
			n.setBorderTag(BorderTag.INTERSECTION_POINT, headIndex);
		}

	}

	/**
	 * Offset the position of the tail in each nucleus based on the difference to the median.
	 * Also updates the top and bottom verticals.
	 * @param collection the nuclei
	 * @param nucleusClass the class of nucleus
	 */
	public void calculateOffsets() throws Exception {

		calculateOPOffsets();
		calculateVerticals();
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
	
	/**
	 * Offset the position of top and bottom vertical points in each nucleus
	 * @throws Exception
	 */
	public void calculateVerticals()  throws Exception {
		switch(collection.getNucleusType()){
		case RODENT_SPERM:
			assignTopAndBottomVerticalsToMouseViaOffsetting();
			break;
		default:
			break;
		}
	}
	
	/**
	 * Use the proportional segment method to update top and bottom vertical positions
	 * within the dataset
	 * @throws Exception
	 */
	public void reCalculateVerticals() throws Exception{
		assignTopAndBottomVerticalsViaFrankenProfile();
	}

}
