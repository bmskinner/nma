package analysis.nucleus;

import java.util.logging.Level;
import java.util.logging.Logger;

import analysis.AnalysisDataset;
import analysis.AnalysisWorker;
import components.CellCollection;
import components.generic.BooleanProfile;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileCollection;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.generic.XYPoint;
import components.nuclear.NucleusBorderSegment;
import components.nuclear.NucleusType;
import components.nuclei.Nucleus;
import components.nuclei.sperm.PigSpermNucleus;
import components.nuclei.sperm.RodentSpermNucleus;
import utility.Constants;
import utility.Utils;

/**
 * This class contains the methods for detecting the reference and orientation points in a median
 * profile, creating the median profile, and adjusting individual nuclei border points to fit the median.
 * @author ben
 *
 */
public class DatasetProfiler extends AnalysisWorker {

	public DatasetProfiler(AnalysisDataset dataset, Logger programLogger){
		super(dataset, programLogger);
	}
	 
	@Override
    protected Boolean doInBackground() throws Exception {
    	
    	boolean result = true;
		try{


				log(Level.FINE, "Profiling dataset");

				this.setProgressTotal(3);

				BorderTag pointType = BorderTag.REFERENCE_POINT;

				// profile the collection from head/tip, then apply to tail
				runProfiler(pointType);

				log(Level.FINE, "Datset profiling complete");
			
			
		} catch(Exception e){
			
			logError("Error in dataset profiling", e);
			return false;
		} 

		return result;
	}
	
	/**
	 * Calculaate the median profile of the colleciton, and generate the
	 * best fit offsets of each nucleus to match
	 * @param collection
	 * @param pointType
	 */
	private void runProfiler(BorderTag pointType){
		
		try{
			log(Level.FINE,  "Profiling collection");
			
			CellCollection collection = getDataset().getCollection();
			
			// A cell collection starts with an empty Regular ProfileCollection
			ProfileCollection pc = collection.getProfileCollection(ProfileType.REGULAR);

			// default is to make profile aggregate from reference point
			pc.createProfileAggregate(collection);
			publish(1);

			// use the median profile of this aggregate to find the orientation point ("tail")
			TailFinder.findTailIndexInMedianCurve(collection);
			publish(2);

			// carry out iterative offsetting to refine the orientation point estimate
			double score = compareProfilesToMedian(collection, pointType);
			double prevScore = score*2;
			while(score < prevScore){

				// rebuild the aggregate - needed if the orientation point index has changed in any nuclei
				pc.createProfileAggregate(collection);

				// carry out the orientation point detection in the median again
				TailFinder.findTailIndexInMedianCurve(collection);

				// apply offsets to each nucleus in the collection
				Offsetter.calculateOffsets(collection); 

				prevScore = score;

				// get the difference between aligned profiles and the median
				score = compareProfilesToMedian(collection, pointType);
				log(Level.FINE, "Reticulating splines: score: "+(int)score);
			}
			publish(3);
			log(Level.FINE,  "Finished profiling collection; median generated");

		} catch(Exception e){
			logError("Error in morphology profiling", e);
		}
	}
	
	/**
	 * Get the total differences to the median for all the nuclei in
	 * the collection
	 * @param collection
	 * @param pointType
	 * @return
	 */
	private static double compareProfilesToMedian(CellCollection collection, BorderTag pointType) throws Exception {
		double[] scores = collection.getDifferencesToMedianFromPoint(pointType);
		double result = 0;
		for(double s : scores){
			result += s;
		}
		return result;
	}
	
	public static class TailFinder {

		private static void findTailInRodentSpermMedian(CellCollection collection) throws Exception {

			// can't use regular tail detector, because it's based on NucleusBorderPoints
			// get minima in curve, then find the lowest minima / minima furthest from both ends
			collection.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.REFERENCE_POINT, 0);

			Profile medianProfile = collection.getProfileCollection(ProfileType.REGULAR).getProfile(BorderTag.REFERENCE_POINT, 50);

			BooleanProfile minima = medianProfile.smooth(2).getLocalMinima(5); // window size 5

