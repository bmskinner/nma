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
import no.export.Logger;
import no.nuclei.INuclearFunctions;

public class SegmentationAnalysis {

	public SegmentationAnalysis(INuclearCollection collection, String pointType){

		IJ.log("    Beginning segmentation...");
		try{	
			assignSegments(collection, pointType);
			reviseSegments(collection, pointType);
			exportSegments(collection, pointType);
			makeClusteringScript(collection);

			collection.createProfileAggregates();
		} catch(Exception e){
			IJ.log("    Error segmenting: "+e.getMessage());
			collection.getProfileCollection().printKeys();
		}
		IJ.log("   Segmentation complete...");
	}

	private void assignSegments(INuclearCollection collection, String pointType){
		// get the segments within the median curve

		collection.getProfileCollection().segmentProfiles();

		IJ.log("    Assigning segments to nuclei...");

		// find the corresponding point in each Nucleus
		Profile medianToCompare = collection.getProfileCollection().getProfile(pointType);
		for(int i= 0; i<collection.getNucleusCount();i++){ // for each roi
			INuclearFunctions n = collection.getNucleus(i);
			n.clearSegments();

			int j=0;
			for(NucleusBorderSegment b : collection.getProfileCollection().getSegments(pointType)){
				int startIndexInMedian = b.getStartIndex();
				int endIndexInMedian = b.getEndIndex();

				Profile startOffsetMedian = medianToCompare.offset(startIndexInMedian);
				Profile endOffsetMedian = medianToCompare.offset(endIndexInMedian);

				int startIndex = n.getAngleProfile().getSlidingWindowOffset(startOffsetMedian);
				int endIndex = n.getAngleProfile().getSlidingWindowOffset(endOffsetMedian);

				NucleusBorderSegment seg = new NucleusBorderSegment(startIndex, endIndex);
				seg.setSegmentType("Seg_"+j);
				n.addSegment(seg);
				n.addSegmentTag("Seg_"+j, j);
				j++;
			}
		}
	}

	private void reviseSegments(INuclearCollection collection, String pointType){
		IJ.log("    Refining segment assignments...");

		List<NucleusBorderSegment> segments = collection.getProfileCollection().getSegments(pointType);
		ProfileCollection frankensteinProfiles = new ProfileCollection("frankenstein");

		frankensteinProfiles.addAggregate( pointType, new ProfileAggregate((int)collection.getMedianArrayLength()));
		frankensteinProfiles.addSegments(pointType, segments);
		frankensteinProfiles.preparePlots(INuclearCollection.CHART_WINDOW_WIDTH, INuclearCollection.CHART_WINDOW_HEIGHT, collection.getMaxProfileLength());
		SegmentFitter fitter = new SegmentFitter(collection.getProfileCollection().getProfile(pointType), segments);
		List<Profile> frankenProfiles = new ArrayList<Profile>(0);

		IJ.log("    Fitting profile segments...");
		for(INuclearFunctions n : collection.getNuclei()){ 
			fitter.fit(n);

			// recombine the segments at the lengths of the median profile segments
			// what does it look like?
			Profile recombinedProfile = fitter.recombine(n);
			frankensteinProfiles.getAggregate(pointType).addValues(recombinedProfile);
			frankenProfiles.add(recombinedProfile);
		}
		IJ.log("    Created "+frankenProfiles.size()+" frankenprofiles");
		frankensteinProfiles.createProfileAggregateFromPoint(    pointType, (int) collection.getMedianArrayLength()    );
		frankensteinProfiles.drawProfilePlots(pointType, frankenProfiles);
		frankensteinProfiles.addMedianLinesToPlots();
		IJ.log("    Created frankenmedian");

		// get the regions with the highest variability within the population
		List<Integer> variableIndexes = frankensteinProfiles.findMostVariableRegions(pointType);

		// these points are indexes in the frankenstein profile. Find the points in each nucleus profile that they
		// compare to 
		// interpolate the frankenprofile to the frankenmedian length. Then we can use the index point directly.
		// export clustering info
		IJ.log("    Top variable indexes:");
		Logger logger = new Logger(collection.getFolder()+File.separator+collection.getOutputFolder());
		logger.addColumnHeading("ID");
		logger.addColumnHeading("AREA");
		logger.addColumnHeading("PERIMETER");
		for(int index : variableIndexes){
			IJ.log("      Index "+index);
			// get the points in a window centred on the index
			for(int i=0; i<21;i++){ // index plus 10 positions to either side
				logger.addColumnHeading("IQR_INDEX_"+index+"_"+i);
			}
		}

		for(int i= 0; i<collection.getNucleusCount();i++){ // for each roi
			//			  IJ.log("Nucleus "+i+" of "+this.getNucleusCount());
			INuclearFunctions n = collection.getNucleus(i);
			logger.addRow("ID",		n.getPath()+"-"+n.getNucleusNumber());
			logger.addRow("AREA",		n.getArea());
			logger.addRow("PERIMETER",n.getPerimeter());
			Profile frankenProfile = frankenProfiles.get(i);
			Profile interpolatedProfile = frankenProfile.interpolate(frankensteinProfiles.getProfile(pointType).size());
			for(int index : variableIndexes){
				// get the points in a window centred on the index
				//				  IJ.log("Index "+index);
				Profile window = interpolatedProfile.getWindow(index, 10);
				for(int j=0; j<21;j++){ // index plus 10 positions to either side
					//					  IJ.log("  Poisiton "+j);
					logger.addRow("IQR_INDEX_"+index+"_"+j, window.get(j));
				}

			}

		}
		//		  IJ.log("    Added all rows");
		//		  logger.print();
		logger.export("log.variability_regions."+collection.getType());
		//		  IJ.log("    Exported rows");
		frankensteinProfiles.exportProfilePlots(collection.getFolder()+
				File.separator+
				collection.getOutputFolder(), collection.getType());


	}

	private void exportSegments(INuclearCollection collection, String pointType){
		// export the individual segment files for each nucleus
		for(INuclearFunctions n : collection.getNuclei()){
			n.exportSegments();
		}

		// also export the group stats for each segment
		Logger logger = new Logger(collection.getFolder()+File.separator+collection.getOutputFolder());
		logger.addColumnHeading("PATH"    );
		logger.addColumnHeading("POSITION");

		IJ.log("    Exporting segments...");
		try{
			List<NucleusBorderSegment> segments = collection.getProfileCollection().getSegments(pointType);
			if(!segments.isEmpty()){

				for(NucleusBorderSegment seg : segments){
					logger.addColumnHeading(seg.getSegmentType());
					//					  IJ.log("    Heading made: "+seg.getSegmentType());
				}

				for(INuclearFunctions n : collection.getNuclei()){

					logger.addRow("PATH", n.getPath());
					logger.addRow("POSITION", n.getPosition());

					for(NucleusBorderSegment seg : segments){
						NucleusBorderSegment nucSeg = n.getSegmentTag(seg.getSegmentType());
						// export the segment length as a fraction of the total array length
						logger.addRow(seg.getSegmentType(),   (double)nucSeg.length(n.getLength())/(double)n.getLength()      );
					}
				}
				logger.export("log.segments."+collection.getType());
				IJ.log("    Segments exported");
			}
		}catch(Exception e){
			IJ.log("    Error exporting segments: "+e.getMessage());
		}
	}

	private void makeClusteringScript(INuclearCollection collection){

		StringBuilder outLine = new StringBuilder();

		String path = collection.getFolder().getAbsolutePath()+File.separator+collection.getOutputFolder()+File.separator;
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
