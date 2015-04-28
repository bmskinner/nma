package no.analysis;

import ij.IJ;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import no.collections.INuclearCollection;
import no.components.NucleusBorderSegment;
import no.components.Profile;
import no.components.ProfileAggregate;
import no.components.ProfileCollection;
import no.export.TableExporter;
import no.nuclei.INuclearFunctions;
import no.utility.Logger;

/**
 * This is the core of the morphology analysis pipeline.
 * It is the only module that is essential. This offsets nucleus profiles,
 * generates the median profiles, segments them, and applies the segments to
 * nuclei.
 */
public class MorphologyAnalysis {
	
	private static Logger logger;
	
	public static boolean run(INuclearCollection collection){

		logger = new Logger(collection.getDebugFile(), "MorphologyAnalysis");
		try{

			logger.log("Beginning core morphology analysis");

			String pointType = collection.getReferencePoint();

			// setup the plots and profile agregates
			initialiseProfileCollection(collection);

			// profile the collection
			runProfiler(collection, pointType);

			// segment the profiles from head
			runSegmentation(collection, pointType);

			// export the core data

			// run the exports
			exportProfiles(collection);
			exportSegments(collection, pointType);
			exportClusteringScript(collection);

			// begin migrating these export functions up
			exportVariabilityRegions(collection, pointType);
			drawProfileCollection(   collection, pointType);

			logger.log("Core morphology analysis complete");
			return true;
		} catch(Exception e){
			
			logger.log("Error in morphology analysis: "+e.getMessage(), Logger.ERROR);
			logger.log(e.getStackTrace().toString(), Logger.ERROR);
			logger.log("Collection keys:", Logger.ERROR);
			collection.getProfileCollection().printKeys(collection.getDebugFile());
			logger.log("FrankenCollection keys:", Logger.ERROR);
			collection.getFrankenCollection().printKeys(collection.getDebugFile());
			return false;
		}

	}

	private static void runProfiler(INuclearCollection collection, String pointType){
				
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
			logger.log("Reticulating splines: score: "+(int)score);
		}
		