			double minAngle = 180;
			int tailIndex = 0;

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
		public static void assignTopAndBottomVerticalInMouse(CellCollection collection) throws Exception{
			 
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

		private static void findTailInPigSpermMedian(CellCollection collection) throws Exception {
			
			// define the current zero offset at the reference point
			// It does not matter, it just gives an offset key for the ProfileCollection
			collection.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.REFERENCE_POINT, 0);
			
			// get the profile
			// This is starting from an arbitrary point?
			// Starting from the head in test data, so the reference point is correct
			Profile medianProfile = collection.getProfileCollection(ProfileType.REGULAR).getProfile(BorderTag.REFERENCE_POINT, 50);
//			medianProfile.print();

			// find local maxima in the median profile over 180
			BooleanProfile maxima = medianProfile.smooth(2).getLocalMaxima(5, 180); // window size 5, only values over 180


			double minAngle = 180;
			int tailIndex = 0;

			// do not consider maxima that are too close to the head of the sperm
			/*
			 * ERROR when head is defined near to tail by chance - we exclude the true tail
			 */
//			int tipExclusionIndex1 = (int) (medianProfile.size() * 0.2);
//			int tipExclusionIndex2 = (int) (medianProfile.size() * 0.6);

			if(maxima.size()==0){
				log(Level.WARNING, "Error: no maxima found in median line");
				tailIndex = 100; // set to roughly the middle of the array for the moment

			} else{
				
				for(int i = 0; i<maxima.size();i++){
					
					if(maxima.get(i)==true){ // look at local maxima
						int index = i;

						double angle = medianProfile.get(index); // get the angle at this maximum
						
						// look for the highest local maximum outside the exclusion range
//						if(angle>minAngle && index > tipExclusionIndex1 && index < tipExclusionIndex2){
						if(angle>minAngle){
							minAngle = angle;
							tailIndex = index;
						}
					}
				}
			}

			// add this index to be the orientation point
			log(Level.FINEST, "Setting tail to index: "+tailIndex);
			collection.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.ORIENTATION_POINT, tailIndex);
			collection.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.REFERENCE_POINT, tailIndex);
			/*
			 * Looks like reference point needs to be 0. Check the process aligning the profiles - they must be settling on 
			 * the RP 
			 */
			
			
			
			// set the reference point half way around from the tail
			double length = (double) collection.getProfileCollection(ProfileType.REGULAR).getAggregate().length();		
			int offset =  (int) Math.ceil(length / 2d); // ceil to ensure offsets are correct
			
