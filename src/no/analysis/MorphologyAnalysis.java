package no.analysis;

import ij.IJ;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import utility.Logger;
import no.collections.CellCollection;
import no.components.NucleusBorderSegment;
import no.components.Profile;
import no.components.ProfileAggregate;
import no.components.ProfileCollection;
import no.components.ProfileFeature;
import no.export.TableExporter;
import no.nuclei.Nucleus;
import no.nuclei.RoundNucleus;
import no.nuclei.sperm.PigSpermNucleus;
import no.nuclei.sperm.RodentSpermNucleus;

/**
 * This is the core of the morphology analysis pipeline.
 * It is the only module that is essential. This offsets nucleus profiles,
 * generates the median profiles, segments them, and applies the segments to
 * nuclei.
 */
public class MorphologyAnalysis {
	
	private static Logger logger;
	
	public static boolean run(CellCollection collection){

		logger = new Logger(collection.getDebugFile(), "MorphologyAnalysis");
		try{

			logger.log("Beginning core morphology analysis");

			String pointType = collection.getReferencePoint();

			// setup the plots and profile agregates
//			initialiseProfileCollection(collection);

			// profile the collection from head/tip, then apply to tail
			runProfiler(collection, pointType);

			// segment the profiles from head
			runSegmentation(collection, pointType);

			// export the core data

			// run the exports
//			exportProfiles(collection);
//			exportSegments(collection, pointType);
//			exportClusteringScript(collection);

			// begin migrating these export functions up
//			exportVariabilityRegions(collection, pointType);
//			drawProfileCollection(   collection, pointType);

			logger.log("Core morphology analysis complete");
			return true;
		} catch(Exception e){
			
			logger.log("Error in morphology analysis: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
			
			logger.log("Collection keys:", Logger.ERROR);
			logger.log(collection.getProfileCollection().printKeys(), Logger.ERROR);
			
			logger.log("FrankenCollection keys:", Logger.ERROR);
			logger.log(collection.getFrankenCollection().printKeys(), Logger.ERROR);
			return false;
		}

	}

	private static void runProfiler(CellCollection collection, String pointType){
		
		// default is to make profile aggregate from reference point
		createProfileAggregateFromPoint(collection, pointType);
		
		// use the median profile of this aggregate to find the tail point
		findTailIndexInMedianCurve(collection);
		
		// carry out iterative offsetting to refine the tail point estimate
		double score = compareProfilesToMedian(collection, pointType);
		double prevScore = score+1;
		while(score < prevScore){
			createProfileAggregateFromPoint(collection, pointType);
			
			// we need to allow each nucleus collection type handle tail finding and offsetting itself
			findTailIndexInMedianCurve(collection);
			calculateOffsets(collection); 

			prevScore = score;
			score = compareProfilesToMedian(collection, pointType);
			logger.log("Reticulating splines: score: "+(int)score);
		}
		
		// update the median profile with the final offset locations
		
		// create a profile aggregate for the orientation point
		createProfileAggregateFromPoint(collection, collection.getOrientationPoint());

	}
	
	
	  /*
    
  */

	/**
	 * Identify tail in median profile and offset nuclei profiles. For a 
	 * regular round nucleus, the tail is one of the points of longest
	 *  diameter, and lowest angle
	 * @param collection the nucleus collection
	 * @param nucleusClass the class of nucleus
	 */
	public static void findTailIndexInMedianCurve(CellCollection collection){

		if(collection.getNucleusClass() == RoundNucleus.class){

			ProfileCollection pc = collection.getProfileCollection();

			Profile medianProfile = pc.getProfile(collection.getReferencePoint());

			int tailIndex = (int) Math.floor(medianProfile.size()/2);

			Profile tailProfile = medianProfile.offset(tailIndex);
			pc.addProfile(collection.getOrientationPoint(), tailProfile);
			pc.addFeature(collection.getReferencePoint(), new ProfileFeature(collection.getOrientationPoint(), tailIndex));
		}

		if(collection.getNucleusClass() == PigSpermNucleus.class){
			Profile medianProfile = collection.getProfileCollection().getProfile(collection.getReferencePoint());

			Profile minima = medianProfile.getLocalMaxima(5); // window size 5

			//    double minDiff = medianProfile.size();
			double minAngle = 180;
			int tailIndex = 0;

			int tipExclusionIndex1 = (int) (medianProfile.size() * 0.2);
			int tipExclusionIndex2 = (int) (medianProfile.size() * 0.6);

			if(minima.size()==0){
				IJ.log("    Error: no minima found in median line");
				tailIndex = 100; // set to roughly the middle of the array for the moment

			} else{

				for(int i = 0; i<minima.size();i++){
					if(minima.get(i)==1){
						int index = (int)minima.get(i);

						//          int toEnd = medianProfile.size() - index;
						//          int diff = Math.abs(index - toEnd);

						double angle = medianProfile.get(index);
						if(angle>minAngle && index > tipExclusionIndex1 && index < tipExclusionIndex2){ // get the lowest point that is not near the tip
							minAngle = angle;
							tailIndex = index;
						}
					}
				}
			}
			// IJ.log("    Tail in median profile is at index "+tailIndex+", angle "+minAngle);
			Profile tailProfile = medianProfile.offset(tailIndex);
			collection.getProfileCollection().addProfile("tail", tailProfile);
			collection.getProfileCollection().addFeature("head", new ProfileFeature("tail", tailIndex));
		}

		if(collection.getNucleusClass() == RodentSpermNucleus.class){
			// can't use regular tail detector, because it's based on NucleusBorderPoints
			// get minima in curve, then find the lowest minima / minima furthest from both ends

			Profile medianProfile = collection.getProfileCollection().getProfile(collection.getReferencePoint());

			Profile minima = medianProfile.smooth(2).getLocalMinima(5); // window size 5

			//		double minDiff = medianProfile.size();
			double minAngle = 180;
			int tailIndex = 0;

			int tipExclusionIndex1 = (int) (medianProfile.size() * 0.2);
			int tipExclusionIndex2 = (int) (medianProfile.size() * 0.6);

			for(int i = 0; i<minima.size();i++){
				if( (int)minima.get(i)==1){
					int index = i;

					double angle = medianProfile.get(index);
					if(angle<minAngle && index > tipExclusionIndex1 && index < tipExclusionIndex2){ // get the lowest point that is not near the tip
						minAngle = angle;
						tailIndex = index;
					}
				}
			}
			Profile tailProfile = medianProfile.offset(tailIndex);
			collection.getProfileCollection().addProfile(collection.getOrientationPoint(), tailProfile);
			collection.getProfileCollection().addFeature(collection.getReferencePoint(), new ProfileFeature(collection.getOrientationPoint(), tailIndex)); // set the tail-index in the tip normalised profile
		}
	}
	
	
	/**
	 * Offset the position of the tail in each nucleus based on the difference to the median
	 * @param collection the nuclei
	 * @param nucleusClass the class of nucleus
	 */
	public static void calculateOffsets(CellCollection collection){

		if(collection.getNucleusClass() == RoundNucleus.class){


			Profile medianToCompare = collection.getProfileCollection().getProfile(collection.getReferencePoint()); // returns a median profile with head at 0

			for(int i= 0; i<collection.getNucleusCount();i++){ // for each roi
				Nucleus n = collection.getCell(i).getNucleus();

				// returns the positive offset index of this profile which best matches the median profile
				int newHeadIndex = n.getAngleProfile().getSlidingWindowOffset(medianToCompare);
				n.addBorderTag(collection.getReferencePoint(), newHeadIndex);

				// check if flipping the profile will help

				double differenceToMedian1 = n.getAngleProfile(collection.getReferencePoint()).differenceToProfile(medianToCompare);
				n.reverse();
				double differenceToMedian2 = n.getAngleProfile(collection.getReferencePoint()).differenceToProfile(medianToCompare);

				if(differenceToMedian1<differenceToMedian2){
					n.reverse(); // put it back if no better
				}

				// also update the tail position
				int tailIndex = n.getIndex(n.findOppositeBorder( n.getPoint(newHeadIndex) ));
				n.addBorderTag("tail", tailIndex);
			}
		}
		
		if(collection.getNucleusClass() == RodentSpermNucleus.class){
			
			Profile medianToCompare = collection.getProfileCollection().getProfile(collection.getOrientationPoint()); // returns a median profile

		    for(int i= 0; i<collection.getNucleusCount();i++){ // for each roi
		      RodentSpermNucleus n = (RodentSpermNucleus)collection.getCell(i).getNucleus();


		      // THE NEW WAY
		      int newTailIndex = n.getAngleProfile().getSlidingWindowOffset(medianToCompare);

		      n.addBorderTag("tail", newTailIndex);

		      // also update the head position
		      int headIndex = n.getIndex(n.findOppositeBorder( n.getPoint(newTailIndex) ));
		      n.addBorderTag("head", headIndex);
		      n.splitNucleusToHeadAndHump();
		    }
			
		}
		
		if(collection.getNucleusClass() == PigSpermNucleus.class){
			Profile medianToCompare = collection.getProfileCollection().getProfile("head"); // returns a median profile with head at 0

			for(int i= 0; i<collection.getNucleusCount();i++){ // for each roi
				PigSpermNucleus n = (PigSpermNucleus)collection.getCell(i).getNucleus();

				// returns the positive offset index of this profile which best matches the median profile
				int newHeadIndex = n.getAngleProfile().getSlidingWindowOffset(medianToCompare);

				n.addBorderTag("head", newHeadIndex);

				// also update the head position
				int tailIndex = n.getIndex(n.findOppositeBorder( n.getPoint(newHeadIndex) ));
				n.addBorderTag("tail", tailIndex);
			}
		}
	}

	
	/**
	 * When a population needs to be reanalysed do not offset nuclei or recalculate best fits;
	 * just get the new median profile 
	 * @param collection the collection of nuclei
	 * @param sourceCollection the collection with segments to copy
	 */
	public static boolean reapplyProfiles(CellCollection collection, CellCollection sourceCollection){
		
		logger = new Logger(collection.getDebugFile(), "MorphologyAnalysis");
		logger.log("Applying existing segmentation profile to population...");
		
		try {
			String referencePoint = collection.getReferencePoint();
			String orientationPoint = collection.getOrientationPoint();
			
			// use the same array length as the source collection to avoid segment slippage
			int profileLength = sourceCollection.getProfileCollection().getProfile(referencePoint).size();
			createProfileAggregateFromPoint(collection, referencePoint, profileLength);
			createProfileAggregateFromPoint(collection, orientationPoint, profileLength);
//			createProfileAggregateFromPoint(collection, referencePoint, (int)sourceCollection.getMedianArrayLength());
//			createProfileAggregateFromPoint(collection, orientationPoint, (int)sourceCollection.getMedianArrayLength());
			
			ProfileCollection pc = collection.getProfileCollection();
			ProfileCollection sc = sourceCollection.getProfileCollection();
			
			
			// What happens when the array length is greater in the source collection? 
			// Segments are added that no longer have an index
			// We need to scale the segments to the array length of the new collection
			pc.addSegments(referencePoint, sc.getSegments(referencePoint));
			pc.addSegments(orientationPoint, sc.getSegments(orientationPoint));
			
			// At hthis point the collection has only a regular profile collection.
			// no frankenprofile has been copied.
			// Create a new frankenprofile
			// copied from segmentation
			reviseSegments(collection, referencePoint);	
//			createProfileAggregates(collection);
			applySegmentsToOtherPointTypes(collection, referencePoint);
//			IJ.log("Regular: "+collection.getProfileCollection().printKeys());
//			IJ.log("Franken: "+collection.getFrankenCollection().printKeys());

		} catch (Exception e) {
			logger.log("Error reapplying profiles: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
			return false;
		}
		logger.log("Re-profiling complete");
		return true;
	}
	
	/**
	 * A temp addition to allow a remapping median to be the same length as the original population median.
	 * Otherwise the segment remapping will fail
	 * @param collection
	 * @param pointType
	 * @param length
	 */
	private static void createProfileAggregateFromPoint(CellCollection collection, String pointType, int length){

		ProfileAggregate profileAggregate = new ProfileAggregate(length);
		collection.getProfileCollection().addAggregate(pointType, profileAggregate);
		
		for(Nucleus n : collection.getNuclei()){
			profileAggregate.addValues(n.getAngleProfile(pointType));
		}

		Profile medians = profileAggregate.getMedian();
		Profile q25     = profileAggregate.getQuartile(25);
		Profile q75     = profileAggregate.getQuartile(75);
		collection.getProfileCollection().addProfile(pointType, medians);
		collection.getProfileCollection().addProfile(pointType+"25", q25);
		collection.getProfileCollection().addProfile(pointType+"75", q75);
	}

	private static void createProfileAggregateFromPoint(CellCollection collection, String pointType){

		ProfileAggregate profileAggregate = new ProfileAggregate((int)collection.getMedianArrayLength());
		collection.getProfileCollection().addAggregate(pointType, profileAggregate);

		for(Nucleus n : collection.getNuclei()){
			profileAggregate.addValues(n.getAngleProfile(pointType));
		}

		Profile medians = profileAggregate.getMedian();
		Profile q25     = profileAggregate.getQuartile(25);
		Profile q75     = profileAggregate.getQuartile(75);
		
		ProfileCollection pc = collection.getProfileCollection();
		pc.addProfile(pointType, medians);
		pc.addProfile(pointType+"25", q25);
		pc.addProfile(pointType+"75", q75);

	}

	private static void createProfileAggregates(CellCollection collection){
		try{
			for( String pointType : collection.getProfileCollection().getProfileKeys() ){
				createProfileAggregateFromPoint(collection, pointType);   
			}
		} catch(Exception e){
			logger.log("Error creating profile aggregates: "+e.getMessage(), Logger.ERROR);
			logger.log(collection.getProfileCollection().printKeys());
		}
	}
	

	private static double compareProfilesToMedian(CellCollection collection, String pointType){
		double[] scores = collection.getDifferencesToMedianFromPoint(pointType);
		double result = 0;
		for(double s : scores){
			result += s;
		}
		return result;
	}
	
	private static void runSegmentation(CellCollection collection, String pointType){
		logger.log("Beginning segmentation...");
		try{	
			createSegments(collection, pointType);
			assignSegments(collection, pointType);
			
			reviseSegments(collection, pointType);		
//			
			// At this point, the franken collection contains tip/head values only
			
			createProfileAggregates(collection);
			
			applySegmentsToOtherPointTypes(collection, pointType);
			
			// At this point, the franken collection still contains tip/head values only
			
		} catch(Exception e){
			logger.log("Error segmenting: "+e.getMessage(), Logger.ERROR);
			collection.getProfileCollection().printKeys();
		}
		logger.log("Segmentation complete");
	}
	
	/**
	 * Run the segmenter on the median profile for the given point type
	 * @param collection
	 * @param pointType
	 */
	private static void createSegments(CellCollection collection, String pointType){
		// get the segments within the median curve
		ProfileCollection pc = collection.getProfileCollection();

		Profile medianToCompare = pc.getProfile(pointType);

		ProfileSegmenter segmenter = new ProfileSegmenter(medianToCompare);		  
		List<NucleusBorderSegment> segments = segmenter.segment();

		logger.log("Found "+segments.size()+" segments in "+pointType+" profile");
		pc.addSegments(pointType, segments);
	}

	/**
	 * From the calculated median profile segments, assign segments to each nucleus
	 * based on the best offset fit of the start and end indexes 
	 * @param collection
	 * @param pointType
	 */
	private static void assignSegments(CellCollection collection, String pointType){

		logger.log("Assigning segments to nuclei...");
		
		ProfileCollection pc = collection.getProfileCollection();

		// find the corresponding point in each Nucleus
		Profile medianToCompare = pc.getProfile(pointType);
		for(int i= 0; i<collection.getNucleusCount();i++){ 
			Nucleus n = collection.getCell(i).getNucleus();
			
			// remove any existing segments
			n.clearSegments();

			// go through each segment defined for the median curve
			int j=0;
			for(NucleusBorderSegment b : pc.getSegments(pointType)){
				
				// get the positions the segment begins and ends in the median profile
				int startIndexInMedian = b.getStartIndex();
				int endIndexInMedian = b.getEndIndex();

				// find the positions these correspond to in the offset profiles
				
				// get the median profile, indexed to the start or end point
				Profile startOffsetMedian = medianToCompare.offset(startIndexInMedian);
				Profile endOffsetMedian = medianToCompare.offset(endIndexInMedian);

				// find the index at the point of the best fit
				int startIndex = n.getAngleProfile().getSlidingWindowOffset(startOffsetMedian);
				int endIndex = n.getAngleProfile().getSlidingWindowOffset(endOffsetMedian);

				// create a segment at these points
				NucleusBorderSegment seg = new NucleusBorderSegment(startIndex, endIndex);
				seg.setSegmentType("Seg_"+j);
				n.addSegment(seg);
				n.addSegmentTag("Seg_"+j, j);
				j++;
			}
		}
		logger.log("Segments assigned to nuclei");
	}

	/**
	 * Update initial segment assignments by stretching each segment to the best possible fit along
	 * the median profile 
	 * @param collection
	 * @param pointType
	 */
	private static void reviseSegments(CellCollection collection, String pointType){
		logger.log("Refining segment assignments...");
		
		ProfileCollection pc = collection.getProfileCollection();
		List<NucleusBorderSegment> segments = pc.getSegments(pointType);
		
		// make a new profile collection to hold the frankendata
		ProfileCollection frankenCollection = new ProfileCollection();

		// fill the frankenCollection  with the segment information previously calculated
		frankenCollection.addAggregate( pointType, new ProfileAggregate((int)collection.getMedianArrayLength()));
		frankenCollection.addSegments(pointType, segments);

		SegmentFitter fitter = new SegmentFitter(pc.getProfile(pointType), segments);
		List<Profile> frankenProfiles = new ArrayList<Profile>(0);

		for(Nucleus n : collection.getNuclei()){ 
			fitter.fit(n);

			// recombine the segments at the lengths of the median profile segments
			// what does it look like?
			Profile recombinedProfile = fitter.recombine(n);
			frankenCollection.getAggregate(pointType).addValues(recombinedProfile);
			frankenProfiles.add(recombinedProfile);
		}
		frankenCollection.addNucleusProfiles(pointType, frankenProfiles);
		// update the profile aggregate
		frankenCollection.createProfileAggregateFromPoint(    pointType, (int) collection.getMedianArrayLength()    );
		
		// Added in for completeness
		// apply the frankenprofile segments to other point type
//		Profile referenceProfile = frankenCollection.getProfile(pointType);
//		Profile orientProfile = frankenCollection.getProfile(collection.getOrientationPoint());
//		int offset = orientProfile.getSlidingWindowOffset(referenceProfile);
//		frankenCollection.addSegments(collection.getOrientationPoint(), pointType, offset);
//		frankenCollection.createProfileAggregateFromPoint(    collection.getOrientationPoint(), (int) collection.getMedianArrayLength()    );
		
		
		collection.setFrankenCollection(frankenCollection);
		logger.log("Segment assignments refined");
	}
	
	private static void applySegmentsToOtherPointTypes(CellCollection collection, String pointType){

		ProfileCollection pc = collection.getProfileCollection();
		
		Profile referenceProfile = pc.getProfile(pointType);
		Profile orientProfile = pc.getProfile(collection.getOrientationPoint());
		int offset = orientProfile.getSlidingWindowOffset(referenceProfile);
		
		pc.addSegments(collection.getOrientationPoint(), pointType, offset);
	}
	
//	private static void exportSegments(NucleusCollection collection, String pointType){
//		
//		logger.log("Exporting segments...");
//		// export the individual segment files for each nucleus
//		for(Nucleus n : collection.getNuclei()){
//			n.exportSegments();
//		}
//
//		// also export the group stats for each segment
//		TableExporter tableExport = new TableExporter(collection.getFolder()+File.separator+collection.getOutputFolderName());
//		tableExport.addColumnHeading("PATH"    );
//		tableExport.addColumnHeading("POSITION");
//
//		
//		try{
//			List<NucleusBorderSegment> segments = collection.getProfileCollection().getSegments(pointType);
//			if(!segments.isEmpty()){
//
//				for(NucleusBorderSegment seg : segments){
//					tableExport.addColumnHeading(seg.getSegmentType());
//				}
//
//				for(Nucleus n : collection.getNuclei()){
//
//					tableExport.addRow("PATH", n.getPath());
//					tableExport.addRow("POSITION", n.getPosition());
//
//					for(NucleusBorderSegment seg : segments){
//						NucleusBorderSegment nucSeg = n.getSegmentTag(seg.getSegmentType());
//						// export the segment length as a fraction of the total array length
//						tableExport.addRow(seg.getSegmentType(),   (double)nucSeg.length(n.getLength())/(double)n.getLength()      );
//					}
//				}
//				tableExport.export("log.segments."+collection.getType());
//				logger.log("Segments exported");
//			}
//		}catch(Exception e){
//			logger.log("Error exporting segments: "+e.getMessage(), Logger.ERROR);
//		}
//	}
	

//	private static void exportClusteringScript(NucleusCollection collection){
//
//		StringBuilder outLine = new StringBuilder();
//
//		String path = collection.getFolder().getAbsolutePath()+File.separator+collection.getOutputFolderName()+File.separator;
//		path = path.replace("\\", "\\\\");// escape folder separator for R
//		outLine.append("path = \""+path+"\"\r\n"); 
//
//		outLine.append("nuclei = read.csv(paste(path,\"log.segments.analysable.txt\", sep=\"\"),header=T, sep=\"\\t\")\r\n");
//		outLine.append("d <- dist(as.matrix(nuclei))\r\n");
//		outLine.append("hc <- hclust(d, method=\"ward.D2\")\r\n");
//		outLine.append("ct <- cutree(hc, k=5)\r\n");
//		outLine.append("nuclei <- cbind(ct, nuclei , deparse.level=1)\r\n");
//		outLine.append("tt <- table(nuclei $ct)\r\n");
//		outLine.append("for (i in 1:dim(tt)) {\r\n");
//		outLine.append("\tsub <- subset(nuclei , ct==i)\r\n");
//		outLine.append("\twrite.table(subset(sub, select=c(PATH, POSITION)), file=paste(path,\"mapping_cluster\",i,\".analysable.txt\", sep=\"\"), sep=\"\\t\", row.names=F, quote=F)\r\n");
//		outLine.append("}\r\n");
//
//		IJ.append(outLine.toString(), path+"clusteringScript.r");
//	}
}
