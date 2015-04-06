package no.analysis;

import java.io.File;
import java.util.List;

import no.collections.INuclearCollection;
import no.components.NucleusBorderSegment;
import no.components.Profile;
import no.components.ProfileAggregate;
import no.nuclei.INuclearFunctions;
import ij.IJ;

public class PopulationProfiler {

	public static void run(INuclearCollection collection){
		String pointType = collection.getReferencePoint();

		// create an initial profile aggregate from the estimated points
		createProfileAggregateFromPoint(collection, pointType);

		// use the median profile of this aggregate to find the tail point
		collection.findTailIndexInMedianCurve();

		// carry out iterative offsetting to refine the tail point estimate
		double score = compareProfilesToMedian(collection, pointType);
		double prevScore = score+1;
		while(score < prevScore){
			createProfileAggregateFromPoint(collection, pointType);
			
			// we need to allow each nucleus collection type handle tail finding and offsetting itself
			collection.findTailIndexInMedianCurve();
			collection.calculateOffsets(); 

			prevScore = score;
			score = compareProfilesToMedian(collection, pointType);

			IJ.log("    Reticulating splines: score: "+(int)score);
		}

		// get the profile plots created
		collection.getProfileCollection().preparePlots(INuclearCollection.CHART_WINDOW_WIDTH, INuclearCollection.CHART_WINDOW_HEIGHT, collection.getMaxProfileLength());

	}
	
	/**
	 * When a population needs to be reanalysed do not offset nuclei or recalculate best fits;
	 * just get the new median profile 
	 * @param collection the collection of nuclei
	 */
	public static void reapplyProfiles(INuclearCollection collection, INuclearCollection sourceCollection){
		String pointType = collection.getReferencePoint();
		createProfileAggregateFromPoint(collection, pointType, (int)sourceCollection.getMedianArrayLength());
		createProfileAggregateFromPoint(collection, collection.getOrientationPoint(), (int)sourceCollection.getMedianArrayLength());
		
		collection.getProfileCollection().addSegments(pointType, sourceCollection.getProfileCollection().getSegments(pointType));
		collection.getProfileCollection().addSegments("tail", sourceCollection.getProfileCollection().getSegments(collection.getOrientationPoint()));
		collection.getProfileCollection().preparePlots(INuclearCollection.CHART_WINDOW_WIDTH, INuclearCollection.CHART_WINDOW_HEIGHT, collection.getMaxProfileLength());
		collection.getProfileCollection().addMedianLinesToPlots();
		collection.getProfileCollection().exportProfilePlots(collection.getFolder()+File.separator+collection.getOutputFolder(), collection.getType());
	}
	
	/**
	 * A temp addition to allow a remapping median to be the same length as the original population median.
	 * Otherwise the segment remapping will fail
	 * @param collection
	 * @param pointType
	 * @param length
	 */
	private static void createProfileAggregateFromPoint(INuclearCollection collection, String pointType, int length){

		ProfileAggregate profileAggregate = new ProfileAggregate(length);
		collection.getProfileCollection().addAggregate(pointType, profileAggregate);
		
		for(INuclearFunctions n : collection.getNuclei()){
			profileAggregate.addValues(n.getAngleProfile(pointType));
		}

		Profile medians = profileAggregate.getMedian();
		Profile q25     = profileAggregate.getQuartile(25);
		Profile q75     = profileAggregate.getQuartile(75);
		collection.getProfileCollection().addProfile(pointType, medians);
		collection.getProfileCollection().addProfile(pointType+"25", q25);
		collection.getProfileCollection().addProfile(pointType+"75", q75);
//		collection.getProfileCollection().printKeys();

	}

	private static void createProfileAggregateFromPoint(INuclearCollection collection, String pointType){

		ProfileAggregate profileAggregate = new ProfileAggregate((int)collection.getMedianArrayLength());
		collection.getProfileCollection().addAggregate(pointType, profileAggregate);

		for(INuclearFunctions n : collection.getNuclei()){
			profileAggregate.addValues(n.getAngleProfile(pointType));
		}

		Profile medians = profileAggregate.getMedian();
		Profile q25     = profileAggregate.getQuartile(25);
		Profile q75     = profileAggregate.getQuartile(75);
		collection.getProfileCollection().addProfile(pointType, medians);
		collection.getProfileCollection().addProfile(pointType+"25", q25);
		collection.getProfileCollection().addProfile(pointType+"75", q75);
//		collection.getProfileCollection().printKeys();

	}

	public static void createProfileAggregates(INuclearCollection collection){
		try{
			for( String pointType : collection.getProfileCollection().getProfileKeys() ){
				createProfileAggregateFromPoint(collection, pointType);   
			}
		} catch(Exception e){
			IJ.log("    Error creating profile aggregates: "+e.getMessage());
			collection.getProfileCollection().printKeys();
		}
	}

	private static double compareProfilesToMedian(INuclearCollection collection, String pointType){
		double[] scores = collection.getDifferencesToMedianFromPoint(pointType);
		double result = 0;
		for(double s : scores){
			result += s;
		}
		return result;
	}

}