			// now we have the tail point located, update the reference point to be opposite
//			fileLogger.log(Level.FINE, "Profile collection before intersection point re-index: ");
//			fileLogger.log(Level.FINE, collection.getProfileCollection(ProfileCollectionType.REGULAR).toString());
			
//			 adjust the index to the offset
			int headIndex  = Utils.wrapIndex( tailIndex - offset, collection.getProfileCollection(ProfileType.REGULAR).getAggregate().length());
//			fileLogger.log(Level.FINE, "Setting head to index: "+headIndex);
			collection.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.INTERSECTION_POINT, headIndex);
		}
		

		private static void findTailInRoundMedian(CellCollection collection) throws Exception {
			
			collection.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.REFERENCE_POINT, 0);
			ProfileCollection pc = collection.getProfileCollection(ProfileType.REGULAR);

			Profile medianProfile = pc.getProfile(BorderTag.REFERENCE_POINT, 50);

			int tailIndex = (int) Math.floor(medianProfile.size()/2);
						
			collection.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.ORIENTATION_POINT, tailIndex);
		}

		/**
		 * Identify tail in median profile and offset nuclei profiles. For a 
		 * regular round nucleus, the tail is one of the points of longest
		 *  diameter, and lowest angle
		 * @param collection the nucleus collection
		 */
		public static void findTailIndexInMedianCurve(CellCollection collection) throws Exception {

			switch(collection.getNucleusType()){

				case PIG_SPERM:
					findTailInPigSpermMedian(collection);	
					break;
				case RODENT_SPERM:
					findTailInRodentSpermMedian(collection);
					assignTopAndBottomVerticalInMouse(collection);
					break;
				default:
					findTailInRoundMedian(collection);
					break;
			}
		}
	}
	
	public static class Offsetter {
		
		private static void calculateOffsetsInRoundNuclei(CellCollection collection) throws Exception {

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

				// also update the tail position
				int tailIndex = n.getIndex(n.findOppositeBorder( n.getPoint(newHeadIndex) ));
				n.setBorderTag(BorderTag.ORIENTATION_POINT, tailIndex);
			}
		}

		
		private static void calculateOffsetsInRodentSpermNuclei(CellCollection collection) throws Exception {

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
				int headIndex = nucleus.getIndex(nucleus.findOppositeBorder( nucleus.getPoint(newTailIndex) ));
				nucleus.setBorderTag(BorderTag.REFERENCE_POINT, headIndex);
				nucleus.splitNucleusToHeadAndHump();

			}			

		}
		
		public static void assignFlatRegionToMouseNuclei(CellCollection collection) throws Exception{
//			Profile median = collection.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(BorderTag.ORIENTATION_POINT, 50); // returns a median profile

//			{
				/*
				 * TODO: This will not work: the segmnets and frankenprofiling has not yet been performed
				 * Find the median profile segment with the flat region
				 */
			
			/*
			 * Franken profile method: segment proportionality
			 */
//				int verticalTopIndex = collection.getProfileCollection(ProfileCollectionType.REGULAR)
//						.getOffset(BorderTag.TOP_VERTICAL); 
//
//				int verticalBottomIndex = collection.getProfileCollection(ProfileCollectionType.REGULAR)
//						.getOffset(BorderTag.BOTTOM_VERTICAL); 
//
//				String topSegName = collection.getProfileCollection(ProfileCollectionType.REGULAR)
//						.getSegmentContaining(BorderTag.TOP_VERTICAL).getName();
//
//				String bottomSegName = collection.getProfileCollection(ProfileCollectionType.REGULAR)
//						.getSegmentContaining(BorderTag.BOTTOM_VERTICAL).getName();
//
//				SegmentedProfile profile = collection.getProfileCollection(ProfileCollectionType.REGULAR)
//						.getSegmentedProfile(BorderTag.REFERENCE_POINT);
//
//				NucleusBorderSegment topSegFromRef    = profile.getSegment(topSegName);
//				NucleusBorderSegment bottomSegFromRef = profile.getSegment(bottomSegName);
//
//				/*
//				 * Get the proportion of the indexes through the segment
//				 */
//				double topProportion = topSegFromRef.getIndexProportion(verticalTopIndex);
//				double bottomProportion = bottomSegFromRef.getIndexProportion(verticalBottomIndex);
//			}
			
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
//					logError("Error assigning vertical in dataset "+collection.getName(), e);
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
		
		private static void calculateOffsetsInPigSpermNuclei(CellCollection collection) throws Exception {

			// get the median profile zeroed on the orientation point
			Profile medianToCompare = collection.getProfileCollection(ProfileType.REGULAR).getProfile(BorderTag.ORIENTATION_POINT, 50); 

			for(Nucleus nucleus : collection.getNuclei()){
				PigSpermNucleus n = (PigSpermNucleus) nucleus;

				// returns the positive offset index of this profile which best matches the median profile
				int tailIndex = n.getProfile(ProfileType.REGULAR).getSlidingWindowOffset(medianToCompare);

				n.setBorderTag(BorderTag.ORIENTATION_POINT, tailIndex);


				// also update the head position
				int headIndex = n.getIndex(n.findOppositeBorder( n.getPoint(tailIndex) ));
				n.setBorderTag(BorderTag.REFERENCE_POINT, headIndex);
			}

		}

		/**
		 * Offset the position of the tail in each nucleus based on the difference to the median
		 * @param collection the nuclei
		 * @param nucleusClass the class of nucleus
		 */
		public static void calculateOffsets(CellCollection collection) throws Exception {

			switch(collection.getNucleusType()){

				case PIG_SPERM:
					calculateOffsetsInPigSpermNuclei(collection);
					break;
				case RODENT_SPERM:
					calculateOffsetsInRodentSpermNuclei(collection);
					assignFlatRegionToMouseNuclei(collection);
					break;
				default:
					calculateOffsetsInRoundNuclei(collection);
					break;
			}
		}

	}
	 
}