		// update the median profile with the final offset locations
		createProfileAggregateFromPoint(collection, collection.getOrientationPoint());

	}
	
	private static void initialiseProfileCollection(INuclearCollection collection){
		ProfileCollection pc = collection.getProfileCollection();
		
		// aggregates
		createProfileAggregateFromPoint(collection, collection.getReferencePoint());
		// plots
		pc.preparePlot(collection.getReferencePoint()  , INuclearCollection.CHART_WINDOW_WIDTH, INuclearCollection.CHART_WINDOW_HEIGHT, collection.getMaxProfileLength());
		pc.preparePlot(collection.getOrientationPoint(), INuclearCollection.CHART_WINDOW_WIDTH, INuclearCollection.CHART_WINDOW_HEIGHT, collection.getMaxProfileLength());
		
		// features
		
	}
	
	/**
	 * When a population needs to be reanalysed do not offset nuclei or recalculate best fits;
	 * just get the new median profile 
	 * @param collection the collection of nuclei
	 * @param sourceCollection the collection with segments to copy
	 */
	public static void reapplyProfiles(INuclearCollection collection, INuclearCollection sourceCollection){
		
		logger.log("Applying existing segmentation profile to population...");
		
		String referencePoint = collection.getReferencePoint();
		String orientationPoint = collection.getOrientationPoint();
		
		createProfileAggregateFromPoint(collection, referencePoint, (int)sourceCollection.getMedianArrayLength());
		createProfileAggregateFromPoint(collection, orientationPoint, (int)sourceCollection.getMedianArrayLength());
		
		ProfileCollection pc = collection.getProfileCollection();
		ProfileCollection sc = sourceCollection.getProfileCollection();
		
		pc.addSegments(referencePoint, sc.getSegments(referencePoint));
		pc.addSegments(sourceCollection.getOrientationPoint(), sc.getSegments(orientationPoint));
		pc.preparePlots(INuclearCollection.CHART_WINDOW_WIDTH, INuclearCollection.CHART_WINDOW_HEIGHT, collection.getMaxProfileLength());
		pc.addMedianLineToPlots(orientationPoint);
		pc.exportProfilePlots(collection.getOutputFolder().getAbsolutePath(), collection.getType());
		
		logger.log("Re-profiling complete");
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
		
		ProfileCollection pc = collection.getProfileCollection();
		pc.addProfile(pointType, medians);
		pc.addProfile(pointType+"25", q25);
		pc.addProfile(pointType+"75", q75);

	}

	private static void createProfileAggregates(INuclearCollection collection){
		try{
			for( String pointType : collection.getProfileCollection().getProfileKeys() ){
				createProfileAggregateFromPoint(collection, pointType);   
			}
		} catch(Exception e){
			logger.log("Error creating profile aggregates: "+e.getMessage(), Logger.ERROR);
			collection.getProfileCollection().printKeys(collection.getDebugFile());
		}
	}
	
	private static void exportProfiles(INuclearCollection collection){

		ProfileCollection pc = collection.getProfileCollection();
		// get the profile plots created
//		pc.preparePlot(collection.getOrientationPoint(), INuclearCollection.CHART_WINDOW_WIDTH, INuclearCollection.CHART_WINDOW_HEIGHT, collection.getMaxProfileLength());
		// export the profiles
		collection.drawProfilePlots();

		pc.addMedianLineToPlots(collection.getOrientationPoint());

		pc.exportProfilePlots(collection.getOutputFolder().getAbsolutePath(), collection.getType());
	}

	private static double compareProfilesToMedian(INuclearCollection collection, String pointType){
		double[] scores = collection.getDifferencesToMedianFromPoint(pointType);
		double result = 0;
		for(double s : scores){
			result += s;
		}
		return result;
	}
	
	private static void runSegmentation(INuclearCollection collection, String pointType){
		logger.log("Beginning segmentation...");
		try{	
			createSegments(collection, pointType);
			assignSegments(collection, pointType);
			
			reviseSegments(collection, pointType);			

			createProfileAggregates(collection);
			
			applySegmentsToOtherPointTypes(collection, pointType);
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
	private static void createSegments(INuclearCollection collection, String pointType){
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
	private static void assignSegments(INuclearCollection collection, String pointType){

		logger.log("Assigning segments to nuclei...");
		
		ProfileCollection pc = collection.getProfileCollection();

		// find the corresponding point in each Nucleus
		Profile medianToCompare = pc.getProfile(pointType);
		for(int i= 0; i<collection.getNucleusCount();i++){ 
			INuclearFunctions n = collection.getNucleus(i);
			
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
	private static void reviseSegments(INuclearCollection collection, String pointType){
		logger.log("Refining segment assignments...");
		
		ProfileCollection pc = collection.getProfileCollection();
		List<NucleusBorderSegment> segments = pc.getSegments(pointType);
		
		// make a new profile collection to hold the frankendata
		ProfileCollection frankenCollection = new ProfileCollection("frankenstein");

		// fill the frankenCollection  with the segment information previously calculated
		frankenCollection.addAggregate( pointType, new ProfileAggregate((int)collection.getMedianArrayLength()));
		frankenCollection.addSegments(pointType, segments);

		SegmentFitter fitter = new SegmentFitter(pc.getProfile(pointType), segments);
		List<Profile> frankenProfiles = new ArrayList<Profile>(0);

		for(INuclearFunctions n : collection.getNuclei()){ 
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
		frankenCollection.preparePlot(collection.getReferencePoint()  , INuclearCollection.CHART_WINDOW_WIDTH, INuclearCollection.CHART_WINDOW_HEIGHT, collection.getMaxProfileLength());
		frankenCollection.preparePlot(collection.getOrientationPoint(), INuclearCollection.CHART_WINDOW_WIDTH, INuclearCollection.CHART_WINDOW_HEIGHT, collection.getMaxProfileLength());
		
		collection.setFrankenCollection(frankenCollection);
		logger.log("Segment assignments refined");
	}
	
	private static void applySegmentsToOtherPointTypes(INuclearCollection collection, String pointType){

		ProfileCollection pc = collection.getProfileCollection();
		
		Profile referenceProfile = pc.getProfile(pointType);
		Profile orientProfile = pc.getProfile(collection.getOrientationPoint());
		int offset = orientProfile.getSlidingWindowOffset(referenceProfile);
		
		pc.addSegments(collection.getOrientationPoint(), pointType, offset);
	}

	private static void drawProfileCollection(INuclearCollection collection, String pointType){

		ProfileCollection pc = collection.getFrankenCollection();

		// get the nucleus profiles
		List<Profile> profiles = pc.getNucleusProfiles(pointType);

		pc.drawProfilePlots(pointType, profiles);
		pc.addMedianLineToPlots(pointType);

		pc.exportProfilePlots(collection.getFolder()+
				File.separator+
				collection.getOutputFolderName(), collection.getType());

	}
	
	private static void exportVariabilityRegions(INuclearCollection collection, String pointType){
		
		ProfileCollection pc = collection.getFrankenCollection();
		
		// get the nucleus profiles
		List<Profile> profiles = pc.getNucleusProfiles(pointType);
		
		// get the regions with the highest variability within the population
		List<Integer> variableIndexes = pc.findMostVariableRegions(pointType);

		// these points are indexes in the frankenstein profile. Find the points in each nucleus profile that they
		// compare to 
		// interpolate the frankenprofile to the frankenmedian length. Then we can use the index point directly.
		// export clustering info
		TableExporter tableExport = new TableExporter(collection.getOutputFolder());
		tableExport.addColumnHeading("ID");
		tableExport.addColumnHeading("AREA");
		tableExport.addColumnHeading("PERIMETER");
		for(int index : variableIndexes){
			// get the points in a window centred on the index
			for(int i=0; i<21;i++){ // index plus 10 positions to either side
				tableExport.addColumnHeading("IQR_INDEX_"+index+"_"+i);
			}
		}

		for(int i= 0; i<collection.getNucleusCount();i++){ // for each roi
			INuclearFunctions n = collection.getNucleus(i);
			tableExport.addRow("ID",		n.getPath()+"-"+n.getNucleusNumber());
			tableExport.addRow("AREA",		n.getArea());
			tableExport.addRow("PERIMETER",n.getPerimeter());
			Profile frankenProfile = profiles.get(i);
			Profile interpolatedProfile = frankenProfile.interpolate(pc.getProfile(pointType).size());
			for(int index : variableIndexes){
				// get the points in a window centred on the index
				Profile window = interpolatedProfile.getWindow(index, 10);
				for(int j=0; j<21;j++){ // index plus 10 positions to either side
					tableExport.addRow("IQR_INDEX_"+index+"_"+j, window.get(j));
				}

			}

		}

		tableExport.export("log.variability_regions."+collection.getType());
	}

	private static void exportSegments(INuclearCollection collection, String pointType){
		
		logger.log("Exporting segments...");
		// export the individual segment files for each nucleus
		for(INuclearFunctions n : collection.getNuclei()){
			n.exportSegments();
		}

		// also export the group stats for each segment
		TableExporter tableExport = new TableExporter(collection.getFolder()+File.separator+collection.getOutputFolderName());
		tableExport.addColumnHeading("PATH"    );
		tableExport.addColumnHeading("POSITION");

		
		try{
			List<NucleusBorderSegment> segments = collection.getProfileCollection().getSegments(pointType);
			if(!segments.isEmpty()){

				for(NucleusBorderSegment seg : segments){
					tableExport.addColumnHeading(seg.getSegmentType());
				}

				for(INuclearFunctions n : collection.getNuclei()){

					tableExport.addRow("PATH", n.getPath());
					tableExport.addRow("POSITION", n.getPosition());

					for(NucleusBorderSegment seg : segments){
						NucleusBorderSegment nucSeg = n.getSegmentTag(seg.getSegmentType());
						// export the segment length as a fraction of the total array length
						tableExport.addRow(seg.getSegmentType(),   (double)nucSeg.length(n.getLength())/(double)n.getLength()      );
					}
				}
				tableExport.export("log.segments."+collection.getType());
				logger.log("Segments exported");
			}
		}catch(Exception e){
			logger.log("Error exporting segments: "+e.getMessage(), Logger.ERROR);
		}
	}

	private static void exportClusteringScript(INuclearCollection collection){

		StringBuilder outLine = new StringBuilder();

		String path = collection.getFolder().getAbsolutePath()+File.separator+collection.getOutputFolderName()+File.separator;
		path = path.replace("\\", "\\\\");// escape folder separator for R
		outLine.append("path = \""+path+"\"\r\n"); 

		outLine.append("nuclei = read.csv(paste(path,\"log.segments.analysable.txt\", sep=\"\"),header=T, sep=\"\\t\")\r\n");
		outLine.append("d <- dist(as.matrix(nuclei))\r\n");
		outLine.append("hc <- hclust(d, method=\"ward.D2\")\r\n");
		outLine.append("ct <- cutree(hc, k=5)\r\n");
		outLine.append("nuclei <- cbind(ct, nuclei , deparse.level=1)\r\n");
		outLine.append("tt <- table(nuclei $ct)\r\n");
		outLine.append("for (i in 1:dim(tt)) {\r\n");
		outLine.append("\tsub <- subset(nuclei , ct==i)\r\n");
		outLine.append("\twrite.table(subset(sub, select=c(PATH, POSITION)), file=paste(path,\"mapping_cluster\",i,\".analysable.txt\", sep=\"\"), sep=\"\\t\", row.names=F, quote=F)\r\n");
		outLine.append("}\r\n");

		IJ.append(outLine.toString(), path+"clusteringScript.r");
	}
}
